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
        self.assertIn("getResource(Gui11V5CArtwork.texture(FREEZER))", source)
        self.assertIn("Gui11V5CRuntimeGeometry.energy(FREEZER)", source)
        self.assertIn('Gui11V5CRuntimeGeometry.effectBounds(FREEZER, "cold-chamber")', source)
        self.assertIn("Gui11V5CArtwork.energyFillPixels(FREEZER, menu.energyStored(), menu.energyCapacity())", source)
        self.assertIn('proof.addProperty("canvas_width", ThermalVerticalGeometry.canvasWidth(FREEZER))', source)
        self.assertIn('proof.addProperty("canvas_height", ThermalVerticalGeometry.canvasHeight(FREEZER))', source)

    def test_v5c_energy_fill_uses_measured_glass_without_zero_state_overlay(self):
        root = Path(__file__).resolve().parents[2]
        artwork = (root / 'src/main/java/com/oblixorprime/engineersdecorreforged/client/Gui11V5CArtwork.java').read_text()
        geometry = (root / 'src/main/java/com/oblixorprime/engineersdecorreforged/utility/Gui11V5CRuntimeGeometry.java').read_text()
        screen = (root / 'src/main/java/com/oblixorprime/engineersdecorreforged/client/ThermalVerticalScreen.java').read_text()
        energy = artwork.split('public static void drawEnergy(GuiGraphics graphics, MachineMenu menu, int originX, int originY)', 1)[1].split('public static int energyFillPixels', 1)[0]
        fill = artwork.split('public static int energyFillPixels', 1)[1].split('/**', 1)[0]
        self.assertIn('Gui11V5CArtwork.draw(graphics, menu)', screen)
        self.assertIn('Gui11V5CRuntimeGeometry.energy(menu.kind())', energy)
        self.assertIn('menu.energyCapacity() <= 0 || menu.energyStored() <= 0) return', energy)
        self.assertIn('int insetX = vertical ? 3 : 4;', energy)
        self.assertIn('int insetY = vertical ? 4 : 3;', energy)
        self.assertIn('y + height - filled', energy)  # vertical track fills bottom-up
        self.assertIn('x + filled', energy)  # horizontal track fills left-to-right
        self.assertIn('Math.min(stored, capacity)', fill)
        self.assertIn('case "05"', geometry)
        self.assertIn('case "06"', geometry)

    def test_capture_is_post_render_not_client_tick(self):
        source = SOURCE.read_text()
        tick = source[source.index('static boolean tick('):source.index('@SubscribeEvent\n   public static void beforeFrostRender')]
        self.assertNotIn('++frostStableFrames', tick)
        self.assertNotIn('capture(mc, "frost-phase-', tick)
        post = source[source.index('public static void afterFrostRender'):source.index('private static void enter(')]
        pre = source[source.index('public static void beforeFrostRender'):source.index('@SubscribeEvent\n   public static void afterFrostRender')]
        self.assertIn('renderingEnergy = menu.energyStored()', pre)
        self.assertIn('++frostStableFrames < 3', post)
        self.assertIn('menu.freezerPhase() != renderingPhase', post)
        self.assertIn('menu.freezerProgressTicks() != renderingProgress', post)
        self.assertIn('menu.energyStored() != renderingEnergy', post)
        self.assertIn('1440 - renderingProgress * (1440 / MachineBlockEntity.FREEZER_PROCESS_TICKS)', post)
        self.assertIn('renderingEnergy != expectedEnergy', post)
        self.assertIn('current(mc) != menu', post)
        self.assertLess(post.index('event.getGuiGraphics().flush()'), post.index('capture(mc, "frost-phase-'))
        self.assertIn('renderFailure = error', post)
        self.assertIn('if (renderFailure != null) throw renderFailure', tick)
        self.assertIn('for (boolean seen : FROST_SEEN) require(seen', tick)

if __name__ == '__main__':
    unittest.main()
