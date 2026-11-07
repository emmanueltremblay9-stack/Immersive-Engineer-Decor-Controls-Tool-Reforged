package com.oblixorprime.engineersdecorreforged.utility;

import java.util.List;

/** Logical-canvas insets covering the two sample bucket icons, separate from machine slot geometry. */
public record SmallFluidFunnelArtworkMask(int left, int top, int right, int bottom) {
   private static final List<SmallFluidFunnelArtworkMask> SAMPLE_BUCKET_MASKS = List.of(
      new SmallFluidFunnelArtworkMask(34, 86, 50, 102),
      new SmallFluidFunnelArtworkMask(156, 86, 172, 102)
   );

   public SmallFluidFunnelArtworkMask {
      if (left < 0 || top < 0 || right <= left || bottom <= top) {
         throw new IllegalArgumentException("Mask bounds must be a positive rectangle");
      }
   }

   public static List<SmallFluidFunnelArtworkMask> sampleBucketMasks() {
      return SAMPLE_BUCKET_MASKS;
   }
}
