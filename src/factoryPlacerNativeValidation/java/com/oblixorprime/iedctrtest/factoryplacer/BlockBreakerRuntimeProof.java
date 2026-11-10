package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.client.MachineScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipFile;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/** Fails closed if production code or the active artwork comes from source output or another pack. */
public final class BlockBreakerRuntimeProof {
    private BlockBreakerRuntimeProof() {}
    public static JsonObject verify(Minecraft mc) throws Exception {
        String expectedHash = System.getProperty("iedctr.expectedModJarSha256", "");
        require(expectedHash.matches("[a-f0-9]{64}"), "Missing expected normal JAR SHA-256");
        String expectedName = System.getProperty("iedctr.expectedModJar", "");
        require(!expectedName.isBlank(), "Missing expected normal JAR path");
        Path expected = Path.of(expectedName).toRealPath();
        require(Files.isRegularFile(expected) && expected.toString().endsWith(".jar"), "Expected artifact is not a normal JAR");
        var modInfo = ModList.get().getModFileById(EngineersDecorReforged.MOD_ID);
        require(modInfo != null, "Production mod not loaded");
        var mod = modInfo.getFile();
        Path actual = mod.getFilePath().toRealPath();
        require(actual.equals(expected) && hash(Files.readAllBytes(actual)).equals(expectedHash), "Loaded production artifact differs");
        var harness = ModList.get().getModFileById(FactoryPlacerValidationMod.MOD_ID);
        require(harness != null && harness.getFile() != mod, "Harness is not a separate mod file");
        String rootUri = mod.getSecureJar().getRootPath().toUri().toString();
        var origins = new JsonArray();
        try (var zip = new ZipFile(actual.toFile())) {
            require(zip.stream().noneMatch(e -> e.getName().startsWith("com/oblixorprime/iedctrtest/")
                || e.getName().contains("NativeFactoryPlacerV2ReferenceValidation")), "Harness leaked into normal JAR");
            for (Class<?> type : new Class<?>[] {EngineersDecorReforged.class, MachineScreen.class,
                    MachineScreen.SmallBlockBreakerScreen.class, MachineMenu.class, MachineBlockEntity.class,
                    Class.forName("com.oblixorprime.engineersdecorreforged.client.RedesignedMachineArtwork")}) {
                String entry = type.getName().replace('.', '/') + ".class";
                var url = type.getResource('/' + entry);
                var source = type.getProtectionDomain().getCodeSource();
                require(url != null && source != null, "Missing production class provenance: " + type.getName());
                String resourceUri = url.toURI().toString();
                String codeSource = source.getLocation().toURI().toString();
                // SecureJarHandler uses union: URLs; compare authoritative roots, never substring-match filenames.
                require(url.toURI().equals(mod.findResource(entry).toUri()), "Class resource not from production mod file: " + type.getName());
                require(source.getLocation().toURI().equals(mod.getSecureJar().getRootPath().toUri()) || source.getLocation().toURI().equals(actual.toUri()),
                    "Class code source not the production JAR: " + type.getName() + " (" + codeSource + ", expected " + rootUri + ")");
                require(type.getModule() == EngineersDecorReforged.class.getModule()
                    && type.getModule() != FactoryPlacerValidationMod.class.getModule(), "Production/harness module isolation differs");
                var zipEntry = zip.getEntry(entry);
                require(zipEntry != null, "Missing production class in normal JAR");
                String resourceHash;
                try (var input = url.openStream(); var archive = zip.getInputStream(zipEntry)) {
                    resourceHash = hash(input.readAllBytes());
                    require(resourceHash.equals(hash(archive.readAllBytes())), "Class resource bytes differ from normal JAR");
                }
                var origin = new JsonObject(); origin.addProperty("class", type.getName());
                origin.addProperty("resource_uri", resourceUri); origin.addProperty("code_source_uri", codeSource);
                origin.addProperty("sha256", resourceHash); origins.add(origin);
            }
            var texture = ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID,
                "textures/gui/block_breaker_rp_v3/background.png");
            var active = mc.getResourceManager().getResource(texture).orElseThrow();
            require(active.sourcePackId().equals("mod/" + EngineersDecorReforged.MOD_ID), "Artwork overridden by another resource pack");
            String activeHash;
            try (var input = active.open(); var archived = zip.getInputStream(zip.getEntry("assets/" + texture.getNamespace() + "/" + texture.getPath()))) {
                activeHash = hash(input.readAllBytes());
                require(activeHash.equals(hash(archived.readAllBytes())) && activeHash.equals("2594e227dd153ffa7b1954d8d5d3069e10d0176c0e8142c7f7d2727f315b0a0d"),
                    "Active artwork bytes differ from normal JAR/source reference");
            }
            var powerTexture = ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID,
                "textures/gui/block_breaker_rp_v3/power_neutral.png");
            var powerResource = mc.getResourceManager().getResource(powerTexture).orElseThrow();
            require(powerResource.sourcePackId().equals("mod/" + EngineersDecorReforged.MOD_ID), "Power artwork override");
            String powerHash;
            try (var input = powerResource.open(); var archived = zip.getInputStream(zip.getEntry("assets/" + powerTexture.getNamespace() + "/" + powerTexture.getPath()))) {
                powerHash = hash(input.readAllBytes());
                require(powerHash.equals(hash(archived.readAllBytes())) && powerHash.equals("23ff15c60420b8951117f748d0b6122e174c915f87e91466930af80fbdc12123"), "Power artwork differs");
            }
            var proof = new JsonObject();
            proof.addProperty("active_power_texture_sha256", powerHash);
            proof.addProperty("jar_path", actual.toString()); proof.addProperty("jar_sha256", expectedHash);
            proof.addProperty("mod_root_uri", rootUri); proof.addProperty("harness_mod_id", FactoryPlacerValidationMod.MOD_ID);
            proof.addProperty("harness_mod_path", harness.getFile().getFilePath().toString());
            proof.addProperty("fixture_free_jar", true); proof.addProperty("separate_harness_module", true);
            proof.addProperty("active_texture_sha256", activeHash); proof.addProperty("active_texture_pack", active.sourcePackId());
            proof.add("class_origins", origins);
            return proof;
        }
    }
    static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
