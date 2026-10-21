package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeCommandQueue.*;

/** Integration carrier only: PR80 targeted invalidation + PR82 actual capture ownership. */
public final class MusleeCrossPrOwnedCaptureCases {
    private static final Validator RETAIN = t -> Verdict.ENGAGE_RETAIN_CAPTURE;
    private static final Validator REJECT = t -> Verdict.REJECT;
    private static void check(boolean ok) { if (!ok) throw new AssertionError("Muslee combined capture lifecycle"); }
    private static final class Clock implements MusleeCommandQueue.Clock {
        long now;
        public long nanoTime() { return now; }
    }
    private static MusleeCommandQueue queue() { return new MusleeCommandQueue(UUID.randomUUID(), new Clock()); }
    private static MusleeItemStackCapture cap(int id) {
        return MusleeItemStackCapture.fromEncodedForOwnershipTest(new byte[]{(byte) id, 2, 3});
    }
    private static Ticket add(MusleeCommandQueue q, UUID session, MusleeItemStackCapture cap, long bytes) {
        var admitted = q.admitCaptureForOwnershipTest(UUID.randomUUID(), session, true, bytes, cap);
        check(admitted.status == AdmissionStatus.ADMITTED && admitted.ticket != null);
        return admitted.ticket;
    }
    private static void released(MusleeItemStackCapture bytes) {
        check(bytes.isClosed() && bytes.releaseCount() == 1);
    }
    public static void run() throws Exception {
        preserveActiveAndSameSessionFifo();
        foreignAndPublicAccessRemainBlocked();
        concurrentInvalidateOrEngageExactlyOnce();
    }
    private static void preserveActiveAndSameSessionFifo() {
        MusleeCommandQueue q = queue(); UUID session = UUID.randomUUID();
        var a = cap(1); var b = cap(2); var c = cap(3); var d = cap(4);
        Ticket active = add(q, session, a, 100);
        check(q.runScheduled(1, RETAIN).engaged == active);
        Ticket first = add(q, session, b, 1);
        Ticket middle = add(q, session, c, 2);
        Ticket last = add(q, session, d, 3);
        check(q.reservedBytes() == 6);
        check(q.invalidateWaiting(middle) && !q.invalidateWaiting(middle));
        released(c);
        check(middle.state() == State.INVALIDATED && middle.releaseCount() == 1);
        check(q.reservedBytes() == 4 && q.waitingCount() == 2);
        check(!q.invalidateWaiting(active) && !a.isClosed());
        check(q.complete(active, false, 2, REJECT).engaged == null && !a.isClosed());
        check(q.complete(active, true, 2, RETAIN).engaged == first);
        released(a);
        check(!b.isClosed() && first.owner() == Owner.TRANSACTION);
        check(q.complete(first, true, 3, RETAIN).engaged == last);
        released(b);
        check(q.complete(last, true, 4, REJECT).engaged == null);
        released(d);
        check(q.active() == null && q.waitingCount() == 0 && q.reservedBytes() == 0);
    }
    private static void foreignAndPublicAccessRemainBlocked() {
        MusleeCommandQueue q = queue(), foreign = queue(); UUID session = UUID.randomUUID();
        var owned = cap(5); var other = cap(6); var candidate = cap(7);
        Ticket t = add(q, session, owned, MAX_CAPTURE_BYTES);
        Ticket f = add(foreign, session, other, 1);
        check(!q.invalidateWaiting(null) && !q.invalidateWaiting(f));
        check(!owned.isClosed() && !other.isClosed());
        var refusal = q.admitCapture(UUID.randomUUID(), session, true, candidate);
        check(refusal.status == AdmissionStatus.INVALID_BOUND && refusal.ticket == null);
        released(candidate);
        check(!owned.isClosed() && q.reservedBytes() == MAX_CAPTURE_BYTES);
        check(q.invalidateWaiting(t) && !q.invalidateWaiting(t));
        released(owned);
        check(!q.invalidateWaiting(f) && !other.isClosed());
        check(foreign.invalidateWaiting(f));
        released(other);
    }
    private static void concurrentInvalidateOrEngageExactlyOnce() throws Exception {
        for (int i = 0; i < 128; i++) {
            MusleeCommandQueue q = queue(); UUID session = UUID.randomUUID();
            var captured = cap(i + 100);
            Ticket ticket = add(q, session, captured, 1);
            CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread invalidator = new Thread(() -> {
                ready.countDown();
                try {
                    if (!go.await(10, TimeUnit.SECONDS)) throw new AssertionError("timeout");
                    q.invalidateWaiting(ticket);
                } catch (Throwable ex) { failure.compareAndSet(null, ex); }
            });
            Thread engager = new Thread(() -> {
                ready.countDown();
                try {
                    if (!go.await(10, TimeUnit.SECONDS)) throw new AssertionError("timeout");
                    q.runScheduled(1, RETAIN);
                } catch (Throwable ex) { failure.compareAndSet(null, ex); }
            });
            invalidator.start(); engager.start();
            try {
                check(ready.await(10, TimeUnit.SECONDS)); go.countDown();
                invalidator.join(10000); engager.join(10000);
                check(!invalidator.isAlive() && !engager.isAlive() && failure.get() == null);
                check(ticket.state() == State.INVALIDATED || ticket.state() == State.ENGAGED);
                if (ticket.state() == State.ENGAGED) {
                    check(q.active() == ticket && !captured.isClosed());
                    check(!q.invalidateWaiting(ticket));
                    q.complete(ticket, false, 2, REJECT);
                    check(!captured.isClosed());
                    q.complete(ticket, true, 2, REJECT);
                }
                released(captured);
                check(ticket.releaseCount() == 1 && q.waitingCount() == 0 && q.reservedBytes() == 0);
            } finally {
                go.countDown();
                if (invalidator.isAlive()) invalidator.interrupt();
                if (engager.isAlive()) engager.interrupt();
            }
        }
    }
    public static void main(String[] args) throws Exception {
        run(); System.out.println("PASS: PR80+PR82 real-owner API composition, 128 race trials");
    }
}
