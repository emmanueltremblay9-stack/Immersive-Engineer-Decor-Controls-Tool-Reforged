package com.oblixorprime.engineersdecorreforged.mixin.client;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.embeddedt.embeddium.impl.world.WorldSlice;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets="org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderMeshingTask", remap=false)
public abstract class EmbeddiumFluidMeshMixin {
   @Redirect(method="execute(Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildContext;Lorg/embeddedt/embeddium/impl/util/task/CancellationToken;)Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildOutput;", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"))
   private FluidState iedctr$read(BlockState state, @Local WorldSlice slice, @Local(ordinal=0) BlockPos.MutableBlockPos pos) {return ((net.minecraft.world.level.BlockGetter)(Object)slice).getFluidState(pos);}
}
