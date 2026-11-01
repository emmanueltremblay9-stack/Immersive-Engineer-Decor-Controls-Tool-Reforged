package com.oblixorprime.engineersdecorreforged.utility;

import java.util.List;

/** Runtime masks for the illustrative bucket and ice cube embedded in the canonical Freezer artwork. */
public final class SmallFreezerArtworkMasks {
   private static final int SOURCE_CROP_WIDTH = 422;
   private static final int SOURCE_CROP_HEIGHT = 443;

   // Pixel bounds measured from the native 422 x 443 crop; right/bottom are exclusive.
   private static final SourceBounds INPUT_BUCKET = new SourceBounds(58, 121, 100, 160);
   private static final SourceBounds OUTPUT_ICE = new SourceBounds(326, 121, 369, 161);

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
