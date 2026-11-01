package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
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
      assertEquals(1, layout.machineSlots().get(1).containerIndex());
      assertEquals(MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, layout.style());
      assertEquals("fluid_barrel/background", layout.textureName());
   }
}
