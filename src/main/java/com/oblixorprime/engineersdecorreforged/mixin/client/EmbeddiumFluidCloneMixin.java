package com.oblixorprime.engineersdecorreforged.mixin.client;
import com.oblixorprime.engineersdecorreforged.fluid.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="org.embeddedt.embeddium.impl.world.cloned.ClonedChunkSection", remap=false)
public abstract class EmbeddiumFluidCloneMixin implements FluidRenderSnapshot {
   @Unique private Map<Long, FluidState> iedctr$cells = Map.of();
   @Inject(method="<init>", at=@At("RETURN"))
   private void iedctr$snapshot(Level level, LevelChunk chunk, LevelChunkSection section, SectionPos position, CallbackInfo ci) {
      var copy = new HashMap<Long, FluidState>(); var cells=NativeFluidCells.snapshot(chunk);
      for(long packed:cells.positions()) {
         var pos=BlockPos.of(packed);
         if(SectionPos.of(pos).equals(position)) copy.put(packed,cells.get(pos));
      }
      iedctr$cells=Map.copyOf(copy);
   }
   public Map<Long,FluidState> iedctr$fluidSnapshot(){return iedctr$cells;}
}
