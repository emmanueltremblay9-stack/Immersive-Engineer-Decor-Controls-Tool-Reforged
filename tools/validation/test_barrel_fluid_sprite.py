#!/usr/bin/env python3
"""Offline behavioral regression, not a mapped Minecraft compile or GPU test.

Compiles the actual production renderer and unchanged extracted Barrel draw method
against recording Minecraft-facing test doubles, then checks pixels, UV clipping,
ARGB, draw order and clip cleanup. No dependencies/download/Gradle required.
"""
import argparse
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client"
FIXTURES = ROOT / "tools/validation/fixtures/barrel_sprite"


def run_checks(screen_source=CLIENT / "MachineScreen.java",
               renderer_source=CLIENT / "FluidBarrelSpriteRenderer.java",
               native_source=CLIENT / "validation/NativeCorrectedMachineGuiValidation.java"):
    source = screen_source.read_text()
    start = source.index("   private void drawFluidBarrelWidgets(")
    end = source.index("   private void drawFluidBarrelLabels(", start)
    method = source[start:end]
    native = native_source.read_text()
    start = native.index("   private static int barrelCaptureOrigin(")
    end = native.index("   /** Sample only the fluid", start)
    capture_method = native[start:end]
    template = (FIXTURES / "BarrelScreenProbe.java.in").read_text()
    with tempfile.TemporaryDirectory(prefix="barrel-sprite-") as directory:
        output = Path(directory)
        probe = output / "BarrelScreenProbe.java"
        probe.write_text(template.replace("   /* PRODUCTION_METHOD */", method)
                         .replace("   /* NATIVE_CAPTURE_METHOD */", capture_method))
        sources = [renderer_source, probe, *sorted(FIXTURES.glob("*.java"))]
        # This environment has jdk.compiler but no javac launcher or ct.sym.
        subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main",
                        "-source", "21", "-target", "21", "-d", str(output),
                        *map(str, sources)], check=True)
        subprocess.run(["java", "-cp", str(output),
                        "com.oblixorprime.engineersdecorreforged.client.FluidBarrelSpriteChecks"], check=True)


class FluidBarrelSpriteTests(unittest.TestCase):
    def test_production_renderer_and_screen_contract(self):
        run_checks()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--screen-source", type=Path, default=CLIENT / "MachineScreen.java")
    parser.add_argument("--renderer-source", type=Path, default=CLIENT / "FluidBarrelSpriteRenderer.java")
    parser.add_argument("--native-source", type=Path, default=CLIENT / "validation/NativeCorrectedMachineGuiValidation.java")
    args = parser.parse_args()
    run_checks(args.screen_source, args.renderer_source, args.native_source)
