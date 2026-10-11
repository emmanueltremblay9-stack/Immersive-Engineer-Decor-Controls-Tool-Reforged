package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HatchPlacementGameTests {
   private static final BlockPos CENTER = new BlockPos(1, 1, 1);

   private HatchPlacementGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void iron_hatch_floor_hinge_matches_vanilla_trapdoor(GameTestHelper helper) {
      assertPlacementOnFace(helper, Direction.UP);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void iron_hatch_ceiling_hinge_matches_vanilla_trapdoor(GameTestHelper helper) {
      assertPlacementOnFace(helper, Direction.DOWN);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void iron_hatch_wall_hinge_matches_vanilla_independent_of_player_look(GameTestHelper helper) {
      for (Direction face : Direction.Plane.HORIZONTAL) {
         assertPlacementOnFace(helper, face);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void iron_hatch_replacing_grass_uses_vanilla_player_relative_hinge(GameTestHelper helper) {
      for (Direction face : Direction.Plane.HORIZONTAL) {
         for (Direction look : Direction.Plane.HORIZONTAL) {
            placeAndAssert(helper, face, look, false, true);
         }
      }
      helper.succeed();
   }

   private static void assertPlacementOnFace(GameTestHelper helper, Direction clickedFace) {
      for (Direction look : Direction.Plane.HORIZONTAL) {
         for (boolean powered : new boolean[]{false, true}) {
            placeAndAssert(helper, clickedFace, look, powered, false);
         }
      }
   }

   private static void placeAndAssert(GameTestHelper helper, Direction clickedFace, Direction look, boolean powered, boolean replacing) {
      Block hatch = ModBlocks.IRON_HATCH.get();
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setYRot(look.toYRot());
      BlockPos target = helper.absolutePos(CENTER);
      BlockPos support = replacing ? target : target.relative(clickedFace.getOpposite());
      helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), 3);
      if (replacing) {
         helper.getLevel().setBlock(target.below(), Blocks.DIRT.defaultBlockState(), 3);
         helper.getLevel().setBlock(target, Blocks.SHORT_GRASS.defaultBlockState(), 3);
      } else {
         helper.getLevel().setBlock(support, (powered ? Blocks.REDSTONE_BLOCK : Blocks.STONE).defaultBlockState(), 3);
      }
      Vec3 hitPos = Vec3.atCenterOf(support).add(
         clickedFace.getStepX() * 0.5, clickedFace.getStepY() * 0.5, clickedFace.getStepZ() * 0.5);
      ItemStack stack = new ItemStack(hatch);
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND,
         stack, new BlockHitResult(hitPos, clickedFace, support, false));
      helper.assertValueEqual(context.getClickedPos(), target, "fixture must target the intended hatch position");
      helper.assertValueEqual(context.replacingClickedOnBlock(), replacing, "fixture must exercise the expected replacement branch");
      BlockState vanilla = Blocks.OAK_TRAPDOOR.getStateForPlacement(context);
      helper.assertTrue(vanilla != null, "vanilla reference must support this placement");
      helper.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(), "actual hatch item placement must succeed");
      BlockState placed = helper.getLevel().getBlockState(target);
      helper.assertTrue(placed.is(hatch), "actual item must place the iron hatch");
      helper.assertValueEqual(placed.getValue(PortedBlocks.POWERED), powered, "placement must preserve existing redstone power");
      helper.assertValueEqual(placed.getValue(PortedBlocks.OPEN), powered, "only powered placement must start open");
      // Hatch FACING denotes the hinge edge; vanilla FACING denotes its opposite.
      helper.assertValueEqual(placed.getValue(PortedBlocks.HORIZONTAL_FACING),
         vanilla.getValue(TrapDoorBlock.FACING).getOpposite(),
         "hatch hinge must match vanilla for face=" + clickedFace + ", look=" + look + ", powered=" + powered);
      if (!powered) {
         helper.useBlock(CENTER, player);
      }
      BlockState open = helper.getLevel().getBlockState(target);
      helper.assertTrue(open.getValue(PortedBlocks.OPEN), "hatch must be open for hinge comparison");
      AABB expected = vanilla.setValue(TrapDoorBlock.OPEN, true).getCollisionShape(helper.getLevel(), target).bounds();
      AABB outline = open.getShape(helper.getLevel(), target).bounds();
      AABB collision = open.getCollisionShape(helper.getLevel(), target).bounds();
      helper.assertValueEqual(outline, collision, "open hatch outline and collision must retain alignment");
      helper.assertTrue(sameEdge(outline.minX, outline.maxX, expected.minX, expected.maxX)
         && sameEdge(outline.minZ, outline.maxZ, expected.minZ, expected.maxZ),
         "open hatch leaf must occupy the same horizontal edge as the vanilla trapdoor");
      helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), 3);
      helper.getLevel().setBlock(support, Blocks.AIR.defaultBlockState(), 3);
      if (replacing) {
         helper.getLevel().setBlock(target.below(), Blocks.AIR.defaultBlockState(), 3);
      }
   }

   private static boolean sameEdge(double min, double max, double expectedMin, double expectedMax) {
      return (min == 0.0) == (expectedMin == 0.0) && (max == 1.0) == (expectedMax == 1.0);
   }
}
