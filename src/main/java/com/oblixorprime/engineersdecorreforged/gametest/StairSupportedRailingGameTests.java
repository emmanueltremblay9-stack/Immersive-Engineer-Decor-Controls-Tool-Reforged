package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Existing standalone Steel Railing blocks on/next to stairs, as in the reported tooltip. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class StairSupportedRailingGameTests {
   private static final BlockPos RAIL = new BlockPos(3, 2, 3);
   private static final Direction[] EDGES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

   private StairSupportedRailingGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void existing_standalone_rail_on_stairs_mines_each_side_without_touching_stair(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction facing : EDGES) {
         for (Direction side : EDGES) {
            BlockState stair = stairs(facing);
            put(helper, RAIL.below(), stair);
            put(helper, RAIL, fullRail());
            clearDrops(helper);
            aim(helper, player, side, false);
            mine(helper, player);
            assertRemoved(helper, side);
            helper.assertValueEqual(helper.getBlockState(RAIL.below()), stair, "mining a standalone railing on a stair must preserve its complete supporting stair");
            helper.assertValueEqual(drops(helper), 1, "one paid standalone side must return exactly one rail");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void close_oblique_standalone_rail_aim_beside_stairs_selects_only_visible_side(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction facing : EDGES) {
         for (Direction side : EDGES) {
            put(helper, RAIL.below(), stairs(facing));
            put(helper, RAIL, fullRail());
            // A side stair may expose its automatic decorative connection to the railing.
            BlockPos neighbor = RAIL.relative(side.getOpposite());
            put(helper, neighbor, stairs(facing));
            
            aim(helper, player, side, true);
            mine(helper, player);
            assertRemoved(helper, side);
            helper.assertTrue(helper.getBlockState(neighbor).is(ModBlocks.STEEL_CATWALK_STAIRS.get()), "mining the distinct railing must not destroy the neighboring stair");
            helper.assertValueEqual(helper.getBlockState(neighbor).getValue(PortedBlocks.HORIZONTAL_FACING), facing, "neighbor refresh must preserve the stair orientation");
            put(helper, neighbor, Blocks.AIR.defaultBlockState());
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stair_visible_through_standalone_center_is_the_actual_mining_target(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, RAIL.below(), stairs(Direction.NORTH));
      BlockState rail = fullRail();
      put(helper, RAIL, rail);
      BlockPos pos = helper.absolutePos(RAIL);
      look(player, Vec3.atCenterOf(pos).add(0, 1.5, 0), Vec3.atCenterOf(pos.below()));
      BlockHitResult hit = eyeHit(helper, player);
      helper.assertValueEqual(hit.getBlockPos(), pos.below(), "the standalone open center must expose the supporting stair");
      start(helper, player, hit);
      player.gameMode.destroyBlock(hit.getBlockPos());
      helper.assertTrue(helper.getBlockState(RAIL.below()).isAir(), "a real stair-surface hit mines the stair normally");
      helper.assertValueEqual(helper.getBlockState(RAIL), rail, "a stair hit must never be redirected into the separate standalone railing");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 400)
   public static void stair_supported_standalone_railing_completes_normal_start_stop_mining(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, RAIL.below(), stairs(Direction.WEST));
      put(helper, RAIL, fullRail());
      aim(helper, player, Direction.EAST, true);
      BlockHitResult hit = eyeHit(helper, player);
      BlockState before = helper.getBlockState(RAIL);
      float progress = before.getDestroyProgress(player, helper.getLevel(), hit.getBlockPos());
      helper.assertTrue(progress > 0 && progress < 1, "normal steel mining requires multiple ticks");
      int ticks = (int)Math.ceil(1.0F / progress) + 2;
      helper.assertTrue(ticks < 350, "native mining must fit the test timeout");
      start(helper, player, hit);
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
      helper.assertValueEqual(helper.getBlockState(RAIL), before, "early STOP must wait for native mining progress");
      tick(helper, player, ticks, () -> {
         assertRemoved(helper, Direction.EAST);
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "completed mining wears the pickaxe only once");
         helper.assertValueEqual(drops(helper), 1, "completed mining returns one paid rail");
         helper.assertValueEqual(helper.getBlockState(RAIL.below()), stairs(Direction.WEST), "normal mining preserves the supporting stair");
         helper.succeed();
      });
   }

   private static ServerPlayer player(GameTestHelper helper) {
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "StairRailTest"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      player.setOnGround(true);
      clearDrops(helper);
      return player;
   }

   private static BlockState stairs(Direction facing) {
      return ModBlocks.STEEL_CATWALK_STAIRS.get().defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, facing);
   }

   private static BlockState fullRail() {
      return ModBlocks.STEEL_RAILING.get().defaultBlockState().setValue(PortedBlocks.NORTH, true)
         .setValue(PortedBlocks.EAST, true).setValue(PortedBlocks.SOUTH, true).setValue(PortedBlocks.WEST, true)
         .setValue(PortedBlocks.PAID_RAILINGS, 3);
   }

   private static BooleanProperty sideProperty(Direction side) {
      return switch (side) {
         case NORTH -> PortedBlocks.NORTH;
         case EAST -> PortedBlocks.EAST;
         case SOUTH -> PortedBlocks.SOUTH;
         case WEST -> PortedBlocks.WEST;
         default -> throw new IllegalArgumentException("Horizontal side required");
      };
   }

   private static void assertRemoved(GameTestHelper helper, Direction removed) {
      BlockState state = helper.getBlockState(RAIL);
      helper.assertTrue(state.is(ModBlocks.STEEL_RAILING.get()), "partial mining must keep the standalone railing block");
      for (Direction side : EDGES) {
         helper.assertValueEqual(state.getValue(sideProperty(side)), side != removed, "only the aimed standalone side can disappear: " + side);
      }
   }

   private static void put(GameTestHelper helper, BlockPos pos, BlockState state) {
      helper.getLevel().setBlock(helper.absolutePos(pos), state, 2);
   }

   private static void aim(GameTestHelper helper, ServerPlayer player, Direction side, boolean oblique) {
      Vec3 center = Vec3.atCenterOf(helper.absolutePos(RAIL));
      Vec3 target = center.add(side.getStepX() * 0.45, 0.25, side.getStepZ() * 0.45);
      Vec3 eye = oblique ? target.add(side.getStepX() * 0.65, 0.55, side.getStepZ() * 0.65)
         : target.add(0, 1.75, 0);
      look(player, eye, target);
      helper.assertValueEqual(eyeHit(helper, player).getBlockPos(), helper.absolutePos(RAIL), "the actual eye ray must target the standalone rail rather than its supporting stair");
   }

   private static void look(ServerPlayer player, Vec3 eye, Vec3 target) {
      player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
      Vec3 delta = target.subtract(eye);
      player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
      player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
   }

   private static BlockHitResult eyeHit(GameTestHelper helper, ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      return helper.getLevel().clip(new ClipContext(eye, eye.add(player.getViewVector(1).scale(player.blockInteractionRange())),
         ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
   }

   private static void start(GameTestHelper helper, ServerPlayer player, BlockHitResult hit) {
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
   }

   private static void action(GameTestHelper helper, ServerPlayer player, BlockHitResult hit, ServerboundPlayerActionPacket.Action action) {
      player.gameMode.handleBlockBreakAction(hit.getBlockPos(), action, hit.getDirection(), helper.getLevel().getMaxBuildHeight(), 0);
   }

   private static void mine(GameTestHelper helper, ServerPlayer player) {
      BlockHitResult hit = eyeHit(helper, player);
      helper.assertValueEqual(hit.getType(), HitResult.Type.BLOCK, "mining requires a real block hit");
      BlockState before = helper.getBlockState(RAIL);
      start(helper, player, hit);
      helper.assertValueEqual(helper.getBlockState(RAIL), before, "survival START must not instantly remove a rail");
      player.gameMode.destroyBlock(hit.getBlockPos());
   }

   private static int drops(GameTestHelper helper) {
      return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(RAIL)).inflate(1.5),
         entity -> entity.getItem().is(ModBlocks.STEEL_RAILING.get().asItem())).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
   }

   private static void clearDrops(GameTestHelper helper) {
      helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(RAIL)).inflate(1.5)).forEach(ItemEntity::discard);
   }

   private static void tick(GameTestHelper helper, ServerPlayer player, int remaining, Runnable complete) {
      if (remaining == 0) {
         complete.run();
      } else {
         helper.runAfterDelay(1, () -> {
            player.gameMode.tick();
            tick(helper, player, remaining - 1, complete);
         });
      }
   }
}
