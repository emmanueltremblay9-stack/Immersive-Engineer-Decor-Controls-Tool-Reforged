"""Verify exact delta reconstruction and fail-closed mutations, not native evidence."""
import hashlib
import unittest
from unittest.mock import patch
import missing_gui_integration as layer
from prism_210_history_projection import expected_current_sha256


class MissingGuiIntegrationTests(unittest.TestCase):
    def test_every_delta_reconstructs_pinned_base(self):
        for path, record in layer.records().items():
            with self.subTest(path=path):
                candidate = (layer.ROOT / path).read_bytes()
                historical = layer.project_history(path, candidate)
                candidate_pin = expected_current_sha256(path) or layer.SUPERSEDED_CURRENT_SHA256.get(path) or record['current_sha256']
                self.assertEqual(hashlib.sha256(candidate).hexdigest(), candidate_pin)
                historical_pin = layer.SUPERSEDED_BASE_SHA256.get(path, record['base_sha256'])
                self.assertEqual(hashlib.sha256(historical).hexdigest(), historical_pin)
                current_hash = layer.SUPERSEDED_CURRENT_SHA256.get(path, record['current_sha256'])
                self.assertEqual(layer.current_hash(path, record['base_sha256']), current_hash)
                with self.assertRaises(AssertionError):
                    layer.project_history(path, candidate + b'\n')
                with self.assertRaises(AssertionError):
                    layer.current_hash(path, '0' * 64)

    def test_record_cannot_be_changed_silently(self):
        with patch.object(layer, 'RECORD_SHA256', '0' * 64), self.assertRaises(AssertionError):
            layer.records()

    def test_unrelated_path_not_exempted(self):
        self.assertEqual(layer.project_history('unrelated', b'bytes'), b'bytes')
        self.assertEqual(layer.current_hash('unrelated', 'hash'), 'hash')


if __name__ == '__main__':
    unittest.main()
