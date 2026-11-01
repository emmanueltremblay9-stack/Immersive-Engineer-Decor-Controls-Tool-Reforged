package com.oblixorprime.engineersdecorreforged.utility;

import java.util.List;

/** Runtime masks for the illustrative bucket and ice cube embedded in the canonical Freezer artwork. */
public final class SmallFreezerArtworkMasks {
   private static final int SOURCE_CROP_WIDTH = 422;
   private static final int SOURCE_CROP_HEIGHT = 443;

   // Bounds include each illustrative item area in the native 422 x 443 crop; right/bottom are exclusive.
   private static final SourceBounds INPUT_BUCKET = new SourceBounds(54, 116, 106, 166);
   private static final SourceBounds OUTPUT_ICE = new SourceBounds(320, 114, 378, 170);

   private SmallFreezerArtworkMasks() {
   }

   public static List<MaskRegion> regions(int destinationWidth, int destinationHeight) {
      if (destinationWidth <= 0 || destinationHeight <= 0) {
         throw new IllegalArgumentException("Mask destination dimensions must be positive");
      }

      return List.of(
         project(INPUT_BUCKET, destinationWidth, destinationHeight),
         project(OUTPUT_ICE, destinationWidth, destinationHeight)
      );
   }

   /**
    * Returns machine slot frames touched by an artwork mask. The screen must redraw these frames
    * after filling the masks so illustrative pixels do not cut through live slot borders.
    */
   public static List<MachineLayout.MachineSlot> slotsIntersectingMasks(
      MachineLayout layout, int destinationWidth, int destinationHeight
   ) {
      List<MaskRegion> masks = regions(destinationWidth, destinationHeight);
      return layout.machineSlots().stream()
         .filter(slot -> masks.stream().anyMatch(mask ->
            overlaps(
               mask.x(), mask.y(), mask.right(), mask.bottom(),
               slot.x() - 1, slot.y() - 1, slot.x() + 17, slot.y() + 17
            )
         ))
         .toList();
   }

   private static boolean overlaps(int leftA, int topA, int rightA, int bottomA,
                                   int leftB, int topB, int rightB, int bottomB) {
      return leftA < rightB && rightA > leftB && topA < bottomB && bottomA > topB;
   }

   private static MaskRegion project(SourceBounds bounds, int destinationWidth, int destinationHeight) {
      int left = floorScale(bounds.left(), destinationWidth, SOURCE_CROP_WIDTH);
      int top = floorScale(bounds.top(), destinationHeight, SOURCE_CROP_HEIGHT);
      int right = ceilScale(bounds.right(), destinationWidth, SOURCE_CROP_WIDTH);
      int bottom = ceilScale(bounds.bottom(), destinationHeight, SOURCE_CROP_HEIGHT);
      return new MaskRegion(left, top, right - left, bottom - top);
   }

   private static int floorScale(int value, int numerator, int denominator) {
      return (int)((long)value * numerator / denominator);
   }

   private static int ceilScale(int value, int numerator, int denominator) {
      return (int)(((long)value * numerator + denominator - 1) / denominator);
   }

   private record SourceBounds(int left, int top, int right, int bottom) {
   }

   public record MaskRegion(int x, int y, int width, int height) {
      public int right() {
         return this.x + this.width;
      }

      public int bottom() {
         return this.y + this.height;
      }
   }
}
