package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.StairRailingTargeting;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real rays entirely above the owner cell, followed by ordinary native mining packets. */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UpperStairRailingTargetingGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 1, 3);

   private UpperStairRailingTargetingGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void aligned_landing_caps_remain_selectable_and_paid_in_every_rotation(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean left : new boolean[]{true, false}) {
            for (boolean wet : new boolean[]{false, true}) {
                BlockState state = NativeFluidloggedBlock.withFluidContent(stairs(facing),
                   wet ? NativeFluidloggedBlock.FluidContent.WATER : NativeFluidloggedBlock.FluidContent.EMPTY);
               put(helper, CENTER, state); clearDrops(helper);
               BlockPos pos = helper.absolutePos(CENTER);
               helper.assertValueEqual(state.getShape(helper.getLevel(), pos).bounds().maxY, 2.0,
                  "selection reaches the aligned visible cap at 32/16");
               helper.assertValueEqual(state.getCollisionShape(helper.getLevel(), pos).bounds().maxY, 2.25,
                  "existing 36/16 fence collision is retained");
               Vec3 cap = Vec3.atLowerCornerOf(pos).add(.5, 31.5/16.0, .5)
                  .add(step(facing, .48)).add(step(side(facing, left), .4375));
               look(player, cap.add(step(side(facing, left), 1.0)), cap);
               BlockHitResult hit = upperHit(helper, player, left);
               BlockState before = helper.getBlockState(CENTER);
               action(helper, player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
               helper.assertValueEqual(helper.getBlockState(CENTER), before, "aligned cap START retains ordinary mining duration");
               player.gameMode.destroyBlock(hit.getBlockPos());
               assertRemoved(helper, left);
               helper.assertValueEqual(drops(helper), 1, "new cap targeting refunds exactly the purchased side");
                helper.assertValueEqual(NativeFluidloggedBlock.fluidContent(helper.getBlockState(CENTER)) == NativeFluidloggedBlock.FluidContent.WATER, wet,
                  "cap mining preserves the existing water state");
               put(helper, CENTER, state);
               Vec3 above = cap.add(0, 1.0/16.0, 0);
               look(player, above.add(step(side(facing, left), 1.0)), above);
               HitResult outside = pick(player, vanillaBlock(player));
               helper.assertTrue(!(outside instanceof BlockHitResult block && block.getType()==HitResult.Type.BLOCK
                  && block.getBlockPos().equals(pos)), "no target above the new visible cap");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void horizontal_upper_rail_rays_select_and_mine_both_sides_in_all_rotations(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean left : new boolean[]{true, false}) {
            put(helper, CENTER, stairs(facing));
            clearDrops(helper);
            aim(helper, player, facing, left, 1.0, 1.625);
            BlockHitResult original = vanillaBlock(player);
            helper.assertTrue(original.getType() != HitResult.Type.BLOCK || !original.getBlockPos().equals(helper.absolutePos(CENTER)),
               "regression ray must never enter the owning stair's cell in vanilla DDA");
            BlockHitResult hit = upperHit(helper, player, left);
            BlockState before = helper.getBlockState(CENTER);
            action(helper, player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
            helper.assertValueEqual(helper.getBlockState(CENTER), before, "upper rail START must not bypass native mining duration");
            player.gameMode.destroyBlock(hit.getBlockPos());
            assertRemoved(helper, left);
            helper.assertValueEqual(drops(helper), 1, "upper rail completion returns exactly one paid railing");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void upper_rail_boundary_and_inside_out_rays_use_the_same_visible_side(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean left : new boolean[]{true, false}) {
            put(helper, CENTER, stairs(facing));
            aim(helper, player, facing, left, 1.0, 1.0);
            upperHit(helper, player, left);
            Vec3 target = target(helper, facing, left, 1.625);
            Vec3 inside = target(helper, facing, left, 1.625).subtract(step(side(facing, left), 0.4375));
            look(player, inside, target);
            upperHit(helper, player, left);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void nearer_solid_and_standalone_blocks_occlude_upper_rails_without_redirection(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      BlockPos obstacle = CENTER.west().above();
      for (BlockState blocker : new BlockState[]{Blocks.STONE.defaultBlockState(),
         ModBlocks.STEEL_RAILING.get().defaultBlockState().setValue(PortedBlocks.WEST, true)}) {
         put(helper, CENTER, stairs(Direction.NORTH));
         put(helper, obstacle, blocker);
         aim(helper, player, Direction.NORTH, true, 2.0, 1.625);
         BlockHitResult original = vanillaBlock(player);
         helper.assertValueEqual(original.getBlockPos(), helper.absolutePos(obstacle), "the real ray must encounter the blocker first");
         helper.assertTrue(pick(player, original) == original, "nearer ordinary blocks and standalone rails must keep vanilla's exact result");
         helper.assertTrue(part(helper, player) == null, "server component selection must reject mining through the blocker");
         BlockState before = helper.getBlockState(CENTER);
         BlockHitResult forged = new BlockHitResult(target(helper, Direction.NORTH, true, 1.625), Direction.WEST, helper.absolutePos(CENTER), false);
         action(helper, player, forged, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
         player.gameMode.destroyBlock(forged.getBlockPos());
         helper.assertValueEqual(helper.getBlockState(CENTER), before, "a spoofed native START/completion cannot mine a hidden upper railing");
         put(helper, obstacle, Blocks.AIR.defaultBlockState());
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void upper_rail_wins_over_a_farther_block_but_preserves_an_exact_solid_tie(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, CENTER, stairs(Direction.NORTH));
      put(helper, CENTER.east().above(), Blocks.STONE.defaultBlockState());
      aim(helper, player, Direction.NORTH, true, 1.0, 1.625);
      helper.assertValueEqual(vanillaBlock(player).getBlockPos(), helper.absolutePos(CENTER.east().above()), "vanilla sees the block behind the missed rail");
      upperHit(helper, player, true);
      put(helper, CENTER.east().above(), Blocks.AIR.defaultBlockState());
      put(helper, CENTER.above(), Blocks.STONE.defaultBlockState());
      BlockHitResult original = vanillaBlock(player);
      helper.assertTrue(pick(player, original) == original, "a solid block sharing the upper rail boundary wins exact hit ties");
      helper.assertTrue(part(helper, player) == null, "overlapping solid geometry must also occlude server rail selection");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void competing_upper_stairs_select_the_nearest_owner_and_never_the_rail_behind_it(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, CENTER, stairs(Direction.NORTH));
      put(helper, CENTER.west(), stairs(Direction.NORTH));
      aim(helper, player, Direction.NORTH, true, 2.0, 1.625);
      HitResult selected = pick(player, vanillaBlock(player));
      helper.assertTrue(selected instanceof BlockHitResult && selected.getType() == HitResult.Type.BLOCK, "a real upper stair intersection must be selected");
      helper.assertValueEqual(((BlockHitResult)selected).getBlockPos(), helper.absolutePos(CENTER.west()), "the nearest stair owner wins regardless of scan order");
      helper.assertTrue(part(helper, player) == null, "server selection cannot authorize the occluded stair behind the first overhang");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void nearer_pickable_entity_keeps_native_target_and_blocks_server_upper_mining(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, CENTER, stairs(Direction.NORTH));
      aim(helper, player, Direction.NORTH, true, 1.5, 1.625);
      Vec3 position = target(helper, Direction.NORTH, true, 1.625).add(-0.75, -1.0, 0);
      ArmorStand stand = EntityType.ARMOR_STAND.create(helper.getLevel());
      helper.assertTrue(stand != null, "entity fixture must be created");
      stand.setPos(position.x, position.y, position.z);
      stand.setNoGravity(true);
      helper.getLevel().addFreshEntity(stand);
      try {
         EntityHitResult entity = entityHit(player);
         helper.assertTrue(entity != null && entity.getEntity() == stand, "the actual vanilla entity ray must encounter the stand before the rail");
         helper.assertTrue(pick(player, entity) == entity, "the extension must preserve a nearer entity's exact vanilla result");
         helper.assertTrue(part(helper, player) == null, "server revalidation must reject mining through a nearer pickable entity");
         // Vanilla represents an out-of-attack-range entity as MISS, even when block reach is longer.
         BlockHitResult outOfRange = BlockHitResult.miss(entity.getLocation(), Direction.WEST, BlockPos.containing(entity.getLocation()));
         helper.assertTrue(pick(player, outOfRange) == outOfRange, "an entity-clamped MISS must not redirect into a hidden block behind it");
      } finally {
         stand.discard();
      }
      upperHit(helper, player, true);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void farther_entity_cannot_occlude_a_nearer_upper_stair_rail(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, CENTER, stairs(Direction.NORTH));
      aim(helper, player, Direction.NORTH, true, 1.0, 1.625);
      Vec3 position = target(helper, Direction.NORTH, true, 1.625).add(1.4, -1.0, 0);
      ArmorStand stand = EntityType.ARMOR_STAND.create(helper.getLevel());
      helper.assertTrue(stand != null, "entity fixture must be created");
      stand.setPos(position.x, position.y, position.z);
      stand.setNoGravity(true);
      helper.getLevel().addFreshEntity(stand);
      try {
         EntityHitResult entity = entityHit(player);
         helper.assertTrue(entity != null && entity.getEntity() == stand, "vanilla's missed overhang leaves a real farther entity hit");
         HitResult selected = pick(player, entity);
         helper.assertTrue(selected instanceof BlockHitResult && selected.getType() == HitResult.Type.BLOCK, "the nearer visible upper rail must occlude the farther entity");
         helper.assertValueEqual(((BlockHitResult)selected).getBlockPos(), helper.absolutePos(CENTER), "the nearer owning stair is selected");
         helper.assertTrue(part(helper, player) != null, "server and client must agree on the visible upper rail before the entity");
      } finally {
         stand.discard();
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void upper_targeting_respects_actual_block_reach_and_does_not_select_empty_air_above_model(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      // Keep both reach-boundary eye positions inside this seven-block template.
      // Default survival reach starts the westward fixture outside it, among other tests.
      var range = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
      helper.assertTrue(range != null, "the player must expose the native block interaction range attribute");
      range.setBaseValue(2.0);
      helper.assertValueEqual(player.blockInteractionRange(), 2.0, "the test must use the actual player reach attribute");
      put(helper, CENTER, stairs(Direction.NORTH));
      // Target point is 1/16 inside the outside face; add that to test actual hit distance.
      aim(helper, player, Direction.NORTH, true, player.blockInteractionRange() + 0.0625 + 0.05, 1.625);
      BlockHitResult original = vanillaBlock(player);
      helper.assertValueEqual(original.getType(), HitResult.Type.MISS, "outside-reach fixture must have no ordinary blocker masking the reach check");
      helper.assertTrue(pick(player, original) == original, "upper selection cannot extend the player's reach");
      helper.assertTrue(part(helper, player) == null, "server upper selection must enforce the same eye-ray reach");
      aim(helper, player, Direction.NORTH, true, player.blockInteractionRange() + 0.0625 - 0.05, 1.625);
      helper.assertValueEqual(vanillaBlock(player).getType(), HitResult.Type.MISS, "inside-reach fixture must remain an unobstructed upper-overhang ray");
      upperHit(helper, player, true);
      aim(helper, player, Direction.NORTH, true, 1.0, 1.95);
      original = vanillaBlock(player);
      helper.assertTrue(pick(player, original) == original, "taller collision may not create a visible target above the unchanged model");
      helper.assertTrue(part(helper, player) == null, "empty air above the model cannot authorize a railing completion");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 400)
   public static void upper_rail_uses_native_start_stop_duration_tool_wear_and_paid_drop(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, CENTER, stairs(Direction.NORTH));
      aim(helper, player, Direction.NORTH, true, 1.0, 1.625);
      BlockHitResult hit = upperHit(helper, player, true);
      BlockState before = helper.getBlockState(CENTER);
      float progress = before.getDestroyProgress(player, helper.getLevel(), hit.getBlockPos());
      helper.assertTrue(progress > 0 && progress < 1, "upper mining must retain ordinary steel hardness");
      int ticks = (int)Math.ceil(1.0F / progress) + 2;
      helper.assertTrue(ticks < 350, "native mining duration must fit the test timeout");
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
      helper.assertValueEqual(helper.getBlockState(CENTER), before, "early STOP must wait for native accumulated progress");
      tick(helper, player, ticks, () -> {
         assertRemoved(helper, true);
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "one completed native mine wears the tool once");
         helper.assertValueEqual(drops(helper), 1, "native completion drops one paid rail without a stair drop");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void upper_mining_honors_cancelled_start_and_break_events(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (boolean cancelStart : new boolean[]{true, false}) {
         put(helper, CENTER, stairs(Direction.NORTH));
         aim(helper, player, Direction.NORTH, true, 1.0, 1.625);
         BlockHitResult hit = upperHit(helper, player, true);
         BlockState before = helper.getBlockState(CENTER);
         Consumer<PlayerInteractEvent.LeftClickBlock> start = event -> {
            if (event.getEntity() == player && cancelStart) event.setCanceled(true);
         };
         Consumer<BlockEvent.BreakEvent> finish = event -> {
            if (event.getPlayer() == player && !cancelStart) event.setCanceled(true);
         };
         NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, PlayerInteractEvent.LeftClickBlock.class, start);
         NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, BlockEvent.BreakEvent.class, finish);
         try {
            action(helper, player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
            player.gameMode.destroyBlock(hit.getBlockPos());
         } finally {
            NeoForge.EVENT_BUS.unregister(start);
            NeoForge.EVENT_BUS.unregister(finish);
         }
         helper.assertValueEqual(helper.getBlockState(CENTER), before, "cancelled upper mining must preserve the entire assembly");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "cancelled upper mining must not damage the tool");
         helper.assertValueEqual(drops(helper), 0, "cancelled upper mining must not mint a drop");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void wall_appearing_after_upper_start_prevents_native_completion(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, CENTER, stairs(Direction.NORTH));
      aim(helper, player, Direction.NORTH, true, 2.0, 1.625);
      BlockHitResult hit = upperHit(helper, player, true);
      BlockState before = helper.getBlockState(CENTER);
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
      put(helper, CENTER.west().above(), Blocks.STONE.defaultBlockState());
      player.gameMode.destroyBlock(hit.getBlockPos());
      helper.assertValueEqual(helper.getBlockState(CENTER), before, "new world occlusion must invalidate previously authorized upper mining");
      helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "blocked completion must preserve durability");
      helper.assertValueEqual(drops(helper), 0, "blocked completion must preserve paid credits without drops");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void absent_upper_rails_preserve_unrelated_native_miss_and_block_targets(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, CENTER, stairs(Direction.NORTH).setValue(PortedBlocks.LEFT_RAILING, false).setValue(PortedBlocks.RIGHT_RAILING, false));
      aim(helper, player, Direction.NORTH, true, 1.0, 1.625);
      BlockHitResult original = vanillaBlock(player);
      helper.assertTrue(pick(player, original) == original, "no integrated upper rail means no target mutation");
      put(helper, CENTER.east().above(), Blocks.STONE.defaultBlockState());
      original = vanillaBlock(player);
      helper.assertTrue(pick(player, original) == original, "unrelated ordinary blocks retain vanilla object identity");
      helper.succeed();
   }

   private static BlockState stairs(Direction facing) {
      return ModBlocks.STEEL_CATWALK_STAIRS.get().defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, facing)
         .setValue(PortedBlocks.LEFT_RAILING, true).setValue(PortedBlocks.RIGHT_RAILING, true)
         .setValue(PortedBlocks.EDITED_STAIR_SIDES, 3).setValue(PortedBlocks.PAID_RAILINGS, 2);
   }
   private static ServerPlayer player(GameTestHelper helper) {
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "UpperRailTest"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      player.setOnGround(true);
      clearDrops(helper);
      return player;
   }
   private static Direction side(Direction facing, boolean left) { return left ? facing.getCounterClockWise() : facing.getClockWise(); }
   private static Vec3 step(Direction direction, double amount) { return new Vec3(direction.getStepX() * amount, 0, direction.getStepZ() * amount); }
   private static Vec3 target(GameTestHelper helper, Direction facing, boolean left, double height) {
      return Vec3.atLowerCornerOf(helper.absolutePos(CENTER)).add(0.5, height, 0.5)
         .add(step(facing, 0.375)).add(step(side(facing, left), 0.4375));
   }
   private static void aim(GameTestHelper helper, ServerPlayer player, Direction facing, boolean left, double distance, double height) {
      Vec3 target = target(helper, facing, left, height);
      look(player, target.add(step(side(facing, left), distance)), target);
   }
   private static void look(ServerPlayer player, Vec3 eye, Vec3 target) {
      player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
      Vec3 delta = target.subtract(eye);
      player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
      player.setYHeadRot(player.getYRot());
      player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
   }
   private static Vec3 end(ServerPlayer player) { return player.getEyePosition().add(player.getViewVector(1).scale(player.blockInteractionRange())); }
   private static BlockHitResult vanillaBlock(ServerPlayer player) {
      return player.level().clip(new ClipContext(player.getEyePosition(), end(player), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
   }
   private static HitResult pick(ServerPlayer player, HitResult original) { return StairRailingTargeting.extendPick(player, player.blockInteractionRange(), 1, original); }
   private static EntityHitResult entityHit(ServerPlayer player) {
      Vec3 start = player.getEyePosition();
      return ProjectileUtil.getEntityHitResult(player, start, end(player), player.getBoundingBox().expandTowards(end(player).subtract(start)).inflate(1.0),
         entity -> !entity.isSpectator() && entity.isPickable(), player.blockInteractionRange() * player.blockInteractionRange());
   }
   private static PortedBlocks.RailingPart part(GameTestHelper helper, ServerPlayer player) {
      return PortedBlocks.selectRailingPart(helper.getLevel(), helper.absolutePos(CENTER), player.getEyePosition(), end(player), player);
   }
   private static BlockHitResult upperHit(GameTestHelper helper, ServerPlayer player, boolean left) {
      HitResult selected = pick(player, vanillaBlock(player));
      helper.assertTrue(selected instanceof BlockHitResult && selected.getType() == HitResult.Type.BLOCK, "real upper eye ray must discover a block target");
      BlockHitResult hit = (BlockHitResult)selected;
      helper.assertValueEqual(hit.getBlockPos(), helper.absolutePos(CENTER), "client extension targets the owning stair");
      helper.assertTrue(hit.getLocation().y >= hit.getBlockPos().getY() + 1.0 - 1.0E-7, "test hit must remain above the owning cell");
      PortedBlocks.RailingPart part = part(helper, player);
      Direction facing = helper.getBlockState(CENTER).getValue(PortedBlocks.HORIZONTAL_FACING);
      helper.assertTrue(part != null && part.kind() == PortedBlocks.PartKind.RAILING && part.side() == side(facing, left),
         "server selector and client highlight must agree on the exact upper side");
      return hit;
   }
   private static void assertRemoved(GameTestHelper helper, boolean left) {
      BlockState state = helper.getBlockState(CENTER);
      helper.assertTrue(state.is(ModBlocks.STEEL_CATWALK_STAIRS.get()), "the stair must survive upper rail mining");
      helper.assertValueEqual(state.getValue(PortedBlocks.LEFT_RAILING), !left, "left side removal must match the real ray");
      helper.assertValueEqual(state.getValue(PortedBlocks.RIGHT_RAILING), left, "right side removal must match the real ray");
      helper.assertValueEqual(state.getValue(PortedBlocks.PAID_RAILINGS), 1, "one paid credit remains for the other side");
   }
   private static void put(GameTestHelper helper, BlockPos relative, BlockState state) { helper.getLevel().setBlock(helper.absolutePos(relative), state, 2); }
   private static void action(GameTestHelper helper, ServerPlayer player, BlockHitResult hit, ServerboundPlayerActionPacket.Action action) {
      player.gameMode.handleBlockBreakAction(hit.getBlockPos(), action, hit.getDirection(), helper.getLevel().getMaxBuildHeight(), 0);
   }
   private static int drops(GameTestHelper helper) {
      return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(1.5), entity -> entity.getItem().is(ModBlocks.STEEL_RAILING.get().asItem()))
         .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
   }
   private static void clearDrops(GameTestHelper helper) { helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(1.5)).forEach(ItemEntity::discard); }
   private static void tick(GameTestHelper helper, ServerPlayer player, int remaining, Runnable complete) {
      if (remaining == 0) complete.run();
      else helper.runAfterDelay(1, () -> { player.gameMode.tick(); tick(helper, player, remaining - 1, complete); });
   }
}
