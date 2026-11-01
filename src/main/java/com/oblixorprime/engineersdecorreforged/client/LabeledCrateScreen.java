package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.menu.LabeledCrateMenu;
import com.oblixorprime.engineersdecorreforged.network.LabeledCrateQuickMovePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LabeledCrateScreen extends AbstractContainerScreen<LabeledCrateMenu> {
   private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
      EngineersDecorReforged.MOD_ID, "textures/gui/labeled_crate_gui.png"
   );

   public LabeledCrateScreen(LabeledCrateMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = 213;
      this.imageHeight = 206;
      this.titleLabelX = 23;
      this.titleLabelY = -10;
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, this.title, this.titleLabelX + 1, this.titleLabelY + 1, 0x303030, false);
      graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x707070, false);
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