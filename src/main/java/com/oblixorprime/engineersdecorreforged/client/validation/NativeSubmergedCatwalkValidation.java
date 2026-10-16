package com.oblixorprime.engineersdecorreforged.client.validation;
import com.google.gson.*;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/** Player-eye underwater reproduction, all six established support models, fixture world only. */
final class NativeSubmergedCatwalkValidation {
   private static final BlockPos POS=new BlockPos(5,80,5);
   private static final Vec3 CAMERA=new Vec3(5.5,78.8,3.4);
   private static final JsonArray RESULTS=new JsonArray();
   private static int index,stage,ticks;
   private static CompletableFuture<?> pending;
   private static BlockState serverState;
   private static FluidState serverFluid;
   private static int variant(){return index/3;}
   private static String mode(){return new String[]{"dry","source-submerged","flow-submerged"}[index%3];}
   static boolean tick(Minecraft mc,Path directory,String nonce)throws Exception {
      if(index==18)return true;
      if(stage==0){
         pending=mc.getSingleplayerServer().submit(()->{
            var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
            // Remove sources left by earlier fixtures before rebuilding this enclosed scene.
            for(var p:BlockPos.betweenClosed(POS.offset(-6,-3,-6),POS.offset(10,4,6))) level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
            level.getFluidTicks().clearArea(net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(POS.offset(-6,-3,-6),POS.offset(10,4,6)));
            for(var p:BlockPos.betweenClosed(POS.offset(-4,-2,-4),POS.offset(4,3,4))) {
               boolean boundary=p.getX()==1 || p.getX()==9 || p.getZ()==1 || p.getZ()==9;
               var state=p.getY()==78?Blocks.STONE.defaultBlockState():boundary?Blocks.GLASS.defaultBlockState():index%3!=0 && p.getY()<=81?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState();
               level.setBlock(p,state,3);
            }
            var support=switch(variant()){case 1->ModBlocks.THICK_STEEL_POLE.get().defaultBlockState().setValue(DirectionalBlock.FACING,net.minecraft.core.Direction.UP);case 2->ModBlocks.THIN_STEEL_POLE.get().defaultBlockState().setValue(DirectionalBlock.FACING,net.minecraft.core.Direction.UP);case 3->Blocks.STONE.defaultBlockState();case 5->ModBlocks.TREATED_WOOD_POLE.get().defaultBlockState().setValue(DirectionalBlock.FACING,net.minecraft.core.Direction.UP);default->index%3==0?Blocks.AIR.defaultBlockState():Blocks.WATER.defaultBlockState();};
            if(index%3!=0 && support.getBlock() instanceof NativeFluidloggedBlock)support=NativeFluidloggedBlock.placement(support,Fluids.WATER.getSource(false));
            level.setBlock(POS.below(),support,3);
            var state=ModBlocks.STEEL_CATWALK_TA.get().defaultBlockState().setValue(RaisedCatwalkBlock.VARIANT,variant()).setValue(RaisedCatwalkBlock.PAID_LIGHT,variant()==4);
            if(index%3==1)state=NativeFluidloggedBlock.placement(state,Fluids.WATER.getSource(false));
            if(index%3==2){level.setBlock(POS.above(),Blocks.GLASS.defaultBlockState(),3);state=StairWaterFlow.withFlow(state,Fluids.FLOWING_WATER.getFlowing(7,false));}
            level.setBlock(POS,state,3);player.connection.teleport(CAMERA.x,CAMERA.y,CAMERA.z,0,0);
         });stage=1;ticks=0;return false;
      }
      if(stage==1){
         if(!pending.isDone())return false;pending.join();
         check(++ticks<300,"camera sync timeout");if(mc.player.position().distanceTo(CAMERA)>.1 || !mc.level.getBlockState(POS).is(ModBlocks.STEEL_CATWALK_TA.get()))return false;
         mc.options.hideGui=true;mc.levelRenderer.allChanged();stage=2;ticks=0;return false;
      }
      var delta=Vec3.atCenterOf(POS).subtract(mc.player.getEyePosition());
      mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));
      if(stage==2){
         if(++ticks<35)return false;
         pending=mc.getSingleplayerServer().submit(()->{var level=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();serverState=level.getBlockState(POS);serverFluid=level.getFluidState(POS);});stage=3;return false;
      }
      if(!pending.isDone())return false;pending.join();var state=mc.level.getBlockState(POS);var fluid=mc.level.getFluidState(POS);
      check(state.equals(serverState) && fluid.equals(serverFluid),"server/client state disagreement");
      check(state.getValue(RaisedCatwalkBlock.VARIANT)==variant(),"support variant changed");
      check(state.getValue(RaisedCatwalkBlock.PAID_LIGHT)==(variant()==4),"paid lamp state changed");
      check(index%3==0?fluid.isEmpty():!fluid.isEmpty() && fluid.isSource()==(index%3==1),"source/current identity changed");
      if(index%3!=0)check(!mc.level.getFluidState(BlockPos.containing(mc.player.getEyePosition())).isEmpty(),"camera must actually be immersed");
      else for(var p:BlockPos.betweenClosed(POS.offset(-3,-1,-3),POS.offset(3,2,3))) check(mc.level.getFluidState(p).isEmpty(),"dry scene retained fluid at "+p);
      var output=directory.resolve("submerged-catwalk-screenshots");Files.createDirectories(output);String filename="variant-"+variant()+"-"+mode()+".png";
      try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve(filename));}
      var row=new JsonObject();row.addProperty("variant",variant());row.addProperty("mode",mode());row.addProperty("server_client_agree",true);row.addProperty("paid_state_preserved",true);row.addProperty("immersed_camera",index%3!=0);row.addProperty("amount",fluid.getAmount());row.addProperty("source",fluid.isSource());row.addProperty("screenshot",filename);RESULTS.add(row);
      index++;stage=0;ticks=0;
      if(index==18){var proof=new JsonObject();proof.addProperty("suite","submerged-catwalk-v1");proof.addProperty("nonce",nonce);proof.addProperty("status","passed");proof.add("cases",RESULTS);Files.writeString(directory.resolve("client-submerged-catwalk-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");return true;}
      return false;
   }
   private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message+" variant="+variant()+" mode="+mode());}
}
