package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercises real PlayerList sound recipient selection; only outgoing transport is captured. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class SlidingDoorSoundGameTests {
   private static final BlockPos FIRST = new BlockPos(3, 1, 3);

   private SlidingDoorSoundGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void manual_sliding_door_sound_reaches_actor_and_observer_once(GameTestHelper helper) {
      Fixture fixture = new Fixture(helper);
      try {
         fixture.addPlayers();
         fixture.assertNativeExclusionControl();
         for (boolean paired : List.of(false, true)) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
               for (int half = 0; half < 2; half++) {
                  fixture.place(facing, paired);
                  BlockPos clicked = paired ? FIRST.relative(facing.getClockWise()) : FIRST;
                  fixture.clearPackets();
                  helper.useBlock(clicked.above(half), fixture.actor);
                  fixture.assertState(true, facing, paired);
                  fixture.assertSound(SoundEvents.IRON_DOOR_OPEN, 1, "manual open");

                  fixture.clearPackets();
                  helper.useBlock(FIRST.above(1 - half), fixture.actor);
                  fixture.assertState(false, facing, paired);
                  fixture.assertSound(SoundEvents.IRON_DOOR_CLOSE, 1, "manual close");
               }
            }
         }
         helper.succeed();
      } finally {
         fixture.cleanup();
      }
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void powered_sliding_door_sound_is_single_and_noop_click_is_silent(GameTestHelper helper) {
      Fixture fixture = new Fixture(helper);
      try {
         fixture.addPlayers();
         fixture.place(Direction.NORTH, true);
         BlockPos power = FIRST.south();
         fixture.clearPackets();
         helper.setBlock(power, Blocks.REDSTONE_BLOCK);
         fixture.assertState(true, Direction.NORTH, true);
         fixture.assertSound(SoundEvents.IRON_DOOR_OPEN, 1, "redstone open");

         fixture.clearPackets();
         helper.useBlock(FIRST.above(), fixture.actor);
         fixture.assertState(true, Direction.NORTH, true);
         fixture.assertSound(SoundEvents.IRON_DOOR_OPEN, 0, "powered no-op click");

         fixture.clearPackets();
         helper.setBlock(power, Blocks.AIR);
         fixture.assertState(false, Direction.NORTH, true);
         fixture.assertSound(SoundEvents.IRON_DOOR_CLOSE, 1, "redstone close");
         helper.succeed();
      } finally {
         fixture.cleanup();
      }
   }

   private static final class Fixture {
      private final GameTestHelper helper;
      private final List<ServerPlayer> players = new ArrayList<>();
      private final List<SoundListener> listeners = new ArrayList<>();
      private ServerPlayer actor;

      private Fixture(GameTestHelper helper) {
         this.helper = helper;
      }

      private void addPlayers() {
         for (int index = 0; index < 2; index++) {
            // Install capture before IE's login sync: the embedded test transport has no
            // negotiated client channels. Registration and sound routing remain native.
            Consumer<PlayerEvent.PlayerLoggedInEvent> captureLogin = event -> {
               if (event.getEntity() instanceof ServerPlayer joining
                  && joining.serverLevel() == helper.getLevel()
                  && joining.getGameProfile().getName().equals("test-mock-player")) {
                  players.add(joining);
                  SoundListener listener = new SoundListener(helper, joining);
                  joining.connection = listener;
                  listeners.add(listener);
               }
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerEvent.PlayerLoggedInEvent.class, captureLogin);
            ServerPlayer player;
            try {
               player = helper.makeMockServerPlayerInLevel();
            } finally {
               NeoForge.EVENT_BUS.unregister(captureLogin);
            }
            helper.assertTrue(players.contains(player), "native login must install packet capture");
            BlockPos origin = helper.absolutePos(FIRST);
            player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 2.5 + index);
            if (index == 0) actor = player;
         }
      }

      private void place(Direction facing, boolean paired) {
         for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
               for (int y = 1; y <= 2; y++) {
                  helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState(), 2);
               }
               helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 2);
            }
         }
         putDoor(FIRST, facing, paired ? PortedBlocks.SlidingDoorPairSide.LEFT : PortedBlocks.SlidingDoorPairSide.SINGLE);
         if (paired) putDoor(FIRST.relative(facing.getClockWise()), facing, PortedBlocks.SlidingDoorPairSide.RIGHT);
      }

      private void putDoor(BlockPos lower, Direction facing, PortedBlocks.SlidingDoorPairSide side) {
         BlockState state = ModBlocks.METAL_SLIDING_DOOR.get().defaultBlockState()
            .setValue(DoorBlock.FACING, facing)
            .setValue(DoorBlock.HINGE, side == PortedBlocks.SlidingDoorPairSide.RIGHT ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT)
            .setValue(PortedBlocks.PAIR_SIDE, side)
            .setValue(DoorBlock.OPEN, false)
            .setValue(DoorBlock.POWERED, false);
         helper.getLevel().setBlock(helper.absolutePos(lower), state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
         helper.getLevel().setBlock(helper.absolutePos(lower.above()), state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
      }

      private void assertState(boolean open, Direction facing, boolean paired) {
         helper.assertBlockProperty(FIRST, DoorBlock.OPEN, open);
         helper.assertBlockProperty(FIRST.above(), DoorBlock.OPEN, open);
         if (paired) {
            BlockPos second = FIRST.relative(facing.getClockWise());
            helper.assertBlockProperty(second, DoorBlock.OPEN, open);
            helper.assertBlockProperty(second.above(), DoorBlock.OPEN, open);
         }
      }

      private void assertNativeExclusionControl() {
         clearPackets();
         helper.getLevel().playSound((Entity)actor, helper.absolutePos(FIRST),
            SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
         // This is the old exclusion behavior as a control, not a run of the old mod.
         assertSoundFor(0, SoundEvents.IRON_DOOR_OPEN, 0, "native excluded actor control");
         assertSoundFor(1, SoundEvents.IRON_DOOR_OPEN, 1, "native observer control");
         clearPackets();
      }

      private void clearPackets() {
         listeners.forEach(listener -> listener.sounds.clear());
      }

      private void assertSound(SoundEvent expected, int count, String phase) {
         for (int index = 0; index < listeners.size(); index++) assertSoundFor(index, expected, count, phase);
      }

      private void assertSoundFor(int index, SoundEvent expected, int count, String phase) {
            BlockPos origin = helper.absolutePos(FIRST);
            List<ClientboundSoundPacket> sounds = listeners.get(index).sounds.stream()
               .filter(packet -> packet.getSource() == SoundSource.BLOCKS
                  && Math.abs(packet.getX() - origin.getX()) <= 2.0
                  && Math.abs(packet.getY() - origin.getY()) <= 1.0
                  && Math.abs(packet.getZ() - origin.getZ()) <= 2.0
                  && (packet.getSound().value() == SoundEvents.IRON_DOOR_OPEN
                     || packet.getSound().value() == SoundEvents.IRON_DOOR_CLOSE))
               .toList();
            helper.assertValueEqual(sounds.size(), count, phase + ": packet count for " + (index == 0 ? "actor" : "observer"));
            for (ClientboundSoundPacket packet : sounds) {
               helper.assertTrue(packet.getSound().value() == expected, phase + ": correct open/close sound");
               helper.assertTrue(packet.getVolume() == 1.0F, phase + ": preserve volume");
            }
      }

      private void cleanup() {
         for (ServerPlayer player : players) helper.getLevel().getServer().getPlayerList().remove(player);
      }
   }

   private static final class SoundListener extends ServerGamePacketListenerImpl {
      private final List<ClientboundSoundPacket> sounds = new ArrayList<>();

      private SoundListener(GameTestHelper helper, ServerPlayer player) {
         super(helper.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), player,
            CommonListenerCookie.createInitial(player.getGameProfile(), false));
      }

      @Override
      public void send(Packet<?> packet) {
         if (packet instanceof ClientboundSoundPacket sound) sounds.add(sound);
      }

      @Override
      public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
         send(packet);
      }
   }
}
