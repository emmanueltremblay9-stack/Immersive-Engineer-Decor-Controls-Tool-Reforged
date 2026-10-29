package com.oblixorprime.engineersdecorreforged.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native stair-integrated editing/mining, distinct from existing standalone rails above stairs. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class StairRailingGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 1, 3);
   private StairRailingGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void held_railing_edits_clicked_stair_side_in_place_for_every_rotation(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean left : new boolean[]{true, false}) {
            put(helper, state(facing, false, false, 0));
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(rail(), 4));
            click(helper, player, left);
            assertSides(helper, left, !left, "native held-railing click must install only its chosen side");
            helper.assertBlockProperty(CENTER, PortedBlocks.HORIZONTAL_FACING, facing);
            helper.assertBlockProperty(CENTER, PortedBlocks.PAID_RAILINGS, 1);
            helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "installing a side consumes exactly one rail");
            for (Direction neighbor : Direction.values()) {
               if (neighbor != Direction.DOWN) {
                  helper.assertTrue(helper.getBlockState(CENTER.relative(neighbor)).isAir(), "integrated installation must not place a neighboring block");
               }
            }
            click(helper, player, left);
            assertSides(helper, false, false, "a second native click removes only the installed side");
            helper.assertValueEqual(player.getMainHandItem().getCount(), 4, "removal refunds only the purchased rail");
            helper.assertBlockProperty(CENTER, PortedBlocks.PAID_RAILINGS, 0);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stair_mining_selects_each_side_and_then_the_floor_in_all_rotations(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean left : new boolean[]{true, false}) {
            clearDrops(helper);
            put(helper, state(facing, true, true, 2));
            mine(helper, player, left);
            assertSides(helper, !left, left, "completed native mining must remove just the aimed stair side");
            helper.assertBlockProperty(CENTER, PortedBlocks.HORIZONTAL_FACING, facing);
            helper.assertValueEqual(dropped(helper, rail()), 1, "only one paid rail drops on side removal");
            helper.assertValueEqual(dropped(helper, stairs()), 0, "side removal must not drop a stair");
            mine(helper, player, null);
            helper.assertTrue(helper.getBlockState(CENTER).isAir(), "mining the actual step removes the stair normally");
            helper.assertValueEqual(dropped(helper, rail()), 2, "floor mining returns the remaining paid rail once");
            helper.assertValueEqual(dropped(helper, stairs()), 1, "floor mining returns one stair");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 400)
   public static void stair_railing_start_stop_uses_native_duration_and_one_tool_durability(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, state(Direction.NORTH, true, true, 2));
      BlockHitResult hit = aim(helper, player, true);
      BlockState before = helper.getBlockState(CENTER);
      float progress = before.getDestroyProgress(player, helper.getLevel(), hit.getBlockPos());
      helper.assertTrue(progress > 0 && progress < 1, "stair rail mining must retain native steel duration");
      int ticks = (int)Math.ceil(1.0F / progress) + 2;
      helper.assertTrue(ticks < 350, "native duration must fit the test timeout");
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
      helper.assertValueEqual(helper.getBlockState(CENTER), before, "early STOP must not remove the rail immediately");
      helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "START does not use durability");
      tick(helper, player, ticks, () -> {
         assertSides(helper, false, true, "native delayed completion must retain the stair and other side");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "one native completion uses one tool durability");
         helper.assertValueEqual(dropped(helper, rail()), 1, "native completion produces one paid drop");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stair_edits_survive_neighbor_changes_waterlogging_and_real_save_reload(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, NativeFluidloggedBlock.withFluidContent(state(Direction.NORTH, true, true, 2),
         NativeFluidloggedBlock.FluidContent.WATER));
      mine(helper, player, true);
      BlockState before = helper.getBlockState(CENTER);
      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos neighbor = helper.absolutePos(CENTER.relative(direction));
         helper.getLevel().setBlock(neighbor, ModBlocks.STEEL_RAILING.get().defaultBlockState(), 3);
         helper.getLevel().setBlock(neighbor, Blocks.AIR.defaultBlockState(), 3);
      }
      helper.assertValueEqual(helper.getBlockState(CENTER), before, "neighbor refresh cannot restore a mined side or erase an installed side");
      var saved = NbtUtils.writeBlockState(before);
      helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved), before, "NBT reload must retain side flags, edits, payment and water");
      helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow(), before, "codec reload must retain the complete stair state");
      mine(helper, player, false);
      mine(helper, player, null);
      helper.assertTrue(helper.getBlockState(CENTER).is(Blocks.WATER), "mining the final waterlogged floor restores source water");
      helper.assertValueEqual(dropped(helper, rail()), 2, "reload and waterlogging cannot duplicate paid material");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void legacy_stair_connections_remain_automatic_until_that_side_is_edited(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      var saved = NbtUtils.writeBlockState(state(Direction.NORTH, true, false, 0));
      saved.getCompound("Properties").remove("edited_stair_sides");
      saved.getCompound("Properties").remove("paid_railings");
      BlockState legacy = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
      put(helper, legacy);
      mine(helper, player, true);
      helper.assertValueEqual(dropped(helper, rail()), 0, "legacy decorative sides were never paid for and must not mint rail items");
      BlockPos west = helper.absolutePos(CENTER.west());
      BlockPos east = helper.absolutePos(CENTER.east());
      helper.getLevel().setBlock(west, ModBlocks.STEEL_RAILING.get().defaultBlockState(), 3);
      helper.getLevel().setBlock(east, ModBlocks.STEEL_RAILING.get().defaultBlockState(), 3);
      assertSides(helper, false, true, "explicitly removed left stays absent while untouched right keeps legacy neighbor connectivity");
      helper.getLevel().setBlock(west, Blocks.AIR.defaultBlockState(), 3);
      helper.getLevel().setBlock(east, Blocks.AIR.defaultBlockState(), 3);
      assertSides(helper, false, false, "untouched legacy side still follows a disappearing neighbor");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void cancelled_stair_start_or_break_preserves_assembly_tool_and_drops(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (boolean cancelStart : new boolean[]{true, false}) {
         put(helper, state(Direction.SOUTH, true, true, 2));
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
            mine(helper, player, true);
            helper.assertValueEqual(helper.getBlockState(CENTER), before, "late native event cancellation must preserve both rails and the stair");
         } finally {
            NeoForge.EVENT_BUS.unregister(start);
            NeoForge.EVENT_BUS.unregister(finish);
         }
         player.gameMode.destroyBlock(helper.absolutePos(CENTER));
         helper.assertValueEqual(helper.getBlockState(CENTER), before, "cancelled event intent cannot authorize another completion without a fresh START");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "cancelled mining preserves tool durability");
         helper.assertValueEqual(dropped(helper, rail()), 0, "cancelled mining cannot produce drops");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void denied_stair_item_use_and_right_click_do_not_install_a_neighbor(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (boolean cancel : new boolean[]{true, false}) {
         put(helper, state(Direction.EAST, false, false, 0));
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(rail(), 4));
         Consumer<PlayerInteractEvent.RightClickBlock> deny = event -> {
            if (event.getEntity() == player) {
               if (cancel) event.setCanceled(true);
               else { event.setUseBlock(TriState.FALSE); event.setUseItem(TriState.FALSE); }
            }
         };
         NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerInteractEvent.RightClickBlock.class, deny);
         try { click(helper, player, true); }
         finally { NeoForge.EVENT_BUS.unregister(deny); }
         assertSides(helper, false, false, "cancelled native right click must preserve the stair");
         helper.assertValueEqual(player.getMainHandItem().getCount(), 4, "cancelled addition must not consume a rail");
         helper.assertTrue(helper.getBlockState(CENTER.above()).isAir(), "cancelled interaction must not fall through to an above-stair block");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stair_edit_respects_per_item_target_permission(GameTestHelper helper) {
      BlockPos denied = helper.absolutePos(CENTER);
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "DeniedStairEdit")) {
         @Override
         public boolean mayUseItemAt(BlockPos pos, Direction face, ItemStack stack) {
            return !pos.equals(denied) && super.mayUseItemAt(pos, face, stack);
         }
      };
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(rail(), 4));
      for (boolean installed : new boolean[]{false, true}) {
         BlockState before = state(Direction.NORTH, installed, false, installed ? 1 : 0);
         put(helper, before);
         click(helper, player, true);
         helper.assertValueEqual(helper.getBlockState(CENTER), before, "per-item target denial must prevent both installation and removal");
         helper.assertValueEqual(player.getMainHandItem().getCount(), 4, "denied edits must not charge or refund a rail");
         helper.assertTrue(helper.getBlockState(CENTER.above()).isAir(), "denied integrated edit must not place a neighboring railing");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void adventure_stair_mining_requires_exact_native_can_break_component(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      player.gameMode.changeGameModeForPlayer(GameType.ADVENTURE);
      for (boolean allowed : new boolean[]{false, true}) {
         put(helper, state(Direction.EAST, true, true, 2));
         player.setItemInHand(InteractionHand.MAIN_HAND, allowed ? adventureTool(helper) : new ItemStack(Items.IRON_PICKAXE));
         mine(helper, player, true);
         assertSides(helper, !allowed, true, "only native CAN_BREAK authorization permits adventure rail removal");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), allowed ? 1 : 0, "only authorized adventure mining wears the tool");
         helper.assertTrue(!player.getAbilities().mayBuild, "mining must not elevate adventure build privileges");
      }
      helper.assertValueEqual(dropped(helper, rail()), 1, "only the authorized adventure mining drops a rail");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void creative_spectator_wrong_tool_and_disabled_drops_preserve_stair_contract(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, state(Direction.WEST, true, true, 2));
      player.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
      mine(helper, player, true);
      assertSides(helper, true, true, "spectators cannot edit a side");
      player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
      mine(helper, player, true);
      assertSides(helper, false, true, "creative mining removes only the aimed side");
      helper.assertBlockProperty(CENTER, PortedBlocks.PAID_RAILINGS, 1);
      helper.assertValueEqual(dropped(helper, rail()), 0, "creative removal destroys its material credit");
      helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "creative mining does not wear the pickaxe");
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      boolean rule = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
      try {
         for (boolean correctTool : new boolean[]{false, true}) {
            put(helper, state(Direction.WEST, true, true, 2));
            player.setItemInHand(InteractionHand.MAIN_HAND, correctTool ? new ItemStack(Items.IRON_PICKAXE) : ItemStack.EMPTY);
            helper.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(!correctTool, helper.getLevel().getServer());
            mine(helper, player, true);
            assertSides(helper, false, true, "nonharvesting removal must still preserve the stair and other side");
            helper.assertBlockProperty(CENTER, PortedBlocks.PAID_RAILINGS, 1);
            helper.assertValueEqual(dropped(helper, rail()), 0, "wrong tool or disabled drops suppresses refunds without preserving the spent credit");
         }
      } finally { helper.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(rule, helper.getLevel().getServer()); }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stale_stair_target_cannot_destroy_a_different_side_floor_or_replacement(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (BlockState replacement : List.of(state(Direction.NORTH, false, true, 1), state(Direction.SOUTH, true, true, 2), Blocks.STONE.defaultBlockState())) {
         put(helper, state(Direction.NORTH, true, true, 2));
         action(helper, player, aim(helper, player, true), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
         put(helper, replacement);
         player.gameMode.destroyBlock(helper.absolutePos(CENTER));
         helper.assertValueEqual(helper.getBlockState(CENTER), replacement, "stale completion cannot target a changed stair or replacement block");
      }
      put(helper, state(Direction.NORTH, true, true, 2));
      BlockState before = helper.getBlockState(CENTER);
      action(helper, player, aim(helper, player, true), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
      aim(helper, player, null);
      player.gameMode.destroyBlock(helper.absolutePos(CENTER));
      helper.assertValueEqual(helper.getBlockState(CENTER), before, "changing aim to the step requires a new START");
      helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "stale targeting cannot consume durability");
      helper.assertValueEqual(dropped(helper, rail()), 0, "stale targeting cannot create loot");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stair_side_collision_is_fence_height_above_both_treads_and_keeps_center_clear(GameTestHelper helper) {
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         BlockState state = state(facing, true, true, 2);
         var collision = state.getCollisionShape(helper.getLevel(), helper.absolutePos(CENTER));
         for (boolean high : new boolean[]{false, true}) {
            double floor = high ? 12.0 / 16 : 4.0 / 16;
            double depth = high ? 0.25 : -0.25;
            for (Direction side : List.of(facing.getCounterClockWise(), facing.getClockWise())) {
               double x = 0.5 + facing.getStepX() * depth + side.getStepX() * 0.45;
               double z = 0.5 + facing.getStepZ() * depth + side.getStepZ() * 0.45;
               var probe = Shapes.box(x - 0.01, floor + 1.25, z - 0.01, x + 0.01, floor + 1.45, z + 0.01);
               helper.assertTrue(Shapes.joinIsNotEmpty(collision, probe, BooleanOp.AND), "side collision must stay above normal jump height on each tread for " + facing);
            }
         }
         helper.assertTrue(!Shapes.joinIsNotEmpty(collision, Shapes.box(0.2, 0.75, 0.2, 0.8, 2.5, 0.8), BooleanOp.AND), "rails must preserve a player-wide center passage");
         helper.assertTrue(state.getShape(helper.getLevel(), helper.absolutePos(CENTER)).max(Direction.Axis.Y) > 1.8,
            "selection must include the unchanged sloped model's upper side envelope");
      }
      helper.succeed();
   }

   private static Block stairs() { return ModBlocks.STEEL_CATWALK_STAIRS.get(); }
   private static Item rail() { return ModBlocks.STEEL_RAILING.get().asItem(); }
   private static BlockState state(Direction facing, boolean left, boolean right, int paid) {
      return stairs().defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, facing)
         .setValue(PortedBlocks.LEFT_RAILING, left).setValue(PortedBlocks.RIGHT_RAILING, right)
         .setValue(PortedBlocks.EDITED_STAIR_SIDES, 3).setValue(PortedBlocks.PAID_RAILINGS, paid);
   }
   private static void put(GameTestHelper helper, BlockState state) { helper.getLevel().setBlock(helper.absolutePos(CENTER), state, 2); }
   private static ServerPlayer player(GameTestHelper helper) {
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "StairEditTest"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      player.setOnGround(true);
      clearDrops(helper);
      return player;
   }
   private static void assertSides(GameTestHelper helper, boolean left, boolean right, String message) {
      BlockState state = helper.getBlockState(CENTER);
      helper.assertTrue(state.is(stairs()), message + ": stair must remain");
      helper.assertValueEqual(state.getValue(PortedBlocks.LEFT_RAILING), left, message + ": left");
      helper.assertValueEqual(state.getValue(PortedBlocks.RIGHT_RAILING), right, message + ": right");
   }
   private static BlockHitResult aim(GameTestHelper helper, ServerPlayer player, Boolean left) {
      BlockPos pos = helper.absolutePos(CENTER);
      Direction facing = helper.getBlockState(CENTER).getValue(PortedBlocks.HORIZONTAL_FACING);
      Direction side = left == null ? facing : left ? facing.getCounterClockWise() : facing.getClockWise();
      double offset = left == null ? 0 : 0.45;
      player.setPos(pos.getX() + 0.5 + side.getStepX() * offset, pos.getY() + 2.7 - player.getEyeHeight(), pos.getZ() + 0.5 + side.getStepZ() * offset);
      player.setYRot(0);
      player.setXRot(90);
      Vec3 eye = player.getEyePosition();
      BlockHitResult hit = helper.getLevel().clip(new ClipContext(eye, eye.add(player.getViewVector(1).scale(player.blockInteractionRange())), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
      helper.assertValueEqual(hit.getType(), HitResult.Type.BLOCK, "native eye ray must hit a stair component");
      helper.assertValueEqual(hit.getBlockPos(), pos, "native eye ray must hit the stair itself");
      return hit;
   }
   private static void click(GameTestHelper helper, ServerPlayer player, boolean left) {
      BlockHitResult hit = aim(helper, player, left);
      player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
   }
   private static void mine(GameTestHelper helper, ServerPlayer player, Boolean left) {
      BlockState before = helper.getBlockState(CENTER);
      BlockHitResult hit = aim(helper, player, left);
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
      if (player.gameMode.isCreative()) return;
      helper.assertValueEqual(helper.getBlockState(CENTER), before, "noncreative START cannot instantly remove a component");
      player.gameMode.destroyBlock(hit.getBlockPos());
   }
   private static void action(GameTestHelper helper, ServerPlayer player, BlockHitResult hit, ServerboundPlayerActionPacket.Action action) {
      player.gameMode.handleBlockBreakAction(hit.getBlockPos(), action, hit.getDirection(), helper.getLevel().getMaxBuildHeight(), 0);
   }
   private static int dropped(GameTestHelper helper, Block block) { return dropped(helper, block.asItem()); }
   private static int dropped(GameTestHelper helper, Item item) {
      return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(1.5), entity -> entity.getItem().is(item))
         .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
   }
   private static void clearDrops(GameTestHelper helper) {
      helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(1.5)).forEach(ItemEntity::discard);
   }
   private static void tick(GameTestHelper helper, ServerPlayer player, int remaining, Runnable complete) {
      if (remaining == 0) complete.run();
      else helper.runAfterDelay(1, () -> { player.gameMode.tick(); tick(helper, player, remaining - 1, complete); });
   }
   private static ItemStack adventureTool(GameTestHelper helper) {
      JsonObject predicate = new JsonObject();
      predicate.addProperty("blocks", BuiltInRegistries.BLOCK.getKey(stairs()).toString());
      JsonArray predicates = new JsonArray(); predicates.add(predicate);
      JsonObject canBreak = new JsonObject(); canBreak.add("predicates", predicates);
      JsonObject components = new JsonObject(); components.add("minecraft:can_break", canBreak);
      JsonObject stack = new JsonObject(); stack.addProperty("id", "minecraft:iron_pickaxe"); stack.addProperty("count", 1); stack.add("components", components);
      return ItemStack.CODEC.parse(helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE), stack).getOrThrow();
   }
}
