#!/usr/bin/env python3
"""Pure pinned-original SDK controls. Retains all attempts and invalid fixtures; no game/Gradle."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import copy
import importlib.util
import inspect
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

loader = importlib.util.spec_from_file_location('fuel_sdk', Path(__file__).with_name('prepare_fuel_sdk_adpother.py'))
m = importlib.util.module_from_spec(loader)
loader.loader.exec_module(m)
ORIGINAL = None
EVIDENCE = None


class Controls(unittest.TestCase):
    def root(self):
        return Path(tempfile.mkdtemp(prefix=self._testMethodName + '-', dir=EVIDENCE))

    def test_exact_production_resource_bytes_and_all_321_classes(self):
        raw = m.v2.read_file(ORIGINAL)
        revised, resources = m.corrected(raw)
        artifact = m.v2.load_spec()['artifacts'][-1]
        production, _ = m.v2.transform(raw, artifact['patches'])
        self.assertEqual(production, revised)
        self.assertEqual(m.OUTPUT_SHA256, m.v2.sha(revised))
        self.assertEqual(m.v2.inventory(raw), m.v2.inventory(revised))
        self.assertEqual(321, len(m.v2.inventory(revised)))
        self.assertEqual([['META-INF/coremods.json']], [r['path'] for r in resources])

    def test_every_other_uncompressed_resource_script_license_unchanged(self):
        raw = m.v2.read_file(ORIGINAL)
        out, _ = m.corrected(raw)
        _, before = m.v2.archive(raw)
        _, after = m.v2.archive(out)
        self.assertEqual(before.keys(), after.keys())
        self.assertEqual(['META-INF/coremods.json'], [p for p in before if before[p] != after[p]])
        coremods = m.v2.parse_json(after['META-INF/coremods.json'])
        for removed in ('coldsweat$hearth', 'coldsweat$boiler', 'littlelogistics$steam_locomotive', 'littlelogistics$steam_tug'):
            self.assertNotIn(removed, coremods)

    def test_two_attempts_retain_originals_receipts_and_never_replace_coordinate(self):
        raw = m.v2.read_file(ORIGINAL)
        root = self.root()
        dest, first = m.prepare(ORIGINAL, root)
        before = dest.stat()
        again, second = m.prepare(ORIGINAL, root)
        self.assertEqual(dest, again)
        self.assertEqual((before.st_ino, before.st_mtime_ns), (dest.stat().st_ino, dest.stat().st_mtime_ns))
        self.assertNotEqual(first, second)
        for attempt in (first, second):
            self.assertEqual(raw, (attempt / m.INPUT).read_bytes())
            receipt = m.v2.parse_json((attempt / 'sdk-receipt.json').read_bytes())
            self.assertEqual('NOT_PERFORMED_SDK_RESOURCE_STAGE_ONLY', receipt['production_provider_qualification'])
            self.assertEqual(321, receipt['class_count'])
        self.assertEqual(raw, m.v2.read_file(ORIGINAL))

    def test_existing_foreign_coordinate_rejected_and_retained(self):
        root = self.root()
        dest = root / 'maven' / m.RELATIVE
        dest.parent.mkdir(parents=True)
        dest.write_bytes(b'foreign immutable bytes')
        with self.assertRaises(m.v2.Rejected):
            m.prepare(ORIGINAL, root)
        self.assertEqual(b'foreign immutable bytes', dest.read_bytes())
        self.assertEqual(1, len(list((root / 'attempts').glob('*/failure.txt'))))

    def test_unknown_original_rejected_with_attempt_evidence(self):
        root = self.root()
        wrong = root / 'wrong.jar'
        wrong.write_bytes(b'not the pinned upstream')
        with self.assertRaises(m.v2.Rejected):
            m.prepare(wrong, root / 'sdk')
        self.assertFalse((root / 'sdk/maven' / m.RELATIVE).exists())
        self.assertEqual(1, len(list((root / 'sdk/attempts').glob('*/failure.txt'))))

    def test_recipe_or_output_drift_cannot_reuse_same_version(self):
        raw = m.v2.read_file(ORIGINAL)
        root = self.root()
        spec = copy.deepcopy(m.v2.load_spec())
        spec['artifacts'][-1]['output'] = 'wrong.jar'
        path = root / 'drift.json'
        path.write_bytes(m.v2.encode_json(spec))
        with self.assertRaises(m.v2.Rejected):
            m.corrected(raw, path)
        with patch.object(m.v2, 'transform', return_value=(b'different SDK bytes', [])):
            with self.assertRaises(m.v2.Rejected):
                m.corrected(raw)

    def test_symlink_root_or_coordinate_rejected_without_target_mutation(self):
        root = self.root()
        real = root / 'real'
        real.mkdir()
        link = root / 'link'
        link.symlink_to(real, target_is_directory=True)
        with self.assertRaises(m.v2.Rejected):
            m.prepare(ORIGINAL, link)
        sdk = root / 'sdk'
        dest = sdk / 'maven' / m.RELATIVE
        dest.parent.mkdir(parents=True)
        target = root / 'target'
        target.write_bytes(b'protected')
        dest.symlink_to(target)
        with self.assertRaises(m.v2.Rejected):
            m.prepare(ORIGINAL, sdk)
        self.assertEqual(b'protected', target.read_bytes())

    def test_concurrent_preparations_converge_without_overwrite(self):
        root = self.root()
        with ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(lambda _: m.prepare(ORIGINAL, root), range(2)))
        self.assertEqual(results[0][0], results[1][0])
        self.assertNotEqual(results[0][1], results[1][1])
        self.assertEqual(m.OUTPUT_SHA256, m.v2.sha(results[0][0].read_bytes()))

    def test_pinned_download_once_preserves_original_and_avoids_gradle_resolution(self):
        raw = m.v2.read_file(ORIGINAL)
        class Response(io.BytesIO):
            def geturl(self):
                return m.v2.load_spec()['artifacts'][-1]['url']
        root = self.root()
        with patch.object(m.v2, 'urlopen', return_value=Response(raw)) as download:
            original = m.pinned_original(root)
            before = original.stat()
            self.assertEqual(original, m.pinned_original(root))
        self.assertEqual(1, download.call_count)
        self.assertEqual(20, download.call_args.kwargs['timeout'])
        self.assertEqual(raw, original.read_bytes())
        self.assertEqual((before.st_ino, before.st_mtime_ns),
                         (original.stat().st_ino, original.stat().st_mtime_ns))
        self.assertEqual(raw, next(root.glob('download-*/*.jar')).read_bytes())

    def test_download_hash_failure_retained_and_not_published(self):
        class Response(io.BytesIO):
            def geturl(self):
                return m.v2.load_spec()['artifacts'][-1]['url']
        root = self.root()
        with patch.object(m.v2, 'urlopen', return_value=Response(b'wrong downloaded original')):
            with self.assertRaises(m.v2.Rejected):
                m.pinned_original(root)
        self.assertFalse((root / m.INPUT).exists())
        self.assertEqual(b'wrong downloaded original', next(root.glob('download-*/*.jar')).read_bytes())
        self.assertEqual(1, len(list(root.glob('download-*/failure.txt'))))

    def test_download_hostile_redirect_rejected(self):
        class Response(io.BytesIO):
            def geturl(self):
                return 'https://unreviewed.example/AdPother.jar'
        root = self.root()
        with patch.object(m.v2, 'urlopen', return_value=Response(m.v2.read_file(ORIGINAL))):
            with self.assertRaises(m.v2.Rejected):
                m.pinned_original(root)
        self.assertFalse((root / m.INPUT).exists())
        self.assertEqual(1, len(list(root.glob('download-*/failure.txt'))))

    def test_existing_raw_input_drift_never_overwritten_or_refetched(self):
        root = self.root()
        (root / m.INPUT).write_bytes(b'foreign original')
        with patch.object(m.v2, 'urlopen') as download:
            with self.assertRaises(m.v2.Rejected):
                m.pinned_original(root)
            download.assert_not_called()
        self.assertEqual(b'foreign original', (root / m.INPUT).read_bytes())
        symlink = self.root()
        (symlink / m.INPUT).symlink_to(ORIGINAL)
        with self.assertRaises(m.v2.Rejected):
            m.pinned_original(symlink)

    def test_production_provider_check_not_bypassed_or_changed(self):
        # SDK invokes the resource transform, never the production qualified assembler.
        self.assertIn('check_compat(', inspect.getsource(m.v2.build))
        self.assertNotIn('v2.build(', inspect.getsource(m.prepare))
        self.assertNotIn('v2.check_compat(', inspect.getsource(m.prepare))
        self.assertNotIn('compat_jar', inspect.signature(m.prepare).parameters)

    def test_gradle_configuration_closure_and_native_only_archive_boundaries(self):
        build = (m.MODULE.parent.parent / 'build.gradle.kts').read_text()
        contract = (m.MODULE.parent.parent / 'gradle/fuel-sdk-adpother.gradle').read_text()
        self.assertLess(build.index('apply(from = "gradle/fuel-sdk-adpother.gradle")'), build.index('dependencies {'))
        self.assertNotIn('runtimeOnly(fg.deobf("curse.maven:pollution-of-the-realms-269973:8554528"))', build)
        self.assertIn('runtimeOnly(fg.deobf(project.extra["bcFuelSdkAdpotherCoordinate"] as String))', build)
        for pin in ('cold-sweat-506194:7893262', 'little-logistics-570050:4799459'):
            self.assertIn('compileOnly(fg.deobf("curse.maven:' + pin + '"))', build)
            self.assertIn('runtimeOnly(fg.deobf("curse.maven:' + pin + '"))', build)
        self.assertEqual(1, build.count('args("--mixin.config", "better_compat_fixes.fuel_fixture.mixins.json")'))
        self.assertIn('"**/fuelfixture/**"', build)
        self.assertIn('it.name.contains("/fuelfixture/")', build)
        self.assertIn('it.name == "better_compat_fixes.fuel_fixture.mixins.json"', build)
        self.assertIn('apply(from = "gradle/upstream-runtime-v2-contract.gradle")', build)
        self.assertNotIn('apply(from = "gradle/upstream-runtime-contract.gradle")', build)
        self.assertIn('apply(from = "gradle/upstream-startup-contract.gradle")', build)
        self.assertNotIn('.singleFile', contract)
        self.assertNotIn('.resolve()', contract)
        self.assertNotIn("configurations.create('fuelSdkAdpotherOriginal')", contract)
        self.assertIn("'--fetch-original-root', originalRoot", contract)
        self.assertIn("'--original', originalJar", contract)
        self.assertIn('forRepositories(localRepository)', contract)
        self.assertIn('metadataSources { artifact() }', contract)
        self.assertIn("includeVersion('bettercontent.fuel.fixture', 'adpother', '8.1.49.0-bc-runtime.2')", contract)
        self.assertNotIn("includeGroup('bettercontent.fuel.fixture')", contract)
        self.assertIn('incoming.beforeResolve', contract)
        self.assertIn('incoming.afterResolve', contract)
        self.assertIn('candidates.size() != 1', contract)
        self.assertIn("it.name == 'verifyFast'", contract)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--original', type=Path, required=True)
    parser.add_argument('--evidence', type=Path, required=True)
    args = parser.parse_args()
    ORIGINAL, EVIDENCE = args.original, m.v2.regular_path(args.evidence)
    EVIDENCE.mkdir(parents=True, exist_ok=False)
    unittest.main(argv=[__file__], verbosity=2)
