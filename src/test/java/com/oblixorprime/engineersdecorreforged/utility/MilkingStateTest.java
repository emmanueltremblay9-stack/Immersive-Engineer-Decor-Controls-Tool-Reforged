package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.*;
import java.util.UUID;
import org.junit.Test;

public class MilkingStateTest {
   private static final UUID ANIMAL = UUID.fromString("fedcba98-7654-3210-ffff-8000abcd1234");

   @Test
   public void everyLongWordAndSignedShortBoundaryRoundTrips() {
      for (long remaining : new long[]{0, 32767, 32768, 65535, 65536, 100001,
            0x1234_ffff_8000_ffffL, Long.MAX_VALUE}) {
         MilkingState state = new MilkingState(MilkingState.TFC, MilkingState.RECOVERING,
               0x8123ffff, ANIMAL, remaining, Integer.MAX_VALUE,
               MilkingState.PAUSED | MilkingState.STORAGE_BLOCKED | MilkingState.RETRY_WAIT);
         assertEquals(state, wire(state));
         for (int word = 0; word < MilkingState.FIELD_COUNT; word++) {
            assertTrue(state.field(word) >= 0 && state.field(word) <= 65535);
         }
         assertEquals(0, state.field(17));
      }
   }

   @Test
   public void identityIncludesBothUuidHalvesAndEntityId() {
      MilkingState state = new MilkingState(MilkingState.TFC, MilkingState.READY,
            Integer.MIN_VALUE, ANIMAL, 0, 100001, 0);
      assertEquals(ANIMAL, wire(state).animalId());
      assertEquals(Integer.MIN_VALUE, wire(state).entityId());
      assertEquals(state, wire(state));
   }

   @Test
   public void changedConfigurationDoesNotClampNativeRemaining() {
      MilkingState state = new MilkingState(MilkingState.TFC, MilkingState.RECOVERING,
            17, ANIMAL, 200003, 100001, 0);
      assertEquals(200003L, wire(state).remainingTicks());
      assertEquals(100001, wire(state).totalTicks());
   }

   @Test
   public void emptyProjectionClearsAllOldAnimalWords() {
      MilkingState stale = new MilkingState(MilkingState.TFC, MilkingState.RECOVERING,
            -1, ANIMAL, Long.MAX_VALUE, Integer.MAX_VALUE, 0);
      int[] packet = new int[MilkingState.FIELD_COUNT];
      for (int i = 0; i < packet.length; i++) packet[i] = (short)stale.field(i);
      for (int i = 0; i < packet.length; i++) packet[i] = (short)MilkingState.empty().field(i);
      assertEquals(MilkingState.empty(), MilkingState.decode(i -> packet[i]));
      for (int word : packet) assertEquals(0, word);
   }

   @Test
   public void vanillaNeverInventsAnimalCooldownAndFlagsStayIndependent() {
      MilkingState vanilla = new MilkingState(MilkingState.VANILLA, MilkingState.READY,
            9, ANIMAL, 120, 120, MilkingState.PAUSED | MilkingState.STORAGE_BLOCKED);
      assertEquals(0L, wire(vanilla).remainingTicks());
      assertEquals(0, wire(vanilla).totalTicks());
      assertEquals(MilkingState.PAUSED | MilkingState.STORAGE_BLOCKED, wire(vanilla).machineFlags());
      MilkingState recovering = new MilkingState(MilkingState.TFC, MilkingState.RECOVERING,
            9, ANIMAL, 90000, 100001, 0);
      assertEquals(recovering.remainingTicks(), recovering.withMachineFlags(MilkingState.PAUSED).remainingTicks());
      assertEquals(recovering.state(), recovering.withMachineFlags(MilkingState.STORAGE_BLOCKED).state());
   }

   @Test
   public void malformedObservationCannotRetainSourceOrNegativeTime() {
      MilkingState invalid = new MilkingState(3, MilkingState.READY, 12, ANIMAL, 30, 40, 0xff);
      assertEquals(MilkingState.NONE, wire(invalid).source());
      assertNull(wire(invalid).animalId());
      assertEquals(0, wire(invalid).entityId());
      assertEquals(0L, wire(invalid).remainingTicks());
      assertEquals(7, wire(invalid).machineFlags());
      MilkingState negative = new MilkingState(MilkingState.TFC, MilkingState.RECOVERING, 1, ANIMAL, -1, -1, 0);
      assertEquals(0L, wire(negative).remainingTicks());
      assertEquals(0, wire(negative).totalTicks());
   }

   private static MilkingState wire(MilkingState state) {
      return MilkingState.decode(index -> (short)state.field(index));
   }
}
