package com.oblixorprime.engineersdecorreforged.menu;

import com.oblixorprime.engineersdecorreforged.ModMenus;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.oblixorprime.engineersdecorreforged.utility.MachineLayout;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public class MachineMenu extends AbstractContainerMenu {
   public static final int MACHINE_SLOT_COUNT = 27;
   public static final int DATA_COUNT = 16;
   private static final int SCALED_ENERGY_FIELD = 3;
   private static final int ELECTRICAL_FURNACE_ENERGY_FIELD = 1;
   private static final int LAB_FURNACE_ENERGY_FIELD = 8;
   private static final int ENERGY_FIELD_SCALE = 100;
   private static final int SOLAR_PANEL_ENERGY_CAPACITY = 64000;
   private static final int STANDARD_MACHINE_ENERGY_CAPACITY = 32000;
   private static final int COMPACT_MACHINE_ENERGY_CAPACITY = 16000;
   private static final int DEFAULT_FLUID_CAPACITY = 4000;
   private static final int SMALL_FLUID_FUNNEL_CAPACITY = 3000;
   private static final int TEMPERATURE_PROCESS_TICKS = 180;
   private static final String IE_NAMESPACE = "immersiveengineering";
   private static final TagKey<Item> IE_HAMMERS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("immersiveengineering", "tools/hammers"));
   private static final int METAL_TABLE_HAMMER = 0;
   private static final int METAL_TABLE_GRID_FIRST = 1;
   private static final int METAL_TABLE_GRID_END = 10;
   private static final int METAL_TABLE_OUTPUT = 10;
   private static final int WASTE_QUEUE_START = 0;
   private static final int WASTE_QUEUE_END = 16;
   private static final int PLAYER_INVENTORY_START = 27;
   private static final int PLAYER_INVENTORY_END = 54;
   private static final int HOTBAR_END = 63;
   private final MachineKind kind;
   private final MachineLayout layout;
   private final int visibleMachineSlots;
   private final Container container;
   private final ContainerData data;

   public MachineMenu(int id, Inventory inventory) {
      this(MachineKind.LABELED_CRATE, id, inventory, new SimpleContainer(27), new SimpleContainerData(16));
   }

   public static MachineMenu client(int id, Inventory inventory, MachineKind kind) {
      return new MachineMenu(kind, id, inventory, new SimpleContainer(27), new SimpleContainerData(16));
   }

   public MachineMenu(MachineKind kind, int id, Inventory inventory, Container container, ContainerData data) {
      super((MenuType)resolveMenuType(kind), id);
      checkContainerSize(container, 27);
      checkContainerDataCount(data, 16);
      this.kind = kind;
      this.layout = MachineLayout.forKind(kind);
      this.visibleMachineSlots = this.layout.machineSlots().size();
      this.container = container;
      this.data = data;
      container.startOpen(inventory.player);

      for (MachineLayout.MachineSlot slot : this.layout.machineSlots()) {
         this.addSlot(this.createMachineSlot(slot));
      }

      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column + row * 9 + 9, this.layout.playerInventoryX() + column * 18, this.layout.playerInventoryY() + row * 18));
         }
      }

      for (int column = 0; column < 9; column++) {
         this.addSlot(new Slot(inventory, column, this.layout.hotbarX() + column * 18, this.layout.hotbarY()));
      }

      this.addDataSlots(data);
   }

   private static MenuType<?> resolveMenuType(MachineKind kind) {
      DeferredHolder<MenuType<?>, MenuType<MachineMenu>> menuType = ModMenus.typeFor(kind);
      if (menuType == null) {
         throw new IllegalStateException("No machine menu registered for kind: " + kind);
      }
      return menuType.get();
   }

   public MachineKind kind() {
      return this.kind;
   }

   public MachineLayout layout() {
      return this.layout;
   }

   public int visibleMachineSlots() {
      return this.visibleMachineSlots;
   }

   public int progress() {
      return switch (this.kind) {
         case SMALL_LAB_FURNACE, SMALL_ELECTRICAL_FURNACE -> scaledProgress(this.data.get(2), this.data.get(3));
         case SMALL_BLOCK_BREAKER -> scaledProgress(this.data.get(1), this.data.get(3));
         case SMALL_MINERAL_SMELTER, SMALL_FREEZER -> scaledProgress(this.data.get(1), TEMPERATURE_PROCESS_TICKS);
         default -> clampPercent(this.data.get(1));
      };
   }

   public int fluidAmount() {
      return this.data.get(2);
   }

   public int fluidCapacity() {
      return switch (this.kind) {
         case SMALL_FLUID_FUNNEL -> SMALL_FLUID_FUNNEL_CAPACITY;
         case FLUID_BARREL, PASSIVE_FLUID_ACCUMULATOR, SMALL_MILKING_MACHINE, SMALL_MINERAL_SMELTER -> DEFAULT_FLUID_CAPACITY;
         default -> 0;
      };
   }

   public int energyStored() {
      return switch (this.kind) {
         case SMALL_ELECTRICAL_FURNACE -> this.data.get(ELECTRICAL_FURNACE_ENERGY_FIELD);
         case SMALL_LAB_FURNACE -> this.data.get(LAB_FURNACE_ENERGY_FIELD);
         case SMALL_SOLAR_PANEL, SMALL_WASTE_INCINERATOR, SMALL_MINERAL_SMELTER, SMALL_FREEZER -> this.data.get(SCALED_ENERGY_FIELD) * ENERGY_FIELD_SCALE;
         default -> 0;
      };
   }

   public int energyCapacity() {
      return switch (this.kind) {
         case SMALL_SOLAR_PANEL -> SOLAR_PANEL_ENERGY_CAPACITY;
         case SMALL_WASTE_INCINERATOR, SMALL_MINERAL_SMELTER, SMALL_FREEZER -> COMPACT_MACHINE_ENERGY_CAPACITY;
         case SMALL_LAB_FURNACE, SMALL_ELECTRICAL_FURNACE -> STANDARD_MACHINE_ENERGY_CAPACITY;
         default -> 0;
      };
   }

   public int comparatorOutput() {
      return this.data.get(4);
   }

   public int activityTicks() {
      return this.data.get(5);
   }

   public int field(int index) {
      return index >= 0 && index < this.data.getCount() ? this.data.get(index) : 0;
   }

   public void handleAction(Player player, int action, int valueA, int valueB) {
      if (this.container instanceof MachineBlockEntity machine) {
         machine.handleGuiAction(player, action, valueA, valueB);
      }
   }

   public boolean stillValid(Player player) {
      return this.container.stillValid(player);
   }

   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack result = ItemStack.EMPTY;
      if (index < 0 || index >= this.slots.size()) {
         return result;
      }

      Slot slot = (Slot)this.slots.get(index);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         result = stack.copy();
         int playerInventoryStart = this.visibleMachineSlots;
         int playerInventoryEnd = playerInventoryStart + 27;
         int hotbarEnd = playerInventoryEnd + 9;
         if (index < this.visibleMachineSlots) {
            boolean virtualMetalOutput = this.kind == MachineKind.METAL_CRAFTING_TABLE && slot.getSlotIndex() == METAL_TABLE_OUTPUT;
            if (virtualMetalOutput && !this.canMoveEntireStackToRange(stack, playerInventoryStart, hotbarEnd)) {
               return ItemStack.EMPTY;
            }

            if (!this.moveItemStackTo(stack, playerInventoryStart, hotbarEnd, true)) {
               return ItemStack.EMPTY;
            }

            if (virtualMetalOutput) {
               slot.onTake(player, result);
               return result;
            }
         } else if (!this.movePlayerStackToMachine(player, stack)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         return result;
      } else {
         return result;
      }
   }

   private Slot createMachineSlot(MachineLayout.MachineSlot slot) {
      if (this.kind == MachineKind.METAL_CRAFTING_TABLE) {
         return slot.containerIndex() == 10
            ? new MachineMenu.MetalCraftingResultSlot(this.container, slot.containerIndex(), slot.x(), slot.y())
            : new MachineMenu.MetalCraftingInputSlot(this.container, slot.containerIndex(), slot.x(), slot.y());
      } else {
         return isResultRole(slot.role())
            ? new MachineMenu.OutputMachineSlot(this.container, slot.containerIndex(), slot.x(), slot.y())
            : new MachineMenu.ValidatedMachineSlot(this.container, slot.containerIndex(), slot.x(), slot.y());
      }
   }

   private static boolean isResultRole(String role) {
      String normalized = role.toLowerCase(Locale.ROOT);
      return normalized.contains("output") || normalized.equals("filled") || normalized.equals("milk");
   }

   private boolean movePlayerStackToMachine(Player player, ItemStack stack) {
      return switch (this.kind) {
         case METAL_CRAFTING_TABLE -> isHammer(stack) ? this.moveToMachineContainerSlots(stack, 0) : this.moveToMachineContainerRange(stack, 1, 10);
         case SMALL_LAB_FURNACE -> this.isSmeltable(player, stack)
            ? this.moveToMachineContainerSlots(stack, 0, 3, 4)
            : (AbstractFurnaceBlockEntity.isFuel(stack) ? this.moveToMachineContainerSlots(stack, 1, 5, 6) : this.moveToMachineContainerSlots(stack, 9, 10));
         case SMALL_ELECTRICAL_FURNACE -> this.isSmeltable(player, stack)
            ? this.moveToMachineContainerSlots(stack, 0, 3, 4)
            : this.moveToMachineContainerSlots(stack, 1);
         case FACTORY_DROPPER -> this.moveToMachineContainerRange(stack, 0, 12);
         case SMALL_WASTE_INCINERATOR -> this.moveToMachineContainerRange(stack, WASTE_QUEUE_START, WASTE_QUEUE_END);
         default -> this.moveToVisibleMachineSlots(stack);
      };
   }

   private boolean moveToMachineContainerRange(ItemStack stack, int startInclusive, int endExclusive) {
      boolean moved = false;

      for (int containerSlot = startInclusive; containerSlot < endExclusive && !stack.isEmpty(); containerSlot++) {
         moved |= this.moveToMachineContainerSlot(stack, containerSlot);
      }

      return moved;
   }

   private boolean moveToMachineContainerSlots(ItemStack stack, int... containerSlots) {
      boolean moved = false;

      for (int containerSlot : containerSlots) {
         if (stack.isEmpty()) {
            break;
         }

         moved |= this.moveToMachineContainerSlot(stack, containerSlot);
      }

      return moved;
   }

   private boolean moveToMachineContainerSlot(ItemStack stack, int containerSlot) {
      int menuIndex = this.machineMenuIndexForContainerSlot(containerSlot);
      return menuIndex >= 0 && this.moveToMachineMenuSlot(stack, menuIndex);
   }

   private boolean moveToVisibleMachineSlots(ItemStack stack) {
      boolean moved = false;

      for (int menuIndex = 0; menuIndex < this.visibleMachineSlots && !stack.isEmpty(); menuIndex++) {
         moved |= this.moveToMachineMenuSlot(stack, menuIndex);
      }

      return moved;
   }

   private boolean moveToMachineMenuSlot(ItemStack stack, int menuIndex) {
      Slot target = (Slot)this.slots.get(menuIndex);
      return target.mayPlace(stack) && this.moveItemStackTo(stack, menuIndex, menuIndex + 1, false);
   }

   private boolean canMoveEntireStackToRange(ItemStack stack, int startInclusive, int endExclusive) {
      if (stack.isEmpty()) {
         return true;
      }

      int remaining = stack.getCount();

      for (int index = startInclusive; index < endExclusive && remaining > 0; index++) {
         Slot target = (Slot)this.slots.get(index);
         ItemStack existing = target.getItem();
         if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
            int capacity = Math.min(target.getMaxStackSize(existing), existing.getMaxStackSize());
            remaining -= Math.max(0, capacity - existing.getCount());
         }
      }

      for (int index = startInclusive; index < endExclusive && remaining > 0; index++) {
         Slot target = (Slot)this.slots.get(index);
         if (!target.hasItem() && target.mayPlace(stack)) {
            remaining -= Math.min(target.getMaxStackSize(stack), stack.getMaxStackSize());
         }
      }

      return remaining <= 0;
   }

   private int machineMenuIndexForContainerSlot(int containerSlot) {
      for (int menuIndex = 0; menuIndex < this.visibleMachineSlots; menuIndex++) {
         if (((Slot)this.slots.get(menuIndex)).getSlotIndex() == containerSlot) {
            return menuIndex;
         }
      }

      return -1;
   }

   private static int scaledProgress(int elapsed, int total) {
      return total > 0 && elapsed > 0 ? clampPercent((int)((long)elapsed * 100L / (long)total)) : 0;
   }

   private static int clampPercent(int value) {
      return Math.max(0, Math.min(100, value));
   }

   private boolean isSmeltable(Player player, ItemStack stack) {
      return stack.isEmpty()
         ? false
         : player.level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), player.level()).isPresent();
   }

   private static boolean isHammer(ItemStack stack) {
      return !stack.isEmpty() && stack.is(IE_HAMMERS);
   }

   private static final class MetalCraftingInputSlot extends MachineMenu.ValidatedMachineSlot {
      private final Container container;

      MetalCraftingInputSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
         this.container = container;
      }

      public void setChanged() {
         super.setChanged();
         if (this.container instanceof MachineBlockEntity machine) {
            machine.refreshMetalCraftingResult();
         }
      }
   }

   private static final class MetalCraftingResultSlot extends MachineMenu.OutputMachineSlot {
      private final Container container;

      MetalCraftingResultSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
         this.container = container;
      }

      public void onTake(Player player, ItemStack stack) {
         super.onTake(player, stack);
         if (this.container instanceof MachineBlockEntity machine) {
            machine.consumeMetalCraftingResult();
         }
      }
   }

   private static class OutputMachineSlot extends MachineMenu.ValidatedMachineSlot {
      OutputMachineSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return false;
      }
   }

   private static class ValidatedMachineSlot extends Slot {
      ValidatedMachineSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      public boolean mayPlace(ItemStack stack) {
         return this.container.canPlaceItem(this.getSlotIndex(), stack);
      }
   }
}
