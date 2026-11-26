"""Exact immutable history projection and fail-closed mutation tests for #210."""
import json
import unittest

from prism_210_history_projection import (
    BASE_COMMIT, HEAD_COMMIT, RECORD, ROOT,
    expected_current_sha256, project_current_candidate, sha256,
)


class Prism210HistoryProjectionTests(unittest.TestCase):
    def test_candidate_commit_range_and_every_reversible_layer_are_exact(self):
        data = json.loads(RECORD.read_bytes())
        self.assertEqual(data["base_commit"], BASE_COMMIT)
        self.assertEqual(data["head_commit"], HEAD_COMMIT)
        self.assertGreaterEqual(len(data["files"]), 10)
        for path, record in data["files"].items():
            current = (ROOT / path).read_bytes()
            self.assertEqual(sha256(current), expected_current_sha256(path), path)
            historical = project_current_candidate(path, current, strict=True)
            self.assertEqual(sha256(historical), record["states"][0]["sha256"], path)
            self.assertEqual(sha256(current), expected_current_sha256(path), "projection mutated source " + path)

    def test_any_unrecorded_candidate_mutation_is_rejected(self):
        data = json.loads(RECORD.read_bytes())
        for path, record in data["files"].items():
            current = (ROOT / path).read_bytes()
            changed = current + b"// outside exact #210 hunk\n"
            with self.subTest(path=path), self.assertRaisesRegex(AssertionError, "Unrecognized #210 candidate bytes"):
                project_current_candidate(path, changed, strict=True)

    def test_projection_starts_from_the_candidate_and_returns_exact_historical_bytes(self):
        data = json.loads(RECORD.read_bytes())
        for path, record in data["files"].items():
            candidate = (ROOT / path).read_bytes()
            history = project_current_candidate(path, candidate, strict=True)
            with self.subTest(path=path):
                self.assertEqual(sha256(history), record["states"][0]["sha256"])
                self.assertEqual(sha256(candidate), expected_current_sha256(path))

    def test_late_native_validation_layers_are_exact_and_reversible(self):
        # These post-276 validation-only changes are separately pinned; no
        # production behavior or artwork is admitted through this projection.
        late = json.loads((RECORD.parent / "prism_210_late_validation_projection.json").read_bytes())
        smelter = "src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeSmelterGuiValidation.java"
        thermal = "src/main/java/com/oblixorprime/engineersdecorreforged/client/ThermalVerticalScreen.java"
        v2 = "tools/validation/factory_placer_v2_reference_native.py"
        self.assertEqual(late["schema"], "iedctr-prism-late-validation-projection-v1")
        self.assertEqual(late["base_commit"], HEAD_COMMIT)
        self.assertEqual(set(late["files"]), {smelter, thermal, v2})
        core = json.loads(RECORD.read_bytes())["files"]
        self.assertEqual(late["files"][smelter]["old_sha256"], core[smelter]["states"][-1]["sha256"])
        self.assertEqual(late["files"][thermal]["old_sha256"], core[thermal]["states"][-1]["sha256"])
        self.assertEqual(late["files"][v2]["previous_source_commit"], "1712b6d5682faaeb84e0be103898b0109aa3c87a")
        for path in (smelter, thermal, v2):
            candidate = (ROOT / path).read_bytes()
            self.assertEqual(sha256(candidate), late["files"][path]["new_sha256"], path)
            projected = project_current_candidate(path, candidate, strict=True)
            expected = (core[path]["states"][0]["sha256"] if path in core
                        else late["files"][path]["old_sha256"])
            self.assertEqual(sha256(projected), expected, path)
        import subprocess
        source = subprocess.check_output(["git", "show", late["files"][v2]["previous_source_commit"] + ":" + v2], cwd=ROOT)
        self.assertEqual(sha256(source), late["files"][v2]["old_sha256"])


if __name__ == "__main__":
    unittest.main()
