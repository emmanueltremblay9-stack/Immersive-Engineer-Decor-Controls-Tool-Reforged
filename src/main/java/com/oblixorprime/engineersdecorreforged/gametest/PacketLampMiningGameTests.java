package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.RailingMining;
import com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkBlock;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Regression for real movement/action packet order, not a synchronized FakePlayer eye helper. */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PacketLampMiningGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 3, 3);
   private static final Direction[] APPROACHES = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.DOWN};

   private PacketLampMiningGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void packet_lamp_creative_turn_and_start_ignores_stale_head_yaw(GameTestHelper helper) {
      ServerPlayer player = player(helper, GameType.CREATIVE);
      try {
         for (int index = 0; index < APPROACHES.length; index++) {
            Fixture fixture = put(helper, index, true);
            BlockHitResult hit = turnByPacket(helper, player, APPROACHES[index]);
            action(player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
            assertRemoved(helper, player, fixture, 0, 0);
         }
         helper.succeed();
      } finally {
         cleanup(helper, player);
      }
   }

   @GameTest(template = "empty", timeoutTicks = 400)
   public static void packet_lamp_survival_turn_start_stop_preserves_paid_deck_and_supports(GameTestHelper helper) {
      ServerPlayer player = player(helper, GameType.SURVIVAL);
      survivalApproach(helper, player, 0);
   }

   private static void survivalApproach(GameTestHelper helper, ServerPlayer player, int index) {
      try {
         if (index == APPROACHES.length) {
            cleanup(helper, player);
            helper.succeed();
            return;
         }
         Fixture fixture = put(helper, index, true);
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
         BlockHitResult hit = turnByPacket(helper, player, APPROACHES[index]);
         assertSolidSupport(helper, fixture, "before native START");
         float progress = fixture.lit().getDestroyProgress(player, helper.getLevel(), hit.getBlockPos());
         helper.assertTrue(progress > 0 && progress < 1, "fixture must exercise real survival duration");
         action(player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
         action(player, hit, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
         helper.assertValueEqual(helper.getBlockState(CENTER), fixture.lit(), "early native STOP must not instantly mine the lamp");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "no wear before native mining completes");
         int ticks = (int)Math.ceil(1.0 / progress) + 2;
         tickMining(helper, player, fixture, ticks, () -> {
            assertRemoved(helper, player, fixture, 1, 1);
            // A duplicate network STOP cannot consume the deck or refund a second lamp.
            action(player, hit, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
            assertRemoved(helper, player, fixture, 1, 1);
            survivalApproach(helper, player, index + 1);
         });
      } catch (RuntimeException | Error failure) {
         cleanup(helper, player);
         throw failure;
      }
   }

   private static ServerPlayer player(GameTestHelper helper, GameType mode) {
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PacketInsetLamp"));
      // FakePlayer's built-in listener silently ignores both packets. Replace it with
      // the real listener; only outgoing transport is stubbed, not movement or mining.
      player.connection = new MiningPacketListener(helper, player);
      player.gameMode.changeGameModeForPlayer(mode);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      player.setOnGround(true);
      return player;
   }

   private static Fixture put(GameTestHelper helper, int index, boolean paid) {
      BlockPos pos = helper.absolutePos(CENTER);
      clearDrops(helper);
      Block[] supports = {Blocks.AIR, ModBlocks.THICK_STEEL_POLE.get(), ModBlocks.THIN_STEEL_POLE.get(), ModBlocks.TREATED_WOOD_POLE.get(), Blocks.AIR};
      int[] variants = {0, 1, 2, 5, 0};
      BlockState support = supports[index].defaultBlockState();
      // Real top-face placement produces a vertical pole. The default NORTH
      // state is horizontal and non-solid to vanilla fluids, so timed water flow
      // can wash that incorrect fixture away before mining even completes.
      if (support.hasProperty(PortedBlocks.FACING)) support = support.setValue(PortedBlocks.FACING, Direction.UP);
      if (!support.isAir()) helper.assertTrue(support.blocksMotion(), "vertical support must resist ordinary fluid replacement");
      helper.getLevel().setBlock(pos.below(), support, 2);
      BlockState lit = ModBlocks.STEEL_CATWALK_TA.get().defaultBlockState()
         .setValue(RaisedCatwalkBlock.VARIANT, 4).setValue(RaisedCatwalkBlock.PAID_LIGHT, paid)
         .setValue(RaisedCatwalkBlock.WATERLOGGED, true);
      helper.getLevel().setBlock(pos, lit, 2);
      BlockState rail = ModBlocks.STEEL_RAILING.get().defaultBlockState().setValue(PortedBlocks.NORTH, true);
      helper.getLevel().setBlock(pos.above(), rail, 2);
      return new Fixture(lit, helper.getLevel().getBlockState(pos.below()), helper.getLevel().getBlockState(pos.above()), variants[index]);
   }

   private static BlockHitResult turnByPacket(GameTestHelper helper, ServerPlayer player, Direction approach) {
      Vec3 origin = Vec3.atLowerCornerOf(helper.absolutePos(CENTER));
      Vec3 eye = approach == Direction.DOWN ? new Vec3(0.5, 0.1, 0.5)
         : new Vec3(0.5 + approach.getStepX() * 1.7, 0.2, 0.5 + approach.getStepZ() * 1.7);
      Vec3 target = new Vec3(0.5, 14.8 / 16, 0.5);
      Vec3 delta = target.subtract(eye);
      float yaw = (float)Math.toDegrees(Math.atan2(-delta.x, delta.z));
      float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
      player.setPos(origin.x + eye.x, origin.y + eye.y - player.getEyeHeight(), origin.z + eye.z);
      player.setLastSectionPos(SectionPos.of(player));
      // This is the previous tick's pose. Crucially, never synchronize the render
      // head after accepting the native rotation packet, as the older tests did.
      player.setYRot(yaw + 10.0F);
      player.setYHeadRot(yaw + 10.0F);
      player.setXRot(pitch);
      player.connection.resetPosition();
      player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Rot(yaw, pitch, true));
      helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(player.getYRot() - yaw)) < 0.001,
         "real network handler must accept the new aim");
      helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(player.getYHeadRot() - yaw)) > 9.9,
         "real packet processing must retain the stale render head until a later AI tick");
      Vec3 start = player.getEyePosition();
      Vec3 end = start.add(player.getLookAngle().scale(player.blockInteractionRange()));
      BlockHitResult hit = helper.getLevel().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
      helper.assertValueEqual(hit.getType(), HitResult.Type.BLOCK, "native client-equivalent ray must hit geometry");
      helper.assertValueEqual(hit.getBlockPos(), helper.absolutePos(CENTER), "native ray must name the actual catwalk owner");
      PortedBlocks.RailingPart selected = PortedBlocks.selectRailingPart(helper.getLevel(), hit.getBlockPos(), start, end, player);
      helper.assertTrue(selected != null && selected.kind() == PortedBlocks.PartKind.INSET_LIGHT, "current packet aim must hit the inset lamp");
      if (approach != Direction.DOWN) {
         PortedBlocks.RailingPart stale = PortedBlocks.selectRailingPart(helper.getLevel(), hit.getBlockPos(), start,
            start.add(player.getViewVector(1).scale(player.blockInteractionRange())), player);
         helper.assertTrue(stale != null && stale.kind() == PortedBlocks.PartKind.BASE,
            "regression must reproduce stale-head selection of the deck, not merely a rejected miss");
      }
      return hit;
   }

   private static void action(ServerPlayer player, BlockHitResult hit, ServerboundPlayerActionPacket.Action action) {
      MiningPacketListener listener = (MiningPacketListener)player.connection;
      listener.handlePlayerAction(new ServerboundPlayerActionPacket(action, hit.getBlockPos(), hit.getDirection(), ++listener.sequence));
   }

   private static void assertRemoved(GameTestHelper helper, ServerPlayer player, Fixture fixture, int lamps, int wear) {
      BlockPos pos = helper.absolutePos(CENTER);
      BlockState expected = fixture.lit().setValue(RaisedCatwalkBlock.VARIANT, fixture.variant()).setValue(RaisedCatwalkBlock.PAID_LIGHT, false);
      helper.assertValueEqual(helper.getBlockState(CENTER), expected, "native packet mining removes only the lamp, retaining the waterlogged deck");
      BlockState support = helper.getLevel().getBlockState(pos.below());
      if (fixture.support().isAir()) {
         // A waterlogged deck naturally fills the empty cell beneath it during
         // survival's real mining duration. That fluid is not a removed support.
         helper.assertTrue(support.isAir() || support.is(Blocks.WATER), "an unsupported deck may emit water but cannot create a solid support");
      } else {
         helper.assertValueEqual(support, fixture.support(), "native lamp mining must preserve its solid support exactly");
      }
      helper.assertValueEqual(helper.getLevel().getBlockState(pos.above()), fixture.rail(), "native lamp mining must preserve railings");
      helper.assertValueEqual(expected.getLightEmission(helper.getLevel(), pos), 0, "removed lamp must stop emitting");
      BlockState reloaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(expected));
      helper.assertValueEqual(reloaded, expected, "saved native removal must retain water, support and cleared credit");
      helper.assertValueEqual(dropped(helper, ModBlocks.IRON_INSET_LIGHT.get().asItem()), lamps, "paid lamp must be returned exactly once only in survival");
      helper.assertValueEqual(dropped(helper, ModBlocks.STEEL_CATWALK_TA.get().asItem()), 0, "packet mining cannot drop the deck");
      helper.assertValueEqual(player.getMainHandItem().getDamageValue(), wear, "native mining must apply tool wear exactly once only in survival");
   }

   private static void assertSolidSupport(GameTestHelper helper, Fixture fixture, String phase) {
      if (!fixture.support().isAir()) {
         helper.assertValueEqual(helper.getLevel().getBlockState(helper.absolutePos(CENTER).below()), fixture.support(),
            "fixture's solid support must still exist " + phase);
      }
   }

   private static void tickMining(GameTestHelper helper, ServerPlayer player, Fixture fixture, int ticks, Runnable finished) {
      helper.runAfterDelay(1, () -> {
         try {
            assertSolidSupport(helper, fixture, "before native mining progress/completion");
            player.gameMode.tick();
            if (ticks <= 1) finished.run();
            else tickMining(helper, player, fixture, ticks - 1, finished);
         } catch (RuntimeException | Error failure) {
            cleanup(helper, player);
            throw failure;
         }
      });
   }

   private static int dropped(GameTestHelper helper, Item item) {
      return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(1.5),
         entity -> entity.getItem().is(item)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
   }

   private static void clearDrops(GameTestHelper helper) {
      helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(1.5)).forEach(ItemEntity::discard);
   }

   private static void cleanup(GameTestHelper helper, ServerPlayer player) {
      RailingMining.clear(player);
      helper.getLevel().getChunkSource().removeEntity(player);
   }

   private record Fixture(BlockState lit, BlockState support, BlockState rail, int variant) {}

   private static final class MiningPacketListener extends ServerGamePacketListenerImpl {
      private int sequence;

      private MiningPacketListener(GameTestHelper helper, ServerPlayer player) {
         super(helper.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), player,
            CommonListenerCookie.createInitial(player.getGameProfile(), false));
      }

      @Override
      public void send(Packet<?> packet) {}

      @Override
      public void send(Packet<?> packet, @Nullable PacketSendListener listener) {}
   }
}
