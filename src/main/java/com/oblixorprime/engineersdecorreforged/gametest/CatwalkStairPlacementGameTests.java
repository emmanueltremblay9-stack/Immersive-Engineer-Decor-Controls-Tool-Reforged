package com.oblixorprime.engineersdecorreforged.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.CatwalkStairsItem;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.io.InputStream;
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
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Uses the real item/game-mode transaction, not just getStateForPlacement. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class CatwalkStairPlacementGameTests {
   private static final BlockPos PLATFORM = new BlockPos(3, 5, 7);
   private static final String ASSETS = "/assets/immersive_engineer_decor_controls_tool_reforged/";

   private CatwalkStairPlacementGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void platform_edge_raycast_attaches_high_end_in_all_four_directions(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      helper.assertTrue(stairs().asItem() instanceof CatwalkStairsItem, "registered stair item must route actual placement");
      for (Block platform : platforms()) {
         for (Direction edge : Direction.Plane.HORIZONTAL) {
            for (Direction face : List.of(edge, Direction.UP, Direction.DOWN)) {
               for (Direction look : Direction.Plane.HORIZONTAL) {
                  clear(helper);
                  give(player, InteractionHand.MAIN_HAND, 8);
                  put(helper, PLATFORM, platform.defaultBlockState());
                  BlockHitResult hit = platformHit(helper, player, edge, face, platform);
                  click(helper, player, hit, look, InteractionHand.MAIN_HAND);
                  BlockPos target = firstTarget(platform, edge);
                  assertStair(helper, target, edge.getOpposite());
                  helper.assertValueEqual(helper.getBlockState(PLATFORM), platform.defaultBlockState(), "attachment preserves clicked platform");
                  helper.assertTrue(helper.getBlockState(PLATFORM.above()).isAir(), "edge attachment must not stack above the platform");
                  helper.assertValueEqual(player.getMainHandItem().getCount(), 7, "real survival attachment spends one stair");
                  double expectedJointY = PLATFORM.getY() + (platform == ModBlocks.STEEL_CATWALK.get() ? 0 : 1);
                  helper.assertTrue(Math.abs(target.getY() + 1.0 - expectedJointY) < 1.0E-6,
                     "the preserved model's high structural end joins the platform's mounting plane");
               }
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void low_end_raycast_chains_three_descending_segments_in_every_rotation(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction downhill : Direction.Plane.HORIZONTAL) {
         for (Direction face : List.of(downhill, Direction.UP, Direction.DOWN,
            downhill.getClockWise(), downhill.getCounterClockWise())) {
            clear(helper);
            give(player, InteractionHand.MAIN_HAND, 8);
            put(helper, PLATFORM, ModBlocks.STEEL_CATWALK.get().defaultBlockState());
            click(helper, player, platformHit(helper, player, downhill, downhill, ModBlocks.STEEL_CATWALK.get()),
               downhill, InteractionHand.MAIN_HAND);
            BlockPos previous = firstTarget(ModBlocks.STEEL_CATWALK.get(), downhill);
            assertStair(helper, previous, downhill.getOpposite());
            for (int index = 0; index < 2; index++) {
               BlockHitResult hit = lowEndHit(helper, player, previous, downhill, face);
               click(helper, player, hit, index == 0 ? downhill.getClockWise() : downhill, InteractionHand.MAIN_HAND);
               BlockPos next = previous.relative(downhill).below();
               assertStair(helper, next, downhill.getOpposite());
               helper.assertTrue(helper.getBlockState(previous.relative(downhill)).isAir(), "chaining must descend rather than extend level");
               assertWalkableJoin(helper, previous, next, downhill);
               previous = next;
            }
            helper.assertValueEqual(player.getMainHandItem().getCount(), 5, "three native placements cost exactly three stairs");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void high_end_continuation_and_ordinary_center_placement_remain_consistent(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction uphill : Direction.Plane.HORIZONTAL) {
         clear(helper);
         give(player, InteractionHand.MAIN_HAND, 8);
         BlockPos source = PLATFORM.below();
         put(helper, source, stairState(uphill));
         Vec3 point = Vec3.atLowerCornerOf(helper.absolutePos(source)).add(0.5 + uphill.getStepX() * 0.25,
            0.5, 0.5 + uphill.getStepZ() * 0.25);
         click(helper, player, ray(helper, player, point.add(0, 2, 0), point, source, Direction.UP),
            uphill.getClockWise(), InteractionHand.MAIN_HAND);
         assertStair(helper, source.relative(uphill).above(), uphill);
         helper.assertValueEqual(player.getMainHandItem().getCount(), 7, "uphill continuation costs one stair");
         clear(helper);
         give(player, InteractionHand.MAIN_HAND, 8);
         put(helper, PLATFORM, Blocks.STONE.defaultBlockState());
         Vec3 center = Vec3.atCenterOf(helper.absolutePos(PLATFORM));
         click(helper, player, ray(helper, player, center.add(0, 2, 0), center, PLATFORM, Direction.UP),
            uphill, InteractionHand.MAIN_HAND);
         assertStair(helper, PLATFORM.above(), uphill);
         helper.assertValueEqual(player.getMainHandItem().getCount(), 7, "ordinary center placement remains native");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void descending_stairs_keep_water_and_saved_facing_after_reload(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction downhill : Direction.Plane.HORIZONTAL) {
         clear(helper);
         give(player, InteractionHand.MAIN_HAND, 8);
         put(helper, PLATFORM, ModBlocks.STEEL_CATWALK.get().defaultBlockState());
         BlockPos first = firstTarget(ModBlocks.STEEL_CATWALK.get(), downhill);
         put(helper, first, Blocks.WATER.defaultBlockState());
         click(helper, player, platformHit(helper, player, downhill, Direction.UP, ModBlocks.STEEL_CATWALK.get()),
            downhill, InteractionHand.MAIN_HAND);
         assertStair(helper, first, downhill.getOpposite());
         helper.assertValueEqual(helper.getBlockState(first).getValue(com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.FLUID_CONTENT),
            com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.FluidContent.WATER, "new stair schema retains source water");
         BlockState original = helper.getBlockState(first);
         BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(original));
         helper.assertValueEqual(loaded, original, "saved stair rotation, water and railings round-trip unchanged");
         put(helper, first, loaded);
         BlockPos next = first.relative(downhill).below();
         put(helper, next, Blocks.WATER.defaultBlockState());
         click(helper, player, lowEndHit(helper, player, first, downhill, Direction.UP), downhill.getClockWise(), InteractionHand.MAIN_HAND);
         assertStair(helper, next, downhill.getOpposite());
         helper.assertValueEqual(helper.getBlockState(next).getValue(com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.FLUID_CONTENT),
            com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.FluidContent.WATER, "chained stair retains source water");
         helper.assertValueEqual(player.getMainHandItem().getCount(), 6, "reload and fluids must not affect item cost");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void occupied_attachment_target_never_shifts_or_consumes_items(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction downhill : Direction.Plane.HORIZONTAL) {
         for (boolean chain : new boolean[]{false, true}) {
            clear(helper);
            give(player, InteractionHand.MAIN_HAND, 8);
            BlockPos source = chain ? PLATFORM.below() : PLATFORM;
            put(helper, source, chain ? stairState(downhill.getOpposite()) : ModBlocks.STEEL_CATWALK.get().defaultBlockState());
            BlockPos target = source.relative(downhill).below();
            put(helper, target, Blocks.STONE.defaultBlockState());
            BlockState before = helper.getBlockState(source);
            BlockHitResult hit = chain ? lowEndHit(helper, player, source, downhill, Direction.UP)
               : platformHit(helper, player, downhill, Direction.UP, ModBlocks.STEEL_CATWALK.get());
            click(helper, player, hit, downhill, InteractionHand.MAIN_HAND);
            helper.assertValueEqual(helper.getBlockState(target), Blocks.STONE.defaultBlockState(), "occupied destination remains untouched");
            helper.assertValueEqual(helper.getBlockState(source), before, "failure must preserve the source");
            helper.assertValueEqual(player.getMainHandItem().getCount(), 8, "rejected placement cannot spend an item");
            helper.assertTrue(helper.getBlockState(target.above()).isAir(), "failure cannot move placement up to another cell");
            helper.assertTrue(helper.getBlockState(source.above()).isAir(), "failure cannot fall back to stacking on the source");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void blocked_vanilla_adjacent_cell_does_not_prevent_valid_descending_attachment(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction downhill : Direction.Plane.HORIZONTAL) {
         clear(helper);
         give(player, InteractionHand.MAIN_HAND, 8);
         put(helper, PLATFORM, ModBlocks.STEEL_CATWALK.get().defaultBlockState());
         // The occupied vanilla above-cell is nonreplaceable, but an edge ray passes
         // beside the fence post and still reaches the platform's real top surface.
         put(helper, PLATFORM.above(), Blocks.OAK_FENCE.defaultBlockState());
         click(helper, player, platformHit(helper, player, downhill, Direction.UP, ModBlocks.STEEL_CATWALK.get()),
            downhill, InteractionHand.MAIN_HAND);
         assertStair(helper, firstTarget(ModBlocks.STEEL_CATWALK.get(), downhill), downhill.getOpposite());
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void cancelled_stair_placement_rolls_back_destination_water_and_inventory(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction downhill : Direction.Plane.HORIZONTAL) {
         for (boolean chain : new boolean[]{false, true}) {
            clear(helper);
            give(player, InteractionHand.MAIN_HAND, 8);
            BlockPos source = chain ? PLATFORM.below() : PLATFORM;
            put(helper, source, chain ? stairState(downhill.getOpposite()) : ModBlocks.STEEL_CATWALK.get().defaultBlockState());
            BlockPos target = source.relative(downhill).below();
            put(helper, target, Blocks.WATER.defaultBlockState());
            boolean[] fired = {false};
            Consumer<BlockEvent.EntityPlaceEvent> listener = event -> {
               if (event.getEntity() == player && event.getPos().equals(helper.absolutePos(target))) {
                  fired[0] = true;
                  event.setCanceled(true);
               }
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, BlockEvent.EntityPlaceEvent.class, listener);
            try {
               BlockState before = helper.getBlockState(source);
               BlockHitResult hit = chain ? lowEndHit(helper, player, source, downhill, Direction.UP)
                  : platformHit(helper, player, downhill, Direction.UP, ModBlocks.STEEL_CATWALK.get());
               click(helper, player, hit, downhill, InteractionHand.MAIN_HAND);
               helper.assertTrue(fired[0], "normal placement event must identify the real redirected target");
               helper.assertValueEqual(helper.getBlockState(target), Blocks.WATER.defaultBlockState(), "canceled placement restores original water");
               helper.assertValueEqual(helper.getBlockState(source), before, "cancellation preserves clicked source");
               helper.assertValueEqual(player.getMainHandItem().getCount(), 8, "cancellation restores the original stack");
            } finally {
               NeoForge.EVENT_BUS.unregister(listener);
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void interaction_cancellations_stop_stair_attachment_before_mutation(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (boolean early : new boolean[]{false, true}) {
         clear(helper);
         give(player, InteractionHand.MAIN_HAND, 8);
         put(helper, PLATFORM, ModBlocks.STEEL_CATWALK.get().defaultBlockState());
         Consumer<PlayerInteractEvent.RightClickBlock> rightClick = event -> {
            if (event.getEntity() == player) {
               event.setCanceled(true);
               event.setCancellationResult(InteractionResult.FAIL);
            }
         };
         Consumer<UseItemOnBlockEvent> itemPhase = event -> {
            if (event.getPlayer() == player && event.getUsePhase() == UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK) {
               event.cancelWithResult(ItemInteractionResult.FAIL);
            }
         };
         if (early) {
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerInteractEvent.RightClickBlock.class, rightClick);
         } else {
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, UseItemOnBlockEvent.class, itemPhase);
         }
         try {
            click(helper, player, platformHit(helper, player, Direction.SOUTH, Direction.UP, ModBlocks.STEEL_CATWALK.get()),
               Direction.SOUTH, InteractionHand.MAIN_HAND);
            helper.assertTrue(helper.getBlockState(PLATFORM.south().below()).isAir(), "canceled interaction cannot place a stair");
            helper.assertValueEqual(player.getMainHandItem().getCount(), 8, "canceled interaction preserves inventory");
         } finally {
            NeoForge.EVENT_BUS.unregister(early ? rightClick : itemPhase);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stair_attachment_respects_target_permission_adventure_spectator_and_offhand_cost(GameTestHelper helper) {
      BlockPos denied = helper.absolutePos(PLATFORM.south().below());
      ServerPlayer restricted = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "DeniedStair")) {
         @Override
         public boolean mayUseItemAt(BlockPos pos, Direction face, ItemStack stack) {
            return !pos.equals(denied) && super.mayUseItemAt(pos, face, stack);
         }
      };
      restricted.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      clear(helper);
      give(restricted, InteractionHand.MAIN_HAND, 8);
      put(helper, PLATFORM, ModBlocks.STEEL_CATWALK.get().defaultBlockState());
      click(helper, restricted, platformHit(helper, restricted, Direction.SOUTH, Direction.UP, ModBlocks.STEEL_CATWALK.get()),
         Direction.SOUTH, InteractionHand.MAIN_HAND);
      helper.assertTrue(helper.getBlockState(PLATFORM.south().below()).isAir(), "permission must be checked at redirected target");
      helper.assertValueEqual(restricted.getMainHandItem().getCount(), 8, "target denial preserves stack");
      ServerPlayer player = player(helper);
      for (GameType mode : List.of(GameType.ADVENTURE, GameType.SPECTATOR, GameType.CREATIVE, GameType.SURVIVAL)) {
         clear(helper);
         player.gameMode.changeGameModeForPlayer(mode);
         give(player, InteractionHand.OFF_HAND, 8);
         put(helper, PLATFORM, ModBlocks.STEEL_CATWALK.get().defaultBlockState());
         click(helper, player, platformHit(helper, player, Direction.SOUTH, Direction.UP, ModBlocks.STEEL_CATWALK.get()),
            Direction.SOUTH, InteractionHand.OFF_HAND);
         if (mode == GameType.ADVENTURE || mode == GameType.SPECTATOR) {
            helper.assertTrue(helper.getBlockState(PLATFORM.south().below()).isAir(), mode + " must not place unpermitted stairs");
         } else {
            assertStair(helper, PLATFORM.south().below(), Direction.NORTH);
         }
         helper.assertValueEqual(player.getOffhandItem().getCount(), mode == GameType.SURVIVAL ? 7 : 8,
            "only successful survival placement charges the interacting hand");
         helper.assertTrue(player.getMainHandItem().isEmpty(), "offhand placement must not charge or populate the main hand");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void preserved_stair_models_and_collision_use_the_same_uphill_direction(GameTestHelper helper) {
      JsonObject model = json("models/block/furniture/steel_catwalk_stairs_model.json");
      JsonObject variants = json("blockstates/steel_catwalk_stairs.json").getAsJsonObject("variants");
      helper.assertTrue(model.getAsJsonArray("elements").asList().stream().anyMatch(element -> {
         var box = element.getAsJsonObject();
         var from = box.getAsJsonArray("from");
         var to = box.getAsJsonArray("to");
         return from.get(1).getAsDouble() == 15 && to.get(1).getAsDouble() == 17 && from.get(2).getAsDouble() == -1;
      }), "preserved north high-end connector straddles the top mounting plane");
      helper.assertTrue(model.getAsJsonArray("elements").asList().stream().anyMatch(element -> {
         var box = element.getAsJsonObject();
         var from = box.getAsJsonArray("from");
         var to = box.getAsJsonArray("to");
         return from.get(1).getAsDouble() == -1 && to.get(1).getAsDouble() == 1 && to.get(2).getAsDouble() == 17;
      }), "preserved south low-end connector straddles the bottom mounting plane");
      for (Direction facing : Direction.Plane.HORIZONTAL) {
          String key = "facing=" + facing.getName() + ",left_railing=false,right_railing=false";
         JsonObject variant = variants.getAsJsonObject(key);
         int rotation = variant.has("y") ? variant.get("y").getAsInt() : 0;
         int expectedRotation = switch (facing) { case NORTH -> 0; case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> -1; };
         helper.assertValueEqual(rotation, expectedRotation, "model high end must rotate with the saved facing");
         put(helper, PLATFORM, stairState(facing));
         helper.assertTrue(Math.abs(treadTop(helper, PLATFORM, facing) - (PLATFORM.getY() + 12.0 / 16.0)) < 1.0E-6,
            "uphill collision tread must match the preserved model's 12/16 top");
         helper.assertTrue(Math.abs(treadTop(helper, PLATFORM, facing.getOpposite()) - (PLATFORM.getY() + 4.0 / 16.0)) < 1.0E-6,
            "downhill collision tread must match the preserved model's 4/16 top");
      }
      helper.succeed();
   }

   private static void assertWalkableJoin(GameTestHelper helper, BlockPos upper, BlockPos lower, Direction downhill) {
      double upperLow = treadTop(helper, upper, downhill);
      double lowerHigh = treadTop(helper, lower, downhill.getOpposite());
      helper.assertTrue(Math.abs(upperLow - lowerHigh - 0.5) < 1.0E-6,
         "consecutive preserved stair treads must stay a walkable half-block apart");
      helper.assertValueEqual(lower.getY() + 1, upper.getY(), "next high structural endpoint meets previous low structural endpoint");
   }

   private static double treadTop(GameTestHelper helper, BlockPos pos, Direction end) {
      BlockState state = helper.getBlockState(pos);
      double x = 0.5 + end.getStepX() * 0.25;
      double z = 0.5 + end.getStepZ() * 0.25;
      return state.getCollisionShape(helper.getLevel(), helper.absolutePos(pos)).toAabbs().stream()
         .filter(box -> x > box.minX && x < box.maxX && z > box.minZ && z < box.maxZ)
         .mapToDouble(box -> box.maxY).max().orElseThrow() + pos.getY();
   }

   private static BlockHitResult platformHit(GameTestHelper helper, ServerPlayer player, Direction edge, Direction face, Block platform) {
      BlockPos absolute = helper.absolutePos(PLATFORM);
      double bottom = platform == ModBlocks.STEEL_CATWALK.get() ? 0 : 14.0 / 16.0;
      double top = platform == ModBlocks.STEEL_CATWALK.get() ? 3.0 / 16.0 : 1;
      Vec3 point = Vec3.atLowerCornerOf(absolute).add(0.5 + edge.getStepX() * 0.4,
         (bottom + top) / 2, 0.5 + edge.getStepZ() * 0.4);
      Vec3 direction = Vec3.atLowerCornerOf(face.getNormal());
      return ray(helper, player, point.add(direction.scale(2)), point, PLATFORM, face);
   }

   private static BlockHitResult lowEndHit(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction downhill, Direction face) {
      Vec3 point = Vec3.atLowerCornerOf(helper.absolutePos(pos)).add(0.5 + downhill.getStepX() * 0.25,
         2.0 / 16.0, 0.5 + downhill.getStepZ() * 0.25);
      Vec3 direction = Vec3.atLowerCornerOf(face.getNormal());
      // Keep the eye outside the selected face but inside this 8-block-wide template.
      // A 1.5-block westward ray from the second descending segment starts beyond
      // the test bounds and hits the enclosure wall before reaching the stair.
      return ray(helper, player, point.add(direction.scale(0.9)), point, pos, face);
   }

   private static BlockHitResult ray(GameTestHelper helper, ServerPlayer player, Vec3 from, Vec3 to, BlockPos expected, Direction face) {
      BlockHitResult hit = helper.getLevel().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
      helper.assertValueEqual(hit.getType(), HitResult.Type.BLOCK, "placement needs an actual selectable surface");
      helper.assertValueEqual(hit.getBlockPos(), helper.absolutePos(expected), "raycast must hit intended source");
      helper.assertValueEqual(hit.getDirection(), face, "raycast must exercise expected clicked face");
      return hit;
   }

   private static void click(GameTestHelper helper, ServerPlayer player, BlockHitResult hit, Direction look, InteractionHand hand) {
      BlockPos pos = hit.getBlockPos();
      player.setPos(pos.getX() + 2.5, pos.getY() + 1.0, pos.getZ() + 2.5);
      player.setYRot(look.toYRot());
      player.setXRot(hit.getDirection() == Direction.UP ? 85.0F : 0.0F);
      player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(hand), hand, hit);
   }

   private static void assertStair(GameTestHelper helper, BlockPos pos, Direction facing) {
      helper.assertTrue(helper.getBlockState(pos).is(stairs()), "real registered item must place a stair in selected cell " + pos);
      helper.assertBlockProperty(pos, PortedBlocks.HORIZONTAL_FACING, facing);
   }

   private static BlockPos firstTarget(Block platform, Direction edge) {
      BlockPos target = PLATFORM.relative(edge);
      return platform == ModBlocks.STEEL_CATWALK.get() ? target.below() : target;
   }

   private static BlockState stairState(Direction facing) {
      return stairs().defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, facing);
   }

   private static Block stairs() {
      return ModBlocks.STEEL_CATWALK_STAIRS.get();
   }

   private static List<Block> platforms() {
      return List.of(ModBlocks.STEEL_CATWALK.get(), ModBlocks.STEEL_CATWALK_TA.get(), ModBlocks.STEEL_FLOOR_GRATING.get());
   }

   private static ServerPlayer player(GameTestHelper helper) {
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "StairTest"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      return player;
   }

   private static void give(ServerPlayer player, InteractionHand hand, int count) {
      player.getInventory().clearContent();
      player.setItemInHand(hand, new ItemStack(stairs(), count));
   }

   private static void put(GameTestHelper helper, BlockPos pos, BlockState state) {
      helper.getLevel().setBlock(helper.absolutePos(pos), state, Block.UPDATE_CLIENTS);
   }

   private static void clear(GameTestHelper helper) {
      for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 0, 3), new BlockPos(7, 7, 11))) {
         put(helper, pos, Blocks.AIR.defaultBlockState());
      }
   }

   private static JsonObject json(String path) {
      try (InputStream stream = CatwalkStairPlacementGameTests.class.getResourceAsStream(ASSETS + path)) {
         if (stream == null) {
            throw new IllegalStateException("Missing stair resource " + path);
         }
         try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
         }
      } catch (java.io.IOException exception) {
         throw new IllegalStateException(exception);
      }
   }
}
