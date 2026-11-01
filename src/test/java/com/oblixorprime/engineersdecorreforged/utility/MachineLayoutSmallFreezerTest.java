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

   @Test
   public void runtimeArtworkMasksCoverTheIllustrativeItemsAtTheLogicalCanvasScale() {
      java.util.List<SmallFreezerArtworkMasks.MaskRegion> masks = SmallFreezerArtworkMasks.regions(209, 235);

      assertEquals(2, masks.size());
      assertEquals(28, masks.get(0).x());
      assertEquals(64, masks.get(0).y());
      assertEquals(22, masks.get(0).width());
      assertEquals(21, masks.get(0).height());
      assertEquals(161, masks.get(1).x());
      assertEquals(64, masks.get(1).y());
      assertEquals(22, masks.get(1).width());
      assertEquals(22, masks.get(1).height());
      assertTrue(masks.get(0).right() <= 209);
      assertTrue(masks.get(1).right() <= 209);
      assertTrue(masks.get(0).bottom() <= 235);
      assertTrue(masks.get(1).bottom() <= 235);
   }
}
