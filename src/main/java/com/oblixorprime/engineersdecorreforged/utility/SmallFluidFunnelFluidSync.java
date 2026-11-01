package com.oblixorprime.engineersdecorreforged.utility;

public final class SmallFluidFunnelFluidSync {
   private SmallFluidFunnelFluidSync() {}

   public static int encode(String fluidType, int amount) {
      if (amount <= 0 || fluidType == null) {
         return 0;
      }

      return switch (fluidType) {
         case "water" -> 1;
         case "lava" -> 2;
         case "milk" -> 3;
         default -> 0;
      };
   }

   public static int normalize(int code) {
      return code >= 1 && code <= 3 ? code : 0;
   }
}
