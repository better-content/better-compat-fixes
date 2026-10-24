#!/usr/bin/env python3
"""Pinned, offline, resource-only local runtime corrections. Never publishes artifacts.

Originals are explicit files in --inputs, never discovered or modified. See
src/upstreamRuntime/README.md for conditions, legal blockers and qualification limits.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import io
import json
import os
from pathlib import Path
import re
import stat
import struct
import sys
import zipfile
import zlib
from urllib.parse import urlparse
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
SPEC = ROOT / 'src/upstreamRuntime/overrides-v2.json'
MAX_ARCHIVE = 256 * 1024 * 1024
MAX_ENTRY = 128 * 1024 * 1024
MAX_EXPANDED = 1024 * 1024 * 1024
MAX_ENTRIES = 100000
MAX_JSON = 2 * 1024 * 1024
MAX_DEPTH = 4


class Rejected(ValueError):
    """Input is outside the narrow reviewed contract."""


def require(condition, message):
    if not condition:
        raise Rejected(message)


def sha(data, algorithm='sha256'):
    return hashlib.new(algorithm, data).hexdigest()


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, 'duplicate JSON key: ' + key)
        result[key] = value
    return result


def parse_json(data):
    require(len(data) <= MAX_JSON, 'JSON size limit')
    try:
        return json.loads(data, object_pairs_hook=unique_object,
                          parse_constant=lambda x: (_ for _ in ()).throw(Rejected('nonfinite JSON')))
    except (UnicodeError, json.JSONDecodeError, RecursionError) as exc:
        raise Rejected('invalid bounded JSON') from exc


def encode_json(value):
    return (json.dumps(value, indent=2, ensure_ascii=True, allow_nan=False) + '\n').encode()


def safe_name(name):
    require(isinstance(name, str) and name and '\\' not in name and '\x00' not in name,
            'invalid archive/path name')
    parts = name.rstrip('/').split('/')
    require(all(p not in ('', '.', '..') for p in parts), 'unsafe archive/path name: ' + name)
    return name


def regular_path(path):
    path = Path(os.path.abspath(path))
    for part in [*reversed(path.parents), path]:
        require(not part.is_symlink(), 'symlink path: ' + str(part))
    return path


def read_file(path, limit=MAX_ARCHIVE):
    path = regular_path(path)
    fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW)
    try:
        before = os.fstat(fd)
        require(stat.S_ISREG(before.st_mode) and before.st_size <= limit, 'file type/size limit')
        with os.fdopen(fd, 'rb', closefd=False) as stream:
            data = stream.read(limit + 1)
        after = os.fstat(fd)
        require(len(data) <= limit and (before.st_ino, before.st_size, before.st_mtime_ns) ==
                (after.st_ino, after.st_size, after.st_mtime_ns), 'input changed while reading')
        return data
    finally:
        os.close(fd)


def digest_matches(data, expected, algorithm='sha256'):
    length = 64 if algorithm == 'sha256' else 40
    require(isinstance(expected, str) and re.fullmatch('[0-9a-f]{%d}' % length, expected),
            'malformed expected digest')
    require(sha(data, algorithm) == expected, algorithm + ' mismatch')


def archive(data):
    """Validate a bounded plain ZIP; return ordered entries and their actual bytes."""
    require(len(data) <= MAX_ARCHIVE, 'archive size limit')
    try:
        z = zipfile.ZipFile(io.BytesIO(data))
        infos = z.infolist()
        require(len(infos) <= MAX_ENTRIES, 'archive entry count limit')
        require(len({i.filename for i in infos}) == len(infos), 'duplicate archive name')
        require(sum(i.file_size for i in infos) <= MAX_EXPANDED, 'expanded archive limit')
        entries = {}
        for info in infos:
            safe_name(info.filename)
            require(info.orig_filename == info.filename, 'truncated/NUL archive name')
            require(info.file_size <= MAX_ENTRY and not info.flag_bits & 1, 'entry size/encryption')
            require(info.compress_type in (zipfile.ZIP_STORED, zipfile.ZIP_DEFLATED),
                    'unsupported compression')
            require(not stat.S_ISLNK(info.external_attr >> 16), 'archive symlink')
            upper = info.filename.upper()
            require(not (upper.startswith('META-INF/') and
                         (upper.endswith(('.SF', '.RSA', '.DSA', '.EC')) or
                          upper.split('/')[-1].startswith('SIG-'))), 'signed archive requires review')
            body = z.read(info)  # CRC checked by zipfile, bounded above before allocation.
            if upper == 'META-INF/MANIFEST.MF':
                require(not re.search(rb'(?im)^[^\r\n:]*-Digest(?:-[^:]+)?:', body),
                        'signed manifest requires review')
            entries[info.filename] = body
        return infos, entries
    except (zipfile.BadZipFile, NotImplementedError, RuntimeError, zlib.error) as exc:
        raise Rejected('invalid/unsupported archive') from exc


def check_extra(extra):
    cursor = 0
    while cursor < len(extra):
        require(cursor + 4 <= len(extra), 'truncated ZIP extra')
        tag, size = struct.unpack_from('<HH', extra, cursor)
        cursor += 4
        require(tag != 1 and cursor + size <= len(extra), 'ZIP64/invalid extra not supported')
        cursor += size


def rewrite_zip(data, changes):
    """Copy untouched compressed local records exactly; preserve all ZIP metadata.

    Only CRC/sizes/compressed payloads for changed entries and central offsets/EOCD
    offsets change. Existing timestamps, flags, extras, names, comments and ordering
    are retained, including the original data-descriptor convention. ZIP64, padding,
    multi-disk and self-extracting layouts fail closed instead of being normalized.
    """
    infos, old = archive(data)
    require(set(changes) <= set(old), 'missing patch entry')
    end = data.rfind(b'PK\x05\x06', max(0, len(data) - 65557))
    require(end >= 0 and end + 22 <= len(data), 'missing EOCD')
    sig, disk, cddisk, countdisk, count, cdsize, cdoffset, commentlen = struct.unpack_from('<4s4H2IH', data, end)
    require(disk == cddisk == 0 and countdisk == count == len(infos) and count != 65535,
            'multi-disk/ZIP64/count mismatch')
    require(end + 22 + commentlen == len(data) and cdoffset + cdsize == end, 'unsupported ZIP trailer')
    require(infos and min(i.header_offset for i in infos) == 0, 'ZIP preamble/empty archive')
    central = []
    pos = cdoffset
    for info in infos:
        require(data[pos:pos + 4] == b'PK\x01\x02' and pos + 46 <= end, 'invalid central header')
        nlen, xlen, clen, diskno = struct.unpack_from('<4H', data, pos + 28)
        size = 46 + nlen + xlen + clen
        require(pos + size <= end and diskno == 0, 'invalid central entry')
        record = bytearray(data[pos:pos + size])
        check_extra(record[46 + nlen:46 + nlen + xlen])
        require(struct.unpack_from('<I', record, 42)[0] == info.header_offset, 'central offset mismatch')
        central.append((info, record))
        pos += size
    require(pos == end, 'central directory extent mismatch')
    result = bytearray()
    offsets, crc_sizes = {}, {}
    by_offset = sorted(infos, key=lambda item: item.header_offset)
    for index, info in enumerate(by_offset):
        start = info.header_offset
        stop = by_offset[index + 1].header_offset if index + 1 < len(by_offset) else cdoffset
        require(data[start:start + 4] == b'PK\x03\x04', 'invalid local header')
        flags, method = struct.unpack_from('<HH', data, start + 6)
        nlen, xlen = struct.unpack_from('<HH', data, start + 26)
        header_end = start + 30 + nlen + xlen
        require(header_end + info.compress_size <= stop and flags == info.flag_bits and
                method == info.compress_type, 'local/central mismatch')
        check_extra(data[start + 30 + nlen:header_end])
        descriptor = data[header_end + info.compress_size:stop]
        if flags & 8:
            expected = struct.pack('<III', info.CRC, info.compress_size, info.file_size)
            require(descriptor in (expected, b'PK\x07\x08' + expected), 'unsupported descriptor')
        else:
            require(not descriptor and struct.unpack_from('<III', data, start + 14) ==
                    (info.CRC, info.compress_size, info.file_size), 'local size/extent mismatch')
        offsets[info.filename] = len(result)
        if info.filename not in changes:
            result.extend(data[start:stop])
            continue
        body = changes[info.filename]
        require(isinstance(body, bytes) and len(body) <= MAX_ENTRY, 'modified entry size/type')
        if method == zipfile.ZIP_DEFLATED:
            compressor = zlib.compressobj(9, zlib.DEFLATED, -15)
            compressed = compressor.compress(body) + compressor.flush()
        else:
            compressed = body
        values = (zlib.crc32(body) & 0xffffffff, len(compressed), len(body))
        crc_sizes[info.filename] = values
        header = bytearray(data[start:header_end])
        if not flags & 8:
            struct.pack_into('<III', header, 14, *values)
        else:
            # Keep local placeholder CRC/size bytes unchanged; descriptor is authoritative.
            descriptor = (b'PK\x07\x08' if len(descriptor) == 16 else b'') + struct.pack('<III', *values)
        result.extend(header)
        result.extend(compressed)
        result.extend(descriptor)
    new_cd = len(result)
    for info, record in central:
        if info.filename in crc_sizes:
            struct.pack_into('<III', record, 16, *crc_sizes[info.filename])
        struct.pack_into('<I', record, 42, offsets[info.filename])
        result.extend(record)
    trailer = bytearray(data[end:])
    struct.pack_into('<II', trailer, 12, len(result) - new_cd, new_cd)
    result.extend(trailer)
    require(len(result) <= MAX_ARCHIVE, 'output archive size limit')
    after_infos, after = archive(bytes(result))
    require([i.filename for i in after_infos] == [i.filename for i in infos], 'entry order changed')
    require(after == {name: changes.get(name, body) for name, body in old.items()}, 'entry invariance failure')
    return bytes(result)


def patch_json(data, patch):
    digest_matches(data, patch['sha256'])
    before = parse_json(data)
    require(isinstance(before, dict), 'resource must be JSON object')
    if 'before' in patch:
        require(before == patch['before'], 'old JSON structure mismatch')
    else:
        require('remove_keys' in patch and isinstance(patch.get('before_fields'), dict), 'missing old structure')
        require(all(key in before and before[key] == value for key, value in patch['before_fields'].items()),
                'old field structure mismatch')
    result = copy.deepcopy(before)
    operations = set(patch) & {'add_min_version', 'move_to_client', 'remove_keys'}
    require(len(operations) == 1, 'exactly one reviewed operation required')
    if 'add_min_version' in patch:
        require('minVersion' not in result and patch['add_min_version'] == '0.8.5', 'unexpected minVersion')
        result['minVersion'] = '0.8.5'
    elif 'move_to_client' in patch:
        moved = patch['move_to_client']
        require(isinstance(moved, list) and moved and all(isinstance(x, str) for x in moved) and
                len(set(moved)) == len(moved), 'invalid relocation list')
        require(isinstance(result.get('mixins'), list) and isinstance(result.get('client'), list),
                'invalid mixin/client list type')
        require(all(result['mixins'].count(x) == 1 and x not in result['client'] for x in moved),
                'unexpected relocation old values')
        result['mixins'] = [x for x in result['mixins'] if x not in moved]
        result['client'].extend(moved)
    else:
        keys = patch['remove_keys']
        require(isinstance(keys, list) and len(keys) == len(set(keys)) == 4 and set(keys) == set(patch['before_fields']),
                'unexpected coremod removals')
        for key in keys:
            del result[key]
    return encode_json(result)


def transform(data, patches, prefix=()):
    require(len(prefix) <= MAX_DEPTH and patches, 'patch nesting/count limit')
    _, entries = archive(data)
    groups = {}
    for patch in patches:
        path = patch['path']
        require(isinstance(path, list) and 1 <= len(path) <= MAX_DEPTH and
                all(isinstance(x, str) for x in path), 'invalid patch path')
        for name in path:
            safe_name(name)
        require(len(patch.get('parents', [])) == len(path) - 1, 'parent hash count')
        groups.setdefault(path[0], []).append(patch)
    changes, resources = {}, []
    for name, group in groups.items():
        require(name in entries, 'missing patch entry: ' + name)
        original = entries[name]
        leaves = [p for p in group if len(p['path']) == 1]
        if leaves:
            require(len(group) == len(leaves) == 1 and name.endswith('.json'), 'duplicate/conflicting patch')
            revised = patch_json(original, group[0])
            resources.append({'path': list(prefix + (name,)), 'before_sha256': sha(original),
                              'after_sha256': sha(revised)})
        else:
            require(name.endswith('.jar'), 'only nested JAR paths supported')
            children = []
            for patch in group:
                digest_matches(original, patch['parents'][0])
                child = dict(patch, path=patch['path'][1:], parents=patch['parents'][1:])
                children.append(child)
            revised, child_resources = transform(original, children, prefix + (name,))
            resources.extend(child_resources)
        changes[name] = revised
    return rewrite_zip(data, changes), resources


def inventory(data, prefix=(), budget=None):
    """Recursive class fingerprints, checking signatures even in untouched nested JARs."""
    budget = [0, 0] if budget is None else budget
    require(len(prefix) <= MAX_DEPTH, 'inventory nesting limit')
    infos, entries = archive(data)
    budget[0] += len(infos)
    budget[1] += sum(len(b) for b in entries.values())
    require(budget[0] <= MAX_ENTRIES and budget[1] <= MAX_EXPANDED, 'recursive inventory budget')
    classes = {}
    for name, body in entries.items():
        path = '!'.join(prefix + (name,))
        if name.endswith('.class'):
            classes[path] = sha(body)
        elif name.endswith('.jar'):
            classes.update(inventory(body, prefix + (name,), budget))
    return classes


def check_compat(data, contract):
    _, entries = archive(data)
    config = parse_json(entries[contract['config']])
    require(config.get('plugin') == contract['plugin'] and isinstance(config.get('package'), str),
            'Compat production plugin missing')
    require(config.get('required') is contract['config_required'] and isinstance(config.get('mixins'), list),
            'Compat production config structure changed')
    plugin_path = contract['plugin'].replace('.', '/') + '.class'
    require(plugin_path in entries, 'Compat production plugin class missing')
    digest_matches(entries[plugin_path], contract['plugin_sha256'])
    for name, digest in contract['classes'].items():
        require(config['mixins'].count(name) == 1, 'Compat pollution replacement not registered')
        path = config['package'].replace('.', '/') + '/' + name.replace('.', '/') + '.class'
        require(path in entries, 'Compat replacement class missing')
        digest_matches(entries[path], digest)
    return {'sha256': sha(data), 'classes': contract['classes'],
            'plugin_sha256': contract['plugin_sha256'], 'config_required': contract['config_required'],
            'condition': contract['condition'], 'runtime_behavior': 'NOT_EVALUATED'}


def load_spec(path=SPEC):
    spec = parse_json(read_file(path, MAX_JSON))
    require(isinstance(spec, dict) and spec.get('schema') == 'bc.upstream_runtime_overrides.v2' and
            spec.get('revision') == 'bc-runtime.2', 'unsupported spec')
    artifacts = spec.get('artifacts')
    require(isinstance(artifacts, list) and len(artifacts) == 5, 'expected five reviewed artifacts')
    for field in ('input', 'output'):
        require(len({a[field] for a in artifacts}) == len(artifacts), 'duplicate artifact name')
        for a in artifacts:
            safe_name(a[field])
            require('/' not in a[field], 'artifact must be basename')
    for a in artifacts:
        suffix = '-bc-runtime.2.jar' if a['input'].startswith('AdPother-') else '-bc-runtime.1.jar'
        require(a['input'] != a['output'] and a['output'] == a['input'][:-4] + suffix, 'output identity')
        require(isinstance(a['patches'], list) and 1 <= len(a['patches']) <= 6, 'patch count limit')
        require(a['url'].startswith('https://mediafilez.forgecdn.net/files/'), 'unreviewed input origin')
    return spec


def build(inputs, output, compat_jar, spec_path=SPEC):
    inputs, output = regular_path(inputs), regular_path(output)
    require(inputs.is_dir() and not output.exists(), 'inputs directory / fresh output required')
    require(inputs not in output.parents and output not in inputs.parents and inputs != output,
            'input/output trees must be disjoint')
    require(output.parent.is_dir(), 'output parent must already exist')
    spec_data = read_file(spec_path, MAX_JSON)
    spec = load_spec(spec_path)
    compat = check_compat(read_file(compat_jar), spec['required_compat'])
    prepared, receipts = [], []
    for artifact in spec['artifacts']:
        original = read_file(inputs / artifact['input'])
        digest_matches(original, artifact['sha256'])
        digest_matches(original, artifact['sha1'], 'sha1')
        classes = inventory(original)
        revised, resources = transform(original, artifact['patches'])
        require(classes == inventory(revised), 'class-byte invariance failure')
        prepared.append((artifact['output'], revised))
        receipts.append({'input': artifact['input'], 'output': artifact['output'],
                         'input_sha256': sha(original), 'output_sha256': sha(revised),
                         'output_sha1': sha(revised, 'sha1'), 'resources': resources,
                         'class_bytes_invariant': True, 'class_count': len(classes),
                         'class_inventory_sha256': sha(encode_json(classes)),
                         'license': artifact['license'], 'redistribution': artifact['redistribution']})
    receipt = {'schema': 'bc.upstream_runtime_receipt.v1', 'revision': spec['revision'],
               'spec_sha256': sha(spec_data), 'builder_sha256': sha(read_file(__file__)),
               'python': sys.version.split()[0], 'zlib': zlib.ZLIB_RUNTIME_VERSION,
               'compat': compat, 'artifacts': receipts, 'runtime_acceptance': 'NOT_EVALUATED',
               'publication': 'NOT_AUTHORIZED_BY_THIS_RECEIPT'}
    # Validate everything before creating a fresh destination. Failed writes remain retained.
    output.mkdir()
    for name, body in prepared + [('receipt.json', encode_json(receipt))]:
        fd = os.open(output / name, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o644)
        with os.fdopen(fd, 'wb') as stream:
            stream.write(body)
            stream.flush()
            os.fsync(stream.fileno())
    return receipt


def fetch_originals(directory, spec_path=SPEC):
    """Explicit opt-in downloads into a NEW directory, separate from any current cache."""
    directory = regular_path(directory)
    require(not directory.exists() and directory.parent.is_dir(), 'fresh fetch directory required')
    spec = load_spec(spec_path)
    directory.mkdir()
    records = []
    for artifact in spec['artifacts']:
        request = Request(artifact['url'], headers={'User-Agent': 'BetterContent-local-pinned-input/1'})
        with urlopen(request, timeout=20) as response:
            parsed = urlparse(response.geturl())
            require(parsed.scheme == 'https' and parsed.hostname in
                    ('mediafilez.forgecdn.net', 'media.forgecdn.net'), 'unreviewed download redirect')
            body = response.read(MAX_ARCHIVE + 1)
        require(len(body) <= MAX_ARCHIVE, 'download size limit')
        digest_matches(body, artifact['sha256'])
        digest_matches(body, artifact['sha1'], 'sha1')
        fd = os.open(directory / artifact['input'], os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o644)
        with os.fdopen(fd, 'wb') as stream:
            stream.write(body)
            stream.flush()
            os.fsync(stream.fileno())
        records.append({'input': artifact['input'], 'sha256': sha(body), 'url': artifact['url']})
    return {'schema': 'bc.upstream_runtime_fetch.v1', 'inputs': records}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--inputs', type=Path, help='explicit original archive directory (offline)')
    parser.add_argument('--output', type=Path, help='fresh disjoint directory; never overwritten')
    parser.add_argument('--compat-jar', type=Path, help='required production pollution replacement provider')
    parser.add_argument('--fetch-originals', type=Path, help='separate opt-in: fetch pinned originals to NEW directory only')
    args = parser.parse_args()
    if args.fetch_originals:
        if any((args.inputs, args.output, args.compat_jar)):
            parser.error('--fetch-originals cannot be combined with build arguments')
    elif not all((args.inputs, args.output, args.compat_jar)):
        parser.error('--inputs, --output and --compat-jar are required for an offline build')
    try:
        receipt = (fetch_originals(args.fetch_originals) if args.fetch_originals else
                   build(args.inputs, args.output, args.compat_jar))
    except (Rejected, OSError, KeyError, TypeError, struct.error) as exc:
        parser.exit(1, 'REJECTED: ' + str(exc) + '\n')
    print(json.dumps(receipt, sort_keys=True))


if __name__ == '__main__':
    main()
