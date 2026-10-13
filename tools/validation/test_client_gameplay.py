"""Evidence-gate regressions, separate from the actual native gameplay run."""

import copy
from pathlib import Path
import unittest

from client_gameplay import EXPECTED_CASES, SUITE, validate_proof
from client_mixin_smoke import SmokeFailure


class NativeGameplayProofTests(unittest.TestCase):
    def setUp(self):
        self.nonce = "a" * 32
        self.proof = {"suite": SUITE, "nonce": self.nonce, "status": "passed", "expected_cases": 10,
                      "actual_integrated_world": True, "cases": []}
        for index, name in enumerate(sorted(EXPECTED_CASES)):
            mode, approach = name.split("-")
            expected = 1 if mode == "survival" else 0
            self.proof["cases"].append({"case": name, "mode": mode, "approach": approach,
                "immediate_client_prediction_lamp_only": True, "authoritative_lamp_only": True,
                "release_allows_native_deck_mining": True, "immediate_client_prediction_deck_removed": True,
                "authoritative_deck_removed": True, "lamp_sequence": 2 * index + 1,
                "lamp_ack_applied": 2 * index + 1, "deck_sequence": 2 * index + 2,
                "deck_ack_applied": 2 * index + 2, "held_ticks": 25, "held_attack_cancellations": 27,
                "held_extra_mining_packets": 0, "lamp_drops_or_inventory": expected,
                "lamp_tool_damage": expected, "native_mining_ticks": 20 * expected})
        self.logs = f"IEDCTR_CLIENT_GAMEPLAY PASSED nonce={self.nonce} cases=10"

    def check(self, proof=None, **overrides):
        args = dict(proof=self.proof if proof is None else proof, nonce=self.nonce,
                    logs=self.logs, exit_code=0, timed_out=False, crash_files=[])
        args.update(overrides)
        return validate_proof(**args)

    def test_complete_proof(self):
        self.assertEqual(self.check(), self.proof)

    def test_rejects_timeout_even_with_success_marker(self):
        with self.assertRaises(SmokeFailure): self.check(timed_out=True)

    def test_rejects_failed_process(self):
        with self.assertRaises(SmokeFailure): self.check(exit_code=1)

    def test_rejects_load_only_stale_failed_and_wrong_scope(self):
        for patch in ({}, {"status": "running"}, {"status": "failed"}, {"nonce": "b" * 32},
                      {"suite": "client-mixin-load"}, {"actual_integrated_world": False}):
            proof = copy.deepcopy(self.proof) if patch else {}
            proof.update(patch)
            with self.subTest(patch=patch), self.assertRaises(SmokeFailure): self.check(proof)

    def test_rejects_incomplete_duplicate_and_mislabeled_cases(self):
        for mutate in (lambda p: p["cases"].pop(),
                       lambda p: p["cases"].__setitem__(1, p["cases"][0]),
                       lambda p: p["cases"][0].update(mode="survival")):
            proof = copy.deepcopy(self.proof)
            mutate(proof)
            with self.assertRaises(SmokeFailure): self.check(proof)

    def test_requires_each_prediction_authority_and_input_assertion(self):
        for key, value in (("immediate_client_prediction_lamp_only", False), ("authoritative_lamp_only", False),
                           ("release_allows_native_deck_mining", False), ("immediate_client_prediction_deck_removed", False),
                           ("authoritative_deck_removed", False), ("lamp_ack_applied", 0), ("deck_ack_applied", 0),
                           ("deck_sequence", 1), ("held_ticks", 0), ("held_attack_cancellations", 0),
                           ("held_extra_mining_packets", 1), ("lamp_drops_or_inventory", 1), ("lamp_tool_damage", 1),
                           ("native_mining_ticks", -1)):
            proof = copy.deepcopy(self.proof)
            proof["cases"][0][key] = value
            with self.subTest(key=key), self.assertRaises(SmokeFailure): self.check(proof)

    def test_requires_real_sequences_not_boolean_json_values(self):
        proof = copy.deepcopy(self.proof)
        proof["cases"][0]["lamp_sequence"] = True
        with self.assertRaises(SmokeFailure): self.check(proof)

    def test_rejects_crashes_fatals_and_missing_completion_marker(self):
        for args in ({"crash_files": [Path("crash.txt")]}, {"logs": self.logs + "\nNoSuchMethodError"},
                     {"logs": "Loaded Engineer's Decor"}, {"logs": self.logs.replace(self.nonce, "b" * 32)}):
            with self.subTest(args=args), self.assertRaises(SmokeFailure): self.check(**args)


if __name__ == "__main__":
    unittest.main()
