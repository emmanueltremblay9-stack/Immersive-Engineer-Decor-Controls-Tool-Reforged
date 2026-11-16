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
      // Tile an unlettered dark brushed inset sample. Never stretch a flat fill
      // across the whole machine, nor copy the Placer slot arrangement.
      for(int row=0;row<height;row+=8) {
         for(int col=0;col<width;col+=32) {
            int w=Math.min(32,width-col),h=Math.min(8,height-row);
            part(g,x+col,y+row,w,h,250,160,w*8,h*3);
         }
      }
      FactorySteelFrame.frame(g,x,y,width,height);
      FactorySteelFrame.divider(g,x+3,y+height-99,width-6);
      for (var slot : menu.slots) {
         int sx=x+slot.x,sy=y+slot.y;
         g.fill(sx,sy,sx+16,sy+16,0xFF101315);
      }
      FactorySteelFrame.slots(g,menu,x,y);
      // Hopper's three rails and redstone controls remain its original artwork,
      // at the exact existing hitboxes; drawOriginalWidgets supplies live state.
      g.blit(original, x + 127, y + 8, 127, 8, 46, 53);
   }

   private static void part(GuiGraphics g, int x, int y, int w, int h, int u, int v, int sw, int sh) {
      g.blit(STEEL, x, y, w, h, (float)u, (float)v, sw, sh, 1199, 1312);
   }
}
