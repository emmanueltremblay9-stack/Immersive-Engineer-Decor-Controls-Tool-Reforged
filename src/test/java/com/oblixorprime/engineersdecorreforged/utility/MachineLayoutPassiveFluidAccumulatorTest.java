package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MachineLayoutPassiveFluidAccumulatorTest {
   @Test
   public void accumulatorUsesReferenceCanvasAndPreservesBucketSlotIndices() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.PASSIVE_FLUID_ACCUMULATOR);

      assertEquals(209, layout.imageWidth());
      assertEquals(238, layout.imageHeight());
      assertEquals(24, layout.playerInventoryX());
      assertEquals(150, layout.playerInventoryY());
      assertEquals(24, layout.hotbarX());
      assertEquals(205, layout.hotbarY());
      assertEquals(layout.playerInventoryX(), layout.hotbarX());
      assertEquals(185, layout.playerInventoryX() + 8 * 18 + 17);
      assertEquals(223, layout.hotbarY() + 18);
      assertEquals(2, layout.machineSlots().size());
      assertEquals(0, layout.machineSlots().get(0).containerIndex());
      assertFalse(layout.machineSlots().get(0).output());
      assertEquals(26, layout.machineSlots().get(0).x());
      assertEquals(106, layout.machineSlots().get(0).y());
      assertEquals(1, layout.machineSlots().get(1).containerIndex());
      assertTrue(layout.machineSlots().get(1).output());
      assertEquals(149, layout.machineSlots().get(1).x());
      assertEquals(106, layout.machineSlots().get(1).y());
      assertEquals("Filled", layout.machineSlots().get(1).role());
      assertEquals(MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, layout.style());
      assertEquals("passive_fluid_accumulator", layout.textureName());
   }

   @Test
   public void accumulatorCropMapsFullNativeRegionToLogicalCanvas() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.PASSIVE_FLUID_ACCUMULATOR);
      GuiTextureBlit blit = new GuiTextureBlit(layout.imageWidth(), layout.imageHeight(), 278, 616, 385, 439, 941, 1672);

      assertEquals(209, blit.destinationWidth());
      assertEquals(238, blit.destinationHeight());
      assertEquals(278, blit.sourceX());
      assertEquals(616, blit.sourceY());
      assertEquals(385, blit.sourceWidth());
      assertEquals(439, blit.sourceHeight());
      assertEquals(941, blit.textureWidth());
      assertEquals(1672, blit.textureHeight());
   }

   @Test(expected = IllegalArgumentException.class)
   public void accumulatorTextureGeometryRejectsCropOutsideSourceImage() {
      new GuiTextureBlit(209, 238, 278, 616, 385, 439, 662, 1672);
   }
}
