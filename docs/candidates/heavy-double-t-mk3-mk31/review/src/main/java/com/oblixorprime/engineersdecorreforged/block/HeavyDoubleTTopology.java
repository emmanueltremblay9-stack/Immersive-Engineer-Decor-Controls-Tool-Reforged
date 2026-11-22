package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Read-only six-face topology. The order is AXIAL > JUNCTION > ANCHOR > NONE. */
public final class HeavyDoubleTTopology {
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
               axial |= bit; // same I-beam cross-section, including reversed axis polarity
            } else if (face.getAxis() != axis.getAxis() && face.getAxis() == nextAxis.getAxis()) {
               junction |= bit; // perpendicular T / cross branch actually points into this face
            }
            // Other heavy supports aren't anchors merely because they occupy an adjacent cell.
         } else if (next.isFaceSturdy(level, neighborPos, face.getOpposite())) {
            anchor |= bit; // face-to-face solid backing; air and non-sturdy partial faces rejected
         }
      }
      return new Result(axial, junction, anchor);
   }
}
