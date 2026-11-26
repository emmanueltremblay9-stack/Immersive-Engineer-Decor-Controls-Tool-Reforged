"""Exact V3 art/runtime identity; old R04 artwork remains history only."""
import hashlib
import json
import unittest
from pathlib import Path

from factory_placer_v2_reference_native import decode_png

ROOT = Path(__file__).resolve().parents[2]
MOD = "immersive_engineer_decor_controls_tool_reforged"
PROVENANCE = ROOT / "docs/qualification/factory-study-v3/provenance.json"
ASSETS = ROOT / "src/main/resources/assets" / MOD / "textures/gui/factory_study_v3"
GEOMETRY = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/utility/FactoryStudyGeometry.java"
SCREEN = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/FactoryStudyScreen.java"
REGISTRATION = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java"
R04 = ROOT / "docs/qualification/factory-steel-v2/protected-regions-r04.json"
R04_SHA256 = "cd1bac05da8d8055cb9d6c2ccaf570a12d1226fa240122559f3b05890bbf17b1"
V3_SOURCE_HEAD = "ea47ffbd"


class RegisteredArtworkTests(unittest.TestCase):
    def test_v3_runtime_artwork_matches_the_exact_provenance_bytes(self):
        record = json.loads(PROVENANCE.read_text(encoding="utf-8"))
        self.assertEqual(record["record_id"], "factory-study-v3-pr206")
        self.assertEqual(record["integration"]["screen_class"],
                         "com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen")
        expected_names = {"factory_hopper", "factory_dropper", "small_lab_furnace"}
        self.assertEqual({row["machine"] for row in record["assets"]}, expected_names)
        java_geometry = GEOMETRY.read_text(encoding="utf-8")
        screen = SCREEN.read_text(encoding="utf-8")
        registration = REGISTRATION.read_text(encoding="utf-8")
        for row in record["assets"]:
            path = ROOT / row["runtime_path"]
            data = path.read_bytes()
            self.assertEqual(hashlib.sha256(data).hexdigest(), row["sha256"], row["machine"])
            self.assertEqual(len(data), row["size_bytes"], row["machine"])
            width, height, pixels = decode_png(path)
            self.assertEqual([width, height], row["dimensions"], row["machine"])
            self.assertEqual((width, height), (1261, 1247))
            alpha = bytes(pixels[3::4])
            self.assertGreater(sum(value > 0 for value in alpha), 100_000, row["machine"])
            self.assertGreater(sum(value == 0 for value in alpha), 10_000, row["machine"])
            self.assertIn(row["sha256"], java_geometry)
            self.assertIn("FactoryStudyScreen::new", registration)
        self.assertIn("font.split(tooltip, FactoryStudyGeometry.tooltipWidth(width))", screen)
        self.assertIn("graphics.renderOutline(slot.x() - 1, slot.y() - 1, 18, 18", screen)

    def test_rejected_r04_record_is_immutable_but_not_a_v3_runtime_asset(self):
        data = R04.read_bytes()
        self.assertEqual(hashlib.sha256(data).hexdigest(), R04_SHA256)
        record = json.loads(data)
        self.assertEqual(record["source_head"], "c3a2fd55e290a1102e885b3b30972cf41f6cf0d4")
        self.assertEqual(len(record["machines"]["hopper"]["regions"]), 54 + 6 + 1 + 18)
        self.assertEqual(len(record["machines"]["dropper"]["regions"]), 51 + 8 + 6)
        old_assets = ROOT / "src/main/resources/assets" / MOD / "textures/gui/factory_steel_v2"
        self.assertFalse((old_assets / "hopper.png").exists())
        self.assertFalse((old_assets / "dropper.png").exists())
        registration = REGISTRATION.read_text(encoding="utf-8")
        self.assertNotIn("FactorySteelV2Screen::new", registration)


if __name__ == "__main__":
    unittest.main()
