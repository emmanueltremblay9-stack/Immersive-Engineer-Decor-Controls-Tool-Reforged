"""Structural guards for fixture synchronization; not native rendering proof."""
from pathlib import Path
import unittest

SOURCE = Path(__file__).resolve().parents[2] / 'src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeFreezerReferenceValidation.java'

class RenderSyncTest(unittest.TestCase):
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
