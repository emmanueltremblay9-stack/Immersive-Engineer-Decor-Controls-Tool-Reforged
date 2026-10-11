package com.oblixorprime.engineersdecorreforged.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.rsgauges.ControlsBlockTypes;
import com.oblixorprime.engineersdecorreforged.rsgauges.IndustrialSwitchBlock;
import com.oblixorprime.engineersdecorreforged.rsgauges.IndustrialSwitchBlock.Part;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IndustrialSwitchGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 2, 3);
   private IndustrialSwitchGameTests() { }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void industrial_switch_floor_and_wall_placement_faces_outward(GameTestHelper helper) {
      for (Direction look : Direction.Plane.HORIZONTAL) {
         place(helper, Direction.UP, look);
         assertPair(helper, look.getOpposite());
         clear(helper);
         for (Direction face : Direction.Plane.HORIZONTAL) {
            place(helper, face, look);
            assertPair(helper, face);
            clear(helper);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void industrial_switch_both_halves_collide_select_and_toggle(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         place(helper, Direction.UP, facing.getOpposite());
         BlockPos lower = helper.absolutePos(CENTER);
         for (int half = 0; half < 2; half++) {
            BlockPos pos = lower.above(half);
            BlockState state = helper.getLevel().getBlockState(pos);
            AABB box = expectedBounds(facing);
            helper.assertValueEqual(state.getShape(helper.getLevel(), pos).bounds(), box, "outline must match cabinet half");
            helper.assertValueEqual(state.getCollisionShape(helper.getLevel(), pos).bounds(), box, "collision must match cabinet half");
            Vec3 middle = Vec3.atCenterOf(pos);
            Vec3 front = middle.add(facing.getStepX() * 2, 0, facing.getStepZ() * 2);
            BlockHitResult hit = helper.getLevel().clip(new ClipContext(front, middle,
               ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            helper.assertValueEqual(hit.getBlockPos(), pos, "normal raycast must select each cabinet half");
            helper.useBlock(CENTER.above(half), player);
            for (int check = 0; check < 2; check++) {
               BlockPos checkPos = lower.above(check);
               BlockState toggled = helper.getLevel().getBlockState(checkPos);
               helper.assertValueEqual(toggled.getValue(ControlsBlockTypes.POWERED), half == 0, "both halves must toggle together");
               for (Direction side : Direction.values()) {
                  helper.assertValueEqual(toggled.getSignal(helper.getLevel(), checkPos, side), half == 0 ? 15 : 0, "weak signal");
                  helper.assertValueEqual(toggled.getDirectSignal(helper.getLevel(), checkPos, side), half == 0 ? 15 : 0, "strong signal");
               }
            }
         }
         clear(helper);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void industrial_switch_rejects_blocked_headroom_without_overwrite(GameTestHelper helper) {
      BlockPos target = helper.absolutePos(CENTER);
      helper.getLevel().setBlock(target.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      BlockPlaceContext context = context(helper, Direction.UP, Direction.NORTH, player);
      ItemStack stack = context.getItemInHand();
      helper.assertTrue(!((BlockItem)stack.getItem()).place(context).consumesAction(), "blocked placement must fail");
      helper.assertTrue(helper.getLevel().getBlockState(target).isAir(), "failed placement must leave lower empty");
      helper.assertTrue(helper.getLevel().getBlockState(target.above()).is(Blocks.DIAMOND_BLOCK), "headroom obstruction must survive");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void industrial_switch_breaking_either_half_cleans_up_and_drops_once(GameTestHelper helper) {
      for (boolean creative : new boolean[]{false, true}) {
         for (int half = 0; half < 2; half++) {
            clear(helper);
            AABB dropArea = new AABB(helper.absolutePos(CENTER)).inflate(2);
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, dropArea).forEach(ItemEntity::discard);
            place(helper, Direction.UP, Direction.SOUTH);
            helper.useBlock(CENTER, helper.makeMockPlayer(GameType.CREATIVE));
            BlockPos lower = helper.absolutePos(CENTER);
            BlockPos target = lower.above(half);
            BlockState lowerState = helper.getLevel().getBlockState(lower);
            BlockState upperState = helper.getLevel().getBlockState(lower.above());
            helper.assertValueEqual(Block.getDrops(lowerState, helper.getLevel(), lower, null).stream().mapToInt(ItemStack::getCount).sum(), 1, "lower owns one drop");
            helper.assertTrue(Block.getDrops(upperState, helper.getLevel(), lower.above(), null).isEmpty(), "upper owns no duplicate drop");
            Player player = helper.makeMockPlayer(creative ? GameType.CREATIVE : GameType.SURVIVAL);
            BlockState before = helper.getLevel().getBlockState(target);
            before.getBlock().playerWillDestroy(helper.getLevel(), target, before, player);
            helper.getLevel().destroyBlock(target, !creative);
            helper.assertTrue(helper.getLevel().getBlockState(lower).isAir(), "lower must be removed");
            helper.assertTrue(helper.getLevel().getBlockState(lower.above()).isAir(), "upper must be removed");
            int drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, dropArea,
               item -> item.getItem().is(cabinet().asItem())).stream().mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertValueEqual(drops, creative ? 0 : 1, "actual break must drop exactly one in survival, none in creative");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void industrial_switch_legacy_saved_states_remain_non_destructive(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      for (Direction facing : Direction.values()) {
         for (boolean powered : new boolean[]{false, true}) {
            BlockState original = cabinet().defaultBlockState().setValue(ControlsBlockTypes.FACING, facing)
               .setValue(ControlsBlockTypes.POWERED, powered);
            var tag = NbtUtils.writeBlockState(original);
            tag.getCompound("Properties").remove("part");
            BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag);
            helper.assertValueEqual(loaded, original, "old facing and power must survive loading");
            helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow(), loaded, "codec must preserve old saves");
            helper.assertValueEqual(loaded.getValue(IndustrialSwitchBlock.PART), Part.LEGACY, "missing saved part must load as legacy");
            helper.getLevel().setBlock(pos, loaded, 3);
            helper.getLevel().setBlock(pos.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
            helper.getLevel().setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
            helper.assertValueEqual(helper.getLevel().getBlockState(pos), loaded, "legacy must survive without conversion or deletion");
            helper.assertTrue(helper.getLevel().getBlockState(pos.above()).is(Blocks.DIAMOND_BLOCK), "legacy must not overwrite its neighbor");
            helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(loaded)), loaded, "legacy round trip");
            helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void industrial_switch_rotation_and_pair_serialization(GameTestHelper helper) {
      place(helper, Direction.UP, Direction.SOUTH);
      BlockPos pos = helper.absolutePos(CENTER);
      BlockState lower = helper.getLevel().getBlockState(pos);
      helper.getLevel().setBlock(pos, lower.rotate(Rotation.CLOCKWISE_90), 3);
      assertPair(helper, Direction.EAST);
      for (int half = 0; half < 2; half++) {
         BlockState state = helper.getLevel().getBlockState(pos.above(half));
         helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(state)), state, "pair state round trip");
      }
      helper.succeed();
   }

   private static Block cabinet() {
      return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "industrialswitch"));
   }
   private static void place(GameTestHelper helper, Direction face, Direction look) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      BlockPlaceContext context = context(helper, face, look, player);
      helper.assertTrue(((BlockItem)context.getItemInHand().getItem()).place(context).consumesAction(), "cabinet placement must succeed");
   }
   private static BlockPlaceContext context(GameTestHelper helper, Direction face, Direction look, Player player) {
      player.setYRot(look.toYRot());
      BlockPos support = helper.absolutePos(CENTER).relative(face.getOpposite());
      helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 3);
      Vec3 hit = Vec3.atCenterOf(support).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
      return new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(cabinet()),
         new BlockHitResult(hit, face, support, false));
   }
   private static void assertPair(GameTestHelper helper, Direction facing) {
      BlockPos pos = helper.absolutePos(CENTER);
      for (int half = 0; half < 2; half++) {
         BlockState state = helper.getLevel().getBlockState(pos.above(half));
         helper.assertTrue(state.is(cabinet()), "cabinet half must exist");
         helper.assertValueEqual(state.getPistonPushReaction(), PushReaction.BLOCK, "pistons must not split cabinet halves");
         helper.assertValueEqual(state.getValue(ControlsBlockTypes.FACING), facing, "indicator side must face outward");
         helper.assertValueEqual(state.getValue(IndustrialSwitchBlock.PART), half == 0 ? Part.LOWER : Part.UPPER, "paired part");
      }
   }
   private static AABB expectedBounds(Direction facing) {
      return switch (facing) {
         case NORTH -> new AABB(0, 0, 7.5 / 16, 1, 1, 1);
         case EAST -> new AABB(0, 0, 0, 8.5 / 16, 1, 1);
         case SOUTH -> new AABB(0, 0, 0, 1, 1, 8.5 / 16);
         default -> new AABB(7.5 / 16, 0, 0, 1, 1, 1);
      };
   }
   private static void clear(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
      for (Direction direction : Direction.values()) {
         helper.getLevel().setBlock(pos.relative(direction), Blocks.AIR.defaultBlockState(), 3);
      }
   }
   @GameTest(template = "empty", timeoutTicks = 40)
   public static void industrial_switch_outlines_match_shipped_models(GameTestHelper helper) {
      for (Part part : new Part[]{Part.LEGACY, Part.LOWER}) {
         for (Direction facing : Direction.values()) {
            for (boolean powered : new boolean[]{false, true}) {
               assertModelBounds(helper, cabinet().defaultBlockState().setValue(IndustrialSwitchBlock.PART, part)
                  .setValue(ControlsBlockTypes.FACING, facing).setValue(ControlsBlockTypes.POWERED, powered), helper.absolutePos(CENTER));
            }
         }
      }
      helper.succeed();
   }

   private static void assertModelBounds(GameTestHelper helper, BlockState state, BlockPos pos) {
      Direction facing = state.getValue(ControlsBlockTypes.FACING);
      boolean powered = state.getValue(ControlsBlockTypes.POWERED);
      String key = "facing=" + facing.getName() + ",powered=" + powered + ",part=" + state.getValue(IndustrialSwitchBlock.PART).getSerializedName();
      JsonObject variant = readJson("/assets/" + EngineersDecorReforged.MOD_ID + "/blockstates/industrialswitch.json")
         .getAsJsonObject("variants").getAsJsonObject(key);
      helper.assertTrue(!variant.has("x") || variant.get("x").getAsInt() == 0, "model oracle requires no X rotation");
      JsonObject model = readModel(variant.get("model").getAsString());
      for (int depth = 0; !model.has("elements") && depth < 8; depth++) {
         model = readModel(model.get("parent").getAsString());
      }
      AABB bounds = null;
      for (JsonElement value : model.getAsJsonArray("elements")) {
         JsonObject element = value.getAsJsonObject();
         helper.assertTrue(!element.has("rotation") || element.getAsJsonObject("rotation").get("angle").getAsDouble() == 0,
            "model oracle requires axis-aligned elements");
         JsonArray from = element.getAsJsonArray("from");
         JsonArray to = element.getAsJsonArray("to");
         AABB box = new AABB(from.get(0).getAsDouble() / 16, from.get(1).getAsDouble() / 16, from.get(2).getAsDouble() / 16,
            to.get(0).getAsDouble() / 16, to.get(1).getAsDouble() / 16, to.get(2).getAsDouble() / 16);
         bounds = bounds == null ? box : bounds.minmax(box);
      }
      int degrees = variant.has("y") ? variant.get("y").getAsInt() : 0;
      helper.assertTrue(degrees % 90 == 0, "model oracle requires quarter-turn Y rotation");
      for (int step = 0; step < Math.floorMod(degrees / 90, 4); step++) {
         bounds = new AABB(1 - bounds.maxZ, bounds.minY, bounds.minX, 1 - bounds.minZ, bounds.maxY, bounds.maxX);
      }
      AABB actual = state.getShape(helper.getLevel(), pos).bounds();
      if (state.getValue(IndustrialSwitchBlock.PART) == Part.LOWER) {
         AABB upper = state.setValue(IndustrialSwitchBlock.PART, Part.UPPER).getShape(helper.getLevel(), pos.above()).bounds().move(0, 1, 0);
         actual = actual.minmax(upper);
      }
      helper.assertValueEqual(actual, bounds, "cabinet outline must match shipped model bounds for " + key);
   }

   private static JsonObject readModel(String id) {
      String[] parts = id.split(":", 2);
      return readJson("/assets/" + parts[0] + "/models/" + parts[1] + ".json");
   }

   private static JsonObject readJson(String path) {
      InputStream stream = IndustrialSwitchGameTests.class.getResourceAsStream(path);
      if (stream == null) {
         throw new IllegalStateException("Missing GameTest resource " + path);
      }
      try (InputStream input = stream; InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      } catch (Exception exception) {
         throw new IllegalStateException("Could not read GameTest resource " + path, exception);
      }
   }
   @GameTest(template = "empty", timeoutTicks = 40)
   public static void industrial_switch_notifies_indirect_consumers_on_upper_toggle(GameTestHelper helper) {
      assertIndirectConsumers(helper, false);
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void industrial_switch_notifies_indirect_consumers_on_powered_removal(GameTestHelper helper) {
      assertIndirectConsumers(helper, true);
   }

   private static void assertIndirectConsumers(GameTestHelper helper, boolean remove) {
      place(helper, Direction.UP, Direction.SOUTH);
      BlockPos lower = helper.absolutePos(CENTER);
      List<BlockPos> lamps = new ArrayList<>();
      for (int half = 0; half < 2; half++) {
         for (Direction side : Direction.values()) {
            if (side == (half == 0 ? Direction.UP : Direction.DOWN)) { continue; }
            BlockPos conductor = lower.above(half).relative(side);
            BlockPos lamp = conductor.relative(side);
            helper.getLevel().setBlock(conductor, Blocks.STONE.defaultBlockState(), 3);
            helper.getLevel().setBlock(lamp, Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
            lamps.add(lamp);
         }
      }
      helper.useBlock(CENTER.above(), helper.makeMockPlayer(GameType.CREATIVE));
      for (BlockPos lamp : lamps) {
         helper.assertTrue(helper.getLevel().getBlockState(lamp).getValue(BlockStateProperties.LIT), "indirect consumer must turn on");
      }
      if (remove) {
         helper.getLevel().destroyBlock(lower.above(), true);
      } else {
         helper.useBlock(CENTER.above(), helper.makeMockPlayer(GameType.CREATIVE));
      }
      helper.runAfterDelay(6, () -> {
         for (BlockPos lamp : lamps) {
            helper.assertTrue(!helper.getLevel().getBlockState(lamp).getValue(BlockStateProperties.LIT), "indirect consumer must turn off");
         }
         helper.succeed();
      });
   }

}
