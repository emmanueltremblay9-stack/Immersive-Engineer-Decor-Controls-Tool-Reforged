package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Inactive, read-only composition of existing revision fences for a supplied read set.
 * MATCHES_ONLY is never permission to reserve, enqueue, write, press or commit.
 * The caller must prove the read set is complete, capture canonical ItemStack state
 * and hold the ONE authoritative transaction lock through finalization. This
 * helper neither provides that lock nor tracks live Minecraft mutation origins.
 */
public final class MusleeReadSetGate {
    public enum Status {
        MATCHES_ONLY,
        INVALID_OR_INCOMPLETE,
        DUPLICATE_LOCATION,
        STALE_OR_UNPROVABLE
    }

    /**
     * A detached observation of ONE authoritative location at ONE caller-chosen instant.
     * It cannot establish that this was an exhaustive or recent inventory snapshot.
     */
    public static final class Observation {
        private final MusleeRevisionFence location;
        private final MusleeRevisionFence.Capture captured;
        private final byte[] currentCanonical;

        private Observation(MusleeRevisionFence location,
                            MusleeRevisionFence.Capture captured, byte[] currentCanonical) {
            this.location = location;
            this.captured = captured;
            this.currentCanonical = currentCanonical.clone();
        }

        /** Copies caller-owned bytes before any later mutation can affect this observation. */
        public static Optional<Observation> of(MusleeRevisionFence location,
                                               MusleeRevisionFence.Capture captured,
                                               byte[] currentCanonical) {
            if (location == null || captured == null || currentCanonical == null)
                return Optional.empty();
            return Optional.of(new Observation(location, captured, currentCanonical));
        }
    }

    private MusleeReadSetGate() {}

    /**
     * Snapshot the membership, reject any repeated LOCATION identity before touching
     * fences, then compare each capture and its current complete canonical bytes.
     * An untracked external state difference invalidates only that location's fence.
     *
     * The host MUST hold its existing authoritative lock while obtaining the
     * observations, calling this method, preflighting all affected write fences,
     * performing the atomic inventory/reserve commit and recording its outcome.
     * A positive result is only a necessary read-set condition, never a grant.
     */
    public static Status compareOnly(List<Observation> supplied) {
        if (supplied == null || supplied.isEmpty()) return Status.INVALID_OR_INCOMPLETE;
        final List<Observation> observations;
        try { observations = List.copyOf(supplied); }
        catch (RuntimeException invalid) { return Status.INVALID_OR_INCOMPLETE; }

        Set<MusleeRevisionFence> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Observation observation : observations) {
            if (observation == null || observation.location == null
                    || observation.captured == null || observation.currentCanonical == null)
                return Status.INVALID_OR_INCOMPLETE;
            if (!seen.add(observation.location)) return Status.DUPLICATE_LOCATION;
        }

        try {
            for (Observation observation : observations) {
                if (!observation.location.matches(observation.captured,
                        observation.currentCanonical)) return Status.STALE_OR_UNPROVABLE;
            }
        } catch (RuntimeException uncertain) {
            return Status.STALE_OR_UNPROVABLE;
        }
        return Status.MATCHES_ONLY;
    }
}
