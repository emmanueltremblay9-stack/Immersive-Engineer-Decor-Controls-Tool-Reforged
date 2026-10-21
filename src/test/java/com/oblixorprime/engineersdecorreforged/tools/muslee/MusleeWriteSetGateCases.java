package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeWriteSetGate.*;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeWriteSetGate.Status.*;

/** Exact production-fence execution; synthetic bytes do not prove Minecraft codec safety. */
public final class MusleeWriteSetGateCases {
    private MusleeWriteSetGateCases() {}
    private static void check(boolean yes, String reason) {
        if (!yes) throw new AssertionError(reason);
    }
    private static MusleeRevisionFence newFence(int limit, byte[] start) {
        MusleeRevisionFence f = new MusleeRevisionFence(4, limit);
        check(f.restoreVerified(start, true), "fixture restore");
        return f;
    }
    private static Proposed p(MusleeRevisionFence fence, byte[] bytes) {
        return Proposed.of(fence, bytes).orElseThrow();
    }
    public static void run() {
        basicWriteSetIsReadOnly();
        malformedAndRepeatedLocationsAreInert();
        detachedProposalsStayDetached();
        failingStateInvalidatesOnlyTheTarget();
        overflowPreflightRefusesBeforeAnyWrite();
        deterministicThreeLocationScenarios();
    }
    private static void basicWriteSetIsReadOnly() {
        byte[] original = {1, 2};
        MusleeRevisionFence a = newFence(4, original), b = newFence(4, original), c = newFence(4, original);
        var ca = a.capture().orElseThrow();
        var cb = b.capture().orElseThrow();
        var cc = c.capture().orElseThrow();
        List<Proposed> entries = new ArrayList<>(List.of(p(a, new byte[]{3, 4}),
                p(b, new byte[]{5}), p(c, original)));
        check(preflightOnly(entries) == PREFLIGHT_ONLY, "all distinct write locations pass preflight");
        entries.clear();
        check(preflightOnly(List.of(p(a, original))) == PREFLIGHT_ONLY, "membership snapshot independent");
        check(a.revision() == 0 && b.revision() == 0 && c.revision() == 0,
                "preflight must not advance any revision");
        check(a.matches(ca, original) && b.matches(cb, original) && c.matches(cc, original),
                "preflight does not change current authoritative state");
        ca.close(); cb.close(); cc.close();
    }
    private static void malformedAndRepeatedLocationsAreInert() {
        byte[] initial = {1};
        MusleeRevisionFence a = newFence(1, initial), b = newFence(1, initial);
        check(preflightOnly(null) == INVALID_OR_INCOMPLETE, "null set");
        check(preflightOnly(List.of()) == INVALID_OR_INCOMPLETE, "empty set");
        List<Proposed> withNull = new ArrayList<>();
        withNull.add(p(a, initial)); withNull.add(null);
        check(preflightOnly(withNull) == INVALID_OR_INCOMPLETE, "null entry");
        check(Proposed.of(null, initial).isEmpty() && Proposed.of(a, null).isEmpty(),
                "invalid proposal constructor");
        check(Proposed.of(a, new byte[MAX_CANONICAL_BYTES + 1]).isEmpty(),
                "oversized canonical bytes rejected before cloning");
        check(a.isUsable(), "oversized proposal does not invalidate a location");
        List<Proposed> tooMany = new ArrayList<>();
        for (int i = 0; i <= MAX_LOCATIONS; i++) tooMany.add(p(a, initial));
        check(preflightOnly(tooMany) == INVALID_OR_INCOMPLETE,
                "excessive location count rejected before comparisons");
        // Duplicate check MUST precede the oversized proposal's preflight.
        check(preflightOnly(List.of(p(a, new byte[]{1, 2}), p(a, initial))) == DUPLICATE_LOCATION,
                "duplicate by fence identity, not canonical bytes");
        check(a.isUsable() && b.isUsable() && a.revision() == 0, "malformed set did not touch fences");
        check(preflightOnly(List.of(p(a, initial), p(b, initial))) == PREFLIGHT_ONLY,
                "distinct fence identities accepted");
    }
    private static void detachedProposalsStayDetached() {
        // At MAX revision even a one-byte change would disable tracking. A detached
        // proposal must still preflight as a true no-op after caller mutation.
        MusleeRevisionFence maximum = new MusleeRevisionFence(4, 2,
                Long.MAX_VALUE, new byte[]{1});
        byte[] input = {1};
        Proposed detached = p(maximum, input);
        input[0] = 2;
        check(preflightOnly(List.of(detached)) == PREFLIGHT_ONLY,
                "proposed byte snapshot must not alias caller input");
        check(maximum.isUsable() && maximum.revision() == Long.MAX_VALUE,
                "caller input mutation cannot falsely exhaust revision fence");
        check(preflightOnly(List.of(p(maximum, input))) == REFUSED_OR_UNUSABLE,
                "newly sampled changed bytes correctly trigger overflow fence");
        check(!maximum.isUsable(), "counter exhaustion is targeted");
    }
    private static void failingStateInvalidatesOnlyTheTarget() {
        MusleeRevisionFence a = newFence(2, new byte[]{1}), b = newFence(2, new byte[]{2});
        var saved = a.capture().orElseThrow();
        Status result = preflightOnly(List.of(p(a, new byte[]{1}), p(b, new byte[]{3, 4, 5})));
        check(result == REFUSED_OR_UNUSABLE, "oversize final bytes refused");
        check(a.isUsable() && !b.isUsable(), "only affected fence became unusable");
        check(a.matches(saved, new byte[]{1}), "earlier preflight has no side effect");
        check(a.revision() == 0 && b.revision() == 0, "failure must not alter revision counters");
        saved.close();
    }
    private static void overflowPreflightRefusesBeforeAnyWrite() {
        MusleeRevisionFence atLimit = new MusleeRevisionFence(4, 2, Long.MAX_VALUE, new byte[]{1});
        MusleeRevisionFence independent = newFence(2, new byte[]{9});
        var old = atLimit.capture().orElseThrow();
        check(preflightOnly(List.of(p(independent, new byte[]{9}), p(atLimit, new byte[]{1})))
                == PREFLIGHT_ONLY, "unchanged state at max revision safe");
        check(preflightOnly(List.of(p(independent, new byte[]{8}), p(atLimit, new byte[]{2})))
                == REFUSED_OR_UNUSABLE, "would overflow revision counter");
        check(!atLimit.isUsable() && independent.isUsable(), "only exhausted counter fenced off");
        check(atLimit.revision() == Long.MAX_VALUE && independent.revision() == 0,
                "preflight never increments existing counters");
        check(old.snapshot()[0] == 1, "retained capture survives unusable tracking");
        old.close();
    }
    private static void deterministicThreeLocationScenarios() {
        Random random = new Random(0x51A7_35FEL);
        for (int iteration = 0; iteration < 12000; iteration++) {
            MusleeRevisionFence[] fences = new MusleeRevisionFence[3];
            List<Proposed> proposed = new ArrayList<>();
            int failing = random.nextInt(7) == 0 ? random.nextInt(3) : -1;
            for (int i = 0; i < 3; i++) {
                byte[] first = {(byte)i, 1};
                fences[i] = newFence(4, first);
                byte[] ending = {(byte) random.nextInt(128), 2, 3};
                if (i == failing) ending = new byte[]{1, 2, 3, 4, 5};
                proposed.add(p(fences[i], ending));
            }
            check(preflightOnly(proposed) == (failing == -1 ? PREFLIGHT_ONLY : REFUSED_OR_UNUSABLE),
                    "three-location preflight mismatch at iteration " + iteration);
            for (int i = 0; i < 3; i++) {
                check(fences[i].revision() == 0, "preflight advanced a revision");
                check(fences[i].isUsable() == (i != failing),
                        "failed preflight touched unrelated or later location");
            }
        }
    }
    public static void main(String[] args) {
        run();
        System.out.println("PASS: 12000 three-location preflight trials plus guarded failure cases");
    }
}
