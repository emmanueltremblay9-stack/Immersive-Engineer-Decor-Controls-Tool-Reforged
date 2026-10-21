package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeCommandQueue.*;

/** In-process tests of actual queue/fence primitives; not a live inventory adapter. */
public final class MusleeTargetedInvalidationCases {
    private static final UUID SESSION = new UUID(0x4000L, Long.MIN_VALUE);
    private static final Validator RETAIN = t -> Verdict.ENGAGE_RETAIN_CAPTURE;
    private static final Validator REJECT = t -> Verdict.REJECT;
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static final class ClockStub implements Clock {
        long now;
        public long nanoTime() { return now; }
    }
    private static Ticket add(MusleeCommandQueue q, long bytes) {
        Admission a = q.admit(UUID.randomUUID(), SESSION, true, bytes);
        check(a.status == AdmissionStatus.ADMITTED, "fixture admission");
        return a.ticket;
    }
    private static MusleeCommandQueue queue() { return new MusleeCommandQueue(UUID.randomUUID(), new ClockStub()); }

    public static void run() throws Exception {
        everyPositionPreservesIndependentWork();
        invalidReferencesAreInert();
        lastRemovalClearsContinuation();
        validationCannotMutateWaiting();
        dependencyInvalidationKeepsEngagedCapture();
        wholeDependencySetUnderCommonLock();
        concurrentRemovalReleasesOnce();
        removalRacingEngagementHasOneWinner();
    }

    private static void everyPositionPreservesIndependentWork() {
        for (int position = 0; position < MAX_WAITING; position++) {
            MusleeCommandQueue q = queue();
            Ticket active = add(q, 100);
            check(q.runScheduled(1, RETAIN).engaged == active, "active fixture");
            List<Ticket> pending = new ArrayList<>();
            for (int i = 0; i < MAX_WAITING; i++) pending.add(add(q, i + 1));
            Ticket target = pending.get(position);
            check(q.invalidateWaiting(target), "waiting target removed");
            check(!q.invalidateWaiting(target), "repeated removal inert");
            check(target.state() == State.INVALIDATED && target.releaseCount() == 1, "target terminal once");
            check(q.reservedBytes() == 528 - position - 1 && q.waitingCount() == 31, "exact ledger delta");
            check(q.active() == active && active.owner() == Owner.TRANSACTION, "active lease preserved");
            List<Ticket> survivors = new ArrayList<>(pending); survivors.remove(position);
            Pass pass = q.complete(active, true, 2, RETAIN);
            for (Ticket survivor : survivors) {
                check(pass.engaged == survivor, "relative FIFO survives middle removal");
                pass = q.complete(survivor, true, 2, RETAIN);
                check(survivor.releaseCount() == 1 && survivor.state() == State.COMPLETED, "one completion release");
            }
            check(q.active() == null && q.waitingCount() == 0 && q.reservedBytes() == 0, "no retained ledger");
        }
    }

    private static void invalidReferencesAreInert() {
        MusleeCommandQueue q = queue(), other = queue();
        Ticket active = add(q, 5), pending = add(q, 7), foreign = add(other, 9);
        q.runScheduled(1, RETAIN);
        check(!q.invalidateWaiting(null) && !q.invalidateWaiting(foreign), "null/foreign rejected");
        check(!q.invalidateWaiting(active), "engaged lease cannot be invalidated");
        check(q.waitingCount() == 1 && q.reservedBytes() == 7 && other.reservedBytes() == 9, "no collateral changes");
        q.complete(active, true, 2, REJECT);
        check(!q.invalidateWaiting(active) && !q.invalidateWaiting(pending), "completed/rejected references inert");
        check(active.releaseCount() == 1 && pending.releaseCount() == 1, "terminal releases unchanged");
        ClockStub clock = new ClockStub();
        MusleeCommandQueue expiring = new MusleeCommandQueue(UUID.randomUUID(), clock);
        Ticket expired = add(expiring, 3); clock.now = MAX_WAIT_NANOS;
        expiring.runScheduled(1, RETAIN);
        check(expired.state() == State.EXPIRED && !expiring.invalidateWaiting(expired), "expired reference inert");
        check(expired.releaseCount() == 1, "expiry release not repeated");
    }

    private static void lastRemovalClearsContinuation() {
        MusleeCommandQueue q = queue(); Ticket only = add(q, MAX_CAPTURE_BYTES);
        check(q.continuationPending(), "pending before invalidation");
        check(q.invalidateWaiting(only), "only entry removed");
        check(!q.continuationPending() && !q.resumeDemanded() && q.reservedBytes() == 0, "empty queue unscheduled");
        Ticket fresh = add(q, MAX_CAPTURE_BYTES);
        check(q.runScheduled(1, RETAIN).engaged == fresh, "fresh work uses released budget");
    }

    private static void validationCannotMutateWaiting() {
        MusleeCommandQueue q = queue(); Ticket first = add(q, 2), second = add(q, 3);
        AtomicInteger attempts = new AtomicInteger();
        Pass result = q.runScheduled(1, t -> {
            if (t == first) { attempts.incrementAndGet(); q.invalidateWaiting(second); }
            return Verdict.ENGAGE_RETAIN_CAPTURE;
        });
        check(attempts.get() == 1 && first.state() == State.REJECTED, "invalid reentry fails validation");
        check(result.engaged == second && second.releaseCount() == 0, "pending structure not mutated by callback");
    }

    private static void dependencyInvalidationKeepsEngagedCapture() {
        MusleeCommandQueue q = queue();
        MusleeRevisionFence affected = new MusleeRevisionFence(8, 16);
        MusleeRevisionFence independent = new MusleeRevisionFence(8, 16);
        byte[] a = {1}, b = {2};
        check(affected.restoreVerified(a, true) && independent.restoreVerified(b, true), "verified fixture states");
        var engagedCapture = affected.capture().orElseThrow(); Ticket active = add(q, 16);
        q.runScheduled(1, RETAIN);
        var oldOne = affected.capture().orElseThrow(); Ticket invalidOne = add(q, 16);
        var good = independent.capture().orElseThrow(); Ticket survivor = add(q, 16);
        var oldTwo = affected.capture().orElseThrow(); Ticket invalidTwo = add(q, 16);
        synchronized (q) { // Stand-in for the host's common authoritative serialization domain.
            affected.invalidateTracking();
            check(q.invalidateWaiting(invalidOne) && q.invalidateWaiting(invalidTwo), "all and only affected waiting tickets");
            oldOne.close(); oldTwo.close();
        }
        check(q.active() == active && q.waitingCount() == 1 && q.reservedBytes() == 16, "engaged/independent work preserved");
        check(engagedCapture.snapshot()[0] == 1 && affected.liveCaptures() == 1, "engaged bytes remain readable");
        check(!affected.restoreVerified(a, true), "old engaged reference prevents generation reset");
        check(independent.matches(good, b), "other fence remains usable");
        check(q.complete(active, true, 2, t -> independent.matches(good, b)
                ? Verdict.ENGAGE_RETAIN_CAPTURE : Verdict.REJECT).engaged == survivor, "independent work resumes");
        engagedCapture.close();
        check(affected.restoreVerified(a, true), "reset only after old references excluded");
        check(!affected.matches(oldOne, a) && !q.invalidateWaiting(invalidOne), "old capture/ticket cannot revive");
        q.complete(survivor, true, 2, REJECT); good.close(); good.close();
        check(affected.liveCaptures() == 0 && independent.liveCaptures() == 0, "actual byte captures released once");
    }

    private static void wholeDependencySetUnderCommonLock() {
        MusleeCommandQueue q = queue(); Ticket active = add(q, 1);
        q.runScheduled(1, RETAIN); List<Ticket> all = new ArrayList<>();
        for (int i = 0; i < MAX_WAITING; i++) all.add(add(q, 8192));
        synchronized (q) { for (Ticket t : all) check(q.invalidateWaiting(t), "mandatory group invalidation"); }
        check(q.active() == active && q.waitingCount() == 0 && q.reservedBytes() == 0, "group ignores expiry-pass budget");
        for (Ticket t : all) check(t.releaseCount() == 1 && t.owner() == Owner.RELEASED, "group releases unique");
    }

    private static void concurrentRemovalReleasesOnce() throws Exception {
        MusleeCommandQueue q = queue(); Ticket target = add(q, 41), survivor = add(q, 7);
        var pool = Executors.newFixedThreadPool(8); CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int i = 0; i < 8; i++) results.add(pool.submit(() -> {
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                int removed = 0; for (int j = 0; j < 100; j++) if (q.invalidateWaiting(target)) removed++;
                return removed;
            }));
            start.countDown(); int removed = 0;
            for (Future<Integer> result : results) removed += result.get(10, TimeUnit.SECONDS);
            check(removed == 1 && target.releaseCount() == 1, "one winner among 800 attempts");
            check(q.waitingCount() == 1 && q.reservedBytes() == 7 && q.runScheduled(1, RETAIN).engaged == survivor,
                    "concurrent invalidations preserve survivor");
        } finally { start.countDown(); pool.shutdownNow(); check(pool.awaitTermination(10, TimeUnit.SECONDS), "workers stopped"); }
    }

    private static void removalRacingEngagementHasOneWinner() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 128; round++) {
                MusleeCommandQueue q = queue(); Ticket first = add(q, 3), second = add(q, 9);
                CountDownLatch start = new CountDownLatch(1);
                Future<Boolean> removal = pool.submit(() -> { check(start.await(10, TimeUnit.SECONDS), "start"); return q.invalidateWaiting(first); });
                Future<Pass> engagement = pool.submit(() -> { check(start.await(10, TimeUnit.SECONDS), "start"); return q.runScheduled(1, RETAIN); });
                start.countDown(); boolean removed = removal.get(10, TimeUnit.SECONDS); Pass pass = engagement.get(10, TimeUnit.SECONDS);
                if (removed) {
                    check(first.state() == State.INVALIDATED && first.releaseCount() == 1 && pass.engaged == second, "invalidation wins safely");
                } else {
                    check(first.state() == State.ENGAGED && first.releaseCount() == 0 && pass.engaged == first, "engagement wins safely");
                    check(q.complete(first, true, 2, RETAIN).engaged == second, "second resumes only on completion");
                }
                q.complete(second, true, 2, REJECT);
                check(q.active() == null && q.waitingCount() == 0 && q.reservedBytes() == 0, "race retains no ledger");
            }
        } finally { pool.shutdownNow(); check(pool.awaitTermination(10, TimeUnit.SECONDS), "race workers stopped"); }
    }
    public static void main(String[] args) throws Exception {
        run(); System.out.println("PASS targeted invalidation: 8 scenarios, 32 FIFO positions, 800 duplicate removals, 128 engagement races");
    }
}
