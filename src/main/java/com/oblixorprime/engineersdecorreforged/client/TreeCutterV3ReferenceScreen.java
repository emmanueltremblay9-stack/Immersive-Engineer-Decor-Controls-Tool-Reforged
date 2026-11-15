package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.TreeCutterV3ReferenceGeometry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Normal Tree Cutter client presentation using retained V3 art. Vanilla owns items, overlays, carried stacks and packets. */
public final class TreeCutterV3ReferenceScreen extends AbstractContainerScreen<MachineMenu> {
   private static final String PREFIX = "gui.immersive_engineer_decor_controls_tool_reforged.tree_cutter.v3.";
   private static final ResourceLocation FACEPLATE = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/tree_cutter_v3_reference/background.png");
   private TreeCutterV3ReferenceGeometry.ViewTransform view = TreeCutterV3ReferenceGeometry.fit(418, 418);
   private boolean renderingCanvas;
   private double renderLocalMouseX, renderLocalMouseY;

   public TreeCutterV3ReferenceScreen(MachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      if (!menu.treeCutterV3ReferencePresentation()) throw new IllegalArgumentException("Tree Cutter V3 requires the client-side Tree Cutter presentation");
      imageWidth = TreeCutterV3ReferenceGeometry.CANVAS_WIDTH;
      imageHeight = TreeCutterV3ReferenceGeometry.CANVAS_HEIGHT;
   }

   @Override
   protected void init() {
      super.init();
      leftPos = 0; topPos = 0;
      view = TreeCutterV3ReferenceGeometry.fit(width, height);
   }

   // NeoForge exposes these bounds to JEI and other screen-space consumers.
   // Internal fields stay on the 418-by-418 logical canvas for vanilla slot input/rendering.
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
      if (menu.energyCapacity() > 0 && hoveredSlot == null && TreeCutterV3ReferenceGeometry.ENERGY.contains(view.localX(mouseX), view.localY(mouseY))) {
         renderBoundedTooltip(graphics, Component.translatable(PREFIX + "energy", menu.energyStored(), menu.energyCapacity()), mouseX, mouseY);
      } else if (hoveredSlot != null && hoveredSlot.index == 0 && !hoveredSlot.hasItem()) {
         renderBoundedTooltip(graphics, Component.translatable(PREFIX + "output"), mouseX, mouseY);
      } else if (hoveredSlot == null && TreeCutterV3ReferenceGeometry.HEADER.contains(view.localX(mouseX), view.localY(mouseY))) {
         renderBoundedTooltip(graphics, title, mouseX, mouseY);
      }
   }

   private void renderBoundedTooltip(GuiGraphics graphics, Component text, int mouseX, int mouseY) {
      // Native font splitting preserves translated styles/newlines and prevents long
      // French controls from becoming one oversized line on small/high-scale views.
      graphics.renderTooltip(font, font.split(text, TreeCutterV3ReferenceGeometry.tooltipWidth(width)), mouseX, mouseY);
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      // Draw the exact source uniformly; never resample or patch the retained artwork.
      graphics.pose().pushPose();
      try {
         graphics.pose().scale(TreeCutterV3ReferenceGeometry.SOURCE_SCALE, TreeCutterV3ReferenceGeometry.SOURCE_SCALE, 1.0F);
         graphics.blit(FACEPLATE, 0, 0, 0, 0,
            TreeCutterV3ReferenceGeometry.SOURCE_WIDTH, TreeCutterV3ReferenceGeometry.SOURCE_HEIGHT,
            TreeCutterV3ReferenceGeometry.SOURCE_WIDTH, TreeCutterV3ReferenceGeometry.SOURCE_HEIGHT);
      } finally {
         graphics.pose().popPose();
      }
      var energy = TreeCutterV3ReferenceGeometry.ENERGY;
      int filled = TreeCutterV3ReferenceGeometry.energyFillWidth(menu.energyStored(), menu.energyCapacity());
      if (filled > 0) graphics.fill(energy.x(), energy.y(), energy.x() + filled, energy.y() + energy.height(), 0xFFE5B539);
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
         for (var anchor : TreeCutterV3ReferenceGeometry.SLOT_ANCHORS) {
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

   /** Read-only hooks for native capture and event verification. */
   public TreeCutterV3ReferenceGeometry.ViewTransform validationViewTransform() { return view; }
   public boolean validationSlotHoveredAt(int index, double screenX, double screenY) {
      if (index < 0 || index >= menu.slots.size()) return false;
      var slot = menu.getSlot(index);
      return slot.isActive() && isHovering(slot.x, slot.y, 16, 16, view.localX(screenX), view.localY(screenY));
   }
}
