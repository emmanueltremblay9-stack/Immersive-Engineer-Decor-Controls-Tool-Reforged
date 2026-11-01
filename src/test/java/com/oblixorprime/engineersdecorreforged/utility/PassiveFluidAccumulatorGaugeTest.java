package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PassiveFluidAccumulatorGaugeTest {
   @Test
   public void fillTracksExactServerAmountWithinActualCapacity() {
      assertEquals(0, PassiveFluidAccumulatorGauge.filledHeight(0, 4000, 60));
      assertEquals(30, PassiveFluidAccumulatorGauge.filledHeight(2000, 4000, 60));
      assertEquals(60, PassiveFluidAccumulatorGauge.filledHeight(4000, 4000, 60));
   }

   @Test
   public void fillIsBoundedForInvalidOrOutOfRangeData() {
      assertEquals(0, PassiveFluidAccumulatorGauge.filledHeight(-20, 4000, 60));
      assertEquals(60, PassiveFluidAccumulatorGauge.filledHeight(5000, 4000, 60));
      assertEquals(0, PassiveFluidAccumulatorGauge.filledHeight(2000, 0, 60));
      assertEquals(0, PassiveFluidAccumulatorGauge.filledHeight(2000, 4000, 0));
   }
}
