package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.Cell;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NativeFluidloggingGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 2, 3);
   private static List<Block> blocks() {
      return ModBlocks.ORDERED_BLOCKS.stream().map(entry -> (Block)entry.get())
         .filter(block -> block instanceof NativeFluidloggedBlock).toList();
   }
   private static List<Fluid> fluids() {
      return List.of(Fluids.WATER, Fluids.LAVA, NativeFluidloggedBlock.creosote());
   }
   private static BlockState filled(Block block, Fluid fluid) {
      return block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, fluid == Fluids.WATER)
         .setValue(NativeFluidloggedBlock.FLUID_CELL, NativeFluidloggedBlock.cell(fluid));
   }

   @GameTest(template = "empty")
   public static void native_fluidlogging_legacy_states_and_save_codecs(GameTestHelper helper) {
      helper.assertTrue(NativeFluidloggedBlock.creosote() != Fluids.EMPTY, "IE source fluid must actually be registered");
      for (Block block : blocks()) {
         for (Fluid fluid : fluids()) {
            BlockState state = filled(block, fluid);
            var tag = NbtUtils.writeBlockState(state);
            helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag), state,
               "NBT reload preserves independent source cell and all existing geometry properties");
            helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow(), state,
               "disk palette codec preserves vanilla and IE source identity");
            helper.assertTrue(state.getFluidState().getType() == fluid && state.getFluidState().isSource(),
               "block state exposes the exact stored source");
         }
         for (boolean water : new boolean[] {false, true}) {
            var legacy = NbtUtils.writeBlockState(block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, water));
            legacy.getCompound("Properties").remove("fluid_cell");
            BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), legacy);
            helper.assertValueEqual(loaded.getValue(NativeFluidloggedBlock.FLUID_CELL), Cell.EMPTY, "legacy non-water cell is empty");
            helper.assertValueEqual(loaded.getValue(BlockStateProperties.WATERLOGGED), water, "legacy waterlogging survives unchanged");
            helper.assertValueEqual(loaded.getFluidState().isEmpty(), !water, "legacy wet/dry states retain fluid behavior");
         }
      }
      helper.assertFalse(Blocks.OAK_SLAB.defaultBlockState().hasProperty(NativeFluidloggedBlock.FLUID_CELL),
         "vanilla waterloggable blocks are outside native integration");
      helper.assertFalse(ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get().defaultBlockState().hasProperty(NativeFluidloggedBlock.FLUID_CELL),
         "machine world water remains independent of its unchanged tank contract");
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void native_fluidlogging_initial_chunk_and_update_palette_roundtrip(GameTestHelper helper) {
      var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
      LevelChunkSection sent = new LevelChunkSection(biomes);
      int x = 0;
      for (Block block : blocks()) for (Fluid fluid : fluids()) sent.setBlockState(x % 16, 1 + x / 256, (1 + x++ / 16) % 16, filled(block, fluid));
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      try {
         sent.write(buffer);
         LevelChunkSection received = new LevelChunkSection(biomes);
         received.read(buffer);
         x = 0;
         for (Block block : blocks()) for (Fluid fluid : fluids()) {
            helper.assertValueEqual(received.getBlockState(x % 16, 1 + x / 256, (1 + x / 16) % 16), filled(block, fluid), "initial section sync preserves every state");
            helper.assertTrue(received.getFluidState(x % 16, 1 + x / 256, (1 + x++ / 16) % 16).getType() == fluid,
               "initial client section fluid lookup sees vanilla and IE sources");
         }
         for (Block block : blocks()) for (Fluid fluid : fluids()) {
            buffer.clear();
            buffer.writeVarInt(Block.BLOCK_STATE_REGISTRY.getId(filled(block, fluid)));
            BlockState updated = buffer.readById(Block.BLOCK_STATE_REGISTRY::byId);
            helper.assertTrue(updated.getFluidState().getType() == fluid, "native single-block ID update retains fluid identity");
         }
         sent.setBlockState(0, 1, 1, blocks().getFirst().defaultBlockState());
         buffer.clear();
         sent.write(buffer);
         received.read(buffer);
         helper.assertTrue(received.getFluidState(0, 1, 1).isEmpty(), "subsequent section sync removes stale source");
      } finally { buffer.release(); }
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void native_fluidlogging_real_survival_bucket_cycles(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      ServerPlayer player = player(helper, pos);
      for (Block block : blocks()) for (Fluid fluid : fluids()) {
         prepare(helper, pos);
         helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
         for (int cycle = 0; cycle < 3; cycle++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(fluid.getBucket()));
            helper.assertTrue(player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction(),
               "real filled bucket inserts " + fluid + " into " + block);
            helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "survival insertion returns exactly one empty bucket");
            BlockState wet = helper.getLevel().getBlockState(pos);
            helper.assertTrue(wet.is(block) && wet.getFluidState().getType() == fluid, "bucket keeps decorative ID and exact source: "+block+" / "+fluid+" actual="+wet);
            helper.assertTrue(helper.getLevel().getFluidTicks().hasScheduledTick(pos, fluid), "insertion schedules source ticks");
            NativeFluidloggedBlock container = (NativeFluidloggedBlock)block;
            helper.assertFalse(container.canPlaceLiquid(player, helper.getLevel(), pos, wet, fluid), "occupied cell rejects a second bucket");
            helper.assertFalse(container.placeLiquid(helper.getLevel(), pos, wet, ((FlowingFluid)fluid).getSource(false)), "no second stored source");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            helper.assertTrue(player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction(),
               "real empty bucket picks up stored source");
            helper.assertTrue(player.getMainHandItem().is(fluid.getBucket()), "pickup returns exact vanilla or IE bucket");
            BlockState dry = helper.getLevel().getBlockState(pos);
            helper.assertTrue(dry.is(block) && dry.getFluidState().isEmpty(), "pickup keeps block and clears world cell");
            helper.assertTrue(container.pickupBlock(player, helper.getLevel(), pos, dry).isEmpty(), "second pickup yields nothing");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void native_fluidlogging_real_item_placement_source_only(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      ServerPlayer player = player(helper, pos);
      for (Block block : blocks()) for (Fluid fluid : fluids()) for (boolean source : new boolean[] {false, true}) {
         // Ariadne is placed by its paid tool, tested separately; it has no BlockItem.
         if (block.asItem() == Items.AIR) continue;
         prepare(helper, pos);
         FlowingFluid flowing = (FlowingFluid)fluid;
         helper.getLevel().setBlock(pos, (source ? flowing.getSource(false) : flowing.getFlowing(4, false)).createLegacyBlock(), 3);
         ItemStack stack = new ItemStack(block);
         player.setItemInHand(InteractionHand.MAIN_HAND, stack);
         BlockPos support = pos.below();
         BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
            new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false));
         helper.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(), "native block item placement succeeds");
         BlockState placed = helper.getLevel().getBlockState(pos);
         helper.assertTrue(placed.is(block), "placement retains existing block registration");
         helper.assertValueEqual(placed.getFluidState().isEmpty(), !source, "flowing input never becomes a stored source");
         if (source) helper.assertTrue(placed.getFluidState().getType() == fluid, "placement preserves source type");
         helper.assertTrue(player.getMainHandItem().isEmpty(), "survival block placement consumes one block item");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void native_fluidlogging_neighbors_ticks_and_drain_stay_dry(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      prepare(helper, pos);
      Block block = ModBlocks.STEEL_CATWALK.get();
      helper.getLevel().setBlock(pos, filled(block, Fluids.LAVA), 3);
      helper.getLevel().setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.east().below(), Blocks.STONE.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.east(2), Blocks.STONE.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.east().north(), Blocks.STONE.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.east().south(), Blocks.STONE.defaultBlockState(), 3);
      helper.assertTrue(helper.getLevel().getFluidTicks().hasScheduledTick(pos, Fluids.LAVA), "neighbor changes schedule lava work");
      helper.runAfterDelay(40, () -> {
         helper.assertTrue(helper.getLevel().getBlockState(pos).is(block), "fluid ticks never replace the decorative source block");
         helper.assertTrue(helper.getLevel().getFluidState(pos.east()).getType() == Fluids.FLOWING_LAVA,
            "stored lava flows through native non-full-cube boundaries");
         NativeFluidloggedBlock container = (NativeFluidloggedBlock)block;
         helper.assertTrue(container.pickupBlock(null, helper.getLevel(), pos, helper.getLevel().getBlockState(pos)).is(Items.LAVA_BUCKET),
            "after flow exactly one source is picked up");
         helper.runAfterDelay(40, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(block), "drain preserves geometry");
            helper.assertTrue(helper.getLevel().getFluidState(pos).isEmpty(), "residual flow never regenerates stored source");
            helper.succeed();
         });
      });
   }

   @GameTest(template = "empty")
   public static void native_fluidlogging_destruction_leaves_exact_source(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      ServerPlayer player = player(helper, pos);
      // Standalone rail and raised deck have intentional part-mining. Direct world
      // destruction exercises the common source restoration path without part selection.
      for (Block block : blocks()) for (Fluid fluid : fluids()) {
         prepare(helper, pos);
         helper.getLevel().setBlock(pos, filled(block, fluid), 3);
         helper.assertTrue(helper.getLevel().destroyBlock(pos, true), "ordinary world destruction succeeds");
         helper.assertTrue(helper.getLevel().getFluidState(pos).getType() == fluid, "destroyed decorative block leaves one exact source");
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
         helper.assertTrue(player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction(),
            "remaining source yields one real bucket");
         helper.assertTrue(player.getMainHandItem().is(fluid.getBucket()), "destruction does not convert source type");
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
         player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
         helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "second pickup cannot duplicate destroyed source");
      }
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void native_fluidlogging_accumulator_rejects_nonwater_neighbors(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      for (Fluid fluid : fluids()) {
         prepare(helper, pos);
         helper.getLevel().setBlock(pos, ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get().defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, true), 3);
         helper.getLevel().setBlock(pos.east(), filled(ModBlocks.STEEL_CATWALK.get(), fluid), 3);
         var machine = (com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity)helper.getLevel().getBlockEntity(pos);
         var tank = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
            pos, Direction.UP);
         for (int tick = 0; tick < 80; tick++)
            com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity.serverTick(helper.getLevel(), pos,
               machine.getBlockState(), machine);
         helper.assertValueEqual(tank.getFluidInTank(0).getAmount(), fluid == Fluids.WATER ? 250 : 0,
            "native adjacent lava/creosote and own exterior water never manufacture tank water");
         helper.assertTrue(helper.getLevel().getFluidState(pos.east()).getType() == fluid,
            "collection never consumes or converts decorative source");
      }
      helper.succeed();
   }


   @GameTest(template = "empty", timeoutTicks = 100)
   public static void native_fluidlogging_creosote_ticks_and_drain_stay_dry(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      prepare(helper, pos);
      Block block = ModBlocks.STEEL_CATWALK.get();
      helper.getLevel().setBlock(pos, filled(block, NativeFluidloggedBlock.creosote()), 3);
      helper.getLevel().setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.east().below(), Blocks.STONE.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.east(2), Blocks.STONE.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.east().north(), Blocks.STONE.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.east().south(), Blocks.STONE.defaultBlockState(), 3);
      helper.assertTrue(helper.getLevel().getFluidTicks().hasScheduledTick(pos, NativeFluidloggedBlock.creosote()), "neighbor changes schedule creosote work");
      helper.runAfterDelay(40, () -> {
         helper.assertTrue(helper.getLevel().getBlockState(pos).is(block), "fluid ticks never replace the decorative source block");
         helper.assertTrue(helper.getLevel().getFluidState(pos.east()).getType() == ((FlowingFluid)NativeFluidloggedBlock.creosote()).getFlowing(4, false).getType(),
            "stored creosote flows through native non-full-cube boundaries");
         NativeFluidloggedBlock container = (NativeFluidloggedBlock)block;
         helper.assertTrue(container.pickupBlock(null, helper.getLevel(), pos, helper.getLevel().getBlockState(pos)).is(NativeFluidloggedBlock.creosote().getBucket()),
            "after flow exactly one source is picked up");
         helper.runAfterDelay(40, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(block), "drain preserves geometry");
            helper.assertTrue(helper.getLevel().getFluidState(pos).isEmpty(), "residual flow never regenerates stored source");
            helper.succeed();
         });
      });
   }

   @GameTest(template = "empty")
   public static void native_fluidlogging_compressed_chunk_save_reload(GameTestHelper helper) throws Exception {
      var level = helper.getLevel();
      var chunkPos = new net.minecraft.world.level.ChunkPos(helper.absolutePos(CENTER));
      var saved = new net.minecraft.world.level.chunk.LevelChunk(level, chunkPos);
      int x = 0;
      for (Block block : blocks()) for (Fluid fluid : fluids())
         saved.setBlockState(new BlockPos(chunkPos.getMinBlockX() + x % 16, 80 + x / 256, chunkPos.getMinBlockZ() + (x++ / 16) % 16), filled(block, fluid), false);
      var tag = net.minecraft.world.level.chunk.storage.ChunkSerializer.write(level, saved);
      var compressed = new java.io.ByteArrayOutputStream();
      net.minecraft.nbt.NbtIo.writeCompressed(tag, compressed);
      var diskTag = net.minecraft.nbt.NbtIo.readCompressed(new java.io.ByteArrayInputStream(compressed.toByteArray()),
         net.minecraft.nbt.NbtAccounter.unlimitedHeap());
      var loaded = net.minecraft.world.level.chunk.storage.ChunkSerializer.read(level, level.getPoiManager(),
         new net.minecraft.world.level.chunk.storage.RegionStorageInfo("native-fluidlogging-test", level.dimension(), "chunk"),
         chunkPos, diskTag);
      x = 0;
      for (Block block : blocks()) for (Fluid fluid : fluids()) {
         BlockPos pos = new BlockPos(chunkPos.getMinBlockX() + x % 16, 80 + x / 256, chunkPos.getMinBlockZ() + (x++ / 16) % 16);
         helper.assertValueEqual(loaded.getBlockState(pos), filled(block, fluid), "actual compressed chunk save/reload keeps decorative state");
         helper.assertTrue(loaded.getFluidState(pos).getType() == fluid, "reloaded chunk exposes exact vanilla/IE source");
      }
      helper.succeed();
   }


   private static void prepare(GameTestHelper helper, BlockPos pos) {
      helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      for (Direction side : Direction.values()) helper.getLevel().setBlock(pos.relative(side),
         side == Direction.UP ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.above(2), Blocks.AIR.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.above(3), Blocks.AIR.defaultBlockState(), 3);
   }
   private static ServerPlayer player(GameTestHelper helper, BlockPos pos) {
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "FluidCellTest"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setPos(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
      player.setYRot(0);
      player.setXRot(90);
      return player;
   }
}
