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
V5C_MAP = ROOT / "docs/testing/gui11-v5c-runtime-map.json"


class RegisteredArtworkTests(unittest.TestCase):
    def test_v3_source_artwork_is_preserved_and_v5c_runtime_artwork_is_selected(self):
        record = json.loads(PROVENANCE.read_text(encoding="utf-8"))
        self.assertEqual(record["record_id"], "factory-study-v3-pr206")
        self.assertEqual(record["integration"]["screen_class"],
                         "com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen")
        expected_names = {"factory_hopper", "factory_dropper", "small_lab_furnace"}
        self.assertEqual({row["machine"] for row in record["assets"]}, expected_names)
        java_geometry = GEOMETRY.read_text(encoding="utf-8")
        screen = SCREEN.read_text(encoding="utf-8")
        registration = REGISTRATION.read_text(encoding="utf-8")
        v5c = json.loads(V5C_MAP.read_text(encoding="utf-8"))
        runtime_geometry = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/utility/Gui11V5CRuntimeGeometry.java").read_text()
        runtime_geometry = runtime_geometry + GEOMETRY.read_text()
        v5c_rows = {key: value for key, value in v5c["surfaces"].items() if key in {"01", "02", "03"}}
        self.assertEqual({"01", "02", "03"}, set(v5c_rows))
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
            # V3 bytes remain provenance history; default runtime is the newer
            # owner-selected GUI11 V5-Corrigee panel for these three routes.
            self.assertNotIn(row["sha256"], runtime_geometry)
            self.assertIn("FactoryStudyScreen::new", registration)
        for screen_id, row in v5c_rows.items():
            path = ROOT / "src/main/resources/assets" / MOD / "textures/gui/gui11_v5c" / f"{screen_id}.png"
            self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(), row["sha256"])
            self.assertIn(row["sha256"], runtime_geometry)
        self.assertIn("return Gui11V5CRuntimeGeometry.texturePath(kind)", java_geometry)
        self.assertIn("Gui11V5CArtwork.drawEnergy(graphics, menu)", screen)
        self.assertIn("Gui11V5CArtwork.drawEffects(graphics, menu, false)", screen)
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
