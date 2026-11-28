"""Bind the 18 selected GUI designs to real runtime screens and exact packaged assets."""
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
        cls.manifest = cls.matrix["runtime_asset_manifest"]
        cls.registration = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java").read_text()
        cls.machine_screen = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java").read_text()
        cls.factory_study = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/FactoryStudyScreen.java").read_text()
        cls.factory_geometry = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/utility/FactoryStudyGeometry.java").read_text()
        cls.runtime_geometry = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/utility/Gui11V5CRuntimeGeometry.java").read_text()
        cls.artwork = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/Gui11V5CArtwork.java").read_text()
        cls.thermal = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/ThermalVerticalScreen.java").read_text()
        cls.solar = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/SolarV2ReferenceScreen.java").read_text()
        cls.menu = (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java").read_text()

    def test_exactly_eighteen_scenes_are_mapped_once(self):
        self.assertEqual([f"{i:02}" for i in range(1, 19)], [row["id"] for row in self.surfaces])
        self.assertEqual(18, len({row["name"] for row in self.surfaces}))
        self.assertEqual(795, sum(row["source_scene_slots"] for row in self.surfaces))
        self.assertEqual(3, len(self.matrix["authority"]["source_zip_sha256"]))

    def test_exact_candidate_png_identity_and_dimensions(self):
        declared = {asset for row in self.surfaces for asset in row["assets"]}
        self.assertEqual(declared, set(self.manifest))
        self.assertEqual(19, len(self.manifest))
        for row in self.surfaces:
            for asset in row["assets"]:
                with self.subTest(screen=row["id"], asset=asset):
                    path = ASSET_ROOT / asset
                    self.assertTrue(path.is_file(), f"missing runtime asset {asset}")
                    blob = path.read_bytes()
                    self.assertTrue(blob.startswith(b"\x89PNG\r\n\x1a\n"), f"not a PNG: {asset}")
                    self.assertEqual(self.manifest[asset]["sha256"], hashlib.sha256(blob).hexdigest(), asset)
                    width, height = struct.unpack(">II", blob[16:24])
                    self.assertEqual((self.manifest[asset]["width"], self.manifest[asset]["height"]),
                                     (width, height), asset)
                    self.assertEqual(self.manifest[asset]["color_type"], blob[25], asset)
                    self.assertEqual(6, blob[25], f"expected RGBA PNG for transparency: {asset}")

    def test_seventeen_v5c_panels_are_exact_and_solar_v2_remains_default(self):
        expected_ids = [f"{i:02}" for i in range(1, 11)] + [f"{i:02}" for i in range(12, 19)]
        rows = {row["id"]: row for row in self.surfaces}
        for screen_id in expected_ids:
            row = rows[screen_id]
            self.assertEqual([f"gui11_v5c/{screen_id}.png"], row["assets"])
            self.assertEqual(row["source_sha256"], self.manifest[row["assets"][0]]["sha256"])
            self.assertEqual("5.0.1 GUI11-R3 V5-Corrigee", row["source_version"])
            self.assertIn("exact-head client validation pending", row["runtime_status"])
        solar = rows["11"]
        self.assertEqual(["solar_v2_reference/background.png", "solar_gui11_r3/background.png"], solar["assets"])
        self.assertIn("Solar V2 default", solar["runtime_status"])
        self.assertIn("Solar V2 ordinary-JAR native workflow passed", solar["proof"])
        self.assertIn("no dedicated R3 client capture yet", solar["proof"])

    def test_registered_menu_routes_resolve_to_expected_screens(self):
        expected = {
            "01": ("FACTORY_HOPPER", "FactoryStudyScreen::new"),
            "02": ("FACTORY_DROPPER", "FactoryStudyScreen::new"),
            "03": ("SMALL_LAB_FURNACE", "FactoryStudyScreen::new"),
            "04": ("FACTORY_PLACER", "FactoryPlacerV2ReferenceScreen(menu, inventory, title)",
                   "MachineScreen.FactoryPlacerScreen(menu, inventory, title)"),
            "05": ("SMALL_FREEZER", "ThermalVerticalScreen::new"),
            "06": ("SMALL_MINERAL_SMELTER", "ThermalVerticalScreen::new"),
            "07": ("SMALL_ELECTRICAL_FURNACE", "MachineScreen.SmallElectricalFurnaceScreen::new"),
            "08": ("SMALL_WASTE_INCINERATOR", "MachineScreen.SmallWasteIncineratorScreen::new"),
            "09": ("SMALL_BLOCK_BREAKER", "MachineScreen.SmallBlockBreakerScreen::new"),
            "10": ("SMALL_TREE_CUTTER", "TreeCutterV3ReferenceScreen::new"),
            "11": ("SMALL_SOLAR_PANEL", "SolarV2ReferenceScreen(menu, inventory, title)",
                   "MachineScreen.SmallSolarPanelScreen(menu, inventory, title)"),
            "12": ("SMALL_MILKING_MACHINE", "MachineScreen.SmallMilkingMachineScreen::new"),
            "13": ("SMALL_FLUID_FUNNEL", "MachineScreen.SmallFluidFunnelScreen::new"),
            "14": ("PASSIVE_FLUID_ACCUMULATOR", "MachineScreen.PassiveFluidAccumulatorScreen::new"),
            "15": ("FLUID_BARREL", "BarrelReferenceScreen(menu, inventory, title)",
                   "MachineScreen.FluidBarrelScreen(menu, inventory, title)"),
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

    def test_candidate_asset_routers_and_dynamic_state_sources_are_present(self):
        self.assertIn('"textures/gui/gui11_v5c/" + id(kind) + ".png"', self.runtime_geometry)
        self.assertIn("Gui11V5CArtwork.draw", self.machine_screen)
        self.assertIn("Gui11V5CArtwork.draw(graphics, menu)", self.thermal)
        self.assertIn("Gui11V5CRuntimeGeometry.texturePath(kind)", self.factory_geometry)
        self.assertIn('"textures/gui/gui11_v5c/17.png"',
                      (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/LabeledCrateScreen.java").read_text())
        self.assertIn('"textures/gui/gui11_v5c/18.png"',
                      (ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/LabeledCrateEditScreen.java").read_text())
        self.assertIn("menu.energyStored()", self.artwork)
        self.assertIn("menu.freezerPhase()", self.artwork)
        self.assertIn("menu.progress()", self.artwork)
        self.assertIn("menu.field(4) > 0", self.artwork)  # Lab Furnace synchronized burn state
        self.assertIn("gui11HeatWaveVisible(menu.field(0), menu.field(7))", self.artwork)
        self.assertIn("gui11CopperReflectionVisible(menu.field(7))", self.artwork)
        self.assertIn("smallFluidFunnelFluidCode", self.artwork)

    def test_electrical_furnace_uses_existing_server_mode_action_and_separate_readouts(self):
        click = self.machine_screen.split("public boolean mouseClicked", 1)[1].split("protected void renderLabels", 1)[0]
        self.assertIn('control(this.menu.kind(), "rotary")', click)
        self.assertIn("ElectricalFurnaceControl.CYCLE_SPEED_ACTION", click)
        self.assertIn("ElectricalFurnaceArtwork.clickStep(button)", click)
        tooltip = self.machine_screen.split('this.menu.kind() == MachineKind.SMALL_ELECTRICAL_FURNACE', 1)[1]
        self.assertIn('this.menu.field(7)', tooltip)
        self.assertIn('this.menu.field(0)', tooltip)
        self.assertIn('this.menu.energyStored()', self.machine_screen)
        self.assertIn('this.menu.energyCapacity()', self.machine_screen)
        self.assertIn('"rotary"', self.runtime_geometry)
        self.assertIn("Gui11V5CRuntimeGeometry.energy", self.machine_screen)

    def test_solar_v2_default_and_r3_opt_in_are_kept_separate(self):
        self.assertIn("solar_gui11_r3/background.png", self.solar)
        self.assertIn("DEV_SOLAR_GUI11_R3_PROPERTY", self.menu)
        self.assertIn("menu.solarGui11R3Presentation()", self.solar)
        self.assertIn("SolarGui11Geometry.fit(width, height) : SolarV2ReferenceGeometry.fit(width, height)", self.solar)
        self.assertIn("menu.solarV2ReferencePresentation()", self.registration)

    def test_private_source_packages_are_not_embedded_as_runtime_assets(self):
        policy = self.matrix["authority"]["runtime_policy"]
        self.assertIn("V5-Corrigee replaces", policy)
        self.assertIn("Solar V2 as its default", policy)
        self.assertIn("K/X and conduits are excluded", policy)
        source_pngs = list(ASSET_ROOT.glob("[0-9][0-9]-V5-Corrigee.png"))
        self.assertEqual([], source_pngs)
        self.assertEqual("2aa82b630ee4c5abd465daecb6d718bf731b36da",
                         self.matrix["candidate_evidence"]["base_head"])
        self.assertIsNone(self.matrix["candidate_evidence"]["candidate_head"])


if __name__ == "__main__":
    unittest.main()
