#!/usr/bin/env python3
"""Nonshipping native-fixture completion from exactly three approved tracked defaults."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess
import uuid

APPROVED = {
    'config/adpother-common.toml': 'd255babc81f6729ce3442522c76cab2a05f960b496f292fdc0371af69745f514',
    'config/forgeendertech-common.toml': 'e3c7fbcf2865785d6dd5ec72b3db1abae3e7ba6376bd3aecc0472586ed21b6ad',
    'config/sophisticatedcore-common.toml': '4f57715393772dce976ebfea3e78378a44ee7fc2fce8975d4811c40b8f9ddbd3',
}


class FixtureError(RuntimeError):
    pass


def digest(data):
    return hashlib.sha256(data).hexdigest()


def git(root, *args):
    result = subprocess.run(['git', '-C', str(root), *args], capture_output=True)
    if result.returncode:
        raise FixtureError('Missing/untracked authoritative input or unavailable Git HEAD')
    return result.stdout


def authority(root):
    result = {}
    for name, expected in APPROVED.items():
        path = root / name
        if not path.is_file() or path.is_symlink():
            raise FixtureError('Missing or symlinked authoritative input: ' + name)
        git(root, 'ls-files', '--error-unmatch', name)
        blob = git(root, 'show', 'HEAD:' + name)
        if digest(blob) != expected or path.read_bytes() != blob:
            raise FixtureError('Authoritative tracked HEAD/working bytes mismatch: ' + name)
        result[name] = {'sha256': expected, 'git_blob': git(root, 'rev-parse', 'HEAD:' + name).decode().strip()}
    return {'root_head': git(root, 'rev-parse', 'HEAD').decode().strip(), 'files': result}


def create_snapshot(root, destination):
    origin = authority(root)
    destination.mkdir(parents=True, exist_ok=False)
    for name in APPROVED:
        target = destination / name
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open('xb') as stream:
            stream.write((root / name).read_bytes())
        target.chmod(0o444)
    with (destination / 'origin.json').open('x') as stream:
        json.dump(origin, stream, indent=2)
        stream.write('\n')
    (destination / 'origin.json').chmod(0o444)


def validate_snapshot(root, snapshot):
    current = authority(root)
    names = {str(p.relative_to(snapshot)) for p in snapshot.rglob('*') if p.is_file()}
    if names != set(APPROVED) | {'origin.json'} or any(p.is_symlink() for p in snapshot.rglob('*')):
        raise FixtureError('Snapshot must contain exactly the three allowlisted inputs and origin.json')
    origin = json.loads((snapshot / 'origin.json').read_text())
    if set(origin['files']) != set(APPROVED) or not re.fullmatch(r'(?:[0-9a-f]{40}|[0-9a-f]{64})', origin['root_head']):
        raise FixtureError('Snapshot origin allowlist/immutable HEAD mismatch')
    for name, expected in APPROVED.items():
        if digest((snapshot / name).read_bytes()) != expected or origin['files'][name] != current['files'][name]:
            raise FixtureError('Immutable snapshot/provenance mismatch: ' + name)
        if digest(git(root, 'show', origin['root_head'] + ':' + name)) != expected:
            raise FixtureError('Snapshot original tracked HEAD mismatch: ' + name)
    return current, origin


def prepare(root, snapshot, run, base):
    root, snapshot = root.resolve(), snapshot.resolve()
    # Validate the configured native destination before any fixture writes.
    if run.is_symlink() or run.resolve().parent != base.resolve() or base.name != 'gametest' or base.parent.name != 'build':
        raise FixtureError('Refusing nonconfigured UUID native destination')
    try:
        uuid.UUID(run.name)
    except ValueError as failure:
        raise FixtureError('Native directory is not a UUID') from failure
    current, origin = validate_snapshot(root, snapshot)
    targets = {}
    for name, expected in APPROVED.items():
        target = run / name
        if any(parent.is_symlink() for parent in [target, target.parent]):
            raise FixtureError('Symlinked native config destination')
        before = target.read_bytes() if target.exists() else None
        if before is not None and digest(before) != expected:
            raise FixtureError('Existing native input differs; bytes preserved: ' + name)
        targets[name] = (target, before)
    records = []
    for name, (target, before) in targets.items():
        target.parent.mkdir(parents=True, exist_ok=True)
        try:
            with target.open('xb') as stream:
                stream.write((snapshot / name).read_bytes())
        except FileExistsError:
            # Do not overwrite an existing file, even if another actor created it after preflight.
            pass
        after = target.read_bytes()
        if digest(after) != APPROVED[name] or (before is not None and after != before):
            raise FixtureError('Native config changed or mismatched; not overwritten: ' + name)
        records.append({'source': name, 'source_sha256': APPROVED[name], 'git_blob': current['files'][name]['git_blob'],
                        'destination': str(target.resolve()), 'before_sha256': digest(before) if before is not None else None,
                        'after_sha256': digest(after), 'existing_bytes_preserved': before is not None})
    receipt = {'phase': 'prepared before native launch', 'utc': datetime.now(timezone.utc).isoformat(),
               'actual_configured_run': str(run.resolve()),
               'authority': current, 'immutable_snapshot_origin': origin, 'inputs': records}
    with (run / 'native-default-config-inputs.json').open('x') as stream:
        json.dump(receipt, stream, indent=2)
        stream.write('\n')
    return receipt


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['snapshot', 'prepare'])
    parser.add_argument('--root', required=True, type=Path)
    parser.add_argument('--snapshot', required=True, type=Path)
    parser.add_argument('--run', type=Path)
    parser.add_argument('--base', type=Path)
    args = parser.parse_args()
    if args.action == 'snapshot':
        create_snapshot(args.root, args.snapshot)
    else:
        if args.run is None or args.base is None:
            parser.error('prepare requires --run and --base')
        prepare(args.root, args.snapshot, args.run, args.base)


if __name__ == '__main__':
    main()
