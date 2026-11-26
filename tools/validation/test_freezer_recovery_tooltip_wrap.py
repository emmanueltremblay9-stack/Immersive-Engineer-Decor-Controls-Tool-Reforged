"""Current Freezer recovery tooltip source/localization gates; native workflow checks placement."""
from pathlib import Path
import json
import unittest

ROOT = Path(__file__).resolve().parents[2]
MOD = "immersive_engineer_decor_controls_tool_reforged"
SCREEN = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/ThermalVerticalScreen.java"
KEY = f"gui.{MOD}.freezer_reference.recover_help"
ENGLISH = "Recover legacy slot 1 to your inventory. Ctrl+Shift+R. Full inventory: items stay stored."
FRENCH = "Récupérer l’ancien emplacement 1 dans votre inventaire. Ctrl+Maj+R. Inventaire plein : les objets restent stockés."


class FreezerRecoveryTooltipTest(unittest.TestCase):
    def test_real_registered_screen_uses_localized_recovery_key(self):
        source = SCREEN.read_text()
        self.assertIn("Component.translatableWithFallback(", source)
        self.assertIn('NS + ".freezer_reference.recover_help"', source)
        self.assertNotIn('Component.literal("Recover reserved legacy items")', source)
        self.assertIn("recoveryButton.isHovered()", source)

    def test_english_and_french_localizations_are_exact(self):
        for language, expected in (("en_us", ENGLISH), ("fr_fr", FRENCH)):
            path = ROOT / f"src/main/resources/assets/{MOD}/lang/{language}.json"
            self.assertEqual(json.loads(path.read_text())[KEY], expected)

    def test_recovery_button_remains_inside_thermal_canvas(self):
        source = SCREEN.read_text()
        self.assertIn(".bounds(238, 199, 16, 16)", source)
        self.assertIn("ThermalVerticalGeometry.CANVAS_WIDTH", source)
        self.assertIn("ThermalVerticalGeometry.CANVAS_HEIGHT", source)


if __name__ == "__main__":
    unittest.main()
