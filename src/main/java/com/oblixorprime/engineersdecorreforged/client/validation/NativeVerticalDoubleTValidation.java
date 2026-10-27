package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.VerticalDoubleTSupportBlock;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Disabled by default; uses only the already-owned nonce-guarded saved test world. */
@EventBusSubscriber(modid=EngineersDecorReforged.MOD_ID,value=Dist.CLIENT)
public final class NativeVerticalDoubleTValidation {
   private static final BlockPos POS=new BlockPos(0,80,0);
   private static final Direction[] FACINGS={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
   private static final JsonArray SCENES=new JsonArray();
   private static CompletableFuture<?> pending;
   private static Vec3 camera,target;
   private static Path output;
   private static int scene,stage,ticks;
   private static boolean guarded,finished;
   private NativeVerticalDoubleTValidation(){}
   private static boolean wallMode(){return Boolean.getBoolean("iedctr.wallDoubleTValidation");}

   @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
      if(!(Boolean.getBoolean("iedctr.verticalDoubleTValidation")||wallMode())||finished)return;
      var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
      try {
         if(!guarded) {
            String nonce=System.getProperty("iedctr.clientGameplayNonce","");require(nonce.matches("[0-9a-f]{32}"),"missing fixture nonce");
            Path game=mc.gameDirectory.toPath().toRealPath();require(Files.readString(game.resolve(".iedctr-client-gameplay-nonce")).trim().equals(nonce),"wrong profile marker");
            require(mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toRealPath().equals(game.resolve("saves/iedctr-ci-"+nonce).toRealPath()),"different world refused");
            output=Path.of(System.getProperty("iedctr.verticalDoubleTEvidence")).toAbsolutePath();Files.createDirectories(output);guarded=true;
         }
         if(Boolean.getBoolean("iedctr.verticalDoubleTReload")) {
            if(stage==0) {
               pending=mc.getSingleplayerServer().submit(()->{
                  var level=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();
                  require(level.hasChunkAt(POS),"saved chunk unavailable");var state=level.getBlockState(POS);
                  require(state.is(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get())&&state.getValue(VerticalDoubleTSupportBlock.FACING)==Direction.WEST,"saved beam/facing missing on server");
                  if(wallMode())require(state.getValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL)&&state.getValue(VerticalDoubleTSupportBlock.JOIN_NEGATIVE),"saved horizontal mount/join missing on server");
               });stage=1;ticks=0;return;
            }
            if(!pending.isDone())return;pending.join();require(++ticks<300,"saved state failed to synchronize");
            if(!mc.level.hasChunkAt(POS)||!mc.level.getBlockState(POS).is(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get())||ticks<80)return;
            var proof=inspect(mc,Direction.WEST);proof.addProperty("actual_process_restart",true);SCENES.add(proof);capture(mc,"reload.png");finish(mc,null);return;
         }
         Direction face=FACINGS[scene%4],side=face.getClockWise();boolean across=wallMode()&&scene>=4;
         if(stage==0) {
            target=Vec3.atCenterOf(POS).add(-face.getStepX()*.34375,0,-face.getStepZ()*.34375);
            Vec3 eye=target.add(face.getStepX()*3+side.getStepX()*1.8,.55,face.getStepZ()*3+side.getStepZ()*1.8);
            camera=eye.add(0,-mc.player.getEyeHeight(),0);
            pending=mc.getSingleplayerServer().submit(()->{
               var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();
               for(var pos:BlockPos.betweenClosed(POS.offset(-3,-2,-3),POS.offset(3,2,3)))require(level.hasChunkAt(pos),"unavailable fixture chunk");
               for(var pos:BlockPos.betweenClosed(POS.offset(-3,-2,-3),POS.offset(3,2,3)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
               BlockPos wall=POS.relative(face.getOpposite());
               for(int dy=-1;dy<=1;dy++)for(int along=-1;along<=1;along++)level.setBlock(wall.relative(side,along).offset(0,dy,0),Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);
               player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.onUpdateAbilities();
               level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);player.connection.teleport(camera.x,camera.y,camera.z,0,0);
               Vec3 wallPoint=Vec3.atCenterOf(wall).add(face.getStepX()*.49,0,face.getStepZ()*.49);
               if(across)wallPoint=wallPoint.add(side.getStepX()*.4,0,side.getStepZ()*.4);
               var hit=level.clip(new ClipContext(player.getEyePosition(),wallPoint,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
               require(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(wall)&&hit.getDirection()==face,"real eye ray did not select intended wall face");
               var stack=new ItemStack(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
               require(player.gameMode.useItemOn(player,level,stack,InteractionHand.MAIN_HAND,hit).consumesAction(),"native placement failed");
               var placed=level.getBlockState(POS);require(placed.is(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get())&&placed.getValue(VerticalDoubleTSupportBlock.FACING)==face,"wrong adjacent cell or facing");
               require(level.getBlockState(wall).is(Blocks.BRICKS)&&stack.getCount()==2,"creative placement altered wall/stack");
               require(placed.getValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL)==across,"native wall click chose wrong mount");
               if(wallMode()){
                  var other=POS.relative(across?Direction.UP:side);
                  level.setBlock(other,ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(VerticalDoubleTSupportBlock.FACING,face).setValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL,!across),Block.UPDATE_ALL);
               }
            });stage=1;ticks=0;return;
         }
         if(stage==1) {
            if(!pending.isDone())return;pending.join();require(++ticks<300,"client placement/camera synchronization timeout");
            if(mc.player.position().distanceTo(camera)>.1||!mc.level.getBlockState(POS).is(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get())
                  ||mc.level.getBlockState(POS).getValue(VerticalDoubleTSupportBlock.FACING)!=face)return;
            mc.options.hideGui=true;mc.options.fov().set(35);mc.levelRenderer.allChanged();stage=2;ticks=0;
         }
         mc.player.setNoGravity(true);mc.player.setDeltaMovement(Vec3.ZERO);Vec3 delta=target.subtract(mc.player.getEyePosition());
         mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));
         if(++ticks<60)return;
         var proof=inspect(mc,face);proof.addProperty("native_game_mode_placement",true);proof.addProperty("actual_wall_face_ray",true);SCENES.add(proof);capture(mc,(across?"horizontal-":"vertical-")+face.getName()+".png");
         if(++scene==(wallMode()?8:4)){finish(mc,null);return;}stage=0;ticks=0;
      }catch(Throwable error){finish(mc,error);}
   }

   private static JsonObject inspect(Minecraft mc,Direction face) {
      var state=mc.level.getBlockState(POS);require(state.getValue(VerticalDoubleTSupportBlock.FACING)==face,"saved/client direction mismatch");
      var shaper=mc.getBlockRenderer().getBlockModelShaper();var actual=shaper.getBlockModel(state).getQuads(state,null,RandomSource.create(0),ModelData.EMPTY,null);
      boolean across=state.getValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL);
      var old=ModBlocks.STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState();
      if(wallMode())old=old.setValue(PortedBlocks.LEFTBEAM,across).setValue(PortedBlocks.RIGHTBEAM,!across);var original=shaper.getBlockModel(old).getQuads(old,null,RandomSource.create(0),ModelData.EMPTY,null);
      require(actual.size()==(wallMode()?36:20)&&original.size()==(wallMode()?36:20),"unexpected source/model face count");
      require(vertices(original,face,true,across).equals(vertices(actual,face,false,across)),"native baked UV/texture/geometry is not the rigid source rotation");
      var box=state.getShape(mc.level,POS).bounds();double contact=switch(face){case NORTH->box.maxZ;case EAST->box.minX;case SOUTH->box.minZ;default->box.maxX;};
      require(contact==(face==Direction.NORTH||face==Direction.WEST?1.:0.),"back flange gap at wall");
      var result=new JsonObject();result.addProperty("wall_horizontal",across);result.addProperty("facing",face.getName());result.addProperty("baked_quads",actual.size());result.addProperty("source_uv_and_geometry_rigid_rotation",true);result.addProperty("back_flange_wall_gap",0);return result;
   }
   private static List<String> vertices(List<BakedQuad> quads,Direction facing,boolean rotate,boolean across) {
      List<String> result=new ArrayList<>();
      for(var quad:quads) {
         String texture=quad.getSprite().contents().name().toString();require(!texture.endsWith("missingno"),"missing source texture");int[] data=quad.getVertices();
         for(int i=0;i<4;i++) {
            double x=Float.intBitsToFloat(data[i*8]),y=Float.intBitsToFloat(data[i*8+1]),z=Float.intBitsToFloat(data[i*8+2]);
            if(rotate){double a=across?1-z:x,b=across?1-x:1-z,c=y;switch(facing){case EAST:x=1-c;y=b;z=a;break;case SOUTH:x=1-a;y=b;z=1-c;break;case WEST:x=c;y=b;z=1-a;break;default:x=a;y=b;z=c;}}
            result.add(String.format(Locale.ROOT,"%s:%.6f,%.6f,%.6f:%.6f,%.6f",texture,x,y,z,Float.intBitsToFloat(data[i*8+4]),Float.intBitsToFloat(data[i*8+5])));
         }
      }
      Collections.sort(result);return result;
   }
   private static void capture(Minecraft mc,String name)throws Exception{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve(name));}}
   private static void finish(Minecraft mc,Throwable error) {
      if(finished)return;finished=true;
      try{var proof=new JsonObject();proof.addProperty("status",error==null?"captured":"failed");proof.addProperty("error",error==null?null:error.toString());proof.add("scenes",SCENES);if(output!=null)Files.writeString(output.resolve(Boolean.getBoolean("iedctr.verticalDoubleTReload")?"reload-result.json":"native-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");}catch(Exception e){EngineersDecorReforged.LOGGER.error("Cannot record vertical beam diagnostic",e);}
      if(error!=null)EngineersDecorReforged.LOGGER.error("Vertical Double T native diagnostic failed",error);mc.stop();
   }
   private static void require(boolean test,String message){if(!test)throw new IllegalStateException(message);}
}
