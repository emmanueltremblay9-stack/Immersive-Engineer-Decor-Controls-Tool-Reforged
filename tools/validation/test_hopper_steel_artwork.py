"""Presentation/source guards only; native screenshots are separately required."""
import hashlib
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged'
ASSETS = ROOT / 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui'


class HopperSteelArtworkTests(unittest.TestCase):
    def test_retained_artwork_not_replaced(self):
        for name, digest in {
            'factory_placer_v2_reference/background.png': 'ff079488dd0d1c7434597a6915231d88b81732472bb23ccc8a61235eca27ec2b',
            'factory_hopper_gui.png': '8a085db73897ea8e7a2e6af75b822a4509f5a0efa300a0a08d22da58f024795f',
        }.items():
            with self.subTest(name=name):
                self.assertEqual(hashlib.sha256((ASSETS / name).read_bytes()).hexdigest(), digest)

    def test_hopper_keeps_own_slot_and_inventory_geometry(self):
        layout = (JAVA / 'utility/MachineLayout.java').read_text()
        self.assertIn('original(grid(0, 11, 9, 6, 3, "Storage"), 8, 71, 8, 129, "factory_hopper_gui")', layout)
        artwork = (JAVA / 'client/FactoryHopperSteelArtwork.java').read_text()
        self.assertIn('for (var slot : menu.slots)', artwork)
        self.assertNotIn('FactoryPlacerV2ReferenceGeometry', artwork)
        for forbidden in ('PacketDistributor', 'MachineActionPayload', 'menu.set', 'setItem(', 'new Slot('):
            self.assertNotIn(forbidden, artwork)

    def test_only_hopper_dispatches_new_material(self):
        screen = (JAVA / 'client/MachineScreen.java').read_text()
        self.assertIn('if (kind == MachineKind.FACTORY_HOPPER) {\n                FactoryHopperSteelArtwork.render(graphics, this.menu, texture, x, y);', screen)
        self.assertIn('this.drawOriginalWidgets(graphics, texture, x, y);', screen)
        self.assertIn('case FACTORY_HOPPER -> this.handleFactoryHopperClick(mouseX, mouseY);', screen)
        self.assertIn('case FACTORY_HOPPER -> this.factoryHopperTooltip(mouseX, mouseY);', screen)


if __name__ == '__main__':
    unittest.main()
