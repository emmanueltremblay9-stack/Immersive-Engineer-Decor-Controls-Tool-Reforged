package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.Gui11V5CRuntimeGeometry;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Client compositor for the owner-selected GUI11-R3 V5-Corrigee runtime panels. */
public final class Gui11V5CArtwork {
   private static final String MOD = "immersive_engineer_decor_controls_tool_reforged";
   private Gui11V5CArtwork() {}

   public static ResourceLocation texture(MachineKind kind) {
      return ResourceLocation.fromNamespaceAndPath(MOD, Gui11V5CRuntimeGeometry.texturePath(kind));
   }

   public static void draw(GuiGraphics graphics, MachineMenu menu) {
      draw(graphics, menu, 0, 0);
   }

   /** Draw at the actual vanilla container origin; fitted custom screens use (0,0). */
   public static void draw(GuiGraphics graphics, MachineMenu menu, int x, int y) {
      MachineKind kind = menu.kind();
      int width = Gui11V5CRuntimeGeometry.width(kind);
      int height = Gui11V5CRuntimeGeometry.height(kind);
      int sourceWidth = Gui11V5CRuntimeGeometry.sourceWidth(kind);
      int sourceHeight = Gui11V5CRuntimeGeometry.sourceHeight(kind);
      graphics.blit(texture(kind), x, y, width, height, 0, 0,
         sourceWidth, sourceHeight, sourceWidth, sourceHeight);
      drawEnergy(graphics, menu, x, y);
      drawEffects(graphics, menu, false, x, y);
      drawFluidState(graphics, menu, x, y);
   }

   /** The glass is inset from the source component; neutral art remains untouched at zero. */
   public static void drawEnergy(GuiGraphics graphics, MachineMenu menu) {
      drawEnergy(graphics, menu, 0, 0);
   }

   public static void drawEnergy(GuiGraphics graphics, MachineMenu menu, int originX, int originY) {
      Gui11V5CRuntimeGeometry.Rect component = Gui11V5CRuntimeGeometry.energy(menu.kind());
      if (component == null || menu.energyCapacity() <= 0 || menu.energyStored() <= 0) return;
      boolean vertical = component.height() > component.width();
      int insetX = vertical ? 3 : 4;
      int insetY = vertical ? 4 : 3;
      int x = component.x() + insetX;
      int y = component.y() + insetY;
      int width = component.width() - insetX * 2;
      int height = component.height() - insetY * 2;
      int filled = energyFillPixels(menu.kind(), menu.energyStored(), menu.energyCapacity());
      if (filled <= 0) return;
      // Contract FE tracks are vertical bottom-up or horizontal left-right. The neutral glass
      // and bezel pixels stay visible around the fill at every energy level.
      if (vertical) graphics.fill(originX + x, originY + y + height - filled, originX + x + width, originY + y + height, 0xFFE4A43C);
      else graphics.fill(originX + x, originY + y, originX + x + filled, originY + y + height, 0xFFE4A43C);
   }

   /** Exact logical fill length used by each measured FE well; zero stays visually inactive. */
   public static int energyFillPixels(MachineKind kind, int stored, int capacity) {
      Gui11V5CRuntimeGeometry.Rect component = Gui11V5CRuntimeGeometry.energy(kind);
      if (component == null || stored <= 0 || capacity <= 0) return 0;
      boolean vertical = component.height() > component.width();
      int inset = vertical ? 4 : 3;
      int length = (vertical ? component.height() : component.width()) - inset * 2;
      return (int)((long)Math.min(stored, capacity) * length / capacity);
   }

   public static void drawEffects(GuiGraphics graphics, MachineMenu menu, boolean previewHovered) {
      drawEffects(graphics, menu, previewHovered, 0, 0);
   }

   public static void drawEffects(GuiGraphics graphics, MachineMenu menu, boolean previewHovered, int originX, int originY) {
      for (Gui11V5CRuntimeGeometry.Effect effect : Gui11V5CRuntimeGeometry.effects(menu.kind())) {
         if (effect.key().equals("preview_focus") && !previewHovered) continue;
         if (effect.key().contains("manual-press")) continue; // transient press feedback needs an input edge
         if (effect.key().contains("indicator") || effect.key().contains("feedback")) continue; // driven by the real control renderer
         if (!active(menu, effect.key()) && !effect.key().equals("preview_focus")) continue;
         float pulse = 0.55F + 0.45F * (float)Math.sin(System.currentTimeMillis() / 230.0D);
         int alpha = Math.max(1, Math.min(255, Math.round(255 * effect.maxOpacity() * pulse)));
         Gui11V5CRuntimeGeometry.Rect bounds = effect.bounds();
         int color = (alpha << 24) | effect.rgb();
         graphics.fill(originX + bounds.x(), originY + bounds.y(), originX + bounds.x() + bounds.width(), originY + bounds.y() + bounds.height(), color);
      }
   }

   private static boolean active(MachineMenu menu, String effect) {
      if (menu.kind() == MachineKind.SMALL_LAB_FURNACE) {
         return switch (effect) {
            case "lab-flame" -> menu.field(4) > 0; // synchronized burn state, independent of recipe progress
            case "transfer-arrow-glint" -> menu.progress() > 0;
            default -> false;
         };
      }
      if (menu.kind() == MachineKind.SMALL_ELECTRICAL_FURNACE) {
         return switch (effect) {
            case "heat-wave-relief" -> ElectricalFurnaceArtwork.gui11HeatWaveVisible(menu.field(0), menu.field(7));
            case "active-copper-reflection" -> ElectricalFurnaceArtwork.gui11CopperReflectionVisible(menu.field(7));
            default -> false;
         };
      }
      return switch (menu.kind()) {
         case SMALL_FREEZER -> menu.freezerProgressTicks() > 0 && menu.freezerPhase() > 0;
         case SMALL_MINERAL_SMELTER -> menu.progress() > 0 || (menu.field(MachineMenu.SMS_UI_FLAGS) & 1) != 0;
         case SMALL_BLOCK_BREAKER -> menu.progress() > 0;
         case SMALL_WASTE_INCINERATOR -> menu.activityTicks() > 0;
         case SMALL_TREE_CUTTER, SMALL_MILKING_MACHINE -> menu.activityTicks() > 0;
         default -> false;
      };
   }

   /** Synchronized fluid shown through the two new artwork-only glass windows. */
   private static void drawFluidState(GuiGraphics graphics, MachineMenu menu, int originX, int originY) {
      int amount = menu.fluidAmount();
      if (amount <= 0 || menu.fluidCapacity() <= 0) return;
      Gui11V5CRuntimeGeometry.Rect glass = Gui11V5CRuntimeGeometry.fluidDisplay(menu.kind());
      if (glass == null) return;
      int fill = (int)((long)Math.min(amount, menu.fluidCapacity()) * glass.height() / menu.fluidCapacity());
      if (fill <= 0) return;
      int code = menu.kind() == MachineKind.SMALL_FLUID_FUNNEL ? menu.smallFluidFunnelFluidCode() : 1;
      int color = RetainedFluidMachineArtwork.fluidColor(code);
      int top = glass.y() + glass.height() - fill;
      for (int row = top; row < glass.y() + glass.height(); row++) {
         int offset = Math.min(3, Math.min(row - top, glass.y() + glass.height() - 1 - row));
         graphics.fill(originX + glass.x() + offset, originY + row,
            originX + glass.x() + glass.width() - offset, originY + row + 1, color);
      }
   }
}
