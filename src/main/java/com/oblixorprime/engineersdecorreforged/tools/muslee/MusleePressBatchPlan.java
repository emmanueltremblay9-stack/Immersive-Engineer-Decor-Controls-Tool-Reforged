package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.Optional;

/** Pure arithmetic for a single approved batch; never a live press executor. */
public final class MusleePressBatchPlan {
    public record Input(int bread, int apples, int seeds, boolean seedAdmissible,
                        int remainingDurability, int verifiedOutputRoom) {}
    public record Result(int breadAfter, int applesAfter, int seedsAfter,
                         int durabilityAfter, int barsProduced, boolean clearSeedType) {}
    private MusleePressBatchPlan() {}
    public static Optional<Result> evaluate(Input in) {
        if (in == null || !stock(in.bread()) || !stock(in.apples()) || !stock(in.seeds())
                || !in.seedAdmissible() || in.remainingDurability() < 1
                || in.remainingDurability() > 64 || in.verifiedOutputRoom() < 4) return Optional.empty();
        return Optional.of(new Result(in.bread()-1, in.apples()-1, in.seeds()-1,
                in.remainingDurability()-1, 4, in.seeds()==1));
    }
    private static boolean stock(int count) { return count >= 1 && count <= 8; }
}
