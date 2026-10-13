package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.ChimneyBlock;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChimneyGameTests {
   private static final BlockPos CENTER = new BlockPos(1, 1, 1);

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void cap_clicks_cycle_intensity_including_off_and_preserve_held_item(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos pos = helper.absolutePos(CENTER);
      var player = helper.makeMockPlayer(GameType.CREATIVE);
      var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
      level.setBlock(pos, ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get().defaultBlockState(), 2);
      for (int click = 1; click <= 32; click++) {
         level.getBlockState(pos).useWithoutItem(level, player, hit);
         helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.POWER), click & 15,
            "one click advances exactly once, 15 wraps to off");
      }
      ItemStack held = new ItemStack(Items.STICK, 3);
      player.setItemInHand(InteractionHand.MAIN_HAND, held);
      level.getBlockState(pos).useItemOn(held, level, player, InteractionHand.MAIN_HAND, hit);
      helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.POWER), 1, "held-item interaction also adjusts smoke");
      helper.assertValueEqual(held.getCount(), 3, "cosmetic adjustment consumes nothing");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void cap_redstone_tracks_all_strengths_and_signal_removal_turns_smoke_off(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos pos = helper.absolutePos(CENTER);
      BlockPos source = pos.east();
      level.setBlock(source.below(), Blocks.STONE.defaultBlockState(), 2);
      level.setBlock(pos, ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get().defaultBlockState(), 2);
      for (int power = 0; power <= 15; power++) {
         level.setBlock(source, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState()
            .setValue(BlockStateProperties.POWER, power), 2);
         helper.assertValueEqual(level.getBestNeighborSignal(pos), power, "analog source fixture");
         level.updateNeighborsAt(source, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
         helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.POWER), power,
            "chimney must track incoming signal exactly");
      }
      level.setBlock(source, Blocks.AIR.defaultBlockState(), 3);
      helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.POWER), 0, "removing signal turns smoke off");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void cap_placement_uses_existing_analog_signal(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos pos = helper.absolutePos(CENTER);
      var player = helper.makeMockPlayer(GameType.CREATIVE);
      level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
      level.setBlock(pos.east().below(), Blocks.STONE.defaultBlockState(), 2);
      for (int power = 0; power <= 15; power++) {
         level.setBlock(pos.east(), Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState()
            .setValue(BlockStateProperties.POWER, power), 2);
         var context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND,
            new ItemStack(ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get()),
            new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0.5, 0, 0.5), Direction.UP, pos.below(), false));
         helper.assertValueEqual(ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get().getStateForPlacement(context).getValue(ChimneyBlock.POWER),
            power == 0 ? 5 : power, "existing analog signal controls initial intensity");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void intensity_survives_saved_state_reload_and_old_caps_remain_valid(GameTestHelper helper) {
      var block = ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get();
      var old = NbtUtils.writeBlockState(block.defaultBlockState());
      old.remove("Properties");
      BlockState loadedOld = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), old);
      helper.assertTrue(loadedOld.is(block), "old cap registry ID must still load");
      helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow(), loadedOld,
         "property-less cap also loads through the block-state codec");
      helper.assertValueEqual(loadedOld.getValue(ChimneyBlock.POWER), 0, "old property-less cap stays silent until adjusted");
      for (int power = 0; power <= 15; power++) {
         BlockState original = block.defaultBlockState().setValue(ChimneyBlock.POWER, power);
         var saved = NbtUtils.writeBlockState(original);
         var loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
         helper.assertValueEqual(loaded, original, "all intensities survive NBT reload");
         helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow(), original, "codec reload preserves intensity");
         helper.setBlock(CENTER, loaded);
         helper.assertValueEqual(helper.getBlockState(CENTER), original, "reloaded block retains its saved intensity");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void smoke_emission_is_bounded_and_zero_intensity_is_silent(GameTestHelper helper) {
      for (int power = 0; power <= 15; power++) {
         for (int seed = 0; seed < 64; seed++) {
            int[] count = {0};
            ChimneyBlock.emitSmoke(power, BlockPos.ZERO, RandomSource.create(seed), (particle, x, y, z, dx, dy, dz) -> {
               count[0]++;
               helper.assertTrue(particle == ParticleTypes.SMOKE || particle == ParticleTypes.LARGE_SMOKE
                  || particle == ParticleTypes.CAMPFIRE_COSY_SMOKE, "historical vanilla smoke types only");
               helper.assertTrue(x >= 0.5 && x < 0.7 && y >= 0.9 && y < 1.0 && z >= 0.5 && z < 0.7,
                  "smoke originates at the chimney outlet");
               helper.assertTrue(dy >= 0.05 && dy < 0.15 && dx >= -0.02 && dx < 0.02 && dz >= -0.02 && dz < 0.02,
                  "smoke rises with bounded drift");
            });
            helper.assertTrue(power == 0 ? count[0] == 0 : count[0] >= 1 && count[0] <= 1 + 9 * power / 15,
               "off emits nothing; intensity controls a bounded emission count");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void trunk_rim_can_be_clicked_to_place_cap_flush_in_all_directions(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos pos = helper.absolutePos(CENTER);
      var player = helper.makeMockPlayer(GameType.CREATIVE);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
         BlockState trunk = ModBlocks.DARK_SHINGLE_ROOF_CHIMNEYTRUNK.get().defaultBlockState().setValue(StairBlock.FACING, facing);
         level.setBlock(pos, trunk, 2);
         // The low-side rim is visibly at y=16 but the old vanilla stair only reaches y=8 here.
         Vec3 rim = rotate(new Vec3(4.0 / 16, 1, 4.0 / 16), facing);
         Vec3 start = Vec3.atLowerCornerOf(pos).add(rim.x, 2, rim.z);
         Vec3 end = Vec3.atLowerCornerOf(pos).add(rim.x, 0, rim.z);
         BlockHitResult hit = trunk.getShape(level, pos).clip(start, end, pos);
         helper.assertTrue(hit != null && hit.getDirection() == Direction.UP, "visible upper rim is selectable from above");
         helper.assertTrue(Math.abs(hit.getLocation().y - (pos.getY() + 1)) < 1e-6, "ray must hit the actual rim, not an invisible stair step");
         ItemStack cap = new ItemStack(ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get());
         player.setItemInHand(InteractionHand.MAIN_HAND, cap);
         var result = ((BlockItem)cap.getItem()).place(new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, cap, hit));
         helper.assertTrue(result.consumesAction(), "cap placement on rim succeeds");
         helper.assertTrue(level.getBlockState(pos.above()).is(ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get()), "cap goes immediately above trunk");
         helper.assertValueEqual(level.getBlockState(pos.above()).getValue(ChimneyBlock.POWER), 5, "new unpowered cap starts with historical intensity five");
         helper.assertTrue(trunk.getShape(level, pos).equals(trunk.getCollisionShape(level, pos)), "selection and collision agree");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void trunk_placement_stays_upright_and_neighbors_preserve_fitting(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos pos = helper.absolutePos(CENTER);
      var player = helper.makeMockPlayer(GameType.CREATIVE);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         player.setYRot(facing.toYRot());
         for (Direction clickedFace : Direction.values()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            BlockPos support = pos.relative(clickedFace.getOpposite());
            level.setBlock(support, Blocks.STONE.defaultBlockState(), 2);
            var context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND,
               new ItemStack(ModBlocks.DARK_SHINGLE_ROOF_CHIMNEYTRUNK.get()),
               new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(clickedFace.getNormal()).scale(0.5)), clickedFace, support, false));
            BlockState placed = ModBlocks.DARK_SHINGLE_ROOF_CHIMNEYTRUNK.get().getStateForPlacement(context);
            helper.assertTrue(placed != null, "placement state exists");
            helper.assertValueEqual(placed.getValue(StairBlock.FACING), facing, "placement follows player direction");
            helper.assertValueEqual(placed.getValue(StairBlock.HALF), Half.BOTTOM, "trunk always places upright");
            helper.assertValueEqual(placed.getValue(StairBlock.SHAPE), StairsShape.STRAIGHT, "trunk keeps its fitting model");
            level.setBlock(support, Blocks.AIR.defaultBlockState(), 2);
         }
         for (Direction side : new Direction[] {facing, facing.getOpposite()}) {
            for (Direction clear : Direction.Plane.HORIZONTAL) level.setBlock(pos.relative(clear), Blocks.AIR.defaultBlockState(), 2);
            BlockState trunk = ModBlocks.DARK_SHINGLE_ROOF_CHIMNEYTRUNK.get().defaultBlockState().setValue(StairBlock.FACING, facing);
            level.setBlock(pos, trunk, 2);
            BlockState neighbor = Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing.getClockWise());
            level.setBlock(pos.relative(side), neighbor, 3);
            BlockState ordinaryStair = Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing)
               .updateShape(side, neighbor, level, pos, pos.relative(side));
            helper.assertTrue(ordinaryStair.getValue(StairBlock.SHAPE) != StairsShape.STRAIGHT,
               "regression fixture must turn an ordinary stair into an inner or outer corner");
            helper.assertValueEqual(level.getBlockState(pos).getValue(StairBlock.SHAPE), StairsShape.STRAIGHT,
               "real neighbor update must preserve the chimney fitting");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void all_legacy_trunk_variants_and_waterlogging_survive_reload(GameTestHelper helper) {
      for (BlockState original : ModBlocks.DARK_SHINGLE_ROOF_CHIMNEYTRUNK.get().getStateDefinition().getPossibleStates()) {
         var saved = NbtUtils.writeBlockState(original);
         var loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
         helper.assertValueEqual(loaded, original, "all old facing/half/shape/waterlogged combinations remain valid");
         var updated = loaded.updateShape(Direction.NORTH, Blocks.STONE_STAIRS.defaultBlockState(), helper.getLevel(),
            helper.absolutePos(CENTER), helper.absolutePos(CENTER.north()));
         helper.assertValueEqual(updated, original, "neighbor updates must not silently migrate old states");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void straight_trunk_shape_matches_model_for_every_rotation_and_half(GameTestHelper helper) throws Exception {
      String resource = "/assets/" + EngineersDecorReforged.MOD_ID + "/models/block/roof/dark_shingle_roof_chimneytrunk_model.json";
      try (var input = ChimneyGameTests.class.getResourceAsStream(resource)) {
         helper.assertTrue(input != null, "preserved model exists");
         var elements = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
            .getAsJsonObject().getAsJsonArray("elements");
         for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (Half half : Half.values()) {
               VoxelShape expected = Shapes.empty();
               for (var entry : elements) {
                  var element = entry.getAsJsonObject();
                  var from = element.getAsJsonArray("from");
                  var to = element.getAsJsonArray("to");
                  Vec3 a = new Vec3(from.get(0).getAsDouble() / 16, from.get(1).getAsDouble() / 16, from.get(2).getAsDouble() / 16);
                  Vec3 b = new Vec3(to.get(0).getAsDouble() / 16, to.get(1).getAsDouble() / 16, to.get(2).getAsDouble() / 16);
                  if (half == Half.TOP) {
                     a = new Vec3(a.x, 1 - a.y, 1 - a.z);
                     b = new Vec3(b.x, 1 - b.y, 1 - b.z);
                  }
                  a = rotate(a, facing);
                  b = rotate(b, facing);
                  expected = Shapes.or(expected, Shapes.box(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z),
                     Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z)));
               }
               BlockState state = ModBlocks.DARK_SHINGLE_ROOF_CHIMNEYTRUNK.get().defaultBlockState()
                  .setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
               helper.assertFalse(Shapes.joinIsNotEmpty(expected, state.getShape(helper.getLevel(), helper.absolutePos(CENTER)), BooleanOp.NOT_SAME),
                  "all roof and rim geometry follows the preserved transformed model: " + facing + "/" + half);
            }
         }
      }
      helper.succeed();
   }


   @GameTest(template = "empty", timeoutTicks = 40)
   public static void multiple_trunks_stack_on_their_rims_and_join_a_final_cap(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos base = helper.absolutePos(CENTER);
      var player = helper.makeMockPlayer(GameType.CREATIVE);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         player.setYRot(facing.toYRot());
         for (int height : new int[] {2, 3, 5}) {
            for (int y = 0; y <= 6; y++) level.setBlock(base.above(y), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(base, ModBlocks.DARK_SHINGLE_ROOF_CHIMNEYTRUNK.get().defaultBlockState()
               .setValue(StairBlock.FACING, facing), 2);
            Vec3 rim = rotate(new Vec3(4.5 / 16, 1, 4.0 / 16), facing);
            for (int y = 1; y <= height; y++) {
               BlockPos support = base.above(y - 1);
               BlockState lower = level.getBlockState(support);
               BlockHitResult hit = lower.getShape(level, support).clip(
                  Vec3.atLowerCornerOf(support).add(rim.x, 2, rim.z),
                  Vec3.atLowerCornerOf(support).add(rim.x, 0, rim.z), support);
               helper.assertTrue(hit != null && hit.getDirection() == Direction.UP, "each upper rim can be targeted");
               ItemStack item = new ItemStack(y == height ? ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get()
                  : ModBlocks.DARK_SHINGLE_ROOF_CHIMNEYTRUNK.get());
               player.setItemInHand(InteractionHand.MAIN_HAND, item);
               helper.assertTrue(((BlockItem)item.getItem()).place(
                  new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, item, hit)).consumesAction(),
                  "each trunk and the final cap place directly on the preceding rim");
            }
            VoxelShape combined = Shapes.empty();
            for (int y = 0; y < height; y++) {
               BlockState trunk = level.getBlockState(base.above(y));
               helper.assertTrue(trunk.is(ModBlocks.DARK_SHINGLE_ROOF_CHIMNEYTRUNK.get()), "stack retains each trunk");
               helper.assertValueEqual(trunk.getValue(StairBlock.FACING), facing, "stack orientations agree");
               helper.assertValueEqual(trunk.getValue(StairBlock.HALF), Half.BOTTOM, "stack remains upright");
               helper.assertValueEqual(trunk.getValue(StairBlock.SHAPE), StairsShape.STRAIGHT, "stack retains fitting models");
               helper.assertFalse(trunk.hasProperty(ChimneyBlock.POWER), "trunk is not a smoke emitter");
               combined = Shapes.or(combined, trunk.getCollisionShape(level, base.above(y)).move(0, y, 0));
            }
            BlockState cap = level.getBlockState(base.above(height));
            helper.assertTrue(cap.is(ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get()), "one final cap closes the assembly");
            helper.assertValueEqual(cap.getValue(ChimneyBlock.POWER), 5, "new final cap starts at intensity five");
            combined = Shapes.or(combined, cap.getCollisionShape(level, base.above(height)).move(0, height, 0));
            VoxelShape joinedWall = Shapes.box(rim.x - 0.001, 0, rim.z - 0.001,
               rim.x + 0.001, height + 6.0 / 16, rim.z + 0.001);
            helper.assertFalse(Shapes.joinIsNotEmpty(joinedWall, combined, BooleanOp.ONLY_FIRST),
               "preserved wall geometry touches continuously across every trunk and cap boundary");
         }
      }
      helper.succeed();
   }


   @GameTest(template = "empty", timeoutTicks = 40)
   public static void chimney_heads_stack_without_gaps_and_emit_only_at_the_top(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos base = helper.absolutePos(CENTER);
      var player = helper.makeMockPlayer(GameType.CREATIVE);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         player.setYRot(facing.toYRot());
         for (int height : new int[] {2, 3, 5}) {
            for (int y = 0; y <= 6; y++) level.setBlock(base.above(y), Blocks.AIR.defaultBlockState(), 2);
            BlockState trunk = ModBlocks.DARK_SHINGLE_ROOF_CHIMNEYTRUNK.get().defaultBlockState().setValue(StairBlock.FACING, facing);
            level.setBlock(base.below(), trunk, 2);
            for (int y = 0; y < height; y++) {
               BlockPos support = base.above(y - 1);
               BlockState lower = level.getBlockState(support);
               Vec3 rim = rotate(new Vec3(4.5 / 16, 1, 4.0 / 16), facing);
               BlockHitResult hit = lower.getShape(level, support).clip(
                  Vec3.atLowerCornerOf(support).add(rim.x, 2, rim.z),
                  Vec3.atLowerCornerOf(support).add(rim.x, 0, rim.z), support);
               helper.assertTrue(hit != null && hit.getDirection() == Direction.UP, "actual rim remains targetable while stacking heads");
               ItemStack item = new ItemStack(ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get());
               player.setItemInHand(InteractionHand.MAIN_HAND, item);
               helper.assertTrue(((BlockItem)item.getItem()).place(
                  new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, item, hit)).consumesAction(),
                  "next head places directly above the existing short head");
            }
            VoxelShape combined = Shapes.empty();
            for (int y = 0; y < height; y++) {
               BlockPos pos = base.above(y);
               BlockState state = level.getBlockState(pos);
               helper.assertTrue(state.is(ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get()), "all stack elements use the same existing chimney item");
               helper.assertValueEqual(state.getValue(ChimneyBlock.STACKED), y < height - 1, "only lower heads extend");
               // All heads are deliberately enabled: suppression must be structural, not merely zero intensity.
               state = state.setValue(ChimneyBlock.POWER, 7);
               level.setBlock(pos, state, 2);
               helper.assertValueEqual(ChimneyBlock.shouldEmitSmoke(state, level, pos), y == height - 1,
                  "only the uppermost enabled head emits smoke");
               combined = Shapes.or(combined, state.getCollisionShape(level, pos).move(0, y, 0));
               var saved = NbtUtils.writeBlockState(state);
               helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved), state,
                  "stacked flag and per-head intensity survive NBT reload");
            }
            VoxelShape joinedWall = Shapes.box(0.249, 0, 0.249, 0.251, height - 1 + 6.0 / 16, 0.251);
            helper.assertFalse(Shapes.joinIsNotEmpty(joinedWall, combined, BooleanOp.ONLY_FIRST),
               "adaptive walls fill every former 10/16-block vertical gap");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void head_extension_updates_when_upper_heads_are_added_or_removed(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos pos = helper.absolutePos(CENTER);
      var cap = ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get();
      level.setBlock(pos, cap.defaultBlockState().setValue(ChimneyBlock.POWER, 6), 3);
      helper.assertFalse(level.getBlockState(pos).getValue(ChimneyBlock.STACKED), "isolated head is short");
      level.setBlock(pos.above(), cap.defaultBlockState().setValue(ChimneyBlock.POWER, 8), 3);
      helper.assertTrue(level.getBlockState(pos).getValue(ChimneyBlock.STACKED), "placing an upper head extends lower walls");
      helper.assertFalse(ChimneyBlock.shouldEmitSmoke(level.getBlockState(pos), level, pos), "covered head is silent");
      level.setBlock(pos.above(2), cap.defaultBlockState().setValue(ChimneyBlock.POWER, 10), 3);
      helper.assertTrue(level.getBlockState(pos.above()).getValue(ChimneyBlock.STACKED), "middle head extends too");
      level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
      helper.assertFalse(level.getBlockState(pos).getValue(ChimneyBlock.STACKED), "a gap breaks the chain even if another head remains higher up");
      level.setBlock(pos, level.getBlockState(pos).setValue(ChimneyBlock.POWER, 6), 2);
      helper.assertTrue(ChimneyBlock.shouldEmitSmoke(level.getBlockState(pos), level, pos), "enabled lower chain has its own top after separation");
      level.setBlock(pos.above(), cap.defaultBlockState().setValue(ChimneyBlock.POWER, 8), 3);
      helper.assertTrue(level.getBlockState(pos).getValue(ChimneyBlock.STACKED), "reconnecting the middle restores extension");
      helper.assertFalse(ChimneyBlock.shouldEmitSmoke(level.getBlockState(pos), level, pos), "reconnected lower head stops emitting");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void ordinary_blocks_do_not_form_a_chimney_chain(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos pos = helper.absolutePos(CENTER);
      var cap = ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get();
      level.setBlock(pos, cap.defaultBlockState(), 3);
      for (var other : new net.minecraft.world.level.block.Block[] {Blocks.STONE, Blocks.GLASS, Blocks.OAK_SLAB}) {
         level.setBlock(pos.above(), other.defaultBlockState(), 3);
         BlockState state = level.getBlockState(pos).setValue(ChimneyBlock.POWER, 5);
         level.setBlock(pos, state, 2);
         helper.assertFalse(state.getValue(ChimneyBlock.STACKED), "ordinary blocks do not extend a chimney head");
         helper.assertTrue(ChimneyBlock.shouldEmitSmoke(state, level, pos), "ordinary obstructions do not introduce a new smoke rule");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void adaptive_head_models_match_outline_collision_and_save_state(GameTestHelper helper) throws Exception {
      var cap = ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get();
      for (boolean stacked : new boolean[] {false, true}) {
         String suffix = stacked ? "extended_model" : "model";
         String resource = "/assets/" + EngineersDecorReforged.MOD_ID + "/models/block/roof/dark_shingle_roof_chimney_" + suffix + ".json";
         VoxelShape expected = Shapes.empty();
         try (var input = ChimneyGameTests.class.getResourceAsStream(resource)) {
            helper.assertTrue(input != null, "adaptive model exists");
            var elements = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
               .getAsJsonObject().getAsJsonArray("elements");
            for (var entry : elements) {
               var a = entry.getAsJsonObject().getAsJsonArray("from");
               var b = entry.getAsJsonObject().getAsJsonArray("to");
               expected = Shapes.or(expected, Shapes.box(a.get(0).getAsDouble() / 16, a.get(1).getAsDouble() / 16, a.get(2).getAsDouble() / 16,
                  b.get(0).getAsDouble() / 16, b.get(1).getAsDouble() / 16, b.get(2).getAsDouble() / 16));
            }
         }
         for (int power = 0; power <= 15; power++) {
            BlockState state = cap.defaultBlockState().setValue(ChimneyBlock.STACKED, stacked).setValue(ChimneyBlock.POWER, power);
            var saved = NbtUtils.writeBlockState(state);
            helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow(), state, "adaptive geometry and independent intensity survive codec reload");
            helper.assertFalse(Shapes.joinIsNotEmpty(expected, state.getShape(helper.getLevel(), helper.absolutePos(CENTER)), BooleanOp.NOT_SAME),
               "selection follows the actual adaptive model");
            helper.assertFalse(Shapes.joinIsNotEmpty(expected, state.getCollisionShape(helper.getLevel(), helper.absolutePos(CENTER)), BooleanOp.NOT_SAME),
               "collision follows the actual adaptive model");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void stacked_top_retains_click_and_redstone_controls(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos lower = helper.absolutePos(CENTER);
      BlockPos top = lower.above();
      var cap = ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get();
      level.setBlock(lower, cap.defaultBlockState().setValue(ChimneyBlock.POWER, 5), 3);
      level.setBlock(top, cap.defaultBlockState().setValue(ChimneyBlock.POWER, 5), 3);
      var player = helper.makeMockPlayer(GameType.CREATIVE);
      var hit = new BlockHitResult(Vec3.atCenterOf(top), Direction.UP, top, false);
      level.getBlockState(top).useWithoutItem(level, player, hit);
      helper.assertValueEqual(level.getBlockState(top).getValue(ChimneyBlock.POWER), 6, "top remains adjustable by click");
      helper.assertTrue(ChimneyBlock.shouldEmitSmoke(level.getBlockState(top), level, top), "enabled top emits");
      level.setBlock(top.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
      helper.assertValueEqual(level.getBlockState(top).getValue(ChimneyBlock.POWER), 15, "top still responds to redstone");
      level.setBlock(top.east(), Blocks.AIR.defaultBlockState(), 3);
      helper.assertValueEqual(level.getBlockState(top).getValue(ChimneyBlock.POWER), 0, "removed signal switches top off");
      helper.assertFalse(ChimneyBlock.shouldEmitSmoke(level.getBlockState(top), level, top), "off top emits nothing");
      helper.assertFalse(ChimneyBlock.shouldEmitSmoke(level.getBlockState(lower), level, lower), "lower head does not take over when top is off");
      helper.succeed();
   }


   @GameTest(template = "empty", timeoutTicks = 40)
   public static void stacking_preserves_manual_settings_but_real_redstone_changes_take_control(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos pos = helper.absolutePos(CENTER);
      BlockPos source = pos.east();
      var cap = ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get();
      level.setBlock(pos, cap.defaultBlockState().setValue(ChimneyBlock.POWER, 9), 3);
      for (int cycle = 0; cycle < 2; cycle++) {
         level.setBlock(pos.above(), cap.defaultBlockState().setValue(ChimneyBlock.POWER, 5), 3);
         helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.POWER), 9, "adding a head does not erase the lower head's manual intensity");
         level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
         helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.POWER), 9, "uncovering restores emission at the retained intensity");
         helper.assertTrue(ChimneyBlock.shouldEmitSmoke(level.getBlockState(pos), level, pos), "newly exposed top resumes smoke");
      }
      level.setBlock(source.below(), Blocks.STONE.defaultBlockState(), 2);
      level.setBlock(source, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState().setValue(BlockStateProperties.POWER, 12), 3);
      helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.POWER), 12, "a true incoming signal change takes control");
      helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.INPUT_POWER), 12, "last input is tracked independently");
      level.setBlock(pos.above(), cap.defaultBlockState(), 3);
      level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
      helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.POWER), 12, "structural changes do not modify an active redstone setting");
      var player = helper.makeMockPlayer(GameType.CREATIVE);
      level.getBlockState(pos).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
      helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.POWER), 13, "explicit click can still adjust intensity as before");
      BlockState beforeSave = level.getBlockState(pos);
      var saved = NbtUtils.writeBlockState(beforeSave);
      var loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
      helper.assertValueEqual(loaded, beforeSave, "manual intensity and observed signal both survive reload");
      level.setBlock(pos, loaded, 2);
      level.updateNeighborsAt(source, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
      helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.POWER), 13, "unchanged signal after reload is not a new redstone command");
      level.setBlock(source, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState().setValue(BlockStateProperties.POWER, 4), 3);
      helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.POWER), 4, "changed analog input updates intensity");
      level.setBlock(source, Blocks.AIR.defaultBlockState(), 3);
      helper.assertValueEqual(level.getBlockState(pos).getValue(ChimneyBlock.POWER), 0, "real signal removal still switches smoke off");
      helper.succeed();
   }

   private static Vec3 rotate(Vec3 point, Direction facing) {
      return switch (facing) {
         case EAST -> point;
         case SOUTH -> new Vec3(1 - point.z, point.y, point.x);
         case WEST -> new Vec3(1 - point.x, point.y, 1 - point.z);
         case NORTH -> new Vec3(point.z, point.y, 1 - point.x);
         default -> throw new IllegalArgumentException("horizontal only");
      };
   }
}
