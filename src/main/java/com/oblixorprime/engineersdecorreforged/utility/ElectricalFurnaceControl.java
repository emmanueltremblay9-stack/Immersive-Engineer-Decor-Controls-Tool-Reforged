package com.oblixorprime.engineersdecorreforged.utility;

/** Existing speed detents plus a relative request evaluated against authoritative server state. */
public final class ElectricalFurnaceControl {
   public static final int SET_SPEED_ACTION = 30;
   public static final int CYCLE_SPEED_ACTION = 32;

   private ElectricalFurnaceControl() { }

   public static boolean validCycleRequest(int direction, int reserved) {
      return (direction == 1 || direction == -1) && reserved == 0;
   }

   /** Retains the legacy absolute selector; only the relative request has a reserved payload field. */
   public static boolean accepts(int action, int valueA, int valueB) {
      return action == SET_SPEED_ACTION
         || action == CYCLE_SPEED_ACTION && validCycleRequest(valueA, valueB);
   }

   /** Off (bottom), low (left), medium (top), high (right), then off again. */
   public static int cycleSpeed(int current, int direction) {
      if (current < 0 || current > 3 || (direction != 1 && direction != -1)) return current;
      return Math.floorMod(current + direction, 4);
   }
}
