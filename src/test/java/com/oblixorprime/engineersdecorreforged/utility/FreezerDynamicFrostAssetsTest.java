package com.oblixorprime.engineersdecorreforged.utility;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

/** Pixel/resource invariants only, not native rendering or final art acceptance. */
public class FreezerDynamicFrostAssetsTest {
   private static final Path ROOT = Path.of("src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/freezer_reference_v5");
   private static BufferedImage read(String name) throws IOException {
      BufferedImage image = ImageIO.read(ROOT.resolve(name).toFile());
      assertNotNull(image); assertEquals(1254, image.getWidth()); assertEquals(1254, image.getHeight());
      return image;
   }

   @Test public void correctedLayersPreserveHousingAndLeavePhaseZeroEmpty() throws Exception {
      BufferedImage canonical = read("background.png"), clean = read("freezer_v5_chamber_clean_base.png");
      BufferedImage mask = read("freezer_v5_chamber_mask_alpha.png");
      var distinct = new HashSet<Long>();
      for (int phase = 0; phase <= 4; phase++) {
         BufferedImage frost = read("freezer_v5_frost_overlay_phase" + phase + ".png");
         long alphaSum = 0;
         for (int y = 0; y < 1254; y++) for (int x = 0; x < 1254; x++) {
            int alpha = frost.getRGB(x, y) >>> 24;
            int coverage = mask.getRaster().getSample(x, y, 0);
            alphaSum += alpha;
            boolean hinge = (x >= 378 && x < 442 && y >= 432 && y < 467)
               || (x >= 811 && x < 864 && y >= 431 && y < 467);
            if (coverage == 0 || hinge) {
               assertEquals("outside chamber/hinge changed " + x + "," + y, canonical.getRGB(x, y), clean.getRGB(x, y));
               assertEquals("frost outside chamber/hinge", 0, alpha);
            }
         }
         if (phase == 0) assertEquals("idle overlay must have no active frost", 0L, alphaSum);
         else assertTrue("phase has visible frost", alphaSum > 0);
         assertTrue("distinct phase coverage", distinct.add(alphaSum));
      }
   }

   @Test public void normalModeIsDynamicAndHistoricalExactStillWins() {
      var energy = FreezerReferenceLiveState.energy(8000, 16000);
      assertEquals(FreezerReferenceLiveState.PreviewMode.LIVE_ENERGY, FreezerReferenceLiveState.previewMode(false, false));
      assertFalse(FreezerReferenceLiveState.energyPatches(FreezerReferenceLiveState.previewMode(false, false), energy).isEmpty());
      assertEquals(FreezerReferenceLiveState.PreviewMode.EXACT_SNAPSHOT, FreezerReferenceLiveState.previewMode(true, false));
      assertTrue(FreezerReferenceLiveState.energyPatches(FreezerReferenceLiveState.previewMode(true, false), energy).isEmpty());
   }

   @Test public void phaseUsesServerFactNotClientProgressOrStatus() {
      for (int phase = 0; phase <= 4; phase++) {
         var state = FreezerReferenceLiveState.frost(1, 180, phase, 0, true);
         assertEquals(phase, state.phase());
         assertEquals(1, state.ticks());
      }
      assertEquals(0, FreezerReferenceLiveState.frost(180, 180, -1, 0, false).phase());
      assertEquals(4, FreezerReferenceLiveState.frost(0, 180, 9, 0, false).phase());
   }
}
