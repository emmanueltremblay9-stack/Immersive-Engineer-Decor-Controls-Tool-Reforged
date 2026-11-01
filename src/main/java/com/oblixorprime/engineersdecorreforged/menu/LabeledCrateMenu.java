package com.oblixorprime.engineersdecorreforged.menu;

import com.oblixorprime.engineersdecorreforged.ModMenus;
import com.oblixorprime.engineersdecorreforged.utility.LabeledCrateItemRules;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class LabeledCrateMenu extends AbstractContainerMenu {
   public static final int STORAGE_SLOT_COUNT = 54;
   public static final int FRAME_SLOT = 54;
   public static final int CRATE_SLOT_COUNT = 55;
   public static final int MENU_SLOT_COUNT = 91;
   private static final int PLAYER_SLOT_START = CRATE_SLOT_COUNT;
   private static final int PLAYER_SLOT_END = MENU_SLOT_COUNT;
   private final Container container;

   public static LabeledCrateMenu client(int id, Inventory inventory) {
      return new LabeledCrateMenu(id, inventory, new SimpleContainer(CRATE_SLOT_COUNT));
   }

   public LabeledCrateMenu(int id, Inventory inventory, Container container) {
      super((MenuType)ModMenus.LABELED_CRATE.get(), id);
      checkContainerSize(container, CRATE_SLOT_COUNT);
      this.container = container;
      container.startOpen(inventory.player);

      for (int row = 0; row < 6; row++) {
         for (int column = 0; column < 9; column++) {
            int slot = column + row * 9;
            this.addSlot(new StorageSlot(container, slot, 28 + column * 18, 10 + row * 18));
         }
      }
      this.addSlot(new FrameSlot(container, FRAME_SLOT, 191, 100));

      // Keep the original inventory order: three main rows first, then the hotbar.
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column + row * 9 + 9, 28 + column * 18, 125 + row * 18));
         }
      }
      for (int column = 0; column < 9; column++) {
         this.addSlot(new Slot(inventory, column, 28 + column * 18, 183));
      }
   }

   public Container crateContainer() {
      return this.container;
   }

   @Override
   public boolean stillValid(Player player) {
      return this.container.stillValid(player);
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
      return slot.getMaxStackSize() > 1;
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      this.container.stopOpen(player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      if (index < 0 || index >= this.slots.size()) {
         return ItemStack.EMPTY;
      }

      Slot source = this.slots.get(index);
      if (!source.hasItem()) {
         return ItemStack.EMPTY;
      }

      ItemStack original = source.getItem().copy();
      ItemStack moving = source.getItem();
      boolean frameSource = index == FRAME_SLOT;
      if (frameSource) {
         // Keep the backing frame stack intact until setByPlayer notifies the block entity.
         moving = moving.copy();
      }
      boolean moved;
      if (index < CRATE_SLOT_COUNT) {
         moved = this.moveItemStackTo(moving, PLAYER_SLOT_START, PLAYER_SLOT_END, true);
      } else {
         moved = this.moveItemStackTo(moving, 0, STORAGE_SLOT_COUNT, false);
      }
      if (!moved) {
         return ItemStack.EMPTY;
      }

      if (moving.isEmpty()) {
         source.setByPlayer(ItemStack.EMPTY);
      } else if (frameSource) {
         source.setByPlayer(moving);
      } else {
         source.setChanged();
      }
      source.onTake(player, original);
      return original;
   }

   public boolean quickMoveAll(Player player, int selectedSlot) {
      if (selectedSlot < 0 || selectedSlot >= this.slots.size() || selectedSlot == FRAME_SLOT) {
         return false;
      }

      boolean fromCrate = selectedSlot < STORAGE_SLOT_COUNT;
      int sourceStart = fromCrate ? 0 : PLAYER_SLOT_START;
      int sourceEnd = fromCrate ? STORAGE_SLOT_COUNT : PLAYER_SLOT_END;
      int targetStart = fromCrate ? PLAYER_SLOT_START : 0;
      int targetEnd = fromCrate ? PLAYER_SLOT_END : STORAGE_SLOT_COUNT;
      Slot selected = this.slots.get(selectedSlot);
      if (!selected.hasItem()) {
         return false;
      }

      ItemStack matching = selected.getItem().copy();
      boolean changed = false;
      for (int index = sourceStart; index < sourceEnd; index++) {
         Slot source = this.slots.get(index);
         if (!source.hasItem() || !ItemStack.isSameItemSameComponents(matching, source.getItem())) {
            continue;
         }

         ItemStack before = source.getItem().copy();
         ItemStack moving = source.getItem();
         if (this.moveItemStackTo(moving, targetStart, targetEnd, fromCrate)) {
            changed = true;
            if (moving.isEmpty()) {
               source.setByPlayer(ItemStack.EMPTY);
            } else {
               source.setChanged();
            }
            source.onTake(player, before);
         }
      }

      if (changed) {
         player.getInventory().setChanged();
         this.broadcastChanges();
      }
      return changed;
   }

   private static final class StorageSlot extends Slot {
      private StorageSlot(Container container, int index, int x, int y) {
         super(container, index, x, y);
      }

      @Override
      public int getMaxStackSize() {
         return 64;
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return LabeledCrateItemRules.canStore(stack);
      }
   }

   private static final class FrameSlot extends Slot {
      private FrameSlot(Container container, int index, int x, int y) {
         super(container, index, x, y);
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return LabeledCrateItemRules.canStore(stack);
      }
   }
}
