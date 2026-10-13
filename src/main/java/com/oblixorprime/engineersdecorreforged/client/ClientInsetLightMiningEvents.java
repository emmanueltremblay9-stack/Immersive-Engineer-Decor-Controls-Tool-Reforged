package com.oblixorprime.engineersdecorreforged.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.block.RailingMining;
import com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/** A completed lamp removal needs release or deliberate retargeting before mining the deck. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class ClientInsetLightMiningEvents {
   private static final InsetLightMiningHold HOLD = new InsetLightMiningHold();

   private ClientInsetLightMiningEvents() {}

   @SubscribeEvent
   public static void removed(RailingMining.InsetLightRemoved event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == event.player() && minecraft.level == event.player().level()) {
         HOLD.completed(minecraft.level, minecraft.player, event.pos().asLong());
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
      if (event.isAttack() && suppressHeldAttack()) {
         // LeftClickBlock cancellation alone still sends a native START packet.
         // This input event precedes gameMode, preventing both prediction and that packet.
         event.setCanceled(true);
         event.setSwingHand(false);
      }
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Pre event) {
      suppressHeldAttack();
   }

   @SubscribeEvent
   public static void mouseReleased(InputEvent.MouseButton.Post event) {
      if (event.getAction() == InputConstants.RELEASE
         && Minecraft.getInstance().options.keyAttack.matchesMouse(event.getButton())) {
         HOLD.reset();
      }
   }

   @SubscribeEvent
   public static void keyReleased(InputEvent.Key event) {
      if (event.getAction() == InputConstants.RELEASE
         && Minecraft.getInstance().options.keyAttack.matches(event.getKey(), event.getScanCode())) {
         HOLD.reset();
      }
   }

   @SubscribeEvent
   public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
      HOLD.reset();
   }

   @SubscribeEvent
   public static void clonePlayer(ClientPlayerNetworkEvent.Clone event) {
      HOLD.reset();
   }

   private static boolean suppressHeldAttack() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.player == null) {
         HOLD.reset();
         return false;
      }
      Long target = null;
      boolean retainedOwner = false;
      if (minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
         target = hit.getBlockPos().asLong();
         BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
         // An early STOP can be acknowledged while the server's delayed break is
         // still pending. Its correction temporarily restores the lamp; retain
         // the latch so the later server-only removal cannot expose the deck to
         // the original held attack. A denied attempt can be retried after release.
         retainedOwner = state.getBlock() instanceof RaisedCatwalkBlock;
      }
      return HOLD.suppress(minecraft.level, minecraft.player, minecraft.options.keyAttack.isDown(), target, retainedOwner);
   }
}
