package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
      assertFalse(layout.machineSlots().get(0).output());
      assertEquals(30, layout.machineSlots().get(0).x());
      assertEquals(66, layout.machineSlots().get(0).y());
      assertEquals(1, layout.machineSlots().get(1).containerIndex());
      assertFalse(layout.machineSlots().get(1).output());
      assertEquals(22, layout.machineSlots().get(1).x());
      assertEquals(84, layout.machineSlots().get(1).y());
      assertTrue(layout.machineSlots().get(1).role().toLowerCase().contains("cold"));
      assertEquals(2, layout.machineSlots().get(2).containerIndex());
      assertTrue(layout.machineSlots().get(2).output());
      assertEquals(164, layout.machineSlots().get(2).x());
      assertEquals(67, layout.machineSlots().get(2).y());
      assertTrue(layout.machineSlots().get(2).role().toLowerCase().contains("output"));
      assertEquals(MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, layout.style());
      assertEquals("small_water_freezer/background", layout.textureName());
   }

   @Test
   public void runtimeArtworkMasksCoverTheIllustrativeItemsAtTheLogicalCanvasScale() {
      java.util.List<SmallFreezerArtworkMasks.MaskRegion> masks = SmallFreezerArtworkMasks.regions(209, 235);

      assertEquals(2, masks.size());
      assertEquals(28, masks.get(0).x());
      assertEquals(63, masks.get(0).y());
      assertEquals(21, masks.get(0).width());
      assertEquals(24, masks.get(0).height());
      assertEquals(163, masks.get(1).x());
      assertEquals(64, masks.get(1).y());
      assertEquals(20, masks.get(1).width());
      assertEquals(23, masks.get(1).height());
      assertTrue(masks.get(0).x() > 46 * 209 / 422);
      assertTrue(masks.get(0).right() < 107 * 209 / 422);
      assertTrue(masks.get(0).y() > 106 * 235 / 443);
      assertTrue(masks.get(0).bottom() < 174 * 235 / 443);
      assertTrue(masks.get(1).x() > 315 * 209 / 422);
      assertTrue(masks.get(1).right() < 380 * 209 / 422);
      assertTrue(masks.get(1).y() > 108 * 235 / 443);
      assertTrue(masks.get(1).bottom() < 174 * 235 / 443);
      assertTrue(masks.get(0).right() <= 209);
      assertTrue(masks.get(1).right() <= 209);
      assertTrue(masks.get(0).bottom() <= 235);
      assertTrue(masks.get(1).bottom() <= 235);
   }

   @Test
   public void liveMetersFollowTheBakedFreezerTracksAndClearTheirSampleFillsAtZero() {
      SmallFreezerArtworkMasks.MaskRegion progress = SmallFreezerArtworkMasks.progressBar(209, 235);
      SmallFreezerArtworkMasks.MaskRegion energy = SmallFreezerArtworkMasks.energyBar(209, 235);

      assertEquals(61, progress.x());
      assertEquals(104, progress.y());
      assertEquals(89, progress.width());
      assertEquals(12, progress.height());
      assertEquals(62, energy.x());
      assertEquals(124, energy.y());
      assertEquals(87, energy.width());
      assertEquals(19, energy.height());
      assertTrue(progress.bottom() < energy.y());
   }

   @Test
   public void itemSlotsAreCenteredOnTheFrameAndTheColdSlotRemainsReserved() {
      SmallFreezerArtworkMasks.SlotOrigin input = SmallFreezerArtworkMasks.slotOriginForFrame(
         46, 106, 106, 174, 209, 235
      );
      SmallFreezerArtworkMasks.SlotOrigin output = SmallFreezerArtworkMasks.slotOriginForFrame(
         315, 108, 380, 174, 209, 235
      );

      assertEquals(30, input.x());
      assertEquals(66, input.y());
      assertEquals(164, output.x());
      assertEquals(67, output.y());
   }

}
