package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Roof fitting with a selectable rim for placing its separate chimney cap. */
public final class ChimneyTrunkBlock extends StairBlock {
   // Geometry from the preserved dark_shingle_roof_chimneytrunk_model.json.
   // Original Engineer's Decor model: Stefan Wilhelm / wile, MIT (see NOTICE.md).
   // Preserve the visible rim at y=16 and the hollow center; an ordinary stair loses both.
   private static final double[][] MODEL_BOXES = {
      {0, 0, 0, 4, 3.5, 16},
      {4, 0, 0, 8, 7.5, 3},
      {4, 0, 13, 8, 7.5, 16},
      {3, 1, 3, 12, 11.5, 5},
      {4, 0, 3, 12, 1, 5},
      {4, 0, 11, 12, 1, 13},
      {3, 1, 11, 12, 11.5, 13},
      {3, 11.5, 3, 13, 16, 5},
      {3, 11.5, 11, 13, 16, 13},
      {11, 11.5, 5, 13, 16, 11},
      {3, 11.5, 5, 5, 16, 11},
      {3, 1, 5, 5, 11.5, 11},
      {11, 0, 5, 12, 11.5, 11},
      {4, 0, 5, 5, 1, 11},
      {8, 0, 0, 12, 11.5, 3},
      {8, 0, 13, 12, 11.5, 16},
      {12, 0, 0, 16, 15.5, 16},
      {4, 7.5, 0, 8, 7.9375, 3},
      {12, 15.5, 0, 16, 15.9375, 3},
      {13, 15.5, 3, 16, 15.9375, 4},
      {15, 15.5, 4, 16, 15.9375, 8},
      {15.9375, 15.9375, 0, 16, 16, 16},
      {11, 11.5, 0, 12, 11.9375, 4},
      {3, 3.5, 0, 4, 3.9375, 3},
      {15, 15.5, 12, 16, 15.9375, 16},
      {7, 7.5, 13, 8, 7.9375, 16},
      {0, 3.5, 4, 3, 3.9375, 8},
      {0, 3.5, 12, 4, 3.9375, 16},
      {8, 11.5, 13, 12, 11.9375, 16},
      {13, 15.5, 8, 16, 15.9375, 12}
   };
   private static final VoxelShape[][] SHAPES = makeShapes();

   public ChimneyTrunkBlock(Properties properties) {
      super(Blocks.STONE.defaultBlockState(), properties);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState placed = super.getStateForPlacement(context);
      return placed == null ? null : placed.setValue(HALF, Half.BOTTOM).setValue(SHAPE, StairsShape.STRAIGHT);
   }

   @Override
   public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                 LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      if (state.getValue(WATERLOGGED)) {
         level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }
      // Never auto-convert this fitting to a corner: corner models have no chimney trunk.
      // Preserve existing serialized states rather than migrating old worlds implicitly.
      return state;
   }

   @Override
   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (state.getValue(SHAPE) != StairsShape.STRAIGHT) {
         return super.getShape(state, level, pos, context);
      }
      return SHAPES[state.getValue(HALF).ordinal()][state.getValue(FACING).get2DDataValue()];
   }

   @Override
   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return getShape(state, level, pos, context);
   }

   private static VoxelShape[][] makeShapes() {
      VoxelShape[][] result = new VoxelShape[2][4];
      for (Half half : Half.values()) {
         for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            int turns = switch (facing) {
               case EAST -> 0;
               case SOUTH -> 1;
               case WEST -> 2;
               case NORTH -> 3;
               default -> throw new IllegalArgumentException("Horizontal facing required");
            };
            for (double[] box : MODEL_BOXES) {
               double x1 = box[0], y1 = box[1], z1 = box[2];
               double x2 = box[3], y2 = box[4], z2 = box[5];
               if (half == Half.TOP) {
                  double oldY = y1, oldZ = z1;
                  y1 = 16 - y2; y2 = 16 - oldY;
                  z1 = 16 - z2; z2 = 16 - oldZ;
               }
               for (int i = 0; i < turns; i++) {
                  double oldX1 = x1, oldX2 = x2;
                  x1 = 16 - z2; x2 = 16 - z1;
                  z1 = oldX1; z2 = oldX2;
               }
               shape = Shapes.or(shape, box(x1, y1, z1, x2, y2, z2));
            }
            result[half.ordinal()][facing.get2DDataValue()] = shape.optimize();
         }
      }
      return result;
   }
}
