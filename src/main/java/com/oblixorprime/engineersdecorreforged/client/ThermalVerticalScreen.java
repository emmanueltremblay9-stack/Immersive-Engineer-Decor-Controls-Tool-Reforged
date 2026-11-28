package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.Gui11V5CRuntimeGeometry;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.oblixorprime.engineersdecorreforged.utility.ThermalVerticalGeometry;
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
   public static final String FREEZER_RECOVERY_HELP_KEY = "gui." + NS + ".freezer_reference.recover_help";
   private static final float MAX_VIEW_SCALE = 0.78F;
   private FreezerReferenceGeometry.ViewTransform view = FreezerReferenceGeometry.fitCanvas(
      ThermalVerticalGeometry.CANVAS_WIDTH, ThermalVerticalGeometry.CANVAS_HEIGHT, 347, 381, MAX_VIEW_SCALE);
   private boolean renderingCanvas;
   private double localMouseX;
   private double localMouseY;
   private Button recoveryButton;

   public ThermalVerticalScreen(MachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      if (menu.kind() != MachineKind.SMALL_FREEZER && menu.kind() != MachineKind.SMALL_MINERAL_SMELTER)
         throw new IllegalArgumentException("Thermal vertical screen is for Freezer/Smelter only");
      imageWidth = ThermalVerticalGeometry.CANVAS_WIDTH;
      imageHeight = ThermalVerticalGeometry.CANVAS_HEIGHT;
   }

   private boolean freezer() { return menu.kind() == MachineKind.SMALL_FREEZER; }

   /** Read-only hook used by nonce-owned native GUI validation. */
   public FreezerReferenceGeometry.ViewTransform validationViewTransform() { return view; }

   /** Hit-test an actual menu slot through the same inverse canvas transform used by mouse input. */
   public boolean validationSlotHoveredAt(int menuIndex, double screenX, double screenY) {
      if (menuIndex < 0 || menuIndex >= menu.slots.size()) return false;
      var slot = menu.getSlot(menuIndex);
      return slot.isActive() && isHovering(slot.x, slot.y, 16, 16,
         view.localX(screenX), view.localY(screenY));
   }

   /** Read-only hooks for the nonce-owned native Freezer recovery validation. */
   public boolean validationRecoveryVisible() {
      return freezer() && recoveryButton != null && recoveryButton.visible;
   }

   public boolean validationRecoveryButtonHovered() {
      return validationRecoveryVisible() && recoveryButton.isHovered();
   }

   public int validationRecoveryButtonX() { return recoveryButton == null ? -1 : recoveryButton.getX(); }
   public int validationRecoveryButtonY() { return recoveryButton == null ? -1 : recoveryButton.getY(); }
   public int validationRecoveryButtonWidth() { return recoveryButton == null ? 0 : recoveryButton.getWidth(); }
   public int validationRecoveryButtonHeight() { return recoveryButton == null ? 0 : recoveryButton.getHeight(); }

   @Override
   protected void init() {
      super.init();
      leftPos = 0;
      topPos = 0;
      view = FreezerReferenceGeometry.fitCanvas(width, height, imageWidth, imageHeight, MAX_VIEW_SCALE);
      if (freezer()) {
         recoveryButton = addRenderableWidget(Button.builder(
            Component.literal("↶"), b -> recoverLegacy())
            .bounds(183, 19, 16, 16).build());
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
         Component recoveryHelp = Component.translatableWithFallback(
            FREEZER_RECOVERY_HELP_KEY,
            "Recover legacy slot 1 to your inventory. Ctrl+Shift+R. Full inventory: items stay stored.");
         // Keep the localized instruction inside narrow Prism windows without
         // changing its wording or the recovery control's hitbox.
         graphics.renderTooltip(font, font.split(recoveryHelp, Math.max(80, Math.min(240, width - 16))), mouseX, mouseY);
      } else if (hoveredSlot == null && energyContains(mouseX, mouseY)) {
         graphics.renderTooltip(font, Component.literal(menu.energyStored() + " / " + menu.energyCapacity() + " FE"),
            mouseX, mouseY);
      } else if (hoveredSlot == null && chamberContains(mouseX, mouseY)) {
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

   private boolean energyContains(double mouseX, double mouseY) {
      Gui11V5CRuntimeGeometry.Rect energy = Gui11V5CRuntimeGeometry.energy(menu.kind());
      return energy != null && energy.contains(view.localX(mouseX), view.localY(mouseY));
   }

   private boolean chamberContains(double mouseX, double mouseY) {
      String key = freezer() ? "cold-chamber" : "heat-chamber";
      Gui11V5CRuntimeGeometry.Rect chamber = Gui11V5CRuntimeGeometry.effectBounds(menu.kind(), key);
      return chamber != null && chamber.contains(view.localX(mouseX), view.localY(mouseY));
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      Gui11V5CArtwork.draw(graphics, menu);
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
