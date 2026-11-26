from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
GUI = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeCorrectedMachineGuiValidation.java"
THERMAL = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/ThermalVerticalScreen.java"


class CorrectedMachineGuiNativeContractTest(unittest.TestCase):
    def test_barrel_matrix_keeps_freezer_and_identifies_waited_machine(self):
        source = GUI.read_text()
        self.assertIn('new Spec(MachineKind.SMALL_FREEZER, "small_freezer"', source)
        self.assertIn('"Real corrected machine screen did not open for "', source)
        self.assertIn('describeClientMenu(mc, spec)', source)
        self.assertIn('currentExpectedMenu(mc, spec)', source)

    def test_freezer_uses_thermal_screen_and_live_menu_identity(self):
        source = GUI.read_text()
        self.assertIn('mc.screen instanceof ThermalVerticalScreen', source)
        self.assertIn('menu.containerId != serverMenuId', source)
        self.assertIn('!menu.getClass().getName().equals(serverMenuIdentity)', source)
        self.assertIn('menu.freezerPhase()', source)
        self.assertIn('menu.energyStored()', source)

    def test_freezer_lattice_and_frame_capture_are_not_bypassed(self):
        source = GUI.read_text()
        self.assertIn('ThermalVerticalGeometry.VISIBLE_SLOTS', source)
        self.assertIn('ThermalVerticalGeometry.slot(menuIndex, kind)', source)
        self.assertIn('captureThermalFreezer', source)
        self.assertIn('freezer_progress_ticks', source)
        self.assertIn('freezer_phase', source)

    def test_thermal_screen_hit_test_uses_the_live_canvas_transform(self):
        source = THERMAL.read_text()
        self.assertIn('validationViewTransform()', source)
        self.assertIn('validationSlotHoveredAt(int menuIndex, double screenX, double screenY)', source)
        self.assertIn('view.localX(screenX), view.localY(screenY)', source)


if __name__ == "__main__":
    unittest.main()
