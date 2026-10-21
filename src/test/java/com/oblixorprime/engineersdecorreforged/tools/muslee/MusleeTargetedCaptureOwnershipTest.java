package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeCommandQueue.*;

/** Concrete private-buffer lifecycle, not ItemStack encoding or a memory-bound proof. */
public final class MusleeTargetedCaptureOwnershipTest {
    private static final UUID SESSION = UUID.randomUUID();
    private static final Validator RETAIN = ticket -> Verdict.ENGAGE_RETAIN_CAPTURE;
    private static final Validator REJECT = ticket -> Verdict.REJECT;

    private static MusleeCommandQueue queue() {
        return new MusleeCommandQueue(UUID.randomUUID(), () -> 0L);
    }

    private static MusleeItemStackCapture capture() {
        return MusleeItemStackCapture.fromEncodedForOwnershipTest(new byte[]{1, 2, 3});
    }

    private static Ticket add(MusleeCommandQueue queue, MusleeItemStackCapture capture, long bytes) {
        Admission admission = queue.admitCaptureForOwnershipTest(
                UUID.randomUUID(), SESSION, true, bytes, capture);
        assertEquals(AdmissionStatus.ADMITTED, admission.status);
        return admission.ticket;
    }

    private static void released(MusleeItemStackCapture capture) {
        assertTrue(capture.isClosed());
        assertEquals(0, capture.encodedSize());
        assertEquals(1, capture.releaseCount());
    }

    @Test public void targetedRemovalReleasesConcreteCaptureAndPreservesEngagedAndFifo() {
        MusleeCommandQueue queue = queue();
        MusleeItemStackCapture activeCapture = capture();
        Ticket active = add(queue, activeCapture, 100);
        assertSame(active, queue.runScheduled(1, RETAIN).engaged);
        MusleeItemStackCapture firstCapture = capture(), targetCapture = capture(), lastCapture = capture();
        Ticket first = add(queue, firstCapture, 11);
        Ticket target = add(queue, targetCapture, 13);
        Ticket last = add(queue, lastCapture, 17);

        assertTrue(queue.invalidateWaiting(target));
        assertFalse(queue.invalidateWaiting(target));
        released(targetCapture);
        assertEquals(State.INVALIDATED, target.state());
        assertEquals(Owner.RELEASED, target.owner());
        assertEquals(1, target.releaseCount());
        assertEquals(2, queue.waitingCount());
        assertEquals(28, queue.reservedBytes());
        assertFalse(queue.invalidateWaiting(active));
        assertSame(active, queue.active());
        assertEquals(State.ENGAGED, active.state());
        assertEquals(Owner.TRANSACTION, active.owner());
        assertEquals(3, activeCapture.encodedSize());
        assertEquals(0, activeCapture.releaseCount());
        assertEquals(0, firstCapture.releaseCount());
        assertEquals(0, lastCapture.releaseCount());

        assertSame(first, queue.complete(active, true, 2, RETAIN).engaged);
        released(activeCapture);
        assertSame(last, queue.complete(first, true, 2, RETAIN).engaged);
        released(firstCapture);
        queue.complete(last, true, 2, REJECT);
        released(lastCapture);
        queue.invalidateSession(SESSION);
        targetCapture.close();
        released(targetCapture);
        assertEquals(1, target.releaseCount());
        assertEquals(0, queue.waitingCount());
        assertEquals(0, queue.reservedBytes());
        assertNull(queue.active());
    }

    @Test public void foreignAndTerminalReferencesNeverReleaseAnotherOwnersCapture() {
        MusleeCommandQueue queue = queue(), other = queue();
        MusleeItemStackCapture ownCapture = capture(), foreignCapture = capture();
        Ticket own = add(queue, ownCapture, 7), foreign = add(other, foreignCapture, 9);
        assertFalse(queue.invalidateWaiting(null));
        assertFalse(queue.invalidateWaiting(foreign));
        assertEquals(3, foreignCapture.encodedSize());
        assertEquals(0, foreignCapture.releaseCount());
        assertEquals(9, other.reservedBytes());
        assertTrue(queue.invalidateWaiting(own));
        released(ownCapture);
        assertFalse(other.invalidateWaiting(own));
        assertTrue(other.invalidateWaiting(foreign));
        released(foreignCapture);
        assertFalse(other.invalidateWaiting(foreign));
        released(foreignCapture);
    }

    @Test public void lastConcreteRemovalReleasesFullBudgetWithoutEnablingRuntimeAdmission() {
        MusleeCommandQueue queue = queue();
        MusleeItemStackCapture oldCapture = capture();
        Ticket old = add(queue, oldCapture, MAX_CAPTURE_BYTES);
        assertTrue(queue.invalidateWaiting(old));
        released(oldCapture);
        assertEquals(0, queue.reservedBytes());
        assertFalse(queue.continuationPending());
        assertFalse(queue.resumeDemanded());
        MusleeItemStackCapture freshCapture = capture();
        Ticket fresh = add(queue, freshCapture, MAX_CAPTURE_BYTES);
        assertSame(fresh, queue.runScheduled(1, RETAIN).engaged);
        MusleeItemStackCapture unqualified = capture();
        assertEquals(AdmissionStatus.INVALID_BOUND,
                queue.admitCapture(UUID.randomUUID(), SESSION, true, unqualified).status);
        released(unqualified);
        assertSame(fresh, queue.active());
        assertEquals(0, freshCapture.releaseCount());
        queue.complete(fresh, true, 2, REJECT);
        released(freshCapture);
    }
}
