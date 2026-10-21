package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.UUID;

/**
 * Inactive, in-memory FIFO accounting foundation for ONE durable press UUID.
 * No registration, inventory access, network handler, persistence or execution is provided.
 * A ticket represents one indivisible capture lease, NOT an allocated snapshot. The caller
 * must prove its maximum footprint (including nested data, copies and overheads) BEFORE
 * admission; a numeric value here does not establish that proof. Actual capture-store
 * allocation, atomic authoritative validation and transaction integration remain required.
 * All integration must use one authoritative serialization domain, not just this monitor.
 */
public final class MusleeCommandQueue {
    public static final int MAX_WAITING = 32;
    public static final long MAX_CAPTURE_BYTES = 262144L;
    public static final long MAX_WAIT_NANOS = 30_000_000_000L;
    public static final int MAX_EXAMINATIONS = 8;

    /** Must supply a reliable monotonic reading, or throw when unavailable. */
    public interface Clock { long nanoTime(); }
    /** Pure validation only: MUST NOT mutate inventory or perform transaction effects. */
    public interface Validator { Verdict validate(Ticket ticket); }
    public enum Verdict { REJECT, TRANSFER_FAILED, ENGAGE_RETAIN_CAPTURE, ENGAGE_RELEASE_CAPTURE }
    public enum State { WAITING, ENGAGED, REJECTED, EXPIRED, INVALIDATED, COMPLETED }
    public enum Owner { FIFO, TRANSACTION, RELEASED }
    public enum AdmissionStatus { ADMITTED, INVALID_SESSION, INVALID_BOUND, FULL, BUDGET_EXCEEDED, CLOCK_UNCERTAIN, CLEANUP_REQUIRED }

    public static final class Ticket {
        private final MusleeCommandQueue queue;
        private final UUID commandId;
        private final UUID sessionId;
        private final long admittedAt;
        private final long reservedBytes;
        private State state = State.WAITING;
        private Owner owner = Owner.FIFO;
        private int releases;
        private Ticket(MusleeCommandQueue queue, UUID commandId, UUID sessionId, long now, long bytes) {
            this.queue = queue; this.commandId = commandId; this.sessionId = sessionId;
            this.admittedAt = now; this.reservedBytes = bytes;
        }
        public UUID commandId() { return commandId; }
        public UUID sessionId() { return sessionId; }
        public long reservedBytes() { return reservedBytes; }
        public State state() { synchronized (queue) { return state; } }
        public Owner owner() { synchronized (queue) { return owner; } }
        /** Logical release event count, not proof that an external store freed anything. */
        public int releaseCount() { synchronized (queue) { return releases; } }
    }
    public static final class Admission {
        public final AdmissionStatus status;
        public final Ticket ticket;
        private Admission(AdmissionStatus status, Ticket ticket) { this.status = status; this.ticket = ticket; }
    }
    public static final class Pass {
        public final int examined;
        public final Ticket engaged;
        public final boolean continuationPending;
        private Pass(int examined, Ticket engaged, boolean continuationPending) {
            this.examined = examined; this.engaged = engaged; this.continuationPending = continuationPending;
        }
    }

    private final UUID pressId;
    private final Clock clock;
    private final ArrayDeque<Ticket> waiting = new ArrayDeque<Ticket>(MAX_WAITING);
    private Ticket active;
    private long reserved;
    private long lastTime;
    private boolean hasTime;
    private boolean inPass;
    private boolean demand;
    private boolean scheduled;
    private boolean hasContinuationFenceTick;
    private long lastContinuationFenceTick;

    public MusleeCommandQueue(UUID pressId, Clock clock) {
        if (pressId == null || clock == null) throw new IllegalArgumentException("press and clock required");
        this.pressId = pressId; this.clock = clock;
    }
    public UUID pressId() { return pressId; }
    public synchronized int waitingCount() { return waiting.size(); }
    public synchronized long reservedBytes() { return reserved; }
    public synchronized Ticket active() { return active; }
    public synchronized boolean continuationPending() { return scheduled; }
    public synchronized boolean resumeDemanded() { return demand; }

    /**
     * Atomically reserves/adopts one ticket. Session validity is an integration assertion,
     * not an authentication check. Duplicate command history belongs to the ingress layer.
     * Admission never retries itself. CLEANUP_REQUIRED is not a queue-full notification.
     */
    public synchronized Admission admit(UUID commandId, UUID sessionId, boolean sessionValid,
                                         long provenUpperBoundBytes) {
        outsidePass();
        if (!sessionValid || commandId == null || sessionId == null) return rejected(AdmissionStatus.INVALID_SESSION);
        if (provenUpperBoundBytes < 0 || provenUpperBoundBytes > MAX_CAPTURE_BYTES)
            return rejected(AdmissionStatus.INVALID_BOUND);
        Long now = time();
        if (now == null) return rejected(AdmissionStatus.CLOCK_UNCERTAIN);
        int examined = 0;
        boolean reachedLiveHead = false;
        while (!waiting.isEmpty() && examined < MAX_EXAMINATIONS) {
            Ticket head = waiting.peekFirst();
            examined++;
            if (!expired(head, now)) { reachedLiveHead = true; break; }
            discardHead(State.EXPIRED);
        }
        // Do not inspect a ninth entry. Conservatively reject this admission without replay.
        if (examined == MAX_EXAMINATIONS && !reachedLiveHead && !waiting.isEmpty()) {
            requestResume();
            return rejected(AdmissionStatus.CLEANUP_REQUIRED);
        }
        if (waiting.size() == MAX_WAITING) return rejected(AdmissionStatus.FULL);
        if (provenUpperBoundBytes > MAX_CAPTURE_BYTES - reserved) return rejected(AdmissionStatus.BUDGET_EXCEEDED);
        Ticket ticket = new Ticket(this, commandId, sessionId, now, provenUpperBoundBytes);
        waiting.addLast(ticket);
        reserved += provenUpperBoundBytes;
        requestResume();
        return new Admission(AdmissionStatus.ADMITTED, ticket);
    }

    /** Mandatory targeted invalidation is immediate, bounded by 32, not a purge pass. */
    public synchronized int invalidateSession(UUID sessionId) {
        outsidePass();
        int count = 0;
        for (Iterator<Ticket> iterator = waiting.iterator(); iterator.hasNext();) {
            Ticket ticket = iterator.next();
            if (ticket.sessionId.equals(sessionId)) {
                iterator.remove(); retireWaiting(ticket, State.INVALIDATED); count++;
            }
        }
        refreshSchedule();
        return count;
    }
    /**
     * Immediately invalidate exactly one waiting lease by object identity. Other
     * commands of the same session retain their FIFO order. Null, foreign, engaged
     * and terminal tickets are inert; repeated invalidation cannot release twice.
     * This is a bounded (at most 32 entries) mandatory invalidation, not an expiry pass.
     *
     * For a dependency set, the adapter must hold its common authoritative lock
     * across every affected queue/fence invalidation and safe capture-store release.
     * This method does not discover dependencies or release actual external captures.
     */
    public synchronized boolean invalidateWaiting(Ticket ticket) {
        outsidePass();
        if (ticket == null || ticket.queue != this || ticket.state != State.WAITING) return false;
        if (!waiting.remove(ticket)) return false;
        retireWaiting(ticket, State.INVALIDATED);
        refreshSchedule();
        return true;
    }
    /** Explicit loss of clock proof invalidates all nonengaged leases, never the active one. */
    public synchronized void invalidateUncertainClock() {
        outsidePass(); invalidateClock();
    }
    /** Coalesced request; safe during pure validation. No scheduler callback is invoked. */
    public synchronized void requestResume() {
        demand = true;
        if (!inPass) refreshSchedule();
    }

    /**
     * Run a scheduled continuation at most once per strictly increasing server tick.
     * The host must call this from its scheduler; this class schedules no Minecraft work.
     */
    public synchronized Pass runScheduled(long serverTick, Validator validator) {
        outsidePass();
        if (hasContinuationFenceTick && serverTick <= lastContinuationFenceTick) return new Pass(0, null, scheduled);
        if (!scheduled || active != null) return new Pass(0, null, scheduled);
        hasContinuationFenceTick = true; lastContinuationFenceTick = serverTick;
        return runPass(validator);
    }
    /**
     * Verified authoritative completion only. False/foreign/repeated completion is inert.
     * serverTick must be the current authoritative server tick, in the same domain as
     * runScheduled. Any continuation after this immediate pass waits until a later tick.
     */
    public synchronized Pass complete(Ticket ticket, boolean authoritativeFinalizationVerified,
                                      long serverTick, Validator validator) {
        outsidePass();
        if (!authoritativeFinalizationVerified || ticket == null || ticket != active)
            return new Pass(0, null, scheduled);
        if (validator == null) throw new IllegalArgumentException("validator required");
        active = null;
        ticket.state = State.COMPLETED;
        release(ticket);
        demand = true;
        // An immediate completion pass uses this tick's processing opportunity too:
        // any remainder must wait for a strictly later tick, even when the last
        // scheduled pass was many ticks ago. Never move the fence backwards.
        if (!hasContinuationFenceTick || serverTick > lastContinuationFenceTick) {
            hasContinuationFenceTick = true;
            lastContinuationFenceTick = serverTick;
        }
        return runPass(validator); // first resumption is immediate, not a scheduled continuation
    }

    private Pass runPass(Validator validator) {
        if (validator == null) throw new IllegalArgumentException("validator required");
        inPass = true; scheduled = false; demand = false;
        int examined = 0;
        Ticket engaged = null;
        try {
            while (active == null && !waiting.isEmpty() && examined < MAX_EXAMINATIONS) {
                Long now = time();
                if (now == null) break;
                Ticket head = waiting.peekFirst();
                examined++;
                if (expired(head, now)) { discardHead(State.EXPIRED); continue; }
                Verdict verdict;
                try { verdict = validator.validate(head); }
                catch (RuntimeException failedValidation) { verdict = Verdict.REJECT; }
                // Validation may take time. Recheck at the actual accounting transition.
                now = time();
                if (now == null) break;
                if (expired(head, now)) { discardHead(State.EXPIRED); continue; }
                if (verdict == Verdict.ENGAGE_RETAIN_CAPTURE || verdict == Verdict.ENGAGE_RELEASE_CAPTURE) {
                    waiting.removeFirst(); reserved -= head.reservedBytes;
                    head.state = State.ENGAGED;
                    head.owner = Owner.TRANSACTION;
                    if (verdict == Verdict.ENGAGE_RELEASE_CAPTURE) release(head);
                    active = head; engaged = head;
                } else { discardHead(State.REJECTED); }
            }
        } finally {
            inPass = false;
            refreshSchedule();
        }
        return new Pass(examined, engaged, scheduled);
    }
    private void refreshSchedule() {
        scheduled = active == null && !waiting.isEmpty();
        if (waiting.isEmpty()) demand = false;
    }
    private void outsidePass() {
        if (inPass) throw new IllegalStateException("validator must not mutate queue");
    }
    private Admission rejected(AdmissionStatus status) { return new Admission(status, null); }
    private boolean expired(Ticket ticket, long now) {
        // Nondecreasing readings: a negative difference means positive elapsed overflow,
        // which is certainly beyond 30 seconds, not a renewed deadline.
        long elapsed = now - ticket.admittedAt;
        return elapsed < 0 || elapsed >= MAX_WAIT_NANOS;
    }
    private Long time() {
        long now;
        try { now = clock.nanoTime(); }
        catch (RuntimeException unavailable) { invalidateClock(); return null; }
        // Negative nanoTime origins are valid. Fail closed on reset, signed wrap or
        // an unrepresentable observation interval; do not guess elapsed duration.
        if (hasTime && (now < lastTime || now - lastTime < 0)) { invalidateClock(); return null; }
        hasTime = true; lastTime = now;
        return now;
    }
    private void invalidateClock() {
        while (!waiting.isEmpty()) discardHead(State.INVALIDATED); // <=32 mandatory invalidations
        hasTime = false; demand = false; scheduled = false;
    }
    private void discardHead(State state) { retireWaiting(waiting.removeFirst(), state); }
    private void retireWaiting(Ticket ticket, State state) {
        reserved -= ticket.reservedBytes; ticket.state = state; release(ticket);
    }
    private void release(Ticket ticket) {
        if (ticket.owner != Owner.RELEASED) { ticket.owner = Owner.RELEASED; ticket.releases++; }
    }
}
