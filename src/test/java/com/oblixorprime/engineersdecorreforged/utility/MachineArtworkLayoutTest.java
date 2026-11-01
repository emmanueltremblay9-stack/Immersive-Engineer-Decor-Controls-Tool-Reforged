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
      assertSlotsInsideCanvas(layout);
   }

   @Test
   public void treeCutterArtworkKeepsAllNineDropsAndTheIoSlotInsideTheCanvas() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.SMALL_TREE_CUTTER);
      GuiTextureBlit blit = layout.artworkBlit();

      assertEquals(MachineLayout.LayoutStyle.ARTWORK_TEXTURE, layout.style());
      assertEquals("small_tree_cutter_art", layout.textureName());
      assertEquals(446, blit.textureWidth());
      assertEquals(418, blit.textureHeight());
      assertEquals(layout.imageWidth(), blit.destinationWidth());
      assertEquals(layout.imageHeight(), blit.destinationHeight());
      assertEquals(10, layout.machineSlots().size());
      for (int index = 0; index < 9; index++) {
         assertEquals(index, layout.machineSlots().get(index).containerIndex());
         assertEquals("Drops", layout.machineSlots().get(index).role());
      }
      assertEquals(9, layout.machineSlots().get(9).containerIndex());
      assertEquals("I/O", layout.machineSlots().get(9).role());

      for (int i = 0; i < layout.machineSlots().size(); i++) {
         MachineLayout.MachineSlot first = layout.machineSlots().get(i);
         assertTrue(first.x() >= 0);
         assertTrue(first.y() >= 0);
         assertTrue(first.x() + 18 <= layout.imageWidth());
         assertTrue(first.y() + 18 <= layout.imageHeight());
         assertTrue(first.y() + 18 <= layout.playerInventoryY());
         for (int j = i + 1; j < layout.machineSlots().size(); j++) {
            assertFalseOverlap(first, layout.machineSlots().get(j));
         }
      }
      assertTrue(layout.playerInventoryY() + 2 * 18 + 18 <= layout.hotbarY());
      assertTrue(layout.playerInventoryX() + 8 * 18 + 18 <= layout.imageWidth());
      assertTrue(layout.hotbarX() + 8 * 18 + 18 <= layout.imageWidth());
      assertTrue(layout.hotbarY() + 18 <= layout.imageHeight());
   }

   private static void assertSlotsInsideCanvas(MachineLayout layout) {
      assertNotNull(layout.artworkBlit());
      for (MachineLayout.MachineSlot slot : layout.machineSlots()) {
         assertTrue(slot.x() >= 0);
         assertTrue(slot.y() >= 0);
         assertTrue(slot.x() + 18 <= layout.imageWidth());
         assertTrue(slot.y() + 18 <= layout.imageHeight());
      }
      assertTrue(layout.playerInventoryX() + 8 * 18 + 18 <= layout.imageWidth());
      assertTrue(layout.hotbarX() + 8 * 18 + 18 <= layout.imageWidth());
      assertTrue(layout.playerInventoryY() + 2 * 18 + 18 <= layout.hotbarY());
      assertTrue(layout.hotbarY() + 18 <= layout.imageHeight());
   }

   private static void assertFalseOverlap(MachineLayout.MachineSlot first, MachineLayout.MachineSlot second) {
      boolean overlap = first.x() < second.x() + 18 && first.x() + 18 > second.x()
         && first.y() < second.y() + 18 && first.y() + 18 > second.y();
      assertTrue("machine slots overlap: " + first + " / " + second, !overlap);
   }
}
