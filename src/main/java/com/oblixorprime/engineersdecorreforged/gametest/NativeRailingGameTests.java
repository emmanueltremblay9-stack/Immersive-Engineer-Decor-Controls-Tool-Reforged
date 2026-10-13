package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native server interactions: deliberately uses no new production implementation API. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class NativeRailingGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 1, 3);
   private static final Direction[] EDGES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
   private static final BooleanProperty[] SIDES = {PortedBlocks.NORTH, PortedBlocks.EAST, PortedBlocks.SOUTH, PortedBlocks.WEST};

   private NativeRailingGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_catwalk_clicks_toggle_each_side_for_all_masks_and_yaws(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (int mask = 0; mask < 16; mask++) {
         for (int edge = 0; edge < EDGES.length; edge++) {
            for (Direction facing : EDGES) {
               clear(helper);
               put(helper, CENTER, sides(catwalk(), mask));
               giveRailings(player, 32);
               clickEdge(helper, player, CENTER, EDGES[edge], facing);
               assertSides(helper, catwalk(), mask ^ (1 << edge),
                  "catwalk mask " + mask + ", edge " + EDGES[edge] + ", yaw " + facing);
               helper.assertTrue(helper.getBlockState(CENTER.above()).isAir(), "railing click must not place a separate block above catwalk");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_standalone_clicks_toggle_all_nonempty_masks_independent_of_yaw(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (int mask = 1; mask < 16; mask++) {
         for (int edge = 0; edge < EDGES.length; edge++) {
            for (Direction facing : EDGES) {
               clear(helper);
               put(helper, CENTER, sides(railing(), mask));
               giveRailings(player, 32);
               clickEdge(helper, player, CENTER, EDGES[edge], facing);
               int expected = mask ^ (1 << edge);
               if (expected == 0) {
                  helper.assertTrue(helper.getBlockState(CENTER).isAir(), "removing the final standalone edge must remove the block");
               } else {
                  assertSides(helper, railing(), expected, "standalone mask " + mask + ", edge " + EDGES[edge] + ", yaw " + facing);
               }
               helper.assertTrue(helper.getBlockState(CENTER.above()).isAir(), "native edit must not place a second standalone block");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_standalone_placement_uses_the_clicked_edge_not_player_yaw(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (int edge = 0; edge < EDGES.length; edge++) {
         for (Direction facing : EDGES) {
            clear(helper);
            giveRailings(player, 32);
            clickEdge(helper, player, CENTER.below(), EDGES[edge], facing);
            assertSides(helper, railing(), 1 << edge, "initial placement edge " + EDGES[edge] + ", yaw " + facing);
            helper.assertValueEqual(player.getMainHandItem().getCount(), 31, "initial standalone placement must consume one railing");
         }
      }
      helper.succeed();
   }

   private static ServerPlayer player(GameTestHelper helper) {
      // FakePlayer exercises the real game mode without IE login packet synchronization.
      return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "RailingTest"));
   }

   private static Block catwalk() {
      return ModBlocks.STEEL_CATWALK.get();
   }

   private static Block railing() {
      return ModBlocks.STEEL_RAILING.get();
   }

   private static void giveRailings(ServerPlayer player, int count) {
      player.getInventory().clearContent();
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(railing().asItem(), count));
   }

   private static BlockState sides(Block block, int mask) {
      BlockState state = block.defaultBlockState();
      for (int edge = 0; edge < SIDES.length; edge++) {
         state = state.setValue(SIDES[edge], (mask & (1 << edge)) != 0);
      }
      return state;
   }

   private static void assertSides(GameTestHelper helper, Block block, int mask, String context) {
      BlockState state = helper.getBlockState(CENTER);
      helper.assertTrue(state.is(block), context + ": expected " + block + ", got " + state);
      for (int edge = 0; edge < SIDES.length; edge++) {
         helper.assertValueEqual(state.getValue(SIDES[edge]), (mask & (1 << edge)) != 0,
            context + ": side " + EDGES[edge]);
      }
   }

   private static void clickEdge(GameTestHelper helper, ServerPlayer player, BlockPos relative, Direction edge, Direction facing) {
      BlockPos pos = helper.absolutePos(relative);
      player.setPos(pos.getX() + 0.5, helper.absolutePos(CENTER).getY(), pos.getZ() + 2.0);
      player.setYRot(facing.toYRot());
      Vec3 hitPoint = new Vec3(pos.getX() + 0.5 + edge.getStepX() * 0.45,
         pos.getY() + 1.0, pos.getZ() + 0.5 + edge.getStepZ() * 0.45);
      player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
         new BlockHitResult(hitPoint, Direction.UP, pos, false));
   }

   private static void put(GameTestHelper helper, BlockPos relative, BlockState state) {
      helper.getLevel().setBlock(helper.absolutePos(relative), state, 2);
   }

   private static void clear(GameTestHelper helper) {
      for (BlockPos pos : BlockPos.betweenClosed(CENTER.offset(-2, 0, -2), CENTER.offset(2, 2, 2))) {
         put(helper, pos, Blocks.AIR.defaultBlockState());
      }
      for (BlockPos pos : BlockPos.betweenClosed(CENTER.offset(-2, -1, -2), CENTER.offset(2, -1, 2))) {
         put(helper, pos, Blocks.STONE.defaultBlockState());
      }
   }
}
