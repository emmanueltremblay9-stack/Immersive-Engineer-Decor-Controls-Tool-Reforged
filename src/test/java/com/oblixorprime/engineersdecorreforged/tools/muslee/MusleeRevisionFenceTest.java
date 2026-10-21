package com.oblixorprime.engineersdecorreforged.tools.muslee;

import org.junit.Test;

import static org.junit.Assert.*;

public class MusleeRevisionFenceTest {
    private static byte[] state(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) result[i] = (byte) values[i];
        return result;
    }

    private static MusleeRevisionFence fence() {
        MusleeRevisionFence fence = new MusleeRevisionFence(4, 64);
        assertTrue(fence.restoreVerified(state(1, 2, 3), true));
        return fence;
    }

    @Test
    public void startsBlockedUntilAuthoritativeTrackingIsProven() {
        MusleeRevisionFence fence = new MusleeRevisionFence(1, 3);
        assertFalse(fence.isUsable());
        assertFalse(fence.capture().isPresent());
        assertFalse(fence.restoreVerified(state(1), false));
        assertFalse(fence.restoreVerified(null, true));
        assertFalse(fence.restoreVerified(state(1, 2, 3, 4), true));
        assertTrue(fence.restoreVerified(state(1), true));
        assertEquals(0L, fence.revision());
    }

    @Test
    public void completeMultiFieldChangeCountsOnceAndRejectOrNoopNeverCounts() {
        MusleeRevisionFence fence = fence();
        assertEquals(MusleeRevisionFence.Change.REJECTED, fence.recordTransaction(false, null));
        assertEquals(MusleeRevisionFence.Change.UNCHANGED, fence.recordTransaction(true, state(1, 2, 3)));
        assertEquals(0L, fence.revision());
        assertEquals(MusleeRevisionFence.Change.CHANGED, fence.recordTransaction(true, state(7, 8, 9)));
        assertEquals(1L, fence.revision());
        assertEquals(MusleeRevisionFence.Change.UNCHANGED, fence.recordTransaction(true, state(7, 8, 9)));
        assertEquals(1L, fence.revision());
    }

    @Test
    public void distinctCommittedChangesThenRestorationInvalidateAbaCapture() {
        MusleeRevisionFence fence = fence();
        MusleeRevisionFence.Capture old = fence.capture().get();
        fence.recordTransaction(true, state(1, 7, 3));
        fence.recordTransaction(true, state(1, 2, 3));
        assertEquals(2L, fence.revision());
        assertArrayEquals(old.snapshot(), state(1, 2, 3));
        assertFalse(fence.matches(old, state(1, 2, 3)));
        assertTrue(fence.matches(fence.capture().get(), state(1, 2, 3)));
    }

    @Test
    public void captureAndCommittedStateDoNotAliasMutableInputOrReturnedArrays() {
        MusleeRevisionFence fence = fence();
        byte[] incoming = state(9, 8, 7);
        fence.recordTransaction(true, incoming);
        MusleeRevisionFence.Capture capture = fence.capture().get();
        incoming[0] = 0;
        byte[] returned = capture.snapshot();
        returned[1] = 0;
        assertArrayEquals(state(9, 8, 7), capture.snapshot());
        assertTrue(fence.matches(capture, state(9, 8, 7)));
    }

    @Test
    public void restoredStateIsIndependentAndReleasedTicketCannotValidateWithoutReset() {
        MusleeRevisionFence fence = new MusleeRevisionFence(1, 4);
        byte[] source = state(4, 5);
        assertTrue(fence.restoreVerified(source, true));
        source[0] = 99;
        MusleeRevisionFence.Capture capture = fence.capture().get();
        assertTrue(fence.matches(capture, state(4, 5)));
        capture.close();
        assertFalse(fence.matches(capture, state(4, 5)));
        assertTrue(fence.isUsable());
        assertEquals(0, fence.liveCaptures());
    }

    @Test
    public void overflowAndFailedRestorationStayTargetedToOneLocation() {
        MusleeRevisionFence exhausted = new MusleeRevisionFence(1, 4, Long.MAX_VALUE, state(1));
        MusleeRevisionFence other = fence();
        MusleeRevisionFence.Capture otherCapture = other.capture().get();
        exhausted.recordTransaction(true, state(2));
        assertFalse(exhausted.restoreVerified(state(2), false));
        assertFalse(exhausted.isUsable());
        assertTrue(other.matches(otherCapture, state(1, 2, 3)));
        assertEquals(0L, other.revision());
    }

    @Test
    public void independentLocationsAndWrongOwnerCannotCrossValidate() {
        MusleeRevisionFence inventorySlot = fence();
        MusleeRevisionFence reserveCompartment = fence();
        MusleeRevisionFence.Capture slot = inventorySlot.capture().get();
        MusleeRevisionFence.Capture reserve = reserveCompartment.capture().get();
        assertFalse(inventorySlot.matches(reserve, state(1, 2, 3)));
        inventorySlot.invalidateTracking();
        assertFalse(inventorySlot.matches(slot, state(1, 2, 3)));
        assertTrue(reserveCompartment.matches(reserve, state(1, 2, 3)));
        assertEquals(0L, reserveCompartment.revision());
    }

    @Test
    public void foreignOrReleasedTicketCannotPoisonAnotherLocationsTracking() {
        MusleeRevisionFence first = fence();
        MusleeRevisionFence second = fence();
        second.recordTransaction(true, state(9));
        MusleeRevisionFence.Capture foreign = second.capture().get();
        MusleeRevisionFence.Capture local = first.capture().get();
        assertFalse(first.matches(foreign, state(9)));
        assertFalse(first.matches(null, null));
        assertTrue(first.isUsable());
        assertTrue(first.matches(local, state(1, 2, 3)));
        local.close();
        assertFalse(first.matches(local, state(9)));
        assertTrue(first.isUsable());
        assertTrue(second.matches(foreign, state(9)));
    }

    @Test
    public void reachesSignedLongMaximumWithoutWrappingAndNoopAtMaximumIsSafe() {
        MusleeRevisionFence fence = new MusleeRevisionFence(4, 64, Long.MAX_VALUE - 1, state(1));
        assertEquals(MusleeRevisionFence.Change.CHANGED, fence.recordTransaction(true, state(2)));
        assertEquals(Long.MAX_VALUE, fence.revision());
        MusleeRevisionFence.Capture maximum = fence.capture().get();
        assertEquals(MusleeRevisionFence.Change.UNCHANGED, fence.recordTransaction(true, state(2)));
        assertTrue(fence.matches(maximum, state(2)));
        assertEquals(MusleeRevisionFence.Change.UNUSABLE, fence.recordTransaction(true, state(3)));
        assertEquals(Long.MAX_VALUE, fence.revision());
        assertFalse(fence.isUsable());
        assertFalse(fence.matches(maximum, state(2)));
        assertFalse(fence.capture().isPresent());
    }

    @Test
    public void resetWaitsForEveryOldReferenceAndOldTicketNeverRevives() {
        MusleeRevisionFence fence = fence();
        MusleeRevisionFence.Capture first = fence.capture().get();
        MusleeRevisionFence.Capture retainedByTransaction = fence.capture().get();
        assertFalse(fence.restoreVerified(state(1, 2, 3), true));
        assertFalse(fence.isUsable());
        assertFalse(fence.matches(first, state(1, 2, 3)));
        first.close();
        first.close();
        assertEquals(1, fence.liveCaptures());
        assertFalse(fence.restoreVerified(state(1, 2, 3), true));
        // Invalidation did not erase data still needed by an engaged owner.
        assertArrayEquals(state(1, 2, 3), retainedByTransaction.snapshot());
        retainedByTransaction.close();
        assertTrue(fence.restoreVerified(state(1, 2, 3), true));
        assertEquals(0L, fence.revision());
        assertFalse(fence.matches(first, state(1, 2, 3)));
        assertFalse(fence.matches(retainedByTransaction, state(1, 2, 3)));
        assertTrue(fence.matches(fence.capture().get(), state(1, 2, 3)));
    }

    @Test
    public void uncertainOrIncompleteComparisonFailsClosedWithoutInventingRevision() {
        MusleeRevisionFence fence = fence();
        MusleeRevisionFence.Capture capture = fence.capture().get();
        assertFalse(fence.matches(capture, state(1, 2, 3, 0)));
        assertFalse(fence.isUsable());
        assertEquals(0L, fence.revision());
        assertEquals(MusleeRevisionFence.Change.UNUSABLE, fence.recordTransaction(true, state(1, 2, 3)));
        assertFalse(fence.matches(capture, state(1, 2, 3)));
    }

    @Test
    public void missingCommittedCompleteStateInvalidatesInsteadOfKeepingStaleCapture() {
        MusleeRevisionFence fence = fence();
        MusleeRevisionFence.Capture capture = fence.capture().get();
        assertEquals(MusleeRevisionFence.Change.UNUSABLE, fence.recordTransaction(true, null));
        assertFalse(fence.matches(capture, state(1, 2, 3)));
        assertEquals(0L, fence.revision());
    }

    @Test
    public void snapshotBoundAndCaptureBoundFailWithoutPartialCapture() {
        MusleeRevisionFence fence = new MusleeRevisionFence(1, 3);
        assertTrue(fence.restoreVerified(state(1, 2, 3), true));
        MusleeRevisionFence.Capture capture = fence.capture().get();
        assertFalse(fence.capture().isPresent());
        assertEquals(1, fence.liveCaptures());
        capture.close();
        assertEquals(0, fence.liveCaptures());
        assertTrue(fence.capture().isPresent());
        assertEquals(MusleeRevisionFence.Change.UNUSABLE, fence.recordTransaction(true, state(1, 2, 3, 4)));
        assertFalse(fence.isUsable());
    }

    @Test
    public void allCanonicalStateBytesAndPresenceAbsenceMatter() {
        // Synthetic encodings test the primitive, not an ItemStack serialization codec.
        MusleeRevisionFence fence = fence();
        MusleeRevisionFence.Capture original = fence.capture().get();
        assertEquals(MusleeRevisionFence.Change.CHANGED, fence.recordTransaction(true, state(1, 2)));
        assertFalse(fence.matches(original, state(1, 2)));
        assertEquals(MusleeRevisionFence.Change.CHANGED, fence.recordTransaction(true, state(1, 2, 0)));
        assertEquals(2L, fence.revision());
    }

    @Test
    public void preflightBlocksOverflowBeforeAnyAuthoritativeWrite() {
        MusleeRevisionFence fence = new MusleeRevisionFence(2, 4, Long.MAX_VALUE, state(1));
        MusleeRevisionFence.Capture capture = fence.capture().get();
        byte[] authoritative = state(1);
        assertTrue(fence.matches(capture, authoritative));
        assertTrue(fence.preflightForCommit(state(1))); // A no-op is safe at MAX.
        if (fence.preflightForCommit(state(2))) {
            authoritative = state(2);
            fence.recordTransaction(true, authoritative);
        }
        assertArrayEquals(state(1), authoritative);
        assertEquals(Long.MAX_VALUE, fence.revision());
        assertFalse(fence.isUsable());
        assertFalse(fence.matches(capture, authoritative));
        assertArrayEquals(state(1), capture.snapshot()); // Still retained by its owner.
    }

    @Test
    public void preflightDoesNotCountAChangeOrInvalidateAUsableCapture() {
        MusleeRevisionFence fence = fence();
        MusleeRevisionFence.Capture capture = fence.capture().get();
        assertTrue(fence.preflightForCommit(state(4, 5, 6)));
        assertEquals(0L, fence.revision());
        assertTrue(fence.matches(capture, state(1, 2, 3)));
        assertEquals(MusleeRevisionFence.Change.CHANGED,
                fence.recordTransaction(true, state(4, 5, 6)));
        assertEquals(1L, fence.revision());
    }

    @Test
    public void preflightRejectsUnprovableFinalStateBeforeWriting() {
        MusleeRevisionFence fence = fence();
        assertFalse(fence.preflightForCommit(null));
        assertFalse(fence.isUsable());
        assertEquals(0L, fence.revision());
        assertFalse(fence.preflightForCommit(state(1, 2, 3)));
    }

    @Test
    public void releaseDiscardsOwnedSnapshotAndRefusesFutureSnapshotAccess() {
        MusleeRevisionFence fence = fence();
        MusleeRevisionFence.Capture capture = fence.capture().get();
        byte[] independentlyOwnedCopy = capture.snapshot();
        capture.close();
        capture.close();
        assertEquals(0, fence.liveCaptures());
        assertArrayEquals(state(1, 2, 3), independentlyOwnedCopy);
        try {
            capture.snapshot();
            fail("Released capture must not retain or return a snapshot");
        } catch (IllegalStateException expected) {
            // The released ticket owns no retained state and cannot allocate a copy.
        }
        assertFalse(fence.matches(capture, state(1, 2, 3)));
        assertTrue(fence.restoreVerified(state(1, 2, 3), true));
        assertFalse(fence.matches(capture, state(1, 2, 3)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeInitialCounter() {
        new MusleeRevisionFence(1, 1, -1, state(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnboundedCaptureConfiguration() {
        new MusleeRevisionFence(0, 1);
    }
}
