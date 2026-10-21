package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Bounded session recognition, not a queue or transaction executor. Unconnected to
 * gameplay until authoritative ownership, FIFO, revisions and atomic commit exist.
 * Gameplay integration must use the owning server thread. Synchronization makes
 * local recognition atomic; callbacks must not re-enter or acquire unrelated locks.
 */
public final class MusleeSessionIngress {
   public static final int MAX_RECOGNIZED_ACTIONS = 4096;
   public enum Phase { ACTIVE, INVALIDATING, CLOSED }
   public enum Status {
      RECOGNIZED, MALFORMED_ENVELOPE, UNAUTHORIZED, INVALID_ACTION_ID,
      DUPLICATE, MALFORMED_COMMAND, INVALIDATING, CLOSED
   }
   public record Result(Status status, Optional<MusleeCommandProtocol.Command> command) {
      private static Result rejected(Status status) { return new Result(status, Optional.empty()); }
   }

   private final UUID sessionId;
   private final Set<UUID> recognized = new HashSet<>();
   private Phase phase = Phase.ACTIVE;
   private boolean receiving;

   private MusleeSessionIngress(UUID sessionId) { this.sessionId = sessionId; }

   /** A new server-generated UUID; never accept a client's requested session ID. */
   public static MusleeSessionIngress open() { return new MusleeSessionIngress(UUID.randomUUID()); }
   public UUID sessionId() { return sessionId; }
   public synchronized Phase phase() { return phase; }
   public synchronized int recognizedCount() { return recognized.size(); }

   /**
    * Recognize before semantic parsing, but only after size/version/session and
    * live authoritative access checks. The callback must validate the current
    * sender, holder, exact press and GUI session, never a cached permission.
    * RECOGNIZED does NOT authorize queue admission, item changes or pressing.
    * Refusal statuses are internal control flow only: do not log, synchronize or notify.
    */
   public synchronized Result receive(byte[] packet, BooleanSupplier authoritativeAccess) {
      if (phase != Phase.ACTIVE) {
         return Result.rejected(phase == Phase.CLOSED ? Status.CLOSED : Status.INVALIDATING);
      }
      if (receiving) {
         return Result.rejected(Status.UNAUTHORIZED);
      }
      // Bound before copying, version parsing or action-ID recognition.
      if (packet == null || packet.length > MusleeCommandProtocol.MAX_PACKET_BYTES || packet.length < 18) {
         return Result.rejected(Status.MALFORMED_ENVELOPE);
      }
      byte[] bytes = packet.clone();
      if (bytes[0] != 0 || Byte.toUnsignedInt(bytes[1]) != MusleeCommandProtocol.VERSION
         || !sessionId.equals(MusleeCommandProtocol.uuidAt(bytes, 2))) {
         return Result.rejected(Status.MALFORMED_ENVELOPE);
      }
      receiving = true;
      try {
         boolean authorized;
         try {
            authorized = authoritativeAccess != null && authoritativeAccess.getAsBoolean();
         } catch (RuntimeException unavailableAuthority) {
            // Uncertain authority is not permission, and emits no diagnostic.
            return Result.rejected(Status.UNAUTHORIZED);
         }
         if (!authorized || phase != Phase.ACTIVE) {
            return Result.rejected(Status.UNAUTHORIZED);
         }
         if (bytes.length < 34) {
            return Result.rejected(Status.INVALID_ACTION_ID);
         }
         UUID actionId = MusleeCommandProtocol.uuidAt(bytes, 18);
         if (!MusleeCommandProtocol.isV4(actionId)) {
            return Result.rejected(Status.INVALID_ACTION_ID);
         }
         if (!recognized.add(actionId)) {
            return Result.rejected(Status.DUPLICATE);
         }
         if (recognized.size() == MAX_RECOGNIZED_ACTIONS) {
            // Reaching the bound starts invalidation; the threshold command must
            // not reach FIFO. Keep IDs until the owner confirms safe invalidation.
            phase = Phase.INVALIDATING;
            return Result.rejected(Status.INVALIDATING);
         }
         Optional<MusleeCommandProtocol.Command> command = MusleeCommandProtocol.decodeRecognized(bytes, actionId);
         return command.isPresent() ? new Result(Status.RECOGNIZED, command) : Result.rejected(Status.MALFORMED_COMMAND);
      } finally {
         receiving = false;
      }
   }

   /** Immediately stops recognition; the future owner must purge only non-engaged work. */
   public synchronized void invalidate() {
      if (phase == Phase.ACTIVE) {
         phase = Phase.INVALIDATING;
      }
   }

   /**
    * Call only after the owner has safely invalidated the GUI/session and purged
    * non-engaged work, without disrupting an already engaged transaction.
    * This class cannot perform or certify those external steps itself.
    * Closure is final; history release never permits reuse of this session UUID.
    */
   public synchronized boolean finishInvalidation() {
      if (phase != Phase.INVALIDATING || receiving) {
         return false;
      }
      recognized.clear();
      phase = Phase.CLOSED;
      return true;
   }
}
