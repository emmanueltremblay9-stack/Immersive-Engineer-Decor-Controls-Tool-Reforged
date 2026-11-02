package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.assertEquals;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import org.junit.Test;

public class MachineScreenSmallFreezerGaugeTest {
   @Test
   public void progressStartsEmptyAndClampsAtTheFourArtworkSegments() {
      assertEquals(0, MachineScreen.smallFreezerProgressPixels(0, 100, 4, 19));
      assertEquals(19, MachineScreen.smallFreezerProgressPixels(25, 100, 4, 19));
      assertEquals(76, MachineScreen.smallFreezerProgressPixels(100, 100, 4, 19));
      assertEquals(76, MachineScreen.smallFreezerProgressPixels(125, 100, 4, 19));
      assertEquals(0, MachineScreen.smallFreezerProgressPixels(-1, 100, 4, 19));
      assertEquals(0, MachineScreen.smallFreezerProgressPixels(10, 0, 4, 19));
   }

   @Test
   public void energyStartsEmptyAndClampsToTheArtworkTrack() {
      assertEquals(0, MachineScreen.smallFreezerEnergyPixels(0, 16000, 83));
      assertEquals(41, MachineScreen.smallFreezerEnergyPixels(8000, 16000, 83));
      assertEquals(83, MachineScreen.smallFreezerEnergyPixels(16000, 16000, 83));
      assertEquals(83, MachineScreen.smallFreezerEnergyPixels(20000, 16000, 83));
      assertEquals(0, MachineScreen.smallFreezerEnergyPixels(-1, 16000, 83));
      assertEquals(0, MachineScreen.smallFreezerEnergyPixels(500, 0, 83));
   }

   @Test
   public void solarEnergyTrackProjectsEmptyHalfAndFullStatesWithoutEscapingItsFrame() {
      assertEquals(0, MachineScreen.artworkEnergyPixels(0, 1000, 40));
      assertEquals(20, MachineScreen.artworkEnergyPixels(500, 1000, 40));
      assertEquals(40, MachineScreen.artworkEnergyPixels(1000, 1000, 40));
      assertEquals(40, MachineScreen.artworkEnergyPixels(1200, 1000, 40));
      assertEquals(0, MachineScreen.artworkEnergyPixels(-1, 1000, 40));
      assertEquals(0, MachineScreen.artworkEnergyPixels(100, 0, 40));
   }

   @Test
   public void freezerStatusAndRedstoneIndicatorProjectLivePausedBlockedAndRunningStates() {
      assertEquals("idle", MachineScreen.smallFreezerStatusKey(MachineMenu.FREEZER_STATUS_IDLE));
      assertEquals("ready", MachineScreen.smallFreezerStatusKey(MachineMenu.FREEZER_STATUS_READY));
      assertEquals("processing", MachineScreen.smallFreezerStatusKey(MachineMenu.FREEZER_STATUS_PROCESSING));
      assertEquals("redstone_paused", MachineScreen.smallFreezerStatusKey(MachineMenu.FREEZER_STATUS_REDSTONE_PAUSED));
      assertEquals("waiting_for_energy", MachineScreen.smallFreezerStatusKey(MachineMenu.FREEZER_STATUS_WAITING_FOR_ENERGY));
      assertEquals("output_blocked", MachineScreen.smallFreezerStatusKey(MachineMenu.FREEZER_STATUS_OUTPUT_BLOCKED));
      assertEquals("invalid_input", MachineScreen.smallFreezerStatusKey(MachineMenu.FREEZER_STATUS_INVALID_INPUT));
      assertEquals(0xFF75B77A, MachineScreen.smallFreezerRedstoneColor(false));
      assertEquals(0xFFFF5656, MachineScreen.smallFreezerRedstoneColor(true));
   }
}
