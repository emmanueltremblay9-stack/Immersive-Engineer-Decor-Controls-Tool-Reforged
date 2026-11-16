package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Steel presentation only. Native menu geometry, packet handling and state rendering are unchanged. */
final class FactoryHopperSteelArtwork {
   private static final ResourceLocation STEEL = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/factory_placer_v2_reference/background.png");
   private FactoryHopperSteelArtwork() {}
   static void render(GuiGraphics g, MachineMenu menu, ResourceLocation original, int x, int y) {
      int width=menu.layout().imageWidth(),height=menu.layout().imageHeight();
      material(g,x,y,width,height);
      FactorySteelFrame.frame(g,x,y,width,height);
      panel(g,x+6,y+4,115,59); // 18 storage slots, never moved.
      panel(g,x+125,y+4,47,59); // Three rails and logic controls.
      panel(g,x+4,y+67,168,57);
      panel(g,x+4,y+126,168,21);
      FactorySteelFrame.divider(g,x+4,y+62,width-8);
      // Recessed ventilation in existing footer space, not an unexplained title plate.
      panel(g,x+5,y+150,166,12);
      for(int vent=0;vent<12;vent++) {
         int vx=x+12+vent*13;
         g.fill(vx,y+154,vx+8,y+157,0xFF080C0F);
         g.fill(vx,y+157,vx+8,y+158,0xFF6B767D);
      }
      for (var slot : menu.slots) {
         int sx=x+slot.x,sy=y+slot.y;
         // Two-tone bevels live outside the complete 16x16 item footprint.
         // Adjacent rims share the 2px gap; their logical item slots never move.
         g.fill(sx-1,sy-2,sx+17,sy+18,0xFF171D21);
         g.fill(sx-2,sy-1,sx+18,sy+17,0xFF171D21);
         g.fill(sx-1,sy-2,sx+17,sy-1,0xFFCFD6D9);
         g.fill(sx,sy-1,sx+16,sy,0xFF76838C);
         g.fill(sx-2,sy,sx-1,sy+16,0xFFAFBBC3);
         g.fill(sx-1,sy,sx,sy+16,0xFF64717A);
         g.fill(sx,sy+16,sx+16,sy+17,0xFF52616B);
         g.fill(sx-1,sy+17,sx+17,sy+18,0xFF080D10);
         g.fill(sx+16,sy,sx+17,sy+16,0xFF45545F);
         g.fill(sx+17,sy-1,sx+18,sy+17,0xFF080D10);
         g.fill(sx,sy,sx+16,sy+16,0xFF101315);
      }
      for(int row=0;row<3;row++) {
         int yy=y+16+13*row;
         g.fill(x+132,yy-1,x+169,yy+2,0xFF0B0E10);
         g.fill(x+132,yy+2,x+169,yy+3,0xFF667179);
         for(int tick=0;tick<5;tick++)g.fill(x+133+8*tick,yy-2,x+134+8*tick,yy,0xFFA7AFB1);
      }
      // Compact neutral glyphs: reach, cadence, quantity. Hover translations remain native.
      g.fill(x+128,y+12,x+130,y+19,0xFFB8C1C6);
      g.fill(x+127,y+13,x+131,y+14,0xFFB8C1C6);
      g.fill(x+127,y+17,x+131,y+18,0xFFB8C1C6);
      g.fill(x+127,y+26,x+131,y+31,0xFFB8C1C6);
      g.fill(x+128,y+27,x+131,y+30,0xFF20262B);
      g.fill(x+129,y+27,x+130,y+29,0xFFB8C1C6);
      for(int row=0;row<3;row++)g.fill(x+127,y+39+row*2,x+131,y+40+row*2,0xFFB8C1C6);
      // Keep the exact original state symbols, but not the obsolete grey control panel.
      for(int bx:new int[]{133,145,159}) {
         panel(g,x+bx-1,y+48,11,11);
         g.blit(original,x+bx,y+49,bx,49,9,9);
      }
   }
   private static void material(GuiGraphics g,int x,int y,int w,int h) {
      for(int row=0;row<h;row+=8)for(int col=0;col<w;col+=32) {
         int cw=Math.min(32,w-col),ch=Math.min(8,h-row);
         part(g,x+col,y+row,cw,ch,250,160,cw*8,ch*3);
      }
   }
   private static void panel(GuiGraphics g,int x,int y,int w,int h) {
      g.fill(x,y,x+w,y+h,0xFF121619);
      g.fill(x,y,x+w,y+1,0xFF78838A);
      g.fill(x,y,x+1,y+h,0xFF4C565D);
      g.fill(x+w-1,y,x+w,y+h,0xFF080B0D);
      g.fill(x,y+h-1,x+w,y+h,0xFF080B0D);
      material(g,x+1,y+1,w-2,h-2);
   }
   private static void part(GuiGraphics g,int x,int y,int w,int h,int u,int v,int sw,int sh) {
      g.blit(STEEL,x,y,w,h,(float)u,(float)v,sw,sh,1199,1312);
   }
}
