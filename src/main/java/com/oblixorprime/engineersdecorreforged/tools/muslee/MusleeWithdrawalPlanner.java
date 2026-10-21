package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Pure destination arithmetic. Slot descriptors must come from a trusted server snapshot. */
public final class MusleeWithdrawalPlanner {
    public static final int SLOT_COUNT = 36;
    public record Slot(int index, int count, int capacity, boolean compatible, boolean admissible) {}
    public record Move(int index, int count) {}
    public static final class Plan {
        private final List<Move> moves;
        private final int transferred;
        private Plan(List<Move> moves, int transferred) {
            this.moves = List.copyOf(moves);
            this.transferred = transferred;
        }
        public List<Move> moves() { return moves; }
        public int transferred() { return transferred; }
    }
    private MusleeWithdrawalPlanner() {}

    /** No live ItemStack, revision, inventory write or transaction is represented here. */
    public static Optional<Plan> simulate(int available, List<Slot> authoritativeSlots) {
        if (available < 1 || available > 8 || authoritativeSlots == null
                || authoritativeSlots.size() != SLOT_COUNT) return Optional.empty();
        final List<Slot> slots;
        try { slots = List.copyOf(authoritativeSlots); }
        catch (RuntimeException invalid) { return Optional.empty(); }
        for (int i = 0; i < SLOT_COUNT; i++) {
            Slot s = slots.get(i);
            if (s.index() != i || s.count() < 0 || s.capacity() < 0
                    || s.count() > s.capacity()) return Optional.empty();
        }
        int remaining = available;
        List<Move> moves = new ArrayList<>();
        // TOOLS-327: compatible nonempty piles first, then empty slots; ascending in each phase.
        for (int phase = 0; phase < 2 && remaining > 0; phase++) {
            for (Slot s : slots) {
                if (!s.admissible() || (phase == 0 ? s.count() == 0 || !s.compatible() : s.count() != 0)) continue;
                int moving = Math.min(remaining, s.capacity() - s.count());
                if (moving > 0) {
                    moves.add(new Move(s.index(), moving));
                    remaining -= moving;
                    if (remaining == 0) break;
                }
            }
        }
        return remaining == available ? Optional.empty() : Optional.of(new Plan(moves, available - remaining));
    }
}
