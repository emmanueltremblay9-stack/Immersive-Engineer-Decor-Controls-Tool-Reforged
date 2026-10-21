package com.oblixorprime.engineersdecorreforged.stool;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Observes real vanilla cake consumption without replacing or replaying its interaction. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID)
public final class CakeMealEvents {
   // Vanilla CakeBlock.eat calls FoodData.eat(2, 0.1F): 2 nutrition * 0.1 modifier * 2 = 0.4.
   private static final float SLICE_SATURATION = 0.4F;
   private static final Map<ServerPlayer, SliceAttempt> ATTEMPTS = new WeakHashMap<>();

   private CakeMealEvents() {
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onCakeInteraction(PlayerInteractEvent.RightClickBlock event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) {
         return;
      }
      settleAt(player.serverLevel(), event.getPos());
      settle(player, ATTEMPTS.remove(player));
      BlockState state = event.getLevel().getBlockState(event.getPos());
      if (event.getUseBlock() == TriState.FALSE || !isVanillaCake(state) || player.isSpectator()
         || !player.isAlive() || !event.getLevel().mayInteract(player, event.getPos())
         || !player.canInteractWithBlock(event.getPos(), 0.0)) {
         return;
      }
      ATTEMPTS.put(player, new SliceAttempt(player.serverLevel(), event.getPos().immutable(),
         state, cakeSlices(player), player.level().getGameTime()));
   }

   @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
   public static void onNativeEat(VanillaGameEvent event) {
      // Cancelling this event suppresses vibration propagation, not CakeBlock's already-applied food.
      if (event.getVanillaEvent() != GameEvent.EAT || !(event.getCause() instanceof ServerPlayer player)) {
         return;
      }
      SliceAttempt attempt = ATTEMPTS.get(player);
      if (attempt == null || attempt.witnessed || event.getLevel() != attempt.level
         || !BlockPos.containing(event.getEventPosition()).equals(attempt.pos)
         || cakeSlices(player) != attempt.statBefore + 1) {
         return;
      }
      BlockState state = attempt.level.getBlockState(attempt.pos);
      if (!state.equals(attempt.stateBefore)) {
         return;
      }
      // At this event vanilla has updated the stat and FoodData, but has NOT changed BITES yet.
      attempt.witnessed = true;
      attempt.rate = DiningSaturation.cakeRate(player, attempt.pos);
   }

   @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
   public static void onCakeStateChanged(BlockEvent.NeighborNotifyEvent event) {
      if (event.getLevel() instanceof ServerLevel level) {
         // Cake's native flag-3 block update reaches here after BITES changes (or the last slice disappears).
         settleAt(level, event.getPos());
      }
   }

   @SubscribeEvent
   public static void afterServerTick(ServerTickEvent.Post event) {
      // Bounded fallback if another mod suppresses normal neighbor notifications. Denied/failed attempts expire.
      Iterator<Map.Entry<ServerPlayer, SliceAttempt>> iterator = ATTEMPTS.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<ServerPlayer, SliceAttempt> entry = iterator.next();
         if (entry.getValue().level.getServer() == event.getServer()) {
            iterator.remove();
            settle(entry.getKey(), entry.getValue());
         }
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
      ATTEMPTS.remove(event.getEntity());
   }

   private static void settleAt(ServerLevel level, BlockPos pos) {
      Iterator<Map.Entry<ServerPlayer, SliceAttempt>> iterator = ATTEMPTS.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<ServerPlayer, SliceAttempt> entry = iterator.next();
         SliceAttempt attempt = entry.getValue();
         if (attempt.level == level && attempt.pos.equals(pos) && attempt.witnessed && biteChanged(attempt)) {
            // Consume the receipt BEFORE updating food data, including reentrant/duplicated notifications.
            iterator.remove();
            settle(entry.getKey(), attempt);
         }
      }
   }

   private static void settle(ServerPlayer player, SliceAttempt attempt) {
      if (attempt == null || !attempt.witnessed || player.isRemoved() || !player.isAlive()
         || player.level() != attempt.level
         || attempt.level.getGameTime() > attempt.gameTime + 1 || !biteChanged(attempt)) {
         return;
      }
      DiningSaturation.add(player, SLICE_SATURATION, attempt.rate);
   }

   private static boolean biteChanged(SliceAttempt attempt) {
      BlockState current = attempt.level.getBlockState(attempt.pos);
      return attempt.bitesBefore == CakeBlock.MAX_BITES ? current.isAir()
         : current.is(Blocks.CAKE) && current.getValue(CakeBlock.BITES) == attempt.bitesBefore + 1;
   }

   private static int cakeSlices(ServerPlayer player) {
      return player.getStats().getValue(Stats.CUSTOM.get(Stats.EAT_CAKE_SLICE));
   }

   private static boolean isVanillaCake(BlockState state) {
      return state.is(Blocks.CAKE) || state.getBlock().getClass() == CandleCakeBlock.class
         && BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("minecraft");
   }

   private static final class SliceAttempt {
      private final ServerLevel level;
      private final BlockPos pos;
      private final BlockState stateBefore;
      private final int bitesBefore;
      private final int statBefore;
      private final long gameTime;
      private boolean witnessed;
      private float rate;

      private SliceAttempt(ServerLevel level, BlockPos pos, BlockState stateBefore, int statBefore, long gameTime) {
         this.level = level;
         this.pos = pos;
         this.stateBefore = stateBefore;
         // Vanilla candle-cakes delegate their first slice to CakeBlock.eat with an unbitten plain cake.
         this.bitesBefore = stateBefore.is(Blocks.CAKE) ? stateBefore.getValue(CakeBlock.BITES) : 0;
         this.statBefore = statBefore;
         this.gameTime = gameTime;
      }
   }
}
