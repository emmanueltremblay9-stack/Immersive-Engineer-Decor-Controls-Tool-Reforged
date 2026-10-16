package com.oblixorprime.engineersdecorreforged.mixin.client;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.chunk.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(SectionCompiler.class)
public abstract class NativeFluidSectionMixin {
   @Redirect(method="compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"))
   private FluidState iedctr$cell(BlockState state, @Local(argsOnly=true) RenderChunkRegion region, @Local(ordinal=2) BlockPos pos) { return region.getFluidState(pos); }
}
