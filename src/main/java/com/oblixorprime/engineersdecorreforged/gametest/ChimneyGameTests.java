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
         level.getBlockState(pos).neighborChanged(level, pos, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, source, false);
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
            helper.assertValueEqual(placed.getValue(StairBlock.HALF), Half.BOTTOM, "trunk always places upright");
            helper.assertValueEqual(placed.getValue(StairBlock.SHAPE), StairsShape.STRAIGHT, "trunk keeps its fitting model");
            for (Direction side : Direction.Plane.HORIZONTAL) {
               var updated = placed.updateShape(side, Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing.getClockWise()),
                  level, pos, pos.relative(side));
               helper.assertValueEqual(updated.getValue(StairBlock.SHAPE), StairsShape.STRAIGHT, "neighbor must not erase the trunk into a plain roof corner");
            }
            level.setBlock(support, Blocks.AIR.defaultBlockState(), 2);
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
