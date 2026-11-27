"""Dual-parent byte and mutation evidence; no runtime qualification claim."""
import copy
import hashlib
import json
from pathlib import Path
import unittest
from breaker_current_main_integration import (
    LEGACY_CYCLE_LAYER, LEGACY_CYCLE_RECORD, project_legacy_cycle,
    LAYER, MERGE_LAYER, MERGE_RECORD, RECORD, assert_merge_record, assert_record,
    integrated_current_hash, merge_current_hash, project, project_merge_tree,
)
ROOT=Path(__file__).resolve().parents[2]
class CurrentMainIntegrationTests(unittest.TestCase):
    def test_both_manifests_and_parent_reconstruction(self):
        for name in ('milking-qualification/production-pins.json','solar-v2-reference/preserved-baseline-sha256.json'):
            manifest=json.loads((ROOT/'docs/testing'/name).read_text())
            assert_record(manifest)
            assert_merge_record(manifest)
        for path,record in MERGE_RECORD['files'].items():
            data=(ROOT/path).read_bytes()
            for parent in ('main','breaker'):
                restored=project_merge_tree(path,data,parent)
                self.assertEqual(hashlib.sha256(restored).hexdigest(),record[parent+'_sha256'])
                self.assertEqual(merge_current_hash(path,record[parent+'_sha256']),hashlib.sha256(data).hexdigest())
        for path,record in RECORD['files'].items():
            data=(ROOT/path).read_bytes()
            for parent in ('main','breaker'):
                restored=project(path,data,parent)
                self.assertEqual(hashlib.sha256(restored).hexdigest(),record[parent+'_sha256'])
                branch_hash=integrated_current_hash(path,record[parent+'_sha256'])
                self.assertEqual(merge_current_hash(path,branch_hash),hashlib.sha256(data).hexdigest())
    def test_every_record_field_is_pinned(self):
        for key in RECORD:
            mutated=copy.deepcopy(RECORD);mutated[key]='tampered'
            with self.subTest(key=key),self.assertRaises(AssertionError):assert_record({LAYER:mutated,MERGE_LAYER:MERGE_RECORD})
        with self.assertRaises(AssertionError):assert_record({})
        for path,record in RECORD['files'].items():
            for field in record:
                mutated=copy.deepcopy(RECORD);mutated['files'][path][field]='tampered'
                with self.subTest(path=path,field=field),self.assertRaises(AssertionError):assert_record({LAYER:mutated,MERGE_LAYER:MERGE_RECORD})
        for key in MERGE_RECORD:
            mutated=copy.deepcopy(MERGE_RECORD);mutated[key]='tampered'
            with self.subTest(merge_key=key),self.assertRaises(AssertionError):assert_merge_record({MERGE_LAYER:mutated})
        for path,record in MERGE_RECORD['files'].items():
            for field in record:
                mutated=copy.deepcopy(MERGE_RECORD);mutated['files'][path][field]='tampered'
                with self.subTest(merge_path=path,field=field),self.assertRaises(AssertionError):assert_merge_record({MERGE_LAYER:mutated})

    def test_legacy_cycle_migration_preserves_the_previous_source(self):
        record = LEGACY_CYCLE_RECORD
        path = record['path']
        current = (ROOT/path).read_bytes()
        previous = project_legacy_cycle(current)
        self.assertEqual(hashlib.sha256(previous).hexdigest(), MERGE_RECORD['files'][path]['integrated_sha256'])
        with self.assertRaisesRegex(AssertionError, 'Merged bytes changed'):
            project_merge_tree(path, previous, 'breaker')

        manifest = {LAYER: RECORD, MERGE_LAYER: MERGE_RECORD}
        with self.assertRaisesRegex(AssertionError, 'Legacy Block Breaker cycle migration record changed'):
            assert_record(manifest)
        assert_record({**manifest, LEGACY_CYCLE_LAYER: record})
        for key in record:
            with self.subTest(key=key), self.assertRaisesRegex(AssertionError, 'Legacy Block Breaker cycle migration record changed'):
                assert_record({**manifest, LEGACY_CYCLE_LAYER: {**record, key: 'tampered'}})

    def test_each_merge_projection_is_exact_and_rejects_mutation(self):
        for path,record in MERGE_RECORD['files'].items():
            data=(ROOT/path).read_bytes()
            for parent in ('main','breaker'):
                restored=project_merge_tree(path,data,parent)
                self.assertEqual(hashlib.sha256(restored).hexdigest(),record[parent+'_sha256'])
                self.assertEqual(merge_current_hash(path,record[parent+'_sha256']),hashlib.sha256(data).hexdigest())
                with self.subTest(path=path,parent=parent),self.assertRaises(AssertionError):
                    project_merge_tree(path,data+b'\nmutation',parent)
    def test_bytes_cannot_be_accepted_by_rehashing_manifest(self):
        for path,record in RECORD['files'].items():
            data=(ROOT/path).read_bytes()+b'// unrelated edit\n'
            mutated=copy.deepcopy(RECORD);mutated['files'][path]['integrated_sha256']=hashlib.sha256(data).hexdigest()
            with self.subTest(path=path),self.assertRaises(AssertionError):assert_record({LAYER:mutated,MERGE_LAYER:MERGE_RECORD})
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
