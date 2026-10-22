package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Inactive, necessary write-fence preflight across supplied independent locations.
 * A positive result is ONLY a preliminary fence check; it is never a transaction
 * authorization, lock lease, ItemStack proof, write grant, or command admission.
 *
 * The caller MUST hold one existing server-authoritative transaction lock across
 * a complete read-set check, this preflight, ALL real inventory/reserve writes,
 * and one outcome record per affected location. This class provides no lock,
 * discovers no locations, and cannot roll back or atomically commit those writes.
 */
public final class MusleeWriteSetGate {
    /** Defensive development limits, NOT a qualified 256 KiB retained-heap proof. */
    public static final int MAX_LOCATIONS = 40;
    public static final int MAX_CANONICAL_BYTES = 262144;

    public enum Status { PREFLIGHT_ONLY, INVALID_OR_INCOMPLETE, DUPLICATE_LOCATION, REFUSED_OR_UNUSABLE }

    public static final class Proposed {
        private final MusleeRevisionFence fence;
        private final byte[] finalCanonical;

        private Proposed(MusleeRevisionFence fence, byte[] finalCanonical) {
            this.fence = fence;
            this.finalCanonical = finalCanonical.clone();
        }

        /** Detach the proposed bytes before any caller mutation. */
        public static Optional<Proposed> of(MusleeRevisionFence fence, byte[] finalCanonical) {
            if (fence == null || finalCanonical == null
                    || finalCanonical.length > MAX_CANONICAL_BYTES) return Optional.empty();
            return Optional.of(new Proposed(fence, finalCanonical));
        }
    }

    private MusleeWriteSetGate() {}

    /**
     * Reject malformed or duplicate locations before touching ANY revision fence,
     * then preflight all supplied final complete-state byte encodings. Rejected
     * canonical encodings can invalidate only the affected fence, as specified by
     * MusleeRevisionFence. Already-checked fences never perform actual mutations.
     *
     * PREFLIGHT_ONLY is an observation, not a grant. In particular, later failing
     * fences do not allow earlier positive results to be cached or acted on.
     */
    public static Status preflightOnly(List<Proposed> supplied) {
        if (supplied == null || supplied.isEmpty() || supplied.size() > MAX_LOCATIONS)
            return Status.INVALID_OR_INCOMPLETE;
        final List<Proposed> entries;
        try { entries = List.copyOf(supplied); }
        catch (RuntimeException invalid) { return Status.INVALID_OR_INCOMPLETE; }

        Set<MusleeRevisionFence> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Proposed p : entries) {
            if (p == null || p.fence == null || p.finalCanonical == null)
                return Status.INVALID_OR_INCOMPLETE;
            if (!unique.add(p.fence)) return Status.DUPLICATE_LOCATION;
        }
        for (Proposed p : entries) {
            try {
                if (!p.fence.preflightForCommit(p.finalCanonical.clone()))
                    return Status.REFUSED_OR_UNUSABLE;
            } catch (RuntimeException uncertain) {
                p.fence.invalidateTracking();
                return Status.REFUSED_OR_UNUSABLE;
            }
        }
        return Status.PREFLIGHT_ONLY;
    }
}
