package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Grey-steel material and frame from Placer V2, not its controls or layout.
 * All inventory positions, packets and synchronized Hopper state stay unchanged.
 */
final class FactoryHopperSteelArtwork {
   private static final ResourceLocation STEEL = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/factory_placer_v2_reference/background.png");

   private FactoryHopperSteelArtwork() {}

   static void render(GuiGraphics g, MachineMenu menu, ResourceLocation original, int x, int y) {
      int width = menu.layout().imageWidth(), height = menu.layout().imageHeight();
      // Retained unlettered steel nameplate material, tiled by the renderer, not a
      // stretched copy of the Placer inventory or an additional texture asset.
      part(g, x, y, width, height, 250, 36, 680, 48);
      g.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0xFF242628);
      // Reuse just outer frame strips. No Placer slider, lamp or button is copied.
      part(g, x, y, width, 5, 220, 15, 740, 20);
      part(g, x, y + height - 5, width, 5, 130, 1190, 930, 30);
      part(g, x, y + 5, 5, height - 10, 33, 200, 22, 940);
      part(g, x + width - 5, y + 5, 5, height - 10, 1150, 200, 22, 940);
      for (var slot : menu.slots) {
         // 16x16 vanilla item footprint is never covered. Rim is outside it.
         int sx = x + slot.x, sy = y + slot.y;
         g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF979A98);
         g.fill(sx, sy, sx + 16, sy + 16, 0xFF101315);
         g.fill(sx - 1, sy + 16, sx + 17, sy + 17, 0xFF484B4C);
         g.fill(sx + 16, sy, sx + 17, sy + 16, 0xFF484B4C);
      }
      // Hopper's three rails and redstone controls remain its original artwork,
      // at the exact existing hitboxes; drawOriginalWidgets supplies live state.
      g.blit(original, x + 127, y + 8, 127, 8, 46, 53);
   }

   private static void part(GuiGraphics g, int x, int y, int w, int h, int u, int v, int sw, int sh) {
      g.blit(STEEL, x, y, w, h, (float)u, (float)v, sw, sh, 1199, 1312);
   }
}
