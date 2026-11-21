package com.oblixorprime.engineersdecorreforged.block;

import org.junit.Test;

import static org.junit.Assert.*;

public class MicroConduitChannelsTest {
   @Test public void exactlyNineLanes() {
      assertEquals(9, MicroConduitChannels.COUNT);
      assertFalse(MicroConduitChannels.valid(-1));
      assertFalse(MicroConduitChannels.valid(9));
      for (int lane = 0; lane < 9; ++lane) assertTrue(MicroConduitChannels.valid(lane));
   }

   @Test public void eachLaneConnectsOnlyToItself() {
      for (int from = 0; from < 9; ++from) {
         for (int to = 0; to < 9; ++to) {
            assertEquals("LV line " + from + " to " + to, from == to, MicroConduitChannels.same(from, to));
         }
      }
      assertFalse(MicroConduitChannels.same(-1, 0));
      assertFalse(MicroConduitChannels.same(0, 9));
   }
}
