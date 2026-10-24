#!/usr/bin/env python3
"""Pure controls, plus optional explicit pinned-original qualification (no Minecraft).

All filesystem controls and outputs are retained; no tempfile cleanup occurs.
"""
import argparse
import copy
import importlib.util
import io
import json
import os
from pathlib import Path
import stat
import struct
import tempfile
import unittest
import warnings
import zipfile
from unittest.mock import patch as mock_patch

MODULE_PATH = Path(__file__).with_name('build_upstream_runtime_overrides_v2.py')
loader = importlib.util.spec_from_file_location('overrides', MODULE_PATH)
m = importlib.util.module_from_spec(loader)
loader.loader.exec_module(m)
CONTROLS = None
ORIGINALS = None
COMPAT = None


def jar(pairs, descriptor=False):
    class Unseekable(io.BytesIO):
        def seekable(self):
            return False

        def seek(self, *args):
            raise OSError('unseekable control')
    stream = Unseekable() if descriptor else io.BytesIO()
    with zipfile.ZipFile(stream, 'w') as z:
        z.comment = b'retained archive comment'
        for name, data in pairs:
            info = zipfile.ZipInfo(name, (2024, 3, 4, 5, 6, 8))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = (stat.S_IFREG | 0o644) << 16
            info.comment = b'retained entry comment'
            info.extra = b'\xfe\xca\x02\x00OK'
            z.writestr(info, data)
    return stream.getvalue()


def patch(before=None, **operation):
    before = {'required': True, 'client': ['Existing'], 'mixins': ['Client', 'Common']} if before is None else before
    raw = m.encode_json(before)
    return raw, dict(path=['m.json'], parents=[], sha256=m.sha(raw), before=before, **operation)


def local_records(data):
    infos, _ = m.archive(data)
    by_offset = sorted(infos, key=lambda i: i.header_offset)
    end = data.rfind(b'PK\x05\x06')
    central = struct.unpack_from('<I', data, end + 16)[0]
    return {info.filename: data[info.header_offset:(by_offset[i + 1].header_offset
            if i + 1 < len(infos) else central)] for i, info in enumerate(by_offset)}


class PureControls(unittest.TestCase):
    def test_spec_has_exact_scope_and_no_logger_repair(self):
        spec = m.load_spec()
        patches = [p for a in spec['artifacts'] for p in a['patches']]
        self.assertEqual(7, sum('add_min_version' in p for p in patches))
        self.assertEqual(3, sum(len(p.get('move_to_client', [])) for p in patches))
        self.assertEqual(4, sum(len(p.get('remove_keys', [])) for p in patches))
        self.assertEqual(10, len(patches))
        self.assertTrue(all(p['path'][-1].endswith('.json') for p in patches))
        self.assertEqual(2, sum(a['redistribution'] == 'BLOCKED_NO_GRANT' for a in spec['artifacts']))
        self.assertTrue(spec['required_compat']['condition'])

    def test_successor_delta_and_frozen_legacy_contract(self):
        legacy_spec = m.ROOT / 'src/upstreamRuntime/overrides-v1.json'
        legacy_builder = m.ROOT / 'scripts/build_upstream_runtime_overrides.py'
        self.assertEqual('caa9067b44e3e8f119cad614dc133f3e1df69ffe6fdc523c1c2df3232517d378', m.sha(legacy_spec.read_bytes()))
        self.assertEqual('09669c8333ce94358b30f8c303f9885a459388ccfd02d0c867b8b77ece472ed6', m.sha(legacy_builder.read_bytes()))
        before, after = m.parse_json(legacy_spec.read_bytes()), m.load_spec()
        expected = copy.deepcopy(before)
        expected['schema'], expected['revision'] = 'bc.upstream_runtime_overrides.v2', 'bc-runtime.2'
        expected['scope'] = before['scope'].replace('three obsolete AdPother', 'four obsolete AdPother')
        # Independently compiled staged class: precisely the two drainFuel()V selector strings.
        hearth = 'efd764d6ac2728e429a8788b09e744e0a5c54f18c86ccfda1ff9ea64940823ce'
        self.assertEqual(hearth, after['required_compat']['classes']['adpother.coldsweat.HearthBlockEntityMixin'])
        expected['required_compat']['classes']['adpother.coldsweat.HearthBlockEntityMixin'] = hearth
        a = expected['artifacts'][-1]
        a['output'] = a['output'].replace('-bc-runtime.1.jar', '-bc-runtime.2.jar')
        a['patches'][0]['before_fields']['coldsweat$boiler'] = 'coremods/coldsweat/boiler_fuel_burnout_hook.js'
        a['patches'][0]['remove_keys'].insert(1, 'coldsweat$boiler')
        self.assertEqual(expected, after)

    def test_wrong_removal_population_rejected(self):
        before = {k: k+'.js' for k in ['a','b','c','d','e']}
        raw = m.encode_json(before)
        for names in [['a','b','c'], ['a','b','c','d','e'], ['a','b','c','c']]:
            p = dict(path=['META-INF/coremods.json'], sha256=m.sha(raw), parents=[],
                     before_fields={k:before[k] for k in names}, remove_keys=names)
            with self.assertRaises(m.Rejected): m.patch_json(raw,p)

    def test_min_version_and_preserve_settings(self):
        raw, p = patch(add_min_version='0.8.5')
        expected = dict(p['before'], minVersion='0.8.5')
        self.assertEqual(expected, m.parse_json(m.patch_json(raw, p)))

    def test_relocation_keeps_other_order_and_fields(self):
        raw, p = patch(move_to_client=['Client'])
        after = m.parse_json(m.patch_json(raw, p))
        self.assertEqual(['Common'], after['mixins'])
        self.assertEqual(['Existing', 'Client'], after['client'])
        self.assertIs(True, after['required'])

    def test_remove_only_four_coremods(self):
        before = {'x': 'x.js', 'a': 'a.js', 'b': 'b.js', 'c': 'c.js', 'd': 'd.js', 'z': 'z.js'}
        raw = m.encode_json(before)
        p = dict(path=['META-INF/coremods.json'], sha256=m.sha(raw), parents=[],
                 before_fields={k: before[k] for k in ['a', 'b', 'c', 'd']}, remove_keys=['a', 'b', 'c', 'd'])
        self.assertEqual({'x': 'x.js', 'z': 'z.js'}, m.parse_json(m.patch_json(raw, p)))

    def test_raw_records_metadata_order_and_classes_stay(self):
        for descriptor in [False, True]:
            with self.subTest(descriptor=descriptor):
                raw, p = patch(add_min_version='0.8.5')
                original = jar([('Unchanged.class', b'CAFEBABE-control'), ('m.json', raw),
                                ('LICENSE', b'license must survive\r\n')], descriptor)
                after, records = m.transform(original, [p])
                self.assertEqual(after, m.transform(original, [p])[0])
                self.assertEqual(m.inventory(original), m.inventory(after))
                old, new = local_records(original), local_records(after)
                self.assertEqual(old['Unchanged.class'], new['Unchanged.class'])
                self.assertEqual(old['LICENSE'], new['LICENSE'])
                oldz, newz = zipfile.ZipFile(io.BytesIO(original)), zipfile.ZipFile(io.BytesIO(after))
                self.assertEqual(oldz.namelist(), newz.namelist())
                self.assertEqual(oldz.comment, newz.comment)
                for oi, ni in zip(oldz.infolist(), newz.infolist()):
                    for field in ['date_time', 'compress_type', 'flag_bits', 'comment', 'extra',
                                  'internal_attr', 'external_attr', 'create_system', 'create_version',
                                  'extract_version']:
                        self.assertEqual(getattr(oi, field), getattr(ni, field), field)
                self.assertEqual(1, len(records))

    def test_nested_archive_two_resources_in_same_jar(self):
        raw, p = patch(add_min_version='0.8.5')
        inner = jar([('m.json', raw), ('n.json', raw), ('A.class', b'opaque actual bytes')])
        p1 = dict(p, path=['nested.jar', 'm.json'], parents=[m.sha(inner)])
        p2 = dict(p, path=['nested.jar', 'n.json'], parents=[m.sha(inner)])
        original = jar([('nested.jar', inner), ('NOTICE', b'retained')])
        revised, receipt = m.transform(original, [p1, p2])
        self.assertEqual(2, len(receipt))
        self.assertEqual(m.inventory(original), m.inventory(revised))
        self.assertEqual(local_records(original)['NOTICE'], local_records(revised)['NOTICE'])

    def test_wrong_resource_hash_and_structure_rejected(self):
        raw, p = patch(add_min_version='0.8.5')
        for changed in [dict(p, sha256='0' * 64), dict(p, before={}),
                        dict(p, add_min_version='0.8.4'), dict(p, move_to_client=['Client'])]:
            with self.assertRaises(m.Rejected):
                m.patch_json(raw, changed)
        for malformed in [b'{"x":1,"x":2}', b'{"x":NaN}', b'[]', b'not json']:
            changed = dict(p, sha256=m.sha(malformed))
            with self.assertRaises(m.Rejected):
                m.patch_json(malformed, changed)

    def test_relocation_missing_duplicate_wrong_types_rejected(self):
        for before in [{'mixins': [], 'client': []}, {'mixins': ['Client', 'Client'], 'client': []},
                       {'mixins': ['Client'], 'client': ['Client']},
                       {'mixins': 'Client', 'client': []}, {'mixins': ['Client'], 'client': {}}]:
            raw, p = patch(before, move_to_client=['Client'])
            with self.assertRaises(m.Rejected):
                m.patch_json(raw, p)

    def test_duplicate_missing_and_wrong_parent_patches_rejected(self):
        raw, p = patch(add_min_version='0.8.5')
        original = jar([('m.json', raw)])
        for patches in [[p, p], [dict(p, path=['absent.json'])],
                        [dict(p, path=['m.json', 'n.json'], parents=['0' * 64])]]:
            with self.assertRaises(m.Rejected):
                m.transform(original, patches)

    def test_unsafe_names_duplicates_signatures_and_nested_signatures(self):
        for name in ['../escape', '/absolute', 'a//b', 'a/./b', 'a\\b', 'META-INF/TEST.SF',
                     'META-INF/TEST.RSA', 'META-INF/SIG-foo']:
            with self.subTest(name=name), self.assertRaises(m.Rejected):
                m.archive(jar([(name, b'payload')]))
        with warnings.catch_warnings():
            warnings.simplefilter('ignore', UserWarning)
            duplicate = jar([('x', b'1'), ('x', b'2')])
        with self.assertRaises(m.Rejected):
            m.archive(duplicate)
        with self.assertRaises(m.Rejected):
            m.archive(jar([('META-INF/MANIFEST.MF', b'Name: A.class\r\nSHA-256-Digest: foo\r\n')]))
        with self.assertRaises(m.Rejected):
            m.inventory(jar([('nested.jar', jar([('META-INF/X.EC', b'signed')]))]))

    def test_size_crc_encryption_symlink_and_zip_layout_bounds(self):
        original = jar([('x', b'abc')])
        old = m.MAX_ENTRY
        try:
            m.MAX_ENTRY = 2
            with self.assertRaises(m.Rejected):
                m.archive(original)
        finally:
            m.MAX_ENTRY = old
        bad = bytearray(original)
        name_length, extra_length = struct.unpack_from('<HH', bad, 26)
        bad[30 + name_length + extra_length] ^= 0xff
        with self.assertRaises(m.Rejected):
            m.archive(bytes(bad))
        with self.assertRaises(m.Rejected):
            m.rewrite_zip(b'preamble' + original, {'x': b'new'})
        encrypted = bytearray(original)
        central = encrypted.find(b'PK\x01\x02')
        struct.pack_into('<H', encrypted, 6, 1)
        struct.pack_into('<H', encrypted, central + 8, 1)
        with self.assertRaises(m.Rejected):
            m.archive(bytes(encrypted))
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, 'w') as z:
            info = zipfile.ZipInfo('link')
            info.create_system = 3
            info.external_attr = (stat.S_IFLNK | 0o777) << 16
            z.writestr(info, b'destination')
        with self.assertRaises(m.Rejected):
            m.archive(stream.getvalue())

    def test_full_offline_builder_pure_and_original_hash_rejection(self):
        base = CONTROLS / 'pure-build'
        base.mkdir()
        inputs = base / 'inputs'
        inputs.mkdir()
        raw, p = patch(add_min_version='0.8.5')
        original = jar([('m.json', raw), ('A.class', b'class-control'), ('LICENSE', b'unchanged')])
        artifacts = []
        for index in range(5):
            name = 'control-%d.jar' % index
            (inputs / name).write_bytes(original)
            artifacts.append(dict(input=name, output='control-%d-bc-runtime.1.jar' % index,
                                  sha256=m.sha(original), sha1=m.sha(original, 'sha1'), patches=[p],
                                  url='https://mediafilez.forgecdn.net/files/control',
                                  license='test-only', redistribution='TEST_ONLY'))
        contract = dict(config='compat.json', config_required=False, plugin='test.Plugin',
                        plugin_sha256=m.sha(b'plugin'), classes={'Replacement': m.sha(b'replacement')},
                        condition='test-only replacement required')
        compat = jar([('compat.json', m.encode_json(dict(required=False, package='test.mixin',
                      plugin='test.Plugin', mixins=['Replacement']))),
                      ('test/Plugin.class', b'plugin'), ('test/mixin/Replacement.class', b'replacement')])
        compat_path = base / 'compat.jar'
        compat_path.write_bytes(compat)
        spec_path = base / 'spec.json'
        spec_path.write_bytes(m.encode_json(dict(schema='bc.upstream_runtime_overrides.v2',
                              revision='bc-runtime.2', artifacts=artifacts, required_compat=contract)))
        first, second = base / 'one', base / 'two'
        self.assertEqual(m.build(inputs, first, compat_path, spec_path),
                         m.build(inputs, second, compat_path, spec_path))
        self.assertEqual(original, (inputs / 'control-0.jar').read_bytes())
        with self.assertRaises(m.Rejected):
            m.build(inputs, first, compat_path, spec_path)
        (inputs / 'control-0.jar').write_bytes(original + b'corrupt')
        failed = base / 'rejected'
        with self.assertRaises(m.Rejected):
            m.build(inputs, failed, compat_path, spec_path)
        self.assertFalse(failed.exists())
        self.assertEqual(original + b'corrupt', (inputs / 'control-0.jar').read_bytes())

    def test_json_and_archive_count_limits(self):
        old_json, old_count, old_expanded = m.MAX_JSON, m.MAX_ENTRIES, m.MAX_EXPANDED
        try:
            m.MAX_JSON = 2
            with self.assertRaises(m.Rejected):
                m.parse_json(b'{}\n')
            m.MAX_ENTRIES = 1
            with self.assertRaises(m.Rejected):
                m.archive(jar([('a', b'1'), ('b', b'2')]))
            m.MAX_EXPANDED = 1
            with self.assertRaises(m.Rejected):
                m.archive(jar([('a', b'12')]))
        finally:
            m.MAX_JSON, m.MAX_ENTRIES, m.MAX_EXPANDED = old_json, old_count, old_expanded

    def test_fetch_is_explicit_fresh_hash_checked_and_origin_bound(self):
        spec = m.load_spec()
        for a in spec['artifacts']:
            a['sha256'], a['sha1'] = m.sha(b'pinned'), m.sha(b'pinned', 'sha1')
        class Response(io.BytesIO):
            def geturl(self):
                return 'https://mediafilez.forgecdn.net/files/reviewed'
        with mock_patch.object(m, 'load_spec', return_value=spec), \
             mock_patch.object(m, 'urlopen', side_effect=lambda *a, **k: Response(b'pinned')):
            target = CONTROLS / 'fetch-positive'
            receipt = m.fetch_originals(target)
            self.assertEqual(5, len(receipt['inputs']))
            self.assertEqual(b'pinned', (target / spec['artifacts'][0]['input']).read_bytes())
            with self.assertRaises(m.Rejected):
                m.fetch_originals(target)
        with mock_patch.object(m, 'load_spec', return_value=spec), \
             mock_patch.object(m, 'urlopen', side_effect=lambda *a, **k: Response(b'wrong')):
            failed = CONTROLS / 'fetch-hash-rejected'
            with self.assertRaises(m.Rejected):
                m.fetch_originals(failed)
            self.assertEqual([], list(failed.iterdir()))
        class WrongOrigin(Response):
            def geturl(self):
                return 'http://unreviewed.example/redirect'
        with mock_patch.object(m, 'load_spec', return_value=spec), \
             mock_patch.object(m, 'urlopen', side_effect=lambda *a, **k: WrongOrigin(b'pinned')):
            with self.assertRaises(m.Rejected):
                m.fetch_originals(CONTROLS / 'fetch-origin-rejected')

    def test_file_symlink_and_fresh_disjoint_output_rejection(self):
        target = CONTROLS / 'original.txt'
        target.write_bytes(b'original')
        link = CONTROLS / 'symlink.txt'
        link.symlink_to(target)
        with self.assertRaises(m.Rejected):
            m.read_file(link)
        with self.assertRaises(m.Rejected):
            m.build(CONTROLS, CONTROLS, target)
        with self.assertRaises(m.Rejected):
            m.build(CONTROLS, CONTROLS / 'child', target)
        self.assertEqual(b'original', target.read_bytes())

    def test_required_compat_provider_and_plugin_controls(self):
        spec = m.load_spec()
        contract = copy.deepcopy(spec['required_compat'])
        package = 'com.bettercontent.bettercompatfixes.mixin'
        entries = [(contract['config'], m.encode_json({'package': package, 'plugin': contract['plugin'],
                    'required': contract['config_required'], 'mixins': list(contract['classes'])}))]
        for name in list(contract['classes']):
            body = name.encode()
            contract['classes'][name] = m.sha(body)
            entries.append((package.replace('.', '/') + '/' + name.replace('.', '/') + '.class', body))
        contract['plugin_sha256'] = m.sha(b'plugin-control')
        plugin_path = contract['plugin'].replace('.', '/') + '.class'
        entries.append((plugin_path, b'plugin-control'))
        self.assertEqual('NOT_EVALUATED', m.check_compat(jar(entries), contract)['runtime_behavior'])
        for altered in [entries[:-1], entries[:-2] + entries[-1:],
                        entries[:-1] + [(plugin_path, b'mutated')]]:
            with self.assertRaises(m.Rejected):
                m.check_compat(jar(altered), contract)


@unittest.skipUnless(ORIGINALS is not None, 'explicit pinned originals not supplied')
class PinnedOriginalControls(unittest.TestCase):
    def test_actual_originals_reproducibility_invariance_and_nonmutation(self):
        spec = m.load_spec()
        before = {a['input']: m.sha(m.read_file(ORIGINALS / a['input'])) for a in spec['artifacts']}
        first, second = CONTROLS / 'qualified-a', CONTROLS / 'qualified-b'
        a = m.build(ORIGINALS, first, COMPAT)
        b = m.build(ORIGINALS, second, COMPAT)
        self.assertEqual(a, b)
        self.assertEqual((first / 'receipt.json').read_bytes(), (second / 'receipt.json').read_bytes())
        self.assertEqual(3144, sum(r['class_count'] for r in a['artifacts']))
        self.assertEqual(10, sum(len(r['resources']) for r in a['artifacts']))
        for artifact, record in zip(spec['artifacts'], a['artifacts']):
            self.assertEqual(before[artifact['input']], m.sha(m.read_file(ORIGINALS / artifact['input'])))
            self.assertEqual((first / artifact['output']).read_bytes(), (second / artifact['output']).read_bytes())
            self.assertEqual(record['output_sha256'], m.sha(m.read_file(first / artifact['output'])))
        with self.assertRaises(m.Rejected):
            m.build(ORIGINALS, first, COMPAT)
        # Negative pre-repair evidence: all reviewed original resource selectors differ
        # from their corrected values, and all minVersion resources really lack the field.
        for artifact in spec['artifacts']:
            archive = m.read_file(ORIGINALS / artifact['input'])
            for p in artifact['patches']:
                body = archive
                for name in p['path']:
                    _, entries = m.archive(body)
                    body = entries[name]
                parsed = m.parse_json(body)
                if 'add_min_version' in p:
                    self.assertNotIn('minVersion', parsed)
                self.assertNotEqual(body, m.patch_json(body, p))


def main():
    global CONTROLS, ORIGINALS, COMPAT
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--originals', type=Path)
    parser.add_argument('--compat-jar', type=Path)
    parser.add_argument('--output-root', type=Path, default=Path('/home/dev/.tmp'))
    args = parser.parse_args()
    if bool(args.originals) != bool(args.compat_jar):
        parser.error('--originals and --compat-jar must be supplied together')
    ORIGINALS, COMPAT = args.originals, args.compat_jar
    CONTROLS = Path(tempfile.mkdtemp(prefix='bc-upstream-controls-', dir=args.output_root))
    print('Retained controls: ' + str(CONTROLS), flush=True)
    # Decorator evaluated before CLI; retain the explicit-input skip only when absent.
    PinnedOriginalControls.__unittest_skip__ = ORIGINALS is None
    suite = unittest.TestSuite([unittest.defaultTestLoader.loadTestsFromTestCase(PureControls),
                               unittest.defaultTestLoader.loadTestsFromTestCase(PinnedOriginalControls)])
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    raise SystemExit(0 if result.wasSuccessful() else 1)


if __name__ == '__main__':
    main()
