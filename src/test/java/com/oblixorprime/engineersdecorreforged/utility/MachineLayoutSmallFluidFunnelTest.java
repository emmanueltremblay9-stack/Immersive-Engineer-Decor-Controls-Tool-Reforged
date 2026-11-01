package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

   @Test
   public void bucketArtworkMasksCoverIllustratedOverflowBeyondRuntimeSlots() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.SMALL_FLUID_FUNNEL);
      MachineLayout.MachineSlot inputSlot = layout.machineSlots().get(0);
      MachineLayout.MachineSlot outputSlot = layout.machineSlots().get(1);
      SmallFluidFunnelArtworkMask inputMask = SmallFluidFunnelArtworkMask.sampleBucketMasks().get(0);
      SmallFluidFunnelArtworkMask outputMask = SmallFluidFunnelArtworkMask.sampleBucketMasks().get(1);

      assertTrue(inputMask.left() <= inputSlot.x());
      assertTrue(inputMask.right() > inputSlot.x() + 18);
      assertTrue(outputMask.left() <= outputSlot.x());
      assertTrue(outputMask.right() > outputSlot.x() + 18);
      assertTrue(inputMask.top() < inputSlot.y());
      assertTrue(outputMask.top() < outputSlot.y());
      assertTrue(inputMask.bottom() > inputSlot.y() + 18);
      assertTrue(outputMask.bottom() > outputSlot.y() + 18);
      assertTrue(inputMask.right() < 60);
      assertTrue(outputMask.left() > 154);
      assertTrue(outputMask.right() < 187);
   }

   @Test
   public void funnelBlitMapsWholeNativeTextureToLogicalCanvas() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.SMALL_FLUID_FUNNEL);
      GuiTextureBlit geometry = GuiTextureBlit.stretchFullTexture(layout.imageWidth(), layout.imageHeight(), 385, 439);

      assertEquals(209, geometry.destinationWidth());
      assertEquals(238, geometry.destinationHeight());
      assertEquals(0, geometry.sourceX());
      assertEquals(0, geometry.sourceY());
      assertEquals(385, geometry.sourceWidth());
      assertEquals(439, geometry.sourceHeight());
      assertEquals(385, geometry.textureWidth());
      assertEquals(439, geometry.textureHeight());
   }

   @Test(expected = IllegalArgumentException.class)
   public void textureBlitRejectsSourceRegionLargerThanTexture() {
      new GuiTextureBlit(209, 238, 0, 0, 385, 439, 384, 439);
   }
}
