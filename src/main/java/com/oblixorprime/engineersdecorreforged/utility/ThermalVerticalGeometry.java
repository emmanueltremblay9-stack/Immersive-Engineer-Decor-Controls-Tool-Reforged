package com.oblixorprime.engineersdecorreforged.utility;

/**
 * Shared, text-free 314x314 treatment screen. Coordinates are client presentation only:
 * backing index 0 is input, 2 is output, 1 is a hidden historical Freezer reserve.
 * This does not change serialized inventories or server menu slot ordering.
 */
public final class ThermalVerticalGeometry {
   public static final int CANVAS_WIDTH = 314;
   public static final int CANVAS_HEIGHT = 314;
   public static final int ITEM_SIZE = 16;
   public static final int VISIBLE_SLOTS = 38;
   private static final int[] PLAYER_X = {48, 73, 98, 123, 148, 173, 198, 223, 248};
   private static final int[] PLAYER_Y = {195, 219, 243, 267};

   private ThermalVerticalGeometry() { }

   public record Point(int x, int y) {
      public Point {
         if (x < 0 || y < 0 || x + ITEM_SIZE > CANVAS_WIDTH || y + ITEM_SIZE > CANVAS_HEIGHT)
            throw new IllegalArgumentException("Slot outside the thermal vertical canvas");
      }
   }

   public static Point slot(int menuIndex) {
      if (menuIndex == 0) return new Point(145, 50);
      if (menuIndex == 2) return new Point(145, 160);
      if (menuIndex == 1 || menuIndex < 0 || menuIndex > 38)
         throw new IllegalArgumentException("No visible thermal slot " + menuIndex);
      int index = menuIndex - 3;
      return new Point(PLAYER_X[index % 9], PLAYER_Y[index / 9]);
   }

   public static boolean contains(int menuIndex, double x, double y) {
      Point p = slot(menuIndex);
      return x >= p.x && x < p.x + ITEM_SIZE && y >= p.y && y < p.y + ITEM_SIZE;
   }
}
