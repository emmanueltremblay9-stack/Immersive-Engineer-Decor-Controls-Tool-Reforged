package com.oblixorprime.engineersdecorreforged.mixin.client;
import com.oblixorprime.engineersdecorreforged.fluid.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.embeddedt.embeddium.impl.world.WorldSlice;
import org.embeddedt.embeddium.impl.world.cloned.ChunkRenderContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets="org.embeddedt.embeddium.impl.world.WorldSlice", remap=false)
public abstract class EmbeddiumFluidSliceMixin {
   @Unique private Map<Long, FluidState> iedctr$cells = Map.of();
   @Inject(method="copyData", at=@At("RETURN"))
   private void iedctr$snapshot(ChunkRenderContext context, CallbackInfo ci) {
      var copy = new HashMap<Long,FluidState>();
      for(var section:context.getSections())if(section!=null)copy.putAll(((FluidRenderSnapshot)(Object)section).iedctr$fluidSnapshot());
      iedctr$cells=Map.copyOf(copy);
   }
   @Inject(method="getFluidState", at=@At("RETURN"), cancellable=true)
   private void iedctr$read(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
      if(cir.getReturnValue().isEmpty() && NativeFluidCells.supported(((net.minecraft.world.level.BlockGetter)(Object)this).getBlockState(pos))) {
         var fluid=iedctr$cells.get(pos.asLong());if(fluid!=null)cir.setReturnValue(fluid);
      }
   }
   @Inject(method="reset", at=@At("RETURN"))
   private void iedctr$reset(CallbackInfo ci){iedctr$cells=Map.of();}
}
