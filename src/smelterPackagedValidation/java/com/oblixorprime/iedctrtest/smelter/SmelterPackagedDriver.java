package com.oblixorprime.iedctrtest.smelter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
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
            stage = 3;
            error.printStackTrace();
            System.err.println("IEDCTR_SMELTER_GUI FAILED nonce=" + nonce);
            // No success marker; the Python gate rejects failure even if the client exits normally.
            if (isolatedDirectoryValidated) mc.stop();
        }
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
