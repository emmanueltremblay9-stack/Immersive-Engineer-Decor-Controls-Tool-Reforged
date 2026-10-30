package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MachineLayoutBlockBreakerTest {
   @Test
   public void blockBreakerExposesOneOutputAndVanillaPlayerInventory() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.SMALL_BLOCK_BREAKER);

      assertEquals(209, layout.imageWidth());
      assertEquals(235, layout.imageHeight());
      assertEquals(23, layout.playerInventoryX());
      assertEquals(144, layout.playerInventoryY());
      assertEquals(23, layout.hotbarX());
      assertEquals(202, layout.hotbarY());
      assertEquals(1, layout.machineSlots().size());
      assertEquals(0, layout.machineSlots().get(0).containerIndex());
      assertTrue(layout.machineSlots().get(0).role().toLowerCase().contains("output"));
      assertEquals(MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, layout.style());
      assertEquals("block_breaker_v08/background", layout.textureName());
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
