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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VerticalSlabPlacementGameTests {
   private static final BlockPos CENTER = new BlockPos(1, 1, 1);

   private VerticalSlabPlacementGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void structured_vertical_slab_places_flush_against_each_wall(GameTestHelper helper) {
      for (Direction look : Direction.Plane.HORIZONTAL) {
         placeAndAssert(helper, look, look.getOpposite());
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void structured_vertical_slab_floor_and_ceiling_placement_follow_player(GameTestHelper helper) {
      for (Direction look : Direction.Plane.HORIZONTAL) {
         placeAndAssert(helper, look, Direction.UP);
         placeAndAssert(helper, look, Direction.DOWN);
      }
      helper.succeed();
   }

   private static void placeAndAssert(GameTestHelper helper, Direction look, Direction clickedFace) {
      Block slab = ModBlocks.CLINKER_BRICK_VERTICAL_SLAB_STRUCTURED.get();
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setYRot(look.toYRot());
      BlockPos target = helper.absolutePos(CENTER);
      BlockPos support = target.relative(clickedFace.getOpposite());
      helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), 3);
      helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 3);
      Vec3 hitPos = Vec3.atCenterOf(support).add(
         clickedFace.getStepX() * 0.5, clickedFace.getStepY() * 0.5, clickedFace.getStepZ() * 0.5);
      ItemStack stack = new ItemStack(slab);
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND,
         stack, new BlockHitResult(hitPos, clickedFace, support, false));
      helper.assertValueEqual(context.getClickedPos(), target, "fixture must target the space beside the support");
      helper.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(), "slab item placement must succeed");
      BlockState placed = helper.getLevel().getBlockState(target);
      helper.assertTrue(placed.is(slab), "the actual item must place the structured slab");
      helper.assertValueEqual(placed.getValue(PortedBlocks.HORIZONTAL_FACING), look,
         "occupied half must face away from the player so the raised face faces the player");
      AABB expected = switch (look) {
         case NORTH -> new AABB(0, 0, 0, 1, 1, 0.5);
         case SOUTH -> new AABB(0, 0, 0.5, 1, 1, 1);
         case WEST -> new AABB(0, 0, 0, 0.5, 1, 1);
         case EAST -> new AABB(0.5, 0, 0, 1, 1, 1);
         default -> throw new IllegalArgumentException("Expected horizontal look direction");
      };
      helper.assertValueEqual(placed.getShape(helper.getLevel(), target).bounds(), expected,
         "outline must occupy the half next to the wall, without an air gap");
      helper.assertValueEqual(placed.getCollisionShape(helper.getLevel(), target).bounds(), expected,
         "collision must preserve the same occupied half as the model body");
      helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), 3);
      helper.getLevel().setBlock(support, Blocks.AIR.defaultBlockState(), 3);
   }
}
