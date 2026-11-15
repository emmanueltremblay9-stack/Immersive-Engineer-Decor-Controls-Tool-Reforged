package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MachineArtworkLayoutTest {
   @Test
   public void solarArtworkMapsTheNativeCropToTheGuiCanvasAndKeepsCellSlotZero() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.SMALL_SOLAR_PANEL);
      GuiTextureBlit blit = layout.artworkBlit();

      assertEquals(MachineLayout.LayoutStyle.ARTWORK_TEXTURE, layout.style());
      assertEquals("small_solar_panel_art", layout.textureName());
      assertEquals(228, layout.imageWidth());
      assertEquals(212, layout.imageHeight());
      assertEquals(455, blit.textureWidth());
      assertEquals(424, blit.textureHeight());
      assertEquals(layout.imageWidth(), blit.destinationWidth());
      assertEquals(layout.imageHeight(), blit.destinationHeight());
      assertEquals(1, layout.machineSlots().size());
      assertEquals(0, layout.machineSlots().get(0).containerIndex());
      assertEquals("Cell", layout.machineSlots().get(0).role());
      assertEquals(20, layout.playerColumnSpacing());
      assertEquals(16, layout.playerRowSpacing());
      assertEquals(24, layout.playerSlotX(0));
      assertEquals(185, layout.hotbarY());
      assertEquals(184, layout.hotbarSlotX(8));
      assertSlotsInsideCanvas(layout);
   }

   private static void assertSlotsInsideCanvas(MachineLayout layout) {
      assertNotNull(layout.artworkBlit());
      for (MachineLayout.MachineSlot slot : layout.machineSlots()) {
         assertTrue(slot.x() >= 0);
         assertTrue(slot.y() >= 0);
         assertTrue(slot.x() + 18 <= layout.imageWidth());
         assertTrue(slot.y() + 18 <= layout.imageHeight());
      }
      assertTrue(layout.playerSlotX(8) + 16 <= layout.imageWidth());
      assertTrue(layout.hotbarSlotX(8) + 16 <= layout.imageWidth());
      assertTrue(layout.playerSlotY(2) + 16 <= layout.hotbarY());
      assertTrue(layout.hotbarY() + 16 <= layout.imageHeight());
   }

}
