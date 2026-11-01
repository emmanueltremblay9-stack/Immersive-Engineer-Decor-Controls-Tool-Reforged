package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.network.MachineActionPayload;
import com.oblixorprime.engineersdecorreforged.network.ModNetworking;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
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
   private static final String[] MODE_KEYS = {
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.mode.lava",
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.mode.magma",
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.mode.obsidian"
   };
   private static final String[] STATUS_KEYS = {
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.status.ready",
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.status.heating",
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.status.cooling",
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.status.no_input",
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.status.invalid_input",
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.status.tank_full",
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.status.output_full",
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.status.wait_lava",
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.status.redstone_inhibited",
      "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.status.wait_fe"
   };
   private int lastSentRevision = -1;
   private long lastModeClickMillis;
   private boolean outputDrawerOpen;
   private Button outputDrawerButton;
   private final Button[] modeButtons = new Button[3];
   private Button statusButton;

   public SmallMineralSmelterHybridScreen(MachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      imageWidth = menu.layout().imageWidth();
      imageHeight = menu.layout().imageHeight();
      inventoryLabelX = -1000;
      titleLabelX = -1000;
      menu.setSmelterOutputDrawerOpen(false);
   }

   @Override
   protected void init() {
      super.init();
      outputDrawerButton = addRenderableWidget(Button.builder(
         Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.drawer.accessible"),
         button -> toggleOutputDrawer())
         .bounds(leftPos + 162, topPos + 35, 54, 8)
         .tooltip(Tooltip.create(Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.drawer.tooltip")))
         .build());
      outputDrawerButton.setAlpha(0.0F);
      for (int mode = 0; mode < modeButtons.length; mode++) {
         final int selectedMode = mode;
         modeButtons[mode] = addRenderableWidget(Button.builder(
            Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.mode.accessible",
               Component.translatable(MODE_KEYS[mode]), Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.mode.unselected")),
            button -> sendModeSelection(selectedMode))
            .bounds(leftPos + 176, topPos + 42 + mode * 11, 39, 9)
            .tooltip(Tooltip.create(Component.translatable(MODE_KEYS[mode])))
            .build());
         modeButtons[mode].setAlpha(0.0F);
      }
      statusButton = addRenderableWidget(Button.builder(
         statusAccessibleComponent(), button -> { })
         .bounds(leftPos + 18, topPos + 157, 116, 12)
         .tooltip(Tooltip.create(statusComponent()))
         .build());
      statusButton.setAlpha(0.0F);
      updateModeButtonAvailability();
   }

   private void toggleOutputDrawer() {
      outputDrawerOpen = !outputDrawerOpen;
      menu.setSmelterOutputDrawerOpen(outputDrawerOpen);
      updateModeButtonAvailability();
   }

   private void updateModeButtonAvailability() {
      for (Button button : modeButtons) {
         if (button != null) button.active = !outputDrawerOpen;
      }
   }

   private void sendModeSelection(int mode) {
      long now = Util.getMillis();
      int revision = menu.field(14);
      if ((lastSentRevision == revision && now - lastModeClickMillis < 1000L) || now - lastModeClickMillis < 250L) return;
      PacketDistributor.sendToServer(new MachineActionPayload(
         menu.containerId, ModNetworking.MINERAL_SMELTER_SELECT_MODE, mode, revision));
      lastSentRevision = revision;
      lastModeClickMillis = now;
   }

   private Component statusComponent() {
      int status = Mth.clamp(menu.field(12), 0, STATUS_KEYS.length - 1);
      return Component.translatable(STATUS_KEYS[status]);
   }

   private Component statusAccessibleComponent() {
      return Component.translatable(
         "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.status.accessible", statusComponent());
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
            if (modeButtons[i] != null && (modeButtons[i].isFocused() || modeButtons[i].isHovered())) {
               outline(graphics, x + 174, top - 1, 43, 11, WHITE);
            }
         }
      }
      if (outputDrawerButton != null && (outputDrawerButton.isFocused() || outputDrawerButton.isHovered())) {
         outline(graphics, x + 160, y + 33, 58, 11, WHITE);
      }
      if (statusButton != null && (statusButton.isFocused() || statusButton.isHovered())) {
         outline(graphics, x + 17, y + 156, 118, 14, AMBER);
      }
   }

   private static void panel(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
      graphics.fill(x1, y1, x2, y2, 0xFF75706A);
      graphics.fill(x1 + 2, y1 + 2, x2 - 2, y2 - 2, INSET);
      graphics.fill(x1 + 2, y1 + 2, x2 - 2, y1 + 4, STEEL_DARK);
      graphics.fill(x1 + 3, y1 + 3, x1 + 5, y1 + 5, 0xFFB7AEA2);
      graphics.fill(x2 - 5, y2 - 5, x2 - 3, y2 - 3, 0xFFB7AEA2);
   }

   private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
      graphics.fill(x, y, x + width, y + 1, color);
      graphics.fill(x, y + height - 1, x + width, y + height, color);
      graphics.fill(x, y, x + 1, y + height, color);
      graphics.fill(x + width - 1, y, x + width, y + height, color);
   }

   private static void gauge(GuiGraphics graphics, int x, int y, int w, int h, int value, int max, int color) {
      graphics.fill(x, y, x + w, y + h, 0xFF080A0C);
      int filled = max <= 0 ? 0 : Mth.clamp(value * w / max, 0, w);
      if (filled > 0) graphics.fill(x, y, x + filled, y + h, color);
   }

   private void drawSmall(GuiGraphics graphics, Component value, int x, int y, int color, float scale) {
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
      String[] phases = {
         "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.phase.cold",
         "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.phase.hot",
         "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.phase.magma",
         "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.phase.lava"
      };
      drawSmall(graphics, Component.translatable(
         "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.thermal", thermal,
         Component.translatable(phases[phase])), 20, 107, AMBER, 0.65F);
      drawSmall(graphics, Component.translatable(
         "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.comparator", menu.comparatorOutput()),
         36, 127, WHITE, 0.58F);
      drawSmall(graphics, Component.translatable(thermal >= 5
         ? "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.surface.hot"
         : "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.surface.cool"),
         20, 140, thermal >= 5 ? RED : MUTED, 0.68F);
      int queued = menu.field(9);
      if (queued >= 0 && queued <= 2) {
         drawSmall(graphics, Component.translatable(
            "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.next",
            Component.translatable(MODE_KEYS[queued])), 20, 152, AMBER, 0.57F);
      }
      Component status = statusComponent();
      drawSmall(graphics, Component.translatable(
         "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.status.label", status),
         20, 163, statusColor(menu.field(12)), 0.57F);
      if (statusButton != null) {
         statusButton.setMessage(statusAccessibleComponent());
         statusButton.setTooltip(Tooltip.create(status));
      }
      drawSmall(graphics, Component.translatable(
         "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.fluid", menu.fluidAmount()),
         148, 102, WHITE, 0.63F);
      drawSmall(graphics, Component.translatable(
         "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.capacity", menu.fluidCapacity()),
         148, 110, MUTED, 0.52F);
      drawSmall(graphics, Component.translatable(
         "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.energy", menu.energyStored()),
         185, 102, WHITE, 0.53F);
      drawSmall(graphics, Component.translatable(
         "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.fe_rate", menu.field(13)),
         191, 120, WHITE, 0.58F);
      drawSmall(graphics, Component.translatable(menu.field(11) != 0
         ? "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.redstone.high"
         : "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.redstone.low"),
         190, 141, menu.field(11) != 0 ? RED : 0xFF81D77B, 0.65F);
      if (outputDrawerOpen) {
         drawSmall(graphics, Component.translatable(
            "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.drawer.title"),
            191, 49, AMBER, 0.54F);
         drawSmall(graphics, Component.translatable(
            "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.drawer.close"),
            158, 71, MUTED, 0.47F);
      } else {
         int selected = Mth.clamp(menu.field(7), 0, 2);
         for (int i = 0; i < MODE_KEYS.length; i++) {
            Component mode = Component.translatable(MODE_KEYS[i]);
            drawSmall(graphics, mode, 183, 44 + i * 11, selected == i ? AMBER : WHITE, 0.60F);
            if (modeButtons[i] != null) {
               modeButtons[i].setMessage(Component.translatable(
                  "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.mode.accessible",
                  mode, Component.translatable(selected == i
                     ? "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.mode.selected"
                     : "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.mode.unselected")));
            }
         }
      }
   }

   private static int statusColor(int status) {
      return switch (status) {
         case 0 -> 0xFF81D77B;
         case 1, 2 -> AMBER;
         default -> RED;
      };
   }

   @Override
   public void removed() {
      menu.setSmelterOutputDrawerOpen(false);
      super.removed();
   }
}