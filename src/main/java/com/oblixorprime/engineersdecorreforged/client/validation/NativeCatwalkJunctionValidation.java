package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Scene captures only, inside the existing nonce-guarded disposable integrated client. */
final class NativeCatwalkJunctionValidation {
   private static final BlockPos UPPER = new BlockPos(0, 80, 0);
   private static final Direction[] FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
   private static final String[] KINDS = {"standard-upper", "raised-upper", "standard-lower", "raised-lower", "chain"};
   private static final JsonArray RESULTS = new JsonArray();
   private static CompletableFuture<JsonObject> pending;
   private static int index, stage, ticks;
   private static Vec3 camera, target;
   private static BlockPos first;

   private NativeCatwalkJunctionValidation() {}

   static boolean tick(Minecraft mc, Path directory, String nonce) throws Exception {
      if (index == KINDS.length * FACINGS.length) return true;
      int kind = index / FACINGS.length;
      Direction facing = FACINGS[index % FACINGS.length];
      Direction downhill = facing.getOpposite();
      boolean raised = kind == 1 || kind == 3;
      boolean chain = kind == 4;
      if (stage == 0) {
         first = UPPER.relative(downhill).offset(0, raised ? 0 : -1, 0);
         BlockPos last = chain ? first.relative(downhill).below() : first;
         boolean lowerView = kind >= 2;
         BlockPos focused = lowerView ? (chain ? first : last) : first;
         Direction end = lowerView ? downhill : facing;
         target = Vec3.atBottomCenterOf(focused).add(end.getStepX() * .5, lowerView ? .97 : 1.95, end.getStepZ() * .5);
         Direction side = facing.getCounterClockWise();
         Vec3 eye = target.add(side.getStepX() * 3.6 + downhill.getStepX() * 1.3, .65,
            side.getStepZ() * 3.6 + downhill.getStepZ() * 1.3);
         camera = eye.add(0, -mc.player.getEyeHeight(), 0);
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
            for (var pos : BlockPos.betweenClosed(UPPER.offset(-5, -5, -5), UPPER.offset(5, 6, 5)))
               level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            var platform = raised ? ModBlocks.STEEL_CATWALK_TA.get().defaultBlockState()
               : flatRails(ModBlocks.STEEL_CATWALK.get().defaultBlockState(), facing);
            level.setBlock(UPPER, platform, Block.UPDATE_ALL);
            if (raised) level.setBlock(UPPER.above(), flatRails(ModBlocks.STEEL_RAILING.get().defaultBlockState(), facing), Block.UPDATE_ALL);
            player.connection.teleport(camera.x, camera.y, camera.z, 0, 0);
            ItemStack stack = new ItemStack(ModBlocks.STEEL_CATWALK_STAIRS.get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            Vec3 hit = Vec3.atCenterOf(UPPER).add(downhill.getStepX() * .5, 0, downhill.getStepZ() * .5);
            check(player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
               new BlockHitResult(hit, downhill, UPPER, false)).consumesAction(), "native upper attachment failed");
            check(level.getBlockState(first).is(ModBlocks.STEEL_CATWALK_STAIRS.get()), "wrong upper target cell");
            check(level.getBlockState(first).getValue(PortedBlocks.HORIZONTAL_FACING) == facing, "uphill direction changed");
            level.setBlock(first, bothRails(level.getBlockState(first)), Block.UPDATE_ALL);
            if (chain) {
               Vec3 lowHit = Vec3.atCenterOf(first).add(downhill.getStepX() * .5, -.25, downhill.getStepZ() * .5);
               check(player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
                  new BlockHitResult(lowHit, downhill, first, false)).consumesAction(), "native continuation failed");
               check(level.getBlockState(last).is(ModBlocks.STEEL_CATWALK_STAIRS.get()), "wrong continuation target");
               level.setBlock(last, bothRails(level.getBlockState(last)), Block.UPDATE_ALL);
            }
            BlockPos lower = last.relative(downhill).offset(0, raised ? -1 : 0, 0);
            level.setBlock(lower, platform, Block.UPDATE_ALL);
            if (raised) level.setBlock(lower.above(), flatRails(ModBlocks.STEEL_RAILING.get().defaultBlockState(), facing), Block.UPDATE_ALL);
            check(level.getBlockState(UPPER).is(platform.getBlock()), "upper landing replaced");
            check(stack.getCount() == 2, "creative stack changed");
            var result = new JsonObject();
            result.addProperty("scene", KINDS[kind]);
            result.addProperty("facing", facing.getName());
            result.addProperty("upper", UPPER.toShortString());
            result.addProperty("first_stair", first.toShortString());
            result.addProperty("last_stair", last.toShortString());
            result.addProperty("lower", lower.toShortString());
            result.addProperty("native_item_attachment", true);
            result.addProperty("railing_state_setup", "explicit edited sides in creative capture fixture; not survival payment proof");
            return result;
         });
         stage = 1; ticks = 0; return false;
      }
      if (stage == 1) {
         if (!pending.isDone()) return false;
         pending.join();
         check(++ticks < 300, "camera/chunk synchronization timeout");
         if (mc.player.position().distanceTo(camera) > .1 || !mc.level.getBlockState(first).is(ModBlocks.STEEL_CATWALK_STAIRS.get())) return false;
         mc.options.hideGui = true;
         mc.options.fov().set(35);
         mc.levelRenderer.allChanged();
         stage = 2; ticks = 0; return false;
      }
      var delta = target.subtract(mc.player.getEyePosition());
      mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
      mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
      if (++ticks < 50) return false;
      var result = pending.join();
      double high = capTop(true), low = capTop(false);
      result.addProperty("model_high_cap_pixels", high);
      result.addProperty("model_low_cap_pixels", low);
      result.addProperty("upper_model_gap_pixels", 32 - high);
      result.addProperty("lower_model_gap_pixels", 16 - low);
      if (chain) result.addProperty("chain_model_gap_pixels", 16 + low - high);
      result.addProperty("measurement_scope", "packaged model coordinates plus actual placement cells; not image-derived gap measurement");
      var output = directory.resolve("catwalk-junction-screenshots");
      Files.createDirectories(output);
      String filename = KINDS[kind] + "-" + facing.getName() + ".png";
      try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve(filename)); }
      result.addProperty("screenshot", filename);
      RESULTS.add(result);
      index++; stage = 0; ticks = 0;
      if (index != KINDS.length * FACINGS.length) return false;
      var proof = new JsonObject();
      proof.addProperty("suite", "catwalk-junction-capture-v1");
      proof.addProperty("nonce", nonce);
      proof.addProperty("status", "captured");
      proof.addProperty("actual_integrated_world", true);
      proof.add("scenes", RESULTS);
      Files.writeString(directory.resolve("client-catwalk-junction-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");
      return true;
   }

   private static BlockState flatRails(BlockState state, Direction facing) {
      return facing.getAxis() == Direction.Axis.Z
         ? state.setValue(PortedBlocks.WEST, true).setValue(PortedBlocks.EAST, true)
         : state.setValue(PortedBlocks.NORTH, true).setValue(PortedBlocks.SOUTH, true);
   }
   private static BlockState bothRails(BlockState state) {
      return state.setValue(PortedBlocks.LEFT_RAILING, true).setValue(PortedBlocks.RIGHT_RAILING, true)
         .setValue(PortedBlocks.EDITED_STAIR_SIDES, 3);
   }
   private static double capTop(boolean high) throws Exception {
      String path = "/assets/immersive_engineer_decor_controls_tool_reforged/models/block/furniture/steel_catwalk_stairs_lr_model.json";
      try (var stream = NativeCatwalkJunctionValidation.class.getResourceAsStream(path)) {
         check(stream != null, "missing packaged stair model");
         var model = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
         for (var entry : model.getAsJsonArray("elements")) {
            var element = entry.getAsJsonObject();
            var from = element.getAsJsonArray("from"); var to = element.getAsJsonArray("to");
            double y = from.get(1).getAsDouble(), z = from.get(2).getAsDouble();
            if (from.get(0).getAsDouble() < 2 && to.get(0).getAsDouble() < 2
               && y > (high ? 25 : 12) && to.get(1).getAsDouble() - y <= 2
               && (high ? z >= 0 && to.get(2).getAsDouble() <= 1 : z >= 15 && to.get(2).getAsDouble() <= 16))
               return to.get(1).getAsDouble();
         }
      }
      throw new IllegalStateException("missing railing end cap");
   }
   private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
