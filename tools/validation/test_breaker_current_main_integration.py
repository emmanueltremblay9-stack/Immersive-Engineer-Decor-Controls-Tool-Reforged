"""Dual-parent byte and mutation evidence; no runtime qualification claim."""
import copy
import hashlib
import json
from pathlib import Path
import unittest
from breaker_current_main_integration import LAYER, RECORD, assert_record, project, integrated_current_hash
ROOT=Path(__file__).resolve().parents[2]
class CurrentMainIntegrationTests(unittest.TestCase):
    def test_both_manifests_and_parent_reconstruction(self):
        for name in ('milking-qualification/production-pins.json','solar-v2-reference/preserved-baseline-sha256.json'):
            manifest=json.loads((ROOT/'docs/testing'/name).read_text())
            assert_record(manifest)
        for path,record in RECORD['files'].items():
            data=(ROOT/path).read_bytes()
            for parent in ('main','breaker'):
                restored=project(path,data,parent)
                self.assertEqual(hashlib.sha256(restored).hexdigest(),record[parent+'_sha256'])
                self.assertEqual(integrated_current_hash(path,record[parent+'_sha256']),hashlib.sha256(data).hexdigest())
    def test_every_record_field_is_pinned(self):
        for key in RECORD:
            mutated=copy.deepcopy(RECORD);mutated[key]='tampered'
            with self.subTest(key=key),self.assertRaises(AssertionError):assert_record({LAYER:mutated})
        with self.assertRaises(AssertionError):assert_record({})
        for path,record in RECORD['files'].items():
            for field in record:
                mutated=copy.deepcopy(RECORD);mutated['files'][path][field]='tampered'
                with self.subTest(path=path,field=field),self.assertRaises(AssertionError):assert_record({LAYER:mutated})
    def test_bytes_cannot_be_accepted_by_rehashing_manifest(self):
        for path,record in RECORD['files'].items():
            data=(ROOT/path).read_bytes()+b'// unrelated edit\n'
            mutated=copy.deepcopy(RECORD);mutated['files'][path]['integrated_sha256']=hashlib.sha256(data).hexdigest()
            with self.subTest(path=path),self.assertRaises(AssertionError):assert_record({LAYER:mutated})
            for parent in ('main','breaker'):
                with self.assertRaises(AssertionError):project(path,data,parent)
            with self.assertRaises(AssertionError):integrated_current_hash(path,'0'*64)
    def test_original_parent_bytes_are_not_current_bytes(self):
        for path,record in RECORD['files'].items():
            data=(ROOT/path).read_bytes()
            for parent in ('main','breaker'):
                restored=project(path,data,parent)
                if record[parent+'_sha256']!=record['integrated_sha256']:
                    with self.assertRaises(AssertionError):project(path,restored,parent)
if __name__=='__main__':unittest.main()
