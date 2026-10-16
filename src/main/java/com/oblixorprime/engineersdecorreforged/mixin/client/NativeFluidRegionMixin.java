package com.oblixorprime.engineersdecorreforged.mixin.client;

import com.oblixorprime.engineersdecorreforged.fluid.*;
import java.util.*;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Snapshot attachments on the preparation thread, never read a live chunk map from mesh workers. */
@Mixin(RenderChunkRegion.class)
public abstract class NativeFluidRegionMixin {
   @Shadow @Final private int minChunkX;
   @Shadow @Final private int minChunkZ;
   @Shadow @Final protected Level level;
   @Unique private Map<Long, FluidState> iedctr$cells = Map.of();
   @Inject(method="<init>(Lnet/minecraft/world/level/Level;II[Lnet/minecraft/client/renderer/chunk/RenderChunk;Lit/unimi/dsi/fastutil/longs/Long2ObjectFunction;)V", at=@At("RETURN"))
   private void iedctr$snapshot(CallbackInfo ci) {
      var copy = new HashMap<Long, FluidState>();
      for (int x=minChunkX; x<minChunkX+3; x++) for (int z=minChunkZ; z<minChunkZ+3; z++) {
         var data = NativeFluidCells.snapshot(level.getChunk(x,z));
         for (long pos : data.positions()) copy.put(pos, data.get(BlockPos.of(pos)));
      }
      iedctr$cells = Map.copyOf(copy);
   }
   @Inject(method="getFluidState", at=@At("RETURN"), cancellable=true)
   private void iedctr$read(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
      if (cir.getReturnValue().isEmpty() && NativeFluidCells.supported(((RenderChunkRegion)(Object)this).getBlockState(pos))) {
         var cell = iedctr$cells.get(pos.asLong()); if (cell != null) cir.setReturnValue(cell);
      }
   }
}
