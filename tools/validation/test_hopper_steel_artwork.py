"""Static guards that the historic Hopper/Dropper artwork remains the runtime source."""
import hashlib
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged'
ASSETS = ROOT / 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui'


class HopperSteelArtworkTests(unittest.TestCase):
    def test_historical_artwork_bytes_are_preserved(self):
        for name, digest in {
            'factory_placer_v2_reference/background.png': 'bc0f0426f308435585e66f39c6a34c08b38173a3f7927ac0fadafe9d161eb99d',
            'factory_hopper_gui.png': '8a085db73897ea8e7a2e6af75b822a4509f5a0efa300a0a08d22da58f024795f',
            'factory_dropper_gui.png': '7d67a1f13fadde830e91aebb79d9014384eecc836e417f6e2ebb66f89543a4d1',
        }.items():
            with self.subTest(name=name):
                self.assertEqual(hashlib.sha256((ASSETS / name).read_bytes()).hexdigest(), digest)

    def test_rejected_candidate_art_and_renderers_are_absent(self):
        candidates = (
            ASSETS / 'factory_steel_v2/hopper.png',
            ASSETS / 'factory_steel_v2/dropper.png',
            ASSETS / 'retained_dropper/background.png',
            ASSETS / 'retained_dropper/steel.png',
            JAVA / 'client/FactoryHopperSteelArtwork.java',
            JAVA / 'client/FactorySteelFrame.java',
            JAVA / 'utility/RetainedDropperLayout.java',
        )
        for path in candidates:
            with self.subTest(path=path):
                self.assertFalse(path.exists(), f'rejected candidate remains: {path.relative_to(ROOT)}')

    def test_historic_hopper_and_dropper_geometry_and_atlas_remain_registered(self):
        layout = (JAVA / 'utility/MachineLayout.java').read_text()
        self.assertIn('original(grid(0, 11, 9, 6, 3, "Storage"), 8, 71, 8, 129, "factory_hopper_gui")', layout)
        self.assertIn('grid(0, 10, 6, 6, 2, "Input")', layout)
        self.assertIn('new MachineLayout.MachineSlot(12, 19, 48, "Filter")', layout)
        self.assertIn('new MachineLayout.MachineSlot(13, 55, 48, "Filter")', layout)
        self.assertIn('new MachineLayout.MachineSlot(14, 91, 48, "Filter")', layout)
        self.assertIn('"factory_dropper_gui"', layout)

        screen = (JAVA / 'client/MachineScreen.java').read_text()
        self.assertIn('this.drawOriginalTextureBackdrop(graphics, x, y);', screen)
        self.assertIn('this.drawFactoryHopperWidgets(graphics, texture, x0, y0);', screen)
        self.assertIn('this.drawFactoryDropperWidgets(graphics, texture, x0, y0);', screen)
        self.assertNotIn('FactoryHopperSteelArtwork', screen)
        self.assertNotIn('FactorySteelFrame', screen)
        self.assertNotIn('factory_steel_v2/', screen)
        self.assertNotIn('retained_dropper/', screen)

    def test_hopper_controls_keep_original_click_and_tooltip_dispatch(self):
        screen = (JAVA / 'client/MachineScreen.java').read_text()
        self.assertIn('case FACTORY_HOPPER -> this.handleFactoryHopperClick(mouseX, mouseY);', screen)
        self.assertIn('case FACTORY_DROPPER -> this.handleFactoryDropperClick(mouseX, mouseY);', screen)
        self.assertIn('case FACTORY_HOPPER -> this.factoryHopperTooltip(mouseX, mouseY);', screen)
        self.assertIn('case FACTORY_DROPPER -> this.factoryDropperTooltip(mouseX, mouseY);', screen)


if __name__ == '__main__':
    unittest.main()
