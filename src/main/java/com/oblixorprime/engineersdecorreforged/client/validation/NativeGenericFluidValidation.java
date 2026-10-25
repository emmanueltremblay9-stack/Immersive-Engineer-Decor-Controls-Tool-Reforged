package com.oblixorprime.engineersdecorreforged.client.validation;
import com.google.gson.*;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.*;
import com.oblixorprime.engineersdecorreforged.fluid.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.*;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/** Real update and initial attachment payloads; only this sync fixture freezes scheduled fluid ticks. */
final class NativeGenericFluidValidation {
   private static final BlockPos POS=new BlockPos(5,80,5);
   private static final String[] FLUIDS={"minecraft:water","minecraft:lava","immersiveengineering:creosote","immersiveengineering:ethanol","immersiveengineering:biodiesel","immersiveengineering:plantoil"};
   private static final JsonArray RESULTS=new JsonArray();
   private static int index,stage,wait;private static CompletableFuture<?> pending;
   private static FlowingFluid fluid(){return (FlowingFluid)BuiltInRegistries.FLUID.get(ResourceLocation.parse(FLUIDS[index%6]));}
   private static FluidState expected(){return switch((index/6)%5){case 0->fluid().getSource(false);case 1->fluid().getFlowing(7,false);case 2->fluid().getFlowing(3,false);case 3->fluid().getFlowing(8,true);default->fluid().getSource(true);};}
   private static BlockState state(){
      var dry=ModBlocks.STEEL_CATWALK.get().defaultBlockState();var fluid=expected();
      if(fluid.isSource())return NativeFluidloggedBlock.placement(dry,fluid);
      return fluid.getType().isSame(Fluids.WATER)?StairWaterFlow.withFlow(dry,fluid):dry;
   }
   static boolean tick(Minecraft mc,Path directory,String nonce)throws Exception {
      if(index==60)return ExternalFluidClientValidation.tick(mc,directory,nonce);check(++wait<400,"sync fixture stalled");
      if(stage==0){
         if(index>=30){mc.level.getChunkAt(POS).removeData(NativeFluidCells.CELLS.get());check(!NativeFluidCells.snapshot(mc.level.getChunkAt(POS)).contains(POS),"initial case must start without cached data");}
         pending=mc.getSingleplayerServer().submit(()->{
            var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();var chunk=level.getChunkAt(POS);
            for(var side:Direction.values())level.setBlock(POS.relative(side),side==Direction.DOWN?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
            level.setBlock(POS,state(),index<30?3:0);
            var cells=NativeFluidCells.snapshot(chunk).with(POS,state().getFluidState().isEmpty()?expected():Fluids.EMPTY.defaultFluidState());
            if(index<30)chunk.setData(NativeFluidCells.CELLS.get(),cells);
            else {
               // Disk loading emits no delta: this case requires the actual initial attachment payload.
               var tag=new CompoundTag();tag.put("immersive_engineer_decor_controls_tool_reforged:fluid_cells",cells.encode());
               chunk.readAttachmentsFromNBT(level.registryAccess(),tag);
               player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk,level.getLightEngine(),null,null));
               AttachmentSync.onChunkSent(new ChunkWatchEvent.Sent(player,chunk,level));
            }
            level.getFluidTicks().clearArea(BoundingBox.fromCorners(POS.offset(-2,-2,-2),POS.offset(2,2,2)));
            check(level.getFluidState(POS).equals(expected()),"server fluid differs");
         });stage=1;wait=0;return false;
      }
      if(!pending.isDone())return false;pending.join();
      if(stage==1){
         if(!mc.level.getFluidState(POS).equals(expected()) || !mc.level.getBlockState(POS).equals(state()))return false;
         check(mc.level.getChunkAt(POS).getFluidState(POS).equals(expected()),"client chunk differs");
         // Exercise the client chunk path used when a predicted removal is later corrected.
         mc.level.setBlock(POS,Blocks.AIR.defaultBlockState(),3);
         check(mc.level.getFluidState(POS).isEmpty(),"removed client block must mask its cell");
         mc.level.setBlock(POS,state(),3);
         check(mc.level.getFluidState(POS).equals(expected()),"client prediction restore lost authoritative cell");
         var region=new net.minecraft.client.renderer.chunk.RenderRegionCache().createRegion(mc.level,SectionPos.of(POS),false);
         check(region!=null && region.getFluidState(POS).equals(expected()),"render snapshot differs");
         var mesh=new NativeFluidloggingValidation.Mesh();mc.getBlockRenderer().renderLiquid(POS,region,mesh,state(),expected());
         check(mesh.vertices>0 && mesh.vertices%4==0 && mesh.uvs>=mesh.vertices,"textured fluid mesh missing");
         pending=mc.getSingleplayerServer().submit(()->{
            var level=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();
            NativeFluidCells.set(level,POS,Fluids.EMPTY.defaultFluidState());level.setBlock(POS,ModBlocks.STEEL_CATWALK.get().defaultBlockState(),3);
         });stage=2;wait=0;return false;
      }
      if(!mc.level.getFluidState(POS).isEmpty())return false;
      var row=new JsonObject();row.addProperty("fluid",FLUIDS[index%6]);row.addProperty("state",(index/6)%5);row.addProperty("sync",index<30?"update":"initial");row.addProperty("server_client_region_agree",true);row.addProperty("drain_cleared",true);row.addProperty("local_prediction_restore",true);RESULTS.add(row);
      index++;stage=0;wait=0;
      if(index==60){var proof=new JsonObject();proof.addProperty("suite","generic-fluid-client-v1");proof.addProperty("nonce",nonce);proof.addProperty("status","passed");proof.add("cases",RESULTS);Files.writeString(directory.resolve("client-generic-fluid-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");}return false;
   }
   private static void check(boolean ok,String text){if(!ok)throw new IllegalStateException(text+" case="+index);}
}
