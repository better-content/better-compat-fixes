#!/usr/bin/env python3
"""Local-only resource SDK stage, NOT production/provider qualification or publication.

Reuse the exact production v2 resource transform, but do not call its qualified build()
without a provider. The independent production build remains mandatory and unchanged.
Every attempt is retained. A Maven coordinate is installed atomically once, never replaced.
"""
import argparse
import importlib.util
import os
from pathlib import Path
import tempfile

MODULE = Path(__file__).with_name('build_upstream_runtime_overrides_v2.py')
loader = importlib.util.spec_from_file_location('fuel_sdk_v2', MODULE)
v2 = importlib.util.module_from_spec(loader)
loader.loader.exec_module(v2)

COORDINATE = 'bettercontent.fuel.fixture:adpother:8.1.49.0-bc-runtime.2'
RELATIVE = Path('bettercontent/fuel/fixture/adpother/8.1.49.0-bc-runtime.2/adpother-8.1.49.0-bc-runtime.2.jar')
INPUT = 'AdPother-1.20.1-8.1.49.0-build.2294.jar'
INPUT_SHA256 = '4d66ceb91116d2f1dbc55a125cf8b237a11e3eba7787908c066e3ee9d18f4de5'
INPUT_SHA1 = 'fa630492fadb60027176fd73a3a1a95c295f295d'
OUTPUT_SHA256 = '8d01d3db3f30d5ecce66cb44a75b79cd684c34f35127573c17c1ddc9ebaa979f'
COREMODS_SHA256 = 'd41c0bb1f59f73ba2ccdfb7d57f70635ab6a4b4098a838113158566f68d6638d'


def fresh_file(path, data):
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o644)
    with os.fdopen(fd, 'wb') as stream:
        stream.write(data)
        stream.flush()
        os.fsync(stream.fileno())


def corrected(original, spec_path=v2.SPEC):
    spec = v2.load_spec(spec_path)
    matches = [a for a in spec['artifacts'] if a['input'] == INPUT]
    v2.require(len(matches) == 1, 'exact AdPother artifact required')
    artifact = matches[0]
    v2.require(artifact['sha256'] == INPUT_SHA256 and artifact['sha1'] == INPUT_SHA1
               and artifact['output'] == INPUT[:-4] + '-bc-runtime.2.jar', 'SDK identity drift')
    v2.digest_matches(original, INPUT_SHA256)
    v2.digest_matches(original, INPUT_SHA1, 'sha1')
    revised, resources = v2.transform(original, artifact['patches'])
    # The immutable SDK version must never feed different bytes to FG's mapping cache.
    v2.digest_matches(revised, OUTPUT_SHA256)
    before, after = v2.inventory(original), v2.inventory(revised)
    v2.require(len(before) == 321 and before == after, 'AdPother SDK class invariance failure')
    _, old_entries = v2.archive(original)
    _, new_entries = v2.archive(revised)
    v2.require(old_entries.keys() == new_entries.keys(), 'SDK archive membership drift')
    changed = [name for name in old_entries if old_entries[name] != new_entries[name]]
    v2.require(changed == ['META-INF/coremods.json'], 'SDK resource scope drift')
    v2.digest_matches(new_entries['META-INF/coremods.json'], COREMODS_SHA256)
    return revised, resources


def prepare(original_path, root, spec_path=v2.SPEC):
    original_path, root = v2.regular_path(original_path), v2.regular_path(root)
    v2.require(root != original_path and root not in original_path.parents, 'SDK root overlaps original')
    root.mkdir(parents=True, exist_ok=True)
    attempts = root / 'attempts'
    v2.regular_path(attempts).mkdir(exist_ok=True)
    attempt = Path(tempfile.mkdtemp(prefix='prepare-', dir=attempts))
    # Failed attempts retain their original and diagnostic; no cleanup or stable-file replacement.
    try:
        original = v2.read_file(original_path)
        fresh_file(attempt / INPUT, original)
        revised, resources = corrected(original, spec_path)
        staged = attempt / RELATIVE.name
        fresh_file(staged, revised)
        destination = v2.regular_path(root / 'maven' / RELATIVE)
        destination.parent.mkdir(parents=True, exist_ok=True)
        # Hard-link publication is atomic and O_EXCL-like. Concurrent same-byte writers converge;
        # a foreign/stale/tampered coordinate is never overwritten or adopted.
        try:
            os.link(staged, destination, follow_symlinks=False)
        except FileExistsError:
            v2.require(v2.read_file(destination) == revised, 'immutable SDK coordinate differs; choose a new reviewed version')
        receipt = {'schema': 'bc.fuel_sdk_adpother.v1', 'coordinate': COORDINATE,
                   'input_sha256': v2.sha(original), 'output_sha256': v2.sha(revised),
                   'class_count': 321, 'all_class_bytes_preserved': True, 'resources': resources,
                   'production_provider_qualification': 'NOT_PERFORMED_SDK_RESOURCE_STAGE_ONLY',
                   'runtime_acceptance': 'NOT_EVALUATED', 'publication': 'LOCAL_ONLY_NO_REDISTRIBUTION_GRANT',
                   'spec_sha256': v2.sha(v2.read_file(spec_path)),
                   'transform_sha256': v2.sha(v2.read_file(MODULE)),
                   'preparer_sha256': v2.sha(v2.read_file(__file__))}
        fresh_file(attempt / 'sdk-receipt.json', v2.encode_json(receipt))
        return destination, attempt
    except Exception as failure:
        fresh_file(attempt / 'failure.txt', (type(failure).__name__ + ': ' + str(failure) + '\n').encode())
        raise


def pinned_original(root):
    """Fetch only the pinned raw SDK, without resolving Gradle during configuration.

    FG still needs to adjust repository descriptors in afterEvaluate. Resolving even
    a dedicated raw configuration earlier freezes them and breaks that lifecycle.
    This independent immutable input store never modifies Gradle's dependency cache.
    """
    root = v2.regular_path(root)
    root.mkdir(parents=True, exist_ok=True)
    destination = v2.regular_path(root / INPUT)
    if not destination.exists():
        attempt = Path(tempfile.mkdtemp(prefix='download-', dir=root))
        artifact = v2.load_spec()['artifacts'][-1]
        v2.require(artifact['input'] == INPUT and artifact['sha256'] == INPUT_SHA256
                   and artifact['sha1'] == INPUT_SHA1, 'pinned raw SDK identity drift')
        url = v2.urlparse(artifact['url'])
        v2.require(url.scheme == 'https' and url.hostname in
                   ('mediafilez.forgecdn.net', 'media.forgecdn.net'), 'unreviewed SDK download origin')
        try:
            request = v2.Request(artifact['url'], headers={'User-Agent': 'BetterContent-local-pinned-SDK/1'})
            with v2.urlopen(request, timeout=20) as response:
                redirect = v2.urlparse(response.geturl())
                v2.require(redirect.scheme == 'https' and redirect.hostname in
                           ('mediafilez.forgecdn.net', 'media.forgecdn.net'), 'unreviewed SDK download redirect')
                body = response.read(v2.MAX_ARCHIVE + 1)
            v2.require(len(body) <= v2.MAX_ARCHIVE, 'raw SDK download size limit')
            # Retain downloaded bytes, even on a digest failure; never publish them as an input.
            staged = attempt / INPUT
            fresh_file(staged, body)
            v2.digest_matches(body, INPUT_SHA256)
            v2.digest_matches(body, INPUT_SHA1, 'sha1')
            try:
                os.link(staged, destination, follow_symlinks=False)
            except FileExistsError:
                v2.require(v2.read_file(destination) == body, 'immutable raw SDK input differs')
        except Exception as failure:
            fresh_file(attempt / 'failure.txt', (type(failure).__name__ + ': ' + str(failure) + '\n').encode())
            raise
    body = v2.read_file(destination)
    v2.digest_matches(body, INPUT_SHA256)
    v2.digest_matches(body, INPUT_SHA1, 'sha1')
    return destination


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    inputs = parser.add_mutually_exclusive_group(required=True)
    inputs.add_argument('--original', type=Path)
    inputs.add_argument('--fetch-original-root', type=Path)
    parser.add_argument('--root', type=Path, required=True)
    args = parser.parse_args()
    original = args.original if args.original is not None else pinned_original(args.fetch_original_root)
    destination, attempt = prepare(original, args.root)
    print('Local-only SDK artifact: ' + str(destination))
    print('Retained SDK attempt: ' + str(attempt))


if __name__ == '__main__':
    main()
