package com.oblixorprime.engineersdecorreforged.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkBlock;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real eye rays and native mining entry points for the recessed, removable catwalk lamp. */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RaisedCatwalkLampGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 3, 3);
   private static final Direction[] APPROACHES = {Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

   private RaisedCatwalkLampGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_eye_selection_matches_existing_model_from_below_and_four_sides(GameTestHelper helper) throws Exception {
      ServerPlayer player = player(helper);
      put(helper, lit(true));
      VoxelShape modelLamp = Shapes.empty();
      String resource = "/assets/" + EngineersDecorReforged.MOD_ID + "/models/block/furniture/steel_catwalk_ta4_model.json";
      try (var input = RaisedCatwalkLampGameTests.class.getResourceAsStream(resource)) {
         helper.assertTrue(input != null, "the original lit catwalk model must exist");
         JsonArray elements = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
            .getAsJsonObject().getAsJsonArray("elements");
         int lampElements = 0;
         for (var element : elements) {
            JsonObject cube = element.getAsJsonObject();
            JsonArray from = cube.getAsJsonArray("from");
            JsonArray to = cube.getAsJsonArray("to");
            if (from.get(1).getAsDouble() > 14 && to.get(1).getAsDouble() <= 15 && to.get(1).getAsDouble() > from.get(1).getAsDouble()) {
               lampElements++;
               modelLamp = Shapes.or(modelLamp, Block.box(from.get(0).getAsDouble(), from.get(1).getAsDouble(), from.get(2).getAsDouble(),
                  to.get(0).getAsDouble(), to.get(1).getAsDouble(), to.get(2).getAsDouble()));
            }
         }
         helper.assertValueEqual(lampElements, 9, "fixture independently reads the glass and eight existing mounting tabs");
      }
      for (Direction approach : APPROACHES) {
         lampAim(helper, player, approach);
         PortedBlocks.RailingPart lamp = selected(helper, player);
         helper.assertValueEqual(lamp.kind(), PortedBlocks.PartKind.INSET_LIGHT, "actual eye ray selects the inset light from " + approach);
         helper.assertFalse(Shapes.joinIsNotEmpty(modelLamp, lamp.shape(), BooleanOp.NOT_SAME),
            "highlight/mining shape must equal the existing model's lamp, not its whole deck");
      }
      floorAim(helper, player);
      helper.assertValueEqual(selected(helper, player).kind(), PortedBlocks.PartKind.BASE, "top deck occludes lamp when viewed from above");
      aim(helper, player, new Vec3(0.2, 0.1, 0.2), new Vec3(0.2, 1, 0.2));
      helper.assertValueEqual(selected(helper, player).kind(), PortedBlocks.PartKind.BASE, "underside outside the lamp is still the deck");
      for (Direction side : Direction.Plane.HORIZONTAL) {
         Vec3 center = new Vec3(0.5, 14.8 / 16, 0.5);
         aim(helper, player, center.add(Vec3.atLowerCornerOf(side.getNormal()).scale(1.7)), center);
         helper.assertValueEqual(selected(helper, player).kind(), PortedBlocks.PartKind.BASE,
            "a horizontal ray through the outer rim must hit that visible rim before the lamp");
      }
      VoxelShape collision = helper.getBlockState(CENTER).getCollisionShape(helper.getLevel(), helper.absolutePos(CENTER));
      helper.assertFalse(Shapes.joinIsNotEmpty(collision, Block.box(0, 14, 0, 16, 16, 16), BooleanOp.NOT_SAME),
         "model-aligned selection must not change the continuous deck walking collision");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_native_mining_from_every_approach_keeps_deck_and_refunds_once(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction approach : APPROACHES) {
         clearDrops(helper);
         put(helper, lit(true));
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
         mine(helper, player, lampAim(helper, player, approach));
         assertUnlit(helper, 0);
         helper.assertValueEqual(dropped(helper, light()), 1, "survival mining must return the one paid lamp");
         helper.assertValueEqual(dropped(helper, catwalk().asItem()), 0, "selective lamp mining cannot drop the deck");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "completed native lamp mining wears the tool once");
         BlockState unlit = helper.getBlockState(CENTER);
         player.gameMode.destroyBlock(helper.absolutePos(CENTER));
         helper.assertValueEqual(helper.getBlockState(CENTER), unlit, "reusing a completed lamp mine cannot destroy the remaining deck");
         helper.assertValueEqual(dropped(helper, light()), 1, "reusing a completed mine cannot duplicate the paid lamp");
         mine(helper, player, floorAim(helper, player));
         helper.assertTrue(helper.getBlockState(CENTER).isAir(), "a fresh mine aimed at the deck can remove it normally");
         helper.assertValueEqual(dropped(helper, catwalk().asItem()), 1, "the separately mined deck returns one catwalk");
         helper.assertValueEqual(dropped(helper, light()), 1, "later deck mining cannot refund the lamp again");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 400)
   public static void inset_lamp_native_start_stop_retains_real_duration_and_one_tool_use(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, lit(true));
      BlockState original = helper.getBlockState(CENTER);
      BlockHitResult hit = lampAim(helper, player, Direction.DOWN);
      float progress = original.getDestroyProgress(player, helper.getLevel(), hit.getBlockPos());
      helper.assertTrue(progress > 0 && progress < 1, "the lamp assembly requires ordinary native mining progress");
      int ticks = (int)Math.ceil(1 / progress) + 2;
      helper.assertTrue(ticks < 350, "native duration fits the test timeout");
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
      helper.assertValueEqual(helper.getBlockState(CENTER), original, "early STOP cannot instantly remove the lamp");
      helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "START/early STOP do not wear the tool");
      tickMining(helper, player, ticks, () -> {
         assertUnlit(helper, 0);
         helper.assertValueEqual(dropped(helper, light()), 1, "delayed native completion drops exactly the paid lamp");
         helper.assertValueEqual(dropped(helper, catwalk().asItem()), 0, "delayed native completion keeps the catwalk");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "delayed completion uses one tool durability");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_creative_native_start_clears_credit_without_loot_or_wear(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
      for (boolean paid : new boolean[]{false, true}) {
         put(helper, lit(paid));
         mine(helper, player, lampAim(helper, player, Direction.DOWN));
         assertUnlit(helper, 0);
         helper.assertValueEqual(dropped(helper, light()), 0, "creative removal drops no paid or unpaid lamp");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "creative START does not wear the tool");
      }
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      mine(helper, player, floorAim(helper, player));
      helper.assertValueEqual(dropped(helper, light()), 0, "survival cannot recover a credit consumed in creative");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_legacy_load_and_last_durability_pickaxe_conserve_paid_material(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (boolean paid : new boolean[]{false, true}) {
         clearDrops(helper);
         var saved = NbtUtils.writeBlockState(lit(paid));
         if (!paid) {
            saved.getCompound("Properties").remove("paid_light");
         }
         put(helper, NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved));
         ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
         pickaxe.setDamageValue(pickaxe.getMaxDamage() - 1);
         player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
         mine(helper, player, lampAim(helper, player, Direction.DOWN));
         assertUnlit(helper, 0);
         helper.assertTrue(player.getMainHandItem().isEmpty(), "the last native tool use breaks the pickaxe");
         helper.assertValueEqual(dropped(helper, light()), paid ? 1 : 0,
            "saved paid lamps refund once even on last durability; legacy coordinate lamps never mint an item");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_wrong_tool_and_disabled_tile_drops_remove_without_refund(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      boolean originalRule = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
      try {
         for (boolean properTool : new boolean[]{false, true}) {
            put(helper, lit(true));
            player.setItemInHand(InteractionHand.MAIN_HAND, properTool ? new ItemStack(Items.IRON_PICKAXE) : ItemStack.EMPTY);
            helper.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(!properTool, helper.getLevel().getServer());
            mine(helper, player, lampAim(helper, player, Direction.DOWN));
            assertUnlit(helper, 0);
            helper.assertValueEqual(dropped(helper, light()), 0, "wrong tool and doTileDrops=false both suppress the refund");
            helper.assertValueEqual(dropped(helper, catwalk().asItem()), 0, "non-harvesting mining must still preserve the deck");
         }
      } finally {
         helper.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(originalRule, helper.getLevel().getServer());
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_native_drop_cancellation_cannot_recreate_paid_credit(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, lit(true));
      int[] drops = {0};
      Consumer<BlockDropsEvent> cancel = event -> {
         if (event.getBreaker() == player) {
            drops[0]++;
            event.setCanceled(true);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, BlockDropsEvent.class, cancel);
      try {
         mine(helper, player, lampAim(helper, player, Direction.DOWN));
         assertUnlit(helper, 0);
         helper.assertValueEqual(drops[0], 1, "one paid lamp removal enters the native drop hook exactly once");
         helper.assertValueEqual(dropped(helper, light()), 0, "the drop hook can suppress lamp loot");
      } finally {
         NeoForge.EVENT_BUS.unregister(cancel);
      }
      mine(helper, player, floorAim(helper, player));
      helper.assertValueEqual(dropped(helper, light()), 0, "later deck mining cannot revive canceled lamp loot");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_canceled_break_early_or_late_preserves_state_tool_and_credit(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (EventPriority priority : List.of(EventPriority.HIGHEST, EventPriority.LOWEST)) {
         put(helper, lit(true));
         BlockState original = helper.getBlockState(CENTER);
         Consumer<BlockEvent.BreakEvent> cancel = event -> {
            if (event.getPlayer() == player) {
               event.setCanceled(true);
            }
         };
         NeoForge.EVENT_BUS.addListener(priority, false, BlockEvent.BreakEvent.class, cancel);
         try {
            mine(helper, player, lampAim(helper, player, Direction.DOWN));
            helper.assertValueEqual(helper.getBlockState(CENTER), original, "canceled native break retains the lamp and its credit");
         } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
         }
         player.gameMode.destroyBlock(helper.absolutePos(CENTER));
         helper.assertValueEqual(helper.getBlockState(CENTER), original, "late canceled completion remains invalid after the listener is gone");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "both canceled attempts preserve tool durability");
         helper.assertValueEqual(dropped(helper, light()), 0, "canceled attempts cannot refund the paid light");
      }
      mine(helper, player, lampAim(helper, player, Direction.DOWN));
      assertUnlit(helper, 0);
      helper.assertValueEqual(dropped(helper, light()), 1, "a fresh uncanceled native START can remove the lamp");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_canceled_or_denied_start_cannot_authorize_later_completion(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (boolean cancel : new boolean[]{false, true}) {
         for (EventPriority priority : List.of(EventPriority.HIGHEST, EventPriority.LOWEST)) {
            put(helper, lit(true));
            BlockState original = helper.getBlockState(CENTER);
            Consumer<PlayerInteractEvent.LeftClickBlock> deny = event -> {
               if (event.getEntity() == player && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
                  if (cancel) {
                     event.setCanceled(true);
                  } else {
                     event.setUseItem(TriState.FALSE);
                  }
               }
            };
            NeoForge.EVENT_BUS.addListener(priority, false, PlayerInteractEvent.LeftClickBlock.class, deny);
            try {
               mine(helper, player, lampAim(helper, player, Direction.DOWN));
               helper.assertValueEqual(helper.getBlockState(CENTER), original, "denied START must retain the lamp even after intent capture");
               helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "denied START must prevent native tool wear");
               helper.assertValueEqual(dropped(helper, light()), 0, "denied START cannot release paid lamp loot");
            } finally {
               NeoForge.EVENT_BUS.unregister(deny);
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_adventure_uses_exact_native_can_break_and_spectator_cannot_mine(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      player.gameMode.changeGameModeForPlayer(GameType.ADVENTURE);
      for (boolean allowed : new boolean[]{false, true}) {
         put(helper, lit(true));
         player.setItemInHand(InteractionHand.MAIN_HAND, allowed ? adventurePickaxe(helper) : new ItemStack(Items.IRON_PICKAXE));
         mine(helper, player, lampAim(helper, player, Direction.DOWN));
         if (allowed) {
            assertUnlit(helper, 0);
            helper.assertValueEqual(dropped(helper, light()), 1, "an authorized adventure pickaxe can recover the paid lamp");
         } else {
            helper.assertValueEqual(helper.getBlockState(CENTER), lit(true), "ordinary adventure pickaxe cannot remove an inset light");
            helper.assertValueEqual(dropped(helper, light()), 0, "denied adventure mining grants no loot");
            helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "denied adventure mining grants no tool use");
         }
         helper.assertFalse(player.getAbilities().mayBuild, "mining must not elevate native adventure permissions");
      }
      clearDrops(helper);
      put(helper, lit(true));
      player.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
      mine(helper, player, lampAim(helper, player, Direction.DOWN));
      helper.assertValueEqual(helper.getBlockState(CENTER), lit(true), "spectator mining cannot alter the lamp");
      helper.assertValueEqual(dropped(helper, light()), 0, "spectator mining cannot refund a light");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_retarget_abort_and_replacement_never_break_a_different_component(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, lit(true));
      action(helper, player, lampAim(helper, player, Direction.DOWN), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
      floorAim(helper, player);
      player.gameMode.destroyBlock(helper.absolutePos(CENTER));
      helper.assertValueEqual(helper.getBlockState(CENTER), lit(true), "lamp START cannot authorize a later deck target with the same null rail side");
      action(helper, player, floorAim(helper, player), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
      lampAim(helper, player, Direction.DOWN);
      player.gameMode.destroyBlock(helper.absolutePos(CENTER));
      helper.assertValueEqual(helper.getBlockState(CENTER), lit(true), "deck START cannot authorize a later lamp target");
      BlockHitResult hit = lampAim(helper, player, Direction.DOWN);
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK);
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
      player.gameMode.destroyBlock(helper.absolutePos(CENTER));
      helper.assertValueEqual(helper.getBlockState(CENTER), lit(true), "an aborted lamp intent cannot be completed later");
      for (BlockState replacement : List.of(catwalk().defaultBlockState(), lit(false), Blocks.STONE.defaultBlockState(), Blocks.AIR.defaultBlockState())) {
         put(helper, lit(true));
         action(helper, player, lampAim(helper, player, Direction.DOWN), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
         put(helper, replacement);
         player.gameMode.destroyBlock(helper.absolutePos(CENTER));
         helper.assertValueEqual(helper.getBlockState(CENTER), replacement, "replacement or paid-state changes invalidate the saved lamp target");
      }
      helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "all stale native completions must precede tool use");
      helper.assertValueEqual(dropped(helper, light()), 0, "stale lamp completions grant no material");
      helper.assertValueEqual(dropped(helper, catwalk().asItem()), 0, "stale lamp completions cannot release the deck");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_tool_or_creative_mode_swap_requires_a_fresh_native_start(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (ItemStack replacement : List.of(new ItemStack(Items.DIAMOND_PICKAXE), ItemStack.EMPTY, adventurePickaxe(helper))) {
         put(helper, lit(true));
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
         action(helper, player, lampAim(helper, player, Direction.DOWN), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
         player.setItemInHand(InteractionHand.MAIN_HAND, replacement);
         player.gameMode.destroyBlock(helper.absolutePos(CENTER));
         helper.assertValueEqual(helper.getBlockState(CENTER), lit(true), "changed tool item or components cannot reuse a previous lamp START");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "a stale tool intent must be rejected before durability use");
         helper.assertValueEqual(dropped(helper, light()), 0, "a stale tool intent cannot refund a lamp");
      }
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      action(helper, player, lampAim(helper, player, Direction.DOWN), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
      player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
      player.gameMode.destroyBlock(helper.absolutePos(CENTER));
      helper.assertValueEqual(helper.getBlockState(CENTER), lit(true), "switching to creative cannot reuse a survival mining intent");
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      mine(helper, player, lampAim(helper, player, Direction.DOWN));
      assertUnlit(helper, 0);
      helper.assertValueEqual(dropped(helper, light()), 1, "fresh START with the current tool and mode can mine the lamp normally");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_mending_damage_change_follows_native_tool_reset_policy(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, lit(true));
      player.getMainHandItem().setDamageValue(4);
      action(helper, player, lampAim(helper, player, Direction.DOWN), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
      player.getMainHandItem().setDamageValue(3); // A mending-style change does not reset native mining.
      player.gameMode.destroyBlock(helper.absolutePos(CENTER));
      assertUnlit(helper, 0);
      helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 4, "allowed durability-only change still receives one native mining use");
      helper.assertValueEqual(dropped(helper, light()), 1, "native mending exception must still harvest the paid lamp once");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_world_occlusion_is_shared_by_highlight_and_native_mining(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      BlockPos pos = helper.absolutePos(CENTER);
      put(helper, lit(true));
      BlockHitResult originalHit = aim(helper, player, new Vec3(0.5, -1, 0.5), new Vec3(0.5, 1, 0.5));
      helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
      try {
         helper.assertValueEqual(eyeHit(helper, player).getBlockPos(), pos.below(), "intervening stone must block the actual eye ray");
         helper.assertTrue(selected(helper, player) == null, "an occluded light has no component highlight");
         action(helper, player, originalHit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
         player.gameMode.destroyBlock(pos);
         helper.assertValueEqual(helper.getBlockState(CENTER), lit(true), "a packet naming a hidden lamp cannot bypass world occlusion");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "an occluded lamp cannot wear the tool");
      } finally {
         helper.getLevel().setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 2);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_removal_preserves_water_support_railings_neighbors_and_saved_state(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      BlockPos pos = helper.absolutePos(CENTER);
      Block[] supports = {Blocks.AIR, ModBlocks.THICK_STEEL_POLE.get(), ModBlocks.THIN_STEEL_POLE.get(), ModBlocks.TREATED_WOOD_POLE.get()};
      int[] variants = {0, 1, 2, 5};
      for (BlockState wetCell : wetCells()) for (int index = 0; index < supports.length; index++) {
         clearDrops(helper);
         helper.getLevel().setBlock(pos.below(), supports[index].defaultBlockState(), 2);
         put(helper, com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.copyCell(lit(true), wetCell));
         BlockState railing = ModBlocks.STEEL_RAILING.get().defaultBlockState().setValue(PortedBlocks.NORTH, true);
         helper.getLevel().setBlock(pos.above(), railing, 2);
         BlockState savedSupport = helper.getLevel().getBlockState(pos.below());
         mine(helper, player, lampAim(helper, player, Direction.DOWN));
         assertUnlit(helper, variants[index]);
         helper.assertValueEqual(helper.getLevel().getBlockState(pos.below()), savedSupport, "mining the lamp cannot remove or alter its supporting pole");
         helper.assertValueEqual(helper.getLevel().getBlockState(pos.above()), railing, "mining the lamp cannot remove or alter railings above the deck");
         helper.assertValueEqual(helper.getBlockState(CENTER).getFluidState(), wetCell.getFluidState(), "selective lamp removal retains source or flowing cell exactly");
         BlockState unlit = helper.getBlockState(CENTER);
         var saved = NbtUtils.writeBlockState(unlit);
         BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
         helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow(), unlit, "codec reload preserves all post-removal state");
         put(helper, loaded);
         for (Direction side : Direction.Plane.HORIZONTAL) {
            helper.getLevel().setBlock(pos.relative(side), Blocks.STONE.defaultBlockState(), 3);
            helper.getLevel().setBlock(pos.relative(side), Blocks.AIR.defaultBlockState(), 3);
         }
         helper.assertValueEqual(helper.getBlockState(CENTER), unlit, "neighbor updates after save/load cannot restore the lamp or credit");
         player.gameMode.destroyBlock(pos);
         helper.assertValueEqual(helper.getBlockState(CENTER), unlit, "repeated old completion after reload cannot remove the remaining deck");
         helper.assertValueEqual(dropped(helper, light()), 1, "water, supports, neighbor updates and reload still refund exactly once");
         helper.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void inset_lamp_repeated_real_installations_and_native_removals_do_not_duplicate_items(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (BlockState wetCell : wetCells()) {
      clearDrops(helper);
      put(helper, wetCell);
      for (int cycle = 0; cycle < 3; cycle++) {
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(light(), 1));
         BlockHitResult underside = aim(helper, player, new Vec3(0.5, 0.1, 0.5), new Vec3(0.5, 1, 0.5));
         helper.assertTrue(player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, underside).consumesAction(),
            "native underside item use installs a lamp");
         helper.assertTrue(player.getMainHandItem().isEmpty(), "survival installation consumes the one lamp");
         helper.assertValueEqual(helper.getBlockState(CENTER), com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.copyCell(lit(true), wetCell), "real installation creates exactly one paid lamp credit");
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
         mine(helper, player, lampAim(helper, player, Direction.DOWN));
         assertUnlit(helper, 0);
         helper.assertValueEqual(helper.getBlockState(CENTER).getFluidState(), wetCell.getFluidState(), "paid lamp cycle preserves independent exterior fluid");
         helper.assertValueEqual(dropped(helper, light()), cycle + 1, "each independently purchased lamp returns once");
      }
      mine(helper, player, floorAim(helper, player));
      helper.assertValueEqual(dropped(helper, light()), 3, "final deck loot cannot recreate previously refunded lamps");
      helper.assertValueEqual(dropped(helper, catwalk().asItem()), 1, "the whole workflow returns the base catwalk once");
      }
      helper.succeed();
   }

   private static java.util.List<BlockState> wetCells() {
      var cells = new java.util.ArrayList<BlockState>();
      for (var fluid : java.util.List.of(net.minecraft.world.level.material.Fluids.WATER,
            net.minecraft.world.level.material.Fluids.LAVA, com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.creosote()))
         cells.add(com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.placement(catwalk().defaultBlockState(),
            ((net.minecraft.world.level.material.FlowingFluid)fluid).getSource(false)));
      cells.add(com.oblixorprime.engineersdecorreforged.block.StairWaterFlow.withFlow(catwalk().defaultBlockState(),
         net.minecraft.world.level.material.Fluids.FLOWING_WATER.getFlowing(5, false)));
      return cells;
   }

   private static ServerPlayer player(GameTestHelper helper) {
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "InsetLampMining"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      player.setOnGround(true);
      clearDrops(helper);
      return player;
   }

   private static Block catwalk() { return ModBlocks.STEEL_CATWALK_TA.get(); }
   private static Item light() { return ModBlocks.IRON_INSET_LIGHT.get().asItem(); }
   private static BlockState lit(boolean paid) {
      return catwalk().defaultBlockState().setValue(RaisedCatwalkBlock.VARIANT, 4).setValue(RaisedCatwalkBlock.PAID_LIGHT, paid);
   }
   private static void put(GameTestHelper helper, BlockState state) { helper.getLevel().setBlock(helper.absolutePos(CENTER), state, 2); }

   private static void assertUnlit(GameTestHelper helper, int variant) {
      BlockState state = helper.getBlockState(CENTER);
      helper.assertTrue(state.is(catwalk()), "removing the inset lamp must retain the raised catwalk");
      helper.assertValueEqual(state.getValue(RaisedCatwalkBlock.VARIANT), variant, "lamp removal restores the current pole-supported deck");
      helper.assertFalse(state.getValue(RaisedCatwalkBlock.PAID_LIGHT), "lamp removal consumes the paid-light credit");
      helper.assertValueEqual(state.getLightEmission(helper.getLevel(), helper.absolutePos(CENTER)), com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.light(state), "lamp removal retains only fluid emission");
   }

   private static BlockHitResult lampAim(GameTestHelper helper, ServerPlayer player, Direction approach) {
      Vec3 eye = approach == Direction.DOWN ? new Vec3(0.5, 0.1, 0.5)
         : new Vec3(0.5 + approach.getStepX() * 1.7, 0.2, 0.5 + approach.getStepZ() * 1.7);
      BlockHitResult hit = aim(helper, player, eye, new Vec3(0.5, 14.8 / 16, 0.5));
      helper.assertValueEqual(selected(helper, player).kind(), PortedBlocks.PartKind.INSET_LIGHT, "fixture must truly aim at the lamp");
      return hit;
   }

   private static BlockHitResult floorAim(GameTestHelper helper, ServerPlayer player) {
      BlockHitResult hit = aim(helper, player, new Vec3(0.5, 2.5, 0.5), new Vec3(0.5, 1, 0.5));
      helper.assertValueEqual(selected(helper, player).kind(), PortedBlocks.PartKind.BASE, "fixture must truly aim at the deck");
      return hit;
   }

   private static BlockHitResult aim(GameTestHelper helper, ServerPlayer player, Vec3 eye, Vec3 target) {
      Vec3 origin = Vec3.atLowerCornerOf(helper.absolutePos(CENTER));
      Vec3 delta = target.subtract(eye);
      player.setPos(origin.x + eye.x, origin.y + eye.y - player.getEyeHeight(), origin.z + eye.z);
      player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
      // Server-side LivingEntity.getViewYRot(1) reads head yaw, not entity/body yaw.
      // FakePlayer is not ticked here, so synchronize it explicitly for real side rays.
      player.setYHeadRot(player.getYRot());
      player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
      // Mth's float lookup table does not produce an exactly unit-length view vector.
      // Normalize only this angular check; actual selection/mining keeps the native ray.
      Vec3 view = player.getViewVector(1);
      helper.assertTrue(view.normalize().dot(delta.normalize()) > 0.99999,
         "fixture eye ray must point toward its requested target: eye=" + eye + ", target=" + target
            + ", view=" + view + ", yaw=" + player.getYRot() + ", headYaw=" + player.getYHeadRot() + ", pitch=" + player.getXRot());
      BlockHitResult hit = eyeHit(helper, player);
      helper.assertValueEqual(hit.getType(), HitResult.Type.BLOCK, "the player's actual eye ray must hit visible geometry");
      helper.assertValueEqual(hit.getBlockPos(), helper.absolutePos(CENTER), "the eye ray must hit the intended raised catwalk");
      return hit;
   }

   private static BlockHitResult eyeHit(GameTestHelper helper, ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      return helper.getLevel().clip(new ClipContext(eye, eye.add(player.getViewVector(1).scale(player.blockInteractionRange())),
         ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
   }

   private static PortedBlocks.RailingPart selected(GameTestHelper helper, ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      return PortedBlocks.selectRailingPart(helper.getLevel(), helper.absolutePos(CENTER), eye,
         eye.add(player.getViewVector(1).scale(player.blockInteractionRange())), player);
   }

   private static void action(GameTestHelper helper, ServerPlayer player, BlockHitResult hit, ServerboundPlayerActionPacket.Action action) {
      player.gameMode.handleBlockBreakAction(hit.getBlockPos(), action, hit.getDirection(), helper.getLevel().getMaxBuildHeight(), 0);
   }

   private static void mine(GameTestHelper helper, ServerPlayer player, BlockHitResult hit) {
      BlockState before = helper.getBlockState(CENTER);
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
      if (!player.gameMode.isCreative()) {
         helper.assertValueEqual(helper.getBlockState(CENTER), before, "survival START cannot bypass native mining duration");
         // Exercise native completion (BreakEvent, held-tool rules/use and onDestroyedByPlayer).
         // The dedicated START/STOP test above drives the full delayed progression as well.
         player.gameMode.destroyBlock(hit.getBlockPos());
      }
   }

   private static void tickMining(GameTestHelper helper, ServerPlayer player, int ticks, Runnable finished) {
      if (ticks == 0) {
         finished.run();
      } else {
         helper.runAfterDelay(1, () -> {
            player.gameMode.tick();
            tickMining(helper, player, ticks - 1, finished);
         });
      }
   }

   private static int dropped(GameTestHelper helper, Item item) {
      return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(1.5),
         entity -> entity.getItem().is(item)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
   }

   private static void clearDrops(GameTestHelper helper) {
      helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(1.5)).forEach(ItemEntity::discard);
   }

   private static ItemStack adventurePickaxe(GameTestHelper helper) {
      JsonObject predicate = new JsonObject();
      predicate.addProperty("blocks", BuiltInRegistries.BLOCK.getKey(catwalk()).toString());
      JsonArray predicates = new JsonArray();
      predicates.add(predicate);
      JsonObject canBreak = new JsonObject();
      canBreak.add("predicates", predicates);
      JsonObject components = new JsonObject();
      components.add("minecraft:can_break", canBreak);
      JsonObject stack = new JsonObject();
      stack.addProperty("id", "minecraft:iron_pickaxe");
      stack.addProperty("count", 1);
      stack.add("components", components);
      return ItemStack.CODEC.parse(helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE), stack).getOrThrow();
   }
}
