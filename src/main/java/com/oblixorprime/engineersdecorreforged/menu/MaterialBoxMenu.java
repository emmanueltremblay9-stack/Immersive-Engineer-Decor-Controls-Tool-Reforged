package com.oblixorprime.engineersdecorreforged.menu;

import com.oblixorprime.engineersdecorreforged.ModMenus;
import com.oblixorprime.engineersdecorreforged.tools.MaterialBoxItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MaterialBoxMenu extends AbstractContainerMenu {
   private static final int STORAGE_SLOTS = 54;
   private final Player player;
   private final InteractionHand hand;
   private final ItemStack box;
   private final SimpleContainer storage;
   private Slot lockedSlot;

   public MaterialBoxMenu(int containerId, Inventory inventory, InteractionHand hand) {
      super(ModMenus.MATERIAL_BOX.get(), containerId);
      this.player = inventory.player;
      this.hand = hand;
      this.box = this.player.getItemInHand(hand);
      this.storage = new SimpleContainer(STORAGE_SLOTS);
      NonNullList<ItemStack> saved = MaterialBoxItem.loadContents(this.box, this.player.registryAccess());
      for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
         this.storage.setItem(slot, saved.get(slot));
      }
      this.storage.addListener(ignored -> this.save());

      for (int row = 0; row < 6; row++) {
         for (int column = 0; column < 9; column++) {
            this.addSlot(new StorageSlot(this.storage, column + row * 9, 28 + column * 18, 10 + row * 18, this.player));
         }
      }
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            int inventorySlot = column + row * 9 + 9;
            this.addSlot(playerSlot(inventory, inventorySlot, 28 + column * 18, 125 + row * 18));
         }
      }
      for (int column = 0; column < 9; column++) {
         this.addSlot(playerSlot(inventory, column, 28 + column * 18, 183));
      }
   }

   @Override
   public boolean stillValid(Player player) {
      return player == this.player && player.isAlive() && player.getItemInHand(this.hand) == this.box;
   }

   public Slot lockedSlot() {
      return this.lockedSlot;
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      if (index < 0 || index >= this.slots.size()) {
         return ItemStack.EMPTY;
      }
      Slot slot = this.slots.get(index);
      if (slot == this.lockedSlot || !slot.mayPickup(player) || !slot.hasItem()) {
         return ItemStack.EMPTY;
      }
      ItemStack moving = slot.getItem();
      ItemStack original = moving.copy();
      if (index < STORAGE_SLOTS) {
         if (!this.moveItemStackTo(moving, STORAGE_SLOTS, this.slots.size(), false)) {
            return ItemStack.EMPTY;
         }
      } else {
         if (isFilledBox(moving) || !this.moveItemStackTo(moving, 0, STORAGE_SLOTS, false)) {
            return ItemStack.EMPTY;
         }
      }
      if (moving.isEmpty()) {
         slot.set(ItemStack.EMPTY);
      } else {
         slot.setChanged();
      }
      slot.onTake(player, moving);
      return original;
   }

   @Override
   public void clicked(int slotId, int button, ClickType clickType, Player player) {
      if (clickType != ClickType.SWAP) {
         super.clicked(slotId, button, clickType, player);
      }
   }

   @Override
   public void removed(Player player) {
      this.save();
      super.removed(player);
   }

   private Slot playerSlot(Inventory inventory, int index, int x, int y) {
      boolean locked = this.hand == InteractionHand.MAIN_HAND && index == inventory.selected;
      if (locked) {
         this.lockedSlot = new LockedSlot(inventory, index, x, y);
         return this.lockedSlot;
      }
      return new Slot(inventory, index, x, y);
   }

   private void save() {
      if (!this.player.level().isClientSide && this.box.getItem() instanceof MaterialBoxItem) {
         MaterialBoxItem.saveContents(this.box, this.storage.getItems(), this.player.registryAccess());
         this.player.getInventory().setChanged();
      }
   }

   private boolean isFilledBox(ItemStack stack) {
      return stack.getItem() instanceof MaterialBoxItem && MaterialBoxItem.hasContents(stack, this.player.registryAccess());
   }

   private static final class StorageSlot extends Slot {
      private final Player player;

      private StorageSlot(SimpleContainer container, int index, int x, int y, Player player) {
         super(container, index, x, y);
         this.player = player;
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return !(stack.getItem() instanceof MaterialBoxItem) || !MaterialBoxItem.hasContents(stack, this.player.registryAccess());
      }
   }

   private static final class LockedSlot extends Slot {
      private LockedSlot(Inventory inventory, int index, int x, int y) {
         super(inventory, index, x, y);
      }

      @Override
      public boolean mayPickup(Player player) {
         return false;
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return false;
      }
   }
}
