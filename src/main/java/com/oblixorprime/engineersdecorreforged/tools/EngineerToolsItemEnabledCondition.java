package com.oblixorprime.engineersdecorreforged.tools;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public record EngineerToolsItemEnabledCondition(ResourceLocation item) implements ICondition {
   public static final MapCodec<EngineerToolsItemEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(ResourceLocation.CODEC.fieldOf("item").forGetter(EngineerToolsItemEnabledCondition::item))
         .apply(instance, EngineerToolsItemEnabledCondition::new)
   );
   private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister.create(
      NeoForgeRegistries.Keys.CONDITION_CODECS, EngineersDecorReforged.MOD_ID
   );
   @SuppressWarnings("unused")
   private static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<EngineerToolsItemEnabledCondition>> ITEM_ENABLED = CONDITION_CODECS.register(
      "engineer_tools_item_enabled", () -> CODEC
   );

   public static void register(IEventBus eventBus) {
      CONDITION_CODECS.register(eventBus);
   }

   @Override
   public boolean test(IContext context) {
      return EngineerToolsConfig.isItemEnabled(this.item.getPath());
   }

   @Override
   public MapCodec<? extends ICondition> codec() {
      return CODEC;
   }
}
