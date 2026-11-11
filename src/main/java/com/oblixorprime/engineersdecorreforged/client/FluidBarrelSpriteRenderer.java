package com.oblixorprime.engineersdecorreforged.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Draws the live Barrel fluid inside the existing glass, without changing its artwork. */
final class FluidBarrelSpriteRenderer {
   private static final int TILE_SIZE = 16;

   private FluidBarrelSpriteRenderer() {}

   static boolean draw(GuiGraphics graphics, FluidStack fluid, int x, int y, int width, int height) {
      return draw(graphics, fluid, x, y, width, height, x, y, x + width, y + height);
   }

   /** Coordinates for blits stay local; scissor bounds must be real GUI screen coordinates. */
   static boolean draw(GuiGraphics graphics, FluidStack fluid, int x, int y, int width, int height,
                       int clipLeft, int clipTop, int clipRight, int clipBottom) {
      if (clipRight <= clipLeft || clipBottom <= clipTop) return false;
      if (fluid.isEmpty() || width <= 0 || height <= 0) return false;
      IClientFluidTypeExtensions properties = IClientFluidTypeExtensions.of(fluid.getFluid());
      ResourceLocation still = properties.getStillTexture(fluid);
      if (still == null) return false;
      // Resolve from the current block atlas on every draw: resource reload replaces sprites/UVs.
      var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
      if (sprite == null || sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) return false;
      int tint = properties.getTintColor(fluid);
      float red = (tint >>> 16 & 255) / 255.0F;
      float green = (tint >>> 8 & 255) / 255.0F;
      float blue = (tint & 255) / 255.0F;
      float alpha = (tint >>> 24 & 255) / 255.0F;

      // GuiGraphics scopes its scissor stack and flushes at the clipping boundaries.
      // Whole 16px sprites are bottom-anchored; scissoring crops the top/right UVs
      // instead of squeezing an entire sprite into a short final row or column.
      graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
      try {
         for (int tileX = x; tileX < x + width; tileX += TILE_SIZE) {
            for (int tileY = y + height - TILE_SIZE; tileY > y - TILE_SIZE; tileY -= TILE_SIZE) {
               // Per-vertex ARGB, never a global shader color that could tint labels/items.
               graphics.blit(tileX, tileY, 0, TILE_SIZE, TILE_SIZE, sprite, red, green, blue, alpha);
            }
         }
      } finally {
         graphics.disableScissor();
      }
      return true;
   }
}
