package com.oblixorprime.engineersdecorreforged.utility;

/** Describes the precision of energy values synchronized through the machine menu. */
public final class MachineEnergySync {
   public static final int SCALED_FIELD_STEP_FE = 100;

   private MachineEnergySync() {
   }

   public static int displayGranularity(MachineKind kind) {
      return switch (kind) {
         case SMALL_SOLAR_PANEL, SMALL_WASTE_INCINERATOR, SMALL_MINERAL_SMELTER, SMALL_FREEZER -> SCALED_FIELD_STEP_FE;
         default -> 1;
      };
   }
}
