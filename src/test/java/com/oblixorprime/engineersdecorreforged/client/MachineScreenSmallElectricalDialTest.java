package com.oblixorprime.engineersdecorreforged.client;

import net.minecraft.network.chat.Component;
import org.junit.Test;
import static org.junit.Assert.*;

public class MachineScreenSmallElectricalDialTest {
   @Test public void originalGripPointsAtAllFourDetentsAndClamps() {
      assertEquals(180, ElectricalFurnaceArtwork.dialAngleDegrees(0));
      assertEquals(270, ElectricalFurnaceArtwork.dialAngleDegrees(1));
      assertEquals(0, ElectricalFurnaceArtwork.dialAngleDegrees(2));
      assertEquals(90, ElectricalFurnaceArtwork.dialAngleDegrees(3));
      assertEquals(180, ElectricalFurnaceArtwork.dialAngleDegrees(-4));
      assertEquals(90, ElectricalFurnaceArtwork.dialAngleDegrees(12));
   }

   @Test public void everyPointOnDialUsesRelativeStepsAndUnsupportedButtonsStayInert() {
      assertTrue(ElectricalFurnaceArtwork.containsDial(184, 88));
      assertTrue(ElectricalFurnaceArtwork.containsDial(168, 74));
      assertFalse(ElectricalFurnaceArtwork.containsDial(167.9, 88));
      assertFalse(ElectricalFurnaceArtwork.containsDial(201, 88));
      assertFalse(ElectricalFurnaceArtwork.containsDial(184, 103));
      assertEquals(1, ElectricalFurnaceArtwork.clickStep(0));
      assertEquals(-1, ElectricalFurnaceArtwork.clickStep(1));
      assertEquals(0, ElectricalFurnaceArtwork.clickStep(2));
      assertEquals(0, ElectricalFurnaceArtwork.clickStep(-1));
   }

   @Test public void energyUsesEightIndependentCellsWithoutPrematureFullState() {
      for (int cell = 0; cell < 8; cell++) {
         assertEquals(0, ElectricalFurnaceArtwork.energyCellPixels(0, 32000, cell, 32));
         assertEquals(cell < 4 ? 32 : 0, ElectricalFurnaceArtwork.energyCellPixels(16000, 32000, cell, 32));
         assertEquals(32, ElectricalFurnaceArtwork.energyCellPixels(32000, 32000, cell, 32));
      }
      assertEquals(16, ElectricalFurnaceArtwork.energyCellPixels(18000, 32000, 4, 32));
      assertEquals(0, ElectricalFurnaceArtwork.energyCellPixels(18000, 32000, 5, 32));
      assertEquals(31, ElectricalFurnaceArtwork.energyCellPixels(31999, 32000, 7, 32));
      assertEquals(32, ElectricalFurnaceArtwork.energyCellPixels(Integer.MAX_VALUE, 32000, 7, 32));
      assertEquals(0, ElectricalFurnaceArtwork.energyCellPixels(16000, 0, 0, 32));
      assertEquals(0, ElectricalFurnaceArtwork.energyCellPixels(-1, 32000, 0, 32));
      assertEquals(0, ElectricalFurnaceArtwork.energyCellPixels(16000, 32000, 8, 32));
   }

   @Test public void blankSlotTooltipTextIsSuppressedWithoutAffectingRealLabels() {
      assertFalse(MachineScreen.hasVisibleTooltipText(Component.literal("")));
      assertFalse(MachineScreen.hasVisibleTooltipText(Component.literal("  ")));
      assertTrue(MachineScreen.hasVisibleTooltipText(Component.literal("Crafting grid")));
      assertFalse(MachineScreen.hasVisibleTooltipText(null));
   }
}
