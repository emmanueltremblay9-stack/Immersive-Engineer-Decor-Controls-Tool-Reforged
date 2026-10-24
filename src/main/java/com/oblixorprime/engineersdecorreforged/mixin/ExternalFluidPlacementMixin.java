package com.oblixorprime.engineersdecorreforged.mixin;

import com.oblixorprime.engineersdecorreforged.fluid.ExternalFluidAdapter;
import com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class ExternalFluidPlacementMixin {
   @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
   private void iedctr$preserveOccupiedCell(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
      var next = cir.getReturnValue();
      if (next == null) return;
      var level = context.getLevel();
      var pos = context.getClickedPos();
      var old = level.getBlockState(pos);
      var fluid = level.getFluidState(pos);
      // Refuse a wet merge before item consumption instead of deleting a source or hiding it in a double slab.
      if (ExternalFluidAdapter.supported(old) && next.hasProperty(BlockStateProperties.SLAB_TYPE)
            && next.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE
            && (!fluid.isEmpty() || NativeFluidCells.reserved(level, pos))) {
         cir.setReturnValue(null);
      } else if (ExternalFluidAdapter.supported(next) && !fluid.isEmpty()
            && ExternalFluidAdapter.supportsFluid(fluid.getType()) && !fluid.equals(Fluids.WATER.getSource(false))) {
         cir.setReturnValue(next.setValue(BlockStateProperties.WATERLOGGED, false));
      }
   }
}
