package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** Transient water in explicitly opted-in stairs and decorative lights. Legacy sources retain their contract. */
public final class StairWaterFlow {
   public static final net.minecraft.world.level.block.state.properties.IntegerProperty WATER_FLOW =
      net.minecraft.world.level.block.state.properties.IntegerProperty.create("water_flow", 0, 15);
   private StairWaterFlow() {}
   public static boolean supported(BlockState state) {
      return state.getBlock() instanceof PortedBlocks.CatwalkStairsBlock
         || state.getBlock() instanceof NativeTransientFluidBlock;
   }
   public static boolean active(BlockState state) {
      return supported(state)
         && !state.getValue(PortedBlocks.WATERLOGGED)
         && state.getValue(NativeFluidloggedBlock.FLUID_CELL) == NativeFluidloggedBlock.Cell.EMPTY
         && state.getValue(WATER_FLOW) != 0;
   }
   public static FluidState fluid(BlockState state) {
      FluidState stored = NativeFluidloggedBlock.fluidState(state);
      if (!stored.isEmpty()) return stored;
      int value = state.getValue(WATER_FLOW);
      return value == 0 ? stored : Fluids.FLOWING_WATER.getFlowing(value < 8 ? value : 16-value, value >= 8);
   }
   public static BlockState withFlow(BlockState state, FluidState fluid) {
      int value = fluid.isEmpty() ? 0 : fluid.getValue(FlowingFluid.FALLING) ? 16-fluid.getAmount() : fluid.getAmount();
      return state.setValue(WATER_FLOW, value);
   }
}
