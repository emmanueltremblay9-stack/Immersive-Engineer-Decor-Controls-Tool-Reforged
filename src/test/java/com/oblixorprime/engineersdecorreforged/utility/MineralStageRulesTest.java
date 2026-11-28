package com.oblixorprime.engineersdecorreforged.utility;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MineralStageRulesTest {
   @Test
   public void eachInputIdentifiesExactlyOneStageWithoutAnyMode() {
      assertEquals(0, MineralStageRules.select(true, true, false, false, 0, 1000));
      assertEquals(1, MineralStageRules.select(true, false, true, false, 0, 1000));
      assertEquals(2, MineralStageRules.select(false, false, false, true, 1000, 1000));
      assertEquals(-1, MineralStageRules.select(true, false, false, true, 3000, 1000));
      assertEquals(-1, MineralStageRules.select(false, false, false, true, 999, 1000));
      assertEquals(-1, MineralStageRules.select(false, false, false, false, 1000, 1000));
   }

   @Test
   public void identicalStackCannotAdvanceFromMagmaToObsidianInOneMachine() {
      assertEquals(1, MineralStageRules.select(true, true, true, true, 4000, 1000));
      assertEquals(-1, MineralStageRules.select(false, false, false, true, 0, 1000));
   }

   @Test
   public void lavaQuantumMustBeStrictlyPositive() {
      assertThrows(IllegalArgumentException.class,
         () -> MineralStageRules.select(false, false, false, true, 1000, 0));
   }
}
