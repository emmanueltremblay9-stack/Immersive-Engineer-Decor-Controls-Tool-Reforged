package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.RailingMining;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Restarts native mining when the selected component changes within the same block. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class RailingMiningClientEvents {
   private RailingMiningClientEvents() {
   }

   @SubscribeEvent
   public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
      RailingMining.clearClientIntents();
   }

   @SubscribeEvent
   public static void clonePlayer(ClientPlayerNetworkEvent.Clone event) {
      RailingMining.clear(event.getOldPlayer());
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void continueMining(PlayerInteractEvent.LeftClickBlock event) {
      if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD
         || event.isCanceled() || event.getUseItem() == TriState.FALSE) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.player != event.getEntity()
         || minecraft.gameMode == null || event.getFace() == null) {
         return;
      }

      BlockPos pos = event.getPos();
      // RailingBlock extends CatwalkBlock, so this covers both kinds without other blocks.
      if (!(minecraft.level.getBlockState(pos).getBlock() instanceof PortedBlocks.CatwalkBlock)
         || RailingMining.matchesCurrentTarget(minecraft.player, pos)) {
         return;
      }

      // CLIENT_HOLD arrives after this tick's progress increment, before native completion.
      // A fresh START resets that progress and records the newly aimed component normally.
      minecraft.gameMode.stopDestroyBlock();
      minecraft.gameMode.startDestroyBlock(pos, event.getFace());
      event.setUseItem(TriState.FALSE);
   }
}
