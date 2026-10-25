package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.rsgauges.ControlsBlockTypes;
import java.util.List;
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
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorSensorAttachmentGameTests {
   private DoorSensorAttachmentGameTests() {}
   @GameTest(template="empty") public static void door_sensor_attaches_north(GameTestHelper h) { contact(h, Direction.NORTH); }
   @GameTest(template="empty") public static void door_sensor_attaches_east(GameTestHelper h) { contact(h, Direction.EAST); }
   @GameTest(template="empty") public static void door_sensor_attaches_south(GameTestHelper h) { contact(h, Direction.SOUTH); }
   @GameTest(template="empty") public static void door_sensor_attaches_west(GameTestHelper h) { contact(h, Direction.WEST); }
   @GameTest(template="empty") public static void door_sensor_attaches_up(GameTestHelper h) { contact(h, Direction.UP); }
   @GameTest(template="empty") public static void door_sensor_attaches_down(GameTestHelper h) { contact(h, Direction.DOWN); }

   private static void contact(GameTestHelper h, Direction face) {
      var level = h.getLevel();
      var target = h.absolutePos(new BlockPos(1, 2, 1));
      var support = target.relative(face.getOpposite());
      var player = h.makeMockPlayer(GameType.SURVIVAL);
      for (double offset : new double[]{.125, .5, .875}) {
         level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
         level.setBlock(support, Blocks.BRICKS.defaultBlockState(), 3);
         player.setYRot((float)(offset * 360));
         player.setXRot((float)(offset * 120 - 60));
         ItemStack stack = new ItemStack(sensor(), 2);
         BlockPlaceContext context = context(level, player, support, face, offset, stack);
         h.assertValueEqual(context.getClickedPos(), target, "native context must target the adjacent cell, not replace the clicked support");
         h.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(), "native item placement succeeds on " + face);
         BlockState placed = level.getBlockState(target);
         h.assertTrue(placed.is(sensor()), "sensor occupies native placement target");
         h.assertTrue(level.getBlockState(support).is(Blocks.BRICKS), "clicked support is preserved");
         h.assertValueEqual(placed.getValue(ControlsBlockTypes.FACING), face, "attachment direction remains the clicked face");
         h.assertValueEqual(stack.getCount(), 1, "one survival item is paid");
         AABB box = placed.getShape(level, target).bounds();
         double contactPlane = switch (face) {
            case NORTH -> box.maxZ; case EAST -> box.minX; case SOUTH -> box.minZ;
            case WEST -> box.maxX; case UP -> box.minY; case DOWN -> box.maxY;
         };
         double expected = face == Direction.NORTH || face == Direction.WEST || face == Direction.DOWN ? 1 : 0;
         h.assertTrue(Math.abs(contactPlane - expected) < 1e-9,
            "sensor must touch clicked support on " + face + ": expected plane " + expected + ", actual " + contactPlane);
         h.assertTrue(placed.getCollisionShape(level, target).isEmpty(), "sensor stays non-colliding");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void door_sensor_placement_preserves_fluid_and_item_accounting(GameTestHelper h) {
      var level = h.getLevel();
      var target = h.absolutePos(new BlockPos(1, 2, 1));
      for (boolean creative : new boolean[]{false, true}) {
         var player = new net.neoforged.neoforge.common.util.FakePlayer(level,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "DoorSensorFixture"));
         player.gameMode.changeGameModeForPlayer(creative ? GameType.CREATIVE : GameType.SURVIVAL);
         for (Direction face : Direction.values()) {
            var support = target.relative(face.getOpposite());
            for (var fluid : List.of(Fluids.EMPTY, Fluids.WATER, Fluids.LAVA, NativeFluidloggedBlock.creosote())) {
               level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
               level.setBlock(support, Blocks.BRICKS.defaultBlockState(), 3);
               var expected = fluid == Fluids.EMPTY ? Fluids.EMPTY.defaultFluidState() : ((FlowingFluid)fluid).getSource(false);
               level.setBlock(target, expected.createLegacyBlock(), 3);
               ItemStack stack = new ItemStack(sensor(), 2);
               player.setItemInHand(InteractionHand.MAIN_HAND, stack);
               Vec3 hit = Vec3.atCenterOf(support).add(face.getStepX()*.5, face.getStepY()*.5, face.getStepZ()*.5);
               h.assertTrue(player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
                  new BlockHitResult(hit, face, support, false)).consumesAction(), "wet native interaction succeeds");
               var placed = level.getBlockState(target);
               h.assertTrue(placed.is(sensor()), "fluid placement retains sensor");
               h.assertValueEqual(placed.getFluidState(), expected, "placement preserves source cell");
               h.assertValueEqual(placed.getValue(ControlsBlockTypes.FACING), face, "fluid placement preserves facing");
               h.assertValueEqual(stack.getCount(), creative ? 2 : 1, "creative/survival item accounting");
               h.assertTrue(placed.getCollisionShape(level, target).isEmpty(), "wet sensor remains non-colliding");
            }
            level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(support, Blocks.AIR.defaultBlockState(), 3);
         }
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void door_sensor_requires_and_tracks_clicked_support(GameTestHelper h) {
      var level = h.getLevel();
      var target = h.absolutePos(new BlockPos(1, 2, 1));
      var player = h.makeMockPlayer(GameType.SURVIVAL);
      for (Direction face : Direction.values()) {
         var support = target.relative(face.getOpposite());
         level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
         level.setBlock(support, Blocks.BRICKS.defaultBlockState(), 3);
         ItemStack stack = new ItemStack(sensor(), 2);
         h.assertTrue(((BlockItem)stack.getItem()).place(context(level, player, support, face, .5, stack)).consumesAction(), "placement succeeds with support");
         var state = level.getBlockState(target);
         h.assertTrue(state.canSurvive(level, target), "clicked block supplies required support");
         level.setBlock(support, Blocks.AIR.defaultBlockState(), 3);
         h.assertFalse(state.canSurvive(level, target), "removing clicked support invalidates attachment");
         h.assertFalse(level.getBlockState(target).is(sensor()), "neighbor update removes unsupported sensor");
         level.setBlock(support, Blocks.GLASS_PANE.defaultBlockState(), 3);
         ItemStack rejected = new ItemStack(sensor(), 2);
         h.assertFalse(((BlockItem)rejected.getItem()).place(context(level, player, support, face, .5, rejected)).consumesAction(), "non-sturdy face rejects placement");
         h.assertValueEqual(rejected.getCount(), 2, "rejected placement consumes no item");
         level.setBlock(support, Blocks.AIR.defaultBlockState(), 3);
      }
      h.succeed();
   }

   @GameTest(template="empty", timeoutTicks=140)
   public static void door_sensor_keeps_player_detection_for_every_attachment(GameTestHelper h) {
      var level = h.getLevel();
      var support = h.absolutePos(new BlockPos(2, 2, 2));
      level.setBlock(support, Blocks.BRICKS.defaultBlockState(), 3);
      var player = h.makeMockPlayer(GameType.CREATIVE);
      for (Direction face : Direction.values()) {
         ItemStack stack = new ItemStack(sensor());
         h.assertTrue(((BlockItem)stack.getItem()).place(context(level, player, support, face, .5, stack)).consumesAction(), "all six attachments place");
      }
      player.moveTo(support.getX()+.5, support.getY()+.5, support.getZ()+.5);
      level.addFreshEntity(player);
      h.runAfterDelay(45, () -> {
         for (Direction face : Direction.values()) {
            var pos = support.relative(face); var state = level.getBlockState(pos);
            h.assertTrue(state.getValue(ControlsBlockTypes.POWERED), "player detection remains active for " + face);
            h.assertValueEqual(state.getSignal(level, pos, face), 15, "detected player emits 15");
         }
         player.moveTo(support.getX()+30, support.getY()+.5, support.getZ()+30);
      });
      h.runAfterDelay(95, () -> {
         for (Direction face : Direction.values()) {
            var pos = support.relative(face); var state = level.getBlockState(pos);
            h.assertFalse(state.getValue(ControlsBlockTypes.POWERED), "detection clears after player leaves");
            h.assertValueEqual(state.getSignal(level, pos, face), 0, "empty detection emits zero");
         }
         player.discard(); h.succeed();
      });
   }

   private static Block sensor() {
      return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "door_sensor_switch"));
   }

   private static BlockPlaceContext context(net.minecraft.world.level.Level level, Player player, BlockPos support,
         Direction face, double tangent, ItemStack stack) {
      Vec3 hit = Vec3.atLowerCornerOf(support).add(
         face.getAxis() == Direction.Axis.X ? (face.getStepX() > 0 ? 1 : 0) : tangent,
         face.getAxis() == Direction.Axis.Y ? (face.getStepY() > 0 ? 1 : 0) : tangent,
         face.getAxis() == Direction.Axis.Z ? (face.getStepZ() > 0 ? 1 : 0) : tangent);
      return new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, new BlockHitResult(hit, face, support, false));
   }
}
