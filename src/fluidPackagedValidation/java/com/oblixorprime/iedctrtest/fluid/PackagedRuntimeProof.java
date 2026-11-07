package com.oblixorprime.iedctrtest.fluid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.client.MachineScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.FluidMachineRedesignLayout;
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
            require(!key.startsWith("iedctr.dev.") && !key.equals("iedctr.fluidGestureNative")
                && !key.equals("iedctr.factoryPlacerV2ReferenceNative"), "Production selector/legacy driver must be absent: " + key);
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
        var harness = ModList.get().getModFileById(FluidValidationMod.MOD_ID);
        require(harness != null && harness.getFile() != mod, "Harness is not a separate mod file");
        String rootUri = mod.getSecureJar().getRootPath().toUri().toString();
        var origins = new JsonArray();
        try (var zip = new ZipFile(actual.toFile())) {
            require(zip.stream().map(e -> e.getName()).distinct().count() == zip.size(), "Duplicate entries in normal JAR");
            require(zip.stream().noneMatch(e -> e.getName().startsWith("com/oblixorprime/iedctrtest/")
                || e.getName().contains("NativeFluidGestureValidation")), "Harness leaked into normal JAR");
            for (Class<?> type : new Class<?>[] {EngineersDecorReforged.class, MachineScreen.class,
                    MachineScreen.SmallFluidFunnelScreen.class, MachineScreen.PassiveFluidAccumulatorScreen.class, MachineMenu.class, FluidMachineRedesignLayout.class, MachineBlockEntity.class,
                    Class.forName("com.oblixorprime.engineersdecorreforged.client.RetainedFluidMachineArtwork")}) {
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
                    && type.getModule() != FluidValidationMod.class.getModule(), "Production/harness module isolation differs");
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
            String[] paths = {"small_fluid_funnel_v1/background.png","passive_fluid_accumulator_v1/background.png"};
            String[] hashes = {"c5dcc2ee56fdace5f9bf66456e0a4dca91ce4fddb78887a9a6b60ff914336a64","da2a83c0f8cc68be5f69ad4f3cc392cc4a600206ae39ac8b81142e421e466872"};
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
            proof.addProperty("mod_root_uri", rootUri); proof.addProperty("harness_mod_id", FluidValidationMod.MOD_ID);
            proof.addProperty("harness_mod_path", harness.getFile().getFilePath().toString());
            proof.addProperty("fixture_free_jar", true); proof.addProperty("separate_harness_module", true);
            proof.add("active_textures",textures);proof.addProperty("production_selectors_absent",true);
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
