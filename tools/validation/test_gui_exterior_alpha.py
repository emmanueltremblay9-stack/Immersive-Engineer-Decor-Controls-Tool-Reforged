"""Guard exact alpha exports and reject unregistered source/behavior changes."""
import hashlib
import re
import struct
import unittest
from unittest.mock import patch

import gui_exterior_alpha as layer


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
                self.assertEqual(hashlib.sha256(current).hexdigest(), record['output_sha256'])
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
                    previous = layer.project_history(path, current)
                    self.assertEqual(re.sub(rb'[a-f0-9]{64}', b'<SHA256>', previous),
                                     re.sub(rb'[a-f0-9]{64}', b'<SHA256>', current))

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
