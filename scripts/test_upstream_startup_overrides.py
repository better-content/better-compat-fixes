#!/usr/bin/env python3
"""Pure exact-original controls, including real Forge annotation scanning and Maven range semantics."""
import argparse
import copy
import importlib.util
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
module = importlib.util.spec_from_file_location('startup_builder', ROOT / 'scripts/build_upstream_startup_overrides.py')
builder = importlib.util.module_from_spec(module); module.loader.exec_module(builder)
TOOLS = [
    ('com.google.guava', 'guava', '31.1-jre', 'a42edc9cab792e39fe39bb94f3fca655ed157ff87a8af78e1d6ba5b07c4a00ab', 'https://repo.maven.apache.org/maven2/'),
    ('net.minecraftforge', 'fmlloader', '1.20.1-47.4.22', '136013541a11e43e405b49ec0a87a91a47222a51b4eb98245640b4512e973a0f', 'https://maven.minecraftforge.net/'),
    ('net.minecraftforge', 'forgespi', '7.0.1', '2298eaba25d066afd9b329de7f75e7bf78ef47f3045e6ecb0e40428d875e4b84', 'https://maven.minecraftforge.net/'),
    ('org.ow2.asm', 'asm', '9.7.1', '8cadd43ac5eb6d09de05faecca38b917a040bb9139c7edeb4cc81c740b713281', 'https://repo.maven.apache.org/maven2/'),
    ('org.apache.maven', 'maven-artifact', '3.9.1', '1b673d76cf8777344c6350d588533ea394dd93fe9a1b2bff319d96cb0dde74f2', 'https://repo.maven.apache.org/maven2/'),
    ('org.apache.commons', 'commons-lang3', '3.12.0', 'd919d904486c037f8d193412da0c92e22a9fa24230b9d67a57855c5c31c7e94e', 'https://repo.maven.apache.org/maven2/'),
]
JAVA = r'''
import java.nio.file.*; import java.util.*; import java.util.zip.*;
import org.objectweb.asm.ClassReader;
import net.minecraftforge.fml.loading.moddiscovery.ModClassVisitor;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.maven.artifact.versioning.*;
public class StartupScanControl {
 static Object canonical(Object value) {
  if(value instanceof net.minecraftforge.fml.loading.moddiscovery.ModAnnotation.EnumHolder e) return Map.of("enumDescriptor",e.getDesc(),"enumValue",e.getValue());
  if(value instanceof net.minecraftforge.fml.loading.moddiscovery.ModAnnotation a) return Map.of("annotationDescriptor",a.getASMType().getDescriptor(),"values",canonical(a.getValues()));
  if(value instanceof Map<?,?> m) {var out=new TreeMap<String,Object>();m.forEach((k,v)->out.put(k.toString(),canonical(v)));return out;}
  if(value instanceof List<?> l) return l.stream().map(StartupScanControl::canonical).toList();
  if(value instanceof int[] a) return Arrays.stream(a).boxed().toList();
  if(value instanceof long[] a) return Arrays.stream(a).boxed().toList();
  if(value instanceof double[] a) return Arrays.stream(a).boxed().toList();
  if(value instanceof Object[] a) return Arrays.stream(a).map(StartupScanControl::canonical).toList();
  if(value==null || value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof org.objectweb.asm.Type) return value;
  throw new AssertionError("unhandled real Forge annotation value "+value.getClass().getName());
 }
 static Map<String,Map<String,Object>> scan(String jar) throws Exception {
  var data=new ModFileScanData();
  try(var zip=new ZipFile(jar)) {
   var entries=zip.entries(); while(entries.hasMoreElements()) {
    var entry=entries.nextElement(); if(!entry.getName().endsWith(".class")) continue;
    var visitor=new ModClassVisitor();
    try(var input=zip.getInputStream(entry)) {new ClassReader(input).accept(visitor,0);}
    visitor.buildData(data.getClasses(),data.getAnnotations());
   }
  }
  var out=new TreeMap<String,Map<String,Object>>();
  for(var a:data.getAnnotations()) {
   var key=a.clazz().getClassName()+"|"+a.memberName()+"|"+a.targetType()+"|"+a.annotationType().getDescriptor();
   var values=new HashMap<String,Object>();a.annotationData().forEach((k,v)->values.put(k,canonical(v)));
   if(out.put(key,values)!=null) throw new AssertionError("duplicate annotation identity");
  }
  return out;
 }
 public static void main(String[] args) throws Exception {
  var before=scan(args[0]);var after=scan(args[1]); if(!before.keySet().equals(after.keySet()))throw new AssertionError("scan population drift");
  int changed=0;
  for(var key:before.keySet()) {
   var old=before.get(key);var revised=after.get(key);
   if(key.startsWith("dev.uncandango.alltheleaks.leaks.common.mods.citadel.UntrackedIssue001|") && key.endsWith("|Ldev/uncandango/alltheleaks/annotation/Issue;")) {
    if(!"citadel".equals(old.get("modId")) || !"[2.5.4,)".equals(old.get("versionRange")))throw new AssertionError("wrong original scan");
    var expected=new HashMap<>(old);expected.put("versionRange","[2.5.4,2.6.3),(2.6.3,)");
    if(!expected.equals(revised))throw new AssertionError("wrong corrected scan");changed++;
   } else if(!old.equals(revised))throw new AssertionError("other annotation changed: "+key);
  }
  if(changed!=1)throw new AssertionError("exactly one changed issue required");
  var range=VersionRange.createFromVersionSpec("[2.5.4,2.6.3),(2.6.3,)");
  String[] versions={"2.5.3","2.5.4","2.6.2","2.6.3-SNAPSHOT","2.6.3-rc1","2.6.3","2.6.3.0","2.6.4","2.7.0","3.0.0"};
  boolean[] expected={false,true,true,true,true,false,false,true,true,true};
  for(int i=0;i<versions.length;i++)if(range.containsVersion(new DefaultArtifactVersion(versions[i]))!=expected[i])throw new AssertionError("Maven applicability "+versions[i]);
  System.out.println("REAL_FORGE_SCAN_PASS annotations="+before.size()+" changed=1 issue_class_initialization=NOT_REQUESTED");
  System.out.println("REAL_MAVEN_UNION_PASS cases="+versions.length+" Maven-equivalent-2.6.3-spellings-excluded=true");
 }
}
'''


def tools(evidence):
    result = []
    for group, artifact, version, digest, origin in TOOLS:
        filename = artifact + '-' + version + '.jar'
        roots = [Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle'))), Path.home() / '.gradle']
        matches = []
        for root in roots:
            matches += list((root / 'caches/modules-2/files-2.1' / group / artifact / version).glob('*/' + filename))
        selected = None
        for path in matches:
            body = builder.read_file(path)
            builder.legacy.digest_matches(body, digest)
            selected = path; break
        if selected is None:
            selected = evidence / filename
            url = origin + group.replace('.', '/') + '/' + artifact + '/' + version + '/' + filename
            with urllib.request.urlopen(url, timeout=20) as response: body = response.read(16 * 1024 * 1024 + 1)
            builder.require(len(body) <= 16 * 1024 * 1024, 'tool download size')
            builder.legacy.digest_matches(body, digest); selected.write_bytes(body)
        result.append(selected)
    return result


class Controls(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.spec = builder.load_spec(); cls.atl = builder.read_file(INPUTS / cls.spec['artifact']['input'])
        builder.legacy.digest_matches(cls.atl, cls.spec['artifact']['sha256'])
        _, entries = builder.legacy.archive(cls.atl); cls.raw = entries[cls.spec['patch']['path']]
        cls.fixed, cls.correction = builder.patch_class(cls.raw, cls.spec['patch'])

    def case(self, label): return EVIDENCE / (label + '-' + next(tempfile._get_candidate_names()))

    def test_actual_original_deterministic_build_and_recursive_single_class_change(self):
        a, b = self.case('build-a'), self.case('build-b')
        first = builder.build(INPUTS, a); second = builder.build(INPUTS, b)
        self.assertEqual(first, second)
        self.assertEqual((a / self.spec['artifact']['output']).read_bytes(), (b / self.spec['artifact']['output']).read_bytes())
        self.assertEqual(880, first['artifacts'][0]['class_count'])
        self.assertFalse(first['citadel_abi']['dataMap_present'])
        self.assertNotIn('dataMap', first['citadel_abi']['field_names'])

    def test_only_derived_utf8_length_payload_changes(self):
        offset = self.correction['utf8_length_offset']; size = len(self.spec['patch']['before'].encode())
        self.assertEqual(self.raw[:offset], self.fixed[:offset])
        delta = len(self.spec['patch']['after'].encode()) - size
        self.assertEqual(self.raw[offset + 2 + size:], self.fixed[offset + 2 + size + delta:])
        self.assertEqual(10, self.correction['constant_pool_index'])

    def test_wrong_annotation_mod_range_and_descriptor_rejected(self):
        for key, value in [('annotation', 'Lother/Issue;'), ('mod_id', 'other'), ('element', 'description'), ('before', '[2.0,)')]:
            patch = dict(self.spec['patch'], **{key: value})
            with self.assertRaises(Exception): builder.patch_class(self.raw, patch)

    def test_malformed_extra_and_truncated_class_rejected(self):
        for data in [b'', self.raw[:20], self.raw + b'X', b'XXXX' + self.raw[4:]]:
            with self.assertRaises(Exception): builder.patch_class(data, self.spec['patch'])

    def test_shared_range_constant_rejected(self):
        import struct
        class TrackingClass(builder.ClassFile):
            def __init__(self, data):
                self.string_operands = []
                super().__init__(data)
            def element(self):
                start = self.pos
                value = super().element()
                if value[0] == 's': self.string_operands.append((start + 1, value[1]))
                return value
        parsed = TrackingClass(self.raw)
        issue = next(values for kind, values in parsed.annotations if kind == self.spec['patch']['annotation'])
        range_index = issue['versionRange'][1]
        description_index = issue['description'][1]
        offset = next(offset for offset, index in parsed.string_operands if index == description_index)
        bad = self.raw[:offset] + struct.pack('>H', range_index) + self.raw[offset + 2:]
        # Rebind the class pin in this negative fixture to reach the sharing oracle,
        # rather than merely observing the outer whole-class digest rejection.
        contract = dict(self.spec['patch'], before_sha256=builder.sha(bad))
        with self.assertRaises(Exception): builder.patch_class(bad, contract)

    def test_original_drift_rejected_without_repair(self):
        inputs = self.case('wrong-originals'); inputs.mkdir()
        for item in (self.spec['artifact'], self.spec['citadel']): shutil.copyfile(INPUTS / item['input'], inputs / item['input'])
        path = inputs / self.spec['artifact']['input']; path.write_bytes(path.read_bytes() + b'X')
        with self.assertRaises(Exception): builder.build(inputs, self.case('wrong-build'))
        self.assertTrue(path.read_bytes().endswith(b'X'))

    def test_existing_output_and_symlink_input_rejected(self):
        output = self.case('existing'); output.mkdir()
        with self.assertRaises(Exception): builder.build(INPUTS, output)
        inputs = self.case('symlinks'); inputs.mkdir()
        for item in (self.spec['artifact'], self.spec['citadel']): (inputs / item['input']).symlink_to(INPUTS / item['input'])
        with self.assertRaises(Exception): builder.build(inputs, self.case('symlink-output'))

    def test_metadata_and_every_non_target_resource_preserved(self):
        corrected = builder.legacy.rewrite_zip(self.atl, {self.spec['patch']['path']: self.fixed})
        old_infos, old = builder.legacy.archive(self.atl); new_infos, new = builder.legacy.archive(corrected)
        self.assertEqual(list(old), list(new))
        for name in old:
            if name != self.spec['patch']['path']: self.assertEqual(old[name], new[name], name)
        for before, after in zip(old_infos, new_infos):
            for key in ('filename', 'date_time', 'comment', 'extra', 'external_attr', 'internal_attr', 'flag_bits', 'compress_type'):
                self.assertEqual(getattr(before, key), getattr(after, key), key)

    def test_real_forge_scan_and_maven_union_applicability(self):
        output = self.case('forge-scan-build'); builder.build(INPUTS, output)
        java = self.case('java'); java.mkdir(); source = java / 'StartupScanControl.java'; source.write_text(JAVA)
        cp = os.pathsep.join(map(str, tools(java)))
        for name, argv in [('compile', ['javac', '-cp', cp, '-d', str(java), str(source)]),
                           ('scan', ['java', '-cp', str(java) + os.pathsep + cp, 'StartupScanControl',
                                     str(INPUTS / self.spec['artifact']['input']), str(output / self.spec['artifact']['output'])])]:
            result = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60, text=True)
            (java / (name + '.log')).write_text(result.stdout)
            self.assertEqual(0, result.returncode, result.stdout)
            if name == 'scan':
                self.assertIn('REAL_FORGE_SCAN_PASS', result.stdout); self.assertIn('REAL_MAVEN_UNION_PASS', result.stdout)
                print(result.stdout, end='')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('--inputs', type=Path, required=True); parser.add_argument('--evidence', type=Path)
    args, extra = parser.parse_known_args(); INPUTS = builder.legacy.regular_path(args.inputs)
    if args.evidence:
        EVIDENCE = builder.legacy.regular_path(args.evidence); EVIDENCE.mkdir()
    else:
        base = Path.home() / '.tmp/startup-override-controls'; base.mkdir(parents=True, exist_ok=True)
        EVIDENCE = Path(tempfile.mkdtemp(dir=base))
    print('Retained startup controls:', EVIDENCE, flush=True)
    unittest.main(argv=[__file__, *extra], verbosity=2)
