package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Six-face MK3/MK3.1 visual topology. Candidate-to-candidate matches are distinct
 * from one-sided legacy junction plates; legacy block states are never mutated.
 *
 * Priority: matching axial Heavy support > Heavy branch/legacy physical junction
 * > sturdy solid block face anchor > exposed face.
 */
public final class HeavyDoubleTTopology {
   private static final double EPS = 1.0E-6;
   private HeavyDoubleTTopology() {}

   public enum Contact { NONE, AXIAL, JUNCTION, ANCHOR }
   public record Result(int axial, int junction, int anchor) {
      public Contact contact(Direction face) {
         int bit = 1 << face.ordinal();
         if ((axial & bit) != 0) return Contact.AXIAL;
         if ((junction & bit) != 0) return Contact.JUNCTION;
         if ((anchor & bit) != 0) return Contact.ANCHOR;
         return Contact.NONE;
      }
   }

   public static Result resolve(BlockGetter level, BlockPos pos, BlockState state) {
      if (!(state.getBlock() instanceof HeavyDoubleTSupportBlock)) return new Result(0, 0, 0);
      Direction axis = state.getValue(HeavyDoubleTSupportBlock.FACING);
      Direction transverse = HeavyDoubleTOrientation.right(axis, state.getValue(HeavyDoubleTSupportBlock.ROLL));
      int axial = 0, junction = 0, anchor = 0;
      for (Direction face : Direction.values()) {
         BlockPos neighborPos = pos.relative(face);
         BlockState next = level.getBlockState(neighborPos);
         int bit = 1 << face.ordinal();
         if (next.getBlock() instanceof HeavyDoubleTSupportBlock) {
            Direction nextAxis = next.getValue(HeavyDoubleTSupportBlock.FACING);
            Direction nextTransverse = HeavyDoubleTOrientation.right(nextAxis, next.getValue(HeavyDoubleTSupportBlock.ROLL));
            if (face.getAxis() == axis.getAxis() && nextAxis.getAxis() == axis.getAxis()
               && nextTransverse.getAxis() == transverse.getAxis()) {
               axial |= bit;
            } else if (face.getAxis() != axis.getAxis() && face.getAxis() == nextAxis.getAxis()) {
               junction |= bit;
            }
            // Incompatible Heavy members do not become arbitrary anchor plates.
         } else if ((next.getBlock() instanceof PortedBlocks.SupportBlock
                  || next.getBlock() instanceof VerticalDoubleTSupportBlock)
               && touchesLegacyFace(level, pos, state, neighborPos, next, face)) {
            // Existing steel Double-T geometry is not the same section as the MK3
            // heavy I-profile: show a junction, not a falsely seamless axial join.
            junction |= bit;
         } else if (next.isFaceSturdy(level, neighborPos, face.getOpposite())) {
            // The backing block really owns the touching exterior face.
            anchor |= bit;
         }
      }
      return new Result(axial, junction, anchor);
   }

   /**
    * Require positive-area overlap on the same world contact plane.
    * Both shapes are local to their own cells. No assumptions about legacy
    * facing, attached Iron Bulbs, connectors, or backing walls are invented.
    */
   static boolean touchesLegacyFace(BlockGetter level, BlockPos pos, BlockState state,
         BlockPos otherPos, BlockState other, Direction face) {
      VoxelShape own = state.getCollisionShape(level, pos);
      VoxelShape legacy = other.getCollisionShape(level, otherPos);
      for (AABB a : own.toAabbs()) {
         if (!reaches(a, face)) continue;
         for (AABB b : legacy.toAabbs()) {
            if (reaches(b, face.getOpposite()) && overlapsAcrossPlane(a, b, face.getAxis())) return true;
         }
      }
      return false;
   }

   private static boolean reaches(AABB box, Direction face) {
      return switch(face) {
         case DOWN -> box.minY <= EPS;
         case UP -> box.maxY >= 1.0 - EPS;
         case NORTH -> box.minZ <= EPS;
         case SOUTH -> box.maxZ >= 1.0 - EPS;
         case WEST -> box.minX <= EPS;
         case EAST -> box.maxX >= 1.0 - EPS;
      };
   }

   private static boolean positiveArea(double aMin, double aMax, double bMin, double bMax) {
      return Math.min(aMax, bMax) - Math.max(aMin, bMin) > EPS;
   }

   private static boolean overlapsAcrossPlane(AABB a, AABB b, Direction.Axis normal) {
      return switch (normal) {
         case X -> positiveArea(a.minY, a.maxY, b.minY, b.maxY)
               && positiveArea(a.minZ, a.maxZ, b.minZ, b.maxZ);
         case Y -> positiveArea(a.minX, a.maxX, b.minX, b.maxX)
               && positiveArea(a.minZ, a.maxZ, b.minZ, b.maxZ);
         case Z -> positiveArea(a.minX, a.maxX, b.minX, b.maxX)
               && positiveArea(a.minY, a.maxY, b.minY, b.maxY);
      };
   }
}
