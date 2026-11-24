package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MusliBarPressMenu;
import com.oblixorprime.engineersdecorreforged.network.MusliActionPayload;
import com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeCommandProtocol;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * PROVISIONAL runtime screen. The owner-approved 1536x1024 concept is preserved
 * externally until its original PNG can be imported and stripped of painted
 * static gauge values. Do not describe this screen as approved final artwork.
 */
public final class MusliBarPressScreen extends AbstractContainerScreen<MusliBarPressMenu> {
   private static final int PANEL = 0xFF29343D;
   private static final int FRAME = 0xFF68737A;
   private static final int ACCENT = 0xFFCDA257;
   private static final String[] GAUGE_NAMES = {"Cereal", "Fruit", "Seeds"};

   public MusliBarPressScreen(MusliBarPressMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = 252;
      this.imageHeight = 220;
      this.inventoryLabelY = 119;
      this.titleLabelY = 9;
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      int x = leftPos, y = topPos;
      graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF15191E);
      graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, FRAME);
      graphics.fill(x + 5, y + 5, x + imageWidth - 5, y + imageHeight - 5, PANEL);
      graphics.fill(x + 12, y + 24, x + 238, y + 112, 0xFF1B242C);
      graphics.fill(x + 20, y + 30, x + 128, y + 103, 0xFF343D43);
      // Entire funnel plus a slightly higher opening is the invisible input region.
      graphics.fill(x + 44, y + 32, x + 104, y + 48, 0xFF7B8589);
      graphics.fill(x + 56, y + 48, x + 92, y + 75, 0xFF4B565B);
      graphics.fill(x + 68, y + 75, x + 80, y + 96, 0xFF9A7741);
      // The drawn bar occupies the (invisible) PRESS target.
      graphics.fill(x + 160, y + 45, x + 219, y + 77, 0xFF8A5839);
      graphics.fill(x + 166, y + 51, x + 213, y + 71, ACCENT);
      graphics.drawString(font, "x4", x + 184, y + 56, 0xFF2E241B, false);
      // Passive gauges: each rectangle is filled solely by server-synchronized counts.
      for (int index = 0; index < 3; index++) {
         int gx = x + 143 + index * 30;
         int count = Math.max(0, Math.min(8, menu.reserve(index)));
         graphics.fill(gx, y + 87, gx + 22, y + 104, 0xFF171C20);
         graphics.fill(gx + 2, y + 102 - count * 13 / 8, gx + 20, y + 102, ACCENT);
         graphics.drawString(font, Integer.toString(count), gx + 8, y + 107, 0xFFE5E7E9, false);
      }
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) drawSlotFrame(graphics, x + 44 + col * 18, y + 132 + row * 18);
      }
      for (int col = 0; col < 9; col++) drawSlotFrame(graphics, x + 44 + col * 18, y + 190);
   }

   private void drawSlotFrame(GuiGraphics graphics, int x, int y) {
      graphics.fill(x, y, x + 18, y + 18, 0xFF1B252C);
      graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF465159);
      graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF252F37);
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(font, "MUSLI PRESS  / PROVISIONAL", titleLabelX, titleLabelY, 0xFFFFFFFF, false);
      graphics.drawString(font, "PROVISIONAL: ACTIONS LOCKED", 17, 114, 0xFFFFCC7A, false);
      for (int i = 0; i < 3; i++) {
         // No ingredient slot. These are passive server-fed gauges.
         graphics.drawString(font, GAUGE_NAMES[i].substring(0, 1), 150 + i * 30, 78, 0xFFDDDDDD, false);
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         MusleeCommandProtocol.Type type = null;
         if (isHovering(20, 25, 110, 80, mouseX, mouseY)) {
            type = MusleeCommandProtocol.Type.ABSORB;
         } else if (isHovering(155, 40, 72, 41, mouseX, mouseY)) {
            type = MusleeCommandProtocol.Type.PRESS;
         }
         if (type != null) {
            byte[] intent = MusleeCommandProtocol.encode(menu.sessionId(),
               new MusleeCommandProtocol.Command(UUID.randomUUID(), type));
            PacketDistributor.sendToServer(new MusliActionPayload(menu.containerId, intent));
            // Recognized intent must not mutate until qualified server admission.
            return true;
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }
}
