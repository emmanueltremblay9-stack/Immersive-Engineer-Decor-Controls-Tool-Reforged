package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in observer of the ordinary ticker in an existing owned cloud world. No manual machine ticks. */
@EventBusSubscriber(modid=EngineersDecorReforged.MOD_ID,value=Dist.CLIENT)
public final class NativeFunnelRuntimeValidation {
   private static final BlockPos NEAR=new BlockPos(0,90,0),FAR=NEAR.east(5);
   private static final JsonArray SAMPLES=new JsonArray();
   private static final JsonObject RESULT=new JsonObject();
   private static volatile boolean monitoring,ready,finished;
   private static volatile Throwable failure;
   private static CompletableFuture<?> pending;
   private static Path output;
   private static int stage,frames,lastProgress=-1,reloadCounter,paidCounter=-1;
   private static long previousWorkTime=-1;

   private static boolean enabled(){return Boolean.getBoolean("iedctr.funnelRuntimeValidation");}
   private static boolean reload(){return Boolean.getBoolean("iedctr.funnelRuntimeReload");}
   private static void guard(MinecraftServer server)throws Exception {
      var mc=Minecraft.getInstance();String nonce=System.getProperty("iedctr.clientGameplayNonce","");
      require(nonce.matches("[0-9a-f]{32}"),"missing owned-world nonce");
      Path game=mc.gameDirectory.toPath().toRealPath();
      require(Files.readString(game.resolve(".iedctr-client-gameplay-nonce")).trim().equals(nonce),"different profile marker");
      require(server.getWorldPath(LevelResource.ROOT).toRealPath().equals(game.resolve("saves/iedctr-ci-"+nonce).toRealPath()),"refusing another world");
   }

   @SubscribeEvent public static void started(ServerStartedEvent event) {
      if(!enabled()||!reload())return;
      try{guard(event.getServer());event.getServer().tickRateManager().setFrozen(true);}
      catch(Throwable error){failure=error;}
   }

   @SubscribeEvent public static void client(ClientTickEvent.Post event) {
      if(!enabled()||finished)return;
      var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
      try {
         if(failure!=null)throw new IllegalStateException("server validation failed",failure);
         if(stage==0) {
            guard(mc.getSingleplayerServer());
            output=Path.of(System.getProperty("iedctr.funnelRuntimeEvidence")).toAbsolutePath();Files.createDirectories(output);
            pending=mc.getSingleplayerServer().submit(()->{try{setup(mc);}catch(Throwable error){failure=error;}});
            stage=1;return;
         }
         if(!pending.isDone())return;pending.join();
         var target=Vec3.atCenterOf(NEAR).add(2.5,7,0);var delta=target.subtract(mc.player.getEyePosition());
         mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
         mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));
         mc.options.hideGui=true;mc.options.fov().set(55);
         require(++frames<2400,"ordinary ticker observation timed out");
         if(!ready)return;
         if(stage==1){mc.levelRenderer.allChanged();stage=2;frames=0;return;}
         if(frames<30)return;
         try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve(reload()?"paid-after-restart.png":"pending-before-stop.png"));}
         finish(mc,null);
      }catch(Throwable error){finish(mc,error);}
   }

   private static void setup(Minecraft mc)throws Exception {
      var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());
      require(player!=null,"no owned-world server player");var level=player.serverLevel();
      for(var p:BlockPos.betweenClosed(NEAR.offset(-2,-3,-2),FAR.offset(2,21,2)))require(level.hasChunkAt(p),"fixture chunk unavailable");
      player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.onUpdateAbilities();
      player.connection.teleport(NEAR.getX()+10,NEAR.getY()+9,NEAR.getZ()-15,0,0);
      if(reload()) {
         var machine=machine(level,NEAR);var data=machine.saveWithFullMetadata(level.registryAccess());
         reloadCounter=data.getInt("TickCounter");
         require(data.getCompound("FunnelFluid").getCompound("Pending").getInt("Progress")==750,"saved pending work missing after process restart");
         require(total(level,NEAR)==0&&total(level,FAR)==0,"unpaid work became fluid during restart");
         require(level.getFluidState(NEAR.above(17)).isSource()&&level.getFluidState(FAR.above(18)).isSource(),"saved sources changed before resumed work");
         RESULT.addProperty("restart_loaded_progress",750);RESULT.addProperty("restart_loaded_tick",reloadCounter);
      } else {
         for(var p:BlockPos.betweenClosed(NEAR.offset(-2,-3,-2),FAR.offset(2,21,2)))level.setBlock(p,Blocks.AIR.defaultBlockState(),18);
         var pipe=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering","fluid_pipe"));
         var barrel=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering","metal_barrel"));
         require(pipe!=Blocks.AIR&&barrel!=Blocks.AIR,"pinned IE blocks missing");
         for(var pos:new BlockPos[]{NEAR,FAR}) {
            for(int y=1;y<=20;y++)for(Direction face:Direction.Plane.HORIZONTAL)
               level.setBlock(pos.above(y).relative(face),face==Direction.NORTH?Blocks.GLASS.defaultBlockState():Blocks.BRICKS.defaultBlockState(),18);
            level.setBlock(pos.below(2),barrel.defaultBlockState(),3);level.setBlock(pos.below(),pipe.defaultBlockState(),3);
            level.setBlock(pos,ModBlocks.SMALL_FLUID_FUNNEL.get().defaultBlockState(),3);
         }
         level.setWeatherParameters(100000,0,false,false);level.setDayTime(6000);
         level.setBlock(NEAR.above(17),Blocks.WATER.defaultBlockState(),3);
         level.setBlock(FAR.above(18),Blocks.WATER.defaultBlockState(),3);
      }
      monitoring=true;server.tickRateManager().setFrozen(false);
   }

   @SubscribeEvent public static void observe(ServerTickEvent.Post event) {
      if(!enabled()||!monitoring||ready||failure!=null)return;
      try {
         var server=event.getServer();var level=server.overworld();var machine=machine(level,NEAR);
         var data=machine.saveWithFullMetadata(level.registryAccess());int counter=data.getInt("TickCounter");
         int work=data.getCompound("FunnelFluid").getCompound("Pending").getInt("Progress");
         int total=total(level,NEAR),farTotal=total(level,FAR);
         require(farTotal==0,"source beyond16edges produced fluid");
         require(level.getFluidState(FAR.above(18)).isSource(),"out-of-range source was consumed");
         if(!reload()&&work!=lastProgress) {
            if(work>0) {
               if(previousWorkTime>=0)require(level.getGameTime()-previousWorkTime==40,"work cadence is not40ordinary world ticks");
               previousWorkTime=level.getGameTime();
               var sample=new JsonObject();sample.addProperty("world_tick",level.getGameTime());sample.addProperty("machine_tick",counter);sample.addProperty("virtual_mb",work);sample.addProperty("extractible_mb",total);SAMPLES.add(sample);
               require(total==0,"unpaid work became extractible fluid");
            }
            lastProgress=work;
         }
         if(!reload()&&work==750) {
            require(level.getFluidState(NEAR.above(17)).isSource(),"source vanished before payment");
            require(!level.getFluidState(FAR.above()).isEmpty()&&!level.getFluidState(FAR.above()).isSource(),"17-edge control has not reached its inlet");
            RESULT.addProperty("saved_pending_mb",750);RESULT.addProperty("saved_machine_tick",counter);
            stopClockAndSave(server);return;
         }
         if(reload()&&total>0) {
            require(total==1000,"paid source volume is not conserved");
            require(!level.getFluidState(NEAR.above(17)).isSource(),"credit appeared before source removal");
            if(paidCounter<0){paidCounter=counter;require(counter-reloadCounter==40-(reloadCounter%40),"saved tick phase did not resume at the next work boundary");RESULT.addProperty("paid_machine_tick",counter);}
            if(counter-paidCounter>=160) {
               RESULT.addProperty("total_paid_mb",total);RESULT.addProperty("funnel_mb",machine.fluidHandler(null).getFluidInTank(0).getAmount());
               RESULT.addProperty("ie_barrel_mb",barrelAmount(level,NEAR));RESULT.addProperty("post_payment_ticks",counter-paidCounter);
               RESULT.addProperty("out_of_range_mb",farTotal);RESULT.addProperty("source16_removed",true);RESULT.addProperty("source17_preserved",true);
               stopClockAndSave(server);
            }
         }
      }catch(Throwable error){failure=error;monitoring=false;}
   }
   private static void stopClockAndSave(MinecraftServer server){server.tickRateManager().setFrozen(true);server.saveEverything(false,true,true);monitoring=false;ready=true;}
   private static MachineBlockEntity machine(ServerLevel level,BlockPos pos){require(level.hasChunkAt(pos),"machine chunk unavailable");var be=level.getBlockEntity(pos);require(be instanceof MachineBlockEntity,"missing Funnel");return (MachineBlockEntity)be;}
   private static int barrelAmount(ServerLevel level,BlockPos pos){require(level.hasChunkAt(pos.below(2)),"destination chunk unavailable");var tank=level.getCapability(Capabilities.FluidHandler.BLOCK,pos.below(2),Direction.UP);require(tank!=null,"IE barrel has no input");return tank.getFluidInTank(0).getAmount();}
   private static int total(ServerLevel level,BlockPos pos){return machine(level,pos).fluidHandler(null).getFluidInTank(0).getAmount()+barrelAmount(level,pos);}
   private static void finish(Minecraft mc,Throwable error){
      if(finished)return;finished=true;
      try{RESULT.addProperty("status",error==null?"passed":"failed");RESULT.addProperty("error",error==null?null:error.toString());RESULT.addProperty("actual_process_restart",reload());RESULT.addProperty("tick_source","ordinary block-entity ticker, observed after real server ticks");RESULT.add("work_samples",SAMPLES);if(output!=null)Files.writeString(output.resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(RESULT)+"\n");}
      catch(Exception problem){EngineersDecorReforged.LOGGER.error("Cannot record native Funnel validation",problem);}
      if(error!=null)EngineersDecorReforged.LOGGER.error("Native Funnel validation failed",error);mc.stop();
   }
   private static void require(boolean condition,String text){if(!condition)throw new IllegalStateException(text);}
   private NativeFunnelRuntimeValidation(){}
}
