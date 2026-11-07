package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.network.MachineActionPayload;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.GuiTextureBlit;
import com.oblixorprime.engineersdecorreforged.utility.MachineLayout;
import com.oblixorprime.engineersdecorreforged.utility.PassiveFluidAccumulatorGauge;
import com.oblixorprime.engineersdecorreforged.utility.SmallFreezerArtworkMasks;
import com.oblixorprime.engineersdecorreforged.utility.MilkingState;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;

public abstract class MachineScreen extends AbstractContainerScreen<MachineMenu> {
   private static final int FRAME = -14934755;
   private static final int BODY = -12960450;
   private static final int BODY_DARK = -13618380;
   private static final int PANEL = -13881552;
   private static final int PANEL_EDGE = -10328475;
   private static final int PANEL_HIGHLIGHT = -8946823;
   private static final int TEXT = -1646126;
   private static final int MUTED = -3949142;
   private static final int HOVER = -1917873;
   private static final String BLOCK_BREAKER_GUI_PREFIX = "gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.";
   private static final String SMALL_FREEZER_GUI_PREFIX = "gui.immersive_engineer_decor_controls_tool_reforged.small_freezer.";
   private static final String MILKING_GUI_PREFIX = "gui.immersive_engineer_decor_controls_tool_reforged.small_milking_machine.";
   private final MachineScreen.Profile profile;
   private boolean suppressTooltipsForPanelCapture;

   protected MachineScreen(MachineMenu menu, Inventory inventory, Component title, MachineScreen.Profile profile) {
      super(menu, inventory, title);
      this.profile = profile;
      this.imageWidth = menu.layout().imageWidth();
      this.imageHeight = menu.layout().imageHeight();
      this.titleLabelX = 8;
      this.titleLabelY = 6;
      this.inventoryLabelX = menu.layout().playerInventoryX();
      this.inventoryLabelY = menu.layout().playerInventoryY() - 10;
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderSoftBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      if (this.suppressTooltipsForPanelCapture) return;
      this.renderOriginalHoverOverlay(graphics, mouseX, mouseY);
      this.renderTooltip(graphics, mouseX, mouseY);
      this.renderMachineTooltip(graphics, mouseX, mouseY);
   }

   @Override
   protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
      if (!this.suppressTooltipsForPanelCapture) super.renderTooltip(graphics, mouseX, mouseY);
   }

   /** Hides pointer tooltips only while taking clean native panel crops. */
   public void setSuppressTooltipsForPanelCapture(boolean suppress) {
      this.suppressTooltipsForPanelCapture = suppress;
   }

   public boolean isSuppressTooltipsForPanelCapture() {
      return this.suppressTooltipsForPanelCapture;
   }

   public boolean isPanelPointerOutsideAt(double mouseX, double mouseY) {
      return mouseX < this.leftPos || mouseX >= this.leftPos + this.imageWidth
         || mouseY < this.topPos || mouseY >= this.topPos + this.imageHeight;
   }

   /** Exposes the vanilla slot-sized hover check to the isolated native GUI validation fixture. */
   public boolean isMachineSlotHoveredAt(int slotListIndex, double mouseX, double mouseY) {
      if (slotListIndex < 0 || slotListIndex >= this.menu.slots.size()) return false;
      var slot = this.menu.getSlot(slotListIndex);
      return slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
   }

   private void renderSoftBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (this.minecraft != null && this.minecraft.level != null) {
         graphics.fill(0, 0, this.width, this.height, 855638016);
      } else {
         this.renderBackground(graphics, mouseX, mouseY, partialTick);
      }
   }

   static boolean usesExplicitTextureDimensions(MachineKind kind) {
      return kind == MachineKind.SMALL_BLOCK_BREAKER || kind == MachineKind.FLUID_BARREL;
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      MachineLayout layout = ((MachineMenu)this.menu).layout();
      int x = this.leftPos;
      int y = this.topPos;
      if (layout.style() == MachineLayout.LayoutStyle.ARTWORK_TEXTURE) {
         ResourceLocation texture = this.backgroundTexture(layout);
         GuiTextureBlit geometry = layout.artworkBlit();
         graphics.blit(
            texture,
            x,
            y,
            geometry.destinationWidth(),
            geometry.destinationHeight(),
            (float)geometry.sourceX(),
            (float)geometry.sourceY(),
            geometry.sourceWidth(),
            geometry.sourceHeight(),
            geometry.textureWidth(),
            geometry.textureHeight()
         );
         this.drawArtworkBackgroundOverlays(graphics, layout, texture, x, y);
      } else if (layout.style() == MachineLayout.LayoutStyle.ORIGINAL_TEXTURE) {
         ResourceLocation texture = this.backgroundTexture(layout);
         if (((MachineMenu)this.menu).kind() == MachineKind.SMALL_MILKING_MACHINE) {
            RedesignedMachineArtwork.milking(graphics, (MachineMenu)this.menu, x, y);
            Entity selected = this.selectedMilkingAnimal();
            if (selected instanceof LivingEntity living) {
               InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                  x + 27, y + 34, x + 74, y + 75, 19, 0.0F,
                  x + 50.0F, y + 54.0F, living);
            }
            return;
         }
         if (((MachineMenu)this.menu).kind() == MachineKind.SMALL_BLOCK_BREAKER) {
            RedesignedMachineArtwork.breaker(graphics, (MachineMenu)this.menu, x, y);
            return;
         }
         this.drawOriginalTextureBackdrop(graphics, x, y);
            MachineKind kind = ((MachineMenu)this.menu).kind();
            if (kind == MachineKind.SMALL_FREEZER) {
                this.drawSmallFreezerTexture(graphics, texture, x, y);
            } else if (usesExplicitTextureDimensions(kind)) {
                graphics.blit(texture, x, y, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
            } else if (kind == MachineKind.SMALL_FLUID_FUNNEL
                    || kind == MachineKind.PASSIVE_FLUID_ACCUMULATOR) {
                GuiTextureBlit geometry = kind == MachineKind.SMALL_FLUID_FUNNEL
                    ? GuiTextureBlit.stretchFullTexture(this.imageWidth, this.imageHeight, 385, 439)
                    : new GuiTextureBlit(this.imageWidth, this.imageHeight, 278, 616, 385, 439, 941, 1672);
                graphics.blit(
                    texture,
                    x,
                    y,
                    geometry.destinationWidth(),
                    geometry.destinationHeight(),
                    (float)geometry.sourceX(),
                    (float)geometry.sourceY(),
                    geometry.sourceWidth(),
                    geometry.sourceHeight(),
                    geometry.textureWidth(),
                    geometry.textureHeight()
                );
            } else {
                graphics.blit(texture, x, y, 0, 0, this.imageWidth, this.imageHeight);
            }
            this.drawOriginalWidgets(graphics, texture, x, y);
            if (kind == MachineKind.SMALL_BLOCK_BREAKER) {
                this.drawBlockBreakerTarget(graphics, x, y);
            } else if (kind == MachineKind.SMALL_FLUID_FUNNEL) {
                this.drawSmallFluidFunnelBackgroundOverlays(graphics, texture, x, y);
            } else if (kind == MachineKind.PASSIVE_FLUID_ACCUMULATOR) {
               this.drawPassiveFluidAccumulatorBackgroundOverlays(graphics, texture, x, y);
            } else if (kind == MachineKind.FLUID_BARREL) {
                this.drawFluidBarrelWidgets(graphics, texture, x, y);
         }
      } else {
         this.drawFrame(graphics, x, y);
         this.drawPanels(graphics, layout, x, y);
         this.drawMachineWidget(graphics, layout, x, y);
         this.drawMachineSlots(graphics, layout, x, y);
         this.drawPlayerSlots(graphics, layout, x, y);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return ((MachineMenu)this.menu).layout().style() == MachineLayout.LayoutStyle.ORIGINAL_TEXTURE && this.handleOriginalClick(mouseX, mouseY)
         ? true
         : super.mouseClicked(mouseX, mouseY, button);
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      MachineLayout layout = ((MachineMenu)this.menu).layout();
      if (((MachineMenu)this.menu).kind() == MachineKind.SMALL_MILKING_MACHINE) {
         this.drawSmallMilkingMachineLabels(graphics);
      } else if (((MachineMenu)this.menu).kind() == MachineKind.SMALL_BLOCK_BREAKER) {
         this.drawBlockBreakerLabels(graphics);
       } else if (((MachineMenu)this.menu).kind() == MachineKind.SMALL_FLUID_FUNNEL) {
          this.drawSmallFluidFunnelLabels(graphics);
       } else if (((MachineMenu)this.menu).kind() == MachineKind.PASSIVE_FLUID_ACCUMULATOR) {
          this.drawPassiveFluidAccumulatorLabels(graphics);
       } else if (((MachineMenu)this.menu).kind() == MachineKind.FLUID_BARREL) {
          this.drawFluidBarrelLabels(graphics);
       } else if (((MachineMenu)this.menu).kind() == MachineKind.SMALL_FREEZER) {
          this.drawSmallFreezerLabels(graphics);
      } else if (layout.style() == MachineLayout.LayoutStyle.ARTWORK_TEXTURE) {
         this.drawArtworkLabels(graphics);
      } else if (layout.style() != MachineLayout.LayoutStyle.ORIGINAL_TEXTURE) {
         this.drawBoundedString(graphics, this.profile.title(), this.titleLabelX, this.titleLabelY, this.imageWidth - this.titleLabelX - 6, -1646126, false);
         this.drawTinyString(graphics, "Inv", this.inventoryLabelX, this.inventoryLabelY + 1, 28, -3949142, false);
         this.drawPanelLabels(graphics, layout);
         this.drawStatusText(graphics, layout);
      }
   }

   private void drawSmallFreezerTexture(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      graphics.pose().pushPose();
      graphics.pose().translate((double)x0, (double)y0, 0.0);
      graphics.pose().scale((float)this.imageWidth / 422.0F, (float)this.imageHeight / 443.0F, 1.0F);
      graphics.blit(texture, 0, 0, 259, 614, 422, 443, 941, 1672);
      graphics.pose().popPose();
   }

   private void drawSmallFreezerWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      MachineMenu menu = (MachineMenu)this.menu;
      this.drawRepeatedTexturePatch(graphics, texture, x0 + 17, y0 + 3, 32, 21,
          358, 632, 16, 14, 941, 1672, (float)this.imageWidth / 422.0F);
      this.drawRepeatedTexturePatch(graphics, texture, x0 + 52, y0 + 5, 142, 14,
          620, 629, 26, 8, 941, 1672, (float)this.imageWidth / 422.0F);
      float textureScale = (float)this.imageWidth / 422.0F;
      // Copy nearby wall texture over the baked sample captions; live values are drawn later.
      this.drawRepeatedTexturePatch(graphics, texture, x0 + 19, y0 + 42, 38, 12,
          360, 691, 18, 22, 941, 1672, textureScale);
      this.drawRepeatedTexturePatch(graphics, texture, x0 + 158, y0 + 42, 38, 12,
          360, 691, 18, 22, 941, 1672, textureScale);
      // Sample the plain wall above the divider so the meter separator is not copied behind text.
      this.drawRepeatedTexturePatch(graphics, texture, x0 + 15, y0 + 101, 48, 12,
          570, 810, 18, 22, 941, 1672, textureScale);
      this.drawRepeatedTexturePatch(graphics, texture, x0 + 15, y0 + 118, 48, 13,
          570, 810, 18, 22, 941, 1672, textureScale);
      this.drawRepeatedTexturePatch(graphics, texture, x0 + 149, y0 + 118, 52, 13,
          570, 810, 18, 22, 941, 1672, textureScale);
       // Reuse the clean, unlettered span of the same inventory rail as a single aligned patch.
       this.drawTexturePatch(graphics, texture, x0 + 20, y0 + 136, 46, 9,
          420, 868, 92, 18, 941, 1672);

      this.drawMachineSlotInteriorsFromArtwork(graphics, texture, x0, y0,
         259, 614, 422, 443, 941, 1672, this.imageWidth, this.imageHeight);

      SmallFreezerArtworkMasks.MaskRegion progressTrack =
         SmallFreezerArtworkMasks.progressBar(this.imageWidth, this.imageHeight);
      int progressSegmentWidth = 19;
      int progressSegmentGap = 2;
      int progressPixels = smallFreezerProgressPixels(
         menu.freezerProgressTicks(), MachineBlockEntity.FREEZER_PROCESS_TICKS, 4, progressSegmentWidth
      );
      for (int segment = 0; segment < 4; segment++) {
         int segmentX = x0 + progressTrack.x() + 2 + segment * (progressSegmentWidth + progressSegmentGap);
         int segmentY = y0 + progressTrack.y() + 3;
         graphics.fill(segmentX, segmentY, segmentX + progressSegmentWidth, segmentY + 6, 0xFF14252C);
         int segmentFill = Mth.clamp(progressPixels - segment * progressSegmentWidth, 0, progressSegmentWidth);
         if (segmentFill > 0) {
            graphics.fill(segmentX, segmentY, segmentX + segmentFill, segmentY + 6, 0xFF79C9F3);
         }
      }

      SmallFreezerArtworkMasks.MaskRegion energyTrack =
         SmallFreezerArtworkMasks.energyBar(this.imageWidth, this.imageHeight);
      int energyX = x0 + energyTrack.x() + 2;
      int energyY = y0 + energyTrack.y() + 2;
      int energyWidth = energyTrack.width() - 4;
      int energyHeight = energyTrack.height() - 4;
      int energyPixels = smallFreezerEnergyPixels(menu.energyStored(), menu.energyCapacity(), energyWidth);
      graphics.fill(energyX, energyY, energyX + energyWidth, energyY + energyHeight, 0xFF171B1D);
      if (energyPixels > 0) {
         graphics.fill(energyX, energyY, energyX + energyPixels, energyY + energyHeight, 0xFFFFB33F);
      }

      boolean redstonePowered = menu.freezerRedstonePowered();
      graphics.fill(x0 + 17, y0 + 95, x0 + 23, y0 + 101, smallFreezerRedstoneColor(redstonePowered));
      if (menu.freezerStatus() == MachineMenu.FREEZER_STATUS_OUTPUT_BLOCKED) {
         MachineLayout.MachineSlot output = menu.layout().machineSlots().get(2);
         this.drawHoverOutline(graphics, x0 + output.x() - 1, y0 + output.y() - 1, 18, 18, 0xFFFF6262);
      }
      if (!menu.freezerPreviewStack().isEmpty()) {
         graphics.renderItem(menu.freezerPreviewStack(), x0 + 97, y0 + 63);
      }
   }

   private void drawSmallFreezerLabels(GuiGraphics graphics) {
      MachineMenu menu = (MachineMenu)this.menu;
      this.drawCompactMachineTitle(graphics,
         Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_freezer").getString(),
         123, 10, 138, TEXT);
      this.drawScaledBoundedString(graphics, smallFreezerText("label.input").getString(), 19, 45, 35, TEXT, false, 0.58F);
      this.drawScaledBoundedString(graphics, smallFreezerText("label.output").getString(), 160, 45, 35, TEXT, false, 0.58F);
      this.drawScaledBoundedString(graphics, smallFreezerText("label.cold").getString(), 22, 76, 27, MUTED, false, 0.50F);
      this.drawScaledBoundedString(graphics, smallFreezerText("label.progress").getString(), 16, 103, 45, TEXT, false, 0.58F);
      this.drawScaledBoundedString(graphics, smallFreezerText("label.energy").getString(), 16, 120, 45, TEXT, false, 0.58F);
      this.drawScaledBoundedString(graphics,
         smallFreezerText("energy.value", menu.energyStored(), menu.energyCapacity()).getString(),
         151, 120, 50, TEXT, false, 0.50F);
      this.drawScaledBoundedString(graphics, smallFreezerText("label.inventory").getString(), 22, 137, 100, TEXT, false, 0.58F);

      String status = smallFreezerText("status." + smallFreezerStatusKey(menu.freezerStatus())).getString();
      String redstone = smallFreezerText(menu.freezerRedstonePowered() ? "redstone.high_short" : "redstone.low_short").getString();
      this.drawScaledBoundedString(graphics,
         smallFreezerText("label.runtime_state", status, menu.freezerPhase(), redstone).getString(),
         25, 94, 179, TEXT, false, 0.44F);
   }

   static String smallFreezerStatusKey(int status) {
      return switch (status) {
         case MachineMenu.FREEZER_STATUS_READY -> "ready";
         case MachineMenu.FREEZER_STATUS_PROCESSING -> "processing";
         case MachineMenu.FREEZER_STATUS_REDSTONE_PAUSED -> "redstone_paused";
         case MachineMenu.FREEZER_STATUS_WAITING_FOR_ENERGY -> "waiting_for_energy";
         case MachineMenu.FREEZER_STATUS_OUTPUT_BLOCKED -> "output_blocked";
         case MachineMenu.FREEZER_STATUS_INVALID_INPUT -> "invalid_input";
         default -> "idle";
      };
   }

   private static Component smallFreezerText(String key, Object... arguments) {
      return Component.translatable(SMALL_FREEZER_GUI_PREFIX + key, arguments);
   }

   private void drawArtworkBackgroundOverlays(
      GuiGraphics graphics, MachineLayout layout, ResourceLocation texture, int x, int y
   ) {
      MachineMenu menu = (MachineMenu)this.menu;
      if (menu.kind() == MachineKind.SMALL_SOLAR_PANEL) {
         this.drawRepeatedTexturePatch(graphics, texture, x + 20, y + 4, 30, 21,
             342, 14, 40, 24, 455, 424, (float)this.imageWidth / 455.0F);
         this.drawRepeatedTexturePatch(graphics, texture, x + 62, y + 8, 108, 11,
            342, 14, 40, 24, 455, 424, (float)this.imageWidth / 455.0F);
      } else if (menu.kind() == MachineKind.SMALL_TREE_CUTTER) {
         this.drawRepeatedTexturePatch(graphics, texture, x + 17, y + 4, 33, 21,
            110, 12, 26, 36, 446, 418, (float)this.imageWidth / 446.0F);
      }
      for (MachineLayout.Panel panel : layout.panels()) {
         int left = x + panel.x();
         int top = y + panel.y();
         switch (panel.label()) {
             case "solar-cell-mask" -> {
                GuiTextureBlit artwork = layout.artworkBlit();
                this.drawMachineSlotInteriorsFromArtwork(graphics, texture, x, y,
                   artwork.sourceX(), artwork.sourceY(), artwork.sourceWidth(), artwork.sourceHeight(),
                   artwork.textureWidth(), artwork.textureHeight(), artwork.destinationWidth(), artwork.destinationHeight());
             }
            case "solar-energy-readout-mask" -> this.drawTexturePatch(
               graphics, texture, left, top + 4, panel.width(), 9, 290, 230, 120, 18, 455, 424
            );
            case "tree-collection-mask" -> graphics.fill(left, top, left + panel.width(), top + panel.height(), 0xFF202629);
            case "solar-energy-bar" -> this.drawArtworkEnergyMeter(
               graphics, left, top, panel.width(), panel.height(), menu.energyStored(), menu.energyCapacity()
            );
            case "tree-status-mask" -> graphics.fill(left, top, left + panel.width(), top + panel.height(), 0xFF102015);
            case "inventory-panel" -> {
               if (menu.kind() == MachineKind.SMALL_SOLAR_PANEL) {
                  // Copy the adjacent clean metal strip over the PNG's French label before localizing it.
                  this.drawTexturePatch(graphics, texture, left, top + 1, 44, 10, 132, 254, 88, 20, 455, 424);
               }
            }
            default -> {
            }
         }
      }
   }

   private void drawArtworkEnergyMeter(GuiGraphics graphics, int x, int y, int width, int height, int stored, int capacity) {
      int innerWidth = Math.max(0, width - 4);
      int fill = artworkEnergyPixels(stored, capacity, innerWidth);
      int bakedSampleWidth = solarArtworkBakedFillWidth(width);
      int trackColor = 0xFF17191A;
      // The PNG contains a sample fill along the top and bottom edges of the meter.
      // Clear only that sample's two-pixel band; keep the printed bezel intact.
      graphics.fill(x + 2, y, x + 2 + bakedSampleWidth, y + 2, trackColor);
      graphics.fill(x + 2, y + height - 2, x + 2 + bakedSampleWidth, y + height - 1, trackColor);
      graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF17191A);
      graphics.fill(x + 2, y + 2, x + 2 + fill, y + height - 2, 0xFFF2A432);
      if (fill > 1) {
         graphics.fill(x + 2, y + 2, x + 2 + fill, y + 3, 0xFFFFD06A);
      }
   }

   static int artworkEnergyPixels(int stored, int capacity, int width) {
      if (capacity <= 0 || width <= 0 || stored <= 0) return 0;
      return (int)((long)Math.min(stored, capacity) * width / capacity);
   }

   static int solarArtworkBakedFillWidth(int meterWidth) {
      return Math.max(0, Math.min(70, meterWidth - 4));
   }

   static int smallFreezerRedstoneColor(boolean powered) {
      return powered ? 0xFFFF5656 : 0xFF75B77A;
   }

   private void drawArtworkLabels(GuiGraphics graphics) {
      MachineMenu menu = (MachineMenu)this.menu;
      MachineLayout layout = menu.layout();
      String inventoryLabel = Component.translatable("container.inventory").getString();
      int labelWidth = Math.min(64, Math.max(24, Mth.ceil(this.font.width(inventoryLabel) * 0.72F) + 2));
      if (menu.kind() == MachineKind.SMALL_SOLAR_PANEL) {
         this.drawCompactMachineTitle(graphics,
            Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_solar_panel").getString(),
            114, 10, 110, TEXT);
      }
      this.drawTinyString(
         graphics,
         inventoryLabel,
         layout.playerInventoryX(),
         layout.playerInventoryY() - 10,
         labelWidth,
         TEXT,
         false
      );

      for (MachineLayout.Panel panel : layout.panels()) {
         if (panel.label().equals("solar-energy-readout-mask")) {
            this.drawFittedString(
               graphics,
               menu.energyStored() + " / " + menu.energyCapacity() + " FE",
               panel.x() + 1,
               panel.y() + 5,
               panel.width() - 2,
               TEXT,
               false,
               0.72F
            );
         } else if (panel.label().equals("solar-comparator")) {
            this.drawTinyString(graphics, "Cmp " + menu.comparatorOutput(), panel.x(), panel.y(), panel.width(), MUTED, false);
         } else if (panel.label().equals("tree-comparator")) {
            this.drawTinyCenteredString(
               graphics, "Cmp " + menu.comparatorOutput(), panel.x() + panel.width() / 2, panel.y() + 1, panel.width(), 0xFF74E85C
            );
         }
      }

      for (MachineLayout.MachineSlot slot : layout.machineSlots()) {
         if (slot.role().equals("I/O")) {
            this.drawTinyCenteredString(graphics, "I/O", slot.x() + 9, slot.y() - 9, 28, TEXT);
         }
      }
   }

   private void drawBlockBreakerTarget(GuiGraphics graphics, int x, int y) {
      MachineMenu menu = (MachineMenu)this.menu;
      if (!menu.blockBreakerTargetStack().isEmpty()) {
         graphics.renderItem(menu.blockBreakerTargetStack(), x + 22, y + 46);
      }

      int total = menu.field(3);
      int elapsed = menu.field(1);
      int fill = total > 0 ? Mth.clamp(elapsed * 65 / total, 0, 65) : 0;
      if (fill > 0) {
         graphics.fill(x + 15, y + 100, x + 15 + fill, y + 103, 0xFF7FA77A);
      }
   }

   private void drawBlockBreakerLabels(GuiGraphics graphics) {
      this.drawScaledCenteredString(graphics,
         Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_block_breaker").getString(),
         128, 13, 155, 0xFF24180F, 1.0F);
   }

   static String blockBreakerStatusLabelKey(int state) {
      return switch (state) {
         case 0 -> "target.none";
         case 1 -> "state.redstone_stop";
         case 2 -> "state.waiting_for_fe";
         case 3 -> "state.output_blocked";
         case 4 -> "state.working";
         case 5 -> "state.cooldown";
         case 6 -> "state.unbreakable";
         default -> "state.unknown";
      };
   }

   static boolean blockBreakerHasTarget(String targetName) {
      return !"No target".equals(targetName);
   }

   static int blockBreakerEstimatedCost(float hardness, boolean hasTarget, boolean hasPreviewItem, int menuEstimate) {
      if (!hasTarget || !Float.isFinite(hardness) || hardness < 0.0F) {
         return -1;
      }
      return hasPreviewItem ? menuEstimate : MachineBlockEntity.blockBreakerEnergyCostPerBlock(hardness);
   }

   static String formatBlockBreakerHardness(float hardness, Locale locale) {
      return String.format(locale, "%.2f", hardness);
   }

   private String formatBlockBreakerHardness(float hardness) {
      String selectedLanguage = this.minecraft.getLanguageManager().getSelected();
      Locale locale = Locale.forLanguageTag(selectedLanguage.replace('_', '-'));
      return formatBlockBreakerHardness(hardness, locale);
   }

   static String blockBreakerStatusDetailKey(int state) {
      return switch (state) {
         case 0 -> "detail.no_target";
         case 1 -> "detail.redstone_stop";
         case 2 -> "detail.waiting_for_fe";
         case 3 -> "detail.output_blocked";
         case 4 -> "detail.working";
         case 5 -> "detail.cooldown";
         case 6 -> "detail.unbreakable";
         default -> "detail.unknown";
      };
   }

   static String blockBreakerHardnessKey(float hardness, boolean hasTarget) {
      if (!hasTarget) {
         return "hardness.unavailable";
      }
      if (!Float.isFinite(hardness)) {
         return "hardness.unknown";
      }
      return hardness < 0.0F ? "hardness.unbreakable" : "hardness.value";
   }

   private static Component blockBreakerText(String key, Object... arguments) {
      return Component.translatable(BLOCK_BREAKER_GUI_PREFIX + key, arguments);
   }

   private Component blockBreakerTooltip(int mouseX, int mouseY) {
      MachineMenu menu = (MachineMenu)this.menu;
      if (menu.kind() != MachineKind.SMALL_BLOCK_BREAKER) return null;
      String targetName = menu.blockBreakerTargetName();
      boolean hasTarget = blockBreakerHasTarget(targetName);
      float hardness = menu.blockBreakerHardness();
      String hardnessKey = blockBreakerHardnessKey(hardness, hasTarget);
      Component hardnessText = hardnessKey.equals("hardness.value")
         ? blockBreakerText(hardnessKey, this.formatBlockBreakerHardness(hardness))
         : blockBreakerText(hardnessKey);
      int cost = blockBreakerEstimatedCost(hardness, hasTarget,
         !menu.blockBreakerTargetStack().isEmpty(), menu.blockBreakerEnergyCostPerBlock());
      if (this.isHovering(191, 49, 46, 12, mouseX, mouseY))
         return blockBreakerText("tooltip.energy", menu.energyStored(), menu.energyCapacity()).copy()
            .append("\n").append(blockBreakerText("tooltip.input"));
      if (this.isHovering(191, 71, 46, 12, mouseX, mouseY))
         return blockBreakerText("tooltip.fluid_unavailable");
      if (this.isHovering(34, 106, 22, 23, mouseX, mouseY))
         return blockBreakerText("tooltip.power_unavailable").copy().append("\n")
            .append(blockBreakerText("redstone.signal", blockBreakerText(menu.blockBreakerRedstonePowered() ? "signal.high" : "signal.low")))
            .append("\n").append(blockBreakerText("tooltip.redstone"));
      if (this.isHovering(193, 88, 41, 47, mouseX, mouseY))
         return blockBreakerText("tooltip.rear_output").copy().append("\n")
            .append(blockBreakerText("tooltip.legacy_pending", menu.blockBreakerPendingItems()));
      if (this.isHovering(26, 51, 39, 45, mouseX, mouseY))
         return blockBreakerText("tooltip.target", hasTarget ? targetName : blockBreakerText("target.none").getString()).copy().append("\n")
            .append(blockBreakerText("tooltip.hardness", hardnessText))
            .append("\n").append(blockBreakerText("tooltip.front_face"));
      if (this.isHovering(100, 85, 40, 41, mouseX, mouseY))
         return blockBreakerText("tooltip.static_scene");
      if (this.isHovering(65, 132, 132, 18, mouseX, mouseY))
         return blockBreakerText("tooltip.work_details", Math.max(0, menu.field(1)), Math.max(0, menu.field(3)),
            cost >= 0 ? blockBreakerText("cost.per_block", cost) : blockBreakerText("cost.unavailable"),
            blockBreakerText(blockBreakerStatusLabelKey(menu.blockBreakerStatus())),
            blockBreakerText(blockBreakerStatusDetailKey(menu.blockBreakerStatus())));
      return null;
   }

   private void renderOriginalHoverOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
      if (((MachineMenu)this.menu).layout().style() == MachineLayout.LayoutStyle.ORIGINAL_TEXTURE) {
         MachineLayout.MachineSlot slot = this.hoveredMachineSlot(mouseX, mouseY);
         if (slot != null) {
            this.drawHoverOutline(graphics, this.leftPos + slot.x() - 1, this.topPos + slot.y() - 1, 18, 18, -1917873);
         }

         if (((MachineMenu)this.menu).kind() == MachineKind.FACTORY_PLACER) {
            this.drawFactoryPlacerControlHover(graphics, mouseX, mouseY);
         }
      }
   }

   private MachineLayout.MachineSlot hoveredMachineSlot(int mouseX, int mouseY) {
      for (MachineLayout.MachineSlot slot : ((MachineMenu)this.menu).layout().machineSlots()) {
         if (this.isHovering(slot.x(), slot.y(), 16, 16, mouseX, mouseY)) {
            return slot;
         }
      }

      return null;
   }

   private void drawBoundedString(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color, boolean shadow) {
      if (maxWidth > 0 && !text.isEmpty()) {
         graphics.drawString(this.font, this.fitText(text, maxWidth), x, y, color, shadow);
      }
   }

   private void drawTinyString(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color, boolean shadow) {
      this.drawScaledBoundedString(graphics, text, x, y, maxWidth, color, shadow, 0.72F);
   }

   private void drawTinyCenteredString(GuiGraphics graphics, String text, int centerX, int y, int maxWidth, int color) {
      String fitted = this.fitText(text, Math.max(1, (int)(maxWidth / 0.72F)));
      int scaledWidth = Mth.ceil(this.font.width(fitted) * 0.72F);
      this.drawTinyString(graphics, fitted, centerX - scaledWidth / 2, y, maxWidth, color, false);
   }

   private void drawCompactMachineTitle(GuiGraphics graphics, String text, int centerX, int y, int maxWidth, int color) {
      String normalized = text.trim();
      float preferredScale = 0.82F;
      int maximumFontWidth = Math.max(1, Mth.floor(maxWidth / preferredScale));
      if (this.font.width(normalized) <= maximumFontWidth) {
         this.drawScaledCenteredString(graphics, normalized, centerX, y, maxWidth, color, preferredScale);
         return;
      }

      String[] words = normalized.split("\\s+");
      int bestSplit = -1;
      int bestLineWidth = Integer.MAX_VALUE;
      for (int split = 1; split < words.length; split++) {
         String first = joinWords(words, 0, split);
         String second = joinWords(words, split, words.length);
         int firstWidth = this.font.width(first);
         int secondWidth = this.font.width(second);
         int widest = Math.max(firstWidth, secondWidth);
         if (firstWidth <= maximumFontWidth && secondWidth <= maximumFontWidth && widest < bestLineWidth) {
            bestSplit = split;
            bestLineWidth = widest;
         }
      }

      if (bestSplit >= 0) {
         this.drawScaledCenteredString(graphics, joinWords(words, 0, bestSplit), centerX, y - 3, maxWidth, color, preferredScale);
         this.drawScaledCenteredString(graphics, joinWords(words, bestSplit, words.length), centerX, y + 4, maxWidth, color, preferredScale);
      } else {
         this.drawScaledCenteredString(graphics, normalized, centerX, y, maxWidth, color, preferredScale);
      }
   }

   private static String joinWords(String[] words, int start, int end) {
      StringBuilder result = new StringBuilder();
      for (int index = start; index < end; index++) {
         if (result.length() > 0) result.append(' ');
         result.append(words[index]);
      }
      return result.toString();
   }

   private void drawScaledCenteredString(GuiGraphics graphics, String text, int centerX, int y, int maxWidth,
                                         int color, float preferredScale) {
      int fontWidth = this.font.width(text);
      if (fontWidth <= 0 || maxWidth <= 0 || preferredScale <= 0.0F) return;
      float scale = Math.min(preferredScale, (float)maxWidth / fontWidth);
      int scaledWidth = Mth.ceil(fontWidth * scale);
      graphics.pose().pushPose();
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, text, Mth.floor((centerX - scaledWidth / 2.0F) / scale),
         Mth.floor(y / scale), color, false);
      graphics.pose().popPose();
   }

   private void drawScaledBoundedString(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color, boolean shadow, float scale) {
      if (maxWidth > 0 && !text.isEmpty()) {
         String fitted = this.fitText(text, Math.max(1, (int)(maxWidth / scale)));
         graphics.pose().pushPose();
         graphics.pose().scale(scale, scale, 1.0F);
         graphics.drawString(this.font, fitted, Mth.floor(x / scale), Mth.floor(y / scale), color, shadow);
         graphics.pose().popPose();
      }
   }

   private String fitText(String text, int maxWidth) {
      if (this.font.width(text) <= maxWidth) {
         return text;
      }

      String ellipsis = "...";
      int textWidth = maxWidth - this.font.width(ellipsis);
      return textWidth <= 0 ? "" : this.font.plainSubstrByWidth(text, textWidth) + ellipsis;
   }

   private void drawFittedString(GuiGraphics graphics, String text, int x, int y, int maxWidth,
                                 int color, boolean shadow, float preferredScale) {
      float scale = fittedTextScale(this.font.width(text), maxWidth, preferredScale);
      if (scale <= 0.0F) {
         return;
      }

      graphics.pose().pushPose();
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, text, Mth.floor(x / scale), Mth.floor(y / scale), color, shadow);
      graphics.pose().popPose();
   }

   static float fittedTextScale(int textWidth, int maxWidth, float preferredScale) {
      if (textWidth <= 0 || maxWidth <= 0 || preferredScale <= 0.0F) {
         return 0.0F;
      }
      return Math.min(preferredScale, Math.nextDown(maxWidth / (float)textWidth));
   }

   private void drawHoverOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
      graphics.fill(x, y, x + width, y + 1, color);
      graphics.fill(x, y + height - 1, x + width, y + height, color);
      graphics.fill(x, y, x + 1, y + height, color);
      graphics.fill(x + width - 1, y, x + width, y + height, color);
   }

   private void drawOriginalWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      switch (((MachineMenu)this.menu).kind()) {
         case FACTORY_HOPPER:
            this.drawFactoryHopperWidgets(graphics, texture, x0, y0);
            break;
         case FACTORY_DROPPER:
            this.drawFactoryDropperWidgets(graphics, texture, x0, y0);
            break;
         case FACTORY_PLACER:
            this.drawFactoryPlacerWidgets(graphics, texture, x0, y0);
            break;
         case SMALL_LAB_FURNACE:
            this.drawLabFurnaceWidgets(graphics, texture, x0, y0);
            break;
         case SMALL_ELECTRICAL_FURNACE:
            this.drawElectricalFurnaceWidgets(graphics, texture, x0, y0);
            break;
         case SMALL_FREEZER:
            this.drawSmallFreezerWidgets(graphics, texture, x0, y0);
      }
   }

   private void drawOriginalTextureBackdrop(GuiGraphics graphics, int x0, int y0) {
      if (((MachineMenu)this.menu).kind() == MachineKind.FACTORY_PLACER) {
         int x = x0 + 126;
         int y = y0;
         int width = 50;
         int height = 43;
         graphics.fill(x, y, x + width, y + height, -14671326);
         graphics.fill(x, y, x + width, y + 1, -10723232);
         graphics.fill(x, y, x + 1, y + height, -12170678);
         graphics.fill(x, y + height - 1, x + width, y + height, -15592685);
         graphics.fill(x + width - 1, y, x + width, y + height, -15592685);
      }
   }

   private void drawFactoryHopperWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      int slotIndex = Mth.clamp(((MachineMenu)this.menu).field(6), 0, 17);
      graphics.blit(texture, x0 + 10 + slotIndex % 6 * 18, y0 + 8 + slotIndex / 6 * 17, 200, 8, 18, 18);
      int[] rangeMarkers = new int[]{133, 141, 149, 157, 166};
      graphics.blit(texture, x0 + rangeMarkers[Mth.clamp(((MachineMenu)this.menu).field(0), 0, 4)] - 2, y0 + 14, 179, 40, 5, 5);
      int periodX = Mth.clamp((int)Math.round(33.5 * ((MachineMenu)this.menu).field(3) / 100.0 + 1.0), 0, 34);
      graphics.blit(texture, x0 + 132 - 2 + periodX, y0 + 27, 179, 40, 5, 5);
      graphics.blit(texture, x0 + 133 - 2 + Mth.clamp(((MachineMenu)this.menu).field(1), 1, 32), y0 + 40, 179, 40, 5, 5);
      if (((MachineMenu)this.menu).field(5) != 0) {
         graphics.blit(texture, x0 + 133, y0 + 49, 217, 49, 9, 9);
      }

      int logic = ((MachineMenu)this.menu).field(2);
      int inverterOffsetX = (logic & 1) != 0 ? 11 : 0;
      int inverterOffsetY = (logic & 4) != 0 ? 10 : 0;
      graphics.blit(texture, x0 + 145, y0 + 49, 177 + inverterOffsetX, 49 + inverterOffsetY, 9, 9);
      int pulseOffset = (logic & 2) != 0 ? 9 : 0;
      graphics.blit(texture, x0 + 159, y0 + 49, 199 + pulseOffset, 49, 9, 9);
      if (((MachineMenu)this.menu).field(4) > 10 && System.currentTimeMillis() % 1000L < 500L) {
         graphics.blit(texture, x0 + 148, y0 + 22, 187, 22, 3, 3);
      }
   }

   private void drawFactoryDropperWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      int slotIndex = Mth.clamp(((MachineMenu)this.menu).field(15), 0, 11);
      graphics.blit(texture, x0 + 9 + slotIndex % 6 * 18, y0 + 5 + slotIndex / 6 * 17, 180, 45, 18, 18);

      for (int i = 0; i < 3; i++) {
         graphics.blit(texture, x0 + 31 + i * 36, y0 + 65, 180 + 6 * Mth.clamp(((MachineMenu)this.menu).field(12 + i), 0, 2), 38, 6, 6);
      }

      int height = 2 + (100 - Mth.clamp(((MachineMenu)this.menu).field(0), 0, 100)) * 21 / 100;
      graphics.blit(texture, x0 + 135, y0 + 12, 181, 4 + (23 - height), 3, height);
      graphics.blit(
         texture,
         x0 + 157 - 3 + Mth.clamp(((MachineMenu)this.menu).field(1), -100, 100) * 12 / 100,
         y0 + 22 - 3 - Mth.clamp(((MachineMenu)this.menu).field(2), -100, 100) * 12 / 100,
         180,
         30,
         7,
         7
      );
      graphics.blit(texture, x0 + 134 - 2 + Mth.clamp(((MachineMenu)this.menu).field(4), 1, 32), y0 + 45, 190, 31, 5, 5);
      int periodX = (int)Math.round(33.0 * ((MachineMenu)this.menu).field(6) / 100.0 + 1.0);
      graphics.blit(texture, x0 + 134 - 2 + Mth.clamp(periodX, 0, 33), y0 + 56, 190, 31, 5, 5);
      if (((MachineMenu)this.menu).field(11) != 0) {
         graphics.blit(texture, x0 + 114, y0 + 51, 189, 18, 9, 9);
      }

      int logic = ((MachineMenu)this.menu).field(5);
      graphics.blit(texture, x0 + 132, y0 + 66, 179 + ((logic & 1) != 0 ? 11 : 0), 66, 9, 9);
      graphics.blit(texture, x0 + 148, y0 + 66, 179 + ((logic & 2) != 0 ? 11 : 0), 66 + ((logic & 32) != 0 ? 10 : 0), 9, 9);
      graphics.blit(texture, x0 + 162, y0 + 66, 200 + ((logic & 16) != 0 ? 10 : 0), 66, 9, 9);
      if (((MachineMenu)this.menu).field(9) > 10 && System.currentTimeMillis() % 1000L < 500L) {
         graphics.blit(texture, x0 + 149, y0 + 51, 201, 39, 3, 3);
      }
   }

   private void drawFactoryPlacerWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      int slotIndex = Mth.clamp(((MachineMenu)this.menu).field(2), 0, 17);
      graphics.blit(texture, x0 + 10 + slotIndex % 6 * 18, y0 + 8 + slotIndex / 6 * 17, 200, 8, 18, 18);
      if (((MachineMenu)this.menu).field(1) != 0) {
         graphics.blit(texture, x0 + 133, y0 + 49, 217, 49, 9, 9);
      }

      int logic = ((MachineMenu)this.menu).field(0);
      int inverterOffsetX = (logic & 1) != 0 ? 11 : 0;
      int inverterOffsetY = (logic & 4) != 0 ? 10 : 0;
      graphics.blit(texture, x0 + 145, y0 + 49, 177 + inverterOffsetX, 49 + inverterOffsetY, 9, 9);
      graphics.blit(texture, x0 + 159, y0 + 49, 199 + ((logic & 2) != 0 ? 9 : 0), 49, 9, 9);
   }

   private void drawLabFurnaceWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      if (((MachineMenu)this.menu).field(4) != 0) {
         int flame = this.fuelPixels(13, ((MachineMenu)this.menu).field(0), ((MachineMenu)this.menu).field(1));
         graphics.blit(texture, x0 + 61, y0 + 36 + 13 - flame, 176, 13 - flame, 14, flame);
      }

      graphics.blit(texture, x0 + 79, y0 + 38, 176, 15, 1 + this.progressPixels(17), 15);
   }

   private void drawElectricalFurnaceWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      if (((MachineMenu)this.menu).field(6) != 0) {
         int heat = Math.min(13, ((MachineMenu)this.menu).field(0) * 14 / 200);
         graphics.blit(texture, x0 + 62, y0 + 55 + 13 - heat, 177, 13 - heat, 13, heat);
      }

      graphics.blit(texture, x0 + 79, y0 + 30, 176, 15, 1 + this.progressPixels(17), 15);
      int energyMax = Math.max(((MachineMenu)this.menu).field(5), 1);
      int width = 32 * ((MachineMenu)this.menu).field(1) * 9 / 8 / (energyMax + 1);
      width = width >= 30 ? 32 : Mth.clamp((width + 4) / 8 * 8, 0, 32);
      if (width > 0) {
         graphics.blit(texture, x0 + 90, y0 + 54, 185, 30, width, 13);
      }

      switch (((MachineMenu)this.menu).field(4)) {
         case 0:
            graphics.blit(texture, x0 + 144, y0 + 57, 180, 57, 6, 9);
            break;
         case 1:
            graphics.blit(texture, x0 + 142, y0 + 58, 190, 58, 9, 6);
            break;
         case 2:
            graphics.blit(texture, x0 + 144, y0 + 56, 200, 57, 6, 9);
            break;
         case 3:
            graphics.blit(texture, x0 + 143, y0 + 58, 210, 58, 9, 6);
      }
   }

   private int progressPixels(int pixels) {
      int total = ((MachineMenu)this.menu).field(3);
      int elapsed = ((MachineMenu)this.menu).field(2);
      return total > 0 && elapsed > 0 ? Mth.clamp(elapsed * pixels / total, 0, pixels) : 0;
   }

   private int fuelPixels(int pixels, int burnTime, int fuelTime) {
      return fuelTime > 0 && burnTime > 0 ? Mth.clamp(burnTime * pixels / fuelTime, 0, pixels) : 0;
   }

   private void drawFrame(GuiGraphics graphics, int x, int y) {
      graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, -14934755);
      graphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, -10328475);
      graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, -13618380);
      graphics.fill(x + 4, y + 4, x + this.imageWidth - 4, y + this.imageHeight - 4, -12960450);
      graphics.fill(x + 5, y + 5, x + this.imageWidth - 5, y + 17, -13618380);
      graphics.fill(x + 5, y + 17, x + this.imageWidth - 5, y + 18, -14934755);
      graphics.fill(x + 3, y + this.imageHeight - 83, x + this.imageWidth - 3, y + this.imageHeight - 81, -14934755);
      this.drawCornerRivets(graphics, x, y);
   }

   private void drawPanels(GuiGraphics graphics, MachineLayout layout, int x, int y) {
      for (MachineLayout.Panel panel : layout.panels()) {
         graphics.fill(x + panel.x(), y + panel.y(), x + panel.x() + panel.width(), y + panel.y() + panel.height(), -14934755);
         graphics.fill(x + panel.x() + 1, y + panel.y() + 1, x + panel.x() + panel.width() - 1, y + panel.y() + panel.height() - 1, -10328475);
         graphics.fill(x + panel.x() + 2, y + panel.y() + 2, x + panel.x() + panel.width() - 2, y + panel.y() + panel.height() - 2, -13881552);
         graphics.fill(x + panel.x() + 2, y + panel.y() + 2, x + panel.x() + panel.width() - 2, y + panel.y() + 3, -8946823);
         graphics.fill(x + panel.x() + 1, y + panel.y() + 1, x + panel.x() + panel.width() - 1, y + panel.y() + 3, this.profile.accent());
      }
   }

   private void drawPanelLabels(GuiGraphics graphics, MachineLayout layout) {
      for (MachineLayout.Panel panel : layout.panels()) {
         int labelY = panel.y() - 8;
         if (labelY >= 20) {
            int labelX = panel.x() + 2;
            int labelWidth = Math.max(12, panel.width() - 4);
            this.drawTinyString(graphics, panel.label(), labelX, labelY, labelWidth, -3949142, false);
         }
      }
   }

   private void drawCornerRivets(GuiGraphics graphics, int x, int y) {
      int right = x + this.imageWidth - 8;
      int bottom = y + this.imageHeight - 8;
      this.drawRivet(graphics, x + 7, y + 7);
      this.drawRivet(graphics, right, y + 7);
      this.drawRivet(graphics, x + 7, bottom);
      this.drawRivet(graphics, right, bottom);
   }

   private void drawRivet(GuiGraphics graphics, int x, int y) {
      graphics.fill(x, y, x + 3, y + 3, -15461099);
      graphics.fill(x + 1, y, x + 2, y + 1, -8946823);
      graphics.fill(x + 1, y + 1, x + 2, y + 2, -7697016);
   }

   private void drawPassiveFluidAccumulatorBackgroundOverlays(
      GuiGraphics graphics, ResourceLocation texture, int x0, int y0
   ) {
      MachineMenu menu = (MachineMenu)this.menu;
      String prefix = "gui.immersive_engineer_decor_controls_tool_reforged.passive_fluid_accumulator.";
      this.drawRepeatedTexturePatch(graphics, texture, x0 + 12, y0 + 4, 33, 20,
          318, 754, 22, 14, 941, 1672, (float)this.imageWidth / 385.0F);
       this.drawTexturePatch(graphics, texture, x0 + 55, y0 + 5, 143, 17,
          318, 754, 80, 14, 941, 1672);
      // Mask baked French rows at their printed PNG positions with clean panel texture, then draw live labels.
      this.maskAccumulatorLabel(graphics, texture, x0, y0,
         Component.translatable(prefix + "collection").getString(), 40, 66, 60, 48, 304, 94);
      this.maskAccumulatorLabel(graphics, texture, x0, y0,
         Component.translatable(prefix + "bucket.input").getString(), 38, 86, 53, 36, 304, 94);
      this.maskAccumulatorLabel(graphics, texture, x0, y0,
         Component.translatable(prefix + "bucket.output").getString(), 157, 86, 59, 43, 529, 90);
      this.maskAccumulatorLabel(graphics, texture, x0, y0,
         Component.translatable(prefix + "water").getString(), 146, 47, 63, 15, 529, 90);
      this.maskAccumulatorLabel(graphics, texture, x0, y0,
         Component.translatable(prefix + "amount", menu.fluidAmount(), menu.fluidCapacity()).getString(), 167, 57, 67, 56, 529, 90);
      this.drawTexturePatch(graphics, texture, x0 + 20, y0 + 141, 48, 9, 416, 876, 88, 18, 941, 1672);

      this.drawMachineSlotInteriorsFromArtwork(graphics, texture, x0, y0,
         278, 616, 385, 439, 941, 1672, this.imageWidth, this.imageHeight);

      int tankLeft = x0 + 80;
      int tankTop = y0 + 63;
      int tankRight = x0 + 122;
      int tankBottom = y0 + 114;
      graphics.fill(tankLeft, tankTop, tankRight, tankBottom, 0xFF13212B);

      int availableHeight = tankBottom - tankTop - 2;
      int fill = PassiveFluidAccumulatorGauge.filledHeight(menu.fluidAmount(), menu.fluidCapacity(), availableHeight);
      if (fill > 0) {
         int fluidTop = tankBottom - 1 - fill;
         graphics.fill(tankLeft + 1, fluidTop, tankRight - 1, tankBottom - 1, 0xFF218CCB);
         graphics.fill(tankLeft + 2, fluidTop, tankRight - 2, fluidTop + 1, 0xFF65C7EF);
      }

   }

   private void drawPassiveFluidAccumulatorLabels(GuiGraphics graphics) {
      MachineMenu menu = (MachineMenu)this.menu;
      String prefix = "gui.immersive_engineer_decor_controls_tool_reforged.passive_fluid_accumulator.";
      this.drawCompactMachineTitle(graphics,
         Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.passive_fluid_accumulator").getString(),
         124, 8, 143, TEXT);
      this.drawTinyCenteredString(graphics, Component.translatable(prefix + "collection").getString(), 40, 66, 60, TEXT);
      this.drawTinyCenteredString(graphics, Component.translatable(prefix + "bucket.input").getString(), 38, 86, 53, TEXT);
      this.drawTinyCenteredString(graphics, Component.translatable(prefix + "bucket.output").getString(), 157, 86, 59, TEXT);
      this.drawTinyCenteredString(graphics, Component.translatable(prefix + "water").getString(), 146, 47, 63, TEXT);
      this.drawTinyCenteredString(
         graphics,
         Component.translatable(prefix + "amount", menu.fluidAmount(), menu.fluidCapacity()).getString(),
         167,
         57,
         67,
         TEXT
      );
      this.drawTinyString(graphics, Component.translatable(prefix + "inventory").getString(), 21, 143, 58, TEXT, false);
   }

   private Component passiveFluidAccumulatorTooltip(double mouseX, double mouseY) {
      if (((MachineMenu)this.menu).kind() != MachineKind.PASSIVE_FLUID_ACCUMULATOR) {
         return null;
      }
      if (this.isHovering(14, 68, 60, 15, mouseX, mouseY)) {
         return Component.translatable(
            "gui.immersive_engineer_decor_controls_tool_reforged.passive_fluid_accumulator.collection.tooltip"
         );
      }
      if (!this.isHovering(78, 61, 46, 54, mouseX, mouseY)) {
         return null;
      }

      MachineMenu menu = (MachineMenu)this.menu;
      return Component.translatable(
         "gui.immersive_engineer_decor_controls_tool_reforged.passive_fluid_accumulator.tank.tooltip",
         menu.fluidAmount(),
         menu.fluidCapacity()
      );
   }

   private void drawMachineWidget(GuiGraphics graphics, MachineLayout layout, int x, int y) {
      switch (layout.style()) {
         case STORAGE:
         case ORIGINAL_TEXTURE:
         default:
            break;
         case CRAFTING:
            this.drawCraftingFlow(graphics, x + 119, y + 39);
            this.drawToolGlyph(graphics, x + 28, y + 42);
            break;
         case PROCESSOR:
            this.drawArrow(graphics, x + 75, y + 40, this.workWidth(24));
            this.drawMeter(graphics, x + 78, y + 61, 23, ((MachineMenu)this.menu).progress(), 100, this.profile.accent());
            break;
         case FLUID:
            this.drawTank(graphics, x + 83, y + 21, 12, 50, ((MachineMenu)this.menu).fluidAmount(), ((MachineMenu)this.menu).fluidCapacity());
            this.drawArrow(graphics, x + 63, y + 42, 10);
            this.drawArrow(graphics, x + 103, y + 42, 10);
            break;
         case ENERGY:
            this.drawSolarCells(graphics, x + 42, y + 22);
            this.drawMeter(graphics, x + 62, y + 43, 52, ((MachineMenu)this.menu).energyStored(), ((MachineMenu)this.menu).energyCapacity(), -1917873);
            this.drawSignal(graphics, x + 115, y + 52);
            break;
         case AUTOMATION:
            // The cutter does not sync work progress; its comparator is the available live readout.
            if (((MachineMenu)this.menu).kind() != MachineKind.SMALL_TREE_CUTTER) {
               this.drawArrow(graphics, x + 80, y + 45, this.workWidth(24));
            }
            this.drawSignal(graphics, x + 91, y + 33);
      }
   }

   private void drawStatusText(GuiGraphics graphics, MachineLayout layout) {
      switch (layout.style()) {
         case STORAGE:
            this.drawTinyString(graphics, "Cmp " + ((MachineMenu)this.menu).comparatorOutput(), 138, 77, 32, -3949142, false);
         case ORIGINAL_TEXTURE:
         default:
            break;
         case CRAFTING:
            this.drawTinyCenteredString(graphics, "3x3 Grid", 89, 77, 48, -3949142);
            break;
         case PROCESSOR:
            this.drawTinyString(graphics, this.profile.process(), 76, 22, 56, -3949142, false);
            this.drawTinyString(graphics, "Work " + ((MachineMenu)this.menu).progress(), 116, 77, 54, -3949142, false);
            break;
         case FLUID:
            this.drawTinyCenteredString(graphics, ((MachineMenu)this.menu).fluidAmount() + " mB", 89, 77, 48, -3949142);
            break;
         case ENERGY:
            this.drawTinyString(graphics, "FE " + ((MachineMenu)this.menu).energyStored(), 62, 77, 54, -3949142, false);
            this.drawTinyString(graphics, "C" + ((MachineMenu)this.menu).comparatorOutput(), 134, 77, 24, -3949142, false);
            break;
         case AUTOMATION:
            this.drawTinyString(graphics, this.profile.process(), 83, 20, 56, -3949142, false);
            this.drawTinyString(graphics, "C" + ((MachineMenu)this.menu).comparatorOutput(), 138, 77, 24, -3949142, false);
      }
   }

   private void drawSmallMilkingMachineTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
      graphics.pose().pushPose();
      graphics.pose().translate((double)x, (double)y, 0.0);
      graphics.pose().scale(0.5F, 0.5F, 1.0F);
      graphics.blit(texture, 0, 0, 255, 626, 431, 420, 941, 1672);
      graphics.pose().popPose();
   }

   private void drawSmallMilkingMachineWidgets(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
      MachineMenu menu = (MachineMenu)this.menu;
       this.drawRepeatedTexturePatch(graphics, texture, x + 17, y + 4, 33, 19,
          620, 780, 26, 20, 941, 1672, 0.5F);

      boolean frenchArtwork = this.isFrenchArtworkLocale();
      this.drawRepeatedTexturePatch(graphics, texture, x + 58, y + 8, 128, 13,
          620, 780, 26, 20, 941, 1672, 0.5F);
       // The printed amount is only an example; clear it in both locales before drawing the live value.
       this.drawRepeatedTexturePatch(graphics, texture, x + 145, y + 50, 64, 11,
          620, 780, 26, 20, 941, 1672, 0.5F);
       // Clear only the baked glyph band so the photo caption's lower frame remains visible.
       this.drawRepeatedTexturePatch(graphics, texture, x + 23, y + 78, 71, 9,
          620, 780, 26, 18, 941, 1672, 0.5F);
      if (!frenchArtwork) {
          this.drawRepeatedTexturePatch(graphics, texture, x + 145, y + 40, 64, 21,
             620, 780, 26, 20, 941, 1672, 0.5F);
          this.drawRepeatedTexturePatch(graphics, texture, x + 157, y + 84, 43, 11,
             620, 780, 26, 20, 941, 1672, 0.5F);
          // Sample an empty part of the same inventory band. The old sample crossed the
          // embedded "Inventaire" caption and copied its final glyphs into the mask.
       this.drawTexturePatch(graphics, texture, x + 28, y + 127, 70, 9,
          460, 880, 88, 18, 941, 1672);
       }
      this.drawSmallMilkingMachineTank(graphics, x, y, menu.fluidAmount(), menu.fluidCapacity());
   }

   private void drawSmallMilkingMachineTank(GuiGraphics graphics, int x, int y, int amount, int capacity) {
      int tankX = x + 103;
      int tankY = y + 49;
      int tankWidth = 23;
      int tankHeight = 54;
      int tankBottom = tankY + tankHeight - 2;
      graphics.fill(tankX, tankY, tankX + tankWidth, tankY + tankHeight, 0xFF172124);
      int fill = smallMilkingMachineFillPixels(amount, capacity, tankHeight - 5);
      if (fill > 0) {
         int fillTop = tankBottom - fill;
         graphics.fill(tankX + 3, fillTop, tankX + tankWidth - 3, tankBottom, 0xFFE2D8B8);
         graphics.fill(tankX + 3, fillTop, tankX + tankWidth - 3, fillTop + 1, 0xFFF3E8C9);
         graphics.fill(tankX + 4, tankBottom - 2, tankX + tankWidth - 4, tankBottom, 0xFFC9BEA1);
      }
      graphics.fill(tankX + 1, tankY + 2, tankX + 2, tankY + tankHeight - 2, 0xFF687477);
      graphics.fill(tankX + tankWidth - 2, tankY + 2, tankX + tankWidth - 1, tankY + tankHeight - 2, 0xFF344448);
      graphics.fill(tankX + 4, tankY + 3, tankX + 5, tankY + 14, 0xFF728083);
   }

   private void drawSmallMilkingMachineLabels(GuiGraphics graphics) {
      MachineMenu menu = (MachineMenu)this.menu;
      MilkingState state = menu.milkingState();
      Entity selected = this.selectedMilkingAnimal();
      Component name = selected == null ? smallMilkingText(state.animalId() == null
         ? "label.no_animal" : "label.animal_unavailable") : selected.getDisplayName();
      this.drawMilkingLine(graphics, name.getString(), 114, 6, 180);
      if (state.animalId() != null) this.drawMilkingLine(graphics,
         "#" + state.animalId().toString().substring(0, 8), 114, 16, 180);
      this.drawMilkingLine(graphics, smallMilkingText(milkingStatusKey(state)).getString(), 114, 115, 180);
      // Use two full-pixel lines: shrinking the complete ratio makes individual
      // Minecraft font strokes disappear in the 260-pixel stress-test window.
      String amount = formatSmallMilkingMachineAmount(menu.fluidAmount(), menu.fluidCapacity(), this.smallMilkingLocale());
      String[] parts = amount.split("/", 2);
      this.drawScaledCenteredString(graphics, parts[0].trim() + " mB", 178, 72, 64, TEXT, 1.0F);
      if (parts.length == 2) this.drawScaledCenteredString(graphics, "/ " + parts[1].trim(), 178, 80, 64, TEXT, 1.0F);
   }

   private void maskAccumulatorLabel(
      GuiGraphics graphics, ResourceLocation texture, int x0, int y0, String text,
      int centerX, int y, int maxWidth, int minimumWidth, int patchSourceX, int patchSourceWidth
   ) {
      int textWidth = Math.max(this.tinyTextWidth(text, maxWidth), Math.min(maxWidth, minimumWidth));
      if (textWidth <= 0) return;
      int left = centerX - textWidth / 2 - 1;
      int width = centerX + (textWidth + 1) / 2 + 1 - left;
      this.drawTexturePatch(graphics, texture, x0 + left, y0 + y - 1, width, 9,
         patchSourceX, 754, patchSourceWidth, 18, 941, 1672);
   }

   private void drawTexturePatch(
      GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
      int sourceX, int sourceY, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight
   ) {
      graphics.blit(texture, x, y, width, height, (float)sourceX, (float)sourceY,
         sourceWidth, sourceHeight, textureWidth, textureHeight);
   }

   private void drawRepeatedTexturePatch(
      GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
      int sourceX, int sourceY, int sourceTileWidth, int sourceHeight,
      int textureWidth, int textureHeight, float textureScale
   ) {
      int targetTileWidth = Math.max(1, Math.round(sourceTileWidth * textureScale));
      for (int offset = 0; offset < width; offset += targetTileWidth) {
         int drawWidth = Math.min(targetTileWidth, width - offset);
         int drawSourceWidth = Math.max(1, Math.round(drawWidth / textureScale));
         graphics.blit(texture, x + offset, y, drawWidth, height, (float)sourceX, (float)sourceY,
            drawSourceWidth, sourceHeight, textureWidth, textureHeight);
      }
   }

   private void drawMachineSlotInteriorsFromArtwork(
      GuiGraphics graphics, ResourceLocation texture, int x0, int y0,
      int sourceX, int sourceY, int sourceWidth, int sourceHeight,
      int textureWidth, int textureHeight, int destinationWidth, int destinationHeight
   ) {
      MachineLayout layout = ((MachineMenu)this.menu).layout();
      float scaleX = (float)destinationWidth / sourceWidth;
      float scaleY = (float)destinationHeight / sourceHeight;
      if (scaleX <= 0.0F || scaleY <= 0.0F) return;

      int playerInteriorX = layout.playerSlotX(0) + 1;
      int playerInteriorY = layout.playerSlotY(0) + 1;
      int sampleX = sourceX + Mth.floor(playerInteriorX / scaleX);
      int sampleY = sourceY + Mth.floor(playerInteriorY / scaleY);
      int sampleWidth = Mth.ceil(16.0F / scaleX);
      int sampleHeight = Mth.ceil(16.0F / scaleY);
      if (sampleX < 0 || sampleY < 0 || sampleX + sampleWidth > textureWidth
         || sampleY + sampleHeight > textureHeight) return;

      for (MachineLayout.MachineSlot slot : layout.machineSlots()) {
         this.drawTexturePatch(graphics, texture, x0 + slot.x() + 1, y0 + slot.y() + 1, 16, 16,
            sampleX, sampleY, sampleWidth, sampleHeight, textureWidth, textureHeight);
      }
   }

   private int tinyTextWidth(String text, int maxWidth) {
      if (text.isEmpty() || maxWidth <= 0) return 0;
      String fitted = this.fitText(text, Math.max(1, (int)(maxWidth / 0.72F)));
      return Mth.ceil(this.font.width(fitted) * 0.72F);
   }

   private Component smallMilkingMachineTooltip(int mouseX, int mouseY) {
      if (((MachineMenu)this.menu).kind() != MachineKind.SMALL_MILKING_MACHINE) return null;
      if (this.isHovering(102, 47, 26, 55, mouseX, mouseY)
         || this.isHovering(146, 74, 64, 12, mouseX, mouseY)) {
         MachineMenu menu = (MachineMenu)this.menu;
         return smallMilkingText("tooltip.tank",
            formatSmallMilkingMachineAmount(menu.fluidAmount(), menu.fluidCapacity(), this.smallMilkingLocale()));
      }
      if (this.isHovering(24, 5, 185, 20, mouseX, mouseY)
         || this.isHovering(27, 34, 47, 41, mouseX, mouseY))
         return this.milkingIdentityTooltip();
      if (this.isHovering(24, 112, 185, 12, mouseX, mouseY)
         || this.isHovering(160, 56, 47, 17, mouseX, mouseY))
         return this.milkingRecoveryTooltip();
      if (this.isHovering(160, 41, 47, 14, mouseX, mouseY))
         return smallMilkingText("tooltip.energy_unavailable");
      return null;
   }

   // The server is the only selector. Never infer an animal from proximity or type.
   private Entity selectedMilkingAnimal() {
      MilkingState state = this.menu.milkingState();
      if (this.minecraft == null || this.minecraft.level == null || state.animalId() == null) return null;
      Entity entity = this.minecraft.level.getEntity(state.entityId());
      return entity != null && milkingIdentityMatches(state, entity.getId(), entity.getUUID()) ? entity : null;
   }

   static boolean milkingIdentityMatches(MilkingState state, int entityId, UUID animalId) {
      return state.animalId() != null && state.entityId() == entityId && state.animalId().equals(animalId);
   }

   static String milkingStatusKey(MilkingState state) {
      if (state.animalId() == null) return "label.no_animal";
      if (state.source() == MilkingState.TFC && state.state() == MilkingState.RECOVERING) return "label.recovering";
      if (state.state() == MilkingState.INELIGIBLE) return "label.ineligible";
      if ((state.machineFlags() & MilkingState.PAUSED) != 0) return "label.paused";
      if ((state.machineFlags() & MilkingState.STORAGE_BLOCKED) != 0) return "label.storage_blocked";
      if ((state.machineFlags() & MilkingState.RETRY_WAIT) != 0) return "label.retry_wait";
      if (state.source() == MilkingState.VANILLA) return "label.vanilla";
      if (state.source() == MilkingState.TFC && state.state() == MilkingState.READY) return "label.ready";
      return "label.unavailable";
   }

   private void drawMilkingLine(GuiGraphics graphics, String text, int center, int y, int maxWidth) {
      // Full pixel text, ellipsized instead of shrinking strokes in a small window.
      String shown = this.font.width(text) <= maxWidth ? text
         : this.font.plainSubstrByWidth(text, maxWidth - this.font.width("…")) + "…";
      graphics.drawString(this.font, shown, center - this.font.width(shown) / 2, y, TEXT, false);
   }

   private Component milkingIdentityTooltip() {
      MilkingState state = this.menu.milkingState();
      if (state.animalId() == null) return smallMilkingText("label.no_animal");
      Entity selected = this.selectedMilkingAnimal();
      return smallMilkingText("tooltip.animal", selected == null
         ? smallMilkingText("label.animal_unavailable") : selected.getDisplayName(), state.animalId().toString());
   }

   static int milkingTooltipWidth(int viewportWidth) {
      // Leave space for the native tooltip border and screen-edge padding.
      return Math.max(1, Math.min(240, Math.max(0, viewportWidth) - 16));
   }

   private Component milkingRecoveryTooltip() {
      MilkingState state = this.menu.milkingState();
      var text = smallMilkingText(milkingStatusKey(state)).copy();
      if (state.source() == MilkingState.TFC && state.animalId() != null
         && (state.state() == MilkingState.RECOVERING || state.state() == MilkingState.READY)) {
         text.append("\n").append(smallMilkingText("tooltip.native_ticks",
            NumberFormat.getIntegerInstance(this.smallMilkingLocale()).format(state.remainingTicks())));
      } else if (state.source() == MilkingState.VANILLA) {
         text.append("\n").append(smallMilkingText("tooltip.vanilla"));
      }
      if ((state.machineFlags() & MilkingState.PAUSED) != 0) text.append("\n").append(smallMilkingText("label.paused"));
      if ((state.machineFlags() & MilkingState.STORAGE_BLOCKED) != 0) text.append("\n").append(smallMilkingText("label.storage_blocked"));
      if ((state.machineFlags() & MilkingState.RETRY_WAIT) != 0) text.append("\n").append(smallMilkingText("label.retry_wait"));
      return text;
   }

   private Locale smallMilkingLocale() {
      String selected = this.minecraft == null ? "en_us" : this.minecraft.getLanguageManager().getSelected();
      return Locale.forLanguageTag(selected.replace('_', '-'));
   }

   private boolean isFrenchArtworkLocale() {
      String selected = this.minecraft == null ? "en_us" : this.minecraft.getLanguageManager().getSelected();
      return selected.toLowerCase(Locale.ROOT).startsWith("fr");
   }

   private static Component smallMilkingText(String key, Object... arguments) {
      return Component.translatable(MILKING_GUI_PREFIX + key, arguments);
   }

   static int smallMilkingMachineFillPixels(int amount, int capacity, int height) {
      if (capacity <= 0 || height <= 0) {
         return 0;
      }
      return Mth.clamp(amount, 0, capacity) * height / capacity;
   }

   static int smallFreezerProgressPixels(int ticks, int maximumTicks, int segmentCount, int segmentWidth) {
      if (maximumTicks <= 0 || segmentCount <= 0 || segmentWidth <= 0 || ticks <= 0) return 0;
      return (int)((long)Math.min(ticks, maximumTicks) * segmentCount * segmentWidth / maximumTicks);
   }

   static int smallFreezerEnergyPixels(int stored, int capacity, int width) {
      if (capacity <= 0 || width <= 0 || stored <= 0) return 0;
      return (int)((long)Math.min(stored, capacity) * width / capacity);
   }

   static String formatSmallMilkingMachineAmount(int amount, int capacity, Locale locale) {
      int safeCapacity = Math.max(0, capacity);
      int safeAmount = Mth.clamp(amount, 0, safeCapacity);
      NumberFormat format = NumberFormat.getIntegerInstance(locale);
      return format.format(safeAmount) + " / " + format.format(safeCapacity) + " mB";
   }

   private ResourceLocation backgroundTexture(MachineLayout layout) {
      return ResourceLocation.fromNamespaceAndPath("immersive_engineer_decor_controls_tool_reforged", "textures/gui/" + layout.textureName() + ".png");
   }

   private void drawCraftingFlow(GuiGraphics graphics, int x, int y) {
      graphics.fill(x, y + 3, x + 10, y + 7, -14934755);
      graphics.fill(x + 1, y + 4, x + 7, y + 6, -10328475);
      graphics.fill(x + 7, y + 2, x + 10, y + 8, -10328475);
   }

   private void drawToolGlyph(GuiGraphics graphics, int x, int y) {
      graphics.fill(x, y, x + 3, y + 9, -9149877);
      graphics.fill(x + 3, y + 1, x + 8, y + 4, -10328475);
      graphics.fill(x + 5, y + 4, x + 9, y + 6, -10328475);
      graphics.fill(x + 4, y + 2, x + 6, y + 3, -8946823);
   }

   private boolean handleOriginalClick(double mouseX, double mouseY) {
      return switch (((MachineMenu)this.menu).kind()) {
         case FACTORY_HOPPER -> this.handleFactoryHopperClick(mouseX, mouseY);
         case FACTORY_DROPPER -> this.handleFactoryDropperClick(mouseX, mouseY);
         case FACTORY_PLACER -> this.handleFactoryPlacerClick(mouseX, mouseY);
         default -> false;
         case SMALL_ELECTRICAL_FURNACE -> this.handleElectricalFurnaceClick(mouseX, mouseY);
      };
   }

   private boolean handleFactoryHopperClick(double mouseX, double mouseY) {
      int mx = (int)(mouseX - this.leftPos + 0.5);
      if (!this.isHovering(126, 1, 49, 60, mouseX, mouseY)) {
         return false;
      }

      if (this.isHovering(128, 9, 44, 10, mouseX, mouseY)) {
         int range = this.sliderValue(mx, 133, 34, 0, 4, ((MachineMenu)this.menu).field(0), 1);
         this.sendAction(1, range);
      } else if (this.isHovering(128, 21, 44, 10, mouseX, mouseY)) {
         this.sendAction(4, this.sliderValue(mx, 133, 34, 0, 100, ((MachineMenu)this.menu).field(3), 3));
      } else if (this.isHovering(128, 34, 44, 10, mouseX, mouseY)) {
         this.sendAction(2, this.sliderValue(mx, 134, 34, 1, 32, ((MachineMenu)this.menu).field(1), 1));
      } else if (this.isHovering(133, 49, 9, 9, mouseX, mouseY)) {
         this.sendAction(5, 1);
      } else if (this.isHovering(145, 49, 9, 9, mouseX, mouseY)) {
         int mask = 5;
         int mode = ((MachineMenu)this.menu).field(2) & mask;

         int next = switch (mode) {
            case 0 -> 1;
            case 1 -> 4;
            default -> 4;
            case 4 -> 0;
         };
         this.sendAction(3, ((MachineMenu)this.menu).field(2) & ~mask | next);
      } else if (this.isHovering(159, 49, 7, 9, mouseX, mouseY)) {
         this.sendAction(3, ((MachineMenu)this.menu).field(2) ^ 2);
      }

      return true;
   }

   private boolean handleFactoryDropperClick(double mouseX, double mouseY) {
      int mx = (int)(mouseX - this.leftPos + 0.5);
      int my = (int)(mouseY - this.topPos + 0.5);
      if (!this.isHovering(114, 1, 61, 79, mouseX, mouseY)) {
         return false;
      }

      if (this.isHovering(130, 10, 12, 25, mouseX, mouseY)) {
         this.sendAction(10, 100 - Mth.clamp((my - 10) * 100 / 25, 0, 100));
      } else if (this.isHovering(145, 10, 25, 25, mouseX, mouseY)) {
         int xDev = Mth.clamp((int)Math.round((mx - 157) * 100.0 / 12.0), -100, 100);
         int yDev = Mth.clamp(-((int)Math.round((my - 22) * 100.0 / 12.0)), -100, 100);
         if (Math.abs(xDev) < 9) {
            xDev = 0;
         }

         if (Math.abs(yDev) < 9) {
            yDev = 0;
         }

         this.sendAction(11, xDev, yDev);
      } else if (this.isHovering(129, 40, 44, 10, mouseX, mouseY)) {
         this.sendAction(12, this.sliderValue(mx, 135, 34, 1, 32, ((MachineMenu)this.menu).field(4), 1));
      } else if (this.isHovering(129, 50, 44, 10, mouseX, mouseY)) {
         this.sendAction(13, this.sliderValue(mx, 135, 34, 0, 100, ((MachineMenu)this.menu).field(6), 3));
      } else if (this.isHovering(114, 51, 9, 9, mouseX, mouseY)) {
         this.sendAction(15, 1);
      } else if (this.isHovering(162, 66, 7, 9, mouseX, mouseY)) {
         this.sendAction(14, ((MachineMenu)this.menu).field(5) ^ 16);
      } else if (this.isHovering(132, 66, 9, 9, mouseX, mouseY)) {
         this.sendAction(14, ((MachineMenu)this.menu).field(5) ^ 1);
      } else if (this.isHovering(148, 66, 9, 9, mouseX, mouseY)) {
         int mask = 34;

         int next = switch (((MachineMenu)this.menu).field(5) & mask) {
            case 0 -> 32;
            case 2 -> 0;
            case 32 -> 2;
            default -> 2;
         };
         this.sendAction(14, ((MachineMenu)this.menu).field(5) & ~mask | next);
      }

      return true;
   }

   private boolean handleFactoryPlacerClick(double mouseX, double mouseY) {
      MachineScreen.FactoryPlacerButton button = this.factoryPlacerButtonAt(mouseX, mouseY);
      if (button != null) {
         switch (button) {
            case REDSTONE_SIGNAL:
               this.sendAction(21, 1);
               break;
            case INVERSION:
               int mask = 5;
               int mode = ((MachineMenu)this.menu).field(0) & mask;

               int next = switch (mode) {
                  case 0 -> 1;
                  case 1 -> 4;
                  default -> 4;
                  case 4 -> 0;
               };
               this.sendAction(20, ((MachineMenu)this.menu).field(0) & ~mask | next);
               break;
            case TRIGGER_MODE:
               this.sendAction(20, ((MachineMenu)this.menu).field(0) ^ 2);
         }

         return true;
      } else {
         MachineScreen.FactoryPlacerLine line = this.factoryPlacerLineAt(mouseX, mouseY);
         if (line == null) {
            return false;
         }

         this.sendAction(22, line.row());
         return true;
      }
   }

   private MachineScreen.FactoryPlacerButton factoryPlacerButtonAt(double mouseX, double mouseY) {
      if (this.isHovering(133, 49, 9, 9, mouseX, mouseY)) {
         return MachineScreen.FactoryPlacerButton.REDSTONE_SIGNAL;
      } else if (this.isHovering(145, 49, 9, 9, mouseX, mouseY)) {
         return MachineScreen.FactoryPlacerButton.INVERSION;
      } else {
         return this.isHovering(159, 49, 7, 9, mouseX, mouseY) ? MachineScreen.FactoryPlacerButton.TRIGGER_MODE : null;
      }
   }

   private MachineScreen.FactoryPlacerLine factoryPlacerLineAt(double mouseX, double mouseY) {
      if (this.isHovering(126, 6, 49, 13, mouseX, mouseY)) {
         return MachineScreen.FactoryPlacerLine.TOP;
      } else if (this.isHovering(126, 20, 49, 13, mouseX, mouseY)) {
         return MachineScreen.FactoryPlacerLine.MIDDLE;
      } else {
         return this.isHovering(126, 34, 49, 10, mouseX, mouseY) ? MachineScreen.FactoryPlacerLine.BOTTOM : null;
      }
   }

   private void drawFactoryPlacerControlHover(GuiGraphics graphics, double mouseX, double mouseY) {
      MachineScreen.FactoryPlacerButton button = this.factoryPlacerButtonAt(mouseX, mouseY);
      if (button != null) {
         switch (button) {
            case REDSTONE_SIGNAL:
               this.drawHoverOutline(graphics, this.leftPos + 133, this.topPos + 49, 9, 9, -1917873);
               break;
            case INVERSION:
               this.drawHoverOutline(graphics, this.leftPos + 145, this.topPos + 49, 9, 9, -1917873);
               break;
            case TRIGGER_MODE:
               this.drawHoverOutline(graphics, this.leftPos + 159, this.topPos + 49, 9, 9, -1917873);
         }
      } else {
         MachineScreen.FactoryPlacerLine line = this.factoryPlacerLineAt(mouseX, mouseY);
         if (line != null) {
            this.drawHoverOutline(graphics, this.leftPos + 126, this.topPos + line.y(), 49, line.height(), -1917873);
            this.drawHoverOutline(graphics, this.leftPos + 9, this.topPos + 7 + line.row() * 17, 110, 19, -1917873);
         }
      }
   }

   private boolean handleElectricalFurnaceClick(double mouseX, double mouseY) {
      if (!this.isHovering(134, 48, 30, 28, mouseX, mouseY)) {
         return false;
      }

      if (this.isHovering(144, 64, 6, 10, mouseX, mouseY)) {
         this.sendAction(30, 0);
      } else if (this.isHovering(134, 58, 10, 6, mouseX, mouseY)) {
         this.sendAction(30, 1);
      } else if (this.isHovering(144, 48, 6, 10, mouseX, mouseY)) {
         this.sendAction(30, 2);
      } else if (this.isHovering(150, 58, 10, 6, mouseX, mouseY)) {
         this.sendAction(30, 3);
      }

      return true;
   }

   private void renderMachineTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Component funnelTooltip = this.smallFluidFunnelTooltip(mouseX, mouseY);
        if (funnelTooltip != null) {
            graphics.renderTooltip(this.font, funnelTooltip, mouseX, mouseY);
            return;
        }
        Component accumulatorTooltip = this.passiveFluidAccumulatorTooltip(mouseX, mouseY);
        if (accumulatorTooltip != null) {
            graphics.renderTooltip(this.font, accumulatorTooltip, mouseX, mouseY);
            return;
        }

      Component fluidBarrelTooltip = this.fluidBarrelTooltip(mouseX, mouseY);
      if (fluidBarrelTooltip != null) {
         graphics.renderTooltip(this.font, fluidBarrelTooltip, mouseX, mouseY);
         return;
      }

      Component milkingTooltip = this.smallMilkingMachineTooltip(mouseX, mouseY);
      if (milkingTooltip != null) {
         // Font.split preserves component styles and interprets explicit newlines.
         graphics.renderTooltip(this.font,
            this.font.split(milkingTooltip, milkingTooltipWidth(this.width)), mouseX, mouseY);
         return;
      }

      Component solarEnergyTooltip = this.solarPanelEnergyTooltip(mouseX, mouseY);
      if (solarEnergyTooltip != null) {
         graphics.renderTooltip(this.font, solarEnergyTooltip, mouseX, mouseY);
         return;
      }
      Component blockBreakerTooltip = this.blockBreakerTooltip(mouseX, mouseY);
      if (blockBreakerTooltip != null) {
         graphics.renderTooltip(this.font, blockBreakerTooltip, mouseX, mouseY);
         return;
      }

      Component controlTooltip = this.originalControlTooltip(mouseX, mouseY);
      if (controlTooltip != null) {
         graphics.renderTooltip(this.font, controlTooltip, mouseX, mouseY);
      } else if (this.hoveredSlot == null || !this.hoveredSlot.hasItem()) {
         for (MachineLayout.MachineSlot slot : ((MachineMenu)this.menu).layout().machineSlots()) {
            if (this.isHovering(slot.x(), slot.y(), 16, 16, mouseX, mouseY)) {
               graphics.renderTooltip(this.font, this.slotTooltip(slot), mouseX, mouseY);
               return;
            }
         }
      }
   }

   private Component solarPanelEnergyTooltip(int mouseX, int mouseY) {
      MachineMenu menu = (MachineMenu)this.menu;
      if (menu.kind() != MachineKind.SMALL_SOLAR_PANEL
         || !containsSolarPanelEnergyTooltip(menu.layout(), mouseX - this.leftPos, mouseY - this.topPos)) {
         return null;
      }

      return Component.translatable(
         "gui.immersive_engineer_decor_controls_tool_reforged.small_solar_panel.energy_tooltip",
         menu.energyStored(), menu.energyCapacity(), menu.energyDisplayGranularity()
      );
   }

   static boolean containsSolarPanelEnergyTooltip(MachineLayout layout, int x, int y) {
      MachineLayout.Panel meter = layout.panels().stream()
         .filter(panel -> panel.label().equals("solar-energy-bar"))
         .findFirst()
         .orElse(null);
      MachineLayout.Panel readout = layout.panels().stream()
         .filter(panel -> panel.label().equals("solar-energy-readout-mask"))
         .findFirst()
         .orElse(null);
      if (meter == null || readout == null) {
         return false;
      }

      int left = Math.min(meter.x(), readout.x());
      int top = Math.min(meter.y(), readout.y());
      int right = Math.max(meter.x() + meter.width(), readout.x() + readout.width());
      int bottom = Math.max(meter.y() + meter.height(), readout.y() + readout.height());
      return x >= left && x < right && y >= top && y < bottom;
   }

   private Component slotTooltip(MachineLayout.MachineSlot slot) {
      return (Component)(switch (((MachineMenu)this.menu).kind()) {
         case FLUID_BARREL -> Component.translatable(
            slot.containerIndex() == 0
               ? "gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.bucket.input"
               : "gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.bucket.output"
         );
         case FACTORY_HOPPER -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.slots.storage");
         case FACTORY_DROPPER -> Component.translatable(
            slot.containerIndex() < 12
               ? "block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.slots.input"
               : "block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.slots.filter"
         );
         case FACTORY_PLACER -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_placer.slots.blocks");
         case SMALL_LAB_FURNACE -> this.labFurnaceSlotTooltip(slot.containerIndex());
         case SMALL_ELECTRICAL_FURNACE -> this.electricalFurnaceSlotTooltip(slot.containerIndex());
            case SMALL_FLUID_FUNNEL -> Component.translatable(
                slot.containerIndex() == 0
                    ? "gui.immersive_engineer_decor_controls_tool_reforged.small_fluid_funnel.bucket.input"
                    : "gui.immersive_engineer_decor_controls_tool_reforged.small_fluid_funnel.bucket.output"
            );
            case PASSIVE_FLUID_ACCUMULATOR -> Component.translatable(
                slot.containerIndex() == 0
                    ? "gui.immersive_engineer_decor_controls_tool_reforged.passive_fluid_accumulator.bucket.input"
                    : "gui.immersive_engineer_decor_controls_tool_reforged.passive_fluid_accumulator.bucket.output"
            );
          case SMALL_FREEZER -> this.smallFreezerSlotTooltip(slot.containerIndex());
         case METAL_CRAFTING_TABLE -> this.metalCraftingTableSlotTooltip(slot.containerIndex());
         case SMALL_WASTE_INCINERATOR -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_waste_incinerator.slots.queue");
         case SMALL_MILKING_MACHINE -> Component.translatable(MILKING_GUI_PREFIX + (slot.containerIndex() == 0 ? "tooltip.slot.bucket" : "tooltip.slot.output"));
         default -> Component.literal(slot.role());
      });
   }

   private Component metalCraftingTableSlotTooltip(int index) {
      return switch (index) {
         case 0 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.metal_crafting_table.slots.hammer");
         case 10 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.metal_crafting_table.slots.output");
         default -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.metal_crafting_table.slots.grid");
      };
   }

   private Component labFurnaceSlotTooltip(int index) {
      return switch (index) {
         case 0 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.input");
         case 1 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.fuel");
         case 2 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.output");
         case 3, 4 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.input_fifo");
         case 5, 6 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.fuel_fifo");
         case 7, 8 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.output_fifo");
         case 9, 10 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.slots.aux");
         default -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.machine.slots.generic");
      };
   }

   private Component electricalFurnaceSlotTooltip(int index) {
      return switch (index) {
         case 0 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.slots.input");
         case 1 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.slots.aux");
         case 2 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.slots.output");
         case 3, 4 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.slots.input_fifo");
         case 5, 6 -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.slots.output_fifo");
         default -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.machine.slots.generic");
      };
   }

   private Component originalControlTooltip(double mouseX, double mouseY) {
      if (((MachineMenu)this.menu).layout().style() != MachineLayout.LayoutStyle.ORIGINAL_TEXTURE) {
         return null;
      }

      return switch (((MachineMenu)this.menu).kind()) {
         case FACTORY_HOPPER -> this.factoryHopperTooltip(mouseX, mouseY);
         case FACTORY_DROPPER -> this.factoryDropperTooltip(mouseX, mouseY);
         case FACTORY_PLACER -> this.factoryPlacerTooltip(mouseX, mouseY);
         case SMALL_LAB_FURNACE -> this.labFurnaceTooltip(mouseX, mouseY);
         case SMALL_ELECTRICAL_FURNACE -> this.electricalFurnaceTooltip(mouseX, mouseY);
         case SMALL_FREEZER -> this.smallFreezerTooltip(mouseX, mouseY);
         default -> null;
         case SMALL_WASTE_INCINERATOR -> this.wasteIncineratorTooltip(mouseX, mouseY);
      };
   }

   private Component smallFreezerTooltip(double mouseX, double mouseY) {
      MachineMenu menu = (MachineMenu)this.menu;
      if (menu.kind() != MachineKind.SMALL_FREEZER) {
         return null;
      }
      if (this.isHovering(16, 102, 137, 16, mouseX, mouseY)) {
         return smallFreezerText("tooltip.progress", menu.freezerProgressTicks(), MachineBlockEntity.FREEZER_PROCESS_TICKS);
      }
      if (this.isHovering(62, 124, 87, 19, mouseX, mouseY)
            || this.isHovering(151, 120, 50, 10, mouseX, mouseY)) {
         return smallFreezerText("tooltip.energy", menu.energyStored(), menu.energyCapacity());
      }
      if (this.isHovering(16, 93, 113, 10, mouseX, mouseY)) {
         return smallFreezerText("tooltip.status",
            smallFreezerText("status." + smallFreezerStatusKey(menu.freezerStatus())),
            smallFreezerText(menu.freezerRedstonePowered() ? "redstone.high" : "redstone.low"));
      }
      if (this.isHovering(129, 93, 30, 10, mouseX, mouseY)) {
         return smallFreezerText("tooltip.phase", menu.freezerPhase());
      }
      if (this.isHovering(159, 93, 46, 10, mouseX, mouseY)) {
         return smallFreezerText("tooltip.redstone",
            smallFreezerText(menu.freezerRedstonePowered() ? "redstone.high" : "redstone.low"));
      }
      if (this.isHovering(94, 55, 24, 24, mouseX, mouseY)) {
         ItemStack preview = menu.freezerPreviewStack();
         return preview.isEmpty()
            ? smallFreezerText("tooltip.preview_empty")
            : smallFreezerText("tooltip.preview", preview.getHoverName());
      }
      return null;
   }

   private Component smallFreezerSlotTooltip(int index) {
      return switch (index) {
         case 0 -> smallFreezerText("slot.input");
         case 1 -> smallFreezerText("slot.cold_reserved");
         case 2 -> smallFreezerText("slot.output");
         default -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.machine.slots.generic");
      };
   }

   private Component factoryHopperTooltip(double mouseX, double mouseY) {
      if (this.isHovering(128, 9, 44, 10, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.range");
      } else if (this.isHovering(145, 20, 8, 8, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.delayindicator");
      } else if (this.isHovering(128, 21, 44, 10, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.period");
      } else if (this.isHovering(128, 34, 44, 10, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.count");
      } else if (this.isHovering(133, 49, 9, 9, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.rssignal");
      } else if (this.isHovering(145, 49, 9, 9, mouseX, mouseY)) {
         int logic = ((MachineMenu)this.menu).field(2);
         String state = (logic & 4) != 0 ? "ignored" : (logic & 1) != 0 ? "inverted" : "normal";
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.inversion")
            .append("\n").append(Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.state." + state));
      } else {
         int logic = ((MachineMenu)this.menu).field(2);
         String state = (logic & 4) != 0 ? "ignore_trigger" : (logic & 2) != 0 ? "continuous" : "pulse";
         return this.isHovering(159, 49, 7, 9, mouseX, mouseY)
            ? Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.tooltips.triggermode")
               .append("\n").append(Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_hopper.state." + state))
            : null;
      }
   }

   private Component factoryDropperTooltip(double mouseX, double mouseY) {
      if (this.isHovering(130, 10, 12, 25, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.velocity");
      } else if (this.isHovering(145, 10, 25, 25, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.direction");
      } else if (this.isHovering(129, 40, 44, 10, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.dropcount");
      } else if (this.isHovering(129, 50, 44, 10, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.period");
      } else if (this.isHovering(114, 51, 9, 9, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.rssignal");
      } else if (this.isHovering(132, 66, 9, 9, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.filtergate");
      } else if (this.isHovering(148, 66, 9, 9, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.externgate");
      } else {
         return this.isHovering(162, 66, 7, 9, mouseX, mouseY)
            ? Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_dropper.tooltips.triggermode")
            : null;
      }
   }

   private Component factoryPlacerTooltip(double mouseX, double mouseY) {
      MachineScreen.FactoryPlacerButton button = this.factoryPlacerButtonAt(mouseX, mouseY);
      if (button != null) {
         return switch (button) {
            case REDSTONE_SIGNAL -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_placer.tooltips.rssignal");
            case INVERSION -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_placer.tooltips.inversion");
            case TRIGGER_MODE -> Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.factory_placer.tooltips.triggermode");
         };
      } else {
         MachineScreen.FactoryPlacerLine line = this.factoryPlacerLineAt(mouseX, mouseY);
         return line != null ? Component.translatable(line.tooltipKey()) : null;
      }
   }

   private Component labFurnaceTooltip(double mouseX, double mouseY) {
      if (this.isHovering(59, 35, 18, 18, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.tooltips.flame");
      } else {
         return this.isHovering(78, 37, 22, 18, mouseX, mouseY)
            ? Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_lab_furnace.tooltips.progress")
            : null;
      }
   }

   private Component electricalFurnaceTooltip(double mouseX, double mouseY) {
      if (this.isHovering(62, 54, 14, 15, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.tooltips.heat");
      } else if (this.isHovering(78, 28, 22, 18, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.tooltips.progress");
      } else if (this.isHovering(89, 52, 35, 17, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.tooltips.energy");
      } else {
         return this.isHovering(134, 48, 30, 28, mouseX, mouseY)
            ? Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_electrical_furnace.tooltips.speed")
            : null;
      }
   }

   private Component wasteIncineratorTooltip(double mouseX, double mouseY) {
      if (this.isHovering(10, 49, 31, 27, mouseX, mouseY)) {
         return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_waste_incinerator.tooltips.firebox");
      } else {
         return this.isHovering(8, 5, 160, 72, mouseX, mouseY)
            ? Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_waste_incinerator.tooltips.queue")
            : null;
      }
   }

   private int sliderValue(int mouseX, int originX, int span, int min, int max, int current, int step) {
      int relative = mouseX - originX;
      if (relative < -1) {
         return Mth.clamp(current - step, min, max);
      } else {
         return relative >= span ? Mth.clamp(current + step, min, max) : Mth.clamp((int)Math.round(min + (max - min) * ((double)relative / span)), min, max);
      }
   }

   private void sendAction(int action, int value) {
      this.sendAction(action, value, 0);
   }

   private void sendAction(int action, int valueA, int valueB) {
      PacketDistributor.sendToServer(new MachineActionPayload(((MachineMenu)this.menu).containerId, action, valueA, valueB), new CustomPacketPayload[0]);
   }

   private void drawMachineSlots(GuiGraphics graphics, MachineLayout layout, int x, int y) {
      for (MachineLayout.MachineSlot slot : layout.machineSlots()) {
         this.drawSlot(graphics, x + slot.x(), y + slot.y());
      }
   }

   private void drawPlayerSlots(GuiGraphics graphics, MachineLayout layout, int x, int y) {
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            this.drawSlot(graphics, x + layout.playerSlotX(column), y + layout.playerSlotY(row));
         }
      }

      for (int column = 0; column < 9; column++) {
         this.drawSlot(graphics, x + layout.hotbarSlotX(column), y + layout.hotbarY());
      }
   }

   private void drawSlot(GuiGraphics graphics, int x, int y) {
      graphics.fill(x - 1, y - 1, x + 17, y + 17, -14934755);
      graphics.fill(x, y, x + 16, y + 16, -12236470);
      graphics.fill(x + 1, y + 1, x + 15, y + 15, -14276310);
      graphics.fill(x + 1, y + 1, x + 15, y + 2, -10854818);
      graphics.fill(x + 1, y + 14, x + 15, y + 15, -15263719);
   }

   private void drawArrow(GuiGraphics graphics, int x, int y, int fill) {
      graphics.fill(x, y, x + 24, y + 10, -14934755);
      graphics.fill(x + 1, y + 2, x + 18, y + 8, -12236470);
      graphics.fill(x + 18, y + 1, x + 21, y + 9, -12236470);
      graphics.fill(x + 21, y + 3, x + 23, y + 7, -12236470);
      graphics.fill(x + 1, y + 2, x + 1 + Mth.clamp(fill, 0, 21), y + 8, this.profile.accent());
   }

   private void drawMeter(GuiGraphics graphics, int x, int y, int width, int value, int max, int color) {
      int fill = Mth.clamp(value * width / Math.max(1, max), 0, width);
      graphics.fill(x, y, x + width + 2, y + 5, -14934755);
      graphics.fill(x + 1, y + 1, x + 1 + fill, y + 4, color);
      if (fill > 1) {
         graphics.fill(x + 1, y + 1, x + 1 + fill, y + 2, 1442840575);
      }
   }

   private void drawTank(GuiGraphics graphics, int x, int y, int width, int height, int value, int max) {
      int fill = Mth.clamp(value * (height - 2) / Math.max(1, max), 0, height - 2);
      graphics.fill(x, y, x + width, y + height, -14934755);
      graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, -14276310);
      graphics.fill(x + 2, y + height - 1 - fill, x + width - 2, y + height - 2, -11753768);
      graphics.fill(x + 2, y + 2, x + width - 2, y + 6, 1442840575);
   }

   private void drawSmallFluidFunnelBackgroundOverlays(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      MachineMenu menu = (MachineMenu)this.menu;
      this.drawTexturePatch(graphics, texture, x0 + 17, y0 + 4, 34, 20,
          308, 18, 22, 14, 385, 439);
      this.drawRepeatedTexturePatch(graphics, texture, x0 + 64, y0 + 6, 104, 16,
          308, 18, 22, 14, 385, 439, 1.0F);

      this.drawMachineSlotInteriorsFromArtwork(graphics, texture, x0, y0,
         0, 0, 385, 439, 385, 439, this.imageWidth, this.imageHeight);

      boolean frenchArtwork = this.isFrenchArtworkLocale();
      // The baked fluid name is a fixed example; replace it in every locale. Fixed French
      // bucket/outlet captions remain as authored in fr_fr and are patched only for other locales.
      if (!frenchArtwork) {
          this.drawRepeatedTexturePatch(graphics, texture, x0 + 20, y0 + 64, 46, 12,
             100, 110, 18, 18, 385, 439, 1.0F);
          this.drawRepeatedTexturePatch(graphics, texture, x0 + 142, y0 + 64, 48, 12,
             100, 110, 18, 18, 385, 439, 1.0F);
          this.drawRepeatedTexturePatch(graphics, texture, x0 + 121, y0 + 125, 65, 13,
             100, 110, 18, 18, 385, 439, 1.0F);
       this.drawTexturePatch(graphics, texture, x0 + 22, y0 + 141, 61, 10,
          140, 262, 50, 14, 385, 439);
      }
       // The former source rectangle covered the baked down-arrow, which leaked into the
       // dynamic fluid-name row. Use clean wall texture from below the input-bucket frame.
       this.drawTexturePatch(graphics, texture, x0 + 78, y0 + 77, 53, 17,
          80, 215, 28, 24, 385, 439);
      // The static numeric sample sits above the trough lip; clear its full ink bounds.
       this.drawTexturePatch(graphics, texture, x0 + 71, y0 + 92, 68, 13,
          80, 215, 28, 24, 385, 439);

      int tankLeft = x0 + 79;
      int tankTop = y0 + 105;
      int tankRight = x0 + 132;
      int tankBottom = y0 + 119;
      graphics.fill(tankLeft, tankTop, tankRight, tankBottom, 0xFF142028);

      int capacity = Math.max(1, menu.fluidCapacity());
      int fill = Mth.clamp((int)((long)Math.max(0, menu.fluidAmount()) * (tankBottom - tankTop - 2) / capacity), 0, tankBottom - tankTop - 2);
      int fluidCode = menu.smallFluidFunnelFluidCode();
      if (fill > 0 && fluidCode > 0) {
         int fluidTop = tankBottom - 1 - fill;
         graphics.fill(tankLeft + 1, fluidTop, tankRight - 1, tankBottom - 1, this.smallFluidFunnelFluidColor(fluidCode));
         graphics.fill(tankLeft + 2, fluidTop, tankRight - 2, fluidTop + 1, 0x99FFFFFF);
      }
   }

   private void drawSmallFluidFunnelLabels(GuiGraphics graphics) {
      MachineMenu menu = (MachineMenu)this.menu;
      this.drawCompactMachineTitle(
         graphics,
          Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.small_fluid_funnel").getString(),
          116, 10, 104, TEXT
      );
      if (!this.isFrenchArtworkLocale()) {
         this.drawScaledCenteredString(
            graphics,
            Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.small_fluid_funnel.bucket.input").getString(),
            43, 66, 45, TEXT, 0.82F
         );
         this.drawScaledCenteredString(
            graphics,
            Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.small_fluid_funnel.bucket.output").getString(),
            165, 66, 48, TEXT, 0.82F
         );
      }
      this.drawScaledCenteredString(
         graphics,
         this.smallFluidFunnelFluidName(menu.smallFluidFunnelFluidCode()).getString(),
         105, 82, 78, TEXT, 0.82F
      );
      this.drawScaledCenteredString(
         graphics,
         Component.translatable(
            "gui.immersive_engineer_decor_controls_tool_reforged.small_fluid_funnel.amount",
            menu.fluidAmount(), menu.fluidCapacity()
         ).getString(),
         105, 93, 78, TEXT, 0.82F
      );
      if (!this.isFrenchArtworkLocale()) {
         this.drawScaledCenteredString(
            graphics,
            Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.small_fluid_funnel.output").getString(),
            154, 126, 65, TEXT, 0.82F
         );
         this.drawScaledCenteredString(
            graphics,
            Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.small_fluid_funnel.inventory").getString(),
            57, 143, 60, TEXT, 0.82F
         );
      }
   }

   private Component smallFluidFunnelTooltip(double mouseX, double mouseY) {
      if (((MachineMenu)this.menu).kind() != MachineKind.SMALL_FLUID_FUNNEL || !this.isHovering(78, 104, 55, 17, mouseX, mouseY)) {
         return null;
      }

      MachineMenu menu = (MachineMenu)this.menu;
      return Component.translatable(
         "gui.immersive_engineer_decor_controls_tool_reforged.small_fluid_funnel.tank.tooltip",
         this.smallFluidFunnelFluidName(menu.smallFluidFunnelFluidCode()),
         menu.fluidAmount(),
         menu.fluidCapacity()
      );
   }

   private Component smallFluidFunnelFluidName(int code) {
      String state = switch (code) {
         case 1 -> "water";
         case 2 -> "lava";
         case 3 -> "milk";
         case 4 -> "other";
         default -> "empty";
      };
      return Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.small_fluid_funnel.fluid." + state);
   }

   private int smallFluidFunnelFluidColor(int code) {
      return switch (code) {
         case 1 -> 0xFF3F76E4;
         case 2 -> 0xFFF06A28;
         case 3 -> 0xFFE6E1D5;
         case 4 -> 0xFF8296A0;
         default -> 0xFF142028;
      };
   }


   private void drawFluidBarrelWidgets(GuiGraphics graphics, ResourceLocation texture, int x0, int y0) {
      MachineMenu menu = (MachineMenu)this.menu;
      boolean frenchArtwork = this.isFrenchArtworkLocale();
      this.drawTexturePatch(graphics, texture, x0 + 15, y0 + 5, 42, 20,
         58, 5, 17, 20, 201, 226);
      this.drawRepeatedTexturePatch(graphics, texture, x0 + 50, y0 + 6, 100, 15,
         155, 5, 22, 14, 201, 226, 1.0F);
      if (!frenchArtwork) {
         // The artwork's bucket, tank, and inventory captions are French. Replace them
         // only when the live locale is not French; French keeps its original lettering.
          this.drawRepeatedTexturePatch(graphics, texture, x0 + 20, y0 + 47, 45, 13,
             48, 34, 14, 14, 201, 226, 1.0F);
          this.drawRepeatedTexturePatch(graphics, texture, x0 + 140, y0 + 47, 46, 13,
             48, 34, 14, 14, 201, 226, 1.0F);
          this.drawRepeatedTexturePatch(graphics, texture, x0 + 70, y0 + 101, 65, 14,
             48, 34, 14, 14, 201, 226, 1.0F);
          this.drawTexturePatch(graphics, texture, x0 + 20, y0 + 132, 56, 10,
             84, 134, 38, 10, 201, 226);
      }
      // Always replace the baked sample amount; it is stale in every language.
      this.drawTexturePatch(graphics, texture, x0 + 68, y0 + 116, 68, 13,
         20, 34, 26, 14, 201, 226);

      this.drawMachineSlotInteriorsFromArtwork(graphics, texture, x0, y0,
         0, 0, 201, 226, 201, 226, this.imageWidth, this.imageHeight);

      int tankX = x0 + 92;
      int tankY = y0 + 35;
      int tankWidth = 25;
      int tankHeight = 74;
      graphics.fill(tankX, tankY, tankX + tankWidth, tankY + tankHeight, 0xFF142028);
      int capacity = Math.max(1, menu.fluidCapacity());
      int fill = Mth.clamp((int)((long)menu.fluidAmount() * (tankHeight - 4) / capacity), 0, tankHeight - 4);
      if (fill > 0 && menu.storedFluid() != Fluids.EMPTY) {
         int fluidTop = tankY + tankHeight - 2 - fill;
         if (FluidBarrelSpriteRenderer.draw(graphics, new FluidStack(menu.storedFluid(), menu.fluidAmount()),
            tankX + 2, fluidTop, tankWidth - 4, fill)) {
            graphics.fill(tankX + 3, fluidTop, tankX + tankWidth - 3, fluidTop + 1, 0x99FFFFFF);
         }
      }
      graphics.fill(tankX + 1, tankY + 2, tankX + 2, tankY + tankHeight - 2, 0xFF77858B);
      graphics.fill(tankX + tankWidth - 2, tankY + 2, tankX + tankWidth - 1, tankY + tankHeight - 2, 0xFF526067);
      graphics.fill(tankX + 3, tankY + 2, tankX + 5, tankY + tankHeight - 4, 0x66FFFFFF);
   }

   private void drawFluidBarrelLabels(GuiGraphics graphics) {
      MachineMenu menu = (MachineMenu)this.menu;
      this.drawCompactMachineTitle(graphics,
         Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.fluid_barrel").getString(),
         100, 8, 100, TEXT);
      if (!this.isFrenchArtworkLocale()) {
         this.drawScaledCenteredString(graphics, Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.bucket.input").getString(), 38, 49, 48, TEXT, 0.82F);
         this.drawScaledCenteredString(graphics, Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.bucket.output").getString(), 168, 49, 60, TEXT, 0.82F);
         this.drawScaledCenteredString(graphics, Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.tank").getString(), 102, 107, 64, TEXT, 0.82F);
      }
      this.drawScaledCenteredString(
         graphics,
         Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.amount", menu.fluidAmount(), menu.fluidCapacity()).getString(),
         102, 118, 68, TEXT, 0.82F
      );
      if (!this.isFrenchArtworkLocale()) {
         this.drawScaledCenteredString(
            graphics,
            Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.inventory").getString(),
            52, 132, 56, TEXT, 0.82F
         );
      }
   }

   private Component fluidBarrelTooltip(double mouseX, double mouseY) {
      if (((MachineMenu)this.menu).kind() != MachineKind.FLUID_BARREL || !this.isHovering(90, 33, 30, 78, mouseX, mouseY)) {
         return null;
      }

      MachineMenu menu = (MachineMenu)this.menu;
      return Component.translatable(
         "gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.tank.tooltip",
         this.fluidBarrelFluidName(menu.storedFluid()), menu.fluidAmount(), menu.fluidCapacity()
      );
   }

   private Component fluidBarrelFluidName(Fluid fluid) {
      if (fluid == Fluids.WATER) {
         return Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.fluid.water");
      } else if (fluid == Fluids.LAVA) {
         return Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.fluid.lava");
      } else if (fluid == Fluids.EMPTY) {
         return Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.fluid.empty");
      } else {
         ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
         return isMilkFluidId(id)
            ? Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.fluid_barrel.fluid.milk")
            : Component.literal(id == null ? "fluid" : id.toString());
      }
   }

   static boolean isMilkFluidId(ResourceLocation id) {
      return id != null
         && id.getNamespace().equals("minecraft")
         && id.getPath().equals("milk");
   }

   private void drawSolarCells(GuiGraphics graphics, int x, int y) {
      for (int row = 0; row < 2; row++) {
         for (int column = 0; column < 5; column++) {
            int cellX = x + column * 18;
            int cellY = y + row * 9;
            graphics.fill(cellX, cellY, cellX + 14, cellY + 6, -15656928);
            graphics.fill(cellX + 1, cellY + 1, cellX + 13, cellY + 5, -12755848);
         }
      }
   }

   private void drawSignal(GuiGraphics graphics, int x, int y) {
      int signal = ((MachineMenu)this.menu).comparatorOutput();

      for (int i = 0; i < 4; i++) {
         int color = signal > i * 4 ? -1941681 : -13353916;
         graphics.fill(x + i * 5, y + 15 - i * 4, x + i * 5 + 3, y + 18, color);
      }
   }

   private int workWidth(int max) {
      return Mth.clamp(((MachineMenu)this.menu).progress() * max / 100, 0, max);
   }

   public static final class FactoryDropperScreen extends MachineScreen {
      public FactoryDropperScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Factory Dropper", "Pulse Drop", -7432541));
      }
   }

   public static final class FactoryHopperScreen extends MachineScreen {
      public FactoryHopperScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Factory Hopper", "Collect", -8477094));
      }
   }

   private enum FactoryPlacerButton {
      REDSTONE_SIGNAL,
      INVERSION,
      TRIGGER_MODE;
   }

   private enum FactoryPlacerLine {
      TOP(0, 6, 13, "block.immersive_engineer_decor_controls_tool_reforged.factory_placer.tooltips.stock_row_top"),
      MIDDLE(1, 20, 13, "block.immersive_engineer_decor_controls_tool_reforged.factory_placer.tooltips.stock_row_middle"),
      BOTTOM(2, 34, 10, "block.immersive_engineer_decor_controls_tool_reforged.factory_placer.tooltips.stock_row_bottom");

      private final int row;
      private final int y;
      private final int height;
      private final String tooltipKey;

      FactoryPlacerLine(int row, int y, int height, String tooltipKey) {
         this.row = row;
         this.y = y;
         this.height = height;
         this.tooltipKey = tooltipKey;
      }

      private int row() {
         return this.row;
      }

      private int y() {
         return this.y;
      }

      private int height() {
         return this.height;
      }

      private String tooltipKey() {
         return this.tooltipKey;
      }
   }

   public static final class FactoryPlacerScreen extends MachineScreen {
      public FactoryPlacerScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Factory Block Placer", "Pulse Place", -7178323));
      }
   }

   public static final class FluidBarrelScreen extends MachineScreen {
      public FluidBarrelScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Fluid Barrel", "Stored Fluid", -11753768));
      }
   }

   public static final class LabeledCrateScreen extends MachineScreen {
      public LabeledCrateScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Labeled Crate", "Storage", -6457774));
      }
   }

   public static final class MetalCraftingTableScreen extends MachineScreen {
      public MetalCraftingTableScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Metal Crafting Table", "Plate Work", -7432541));
      }
   }

   public static final class PassiveFluidAccumulatorScreen extends MachineScreen {
      public PassiveFluidAccumulatorScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Passive Fluid Accumulator", "Accumulate", -11753768));
      }
   }

   protected record Profile(String title, String process, int accent) {
   }

   public static final class SmallBlockBreakerScreen extends MachineScreen {
      public SmallBlockBreakerScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Block Breaker", "Break Face", -5149104));
      }
   }

   public static final class SmallElectricalFurnaceScreen extends MachineScreen {
      public SmallElectricalFurnaceScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Electrical Furnace", "Electric", -1917873));
      }
   }

   public static final class SmallFluidFunnelScreen extends MachineScreen {
      public SmallFluidFunnelScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Fluid Funnel", "Collect Fluid", -11753768));
      }
   }

   public static final class SmallFreezerScreen extends MachineScreen {
      public SmallFreezerScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Water Freezer", "Freezing", -9652519));
      }
   }

   public static final class SmallLabFurnaceScreen extends MachineScreen {
      public SmallLabFurnaceScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Laboratory Furnace", "Smelting", -2066107));
      }
   }

   public static final class SmallMilkingMachineScreen extends MachineScreen {
      public SmallMilkingMachineScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Milking Machine", "Milk Cow", -1054776));
      }
   }

   public static final class SmallMineralSmelterScreen extends MachineScreen {
      public SmallMineralSmelterScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Mineral Smelter", "Melting", -2790851));
      }
   }

   public static final class SmallSolarPanelScreen extends MachineScreen {
      public SmallSolarPanelScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Solar Panel", "Sun Charge", -1917873));
      }
   }

   public static final class SmallTreeCutterScreen extends MachineScreen {
      public SmallTreeCutterScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Tree Cutter", "Cut Logs", -8477094));
      }
   }

   public static final class SmallWasteIncineratorScreen extends MachineScreen {
      public SmallWasteIncineratorScreen(MachineMenu menu, Inventory inventory, Component title) {
         super(menu, inventory, title, new MachineScreen.Profile("Small Waste Incinerator", "Burn Waste", -3839166));
      }
   }
}

