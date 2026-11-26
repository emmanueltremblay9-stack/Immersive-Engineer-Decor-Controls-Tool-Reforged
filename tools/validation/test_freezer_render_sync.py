"""Structural guards for fixture synchronization; not native rendering proof."""
from pathlib import Path
import unittest

SOURCE = Path(__file__).resolve().parents[2] / 'src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeFreezerReferenceValidation.java'

class RenderSyncTest(unittest.TestCase):
    def test_freezer_fixture_uses_registered_thermal_screen_and_menu_contract(self):
        source = SOURCE.read_text()
        self.assertIn("instanceof ThermalVerticalScreen", source)
        self.assertNotIn("instanceof FreezerReferenceScreen", source)
        self.assertIn("menu.slots.size() == 39", source)
        self.assertIn("menu.getSlot(1).isActive()", source)
        self.assertIn("visible == ThermalVerticalGeometry.VISIBLE_SLOTS", source)
        self.assertIn("ThermalVerticalGeometry.slot(menuIndex, MachineKind.SMALL_FREEZER)", source)

    def test_capture_crop_and_proof_use_real_canvas_dynamic_layers(self):
        source = SOURCE.read_text()
        self.assertIn("ENERGY_LEFT", source)
        self.assertIn("EFFECT_LEFT", source)
        self.assertIn("ThermalVerticalGeometry.energyClipTop(89, 192", source)
        self.assertIn('proof.addProperty("canvas_width", ThermalVerticalGeometry.CANVAS_WIDTH)', source)
        self.assertIn('proof.addProperty("canvas_height", ThermalVerticalGeometry.CANVAS_HEIGHT)', source)

    def test_capture_is_post_render_not_client_tick(self):
        source = SOURCE.read_text()
        tick = source[source.index('static boolean tick('):source.index('@SubscribeEvent\n   public static void beforeFrostRender')]
        self.assertNotIn('++frostStableFrames', tick)
        self.assertNotIn('capture(mc, "frost-phase-', tick)
        post = source[source.index('public static void afterFrostRender'):source.index('private static void enter(')]
        self.assertIn('++frostStableFrames < 3', post)
        self.assertIn('menu.freezerPhase() != renderingPhase', post)
        self.assertIn('menu.freezerProgressTicks() != renderingProgress', post)
        self.assertIn('current(mc) != menu', post)
        self.assertLess(post.index('event.getGuiGraphics().flush()'), post.index('capture(mc, "frost-phase-'))
        self.assertIn('renderFailure = error', post)
        self.assertIn('if (renderFailure != null) throw renderFailure', tick)
        self.assertIn('for (boolean seen : FROST_SEEN) require(seen', tick)

if __name__ == '__main__':
    unittest.main()
