package com.oblixorprime.engineersdecorreforged.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import org.lwjgl.glfw.GLFW;

/**
 * Freezer V5 industrial steel reference adapted for two vertical, single-stage machines.
 * Slot coordinates come from ThermalVerticalGeometry; this screen never alters gameplay.
 * No painted/drawn titles, recipe tabs or mode controls are used.
 */
public final class ThermalVerticalScreen extends AbstractContainerScreen<MachineMenu> {
   private static final String NS = "immersive_engineer_decor_controls_tool_reforged";
   private static final ResourceLocation FREEZER_ART = texture("freezer_reference_v5/background.png");
   private static final ResourceLocation FREEZER_CLEAN = texture("freezer_reference_v5/freezer_v5_chamber_clean_base.png");
   private static final List<ResourceLocation> FROST = java.util.stream.IntStream.rangeClosed(0, 4)
      .mapToObj(i -> texture("freezer_reference_v5/freezer_v5_frost_overlay_phase" + i + ".png")).toList();
   private static final ResourceLocation SMELTER_ART = texture("small_mineral_smelter_hybrid_v05/background.png");
   private FreezerReferenceGeometry.ViewTransform view = FreezerReferenceGeometry.fit(314, 314);
   private boolean renderingCanvas;
   private double localMouseX;
   private double localMouseY;
   private Button recoveryButton;

   private static ResourceLocation texture(String path) {
      return ResourceLocation.fromNamespaceAndPath(NS, "textures/gui/" + path);
   }

   public ThermalVerticalScreen(MachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      if (menu.kind() != MachineKind.SMALL_FREEZER && menu.kind() != MachineKind.SMALL_MINERAL_SMELTER)
         throw new IllegalArgumentException("Thermal vertical screen is for Freezer/Smelter only");
      imageWidth = 314;
      imageHeight = 314;
   }

   private boolean freezer() { return menu.kind() == MachineKind.SMALL_FREEZER; }

   @Override
   protected void init() {
      super.init();
      leftPos = 0;
      topPos = 0;
      view = FreezerReferenceGeometry.fit(width, height);
      if (freezer()) {
         recoveryButton = addRenderableWidget(Button.builder(
            Component.literal("↶"), b -> recoverLegacy())
            .bounds(246, 155, 16, 16).build());
         updateRecovery();
      }
   }

   private void updateRecovery() {
      if (recoveryButton != null) {
         recoveryButton.visible = menu.getSlot(1).hasItem();
         recoveryButton.active = recoveryButton.visible && menu.getCarried().isEmpty();
      }
   }

   private void recoverLegacy() {
      if (freezer() && menu.getSlot(1).hasItem() && menu.getCarried().isEmpty())
         slotClicked(menu.getSlot(1), 1, 0, ClickType.QUICK_MOVE);
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBg(graphics, partialTick, mouseX, mouseY);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, width, height, 0xB0000000);
      updateRecovery();
      graphics.pose().pushPose();
      try {
         graphics.pose().translate(view.originX(), view.originY(), 0);
         graphics.pose().scale(view.scale(), view.scale(), 1.0F);
         localMouseX = view.localX(mouseX);
         localMouseY = view.localY(mouseY);
         renderingCanvas = true;
         super.render(graphics, view.renderPointerX(mouseX), view.renderPointerY(mouseY), partialTick);
      } finally {
         renderingCanvas = false;
         graphics.pose().popPose();
      }
      renderTooltip(graphics, mouseX, mouseY);
      if (recoveryButton != null && recoveryButton.visible && recoveryButton.isHovered()) {
         graphics.renderTooltip(font, Component.literal("Recover reserved legacy items"), mouseX, mouseY);
      } else if (hoveredSlot == null && localOver(mouseX, mouseY, 52, 76, 18, 78)) {
         graphics.renderTooltip(font, Component.literal(menu.energyStored() + " / " + menu.energyCapacity() + " FE"),
            mouseX, mouseY);
      } else if (hoveredSlot == null && localOver(mouseX, mouseY, 106, 89, 102, 61)) {
         Component status = freezer()
            ? Component.literal("Cold stage " + menu.freezerPhase() + " / 4")
            : Component.literal("Heat phase " + menu.field(MachineMenu.SMS_UI_PHASE)
               + " | Lava " + menu.fluidAmount() + " mB");
         graphics.renderTooltip(font, status, mouseX, mouseY);
      }
   }

   private boolean localOver(double mouseX, double mouseY, int x, int y, int w, int h) {
      double px = view.localX(mouseX), py = view.localY(mouseY);
      return px >= x && px < x + w && py >= y && py < y + h;
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      // Reuse known Freezer steel art, not an unverified resized generated image.
      graphics.blit(FREEZER_CLEAN, 0, 0, imageWidth, imageHeight, 0, 0, 1254, 1254, 1254, 1254);
      if (freezer()) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         int phase = Math.max(0, Math.min(4, menu.freezerPhase()));
         graphics.blit(FROST.get(phase), 0, 0, imageWidth, imageHeight, 0, 0, 1254, 1254, 1254, 1254);
         RenderSystem.disableBlend();
      } else {
         // Hot chamber: actual refractory Smelter source, not orange-recolored frost.
         graphics.blit(SMELTER_ART, 106, 89, 102, 61,
            150, 170, 260, 300, 1168, 1346);
         int phase = menu.field(MachineMenu.SMS_UI_SCHEMA) == 3
            ? menu.field(MachineMenu.SMS_UI_PHASE) : -1;
         if (phase <= 0) graphics.fill(115, 99, 198, 140, 0xD010161B);
         else if (phase == 1) graphics.fill(115, 99, 198, 140, 0x80161412);
      }

      // Remove old lateral wells using independent patches of source steel/pipe artwork.
      // These are presentation regions only: no item can occupy them.
      graphics.blit(FREEZER_ART, 30, 119, 48, 48, 90, 220, 210, 210, 1254, 1254);
      graphics.blit(FREEZER_ART, 235, 119, 48, 48, 920, 220, 210, 210, 1254, 1254);
      // Move the original sculpted slot rims to the vertical center line.
      graphics.blit(FREEZER_ART, 137, 37, 32, 34, 154, 502, 130, 120, 1254, 1254);
      graphics.blit(FREEZER_ART, 137, 149, 32, 35, 964, 502, 130, 120, 1254, 1254);

      // The V5 reference contains lit energy cells; replace them with server-owned state.
      graphics.fill(56, 99, 66, 148, 0xFF0D1318);
      int capacity = Math.max(0, menu.energyCapacity());
      int stored = Math.max(0, menu.energyStored());
      int cells = capacity == 0 ? 0 : Math.min(10, (int)((long)stored * 10 / capacity));
      for (int i = 0; i < cells; i++) {
         int y = 144 - i * 5;
         graphics.fill(57, y, 65, y + 4, 0xFFFFB22C);
      }
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      // No title, inventory label, recipe names or permanently drawn data strings.
   }

   @Override
   protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
      if (renderingCanvas) {
         mouseX = localMouseX;
         mouseY = localMouseY;
      }
      return super.isHovering(x, y, w, h, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return super.mouseClicked(view.localX(mouseX), view.localY(mouseY), button);
   }
   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return super.mouseReleased(view.localX(mouseX), view.localY(mouseY), button);
   }
   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
      return super.mouseDragged(view.localX(mouseX), view.localY(mouseY), button,
         view.localDelta(dx), view.localDelta(dy));
   }
   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return super.mouseScrolled(view.localX(mouseX), view.localY(mouseY), scrollX, scrollY);
   }
   @Override
   public void mouseMoved(double mouseX, double mouseY) {
      super.mouseMoved(view.localX(mouseX), view.localY(mouseY));
   }
   @Override
   public boolean keyPressed(int key, int scanCode, int modifiers) {
      if (freezer() && key == GLFW.GLFW_KEY_R && hasControlDown() && hasShiftDown()
         && menu.getSlot(1).hasItem()) {
         recoverLegacy();
         return true;
      }
      return super.keyPressed(key, scanCode, modifiers);
   }
}
