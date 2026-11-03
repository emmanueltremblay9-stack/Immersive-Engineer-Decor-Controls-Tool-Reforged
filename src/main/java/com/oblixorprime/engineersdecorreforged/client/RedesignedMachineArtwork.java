package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.BlockBreakerRedesignLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** New art only. This renderer never synthesizes unsupported machine state. */
final class RedesignedMachineArtwork {
   private static final String ROOT = "textures/gui/";
   private static final String MOD = "immersive_engineer_decor_controls_tool_reforged";

   private RedesignedMachineArtwork() {}

   private static ResourceLocation texture(String path) {
      return ResourceLocation.fromNamespaceAndPath(MOD, ROOT + path + ".png");
   }

   static void milking(GuiGraphics graphics, MachineMenu menu, int x, int y) {
      full(graphics, "milking_redesign_v1/background", x, y, 228);
      // The supplied layers are independent sprites, not aligned canvas overlays.
      // Crop only their actual content and clip to the glass interior, preserving alpha.
      int fill = fillPixels(menu.fluidAmount(), menu.fluidCapacity(), 51);
      if (fill > 0) {
         graphics.enableScissor(x + 104, y + 100 - fill, x + 126, y + 100);
         part(graphics, "milking_redesign_v1/milk", x + 104, y + 49, 22, 51,
            479, 228, 296, 802);
         graphics.disableScissor();
      }
      // The supplied glass layer has opaque blue/black artifacts and hides the live
      // milk at every level. Keep its source file intact, but do not render it until
      // a corrected transparent layer is available. The background already frames
      // the tank and the milk stays clipped inside that existing frame.
      // FE, extraction cycle and selected animal are not synchronized for Milking.
      // Leave those areas neutral. Do not claim a detected animal or paint fake energy.
   }

   static void breaker(GuiGraphics graphics, MachineMenu menu, int x, int y) {
      full(graphics, "block_breaker_rp_v3/background", x, y, BlockBreakerRedesignLayout.IMAGE_WIDTH);
      full(graphics, "block_breaker_rp_v3/power_neutral", x, y, BlockBreakerRedesignLayout.IMAGE_WIDTH);
      // The candidate background contains a sample FE fill. Clear the gauge interior
      // before rendering the actual synchronized value, including the zero case.
      var gauge = BlockBreakerRedesignLayout.FE;
      graphics.fill(x + gauge.x(), y + gauge.y(), x + gauge.right(), y + gauge.bottom(), 0xFF181A19);
      int fill = fillPixels(menu.energyStored(), menu.energyCapacity(), gauge.width());
      if (fill > 0) graphics.fill(x + gauge.x(), y + gauge.y(), x + gauge.x() + fill, y + gauge.bottom(), 0xFFFFC326);
      // The central broken block is static artwork, not the block in the world.
      // No fluid reservoir exists yet; the fluid gauge remains empty. The power icon
      // is a non-interactive neutral presentation, not an invented on/off command.
   }

   static int fillPixels(int amount, int capacity, int pixels) {
      if (amount <= 0 || capacity <= 0 || pixels <= 0) return 0;
      return (int) Math.min(pixels, (long) amount * pixels / capacity);
   }

   private static void full(GuiGraphics graphics, String path, int x, int y, int size) {
      part(graphics, path, x, y, size, size, 0, 0, 1254, 1254);
   }

   private static void part(GuiGraphics graphics, String path, int x, int y, int width, int height,
                            int u, int v, int sourceWidth, int sourceHeight) {
      graphics.blit(texture(path), x, y, width, height, (float) u, (float) v,
         sourceWidth, sourceHeight, 1254, 1254);
   }
}
