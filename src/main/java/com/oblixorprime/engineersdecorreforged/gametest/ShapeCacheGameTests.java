package com.oblixorprime.engineersdecorreforged.gametest;

import static com.oblixorprime.engineersdecorreforged.block.PortedBlocks.*;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.StairWaterFlow;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Issue 93: frozen pre-cache geometry oracle from release 1.1.64, not the new cache builder. */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShapeCacheGameTests {
   @GameTest(template = "empty", timeoutTicks = 1200)
   public static void shape_caches_preserve_all_states(GameTestHelper h) {
      verify(h, ModBlocks.STEEL_CATWALK_STAIRS.get(), true, 30720);
      verify(h, ModBlocks.STEEL_DOUBLE_T_SUPPORT.get(), false, 2304);
      h.succeed();
   }

   private static void verify(GameTestHelper h, Block block, boolean stairs, int count) {
      var properties = new java.util.HashSet<>(block.getStateDefinition().getProperties());
      var expectedProperties = stairs
         ? java.util.Set.of(HORIZONTAL_FACING, LEFT_RAILING, RIGHT_RAILING, WATERLOGGED,
            NativeFluidloggedBlock.FLUID_CELL, StairWaterFlow.WATER_FLOW, EDITED_STAIR_SIDES, PAID_RAILINGS)
         : java.util.Set.of(EASTWEST, LEFTBEAM, RIGHTBEAM, DOWNCONNECT, WATERLOGGED,
            NativeFluidloggedBlock.FLUID_CELL, StairWaterFlow.WATER_FLOW);
      h.assertTrue(properties.equals(expectedProperties), "legacy state property schema retained");
      var states = block.getStateDefinition().getPossibleStates();
      h.assertValueEqual(states.size(), count, "save-compatible state count");
      var outlines = new VoxelShape[16];
      var collisions = new VoxelShape[16];
      var expectedOutlines = new VoxelShape[16];
      var expectedCollisions = new VoxelShape[16];
      var pos = h.absolutePos(new BlockPos(2, 2, 2));
      var context = CollisionContext.empty();
      for (BlockState state : states) {
         int key = stairs
            ? (state.getValue(HORIZONTAL_FACING).get2DDataValue() << 2) | (state.getValue(LEFT_RAILING) ? 1 : 0) | (state.getValue(RIGHT_RAILING) ? 2 : 0)
            : (state.getValue(EASTWEST) ? 8 : 0) | (state.getValue(LEFTBEAM) ? 4 : 0) | (state.getValue(RIGHTBEAM) ? 2 : 0) | (state.getValue(DOWNCONNECT) > 0 ? 1 : 0);
         var outline = state.getShape(h.getLevel(), pos, context);
         var collision = state.getCollisionShape(h.getLevel(), pos, context);
         if (outlines[key] == null) {
            outlines[key] = outline;
            collisions[key] = collision;
            expectedOutlines[key] = stairs ? legacyStairOutline(state, h.getLevel(), pos, context) : legacySupport(state, h.getLevel(), pos, context);
            expectedCollisions[key] = stairs ? legacyStairCollision(state, h.getLevel(), pos, context) : expectedOutlines[key];
         }
         h.assertTrue(outlines[key] == outline && collisions[key] == collision, "reuse across every fluid/payment/history state: " + key);
         h.assertTrue(!Shapes.joinIsNotEmpty(outline, expectedOutlines[key], BooleanOp.NOT_SAME), "legacy outline parity: " + key);
         h.assertTrue(!Shapes.joinIsNotEmpty(collision, expectedCollisions[key], BooleanOp.NOT_SAME), "legacy collision parity: " + key);
         h.assertTrue(outline == state.getShape(h.getLevel(), pos, context), "repeated outline query reuses cache");
         h.assertTrue(collision == state.getCollisionShape(h.getLevel(), pos, context), "repeated collision query reuses cache");
      }
      for (int key = 0; key < 16; key++) h.assertTrue(outlines[key] != null, "every geometry key covered: " + key);
   }

   private static final VoxelShape CATWALK_FLOOR_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0);
   private static final VoxelShape SUPPORT_BEAM_NORTH_SOUTH_SHAPE = Shapes.or(
      Block.box(5.0, 15.0, 0.0, 11.0, 16.0, 16.0),
      new VoxelShape[]{
         Block.box(5.0, 11.0, 0.0, 11.0, 12.0, 16.0),
         Block.box(6.0, 12.0, 0.0, 7.0, 15.0, 16.0),
         Block.box(9.0, 12.0, 0.0, 10.0, 15.0, 16.0)
      }
   );
   private static final VoxelShape SUPPORT_BEAM_EAST_WEST_SHAPE = Shapes.or(
      Block.box(0.0, 15.0, 5.0, 16.0, 16.0, 11.0),
      new VoxelShape[]{
         Block.box(0.0, 11.0, 5.0, 16.0, 12.0, 11.0),
         Block.box(0.0, 12.0, 6.0, 16.0, 15.0, 7.0),
         Block.box(0.0, 12.0, 9.0, 16.0, 15.0, 10.0)
      }
   );
   private static final VoxelShape[][] STAIR_RAIL_SHAPES = stairRailShapes(false);
   private static final VoxelShape[][] STAIR_RAIL_COLLISIONS = stairRailShapes(true);

   private static VoxelShape catwalkStairShape(Direction facing) {
      return switch (facing) {
         case NORTH -> Shapes.or(Block.box(0, 0, 8, 16, 4, 16), Block.box(0, 4, 0, 16, 12, 8));
         case SOUTH -> Shapes.or(Block.box(0, 0, 0, 16, 4, 8), Block.box(0, 4, 8, 16, 12, 16));
         case WEST -> Shapes.or(Block.box(8, 0, 0, 16, 4, 16), Block.box(0, 4, 0, 8, 12, 16));
         case EAST -> Shapes.or(Block.box(0, 0, 0, 8, 4, 16), Block.box(8, 4, 0, 16, 12, 16));
         default -> CATWALK_FLOOR_SHAPE;
      };
   }

   private static VoxelShape[][] stairRailShapes(boolean collision) {
      VoxelShape[][] result = new VoxelShape[4][2];
      for (int side = 0; side < 2; side++) {
         VoxelShape north = Shapes.empty();
         double minX = side == 0 ? 0 : 14;
         for (int z = 0; z < 16; z++) {
            double floor = z < 8 ? 12 : 4;
            // Keep the visible sloped outline, but guard the whole side above the highest tread.
            // A locally lowered cap is only one block above the upper tread, allowing a diagonal
            // normal/auto-jump over its lower half before the entity descends onto the lower tread.
            double top = collision ? 12 + 24 : 30.5 - z * 15.0 / 16.0;
            north = Shapes.or(north, Block.box(minX, floor, z, minX + 2, top, z + 1));
         }
         for (Direction facing : Direction.Plane.HORIZONTAL) {
            result[facing.get2DDataValue()][side] = orientedNorthShape(north, facing, true);
         }
      }
      return result;
   }

   private static VoxelShape orientedNorthShape(VoxelShape northShape, Direction facing, boolean horizontalRotation) {
      VoxelShape oriented = Shapes.empty();
      for (AABB box : northShape.toAabbs()) {
         oriented = Shapes.or(oriented, Shapes.create(orientedNorthBox(box, facing, horizontalRotation)));
      }

      return oriented.optimize();
   }

   private static AABB orientedNorthBox(AABB box, Direction facing, boolean horizontalRotation) {
      if (horizontalRotation && !facing.getAxis().isHorizontal()) {
         return box;
      }

      return switch (facing) {
         case DOWN -> new AABB(1.0 - box.maxX, box.minZ, box.minY, 1.0 - box.minX, box.maxZ, box.maxY);
         case UP -> new AABB(
            1.0 - box.maxX, 1.0 - box.maxZ, 1.0 - box.maxY, 1.0 - box.minX, 1.0 - box.minZ, 1.0 - box.minY
         );
         case NORTH -> box;
         case SOUTH -> new AABB(1.0 - box.maxX, box.minY, 1.0 - box.maxZ, 1.0 - box.minX, box.maxY, 1.0 - box.minZ);
         case WEST -> new AABB(box.minZ, box.minY, 1.0 - box.maxX, box.maxZ, box.maxY, 1.0 - box.minX);
         case EAST -> new AABB(1.0 - box.maxZ, box.minY, box.minX, 1.0 - box.minZ, box.maxY, box.maxX);
      };
   }
      private static VoxelShape legacyStairOutline(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         VoxelShape shape = catwalkStairShape((Direction)state.getValue(HORIZONTAL_FACING));
         if ((Boolean)state.getValue(LEFT_RAILING)) {
            shape = Shapes.or(shape, STAIR_RAIL_SHAPES[state.getValue(HORIZONTAL_FACING).get2DDataValue()][0]);
         }

         if ((Boolean)state.getValue(RIGHT_RAILING)) {
            shape = Shapes.or(shape, STAIR_RAIL_SHAPES[state.getValue(HORIZONTAL_FACING).get2DDataValue()][1]);
         }

         return shape;
      }
      private static VoxelShape legacyStairCollision(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         Direction facing = state.getValue(HORIZONTAL_FACING);
         VoxelShape shape = catwalkStairShape(facing);
         if (state.getValue(LEFT_RAILING)) {
            shape = Shapes.or(shape, STAIR_RAIL_COLLISIONS[facing.get2DDataValue()][0]);
         }
         if (state.getValue(RIGHT_RAILING)) {
            shape = Shapes.or(shape, STAIR_RAIL_COLLISIONS[facing.get2DDataValue()][1]);
         }
         return shape;
      }
      private static VoxelShape legacySupport(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         boolean eastWest = state.getValue(EASTWEST);
         VoxelShape beam = eastWest ? SUPPORT_BEAM_EAST_WEST_SHAPE : SUPPORT_BEAM_NORTH_SOUTH_SHAPE;
         if (state.getValue(LEFTBEAM)) {
            beam = Shapes.or(beam, eastWest ? Block.box(5.0, 11.0, 0.0, 11.0, 16.0, 5.0) : Block.box(0.0, 11.0, 5.0, 5.0, 16.0, 11.0));
         }

         if (state.getValue(RIGHTBEAM)) {
            beam = Shapes.or(beam, eastWest ? Block.box(5.0, 11.0, 11.0, 11.0, 16.0, 16.0) : Block.box(11.0, 11.0, 5.0, 16.0, 16.0, 11.0));
         }

         if ((Integer)state.getValue(DOWNCONNECT) > 0) {
            beam = Shapes.or(beam, Block.box(6.0, 0.0, 6.0, 10.0, 11.0, 10.0));
         }

         return beam;
      }
}
