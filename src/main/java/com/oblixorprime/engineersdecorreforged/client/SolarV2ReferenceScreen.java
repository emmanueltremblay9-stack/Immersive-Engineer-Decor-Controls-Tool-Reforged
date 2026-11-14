package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.SolarV2ReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.SolarV2ReferenceState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Default client retained V2 art. Vanilla owns items, overlays, carried stacks and packets. */
public final class SolarV2ReferenceScreen extends AbstractContainerScreen<MachineMenu> {
   private static final String PREFIX = "gui.immersive_engineer_decor_controls_tool_reforged.small_solar_panel.";
   private static final ResourceLocation FACEPLATE = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/solar_v2_reference/background.png");
   private SolarV2ReferenceGeometry.ViewTransform view = SolarV2ReferenceGeometry.fit(418, 418);
   private boolean renderingCanvas;
   private double renderLocalMouseX, renderLocalMouseY;

   public SolarV2ReferenceScreen(MachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      if (!menu.solarV2ReferencePresentation()) throw new IllegalArgumentException("Solar V2 requires the client presentation");
      imageWidth = SolarV2ReferenceGeometry.CANVAS_WIDTH;
      imageHeight = SolarV2ReferenceGeometry.CANVAS_HEIGHT;
   }

   @Override
   protected void init() {
      super.init();
      leftPos = 0; topPos = 0;
      view = SolarV2ReferenceGeometry.fit(width, height);
   }

   // NeoForge exposes these bounds to JEI and other screen-space consumers.
   // Internal fields stay on the 418px logical canvas for vanilla slot input/rendering.
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
      if (hoveredSlot == null && SolarV2ReferenceState.containsMeter(view.localX(mouseX), view.localY(mouseY))) {
         Component energyTooltip = Component.translatable(PREFIX + "energy_tooltip",
            menu.energyStored(), menu.energyCapacity(), menu.energyDisplayGranularity());
         int wrapWidth = Math.max(1, Math.min(240, width - 16));
         graphics.renderTooltip(font, font.split(energyTooltip, wrapWidth), mouseX, mouseY);
      } else if (hoveredSlot != null && hoveredSlot.index == 0 && !hoveredSlot.hasItem()) {
         // Preserve the existing machine role; do not imply new charging compatibility.
         graphics.renderTooltip(font, Component.literal(menu.layout().machineSlots().getFirst().role()), mouseX, mouseY);
      }
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.blit(FACEPLATE, 0, 0, imageWidth, imageHeight, 0, 0,
         SolarV2ReferenceGeometry.SOURCE_WIDTH, SolarV2ReferenceGeometry.SOURCE_HEIGHT,
         SolarV2ReferenceGeometry.SOURCE_WIDTH, SolarV2ReferenceGeometry.SOURCE_HEIGHT);
      int fill = SolarV2ReferenceState.fillWidth(menu.energyStored(), menu.energyCapacity());
      if (fill > 0) {
         // The retained source has an empty meter. Overlay only the existing live FE
         // colours, never erase its backing or modify its bevel/frame at zero charge.
         // Scissor coordinates must use the unscaled screen space, not the canvas pose.
         graphics.enableScissor(
            (int)Math.ceil(view.screenX(SolarV2ReferenceState.METER_X)),
            (int)Math.ceil(view.screenY(SolarV2ReferenceState.METER_Y)),
            (int)Math.floor(view.screenX(SolarV2ReferenceState.METER_X + SolarV2ReferenceState.METER_WIDTH)),
            (int)Math.floor(view.screenY(SolarV2ReferenceState.METER_Y + SolarV2ReferenceState.METER_HEIGHT)));
         try {
            graphics.fill(SolarV2ReferenceState.METER_X, SolarV2ReferenceState.METER_Y,
               SolarV2ReferenceState.METER_X + fill, SolarV2ReferenceState.METER_Y + SolarV2ReferenceState.METER_HEIGHT,
               SolarV2ReferenceState.FILL_COLOR);
            if (fill > 1) {
               graphics.fill(SolarV2ReferenceState.METER_X, SolarV2ReferenceState.METER_Y,
                  SolarV2ReferenceState.METER_X + fill, SolarV2ReferenceState.METER_Y + 1,
                  SolarV2ReferenceState.HIGHLIGHT_COLOR);
            }
         } finally {
            graphics.disableScissor();
         }
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
         for (var anchor : SolarV2ReferenceGeometry.SLOT_ANCHORS) {
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
   public SolarV2ReferenceGeometry.ViewTransform validationViewTransform() { return view; }
   public boolean validationSlotHoveredAt(int index, double screenX, double screenY) {
      if (index < 0 || index >= menu.slots.size()) return false;
      var slot = menu.getSlot(index);
      return slot.isActive() && isHovering(slot.x, slot.y, 16, 16, view.localX(screenX), view.localY(screenY));
   }
}
