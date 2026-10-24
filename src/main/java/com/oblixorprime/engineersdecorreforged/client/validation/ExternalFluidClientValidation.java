package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.*;
import com.oblixorprime.engineersdecorreforged.fluid.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.*;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

/** Uses the existing isolated client harness and real attachment packets, on every renderer profile. */
final class ExternalFluidClientValidation {
   private static final BlockPos POS = new BlockPos(5,80,5);
   private static final String[] BLOCKS = {
      "minecraft:oak_stairs", "minecraft:stone_slab", "minecraft:oak_fence", "minecraft:cobblestone_wall",
      "minecraft:glass_pane", "minecraft:red_stained_glass_pane", "minecraft:iron_bars", "minecraft:ladder",
      "minecraft:oak_trapdoor", "minecraft:cut_copper_stairs", "minecraft:cut_copper_slab", "minecraft:copper_trapdoor",
      "immersiveengineering:stairs_treated_wood_horizontal", "immersiveengineering:stairs_treated_wood_vertical",
      "immersiveengineering:stairs_treated_wood_packaged", "immersiveengineering:slab_treated_wood_horizontal",
      "immersiveengineering:slab_treated_wood_vertical", "immersiveengineering:slab_treated_wood_packaged",
      "immersiveengineering:wall_clinker_brick", "immersiveengineering:treated_trapdoor",
      "immersiveengineering:treated_trapdoor_framed", "immersiveengineering:steel_trapdoor"};
   private static final int PER_SYNC = BLOCKS.length * 6;
   private static final JsonArray RESULTS = new JsonArray();
   private static int index, stage, wait;
   private static CompletableFuture<?> pending;

   private static String blockId() { return BLOCKS[(index / 6) % BLOCKS.length]; }
   private static FluidState expected() {
      return switch (index % 6) {
         case 0 -> Fluids.WATER.getSource(false);
         case 1 -> Fluids.FLOWING_WATER.getFlowing(7,false);
         case 2 -> Fluids.FLOWING_WATER.getFlowing(8,true);
         case 3 -> Fluids.LAVA.getSource(false);
         case 4 -> Fluids.FLOWING_LAVA.getFlowing(7,false);
         default -> ((FlowingFluid)BuiltInRegistries.FLUID.get(ResourceLocation.parse("immersiveengineering:ethanol"))).getFlowing(7,false);
      };
   }
   private static BlockState state() {
      return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId())).defaultBlockState()
         .setValue(BlockStateProperties.WATERLOGGED, index % 6 == 0);
   }
   static boolean tick(Minecraft mc, Path directory, String nonce) throws Exception {
      if (index == PER_SYNC * 2) return true;
      check(++wait < 400, "external sync stalled");
      if (stage == 0) {
         if (index >= PER_SYNC) mc.level.getChunkAt(POS).removeData(NativeFluidCells.CELLS.get());
         pending = mc.getSingleplayerServer().submit(() -> {
            var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            var level=player.serverLevel();var chunk=level.getChunkAt(POS);
            for(var direction:Direction.values()) level.setBlock(POS.relative(direction),direction==Direction.DOWN || direction==Direction.SOUTH
               ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),3);
            check(ExternalFluidAdapter.supported(state()),"client fixture block is not qualified");
            level.setBlock(POS,state(),index<PER_SYNC?3:0);
            var cells=NativeFluidCells.snapshot(chunk).with(POS,state().getFluidState().isEmpty()?expected():Fluids.EMPTY.defaultFluidState());
            if(index<PER_SYNC) chunk.setData(NativeFluidCells.CELLS.get(),cells);
            else {
               var tag=new CompoundTag();tag.put("immersive_engineer_decor_controls_tool_reforged:fluid_cells",cells.encode());
               chunk.readAttachmentsFromNBT(level.registryAccess(),tag);
               NativeFluidCells.refreshLight(chunk,POS,cells);
               player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk,level.getLightEngine(),null,null));
               AttachmentSync.onChunkSent(new ChunkWatchEvent.Sent(player,chunk,level));
            }
            level.getFluidTicks().clearArea(BoundingBox.fromCorners(POS.offset(-2,-2,-2),POS.offset(2,2,2)));
            check(level.getFluidState(POS).equals(expected()),"server fluid differs");
         });stage=1;wait=0;return false;
      }
      if(!pending.isDone())return false;pending.join();
      if(stage==1) {
         if(!mc.level.getFluidState(POS).equals(expected()) || !mc.level.getBlockState(POS).equals(state()))return false;
         check(mc.level.getChunkAt(POS).getFluidState(POS).equals(expected()),"client chunk differs");
         mc.level.setBlock(POS,Blocks.AIR.defaultBlockState(),3);
         check(mc.level.getFluidState(POS).isEmpty(),"prediction masks removed block");
         mc.level.setBlock(POS,state(),3);
         check(mc.level.getFluidState(POS).equals(expected()),"prediction restoration lost authoritative cell");
         var region=new net.minecraft.client.renderer.chunk.RenderRegionCache().createRegion(mc.level,SectionPos.of(POS),false);
         check(region!=null && region.getFluidState(POS).equals(expected()),"external render snapshot differs");
         var mesh=new NativeFluidloggingValidation.Mesh();mc.getBlockRenderer().renderLiquid(POS,region,mesh,state(),expected());
         check(mesh.vertices>0 && mesh.vertices%4==0 && mesh.uvs>=mesh.vertices,"external textured mesh missing");
         int light=expected().getFluidType().getLightLevel();
         check(mc.level.getBlockState(POS).getLightEmission(mc.level,POS)==light,"client auxiliary light differs");
         pending=mc.getSingleplayerServer().submit(()->{
            var level=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();
            NativeFluidCells.set(level,POS,Fluids.EMPTY.defaultFluidState());
            level.setBlock(POS,state().setValue(BlockStateProperties.WATERLOGGED,false),3);
         });stage=2;wait=0;return false;
      }
      if(!mc.level.getFluidState(POS).isEmpty())return false;
      check(mc.level.getBlockState(POS).getLightEmission(mc.level,POS)==0,"drain left stale client light");
      var row=new JsonObject();row.addProperty("block",blockId());row.addProperty("state",index%6);row.addProperty("sync",index<PER_SYNC?"update":"initial");
      for(var field:new String[]{"server_client_region_agree","drain_cleared","local_prediction_restore","textured_mesh","light_cleared"})row.addProperty(field,true);
      RESULTS.add(row);index++;stage=0;wait=0;
      if(index==PER_SYNC*2) {
         var path=directory.resolve("client-generic-fluid-result.json");
         var proof=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
         check(proof.get("nonce").getAsString().equals(nonce),"external result must extend current native proof");
         var external=new JsonObject();external.addProperty("suite","external-fluid-client-v1");external.addProperty("nonce",nonce);
         external.addProperty("status","passed");external.add("cases",RESULTS);proof.add("external",external);
         Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");return true;
      }
      return false;
   }
   private static void check(boolean condition,String text){if(!condition)throw new IllegalStateException(text+" externalCase="+index);}
}
