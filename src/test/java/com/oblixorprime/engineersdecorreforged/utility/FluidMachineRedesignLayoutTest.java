package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import com.oblixorprime.engineersdecorreforged.utility.FluidMachineRedesignLayout.Rect;

public class FluidMachineRedesignLayoutTest {
   private static final MachineKind[] KINDS = {
      MachineKind.SMALL_FLUID_FUNNEL, MachineKind.PASSIVE_FLUID_ACCUMULATOR
   };

   @Test
   public void bothInventoriesUseEveryIndependentlyFittedAnchor() {
      int[] xs = {44, 63, 82, 101, 120, 139, 158, 177, 196};
      int[] ys = {138, 156, 175};
      assertEquals(16, FluidMachineRedesignLayout.SLOT_SIZE);
      assertEquals(206, FluidMachineRedesignLayout.HOTBAR_Y);
      for (MachineKind kind : KINDS) {
         MachineLayout layout = MachineLayout.forKind(kind);
         for (int column = 0; column < 9; column++) {
            assertEquals(xs[column], FluidMachineRedesignLayout.playerSlotX(column));
            assertEquals(xs[column], layout.playerSlotX(column));
            assertEquals(xs[column], layout.hotbarSlotX(column));
         }
         for (int row = 0; row < 3; row++) {
            assertEquals(ys[row], FluidMachineRedesignLayout.playerSlotY(row));
            assertEquals(ys[row], layout.playerSlotY(row));
         }
         // A constant 18-pixel row pitch would incorrectly place the last row at 174.
         assertEquals(19, layout.playerSlotY(2) - layout.playerSlotY(1));
      }
   }

   @Test
   public void allThirtyEightItemRectanglesFitAndNeverOverlap() {
      for (MachineKind kind : KINDS) {
         List<Rect> slots = itemRects(MachineLayout.forKind(kind));
         assertEquals(38, slots.size());
         for (int i = 0; i < slots.size(); i++) {
            Rect slot = slots.get(i);
            assertEquals(16, slot.width());
            assertEquals(16, slot.height());
            assertWithinCanvas(slot);
            for (int j = i + 1; j < slots.size(); j++) {
               assertFalse(kind + " slots " + i + " and " + j, overlaps(slot, slots.get(j)));
            }
         }
      }
   }

   @Test
   public void tooltipRegionsAvoidAllItemsIncludingTheirOnePixelHoverPadding() {
      for (MachineKind kind : KINDS) {
         boolean funnel = kind == MachineKind.SMALL_FLUID_FUNNEL;
         List<Rect> hoverRects = List.of(FluidMachineRedesignLayout.HEADER,
            funnel ? FluidMachineRedesignLayout.FUNNEL_TANK : FluidMachineRedesignLayout.ACCUMULATOR_TANK,
            funnel ? FluidMachineRedesignLayout.FUNNEL_OUTLET : FluidMachineRedesignLayout.ACCUMULATOR_COLLECTION);
         for (Rect hover : hoverRects) {
            assertWithinCanvas(hover);
            for (Rect slot : itemRects(MachineLayout.forKind(kind))) {
               Rect padded = new Rect(slot.x() - 1, slot.y() - 1, slot.width() + 2, slot.height() + 2);
               assertFalse(kind + " tooltip intersects padded item " + slot, overlaps(hover, padded));
            }
         }
      }
   }

   @Test
   public void fillClampsInvalidEmptyFullAndOverflowAmountsWithoutIntegerOverflow() {
      for (Rect tank : List.of(FluidMachineRedesignLayout.FUNNEL_TANK, FluidMachineRedesignLayout.ACCUMULATOR_TANK)) {
         int h = tank.height();
         assertEquals(0, FluidMachineRedesignLayout.fillHeight(-1, 4000, h));
         assertEquals(0, FluidMachineRedesignLayout.fillHeight(Integer.MIN_VALUE, 4000, h));
         assertEquals(0, FluidMachineRedesignLayout.fillHeight(0, 4000, h));
         assertEquals(0, FluidMachineRedesignLayout.fillHeight(500, 0, h));
         assertEquals(0, FluidMachineRedesignLayout.fillHeight(500, -1, h));
         assertEquals(h / 2, FluidMachineRedesignLayout.fillHeight(2000, 4000, h));
         assertEquals(0, FluidMachineRedesignLayout.fillHeight(1, 4000, h));
         assertEquals(h, FluidMachineRedesignLayout.fillHeight(4000, 4000, h));
         assertEquals(h, FluidMachineRedesignLayout.fillHeight(4500, 4000, h));
         assertEquals(h, FluidMachineRedesignLayout.fillHeight(Integer.MAX_VALUE, 4000, h));
         assertEquals(h, FluidMachineRedesignLayout.fillHeight(Integer.MAX_VALUE, Integer.MAX_VALUE, h));
         assertEquals(h - 1, FluidMachineRedesignLayout.fillHeight(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, h));
      }
      assertEquals(0, FluidMachineRedesignLayout.fillHeight(1, 1, 0));
      assertEquals(0, FluidMachineRedesignLayout.fillHeight(1, 1, -1));
      assertEquals(Integer.MAX_VALUE,
         FluidMachineRedesignLayout.fillHeight(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
   }

   @Test
   public void bevelClippingIsSymmetricAndKeepsEveryPaintedPixelInsideGlass() {
      for (Rect tank : List.of(FluidMachineRedesignLayout.FUNNEL_TANK, FluidMachineRedesignLayout.ACCUMULATOR_TANK)) {
         int corner = tank.equals(FluidMachineRedesignLayout.ACCUMULATOR_TANK) ? 3 : 2;
         for (int row = 0; row < tank.height(); row++) {
            int inset = FluidMachineRedesignLayout.glassInset(tank, row);
            assertEquals(Math.max(0, corner - Math.min(row, tank.height() - 1 - row)), inset);
            assertEquals(inset, FluidMachineRedesignLayout.glassInset(tank, tank.height() - row - 1));
            assertTrue(tank.width() - 2 * inset > 0);
            for (int x = tank.x() + inset; x < tank.right() - inset; x++) {
               assertTrue(tank.contains(x, tank.y() + row));
            }
         }
         assertThrows(IllegalArgumentException.class, () -> FluidMachineRedesignLayout.glassInset(tank, -1));
         assertThrows(IllegalArgumentException.class, () -> FluidMachineRedesignLayout.glassInset(tank, tank.height()));
      }
   }

   @Test
   public void rectanglesUseExclusiveRightAndBottomEdges() {
      Rect rect = FluidMachineRedesignLayout.FUNNEL_TANK;
      assertTrue(rect.contains(rect.x(), rect.y()));
      assertTrue(rect.contains(rect.right() - 1, rect.bottom() - 1));
      assertFalse(rect.contains(rect.right(), rect.y()));
      assertFalse(rect.contains(rect.x(), rect.bottom()));
      assertFalse(rect.contains(rect.x() - 1, rect.y()));
      assertFalse(rect.contains(rect.x(), rect.y() - 1));
   }

   @Test
   public void retainedTextureDispatchDoesNotCaptureOtherMachinesOrLegacyArt() {
      assertTrue(FluidMachineRedesignLayout.usesRetainedTexture("small_fluid_funnel_v1/background"));
      assertTrue(FluidMachineRedesignLayout.usesRetainedTexture("passive_fluid_accumulator_v1/background"));
      for (String texture : new String[] {null, "", "small_fluid_funnel", "passive_fluid_accumulator",
            "fluid_barrel/background", "milking_redesign_v1/background", "small_fluid_funnel_v1/background.png"}) {
         assertFalse(FluidMachineRedesignLayout.usesRetainedTexture(texture));
      }
   }

   @Test
   public void inventoryAccessorsRejectCoordinatesOutsideTheirGrids() {
      for (int column : new int[] {-1, 9}) {
         assertThrows(IllegalArgumentException.class, () -> FluidMachineRedesignLayout.playerSlotX(column));
         for (MachineKind kind : KINDS) {
            MachineLayout layout = MachineLayout.forKind(kind);
            assertThrows(IllegalArgumentException.class, () -> layout.playerSlotX(column));
            assertThrows(IllegalArgumentException.class, () -> layout.hotbarSlotX(column));
         }
      }
      for (int row : new int[] {-1, 3}) {
         assertThrows(IllegalArgumentException.class, () -> FluidMachineRedesignLayout.playerSlotY(row));
         for (MachineKind kind : KINDS) {
            assertThrows(IllegalArgumentException.class, () -> MachineLayout.forKind(kind).playerSlotY(row));
         }
      }
   }

   private static List<Rect> itemRects(MachineLayout layout) {
      List<Rect> result = new ArrayList<>();
      for (MachineLayout.MachineSlot slot : layout.machineSlots()) result.add(new Rect(slot.x(), slot.y(), 16, 16));
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) result.add(new Rect(layout.playerSlotX(column), layout.playerSlotY(row), 16, 16));
      }
      for (int column = 0; column < 9; column++) result.add(new Rect(layout.hotbarSlotX(column), layout.hotbarY(), 16, 16));
      return result;
   }

   private static void assertWithinCanvas(Rect rect) {
      assertTrue(rect.x() >= 0 && rect.y() >= 0);
      assertTrue(rect.width() > 0 && rect.height() > 0);
      assertTrue(rect.right() <= FluidMachineRedesignLayout.SIZE);
      assertTrue(rect.bottom() <= FluidMachineRedesignLayout.SIZE);
   }

   private static boolean overlaps(Rect a, Rect b) {
      return a.x() < b.right() && b.x() < a.right() && a.y() < b.bottom() && b.y() < a.bottom();
   }
}
