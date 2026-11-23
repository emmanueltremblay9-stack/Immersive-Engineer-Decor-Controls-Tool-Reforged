"""Regression guards against shipping rejected Factory steel GUI candidates."""
import hashlib
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui'
JAVA = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged'


class RetiredFactorySteelCandidateV2Tests(unittest.TestCase):
    def test_rejected_candidate_assets_are_not_runtime_resources(self):
        for path in (
            ASSETS / 'factory_steel_v2/hopper.png',
            ASSETS / 'factory_steel_v2/dropper.png',
            ASSETS / 'retained_dropper/background.png',
            ASSETS / 'retained_dropper/steel.png',
        ):
            with self.subTest(path=path):
                self.assertFalse(path.exists(), f'rejected candidate remains: {path.relative_to(ROOT)}')

    def test_historic_art_and_slot_geometry_stay_pinned(self):
        expected = {
            'factory_hopper_gui.png': '8a085db73897ea8e7a2e6af75b822a4509f5a0efa300a0a08d22da58f024795f',
            'factory_dropper_gui.png': '7d67a1f13fadde830e91aebb79d9014384eecc836e417f6e2ebb66f89543a4d1',
        }
        for name, digest in expected.items():
            with self.subTest(name=name):
                self.assertEqual(hashlib.sha256((ASSETS / name).read_bytes()).hexdigest(), digest)

        layout = (JAVA / 'utility/MachineLayout.java').read_text()
        self.assertIn('original(grid(0, 11, 9, 6, 3, "Storage"), 8, 71, 8, 129, "factory_hopper_gui")', layout)
        self.assertIn('grid(0, 10, 6, 6, 2, "Input")', layout)
        self.assertIn('new MachineLayout.MachineSlot(12, 19, 48, "Filter")', layout)
        self.assertIn('new MachineLayout.MachineSlot(13, 55, 48, "Filter")', layout)
        self.assertIn('new MachineLayout.MachineSlot(14, 91, 48, "Filter")', layout)
        self.assertIn('"factory_dropper_gui"', layout)

    def test_runtime_has_no_candidate_renderer_or_texture_reference(self):
        runtime = '\n'.join(p.read_text() for p in (JAVA / 'client').glob('*.java'))
        self.assertNotIn('FactoryHopperSteelArtwork', runtime)
        self.assertNotIn('FactorySteelFrame', runtime)
        self.assertNotIn('factory_steel_v2/', runtime)
        self.assertNotIn('retained_dropper/', runtime)

    def test_capture_uses_full_stacks_for_frame_review(self):
        source = (ROOT / 'src/guiAlbumThree/java/com/oblixorprime/iedctrtest/factoryplacer/GuiAlbumThree.java').read_text()
        for index in [0, 5]:
            self.assertIn(f'machines[{index}].setItem(0,new ItemStack(Items.COBBLESTONE,64));', source)


if __name__ == '__main__':
    unittest.main()
