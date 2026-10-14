package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.RailingMining;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
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
      if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
         && event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD
         || event.isCanceled() || !event.getEntity().getAbilities().instabuild && event.getUseItem() == TriState.FALSE) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.player != event.getEntity()
         || minecraft.gameMode == null || event.getFace() == null) {
         return;
      }

      BlockPos pos = event.getPos();
      // Use the same block family as server completion and the shared component highlight.
      if (!PortedBlocks.hasSelectableRailings(minecraft.level.getBlockState(pos))) {
         return;
      }

      // Minecraft handles attack input before LocalPlayer sends this tick's rotation.
      // START/STOP mining packets contain only the block and face, so without this
      // ordinary pose update a turn-and-click can mine the deck behind the lamp.
      // Send before native START, or before a HOLD can produce native STOP. The
      // server still validates reach, occlusion, protection and the selected shape.
      minecraft.player.connection.send(new ServerboundMovePlayerPacket.Rot(
         minecraft.player.getViewYRot(1.0F), minecraft.player.getViewXRot(1.0F), minecraft.player.onGround()));
      if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD
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
