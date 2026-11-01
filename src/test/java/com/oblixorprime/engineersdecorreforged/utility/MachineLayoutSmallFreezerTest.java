package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MachineLayoutSmallFreezerTest {
   @Test
   public void smallFreezerUsesContractCanvasAndPreservesMachineSlotIndices() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.SMALL_FREEZER);

      assertEquals(209, layout.imageWidth());
      assertEquals(235, layout.imageHeight());
      assertEquals(24, layout.playerInventoryX());
      assertEquals(145, layout.playerInventoryY());
      assertEquals(24, layout.hotbarX());
      assertEquals(203, layout.hotbarY());
      assertEquals(3, layout.machineSlots().size());
      assertEquals(0, layout.machineSlots().get(0).containerIndex());
      assertEquals(22, layout.machineSlots().get(0).x());
      assertEquals(57, layout.machineSlots().get(0).y());
      assertEquals(1, layout.machineSlots().get(1).containerIndex());
      assertEquals(22, layout.machineSlots().get(1).x());
      assertEquals(84, layout.machineSlots().get(1).y());
      assertTrue(layout.machineSlots().get(1).role().toLowerCase().contains("cold"));
      assertEquals(2, layout.machineSlots().get(2).containerIndex());
      assertEquals(170, layout.machineSlots().get(2).x());
      assertEquals(57, layout.machineSlots().get(2).y());
      assertTrue(layout.machineSlots().get(2).role().toLowerCase().contains("output"));
      assertEquals(MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, layout.style());
      assertEquals("small_water_freezer/background", layout.textureName());
   }
}
