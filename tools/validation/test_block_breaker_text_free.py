"""Offline tests of the exact production label and tooltip methods.

Minecraft-facing recording doubles prove dispatch and content, not native font,
screen input, framebuffer rendering, or actual machine behavior.
"""
import hashlib
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCREEN = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java"
FIXTURE = ROOT / "tools/validation/fixtures/block_breaker/BreakerTextProbe.java.in"
ART = ROOT / "src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/block_breaker_rp_v3"


def production_methods(source):
    labels = source[source.index("   private void drawBlockBreakerLabels("):
                    source.index("   static String blockBreakerStatusLabelKey(")]
    tooltip = source[source.index("   private Component blockBreakerTooltip("):
                     source.index("   private void renderOriginalHoverOverlay(")]
    return labels + tooltip


def run_probe(source):
    with tempfile.TemporaryDirectory(prefix="breaker-text-") as directory:
        target = Path(directory)
        probe = target / "BreakerTextProbe.java"
        probe.write_text(FIXTURE.read_text().replace("   /* PRODUCTION_METHODS */", production_methods(source)))
        compile_result = subprocess.run(
            ["java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "-source", "21", "-target", "21",
             "-d", str(target), str(probe)], capture_output=True, text=True)
        if compile_result.returncode:
            raise AssertionError(compile_result.stderr)
        return subprocess.run(["java", "-cp", str(target), "BreakerTextProbe"], capture_output=True, text=True)


class BlockBreakerTextFreeTests(unittest.TestCase):
    def test_exact_production_methods_keep_title_on_hover_only(self):
        result = run_probe(SCREEN.read_text())
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Breaker text-free checks passed", result.stdout)

    def test_restoring_permanent_title_is_rejected(self):
        source = SCREEN.read_text().replace(
            "   private void drawBlockBreakerLabels(GuiGraphics graphics) {",
            "   private void drawBlockBreakerLabels(GuiGraphics graphics) {\n"
            '      this.drawScaledCenteredString(graphics, "title", 128, 13, 155, 0xFF24180F, 1.0F);')
        result = run_probe(source)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Permanent title drawn", result.stderr)

    def test_removing_title_hover_is_rejected(self):
        source = SCREEN.read_text().replace(
            "if (this.isHovering(42, 6, 172, 18, mouseX, mouseY))", "if (false)")
        result = run_probe(source)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Nameplate identity", result.stderr)

    def test_retained_art_is_byte_identical(self):
        for name, digest in {
            "background.png": "2594e227dd153ffa7b1954d8d5d3069e10d0176c0e8142c7f7d2727f315b0a0d",
            "power_neutral.png": "23ff15c60420b8951117f748d0b6122e174c915f87e91466930af80fbdc12123",
        }.items():
            with self.subTest(name=name):
                self.assertEqual(hashlib.sha256((ART / name).read_bytes()).hexdigest(), digest)


if __name__ == "__main__":
    unittest.main()
