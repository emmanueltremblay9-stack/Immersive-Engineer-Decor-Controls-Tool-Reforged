package com.oblixorprime.engineersdecorreforged.client;

import net.minecraft.network.chat.Component;
import org.junit.Test;
import static org.junit.Assert.*;

public class MachineScreenSmallElectricalDialTest {
   @Test public void allFourDialSpeedsSelectDifferentDetentsAndClamp() {
      assertArrayEquals(new int[]{0,9},MachineScreen.electricalDialOffset(0));
      assertArrayEquals(new int[]{-9,0},MachineScreen.electricalDialOffset(1));
      assertArrayEquals(new int[]{0,-9},MachineScreen.electricalDialOffset(2));
      assertArrayEquals(new int[]{9,0},MachineScreen.electricalDialOffset(3));
      assertArrayEquals(MachineScreen.electricalDialOffset(0),MachineScreen.electricalDialOffset(-4));
      assertArrayEquals(MachineScreen.electricalDialOffset(3),MachineScreen.electricalDialOffset(12));
   }

   @Test public void blankSlotTooltipTextIsSuppressedWithoutAffectingRealLabels() {
      assertFalse(MachineScreen.hasVisibleTooltipText(Component.literal("")));
      assertFalse(MachineScreen.hasVisibleTooltipText(Component.literal("  ")));
      assertTrue(MachineScreen.hasVisibleTooltipText(Component.literal("Crafting grid")));
      assertFalse(MachineScreen.hasVisibleTooltipText(null));
   }
}
