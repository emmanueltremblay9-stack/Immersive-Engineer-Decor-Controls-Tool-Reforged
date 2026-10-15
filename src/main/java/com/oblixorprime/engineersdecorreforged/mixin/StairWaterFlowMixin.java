package com.oblixorprime.engineersdecorreforged.mixin;

import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.StairWaterFlow;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla computes all levels/delays/spread; only its replacement of our occupied block is adapted. */
@Mixin(FlowingFluid.class)
public abstract class StairWaterFlowMixin {
   @Redirect(method="tick", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), require=2)
   private boolean iedctr$keepStair(Level level, BlockPos pos, BlockState replacement, int flags) {
      BlockState current = level.getBlockState(pos);
      if (StairWaterFlow.active(current)) replacement = StairWaterFlow.withFlow(current, replacement.getFluidState());
      return level.setBlock(pos, replacement, flags);
   }

   @Inject(method="getNewLiquid", at=@At("RETURN"), cancellable=true)
   private void iedctr$noNewStoredSource(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<FluidState> cir) {
      if (state.getBlock() instanceof PortedBlocks.CatwalkStairsBlock && !state.getValue(PortedBlocks.WATERLOGGED)
            && state.getValue(com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.FLUID_CELL) == com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.Cell.EMPTY
            && cir.getReturnValue().getType() == Fluids.WATER) {
         // Two neighboring sources must not turn a transient stair cell into an immortal source.
         boolean above = level.getFluidState(pos.above()).getType().isSame(Fluids.WATER);
         cir.setReturnValue(Fluids.FLOWING_WATER.getFlowing(above ? 8 : 7, above));
      }
   }
}
