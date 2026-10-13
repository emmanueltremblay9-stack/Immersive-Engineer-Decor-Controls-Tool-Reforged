package com.oblixorprime.engineersdecorreforged.gametest;

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
         ModBlocks.THICK_STEEL_POLE_HEAD.get(), ModBlocks.THIN_STEEL_POLE_HEAD.get()};
      int[] expected = {0, 1, 2, 1, 2};
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
         ModBlocks.TREATED_WOOD_POLE.get(), Blocks.STONE, Blocks.AIR};
      int[] expected = {1, 2, 1, 2, 0, 0, 0};
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
   public static void raised_catwalk_variant_geometry_and_saved_economic_state_match_models(GameTestHelper helper) throws Exception {
      BlockPos pos = helper.absolutePos(CENTER);
      VoxelShape deck = Block.box(0, 14, 0, 16, 16, 16);
      VoxelShape[] expected = {deck, Shapes.or(deck, Block.box(5, 0, 5, 11, 15, 11)),
         Shapes.or(deck, Block.box(6, 0, 6, 10, 15, 10)),
         Shapes.or(deck, Block.box(0, 0, 0, 16, 2, 16), Block.box(0, 0, 0, 1, 16, 1),
            Block.box(15, 0, 0, 16, 16, 1), Block.box(15, 0, 15, 16, 16, 16), Block.box(0, 0, 15, 1, 16, 16)), deck};
      String path = "/assets/" + EngineersDecorReforged.MOD_ID + "/blockstates/steel_catwalk_ta.json";
      try (var input = RaisedCatwalkGameTests.class.getResourceAsStream(path)) {
         helper.assertTrue(input != null, "raised catwalk blockstate resource must exist");
         var variants = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("variants");
         for (BlockState state : catwalk().getStateDefinition().getPossibleStates()) {
            int variant = state.getValue(RaisedCatwalkBlock.VARIANT);
            helper.assertFalse(Shapes.joinIsNotEmpty(state.getShape(helper.getLevel(), pos), expected[variant], BooleanOp.NOT_SAME),
               "outline must match the original variant's deck, pole, or frame geometry");
            helper.assertFalse(Shapes.joinIsNotEmpty(state.getCollisionShape(helper.getLevel(), pos), expected[variant], BooleanOp.NOT_SAME),
               "collision must match the selected model's structural geometry");
            String model = variants.getAsJsonObject("variant=" + variant).get("model").getAsString();
            helper.assertTrue(model.endsWith("steel_catwalk_ta" + variant + "_model"), "every numeric variant retains its original model");
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
