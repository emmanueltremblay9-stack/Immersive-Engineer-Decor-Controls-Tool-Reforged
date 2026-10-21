package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeCommandQueue.*;

public class MusleeCommandQueueTest {
    private static final UUID SESSION = UUID.randomUUID();
    private static final Validator RETAIN = ticket -> Verdict.ENGAGE_RETAIN_CAPTURE;
    private static final Validator REJECT = ticket -> Verdict.REJECT;
    private static class TestClock implements Clock {
        long now;
        boolean unavailable;
        public long nanoTime() { if (unavailable) throw new IllegalStateException(); return now; }
    }
    private final TestClock clock = new TestClock();
    private final MusleeCommandQueue queue = new MusleeCommandQueue(UUID.randomUUID(), clock);
    private Ticket add(long bytes) { return add(SESSION, bytes); }
    private Ticket add(UUID session, long bytes) {
        Admission admission = queue.admit(UUID.randomUUID(), session, true, bytes);
        assertEquals(AdmissionStatus.ADMITTED, admission.status);
        return admission.ticket;
    }

    @Test public void strictFifoAndIndependentPresses() {
        Ticket first = add(1), second = add(2);
        assertSame(first, queue.runScheduled(1, RETAIN).engaged);
        assertSame(second, queue.complete(first, true, 1, RETAIN).engaged);
        MusleeCommandQueue other = new MusleeCommandQueue(UUID.randomUUID(), clock);
        Ticket independent = other.admit(UUID.randomUUID(), SESSION, true, 1).ticket;
        assertSame(independent, other.runScheduled(1, RETAIN).engaged);
    }
    @Test public void thirtyTwoWaitingPlusOneEngaged() {
        Ticket active = add(MAX_CAPTURE_BYTES);
        queue.runScheduled(1, RETAIN);
        assertEquals(0, queue.reservedBytes());
        for (int i = 0; i < 32; i++) add(1);
        assertEquals(AdmissionStatus.FULL, queue.admit(UUID.randomUUID(), SESSION, true, 1).status);
        assertEquals(32, queue.waitingCount());
        assertSame(active, queue.active());
        assertEquals(0, queue.runScheduled(2, RETAIN).examined);
    }
    @Test public void budgetAdmissionIsAtomicAndOverflowSafe() {
        add(MAX_CAPTURE_BYTES);
        assertEquals(AdmissionStatus.BUDGET_EXCEEDED, queue.admit(UUID.randomUUID(), SESSION, true, 1).status);
        assertEquals(AdmissionStatus.INVALID_BOUND, queue.admit(UUID.randomUUID(), SESSION, true, Long.MAX_VALUE).status);
        assertEquals(AdmissionStatus.INVALID_BOUND, queue.admit(UUID.randomUUID(), SESSION, true, -1).status);
        assertEquals(1, queue.waitingCount());
        assertEquals(MAX_CAPTURE_BYTES, queue.reservedBytes());
    }
    @Test public void exactDeadlineAndRecheckAfterValidation() {
        Ticket before = add(9);
        clock.now = MAX_WAIT_NANOS - 1;
        assertSame(before, queue.runScheduled(1, RETAIN).engaged);
        queue.complete(before, true, 1, REJECT);
        Ticket atDeadline = add(9);
        clock.now += MAX_WAIT_NANOS;
        assertNull(queue.runScheduled(2, RETAIN).engaged);
        assertEquals(State.EXPIRED, atDeadline.state());
        Ticket expiresDuringValidation = add(7);
        assertNull(queue.runScheduled(3, ticket -> { clock.now += MAX_WAIT_NANOS; return Verdict.ENGAGE_RETAIN_CAPTURE; }).engaged);
        assertEquals(State.EXPIRED, expiresDuringValidation.state());
        assertEquals(0, queue.reservedBytes());
    }
    @Test public void expiredBacklogIsBoundedAndNeverReportedAsFull() {
        List<Ticket> old = new ArrayList<Ticket>();
        for (int i = 0; i < 32; i++) old.add(add(10));
        clock.now = MAX_WAIT_NANOS;
        assertEquals(AdmissionStatus.CLEANUP_REQUIRED, queue.admit(UUID.randomUUID(), SESSION, true, 1).status);
        assertEquals(24, queue.waitingCount());
        Pass pass = queue.runScheduled(1, RETAIN);
        assertEquals(8, pass.examined);
        assertEquals(16, queue.waitingCount());
        assertNull(pass.engaged);
        assertEquals(0, queue.runScheduled(1, RETAIN).examined);
        queue.runScheduled(2, RETAIN);
        queue.runScheduled(3, RETAIN);
        assertEquals(0, queue.waitingCount());
        for (Ticket ticket : old) { assertEquals(State.EXPIRED, ticket.state()); assertEquals(1, ticket.releaseCount()); }
        assertFalse(queue.continuationPending());
    }
    @Test public void sevenExpiredThenLiveDoesNotNeedAnotherCleanupPass() {
        for (int i = 0; i < 7; i++) add(1);
        clock.now = MAX_WAIT_NANOS - 1;
        Ticket survivor = add(1);
        clock.now++;
        Ticket fresh = add(1);
        assertEquals(2, queue.waitingCount());
        assertSame(survivor, queue.runScheduled(1, RETAIN).engaged);
        assertSame(fresh, queue.complete(survivor, true, 1, RETAIN).engaged);
    }
    @Test public void everyStatusConsumesAnExamination() {
        for (int i = 0; i < 4; i++) add(1);
        clock.now = MAX_WAIT_NANOS - 1;
        for (int i = 0; i < 5; i++) add(1);
        clock.now++;
        AtomicInteger validations = new AtomicInteger();
        Pass pass = queue.runScheduled(1, ticket -> { validations.incrementAndGet(); return Verdict.TRANSFER_FAILED; });
        assertEquals(8, pass.examined);
        assertEquals(4, validations.get());
        assertEquals(1, queue.waitingCount());
        assertTrue(pass.continuationPending);
        assertEquals(1, queue.runScheduled(2, RETAIN).examined);
    }
    @Test public void sessionInvalidationImmediatelyRemovesAllAndPreservesActiveAndOrder() {
        UUID other = UUID.randomUUID();
        Ticket active = add(SESSION, 20);
        queue.runScheduled(1, RETAIN);
        Ticket survivor = add(other, 3);
        for (int i = 0; i < 30; i++) add(SESSION, 2);
        Ticket last = add(other, 4);
        assertEquals(30, queue.invalidateSession(SESSION));
        assertEquals(2, queue.waitingCount());
        assertEquals(7, queue.reservedBytes());
        assertEquals(State.ENGAGED, active.state());
        assertEquals(Owner.TRANSACTION, active.owner());
        assertSame(survivor, queue.complete(active, true, 1, RETAIN).engaged);
        assertSame(last, queue.complete(survivor, true, 1, RETAIN).engaged);
    }
    @Test public void exclusiveOwnershipAndExactlyOnceRelease() {
        Ticket retained = add(40);
        queue.runScheduled(1, RETAIN);
        assertEquals(Owner.TRANSACTION, retained.owner());
        assertEquals(0, retained.releaseCount());
        assertEquals(0, queue.waitingCount());
        assertEquals(0, queue.reservedBytes());
        queue.complete(retained, false, 1, REJECT);
        assertSame(retained, queue.active());
        queue.complete(retained, true, 1, REJECT);
        queue.complete(retained, true, 1, REJECT);
        queue.invalidateSession(SESSION);
        assertEquals(1, retained.releaseCount());
        assertEquals(State.COMPLETED, retained.state());
        Ticket unnecessary = add(30);
        queue.runScheduled(2, ticket -> Verdict.ENGAGE_RELEASE_CAPTURE);
        assertEquals(Owner.RELEASED, unnecessary.owner());
        assertEquals(1, unnecessary.releaseCount());
        queue.complete(unnecessary, true, 2, REJECT);
        assertEquals(1, unnecessary.releaseCount());
    }
    @Test public void failedTransferAndValidationAreTerminal() {
        Ticket failed = add(8), thrown = add(9);
        Pass pass = queue.runScheduled(1, ticket -> {
            if (ticket == failed) return Verdict.TRANSFER_FAILED;
            throw new IllegalStateException("unprovable validation");
        });
        assertEquals(2, pass.examined);
        assertNull(queue.active());
        assertEquals(0, queue.reservedBytes());
        assertEquals(State.REJECTED, failed.state());
        assertEquals(State.REJECTED, thrown.state());
        assertEquals(1, failed.releaseCount());
        assertEquals(1, thrown.releaseCount());
    }
    @Test public void uncertainClockInvalidatesAllWaitingButNotActive() {
        Ticket active = add(1);
        queue.runScheduled(1, RETAIN);
        List<Ticket> waiting = new ArrayList<Ticket>();
        for (int i = 0; i < 32; i++) waiting.add(add(1));
        clock.unavailable = true;
        assertEquals(AdmissionStatus.CLOCK_UNCERTAIN, queue.admit(UUID.randomUUID(), SESSION, true, 1).status);
        assertSame(active, queue.active());
        assertEquals(0, queue.waitingCount());
        assertEquals(0, queue.reservedBytes());
        for (Ticket ticket : waiting) { assertEquals(State.INVALIDATED, ticket.state()); assertEquals(1, ticket.releaseCount()); }
        clock.unavailable = false;
        add(1);
        assertEquals(1, queue.waitingCount()); // Only a fresh manual admission exists.
    }
    @Test public void negativeOriginsAreValidButBackwardAndOverflowReadingsFailClosed() {
        clock.now = -100;
        Ticket first = add(1);
        clock.now = -99;
        assertSame(first, queue.runScheduled(1, RETAIN).engaged);
        Ticket waiting = add(1);
        clock.now = -101;
        assertEquals(AdmissionStatus.CLOCK_UNCERTAIN, queue.admit(UUID.randomUUID(), SESSION, true, 1).status);
        assertEquals(State.INVALIDATED, waiting.state());
        clock.now = Long.MIN_VALUE;
        Ticket hugeGap = add(1);
        clock.now = Long.MAX_VALUE;
        assertEquals(AdmissionStatus.CLOCK_UNCERTAIN, queue.admit(UUID.randomUUID(), SESSION, true, 1).status);
        assertEquals(State.INVALIDATED, hugeGap.state());
    }
    @Test public void requestsDuringPassCoalesceAndNoTickRunsTwice() {
        for (int i = 0; i < 20; i++) add(1);
        Validator requestWhileRejecting = ticket -> { queue.requestResume(); queue.requestResume(); return Verdict.REJECT; };
        assertEquals(8, queue.runScheduled(11, requestWhileRejecting).examined);
        assertTrue(queue.continuationPending());
        assertTrue(queue.resumeDemanded());
        assertEquals(0, queue.runScheduled(11, REJECT).examined);
        assertEquals(0, queue.runScheduled(10, REJECT).examined);
        assertEquals(8, queue.runScheduled(12, REJECT).examined);
        assertEquals(4, queue.runScheduled(13, REJECT).examined);
        assertFalse(queue.continuationPending());
        assertFalse(queue.resumeDemanded());
    }
    @Test(timeout = 10000) public void concurrentAdmissionsCannotExceedMemoryOrCapacity() throws Exception {
        final int workers = 48;
        CountDownLatch ready = new CountDownLatch(workers), go = new CountDownLatch(1), finished = new CountDownLatch(workers);
        AtomicInteger admitted = new AtomicInteger();
        for (int i = 0; i < workers; i++) new Thread(() -> {
            ready.countDown();
            try { go.await(); if (queue.admit(UUID.randomUUID(), SESSION, true, 8192).status == AdmissionStatus.ADMITTED) admitted.incrementAndGet(); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { finished.countDown(); }
        }).start();
        ready.await(); go.countDown(); finished.await();
        assertEquals(32, admitted.get());
        assertEquals(32, queue.waitingCount());
        assertEquals(MAX_CAPTURE_BYTES, queue.reservedBytes());
    }
    @Test public void invalidSessionCannotReceiveSaturationStatus() {
        for (int i = 0; i < 32; i++) add(1);
        assertEquals(AdmissionStatus.INVALID_SESSION, queue.admit(UUID.randomUUID(), SESSION, false, 1).status);
    }
    @Test public void immediateCompletionPassAlsoStopsAtEight() {
        Ticket active = add(1);
        queue.runScheduled(1, RETAIN);
        for (int i = 0; i < 17; i++) add(1);
        Pass pass = queue.complete(active, true, 1, REJECT);
        assertEquals(8, pass.examined);
        assertEquals(9, queue.waitingCount());
        assertTrue(pass.continuationPending);
    }
    @Test public void completionTickDefersContinuationUntilStrictlyNextTick() {
        Ticket active = add(1);
        queue.runScheduled(1, RETAIN);
        for (int i = 0; i < 17; i++) add(1);
        Pass immediate = queue.complete(active, true, 10, REJECT);
        assertEquals(8, immediate.examined);
        assertEquals(9, queue.waitingCount());
        assertTrue(immediate.continuationPending);
        assertEquals(0, queue.runScheduled(10, REJECT).examined);
        assertEquals(0, queue.runScheduled(9, REJECT).examined);
        assertEquals(9, queue.waitingCount());
        assertEquals(8, queue.runScheduled(11, REJECT).examined);
        assertEquals(0, queue.runScheduled(11, REJECT).examined);
        assertEquals(1, queue.runScheduled(12, REJECT).examined);
        assertFalse(queue.continuationPending());
    }

}
