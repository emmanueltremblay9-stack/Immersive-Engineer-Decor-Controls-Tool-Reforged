package com.oblixorprime.engineersdecorreforged.utility;

public final class SmallFluidFunnelFluidSync {
   private static final int OTHER_NONEMPTY_FLUID = 4;

   private SmallFluidFunnelFluidSync() {}

   public static int encode(String fluidType, int amount) {
      if (amount <= 0) {
         return 0;
      }

      if (fluidType == null) {
         return OTHER_NONEMPTY_FLUID;
      }

      return switch (fluidType) {
         case "water" -> 1;
         case "lava" -> 2;
         case "milk" -> 3;
         default -> OTHER_NONEMPTY_FLUID;
      };
   }

   public static int normalize(int code) {
      return code >= 1 && code <= OTHER_NONEMPTY_FLUID ? code : 0;
   }
}
