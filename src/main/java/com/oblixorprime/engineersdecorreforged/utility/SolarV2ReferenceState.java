package com.oblixorprime.engineersdecorreforged.utility;

/** Read-only live FE presentation. No energy, charging or comparator semantics change. */
public final class SolarV2ReferenceState {
   // Source safe region is [146,588,997,625). Round inwards on the exact 1:3
   // canvas, leaving every bevel/frame pixel outside the dynamic overlay.
   public static final int METER_X = 49, METER_Y = 196, METER_WIDTH = 283, METER_HEIGHT = 12;
   // Reuse only the existing legacy live-meter colours, never its dark backing.
   public static final int FILL_COLOR = 0xFFF2A432, HIGHLIGHT_COLOR = 0xFFFFD06A;
   private SolarV2ReferenceState() {}

   public static int fillWidth(int amount, int capacity) {
      if (amount <= 0 || capacity <= 0) return 0;
      return (int)(Math.min((long)amount, capacity) * METER_WIDTH / capacity);
   }

   public static boolean containsMeter(double x, double y) {
      return x >= METER_X && x < METER_X + METER_WIDTH && y >= METER_Y && y < METER_Y + METER_HEIGHT;
   }
}
