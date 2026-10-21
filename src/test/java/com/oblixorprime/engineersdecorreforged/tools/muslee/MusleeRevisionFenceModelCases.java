package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Deterministic model-based regression of the ACTUAL merged revision-fence primitive.
 * No Minecraft, inventory writes, fake production classes, or runtime registration.
 */
public final class MusleeRevisionFenceModelCases {
    private static final int MAX_CAPTURES = 8;
    private static final int MAX_STATE_BYTES = 12;
    private static final int SEEDS = 16;
    private static final int STEPS_PER_SEED = 4000;
    private static final int LOCATIONS = 3;

    private MusleeRevisionFenceModelCases() {}

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Ticket {
        final MusleeRevisionFence.Capture capture;
        final long revision;
        final byte[] canonical;

        Ticket(MusleeRevisionFence.Capture capture, long revision, byte[] canonical) {
            this.capture = capture;
            this.revision = revision;
            this.canonical = canonical.clone();
        }
    }

    private static final class Oracle {
        final MusleeRevisionFence fence = new MusleeRevisionFence(MAX_CAPTURES, MAX_STATE_BYTES);
        final List<Ticket> open = new ArrayList<>();
        byte[] state;
        long revision;
        boolean usable;

        Oracle(byte[] initial) {
            check(fence.restoreVerified(initial, true), "initial verified restoration");
            state = initial.clone();
            usable = true;
        }

        void invariants() {
            check(fence.isUsable() == usable, "usable flag diverged");
            check(fence.revision() == revision, "revision diverged");
            check(fence.liveCaptures() == open.size(), "live capture count diverged");
            check(open.size() <= MAX_CAPTURES, "capture capacity exceeded");
        }

        void takeCapture() {
            var captured = fence.capture();
            check(captured.isPresent() == (usable && open.size() < MAX_CAPTURES),
                  "capture gate mismatched oracle");
            if (captured.isPresent()) {
                var c = captured.orElseThrow();
                check(c.revision() == revision, "capture revision differs");
                check(Arrays.equals(c.snapshot(), state), "snapshot detached content differs");
                open.add(new Ticket(c, revision, state));
            }
        }

        void release(Random random) {
            if (open.isEmpty()) return;
            Ticket old = open.remove(random.nextInt(open.size()));
            byte[] independentCopy = old.capture.snapshot();
            check(Arrays.equals(independentCopy, old.canonical), "stored capture mutated");
            old.capture.close();
            old.capture.close();
            check(fence.liveCaptures() == open.size(), "idempotent close counted twice");
            try {
                old.capture.snapshot();
                throw new AssertionError("released capture remains readable");
            } catch (IllegalStateException expected) {
                // A released snapshot is not readable or reusable.
            }
            check(!fence.matches(old.capture, state), "released capture matched");
        }

        void commit(Random random, boolean change) {
            byte[] proposed = state.clone();
            if (change) proposed[random.nextInt(proposed.length)] ^= (byte) (1 + random.nextInt(127));
            if (!usable) {
                check(!fence.preflightForCommit(proposed), "unusable location preflight succeeded");
                return;
            }
            check(fence.preflightForCommit(proposed), "valid preflight failed");
            check(fence.revision() == revision, "preflight advanced revision");
            var actual = fence.recordTransaction(true, proposed);
            check(actual == (change ? MusleeRevisionFence.Change.CHANGED : MusleeRevisionFence.Change.UNCHANGED),
                  "commit status differed from complete-state equality");
            if (change) revision++;
            state = proposed.clone();
            // The submitted byte array cannot remain an alias of the authoritative snapshot.
            proposed[0] ^= (byte) 0x5a;
            var newlyCaptured = fence.capture();
            if (newlyCaptured.isPresent()) {
                check(Arrays.equals(newlyCaptured.orElseThrow().snapshot(), state),
                      "retained authoritative array aliases input");
                newlyCaptured.orElseThrow().close();
            }
        }

        void compare(Random random) {
            if (open.isEmpty()) return;
            Ticket t = open.get(random.nextInt(open.size()));
            boolean valid = usable && t.revision == revision && Arrays.equals(t.canonical, state);
            check(Arrays.equals(t.capture.snapshot(), t.canonical), "captured bytes changed");
            check(fence.matches(t.capture, state.clone()) == valid, "anti-ABA match diverged");
        }

        void divergentAuthority(Random random) {
            if (!usable || open.isEmpty()) return;
            byte[] impossible = state.clone();
            impossible[random.nextInt(impossible.length)] ^= (byte) 1;
            check(!fence.matches(open.get(0).capture, impossible), "untracked mutation not rejected");
            usable = false;
            check(!fence.capture().isPresent(), "capture after uncertainty");
        }

        void invalidate() {
            fence.invalidateTracking();
            usable = false;
        }

        void reset(Random random) {
            byte[] desired = new byte[1 + random.nextInt(MAX_STATE_BYTES)];
            random.nextBytes(desired);
            boolean trackingProven = random.nextInt(5) != 0;
            boolean shouldRestore = trackingProven && open.isEmpty();
            check(fence.restoreVerified(desired, trackingProven) == shouldRestore,
                  "old-reference exclusion or proof was bypassed");
            usable = shouldRestore;
            if (shouldRestore) {
                revision = 0;
                state = desired.clone();
            }
            desired[0] ^= (byte) 0x5a;
        }

        void invalidPreflight() {
            boolean worked = fence.preflightForCommit(null);
            check(!worked, "null final-state preflight accepted");
            usable = false;
        }

        void foreignCapture() {
            MusleeRevisionFence other = new MusleeRevisionFence(1, MAX_STATE_BYTES);
            check(other.restoreVerified(new byte[] {0x2b}, true), "foreign fixture restore failed");
            var foreign = other.capture().orElseThrow();
            boolean before = usable;
            check(!fence.matches(foreign, state), "foreign location capture was accepted");
            check(fence.isUsable() == before, "foreign capture poisoned local location");
            foreign.close();
        }

        void releaseAllAndRestore() {
            for (Ticket ticket : open) {
                check(Arrays.equals(ticket.capture.snapshot(), ticket.canonical), "engaged-like capture not retained");
                ticket.capture.close();
            }
            open.clear();
            check(fence.liveCaptures() == 0, "final outstanding captures");
            byte[] end = new byte[] {0x12, 0x34, 0x56};
            check(fence.restoreVerified(end, true), "fresh manual operations could not resume");
            usable = true;
            revision = 0;
            state = end;
            Ticket fresh = new Ticket(fence.capture().orElseThrow(), revision, state);
            check(fence.matches(fresh.capture, end), "new generation capture invalid");
            fresh.capture.close();
        }
    }

    public static void run() {
        long operations = 0;
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(0x1368_0331L + seed * 104729L);
            Oracle[] locations = new Oracle[LOCATIONS];
            for (int i = 0; i < LOCATIONS; i++) {
                byte[] initial = new byte[] {(byte) i, (byte) (seed + 1), 0x22};
                locations[i] = new Oracle(initial);
            }
            for (int step = 0; step < STEPS_PER_SEED; step++) {
                Oracle target = locations[random.nextInt(LOCATIONS)];
                int action = random.nextInt(100);
                if (action < 20) target.takeCapture();
                else if (action < 37) target.release(random);
                else if (action < 57) target.commit(random, action % 4 != 0);
                else if (action < 65) {
                    check(target.fence.recordTransaction(false, null) == MusleeRevisionFence.Change.REJECTED,
                          "rejected transaction advanced revision");
                }
                else if (action < 76) target.compare(random);
                else if (action < 82) target.divergentAuthority(random);
                else if (action < 87) target.invalidate();
                else if (action < 94) target.reset(random);
                else if (action < 97) target.invalidPreflight();
                else target.foreignCapture();
                target.invariants();
                // A failed local location must not change the other two trackers.
                if (step % 47 == 0) for (Oracle location : locations) location.invariants();
                operations++;
            }
            for (Oracle location : locations) {
                location.releaseAllAndRestore();
                location.invariants();
            }
        }
        check(operations == (long) SEEDS * STEPS_PER_SEED, "wrong randomized coverage count");
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASS: 64,000 deterministic revision-fence transitions over 3 independent locations");
    }
}
