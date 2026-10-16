package com.oblixorprime.engineersdecorreforged.mixin.client;

import net.minecraft.core.*;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Embeddium already reads the upper fluid positionally; do the same for the sampled cell. */
@Pseudo
@Mixin(targets="org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.FluidRenderer", remap=false)
public abstract class EmbeddiumFluidHeightMixin {
   @Redirect(method="fluidHeight", at=@At(value="INVOKE",
      target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"))
   private FluidState iedctr$height(BlockState state, BlockAndTintGetter level, Fluid fluid, BlockPos pos, Direction direction) {
      return level.getFluidState(pos);
   }
}
