package com.oblixorprime.engineersdecorreforged.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Keep vanilla level/delay/occlusion calculations; read fluid cells at their actual positions. */
// Permit strict positional-read injections into Lithium 0.15.4's
// priority-1000 isWaterHole overwrite as well as the vanilla implementation.
@Mixin(value=FlowingFluid.class, priority=1100)
public abstract class FluidNeighbourReadsMixin {
   @Redirect(method="getNewLiquid", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;", ordinal=0))
   private FluidState iedctr$neighbor(BlockState state, Level level, BlockPos origin, BlockState current, @Local(ordinal=1) BlockPos neighbor) { return level.getFluidState(neighbor); }
   @Redirect(method="getNewLiquid", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;", ordinal=1))
   private FluidState iedctr$below(BlockState state, Level level, BlockPos pos, BlockState current) { return level.getFluidState(pos.below()); }
   @Redirect(method="getNewLiquid", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;", ordinal=2))
   private FluidState iedctr$above(BlockState state, Level level, BlockPos pos, BlockState current) { return level.getFluidState(pos.above()); }
   @Redirect(method="isWaterHole", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"))
   private FluidState iedctr$hole(BlockState state, BlockGetter level, Fluid fluid, BlockPos origin, BlockState current, BlockPos pos, BlockState target) { return level.getFluidState(pos); }
   @Redirect(method="lambda$getSpread$3", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"))
   private static FluidState iedctr$spread(BlockState state, Level level, BlockPos pos, short key) { return level.getFluidState(pos); }
   @Redirect(method="lambda$getSlopeDistance$1", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"))
   private static FluidState iedctr$slope(BlockState state, LevelReader level, BlockPos pos, short key) { return level.getFluidState(pos); }
}
