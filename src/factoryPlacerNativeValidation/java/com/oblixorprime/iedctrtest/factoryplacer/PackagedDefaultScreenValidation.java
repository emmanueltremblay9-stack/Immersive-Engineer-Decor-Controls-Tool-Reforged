package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.FactoryPlacerV2ReferenceScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Separate flag-absent packaged smoke. Never changes machine logic or global feature defaults. */
@EventBusSubscriber(modid = FactoryPlacerValidationMod.MOD_ID, value = Dist.CLIENT)
public final class PackagedDefaultScreenValidation {
    private static final BlockPos POS = new BlockPos(0, 80, 0);
    private enum Stage { BOOT, WORLD, OPEN, PICKED, RETURNED }
    private static Stage stage = Stage.BOOT;
    private static boolean complete;
    private static long deadline, pickupTime;
    private static int ticks, menuId, pickups, returns;
    private static UUID playerId;
    private static Path directory;
    private static String nonce;
    private static JsonObject packaged;
    private static MachineBlockEntity machine;
    private static CompletableFuture<?> pending;
    private static CompletableFuture<Boolean> inspection;
    private PackagedDefaultScreenValidation() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("iedctr.factoryPlacerPackagedDefaultNative") || complete) return;
        var mc = Minecraft.getInstance();
        try {
            if (deadline == 0) deadline = System.nanoTime() + 900_000_000_000L;
            require(System.nanoTime() < deadline && ++ticks < 1800, "Default packaged smoke timeout at " + stage);
            require(System.getProperty(MachineMenu.DEV_FACTORY_PLACER_V2_REFERENCE_PROPERTY) == null,
                "Ordinary-client smoke requires the historical V2 property to be absent, not false");
            require(!Boolean.getBoolean("iedctr.factoryPlacerV2ReferenceNative") && !Boolean.getBoolean("iedctr.clientGameplay"),
                "Conflicting fixture enabled");
            if (stage == Stage.BOOT) {
                if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) boot(mc);
                return;
            }
            if (stage == Stage.WORLD) {
                if (mc.level == null || mc.player == null || mc.gameMode == null || mc.screen != null) return;
                require(mc.getSingleplayerServer() != null && mc.player.connection.getConnection().isMemoryConnection(), "Only integrated worlds permitted");
                playerId = mc.player.getUUID();
                pending = mc.getSingleplayerServer().submit(() -> setup(mc)); enter(Stage.OPEN); return;
            }
            require(mc.level != null && mc.player != null && mc.getSingleplayerServer() != null, "World disconnected");
            if (pending != null) { if (!pending.isDone()) return; pending.join(); pending = null; }
            if (mc.getOverlay() != null) return;
            // The server task can finish before the client receives the open-screen packet.
            if (mc.screen == null) return;
            require(mc.screen instanceof FactoryPlacerV2ReferenceScreen,
                "Unexpected default screen: " + mc.screen.getClass().getName());
            require(mc.player.containerMenu instanceof MachineMenu, "Default menu type differs");
            var menu = (MachineMenu) mc.player.containerMenu;
            require(menu.factoryPlacerV2ReferencePresentation() && menu.slots.size() == 54 && menu.visibleMachineSlots() == 18,
                "Default menu projection changed");
            if (menu.containerId != menuId) return;
            if (stage == Stage.OPEN) {
                if (ticks < 20 || !stone(menu.getSlot(0).getItem()) || !menu.getCarried().isEmpty()
                        || !matches(mc, p -> stone(machine.getItem(0)) && p.containerMenu.getCarried().isEmpty() && defaults(p))) return;
                require(menu.field(0) == 6 && menu.field(2) == 0, "Default client state changed");
                mc.gameMode.handleInventoryMouseClick(menuId, 0, 0, ClickType.PICKUP, mc.player);
                pickupTime = System.nanoTime(); enter(Stage.PICKED);
            } else if (stage == Stage.PICKED) {
                if (ticks < 6 || System.nanoTime() - pickupTime < 300_000_000L || !menu.getSlot(0).getItem().isEmpty() || !stone(menu.getCarried())
                        || !matches(mc, p -> machine.getItem(0).isEmpty() && stone(p.containerMenu.getCarried()) && defaults(p))) return;
                pickups++;
                mc.gameMode.handleInventoryMouseClick(menuId, 0, 0, ClickType.PICKUP, mc.player); enter(Stage.RETURNED);
            } else if (stage == Stage.RETURNED) {
                if (ticks < 10 || !stone(menu.getSlot(0).getItem()) || !menu.getCarried().isEmpty()
                        || !matches(mc, p -> stone(machine.getItem(0)) && p.containerMenu.getCarried().isEmpty() && defaults(p))) return;
                returns++;
                packaged = PackagedRuntimeProof.verify(mc);
                var capture = directory.resolve("factory-placer-packaged-default.png");
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(capture); }
                finish(mc, null);
            }
        } catch (Throwable error) { finish(mc, error); }
    }

    private static void boot(Minecraft mc) throws Exception {
        nonce = System.getProperty("iedctr.clientGameplayNonce", "");
        require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha", "").matches("[a-f0-9]{40}"), "Missing run identity");
        directory = mc.gameDirectory.toPath().toRealPath();
        require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory", "")).toRealPath())
            && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce), "Isolation marker mismatch");
        require(mc.level == null && mc.getSingleplayerServer() == null
            && !Files.exists(directory.resolve("client-factory-placer-packaged-default-result.json"))
            && !Files.exists(directory.resolve("factory-placer-packaged-default.png")), "Refusing existing world/proof");
        if (Files.exists(directory.resolve("saves"))) try (var entries = Files.list(directory.resolve("saves"))) {
            require(entries.findAny().isEmpty(), "Existing saves forbidden");
        }
        packaged = PackagedRuntimeProof.verify(mc);
        String name = "iedctr-factory-placer-default-" + nonce;
        mc.options.pauseOnLostFocus = false; enter(Stage.WORLD);
        mc.createWorldOpenFlows().createFreshLevel(name,
            new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(0L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }
    private static void setup(Minecraft mc) {
        var p = player(mc); p.setGameMode(GameType.SURVIVAL); var level = p.serverLevel();
        for (int x = -2; x <= 2; x++) for (int z = -1; z <= 6; z++) level.setBlock(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState(), 3);
        p.connection.teleport(.5, 80, 4.5, 180, 18);
        // An obstructed target prevents automatic placement without altering default logic=6.
        level.setBlock(POS.south(), Blocks.BEDROCK.defaultBlockState(), 3);
        level.setBlock(POS, ModBlocks.FACTORY_PLACER.get().defaultBlockState().setValue(MachineBlocks.FACING, Direction.SOUTH), 3);
        require(level.getBlockEntity(POS) instanceof MachineBlockEntity, "Missing Factory Placer");
        machine = (MachineBlockEntity) level.getBlockEntity(POS);
        require(machine.getContainerSize() == 27 && machine.isEmpty(), "Unexpected initial storage");
        machine.setItem(0, new ItemStack(Items.STONE, 1)); p.getInventory().clearContent(); p.openMenu(machine);
        require(p.containerMenu instanceof MachineMenu m && !m.factoryPlacerV2ReferencePresentation() && defaults(p), "Default server state differs");
        menuId = p.containerMenu.containerId;
    }
    private static boolean defaults(ServerPlayer p) {
        if (!(p.containerMenu instanceof MachineMenu menu) || menu.factoryPlacerV2ReferencePresentation() || menu.field(0) != 6 || menu.field(2) != 0) return false;
        for (int i = 1; i < 27; i++) if (!machine.getItem(i).isEmpty()) return false;
        return p.getInventory().isEmpty() && p.serverLevel().getBlockState(POS.south()).is(Blocks.BEDROCK)
            && p.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(12)).isEmpty();
    }
    private static boolean matches(Minecraft mc, Predicate<ServerPlayer> check) {
        if (inspection == null) { inspection = mc.getSingleplayerServer().submit(() -> check.test(player(mc))); return false; }
        if (!inspection.isDone()) return false;
        boolean result = inspection.join(); inspection = null; return result;
    }
    private static ServerPlayer player(Minecraft mc) {
        var p = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId); require(p != null, "Player vanished"); return p;
    }
    private static boolean stone(ItemStack stack) { return stack.is(Items.STONE) && stack.getCount() == 1; }
    private static void enter(Stage next) { stage = next; ticks = 0; inspection = null; }
    private static void finish(Minecraft mc, Throwable error) {
        complete = true;
        try {
            if (directory != null) {
                var proof = new JsonObject();
                proof.addProperty("suite", "native-factory-placer-packaged-default-v1");
                proof.addProperty("status", error == null ? "passed_bounded_smoke" : "failed");
                proof.addProperty("nonce", nonce); proof.addProperty("executed_source_sha", System.getProperty("iedctr.executedSourceSha"));
                proof.add("packaged_runtime", packaged);
                proof.addProperty("v2_property_absent", System.getProperty(MachineMenu.DEV_FACTORY_PLACER_V2_REFERENCE_PROPERTY) == null);
                proof.addProperty("actual_integrated_world", mc.getSingleplayerServer() != null);
                proof.addProperty("native_visual_acceptance", false);
                proof.addProperty("screen_class", mc.screen == null ? "none" : mc.screen.getClass().getName());
                proof.addProperty("client_v2_presentation", mc.player != null && mc.player.containerMenu instanceof MachineMenu menu && menu.factoryPlacerV2ReferencePresentation());
                proof.addProperty("server_v2_presentation", false);
                proof.addProperty("menu_slot_count", 54); proof.addProperty("machine_saved_slot_count", 27); proof.addProperty("machine_visible_slot_count", 18);
                proof.addProperty("world_drop_count", 0);
                proof.addProperty("default_logic", 6); proof.addProperty("default_selected_slot", 0);
                proof.addProperty("pickup_server_acks", pickups); proof.addProperty("return_server_acks", returns);
                proof.addProperty("initial_item_total", 1); proof.addProperty("final_item_total", error == null ? 1 : -1);
                proof.addProperty("input_method", "Minecraft.gameMode.handleInventoryMouseClick with vanilla packets and independent server observation");
                proof.addProperty("selected_language", mc.getLanguageManager().getSelected());
                proof.addProperty("width", mc.getWindow().getWidth()); proof.addProperty("height", mc.getWindow().getHeight());
                if (error == null) {
                    proof.addProperty("capture", "factory-placer-packaged-default.png");
                    proof.addProperty("capture_sha256", PackagedRuntimeProof.hash(Files.readAllBytes(directory.resolve("factory-placer-packaged-default.png"))));
                } else proof.addProperty("error", error.toString());
                Files.writeString(directory.resolve("client-factory-placer-packaged-default-result.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
            }
        } catch (Exception writeError) { EngineersDecorReforged.LOGGER.error("Default packaged proof write failed", writeError); }
        if (error == null) EngineersDecorReforged.LOGGER.info("IEDCTR_FACTORY_PLACER_PACKAGED_DEFAULT PASSED nonce={}", nonce);
        else EngineersDecorReforged.LOGGER.error("IEDCTR_FACTORY_PLACER_PACKAGED_DEFAULT FAILED at " + stage, error);
        mc.stop();
    }
    private static void require(boolean condition, String message) { PackagedRuntimeProof.require(condition, message); }
}
