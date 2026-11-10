package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.TreeCutterV3ReferenceScreen;
import com.oblixorprime.engineersdecorreforged.utility.TreeCutterV3ReferenceGeometry;
import com.mojang.blaze3d.platform.NativeImage;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/** Opt-in isolated synthetic inventory qualification; not a tree-felling, drag or keyboard-input claim. */
@EventBusSubscriber(modid = FactoryPlacerValidationMod.MOD_ID, value = Dist.CLIENT)
public final class TreeCutterPackagedValidation {
    private static final BlockPos POS = new BlockPos(0,80,0);
    private enum Stage { BOOT, WORLD, INTERACT, OPEN, SEEDED, PICKED, REJECTED, RETURNED, CAPTURE, CLOSE, REOPEN, RELOAD }
    private static Stage stage=Stage.BOOT;
    private static boolean complete;
    private static int ticks,index,opened,oldMenu,pickups,returns,outputAttemptsPreserved;
    private static long deadline,lastClick;
    private static int physicalSlotClicks;
    private static UUID playerId;
    private static MachineBlockEntity machine;
    private static CompletableFuture<?> pending;
    private static CompletableFuture<Boolean> inspection;
    private static Path directory;
    private static String nonce;
    private static JsonObject packaged;
    private static final JsonArray routes=new JsonArray(),captures=new JsonArray();
    private static NativeLibrary x11,xtest;
    private static Pointer display;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("iedctr.treeCutterPackagedNative") || complete)return;
        var mc=Minecraft.getInstance();
        try {
            if(deadline==0)deadline=System.nanoTime()+900_000_000_000L;
            require(System.nanoTime()<deadline && ++ticks<1800,"Timeout at "+stage);
            require(System.getProperty(MachineMenu.DEV_FACTORY_PLACER_V2_REFERENCE_PROPERTY)==null,"Presentation selector forbidden");
            if(stage==Stage.BOOT){if(mc.screen instanceof TitleScreen && mc.getOverlay()==null)boot(mc);return;}
            if(stage==Stage.WORLD){
                if(mc.level==null || mc.player==null || mc.gameMode==null || mc.screen!=null)return;
                require(mc.getSingleplayerServer()!=null && mc.player.connection.getConnection().isMemoryConnection(),"Integrated world required");
                playerId=mc.player.getUUID();pending=mc.getSingleplayerServer().submit(()->setup(mc));enter(Stage.INTERACT);return;
            }
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(mc.getOverlay()!=null)return;
            require(mc.level!=null && mc.player!=null && mc.getSingleplayerServer()!=null,"Disconnected");
            if(stage==Stage.INTERACT){
                if(ticks<20 || !mc.level.getBlockState(POS).is(ModBlocks.SMALL_TREE_CUTTER.get()))return;
                require(mc.screen==null && mc.player.getMainHandItem().isEmpty(),"Empty-hand interaction required");
                if(!(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(POS))return;
                if(GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_FOCUSED)!=GLFW.GLFW_TRUE){GLFW.glfwFocusWindow(mc.getWindow().getWindow());return;}
                require(mc.player.getEyePosition().distanceTo(hit.getLocation())<4,"Beyond survival reach");
                nativeClick(3);enter(Stage.OPEN);return;
            }
            if(stage==Stage.CLOSE){
                if(mc.screen!=null || !matches(mc,p->p.containerMenu==p.inventoryMenu && finalState(p)))return;
                pending=mc.getSingleplayerServer().submit(()->player(mc).openMenu(machine));enter(Stage.REOPEN);return;
            }
            if(!(mc.screen instanceof TreeCutterV3ReferenceScreen screen)){
                require(mc.screen==null,"Unexpected default screen: "+mc.screen);return;
            }
            var menu=(MachineMenu)mc.player.containerMenu;
            geometry(mc,screen,menu);
            switch(stage){
                case OPEN -> {
                    if(ticks<15 || !matches(mc,p->p.containerMenu instanceof MachineMenu && machine.isEmpty() && p.getInventory().isEmpty()))return;
                    opened++;park(mc);capture(mc,screen,"normal");seed(mc);
                }
                case SEEDED -> {
                    if(ticks<10 || !log(menu.getSlot(index).getItem()) || !menu.getCarried().isEmpty() || !matches(mc,p->total(p)==1))return;
                    if(!click(screen,index))return;enter(Stage.PICKED);
                }
                case PICKED -> {
                    if(ticks<8 || System.nanoTime()-lastClick<350_000_000L || !menu.getSlot(index).getItem().isEmpty() || !log(menu.getCarried())
                        || !matches(mc,p->p.containerMenu.getSlot(index).getItem().isEmpty() && log(p.containerMenu.getCarried()) && total(p)==1))return;
                    // Exercise the physical input path, not direct screen-handler calls.
                    // Unchanged post-attempt state does NOT prove the server processed a click packet.
                    if(!click(screen,0))return;pickups++;enter(Stage.REJECTED);
                }
                case REJECTED -> {
                    if(ticks<8 || System.nanoTime()-lastClick<350_000_000L || !menu.getSlot(0).getItem().isEmpty() || !log(menu.getCarried())
                        || !matches(mc,p->machine.isEmpty() && log(p.containerMenu.getCarried()) && total(p)==1 && !p.containerMenu.getSlot(0).mayPlace(p.containerMenu.getCarried())))return;
                    // This state was also true before the attempt: record preservation, never a packet acknowledgement.
                    if(!click(screen,index==0?1:index))return;
                    outputAttemptsPreserved++;enter(Stage.RETURNED);
                }
                case RETURNED -> {
                    int destination=index==0?1:index;
                    if(ticks<8 || !menu.getCarried().isEmpty() || !log(menu.getSlot(destination).getItem())
                        || !matches(mc,p->machine.isEmpty() && p.containerMenu.getCarried().isEmpty() && log(p.containerMenu.getSlot(destination).getItem()) && total(p)==1))return;
                    returns++;routes.add(index);
                    if(++index<37){seed(mc);return;}
                    park(mc);enter(Stage.CAPTURE);
                }
                case CAPTURE -> {
                    if(ticks<5 || !matches(mc,TreeCutterPackagedValidation::finalState))return;
                    capture(mc,screen,"transferred");oldMenu=menu.containerId;mc.player.closeContainer();enter(Stage.CLOSE);
                }
                case REOPEN -> {
                    if(ticks<15 || menu.containerId==oldMenu || !matches(mc,TreeCutterPackagedValidation::finalState))return;
                    capture(mc,screen,"reopened");pending=mc.reloadResourcePacks();enter(Stage.RELOAD);
                }
                case RELOAD -> {
                    if(ticks<30 || !matches(mc,TreeCutterPackagedValidation::finalState))return;
                    packaged=TreeCutterRuntimeProof.verify(mc);capture(mc,screen,"reloaded");finish(mc,null);
                }
                default -> {}
            }
        }catch(Throwable error){finish(mc,error);}
    }
    private static void boot(Minecraft mc) throws Exception {
        nonce = System.getProperty("iedctr.clientGameplayNonce", "");
        require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha", "").matches("[a-f0-9]{40}"), "Missing run identity");
        directory = mc.gameDirectory.toPath().toRealPath();
        require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory", "")).toRealPath())
            && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce), "Isolation marker differs");
        require(mc.level == null && mc.getSingleplayerServer() == null && !Files.exists(directory.resolve("tree-proof.json")), "Existing proof/world forbidden");
        if (Files.exists(directory.resolve("saves"))) try (var entries = Files.list(directory.resolve("saves"))) { require(entries.findAny().isEmpty(), "Existing save"); }
        packaged = TreeCutterRuntimeProof.verify(mc);
        require(Files.readString(directory.resolve(".iedctr-tree-xvfb")).equals(nonce + "\n" + System.getenv("DISPLAY")), "Display is not nonce-owned Xvfb");
        x11 = NativeLibrary.getInstance("X11"); xtest = NativeLibrary.getInstance("Xtst");
        display = x11.getFunction("XOpenDisplay").invokePointer(new Object[]{null}); require(display != null, "X11 unavailable");
        String name = "iedctr-tree-" + nonce;
        mc.options.pauseOnLostFocus = false; enter(Stage.WORLD);
        mc.createWorldOpenFlows().createFreshLevel(name,
            new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(0L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }
    private static void setup(Minecraft mc) {
        var p = player(mc); var level = p.serverLevel(); p.setGameMode(GameType.SURVIVAL);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 5; z++) level.setBlock(new BlockPos(x,79,z), Blocks.STONE.defaultBlockState(), 3);

        level.setBlock(POS, ModBlocks.SMALL_TREE_CUTTER.get().defaultBlockState().setValue(MachineBlocks.HORIZONTAL_FACING, Direction.NORTH), 3);
        require(level.getBlockEntity(POS) instanceof MachineBlockEntity, "Missing Tree Cutter"); machine = (MachineBlockEntity)level.getBlockEntity(POS);
        require(machine.isEmpty() && machine.energyStorage(Direction.DOWN) == null, "Nonempty initial machine");
        p.getInventory().clearContent(); p.connection.teleport(.5,80,3.5,180,24);
    }
    private static ServerPlayer player(Minecraft mc) { var p = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId); require(p != null, "Player missing"); return p; }
    private static boolean matches(Minecraft mc, Predicate<ServerPlayer> test) {
        if (inspection == null) { inspection = mc.getSingleplayerServer().submit(() -> test.test(player(mc))); return false; }
        if (!inspection.isDone()) return false; boolean result = inspection.join(); inspection = null; return result;
    }
    private static void enter(Stage next) { stage = next; ticks = 0; inspection = null; }

    private static void seed(Minecraft mc){
        pending=mc.getSingleplayerServer().submit(()->{
            var p=player(mc);require(p.containerMenu.getCarried().isEmpty(),"Carried item would be discarded");
            require(machine.isEmpty(),"Unexpected machine item would be discarded");
            // Each route is an explicitly independent synthetic setup, never live user data.
            p.getInventory().clearContent();p.containerMenu.getSlot(index).set(new ItemStack(Items.OAK_LOG));
            p.containerMenu.broadcastChanges();require(total(p)==1,"Seed conservation");
        });enter(Stage.SEEDED);
    }
    private static boolean log(ItemStack stack){return stack.is(Items.OAK_LOG) && stack.getCount()==1;}
    private static int total(ServerPlayer p){
        int count=p.containerMenu.getCarried().getCount();
        for(int i=0;i<machine.getContainerSize();i++)count+=machine.getItem(i).getCount();
        for(int i=0;i<p.getInventory().getContainerSize();i++)count+=p.getInventory().getItem(i).getCount();
        require(p.serverLevel().getEntitiesOfClass(ItemEntity.class,new AABB(POS).inflate(12)).isEmpty(),"Unexpected world item drop");
        return count;
    }
    private static boolean finalState(ServerPlayer p){
        return machine.isEmpty() && p.containerMenu.getCarried().isEmpty() && total(p)==1 && log(p.getInventory().getItem(8));
    }
    private static void geometry(Minecraft mc,TreeCutterV3ReferenceScreen screen,MachineMenu menu){
        var w=mc.getWindow();int scale=Integer.getInteger("iedctr.requestedGuiScale",0);
        require(w.getGuiScale()==scale && mc.options.guiScale().get()==scale && scale>=1 && scale<=3,"Actual GUI scale differs");
        require(w.getWidth()==Integer.getInteger("iedctr.requestedWidth",0) && w.getHeight()==Integer.getInteger("iedctr.requestedHeight",0),"Framebuffer differs");
        require(menu.treeCutterV3ReferencePresentation() && menu.slots.size()==37 && menu.visibleMachineSlots()==1,"37 target topology differs");
        var v=screen.validationViewTransform();
        require(screen.getGuiLeft()==v.guiLeft() && screen.getGuiTop()==v.guiTop() && screen.getXSize()==v.guiWidth() && screen.getYSize()==v.guiHeight(),"Public bounds differ");
        for(var a:TreeCutterV3ReferenceGeometry.SLOT_ANCHORS){
            var slot=menu.getSlot(a.menuIndex());int backing=a.menuIndex()==0?0:a.menuIndex()<28?a.menuIndex()+8:a.menuIndex()-28;
            require(slot.getSlotIndex()==backing && slot.x==a.x() && slot.y==a.y(),"Backing mapping differs");
            if(a.menuIndex()>0)require(slot.container==mc.player.getInventory(),"Player destination not player inventory");
            double x=v.screenX(a.x()+8),y=v.screenY(a.y()+8);
            require(screen.validationSlotHoveredAt(a.menuIndex(),x,y),"Transformed target centre misses");
            require(!screen.validationSlotHoveredAt(a.menuIndex(),v.screenX(a.hoverLeft()-.01),y)
                && !screen.validationSlotHoveredAt(a.menuIndex(),v.screenX(a.hoverRight()+.01),y)
                && !screen.validationSlotHoveredAt(a.menuIndex(),x,v.screenY(a.hoverTop()-.01))
                && !screen.validationSlotHoveredAt(a.menuIndex(),x,v.screenY(a.hoverBottom()+.01)),"Target boundary escape");
        }
    }
    /** Move through X11, wait for GLFW's observed cursor, then send one physical click. */
    private static boolean click(TreeCutterV3ReferenceScreen screen,int target){
        var mc=Minecraft.getInstance();long window=mc.getWindow().getWindow();
        if(GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED)!=GLFW.GLFW_TRUE){
            GLFW.glfwFocusWindow(window);return false;
        }
        var a=TreeCutterV3ReferenceGeometry.anchor(target);var v=screen.validationViewTransform();
        double x=v.screenX(a.x()+8),y=v.screenY(a.y()+8);
        double observedX=mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth();
        double observedY=mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight();
        if(Math.abs(observedX-x)>1 || Math.abs(observedY-y)>1){
            int[] wx={0},wy={0};GLFW.glfwGetWindowPos(window,wx,wy);
            int px=wx[0]+(int)Math.round(x*mc.getWindow().getScreenWidth()/screen.width);
            int py=wy[0]+(int)Math.round(y*mc.getWindow().getScreenHeight()/screen.height);
            require(xtest.getFunction("XTestFakeMotionEvent").invokeInt(new Object[]{display,-1,px,py,new NativeLong(0)})!=0,"Physical slot motion failed");
            x11.getFunction("XFlush").invokeInt(new Object[]{display});return false;
        }
        require(screen.validationSlotHoveredAt(target,observedX,observedY),"Physical cursor misses transformed slot");
        nativeClick(1);physicalSlotClicks++;lastClick=System.nanoTime();return true;
    }
    private static void nativeClick(int button){
        require(xtest.getFunction("XTestFakeButtonEvent").invokeInt(new Object[]{display,button,1,new NativeLong(0)})!=0,"Physical press failed");
        require(xtest.getFunction("XTestFakeButtonEvent").invokeInt(new Object[]{display,button,0,new NativeLong(0)})!=0,"Physical release failed");
        x11.getFunction("XFlush").invokeInt(new Object[]{display});
    }
    private static void park(Minecraft mc){GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),mc.getWindow().getScreenWidth()-2,mc.getWindow().getScreenHeight()-2);}
    private static void capture(Minecraft mc,TreeCutterV3ReferenceScreen screen,String name)throws Exception{
        var v=screen.validationViewTransform();double scale=mc.getWindow().getGuiScale();
        // Crop strictly INSIDE the GUI rectangle. Never write a full framebuffer/world/HUD to disk.
        int x=(int)Math.ceil(v.screenX(0)*scale),y=(int)Math.ceil(v.screenY(0)*scale);
        int width=(int)Math.floor(v.screenX(418)*scale)-x,height=(int)Math.floor(v.screenY(418)*scale)-y;
        Path file=directory.resolve("tree-"+name+".png");require(!Files.exists(file),"Stale capture");
        try(var full=Screenshot.takeScreenshot(mc.getMainRenderTarget());var crop=new NativeImage(width,height,false)){
            for(int row=0;row<height;row++)for(int col=0;col<width;col++)crop.setPixelRGBA(col,row,full.getPixelRGBA(x+col,y+row));
            crop.writeToFile(file);
        }
        var c=new JsonObject();c.addProperty("name",name);c.addProperty("file",file.getFileName().toString());
        c.addProperty("sha256",TreeCutterRuntimeProof.hash(Files.readAllBytes(file)));c.addProperty("x",x);c.addProperty("y",y);
        c.addProperty("width",width);c.addProperty("height",height);c.addProperty("menu_id",mc.player.containerMenu.containerId);
        c.addProperty("screen_class",screen.getClass().getName());captures.add(c);
    }
    private static void finish(Minecraft mc,Throwable error){
        complete=true;if(display!=null){x11.getFunction("XCloseDisplay").invokeInt(new Object[]{display});display=null;}
        try{if(directory!=null){
            var p=new JsonObject();p.addProperty("suite","native-tree-cutter-v3-v2");p.addProperty("status",error==null?"passed_bounded_smoke":"failed");
            p.addProperty("nonce",nonce);p.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha"));p.add("packaged_runtime",packaged);
            p.addProperty("selected_language",mc.getLanguageManager().getSelected());p.addProperty("width",mc.getWindow().getWidth());p.addProperty("height",mc.getWindow().getHeight());
            p.addProperty("gui_scale",mc.getWindow().getGuiScale());p.addProperty("menu_slot_count",37);p.addProperty("native_visual_acceptance",false);
            p.addProperty("opened_via_native_block_interaction",opened);p.addProperty("pickup_server_acks",pickups);p.addProperty("return_server_acks",returns);p.addProperty("rejected_output_attempts_state_preserved",outputAttemptsPreserved);
            p.addProperty("output_packet_processing_observed",false);
            p.addProperty("physical_slot_clicks",physicalSlotClicks);
            p.addProperty("final_item_total",error==null?1:-1);p.addProperty("resource_reload_confirmed",error==null && stage==Stage.RELOAD);
            p.addProperty("input_method","XTest physical block opening and slot clicks; observed GLFW cursor and server inventory transitions");
            p.addProperty("setup","Fresh nonce-owned world. Independently seeded one oak log per route. No tree-felling, disk reload, migration or tooltip qualification claimed.");
            p.add("slot_routes",routes);p.add("captures",captures);if(error!=null)p.addProperty("error",error.toString());
            Files.writeString(directory.resolve("tree-proof.json"),new GsonBuilder().setPrettyPrinting().create().toJson(p)+"\n");
        }}catch(Exception ex){EngineersDecorReforged.LOGGER.error("Tree proof write failed",ex);}
        if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_TREE_PACKAGED PASSED nonce={}",nonce);
        else EngineersDecorReforged.LOGGER.error("IEDCTR_TREE_PACKAGED FAILED at "+stage,error);
        mc.stop();
    }
    private static void require(boolean ok,String reason){TreeCutterRuntimeProof.require(ok,reason);}
}
