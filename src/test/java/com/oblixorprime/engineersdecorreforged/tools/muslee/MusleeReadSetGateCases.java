package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeReadSetGate.Status.*;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeReadSetGate.*;

/** Deterministic in-process tests against the exact production MusleeRevisionFence. */
public final class MusleeReadSetGateCases {
    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
    private static final class Slot implements AutoCloseable {
        final MusleeRevisionFence fence = new MusleeRevisionFence(2, 128);
        final MusleeRevisionFence.Capture captured;
        final byte[] original;
        Slot(int i) {
            original = new byte[] {(byte) i, 1, 2, 3};
            check(fence.restoreVerified(original, true), "fixture restoration");
            captured = fence.capture().orElseThrow();
        }
        Observation observed(byte[] now) {
            return Observation.of(fence, captured, now).orElseThrow();
        }
        @Override public void close() { captured.close(); }
    }
    private MusleeReadSetGateCases() {}

    public static void run() {
        basicReadSetAndDetachedInputs();
        malformedAndDuplicateReadSets();
        staleAbaAndUntrackedMutation();
        releasedAndForeignCaptures();
        repeatableIndependentLocations();
    }

    private static void basicReadSetAndDetachedInputs() {
        try (Slot source = new Slot(1); Slot reserve = new Slot(2); Slot destination = new Slot(3)) {
            byte[] fromInventory = source.original.clone();
            Observation a = source.observed(fromInventory);
            fromInventory[0] = 100;
            List<Observation> readSet = new ArrayList<>(List.of(a,
                    reserve.observed(reserve.original), destination.observed(destination.original)));
            check(compareOnly(readSet) == MATCHES_ONLY, "original bytes must have been detached");
            readSet.clear();
            check(compareOnly(List.of(a)) == MATCHES_ONLY, "copy of membership isolated");
            check(source.fence.revision() == 0 && reserve.fence.revision() == 0
                    && destination.fence.revision() == 0, "read-only comparison advanced a counter");
            check(source.fence.liveCaptures() == 1 && reserve.fence.liveCaptures() == 1,
                    "read-only check released captured source");
        }
    }

    private static void malformedAndDuplicateReadSets() {
        try (Slot a = new Slot(1); Slot b = new Slot(2)) {
            Observation first = a.observed(a.original);
            check(compareOnly(null) == INVALID_OR_INCOMPLETE, "null set");
            check(compareOnly(List.of()) == INVALID_OR_INCOMPLETE, "empty set");
            List<Observation> containingNull = new ArrayList<>();
            containingNull.add(first); containingNull.add(null);
            check(compareOnly(containingNull) == INVALID_OR_INCOMPLETE, "null member");
            check(Observation.of(null, a.captured, a.original).isEmpty(), "missing fence");
            check(Observation.of(a.fence, null, a.original).isEmpty(), "missing capture");
            check(Observation.of(a.fence, a.captured, null).isEmpty(), "missing authoritative bytes");
            check(compareOnly(List.of(first, a.observed(a.original))) == DUPLICATE_LOCATION,
                    "the same location must not be counted twice");
            check(compareOnly(List.of(b.observed(b.original), first)) == MATCHES_ONLY,
                    "distinct locations accepted without requiring a particular order");
            check(a.fence.isUsable() && b.fence.isUsable(), "input rejection poisoned trackers");
        }
    }

    private static void staleAbaAndUntrackedMutation() {
        try (Slot a = new Slot(1); Slot b = new Slot(2); Slot c = new Slot(3)) {
            check(compareOnly(List.of(a.observed(a.original), b.observed(b.original),
                    c.observed(c.original))) == MATCHES_ONLY, "initial set matches");
            byte[] changed = b.original.clone(); changed[1] ^= 12;
            check(b.fence.preflightForCommit(changed), "fixture preflight");
            check(b.fence.recordTransaction(true, changed) == MusleeRevisionFence.Change.CHANGED,
                    "fixture update");
            check(b.fence.recordTransaction(true, b.original) == MusleeRevisionFence.Change.CHANGED,
                    "fixture restoration");
            check(compareOnly(List.of(a.observed(a.original), b.observed(b.original),
                    c.observed(c.original))) == STALE_OR_UNPROVABLE,
                    "same bytes after ABA must not make stale capture valid");
            check(b.fence.revision() == 2 && b.fence.isUsable(), "ABA counter tracks both changes");
            check(a.fence.isUsable() && c.fence.isUsable(), "ABA must not poison other trackers");
        }
        try (Slot a = new Slot(1); Slot b = new Slot(2)) {
            byte[] untracked = a.original.clone(); untracked[0]++;
            check(compareOnly(List.of(a.observed(untracked), b.observed(b.original)))
                    == STALE_OR_UNPROVABLE, "unknown in-place change must fail closed");
            check(!a.fence.isUsable() && b.fence.isUsable(),
                    "unknown mutation invalidates only its own location");
        }
    }

    private static void releasedAndForeignCaptures() {
        Slot source = new Slot(1);
        try (Slot destination = new Slot(2)) {
            Observation before = source.observed(source.original);
            source.close();
            check(compareOnly(List.of(before)) == STALE_OR_UNPROVABLE,
                    "released token not valid even with matching bytes");
            check(!source.fence.matches(source.captured, source.original), "closed capture stays unusable");
            check(compareOnly(List.of(Observation.of(destination.fence,
                    source.captured, destination.original).orElseThrow())) == STALE_OR_UNPROVABLE,
                    "foreign capture rejected");
            check(destination.fence.isUsable(), "foreign capture must not poison valid location");
        } finally { source.close(); }
    }

    private static void repeatableIndependentLocations() {
        Random random = new Random(0x3a34_6851L);
        for (int round = 0; round < 8192; round++) {
            try (Slot a = new Slot(0); Slot b = new Slot(1); Slot c = new Slot(2)) {
                Slot[] slots = {a, b, c};
                boolean[] changed = new boolean[3];
                byte[][] values = new byte[3][];
                for (int i = 0; i < 3; i++) {
                    values[i] = slots[i].original.clone();
                    if (random.nextInt(4) == 0) {
                        byte[] mutation = values[i].clone();
                        mutation[random.nextInt(4)] ^= 1;
                        check(slots[i].fence.preflightForCommit(mutation), "changed-state preflight");
                        slots[i].fence.recordTransaction(true, mutation);
                        if (random.nextBoolean()) {
                            check(slots[i].fence.preflightForCommit(values[i]), "ABA restoration preflight");
                            slots[i].fence.recordTransaction(true, values[i]); // ABA
                        } else {
                            values[i] = mutation;
                        }
                        changed[i] = true;
                    }
                }
                List<Observation> set = List.of(a.observed(values[0]),
                        b.observed(values[1]), c.observed(values[2]));
                boolean expectedMatch = !changed[0] && !changed[1] && !changed[2];
                check((compareOnly(set) == MATCHES_ONLY) == expectedMatch,
                        "composed read-set result differs from revision oracle at trial " + round);
                for (int i = 0; i < 3; i++) {
                    check(slots[i].fence.revision() == (changed[i]
                            ? Arrays.equals(values[i], slots[i].original) ? 2 : 1 : 0),
                            "per-location revision count diverged");
                }
            }
        }
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASS: 8192 independent 3-location read-set trials plus targeted regressions");
    }
}
