package com.oblixorprime.engineersdecorreforged.mixin;

import com.oblixorprime.engineersdecorreforged.fluid.ExternalFluidAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla buckets and FlowingFluid both reach these container methods; native IEDCTR overrides remain intact. */
@Mixin(SimpleWaterloggedBlock.class)
public interface ExternalFluidContainerMixin {
   @Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
   default void iedctr$canFill(Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
      if (ExternalFluidAdapter.supported(state)) cir.setReturnValue(ExternalFluidAdapter.canPlace(level, pos, state, fluid));
   }

   @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
   default void iedctr$fill(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
      if (ExternalFluidAdapter.supported(state)) cir.setReturnValue(ExternalFluidAdapter.place(level, pos, state, fluid));
   }

   @Inject(method = "pickupBlock", at = @At("HEAD"), cancellable = true)
   default void iedctr$drain(Player player, LevelAccessor level, BlockPos pos, BlockState state, CallbackInfoReturnable<ItemStack> cir) {
      if (ExternalFluidAdapter.supported(state)) cir.setReturnValue(ExternalFluidAdapter.pickup(level, pos, state));
   }
}
