package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks.PassiveFluidAccumulatorBlock;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PassiveAccumulatorGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 2, 3);

   private PassiveAccumulatorGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void passive_accumulator_real_item_placement_preserves_source_water(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      ServerPlayer player = player(helper, pos);
      for (boolean wet : new boolean[] {false, true}) {
         for (Direction face : Direction.values()) {
            prepare(helper, pos);
            helper.getLevel().setBlock(pos, wet ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            helper.getLevel().setBlock(pos.relative(face.getOpposite()), Blocks.STONE.defaultBlockState(), 3);
            place(helper, player, pos, face);
            BlockState state = helper.getLevel().getBlockState(pos);
            helper.assertValueEqual(state.getValue(PassiveFluidAccumulatorBlock.WATERLOGGED), wet,
               "real BlockItem placement must preserve source water for every clicked face");
            helper.assertValueEqual(state.getFluidState().isSource(), wet, "world fluid state must agree with waterlogging");
            helper.assertTrue(player.getMainHandItem().isEmpty(), "survival placement consumes exactly one accumulator");
            MachineBlockEntity machine = machine(helper, pos);
            helper.assertValueEqual(machine.kind(), MachineKind.PASSIVE_FLUID_ACCUMULATOR, "machine kind is unchanged");
            helper.assertValueEqual(tank(helper, pos).getTankCapacity(0), 4000, "the existing tank remains 4000 mB");
            helper.assertValueEqual(amount(helper, pos), 0, "world water does not prefill the machine tank");
            helper.assertTrue(machine.energyStorage(Direction.UP) == null, "the passive accumulator still needs no FE");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void passive_accumulator_placement_matches_vanilla_source_only_waterlogging(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      ServerPlayer player = player(helper, pos);
      for (BlockState replaced : new BlockState[] {
         Blocks.AIR.defaultBlockState(), Fluids.FLOWING_WATER.getFlowing(4, false).createLegacyBlock(),
         Blocks.LAVA.defaultBlockState()
      }) {
         prepare(helper, pos);
         helper.getLevel().setBlock(pos, replaced, 3);
         place(helper, player, pos, Direction.UP);
         helper.assertFalse(helper.getLevel().getBlockState(pos).getValue(PassiveFluidAccumulatorBlock.WATERLOGGED),
            "air, flowing water and lava must not mint a water source during placement");
         helper.assertValueEqual(helper.getLevel().getFluidState(pos), replaced.getFluidState().isSource()
            ? replaced.getFluidState() : Fluids.EMPTY.defaultFluidState(), "source lava remains lava externally; current never becomes source water");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void passive_accumulator_normal_buckets_still_transfer_only_machine_tank(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      ServerPlayer player = player(helper, pos);
      for (boolean wet : new boolean[] {false, true}) {
         prepare(helper, pos);
         put(helper, pos, wet);
         player.setShiftKeyDown(false);
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
         helper.assertTrue(useOn(helper, player, pos).consumesAction(), "normal water-bucket use fills the existing tank");
         helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "tank filling returns an empty bucket");
         helper.assertValueEqual(amount(helper, pos), 1000, "one normal bucket adds exactly 1000 mB");
         helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(PassiveFluidAccumulatorBlock.WATERLOGGED), wet,
            "tank filling must not change world waterlogging");
         helper.assertTrue(useOn(helper, player, pos).consumesAction(), "normal empty-bucket use drains the tank");
         helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "tank draining returns one water bucket");
         helper.assertValueEqual(amount(helper, pos), 0, "tank draining removes exactly 1000 mB");
         helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(PassiveFluidAccumulatorBlock.WATERLOGGED), wet,
            "tank draining must not consume world water");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void passive_accumulator_sneaking_buckets_roundtrip_world_water_without_duplication(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      prepare(helper, pos);
      put(helper, pos, false);
      ServerPlayer player = player(helper, pos);
      player.setShiftKeyDown(true);
      IFluidHandler tank = tank(helper, pos);
      tank.fill(new FluidStack(Fluids.WATER, 750), FluidAction.EXECUTE);
      MachineBlockEntity original = machine(helper, pos);
      SimpleWaterloggedBlock waterlogged = (SimpleWaterloggedBlock)block();
      for (int repetition = 0; repetition < 8; repetition++) {
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
         useWorldBucket(helper, player, pos);
         BlockState wet = helper.getLevel().getBlockState(pos);
         helper.assertTrue(wet.getValue(PassiveFluidAccumulatorBlock.WATERLOGGED), "sneak bucket use waterlogs the world block");
         helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "world placement consumes the water bucket");
         helper.assertTrue(helper.getLevel().getFluidTicks().hasScheduledTick(pos, Fluids.WATER), "water placement schedules a real water tick");
         helper.assertFalse(waterlogged.canPlaceLiquid(player, helper.getLevel(), pos, wet, Fluids.WATER),
            "native occupied exterior cell rejects duplicate insertion before bucket consumption");
         helper.assertFalse(waterlogged.placeLiquid(helper.getLevel(), pos, wet, Fluids.WATER.getSource(false)),
            "repeated liquid insertion must not manufacture another source");
         helper.getLevel().setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 3);
         helper.getLevel().setBlock(pos.east(), Blocks.STONE.defaultBlockState(), 3);
         useWorldBucket(helper, player, pos);
         BlockState dry = helper.getLevel().getBlockState(pos);
         helper.assertFalse(dry.getValue(PassiveFluidAccumulatorBlock.WATERLOGGED), "sneak empty bucket drains only the world cell");
         helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "world pickup yields exactly one water bucket");
         helper.assertTrue(waterlogged.pickupBlock(player, helper.getLevel(), pos, dry).isEmpty(),
            "a second pickup from a dry accumulator must yield nothing");
         helper.assertTrue(machine(helper, pos) == original, "water toggles and neighbors must retain the block entity");
         helper.assertValueEqual(amount(helper, pos), 750, "world bucket cycles never read or copy tank contents");
      }
      helper.assertTrue(waterlogged.canPlaceLiquid(player, helper.getLevel(), pos, helper.getLevel().getBlockState(pos), Fluids.LAVA),
         "sneaking accepts exterior lava without making it tank water");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void passive_accumulator_reload_keeps_water_tank_and_inventory_with_safe_legacy_default(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      prepare(helper, pos);
      put(helper, pos, true);
      MachineBlockEntity original = machine(helper, pos);
      tank(helper, pos).fill(new FluidStack(Fluids.WATER, 1750), FluidAction.EXECUTE);
      original.setItem(0, new ItemStack(Items.BUCKET, 3));
      CompoundTag stateTag = NbtUtils.writeBlockState(original.getBlockState());
      CompoundTag machineTag = original.saveWithFullMetadata(helper.getLevel().registryAccess());
      helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag);
      helper.getLevel().setBlock(pos, loaded, 3);
      MachineBlockEntity reloaded = machine(helper, pos);
      reloaded.loadWithComponents(machineTag, helper.getLevel().registryAccess());
      helper.assertTrue(reloaded != original, "reload must reconstruct the real block entity");
      helper.assertTrue(loaded.getValue(PassiveFluidAccumulatorBlock.WATERLOGGED), "reload retains world water");
      helper.assertValueEqual(amount(helper, pos), 1750, "reload retains independent tank contents");
      helper.assertValueEqual(reloaded.getItem(0).getCount(), 3, "reload retains bucket inventory");
      helper.getLevel().setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 3);
      helper.assertValueEqual(helper.getLevel().getBlockState(pos), loaded, "neighbor update after reload retains waterlogging");
      helper.assertTrue(helper.getLevel().getFluidTicks().hasScheduledTick(pos, Fluids.WATER), "wet neighbor updates schedule fluid work");
      stateTag.remove("Properties");
      BlockState legacy = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag);
      helper.assertTrue(legacy.is(block()), "legacy ID continues to resolve to the same accumulator");
      helper.assertFalse(legacy.getValue(PassiveFluidAccumulatorBlock.WATERLOGGED), "old property-free saves default safely to dry");
      helper.assertFalse(BlockState.CODEC.parse(NbtOps.INSTANCE, stateTag).getOrThrow().getValue(PassiveFluidAccumulatorBlock.WATERLOGGED),
         "the block-state codec also supplies the dry default for old property-free saves");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void passive_accumulator_keeps_all_six_adjacent_source_rules_when_waterlogged(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      for (boolean wet : new boolean[] {false, true}) {
         for (Direction direction : Direction.values()) {
            prepare(helper, pos);
            put(helper, pos, wet);
            MachineBlockEntity machine = machine(helper, pos);
            tick(helper, machine, 80);
            helper.assertValueEqual(amount(helper, pos), 0, "own waterlogged cell alone is not an existing collection source");
            BlockPos neighbor = pos.relative(direction);
            helper.getLevel().setBlock(neighbor, Fluids.FLOWING_WATER.getFlowing(4, false).createLegacyBlock(), 3);
            tick(helper, machine, 80);
            helper.assertValueEqual(amount(helper, pos), 0, "flowing neighbors do not meet the existing source-water rule");
            helper.getLevel().setBlock(neighbor, Blocks.WATER.defaultBlockState(), 3);
            tick(helper, machine, 79);
            helper.assertValueEqual(amount(helper, pos), 0, "accumulation still waits for tick 80");
            tick(helper, machine, 1);
            helper.assertValueEqual(amount(helper, pos), 250, "each of six adjacent sources enables 250 mB per 80 ticks");
            helper.assertTrue(helper.getLevel().getFluidState(neighbor).isSource(), "existing passive collection does not consume the source");
            helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(PassiveFluidAccumulatorBlock.WATERLOGGED), wet,
               "collection never changes the machine's world-water state");
            helper.getLevel().setBlock(neighbor, Blocks.STONE.defaultBlockState(), 3);
            tick(helper, machine, 80);
            helper.assertValueEqual(amount(helper, pos), 250, "removing the qualifying neighbor stops accumulation");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void passive_accumulator_neighbor_waterlogging_capacity_slots_and_pipe_drain_stay_compatible(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      prepare(helper, pos);
      put(helper, pos, true);
      helper.getLevel().setBlock(pos.north(), Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), 3);
      helper.getLevel().setBlock(pos.south(), Blocks.WATER.defaultBlockState(), 3);
      MachineBlockEntity machine = machine(helper, pos);
      tick(helper, machine, 80);
      helper.assertValueEqual(amount(helper, pos), 250, "multiple qualifying neighbors still give only one 250 mB collection");
      tick(helper, machine, 80 * 20);
      helper.assertValueEqual(amount(helper, pos), 4000, "underwater collection respects the unchanged tank capacity");
      machine.setItem(0, new ItemStack(Items.BUCKET));
      tick(helper, machine, 1);
      helper.assertTrue(machine.getItem(0).isEmpty(), "bucket input slot still consumes its bucket");
      helper.assertTrue(machine.getItem(1).is(Items.WATER_BUCKET), "bucket output slot still produces water");
      helper.assertValueEqual(amount(helper, pos), 3000, "bucket slot filling deducts exactly 1000 mB");
      IFluidHandler tank = tank(helper, pos);
      helper.assertValueEqual(tank.drain(4000, FluidAction.SIMULATE).getAmount(), 3000, "simulated pipe drain reports available tank fluid");
      helper.assertValueEqual(amount(helper, pos), 3000, "simulated drain does not mutate tank");
      helper.assertValueEqual(tank.drain(4000, FluidAction.EXECUTE).getAmount(), 3000, "pipe drainage stays independent of world water");
      helper.assertTrue(tank.drain(4000, FluidAction.EXECUTE).isEmpty(), "a second pipe drain cannot duplicate water");
      helper.assertTrue(helper.getLevel().getFluidState(pos).isSource(), "draining the tank leaves world water intact");
      helper.assertValueEqual(tank.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE), 0, "accumulator still rejects lava storage");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 200)
   public static void passive_accumulator_real_server_ticker_collects_underwater_and_stops_without_neighbor_source(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      prepare(helper, pos);
      put(helper, pos, true);
      // Enclose the source so this test cannot gain any additional neighboring source cells through flow.
      BlockPos source = pos.north();
      for (Direction direction : Direction.values()) {
         if (!source.relative(direction).equals(pos)) {
            helper.getLevel().setBlock(source.relative(direction), Blocks.STONE.defaultBlockState(), 3);
         }
      }
      helper.getLevel().setBlock(source, Blocks.WATER.defaultBlockState(), 3);
      helper.runAfterDelay(85, () -> {
         helper.assertValueEqual(amount(helper, pos), 250, "the registered machine ticker must collect underwater during real world ticks");
         helper.getLevel().setBlock(source, Blocks.STONE.defaultBlockState(), 3);
         helper.runAfterDelay(85, () -> {
            helper.assertValueEqual(amount(helper, pos), 250, "own-cell water must not keep collection running after the neighbor is removed");
            helper.assertTrue(helper.getLevel().getFluidState(pos).isSource(), "collection does not drain waterlogging");
            helper.succeed();
         });
      });
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void passive_accumulator_neighbor_updates_allow_vanilla_flow_and_draining_does_not_refill_source(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      prepare(helper, pos);
      put(helper, pos, true);
      helper.getLevel().setBlock(pos.east().below(), Blocks.STONE.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.east(2), Blocks.STONE.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.east().north(), Blocks.STONE.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.east().south(), Blocks.STONE.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 3);
      helper.assertTrue(helper.getLevel().getFluidTicks().hasScheduledTick(pos, Fluids.WATER), "opening a neighboring cell schedules a water tick");
      helper.runAfterDelay(15, () -> {
         helper.assertTrue(helper.getLevel().getFluidState(pos.east()).getType() == Fluids.FLOWING_WATER,
            "world water flows through the accumulator's existing non-full-cube boundary");
         helper.assertFalse(helper.getLevel().getFluidState(pos.east()).isSource(), "one waterlogged block does not create a neighboring source");
         SimpleWaterloggedBlock waterlogged = (SimpleWaterloggedBlock)block();
         helper.assertTrue(waterlogged.pickupBlock(null, helper.getLevel(), pos, helper.getLevel().getBlockState(pos)).is(Items.WATER_BUCKET),
            "the source can be collected after neighboring fluid updates");
         helper.runAfterDelay(35, () -> {
            helper.assertFalse(helper.getLevel().getBlockState(pos).getValue(PassiveFluidAccumulatorBlock.WATERLOGGED),
               "scheduled updates and residual flowing water cannot recreate a removed source");
            helper.assertTrue(helper.getLevel().getFluidState(pos).isEmpty(), "drained machine cell remains dry");
            helper.assertValueEqual(amount(helper, pos), 0, "flow and drain updates never credit the internal tank");
            helper.succeed();
         });
      });
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void passive_accumulator_destroy_leaves_one_vanilla_source_and_no_second_bucket(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      prepare(helper, pos);
      put(helper, pos, true);
      ServerPlayer player = player(helper, pos);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      helper.assertTrue(player.gameMode.destroyBlock(pos), "normal player destruction removes the machine");
      helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.WATER), "breaking a waterlogged accumulator leaves the one vanilla source");
      helper.assertTrue(helper.getLevel().getBlockEntity(pos) == null, "destruction removes its block entity");
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
      helper.assertTrue(player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction(),
         "the remaining source yields one water bucket");
      helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "source pickup returns a water bucket");
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
      player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
      helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "a second pickup after destruction cannot duplicate the source");
      helper.assertTrue(helper.getLevel().getFluidState(pos).isEmpty(), "the destroyed and drained cell stays empty");
      helper.succeed();
   }

   private static Block block() {
      return ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get();
   }

   private static void prepare(GameTestHelper helper, BlockPos pos) {
      helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      for (Direction direction : Direction.values()) {
         helper.getLevel().setBlock(pos.relative(direction), direction == Direction.UP ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(), 3);
      }
      helper.getLevel().setBlock(pos.above(2), Blocks.AIR.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.above(3), Blocks.AIR.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.above(4), Blocks.AIR.defaultBlockState(), 3);
   }

   private static void put(GameTestHelper helper, BlockPos pos, boolean wet) {
      helper.getLevel().setBlock(pos, block().defaultBlockState().setValue(PassiveFluidAccumulatorBlock.WATERLOGGED, wet), 3);
   }

   private static ServerPlayer player(GameTestHelper helper, BlockPos pos) {
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "AccumulatorTest"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setPos(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
      player.setYRot(0);
      player.setXRot(90);
      return player;
   }

   private static void place(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction face) {
      BlockPos support = pos.relative(face.getOpposite());
      ItemStack stack = new ItemStack(block());
      player.setItemInHand(InteractionHand.MAIN_HAND, stack);
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
         new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5)), face, support, false));
      helper.assertValueEqual(context.getClickedPos(), pos, "real placement context must point at the intended cell");
      helper.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(), "actual accumulator BlockItem placement succeeds");
      helper.assertTrue(helper.getLevel().getBlockState(pos).is(block()), "actual placement keeps the existing block ID");
   }

   private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
      return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
         new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0.5, 15.0 / 16.0, 0.5), Direction.UP, pos, false));
   }

   private static void useWorldBucket(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
      // Match the normal client sequence: sneak bypasses the machine, then BucketItem raycasts on useItem.
      helper.assertFalse(useOn(helper, player, pos).consumesAction(), "sneaking must bypass tank interaction");
      helper.assertTrue(player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction(),
         "real bucket item use must interact with the waterlogged world cell");
   }

   private static MachineBlockEntity machine(GameTestHelper helper, BlockPos pos) {
      helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof MachineBlockEntity, "accumulator keeps its existing block entity");
      return (MachineBlockEntity)helper.getLevel().getBlockEntity(pos);
   }

   private static IFluidHandler tank(GameTestHelper helper, BlockPos pos) {
      IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
      helper.assertTrue(tank != null, "registered fluid capability remains available");
      return tank;
   }

   private static int amount(GameTestHelper helper, BlockPos pos) {
      return tank(helper, pos).getFluidInTank(0).getAmount();
   }

   private static void tick(GameTestHelper helper, MachineBlockEntity machine, int ticks) {
      for (int i = 0; i < ticks; i++) {
         MachineBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
      }
   }
}
