package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Records targeting only. Actual removal stays in NeoForge's completed native mining hook. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID)
public final class RailingMining {
   private static final Map<Player, Intent> INTENTS = Collections.synchronizedMap(new WeakHashMap<>());

   private RailingMining() {}

   @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
   public static void miningAction(PlayerInteractEvent.LeftClickBlock event) {
      // Client continuation may nest a fresh START, then suppress only the outer HOLD.
      // That outer event must never erase the newly captured target.
      if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) {
         return;
      }
      Player player = event.getEntity();
      if (event.isCanceled() || event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.ABORT
         || !player.getAbilities().instabuild && event.getUseItem() == TriState.FALSE) {
         INTENTS.remove(player);
         return;
      }
      if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
         return;
      }
      INTENTS.remove(player);
      Level level = player.level();
      BlockPos pos = event.getPos();
      BlockState state = level.getBlockState(pos);
      if (!PortedBlocks.hasSelectableRailings(state) || !canMine(player, level, pos)) {
         return;
      }
      PortedBlocks.RailingPart part = aimedPart(player, level, pos);
      if (part != null) {
         INTENTS.put(player, new Intent(level, pos.immutable(), state, part, player.getAbilities().instabuild,
            player.getMainHandItem().copy(), event, null));
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
   public static void beforeBreak(BlockEvent.BreakEvent event) {
      Player player = event.getPlayer();
      Intent intent = INTENTS.get(player);
      if (event.isCanceled()) {
         INTENTS.remove(player);
      } else if (PortedBlocks.hasSelectableRailings(event.getState())
         || intent != null && intent.pos().equals(event.getPos())) {
         if (intent == null || intent.state() != event.getState() || !matchesCurrentTarget(player, event.getPos())) {
            // Also protect a different block that replaced the originally targeted railing.
            // Its own onDestroyedByPlayer hook cannot know about our stale start target.
            event.setCanceled(true);
            INTENTS.remove(player);
            return;
         }
         // The normal mining callback can break the tool before onDestroyedByPlayer runs.
         INTENTS.put(player, new Intent(intent.level(), intent.pos(), intent.state(), intent.part(), intent.creative(),
            player.getMainHandItem().copy(), null, event));
      }
   }

   @SubscribeEvent
   public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
      INTENTS.remove(event.getEntity());
   }

   @SubscribeEvent
   public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
      INTENTS.remove(event.getEntity());
   }

   public static void clear(Player player) {
      INTENTS.remove(player);
   }

   public static void clearClientIntents() {
      synchronized (INTENTS) {
         INTENTS.keySet().removeIf(player -> player.level().isClientSide);
      }
   }

   @SubscribeEvent
   public static void clonePlayer(PlayerEvent.Clone event) {
      INTENTS.remove(event.getOriginal());
      INTENTS.remove(event.getEntity());
   }

   @Nullable
   static Completion complete(Player player, Level level, BlockPos pos, BlockState state) {
      Intent intent = INTENTS.remove(player);
      if (intent == null || intent.level() != level || !intent.pos().equals(pos) || intent.state() != state
         || level.getBlockState(pos) != state || intent.creative() != player.getAbilities().instabuild
         || !startAllowed(intent) || !canMine(player, level, pos)) {
         return null;
      }
      PortedBlocks.RailingPart current = aimedPart(player, level, pos);
      return current != null && current.sameComponent(intent.part()) ? new Completion(current, intent.tool()) : null;
   }

   /** Lets the client restart ordinary mining progress when the aimed component changes. */
   public static boolean matchesCurrentTarget(Player player, BlockPos pos) {
      Intent intent = INTENTS.get(player);
      Level level = player.level();
      if (intent == null || intent.level() != level || !intent.pos().equals(pos) || level.getBlockState(pos) != intent.state()
         || intent.creative() != player.getAbilities().instabuild || !startAllowed(intent) || !canMine(player, level, pos)
         // Match native client tool-reset policy (including mending/modded exceptions).
         // After BreakEvent, native durability use may legitimately change or destroy it.
         || intent.start() != null && intent.tool().shouldCauseBlockBreakReset(player.getMainHandItem())) {
         return false;
      }
      PortedBlocks.RailingPart current = aimedPart(player, level, pos);
      return current != null && current.sameComponent(intent.part());
   }

   private static boolean canMine(Player player, Level level, BlockPos pos) {
      // Native game mode/BreakEvent owns adventure CAN_BREAK and tool authorization.
      return player.level() == level && !player.isSpectator()
         && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
         && level.mayInteract(player, pos) && player.canInteractWithBlock(pos, 1.0);
   }

   private static boolean startAllowed(Intent intent) {
      PlayerInteractEvent.LeftClickBlock start = intent.start();
      return (start == null || !start.isCanceled() && (intent.creative() || start.getUseItem() != TriState.FALSE))
         && (intent.breakEvent() == null || !intent.breakEvent().isCanceled());
   }

   /** Notify client input only after its lamp-only prediction actually changed the block. */
   public static void clientInsetLightRemoved(Player player, BlockPos pos) {
      if (player.level().isClientSide) {
         NeoForge.EVENT_BUS.post(new InsetLightRemoved(player, pos));
      }
   }

   /** No client classes are referenced from the common mining implementation. */
   public static final class InsetLightRemoved extends Event {
      private final Player player;
      private final BlockPos pos;

      private InsetLightRemoved(Player player, BlockPos pos) {
         this.player = player;
         this.pos = pos.immutable();
      }

      public Player player() { return player; }
      public BlockPos pos() { return pos; }
   }

   @Nullable
   private static PortedBlocks.RailingPart aimedPart(Player player, Level level, BlockPos pos) {
      Vec3 start = player.getEyePosition();
      // LocalPlayer's camera follows its current yaw. ServerPlayer instead inherits
      // LivingEntity.getViewYRot, which reads the render head yaw: movement packets
      // update yRot/xRot before serverAiStep catches yHeadRot up on a later tick.
      // A click in that interval must select the same lamp/rail as the client, not
      // the deck intersected by the stale head pose. Keep the client's camera rules
      // (including riding), but use the accepted packet rotation on the server.
      Vec3 direction = level.isClientSide ? player.getViewVector(1.0F) : player.getLookAngle();
      Vec3 end = start.add(direction.scale(player.blockInteractionRange()));
      return PortedBlocks.selectRailingPart(level, pos, start, end, player);
   }

   record Completion(PortedBlocks.RailingPart part, ItemStack tool) {}

   private record Intent(Level level, BlockPos pos, BlockState state, PortedBlocks.RailingPart part, boolean creative,
                         ItemStack tool, @Nullable PlayerInteractEvent.LeftClickBlock start,
                         @Nullable BlockEvent.BreakEvent breakEvent) {}
}
