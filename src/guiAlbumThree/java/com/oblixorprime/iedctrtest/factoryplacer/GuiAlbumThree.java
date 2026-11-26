package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.*;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Test-only native album and bounded Electrical Furnace interaction evidence; no human visual acceptance. */
@EventBusSubscriber(modid=FactoryPlacerValidationMod.MOD_ID, value=Dist.CLIENT)
public final class GuiAlbumThree {
    private static final String[] NAMES={"factory-hopper","metal-crafting-table","small-electrical-furnace","small-waste-incinerator","labeled-crate","factory-dropper","small-lab-furnace"};
    private static final String[] SCREEN_CLASSES={
        "com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen",
        "com.oblixorprime.engineersdecorreforged.client.MachineScreen$MetalCraftingTableScreen",
        "com.oblixorprime.engineersdecorreforged.client.MachineScreen$SmallElectricalFurnaceScreen",
        "com.oblixorprime.engineersdecorreforged.client.MachineScreen$SmallWasteIncineratorScreen",
        "com.oblixorprime.engineersdecorreforged.client.LabeledCrateScreen",
        "com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen",
        "com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen"
    };
    private static final int[] EXPECTED_MACHINE_SLOTS={18,0,0,0,0,15,11};
    private static final int[] EXPECTED_TOTAL_SLOTS={54,0,0,0,0,51,47};
    // Slot positions and occupied stacks are distinct facts: the Lab Furnace
    // menu has 47 positions, while this fixture presents 5 machine stacks plus
    // the 36 player inventory stacks (41 total).
    private static final int[] EXPECTED_OCCUPIED_SLOTS={54,0,0,0,0,51,41};
    private static final int[] EXPECTED_OCCUPIED_MACHINE_SLOTS={18,0,0,0,0,15,5};
    private static final String[] EXPECTED_KINDS={"factory_hopper","","","","","factory_dropper","small_lab_furnace"};
    private enum Stage {BOOT,WORLD,OPEN,WAIT,ELECTRICAL,CLOSE}
    private static Stage stage=Stage.BOOT;
    private static boolean complete;
    private static final int POINTER_TARGET=8, POINTER_PRIME=12;
    private static int pointerPhase,lastMotionTick=-1;
    private static NativeLibrary x11,xtest;
    private static Pointer display;
    private static String waitReason="not waiting";
    private static final JsonArray waitDiagnostics=new JsonArray();
    private static long deadline;
    private static int ticks,index,selectionRow,selectionRenderFrames;
    private static UUID playerId;
    private static Path directory;
    private static String nonce;
    private static JsonObject packaged;
    private static final JsonArray cases=new JsonArray();
    private static final JsonArray selections=new JsonArray();
    private static final MachineBlockEntity[] machines=new MachineBlockEntity[NAMES.length];
    private static CompletableFuture<?> pending;
    private static CompletableFuture<Boolean> inspection;
    private static ElectricalFurnaceAlbumValidation electricalValidation;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("iedctr.guiAlbumThree")||complete)return;
        var mc=Minecraft.getInstance();
        try {
            if(deadline==0)deadline=System.nanoTime()+900_000_000_000L;
            require(System.nanoTime()<deadline&&++ticks<1800,"Timeout at "+stage+" reason="+waitReason+" state="+diagnostic(mc));
            if(stage==Stage.BOOT){if(mc.screen instanceof TitleScreen&&mc.getOverlay()==null)boot(mc);return;}
            if(stage==Stage.WORLD){
                if(mc.level==null||mc.player==null||mc.gameMode==null||mc.screen!=null)return;
                require(mc.getSingleplayerServer()!=null&&mc.player.connection.getConnection().isMemoryConnection(),"Isolated integrated world required");
                playerId=mc.player.getUUID();pending=mc.getSingleplayerServer().submit(()->setup(mc));enter(Stage.OPEN);return;
            }
            require(mc.level!=null&&mc.player!=null&&mc.getSingleplayerServer()!=null,"World disconnected");
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(mc.getOverlay()!=null)return;
            switch(stage){
                case OPEN -> {
                    pending=mc.getSingleplayerServer().submit(()->{
                        var p=player(mc); var pos=machines[index].getBlockPos();
                        // Every case must remain within the native menu stillValid radius.
                        p.connection.teleport(pos.getX()+.5,pos.getY(),pos.getZ()+2.5,180,0);
                        p.openMenu(machines[index]);
                    });
                    enter(Stage.WAIT);
                }
                case WAIT -> {
                    if(mc.screen==null){waiting(mc,"screen not opened");return;}
                    if(GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_FOCUSED)!=GLFW.GLFW_TRUE){
                        waiting(mc,"window not focused");GLFW.glfwFocusWindow(mc.getWindow().getWindow());return;
                    }
                    if(!pointerReady(mc)){selectionRenderFrames=0;return;}
                    require(index==4 || mc.player.containerMenu instanceof MachineMenu,"Unexpected menu while positioning pointer");
                    var containerScreen=(AbstractContainerScreen<?>)mc.screen;
                    require(mc.mouseHandler.xpos()*mc.screen.width/mc.getWindow().getScreenWidth()<containerScreen.getGuiLeft()
                        && mc.mouseHandler.ypos()*mc.screen.height/mc.getWindow().getScreenHeight()<containerScreen.getGuiTop(),
                        "Pointer target must remain outside panel");
                    if(ticks<(index==0?650:35)){waiting(mc,"render warmup");return;}
                    require(mc.screen.getClass().getName().equals(SCREEN_CLASSES[index]),"Wrong machine screen");
                    require(mc.screen instanceof AbstractContainerScreen<?> && (index==4 || mc.player.containerMenu instanceof MachineMenu),"Missing actual container");
                    MachineMenu activeMenu=index==4?null:(MachineMenu)mc.player.containerMenu;
                    if(EXPECTED_MACHINE_SLOTS[index]>0){
                        require(activeMenu.factoryStudyPresentation()
                            && activeMenu.kind().registryName().equals(EXPECTED_KINDS[index])
                            && activeMenu.visibleMachineSlots()==EXPECTED_MACHINE_SLOTS[index]
                            && activeMenu.slots.size()==EXPECTED_TOTAL_SLOTS[index],
                            "Factory Study V3 menu slot layout mismatch for "+NAMES[index]);
                    }
                    if(index==0 && ((MachineMenu)mc.player.containerMenu).field(6)!=selectionRow*6){waiting(mc,"client selection not synchronized");return;}
                    if(index==0 && selectionRenderFrames<3){waiting(mc,"selection render frames missing");return;}
                    if(inspection==null){
                        final int caseIndex=index;
                        final Class<?> clientMenuClass=mc.player.containerMenu.getClass();
                        final int clientMenuId=mc.player.containerMenu.containerId;
                        inspection=mc.getSingleplayerServer().submit(()->{
                            var serverMenu=player(mc).containerMenu;
                            if(serverMenu.getClass()!=clientMenuClass||serverMenu.containerId!=clientMenuId)return false;
                            if(caseIndex==0&&(!(serverMenu instanceof MachineMenu menu)||menu.field(6)!=selectionRow*6))return false;
                            if(EXPECTED_MACHINE_SLOTS[caseIndex]>0){
                                if(!(serverMenu instanceof MachineMenu menu)
                                    ||!menu.kind().registryName().equals(EXPECTED_KINDS[caseIndex])
                                    ||menu.visibleMachineSlots()!=EXPECTED_MACHINE_SLOTS[caseIndex]
                                    ||menu.slots.size()!=EXPECTED_TOTAL_SLOTS[caseIndex])return false;
                                if(menu.slots.stream().filter(slot->slot.hasItem()).count()!=EXPECTED_OCCUPIED_SLOTS[caseIndex])return false;
                                long occupiedMachine=menu.slots.subList(0,menu.visibleMachineSlots()).stream().filter(slot->slot.hasItem()).count();
                                if(occupiedMachine!=EXPECTED_OCCUPIED_MACHINE_SLOTS[caseIndex])return false;
                            }
                            return true;
                        });
                        return;
                    }
                    if(!inspection.isDone()){waiting(mc,"server inspection pending");return;}
                    require(inspection.join(),"Server menu mismatch");
                    var obs=new JsonObject();obs.addProperty("screen",mc.screen.getClass().getName());obs.addProperty("case",NAMES[index]);obs.addProperty("server_menu_acknowledged",true);
                    obs.addProperty("gui_scale",mc.getWindow().getGuiScale());
                    long occupied=mc.player.containerMenu.slots.stream().filter(slot->slot.hasItem()).count();
                    if(EXPECTED_MACHINE_SLOTS[index]>0)
                        require(occupied==EXPECTED_OCCUPIED_SLOTS[index],"Factory Study V3 occupied-stack fixture changed during capture");
                    obs.addProperty("visible_nonempty_slots",occupied);
                    if(activeMenu!=null){
                        long occupiedMachine=activeMenu.slots.subList(0,activeMenu.visibleMachineSlots()).stream().filter(slot->slot.hasItem()).count();
                        if(EXPECTED_MACHINE_SLOTS[index]>0)
                            require(occupiedMachine==EXPECTED_OCCUPIED_MACHINE_SLOTS[index],"Factory Study V3 machine-stack fixture changed during capture");
                        obs.addProperty("visible_machine_slots",activeMenu.visibleMachineSlots());
                        obs.addProperty("total_menu_slots",activeMenu.slots.size());
                        obs.addProperty("visible_nonempty_machine_slots",occupiedMachine);
                        obs.addProperty("machine_kind",activeMenu.kind().registryName());
                        if(index==5){
                            int filterGate=activeMenu.field(5);
                            boolean powered=mc.level.hasNeighborSignal(machines[5].getBlockPos());
                            require(filterGate==2,"Dropper capture fixture must use the AND filter gate");
                            require(!powered,"Dropper capture fixture must remain unpowered");
                            obs.addProperty("dropper_filter_gate",filterGate);
                            obs.addProperty("dropper_external_powered",powered);
                        }
                    }
                    var production=ModList.get().getModFileById(EngineersDecorReforged.MOD_ID).getFile();
                    var entry=mc.screen.getClass().getName().replace('.', '/')+".class";
                    var resource=mc.screen.getClass().getResource("/"+entry);
                    require(resource!=null&&resource.toURI().equals(production.findResource(entry).toUri()),"Screen not from production JAR");
                    try(var input=resource.openStream();var zip=new java.util.zip.ZipFile(production.getFilePath().toFile());var archived=zip.getInputStream(zip.getEntry(entry))){
                        String hash=PackagedRuntimeProof.hash(input.readAllBytes());require(hash.equals(PackagedRuntimeProof.hash(archived.readAllBytes())),"Screen bytes differ");obs.addProperty("screen_class_sha256",hash);
                    }
                    if(index==0){
                        int selected=((MachineMenu)mc.player.containerMenu).field(6);
                        var slot=mc.player.containerMenu.slots.get(selected);
                        require(slot.x==34 && slot.y==68+selectionRow*25,"Unexpected Factory Study V3 Hopper slot geometry");
                        obs.addProperty("selection_render_frames",selectionRenderFrames);
                        obs.addProperty("server_selection",selectionRow*6);obs.addProperty("client_selection",selected);
                        obs.addProperty("ring_x",slot.x-1);obs.addProperty("ring_y",slot.y-1);
                        obs.addProperty("selection_source","server-seeded ContainerData; not user selection");
                    }
                    var imageName=NAMES[index]+(index==0 && selectionRow>0?"-selection-row-"+selectionRow:"")+".png";
                    try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(directory.resolve(imageName));}
                    obs.addProperty("capture",imageName);obs.addProperty("capture_sha256",PackagedRuntimeProof.hash(Files.readAllBytes(directory.resolve(imageName))));
                    if(index==0){
                        selections.add(obs.deepCopy());
                        if(selectionRow==0)cases.add(obs);
                        if(selectionRow<2){
                            selectionRow++;
                            pending=mc.getSingleplayerServer().submit(()->{machines[0].dataAccessForTests().set(6,selectionRow*6);player(mc).containerMenu.broadcastChanges();});
                            enter(Stage.WAIT);return;
                        }
                    }else cases.add(obs);
                    if(index==2){
                        electricalValidation=new ElectricalFurnaceAlbumValidation(directory,playerId,machines[2],x11,xtest,display);
                        enter(Stage.ELECTRICAL);return;
                    }
                    mc.screen.onClose();enter(Stage.CLOSE);
                }
                case ELECTRICAL -> {
                    if(electricalValidation.tick(mc)){mc.screen.onClose();enter(Stage.CLOSE);}
                }
                case CLOSE -> {if(mc.screen!=null||ticks<20)return;index++;if(index==NAMES.length)finish(mc,null);else enter(Stage.OPEN);}
                default -> throw new IllegalStateException("Unexpected "+stage);
            }
        }catch(Throwable error){finish(mc,error);}
    }
    @SubscribeEvent public static void rendered(ScreenEvent.Render.Post event){
        if(Boolean.getBoolean("iedctr.guiAlbumThree")&&!complete&&stage==Stage.ELECTRICAL){
            electricalValidation.rendered(event);return;
        }
        if(!Boolean.getBoolean("iedctr.guiAlbumThree") || complete || stage!=Stage.WAIT || index!=0)return;
        var mc=Minecraft.getInstance();
        if(event.getScreen()==mc.screen && mc.player!=null && mc.player.containerMenu instanceof MachineMenu menu
            && menu.field(6)==selectionRow*6)selectionRenderFrames++;
        else selectionRenderFrames=0;
    }
    @SubscribeEvent public static void mousePressed(ScreenEvent.MouseButtonPressed.Post event){
        if(Boolean.getBoolean("iedctr.guiAlbumThree")&&!complete&&stage==Stage.ELECTRICAL){
            try{electricalValidation.mousePressed(event);}catch(Throwable error){finish(Minecraft.getInstance(),error);}
        }
    }
    private static boolean pointerReady(Minecraft mc){
        // Opening the screen can discard the first cursor motion. Force two
        // distinct OS motions AFTER opening, separated by client ticks.
        if(pointerPhase==0){
            waiting(mc,"pointer priming motion");positionPointer(mc,POINTER_PRIME);
            pointerPhase=1;lastMotionTick=ticks;return false;
        }
        if(ticks<=lastMotionTick)return false;
        if(pointerPhase==1){
            double[] x={0},y={0};GLFW.glfwGetCursorPos(mc.getWindow().getWindow(),x,y);
            if(Math.abs(x[0]-POINTER_PRIME)>1 || Math.abs(y[0]-POINTER_PRIME)>1){
                waiting(mc,"pointer priming event pending");return false;
            }
            waiting(mc,"pointer final motion");positionPointer(mc,POINTER_TARGET);
            pointerPhase=2;lastMotionTick=ticks;return false;
        }
        if(Math.abs(mc.mouseHandler.xpos()-POINTER_TARGET)>1
            || Math.abs(mc.mouseHandler.ypos()-POINTER_TARGET)>1){
            // A repeated move to the same OS coordinate may emit no event.
            // Retry through the distinct prime point, never overwrite MouseHandler.
            waiting(mc,"pointer callback not at target");pointerPhase=0;return false;
        }
        return true;
    }
    private static void positionPointer(Minecraft mc,int target){
        long window=mc.getWindow().getWindow();
        if(GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED)!=GLFW.GLFW_TRUE){GLFW.glfwFocusWindow(window);return;}
        // Window-local interior target: unlike width-2,height-2, not beyond Xvfb's
        // desktop when the client window has decorations or a nonzero origin.
        int[] wx={0},wy={0};GLFW.glfwGetWindowPos(window,wx,wy);
        require(xtest.getFunction("XTestFakeMotionEvent").invokeInt(new Object[]{display,-1,wx[0]+target,wy[0]+target,new NativeLong(0)})!=0,
            "Synthetic pointer motion failed");
        x11.getFunction("XFlush").invokeInt(new Object[]{display});
    }
    private static JsonObject diagnostic(Minecraft mc){
        var d=new JsonObject();d.addProperty("reason",waitReason);d.addProperty("stage",stage.name());
        d.addProperty("ticks",ticks);d.addProperty("case_index",index);d.addProperty("selection_row",selectionRow);
        d.addProperty("selection_render_frames",selectionRenderFrames);d.addProperty("pointer_target",POINTER_TARGET);
        d.addProperty("pointer_phase",pointerPhase);d.addProperty("last_motion_tick",lastMotionTick);
        d.addProperty("mouse_x",mc.mouseHandler.xpos());d.addProperty("mouse_y",mc.mouseHandler.ypos());
        d.addProperty("screen_width",mc.getWindow().getScreenWidth());d.addProperty("screen_height",mc.getWindow().getScreenHeight());
        d.addProperty("framebuffer_width",mc.getWindow().getWidth());d.addProperty("framebuffer_height",mc.getWindow().getHeight());
        d.addProperty("focused",GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_FOCUSED)==GLFW.GLFW_TRUE);
        int[] wx={0},wy={0};GLFW.glfwGetWindowPos(mc.getWindow().getWindow(),wx,wy);
        d.addProperty("window_x",wx[0]);d.addProperty("window_y",wy[0]);
        double[] gx={0},gy={0};GLFW.glfwGetCursorPos(mc.getWindow().getWindow(),gx,gy);
        d.addProperty("glfw_cursor_x",gx[0]);d.addProperty("glfw_cursor_y",gy[0]);
        d.addProperty("screen",mc.screen==null?"none":mc.screen.getClass().getName());
        if(mc.player!=null && mc.player.containerMenu instanceof MachineMenu menu){
            d.addProperty("client_selection",menu.field(6));d.addProperty("menu_id",menu.containerId);
            d.addProperty("occupied_slots",menu.slots.stream().filter(slot->slot.hasItem()).count());
        }
        return d;
    }
    private static void waiting(Minecraft mc,String reason){
        boolean changed=!reason.equals(waitReason);waitReason=reason;
        if(changed || ticks%100==0){var d=diagnostic(mc);waitDiagnostics.add(d);
            EngineersDecorReforged.LOGGER.info("IEDCTR_GUI_ALBUM_WAIT {}",d);}
    }
    private static void boot(Minecraft mc) throws Exception {
        nonce=System.getProperty("iedctr.clientGameplayNonce","");
        require(nonce.matches("[a-f0-9]{32}")&&System.getProperty("iedctr.executedSourceSha","").matches("[a-f0-9]{40}"),"Missing identity");
        directory=mc.gameDirectory.toPath().toRealPath();
        require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory","")).toRealPath())&&Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce),"Isolation marker mismatch");
        require(Files.readString(directory.resolve(".iedctr-gui-album-three-xvfb")).equals(nonce+"\n"+System.getenv("DISPLAY")),"Display not owned");
        require(mc.level==null&&mc.getSingleplayerServer()==null&&!Files.exists(directory.resolve("gui-album-three-proof.json")),"Existing world/proof");
        if(Files.exists(directory.resolve("saves")))try(var entries=Files.list(directory.resolve("saves"))){require(entries.findAny().isEmpty(),"Existing saves forbidden");}
        packaged=PackagedRuntimeProof.verify(mc);
        x11=NativeLibrary.getInstance("X11");xtest=NativeLibrary.getInstance("Xtst");
        display=x11.getFunction("XOpenDisplay").invokePointer(new Object[]{null});
        require(display!=null,"X11 unavailable");
        String name="iedctr-gui-album-three-"+nonce;mc.options.pauseOnLostFocus=false;enter(Stage.WORLD);
        mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(0L,false,false),registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
    }
    private static void setup(Minecraft mc){
        var p=player(mc);p.setGameMode(GameType.SURVIVAL);var level=p.serverLevel();
        for(int x=-2;x<=12;x++)for(int z=-1;z<=4;z++)level.setBlock(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState(),3);
        p.connection.teleport(.5,80,2.5,180,0);
        for(int slot=0;slot<36;slot++)p.getInventory().setItem(slot,new ItemStack(slot%2==0?Items.IRON_INGOT:Items.REDSTONE,slot+1));
        p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,64));
        p.getInventory().setItem(1,new ItemStack(Items.REDSTONE,8));
        p.getInventory().setItem(9,new ItemStack(Items.COBBLESTONE,64));
        var blocks=List.of(ModBlocks.FACTORY_HOPPER.get(),ModBlocks.METAL_CRAFTING_TABLE.get(),ModBlocks.SMALL_ELECTRICAL_FURNACE.get(),ModBlocks.SMALL_WASTE_INCINERATOR.get(),ModBlocks.LABELED_CRATE.get(),ModBlocks.FACTORY_DROPPER.get(),ModBlocks.SMALL_LAB_FURNACE.get());
        for(int i=0;i<NAMES.length;i++){var pos=new BlockPos(i*2,80,0);level.setBlock(pos,blocks.get(i).defaultBlockState(),3);machines[i]=(MachineBlockEntity)level.getBlockEntity(pos);require(machines[i]!=null,"Missing machine "+i);}
        // Disable continuous transfer while recording deterministic, filled slots.
        machines[0].dataAccessForTests().set(2,0);
        // The capture fixture fills matching input and filter stacks. Keep the
        // production AND gate and leave redstone disconnected so it cannot
        // trigger real Dropper transfers during the screenshot.
        machines[5].dataAccessForTests().set(5,2);
        for(int slot=0;slot<18;slot++)machines[0].setItem(slot,new ItemStack(Items.COBBLESTONE,slot+1));
        for(int slot=0;slot<12;slot++)machines[5].setItem(slot,new ItemStack(Items.COBBLESTONE,slot+1));
        for(int slot=12;slot<15;slot++)machines[5].setItem(slot,new ItemStack(Items.COBBLESTONE,1));
        var heater=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering","furnace_heater"));
        require(heater!=Items.AIR,"Immersive Engineering furnace heater item is required for Lab Furnace fixture");
        for(int slot=0;slot<9;slot++)machines[6].setItem(slot,new ItemStack(Items.DIAMOND,1));
        machines[6].setItem(9,new ItemStack(heater));
        machines[6].setItem(10,new ItemStack(heater));
        machines[0].dataAccessForTests().set(6,0);
        machines[0].setItem(0,new ItemStack(Items.COBBLESTONE,64));
        machines[5].setItem(0,new ItemStack(Items.COBBLESTONE,64));
    }
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);require(p!=null,"Player missing");return p;}
    private static void enter(Stage next){stage=next;ticks=0;inspection=null;selectionRenderFrames=0;pointerPhase=0;lastMotionTick=-1;}
    private static void finish(Minecraft mc,Throwable error){
        complete=true;
        if(display!=null){x11.getFunction("XCloseDisplay").invokeInt(new Object[]{display});display=null;}
        try{if(directory!=null){var proof=new JsonObject();proof.addProperty("suite","gui-album-three-v3");proof.addProperty("status",error==null?"observed":"failed");proof.addProperty("nonce",nonce);proof.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha"));proof.add("packaged_runtime",packaged);proof.add("cases",cases);proof.add("hopper_selections",selections);proof.add("wait_diagnostics",waitDiagnostics);proof.add("terminal_state",diagnostic(mc));proof.addProperty("visual_acceptance",false);proof.addProperty("physical_os_input",true);proof.addProperty("human_input",false);proof.addProperty("input_method",ElectricalFurnaceAlbumValidation.INPUT_METHOD);proof.addProperty("synthetic_button_events",electricalValidation==null?0:electricalValidation.syntheticButtonEvents());if(electricalValidation!=null)proof.add("electrical_controls",electricalValidation.proof());if(error!=null)proof.addProperty("error",error.toString());Files.writeString(directory.resolve("gui-album-three-proof.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");}}
        catch(Exception e){EngineersDecorReforged.LOGGER.error("Album proof error",e);}
        if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_GUI_ALBUM_THREE OBSERVED nonce={}",nonce);else EngineersDecorReforged.LOGGER.error("IEDCTR_GUI_ALBUM_THREE FAILED "+stage,error);mc.stop();
    }
    private static void require(boolean b,String message){PackagedRuntimeProof.require(b,message);}
}
