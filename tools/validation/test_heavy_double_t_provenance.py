import hashlib
from pathlib import Path
import unittest
from unittest.mock import patch
import heavy_double_t_provenance as layer

ROOT=Path(__file__).resolve().parents[2]
class HeavyProvenanceTests(unittest.TestCase):
    def test_exact_additions_restore_original_bytes_and_accept_original(self):
        for path,(old,new,inserted,previous) in layer.ROWS.items():
            data=(ROOT/path).read_bytes()
            self.assertEqual(hashlib.sha256(data).hexdigest(),new)
            restored=layer.project(path,data)
            self.assertEqual(hashlib.sha256(restored).hexdigest(),old)
            self.assertEqual(layer.project(path,restored),restored)
            self.assertEqual(layer.current_hash(path,old),new)
    def test_unknown_hashes_and_changes_fail_closed(self):
        for path,(old,new,inserted,previous) in layer.ROWS.items():
            data=(ROOT/path).read_bytes()
            changes=[data+b' ',data.replace(inserted,inserted+inserted,1),
                     data.replace(b'heavy_industrial_double_t_support',b'changed_id',1),
                     data.replace(b'steel_double_t_support',b'changed_legacy_id',1)]
            if path.endswith('.java'):
                changes.append(data.replace(b'15.0F',b'14.0F',1))
            else:
                changes.append(data.replace(b'FE',b'RF',1))
            for changed in changes:
                self.assertNotEqual(changed,data)
                with self.assertRaises(AssertionError):layer.project(path,changed)
            with self.assertRaises(AssertionError):layer.current_hash(path,'0'*64)
    def test_wrong_known_path_and_forged_projection_rejected(self):
        paths=list(layer.ROWS)
        with self.assertRaises(AssertionError):layer.project(paths[1],(ROOT/paths[0]).read_bytes())
        for path,(old,new,inserted,previous) in layer.ROWS.items():
            with patch.dict(layer.ROWS,{path:(old,new,inserted,previous+b'bad')}):
                with self.assertRaises(AssertionError):layer.project(path,(ROOT/path).read_bytes())
    def test_other_paths_not_transformed(self):
        self.assertEqual(layer.project('other',b'payload'),b'payload')
        self.assertEqual(layer.current_hash('other','pin'),'pin')
