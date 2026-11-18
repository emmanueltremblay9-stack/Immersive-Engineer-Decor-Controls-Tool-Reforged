package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.MachineScreen;
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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/** Independent test driver only: no production screen construction or presentation flags. */
@EventBusSubscriber(modid = FactoryPlacerValidationMod.MOD_ID, value = Dist.CLIENT)
public final class FactoryControlTooltipValidation {
    private static final BlockPos POS = new BlockPos(0, 80, 0);
    private static final boolean HOPPER = "hopper".equals(System.getProperty("iedctr.factoryControlMachine"));
    private static final String PREFIX = "block.immersive_engineer_decor_controls_tool_reforged.factory_" + (HOPPER ? "hopper" : "dropper");
    private static final int[][] POINTS = HOPPER ? new int[][]{{148,23},{148,52},{148,52},{148,52},{162,52},{162,52}} : new int[][]{{156,46},{151,103},{151,103},{174,103},{174,103},{197,103},{197,103}};
    private static final String[] NAMES = HOPPER ? new String[]{"delayindicator","inversion","inversion_changed","inversion_normal","triggermode","triggermode_changed"} : new String[]{"velocity","filtergate","filtergate_changed","externgate","externgate_changed","triggermode","triggermode_changed"};
    private enum Stage { BOOT, WORLD, INTERACT, OPEN, CLEAN, HOVER, TRIGGER_SETUP, ICON, RESUME }
    private static Stage stage = Stage.BOOT;
    private static boolean complete, tooltipInFrame;
    private static int ticks, hoverIndex, tooltipFrames, pointerX, pointerY;
    private static long deadline;
    private static UUID playerId;
    private static MachineBlockEntity machine;
    private static CompletableFuture<?> pending;
    private static CompletableFuture<Boolean> inspection;
    private static Path directory;
    private static String nonce;
    private static JsonObject packaged, tooltip, observed;
    private static final JsonArray captures = new JsonArray();
    private static NativeLibrary x11, xtest;
    private static Pointer display;
    private static int opened, awaitingLogic = -1, stateChangeAcks, expectedDropperLogic;
    private static final JsonArray transitions = new JsonArray();
    private static String triggerTextBefore;
    private static final JsonArray hopperIcons = new JsonArray();
    private static int iconFrames, iconLogic;
    private static int triggerSetupBefore = -1, triggerSetupAfter = -1;
    private FactoryControlTooltipValidation() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("iedctr.factoryControlTooltipNative") || complete) return;
        var mc = Minecraft.getInstance();
        try {
            if (deadline == 0) deadline = System.nanoTime() + 900_000_000_000L;
            require(System.nanoTime() < deadline && ++ticks < 1800, "Timeout at " + stage);
            require(System.getProperty(MachineMenu.DEV_FACTORY_PLACER_V2_REFERENCE_PROPERTY) == null,
                "Presentation selector must be absent");
            require(!Boolean.getBoolean("iedctr.factoryPlacerPackagedDefaultNative") && !Boolean.getBoolean("iedctr.factoryPlacerV2ReferenceNative")
                && !Boolean.getBoolean("iedctr.clientGameplay"), "Conflicting test driver");
            if (stage == Stage.BOOT) {
                if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) boot(mc);
                return;
            }
            if (stage == Stage.WORLD) {
                if (mc.level == null || mc.player == null || mc.gameMode == null || mc.screen != null) return;
                require(mc.getSingleplayerServer() != null && mc.player.connection.getConnection().isMemoryConnection(), "Integrated test world required");
                playerId = mc.player.getUUID(); pending = mc.getSingleplayerServer().submit(() -> setup(mc)); enter(Stage.INTERACT); return;
            }
            if (pending != null) { if (!pending.isDone()) return; pending.join(); pending = null; }
            if (mc.getOverlay() != null) return;
            require(mc.level != null && mc.player != null && mc.getSingleplayerServer() != null, "Disconnected");
            if (stage == Stage.INTERACT) {
                if (ticks < 20 || !mc.level.getBlockState(POS).is((HOPPER ? ModBlocks.FACTORY_HOPPER.get() : ModBlocks.FACTORY_DROPPER.get()))) return;
                require(mc.screen == null && mc.player.getMainHandItem().isEmpty(), "Not an empty-hand world interaction");
                if (!(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(POS)) return;
                if (GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(), GLFW.GLFW_FOCUSED) != GLFW.GLFW_TRUE) { GLFW.glfwFocusWindow(mc.getWindow().getWindow()); return; }
                require(mc.player.getEyePosition().distanceTo(hit.getLocation()) < 4, "Interaction beyond survival reach");
                nativeClick(3); enter(Stage.OPEN); return;
            }
            if (!(mc.screen instanceof MachineScreen screen)) {
                require(mc.screen == null, "Unexpected screen: " + String.valueOf(mc.screen)); return;
            }
            var menu = (MachineMenu)mc.player.containerMenu;
            require(menu.kind() == (HOPPER ? com.oblixorprime.engineersdecorreforged.utility.MachineKind.FACTORY_HOPPER : com.oblixorprime.engineersdecorreforged.utility.MachineKind.FACTORY_DROPPER), "Wrong factory menu");
            require(!screen.isSuppressTooltipsForPanelCapture(), "Tooltip suppression forbidden");
            if (stage == Stage.OPEN) {
                if (ticks < 20 || !matches(mc, p -> p.containerMenu instanceof MachineMenu serverMenu && serverMenu.kind() == menu.kind() && serverMenu.field(2) == menu.field(2))) return;
                opened++; pointerX = screen.width - 2; pointerY = screen.height - 2; move(mc, screen);
                enter(Stage.CLEAN); return;
            }
            if (stage == Stage.CLEAN) {
                if (!atPointer(mc, screen)) { move(mc, screen); return; }
                if (ticks < 12) return;
                capture(mc, "normal", null); enter(Stage.HOVER); setHover(mc, screen); return;
            }
            if (stage == Stage.TRIGGER_SETUP) {
                if (menu.field(2) != 2 || !matches(mc, p -> p.containerMenu instanceof MachineMenu serverMenu && serverMenu.containerId == menu.containerId && serverMenu.field(2) == 2)) return;
                require((menu.field(2) & 4) == 0, "Trigger setup must exit ignored-redstone mode");
                triggerSetupAfter = menu.field(2); awaitingLogic = -1; stateChangeAcks++;
                enter(Stage.HOVER); setHover(mc, screen); return;
            }
            if (stage == Stage.ICON) {
                if (!atPointer(mc, screen)) { move(mc, screen); iconFrames=0; return; }
                require(menu.field(2)==iconLogic, "Hopper icon state drift");
                if(ticks<12 || iconFrames<3 || !matches(mc,p -> p.containerMenu instanceof MachineMenu serverMenu
                    && serverMenu.containerId==menu.containerId && serverMenu.field(2)==iconLogic))return;
                String name="icon-"+NAMES[hoverIndex];
                Path file=directory.resolve("factory-control-"+name+".png");
                require(!Files.exists(file),"Stale icon capture");
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(file);}
                var entry=new JsonObject();entry.addProperty("name",name);entry.addProperty("file",file.getFileName().toString());
                entry.addProperty("sha256",FactoryControlRuntimeProof.hash(Files.readAllBytes(file)));
                entry.addProperty("client_logic",iconLogic);entry.addProperty("server_logic",iconLogic);
                entry.addProperty("render_frames",iconFrames);entry.addProperty("gui_scale",mc.getWindow().getGuiScale());
                entry.addProperty("tooltip_absent",true);hopperIcons.add(entry);
                enter(Stage.RESUME);setHover(mc,screen);return;
            }
            if(stage==Stage.RESUME){
                if(!atPointer(mc,screen)){move(mc,screen);return;}
                if(ticks<3)return;
                advance(mc,screen,menu);return;
            }
            if (stage == Stage.HOVER) {
                if (!atPointer(mc, screen)) { move(mc, screen); return; }
                if (awaitingLogic != -1) {
                    int field = HOPPER ? 2 : 5;
                    int current = menu.field(field);
                    if (current == awaitingLogic || (HOPPER && current != (hoverIndex==2?6:0)) || (!HOPPER && current != expectedDropperLogic)
                        || !matches(mc, p -> p.containerMenu instanceof MachineMenu serverMenu
                            && serverMenu.containerId == menu.containerId && serverMenu.field(field) == current)) return;
                    if(!HOPPER){var transition=new JsonObject();transition.addProperty("control",NAMES[hoverIndex].replace("_changed",""));
                        transition.addProperty("before",awaitingLogic);transition.addProperty("client_after",current);
                        transition.addProperty("server_after",current);transition.addProperty("expected_after",expectedDropperLogic);
                        transitions.add(transition);}
                    awaitingLogic = -1; stateChangeAcks++; ticks = 0; tooltipFrames = 0; observed = null; return;
                }
                if (ticks < 12 || tooltipFrames < 3 || observed == null) return;
                String part=NAMES[hoverIndex].replace("_changed", "").replace("_normal", "");
                String expected = Component.translatable(PREFIX + ".tooltips." + part).getString();
                if (HOPPER && hoverIndex > 0) {
                    int logic = menu.field(2);
                    String state = (hoverIndex >= 1 && hoverIndex <= 3) ? ((logic & 4) != 0 ? "ignored" : (logic & 1) != 0 ? "inverted" : "normal")
                        : ((logic & 4) != 0 ? "ignore_trigger" : (logic & 2) != 0 ? "continuous" : "pulse");
                    expected += "\n" + Component.translatable(PREFIX + ".state." + state).getString();
                    observed.addProperty("synced_logic", logic);
                }
                expected = net.minecraft.ChatFormatting.stripFormatting(expected);
                observed.addProperty("expected_text", expected);
                if(!HOPPER)observed.addProperty("synced_logic",menu.field(5));
                if (HOPPER && hoverIndex >= 4) {
                    require((menu.field(2) & 4) == 0, "Trigger tooltip must describe pulse or continuous, not ignored mode");
                    String visible = observed.get("text").getAsString().replaceAll("\\s+", " ").trim();
                    if (hoverIndex == 4) triggerTextBefore = visible;
                    else require(triggerTextBefore != null && !triggerTextBefore.equals(visible), "Trigger click must change visible semantic text");
                }
                require(observed.get("gray_characters").getAsInt() > 0, "Legacy gray text style lost");
                // Wrapping inserts line breaks, but must not lose any localized words.
                require(observed.get("text").getAsString().replaceAll("\\s+", " ").trim().equals(expected.replaceAll("\\s+", " ").trim()), "Native tooltip text differs");
                require(observed.get("in_bounds").getAsBoolean(), "Tooltip clipped");
                require(observed.get("width").getAsInt() <= Math.max(1, Math.min(240, screen.width - 16)), "Tooltip exceeds wrap bound");
                require(observed.get("height").getAsInt() > 10, "Expected multiline tooltip");
                capture(mc, NAMES[hoverIndex], observed);
                if(HOPPER && hoverIndex>=1 && hoverIndex<=3){
                    int[] expectedStates={3,6,2};
                    require(menu.field(2)==expectedStates[hoverIndex-1],"Unexpected Hopper redstone state");
                    iconLogic=menu.field(2);iconFrames=0;pointerX=8;pointerY=8;
                    enter(Stage.ICON);move(mc,screen);return;
                }
                advance(mc,screen,menu);
            }
        } catch (Throwable error) { finish(mc, error); }
    }
    private static void advance(Minecraft mc,MachineScreen screen,MachineMenu menu) throws Exception {
                if (HOPPER && hoverIndex == 2) {
                    require((menu.field(2) & 4) != 0, "Expected ignored-redstone inversion example before trigger setup");
                    triggerSetupBefore = menu.field(2); awaitingLogic = triggerSetupBefore;
                    nativeClick(1); hoverIndex++; enter(Stage.TRIGGER_SETUP); return;
                }
                if (HOPPER && (hoverIndex == 1 || hoverIndex == 4)) {
                    awaitingLogic = menu.field(2); nativeClick(1);
                }
                if (!HOPPER && (hoverIndex==1 || hoverIndex==3 || hoverIndex==5)) {
                    awaitingLogic=menu.field(5);
                    expectedDropperLogic=hoverIndex==1?awaitingLogic^1:hoverIndex==5?awaitingLogic^16:
                        (awaitingLogic&~34)|switch(awaitingLogic&34){case 0->32;case 2->0;default->2;};
                    nativeClick(1);
                }
                if (++hoverIndex < POINTS.length) { enter(Stage.HOVER); setHover(mc, screen); return; }
                packaged = FactoryControlRuntimeProof.verify(mc); finish(mc, null);
    }
    private static void boot(Minecraft mc) throws Exception {
        nonce = System.getProperty("iedctr.clientGameplayNonce", "");
        require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha", "").matches("[a-f0-9]{40}"), "Missing run identity");
        directory = mc.gameDirectory.toPath().toRealPath();
        require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory", "")).toRealPath())
            && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce), "Isolation marker differs");
        require(mc.level == null && mc.getSingleplayerServer() == null && !Files.exists(directory.resolve("factory-control-proof.json")), "Existing proof/world forbidden");
        if (Files.exists(directory.resolve("saves"))) try (var entries = Files.list(directory.resolve("saves"))) { require(entries.findAny().isEmpty(), "Existing save"); }
        packaged = FactoryControlRuntimeProof.verify(mc);
        require(Files.readString(directory.resolve(".iedctr-factory-control-xvfb")).equals(nonce + "\n" + System.getenv("DISPLAY")), "Display is not nonce-owned Xvfb");
        x11 = NativeLibrary.getInstance("X11"); xtest = NativeLibrary.getInstance("Xtst");
        display = x11.getFunction("XOpenDisplay").invokePointer(new Object[]{null}); require(display != null, "X11 unavailable");
        String name = "iedctr-factory-control-" + nonce;
        mc.options.pauseOnLostFocus = false; enter(Stage.WORLD);
        mc.createWorldOpenFlows().createFreshLevel(name,
            new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(0L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }
    private static void setup(Minecraft mc) {
        var p = player(mc); var level = p.serverLevel(); p.setGameMode(GameType.SURVIVAL);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 5; z++) level.setBlock(new BlockPos(x,79,z), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(POS, (HOPPER ? ModBlocks.FACTORY_HOPPER.get() : ModBlocks.FACTORY_DROPPER.get()).defaultBlockState().setValue(MachineBlocks.FACING, Direction.NORTH), 3);
        require(level.getBlockEntity(POS) instanceof MachineBlockEntity, "Missing Factory machine"); machine = (MachineBlockEntity)level.getBlockEntity(POS);
        require(machine.isEmpty(), "Nonempty initial machine");
        p.getInventory().clearContent(); p.connection.teleport(.5,80,3.5,180,24);
    }
    private static ServerPlayer player(Minecraft mc) { var p = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId); require(p != null, "Player missing"); return p; }
    private static boolean matches(Minecraft mc, Predicate<ServerPlayer> test) {
        if (inspection == null) { inspection = mc.getSingleplayerServer().submit(() -> test.test(player(mc))); return false; }
        if (!inspection.isDone()) return false; boolean result = inspection.join(); inspection = null; return result;
    }
    private static void enter(Stage next) { stage = next; ticks = 0; inspection = null; }
    private static void nativeClick(int button) {
        require(xtest.getFunction("XTestFakeButtonEvent").invokeInt(new Object[]{display,button,1,new NativeLong(0)}) != 0,"Native button press failed");
        require(xtest.getFunction("XTestFakeButtonEvent").invokeInt(new Object[]{display,button,0,new NativeLong(0)}) != 0,"Native button release failed");
        x11.getFunction("XFlush").invokeInt(new Object[]{display});
    }
    private static void setHover(Minecraft mc, MachineScreen screen) {
        pointerX = (screen.width-((MachineMenu)mc.player.containerMenu).layout().imageWidth())/2 + POINTS[hoverIndex][0];
        pointerY = (screen.height-((MachineMenu)mc.player.containerMenu).layout().imageHeight())/2 + POINTS[hoverIndex][1];
        tooltipFrames = 0; observed = null; move(mc,screen);
    }
    private static void move(Minecraft mc, MachineScreen screen) {
        long window = mc.getWindow().getWindow();
        if (GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED) != GLFW.GLFW_TRUE) { GLFW.glfwFocusWindow(window); return; }
        int[] x={0}, y={0}; GLFW.glfwGetWindowPos(window,x,y);
        int tx=x[0]+(int)Math.round(pointerX*mc.getWindow().getScreenWidth()/(double)screen.width);
        int ty=y[0]+(int)Math.round(pointerY*mc.getWindow().getScreenHeight()/(double)screen.height);
        require(xtest.getFunction("XTestFakeMotionEvent").invokeInt(new Object[]{display,-1,tx,ty,new NativeLong(0)}) != 0,"Pointer motion failed");
        x11.getFunction("XFlush").invokeInt(new Object[]{display});
    }
    private static boolean atPointer(Minecraft mc, MachineScreen screen) {
        return Math.abs(mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth()-pointerX)<1
            && Math.abs(mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight()-pointerY)<1;
    }
    @SubscribeEvent public static void before(ScreenEvent.Render.Pre event) { tooltipInFrame=false; }
    @SubscribeEvent public static void tooltip(RenderTooltipEvent.Color event) {
        var mc=Minecraft.getInstance();
        if (!Boolean.getBoolean("iedctr.factoryControlTooltipNative") || complete || (stage != Stage.HOVER && stage != Stage.ICON)
            || !(mc.screen instanceof MachineScreen screen) || !atPointer(mc,screen)) return;
        try {
            require(stage!=Stage.ICON,"Tooltip covers icon evidence");
            int[] gray = {0};
            StringBuilder text=new StringBuilder(); int width=0,height=event.getComponents().size()==1?-2:0;
            for (var c:event.getComponents()) {
                FormattedCharSequence seq=null;
                for(var f:c.getClass().getDeclaredFields()) if(FormattedCharSequence.class.isAssignableFrom(f.getType())) { f.setAccessible(true);seq=(FormattedCharSequence)f.get(c);break; }
                require(seq != null,"Unobservable tooltip text"); if(!text.isEmpty())text.append('\n');
                seq.accept((index,style,cp)->{text.appendCodePoint(cp);if (style.getColor()!=null && style.getColor().getValue()==0xAAAAAA) gray[0]++;return true;});
                width=Math.max(width,c.getWidth(event.getFont()));height+=c.getHeight();
            }
            tooltip=new JsonObject();tooltip.addProperty("text",text.toString());tooltip.addProperty("gray_characters",gray[0]);
            tooltip.addProperty("in_bounds",event.getX()-4>=0 && event.getY()-4>=0 && event.getX()+width+4<=screen.width && event.getY()+height+4<=screen.height);
            tooltip.addProperty("x",event.getX());tooltip.addProperty("y",event.getY());tooltip.addProperty("width",width);tooltip.addProperty("height",height);tooltipInFrame=true;
        } catch(Throwable error){finish(mc,error);}
    }
    @SubscribeEvent public static void after(ScreenEvent.Render.Post event) {
        if(stage==Stage.ICON && event.getScreen()==Minecraft.getInstance().screen && atPointer(Minecraft.getInstance(),(MachineScreen)event.getScreen()))iconFrames++;
        if(tooltipInFrame && stage==Stage.HOVER && event.getScreen()==Minecraft.getInstance().screen){observed=tooltip.deepCopy();tooltipFrames++;} tooltipInFrame=false;
    }
    private static void capture(Minecraft mc,String name,JsonObject detail)throws Exception {
        Path file=directory.resolve("factory-control-"+name+".png");require(!Files.exists(file),"Stale capture");
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(file);}
        var entry=new JsonObject();entry.addProperty("name",name);entry.addProperty("file",file.getFileName().toString());
        entry.addProperty("sha256",FactoryControlRuntimeProof.hash(Files.readAllBytes(file)));
        if(detail!=null){entry.add("tooltip",detail.deepCopy());entry.addProperty("render_frames",tooltipFrames);entry.addProperty("pointer_x",pointerX);entry.addProperty("pointer_y",pointerY);}
        captures.add(entry);
    }
    private static void finish(Minecraft mc,Throwable error) {
        complete=true;
        if(display!=null){x11.getFunction("XCloseDisplay").invokeInt(new Object[]{display});display=null;}
        try { if(directory!=null){
            var proof=new JsonObject();proof.addProperty("suite","native-factory-control-tooltip-v2");proof.addProperty("status",error==null?"passed":"failed");
            proof.addProperty("nonce",nonce);proof.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha"));proof.add("packaged_runtime",packaged);
            proof.addProperty("presentation_selector_absent",System.getProperty(MachineMenu.DEV_FACTORY_PLACER_V2_REFERENCE_PROPERTY)==null);
            proof.addProperty("opened_via_native_block_interaction",opened);
            proof.addProperty("state_change_server_acks",stateChangeAcks);
            proof.add("dropper_transitions",transitions);
            if (HOPPER) {
                proof.add("hopper_icons",hopperIcons);
                proof.addProperty("trigger_setup_before",triggerSetupBefore);
                proof.addProperty("trigger_setup_after",triggerSetupAfter);
            }
            proof.addProperty("machine", HOPPER ? "hopper" : "dropper");
            proof.addProperty("selected_language",mc.getLanguageManager().getSelected());proof.addProperty("width",mc.getWindow().getWidth());proof.addProperty("height",mc.getWindow().getHeight());
            proof.addProperty("native_visual_acceptance",false);
            proof.addProperty("input_method", "XTest block interaction and tooltip hover; seeded isolated integrated world");
            proof.add("captures",captures);if(error!=null)proof.addProperty("error",error.toString());
            Files.writeString(directory.resolve("factory-control-proof.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");
        }}catch(Exception ex){EngineersDecorReforged.LOGGER.error("Factory tooltip proof write failed",ex);}
        if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_FACTORY_CONTROL PASSED nonce={}",nonce);
        else EngineersDecorReforged.LOGGER.error("IEDCTR_FACTORY_CONTROL FAILED at "+stage,error);
        mc.stop();
    }
    private static void require(boolean ok,String reason){FactoryControlRuntimeProof.require(ok,reason);}
}
