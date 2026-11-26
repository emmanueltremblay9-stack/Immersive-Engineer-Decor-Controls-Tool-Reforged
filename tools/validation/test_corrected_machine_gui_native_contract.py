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
        self.assertIn('result.addProperty("visible_machine_slots", 2)', source)
        self.assertIn('result.addProperty("protocol_machine_slots", menu.visibleMachineSlots())', source)
        self.assertIn('result.addProperty("visible_screen_slots", ThermalVerticalGeometry.VISIBLE_SLOTS)', source)
        self.assertIn('capture.addProperty("capture_mode", "native_thermal_canvas_crop")', source)
        self.assertIn('capture.addProperty("framebuffer_width", mc.getWindow().getScreenWidth())', source)
        self.assertIn('capture.addProperty("crop_x", cropX)', source)

    def test_near_end_snapshot_seeds_progress_phase_and_energy_consistently(self):
        source = GUI.read_text()
        setup = source.split('private static void scheduleFreezerNearEnd(')[1].split('static void restoreFreezerSnapshot(', 1)[0]
        self.assertIn('int nearEndProgress = MachineBlockEntity.FREEZER_PROCESS_TICKS - 2;', setup)
        self.assertIn('serverMachine.dataAccessForTests().set(3, 8);', setup)
        self.assertIn('freezerState.setValue(MachineBlocks.PHASE_0_4, Math.min(4, nearEndProgress / 45))', setup)
        self.assertIn('menu.freezerPhase()', source)

    def test_thermal_screen_hit_test_uses_the_live_canvas_transform(self):
        source = THERMAL.read_text()
        self.assertIn('validationViewTransform()', source)
        self.assertIn('validationSlotHoveredAt(int menuIndex, double screenX, double screenY)', source)
        self.assertIn('view.localX(screenX), view.localY(screenY)', source)


if __name__ == "__main__":
    unittest.main()
