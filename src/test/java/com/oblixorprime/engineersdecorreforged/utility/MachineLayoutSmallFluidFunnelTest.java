package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MachineLayoutSmallFluidFunnelTest {
   @Test
   public void funnelUsesTheReferenceScaledCanvasAndExistingSlots() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.SMALL_FLUID_FUNNEL);
      assertEquals(209, layout.imageWidth());
      assertEquals(238, layout.imageHeight());
      assertEquals(26, layout.playerInventoryX());
      assertEquals(151, layout.playerInventoryY());
      assertEquals(26, layout.hotbarX());
      assertEquals(208, layout.hotbarY());
      assertEquals(2, layout.machineSlots().size());
      assertEquals(0, layout.machineSlots().get(0).containerIndex());
      assertEquals(33, layout.machineSlots().get(0).x());
      assertEquals(85, layout.machineSlots().get(0).y());
      assertEquals(1, layout.machineSlots().get(1).containerIndex());
      assertEquals("Filled", layout.machineSlots().get(1).role());
      assertEquals(155, layout.machineSlots().get(1).x());
      assertEquals(85, layout.machineSlots().get(1).y());
      assertEquals(MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, layout.style());
      assertEquals("small_fluid_funnel", layout.textureName());
   }
}
