package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class TrackerClientEvents {
   private TrackerClientEvents() {}

   @SubscribeEvent
   public static void registerModels(ModelEvent.RegisterAdditional event) {
      event.register(TrackerItemRenderer.BODY);
      event.register(TrackerItemRenderer.POINTER);
   }

   @SubscribeEvent
   public static void registerExtensions(RegisterClientExtensionsEvent event) {
      event.registerItem(new IClientItemExtensions() {
         private final TrackerItemRenderer renderer = new TrackerItemRenderer();
         @Override
         public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
      }, EngineerToolsModule.TRACKER.get());
   }
}
