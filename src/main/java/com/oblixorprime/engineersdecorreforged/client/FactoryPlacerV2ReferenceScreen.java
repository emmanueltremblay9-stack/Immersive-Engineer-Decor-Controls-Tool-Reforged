package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.FactoryPlacerV2ReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.FactoryPlacerV2ReferenceState;
import com.oblixorprime.engineersdecorreforged.network.MachineActionPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Default-off retained V2 art. Vanilla owns items, overlays, carried stacks and packets. */
public final class FactoryPlacerV2ReferenceScreen extends AbstractContainerScreen<MachineMenu> {
   private static final String PREFIX = "gui.immersive_engineer_decor_controls_tool_reforged.factory_placer.v2.";
   private static final ResourceLocation FACEPLATE = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/factory_placer_v2_reference/background.png");
   private FactoryPlacerV2ReferenceGeometry.ViewTransform view = FactoryPlacerV2ReferenceGeometry.fit(400, 438);
   private boolean renderingCanvas;
   private double renderLocalMouseX, renderLocalMouseY;

   public FactoryPlacerV2ReferenceScreen(MachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      if (!menu.factoryPlacerV2ReferencePresentation()) throw new IllegalArgumentException("Factory Placer V2 requires explicit client opt-in");
      imageWidth = FactoryPlacerV2ReferenceGeometry.CANVAS_WIDTH;
      imageHeight = FactoryPlacerV2ReferenceGeometry.CANVAS_HEIGHT;
   }

   @Override
   protected void init() {
      super.init();
      leftPos = 0; topPos = 0;
      view = FactoryPlacerV2ReferenceGeometry.fit(width, height);
   }

   // NeoForge exposes these bounds to JEI and other screen-space consumers.
   // Internal fields stay on the 400-by-438 logical canvas for vanilla slot input/rendering.
   @Override
   public int getGuiLeft() { return view.guiLeft(); }

   @Override
   public int getGuiTop() { return view.guiTop(); }

   @Override
   public int getXSize() { return view.guiWidth(); }

   @Override
   public int getYSize() { return view.guiHeight(); }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBg(graphics, partialTick, mouseX, mouseY);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, width, height, 0xB0000000);
      graphics.pose().pushPose();
      try {
         graphics.pose().translate(view.originX(), view.originY(), 0);
         graphics.pose().scale(view.scale(), view.scale(), 1.0F);
         renderLocalMouseX = view.localX(mouseX); renderLocalMouseY = view.localY(mouseY);
         renderingCanvas = true;
         super.render(graphics, view.renderPointerX(mouseX), view.renderPointerY(mouseY), partialTick);
      } finally {
         renderingCanvas = false;
         graphics.pose().popPose();
      }
      renderTooltip(graphics, mouseX, mouseY);
      var control = FactoryPlacerV2ReferenceGeometry.controlAt(view.localX(mouseX), view.localY(mouseY));
      if (hoveredSlot == null && control != null) {
         renderBoundedTooltip(graphics, controlTooltip(control), mouseX, mouseY);
      } else if (hoveredSlot != null && hoveredSlot.index < 18 && !hoveredSlot.hasItem()) {
         renderBoundedTooltip(graphics, Component.translatable(PREFIX + "slot_tooltip",
            hoveredSlot.index + 1, FactoryPlacerV2ReferenceState.selectedSlot(menu.field(2)) + 1), mouseX, mouseY);
      } else if (hoveredSlot == null && FactoryPlacerV2ReferenceGeometry.HEADER.contains(view.localX(mouseX), view.localY(mouseY))) {
         renderBoundedTooltip(graphics, title, mouseX, mouseY);
      }
   }

   private void renderBoundedTooltip(GuiGraphics graphics, Component text, int mouseX, int mouseY) {
      // Native font splitting preserves translated styles/newlines and prevents long
      // French controls from becoming one oversized line on small/high-scale views.
      graphics.renderTooltip(font, font.split(text, FactoryPlacerV2ReferenceGeometry.tooltipWidth(width)), mouseX, mouseY);
   }

   private Component controlTooltip(FactoryPlacerV2ReferenceGeometry.Control control) {
      return switch (control) {
         case ROW_TOP, ROW_MIDDLE, ROW_BOTTOM -> Component.translatable(PREFIX + "row_tooltip",
            control.ordinal() + 1, FactoryPlacerV2ReferenceState.selectedRow(menu.field(2)) + 1,
            FactoryPlacerV2ReferenceState.selectedSlot(menu.field(2)) + 1);
         case MANUAL_TRIGGER -> Component.translatable(PREFIX + "manual_tooltip",
            Component.translatable(PREFIX + (FactoryPlacerV2ReferenceState.signalActive(menu.field(1)) ? "signal_on" : "signal_off")));
         case INVERSION -> Component.translatable(PREFIX + "redstone_tooltip",
            Component.translatable(PREFIX + "redstone_" + FactoryPlacerV2ReferenceState.redstoneMode(menu.field(0))));
         case TRIGGER_MODE -> Component.translatable(PREFIX + "trigger_tooltip",
            Component.translatable(PREFIX + (FactoryPlacerV2ReferenceState.continuous(menu.field(0)) ? "continuous" : "edge")));
      };
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      // The odd source dimensions must not be squeezed into the rounded logical canvas.
      graphics.pose().pushPose();
      try {
         graphics.pose().scale(FactoryPlacerV2ReferenceGeometry.SOURCE_SCALE, FactoryPlacerV2ReferenceGeometry.SOURCE_SCALE, 1.0F);
         graphics.blit(FACEPLATE, 0, 0, 0, 0,
            FactoryPlacerV2ReferenceGeometry.SOURCE_WIDTH, FactoryPlacerV2ReferenceGeometry.SOURCE_HEIGHT,
            FactoryPlacerV2ReferenceGeometry.SOURCE_WIDTH, FactoryPlacerV2ReferenceGeometry.SOURCE_HEIGHT);
      } finally {
         graphics.pose().popPose();
      }
      var selected = FactoryPlacerV2ReferenceGeometry.anchor(FactoryPlacerV2ReferenceState.selectedSlot(menu.field(2)));
      double guiScale = minecraft.getWindow().getGuiScale();
      if (view.scale() * guiScale >= 1.0D) {
         graphics.renderOutline(selected.x() - 1, selected.y() - 1, 18, 18, FactoryPlacerV2ReferenceState.SELECTED_COLOR);
      } else {
         for (var edge : FactoryPlacerV2ReferenceGeometry.selectionEdges(view, guiScale, selected.menuIndex())) {
            // Keep the existing canvas pose and item pass. Only this state overlay is
            // quantized to physical pixels; its strips lie outside the item footprint.
            double x = view.localX(edge.left() / guiScale), y = view.localY(edge.top() / guiScale);
            double right = view.localX(edge.right() / guiScale), bottom = view.localY(edge.bottom() / guiScale);
            graphics.pose().pushPose();
            try {
               graphics.pose().translate(x, y, 0);
               graphics.pose().scale((float)(right - x), (float)(bottom - y), 1.0F);
               graphics.fill(0, 0, 1, 1, FactoryPlacerV2ReferenceState.SELECTED_COLOR);
            } finally {
               graphics.pose().popPose();
            }
         }
      }
      fill(graphics, FactoryPlacerV2ReferenceGeometry.rowIndicator(FactoryPlacerV2ReferenceState.selectedRow(menu.field(2))),
         FactoryPlacerV2ReferenceState.SELECTED_COLOR);
      fill(graphics, FactoryPlacerV2ReferenceGeometry.SIGNAL_INDICATOR,
         FactoryPlacerV2ReferenceState.signalActive(menu.field(1))
            ? FactoryPlacerV2ReferenceState.SIGNAL_ON_COLOR : FactoryPlacerV2ReferenceState.SIGNAL_OFF_COLOR);
      var redstone = FactoryPlacerV2ReferenceGeometry.INVERSION_INDICATOR;
      for (int mode = 0; mode < 3; mode++) {
         graphics.fill(redstone.x() + mode * 11, redstone.y(), redstone.x() + mode * 11 + 6, redstone.y() + redstone.height(),
            mode == FactoryPlacerV2ReferenceState.redstoneMode(menu.field(0))
               ? FactoryPlacerV2ReferenceState.ACTIVE_COLOR : FactoryPlacerV2ReferenceState.INACTIVE_COLOR);
      }
      var trigger = FactoryPlacerV2ReferenceGeometry.TRIGGER_INDICATOR;
      for (int mode = 0; mode < 2; mode++) {
         graphics.fill(trigger.x() + mode * 16, trigger.y(), trigger.x() + mode * 16 + 10, trigger.y() + trigger.height(),
            (mode == 1) == FactoryPlacerV2ReferenceState.continuous(menu.field(0))
               ? FactoryPlacerV2ReferenceState.ACTIVE_COLOR : FactoryPlacerV2ReferenceState.INACTIVE_COLOR);
      }
   }

   private static void fill(GuiGraphics graphics, FactoryPlacerV2ReferenceGeometry.Rect rect, int color) {
      graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), color);
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      // Keep the retained blank plaque and all frame pixels intact. Labels live in tooltips.
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
         for (var anchor : FactoryPlacerV2ReferenceGeometry.SLOT_ANCHORS) {
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
      var action = FactoryPlacerV2ReferenceState.actionAt(view.localX(mouseX), view.localY(mouseY),
         button, !menu.getCarried().isEmpty(), menu.field(0));
      if (action != null) {
         PacketDistributor.sendToServer(new MachineActionPayload(menu.containerId, action.action(), action.valueA(), 0));
         // No optimistic field mutation, fake press effect, or invented placement success.
         return true;
      }
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

   /** Read-only hooks for native capture and event verification. */
   public FactoryPlacerV2ReferenceGeometry.ViewTransform validationViewTransform() { return view; }
   public boolean validationSlotHoveredAt(int index, double screenX, double screenY) {
      if (index < 0 || index >= menu.slots.size()) return false;
      var slot = menu.getSlot(index);
      return slot.isActive() && isHovering(slot.x, slot.y, 16, 16, view.localX(screenX), view.localY(screenY));
   }
}
