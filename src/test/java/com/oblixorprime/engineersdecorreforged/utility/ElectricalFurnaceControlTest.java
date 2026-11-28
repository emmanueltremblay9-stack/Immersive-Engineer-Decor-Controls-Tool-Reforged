package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.*;
import org.junit.Test;

public class ElectricalFurnaceControlTest {
   @Test public void clockwiseAndCounterclockwiseRequestsWrapAcrossOff() {
      int speed = 0;
      for (int expected : new int[]{1, 2, 3, 0, 1, 2}) {
         speed = ElectricalFurnaceControl.cycleSpeed(speed, 1);
         assertEquals(expected, speed);
      }
      for (int expected : new int[]{1, 0, 3, 2}) {
         speed = ElectricalFurnaceControl.cycleSpeed(speed, -1);
         assertEquals(expected, speed);
      }
   }

   @Test public void relativeProtocolAcceptsOnlyOneSignedStepWithNoExtraPayload() {
      assertTrue(ElectricalFurnaceControl.accepts(32, 1, 0));
      assertTrue(ElectricalFurnaceControl.accepts(32, -1, 0));
      for (int direction : new int[]{Integer.MIN_VALUE, -2, 0, 2, Integer.MAX_VALUE}) {
         assertFalse(ElectricalFurnaceControl.accepts(32, direction, 0));
         assertEquals(2, ElectricalFurnaceControl.cycleSpeed(2, direction));
      }
      assertFalse(ElectricalFurnaceControl.accepts(32, 1, 1));
      assertFalse(ElectricalFurnaceControl.accepts(31, 1, 0));
      assertFalse(ElectricalFurnaceControl.accepts(33, 1, 0));
      assertTrue(ElectricalFurnaceControl.accepts(30, -99, 0));
      assertTrue(ElectricalFurnaceControl.accepts(30, 99, 0));
   }
}
