#!/usr/bin/env python3
"""Actual-original positive and adversarial controls; retains every attempt."""
import argparse
import copy
import io
import json
from pathlib import Path
import shutil
import struct
import tempfile
import unittest
import zipfile
import build_upstream_interface_overrides as m

parser = argparse.ArgumentParser()
parser.add_argument('--inputs', required=True, type=Path)
parser.add_argument('--evidence', type=Path)
args, remaining = parser.parse_known_args()
EVIDENCE = args.evidence or Path(tempfile.mkdtemp(prefix='bc-interface-controls-', dir='/home/dev/workspace_artifacts'))
EVIDENCE.mkdir(parents=True, exist_ok=True)
print('Retained interface controls:', EVIDENCE, flush=True)


class Controls(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.spec = m.load_spec()
        cls.originals = {a['input']: m.base.read_file(args.inputs / a['input']) for a in cls.spec['artifacts']}
        cls.vs = cls.spec['artifacts'][1]
        cls.class_data = m.base.archive(cls.originals[cls.vs['input']])[1][m.CLASS]
        cls.receipt = m.build(args.inputs, EVIDENCE/'qualified-a')
        m.build(args.inputs, EVIDENCE/'qualified-b')

    def rejects(self, action):
        with self.assertRaises((m.base.Rejected, KeyError, UnicodeError, ValueError, OSError)):
            action()

    def mutated_spec(self, name, mutate):
        spec = copy.deepcopy(self.spec); mutate(spec)
        path = EVIDENCE/(name+'.json'); path.write_bytes(m.base.encode_json(spec)); return path

    def test_01_repeatable_actual_outputs(self):
        for p in (EVIDENCE/'qualified-a').iterdir():
            self.assertEqual(p.read_bytes(), (EVIDENCE/'qualified-b'/p.name).read_bytes())
        self.assertEqual(self.receipt['runtime_acceptance'], 'NOT_EVALUATED')
        self.assertEqual(self.receipt['publication'], 'NOT_AUTHORIZED_BY_THIS_RECEIPT')
        self.assertEqual(sum(r['class_count'] for r in self.receipt['artifacts']), 8062)

    def test_02_exact_three_static_words_and_every_other_byte(self):
        new, edits = m.promote_static_handlers(self.class_data)
        self.assertEqual({e['method'] for e in edits}, set(m.METHODS))
        self.assertEqual([i for i,(a,b) in enumerate(zip(self.class_data,new)) if a != b],
                         sorted(e['offset']+1 for e in edits))
        for e in edits:
            self.assertEqual(struct.unpack_from('>H',self.class_data,e['offset'])[0],10)
            self.assertEqual(struct.unpack_from('>H',new,e['offset'])[0],9)
        expected = bytearray(self.class_data)
        for e in edits: struct.pack_into('>H',expected,e['offset'],9)
        self.assertEqual(bytes(expected),new)

    def test_03_all_recursive_classes_except_exact_interface_invariant(self):
        for a in self.spec['artifacts']:
            before = m.base.inventory(self.originals[a['input']])
            after = m.base.inventory((EVIDENCE/'qualified-a'/a['output']).read_bytes())
            self.assertEqual(set(before),set(after))
            changed = [k for k in before if before[k]!=after[k]]
            self.assertEqual(changed,[m.CLASS] if a['input'].startswith('valkyrienskies') else [])

    def test_04_only_exact_json_relocation_and_class_words_change(self):
        for a in self.spec['artifacts']:
            old_infos,old=m.base.archive(self.originals[a['input']]); new_infos,new=m.base.archive((EVIDENCE/'qualified-a'/a['output']).read_bytes())
            self.assertEqual([i.filename for i in old_infos],[i.filename for i in new_infos])
            changed={k for k in old if old[k]!=new[k]}
            self.assertEqual(changed,{a['patches'][0]['path']}|({m.CLASS} if 'class_access'in a else set()))
            for x,y in zip(old_infos,new_infos):
                for key in ('date_time','compress_type','comment','extra','create_system','create_version','extract_version','flag_bits','internal_attr','external_attr'):
                    self.assertEqual(getattr(x,key),getattr(y,key),(x.filename,key))
            before=m.base.parse_json(old[a['patches'][0]['path']]);after=m.base.parse_json(new[a['patches'][0]['path']]);moved=a['patches'][0]['move_to_client'][0]
            self.assertNotIn(moved,after['mixins']);self.assertEqual(after['client'].count(moved),1)
            expected=copy.deepcopy(before);expected['mixins'].remove(moved);expected.setdefault('client',[]).append(moved)
            self.assertEqual(expected,after)

    def test_05_unknown_class_byte_or_trailer_rejected(self):
        for offset in (0,100,3687,len(self.class_data)-1):
            bad=bytearray(self.class_data);bad[offset]^=1
            self.rejects(lambda:m.promote_static_handlers(bytes(bad)))
        self.rejects(lambda:m.promote_static_handlers(self.class_data+b'\x00'))

    def test_06_static_preservation_and_exact_methods_are_not_optional(self):
        for name,mutate in [('nonstatic',lambda s:s['artifacts'][1]['class_access'].update(after=1)),
                            ('extra',lambda s:s['artifacts'][1]['class_access']['methods'].update(other='()V')),
                            ('missing',lambda s:s['artifacts'][1]['class_access']['methods'].pop('destSourcePos')),
                            ('descriptor',lambda s:s['artifacts'][1]['class_access']['methods'].update(destWorldPos='()V')),
                            ('scope',lambda s:s['artifacts'][0].update(class_access=s['artifacts'][1]['class_access']))]:
            self.rejects(lambda p=self.mutated_spec(name,mutate):m.load_spec(p))

    def test_07_unknown_side_resource_or_artifact_rejected(self):
        for name,mutate in [('side',lambda s:s['artifacts'][0]['patches'][0].update(move_to_client=['OtherMixin'])),
                            ('resource',lambda s:s['artifacts'][1]['patches'][0].update(path='other.json')),
                            ('artifact',lambda s:s['artifacts'][0].update(input='unknown.jar')),
                            ('output',lambda s:s['artifacts'][0].update(output='../escape.jar'))]:
            self.rejects(lambda p=self.mutated_spec(name,mutate):m.load_spec(p))

    def test_08_existing_or_nested_output_rejected_without_input_changes(self):
        old={n:m.sha(b)for n,b in self.originals.items()}
        self.rejects(lambda:m.build(args.inputs,EVIDENCE/'qualified-a'))
        self.rejects(lambda:m.build(args.inputs,args.inputs/'nested-output'))
        self.assertEqual(old,{n:m.sha(m.base.read_file(args.inputs/n))for n in old})

    def test_09_wrong_actual_archive_pin_rejected(self):
        p=self.mutated_spec('wrong-pin',lambda s:s['artifacts'][0].update(sha256='0'*64))
        self.rejects(lambda:m.build(args.inputs,EVIDENCE/'wrong-pin-output',p))
        self.assertFalse((EVIDENCE/'wrong-pin-output').exists())

    def test_10_duplicate_signed_and_unsafe_archives_rejected(self):
        for name,entries in [('duplicate',[('same',b'a'),('same',b'b')]),
                             ('signed',[('META-INF/X.SF',b'signature')]),('unsafe',[('../escape',b'x')])]:
            stream=io.BytesIO()
            with zipfile.ZipFile(stream,'w') as z:
                for key,body in entries:z.writestr(key,body)
            self.rejects(lambda:m.base.archive(stream.getvalue()))

    def test_11_symlink_and_truncated_class_rejected(self):
        link=EVIDENCE/'input-symlink';link.symlink_to(args.inputs,target_is_directory=True)
        self.rejects(lambda:m.build(link,EVIDENCE/'symlink-output'))
        for cut in (0,8,100,len(self.class_data)-1):self.rejects(lambda:m.method_access_offsets(self.class_data[:cut]))

    def test_12_legacy_five_recipe_and_builder_remain_exact(self):
        self.assertEqual(m.sha(m.base.read_file(m.base.__file__)),'09669c8333ce94358b30f8c303f9885a459388ccfd02d0c867b8b77ece472ed6')
        self.assertEqual(m.sha(m.base.read_file(m.base.SPEC)),'caa9067b44e3e8f119cad614dc133f3e1df69ffe6fdc523c1c2df3232517d378')


if __name__=='__main__':unittest.main(argv=[__file__,*remaining],verbosity=2)
