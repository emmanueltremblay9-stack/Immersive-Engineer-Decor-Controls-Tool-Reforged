package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.BarrelReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.BarrelReferenceState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Default-off retained V5 art. Vanilla owns items, overlays, carried stacks and packets. */
public final class BarrelReferenceScreen extends AbstractContainerScreen<MachineMenu> {
   private static final String PREFIX = "gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.";
   private static final ResourceLocation FACEPLATE = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/barrel_reference_v5/background.png");
   private BarrelReferenceGeometry.ViewTransform view = BarrelReferenceGeometry.fit(379, 461);
   private boolean renderingCanvas;
   private double renderLocalMouseX, renderLocalMouseY;

   public BarrelReferenceScreen(MachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      if (!menu.barrelReferencePresentation()) throw new IllegalArgumentException("Barrel V5 requires explicit client opt-in");
      imageWidth = BarrelReferenceGeometry.CANVAS_WIDTH;
      imageHeight = BarrelReferenceGeometry.CANVAS_HEIGHT;
   }

   @Override
   protected void init() {
      super.init();
      leftPos = 0; topPos = 0;
      view = BarrelReferenceGeometry.fit(width, height);
   }

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
      if (hoveredSlot == null && BarrelReferenceState.containsTank(view.localX(mouseX), view.localY(mouseY))) {
         Component name = menu.storedFluid() == Fluids.EMPTY
            ? Component.translatable(PREFIX + "fluid.empty")
            : new FluidStack(menu.storedFluid(), Math.max(1, menu.fluidAmount())).getHoverName();
         graphics.renderTooltip(font, Component.translatable(PREFIX + "tank.tooltip",
            name, menu.fluidAmount(), menu.fluidCapacity()), mouseX, mouseY);
      } else if (hoveredSlot != null && hoveredSlot.index < 2 && !hoveredSlot.hasItem()) {
         graphics.renderTooltip(font, Component.translatable(PREFIX + (hoveredSlot.index == 0 ? "bucket.input" : "bucket.output")), mouseX, mouseY);
      }
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.blit(FACEPLATE, 0, 0, imageWidth, imageHeight, 0, 0,
         BarrelReferenceGeometry.SOURCE_WIDTH, BarrelReferenceGeometry.SOURCE_HEIGHT,
         BarrelReferenceGeometry.SOURCE_WIDTH, BarrelReferenceGeometry.SOURCE_HEIGHT);
      int fill = BarrelReferenceState.fillHeight(menu.fluidAmount(), menu.fluidCapacity());
      if (fill > 0 && menu.storedFluid() != Fluids.EMPTY) {
         // The existing renderer resolves the current native atlas/tint each frame and
         // clips tiles using explicitly transformed screen-space scissor bounds. Missing sprites
         // leave the original dark aperture visible; never fabricate a fluid colour.
         FluidBarrelSpriteRenderer.draw(graphics, new FluidStack(menu.storedFluid(), menu.fluidAmount()),
            BarrelReferenceState.TANK_X, BarrelReferenceState.TANK_Y + BarrelReferenceState.TANK_HEIGHT - fill,
            BarrelReferenceState.TANK_WIDTH, fill,
            (int)Math.ceil(view.screenX(BarrelReferenceState.TANK_X)),
            (int)Math.ceil(view.screenY(BarrelReferenceState.TANK_Y + BarrelReferenceState.TANK_HEIGHT - fill)),
            (int)Math.floor(view.screenX(BarrelReferenceState.TANK_X + BarrelReferenceState.TANK_WIDTH)),
            (int)Math.floor(view.screenY(BarrelReferenceState.TANK_Y + BarrelReferenceState.TANK_HEIGHT)));
      }
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
         for (var anchor : BarrelReferenceGeometry.SLOT_ANCHORS) {
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
   public BarrelReferenceGeometry.ViewTransform validationViewTransform() { return view; }
   public boolean validationSlotHoveredAt(int index, double screenX, double screenY) {
      if (index < 0 || index >= menu.slots.size()) return false;
      var slot = menu.getSlot(index);
      return slot.isActive() && isHovering(slot.x, slot.y, 16, 16, view.localX(screenX), view.localY(screenY));
   }
}
