package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.utility.MilkingState;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class MachineScreenMilkingRecoveryTest {
   private static final UUID ANIMAL = UUID.fromString("ffffffff-ffff-ffff-8000-123456789abc");

   private static MilkingState observation(int source, int state, int flags) {
      return new MilkingState(source, state, Integer.MAX_VALUE, ANIMAL,
         Long.MAX_VALUE, Integer.MAX_VALUE, flags);
   }

   @Test public void tooltipWidthIsBoundedToTheScaledViewport() {
      assertEquals(240, MachineScreen.milkingTooltipWidth(640));
      assertEquals(240, MachineScreen.milkingTooltipWidth(256));
      assertEquals(224, MachineScreen.milkingTooltipWidth(240));
      assertEquals(64, MachineScreen.milkingTooltipWidth(80));
   }

   @Test public void tooltipWidthStaysPositiveInTinyOrUninitializedViewports() {
      assertEquals(1, MachineScreen.milkingTooltipWidth(17));
      assertEquals(1, MachineScreen.milkingTooltipWidth(16));
      assertEquals(1, MachineScreen.milkingTooltipWidth(0));
      assertEquals(1, MachineScreen.milkingTooltipWidth(Integer.MIN_VALUE));
      assertEquals(240, MachineScreen.milkingTooltipWidth(Integer.MAX_VALUE));
   }

   @Test public void identityRequiresBothFullEntityIdAndUuid() {
      MilkingState state = observation(MilkingState.TFC, MilkingState.RECOVERING, 0);
      assertTrue(MachineScreen.milkingIdentityMatches(state, Integer.MAX_VALUE, ANIMAL));
      assertFalse(MachineScreen.milkingIdentityMatches(state, 65535, ANIMAL));
      assertFalse(MachineScreen.milkingIdentityMatches(state, Integer.MAX_VALUE,
         UUID.fromString("ffffffff-ffff-ffff-8000-123456789abd")));
      assertFalse(MachineScreen.milkingIdentityMatches(state, Integer.MAX_VALUE, null));
      assertFalse(MachineScreen.milkingIdentityMatches(MilkingState.empty(), 0, new UUID(0, 0)));
   }

   @Test public void statusUsesServerObservationNotAnExtractionTimer() {
      assertEquals("label.recovering", MachineScreen.milkingStatusKey(
         observation(MilkingState.TFC, MilkingState.RECOVERING, MilkingState.PAUSED)));
      assertEquals("label.ready", MachineScreen.milkingStatusKey(observation(MilkingState.TFC, MilkingState.READY, 0)));
      assertEquals("label.vanilla", MachineScreen.milkingStatusKey(observation(MilkingState.VANILLA, MilkingState.READY, 0)));
      assertEquals("label.ineligible", MachineScreen.milkingStatusKey(observation(MilkingState.TFC, MilkingState.INELIGIBLE, 0)));
      assertEquals("label.no_animal", MachineScreen.milkingStatusKey(MilkingState.empty()));
   }

   @Test public void machineWaitsAreNeverLabeledAnimalRecovery() {
      assertEquals("label.paused", MachineScreen.milkingStatusKey(observation(MilkingState.TFC, MilkingState.READY, MilkingState.PAUSED)));
      assertEquals("label.storage_blocked", MachineScreen.milkingStatusKey(observation(MilkingState.TFC, MilkingState.READY, MilkingState.STORAGE_BLOCKED)));
      assertEquals("label.retry_wait", MachineScreen.milkingStatusKey(observation(MilkingState.VANILLA, MilkingState.READY, MilkingState.RETRY_WAIT)));
   }

   @Test public void signedPacketWordsPreserveFullIdentityAndCalendarTicks() {
      MilkingState expected = observation(MilkingState.TFC, MilkingState.RECOVERING, 7);
      MilkingState received = MilkingState.decode(index -> (short) expected.field(index));
      assertEquals(expected, received);
      assertTrue(MachineScreen.milkingIdentityMatches(received, Integer.MAX_VALUE, ANIMAL));
      assertEquals("label.recovering", MachineScreen.milkingStatusKey(received));
   }
}
