package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;

/** Runs only inside the existing nonce-guarded fresh integrated CI fixture. */
final class NativeFluidloggingValidation {
   private static final BlockPos POS = new BlockPos(5, 80, 5);
   private static final JsonArray RESULTS = new JsonArray();
   private static int index;
   private static int stage;
   private static int wait;
   private static CompletableFuture<?> pending;
   private static JsonObject result;

   private static List<Block> blocks() {
      return List.of(ModBlocks.STEEL_CATWALK.get(), ModBlocks.STEEL_CATWALK_STAIRS.get(),
         ModBlocks.STEEL_CATWALK_TA.get(), ModBlocks.STEEL_RAILING.get());
   }
   private static List<Fluid> fluids() {
      return List.of(Fluids.WATER, Fluids.LAVA, NativeFluidloggedBlock.creosote());
   }
   private static Block block() { return blocks().get((index % 12) / 3); }
   private static Fluid fluid() { return fluids().get(index % 3); }
   private static BlockState filled() {
      return NativeFluidloggedBlock.withFluidContent(block().defaultBlockState(),
         NativeFluidloggedBlock.FluidContent.fromFluidState(((net.minecraft.world.level.material.FlowingFluid)fluid()).getSource(false)));
   }

   static boolean tick(Minecraft mc, Path directory, String nonce) throws Exception {
      if (index == 24) return true;
      check(++wait < 400, "fluidlogging client phase stalled: " + index + "/" + stage);
      if (stage == 0) {
         BlockState expected = filled();
         pending = mc.getSingleplayerServer().submit(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            var level = player.serverLevel();
            for (Direction side : Direction.values()) level.setBlock(POS.relative(side),
               side == Direction.DOWN ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            // update cases exercise ordinary block updates; chunk cases suppress that
            // update and send the actual full initial-load packet from the server chunk.
            level.setBlock(POS, expected, index < 12 ? 3 : 0);
            if (index >= 12) player.connection.send(new ClientboundLevelChunkWithLightPacket(
               level.getChunkAt(POS), level.getLightEngine(), null, null));
            check(level.getFluidState(POS).getType() == fluid(), "server did not retain expected source");
         });
         stage = 1;
         wait = 0;
         return false;
      }
      if (stage == 1) {
         if (!pending.isDone()) return false;
         pending.join();
         BlockState received = mc.level.getBlockState(POS);
         if (!received.equals(filled())) return false;
         check(mc.level.getFluidState(POS).getType() == fluid(), "client world fluid differs from block state");
         check(mc.level.getChunkAt(POS).getFluidState(POS).getType() == fluid(), "client chunk fluid differs");
         var sprites = FluidSpriteCache.getFluidSprites(mc.level, POS, received.getFluidState());
         check(sprites[0] != null && !sprites[0].contents().name().getPath().equals("missingno"),
            "fluid still sprite is missing");
         check(sprites[1] != null && !sprites[1].contents().name().getPath().equals("missingno"),
            "fluid flowing sprite is missing");
         Mesh mesh = new Mesh();
         mc.getBlockRenderer().renderLiquid(POS, mc.level, mesh, received, received.getFluidState());
         check(mesh.vertices > 0 && mesh.vertices % 4 == 0 && mesh.uvs >= mesh.vertices,
            "native fluid tessellation produced no complete textured quads");
         result = new JsonObject();
         result.addProperty("block", BuiltInRegistries.BLOCK.getKey(block()).toString());
         result.addProperty("fluid", BuiltInRegistries.FLUID.getKey(fluid()).toString());
         result.addProperty("sync", index < 12 ? "update" : "chunk");
         result.addProperty("client_world_and_chunk_source", true);
         result.addProperty("native_textured_vertices", mesh.vertices);
         pending = mc.getSingleplayerServer().submit(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            var level = player.serverLevel();
            check(((NativeFluidloggedBlock)block()).pickupBlock(player, level, POS, level.getBlockState(POS))
               .is(fluid().getBucket()), "server drain failed during client synchronization check");
         });
         stage = 2;
         wait = 0;
         return false;
      }
      if (!pending.isDone()) return false;
      pending.join();
      if (!mc.level.getBlockState(POS).equals(block().defaultBlockState())) return false;
      check(mc.level.getFluidState(POS).isEmpty(), "client retained source after drain update");
      result.addProperty("client_drain_cleared", true);
      RESULTS.add(result);
      index++;
      stage = 0;
      wait = 0;
      if (index == 24) {
         JsonObject proof = new JsonObject();
         proof.addProperty("suite", "native-fluidlogging-client-v1");
         proof.addProperty("nonce", nonce);
         proof.addProperty("status", "passed");
         proof.addProperty("actual_integrated_world", true);
         proof.add("cases", RESULTS);
         Files.writeString(directory.resolve("client-fluidlogging-result.json"),
            new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
         return true;
      }
      return false;
   }

   private static void check(boolean condition, String message) {
      if (!condition) throw new IllegalStateException(message);
   }

   static final class Mesh implements VertexConsumer {
      int vertices;
      int uvs;
      @Override public VertexConsumer addVertex(float x, float y, float z) {
         check(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z), "invalid fluid vertex");
         vertices++;
         return this;
      }
      @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
      @Override public VertexConsumer setUv(float u, float v) {
         check(Float.isFinite(u) && Float.isFinite(v), "invalid fluid texture coordinate");
         uvs++;
         return this;
      }
      @Override public VertexConsumer setUv1(int u, int v) { return this; }
      @Override public VertexConsumer setUv2(int u, int v) { return this; }
      @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
   }
}
