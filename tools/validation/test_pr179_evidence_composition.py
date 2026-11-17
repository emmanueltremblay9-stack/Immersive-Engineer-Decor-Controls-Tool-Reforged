"""Regression coverage for exact preservation-layer composition, not GUI evidence."""
import hashlib
import json
from pathlib import Path
import runpy
import unittest
from unittest.mock import patch

from breaker_current_main_integration import MERGE_RECORD, project_merge_tree
from check_milking_qualification import current_hash
from solar_cell_integration import FILES, project_history

ROOT = Path(__file__).resolve().parents[2]


class EvidenceCompositionTests(unittest.TestCase):
    def test_milking_resolves_every_historical_pin_to_current_bytes(self):
        pins = json.loads((ROOT / 'docs/testing/milking-qualification/production-pins.json').read_text())
        for name, historical in pins['sha256'].items():
            with self.subTest(path=name):
                self.assertEqual(current_hash(name, historical), hashlib.sha256((ROOT / name).read_bytes()).hexdigest())

    def test_milking_merge_paths_reject_unknown_parent_hash(self):
        for name in MERGE_RECORD['files']:
            with self.subTest(path=name), self.assertRaises(AssertionError):
                current_hash(name, '0' * 64)

    def test_solar_composes_all_locales_back_to_immutable_history(self):
        for name, row in FILES.items():
            with self.subTest(path=name):
                historical = project_history(name, project_merge_tree(name, (ROOT / name).read_bytes(), 'main'))
                self.assertEqual(hashlib.sha256(historical).hexdigest(), row['baseline_sha256'])

    def test_actual_solar_guard_rejects_mutation_in_every_locale(self):
        original_read = Path.read_bytes
        guard = ROOT / 'tools/validation/check_solar_v2_reference_wiring.py'
        for name in FILES:
            target = ROOT / name
            def altered_read(path):
                data = original_read(path)
                return data + b'\nmutation' if path == target else data
            with self.subTest(path=name), patch.object(Path, 'read_bytes', altered_read):
                with self.assertRaisesRegex(AssertionError, 'Merged bytes changed'):
                    runpy.run_path(str(guard))


if __name__ == '__main__':
    unittest.main()
