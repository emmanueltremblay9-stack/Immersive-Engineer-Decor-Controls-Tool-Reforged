package com.oblixorprime.engineersdecorreforged.utility;

/** Describes a blit with its destination canvas separate from the native source crop. */
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

   public static GuiTextureBlit forMachineArtwork(String textureName, int destinationWidth, int destinationHeight) {
      return switch (textureName) {
         case "small_solar_panel_art" -> stretchFullTexture(destinationWidth, destinationHeight, 455, 424);
         case "small_tree_cutter_art" -> stretchFullTexture(destinationWidth, destinationHeight, 446, 418);
         default -> throw new IllegalArgumentException("Unknown machine artwork texture: " + textureName);
      };
   }
}
