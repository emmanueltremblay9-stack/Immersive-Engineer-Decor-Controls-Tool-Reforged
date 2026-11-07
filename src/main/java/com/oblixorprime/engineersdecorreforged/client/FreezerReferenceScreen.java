package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceLiveState;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import org.lwjgl.glfw.GLFW;

/**
 * DEVELOPMENT/VALIDATION experiment, not the production Freezer GUI.
 * Exact snapshot mode retains the whole PNG with baked frost and energy.
 * Separate live-energy mode samples its source cells using synchronized FE. Frost
 * remains static in both modes, explicitly disclosed alongside real progress/status.
 * Vanilla still owns slot rendering, item count/durability overlays, carried stacks,
 * selection, drag splitting and packets. Only the presentation coordinate space changes.
 */
public final class FreezerReferenceScreen extends AbstractContainerScreen<MachineMenu> {
   private static final String PREFIX = "gui.immersive_engineer_decor_controls_tool_reforged.freezer_reference.";
   private static final String FREEZER_PREFIX = "gui.immersive_engineer_decor_controls_tool_reforged.small_freezer.";
   private static final ResourceLocation FACEPLATE = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/freezer_reference_v5/background.png");
   private FreezerReferenceGeometry.ViewTransform view = FreezerReferenceGeometry.fit(314, 314);
   private final FreezerReferenceLiveState.PreviewMode previewMode;
   private Button recoveryButton;
   private boolean renderingCanvas;
   private double renderLocalMouseX;
   private double renderLocalMouseY;

   public FreezerReferenceScreen(MachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      if (!menu.freezerReferencePresentation()) {
         throw new IllegalArgumentException("Exact reference screen requires an opted-in client presentation");
      }
      previewMode = FreezerReferenceLiveState.previewMode(
         Boolean.getBoolean(MachineMenu.DEV_FREEZER_REFERENCE_PROPERTY),
         Boolean.getBoolean(MachineMenu.DEV_FREEZER_LIVE_ENERGY_PROPERTY));
      imageWidth = FreezerReferenceGeometry.CANVAS_WIDTH;
      imageHeight = FreezerReferenceGeometry.CANVAS_HEIGHT;
   }

   @Override
   protected void init() {
      super.init();
      // The same established virtual-canvas pattern as SmallMineralSmelterHybridScreen.
      // Do not change the player's global GUI scale or stretch to a legacy rectangle.
      leftPos = 0;
      topPos = 0;
      view = FreezerReferenceGeometry.fit(width, height);
      recoveryButton = addRenderableWidget(Button.builder(
         Component.translatableWithFallback(PREFIX + "recover", "Recover reserved items"),
         button -> recoverReservedItems()).bounds(70, 83, 174, 20).build());
      updateRecoveryButton();
   }

   private void updateRecoveryButton() {
      // Not a third normal slot/opening. This exceptional recovery control is visible
      // only when historical slot 1 actually holds items, and is excluded from empty-art QA.
      if (recoveryButton != null) {
         recoveryButton.visible = menu.getSlot(1).hasItem();
         recoveryButton.active = recoveryButton.visible && menu.getCarried().isEmpty();
      }
   }

   private void recoverReservedItems() {
      if (menu.getSlot(1).hasItem() && menu.getCarried().isEmpty()) {
         // Standard container click/quick-move, same slot ID and server rules as before.
         // If the inventory is full, contents remain safe and the control remains visible.
         slotClicked(menu.getSlot(1), 1, 0, ClickType.QUICK_MOVE);
      }
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBg(graphics, partialTick, mouseX, mouseY);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, width, height, 0xB0000000);
      updateRecoveryButton();
      graphics.pose().pushPose();
      try {
         graphics.pose().translate(view.originX(), view.originY(), 0);
         graphics.pose().scale(view.scale(), view.scale(), 1.0F);
         // Keep raw coordinates for slot hover/click parity at fractional art boundaries.
         renderLocalMouseX = view.localX(mouseX);
         renderLocalMouseY = view.localY(mouseY);
         renderingCanvas = true;
         // floor, not Java's truncation toward zero, keeps negative pointers outside.
         super.render(graphics, view.renderPointerX(mouseX), view.renderPointerY(mouseY), partialTick);
      } finally {
         renderingCanvas = false;
         graphics.pose().popPose();
      }
      // Tooltips use the real screen coordinates and normal font size, outside the pose.
      renderTooltip(graphics, mouseX, mouseY);
      if (recoveryButton != null && recoveryButton.visible && recoveryButton.isHovered()) {
         graphics.renderTooltip(font, Component.translatableWithFallback(PREFIX + "recover_help",
            "Recover legacy slot 1 to your inventory. Ctrl+Shift+R. Full inventory: items stay stored."), mouseX, mouseY);
      } else if (hoveredSlot == null && overStaticGauge(mouseX, mouseY)) {
         if (previewMode == FreezerReferenceLiveState.PreviewMode.LIVE_ENERGY) {
            renderLiveTooltip(graphics, mouseX, mouseY);
         } else {
            graphics.renderTooltip(font, Component.translatableWithFallback(PREFIX + "static_art",
               "STATIC REFERENCE ART: frost and energy are not live."), mouseX, mouseY);
         }
      }
   }

   private double sourceMouseX(double mouseX) {
      return view.localX(mouseX) * FreezerReferenceGeometry.SOURCE_WIDTH / imageWidth;
   }

   private double sourceMouseY(double mouseY) {
      return view.localY(mouseY) * FreezerReferenceGeometry.SOURCE_HEIGHT / imageHeight;
   }

   private boolean overStaticGauge(double mouseX, double mouseY) {
      double x = sourceMouseX(mouseX);
      double y = sourceMouseY(mouseY);
      return FreezerReferenceLiveState.FROST_HIT_REGION.contains(x, y)
         || FreezerReferenceLiveState.ENERGY_HIT_REGION.contains(x, y);
   }

   private FreezerReferenceLiveState.EnergyState liveEnergy() {
      return FreezerReferenceLiveState.energy(menu.energyStored(), menu.energyCapacity());
   }

   private FreezerReferenceLiveState.FrostState liveFrost() {
      return FreezerReferenceLiveState.frost(menu.freezerProgressTicks(), MachineBlockEntity.FREEZER_PROCESS_TICKS,
         menu.freezerPhase(), menu.freezerStatus(), menu.freezerRedstonePowered());
   }

   private void renderLiveTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
      if (FreezerReferenceLiveState.ENERGY_HIT_REGION.contains(sourceMouseX(mouseX), sourceMouseY(mouseY))) {
         var energy = liveEnergy();
         graphics.renderTooltip(font, List.of(
            Component.translatable(FREEZER_PREFIX + "tooltip.energy", energy.stored(), energy.capacity()),
            Component.translatableWithFallback(PREFIX + "live_energy_cells",
               "Live energy: %s / %s segments (rounded down).", energy.litCells(), FreezerReferenceLiveState.ENERGY_CELLS)),
            Optional.empty(), mouseX, mouseY);
      } else {
         var frost = liveFrost();
         graphics.renderTooltip(font, List.of(
            Component.translatableWithFallback(PREFIX + "static_frost",
               "STATIC FROST ART: no clean chamber layer is available."),
            Component.translatable(FREEZER_PREFIX + "tooltip.progress", frost.ticks(), frost.totalTicks()),
            Component.translatable(FREEZER_PREFIX + "tooltip.phase", frost.phase()),
            Component.translatable(FREEZER_PREFIX + "tooltip.status",
               Component.translatable(FREEZER_PREFIX + "status." + MachineScreen.smallFreezerStatusKey(frost.status())),
               Component.translatable(FREEZER_PREFIX + (frost.redstonePowered() ? "redstone.high" : "redstone.low")))),
            Optional.empty(), mouseX, mouseY);
      }
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      // The immutable original remains the base, including all frost and housing.
      graphics.blit(FACEPLATE, 0, 0, imageWidth, imageHeight, 0, 0,
         FreezerReferenceGeometry.SOURCE_WIDTH, FreezerReferenceGeometry.SOURCE_HEIGHT,
         FreezerReferenceGeometry.SOURCE_WIDTH, FreezerReferenceGeometry.SOURCE_HEIGHT);
      var patches = FreezerReferenceLiveState.energyPatches(previewMode, liveEnergy());
      if (!patches.isEmpty()) {
         graphics.pose().pushPose();
         try {
            // Preserve source-pixel boundaries. Never round each cell to canvas ints.
            graphics.pose().scale((float)imageWidth / FreezerReferenceGeometry.SOURCE_WIDTH,
               (float)imageHeight / FreezerReferenceGeometry.SOURCE_HEIGHT, 1.0F);
            for (var patch : patches) {
               var source = patch.source();
               var destination = patch.destination();
               graphics.blit(FACEPLATE, destination.x(), destination.y(), destination.width(), destination.height(),
                  source.x(), source.y(), source.width(), source.height(),
                  FreezerReferenceGeometry.SOURCE_WIDTH, FreezerReferenceGeometry.SOURCE_HEIGHT);
            }
         } finally {
            graphics.pose().popPose();
         }
      }
      // No frosting overlay: the original frost cannot be separated without a clean layer.
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      // Preserve the retained reference's blank title plate and inventory artwork.
   }

   @Override
   protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
      if (renderingCanvas) {
         // Vanilla's render API takes ints, but hit testing must not round a pointer
         // just outside the measured interior back into a highlighted slot.
         mouseX = renderLocalMouseX;
         mouseY = renderLocalMouseY;
      }
      if (!super.isHovering(x, y, width, height, mouseX, mouseY)) return false;
      if (width == 16 && height == 16) {
         for (var anchor : FreezerReferenceGeometry.SLOT_ANCHORS) {
            if (anchor.x() == x && anchor.y() == y) {
               // Clip only vanilla's extra hover padding to the measured interior;
               // the complete 16x16 item/highlight stays unchanged and fully contained.
               return anchor.containsHover(mouseX - leftPos, mouseY - topPos);
            }
         }
      }
      return true;
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
   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      return super.mouseDragged(view.localX(mouseX), view.localY(mouseY), button,
         view.localDelta(dragX), view.localDelta(dragY));
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      // Wheel values are not pixel deltas and must not be scaled.
      return super.mouseScrolled(view.localX(mouseX), view.localY(mouseY), scrollX, scrollY);
   }

   @Override
   public void mouseMoved(double mouseX, double mouseY) {
      super.mouseMoved(view.localX(mouseX), view.localY(mouseY));
   }

   @Override
   public boolean keyPressed(int key, int scanCode, int modifiers) {
      if (key == GLFW.GLFW_KEY_R && hasControlDown() && hasShiftDown() && menu.getSlot(1).hasItem()) {
         recoverReservedItems();
         return true;
      }
      return super.keyPressed(key, scanCode, modifiers);
   }

   /** Read-only hooks for a future real Minecraft native-capture fixture. */
   public FreezerReferenceGeometry.ViewTransform validationViewTransform() { return view; }
   public boolean validationRecoveryVisible() { return recoveryButton != null && recoveryButton.visible; }
   public boolean validationSlotHoveredAt(int menuIndex, double screenX, double screenY) {
      if (menuIndex < 0 || menuIndex >= menu.slots.size()) return false;
      var slot = menu.getSlot(menuIndex);
      return slot.isActive() && isHovering(slot.x, slot.y, 16, 16, view.localX(screenX), view.localY(screenY));
   }
}
