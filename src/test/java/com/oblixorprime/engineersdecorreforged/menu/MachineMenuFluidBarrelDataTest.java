package com.oblixorprime.engineersdecorreforged.menu;

import static org.junit.Assert.assertEquals;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluids;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import org.junit.Test;

public class MachineMenuFluidBarrelDataTest {
   static {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
   }
   @Test
   public void fluidBarrelDecodesTheServerSyncedFluidRegistryId() {
      int waterId = BuiltInRegistries.FLUID.getId(Fluids.WATER);

      assertEquals(Fluids.WATER, MachineMenu.decodeStoredFluid(
         MachineKind.FLUID_BARREL, 1000, waterId & 0x7FFF, waterId >>> 15 & 0x7FFF, waterId >>> 30
      ));
   }

   @Test
   public void emptyAndOtherMachinesDoNotExposeAStoredBarrelFluid() {
      int lavaId = BuiltInRegistries.FLUID.getId(Fluids.LAVA);

      assertEquals(Fluids.EMPTY, MachineMenu.decodeStoredFluid(
         MachineKind.FLUID_BARREL, 0, lavaId & 0x7FFF, lavaId >>> 15 & 0x7FFF, lavaId >>> 30
      ));
      assertEquals(Fluids.EMPTY, MachineMenu.decodeStoredFluid(
         MachineKind.SMALL_FLUID_FUNNEL, 1000, lavaId & 0x7FFF, lavaId >>> 15 & 0x7FFF, lavaId >>> 30
      ));
   }
}
