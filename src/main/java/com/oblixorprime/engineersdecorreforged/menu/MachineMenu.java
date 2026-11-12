package com.oblixorprime.engineersdecorreforged.menu;

import com.oblixorprime.engineersdecorreforged.utility.BarrelReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.SolarV2ReferenceGeometry;

import com.oblixorprime.engineersdecorreforged.ModMenus;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockBreakerDataSync;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineEnergySync;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.oblixorprime.engineersdecorreforged.utility.MachineLayout;
import com.oblixorprime.engineersdecorreforged.utility.FactoryPlacerV2ReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.TreeCutterV3ReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.SmallFluidFunnelFluidSync;
import java.util.Locale;
import java.util.function.IntFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.fluids.FluidStack;

public class MachineMenu extends AbstractContainerMenu {
   // Historical property name retained for compatibility and flag-absent validation only.
   // It no longer selects the Factory Placer presentation: ordinary clients use V2.
   public static final String DEV_FACTORY_PLACER_V2_REFERENCE_PROPERTY = "iedctr.dev.factoryPlacerV2Reference";
   private final boolean factoryPlacerV2ReferencePresentation;
   private final boolean treeCutterV3ReferencePresentation;
   public static final int MACHINE_SLOT_COUNT = 27;
   public static final int DATA_COUNT = 16;
   public static final int SMALL_MINERAL_SMELTER_V05_DATA_COUNT = 17;
   public static final int SMS_UI_FLAGS = 0;
   public static final int SMS_UI_REVISION = 1;
   public static final int SMS_UI_SELECTED_MODE = 2;
   public static final int SMS_UI_BATCH_MODE = 3;
   public static final int SMS_UI_QUEUED_MODE = 4;
   public static final int SMS_UI_THERMAL = 5;
   public static final int SMS_UI_PHASE = 6;
   public static final int SMS_UI_TANK_AMOUNT = 7;
   public static final int SMS_UI_TANK_CAPACITY = 8;
   public static final int SMS_UI_ENERGY = 9;
   public static final int SMS_UI_ENERGY_CAPACITY = 10;
   public static final int SMS_UI_FE_RATE = 11;
   public static final int SMS_UI_REDSTONE = 12;
   public static final int SMS_UI_COMPARATOR = 13;
   public static final int SMS_UI_HAZARD = 14;
   public static final int SMS_UI_STATUS = 15;
   public static final int SMS_UI_BATCH_PROGRESS = 16;
   public static final int FREEZER_STATUS_IDLE = MachineBlockEntity.FREEZER_STATUS_IDLE;
   public static final int FREEZER_STATUS_READY = MachineBlockEntity.FREEZER_STATUS_READY;
   public static final int FREEZER_STATUS_PROCESSING = MachineBlockEntity.FREEZER_STATUS_PROCESSING;
   public static final int FREEZER_STATUS_REDSTONE_PAUSED = MachineBlockEntity.FREEZER_STATUS_REDSTONE_PAUSED;
   public static final int FREEZER_STATUS_WAITING_FOR_ENERGY = MachineBlockEntity.FREEZER_STATUS_WAITING_FOR_ENERGY;
   public static final int FREEZER_STATUS_OUTPUT_BLOCKED = MachineBlockEntity.FREEZER_STATUS_OUTPUT_BLOCKED;
   public static final int FREEZER_STATUS_INVALID_INPUT = MachineBlockEntity.FREEZER_STATUS_INVALID_INPUT;
   public static final int FREEZER_REDSTONE_FLAG = MachineBlockEntity.FREEZER_REDSTONE_FLAG;
   private static final int SCALED_ENERGY_FIELD = 3;
   private static final int FREEZER_ENERGY_FIELD = 3;
   private static final int ELECTRICAL_FURNACE_ENERGY_FIELD = 1;
   private static final int LAB_FURNACE_ENERGY_FIELD = 8;
   private static final int ENERGY_FIELD_SCALE = MachineEnergySync.SCALED_FIELD_STEP_FE;
   private static final int SOLAR_PANEL_ENERGY_CAPACITY = 64000;
   private static final int STANDARD_MACHINE_ENERGY_CAPACITY = 32000;
   private static final int COMPACT_MACHINE_ENERGY_CAPACITY = 16000;
   private static final int DEFAULT_FLUID_CAPACITY = 4000;
   private static final int SMALL_FLUID_FUNNEL_CAPACITY = 3000;
   private static final int BLOCK_BREAKER_FLUID_PRESENT = 1;
   private static final int BLOCK_BREAKER_ACCELERATED = 2;
   private static final int BLOCK_BREAKER_CYCLE_ACTIVE = 4;
   private static final ResourceLocation BLOCK_BREAKER_LUBRICANT_ID = ResourceLocation.fromNamespaceAndPath("immersivepetroleum", "lubricant");
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
   private static final int[] SMELTER_PLAYER_SLOT_X = {27, 48, 68, 88, 109, 129, 150, 170, 191};
   // V5 is the normal client presentation; explicit false retains the diagnostic legacy path.
   public static final String DEV_BARREL_REFERENCE_PROPERTY = "iedctr.dev.barrelV5Reference";
   public static final String DEV_SOLAR_V2_REFERENCE_PROPERTY = "iedctr.dev.solarV2Reference";
   private final boolean solarV2ReferencePresentation;
   public boolean solarV2ReferencePresentation() { return solarV2ReferencePresentation; }
   private final boolean barrelReferencePresentation;
   public boolean barrelReferencePresentation() { return barrelReferencePresentation; }
   private final Player menuPlayer;
   private boolean smelterOutputDrawerOpen;
   private final MachineKind kind;
   private final MachineLayout layout;
   private final int visibleMachineSlots;
   private final Container container;
   private final ContainerData data;
   private final boolean smelterV05Projection;
   private final ContainerLevelAccess menuAccess;
   private final Block expectedMenuBlock;
   private final boolean hasServerMenuAccess;
   private long lastMineralModeRequestTick = Long.MIN_VALUE;

   public MachineMenu(int id, Inventory inventory) {
      this(MachineKind.LABELED_CRATE, id, inventory, new SimpleContainer(27), new SimpleContainerData(16));
   }

   public static MachineMenu client(int id, Inventory inventory, MachineKind kind) {
      return new MachineMenu(kind, id, inventory, new SimpleContainer(27),
         new SimpleContainerData(kind == MachineKind.SMALL_MINERAL_SMELTER ? SMALL_MINERAL_SMELTER_V05_DATA_COUNT : DATA_COUNT),
         kind == MachineKind.FLUID_BARREL && Boolean.parseBoolean(System.getProperty(DEV_BARREL_REFERENCE_PROPERTY, "true")),
         kind == MachineKind.FACTORY_PLACER,
         kind == MachineKind.SMALL_SOLAR_PANEL && Boolean.parseBoolean(System.getProperty(DEV_SOLAR_V2_REFERENCE_PROPERTY, "true")));
   }

   public MachineMenu(MachineKind kind, int id, Inventory inventory, Container container, ContainerData data) {
      this(kind, id, inventory, container, data, false, false, false);
   }

   private MachineMenu(MachineKind kind, int id, Inventory inventory, Container container, ContainerData data,
                       boolean barrelReferenceRequested, boolean factoryPlacerV2ReferenceRequested,
                       boolean solarV2ReferenceRequested) {
      super((MenuType)resolveMenuType(kind), id);
      this.solarV2ReferencePresentation = solarV2ReferenceRequested
         && kind == MachineKind.SMALL_SOLAR_PANEL && inventory.player.level().isClientSide;
      this.barrelReferencePresentation = barrelReferenceRequested
         && kind == MachineKind.FLUID_BARREL && inventory.player.level().isClientSide;
      this.factoryPlacerV2ReferencePresentation = factoryPlacerV2ReferenceRequested
         && kind == MachineKind.FACTORY_PLACER && inventory.player.level().isClientSide;
      this.treeCutterV3ReferencePresentation = kind == MachineKind.SMALL_TREE_CUTTER && inventory.player.level().isClientSide;
      checkContainerSize(container, 27);
      checkContainerDataCount(data, kind == MachineKind.SMALL_MINERAL_SMELTER ? SMALL_MINERAL_SMELTER_V05_DATA_COUNT : DATA_COUNT);
      this.kind = kind;
      this.menuPlayer = inventory.player;
      this.layout = MachineLayout.forKind(kind);
      this.visibleMachineSlots = this.layout.machineSlots().size();
      this.container = container;
      this.data = data;
      this.smelterV05Projection = kind == MachineKind.SMALL_MINERAL_SMELTER
         && data.getCount() == SMALL_MINERAL_SMELTER_V05_DATA_COUNT;
      if (kind == MachineKind.SMALL_MINERAL_SMELTER && container instanceof MachineBlockEntity machine
         && machine.getLevel() != null) {
         this.menuAccess = ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos());
         this.expectedMenuBlock = ModBlocks.SMALL_MINERAL_SMELTER.get();
         this.hasServerMenuAccess = true;
      } else {
         this.menuAccess = ContainerLevelAccess.NULL;
         this.expectedMenuBlock = Blocks.AIR;
         this.hasServerMenuAccess = false;
      }
      container.startOpen(inventory.player);

      for (MachineLayout.MachineSlot slot : this.layout.machineSlots()) {
         MachineLayout.MachineSlot presentationSlot = slot;
         if (barrelReferencePresentation) {
            var anchor = BarrelReferenceGeometry.anchor(slot.containerIndex());
            presentationSlot = new MachineLayout.MachineSlot(slot.containerIndex(), anchor.x(), anchor.y(), slot.role(), slot.output());
         } else if (factoryPlacerV2ReferencePresentation) {
            var anchor = FactoryPlacerV2ReferenceGeometry.anchor(slot.containerIndex());
            presentationSlot = new MachineLayout.MachineSlot(slot.containerIndex(), anchor.x(), anchor.y(), slot.role(), slot.output());
         }
         if (treeCutterV3ReferencePresentation) {
            var anchor = TreeCutterV3ReferenceGeometry.anchor(slot.containerIndex());
            presentationSlot = new MachineLayout.MachineSlot(slot.containerIndex(), anchor.x(), anchor.y(), slot.role(), slot.output());
         }
         if (solarV2ReferencePresentation) {
            var anchor = SolarV2ReferenceGeometry.anchor(slot.containerIndex());
            presentationSlot = new MachineLayout.MachineSlot(slot.containerIndex(), anchor.x(), anchor.y(), slot.role(), slot.output());
         }
         this.addSlot(this.createMachineSlot(presentationSlot));
      }

      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            int x = kind == MachineKind.SMALL_MINERAL_SMELTER
               ? SMELTER_PLAYER_SLOT_X[column] : this.layout.playerSlotX(column);
            int y = this.layout.playerSlotY(row);
            if (barrelReferencePresentation) {
               var anchor = BarrelReferenceGeometry.anchor(2 + row * 9 + column);
               x = anchor.x(); y = anchor.y();
            } else if (factoryPlacerV2ReferencePresentation) {
               var anchor = FactoryPlacerV2ReferenceGeometry.anchor(18 + row * 9 + column);
               x = anchor.x(); y = anchor.y();
            }
            if (treeCutterV3ReferencePresentation) {
               var anchor = TreeCutterV3ReferenceGeometry.anchor(1 + row * 9 + column);
               x = anchor.x(); y = anchor.y();
            }
            if (solarV2ReferencePresentation) {
               var anchor = SolarV2ReferenceGeometry.anchor(1 + row * 9 + column);
               x = anchor.x(); y = anchor.y();
            }
            this.addSlot(new Slot(inventory, column + row * 9 + 9, x, y));
         }
      }

      for (int column = 0; column < 9; column++) {
         int x = kind == MachineKind.SMALL_MINERAL_SMELTER
            ? SMELTER_PLAYER_SLOT_X[column] : this.layout.hotbarSlotX(column);
         int y = this.layout.hotbarY();
         if (barrelReferencePresentation) {
            var anchor = BarrelReferenceGeometry.anchor(29 + column);
            x = anchor.x(); y = anchor.y();
         } else if (factoryPlacerV2ReferencePresentation) {
            var anchor = FactoryPlacerV2ReferenceGeometry.anchor(45 + column);
            x = anchor.x(); y = anchor.y();
         }
         if (treeCutterV3ReferencePresentation) {
            var anchor = TreeCutterV3ReferenceGeometry.anchor(28 + column);
            x = anchor.x(); y = anchor.y();
         }
         if (solarV2ReferencePresentation) {
            var anchor = SolarV2ReferenceGeometry.anchor(28 + column);
            x = anchor.x(); y = anchor.y();
         }
         this.addSlot(new Slot(inventory, column, x, y));
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

   public boolean treeCutterV3ReferencePresentation() { return this.treeCutterV3ReferencePresentation; }

   public boolean factoryPlacerV2ReferencePresentation() { return this.factoryPlacerV2ReferencePresentation; }

   public MachineKind kind() {
      return this.kind;
   }

   public MachineLayout layout() {
      return this.layout;
   }

   public int visibleMachineSlots() {
      return this.visibleMachineSlots;
   }

   /** Only client presentation is gated; the server always exposes the output slot. */
   public void setSmelterOutputDrawerOpen(boolean open) {
      this.smelterOutputDrawerOpen = this.kind == MachineKind.SMALL_MINERAL_SMELTER && open;
   }

   public int progress() {
      return switch (this.kind) {
         case SMALL_LAB_FURNACE, SMALL_ELECTRICAL_FURNACE -> scaledProgress(this.data.get(2), this.data.get(3));
         case SMALL_BLOCK_BREAKER -> scaledProgress(this.data.get(1), this.data.get(3));
         case SMALL_MINERAL_SMELTER -> this.smelterV05Projection
            ? this.data.get(SMS_UI_BATCH_PROGRESS) < 0 ? -1 : clampPercent(this.data.get(SMS_UI_BATCH_PROGRESS))
            : clampPercent(this.data.get(1));
         case SMALL_FREEZER -> scaledProgress(this.data.get(1), TEMPERATURE_PROCESS_TICKS);
         default -> clampPercent(this.data.get(1));
      };
   }

   public int fluidAmount() {
      return this.kind == MachineKind.SMALL_MINERAL_SMELTER && this.smelterV05Projection
         ? this.data.get(SMS_UI_TANK_AMOUNT) : this.data.get(2);
   }

   public int fluidCapacity() {
      return switch (this.kind) {
         case SMALL_FLUID_FUNNEL -> SMALL_FLUID_FUNNEL_CAPACITY;
         case FLUID_BARREL -> 32000;
         case SMALL_BLOCK_BREAKER -> MachineBlockEntity.BLOCK_BREAKER_LUBRICANT_CAPACITY;
         case SMALL_MINERAL_SMELTER -> this.smelterV05Projection ? this.data.get(SMS_UI_TANK_CAPACITY) : DEFAULT_FLUID_CAPACITY;
         case PASSIVE_FLUID_ACCUMULATOR, SMALL_MILKING_MACHINE -> DEFAULT_FLUID_CAPACITY;
         default -> 0;
      };
   }

   public int smallFluidFunnelFluidCode() {
      return this.kind == MachineKind.SMALL_FLUID_FUNNEL
         ? SmallFluidFunnelFluidSync.normalize(this.data.get(11))
         : 0;
   }

   public Fluid storedFluid() {
      if (this.kind == MachineKind.SMALL_BLOCK_BREAKER) {
         return (this.data.get(4) & BLOCK_BREAKER_FLUID_PRESENT) != 0 && this.data.get(2) > 0
            ? BuiltInRegistries.FLUID.get(BLOCK_BREAKER_LUBRICANT_ID) : Fluids.EMPTY;
      }
      return decodeStoredFluid(this.kind, this.fluidAmount(), this.data.get(11), this.data.get(12), this.data.get(13));
   }

   public boolean blockBreakerAccelerated() {
      return this.kind == MachineKind.SMALL_BLOCK_BREAKER && (this.data.get(4) & BLOCK_BREAKER_ACCELERATED) != 0;
   }

   public boolean blockBreakerCycleActive() {
      return this.kind == MachineKind.SMALL_BLOCK_BREAKER && (this.data.get(4) & BLOCK_BREAKER_CYCLE_ACTIVE) != 0;
   }

   public FluidStack blockBreakerFluidStack() {
      Fluid fluid = this.storedFluid();
      return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, this.fluidAmount());
   }

   public Component blockBreakerFluidName() {
      FluidStack stack = this.blockBreakerFluidStack();
      return stack.isEmpty()
         ? Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.fluid_name")
         : stack.getHoverName();
   }

   public int blockBreakerDurationTicks() {
      if (this.kind != MachineKind.SMALL_BLOCK_BREAKER) return 0;
      int synced = this.data.get(3);
      if (synced > 0) return synced;
      float hardness = this.blockBreakerHardness();
      if (!Float.isFinite(hardness) || hardness < 0.0F || this.blockBreakerTargetStack().isEmpty()) return 0;
      long normalTicks = 20L + Math.round(hardness * 20.0F);
      int normal = (int)Math.max(20L, Math.min(200L, normalTicks));
      return this.fluidAmount() >= MachineBlockEntity.BLOCK_BREAKER_LUBRICANT_PER_SUCCESS
         ? Math.max(20, (normal + 1) / 2) : normal;
   }

   static Fluid decodeStoredFluid(MachineKind kind, int amount, int low, int middle, int high) {
      return decodeStoredFluid(kind, amount, low, middle, high, BuiltInRegistries.FLUID.size(), BuiltInRegistries.FLUID::byId, Fluids.EMPTY);
   }

   static <T> T decodeStoredFluid(MachineKind kind, int amount, int low, int middle, int high, int registrySize, IntFunction<T> valueById, T emptyValue) {
      if (kind != MachineKind.FLUID_BARREL || amount <= 0) {
         return emptyValue;
      }

      int registryId = decodeFluidRegistryId(low, middle, high);
      if (registryId < 0 || registryId >= registrySize) {
         return emptyValue;
      }
      T value = valueById.apply(registryId);
      return value == null ? emptyValue : value;
   }

   static int decodeFluidRegistryId(int low, int middle, int high) {
      return (low & 0x7FFF) | ((middle & 0x7FFF) << 15) | ((high & 0x7FFF) << 30);
   }


   public int energyStored() {
      return switch (this.kind) {
         case SMALL_ELECTRICAL_FURNACE -> this.data.get(ELECTRICAL_FURNACE_ENERGY_FIELD);
         case SMALL_LAB_FURNACE -> this.data.get(LAB_FURNACE_ENERGY_FIELD);
            case SMALL_MINERAL_SMELTER -> this.smelterV05Projection ? this.data.get(SMS_UI_ENERGY) : this.data.get(6);
            case SMALL_FREEZER -> this.data.get(FREEZER_ENERGY_FIELD);
            case SMALL_SOLAR_PANEL, SMALL_WASTE_INCINERATOR -> this.data.get(SCALED_ENERGY_FIELD) * ENERGY_FIELD_SCALE;
          case SMALL_BLOCK_BREAKER -> this.data.get(10);
         default -> 0;
      };
   }

   public int energyCapacity() {
      return switch (this.kind) {
         case SMALL_SOLAR_PANEL -> SOLAR_PANEL_ENERGY_CAPACITY;
         case SMALL_MINERAL_SMELTER -> this.smelterV05Projection ? this.data.get(SMS_UI_ENERGY_CAPACITY) : COMPACT_MACHINE_ENERGY_CAPACITY;
         case SMALL_WASTE_INCINERATOR, SMALL_FREEZER -> COMPACT_MACHINE_ENERGY_CAPACITY;
         case SMALL_LAB_FURNACE, SMALL_ELECTRICAL_FURNACE -> STANDARD_MACHINE_ENERGY_CAPACITY;
          case SMALL_BLOCK_BREAKER -> 32000;
         default -> 0;
      };
   }

   public int freezerProgressTicks() {
      return this.kind == MachineKind.SMALL_FREEZER
         ? Math.max(0, Math.min(MachineBlockEntity.FREEZER_PROCESS_TICKS, this.data.get(1)))
         : 0;
   }

   public int freezerPhase() {
      return this.kind == MachineKind.SMALL_FREEZER ? Math.max(0, Math.min(4, this.data.get(14))) : 0;
   }

   public int freezerStatus() {
      return this.kind == MachineKind.SMALL_FREEZER ? this.data.get(15) & 0xFF : FREEZER_STATUS_IDLE;
   }

   public boolean freezerRedstonePowered() {
      return this.kind == MachineKind.SMALL_FREEZER && (this.data.get(15) & FREEZER_REDSTONE_FLAG) != 0;
   }

   public ItemStack freezerPreviewStack() {
      if (this.kind != MachineKind.SMALL_FREEZER || this.visibleMachineSlots == 0) return ItemStack.EMPTY;
      ItemStack input = ((Slot)this.slots.get(0)).getItem();
      // Field 2 was unused for the Freezer; preserve the existing 16-field/39-slot menu protocol.
      return input.isEmpty() && this.data.get(2) >= 1000
         ? new ItemStack(Items.ICE) : MachineBlockEntity.freezerResultForDisplay(input);
   }

   public int energyDisplayGranularity() {
      return MachineEnergySync.displayGranularity(this.kind);
   }

   public int comparatorOutput() {
      return this.kind == MachineKind.SMALL_MINERAL_SMELTER && this.smelterV05Projection
         ? this.data.get(SMS_UI_COMPARATOR) : this.data.get(4);
   }

   public int activityTicks() {
      return this.data.get(5);
   }

   public int blockBreakerStatus() {
      return this.kind == MachineKind.SMALL_BLOCK_BREAKER ? this.data.get(6) : 0;
   }

   public float blockBreakerHardness() {
      return this.kind == MachineKind.SMALL_BLOCK_BREAKER
         ? MachineBlockBreakerDataSync.hardnessFromFields(
            this.data.get(MachineBlockBreakerDataSync.HARDNESS_LOW_FIELD),
            this.data.get(MachineBlockBreakerDataSync.HARDNESS_MIDDLE_FIELD),
            this.data.get(MachineBlockBreakerDataSync.HARDNESS_HIGH_FIELD)
         )
         : 0.0F;
   }

   public int blockBreakerEnergyCostPerBlock() {
      return this.kind == MachineKind.SMALL_BLOCK_BREAKER && !this.blockBreakerTargetStack().isEmpty() && this.blockBreakerHardness() >= 0.0F
         ? MachineBlockEntity.blockBreakerEnergyCostPerBlock(this.blockBreakerHardness())
         : 0;
   }

   public int blockBreakerPendingItems() {
      return this.kind == MachineKind.SMALL_BLOCK_BREAKER ? this.data.get(8) : 0;
   }

   public boolean blockBreakerRedstonePowered() {
      return this.kind == MachineKind.SMALL_BLOCK_BREAKER && this.data.get(9) != 0;
   }

   public String blockBreakerTargetName() {
      Block target = this.blockBreakerTargetBlock();
      return target == Blocks.AIR ? "No target" : Component.translatable(target.getDescriptionId()).getString();
   }

   public ItemStack blockBreakerTargetStack() {
      Item item = this.blockBreakerTargetBlock().asItem();
      return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
   }

   private Block blockBreakerTargetBlock() {
      if (this.kind != MachineKind.SMALL_BLOCK_BREAKER) {
         return Blocks.AIR;
      }
      // The three positive 15-bit components survive short-based container-data sync without truncation.
      int registryId = (this.data.get(11) & 0x7FFF)
         | ((this.data.get(12) & 0x7FFF) << 15)
         | ((this.data.get(13) & 0x7FFF) << 30);
      if (registryId < 0 || registryId >= BuiltInRegistries.BLOCK.size()) {
         return Blocks.AIR;
      }
      Block target = BuiltInRegistries.BLOCK.byId(registryId);
      return target == null ? Blocks.AIR : target;
   }

   public int field(int index) {
      return index >= 0 && index < this.data.getCount() ? this.data.get(index) : 0;
   }

   public void handleAction(Player player, int action, int valueA, int valueB) {
      if (action == 31) return; // Smelter mode changes use the v0.5 dedicated payload only.
      if (this.container instanceof MachineBlockEntity machine) {
         machine.handleGuiAction(player, action, valueA, valueB);
      }
   }

   public boolean handleMineralModeRequest(Player player, int expectedRevision, int modeId) {
      if (player == null || this.kind != MachineKind.SMALL_MINERAL_SMELTER || !this.smelterV05Projection
         || player.containerMenu != this || player.isSpectator() || !this.stillValid(player)
         || expectedRevision < 0 || expectedRevision >= 30000 || modeId < 0 || modeId > 2
         || (this.data.get(SMS_UI_FLAGS) & 2) == 0) {
         return false;
      }
      long currentTick = player.level().getGameTime();
      if (this.lastMineralModeRequestTick != Long.MIN_VALUE
         && currentTick - this.lastMineralModeRequestTick < 1L) {
         this.broadcastChanges();
         return false;
      }
      boolean accepted = this.container instanceof MachineBlockEntity machine
         && machine.requestMineralModeFromGui(player, modeId, expectedRevision);
      if (accepted) this.lastMineralModeRequestTick = currentTick;
      else this.broadcastChanges();
      return accepted;
   }

   public boolean isMineralSmelterMenuFor(MachineBlockEntity machine) {
      return this.kind == MachineKind.SMALL_MINERAL_SMELTER && this.smelterV05Projection && this.container == machine;
   }

   @Override
   public boolean clickMenuButton(Player player, int id) {
      return this.kind == MachineKind.SMALL_MINERAL_SMELTER ? false : super.clickMenuButton(player, id);
   }

   public boolean stillValid(Player player) {
      if (this.kind == MachineKind.SMALL_MINERAL_SMELTER) {
         return this.hasServerMenuAccess && AbstractContainerMenu.stillValid(this.menuAccess, player, this.expectedMenuBlock);
      }
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

      }
      if (this.kind == MachineKind.SMALL_MINERAL_SMELTER && slot.containerIndex() == 2) {
         return new MachineMenu.OutputMachineSlot(this.container, slot.containerIndex(), slot.x(), slot.y()) {
            @Override
            public boolean isActive() {
               // The drawer is only a client-side visual gate, not a server permission.
               return !MachineMenu.this.menuPlayer.level().isClientSide || MachineMenu.this.smelterOutputDrawerOpen;
            }
         };
      }
      return slot.output()
         ? new MachineMenu.OutputMachineSlot(this.container, slot.containerIndex(), slot.x(), slot.y())
            : new MachineMenu.ValidatedMachineSlot(
                this.container,
                slot.containerIndex(),
                slot.x(),
                slot.y(),
                this.kind == MachineKind.SMALL_FREEZER && slot.containerIndex() == 1
            );
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
      private final boolean insertionDisabled;

      ValidatedMachineSlot(Container container, int slot, int x, int y) {
         this(container, slot, x, y, false);
      }

      ValidatedMachineSlot(Container container, int slot, int x, int y, boolean insertionDisabled) {
         super(container, slot, x, y);
         this.insertionDisabled = insertionDisabled;
      }

      public boolean mayPlace(ItemStack stack) {
         return !this.insertionDisabled && this.container.canPlaceItem(this.getSlotIndex(), stack);
      }
   }
}
