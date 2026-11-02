package com.oblixorprime.engineersdecorreforged.utility;

import java.util.List;

/** Runtime masks for the illustrative bucket and ice cube embedded in the canonical Freezer artwork. */
public final class SmallFreezerArtworkMasks {
   private static final int SOURCE_CROP_WIDTH = 422;
   private static final int SOURCE_CROP_HEIGHT = 443;

   // Bounds cover the printed examples, leaving their integrated metal bezels visible; right/bottom are exclusive.
   private static final SourceBounds INPUT_BUCKET = new SourceBounds(58, 120, 98, 164);
   private static final SourceBounds OUTPUT_ICE = new SourceBounds(331, 122, 369, 164);
   private static final SourceBounds PROGRESS_BAR = new SourceBounds(125, 197, 301, 218);
   // The live mask follows the baked frame itself. The old bounds ran below and
   // into the numeric readout, leaving the sample orange fill visible above it.
   private static final SourceBounds ENERGY_BAR = new SourceBounds(121, 224, 272, 253);

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

   public static MaskRegion progressBar(int destinationWidth, int destinationHeight) {
      return project(PROGRESS_BAR, destinationWidth, destinationHeight);
   }

   public static MaskRegion energyBar(int destinationWidth, int destinationHeight) {
      return project(ENERGY_BAR, destinationWidth, destinationHeight);
   }

   /** Centers the live item hitbox on the PNG's existing machine-slot frame. */
   public static SlotOrigin slotOriginForFrame(
      int sourceLeft, int sourceTop, int sourceRight, int sourceBottom, int destinationWidth, int destinationHeight
   ) {
      if (sourceLeft < 0 || sourceTop < 0 || sourceRight <= sourceLeft || sourceBottom <= sourceTop
         || sourceRight > SOURCE_CROP_WIDTH || sourceBottom > SOURCE_CROP_HEIGHT
         || destinationWidth <= 0 || destinationHeight <= 0) {
         throw new IllegalArgumentException("Invalid Freezer frame or destination bounds");
      }
      int centerX = (int)Math.round((sourceLeft + sourceRight) * (double)destinationWidth
         / (2.0D * SOURCE_CROP_WIDTH));
      int centerY = (int)Math.round((sourceTop + sourceBottom) * (double)destinationHeight
         / (2.0D * SOURCE_CROP_HEIGHT));
      return new SlotOrigin(centerX - 8, centerY - 8);
   }

   private static MaskRegion project(SourceBounds bounds, int destinationWidth, int destinationHeight) {
      if (destinationWidth <= 0 || destinationHeight <= 0) {
         throw new IllegalArgumentException("Mask destination dimensions must be positive");
      }
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

   public record SlotOrigin(int x, int y) {
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
