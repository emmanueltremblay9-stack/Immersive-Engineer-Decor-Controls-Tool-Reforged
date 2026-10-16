package com.oblixorprime.engineersdecorreforged.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Preserve vanilla height/shape calculations, reading every fluid at its actual position. */
@Mixin(LiquidBlockRenderer.class)
public abstract class NativeFluidRendererReadsMixin {
   @Redirect(method="tesselate", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;", ordinal=0))
   private FluidState iedctr$below(BlockState state, BlockAndTintGetter level, BlockPos pos, VertexConsumer vertices, BlockState current, FluidState fluid) {
      return level.getFluidState(pos.below());
   }
   @Redirect(method="tesselate", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;", ordinal=1))
   private FluidState iedctr$above(BlockState state, BlockAndTintGetter level, BlockPos pos, VertexConsumer vertices, BlockState current, FluidState fluid) {
      return level.getFluidState(pos.above());
   }
   @Redirect(method="tesselate", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;", ordinal=2))
   private FluidState iedctr$north(BlockState state, BlockAndTintGetter level, BlockPos pos, VertexConsumer vertices, BlockState current, FluidState fluid) {
      return level.getFluidState(pos.north());
   }
   @Redirect(method="tesselate", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;", ordinal=3))
   private FluidState iedctr$south(BlockState state, BlockAndTintGetter level, BlockPos pos, VertexConsumer vertices, BlockState current, FluidState fluid) {
      return level.getFluidState(pos.south());
   }
   @Redirect(method="tesselate", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;", ordinal=4))
   private FluidState iedctr$west(BlockState state, BlockAndTintGetter level, BlockPos pos, VertexConsumer vertices, BlockState current, FluidState fluid) {
      return level.getFluidState(pos.west());
   }
   @Redirect(method="tesselate", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;", ordinal=5))
   private FluidState iedctr$east(BlockState state, BlockAndTintGetter level, BlockPos pos, VertexConsumer vertices, BlockState current, FluidState fluid) {
      return level.getFluidState(pos.east());
   }
   @Redirect(method="getHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;)F",
      at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"))
   private FluidState iedctr$height(BlockState state, BlockAndTintGetter level, Fluid fluid, BlockPos pos) {
      return level.getFluidState(pos);
   }
   @Redirect(method="getHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)F",
      at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"))
   private FluidState iedctr$heightAbove(BlockState state, BlockAndTintGetter level, Fluid fluid, BlockPos pos, BlockState current, FluidState currentFluid) {
      return level.getFluidState(pos.above());
   }
}
