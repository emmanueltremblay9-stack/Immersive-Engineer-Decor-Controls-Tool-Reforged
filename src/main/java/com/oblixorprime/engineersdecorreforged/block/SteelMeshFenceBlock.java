package com.oblixorprime.engineersdecorreforged.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Keeps vanilla wall topology while recognizing the mod's segmented mesh gate. */
public final class SteelMeshFenceBlock extends WallBlock {
   public static final MapCodec<WallBlock> CODEC = simpleCodec(SteelMeshFenceBlock::new);

   public SteelMeshFenceBlock(Properties properties) {
      super(properties);
   }

   @Override
   public MapCodec<WallBlock> codec() {
      return CODEC;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = super.getStateForPlacement(context);
      if (state == null) {
         return null;
      }
      // Placement only calculates a candidate state. Avoid scheduling extra fluid ticks
      // while replaying vanilla's neighbor calculation, then restore its waterlogging.
      boolean waterlogged = state.getValue(WATERLOGGED);
      state = state.setValue(WATERLOGGED, false);
      BlockPos pos = context.getClickedPos();
      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos neighborPos = pos.relative(direction);
         BlockState neighbor = context.getLevel().getBlockState(neighborPos);
         if (neighbor.getBlock() instanceof PortedBlocks.FenceGateSegmentBlock) {
            state = super.updateShape(state, direction, connectionView(neighbor), context.getLevel(), pos, neighborPos);
         }
      }
      return state.setValue(WATERLOGGED, waterlogged);
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                    LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      BlockState connectionNeighbor = direction.getAxis().isHorizontal() ? connectionView(neighborState) : neighborState;
      return super.updateShape(state, direction, connectionNeighbor, level, pos, neighborPos);
   }

   private static BlockState connectionView(BlockState neighbor) {
      if (neighbor.getBlock() instanceof PortedBlocks.FenceGateSegmentBlock) {
         // WallBlock's private connection rule recognizes FenceGateBlock by type. Supply
         // a temporary directional view only to that wall calculation, never to the world.
         // Above/below neighbors retain their real geometry for wall heights and posts.
         return Blocks.OAK_FENCE_GATE.defaultBlockState()
            .setValue(FenceGateBlock.FACING, neighbor.getValue(PortedBlocks.HORIZONTAL_FACING))
            .setValue(FenceGateBlock.OPEN, neighbor.getValue(PortedBlocks.OPEN));
      }
      return neighbor;
   }
}
