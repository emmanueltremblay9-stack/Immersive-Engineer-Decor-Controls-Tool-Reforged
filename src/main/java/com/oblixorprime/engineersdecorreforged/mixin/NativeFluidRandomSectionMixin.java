package com.oblixorprime.engineersdecorreforged.mixin;
import com.oblixorprime.engineersdecorreforged.fluid.FluidTickSection;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LevelChunkSection.class)
public abstract class NativeFluidRandomSectionMixin implements FluidTickSection {
   @Unique private boolean iedctr$randomFluid;
   public void iedctr$randomFluid(boolean value) { iedctr$randomFluid=value; }
   @Inject(method="isRandomlyTicking", at=@At("RETURN"), cancellable=true)
   private void iedctr$includeCells(CallbackInfoReturnable<Boolean> cir) { if(iedctr$randomFluid)cir.setReturnValue(true); }
}
