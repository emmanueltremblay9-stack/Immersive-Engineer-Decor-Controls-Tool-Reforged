package com.oblixorprime.engineersdecorreforged;

import com.oblixorprime.engineersdecorreforged.stool.StoolSeatEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(
      Registries.ENTITY_TYPE, EngineersDecorReforged.MOD_ID
   );
   public static final DeferredHolder<EntityType<?>, EntityType<StoolSeatEntity>> STOOL_SEAT = ENTITY_TYPES.register(
      "stool_seat", () -> EntityType.Builder.<StoolSeatEntity>of(StoolSeatEntity::new, MobCategory.MISC)
         .sized(0.01F, 0.01F).noSave().noSummon().fireImmune().clientTrackingRange(10).updateInterval(20)
         .build(EngineersDecorReforged.MOD_ID + ":stool_seat")
   );

   private ModEntities() {
   }
}
