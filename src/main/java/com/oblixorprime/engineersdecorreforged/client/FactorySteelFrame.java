package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Runtime slot rim sampled only from the owner-approved Hopper source. No geometry or input ownership. */
final class FactorySteelFrame {
   private static final ResourceLocation STEEL = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/factory_steel_v2/owner_slot_rim.png");
   private FactorySteelFrame() {}

   static void slots(GuiGraphics g,MachineMenu menu,int x,int y) {
      for(var slot:menu.slots) {
         int sx=x+slot.x,sy=y+slot.y;
         // Crop the silver rim only; never paint over the 16x16 item content.
         part(g,sx-1,sy-1,18,1,0,0,108,6);
         part(g,sx-1,sy+16,18,1,0,102,108,6);
         part(g,sx-1,sy,1,16,0,6,6,96);
         part(g,sx+16,sy,1,16,102,6,6,96);
      }
   }

   private static final ResourceLocation WELL = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/factory_steel_v2/owner_control_well.png");
   private static final ResourceLocation THUMB = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/factory_steel_v2/owner_control_thumb.png");
   private static final ResourceLocation NATIVE_DROPPER = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/factory_dropper_gui.png");

   static void inset(GuiGraphics g,int x,int y,int w,int h) {
      g.blit(WELL,x,y,w,h,0F,0F,108,108,108,108);
   }

   static void thumb(GuiGraphics g,int x,int y,int w,int h) {
      g.blit(THUMB,x,y,w,h,0F,0F,60,60,60,60);
   }

   // Preserve the original mod's semantic state symbols, not R04's drawn ASCII arrows.
   static void controlSymbol(GuiGraphics g,int x,int y,int index,int state) {
      int u=index==2 ? 200+(state==1?10:0) : 179+(state==1?11:0);
      int v=index==1 && state==2 ? 76 : 66;
      g.blit(NATIVE_DROPPER,x,y,u,v,9,9);
   }

   private static void part(GuiGraphics g,int x,int y,int w,int h,int u,int v,int sw,int sh) {
      g.blit(STEEL,x,y,w,h,(float)u,(float)v,sw,sh,108,108);
   }
}
