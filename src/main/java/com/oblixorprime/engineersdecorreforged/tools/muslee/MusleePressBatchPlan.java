package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.Optional;

/** Pure arithmetic for a generic-reserve batch; never a live press executor. */
public final class MusleePressBatchPlan {
    public record Input(int cereal, int fruit, int seeds,
                        int remainingDurability, int verifiedOutputRoom) {}
    public record Result(int cerealAfter, int fruitAfter, int seedsAfter,
                         int durabilityAfter, int barsProduced) {}
    private MusleePressBatchPlan() {}
    public static Optional<Result> evaluate(Input in) {
        if (in == null || !stock(in.cereal()) || !stock(in.fruit()) || !stock(in.seeds())
                || in.remainingDurability() < 1
                || in.remainingDurability() > 64 || in.verifiedOutputRoom() < 4) return Optional.empty();
        return Optional.of(new Result(in.cereal()-1, in.fruit()-1, in.seeds()-1,
                in.remainingDurability()-1, 4));
    }
    private static boolean stock(int count) { return count >= 1 && count <= 8; }
}
