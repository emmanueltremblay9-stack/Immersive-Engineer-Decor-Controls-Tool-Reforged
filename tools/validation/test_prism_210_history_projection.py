"""Exact immutable history projection and fail-closed mutation tests for #210."""
import json
import subprocess
import unittest

from prism_210_history_projection import (
    BASE_COMMIT, HEAD_COMMIT, RECORD, ROOT, FREEZER_NATIVE_PORT_RECORD, FREEZER_RECOVERY_TOOLTIP_RECORD,
    FREEZER_RECOVERY_KEY_RECORD, FREEZER_ENERGY_SCISSOR_RECORD, FREEZER_QA_FOLLOWUP_RECORD,
    LATE_VALIDATION_RECORD, _freezer_qa_followup_record, _electrical_controls_record,
    _reverse_layer, expected_current_sha256,
    project_current_candidate, sha256,
)


def reverse_electrical_controls(path, source):
    layer = _electrical_controls_record()["files"].get(path)
    return _reverse_layer(path, source, layer) if layer is not None else source


def reverse_qa_followup(path, source):
    source = reverse_electrical_controls(path, source)
    layer = _freezer_qa_followup_record()["files"].get(path)
    return _reverse_layer(path, source, layer) if layer is not None else source


def reverse_energy_scissor(path, source, record):
    source = reverse_qa_followup(path, source)
    layer = record["files"].get(path)
    return _reverse_layer(path, source, layer) if layer is not None else source


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
        recovery = json.loads(FREEZER_RECOVERY_TOOLTIP_RECORD.read_bytes())
        key_layer = json.loads(FREEZER_RECOVERY_KEY_RECORD.read_bytes())
        scissor = json.loads(FREEZER_ENERGY_SCISSOR_RECORD.read_bytes())
        self.assertEqual(late["files"][smelter]["old_sha256"], core[smelter]["states"][-1]["sha256"])
        self.assertEqual(late["files"][thermal]["old_sha256"], core[thermal]["states"][-1]["sha256"])
        self.assertEqual(late["files"][v2]["previous_source_commit"], "1712b6d5682faaeb84e0be103898b0109aa3c87a")
        for path in (smelter, thermal, v2):
            candidate = (ROOT / path).read_bytes()
            late_candidate = (subprocess.check_output(["git", "show", port["base_commit"] + ":" + path], cwd=ROOT)
                              if path in port["files"] else candidate)
            self.assertEqual(sha256(late_candidate), late["files"][path]["new_sha256"], path)
            if path in port["files"]:
                candidate_before_recovery = reverse_energy_scissor(path, candidate, scissor)
                key = key_layer["files"].get(path)
                candidate_before_recovery = (_reverse_layer(path, candidate_before_recovery, key)
                                             if key is not None else candidate_before_recovery)
                top = recovery["files"].get(path)
                if top is not None:
                    candidate_before_recovery = _reverse_layer(path, candidate_before_recovery, top)
                self.assertEqual(sha256(candidate_before_recovery), port["files"][path]["new_sha256"], path)
            projected = project_current_candidate(path, candidate, strict=True)
            expected = (core[path]["states"][0]["sha256"] if path in core
                        else late["files"][path]["old_sha256"])
            self.assertEqual(sha256(projected), expected, path)
        source = subprocess.check_output(["git", "show", late["files"][v2]["previous_source_commit"] + ":" + v2], cwd=ROOT)
        self.assertEqual(sha256(source), late["files"][v2]["old_sha256"])

    def test_freezer_native_port_is_exact_and_reversible_from_e429(self):
        port = json.loads(FREEZER_NATIVE_PORT_RECORD.read_bytes())
        scissor = json.loads(FREEZER_ENERGY_SCISSOR_RECORD.read_bytes())
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
        recovery = json.loads(FREEZER_RECOVERY_TOOLTIP_RECORD.read_bytes())
        key_layer = json.loads(FREEZER_RECOVERY_KEY_RECORD.read_bytes())
        self.assertEqual(recovery["schema"], "iedctr-prism-freezer-recovery-tooltip-wrap-v1")
        self.assertEqual(recovery["base_commit"], "1a2963e3120987eee036007b209ef87cf70b44ae")
        for path, layer in port["files"].items():
            with self.subTest(path=path):
                parent = subprocess.check_output(["git", "show", port["base_commit"] + ":" + path], cwd=ROOT)
                candidate = (ROOT / path).read_bytes()
                current = reverse_energy_scissor(path, candidate, scissor)
                self.assertEqual(sha256(parent), layer["old_sha256"])
                key = key_layer["files"].get(path)
                if key is not None:
                    current = _reverse_layer(path, current, key)
                top = recovery["files"].get(path)
                if top is not None:
                    self.assertEqual(top["old_sha256"], layer["new_sha256"])
                    self.assertEqual(sha256(_reverse_layer(path, current, top)), layer["new_sha256"])
                else:
                    self.assertEqual(sha256(current), layer["new_sha256"])
                expected = (core_files[path]["states"][0]["sha256"] if path in core_files else
                            late_files[path]["old_sha256"] if path in late_files else layer["old_sha256"])
                self.assertEqual(sha256(project_current_candidate(path, candidate, strict=True)), expected)
                with self.assertRaisesRegex(AssertionError, "Unrecognized #210 candidate bytes"):
                    project_current_candidate(path, candidate + b"outside exact freezer port\n", strict=True)

    def test_freezer_recovery_tooltip_layer_is_exact_and_reversible(self):
        recovery = json.loads(FREEZER_RECOVERY_TOOLTIP_RECORD.read_bytes())
        scissor = json.loads(FREEZER_ENERGY_SCISSOR_RECORD.read_bytes())
        port = json.loads(FREEZER_NATIVE_PORT_RECORD.read_bytes())
        key_layer = json.loads(FREEZER_RECOVERY_KEY_RECORD.read_bytes())
        core = json.loads(RECORD.read_bytes())["files"]
        late = json.loads(LATE_VALIDATION_RECORD.read_bytes())["files"]
        self.assertEqual(recovery["schema"], "iedctr-prism-freezer-recovery-tooltip-wrap-v1")
        self.assertEqual(recovery["base_commit"], "1a2963e3120987eee036007b209ef87cf70b44ae")
        self.assertGreaterEqual(len(recovery["files"]), 6)
        for path, layer in recovery["files"].items():
            with self.subTest(path=path):
                parent = subprocess.check_output(["git", "show", recovery["base_commit"] + ":" + path], cwd=ROOT)
                candidate = (ROOT / path).read_bytes()
                current = reverse_energy_scissor(path, candidate, scissor)
                self.assertEqual(sha256(parent), layer["old_sha256"])
                key = key_layer["files"].get(path)
                if key is not None:
                    current = _reverse_layer(path, current, key)
                self.assertEqual(sha256(current), layer["new_sha256"])
                self.assertEqual(sha256(_reverse_layer(path, current, layer)), layer["old_sha256"])
                expected = (core[path]["states"][0]["sha256"] if path in core else
                            late[path]["old_sha256"] if path in late else port["files"][path]["old_sha256"])
                self.assertEqual(sha256(project_current_candidate(path, candidate, strict=True)), expected)
                with self.assertRaisesRegex(AssertionError, "Unrecognized #210 candidate bytes"):
                    project_current_candidate(path, candidate + b"outside exact recovery tooltip layer\n", strict=True)

    def test_freezer_recovery_key_and_stage_layer_is_exact_and_reversible(self):
        key_layer = json.loads(FREEZER_RECOVERY_KEY_RECORD.read_bytes())
        scissor = json.loads(FREEZER_ENERGY_SCISSOR_RECORD.read_bytes())
        recovery = json.loads(FREEZER_RECOVERY_TOOLTIP_RECORD.read_bytes())
        port = json.loads(FREEZER_NATIVE_PORT_RECORD.read_bytes())
        core = json.loads(RECORD.read_bytes())["files"]
        late = json.loads(LATE_VALIDATION_RECORD.read_bytes())["files"]
        self.assertEqual(key_layer["schema"], "iedctr-prism-freezer-recovery-key-stage-v1")
        self.assertEqual(key_layer["base_commit"], "d6ed3f6bf31190c8da5205ae9430354f78f6da29")
        self.assertEqual(set(key_layer["files"]), {
            "src/main/java/com/oblixorprime/engineersdecorreforged/client/ThermalVerticalScreen.java",
            "src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeFreezerReferenceValidation.java",
            "tools/validation/freezer_reference_native.py",
            "tools/validation/test_freezer_recovery_tooltip_wrap.py",
            "tools/validation/test_freezer_reference_native.py",
        })
        for path, layer in key_layer["files"].items():
            with self.subTest(path=path):
                parent = subprocess.check_output(["git", "show", key_layer["base_commit"] + ":" + path], cwd=ROOT)
                candidate = (ROOT / path).read_bytes()
                current = reverse_energy_scissor(path, candidate, scissor)
                self.assertEqual(sha256(parent), layer["old_sha256"])
                self.assertEqual(sha256(current), layer["new_sha256"])
                self.assertEqual(sha256(_reverse_layer(path, current, layer)), layer["old_sha256"])
                expected = (core[path]["states"][0]["sha256"] if path in core
                            else late[path]["old_sha256"] if path in late
                            else port["files"][path]["old_sha256"])
                self.assertEqual(sha256(project_current_candidate(path, candidate, strict=True)), expected)
                with self.assertRaisesRegex(AssertionError, "Unrecognized #210 candidate bytes"):
                    project_current_candidate(path, candidate + b"outside exact recovery key layer\n", strict=True)

    def test_freezer_energy_scissor_layer_is_exact_and_reversible(self):
        scissor = json.loads(FREEZER_ENERGY_SCISSOR_RECORD.read_bytes())
        self.assertEqual(scissor["schema"], "iedctr-prism-freezer-energy-scissor-v1")
        self.assertEqual(scissor["base_commit"], "b9fd7697645654633873a967eebaf2d61eda35ce")
        self.assertEqual(set(scissor["files"]), {
            "src/main/java/com/oblixorprime/engineersdecorreforged/client/ThermalVerticalScreen.java",
            "src/main/java/com/oblixorprime/engineersdecorreforged/utility/FreezerReferenceGeometry.java",
            "src/test/java/com/oblixorprime/engineersdecorreforged/utility/FreezerReferenceGeometryTest.java",
            "tools/validation/test_freezer_render_sync.py",
        })
        core = json.loads(RECORD.read_bytes())["files"]
        port = json.loads(FREEZER_NATIVE_PORT_RECORD.read_bytes())["files"]
        for path, layer in scissor["files"].items():
            with self.subTest(path=path):
                self.assertEqual(layer["previous_source_commit"], scissor["base_commit"])
                previous = subprocess.check_output(["git", "show", layer["previous_source_commit"] + ":" + path], cwd=ROOT)
                current = reverse_qa_followup(path, (ROOT / path).read_bytes())
                self.assertEqual(sha256(previous), layer["old_sha256"])
                self.assertEqual(sha256(current), layer["new_sha256"])
                self.assertEqual(sha256(_reverse_layer(path, current, layer)), layer["old_sha256"])
                expected = (core[path]["states"][0]["sha256"] if path in core else
                            port[path]["old_sha256"] if path in port else layer["old_sha256"])
                self.assertEqual(sha256(project_current_candidate(path, current, strict=True)), expected)

    def test_freezer_qa_followup_is_exact_and_reversible_from_a572(self):
        record = json.loads(FREEZER_QA_FOLLOWUP_RECORD.read_bytes())
        self.assertEqual(record["schema"], "iedctr-prism-freezer-qa-followup-v1")
        self.assertEqual(record["base_commit"], "a5723c09d735bf5500586737c8a04ae97ff63e60")
        expected_paths = {
            ".github/workflows/milking-packaged-native.yml",
            ".github/workflows/smelter-packaged-native.yml",
            ".github/workflows/tfc-packaged-native.yml",
            "gradle/tfc-packaged-validation.gradle",
            "src/guiAlbumThree/java/com/oblixorprime/iedctrtest/factoryplacer/GuiAlbumThree.java",
            "src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeFreezerReferenceValidation.java",
            "tools/validation/freezer_packaged_native.py",
            "tools/validation/freezer_reference_native.py",
            "tools/validation/gui_album_three.py",
            "tools/validation/test_freezer_packaged_driver.py",
            "tools/validation/test_freezer_render_sync.py",
            "tools/validation/test_gui_album_three.py",
            "tools/validation/test_tfc_packaged_driver.py",
            "tools/validation/tfc_packaged_mooshroom_native.py",
            "tools/validation/tfc_packaged_recovery_native.py",
        }
        self.assertEqual(set(record["files"]), expected_paths)
        for path, layer in record["files"].items():
            with self.subTest(path=path):
                previous = subprocess.check_output(["git", "show", record["base_commit"] + ":" + path], cwd=ROOT)
                current = reverse_electrical_controls(path, (ROOT / path).read_bytes())
                self.assertEqual(sha256(previous), layer["old_sha256"])
                self.assertEqual(sha256(current), layer["new_sha256"])
                self.assertEqual(_reverse_layer(path, current, layer), previous)
                self.assertEqual(project_current_candidate(path, current, strict=True),
                                 project_current_candidate(path, previous, strict=False))
                with self.assertRaisesRegex(AssertionError, "Unrecognized #210"):
                    project_current_candidate(path, current + b"unrecorded validation bytes\n", strict=True)


if __name__ == "__main__":
    unittest.main()
