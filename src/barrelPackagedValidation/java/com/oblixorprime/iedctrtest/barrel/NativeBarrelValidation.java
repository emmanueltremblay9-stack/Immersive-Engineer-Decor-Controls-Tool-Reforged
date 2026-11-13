package com.oblixorprime.iedctrtest.barrel;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.oblixorprime.engineersdecorreforged.client.MachineScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.minecraft.core.Direction;

/** Local-only independent Barrel harness. All seeds are explicit fixture setup, not gameplay evidence. */
@EventBusSubscriber(modid = BarrelValidationMod.MOD_ID, value = Dist.CLIENT)
public final class NativeBarrelValidation {
   private enum Stage { BOOT, WORLD, OPEN, OPEN_RELEASE, OPEN_ACK, SEED, CAPTURE, MOVE, PRESS, RELEASE, ACK, DONE }
   private static Stage stage=Stage.BOOT;
   private static final BlockPos POS=new BlockPos(0,80,0);
   private static final String[] NAMES={"empty","half","full","full-hover","inventory-pickup","inventory-return"};
   private static int ticks, menuId, captureIndex, targetX, targetY, moveCount, buttonEvents;
   private static long deadline, outerFrameId, renderId, screenId;
   private static Path directory;
   private static String nonce, selectedFluid;
   private static Fluid fixtureFluid;
   private static UUID playerId;
   private static MachineBlockEntity machine;
   private static CompletableFuture<?> pending;
   private static CompletableFuture<JsonObject> inspection;
   private static JsonObject serverSnapshot, packaged, opening, tooltipGeometry, pendingGeometry;
   private static final JsonArray CAPTURES=new JsonArray(), GESTURES=new JsonArray();
   private static NativeLibrary x11, xtest;
   private static Pointer display;
   private static boolean isolated, complete, rightPressed, rightObserved, pressed, observedPress, captureReady, outerFrameOpen;
   private static net.minecraft.client.gui.screens.Screen observedScreen, renderedScreen;
   private static MachineScreen pendingScreen;
   private static MachineMenu pendingMenu;
   private static int renderedPointerX,renderedPointerY, tooltipEvents;
   private static void clearFrame(){pendingGeometry=null;pendingScreen=null;pendingMenu=null;tooltipGeometry=null;renderedScreen=null;}
   @SubscribeEvent(priority=EventPriority.HIGHEST)
   public static void beforeFrame(RenderFrameEvent.Pre event){
      if(!isolated||complete)return;
      outerFrameId++;outerFrameOpen=true;clearFrame();
   }
   @SubscribeEvent public static void beforeRender(ScreenEvent.Render.Pre event){
      if(!isolated||complete)return;
      clearFrame();renderId++;tooltipEvents=0;renderedScreen=event.getScreen();
      renderedPointerX=event.getMouseX();renderedPointerY=event.getMouseY();
      if(observedScreen!=renderedScreen){observedScreen=renderedScreen;screenId++;}
   }
   @SubscribeEvent public static void tooltip(RenderTooltipEvent.Color event){
      Minecraft mc=Minecraft.getInstance();
      if(!isolated||complete||!outerFrameOpen||renderedScreen!=mc.screen||!(mc.screen instanceof MachineScreen screen)
         ||screen.getMenu().containerId!=menuId||!atTarget(mc,screen))return;
      tooltipEvents++;
      int width=0,height=event.getComponents().size()==1?-2:0;
      for(var component:event.getComponents()){width=Math.max(width,component.getWidth(event.getFont()));height+=component.getHeight();}
      var g=new JsonObject();g.addProperty("text_x",event.getX());g.addProperty("text_y",event.getY());
      g.addProperty("text_width",width);g.addProperty("text_height",height);
      g.addProperty("frame_left",event.getX()-4);g.addProperty("frame_top",event.getY()-4);
      g.addProperty("frame_right",event.getX()+width+4);g.addProperty("frame_bottom",event.getY()+height+4);tooltipGeometry=g;
   }
   @SubscribeEvent public static void afterRender(ScreenEvent.Render.Post event){
      Minecraft mc=Minecraft.getInstance();
      try {
         if(!isolated||complete||!outerFrameOpen||!captureReady||stage!=Stage.CAPTURE
            ||event.getScreen()!=renderedScreen||event.getScreen()!=mc.screen||!(mc.screen instanceof MachineScreen screen)
            ||screen.getMenu().containerId!=menuId||!atTarget(mc,screen)
            ||event.getMouseX()!=renderedPointerX||event.getMouseY()!=renderedPointerY)return;
         boolean hover=captureIndex==3;
         if(hover?(tooltipEvents!=1||tooltipGeometry==null):tooltipEvents!=0)return;
         var g=new JsonObject();g.addProperty("outer_frame_id",outerFrameId);g.addProperty("render_id",renderId);
         g.addProperty("screen_id",screenId);g.addProperty("menu_id",menuId);g.addProperty("state_name",NAMES[captureIndex]);
         g.addProperty("pointer_x",mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth());
         g.addProperty("pointer_y",mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight());
         require(renderedPointerX==(int)g.get("pointer_x").getAsDouble()&&renderedPointerY==(int)g.get("pointer_y").getAsDouble(),"Pointer/render mismatch");
         g.addProperty("gui_width",screen.width);g.addProperty("gui_height",screen.height);
         g.addProperty("gui_left",screen.getGuiLeft());g.addProperty("gui_top",screen.getGuiTop());
         var layout=screen.getMenu().layout();
         require(layout.imageWidth()==201&&layout.imageHeight()==226
            &&screen.getGuiLeft()==(screen.width-layout.imageWidth())/2
            &&screen.getGuiTop()==(screen.height-layout.imageHeight())/2,"Live Barrel layout/origin differs");
         g.addProperty("image_width",layout.imageWidth());g.addProperty("image_height",layout.imageHeight());
         g.addProperty("tank_x",screen.getGuiLeft()+92);g.addProperty("tank_y",screen.getGuiTop()+35);
         g.addProperty("tank_width",25);g.addProperty("tank_height",74);
         require(screen.getGuiLeft()>=0&&screen.getGuiTop()>=0&&screen.getGuiLeft()+201<=screen.width&&screen.getGuiTop()+226<=screen.height,"Barrel frame clipped");
         if(hover){var t=tooltipGeometry;
            require(t.get("text_width").getAsInt()>0&&t.get("text_height").getAsInt()>0
               &&t.get("frame_left").getAsInt()>=0&&t.get("frame_top").getAsInt()>=0
               &&t.get("frame_right").getAsInt()<=screen.width&&t.get("frame_bottom").getAsInt()<=screen.height,"Tooltip clipped");
            g.add("tooltip",t.deepCopy());
         }
         pendingGeometry=g;pendingScreen=screen;pendingMenu=screen.getMenu();
      }catch(Throwable e){finish(mc,e);}
   }
   // Runs after GameRenderer and JEI Screen.Post. Does not claim later profiler/mod post-frame draws.
   @SubscribeEvent(priority=EventPriority.LOWEST)
   public static void afterFrame(RenderFrameEvent.Post event){
      Minecraft mc=Minecraft.getInstance();
      try {
         if(pendingGeometry==null)return;
         var g=pendingGeometry;var screen=pendingScreen;
         require(!complete&&isolated&&outerFrameOpen&&captureReady&&stage==Stage.CAPTURE
            &&g.get("outer_frame_id").getAsLong()==outerFrameId&&g.get("render_id").getAsLong()==renderId
            &&g.get("screen_id").getAsLong()==screenId&&g.get("state_name").getAsString().equals(NAMES[captureIndex]),"Stale capture frame/state");
         require(mc.screen==screen&&observedScreen==screen&&screen.getMenu()==pendingMenu
            &&mc.player!=null&&mc.player.containerMenu==pendingMenu&&pendingMenu.containerId==menuId
            &&pendingMenu.kind()==MachineKind.FLUID_BARREL,"Capture identity changed");
         require(atTarget(mc,screen)&&g.get("pointer_x").getAsDouble()==mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth()
            &&g.get("pointer_y").getAsDouble()==mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight()
            &&g.get("gui_width").getAsInt()==screen.width&&g.get("gui_height").getAsInt()==screen.height,"Capture pointer/viewport changed");
         require(mc.getOverlay()==null&&!recipeToastVisible(mc)&&!mc.getDebugOverlay().showProfilerChart(),"Capture overlay");
         require(expected(snapshot(pendingMenu))&&expected(serverSnapshot),"Capture state changed");
         var row=capture(mc,screen);row.add("geometry",g.deepCopy());row.add("client",snapshot(pendingMenu));row.add("server",serverSnapshot.deepCopy());
         row.addProperty("profiler_chart_disabled",true);
         row.addProperty("capture_phase","RenderFrameEvent.Post:LOWEST");row.addProperty("capture_outer_frame_id",outerFrameId);
         row.addProperty("capture_render_id",renderId);row.addProperty("capture_screen_id",screenId);
         row.addProperty("setup_seed",true);row.addProperty("state_name",NAMES[captureIndex]);CAPTURES.add(row);
         if(captureIndex==4||captureIndex==5){var gesture=new JsonObject();gesture.addProperty("action",NAMES[captureIndex]);
            gesture.addProperty("menu_slot",2);gesture.addProperty("native_press_observed",observedPress);
            gesture.addProperty("native_release_observed",!pressed&&GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_RELEASE);
            gesture.addProperty("button_events",buttonEvents);gesture.add("client",snapshot(pendingMenu));gesture.add("server",serverSnapshot.deepCopy());GESTURES.add(gesture);}
         captureIndex++;
         if(captureIndex==6){finish(mc,null);return;}
         if(captureIndex<3){seedTank(mc);enter(Stage.SEED);}
         else if(captureIndex==3){setCaptureTarget(screen);enter(Stage.CAPTURE);}
         else startGesture(screen);
      }catch(Throwable e){finish(mc,e);}finally{clearFrame();outerFrameOpen=false;}
   }
   @SubscribeEvent public static void tick(ClientTickEvent.Post event){
      if(!Boolean.getBoolean("iedctr.barrelPackagedNative")||complete)return;
      Minecraft mc=Minecraft.getInstance();
      try {
         if(deadline==0)deadline=System.nanoTime()+600_000_000_000L;
         require(System.nanoTime()<deadline&&++ticks<1200,"Timeout at "+stage+" capture "+captureIndex);
         if(stage==Stage.BOOT){if(mc.screen instanceof TitleScreen&&mc.getOverlay()==null)boot(mc);return;}
         if(stage==Stage.WORLD){if(mc.player==null||mc.level==null||mc.screen!=null)return;
            require(mc.getSingleplayerServer()!=null&&mc.player.connection.getConnection().isMemoryConnection(),"Integrated world required");
            playerId=mc.player.getUUID();setupAsync(mc);return;}
         require(mc.getSingleplayerServer()!=null&&mc.player!=null,"World disconnected");
         if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
         if(mc.getOverlay()!=null)return;
         if(stage==Stage.OPEN){
            if(mc.screen!=null||ticks<20||!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)||!hit.getBlockPos().equals(POS))return;
            require(mc.player.getMainHandItem().isEmpty()&&mc.player.getEyePosition().distanceTo(hit.getLocation())<4,"Empty-hand survival reach required");
            if(GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_FOCUSED)!=GLFW.GLFW_TRUE){GLFW.glfwFocusWindow(mc.getWindow().getWindow());return;}
            rightButton(true);enter(Stage.OPEN_RELEASE);return;
         }
         if(stage==Stage.OPEN_RELEASE){if(ticks<3||GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_RIGHT)!=GLFW.GLFW_PRESS)return;
            rightObserved=true;rightButton(false);enter(Stage.OPEN_ACK);return;}
         if(stage==Stage.OPEN_ACK){
            if(ticks<4||!(mc.screen instanceof MachineScreen.FluidBarrelScreen)||!(mc.player.containerMenu instanceof MachineMenu opened)
               ||GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_RIGHT)!=GLFW.GLFW_RELEASE)return;
            require(opened.kind()==MachineKind.FLUID_BARREL&&opened.fluidCapacity()==32000&&opened.visibleMachineSlots()==2&&opened.slots.size()==38,"Wrong Barrel menu");
            menuId=opened.containerId;opening=new JsonObject();opening.addProperty("method","XTEST_RMB_BLOCK_HIT");
            opening.addProperty("native_right_press_observed",rightObserved);opening.addProperty("native_right_release_observed",!rightPressed);
            opening.addProperty("menu_id",menuId);opening.addProperty("hit_block","0,80,0");
            captureIndex=0;seedTank(mc);enter(Stage.SEED);return;
         }
         if(!(mc.screen instanceof MachineScreen.FluidBarrelScreen screen)||!(mc.player.containerMenu instanceof MachineMenu menu)||menu.containerId!=menuId)return;
         switch(stage){
            case SEED -> {if(ticks>=20&&agrees(mc,menu)){setCaptureTarget(screen);enter(Stage.CAPTURE);}}
            case CAPTURE -> {captureReady=false;if(!atTarget(mc,screen)){move(mc,screen);return;}
               if(ticks>=120&&!recipeToastVisible(mc)&&agrees(mc,menu))captureReady=true;}
            case MOVE -> {if(!atTarget(mc,screen)){move(mc,screen);return;}if(ticks>=8)enter(Stage.PRESS);}
            case PRESS -> {if(ticks<3)return;
               require(atTarget(mc,screen)&&targetX==screen.getGuiLeft()+menu.getSlot(2).x+8&&targetY==screen.getGuiTop()+menu.getSlot(2).y+8,"Gesture target changed");
               button(true);enter(Stage.RELEASE);}
            case RELEASE -> {if(ticks<3||GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_LEFT)!=GLFW.GLFW_PRESS)return;
               observedPress=true;button(false);enter(Stage.ACK);}
            case ACK -> {if(ticks<8||GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_LEFT)!=GLFW.GLFW_RELEASE||!agrees(mc,menu))return;
               setCaptureTarget(screen);enter(Stage.CAPTURE);}
            default -> {}
         }
      }catch(Throwable e){finish(mc,e);}
   }
   private static void boot(Minecraft mc) throws Exception {
      nonce=System.getProperty("iedctr.clientGameplayNonce","");
      require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha","").matches("[a-f0-9]{40}"),"Missing identity");
      require(!Boolean.getBoolean("iedctr.clientGameplay"),"Dedicated runner required");
      directory=mc.gameDirectory.toPath().toRealPath();
      require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory","")).toRealPath())
         && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce),"Isolation mismatch");
      String nativeDisplay=System.getenv("DISPLAY");
      require(nativeDisplay!=null && nativeDisplay.matches(":\\d+(\\.\\d+)?")
         && Files.readString(directory.resolve(".iedctr-barrel-xvfb")).equals(nonce+"\n"+nativeDisplay),"Runner-owned display/nonce mismatch");
      require(mc.level==null && mc.getSingleplayerServer()==null && !Files.exists(directory.resolve("barrel-packaged-screenshots")),"Existing world/evidence forbidden");
      if(Files.exists(directory.resolve("saves"))) try(var entries=Files.list(directory.resolve("saves"))) {require(entries.findAny().isEmpty(),"Existing saves forbidden");}
      int requestedScale=Integer.getInteger("iedctr.requestedGuiScale",0);
      require(requestedScale==1 && mc.getWindow().getGuiScale()==requestedScale,"Requested GUI scale was clamped or changed");
      require(mc.getWindow().getGuiScaledWidth()>=320 && mc.getWindow().getGuiScaledHeight()>=256,"Machine frame does not fit logical viewport");
      require(Integer.getInteger("iedctr.requestedWidth",0)==mc.getWindow().getWidth()
         && Integer.getInteger("iedctr.requestedHeight",0)==mc.getWindow().getHeight()
         && mc.getWindow().getWidth()==mc.getWindow().getScreenWidth()
         && mc.getWindow().getHeight()==mc.getWindow().getScreenHeight(),"Requested framebuffer/window mismatch");
      require(!mc.getDebugOverlay().showProfilerChart(),"Debug profiler must be disabled for renderer-end capture");
      selectedFluid=System.getProperty("iedctr.barrelFixtureFluid", "");
      fixtureFluid=switch(selectedFluid) {case "water" -> Fluids.WATER; case "lava" -> Fluids.LAVA; case "milk" -> NeoForgeMod.MILK.get(); default -> throw new IllegalStateException("Select water, lava or milk");};
      require(fixtureFluid!=Fluids.EMPTY,"Missing selected fluid");
      packaged=PackagedRuntimeProof.verify(mc);
      isolated=true; mc.options.pauseOnLostFocus=false;
      x11=NativeLibrary.getInstance("X11");xtest=NativeLibrary.getInstance("Xtst");
      display=x11.getFunction("XOpenDisplay").invokePointer(new Object[]{null}); require(display!=null,"X11 unavailable");
      String name="iedctr-barrel-packaged-"+nonce;enter(Stage.WORLD);
      mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
         new WorldOptions(0L,false,false),registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
   }
   private static ServerPlayer player(Minecraft mc) {return mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);}

   private static void setupAsync(Minecraft mc){
      pending=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);p.closeContainer();p.setGameMode(GameType.SURVIVAL);var level=p.serverLevel();
         level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,mc.getSingleplayerServer());level.setWeatherParameters(1000000,0,false,false);
         for(int x=-3;x<=3;x++)for(int y=79;y<=83;y++)for(int z=-3;z<=3;z++)level.setBlock(new BlockPos(x,y,z),y==79?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
         p.connection.teleport(.5,80,2.5,180,29.25f);level.setBlock(POS,ModBlocks.FLUID_BARREL.get().defaultBlockState(),3);
         machine=(MachineBlockEntity)level.getBlockEntity(POS);require(machine!=null,"Barrel missing");machine.clearContent();
         machine.fluidHandler(Direction.UP).drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
         p.getInventory().clearContent();p.getInventory().setItem(9,new ItemStack(Items.BUCKET));
      });enter(Stage.OPEN);
   }
   private static int amount(){return captureIndex==0?0:captureIndex==1?16000:32000;}
   private static void seedTank(Minecraft mc){
      int amount=amount();pending=mc.getSingleplayerServer().submit(()->{
         machine.fluidHandler(Direction.UP).drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
         if(amount>0)require(machine.fluidHandler(Direction.UP).fill(new FluidStack(fixtureFluid,amount),FluidAction.EXECUTE)==amount,"Barrel fixture fill rejected");
         player(mc).containerMenu.broadcastChanges();
      });
   }
   private static void setCaptureTarget(MachineScreen screen){
      targetX=screen.getGuiLeft()+(captureIndex==3?105:8);targetY=screen.getGuiTop()+(captureIndex==3?65:8);move(Minecraft.getInstance(),screen);
   }
   private static void startGesture(MachineScreen screen){
      observedPress=false;targetX=screen.getGuiLeft()+screen.getMenu().getSlot(2).x+8;targetY=screen.getGuiTop()+screen.getMenu().getSlot(2).y+8;
      move(Minecraft.getInstance(),screen);enter(Stage.MOVE);
   }
   private static void move(Minecraft mc,MachineScreen screen) {
      require(isolated && display!=null,"Unsafe native input");long window=mc.getWindow().getWindow();
      if(GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED)!=GLFW.GLFW_TRUE){GLFW.glfwFocusWindow(window);return;}
      int[] ox={0},oy={0};GLFW.glfwGetWindowPos(window,ox,oy);
      int x=ox[0]+(int)Math.round(targetX*mc.getWindow().getScreenWidth()/(double)screen.width);
      int y=oy[0]+(int)Math.round(targetY*mc.getWindow().getScreenHeight()/(double)screen.height);
      require(x>=0&&y>=0,"Native target outside display");
      require(xtest.getFunction("XTestFakeMotionEvent").invokeInt(new Object[]{display,-1,x,y,new NativeLong(0)})!=0,"XTest motion rejected");flush();moveCount++;
   }
   private static void flush(){x11.getFunction("XFlush").invokeInt(new Object[]{display});}
   private static void rightButton(boolean down){require(isolated && display!=null,"Unsafe native RMB");require(xtest.getFunction("XTestFakeButtonEvent").invokeInt(new Object[]{display,3,down?1:0,new NativeLong(0)})!=0,"XTest RMB rejected");rightPressed=down;flush();}
   private static void button(boolean down){require(xtest.getFunction("XTestFakeButtonEvent").invokeInt(new Object[]{display,1,down?1:0,new NativeLong(0)})!=0,"XTest button rejected");pressed=down;buttonEvents++;flush();}
   private static boolean atTarget(Minecraft mc,MachineScreen s){return GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_FOCUSED)==GLFW.GLFW_TRUE
      && GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_VISIBLE)==GLFW.GLFW_TRUE
      && GLFW.glfwGetInputMode(mc.getWindow().getWindow(),GLFW.GLFW_CURSOR)==GLFW.GLFW_CURSOR_NORMAL
      && Math.abs(mc.mouseHandler.xpos()*s.width/mc.getWindow().getScreenWidth()-targetX)<1
      && Math.abs(mc.mouseHandler.ypos()*s.height/mc.getWindow().getScreenHeight()-targetY)<1;}

   private static JsonObject snapshot(MachineMenu m){
      var o=new JsonObject();int total=m.getCarried().getCount(),buckets=0;
      for(int i=0;i<m.slots.size();i++){var stack=m.getSlot(i).getItem();total+=stack.getCount();if(i>=2&&stack.is(Items.BUCKET))buckets+=stack.getCount();}
      o.addProperty("menu_id",m.containerId);o.addProperty("fluid_amount",m.fluidAmount());o.addProperty("fluid_capacity",m.fluidCapacity());
      o.addProperty("fluid_id",BuiltInRegistries.FLUID.getKey(m.storedFluid()).toString());o.addProperty("total_items",total);
      o.addProperty("machine_input_empty",m.getSlot(0).getItem().isEmpty());o.addProperty("machine_output_empty",m.getSlot(1).getItem().isEmpty());
      o.addProperty("player_buckets",buckets);o.addProperty("player_slot_2_buckets",m.getSlot(2).getItem().is(Items.BUCKET)?m.getSlot(2).getItem().getCount():0);
      o.addProperty("carried_buckets",m.getCarried().is(Items.BUCKET)?m.getCarried().getCount():0);return o;
   }
   private static boolean expected(JsonObject o){
      if(o==null)return false;int carried=captureIndex==4?1:0;
      return o.get("menu_id").getAsInt()==menuId&&o.get("fluid_amount").getAsInt()==amount()&&o.get("fluid_capacity").getAsInt()==32000
         &&o.get("fluid_id").getAsString().equals(BuiltInRegistries.FLUID.getKey(amount()==0?Fluids.EMPTY:fixtureFluid).toString())
         &&o.get("total_items").getAsInt()==1&&o.get("machine_input_empty").getAsBoolean()&&o.get("machine_output_empty").getAsBoolean()
         &&o.get("player_buckets").getAsInt()==1-carried&&o.get("player_slot_2_buckets").getAsInt()==1-carried&&o.get("carried_buckets").getAsInt()==carried;
   }
   private static boolean agrees(Minecraft mc,MachineMenu menu){
      if(!expected(snapshot(menu)))return false;
      if(inspection==null){inspection=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);require(p.containerMenu instanceof MachineMenu&&p.containerMenu.containerId==menuId,"Server menu mismatch");
         require(p.serverLevel().getEntitiesOfClass(ItemEntity.class,new AABB(POS).inflate(12)).isEmpty(),"Dropped item");
         return snapshot((MachineMenu)p.containerMenu);
      });return false;}
      if(!inspection.isDone())return false;serverSnapshot=inspection.join();inspection=null;return expected(serverSnapshot);
   }
   private static JsonObject capture(Minecraft mc,MachineScreen screen)throws Exception{
      require(atTarget(mc,screen),"Pointer missing");var out=directory.resolve("barrel-packaged-screenshots");Files.createDirectories(out);
      String file="barrel-"+NAMES[captureIndex]+".png";var path=out.resolve(file);require(!Files.exists(path),"Stale capture");
      int width,height;JsonObject pixels=null;
      try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
         width=image.getWidth();height=image.getHeight();
         require(width==mc.getWindow().getWidth()&&height==mc.getWindow().getHeight(),"Framebuffer changed");
         if(captureIndex<3)pixels=barrelFluidPixels(mc,screen,image);
         image.writeToFile(path);
      }
      var o=new JsonObject();o.addProperty("name",file);o.addProperty("sha256",PackagedRuntimeProof.hash(Files.readAllBytes(path)));
      o.addProperty("machine","barrel");o.addProperty("selected_fluid",selectedFluid);o.addProperty("menu_id",menuId);o.addProperty("screen_class",screen.getClass().getName());
      o.addProperty("width",width);o.addProperty("height",height);o.addProperty("window_width",mc.getWindow().getScreenWidth());o.addProperty("window_height",mc.getWindow().getScreenHeight());
      o.addProperty("gui_scale",mc.getWindow().getGuiScale());o.addProperty("gui_width",screen.width);o.addProperty("gui_height",screen.height);
      if(pixels!=null)o.add("barrel_fluid_pixels",pixels);
      o.addProperty("target_x",targetX);o.addProperty("target_y",targetY);o.addProperty("xtest_motion_count",moveCount);o.addProperty("recipe_toast_absent",!recipeToastVisible(mc));return o;
   }
   /** Samples the native tank interior from the same full framebuffer that is written as PNG. */
   private static JsonObject barrelFluidPixels(Minecraft mc,MachineScreen screen,NativeImage image)throws Exception{
      var menu=screen.getMenu();int fill=(int)Math.max(0,Math.min(70,(long)menu.fluidAmount()*70/menu.fluidCapacity()));
      int top=fill==0?38:108-fill;
      double sx=image.getWidth()/(double)screen.width,sy=image.getHeight()/(double)screen.height;
      int x=(int)Math.round((screen.getGuiLeft()+99)*sx),y=(int)Math.round((screen.getGuiTop()+top)*sy);
      int right=(int)Math.round((screen.getGuiLeft()+115)*sx),bottom=(int)Math.round((screen.getGuiTop()+107)*sy);
      require(x>=0&&y>=0&&right<=image.getWidth()&&bottom<=image.getHeight()&&right>x&&bottom>y,"Barrel sample exceeds framebuffer");
      var colors=new java.util.HashSet<Integer>();
      for(int py=y;py<bottom;py++)for(int px=x;px<right;px++)colors.add(image.getPixelRGBA(px,py));
      var result=new JsonObject();result.addProperty("x",x);result.addProperty("y",y);result.addProperty("width",right-x);result.addProperty("height",bottom-y);
      result.addProperty("distinct_rgba_colors",colors.size());result.addProperty("fluid_id",BuiltInRegistries.FLUID.getKey(menu.storedFluid()).toString());
      if(fill==0){require(menu.storedFluid()==Fluids.EMPTY&&colors.size()==1&&colors.contains(0xFF282014),"Empty Barrel backing differs");}
      else{
         require(menu.storedFluid()==fixtureFluid,"Barrel fixture fluid not synchronized");
         var stack=new FluidStack(menu.storedFluid(),menu.fluidAmount());
         var properties=net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(stack.getFluid());
         var still=properties.getStillTexture(stack);require(still!=null,"No native still texture");
         String expectedStill=switch(selectedFluid){case "water"->"minecraft:block/water_still";case "lava"->"minecraft:block/lava_still";case "milk"->"neoforge:block/milk_still";default->throw new IllegalStateException("Unknown fluid");};
         require(still.toString().equals(expectedStill),"Unexpected native still texture");
         var sprite=mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(still);
         require(sprite!=null&&sprite.contents().name().equals(still),"Missing native still sprite in block atlas");
         require(colors.size()>1,"Occupied Barrel tank is flat rather than textured");
         result.addProperty("still_texture",still.toString());result.addProperty("atlas_sprite",sprite.contents().name().toString());
         result.addProperty("tint_argb",String.format(java.util.Locale.ROOT,"%08x",properties.getTintColor(stack)));
         var texture=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(still.getNamespace(),"textures/"+still.getPath()+".png");
         var resource=mc.getResourceManager().getResource(texture).orElseThrow();
         // Fail closed on external pack overrides. NeoForge uses mod/<id>; vanilla uses vanilla.
         String expectedPack=selectedFluid.equals("milk")?"mod/neoforge":"vanilla";
         require(resource.sourcePackId().equals(expectedPack),"Native fluid texture overridden by another resource pack");
         result.addProperty("active_texture",texture.toString());result.addProperty("active_texture_pack",resource.sourcePackId());
         String activeHash;try(var input=resource.open()){activeHash=PackagedRuntimeProof.hash(input.readAllBytes());}
         result.addProperty("active_texture_sha256",activeHash);
         if(selectedFluid.equals("milk")){
            var owner=net.neoforged.fml.ModList.get().getModFileById("neoforge");require(owner!=null,"Missing milk resource owner");
            var origin=owner.getFile().findResource("assets/"+texture.getNamespace()+"/"+texture.getPath());
            require(Files.isRegularFile(origin)&&activeHash.equals(PackagedRuntimeProof.hash(Files.readAllBytes(origin))),"Milk texture differs from NeoForge mod origin");
            result.addProperty("authoritative_texture_uri",origin.toUri().toString());
         }

      }
      return result;
   }
   private static boolean recipeToastVisible(Minecraft mc){return mc.getToasts().getToast(net.minecraft.client.gui.components.toasts.RecipeToast.class,net.minecraft.client.gui.components.toasts.Toast.NO_TOKEN)!=null;}
   private static void enter(Stage next){stage=next;ticks=0;inspection=null;captureReady=false;}
   private static void finish(Minecraft mc,Throwable error){
      complete=true;
      try{
         if(display!=null){try{rightButton(false);button(false);}finally{x11.getFunction("XCloseDisplay").invokeInt(new Object[]{display});display=null;}}
         if(directory!=null){var proof=new JsonObject();proof.addProperty("suite","barrel-packaged-native-v1");proof.addProperty("nonce",nonce);
            proof.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha",""));proof.addProperty("status",error==null?"passed_bounded_native":"failed");
            proof.addProperty("selected_fluid",selectedFluid);proof.addProperty("selected_language",mc.getLanguageManager().getSelected());
            proof.addProperty("input_backend","XTEST");proof.addProperty("actual_integrated_world",mc.getSingleplayerServer()!=null&&mc.player!=null&&mc.player.connection.getConnection().isMemoryConnection());
            proof.addProperty("cleanup_released",!pressed&&!rightPressed);proof.add("packaged_runtime",packaged);proof.add("opening",opening);proof.add("captures",CAPTURES);proof.add("gestures",GESTURES);
            proof.addProperty("rendered_sprite_identity_verified",false);proof.addProperty("natural_collection_verified",false);proof.addProperty("visual_acceptance",false);
            proof.addProperty("scope","Native selected fluid identity and atlas availability checked; occupied region is nonflat. Exact rendered sprite/tint still requires human visual review. Server-injected selected-fluid empty/half/full Barrel, ordinary physical opening, renderer-end full-window captures and full tank hover, player inventory bucket pickup/return conservation. No natural collection, bucket processing, transfer automation, Freezer, Placer or V5 evidence.");
            if(error!=null)proof.addProperty("error",error.toString());Files.writeString(directory.resolve("client-barrel-packaged-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof));}
         if(error==null)System.out.println("IEDCTR_BARREL_PACKAGED PASSED nonce="+nonce+" source="+System.getProperty("iedctr.executedSourceSha",""));else error.printStackTrace();
      }catch(Throwable e){e.printStackTrace();}finally{mc.stop();}
   }
   private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
