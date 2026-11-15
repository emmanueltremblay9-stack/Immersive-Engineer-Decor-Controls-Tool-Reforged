package com.oblixorprime.iedctrtest.smelter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Separate bootstrap only. Retains the original native Smelter fixture unmodified. */
@EventBusSubscriber(modid = SmelterValidationMod.MOD_ID, value = Dist.CLIENT)
public final class SmelterPackagedDriver {
    private static int stage, ticks;
    private static long deadline;
    private static Path directory;
    private static String nonce;
    private static JsonObject before;
    private static boolean isolatedDirectoryValidated;
    private static CompletableFuture<?> supportReady;
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("iedctr.smelterPackagedNative") || stage == 3) return;
        var mc = Minecraft.getInstance();
        try {
            if (deadline == 0) deadline = System.nanoTime() + 900_000_000_000L;
            require(System.nanoTime() < deadline && ++ticks < 36000, "Bounded Smelter client timeout");
            if (stage == 0) {
                if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null) return;
                directory = mc.gameDirectory.toPath().toRealPath();
                nonce = System.getProperty("iedctr.clientGameplayNonce", "");
                require(nonce.matches("[a-f0-9]{32}") && directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory", "")).toRealPath())
                    && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce), "Isolated directory/nonce mismatch");
                require(mc.level == null && mc.getSingleplayerServer() == null
                    && !Files.exists(directory.resolve("client-smelter-gui-result.json"))
                    && !Files.exists(directory.resolve("smelter-gui-screenshots")), "Existing runtime/evidence forbidden");
                if (Files.exists(directory.resolve("saves"))) try (var saves = Files.list(directory.resolve("saves"))) {
                    require(saves.findAny().isEmpty(), "Existing saves forbidden");
                }
                isolatedDirectoryValidated = true;
                before = PackagedRuntimeProof.verify(mc);
                require(mc.getWindow().getWidth() == Integer.getInteger("iedctr.requestedWidth", 0)
                    && mc.getWindow().getHeight() == Integer.getInteger("iedctr.requestedHeight", 0)
                    && mc.options.guiScale().get() == 1, "Requested viewport mismatch");
                String name = "iedctr-smelter-packaged-" + nonce;
                stage = 1;
                mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(0L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
            } else if (stage == 1) {
                if (mc.level == null || mc.player == null || mc.gameMode == null || mc.screen != null) return;
                require(mc.getSingleplayerServer() != null && mc.player.connection.getConnection().isMemoryConnection(), "Integrated world required");
                // The retained fixture teleports to (.5,81,3.5); provide support before
                // opening its distance-checked menu. Never alter production stillValid.
                var playerId = mc.player.getUUID();
                supportReady = mc.getSingleplayerServer().submit(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                    require(player != null, "Player missing while preparing fixture support");
                    for (int x = -1; x <= 1; x++) for (int z = 2; z <= 4; z++)
                        player.serverLevel().setBlock(new BlockPos(x, 80, z), Blocks.STONE.defaultBlockState(), 3);
                });
                stage = 4;
            } else if (stage == 4) {
                if (!supportReady.isDone()) return;
                supportReady.join();
                NativeSmelterGuiValidation.begin(mc, directory, nonce);
                stage = 2;
            } else if (NativeSmelterGuiValidation.tick(mc, directory, nonce)) {
                var after = PackagedRuntimeProof.verify(mc);
                require(before.equals(after), "Runtime provenance changed");
                var result = directory.resolve("client-smelter-gui-result.json");
                var proof = JsonParser.parseString(Files.readString(result)).getAsJsonObject();
                proof.add("packaged_runtime", before);
                proof.add("packaged_runtime_after", after);
                Files.writeString(result, new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
                stage = 3;
                System.out.println("IEDCTR_SMELTER_GUI PASSED nonce=" + nonce);
                mc.stop();
            }
        } catch (Throwable error) {
            if (isolatedDirectoryValidated) writeFailure(mc, error);
            stage = 3;
            error.printStackTrace();
            System.err.println("IEDCTR_SMELTER_GUI FAILED nonce=" + nonce);
            // No success marker; the Python gate rejects failure even if the client exits normally.
            if (isolatedDirectoryValidated) mc.stop();
        }
    }
    private static void writeFailure(Minecraft mc, Throwable error) {
        try {
            var report = new JsonObject();
            report.addProperty("status", "failed");
            report.addProperty("nonce", nonce);
            report.addProperty("bootstrap_stage", stage);
            report.addProperty("error", error.toString());
            report.addProperty("screen", mc.screen == null ? "null" : mc.screen.getClass().getName());
            if (mc.player != null) {
                report.add("client", playerState(mc.player));
                var id = mc.player.getUUID();
                var server = mc.getSingleplayerServer();
                if (server != null) try { report.add("server", server.submit(() -> {
                    var player = server.getPlayerList().getPlayer(id);
                    return player == null ? new JsonObject() : playerState(player);
                }).get(3, TimeUnit.SECONDS));
                } catch (Exception unavailable) { report.addProperty("server_observation_error", unavailable.toString()); }
            }
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                var capture = directory.resolve("smelter-failure.png");
                require(!Files.exists(capture), "Refusing to overwrite failure capture");
                image.writeToFile(capture);
                report.addProperty("failure_capture_sha256", PackagedRuntimeProof.hash(Files.readAllBytes(capture)));
            } catch (Exception unavailable) { report.addProperty("capture_error", unavailable.toString()); }
            Files.writeString(directory.resolve("smelter-failure-diagnostic.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
        } catch (Throwable diagnosticError) { diagnosticError.printStackTrace(); }
    }
    private static JsonObject playerState(net.minecraft.world.entity.player.Player player) {
        var state = new JsonObject();
        state.addProperty("x", player.getX()); state.addProperty("y", player.getY()); state.addProperty("z", player.getZ());
        state.addProperty("menu", player.containerMenu.getClass().getName());
        state.addProperty("menu_id", player.containerMenu.containerId);
        state.addProperty("flying", player.getAbilities().flying);
        if (player.containerMenu instanceof MachineMenu menu) {
            state.addProperty("valid", menu.stillValid(player));
            var data = new com.google.gson.JsonArray();
            for (int i = 0; i < MachineMenu.SMALL_MINERAL_SMELTER_V05_DATA_COUNT; i++) data.add(menu.field(i));
            state.add("fields", data);
        }
        return state;
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
