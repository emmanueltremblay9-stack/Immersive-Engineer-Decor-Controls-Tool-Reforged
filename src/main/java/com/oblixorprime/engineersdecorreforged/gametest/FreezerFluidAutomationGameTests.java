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
   private static int amount(GameTestHelper h, MachineBlockEntity machine) { return tank(h, machine, null).getFluidInTank(0).getAmount(); }
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
   public static void freezer_water_ports_all_faces_keep_capacity_types_and_simulation(GameTestHelper h) {
      MachineBlockEntity machine = place(h);
      Direction[] sides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
      for (Direction side : sides) {
         IFluidHandler handler = tank(h, machine, side);
         h.assertValueEqual(handler.getTanks(), 1, "one tank on every side");
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
      }
      IFluidHandler handler = tank(h, machine, null);
      FluidStack markedWater = water(1000);
      CompoundTag marker = new CompoundTag(); marker.putInt("freezer_test", 7);
      markedWater.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
      h.assertFalse(handler.isFluidValid(0, markedWater), "unsupported fluid components must not be discarded");
      h.assertValueEqual(handler.fill(markedWater, FluidAction.EXECUTE), 0, "component-bearing water is left with sender");
      h.assertValueEqual(handler.fill(water(375), FluidAction.EXECUTE), 375, "partial fill retained");
      h.assertValueEqual(handler.fill(water(2000), FluidAction.EXECUTE), 1625, "overflow remains with sender");
      FluidStack copy = handler.getFluidInTank(0); copy.setAmount(1);
      h.assertValueEqual(amount(h, machine), 2000, "query stack cannot mutate tank");
      h.assertTrue(handler.drain(markedWater, FluidAction.EXECUTE).isEmpty(), "component mismatch cannot drain plain water");
      h.assertTrue(handler.drain(new FluidStack(Fluids.LAVA, 1), FluidAction.EXECUTE).isEmpty(), "wrong fluid cannot drain");
      h.assertTrue(handler.drain(0, FluidAction.EXECUTE).isEmpty(), "zero drain is inert");
      h.assertTrue(handler.drain(-1, FluidAction.EXECUTE).isEmpty(), "negative drain is inert");
      h.assertValueEqual(handler.drain(water(125), FluidAction.SIMULATE).getAmount(), 125, "stack drain simulates");
      h.assertValueEqual(amount(h, machine), 2000, "simulated drain preserves tank");
      h.assertValueEqual(handler.drain(water(125), FluidAction.EXECUTE).getAmount(), 125, "stack drain executes exactly");
      h.assertValueEqual(handler.drain(5000, FluidAction.EXECUTE).getAmount(), 1875, "amount drain never overdraws");
      h.assertValueEqual(amount(h, machine), 0, "tank emptied exactly");
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
      tank(h, machine, null).fill(water(2000), FluidAction.EXECUTE);
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
   public static void freezer_external_drain_invalidates_only_lost_active_water(GameTestHelper h) {
      MachineBlockEntity machine = place(h); power(machine);
      IFluidHandler handler = tank(h, machine, null);
      handler.fill(water(2000), FluidAction.EXECUTE); ticks(h, machine, 179);
      CompoundTag before = machine.saveWithFullMetadata(h.getLevel().registryAccess());
      handler.drain(2000, FluidAction.SIMULATE);
      handler.fill(water(1), FluidAction.SIMULATE);
      h.assertValueEqual(machine.saveWithFullMetadata(h.getLevel().registryAccess()), before, "all simulated operations preserve active work exactly");
      handler.drain(1000, FluidAction.EXECUTE);
      h.assertValueEqual(progress(machine), 179, "a complete remaining batch retains its work");
      handler.drain(water(1), FluidAction.EXECUTE);
      h.assertValueEqual(progress(machine), 0, "lost batch resets immediately before another server tick");
      handler.fill(water(1), FluidAction.EXECUTE); ticks(h, machine, 1);
      h.assertTrue(machine.getItem(2).isEmpty(), "same-tick drain/refill cannot inherit work");
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
         tank(h, machine, null).fill(water(2000), FluidAction.EXECUTE); ticks(h, machine, 179);
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
   public static void freezer_water_output_blocking_redstone_and_fe_keep_existing_reset_rules(GameTestHelper h) {
      for (int failure = 0; failure < 4; failure++) {
         MachineBlockEntity machine = place(h); power(machine);
         tank(h, machine, null).fill(water(1000), FluidAction.EXECUTE); ticks(h, machine, 179);
         int before = energy(machine);
         if (failure == 0) machine.setItem(2, new ItemStack(Items.ICE, 64));
         if (failure == 1) machine.setItem(2, new ItemStack(Items.PACKED_ICE));
         if (failure == 2) h.getLevel().setBlock(machine.getBlockPos().west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
         if (failure == 3) machine.dataAccessForTests().set(3, 7);
         ticks(h, machine, 1);
         h.assertValueEqual(progress(machine), 0, "existing failure rule resets work: " + failure);
         h.assertValueEqual(amount(h, machine), 1000, "blocked cycle retains its water");
         h.assertValueEqual(energy(machine), failure == 3 ? 7 : before, "blocked cycle spends no FE");
         machine.setItem(2, ItemStack.EMPTY);
         h.getLevel().setBlock(machine.getBlockPos().west(), Blocks.AIR.defaultBlockState(), 3); power(machine);
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
      tank(h, machine, null).fill(water(2000), FluidAction.EXECUTE);
      IItemHandler handler = items(h, machine, Direction.NORTH);
      h.assertTrue(handler.insertItem(0, new ItemStack(Items.WATER_BUCKET), false).isEmpty(), "water bucket inserts through public item capability");
      for (Direction side : Direction.values()) {
         h.assertTrue(items(h, machine, side).extractItem(0, 1, false).isEmpty(), "full bucket cannot be pulled before processing");
         h.assertFalse(machine.canTakeItemThroughFace(0, machine.getItem(0), side), "vanilla extraction agrees for full bucket");
      }
      h.assertFalse(machine.canTakeItem(new SimpleContainer(1), 0, machine.getItem(0)), "Container extraction also protects input");
      ticks(h, machine, 180);
      h.assertTrue(machine.getItem(0).is(Items.BUCKET) && machine.getItem(2).is(Items.ICE), "bucket recipe conserves the bucket");
      h.assertValueEqual(amount(h, machine), 2000, "bucket recipe never also charges tank water");
      h.assertTrue(handler.extractItem(0, 1, true).is(Items.BUCKET), "empty bucket extraction simulates");
      h.assertTrue(machine.getItem(0).is(Items.BUCKET), "simulation preserves empty bucket");
      for (Direction side : Direction.values()) h.assertTrue(machine.canTakeItemThroughFace(0, machine.getItem(0), side), "empty bucket can leave through any face");
      h.assertTrue(machine.canTakeItem(new SimpleContainer(1), 0, machine.getItem(0)), "Container may recover empty bucket");
      h.assertTrue(handler.extractItem(0, 1, false).is(Items.BUCKET), "empty bucket extraction executes");
      h.assertTrue(handler.extractItem(0, 1, false).isEmpty(), "bucket is not duplicated");
      h.assertTrue(handler.extractItem(2, 1, false).is(Items.ICE), "ice extraction still works");
      for (Item input : new Item[]{Items.ICE, Items.PACKED_ICE}) {
         handler.insertItem(0, new ItemStack(input), false);
         h.assertTrue(handler.extractItem(0, 1, false).isEmpty(), "solid recipe input remains protected");
         ticks(h, machine, 180);
         Item expected = input == Items.ICE ? Items.PACKED_ICE : Items.BLUE_ICE;
         h.assertTrue(handler.extractItem(2, 1, false).is(expected), "existing solid recipe output retained");
         h.assertValueEqual(amount(h, machine), 2000, "solid recipes do not spend water");
      }
      h.assertFalse(handler.insertItem(0, new ItemStack(Items.BUCKET), false).isEmpty(), "empty bucket remains output-only, not a new recipe input");
      machine.setItem(1, new ItemStack(Items.DIRT));
      h.assertTrue(handler.extractItem(1, 1, false).isEmpty(), "reserved slot remains outside item automation");
      h.assertFalse(machine.canTakeItemThroughFace(1, machine.getItem(1), Direction.DOWN), "vanilla hopper cannot take reserved items");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void freezer_water_progress_and_legacy_item_progress_survive_reload(GameTestHelper h) {
      MachineBlockEntity machine = place(h); power(machine);
      tank(h, machine, null).fill(water(2000), FluidAction.EXECUTE); ticks(h, machine, 179);
      machine = reload(h, machine.saveWithFullMetadata(h.getLevel().registryAccess()));
      h.assertValueEqual(progress(machine), 179, "fluid work source persists");
      ticks(h, machine, 1);
      h.assertTrue(machine.getItem(2).is(Items.ICE), "reloaded fluid cycle completes once");
      h.assertValueEqual(amount(h, machine), 1000, "reloaded cycle pays exactly one bucket");
      h.assertValueEqual(energy(machine), 16000 - 1440, "reloaded cycle pays exact total FE");
      machine = place(h); power(machine); machine.setItem(0, new ItemStack(Items.ICE));
      machine.setItem(1, new ItemStack(Items.DIRT)); ticks(h, machine, 179);
      CompoundTag legacy = machine.saveWithFullMetadata(h.getLevel().registryAccess()); legacy.remove("FreezerFluidProcess");
      machine = reload(h, legacy); ticks(h, machine, 1);
      h.assertTrue(machine.getItem(2).is(Items.PACKED_ICE), "old item-only saves keep their paid work");
      h.assertTrue(machine.getItem(1).is(Items.DIRT), "legacy reserved contents survive");
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
      tank(h, machine, null).fill(water(1000), FluidAction.EXECUTE); ticks(h, machine, 179);
      machine.setItem(0, new ItemStack(Items.ICE));
      machine = reload(h, machine.saveWithFullMetadata(h.getLevel().registryAccess())); ticks(h, machine, 1);
      h.assertValueEqual(progress(machine), 1, "saving between source mutation and tick cannot transfer credit");
      h.assertTrue(machine.getItem(2).isEmpty(), "no premature packed ice on load");
      machine = place(h); power(machine); machine.setItem(0, new ItemStack(Items.ICE));
      tank(h, machine, null).fill(water(1000), FluidAction.EXECUTE); ticks(h, machine, 179);
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
      tank(h, machine, null).fill(water(999), FluidAction.EXECUTE);
      h.assertTrue(menu.freezerPreviewStack().isEmpty(), "partial water has no recipe preview");
      h.assertValueEqual(menu.freezerStatus(), MachineMenu.FREEZER_STATUS_IDLE, "partial water is idle");
      tank(h, machine, null).fill(water(1), FluidAction.EXECUTE);
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
   public static void freezer_hand_bucket_contract_remains_independent_from_pipe_tank(GameTestHelper h) {
      MachineBlockEntity machine = place(h); power(machine);
      tank(h, machine, null).fill(water(1250), FluidAction.EXECUTE);
      var player = h.makeMockPlayer(GameType.SURVIVAL);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
      h.assertTrue(machine.handleItemUse(player.getMainHandItem(), player, InteractionHand.MAIN_HAND), "existing direct bucket action still succeeds");
      h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "direct action returns the player's bucket");
      h.assertTrue(machine.getItem(2).is(Items.ICE), "direct action retains immediate ice output");
      h.assertValueEqual(energy(machine), 16000 - 1440, "direct action retains exact cost");
      h.assertValueEqual(amount(h, machine), 1250, "direct action does not silently fill or spend pipe tank");
      h.assertValueEqual(machine.comparatorOutput(), 0, "comparator continues to indicate process, not tank fullness");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void freezer_real_ie_pipes_connect_and_supply_each_face_after_entity_reload(GameTestHelper h) {
      var pipeBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "fluid_pipe"));
      h.assertTrue(pipeBlock != Blocks.AIR, "required IE pipe is loaded");
      for (Direction side : Direction.values()) {
         MachineBlockEntity machine = place(h); power(machine);
         BlockPos pipePos = machine.getBlockPos().relative(side);
         h.getLevel().setBlock(pipePos, pipeBlock.defaultBlockState(), 3);
         FluidPipeBlockEntity pipe = (FluidPipeBlockEntity)h.getLevel().getBlockEntity(pipePos);
         pipe.onNeighborBlockChange(machine.getBlockPos());
         h.assertTrue((pipe.getAvailableConnectionByte() & (1 << side.getOpposite().get3DDataValue())) != 0, "IE detects Freezer port on " + side);
         IFluidHandler inlet = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pipePos, side);
         h.assertTrue(inlet != null, "external pipe inlet exists");
         int transferred = 0;
         for (int attempt = 0; transferred < 1000 && attempt < 100; attempt++) {
            int accepted = inlet.fill(water(1000 - transferred), FluidAction.SIMULATE);
            h.assertTrue(accepted > 0, "IE route accepts water on " + side);
            h.assertValueEqual(amount(h, machine), transferred, "pipe simulation does not fill destination");
            int executed = inlet.fill(water(1000 - transferred), FluidAction.EXECUTE);
            h.assertValueEqual(executed, accepted, "IE simulated and actual transfer agree");
            transferred += executed;
         }
         h.assertValueEqual(transferred, 1000, "one actual bucket crosses IE pipe");
         h.assertValueEqual(amount(h, machine), 1000, "pipe transfer is conserved");
         ticks(h, machine, 179);
         machine = reload(h, machine.saveWithFullMetadata(h.getLevel().registryAccess()));
         pipe.onNeighborBlockChange(machine.getBlockPos());
         ticks(h, machine, 1);
         h.assertTrue(items(h, machine, side.getOpposite()).extractItem(2, 1, false).is(Items.ICE), "pipe-fed cycle survives entity reload and extracts output");
         h.assertValueEqual(amount(h, machine), 0, "reloaded cycle pays its water");
         int reloadedTransfer = inlet.fill(water(1000), FluidAction.EXECUTE);
         h.assertTrue(reloadedTransfer > 0, "IE re-discovers replacement entity");
         h.assertValueEqual(amount(h, machine), reloadedTransfer, "post-reload transfer reaches the replacement rather than a stale entity");
         h.getLevel().setBlock(pipePos, Blocks.AIR.defaultBlockState(), 3);
      }
      h.succeed();
   }
}
