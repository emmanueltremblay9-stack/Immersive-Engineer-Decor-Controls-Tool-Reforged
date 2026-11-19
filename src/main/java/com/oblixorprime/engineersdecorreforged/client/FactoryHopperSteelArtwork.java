package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Specialist candidate v2: presentation only; all native geometry and states are retained. */
final class FactoryHopperSteelArtwork {
   private static final ResourceLocation STEEL = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/factory_steel_v2/hopper.png");
   private FactoryHopperSteelArtwork() {}
   static void render(GuiGraphics g, MachineMenu menu, ResourceLocation original, int x, int y) {
      // Six raster texels per logical pixel, no change to the 176x166 menu.
      g.blit(STEEL,x,y,176,166,0F,0F,1056,996,1056,996);
      FactorySteelFrame.slots(g,menu,x,y);
      // Candidate is neutral. Only native symbols and synchronized widget overlays indicate state.
      for(int bx:new int[]{133,145,159}) g.blit(original,x+bx,y+49,bx,49,9,9);
   }
}
