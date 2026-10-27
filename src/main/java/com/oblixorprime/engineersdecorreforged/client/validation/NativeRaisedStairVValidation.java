package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.client.RaisedStairPoleModels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Explicit opt-in only, guarded reuse of the already-owned disposable world. */
@EventBusSubscriber(modid=EngineersDecorReforged.MOD_ID,value=Dist.CLIENT)
public final class NativeRaisedStairVValidation {
   private static final BlockPos POS=new BlockPos(0,80,0);
   private static final Direction[] FACINGS={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
   private static final JsonArray SCENES=new JsonArray();
   private static CompletableFuture<?> pending;
   private static int scene,stage,ticks;
   private static boolean guarded,finished;
   private static Path evidence;
   private static Vec3 camera,target;
   private static ModelData previousPoleData;
   private NativeRaisedStairVValidation() {}

   @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
      if(!Boolean.getBoolean("iedctr.raisedStairVValidation")||finished)return;
      Minecraft mc=Minecraft.getInstance();
      if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
      try {
         if(!guarded) {
            String nonce=System.getProperty("iedctr.clientGameplayNonce","");
            require(nonce.matches("[0-9a-f]{32}"),"missing isolated-world nonce");
            Path game=mc.gameDirectory.toPath().toRealPath();
            require(Files.readString(game.resolve(".iedctr-client-gameplay-nonce")).trim().equals(nonce),"wrong owned profile marker");
            require(mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toRealPath().equals(game.resolve("saves/iedctr-ci-"+nonce).toRealPath()),"refusing a different world");
            evidence=Path.of(System.getProperty("iedctr.raisedStairVEvidence")).toAbsolutePath();Files.createDirectories(evidence);
            guarded=true;
         }
         boolean reload=Boolean.getBoolean("iedctr.raisedStairVReload");
         if(reload) {
            if(stage==0) {
               pending=mc.getSingleplayerServer().submit(()->{
                  var level=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();
                  require(level.hasChunkAt(POS),"saved fixture chunk is unavailable");
                  require(level.getBlockState(POS).is(ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get()),"saved new stair missing on server after restart");
                  require(level.getBlockState(POS.below()).is(ModBlocks.THICK_STEEL_POLE.get()),"saved independent pole missing on server after restart");
               });stage=10;ticks=0;return;
            }
            if(stage==10) {
               if(!pending.isDone())return;pending.join();
               require(++ticks<300,"saved fixture did not synchronize to the client");
               if(!mc.level.hasChunkAt(POS)||!mc.level.getBlockState(POS).is(ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get())
                     ||!mc.level.getBlockState(POS.below()).is(ModBlocks.THICK_STEEL_POLE.get())||ticks<80)return;
               var proof=inspect(mc,true);proof.addProperty("real_process_reload",true);proof.addProperty("server_saved_state_verified_before_client_wait",true);SCENES.add(proof);
               capture(mc,"reload-initial.png");stage=20;scene=0;return;
            }
            if(stage==20) {
               pending=mc.getSingleplayerServer().submit(()->{
                  var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();
                  var pole=(scene==0?ModBlocks.TREATED_WOOD_POLE:scene==1?ModBlocks.THIN_STEEL_POLE:ModBlocks.THICK_STEEL_POLE).get().defaultBlockState().setValue(DirectionalBlock.FACING,Direction.UP);
                  level.setBlock(POS.below(),pole,Block.UPDATE_ALL);level.setBlock(POS.below(2),pole,Block.UPDATE_ALL);
                  player.connection.teleport(.5,79.4-player.getEyeHeight(),4.2,180,4);
               });stage=21;ticks=0;return;
            }
            if(!pending.isDone())return;pending.join();
            require(++ticks<300,"profile view did not synchronize");
            var expected=(scene==0?ModBlocks.TREATED_WOOD_POLE:scene==1?ModBlocks.THIN_STEEL_POLE:ModBlocks.THICK_STEEL_POLE).get();
            if(!mc.level.getBlockState(POS.below()).is(expected)||ticks<40)return;
            mc.player.setYRot(180);mc.player.setXRot(4);mc.options.hideGui=true;mc.options.fov().set(35);
            var proof=inspect(mc,true);proof.addProperty("profile_material",scene==0?"wood":scene==1?"thin-steel":"thick-steel");SCENES.add(proof);
            capture(mc,"profile-"+scene+".png");
            if(++scene==3)finish(mc,null);else stage=20;
            return;
         }
         if(stage==3) {
            var pole=mc.level.getBlockState(POS.below());var model=mc.getBlockRenderer().getBlockModelShaper().getBlockModel(pole);
            previousPoleData=model.getModelData(mc.level,POS.below(),pole,ModelData.EMPTY);
            pending=mc.getSingleplayerServer().submit(()->mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel()
               .setBlock(POS,ModBlocks.STEEL_CATWALK_STAIRS.get().defaultBlockState(),Block.UPDATE_ALL));
            stage=4;ticks=0;return;
         }
         if(stage==4) {
            if(!pending.isDone())return;pending.join();
            require(++ticks<300,"ordinary neighbor restoration timeout");
            if(!mc.level.getBlockState(POS).is(ModBlocks.STEEL_CATWALK_STAIRS.get())||ticks<40)return;
            var pole=mc.level.getBlockState(POS.below());var model=mc.getBlockRenderer().getBlockModelShaper().getBlockModel(pole);
            var data=model.getModelData(mc.level,POS.below(),pole,previousPoleData);
            require(model.getQuads(pole,null,RandomSource.create(0),data,null).equals(model.getQuads(pole,null,RandomSource.create(0))),"pole retains stale connection after neighbor replacement");
            capture(mc,"restored-ordinary-neighbor.png");
            var record=new JsonObject();record.addProperty("ordinary_neighbor_restores_original_pole",true);record.addProperty("stale_model_data_cleared",true);SCENES.add(record);
            pending=mc.getSingleplayerServer().submit(()->mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel()
               .setBlock(POS,ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get().defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING,Direction.WEST),Block.UPDATE_ALL));
            stage=5;ticks=0;return;
         }
         if(stage==5) {
            if(!pending.isDone())return;pending.join();
            if(++ticks<40||!mc.level.getBlockState(POS).is(ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get()))return;
            finish(mc,null);return;
         }
         int material=scene/4;Direction facing=FACINGS[scene%4];
         if(stage==0) {
            target=Vec3.atBottomCenterOf(POS).add(0,-.15,0);
            Direction side=facing.getCounterClockWise(),back=facing.getOpposite();
            Vec3 eye=target.add(side.getStepX()*3.3+back.getStepX()*2,.6,side.getStepZ()*3.3+back.getStepZ()*2);
            camera=eye.add(0,-mc.player.getEyeHeight(),0);
            pending=mc.getSingleplayerServer().submit(()->{
               var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();
               player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.onUpdateAbilities();
               level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
               for(var p:BlockPos.betweenClosed(POS.offset(-3,-4,-3),POS.offset(3,3,3))) {
                  require(level.hasChunkAt(p),"fixture would access an unavailable chunk");level.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
               }
               var pole=(material==0?ModBlocks.TREATED_WOOD_POLE:material==1?ModBlocks.THIN_STEEL_POLE:ModBlocks.THICK_STEEL_POLE).get().defaultBlockState().setValue(DirectionalBlock.FACING,Direction.UP);
               level.setBlock(POS.below(2),pole,Block.UPDATE_ALL);
               level.setBlock(POS.below(),pole,Block.UPDATE_ALL);
               level.setBlock(POS,ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get().defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING,facing),Block.UPDATE_ALL);
               player.connection.teleport(camera.x,camera.y,camera.z,0,0);
            });stage=1;ticks=0;return;
         }
         if(stage==1) {
            if(!pending.isDone())return;pending.join();
            require(++ticks<300,"client snapshot/camera timeout");
            if(mc.player.position().distanceTo(camera)>.1||!mc.level.getBlockState(POS).is(ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get())
                  ||mc.level.getBlockState(POS).getValue(PortedBlocks.HORIZONTAL_FACING)!=facing)return;
            mc.options.hideGui=true;mc.options.fov().set(35);mc.levelRenderer.allChanged();stage=2;ticks=0;
         }
         mc.player.setNoGravity(true);mc.player.setDeltaMovement(Vec3.ZERO);
         Vec3 delta=target.subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
         mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));
         if(++ticks<60)return;
         var proof=inspect(mc,true);proof.addProperty("material",material==0?"wood":material==1?"thin-steel":"thick-steel");proof.addProperty("facing",facing.getName());
         proof.addProperty("setup","explicit native server block states, not item placement proof");
         capture(mc,"scene-"+scene+".png");SCENES.add(proof);scene++;
         if(scene==12) { stage=3;ticks=0;return; }
         stage=0;ticks=0;
      } catch(Throwable error) { finish(mc,error); }
   }

   private static JsonObject inspect(Minecraft mc,boolean connected) {
      var state=mc.level.getBlockState(POS);var pole=mc.level.getBlockState(POS.below());
      var shaper=mc.getBlockRenderer().getBlockModelShaper();var stairs=shaper.getBlockModel(state);var model=shaper.getBlockModel(pole);
      require(stairs instanceof RaisedStairPoleModels.PoleModel,"new stair model wrapper missing");require(model instanceof RaisedStairPoleModels.PoleModel,"pole model wrapper missing");
      var data=model.getModelData(mc.level,POS.below(),pole,ModelData.EMPTY);
      var quads=model.getQuads(pole,null,RandomSource.create(0),data,null);
      var original=model.getQuads(pole,null,RandomSource.create(0));
      require(!quads.isEmpty()&&!Arrays.equals(quads.get(0).getVertices(),original.get(0).getVertices()),"contextual lower mesh not selected");
      var section=com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkStairsBlock.section(pole);
      double lo=section==com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkStairsBlock.Section.WOOD?5.5:
         section==com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkStairsBlock.Section.THIN_STEEL?6:5;
      double hi=16-lo;
      double minFoot=16,maxFoot=0;int footVertices=0;
      for(var quad:quads) {
         require(!quad.getSprite().contents().name().getPath().equals("missingno"),"missing connection texture");
         int[] vertices=quad.getVertices();
         boolean flatBottom=true;
         for(int i=0;i<4;i++) {
            double x=Float.intBitsToFloat(vertices[i*8])*16,y=Float.intBitsToFloat(vertices[i*8+1])*16,z=Float.intBitsToFloat(vertices[i*8+2])*16;
            flatBottom&=Math.abs(y)<1e-6;
            if(Math.abs(y)<1e-6) {
               footVertices++;minFoot=Math.min(minFoot,Math.min(x,z));maxFoot=Math.max(maxFoot,Math.max(x,z));
               require(onFoot(section,x,z),"native foot vertex does not lie on the real pole contour");
            }
         }
         require(!flatBottom,"matching ordinary lower pole has a duplicate join cap");
         for(int i=0;i<4;i++)for(int axis=0;axis<3;axis++) {
            float value=Float.intBitsToFloat(vertices[i*8+axis]);require(value>=-1e-5&&value<=1.00001,"pole render escapes its own cell");
         }
      }
      require(footVertices>0&&Math.abs(minFoot-lo)<1e-6&&Math.abs(maxFoot-hi)<1e-6,"native foot contour has wrong dimensions/centre");
      var upperData=stairs.getModelData(mc.level,POS,state,ModelData.EMPTY);
      require(stairs.getQuads(state,null,RandomSource.create(0),upperData,null).size()>stairs.getQuads(state,null,RandomSource.create(0)).size(),"upper V mesh missing");
      var out=new JsonObject();out.addProperty("lower_quads",quads.size());out.addProperty("section",section.getSerializedName());out.addProperty("foot_min",minFoot);out.addProperty("foot_max",maxFoot);out.addProperty("foot_contour_checked",true);out.addProperty("join_cap_absent",true);out.addProperty("upper_v_present",true);out.addProperty("textures_resolved",true);out.addProperty("separate_pole_registry",pole.getBlock().toString());return out;
   }

   private static boolean onFoot(com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkStairsBlock.Section section,double x,double z) {
      double epsilon=2e-6;
      if(section==com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkStairsBlock.Section.WOOD) {
         double[][] p={{5.5,5.75},{5.75,5.75},{5.75,5.5},{10.25,5.5},{10.25,5.75},{10.5,5.75},
            {10.5,10.25},{10.25,10.25},{10.25,10.5},{5.75,10.5},{5.75,10.25},{5.5,10.25}};
         for(int i=0;i<p.length;i++) {var a=p[i];var b=p[(i+1)%p.length];double dx=b[0]-a[0],dz=b[1]-a[1],length=Math.hypot(dx,dz);
            double t=((x-a[0])*dx+(z-a[1])*dz)/(length*length);
            if(t>=-epsilon&&t<=1+epsilon&&Math.abs((x-a[0])*dz-(z-a[1])*dx)/length<epsilon)return true;
         }return false;
      }
      double outer=section==com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkStairsBlock.Section.THIN_STEEL?2:3;
      for(double radius:new double[]{outer,outer-1}) {
         double a=Math.abs(x-8),b=Math.abs(z-8);
         if(a<=radius+epsilon&&b<=radius+epsilon&&(Math.abs(a-radius)<epsilon||Math.abs(b-radius)<epsilon))return true;
      }return false;
   }

   private static void capture(Minecraft mc,String name)throws Exception {
      try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(evidence.resolve(name));}
   }
   private static void finish(Minecraft mc,Throwable error) {
      if(finished)return;finished=true;
      try {
         var result=new JsonObject();result.addProperty("status",error==null?"captured":"failed");result.addProperty("error",error==null?null:error.toString());result.add("scenes",SCENES);
         if(evidence!=null)Files.writeString(evidence.resolve(Boolean.getBoolean("iedctr.raisedStairVReload")?"reload-result.json":"native-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n");
      }catch(Exception e){EngineersDecorReforged.LOGGER.error("Cannot record V stair diagnostic",e);}
      if(error!=null)EngineersDecorReforged.LOGGER.error("Raised stair V diagnostic failed",error);
      mc.stop();
   }
   private static void require(boolean test,String message){if(!test)throw new IllegalStateException(message);}
}
