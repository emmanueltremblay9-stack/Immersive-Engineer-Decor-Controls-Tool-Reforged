package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.client.MachineScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/** Separate-harness interaction evidence: no direct screen clicks or production action calls. */
final class ElectricalFurnaceAlbumValidation {
    static final String INPUT_METHOD = "synthetic XTest pointer and Electrical Furnace button events through GLFW; server-opened menus; no human input";
    // Independent acceptance coordinates from the retained 1199x1312 artwork,
    // not imported from the production rendering or input implementation.
    private static final int DIAL_X=184, DIAL_Y=88, BORDER_X=208, BORDER_Y=88;
    private static final String[] NAMES={"empty-off","left-one","left-two","left-three","left-wrap",
        "right-wrap","rapid-left-three","middle-inert","border-inert","reopened","half-energy","full-energy"};
    private static final int[] SPEEDS={0,1,2,3,0,3,2,2,2,2,2,2};
    private static final int[] ENERGY={0,0,0,0,0,0,0,0,0,0,16000,32000};
    private static final int[] BUTTONS={0,1,1,1,1,3,1,2,1,0,0,0}; // X11: left=1, middle=2, right=3
    private static final int[] CLICKS={0,1,1,1,1,1,3,1,1,0,0,0};
    private enum Phase { PREPARE, REOPEN_WAIT, POSITION, ACKNOWLEDGE, PARK, CAPTURE, DONE }
    private Phase phase=Phase.PREPARE;
    private final Path directory;
    private final UUID playerId;
    private final MachineBlockEntity machine;
    private final NativeLibrary x11,xtest;
    private final Pointer display;
    private final JsonArray observations=new JsonArray();
    private final JsonArray inputEvents=new JsonArray();
    private int step,ticks,sentPresses,observedPresses,pressesBefore,stableTicks,renderFrames,initialMenuId;
    private int parkedPhase,lastMotionTick=-1;
    private boolean recordingInput;
    private CompletableFuture<?> pending;
    private CompletableFuture<JsonObject> inspection;
    private JsonObject serverEvidence;

    ElectricalFurnaceAlbumValidation(Path directory,UUID playerId,MachineBlockEntity machine,
                                    NativeLibrary x11,NativeLibrary xtest,Pointer display) {
        this.directory=directory;this.playerId=playerId;this.machine=machine;
        this.x11=x11;this.xtest=xtest;this.display=display;
    }

    boolean tick(Minecraft mc) throws Exception {
        require(++ticks<600,"Electrical validation timeout: "+NAMES[step]+"/"+phase);
        if(pending!=null){if(!pending.isDone())return false;pending.join();pending=null;}
        if(GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_FOCUSED)!=GLFW.GLFW_TRUE){
            GLFW.glfwFocusWindow(mc.getWindow().getWindow());return false;
        }
        switch(phase) {
            case PREPARE -> {
                pressesBefore=observedPresses;stableTicks=0;renderFrames=0;parkedPhase=0;
                if(step==0){
                    initialMenuId=menu(mc).containerId;
                    pending=mc.getSingleplayerServer().submit(()->{
                        require(machine.isEmpty(),"Electrical fixture must have no recipe or energy consumption");
                        // Seed only the starting state. All subsequent speed changes must arrive from native clicks.
                        machine.dataAccessForTests().set(4,0);
                        machine.dataAccessForTests().set(1,0);
                        player(mc).containerMenu.broadcastChanges();
                    });
                    phase=Phase.ACKNOWLEDGE;
                }else if(step==9){
                    mc.screen.onClose();phase=Phase.REOPEN_WAIT;
                }else if(step>=10){
                    int energy=ENERGY[step];
                    pending=mc.getSingleplayerServer().submit(()->{
                        machine.dataAccessForTests().set(1,energy);
                        player(mc).containerMenu.broadcastChanges();
                    });
                    phase=Phase.ACKNOWLEDGE;
                }else phase=Phase.POSITION;
            }
            case REOPEN_WAIT -> {
                if(mc.screen!=null)return false;
                if(inspection==null){
                    inspection=mc.getSingleplayerServer().submit(()->{
                        var p=player(mc);var result=new JsonObject();
                        result.addProperty("closed",p.containerMenu==p.inventoryMenu);
                        result.addProperty("persisted_speed",machine.dataAccessForTests().get(4));
                        return result;
                    });return false;
                }
                if(!inspection.isDone())return false;
                var result=inspection.join();inspection=null;
                if(!result.get("closed").getAsBoolean())return false;
                require(result.get("persisted_speed").getAsInt()==2,"Speed changed while menu was closed");
                pending=mc.getSingleplayerServer().submit(()->player(mc).openMenu(machine));
                phase=Phase.ACKNOWLEDGE;
            }
            case POSITION -> {
                int x=step==8?BORDER_X:DIAL_X;
                if(!pointerAtGui(mc,x,DIAL_Y)){moveToGui(mc,x,DIAL_Y);return false;}
                require(observedPresses==sentPresses,"Prior native click is still pending");
                recordingInput=true;
                // Queue every pair before one XFlush. No synchronized client-state read is used between burst clicks.
                for(int i=0;i<CLICKS[step];i++){
                    nativeButton(BUTTONS[step],true);nativeButton(BUTTONS[step],false);sentPresses++;
                }
                x11.getFunction("XFlush").invokeInt(new Object[]{display});
                phase=Phase.ACKNOWLEDGE;
            }
            case ACKNOWLEDGE -> {
                if(!(mc.screen instanceof MachineScreen.SmallElectricalFurnaceScreen)
                    ||!(mc.player.containerMenu instanceof MachineMenu active))return false;
                if(observedPresses-pressesBefore!=CLICKS[step])return false;
                recordingInput=false;
                if(active.field(4)!=SPEEDS[step]||active.field(1)!=ENERGY[step]||active.field(5)!=32000){stableTicks=0;return false;}
                require(active.getCarried().isEmpty(),"Control input moved an item to the cursor");
                if(inspection==null){
                    int menuId=active.containerId;
                    inspection=mc.getSingleplayerServer().submit(()->inspectServer(mc,menuId));return false;
                }
                if(!inspection.isDone())return false;
                var result=inspection.join();inspection=null;
                if(!result.get("matching_menu").getAsBoolean()
                    ||result.get("speed").getAsInt()!=SPEEDS[step]
                    ||result.get("energy").getAsInt()!=ENERGY[step]){stableTicks=0;return false;}
                require(result.get("capacity").getAsInt()==32000,"Electrical capacity differs");
                require(result.get("machine_speed").getAsInt()==SPEEDS[step],"Server menu differs from machine state");
                require(result.get("cursor_empty").getAsBoolean()&&result.get("machine_empty").getAsBoolean(),"Electrical controls changed inventory");
                // Multiple fresh server snapshots ensure inert clicks and queued burst packets have settled.
                if(++stableTicks<5)return false;
                serverEvidence=result;
                require(step<9?active.containerId==initialMenuId:active.containerId!=initialMenuId,"Unexpected reopen menu identity");
                phase=Phase.PARK;
            }
            case PARK -> {
                // Like the album baseline, prime at a distinct position to survive a discarded first move after reopen.
                if(parkedPhase==0){moveToWindow(mc,12,12);parkedPhase=1;lastMotionTick=ticks;return false;}
                if(ticks<=lastMotionTick)return false;
                if(parkedPhase==1){if(!pointerAtWindow(mc,12,12))return false;moveToWindow(mc,8,8);parkedPhase=2;return false;}
                if(!pointerAtWindow(mc,8,8))return false;
                renderFrames=0;phase=Phase.CAPTURE;
            }
            case CAPTURE -> {
                require(menu(mc).field(4)==SPEEDS[step]&&menu(mc).field(1)==ENERGY[step],"State changed before screenshot");
                if(renderFrames<3)return false;
                var record=new JsonObject();record.addProperty("case",NAMES[step]);
                record.addProperty("expected_speed",SPEEDS[step]);record.addProperty("expected_energy",ENERGY[step]);
                record.addProperty("client_speed",menu(mc).field(4));record.addProperty("client_energy",menu(mc).field(1));
                record.addProperty("client_capacity",menu(mc).field(5));record.add("server",serverEvidence);
                record.addProperty("menu_id",menu(mc).containerId);record.addProperty("gui_scale",mc.getWindow().getGuiScale());
                record.addProperty("screen",mc.screen.getClass().getName());record.addProperty("render_frames",renderFrames);
                record.addProperty("native_presses",CLICKS[step]);record.addProperty("x11_button",BUTTONS[step]);
                record.addProperty("click_x",step==8?BORDER_X:DIAL_X);record.addProperty("click_y",DIAL_Y);
                record.addProperty("input_source",CLICKS[step]>0?"XTest via GLFW":"server fixture or programmatic reopen; no button input");
                String name="electrical-"+NAMES[step]+".png";
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(directory.resolve(name));}
                record.addProperty("capture",name);record.addProperty("capture_sha256",PackagedRuntimeProof.hash(Files.readAllBytes(directory.resolve(name))));
                observations.add(record);step++;ticks=0;
                phase=step==NAMES.length?Phase.DONE:Phase.PREPARE;
            }
            case DONE -> {return true;}
        }
        return phase==Phase.DONE;
    }

    void mousePressed(ScreenEvent.MouseButtonPressed.Post event) {
        if(!recordingInput)return;
        var mc=Minecraft.getInstance();
        require(event.getScreen()==mc.screen&&mc.screen instanceof MachineScreen.SmallElectricalFurnaceScreen,"Native click reached a different screen");
        var screen=(AbstractContainerScreen<?>)mc.screen;
        int expectedButton=BUTTONS[step]==1?GLFW.GLFW_MOUSE_BUTTON_LEFT:BUTTONS[step]==3?GLFW.GLFW_MOUSE_BUTTON_RIGHT:GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
        require(event.getButton()==expectedButton,"Unexpected native button");
        double x=event.getMouseX()-screen.getGuiLeft(),y=event.getMouseY()-screen.getGuiTop();
        require(Math.abs(x-(step==8?BORDER_X:DIAL_X))<=1&&Math.abs(y-DIAL_Y)<=1,"Native click missed acceptance point");
        if(step<=6)require(event.getClickResult(),"Dial did not consume its native click");
        var record=new JsonObject();record.addProperty("case",NAMES[step]);record.addProperty("glfw_button",event.getButton());
        record.addProperty("x",x);record.addProperty("y",y);record.addProperty("handled",event.getClickResult());
        record.addProperty("client_speed_during_callback",menu(mc).field(4));inputEvents.add(record);observedPresses++;
    }

    void rendered(ScreenEvent.Render.Post event) {
        var mc=Minecraft.getInstance();
        if(phase==Phase.CAPTURE&&event.getScreen()==mc.screen&&pointerAtWindow(mc,8,8)
            &&menu(mc).field(4)==SPEEDS[step]&&menu(mc).field(1)==ENERGY[step])renderFrames++;
    }

    JsonObject proof() {
        var p=new JsonObject();p.addProperty("suite","electrical-furnace-native-controls-v1");
        p.addProperty("complete",phase==Phase.DONE);p.addProperty("input_method",INPUT_METHOD);
        p.addProperty("native_presses_sent",sentPresses);p.addProperty("native_presses_observed",observedPresses);
        p.addProperty("synthetic_button_events",sentPresses*2);p.addProperty("initial_menu_id",initialMenuId);
        p.addProperty("fixture_energy_capacity",32000);p.addProperty("fixture_has_recipe",false);
        p.addProperty("visual_acceptance",false);p.add("observations",observations);p.add("input_events",inputEvents);
        return p;
    }

    int syntheticButtonEvents(){return sentPresses*2;}

    private JsonObject inspectServer(Minecraft mc,int expectedMenuId){
        var p=player(mc);var r=new JsonObject();
        boolean matches=p.containerMenu instanceof MachineMenu m&&m.kind()==MachineKind.SMALL_ELECTRICAL_FURNACE&&m.containerId==expectedMenuId;
        r.addProperty("matching_menu",matches);
        r.addProperty("menu_id",p.containerMenu.containerId);
        r.addProperty("speed",matches?((MachineMenu)p.containerMenu).field(4):-1);
        r.addProperty("energy",matches?((MachineMenu)p.containerMenu).field(1):-1);
        r.addProperty("capacity",matches?((MachineMenu)p.containerMenu).field(5):-1);
        r.addProperty("machine_speed",machine.dataAccessForTests().get(4));
        r.addProperty("cursor_empty",p.containerMenu.getCarried().isEmpty());r.addProperty("machine_empty",machine.isEmpty());return r;
    }
    private ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);require(p!=null,"Electrical player missing");return p;}
    private static MachineMenu menu(Minecraft mc){require(mc.player.containerMenu instanceof MachineMenu,"Electrical menu missing");return (MachineMenu)mc.player.containerMenu;}
    private boolean pointerAtGui(Minecraft mc,int x,int y){
        var screen=(AbstractContainerScreen<?>)mc.screen;
        return Math.abs(mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth()-screen.getGuiLeft()-x)<.75
            &&Math.abs(mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight()-screen.getGuiTop()-y)<.75;
    }
    private void moveToGui(Minecraft mc,int x,int y){
        var screen=(AbstractContainerScreen<?>)mc.screen;
        moveToWindow(mc,(int)Math.round((screen.getGuiLeft()+x)*(double)mc.getWindow().getScreenWidth()/screen.width),
            (int)Math.round((screen.getGuiTop()+y)*(double)mc.getWindow().getScreenHeight()/screen.height));
    }
    private static boolean pointerAtWindow(Minecraft mc,int x,int y){return Math.abs(mc.mouseHandler.xpos()-x)<=1&&Math.abs(mc.mouseHandler.ypos()-y)<=1;}
    private void moveToWindow(Minecraft mc,int x,int y){
        int[] wx={0},wy={0};GLFW.glfwGetWindowPos(mc.getWindow().getWindow(),wx,wy);
        require(xtest.getFunction("XTestFakeMotionEvent").invokeInt(new Object[]{display,-1,wx[0]+x,wy[0]+y,new NativeLong(0)})!=0,"Electrical pointer motion failed");
        x11.getFunction("XFlush").invokeInt(new Object[]{display});
    }
    private void nativeButton(int button,boolean pressed){
        require(xtest.getFunction("XTestFakeButtonEvent").invokeInt(new Object[]{display,button,pressed?1:0,new NativeLong(0)})!=0,"Electrical button event failed");
    }
    private static void require(boolean value,String message){PackagedRuntimeProof.require(value,message);}
}
