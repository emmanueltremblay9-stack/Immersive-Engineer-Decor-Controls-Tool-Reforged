package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Diagnostic fixture; runs only inside the existing nonce-guarded fresh client world. */
final class NativeWaterCanalValidation {
   private static final BlockPos POS = new BlockPos(5, 80, 5);
   private static final String[] NAMES = {"stairs-wet", "stairs-dry-control", "catwalk-wet", "raised-wet", "stairs-railed-wet", "stairs-railed-reversed-wet", "stairs-flow-adjacent-dry", "stairs-flow-adjacent-stored"};
   private static final JsonArray RESULTS = new JsonArray();
   private static int index;
   private static int stage;
   private static int ticks;
   private static CompletableFuture<?> pending;
   private static JsonObject result;

   private static BlockState state() {
      BlockState state = switch (index) {
         case 0, 1, 4, 5, 6, 7 -> ModBlocks.STEEL_CATWALK_STAIRS.get().defaultBlockState()
            .setValue(PortedBlocks.HORIZONTAL_FACING, index == 5 ? Direction.SOUTH : Direction.NORTH);
         case 2 -> ModBlocks.STEEL_CATWALK.get().defaultBlockState();
         default -> ModBlocks.STEEL_CATWALK_TA.get().defaultBlockState();
      };
      if (index >= 4) state = state.setValue(PortedBlocks.LEFT_RAILING, true)
         .setValue(PortedBlocks.RIGHT_RAILING, true).setValue(PortedBlocks.EDITED_STAIR_SIDES, 3);
      return state.setValue(BlockStateProperties.WATERLOGGED, index != 1 && index != 6);
   }

   static boolean tick(Minecraft mc, Path directory, String nonce) throws Exception {
      if (index == NAMES.length) return true;
      if (stage == 0) {
         BlockState expected = state();
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
            for (BlockPos pos : BlockPos.betweenClosed(POS.offset(-4, -1, -5), POS.offset(4, 5, 5)))
               level.setBlock(pos, pos.getY() == POS.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            for (int z = -4; z <= 1; z++) {
               level.setBlock(POS.offset(-1, 0, z), Blocks.BRICKS.defaultBlockState(), 3);
               level.setBlock(POS.offset(1, 0, z), Blocks.BRICKS.defaultBlockState(), 3);
            }
            level.setBlock(POS.south(), Blocks.BRICKS.defaultBlockState(), 3);
            level.setBlock(POS.north(4), Blocks.BRICKS.defaultBlockState(), 3);
            level.setBlock(POS, expected, 3);
            if (index >= 6) level.setBlock(POS.north(3), Blocks.WATER.defaultBlockState(), 3);
            else if (index != 1) for (int z = 1; z <= 3; z++) level.setBlock(POS.north(z), Blocks.WATER.defaultBlockState(), 3);
            player.connection.teleport(POS.getX()+3.5, POS.getY()+2.0, POS.getZ()+4.5, 0, 0);
         });
         stage = 1;
         ticks = 0;
         return false;
      }
      if (stage == 1) {
         if (!pending.isDone()) return false;
         pending.join();
         if (++ticks > 300) throw new IllegalStateException("Canal did not synchronize: " + NAMES[index]);
         if (!mc.level.getBlockState(POS).equals(state()) || mc.player.position().distanceTo(
            new Vec3(POS.getX()+3.5,POS.getY()+2.0,POS.getZ()+4.5)) > 0.1) return false;
         mc.options.hideGui = true;
         look(mc, Vec3.atCenterOf(POS).add(0, 0.2, -0.5));
         mc.levelRenderer.allChanged();
         stage = 2;
         ticks = 0;
         return false;
      }
      if (stage == 2) {
         if (++ticks < 50) return false;
         BlockState client = mc.level.getBlockState(POS);
         Mesh mesh = new Mesh(POS.getY() & 15);
         if (!client.getFluidState().isEmpty()) mc.getBlockRenderer().renderLiquid(POS, mc.level, mesh, client, client.getFluidState());
         result = new JsonObject();
         result.addProperty("case", NAMES[index]);
         result.addProperty("block_state", NbtUtils.writeBlockState(client).toString());
         result.addProperty("fluid_state", client.getFluidState().toString());
         result.addProperty("vertices", mesh.vertices);
         result.addProperty("upper_horizontal_quads", mesh.upperQuads);
         result.addProperty("upper_surface_quads", mesh.upperSurfaceQuads);
         result.addProperty("source", client.getFluidState().isSource());
         result.addProperty("cell_fluid_height", client.getFluidState().getHeight(mc.level, POS));
         var adjacent = mc.level.getFluidState(POS.north());
         result.addProperty("adjacent_fluid_state", adjacent.toString());
         result.addProperty("adjacent_source", adjacent.isSource());
         result.addProperty("adjacent_water", adjacent.is(net.minecraft.tags.FluidTags.WATER));
         result.addProperty("adjacent_fluid_height", adjacent.getHeight(mc.level, POS.north()));
         capture(mc, directory, NAMES[index]+"-oblique.png");
         result.addProperty("oblique_screenshot", NAMES[index]+"-oblique.png");
         pending = mc.getSingleplayerServer().submit(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            player.connection.teleport(POS.getX()+0.5, POS.getY()+1.5, POS.getZ()+0.8, 0, 90);
         });
         stage = 3;
         ticks = 0;
         return false;
      }
      if (stage == 3) {
         if (!pending.isDone()) return false;
         pending.join();
         look(mc, Vec3.atCenterOf(POS));
         if (++ticks > 300) throw new IllegalStateException("Canal overhead camera did not synchronize");
         if (mc.player.position().distanceTo(new Vec3(POS.getX()+0.5, POS.getY()+1.5, POS.getZ()+0.8)) > 0.1) return false;
         stage = 4;
         ticks = 0;
         return false;
      }
      if (stage == 4) {
         if (++ticks < 40) return false;
         capture(mc, directory, NAMES[index]+"-overhead.png");
         result.addProperty("overhead_screenshot", NAMES[index]+"-overhead.png");
         RESULTS.add(result);
         index++;
         stage = 0;
         if (index == NAMES.length) {
            JsonObject proof = new JsonObject();
            proof.addProperty("suite", "native-water-canal-diagnostic-v1");
            proof.addProperty("nonce", nonce);
            proof.addProperty("status", "captured");
            proof.add("cases", RESULTS);
            Files.writeString(directory.resolve("client-water-canal-result.json"),
               new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");
            return true;
         }
      }
      return false;
   }

   private static void capture(Minecraft mc, Path directory, String name) throws Exception {
      Path output = directory.resolve("canal-screenshots");
      Files.createDirectories(output);
      try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve(name)); }
   }
   private static void look(Minecraft mc, Vec3 target) {
      Vec3 delta = target.subtract(mc.player.getEyePosition());
      mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
      mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x*delta.x+delta.z*delta.z))));
   }

   private static final class Mesh implements VertexConsumer {
      int vertices;
      int upperQuads;
      int upperSurfaceQuads;
      private final int originY;
      private final float[] y = new float[4];
      private final float[] xzX = new float[4];
      private final float[] xzZ = new float[4];
      Mesh(int originY) { this.originY = originY; }
      @Override public VertexConsumer addVertex(float x, float height, float z) {
         y[vertices % 4] = height;
         xzX[vertices % 4] = x;
         xzZ[vertices % 4] = z;
         if (++vertices % 4 == 0 && y[0] > originY+0.5f && Math.abs(y[0]-y[1])<0.001
            && Math.abs(y[0]-y[2])<0.001 && Math.abs(y[0]-y[3])<0.001) upperQuads++;
         if (vertices % 4 == 0) {
            // A sloped top still projects an area onto XZ; a vertical side does not.
            float area = 0;
            boolean upper = true;
            for (int i = 0; i < 4; i++) {
               int next = (i + 1) % 4;
               area += xzX[i] * xzZ[next] - xzX[next] * xzZ[i];
               upper &= y[i] > originY + 0.5f;
            }
            if (upper && Math.abs(area) > 0.0001f) upperSurfaceQuads++;
         }
         return this;
      }
      @Override public VertexConsumer setColor(int r,int g,int b,int a) { return this; }
      @Override public VertexConsumer setUv(float u,float v) { return this; }
      @Override public VertexConsumer setUv1(int u,int v) { return this; }
      @Override public VertexConsumer setUv2(int u,int v) { return this; }
      @Override public VertexConsumer setNormal(float x,float y,float z) { return this; }
   }
}
