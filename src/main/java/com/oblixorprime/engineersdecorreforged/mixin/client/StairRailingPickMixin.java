package com.oblixorprime.engineersdecorreforged.mixin.client;

import com.oblixorprime.engineersdecorreforged.block.StairRailingTargeting;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NeoForge 1.21.1 has no post-pick event; keep vanilla's complete result unless our upper rail is nearer. */
@Mixin(GameRenderer.class)
abstract class StairRailingPickMixin {
   @Inject(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
      at = @At("RETURN"), cancellable = true, require = 1)
   private void engineersdecorreforged$pickUpperStairRail(Entity camera, double blockRange, double entityRange,
                                                        float partialTick, CallbackInfoReturnable<HitResult> callback) {
      HitResult vanilla = callback.getReturnValue();
      HitResult selected = StairRailingTargeting.extendPick(camera, blockRange, partialTick, vanilla);
      if (selected != vanilla) {
         callback.setReturnValue(selected);
      }
   }
}
