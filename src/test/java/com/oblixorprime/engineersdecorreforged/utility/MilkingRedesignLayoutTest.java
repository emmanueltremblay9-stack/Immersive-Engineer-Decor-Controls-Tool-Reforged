package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import com.oblixorprime.engineersdecorreforged.utility.MilkingRedesignLayout.Rect;
import com.oblixorprime.engineersdecorreforged.utility.MilkingRedesignLayout.Sprite;

public class MilkingRedesignLayoutTest {
   @Test
   public void allThirtyEightNativeSlotsFitWithoutOverlap() {
      List<Rect> slots = new ArrayList<>();
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) slots.add(MilkingRedesignLayout.playerSlot(column, row));
      }
      for (int column = 0; column < 9; column++) slots.add(MilkingRedesignLayout.hotbarSlot(column));
      assertEquals(36, slots.size());
      slots.add(MilkingRedesignLayout.INPUT_SLOT);
      slots.add(MilkingRedesignLayout.OUTPUT_SLOT);
      for (int i = 0; i < slots.size(); i++) {
         Rect a = slots.get(i);
         assertEquals(16, a.width());
         assertEquals(16, a.height());
         assertInside(a, MilkingRedesignLayout.CANVAS);
         for (int j = 0; j < i; j++) assertFalse(overlaps(a, slots.get(j)));
      }
      assertEquals(new Rect(26, 132, 16, 16), slots.get(0));
      assertEquals(new Rect(186, 172, 16, 16), slots.get(26));
      assertEquals(new Rect(186, 195, 16, 16), slots.get(35));
   }

   @Test
   public void spriteCropsAndDestinationsStayInsideTheirCanvases() {
      Rect texture = new Rect(0, 0, 1254, 1254);
      for (Sprite sprite : List.of(MilkingRedesignLayout.BACKGROUND, MilkingRedesignLayout.COW,
         MilkingRedesignLayout.MOOSHROOM, MilkingRedesignLayout.PORTRAIT_VEIL,
         MilkingRedesignLayout.MILK, MilkingRedesignLayout.CREAM, MilkingRedesignLayout.GLASS,
         MilkingRedesignLayout.ENERGY, MilkingRedesignLayout.PROGRESS)) {
         assertInside(sprite.source(), texture);
         assertInside(sprite.destination(), MilkingRedesignLayout.CANVAS);
      }
      assertInside(MilkingRedesignLayout.COW.destination(), MilkingRedesignLayout.PORTRAIT);
      assertInside(MilkingRedesignLayout.MOOSHROOM.destination(), MilkingRedesignLayout.PORTRAIT);
      assertInside(MilkingRedesignLayout.TANK, MilkingRedesignLayout.GLASS.destination());
      assertNotEquals(MilkingRedesignLayout.MILK.source(), MilkingRedesignLayout.CREAM.source());
      assertEquals(228, MilkingRedesignLayout.WIDTH);
      assertEquals(228, MilkingRedesignLayout.HEIGHT);
   }

   @Test
   public void fillsClampAndAvoidIntegerOverflow() {
      assertEquals(0, MilkingRedesignLayout.fillPixels(-1, 100, 51));
      assertEquals(0, MilkingRedesignLayout.fillPixels(100, 0, 51));
      assertEquals(0, MilkingRedesignLayout.fillPixels(100, -1, 51));
      assertEquals(0, MilkingRedesignLayout.fillPixels(100, 100, -1));
      assertEquals(25, MilkingRedesignLayout.fillPixels(50, 100, 51));
      assertEquals(51, MilkingRedesignLayout.fillPixels(101, 100, 51));
      assertEquals(Integer.MAX_VALUE, MilkingRedesignLayout.fillPixels(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
      assertEquals(new Rect(104, 100, 22, 0), MilkingRedesignLayout.bottomFill(MilkingRedesignLayout.TANK, 0, 100));
      assertEquals(new Rect(104, 75, 22, 25), MilkingRedesignLayout.bottomFill(MilkingRedesignLayout.TANK, 50, 100));
      assertEquals(MilkingRedesignLayout.TANK, MilkingRedesignLayout.bottomFill(MilkingRedesignLayout.TANK, 100, 100));
      Rect energy = MilkingRedesignLayout.ENERGY.destination();
      assertEquals(new Rect(163, 44, 20, 7), MilkingRedesignLayout.leftFill(energy, 50, 100));
      assertEquals(energy, MilkingRedesignLayout.leftFill(energy, Integer.MAX_VALUE, 100));
   }

   @Test
   public void inventoryIndicesAreValidated() {
      assertThrows(IllegalArgumentException.class, () -> MilkingRedesignLayout.playerSlot(-1, 0));
      assertThrows(IllegalArgumentException.class, () -> MilkingRedesignLayout.playerSlot(9, 0));
      assertThrows(IllegalArgumentException.class, () -> MilkingRedesignLayout.playerSlot(0, -1));
      assertThrows(IllegalArgumentException.class, () -> MilkingRedesignLayout.playerSlot(0, 3));
      assertThrows(IllegalArgumentException.class, () -> MilkingRedesignLayout.hotbarSlot(-1));
      assertThrows(IllegalArgumentException.class, () -> MilkingRedesignLayout.hotbarSlot(9));
   }

   private static void assertInside(Rect inner, Rect outer) {
      assertTrue(inner.x() >= outer.x());
      assertTrue(inner.y() >= outer.y());
      assertTrue(inner.x() + inner.width() <= outer.x() + outer.width());
      assertTrue(inner.y() + inner.height() <= outer.y() + outer.height());
   }

   private static boolean overlaps(Rect a, Rect b) {
      return a.x() < b.x() + b.width() && b.x() < a.x() + a.width()
         && a.y() < b.y() + b.height() && b.y() < a.y() + a.height();
   }
}
