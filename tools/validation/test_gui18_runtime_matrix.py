"""The 18 GUI design entries must resolve to existing runtime screens and assets."""
import hashlib
import json
import struct
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
MOD = "immersive_engineer_decor_controls_tool_reforged"
ASSET_ROOT = ROOT / "src/main/resources/assets" / MOD / "textures/gui"


class Gui18RuntimeMatrixTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.matrix = json.loads((ROOT / "docs/testing/gui18-runtime-matrix.json").read_text())
        cls.surfaces = cls.matrix["surfaces"]
        cls.registration = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java").read_text()
        cls.machine_screen = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java").read_text()
        cls.factory_study = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/FactoryStudyScreen.java").read_text()
        cls.factory_geometry = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/utility/FactoryStudyGeometry.java").read_text()
        cls.thermal = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/ThermalVerticalScreen.java").read_text()
        cls.solar = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/SolarV2ReferenceScreen.java").read_text()
        cls.menu = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java").read_text()

    def test_exactly_eighteen_unique_source_scenes_are_mapped(self):
        self.assertEqual([f"{i:02}" for i in range(1, 19)], [row["id"] for row in self.surfaces])
        self.assertEqual(18, len({row["name"] for row in self.surfaces}))
        self.assertEqual(795, sum(row["source_scene_slots"] for row in self.surfaces))

    def test_every_declared_runtime_asset_matches_the_checked_in_manifest(self):
        manifest = self.matrix["runtime_asset_manifest"]
        declared = {asset for row in self.surfaces for asset in row["assets"]}
        self.assertEqual(declared, set(manifest))
        for row in self.surfaces:
            with self.subTest(screen=row["name"]):
                self.assertTrue(row["proof"].strip())
                self.assertTrue(row["assets"] or row["id"] == "18")
                for asset in row["assets"]:
                    path = ASSET_ROOT / asset
                    self.assertTrue(path.is_file(), f"missing runtime asset {asset}")
                    blob = path.read_bytes()
                    self.assertTrue(blob.startswith(b"\x89PNG\r\n\x1a\n"), f"not a PNG: {asset}")
                    self.assertEqual(manifest[asset]["sha256"], hashlib.sha256(blob).hexdigest(), asset)
                    width, height = struct.unpack(">II", blob[16:24])
                    self.assertEqual((manifest[asset]["width"], manifest[asset]["height"]), (width, height), asset)

    def test_menu_routes_use_expected_screen_families(self):
        expected = {
            "01": ("FACTORY_HOPPER", "FactoryStudyScreen::new"),
            "02": ("FACTORY_DROPPER", "FactoryStudyScreen::new"),
            "03": ("SMALL_LAB_FURNACE", "FactoryStudyScreen::new"),
            "04": ("FACTORY_PLACER", "FactoryPlacerV2ReferenceScreen(menu, inventory, title)", "MachineScreen.FactoryPlacerScreen(menu, inventory, title)"),
            "05": ("SMALL_FREEZER", "ThermalVerticalScreen::new"),
            "06": ("SMALL_MINERAL_SMELTER", "ThermalVerticalScreen::new"),
            "07": ("SMALL_ELECTRICAL_FURNACE", "MachineScreen.SmallElectricalFurnaceScreen::new"),
            "08": ("SMALL_WASTE_INCINERATOR", "MachineScreen.SmallWasteIncineratorScreen::new"),
            "09": ("SMALL_BLOCK_BREAKER", "MachineScreen.SmallBlockBreakerScreen::new"),
            "10": ("SMALL_TREE_CUTTER", "TreeCutterV3ReferenceScreen::new"),
            "11": ("SMALL_SOLAR_PANEL", "SolarV2ReferenceScreen(menu, inventory, title)", "MachineScreen.SmallSolarPanelScreen(menu, inventory, title)"),
            "12": ("SMALL_MILKING_MACHINE", "MachineScreen.SmallMilkingMachineScreen::new"),
            "13": ("SMALL_FLUID_FUNNEL", "MachineScreen.SmallFluidFunnelScreen::new"),
            "14": ("PASSIVE_FLUID_ACCUMULATOR", "MachineScreen.PassiveFluidAccumulatorScreen::new"),
            "15": ("FLUID_BARREL", "BarrelReferenceScreen(menu, inventory, title)", "MachineScreen.FluidBarrelScreen(menu, inventory, title)"),
            "16": ("METAL_CRAFTING_TABLE", "MachineScreen.MetalCraftingTableScreen::new"),
            "17": ("LABELED_CRATE", "LabeledCrateScreen::new"),
        }
        for row in self.surfaces:
            with self.subTest(screen=row["id"]):
                if row["id"] == "18":
                    self.assertIn("LabeledCrateLabelClientBridge.setOpener", self.registration)
                    self.assertIn("new LabeledCrateEditScreen", self.registration)
                    continue
                tokens = expected[row["id"]]
                self.assertIn(f"ModMenus.{tokens[0]}", self.registration)
                for token in tokens[1:]:
                    self.assertIn(token, self.registration)

    def test_screen_specific_resource_and_state_paths_are_wired(self):
        self.assertIn('"textures/gui/factory_study_v3/" + kind.registryName() + ".png"', self.factory_geometry)
        self.assertIn("FactoryStudyGeometry.texturePath(kind)", self.factory_study)
        for path in ("thermal_vertical/freezer/neutral.png", "thermal_vertical/freezer/effect.png",
                     "thermal_vertical/freezer/energy.png", "thermal_vertical/smelter/neutral.png",
                     "thermal_vertical/smelter/effect.png", "thermal_vertical/smelter/energy.png"):
            self.assertIn(path, self.thermal)
        self.assertIn("solar_gui11_r3/background.png", self.solar)
        self.assertIn("DEV_SOLAR_GUI11_R3_PROPERTY", self.menu)
        self.assertIn("menu.solarGui11R3Presentation()", self.solar)
        self.assertIn("SolarGui11Geometry.fit(width, height) : SolarV2ReferenceGeometry.fit(width, height)", self.solar)
        electrical = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/ElectricalFurnaceArtwork.java").read_text()
        self.assertIn("ElectricalFurnaceArtwork.render", self.machine_screen)
        self.assertIn("energyCellPixels", electrical)
        self.assertIn("retainedFluidLayout", (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineLayout.java").read_text())

    def test_every_runtime_asset_is_referenced_by_screen_or_layout_code(self):
        java_sources = "\n".join(path.read_text(errors="ignore") for path in (ROOT / "src/main/java").rglob("*.java"))
        kind_source = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineKind.java").read_text()
        self.assertIn('"textures/gui/factory_study_v3/" + kind.registryName() + ".png"', self.factory_geometry)
        for row in self.surfaces:
            for asset in row["assets"]:
                stem = asset.removesuffix(".png")
                if row["id"] in {"01", "02", "03"}:
                    machine = Path(asset).stem
                    self.assertIn(f'"{machine}"', kind_source)
                else:
                    self.assertIn(stem, java_sources, f"runtime code does not reference {asset}")
        self.assertIn("backgroundTexture(layout)", self.machine_screen)

    def test_source_packages_are_not_runtime_artwork(self):
        self.assertEqual(3, len(self.matrix["authority"]["source_zip_sha256"]))
        self.assertIn("V5 artwork is explicitly excluded", self.matrix["authority"]["source_package_acceptance"])
        self.assertIn("reference material, not runtime resources", self.matrix["authority"]["runtime_policy"])
        source_pngs = list(ASSET_ROOT.glob("[0-9][0-9]-V5-Corrigee.png"))
        self.assertEqual([], source_pngs)


if __name__ == "__main__":
    unittest.main()
