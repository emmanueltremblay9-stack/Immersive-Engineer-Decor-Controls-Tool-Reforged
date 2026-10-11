package com.oblixorprime.engineersdecorreforged.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.rsgauges.ControlsBlockTypes;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorSensorShapeGameTests {
   private static final BlockPos CENTER = new BlockPos(1, 1, 1);

   private DoorSensorShapeGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void door_sensor_outline_matches_shipped_model_in_all_states(GameTestHelper helper) {
      Block sensor = sensor();
      BlockPos pos = helper.absolutePos(CENTER);
      for (Direction facing : Direction.values()) {
         for (boolean powered : new boolean[]{false, true}) {
            BlockState state = sensor.defaultBlockState().setValue(ControlsBlockTypes.FACING, facing)
               .setValue(ControlsBlockTypes.POWERED, powered);
            assertModelBounds(helper, state, pos);
            helper.assertTrue(state.getCollisionShape(helper.getLevel(), pos).isEmpty(), "sensor must remain non-colliding");
            helper.assertValueEqual(state.getSignal(helper.getLevel(), pos, facing), powered ? 15 : 0,
               "shape must not change sensor redstone output");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void door_sensor_item_placement_keeps_model_aligned_on_every_face(GameTestHelper helper) {
      Block sensor = sensor();
      BlockPos target = helper.absolutePos(CENTER);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      for (Direction face : Direction.values()) {
         BlockPos support = target.relative(face.getOpposite());
         helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), 3);
         helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 3);
         Vec3 hit = Vec3.atCenterOf(support).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
         ItemStack stack = new ItemStack(sensor);
         BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND,
            stack, new BlockHitResult(hit, face, support, false));
         helper.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(), "sensor item must place on " + face);
         BlockState placed = helper.getLevel().getBlockState(target);
         helper.assertTrue(placed.is(sensor), "sensor item must place the registered sensor");
         helper.assertValueEqual(placed.getValue(ControlsBlockTypes.FACING), face, "clicked-face placement must be preserved");
         assertModelBounds(helper, placed, target);
         helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), 3);
         helper.getLevel().setBlock(support, Blocks.AIR.defaultBlockState(), 3);
      }
      helper.succeed();
   }

   private static Block sensor() {
      return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "door_sensor_switch"));
   }

   private static void assertModelBounds(GameTestHelper helper, BlockState state, BlockPos pos) {
      Direction facing = state.getValue(ControlsBlockTypes.FACING);
      boolean powered = state.getValue(ControlsBlockTypes.POWERED);
      String key = "facing=" + facing.getName() + ",powered=" + powered;
      JsonObject variant = readJson("/assets/" + EngineersDecorReforged.MOD_ID + "/blockstates/door_sensor_switch.json")
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
      helper.assertValueEqual(state.getShape(helper.getLevel(), pos).bounds(), bounds,
         "door sensor outline must match shipped model bounds for " + key);
   }

   private static JsonObject readModel(String id) {
      String[] parts = id.split(":", 2);
      return readJson("/assets/" + parts[0] + "/models/" + parts[1] + ".json");
   }

   private static JsonObject readJson(String path) {
      InputStream stream = DoorSensorShapeGameTests.class.getResourceAsStream(path);
      if (stream == null) {
         throw new IllegalStateException("Missing GameTest resource " + path);
      }
      try (InputStream input = stream; InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      } catch (Exception exception) {
         throw new IllegalStateException("Could not read GameTest resource " + path, exception);
      }
   }
}
