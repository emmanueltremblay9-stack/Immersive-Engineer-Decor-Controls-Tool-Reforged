package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MachineLayoutSmallMilkingMachineTest {
   @Test
   public void milkingMachineUsesArtworkCanvasAndKeepsInteractiveSlotIndices() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.SMALL_MILKING_MACHINE);

      assertEquals(228, layout.imageWidth());
      assertEquals(228, layout.imageHeight());
      assertEquals(MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, layout.style());
      assertEquals("milking_redesign_v1/background", layout.textureName());
      assertEquals(2, layout.machineSlots().size());
      assertEquals(0, layout.machineSlots().get(0).containerIndex());
      assertEquals(1, layout.machineSlots().get(1).containerIndex());
      assertFalse(layout.machineSlots().get(0).output());
      assertTrue(layout.machineSlots().get(1).output());
      assertEquals(49, layout.machineSlots().get(0).x());
      assertEquals(163, layout.machineSlots().get(1).x());
      assertTrue(layout.machineSlots().get(0).role().toLowerCase().contains("bucket"));
      assertTrue(layout.machineSlots().get(1).role().toLowerCase().contains("output"));
      assertEquals(26, layout.playerInventoryX());
      assertEquals(132, layout.playerInventoryY());
      assertEquals(26, layout.hotbarX());
      assertEquals(195, layout.hotbarY());
      assertEquals(20, layout.playerRowSpacing());
      assertEquals(132, layout.playerSlotY(0));
      assertEquals(172, layout.playerSlotY(2));
      assertTrue(layout.hotbarY() + 16 <= layout.imageHeight());
   }
}
