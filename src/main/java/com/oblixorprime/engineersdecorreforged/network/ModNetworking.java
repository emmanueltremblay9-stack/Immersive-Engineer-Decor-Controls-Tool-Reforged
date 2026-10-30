package com.oblixorprime.engineersdecorreforged.network;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
   private static final long LABELED_CRATE_EDIT_WINDOW_TICKS = 2400L;
   private static final Map<UUID, LabeledCrateEditAuthorization> LABELED_CRATE_EDIT_AUTHORIZATIONS = new ConcurrentHashMap<>();
   public static final int HOPPER_RANGE = 1;
   public static final int HOPPER_TRANSFER_COUNT = 2;
   public static final int HOPPER_LOGIC = 3;
   public static final int HOPPER_PERIOD = 4;
   public static final int HOPPER_MANUAL_TRIGGER = 5;
   public static final int DROPPER_SPEED = 10;
   public static final int DROPPER_ANGLE = 11;
   public static final int DROPPER_COUNT = 12;
   public static final int DROPPER_PERIOD = 13;
   public static final int DROPPER_LOGIC = 14;
   public static final int DROPPER_MANUAL_REDSTONE = 15;
   public static final int DROPPER_MANUAL_TRIGGER = 16;
   public static final int PLACER_LOGIC = 20;
   public static final int PLACER_MANUAL_TRIGGER = 21;
   public static final int PLACER_STOCK_ROW = 22;
   public static final int ELECTRICAL_FURNACE_SPEED = 30;

   private ModNetworking() {
   }

   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("1");
      registrar.playToServer(MachineActionPayload.TYPE, MachineActionPayload.STREAM_CODEC, ModNetworking::handleMachineAction);
      registrar.playToServer(LabeledCrateLabelPayload.TYPE, LabeledCrateLabelPayload.STREAM_CODEC, ModNetworking::handleLabeledCrateLabel);
   }

   private static void handleMachineAction(MachineActionPayload payload, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.containerMenu instanceof MachineMenu menu && menu.containerId == payload.containerId()) {
               menu.handleAction(serverPlayer, payload.action(), payload.valueA(), payload.valueB());
            }
         }
      });
   }

   private static void handleLabeledCrateLabel(LabeledCrateLabelPayload payload, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer serverPlayer) {
            if (!consumeLabeledCrateEditAuthorization(serverPlayer, payload.pos())) {
               return;
            }
            if (!(serverPlayer.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 64.0)) {
               if (serverPlayer.serverLevel().isLoaded(payload.pos())) {
                  if (serverPlayer.serverLevel().getBlockState(payload.pos()).is((Block)ModBlocks.LABELED_CRATE.get())) {
                     if (serverPlayer.serverLevel().getBlockEntity(payload.pos()) instanceof MachineBlockEntity machine) {
                        machine.setLabeledCrateLabel(payload.line0(), payload.line1());
                     }
                  }
               }
            }
         }
      });
   }

   public static void authorizeLabeledCrateEdit(ServerPlayer player, BlockPos pos) {
      LABELED_CRATE_EDIT_AUTHORIZATIONS.put(
         player.getUUID(),
         new LabeledCrateEditAuthorization(pos.immutable(), player.serverLevel().dimension(), player.serverLevel().getGameTime() + LABELED_CRATE_EDIT_WINDOW_TICKS)
      );
   }

   public static boolean consumeLabeledCrateEditAuthorization(ServerPlayer player, BlockPos pos) {
      return consumeLabeledCrateEditAuthorization(player, pos, player.serverLevel().getGameTime());
   }

   public static boolean consumeLabeledCrateEditAuthorization(ServerPlayer player, BlockPos pos, long currentGameTime) {
      LabeledCrateEditAuthorization authorization = LABELED_CRATE_EDIT_AUTHORIZATIONS.get(player.getUUID());
      if (authorization == null) {
         return false;
      }
      if (currentGameTime > authorization.expiresAt()) {
         LABELED_CRATE_EDIT_AUTHORIZATIONS.remove(player.getUUID(), authorization);
         return false;
      }
      if (!authorization.pos().equals(pos) || !authorization.dimension().equals(player.serverLevel().dimension())) {
         return false;
      }
      return LABELED_CRATE_EDIT_AUTHORIZATIONS.remove(player.getUUID(), authorization);
   }

   public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
      LABELED_CRATE_EDIT_AUTHORIZATIONS.remove(event.getEntity().getUUID());
   }

   public static void onServerStopped(ServerStoppedEvent event) {
      LABELED_CRATE_EDIT_AUTHORIZATIONS.clear();
   }

   private record LabeledCrateEditAuthorization(BlockPos pos, ResourceKey<Level> dimension, long expiresAt) {
   }
}
