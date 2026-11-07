package com.oblixorprime.iedctrtest.fluid;
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
import com.oblixorprime.engineersdecorreforged.utility.FluidMachineRedesignLayout;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.minecraft.core.Direction;

/** Isolated opt-in X11 gestures. Never included in the distributed mod JAR. */
@EventBusSubscriber(modid = FluidValidationMod.MOD_ID, value = Dist.CLIENT)
public final class NativeFluidGestureValidation {
   private enum Stage { BOOT, WORLD, OPEN, OPEN_RELEASE, OPEN_ACK, SETUP, TANK, TANK_RESET, HOVER, MOVE, PRESS, RELEASE, ACK, SEED, DONE }
   private static Stage stage = Stage.BOOT;
   private static final BlockPos POS = new BlockPos(0,80,0);
   private static final String SUITE = "native-fluid-gestures-v2";
   private static final String[] ACTIONS = {"pickup", "insert", "pickup_input", "return", "shift_insert", "shift_return", "process_insert", "output_pickup", "output_return", "invalid_pickup", "invalid_insert_attempt", "invalid_return"};
   // [input empty buckets, output water buckets, player empty buckets, player water buckets, carried empty, carried water, tank mB]
   private static final int[][] STATES = {{0,0,0,0,1,0,0},{1,0,0,0,0,0,0},{0,0,0,0,1,0,0},{0,0,1,0,0,0,0},
      {1,0,0,0,0,0,0},{0,0,1,0,0,0,0},{0,1,0,0,0,0,0},{0,0,0,0,0,1,0},{0,0,0,1,0,0,0},{0,0,0,0,0,1,0},{0,0,0,0,0,1,0},{0,0,0,1,0,0,0}};
   private static int ticks, machineIndex, hoverIndex, actionIndex, menuId, tooltipFrames, moveCount, buttonEvents, keyEvents;
   private static long deadline;
   private static Path directory;
   private static String nonce;
   private static UUID playerId;
   private static MachineBlockEntity machine;
   private static CompletableFuture<?> pending;
   private static CompletableFuture<JsonObject> inspection;
   private static JsonObject serverSnapshot, packaged;
   private static int tankIndex, tankAmount, tankCapacity;
   private static boolean rightPressed, rightObserved;
   private static final JsonArray OPENINGS = new JsonArray(), TANKS = new JsonArray();
   private static final JsonArray HOVERS = new JsonArray(), GESTURES = new JsonArray(), SEEDS = new JsonArray(), CONTRACTS = new JsonArray();
   private static NativeLibrary x11, xtest;
   private static Pointer display;
   private static boolean isolated, complete, pressed, shifted, observedPress, observedShift, hoverCaptureReady;
   private static int targetX, targetY, tooltipStart, targetSlot;

   private static final String[] HOVER_ROLES = {"header","tank","empty_input","empty_output","structure","occupied_input"};
   private static long renderId, screenId, outerFrameId;
   private static boolean outerFrameOpen;
   private static JsonObject pendingHoverGeometry;
   private static MachineScreen pendingHoverScreen;
   private static MachineMenu pendingHoverMenu;

   private static void clearPendingHover() {
      pendingHoverGeometry=null;pendingHoverScreen=null;pendingHoverMenu=null;
   }
   @SubscribeEvent(priority=EventPriority.HIGHEST)
   public static void beforeFrame(RenderFrameEvent.Pre event) {
      outerFrameId++;outerFrameOpen=true;clearPendingHover();tooltipGeometry=null;renderedScreen=null;
   }
   private static net.minecraft.client.gui.screens.Screen renderedScreen, observedScreen;
   private static JsonObject tooltipGeometry;
   private static int tooltipEventsInFrame, renderedPointerX, renderedPointerY;

   @SubscribeEvent public static void beforeRender(ScreenEvent.Render.Pre event) {
      clearPendingHover();renderId++; tooltipGeometry=null; tooltipEventsInFrame=0;
      renderedScreen=event.getScreen();renderedPointerX=event.getMouseX();renderedPointerY=event.getMouseY();
      if(observedScreen!=renderedScreen) { observedScreen=renderedScreen; screenId++; }
   }
   @SubscribeEvent public static void tooltip(RenderTooltipEvent.Color event) {
      Minecraft mc=Minecraft.getInstance();
      // Only the current Pre -> Color -> Post sequence may supply capture evidence.
      if(!outerFrameOpen || !isolated || stage!=Stage.HOVER || renderedScreen!=mc.screen
         || !(mc.screen instanceof MachineScreen screen) || screen.getMenu().containerId!=menuId
         || !atTarget(mc,screen)
         || renderedPointerX!=(int)(mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth())
         || renderedPointerY!=(int)(mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight())) return;
      tooltipEventsInFrame++;
      int width=0,height=event.getComponents().size()==1?-2:0;
      for(var component:event.getComponents()) {
         width=Math.max(width,component.getWidth(event.getFont()));height+=component.getHeight();
      }
      var g=new JsonObject();
      g.addProperty("outer_frame_id",outerFrameId);g.addProperty("render_id",renderId);g.addProperty("screen_id",screenId);
      g.addProperty("menu_id",screen.getMenu().containerId);
      g.addProperty("machine",machineIndex==0?"funnel":"accumulator");g.addProperty("role",HOVER_ROLES[hoverIndex]);
      g.addProperty("pointer_x",mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth());
      g.addProperty("pointer_y",mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight());
      g.addProperty("text_x",event.getX());g.addProperty("text_y",event.getY());
      g.addProperty("text_width",width);g.addProperty("text_height",height);
      g.addProperty("frame_margin",4);g.addProperty("frame_left",event.getX()-4);g.addProperty("frame_top",event.getY()-4);
      g.addProperty("frame_right",event.getX()+width+4);g.addProperty("frame_bottom",event.getY()+height+4);
      g.addProperty("gui_width",screen.width);g.addProperty("gui_height",screen.height);
      tooltipGeometry=g;
   }
   @SubscribeEvent public static void afterRender(ScreenEvent.Render.Post event) {
      Minecraft mc=Minecraft.getInstance();
      try {
         if(!outerFrameOpen || tooltipGeometry==null || tooltipEventsInFrame!=1 || !isolated || stage!=Stage.HOVER
            || event.getScreen()!=renderedScreen || event.getScreen()!=mc.screen
            || !(event.getScreen() instanceof MachineScreen screen) || screen.getMenu().containerId!=menuId
            || !atTarget(mc,screen) || event.getMouseX()!=renderedPointerX || event.getMouseY()!=renderedPointerY) return;
         tooltipFrames++;
         if(!hoverCaptureReady || recipeToastVisible(mc)) return;
         var g=tooltipGeometry;
         require(g.get("render_id").getAsLong()==renderId && g.get("screen_id").getAsLong()==screenId
            && g.get("role").getAsString().equals(HOVER_ROLES[hoverIndex])
            && g.get("pointer_x").getAsDouble()==mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth()
            && g.get("pointer_y").getAsDouble()==mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight(),"Stale tooltip geometry");
         require(g.get("text_width").getAsInt()>0 && g.get("text_height").getAsInt()>0
            && g.get("frame_left").getAsInt()>=0 && g.get("frame_top").getAsInt()>=0
            && g.get("frame_right").getAsInt()<=screen.width && g.get("frame_bottom").getAsInt()<=screen.height,
            "Tooltip frame clipped: "+g);
         g.addProperty("screen_post_outer_frame_id",outerFrameId);
         pendingHoverGeometry=g.deepCopy();pendingHoverScreen=screen;pendingHoverMenu=screen.getMenu();
      } catch(Throwable error) {clearPendingHover();finish(mc,error);}
      finally {tooltipGeometry=null;renderedScreen=null;}
   }
   // NeoForge 21.1.230 fires this after GameRenderer.render returns, including JEI's Screen.Post draw.
   // This does not include Minecraft's later debug profiler pie or arbitrary later post-frame mod draws.
   @SubscribeEvent(priority=EventPriority.LOWEST)
   public static void afterFrame(RenderFrameEvent.Post event) {
      Minecraft mc=Minecraft.getInstance();
      try {
         if(pendingHoverGeometry==null) return;
         var g=pendingHoverGeometry;var screen=pendingHoverScreen;
         require(outerFrameOpen && g.get("outer_frame_id").getAsLong()==outerFrameId
            && g.get("screen_post_outer_frame_id").getAsLong()==outerFrameId
            && g.get("render_id").getAsLong()==renderId,"Stale outer frame");
         require(!complete && isolated && stage==Stage.HOVER && hoverCaptureReady
            && screen==mc.screen && screen==observedScreen && g.get("screen_id").getAsLong()==screenId
            && screen.getMenu()==pendingHoverMenu && mc.player!=null && mc.player.containerMenu==pendingHoverMenu
            && pendingHoverMenu.containerId==menuId && g.get("menu_id").getAsInt()==menuId
            && g.get("role").getAsString().equals(HOVER_ROLES[hoverIndex])
            && g.get("machine").getAsString().equals(machineIndex==0?"funnel":"accumulator"),"Hover identity changed after Screen.Post");
         require(atTarget(mc,screen)
            && g.get("pointer_x").getAsDouble()==mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth()
            && g.get("pointer_y").getAsDouble()==mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight()
            && g.get("gui_width").getAsInt()==screen.width && g.get("gui_height").getAsInt()==screen.height,
            "Hover pointer/viewport changed after Screen.Post");
         require(mc.getOverlay()==null && !recipeToastVisible(mc) && !mc.getDebugOverlay().showProfilerChart(),
            "Capture overlay/profiler state changed");
         int[] state=hoverIndex==5?new int[]{1,0,0,0,0,0,0}:new int[]{0,0,1,0,0,0,0};
         require(expected(snapshot(pendingHoverMenu),state) && expected(serverSnapshot,state),"Hover state changed after arming");
         // The screen-render geometry is retained; screenshot and state advance wait for this renderer-end event.
         JsonObject h=capture(mc,screen,"hover-"+hoverIndex);
         h.addProperty("capture_phase","RenderFrameEvent.Post:LOWEST");h.addProperty("capture_outer_frame_id",outerFrameId);
         h.addProperty("profiler_chart_disabled",true);
         h.addProperty("role",HOVER_ROLES[hoverIndex]);h.addProperty("capture_render_id",renderId);h.addProperty("capture_screen_id",screenId);
         h.add("tooltip_geometry",g.deepCopy());h.addProperty("tooltip_render_events",tooltipFrames-tooltipStart);
         h.addProperty("tooltip_event_class","RenderTooltipEvent.Color+ScreenEvent.Render.Post");
         h.add("client",snapshot(screen.getMenu()));h.add("server",serverSnapshot.deepCopy());HOVERS.add(h);
         if(++hoverIndex<5) { hoverTarget(screen,screen.getMenu()); enter(Stage.HOVER); }
         else if(hoverIndex==6) { actionIndex=2; startAction(screen,screen.getMenu()); }
         else { actionIndex=0; startAction(screen,screen.getMenu()); }
      } catch(Throwable error) {finish(mc,error);}
      finally {clearPendingHover();tooltipGeometry=null;renderedScreen=null;outerFrameOpen=false;}
   }
   @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
      if (!Boolean.getBoolean("iedctr.fluidPackagedNative") || complete) return;
      Minecraft mc = Minecraft.getInstance();
      try {
         if (deadline == 0) deadline = System.nanoTime()+600_000_000_000L;
         require(System.nanoTime()<deadline && ++ticks<600,"Timeout at "+stage+" action "+actionIndex);
         if (stage==Stage.BOOT) { if(mc.screen instanceof TitleScreen && mc.getOverlay()==null) boot(mc); return; }
         if(stage==Stage.WORLD) {
            if(mc.player==null || mc.level==null || mc.screen!=null) return;
            require(mc.getSingleplayerServer()!=null && mc.player.connection.getConnection().isMemoryConnection(),"Integrated world required");
            playerId=mc.player.getUUID(); setupAsync(mc); return;
         }
         require(mc.getSingleplayerServer()!=null && mc.player!=null,"World disconnected");
         if(pending!=null) { if(!pending.isDone()) return; pending.join(); pending=null; }
         if(mc.getOverlay()!=null) return;
         if(stage==Stage.OPEN) {
            if(mc.screen!=null || ticks<20 || !(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)
               || !hit.getBlockPos().equals(POS)) return;
            require(mc.player.getMainHandItem().isEmpty() && mc.player.getEyePosition().distanceTo(hit.getLocation())<4,
               "Opening must be empty-hand and within survival reach");
            if(GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_FOCUSED)!=GLFW.GLFW_TRUE) {
               GLFW.glfwFocusWindow(mc.getWindow().getWindow());return;
            }
            rightButton(true); enter(Stage.OPEN_RELEASE); return;
         }
         if(stage==Stage.OPEN_RELEASE) {
            if(ticks<3 || GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_RIGHT)!=GLFW.GLFW_PRESS) return;
            rightObserved=true;rightButton(false);enter(Stage.OPEN_ACK);return;
         }
         if(stage==Stage.OPEN_ACK) {
            if(ticks<4 || !(mc.screen instanceof MachineScreen) || !(mc.player.containerMenu instanceof MachineMenu opened)
               || GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_RIGHT)!=GLFW.GLFW_RELEASE) return;
            menuId=opened.containerId;
            var opening=new JsonObject();opening.addProperty("machine",machineIndex==0?"funnel":"accumulator");
            opening.addProperty("menu_id",menuId);opening.addProperty("hit_block","0,80,0");
            opening.addProperty("native_right_press_observed",rightObserved);opening.addProperty("native_right_release_observed",!rightPressed);
            opening.addProperty("method","XTEST_RMB_BLOCK_HIT");OPENINGS.add(opening);
            pending=mc.getSingleplayerServer().submit(()->recordContract(mc)); enter(Stage.SETUP);return;
         }
         if(!(mc.screen instanceof MachineScreen screen) || !(mc.player.containerMenu instanceof MachineMenu menu)
            || menu.containerId!=menuId || mc.getOverlay()!=null) return;
         require(menu.kind()==(machineIndex==0?MachineKind.SMALL_FLUID_FUNNEL:MachineKind.PASSIVE_FLUID_ACCUMULATOR),"Wrong machine");
         switch(stage) {
            case SETUP -> {
               if(ticks<15 || !agrees(mc,menu,new int[]{0,0,1,0,0,0,0})) return;
               require(menu.visibleMachineSlots()==2 && menu.slots.size()==38,"Wrong slot projection");
               boolean clientAcceptsEmpty=menu.getSlot(0).mayPlace(new ItemStack(Items.BUCKET));
               boolean clientOutputRejects=!menu.getSlot(1).mayPlace(new ItemStack(Items.BUCKET));
               require(clientAcceptsEmpty && clientOutputRejects,"Client slot projection changed");
               var contract=CONTRACTS.get(machineIndex).getAsJsonObject();
               require(contract.get("menu_id").getAsInt()==menu.containerId,"Contract menu context changed");
               contract.addProperty("client_accepts_empty_bucket",clientAcceptsEmpty);
               contract.addProperty("client_output_rejects_insertion",clientOutputRejects);
               tankIndex=0;tankCapacity=menu.fluidCapacity();
               require(tankCapacity==(machineIndex==0?3000:4000),"Unexpected tank capacity");
               seedTank(mc);enter(Stage.TANK);
            }
            case TANK -> {
               // Setup-only seeds. The empty bucket remains outside input, so ordinary processing cannot consume fluid.
               if(ticks<120 || recipeToastVisible(mc) || !agrees(mc,menu,new int[]{0,0,1,0,0,0,tankAmount})) return;
               targetX=screen.getGuiLeft()+8;targetY=screen.getGuiTop()+8;
               if(!atTarget(mc,screen)){move(mc,screen);return;}
               var row=capture(mc,screen,"tank-"+new String[]{"empty","half","full"}[tankIndex]);
               row.addProperty("setup_seed",true);row.addProperty("amount",tankAmount);row.addProperty("capacity",tankCapacity);
               row.add("client",snapshot(menu));row.add("server",serverSnapshot.deepCopy());TANKS.add(row);
               if(++tankIndex<3){seedTank(mc);enter(Stage.TANK);}else{tankIndex=0;seedTank(mc);enter(Stage.TANK_RESET);}
            }
            case TANK_RESET -> {
               if(!agrees(mc,menu,new int[]{0,0,1,0,0,0,0}))return;
               hoverIndex=0;hoverTarget(screen,menu);enter(Stage.HOVER);
            }
            case HOVER -> {
               hoverCaptureReady=false;
               if(!atTarget(mc,screen)) { move(mc,screen); return; }
               if(ticks<120 || recipeToastVisible(mc) || tooltipFrames<=tooltipStart
                  || !agrees(mc,menu,hoverIndex==5?new int[]{1,0,0,0,0,0,0}:new int[]{0,0,1,0,0,0,0})) return;
               hoverCaptureReady=true;
            }
            case MOVE -> {
               if(!atTarget(mc,screen)) { move(mc,screen); return; }
               // Separate clicks by >250ms to exclude vanilla PICKUP_ALL double-click semantics.
               if(ticks<8) return;
               if(actionIndex==4 || actionIndex==5 || actionIndex==6) key(true);
               enter(Stage.PRESS);
            }
            case PRESS -> {
               if(ticks<3) return;
               if(shifted && GLFW.glfwGetKey(mc.getWindow().getWindow(),GLFW.GLFW_KEY_LEFT_SHIFT)!=GLFW.GLFW_PRESS) return;
               require(atTarget(mc,screen) && menu.containerId==menuId && mc.player.containerMenu==menu
                  && targetX==screen.getGuiLeft()+menu.getSlot(targetSlot).x+8
                  && targetY==screen.getGuiTop()+menu.getSlot(targetSlot).y+8,"Press target/menu changed");
               observedShift=shifted && GLFW.glfwGetKey(mc.getWindow().getWindow(),GLFW.GLFW_KEY_LEFT_SHIFT)==GLFW.GLFW_PRESS;
               button(true); enter(Stage.RELEASE);
            }
            case RELEASE -> {
               if(ticks<3 || GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_LEFT)!=GLFW.GLFW_PRESS) return;
               observedPress=true;button(false); if(shifted) key(false); enter(Stage.ACK);
            }
            case ACK -> {
               if(ticks<8 || GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_LEFT)!=GLFW.GLFW_RELEASE
                  || GLFW.glfwGetKey(mc.getWindow().getWindow(),GLFW.GLFW_KEY_LEFT_SHIFT)!=GLFW.GLFW_RELEASE
                  || !agrees(mc,menu,STATES[actionIndex])) return;
               // Unchanged state after invalid_insert_attempt does not prove receipt of a server click packet.
               JsonObject g=capture(mc,screen,ACTIONS[actionIndex]); g.addProperty("action",ACTIONS[actionIndex]);g.addProperty("menu_slot",targetSlot);
               g.add("client",snapshot(menu)); g.add("server",serverSnapshot.deepCopy());
               g.addProperty("native_press_observed",observedPress);g.addProperty("native_release_observed",GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_RELEASE);
               g.addProperty("native_shift_observed",observedShift);g.addProperty("button_events",buttonEvents);g.addProperty("key_events",keyEvents);g.addProperty("released",!pressed&&!shifted);
               GESTURES.add(g);
               if(actionIndex==1) {hoverIndex=5;hoverTarget(screen,menu);enter(Stage.HOVER);}
               else if(actionIndex==5) {
                  pending=mc.getSingleplayerServer().submit(()-> {
                     require(machine.fluidHandler(Direction.UP).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==1000,"Water seed failed");
                     player(mc).containerMenu.broadcastChanges();
                  }); enter(Stage.SEED);
               } else if(++actionIndex<ACTIONS.length) startAction(screen,menu);
               else if(++machineIndex<2) setupAsync(mc);
               else finish(mc,null);
            }
            case SEED -> { if(agrees(mc,menu,new int[]{0,0,1,0,0,0,1000})) {var seed=new JsonObject();seed.add("client",snapshot(menu));seed.add("server",serverSnapshot.deepCopy());SEEDS.add(seed);actionIndex=6;startAction(screen,menu);} }
            default -> { }
         }
      } catch(Throwable failure) { finish(mc,failure); }
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
         && Files.readString(directory.resolve(".iedctr-fluid-xvfb")).equals(nonce+"\n"+nativeDisplay),"Runner-owned display/nonce mismatch");
      require(mc.level==null && mc.getSingleplayerServer()==null && !Files.exists(directory.resolve("fluid-gesture-screenshots")),"Existing world/evidence forbidden");
      if(Files.exists(directory.resolve("saves"))) try(var entries=Files.list(directory.resolve("saves"))) {require(entries.findAny().isEmpty(),"Existing saves forbidden");}
      int requestedScale=Integer.getInteger("iedctr.requestedGuiScale",0);
      require(requestedScale>=1 && requestedScale<=3 && mc.getWindow().getGuiScale()==requestedScale,"Requested GUI scale was clamped or changed");
      require(mc.getWindow().getGuiScaledWidth()>=320 && mc.getWindow().getGuiScaledHeight()>=256,"Machine frame does not fit logical viewport");
      require(!mc.getDebugOverlay().showProfilerChart(),"Debug profiler must be disabled for renderer-end capture");
      packaged=PackagedRuntimeProof.verify(mc);
      isolated=true; mc.options.pauseOnLostFocus=false;
      x11=NativeLibrary.getInstance("X11");xtest=NativeLibrary.getInstance("Xtst");
      display=x11.getFunction("XOpenDisplay").invokePointer(new Object[]{null}); require(display!=null,"X11 unavailable");
      String name="iedctr-fluid-gestures-"+nonce;enter(Stage.WORLD);
      mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
         new WorldOptions(0L,false,false),registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
   }
   private static ServerPlayer player(Minecraft mc) {return mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);}
   private static void setupAsync(Minecraft mc) {
      pending=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);p.closeContainer();p.setGameMode(GameType.SURVIVAL);var level=p.serverLevel();
         level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,mc.getSingleplayerServer());level.setWeatherParameters(1000000,0,false,false);
         for(int x=-3;x<=3;x++) for(int y=79;y<=83;y++) for(int z=-3;z<=3;z++) level.setBlock(new BlockPos(x,y,z),y==79?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
         p.connection.teleport(.5,80,2.5,180,29.25f);
         level.setBlock(POS,(machineIndex==0?ModBlocks.SMALL_FLUID_FUNNEL.get():ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get()).defaultBlockState(),3);
         machine=(MachineBlockEntity)level.getBlockEntity(POS);machine.clearContent();
         machine.fluidHandler(Direction.UP).drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
         p.getInventory().clearContent();p.getInventory().setItem(9,new ItemStack(Items.BUCKET));
      });rightObserved=false;enter(Stage.OPEN);
   }
   private static void seedTank(Minecraft mc) {
      tankAmount=tankIndex==0?0:tankIndex==1?tankCapacity/2:tankCapacity;
      pending=mc.getSingleplayerServer().submit(()->{
         machine.fluidHandler(Direction.UP).drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
         if(tankAmount>0)require(machine.fluidHandler(Direction.UP).fill(new FluidStack(Fluids.WATER,tankAmount),FluidAction.EXECUTE)==tankAmount,"Tank setup seed failed");
         player(mc).containerMenu.broadcastChanges();
      });
   }
   private static void recordContract(Minecraft mc) {
         var p=player(mc);require(p.containerMenu.containerId==menuId,"Opening menu mismatch");
         require(p.containerMenu instanceof MachineMenu,"Missing authoritative machine menu");
         var serverMenu=(MachineMenu)p.containerMenu;
         boolean serverAcceptsEmpty=serverMenu.getSlot(0).mayPlace(new ItemStack(Items.BUCKET));
         boolean serverRejectsFilled=!serverMenu.getSlot(0).mayPlace(new ItemStack(Items.WATER_BUCKET));
         boolean backingAcceptsEmpty=machine.canPlaceItem(serverMenu.getSlot(0).getSlotIndex(),new ItemStack(Items.BUCKET));
         boolean backingRejectsFilled=!machine.canPlaceItem(serverMenu.getSlot(0).getSlotIndex(),new ItemStack(Items.WATER_BUCKET));
         boolean serverOutputRejects=!serverMenu.getSlot(1).mayPlace(new ItemStack(Items.BUCKET));
         require(serverAcceptsEmpty && serverRejectsFilled && backingAcceptsEmpty && backingRejectsFilled && serverOutputRejects,
            "Authoritative input/output contract changed");
         var contract=new JsonObject();contract.addProperty("machine",machineIndex==0?"funnel":"accumulator");
         contract.addProperty("menu_id",serverMenu.containerId);
         contract.addProperty("server_menu_class",serverMenu.getClass().getName());
         contract.addProperty("server_accepts_empty_bucket",serverAcceptsEmpty);
         contract.addProperty("server_rejects_water_bucket",serverRejectsFilled);
         contract.addProperty("backing_accepts_empty_bucket",backingAcceptsEmpty);
         contract.addProperty("backing_rejects_water_bucket",backingRejectsFilled);
         contract.addProperty("server_output_rejects_insertion",serverOutputRejects);CONTRACTS.add(contract);
   }
   private static void hoverTarget(MachineScreen screen,MachineMenu menu) {
      var r=switch(hoverIndex) {
         case 0->FluidMachineRedesignLayout.HEADER;
         case 1->machineIndex==0?FluidMachineRedesignLayout.FUNNEL_TANK:FluidMachineRedesignLayout.ACCUMULATOR_TANK;
         case 2,5->machineIndex==0?FluidMachineRedesignLayout.FUNNEL_INPUT:FluidMachineRedesignLayout.ACCUMULATOR_INPUT;
         case 3->machineIndex==0?FluidMachineRedesignLayout.FUNNEL_OUTPUT:FluidMachineRedesignLayout.ACCUMULATOR_OUTPUT;
         default->machineIndex==0?FluidMachineRedesignLayout.FUNNEL_OUTLET:FluidMachineRedesignLayout.ACCUMULATOR_COLLECTION;
      };
      targetX=screen.getGuiLeft()+r.x()+r.width()/2;targetY=screen.getGuiTop()+r.y()+r.height()/2;tooltipStart=tooltipFrames;move(Minecraft.getInstance(),screen);
   }
   private static void startAction(MachineScreen screen,MachineMenu menu) {
      observedPress=false;observedShift=false;
      int slot=switch(actionIndex) {case 0,3,8,9,11->2;case 1,2,5,10->0;case 7->1;default->findPlayerBucket(menu);};
      targetSlot=slot;targetX=screen.getGuiLeft()+menu.getSlot(slot).x+8;targetY=screen.getGuiTop()+menu.getSlot(slot).y+8;
      move(Minecraft.getInstance(),screen);enter(Stage.MOVE);
   }
   private static int findPlayerBucket(MachineMenu m) {for(int i=2;i<m.slots.size();i++) if(m.getSlot(i).getItem().is(Items.BUCKET)) return i;throw new IllegalStateException("Missing player bucket");}
   // Adapted from committed NativeMooshroomMilkingValidation, not its working tree.
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
   private static void key(boolean down){int code=x11.getFunction("XKeysymToKeycode").invokeInt(new Object[]{display,new NativeLong(0xffe1)});require(code>0,"Shift key missing");require(xtest.getFunction("XTestFakeKeyEvent").invokeInt(new Object[]{display,code,down?1:0,new NativeLong(0)})!=0,"XTest key rejected");shifted=down;keyEvents++;flush();}
   private static boolean atTarget(Minecraft mc,MachineScreen s){return GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_FOCUSED)==GLFW.GLFW_TRUE
      && GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_VISIBLE)==GLFW.GLFW_TRUE
      && GLFW.glfwGetInputMode(mc.getWindow().getWindow(),GLFW.GLFW_CURSOR)==GLFW.GLFW_CURSOR_NORMAL
      && Math.abs(mc.mouseHandler.xpos()*s.width/mc.getWindow().getScreenWidth()-targetX)<1
      && Math.abs(mc.mouseHandler.ypos()*s.height/mc.getWindow().getScreenHeight()-targetY)<1;}
   private static JsonObject snapshot(MachineMenu m){
      int[] a=new int[7];a[0]=count(m.getSlot(0).getItem(),Items.BUCKET);a[1]=count(m.getSlot(1).getItem(),Items.WATER_BUCKET);
      int total=m.getCarried().getCount();for(int i=0;i<m.slots.size();i++){var s=m.getSlot(i).getItem();total+=s.getCount();if(i>=2){a[2]+=count(s,Items.BUCKET);a[3]+=count(s,Items.WATER_BUCKET);}}
      a[4]=count(m.getCarried(),Items.BUCKET);a[5]=count(m.getCarried(),Items.WATER_BUCKET);a[6]=m.fluidAmount();
      var o=new JsonObject();var array=new JsonArray();for(int n:a)array.add(n);o.add("state",array);o.addProperty("total",total);o.addProperty("menu_id",m.containerId);return o;
   }
   private static int count(ItemStack s,net.minecraft.world.item.Item item){return s.is(item)?s.getCount():0;}
   private static boolean expected(JsonObject o,int[] state){if(o.get("total").getAsInt()!=1)return false;var a=o.getAsJsonArray("state");for(int i=0;i<7;i++)if(a.get(i).getAsInt()!=state[i])return false;return true;}
   private static boolean agrees(Minecraft mc,MachineMenu menu,int[] state){
      if(!expected(snapshot(menu),state))return false;
      if(inspection==null){inspection=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);require(p.containerMenu instanceof MachineMenu && p.containerMenu.containerId==menuId,"Server menu mismatch");
         require(p.serverLevel().getEntitiesOfClass(ItemEntity.class,new AABB(POS).inflate(12)).isEmpty(),"Dropped item");
         return snapshot((MachineMenu)p.containerMenu);
      });return false;}
      if(!inspection.isDone())return false;serverSnapshot=inspection.join();inspection=null;return expected(serverSnapshot,state);
   }
   private static JsonObject capture(Minecraft mc,MachineScreen screen,String name) throws Exception {
      require(atTarget(mc,screen),"Minecraft pointer not delivered");var out=directory.resolve("fluid-gesture-screenshots");Files.createDirectories(out);
      String file=(machineIndex==0?"funnel-":"accumulator-")+name+".png";var path=out.resolve(file);require(!Files.exists(path),"Stale capture");
      try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(path);}
      var o=new JsonObject();o.addProperty("machine",machineIndex==0?"funnel":"accumulator");o.addProperty("name",file);o.addProperty("menu_id",menuId);
      o.addProperty("screen_class",screen.getClass().getName());
      o.addProperty("sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
      o.addProperty("gui_scale",mc.getWindow().getGuiScale());o.addProperty("gui_width",screen.width);o.addProperty("gui_height",screen.height);
      o.addProperty("window_width",mc.getWindow().getScreenWidth());o.addProperty("window_height",mc.getWindow().getScreenHeight());
      o.addProperty("raw_mouse_x",mc.mouseHandler.xpos());o.addProperty("raw_mouse_y",mc.mouseHandler.ypos());
      o.addProperty("width",mc.getWindow().getWidth());o.addProperty("height",mc.getWindow().getHeight());
      o.addProperty("target_x",targetX);o.addProperty("target_y",targetY);o.addProperty("actual_x",mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth());o.addProperty("actual_y",mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight());
      o.addProperty("xtest_motion_count",moveCount);o.addProperty("recipe_toast_absent",!recipeToastVisible(mc));return o;
   }
   private static boolean recipeToastVisible(Minecraft mc){return mc.getToasts().getToast(net.minecraft.client.gui.components.toasts.RecipeToast.class,net.minecraft.client.gui.components.toasts.Toast.NO_TOKEN)!=null;}
   private static void enter(Stage next){stage=next;ticks=0;inspection=null;hoverCaptureReady=false;}
   private static void finish(Minecraft mc,Throwable error){
      complete=true;
      try {if(display!=null){try{rightButton(false);button(false);}finally{try{key(false);}finally{x11.getFunction("XCloseDisplay").invokeInt(new Object[]{display});display=null;}}}
         if(directory!=null){var proof=new JsonObject();proof.addProperty("suite",SUITE);proof.addProperty("nonce",nonce);proof.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha",""));
            proof.addProperty("status",error==null?"passed_bounded_native":"failed");proof.addProperty("input_backend","XTEST");proof.addProperty("actual_integrated_world",mc.getSingleplayerServer()!=null && mc.player!=null && mc.player.connection.getConnection().isMemoryConnection());
            proof.addProperty("invalid_input_evidence","native-attempt-and-state-conservation-only");
            proof.addProperty("selected_language",mc.getLanguageManager().getSelected());proof.addProperty("cleanup_released",!pressed&&!shifted&&!rightPressed);proof.add("packaged_runtime",packaged);proof.add("native_openings",OPENINGS);proof.add("tank_captures",TANKS);proof.add("hovers",HOVERS);proof.add("gestures",GESTURES);proof.add("process_seeds",SEEDS);proof.add("slot_contracts",CONTRACTS);
            if(error!=null)proof.addProperty("error",error.toString());Files.writeString(directory.resolve("client-fluid-gestures-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof));}
         if(error==null)System.out.println("IEDCTR_FLUID_GESTURES PASSED nonce="+nonce);else error.printStackTrace();
      }catch(Throwable failure){failure.printStackTrace();}finally{mc.stop();}
   }
   private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
