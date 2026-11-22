package com.oblixorprime.engineersdecorreforged.utility;

/** Pure automatic recipe resolution for the Small Mineral Smelter.
 *  Production stage identity must depend only on the supplied input,
 *  never on an old saved mode or the machine's own output lava.
 */
public final class MineralStageRules {
   public static final int NONE = -1;
   public static final int MINERAL_TO_MAGMA = 0;
   public static final int MAGMA_TO_LAVA = 1;
   public static final int LAVA_TO_OBSIDIAN = 2;

   private MineralStageRules() { }

   public static int select(boolean hasItem, boolean mineral, boolean magma,
                            boolean hasLava, int topLavaCredit, int lavaPerObsidian) {
      if (lavaPerObsidian <= 0) throw new IllegalArgumentException("lava quantum");
      if (hasItem) {
         if (magma) return MAGMA_TO_LAVA;
         return mineral ? MINERAL_TO_MAGMA : NONE;
      }
      return hasLava && topLavaCredit >= lavaPerObsidian ? LAVA_TO_OBSIDIAN : NONE;
   }
}
