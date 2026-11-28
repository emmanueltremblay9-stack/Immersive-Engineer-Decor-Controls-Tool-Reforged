package com.oblixorprime.iedctrtest.milking;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.client.MachineScreen;
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
        for (String key : System.getProperties().stringPropertyNames()) {
            require(!key.startsWith("iedctr.") || !key.endsWith("Native") || key.equals("iedctr.milkingPackagedNative"),
                "Other native validation selector must be absent: " + key);
            require(!key.startsWith("iedctr.dev.") && !key.equals("iedctr.fluidGestureNative") && !key.equals("iedctr.barrelPackagedNative") && !key.equals("iedctr.barrelReferenceNative")
                && !key.equals("iedctr.factoryPlacerV2ReferenceNative") && !key.equals("iedctr.fluidPackagedNative")
                && !key.equals("iedctr.clientGameplay") && !key.equals("iedctr.smelterGuiOnly")
                && !key.equals("iedctr.correctedMachineGuiOnly")
                && !key.endsWith("Only") && !key.endsWith("Validation"), "Production selector/legacy driver must be absent: " + key);
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
        var harness = ModList.get().getModFileById(MilkingValidationMod.MOD_ID);
        require(harness != null && harness.getFile() != mod, "Harness is not a separate mod file");
        String rootUri = mod.getSecureJar().getRootPath().toUri().toString();
        var origins = new JsonArray();
        try (var zip = new ZipFile(actual.toFile())) {
            require(zip.stream().map(e -> e.getName()).distinct().count() == zip.size(), "Duplicate entries in normal JAR");
            require(zip.stream().noneMatch(e -> e.getName().startsWith("com/oblixorprime/iedctrtest/")
                || e.getName().contains("client/validation/NativeMilkingPackagedValidation")), "Harness leaked into normal JAR");
            for (Class<?> type : new Class<?>[] {EngineersDecorReforged.class, MachineScreen.class, MachineScreen.SmallMilkingMachineScreen.class, MachineMenu.class, MachineLayout.class, MachineBlockEntity.class}) {
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
                    && type.getModule() != MilkingValidationMod.class.getModule(), "Production/harness module isolation differs");
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
            String[] paths = {"milking_redesign_v1/background.png","milking_redesign_v1/milk.png","gui11_v5c/12.png"};
            String[] hashes = {"1405a002a4cf0ece130e1a77befa8c947891e4c6d6b96d104b7177646d060411","f6f0507deb081a2a83b6bb455abc6237ac2b41f4ab21d43c833e356fb2d9bbe5","82e56adfc0307552596cf64a6e50e15b1449f50546819c64d93839ae2c18fffa"};
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
            proof.addProperty("mod_root_uri", rootUri); proof.addProperty("harness_mod_id", MilkingValidationMod.MOD_ID);
            proof.addProperty("harness_mod_path", harness.getFile().getFilePath().toString());
            // Historical opt-in production validation classes may exist. This flag only excludes the separate harness namespace.
            proof.addProperty("fixture_free_jar", true);
            proof.addProperty("fixture_free_scope", "Separate com/oblixorprime/iedctrtest harness excluded; historical main validation classes may exist but their activation selectors are absent"); proof.addProperty("separate_harness_module", true);
            proof.add("active_textures",textures);proof.addProperty("production_selectors_absent",true);
            proof.add("production_selectors",new JsonArray());
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
