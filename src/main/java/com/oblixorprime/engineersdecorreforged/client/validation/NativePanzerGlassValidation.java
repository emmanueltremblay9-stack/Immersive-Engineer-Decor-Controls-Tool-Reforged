package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/** Runs only inside the nonce-guarded, disposable native client fixture. */
final class NativePanzerGlassValidation {
   private static final BlockPos POS = new BlockPos(0, 80, 0);
   private static final String[] SCENES = {"wall-front", "wall-oblique", "single", "l-shape", "water-wall", "lava-wall"};
   private static final JsonArray RESULTS = new JsonArray();
   private static CompletableFuture<?> pending;
   private static int index, stage, ticks;
   private static Vec3 camera;

   private NativePanzerGlassValidation() {}

   static boolean tick(Minecraft mc, Path directory, String nonce) throws Exception {
      if (index == SCENES.length) return true;
      if (stage == 0) {
         camera = index == 1 ? new Vec3(3.6, 80.8, -4.4) : new Vec3(.5, 80.0, -5.8);
         pending = mc.getSingleplayerServer().submit(() -> {
            var server = mc.getSingleplayerServer();
            var player = server.getPlayerList().getPlayer(mc.player.getUUID());
            var level = player.serverLevel();
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.setDayTime(6000);
            level.setWeatherParameters(100000, 0, false, false);
            for (var pos : BlockPos.betweenClosed(POS.offset(-5, -2, -3), POS.offset(5, 5, 4)))
               level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.getFluidTicks().clearArea(net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(
               POS.offset(-5, -2, -3), POS.offset(5, 5, 4)));
            for (int x = -3; x <= 3; x++) {
               for (int y = -1; y <= 3; y++) {
                  level.setBlock(POS.offset(x, y, 3), (Math.floorMod(x+y, 2)==0 ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_BLUE_CONCRETE).defaultBlockState(), Block.UPDATE_ALL);
                  if (Math.abs(x) == 2 || y == -1 || y == 3)
                     level.setBlock(POS.offset(x, y, 0), Blocks.BRICKS.defaultBlockState(), Block.UPDATE_ALL);
               }
            }
            for (int x = -1; x <= 1; x++) {
               for (int y = 0; y <= 2; y++) {
                  if (index == 2 && (x != 0 || y != 1)) continue;
                  if (index == 3 && x > -1 && y > 0) continue;
                  var state = ModBlocks.PANZERGLASS_BLOCK.get().defaultBlockState();
                  if (index == 4) state = NativeFluidloggedBlock.placement(state, Fluids.WATER.getSource(false));
                  if (index == 5) state = NativeFluidloggedBlock.placement(state, Fluids.LAVA.getSource(false));
                  level.setBlock(POS.offset(x, y, 0), state, Block.UPDATE_ALL);
               }
            }
            player.connection.teleport(camera.x, camera.y, camera.z, 0, 0);
         });
         stage = 1;
         ticks = 0;
         return false;
      }
      if (stage == 1) {
         if (!pending.isDone()) return false;
         pending.join();
         check(++ticks < 300, "camera/chunk synchronization timeout");
         if (mc.player.position().distanceTo(camera) > .1
            || !mc.level.getBlockState(POS.above()).is(ModBlocks.PANZERGLASS_BLOCK.get()) && index != 3) return false;
         mc.options.hideGui = true;
         mc.levelRenderer.allChanged();
         stage = 2;
         ticks = 0;
         return false;
      }
      var target = Vec3.atCenterOf(POS.above());
      var delta = target.subtract(mc.player.getEyePosition());
      mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
      mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
      if (++ticks < 50) return false;
      int visibleInternalFaces = 0;
      int pairs = 0;
      for (var pos : BlockPos.betweenClosed(POS.offset(-1, 0, 0), POS.offset(1, 2, 0))) {
         var state = mc.level.getBlockState(pos);
         if (!state.is(ModBlocks.PANZERGLASS_BLOCK.get())) continue;
         for (var face : Direction.values()) {
            var neighbor = pos.relative(face);
            if (mc.level.getBlockState(neighbor).is(ModBlocks.PANZERGLASS_BLOCK.get())) {
               pairs++;
               if (Block.shouldRenderFace(state, mc.level, pos, face, neighbor)) visibleInternalFaces++;
            }
         }
      }
      var output = directory.resolve("panzer-glass-screenshots");
      Files.createDirectories(output);
      var filename = SCENES[index]+".png";
      try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         image.writeToFile(output.resolve(filename));
      }
      var result = new JsonObject();
      result.addProperty("scene", SCENES[index]);
      result.addProperty("adjacent_face_pairs", pairs);
      result.addProperty("visible_internal_faces", visibleInternalFaces);
      result.addProperty("screenshot", filename);
      RESULTS.add(result);
      index++;
      stage = 0;
      ticks = 0;
      if (index != SCENES.length) return false;
      var proof = new JsonObject();
      proof.addProperty("suite", "panzer-glass-capture-v1");
      proof.addProperty("nonce", nonce);
      proof.addProperty("status", "captured");
      proof.addProperty("actual_integrated_world", true);
      proof.add("scenes", RESULTS);
      Files.writeString(directory.resolve("client-panzer-glass-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");
      return true;
   }

   private static void check(boolean value, String message) {
      if (!value) throw new IllegalStateException(message);
   }
}
