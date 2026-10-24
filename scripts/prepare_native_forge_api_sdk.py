#!/usr/bin/env python3
"""Pinned local-only native Forge FFAPI SDK; no Gradle resolution/game class loading.

Recursively runs the cached actual Forge SRG_TO_MCP renamer, then reassembles one
nonshipping root. Nested modules are compile SDKs only, never top-level runtime copies.
Originals, mapping/tool inputs, all attempts and logs are retained. No production recipe edits.
"""
import argparse
import importlib.util
import io
import os
from pathlib import Path
import subprocess
import sys
import tempfile
from urllib.request import HTTPRedirectHandler, Request, build_opener
from urllib.parse import urlparse
import zipfile

MODULE = Path(__file__).with_name('build_upstream_runtime_overrides_v2.py')
loader = importlib.util.spec_from_file_location('native_sdk_v2', MODULE)
v2 = importlib.util.module_from_spec(loader)
loader.loader.exec_module(v2)
FFAPI_RAW = '9b140af437581d32e5ee17d20f8fe882000e6a34cba85c152ae5bccc9570cc03'
FFAPI_FINAL = '429b25aaa42c88393cafca04c0e2b9ecfcfc277e14fc3fb86045877144541742'
MOONLIGHT_RAW = '007c05ae799417ca7aa26669c5d97be9dface5b6fc27aca4ca8031cafe4ddd6e'
MAPPING = '84e2278aeeabd496194dbe2fa08031be58bad277d7e3aee036b79d9222d8cbc7'
TOOL = '3de0f0899c060b6f5aa79d4d9d79063ca9029cfefa19e792cb222782f754184d'
OUTPUTS = {
    'ffapi-native': '447b9e285717e66e95924e190a13f5a2079b969af70ed4c6577d40cad9423351',
    'moonlight-native': 'ec130f96cb65ce97d600880127322656021bd5c6e60a976de9eb9403e5f3d5a3',
    'base': 'ccfa049091ce36c8ba4fab642c5c7291f4eb7729afed65a43ec1bebd999550a9',
    'interaction': 'bc2e6f06e8791cc98fce43408427d6a5125645c75abb153abdcfa009c3ba458c',
    'resource': '254113f0d62b8b953abe3a20f053060a61ad21c480304442b80275a2389e82de',
}
MODULES = {
    'base': 'META-INF/jarjar/fabric-api-base-0.4.32+ef105b4977.jar',
    'interaction': 'META-INF/jarjar/fabric-events-interaction-v0-0.6.5+0f26153f77.jar',
    'resource': 'META-INF/jarjar/fabric-resource-loader-v0-0.11.12+4b4151c077.jar',
}


def write_new(path, data):
    path = v2.regular_path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    fd = os.open(path, os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW | os.O_WRONLY, 0o644)
    with os.fdopen(fd, 'wb') as stream:
        stream.write(data)
        stream.flush()
        os.fsync(stream.fileno())


def inventory(data, prefix=''):
    _, entries = v2.archive(data)
    result = {prefix + name: v2.sha(body) for name, body in entries.items()}
    for name, body in entries.items():
        if name.endswith('.jar'):
            result.update(inventory(body, prefix + name + '!'))
    return result


def rename(data, directory, tool, mapping, label, depth=0):
    v2.require(depth <= 4, 'nested SDK depth')
    directory.mkdir()
    input_path, output_path = directory / 'input.jar', directory / 'mapped.jar'
    write_new(input_path, data)
    args = ['java', '-jar', str(tool), '--task', 'SRG_TO_MCP', '--input', str(input_path),
            '--output', str(output_path), '--mcp', str(mapping), '--strip-signatures']
    process = subprocess.run(args, capture_output=True, timeout=60)
    write_new(directory / 'renamer.log', process.stdout + process.stderr)
    v2.require(process.returncode == 0, 'actual Forge SDK renamer failed: ' + label)
    mapped = v2.read_file(output_path)
    _, old = v2.archive(data)
    changes = {}
    for number, (name, nested) in enumerate(old.items()):
        if name.endswith('.jar'):
            changes[name] = rename(nested, directory / ('nested-%03d' % number), tool, mapping, label + '!' + name, depth + 1)
    if changes:
        mapped = v2.rewrite_zip(mapped, changes)
    write_new(directory / 'complete.jar', mapped)
    return mapped


def download(url, destination):
    allowed = {'maven.minecraftforge.net', 'mediafilez.forgecdn.net', 'media.forgecdn.net'}
    def checked(value):
        parts = urlparse(value)
        v2.require(parts.scheme == 'https' and parts.hostname in allowed and parts.port in (None, 443)
                   and parts.username is None and parts.password is None, 'unreviewed SDK download/redirect origin')
    class Redirects(HTTPRedirectHandler):
        def redirect_request(self, request, fp, code, message, headers, newurl):
            checked(newurl)
            return super().redirect_request(request, fp, code, message, headers, newurl)
    checked(url)
    # Stream into a fresh evidence file: timeout/digest/redirect failures retain received bytes.
    write_new(destination, b'')
    with open(destination, 'ab') as output:
        try:
            with build_opener(Redirects()).open(Request(url, headers={'User-Agent': 'BetterContent-local-pinned-SDK/1'}), timeout=40) as response:
                checked(response.geturl())
                total = 0
                while chunk := response.read(256 * 1024):
                    output.write(chunk)
                    total += len(chunk)
                    v2.require(total <= v2.MAX_ARCHIVE, 'SDK download size limit')
        finally:
            output.flush()
            os.fsync(output.fileno())
    return v2.read_file(destination)


def original(path, digest, url, attempt, name):
    if path is None:
        data = download(url, attempt / name)
    else:
        data = v2.read_file(path)
        write_new(attempt / name, data)
    v2.digest_matches(data, digest)
    return data


def coordinate_path(root, name):
    version = 'official-1.20.1-' + OUTPUTS[name]
    return v2.regular_path(root / 'maven/bettercontent/nativeforge/fixture' / name / version / (name + '-' + version + '.jar'))


def install(staged, destination, body):
    destination = v2.regular_path(destination)
    destination.parent.mkdir(parents=True, exist_ok=True)
    try:
        os.link(staged, destination, follow_symlinks=False)
    except FileExistsError:
        v2.require(v2.read_file(destination) == body, 'immutable native SDK coordinate/input drift')


def prepare(root, tool, mapping, ffapi=None, moonlight=None):
    root = v2.regular_path(root)
    v2.regular_path(root / 'attempts').mkdir(parents=True, exist_ok=True)
    attempt = Path(tempfile.mkdtemp(prefix='prepare-', dir=root / 'attempts'))
    write_new(attempt / 'input-selection.json', v2.encode_json({
        'tool': str(tool), 'mapping': str(mapping), 'ffapi': str(ffapi), 'moonlight': str(moonlight),
        'expected_tool': TOOL, 'expected_mapping': MAPPING, 'expected_outputs': OUTPUTS}))
    try:
        return _prepare(root, tool, mapping, ffapi, moonlight, attempt)
    except Exception as failure:
        write_new(attempt / 'failure.txt', (type(failure).__name__ + ': ' + str(failure) + '\n').encode())
        print('Retained failed SDK attempt: ' + str(attempt), file=sys.stderr)
        raise


def _prepare(root, tool, mapping, ffapi, moonlight, attempt):
    tool, mapping = map(v2.regular_path, (tool, mapping))
    # Current private-worker FG mapping only; never historical/global cache fallback.
    mapping_bytes = v2.read_file(mapping)
    write_new(attempt / 'mapping.zip', mapping_bytes)
    v2.digest_matches(mapping_bytes, MAPPING)
    if tool.exists():
        tool_bytes = v2.read_file(tool)
        write_new(attempt / 'installertools-1.4.4.jar', tool_bytes)
    else:
        tool_bytes = download('https://maven.minecraftforge.net/net/minecraftforge/installertools/1.4.4/installertools-1.4.4-fatjar.jar',
                              attempt / 'installertools-1.4.4.jar')
    v2.digest_matches(tool_bytes, TOOL)
    # Execute retained snapshots, never potentially changing shared cache paths.
    tool, mapping = attempt / 'installertools-1.4.4.jar', attempt / 'mapping.zip'
    # Keep input correspondence independently, even if a later rename/publication fails.
    artifact = v2.load_spec()['artifacts'][0]
    raw_cache = v2.regular_path(root / 'inputs' / (FFAPI_RAW + '.jar'))
    moon_cache = v2.regular_path(root / 'inputs' / (MOONLIGHT_RAW + '.jar'))
    raw = original(ffapi if ffapi is not None else raw_cache if raw_cache.exists() else None,
                   FFAPI_RAW, artifact['url'], attempt, 'ffapi-original.jar')
    install(attempt / 'ffapi-original.jar', raw_cache, raw)
    corrected, _ = v2.transform(raw, artifact['patches'])
    v2.digest_matches(corrected, FFAPI_FINAL)
    write_new(attempt / 'ffapi-production-resource-corrected.jar', corrected)
    moon = original(moonlight if moonlight is not None else moon_cache if moon_cache.exists() else None,
                    MOONLIGHT_RAW, 'https://mediafilez.forgecdn.net/files/8261/419/moonlight-1.20-2.16.34-forge.jar',
                    attempt, 'moonlight-original.jar')
    install(attempt / 'moonlight-original.jar', moon_cache, moon)
    existing = {}
    for name, digest in OUTPUTS.items():
        path = coordinate_path(root, name)
        if path.exists():
            existing[name] = v2.read_file(path)
            v2.digest_matches(existing[name], digest)  # Never remap over stale/foreign bytes.
    reused = len(existing) == len(OUTPUTS)
    if reused:
        outputs = existing
        mapped_ffapi, mapped_moon = outputs['ffapi-native'], outputs['moonlight-native']
    else:
        mapped_ffapi = rename(corrected, attempt / 'ffapi-remap', tool, mapping, 'ffapi')
        mapped_moon = rename(moon, attempt / 'moonlight-remap', tool, mapping, 'moonlight')
        _, mapped_entries = v2.archive(mapped_ffapi)
        outputs = {'ffapi-native': mapped_ffapi, 'moonlight-native': mapped_moon,
                   **{name: mapped_entries[path] for name, path in MODULES.items()}}
    _, mapped_entries = v2.archive(mapped_ffapi)
    v2.require(all(mapped_entries[path] == outputs[name] for name, path in MODULES.items()), 'mapped module/root ownership drift')
    for name, body in outputs.items():
        v2.digest_matches(body, OUTPUTS[name])
    receipt = {'schema': 'bc.native_forge_api_sdk.v1', 'role': 'NONPRODUCTION_OFFICIAL_1_20_1',
               'tool_sha256': TOOL, 'mapping_sha256': MAPPING,
               'mapping_execution': 'REUSED_EXACT_PINNED_OFFICIAL_OUTPUTS' if reused else 'EXECUTED_PINNED_FORGE_RENAMER',
               'preparer_sha256': v2.sha(v2.read_file(__file__)),
               'raw_ffapi_sha256': FFAPI_RAW, 'production_ffapi_sha256': FFAPI_FINAL,
               'raw_moonlight_sha256': MOONLIGHT_RAW,
               'outputs': {name: v2.sha(body) for name, body in outputs.items()},
               'ffapi_inventory': inventory(mapped_ffapi), 'moonlight_inventory': inventory(mapped_moon),
               'runtime_acceptance': 'NOT_EVALUATED', 'publication': 'LOCAL_ONLY'}
    write_new(attempt / 'sdk-receipt.json', v2.encode_json(receipt))
    # Content-addressed immutable SDK namespace: no changed bytes ever reuse a mapping identity.
    for name, body in outputs.items():
        staged = attempt / (name + '.jar')
        write_new(staged, body)
        install(staged, coordinate_path(root, name), body)
    return attempt, receipt


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--root', type=Path, required=True)
    p.add_argument('--tool', type=Path, required=True)
    p.add_argument('--mapping', type=Path, required=True)
    p.add_argument('--ffapi', type=Path)
    p.add_argument('--moonlight', type=Path)
    args = p.parse_args()
    attempt, receipt = prepare(args.root, args.tool, args.mapping, args.ffapi, args.moonlight)
    print(str(attempt))
    print(v2.encode_json(receipt).decode())
