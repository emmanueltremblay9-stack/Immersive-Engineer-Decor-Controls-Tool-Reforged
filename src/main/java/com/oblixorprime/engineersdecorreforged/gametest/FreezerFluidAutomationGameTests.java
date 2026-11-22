package com.oblixorprime.engineersdecorreforged.gametest;

import blusunrize.immersiveengineering.common.blocks.metal.FluidPipeBlockEntity;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Public capabilities and real server ticks, including the pinned IE pipe implementation. */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FreezerFluidAutomationGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 3, 3);

   private static MachineBlockEntity place(GameTestHelper h) {
      BlockPos pos = h.absolutePos(CENTER);
      h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      h.getLevel().setBlock(pos, ModBlocks.SMALL_FREEZER.get().defaultBlockState(), 3);
      return (MachineBlockEntity)h.getLevel().getBlockEntity(pos);
   }

   private static IFluidHandler tank(GameTestHelper h, MachineBlockEntity machine, Direction side) {
      IFluidHandler result = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, machine.getBlockPos(), side);
      h.assertTrue(result != null, "registered Freezer fluid capability exists on " + side);
      return result;
   }

   private static IItemHandler items(GameTestHelper h, MachineBlockEntity machine, Direction side) {
      return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, machine.getBlockPos(), side);
   }

   private static FluidStack water(int amount) { return new FluidStack(Fluids.WATER, amount); }
   private static int amount(GameTestHelper h, MachineBlockEntity machine) { return tank(h, machine, Direction.UP).getFluidInTank(0).getAmount(); }
   private static int energy(MachineBlockEntity machine) { return machine.energyCapability(null).getEnergyStored(); }
   private static int progress(MachineBlockEntity machine) { return machine.dataAccessForTests().get(1); }
   private static void ticks(GameTestHelper h, MachineBlockEntity machine, int count) {
      for (int i = 0; i < count; i++) MachineBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
   }
   private static void power(MachineBlockEntity machine) { machine.energyCapability(null).receiveEnergy(16000, false); }
   private static MachineMenu menu(GameTestHelper h, MachineBlockEntity machine) {
      return new MachineMenu(MachineKind.SMALL_FREEZER, 1, h.makeMockPlayer(GameType.CREATIVE).getInventory(), machine, machine.dataAccessForTests());
   }
   private static MachineBlockEntity reload(GameTestHelper h, CompoundTag saved) {
      MachineBlockEntity loaded = place(h);
      loaded.loadWithComponents(saved, h.getLevel().registryAccess());
      return loaded;
   }

   @GameTest(template = "empty")
   public static void freezer_public_fluid_port_is_top_input_only_and_keeps_capacity_types_and_simulation(GameTestHelper h) {
      MachineBlockEntity machine = place(h);
      BlockPos pos = machine.getBlockPos();
      IFluidHandler handler = tank(h, machine, Direction.UP);
      for (Direction side : Direction.values()) {
         if (side == Direction.UP) continue;
         h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, side) == null,
            "Freezer has no public fluid port on " + side);
      }
      h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null) == null,
         "Freezer has no unsided public fluid port");

      h.assertValueEqual(handler.getTanks(), 1, "one tank on the upper input port");
      h.assertValueEqual(handler.getTankCapacity(0), 2000, "upstream two-bucket capacity");
      h.assertValueEqual(handler.getTankCapacity(1), 0, "invalid tank has no capacity");
      h.assertTrue(handler.getFluidInTank(-1).isEmpty(), "invalid tank is empty");
      h.assertFalse(handler.isFluidValid(1, water(1)), "invalid tank rejects water");
      h.assertTrue(handler.isFluidValid(0, water(1)), "water is valid");
      h.assertFalse(handler.isFluidValid(0, FluidStack.EMPTY), "empty fluid is invalid");
      h.assertValueEqual(handler.fill(water(3000), FluidAction.SIMULATE), 2000, "simulated fill clamps to capacity");
      h.assertValueEqual(amount(h, machine), 0, "simulation does not fill");
      h.assertValueEqual(handler.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE), 0, "lava rejected");
      h.assertValueEqual(handler.fill(new FluidStack(NeoForgeMod.MILK.get(), 1000), FluidAction.EXECUTE), 0, "milk rejected");

      FluidStack markedWater = water(1000);
      CompoundTag marker = new CompoundTag(); marker.putInt("freezer_test", 7);
      markedWater.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
      h.assertFalse(handler.isFluidValid(0, markedWater), "unsupported fluid components must not be discarded");
      h.assertValueEqual(handler.fill(markedWater, FluidAction.EXECUTE), 0, "component-bearing water is left with sender");
      h.assertValueEqual(handler.fill(water(375), FluidAction.EXECUTE), 375, "partial fill retained");
      h.assertValueEqual(handler.fill(water(2000), FluidAction.EXECUTE), 1625, "overflow remains with sender");
      FluidStack copy = handler.getFluidInTank(0); copy.setAmount(1);
      h.assertValueEqual(amount(h, machine), 2000, "query stack cannot mutate tank");

      h.assertTrue(handler.drain(markedWater, FluidAction.EXECUTE).isEmpty(), "upper input port refuses component-mismatched drain");
      h.assertTrue(handler.drain(new FluidStack(Fluids.LAVA, 1), FluidAction.EXECUTE).isEmpty(), "upper input port refuses wrong-fluid drain");
      h.assertTrue(handler.drain(water(125), FluidAction.SIMULATE).isEmpty(), "upper input port refuses simulated drain");
      h.assertTrue(handler.drain(water(125), FluidAction.EXECUTE).isEmpty(), "upper input port refuses executed stack drain");
      h.assertTrue(handler.drain(5000, FluidAction.SIMULATE).isEmpty(), "upper input port refuses simulated amount drain");
      h.assertTrue(handler.drain(5000, FluidAction.EXECUTE).isEmpty(), "upper input port refuses amount drain");
      h.assertTrue(handler.drain(0, FluidAction.EXECUTE).isEmpty(), "zero drain is inert");
      h.assertTrue(handler.drain(-1, FluidAction.EXECUTE).isEmpty(), "negative drain is inert");
      h.assertValueEqual(amount(h, machine), 2000, "input-only port cannot remove stored water");
      h.succeed();
   }
   @GameTest(template = "empty")
   public static void freezer_partial_water_waits_then_pays_one_exact_cycle(GameTestHelper h) {
      MachineBlockEntity machine = place(h); power(machine);
      IFluidHandler handler = tank(h, machine, Direction.UP);
      handler.fill(water(999), FluidAction.EXECUTE);
      ticks(h, machine, 200);
      h.assertValueEqual(progress(machine), 0, "partial bucket cannot begin work");
      h.assertValueEqual(energy(machine), 16000, "partial bucket consumes no FE");
      handler.fill(water(1), FluidAction.EXECUTE);
      ticks(h, machine, 179);
      h.assertValueEqual(progress(machine), 179, "work reaches last incomplete tick");
      h.assertValueEqual(amount(h, machine), 1000, "water is not spent before completion");
      h.assertTrue(machine.getItem(2).isEmpty(), "no early ice");
      ticks(h, machine, 1);
      h.assertTrue(machine.getItem(2).is(Items.ICE) && machine.getItem(2).getCount() == 1, "one ice at tick 180");
      h.assertValueEqual(amount(h, machine), 0, "one bucket of water consumed");
      h.assertValueEqual(energy(machine), 16000 - 1440, "same exact FE cost as item recipe");
      h.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(1).isEmpty(), "fluid path never creates a bucket or touches reserved slot");
      h.assertValueEqual(progress(machine), 0, "completed cycle resets");
      ticks(h, machine, 200);
      h.assertValueEqual(machine.getItem(2).getCount(), 1, "empty tank cannot mint more ice");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void freezer_two_water_batches_produce_two_ice_without_free_upgrades(GameTestHelper h) {
      MachineBlockEntity machine = place(h); power(machine);
      tank(h, machine, Direction.UP).fill(water(2000), FluidAction.EXECUTE);
      ticks(h, machine, 360);
      h.assertTrue(machine.getItem(2).is(Items.ICE), "output is not automatically upgraded");
      h.assertValueEqual(machine.getItem(2).getCount(), 2, "two paid water batches");
      h.assertValueEqual(amount(h, machine), 0, "two buckets consumed");
      h.assertValueEqual(energy(machine), 16000 - 2880, "two full FE charges");
      ticks(h, machine, 180);
      h.assertValueEqual(machine.getItem(2).getCount(), 2, "no third batch or output-slot processing");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void freezer_internal_source_loss_invalidates_only_lost_active_water(GameTestHelper h) {
      MachineBlockEntity machine = place(h); power(machine);
      IFluidHandler publicTop = tank(h, machine, Direction.UP);
      // This handler is a direct internal test hook for simulating an in-tick source mutation.
      // It is deliberately not used as evidence of a public port or an IE pipe connection.
      IFluidHandler internalMutation = machine.fluidHandler(null);
      h.assertTrue(internalMutation != null, "internal mutation hook exists separately from the public top port");
      publicTop.fill(water(2000), FluidAction.EXECUTE); ticks(h, machine, 179);
      CompoundTag before = machine.saveWithFullMetadata(h.getLevel().registryAccess());
      internalMutation.drain(2000, FluidAction.SIMULATE);
      internalMutation.fill(water(1), FluidAction.SIMULATE);
      h.assertValueEqual(machine.saveWithFullMetadata(h.getLevel().registryAccess()), before,
         "simulated internal mutations preserve active work exactly");
      internalMutation.drain(1000, FluidAction.EXECUTE);
      h.assertValueEqual(progress(machine), 179, "a complete remaining batch retains its work");
      internalMutation.drain(water(1), FluidAction.EXECUTE);
      h.assertValueEqual(progress(machine), 0, "lost source resets before another server tick");
      internalMutation.fill(water(1), FluidAction.EXECUTE);
      ticks(h, machine, 1);
      h.assertTrue(machine.getItem(2).isEmpty(), "same-tick internal drain/refill cannot inherit work");
      h.assertValueEqual(progress(machine), 1, "replacement starts from the first paid tick");
      ticks(h, machine, 179);
      h.assertTrue(machine.getItem(2).is(Items.ICE), "replacement finishes after its own full cycle");
      h.assertValueEqual(amount(h, machine), 0, "replacement batch is consumed exactly once");
      h.succeed();
   }
   @GameTest(template = "empty")
   public static void freezer_item_and_water_sources_cannot_exchange_partial_work(GameTestHelper h) {
      for (Item input : new Item[]{Items.WATER_BUCKET, Items.ICE, Items.PACKED_ICE}) {
         MachineBlockEntity machine = place(h); power(machine);
         tank(h, machine, Direction.UP).fill(water(2000), FluidAction.EXECUTE); ticks(h, machine, 179);
         machine.setItem(0, new ItemStack(input)); ticks(h, machine, 1);
         h.assertValueEqual(progress(machine), 1, "item starts fresh after fluid work: " + input);
         h.assertTrue(machine.getItem(2).isEmpty(), "no early item output");
         h.assertValueEqual(amount(h, machine), 2000, "item selection does not spend tank water");
         ticks(h, machine, 178);
         machine.setItem(0, ItemStack.EMPTY); ticks(h, machine, 1);
         h.assertValueEqual(progress(machine), 1, "fluid starts fresh after item work");
         h.assertTrue(machine.getItem(2).isEmpty(), "no early fluid output");
         ticks(h, machine, 179);
         h.assertTrue(machine.getItem(2).is(Items.ICE), "new fluid cycle produces ordinary ice");
         h.assertValueEqual(amount(h, machine), 1000, "only fluid completion consumes water");
      }
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void freezer_output_full_keeps_existing_reset_assertions(GameTestHelper h) {
      for (int failure = 0; failure < 2; failure++) {
         MachineBlockEntity machine = place(h); power(machine);
         tank(h, machine, Direction.UP).fill(water(1000), FluidAction.EXECUTE); ticks(h, machine, 179);
         int before = energy(machine);
         if (failure == 0) machine.setItem(2, new ItemStack(Items.ICE, 64));
         if (failure == 1) machine.setItem(2, new ItemStack(Items.PACKED_ICE));
         ticks(h, machine, 1);
         h.assertValueEqual(progress(machine), 0, "existing output-full rule resets work: " + failure);
         h.assertValueEqual(amount(h, machine), 1000, "blocked output retains its water");
         h.assertValueEqual(energy(machine), before, "blocked output spends no FE");
         machine.setItem(2, ItemStack.EMPTY); power(machine);
         ticks(h, machine, 179);
         h.assertTrue(machine.getItem(2).isEmpty(), "unblocking does not restore discarded work");
         ticks(h, machine, 1);
         h.assertTrue(machine.getItem(2).is(Items.ICE), "unblocked cycle completes after 180 paid ticks");
      }
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void freezer_redstone_and_insufficient_fe_keep_confirmed_reset_rules(GameTestHelper h) {
      for (int failure = 2; failure < 4; failure++) {
         MachineBlockEntity machine = place(h); power(machine);
         tank(h, machine, Direction.UP).fill(water(1000), FluidAction.EXECUTE); ticks(h, machine, 179);
         int before = energy(machine);
         if (failure == 2) h.getLevel().setBlock(machine.getBlockPos().west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
         if (failure == 3) machine.dataAccessForTests().set(3, 7);
         ticks(h, machine, 1);
         h.assertValueEqual(progress(machine), 0, "confirmed redstone/FE rule resets work: " + failure);
         h.assertValueEqual(amount(h, machine), 1000, "blocked cycle retains its water");
         h.assertValueEqual(energy(machine), failure == 3 ? 7 : before, "blocked cycle spends no FE");
         h.getLevel().setBlock(machine.getBlockPos().west(), Blocks.AIR.defaultBlockState(), 3);
         power(machine);
         ticks(h, machine, 179);
         h.assertTrue(machine.getItem(2).isEmpty(), "unblocking does not restore discarded work");
         ticks(h, machine, 1);
         h.assertTrue(machine.getItem(2).is(Items.ICE), "unblocked cycle completes after 180 paid ticks");
      }
      h.succeed();
   }
   @GameTest(template = "empty")
   public static void freezer_bucket_recycling_preserves_item_recipes_and_restricts_inputs(GameTestHelper h) {
      MachineBlockEntity machine = place(h); power(machine);
      tank(h, machine, Direction.UP).fill(water(2000), FluidAction.EXECUTE);
      IItemHandler input = items(h, machine, Direction.UP);
      IItemHandler output = items(h, machine, Direction.DOWN);
      BlockPos pos = machine.getBlockPos();
      h.assertValueEqual(input.getSlots(), 1, "top item port exposes only the input");
      h.assertValueEqual(output.getSlots(), 2, "bottom item port exposes output and returned bucket only");
      for (Direction side : Direction.values()) {
         if (side == Direction.UP || side == Direction.DOWN) continue;
         h.assertTrue(h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, side) == null,
            "Freezer has no public item port on " + side);
      }
      h.assertTrue(input.insertItem(0, new ItemStack(Items.WATER_BUCKET), false).isEmpty(),
         "water bucket inserts through the public top item capability");
      for (Direction side : Direction.values()) {
         h.assertTrue(input.extractItem(0, 1, false).isEmpty(), "top input handler never extracts the full bucket");
         h.assertFalse(machine.canTakeItemThroughFace(0, machine.getItem(0), side),
            "full water bucket cannot leave through " + side);
      }
      h.assertTrue(output.extractItem(0, 1, false).isEmpty(), "no ice output before the timed recipe");
      h.assertTrue(output.extractItem(1, 1, false).isEmpty(), "no returned empty bucket before processing");
      h.assertFalse(machine.canTakeItem(new SimpleContainer(1), 0, machine.getItem(0)),
         "arbitrary Container cannot pull a full input bucket");
      ticks(h, machine, 180);
      h.assertTrue(machine.getItem(0).is(Items.BUCKET) && machine.getItem(2).is(Items.ICE),
         "bucket recipe conserves the bucket and produces ice");
      h.assertValueEqual(amount(h, machine), 2000, "bucket recipe never also charges tank water");
      h.assertTrue(output.getStackInSlot(1).is(Items.BUCKET), "bottom output port exposes the returned bucket");
      h.assertTrue(output.extractItem(1, 1, true).is(Items.BUCKET), "empty bucket extraction simulates");
      h.assertTrue(machine.getItem(0).is(Items.BUCKET), "simulation preserves empty bucket");
      for (Direction side : Direction.values()) {
         h.assertValueEqual(machine.canTakeItemThroughFace(0, machine.getItem(0), side), side == Direction.DOWN,
            "only the bottom face can extract the returned bucket");
      }
      h.assertFalse(machine.canTakeItem(new SimpleContainer(1), 0, machine.getItem(0)),
         "arbitrary Container cannot pull a bucket without being the block below");
      h.assertTrue(output.extractItem(1, 1, false).is(Items.BUCKET), "empty bucket extraction executes");
      h.assertTrue(output.extractItem(1, 1, false).isEmpty(), "bucket is not duplicated");
      h.assertTrue(output.extractItem(0, 1, false).is(Items.ICE), "ice extraction still works");

      for (Item item : new Item[]{Items.ICE, Items.PACKED_ICE}) {
         h.assertTrue(input.insertItem(0, new ItemStack(item), false).isEmpty(), "solid recipe input uses the top port");
         h.assertTrue(input.extractItem(0, 1, false).isEmpty(), "solid recipe input remains protected");
         ticks(h, machine, 180);
         Item expected = item == Items.ICE ? Items.PACKED_ICE : Items.BLUE_ICE;
         h.assertTrue(output.extractItem(0, 1, false).is(expected), "existing solid recipe output retained");
         h.assertValueEqual(amount(h, machine), 2000, "solid recipes do not spend water");
      }
      h.assertFalse(input.insertItem(0, new ItemStack(Items.BUCKET), false).isEmpty(),
         "empty bucket remains output-only, not a new recipe input");
      machine.setItem(1, new ItemStack(Items.DIRT));
      h.assertTrue(output.getStackInSlot(1).isEmpty(), "reserved legacy slot is not exposed by bottom automation");
      h.assertFalse(machine.canTakeItemThroughFace(1, machine.getItem(1), Direction.DOWN),
         "vanilla hopper cannot take reserved items");
      h.assertFalse(machine.canTakeItem(new SimpleContainer(1), 1, machine.getItem(1)),
         "arbitrary Container cannot take reserved items");
      h.assertTrue(machine.getItem(1).is(Items.DIRT), "reserved slot contents remain untouched");
      h.succeed();
   }
   @GameTest(template = "empty")
   public static void freezer_water_progress_and_legacy_item_progress_survive_reload(GameTestHelper h) {
      MachineBlockEntity machine = place(h); power(machine);
      tank(h, machine, Direction.UP).fill(water(2000), FluidAction.EXECUTE); ticks(h, machine, 179);
      CompoundTag savedWater = machine.saveWithFullMetadata(h.getLevel().registryAccess());
      h.assertTrue(savedWater.getBoolean("FreezerFluidProcess"), "actual water batch keeps its source marker");
      h.assertValueEqual(savedWater.getInt("FreezerBatchVersion"), 1, "new water batch format is versioned");
      h.assertFalse(savedWater.contains("FreezerBatchInput"), "water work has no item signature");
      machine = reload(h, savedWater);
      h.assertValueEqual(progress(machine), 179, "fluid work source persists");
      ticks(h, machine, 1);
      h.assertTrue(machine.getItem(2).is(Items.ICE), "reloaded fluid cycle completes once");
      h.assertValueEqual(tank(h, machine, Direction.UP).getFluidInTank(0).getAmount(), 1000, "reloaded cycle pays exactly one bucket");
      h.assertValueEqual(energy(machine), 16000 - 1440, "reloaded cycle pays exact total FE");
      machine = place(h); power(machine); machine.setItem(0, new ItemStack(Items.ICE));
      machine.setItem(1, new ItemStack(Items.DIRT)); ticks(h, machine, 179);
      CompoundTag legacy = machine.saveWithFullMetadata(h.getLevel().registryAccess());
      h.assertTrue(legacy.contains("FreezerFluidProcess", Tag.TAG_BYTE)
         && !legacy.getBoolean("FreezerFluidProcess"), "main-era item work writes fluid marker false");
      legacy.remove("FreezerBatchVersion");
      legacy.remove("FreezerBatchInput");
      h.assertTrue(legacy.contains("FreezerFluidProcess", Tag.TAG_BYTE),
         "historical fixture must retain original fluid marker");
      machine = reload(h, legacy);
      h.assertValueEqual(progress(machine), 179, "historical item work retains 179 paid ticks");
      ticks(h, machine, 1);
      h.assertTrue(machine.getItem(2).is(Items.PACKED_ICE), "old item-only saves keep their paid work");
      h.assertValueEqual(energy(machine), 16000 - 1440, "legacy item uses exactly 180 paid ticks");
      h.assertTrue(machine.getItem(1).is(Items.DIRT), "legacy reserved contents survive");
      h.succeed();
   }


   @GameTest(template = "empty")
   public static void freezer_batch_signature_reload_rejects_modern_incomplete_and_changed_items(GameTestHelper h) {
      MachineBlockEntity machine = place(h); power(machine);
      machine.setItem(0, new ItemStack(Items.ICE));
      machine.setItem(1, new ItemStack(Items.DIRT)); ticks(h, machine, 179);
      CompoundTag saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
      h.assertValueEqual(saved.getInt("FreezerBatchVersion"), 1, "modern item work writes version");
      h.assertTrue(saved.contains("FreezerBatchInput", Tag.TAG_COMPOUND), "modern item work has signature");
      h.assertTrue(saved.contains("FreezerFluidProcess", Tag.TAG_BYTE)
         && !saved.getBoolean("FreezerFluidProcess"), "item work is not water funded");

      machine = reload(h, saved);
      h.assertValueEqual(progress(machine), 179, "valid modern item signature retains work");
      ticks(h, machine, 1);
      h.assertTrue(machine.getItem(2).is(Items.PACKED_ICE), "modern saved item completes once");
      h.assertTrue(machine.getItem(1).is(Items.DIRT), "modern reload preserves reserved slot");

      // Prior unversioned candidate saved the item signature, unlike historical main.
      CompoundTag transitional = saved.copy(); transitional.remove("FreezerBatchVersion");
      machine = reload(h, transitional);
      h.assertValueEqual(progress(machine), 179, "pre-version candidate with signature retains work");
      ticks(h, machine, 1);
      h.assertTrue(machine.getItem(2).is(Items.PACKED_ICE), "pre-version signed item completes once");

      MachineBlockEntity changedSource = place(h); power(changedSource);
      changedSource.setItem(0, new ItemStack(Items.ICE));
      changedSource.setItem(1, new ItemStack(Items.DIRT)); ticks(h, changedSource, 179);
      changedSource.setItem(0, new ItemStack(Items.PACKED_ICE));
      CompoundTag changed = changedSource.saveWithFullMetadata(h.getLevel().registryAccess());
      machine = reload(h, changed);
      h.assertValueEqual(progress(machine), 0, "changed ingredient discards unrelated paid work on load");
      ticks(h, machine, 1);
      h.assertValueEqual(progress(machine), 1, "new ingredient starts from first paid tick");
      h.assertTrue(machine.getItem(2).isEmpty(), "no premature blue ice");
      h.assertTrue(machine.getItem(1).is(Items.DIRT), "ingredient change preserves reserved slot");

      CompoundTag missing = saved.copy(); missing.remove("FreezerBatchInput");
      machine = reload(h, missing);
      h.assertValueEqual(progress(machine), 0, "incomplete versioned signature cannot borrow legacy work");
      ticks(h, machine, 1);
      h.assertValueEqual(progress(machine), 1, "missing signature restarts work");
      h.assertTrue(machine.getItem(2).isEmpty(), "missing signature cannot grant output");
      h.assertTrue(machine.getItem(1).is(Items.DIRT), "missing signature retains reserved slot");

      CompoundTag invalidVersion = saved.copy(); invalidVersion.putInt("FreezerBatchVersion", 2);
      machine = reload(h, invalidVersion);
      h.assertValueEqual(progress(machine), 0, "unsupported modern version cannot borrow work");
      CompoundTag missingMarker = saved.copy(); missingMarker.remove("FreezerFluidProcess");
      machine = reload(h, missingMarker);
      h.assertValueEqual(progress(machine), 0, "missing source marker cannot authorize paid item work");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void freezer_real_hopper_recovers_bucket_and_ice_without_stealing_inputs(GameTestHelper h) {
      MachineBlockEntity machine = place(h); power(machine);
      machine.setItem(0, new ItemStack(Items.WATER_BUCKET));
      machine.setItem(1, new ItemStack(Items.DIRT));
      BlockPos hopperPos = machine.getBlockPos().below();
      h.getLevel().setBlock(hopperPos, Blocks.HOPPER.defaultBlockState(), 3);
      HopperBlockEntity hopper = (HopperBlockEntity)h.getLevel().getBlockEntity(hopperPos);
      h.assertFalse(HopperBlockEntity.suckInItems(h.getLevel(), hopper), "real hopper cannot take full bucket or reserved contents");
      ticks(h, machine, 180);
      h.assertTrue(HopperBlockEntity.suckInItems(h.getLevel(), hopper), "real hopper recovers completed empty bucket");
      h.assertTrue(HopperBlockEntity.suckInItems(h.getLevel(), hopper), "real hopper recovers completed ice");
      int buckets = 0, ice = 0;
      for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
         if (hopper.getItem(slot).is(Items.BUCKET)) buckets += hopper.getItem(slot).getCount();
         if (hopper.getItem(slot).is(Items.ICE)) ice += hopper.getItem(slot).getCount();
      }
      h.assertValueEqual(buckets, 1, "one conserved bucket in hopper");
      h.assertValueEqual(ice, 1, "one completed ice in hopper");
      h.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(2).isEmpty(), "both recoverable outputs leave machine");
      h.assertTrue(machine.getItem(1).is(Items.DIRT), "legacy reserved contents remain for manual recovery");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void freezer_reload_cannot_transfer_source_credit_or_restore_invalid_water(GameTestHelper h) {
      MachineBlockEntity machine = place(h); power(machine);
      tank(h, machine, Direction.UP).fill(water(1000), FluidAction.EXECUTE); ticks(h, machine, 179);
      machine.setItem(0, new ItemStack(Items.ICE));
      machine = reload(h, machine.saveWithFullMetadata(h.getLevel().registryAccess())); ticks(h, machine, 1);
      h.assertValueEqual(progress(machine), 1, "saving between source mutation and tick cannot transfer credit");
      h.assertTrue(machine.getItem(2).isEmpty(), "no premature packed ice on load");
      machine = place(h); power(machine); machine.setItem(0, new ItemStack(Items.ICE));
      tank(h, machine, Direction.UP).fill(water(1000), FluidAction.EXECUTE); ticks(h, machine, 179);
      machine.setItem(0, ItemStack.EMPTY);
      machine = reload(h, machine.saveWithFullMetadata(h.getLevel().registryAccess())); ticks(h, machine, 1);
      h.assertValueEqual(progress(machine), 1, "item-to-water mutation before save cannot transfer credit either");
      h.assertTrue(machine.getItem(2).isEmpty(), "no premature ice after item source disappears");
      h.assertValueEqual(amount(h, machine), 1000, "uncompleted replacement water stays in tank");
      for (int invalid : new int[]{-1, 999, 180, 100000}) {
         CompoundTag saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
         saved.putInt("Progress", invalid); saved.putBoolean("FreezerFluidProcess", true);
         saved.putInt("FluidAmount", -12); saved.putString("FluidType", "water");
         machine = reload(h, saved);
         h.assertValueEqual(amount(h, machine), 0, "negative water normalizes empty");
         h.assertValueEqual(progress(machine), 0, "unfunded fluid work is discarded on load");
      }
      CompoundTag saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
      saved.putInt("FluidAmount", 9000); saved.putString("FluidType", "water");
      machine = reload(h, saved);
      h.assertValueEqual(amount(h, machine), 2000, "saved water is capacity-bounded");
      saved.putString("FluidType", "lava"); machine = reload(h, saved);
      h.assertValueEqual(amount(h, machine), 0, "incompatible saved fluid cannot become water");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void freezer_pipe_water_updates_existing_menu_without_changing_protocol(GameTestHelper h) {
      MachineBlockEntity machine = place(h);
      MachineMenu menu = menu(h, machine);
      h.assertValueEqual(menu.slots.size(), 39, "three existing machine slots and vanilla player inventory");
      h.assertValueEqual(machine.dataAccessForTests().getCount(), 16, "existing data count preserved");
      h.assertTrue(menu.freezerPreviewStack().isEmpty(), "empty tank has no result preview");
      tank(h, machine, Direction.UP).fill(water(999), FluidAction.EXECUTE);
      h.assertTrue(menu.freezerPreviewStack().isEmpty(), "partial water has no recipe preview");
      h.assertValueEqual(menu.freezerStatus(), MachineMenu.FREEZER_STATUS_IDLE, "partial water is idle");
      tank(h, machine, Direction.UP).fill(water(1), FluidAction.EXECUTE);
      h.assertValueEqual(machine.dataAccessForTests().get(2), 1000, "existing unused field carries exact water amount");
      h.assertTrue(menu.freezerPreviewStack().is(Items.ICE), "fluid recipe preview matches processing");
      h.assertValueEqual(menu.freezerStatus(), MachineMenu.FREEZER_STATUS_WAITING_FOR_ENERGY, "fluid recipe reports missing FE");
      power(machine);
      h.assertValueEqual(menu.freezerStatus(), MachineMenu.FREEZER_STATUS_READY, "fluid recipe reports ready");
      ticks(h, machine, 45);
      h.assertValueEqual(menu.freezerProgressTicks(), 45, "existing progress field remains exact");
      h.assertValueEqual(menu.freezerPhase(), 1, "existing phase field retained");
      h.assertValueEqual(menu.freezerStatus(), MachineMenu.FREEZER_STATUS_PROCESSING, "fluid recipe reports processing");
      for (Item blocked : new Item[]{Items.BUCKET, Items.DIRT}) {
         machine.setItem(0, new ItemStack(blocked)); ticks(h, machine, 1);
         h.assertTrue(menu.freezerPreviewStack().isEmpty(), "occupied invalid input hides fluid preview");
         h.assertValueEqual(menu.freezerStatus(), MachineMenu.FREEZER_STATUS_INVALID_INPUT, "occupied input blocks fluid recipe");
      }
      machine.setItem(0, ItemStack.EMPTY); machine.setItem(2, new ItemStack(Items.ICE, 64));
      h.assertValueEqual(menu.freezerStatus(), MachineMenu.FREEZER_STATUS_OUTPUT_BLOCKED, "fluid recipe reports full output");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void freezer_hand_bucket_inserts_item_recipe_and_keeps_pipe_tank_independent(GameTestHelper h) {
      MachineBlockEntity machine = place(h); power(machine);
      tank(h, machine, Direction.UP).fill(water(1250), FluidAction.EXECUTE);
      var player = h.makeMockPlayer(GameType.SURVIVAL);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
      h.assertTrue(machine.handleItemUse(player.getMainHandItem(), player, InteractionHand.MAIN_HAND),
         "right-click inserts the water-bucket ingredient");
      h.assertTrue(player.getMainHandItem().isEmpty(), "handed water bucket is transferred to the machine");
      h.assertTrue(machine.getItem(0).is(Items.WATER_BUCKET), "water bucket becomes the timed item input");
      h.assertTrue(machine.getItem(2).isEmpty(), "bucket insertion does not instantly create ice");
      h.assertValueEqual(progress(machine), 0, "bucket insertion itself does not advance the cycle");
      h.assertValueEqual(energy(machine), 16000, "bucket insertion itself does not consume FE");
      h.assertValueEqual(amount(h, machine), 1250, "hand bucket does not fill or spend the pipe tank");
      ticks(h, machine, 179);
      h.assertValueEqual(progress(machine), 179, "hand-inserted bucket follows the timed 180-tick cycle");
      h.assertTrue(machine.getItem(0).is(Items.WATER_BUCKET), "input bucket remains until cycle completion");
      h.assertTrue(machine.getItem(2).isEmpty(), "hand bucket cycle has no early ice");
      h.assertValueEqual(energy(machine), 16000 - 179 * 8, "hand bucket pays each active tick");
      h.assertValueEqual(amount(h, machine), 1250, "hand bucket recipe does not alter pipe tank");
      ticks(h, machine, 1);
      h.assertTrue(machine.getItem(0).is(Items.BUCKET), "empty bucket returns in the input slot");
      h.assertTrue(machine.getItem(2).is(Items.ICE), "one ice is produced at tick 180");
      h.assertValueEqual(progress(machine), 0, "completed bucket cycle resets");
      h.assertValueEqual(energy(machine), 16000 - 1440, "hand bucket cycle pays the exact FE cost");
      h.assertValueEqual(amount(h, machine), 1250, "direct item recipe remains independent from pipe water");
      h.assertValueEqual(machine.comparatorOutput(), 0, "comparator continues to indicate process, not tank fullness");
      h.succeed();
   }
   @GameTest(template = "empty")
   public static void freezer_real_ie_pipes_use_top_input_only_and_reconnect_after_entity_reload(GameTestHelper h) {
      var pipeBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "fluid_pipe"));
      h.assertTrue(pipeBlock != Blocks.AIR, "required IE pipe is loaded");
      MachineBlockEntity machine = place(h); power(machine);

      // A real IE pipe on any non-top face must not discover a Freezer fluid endpoint.
      for (Direction side : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
         BlockPos sidePipePos = machine.getBlockPos().relative(side);
         h.getLevel().setBlock(sidePipePos, pipeBlock.defaultBlockState(), 3);
         FluidPipeBlockEntity sidePipe = (FluidPipeBlockEntity)h.getLevel().getBlockEntity(sidePipePos);
         sidePipe.onNeighborBlockChange(machine.getBlockPos());
         int machineFace = 1 << side.getOpposite().get3DDataValue();
         h.assertFalse((sidePipe.getAvailableConnectionByte() & machineFace) != 0,
            "IE must not detect a Freezer fluid port on " + side);
         IFluidHandler sideFeed = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, sidePipePos, side);
         h.assertTrue(sideFeed != null, "IE pipe retains its configured feed face on " + side);
         h.assertValueEqual(sideFeed.fill(water(1000), FluidAction.SIMULATE), 0,
            "IE pipe cannot route water through the closed Freezer face " + side);
         h.getLevel().setBlock(sidePipePos, Blocks.AIR.defaultBlockState(), 3);
      }

      // The upper pipe face feeds the Freezer's public UP capability.
      BlockPos pipePos = machine.getBlockPos().above();
      h.getLevel().setBlock(pipePos, pipeBlock.defaultBlockState(), 3);
      FluidPipeBlockEntity pipe = (FluidPipeBlockEntity)h.getLevel().getBlockEntity(pipePos);
      pipe.onNeighborBlockChange(machine.getBlockPos());
      int downConnection = 1 << Direction.DOWN.get3DDataValue();
      h.assertTrue((pipe.getAvailableConnectionByte() & downConnection) != 0, "IE detects the Freezer top input");
      IFluidHandler inlet = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pipePos, Direction.UP);
      h.assertTrue(inlet != null, "external IE pipe top feed exists");

      int transferred = 0;
      for (int attempt = 0; transferred < 1000 && attempt < 100; attempt++) {
         int accepted = inlet.fill(water(1000 - transferred), FluidAction.SIMULATE);
         h.assertTrue(accepted > 0, "IE top route accepts water");
         h.assertValueEqual(amount(h, machine), transferred, "IE pipe simulation does not fill destination");
         int executed = inlet.fill(water(1000 - transferred), FluidAction.EXECUTE);
         h.assertValueEqual(executed, accepted, "IE simulated and actual transfer agree");
         transferred += executed;
      }
      h.assertValueEqual(transferred, 1000, "one actual bucket crosses the top IE pipe");
      h.assertValueEqual(amount(h, machine), 1000, "top pipe transfer is conserved");
      ticks(h, machine, 179);

      machine = reload(h, machine.saveWithFullMetadata(h.getLevel().registryAccess()));
      FluidPipeBlockEntity reloadedPipe = (FluidPipeBlockEntity)h.getLevel().getBlockEntity(pipePos);
      reloadedPipe.onNeighborBlockChange(machine.getBlockPos());
      h.assertTrue((reloadedPipe.getAvailableConnectionByte() & downConnection) != 0,
         "IE rediscovers the replacement Freezer through its top input");
      IFluidHandler reloadedInlet = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pipePos, Direction.UP);
      h.assertTrue(reloadedInlet != null, "reloaded IE top feed exists");
      ticks(h, machine, 1);
      h.assertTrue(machine.getItem(2).is(Items.ICE), "top pipe-fed cycle completes after entity reload");
      h.assertTrue(items(h, machine, Direction.DOWN).extractItem(0, 1, false).is(Items.ICE),
         "bottom item handler extracts output slot 2");
      h.assertValueEqual(amount(h, machine), 0, "reloaded cycle pays its water");
      int reloadedTransfer = reloadedInlet.fill(water(1000), FluidAction.EXECUTE);
      h.assertTrue(reloadedTransfer > 0, "IE re-discovers replacement entity after reload");
      h.assertValueEqual(amount(h, machine), reloadedTransfer,
         "post-reload top transfer reaches the replacement rather than a stale entity");
      h.getLevel().setBlock(pipePos, Blocks.AIR.defaultBlockState(), 3);
      h.succeed();
   }
}
