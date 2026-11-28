package com.oblixorprime.engineersdecorreforged.utility;

/** Read-only fluid overlay inside the dark glass of GUI11-R3 V5-Corrigee #15. */
public final class BarrelReferenceState {
   // Pixel-reviewed safe interior of the source PNG's beveled glass (1182x1330
   // source to 292x329 logical). The narrower rectangle stays inside the chamfers.
   public static final int TANK_X = 134, TANK_Y = 80, TANK_WIDTH = 26, TANK_HEIGHT = 52;
   private BarrelReferenceState() {}
   public static int fillHeight(int amount, int capacity) {
      if (amount <= 0 || capacity <= 0) return 0;
      return (int)(Math.min((long)amount, capacity) * TANK_HEIGHT / capacity);
   }
   public static boolean containsTank(double x, double y) {
      return x >= TANK_X && x < TANK_X + TANK_WIDTH && y >= TANK_Y && y < TANK_Y + TANK_HEIGHT;
   }
}
