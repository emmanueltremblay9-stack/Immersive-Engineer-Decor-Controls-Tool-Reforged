package com.oblixorprime.engineersdecorreforged.mixin;

import com.oblixorprime.engineersdecorreforged.fluid.*;
import com.oblixorprime.engineersdecorreforged.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class NativeFluidChunkMixin {
   @Inject(method="getFluidState(III)Lnet/minecraft/world/level/material/FluidState;", at=@At("HEAD"), cancellable=true)
   private void iedctr$cell(int x, int y, int z, CallbackInfoReturnable<FluidState> cir) {
      var chunk = (LevelChunk)(Object)this;
      var fluid = NativeFluidCells.stored(chunk, new BlockPos(x, y, z));
      if (!fluid.isEmpty()) cir.setReturnValue(fluid);
   }
   @Inject(method="setBlockState", at=@At("RETURN"))
   private void iedctr$keepOrClear(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> cir) {
      var previous = cir.getReturnValue();
      if (previous == null || (!NativeFluidCells.supported(previous) && !NativeFluidCells.supported(state))) return;
      var chunk = (LevelChunk)(Object)this;
      var data = NativeFluidCells.snapshot(chunk);
      if (!chunk.getLevel().isClientSide && (!NativeFluidCells.supported(state) || !state.getFluidState().isEmpty()) && data.contains(pos)) {
         chunk.setData(NativeFluidCells.CELLS.get(), data.with(pos, net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState()));
         chunk.setUnsaved(true);
         NativeFluidCells.refreshRandomTicks(chunk, NativeFluidCells.snapshot(chunk));
      } else if (!chunk.getLevel().isClientSide && NativeFluidCells.supported(state) && !NativeFluidCells.supported(previous)) {
         var fluid = previous.getFluidState();
         if (NativeFluidCells.acceptsCaptured(state, fluid) && !fluid.isEmpty() && state.getFluidState().isEmpty()
               && (fluid.isSource() || (NativeFluidCells.transientSupported(state) && !(state.getBlock() instanceof com.oblixorprime.engineersdecorreforged.utility.MachineBlocks.PassiveFluidAccumulatorBlock)))) {
            chunk.setData(NativeFluidCells.CELLS.get(), data.with(pos, fluid));
            chunk.setUnsaved(true);
            NativeFluidCells.refreshRandomTicks(chunk, NativeFluidCells.snapshot(chunk));
            if (!chunk.getLevel().isClientSide) chunk.getLevel().scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(chunk.getLevel()));
         }
      }
      // Client block packets may coalesce intermediate states; only the server mutates attachment cells.
      // A cell packet can arrive before its block packet, so refresh light after either order.
      NativeFluidCells.refreshLight(chunk,pos,NativeFluidCells.snapshot(chunk));
   }
}
