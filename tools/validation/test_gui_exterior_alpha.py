"""Guard exact alpha exports and reject unregistered source/behavior changes."""
import hashlib
import re
import struct
import subprocess
import unittest
from unittest.mock import patch

import gui_exterior_alpha as layer
from prism_210_history_projection import project_current_candidate

ROOT = layer.ROOT
HISTORIC_HOPPER_ARTWORK_TEST = 'tools/validation/test_hopper_steel_artwork.py'
ALPHA_COMMIT = 'a6436f1f3616bc2a5b380b7e18c899da755291e6'
COMBINED_BASE_COMMIT = 'd7dce0724698a069855d2284c758e8761edd17b6'
ALPHA_HOPPER_TEST_SHA256 = 'f5c8c01d0748cbcdd1f2ef872c592da71b54c861c2772aa7a7739d95ae6cd632'
COMBINED_HOPPER_TEST_SHA256 = 'f3a09873cb62b5125c5a599091c55c8b7a75235d13da0e114df71a45c1c42afe'


class ExteriorAlphaTests(unittest.TestCase):
    def test_fourteen_exact_runtime_exports_keep_source_dimensions(self):
        textures = layer.records()['textures']
        self.assertEqual(len(textures), 14)
        for path, record in textures.items():
            with self.subTest(path=path):
                data = (layer.ROOT / path).read_bytes()
                self.assertEqual(hashlib.sha256(data).hexdigest(), record['output_sha256'])
                self.assertEqual(list(struct.unpack('>II', data[16:24])), record['dimensions'])
                self.assertEqual(data[24:26], bytes((8, 6)))  # RGBA8
                self.assertEqual(layer.current_hash(path, record['source_sha256']), record['output_sha256'])
                self.assertGreater(record['alpha_zero_pixels'], 0)
                self.assertEqual(record['rgb_changed_pixels'], 0)
                self.assertEqual(record['protected_rgba_changed_pixels'], 0)
                self.assertEqual(record['canvas_edge_nonzero_alpha_pixels'], 0)

    def test_hash_updates_preserve_every_original_text_byte(self):
        for path, record in layer.records()['text_files'].items():
            with self.subTest(path=path):
                current = (layer.ROOT / path).read_bytes()
                if path == HISTORIC_HOPPER_ARTWORK_TEST:
                    # The exact alpha revision is retained in a643; #210's d7
                    # base has a later, independently reviewed test body. Assert
                    # both source identities explicitly instead of forcing an
                    # obsolete alpha snapshot over the current candidate.
                    alpha = subprocess.check_output(['git', 'show', f'{ALPHA_COMMIT}:{path}'], cwd=ROOT)
                    combined = subprocess.check_output(['git', 'show', f'{COMBINED_BASE_COMMIT}:{path}'], cwd=ROOT)
                    self.assertEqual(hashlib.sha256(alpha).hexdigest(), record['output_sha256'])
                    self.assertEqual(hashlib.sha256(combined).hexdigest(), COMBINED_HOPPER_TEST_SHA256)
                    self.assertEqual(hashlib.sha256(current).hexdigest(), COMBINED_HOPPER_TEST_SHA256)
                    continue
                self.assertEqual(hashlib.sha256(project_current_candidate(path, current)).hexdigest(), record['output_sha256'])
                previous = layer.project_history(path, current)
                self.assertEqual(hashlib.sha256(previous).hexdigest(), record['source_sha256'])
                self.assertEqual(layer.project_history(path, previous), previous)
                self.assertEqual(layer.current_hash(path, record['source_sha256']), record['output_sha256'])
                with self.assertRaises(AssertionError):
                    layer.project_history(path, current + b'\n')

    def test_all_java_changes_are_hash_literals_only(self):
        for path in layer.records()['text_files']:
            if path.endswith('.java'):
                with self.subTest(path=path):
                    current = (layer.ROOT / path).read_bytes()
                    alpha_stage = project_current_candidate(path, current)
                    previous = layer.project_history(path, alpha_stage)
                    self.assertEqual(re.sub(rb'[a-f0-9]{64}', b'<SHA256>', previous),
                                     re.sub(rb'[a-f0-9]{64}', b'<SHA256>', alpha_stage))

    def test_original_pin_cannot_be_replaced_by_an_unrelated_digest(self):
        record = layer.records()
        for path in (*record['textures'], *record['text_files']):
            with self.subTest(path=path), self.assertRaises(AssertionError):
                layer.current_hash(path, '0' * 64)

    def test_record_is_pinned_and_unrelated_paths_remain_strict(self):
        with patch.object(layer, 'RECORD_BYTES', layer.RECORD_BYTES + b'\n'):
            with self.assertRaises(AssertionError):
                layer.records()
        self.assertEqual(layer.current_hash('unrelated', 'unchanged'), 'unchanged')
        self.assertEqual(layer.project_history('unrelated', b'bytes'), b'bytes')

    def test_factory_pair_and_provisional_furnaces_remain_excluded(self):
        record = layer.records()
        self.assertEqual(set(record['excluded']), {
            'Factory Dropper', 'Factory Hopper', 'Small Lab Furnace', 'Small Mineral Smelter'})
        self.assertFalse(any('factory_steel_v2' in p or 'retained_dropper' in p for p in record['textures']))


if __name__ == '__main__':
    unittest.main()
