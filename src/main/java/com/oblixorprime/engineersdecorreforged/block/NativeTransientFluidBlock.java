package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;

/** Explicit opt-in for native sources plus transient vanilla water; no global block integration. */
public interface NativeTransientFluidBlock extends NativeFluidloggedBlock {
   @Override
   default boolean canPlaceLiquid(@javax.annotation.Nullable net.minecraft.world.entity.player.Player player, BlockGetter level, BlockPos pos, BlockState state, net.minecraft.world.level.material.Fluid fluid) {
         var current = level.getFluidState(pos);
         if (!com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.standard(fluid)) return false;
         if (com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.reserved(level, pos) && current.isEmpty()) return false;
         return current.isEmpty() || (!current.isSource() && current.getType().isSame(fluid));
      }

   @Override
   default boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
         if (fluid.getType() == net.minecraft.world.level.material.Fluids.FLOWING_WATER
               && canPlaceLiquid(null, level, pos, state, fluid.getType())) {
            if (!level.isClientSide()) {
               if (!level.setBlock(pos, StairWaterFlow.withFlow(state, fluid), 3)) return false;
               level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
            }
            return true;
         }
         if (!canPlaceLiquid(null, level, pos, state, fluid.getType())) return false;
         if (!fluid.isSource() || (fluid.getType() != net.minecraft.world.level.material.Fluids.WATER
               && NativeFluidloggedBlock.cell(fluid.getType()) == NativeFluidloggedBlock.Cell.EMPTY))
            return com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.place(level, pos, state, fluid);
         return NativeFluidloggedBlock.super.placeLiquid(level, pos, state.setValue(StairWaterFlow.WATER_FLOW, 0), fluid);
      }

   @Override
   default net.minecraft.world.item.ItemStack pickupBlock(@javax.annotation.Nullable net.minecraft.world.entity.player.Player player, LevelAccessor level, BlockPos pos, BlockState state) {
         if (StairWaterFlow.active(state)) return net.minecraft.world.item.ItemStack.EMPTY;
         return NativeFluidloggedBlock.super.pickupBlock(player, level, pos, state.setValue(StairWaterFlow.WATER_FLOW, 0));
      }

}
