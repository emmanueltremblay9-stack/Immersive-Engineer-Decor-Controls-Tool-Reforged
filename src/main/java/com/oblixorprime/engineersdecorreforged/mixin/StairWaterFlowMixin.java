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
   @org.spongepowered.asm.mixin.Shadow protected abstract int getDropOff(net.minecraft.world.level.LevelReader level);
   @org.spongepowered.asm.mixin.Shadow protected abstract boolean canPassThroughWall(net.minecraft.core.Direction direction, net.minecraft.world.level.BlockGetter level, BlockPos pos, BlockState state, BlockPos neighborPos, BlockState neighborState);
   @Redirect(method="tick", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), require=2)
   private boolean iedctr$keepStair(Level level, BlockPos pos, BlockState replacement, int flags) {
      BlockState current = level.getBlockState(pos);
      if (com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.active(level, pos)) {
         com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.set(level, pos, replacement.getFluidState());
         return true;
      }
      if (StairWaterFlow.active(current)) replacement = StairWaterFlow.withFlow(current, replacement.getFluidState());
      return level.setBlock(pos, replacement, flags);
   }

   @Inject(method="getNewLiquid", at=@At("RETURN"), cancellable=true)
   private void iedctr$noNewStoredSource(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<FluidState> cir) {
      var candidate = cir.getReturnValue();
      if (com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.transientSupported(state) && !level.getFluidState(pos).isSource() && candidate.isSource()) {
         var fluid = (FlowingFluid)(Object)this;
         boolean above = level.getFluidState(pos.above()).getType().isSame(fluid)
            && canPassThroughWall(net.minecraft.core.Direction.UP, level, pos, state, pos.above(), level.getBlockState(pos.above()));
         int amount = above ? 8 : 8-getDropOff(level);
         cir.setReturnValue(amount <= 0 ? Fluids.EMPTY.defaultFluidState() : fluid.getFlowing(Math.min(8, amount), above));
      }
   }
}
