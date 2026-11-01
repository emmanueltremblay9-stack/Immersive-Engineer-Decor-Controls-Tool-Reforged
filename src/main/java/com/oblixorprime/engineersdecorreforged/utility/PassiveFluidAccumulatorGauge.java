package com.oblixorprime.engineersdecorreforged.utility;

/** Converts the synchronized fluid amount to a bounded screen fill height. */
public final class PassiveFluidAccumulatorGauge {
   private PassiveFluidAccumulatorGauge() {
   }

   public static int filledHeight(int amount, int capacity, int availableHeight) {
      if (capacity <= 0 || availableHeight <= 0) {
         return 0;
      }

      int boundedAmount = Math.max(0, Math.min(amount, capacity));
      return (int)((long)boundedAmount * availableHeight / capacity);
   }
}
