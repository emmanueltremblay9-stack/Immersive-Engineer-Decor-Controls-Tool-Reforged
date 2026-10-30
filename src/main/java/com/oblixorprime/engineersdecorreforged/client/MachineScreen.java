package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.network.MachineActionPayload;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.oblixorprime.engineersdecorreforged.utility.MachineLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public abstract class MachineScreen extends AbstractContainerScreen<MachineMenu> {
   private static final int FRAME = -14934755;
   private static final int BODY = -12960450;
   private static final int BODY_DARK = -13618380;
   private static final int PANEL = -13881552;
   private static final int PANEL_EDGE = -10328475;
   private static final int PANEL_HIGHLIGHT = -8946823;
   private static final int TEXT = -1646126;
   private static final int MUTED = -3949142;
   private static final int HOVER = -1917873;
   private static final int LABEL_BACKDROP = -1441327079;
   private static final int SLOT_FACE = -12236470;
   private static final int SLOT_INNER = -14276310;
   private final MachineScreen.Profile profile;

   protected MachineScreen(MachineMenu menu, Inventory inventory, Component title, MachineScreen.Profile profile) {
      super(menu, inventory, title);
      this.profile = profile;
      this.imageWidth = menu.layout().imageWidth();
      this.imageHeight = menu.layout().imageHeight();
      this.titleLabelX = 8;
      this.titleLabelY = 6;
      this.inventoryLabelX = menu.layout().playerInventoryX();
      this.inventoryLabelY = menu.layout().playerInventoryY() - 10;
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderSoftBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderOriginalHoverOverlay(graphics, mouseX, mouseY);
      this.renderTooltip(graphics, mouseX, mouseY);
      this.renderMachineTooltip(graphics, mouseX, mouseY);
   }

   private void renderSoftBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (this.minecraft != null && this.minecraft.level != null) {
         graphics.fill(0, 0, this.width, this.height, 855638016);
      } else {
         this.renderBackground(graphics, mouseX, mouseY, partialTick);
      }
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      MachineLayout layout = ((MachineMenu)this.menu).layout();
      int x = this.leftPos;
      int y = this.topPos;
      if (layout.style() == MachineLayout.LayoutStyle.ORIGINAL_TEXTURE) {
         ResourceLocation texture = this.backgroundTexture(layout);
         this.drawOriginalTextureBackdrop(graphics, x, y);
         if (((MachineMenu)this.menu).kind() == MachineKind.SMALL_BLOCK_BREAKER) {
            graphics.blit(texture, x, y, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
         } else {
            graphics.blit(texture, x, y, 0, 0, this.imageWidth, this.imageHeight);
         }
         this.drawOriginalWidgets(graphics, texture, x, y);
         if (((MachineMenu)this.menu).kind() == MachineKind.SMALL_BLOCK_BREAKER) {
            this.drawBlockBreakerTarget(graphics, x, y);
         }
      } else {
         this.drawFrame(graphics, x, y);
         this.drawPanels(graphics, layout, x, y);
         this.drawMachineWidget(graphics, layout, x, y);
         this.drawMachineSlots(graphics, layout, x, y);
         this.drawPlayerSlots(graphics, layout, x, y);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return ((MachineMenu)this.menu).layout().style() == MachineLayout.LayoutStyle.ORIGINAL_TEXTURE && this.handleOriginalClick(mouseX, mouseY)
         ? true
         : super.mouseClicked(mouseX, mouseY, button);
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      MachineLayout layout = ((MachineMenu)this.menu).layout();
      if (((MachineMenu)this.menu).kind() == MachineKind.SMALL_BLOCK_BREAKER) {
         this.drawBlockBreakerLabels(graphics);
      } else if (layout.style() != MachineLayout.LayoutStyle.ORIGINAL_TEXTURE) {
         this.drawBoundedString(graphics, this.profile.title(), this.titleLabelX, this.titleLabelY, this.imageWidth - this.titleLabelX - 6, -1646126, false);
         this.drawTinyString(graphics, "Inv", this.inventoryLabelX, this.inventoryLabelY + 1, 28, -3949142, false);
         this.drawPanelLabels(graphics, layout);
         this.drawStatusText(graphics, layout);
      }
   }

   private void drawBlockBreakerTarget(GuiGraphics graphics, int x, int y) {
      MachineMenu menu = (MachineMenu)this.menu;
      if (!menu.blockBreakerTargetStack().isEmpty()) {
         graphics.renderItem(menu.blockBreakerTargetStack(), x + 22, y + 46);
      }

      int total = menu.field(3);
      int elapsed = menu.field(1);
      int fill = total > 0 ? Mth.clamp(elapsed * 65 / total, 0, 65) : 0;
      if (fill > 0) {
         graphics.fill(x + 15, y + 100, x + 15 + fill, y + 103, 0xFF7FA77A);
      }
   }

   private void drawBlockBreakerLabels(GuiGraphics graphics) {
      MachineMenu menu = (MachineMenu)this.menu;
      this.drawTinyCenteredString(graphics, "Small Block Breaker", 105, 7, 144, -1646126);
      this.drawTinyString(graphics, "Target Block", 16, 25, 77, -1646126, false);
      this.drawTinyString(graphics, "Output", 109, 25, 34, -1646126, false);
      this.drawTinyString(graphics, menu.blockBreakerTargetName(), 48, 39, 46, -1646126, false);
      this.drawTinyString(graphics, "Front face", 48, 46, 44, -3949142, false);

      float hardness = menu.blockBreakerHardness();
      String hardnessText = hardness < 0.0F ? "Hardness: unbreakable" : String.format(java.util.Locale.ROOT, "Hardness: %.2f", hardness);
      this.drawTinyString(graphics, hardnessText, 15, 55, 78, hardness < 0.0F ? 0xFFFF7777 : -3949142, false);
      this.drawTinyString(graphics, "Work: " + menu.field(1) + "/" + Math.max(0, menu.field(3)) + " ticks", 15, 76, 80, -1646126, false);
      this.drawTinyString(graphics, menu.progress() + "%", 81, 99, 12, -1646126, false);
      String energyCost = menu.blockBreakerEnergyCostPerBlock() > 0 ? menu.blockBreakerEnergyCostPerBlock() + " FE/block" : "FE/block: —";
      this.drawTinyString(graphics, energyCost, 16, 114, 78, -3949142, false);

      this.drawTinyString(graphics, "Behavior & Operation", 106, 65, 87, -1646126, false);
      this.drawTinyString(graphics, "Redstone:", 106, 79, 39, -3949142, false);
      this.drawTinyString(graphics, menu.blockBreakerRedstonePowered() ? "HIGH" : "LOW", 151, 79, 26,
         menu.blockBreakerRedstonePowered() ? 0xFFFF7777 : 0xFF9BCB83, false);
      this.drawTinyString(graphics, "HIGH stops work", 106, 89, 75, -3949142, false);
      this.drawTinyString(graphics, "Energy:", 106, 100, 31, -3949142, false);
      this.drawTinyString(graphics, menu.energyStored() + " FE", 140, 100, 43, -1646126, false);
      this.drawTinyString(graphics, "FE inlet: underside", 106, 110, 80, -3949142, false);
      this.drawTinyString(graphics, "Status:", 106, 120, 30, -3949142, false);
      this.drawTinyString(graphics, blockBreakerStatus(menu.blockBreakerStatus()), 139, 120, 52, -1646126, false);
      this.drawTinyString(graphics, "Rear only", 163, 44, 34, -3949142, false);
      this.drawTinyString(graphics, "Q " + menu.blockBreakerPendingItems(), 163, 54, 35, -3949142, false);
      this.drawTinyString(graphics, "Inventory", 15, 135, 42, -1646126, false);
   }

   private static String blockBreakerStatus(int state) {
      return switch (state) {
         case 1 -> "Stopped";
         case 2 -> "Waiting for FE";
         case 3 -> "Output full";
         case 4 -> "Working";
         case 5 -> "Cooldown";
         case 6 -> "Unbreakable";
         default -> "No target";
      };
   }

   private void renderOriginalHoverOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
      if (((MachineMenu)this.menu).layout().style() == MachineLayout.LayoutStyle.ORIGINAL_TEXTURE) {
         MachineLayout.MachineSlot slot = this.hoveredMachineSlot(mouseX, mouseY);
         if (slot != null) {
            this.drawHoverOutline(graphics, this.leftPos + slot.x() - 1, this.topPos + slot.y() - 1, 18, 18, -1917873);
         }

         if (((MachineMenu)this.menu).kind() == MachineKind.FACTORY_PLACER) {
            this.drawFactoryPlacerControlHover(graphics, mouseX, mouseY);
         }
      }
   }

   private MachineLayout.MachineSlot hoveredMachineSlot(int mouseX, int mouseY) {
      for (MachineLayout.MachineSlot slot : ((MachineMenu)this.menu).layout().machineSlots()) {
         if (this.isHovering(slot.x(), slot.y(), 16, 16, mouseX, mouseY)) {
            return slot;
         }
      }

      return null;
   }

   private void drawLabelBackdrop(GuiGraphics graphics, int x, int y, int width, int height) {
      graphics.fill(x, y, x + width, y + height, -1441327079);
   }

   private void drawBoundedString(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color, boolean shadow) {
      if (maxWidth > 0 && !text.isEmpty()) {
         graphics.drawString(this.font, this.fitText(text, maxWidth), x, y, color, shadow);
      }
   }

   private void drawTinyString(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color, boolean shadow) {
      this.drawScaledBoundedString(graphics, text, x, y, maxWidth, color, shadow, 0.72F);
   }

   private void drawTinyCenteredString(GuiGraphics graphics, String text, int centerX, int y, int maxWidth, int color) {
      String fitted = this.fitText(text, Math.max(1, (int)(maxWidth / 0.72F)));
      int scaledWidth = Mth.ceil(this.font.width(fitted) * 0.72F);
      this.drawTinyString(graphics, fitted, centerX - scaledWidth / 2, y, maxWidth, color, false);
   }

   private void drawScaledBoundedString(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color, boolean shadow, float scale) {
      if (maxWidth > 0 && !text.isEmpty()) {
         String fitted = this.fitText(text, Math.max(1, (int)(maxWidth / scale)));
         graphics.pose().pushPose();
         graphics.pose().scale(scale, scale, 1.0F);
         graphics.drawString(this.font, fitted, Mth.floor(x / scale), Mth.floor(y / scale), color, shadow);
         graphics.pose().popPose();
      }
   }

   private String fitText(String text, int maxWidth) {
      if (this.font.width(text) <= maxWidth) {
         return text;
      }

      String ellipsis = "...";
      int textWidth = maxWidth - this.font.width(ellipsis);
      return textWidth <= 0 ? "" : this.font.plainSubstrByWidth(text, textWidth) + ellipsis;
   }

   private void drawHoverOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
      graphics.fill(x, y, x + width, y + 1, color);
      graphics.fill(x, y + height - 1, x + width, y + height, color);
      graphics.fill(x, y, x + 1, y + height, color);
      graphics.fill(x + width - 1, y, x + width, y + height, color);
   }

   private void drawOriginalWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      switch (((MachineMenu)this.menu).kind()) {
         case FACTORY_HOPPER:
            this.drawFactoryHopperWidgets(graphics, texture, x0, y0);
            break;
         case FACTORY_DROPPER:
            this.drawFactoryDropperWidgets(graphics, texture, x0, y0);
            break;
         case FACTORY_PLACER:
            this.drawFactoryPlacerWidgets(graphics, texture, x0, y0);
            break;
         case SMALL_LAB_FURNACE:
            this.drawLabFurnaceWidgets(graphics, texture, x0, y0);
            break;
         case SMALL_ELECTRICAL_FURNACE:
            this.drawElectricalFurnaceWidgets(graphics, texture, x0, y0);
      }
   }

   private void drawOriginalTextureBackdrop(GuiGraphics graphics, int x0, int y0) {
      if (((MachineMenu)this.menu).kind() == MachineKind.FACTORY_PLACER) {
         int x = x0 + 126;
         int y = y0;
         int width = 50;
         int height = 43;
         graphics.fill(x, y, x + width, y + height, -14671326);
         graphics.fill(x, y, x + width, y + 1, -10723232);
         graphics.fill(x, y, x + 1, y + height, -12170678);
         graphics.fill(x, y + height - 1, x + width, y + height, -15592685);
         graphics.fill(x + width - 1, y, x + width, y + height, -15592685);
      }
   }

   private void drawFactoryHopperWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      int slotIndex = Mth.clamp(((MachineMenu)this.menu).field(6), 0, 17);
      graphics.blit(texture, x0 + 10 + slotIndex % 6 * 18, y0 + 8 + slotIndex / 6 * 17, 200, 8, 18, 18);
      int[] rangeMarkers = new int[]{133, 141, 149, 157, 166};
      graphics.blit(texture, x0 + rangeMarkers[Mth.clamp(((MachineMenu)this.menu).field(0), 0, 4)] - 2, y0 + 14, 179, 40, 5, 5);
      int periodX = Mth.clamp((int)Math.round(33.5 * ((MachineMenu)this.menu).field(3) / 100.0 + 1.0), 0, 34);
      graphics.blit(texture, x0 + 132 - 2 + periodX, y0 + 27, 179, 40, 5, 5);
      graphics.blit(texture, x0 + 133 - 2 + Mth.clamp(((MachineMenu)this.menu).field(1), 1, 32), y0 + 40, 179, 40, 5, 5);
      if (((MachineMenu)this.menu).field(5) != 0) {
         graphics.blit(texture, x0 + 133, y0 + 49, 217, 49, 9, 9);
      }

      int logic = ((MachineMenu)this.menu).field(2);
      int inverterOffsetX = (logic & 1) != 0 ? 11 : 0;
      int inverterOffsetY = (logic & 4) != 0 ? 10 : 0;
      graphics.blit(texture, x0 + 145, y0 + 49, 177 + inverterOffsetX, 49 + inverterOffsetY, 9, 9);
      int pulseOffset = (logic & 2) != 0 ? 9 : 0;
      graphics.blit(texture, x0 + 159, y0 + 49, 199 + pulseOffset, 49, 9, 9);
      if (((MachineMenu)this.menu).field(4) > 10 && System.currentTimeMillis() % 1000L < 500L) {
         graphics.blit(texture, x0 + 148, y0 + 22, 187, 22, 3, 3);
      }
   }

   private void drawFactoryDropperWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      int slotIndex = Mth.clamp(((MachineMenu)this.menu).field(15), 0, 11);
      graphics.blit(texture, x0 + 9 + slotIndex % 6 * 18, y0 + 5 + slotIndex / 6 * 17, 180, 45, 18, 18);

      for (int i = 0; i < 3; i++) {
         graphics.blit(texture, x0 + 31 + i * 36, y0 + 65, 180 + 6 * Mth.clamp(((MachineMenu)this.menu).field(12 + i), 0, 2), 38, 6, 6);
      }

      int height = 2 + (100 - Mth.clamp(((MachineMenu)this.menu).field(0), 0, 100)) * 21 / 100;
      graphics.blit(texture, x0 + 135, y0 + 12, 181, 4 + (23 - height), 3, height);
      graphics.blit(
         texture,
         x0 + 157 - 3 + Mth.clamp(((MachineMenu)this.menu).field(1), -100, 100) * 12 / 100,
         y0 + 22 - 3 - Mth.clamp(((MachineMenu)this.menu).field(2), -100, 100) * 12 / 100,
         180,
         30,
         7,
         7
      );
      graphics.blit(texture, x0 + 134 - 2 + Mth.clamp(((MachineMenu)this.menu).field(4), 1, 32), y0 + 45, 190, 31, 5, 5);
      int periodX = (int)Math.round(33.0 * ((MachineMenu)this.menu).field(6) / 100.0 + 1.0);
      graphics.blit(texture, x0 + 134 - 2 + Mth.clamp(periodX, 0, 33), y0 + 56, 190, 31, 5, 5);
      if (((MachineMenu)this.menu).field(11) != 0) {
         graphics.blit(texture, x0 + 114, y0 + 51, 189, 18, 9, 9);
      }

      int logic = ((MachineMenu)this.menu).field(5);
      graphics.blit(texture, x0 + 132, y0 + 66, 179 + ((logic & 1) != 0 ? 11 : 0), 66, 9, 9);
      graphics.blit(texture, x0 + 148, y0 + 66, 179 + ((logic & 2) != 0 ? 11 : 0), 66 + ((logic & 32) != 0 ? 10 : 0), 9, 9);
      graphics.blit(texture, x0 + 162, y0 + 66, 200 + ((logic & 16) != 0 ? 10 : 0), 66, 9, 9);
      if (((MachineMenu)this.menu).field(9) > 10 && System.currentTimeMillis() % 1000L < 500L) {
         graphics.blit(texture, x0 + 149, y0 + 51, 201, 39, 3, 3);
      }
   }

   private void drawFactoryPlacerWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      int slotIndex = Mth.clamp(((MachineMenu)this.menu).field(2), 0, 17);
      graphics.blit(texture, x0 + 10 + slotIndex % 6 * 18, y0 + 8 + slotIndex / 6 * 17, 200, 8, 18, 18);
      if (((MachineMenu)this.menu).field(1) != 0) {
         graphics.blit(texture, x0 + 133, y0 + 49, 217, 49, 9, 9);
      }

      int logic = ((MachineMenu)this.menu).field(0);
      int inverterOffsetX = (logic & 1) != 0 ? 11 : 0;
      int inverterOffsetY = (logic & 4) != 0 ? 10 : 0;
      graphics.blit(texture, x0 + 145, y0 + 49, 177 + inverterOffsetX, 49 + inverterOffsetY, 9, 9);
      graphics.blit(texture, x0 + 159, y0 + 49, 199 + ((logic & 2) != 0 ? 9 : 0), 49, 9, 9);
   }

   private void drawLabFurnaceWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      if (((MachineMenu)this.menu).field(4) != 0) {
         int flame = this.fuelPixels(13, ((MachineMenu)this.menu).field(0), ((MachineMenu)this.menu).field(1));
         graphics.blit(texture, x0 + 61, y0 + 36 + 13 - flame, 176, 13 - flame, 14, flame);
      }

      graphics.blit(texture, x0 + 79, y0 + 38, 176, 15, 1 + this.progressPixels(17), 15);
   }

   private void drawElectricalFurnaceWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      if (((MachineMenu)this.menu).field(6) != 0) {
         int heat = Math.min(13, ((MachineMenu)this.menu).field(0) * 14 / 200);
         graphics.blit(texture, x0 + 62, y0 + 55 + 13 - heat, 177, 13 - heat, 13, heat);
      }

      graphics.blit(texture, x0 + 79, y0 + 30, 176, 15, 1 + this.progressPixels(17), 15);
      int energyMax = Math.max(((MachineMenu)this.menu).field(5), 1);
      int width = 32 * ((MachineMenu)this.menu).field(1) * 9 / 8 / (energyMax + 1);
      width = width >= 30 ? 32 : Mth.clamp((width + 4) / 8 * 8, 0, 32);
      if (width > 0) {
         graphics.blit(texture, x0 + 90, y0 + 54, 185, 30, width, 13);
      }

      switch (((MachineMenu)this.menu).field(4)) {
         case 0:
            graphics.blit(texture, x0 + 144, y0 + 57, 180, 57, 6, 9);
            break;
         case 1:
            graphics.blit(texture, x0 + 142, y0 + 58, 190, 58, 9, 6);
            break;
         case 2:
            graphics.blit(texture, x0 + 144, y0 + 56, 200, 57, 6, 9);
            break;
         case 3:
            graphics.blit(texture, x0 + 143, y0 + 58, 210, 58, 9, 6);
      }
   }

   private int progressPixels(int pixels) {
      int total = ((MachineMenu)this.menu).field(3);
      int elapsed = ((MachineMenu)this.menu).field(2);
      return total > 0 && elapsed > 0 ? Mth.clamp(elapsed * pixels / total, 0, pixels) : 0;
   }

   private int fuelPixels(int pixels, int burnTime, int fuelTime) {
      return fuelTime > 0 && burnTime > 0 ? Mth.clamp(burnTime * pixels / fuelTime, 0, pixels) : 0;
   }

   private void drawFrame(GuiGraphics graphics, int x, int y) {
      graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, -14934755);
      graphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, -10328475);
      graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, -13618380);
      graphics.fill(x + 4, y + 4, x + this.imageWidth - 4, y + this.imageHeight - 4, -12960450);
      graphics.fill(x + 5, y + 5, x + this.imageWidth - 5, y + 17, -13618380);
      graphics.fill(x + 5, y + 17, x + this.imageWidth - 5, y + 18, -14934755);
      graphics.fill(x + 3, y + this.imageHeight - 83, x + this.imageWidth - 3, y + this.imageHeight - 81, -14934755);
      this.drawCornerRivets(graphics, x, y);
   }

   private void drawPanels(GuiGraphics graphics, MachineLayout layout, int x, int y) {
      for (MachineLayout.Panel panel : layout.panels()) {
         graphics.fill(x + panel.x(), y + panel.y(), x + panel.x() + panel.width(), y + panel.y() + panel.height(), -14934755);
         graphics.fill(x + panel.x() + 1, y + panel.y() + 1, x + panel.x() + panel.width() - 1, y + panel.y() + panel.height() - 1, -10328475);
         graphics.fill(x + panel.x() + 2, y + panel.y() + 2, x + panel.x() + panel.width() - 2, y + panel.y() + panel.height() - 2, -13881552);
         graphics.fill(x + panel.x() + 2, y + panel.y() + 2, x + panel.x() + panel.width() - 2, y + panel.y() + 3, -8946823);
         graphics.fill(x + panel.x() + 1, y + panel.y() + 1, x + panel.x() + panel.width() - 1, y + panel.y() + 3, this.profile.accent());
      }
   }

   private void drawPanelLabels(GuiGraphics graphics, MachineLayout layout) {
      for (MachineLayout.Panel panel : layout.panels()) {
         int labelY = panel.y() - 8;
         if (labelY >= 20) {
            int labelX = panel.x() + 2;
            int labelWidth = Math.max(12, panel.width() - 4);
            this.drawTinyString(graphics, panel.label(), labelX, labelY, labelWidth, -3949142, false);
         }
      }
   }

   private void drawCornerRivets(GuiGraphics graphics, int x, int y) {
      int right = x + this.imageWidth - 8;
      int bottom = y + this.imageHeight - 8;
      this.drawRivet(graphics, x + 7, y + 7);
      this.drawRivet(graphics, right, y + 7);
      this.drawRivet(graphics, x + 7, bottom);
      this.drawRivet(graphics, right, bottom);
   }

   private void drawRivet(GuiGraphics graphics, int x, int y) {
      graphics.fill(x, y, x + 3, y + 3, -15461099);
      graphics.fill(x + 1, y, x + 2, y + 1, -8946823);
      graphics.fill(x + 1, y + 1, x + 2, y + 2, -7697016);
   }

   private void drawMachineWidget(GuiGraphics graphics, MachineLayout layout, int x, int y) {
      switch (layout.style()) {
         case STORAGE:
         case ORIGINAL_TEXTURE:
         default:
            break;
         case CRAFTING:
            this.drawCraftingFlow(graphics, x + 119, y + 39);
            this.drawToolGlyph(graphics, x + 28, y + 42);
            break;
         case PROCESSOR:
            this.drawArrow(graphics, x + 75, y + 40, this.workWidth(24));
            this.drawMeter(graphics, x + 78, y + 61, 23, ((MachineMenu)this.menu).progress(), 100, this.profile.accent());
            break;
         case FLUID:
            this.drawTank(graphics, x + 83, y + 21, 12, 50, ((MachineMenu)this.menu).fluidAmount(), ((MachineMenu)this.menu).fluidCapacity());
            this.drawArrow(graphics, x + 63, y + 42, 10);
            this.drawArrow(graphics, x + 103, y + 42, 10);
            break;
         case ENERGY:
            this.drawSolarCells(graphics, x + 42, y + 22);
            this.drawMeter(graphics, x + 62, y + 43, 52, ((MachineMenu)this.menu).energyStored(), ((MachineMenu)this.menu).energyCapacity(), -1917873);
            this.drawSignal(graphics, x + 115, y + 52);
            break;
         case AUTOMATION:
            this.drawArrow(graphics, x + 80, y + 45, this.workWidth(24));
            this.drawSignal(graphics, x + 91, y + 33);
      }
   }

   private void drawStatusText(GuiGraphics graphics, MachineLayout layout) {
      switch (layout.style()) {
         case STORAGE:
            this.drawTinyString(graphics, "Cmp " + ((MachineMenu)this.menu).comparatorOutput(), 138, 77, 32, -3949142, false);
         case ORIGINAL_TEXTURE:
         default:
            break;
         case CRAFTING:
            this.drawTinyCenteredString(graphics, "3x3 Grid", 89, 77, 48, -3949142);
            break;
         case PROCESSOR:
            this.drawTinyString(graphics, this.profile.process(), 76, 22, 56, -3949142, false);
            this.drawTinyString(graphics, "Work " + ((MachineMenu)this.menu).progress(), 116, 77, 54, -3949142, false);
            break;
         case FLUID:
            this.drawTinyCenteredString(graphics, ((MachineMenu)this.menu).fluidAmount() + " mB", 89, 77, 48, -3949142);
            break;
         case ENERGY:
            this.drawTinyString(graphics, "FE " + ((MachineMenu)this.menu).energyStored(), 62, 77, 54, -3949142, false);
            this.drawTinyString(graphics, "C" + ((MachineMenu)this.menu).comparatorOutput(), 134, 77, 24, -3949142, false);
            break;
         case AUTOMATION:
            this.drawTinyString(graphics, this.profile.process(), 83, 20, 56, -3949142, false);
            this.drawTinyString(graphics, "C" + ((MachineMenu)this.menu).comparatorOutput(), 138, 77, 24, -3949142, false);
      }
   }

   private ResourceLocation backgroundTexture(MachineLayout layout) {
      return ResourceLocation.fromNamespaceAndPath("immersive_engineer_decor_controls_tool_reforged", "textures/gui/" + layout.textureName() + ".png");
   }

   private void drawCraftingFlow(GuiGraphics graphics, int x, int y) {
      graphics.fill(x, y + 3, x + 10, y + 7, -14934755);
      graphics.fill(x + 1, y + 4, x + 7, y + 6, -10328475);
      graphics.fill(x + 7, y + 2, x + 10, y + 8, -10328475);
   }

   private void drawToolGlyph(GuiGraphics graphics, int x, int y) {
      graphics.fill(x, y, x + 3, y + 9, -9149877);
      graphics.fill(x + 3, y + 1, x + 8, y + 4, -10328475);
      graphics.fill(x + 5, y + 4, x + 9, y + 6, -10328475);
      graphics.fill(x + 4, y + 2, x + 6, y + 3, -8946823);
   }

   private boolean handleOriginalClick(double mouseX, double mouseY) {
      return switch (((MachineMenu)this.menu).kind()) {
         case FACTORY_HOPPER -> this.handleFactoryHopperClick(mouseX, mouseY);
         case FACTORY_DROPPER -> this.handleFactoryDropperClick(mouseX, mouseY);
         case FACTORY_PLACER -> this.handleFactoryPlacerClick(mouseX, mouseY);
         default -> false;
         case SMALL_ELECTRICAL_FURNACE -> this.handleElectricalFurnaceClick(mouseX, mouseY);
      };
   }

   private boolean handleFactoryHopperClick(double mouseX, double mouseY) {
      int mx = (int)(mouseX - this.leftPos + 0.5);
      if (!this.isHovering(126, 1, 49, 60, mouseX, mouseY)) {
         return false;
      }

      if (this.isHovering(128, 9, 44, 10, mouseX, mouseY)) {
         int range = this.sliderValue(mx, 133, 34, 0, 4, ((MachineMenu)this.menu).field(0), 1);
         this.sendAction(1, range);
      } else if (this.isHovering(128, 21, 44, 10, mouseX, mouseY)) {
         this.sendAction(4, this.sliderValue(mx, 133, 34, 0, 100, ((MachineMenu)this.menu).field(3), 3));
      } else if (this.isHovering(128, 34, 44, 10, mouseX, mouseY)) {
         this.sendAction(2, this.sliderValue(mx, 134, 34, 1, 32, ((MachineMenu)this.menu).field(1), 1));
      } else if (this.isHovering(133, 49, 9, 9, mouseX, mouseY)) {
         this.sendAction(5, 1);
      } else if (this.isHovering(145, 49, 9, 9, mouseX, mouseY)) {
         int mask = 5;
         int mode = ((MachineMenu)this.menu).field(2) & mask;

         int next = switch (mode) {
            case 0 -> 1;
            case 1 -> 4;
            default -> 4;
            case 4 -> 0;
         };
         this.sendAction(3, ((MachineMenu)this.menu).field(2) & ~mask | next);
      } else if (this.isHovering(159, 49, 7, 9, mouseX, mouseY)) {
         this.sendAction(3, ((MachineMenu)this.menu).field(2) ^ 2);
      }

      return true;
   }

   private boolean handleFactoryDropperClick(double mouseX, double mouseY) {
      int mx = (int)(mouseX - this.leftPos + 0.5);
      int my = (int)(mouseY - this.topPos + 0.5);
      if (!this.isHovering(114, 1, 61, 79, mouseX, mouseY)) {
         return false;
      }

      if (this.isHovering(130, 10, 12, 25, mouseX, mouseY)) {
         this.sendAction(10, 100 - Mth.clamp((my - 10) * 100 / 25, 0, 100));
      } else if (this.isHovering(145, 10, 25, 25, mouseX, mouseY)) {
         int xDev = Mth.clamp((int)Math.round((mx - 157) * 100.0 / 12.0), -100, 100);
         int yDev = Mth.clamp(-((int)Math.round((my - 22) * 100.0 / 12.0)), -100, 100);
         if (Math.abs(xDev) < 9) {
            xDev = 0;
         }

         if (Math.abs(yDev) < 9) {
            yDev = 0;
         }

         this.sendAction(11, xDev, yDev);
      } else if (this.isHovering(129, 40, 44, 10, mouseX, mouseY)) {
         this.sendAction(12, this.sliderValue(mx, 135, 34, 1, 32, ((MachineMenu)this.menu).field(4), 1));
      } else if (this.isHovering(129, 50, 44, 10, mouseX, mouseY)) {
         this.sendAction(13, this.sliderValue(mx, 135, 34, 0, 100, ((MachineMenu)this.menu).field(6), 3));
      } else if (this.isHovering(114, 51, 9, 9, mouseX, mouseY)) {
         this.sendAction(15, 1);
      } else if (this.isHovering(162, 66, 7, 9, mouseX, mouseY)) {
         this.sendAction(14, ((MachineMenu)this.menu).field(5) ^ 16);
      } else if (this.isHovering(132, 66, 9, 9, mouseX, mouseY)) {
         this.sendAction(14, ((MachineMenu)this.menu).field(5) ^ 1);
      } else if (this.isHovering(148, 66, 9, 9, mouseX, mouseY)) {
         int mask = 34;

         int next = switch (((MachineMenu)this.menu).field(5) & mask) {
            case 0 -> 32;
            case 2 -> 0;
            case 32 -> 2;
            default -> 2;
         };
         this.sendAction(14, ((MachineMenu)this.menu).field(5) & ~mask | next);
      }

      return true;
   }

   private boolean handleFactoryPlacerClick(double mouseX, double mouseY) {
      MachineScreen.FactoryPlacerButton button = this.factoryPlacerButtonAt(mouseX, mouseY);
      if (button != null) {
         switch (button) {
            case REDSTONE_SIGNAL:
               this.sendAction(21, 1);
               break;
            case INVERSION:
               int mask = 5;
               int mode = ((MachineMenu)this.menu).field(0) & mask;

               int next = switch (mode) {
                  case 0 -> 1;
                  case 1 -> 4;
                  default -> 4;
                  case 4 -> 0;
               };
               this.sendAction(20, ((MachineMenu)this.menu).field(0) & ~mask | next);
               break;
            case TRIGGER_MODE:
               this.sendAction(20, ((MachineMenu)this.menu).field(0) ^ 2);
         }

         return true;
      } else {
         MachineScreen.FactoryPlacerLine line = this.factoryPlacerLineAt(mouseX, mouseY);
         if (line == null) {
            return false;
         }

         this.sendAction(22, line.row());
         return true;
      }
   }

   private MachineScreen.FactoryPlacerButton factoryPlacerButtonAt(double mouseX, double mouseY) {
      if (this.isHovering(133, 49, 9, 9, mouseX, mouseY)) {
         return MachineScreen.FactoryPlacerButton.REDSTONE_SIGNAL;
      } else if (this.isHovering(145, 49, 9, 9, mouseX, mouseY)) {
         return MachineScreen.FactoryPlacerButton.INVERSION;
      } else {
         return this.isHovering(159, 49, 7, 9, mouseX, mouseY) ? MachineScreen.FactoryPlacerButton.TRIGGER_MODE : null;
      }
   }

   private MachineScreen.FactoryPlacerLine factoryPlacerLineAt(double mouseX, double mouseY) {
      if (this.isHovering(126, 6, 49, 13, mouseX, mouseY)) {
         return MachineScreen.FactoryPlacerLine.TOP;
      } else if (this.isHovering(126, 20, 49, 13, mouseX, mouseY)) {
         return MachineScreen.FactoryPlacerLine.MIDDLE;
      } else {
         return this.isHovering(126, 34, 49, 10, mouseX, mouseY) ? MachineScreen.FactoryPlacerLine.BOTTOM : null;
      }
   }

   private void drawFactoryPlacerControlHover(GuiGraphics graphics, double mouseX, double mouseY) {
      MachineScreen.FactoryPlacerButton button = this.factoryPlacerButtonAt(mouseX, mouseY);
      if (button != null) {
         switch (button) {
            case REDSTONE_SIGNAL:
               this.drawHoverOutline(graphics, this.leftPos + 133, this.topPos + 49, 9, 9, -1917873);
               break;
            case INVERSION:
               this.drawHoverOutline(graphics, this.leftPos + 145, this.topPos + 49, 9, 9, -1917873);
               break;
            case TRIGGER_MODE:
               this.drawHoverOutline(graphics, this.leftPos + 159, this.topPos + 49, 9, 9, -1917873);
         }
      } else {
         MachineScreen.FactoryPlacerLine line = this.factoryPlacerLineAt(mouseX, mouseY);
         if (line != null) {
            this.drawHoverOutline(graphics, this.leftPos + 126, this.topPos + line.y(), 49, line.height(), -1917873);
            this.drawHoverOutline(graphics, this.leftPos + 9, this.topPos + 7 + line.row() * 17, 110, 19, -1917873);
         }
      }
   }

   private boolean handleElectricalFurnaceClick(double mouseX, double mouseY) {
      if (!this.isHovering(134, 48, 30, 28, mouseX, mouseY)) {
         return false;
      }

      if (this.isHovering(144, 64, 6, 10, mouseX, mouseY)) {
         this.sendAction(30, 0);
      } else if (this.isHovering(134, 58, 10, 6, mouseX, mouseY)) {
         this.sendAction(30, 1);
      } else if (this.isHovering(144, 48, 6, 10, mouseX, mouseY)) {
         this.sendAction(30, 2);
      } else if (this.isHovering(150, 58, 10, 6, mouseX, mouseY)) {
         this.sendAction(30, 3);
      }

      return true;
   }

   private void renderMachineTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
      Component controlTooltip = this.originalControlTooltip(mouseX, mouseY);
      if (controlTooltip != null) {
         graphics.renderTooltip(this.font, controlTooltip, mouseX, mouseY);
      } else if (this.hoveredSlot == null || !this.hoveredSlot.hasItem()) {
         for (MachineLayout.MachineSlot slot : ((MachineMenu)this.menu).layout().machineSlots()) {
            if (this.isHovering(slot.x(), slot.y(), 16, 16, mouseX, mouseY)) {
               graphics.renderTooltip(this.font, this.slotTooltip(slot), mouseX, mouseY);
               return;
            }
         }
      }
   }

   private Component slotTooltip(MachineLayout.MachineSlot slot) {
      return (Component)(switch (((MachineMenu)this.menu).kind()) {
         case FACTORY_HOPPER -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.slots.storage");
         case FACTORY_DROPPER -> Component.translatable(
            slot.containerIndex() < 12
               ? "block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.slots.input"
               : "block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.slots.filter"
         );
         case FACTORY_PLACER -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_placer.slots.blocks");
         case SMALL_LAB_FURNACE -> this.labFurnaceSlotTooltip(slot.containerIndex());
         case SMALL_ELECTRICAL_FURNACE -> this.electricalFurnaceSlotTooltip(slot.containerIndex());
         case METAL_CRAFTING_TABLE -> this.metalCraftingTableSlotTooltip(slot.containerIndex());
         case SMALL_WASTE_INCINERATOR -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_waste_incinerator.slots.queue");
         default -> Component.literal(slot.role());
      });
   }

   private Component metalCraftingTableSlotTooltip(int index) {
      return switch (index) {
         case 0 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.metal_crafting_table.slots.hammer");
         case 10 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.metal_crafting_table.slots.output");
         default -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.metal_crafting_table.slots.grid");
      };
   }

   private Component labFurnaceSlotTooltip(int index) {
      return switch (index) {
         case 0 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.input");
         case 1 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.fuel");
         case 2 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.output");
         case 3, 4 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.input_fifo");
         case 5, 6 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.fuel_fifo");
         case 7, 8 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.output_fifo");
         case 9, 10 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.aux");
         default -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.machine.slots.generic");
      };
   }

   private Component electricalFurnaceSlotTooltip(int index) {
      return switch (index) {
         case 0 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.slots.input");
         case 1 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.slots.aux");
         case 2 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.slots.output");
         case 3, 4 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.slots.input_fifo");
         case 5, 6 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.slots.output_fifo");
         default -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.machine.slots.generic");
      };
   }

   private Component originalControlTooltip(double mouseX, double mouseY) {
      if (((MachineMenu)this.menu).layout().style() != MachineLayout.LayoutStyle.ORIGINAL_TEXTURE) {
         return null;
      }

      return switch (((MachineMenu)this.menu).kind()) {
         case FACTORY_HOPPER -> this.factoryHopperTooltip(mouseX, mouseY);
         case FACTORY_DROPPER -> this.factoryDropperTooltip(mouseX, mouseY);
         case FACTORY_PLACER -> this.factoryPlacerTooltip(mouseX, mouseY);
         case SMALL_LAB_FURNACE -> this.labFurnaceTooltip(mouseX, mouseY);
         case SMALL_ELECTRICAL_FURNACE -> this.electricalFurnaceTooltip(mouseX, mouseY);
         default -> null;
         case SMALL_WASTE_INCINERATOR -> this.wasteIncineratorTooltip(mouseX, mouseY);
      };
   }

   private Component factoryHopperTooltip(double mouseX, double mouseY) {
      if (this.isHovering(128, 9, 44, 10, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.range");
      } else if (this.isHovering(145, 20, 8, 8, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.delayindicator");
      } else if (this.isHovering(128, 21, 44, 10, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.period");
      } else if (this.isHovering(128, 34, 44, 10, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.count");
      } else if (this.isHovering(133, 49, 9, 9, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.rssignal");
      } else if (this.isHovering(145, 49, 9, 9, mouseX, mouseY)) {
         int logic = ((MachineMenu)this.menu).field(2);
         String state = (logic & 4) != 0 ? "ignored" : (logic & 1) != 0 ? "inverted" : "normal";
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.inversion")
            .append("\n").append(Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.state." + state));
      } else {
         int logic = ((MachineMenu)this.menu).field(2);
         String state = (logic & 4) != 0 ? "ignore_trigger" : (logic & 2) != 0 ? "continuous" : "pulse";
         return this.isHovering(159, 49, 7, 9, mouseX, mouseY)
            ? Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.triggermode")
               .append("\n").append(Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.state." + state))
            : null;
      }
   }

   private Component factoryDropperTooltip(double mouseX, double mouseY) {
      if (this.isHovering(130, 10, 12, 25, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.velocity");
      } else if (this.isHovering(145, 10, 25, 25, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.direction");
      } else if (this.isHovering(129, 40, 44, 10, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.dropcount");
      } else if (this.isHovering(129, 50, 44, 10, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.period");
      } else if (this.isHovering(114, 51, 9, 9, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.rssignal");
      } else if (this.isHovering(132, 66, 9, 9, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.filtergate");
      } else if (this.isHovering(148, 66, 9, 9, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.externgate");
      } else {
         return this.isHovering(162, 66, 7, 9, mouseX, mouseY)
            ? Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.triggermode")
            : null;
      }
   }

   private Component factoryPlacerTooltip(double mouseX, double mouseY) {
      MachineScreen.FactoryPlacerButton button = this.factoryPlacerButtonAt(mouseX, mouseY);
      if (button != null) {
         return switch (button) {
            case REDSTONE_SIGNAL -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_placer.tooltips.rssignal");
            case INVERSION -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_placer.tooltips.inversion");
            case TRIGGER_MODE -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_placer.tooltips.triggermode");
         };
      } else {
         MachineScreen.FactoryPlacerLine line = this.factoryPlacerLineAt(mouseX, mouseY);
         return line != null ? Component.translatable(line.tooltipKey()) : null;
      }
   }

   private Component labFurnaceTooltip(double mouseX, double mouseY) {
      if (this.isHovering(59, 35, 18, 18, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.tooltips.flame");
      } else {
         return this.isHovering(78, 37, 22, 18, mouseX, mouseY)
            ? Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.tooltips.progress")
            : null;
      }
   }

   private Component electricalFurnaceTooltip(double mouseX, double mouseY) {
      if (this.isHovering(62, 54, 14, 15, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.tooltips.heat");
      } else if (this.isHovering(78, 28, 22, 18, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.tooltips.progress");
      } else if (this.isHovering(89, 52, 35, 17, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.tooltips.energy");
      } else {
         return this.isHovering(134, 48, 30, 28, mouseX, mouseY)
            ? Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.tooltips.speed")
            : null;
      }
   }

   private Component wasteIncineratorTooltip(double mouseX, double mouseY) {
      if (this.isHovering(10, 49, 31, 27, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_waste_incinerator.tooltips.firebox");
      } else {
         return this.isHovering(8, 5, 160, 72, mouseX, mouseY)
            ? Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_waste_incinerator.tooltips.queue")
            : null;
      }
   }

   private int sliderValue(int mouseX, int originX, int span, int min, int max, int current, int step) {
      int relative = mouseX - originX;
      if (relative < -1) {
         return Mth.clamp(current - step, min, max);
      } else {
         return relative >= span ? Mth.clamp(current + step, min, max) : Mth.clamp((int)Math.round(min + (max - min) * ((double)relative / span)), min, max);
      }
   }

   private void sendAction(int action, int value) {
      this.sendAction(action, value, 0);
   }

   private void sendAction(int action, int valueA, int valueB) {
      PacketDistributor.sendToServer(new MachineActionPayload(((MachineMenu)this.menu).containerId, action, valueA, valueB), new CustomPacketPayload[0]);
   }

   private void drawMachineSlots(GuiGraphics graphics, MachineLayout layout, int x, int y) {
      for (MachineLayout.MachineSlot slot : layout.machineSlots()) {
         this.drawSlot(graphics, x + slot.x(), y + slot.y());
      }
   }

   private void drawPlayerSlots(GuiGraphics graphics, MachineLayout layout, int x, int y) {
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            this.drawSlot(graphics, x + layout.playerInventoryX() + column * 18, y + layout.playerInventoryY() + row * 18);
         }
      }

      for (int column = 0; column < 9; column++) {
         this.drawSlot(graphics, x + layout.hotbarX() + column * 18, y + layout.hotbarY());
      }
   }

   private void drawSlot(GuiGraphics graphics, int x, int y) {
      graphics.fill(x - 1, y - 1, x + 17, y + 17, -14934755);
      graphics.fill(x, y, x + 16, y + 16, -12236470);
      graphics.fill(x + 1, y + 1, x + 15, y + 15, -14276310);
      graphics.fill(x + 1, y + 1, x + 15, y + 2, -10854818);
      graphics.fill(x + 1, y + 14, x + 15, y + 15, -15263719);
   }

   private void drawArrow(GuiGraphics graphics, int x, int y, int fill) {
      graphics.fill(x, y, x + 24, y + 10, -14934755);
      graphics.fill(x + 1, y + 2, x + 18, y + 8, -12236470);
      graphics.fill(x + 18, y + 1, x + 21, y + 9, -12236470);
      graphics.fill(x + 21, y + 3, x + 23, y + 7, -12236470);
      graphics.fill(x + 1, y + 2, x + 1 + Mth.clamp(fill, 0, 21), y + 8, this.profile.accent());
   }

   private void drawMeter(GuiGraphics graphics, int x, int y, int width, int value, int max, int color) {
      int fill = Mth.clamp(value * width / Math.max(1, max), 0, width);
      graphics.fill(x, y, x + width + 2, y + 5, -14934755);
      graphics.fill(x + 1, y + 1, x + 1 + fill, y + 4, color);
      if (fill > 1) {
         graphics.fill(x + 1, y + 1, x + 1 + fill, y + 2, 1442840575);
      }
   }

   private void drawTank(GuiGraphics graphics, int x, int y, int width, int height, int value, int max) {
      int fill = Mth.clamp(value * (height - 2) / Math.max(1, max), 0, height - 2);
      graphics.fill(x, y, x + width, y + height, -14934755);
      graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, -14276310);
      graphics.fill(x + 2, y + height - 1 - fill, x + width - 2, y + height - 2, -11753768);
      graphics.fill(x + 2, y + 2, x + width - 2, y + 6, 1442840575);
   }

   private void drawSolarCells(GuiGraphics graphics, int x, int y) {
      for (int row = 0; row < 2; row++) {
         for (int column = 0; column < 5; column++) {
            int cellX = x + column * 18;
            int cellY = y + row * 9;
            graphics.fill(cellX, cellY, cellX + 14, cellY + 6, -15656928);
            graphics.fill(cellX + 1, cellY + 1, cellX + 13, cellY + 5, -12755848);
         }
      }
   }

   private void drawSignal(GuiGraphics graphics, int x, int y) {
      int signal = ((MachineMenu)this.menu).comparatorOutput();

      for (int i = 0; i < 4; i++) {
         int color = signal > i * 4 ? -1941681 : -13353916;
         graphics.fill(x + i * 5, y + 15 - i * 4, x + i * 5 + 3, y + 18, color);
      }
   }

   private int workWidth(int max) {
      return Mth.clamp(((MachineMenu)this.menu).progress() * max / 100, 0, max);
   }

   public static final class FactoryDropperScreen extends MachineScreen {
      public FactoryDropperScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Factory Dropper", "Pulse Drop", -7432541));
      }
   }

   public static final class FactoryHopperScreen extends MachineScreen {
      public FactoryHopperScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Factory Hopper", "Collect", -8477094));
      }
   }

   private enum FactoryPlacerButton {
      REDSTONE_SIGNAL,
      INVERSION,
      TRIGGER_MODE;
   }

   private enum FactoryPlacerLine {
      TOP(0, 6, 13, "block.immersive_engineer_decor_controls_tool_reforged.factory_placer.tooltips.stock_row_top"),
      MIDDLE(1, 20, 13, "block.immersive_engineer_decor_controls_tool_reforged.factory_placer.tooltips.stock_row_middle"),
      BOTTOM(2, 34, 10, "block.immersive_engineer_decor_controls_tool_reforged.factory_placer.tooltips.stock_row_bottom");

      private final int row;
      private final int y;
      private final int height;
      private final String tooltipKey;

      FactoryPlacerLine(int row, int y, int height, String tooltipKey) {
         this.row = row;
         this.y = y;
         this.height = height;
         this.tooltipKey = tooltipKey;
      }

      private int row() {
         return this.row;
      }

      private int y() {
         return this.y;
      }

      private int height() {
         return this.height;
      }

      private String tooltipKey() {
         return this.tooltipKey;
      }
   }

   public static final class FactoryPlacerScreen extends MachineScreen {
      public FactoryPlacerScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Factory Block Placer", "Pulse Place", -7178323));
      }
   }

   public static final class FluidBarrelScreen extends MachineScreen {
      public FluidBarrelScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Fluid Barrel", "Stored Fluid", -11753768));
      }
   }

   public static final class LabeledCrateScreen extends MachineScreen {
      public LabeledCrateScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Labeled Crate", "Storage", -6457774));
      }
   }

   public static final class MetalCraftingTableScreen extends MachineScreen {
      public MetalCraftingTableScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Metal Crafting Table", "Plate Work", -7432541));
      }
   }

   public static final class PassiveFluidAccumulatorScreen extends MachineScreen {
      public PassiveFluidAccumulatorScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Passive Fluid Accumulator", "Accumulate", -11753768));
      }
   }

   protected record Profile(String title, String process, int accent) {
   }

   public static final class SmallBlockBreakerScreen extends MachineScreen {
      public SmallBlockBreakerScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Block Breaker", "Break Face", -5149104));
      }
   }

   public static final class SmallElectricalFurnaceScreen extends MachineScreen {
      public SmallElectricalFurnaceScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Electrical Furnace", "Electric", -1917873));
      }
   }

   public static final class SmallFluidFunnelScreen extends MachineScreen {
      public SmallFluidFunnelScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Fluid Funnel", "Collect Fluid", -11753768));
      }
   }

   public static final class SmallFreezerScreen extends MachineScreen {
      public SmallFreezerScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Water Freezer", "Freezing", -9652519));
      }
   }

   public static final class SmallLabFurnaceScreen extends MachineScreen {
      public SmallLabFurnaceScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Laboratory Furnace", "Smelting", -2066107));
      }
   }

   public static final class SmallMilkingMachineScreen extends MachineScreen {
      public SmallMilkingMachineScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Milking Machine", "Milk Cow", -1054776));
      }
   }

   public static final class SmallMineralSmelterScreen extends MachineScreen {
      public SmallMineralSmelterScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Mineral Smelter", "Melting", -2790851));
      }
   }

   public static final class SmallSolarPanelScreen extends MachineScreen {
      public SmallSolarPanelScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Solar Panel", "Sun Charge", -1917873));
      }
   }

   public static final class SmallTreeCutterScreen extends MachineScreen {
      public SmallTreeCutterScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Tree Cutter", "Cut Logs", -8477094));
      }
   }

   public static final class SmallWasteIncineratorScreen extends MachineScreen {
      public SmallWasteIncineratorScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Waste Incinerator", "Burn Waste", -3839166));
      }
   }
}
