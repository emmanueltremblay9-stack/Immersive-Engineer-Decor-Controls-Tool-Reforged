package com.oblixorprime.engineersdecorreforged.utility;

/** Minecraft anchors measured from the GUI11-r3 Solar Array contract. */
public final class SolarGui11Geometry {
   public static final int SOURCE_WIDTH = 1312;
   public static final int SOURCE_HEIGHT = 1199;
   public static final float NATIVE_TO_LOGICAL = 22.0F / 93.0F;
   public static final double CANVAS_WIDTH = SOURCE_WIDTH * (22.0D / 93.0D);
   public static final double CANVAS_HEIGHT = SOURCE_HEIGHT * (22.0D / 93.0D);
   public static final int METER_X = 887;
   public static final int METER_Y = 499;
   public static final int METER_WIDTH = 185;
   public static final int METER_HEIGHT = 24;
   // The GUI transform uses a float scale. Accept its subpixel rounding at
   // the inclusive edges while keeping the rendered right/bottom edge exclusive.
   private static final double HIT_TEST_EPSILON = 0.00001D;

   // Vanilla menu order: Cell, three rows of inventory, then hotbar.
   // Use the nearest integer to each 16-by-16 item's measured origin.
   private static final int[][] ITEM_ORIGINS = {
      {224, 76},
      {57, 159}, {80, 159}, {103, 159}, {125, 159}, {148, 159}, {170, 159}, {192, 159}, {215, 159}, {237, 159},
      {57, 182}, {80, 182}, {103, 182}, {125, 182}, {148, 182}, {170, 182}, {192, 182}, {215, 182}, {237, 182},
      {57, 205}, {80, 205}, {103, 205}, {125, 205}, {148, 205}, {170, 205}, {192, 205}, {215, 205}, {237, 205},
      {57, 238}, {80, 238}, {103, 238}, {125, 238}, {147, 238}, {170, 238}, {192, 238}, {214, 238}, {237, 238}
   };

   private SolarGui11Geometry() {}

   public static int slotX(int index) { return ITEM_ORIGINS[index][0]; }

   public static int slotY(int index) { return ITEM_ORIGINS[index][1]; }

   public static SolarV2ReferenceGeometry.ViewTransform fit(int width, int height) {
      return SolarV2ReferenceGeometry.fit(width, height, CANVAS_WIDTH, CANVAS_HEIGHT);
   }

   public static boolean containsMeter(double x, double y) {
      double scale = NATIVE_TO_LOGICAL;
      double left = METER_X * scale;
      double top = METER_Y * scale;
      double right = (METER_X + METER_WIDTH) * scale;
      double bottom = (METER_Y + METER_HEIGHT) * scale;
      return x >= left - HIT_TEST_EPSILON && x < right - HIT_TEST_EPSILON
         && y >= top - HIT_TEST_EPSILON && y < bottom - HIT_TEST_EPSILON;
   }
}
