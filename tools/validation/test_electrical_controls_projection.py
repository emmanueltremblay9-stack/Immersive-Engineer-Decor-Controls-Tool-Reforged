"""Electrical GUI source history stays exact; these checks are not game evidence."""
import subprocess
import unittest
from unittest.mock import patch

import prism_210_history_projection as projection


class ElectricalControlsProjectionTests(unittest.TestCase):
    BASE = "366ecf2c9d7e74afcc6398b80cfa84896b74e819"

    def test_each_reviewed_delta_restores_its_immutable_parent(self):
        record = projection._electrical_controls_record()
        self.assertEqual(record["base_commit"], self.BASE)
        java = "src/main/java/com/oblixorprime/engineersdecorreforged/"
        lang = "src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/"
        required = {
            "src/guiAlbumThree/java/com/oblixorprime/iedctrtest/factoryplacer/GuiAlbumThree.java",
            java + "client/MachineScreen.java",
            java + "gametest/MachineGuiGameTests.java",
            java + "menu/MachineMenu.java",
            java + "utility/MachineBlockEntity.java",
            *(lang + locale + ".json" for locale in ("en_us", "fr_fr", "fr_ca")),
            "tools/validation/gui_album_three.py",
            "tools/validation/test_gui_album_three.py",
        }
        self.assertEqual(set(record["files"]), required)
        for path, layer in record["files"].items():
            with self.subTest(path=path):
                current = (projection.ROOT / path).read_bytes()
                parent = subprocess.check_output(
                    ["git", "show", self.BASE + ":" + path], cwd=projection.ROOT
                )
                self.assertEqual(layer["previous_source_commit"], self.BASE)
                # GUI11-r3 is an exact later layer on Solar sources; reverse only
                # that recorded layer before comparing the electrical history.
                v5c_parent = projection.project_before_gui11_v5c(path, current, strict=True)
                electrical_current = projection.project_before_solar_gui11_r3(path, v5c_parent)
                self.assertEqual(projection.sha256(electrical_current), layer["new_sha256"])
                self.assertEqual(projection.sha256(parent), layer["old_sha256"])
                self.assertEqual(projection._reverse_layer(path, electrical_current, layer), parent)
                self.assertEqual(projection.sha256(current), projection.expected_current_sha256(path))
                self.assertEqual(
                    projection.project_current_candidate(path, current, strict=True),
                    projection.project_current_candidate(path, parent),
                )
                self.assertEqual((projection.ROOT / path).read_bytes(), current)

    def test_changes_inside_or_outside_recorded_hunks_are_rejected(self):
        for path, layer in projection._electrical_controls_record()["files"].items():
            current = (projection.ROOT / path).read_bytes()
            v5c_parent = projection.project_before_gui11_v5c(path, current, strict=True)
            electrical_current = projection.project_before_solar_gui11_r3(path, v5c_parent)
            hunk_text = next(h["current"] for h in layer["hunks"] if h["current"])
            changed_hunk = electrical_current.replace(hunk_text.encode(), (hunk_text + " ").encode(), 1)
            self.assertNotEqual(changed_hunk, electrical_current, path)
            for mutated in (electrical_current + b"unrecorded\n", changed_hunk):
                with self.subTest(path=path), self.assertRaisesRegex(AssertionError, "candidate bytes changed"):
                    projection._reverse_layer(path, mutated, layer)

    def test_receipt_mutation_is_rejected_and_unrelated_paths_stay_unrecognized(self):
        with patch.object(projection, "ELECTRICAL_CONTROLS_RECORD_BYTES",
                          projection.ELECTRICAL_CONTROLS_RECORD_BYTES + b"\n"):
            with self.assertRaisesRegex(AssertionError, "electrical controls projection record changed"):
                projection._electrical_controls_record()
        with self.assertRaisesRegex(AssertionError, "outside the #210 projection allowlist"):
            projection.project_current_candidate("unrelated-electrical.java", b"anything", strict=True)

    def test_all_previous_history_records_are_unchanged(self):
        baseline_paths = subprocess.check_output(
            ["git", "ls-tree", "-r", "--name-only", self.BASE, "tools/validation"],
            cwd=projection.ROOT, text=True,
        ).splitlines()
        records = [path for path in baseline_paths
                   if path.startswith("tools/validation/prism_210") and path.endswith("projection.json")]
        self.assertEqual(len(records), 7)
        for relative in records:
            with self.subTest(path=relative):
                parent = subprocess.check_output(
                    ["git", "show", self.BASE + ":" + relative], cwd=projection.ROOT
                )
                self.assertEqual((projection.ROOT / relative).read_bytes(), parent)


if __name__ == "__main__":
    unittest.main()
