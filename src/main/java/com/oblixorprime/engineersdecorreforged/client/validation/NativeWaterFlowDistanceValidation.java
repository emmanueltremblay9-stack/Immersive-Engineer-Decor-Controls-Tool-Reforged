package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Observes natural flow only, in the existing nonce-owned integrated world. */
final class NativeWaterFlowDistanceValidation {
   private static final BlockPos SOURCE = new BlockPos(13, 80, 5);
   private static final JsonArray RESULTS = new JsonArray();
   private static int distance;
   private static int stage;
   private static int ticks;
   private static int stableTicks;
   private static String previous;
   private static long started;
   private static long serverStarted;
   private static CompletableFuture<?> pending;
   private static JsonObject result;

   private static BlockPos target() { return SOURCE.west(distance == 0 ? 4 : distance); }
   private static Vec3 overview() { return new Vec3(SOURCE.getX()-4.5, SOURCE.getY()+4.0, SOURCE.getZ()+8.5); }
   private static Vec3 close() { return Vec3.atCenterOf(target()).add(0, 3.0, 0.15); }

   static boolean tick(Minecraft mc, Path directory, String nonce) throws Exception {
      if (distance > 8) return true;
      if (stage == 0) {
         final int current = distance;
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
            for (BlockPos pos : BlockPos.betweenClosed(SOURCE.offset(-12,-1,-4), SOURCE.offset(3,5,4)))
               level.setBlock(pos, pos.getY()==SOURCE.getY()-1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            for (int d = -1; d <= 10; d++) {
               level.setBlock(SOURCE.west(d).north(), Blocks.BRICKS.defaultBlockState(), 3);
               level.setBlock(SOURCE.west(d).south(), Blocks.BRICKS.defaultBlockState(), 3);
            }
            level.setBlock(SOURCE.east(), Blocks.BRICKS.defaultBlockState(), 3);
            level.setBlock(SOURCE.west(10), Blocks.BRICKS.defaultBlockState(), 3);
            if (current > 0) level.setBlock(SOURCE.west(current),
               ModBlocks.STEEL_CATWALK_STAIRS.get().defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, Direction.EAST), 3);
            serverStarted = level.getGameTime();
            // Exactly one initial source; never call placeLiquid or set WATERLOGGED.
            level.setBlock(SOURCE, Blocks.WATER.defaultBlockState(), 3);
            Vec3 camera = overview();
            player.connection.teleport(camera.x, camera.y, camera.z, 0, 0);
         });
         stage = 1; ticks = 0; stableTicks = 0; previous = "";
         return false;
      }
      if (stage == 1) {
         if (!pending.isDone()) return false;
         pending.join();
         if (++ticks > 300) throw new IllegalStateException("Distance scene did not synchronize");
         if (!mc.level.getFluidState(SOURCE).isSource() || mc.player.position().distanceTo(overview()) > 0.1) return false;
         started = mc.level.getGameTime();
         mc.options.hideGui = true;
         look(mc, Vec3.atCenterOf(SOURCE.west(4)));
         mc.levelRenderer.allChanged();
         stage = 2; ticks = 0;
         return false;
      }
      if (stage == 2) {
         if (++ticks > 400) throw new IllegalStateException("Natural flow did not stabilize at distance "+distance);
         JsonArray channel = channel(mc.level);
         String signature = channel.toString();
         stableTicks = signature.equals(previous) ? stableTicks+1 : 0;
         previous = signature;
         if (mc.level.getGameTime()-started < 100 || stableTicks < 20) return false;
         result = new JsonObject();
         result.addProperty("case", distance==0 ? "unobstructed-control" : "distance-"+distance);
         result.addProperty("distance", distance);
         result.addProperty("settled_ticks", mc.level.getGameTime()-started);
         result.addProperty("unchanged_client_ticks", stableTicks);
         result.addProperty("initial_sources_placed", 1);
         result.addProperty("direct_cell_fill", false);
         result.add("source", sample(mc.level, SOURCE));
         result.add("target", sample(mc.level, target()));
         result.add("upstream", sample(mc.level, target().east()));
         result.add("downstream", sample(mc.level, target().west()));
         result.add("channel", channel);
         result.addProperty("mechanical_flow_in_cell", mc.level.getFluidState(target()).is(FluidTags.WATER));
         result.addProperty("mechanical_flow_after_cell", mc.level.getFluidState(target().west()).is(FluidTags.WATER));
         pending = mc.getSingleplayerServer().submit(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            var level = player.serverLevel();
            JsonObject authoritative = new JsonObject();
            authoritative.addProperty("elapsed_ticks",level.getGameTime()-serverStarted);
            authoritative.add("channel",channel(level));
            return authoritative;
         });
         stage = 20;
         return false;
      }
      if (stage == 20) {
         if (!pending.isDone()) return false;
         JsonObject authoritative = (JsonObject)pending.join();
         if (authoritative.get("elapsed_ticks").getAsLong() < 100) { stage=2; return false; }
         if (!authoritative.get("channel").equals(result.get("channel")))
            throw new IllegalStateException("Server/client flow states disagree at distance "+distance);
         result.addProperty("server_elapsed_ticks",authoritative.get("elapsed_ticks").getAsLong());
         result.add("server_channel",authoritative.get("channel"));
         result.addProperty("server_client_agree",true);
         result.addProperty("overview_screenshot", name("overview"));
         result.addProperty("overview_yaw",mc.player.getYRot());
         result.addProperty("overview_pitch",mc.player.getXRot());
         capture(mc, directory, name("overview"));
         pending = mc.getSingleplayerServer().submit(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            Vec3 camera = close();
            player.connection.teleport(camera.x,camera.y,camera.z,0,0);
         });
         stage = 3; ticks = 0;
         return false;
      }
      if (stage == 3) {
         if (!pending.isDone()) return false;
         pending.join();
         if (++ticks > 300) throw new IllegalStateException("Close camera did not synchronize");
         if (mc.player.position().distanceTo(close()) > 0.1) return false;
         look(mc, Vec3.atCenterOf(target()));
         stage = 4; ticks = 0;
         return false;
      }
      if (stage == 4) {
         if (++ticks < 25) return false;
         result.addProperty("close_screenshot", name("close"));
         result.addProperty("close_yaw",mc.player.getYRot());
         result.addProperty("close_pitch",mc.player.getXRot());
         capture(mc, directory, name("close"));
         RESULTS.add(result);
         distance++; stage = 0;
         if (distance > 8) {
            JsonObject proof = new JsonObject();
            proof.addProperty("suite","native-water-flow-distance-v1");
            proof.addProperty("nonce",nonce);
            proof.addProperty("status","captured");
            proof.addProperty("actual_integrated_world",true);
            proof.addProperty("orientation","east");
            proof.addProperty("source_position",SOURCE.toShortString());
            JsonObject mods = new JsonObject();
            for (String id : new String[]{"embeddium","modernfix"})
               net.neoforged.fml.ModList.get().getModContainerById(id).ifPresent(mod ->
                  mods.addProperty(id,mod.getModInfo().getVersion().toString()));
            proof.add("renderer_mods",mods);
            proof.add("cases",RESULTS);
            Files.writeString(directory.resolve("client-water-flow-distance-result.json"),
               new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");
            return true;
         }
      }
      return false;
   }

   private static JsonArray channel(net.minecraft.world.level.BlockGetter level) {
      JsonArray samples = new JsonArray();
      for (int d=0;d<=9;d++) samples.add(sample(level,SOURCE.west(d)));
      return samples;
   }
   private static JsonObject sample(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
      JsonObject sample = new JsonObject();
      var state = level.getBlockState(pos);
      var fluid = state.getFluidState();
      sample.addProperty("position",pos.toShortString());
      sample.addProperty("block_state",NbtUtils.writeBlockState(state).toString());
      sample.addProperty("fluid_state",fluid.toString());
      sample.addProperty("water",fluid.is(FluidTags.WATER));
      sample.addProperty("source",fluid.isSource());
      sample.addProperty("amount",fluid.getAmount());
      sample.addProperty("height",fluid.getHeight(level,pos));
      return sample;
   }
   private static String name(String angle) { return "flow-distance-"+distance+"-"+angle+".png"; }
   private static void capture(Minecraft mc, Path directory, String name) throws Exception {
      Path output=directory.resolve("canal-screenshots");
      Files.createDirectories(output);
      try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve(name)); }
   }
   private static void look(Minecraft mc, Vec3 target) {
      Vec3 delta=target.subtract(mc.player.getEyePosition());
      mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
      mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));
   }
}

