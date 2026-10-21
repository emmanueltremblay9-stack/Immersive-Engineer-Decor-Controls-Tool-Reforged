package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.*;
import com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.Vec3;

/** Opt-in diagnostic in the nonce-owned fresh integrated world, never a user's save. */
final class NativeLavaSurfaceValidation {
   private static final BlockPos ORIGIN=new BlockPos(8,80,8);
   private static final Vec3 CAMERA=new Vec3(12.5,81.5,14);
   private static final JsonArray RESULTS=new JsonArray();
   private static int index,stage,ticks;
   private static CompletableFuture<?> pending;
   private static JsonArray serverStates;
   private static Object embeddium;
   private static Method embeddiumHeight;
   private static boolean embeddiumChecked;
   private static boolean decor(){return index%2==1;}
   private static boolean natural(){return index>=2;}
   static boolean tick(Minecraft mc,Path directory,String nonce)throws Exception {
      if(index==4)return true;
      if(stage==0){
         pending=mc.getSingleplayerServer().submit(()->{
            var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
            for(var p:BlockPos.betweenClosed(ORIGIN.offset(-6,-2,-6),ORIGIN.offset(6,5,6))) {
               NativeFluidCells.set(level,p,Fluids.EMPTY.defaultFluidState());
               level.setBlock(p,p.getY()==79?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
            }
            // Same stepped support in both baseline and decorative scenes.
            level.setBlock(ORIGIN.above(),Blocks.STONE.defaultBlockState(),3);
            if(decor()){
               for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                  if(x==0 && z==0)continue;
                  BlockState state=(x%2==0?ModBlocks.STEEL_CATWALK_STAIRS.get():ModBlocks.THIN_STEEL_POLE.get()).defaultBlockState();
                  if(state.hasProperty(DirectionalBlock.FACING))state=state.setValue(DirectionalBlock.FACING,Direction.UP);
                  if(state.hasProperty(PortedBlocks.HORIZONTAL_FACING))state=state.setValue(PortedBlocks.HORIZONTAL_FACING,Direction.SOUTH);
                  level.setBlock(ORIGIN.offset(x,0,z),state,3);
               }
            }
            if(natural())level.setBlock(ORIGIN.above(2),Blocks.LAVA.defaultBlockState(),3);
            else {
               for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                  if(x==0 && z==0)continue;
                  var p=ORIGIN.offset(x,0,z);var f=Fluids.FLOWING_LAVA.getFlowing(Math.max(1,8-2*Math.max(Math.abs(x),Math.abs(z))),false);
                  if(decor())NativeFluidCells.set(level,p,f);else level.setBlock(p,f.createLegacyBlock(),3);
               }
               level.setBlock(ORIGIN,Fluids.FLOWING_LAVA.getFlowing(8,true).createLegacyBlock(),3);
               level.setBlock(ORIGIN.above(2),Blocks.LAVA.defaultBlockState(),3);
               level.getFluidTicks().clearArea(BoundingBox.fromCorners(ORIGIN.offset(-7,-3,-7),ORIGIN.offset(7,6,7)));
            }
            player.connection.teleport(CAMERA.x,CAMERA.y,CAMERA.z,0,0);
         });stage=1;ticks=0;return false;
      }
      if(stage==1){
         if(!pending.isDone())return false;pending.join();
         if(++ticks>300)throw new IllegalStateException("lava fixture sync timeout");
         if(mc.player.position().distanceTo(CAMERA)>.1 || !mc.level.getFluidState(ORIGIN.above(2)).isSource())return false;
         mc.options.hideGui=true;mc.levelRenderer.allChanged();stage=2;ticks=0;return false;
      }
      var delta=Vec3.atCenterOf(ORIGIN).subtract(mc.player.getEyePosition());
      mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));
      if(stage==2){
         if(++ticks<(natural()?500:45))return false;
         pending=mc.getSingleplayerServer().submit(()->{
            var level=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();
            level.getFluidTicks().clearArea(BoundingBox.fromCorners(ORIGIN.offset(-7,-3,-7),ORIGIN.offset(7,6,7)));
            serverStates=states(level);
         });stage=3;ticks=0;return false;
      }
      if(!pending.isDone())return false;pending.join();
      if(++ticks<5)return false;
      var actual=states(mc.level);if(!actual.equals(serverStates))throw new IllegalStateException("lava server/client disagreement");
      var meshes=new JsonArray();int mismatch=0,attachments=0,heightMismatch=0,topCount=0,embeddiumProbes=0,embeddiumMismatch=0;
      if(!embeddiumChecked){
         embeddiumChecked=true;
         try{
            var type=Class.forName("org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.FluidRenderer");
            var colors=Class.forName("org.embeddedt.embeddium.impl.model.color.ColorProviderRegistry");
            var lights=Class.forName("org.embeddedt.embeddium.impl.model.light.LightPipelineProvider");
            embeddium=type.getConstructor(colors,lights).newInstance(null,null);
            embeddiumHeight=type.getDeclaredMethod("fluidHeight",BlockAndTintGetter.class,Fluid.class,BlockPos.class,Direction.class);
            embeddiumHeight.setAccessible(true);
         }catch(ClassNotFoundException absent){
            if(net.neoforged.fml.ModList.get().isLoaded("embeddium"))throw absent;
         }
      }
      var renderer=new net.minecraft.client.renderer.block.LiquidBlockRenderer();
      var heightMethod=renderer.getClass().getDeclaredMethod("getHeight",BlockAndTintGetter.class,Fluid.class,BlockPos.class);
      heightMethod.setAccessible(true);
      for(var p:BlockPos.betweenClosed(ORIGIN.offset(-3,0,-3),ORIGIN.offset(3,2,3))){
         var f=mc.level.getFluidState(p);if(f.isEmpty())continue;
         if(mc.level.getBlockState(p).getFluidState().isEmpty())attachments++;
         var mesh=new Mesh();
         mc.getBlockRenderer().renderLiquid(p,mc.level,mesh,mc.level.getBlockState(p),f);
         float observed=(float)heightMethod.invoke(renderer,mc.level,f.getType(),p);
         float expected=height(mc.level,f.getType(),p);
         if(!Float.isFinite(observed)||!Float.isFinite(expected))throw new IllegalStateException("Nonfinite native height");
         boolean heightEqual=Math.abs(observed-expected)<.00001f;if(!heightEqual)heightMismatch++;
         if(embeddiumHeight!=null){
            for(var direction:Direction.Plane.HORIZONTAL){
               float h=(float)embeddiumHeight.invoke(embeddium,mc.level,f.getType(),p,direction);
               if(!Float.isFinite(h))throw new IllegalStateException("Nonfinite Embeddium height");
               embeddiumProbes++;if(Math.abs(h-expected)>.00001f)embeddiumMismatch++;
            }
         }
         var row=new JsonObject();row.addProperty("pos",p.toShortString());row.addProperty("observed_height",observed);row.addProperty("expected_height",expected);
         row.addProperty("position_height_equal",heightEqual);row.add("actual_vertices",mesh.points);
         boolean top=mesh.points.size()>=4;
         int[][] corners={{0,0},{0,1},{1,1},{1,0}};
         for(int i=0;i<4 && top;i++){var v=mesh.points.get(i).getAsJsonArray();top=v.get(0).getAsFloat()==(p.getX()&15)+corners[i][0] && v.get(2).getAsFloat()==(p.getZ()&15)+corners[i][1];}
         if(top){
            topCount++;boolean equal=true;var expectedTop=new JsonArray();
            for(int i=0;i<4;i++){
               float h=corner(mc.level,f.getType(),p,corners[i][0]==0?-1:1,corners[i][1]==0?-1:1)-.001f+(p.getY()&15);
               expectedTop.add(h);if(Math.abs(mesh.points.get(i).getAsJsonArray().get(1).getAsFloat()-h)>.00001f)equal=false;
            }
            if(!equal)mismatch++;row.addProperty("top_matches_position_heights",equal);row.add("expected_top_y",expectedTop);
         }
         row.addProperty("top_present",top);meshes.add(row);
      }
      var output=directory.resolve("lava-surface-screenshots");Files.createDirectories(output);
      var name=(natural()?"natural":"controlled")+"-"+(decor()?"decor":"vanilla");
      try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve(name+".png"));}
      var row=new JsonObject();row.addProperty("case",name);row.addProperty("server_client_agree",true);row.addProperty("attachment_cells",attachments);
      row.addProperty("native_mesh_mismatches",mismatch);row.addProperty("position_height_mismatches",heightMismatch);row.addProperty("top_faces_checked",topCount);row.addProperty("embeddium_height_probes",embeddiumProbes);row.addProperty("embeddium_height_mismatches",embeddiumMismatch);row.addProperty("screenshot",name+".png");row.add("states",actual);row.add("meshes",meshes);RESULTS.add(row);
      var proof=new JsonObject();proof.addProperty("suite","lava-surface-v1");proof.addProperty("nonce",nonce);proof.addProperty("status",index==3?"passed":"in-progress");proof.add("cases",RESULTS);
      Files.writeString(directory.resolve("client-lava-surface-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");
      index++;stage=0;ticks=0;return index==4;
   }
   private static float height(BlockGetter level,Fluid fluid,BlockPos pos){
      var f=level.getFluidState(pos);
      if(fluid.isSame(f.getType()))return fluid.isSame(level.getFluidState(pos.above()).getType())?1:f.getOwnHeight();
      return level.getBlockState(pos).isSolid()?-1:0;
   }
   private static float corner(BlockGetter level,Fluid fluid,BlockPos pos,int dx,int dz){
      float center=height(level,fluid,pos);if(center>=1)return 1;
      float x=height(level,fluid,pos.offset(dx,0,0)),z=height(level,fluid,pos.offset(0,0,dz));
      if(x>=1||z>=1)return 1;
      float total=0,weight=0;
      float diagonal=x>0||z>0?height(level,fluid,pos.offset(dx,0,dz)):-1;
      if(diagonal>=1)return 1;
      for(float h:new float[]{center,x,z,diagonal}){if(h<0)continue;float w=h>=.8f?10:1;total+=h*w;weight+=w;}
      return total/weight;
   }
   private static JsonArray states(BlockGetter level){
      var rows=new JsonArray();for(var p:BlockPos.betweenClosed(ORIGIN.offset(-3,0,-3),ORIGIN.offset(3,2,3))){
         var state=level.getBlockState(p);var fluid=level.getFluidState(p);var row=new JsonObject();
         row.addProperty("pos",p.toShortString());row.addProperty("block",state.toString());row.addProperty("fluid",fluid.toString());row.addProperty("block_fluid",state.getFluidState().toString());rows.add(row);
      }return rows;
   }
   private static final class Mesh implements VertexConsumer{
      final JsonArray points=new JsonArray();
      public VertexConsumer addVertex(float x,float y,float z){var p=new JsonArray();p.add(x);p.add(y);p.add(z);points.add(p);return this;}
      public VertexConsumer setColor(int r,int g,int b,int a){return this;}
      public VertexConsumer setUv(float u,float v){return this;}
      public VertexConsumer setUv1(int u,int v){return this;}
      public VertexConsumer setUv2(int u,int v){return this;}
      public VertexConsumer setNormal(float x,float y,float z){return this;}
   }
}
