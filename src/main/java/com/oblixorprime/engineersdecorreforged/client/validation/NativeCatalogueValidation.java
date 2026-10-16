package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.*;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.*;
import com.oblixorprime.engineersdecorreforged.rsgauges.ControlsModule;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Dedicated screenshots in the existing nonce-guarded synthetic integrated world only. */
final class NativeCatalogueValidation {
   private static final BlockPos POS=new BlockPos(5,80,5);
   private static final Vec3 CAMERA=new Vec3(7.5,82.7,1.5);
   private static final JsonArray RESULTS=new JsonArray();
   private static int index,stage,ticks;
   private static CompletableFuture<?> pending;
   private static BlockState serverState;
   private static JsonObject result;
   private static List<Block> blocks() {
      var list=new ArrayList<Block>(List.of(ModBlocks.IRON_BULB_LIGHT.get(),ModBlocks.IRON_INSET_LIGHT.get(),
         ModBlocks.IRON_CEILING_EDGE_LIGHT.get(),ModBlocks.IRON_FLOOR_EDGE_LIGHT.get()));
      for(String name:List.of("industrial_lever","industrial_button","industrial_contact_mat"))
         list.add(ControlsModule.CONTROLS.stream().filter(e->e.getId().getPath().equals(name)).findFirst().orElseThrow().get());
      list.add(ModBlocks.STEEL_CATWALK_TA.get());return list;
   }
   private static Block block(){return blocks().get(index/4);}
   private static boolean natural(){return index%4==3;}
   private static String mode(){return new String[]{"water","lava","creosote","natural-water"}[index%4];}
   private static Fluid fluid(){return switch(index%4){case 1->Fluids.LAVA;case 2->NativeFluidloggedBlock.creosote();default->Fluids.WATER;};}
   private static BlockState dry(){
      var state=block().defaultBlockState();
      if(block() instanceof RaisedCatwalkBlock)state=state.setValue(RaisedCatwalkBlock.VARIANT,4).setValue(RaisedCatwalkBlock.PAID_LIGHT,true);
      return state;
   }
   static boolean tick(Minecraft mc,Path directory,String nonce)throws Exception {
      if(index==blocks().size()*4)return true;
      if(stage==0){
         pending=mc.getSingleplayerServer().submit(()->{
            var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
            for(BlockPos p:BlockPos.betweenClosed(POS.offset(-3,-1,-3),POS.offset(4,4,3)))
               level.setBlock(p,p.getY()==79?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
            for(int x=-2;x<=3;x++){
               level.setBlock(POS.east(x).north(),Blocks.BRICKS.defaultBlockState(),3);
               level.setBlock(POS.east(x).south(),Blocks.BRICKS.defaultBlockState(),3);
            }
            level.setBlock(POS.west(2),Blocks.BRICKS.defaultBlockState(),3);level.setBlock(POS.east(3),Blocks.BRICKS.defaultBlockState(),3);
            var placed=natural()?dry():NativeFluidloggedBlock.placement(dry(),((net.minecraft.world.level.material.FlowingFluid)fluid()).getSource(false));
            level.setBlock(POS,placed,3);
            if(natural())level.setBlock(POS.west(),Blocks.WATER.defaultBlockState(),3);
            player.connection.teleport(CAMERA.x,CAMERA.y,CAMERA.z,0,0);
         });stage=1;ticks=0;return false;
      }
      if(stage==1){
         if(!pending.isDone())return false;pending.join();
         check(++ticks<300,"catalogue camera/world sync stalled");
         if(mc.player.position().distanceTo(CAMERA)>.1 || !mc.level.getBlockState(POS).is(block()))return false;
         mc.options.hideGui=true;look(mc);mc.levelRenderer.allChanged();stage=2;ticks=0;return false;
      }
      look(mc);
      if(stage==2){
         if(++ticks<(natural()?60:20))return false;
         pending=mc.getSingleplayerServer().submit(()->{
            var level=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();serverState=level.getBlockState(POS);
         });stage=3;return false;
      }
      if(stage==3){
         if(!pending.isDone())return false;pending.join();var state=mc.level.getBlockState(POS);var flow=state.getFluidState();
         check(state.equals(serverState),"catalogue server/client disagreement");check(state.is(block()),"catalogue block replaced");
         check(!flow.isEmpty() && flow.getType().isSame(fluid()),"catalogue expected fluid missing");
         check(flow.isSource()!=natural(),"natural flow must not become stored source");check(flow.getAmount()==(natural()?7:8),"catalogue fluid amount differs from vanilla");
         for(var property:dry().getProperties())if(!Set.of("waterlogged","fluid_cell","water_flow").contains(property.getName()))
            check(state.getValue(property).equals(dry().getValue(property)),"fluid changed geometry/paid lamp/signal property "+property);
         Mesh mesh=new Mesh();mc.getBlockRenderer().renderLiquid(POS,mc.level,mesh,state,flow);check(mesh.vertices>0,"catalogue fluid has no rendered surface");
         result=new JsonObject();result.addProperty("block",BuiltInRegistries.BLOCK.getKey(block()).toString());result.addProperty("mode",mode());
         result.addProperty("source_before",flow.isSource());result.addProperty("amount_before",flow.getAmount());result.addProperty("fluid_vertices",mesh.vertices);
         result.addProperty("server_client_agree",true);result.addProperty("geometry_and_paid_state_preserved",true);
         result.addProperty("before",capture(mc,directory,"before"));
         pending=mc.getSingleplayerServer().submit(()->{
            var level=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();
            if(natural())level.setBlock(POS.west(),Blocks.AIR.defaultBlockState(),3);
            else check(((NativeFluidloggedBlock)block()).pickupBlock(null,level,POS,level.getBlockState(POS)).is(fluid().getBucket()),"catalogue source pickup failed");
         });stage=4;ticks=0;return false;
      }
      if(stage==4){
         if(!pending.isDone())return false;pending.join();if(++ticks<(natural()?100:80))return false;
         pending=mc.getSingleplayerServer().submit(()->{
            var level=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();serverState=level.getBlockState(POS);
            check(serverState.equals(dry()) && serverState.getFluidState().isEmpty(),"server catalogue recession changed geometry, signal or paid state");
         });stage=5;return false;
      }
      if(!pending.isDone())return false;pending.join();var state=mc.level.getBlockState(POS);
      check(state.equals(serverState) && state.equals(dry()) && state.getFluidState().isEmpty(),"client catalogue recession changed geometry, signal or paid state");
      result.addProperty("after_empty",true);result.addProperty("after",capture(mc,directory,"after"));RESULTS.add(result);
      index++;stage=0;ticks=0;
      if(index==blocks().size()*4){
         var proof=new JsonObject();proof.addProperty("suite","native-catalogue-visual-v1");proof.addProperty("nonce",nonce);proof.addProperty("status","passed");
         proof.add("cases",RESULTS);Files.writeString(directory.resolve("client-catalogue-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");return true;
      }
      return false;
   }
   private static String capture(Minecraft mc,Path directory,String suffix)throws Exception{
      String name=BuiltInRegistries.BLOCK.getKey(block()).getPath()+"-"+mode()+"-"+suffix+".png";
      Path output=directory.resolve("catalogue-screenshots");Files.createDirectories(output);
      try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve(name));}return name;
   }
   private static void look(Minecraft mc){var d=Vec3.atCenterOf(POS).subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));}
   private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message+" case="+index+"/"+mode());}
   private static final class Mesh implements VertexConsumer{
      int vertices;
      public VertexConsumer addVertex(float x,float y,float z){vertices++;return this;}
      public VertexConsumer setColor(int r,int g,int b,int a){return this;}
      public VertexConsumer setUv(float u,float v){return this;}
      public VertexConsumer setUv1(int u,int v){return this;}
      public VertexConsumer setUv2(int u,int v){return this;}
      public VertexConsumer setNormal(float x,float y,float z){return this;}
   }
}
