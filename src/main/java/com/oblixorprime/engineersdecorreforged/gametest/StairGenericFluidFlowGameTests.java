package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Natural simulation fixtures; no direct insertion of flowing states into the stairs. */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StairGenericFluidFlowGameTests {
   private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

   @GameTest(template = "empty", timeoutTicks = 1100)
   public static void stair_natural_water_matches_control_and_recedes(GameTestHelper helper) {
      channel(helper, "minecraft:water", 1);
   }

   @GameTest(template = "empty", timeoutTicks = 1100)
   public static void stair_natural_lava_matches_control_and_recedes(GameTestHelper helper) {
      channel(helper, "minecraft:lava", 1);
   }

   @GameTest(template = "empty", timeoutTicks = 1100)
   public static void stair_natural_creosote_matches_control_and_recedes(GameTestHelper helper) {
      channel(helper, "immersiveengineering:creosote", 1);
   }

   @GameTest(template = "empty", timeoutTicks = 1100)
   public static void stair_natural_ethanol_matches_control_and_recedes(GameTestHelper helper) {
      channel(helper, "immersiveengineering:ethanol", 1);
   }

   @GameTest(template = "empty", timeoutTicks = 1100)
   public static void stair_natural_lava_chain_matches_control_and_recedes(GameTestHelper helper) {
      channel(helper, "minecraft:lava", 2);
   }

   private static FlowingFluid fluid(String id) {
      var registered = BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
      if (!(registered instanceof FlowingFluid flowing)) {
         throw new AssertionError("Expected registered world fluid: " + id);
      }
      return flowing;
   }

   private static BlockState stairState() {
      return ModBlocks.STEEL_CATWALK_STAIRS.get().defaultBlockState()
         .setValue(PortedBlocks.HORIZONTAL_FACING, Direction.EAST)
         .setValue(PortedBlocks.LEFT_RAILING, true)
         .setValue(PortedBlocks.RIGHT_RAILING, true)
         .setValue(PortedBlocks.EDITED_STAIR_SIDES, 3);
   }

   private static void assertStairPreserved(GameTestHelper helper, BlockPos pos) {
      var state = helper.getLevel().getBlockState(pos);
      helper.assertTrue(state.is(ModBlocks.STEEL_CATWALK_STAIRS.get()), "flow preserves the stair block");
      helper.assertValueEqual(state.getValue(PortedBlocks.HORIZONTAL_FACING), Direction.EAST, "facing is retained");
      helper.assertTrue(state.getValue(PortedBlocks.LEFT_RAILING) && state.getValue(PortedBlocks.RIGHT_RAILING),
         "both installed railing states are retained");
      helper.assertValueEqual(state.getValue(PortedBlocks.EDITED_STAIR_SIDES), 3, "edited side mask is retained");
   }

   private static void channel(GameTestHelper helper, String id, int stairCount) {
      var level = helper.getLevel();
      var source = helper.absolutePos(new BlockPos(1, 2, 2));
      var control = source.south(3);
      var flowing = fluid(id);
      var stair = (NativeFluidloggedBlock) ModBlocks.STEEL_CATWALK_STAIRS.get();

      for (var pos : BlockPos.betweenClosed(source.offset(-1, -1, -1), source.offset(4, 2, 4))) {
         level.setBlock(pos, pos.getY() == source.getY() - 1
            ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
      }
      for (int distance = -1; distance <= 4; distance++) {
         for (int wall : new int[]{-1, 1, 2, 4}) {
            level.setBlock(source.offset(distance, 0, wall), Blocks.BRICKS.defaultBlockState(), 3);
         }
      }
      for (var end : List.of(source.west(), control.west(), source.east(4), control.east(4))) {
         level.setBlock(end, Blocks.BRICKS.defaultBlockState(), 3);
      }
      for (int distance = 1; distance <= stairCount; distance++) {
         level.setBlock(source.east(distance), stairState(), 3);
      }
      // Only the upstream sources are placed: vanilla must admit and propagate each current.
      level.setBlock(source, flowing.getSource(false).createLegacyBlock(), 3);
      level.setBlock(control, flowing.getSource(false).createLegacyBlock(), 3);

      helper.runAfterDelay(400, () -> {
         for (int distance = 1; distance <= 3; distance++) {
            var actual = level.getFluidState(source.east(distance));
            var expected = level.getFluidState(control.east(distance));
            helper.assertTrue(!expected.isEmpty(), "unobstructed control reaches distance " + distance + " for " + id);
            helper.assertValueEqual(actual, expected, "natural identity, amount and FALLING match control at " + distance);
            helper.assertTrue(!actual.isSource(), "natural passage creates no source");
            if (distance <= stairCount) assertStairPreserved(helper, source.east(distance));
         }
         var first = source.east();
         var current = level.getFluidState(first);
         var state = level.getBlockState(first);
         helper.assertTrue(stair.pickupBlock(null, level, first, state).isEmpty(), "flowing stair fluid cannot fill a bucket");
         helper.assertValueEqual(level.getFluidState(first), current, "failed pickup retains exact current");
         var otherFluid = flowing.isSame(Fluids.WATER) ? Fluids.LAVA : Fluids.WATER;
         helper.assertFalse(stair.canPlaceLiquid(null, level, first, state, otherFluid),
            "a different fluid cannot overwrite the current");

         level.setBlock(source, Blocks.AIR.defaultBlockState(), 3);
         level.setBlock(control, Blocks.AIR.defaultBlockState(), 3);
         helper.runAfterDelay(600, () -> {
            for (int distance = 0; distance <= 3; distance++) {
               helper.assertTrue(level.getFluidState(control.east(distance)).isEmpty(), "control fully recedes at " + distance);
               helper.assertTrue(level.getFluidState(source.east(distance)).isEmpty(),
                  "decorative channel fully recedes at " + distance + " for " + id);
               if (distance > 0 && distance <= stairCount) assertStairPreserved(helper, source.east(distance));
            }
            LOGGER.info("IEDCTR_STAIR_FLOW_PASS fluid={} stairs={} matchedPositions=3 recession=true", id, stairCount);
            helper.succeed();
         });
      });
   }

   @GameTest(template = "empty")
   public static void stair_source_buckets_still_roundtrip(GameTestHelper helper) {
      var level = helper.getLevel();
      var pos = helper.absolutePos(new BlockPos(3, 2, 3));
      var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "StairSourceBuckets"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setPos(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
      player.setYRot(0);
      player.setXRot(90);
      var stair = (NativeFluidloggedBlock) ModBlocks.STEEL_CATWALK_STAIRS.get();
      for (var id : List.of("minecraft:water", "minecraft:lava", "immersiveengineering:creosote", "immersiveengineering:ethanol")) {
         var source = fluid(id);
         for (var clear : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 3, 1))) {
            level.setBlock(clear, Blocks.AIR.defaultBlockState(), 3);
         }
         level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
         level.setBlock(pos, stairState(), 3);
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(source.getBucket()));
         helper.assertTrue(player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction(),
            "real source bucket fills the stair: " + id);
         helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "source insertion returns one empty bucket");
         helper.assertValueEqual(level.getFluidState(pos), source.getSource(false), "bucket preserves intentional source state");
         assertStairPreserved(helper, pos);
         helper.assertFalse(stair.canPlaceLiquid(player, level, pos, level.getBlockState(pos), source), "second source bucket is rejected");
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
         helper.assertTrue(player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction(),
            "real empty bucket drains the stair source");
         helper.assertTrue(player.getMainHandItem().is(source.getBucket()), "the exact source bucket is returned");
         helper.assertTrue(level.getFluidState(pos).isEmpty(), "no hidden fluid remains after source pickup");
         assertStairPreserved(helper, pos);
         helper.assertTrue(stair.pickupBlock(null, level, pos, level.getBlockState(pos)).isEmpty(), "source pickup cannot duplicate a bucket");
      }
      LOGGER.info("IEDCTR_GAME_TEST_PASS stair_source_buckets_still_roundtrip fluids=water,lava,creosote,ethanol");
      helper.succeed();
   }
}
