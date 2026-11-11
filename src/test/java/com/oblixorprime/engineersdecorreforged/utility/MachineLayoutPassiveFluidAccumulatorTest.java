package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MachineLayoutPassiveFluidAccumulatorTest {
   @Test
   public void retainedCanvasPreservesBucketRolesAndIndices() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.PASSIVE_FLUID_ACCUMULATOR);
      assertEquals(256, layout.imageWidth());
      assertEquals(256, layout.imageHeight());
      assertEquals(44, layout.playerInventoryX());
      assertEquals(138, layout.playerInventoryY());
      assertEquals(44, layout.hotbarX());
      assertEquals(206, layout.hotbarY());
      assertEquals(2, layout.machineSlots().size());
      MachineLayout.MachineSlot input = layout.machineSlots().get(0);
      MachineLayout.MachineSlot output = layout.machineSlots().get(1);
      assertEquals(0, input.containerIndex());
      assertEquals("Empty bucket", input.role());
      assertFalse(input.output());
      assertEquals(40, input.x());
      assertEquals(100, input.y());
      assertEquals(1, output.containerIndex());
      assertEquals("Filled", output.role());
      assertTrue(output.output());
      assertEquals(200, output.x());
      assertEquals(100, output.y());
      assertFalse(layout.isOutputSlot(0));
      assertTrue(layout.isOutputSlot(1));
      assertFalse(layout.allowsAutomationExtraction(0));
      assertTrue(layout.allowsAutomationExtraction(1));
      assertTrue(layout.panels().isEmpty());
      assertEquals(MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, layout.style());
      assertEquals("passive_fluid_accumulator_v1/background", layout.textureName());
   }

   @Test
   public void retainedBlitUsesTheEntireSquareImageWithoutDistortion() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.PASSIVE_FLUID_ACCUMULATOR);
      GuiTextureBlit blit = GuiTextureBlit.stretchFullTexture(
         layout.imageWidth(), layout.imageHeight(),
         FluidMachineRedesignLayout.SOURCE_SIZE, FluidMachineRedesignLayout.SOURCE_SIZE);
      assertEquals(256, blit.destinationWidth());
      assertEquals(256, blit.destinationHeight());
      assertEquals(0, blit.sourceX());
      assertEquals(0, blit.sourceY());
      assertEquals(1254, blit.sourceWidth());
      assertEquals(1254, blit.sourceHeight());
      assertEquals(1254, blit.textureWidth());
      assertEquals(1254, blit.textureHeight());
      assertEquals(blit.sourceWidth() * blit.destinationHeight(),
         blit.sourceHeight() * blit.destinationWidth());
   }

   @Test(expected = IllegalArgumentException.class)
   public void retainedBlitRejectsSourceRegionOutsideTexture() {
      new GuiTextureBlit(256, 256, 1, 0, 1254, 1254, 1254, 1254);
   }

   @Test
   public void liveTankFillProjectsEmptyIntermediateFullAndClampedAmounts() {
      assertEquals(0, PassiveFluidAccumulatorGauge.filledHeight(0, 4000, 50));
      assertEquals(25, PassiveFluidAccumulatorGauge.filledHeight(2000, 4000, 50));
      assertEquals(50, PassiveFluidAccumulatorGauge.filledHeight(4000, 4000, 50));
      assertEquals(50, PassiveFluidAccumulatorGauge.filledHeight(4500, 4000, 50));
      assertEquals(0, PassiveFluidAccumulatorGauge.filledHeight(-1, 4000, 50));
      assertEquals(0, PassiveFluidAccumulatorGauge.filledHeight(500, 0, 50));
   }
}
