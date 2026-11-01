package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.Test;

public class MachineScreenFluidBarrelMilkIdTest {
   @Test
   public void recognizesMinecraftMilkFluidIdFromNeoForgeRegistrationAndUsesCreamTint() {
      ResourceLocation milk = ResourceLocation.fromNamespaceAndPath("minecraft", "milk");
      assertTrue(MachineScreen.isMilkFluidId(milk));
      assertEquals(0xFFE6E1D5, MachineScreen.fluidBarrelMilkTint(milk));
   }

   @Test
   public void waterKeepsTheGenericNonMilkFluidPath() {
      ResourceLocation water = ResourceLocation.fromNamespaceAndPath("minecraft", "water");

      assertFalse(MachineScreen.isMilkFluidId(water));
      assertEquals(0xFF7FAFC2, MachineScreen.fluidBarrelMilkTint(water));
   }
}
