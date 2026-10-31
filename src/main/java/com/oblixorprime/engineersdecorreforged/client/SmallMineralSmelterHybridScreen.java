package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.network.MachineActionPayload;
import com.oblixorprime.engineersdecorreforged.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Draft runnable, in-game GUI for the hybrid smelter. The source-matched metal
 * faceplate (1168x1346) is a separately tracked art gate; this preview uses
 * the already integrated IEDCTR steel base while preserving the 27+9 slots.
 * All numeric displays come from the server's MachineMenu projection.
 */
public final class SmallMineralSmelterHybridScreen extends AbstractContainerScreen<MachineMenu> {
   private static final ResourceLocation STEEL = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/small_mineral_smelter_hybrid_v05/background.png");
   private static final int WHITE = 0xFFE8E1CE, MUTED = 0xFFB1AFA8, AMBER = 0xFFFFB348,
      STEEL_DARK = 0xFF24272A, INSET = 0xFF151718, EDGE = 0xFF656462, RED = 0xFFFF6343;
   private int lastSentRevision = -1;
   private boolean outputDrawerOpen;

   public SmallMineralSmelterHybridScreen(MachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      imageWidth = menu.layout().imageWidth();
      imageHeight = menu.layout().imageHeight();
      inventoryLabelX = -1000;
      titleLabelX = -1000;
      menu.setSmelterOutputDrawerOpen(false);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      renderTooltip(graphics, mouseX, mouseY);
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      int x = leftPos, y = topPos;
      // Exact v0.5 blank faceplate at the logical 234x270 canvas dimensions.
      graphics.pose().pushPose();
      graphics.pose().translate(x, y, 0);
      graphics.pose().scale(imageWidth / 1168.0F, imageHeight / 1346.0F, 1.0F);
      graphics.blit(STEEL, 0, 0, 0, 0, 1168, 1346, 1168, 1346);
      graphics.pose().popPose();

      int thermal = Mth.clamp(menu.field(10), 0, 100);
      int phase = thermal >= 100 ? 3 : thermal >= 90 ? 2 : thermal >= 5 ? 1 : 0;
      int fire = thermal < 5 ? 0xFF69AFC4 : thermal < 90 ? AMBER : thermal < 100 ? 0xFFE46A25 : RED;
      // Source artwork contains the blank phase lamps and meter apertures.
      for (int i = 0; i < 4; i++) {
         int yy = y + 45 + i * 10;
         graphics.fill(x + 90, yy, x + 95, yy + 6, i == phase ? fire : 0xFF18211E);
      }
      gauge(graphics, x + 19, y + 114, 87, 5, thermal, 100, fire);
      int amount = Mth.clamp(menu.fluidAmount(), 0, Math.max(1, menu.fluidCapacity()));
      int fill = Mth.clamp(amount * 40 / Math.max(1, menu.fluidCapacity()), 0, 40);
      if (fill > 0) {
         graphics.fill(x + 124, y + 145 - fill, x + 138, y + 145, 0xFFE7611D);
         graphics.fill(x + 124, y + 145 - fill, x + 138, y + 146 - fill, AMBER);
      }
      gauge(graphics, x + 174, y + 111, 43, 5, menu.energyStored(), menu.energyCapacity(), AMBER);
      if (outputDrawerOpen) {
         graphics.fill(x + 155, y + 42, x + 216, y + 77, 0xFF797467);
         graphics.fill(x + 156, y + 43, x + 215, y + 76, 0xFF141A19);
         // The real output slot renders above this backing when the drawer is open.
         graphics.fill(x + 168, y + 47, x + 190, y + 69, 0xFF343B36);
         graphics.fill(x + 169, y + 48, x + 189, y + 68, 0xFF0B1010);
      } else {
         int mode = Mth.clamp(menu.field(7), 0, 2);
         for (int i = 0; i < 3; i++) {
            int top = y + 42 + i * 11;
            graphics.fill(x + 176, top, x + 215, top + 1, mode == i ? AMBER : EDGE);
            graphics.fill(x + 176, top + 8, x + 215, top + 9, mode == i ? AMBER : EDGE);
         }
      }
   }

   private static void panel(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
      graphics.fill(x1, y1, x2, y2, 0xFF75706A);
      graphics.fill(x1 + 2, y1 + 2, x2 - 2, y2 - 2, INSET);
      graphics.fill(x1 + 2, y1 + 2, x2 - 2, y1 + 4, STEEL_DARK);
      graphics.fill(x1 + 3, y1 + 3, x1 + 5, y1 + 5, 0xFFB7AEA2);
      graphics.fill(x2 - 5, y2 - 5, x2 - 3, y2 - 3, 0xFFB7AEA2);
   }

   private static void gauge(GuiGraphics graphics, int x, int y, int w, int h, int value, int max, int color) {
      graphics.fill(x, y, x + w, y + h, 0xFF080A0C);
      int filled = max <= 0 ? 0 : Mth.clamp(value * w / max, 0, w);
      if (filled > 0) graphics.fill(x, y, x + filled, y + h, color);
   }

   private void drawSmall(GuiGraphics graphics, String value, int x, int y, int color, float scale) {
      graphics.pose().pushPose();
      graphics.pose().translate(x, y, 0);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(font, value, 0, 0, color, false);
      graphics.pose().popPose();
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      int thermal = Mth.clamp(menu.field(10), 0, 100);
      int phase = thermal >= 100 ? 3 : thermal >= 90 ? 2 : thermal >= 5 ? 1 : 0;
      String[] phases = {"COLD", "HOT", "MAGMA", "LAVA"};
      drawSmall(graphics, thermal + "%  " + phases[phase], 20, 107, AMBER, 0.65F);
      drawSmall(graphics, "COMPARATOR " + menu.comparatorOutput(), 36, 127, WHITE, 0.58F);
      drawSmall(graphics, thermal >= 5 ? "HOT SURFACE" : "SURFACE COOL", 20, 140,
         thermal >= 5 ? RED : MUTED, 0.68F);
      int queued = menu.field(9);
      if (queued >= 0 && queued <= 2) {
         drawSmall(graphics, "NEXT: " + new String[]{"LAVA", "MAGMA", "OBSIDIAN"}[queued], 20, 152, AMBER, 0.57F);
      }
      drawSmall(graphics, menu.fluidAmount() + " mB", 148, 102, WHITE, 0.63F);
      drawSmall(graphics, "/ " + menu.fluidCapacity() + " mB", 148, 110, MUTED, 0.52F);
      drawSmall(graphics, menu.energyStored() + " FE", 185, 102, WHITE, 0.53F);
      drawSmall(graphics, menu.field(13) + " FE/t", 191, 120, WHITE, 0.58F);
      drawSmall(graphics, menu.field(11) != 0 ? "HIGH" : "LOW", 190, 141,
         menu.field(11) != 0 ? RED : 0xFF81D77B, 0.65F);
      if (outputDrawerOpen) {
         drawSmall(graphics, "ITEM OUTPUT", 191, 49, AMBER, 0.54F);
         drawSmall(graphics, "CLICK HEADER TO CLOSE", 158, 71, MUTED, 0.47F);
      } else {
         int selected = Mth.clamp(menu.field(7), 0, 2);
         String[] names = {"LAVA", "MAGMA BLOCK", "OBSIDIAN"};
         for (int i = 0; i < 3; i++) {
            drawSmall(graphics, names[i], 183, 44 + i * 11,
               selected == i ? AMBER : WHITE, 0.60F);
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (isHovering(162, 35, 54, 8, mouseX, mouseY)) {
            outputDrawerOpen = !outputDrawerOpen;
            menu.setSmelterOutputDrawerOpen(outputDrawerOpen);
            return true;
         }
         if (!outputDrawerOpen) {
            for (int mode = 0; mode < 3; mode++) {
               if (isHovering(176, 42 + mode * 11, 39, 9, mouseX, mouseY)) {
                  int revision = menu.field(14);
                  if (lastSentRevision != revision) {
                     PacketDistributor.sendToServer(new MachineActionPayload(
                        menu.containerId, ModNetworking.MINERAL_SMELTER_SELECT_MODE, mode, revision));
                     lastSentRevision = revision;
                  }
                  return true;
               }
            }
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void removed() {
      menu.setSmelterOutputDrawerOpen(false);
      super.removed();
   }
}
