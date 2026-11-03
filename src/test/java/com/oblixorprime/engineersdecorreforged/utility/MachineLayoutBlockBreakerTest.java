package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MachineLayoutBlockBreakerTest {
   @Test
   public void untexturedMachineLayoutsStillExposeTheirPlayerSlots() {
      for (MachineKind kind : MachineKind.values()) {
         MachineLayout layout = MachineLayout.forKind(kind);
         for (int column = 0; column < 9; column++) {
            assertTrue(layout.playerSlotX(column) >= 0);
            assertTrue(layout.hotbarSlotX(column) >= 0);
         }
         for (int row = 0; row < 3; row++) assertTrue(layout.playerSlotY(row) >= 0);
      }
   }

   @Test
   public void blockBreakerExposesOneOutputAndVanillaPlayerInventory() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.SMALL_BLOCK_BREAKER);

      assertEquals(256, layout.imageWidth());
      assertEquals(256, layout.imageHeight());
      assertEquals(34, layout.playerInventoryX());
      assertEquals(161, layout.playerInventoryY());
      assertEquals(34, layout.hotbarX());
      assertEquals(226, layout.hotbarY());
      assertEquals(1, layout.machineSlots().size());
      assertEquals(0, layout.machineSlots().get(0).containerIndex());
      assertTrue(layout.machineSlots().get(0).role().toLowerCase().contains("output"));
      assertEquals(MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, layout.style());
      assertEquals("block_breaker_rp_v3/background", layout.textureName());
   }

   @Test
   public void blockBreakerLayoutDoesNotChangeTreeCutterSlots() {
      MachineLayout breaker = MachineLayout.forKind(MachineKind.SMALL_BLOCK_BREAKER);
      MachineLayout treeCutter = MachineLayout.forKind(MachineKind.SMALL_TREE_CUTTER);

      assertEquals(1, breaker.machineSlots().size());
      assertEquals(10, treeCutter.machineSlots().size());
      assertFalse(treeCutter.machineSlots().stream().anyMatch(slot -> slot.role().toLowerCase().contains("output")));
   }
}
