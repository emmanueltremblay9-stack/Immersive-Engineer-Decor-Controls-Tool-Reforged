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
      // Owner-source decoration extends 8px sideways and 16px above the unchanged 176x166 menu.
      // Slot/widget/menu geometry stays unchanged; MachineScreen protects the visible border from outside clicks.
      g.blit(STEEL,x-8,y-16,192,182,0F,0F,1152,1092,1152,1092);
      FactorySteelFrame.slots(g,menu,x,y);
      // Candidate is neutral. Only native symbols and synchronized widget overlays indicate state.
      for(int bx:new int[]{133,145,159}) g.blit(original,x+bx,y+49,bx,49,9,9);
   }
}
