package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.Arrays;
import java.util.Optional;

/**
 * Inactive, volatile revision primitive for one inventory slot or reserve compartment.
 *
 * <p>This is not an Inventory/ItemStack mutation tracker or a canonical-state codec.
 * An eventual authoritative adapter must supply a complete, canonical encoding of the
 * item identifier, quantity, component values AND presence/absence, and reserve seed
 * type. Equal logical states must have equal encodings. Every mutation origin,
 * including in-place component changes, must be tracked. If any of those guarantees
 * cannot be proved, the adapter must invalidate this fence and refuse engagement.
 * No runtime hook or registration is installed by this class.
 *
 * <p>Use a distinct instance for every tracked location. Call recordTransaction once
 * per complete committed transaction, never for its individual field writes. The
 * caller must hold its authoritative transaction lock across all affected locations,
 * FIFO invalidation and engagement. Preflight EVERY affected location before any
 * authoritative write; this object's monitor alone does not make a
 * multi-location transaction atomic. Invalid captures are admission/engagement
 * fences, not instructions to cancel an already engaged transaction. Their immutable
 * state remains readable until the owner safely releases them.
 *
 * <p>The limits below bound this primitive, not the aggregate FIFO memory budget:
 * the adapter must separately account for structures, copies and overhead.
 */
public final class MusleeRevisionFence {
    public enum Change { REJECTED, UNCHANGED, CHANGED, UNUSABLE }

    private final int maxCaptures;
    private final int maxSnapshotBytes;
    private Object generation = new Object();
    private long revision;
    private byte[] current;
    private boolean usable;
    private int liveCaptures;

    /** Starts blocked. Restoration requires verified tracking and no old references. */
    public MusleeRevisionFence(int maxCaptures, int maxSnapshotBytes) {
        if (maxCaptures <= 0 || maxSnapshotBytes <= 0) {
            throw new IllegalArgumentException("Positive capture and snapshot bounds required");
        }
        this.maxCaptures = maxCaptures;
        this.maxSnapshotBytes = maxSnapshotBytes;
    }

    /** Package-private boundary fixture; not a persistence or runtime restoration API. */
    MusleeRevisionFence(int maxCaptures, int maxSnapshotBytes, long initialRevision,
                        byte[] verifiedCanonicalState) {
        this(maxCaptures, maxSnapshotBytes);
        if (initialRevision < 0 || !validState(verifiedCanonicalState)) {
            throw new IllegalArgumentException("Invalid revision or complete state");
        }
        revision = initialRevision;
        current = verifiedCanonicalState.clone();
        usable = true;
    }

    /**
     * Explicit restoration only after authoritative verification of exhaustive tracking.
     * Even a failed reset attempt first fences off the old generation. A successful
     * reset permits NEW manual commands only; it never revives a ticket or replays work.
     * All references held by queued or engaged transactions must have been released.
     */
    public synchronized boolean restoreVerified(byte[] canonicalState, boolean trackingProven) {
        usable = false;
        if (!trackingProven || liveCaptures != 0 || !validState(canonicalState)) {
            return false;
        }
        current = canonicalState.clone();
        generation = new Object();
        revision = 0;
        usable = true;
        return true;
    }

    /** No snapshot, partial reservation or ticket is retained on failure. */
    public synchronized Optional<Capture> capture() {
        if (!usable || liveCaptures == maxCaptures) {
            return Optional.empty();
        }
        Capture capture = new Capture(this, generation, revision, current.clone());
        liveCaptures++;
        return Optional.of(capture);
    }

    /**
     * Required before committing any authoritative change, under the caller's common
     * transaction lock. Preflight every affected fence before writing any location and
     * keep that lock through all writes and recordTransaction calls. A changed state
     * at Long.MAX_VALUE is fenced off here BEFORE the authoritative mutation; an
     * unchanged state at the maximum remains safe. Failure permits no write.
     *
     * <p>This is not a lock lease or proof of complete mutation tracking. Its result
     * must not be cached or used after releasing the common authoritative lock.
     */
    public synchronized boolean preflightForCommit(byte[] finalCanonicalState) {
        if (!usable) {
            return false;
        }
        if (!validState(finalCanonicalState)
                || (revision == Long.MAX_VALUE && !Arrays.equals(current, finalCanonicalState))) {
            usable = false;
            return false;
        }
        return true;
    }

    /**
     * Records the final complete state, once, after a transaction's outcome is known.
     * Rejection does not read the supplied state. A committed state whose complete
     * encoding is unavailable or oversized makes tracking unusable instead of silently
     * retaining the old revision. No counter ever wraps or becomes negative. The
     * preflightForCommit check is mandatory BEFORE the actual write; the defensive
     * checks here cannot undo an authoritative change already made by a broken adapter.
     */
    public synchronized Change recordTransaction(boolean committed, byte[] canonicalState) {
        if (!committed) {
            return Change.REJECTED;
        }
        if (!usable) {
            return Change.UNUSABLE;
        }
        if (!validState(canonicalState)) {
            usable = false;
            return Change.UNUSABLE;
        }
        if (Arrays.equals(current, canonicalState)) {
            return Change.UNCHANGED;
        }
        if (revision == Long.MAX_VALUE) {
            usable = false;
            return Change.UNUSABLE;
        }
        byte[] next = canonicalState.clone();
        revision++;
        current = next;
        return Change.CHANGED;
    }

    /** Targeted invalidation; other fence instances and retained capture data are untouched. */
    public synchronized void invalidateTracking() {
        usable = false;
    }

    /**
     * Compare both revision and full immutable snapshot against the authoritative state.
     * An unexpected current-state difference proves tracking unreliable and blocks this
     * location. The caller must not separate this check from atomic engagement.
     * A match is not write permission: preflightForCommit is separately required.
     */
    public synchronized boolean matches(Capture capture, byte[] authoritativeCanonicalState) {
        if (!usable || capture == null || capture.owner != this || capture.released
                || capture.generation != generation) {
            return false;
        }
        if (!validState(authoritativeCanonicalState)
                || !Arrays.equals(current, authoritativeCanonicalState)) {
            usable = false;
            return false;
        }
        return capture.revision == revision
                && Arrays.equals(capture.state, authoritativeCanonicalState);
    }

    public synchronized boolean isUsable() { return usable; }
    public synchronized long revision() { return revision; }
    public synchronized int liveCaptures() { return liveCaptures; }

    private boolean validState(byte[] state) {
        return state != null && state.length <= maxSnapshotBytes;
    }

    /** Independent immutable capture. Release only after no engaged transaction depends on it. */
    public static final class Capture implements AutoCloseable {
        private final MusleeRevisionFence owner;
        private final Object generation;
        private final long revision;
        private byte[] state;
        private boolean released;

        private Capture(MusleeRevisionFence owner, Object generation, long revision, byte[] state) {
            this.owner = owner;
            this.generation = generation;
            this.revision = revision;
            this.state = state;
        }

        public long revision() { return revision; }
        public byte[] snapshot() {
            synchronized (owner) {
                if (released) {
                    throw new IllegalStateException("Capture has been released");
                }
                return state.clone();
            }
        }

        /** Idempotent release invalidates this ticket permanently, including across resets. */
        @Override
        public void close() {
            synchronized (owner) {
                if (!released) {
                    released = true;
                    Arrays.fill(state, (byte) 0);
                    state = null;
                    owner.liveCaptures--;
                }
            }
        }
    }
}
