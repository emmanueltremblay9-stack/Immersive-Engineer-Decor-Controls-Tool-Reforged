package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.network.SmelterModePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.network.PacketDistributor;

/** v0.5 exact-art faceplate with live server projection and accessible controls. */
public final class SmallMineralSmelterHybridScreen extends AbstractContainerScreen<MachineMenu> {
   private static final String PREFIX = "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.";
   private static final ResourceLocation FACEPLATE = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/small_mineral_smelter_hybrid_v05/background.png");
   private static final int WHITE = 0xFFE8E1CE, MUTED = 0xFFB1AFA8, AMBER = 0xFFFFB348,
      EDGE = 0xFF656462, RED = 0xFFFF6343;
   private static final String[] MODE_KEYS = {
      PREFIX + "mode.lava",
      PREFIX + "mode.magma",
      PREFIX + "mode.obsidian"
   };
   private static final String[] PHASE_KEYS = {
      PREFIX + "phase.cold",
      PREFIX + "phase.hot",
      PREFIX + "phase.magma",
      PREFIX + "phase.lava"
   };
   // Stable v0.5 enum, followed by the v0.4 mechanical WAIT_LAVA extension.
   private static final String[] STATUS_KEYS = {
      PREFIX + "status.syncing",
      PREFIX + "status.heating",
      PREFIX + "status.cooling",
      PREFIX + "status.wait_fe",
      PREFIX + "status.redstone_inhibited",
      PREFIX + "status.tank_full",
      PREFIX + "status.output_full",
      PREFIX + "status.no_input",
      PREFIX + "status.invalid_input",
      PREFIX + "status.ready",
      PREFIX + "status.mode_queued",
      PREFIX + "status.wait_lava"
   };

   private boolean outputDrawerOpen;
   private float viewScale = 1.0F;
   private float originX;
   private float originY;
   private int pendingRevision = -1;
   private int pendingMode = -1;
   private int pendingTicks;
   private int requestFeedback;
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

   // Public screen-space bounds for overlays; internal slots retain logical coordinates.
   @Override
   public int getGuiLeft() { return (int)Math.floor(originX); }

   @Override
   public int getGuiTop() { return (int)Math.floor(originY); }

   @Override
   public int getXSize() { return (int)Math.ceil(originX + imageWidth * viewScale) - getGuiLeft(); }

   @Override
   public int getYSize() { return (int)Math.ceil(originY + imageHeight * viewScale) - getGuiTop(); }

   @Override
   protected void init() {
      super.init();
      // Keep the source artwork's logical canvas, scaling the panel and its hit targets together.
      leftPos = 0;
      topPos = 0;
      viewScale = Math.min(1.0F, Math.min((width - 12.0F) / imageWidth, (height - 12.0F) / imageHeight));
      viewScale = Math.max(0.1F, viewScale);
      originX = (width - imageWidth * viewScale) / 2.0F;
      originY = (height - imageHeight * viewScale) / 2.0F;

      outputDrawerButton = addRenderableWidget(Button.builder(
         drawerAccessibleComponent(),
         button -> toggleOutputDrawer())
         .bounds(162, 35, 54, 8)
         .tooltip(Tooltip.create(Component.translatable(PREFIX + "drawer.tooltip")))
         .build());
      outputDrawerButton.setAlpha(0.0F);

      for (int mode = 0; mode < modeButtons.length; mode++) {
         final int selectedMode = mode;
         modeButtons[mode] = addRenderableWidget(Button.builder(
            modeAccessibleComponent(mode),
            button -> sendModeSelection(selectedMode))
            .bounds(176, 42 + mode * 11, 39, 9)
            .tooltip(Tooltip.create(Component.translatable(MODE_KEYS[mode])))
            .build());
         modeButtons[mode].setAlpha(0.0F);
      }

      statusButton = addRenderableWidget(Button.builder(
         statusAccessibleComponent(), button -> { })
         .bounds(18, 157, 116, 12)
         .tooltip(Tooltip.create(statusLabelComponent()))
         .build());
      statusButton.setAlpha(0.0F);
      updateModeButtonAvailability();
   }

   @Override
   protected void containerTick() {
      super.containerTick();
      if (pendingRevision >= 0) {
         if (isUiSynchronized() && menu.field(MachineMenu.SMS_UI_REVISION) != pendingRevision) {
            int queued = projectedField(MachineMenu.SMS_UI_QUEUED_MODE);
            requestFeedback = selectedMode() == pendingMode || queued == pendingMode ? 0 : 2;
            pendingRevision = -1;
            pendingMode = -1;
            pendingTicks = 0;
         } else if (++pendingTicks >= 100) {
            requestFeedback = 1;
            pendingRevision = -1;
            pendingMode = -1;
            pendingTicks = 0;
         }
         updateModeButtonAvailability();
      }
   }

   private boolean isUiSynchronized() {
      return (menu.field(MachineMenu.SMS_UI_FLAGS) & 1) != 0;
   }

   private int projectedField(int index) {
      return isUiSynchronized() ? menu.field(index) : -1;
   }

   private int selectedMode() {
      int selected = projectedField(MachineMenu.SMS_UI_SELECTED_MODE);
      return selected >= 0 && selected < MODE_KEYS.length ? selected : -1;
   }

   private Component modeComponent(int mode) {
      return mode >= 0 && mode < MODE_KEYS.length
         ? Component.translatable(MODE_KEYS[mode])
         : Component.translatable(PREFIX + "unknown");
   }

   private Component modeAccessibleComponent(int mode) {
      int selected = selectedMode();
      String stateKey = pendingMode == mode ? "mode.pending"
         : selected == mode ? "mode.selected" : "mode.unselected";
      return Component.translatable(PREFIX + "mode.accessible",
         Component.translatable(MODE_KEYS[mode]), Component.translatable(PREFIX + stateKey));
   }

   private void toggleOutputDrawer() {
      outputDrawerOpen = !outputDrawerOpen;
      menu.setSmelterOutputDrawerOpen(outputDrawerOpen);
      outputDrawerButton.setMessage(drawerAccessibleComponent());
      updateModeButtonAvailability();
   }

   private Component drawerAccessibleComponent() {
      return Component.translatable(PREFIX + (outputDrawerOpen
         ? "drawer.close_accessible" : "drawer.open_accessible"));
   }

   private void updateModeButtonAvailability() {
      boolean available = isUiSynchronized() && (menu.field(MachineMenu.SMS_UI_FLAGS) & 2) != 0
         && pendingRevision < 0 && !outputDrawerOpen;
      for (Button button : modeButtons) {
         if (button != null) button.active = available;
      }
   }

   private void sendModeSelection(int mode) {
      if (mode < 0 || mode >= MODE_KEYS.length || !isUiSynchronized()
         || (menu.field(MachineMenu.SMS_UI_FLAGS) & 2) == 0 || pendingRevision >= 0) {
         return;
      }
      int revision = menu.field(MachineMenu.SMS_UI_REVISION);
      if (revision < 0 || revision > 0x1FFFFFFF) return;
      PacketDistributor.sendToServer(new SmelterModePayload(menu.containerId, revision, mode));
      pendingRevision = revision;
      pendingMode = mode;
      requestFeedback = 0;
      pendingTicks = 0;
      updateModeButtonAvailability();
   }

   private Component statusComponent() {
      if (!isUiSynchronized()) return Component.translatable(STATUS_KEYS[0]);
      int status = menu.field(MachineMenu.SMS_UI_STATUS);
      if (status < 0 || status >= STATUS_KEYS.length) status = 0;
      return Component.translatable(STATUS_KEYS[status]);
   }

   private Component statusLabelComponent() {
      if (requestFeedback == 1) return Component.translatable(PREFIX + "status.timeout", statusComponent());
      if (requestFeedback == 2) return Component.translatable(PREFIX + "status.rejected", statusComponent());
      return Component.translatable(PREFIX + "status.label", statusComponent());
   }

   private Component statusAccessibleComponent() {
      String key = requestFeedback == 1 ? "status.accessible_timeout"
         : requestFeedback == 2 ? "status.accessible_rejected" : "status.accessible";
      return Component.translatable(PREFIX + key, statusComponent());
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBg(graphics, partialTick, mouseX, mouseY);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, width, height, 0xB0000000);
      updateModeButtonAvailability();
      graphics.pose().pushPose();
      graphics.pose().translate(originX, originY, 0);
      graphics.pose().scale(viewScale, viewScale, 1.0F);
      super.render(graphics, (int)localX(mouseX), (int)localY(mouseY), partialTick);
      graphics.pose().popPose();
      // Keep item tooltips legible outside the panel transform.
      if (hoveredSlot != null && hoveredSlot.hasItem()) {
         graphics.renderTooltip(font, hoveredSlot.getItem(), mouseX, mouseY);
      }
   }

   private double localX(double x) {
      return (x - originX) / viewScale;
   }

   private double localY(double y) {
      return (y - originY) / viewScale;
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      int x = leftPos, y = topPos;
      graphics.pose().pushPose();
      graphics.pose().translate(x, y, 0);
      graphics.pose().scale(imageWidth / 1168.0F, imageHeight / 1346.0F, 1.0F);
      graphics.blit(FACEPLATE, 0, 0, 0, 0, 1168, 1346, 1168, 1346);
      graphics.pose().popPose();
      drawTitleArtworkPatch(graphics, x, y);

      int thermal = projectedField(MachineMenu.SMS_UI_THERMAL);
      int phase = projectedField(MachineMenu.SMS_UI_PHASE);
      int fire = phase == 0 ? 0xFF69AFC4 : phase == 1 ? AMBER : phase == 2 ? 0xFFE46A25 : RED;
      for (int i = 0; i < 4; i++) {
         int yy = y + 45 + i * 10;
         graphics.fill(x + 90, yy, x + 95, yy + 6, phase == i ? fire : 0xFF18211E);
      }
      gauge(graphics, x + 19, y + 114, 87, 5, thermal, 100, fire);

      int amount = projectedField(MachineMenu.SMS_UI_TANK_AMOUNT);
      int capacity = projectedField(MachineMenu.SMS_UI_TANK_CAPACITY);
      int fill = capacity > 0 && amount >= 0 ? Mth.clamp((int)((long)Math.min(amount, capacity) * 40 / capacity), 0, 40) : 0;
      if (fill > 0) {
         graphics.fill(x + 124, y + 145 - fill, x + 138, y + 145, 0xFFE7611D);
         graphics.fill(x + 124, y + 145 - fill, x + 138, y + 146 - fill, AMBER);
      }

      gauge(graphics, x + 174, y + 111, 43, 5,
         projectedField(MachineMenu.SMS_UI_ENERGY), projectedField(MachineMenu.SMS_UI_ENERGY_CAPACITY), AMBER);

      if (outputDrawerOpen) {
         graphics.fill(x + 155, y + 42, x + 216, y + 77, 0xFF797467);
         graphics.fill(x + 156, y + 43, x + 215, y + 76, 0xFF141A19);
         // The real output slot renders above this backing while the drawer is open.
         graphics.fill(x + 168, y + 47, x + 190, y + 69, 0xFF343B36);
         graphics.fill(x + 169, y + 48, x + 189, y + 68, 0xFF0B1010);
      } else {
         int selected = selectedMode();
         for (int i = 0; i < 3; i++) {
            int top = y + 42 + i * 11;
            graphics.fill(x + 176, top, x + 215, top + 1, selected == i ? AMBER : EDGE);
            graphics.fill(x + 176, top + 8, x + 215, top + 9, selected == i ? AMBER : EDGE);
            if (modeButtons[i] != null && (modeButtons[i].isFocused() || modeButtons[i].isHovered())) {
               outline(graphics, x + 174, top - 1, 43, 11, WHITE);
            }
            if (pendingMode == i) outline(graphics, x + 175, top, 41, 9, 0xFF92D9F3);
         }
      }
      if (outputDrawerButton != null && (outputDrawerButton.isFocused() || outputDrawerButton.isHovered())) {
         outline(graphics, x + 160, y + 33, 58, 11, WHITE);
      }
      if (statusButton != null && (statusButton.isFocused() || statusButton.isHovered())) {
         outline(graphics, x + 17, y + 156, 118, 14, AMBER);
      }
   }

   private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
      graphics.fill(x, y, x + width, y + 1, color);
      graphics.fill(x, y + height - 1, x + width, y + height, color);
      graphics.fill(x, y, x + 1, y + height, color);
      graphics.fill(x + width - 1, y, x + width, y + height, color);
   }

   private void drawTitleArtworkPatch(GuiGraphics graphics, int x, int y) {
      int targetX = x + 34;
      int targetY = y + 7;
      int targetWidth = 166;
      int targetHeight = 16;
      int targetTileWidth = 10;
      for (int offset = 0; offset < targetWidth; offset += targetTileWidth) {
         int tileWidth = Math.min(targetTileWidth, targetWidth - offset);
         int sourceWidth = Mth.ceil(tileWidth * 1168.0F / imageWidth);
         graphics.blit(FACEPLATE, targetX + offset, targetY, tileWidth, targetHeight,
            822, 27, sourceWidth, 46, 1168, 1346);
      }
   }

   private void drawLocalizedMachineTitle(GuiGraphics graphics) {
      Component title = Component.translatable(
         "block.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter"
      );
      float scale = fittedTextScale(font.width(title), 164, 0.82F);
      if (scale <= 0.0F) return;
      int centeredX = 117 - Mth.ceil(font.width(title) * scale) / 2;
      drawSmall(graphics, title, centeredX, 9, 164, 0xFF171614, 0.82F);
   }

   private static void gauge(GuiGraphics graphics, int x, int y, int width, int height, int value, int max, int color) {
      graphics.fill(x, y, x + width, y + height, 0xFF080A0C);
      int filled = value < 0 || max <= 0 ? 0 : Mth.clamp((int)((long)Math.min(value, max) * width / max), 0, width);
      if (filled > 0) graphics.fill(x, y, x + filled, y + height, color);
   }

   static float fittedTextScale(int textWidth, int maximumWidth, float preferredScale) {
      if (textWidth <= 0 || maximumWidth <= 0 || preferredScale <= 0.0F) return 0.0F;
      return Math.min(preferredScale, maximumWidth / (float)textWidth);
   }

   private void drawSmall(GuiGraphics graphics, Component value, int x, int y,
                          int maximumWidth, int color, float preferredScale) {
      float scale = fittedTextScale(font.width(value), maximumWidth, preferredScale);
      if (scale <= 0.0F) return;
      graphics.pose().pushPose();
      graphics.pose().translate(x, y, 0);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(font, value, 0, 0, color, false);
      graphics.pose().popPose();
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      drawLocalizedMachineTitle(graphics);
      int thermal = projectedField(MachineMenu.SMS_UI_THERMAL);
      int phase = projectedField(MachineMenu.SMS_UI_PHASE);
      Component phaseName = phase >= 0 && phase < PHASE_KEYS.length
         ? Component.translatable(PHASE_KEYS[phase]) : Component.translatable(PREFIX + "unknown");
      Component thermalLabel = thermal < 0
         ? Component.translatable(PREFIX + "thermal.unknown")
         : Component.translatable(PREFIX + "thermal", thermal, phaseName);
      drawSmall(graphics, thermalLabel, 20, 107, 84, thermal < 0 ? MUTED : AMBER, 0.65F);

      int comparator = projectedField(MachineMenu.SMS_UI_COMPARATOR);
      drawSmall(graphics, Component.translatable(PREFIX + "comparator", numberComponent(comparator)),
         36, 127, 68, comparator < 0 ? MUTED : WHITE, 0.58F);

      int hazard = projectedField(MachineMenu.SMS_UI_HAZARD);
      String surfaceKey = hazard < 0 ? "surface.unknown" : hazard == 1 ? "surface.hot" : "surface.cool";
      drawSmall(graphics, Component.translatable(PREFIX + surfaceKey),
         20, 140, 84, hazard == 1 ? RED : hazard < 0 ? MUTED : 0xFF81D77B, 0.68F);

      int batchMode = projectedField(MachineMenu.SMS_UI_BATCH_MODE);
      int batchProgress = projectedField(MachineMenu.SMS_UI_BATCH_PROGRESS);
      Component batchLabel = batchMode >= 0 && batchMode < MODE_KEYS.length
         ? Component.translatable(PREFIX + "batch.progress",
            modeComponent(batchMode), numberComponent(batchProgress))
         : Component.translatable(PREFIX + "batch.idle");
      drawSmall(graphics, batchLabel, 20, 150, 114, batchMode >= 0 ? AMBER : MUTED, 0.52F);

      int queued = projectedField(MachineMenu.SMS_UI_QUEUED_MODE);
      if (queued >= 0 && queued < MODE_KEYS.length) {
         drawSmall(graphics, Component.translatable(PREFIX + "next", modeComponent(queued)),
            20, 158, 114, AMBER, 0.52F);
      }

      int statusId = isUiSynchronized() ? menu.field(MachineMenu.SMS_UI_STATUS) : 0;
      drawSmall(graphics, statusLabelComponent(),
         20, 163, 114, requestFeedback == 0 ? statusColor(statusId) : RED, 0.52F);
      if (statusButton != null) {
         statusButton.setMessage(statusAccessibleComponent());
         statusButton.setTooltip(Tooltip.create(statusLabelComponent()));
      }

      int fluid = projectedField(MachineMenu.SMS_UI_TANK_AMOUNT);
      int fluidCapacity = projectedField(MachineMenu.SMS_UI_TANK_CAPACITY);
      drawSmall(graphics, Component.translatable(PREFIX + "fluid", numberComponent(fluid)),
         148, 102, 34, fluid < 0 ? MUTED : WHITE, 0.63F);
      drawSmall(graphics, Component.translatable(PREFIX + "capacity", numberComponent(fluidCapacity)),
         148, 110, 34, MUTED, 0.52F);

      int energy = projectedField(MachineMenu.SMS_UI_ENERGY);
      int energyCapacity = projectedField(MachineMenu.SMS_UI_ENERGY_CAPACITY);
      drawSmall(graphics, Component.translatable(PREFIX + "energy", numberComponent(energy)),
         185, 102, 39, energy < 0 ? MUTED : WHITE, 0.53F);
      drawSmall(graphics, Component.translatable(PREFIX + "energy_capacity", numberComponent(energyCapacity)),
         185, 110, 39, MUTED, 0.49F);

      int feRate = projectedField(MachineMenu.SMS_UI_FE_RATE);
      Component feRateValue = feRate < 0 ? Component.translatable(PREFIX + "unknown") : Component.literal(Integer.toString(feRate));
      drawSmall(graphics, Component.translatable(PREFIX + "fe_rate", feRateValue),
         191, 120, 39, feRate < 0 ? MUTED : WHITE, 0.58F);

      int redstone = projectedField(MachineMenu.SMS_UI_REDSTONE);
      String redstoneKey = redstone < 0 ? "redstone.unknown" : redstone == 1 ? "redstone.high" : "redstone.low";
      drawSmall(graphics, Component.translatable(PREFIX + redstoneKey),
         190, 141, 44, redstone == 1 ? RED : redstone < 0 ? MUTED : 0xFF81D77B, 0.65F);

      if (outputDrawerOpen) {
         drawSmall(graphics, Component.translatable(PREFIX + "drawer.title"), 191, 49, 25, AMBER, 0.54F);
         drawSmall(graphics, Component.translatable(PREFIX + "drawer.close"), 158, 71, 58, MUTED, 0.47F);
      } else {
         int selected = selectedMode();
         for (int i = 0; i < MODE_KEYS.length; i++) {
            Component mode = Component.translatable(MODE_KEYS[i]);
            drawSmall(graphics, mode, 183, 44 + i * 11, 32, selected == i ? AMBER : WHITE, 0.60F);
            if (modeButtons[i] != null) modeButtons[i].setMessage(modeAccessibleComponent(i));
         }
      }
   }

   private Component numberComponent(int value) {
      return value < 0 ? Component.translatable(PREFIX + "unknown") : Component.literal(Integer.toString(value));
   }

   private int statusColor(int status) {
      if (!isUiSynchronized() || status == 0) return MUTED;
      return switch (status) {
         case 1, 2 -> AMBER;
         case 9, 10 -> 0xFF81D77B;
         default -> RED;
      };
   }

   @Override
   public boolean keyPressed(int key, int scanCode, int modifiers) {
      if (key == GLFW.GLFW_KEY_ESCAPE && outputDrawerOpen) {
         toggleOutputDrawer();
         setFocused(outputDrawerButton);
         return true;
      }
      return super.keyPressed(key, scanCode, modifiers);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return super.mouseClicked(localX(mouseX), localY(mouseY), button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return super.mouseReleased(localX(mouseX), localY(mouseY), button);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      return super.mouseDragged(localX(mouseX), localY(mouseY), button, dragX / viewScale, dragY / viewScale);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return super.mouseScrolled(localX(mouseX), localY(mouseY), scrollX, scrollY);
   }

   @Override
   public void mouseMoved(double mouseX, double mouseY) {
      super.mouseMoved(localX(mouseX), localY(mouseY));
   }

   public float validationViewScale() { return viewScale; }

   public boolean validationOutputDrawerOpen() { return outputDrawerOpen; }

   @Override
   public void removed() {
      menu.setSmelterOutputDrawerOpen(false);
      super.removed();
   }
}
