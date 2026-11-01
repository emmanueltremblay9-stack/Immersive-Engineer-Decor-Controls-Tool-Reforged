package com.oblixorprime.engineersdecorreforged.utility;

/**
 * Describes a texture blit with destination canvas dimensions kept separate from its source region.
 * This prevents a native source crop from being mistaken for its logical GUI size.
 */
public record GuiTextureBlit(
   int destinationWidth,
   int destinationHeight,
   int sourceX,
   int sourceY,
   int sourceWidth,
   int sourceHeight,
   int textureWidth,
   int textureHeight
) {
   public GuiTextureBlit {
      if (destinationWidth <= 0 || destinationHeight <= 0) {
         throw new IllegalArgumentException("Destination dimensions must be positive");
      }
      if (textureWidth <= 0 || textureHeight <= 0) {
         throw new IllegalArgumentException("Texture dimensions must be positive");
      }
      if (sourceX < 0 || sourceY < 0 || sourceWidth <= 0 || sourceHeight <= 0
         || sourceWidth > textureWidth || sourceHeight > textureHeight
         || sourceX > textureWidth - sourceWidth || sourceY > textureHeight - sourceHeight) {
         throw new IllegalArgumentException("Source region must fit inside the texture");
      }
   }

   public static GuiTextureBlit stretchFullTexture(int destinationWidth, int destinationHeight, int textureWidth, int textureHeight) {
      return new GuiTextureBlit(destinationWidth, destinationHeight, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
   }
}
