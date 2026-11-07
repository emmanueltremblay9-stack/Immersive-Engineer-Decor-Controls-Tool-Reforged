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

/** Independent test driver only: no production screen construction or presentation flags. */
@EventBusSubscriber(modid = FactoryPlacerValidationMod.MOD_ID, value = Dist.CLIENT)
public final class BlockBreakerPackagedValidation {
    private static final BlockPos POS = new BlockPos(0, 80, 0);
    private static final String PREFIX = "gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.";
    private static final String TITLE = "block.immersive_engineer_decor_controls_tool_reforged.small_block_breaker";
    private static final int[][] POINTS = {{128,14},{200,55},{200,75},{40,110},{40,75},{115,100},{128,140}};
    private static final String[] NAMES = {"title","energy","fluid","power","target","scene","work"};
    private enum Stage { BOOT, WORLD, INTERACT, OPEN, CLEAN, HOVER, WORK, PICKED, RETURNED }
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
    private static JsonObject packaged, tooltip, observed, workEvidence, finalConservation;
    private static final JsonArray captures = new JsonArray();
    private static NativeLibrary x11, xtest;
    private static Pointer display;
    private static int pickupAcks, returnAcks, opened, returnMenuSlot = -1;
    private BlockBreakerPackagedValidation() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("iedctr.blockBreakerPackagedNative") || complete) return;
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
                if (ticks < 20 || !mc.level.getBlockState(POS).is(ModBlocks.SMALL_BLOCK_BREAKER.get())) return;
                require(mc.screen == null && mc.player.getMainHandItem().isEmpty(), "Not an empty-hand world interaction");
                if (!(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(POS)) return;
                if (GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(), GLFW.GLFW_FOCUSED) != GLFW.GLFW_TRUE) { GLFW.glfwFocusWindow(mc.getWindow().getWindow()); return; }
                require(mc.player.getEyePosition().distanceTo(hit.getLocation()) < 4, "Interaction beyond survival reach");
                nativeClick(3); enter(Stage.OPEN); return;
            }
            if (!(mc.screen instanceof MachineScreen.SmallBlockBreakerScreen screen)) {
                require(mc.screen == null, "Unexpected screen: " + String.valueOf(mc.screen)); return;
            }
            var menu = (MachineMenu)mc.player.containerMenu;
            require(menu.slots.size() == 37 && menu.visibleMachineSlots() == 1, "Default Breaker slot topology differs");
            require(!screen.isSuppressTooltipsForPanelCapture(), "Tooltip suppression forbidden");
            if (stage == Stage.OPEN) {
                if (ticks < 20 || menu.energyStored() != 0 || menu.blockBreakerStatus() != 2 || !matches(mc, p ->
                    p.containerMenu instanceof MachineMenu && machine.isEmpty() && machine.energyStorage(Direction.DOWN).getEnergyStored() == 0
                    && p.serverLevel().getBlockState(POS.north()).is(Blocks.DIRT))) return;
                opened++; pointerX = screen.width - 2; pointerY = screen.height - 2; move(mc, screen);
                enter(Stage.CLEAN); return;
            }
            if (stage == Stage.CLEAN) {
                if (!atPointer(mc, screen)) { move(mc, screen); return; }
                if (ticks < 12) return;
                capture(mc, "normal", null); enter(Stage.HOVER); setHover(mc, screen); return;
            }
            if (stage == Stage.HOVER) {
                if (!atPointer(mc, screen)) { move(mc, screen); return; }
                if (ticks < 12 || tooltipFrames < 3 || observed == null) return;
                String actual = observed.get("text").getAsString().replace('\n', ' ');
                String expected = hoverIndex == 0 ? Component.translatable(TITLE).getString() : switch (hoverIndex) {
                    case 1 -> Component.translatable(PREFIX + "tooltip.input").getString();
                    case 2 -> Component.translatable(PREFIX + "tooltip.fluid_unavailable").getString();
                    case 3 -> Component.translatable(PREFIX + "tooltip.power_unavailable").getString();
                    case 4 -> Component.translatable(PREFIX + "tooltip.front_face").getString();
                    case 5 -> Component.translatable(PREFIX + "tooltip.static_scene").getString();
                    default -> Component.translatable(PREFIX + "cost.per_block", 240).getString();
                };
                require(actual.contains(expected), "Actual native translated tooltip differs: " + actual + " / " + expected);
                require(observed.get("in_bounds").getAsBoolean(), "Tooltip clipped");
                capture(mc, NAMES[hoverIndex], observed);
                if (++hoverIndex < POINTS.length) { ticks = 0; setHover(mc, screen); return; }
                pending = mc.getSingleplayerServer().submit(() -> {
                    require(machine.energyStorage(Direction.DOWN).receiveEnergy(240, false) == 240, "Setup FE admission differs");
                });
                pointerX = screen.width - 2; pointerY = screen.height - 2; move(mc, screen); enter(Stage.WORK); return;
            }
            if (stage == Stage.WORK) {
                if (ticks < 45 || !dirt(menu.getSlot(0).getItem()) || menu.energyStored() != 0 || !matches(mc, p ->
                    workComplete(p))) return;
                capture(mc, "worked", null); clickSlot(screen, menu, 0); enter(Stage.PICKED); return;
            }
            if (stage == Stage.PICKED) {
                if (ticks < 8 || !menu.getSlot(0).getItem().isEmpty() || !dirt(menu.getCarried()) || !matches(mc, p ->
                    machine.isEmpty() && dirt(p.containerMenu.getCarried()) && noDropsAndEmptyInventory(p)
                        && !p.containerMenu.getSlot(0).mayPlace(p.containerMenu.getCarried()))) return;
                pickupAcks++; returnMenuSlot = playerReturnSlot(menu, mc.player.getInventory(), menu.getCarried());
                clickSlot(screen, menu, returnMenuSlot); enter(Stage.RETURNED); return;
            }
            if (stage == Stage.RETURNED) {
                if (ticks < 8 || !menu.getSlot(0).getItem().isEmpty() || !menu.getCarried().isEmpty()
                        || !dirt(menu.getSlot(returnMenuSlot).getItem()) || !exactlyOneDirt(mc.player.getInventory())
                        || !matches(mc, p -> finalInventoryConfirmed(p))) return;
                returnAcks++; packaged = BlockBreakerRuntimeProof.verify(mc); capture(mc, "returned", null); finish(mc, null);
            }
        } catch (Throwable error) { finish(mc, error); }
    }
    private static void boot(Minecraft mc) throws Exception {
        nonce = System.getProperty("iedctr.clientGameplayNonce", "");
        require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha", "").matches("[a-f0-9]{40}"), "Missing run identity");
        directory = mc.gameDirectory.toPath().toRealPath();
        require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory", "")).toRealPath())
            && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce), "Isolation marker differs");
        require(mc.level == null && mc.getSingleplayerServer() == null && !Files.exists(directory.resolve("breaker-proof.json")), "Existing proof/world forbidden");
        if (Files.exists(directory.resolve("saves"))) try (var entries = Files.list(directory.resolve("saves"))) { require(entries.findAny().isEmpty(), "Existing save"); }
        packaged = BlockBreakerRuntimeProof.verify(mc);
        require(Files.readString(directory.resolve(".iedctr-breaker-xvfb")).equals(nonce + "\n" + System.getenv("DISPLAY")), "Display is not nonce-owned Xvfb");
        x11 = NativeLibrary.getInstance("X11"); xtest = NativeLibrary.getInstance("Xtst");
        display = x11.getFunction("XOpenDisplay").invokePointer(new Object[]{null}); require(display != null, "X11 unavailable");
        String name = "iedctr-breaker-" + nonce;
        mc.options.pauseOnLostFocus = false; enter(Stage.WORLD);
        mc.createWorldOpenFlows().createFreshLevel(name,
            new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(0L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }
    private static void setup(Minecraft mc) {
        var p = player(mc); var level = p.serverLevel(); p.setGameMode(GameType.SURVIVAL);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 5; z++) level.setBlock(new BlockPos(x,79,z), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(POS.north(), Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(POS, ModBlocks.SMALL_BLOCK_BREAKER.get().defaultBlockState().setValue(MachineBlocks.HORIZONTAL_FACING, Direction.NORTH), 3);
        require(level.getBlockEntity(POS) instanceof MachineBlockEntity, "Missing Breaker"); machine = (MachineBlockEntity)level.getBlockEntity(POS);
        require(machine.isEmpty() && machine.energyStorage(Direction.DOWN).getEnergyStored() == 0, "Nonempty initial machine");
        p.getInventory().clearContent(); p.connection.teleport(.5,80,3.5,180,24);
    }
    private static boolean workComplete(ServerPlayer p) {
        if (!p.serverLevel().getBlockState(POS.north()).isAir() || !dirt(machine.getItem(0))
                || machine.energyStorage(Direction.DOWN).getEnergyStored() != 0 || !noDropsAndEmptyInventory(p)) return false;
        workEvidence = new JsonObject();
        workEvidence.addProperty("target_before", "minecraft:dirt");workEvidence.addProperty("target_after", "minecraft:air");
        workEvidence.addProperty("energy_seeded",240);workEvidence.addProperty("energy_after",machine.energyStorage(Direction.DOWN).getEnergyStored());
        workEvidence.addProperty("output_item","minecraft:dirt");workEvidence.addProperty("output_count",machine.getItem(0).getCount());
        workEvidence.addProperty("world_drop_count",0);workEvidence.addProperty("server_confirmed",true);
        return true;
    }
    private static boolean noWorldDrops(ServerPlayer p) {
        return p.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(12)).isEmpty();
    }
    private static boolean noDropsAndEmptyInventory(ServerPlayer p) { return noWorldDrops(p) && p.getInventory().isEmpty(); }
    private static boolean exactlyOneDirt(Inventory inventory) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (!dirt(stack)) return false;
            count++;
        }
        return count == 1;
    }
    private static boolean finalInventoryConfirmed(ServerPlayer p) {
        if (returnMenuSlot <= 0 || !machine.isEmpty() || !p.containerMenu.getCarried().isEmpty()
                || p.containerMenu.getSlot(returnMenuSlot).container != p.getInventory()
                || !dirt(p.containerMenu.getSlot(returnMenuSlot).getItem()) || !exactlyOneDirt(p.getInventory())
                || !noWorldDrops(p) || p.containerMenu.getSlot(0).mayPlace(new ItemStack(Items.DIRT))) return false;
        finalConservation = new JsonObject();
        finalConservation.addProperty("destination", "player_inventory");
        finalConservation.addProperty("destination_menu_slot", returnMenuSlot);
        finalConservation.addProperty("machine_item_count", 0);
        finalConservation.addProperty("player_item", "minecraft:dirt");
        finalConservation.addProperty("player_item_count", 1);
        finalConservation.addProperty("carried_item_count", 0);
        finalConservation.addProperty("world_drop_count", 0);
        finalConservation.addProperty("output_accepts_insertion", false);
        finalConservation.addProperty("server_confirmed", true);
        return true;
    }
    private static boolean dirt(net.minecraft.world.item.ItemStack stack) { return stack.is(Items.DIRT) && stack.getCount() == 1; }
    private static ServerPlayer player(Minecraft mc) { var p = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId); require(p != null, "Player missing"); return p; }
    private static boolean matches(Minecraft mc, Predicate<ServerPlayer> test) {
        if (inspection == null) { inspection = mc.getSingleplayerServer().submit(() -> test.test(player(mc))); return false; }
        if (!inspection.isDone()) return false; boolean result = inspection.join(); inspection = null; return result;
    }
    private static void enter(Stage next) { stage = next; ticks = 0; inspection = null; }
    private static int playerReturnSlot(MachineMenu menu, Inventory inventory, ItemStack carried) {
        require(!menu.getSlot(0).mayPlace(carried), "Output must reject insertion");
        for (int i = 1; i < menu.slots.size(); i++) {
            var slot = menu.getSlot(i);
            if (slot.container == inventory && slot.isActive() && slot.getItem().isEmpty() && slot.mayPlace(carried)) return i;
        }
        throw new IllegalStateException("No empty insertable player-inventory slot");
    }
    private static void clickSlot(MachineScreen screen, MachineMenu menu, int slotIndex) {
        var slot = menu.getSlot(slotIndex); double x = (screen.width-menu.layout().imageWidth())/2 + slot.x + 8;
        double y = (screen.height-menu.layout().imageHeight())/2 + slot.y + 8;
        pointerX=(int)x;pointerY=(int)y;move(Minecraft.getInstance(),screen);nativeClick(1);
    }
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
        if (!Boolean.getBoolean("iedctr.blockBreakerPackagedNative") || complete || stage != Stage.HOVER
            || !(mc.screen instanceof MachineScreen.SmallBlockBreakerScreen screen) || !atPointer(mc,screen)) return;
        try {
            StringBuilder text=new StringBuilder(); int width=0,height=event.getComponents().size()==1?-2:0;
            for (var c:event.getComponents()) {
                FormattedCharSequence seq=null;
                for(var f:c.getClass().getDeclaredFields()) if(FormattedCharSequence.class.isAssignableFrom(f.getType())) { f.setAccessible(true);seq=(FormattedCharSequence)f.get(c);break; }
                require(seq != null,"Unobservable tooltip text"); if(!text.isEmpty())text.append('\n');
                seq.accept((index,style,cp)->{text.appendCodePoint(cp);return true;});
                width=Math.max(width,c.getWidth(event.getFont()));height+=c.getHeight();
            }
            tooltip=new JsonObject();tooltip.addProperty("text",text.toString());
            tooltip.addProperty("in_bounds",event.getX()-4>=0 && event.getY()-4>=0 && event.getX()+width+4<=screen.width && event.getY()+height+4<=screen.height);
            tooltip.addProperty("x",event.getX());tooltip.addProperty("y",event.getY());tooltip.addProperty("width",width);tooltip.addProperty("height",height);tooltipInFrame=true;
        } catch(Throwable error){finish(mc,error);}
    }
    @SubscribeEvent public static void after(ScreenEvent.Render.Post event) {
        if(tooltipInFrame && stage==Stage.HOVER && event.getScreen()==Minecraft.getInstance().screen){observed=tooltip.deepCopy();tooltipFrames++;} tooltipInFrame=false;
    }
    private static void capture(Minecraft mc,String name,JsonObject detail)throws Exception {
        Path file=directory.resolve("breaker-"+name+".png");require(!Files.exists(file),"Stale capture");
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(file);}
        var entry=new JsonObject();entry.addProperty("name",name);entry.addProperty("file",file.getFileName().toString());
        entry.addProperty("sha256",BlockBreakerRuntimeProof.hash(Files.readAllBytes(file)));
        if(detail!=null){entry.add("tooltip",detail.deepCopy());entry.addProperty("render_frames",tooltipFrames);entry.addProperty("pointer_x",pointerX);entry.addProperty("pointer_y",pointerY);}
        captures.add(entry);
    }
    private static void finish(Minecraft mc,Throwable error) {
        complete=true;
        if(display!=null){x11.getFunction("XCloseDisplay").invokeInt(new Object[]{display});display=null;}
        try { if(directory!=null){
            var proof=new JsonObject();proof.addProperty("suite","native-block-breaker-packaged-v1");proof.addProperty("status",error==null?"passed":"failed");
            proof.addProperty("nonce",nonce);proof.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha"));proof.add("packaged_runtime",packaged);
            proof.addProperty("presentation_selector_absent",System.getProperty(MachineMenu.DEV_FACTORY_PLACER_V2_REFERENCE_PROPERTY)==null);
            proof.addProperty("opened_via_native_block_interaction",opened);proof.addProperty("pickup_server_acks",pickupAcks);proof.addProperty("return_server_acks",returnAcks);
            proof.addProperty("energy_seeded",240);proof.addProperty("energy_final",machine==null?-1:machine.energyStorage(Direction.DOWN).getEnergyStored());
            proof.addProperty("selected_language",mc.getLanguageManager().getSelected());proof.addProperty("width",mc.getWindow().getWidth());proof.addProperty("height",mc.getWindow().getHeight());
            proof.addProperty("native_visual_acceptance",false);proof.addProperty("input_method","XTest physical right-click on verified block raycast; XTest physical inventory clicks and pointer hover through GLFW/Minecraft; independent server acknowledgements");
            proof.addProperty("setup","Seeded flat survival world, machine, dirt target and 240FE via underside capability. Real ticking performs work. No production state injection during work.");
            proof.add("work",workEvidence);proof.add("final_conservation",finalConservation);proof.add("captures",captures);if(error!=null)proof.addProperty("error",error.toString());
            Files.writeString(directory.resolve("breaker-proof.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");
        }}catch(Exception ex){EngineersDecorReforged.LOGGER.error("Breaker proof write failed",ex);}
        if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_BREAKER_PACKAGED PASSED nonce={}",nonce);
        else EngineersDecorReforged.LOGGER.error("IEDCTR_BREAKER_PACKAGED FAILED at "+stage,error);
        mc.stop();
    }
    private static void require(boolean ok,String reason){BlockBreakerRuntimeProof.require(ok,reason);}
}
