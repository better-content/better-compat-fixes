#!/usr/bin/env python3
"""Successor contract: retain historical v2 verbatim; requalify only current provider plugin."""
import argparse
import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import build_upstream_runtime_overrides_v2 as m

ORIGINALS = None
COMPAT = None
OUTPUT_ROOT = None


class SuccessorControls(unittest.TestCase):
    def test_frozen_parent_and_only_plugin_digest_migrated(self):
        self.assertEqual(m.FROZEN_V2_SHA256, m.sha(m.SPEC.read_bytes()))
        delta = json.loads(m.SUCCESSOR_SPEC.read_text())
        self.assertEqual(m.FROZEN_V2_SHA256, delta['parent_sha256'])
        original, successor = m.load_spec(), m.load_spec(m.SUCCESSOR_SPEC)
        expected = copy.deepcopy(original)
        expected['schema'], expected['revision'] = delta['schema'], delta['revision']
        expected['required_compat']['plugin_sha256'] = delta['plugin_sha256']
        self.assertEqual(expected, successor)
        self.assertNotEqual(original['required_compat']['plugin_sha256'], delta['plugin_sha256'])

    def test_tampered_parent_or_successor_rejected(self):
        delta = json.loads(m.SUCCESSOR_SPEC.read_text())
        with tempfile.TemporaryDirectory(dir=OUTPUT_ROOT) as temp:
            for field, value in [('parent_sha256', '0' * 64), ('plugin_sha256', 'not-a-digest'),
                                 ('extra', 'unreviewed')]:
                changed = dict(delta, **{field: value})
                path = Path(temp) / 'bad.json'
                path.write_text(json.dumps(changed))
                with patch.object(m, 'SUCCESSOR_SPEC', path), self.assertRaises(m.Rejected):
                    m.load_spec(path)
                self.assertEqual(delta, json.loads(m.SUCCESSOR_SPEC.read_text()))
            parent = Path(temp) / 'parent.json'
            parent.write_bytes(m.SPEC.read_bytes() + b' ')
            with patch.object(m, 'SPEC', parent), self.assertRaises(m.Rejected):
                m.load_spec(m.SUCCESSOR_SPEC)

    def test_current_provider_requalified_and_old_plugin_rejected(self):
        if ORIGINALS is None:
            self.skipTest('requires pinned originals and current staged provider')
        current = m.load_spec(m.SUCCESSOR_SPEC)
        plugin = current['required_compat']['plugin'].replace('.', '/') + '.class'
        with zipfile.ZipFile(COMPAT) as staged:
            self.assertEqual(current['required_compat']['plugin_sha256'], m.sha(staged.read(plugin)))
        with self.assertRaises(m.Rejected):
            m.check_compat(m.read_file(COMPAT), m.load_spec()['required_compat'])
        with tempfile.TemporaryDirectory(dir=OUTPUT_ROOT) as temp:
            output = Path(temp) / 'qualified'
            receipt = m.build(ORIGINALS, output, COMPAT, m.SUCCESSOR_SPEC)
            self.assertEqual('bc-runtime.3', receipt['revision'])
            self.assertEqual(m.sha(m.SUCCESSOR_SPEC.read_bytes()), receipt['spec_sha256'])
            self.assertEqual(current['required_compat']['plugin_sha256'], receipt['compat']['plugin_sha256'])
            self.assertEqual(5, len(receipt['artifacts']))
            self.assertTrue(all(a['class_bytes_invariant'] for a in receipt['artifacts']))


def main():
    global ORIGINALS, COMPAT, OUTPUT_ROOT
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--originals', type=Path)
    parser.add_argument('--compat-jar', type=Path)
    parser.add_argument('--output-root', type=Path, required=True)
    args = parser.parse_args()
    if bool(args.originals) != bool(args.compat_jar):
        parser.error('originals and compat jar must be supplied together')
    ORIGINALS, COMPAT, OUTPUT_ROOT = args.originals, args.compat_jar, args.output_root
    result = unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(SuccessorControls))
    raise SystemExit(0 if result.wasSuccessful() else 1)


if __name__ == '__main__':
    main()
