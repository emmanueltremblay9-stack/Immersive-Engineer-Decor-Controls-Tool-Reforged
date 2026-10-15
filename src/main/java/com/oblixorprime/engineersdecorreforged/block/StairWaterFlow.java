package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** Only transient water in catwalk stairs. Existing WATERLOGGED sources keep their contract. */
public final class StairWaterFlow {
   private StairWaterFlow() {}
   public static boolean active(BlockState state) {
      return state.getBlock() instanceof PortedBlocks.CatwalkStairsBlock
         && !state.getValue(PortedBlocks.WATERLOGGED)
         && state.getValue(NativeFluidloggedBlock.FLUID_CELL) == NativeFluidloggedBlock.Cell.EMPTY
         && state.getValue(PortedBlocks.CatwalkStairsBlock.WATER_FLOW) != 0;
   }
   public static FluidState fluid(BlockState state) {
      FluidState stored = NativeFluidloggedBlock.fluidState(state);
      if (!stored.isEmpty()) return stored;
      int value = state.getValue(PortedBlocks.CatwalkStairsBlock.WATER_FLOW);
      return value == 0 ? stored : Fluids.FLOWING_WATER.getFlowing(value < 8 ? value : 16-value, value >= 8);
   }
   public static BlockState withFlow(BlockState state, FluidState fluid) {
      int value = fluid.isEmpty() ? 0 : fluid.getValue(FlowingFluid.FALLING) ? 16-fluid.getAmount() : fluid.getAmount();
      return state.setValue(PortedBlocks.CatwalkStairsBlock.WATER_FLOW, value);
   }
}
