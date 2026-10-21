package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

/** Structural pole ends, not wall-mounted plates. Keeps the existing six facing states. */
public final class PoleHeadBlock extends PortedBlocks.DirectionalShapeBlock {
   public PoleHeadBlock(Properties properties, double shaftMin, double shaftMax, double plateMin, double plateMax) {
      super(properties, Shapes.or(Block.box(shaftMin, shaftMin, 0, shaftMax, shaftMax, 14),
         Block.box(plateMin, plateMin, 14, plateMax, plateMax, 16)), false, false);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction facing = context.getClickedFace();
      BlockState support = context.getLevel().getBlockState(context.getClickedPos().relative(facing.getOpposite()));
      // Upstream EdStraightPoleBlock CFG_FLIP_PLACEMENT_IF_SAME: on a pole,
      // put the end plate away from the clicked connection instead of making another foot.
      if (support.getBlock() instanceof PortedBlocks.CenteredPoleBlock
            || support.getBlock() instanceof PoleHeadBlock
            || support.is(ModBlocks.TREATED_WOOD_POLE_SUPPORT.get())) {
         facing = facing.getOpposite();
      }
      return NativeFluidloggedBlock.placement(defaultBlockState().setValue(FACING, facing), context);
   }
}
