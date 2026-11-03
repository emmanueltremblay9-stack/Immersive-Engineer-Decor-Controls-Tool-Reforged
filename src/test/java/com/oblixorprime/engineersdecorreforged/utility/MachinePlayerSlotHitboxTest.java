package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class MachinePlayerSlotHitboxTest {
   @Test
   public void milkingAndSolarSlotsStayInsideTheCanvasWithoutOverlappingClickRegions() {
      assertNoHitboxOverlap(MachineLayout.forKind(MachineKind.SMALL_MILKING_MACHINE));
      assertNoHitboxOverlap(MachineLayout.forKind(MachineKind.SMALL_SOLAR_PANEL));
   }

   @Test
   public void solarSixteenPixelRowsMeetAtEdgesAndDoNotShareAnyHoverPixel() {
      for (MachineKind kind : List.of(MachineKind.SMALL_SOLAR_PANEL)) {
         MachineLayout layout = MachineLayout.forKind(kind);
         assertEquals(layout.playerSlotY(0) + 16, layout.playerSlotY(1));
         assertEquals(layout.playerSlotY(1) + 16, layout.playerSlotY(2));
         assertEquals(layout.playerSlotY(2) + 16, layout.hotbarY());
      }
   }

   @Test
   public void redesignedMilkingRowsKeepTheArtworkGapsBetweenSixteenPixelHitboxes() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.SMALL_MILKING_MACHINE);
      assertEquals(layout.playerSlotY(0) + 20, layout.playerSlotY(1));
      assertEquals(layout.playerSlotY(1) + 20, layout.playerSlotY(2));
      assertEquals(layout.playerSlotY(2) + 23, layout.hotbarY());
      assertNoHitboxOverlap(layout);
   }

   private static void assertNoHitboxOverlap(MachineLayout layout) {
      List<Hitbox> hitboxes = new ArrayList<>();
      for (MachineLayout.MachineSlot slot : layout.machineSlots()) {
         hitboxes.add(new Hitbox(slot.x(), slot.y(), "machine " + slot.containerIndex()));
      }
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            hitboxes.add(new Hitbox(layout.playerSlotX(column), layout.playerSlotY(row), "inventory " + row + "," + column));
         }
      }
      for (int column = 0; column < 9; column++) {
         hitboxes.add(new Hitbox(layout.hotbarSlotX(column), layout.hotbarY(), "hotbar " + column));
      }

      for (int i = 0; i < hitboxes.size(); i++) {
         Hitbox first = hitboxes.get(i);
         assertTrue(first.label() + " left bound", first.x() >= 0);
         assertTrue(first.label() + " top bound", first.y() >= 0);
         assertTrue(first.label() + " right bound", first.x() + 16 <= layout.imageWidth());
         assertTrue(first.label() + " bottom bound", first.y() + 16 <= layout.imageHeight());
         for (int j = i + 1; j < hitboxes.size(); j++) {
            Hitbox second = hitboxes.get(j);
            boolean overlaps = first.x() < second.x() + 16 && first.x() + 16 > second.x()
               && first.y() < second.y() + 16 && first.y() + 16 > second.y();
            assertFalse(first.label() + " overlaps " + second.label(), overlaps);
         }
      }
   }

   private record Hitbox(int x, int y, String label) {
   }
}
