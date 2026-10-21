package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeCommandQueue.*;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeSessionIngress.Status.*;

/** Test-only composition; deliberately not a production ownership/GUI adapter. */
public final class MusleeIngressQueueIntegrationCases {
    private static final Validator RETAIN = t -> Verdict.ENGAGE_RETAIN_CAPTURE;
    private static final Validator REJECT = t -> Verdict.REJECT;
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static UUID action(int index) { return new UUID(0x4000L, Long.MIN_VALUE | index); }
    private static final class TestClock implements Clock {
        long now; boolean unavailable;
        public long nanoTime() { if (unavailable) throw new IllegalStateException("fixture clock unavailable"); return now; }
    }
    private record Attempt(MusleeSessionIngress.Result recognized, Admission admitted) {}
    private static final class Fixture {
        final MusleeSessionIngress ingress = MusleeSessionIngress.open();
        final TestClock clock = new TestClock();
        final MusleeCommandQueue queue = new MusleeCommandQueue(UUID.randomUUID(), clock);
        byte[] wire(int index) {
            return MusleeCommandProtocol.encode(ingress.sessionId(),
                    new MusleeCommandProtocol.Command(action(index), MusleeCommandProtocol.Type.INSERT, 0));
        }
        Attempt submit(byte[] packet, BooleanSupplier authority, long bytes) {
            synchronized (queue) { // Explicit stand-in for the future common authoritative lock.
                var result = ingress.receive(packet, authority);
                Admission admission = result.status() == RECOGNIZED
                        ? queue.admit(result.command().orElseThrow().actionId(), ingress.sessionId(),
                            ingress.phase() == MusleeSessionIngress.Phase.ACTIVE, bytes) : null;
                return new Attempt(result, admission);
            }
        }
        Ticket add(int index, long bytes) {
            Attempt a = submit(wire(index), () -> true, bytes);
            check(a.recognized.status() == RECOGNIZED && a.admitted.status == AdmissionStatus.ADMITTED, "fixture admission");
            return a.admitted.ticket;
        }
        void duplicate(int index) {
            Attempt a = submit(wire(index), () -> true, 1);
            check(a.recognized.status() == DUPLICATE && a.admitted == null, "recognized UUID cannot re-enter queue");
        }
    }
    public static void run() {
        fullQueueDoesNotReenableRecognizedId();
        budgetFailureDoesNotReenableRecognizedId();
        malformedInputCannotReserveMemory();
        expiryDoesNotEraseDeduplication();
        saturationClosurePreservesEngagedWork();
        boundedCleanupDoesNotReadmitRejectedAction();
        clockFailureDoesNotReplayOrCancelEngagedWork();
        authorityIsRevalidatedBeforeEngagement();
    }
    private static void fullQueueDoesNotReenableRecognizedId() {
        Fixture f = new Fixture(); List<Ticket> waiting = new ArrayList<>();
        for (int i = 1; i <= 32; i++) waiting.add(f.add(i, 1));
        Attempt full = f.submit(f.wire(33), () -> true, 1);
        check(full.recognized.status() == RECOGNIZED && full.admitted.status == AdmissionStatus.FULL, "recognition separate from admission");
        check(f.ingress.recognizedCount() == 33 && f.queue.waitingCount() == 32, "rejected action keeps only dedup history");
        check(f.queue.runScheduled(1, RETAIN).engaged == waiting.get(0), "first command engaged");
        f.duplicate(33);
        check(f.queue.waitingCount() == 31, "replay cannot use newly freed slot");
        f.add(34, 1);
        check(f.queue.waitingCount() == 32 && f.ingress.recognizedCount() == 34, "fresh manual identity admitted");
    }
    private static void budgetFailureDoesNotReenableRecognizedId() {
        Fixture f = new Fixture(); Ticket first = f.add(1, MAX_CAPTURE_BYTES);
        Attempt rejected = f.submit(f.wire(2), () -> true, 1);
        check(rejected.admitted.status == AdmissionStatus.BUDGET_EXCEEDED && rejected.admitted.ticket == null, "budget refusal owns no ticket");
        check(f.queue.reservedBytes() == MAX_CAPTURE_BYTES && f.ingress.recognizedCount() == 2, "no partial reservation");
        f.queue.runScheduled(1, RETAIN); f.duplicate(2);
        check(f.queue.reservedBytes() == 0 && first.owner() == Owner.TRANSACTION && first.releaseCount() == 0, "engaged ownership survives budget release");
        f.add(3, 7);
        for (int i = 0; i < 2; i++) {
            int id = 4 + i; long bound = i == 0 ? -1 : Long.MAX_VALUE;
            Attempt invalid = f.submit(f.wire(id), () -> true, bound);
            check(invalid.admitted.status == AdmissionStatus.INVALID_BOUND && invalid.admitted.ticket == null, "invalid bound refused");
            f.duplicate(id);
        }
        check(f.queue.waitingCount() == 1 && f.queue.reservedBytes() == 7, "invalid attempts leave ledger unchanged");
    }
    private static void malformedInputCannotReserveMemory() {
        Fixture f = new Fixture(); byte[] oversized = new byte[8193];
        byte[] version = f.wire(1); version[1] = 2;
        byte[] session = f.wire(1); session[2] ^= 1;
        byte[] uuid = f.wire(1); uuid[24] = 0;
        for (byte[] bad : new byte[][]{null, new byte[17], oversized, version, session, uuid}) {
            Attempt result = f.submit(bad, () -> true, 123);
            check(result.admitted == null && f.ingress.recognizedCount() == 0, "invalid envelope or ID not recognized");
        }
        byte[] malformed = f.wire(1); malformed[34] = 0;
        Attempt result = f.submit(malformed, () -> true, 123);
        check(result.recognized.status() == MALFORMED_COMMAND && result.admitted == null, "recognized malformed type not admitted");
        check(f.ingress.recognizedCount() == 1 && f.queue.waitingCount() == 0 && f.queue.reservedBytes() == 0, "history not FIFO allocation");
        f.duplicate(1);
    }
    private static void expiryDoesNotEraseDeduplication() {
        Fixture f = new Fixture(); Ticket expired = f.add(1, 43);
        f.clock.now = MAX_WAIT_NANOS;
        check(f.queue.runScheduled(1, RETAIN).engaged == null && expired.state() == State.EXPIRED, "exact deadline refuses engagement");
        check(expired.releaseCount() == 1 && f.queue.reservedBytes() == 0 && f.ingress.recognizedCount() == 1, "expiry retires lease not UUID");
        f.duplicate(1); Ticket fresh = f.add(2, 5);
        check(f.queue.runScheduled(2, RETAIN).engaged == fresh, "fresh deadline belongs to fresh action only");
    }
    private static void saturationClosurePreservesEngagedWork() {
        Fixture f = new Fixture(); Ticket active = f.add(1, 7);
        f.queue.runScheduled(1, RETAIN); Ticket waiting = f.add(2, 11);
        for (int i = 3; i < MusleeSessionIngress.MAX_RECOGNIZED_ACTIONS; i++) {
            byte[] malformed = f.wire(i); malformed[34] = 0;
            Attempt result = f.submit(malformed, () -> true, 1);
            check(result.recognized.status() == MALFORMED_COMMAND && result.admitted == null, "malformed recognized history advances");
        }
        Attempt limit = f.submit(f.wire(4096), () -> true, 1);
        check(limit.recognized.status() == INVALIDATING && limit.admitted == null, "threshold action cannot reach FIFO");
        check(f.ingress.recognizedCount() == 4096 && f.queue.waitingCount() == 1, "history waits for safe owner cleanup");
        synchronized (f.queue) {
            check(f.queue.invalidateSession(f.ingress.sessionId()) == 1, "owner purges only waiting work");
            check(f.ingress.finishInvalidation(), "owner acknowledges safe invalidation");
        }
        check(waiting.state() == State.INVALIDATED && waiting.releaseCount() == 1, "pending release once");
        check(active.state() == State.ENGAGED && active.owner() == Owner.TRANSACTION && active.releaseCount() == 0, "engaged lease untouched");
        Attempt closed = f.submit(f.wire(5000), () -> true, 1);
        check(closed.recognized.status() == CLOSED && closed.admitted == null && f.ingress.recognizedCount() == 0, "closed session remains terminal");
        MusleeSessionIngress manual = MusleeSessionIngress.open();
        var packet = MusleeCommandProtocol.encode(manual.sessionId(), new MusleeCommandProtocol.Command(action(5001), MusleeCommandProtocol.Type.INSERT, 0));
        var recognized = manual.receive(packet, () -> true);
        Ticket next = f.queue.admit(recognized.command().orElseThrow().actionId(), manual.sessionId(), true, 5).ticket;
        check(next != null && f.queue.runScheduled(2, RETAIN).engaged == null, "new manual session cannot overtake active work");
        check(f.queue.complete(active, true, 2, RETAIN).engaged == next && active.releaseCount() == 1, "verified completion resumes next session");
    }
    private static void boundedCleanupDoesNotReadmitRejectedAction() {
        Fixture f = new Fixture(); List<Ticket> old = new ArrayList<>();
        for (int i = 1; i <= 32; i++) old.add(f.add(i, 1));
        f.clock.now = MAX_WAIT_NANOS;
        Attempt result = f.submit(f.wire(33), () -> true, 1);
        check(result.admitted.status == AdmissionStatus.CLEANUP_REQUIRED && f.queue.waitingCount() == 24, "admission cleanup bounded to eight");
        f.duplicate(33); check(f.queue.waitingCount() == 24, "replay cannot cause a second admission cleanup");
        for (int tick = 1; tick <= 3; tick++) {
            check(f.queue.runScheduled(tick, RETAIN).examined == 8, "scheduled cleanup bounded");
            check(f.queue.runScheduled(tick, RETAIN).examined == 0, "no duplicate same-tick pass");
        }
        for (Ticket ticket : old) check(ticket.state() == State.EXPIRED && ticket.releaseCount() == 1, "every expired lease retired once");
        check(f.ingress.recognizedCount() == 33 && f.queue.reservedBytes() == 0, "cleanup preserves consumed UUIDs");
        f.add(34, 1); check(f.queue.waitingCount() == 1, "only new manual command admitted after cleanup");
    }
    private static void clockFailureDoesNotReplayOrCancelEngagedWork() {
        Fixture f = new Fixture(); Ticket active = f.add(1, 4); f.queue.runScheduled(1, RETAIN);
        Ticket waiting = f.add(2, 5); f.clock.unavailable = true;
        Attempt rejected = f.submit(f.wire(3), () -> true, 6);
        check(rejected.admitted.status == AdmissionStatus.CLOCK_UNCERTAIN, "uncertain time refuses admission");
        check(waiting.state() == State.INVALIDATED && waiting.releaseCount() == 1 && f.queue.reservedBytes() == 0, "waiting work invalidated");
        check(f.queue.active() == active && active.releaseCount() == 0, "active transaction protected");
        f.clock.unavailable = false; f.duplicate(2); f.duplicate(3); f.add(4, 7);
        check(f.queue.waitingCount() == 1 && f.ingress.recognizedCount() == 4, "no automatic resurrection after clock restoration");
    }
    private static void authorityIsRevalidatedBeforeEngagement() {
        Fixture f = new Fixture(); AtomicBoolean access = new AtomicBoolean(false);
        Attempt denied = f.submit(f.wire(1), access::get, 1);
        check(denied.recognized.status() == UNAUTHORIZED && denied.admitted == null, "denial before history and FIFO");
        check(f.ingress.recognizedCount() == 0, "denied identity not recognized");
        access.set(true); Ticket first = f.add(1, 1); access.set(false);
        check(f.queue.runScheduled(1, t -> access.get() ? Verdict.ENGAGE_RETAIN_CAPTURE : Verdict.REJECT).engaged == null,
                "test adapter rechecks current access, not original recognition");
        check(first.state() == State.REJECTED && first.releaseCount() == 1, "access loss retires waiting lease");
        access.set(true); f.duplicate(1); Ticket fresh = f.add(2, 1);
        check(f.queue.runScheduled(2, RETAIN).engaged == fresh, "restored access requires fresh interaction");
    }
    public static void main(String[] args) {
        run(); System.out.println("PASS ingress/FIFO composition: 8 scenarios, exact expiry, saturation at 4096, independent UUID/lease lifetimes");
    }
}
