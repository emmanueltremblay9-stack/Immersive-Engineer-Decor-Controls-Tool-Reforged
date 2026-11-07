package com.oblixorprime.engineersdecorreforged.utility;

import java.util.UUID;
import java.util.function.IntUnaryOperator;

/** Read-only server observation. These fields never authorize or time a transaction. */
public record MilkingState(int source, int state, int entityId, UUID animalId,
                           long remainingTicks, int totalTicks, int machineFlags) {
   public static final int NONE = 0, VANILLA = 1, TFC = 2;
   public static final int READY = 1, RECOVERING = 2, INELIGIBLE = 3;
   public static final int PAUSED = 1, STORAGE_BLOCKED = 2, RETRY_WAIT = 4;
   public static final int FIELD_COUNT = 18;

   public MilkingState {
      machineFlags &= PAUSED | STORAGE_BLOCKED | RETRY_WAIT;
      if ((source != VANILLA && source != TFC) || state < READY || state > INELIGIBLE || animalId == null) {
         source = NONE; state = NONE; entityId = 0; animalId = null;
      }
      remainingTicks = source == TFC ? Math.max(0L, remainingTicks) : 0L;
      totalTicks = source == TFC ? Math.max(0, totalTicks) : 0;
   }

   public static MilkingState empty() { return new MilkingState(NONE, NONE, 0, null, 0, 0, 0); }

   public MilkingState withMachineFlags(int flags) {
      return new MilkingState(source, state, entityId, animalId, remainingTicks, totalTicks, flags);
   }

   /** Every word is explicitly decoded as unsigned, including signed-short packet transport. */
   public int field(int index) {
      if (index == 0) return source | (state << 2) | (machineFlags << 4);
      if (index >= 1 && index <= 2) return word(entityId, index - 1);
      if (index >= 3 && index <= 6) return word(animalId == null ? 0 : animalId.getMostSignificantBits(), index - 3);
      if (index >= 7 && index <= 10) return word(animalId == null ? 0 : animalId.getLeastSignificantBits(), index - 7);
      if (index >= 11 && index <= 14) return word(remainingTicks, index - 11);
      if (index >= 15 && index <= 16) return word(totalTicks, index - 15);
      return 0; // reserved, initialized even when the previous target disappears
   }

   public static MilkingState decode(IntUnaryOperator fields) {
      int flags = fields.applyAsInt(0) & 0xFFFF;
      return new MilkingState(flags & 3, (flags >>> 2) & 3,
            (int) join(fields, 1, 2), new UUID(join(fields, 3, 4), join(fields, 7, 4)),
            join(fields, 11, 4), (int) join(fields, 15, 2), flags >>> 4);
   }

   private static int word(long value, int index) { return (int) (value >>> (16 * index)) & 0xFFFF; }
   private static long join(IntUnaryOperator fields, int start, int count) {
      long result = 0;
      for (int index = 0; index < count; index++) result |= (long) (fields.applyAsInt(start + index) & 0xFFFF) << (16 * index);
      return result;
   }
}
