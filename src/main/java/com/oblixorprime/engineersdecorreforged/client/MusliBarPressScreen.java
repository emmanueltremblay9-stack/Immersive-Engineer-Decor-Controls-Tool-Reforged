package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.menu.MusliBarPressMenu;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public final class MusliBarPressScreen extends AbstractContainerScreen<MusliBarPressMenu> {
   private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
      EngineersDecorReforged.MOD_ID, "textures/gui/musli_bar_press_gui.png"
   );

   public MusliBarPressScreen(MusliBarPressMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      int x = this.leftPos;
      int y = this.topPos;
      graphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);
      graphics.blit(BACKGROUND, x + 48, y + 45, 180, 45, progressPixels(this.menu.seedsStored(), MusliBarPressItem.MAX_SEEDS), 7);
      graphics.blit(BACKGROUND, x + 48, y + 54, 180, 54, progressPixels(this.menu.foodStored(), MusliBarPressItem.MAX_FOOD), 7);
      Slot locked = this.menu.lockedSlot();
      if (locked != null) {
         graphics.blit(BACKGROUND, x + locked.x, y + locked.y, 181, 144, 16, 16);
      }
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   @Override
   protected void slotClicked(Slot slot, int slotId, int button, ClickType clickType) {
      if (clickType != ClickType.SWAP) {
         super.slotClicked(slot, slotId, button, clickType);
      }
   }

   private static int progressPixels(int value, int maximum) {
      int clamped = Mth.clamp(value, 0, maximum);
      return clamped <= 0 ? 0 : Math.max(1, (int)(Math.sqrt(clamped) / Math.sqrt(maximum) * 43.0));
   }
}
