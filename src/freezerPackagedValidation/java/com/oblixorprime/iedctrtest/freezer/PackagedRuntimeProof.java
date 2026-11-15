package com.oblixorprime.iedctrtest.freezer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.client.FreezerReferenceScreen;
import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceLiveState;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineLayout;
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
public final class PackagedRuntimeProof {
    private PackagedRuntimeProof() {}
    public static JsonObject verify(Minecraft mc) throws Exception {
        java.util.Set<String> allowed = java.util.Set.of("iedctr.clientGameplay", "iedctr.freezerReferenceOnly", "iedctr.freezerPackagedNative");
        for (String key : allowed) require("true".equals(System.getProperty(key)), "Required explicit selector missing: " + key);
        for (String key : System.getProperties().stringPropertyNames()) {
            if (key.startsWith("iedctr.dev.") || key.endsWith("Only") || key.endsWith("Native") || key.endsWith("Validation"))
                require(allowed.contains(key), "Unexpected selector: " + key);
        }
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
        var harness = ModList.get().getModFileById(FreezerValidationMod.MOD_ID);
        require(harness != null && harness.getFile() != mod, "Harness is not a separate mod file");
        String rootUri = mod.getSecureJar().getRootPath().toUri().toString();
        var origins = new JsonArray();
        try (var zip = new ZipFile(actual.toFile())) {
            require(zip.stream().map(e -> e.getName()).distinct().count() == zip.size(), "Duplicate entries in normal JAR");
            require(zip.stream().noneMatch(e -> e.getName().startsWith("com/oblixorprime/iedctrtest/")
                || e.getName().contains("PackagedRuntimeProof")), "Harness leaked into normal JAR");
            for (Class<?> type : new Class<?>[] {EngineersDecorReforged.class, FreezerReferenceScreen.class,
                    FreezerReferenceGeometry.class, FreezerReferenceLiveState.class, MachineMenu.class, MachineLayout.class, MachineBlockEntity.class, Class.forName("com.oblixorprime.engineersdecorreforged.client.validation.NativeFreezerReferenceValidation"), Class.forName("com.oblixorprime.engineersdecorreforged.client.validation.NativeClientMiningValidation")}) {
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
                    && type.getModule() != FreezerValidationMod.class.getModule(), "Production/harness module isolation differs");
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
            var textures = new JsonArray();
            String[] paths = {"freezer_reference_v5/background.png", "freezer_reference_v5/freezer_v5_chamber_clean_base.png", "freezer_reference_v5/freezer_v5_chamber_mask_alpha.png", "freezer_reference_v5/freezer_v5_frost_overlay_phase0.png", "freezer_reference_v5/freezer_v5_frost_overlay_phase1.png", "freezer_reference_v5/freezer_v5_frost_overlay_phase2.png", "freezer_reference_v5/freezer_v5_frost_overlay_phase3.png", "freezer_reference_v5/freezer_v5_frost_overlay_phase4.png"};
            String[] hashes = {"68c1dc11e7d0e920addebca18100892861a70c5d68db5547b8014235acee3d46", "c5c5050e15adba77d351f4b4bd2f22af95c286849bb933dc4a894997d4bb8a0c", "625c5e313d52be467e8912702325adb13e2e8babb2d51ab5e89ecc8233f1f6e0", "eae6cf6edbc79b1ed3827d98c9617e78715690260b0e1dd99a1c367bc1ba2664", "5c4a62addce2de30daacfe41b5f12e527ab7d45c5a62a4665f57bfdbb082c14b", "ddd50c1a598e716e146485c1a8e4c3c9b5862b0810952d5b9ca1798089a89898", "3e7055a20030cf5349c85238a48e7bb8d48e6a8108aa712bad5ff756c33baa49", "b35039c033225dc86b830c05088e7eef06ea44004f7f9ab85a374fe9a23f4fe0"};
            for(int i=0;i<paths.length;i++) {
                var texture = ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID,"textures/gui/"+paths[i]);
                var active = mc.getResourceManager().getResource(texture).orElseThrow();
                require(active.sourcePackId().equals("mod/"+EngineersDecorReforged.MOD_ID),"Artwork overridden by another resource pack");
                var entry=zip.getEntry("assets/"+texture.getNamespace()+"/"+texture.getPath());
                require(entry!=null,"Missing retained artwork");
                try(var input=active.open();var archived=zip.getInputStream(entry)) {
                    String digest=hash(input.readAllBytes());
                    require(digest.equals(hash(archived.readAllBytes()))&&digest.equals(hashes[i]),"Active artwork differs from retained source");
                    var row=new JsonObject();row.addProperty("path",texture.getPath());row.addProperty("sha256",digest);
                    row.addProperty("pack",active.sourcePackId());textures.add(row);
                }
            }
            var proof = new JsonObject();
            proof.addProperty("jar_path", actual.toString()); proof.addProperty("jar_sha256", expectedHash);
            proof.addProperty("mod_root_uri", rootUri); proof.addProperty("harness_mod_id", FreezerValidationMod.MOD_ID);
            proof.addProperty("harness_mod_path", harness.getFile().getFilePath().toString());
            // Historical opt-in production validation classes may exist. This flag only excludes the separate harness namespace.
            proof.addProperty("fixture_free_jar", false);
            proof.addProperty("separate_provenance_harness", true);
            proof.addProperty("fixture_scope", "Separate com/oblixorprime/iedctrtest harness excluded; historical bundled Freezer/world fixture explicitly enabled; no physical OS input claim"); proof.addProperty("separate_harness_module", true);
            proof.add("active_textures",textures);proof.addProperty("production_selectors_absent",false);
            var selectors = new JsonArray(); for (String key : allowed.stream().sorted().toList()) selectors.add(key);
            proof.add("production_selectors",selectors); proof.addProperty("presentation_mode", "normal-dynamic"); proof.addProperty("dev_selectors_absent", true);
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
