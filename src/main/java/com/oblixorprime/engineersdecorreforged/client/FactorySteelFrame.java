package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Read-only steel skin, assembled from the retained Placer material. No geometry or input ownership. */
final class FactorySteelFrame {
   private static final ResourceLocation STEEL = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/factory_placer_v2_reference/background.png");
   private FactorySteelFrame() {}

   static void frame(GuiGraphics g,int x,int y,int width,int height) {
      // Bevelled structural rails outside the native item footprint; square corner
      // rivets keep the industrial silhouette readable even at GUI scale one.
      part(g,x-5,y-5,width+10,7,135,1190,920,30);
      part(g,x-5,y+height-2,width+10,7,135,1190,920,30);
      part(g,x-5,y+2,7,height-4,33,200,22,940);
      part(g,x+width-2,y+2,7,height-4,1150,200,22,940);
      corner(g,x-6,y-6);corner(g,x+width-6,y-6);
      corner(g,x-6,y+height-6);corner(g,x+width-6,y+height-6);
   }

   static void divider(GuiGraphics g,int x,int y,int width) {
      part(g,x,y,width,6,135,1190,920,30);
      // Narrow safety marks are accents, not a copper structural frame.
      for(int i=4;i<width-4;i+=12) {
         g.fill(x+i,y+2,x+i+5,y+4,0xFFB99A3C);
      }
   }

   static void slots(GuiGraphics g,MachineMenu menu,int x,int y) {
      for(var slot:menu.slots) {
         int sx=x+slot.x,sy=y+slot.y;
         // Crop the silver rim only; never paint over the 16x16 item content.
         part(g,sx-1,sy-1,18,1,110,202,90,8);
         part(g,sx-1,sy+16,18,1,110,288,90,8);
         part(g,sx-1,sy,1,16,110,210,8,78);
         part(g,sx+16,sy,1,16,192,210,8,78);
      }
   }

   static void inset(GuiGraphics g,int x,int y,int w,int h) {
      g.fill(x,y,x+w,y+h,0xFF101519);
      g.fill(x+1,y+1,x+w-1,y+h-1,0xFF222A30);
      g.fill(x+1,y+1,x+w-1,y+2,0xFF070B0D);
      g.fill(x,y+h-1,x+w,y+h,0xFF727E85);
   }
   static void thumb(GuiGraphics g,int x,int y,int w,int h) {
      g.fill(x,y,x+w,y+h,0xFF151A1E);
      g.fill(x,y,x+w,y+1,0xFFD9DEE0);
      g.fill(x,y,x+1,y+h,0xFFACB7BE);
      g.fill(x+1,y+1,x+w-1,y+h-1,0xFF87969F);
      g.fill(x+w-1,y+1,x+w,y+h,0xFF414D55);
      g.fill(x+1,y+h-1,x+w,y+h,0xFF414D55);
   }

   /** Fixed identity glyphs: play, direction and pulse, separate from state rails. */
   static void controlSymbol(GuiGraphics g,int x,int y,int index) {
      String[] rows = switch(index) {
         case 0 -> new String[]{"1000000","1110000","1111100","1111111","1111100","1110000","1000000"};
         case 1 -> new String[]{"0001000","0001100","1111110","1111111","1111110","0001100","0001000"};
         default -> new String[]{"0011100","0010100","0010100","0010100","1110111","0000000","0000000"};
      };
      for(int row=0;row<rows.length;row++) for(int col=0;col<7;col++)
         if(rows[row].charAt(col)=='1') g.fill(x+col,y+row,x+col+1,y+row+1,0xFFD9DEE0);
   }

   private static void corner(GuiGraphics g,int x,int y) {part(g,x,y,12,12,30,1130,100,90);}
   private static void part(GuiGraphics g,int x,int y,int w,int h,int u,int v,int sw,int sh) {
      g.blit(STEEL,x,y,w,h,(float)u,(float)v,sw,sh,1199,1312);
   }
}
