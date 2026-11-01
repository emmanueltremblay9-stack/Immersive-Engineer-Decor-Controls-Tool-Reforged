package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MachineEnergySyncTest {
   @Test
   public void everyEnergyFieldScaledByOneHundredReportsThatPrecision() {
      assertEquals(100, MachineEnergySync.displayGranularity(MachineKind.SMALL_SOLAR_PANEL));
      assertEquals(100, MachineEnergySync.displayGranularity(MachineKind.SMALL_WASTE_INCINERATOR));
      assertEquals(100, MachineEnergySync.displayGranularity(MachineKind.SMALL_MINERAL_SMELTER));
      assertEquals(100, MachineEnergySync.displayGranularity(MachineKind.SMALL_FREEZER));
   }

   @Test
   public void rawEnergyFieldsAndMachinesWithoutEnergyReportOneFePrecision() {
      assertEquals(1, MachineEnergySync.displayGranularity(MachineKind.SMALL_LAB_FURNACE));
      assertEquals(1, MachineEnergySync.displayGranularity(MachineKind.SMALL_ELECTRICAL_FURNACE));
      assertEquals(1, MachineEnergySync.displayGranularity(MachineKind.SMALL_BLOCK_BREAKER));
      assertEquals(1, MachineEnergySync.displayGranularity(MachineKind.SMALL_TREE_CUTTER));
   }
}
