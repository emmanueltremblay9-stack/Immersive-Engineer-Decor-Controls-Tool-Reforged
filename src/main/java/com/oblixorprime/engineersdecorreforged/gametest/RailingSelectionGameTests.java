package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Shared selection geometry used by mining and the client-only outline renderer. */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RailingSelectionGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 2, 3);
   private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

   private RailingSelectionGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void shared_selection_picks_each_rail_from_inside_outside_and_above(GameTestHelper helper) {
      Player player = player(helper);
      for (Block block : List.of(ModBlocks.STEEL_CATWALK.get(), ModBlocks.STEEL_RAILING.get())) {
         place(helper, allSides(block));
         for (Direction side : SIDES) {
            Vec3 edge = new Vec3(0.5 + side.getStepX() * 0.45, 0.65, 0.5 + side.getStepZ() * 0.45);
            Vec3 outside = edge.add(side.getStepX() * 1.5, 0, side.getStepZ() * 1.5);
            assertRail(helper, player, new Vec3(0.5, 0.65, 0.5), outside, side);
            assertRail(helper, player, outside, new Vec3(0.5, 0.65, 0.5), side);
            assertRail(helper, player, new Vec3(edge.x, 2, edge.z), new Vec3(edge.x, 0.4, edge.z), side);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void shared_selection_distinguishes_top_and_bottom_floor_from_rails(GameTestHelper helper) {
      Player player = player(helper);
      place(helper, allSides(ModBlocks.STEEL_CATWALK.get()));
      for (Vec3 start : List.of(new Vec3(0.5, 2, 0.5), new Vec3(0.5, -1, 0.5))) {
         PortedBlocks.RailingPart part = select(helper, player, start, new Vec3(0.5, 0.1, 0.5));
         helper.assertTrue(part != null && part.side() == null, "top/bottom floor hits must select the floor only");
         helper.assertValueEqual(part.shape().bounds().maxY, 3.0 / 16.0, "floor outline must keep its original height");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void shared_selection_respects_occlusion_legacy_north_and_visual_height(GameTestHelper helper) {
      Player player = player(helper);
      place(helper, ModBlocks.STEEL_RAILING.get().defaultBlockState());
      assertRail(helper, player, new Vec3(0.5, 0.6, -1), new Vec3(0.5, 0.6, 0.5), Direction.NORTH);
      helper.getLevel().setBlock(helper.absolutePos(CENTER.north()), Blocks.STONE.defaultBlockState(), 2);
      helper.assertTrue(select(helper, player, new Vec3(0.5, 0.6, -2), new Vec3(0.5, 0.6, 0.5)) == null,
         "a nearer solid block must occlude the rail, not be redirected into it");
      helper.getLevel().setBlock(helper.absolutePos(CENTER.north()), Blocks.AIR.defaultBlockState(), 2);
      helper.assertTrue(select(helper, player, new Vec3(0.5, 1.25, -1), new Vec3(0.5, 1.25, 2)) == null,
         "taller physical collision must not create an invisible selectable or outlined rail");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void shared_selection_corner_ties_choose_one_deterministic_existing_component(GameTestHelper helper) {
      Player player = player(helper);
      place(helper, allSides(ModBlocks.STEEL_RAILING.get()));
      double[][] corners = {{0.05, 0.05}, {0.95, 0.05}, {0.95, 0.95}, {0.05, 0.95}};
      Direction[] expected = {Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.SOUTH};
      for (int i = 0; i < corners.length; ++i) {
         Vec3 from = new Vec3(corners[i][0], 2, corners[i][1]);
         Vec3 to = new Vec3(corners[i][0], 0.5, corners[i][1]);
         for (int repeat = 0; repeat < 3; ++repeat) {
            assertRail(helper, player, from, to, expected[i]);
         }
      }
      helper.succeed();
   }

   private static void assertRail(GameTestHelper helper, Player player, Vec3 start, Vec3 end, Direction expected) {
      PortedBlocks.RailingPart part = select(helper, player, start, end);
      helper.assertTrue(part != null && part.side() == expected, "ray must select only the expected visible rail: " + expected);
      helper.assertValueEqual(part.shape().bounds().maxY, 1.0, "rail outline must retain visible height");
      helper.assertTrue(part.shape().bounds().getXsize() <= 0.125 || part.shape().bounds().getZsize() <= 0.125,
         "selected outline must contain one edge strip rather than the complete assembly");
   }

   private static PortedBlocks.RailingPart select(GameTestHelper helper, Player player, Vec3 start, Vec3 end) {
      Vec3 origin = Vec3.atLowerCornerOf(helper.absolutePos(CENTER));
      return PortedBlocks.selectRailingPart(helper.getLevel(), helper.absolutePos(CENTER), start.add(origin), end.add(origin), player);
   }

   private static BlockState allSides(Block block) {
      return block.defaultBlockState().setValue(PortedBlocks.NORTH, true).setValue(PortedBlocks.EAST, true)
         .setValue(PortedBlocks.SOUTH, true).setValue(PortedBlocks.WEST, true);
   }

   private static Player player(GameTestHelper helper) {
      return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "RailSelection"));
   }

   private static void place(GameTestHelper helper, BlockState state) {
      for (BlockPos pos : BlockPos.betweenClosed(CENTER.offset(-2, -1, -2), CENTER.offset(2, 3, 2))) {
         helper.getLevel().setBlock(helper.absolutePos(pos), Blocks.AIR.defaultBlockState(), 2);
      }
      helper.getLevel().setBlock(helper.absolutePos(CENTER), state, 2);
   }
}
