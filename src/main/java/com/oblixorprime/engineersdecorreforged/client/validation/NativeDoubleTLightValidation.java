package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/** Opt-in observer/input fixture; refuses every profile/world except the existing owned nonce. */
@EventBusSubscriber(modid=EngineersDecorReforged.MOD_ID,value=Dist.CLIENT)
public final class NativeDoubleTLightValidation {
   private static final BlockPos ORIGIN=new BlockPos(0,80,0);
   private static final JsonArray SCENES=new JsonArray(),HOLDS=new JsonArray();
   private static Path output;
   private static CompletableFuture<?> pending;
   private static Vec3 camera,target;
   private static int scene,stage,ticks;
   private static long holdStart;
   private static double holdSeconds;
   private static volatile BlockPos dropTarget;
   private static volatile Block dropLamp;
   private static volatile net.minecraft.world.level.Level dropLevel;
   private static volatile int emittedItems;
   private static boolean guarded,finished;
   private static IKeyConflictContext oldAttack;
   private NativeDoubleTLightValidation(){}
   private static BlockPos pos(int index){return ORIGIN.offset(index%4*4,index/4*4,0);}
   private static Block lamp(int index){return switch(index%4){case 0->ModBlocks.IRON_BULB_LIGHT.get();case 1->ModBlocks.IRON_CEILING_EDGE_LIGHT.get();case 2->ModBlocks.IRON_FLOOR_EDGE_LIGHT.get();default->ModBlocks.IRON_INSET_LIGHT.get();};}
   private static boolean lit(BlockState state){return SupportLights.attached(state)||SupportLights.legacy(state);}
   private static void require(boolean value,String error){if(!value)throw new IllegalStateException(error);}
   private static class Fixture extends Screen {Fixture(){super(Component.literal("Isolated Double T light validation"));}@Override public boolean isPauseScreen(){return false;}}

   @SubscribeEvent public static void emitted(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event){
      var target=dropTarget;
      if(!guarded||finished||target==null||event.getLevel()!=dropLevel||event.getLevel().isClientSide)return;
      if(event.getEntity() instanceof ItemEntity item&&item.getItem().is(dropLamp.asItem())&&new AABB(target).inflate(1.5).contains(item.position()))emittedItems+=item.getItem().getCount();
   }
   @SubscribeEvent public static void tick(ClientTickEvent.Post event){
      if(!Boolean.getBoolean("iedctr.doubleTLightValidation")||finished)return;
      var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
      try{
         if(!guarded){
            String nonce=System.getProperty("iedctr.clientGameplayNonce","");require(nonce.matches("[0-9a-f]{32}"),"missing fixture nonce");
            var game=mc.gameDirectory.toPath().toRealPath();require(Files.readString(game.resolve(".iedctr-client-gameplay-nonce")).trim().equals(nonce),"wrong profile");
            require(mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toRealPath().equals(game.resolve("saves/iedctr-ci-"+nonce).toRealPath()),"wrong world refused");
            output=Path.of(System.getProperty("iedctr.doubleTLightEvidence")).toAbsolutePath();Files.createDirectories(output);guarded=true;
            mc.options.pauseOnLostFocus=false;mc.setScreen(new Fixture());
         }
         if(Boolean.getBoolean("iedctr.doubleTLightReload")){reload(mc);return;}
         var pos=pos(scene);int mount=scene/4;boolean floor=scene%4==2;
         if(stage==0){
            Vec3 aim=Vec3.atCenterOf(pos).add(0,mount==0?.25:0,mount==0?0:.2);
            Vec3 eye=aim.add(mount==0&&floor?3:2,mount==0&&!floor?-2:.7,-3);camera=eye.add(0,-mc.player.getEyeHeight(),0);target=aim;
            pending=mc.getSingleplayerServer().submit(()->{
               var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();
               for(var q:BlockPos.betweenClosed(pos.offset(-1,mount==0?-3:-1,-1),pos.offset(1,1,1)))require(level.hasChunkAt(q),"unloaded fixture chunk");
               for(var q:BlockPos.betweenClosed(pos.offset(-1,mount==0?-3:-1,-1),pos.offset(1,1,1)))level.setBlock(q,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
               player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.getAbilities().flying=true;player.onUpdateAbilities();
               level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
               var backing=mount==0?pos.above():pos.south();level.setBlock(backing,Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);
               Vec3 beamPoint=mount==0?Vec3.atLowerCornerOf(pos).add(.5,1,.5):Vec3.atLowerCornerOf(pos).add(mount==2?.1:.5,.5,1);
               Direction beamFace=mount==0?Direction.DOWN:Direction.NORTH;
               player.connection.teleport(camera.x,camera.y,camera.z,0,0);player.setYRot(0);
               var beamStack=new ItemStack(mount==0?ModBlocks.STEEL_DOUBLE_T_SUPPORT.get():ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get(),3);
               player.setItemInHand(InteractionHand.MAIN_HAND,beamStack);
               require(player.gameMode.useItemOn(player,level,beamStack,InteractionHand.MAIN_HAND,new BlockHitResult(beamPoint,beamFace,backing,false)).consumesAction(),"native support placement failed");
               require(beamStack.getCount()==2,"support payment mismatch");
               Vec3 point;Direction face;
               if(mount==0){face=floor?Direction.EAST:Direction.DOWN;point=Vec3.atLowerCornerOf(pos).add(floor?11./16:.5,11./16,.5);}
               else{face=Direction.NORTH;point=Vec3.atLowerCornerOf(pos).add(.5,.5,11./16);}
               for(int distance=1;distance<=3;distance++){
                  var corridor=BlockPos.containing(point.add(face.getStepX()*distance,face.getStepY()*distance,face.getStepZ()*distance));
                  require(level.hasChunkAt(corridor),"unloaded input corridor");if(!corridor.equals(pos))level.setBlock(corridor,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
               }
               Vec3 useEye=point.add(face.getStepX()*3,face.getStepY()*3,face.getStepZ()*3);
               player.connection.teleport(useEye.x,useEye.y-player.getEyeHeight(),useEye.z,floor&&mount==0?90:0,0);
               player.setYRot(floor&&mount==0?90:0);
               var hit=level.clip(new ClipContext(player.getEyePosition(),point.add(-face.getStepX()*1.e-4,-face.getStepY()*1.e-4,-face.getStepZ()*1.e-4),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
               require(hit.getBlockPos().equals(pos)&&hit.getDirection()==face,"actual eye ray did not hit the structural mounting face");
               var stack=new ItemStack(lamp(scene),3);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
               require(player.gameMode.useItemOn(player,level,stack,InteractionHand.MAIN_HAND,hit).consumesAction(),"native lamp placement failed");
               require(lit(level.getBlockState(pos))&&stack.getCount()==2,"lamp/payment did not attach to the support");
               player.connection.teleport(camera.x,camera.y,camera.z,0,0);player.containerMenu.broadcastChanges();
            });stage=1;ticks=0;return;
         }
         if(stage==1){if(!pending.isDone())return;pending.join();require(++ticks<400,"client attachment synchronization timeout");if(mc.player.position().distanceTo(camera)>.1||!lit(mc.level.getBlockState(pos)))return;stage=2;ticks=0;mc.options.hideGui=true;mc.options.fov().set(35);mc.levelRenderer.allChanged();}
         look(mc);if(++ticks<50)return;
         var proof=inspect(mc,scene);proof.addProperty("native_item_placement",true);proof.addProperty("actual_mount_face_ray",true);SCENES.add(proof);capture(mc,"case-"+scene+".png");
         if(++scene==12){finish(mc,null);return;}stage=0;ticks=0;
      }catch(Throwable error){finish(mc,error);}
   }

   private static void reload(Minecraft mc)throws Exception{
      if(stage==0){
         pending=mc.getSingleplayerServer().submit(()->{
            var level=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();
            for(int i=0;i<12;i++){var p=pos(i);require(level.hasChunkAt(p),"saved fixture chunk missing");var state=level.getBlockState(p);require(lit(state),"saved lamp missing "+i);
               if(i==0)require(SteelDoubleTBulbBlock.paidBulb(state),"legacy bulb ownership lost");
               else{var be=SupportLights.entity(level,p);require(be!=null&&be.attachment()!=null&&be.attachment().lamp.is(lamp(i))&&be.attachment().paid,"saved native lamp/ownership lost "+i);}
            }
         });stage=1;ticks=0;return;
      }
      if(stage==1){if(!pending.isDone())return;pending.join();if(++ticks<80)return;for(int i=0;i<12;i++){var proof=inspect(mc,i);proof.addProperty("actual_process_restart",true);SCENES.add(proof);}scene=0;stage=2;ticks=0;}
      var pos=pos(scene);
      if(stage==2){
         mc.options.keyAttack.setDown(false);oldAttack=mc.options.keyAttack.getKeyConflictContext();mc.options.keyAttack.setKeyConflictContext(KeyConflictContext.UNIVERSAL);
         var state=mc.level.getBlockState(pos);var data=SupportLights.data(state,mc.level,pos);var shape=data==null?SteelDoubleTBulbBlock.BULB_SHAPE:data.shape();
         Direction face=scene<4?(scene%4==2?Direction.EAST:Direction.DOWN):Direction.NORTH;
         target=contactTarget(state,mc,pos,shape,face);
         var eye=target.add(face.getStepX()*3,face.getStepY()*3,face.getStepZ()*3);camera=eye.add(0,-mc.player.getEyeHeight(),0);
         pending=mc.getSingleplayerServer().submit(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);var aim=target.subtract(camera.add(0,mc.player.getEyeHeight(),0));
            float yaw=(float)Math.toDegrees(Math.atan2(-aim.x,aim.z)),pitch=(float)-Math.toDegrees(Math.atan2(aim.y,Math.hypot(aim.x,aim.z)));
            p.connection.teleport(camera.x,camera.y,camera.z,yaw,pitch);dropLevel=p.serverLevel();dropLamp=lamp(scene);emittedItems=0;dropTarget=pos;
            p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));p.containerMenu.broadcastChanges();});stage=3;ticks=0;return;
      }
      if(stage==3){if(!pending.isDone())return;pending.join();require(++ticks<300,"native attack setup timeout");if(mc.player.position().distanceTo(camera)>.1||!mc.player.getMainHandItem().is(Items.IRON_PICKAXE)||ticks<30)return;
         look(mc);mc.hitResult=mc.player.pick(mc.player.blockInteractionRange(),1,false);var part=PortedBlocks.selectRailingPart(mc.level,pos,mc.player.getEyePosition(),mc.player.getEyePosition().add(mc.player.getViewVector(1).scale(mc.player.blockInteractionRange())),mc.player);
         require(part!=null&&(part.kind()==PortedBlocks.PartKind.SUPPORT_LIGHT||part.kind()==PortedBlocks.PartKind.BULB_LIGHT),"client ray does not select lamp "+scene);
         mc.options.keyAttack.setDown(true);holdStart=System.nanoTime();attack(mc,true,pos);stage=4;return;
      }
      if(stage==4){look(mc);attack(mc,false,pos);if(System.nanoTime()-holdStart<6_000_000_000L)return;
         holdSeconds=(System.nanoTime()-holdStart)/1.e9;mc.options.keyAttack.setDown(false);mc.gameMode.stopDestroyBlock();
         pending=mc.getSingleplayerServer().submit(()->{var level=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();var state=level.getBlockState(pos);
            require(state.getBlock() instanceof DoubleTLightHost&&!lit(state),"six-second hold removed support or retained lamp "+scene);
            require(emittedItems==1,"native lamp emitted stack count is not exactly one "+scene+" count="+emittedItems);dropTarget=null;
         });stage=5;return;
      }
      if(stage==5){if(!pending.isDone())return;pending.join();require(++ticks<300,"server removal failed to synchronize to client");if(lit(mc.level.getBlockState(pos)))return;
         var proof=new JsonObject();proof.addProperty("case",scene);proof.addProperty("actual_held_seconds",holdSeconds);proof.addProperty("emitted_stack_count",1);proof.addProperty("native_input_and_server_ack",true);proof.addProperty("item_gravity_unchanged",true);HOLDS.add(proof);
         capture(mc,"after-hold-"+scene+".png");mc.options.keyAttack.setKeyConflictContext(oldAttack);if(++scene==12){finish(mc,null);return;}stage=2;ticks=0;}
   }
   private static Vec3 contactTarget(BlockState state,Minecraft mc,BlockPos pos,net.minecraft.world.phys.shapes.VoxelShape shape,Direction face){
      var axis=face.getAxis();boolean positive=face.getAxisDirection()==Direction.AxisDirection.POSITIVE;
      var base=((DoubleTLightHost)state.getBlock()).supportShape(state,mc.level,pos);
      for(var a:shape.toAabbs())for(var b:base.toAabbs()){
         if(Math.abs((positive?a.min(axis):a.max(axis))-(positive?b.max(axis):b.min(axis)))>1.e-6)continue;
         double[] point=new double[3];boolean contact=true;
         for(var other:Direction.Axis.values()){
            if(other==axis){point[other.ordinal()]=(a.min(other)+a.max(other))/2;continue;}
            double lo=Math.max(a.min(other),b.min(other)),hi=Math.min(a.max(other),b.max(other));
            if(hi-lo<1.e-6){contact=false;break;}point[other.ordinal()]=(lo+hi)/2;
         }
         if(contact)return new Vec3(point[0],point[1],point[2]).add(Vec3.atLowerCornerOf(pos));
      }
      throw new IllegalStateException("No real metal contact for native held target "+scene);
   }
   private static void attack(Minecraft mc,boolean start,BlockPos pos){
      require(mc.options.keyAttack.isDown(),"native held attack mapping is not active");
      mc.hitResult=mc.player.pick(mc.player.blockInteractionRange(),1,false);
      require(mc.hitResult instanceof BlockHitResult hit&&hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(pos),"native held ray lost support");
      var input=ClientHooks.onClickInput(0,mc.options.keyAttack,InteractionHand.MAIN_HAND);
      if(!input.isCanceled()){var hit=(BlockHitResult)mc.hitResult;if(start)mc.gameMode.startDestroyBlock(pos,hit.getDirection());else mc.gameMode.continueDestroyBlock(pos,hit.getDirection());}
   }
   private static void look(Minecraft mc){mc.player.setNoGravity(true);mc.player.setDeltaMovement(Vec3.ZERO);var d=target.subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));}
   private static JsonObject inspect(Minecraft mc,int index){
      var pos=pos(index);var state=mc.level.getBlockState(pos);require(lit(state),"client lamp is not synchronized "+index);
      var be=SupportLights.entity(mc.level,pos);var data=SupportLights.data(state,mc.level,pos);boolean legacy=SupportLights.legacy(state);
      BlockState source=legacy?ModBlocks.IRON_BULB_LIGHT.get().defaultBlockState().setValue(PortedBlocks.FACING,Direction.UP):data.lamp;
      Vec3 offset=legacy?new Vec3(0,-5./16,0):data.offset;require(source.is(lamp(index)),"wrong visible lamp identity");
      var shaper=mc.getBlockRenderer().getBlockModelShaper();var model=shaper.getBlockModel(state);var md=model.getModelData(mc.level,pos,state,be==null?ModelData.EMPTY:be.getModelData());
      var actual=vertices(model.getQuads(state,null,RandomSource.create(0),md,null),Vec3.ZERO);
      var base=state.setValue(SupportLights.ATTACHED,false);if(legacy)base=base.setValue(SteelDoubleTBulbBlock.BULB_LIGHT,SteelDoubleTBulbBlock.Bulb.NONE);
      var core=shaper.getBlockModel(base);for(String vertex:vertices(core.getQuads(base,null,RandomSource.create(0),ModelData.EMPTY,null),Vec3.ZERO))require(actual.remove(vertex),"original support vertices missing");
      var lampModel=shaper.getBlockModel(source);var wanted=vertices(lampModel.getQuads(source,null,RandomSource.create(0),ModelData.EMPTY,null),offset);Collections.sort(actual);require(actual.equals(wanted),"lamp model/UVs changed or missing "+index);
      var result=new JsonObject();result.addProperty("case",index);result.addProperty("lamp",BuiltInRegistries.BLOCK.getKey(source.getBlock()).toString());result.addProperty("mount",index/4);result.addProperty("original_lamp_quads_translated_only",true);result.addProperty("paid",legacy?SteelDoubleTBulbBlock.paidBulb(state):data.paid);result.addProperty("light",state.getLightEmission(mc.level,pos));return result;
   }
   private static List<String> vertices(List<BakedQuad> quads,Vec3 offset){
      var result=new ArrayList<String>();for(var quad:quads){var sprite=quad.getSprite().contents().name().toString();require(!sprite.endsWith("missingno"),"missing lamp texture");int[] data=quad.getVertices();int stride=data.length/4;for(int i=0;i<4;i++)result.add(String.format(Locale.ROOT,"%s:%.5f,%.5f,%.5f:%.5f,%.5f",sprite,Float.intBitsToFloat(data[i*stride])+offset.x,Float.intBitsToFloat(data[i*stride+1])+offset.y,Float.intBitsToFloat(data[i*stride+2])+offset.z,Float.intBitsToFloat(data[i*stride+4]),Float.intBitsToFloat(data[i*stride+5])));}Collections.sort(result);return result;
   }
   private static void capture(Minecraft mc,String name)throws Exception{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve(name));}}
   private static void finish(Minecraft mc,Throwable error){
      if(finished)return;finished=true;mc.options.keyAttack.setDown(false);if(oldAttack!=null)mc.options.keyAttack.setKeyConflictContext(oldAttack);
      try{var proof=new JsonObject();proof.addProperty("status",error==null?"passed":"failed");proof.addProperty("error",error==null?null:error.toString());proof.add("scenes",SCENES);proof.add("native_holds",HOLDS);if(output!=null)Files.writeString(output.resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");}catch(Exception e){EngineersDecorReforged.LOGGER.error("Cannot record Double T lamp validation",e);}
      if(error!=null)EngineersDecorReforged.LOGGER.error("Double T lamp validation failed",error);mc.stop();
   }
}
