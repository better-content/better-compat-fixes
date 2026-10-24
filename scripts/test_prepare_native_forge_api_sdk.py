#!/usr/bin/env python3
"""Pure exact SDK mapping/identity controls; no Gradle/native game. Retains all evidence."""
import argparse
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

loader = importlib.util.spec_from_file_location('native_sdk', Path(__file__).with_name('prepare_native_forge_api_sdk.py'))
m = importlib.util.module_from_spec(loader)
loader.loader.exec_module(m)
ATTEMPT = None
EVIDENCE = None


class Controls(unittest.TestCase):
    def test_exact_explicit_role_input_tool_mapping_and_outputs(self):
        receipt = m.v2.parse_json((ATTEMPT / 'sdk-receipt.json').read_bytes())
        self.assertEqual(m.TOOL, receipt['tool_sha256'])
        self.assertEqual(m.MAPPING, receipt['mapping_sha256'])
        self.assertEqual(m.OUTPUTS, receipt['outputs'])
        self.assertEqual('NONPRODUCTION_OFFICIAL_1_20_1', receipt['role'])
        for name, digest in m.OUTPUTS.items():
            self.assertEqual(digest, m.v2.sha((ATTEMPT / (name + '.jar')).read_bytes()))

    def test_same_corrected_production_root_and_all_raw_class_bytes(self):
        raw = (ATTEMPT / 'ffapi-original.jar').read_bytes()
        corrected = (ATTEMPT / 'ffapi-production-resource-corrected.jar').read_bytes()
        artifact = m.v2.load_spec()['artifacts'][0]
        self.assertEqual(m.FFAPI_RAW, m.v2.sha(raw))
        self.assertEqual(m.FFAPI_FINAL, m.v2.sha(corrected))
        self.assertEqual(corrected, m.v2.transform(raw, artifact['patches'])[0])
        self.assertEqual(m.v2.inventory(raw), m.v2.inventory(corrected))

    def test_every_nested_archive_mapped_once_and_nonclass_resources_preserved(self):
        def compare(raw, mapped):
            _, before = m.v2.archive(raw)
            _, after = m.v2.archive(mapped)
            self.assertEqual(before.keys(), after.keys())
            for name in before:
                if name.endswith('.jar'):
                    compare(before[name], after[name])
                elif not name.endswith('.class') and name.upper() != 'META-INF/MANIFEST.MF':
                    self.assertEqual(before[name], after[name], name)
        compare((ATTEMPT / 'ffapi-production-resource-corrected.jar').read_bytes(), (ATTEMPT / 'ffapi-native.jar').read_bytes())
        compare((ATTEMPT / 'moonlight-original.jar').read_bytes(), (ATTEMPT / 'moonlight-native.jar').read_bytes())
        _, root = m.v2.archive((ATTEMPT / 'ffapi-native.jar').read_bytes())
        for name, path in m.MODULES.items():
            self.assertEqual(root[path], (ATTEMPT / (name + '.jar')).read_bytes())

    def test_immutable_publication_and_wrong_tool_rejection(self):
        tool = ATTEMPT / 'installertools-1.4.4.jar'
        root = EVIDENCE / 'publication'
        def mapped(data, directory, tool, mapping, label, depth=0):
            return (ATTEMPT / ('ffapi-native.jar' if label == 'ffapi' else 'moonlight-native.jar')).read_bytes()
        with patch.object(m, 'rename', side_effect=mapped) as remapper:
            first, _ = m.prepare(root, tool, ATTEMPT / 'mapping.zip', ATTEMPT / 'ffapi-original.jar', ATTEMPT / 'moonlight-original.jar')
            files = list((root / 'maven').rglob('*.jar'))
            stats = {p: (p.stat().st_ino, p.stat().st_mtime_ns) for p in files}
            with patch.object(m, 'download', side_effect=AssertionError('immutable reuse must not access network')):
                second, reused = m.prepare(root, tool, ATTEMPT / 'mapping.zip')
            self.assertEqual(2, remapper.call_count, 'reuse must not launch another mapping process')
            self.assertEqual('REUSED_EXACT_PINNED_OFFICIAL_OUTPUTS', reused['mapping_execution'])
            self.assertNotEqual(first, second)
            self.assertEqual(stats, {p: (p.stat().st_ino, p.stat().st_mtime_ns) for p in files})
            foreign_root = EVIDENCE / 'foreign-publication'
            foreign = foreign_root / files[0].relative_to(root)
            foreign.parent.mkdir(parents=True)
            foreign.write_bytes(b'foreign coordinate')
            with self.assertRaises(m.v2.Rejected):
                m.prepare(foreign_root, tool, ATTEMPT / 'mapping.zip', ATTEMPT / 'ffapi-original.jar', ATTEMPT / 'moonlight-original.jar')
            self.assertEqual(b'foreign coordinate', foreign.read_bytes())
        wrong = EVIDENCE / 'wrong-tool.jar'; wrong.write_bytes(b'not an accepted SDK tool')
        with self.assertRaises(m.v2.Rejected):
            m.prepare(EVIDENCE / 'wrong-tool-stage', wrong, ATTEMPT / 'mapping.zip')
        failed = next((EVIDENCE / 'wrong-tool-stage/attempts').iterdir())
        self.assertEqual(wrong.read_bytes(), (failed / 'installertools-1.4.4.jar').read_bytes())
        self.assertTrue((failed / 'input-selection.json').is_file())
        self.assertTrue((failed / 'failure.txt').is_file())
        self.assertEqual((ATTEMPT / 'mapping.zip').read_bytes(), (failed / 'mapping.zip').read_bytes())

    def test_no_config_resolution_and_no_nested_runtime_duplicates(self):
        r = m.MODULE.parent.parent
        contract = (r / 'gradle/native-forge-api-fixture.gradle').read_text()
        build = (r / 'build.gradle.kts').read_text()
        self.assertNotIn('.resolve(', contract)
        self.assertNotIn('.singleFile', contract)
        self.assertNotIn('repositories.', contract)
        self.assertNotIn("System.getProperty('user.home')", contract)
        self.assertIn("dependsOn('compileJava')", contract)
        self.assertIn('files(sdkFile(name)).builtBy(prepare)', contract)
        self.assertIn("['base', 'interaction', 'resource', 'moonlight-native'].each", contract)
        self.assertLess(contract.index("['base', 'interaction', 'resource', 'moonlight-native'].each"), contract.index('if (enabled)'))
        self.assertIn("dependencies.add(sourceSets.nativeForgeApiFixture.compileOnlyConfigurationName, sdkFiles(name))", contract)
        self.assertIn("dependencies.add('runtimeOnly', sdkFiles('ffapi-native'))", contract)
        self.assertIn("dependencies.add('runtimeOnly', sdkFiles('moonlight-native'))", contract)
        self.assertNotIn("dependencies.add('runtimeOnly', sdkFiles('base'))", contract)
        self.assertIn('compileOnly(fg.deobf("curse.maven:selene-499980:8261419"))', build)
        self.assertNotIn('if (!withNativeForgeApiFixture) compileOnly', build)
        self.assertIn('if (withNativeForgeApiFixture) source(nativeForgeApiFixture)', build)
        self.assertIn('if (!withNativeForgeApiFixture) runtimeOnly(fg.deobf("curse.maven:selene-499980:8261419"))', build)
        self.assertIn('"**/nativeforgeapifixture/**"', build)
        self.assertIn('it.name == "better_compat_fixes.native_forge_api_fixture.mixins.json"', build)
        self.assertIn('if (withNativeForgeApiFixture) args("--mixin.config", "better_compat_fixes.native_forge_api_fixture.mixins.json")', build)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--attempt', type=Path, required=True)
    p.add_argument('--evidence', type=Path, required=True)
    args = p.parse_args()
    ATTEMPT, EVIDENCE = map(m.v2.regular_path, (args.attempt, args.evidence))
    EVIDENCE.mkdir(parents=True, exist_ok=False)
    unittest.main(argv=[__file__], verbosity=2)
