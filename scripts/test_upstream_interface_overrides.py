#!/usr/bin/env python3
"""Actual-original positive/adversarial controls; retains every attempt; no runtime acceptance claim."""
import argparse
import copy
import io
from pathlib import Path
import struct
import tempfile
import unittest
import zipfile
import build_upstream_interface_overrides as m

parser = argparse.ArgumentParser()
parser.add_argument('--inputs', required=True, type=Path)
parser.add_argument('--provider-jar', required=True, type=Path)
parser.add_argument('--evidence', type=Path)
args, remaining = parser.parse_known_args()
EVIDENCE = args.evidence or Path(tempfile.mkdtemp(prefix='bc-interface-controls-', dir='/home/dev/workspace_artifacts'))
EVIDENCE.mkdir(parents=True, exist_ok=True)
print('Retained interface controls:', EVIDENCE, flush=True)


class Controls(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.spec=m.load_spec();cls.originals={a['input']:m.base.read_file(args.inputs/a['input']) for a in cls.spec['artifacts']}
        cls.vs=cls.spec['artifacts'][1];cls.class_data=m.base.archive(cls.originals[cls.vs['input']])[1][m.CLASS]
        cls.receipt=m.build(args.inputs,EVIDENCE/'qualified-a',provider_jar=args.provider_jar)
        m.build(args.inputs,EVIDENCE/'qualified-b',provider_jar=args.provider_jar)

    def rejects(self, action):
        with self.assertRaises((m.base.Rejected,KeyError,UnicodeError,ValueError,OSError)):action()
    def mutated_spec(self,name,mutate):
        spec=copy.deepcopy(self.spec);mutate(spec);path=EVIDENCE/(name+'.json');path.write_bytes(m.base.encode_json(spec));return path
    def provider_mutant(self,name,mutate):
        _,entries=m.base.archive(m.base.read_file(args.provider_jar));mutate(entries)
        parent=EVIDENCE/name;parent.mkdir();path=parent/m.PROVIDER
        with zipfile.ZipFile(path,'w',compression=zipfile.ZIP_DEFLATED) as z:
            for key,body in entries.items():z.writestr(key,body)
        return path

    def test_01_repeatable_actual_outputs(self):
        for p in (EVIDENCE/'qualified-a').iterdir():self.assertEqual(p.read_bytes(),(EVIDENCE/'qualified-b'/p.name).read_bytes())
        self.assertEqual(self.receipt['runtime_acceptance'],'NOT_EVALUATED')
        self.assertEqual(self.receipt['publication'],'NOT_AUTHORIZED_BY_THIS_RECEIPT')
        self.assertEqual(sum(r['class_count'] for r in self.receipt['artifacts']),8062)

    def test_02_original_three_private_static_handlers_are_unchanged(self):
        m.validate_original_handlers(self.class_data)
        fixed=m.base.archive((EVIDENCE/'qualified-a'/self.vs['output']).read_bytes())[1][m.CLASS]
        self.assertEqual(fixed,self.class_data)
        self.assertEqual({k[0].decode() for k in m.method_access_offsets(fixed)},set(m.METHODS))
        self.assertTrue(all(flags==10 for offset,flags in m.method_access_offsets(fixed).values()))
        for row in self.receipt['artifacts']:
            self.assertTrue(row['class_bytes_invariant']);self.assertEqual(row['class_access_patches'],[])
            self.assertEqual(row['before_class_inventory_sha256'],row['after_class_inventory_sha256'])

    def test_visibility_and_synthetic_do_not_solve_third_interface_injection_stage(self):
        # Structural history control only. Native dd3a2d57 rejected synthetic-only at PREINJECT.
        def prepares(flags):return bool(flags & (1|0x1000))
        def applies_static(flags):return not flags&8 or bool(flags&2)
        interface_injectors_supported_in_085=False
        self.assertFalse(prepares(10));self.assertTrue(prepares(9));self.assertFalse(applies_static(9))
        self.assertTrue(prepares(0x100a));self.assertTrue(applies_static(0x100a))
        for flags in (10,9,0x100a):self.assertFalse(prepares(flags) and applies_static(flags) and interface_injectors_supported_in_085)

    def test_03_all_recursive_classes_in_both_archives_are_identical(self):
        for a in self.spec['artifacts']:
            self.assertEqual(m.base.inventory(self.originals[a['input']]),m.base.inventory((EVIDENCE/'qualified-a'/a['output']).read_bytes()))

    def test_04_only_exact_json_relocation_and_coupled_transfer_change(self):
        for a in self.spec['artifacts']:
            old_infos,old=m.base.archive(self.originals[a['input']]);new_infos,new=m.base.archive((EVIDENCE/'qualified-a'/a['output']).read_bytes())
            self.assertEqual([i.filename for i in old_infos],[i.filename for i in new_infos])
            self.assertEqual({k for k in old if old[k]!=new[k]},{a['patches'][0]['path']})
            for x,y in zip(old_infos,new_infos):
                for key in ('date_time','compress_type','comment','extra','create_system','create_version','extract_version','flag_bits','internal_attr','external_attr'):
                    self.assertEqual(getattr(x,key),getattr(y,key),(x.filename,key))
            before=m.base.parse_json(old[a['patches'][0]['path']]);after=m.base.parse_json(new[a['patches'][0]['path']]);moved=a['patches'][0]['move_to_client'][0]
            self.assertNotIn(moved,after['mixins']);self.assertEqual(after['client'].count(moved),1)
            expected=copy.deepcopy(before);expected['mixins'].remove(moved);expected.setdefault('client',[]).append(moved)
            if 'interface_transfer' in a:expected['mixins'].remove(m.TRANSFER_MIXIN)
            self.assertEqual(expected,after)

    def test_05_unknown_class_byte_or_trailer_rejected(self):
        for offset in (0,100,3687,len(self.class_data)-1):
            bad=bytearray(self.class_data);bad[offset]^=1;self.rejects(lambda:m.validate_original_handlers(bytes(bad)))
        self.rejects(lambda:m.validate_original_handlers(self.class_data+b'\x00'))

    def test_06_class_access_patching_and_transfer_scope_drift_rejected(self):
        for name,mutate in [
            ('old-public',lambda s:s['artifacts'][1].update(class_access={'before':10,'after':9})),
            ('old-synthetic',lambda s:s['artifacts'][1].update(class_access={'before':10,'after':0x100a})),
            ('transfer-mixin',lambda s:s['artifacts'][1]['interface_transfer'].update(mixin='other.Mixin')),
            ('transfer-class',lambda s:s['artifacts'][1]['interface_transfer'].update(original_class='other.class')),
            ('transfer-hash',lambda s:s['artifacts'][1]['interface_transfer'].update(original_class_sha256='0'*64)),
            ('transfer-provider',lambda s:s['artifacts'][1]['interface_transfer'].update(provider_artifact='other.jar')),
            ('transfer-extra',lambda s:s['artifacts'][1]['interface_transfer']['provider_entries'].update(other='0'*64)),
            ('transfer-missing',lambda s:s['artifacts'][1]['interface_transfer']['provider_entries'].pop(m.PROVIDER_CONFIG)),
            ('transfer-scope',lambda s:s['artifacts'][0].update(interface_transfer=s['artifacts'][1]['interface_transfer']))]:
            self.rejects(lambda p=self.mutated_spec(name,mutate):m.load_spec(p))

    def test_07_unknown_side_resource_or_artifact_rejected(self):
        for name,mutate in [('side',lambda s:s['artifacts'][0]['patches'][0].update(move_to_client=['OtherMixin'])),
                            ('resource',lambda s:s['artifacts'][1]['patches'][0].update(path='other.json')),
                            ('artifact',lambda s:s['artifacts'][0].update(input='unknown.jar')),
                            ('output',lambda s:s['artifacts'][0].update(output='../escape.jar')),
                            ('stale-output',lambda s:s['artifacts'][1].update(output=s['artifacts'][1]['output'].replace('.3.jar','.2.jar')))]:
            self.rejects(lambda p=self.mutated_spec(name,mutate):m.load_spec(p))

    def test_08_existing_or_nested_output_rejected_without_input_changes(self):
        old={n:m.sha(b) for n,b in self.originals.items()}
        self.rejects(lambda:m.build(args.inputs,EVIDENCE/'qualified-a',provider_jar=args.provider_jar))
        self.rejects(lambda:m.build(args.inputs,args.inputs/'nested-output',provider_jar=args.provider_jar))
        self.assertEqual(old,{n:m.sha(m.base.read_file(args.inputs/n)) for n in old})

    def test_09_wrong_actual_archive_pin_rejected(self):
        p=self.mutated_spec('wrong-pin',lambda s:s['artifacts'][0].update(sha256='0'*64))
        self.rejects(lambda:m.build(args.inputs,EVIDENCE/'wrong-pin-output',p,args.provider_jar))
        self.assertFalse((EVIDENCE/'wrong-pin-output').exists())

    def test_10_duplicate_signed_and_unsafe_archives_rejected(self):
        for name,entries in [('duplicate',[('same',b'a'),('same',b'b')]),('signed',[('META-INF/X.SF',b'signature')]),('unsafe',[('../escape',b'x')])]:
            stream=io.BytesIO()
            with zipfile.ZipFile(stream,'w') as z:
                for key,body in entries:z.writestr(key,body)
            self.rejects(lambda:m.base.archive(stream.getvalue()))

    def test_11_symlink_and_truncated_class_rejected(self):
        link=EVIDENCE/'input-symlink';link.symlink_to(args.inputs,target_is_directory=True)
        self.rejects(lambda:m.build(link,EVIDENCE/'symlink-output',provider_jar=args.provider_jar))
        for cut in (0,8,100,len(self.class_data)-1):self.rejects(lambda:m.method_access_offsets(self.class_data[:cut]))

    def test_12_legacy_five_recipe_and_builder_remain_exact(self):
        self.assertEqual(m.sha(m.base.read_file(m.base.__file__)),'09669c8333ce94358b30f8c303f9885a459388ccfd02d0c867b8b77ece472ed6')
        self.assertEqual(m.sha(m.base.read_file(m.base.SPEC)),'caa9067b44e3e8f119cad614dc133f3e1df69ffe6fdc523c1c2df3232517d378')

    def test_13_transfer_cannot_be_built_without_bound_source_provider(self):
        self.rejects(lambda:m.build(args.inputs,EVIDENCE/'missing-provider'))
        self.assertFalse((EVIDENCE/'missing-provider').exists())
        for name,mutate in [('missing-provider-class',lambda e:e.pop(m.PROVIDER_PREFIX+'compat/vibration/VibrationWorldCoordinates.class')),
                            ('changed-provider-class',lambda e:e.update({m.PROVIDER_PREFIX+'compat/vibration/VibrationWorldCoordinates.class':b'changed'}))]:
            provider=self.provider_mutant(name,mutate)
            self.rejects(lambda:m.build(args.inputs,EVIDENCE/(name+'-output'),provider_jar=provider))
            self.assertFalse((EVIDENCE/(name+'-output')).exists())

    def test_14_manifest_registration_required_independently_of_entry_hashes(self):
        provider=self.provider_mutant('unregistered-config',lambda e:e.update({'META-INF/MANIFEST.MF':b'Manifest-Version: 1.0\r\nMixinConfigs: other.mixins.json\r\n\r\n'}))
        self.rejects(lambda:m.validate_provider(provider,self.vs['interface_transfer']))

    def test_15_original_handler_flags_cannot_be_widened_even_with_class_hash_rebound(self):
        original_hash=m.ORIGINAL_CLASS
        try:
            for flags in (1,9,8,0x100a,0x1009):
                bad=bytearray(self.class_data)
                for offset,_ in m.method_access_offsets(self.class_data).values():struct.pack_into('>H',bad,offset,flags)
                m.ORIGINAL_CLASS=m.sha(bytes(bad));self.rejects(lambda:m.validate_original_handlers(bytes(bad)))
        finally:m.ORIGINAL_CLASS=original_hash


if __name__=='__main__':unittest.main(argv=[__file__,*remaining],verbosity=2)
