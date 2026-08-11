package com.oblixorprime.engineersdecorreforged.utility;

import blusunrize.immersiveengineering.api.utils.ItemUtils;
import com.oblixorprime.engineersdecorreforged.ModBlockEntities;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;

public class MachineBlockEntity extends BaseContainerBlockEntity {
   private static final int SLOT_COUNT = 27;
   private static final String IE_NAMESPACE = "immersiveengineering";
   private static final TagKey<Item> IE_HAMMERS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("immersiveengineering", "tools/hammers"));
   private static final ResourceLocation IE_FURNACE_HEATER = ResourceLocation.fromNamespaceAndPath("immersiveengineering", "furnace_heater");
   private static final int BUCKET_AMOUNT = 1000;
   private static final int BARREL_CAPACITY = 4000;
   private static final int FUNNEL_CAPACITY = 3000;
   private static final int FUNNEL_TRANSFER = 250;
   private static final int SOLAR_CAPACITY = 64000;
   private static final int SOLAR_ITEM_TRANSFER = 256;
   private static final Direction[] SOLAR_OUTPUT_DIRECTIONS = new Direction[]{
      Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
   };
   private static final int METAL_TABLE_HAMMER = 0;
   private static final int METAL_TABLE_GRID_FIRST = 1;
   private static final int METAL_TABLE_GRID_END = 10;
   private static final int METAL_TABLE_OUTPUT = 10;
   private static final int LAB_INPUT = 0;
   private static final int LAB_FUEL = 1;
   private static final int LAB_OUTPUT = 2;
   private static final int LAB_INPUT_FIFO_0 = 3;
   private static final int LAB_INPUT_FIFO_1 = 4;
   private static final int LAB_FUEL_FIFO_0 = 5;
   private static final int LAB_FUEL_FIFO_1 = 6;
   private static final int LAB_OUTPUT_FIFO_0 = 7;
   private static final int LAB_OUTPUT_FIFO_1 = 8;
   private static final int LAB_AUX_0 = 9;
   private static final int LAB_AUX_1 = 10;
   private static final int LAB_ENERGY_SYNC_FIELD = 8;
   private static final int ELECTRIC_INPUT = 0;
   private static final int ELECTRIC_AUX = 1;
   private static final int ELECTRIC_OUTPUT = 2;
   private static final int ELECTRIC_INPUT_FIFO_0 = 3;
   private static final int ELECTRIC_INPUT_FIFO_1 = 4;
   private static final int ELECTRIC_OUTPUT_FIFO_0 = 5;
   private static final int ELECTRIC_OUTPUT_FIFO_1 = 6;
   private static final int DROPPER_INPUT_SLOTS = 12;
   private static final int DROPPER_FILTER_FIRST = 12;
   private static final int DROPPER_FILTER_SLOTS = 3;
   private static final int WASTE_BURN_SLOT = 15;
   private static final int HOPPER_MAX_TRANSFER_COUNT = 32;
   private static final int HOPPER_MAX_COLLECTION_RANGE = 4;
   private static final int HOPPER_PERIOD_OFFSET = 10;
   private static final int LOGIC_NOT_INVERTED = 0;
   private static final int LOGIC_INVERTED = 1;
   private static final int LOGIC_CONTINUOUS = 2;
   private static final int LOGIC_IGNORE_EXT = 4;
   private static final int TRIGGER_LOGIC_MASK = LOGIC_INVERTED | LOGIC_CONTINUOUS | LOGIC_IGNORE_EXT;
   private static final int DROPPER_MAX_DROP_COUNT = 32;
   private static final int DROPPER_PERIOD_OFFSET = 10;
   private static final int DROPPER_LOGIC_FILTER_ANDGATE = 1;
   private static final int DROPPER_LOGIC_EXTERN_ANDGATE = 2;
   private static final int DROPPER_LOGIC_CONTINUOUS = 16;
   private static final int DROPPER_LOGIC_IGNORE_EXT = 32;
   private static final int DROPPER_LOGIC_MASK = DROPPER_LOGIC_FILTER_ANDGATE | DROPPER_LOGIC_EXTERN_ANDGATE | DROPPER_LOGIC_CONTINUOUS | DROPPER_LOGIC_IGNORE_EXT;
   private static final int ELECTRIC_MAX_ENERGY = 32000;
   private static final int ELECTRIC_HEAT_CAPACITY = 200;
   private static final int ELECTRIC_HEAT_INCREMENT = 20;
   private static final int ELECTRIC_ENERGY_PER_TICK = 16;
   private static final int ELECTRIC_FEEDER_ENERGY = 4;
   private static final int ELECTRIC_MAX_ENERGY_PER_TICK = 64;
   private static final int LAB_HEATER_ENERGY_PER_TICK = 8;
   private static final int MINERAL_SMELTER_ENERGY_PER_TICK = 16;
   private static final int MINERAL_SMELTER_PROCESS_TICKS = 180;
   private static final int FREEZER_PROCESS_TICKS = 180;
   private static final int FREEZER_ENERGY_PER_TICK = 8;
   private static final int FREEZER_BUCKET_ENERGY = 1440;
   public static final int LABEL_LINE_COUNT = 2;
   public static final int LABEL_MAX_LINE_LENGTH = 24;
   public static final int LABEL_MAX_UTF16_UNITS = LABEL_MAX_LINE_LENGTH * 2;
   private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
   private int tickCounter;
   private int fifoTimer;
   private int machineSlotCursor;
   private int progress;
   private int processTimeNeeded;
   private int cooldown;
   private int burnTicks;
   private int fuelBurnTime;
   private int fluidAmount;
   private int energy;
   private String fluidType = "";
   private boolean previousPowered;
   private boolean redstoneUpdated;
   private boolean manualTrigger;
   private boolean manualRedstoneTrigger;
   private int guiSignalFlashTicks;
   private boolean mineralCooling;
   private int hopperRange;
   private int hopperTransferCount = 1;
   private int hopperLogic = 3;
   private int hopperPeriod;
   private int dropperSpeed = 10;
   private int dropperXDeviation;
   private int dropperYDeviation;
   private int dropperNoise;
   private int dropperCount = 1;
   private int dropperLogic = 2;
   private int dropperPeriod;
   private int dropperOpenTimer;
   private int[] dropperFilterMatches = new int[3];
   private int placerLogic = 6;
   private int electricalSpeed = 1;
   private int powerConsumptionField;
   private final String[] labelLines = new String[]{"", ""};
   private final IItemHandler itemHandler = new IItemHandler() {
      public int getSlots() {
         MachineKind kind = MachineBlockEntity.this.kind();
         return kind == null ? 0 : MachineBlockEntity.this.visibleSlotCount(kind);
      }

      public ItemStack getStackInSlot(int slot) {
         return !MachineBlockEntity.this.validAutomationSlot(slot) ? ItemStack.EMPTY : ((ItemStack)MachineBlockEntity.this.items.get(slot)).copy();
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         if (MachineBlockEntity.this.validAutomationSlot(slot) && !stack.isEmpty() && MachineBlockEntity.this.canPlaceItem(slot, stack)) {
            ItemStack existing = (ItemStack)MachineBlockEntity.this.items.get(slot);
            int slotLimit = this.getSlotLimit(slot);
            slotLimit = Math.min(slotLimit, Math.min(stack.getMaxStackSize(), MachineBlockEntity.this.getMaxStackSize(stack)));
            if (!existing.isEmpty()) {
               if (!ItemStack.isSameItemSameComponents(existing, stack)) {
                  return stack;
               }

               slotLimit -= existing.getCount();
            }

            int accepted = Math.min(stack.getCount(), slotLimit);
            if (accepted <= 0) {
               return stack;
            }

            if (!simulate) {
               if (existing.isEmpty()) {
                  MachineBlockEntity.this.items.set(slot, stack.copyWithCount(accepted));
               } else {
                  existing.grow(accepted);
               }

               MachineBlockEntity.this.setChanged();
               MachineBlockEntity.this.updateComparator();
            }

            return stack.getCount() == accepted ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - accepted);
         } else {
            return stack;
         }
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         if (MachineBlockEntity.this.canAutomationExtract(slot) && amount > 0) {
            ItemStack existing = (ItemStack)MachineBlockEntity.this.items.get(slot);
            if (existing.isEmpty()) {
               return ItemStack.EMPTY;
            }

            int extracted = Math.min(amount, Math.min(existing.getCount(), existing.getMaxStackSize()));
            ItemStack result = existing.copyWithCount(extracted);
            if (!simulate) {
               existing.shrink(extracted);
               if (existing.isEmpty()) {
                  MachineBlockEntity.this.items.set(slot, ItemStack.EMPTY);
               }

               MachineBlockEntity.this.setChanged();
               MachineBlockEntity.this.updateComparator();
            }

            return result;
         } else {
            return ItemStack.EMPTY;
         }
      }

      public int getSlotLimit(int slot) {
         if (!MachineBlockEntity.this.validAutomationSlot(slot)) {
            return 0;
         }

         ItemStack existing = (ItemStack)MachineBlockEntity.this.items.get(slot);
         return existing.isEmpty()
            ? MachineBlockEntity.this.getMaxStackSize()
            : Math.min(existing.getMaxStackSize(), MachineBlockEntity.this.getMaxStackSize(existing));
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return MachineBlockEntity.this.validAutomationSlot(slot) && MachineBlockEntity.this.canPlaceItem(slot, stack);
      }
   };
   private final IFluidHandler fluidHandler = new IFluidHandler() {
      public int getTanks() {
         return MachineBlockEntity.this.canHandleFluids(MachineBlockEntity.this.kind()) ? 1 : 0;
      }

      public FluidStack getFluidInTank(int tank) {
         if (tank == 0 && MachineBlockEntity.this.canHandleFluids(MachineBlockEntity.this.kind()) && MachineBlockEntity.this.fluidAmount > 0) {
            Fluid fluid = MachineBlockEntity.this.fluidForType(MachineBlockEntity.this.fluidType);
            return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, MachineBlockEntity.this.fluidAmount);
         } else {
            return FluidStack.EMPTY;
         }
      }

      public int getTankCapacity(int tank) {
         MachineKind kind = MachineBlockEntity.this.kind();
         return tank == 0 && MachineBlockEntity.this.canHandleFluids(kind) ? MachineBlockEntity.this.capacityFor(kind) : 0;
      }

      public boolean isFluidValid(int tank, FluidStack stack) {
         MachineKind kind = MachineBlockEntity.this.kind();
         return tank == 0
            && MachineBlockEntity.this.canHandleFluids(kind)
            && !stack.isEmpty()
            && MachineBlockEntity.this.canStoreFluidType(kind, MachineBlockEntity.this.typeForFluid(stack.getFluid()));
      }

      public int fill(FluidStack resource, FluidAction action) {
         MachineKind kind = MachineBlockEntity.this.kind();
         if (MachineBlockEntity.this.canHandleFluids(kind) && !resource.isEmpty()) {
            String type = MachineBlockEntity.this.typeForFluid(resource.getFluid());
            if (MachineBlockEntity.this.canStoreFluidType(kind, type)
               && (MachineBlockEntity.this.fluidType.isEmpty() || MachineBlockEntity.this.fluidType.equals(type))) {
               int capacity = MachineBlockEntity.this.capacityFor(kind);
               int filled = Math.min(resource.getAmount(), Math.max(0, capacity - MachineBlockEntity.this.fluidAmount));
               if (filled > 0 && action.execute()) {
                  MachineBlockEntity.this.fluidType = type;
                  MachineBlockEntity.this.fluidAmount += filled;
                  MachineBlockEntity.this.syncFluidLevel(MachineBlockEntity.this.propertyForFluidLevel(), capacity);
                  MachineBlockEntity.this.syncMilkingFilled();
                  MachineBlockEntity.this.setChanged();
                  MachineBlockEntity.this.updateComparator();
               }

               return filled;
            } else {
               return 0;
            }
         } else {
            return 0;
         }
      }

      public FluidStack drain(FluidStack resource, FluidAction action) {
         if (!resource.isEmpty() && MachineBlockEntity.this.canHandleFluids(MachineBlockEntity.this.kind()) && MachineBlockEntity.this.fluidAmount > 0) {
            Fluid storedFluid = MachineBlockEntity.this.fluidForType(MachineBlockEntity.this.fluidType);
            return storedFluid != Fluids.EMPTY && resource.is(storedFluid) ? this.drain(resource.getAmount(), action) : FluidStack.EMPTY;
         } else {
            return FluidStack.EMPTY;
         }
      }

      public FluidStack drain(int maxDrain, FluidAction action) {
         MachineKind kind = MachineBlockEntity.this.kind();
         if (MachineBlockEntity.this.canHandleFluids(kind) && maxDrain > 0 && MachineBlockEntity.this.fluidAmount > 0) {
            Fluid storedFluid = MachineBlockEntity.this.fluidForType(MachineBlockEntity.this.fluidType);
            if (storedFluid == Fluids.EMPTY) {
               return FluidStack.EMPTY;
            }

            int drained = Math.min(maxDrain, MachineBlockEntity.this.fluidAmount);
            FluidStack result = new FluidStack(storedFluid, drained);
            if (action.execute()) {
               MachineBlockEntity.this.fluidAmount -= drained;
               if (MachineBlockEntity.this.fluidAmount <= 0) {
                  MachineBlockEntity.this.fluidAmount = 0;
                  MachineBlockEntity.this.fluidType = "";
               }

               MachineBlockEntity.this.syncFluidLevel(MachineBlockEntity.this.propertyForFluidLevel(), MachineBlockEntity.this.capacityFor(kind));
               MachineBlockEntity.this.syncMilkingFilled();
               MachineBlockEntity.this.setChanged();
               MachineBlockEntity.this.updateComparator();
            }

            return result;
         } else {
            return FluidStack.EMPTY;
         }
      }
   };
   private final ContainerData dataAccess = new ContainerData() {
      public int get(int index) {
         return MachineBlockEntity.this.fieldValue(MachineBlockEntity.this.kind(), index);
      }

      public void set(int index, int value) {
         MachineBlockEntity.this.setFieldValue(MachineBlockEntity.this.kind(), index, value);
      }

      public int getCount() {
         return 16;
      }
   };
   private final IEnergyStorage energyHandler = new IEnergyStorage() {
      public int receiveEnergy(int maxReceive, boolean simulate) {
         MachineKind kind = MachineBlockEntity.this.kind();
         if (MachineBlockEntity.this.canReceiveEnergy(kind) && maxReceive > 0) {
            int capacity = MachineBlockEntity.this.energyCapacity(kind);
            int received = Math.min(maxReceive, Math.max(0, capacity - MachineBlockEntity.this.energy));
            if (!simulate && received > 0) {
               MachineBlockEntity.this.energy += received;
               MachineBlockEntity.this.setChanged();
            }

            return received;
         } else {
            return 0;
         }
      }

      public int extractEnergy(int maxExtract, boolean simulate) {
         MachineKind kind = MachineBlockEntity.this.kind();
         if (kind == MachineKind.SMALL_SOLAR_PANEL && maxExtract > 0) {
            int extracted = Math.min(maxExtract, Math.max(0, MachineBlockEntity.this.energy));
            if (!simulate && extracted > 0) {
               MachineBlockEntity.this.energy -= extracted;
               MachineBlockEntity.this.setChanged();
               MachineBlockEntity.this.updateComparator();
            }

            return extracted;
         } else {
            return 0;
         }
      }

      public int getEnergyStored() {
         return MachineBlockEntity.this.energy;
      }

      public int getMaxEnergyStored() {
         return MachineBlockEntity.this.energyCapacity(MachineBlockEntity.this.kind());
      }

      public boolean canExtract() {
         return MachineBlockEntity.this.kind() == MachineKind.SMALL_SOLAR_PANEL;
      }

      public boolean canReceive() {
         return MachineBlockEntity.this.canReceiveEnergy(MachineBlockEntity.this.kind());
      }
   };

   public MachineBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.MACHINE.get(), pos, state);
   }

   public List<String> labelLines() {
      return List.of(this.labelLines[0], this.labelLines[1]);
   }

   public String labelLine(int index) {
      return index >= 0 && index < 2 ? this.labelLines[index] : "";
   }

   public void setLabeledCrateLabel(String line0, String line1) {
      String sanitized0 = sanitizeLabelLine(line0);
      String sanitized1 = sanitizeLabelLine(line1);
      if (!sanitized0.equals(this.labelLines[0]) || !sanitized1.equals(this.labelLines[1])) {
         this.labelLines[0] = sanitized0;
         this.labelLines[1] = sanitized1;
         this.setChanged();
         if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
            if (!this.level.isClientSide && this.level instanceof ServerLevel serverLevel) {
               serverLevel.getChunkSource().blockChanged(this.worldPosition);
            }
         }
      }
   }

   public static String sanitizeLabelLine(String value) {
      if (value != null && !value.isBlank()) {
         StringBuilder builder = new StringBuilder();
         value.codePoints().filter(codePoint -> !Character.isISOControl(codePoint)).limit(24L).forEach(builder::appendCodePoint);
         return builder.toString().trim();
      } else {
         return "";
      }
   }

   public IItemHandler itemHandler(Direction side) {
      return this.itemHandler;
   }

   public IFluidHandler fluidHandler(Direction side) {
      return this.canHandleFluids(this.kind()) ? this.fluidHandler : null;
   }

   public IEnergyStorage energyStorage(Direction side) {
      MachineKind kind = this.kind();
      return kind != MachineKind.SMALL_ELECTRICAL_FURNACE
            && kind != MachineKind.SMALL_WASTE_INCINERATOR
            && kind != MachineKind.SMALL_SOLAR_PANEL
            && kind != MachineKind.SMALL_MINERAL_SMELTER
            && kind != MachineKind.SMALL_FREEZER
            && kind != MachineKind.SMALL_LAB_FURNACE
         ? null
         : this.energyHandler;
   }

   public ContainerData dataAccessForTests() {
      return this.dataAccess;
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
      MachineKind kind = machine.kind();
      if (kind != null) {
         machine.tickCounter++;
         if (machine.cooldown > 0) {
            machine.cooldown--;
         }

         if (machine.burnTicks > 0) {
            machine.burnTicks--;
         }

         if (machine.guiSignalFlashTicks > 0) {
            machine.guiSignalFlashTicks--;
         }

         boolean powered = level.hasNeighborSignal(pos);
         boolean powerChanged = powered != machine.previousPowered;
         if (powerChanged) {
            machine.redstoneUpdated = true;
         }

         machine.previousPowered = powered;
         switch (kind) {
            case METAL_CRAFTING_TABLE:
               machine.tickMetalCraftingTable();
               break;
            case FACTORY_HOPPER:
               machine.tickFactoryHopper(powered);
               break;
            case FACTORY_DROPPER:
               machine.tickFactoryDropper(powered);
               break;
            case FACTORY_PLACER:
               machine.tickFactoryPlacer(powered);
               break;
            case SMALL_BLOCK_BREAKER:
               machine.tickBlockBreaker(powered);
               break;
            case SMALL_TREE_CUTTER:
               machine.tickTreeCutter(powered);
               break;
            case SMALL_WASTE_INCINERATOR:
               machine.tickIncinerator();
               break;
            case SMALL_LAB_FURNACE:
               machine.tickLabFurnace();
               break;
            case SMALL_ELECTRICAL_FURNACE:
               machine.tickElectricalFurnace();
               break;
            case SMALL_MINERAL_SMELTER:
               machine.tickMineralSmelter(powered);
               break;
            case SMALL_FREEZER:
               machine.tickFreezer(powered);
               break;
            case FLUID_BARREL:
               machine.tickFluidBarrel();
               machine.tickFluidBucketSlots(kind);
               machine.syncFluidLevel(MachineBlocks.LEVEL_0_4, 4000);
               break;
            case SMALL_FLUID_FUNNEL:
               machine.tickFluidFunnel();
               machine.tickFluidBucketSlots(kind);
               break;
            case PASSIVE_FLUID_ACCUMULATOR:
               machine.tickFluidAccumulator();
               machine.tickFluidBucketSlots(kind);
               break;
            case SMALL_SOLAR_PANEL:
               machine.tickSolarPanel();
               break;
            case SMALL_MILKING_MACHINE:
               machine.tickFluidBucketSlots(kind);
               machine.tickMilkingMachine(powered);
            case LABELED_CRATE:
         }
      }
   }

   public MachineKind kind() {
      return this.getBlockState().getBlock() instanceof MachineBlocks.MachineBlock machineBlock ? machineBlock.kind() : null;
   }

   public boolean handleItemUse(ItemStack stack, Player player, InteractionHand hand) {
      MachineKind kind = this.kind();
      if (this.level != null && kind != null && !this.level.isClientSide && !stack.isEmpty()) {
         boolean changed = false;
         if (kind.isFluidContainer()) {
            changed = this.handleFluidBucket(stack, player, hand, this.capacityFor(kind));
         } else if (kind == MachineKind.SMALL_FREEZER && stack.is(Items.WATER_BUCKET)) {
            if (this.energy < 1440) {
               return false;
            }

            ItemStack result = new ItemStack(Items.ICE);
            changed = this.canInsertIntoSlots(result, 2)
               && this.consumeHeldBucket(stack, player, hand, new ItemStack(Items.BUCKET))
               && this.insertIntoSlots(result, 2);
            if (changed) {
               this.energy -= 1440;
            }

            this.stopFreezerProcess();
         }

         if (changed) {
            this.setChanged();
            this.updateComparator();
         }

         return changed;
      } else {
         return false;
      }
   }

   public boolean canPreviewItemUse(ItemStack stack) {
      MachineKind kind = this.kind();
      if (kind == null || stack.isEmpty()) {
         return false;
      } else {
         return kind.isFluidContainer()
            ? this.canHandleFluidBucket(stack, this.capacityFor(kind))
            : kind == MachineKind.SMALL_FREEZER && stack.is(Items.WATER_BUCKET) && this.energy >= 1440 && this.canInsertIntoSlots(new ItemStack(Items.ICE), 2);
      }
   }

   public int comparatorOutput() {
      MachineKind kind = this.kind();
      if (kind == MachineKind.SMALL_SOLAR_PANEL) {
         return this.scaleToComparator(this.energy, 64000);
      } else if (kind == MachineKind.SMALL_MINERAL_SMELTER) {
         return this.scaleToComparator(this.propertyValue(MachineBlocks.PHASE_0_3), 3);
      } else if (kind == MachineKind.SMALL_FREEZER) {
         return this.scaleToComparator(this.propertyValue(MachineBlocks.PHASE_0_4), 4);
      } else {
         return kind != null && kind.isFluidContainer()
            ? this.scaleToComparator(this.fluidAmount, this.capacityFor(kind))
            : AbstractContainerMenu.getRedstoneSignalFromContainer(this);
      }
   }

   public int getContainerSize() {
      return this.items.size();
   }

   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   protected void setItems(NonNullList<ItemStack> items) {
      this.items = items;
   }

   protected Component getDefaultName() {
      MachineKind kind = this.kind();
      String key = kind == null ? "labeled_crate" : kind.registryName();
      return Component.translatable("block.immersive_engineer_decor_controls_tool_reforged." + key);
   }

   protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
      MachineKind kind = this.kind();
      return new MachineMenu(kind == null ? MachineKind.LABELED_CRATE : kind, id, inventory, this, this.dataAccess);
   }

   public boolean canPlaceItem(int index, ItemStack stack) {
      MachineKind kind = this.kind();
      if (kind == null || index < 0 || index >= this.visibleSlotCount(kind)) {
         return false;
      }

      if (isLayoutResultSlot(kind, index)) {
         return false;
      }

      return switch (kind) {
         case METAL_CRAFTING_TABLE -> {
            switch (index) {
               case 0:
                  yield isHammer(stack);
               case 10:
                  yield false;
               default:
                  yield index >= 1 && index < 10;
            }
         }
         default -> true;
         case FACTORY_PLACER -> isFactoryPlacerItem(stack);
         case SMALL_WASTE_INCINERATOR -> index <= WASTE_BURN_SLOT;
         case SMALL_LAB_FURNACE -> {
            switch (index) {
               case 0:
               case 3:
               case 4:
                  yield this.canSmelt(stack);
               case 1:
               case 5:
               case 6:
                  yield this.isFuel(stack);
               case 2:
               case 7:
               case 8:
                  yield false;
               default:
                  yield true;
            }
         }
         case SMALL_ELECTRICAL_FURNACE -> {
            switch (index) {
               case 0:
               case 3:
               case 4:
                  yield this.canSmelt(stack);
               case 1:
                  yield stack.is(Items.HOPPER);
               case 2:
               case 5:
               case 6:
                  yield false;
               default:
                  yield false;
            }
         }
         case SMALL_MINERAL_SMELTER -> {
            switch (index) {
               case 0:
                  yield this.isMineralInput(stack);
               default:
                  yield false;
            }
         }
         case SMALL_FREEZER -> {
            switch (index) {
               case 0:
                  yield !this.freezerResult(stack).isEmpty();
               default:
                  yield false;
            }
         }
         case FLUID_BARREL, SMALL_FLUID_FUNNEL, PASSIVE_FLUID_ACCUMULATOR -> {
            switch (index) {
               case 0:
                  yield stack.is(Items.BUCKET);
               default:
                  yield false;
            }
         }
         case SMALL_SOLAR_PANEL -> isEnergyItem(stack);
         case SMALL_MILKING_MACHINE -> {
            switch (index) {
                case 0:
                   yield stack.is(Items.BUCKET);
                default:
                   yield false;
             }
         }
      };
   }

   private static boolean isLayoutResultSlot(MachineKind kind, int index) {
      return MachineLayout.forKind(kind).machineSlots().stream().anyMatch(slot -> slot.containerIndex() == index && isResultRole(slot.role()));
   }

   private static boolean hasLayoutResultSlots(MachineKind kind) {
      return MachineLayout.forKind(kind).machineSlots().stream().anyMatch(slot -> isResultRole(slot.role()));
   }

   private static boolean isResultRole(String role) {
      String normalized = role.toLowerCase(Locale.ROOT);
      return normalized.contains("output") || normalized.equals("filled") || normalized.equals("milk");
   }

   public void handleGuiAction(Player player, int action, int valueA, int valueB) {
      MachineKind kind = this.kind();
      if (kind != null && this.level != null && !this.level.isClientSide) {
         switch (action) {
            case 1:
               if (kind == MachineKind.FACTORY_HOPPER) {
                  this.hopperRange = Mth.clamp(valueA, 0, 4);
               }
               break;
            case 2:
               if (kind == MachineKind.FACTORY_HOPPER) {
                  this.hopperTransferCount = Mth.clamp(valueA, 1, 32);
               }
               break;
            case 3:
               if (kind == MachineKind.FACTORY_HOPPER) {
                  this.hopperLogic = sanitizeTriggerLogic(valueA);
               }
               break;
            case 4:
               if (kind == MachineKind.FACTORY_HOPPER) {
                  this.hopperPeriod = Mth.clamp(valueA, 0, 100);
               }
               break;
            case 5:
               if (kind == MachineKind.FACTORY_HOPPER) {
                  this.manualTrigger = true;
                  this.guiSignalFlashTicks = 10;
                  this.cooldown = Math.min(this.cooldown, 1);
               }
               break;
            case 6:
            case 7:
            case 8:
            case 9:
            case 17:
            case 18:
            case 19:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            default:
               return;
            case 10:
               if (kind == MachineKind.FACTORY_DROPPER) {
                  this.dropperSpeed = Mth.clamp(valueA, 0, 100);
               }
               break;
            case 11:
               if (kind == MachineKind.FACTORY_DROPPER) {
                  this.dropperXDeviation = Mth.clamp(valueA, -100, 100);
                  this.dropperYDeviation = Mth.clamp(valueB, -100, 100);
               }
               break;
            case 12:
               if (kind == MachineKind.FACTORY_DROPPER) {
                  this.dropperCount = Mth.clamp(valueA, 1, 32);
               }
               break;
            case 13:
               if (kind == MachineKind.FACTORY_DROPPER) {
                  this.dropperPeriod = Mth.clamp(valueA, 0, 100);
               }
               break;
            case 14:
               if (kind == MachineKind.FACTORY_DROPPER) {
                  this.dropperLogic = sanitizeDropperLogic(valueA);
               }
               break;
            case 15:
               if (kind == MachineKind.FACTORY_DROPPER) {
                  this.manualRedstoneTrigger = true;
                  this.guiSignalFlashTicks = 10;
               }
               break;
            case 16:
               if (kind == MachineKind.FACTORY_DROPPER) {
                  this.manualTrigger = true;
                  this.cooldown = Math.min(this.cooldown, 1);
               }
               break;
            case 20:
               if (kind == MachineKind.FACTORY_PLACER) {
                  this.placerLogic = sanitizeTriggerLogic(valueA);
               }
               break;
            case 21:
               if (kind == MachineKind.FACTORY_PLACER) {
                  this.manualTrigger = true;
                  this.guiSignalFlashTicks = 10;
                  this.cooldown = Math.min(this.cooldown, 1);
               }
               break;
            case 22:
               if (kind == MachineKind.FACTORY_PLACER) {
                  int row = Mth.clamp(valueA, 0, 2);
                  this.machineSlotCursor = Mth.clamp(row * 6, 0, this.visibleSlotCount(kind) - 1);
               }
               break;
            case 30:
               if (kind == MachineKind.SMALL_ELECTRICAL_FURNACE) {
                  this.electricalSpeed = Mth.clamp(valueA, 0, 3);
               }
         }

         this.setChanged();
      }
   }

   private int fieldValue(MachineKind kind, int index) {
      if (kind == null) {
         return 0;
      }

      return switch (kind) {
         case FACTORY_HOPPER -> {
            switch (index) {
               case 0:
                  yield this.hopperRange;
               case 1:
                  yield this.hopperTransferCount;
               case 2:
                  yield this.hopperLogic;
               case 3:
                  yield this.hopperPeriod;
               case 4:
                  yield this.cooldown;
               case 5:
                  yield !this.previousPowered && this.guiSignalFlashTicks <= 0 ? 0 : 1;
               case 6:
                  yield Mth.clamp(this.machineSlotCursor, 0, 17);
               default:
                  yield 0;
            }
         }
         case FACTORY_DROPPER -> {
            switch (index) {
               case 0:
                  yield this.dropperSpeed;
               case 1:
                  yield this.dropperXDeviation;
               case 2:
                  yield this.dropperYDeviation;
               case 3:
                  yield this.dropperNoise;
               case 4:
                  yield this.dropperCount;
               case 5:
                  yield this.dropperLogic;
               case 6:
                  yield this.dropperPeriod;
               case 7:
               case 8:
               default:
                  yield 0;
               case 9:
                  yield this.cooldown;
               case 10:
                  yield this.dropperOpenTimer;
               case 11:
                  yield !this.previousPowered && this.guiSignalFlashTicks <= 0 ? 0 : 1;
               case 12:
                  yield this.dropperFilterMatches[0];
               case 13:
                  yield this.dropperFilterMatches[1];
               case 14:
                  yield this.dropperFilterMatches[2];
               case 15:
                  yield Mth.clamp(this.machineSlotCursor, 0, 11);
            }
         }
         case FACTORY_PLACER -> {
            switch (index) {
               case 0:
                  yield this.placerLogic;
               case 1:
                  yield !this.previousPowered && this.guiSignalFlashTicks <= 0 ? 0 : 1;
               case 2:
                  yield Mth.clamp(this.machineSlotCursor, 0, this.visibleSlotCount(kind) - 1);
               default:
                  yield 0;
            }
         }
         case SMALL_BLOCK_BREAKER -> {
            switch (index) {
               case 0:
                  yield kind.ordinal();
               case 1:
                  yield this.progress;
               case 3:
                  yield this.processTimeNeeded;
               case 4:
                  yield this.comparatorOutput();
               case 5:
                  yield this.cooldown;
               default:
                  yield 0;
            }
         }
         default -> {
            switch (index) {
               case 0:
                  yield kind.ordinal();
               case 1:
                  yield this.progress;
               case 2:
                  yield this.fluidAmount;
               case 3:
                  yield this.energy / 100;
               case 4:
                  yield this.comparatorOutput();
               case 5:
                  yield Math.max(this.cooldown, this.burnTicks);
               default:
                  yield 0;
            }
         }
         case SMALL_LAB_FURNACE -> {
            switch (index) {
               case 0:
                  yield this.burnTicks;
               case 1:
                  yield this.fuelBurnTime;
               case 2:
                  yield this.progress;
               case 3:
                  yield this.processTimeNeeded;
               case 4:
                  yield this.burnTicks > 0 ? 1 : 0;
               case LAB_ENERGY_SYNC_FIELD:
                  yield this.energy;
               default:
                  yield 0;
            }
         }
         case SMALL_ELECTRICAL_FURNACE -> {
            switch (index) {
                case 0:
                   yield this.burnTicks;
                case 1:
                   yield this.energy;
                case 2:
                   yield this.progress;
                case 3:
                   yield this.processTimeNeeded;
                case 4:
                   yield this.electricalSpeed;
                case 5:
                   yield this.energyCapacity(kind);
                case 6:
                   yield this.burnTicks > 0 ? 1 : 0;
                case 7:
                   yield this.powerConsumptionField;
                default:
                   yield 0;
             }
         }
      };
   }

   private void setFieldValue(MachineKind kind, int index, int value) {
      if (kind != null) {
         switch (kind) {
            case FACTORY_HOPPER:
               switch (index) {
                  case 0:
                     this.hopperRange = Mth.clamp(value, 0, 4);
                     return;
                  case 1:
                     this.hopperTransferCount = Mth.clamp(value, 1, 32);
                     return;
                  case 2:
                     this.hopperLogic = sanitizeTriggerLogic(value);
                     return;
                  case 3:
                     this.hopperPeriod = Mth.clamp(value, 0, 100);
                     return;
                  case 4:
                     this.cooldown = Mth.clamp(value, 0, 400);
                     return;
                  case 5:
                     this.previousPowered = value != 0;
                     return;
                  case 6:
                     this.machineSlotCursor = Mth.clamp(value, 0, this.visibleSlotCount(kind) - 1);
                     return;
                  default:
                     return;
               }
            case FACTORY_DROPPER:
               switch (index) {
                  case 0:
                     this.dropperSpeed = Mth.clamp(value, 0, 100);
                     return;
                  case 1:
                     this.dropperXDeviation = Mth.clamp(value, -100, 100);
                     return;
                  case 2:
                     this.dropperYDeviation = Mth.clamp(value, -100, 100);
                     return;
                  case 3:
                     this.dropperNoise = Mth.clamp(value, 0, 100);
                     return;
                  case 4:
                     this.dropperCount = Mth.clamp(value, 1, 32);
                     return;
                  case 5:
                     this.dropperLogic = sanitizeDropperLogic(value);
                     return;
                  case 6:
                     this.dropperPeriod = Mth.clamp(value, 0, 100);
                     return;
                  case 7:
                  case 8:
                  default:
                     return;
                  case 9:
                     this.cooldown = Mth.clamp(value, 0, 400);
                     return;
                  case 10:
                     this.dropperOpenTimer = Mth.clamp(value, 0, 400);
                     return;
                  case 11:
                     this.previousPowered = value != 0;
                     return;
                  case 12:
                     this.dropperFilterMatches[0] = Mth.clamp(value, 0, 2);
                     return;
                  case 13:
                     this.dropperFilterMatches[1] = Mth.clamp(value, 0, 2);
                     return;
                  case 14:
                     this.dropperFilterMatches[2] = Mth.clamp(value, 0, 2);
                     return;
                  case 15:
                     this.machineSlotCursor = Mth.clamp(value, 0, 11);
                     return;
               }
            case FACTORY_PLACER:
               switch (index) {
                  case 0:
                     this.placerLogic = sanitizeTriggerLogic(value);
                     return;
                  case 1:
                     this.previousPowered = value != 0;
                     return;
                  case 2:
                     this.machineSlotCursor = Mth.clamp(value, 0, this.visibleSlotCount(kind) - 1);
                     return;
                  default:
                     return;
               }
            case SMALL_TREE_CUTTER:
            case SMALL_WASTE_INCINERATOR:
            default:
               switch (index) {
                  case 1:
                     this.progress = value;
                     return;
                  case 2:
                     this.fluidAmount = value;
                     return;
                  case 3:
                     this.energy = value * 100;
                     return;
                  case 4:
                  default:
                     return;
                  case 5:
                     this.cooldown = value;
                     return;
               }
            case SMALL_BLOCK_BREAKER:
               switch (index) {
                  case 1:
                     this.progress = value;
                     return;
                  case 3:
                     this.processTimeNeeded = Math.max(0, value);
                     return;
                  case 5:
                     this.cooldown = value;
                     return;
                  default:
                     return;
               }
            case SMALL_LAB_FURNACE:
               switch (index) {
                  case 0:
                     this.burnTicks = value;
                     return;
                  case 1:
                     this.fuelBurnTime = value;
                     return;
                  case 2:
                     this.progress = value;
                     return;
                  case 3:
                     this.processTimeNeeded = value;
                     return;
                  case LAB_ENERGY_SYNC_FIELD:
                     this.energy = Mth.clamp(value, 0, this.energyCapacity(kind));
                     return;
                  default:
                     return;
               }
            case SMALL_ELECTRICAL_FURNACE:
               switch (index) {
                  case 0:
                     this.burnTicks = value;
                     break;
                  case 1:
                     this.energy = Mth.clamp(value, 0, this.energyCapacity(kind));
                     break;
                  case 2:
                     this.progress = value;
                     break;
                  case 3:
                     this.processTimeNeeded = value;
                     break;
                  case 4:
                     this.electricalSpeed = Mth.clamp(value, 0, 3);
                  case 5:
                  case 6:
                  default:
                     break;
                  case 7:
                     this.setPowerConsumptionField(value);
               }
         }
      }
   }

   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.items = NonNullList.withSize(27, ItemStack.EMPTY);
      ContainerHelper.loadAllItems(tag, this.items, registries);
      if (this.kind() == MachineKind.METAL_CRAFTING_TABLE) {
         this.items.set(10, ItemStack.EMPTY);
      }

      this.tickCounter = tag.getInt("TickCounter");
      this.fifoTimer = tag.getInt("FifoTimer");
      this.machineSlotCursor = tag.getInt("MachineSlotCursor");
      this.progress = tag.getInt("Progress");
      this.processTimeNeeded = tag.getInt("ProcessTimeNeeded");
      this.cooldown = tag.getInt("Cooldown");
      this.burnTicks = tag.getInt("BurnTicks");
      this.fuelBurnTime = tag.getInt("FuelBurnTime");
      this.fluidAmount = tag.getInt("FluidAmount");
      this.energy = tag.getInt("Energy");
      this.fluidType = tag.getString("FluidType");
      this.previousPowered = tag.getBoolean("PreviousPowered");
      this.redstoneUpdated = tag.getBoolean("RedstoneUpdated");
      this.manualTrigger = tag.getBoolean("ManualTrigger");
      this.manualRedstoneTrigger = tag.getBoolean("ManualRedstoneTrigger");
      this.mineralCooling = tag.getBoolean("MineralCooling");
      this.hopperRange = Mth.clamp(tag.getInt("HopperRange"), 0, 4);
      this.hopperTransferCount = Mth.clamp(tag.getInt("HopperTransferCount"), 1, 32);
      this.hopperLogic = sanitizeTriggerLogic(savedIntOrDefault(tag, "HopperLogic", 3));
      this.hopperPeriod = Mth.clamp(tag.getInt("HopperPeriod"), 0, 100);
      this.dropperSpeed = Mth.clamp(savedIntOrDefault(tag, "DropperSpeed", 10), 0, 100);
      this.dropperXDeviation = Mth.clamp(tag.getInt("DropperXDeviation"), -100, 100);
      this.dropperYDeviation = Mth.clamp(tag.getInt("DropperYDeviation"), -100, 100);
      this.dropperNoise = Mth.clamp(tag.getInt("DropperNoise"), 0, 100);
      this.dropperCount = Mth.clamp(savedIntOrDefault(tag, "DropperCount", 1), 1, 32);
      this.dropperLogic = sanitizeDropperLogic(savedIntOrDefault(tag, "DropperLogic", 2));
      this.dropperPeriod = Mth.clamp(tag.getInt("DropperPeriod"), 0, 100);
      this.dropperOpenTimer = Mth.clamp(tag.getInt("DropperOpenTimer"), 0, 400);

      for (int i = 0; i < this.dropperFilterMatches.length; i++) {
         this.dropperFilterMatches[i] = Mth.clamp(tag.getInt("DropperFilterMatch" + i), 0, 2);
      }

      this.placerLogic = sanitizeTriggerLogic(savedIntOrDefault(tag, "PlacerLogic", 6));
      this.electricalSpeed = Mth.clamp(savedIntOrDefault(tag, "ElectricalSpeed", 1), 0, 3);
      this.powerConsumptionField = tag.getInt("PowerConsumptionField");

      for (int i = 0; i < 2; i++) {
         this.labelLines[i] = sanitizeLabelLine(tag.getString("LabelLine" + i));
      }

      this.normalizeLoadedState();
   }

   private static int savedIntOrDefault(CompoundTag tag, String key, int defaultValue) {
      return tag.contains(key, Tag.TAG_INT) ? tag.getInt(key) : defaultValue;
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      if (this.kind() == MachineKind.METAL_CRAFTING_TABLE) {
         ItemStack result = (ItemStack)this.items.get(10);
         this.items.set(10, ItemStack.EMPTY);
         ContainerHelper.saveAllItems(tag, this.items, registries);
         this.items.set(10, result);
      } else {
         ContainerHelper.saveAllItems(tag, this.items, registries);
      }

      tag.putInt("TickCounter", this.tickCounter);
      tag.putInt("FifoTimer", this.fifoTimer);
      tag.putInt("MachineSlotCursor", this.machineSlotCursor);
      tag.putInt("Progress", this.progress);
      tag.putInt("ProcessTimeNeeded", this.processTimeNeeded);
      tag.putInt("Cooldown", this.cooldown);
      tag.putInt("BurnTicks", this.burnTicks);
      tag.putInt("FuelBurnTime", this.fuelBurnTime);
      tag.putInt("FluidAmount", this.fluidAmount);
      tag.putInt("Energy", this.energy);
      tag.putString("FluidType", this.fluidType);
      tag.putBoolean("PreviousPowered", this.previousPowered);
      tag.putBoolean("RedstoneUpdated", this.redstoneUpdated);
      tag.putBoolean("ManualTrigger", this.manualTrigger);
      tag.putBoolean("ManualRedstoneTrigger", this.manualRedstoneTrigger);
      tag.putBoolean("MineralCooling", this.mineralCooling);
      tag.putInt("HopperRange", this.hopperRange);
      tag.putInt("HopperTransferCount", this.hopperTransferCount);
      tag.putInt("HopperLogic", this.hopperLogic);
      tag.putInt("HopperPeriod", this.hopperPeriod);
      tag.putInt("DropperSpeed", this.dropperSpeed);
      tag.putInt("DropperXDeviation", this.dropperXDeviation);
      tag.putInt("DropperYDeviation", this.dropperYDeviation);
      tag.putInt("DropperNoise", this.dropperNoise);
      tag.putInt("DropperCount", this.dropperCount);
      tag.putInt("DropperLogic", this.dropperLogic);
      tag.putInt("DropperPeriod", this.dropperPeriod);
      tag.putInt("DropperOpenTimer", this.dropperOpenTimer);

      for (int i = 0; i < this.dropperFilterMatches.length; i++) {
         tag.putInt("DropperFilterMatch" + i, this.dropperFilterMatches[i]);
      }

      tag.putInt("PlacerLogic", this.placerLogic);
      tag.putInt("ElectricalSpeed", this.electricalSpeed);
      tag.putInt("PowerConsumptionField", this.powerConsumptionField);

      for (int i = 0; i < 2; i++) {
         tag.putString("LabelLine" + i, this.labelLines[i]);
      }
   }

   public CompoundTag getUpdateTag(Provider registries) {
      CompoundTag tag = super.getUpdateTag(registries);
      this.saveAdditional(tag, registries);
      return tag;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   private void tickFactoryHopper(boolean powered) {
      if (this.level != null && this.tickCounter % 10 == 0) {
         boolean redstoneAllowed = this.logicAllowsTrigger(this.hopperLogic, powered);
         boolean pulseMode = (this.hopperLogic & 6) == 0;
         boolean manual = this.manualTrigger;
         boolean trigger = (this.hopperLogic & 4) != 0 || manual || redstoneAllowed && (this.redstoneUpdated || !pulseMode);
         this.manualTrigger = false;
         this.redstoneUpdated = false;
         Direction direction = this.facing(this.getBlockState());
         if (trigger) {
            this.collectNearbyItems(direction);
         }

         if (this.cooldown <= 0) {
            if (trigger) {
               this.cooldown = 10 + this.hopperPeriod * 2;
               this.pushItems(direction, this.hopperTransferCount);
            }
         }
      }
   }

   private void tickFactoryDropper(boolean powered) {
      if (this.level != null) {
         if (this.dropperOpenTimer > 0) {
            this.dropperOpenTimer--;
         }

         if (this.dropperOpenTimer <= 0) {
            this.setBooleanProperty(MachineBlocks.OPEN, false);
         }

         if (this.tickCounter % 4 == 0) {
            this.updateDropperFilters();
            boolean continuousMode = (this.dropperLogic & 16) != 0;
            boolean redstoneTrigger = this.manualRedstoneTrigger || powered && (this.redstoneUpdated || continuousMode) || (this.dropperLogic & 32) != 0;
            boolean filterDefined = false;
            boolean filterTrigger = false;
            int matched = 0;
            int defined = 0;

            for (int filter : this.dropperFilterMatches) {
               if (filter > 0) {
                  defined++;
                  filterDefined = true;
               }

               if (filter > 1) {
                  matched++;
               }
            }

            if (filterDefined) {
               filterTrigger = matched > 0;
               if ((this.dropperLogic & 1) != 0 && matched != defined) {
                  filterTrigger = false;
               }
            }

            boolean trigger = this.manualTrigger
               || (filterDefined ? ((this.dropperLogic & 2) != 0 ? filterTrigger && redstoneTrigger : filterTrigger || redstoneTrigger) : redstoneTrigger);
            this.manualTrigger = false;
            this.manualRedstoneTrigger = false;
            this.redstoneUpdated = false;
            if (this.hasDropperCandidate(filterTrigger) && (trigger || filterTrigger || redstoneTrigger)) {
               this.dropperOpenTimer = 40;
               this.setBooleanProperty(MachineBlocks.OPEN, true);
            }

            if (trigger && this.cooldown <= 0) {
               Direction direction = this.facing(this.getBlockState());
               boolean dropped = filterTrigger ? this.dropFilteredStacks(direction) : this.dropRoundRobinStack(direction);
               if (dropped) {
                  this.cooldown = 10 + this.dropperPeriod * 2;
                  this.dropperOpenTimer = 40;
                  this.setBooleanProperty(MachineBlocks.OPEN, true);
                  this.level.playSound(null, this.worldPosition, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.1F, 4.0F);
                  this.setChanged();
                  this.updateComparator();
               }
            }
         }
      }
   }

   private void tickFactoryPlacer(boolean powered) {
      if (this.level != null && this.tickCounter % 10 == 0) {
         boolean redstoneAllowed = this.logicAllowsTrigger(this.placerLogic, powered);
         boolean continuousMode = (this.placerLogic & 2) != 0;
         boolean trigger = (this.placerLogic & 4) != 0 || this.manualTrigger || redstoneAllowed && (this.redstoneUpdated || continuousMode);
         boolean triggeredByManual = this.manualTrigger;
         boolean triggeredByEdge = redstoneAllowed && this.redstoneUpdated;
         boolean shouldSpitFailedPlacement = triggeredByManual || triggeredByEdge;
         this.manualTrigger = false;
         this.redstoneUpdated = false;
         if (trigger && this.cooldown <= 0) {
            Direction direction = this.facing(this.getBlockState());
            BlockPos target = this.worldPosition.relative(direction);
            int[] slots = this.slotsUpTo(this.visibleSlotCount(this.kind()));

            for (int offset = 0; offset < slots.length; offset++) {
               int slotIndex = (this.machineSlotCursor + offset) % slots.length;
               ItemStack slot = (ItemStack)this.items.get(slotIndex);
               if (!slot.isEmpty() && isFactoryPlacerItem(slot)) {
                  ItemStack placing = slot.copyWithCount(1);
                  if (!this.level.getBlockState(target).canBeReplaced()) {
                     if (shouldSpitFailedPlacement) {
                        this.spitOut(direction, slotIndex, false);
                     }
                  } else {
                     boolean placed = false;
                     if (slot.getItem() instanceof BlockItem blockItem) {
                        DirectionalPlaceContext context = new DirectionalPlaceContext(this.level, target, direction.getOpposite(), placing, direction);
                        InteractionResult result = blockItem.place(context);
                        placed = result.consumesAction();
                        if (!placed && this.level.getBlockState(target).canBeReplaced()) {
                           BlockState fallback = blockItem.getBlock().defaultBlockState();
                           placed = !fallback.isAir() && fallback.canSurvive(this.level, target) && this.level.setBlock(target, fallback, 3);
                        }
                     } else {
                        BlockState crop = cropForSeed(slot);
                        if (crop != null && this.level.getBlockState(target.below()).is(Blocks.FARMLAND) && crop.canSurvive(this.level, target)) {
                           placed = this.level.setBlock(target, crop, 3);
                        }
                     }

                     if (placed) {
                        slot.shrink(1);
                        this.machineSlotCursor = (slotIndex + 1) % slots.length;
                        this.cooldown = 4;
                        this.setChanged();
                        this.updateComparator();
                        return;
                     }

                     if (shouldSpitFailedPlacement) {
                        this.spitOut(direction, slotIndex, false);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isFactoryPlacerItem(ItemStack stack) {
      return stack.getItem() instanceof BlockItem || cropForSeed(stack) != null;
   }

   private static BlockState cropForSeed(ItemStack stack) {
      if (stack.is(Items.WHEAT_SEEDS)) {
         return Blocks.WHEAT.defaultBlockState();
      } else if (stack.is(Items.BEETROOT_SEEDS)) {
         return Blocks.BEETROOTS.defaultBlockState();
      } else if (stack.is(Items.MELON_SEEDS)) {
         return Blocks.MELON_STEM.defaultBlockState();
      } else if (stack.is(Items.PUMPKIN_SEEDS)) {
         return Blocks.PUMPKIN_STEM.defaultBlockState();
      } else if (stack.is(Items.TORCHFLOWER_SEEDS)) {
         return Blocks.TORCHFLOWER_CROP.defaultBlockState();
      } else {
         return stack.is(Items.PITCHER_POD) ? Blocks.PITCHER_CROP.defaultBlockState() : null;
      }
   }

   private void tickBlockBreaker(boolean powered) {
      if (this.level != null) {
         if (powered) {
            this.progress = 0;
            this.processTimeNeeded = 0;
            this.setBooleanProperty(MachineBlocks.ACTIVE, false);
         } else if (this.cooldown <= 0) {
            Direction direction = this.facing(this.getBlockState());
            BlockPos target = this.worldPosition.relative(direction);
            BlockState targetState = this.level.getBlockState(target);
            int requiredTicks = this.blockBreakerProcessTicks(targetState, target);
            if (requiredTicks <= 0) {
               this.progress = 0;
               this.processTimeNeeded = 0;
               this.setBooleanProperty(MachineBlocks.ACTIVE, false);
            } else {
               if (this.processTimeNeeded != requiredTicks) {
                  this.processTimeNeeded = requiredTicks;
                  this.progress = 0;
               }

               this.progress++;
               this.setBooleanProperty(MachineBlocks.ACTIVE, true);
               if (this.progress < this.processTimeNeeded) {
                  this.setChanged();
               } else {
                  if (this.destroyBlockIntoDropBuffer(target, targetState)) {
                     this.progress = 0;
                     this.processTimeNeeded = 0;
                     this.cooldown = 12;
                     this.setChanged();
                     this.updateComparator();
                  } else {
                     this.progress = 0;
                     this.processTimeNeeded = 0;
                     this.cooldown = 12;
                     this.setBooleanProperty(MachineBlocks.ACTIVE, false);
                     this.setChanged();
                     this.updateComparator();
                  }
               }
            }
         }
      }
   }

   private boolean destroyBlockIntoDropBuffer(BlockPos target, BlockState targetState) {
      if (!(this.level instanceof ServerLevel serverLevel) || targetState.isAir()) {
         return false;
      }

      BlockEntity targetEntity = this.level.getBlockEntity(target);
      List<ItemStack> drops = Block.getDrops(targetState, serverLevel, target, targetEntity);
      int[] slots = this.accessibleSlots();
      if (!this.canInsertAllIntoSlots(drops, slots) || !this.level.destroyBlock(target, false, null, 512)) {
         return false;
      }

      for (ItemStack drop : drops) {
         this.insertIntoSlots(drop.copy(), slots);
      }

      return true;
   }

   private int blockBreakerProcessTicks(BlockState targetState, BlockPos target) {
      if (this.level != null && !targetState.isAir()) {
         float hardness = targetState.getDestroySpeed(this.level, target);
         return hardness < 0.0F ? 0 : Mth.clamp(20 + Math.round(hardness * 20.0F), 20, 200);
      } else {
         return 0;
      }
   }

   private void tickTreeCutter(boolean powered) {
      if (this.level != null && powered) {
         this.setBooleanProperty(MachineBlocks.ACTIVE, false);
      } else if (this.level != null && this.cooldown <= 0 && this.tickCounter % 40 == 0) {
         Direction direction = this.facing(this.getBlockState());
         BlockPos cursor = this.worldPosition.relative(direction);
         int cut = 0;

         for (int i = 0; i < 18; i++) {
            BlockState state = this.level.getBlockState(cursor);
            if (!state.is(BlockTags.LOGS)) {
               break;
            }

            if (!this.destroyBlockIntoDropBuffer(cursor, state)) {
               break;
            }

            cut++;
            cursor = cursor.above();
         }

         this.setBooleanProperty(MachineBlocks.ACTIVE, cut > 0);
         if (cut > 0) {
            this.cooldown = 30;
            this.setChanged();
            this.updateComparator();
         }
      } else {
         if (this.cooldown <= 0) {
            this.setBooleanProperty(MachineBlocks.ACTIVE, false);
         }
      }
   }

   private void tickIncinerator() {
      boolean boosted = this.energy >= 8;
      if (this.tickCounter % this.incineratorInterval(boosted) == 0) {
         boolean hadWaste = this.hasWasteQueued();

         for (int slot = 14; slot >= 0; slot--) {
            this.transferItems(slot, slot + 1, 64);
         }

         ItemStack burnSlot = (ItemStack)this.items.get(15);
         if (!burnSlot.isEmpty()) {
            if (boosted && hadWaste) {
               this.energy -= 8;
            }

            burnSlot.shrink(1);
            this.cooldown = 15;
            this.setBooleanProperty(MachineBlocks.LIT, true);
            this.setChanged();
            this.updateComparator();
         } else {
            this.setBooleanProperty(MachineBlocks.LIT, false);
         }
      }
   }

   private boolean hasWasteQueued() {
      for (int slot = 0; slot <= 15; slot++) {
         if (!((ItemStack)this.items.get(slot)).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private void tickMetalCraftingTable() {
      if (this.level != null && this.tickCounter % 5 == 0) {
         this.refreshMetalCraftingResult();
      }
   }

   public void refreshMetalCraftingResult() {
      if (this.kind() == MachineKind.METAL_CRAFTING_TABLE) {
         Optional<MachineBlockEntity.MetalCraftingResult> result = this.metalTableCraftingResult();
         ItemStack output = result.map(MachineBlockEntity.MetalCraftingResult::output).orElse(ItemStack.EMPTY);
         if (!ItemStack.matches((ItemStack)this.items.get(10), output)) {
            this.items.set(10, output.copy());
            this.progress = output.isEmpty() ? 0 : 100;
            this.processTimeNeeded = output.isEmpty() ? 0 : 100;
            this.setChanged();
            this.updateComparator();
         }
      }
   }

   public void consumeMetalCraftingResult() {
      if (this.kind() == MachineKind.METAL_CRAFTING_TABLE && this.level != null) {
         Optional<MachineBlockEntity.MetalCraftingResult> result = this.metalTableCraftingResult();
         if (result.isEmpty()) {
            this.items.set(10, ItemStack.EMPTY);
            this.setChanged();
         } else {
            MachineBlockEntity.MetalCraftingResult crafting = result.get();
            if (crafting.plateRecipe()) {
               ItemStack material = (ItemStack)this.items.get(crafting.plateMaterialSlot());
               if (!material.isEmpty()) {
                  material.shrink(1);
               }

               ItemUtils.damageDirect((ItemStack)this.items.get(0), 1);
            } else if (crafting.recipe().isPresent()) {
               this.consumeVanillaCraftingGrid(crafting.input(), crafting.recipe().get());
            }

            this.items.set(10, ItemStack.EMPTY);
            this.refreshMetalCraftingResult();
            this.cooldown = 8;
            this.setChanged();
            this.updateComparator();
         }
      }
   }

   public void clearMetalCraftingResultForDrop() {
      if (this.kind() == MachineKind.METAL_CRAFTING_TABLE) {
         this.items.set(10, ItemStack.EMPTY);
      }
   }

   private void tickLabFurnace() {
      if (this.level != null) {
         if (this.tickCounter % 20 == 0) {
            this.transferItems(7, 8, 1);
            this.transferItems(2, 7, 1);
            this.transferItems(5, 1, 1);
            this.transferItems(6, 5, 1);
            this.transferItems(3, 0, 1);
            this.transferItems(4, 3, 1);
         }

         Optional<RecipeHolder<SmeltingRecipe>> recipe = this.smeltingRecipe(0);
         if (recipe.isEmpty()) {
            this.progress = 0;
            this.processTimeNeeded = 0;
            this.setBooleanProperty(MachineBlocks.LIT, false);
         } else {
            ItemStack result = ((SmeltingRecipe)recipe.get().value())
               .assemble(new SingleRecipeInput((ItemStack)this.items.get(0)), this.level.registryAccess());
            if (result.isEmpty() || !this.canInsertIntoSlots(result, 2)) {
               this.setBooleanProperty(MachineBlocks.LIT, false);
            } else if (this.burnTicks <= 0 && !this.consumeFuelFromSlot(1)) {
               this.progress = 0;
               this.setBooleanProperty(MachineBlocks.LIT, false);
            } else {
               boolean heaterBoost = this.hasLabFurnaceHeater() && this.energy >= 8;
               this.processTimeNeeded = this.smeltingTime(recipe.get(), 1.2);
               this.progress += heaterBoost ? 2 : 1;
               if (heaterBoost) {
                  this.energy -= 8;
               }

               this.setBooleanProperty(MachineBlocks.LIT, true);
               if (this.progress >= this.processTimeNeeded) {
                  ((ItemStack)this.items.get(0)).shrink(1);
                  this.insertIntoSlots(result.copy(), 2);
                  this.progress = 0;
                  this.setChanged();
                  this.updateComparator();
               }
            }
         }
      }
   }

   private void tickElectricalFurnace() {
      if (this.level != null) {
         this.setPowerConsumptionField(0);
         if (this.tickCounter % 20 == 0) {
            this.transferItems(5, 6, 64);
            this.transferItems(2, 5, 64);
            this.transferItems(3, 0, 64);
            this.transferItems(4, 3, 64);
            this.tickElectricalFeeders();
         }

         if (this.burnTicks > 0 && this.tickCounter % 4 == 0) {
            this.burnTicks = Math.max(0, this.burnTicks - 4);
         }

         Optional<RecipeHolder<SmeltingRecipe>> recipe = this.smeltingRecipe(0);
         if (recipe.isEmpty()) {
            this.progress = 0;
            this.processTimeNeeded = 0;
            this.setBooleanProperty(MachineBlocks.LIT, false);
         } else if (this.electricalSpeed <= 0) {
            this.setBooleanProperty(MachineBlocks.LIT, this.burnTicks > 0);
         } else {
            ItemStack result = ((SmeltingRecipe)recipe.get().value())
               .assemble(new SingleRecipeInput((ItemStack)this.items.get(0)), this.level.registryAccess());
            if (!result.isEmpty() && this.canInsertIntoSlots(result, 2)) {
               int energyPerTick = this.electricalEnergyPerTick();
               if (this.energy < energyPerTick) {
                  this.setBooleanProperty(MachineBlocks.LIT, false);
               } else {
                  this.energy -= energyPerTick;
                  this.setPowerConsumptionField(energyPerTick);
                  this.burnTicks = Math.min(200, this.burnTicks + 20);
                  this.processTimeNeeded = this.smeltingTime(recipe.get(), 1.0);
                  this.progress = this.progress + this.electricalProgressStep();
                  this.setBooleanProperty(MachineBlocks.LIT, this.burnTicks > 0);
                  if (this.progress >= this.processTimeNeeded) {
                     ((ItemStack)this.items.get(0)).shrink(1);
                     this.insertIntoSlots(result.copy(), 2);
                     this.progress = 0;
                     this.setChanged();
                     this.updateComparator();
                  }
               }
            } else {
               this.setBooleanProperty(MachineBlocks.LIT, false);
            }
         }
      }
   }

   private void tickMineralSmelter(boolean powered) {
      if (this.level != null) {
         if (powered) {
            this.tickMineralSmelterCooling();
         } else {
            int slot = this.firstMineralSlot();
            ItemStack result = new ItemStack(Items.MAGMA_BLOCK);
            boolean canStoreLavaByproduct = this.canAddFluid("lava", 250, this.capacityFor(MachineKind.SMALL_MINERAL_SMELTER));
            if (slot < 0 || !this.canInsertIntoSlots(result, 2) || !canStoreLavaByproduct) {
               this.stopMineralSmelterProcess();
            } else if (this.energy < 16) {
               this.tickMineralSmelterCooling();
            } else {
               if (this.mineralCooling) {
                  this.mineralCooling = false;
                  this.progress = 0;
               }

               this.energy -= 16;
               this.setChanged();
               this.progress++;
               this.setIntProperty(MachineBlocks.PHASE_0_3, Math.min(3, this.progress / 50));
               if (this.progress >= MINERAL_SMELTER_PROCESS_TICKS) {
                  ((ItemStack)this.items.get(slot)).shrink(1);
                  this.insertIntoSlots(result, 2);
                  this.addFluid("lava", 250, this.capacityFor(MachineKind.SMALL_MINERAL_SMELTER));
                  this.stopMineralSmelterProcess();
               }
            }
         }
      }
   }

   private void tickMineralSmelterCooling() {
      ItemStack result = new ItemStack(Items.OBSIDIAN);
      if (this.fluidType.equals("lava") && this.fluidAmount >= 1000 && this.canInsertIntoSlots(result, 2)) {
         if (!this.mineralCooling) {
            this.mineralCooling = true;
            this.progress = 0;
         }

         this.progress++;
         this.setIntProperty(MachineBlocks.PHASE_0_3, Math.max(0, 3 - Math.min(3, this.progress / 50)));
         if (this.progress >= MINERAL_SMELTER_PROCESS_TICKS) {
            this.fluidAmount -= 1000;
            if (this.fluidAmount <= 0) {
               this.fluidAmount = 0;
               this.fluidType = "";
            }

            this.insertIntoSlots(result, 2);
            this.stopMineralSmelterProcess();
            this.setChanged();
            this.updateComparator();
         }
      } else {
         this.stopMineralSmelterProcess();
      }
   }

   private void stopMineralSmelterProcess() {
      boolean changed = this.progress != 0 || this.mineralCooling || this.propertyValue(MachineBlocks.PHASE_0_3) != 0;
      this.progress = 0;
      this.mineralCooling = false;
      this.setIntProperty(MachineBlocks.PHASE_0_3, 0);
      if (changed) {
         this.setChanged();
         this.updateComparator();
      }
   }

   private void tickFreezer(boolean powered) {
      if (powered) {
         this.stopFreezerProcess();
      } else {
         int slot = this.firstFreezableSlot();
         if (slot < 0) {
            this.stopFreezerProcess();
         } else {
            ItemStack result = this.freezerResult((ItemStack)this.items.get(slot));
            if (!result.isEmpty() && this.canInsertIntoSlots(result, 2)) {
               if (this.energy < 8) {
                  this.stopFreezerProcess();
               } else {
                  this.energy -= 8;
                  this.setChanged();
                  this.progress++;
                  this.setIntProperty(MachineBlocks.PHASE_0_4, Math.min(4, this.progress / 45));
                  if (this.progress >= FREEZER_PROCESS_TICKS) {
                     ItemStack input = (ItemStack)this.items.get(slot);
                     if (input.is(Items.WATER_BUCKET)) {
                        input.shrink(1);
                        this.insertIntoSlots(new ItemStack(Items.BUCKET), slot);
                     } else {
                        input.shrink(1);
                     }

                     this.insertIntoSlots(result, 2);
                     this.stopFreezerProcess();
                  }
               }
            } else {
               this.stopFreezerProcess();
            }
         }
      }
   }

   private void stopFreezerProcess() {
      boolean changed = this.progress != 0 || this.propertyValue(MachineBlocks.PHASE_0_4) != 0;
      this.progress = 0;
      this.setIntProperty(MachineBlocks.PHASE_0_4, 0);
      if (changed) {
         this.setChanged();
         this.updateComparator();
      }
   }

   private void tickFluidFunnel() {
      if (this.level != null && this.tickCounter % 40 == 0) {
         if (this.fluidAmount < 3000) {
            BlockPos source = this.worldPosition.above();
            if (this.level.getFluidState(source).getType() == Fluids.WATER) {
               this.addFluid("water", 250, 3000);
            } else if (this.level.getFluidState(source).getType() == Fluids.LAVA) {
               this.addFluid("lava", 250, 3000);
            }
         }

         this.pushFluid(Direction.DOWN, 250);
         this.syncFluidLevel(MachineBlocks.LEVEL_0_3, 3000);
      } else {
         this.syncFluidLevel(MachineBlocks.LEVEL_0_3, 3000);
      }
   }

   private void tickFluidBarrel() {
      if (this.level != null && this.tickCounter % 40 == 0 && this.fluidAmount > 0 && this.getBlockState().hasProperty(MachineBlocks.FACING)) {
         if (this.getBlockState().getValue(MachineBlocks.FACING) == Direction.UP) {
            this.pushFluid(Direction.DOWN, 250);
         }
      }
   }

   private void tickFluidAccumulator() {
      if (this.level != null && this.tickCounter % 80 == 0 && this.fluidAmount < 4000) {
         for (Direction direction : Direction.values()) {
            if (this.level.getFluidState(this.worldPosition.relative(direction)).getType() == Fluids.WATER) {
               this.addFluid("water", 250, 4000);
               return;
            }
         }
      }
   }

   private void tickFluidBucketSlots(MachineKind kind) {
      if (hasBucketFillSlots(kind) && this.fluidAmount >= 1000 && ((ItemStack)this.items.get(0)).is(Items.BUCKET)) {
         ItemStack filledBucket = this.bucketForFluid();
         if (!filledBucket.isEmpty() && this.canInsertIntoSlots(filledBucket, 1)) {
            ((ItemStack)this.items.get(0)).shrink(1);
            this.insertIntoSlots(filledBucket, 1);
            this.fluidAmount -= 1000;
            if (this.fluidAmount <= 0) {
               this.fluidAmount = 0;
               this.fluidType = "";
            }

            this.syncFluidLevel(this.propertyForFluidLevel(), this.capacityFor(kind));
            this.syncMilkingFilled();
            this.setChanged();
            this.updateComparator();
         }
      }
   }

   private static boolean hasBucketFillSlots(MachineKind kind) {
      return kind == MachineKind.FLUID_BARREL
         || kind == MachineKind.SMALL_FLUID_FUNNEL
         || kind == MachineKind.PASSIVE_FLUID_ACCUMULATOR
         || kind == MachineKind.SMALL_MILKING_MACHINE;
   }

   private void tickSolarPanel() {
      if (this.level != null && this.tickCounter % 20 == 0) {
         int exposure = 0;
         if (this.level.dimensionType().hasSkyLight() && this.level.isDay() && this.level.canSeeSky(this.worldPosition.above())) {
            exposure = 4;
            if (this.level.isRainingAt(this.worldPosition.above())) {
               exposure = Math.max(1, exposure - 2);
            }

            this.energy = Math.min(SOLAR_CAPACITY, this.energy + exposure * 8);
         }

         int transferBudget = SOLAR_ITEM_TRANSFER;
         transferBudget -= this.chargeSolarCell(transferBudget);
         this.pushSolarEnergy(transferBudget);
         this.setIntProperty(MachineBlocks.EXPOSITION, exposure);
         this.setChanged();
         this.updateComparator();
      }
   }

   private int chargeSolarCell(int maxTransfer) {
      if (this.energy > 0 && maxTransfer > 0) {
         ItemStack cell = (ItemStack)this.items.get(0);
         IEnergyStorage cellEnergy = (IEnergyStorage)cell.getCapability(EnergyStorage.ITEM);
         if (cellEnergy != null && cellEnergy.canReceive()) {
            int received = Mth.clamp(cellEnergy.receiveEnergy(Math.min(maxTransfer, this.energy), false), 0, Math.min(maxTransfer, this.energy));
            if (received > 0) {
               this.energy -= received;
            }

            return received;
         }
      }

      return 0;
   }

   private int pushSolarEnergy(int maxTransfer) {
      if (this.level == null || this.level.isClientSide || this.energy <= 0 || maxTransfer <= 0) {
         return 0;
      }

      int transferred = 0;
      for (Direction direction : SOLAR_OUTPUT_DIRECTIONS) {
         int remaining = Math.min(maxTransfer - transferred, this.energy);
         if (remaining <= 0) {
            break;
         }

         IEnergyStorage receiver = this.level.getCapability(EnergyStorage.BLOCK, this.worldPosition.relative(direction), direction.getOpposite());
         if (receiver == null || !receiver.canReceive()) {
            continue;
         }

         int simulated = Mth.clamp(receiver.receiveEnergy(remaining, true), 0, remaining);
         if (simulated <= 0) {
            continue;
         }

         int accepted = Mth.clamp(receiver.receiveEnergy(simulated, false), 0, simulated);
         this.energy -= accepted;
         transferred += accepted;
      }

      return transferred;
   }

   private void tickMilkingMachine(boolean powered) {
      if (this.level != null) {
         Direction direction = this.facing(this.getBlockState());
         if (this.tickCounter % 20 == 0) {
            this.tickMilkingMachineContainerIo(direction);
         }

         if (!powered && this.tickCounter % 120 == 0) {
            this.tickMilkingMachineContainerIo(direction);
            ItemStack milkBucket = new ItemStack(Items.MILK_BUCKET);
            boolean canFillBucket = ((ItemStack)this.items.get(0)).is(Items.BUCKET) && this.canInsertIntoSlots(milkBucket, 1);
            boolean canStoreFluid = this.canAddFluid("milk", 1000, 4000);
            if (!canFillBucket && !canStoreFluid) {
               this.setBooleanProperty(MachineBlocks.ACTIVE, false);
               this.syncMilkingFilled();
               return;
            }

            AABB box = new AABB(this.worldPosition.relative(direction)).inflate(2.0, 1.5, 2.0);
            if (this.level.getEntitiesOfClass(Cow.class, box, cow -> cow.isAlive() && !cow.isBaby()).isEmpty()) {
               this.setBooleanProperty(MachineBlocks.ACTIVE, false);
            } else {
               if (canFillBucket) {
                  ((ItemStack)this.items.get(0)).shrink(1);
                  this.insertIntoSlots(milkBucket, 1);
                  this.tickMilkingMachineContainerIo(direction);
               } else {
                  this.addFluid("milk", 1000, 4000);
               }

               this.setBooleanProperty(MachineBlocks.ACTIVE, true);
               this.syncMilkingFilled();
               this.setChanged();
               this.updateComparator();
            }
         } else {
            this.setBooleanProperty(MachineBlocks.ACTIVE, false);
            this.syncMilkingFilled();
         }
      }
   }

   private void tickMilkingMachineContainerIo(Direction direction) {
      boolean moved = this.pushMilkingResultToContainer(Direction.DOWN);
      moved |= this.pushMilkingResultToContainer(direction.getOpposite());
      int fluidBefore = this.fluidAmount;
      this.pushFluid(Direction.DOWN, 250);
      moved |= this.fluidAmount != fluidBefore;
      if (((ItemStack)this.items.get(0)).isEmpty()) {
         moved |= this.pullEmptyBucketFromContainer(direction.getOpposite());
      }

      if (((ItemStack)this.items.get(0)).isEmpty()) {
         moved |= this.pullEmptyBucketFromContainer(Direction.DOWN);
      }

      if (moved) {
         this.syncMilkingFilled();
         this.setChanged();
         this.updateComparator();
      }
   }

   private boolean pullEmptyBucketFromContainer(Direction direction) {
      if (this.level != null
         && ((ItemStack)this.items.get(0)).isEmpty()
         && this.level.getBlockEntity(this.worldPosition.relative(direction)) instanceof Container source) {
         for (int var6 = 0; var6 < source.getContainerSize(); var6++) {
            ItemStack stored = source.getItem(var6);
            if (stored.is(Items.BUCKET) && source.canTakeItem(this, var6, stored)) {
               ItemStack moving = stored.copyWithCount(1);
               if (!this.canInsertIntoSlots(moving, 0)) {
                  return false;
               }

               stored.shrink(1);
               if (stored.isEmpty()) {
                  source.setItem(var6, ItemStack.EMPTY);
               }

               this.insertIntoSlots(moving, 0);
               source.setChanged();
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean pushMilkingResultToContainer(Direction direction) {
      if (this.level != null
         && ((ItemStack)this.items.get(1)).is(Items.MILK_BUCKET)
         && this.level.getBlockEntity(this.worldPosition.relative(direction)) instanceof Container target) {
         ItemStack var4 = ((ItemStack)this.items.get(1)).copyWithCount(1);
         if (!this.insertIntoContainer(target, var4)) {
            return false;
         }

         ((ItemStack)this.items.get(1)).shrink(1);
         if (((ItemStack)this.items.get(1)).isEmpty()) {
            this.items.set(1, ItemStack.EMPTY);
         }

         target.setChanged();
         return true;
      } else {
         return false;
      }
   }

   private boolean handleFluidBucket(ItemStack stack, Player player, InteractionHand hand, int capacity) {
      if (stack.is(Items.WATER_BUCKET)) {
         return this.addHeldFluid(stack, player, hand, "water", capacity);
      }

      if (stack.is(Items.LAVA_BUCKET)) {
         return this.addHeldFluid(stack, player, hand, "lava", capacity);
      }

      if (stack.is(Items.MILK_BUCKET)) {
         return this.addHeldFluid(stack, player, hand, "milk", capacity);
      }

      if (stack.is(Items.BUCKET) && this.fluidAmount >= 1000) {
         ItemStack filledBucket = this.bucketForFluid();
         if (filledBucket.isEmpty()) {
            return false;
         }

         this.fluidAmount -= 1000;
         if (this.fluidAmount <= 0) {
            this.fluidAmount = 0;
            this.fluidType = "";
         }

         this.fillHeldBucketFromMachine(stack, player, hand, filledBucket);
         this.syncFluidLevel(this.propertyForFluidLevel(), capacity);
         this.syncMilkingFilled();
         return true;
      } else {
         return false;
      }
   }

   private boolean canHandleFluidBucket(ItemStack stack, int capacity) {
      if (stack.is(Items.WATER_BUCKET)) {
         return this.canAddFluid("water", 1000, capacity);
      } else if (stack.is(Items.LAVA_BUCKET)) {
         return this.canAddFluid("lava", 1000, capacity);
      } else {
         return stack.is(Items.MILK_BUCKET)
            ? this.canAddFluid("milk", 1000, capacity)
            : stack.is(Items.BUCKET) && this.fluidAmount >= 1000 && !this.bucketForFluid().isEmpty();
      }
   }

   private boolean addHeldFluid(ItemStack stack, Player player, InteractionHand hand, String type, int capacity) {
      if (!this.addFluid(type, 1000, capacity)) {
         return false;
      }

      this.syncFluidLevel(this.propertyForFluidLevel(), capacity);
      this.syncMilkingFilled();
      return this.consumeHeldBucket(stack, player, hand, new ItemStack(Items.BUCKET));
   }

   private boolean consumeHeldBucket(ItemStack stack, Player player, InteractionHand hand, ItemStack replacement) {
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
         if (stack.isEmpty()) {
            player.setItemInHand(hand, replacement);
         } else {
            this.giveOrDrop(player, replacement);
         }
      }

      return true;
   }

   private boolean fillHeldBucketFromMachine(ItemStack stack, Player player, InteractionHand hand, ItemStack filledBucket) {
      if (player.getAbilities().instabuild) {
         this.giveOrDrop(player, filledBucket.copy());
         return true;
      } else {
         return this.consumeHeldBucket(stack, player, hand, filledBucket);
      }
   }

   private void giveOrDrop(Player player, ItemStack stack) {
      if (!player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }

   private boolean addFluid(String type, int amount, int capacity) {
      if (!this.canAddFluid(type, amount, capacity)) {
         return false;
      }

      this.fluidType = type;
      this.fluidAmount += amount;
      this.setChanged();
      this.updateComparator();
      return true;
   }

   private boolean canAddFluid(String type, int amount, int capacity) {
      return this.canStoreFluidType(this.kind(), type) && (this.fluidType.isEmpty() || this.fluidType.equals(type)) && this.fluidAmount + amount <= capacity;
   }

   private void pushFluid(Direction direction, int maxTransfer) {
      if (this.level != null && this.fluidAmount > 0 && maxTransfer > 0) {
         Fluid fluid = this.fluidForType(this.fluidType);
         if (fluid != Fluids.EMPTY) {
            IFluidHandler target = (IFluidHandler)this.level.getCapability(FluidHandler.BLOCK, this.worldPosition.relative(direction), direction.getOpposite());
            if (target != null) {
               FluidStack offered = new FluidStack(fluid, Math.min(maxTransfer, this.fluidAmount));
               int accepted = target.fill(offered, FluidAction.SIMULATE);
               if (accepted > 0) {
                  FluidStack drained = this.fluidHandler.drain(Math.min(accepted, offered.getAmount()), FluidAction.EXECUTE);
                  if (!drained.isEmpty()) {
                     int filled = target.fill(drained, FluidAction.EXECUTE);
                     if (filled < drained.getAmount()) {
                        this.addFluid(this.typeForFluid(drained.getFluid()), drained.getAmount() - filled, this.capacityFor(this.kind()));
                     }
                  }
               }
            }
         }
      }
   }

   private ItemStack bucketForFluid() {
      return switch (this.fluidType) {
         case "water" -> new ItemStack(Items.WATER_BUCKET);
         case "lava" -> new ItemStack(Items.LAVA_BUCKET);
         case "milk" -> new ItemStack(Items.MILK_BUCKET);
         default -> ItemStack.EMPTY;
      };
   }

   private void syncFluidLevel(IntegerProperty property, int capacity) {
      if (property != null) {
         this.setIntProperty(
            property,
            Math.min(property.getPossibleValues().stream().max(Integer::compareTo).orElse(0), this.scaleToLevel(this.fluidAmount, capacity, property))
         );
      }
   }

   private void syncMilkingFilled() {
      this.setBooleanProperty(MachineBlocks.FILLED, this.fluidAmount > 0 || ((ItemStack)this.items.get(1)).is(Items.MILK_BUCKET));
   }

   private IntegerProperty propertyForFluidLevel() {
      MachineKind kind = this.kind();
      if (kind == MachineKind.FLUID_BARREL) {
         return MachineBlocks.LEVEL_0_4;
      } else {
         return kind == MachineKind.SMALL_FLUID_FUNNEL ? MachineBlocks.LEVEL_0_3 : null;
      }
   }

   private boolean canHandleFluids(MachineKind kind) {
      return kind != null && kind.isFluidContainer();
   }

   private boolean canStoreFluidType(MachineKind kind, String type) {
      if (kind == null || type.isEmpty()) {
         return false;
      }

      return switch (kind) {
         case SMALL_MINERAL_SMELTER -> type.equals("lava");
         case PASSIVE_FLUID_ACCUMULATOR -> type.equals("water");
         case SMALL_MILKING_MACHINE -> type.equals("milk");
         case FLUID_BARREL, SMALL_FLUID_FUNNEL -> type.equals("water") || type.equals("lava") || type.equals("milk");
         default -> false;
      };
   }

   private String typeForFluid(Fluid fluid) {
      if (fluid == Fluids.WATER) {
         return "water";
      } else if (NeoForgeMod.MILK.isBound() && fluid == NeoForgeMod.MILK.value()) {
         return "milk";
      } else {
         return fluid == Fluids.LAVA ? "lava" : "";
      }
   }

   private Fluid fluidForType(String type) {
      return (Fluid)(switch (type) {
         case "water" -> Fluids.WATER;
         case "lava" -> Fluids.LAVA;
         case "milk" -> NeoForgeMod.MILK.isBound() ? NeoForgeMod.MILK.value() : Fluids.EMPTY;
         default -> Fluids.EMPTY;
      });
   }

   private int capacityFor(MachineKind kind) {
      if (kind == MachineKind.SMALL_FLUID_FUNNEL) {
         return 3000;
      } else {
         return kind == MachineKind.SMALL_MINERAL_SMELTER ? 4000 : 4000;
      }
   }

   private int scaleToComparator(int amount, int capacity) {
      return amount > 0 && capacity > 0 ? Math.max(1, Math.min(15, (int)Math.ceil(amount * 15.0 / capacity))) : 0;
   }

   private int scaleToLevel(int amount, int capacity, IntegerProperty property) {
      int max = property.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
      return amount > 0 && max > 0 ? Math.max(1, Math.min(max, (int)Math.ceil((double)amount * max / capacity))) : 0;
   }

   private Direction facing(BlockState state) {
      if (state.hasProperty(MachineBlocks.HORIZONTAL_FACING)) {
         return (Direction)state.getValue(MachineBlocks.HORIZONTAL_FACING);
      } else {
         return state.hasProperty(MachineBlocks.FACING) ? (Direction)state.getValue(MachineBlocks.FACING) : Direction.NORTH;
      }
   }

   private void setBooleanProperty(BooleanProperty property, boolean value) {
      if (this.level != null) {
         BlockState state = this.getBlockState();
         if (state.hasProperty(property) && (Boolean)state.getValue(property) != value) {
            this.level.setBlock(this.worldPosition, (BlockState)state.setValue(property, value), 3);
         }
      }
   }

   private void setIntProperty(IntegerProperty property, int value) {
      if (this.level != null) {
         BlockState state = this.getBlockState();
         if (state.hasProperty(property)) {
            int clamped = Math.max(
               property.getPossibleValues().stream().min(Integer::compareTo).orElse(value),
               Math.min(property.getPossibleValues().stream().max(Integer::compareTo).orElse(value), value)
            );
            if ((Integer)state.getValue(property) != clamped) {
               this.level.setBlock(this.worldPosition, (BlockState)state.setValue(property, clamped), 3);
            }
         }
      }
   }

   private int propertyValue(IntegerProperty property) {
      BlockState state = this.getBlockState();
      return state.hasProperty(property) ? (Integer)state.getValue(property) : 0;
   }

   private Optional<RecipeHolder<SmeltingRecipe>> smeltingRecipe(int inputSlot) {
      return this.level != null && inputSlot >= 0
         ? this.level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput((ItemStack)this.items.get(inputSlot)), this.level)
         : Optional.empty();
   }

   private Optional<RecipeHolder<SmeltingRecipe>> smeltingRecipeFor(ItemStack stack) {
      return this.level != null && !stack.isEmpty()
         ? this.level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), this.level)
         : Optional.empty();
   }

   private boolean canSmelt(ItemStack stack) {
      return this.smeltingRecipeFor(stack).isPresent();
   }

   private int smeltingTime(RecipeHolder<SmeltingRecipe> recipe, double speed) {
      int base = Math.max(1, ((SmeltingRecipe)recipe.value()).getCookingTime());
      return Math.max(1, (int)Math.ceil(base / speed));
   }

   private boolean consumeFuelFromSlot(int slot) {
      if (slot >= 0 && slot < this.items.size()) {
         ItemStack fuel = (ItemStack)this.items.get(slot);
         int burnTime = this.fuelBurnTime(fuel);
         if (burnTime <= 0) {
            return false;
         }

         Item remainder = fuel.getItem().getCraftingRemainingItem();
         ItemStack remainderStack = remainder == null ? ItemStack.EMPTY : new ItemStack(remainder);
         fuel.shrink(1);
         if (!remainderStack.isEmpty()) {
            if (fuel.isEmpty()) {
               this.items.set(slot, remainderStack);
            } else if (!this.insertIntoSlots(remainderStack, slot) && this.level != null) {
               this.dropItem(this.facing(this.getBlockState()), remainderStack);
            }
         }

         this.burnTicks = burnTime;
         this.fuelBurnTime = burnTime;
         this.setChanged();
         return true;
      } else {
         return false;
      }
   }

   private int fuelBurnTime(ItemStack stack) {
      return stack.isEmpty() ? 0 : AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
   }

   private boolean isFuel(ItemStack stack) {
      return this.fuelBurnTime(stack) > 0;
   }

   private static boolean isEnergyItem(ItemStack stack) {
      return !stack.isEmpty() && stack.getCapability(EnergyStorage.ITEM) != null;
   }

   private Optional<MachineBlockEntity.MetalCraftingResult> metalTableCraftingResult() {
      if (this.level == null) {
         return Optional.empty();
      }

      CraftingInput grid = this.metalTableGridInput();
      Optional<MachineBlockEntity.MetalCraftingResult> plateRecipe = this.metalTablePlateCraftingResult(grid);
      if (plateRecipe.isPresent()) {
         return plateRecipe;
      }

      Optional<RecipeHolder<CraftingRecipe>> vanillaRecipe = this.level
         .getRecipeManager()
         .getRecipeFor(RecipeType.CRAFTING, grid, this.level)
         .filter(recipe -> !isIePlateHammerRecipe(recipe.id()));
      if (vanillaRecipe.isPresent()) {
         ItemStack output = ((CraftingRecipe)vanillaRecipe.get().value()).assemble(grid, this.level.registryAccess());
         if (!output.isEmpty()) {
            return Optional.of(new MachineBlockEntity.MetalCraftingResult(output, vanillaRecipe, grid, false, -1));
         }
      }

      return Optional.empty();
   }

   private Optional<MachineBlockEntity.MetalCraftingResult> metalTablePlateCraftingResult(CraftingInput grid) {
      ItemStack hammer = (ItemStack)this.items.get(0);
      if (!isHammer(hammer)) {
         return Optional.empty();
      }

      int materialSlot = this.singleGridMaterialSlot();
      if (materialSlot < 0) {
         return Optional.empty();
      }

      ItemStack material = (ItemStack)this.items.get(materialSlot);
      CraftingInput plateInput = CraftingInput.of(2, 1, List.of(material.copyWithCount(1), hammer.copyWithCount(1)));
      return this.level
         .getRecipeManager()
         .getAllRecipesFor(RecipeType.CRAFTING)
         .stream()
         .filter(recipe -> isIePlateHammerRecipe(recipe.id()))
         .filter(recipe -> ((CraftingRecipe)recipe.value()).matches(plateInput, this.level))
         .map(
            recipe -> new MachineBlockEntity.MetalCraftingResult(
               ((CraftingRecipe)recipe.value()).assemble(plateInput, this.level.registryAccess()),
               Optional.of((RecipeHolder<CraftingRecipe>)recipe),
               grid,
               true,
               materialSlot
            )
         )
         .filter(result -> isIePlateStack(result.output()))
         .findFirst();
   }

   private CraftingInput metalTableGridInput() {
      List<ItemStack> stacks = new ArrayList<>(9);

      for (int slot = 1; slot < 10; slot++) {
         stacks.add(((ItemStack)this.items.get(slot)).copy());
      }

      return CraftingInput.of(3, 3, stacks);
   }

   private int singleGridMaterialSlot() {
      int foundSlot = -1;

      for (int slot = 1; slot < 10; slot++) {
         ItemStack stack = (ItemStack)this.items.get(slot);
         if (!stack.isEmpty()) {
            if (isHammer(stack) || foundSlot != -1) {
               return -1;
            }

            foundSlot = slot;
         }
      }

      return foundSlot;
   }

   private void consumeVanillaCraftingGrid(CraftingInput input, RecipeHolder<CraftingRecipe> recipe) {
      NonNullList<ItemStack> remainders = ((CraftingRecipe)recipe.value()).getRemainingItems(input);

      for (int gridIndex = 0; gridIndex < 9; gridIndex++) {
         int slot = 1 + gridIndex;
         ItemStack stack = (ItemStack)this.items.get(slot);
         if (!stack.isEmpty()) {
            stack.shrink(1);
         }

         ItemStack remainder = gridIndex < remainders.size() ? (ItemStack)remainders.get(gridIndex) : ItemStack.EMPTY;
         if (!remainder.isEmpty()) {
            this.addCraftingRemainder(slot, remainder.copy());
         }
      }
   }

   private void addCraftingRemainder(int slot, ItemStack remainder) {
      ItemStack current = (ItemStack)this.items.get(slot);
      if (current.isEmpty()) {
         this.items.set(slot, remainder);
      } else if (ItemStack.isSameItemSameComponents(current, remainder) && current.getCount() + remainder.getCount() <= current.getMaxStackSize()) {
         current.grow(remainder.getCount());
      } else {
         ItemStack moving = remainder.copy();
         if (!this.insertIntoSlots(moving, metalTableGridSlots()) && !moving.isEmpty() && this.level != null) {
            this.dropItem(this.facing(this.getBlockState()), moving);
         }
      }
   }

   private static int[] metalTableGridSlots() {
      int[] slots = new int[9];

      for (int i = 0; i < slots.length; i++) {
         slots[i] = 1 + i;
      }

      return slots;
   }

   private static boolean isHammer(ItemStack stack) {
      return !stack.isEmpty() && stack.is(IE_HAMMERS);
   }

   private static boolean isExternalHeater(ItemStack stack) {
      return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(IE_FURNACE_HEATER);
   }

   private boolean hasLabFurnaceHeater() {
      return isExternalHeater((ItemStack)this.items.get(9)) || isExternalHeater((ItemStack)this.items.get(10));
   }

   private static boolean isIePlateHammerRecipe(ResourceLocation id) {
      String path = id.getPath();
      return "immersiveengineering".equals(id.getNamespace()) && path.startsWith("crafting/plate_") && path.endsWith("_hammering");
   }

   private static boolean isIePlateStack(ItemStack stack) {
      ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
      return !stack.isEmpty() && "immersiveengineering".equals(itemId.getNamespace()) && itemId.getPath().startsWith("plate_");
   }

   private int visibleSlotCount(MachineKind kind) {
      return MachineLayout.forKind(kind).machineSlots().size();
   }

   private int[] accessibleSlots() {
      MachineKind kind = this.kind();
      return kind == null ? this.slotsUpTo(this.items.size()) : this.slotsUpTo(this.visibleSlotCount(kind));
   }

   private int[] slotsUpTo(int count) {
      int safeCount = Math.max(0, Math.min(count, this.items.size()));
      int[] slots = new int[safeCount];
      int i = 0;

      while (i < safeCount) {
         slots[i] = i++;
      }

      return slots;
   }

   private boolean canInsertIntoSlots(ItemStack source, int... slots) {
      if (source.isEmpty()) {
         return true;
      }

      int remaining = source.getCount();

      for (int slotIndex : slots) {
         if (this.validSlot(slotIndex)) {
            ItemStack slot = (ItemStack)this.items.get(slotIndex);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, source)) {
               int capacity = Math.min(slot.getMaxStackSize(), this.getMaxStackSize(slot));
               remaining -= Math.max(0, capacity - slot.getCount());
               if (remaining <= 0) {
                  return true;
               }
            }
         }
      }

      for (int slotIndex : slots) {
         if (this.validSlot(slotIndex) && ((ItemStack)this.items.get(slotIndex)).isEmpty()) {
            remaining -= Math.min(source.getMaxStackSize(), this.getMaxStackSize(source));
            if (remaining <= 0) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean canInsertAllIntoSlots(List<ItemStack> sources, int... slots) {
      NonNullList<ItemStack> simulated = NonNullList.withSize(this.items.size(), ItemStack.EMPTY);

      for (int i = 0; i < this.items.size(); i++) {
         simulated.set(i, ((ItemStack)this.items.get(i)).copy());
      }

      for (ItemStack source : sources) {
         if (!this.simulateInsertIntoSlots(simulated, source, slots)) {
            return false;
         }
      }

      return true;
   }

   private boolean simulateInsertIntoSlots(NonNullList<ItemStack> simulated, ItemStack source, int... slots) {
      if (source.isEmpty()) {
         return true;
      }

      ItemStack remaining = source.copy();

      for (int slotIndex : slots) {
         if (this.validSlot(slotIndex)) {
            ItemStack slot = (ItemStack)simulated.get(slotIndex);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remaining)) {
               int move = Math.min(remaining.getCount(), Math.min(slot.getMaxStackSize(), this.getMaxStackSize(slot)) - slot.getCount());
               if (move > 0) {
                  slot.grow(move);
                  remaining.shrink(move);
                  if (remaining.isEmpty()) {
                     return true;
                  }
               }
            }
         }
      }

      for (int slotIndex : slots) {
         if (this.validSlot(slotIndex) && ((ItemStack)simulated.get(slotIndex)).isEmpty()) {
            int move = Math.min(remaining.getCount(), Math.min(remaining.getMaxStackSize(), this.getMaxStackSize(remaining)));
            simulated.set(slotIndex, remaining.copyWithCount(move));
            remaining.shrink(move);
            if (remaining.isEmpty()) {
               return true;
            }
         }
      }

      return remaining.isEmpty();
   }

   private boolean insertIntoSlots(ItemStack source, int... slots) {
      if (source.isEmpty()) {
         return true;
      }

      ItemStack remaining = source;

      for (int slotIndex : slots) {
         if (this.validSlot(slotIndex)) {
            ItemStack slot = (ItemStack)this.items.get(slotIndex);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remaining)) {
               int move = Math.min(remaining.getCount(), Math.min(slot.getMaxStackSize(), this.getMaxStackSize(slot)) - slot.getCount());
               if (move > 0) {
                  slot.grow(move);
                  remaining.shrink(move);
                  this.setChanged();
                  if (remaining.isEmpty()) {
                     return true;
                  }
               }
            }
         }
      }

      for (int slotIndex : slots) {
         if (this.validSlot(slotIndex) && ((ItemStack)this.items.get(slotIndex)).isEmpty()) {
            int move = Math.min(remaining.getCount(), Math.min(remaining.getMaxStackSize(), this.getMaxStackSize(remaining)));
            this.items.set(slotIndex, remaining.copyWithCount(move));
            remaining.shrink(move);
            this.setChanged();
            if (remaining.isEmpty()) {
               return true;
            }
         }
      }

      return remaining.isEmpty();
   }

   private boolean transferItems(int fromSlot, int toSlot, int maxCount) {
      if (this.validSlot(fromSlot) && this.validSlot(toSlot) && fromSlot != toSlot && maxCount > 0) {
         ItemStack from = (ItemStack)this.items.get(fromSlot);
         if (from.isEmpty()) {
            return false;
         }

         ItemStack to = (ItemStack)this.items.get(toSlot);
         int move;
         if (to.isEmpty()) {
            int capacity = Math.min(from.getMaxStackSize(), this.getMaxStackSize(from));
            move = Math.min(maxCount, Math.min(from.getCount(), capacity));
            if (move <= 0) {
               return false;
            }

            this.items.set(toSlot, from.copyWithCount(move));
         } else {
            if (!ItemStack.isSameItemSameComponents(from, to)) {
               return false;
            }

            int capacity = Math.min(to.getMaxStackSize(), this.getMaxStackSize(to));
            move = Math.min(maxCount, Math.min(from.getCount(), capacity - to.getCount()));
            if (move <= 0) {
               return false;
            }

            to.grow(move);
         }

         from.shrink(move);
         if (from.isEmpty()) {
            this.items.set(fromSlot, ItemStack.EMPTY);
         }

         this.setChanged();
         this.updateComparator();
         return true;
      } else {
         return false;
      }
   }

   private boolean logicAllowsTrigger(int logic, boolean powered) {
      if ((logic & 4) != 0) {
         return true;
      }

      boolean inverted = (logic & 1) != 0;
      return inverted ? !powered : powered;
   }

   private static int sanitizeTriggerLogic(int logic) {
      return logic & TRIGGER_LOGIC_MASK;
   }

   private static int sanitizeDropperLogic(int logic) {
      return logic & DROPPER_LOGIC_MASK;
   }

   private void collectNearbyItems(Direction facing) {
      if (this.level != null) {
         AABB range = facing == Direction.UP
            ? new AABB(this.worldPosition.above()).inflate(0.1 + this.hopperRange, 0.6, 0.1 + this.hopperRange)
            : new AABB(this.worldPosition.below(2)).inflate(0.1 + this.hopperRange, 1.0, 0.1 + this.hopperRange);

         for (ItemEntity itemEntity : this.level.getEntitiesOfClass(ItemEntity.class, range, item -> item.isAlive() && !item.getItem().isEmpty())) {
            ItemStack stack = itemEntity.getItem();
            ItemStack remainder = this.insertRemainder(stack);
            if (remainder.isEmpty()) {
               itemEntity.discard();
            } else if (remainder.getCount() != stack.getCount()) {
               itemEntity.setItem(remainder);
            }
         }
      }
   }

   private void pushItems(Direction direction, int maxCount) {
      if (this.level != null && this.level.getBlockEntity(this.worldPosition.relative(direction)) instanceof Container target) {
         int var11 = Math.max(1, maxCount);

         for (int tries = 0; tries < this.accessibleSlots().length && var11 > 0; tries++) {
            int slotIndex = this.machineSlotCursor % this.accessibleSlots().length;
            ItemStack stack = (ItemStack)this.items.get(slotIndex);
            if (!stack.isEmpty()) {
               int count = Math.min(var11, stack.getCount());
               ItemStack moving = stack.copyWithCount(count);
               int inserted = this.insertIntoContainerCount(target, moving);
               if (inserted > 0) {
                  stack.shrink(inserted);
                  var11 -= inserted;
                  this.setChanged();
                  target.setChanged();
                  this.updateComparator();
               }
            }

            this.machineSlotCursor = (this.machineSlotCursor + 1) % this.accessibleSlots().length;
         }
      }
   }

   private int insertIntoContainerCount(Container target, ItemStack stack) {
      int before = stack.getCount();

      for (int slot = 0; slot < target.getContainerSize() && !stack.isEmpty(); slot++) {
         if (target.canPlaceItem(slot, stack)) {
            ItemStack existing = target.getItem(slot);
            if (existing.isEmpty()) {
               int move = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), target.getMaxStackSize(stack)));
               if (move > 0) {
                  target.setItem(slot, stack.copyWithCount(move));
                  stack.shrink(move);
               }
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
               int capacity = Math.min(existing.getMaxStackSize(), target.getMaxStackSize(existing));
               int move = Math.min(stack.getCount(), capacity - existing.getCount());
               if (move > 0) {
                  existing.grow(move);
                  stack.shrink(move);
               }
            }
         }
      }

      return before - stack.getCount();
   }

   private void tickElectricalFeeders() {
      if (this.level != null && ((ItemStack)this.items.get(1)).is(Items.HOPPER)) {
         Direction outputSide = this.facing(this.getBlockState());
         Direction inputSide = outputSide.getOpposite();
         if (this.energy >= 4 && this.pullSmeltableFromContainer(inputSide, 64)) {
            this.energy -= 4;
            this.setChanged();
         }

         if (this.energy >= 4 && this.pushSlotsToContainer(outputSide, 64, 6, 5, 2)) {
            this.energy -= 4;
            this.setChanged();
         }
      }
   }

   private boolean pullSmeltableFromContainer(Direction direction, int maxCount) {
      if (this.level != null && this.level.getBlockEntity(this.worldPosition.relative(direction)) instanceof Container source) {
         int var10 = Math.max(1, maxCount);

         for (int slot = 0; slot < source.getContainerSize() && var10 > 0; slot++) {
            ItemStack stored = source.getItem(slot);
            if (!stored.isEmpty() && this.canSmelt(stored) && source.canTakeItem(this, slot, stored)) {
               ItemStack moving = stored.copyWithCount(Math.min(var10, stored.getCount()));
               int before = moving.getCount();
               this.insertIntoSlots(moving, 0, 3, 4);
               int moved = before - moving.getCount();
               if (moved > 0) {
                  stored.shrink(moved);
                  if (stored.isEmpty()) {
                     source.setItem(slot, ItemStack.EMPTY);
                  }

                  source.setChanged();
                  this.setChanged();
                  this.updateComparator();
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean pushSlotsToContainer(Direction direction, int maxCount, int... sourceSlots) {
      if (this.level != null && this.level.getBlockEntity(this.worldPosition.relative(direction)) instanceof Container target) {
         int var14 = Math.max(1, maxCount);
         boolean movedAny = false;

         for (int sourceSlot : sourceSlots) {
            if (this.validSlot(sourceSlot) && var14 > 0) {
               ItemStack stored = (ItemStack)this.items.get(sourceSlot);
               if (!stored.isEmpty()) {
                  ItemStack moving = stored.copyWithCount(Math.min(var14, stored.getCount()));
                  int moved = this.insertIntoContainerCount(target, moving);
                  if (moved > 0) {
                     stored.shrink(moved);
                     if (stored.isEmpty()) {
                        this.items.set(sourceSlot, ItemStack.EMPTY);
                     }

                     var14 -= moved;
                     movedAny = true;
                  }
               }
            }
         }

         if (movedAny) {
            target.setChanged();
            this.setChanged();
            this.updateComparator();
         }

         return movedAny;
      } else {
         return false;
      }
   }

   private void updateDropperFilters() {
      Arrays.fill(this.dropperFilterMatches, 0);
      int[] availableCounts = new int[12];

      for (int slot = 0; slot < 12; slot++) {
         availableCounts[slot] = ((ItemStack)this.items.get(slot)).getCount();
      }

      for (int filter = 0; filter < 3; filter++) {
         ItemStack wanted = (ItemStack)this.items.get(12 + filter);
         if (!wanted.isEmpty()) {
            this.dropperFilterMatches[filter] = 1;
            if (this.countAvailableDropperStock(wanted, availableCounts) >= wanted.getCount()) {
               this.dropperFilterMatches[filter] = 2;
               this.reserveAvailableDropperStock(wanted, availableCounts);
            }
         }
      }
   }

   private int countAvailableDropperStock(ItemStack wanted, int[] availableCounts) {
      int found = 0;

      for (int slot = 0; slot < 12; slot++) {
         ItemStack candidate = (ItemStack)this.items.get(slot);
         if (availableCounts[slot] > 0 && ItemStack.isSameItemSameComponents(candidate, wanted)) {
            found += availableCounts[slot];
         }
      }

      return found;
   }

   private void reserveAvailableDropperStock(ItemStack wanted, int[] availableCounts) {
      int remaining = wanted.getCount();

      for (int slot = 0; slot < 12 && remaining > 0; slot++) {
         ItemStack candidate = (ItemStack)this.items.get(slot);
         if (availableCounts[slot] > 0 && ItemStack.isSameItemSameComponents(candidate, wanted)) {
            int take = Math.min(remaining, availableCounts[slot]);
            availableCounts[slot] -= take;
            remaining -= take;
         }
      }
   }

   private boolean hasDropperCandidate(boolean filterTrigger) {
      if (filterTrigger) {
         for (int i = 0; i < 3; i++) {
            if (this.dropperFilterMatches[i] > 1) {
               return true;
            }
         }

         return false;
      } else {
         for (int i = 0; i < 12; i++) {
            ItemStack stack = (ItemStack)this.items.get(i);
            if (!stack.isEmpty() && stack.getCount() >= this.dropperCount) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean dropRoundRobinStack(Direction direction) {
      for (int offset = 0; offset < 12; offset++) {
         int i = (this.machineSlotCursor + offset) % 12;
         ItemStack slot = (ItemStack)this.items.get(i);
         if (!slot.isEmpty() && slot.getCount() >= this.dropperCount && !this.matchesAnyDropperFilter(slot)) {
            ItemStack dropped = slot.split(this.dropperCount);
            this.dropItem(direction, dropped);
            this.machineSlotCursor = (i + 1) % 12;
            return true;
         }
      }

      return false;
   }

   private boolean dropFilteredStacks(Direction direction) {
      boolean droppedAny = false;

      for (int filter = 0; filter < 3; filter++) {
         if (this.dropperFilterMatches[filter] > 1) {
            ItemStack wanted = ((ItemStack)this.items.get(12 + filter)).copy();
            int wantedCount = wanted.getCount();
            if (this.countDropperStock(wanted) < wantedCount) {
               continue;
            }

            int remaining = wantedCount;

            for (int slot = 11; slot >= 0 && remaining > 0; slot--) {
               ItemStack stored = (ItemStack)this.items.get(slot);
               if (ItemStack.isSameItemSameComponents(stored, wanted)) {
                  int take = Math.min(remaining, stored.getCount());
                  stored.shrink(take);
                  remaining -= take;
               }
            }

            this.dropItem(direction, wanted.copyWithCount(wantedCount));
            droppedAny = true;
         }
      }

      return droppedAny;
   }

   private int countDropperStock(ItemStack wanted) {
      int found = 0;

      for (int slot = 0; slot < 12; slot++) {
         ItemStack candidate = (ItemStack)this.items.get(slot);
         if (ItemStack.isSameItemSameComponents(candidate, wanted)) {
            found += candidate.getCount();
         }
      }

      return found;
   }

   private boolean matchesAnyDropperFilter(ItemStack stack) {
      for (int filter = 0; filter < 3; filter++) {
         ItemStack wanted = (ItemStack)this.items.get(12 + filter);
         if (!wanted.isEmpty() && ItemStack.isSameItemSameComponents(stack, wanted)) {
            return true;
         }
      }

      return false;
   }

   private void dropItem(Direction direction, ItemStack stack) {
      if (this.level != null && !stack.isEmpty()) {
         double offset = direction == Direction.DOWN ? 0.8 : 0.7;
         Vec3 velocity = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
         double xDeviation = 0.01 * Mth.clamp(this.dropperXDeviation, -100, 100);
         double yDeviation = 0.01 * Mth.clamp(this.dropperYDeviation, -100, 100);

         velocity = switch (direction) {
            case DOWN -> velocity.add(xDeviation, 0.0, -yDeviation);
            case NORTH -> velocity.add(xDeviation, yDeviation, 0.0);
            case SOUTH -> velocity.add(-xDeviation, yDeviation, 0.0);
            case EAST -> velocity.add(0.0, yDeviation, xDeviation);
            case WEST -> velocity.add(0.0, yDeviation, -xDeviation);
            case UP -> velocity.add(xDeviation, 0.0, yDeviation);
            default -> throw new MatchException(null, null);
         };
         if (this.dropperNoise > 0) {
            velocity = velocity.add(
               (this.level.random.nextDouble() - 0.5) * 0.001 * this.dropperNoise,
               (this.level.random.nextDouble() - 0.5) * 0.001 * this.dropperNoise,
               (this.level.random.nextDouble() - 0.5) * 0.001 * this.dropperNoise
            );
         }

         double speed = Math.max(5, this.dropperSpeed) * 0.01;
         velocity = velocity.normalize().scale(speed);
         ItemEntity itemEntity = new ItemEntity(
            this.level,
            this.worldPosition.getX() + 0.5 + direction.getStepX() * offset,
            this.worldPosition.getY() + 0.5 + direction.getStepY() * offset,
            this.worldPosition.getZ() + 0.5 + direction.getStepZ() * offset,
            stack
         );
         itemEntity.setDeltaMovement(velocity);
         itemEntity.hurtMarked = true;
         this.level.addFreshEntity(itemEntity);
      }
   }

   private void spitOut(Direction direction, int slotIndex, boolean all) {
      if (this.validSlot(slotIndex)) {
         ItemStack slot = (ItemStack)this.items.get(slotIndex);
         if (!slot.isEmpty()) {
            ItemStack drop = all ? slot.copy() : slot.copyWithCount(1);
            if (all) {
               this.items.set(slotIndex, ItemStack.EMPTY);
            } else {
               slot.shrink(1);
            }

            this.dropItem(direction, drop);
            this.setChanged();
            this.updateComparator();
         }
      }
   }

   private int electricalProgressStep() {
      return switch (this.electricalSpeed) {
         case 0 -> 0;
         default -> 3;
         case 2 -> 4;
         case 3 -> 6;
      };
   }

   private int electricalEnergyPerTick() {
      return switch (this.electricalSpeed) {
         case 0 -> 0;
         default -> 16;
         case 2 -> 32;
         case 3 -> 64;
      };
   }

   private void setPowerConsumptionField(int value) {
      int clamped = Mth.clamp(value, 0, ELECTRIC_MAX_ENERGY_PER_TICK);
      if (this.powerConsumptionField != clamped) {
         this.powerConsumptionField = clamped;
         this.setChanged();
      }
   }

   private boolean canReceiveEnergy(MachineKind kind) {
      return kind == MachineKind.SMALL_ELECTRICAL_FURNACE
         || kind == MachineKind.SMALL_WASTE_INCINERATOR
         || kind == MachineKind.SMALL_MINERAL_SMELTER
         || kind == MachineKind.SMALL_FREEZER
         || kind == MachineKind.SMALL_LAB_FURNACE;
   }

   private boolean canStoreEnergy(MachineKind kind) {
      return kind == MachineKind.SMALL_SOLAR_PANEL || this.canReceiveEnergy(kind);
   }

   private void normalizeLoadedState() {
      this.tickCounter = Math.max(0, this.tickCounter);
      this.fifoTimer = Math.max(0, this.fifoTimer);
      this.cooldown = Mth.clamp(this.cooldown, 0, 400);
      this.burnTicks = Math.max(0, this.burnTicks);
      this.fuelBurnTime = Math.max(0, this.fuelBurnTime);
      this.redstoneUpdated = false;
      this.manualTrigger = false;
      this.manualRedstoneTrigger = false;
      this.guiSignalFlashTicks = 0;
      MachineKind kind = this.kind();
      if (kind == null) {
         this.energy = 0;
         this.fluidAmount = 0;
         this.fluidType = "";
         return;
      }

      this.machineSlotCursor = Mth.clamp(this.machineSlotCursor, 0, this.visibleSlotCount(kind) - 1);
      this.hopperLogic = sanitizeTriggerLogic(this.hopperLogic);
      this.dropperLogic = sanitizeDropperLogic(this.dropperLogic);
      this.placerLogic = sanitizeTriggerLogic(this.placerLogic);
      if (this.canStoreEnergy(kind)) {
         this.energy = Mth.clamp(this.energy, 0, this.energyCapacity(kind));
      } else {
         this.energy = 0;
      }

      if (kind == MachineKind.SMALL_MINERAL_SMELTER) {
         this.progress = validSavedProgress(this.progress, MINERAL_SMELTER_PROCESS_TICKS);
      } else if (kind == MachineKind.SMALL_FREEZER) {
         this.progress = validSavedProgress(this.progress, FREEZER_PROCESS_TICKS);
      } else if (kind == MachineKind.SMALL_LAB_FURNACE
         || kind == MachineKind.SMALL_ELECTRICAL_FURNACE
         || kind == MachineKind.SMALL_BLOCK_BREAKER) {
         this.processTimeNeeded = Math.max(0, this.processTimeNeeded);
         this.progress = validSavedProgress(this.progress, this.processTimeNeeded);
      }

      if (kind == MachineKind.SMALL_ELECTRICAL_FURNACE) {
         this.powerConsumptionField = Mth.clamp(this.powerConsumptionField, 0, ELECTRIC_MAX_ENERGY_PER_TICK);
      } else {
         this.powerConsumptionField = 0;
      }

      if (this.canHandleFluids(kind)) {
         int capacity = this.capacityFor(kind);
         if (this.fluidAmount <= 0 || !this.canStoreFluidType(kind, this.fluidType)) {
            this.fluidAmount = 0;
            this.fluidType = "";
         } else {
            this.fluidAmount = Mth.clamp(this.fluidAmount, 0, capacity);
         }
      } else {
         this.fluidAmount = 0;
         this.fluidType = "";
      }
   }

   private static int validSavedProgress(int value, int completeAt) {
      return value >= 0 && value < completeAt ? value : 0;
   }

   private int energyCapacity(MachineKind kind) {
      if (kind == MachineKind.SMALL_SOLAR_PANEL) {
         return 64000;
      } else {
         return kind != MachineKind.SMALL_WASTE_INCINERATOR && kind != MachineKind.SMALL_MINERAL_SMELTER && kind != MachineKind.SMALL_FREEZER ? 32000 : 16000;
      }
   }

   private int incineratorInterval(boolean boosted) {
      return boosted ? 10 : 30;
   }

   private boolean validSlot(int slot) {
      return slot >= 0 && slot < this.items.size();
   }

   private boolean validAutomationSlot(int slot) {
      MachineKind kind = this.kind();
      if (kind != null && slot >= 0 && slot < this.visibleSlotCount(kind)) {
         return switch (kind) {
            case METAL_CRAFTING_TABLE -> slot != 10;
            case FACTORY_DROPPER -> slot < 12;
            default -> true;
         };
      } else {
         return false;
      }
   }

   private boolean canAutomationExtract(int slot) {
      MachineKind kind = this.kind();
      if (kind == null || !this.validAutomationSlot(slot)) {
         return false;
      }

      return !hasLayoutResultSlots(kind) || isLayoutResultSlot(kind, slot);
   }

   private int firstSlotWith(Item item) {
      for (int i : this.accessibleSlots()) {
         if (((ItemStack)this.items.get(i)).is(item)) {
            return i;
         }
      }

      return -1;
   }

   private int firstMineralSlot() {
      return this.isMineralInput((ItemStack)this.items.get(0)) ? 0 : -1;
   }

   private boolean isMineralInput(ItemStack stack) {
      return stack.is(Items.STONE)
         || stack.is(Items.COBBLESTONE)
         || stack.is(Items.DEEPSLATE)
         || stack.is(Items.COBBLED_DEEPSLATE)
         || stack.is(Items.GRANITE)
         || stack.is(Items.DIORITE)
         || stack.is(Items.ANDESITE)
         || stack.is(Items.NETHERRACK)
         || stack.is(Items.BLACKSTONE);
   }

   private int firstFreezableSlot() {
      return !this.freezerResult((ItemStack)this.items.get(0)).isEmpty() ? 0 : -1;
   }

   private ItemStack freezerResult(ItemStack stack) {
      if (stack.is(Items.WATER_BUCKET)) {
         return new ItemStack(Items.ICE);
      } else if (stack.is(Items.ICE)) {
         return new ItemStack(Items.PACKED_ICE);
      } else {
         return stack.is(Items.PACKED_ICE) ? new ItemStack(Items.BLUE_ICE) : ItemStack.EMPTY;
      }
   }

   private boolean canAddItem(ItemStack stack) {
      return this.canInsertIntoSlots(stack, this.accessibleSlots());
   }

   private boolean addItem(ItemStack stack) {
      ItemStack remainder = this.insertRemainder(stack);
      return remainder.isEmpty();
   }

   private ItemStack insertRemainder(ItemStack source) {
      if (source.isEmpty()) {
         return ItemStack.EMPTY;
      }

      ItemStack remaining = source.copy();
      int[] slots = this.accessibleSlots();
      boolean insertedAny = false;

      for (int slotIndex : slots) {
         ItemStack slot = (ItemStack)this.items.get(slotIndex);
         if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remaining)) {
            int move = Math.min(remaining.getCount(), Math.min(slot.getMaxStackSize(), this.getMaxStackSize(slot)) - slot.getCount());
            if (move > 0) {
               slot.grow(move);
               remaining.shrink(move);
               this.setChanged();
               insertedAny = true;
               if (remaining.isEmpty()) {
                  this.updateComparator();
                  return ItemStack.EMPTY;
               }
            }
         }
      }

      for (int i : slots) {
         if (((ItemStack)this.items.get(i)).isEmpty()) {
            int move = Math.min(remaining.getCount(), Math.min(remaining.getMaxStackSize(), this.getMaxStackSize(remaining)));
            this.items.set(i, remaining.copyWithCount(move));
            remaining.shrink(move);
            this.setChanged();
            insertedAny = true;
            if (remaining.isEmpty()) {
               this.updateComparator();
               return ItemStack.EMPTY;
            }
         }
      }

      if (insertedAny) {
         this.updateComparator();
      }

      return remaining;
   }

   private void pushOneItem(Direction direction) {
      if (this.level != null && this.level.getBlockEntity(this.worldPosition.relative(direction)) instanceof Container target) {
         for (int slotIndex : this.accessibleSlots()) {
            ItemStack stack = (ItemStack)this.items.get(slotIndex);
            if (!stack.isEmpty()) {
               ItemStack moving = stack.copyWithCount(1);
               if (this.insertIntoContainer(target, moving)) {
                  stack.shrink(1);
                  this.setChanged();
                  target.setChanged();
                  this.updateComparator();
                  return;
               }
            }
         }
      }
   }

   private boolean insertIntoContainer(Container target, ItemStack stack) {
      for (int slot = 0; slot < target.getContainerSize(); slot++) {
         if (target.canPlaceItem(slot, stack)) {
            ItemStack existing = target.getItem(slot);
            if (existing.isEmpty()) {
               target.setItem(slot, stack.copy());
               return true;
            }

            if (ItemStack.isSameItemSameComponents(existing, stack)
               && existing.getCount() < Math.min(existing.getMaxStackSize(), target.getMaxStackSize(existing))) {
               existing.grow(1);
               return true;
            }
         }
      }

      return false;
   }

   private void updateComparator() {
      if (this.level != null) {
         this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
      }
   }

   private record MetalCraftingResult(
      ItemStack output, Optional<RecipeHolder<CraftingRecipe>> recipe, CraftingInput input, boolean plateRecipe, int plateMaterialSlot
   ) {
   }
}
