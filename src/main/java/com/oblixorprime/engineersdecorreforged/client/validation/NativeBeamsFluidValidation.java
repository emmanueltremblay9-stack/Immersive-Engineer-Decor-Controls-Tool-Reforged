package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.SteelDoubleTBulbBlock;
import com.oblixorprime.engineersdecorreforged.block.TreatedWoodBeamBlock;
import com.oblixorprime.engineersdecorreforged.block.TreatedWoodBeamBlock.Mount;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Opt-in inspection of the existing owned cloud world; never creates a world or profile. */
@EventBusSubscriber(modid=EngineersDecorReforged.MOD_ID,value=Dist.CLIENT)
public final class NativeBeamsFluidValidation {
   private static final BlockPos POS=new BlockPos(0,80,0);
   private static final BlockPos SAVED_BEAM=POS.west(3),SAVED_FUNNEL=POS.east(2);
   private static final JsonArray RESULTS=new JsonArray();
   private static CompletableFuture<?> pending;
   private static Path output;
   private static Vec3 camera,target;
   private static int scene,stage,ticks;
   private static boolean guarded,finished;
   private static final String[] SCENES={"wood-wall-junction","wood-ceiling-junction","barrel-valve-ie"};

   @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
      if(!Boolean.getBoolean("iedctr.beamsFluidValidation")||finished)return;
      var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
      try {
         if(!guarded) {
            String nonce=System.getProperty("iedctr.clientGameplayNonce","");require(nonce.matches("[0-9a-f]{32}"),"missing owned-world nonce");
            Path game=mc.gameDirectory.toPath().toRealPath();
            require(Files.readString(game.resolve(".iedctr-client-gameplay-nonce")).trim().equals(nonce),"different profile marker");
            require(mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toRealPath().equals(game.resolve("saves/iedctr-ci-"+nonce).toRealPath()),"refusing a different saved world");
            output=Path.of(System.getProperty("iedctr.beamsFluidEvidence")).toAbsolutePath();Files.createDirectories(output);
            inspectBakedWood(mc);guarded=true;
         }
         if(Boolean.getBoolean("iedctr.beamsFluidReload")) {
            if(stage==0){pending=mc.getSingleplayerServer().submit(()->inspectSaved(serverPlayer(mc).serverLevel()));camera=Vec3.atCenterOf(POS).add(4,1,-6).add(0,-mc.player.getEyeHeight(),0);target=Vec3.atCenterOf(POS);stage=1;}
            if(!pending.isDone())return;pending.join();mc.options.fov().set(40);camera(mc);if(++ticks<100)return;
            capture(mc,"reload.png");finish(mc,null);return;
         }
         if(stage==0) {
            target=Vec3.atCenterOf(POS);Vec3 eye=target.add(4,scene==1?1.5:1,-6);camera=eye.add(0,-mc.player.getEyeHeight(),0);
            pending=mc.getSingleplayerServer().submit(()->setup(mc,scene));stage=1;ticks=0;return;
         }
         if(stage==1) {
            if(!pending.isDone())return;pending.join();require(++ticks<300,"camera or scene synchronization timed out");
            if(mc.player.position().distanceTo(camera)>.1||ticks<50)return;
            mc.options.hideGui=true;mc.options.fov().set(40);mc.levelRenderer.allChanged();stage=2;ticks=0;
         }
         if(stage==2) {
            camera(mc);if(++ticks<60)return;capture(mc,SCENES[scene]+".png");
            if(++scene<SCENES.length){stage=0;ticks=0;return;}
            pending=mc.getSingleplayerServer().submit(()->{
               inspectSaved(serverPlayer(mc).serverLevel());
               serverPlayer(mc).openMenu((MachineBlockEntity)serverPlayer(mc).serverLevel().getBlockEntity(POS));
            });stage=3;ticks=0;return;
         }
         if(stage==3) {
            if(!pending.isDone())return;pending.join();require(++ticks<300,"barrel menu synchronization timed out");
            if(!(mc.player.containerMenu instanceof MachineMenu menu)||menu.fluidCapacity()!=32000||menu.fluidAmount()!=17500||mc.screen==null||ticks<50)return;
            mc.options.hideGui=false;capture(mc,"barrel-32000-gui.png");mc.player.closeContainer();
            var proof=new JsonObject();proof.addProperty("menu_capacity",menu.fluidCapacity());proof.addProperty("menu_amount",menu.fluidAmount());RESULTS.add(proof);
            finish(mc,null);
         }
      }catch(Throwable failure){finish(mc,failure);}
   }
   private static ServerPlayer serverPlayer(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
   private static void setup(Minecraft mc,int scene) {
      var player=serverPlayer(mc);var level=player.serverLevel();
      for(var pos:BlockPos.betweenClosed(POS.offset(-4,-3,-4),POS.offset(4,4,4)))require(level.hasChunkAt(pos),"unavailable fixture chunk");
      for(var pos:BlockPos.betweenClosed(POS.offset(-4,-3,-4),POS.offset(4,4,4)))level.setBlock(pos,pos.getY()==77?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
      player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.onUpdateAbilities();
      player.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(ModBlocks.TREATED_WOOD_BEAM.get()));
      level.setWeatherParameters(100000,0,false,false);level.setDayTime(6000);player.connection.teleport(camera.x,camera.y,camera.z,0,0);
      if(scene==0) {
         for(int x=-2;x<=2;x++)for(int y=-1;y<=2;y++)level.setBlock(POS.offset(x,y,1),Blocks.BRICKS.defaultBlockState(),3);
         level.setBlock(POS,wood(Mount.WALL_VERTICAL),3);level.setBlock(POS.west(),wood(Mount.WALL_HORIZONTAL),3);level.setBlock(POS.east(),wood(Mount.WALL_HORIZONTAL),3);
      } else if(scene==1) {
         level.setBlock(POS,wood(Mount.CEILING),3);
         level.setBlock(POS.below(),ModBlocks.TREATED_WOOD_POLE.get().defaultBlockState().setValue(BlockStateProperties.FACING,Direction.UP),3);
         level.setBlock(POS.below(2),ModBlocks.TREATED_WOOD_POLE.get().defaultBlockState().setValue(BlockStateProperties.FACING,Direction.UP),3);
         level.setBlock(POS.east(),ModBlocks.STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(SteelDoubleTBulbBlock.BULB_LIGHT,SteelDoubleTBulbBlock.Bulb.PAID),3);
      } else {
         level.setBlock(POS,ModBlocks.FLUID_BARREL.get().defaultBlockState().setValue(MachineBlocks.FACING,Direction.NORTH),3);
         level.setBlock(POS.below(),ModBlocks.STRAIGHT_PIPE_VALVE.get().defaultBlockState().setValue(MachineBlocks.FACING,Direction.UP),3);
         var pipePos=POS.below(2);var pipe=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering","fluid_pipe"));
         level.setBlock(pipePos,pipe.defaultBlockState(),3);
         var pipeEntity=(blusunrize.immersiveengineering.common.blocks.metal.FluidPipeBlockEntity)level.getBlockEntity(pipePos);
         pipeEntity.onNeighborBlockChange(POS.below());
         var inlet=level.getCapability(Capabilities.FluidHandler.BLOCK,pipePos,Direction.DOWN);
         var barrel=(MachineBlockEntity)level.getBlockEntity(POS);var tank=barrel.fluidHandler(null);
         int accepted=inlet.fill(new FluidStack(Fluids.WATER,1000),FluidAction.SIMULATE);require(accepted>0&&accepted<=1000,"IE route unavailable");
         tank.fill(new FluidStack(Fluids.WATER,17500-accepted),FluidAction.EXECUTE);
         require(inlet.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==accepted,"IE route changed between simulation and actual fill");
         require(tank.getFluidInTank(0).getAmount()==17500,"IE pipe/valve/barrel volume mismatch");
         level.setBlock(SAVED_BEAM.south(),Blocks.BRICKS.defaultBlockState(),3);level.setBlock(SAVED_BEAM,wood(Mount.WALL_HORIZONTAL),3);
         level.setBlock(SAVED_FUNNEL,ModBlocks.SMALL_FLUID_FUNNEL.get().defaultBlockState(),3);
         require(((MachineBlockEntity)level.getBlockEntity(SAVED_FUNNEL)).fluidHandler(null).fill(markedFluid(),FluidAction.EXECUTE)==1250,"generic component stack rejected");
         var proof=new JsonObject();proof.addProperty("actual_ie_forwarded_mb",accepted);proof.addProperty("barrel_amount",17500);proof.addProperty("barrel_capacity",32000);RESULTS.add(proof);
      }
   }
   private static FluidStack markedFluid() {
      var fluid=BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering","creosote"));require(fluid!=Fluids.EMPTY,"missing pinned IE creosote");
      var stack=new FluidStack(fluid,1250);var tag=new CompoundTag();tag.putString("native_fixture_marker","component-round-trip");
      stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;
   }
   private static BlockState wood(Mount mount){return ModBlocks.TREATED_WOOD_BEAM.get().defaultBlockState().setValue(TreatedWoodBeamBlock.MOUNT,mount).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);}
   private static void inspectSaved(ServerLevel level) {
      require(level.hasChunkAt(POS)&&level.hasChunkAt(SAVED_FUNNEL)&&level.hasChunkAt(SAVED_BEAM),"saved fixture chunks unavailable");
      var barrel=(MachineBlockEntity)level.getBlockEntity(POS);var stored=barrel.fluidHandler(null).getFluidInTank(0);
      require(stored.is(Fluids.WATER)&&stored.getAmount()==17500&&barrel.fluidHandler(null).getTankCapacity(0)==32000,"barrel saved volume/capacity changed");
      var funnel=(MachineBlockEntity)level.getBlockEntity(SAVED_FUNNEL);var expected=markedFluid();var actual=funnel.fluidHandler(null).getFluidInTank(0);
      require(actual.getAmount()==1250&&FluidStack.isSameFluidSameComponents(actual,expected),"Funnel component/volume did not survive save/reload");
      require(level.getBlockState(SAVED_BEAM).is(ModBlocks.TREATED_WOOD_BEAM.get())&&level.getBlockState(SAVED_BEAM).getValue(TreatedWoodBeamBlock.MOUNT)==Mount.WALL_HORIZONTAL,"wood beam orientation did not survive save/reload");
      require(level.getBlockEntity(POS.below()) instanceof com.oblixorprime.engineersdecorreforged.utility.PipeValveBlockEntity,"valve BE not restored");
      var proof=new JsonObject();proof.addProperty("actual_process_reload",Boolean.getBoolean("iedctr.beamsFluidReload"));proof.addProperty("barrel_mb",17500);proof.addProperty("funnel_components_preserved",true);proof.addProperty("wood_orientation_preserved",true);RESULTS.add(proof);
      if(Boolean.getBoolean("iedctr.beamsFluidReload")) {
         var inlet=level.getCapability(Capabilities.FluidHandler.BLOCK,POS.below(2),Direction.DOWN);
         require(inlet!=null,"saved IE inlet capability not restored");
         int simulated=inlet.fill(new FluidStack(Fluids.WATER,1000),FluidAction.SIMULATE);
         require(simulated>0&&simulated<=1000&&barrel.fluidHandler(null).getFluidInTank(0).getAmount()==17500,"saved route simulation changed storage");
         require(inlet.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==simulated,"saved IE/valve route failed to transfer");
         require(barrel.fluidHandler(null).drain(simulated,FluidAction.EXECUTE).getAmount()==simulated,"test route volume could not be reconciled");
         require(barrel.fluidHandler(null).getFluidInTank(0).getAmount()==17500,"post-restart route failed conservation");
         proof.addProperty("post_restart_ie_actual_mb",simulated);
      }
   }
   private static void inspectBakedWood(Minecraft mc) {
      var shaper=mc.getBlockRenderer().getBlockModelShaper();var source=ModBlocks.TREATED_WOOD_POLE.get().defaultBlockState().setValue(BlockStateProperties.FACING,Direction.NORTH);
      var original=shaper.getBlockModel(source).getQuads(source,null,RandomSource.create(0),ModelData.EMPTY,null);
      require(original.size()==16,"unexpected straight pole source face count");
      int count=0;
      for(Mount mount:Mount.values())for(Direction facing:Direction.Plane.HORIZONTAL) {
         var state=wood(mount).setValue(BlockStateProperties.HORIZONTAL_FACING,facing);
         var quads=shaper.getBlockModel(state).getQuads(state,null,RandomSource.create(0),ModelData.EMPTY,null);
         require(quads.size()==16,"wood beam core face count changed");
         require(vertices(original,mount,facing,true).equals(vertices(quads,mount,facing,false)),"baked source geometry/UV mismatch: "+mount+" "+facing);count++;
      }
      var proof=new JsonObject();proof.addProperty("baked_rigid_source_cases",count);proof.addProperty("source_quads",original.size());proof.addProperty("textures_and_vertex_uvs_preserved",true);RESULTS.add(proof);
   }
   private static List<String> vertices(List<BakedQuad> quads,Mount mount,Direction facing,boolean turn) {
      var result=new ArrayList<String>();
      for(var quad:quads){String texture=quad.getSprite().contents().name().toString();require(!texture.endsWith("missingno"),"missing wood texture");int[] data=quad.getVertices();
         for(int i=0;i<4;i++){double x=Float.intBitsToFloat(data[i*8]),y=Float.intBitsToFloat(data[i*8+1]),z=Float.intBitsToFloat(data[i*8+2]);
            if(turn){double a,b,c;switch(mount){case WALL_VERTICAL:a=x;b=1-z;c=y+5.5/16;break;case WALL_HORIZONTAL:a=z;b=x;c=y+5.5/16;break;default:a=x;b=y+5.5/16;c=z;}
               switch(facing){case EAST:x=1-c;y=b;z=a;break;case SOUTH:x=1-a;y=b;z=1-c;break;case WEST:x=c;y=b;z=1-a;break;default:x=a;y=b;z=c;}}
            result.add(String.format(Locale.ROOT,"%s:%.6f,%.6f,%.6f:%.6f,%.6f",texture,x,y,z,Float.intBitsToFloat(data[i*8+4]),Float.intBitsToFloat(data[i*8+5])));}}
      Collections.sort(result);return result;
   }
   private static void camera(Minecraft mc) {
      mc.options.hideGui=true;mc.player.setNoGravity(true);mc.player.setDeltaMovement(Vec3.ZERO);Vec3 delta=target.subtract(mc.player.getEyePosition());
      mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));
   }
   private static void capture(Minecraft mc,String name)throws Exception{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve(name));}}
   private static void finish(Minecraft mc,Throwable failure) {
      if(finished)return;finished=true;
      try{var proof=new JsonObject();proof.addProperty("status",failure==null?"captured":"failed");proof.addProperty("error",failure==null?null:failure.toString());proof.add("checks",RESULTS);
         proof.addProperty("scope","Wood geometry, Barrel/IE valve route and Funnel tank persistence; automated source capture is a separate qualification");
         if(output!=null)Files.writeString(output.resolve(Boolean.getBoolean("iedctr.beamsFluidReload")?"reload-result.json":"native-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");
      }catch(Exception error){EngineersDecorReforged.LOGGER.error("Cannot record native beams/fluid validation",error);}
      if(failure!=null)EngineersDecorReforged.LOGGER.error("Native beams/fluid validation failed",failure);mc.stop();
   }
   private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
   private NativeBeamsFluidValidation(){}
}
