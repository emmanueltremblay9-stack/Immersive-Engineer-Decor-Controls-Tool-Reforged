package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.rsgauges.ControlsBlockTypes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Native item placements and captures inside the existing nonce-guarded disposable client. */
final class NativeDoorSensorValidation {
   private static final BlockPos SUPPORT = new BlockPos(0, 80, 0);
   private static final Direction[] FACES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN};
   private static final JsonArray RESULTS = new JsonArray();
   private static CompletableFuture<JsonObject> pending;
   private static int index, stage, ticks;
   private static Vec3 camera, look;

   private NativeDoorSensorValidation() {}

   static boolean tick(Minecraft mc, Path directory, String nonce) throws Exception {
      if (index == FACES.length) return true;
      Direction face = FACES[index];
      BlockPos target = SUPPORT.relative(face);
      if (stage == 0) {
         Vec3 center = Vec3.atCenterOf(SUPPORT);
         Vec3 eye = face.getAxis() == Direction.Axis.Y
            ? center.add(1.7, face.getStepY() * 2.5, -2.0)
            : center.add(face.getStepX() * 2.8 + face.getStepZ() * -1.2, .4, face.getStepZ() * 2.8 + face.getStepX() * 1.2);
         camera = eye.add(0, -mc.player.getEyeHeight(), 0);
         look = face.getAxis() == Direction.Axis.Y ? center.add(0, face.getStepY() * .65, -.2)
            : center.add(face.getStepX() * .7, -.25, face.getStepZ() * .7);
         pending = mc.getSingleplayerServer().submit(() -> {
            var server = mc.getSingleplayerServer();
            var player = server.getPlayerList().getPlayer(mc.player.getUUID());
            var level = player.serverLevel();
            player.setGameMode(GameType.CREATIVE);
            player.setNoGravity(true);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.setDayTime(6000);
            level.setWeatherParameters(100000, 0, false, false);
            for (var pos : BlockPos.betweenClosed(SUPPORT.offset(-5, -4, -5), SUPPORT.offset(5, 4, 5)))
               level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(SUPPORT, Blocks.BRICKS.defaultBlockState(), Block.UPDATE_ALL);
            ItemStack stack = new ItemStack(sensor(), 2);
            Vec3 hit = Vec3.atCenterOf(SUPPORT).add(face.getStepX() * .5, face.getStepY() * .5, face.getStepZ() * .5);
            BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack,
               new BlockHitResult(hit, face, SUPPORT, false));
            check(context.getClickedPos().equals(target), "unexpected native placement target");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            check(player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
               new BlockHitResult(hit, face, SUPPORT, false)).consumesAction(), "native sensor placement failed");
            check(level.getBlockState(target).is(sensor()), "sensor missing from adjacent target");
            check(level.getBlockState(SUPPORT).is(Blocks.BRICKS), "clicked support replaced");
            check(level.getBlockState(target).getValue(ControlsBlockTypes.FACING) == face, "clicked face changed");
            check(stack.getCount() == 2, "creative placement consumed item");
            player.connection.teleport(camera.x, camera.y, camera.z, 0, 0);
            var result = new JsonObject();
            result.addProperty("clicked_face", face.getName());
            result.addProperty("clicked_support", SUPPORT.toShortString());
            result.addProperty("context_target", context.getClickedPos().toShortString());
            result.addProperty("actual_sensor_cell", target.toShortString());
            result.addProperty("support_preserved", true);
            result.addProperty("creative_stack_count", stack.getCount());
            return result;
         });
         stage = 1; ticks = 0; return false;
      }
      if (stage == 1) {
         if (!pending.isDone()) return false;
         pending.join();
         check(++ticks < 300, "camera/chunk synchronization timeout");
         if (mc.player.position().distanceTo(camera) > .1 || !mc.level.getBlockState(target).is(sensor())) return false;
         mc.options.hideGui = true;
         mc.levelRenderer.allChanged();
         stage = 2; ticks = 0; return false;
      }
      Vec3 delta = look.subtract(mc.player.getEyePosition());
      mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
      mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
      if (++ticks < 50) return false;
      var state = mc.level.getBlockState(target);
      AABB box = state.getShape(mc.level, target).bounds();
      double gap = switch (face) {
         case NORTH -> 1-box.maxZ; case EAST -> box.minX; case SOUTH -> box.minZ;
         case WEST -> 1-box.maxX; case UP -> box.minY; case DOWN -> 1-box.maxY;
      };
      var result = pending.join();
      result.addProperty("support_gap_blocks", gap);
      result.addProperty("powered", state.getValue(ControlsBlockTypes.POWERED));
      result.addProperty("collision_empty", state.getCollisionShape(mc.level, target).isEmpty());
      var output = directory.resolve("door-sensor-screenshots");
      Files.createDirectories(output);
      String filename = face.getName()+".png";
      try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve(filename)); }
      result.addProperty("screenshot", filename);
      RESULTS.add(result);
      index++; stage = 0; ticks = 0;
      if (index != FACES.length) return false;
      var proof = new JsonObject();
      proof.addProperty("suite", "door-sensor-attachment-capture-v1");
      proof.addProperty("nonce", nonce);
      proof.addProperty("status", "captured");
      proof.addProperty("actual_integrated_world", true);
      proof.add("scenes", RESULTS);
      Files.writeString(directory.resolve("client-door-sensor-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");
      return true;
   }

   private static Block sensor() {
      return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "door_sensor_switch"));
   }
   private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
