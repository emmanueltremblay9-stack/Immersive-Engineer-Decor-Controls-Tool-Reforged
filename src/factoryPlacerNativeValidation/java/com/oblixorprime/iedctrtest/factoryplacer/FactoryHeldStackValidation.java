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
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
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
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Isolated real-XTest inventory safety witness for the two Factory screens.
 * Uses the ordinary packaged mod JAR with this class in the separate test mod.
 * Does not alter production hitboxes, server actions or inventory logic.
 */
@EventBusSubscriber(modid = FactoryPlacerValidationMod.MOD_ID, value = Dist.CLIENT)
public final class FactoryHeldStackValidation {
    private static final BlockPos POS = new BlockPos(0, 80, 0);
    private static final boolean HOPPER = "hopper".equals(System.getProperty("iedctr.factoryControlMachine"));
    private static final int SCALE = Integer.getInteger("iedctr.factoryHeldScale", 0);
    private enum Stage {
        BOOT, WORLD, INTERACT, OPEN, SEEDED, PICKUP, PICKED,
        BORDER_CLICK, BORDER_CHECK, RETURN_CLICK, RETURNED,
        REPICK_CLICK, REPICKED, OUTSIDE_CLICK, OUTSIDE_CHECK
    }
    private static Stage stage = Stage.BOOT;
    private static boolean complete;
    private static int ticks, opened, physicalClicks, picked, borderPreserved, returned, outsideDropped;
    private static long deadline, lastClick;
    private static UUID playerId;
    private static MachineBlockEntity machine;
    private static CompletableFuture<?> pending;
    private static CompletableFuture<Boolean> inspection;
    private static Path directory;
    private static String nonce;
    private static JsonObject packaged;
    private static NativeLibrary x11, xtest;
    private static Pointer display;
    private static double pointerX, pointerY;
    private static final JsonArray captures = new JsonArray();
    private FactoryHeldStackValidation() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("iedctr.factoryHeldStackNative") || complete) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (deadline == 0) deadline = System.nanoTime() + 900_000_000_000L;
            require(System.nanoTime() < deadline && ++ticks < 1800, "Timeout at " + stage);
            require(!Boolean.getBoolean("iedctr.factoryControlTooltipNative"), "Competing Factory native test");
            require(System.getProperty(MachineMenu.DEV_FACTORY_PLACER_V2_REFERENCE_PROPERTY) == null,
                "Presentation selector forbidden");
            if (stage == Stage.BOOT) {
                if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) boot(mc);
                return;
            }
            if (stage == Stage.WORLD) {
                if (mc.level == null || mc.player == null || mc.gameMode == null || mc.screen != null) return;
                require(mc.getSingleplayerServer() != null && mc.player.connection.getConnection().isMemoryConnection(),
                    "Isolated integrated server required");
                playerId = mc.player.getUUID();
                pending = mc.getSingleplayerServer().submit(() -> setup(mc));
                enter(Stage.INTERACT);
                return;
            }
            if (pending != null) {
                if (!pending.isDone()) return;
                pending.join();
                pending = null;
            }
            if (mc.getOverlay() != null) return;
            require(mc.level != null && mc.player != null && mc.getSingleplayerServer() != null, "Disconnected");
            if (stage == Stage.INTERACT) {
                if (ticks < 20 || !mc.level.getBlockState(POS).is(
                    HOPPER ? ModBlocks.FACTORY_HOPPER.get() : ModBlocks.FACTORY_DROPPER.get())) return;
                require(mc.screen == null && mc.player.getMainHandItem().isEmpty(), "Empty-hand opening required");
                if (!(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(POS)) return;
                long window = mc.getWindow().getWindow();
                if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) != GLFW.GLFW_TRUE) {
                    GLFW.glfwFocusWindow(window);
                    return;
                }
                require(mc.player.getEyePosition().distanceTo(hit.getLocation()) < 4, "Beyond reach");
                nativeClick(3);
                enter(Stage.OPEN);
                return;
            }
            if (!(mc.screen instanceof MachineScreen screen)) {
                require(mc.screen == null, "Unexpected screen");
                return;
            }
            MachineMenu menu = (MachineMenu) mc.player.containerMenu;
            int index = menu.visibleMachineSlots(); // First real player inventory slot; never a filter.
            geometry(mc, screen, menu, index);
            switch (stage) {
                case OPEN -> {
                    if (ticks < 12 || !matches(mc, p -> p.containerMenu instanceof MachineMenu sm
                        && sm.kind() == menu.kind() && sm.getCarried().isEmpty() && empty(sm, index)
                        && inventoryLogs(p) == 0 && worldLogs(p) == 0)) return;
                    opened++;
                    pending = mc.getSingleplayerServer().submit(() -> {
                        ServerPlayer p = player(mc);
                        require(p.containerMenu instanceof MachineMenu && p.containerMenu.getCarried().isEmpty(),
                            "Unclean menu before synthetic seed");
                        p.containerMenu.getSlot(index).set(new ItemStack(Items.OAK_LOG, 1));
                        p.containerMenu.broadcastChanges();
                    });
                    enter(Stage.SEEDED);
                }
                case SEEDED -> {
                    if (ticks < 8 || !slotLog(menu, index) || !menu.getCarried().isEmpty()
                        || !matches(mc, p -> slotLog(p.containerMenu, index)
                            && p.containerMenu.getCarried().isEmpty() && inventoryLogs(p) == 1 && worldLogs(p) == 0)) return;
                    enter(Stage.PICKUP);
                }
                case PICKUP -> {
                    if (!clickAt(mc, screen, slotX(screen, menu, index), slotY(screen, menu, index))) return;
                    enter(Stage.PICKED);
                }
                case PICKED -> {
                    if (!settled() || !carriedLog(menu) || !empty(menu, index)
                        || !matches(mc, p -> carriedLog(p.containerMenu) && empty(p.containerMenu, index)
                            && inventoryLogs(p) == 1 && worldLogs(p) == 0)) return;
                    picked++;
                    capture(mc, "picked");
                    enter(Stage.BORDER_CLICK);
                }
                case BORDER_CLICK -> {
                    double bx = screen.getGuiLeft() + (HOPPER ? -4 : 6);
                    double by = screen.getGuiTop() + (HOPPER ? -8 : 8);
                    if (!clickAt(mc, screen, bx, by)) return;
                    enter(Stage.BORDER_CHECK);
                }
                case BORDER_CHECK -> {
                    if (!settled() || !carriedLog(menu) || !empty(menu, index)
                        || !matches(mc, p -> carriedLog(p.containerMenu) && empty(p.containerMenu, index)
                            && inventoryLogs(p) == 1 && worldLogs(p) == 0)) return;
                    borderPreserved++;
                    capture(mc, "inert-border-held");
                    enter(Stage.RETURN_CLICK);
                }
                case RETURN_CLICK -> {
                    if (!clickAt(mc, screen, slotX(screen, menu, index), slotY(screen, menu, index))) return;
                    enter(Stage.RETURNED);
                }
                case RETURNED -> {
                    if (!settled() || !slotLog(menu, index) || !menu.getCarried().isEmpty()
                        || !matches(mc, p -> slotLog(p.containerMenu, index)
                            && p.containerMenu.getCarried().isEmpty() && inventoryLogs(p) == 1 && worldLogs(p) == 0)) return;
                    returned++;
                    capture(mc, "returned");
                    enter(Stage.REPICK_CLICK);
                }
                case REPICK_CLICK -> {
                    if (!clickAt(mc, screen, slotX(screen, menu, index), slotY(screen, menu, index))) return;
                    enter(Stage.REPICKED);
                }
                case REPICKED -> {
                    if (!settled() || !carriedLog(menu) || !empty(menu, index)
                        || !matches(mc, p -> carriedLog(p.containerMenu) && empty(p.containerMenu, index)
                            && inventoryLogs(p) == 1 && worldLogs(p) == 0)) return;
                    picked++;
                    enter(Stage.OUTSIDE_CLICK);
                }
                case OUTSIDE_CLICK -> {
                    // Separate destructive control: deliberately outside the *visible* frame.
                    // Hopper's custom border extends to left-8; its outside witness is left-9.
                    double ox = screen.getGuiLeft() + (HOPPER ? -9 : -2);
                    double oy = screen.getGuiTop() + 83;
                    if (!clickAt(mc, screen, ox, oy)) return;
                    enter(Stage.OUTSIDE_CHECK);
                }
                case OUTSIDE_CHECK -> {
                    if (!settled() || !menu.getCarried().isEmpty() || !empty(menu, index)
                        || !matches(mc, p -> p.containerMenu.getCarried().isEmpty()
                            && empty(p.containerMenu, index) && inventoryLogs(p) == 0 && worldLogs(p) == 1)) return;
                    outsideDropped++;
                    capture(mc, "outside-witness");
                    packaged = FactoryControlRuntimeProof.verify(mc);
                    require(physicalClicks == 5 && picked == 2 && returned == 1
                        && borderPreserved == 1 && outsideDropped == 1, "Incomplete physical transition coverage");
                    finish(mc, null);
                }
                default -> {}
            }
        } catch (Throwable error) {
            finish(mc, error);
        }
    }

    private static void boot(Minecraft mc) throws Exception {
        nonce = System.getProperty("iedctr.clientGameplayNonce", "");
        require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha", "").matches("[a-f0-9]{40}"),
            "Missing unique run identity");
        require(SCALE == 1 || SCALE == 2, "Invalid requested GUI scale");
        directory = mc.gameDirectory.toPath().toRealPath();
        require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory", "")).toRealPath())
            && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce),
            "Run ownership mismatch");
        require(mc.level == null && mc.getSingleplayerServer() == null
            && !Files.exists(directory.resolve("factory-held-proof.json")), "Stale world/proof");
        if (Files.exists(directory.resolve("saves"))) {
            try (var entries = Files.list(directory.resolve("saves"))) {
                require(entries.findAny().isEmpty(), "Existing save forbidden");
            }
        }
        packaged = FactoryControlRuntimeProof.verify(mc);
        require(Files.readString(directory.resolve(".iedctr-factory-control-xvfb")).equals(nonce + "\n" + System.getenv("DISPLAY")),
            "Xvfb ownership marker differs");
        x11 = NativeLibrary.getInstance("X11");
        xtest = NativeLibrary.getInstance("Xtst");
        display = x11.getFunction("XOpenDisplay").invokePointer(new Object[]{null});
        require(display != null, "X11 unavailable");
        String worldName = "iedctr-held-" + nonce;
        mc.options.pauseOnLostFocus = false;
        enter(Stage.WORLD);
        mc.createWorldOpenFlows().createFreshLevel(worldName,
            new LevelSettings(worldName, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(0L, false, false),
            registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }

    private static void setup(Minecraft mc) {
        ServerPlayer p = player(mc);
        var level = p.serverLevel();
        p.setGameMode(GameType.SURVIVAL);
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 5; z++)
                level.setBlock(new BlockPos(x,79,z), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(POS, (HOPPER ? ModBlocks.FACTORY_HOPPER.get() : ModBlocks.FACTORY_DROPPER.get())
            .defaultBlockState().setValue(MachineBlocks.FACING, Direction.NORTH), 3);
        require(level.getBlockEntity(POS) instanceof MachineBlockEntity, "Factory machine missing");
        machine = (MachineBlockEntity) level.getBlockEntity(POS);
        require(machine.isEmpty(), "Fresh Factory machine is not empty");
        p.getInventory().clearContent();
        p.connection.teleport(.5,80,3.5,180,24);
    }

    private static ServerPlayer player(Minecraft mc) {
        ServerPlayer p = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
        require(p != null, "Server player missing");
        return p;
    }
    private static boolean matches(Minecraft mc, Predicate<ServerPlayer> check) {
        if (inspection == null) {
            inspection = mc.getSingleplayerServer().submit(() -> check.test(player(mc)));
            return false;
        }
        if (!inspection.isDone()) return false;
        boolean result = inspection.join();
        inspection = null;
        return result;
    }
    private static void enter(Stage next) {
        stage = next;
        ticks = 0;
        inspection = null;
    }
    private static boolean empty(net.minecraft.world.inventory.AbstractContainerMenu menu, int index) {
        return menu.getSlot(index).getItem().isEmpty();
    }
    private static boolean slotLog(net.minecraft.world.inventory.AbstractContainerMenu menu, int index) {
        ItemStack stack = menu.getSlot(index).getItem();
        return stack.is(Items.OAK_LOG) && stack.getCount() == 1;
    }
    private static boolean carriedLog(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        ItemStack stack = menu.getCarried();
        return stack.is(Items.OAK_LOG) && stack.getCount() == 1;
    }
    private static int inventoryLogs(ServerPlayer p) {
        int count = p.containerMenu.getCarried().getCount();
        for (int i = 0; i < machine.getContainerSize(); i++) count += machine.getItem(i).getCount();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) count += p.getInventory().getItem(i).getCount();
        return count;
    }
    private static int worldLogs(ServerPlayer p) {
        int count = 0;
        for (ItemEntity item : p.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(12))) {
            require(item.getItem().is(Items.OAK_LOG), "Unexpected world item type");
            count += item.getItem().getCount();
        }
        return count;
    }
    private static void geometry(Minecraft mc, MachineScreen screen, MachineMenu menu, int index) {
        MachineKind kind = HOPPER ? MachineKind.FACTORY_HOPPER : MachineKind.FACTORY_DROPPER;
        require(menu.kind() == kind && menu.slots.size() == index + 36
            && index == (HOPPER ? 18 : 15), "Factory slot topology differs");
        require(mc.getWindow().getGuiScale() == SCALE && mc.options.guiScale().get() == SCALE,
            "Actual GUI scale differs");
        require(mc.getWindow().getWidth() == 1280 && mc.getWindow().getHeight() == 720,
            "Framebuffer differs");
        require((HOPPER && menu.layout().imageWidth() == 176 && menu.layout().imageHeight() == 166)
            || (!HOPPER && menu.layout().imageWidth() == 228 && menu.layout().imageHeight() == 250),
            "Runtime layout unexpectedly changed");
        var slot = menu.getSlot(index);
        require(slot.getItem() != null && slot.x >= 0 && slot.y >= 0, "Invalid player slot");
        // One inventory index only; the source geometry and all other slots are unchanged.
        require(slot.x == (HOPPER ? 8 : menu.layout().playerSlotX(0))
            && slot.y == (HOPPER ? 71 : menu.layout().playerSlotY(0)),
            "Player slot does not use registered coordinates");
    }
    private static double slotX(MachineScreen screen, MachineMenu menu, int index) {
        return screen.getGuiLeft() + menu.getSlot(index).x + 8;
    }
    private static double slotY(MachineScreen screen, MachineMenu menu, int index) {
        return screen.getGuiTop() + menu.getSlot(index).y + 8;
    }
    /** XTest physical motion/click, observed through the actual GLFW cursor. */
    private static boolean clickAt(Minecraft mc, MachineScreen screen, double x, double y) {
        long window = mc.getWindow().getWindow();
        if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) != GLFW.GLFW_TRUE) {
            GLFW.glfwFocusWindow(window);
            return false;
        }
        double observedX = mc.mouseHandler.xpos() * screen.width / mc.getWindow().getScreenWidth();
        double observedY = mc.mouseHandler.ypos() * screen.height / mc.getWindow().getScreenHeight();
        if (Math.abs(observedX - x) >= 1 || Math.abs(observedY - y) >= 1) {
            int[] wx = {0}, wy = {0};
            GLFW.glfwGetWindowPos(window, wx, wy);
            int px = wx[0] + (int)Math.round(x * mc.getWindow().getScreenWidth() / screen.width);
            int py = wy[0] + (int)Math.round(y * mc.getWindow().getScreenHeight() / screen.height);
            require(xtest.getFunction("XTestFakeMotionEvent").invokeInt(
                new Object[]{display, -1, px, py, new NativeLong(0)}) != 0, "Physical motion failed");
            x11.getFunction("XFlush").invokeInt(new Object[]{display});
            return false;
        }
        nativeClick(1);
        pointerX = observedX;
        pointerY = observedY;
        physicalClicks++;
        lastClick = System.nanoTime();
        return true;
    }
    private static boolean settled() {
        return ticks >= 8 && System.nanoTime() - lastClick >= 350_000_000L;
    }
    private static void nativeClick(int button) {
        require(xtest.getFunction("XTestFakeButtonEvent").invokeInt(
            new Object[]{display, button, 1, new NativeLong(0)}) != 0, "Physical press failed");
        require(xtest.getFunction("XTestFakeButtonEvent").invokeInt(
            new Object[]{display, button, 0, new NativeLong(0)}) != 0, "Physical release failed");
        x11.getFunction("XFlush").invokeInt(new Object[]{display});
    }
    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = directory.resolve("factory-held-" + name + ".png");
        require(!Files.exists(file), "Stale screenshot");
        try (var png = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            png.writeToFile(file);
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("name", name);
        entry.addProperty("file", file.getFileName().toString());
        entry.addProperty("sha256", FactoryControlRuntimeProof.hash(Files.readAllBytes(file)));
        entry.addProperty("gui_scale", mc.getWindow().getGuiScale());
        entry.addProperty("pointer_screen_x", pointerX);
        entry.addProperty("pointer_screen_y", pointerY);
        entry.addProperty("menu_id", mc.player.containerMenu.containerId);
        captures.add(entry);
    }
    private static void finish(Minecraft mc, Throwable error) {
        complete = true;
        if (display != null) {
            x11.getFunction("XCloseDisplay").invokeInt(new Object[]{display});
            display = null;
        }
        try {
            if (directory != null) {
                JsonObject proof = new JsonObject();
                proof.addProperty("suite", "native-factory-held-stack-v1");
                proof.addProperty("status", error == null ? "passed" : "failed");
                proof.addProperty("nonce", nonce);
                proof.addProperty("executed_source_sha", System.getProperty("iedctr.executedSourceSha"));
                proof.add("packaged_runtime", packaged);
                proof.addProperty("machine", HOPPER ? "hopper" : "dropper");
                proof.addProperty("selected_language", mc.getLanguageManager().getSelected());
                proof.addProperty("width", mc.getWindow().getWidth());
                proof.addProperty("height", mc.getWindow().getHeight());
                proof.addProperty("gui_scale", mc.getWindow().getGuiScale());
                proof.addProperty("opened_via_native_block_interaction", opened);
                proof.addProperty("physical_left_clicks", physicalClicks);
                proof.addProperty("server_client_pickups", picked);
                proof.addProperty("inert_border_preserved", borderPreserved);
                proof.addProperty("server_client_returns", returned);
                proof.addProperty("outside_item_entity_confirmed", outsideDropped);
                proof.addProperty("outside_witness_consumes_stack_by_design", true);
                proof.addProperty("native_visual_acceptance", false);
                proof.addProperty("input_method",
                    "XTest physical click on slot, visible inert border, return, re-pick, real exterior with world-drop witness");
                proof.add("captures", captures);
                if (error != null) proof.addProperty("error", error.toString());
                Files.writeString(directory.resolve("factory-held-proof.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
            }
        } catch (Exception ex) {
            EngineersDecorReforged.LOGGER.error("Factory held-stack proof write failed", ex);
        }
        if (error == null) EngineersDecorReforged.LOGGER.info("IEDCTR_FACTORY_HELD PASSED nonce={}", nonce);
        else EngineersDecorReforged.LOGGER.error("IEDCTR_FACTORY_HELD FAILED at " + stage, error);
        mc.stop();
    }
    private static void require(boolean condition, String reason) {
        FactoryControlRuntimeProof.require(condition, reason);
    }
}
