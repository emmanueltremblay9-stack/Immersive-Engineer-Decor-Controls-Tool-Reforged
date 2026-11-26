"""Exact immutable history projection and fail-closed mutation tests for #210."""
import json
import subprocess
import unittest

from prism_210_history_projection import (
    BASE_COMMIT, HEAD_COMMIT, RECORD, ROOT, FREEZER_NATIVE_PORT_RECORD, LATE_VALIDATION_RECORD,
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
        port = json.loads(FREEZER_NATIVE_PORT_RECORD.read_bytes())
        self.assertEqual(late["files"][smelter]["old_sha256"], core[smelter]["states"][-1]["sha256"])
        self.assertEqual(late["files"][thermal]["old_sha256"], core[thermal]["states"][-1]["sha256"])
        self.assertEqual(late["files"][v2]["previous_source_commit"], "1712b6d5682faaeb84e0be103898b0109aa3c87a")
        for path in (smelter, thermal, v2):
            candidate = (ROOT / path).read_bytes()
            late_candidate = (subprocess.check_output(["git", "show", port["base_commit"] + ":" + path], cwd=ROOT)
                              if path in port["files"] else candidate)
            self.assertEqual(sha256(late_candidate), late["files"][path]["new_sha256"], path)
            if path in port["files"]:
                self.assertEqual(sha256(candidate), port["files"][path]["new_sha256"], path)
            projected = project_current_candidate(path, candidate, strict=True)
            expected = (core[path]["states"][0]["sha256"] if path in core
                        else late["files"][path]["old_sha256"])
            self.assertEqual(sha256(projected), expected, path)
        source = subprocess.check_output(["git", "show", late["files"][v2]["previous_source_commit"] + ":" + v2], cwd=ROOT)
        self.assertEqual(sha256(source), late["files"][v2]["old_sha256"])

    def test_freezer_native_port_is_exact_and_reversible_from_e429(self):
        port = json.loads(FREEZER_NATIVE_PORT_RECORD.read_bytes())
        self.assertEqual(port["schema"], "iedctr-prism-freezer-native-port-v1")
        self.assertEqual(port["base_commit"], "e429a1fa1551c4532eb3e59d531ccf86570ff9b5")
        self.assertEqual(set(port["files"]), {
            "src/freezerPackagedValidation/java/com/oblixorprime/iedctrtest/freezer/PackagedRuntimeProof.java",
            "src/main/java/com/oblixorprime/engineersdecorreforged/client/ThermalVerticalScreen.java",
            "src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeClientMiningValidation.java",
            "src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeFreezerReferenceValidation.java",
            "tools/validation/freezer_packaged_native.py",
            "tools/validation/freezer_packaged_proof.py",
            "tools/validation/freezer_recovery_tooltip_proof.py",
            "tools/validation/freezer_reference_native.py",
            "tools/validation/test_freezer_packaged_driver.py",
            "tools/validation/test_freezer_packaged_proof.py",
            "tools/validation/test_freezer_recovery_tooltip_proof.py",
            "tools/validation/test_freezer_recovery_tooltip_wrap.py",
            "tools/validation/test_freezer_reference_native.py",
            "tools/validation/test_freezer_render_sync.py",
        })
        core_files = json.loads(RECORD.read_bytes())["files"]
        late_files = json.loads(LATE_VALIDATION_RECORD.read_bytes())["files"]
        for path, layer in port["files"].items():
            with self.subTest(path=path):
                parent = subprocess.check_output(["git", "show", port["base_commit"] + ":" + path], cwd=ROOT)
                current = (ROOT / path).read_bytes()
                self.assertEqual(sha256(parent), layer["old_sha256"])
                self.assertEqual(sha256(current), layer["new_sha256"])
                expected = (core_files[path]["states"][0]["sha256"] if path in core_files else
                            late_files[path]["old_sha256"] if path in late_files else layer["old_sha256"])
                self.assertEqual(sha256(project_current_candidate(path, current, strict=True)), expected)
                with self.assertRaisesRegex(AssertionError, "Unrecognized #210 candidate bytes"):
                    project_current_candidate(path, current + b"outside exact freezer port\n", strict=True)


if __name__ == "__main__":
    unittest.main()
