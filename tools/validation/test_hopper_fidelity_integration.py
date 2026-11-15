"""Exact integration identity and negative non-Hopper mutations, not runtime tests."""
import hashlib
from pathlib import Path
import unittest
from unittest.mock import patch
from breaker_current_main_integration import project_merge_tree
import hopper_fidelity_integration as layer
from freezer_v5_reconciliation import current_hash

ROOT = Path(__file__).resolve().parents[2]

class HopperFidelityIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.merged_data = (ROOT / layer.PATH).read_bytes()
        self.data = project_merge_tree(layer.PATH, self.merged_data, 'main')

    def test_exact_inverse_reconstructs_immutable_history(self):
        old = layer.project_history(self.data)
        self.assertEqual(hashlib.sha256(old).hexdigest(), layer.BASE_SHA256)
        self.assertEqual(current_hash(layer.PATH, layer.BASE_SHA256), hashlib.sha256(self.merged_data).hexdigest())
        self.assertNotEqual(old, self.data)

    def test_changed_current_bytes_rejected_inside_and_outside_hopper(self):
        for marker in (b'hopperTickTimer', b'tickMetalCraftingTable', b'freezer', b'milking'):
            self.assertIn(marker, self.data)
            with self.subTest(marker=marker), self.assertRaises(AssertionError):
                layer.project_history(self.data.replace(marker, marker+b'_MUTATED', 1))
        with self.assertRaises(AssertionError):
            layer.project_history(self.data+b'\n')

    def test_unknown_baseline_rejected_other_paths_not_exempted(self):
        with self.assertRaises(AssertionError):
            current_hash(layer.PATH, '0'*64)
        self.assertEqual(current_hash('unrelated.java', 'a'*64), 'a'*64)

    def test_projection_changes_cannot_be_omitted_or_forged(self):
        for changes in (layer.CHANGES[:-1], layer.CHANGES+((0,0,'','tampered'),)):
            with patch.object(layer, 'CHANGES', changes), self.assertRaises(AssertionError):
                layer.project_history(self.data)
        changes=list(layer.CHANGES)
        start,end,current,previous=changes[0]
        changes[0]=(start,end,current+'tampered',previous)
        with patch.object(layer,'CHANGES',tuple(changes)), self.assertRaises(AssertionError):
            layer.project_history(self.data)

    def test_strength_exact_inverse_and_immutable_prior_hash(self):
        merged_data = (ROOT / layer.BLOCKS_PATH).read_bytes()
        data = project_merge_tree(layer.BLOCKS_PATH, merged_data, 'main')
        self.assertEqual(hashlib.sha256(layer.project_blocks_history(data)).hexdigest(), layer.BLOCKS_BASE_SHA256)
        self.assertEqual(current_hash(layer.BLOCKS_PATH, layer.BLOCKS_BASE_SHA256), hashlib.sha256(merged_data).hexdigest())
        with self.assertRaises(AssertionError):
            current_hash(layer.BLOCKS_PATH, '0'*64)
        for marker in (b'15.0F', b'MachineKind.FACTORY_DROPPER', b'MachineKind.SMALL_FREEZER'):
            self.assertIn(marker, data)
            with self.subTest(marker=marker), self.assertRaises(AssertionError):
                layer.project_blocks_history(data.replace(marker, marker+b'_MUTATED', 1))
        with patch.object(layer, 'BLOCKS_BEFORE', layer.BLOCKS_BEFORE+'tampered'), self.assertRaises(AssertionError):
            layer.project_blocks_history(data)

if __name__=='__main__': unittest.main()
