"""Recording tests for text-free Funnel/Accumulator presentation, not native gameplay."""
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCREEN = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java"
GAUGE = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/utility/PassiveFluidAccumulatorGauge.java"
FIXTURE = ROOT / "tools/validation/fixtures/fluid_text_free/FluidTextProbe.java.in"
ASSETS = ROOT / "src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged"


def methods(source):
    accumulator = source[source.index("   private void drawPassiveFluidAccumulatorBackgroundOverlays("):
                         source.index("   private void drawMachineWidget(")]
    funnel = source[source.index("   private void drawSmallFluidFunnelBackgroundOverlays("):
                    source.index("   private void drawFluidBarrelWidgets(")]
    return accumulator + funnel


def run_probe(source):
    with tempfile.TemporaryDirectory(prefix="fluid-text-") as directory:
        target = Path(directory)
        probe = target / "FluidTextProbe.java"
        probe.write_text(FIXTURE.read_text().replace("   /* PRODUCTION_METHODS */", methods(source)))
        result = subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main",
                                 "-source", "21", "-target", "21", "-d", str(target),
                                 str(probe), str(GAUGE)], capture_output=True, text=True)
        if result.returncode:
            raise AssertionError(result.stderr)
        return subprocess.run(["java", "-cp", str(target), "FluidTextProbe"], capture_output=True, text=True)


class FluidGuiTextFreeTests(unittest.TestCase):
    def test_exact_render_and_tooltip_methods(self):
        result = run_probe(SCREEN.read_text())
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Fluid text-free checks passed", result.stdout)

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

    def test_french_baked_caption_regression_is_rejected(self):
        source = SCREEN.read_text().replace(
            "      this.drawRepeatedTexturePatch(graphics, texture, x0 + 20, y0 + 64, 46, 12,",
            "      if (!this.isFrenchArtworkLocale())\n"
            "      this.drawRepeatedTexturePatch(graphics, texture, x0 + 20, y0 + 64, 46, 12,")
        result = run_probe(source)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Baked bucket caption", result.stderr)

    def test_missing_outlet_hover_is_rejected(self):
        source = SCREEN.read_text().replace(
            "this.isHovering(94, 123, 97, 15, mouseX, mouseY)", "false")
        result = run_probe(source)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Outlet hover", result.stderr)

    def test_source_artwork_is_unchanged(self):
        expected = {
            "small_fluid_funnel.png": "58d9c9be49aa88db60e15f1fab8f7206853f4a061a96a2ef533727f9b7514f51",
            "passive_fluid_accumulator.png": "bb2206d5caf1913081f66b4f22ac9cb2fa5720a0489fd28fc80ab1b96eafc5a7",
        }
        for name, digest in expected.items():
            self.assertEqual(hashlib.sha256((ASSETS / "textures/gui" / name).read_bytes()).hexdigest(), digest)

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
