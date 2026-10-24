package com.oblixorprime.engineersdecorreforged.mixin;

import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.fluid.ExternalFluidAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ExternalFluidStateMixin implements IBlockStateExtension {
   @Override
   public boolean hasDynamicLightEmission() {
      return ExternalFluidAdapter.supported((BlockState)(Object)this) || IBlockStateExtension.super.hasDynamicLightEmission();
   }

   @Override
   public int getLightEmission(BlockGetter level, BlockPos pos) {
      int nativeLight = IBlockStateExtension.super.getLightEmission(level, pos);
      if (!ExternalFluidAdapter.supported((BlockState)(Object)this)) return nativeLight;
      // Light workers must never request a FULL chunk or a live fluid map.
      var auxiliary = level.getAuxLightManager(pos);
      return Math.max(nativeLight, auxiliary == null ? 0 : auxiliary.getLightAt(pos));
   }

   @Inject(method = "updateShape", at = @At("RETURN"))
   private void iedctr$wakeCell(Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
      if (!level.isClientSide() && ExternalFluidAdapter.supported((BlockState)(Object)this))
         NativeFluidloggedBlock.schedule((BlockState)(Object)this, level, pos);
   }
}
