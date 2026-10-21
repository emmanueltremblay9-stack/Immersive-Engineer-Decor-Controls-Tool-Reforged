package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeCommandQueue.*;

/** Real buffer lifetime tests, not proof of ItemStack encoding or JVM memory sizing. */
public class MusleeCaptureOwnershipTest {
    private static final UUID SESSION = UUID.randomUUID();
    private static final Validator RETAIN = ticket -> Verdict.ENGAGE_RETAIN_CAPTURE;
    private static final Validator REJECT = ticket -> Verdict.REJECT;
    private static final class TestClock implements Clock {
        long now;
        public long nanoTime() { return now; }
    }
    private final TestClock clock = new TestClock();
    private final MusleeCommandQueue queue = new MusleeCommandQueue(UUID.randomUUID(), clock);
    private MusleeItemStackCapture capture() {
        return MusleeItemStackCapture.fromEncodedForOwnershipTest(new byte[] { 1, 2, 3 });
    }
    private Ticket add(MusleeItemStackCapture capture) {
        Admission a = queue.admitCaptureForOwnershipTest(UUID.randomUUID(), SESSION, true, 100, capture);
        assertEquals(AdmissionStatus.ADMITTED, a.status);
        return a.ticket;
    }
    private void released(MusleeItemStackCapture c) {
        assertEquals(0, c.encodedSize());
        assertEquals(1, c.releaseCount());
    }
    @Test public void runtimeAdmissionAlwaysRefusesUnprovedFootprintAndReleasesCandidate() {
        MusleeItemStackCapture c = capture();
        assertEquals(AdmissionStatus.INVALID_BOUND,
                queue.admitCapture(UUID.randomUUID(), SESSION, true, c).status);
        assertEquals(0, queue.waitingCount());
        assertEquals(0, queue.reservedBytes());
        released(c);
    }
    @Test public void rejectedAdmissionReleasesRealBufferWithoutReservation() {
        MusleeItemStackCapture c = capture();
        assertEquals(AdmissionStatus.INVALID_SESSION,
                queue.admitCaptureForOwnershipTest(UUID.randomUUID(), SESSION, false, 100, c).status);
        assertEquals(0, queue.reservedBytes());
        assertEquals(0, queue.waitingCount());
        released(c);
    }
    @Test public void unknownOrExcessiveScalarFixtureStillRejectsAndReleases() {
        MusleeItemStackCapture c = capture();
        assertEquals(AdmissionStatus.INVALID_BOUND,
                queue.admitCaptureForOwnershipTest(UUID.randomUUID(), SESSION, true, Long.MAX_VALUE, c).status);
        released(c);
        assertEquals(0, queue.reservedBytes());
    }
    @Test public void callerCannotCloseOrReadoptAnOwnedCapture() {
        MusleeItemStackCapture c = capture();
        Ticket t = add(c);
        c.close();
        assertEquals(3, c.encodedSize());
        assertEquals(0, c.releaseCount());
        MusleeCommandQueue other = new MusleeCommandQueue(UUID.randomUUID(), clock);
        assertEquals(AdmissionStatus.INVALID_BOUND,
                other.admitCaptureForOwnershipTest(UUID.randomUUID(), SESSION, true, 100, c).status);
        assertEquals(AdmissionStatus.INVALID_BOUND,
                other.admitCapture(UUID.randomUUID(), SESSION, true, c).status);
        assertEquals(0, c.releaseCount());
        assertSame(t, queue.runScheduled(1, RETAIN).engaged);
    }
    @Test public void engagementTransfersWithoutCopyAndPreservesUntilVerifiedCompletion() {
        MusleeItemStackCapture c = capture();
        Ticket t = add(c);
        assertSame(t, queue.runScheduled(1, RETAIN).engaged);
        assertEquals(Owner.TRANSACTION, t.owner());
        assertEquals(0, queue.reservedBytes());
        assertEquals(3, c.encodedSize());
        assertEquals(0, c.releaseCount());
        queue.complete(t, false, 2, REJECT);
        assertSame(t, queue.active());
        assertEquals(0, c.releaseCount());
        queue.complete(t, true, 3, REJECT);
        released(c);
        queue.complete(t, true, 4, REJECT);
        c.close();
        released(c);
    }
    @Test public void sessionInvalidationReleasesWaitingButNeverActive() {
        MusleeItemStackCapture active = capture(), waiting = capture();
        Ticket t = add(active);
        queue.runScheduled(1, RETAIN);
        add(waiting);
        assertEquals(1, queue.invalidateSession(SESSION));
        released(waiting);
        assertEquals(0, active.releaseCount());
        assertSame(t, queue.active());
        queue.complete(t, true, 2, REJECT);
        released(active);
    }
    @Test public void clockInvalidationReleasesWaitingButNeverActive() {
        MusleeItemStackCapture active = capture(), waiting = capture();
        Ticket t = add(active);
        queue.runScheduled(1, RETAIN);
        add(waiting);
        queue.invalidateUncertainClock();
        released(waiting);
        assertEquals(0, active.releaseCount());
        queue.complete(t, true, 2, REJECT);
        released(active);
    }
    @Test public void exactExpirationReleasesActualBufferOnce() {
        MusleeItemStackCapture c = capture();
        Ticket t = add(c);
        clock.now = MAX_WAIT_NANOS;
        queue.runScheduled(1, RETAIN);
        assertEquals(State.EXPIRED, t.state());
        released(c);
        queue.invalidateSession(SESSION);
        released(c);
    }
    @Test public void rejectedValidationAndFailedTransferVerdictReleaseActualBuffers() {
        MusleeItemStackCapture a = capture(), b = capture();
        Ticket first = add(a), second = add(b);
        queue.runScheduled(1, ticket -> ticket == first ? Verdict.REJECT : Verdict.TRANSFER_FAILED);
        assertEquals(State.REJECTED, first.state());
        assertEquals(State.REJECTED, second.state());
        released(a); released(b);
        assertNull(queue.active());
        assertEquals(0, queue.reservedBytes());
    }
    @Test public void unneededCaptureReleasesAtEngagementNotAtFinalization() {
        MusleeItemStackCapture c = capture();
        Ticket t = add(c);
        assertSame(t, queue.runScheduled(1, ticket -> Verdict.ENGAGE_RELEASE_CAPTURE).engaged);
        released(c);
        assertEquals(Owner.RELEASED, t.owner());
        queue.complete(t, true, 2, REJECT);
        released(c);
    }
    @Test public void fullQueueReleasesOnlyTheRejectedCandidate() {
        for (int i = 0; i < MAX_WAITING; i++) add(capture());
        MusleeItemStackCapture rejected = capture();
        assertEquals(AdmissionStatus.FULL,
                queue.admitCaptureForOwnershipTest(UUID.randomUUID(), SESSION, true, 100, rejected).status);
        released(rejected);
        assertEquals(MAX_WAITING, queue.waitingCount());
        assertEquals(3200, queue.reservedBytes());
    }
    @Test public void budgetRejectionDoesNotReleaseExistingCapture() {
        MusleeItemStackCapture retained = capture(), rejected = capture();
        assertEquals(AdmissionStatus.ADMITTED,
                queue.admitCaptureForOwnershipTest(UUID.randomUUID(), SESSION, true,
                        MAX_CAPTURE_BYTES, retained).status);
        assertEquals(AdmissionStatus.BUDGET_EXCEEDED,
                queue.admitCaptureForOwnershipTest(UUID.randomUUID(), SESSION, true, 1, rejected).status);
        released(rejected);
        assertEquals(0, retained.releaseCount());
        assertEquals(MAX_CAPTURE_BYTES, queue.reservedBytes());
        queue.invalidateSession(SESSION);
        released(retained);
    }
    @Test public void concurrentQueuesCannotBecomeDoubleOwners() throws Exception {
        MusleeItemStackCapture c = capture();
        MusleeCommandQueue other = new MusleeCommandQueue(UUID.randomUUID(), clock);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        Admission[] results = new Admission[2];
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Thread first = new Thread(() -> {
            try { start.await(); results[0] = queue.admitCaptureForOwnershipTest(UUID.randomUUID(), SESSION, true, 100, c); }
            catch (Throwable error) { failure.compareAndSet(null, error); }
        });
        Thread second = new Thread(() -> {
            try { start.await(); results[1] = other.admitCaptureForOwnershipTest(UUID.randomUUID(), SESSION, true, 100, c); }
            catch (Throwable error) { failure.compareAndSet(null, error); }
        });
        first.start(); second.start(); start.countDown();
        first.join(5000); second.join(5000);
        assertFalse(first.isAlive()); assertFalse(second.isAlive());
        assertNull(failure.get());
        assertNotNull(results[0]); assertNotNull(results[1]);
        assertEquals(1, queue.waitingCount() + other.waitingCount());
        assertEquals(100, queue.reservedBytes() + other.reservedBytes());
        assertEquals(0, c.releaseCount());
        queue.invalidateSession(SESSION); other.invalidateSession(SESSION);
        released(c);
    }
}
