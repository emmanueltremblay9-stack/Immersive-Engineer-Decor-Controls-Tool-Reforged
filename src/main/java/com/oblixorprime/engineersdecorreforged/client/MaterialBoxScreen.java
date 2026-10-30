package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.menu.MaterialBoxMenu;
import com.oblixorprime.engineersdecorreforged.tools.MaterialBoxItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MaterialBoxScreen extends AbstractContainerScreen<MaterialBoxMenu> {
   private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
      EngineersDecorReforged.MOD_ID, "textures/gui/material_box_gui.png"
   );

   public MaterialBoxScreen(MaterialBoxMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = 213;
      this.imageHeight = 206;
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
      Slot locked = this.menu.lockedSlot();
      if (locked != null) {
         graphics.blit(BACKGROUND, this.leftPos + locked.x, this.topPos + locked.y, 240, 183, 16, 16);
      }
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   @Override
   protected void slotClicked(Slot slot, int slotId, int button, ClickType clickType) {
      if (clickType == ClickType.SWAP) {
         return;
      }
      if (clickType == ClickType.QUICK_MOVE && slot != null && slot.hasItem() && hasShiftDown() && hasControlDown()) {
         ItemStack reference = slot.getItem().copy();
         int first = slotId < MaterialBoxItem.SLOT_COUNT ? 0 : MaterialBoxItem.SLOT_COUNT;
         int last = slotId < MaterialBoxItem.SLOT_COUNT ? MaterialBoxItem.SLOT_COUNT : this.menu.slots.size();
         List<Integer> matches = new ArrayList<>();
         for (int candidate = first; candidate < last; candidate++) {
            ItemStack candidateStack = this.menu.getSlot(candidate).getItem();
            if (!candidateStack.isEmpty() && ItemStack.isSameItemSameComponents(reference, candidateStack)) {
               matches.add(candidate);
            }
         }
         for (int candidate : matches) {
            super.slotClicked(this.menu.getSlot(candidate), candidate, button, ClickType.QUICK_MOVE);
         }
         return;
      }
      super.slotClicked(slot, slotId, button, clickType);
   }
}
