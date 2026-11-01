package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MachineLayoutSmallMilkingMachineTest {
   @Test
   public void milkingMachineUsesArtworkCanvasAndKeepsInteractiveSlotIndices() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.SMALL_MILKING_MACHINE);

      assertEquals(216, layout.imageWidth());
      assertEquals(210, layout.imageHeight());
      assertEquals(MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, layout.style());
      assertEquals("small_milking_machine_gui", layout.textureName());
      assertEquals(2, layout.machineSlots().size());
      assertEquals(0, layout.machineSlots().get(0).containerIndex());
      assertEquals(1, layout.machineSlots().get(1).containerIndex());
      assertFalse(layout.machineSlots().get(0).output());
      assertTrue(layout.machineSlots().get(1).output());
      assertEquals(46, layout.machineSlots().get(0).x());
      assertEquals(156, layout.machineSlots().get(1).x());
      assertTrue(layout.machineSlots().get(0).role().toLowerCase().contains("bucket"));
      assertTrue(layout.machineSlots().get(1).role().toLowerCase().contains("output"));
      assertEquals(27, layout.playerInventoryX());
      assertEquals(136, layout.playerInventoryY());
      assertEquals(27, layout.hotbarX());
      assertEquals(190, layout.hotbarY());
      assertTrue(layout.hotbarY() + 16 <= layout.imageHeight());
   }
}
