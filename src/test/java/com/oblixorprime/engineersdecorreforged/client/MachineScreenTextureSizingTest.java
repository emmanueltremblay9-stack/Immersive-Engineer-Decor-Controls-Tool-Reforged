package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import org.junit.Test;

public class MachineScreenTextureSizingTest {
   @Test
   public void croppedNonSquareTexturesUseTheirDeclaredTextureDimensions() {
      assertTrue(MachineScreen.usesExplicitTextureDimensions(MachineKind.SMALL_BLOCK_BREAKER));
      assertTrue(MachineScreen.usesExplicitTextureDimensions(MachineKind.FLUID_BARREL));
      assertFalse(MachineScreen.usesExplicitTextureDimensions(MachineKind.FACTORY_HOPPER));
   }
}
