package com.oblixorprime.engineersdecorreforged.utility;

/** Read-only presentation arithmetic. No tank capacity, transfers or persistence changes. */
public final class BarrelReferenceState {
   // Conservative rectangle wholly inside the measured dark aperture, including chamfers.
   // Coordinates are on the exact 1:3 canvas: source [510,633) x [345,579).
   public static final int TANK_X = 170, TANK_Y = 115, TANK_WIDTH = 41, TANK_HEIGHT = 78;
   private BarrelReferenceState() {}
   public static int fillHeight(int amount, int capacity) {
      if (amount <= 0 || capacity <= 0) return 0;
      return (int)(Math.min((long)amount, capacity) * TANK_HEIGHT / capacity);
   }
   public static boolean containsTank(double x, double y) {
      return x >= TANK_X && x < TANK_X + TANK_WIDTH && y >= TANK_Y && y < TANK_Y + TANK_HEIGHT;
   }
}
