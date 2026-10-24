#!/usr/bin/env python3
"""Independent, exact two-artifact local compatibility corrections.

The earlier five-artifact resource-only builder/spec remain untouched. Here one
reviewed Java interface changes precisely three method access words; ALL remaining
class bytes (including every Code/annotation/descriptor) remain byte-identical.
Never publishes, edits original caches, or substitutes the global Mixin bootstrap.
"""
from __future__ import annotations
import argparse
import copy
import os
from pathlib import Path
import struct
import sys
import zipfile
from urllib.parse import quote, urlparse
from urllib.request import Request, urlopen
import zlib
import build_upstream_runtime_overrides as base

ROOT = Path(__file__).resolve().parents[1]
SPEC = ROOT / 'src/upstreamRuntime/interface-overrides-v1.json'
CLASS = 'org/valkyrienskies/mod/mixin/feature/sculk/MixinVibrationSystemTicker.class'
ORIGINAL_CLASS = '399ca37359fbbf851eb54f5e3524c59a25751998cae0c9f534bb68a45f56659f'
USER_DESC = '(Lnet/minecraft/world/level/gameevent/vibrations/VibrationSystem$User;Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/level/gameevent/PositionSource;'
METHODS = {'destWorldPos': '(Lnet/minecraft/world/level/gameevent/vibrations/VibrationInfo;Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/phys/Vec3;',
           'destSourcePos': USER_DESC, 'destReloadSourcePos': USER_DESC}
require, sha = base.require, base.sha


def method_access_offsets(data):
    """Read the complete bounded class structure; never rewrite a pool/attribute."""
    require(len(data) <= base.MAX_ENTRY, 'class size limit')
    pos = 0
    def take(n):
        nonlocal pos
        require(n >= 0 and pos + n <= len(data), 'truncated class structure')
        body = data[pos:pos+n]; pos += n; return body
    def u2(): return struct.unpack('>H', take(2))[0]
    def u4(): return struct.unpack('>I', take(4))[0]
    require(take(4) == b'\xca\xfe\xba\xbe', 'not a Java class')
    take(4)
    count = u2(); require(count > 1, 'empty constant pool')
    pool = {}; index = 1
    while index < count:
        tag = take(1)[0]
        if tag == 1: pool[index] = take(u2())
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): take(4)
        elif tag in (5, 6): take(8); index += 1
        elif tag in (7, 8, 16, 19, 20): take(2)
        elif tag == 15: take(3)
        else: raise base.Rejected('unsupported constant-pool tag')
        index += 1
    require(index == count, 'invalid wide constant-pool slot')
    access = u2(); require(access & 0x0200, 'reviewed class must remain an interface')
    take(4); take(u2() * 2)
    def attributes():
        for _ in range(u2()):
            require(u2() in pool, 'attribute name is not UTF8')
            take(u4())
    for _ in range(u2()):
        take(6); attributes()
    methods = {}
    for _ in range(u2()):
        offset = pos; flags, name, desc = u2(), u2(), u2()
        require(name in pool and desc in pool, 'method name/descriptor is not UTF8')
        key = (pool[name], pool[desc])
        require(key not in methods, 'duplicate method signature')
        methods[key] = (offset, flags)
        attributes()
    attributes(); require(pos == len(data), 'class trailer/extent changed')
    return methods


def promote_static_handlers(data):
    base.digest_matches(data, ORIGINAL_CLASS)
    methods = method_access_offsets(data)
    revised = bytearray(data); edits = []
    expected = {(name.encode('ascii'), desc.encode('ascii')) for name, desc in METHODS.items()}
    require(set(methods) == expected, 'exact three handler signatures required')
    for name, desc in METHODS.items():
        offset, flags = methods[(name.encode('ascii'), desc.encode('ascii'))]
        require(flags == 0x000a, 'handler must be exactly PRIVATE|STATIC')
        struct.pack_into('>H', revised, offset, 0x0009)
        edits.append({'method': name, 'descriptor': desc, 'offset': offset, 'before': 10, 'after': 9})
    after = method_access_offsets(bytes(revised))
    require(all(after[key] == (offset, 9) for key, (offset, _) in methods.items()), 'static access proof failed')
    # Bytes outside these exact access words are untouched by construction.
    return bytes(revised), edits


def relocate(data, patch):
    base.digest_matches(data, patch['sha256'])
    old = base.parse_json(data)
    require(old == patch['before'] and isinstance(old, dict), 'exact old config required')
    moved = patch['move_to_client']
    require(isinstance(moved, list) and len(moved) == 1 and isinstance(moved[0], str), 'one exact side relocation')
    require(isinstance(old.get('mixins'), list) and old['mixins'].count(moved[0]) == 1, 'common mixin occurrence')
    require('client' not in old or isinstance(old['client'], list), 'client list type')
    require(moved[0] not in old.get('client', []), 'already client listed')
    new = copy.deepcopy(old)
    new['mixins'].remove(moved[0]); new.setdefault('client', []).append(moved[0])
    return base.encode_json(new)


def load_spec(path=SPEC):
    spec = base.parse_json(base.read_file(path, base.MAX_JSON))
    require(spec.get('schema') == 'bc.upstream_interface_runtime_overrides.v1' and
            spec.get('revision') == 'bc-interface.1', 'unsupported independent spec')
    artifacts = spec.get('artifacts')
    require(isinstance(artifacts, list) and len(artifacts) == 2, 'exactly two reviewed artifacts')
    expected = {'epic x parcool-forge-20.12.0.1-1.20.1.jar', 'valkyrienskies-120-2.4.11.jar'}
    require({a['input'] for a in artifacts} == expected and len({a['output'] for a in artifacts}) == 2, 'artifact membership')
    for a in artifacts:
        for field in ('input', 'output'):
            base.safe_name(a[field]); require('/' not in a[field], 'artifact basename required')
        require(a['output'] == a['input'][:-4] + '-bc-interface.1.jar', 'separate corrected filename')
        require(len(a['patches']) == 1 and a['url'].startswith('https://mediafilez.forgecdn.net/files/'), 'exact JSON/origin scope')
        p = a['patches'][0]
        require(set(p) == {'path', 'sha256', 'before', 'move_to_client'}, 'JSON patch shape')
        if a['input'].startswith('valkyrienskies'):
            require(p['path'] == 'valkyrienskies-common.mixins.json' and p['move_to_client'] ==
                    ['mod_compat.create.blockentity.MixinChainConveyorInteractionHandler'], 'VS side scope')
            c = a['class_access']
            require(c == {'path': CLASS, 'before_sha256': ORIGINAL_CLASS, 'methods': METHODS,
                          'before': 10, 'after': 9}, 'exact STATIC visibility scope')
        else:
            require(p['path'] == 'mixins.epicparcool.json' and p['move_to_client'] == ['ParCoolMixinAnimation'] and
                    'class_access' not in a, 'Epic accessor-only scope')
    return spec


def build(inputs, output, spec_path=SPEC):
    inputs, output = base.regular_path(inputs), base.regular_path(output)
    require(inputs.is_dir() and not output.exists() and output.parent.is_dir(), 'fresh output / existing inputs required')
    require(inputs != output and inputs not in output.parents and output not in inputs.parents, 'disjoint trees required')
    spec = load_spec(spec_path); prepared = []; rows = []
    for a in spec['artifacts']:
        original = base.read_file(inputs / a['input'])
        base.digest_matches(original, a['sha256']); base.digest_matches(original, a['sha1'], 'sha1')
        _, entries = base.archive(original)
        patch = a['patches'][0]; path = patch['path']
        require(path in entries, 'missing config')
        changed = {path: relocate(entries[path], patch)}
        access = []
        if 'class_access' in a:
            require(CLASS in entries, 'missing exact interface')
            revised, edits = promote_static_handlers(entries[CLASS]); changed[CLASS] = revised
            access = [{'path': CLASS, 'before_sha256': sha(entries[CLASS]), 'after_sha256': sha(revised),
                       'edits': edits, 'all_other_class_bytes_invariant': True}]
        revised = base.rewrite_zip(original, changed)
        before_classes = base.inventory(original); after_classes = base.inventory(revised)
        expected_classes = dict(before_classes)
        if access: expected_classes[CLASS] = access[0]['after_sha256']
        require(after_classes == expected_classes, 'class inventory exceeds exact three access words')
        rows.append({'input': a['input'], 'output': a['output'], 'input_sha256': sha(original),
                     'output_sha256': sha(revised), 'output_sha1': sha(revised, 'sha1'),
                     'resources': [{'path': path, 'before_sha256': sha(entries[path]), 'after_sha256': sha(changed[path])}],
                     'class_access_patches': access, 'class_bytes_invariant': not access,
                     'class_count': len(before_classes),
                     'before_class_inventory_sha256': sha(base.encode_json(before_classes)),
                     'after_class_inventory_sha256': sha(base.encode_json(after_classes)),
                     'license': a['license'], 'redistribution': a['redistribution']})
        prepared.append((a['output'], revised))
    receipt = {'schema': 'bc.upstream_interface_runtime_receipt.v1', 'revision': spec['revision'],
               'spec_sha256': sha(base.read_file(spec_path)), 'builder_sha256': sha(base.read_file(__file__)),
               'legacy_builder_sha256': sha(base.read_file(base.__file__)),
               'python': sys.version.split()[0], 'zlib': zlib.ZLIB_RUNTIME_VERSION,
               'artifacts': rows, 'runtime_acceptance': 'NOT_EVALUATED',
               'publication': 'NOT_AUTHORIZED_BY_THIS_RECEIPT'}
    output.mkdir()
    for name, body in prepared + [('receipt.json', base.encode_json(receipt))]:
        fd = os.open(output / name, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o644)
        with os.fdopen(fd, 'wb') as stream:
            stream.write(body); stream.flush(); os.fsync(stream.fileno())
    return receipt


def fetch_originals(directory, spec_path=SPEC):
    directory = base.regular_path(directory)
    require(not directory.exists() and directory.parent.is_dir(), 'fresh fetch directory required')
    spec = load_spec(spec_path); directory.mkdir()
    for a in spec['artifacts']:
        request = Request(a['url'], headers={'User-Agent': 'BetterContent-local-interface-qualification/1'})
        with urlopen(request, timeout=120) as response:
            final = urlparse(response.geturl())
            require(final.scheme == 'https' and final.hostname in ('mediafilez.forgecdn.net', 'media.forgecdn.net', 'edge.forgecdn.net'), 'unreviewed redirect')
            data = response.read(base.MAX_ARCHIVE + 1)
        require(len(data) <= base.MAX_ARCHIVE, 'download size')
        base.digest_matches(data, a['sha256']); base.digest_matches(data, a['sha1'], 'sha1')
        with (directory / a['input']).open('xb') as stream: stream.write(data)
    return directory


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--inputs', type=Path); parser.add_argument('--output', type=Path)
    parser.add_argument('--fetch-originals', type=Path); parser.add_argument('--spec', type=Path, default=SPEC)
    args = parser.parse_args()
    if args.fetch_originals:
        require(args.inputs is None and args.output is None, 'fetch is a separate operation')
        fetch_originals(args.fetch_originals, args.spec)
    else:
        require(args.inputs is not None and args.output is not None, 'inputs/output required')
        build(args.inputs, args.output, args.spec)
