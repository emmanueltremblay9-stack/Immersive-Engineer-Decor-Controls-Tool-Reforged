package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.List;
import java.util.Optional;

/** Pure pickup policy for the approved metal-table repair. No recipe registration. */
public final class MusleeRepairPlan {
    public enum Kind { EMPTY, PRESS, IE_IRON_PLATE, OTHER }
    public record Cell(Kind kind, int count) {}
    public record Result(int durabilityAfter, int durabilityRestored, int platesConsumed,
                         int experienceCost, int energyCost) {}
    private MusleeRepairPlan() {}

    /** Kind and station identity must be resolved from trusted current server inputs. */
    public static Optional<Result> evaluatePickup(boolean exactMetalCraftingTable,
            List<Cell> grid, int remainingDurability, boolean destinationAcceptsOriginal) {
        if (!exactMetalCraftingTable || !destinationAcceptsOriginal || grid == null || grid.size()!=9
                || remainingDurability<0 || remainingDurability>=64) return Optional.empty();
        final List<Cell> cells;
        try { cells=List.copyOf(grid); } catch(RuntimeException invalid) { return Optional.empty(); }
        for(int i=0;i<9;i++) {
            Kind expected=i==4?Kind.PRESS:(i==1||i==3||i==5||i==7)?Kind.IE_IRON_PLATE:Kind.EMPTY;
            int count=expected==Kind.EMPTY?0:1;
            if(cells.get(i).kind()!=expected || cells.get(i).count()!=count) return Optional.empty();
        }
        return Optional.of(new Result(64,64-remainingDurability,4,0,0));
    }
}
