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
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
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
import org.lwjgl.glfw.GLFW;

/** Opt-in isolated native Mooshroom proof. Production uses normal server ticks only. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class NativeMooshroomMilkingValidation {
   private static final String SUITE="native-mooshroom-milking-v1";
   private static final BlockPos POS=new BlockPos(0,80,0);
   private enum Stage { BOOT,WORLD,RUN,PORTRAIT,TOOLTIP,DONE }
   private static Stage stage=Stage.BOOT;
   private static Path directory;
   private static String nonce,tfcVersion,stableKey="";
   private static UUID playerId;
   private static long deadline,startedTick,firstTick;
   private static int ticks,serverMenuId,stableObservations,hoverTicks,scenario;
   private static volatile boolean complete,monitoring,scenarioDone;
   private static boolean isolatedDirectoryValidated;
   private static volatile Throwable serverError;
   private static MachineBlockEntity machine;
   private static MushroomCow cow;
   private static CompletableFuture<?> pending;
   private static CompletableFuture<JsonObject> inspection;
   private static final JsonArray CAPTURES=new JsonArray(),PRODUCTION=new JsonArray(),BABIES=new JsonArray();
   private NativeMooshroomMilkingValidation() {}
   private static boolean baby(){return scenario>=2;}
   private static String variant(){return scenario%2==0?"RED":"BROWN";}

   @SubscribeEvent public static void serverTick(LevelTickEvent.Post event){
      if(!Boolean.getBoolean("iedctr.mooshroomMilkingNative") || !monitoring || complete || machine==null || event.getLevel()!=machine.getLevel())return;
      try{
         var level=machine.getLevel();long tick=level.getGameTime();
         // Setup can run during this level tick, before the new BE has ticked.
         // Ignore that instant only; every subsequent normal tick is observed.
         if(tick<=startedTick)return;
         var s=MilkingState.decode(machine.milkingDataAccessForTests()::get);
         int amount=machine.fluidHandler(Direction.UP).getFluidInTank(0).getAmount();
         require(cow instanceof Cow && cow.isAlive() && !cow.isRemoved(),"MushroomCow must be a live Cow subclass");
         if(baby()){
            require(cow.isBaby() && s.source()==MilkingState.NONE && s.animalId()==null && s.entityId()==0,"Baby was selected");
            require(amount==0 && machine.getItem(0).is(Items.BUCKET) && machine.getItem(0).getCount()==1 && machine.getItem(1).isEmpty(),"Baby produced milk");
            if(tick-startedTick<240)return;
            var result=new JsonObject();result.addProperty("variant",variant());result.addProperty("baby",true);
            result.addProperty("observed_ticks",tick-startedTick);result.addProperty("amount",amount);result.addProperty("bucket_input",1);result.addProperty("bucket_output",0);result.addProperty("source","NONE");BABIES.add(result);
         }else{
            require(!cow.isBaby() && s.source()==MilkingState.VANILLA && cow.getUUID().equals(s.animalId()) && cow.getId()==s.entityId(),"Wrong adult source");
            require(s.remainingTicks()==0 && s.totalTicks()==0,"Invented vanilla animal cooldown");
            if(firstTick<0){
               if(machine.getItem(1).isEmpty()){require(amount==0 && machine.getItem(0).is(Items.BUCKET),"Unexpected pre-bucket production");return;}
               require(machine.getItem(1).is(Items.MILK_BUCKET) && machine.getItem(1).getCount()==1 && machine.getItem(0).isEmpty() && amount==0,"First product is not one milk bucket");
               firstTick=tick;machine.setItem(1,ItemStack.EMPTY);return;
            }
            require(machine.getItem(0).isEmpty() && machine.getItem(1).isEmpty(),"Unexpected bucket replay");
            if(tick-firstTick<120){require(amount==0,"Tank produced before 120 machine ticks");return;}
            require(tick-firstTick==120 && amount==1000,"Expected 1000 mB exactly 120 machine ticks after bucket");
            require(machine.fluidHandler(Direction.UP).getFluidInTank(0).getFluid()==NeoForgeMod.MILK.get(),"Wrong tank fluid");
            var result=new JsonObject();result.addProperty("variant",variant());result.addProperty("bucket",true);result.addProperty("tank_amount",amount);
            result.addProperty("cadence_ticks",120);result.addProperty("first_tick",firstTick);result.addProperty("second_tick",tick);result.addProperty("no_early_repeat",true);PRODUCTION.add(result);
         }
         level.setBlock(POS.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);monitoring=false;scenarioDone=true;
      }catch(Throwable error){monitoring=false;serverError=error;}
   }
   @SubscribeEvent public static void tick(ClientTickEvent.Post event){
      if(!Boolean.getBoolean("iedctr.mooshroomMilkingNative") || complete)return;
      Minecraft mc=Minecraft.getInstance();
      try{
         if(deadline==0)deadline=System.nanoTime()+900_000_000_000L;
         require(System.nanoTime()<deadline && ++ticks<6000,"Native Mooshroom timeout at "+stage);
         if(serverError!=null)throw new IllegalStateException("Server fixture failed",serverError);
         if(stage==Stage.BOOT){if(mc.screen instanceof TitleScreen && mc.getOverlay()==null)boot(mc);return;}
         if(stage==Stage.WORLD){
            if(mc.level==null || mc.player==null || mc.gameMode==null || mc.screen!=null)return;
            require(mc.getSingleplayerServer()!=null && mc.player.connection.getConnection().isMemoryConnection(),"Only isolated integrated worlds permitted");
            playerId=mc.player.getUUID();prepare(mc);return;
         }
         require(mc.level!=null && mc.player!=null && mc.getSingleplayerServer()!=null,"World disconnected");
         if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
         if(!(mc.screen instanceof MachineScreen.SmallMilkingMachineScreen screen) || !(mc.player.containerMenu instanceof MachineMenu menu) || menu.containerId!=serverMenuId)return;
         if(stage==Stage.RUN){if(!scenarioDone)return;enter(Stage.PORTRAIT);}
         cursor(mc,screen,stage==Stage.TOOLTIP);
         boolean settledInput=stage==Stage.TOOLTIP?actualIdentityHover(mc,screen):actualOutsideIdentity(mc,screen);
         hoverTicks=settledInput?hoverTicks+1:0;
         require(ticks<120 || hoverTicks>=8,"Native capture input did not settle at "+stage+": "+inputDiagnostic(mc));
         if(hoverTicks<8)return;
         if(ticks<12)return;
         JsonObject state=observe(mc,menu);if(state==null)return;
         String key=state.get("client_uuid").getAsString()+":"+number(state,"amount");
         if(!key.equals(stableKey)){stableKey=key;stableObservations=0;}
         if(++stableObservations<8)return;
         if(stage==Stage.PORTRAIT){
            capture(mc,menu,state,baby()?"baby-"+variant().toLowerCase(Locale.ROOT):variant().toLowerCase(Locale.ROOT)+"-portrait",false);
            if(!baby()){enter(Stage.TOOLTIP);return;}
            next(mc);
         }else if(stage==Stage.TOOLTIP && hoverTicks>=8){
            capture(mc,menu,state,variant().toLowerCase(Locale.ROOT)+"-identity-tooltip",true);next(mc);
         }
      }catch(Throwable error){finish(mc,error);}
   }
   private static void next(Minecraft mc){if(++scenario==4){finish(mc,null);return;}prepare(mc);}
   private static void prepare(Minecraft mc){
      scenarioDone=false;monitoring=false;firstTick=-1;
      pending=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);p.closeContainer();p.setGameMode(GameType.SURVIVAL);var level=p.serverLevel();
         if(cow!=null)cow.discard();
         if(machine!=null){machine.setItem(0,ItemStack.EMPTY);machine.setItem(1,ItemStack.EMPTY);}
         for(int x=-1;x<=1;x++)for(int z=-2;z<=4;z++)level.setBlock(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState(),3);
         p.connection.teleport(.5,81,3.5,180,0);
         level.setBlock(POS,Blocks.AIR.defaultBlockState(),3);
         level.setBlock(POS.east(),Blocks.AIR.defaultBlockState(),3);
         level.setBlock(POS,ModBlocks.SMALL_MILKING_MACHINE.get().defaultBlockState().setValue(MachineBlocks.HORIZONTAL_FACING,Direction.NORTH),3);
         require(level.getBlockEntity(POS) instanceof MachineBlockEntity,"Missing machine");machine=(MachineBlockEntity)level.getBlockEntity(POS);
         machine.setItem(0,new ItemStack(Items.BUCKET));
         cow=EntityType.MOOSHROOM.create(level);require(cow!=null,"Missing vanilla Mooshroom");
         cow.setVariant(scenario%2==0?MushroomCow.MushroomType.RED:MushroomCow.MushroomType.BROWN);
         cow.setPos(.5,80,-.5);cow.setNoAi(true);cow.setNoGravity(true);cow.setAge(baby()?-24000:0);
         cow.setCustomName(Component.literal("Native "+variant()+" Mooshroom"));cow.setPersistenceRequired();
         require(level.addFreshEntity(cow),"Mooshroom spawn failed");
         p.openMenu(machine);require(p.containerMenu instanceof MachineMenu,"Wrong menu");serverMenuId=p.containerMenu.containerId;
         startedTick=level.getGameTime();monitoring=true;
      });enter(Stage.RUN);
   }
   private static void boot(Minecraft mc) throws Exception {
      require(!Boolean.getBoolean("iedctr.tfcMilkingRecoveryNative") && !Boolean.getBoolean("iedctr.milkingQualificationNative") && !Boolean.getBoolean("iedctr.clientGameplay") && !Boolean.getBoolean("iedctr.barrelReferenceNative") && !Boolean.getBoolean("iedctr.solarV2ReferenceNative"),"Dedicated fixture only");
      nonce=System.getProperty("iedctr.clientGameplayNonce","");
      require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha","").matches("[a-f0-9]{40}"),"Missing nonce or exact revision");
      directory=mc.gameDirectory.toPath().toRealPath();
      require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory","")).toRealPath()) && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce),"Isolation marker mismatch");
      require(mc.level==null && mc.getSingleplayerServer()==null && !Files.exists(directory.resolve("mooshroom-milking-screenshots"))
         && !Files.exists(directory.resolve("client-mooshroom-milking-result.json")),"Refusing existing world/captures/result");
      if(Files.exists(directory.resolve("saves"))) try(var entries=Files.list(directory.resolve("saves"))) { require(entries.findAny().isEmpty(),"Existing saves forbidden"); }
      isolatedDirectoryValidated=true;
      tfcVersion=net.neoforged.fml.ModList.get().getModContainerById("tfc").orElseThrow().getModInfo().getVersion().toString();
      require(tfcVersion.equals("4.2.12"),"Wrong native TFC runtime version");
      verifyViewport(mc);
      verifyArt(mc,"background","871f7c4512227153a4c49591c5facdbc95b1443114cc3206659d6dd524d1326b");
      verifyArt(mc,"milk","f6f0507deb081a2a83b6bb455abc6237ac2b41f4ab21d43c833e356fb2d9bbe5");
      String name="iedctr-mooshroom-milking-"+nonce;
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

   private static JsonObject observe(Minecraft mc,MachineMenu menu){
      if(inspection==null){inspection=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);require(p.containerMenu.containerId==serverMenuId && p.serverLevel().getBlockEntity(POS)==machine,"Server identity changed");
         var s=((MachineMenu)p.containerMenu).milkingState();
         require(s.source()==(baby()?MilkingState.NONE:MilkingState.VANILLA),"Wrong projected source");
         var o=new JsonObject();o.addProperty("server_uuid",cow.getUUID().toString());o.addProperty("server_entity_id",cow.getId());o.addProperty("server_name",cow.getDisplayName().getString());
         o.addProperty("source",BuiltInRegistries.ENTITY_TYPE.getKey(cow.getType()).toString());o.addProperty("variant",cow.getVariant().name());
         o.addProperty("server_source",baby()?"NONE":"VANILLA");o.addProperty("baby_rejected",baby());
         o.addProperty("server_selected_entity_id",s.entityId());o.addProperty("server_selected_uuid",s.animalId()==null?null:s.animalId().toString());
         o.addProperty("amount",machine.fluidHandler(Direction.UP).getFluidInTank(0).getAmount());o.addProperty("remaining_ticks",s.remainingTicks());o.addProperty("total_ticks",s.totalTicks());return o;
      });return null;}
      if(!inspection.isDone())return null;
      var o=inspection.join();inspection=null;var s=menu.milkingState();
      var entity=mc.level.getEntity(o.get("server_entity_id").getAsInt());
      if(!(entity instanceof MushroomCow mushroom) || !entity.getUUID().toString().equals(o.get("server_uuid").getAsString()) || menu.fluidAmount()!=number(o,"amount"))return null;
      if(s.source()!=(baby()?MilkingState.NONE:MilkingState.VANILLA))return null;
      require(mushroom.getVariant().name().equals(variant()) && mushroom.isBaby()==baby(),"Client variant/age mismatch");
      require(baby()?s.animalId()==null && s.entityId()==0:entity.getUUID().equals(s.animalId()) && entity.getId()==s.entityId(),"Client selected identity mismatch");
      require(s.remainingTicks()==0 && s.totalTicks()==0,"Client invented vanilla cooldown");
      o.addProperty("client_source",baby()?"NONE":"VANILLA");o.addProperty("client_uuid",entity.getUUID().toString());o.addProperty("client_entity_id",entity.getId());o.addProperty("client_name",entity.getDisplayName().getString());
      o.addProperty("client_selected_entity_id",s.entityId());o.addProperty("client_selected_uuid",s.animalId()==null?null:s.animalId().toString());
      o.addProperty("client_amount",menu.fluidAmount());o.addProperty("client_remaining_ticks",s.remainingTicks());o.addProperty("client_total_ticks",s.totalTicks());o.addProperty("client_state_code",s.state());return o;
   }
   private static void cursor(Minecraft mc,MachineScreen screen,boolean tooltip){
      long window=mc.getWindow().getWindow();
      if(GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED)!=GLFW.GLFW_TRUE){
         GLFW.glfwFocusWindow(window);
         return; // Focus and cursor callbacks are consumed by the normal render loop.
      }
      // Alternate within the same hitbox so an earlier ignored warp is retried
      // as a real native movement rather than a no-op to an unchanged position.
      // Stay well inside the native client area: edge warps under Xvfb can
      // move GLFW's cursor while leaving Minecraft's last delivered position stale.
      double x=screen.getGuiLeft()+(tooltip?50:-40)+(ticks%2);
      double y=screen.getGuiTop()+45;
      GLFW.glfwSetCursorPos(window,x*mc.getWindow().getScreenWidth()/screen.width,y*mc.getWindow().getScreenHeight()/screen.height);
   }
   private static boolean actualIdentityHover(Minecraft mc,MachineScreen screen){
      double x=mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth();
      double y=mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight();
      return GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_FOCUSED)==GLFW.GLFW_TRUE
         && GLFW.glfwGetInputMode(mc.getWindow().getWindow(),GLFW.GLFW_CURSOR)==GLFW.GLFW_CURSOR_NORMAL
         && x>=screen.getGuiLeft()+27 && x<screen.getGuiLeft()+74 && y>=screen.getGuiTop()+34 && y<screen.getGuiTop()+75;
   }
   private static boolean actualOutsideIdentity(Minecraft mc,MachineScreen screen){
      double x=mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth();
      double y=mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight();
      return GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_FOCUSED)==GLFW.GLFW_TRUE
         && GLFW.glfwGetInputMode(mc.getWindow().getWindow(),GLFW.GLFW_CURSOR)==GLFW.GLFW_CURSOR_NORMAL
         && x>=screen.getGuiLeft()-40 && x<screen.getGuiLeft()-38
         && y>=screen.getGuiTop()+45 && y<screen.getGuiTop()+46;
   }
   private static JsonObject inputDiagnostic(Minecraft mc){
      long window=mc.getWindow().getWindow();double[] x={0},y={0};GLFW.glfwGetCursorPos(window,x,y);
      var o=new JsonObject();o.addProperty("glfw_focused",GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED)==GLFW.GLFW_TRUE);
      o.addProperty("glfw_visible",GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_VISIBLE)==GLFW.GLFW_TRUE);
      o.addProperty("glfw_cursor_mode",GLFW.glfwGetInputMode(window,GLFW.GLFW_CURSOR));
      o.addProperty("glfw_cursor_x",x[0]);o.addProperty("glfw_cursor_y",y[0]);
      o.addProperty("minecraft_cursor_x",mc.mouseHandler.xpos());o.addProperty("minecraft_cursor_y",mc.mouseHandler.ypos());
      o.addProperty("window_width",mc.getWindow().getScreenWidth());o.addProperty("window_height",mc.getWindow().getScreenHeight());
      if(mc.screen instanceof MachineScreen screen){
         o.addProperty("target_gui_x_min",screen.getGuiLeft()+(stage==Stage.TOOLTIP?50:-40));
         o.addProperty("target_gui_x_max_exclusive",screen.getGuiLeft()+(stage==Stage.TOOLTIP?52:-38));
         o.addProperty("target_gui_y",screen.getGuiTop()+45);
      }
      o.addProperty("settled_hover_ticks",hoverTicks);return o;
   }
   private static void capture(Minecraft mc,MachineMenu menu,JsonObject state,String phase,boolean tooltip)throws Exception{
      verifyViewport(mc);require(mc.getOverlay()==null,"Loading overlay");var screen=(MachineScreen)mc.screen;
      require(screen.getXSize()==228 && screen.getYSize()==228 && menu.slots.size()==38,"Wrong canvas/menu");
      var o=state.deepCopy();addViewport(o,mc);o.addProperty("phase",phase);o.addProperty("name",phase+".png");
      o.addProperty("menu_id",menu.containerId);o.addProperty("server_menu_id",serverMenuId);
      o.addProperty("screen_class",screen.getClass().getName());o.addProperty("tooltip_hovered",actualIdentityHover(mc,screen));
      require(tooltip==actualIdentityHover(mc,screen),"Actual hover differs from capture phase");
      int hoverX=(int)(mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth());
      int hoverY=(int)(mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight());
      require(hoverTicks>=8 && (tooltip?actualIdentityHover(mc,screen):actualOutsideIdentity(mc,screen)),
         "Actual cursor is not settled at capture target: "+inputDiagnostic(mc));
      o.add("native_input",inputDiagnostic(mc));
      o.addProperty("hover_x",hoverX);o.addProperty("hover_y",hoverY);o.addProperty("settled_observations",stableObservations);
      require(menu.getCarried().isEmpty(),"Unexpected carried stack");
      int scale=(int)mc.getWindow().getGuiScale(),x=(screen.getGuiLeft()+27)*scale,y=(screen.getGuiTop()+34)*scale,w=47*scale,h=41*scale;
      Path folder=directory.resolve("mooshroom-milking-screenshots");Files.createDirectories(folder);
      Path file=folder.resolve(phase+".png"),portrait=folder.resolve(phase+"-portrait.png");require(!Files.exists(file) && !Files.exists(portrait),"Stale capture");
      try(var frame=Screenshot.takeScreenshot(mc.getMainRenderTarget());var crop=new NativeImage(w,h,false)){
         frame.writeToFile(file);for(int iy=0;iy<h;iy++)for(int ix=0;ix<w;ix++)crop.setPixelRGBA(ix,iy,frame.getPixelRGBA(x+ix,y+iy));crop.writeToFile(portrait);
      }
      o.addProperty("sha256",hash(file));o.addProperty("portrait_name",phase+"-portrait.png");o.addProperty("portrait_sha256",hash(portrait));
      o.addProperty("portrait_x",x);o.addProperty("portrait_y",y);o.addProperty("portrait_width",w);o.addProperty("portrait_height",h);CAPTURES.add(o);
   }
   private static long number(JsonObject o,String key){return o.get(key).getAsLong();}
   private static String hash(Path file)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
   private static void enter(Stage next){stage=next;ticks=0;inspection=null;stableObservations=0;stableKey="";hoverTicks=0;}
   private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);require(p!=null,"Player vanished");return p;}
   private static void finish(Minecraft mc,Throwable error){
      complete=true;monitoring=false;
      if(!isolatedDirectoryValidated){EngineersDecorReforged.LOGGER.error("IEDCTR_MOOSHROOM_MILKING rejected before isolation validation",error);return;}
      try{
         var p=new JsonObject();p.addProperty("suite",SUITE);p.addProperty("status",error==null?"passed_bounded_smoke":"failed");
         p.addProperty("nonce",nonce);p.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha"));
         p.addProperty("requested_language",System.getProperty("iedctr.requestedLanguage",""));p.addProperty("selected_language",mc.getLanguageManager().getSelected());
         p.addProperty("source_background_sha256","871f7c4512227153a4c49591c5facdbc95b1443114cc3206659d6dd524d1326b");
         p.addProperty("source_milk_sha256","f6f0507deb081a2a83b6bb455abc6237ac2b41f4ab21d43c833e356fb2d9bbe5");
         addViewport(p,mc);p.addProperty("actual_integrated_world",mc.getSingleplayerServer()!=null);p.addProperty("native_visual_acceptance",false);
         p.addProperty("tfc_version",tfcVersion);p.addProperty("native_tick_production",true);
         p.addProperty("screen_class",MachineScreen.SmallMilkingMachineScreen.class.getName());p.add("captures",CAPTURES);p.add("production_checks",PRODUCTION);p.add("baby_checks",BABIES);p.add("last_native_input",inputDiagnostic(mc));
         if(error!=null)p.addProperty("error",error.toString());
         Files.writeString(directory.resolve("client-mooshroom-milking-result.json"),new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(p)+"\n");
      }catch(Exception writeError){EngineersDecorReforged.LOGGER.error("Mooshroom proof write failed",writeError);}
      if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_MOOSHROOM_MILKING PASSED nonce={}",nonce);
      else EngineersDecorReforged.LOGGER.error("IEDCTR_MOOSHROOM_MILKING FAILED at "+stage,error);
      mc.stop();
   }
   private static void require(boolean b,String message){if(!b)throw new IllegalStateException(message);}
}
