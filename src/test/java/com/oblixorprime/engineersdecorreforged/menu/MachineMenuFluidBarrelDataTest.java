package com.oblixorprime.engineersdecorreforged.menu;

import static org.junit.Assert.assertEquals;
import java.util.function.IntFunction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import org.junit.Test;

public class MachineMenuFluidBarrelDataTest {
   @Test
   public void fluidBarrelDecodesTheServerSyncedFluidRegistryId() {
      int waterId = 0x40008007;
      Fluid water = Fluids.WATER;

      assertEquals(Fluids.WATER, MachineMenu.decodeStoredFluid(
         MachineKind.FLUID_BARREL, 1000, waterId & 0x7FFF, waterId >>> 15 & 0x7FFF, waterId >>> 30,
         waterId + 1, id -> id == waterId ? water : null
      ));
   }

   @Test
   public void emptyAndOtherMachinesDoNotExposeAStoredBarrelFluid() {
      int lavaId = 10;
      Fluid lava = Fluids.LAVA;
      int registrySize = lavaId + 1;
      IntFunction<Fluid> fluidById = id -> id == lavaId ? lava : null;

      assertEquals(Fluids.EMPTY, MachineMenu.decodeStoredFluid(
         MachineKind.FLUID_BARREL, 0, lavaId & 0x7FFF, lavaId >>> 15 & 0x7FFF, lavaId >>> 30, registrySize, fluidById
      ));
      assertEquals(Fluids.EMPTY, MachineMenu.decodeStoredFluid(
         MachineKind.SMALL_FLUID_FUNNEL, 1000, lavaId & 0x7FFF, lavaId >>> 15 & 0x7FFF, lavaId >>> 30, registrySize, fluidById
      ));
   }
}
