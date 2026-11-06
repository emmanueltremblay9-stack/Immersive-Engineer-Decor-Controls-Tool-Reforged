package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.Test;

public class MachineScreenFluidBarrelMilkIdTest {
   @Test
   public void recognizesMinecraftMilkFluidIdFromNeoForgeRegistrationForItsLabel() {
      ResourceLocation milk = ResourceLocation.fromNamespaceAndPath("minecraft", "milk");
      assertTrue(MachineScreen.isMilkFluidId(milk));
   }

   @Test
   public void waterKeepsTheGenericNonMilkFluidPath() {
      ResourceLocation water = ResourceLocation.fromNamespaceAndPath("minecraft", "water");

      assertFalse(MachineScreen.isMilkFluidId(water));
      assertFalse(MachineScreen.isMilkFluidId(null));
      assertFalse(MachineScreen.isMilkFluidId(ResourceLocation.fromNamespaceAndPath("example", "milk")));
   }
}
