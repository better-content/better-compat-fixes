#!/usr/bin/env python3
"""Retained, Minecraft-free controls for the exact production fixture functions."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tomllib
import unittest
import uuid

spec = importlib.util.spec_from_file_location('fixture', Path(__file__).with_name('native_default_configs.py'))
f = importlib.util.module_from_spec(spec)
spec.loader.exec_module(f)
E = Path(os.environ.get('BC_NATIVE_DEFAULT_TEST_EVIDENCE_DIR', Path(__file__).resolve().parents[1] / 'build/native-default-controls')) / str(uuid.uuid4())
E.mkdir(parents=True)
SNAPSHOT = Path(__file__).resolve().parents[1] / 'gradle/native-default-configs-v2'
LEGACY_SNAPSHOT = SNAPSHOT.with_name('native-default-configs')
SOPHISTICATED = 'config/sophisticatedcore-common.toml'
EXPECTED_INPUTS = {
    'config/adpother-common.toml': 'd255babc81f6729ce3442522c76cab2a05f960b496f292fdc0371af69745f514',
    'config/forgeendertech-common.toml': 'e3c7fbcf2865785d6dd5ec72b3db1abae3e7ba6376bd3aecc0472586ed21b6ad',
    SOPHISTICATED: '4f57715393772dce976ebfea3e78378a44ee7fc2fce8975d4811c40b8f9ddbd3',
}
EXPECTED_DISABLED = {
    'sophisticatedbackpacks:copper_backpack',
    'sophisticatedbackpacks:jukebox_upgrade',
    'sophisticatedbackpacks:advanced_jukebox_upgrade',
    'sophisticatedstorage:jukebox_upgrade',
    'sophisticatedstorage:advanced_jukebox_upgrade',
    'sophisticatedstorage:basic_to_copper_tier_upgrade',
    'sophisticatedstorage:copper_barrel',
    'sophisticatedstorage:limited_copper_barrel_1',
    'sophisticatedstorage:limited_copper_barrel_2',
    'sophisticatedstorage:limited_copper_barrel_3',
    'sophisticatedstorage:limited_copper_barrel_4',
    'sophisticatedstorage:copper_chest',
    'sophisticatedstorage:copper_shulker_box',
}


class Controls(unittest.TestCase):
    def setUp(self):
        self.case = E / self._testMethodName
        self.root = self.case / 'authority'
        self.root.mkdir(parents=True)
        for name in f.APPROVED:
            p = self.root / name
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_bytes((SNAPSHOT / name).read_bytes())
        def git(*args):
            subprocess.run(['git', '-C', str(self.root), *args], check=True, capture_output=True)
        self.git = git
        git('init', '-q'); git('add', '.')
        git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@invalid', 'commit', '-qm', 'Retained synthetic tracked-input controls')
        self.snapshot = self.case / 'snapshot'
        f.create_snapshot(self.root, self.snapshot)
        self.base = self.case / 'build/gametest'
        self.run = self.base / str(uuid.uuid4())

    def prepare(self):
        return f.prepare(self.root, self.snapshot, self.run, self.base)

    def test_absent_creation_and_pre_native_receipt(self):
        receipt = self.prepare()
        self.assertEqual(3, len(receipt['inputs']))
        self.assertEqual(set(EXPECTED_INPUTS), {r['source'] for r in receipt['inputs']})
        self.assertTrue(all(r['before_sha256'] is None for r in receipt['inputs']))
        self.assertEqual(str(self.run.resolve()), receipt['actual_configured_run'])
        for name, expected in f.APPROVED.items():
            self.assertEqual(expected, f.digest((self.run / name).read_bytes()))
        self.assertEqual(receipt, json.loads((self.run / 'native-default-config-inputs.json').read_text()))

    def test_exact_inventory_and_legacy_inputs_preserved(self):
        self.assertEqual(EXPECTED_INPUTS, f.APPROVED)
        self.assertEqual(set(EXPECTED_INPUTS) | {'origin.json'},
                         {str(p.relative_to(SNAPSHOT)) for p in SNAPSHOT.rglob('*') if p.is_file()})
        for name, expected in EXPECTED_INPUTS.items():
            self.assertEqual(expected, f.digest((SNAPSHOT / name).read_bytes()))
        for name in set(EXPECTED_INPUTS) - {SOPHISTICATED}:
            self.assertEqual((LEGACY_SNAPSHOT / name).read_bytes(), (SNAPSHOT / name).read_bytes())

    def test_production_sophisticated_policy_preserved(self):
        self.prepare()
        for path in (SNAPSHOT / SOPHISTICATED, self.run / SOPHISTICATED):
            data = tomllib.loads(path.read_text())
            self.assertEqual({'common'}, set(data))
            self.assertEqual({'enabledItems'}, set(data['common']))
            entries = data['common']['enabledItems']
            values = dict(entry.rsplit('|', 1) for entry in entries)
            self.assertEqual(182, len(entries))
            self.assertEqual(182, len(values))
            self.assertEqual({'true', 'false'}, set(values.values()))
            self.assertEqual(EXPECTED_DISABLED, {name for name, value in values.items() if value == 'false'})
            self.assertTrue(all(name.startswith(('sophisticatedstorage:', 'sophisticatedbackpacks:'))
                                for name in values))

    def test_legacy_two_input_snapshot_rejected_before_attempt_writes(self):
        self.snapshot = LEGACY_SNAPSHOT
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_missing_sophisticated_snapshot_rejected_before_attempt_writes(self):
        p = self.snapshot / SOPHISTICATED
        p.rename(self.case / 'retained-missing-sophisticated-snapshot.toml')
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_missing_sophisticated_authority_rejected_before_attempt_writes(self):
        (self.root / SOPHISTICATED).rename(self.case / 'retained-missing-sophisticated-authority.toml')
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_existing_custom_sophisticated_bytes_reject_all_writes(self):
        target = self.run / SOPHISTICATED
        target.parent.mkdir(parents=True)
        original = b'# exact operator bytes\r\n[common]\r\nenabledItems = []\r\n'
        target.write_bytes(original)
        before = target.stat().st_mtime_ns
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertEqual(original, target.read_bytes())
        self.assertEqual(before, target.stat().st_mtime_ns)
        self.assertEqual([target], list(target.parent.iterdir()))
        self.assertFalse((self.run / 'native-default-config-inputs.json').exists())

    def test_sophisticated_symlink_rejects_all_writes(self):
        target = self.run / SOPHISTICATED
        target.parent.mkdir(parents=True)
        outside = self.case / 'operator-sophisticated.toml'
        original = b'preserve=true\n'; outside.write_bytes(original)
        target.symlink_to(outside)
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertEqual(original, outside.read_bytes())
        self.assertEqual([target], list(target.parent.iterdir()))
        self.assertFalse((self.run / 'native-default-config-inputs.json').exists())

    def test_sophisticated_disabled_policy_change_rejected(self):
        target = self.snapshot / SOPHISTICATED
        target.chmod(0o644)
        target.write_bytes(target.read_bytes().replace(b'copper_backpack|false', b'copper_backpack|true'))
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_sophisticated_working_authority_policy_change_rejected(self):
        target = self.root / SOPHISTICATED
        target.write_bytes(target.read_bytes().replace(b'copper_backpack|false', b'copper_backpack|true'))
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_sophisticated_committed_authority_policy_change_rejected(self):
        target = self.root / SOPHISTICATED
        target.write_bytes(target.read_bytes().replace(b'copper_backpack|false', b'copper_backpack|true'))
        self.git('add', '.')
        self.git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@invalid', 'commit', '-qm',
                 'Retained negative: production-disabled policy changed')
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_sophisticated_origin_blob_forgery_rejected(self):
        target = self.snapshot / 'origin.json'; target.chmod(0o644)
        origin = json.loads(target.read_text())
        origin['files'][SOPHISTICATED]['git_blob'] = 'forged'
        target.write_text(json.dumps(origin))
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_three_input_cli_snapshot_and_prepare(self):
        cli_snapshot = self.case / 'cli-snapshot'
        script = str(Path(f.__file__).resolve())
        subprocess.run([sys.executable, '-B', script, 'snapshot', '--root', str(self.root),
                        '--snapshot', str(cli_snapshot)], check=True, capture_output=True, timeout=15)
        subprocess.run([sys.executable, '-B', script, 'prepare', '--root', str(self.root),
                        '--snapshot', str(cli_snapshot), '--run', str(self.run), '--base', str(self.base)],
                       check=True, capture_output=True, timeout=15)
        receipt = json.loads((self.run / 'native-default-config-inputs.json').read_text())
        self.assertEqual(set(EXPECTED_INPUTS), {entry['source'] for entry in receipt['inputs']})
        self.assertEqual(set(EXPECTED_INPUTS), set(receipt['immutable_snapshot_origin']['files']))

    def test_existing_approved_bytes_preserved(self):
        before = {}
        for name in f.APPROVED:
            p = self.run / name; p.parent.mkdir(parents=True, exist_ok=True)
            p.write_bytes((self.snapshot / name).read_bytes()); before[name] = (p.read_bytes(), p.stat().st_mtime_ns)
        self.prepare()
        for name, (data, mtime) in before.items():
            self.assertEqual(data, (self.run / name).read_bytes())
            self.assertEqual(mtime, (self.run / name).stat().st_mtime_ns)

    def test_missing_authority_rejected(self):
        self.root = self.case / 'missing-root'
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_untracked_authority_rejected(self):
        self.git('rm', '--cached', next(iter(f.APPROVED)))
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_working_source_mismatch_rejected(self):
        (self.root / next(iter(f.APPROVED))).write_text('guessed=true\n')
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_committed_head_mismatch_rejected(self):
        (self.root / next(iter(f.APPROVED))).write_text('different=true\n')
        self.git('add', '.')
        self.git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@invalid', 'commit', '-qm', 'Wrong tracked input control')
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_unrelated_root_commit_preserves_scoped_inputs(self):
        original = json.loads((self.snapshot / 'origin.json').read_text())
        (self.root / 'unrelated-harness.txt').write_text('Unrelated Root harness change\n')
        self.git('add', '.')
        self.git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@invalid', 'commit', '-qm', 'Unrelated Root commit control')
        receipt = self.prepare()
        self.assertNotEqual(original['root_head'], receipt['authority']['root_head'])
        self.assertEqual(original, receipt['immutable_snapshot_origin'])
        self.assertEqual(original['files'], receipt['authority']['files'])
        self.assertTrue(all(r['source_sha256'] == r['after_sha256'] for r in receipt['inputs']))

    def test_missing_snapshot_rejected(self):
        self.snapshot = self.case / 'missing-snapshot'
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_snapshot_mismatch_rejected(self):
        p = self.snapshot / next(iter(f.APPROVED)); p.chmod(0o644); p.write_text('guessed=true\n')
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_extra_snapshot_input_rejected(self):
        (self.snapshot / 'Sources.cfg').write_text('not approved')
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_wrong_destination_rejected(self):
        self.run = self.case / 'operator-run' / str(uuid.uuid4())
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_non_uuid_destination_rejected(self):
        self.run = self.base / 'ordinary-run'
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_existing_custom_bytes_not_overwritten(self):
        p = self.run / next(iter(f.APPROVED)); p.parent.mkdir(parents=True)
        original = b'# preserve exact\r\ncustom=true\r\n'; p.write_bytes(original)
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertEqual(original, p.read_bytes())
        self.assertFalse((self.run / 'native-default-config-inputs.json').exists())

    def test_symlink_destination_rejected(self):
        outside = self.case / 'operator-config'; outside.mkdir()
        self.run.mkdir(parents=True); (self.run / 'config').symlink_to(outside, target_is_directory=True)
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(list(outside.iterdir()))

    def test_symbolic_origin_not_immutable(self):
        p = self.snapshot / 'origin.json'; p.chmod(0o644)
        data = json.loads(p.read_text()); data['root_head'] = 'HEAD'; p.write_text(json.dumps(data))
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())

    def test_forged_origin_rejected(self):
        p = self.snapshot / 'origin.json'; p.chmod(0o644)
        data = json.loads(p.read_text()); data['files'][next(iter(f.APPROVED))]['git_blob'] = 'forged'
        p.write_text(json.dumps(data))
        with self.assertRaises(f.FixtureError): self.prepare()
        self.assertFalse(self.run.exists())


if __name__ == '__main__':
    print('Retained pure fixture controls:', E, flush=True)
    unittest.main(verbosity=2)
