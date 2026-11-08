package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.FluidMachineRedesignLayout;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** The retained V1 originals plus only synchronized stored fluid. No activity fiction. */
final class RetainedFluidMachineArtwork {
   private RetainedFluidMachineArtwork() {}

   static void render(GuiGraphics graphics, MachineMenu menu, int x, int y) {
      boolean funnel = menu.kind() == MachineKind.SMALL_FLUID_FUNNEL;
      String path = funnel ? "small_fluid_funnel_v1/background" : "passive_fluid_accumulator_v1/background";
      var texture = ResourceLocation.fromNamespaceAndPath("immersive_engineer_decor_controls_tool_reforged", "textures/gui/" + path + ".png");
      graphics.blit(texture, x, y, FluidMachineRedesignLayout.SIZE, FluidMachineRedesignLayout.SIZE,
         0.0F, 0.0F, FluidMachineRedesignLayout.SOURCE_SIZE, FluidMachineRedesignLayout.SOURCE_SIZE,
         FluidMachineRedesignLayout.SOURCE_SIZE, FluidMachineRedesignLayout.SOURCE_SIZE);
      var tank = funnel ? FluidMachineRedesignLayout.FUNNEL_TANK : FluidMachineRedesignLayout.ACCUMULATOR_TANK;
      int code = funnel ? menu.smallFluidFunnelFluidCode() : 1;
      int fill = FluidMachineRedesignLayout.fillHeight(menu.fluidAmount(), menu.fluidCapacity(), tank.height());
      if (fill == 0 || code <= 0) return;
      // Translucency retains the original glass reflections; there is no opaque
      // replacement rectangle, sample fill, rain animation or simulated inlet flow.
      int color = fluidColor(code);
      int firstRow = tank.height() - fill;
      for (int row = firstRow; row < tank.height(); row++) {
         int inset = FluidMachineRedesignLayout.glassInset(tank, row);
         int left = x + tank.x() + inset;
         int right = x + tank.right() - inset;
         int top = y + tank.y() + row;
         graphics.fill(left, top, right, top + 1, color);
         if (row == firstRow && right - left > 2) graphics.fill(left + 1, top, right - 1, top + 1, 0x66FFFFFF);
      }
   }

   static int fluidColor(int code) {
      return switch (code) {
         case 1 -> 0xC03F76E4;
         case 2 -> 0xC0F06A28;
         case 3 -> 0xD0E6E1D5;
         default -> 0xC08296A0;
      };
   }
}
