#!/usr/bin/env python3
"""Exact local-only ATL @Issue applicability correction; no code execution or bootstrap hooks."""
from __future__ import annotations
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import struct
import sys
import zlib

ROOT = Path(__file__).resolve().parents[1]
SPEC = ROOT / 'src/upstreamRuntime/startup-overrides-v1.json'
LEGACY = ROOT / 'scripts/build_upstream_runtime_overrides.py'
LEGACY_SHA = '09669c8333ce94358b30f8c303f9885a459388ccfd02d0c867b8b77ece472ed6'
for part in [LEGACY, *LEGACY.parents]:
    if part.is_symlink():
        raise ValueError('symlink helper path')
if hashlib.sha256(LEGACY.read_bytes()).hexdigest() != LEGACY_SHA:
    raise ValueError('immutable safe ZIP helper drift')
loader = importlib.util.spec_from_file_location('startup_immutable_zip', LEGACY)
legacy = importlib.util.module_from_spec(loader)
loader.loader.exec_module(legacy)
require, sha, read_file, parse_json = legacy.require, legacy.sha, legacy.read_file, legacy.parse_json


class ClassFile:
    """Bounded structure parser. UTF8 locations come from annotation operands, never text searches."""
    def __init__(self, data):
        require(isinstance(data, bytes) and len(data) <= legacy.MAX_ENTRY, 'class size/type')
        self.data, self.pos = data, 0
        require(self.u4() == 0xcafebabe, 'class magic')
        self.minor, self.major = self.u2(), self.u2()
        require(45 <= self.major <= 65, 'class version')
        self.cp, self.utf_locations, self.references = {}, {}, []
        count, index = self.u2(), 1
        while index < count:
            tag = self.u1()
            if tag == 1:
                offset = self.pos
                size = self.u2()
                raw = self.take(size)
                # All reviewed annotation names/ranges and field names are ASCII.
                self.cp[index] = raw
                self.utf_locations[index] = (offset, size)
            elif tag in (3, 4):
                self.take(4)
            elif tag in (5, 6):
                self.take(8); index += 1
                require(index < count, 'truncated wide constant')
            elif tag in (7, 8, 16, 19, 20):
                self.references.append(self.u2())
            elif tag in (9, 10, 11, 12, 17, 18):
                self.references.extend((self.u2(), self.u2()))
            elif tag == 15:
                self.u1(); self.references.append(self.u2())
            else:
                raise ValueError('unsupported class constant')
            index += 1
        self.access, self.this_class, self.super_class = self.u2(), self.u2(), self.u2()
        for _ in range(self.u2()): self.u2()
        self.fields = []
        for _ in range(self.u2()):
            self.u2(); name, descriptor = self.u2(), self.u2()
            self.references.extend((name, descriptor))
            self.fields.append(self.utf(name))
            self.attributes()
        for _ in range(self.u2()):
            self.u2(); name, descriptor = self.u2(), self.u2()
            self.references.extend((name, descriptor)); self.attributes()
        self.annotation_strings, self.annotations = [], []
        self.attributes(class_level=True)
        require(self.pos == len(data), 'trailing class bytes')

    def take(self, size):
        require(isinstance(size, int) and 0 <= size <= len(self.data) - self.pos, 'truncated class')
        start = self.pos; self.pos += size
        return self.data[start:self.pos]

    def u1(self): return self.take(1)[0]
    def u2(self): return struct.unpack('>H', self.take(2))[0]
    def u4(self): return struct.unpack('>I', self.take(4))[0]
    def utf(self, index):
        require(index in self.cp, 'invalid UTF8 operand')
        return self.cp[index].decode('ascii')

    def attributes(self, class_level=False):
        for _ in range(self.u2()):
            name = self.utf(self.u2()); size = self.u4(); end = self.pos + size
            require(end <= len(self.data), 'attribute extent')
            if class_level and name == 'RuntimeVisibleAnnotations':
                for _ in range(self.u2()): self.annotations.append(self.annotation())
                require(self.pos == end, 'annotation attribute extent')
            else:
                self.take(size)

    def annotation(self):
        kind = self.utf(self.u2()); values = {}
        for _ in range(self.u2()):
            key = self.utf(self.u2()); require(key not in values, 'duplicate annotation element')
            values[key] = self.element()
        return kind, values

    def element(self):
        tag = chr(self.u1())
        if tag == 's':
            index = self.u2(); self.annotation_strings.append(index)
            return ('s', index, self.utf(index))
        if tag in 'BCDFIJSZc': return (tag, self.u2())
        if tag == 'e': return (tag, self.u2(), self.u2())
        if tag == '@': return (tag, self.annotation())
        if tag == '[': return (tag, [self.element() for _ in range(self.u2())])
        raise ValueError('invalid annotation element tag')


def patch_class(data, contract):
    legacy.digest_matches(data, contract['before_sha256'])
    original = ClassFile(data)
    candidates = [values for kind, values in original.annotations if kind == contract['annotation']]
    require(len(candidates) == 1, 'exactly one reviewed @Issue required')
    issue = candidates[0]
    require(issue.get('modId', (None,))[0] == 's' and issue['modId'][2] == contract['mod_id'], 'wrong issue mod')
    value = issue.get(contract['element'])
    require(value is not None and value[0] == 's' and value[2] == contract['before'], 'wrong annotation range')
    index = value[1]
    require(original.annotation_strings.count(index) == 1 and index not in original.references,
            'annotation range UTF8 is shared with another class operand')
    offset, size = original.utf_locations[index]
    replacement = contract['after'].encode('ascii')
    require(len(replacement) <= 65535, 'replacement UTF8 length')
    corrected = data[:offset] + struct.pack('>H', len(replacement)) + replacement + data[offset + 2 + size:]
    check = ClassFile(corrected)
    revised = [v for kind, v in check.annotations if kind == contract['annotation']]
    require(len(revised) == 1 and revised[0][contract['element']][2] == contract['after'], 'annotation rewrite failed')
    return corrected, {'path': contract['path'], 'before_sha256': sha(data), 'after_sha256': sha(corrected),
                       'constant_pool_index': index, 'utf8_length_offset': offset, 'before': contract['before'],
                       'after': contract['after'], 'all_other_class_bytes_invariant': True}


def load_spec(path=SPEC):
    spec = parse_json(read_file(path, legacy.MAX_JSON))
    require(set(spec) == {'schema', 'revision', 'legacy_builder_sha256', 'artifact', 'citadel', 'patch', 'scope'}, 'spec fields')
    require(spec['schema'] == 'bc.upstream_startup_runtime_overrides.v1' and spec['revision'] == 'bc-startup.1', 'spec identity')
    require(spec['legacy_builder_sha256'] == LEGACY_SHA, 'legacy helper pin')
    for item in (spec['artifact'], spec['citadel']):
        for field in ('input', 'manifest'):
            legacy.safe_name(item[field]); require('/' not in item[field], 'basename required')
        require(item['url'].startswith('https://mediafilez.forgecdn.net/files/'), 'unreviewed origin')
    require(spec['artifact']['input'] == 'alltheleaks-1.1.1+1.20.1-forge.jar' and
            spec['artifact']['output'] == 'alltheleaks-1.1.1+1.20.1-forge-bc-startup.1.jar', 'artifact identity')
    p = spec['patch']
    require(p['annotation'] == 'Ldev/uncandango/alltheleaks/annotation/Issue;' and p['mod_id'] == 'citadel' and
            p['element'] == 'versionRange' and p['before'] == '[2.5.4,)' and
            p['after'] == '[2.5.4,2.6.3),(2.6.3,)', 'exact applicability correction required')
    return spec


def build(inputs, output, spec_path=SPEC):
    inputs, output = legacy.regular_path(inputs), legacy.regular_path(output)
    require(inputs.is_dir() and not output.exists() and output.parent.is_dir(), 'existing originals/fresh output required')
    require(inputs != output and inputs not in output.parents and output not in inputs.parents, 'disjoint trees required')
    spec = load_spec(spec_path); artifact, citadel, patch = spec['artifact'], spec['citadel'], spec['patch']
    originals = {}
    for item in (artifact, citadel):
        body = read_file(inputs / item['input']); legacy.digest_matches(body, item['sha256']); legacy.digest_matches(body, item['sha1'], 'sha1')
        originals[item['input']] = body
    _, abi_entries = legacy.archive(originals[citadel['input']])
    abi = abi_entries[citadel['class']]; legacy.digest_matches(abi, citadel['class_sha256'])
    fields = ClassFile(abi).fields
    require('dataMap' not in fields, 'reviewed Citadel ABI no longer matches')
    raw = originals[artifact['input']]; _, entries = legacy.archive(raw)
    original = entries[patch['path']]; legacy.digest_matches(original, patch['before_sha256'])
    revised, correction = patch_class(original, patch)
    corrected = legacy.rewrite_zip(raw, {patch['path']: revised})
    before_classes, after_classes = legacy.inventory(raw), legacy.inventory(corrected)
    require(set(before_classes) == set(after_classes), 'class population changed')
    require({k: v for k, v in before_classes.items() if k != patch['path']} ==
            {k: v for k, v in after_classes.items() if k != patch['path']}, 'unreviewed recursive class change')
    require(after_classes[patch['path']] == sha(revised), 'patched class inventory mismatch')
    row = {'input': artifact['input'], 'output': artifact['output'], 'input_sha256': sha(raw),
           'output_sha256': sha(corrected), 'output_sha1': sha(corrected, 'sha1'), 'annotation_patch': correction,
           'class_count': len(before_classes), 'before_class_inventory_sha256': sha(legacy.encode_json(before_classes)),
           'after_class_inventory_sha256': sha(legacy.encode_json(after_classes)),
           'license': artifact['license'], 'redistribution': artifact['redistribution']}
    receipt = {'schema': 'bc.upstream_startup_runtime_receipt.v1', 'revision': spec['revision'],
               'spec_sha256': sha(read_file(spec_path, legacy.MAX_JSON)), 'builder_sha256': sha(read_file(__file__)),
               'legacy_builder_sha256': LEGACY_SHA, 'python': sys.version.split()[0], 'zlib': zlib.ZLIB_RUNTIME_VERSION,
               'citadel_abi': {'input': citadel['input'], 'sha256': citadel['sha256'], 'class_sha256': sha(abi),
                               'field_names': fields, 'dataMap_present': False}, 'artifacts': [row],
               'runtime_acceptance': 'NOT_EVALUATED', 'publication': 'NOT_AUTHORIZED_BY_THIS_RECEIPT'}
    output.mkdir()
    for name, body in ((artifact['output'], corrected), ('receipt.json', legacy.encode_json(receipt))):
        fd = os.open(output / name, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o644)
        with os.fdopen(fd, 'wb') as stream: stream.write(body); stream.flush(); os.fsync(stream.fileno())
    return receipt


def fetch_originals(directory, spec_path=SPEC):
    from urllib.request import Request, urlopen
    from urllib.parse import urlparse
    directory = legacy.regular_path(directory)
    require(not directory.exists() and directory.parent.is_dir(), 'fresh original directory required')
    spec = load_spec(spec_path); directory.mkdir()
    for item in (spec['artifact'], spec['citadel']):
        with urlopen(Request(item['url'], headers={'User-Agent': 'BetterContent-local-pinned-startup/1'}), timeout=20) as response:
            parsed = urlparse(response.geturl())
            require(parsed.scheme == 'https' and parsed.hostname in ('mediafilez.forgecdn.net', 'media.forgecdn.net'), 'unreviewed redirect')
            data = response.read(legacy.MAX_ARCHIVE + 1)
        legacy.digest_matches(data, item['sha256']); legacy.digest_matches(data, item['sha1'], 'sha1')
        fd = os.open(directory / item['input'], os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o644)
        with os.fdopen(fd, 'wb') as stream: stream.write(data); stream.flush(); os.fsync(stream.fileno())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--inputs', type=Path); parser.add_argument('--output', type=Path)
    parser.add_argument('--fetch-originals', type=Path)
    args = parser.parse_args()
    if args.fetch_originals:
        if args.inputs or args.output: parser.error('fetch is separate from offline build')
        fetch_originals(args.fetch_originals)
    else:
        if not args.inputs or not args.output: parser.error('--inputs and --output required')
        print(json.dumps(build(args.inputs, args.output), sort_keys=True))

if __name__ == '__main__': main()
