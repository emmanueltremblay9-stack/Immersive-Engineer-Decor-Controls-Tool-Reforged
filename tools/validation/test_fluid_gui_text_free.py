"""Exact production recording checks and original-pixel fit, not native gameplay."""
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCREEN = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java"
RENDERER = SCREEN.with_name("RetainedFluidMachineArtwork.java")
GEOMETRY = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/utility/FluidMachineRedesignLayout.java"
FIXTURE = ROOT / "tools/validation/fixtures/fluid_text_free/FluidTextProbe.java.in"
ASSETS = ROOT / "src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged"


def methods(source):
    accumulator = source[source.index("   private void drawPassiveFluidAccumulatorLabels("):
                         source.index("   private void drawMachineWidget(")]
    funnel = source[source.index("   private void drawSmallFluidFunnelLabels("):
                    source.index("   private void drawFluidBarrelWidgets(")]
    return accumulator + funnel


def run_probe(source, renderer=None, geometry=None):
    with tempfile.TemporaryDirectory(prefix="fluid-art-") as directory:
        target = Path(directory)
        rendering = renderer if renderer is not None else RENDERER.read_text()
        rendering = rendering[rendering.index("final class RetainedFluidMachineArtwork"):]
        rendering = rendering.replace("final class RetainedFluidMachineArtwork", "static final class RetainedFluidMachineArtwork", 1)
        probe = target / "FluidTextProbe.java"
        probe.write_text(FIXTURE.read_text().replace("   /* PRODUCTION_METHODS */", methods(source))
                         .replace("   /* PRODUCTION_RENDERER */", rendering))
        layout = target / "FluidMachineRedesignLayout.java"
        layout.write_text(geometry if geometry is not None else GEOMETRY.read_text())
        result = subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main",
                                 "-source", "21", "-target", "21", "-d", str(target),
                                 str(probe), str(layout)], capture_output=True, text=True)
        if result.returncode:
            raise AssertionError(result.stderr)
        art = ASSETS / "textures/gui"
        return subprocess.run(["java", "-cp", str(target), "FluidTextProbe",
                               str(art / "small_fluid_funnel_v1/background.png"),
                               str(art / "passive_fluid_accumulator_v1/background.png")], capture_output=True, text=True)


class FluidGuiTextFreeTests(unittest.TestCase):
    def test_exact_render_tooltip_methods_and_all_source_pixel_item_footprints(self):
        result = run_probe(SCREEN.read_text())
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Fluid retained-art checks passed", result.stdout)

    def test_restored_permanent_labels_are_rejected(self):
        for name in ("drawSmallFluidFunnelLabels", "drawPassiveFluidAccumulatorLabels"):
            with self.subTest(name=name):
                source = SCREEN.read_text().replace(
                    f"   private void {name}(GuiGraphics graphics) {{",
                    f"   private void {name}(GuiGraphics graphics) {{\n"
                    '      this.drawTinyString(graphics, "label", 0, 0, 40, 0, false);')
                result = run_probe(source)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("Permanent label drawn", result.stderr)

    def test_opaque_glass_regression_is_rejected(self):
        result = run_probe(SCREEN.read_text(), RENDERER.read_text().replace("0xC03F76E4", "0xFF3F76E4"))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Original glass must remain visible", result.stderr)

    def test_missing_outlet_hover_is_rejected(self):
        source = SCREEN.read_text().replace(
            "this.isFluidArtworkHovered(FluidMachineRedesignLayout.FUNNEL_OUTLET, mouseX, mouseY)", "false")
        result = run_probe(source)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Outlet hover", result.stderr)

    def test_misaligned_inventory_geometry_is_rejected_by_original_pixels(self):
        geometry = GEOMETRY.read_text().replace("{44, 63,", "{52, 63,")
        result = run_probe(SCREEN.read_text(), geometry=geometry)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Item footprint intersects source metal", result.stderr)

    def test_source_artwork_is_exact_retained_v1(self):
        expected = {
            "small_fluid_funnel_v1/background.png": "c5dcc2ee56fdace5f9bf66456e0a4dca91ce4fddb78887a9a6b60ff914336a64",
            "passive_fluid_accumulator_v1/background.png": "da2a83c0f8cc68be5f69ad4f3cc392cc4a600206ae39ac8b81142e421e466872",
        }
        for name, digest in expected.items():
            self.assertEqual(hashlib.sha256((ASSETS / "textures/gui" / name).read_bytes()).hexdigest(), digest)

    def test_legacy_patch_renderers_are_removed_from_runtime(self):
        source = SCREEN.read_text()
        self.assertNotIn("drawSmallFluidFunnelBackgroundOverlays(", source)
        self.assertNotIn("drawPassiveFluidAccumulatorBackgroundOverlays(", source)
        self.assertNotIn("maskAccumulatorLabel(", source)
        self.assertIn("RetainedFluidMachineArtwork.render(graphics, (MachineMenu)this.menu, x, y);", source)

    def test_preserved_hover_keys_exist_in_english_and_french(self):
        for locale in ("en_us", "fr_fr"):
            data = json.loads((ASSETS / "lang" / f"{locale}.json").read_text())
            for machine in ("small_fluid_funnel", "passive_fluid_accumulator"):
                self.assertTrue(data[f"block.immersive_engineer_decor_controls_tool_reforged.{machine}"])
                for key in ("tank.tooltip", "bucket.input", "bucket.output"):
                    self.assertTrue(data[f"gui.immersive_engineer_decor_controls_tool_reforged.{machine}.{key}"])
            self.assertTrue(data["gui.immersive_engineer_decor_controls_tool_reforged.small_fluid_funnel.output"])
            self.assertTrue(data["gui.immersive_engineer_decor_controls_tool_reforged.passive_fluid_accumulator.collection.tooltip"])


if __name__ == "__main__":
    unittest.main()
