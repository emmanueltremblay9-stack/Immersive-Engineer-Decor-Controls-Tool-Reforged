package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.Objects;

/**
 * TEST-ONLY failure model. No filesystem, Minecraft state, save hooks or runtime grant.
 * Passing this simulation cannot qualify an actual persistence implementation.
 */
public final class MusleePersistenceContractCases {
    enum Cut { BEFORE_ENGAGEMENT, AFTER_ENGAGEMENT, BEFORE_REPLACE, DURING_REPLACE_OLD,
        DURING_REPLACE_NEW, AFTER_REPLACE, AFTER_TERMINAL }
    enum Fault { NONE, DROP_RECEIPT, RECEIPT_ONLY, STALE_SAVE, DUPLICATE_CURSOR,
        FALSE_CANCELLATION, REPLAY_ON_RECOVERY }
    enum Outcome { NOT_ENGAGED, UNKNOWN, EXECUTED, NO_EFFECT }
    enum Owner { CURSOR, INVENTORY }
    enum Receipt { NONE, EXECUTED, NO_EFFECT }

    /** Scalars stand in for complete canonical game state. They are not a real state codec. */
    record State(int cereal, int fruit, int seeds, int bars, int durability, Owner owner) {}
    /** A normal player snapshot and receipt must form ONE persistence unit. */
    record Snapshot(State state, Receipt receipt) {}
    record Evidence(boolean engaged, boolean terminal, Outcome terminalOutcome, Snapshot durable) {}
    record Reconciliation(Outcome outcome, boolean suspended) {}

    static final State BEFORE = new State(1, 1, 1, 0, 64, Owner.CURSOR);
    static final State AFTER = new State(0, 0, 0, 4, 63, Owner.CURSOR);
    static final Snapshot OLD = new Snapshot(BEFORE, Receipt.NONE);
    static final Snapshot NEW = new Snapshot(AFTER, Receipt.EXECUTED);
    static final Snapshot CANCELLED = new Snapshot(BEFORE, Receipt.NO_EFFECT);
    static int assertions;

    /** Synthetic storage with deliberately injectable broken save/ownership policies. */
    static final class Model {
        Snapshot durable = OLD;
        boolean engaged;
        boolean terminal;
        Outcome terminalOutcome = Outcome.UNKNOWN;
        int writes;
        int publishedBars;

        void run(Cut cut, Fault fault) {
            if (cut == Cut.BEFORE_ENGAGEMENT) return;
            engaged = true; writes++;
            if (cut == Cut.AFTER_ENGAGEMENT || cut == Cut.BEFORE_REPLACE
                    || cut == Cut.DURING_REPLACE_OLD) return;
            durable = switch (fault) {
                case DROP_RECEIPT -> new Snapshot(AFTER, Receipt.NONE);
                case RECEIPT_ONLY -> new Snapshot(BEFORE, Receipt.EXECUTED);
                case DUPLICATE_CURSOR -> new Snapshot(new State(0, 0, 0, 8, 63, Owner.CURSOR), Receipt.EXECUTED);
                default -> NEW;
            };
            writes++;
            if (cut == Cut.DURING_REPLACE_NEW) return;
            if (fault == Fault.STALE_SAVE) { durable = OLD; writes++; }
            // Only the qualified full outcome could permit publication. The faulty models
            // intentionally omit validation so the independent oracle must catch them.
            publishedBars = 4;
            if (cut == Cut.AFTER_REPLACE) return;
            terminal = true;
            terminalOutcome = fault == Fault.FALSE_CANCELLATION ? Outcome.NO_EFFECT : Outcome.EXECUTED;
            writes++;
        }

        /** Synthetic durable no-effect proof, never inferred merely from OLD or timeout. */
        void cancel(Cut cut) {
            if (cut == Cut.BEFORE_ENGAGEMENT) return;
            engaged = true; writes++;
            if (cut == Cut.AFTER_ENGAGEMENT || cut == Cut.BEFORE_REPLACE
                    || cut == Cut.DURING_REPLACE_OLD) return;
            durable = CANCELLED; writes++;
            if (cut == Cut.DURING_REPLACE_NEW || cut == Cut.AFTER_REPLACE) return;
            terminal = true; terminalOutcome = Outcome.NO_EFFECT; writes++;
        }

        Evidence evidence() { return new Evidence(engaged, terminal, terminalOutcome, durable); }

        Reconciliation recover(Fault fault) {
            if (fault == Fault.REPLAY_ON_RECOVERY && engaged && !terminal) {
                durable = NEW; writes++;
            }
            // Deliberately conservative: OLD alone is not durable no-effect proof.
            if (!engaged) return new Reconciliation(Outcome.NOT_ENGAGED, false);
            if (Objects.equals(durable, NEW) && (!terminal || terminalOutcome == Outcome.EXECUTED))
                return new Reconciliation(Outcome.EXECUTED, !terminal);
            if (Objects.equals(durable, CANCELLED) && (!terminal || terminalOutcome == Outcome.NO_EFFECT))
                return new Reconciliation(Outcome.NO_EFFECT, !terminal);
            return new Reconciliation(Outcome.UNKNOWN, true);
        }
    }

    /** Independent contract oracle for the fixed one-batch scenario, not an implementation. */
    static boolean coherent(Evidence evidence, int publishedBars) {
        Snapshot s = evidence.durable;
        if (!s.equals(OLD) && !s.equals(NEW) && !s.equals(CANCELLED)) return false;
        if (!evidence.engaged && (!s.equals(OLD) || evidence.terminal)) return false;
        if (publishedBars != 0 && (!s.equals(NEW) || !evidence.engaged)) return false;
        return !evidence.terminal || (evidence.engaged && (
                evidence.terminalOutcome == Outcome.EXECUTED && s.equals(NEW)
                || evidence.terminalOutcome == Outcome.NO_EFFECT && s.equals(CANCELLED)));
    }

    static boolean readOnly(Evidence before, int writes, Model after) {
        return after.writes == writes && after.evidence().equals(before);
    }

    static void check(boolean condition, String description) {
        assertions++;
        if (!condition) throw new AssertionError(description);
    }

    public static void run() {
        assertions = 0;
        for (Cut cut : Cut.values()) {
            Model model = new Model();
            model.run(cut, Fault.NONE);
            Evidence beforeRecovery = model.evidence();
            int writes = model.writes;
            check(coherent(beforeRecovery, model.publishedBars), "atomic old/new at " + cut);
            Reconciliation result = model.recover(Fault.NONE);
            check(model.writes == writes && model.evidence().equals(beforeRecovery), "read-only recovery " + cut);
            if (!model.engaged) check(result.outcome == Outcome.NOT_ENGAGED, "no fabricated engagement");
            else if (model.durable.equals(OLD))
                check(result.outcome == Outcome.UNKNOWN && result.suspended, "absence is not cancellation");
            else {
                check(result.outcome == Outcome.EXECUTED, "receipt-backed simulated outcome");
                check(result.suspended == !model.terminal, "missing terminal stays suspended");
            }
            check(model.publishedBars == 0 || model.durable.equals(NEW), "no early client publication");
            // Repeated read-only reconciliation must not append a missing terminal or replay.
            for (int retry = 0; retry < 20; retry++) {
                check(model.recover(Fault.NONE).equals(result), "stable reconciliation");
                check(model.writes == writes && model.evidence().equals(beforeRecovery), "no repair/replay");
            }
        }
        for (Cut cut : Cut.values()) {
            Model cancelled = new Model();
            cancelled.cancel(cut);
            Evidence evidence = cancelled.evidence();
            int writes = cancelled.writes;
            check(coherent(evidence, cancelled.publishedBars), "coherent cancellation " + cut);
            Reconciliation result = cancelled.recover(Fault.NONE);
            Outcome expected = !cancelled.engaged ? Outcome.NOT_ENGAGED
                    : cancelled.durable.equals(CANCELLED) ? Outcome.NO_EFFECT : Outcome.UNKNOWN;
            check(result.outcome == expected, "no-effect requires explicit proof " + cut);
            check(result.suspended == (cancelled.engaged && !cancelled.terminal), "cancellation suspension " + cut);
            check(cancelled.publishedBars == 0, "cancellation publishes no output");
            for (int repeat = 0; repeat < 20; repeat++) {
                check(cancelled.recover(Fault.NONE).equals(result), "stable cancellation reconciliation");
                check(readOnly(evidence, writes, cancelled), "cancellation recovery read-only");
            }
        }
        for (Fault fault : new Fault[] {Fault.DROP_RECEIPT, Fault.RECEIPT_ONLY,
                Fault.STALE_SAVE, Fault.DUPLICATE_CURSOR, Fault.FALSE_CANCELLATION}) {
            for (Cut cut : Cut.values()) {
                Model broken = new Model();
                broken.run(cut, fault);
                boolean takesEffect = switch (fault) {
                    case DROP_RECEIPT, RECEIPT_ONLY, DUPLICATE_CURSOR -> cut.ordinal() >= Cut.DURING_REPLACE_NEW.ordinal();
                    case STALE_SAVE -> cut.ordinal() >= Cut.AFTER_REPLACE.ordinal();
                    case FALSE_CANCELLATION -> cut == Cut.AFTER_TERMINAL;
                    default -> false;
                };
                check(coherent(broken.evidence(), broken.publishedBars) != takesEffect,
                        "fault activation boundary " + fault + " at " + cut);
                Evidence original = broken.evidence();
                int writes = broken.writes;
                broken.recover(Fault.NONE);
                check(readOnly(original, writes, broken), "corruption never repaired " + fault);
            }
        }
        for (Cut cut : Cut.values()) {
            Model replay = new Model();
            replay.run(cut, Fault.NONE);
            Evidence original = replay.evidence();
            int writes = replay.writes;
            replay.recover(Fault.REPLAY_ON_RECOVERY);
            boolean attemptedReplay = replay.engaged && !replay.terminal;
            check(readOnly(original, writes, replay) != attemptedReplay,
                    "read-only oracle detects replay writes even if state equal " + cut);
        }
        check(!coherent(new Evidence(true, true, Outcome.EXECUTED,
                new Snapshot(new State(0, 0, 0, 4, 63, Owner.INVENTORY), Receipt.EXECUTED)), 4),
                "cursor destination is part of atomic state");
        check(!coherent(new Evidence(true, false, Outcome.UNKNOWN, OLD), 4), "reject premature publication");
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASS: " + assertions + " simulation assertions; NO runtime persistence qualification");
    }
}
