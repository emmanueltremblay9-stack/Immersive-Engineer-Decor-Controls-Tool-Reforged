"""Checks that native fluid client evidence cannot pass on stale or incomplete data."""
import copy
import unittest
from client_gameplay import validate_fluidlogging_proof
from client_mixin_smoke import SmokeFailure

class NativeFluidloggingProofTests(unittest.TestCase):
    def setUp(self):
        self.proof = {"suite": "native-fluidlogging-client-v1", "nonce": "current",
                      "status": "passed", "actual_integrated_world": True, "cases": [
            {"block": "immersive_engineer_decor_controls_tool_reforged:" + block,
             "fluid": fluid, "sync": sync, "client_world_and_chunk_source": True,
             "client_drain_cleared": True, "native_textured_vertices": 16}
            for block in ("steel_catwalk", "steel_catwalk_stairs", "steel_catwalk_ta", "steel_railing")
            for fluid in ("minecraft:water", "minecraft:lava", "immersiveengineering:creosote")
            for sync in ("update", "chunk")]}

    def test_complete_matrix_passes(self):
        self.assertEqual(validate_fluidlogging_proof(self.proof, "current"), self.proof)

    def test_stale_failed_or_non_world_proof_fails(self):
        for field, value in (("nonce", "old"), ("status", "running"), ("actual_integrated_world", False)):
            proof = copy.deepcopy(self.proof)
            proof[field] = value
            with self.assertRaises(SmokeFailure):
                validate_fluidlogging_proof(proof, "current")

    def test_missing_duplicate_or_untextured_case_fails(self):
        for mutation in ("missing", "duplicate", "no_mesh", "stale_source"):
            proof = copy.deepcopy(self.proof)
            if mutation == "missing": proof["cases"].pop()
            elif mutation == "duplicate": proof["cases"][-1] = proof["cases"][0]
            elif mutation == "no_mesh": proof["cases"][0]["native_textured_vertices"] = 0
            else: proof["cases"][0]["client_drain_cleared"] = False
            with self.assertRaises(SmokeFailure):
                validate_fluidlogging_proof(proof, "current")
