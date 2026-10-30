package com.oblixorprime.engineersdecorreforged;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, EngineersDecorReforged.MOD_ID);
   public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_SIREN = SOUND_EVENTS.register(
      "alarm_siren",
      () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "alarm_siren"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> STIMPACK_INJECT = SOUND_EVENTS.register(
      "stimpack_inject_sound",
      () -> SoundEvent.createVariableRangeEvent(
         ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "stimpack_inject_sound")
      )
   );

   private ModSounds() {
   }
}
