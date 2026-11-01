package com.oblixorprime.engineersdecorreforged.menu;

import static org.junit.Assert.assertSame;
import java.util.function.IntFunction;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import org.junit.Test;

public class MachineMenuFluidBarrelDataTest {
   @Test
   public void fluidBarrelDecodesTheServerSyncedFluidRegistryId() {
      int waterId = 0x40008007;
      Object water = new Object();
      Object empty = new Object();

      assertSame(water, MachineMenu.decodeStoredFluid(
         MachineKind.FLUID_BARREL, 1000, waterId & 0x7FFF, waterId >>> 15 & 0x7FFF, waterId >>> 30,
         waterId + 1, id -> id == waterId ? water : null, empty
      ));
   }

   @Test
   public void emptyAndOtherMachinesDoNotExposeAStoredBarrelFluid() {
      int lavaId = 10;
      Object lava = new Object();
      Object empty = new Object();
      int registrySize = lavaId + 1;
      IntFunction<Object> valueById = id -> id == lavaId ? lava : null;

      assertSame(empty, MachineMenu.decodeStoredFluid(
         MachineKind.FLUID_BARREL, 0, lavaId & 0x7FFF, lavaId >>> 15 & 0x7FFF, lavaId >>> 30, registrySize, valueById, empty
      ));
      assertSame(empty, MachineMenu.decodeStoredFluid(
         MachineKind.SMALL_FLUID_FUNNEL, 1000, lavaId & 0x7FFF, lavaId >>> 15 & 0x7FFF, lavaId >>> 30, registrySize, valueById, empty
      ));
   }
}
