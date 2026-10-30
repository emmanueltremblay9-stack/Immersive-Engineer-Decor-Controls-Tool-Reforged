package com.oblixorprime.engineersdecorreforged.menu;

import com.oblixorprime.engineersdecorreforged.ModMenus;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MusliBarPressMenu extends AbstractContainerMenu {
   private static final int PLAYER_SLOTS_START = 2;
   private final Player player;
   private final InteractionHand hand;
   private final ItemStack press;
   private final SimpleContainer storage;
   private int seedsStored;
   private int foodStored;
   private boolean processing;
   private Slot lockedSlot;

   public MusliBarPressMenu(int containerId, Inventory inventory, InteractionHand hand) {
      super(ModMenus.MUSLI_BAR_PRESS.get(), containerId);
      this.player = inventory.player;
      this.hand = hand;
      this.press = this.player.getItemInHand(hand);
      this.storage = new SimpleContainer(2);
      NonNullList<ItemStack> saved = MusliBarPressItem.loadInventory(this.press, this.player.registryAccess());
      this.storage.setItem(0, saved.get(0));
      this.storage.setItem(1, saved.get(1));
      this.seedsStored = MusliBarPressItem.loadSeeds(this.press);
      this.foodStored = MusliBarPressItem.loadFood(this.press);
      this.storage.addListener(ignored -> this.slotsChanged(this.storage));

      this.addSlot(new InputSlot(this.storage, 0, 31, 45, this.player));
      this.addSlot(new OutputSlot(this.storage, 1, 92, 45));
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            int inventorySlot = column + row * 9 + 9;
            this.addSlot(playerSlot(inventory, inventorySlot, 8 + column * 18, 86 + row * 18));
         }
      }
      for (int column = 0; column < 9; column++) {
         this.addSlot(playerSlot(inventory, column, 8 + column * 18, 144));
      }
      this.addDataSlot(new DataSlot() {
         @Override
         public int get() {
            return MusliBarPressMenu.this.seedsStored;
         }

         @Override
         public void set(int value) {
            MusliBarPressMenu.this.seedsStored = value;
         }
      });
      this.addDataSlot(new DataSlot() {
         @Override
         public int get() {
            return MusliBarPressMenu.this.foodStored;
         }

         @Override
         public void set(int value) {
            MusliBarPressMenu.this.foodStored = value;
         }
      });
      if (!this.player.level().isClientSide) {
         this.processInputAndOutput();
      }
   }

   public int seedsStored() {
      return this.seedsStored;
   }

   public int foodStored() {
      return this.foodStored;
   }

   @Override
   public boolean stillValid(Player player) {
      return player == this.player && player.isAlive() && player.getItemInHand(this.hand) == this.press;
   }

   public Slot lockedSlot() {
      return this.lockedSlot;
   }

   @Override
   public void slotsChanged(net.minecraft.world.Container container) {
      if (!this.processing && !this.player.level().isClientSide) {
         this.processInputAndOutput();
      }
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
      if (index < PLAYER_SLOTS_START) {
         if (!this.moveItemStackTo(moving, PLAYER_SLOTS_START, this.slots.size(), true)) {
            return ItemStack.EMPTY;
         }
      } else if ((MusliBarPressItem.isValidSeed(moving) || MusliBarPressItem.isValidFood(moving, this.player))
         && this.moveItemStackTo(moving, 0, 1, false)) {
      } else {
         return ItemStack.EMPTY;
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

   private void processInputAndOutput() {
      this.processing = true;
      try {
         ItemStack input = this.storage.getItem(0);
         if (MusliBarPressItem.isValidSeed(input)) {
            int accepted = Math.min(input.getCount(), MusliBarPressItem.MAX_SEEDS - this.seedsStored);
            this.seedsStored += accepted;
            input.shrink(accepted);
         } else if (MusliBarPressItem.isValidFood(input, this.player)) {
            FoodProperties food = input.getFoodProperties(this.player);
            int nutrition = food == null ? 0 : food.nutrition();
            if (nutrition > 0) {
               int totalFood = this.foodStored + input.getCount() * nutrition;
               if (totalFood > MusliBarPressItem.MAX_FOOD) {
                  input.setCount((totalFood - MusliBarPressItem.MAX_FOOD) / nutrition);
                  this.foodStored = MusliBarPressItem.MAX_FOOD;
               } else {
                  this.foodStored = totalFood;
                  input.setCount(0);
               }
            }
         }
         if (input.isEmpty()) {
            this.storage.setItem(0, ItemStack.EMPTY);
         }

         ItemStack output = this.storage.getItem(1);
         while (this.seedsStored >= MusliBarPressItem.SEEDS_PER_BAR
            && this.foodStored >= MusliBarPressItem.FOOD_PER_BAR
            && (output.isEmpty() || output.is(EngineerToolsModule.MUSLI_BAR.get()))
            && output.getCount() < output.getMaxStackSize()) {
            if (output.isEmpty()) {
               output = new ItemStack(EngineerToolsModule.MUSLI_BAR.get());
               this.storage.setItem(1, output);
            } else {
               output.grow(1);
            }
            this.seedsStored -= MusliBarPressItem.SEEDS_PER_BAR;
            this.foodStored -= MusliBarPressItem.FOOD_PER_BAR;
         }
         this.save();
         this.broadcastChanges();
      } finally {
         this.processing = false;
      }
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
      if (!this.player.level().isClientSide && this.press.getItem() instanceof MusliBarPressItem) {
         MusliBarPressItem.saveState(
            this.press,
            MusliBarPressItem.ListLike.copyOf(this.storage.getItems()),
            this.seedsStored,
            this.foodStored,
            this.player.registryAccess()
         );
         this.player.getInventory().setChanged();
      }
   }

   private static final class InputSlot extends Slot {
      private final Player player;

      private InputSlot(SimpleContainer container, int index, int x, int y, Player player) {
         super(container, index, x, y);
         this.player = player;
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return MusliBarPressItem.isValidSeed(stack) || MusliBarPressItem.isValidFood(stack, this.player);
      }
   }

   private static final class OutputSlot extends Slot {
      private OutputSlot(SimpleContainer container, int index, int x, int y) {
         super(container, index, x, y);
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return false;
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
