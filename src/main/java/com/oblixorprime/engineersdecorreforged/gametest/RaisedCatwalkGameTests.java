package com.oblixorprime.engineersdecorreforged.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RaisedCatwalkGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 2, 3);

   private RaisedCatwalkGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_item_placement_uses_support_not_coordinates(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block[] supports = {Blocks.AIR, ModBlocks.THICK_STEEL_POLE.get(), ModBlocks.THIN_STEEL_POLE.get(),
         ModBlocks.THICK_STEEL_POLE_HEAD.get(), ModBlocks.THIN_STEEL_POLE_HEAD.get(),
         ModBlocks.TREATED_WOOD_POLE.get(), ModBlocks.TREATED_WOOD_POLE_HEAD.get()};
      int[] expected = {0, 1, 2, 1, 2, 5, 5};
      for (int x = 1; x <= 5; x++) {
         BlockPos pos = helper.absolutePos(new BlockPos(x, 2, 3));
         for (int support = 0; support < supports.length; support++) {
            clearAround(helper, pos);
            helper.getLevel().setBlock(pos.below(), supports[support].defaultBlockState(), 3);
            helper.getLevel().setBlock(pos.north(), Blocks.STONE.defaultBlockState(), 3);
            place(helper, player, pos, Direction.SOUTH);
            helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(RaisedCatwalkBlock.VARIANT),
               expected[support], "side placement must be determined by the support, at every former coordinate residue");
         }
         clearAround(helper, pos);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void raised_catwalk_wood_support_works_for_every_placement_face_and_water_state(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      for (int x = 1; x <= 5; x++) {
         BlockPos pos = helper.absolutePos(new BlockPos(x, 2, 3));
         for (Block support : List.of(ModBlocks.TREATED_WOOD_POLE.get(), ModBlocks.TREATED_WOOD_POLE_HEAD.get())) {
            for (boolean wet : new boolean[] {false, true}) {
               for (Direction face : Direction.values()) {
                  clearAround(helper, pos);
                  helper.getLevel().setBlock(pos.relative(face.getOpposite()), Blocks.STONE.defaultBlockState(), 3);
                  helper.getLevel().setBlock(pos.below(), support.defaultBlockState()
                     .setValue(DirectionalBlock.FACING, Direction.UP), 3);
                  if (wet) {
                     helper.getLevel().setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                  }
                  place(helper, player, pos, face);
                  BlockState state = helper.getLevel().getBlockState(pos);
                  helper.assertValueEqual(state.getValue(RaisedCatwalkBlock.VARIANT), 5,
                     "wood below must select the wooden connection for every click face and former coordinate residue");
                  helper.assertValueEqual(state.getValue(RaisedCatwalkBlock.WATERLOGGED), wet,
                     "wood-supported item placement must preserve water");
                  helper.assertFalse(state.getValue(RaisedCatwalkBlock.PAID_LIGHT), "wood never creates paid light credit");
                  helper.getLevel().setBlock(pos, NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),
                     NbtUtils.writeBlockState(state)), 3);
                  helper.getLevel().setBlock(pos.east(), Blocks.STONE.defaultBlockState(), 3);
                  helper.getLevel().setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 3);
                  helper.assertValueEqual(helper.getLevel().getBlockState(pos), state,
                     "saved wooden connections survive unrelated side updates");
                  helper.getLevel().setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
                  helper.assertValueEqual(helper.getLevel().getBlockState(pos), state.setValue(RaisedCatwalkBlock.VARIANT, 0),
                     "removing the wooden support after save/load restores the plain deck without losing water");
               }
            }
         }
         clearAround(helper, pos);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_top_placement_selects_frame_and_preserves_water(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      for (boolean wet : new boolean[] {false, true}) {
         for (Direction face : Direction.values()) {
            clearAround(helper, pos);
            helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            helper.getLevel().setBlock(pos.relative(face.getOpposite()), Blocks.STONE.defaultBlockState(), 3);
            if (wet) {
               helper.getLevel().setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            }
            place(helper, player, pos, face);
            BlockState state = helper.getLevel().getBlockState(pos);
            helper.assertValueEqual(state.getValue(RaisedCatwalkBlock.VARIANT), face == Direction.UP ? 3 : 0,
               "only an explicit sturdy-top click creates the frame");
            helper.assertValueEqual(state.getValue(RaisedCatwalkBlock.WATERLOGGED), wet,
               "real item placement must retain source water");
            helper.assertValueEqual(state.getFluidState().isSource(), wet, "waterlogged state must expose source fluid");
         }
      }
      clearAround(helper, pos);
      for (Block support : List.of(ModBlocks.THICK_STEEL_POLE.get(), ModBlocks.THIN_STEEL_POLE.get(),
            ModBlocks.THICK_STEEL_POLE_HEAD.get(), ModBlocks.THIN_STEEL_POLE_HEAD.get())) {
         helper.getLevel().setBlock(pos.below(), support.defaultBlockState(), 3);
         place(helper, player, pos, Direction.UP);
         helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(RaisedCatwalkBlock.VARIANT),
            support == ModBlocks.THICK_STEEL_POLE.get() || support == ModBlocks.THICK_STEEL_POLE_HEAD.get() ? 1 : 2,
            "a recognized steel pole takes priority over the frame rule");
         helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_support_updates_are_reversible_and_keep_explicit_choices(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      clearAround(helper, pos);
      helper.getLevel().setBlock(pos, catwalk().defaultBlockState().setValue(RaisedCatwalkBlock.WATERLOGGED, true), 3);
      Block[] supports = {ModBlocks.THICK_STEEL_POLE.get(), ModBlocks.THIN_STEEL_POLE.get(),
         ModBlocks.THICK_STEEL_POLE_HEAD.get(), ModBlocks.THIN_STEEL_POLE_HEAD.get(),
         ModBlocks.TREATED_WOOD_POLE.get(), ModBlocks.THICK_STEEL_POLE.get(), ModBlocks.TREATED_WOOD_POLE_HEAD.get(),
         ModBlocks.THIN_STEEL_POLE.get(), ModBlocks.TREATED_WOOD_POLE.get(), Blocks.AIR,
         ModBlocks.TREATED_WOOD_POLE_HEAD.get(), Blocks.STONE, Blocks.AIR};
      int[] expected = {1, 2, 1, 2, 5, 1, 5, 2, 5, 0, 5, 0, 0};
      for (int i = 0; i < supports.length; i++) {
         helper.getLevel().setBlock(pos.below(), supports[i].defaultBlockState(), 3);
         BlockState state = helper.getLevel().getBlockState(pos);
         helper.assertValueEqual(state.getValue(RaisedCatwalkBlock.VARIANT), expected[i],
            "a real below-neighbor update must connect, switch and disconnect pole geometry");
         helper.assertTrue(state.getValue(RaisedCatwalkBlock.WATERLOGGED), "support changes must keep the water");
      }
      helper.getLevel().setBlock(pos, catwalk().defaultBlockState().setValue(RaisedCatwalkBlock.VARIANT, 3), 3);
      helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
      helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(RaisedCatwalkBlock.VARIANT), 3,
         "the explicitly placed frame remains a frame after ordinary support changes");
      helper.getLevel().setBlock(pos, catwalk().defaultBlockState().setValue(RaisedCatwalkBlock.VARIANT, 4)
         .setValue(RaisedCatwalkBlock.PAID_LIGHT, true), 3);
      for (Block support : supports) {
         helper.getLevel().setBlock(pos.below(), support.defaultBlockState(), 3);
         helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(RaisedCatwalkBlock.VARIANT), 4,
            "neighbor changes must never replace the installed light");
         assertDrops(helper, helper.getLevel().getBlockState(pos), 1);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_native_inset_light_cycles_conserve_items_in_both_hands(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      ServerPlayer player = player(helper);
      for (InteractionHand hand : InteractionHand.values()) {
         clearAround(helper, pos);
         helper.getLevel().setBlock(pos, catwalk().defaultBlockState().setValue(RaisedCatwalkBlock.WATERLOGGED, true), 3);
         player.getInventory().clearContent();
         player.setItemInHand(hand, new ItemStack(light(), 4));
         for (int cycle = 0; cycle < 3; cycle++) {
            click(helper, player, hand, Direction.DOWN);
            BlockState installed = helper.getLevel().getBlockState(pos);
            helper.assertValueEqual(installed.getValue(RaisedCatwalkBlock.VARIANT), 4, "underside click installs the light");
            helper.assertTrue(installed.getValue(RaisedCatwalkBlock.PAID_LIGHT), "survival installation owns one lamp");
            helper.assertValueEqual(owned(helper, player, light()), 3, "installation consumes exactly one lamp");
            helper.assertValueEqual(installed.getLightEmission(helper.getLevel(), pos),
               ModBlocks.IRON_INSET_LIGHT.get().defaultBlockState().getLightEmission(helper.getLevel(), pos),
               "the integrated light must emit the same actual light as its standalone block");
            assertDrops(helper, installed, 1);
            BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(installed));
            helper.getLevel().setBlock(pos, loaded, 3);
            click(helper, player, hand, Direction.DOWN);
            BlockState removed = helper.getLevel().getBlockState(pos);
            helper.assertValueEqual(removed.getValue(RaisedCatwalkBlock.VARIANT), 0, "a second underside click removes the light");
            helper.assertFalse(removed.getValue(RaisedCatwalkBlock.PAID_LIGHT), "removal clears the lamp credit");
            helper.assertValueEqual(removed.getLightEmission(helper.getLevel(), pos), 0, "plain catwalk emits no light");
            helper.assertTrue(removed.getValue(RaisedCatwalkBlock.WATERLOGGED), "editing keeps waterlogging");
            helper.assertValueEqual(owned(helper, player, light()), 4, "removal returns exactly the purchased lamp");
            assertDrops(helper, removed, 0);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_paid_light_break_returns_one_lamp_and_one_catwalk(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      clearAround(helper, pos);
      helper.getLevel().setBlock(pos, catwalk().defaultBlockState(), 3);
      ServerPlayer player = player(helper);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(light(), 2));
      click(helper, player, InteractionHand.MAIN_HAND, Direction.DOWN);
      ItemStack remaining = player.getMainHandItem();
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      helper.assertTrue(player.getInventory().add(remaining), "fixture must retain the unspent lamp");
      helper.assertTrue(player.gameMode.destroyBlock(pos), "real survival game-mode break must succeed");
      helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "the whole raised catwalk must be gone");
      helper.assertValueEqual(owned(helper, player, light()), 2, "mining must return the one paid lamp without duplication");
      helper.assertValueEqual(owned(helper, player, catwalk().asItem()), 1, "mining must drop exactly one base catwalk");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_light_refund_survives_full_inventory_and_new_pole_support(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      clearAround(helper, pos);
      helper.getLevel().setBlock(pos, catwalk().defaultBlockState(), 3);
      ServerPlayer player = player(helper);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(light(), 2));
      click(helper, player, InteractionHand.MAIN_HAND, Direction.DOWN);
      helper.getLevel().setBlock(pos.below(), ModBlocks.THICK_STEEL_POLE.get().defaultBlockState(), 3);
      helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(RaisedCatwalkBlock.VARIANT), 4,
         "adding a pole must not discard the installed lamp");
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
      }
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(light(), 64));
      click(helper, player, InteractionHand.MAIN_HAND, Direction.DOWN);
      BlockState removed = helper.getLevel().getBlockState(pos);
      helper.assertValueEqual(removed.getValue(RaisedCatwalkBlock.VARIANT), 1,
         "removing a light must discover the pole support added while it was installed");
      helper.assertValueEqual(owned(helper, player, light()), 65,
         "full inventory must drop exactly the one refund instead of losing or duplicating it");
      assertDrops(helper, removed, 0);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_wood_connection_after_saved_paid_light_removal_conserves_items(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      ServerPlayer player = player(helper);
      for (Block support : List.of(ModBlocks.TREATED_WOOD_POLE.get(), ModBlocks.TREATED_WOOD_POLE_HEAD.get())) {
         for (InteractionHand hand : InteractionHand.values()) {
            clearAround(helper, pos);
            player.getInventory().clearContent();
            helper.getLevel().setBlock(pos, catwalk().defaultBlockState()
               .setValue(RaisedCatwalkBlock.WATERLOGGED, true), 3);
            player.setItemInHand(hand, new ItemStack(light(), 2));
            click(helper, player, hand, Direction.DOWN);
            helper.getLevel().setBlock(pos.below(), support.defaultBlockState()
               .setValue(DirectionalBlock.FACING, Direction.UP), 3);
            BlockState lit = helper.getLevel().getBlockState(pos);
            helper.assertValueEqual(lit.getValue(RaisedCatwalkBlock.VARIANT), 4,
               "a new wood support cannot discard the installed inset light");
            assertDrops(helper, lit, 1);
            helper.getLevel().setBlock(pos, NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),
               NbtUtils.writeBlockState(lit)), 3);
            click(helper, player, hand, Direction.DOWN);
            BlockState unlit = helper.getLevel().getBlockState(pos);
            helper.assertValueEqual(unlit.getValue(RaisedCatwalkBlock.VARIANT), 5,
               "removing the saved inset light must discover its new wooden support");
            helper.assertFalse(unlit.getValue(RaisedCatwalkBlock.PAID_LIGHT), "the refund clears the paid-light credit");
            helper.assertTrue(unlit.getValue(RaisedCatwalkBlock.WATERLOGGED), "the wooden connection retains water");
            helper.assertValueEqual(owned(helper, player, light()), 2, "exactly the purchased inset light is returned");
            helper.assertValueEqual(unlit.getLightEmission(helper.getLevel(), pos), 0, "wooden connections emit no light");
            assertDrops(helper, unlit, 0);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_adventure_and_spectator_cannot_edit_or_extend(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      ServerPlayer player = player(helper);
      for (GameType mode : List.of(GameType.ADVENTURE, GameType.SPECTATOR)) {
         player.gameMode.changeGameModeForPlayer(mode);
         for (int variant : new int[] {0, 4}) {
            clearAround(helper, pos);
            BlockState original = catwalk().defaultBlockState().setValue(RaisedCatwalkBlock.VARIANT, variant)
               .setValue(RaisedCatwalkBlock.PAID_LIGHT, variant == 4);
            helper.getLevel().setBlock(pos, original, 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(light(), 2));
            click(helper, player, InteractionHand.MAIN_HAND, Direction.DOWN);
            helper.assertValueEqual(helper.getLevel().getBlockState(pos), original, mode + " cannot add or remove inset lights");
            helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "denied light edit must retain the held items");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(catwalk(), 2));
            click(helper, player, InteractionHand.MAIN_HAND, Direction.UP);
            helper.assertTrue(helper.getLevel().getBlockState(pos.relative(player.getDirection())).isAir(),
               mode + " cannot extend catwalks");
            helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "denied extension must retain the held items");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_legacy_and_creative_lights_cannot_refund_free_items(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      clearAround(helper, pos);
      ServerPlayer player = player(helper);
      for (int variant = 0; variant <= 4; variant++) {
         var saved = NbtUtils.writeBlockState(catwalk().defaultBlockState().setValue(RaisedCatwalkBlock.VARIANT, variant));
         saved.getCompound("Properties").remove("waterlogged");
         saved.getCompound("Properties").remove("paid_light");
         BlockState legacy = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
         helper.assertValueEqual(legacy.getValue(RaisedCatwalkBlock.VARIANT), variant, "every old numeric state must still load unchanged");
         helper.assertFalse(legacy.getValue(RaisedCatwalkBlock.PAID_LIGHT), "old random states cannot fabricate paid lamp credit");
         helper.assertFalse(legacy.getValue(RaisedCatwalkBlock.WATERLOGGED), "old dry states default to dry");
         assertDrops(helper, legacy, 0);
         helper.getLevel().setBlock(pos, legacy, 3);
         helper.getLevel().setBlock(pos.east(), Blocks.STONE.defaultBlockState(), 3);
         helper.getLevel().setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 3);
         helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(RaisedCatwalkBlock.VARIANT), variant,
            "unrelated neighbor updates preserve legacy variants");
      }
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(light(), 2));
      click(helper, player, InteractionHand.MAIN_HAND, Direction.DOWN);
      helper.assertValueEqual(owned(helper, player, light()), 2, "removing an unpaid legacy lamp grants nothing");
      player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
      click(helper, player, InteractionHand.MAIN_HAND, Direction.DOWN);
      helper.assertValueEqual(owned(helper, player, light()), 2, "creative installation consumes no lamp");
      assertDrops(helper, helper.getLevel().getBlockState(pos), 0);
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      click(helper, player, InteractionHand.MAIN_HAND, Direction.DOWN);
      helper.assertValueEqual(owned(helper, player, light()), 2, "changing to survival cannot cash out a creative lamp");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_native_extension_uses_player_direction_and_checks_obstructions(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      ServerPlayer player = player(helper);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (Direction face : List.of(Direction.UP, Direction.DOWN)) {
            clearAround(helper, pos);
            helper.getLevel().setBlock(pos, catwalk().defaultBlockState(), 3);
            BlockPos target = pos.relative(facing);
            helper.getLevel().setBlock(target, Blocks.WATER.defaultBlockState(), 3);
            helper.getLevel().setBlock(target.below(), ModBlocks.THIN_STEEL_POLE.get().defaultBlockState(), 3);
            player.getInventory().clearContent();
            player.setYRot(facing.toYRot());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(catwalk(), 3));
            click(helper, player, InteractionHand.MAIN_HAND, face);
            BlockState extended = helper.getLevel().getBlockState(target);
            helper.assertTrue(extended.is(catwalk()), "vertical-face use must extend one block in the player's horizontal direction");
            helper.assertValueEqual(extended.getValue(RaisedCatwalkBlock.VARIANT), 2, "extension adapts to its own pole support");
            helper.assertTrue(extended.getValue(RaisedCatwalkBlock.WATERLOGGED), "extension preserves target water");
            helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "extension consumes one catwalk");
            helper.assertTrue(helper.getLevel().getBlockState(pos.above()).isAir(), "extension must not place a catwalk above the clicked one");
            click(helper, player, InteractionHand.MAIN_HAND, face);
            helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "occupied extension target must not consume another item");
            helper.assertTrue(helper.getLevel().getBlockState(target.relative(facing)).isAir(), "blocked extension must not skip over its target");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_extension_honors_native_placement_cancellation(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      clearAround(helper, pos);
      helper.getLevel().setBlock(pos, catwalk().defaultBlockState(), 3);
      ServerPlayer player = player(helper);
      player.setYRot(Direction.EAST.toYRot());
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(catwalk(), 2));
      BlockPos target = pos.east();
      boolean[] notified = {false};
      Consumer<BlockEvent.EntityPlaceEvent> listener = event -> {
         if (event.getEntity() == player && event.getPos().equals(target)) {
            notified[0] = true;
            event.setCanceled(true);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, BlockEvent.EntityPlaceEvent.class, listener);
      try {
         click(helper, player, InteractionHand.MAIN_HAND, Direction.UP);
         helper.assertTrue(notified[0], "extension must pass through the native block-placement protection hook");
         helper.assertTrue(helper.getLevel().getBlockState(target).isAir(), "cancelled placement must restore the target");
         helper.assertTrue(helper.getLevel().getBlockState(pos.above()).isAir(), "cancellation must not fall through to another placement");
         helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "cancelled extension cannot consume an item");
      } finally {
         NeoForge.EVENT_BUS.unregister(listener);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_native_extension_discovers_wood_at_its_new_position(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      ServerPlayer player = player(helper);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (Direction face : List.of(Direction.UP, Direction.DOWN)) {
            for (Block support : List.of(ModBlocks.TREATED_WOOD_POLE.get(), ModBlocks.TREATED_WOOD_POLE_HEAD.get())) {
               clearAround(helper, pos);
               helper.getLevel().setBlock(pos, catwalk().defaultBlockState(), 3);
               BlockPos target = pos.relative(facing);
               helper.getLevel().setBlock(target, Blocks.WATER.defaultBlockState(), 3);
               helper.getLevel().setBlock(target.below(), support.defaultBlockState()
                  .setValue(DirectionalBlock.FACING, Direction.UP), 3);
               player.getInventory().clearContent();
               player.setYRot(facing.toYRot());
               player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(catwalk(), 3));
               click(helper, player, InteractionHand.MAIN_HAND, face);
               BlockState extended = helper.getLevel().getBlockState(target);
               helper.assertTrue(extended.is(catwalk()), "native extension places the new catwalk at the same height");
               helper.assertValueEqual(extended.getValue(RaisedCatwalkBlock.VARIANT), 5,
                  "extension must discover the wooden support directly below its own destination");
               helper.assertTrue(extended.getValue(RaisedCatwalkBlock.WATERLOGGED), "extension retains destination water");
               helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "wood-supported extension consumes one catwalk");
               click(helper, player, InteractionHand.MAIN_HAND, face);
               helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "blocked wood-supported extension consumes no item");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_variant_geometry_and_saved_economic_state_match_models(GameTestHelper helper) throws Exception {
      BlockPos pos = helper.absolutePos(CENTER);
      VoxelShape deck = Block.box(0, 14, 0, 16, 16, 16);
      VoxelShape[] expected = {deck, completeHeadGeometry(1), completeHeadGeometry(2),
         Shapes.or(deck, Block.box(0, 0, 0, 16, 2, 16), Block.box(0, 0, 0, 1, 16, 1),
            Block.box(15, 0, 0, 16, 16, 1), Block.box(15, 0, 15, 16, 16, 16), Block.box(0, 0, 15, 1, 16, 16)), deck,
         completeHeadGeometry(5)};
      String path = "/assets/" + EngineersDecorReforged.MOD_ID + "/blockstates/steel_catwalk_ta.json";
      try (var input = RaisedCatwalkGameTests.class.getResourceAsStream(path)) {
         helper.assertTrue(input != null, "raised catwalk blockstate resource must exist");
         var variants = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("variants");
         for (BlockState state : catwalk().getStateDefinition().getPossibleStates()) {
            int variant = state.getValue(RaisedCatwalkBlock.VARIANT);
            helper.assertFalse(Shapes.joinIsNotEmpty(state.getShape(helper.getLevel(), pos), expected[variant], BooleanOp.NOT_SAME),
               "outline must match the selected deck, complete pole head, or frame geometry");
            helper.assertFalse(Shapes.joinIsNotEmpty(state.getCollisionShape(helper.getLevel(), pos), expected[variant], BooleanOp.NOT_SAME),
               "collision must match the selected model's structural geometry");
            String model = variants.getAsJsonObject("variant=" + variant).get("model").getAsString();
            helper.assertTrue(model.endsWith("steel_catwalk_ta" + variant + "_model"), "every numeric variant retains its stable model selector");
            var saved = NbtUtils.writeBlockState(state);
            helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved), state,
               "NBT save/load retains variant, water and paid lamp credit");
            helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow(), state,
               "codec save/load retains all state properties");
            assertDrops(helper, state, variant == 4 && state.getValue(RaisedCatwalkBlock.PAID_LIGHT) ? 1 : 0);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void raised_catwalk_connections_preserve_complete_native_pole_heads(GameTestHelper helper) throws Exception {
      JsonObject deck = readModel("furniture/steel_catwalk_ta0_model");
      int[] variants = {1, 2, 5};
      String[] heads = {"straight_thick_metal_pole_head_model", "straight_thin_metal_pole_head_model", "straight_pole_head_model"};
      String[] sourceFaces = {"north", "south", "east", "west", "up", "down"};
      String[] targetFaces = {"down", "up", "east", "west", "north", "south"};
      int[] uvRotations = {180, 0, 90, 270, 180, 0};
      for (int variantIndex = 0; variantIndex < variants.length; variantIndex++) {
         int variant = variants[variantIndex];
         JsonObject connected = readModel("furniture/steel_catwalk_ta" + variant + "_model");
         JsonObject head = readModel("pole/" + heads[variantIndex]);
         var deckElements = deck.getAsJsonArray("elements");
         var elements = connected.getAsJsonArray("elements");
         var headElements = head.getAsJsonArray("elements");
         helper.assertValueEqual(elements.size(), deckElements.size() + headElements.size(),
            "each connection must retain every native pole-head element, including the plate and four braces");
         for (int i = 0; i < deckElements.size(); i++) {
            helper.assertValueEqual(elements.get(i), deckElements.get(i), "the steel deck must remain unchanged");
         }
         for (String texture : head.getAsJsonObject("textures").keySet()) {
            if (!texture.equals("particle")) {
               helper.assertValueEqual(connected.getAsJsonObject("textures").get(texture), head.getAsJsonObject("textures").get(texture),
                  "the connection must use the complete native head's textures, including wooden plate and braces");
            }
         }
         int plateCount = 0;
         int braceCount = 0;
         for (int i = 0; i < headElements.size(); i++) {
            JsonObject original = headElements.get(i).getAsJsonObject();
            JsonObject integrated = elements.get(deckElements.size() + i).getAsJsonObject();
            double[] from = integratedHeadPoint(original.getAsJsonArray("from"));
            double[] to = integratedHeadPoint(original.getAsJsonArray("to"));
            for (int axis = 0; axis < 3; axis++) {
               double lower = Math.min(from[axis], to[axis]);
               assertNear(helper, integrated.getAsJsonArray("from").get(axis).getAsDouble(), axis == 1 ? Math.max(0, lower) : lower,
                  "head geometry rotates upwards and trims only the shaft at the lower block boundary");
               assertNear(helper, integrated.getAsJsonArray("to").get(axis).getAsDouble(), Math.max(from[axis], to[axis]),
                  "plate and brace dimensions are retained");
            }
            helper.assertValueEqual(integrated.has("rotation"), original.has("rotation"), "all native angled braces are retained");
            if (original.has("rotation")) {
               braceCount++;
               JsonObject oldRotation = original.getAsJsonObject("rotation");
               JsonObject newRotation = integrated.getAsJsonObject("rotation");
               String oldAxis = oldRotation.get("axis").getAsString();
               String newAxis = switch (oldAxis) { case "x" -> "x"; case "y" -> "z"; default -> "y"; };
               helper.assertValueEqual(newRotation.get("axis").getAsString(), newAxis, "brace rotation axis follows the vertical head");
               assertNear(helper, newRotation.get("angle").getAsDouble(), oldRotation.get("angle").getAsDouble() * (oldAxis.equals("y") ? -1 : 1),
                  "brace slope is preserved");
               double[] origin = integratedHeadPoint(oldRotation.getAsJsonArray("origin"));
               for (int axis = 0; axis < 3; axis++) {
                  assertNear(helper, newRotation.getAsJsonArray("origin").get(axis).getAsDouble(), origin[axis], "brace pivot is preserved");
               }
            } else if (original.getAsJsonArray("from").get(2).getAsDouble() == 14) {
               plateCount++;
               assertNear(helper, integrated.getAsJsonArray("from").get(1).getAsDouble(), 12, "the plate is visibly below the deck");
               assertNear(helper, integrated.getAsJsonArray("to").get(1).getAsDouble(), 14, "the plate meets the deck with no gap");
            }
            var originalFaces = original.getAsJsonObject("faces");
            var integratedFaces = integrated.getAsJsonObject("faces");
            helper.assertValueEqual(integratedFaces.size(), originalFaces.size(), "head faces must not disappear when oriented vertically");
            for (int face = 0; face < sourceFaces.length; face++) {
               if (originalFaces.has(sourceFaces[face])) {
                  var originalFace = originalFaces.getAsJsonObject(sourceFaces[face]);
                  var integratedFace = integratedFaces.getAsJsonObject(targetFaces[face]);
                  helper.assertTrue(integratedFace != null, "the rotated face must exist");
                  helper.assertValueEqual(integratedFace.get("uv"), originalFace.get("uv"), "native texture sampling is retained");
                  helper.assertValueEqual(integratedFace.get("texture"), originalFace.get("texture"), "native face textures are retained");
                  int originalRotation = originalFace.has("rotation") ? originalFace.get("rotation").getAsInt() : 0;
                  int integratedRotation = integratedFace.has("rotation") ? integratedFace.get("rotation").getAsInt() : 0;
                  helper.assertValueEqual(integratedRotation, (originalRotation + uvRotations[face]) % 360,
                     "UV orientation follows each rotated head face");
               }
            }
         }
         helper.assertValueEqual(plateCount, 1, "each integrated head has exactly one visible plate");
         helper.assertValueEqual(braceCount, 4, "each integrated head retains all four native braces");
         BlockState state = catwalk().defaultBlockState().setValue(RaisedCatwalkBlock.VARIANT, variant);
         VoxelShape outline = state.getShape(helper.getLevel(), helper.absolutePos(CENTER));
         double plateMin = variant == 2 ? 4 : 3;
         VoxelShape plate = Block.box(plateMin, 12, plateMin, 16 - plateMin, 14, 16 - plateMin);
         helper.assertFalse(Shapes.joinIsNotEmpty(plate, outline, BooleanOp.ONLY_FIRST), "the visible plate must be fully selectable");
         for (int height = 0; height < 16; height++) {
            helper.assertFalse(Shapes.joinIsNotEmpty(Block.box(7.5, height, 7.5, 8.5, height + 1, 8.5), outline, BooleanOp.ONLY_FIRST),
               "support, plate, and deck must remain continuously connected");
         }
         assertDrops(helper, state, 0);
      }
      helper.succeed();
   }

   private static double[] integratedHeadPoint(JsonArray point) {
      return new double[] {point.get(0).getAsDouble(), point.get(2).getAsDouble() - 2, 16 - point.get(1).getAsDouble()};
   }

   private static void assertNear(GameTestHelper helper, double actual, double expected, String description) {
      helper.assertTrue(Math.abs(actual - expected) < 1.0e-8, description + ": " + actual + " != " + expected);
   }

   private static VoxelShape completeHeadGeometry(int variant) throws Exception {
      VoxelShape shape = Block.box(0, 14, 0, 16, 16, 16);
      // Preserve the established solid collision of the hollow rendered steel shafts.
      if (variant == 1 || variant == 2) {
         double min = variant == 1 ? 5 : 6;
         shape = Shapes.or(shape, Block.box(min, 0, min, 16 - min, 12, 16 - min));
      }
      var elements = readModel("furniture/steel_catwalk_ta" + variant + "_model").getAsJsonArray("elements");
      for (int i = 6; i < elements.size(); i++) {
         JsonObject element = elements.get(i).getAsJsonObject();
         double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
         double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
         for (int corner = 0; corner < 8; corner++) {
            double[] point = new double[3];
            for (int axis = 0; axis < 3; axis++) {
               point[axis] = element.getAsJsonArray((corner & (1 << axis)) == 0 ? "from" : "to").get(axis).getAsDouble();
            }
            if (element.has("rotation")) {
               JsonObject rotation = element.getAsJsonObject("rotation");
               var origin = rotation.getAsJsonArray("origin");
               double x = point[0] - origin.get(0).getAsDouble();
               double y = point[1] - origin.get(1).getAsDouble();
               double z = point[2] - origin.get(2).getAsDouble();
               double angle = Math.toRadians(rotation.get("angle").getAsDouble());
               double c = Math.cos(angle), s = Math.sin(angle);
               point = switch (rotation.get("axis").getAsString()) {
                  case "x" -> new double[] {x, y * c - z * s, y * s + z * c};
                  case "y" -> new double[] {x * c + z * s, y, -x * s + z * c};
                  default -> new double[] {x * c - y * s, x * s + y * c, z};
               };
               for (int axis = 0; axis < 3; axis++) {
                  point[axis] += origin.get(axis).getAsDouble();
               }
            }
            for (int axis = 0; axis < 3; axis++) {
               min[axis] = Math.min(min[axis], point[axis]);
               max[axis] = Math.max(max[axis], point[axis]);
            }
         }
         shape = Shapes.or(shape, Block.box(min[0], min[1], min[2], max[0], max[1], max[2]));
      }
      return shape;
   }

   private static JsonObject readModel(String name) throws Exception {
      String path = "/assets/" + EngineersDecorReforged.MOD_ID + "/models/block/" + name + ".json";
      try (var input = RaisedCatwalkGameTests.class.getResourceAsStream(path)) {
         if (input == null) {
            throw new IllegalStateException("Missing model " + path);
         }
         return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
      }
   }

   private static Block catwalk() {
      return ModBlocks.STEEL_CATWALK_TA.get();
   }

   private static Item light() {
      return ModBlocks.IRON_INSET_LIGHT.get().asItem();
   }

   private static ServerPlayer player(GameTestHelper helper) {
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "RaisedCatwalk"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      return player;
   }

   private static void place(GameTestHelper helper, Player player, BlockPos pos, Direction face) {
      BlockPos support = pos.relative(face.getOpposite());
      ItemStack stack = new ItemStack(catwalk());
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
         new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5)), face, support, false));
      helper.assertValueEqual(context.getClickedPos(), pos, "placement context must point at the intended empty space");
      helper.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(), "real raised-catwalk item placement must succeed");
      helper.assertTrue(helper.getLevel().getBlockState(pos).is(catwalk()), "the real item must place the raised catwalk");
   }

   private static void click(GameTestHelper helper, ServerPlayer player, InteractionHand hand, Direction face) {
      BlockPos pos = helper.absolutePos(CENTER);
      player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 3.0);
      player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(hand), hand,
         new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5)), face, pos, false));
   }

   private static void assertDrops(GameTestHelper helper, BlockState state, int lights) {
      List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(CENTER), null);
      helper.assertValueEqual(drops.stream().filter(stack -> stack.is(catwalk().asItem())).mapToInt(ItemStack::getCount).sum(), 1,
         "loot must return exactly one base catwalk");
      helper.assertValueEqual(drops.stream().filter(stack -> stack.is(light())).mapToInt(ItemStack::getCount).sum(), lights,
         "loot must return only the paid inset light, once");
      helper.assertValueEqual(drops.stream().mapToInt(ItemStack::getCount).sum(), 1 + lights, "loot must not fabricate extra items");
   }

   private static int owned(GameTestHelper helper, ServerPlayer player, Item item) {
      int count = 0;
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (stack.is(item)) {
            count += stack.getCount();
         }
      }
      return count + helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(3),
         entity -> entity.getItem().is(item)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
   }

   private static void clearAround(GameTestHelper helper, BlockPos center) {
      helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(3)).forEach(ItemEntity::discard);
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
         helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      }
   }
}
