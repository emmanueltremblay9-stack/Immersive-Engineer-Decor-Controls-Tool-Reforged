package com.oblixorprime.engineersdecorreforged.utility;

public final class MachineBlockBreakerDataSync {
   public static final int HARDNESS_LOW_FIELD = 7;
   public static final int HARDNESS_MIDDLE_FIELD = 14;
   public static final int HARDNESS_HIGH_FIELD = 15;

   // Container data transports signed shorts, so each synchronized component must stay positive.
   private static final int LOWER_COMPONENT_MASK = 0x7FFF;
   private static final int UPPER_COMPONENT_MASK = 0x3;
   private static final int COMPONENT_SHIFT = 15;

   private MachineBlockBreakerDataSync() {
   }

   public static int hardnessFieldValue(int bits, int field) {
      return switch (field) {
         case HARDNESS_LOW_FIELD -> bits & LOWER_COMPONENT_MASK;
         case HARDNESS_MIDDLE_FIELD -> (bits >>> COMPONENT_SHIFT) & LOWER_COMPONENT_MASK;
         case HARDNESS_HIGH_FIELD -> (bits >>> (COMPONENT_SHIFT * 2)) & UPPER_COMPONENT_MASK;
         default -> throw new IllegalArgumentException("Not a block breaker hardness field: " + field);
      };
   }

   public static int withHardnessField(int bits, int field, int value) {
      return switch (field) {
         case HARDNESS_LOW_FIELD -> (bits & ~LOWER_COMPONENT_MASK) | (value & LOWER_COMPONENT_MASK);
         case HARDNESS_MIDDLE_FIELD -> (bits & ~(LOWER_COMPONENT_MASK << COMPONENT_SHIFT))
            | ((value & LOWER_COMPONENT_MASK) << COMPONENT_SHIFT);
         case HARDNESS_HIGH_FIELD -> (bits & ~(UPPER_COMPONENT_MASK << (COMPONENT_SHIFT * 2)))
            | ((value & UPPER_COMPONENT_MASK) << (COMPONENT_SHIFT * 2));
         default -> throw new IllegalArgumentException("Not a block breaker hardness field: " + field);
      };
   }

   public static float hardnessFromFields(int low, int middle, int high) {
      int bits = (low & LOWER_COMPONENT_MASK)
         | ((middle & LOWER_COMPONENT_MASK) << COMPONENT_SHIFT)
         | ((high & UPPER_COMPONENT_MASK) << (COMPONENT_SHIFT * 2));
      return Float.intBitsToFloat(bits);
   }
}
