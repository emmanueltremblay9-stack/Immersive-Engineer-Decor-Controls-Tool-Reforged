package com.oblixorprime.engineersdecorreforged.stool;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID)
public final class StoolMealEvents {
   // A native use can finish only once. Weak keys cannot retain players after leaving the server.
   private static final Set<Player> ACTIVE_USES = Collections.newSetFromMap(new WeakHashMap<>());

   private StoolMealEvents() {
   }

   @SubscribeEvent
   public static void onUseStart(LivingEntityUseItemEvent.Start event) {
      if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
         ACTIVE_USES.add(player);
      }
   }

   @SubscribeEvent
   public static void onUseStop(LivingEntityUseItemEvent.Stop event) {
      if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
         ACTIVE_USES.remove(player);
      }
   }

   @SubscribeEvent
   public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
      if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !ACTIVE_USES.remove(player)
         || !player.isUsingItem() || !(player.getVehicle() instanceof StoolSeatEntity seat) || !seat.isValidSeatFor(player)) {
         return;
      }
      // Finish runs AFTER vanilla finishUsingItem; getItem is the pre-consumption copy, even for bowls/last items.
      FoodProperties food = event.getItem().getFoodProperties(player);
      if (food == null || !Float.isFinite(food.saturation()) || food.saturation() <= 0.0F) {
         return;
      }
      FoodData data = player.getFoodData();
      // In 1.21.1 saturation() already equals nutrition * saturationModifier * 2.
      data.setSaturation(Math.min(data.getFoodLevel(), data.getSaturationLevel() + food.saturation() * 0.20F));
   }

   @SubscribeEvent
   public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
      Player player = event.getEntity();
      ACTIVE_USES.remove(player);
      if (!player.level().isClientSide && player.getVehicle() instanceof StoolSeatEntity seat) {
         seat.discard();
      }
   }
}
