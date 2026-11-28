package com.oblixorprime.engineersdecorreforged.utility;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.oblixorprime.engineersdecorreforged.utility.FactoryStudyGeometry.Control.*;

/** Expected values come from the existing server protocol, independent of pointer geometry. */
public final class FactoryStudyStateTest {
   private static final MachineKind HOPPER = MachineKind.FACTORY_HOPPER;
   private static final MachineKind DROPPER = MachineKind.FACTORY_DROPPER;
   private static final MachineKind LAB = MachineKind.SMALL_LAB_FURNACE;

   @Test public void hopperUsesExistingActionIdsAndServerRanges() {
      action(1, 0, 0, FactoryStudyState.pointAction(HOPPER, RANGE, -0.5, 0, 0));
      action(1, 4, 0, FactoryStudyState.pointAction(HOPPER, RANGE, 1.5, 0, 0));
      action(1, 2, 0, FactoryStudyState.pointAction(HOPPER, RANGE, 0.5, 0, 0));
      action(2, 1, 0, FactoryStudyState.pointAction(HOPPER, COUNT, 0, 0, 0));
      action(2, 32, 0, FactoryStudyState.pointAction(HOPPER, COUNT, 1, 0, 0));
      action(4, 0, 0, FactoryStudyState.pointAction(HOPPER, PERIOD, 0, 0, 0));
      action(4, 100, 0, FactoryStudyState.pointAction(HOPPER, PERIOD, 1, 0, 0));
      action(5, 1, 0, FactoryStudyState.pointAction(HOPPER, MANUAL, 0, 0, 0));
      assertNull(FactoryStudyState.pointAction(HOPPER, DIRECTION, 0.5, 0.5, 0));
      assertNull(FactoryStudyState.pointAction(HOPPER, FILTER_GATE, 0.5, 0.5, 0));
   }

   @Test public void dropperUsesUpwardSpeedAndDirectionAndManualSignalNotForceDrop() {
      action(10, 100, 0, FactoryStudyState.pointAction(DROPPER, VELOCITY, 0, 0, 0));
      action(10, 0, 0, FactoryStudyState.pointAction(DROPPER, VELOCITY, 0, 1, 0));
      action(11, -100, 100, FactoryStudyState.pointAction(DROPPER, DIRECTION, 0, 0, 0));
      action(11, 100, -100, FactoryStudyState.pointAction(DROPPER, DIRECTION, 1, 1, 0));
      action(11, 0, 0, FactoryStudyState.pointAction(DROPPER, DIRECTION, 0.5, 0.5, 0));
      action(11, 0, 0, FactoryStudyState.pointAction(DROPPER, DIRECTION, 0.54, 0.46, 0));
      action(11, 10, -10, FactoryStudyState.pointAction(DROPPER, DIRECTION, 0.55, 0.55, 0));
      action(12, 1, 0, FactoryStudyState.pointAction(DROPPER, COUNT, -1, 0, 0));
      action(12, 32, 0, FactoryStudyState.pointAction(DROPPER, COUNT, 2, 0, 0));
      action(13, 100, 0, FactoryStudyState.pointAction(DROPPER, PERIOD, 1, 0, 0));
      action(15, 1, 0, FactoryStudyState.pointAction(DROPPER, MANUAL, 0, 0, 0));
      assertNull(FactoryStudyState.pointAction(DROPPER, RANGE, 0, 0, 0));
   }

   @Test public void hopperModeChangesPreserveTheOtherBitAndCycleThreeDistinctModes() {
      int logic = 2; // Continuous enabled; redstone normal.
      logic = FactoryStudyState.pointAction(HOPPER, REDSTONE, 0, 0, logic).valueA();
      assertEquals(3, logic); // Inverted + continuous.
      logic = FactoryStudyState.pointAction(HOPPER, REDSTONE, 0, 0, logic).valueA();
      assertEquals(6, logic); // Ignore external + continuous.
      logic = FactoryStudyState.pointAction(HOPPER, REDSTONE, 0, 0, logic).valueA();
      assertEquals(2, logic); // Normal + continuous again.
      action(3, 4, 0, FactoryStudyState.pointAction(HOPPER, TRIGGER, 0, 0, 6));
      action(3, 6, 0, FactoryStudyState.pointAction(HOPPER, TRIGGER, 0, 0, 4));
      for (int raw = 0; raw < 256; raw++) {
         int next = FactoryStudyState.pointAction(HOPPER, REDSTONE, 0, 0, raw).valueA();
         assertEquals(0, next & ~7);
         assertEquals(raw & 2, next & 2);
      }
   }

   @Test public void dropperModesPreserveFilterAndTriggerAcrossExternalGateChanges() {
      int logic = 17; // All filters + continuous; external OR.
      logic = FactoryStudyState.pointAction(DROPPER, REDSTONE, 0, 0, logic).valueA();
      assertEquals(49, logic); // Ignore external.
      logic = FactoryStudyState.pointAction(DROPPER, REDSTONE, 0, 0, logic).valueA();
      assertEquals(19, logic); // External AND.
      logic = FactoryStudyState.pointAction(DROPPER, REDSTONE, 0, 0, logic).valueA();
      assertEquals(17, logic); // External OR again.
      action(14, 18, 0, FactoryStudyState.pointAction(DROPPER, FILTER_GATE, 0, 0, 19));
      action(14, 3, 0, FactoryStudyState.pointAction(DROPPER, TRIGGER, 0, 0, 19));
      for (int raw = 0; raw < 256; raw++) {
         int next = FactoryStudyState.pointAction(DROPPER, REDSTONE, 0, 0, raw).valueA();
         assertEquals(0, next & ~51);
         assertEquals(raw & 17, next & 17);
      }
   }

   @Test public void keyboardIncrementsSaturateAndAllowFineDirectionAdjustment() {
      action(1, 4, 0, FactoryStudyState.incrementAction(HOPPER, RANGE, 1, 0, 4, 0, 0));
      action(2, 1, 0, FactoryStudyState.incrementAction(HOPPER, COUNT, -1, 0, 1, 0, 0));
      action(4, 55, 0, FactoryStudyState.incrementAction(HOPPER, PERIOD, 5, 0, 50, 0, 0));
      action(10, 60, 0, FactoryStudyState.incrementAction(DROPPER, VELOCITY, 0, 10, 50, 0, 0));
      action(11, 1, -1, FactoryStudyState.incrementAction(DROPPER, DIRECTION, 1, -1, 0, 0, 0));
      action(11, 100, -100, FactoryStudyState.incrementAction(DROPPER, DIRECTION, 10, -10, 98, -98, 0));
   }

   @Test public void laboratoryHasNoGuiActionsAndOtherMachinesAreNotAdopted() {
      for (var control : FactoryStudyGeometry.Control.values()) {
         assertNull(FactoryStudyState.pointAction(LAB, control, 0.5, 0.5, 0));
         assertNull(FactoryStudyState.incrementAction(LAB, control, 1, 1, 0, 0, 0));
         assertNull(FactoryStudyState.pointAction(MachineKind.SMALL_ELECTRICAL_FURNACE, control, 0.5, 0.5, 0));
      }
   }

   @Test public void confirmationReadsTheCorrectFieldsAndBothDirectionAxes() {
      int[] fields = new int[16];
      var hopperCount = new FactoryStudyState.Action(2, 12, 0);
      fields[4] = 12;
      assertFalse(FactoryStudyState.actionMatches(HOPPER, hopperCount, i -> fields[i]));
      fields[1] = 12;
      assertTrue(FactoryStudyState.actionMatches(HOPPER, hopperCount, i -> fields[i]));
      var direction = new FactoryStudyState.Action(11, 20, -35);
      fields[1] = 20;
      assertFalse(FactoryStudyState.actionMatches(DROPPER, direction, i -> fields[i]));
      fields[2] = -35;
      assertTrue(FactoryStudyState.actionMatches(DROPPER, direction, i -> fields[i]));
      assertFalse(FactoryStudyState.actionMatches(LAB, direction, i -> fields[i]));
      assertFalse(FactoryStudyState.actionMatches(DROPPER, null, i -> fields[i]));
      assertFalse(FactoryStudyState.actionMatches(DROPPER, new FactoryStudyState.Action(16, 1, 0), i -> fields[i]));
   }

   @Test public void manualConfirmationIsOnlyTheSignalField() {
      int[] fields = new int[16];
      var manual = new FactoryStudyState.Action(15, 1, 0);
      fields[10] = 1; // Open shutter is not confirmation of an external/manual signal.
      assertFalse(FactoryStudyState.actionMatches(DROPPER, manual, i -> fields[i]));
      fields[11] = 1;
      assertTrue(FactoryStudyState.actionMatches(DROPPER, manual, i -> fields[i]));
      assertFalse(FactoryStudyState.actionMatches(HOPPER, new FactoryStudyState.Action(5, 1, 0), i -> fields[i]));
      fields[5] = 1;
      assertTrue(FactoryStudyState.actionMatches(HOPPER, new FactoryStudyState.Action(5, 1, 0), i -> fields[i]));
   }

   @Test public void periodAndGaugeProjectionStayBoundedForEmptyOrInvalidData() {
      assertEquals(10, FactoryStudyState.periodTicks(-1));
      assertEquals(10, FactoryStudyState.periodTicks(0));
      assertEquals(110, FactoryStudyState.periodTicks(50));
      assertEquals(210, FactoryStudyState.periodTicks(100));
      assertEquals(210, FactoryStudyState.periodTicks(101));
      assertEquals(0, FactoryStudyState.fractionPixels(1, 0, 72));
      assertEquals(0, FactoryStudyState.fractionPixels(1, -1, 72));
      assertEquals(0, FactoryStudyState.fractionPixels(-1, 100, 72));
      assertEquals(0, FactoryStudyState.fractionPixels(1, 100, 0));
      assertEquals(36, FactoryStudyState.fractionPixels(16000, 32000, 72));
      assertEquals(72, FactoryStudyState.fractionPixels(40000, 32000, 72));
      assertEquals(Integer.MAX_VALUE,
         FactoryStudyState.fractionPixels(Integer.MAX_VALUE, 1, Integer.MAX_VALUE));
   }

   private static void action(int id, int valueA, int valueB, FactoryStudyState.Action actual) {
      assertNotNull(actual);
      assertEquals(id, actual.action());
      assertEquals(valueA, actual.valueA());
      assertEquals(valueB, actual.valueB());
   }
}
