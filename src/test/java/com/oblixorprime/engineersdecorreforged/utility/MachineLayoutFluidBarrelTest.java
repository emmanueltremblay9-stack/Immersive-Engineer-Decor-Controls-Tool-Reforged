package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MachineLayoutFluidBarrelTest {
   @Test
   public void fluidBarrelPreservesTheTwoBucketSlotsAndPlayerInventory() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.FLUID_BARREL);

      assertEquals(201, layout.imageWidth());
      assertEquals(226, layout.imageHeight());
      assertEquals(24, layout.playerInventoryX());
      assertEquals(144, layout.playerInventoryY());
      assertEquals(24, layout.hotbarX());
      assertEquals(202, layout.hotbarY());
      assertEquals(2, layout.machineSlots().size());
      assertEquals(0, layout.machineSlots().get(0).containerIndex());
      assertEquals(34, layout.machineSlots().get(0).x());
      assertEquals(73, layout.machineSlots().get(0).y());
      assertEquals(1, layout.machineSlots().get(1).containerIndex());
      assertEquals(158, layout.machineSlots().get(1).x());
      assertEquals(73, layout.machineSlots().get(1).y());
      assertEquals("Filled", layout.machineSlots().get(1).role());
      assertFalse(layout.machineSlots().get(0).output());
      assertTrue(layout.machineSlots().get(1).output());
      assertFalse(layout.isOutputSlot(0));
      assertTrue(layout.isOutputSlot(1));
      assertEquals(MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, layout.style());
      assertEquals("fluid_barrel/background", layout.textureName());
   }

   @Test
   public void automationExtractionRefusesBucketInputAndAllowsFilledOutput() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.FLUID_BARREL);

      assertFalse(layout.allowsAutomationExtraction(0));
      assertTrue(layout.allowsAutomationExtraction(1));
   }
}
