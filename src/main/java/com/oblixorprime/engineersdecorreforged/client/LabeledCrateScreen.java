package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.LabeledCrateMenu;
import com.oblixorprime.engineersdecorreforged.utility.CrateReferenceGeometry;

import com.oblixorprime.engineersdecorreforged.network.LabeledCrateQuickMovePayload;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;



/** Retained Crate V1 art. Vanilla owns items, overlays, carried stacks and packets. */
public final class LabeledCrateScreen extends AbstractContainerScreen<LabeledCrateMenu> {
   private static final ResourceLocation FACEPLATE = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/crate_reference_v1/background.png");
   private CrateReferenceGeometry.ViewTransform view = CrateReferenceGeometry.fit(374, 467);
   private boolean renderingCanvas;
   private double renderLocalMouseX, renderLocalMouseY;

   public LabeledCrateScreen(LabeledCrateMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      imageWidth = CrateReferenceGeometry.CANVAS_WIDTH;
      imageHeight = CrateReferenceGeometry.CANVAS_HEIGHT;
   }

   @Override
   protected void init() {
      super.init();
      leftPos = 0; topPos = 0;
      view = CrateReferenceGeometry.fit(width, height);
   }

   // NeoForge exposes these bounds to JEI and other screen-space consumers.
   // Internal fields stay on the 374x467 logical canvas for vanilla slot input/rendering.
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
      if (hoveredSlot == null && CrateReferenceGeometry.containsTitlePlaque(view.localX(mouseX), view.localY(mouseY))) {
         // Retain the menu's translated/custom title without painting over the blank source plaque.
         graphics.renderTooltip(font, font.split(title, Math.max(1, Math.min(240, width - 16))), mouseX, mouseY);
      }
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.blit(FACEPLATE, 0, 0, imageWidth, imageHeight, 0, 0,
         CrateReferenceGeometry.SOURCE_WIDTH, CrateReferenceGeometry.SOURCE_HEIGHT,
         CrateReferenceGeometry.SOURCE_WIDTH, CrateReferenceGeometry.SOURCE_HEIGHT);
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
         for (var anchor : CrateReferenceGeometry.SLOT_ANCHORS) {
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
   public CrateReferenceGeometry.ViewTransform validationViewTransform() { return view; }
   public boolean validationSlotHoveredAt(int index, double screenX, double screenY) {
      if (index < 0 || index >= menu.slots.size()) return false;
      var slot = menu.getSlot(index);
      return slot.isActive() && isHovering(slot.x, slot.y, 16, 16, view.localX(screenX), view.localY(screenY));
   }
   @Override
   protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
      if (type == ClickType.QUICK_MOVE && slot != null && slot.hasItem() && Screen.hasShiftDown() && Screen.hasControlDown()) {
         PacketDistributor.sendToServer(
            new LabeledCrateQuickMovePayload(this.menu.containerId, slotId), new CustomPacketPayload[0]
         );
         return;
      }
      super.slotClicked(slot, slotId, mouseButton, type);
   }
}
