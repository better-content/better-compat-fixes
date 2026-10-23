#!/usr/bin/env python3
import json
import unittest
from rooty_witness import verify


def fixture():
    value = dict(schema='bc.native.rooty-soil-witness.v1', observerComplete=True,
                fullLogReconciliationRequired=True, callerPrefixLimit=8, recordLimit=128,
                byteLimit=131072, recordBytes=512, calls=1, finished=1, open=0, unsupported=1,
                dropped=0, overflow=0, missing=0, doubleForward=0, late=0, exceptions=0,
                records=[dict(species='dynamictrees:oak', soil='minecraft:seagrass', root=[1, 2, 3],
                              fertility=0, soilStateId=100, generation=dict(origin=[1, 3, 3], root=[1, 2, 3],
                              radius=4, facing=2, secondChance=False), thread='worldgen',
                              callerPrefix=[{'class': 'native.JoCode', 'method': 'generate', 'line': 271}],
                              warnings=1, setCalls=1, setBlock=dict(pos=[1, 2, 3], block='dynamictrees:rooty_dirt',
                              stateId=200, flags=3, returned=True), returned=False, exception=None)])
    value['recordBytes'] = sum(len(json.dumps(row, separators=(',', ':'))) + 1 for row in value['records'])
    return value


class RootyWitnessProtocolTests(unittest.TestCase):
    warning = 'Rooty Dirt block NOT FOUND for soil minecraft:seagrass\n'

    def check(self, value):
        return verify(json.dumps(value), self.warning, self.warning)

    def test_positive_is_diagnostic_only_and_failed_soil_still_blocks_product(self):
        result = self.check(fixture())
        self.assertTrue(result['diagnostic_complete'])
        self.assertFalse(result['product_qualification'])
        self.assertTrue(result['soil_operations_still_reject_product'])

    def test_each_fault_counter_rejects_even_if_complete_claim_is_forged(self):
        for key in ('open', 'dropped', 'overflow', 'missing', 'doubleForward', 'late', 'exceptions'):
            with self.subTest(key=key), self.assertRaises(ValueError):
                value = fixture(); value[key] = 1; self.check(value)

    def test_missing_or_extra_raw_warning_rejects(self):
        for debug, latest in ((self.warning, ''), ('', ''), (self.warning * 2, self.warning * 2)):
            with self.subTest(debug=debug, latest=latest), self.assertRaises(ValueError):
                verify(json.dumps(fixture()), debug, latest)

    def test_raw_soil_mismatch_rejects(self):
        with self.assertRaises(ValueError):
            verify(json.dumps(fixture()), self.warning.replace('seagrass', 'dirt'), self.warning)

    def test_unknown_schema_extra_payload_and_legacy_reject(self):
        for change in ({'schema': 'bc.native.rooty-soil-witness.v0'}, {'privatePayload': 'not allowed'},
                       {'observerComplete': False}, {'fullLogReconciliationRequired': False}):
            with self.subTest(change=change), self.assertRaises(ValueError):
                value = fixture(); value.update(change); self.check(value)

    def test_wrong_primitive_types_or_invented_success_reject(self):
        for key, bad in (('returned', True), ('warnings', True), ('setCalls', 2), ('fertility', '0'),
                         ('species', 'private\nNBT'), ('generation', None), ('callerPrefix', [])):
            with self.subTest(key=key), self.assertRaises(ValueError):
                value = fixture(); value['records'][0][key] = bad; self.check(value)

    def test_state_and_root_payload_strict(self):
        for field, bad in (('pos', [1, 2]), ('block', 'private\nNBT'), ('flags', True), ('returned', 1)):
            with self.subTest(field=field), self.assertRaises(ValueError):
                value = fixture(); value['records'][0]['setBlock'][field] = bad; self.check(value)

    def test_duplicate_keys_and_false_byte_accounting_reject(self):
        raw = json.dumps(fixture())
        with self.assertRaises(ValueError):
            verify(raw.replace('"open": 0', '"open": 0, "open": 0'), self.warning, self.warning)
        value = fixture(); value['recordBytes'] += 1
        with self.assertRaises(ValueError):
            self.check(value)

    def test_no_warn_empty_complete_report_is_not_product_acceptance(self):
        value = fixture(); value.update(records=[], unsupported=0, calls=0, finished=0, recordBytes=0)
        self.assertFalse(verify(json.dumps(value), '', '')['product_qualification'])


if __name__ == '__main__':
    unittest.main()
