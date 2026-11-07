package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.MachineScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.dries007.tfc.common.entities.livestock.DairyAnimal;
import net.dries007.tfc.common.entities.livestock.Gender;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.config.ConfigBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.lwjgl.glfw.GLFW;

/** Isolated, opt-in actual TFC cow/client proof. Never owner visual acceptance. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class NativeTfcMilkingRecoveryValidation {
   private static final String SUITE="native-tfc-milking-recovery-v1";
   private static final BlockPos POS=new BlockPos(0,80,0);
   private enum Stage { BOOT,WORLD,READY,RECOVERING,TOOLTIP,SECOND,RESTORE,DONE }
   private static Stage stage=Stage.BOOT;
   private static Path directory;
   private static String nonce;
   private static UUID playerId;
   private static long deadline;
   private static int ticks,serverMenuId,stableObservations;
   private static String stableKey="";
   private static boolean complete,isolatedDirectoryValidated,configRestored;
   private static MachineBlockEntity machine;
   private static DairyAnimal cow;
   private static ModConfigSpec.IntValue duration;
   private static int previousDuration;
   private static CompletableFuture<?> pending;
   private static CompletableFuture<JsonObject> inspection;
   private static final JsonArray CAPTURES=new JsonArray();
   private static final JsonObject NO_REPEAT=new JsonObject();
   private static JsonObject first;
   private static String tfcVersion;
   private NativeTfcMilkingRecoveryValidation() {}
   @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
      if(!Boolean.getBoolean("iedctr.tfcMilkingRecoveryNative") || complete)return;
      Minecraft mc=Minecraft.getInstance();
      try {
         if(deadline==0)deadline=System.nanoTime()+900_000_000_000L;
         require(System.nanoTime()<deadline && ++ticks<6000,"Native TFC timeout at "+stage);
         if(stage==Stage.BOOT){if(mc.screen instanceof TitleScreen && mc.getOverlay()==null)boot(mc);return;}
         if(stage==Stage.WORLD){
            if(mc.level==null || mc.player==null || mc.gameMode==null || mc.screen!=null)return;
            require(mc.getSingleplayerServer()!=null && mc.player.connection.getConnection().isMemoryConnection(),"Only isolated integrated worlds permitted");
            playerId=mc.player.getUUID();
            pending=mc.getSingleplayerServer().submit(()->{
               var p=player(mc);p.setGameMode(GameType.SURVIVAL);var level=p.serverLevel();
               for(int x=-1;x<=1;x++)for(int z=-2;z<=4;z++)level.setBlock(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState(),3);
               p.connection.teleport(.5,81,3.5,180,0);
               level.setBlock(POS,ModBlocks.SMALL_MILKING_MACHINE.get().defaultBlockState().setValue(MachineBlocks.HORIZONTAL_FACING,Direction.NORTH),3);
               level.setBlock(POS.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
               require(level.getBlockEntity(POS) instanceof MachineBlockEntity,"Missing machine");
               machine=(MachineBlockEntity)level.getBlockEntity(POS);
               duration=(ModConfigSpec.IntValue)((ConfigBuilder.ServerValue<?>)TFCConfig.SERVER.cowConfig.produceTicks()).value();
               previousDuration=duration.get();duration.set(400);
               cow=(DairyAnimal)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("tfc","cow")).create(level);
               require(cow!=null,"Missing real TFC cow");
               cow.setPos(.5,80,-.5);cow.setNoAi(true);cow.setNoGravity(true);cow.setGender(Gender.FEMALE);
               cow.setBirthTick(cow.calendar().getTicks()-1_000_000_000L);cow.setOldTick(-1);cow.setFamiliarity(1);cow.setUses(0);
               // Initial ready fixture only. Neither produced tick nor calendar is edited after setup.
               cow.setProducedTick(-1_000_000_000L);cow.setCustomName(Component.literal("Native TFC Cow"));cow.setPersistenceRequired();
               require(cow.isReadyForAnimalProduct() && level.addFreshEntity(cow),"Real cow is not ready/spawned");
               require(machine.fluidHandler(Direction.UP).getFluidInTank(0).isEmpty(),"Tank must start empty");
               p.openMenu(machine);require(p.containerMenu instanceof MachineMenu,"Wrong menu");serverMenuId=p.containerMenu.containerId;
            });enter(Stage.READY);return;
         }
         require(mc.level!=null && mc.player!=null && mc.getSingleplayerServer()!=null,"World disconnected");
         if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
         if(stage==Stage.RESTORE){finish(mc,null);return;}
         if(!(mc.screen instanceof MachineScreen.SmallMilkingMachineScreen screen) || !(mc.player.containerMenu instanceof MachineMenu menu) || menu.containerId!=serverMenuId)return;
         if(stage==Stage.READY)cursor(mc,screen,false);
         if(ticks<12)return;
         JsonObject state=observe(mc,menu);if(state==null)return;
         String key=state.get("server_uuid").getAsString()+":"+number(state,"amount")+":"+number(state,"client_state_code");
         if(!key.equals(stableKey)){stableKey=key;stableObservations=0;}
         if(++stableObservations<8)return;
         switch(stage){
            case READY -> {
               if(number(state,"amount")!=0 || number(state,"client_state_code")!=MilkingState.READY)return;
               require(state.get("ready").getAsBoolean(),"Native ready state mismatch");
               capture(mc,menu,state,"ready",false);
               pending=mc.getSingleplayerServer().submit(()->player(mc).serverLevel().setBlock(POS.east(),Blocks.AIR.defaultBlockState(),3));
               cursor(mc,screen,false);enter(Stage.RECOVERING);
            }
            case RECOVERING -> {
               if(number(state,"amount")!=1000 || number(state,"client_state_code")!=MilkingState.RECOVERING)return;
               require(number(state,"uses")==1 && number(state,"remaining_ticks")>100,"First native production missing");
               first=state.deepCopy();NO_REPEAT.add("first",first);capture(mc,menu,state,"recovering",false);
               cursor(mc,screen,true);enter(Stage.TOOLTIP);
            }
            case TOOLTIP -> {
               if(number(state,"calendar_tick")-number(first,"calendar_tick")<80)return;
               require(number(state,"amount")==1000 && number(state,"uses")==1 && number(state,"produced_tick")==number(first,"produced_tick") && number(state,"remaining_ticks")>0,"Unexpected replay before native expiry");
               NO_REPEAT.add("later",state.deepCopy());capture(mc,menu,state,"recovering-tooltip",true);
               cursor(mc,screen,false);enter(Stage.SECOND);
            }
            case SECOND -> {
               if(number(state,"amount")<2000)return;
               require(number(state,"amount")==2000 && number(state,"uses")==2 && number(state,"produced_tick")-number(first,"produced_tick")>=400,"Second production preceded native expiry");
               capture(mc,menu,state,"post-expiry",false);
               pending=mc.getSingleplayerServer().submit(()->{duration.set(previousDuration);configRestored=duration.get()==previousDuration;require(configRestored,"Config restoration failed");});
               enter(Stage.RESTORE);
            }
            default -> throw new IllegalStateException("Unexpected stage "+stage);
         }
      }catch(Throwable error){finish(mc,error);}
   }
   private static void boot(Minecraft mc) throws Exception {
      require(!Boolean.getBoolean("iedctr.milkingQualificationNative") && !Boolean.getBoolean("iedctr.clientGameplay") && !Boolean.getBoolean("iedctr.barrelReferenceNative") && !Boolean.getBoolean("iedctr.solarV2ReferenceNative"),"Dedicated fixture only");
      nonce=System.getProperty("iedctr.clientGameplayNonce","");
      require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha","").matches("[a-f0-9]{40}"),"Missing nonce or exact revision");
      directory=mc.gameDirectory.toPath().toRealPath();
      require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory","")).toRealPath()) && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce),"Isolation marker mismatch");
      require(mc.level==null && mc.getSingleplayerServer()==null && !Files.exists(directory.resolve("tfc-milking-recovery-screenshots"))
         && !Files.exists(directory.resolve("client-tfc-milking-recovery-result.json")),"Refusing existing world/captures/result");
      if(Files.exists(directory.resolve("saves"))) try(var entries=Files.list(directory.resolve("saves"))) { require(entries.findAny().isEmpty(),"Existing saves forbidden"); }
      isolatedDirectoryValidated=true;
      tfcVersion=net.neoforged.fml.ModList.get().getModContainerById("tfc").orElseThrow().getModInfo().getVersion().toString();
      require(tfcVersion.equals("4.2.12"),"Wrong native TFC runtime version");
      verifyViewport(mc);
      verifyArt(mc,"background","871f7c4512227153a4c49591c5facdbc95b1443114cc3206659d6dd524d1326b");
      verifyArt(mc,"milk","f6f0507deb081a2a83b6bb455abc6237ac2b41f4ab21d43c833e356fb2d9bbe5");
      String name="iedctr-tfc-milking-recovery-"+nonce;
      mc.options.pauseOnLostFocus=false;
      enter(Stage.WORLD);
      mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
         new WorldOptions(0L,false,false),registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
   }
   private static void verifyArt(Minecraft mc,String name,String expected)throws Exception{
      var id=ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID,"textures/gui/milking_redesign_v1/"+name+".png");
      try(var input=mc.getResourceManager().getResourceOrThrow(id).open()){
         require(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes())).equals(expected),"Active resource artwork differs: "+name);
      }
   }
   private static void verifyViewport(Minecraft mc) {
      require(mc.getLanguageManager().getSelected().equals(System.getProperty("iedctr.requestedLanguage","")),"Requested locale not applied");
      int requested=Integer.getInteger("iedctr.requestedGuiScale",0);
      var window=mc.getWindow();double actual=window.getGuiScale();
      require(requested>=1 && requested<=3 && mc.options.guiScale().get()==requested && actual==requested,
         "Requested GUI scale was not applied");
      require(window.getWidth()==Integer.getInteger("iedctr.requestedWidth",0)
         && window.getHeight()==Integer.getInteger("iedctr.requestedHeight",0),"Requested framebuffer size was not applied");
      require(window.getGuiScaledWidth()==(int)Math.ceil(window.getWidth()/actual)
         && window.getGuiScaledHeight()==(int)Math.ceil(window.getHeight()/actual),"Scaled GUI dimensions differ");
   }
   private static void addViewport(JsonObject proof,Minecraft mc) {
      var window=mc.getWindow();
      proof.addProperty("requested_width",Integer.getInteger("iedctr.requestedWidth",0));proof.addProperty("requested_height",Integer.getInteger("iedctr.requestedHeight",0));
      proof.addProperty("requested_gui_scale",Integer.getInteger("iedctr.requestedGuiScale",0));
      proof.addProperty("actual_gui_scale",window.getGuiScale());
      proof.addProperty("gui_width",window.getGuiScaledWidth());proof.addProperty("gui_height",window.getGuiScaledHeight());
      proof.addProperty("framebuffer_width",window.getWidth());proof.addProperty("framebuffer_height",window.getHeight());
   }

   private static JsonObject observe(Minecraft mc,MachineMenu menu) {
      if(inspection==null){inspection=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);require(p.containerMenu.containerId==serverMenuId && p.serverLevel().getBlockEntity(POS)==machine,"Server identity changed");
         var s=((MachineMenu)p.containerMenu).milkingState();
         require(s.source()==MilkingState.TFC && cow.getUUID().equals(s.animalId()) && cow.getId()==s.entityId(),"Wrong authoritative source");
         var o=new JsonObject();o.addProperty("server_uuid",cow.getUUID().toString());o.addProperty("server_entity_id",cow.getId());o.addProperty("server_name",cow.getDisplayName().getString());
         o.addProperty("source",BuiltInRegistries.ENTITY_TYPE.getKey(cow.getType()).toString());o.addProperty("server_source","TFC");o.addProperty("server_state",s.state()==MilkingState.READY?"READY":"RECOVERING");
         o.addProperty("amount",machine.fluidHandler(Direction.UP).getFluidInTank(0).getAmount());o.addProperty("uses",cow.getUses());
         o.addProperty("produced_tick",cow.getProducedTick());o.addProperty("calendar_tick",cow.calendar().getTicks());
         o.addProperty("remaining_ticks",Math.max(0,cow.getProductsCooldown()));o.addProperty("total_ticks",TFCConfig.SERVER.cowConfig.produceTicks().get());o.addProperty("ready",cow.isReadyForAnimalProduct());
         require(Math.abs(s.remainingTicks()-Math.max(0,cow.getProductsCooldown()))<=2,"Server projection differs from native calendar");
         return o;
      });return null;}
      if(!inspection.isDone())return null;
      var o=inspection.join();inspection=null;
      var s=menu.milkingState();var entity=mc.level.getEntity(s.entityId());
      if(entity==null || s.animalId()==null || !s.animalId().equals(entity.getUUID()) || !entity.getUUID().toString().equals(o.get("server_uuid").getAsString()) || menu.fluidAmount()!=number(o,"amount") || Math.abs(s.remainingTicks()-number(o,"remaining_ticks"))>30)return null;
      require(entity instanceof DairyAnimal && s.source()==MilkingState.TFC,"Client is not the real TFC source");
      o.addProperty("client_source","TFC");o.addProperty("client_uuid",entity.getUUID().toString());o.addProperty("client_entity_id",entity.getId());o.addProperty("client_name",entity.getDisplayName().getString());
      o.addProperty("client_amount",menu.fluidAmount());o.addProperty("client_remaining_ticks",s.remainingTicks());o.addProperty("client_total_ticks",s.totalTicks());
      o.addProperty("client_state_code",s.state());o.addProperty("client_state",s.state()==MilkingState.READY?"READY":"RECOVERING");return o;
   }
   private static void cursor(Minecraft mc,MachineScreen screen,boolean tooltip){
      double x=tooltip?screen.getGuiLeft()+180:screen.width-2,y=tooltip?screen.getGuiTop()+63:screen.height-2;
      GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),x*mc.getWindow().getScreenWidth()/screen.width,y*mc.getWindow().getScreenHeight()/screen.height);
   }
   private static void capture(Minecraft mc,MachineMenu menu,JsonObject state,String phase,boolean tooltip)throws Exception{
      verifyViewport(mc);require(mc.getOverlay()==null,"Loading overlay");var screen=(MachineScreen)mc.screen;
      require(screen.getXSize()==228 && screen.getYSize()==228 && menu.slots.size()==38,"Wrong canvas/menu");
      var o=state.deepCopy();addViewport(o,mc);o.addProperty("phase",phase);o.addProperty("name",phase+".png");
      o.addProperty("menu_id",menu.containerId);o.addProperty("server_menu_id",serverMenuId);
      o.addProperty("screen_class",screen.getClass().getName());o.addProperty("tooltip_hovered",tooltip);
      int hoverX=(int)(mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth());
      int hoverY=(int)(mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight());
      if(tooltip)require(hoverX>=screen.getGuiLeft()+160 && hoverX<screen.getGuiLeft()+207 && hoverY>=screen.getGuiTop()+56 && hoverY<screen.getGuiTop()+73,"Actual cursor is outside native recovery tooltip");
      o.addProperty("hover_x",hoverX);o.addProperty("hover_y",hoverY);o.addProperty("settled_observations",stableObservations);
      require(menu.slots.stream().allMatch(s->s.getItem().isEmpty()) && menu.getCarried().isEmpty(),"Fixture inventory changed");
      int scale=(int)mc.getWindow().getGuiScale(),x=(screen.getGuiLeft()+27)*scale,y=(screen.getGuiTop()+34)*scale,w=47*scale,h=41*scale;
      Path folder=directory.resolve("tfc-milking-recovery-screenshots");Files.createDirectories(folder);
      Path file=folder.resolve(phase+".png"),portrait=folder.resolve(phase+"-portrait.png");require(!Files.exists(file) && !Files.exists(portrait),"Stale capture");
      try(var frame=Screenshot.takeScreenshot(mc.getMainRenderTarget());var crop=new NativeImage(w,h,false)){
         frame.writeToFile(file);for(int iy=0;iy<h;iy++)for(int ix=0;ix<w;ix++)crop.setPixelRGBA(ix,iy,frame.getPixelRGBA(x+ix,y+iy));crop.writeToFile(portrait);
      }
      o.addProperty("sha256",hash(file));o.addProperty("portrait_name",phase+"-portrait.png");o.addProperty("portrait_sha256",hash(portrait));
      o.addProperty("portrait_x",x);o.addProperty("portrait_y",y);o.addProperty("portrait_width",w);o.addProperty("portrait_height",h);CAPTURES.add(o);
   }
   private static long number(JsonObject o,String key){return o.get(key).getAsLong();}
   private static String hash(Path file)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
   private static void enter(Stage next){stage=next;ticks=0;inspection=null;stableObservations=0;stableKey="";}
   private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);require(p!=null,"Player vanished");return p;}
   private static void finish(Minecraft mc,Throwable error){
      complete=true;
      if(!isolatedDirectoryValidated){EngineersDecorReforged.LOGGER.error("IEDCTR_TFC_MILKING_RECOVERY rejected before isolation validation",error);return;}
      // Restoration is executed on the authoritative server, also on failure.
      if(duration!=null && !configRestored && mc.getSingleplayerServer()!=null){
         try{mc.getSingleplayerServer().submit(()->{duration.set(previousDuration);configRestored=duration.get()==previousDuration;}).join();}
         catch(Throwable restoreError){if(error==null)error=restoreError;}
      }
      try{
         var p=new JsonObject();p.addProperty("suite",SUITE);p.addProperty("status",error==null?"passed_bounded_smoke":"failed");
         p.addProperty("nonce",nonce);p.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha"));
         p.addProperty("requested_language",System.getProperty("iedctr.requestedLanguage",""));p.addProperty("selected_language",mc.getLanguageManager().getSelected());
         p.addProperty("source_background_sha256","871f7c4512227153a4c49591c5facdbc95b1443114cc3206659d6dd524d1326b");
         p.addProperty("source_milk_sha256","f6f0507deb081a2a83b6bb455abc6237ac2b41f4ab21d43c833e356fb2d9bbe5");
         addViewport(p,mc);p.addProperty("actual_integrated_world",mc.getSingleplayerServer()!=null);p.addProperty("native_visual_acceptance",false);
         p.addProperty("tfc_version",tfcVersion);p.addProperty("native_tick_production",true);p.addProperty("produced_tick_mutated_after_setup",false);p.addProperty("config_restored",configRestored);
         p.addProperty("screen_class",MachineScreen.SmallMilkingMachineScreen.class.getName());p.add("captures",CAPTURES);p.add("no_repeat",NO_REPEAT);
         if(error!=null)p.addProperty("error",error.toString());
         Files.writeString(directory.resolve("client-tfc-milking-recovery-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(p)+"\n");
      }catch(Exception writeError){EngineersDecorReforged.LOGGER.error("TFC recovery proof write failed",writeError);}
      if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_TFC_MILKING_RECOVERY PASSED nonce={}",nonce);
      else EngineersDecorReforged.LOGGER.error("IEDCTR_TFC_MILKING_RECOVERY FAILED at "+stage,error);
      mc.stop();
   }
   private static void require(boolean b,String message){if(!b)throw new IllegalStateException(message);}
}
