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
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/block_breaker_v08/background.png");
   private static final int WHITE = 0xFFE8E1CE, MUTED = 0xFFB1AFA8, AMBER = 0xFFFFB348,
      STEEL_DARK = 0xFF24272A, INSET = 0xFF151718, EDGE = 0xFF656462, RED = 0xFFFF6343;
   private int lastSentRevision = -1;

   public SmallMineralSmelterHybridScreen(MachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      imageWidth = menu.layout().imageWidth();
      imageHeight = menu.layout().imageHeight();
      inventoryLabelX = 26;
      inventoryLabelY = 132;
      titleLabelX = 40;
      titleLabelY = 7;
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
      // Existing genuine IEDCTR background: no external texture is required to open the menu.
      graphics.blit(STEEL, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
      panel(graphics, x + 11, y + 20, x + 104, y + 133);
      panel(graphics, x + 106, y + 20, x + 200, y + 75);
      panel(graphics, x + 106, y + 78, x + 200, y + 132);
      // Smelting chamber, showing the heat *state* rather than invented Celsius.
      int thermal = Mth.clamp(menu.field(10), 0, 100);
      int fire = thermal < 5 ? 0xFF3A4A54 : thermal < 90 ? 0xFFF58A2D : thermal < 100 ? 0xFFE65B1D : 0xFFFF460B;
      graphics.fill(x + 21, y + 38, x + 76, y + 96, 0xFF0A1013);
      graphics.fill(x + 24, y + 40, x + 73, y + 93, 0xFF25221F);
      graphics.fill(x + 26, y + 40, x + 70, y + 44, EDGE);
      graphics.fill(x + 25, y + 88, x + 72, y + 92, 0xFF8F7C6C);
      graphics.fill(x + 31, y + 78, x + 66, y + 87, fire);
      if (thermal >= 5) {
         graphics.fill(x + 33, y + 74, x + 39, y + 82, AMBER);
         graphics.fill(x + 48, y + 70, x + 52, y + 79, fire);
         graphics.fill(x + 58, y + 73, x + 62, y + 83, 0xFFFFD26E);
      }
      for (int side = 0; side < 2; side++) {
         int xx = x + (side == 0 ? 20 : 74);
         graphics.fill(xx, y + 36, xx + 3, y + 99, 0xFF4F4D47);
         graphics.fill(xx, y + 42, xx + 2, y + 94, AMBER);
      }
      gauge(graphics, x + 23, y + 105, 73, 5, menu.progress(), 100, fire);
      // Read-only thermal phase lamps (only one active).
      int phase = thermal >= 100 ? 3 : thermal >= 90 ? 2 : thermal >= 5 ? 1 : 0;
      for (int i = 0; i < 4; i++) {
         int xx = x + 19 + i * 19;
         graphics.fill(xx, y + 113, xx + 14, y + 120, i == phase ? (i == 0 ? 0xFF64B7C4 : AMBER) : 0xFF34383C);
      }
      // True tank, one fluid type only. Fill is proportional to actual server amount/capacity.
      int amount = Mth.clamp(menu.fluidAmount(), 0, 4000);
      graphics.fill(x + 112, y + 85, x + 126, y + 119, 0xFF080C0E);
      graphics.fill(x + 113, y + 86, x + 125, y + 118, 0xFF374044);
      int level = Mth.clamp(amount * 30 / 4000, 0, 30);
      if (level > 0) {
         graphics.fill(x + 114, y + 117 - level, x + 124, y + 117, 0xFFED591E);
         graphics.fill(x + 114, y + 117 - level, x + 124, y + 118 - level, AMBER);
      }
      graphics.fill(x + 112, y + 85, x + 126, y + 87, EDGE);
      graphics.fill(x + 112, y + 118, x + 126, y + 120, EDGE);
      // Source/target flow is per mode: Lava goes to tank; solids to item output.
      int mode = Mth.clamp(menu.field(7), 0, 2);
      if (mode == 0) {
         graphics.fill(x + 128, y + 58, x + 133, y + 60, AMBER);
         graphics.fill(x + 128, y + 60, x + 130, y + 86, AMBER);
      } else {
         graphics.fill(x + 125, y + 51, x + 132, y + 53, AMBER);
      }
      // Three real selectable commands; a mode icon is not a fake storage slot.
      for (int i = 0; i < 3; i++) {
         int top = y + 25 + i * 16;
         graphics.fill(x + 151, top, x + 196, top + 14, i == mode ? 0xFF8A511E : INSET);
         graphics.fill(x + 151, top, x + 196, top + 1, i == mode ? AMBER : EDGE);
      }
      gauge(graphics, x + 153, y + 108, 39, 4, menu.energyStored(), menu.energyCapacity(), AMBER);
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

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      int thermal = Mth.clamp(menu.field(10), 0, 100);
      int phase = thermal >= 100 ? 3 : thermal >= 90 ? 2 : thermal >= 5 ? 1 : 0;
      String[] phases = {"COLD", "HOT", "MAGMA", "LAVA"};
      graphics.drawString(font, "MINERAL SMELTER", 43, 8, WHITE, false);
      graphics.drawString(font, "THERMAL CORE", 21, 25, WHITE, false);
      graphics.drawString(font, "INPUT", 108, 26, MUTED, false);
      graphics.drawString(font, "OUTPUT", 130, 26, MUTED, false);
      graphics.drawString(font, "PRODUCT", 154, 17, WHITE, false);
      graphics.drawString(font, "LAVA", 163, 28, menu.field(7) == 0 ? WHITE : MUTED, false);
      graphics.drawString(font, "MAGMA", 160, 44, menu.field(7) == 1 ? WHITE : MUTED, false);
      graphics.drawString(font, "OBS.", 163, 60, menu.field(7) == 2 ? WHITE : MUTED, false);
      graphics.drawString(font, "HOT: " + thermal + "%", 20, 99, thermal >= 5 ? AMBER : MUTED, false);
      graphics.drawString(font, phases[phase], 74, 99, WHITE, false);
      graphics.drawString(font, "LAVA TANK", 112, 78, AMBER, false);
      graphics.drawString(font, menu.fluidAmount() + "/4000mB", 129, 90, WHITE, false);
      graphics.drawString(font, "FE " + menu.energyStored(), 153, 96, WHITE, false);
      graphics.drawString(font, menu.field(11) != 0 ? "RS HIGH" : "RS LOW", 153, 119,
         menu.field(11) != 0 ? RED : 0xFF80D986, false);
      graphics.drawString(font, "FE/t " + menu.field(13), 129, 123, MUTED, false);
      graphics.drawString(font, "INV", 24, 134, WHITE, false);
      if (menu.field(9) >= 0) graphics.drawString(font, "NEXT: " + new String[]{"LAVA","MAGMA","OBS."}[Mth.clamp(menu.field(9), 0, 2)], 20, 126, AMBER, false);
      else if (thermal >= 5) graphics.drawString(font, "HOT SURFACE", 20, 126, RED, false);
      else graphics.drawString(font, "READY", 20, 126, MUTED, false);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         for (int mode = 0; mode < 3; mode++) {
            if (isHovering(151, 25 + mode * 16, 45, 14, mouseX, mouseY)) {
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
      return super.mouseClicked(mouseX, mouseY, button);
   }
}
