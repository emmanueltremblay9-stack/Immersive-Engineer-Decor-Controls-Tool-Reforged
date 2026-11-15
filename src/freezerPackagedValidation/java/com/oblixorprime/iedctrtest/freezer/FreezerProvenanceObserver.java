package com.oblixorprime.iedctrtest.freezer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Read-only observer of the explicitly enabled bundled fixture. Never drives gameplay. */
@EventBusSubscriber(modid = FreezerValidationMod.MOD_ID, value = Dist.CLIENT)
public final class FreezerProvenanceObserver {
    private static boolean started, finished;
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void before(ClientTickEvent.Post event) { if (!started) observe(); }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void after(ClientTickEvent.Post event) { if (started) observe(); }
    private static void observe() {
        if (!Boolean.getBoolean("iedctr.freezerPackagedNative") || finished) return;
        var mc = Minecraft.getInstance();
        try {
            var directory = mc.gameDirectory.toPath();
            boolean ending = Files.isRegularFile(directory.resolve("client-freezer-reference-result.json"));
            if ((!started && mc.screen instanceof TitleScreen && mc.getOverlay() == null) || ending) {
                var proof = new JsonObject();
                proof.add("packaged_runtime", PackagedRuntimeProof.verify(mc));
                proof.addProperty("nonce", System.getProperty("iedctr.clientGameplayNonce"));
                proof.addProperty("executed_source_sha", System.getProperty("iedctr.executedSourceSha"));
                proof.addProperty("stage", ending ? "after" : "before");
                Files.writeString(directory.resolve("freezer-packaged-" + (ending ? "after" : "before") + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(proof));
                started = true;
                if (ending) finished = true;
            }
        } catch (Exception error) { finished = true; throw new IllegalStateException("Freezer packaged provenance failed", error); }
    }
}
