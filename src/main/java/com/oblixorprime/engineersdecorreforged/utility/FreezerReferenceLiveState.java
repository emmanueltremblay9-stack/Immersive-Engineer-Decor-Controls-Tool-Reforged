package com.oblixorprime.engineersdecorreforged.utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Client presentation model only: no clock, recipe, inventory, or machine mutation.
 * Every energy patch samples the original V5 texture. No generated replacement art.
 * The frost is baked into that texture and deliberately has NO live drawing plan.
 */
public final class FreezerReferenceLiveState {
   public static final int ENERGY_CELLS = 11;
   public static final int REFERENCE_LIT_CELLS = 8;
   public static final Rect ENERGY_HIT_REGION = new Rect(500, 670, 290, 28);
   /** Tooltip/model region, NOT a clean layer, erase mask, or full chamber silhouette. */
   public static final Rect FROST_HIT_REGION = new Rect(430, 370, 390, 230);
   public static final Rect DARK_DONOR = new Rect(738, 671, 25, 25);
   public static final Rect LIT_DONOR = new Rect(634, 671, 25, 25);
   public static final List<Integer> CELL_EDGES = List.of(507, 533, 558, 584, 609, 634, 659, 684, 713, 738, 763, 787);
   /** A conservative polygon inside the glass/well, excluding the metallic housing. */
   public static final List<Point> ENERGY_MASK = List.of(
      new Point(511, 671), new Point(782, 671), new Point(786, 675), new Point(786, 690),
      new Point(782, 695), new Point(511, 695), new Point(507, 691), new Point(507, 675));

   private FreezerReferenceLiveState() { }

   public enum PreviewMode { PRODUCTION, EXACT_SNAPSHOT, LIVE_ENERGY }

   /** Both opt-ins default false. Explicit exact mode wins if both are requested. */
   public static PreviewMode previewMode(boolean exactRequested, boolean liveRequested) {
      return exactRequested ? PreviewMode.EXACT_SNAPSHOT
         : liveRequested ? PreviewMode.LIVE_ENERGY : PreviewMode.PRODUCTION;
   }

   public record Point(int x, int y) { }

   public record Rect(int x, int y, int width, int height) {
      public Rect {
         if (x < 0 || y < 0 || width <= 0 || height <= 0
            || (long)x + width > FreezerReferenceGeometry.SOURCE_WIDTH
            || (long)y + height > FreezerReferenceGeometry.SOURCE_HEIGHT) {
            throw new IllegalArgumentException("Rectangle must lie inside retained source");
         }
      }

      public int right() { return x + width; }
      public int bottom() { return y + height; }
      public boolean contains(double px, double py) {
         return px >= x && px < right() && py >= y && py < bottom();
      }
   }

   /** All coordinates are source pixels; destination is clipped to ENERGY_MASK. */
   public record TexturePatch(int cell, boolean lit, Rect source, Rect destination) { }

   public record EnergyState(int stored, int capacity, int litCells) {
      public double fraction() { return capacity == 0 ? 0.0D : (double)stored / capacity; }
   }

   /** Exact FE from the menu is retained; only the artwork is quantized into 11 cells. */
   public static EnergyState energy(int rawStored, int rawCapacity) {
      int capacity = Math.max(0, rawCapacity);
      int stored = Math.max(0, Math.min(capacity, rawStored));
      int lit = capacity == 0 ? 0 : (int)((long)stored * ENERGY_CELLS / capacity);
      return new EnergyState(stored, capacity, lit);
   }

   /**
    * Progress and status are independent synchronized facts. In particular, the tick
    * consuming the last 8 FE can report progress=1 AND waiting-for-energy. Do not
    * infer/reset progress from status or extrapolate it with a client-side timer.
    */
   public record FrostState(int ticks, int totalTicks, int phase, int status, boolean redstonePowered) {
      public double fraction() { return totalTicks == 0 ? 0.0D : (double)ticks / totalTicks; }
      public int percent() { return totalTicks == 0 ? 0 : (int)((long)ticks * 100 / totalTicks); }
      public boolean artworkIsStatic() { return true; }
      public boolean cleanChamberAssetAvailable() { return false; }
   }

   public static FrostState frost(int rawTicks, int rawTotal, int rawPhase, int status, boolean redstonePowered) {
      int total = Math.max(0, rawTotal);
      return new FrostState(Math.max(0, Math.min(total, rawTicks)), total,
         Math.max(0, Math.min(4, rawPhase)), status, redstonePowered);
   }

   /** Intentionally empty until an independently reviewed clean chamber and frost layer exist. */
   public static List<TexturePatch> frostPatches(FrostState state) {
      if (state == null) throw new IllegalArgumentException("A synchronized state is required");
      return List.of();
   }

   /** Pixel-center inclusion in the explicitly measured conservative mask. */
   public static boolean allowsEnergyPixel(int x, int y) {
      double px = x + 0.5D;
      double py = y + 0.5D;
      boolean inside = false;
      for (int i = 0, j = ENERGY_MASK.size() - 1; i < ENERGY_MASK.size(); j = i++) {
         Point a = ENERGY_MASK.get(i);
         Point b = ENERGY_MASK.get(j);
         if ((a.y > py) != (b.y > py)
            && px < (double)(b.x - a.x) * (py - a.y) / (b.y - a.y) + a.x) inside = !inside;
      }
      return inside;
   }

   /**
    * Source-segment decomposition, not hidden-background recovery. Existing lit
    * cells are retained. Off cells use the dark middle donor so the orange spill
    * baked into cell 8 does not masquerade as power. Outer warm metal reflections
    * stay exactly as drawn. At 8/11 there are no substitutions at all.
    */
   public static List<TexturePatch> energyPatches(EnergyState state) {
      if (state == null) throw new IllegalArgumentException("A synchronized state is required");
      if (state.litCells == REFERENCE_LIT_CELLS) return List.of();
      List<TexturePatch> patches = new ArrayList<>();
      for (int cell = 0; cell < ENERGY_CELLS; cell++) {
         boolean lit = cell < state.litCells;
         if (lit && cell < REFERENCE_LIT_CELLS) continue;
         Rect donor = lit ? LIT_DONOR : DARK_DONOR;
         int left = CELL_EDGES.get(cell);
         int right = CELL_EDGES.get(cell + 1);
         for (int y = donor.y; y < donor.bottom(); y++) {
            int start = left;
            int end = right;
            while (start < end && !allowsEnergyPixel(start, y)) start++;
            while (end > start && !allowsEnergyPixel(end - 1, y)) end--;
            if (start == end) continue;
            // Integer source subregion; the same bounded blit is used by GUI and CPU proof.
            int sourceLeft = donor.x + (start - left) * donor.width / (right - left);
            int sourceRight = donor.x + ((end - left) * donor.width + right - left - 1) / (right - left);
            Rect source = new Rect(sourceLeft, y, sourceRight - sourceLeft, 1);
            Rect destination = new Rect(start, y, end - start, 1);
            // Merge vertically identical spans, keeping native work bounded (normally <30 blits).
            if (!patches.isEmpty()) {
               TexturePatch previous = patches.get(patches.size() - 1);
               if (previous.cell == cell && previous.source.x == source.x && previous.source.width == source.width
                  && previous.destination.x == destination.x && previous.destination.width == destination.width
                  && previous.destination.bottom() == y) {
                  patches.set(patches.size() - 1, new TexturePatch(cell, lit,
                     new Rect(source.x, previous.source.y, source.width, previous.source.height + 1),
                     new Rect(destination.x, previous.destination.y, destination.width, previous.destination.height + 1)));
                  continue;
               }
            }
            patches.add(new TexturePatch(cell, lit, source, destination));
         }
      }
      return List.copyOf(patches);
   }

   public static List<TexturePatch> energyPatches(PreviewMode mode, EnergyState state) {
      return mode == PreviewMode.LIVE_ENERGY ? energyPatches(state) : List.of();
   }
}
