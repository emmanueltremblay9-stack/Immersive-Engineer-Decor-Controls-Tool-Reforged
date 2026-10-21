package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModEntities;
import com.oblixorprime.engineersdecorreforged.stool.StoolSeatEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class StoolSeatRenderer extends EntityRenderer<StoolSeatEntity> {
   public StoolSeatRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @SubscribeEvent
   public static void register(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ModEntities.STOOL_SEAT.get(), StoolSeatRenderer::new);
   }

   @Override
   public boolean shouldRender(StoolSeatEntity entity, Frustum frustum, double x, double y, double z) {
      return false;
   }

   @Override
   public ResourceLocation getTextureLocation(StoolSeatEntity entity) {
      return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
   }
}
