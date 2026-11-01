package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SmallFluidFunnelFluidSyncTest {
   @Test
   public void syncCodesKeepUnknownNonemptyFluidsVisible() {
      assertEquals(0, SmallFluidFunnelFluidSync.encode("water", 0));
      assertEquals(1, SmallFluidFunnelFluidSync.encode("water", 1));
      assertEquals(2, SmallFluidFunnelFluidSync.encode("lava", 1000));
      assertEquals(3, SmallFluidFunnelFluidSync.encode("milk", 250));
      assertEquals(4, SmallFluidFunnelFluidSync.encode("creosote", 1000));
      assertEquals(4, SmallFluidFunnelFluidSync.encode("other_mod:fluid", 500));
      assertEquals(4, SmallFluidFunnelFluidSync.encode("", 500));
      assertEquals(4, SmallFluidFunnelFluidSync.encode(null, 1000));
      assertEquals(0, SmallFluidFunnelFluidSync.encode("creosote", 0));

      assertEquals(0, SmallFluidFunnelFluidSync.normalize(-1));
      assertEquals(1, SmallFluidFunnelFluidSync.normalize(1));
      assertEquals(2, SmallFluidFunnelFluidSync.normalize(2));
      assertEquals(3, SmallFluidFunnelFluidSync.normalize(3));
      assertEquals(4, SmallFluidFunnelFluidSync.normalize(4));
      assertEquals(0, SmallFluidFunnelFluidSync.normalize(5));
   }
}
