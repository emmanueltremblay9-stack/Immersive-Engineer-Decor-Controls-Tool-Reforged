package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeCommandProtocol.Type.*;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeSessionIngress.Phase.ACTIVE;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeSessionIngress.Status.*;
import static org.junit.Assert.*;

public class MusleeSessionIngressTest {
   private static final UUID SESSION = UUID.fromString("00112233-4455-4677-8899-aabbccddeeff");
   private static final UUID ACTION = UUID.fromString("10213243-5465-4768-899a-abbccddeef00");

   @Test public void encodingHasExactBigEndianLayout() {
      byte[] header = hex("000200112233445546778899aabbccddeeff1021324354654768899aabbccddeef00");
      for (MusleeCommandProtocol.Type type : MusleeCommandProtocol.Type.values()) {
         byte[] expected = Arrays.copyOf(header, 37);
         expected[34] = (byte) (type == ABSORB ? 1 : 3);
         assertArrayEquals(expected, MusleeCommandProtocol.encode(SESSION,
            new MusleeCommandProtocol.Command(ACTION, type)));
      }
   }

   @Test public void cursorOnlyCommandsRoundTrip() {
      MusleeSessionIngress ingress = MusleeSessionIngress.open();
      roundTrip(ingress, ABSORB, id(1));
      roundTrip(ingress, PRESS, id(2));
      assertEquals(2, ingress.recognizedCount());
      assertTrue(MusleeCommandProtocol.isV4(ingress.sessionId()));
   }

   @Test public void constructorsRejectInvalidCommandsAndSessions() {
      expectIllegalArgument(() -> new MusleeCommandProtocol.Command(ACTION, null));
      for (UUID invalid : new UUID[] {null, new UUID(0, 0), UUID.fromString("00112233-4455-1677-8899-aabbccddeeff"),
            UUID.fromString("00112233-4455-4677-0899-aabbccddeeff")}) {
         assertFalse(MusleeCommandProtocol.isV4(invalid));
         expectIllegalArgument(() -> new MusleeCommandProtocol.Command(invalid, PRESS));
         expectIllegalArgument(() -> MusleeCommandProtocol.encode(invalid,
            new MusleeCommandProtocol.Command(ACTION, PRESS)));
      }
      expectIllegalArgument(() -> MusleeCommandProtocol.encode(SESSION, null));
   }

   @Test public void allLegacyArgumentsAreRejectedAndCannotBeReplayedAsCursorCommands() {
      for (int type : new int[] {1, 2, 3}) {
         for (int argument = 0; argument <= 255; argument++) {
            MusleeSessionIngress ingress = MusleeSessionIngress.open();
            byte[] bytes = Arrays.copyOf(packet(ingress, ACTION, ABSORB), 38);
            bytes[34] = (byte) type;
            bytes[36] = 1;
            bytes[37] = (byte) argument;
            status(ingress.receive(bytes, () -> true), MALFORMED_COMMAND);
            assertEquals(1, ingress.recognizedCount());
            status(ingress.receive(packet(ingress, ACTION, ABSORB), () -> true), DUPLICATE);
         }
      }
   }

   @Test public void everyTruncationHasExpectedRecognitionBoundary() {
      for (int length = 0; length < 37; length++) {
         MusleeSessionIngress ingress = MusleeSessionIngress.open();
         AtomicInteger checks = new AtomicInteger();
         byte[] bytes = Arrays.copyOf(packet(ingress, ACTION, ABSORB), length);
         status(ingress.receive(bytes, () -> { checks.incrementAndGet(); return true; }),
            length < 18 ? MALFORMED_ENVELOPE : length < 34 ? INVALID_ACTION_ID : MALFORMED_COMMAND);
         assertEquals(length < 18 ? 0 : 1, checks.get());
         assertEquals(length < 34 ? 0 : 1, ingress.recognizedCount());
      }
   }

   @Test public void nullPacketAndBadVersionOrSessionSkipAuthorizationAndDedup() {
      for (int variation = 0; variation < 7; variation++) {
         MusleeSessionIngress ingress = MusleeSessionIngress.open();
         byte[] bytes = packet(ingress, ACTION, PRESS);
         switch (variation) {
            case 0: bytes = null; break;
            case 1: bytes[0] = 1; break;
            case 2: bytes[1] = 0; break;
            case 3: bytes[1] = 1; break;
            case 4: bytes[2] ^= 1; break;
            case 5: bytes[8] = (byte) ((bytes[8] & 15) | 16); break;
            case 6: bytes[10] &= 63; break;
            default: throw new AssertionError();
         }
         status(ingress.receive(bytes, () -> { fail("Envelope rejection must precede authorization"); return true; }), MALFORMED_ENVELOPE);
         assertEquals(0, ingress.recognizedCount());
         status(ingress.receive(packet(ingress, ACTION, PRESS), () -> true), RECOGNIZED);
      }
   }

   @Test public void invalidActionVersionAndVariantDoNotConsumeIds() {
      for (int offset : new int[] {24, 26}) {
         MusleeSessionIngress ingress = MusleeSessionIngress.open();
         byte[] bytes = packet(ingress, ACTION, PRESS);
         if (offset == 24) bytes[offset] = (byte) ((bytes[offset] & 15) | 16);
         else bytes[offset] &= 63;
         AtomicInteger checks = new AtomicInteger();
         status(ingress.receive(bytes, () -> { checks.incrementAndGet(); return true; }), INVALID_ACTION_ID);
         assertEquals(1, checks.get());
         assertEquals(0, ingress.recognizedCount());
         status(ingress.receive(packet(ingress, ACTION, PRESS), () -> true), RECOGNIZED);
      }
   }

   @Test public void authorizationPrecedesActionValidationAndDuplicateLookup() {
      MusleeSessionIngress ingress = MusleeSessionIngress.open();
      byte[] bytes = packet(ingress, ACTION, PRESS);
      status(ingress.receive(bytes, null), UNAUTHORIZED);
      status(ingress.receive(bytes, () -> false), UNAUTHORIZED);
      status(ingress.receive(Arrays.copyOf(bytes, 18), () -> false), UNAUTHORIZED);
      byte[] invalid = bytes.clone();
      invalid[24] = 0;
      status(ingress.receive(invalid, () -> false), UNAUTHORIZED);
      assertEquals(0, ingress.recognizedCount());
      status(ingress.receive(bytes, () -> true), RECOGNIZED);
      status(ingress.receive(bytes, () -> false), UNAUTHORIZED);
      status(ingress.receive(bytes, () -> true), DUPLICATE);
      assertEquals(1, ingress.recognizedCount());
   }

   @Test public void unknownCommandsAndMalformedLengthsConsumeDedup() {
      for (int variation = 0; variation < 9; variation++) {
         MusleeSessionIngress ingress = MusleeSessionIngress.open();
         byte[] bytes = packet(ingress, ACTION, PRESS);
         switch (variation) {
            case 0: bytes[34] = 0; break;
            case 1: bytes[34] = 4; break;
            case 2: bytes[34] = (byte) 255; break;
            case 3: bytes[35] = 1; break;
            case 4: bytes[36] = 1; break;
            case 5: bytes[35] = (byte) 255; bytes[36] = (byte) 255; break;
            case 6: bytes = Arrays.copyOf(bytes, 38); break;
            case 7: bytes[34] = 2; break;
            case 8: bytes = Arrays.copyOf(packet(ingress, ACTION, ABSORB), 39); break;
            default: throw new AssertionError();
         }
         status(ingress.receive(bytes, () -> true), MALFORMED_COMMAND);
         assertEquals(1, ingress.recognizedCount());
         status(ingress.receive(bytes, () -> true), DUPLICATE);
         status(ingress.receive(packet(ingress, ACTION, PRESS), () -> true), DUPLICATE);
      }
   }

   @Test public void packetSizeBoundAppliesBeforeAnyRecognition() {
      MusleeSessionIngress ingress = MusleeSessionIngress.open();
      byte[] valid = packet(ingress, ACTION, PRESS);
      status(ingress.receive(Arrays.copyOf(valid, 8193), () -> { fail("Oversize must skip callback"); return true; }), MALFORMED_ENVELOPE);
      assertEquals(0, ingress.recognizedCount());
      status(ingress.receive(Arrays.copyOf(valid, 8192), () -> true), MALFORMED_COMMAND);
      assertEquals(1, ingress.recognizedCount());
      status(ingress.receive(valid, () -> true), DUPLICATE);
   }

   @Test public void callbackCannotMutatePacketBeingRecognized() {
      MusleeSessionIngress ingress = MusleeSessionIngress.open();
      byte[] bytes = packet(ingress, ACTION, ABSORB);
      MusleeSessionIngress.Result result = ingress.receive(bytes, () -> {
         Arrays.fill(bytes, (byte) 0);
         return true;
      });
      status(result, RECOGNIZED);
      assertEquals(new MusleeCommandProtocol.Command(ACTION, ABSORB), result.command().get());
      status(ingress.receive(packet(ingress, ACTION, PRESS), () -> true), DUPLICATE);
   }

   @Test public void reentrantReceiveCannotRecognizeOrInvokeNestedCallback() {
      MusleeSessionIngress ingress = MusleeSessionIngress.open();
      byte[] nested = packet(ingress, id(1), PRESS);
      status(ingress.receive(packet(ingress, ACTION, PRESS), () -> {
         status(ingress.receive(nested, () -> { fail("Nested callback must not run"); return true; }), UNAUTHORIZED);
         assertEquals(0, ingress.recognizedCount());
         return true;
      }), RECOGNIZED);
      status(ingress.receive(nested, () -> true), RECOGNIZED);
      assertEquals(2, ingress.recognizedCount());
   }

   @Test public void callbackExceptionDoesNotConsumeIdOrLeaveReentrancyGuardSet() {
      MusleeSessionIngress ingress = MusleeSessionIngress.open();
      byte[] bytes = packet(ingress, ACTION, PRESS);
      RuntimeException sentinel = new RuntimeException("authorization failure");
      status(ingress.receive(bytes, () -> { throw sentinel; }), UNAUTHORIZED);
      assertEquals(0, ingress.recognizedCount());
      status(ingress.receive(bytes, () -> true), RECOGNIZED);
   }

   @Test public void invalidationDuringCallbackCannotRecognizeOrFinishPrematurely() {
      MusleeSessionIngress ingress = MusleeSessionIngress.open();
      status(ingress.receive(packet(ingress, ACTION, PRESS), () -> {
         ingress.invalidate();
         assertFalse(ingress.finishInvalidation());
         return true;
      }), UNAUTHORIZED);
      assertEquals(MusleeSessionIngress.Phase.INVALIDATING, ingress.phase());
      assertEquals(0, ingress.recognizedCount());
      assertTrue(ingress.finishInvalidation());
      assertEquals(MusleeSessionIngress.Phase.CLOSED, ingress.phase());
   }

   @Test public void explicitInvalidationRetainsHistoryUntilFinalClosure() {
      MusleeSessionIngress ingress = MusleeSessionIngress.open();
      UUID originalSession = ingress.sessionId();
      byte[] bytes = packet(ingress, ACTION, PRESS);
      assertFalse(ingress.finishInvalidation());
      status(ingress.receive(bytes, () -> true), RECOGNIZED);
      ingress.invalidate();
      ingress.invalidate();
      assertEquals(MusleeSessionIngress.Phase.INVALIDATING, ingress.phase());
      assertEquals(1, ingress.recognizedCount());
      status(ingress.receive(bytes, () -> { fail("Invalidating callback must not run"); return true; }),
         MusleeSessionIngress.Status.INVALIDATING);
      assertTrue(ingress.finishInvalidation());
      assertEquals(0, ingress.recognizedCount());
      assertEquals(originalSession, ingress.sessionId());
      assertFalse(ingress.finishInvalidation());
      ingress.invalidate();
      assertEquals(MusleeSessionIngress.Phase.CLOSED, ingress.phase());
      status(ingress.receive(bytes, () -> { fail("Closed callback must not run"); return true; }), MusleeSessionIngress.Status.CLOSED);
      status(ingress.receive(packet(ingress, id(2), PRESS), () -> true), MusleeSessionIngress.Status.CLOSED);
   }

   @Test public void recognition4096InvalidatesWithoutExecutingOrEvicting() {
      MusleeSessionIngress ingress = MusleeSessionIngress.open();
      for (int index = 0; index < 4095; index++) {
         byte[] bytes = packet(ingress, id(index), PRESS);
         // Malformed commands count toward the same bound as valid commands.
         if (index % 2 == 0) bytes[34] = 0;
         status(ingress.receive(bytes, () -> true), index % 2 == 0 ? MALFORMED_COMMAND : RECOGNIZED);
      }
      assertEquals(ACTIVE, ingress.phase());
      assertEquals(4095, ingress.recognizedCount());
      for (int index : new int[] {0, 1, 2048, 4094}) {
         status(ingress.receive(packet(ingress, id(index), PRESS), () -> true), DUPLICATE);
      }
      assertEquals(4095, ingress.recognizedCount());
      status(ingress.receive(packet(ingress, id(4095), PRESS), () -> true), MusleeSessionIngress.Status.INVALIDATING);
      assertEquals(MusleeSessionIngress.Phase.INVALIDATING, ingress.phase());
      assertEquals(4096, ingress.recognizedCount());
      status(ingress.receive(packet(ingress, id(4096), PRESS), () -> true), MusleeSessionIngress.Status.INVALIDATING);
      assertEquals(4096, ingress.recognizedCount());
      assertTrue(ingress.finishInvalidation());
      assertEquals(0, ingress.recognizedCount());
      status(ingress.receive(packet(ingress, id(0), PRESS), () -> true), MusleeSessionIngress.Status.CLOSED);
   }

   @Test public void malformed4096thActionAlsoInvalidates() {
      MusleeSessionIngress ingress = MusleeSessionIngress.open();
      for (int index = 0; index < 4096; index++) {
         byte[] bytes = packet(ingress, id(index), PRESS);
         bytes[34] = (byte) 255;
         status(ingress.receive(bytes, () -> true), index == 4095
            ? MusleeSessionIngress.Status.INVALIDATING : MALFORMED_COMMAND);
      }
      assertEquals(MusleeSessionIngress.Phase.INVALIDATING, ingress.phase());
      assertEquals(4096, ingress.recognizedCount());
      assertTrue(ingress.finishInvalidation());
      assertEquals(MusleeSessionIngress.Phase.CLOSED, ingress.phase());
   }

   private static void roundTrip(MusleeSessionIngress ingress, MusleeCommandProtocol.Type type, UUID action) {
      MusleeCommandProtocol.Command expected = new MusleeCommandProtocol.Command(action, type);
      MusleeSessionIngress.Result result = ingress.receive(MusleeCommandProtocol.encode(ingress.sessionId(), expected), () -> true);
      status(result, RECOGNIZED);
      assertEquals(expected, result.command().get());
   }

   private static byte[] packet(MusleeSessionIngress ingress, UUID action, MusleeCommandProtocol.Type type) {
      return MusleeCommandProtocol.encode(ingress.sessionId(), new MusleeCommandProtocol.Command(action, type));
   }

   private static UUID id(int value) {
      return new UUID(0x123456789abc4000L, 0x8000000000000000L | Integer.toUnsignedLong(value));
   }

   private static void status(MusleeSessionIngress.Result result, MusleeSessionIngress.Status expected) {
      assertEquals(expected, result.status());
      assertEquals(expected == RECOGNIZED, result.command().isPresent());
   }

   private static void expectIllegalArgument(Runnable action) {
      try {
         action.run();
         fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
         // Expected contract rejection.
      }
   }

   private static byte[] hex(String text) {
      byte[] result = new byte[text.length() / 2];
      for (int i = 0; i < result.length; i++) result[i] = (byte) Integer.parseInt(text.substring(i * 2, i * 2 + 2), 16);
      return result;
   }
}
