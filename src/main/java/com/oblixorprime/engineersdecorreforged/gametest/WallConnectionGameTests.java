package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WallConnectionGameTests {
   private static final BlockPos CENTER = new BlockPos(1, 1, 1);

   private WallConnectionGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void decorative_walls_have_the_vanilla_connection_tag(GameTestHelper helper) {
      for (WallBlock wall : decorativeWalls()) {
         helper.assertTrue(wall.defaultBlockState().is(BlockTags.WALLS), wall + " must belong to minecraft:walls");
      }
      helper.assertTrue(Blocks.COBBLESTONE_WALL.defaultBlockState().is(BlockTags.WALLS), "vanilla wall tag entries must remain intact");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void decorative_walls_connect_to_themselves_in_every_direction(GameTestHelper helper) {
      for (WallBlock wall : decorativeWalls()) {
         for (Direction direction : Direction.Plane.HORIZONTAL) {
            assertPairConnects(helper, wall, wall, direction);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void mixed_decorative_walls_connect_in_every_direction(GameTestHelper helper) {
      for (WallBlock first : decorativeWalls()) {
         for (WallBlock second : decorativeWalls()) {
            if (first == second) {
               continue;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
               assertPairConnects(helper, first, second, direction);
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void decorative_and_vanilla_walls_connect_in_both_placement_orders(GameTestHelper helper) {
      for (WallBlock wall : decorativeWalls()) {
         for (Direction direction : Direction.Plane.HORIZONTAL) {
            assertPairConnects(helper, wall, Blocks.COBBLESTONE_WALL, direction);
            assertPairConnects(helper, Blocks.COBBLESTONE_WALL, wall, direction);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void decorative_walls_keep_solid_connections_and_disconnect_from_air(GameTestHelper helper) {
      for (WallBlock wall : decorativeWalls()) {
         for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = CENTER.relative(direction);
            placeWall(helper, wall, CENTER);
            helper.setBlock(neighbor, Blocks.STONE);
            assertSide(helper, CENTER, direction, WallSide.LOW, "wall should still connect to a solid block");
            helper.setBlock(neighbor, Blocks.AIR);
            assertSide(helper, CENTER, direction, WallSide.NONE, "wall should disconnect when the solid block is removed");
            helper.setBlock(CENTER, Blocks.AIR);
         }
      }
      helper.succeed();
   }

   private static List<WallBlock> decorativeWalls() {
      // Steel mesh fence intentionally uses WallBlock, so it needs the wall tag too.
      return List.of(ModBlocks.CLINKER_BRICK_WALL.get(), ModBlocks.SLAG_BRICK_WALL.get(),
         ModBlocks.REBAR_CONCRETE_WALL.get(), ModBlocks.STEEL_MESH_FENCE.get());
   }

   private static void assertPairConnects(GameTestHelper helper, Block first, Block second, Direction direction) {
      BlockPos neighbor = CENTER.relative(direction);
      placeWall(helper, first, CENTER);
      assertSide(helper, CENTER, direction, WallSide.NONE, "isolated wall must start disconnected");
      placeWall(helper, second, neighbor);
      String pair = first + " / " + second + " toward " + direction;
      assertSide(helper, CENTER, direction, WallSide.LOW, "existing wall must connect after neighbor placement: " + pair);
      assertSide(helper, neighbor, direction.getOpposite(), WallSide.LOW, "new wall must connect to existing wall: " + pair);
      helper.setBlock(neighbor, Blocks.AIR);
      assertSide(helper, CENTER, direction, WallSide.NONE, "wall must disconnect after neighbor removal: " + pair);
      helper.setBlock(CENTER, Blocks.AIR);
   }

   private static void placeWall(GameTestHelper helper, Block wall, BlockPos relativePos) {
      BlockPos absolutePos = helper.absolutePos(relativePos);
      BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false);
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), helper.makeMockPlayer(GameType.CREATIVE),
         InteractionHand.MAIN_HAND, new ItemStack(wall), hit);
      BlockState state = wall.getStateForPlacement(context);
      helper.assertTrue(state != null, "wall placement state must exist");
      helper.setBlock(relativePos, state);
   }

   private static void assertSide(GameTestHelper helper, BlockPos pos, Direction direction, WallSide expected, String message) {
      EnumProperty<WallSide> property = switch (direction) {
         case NORTH -> WallBlock.NORTH_WALL;
         case SOUTH -> WallBlock.SOUTH_WALL;
         case EAST -> WallBlock.EAST_WALL;
         case WEST -> WallBlock.WEST_WALL;
         default -> throw new IllegalArgumentException("Expected horizontal wall direction");
      };
      helper.assertValueEqual(helper.getBlockState(pos).getValue(property), expected, message);
   }
}
