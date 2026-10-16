package com.oblixorprime.engineersdecorreforged.mixin;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ServerLevel.class)
public abstract class NativeFluidRandomTickMixin {
   @Redirect(method="tickChunk", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"))
   private FluidState iedctr$randomCell(BlockState state, @Local BlockPos pos) { return ((ServerLevel)(Object)this).getFluidState(pos); }
}
