package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen;
import com.oblixorprime.engineersdecorreforged.utility.FactoryStudyGeometry;
import com.oblixorprime.engineersdecorreforged.utility.FactoryStudyState;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/** Exact packaged proof for the three study GUIs, kept exclusively in the test mod. */
public final class FactoryStudyRuntimeProof {
   private FactoryStudyRuntimeProof() { }

   public static JsonObject verify(Minecraft mc) throws Exception {
      // Existing proof checks normal-JAR identity, absence of fixtures, main menu /
      // machine classes and isolation of the validation mod's module and code root.
      JsonObject proof = FactoryControlRuntimeProof.verify(mc);
      Path actual = Path.of(proof.get("jar_path").getAsString()).toRealPath();
      var mod = ModList.get().getModFileById(EngineersDecorReforged.MOD_ID).getFile();
      var root = mod.getSecureJar().getRootPath().toUri();
      JsonArray origins = proof.getAsJsonArray("class_origins");
      List<Class<?>> classes = new ArrayList<>();
      for (Class<?> type : new Class<?>[] {FactoryStudyScreen.class, FactoryStudyGeometry.class, FactoryStudyState.class}) {
         classes.add(type);
         classes.addAll(List.of(type.getDeclaredClasses()));
      }
      try (ZipFile zip = new ZipFile(actual.toFile())) {
         for (Class<?> type : classes) {
            String entry = type.getName().replace('.', '/') + ".class";
            var url = type.getResource('/' + entry);
            var source = type.getProtectionDomain().getCodeSource();
            require(url != null && source != null, "Missing study class provenance: " + type.getName());
            require(url.toURI().equals(mod.findResource(entry).toUri()),
               "Study class resource is not from production SecureJar: " + type.getName());
            require(source.getLocation().toURI().equals(root) || source.getLocation().toURI().equals(actual.toUri()),
               "Study class code source differs from the normal production JAR: " + type.getName());
            require(type.getModule() == EngineersDecorReforged.class.getModule()
               && type.getModule() != FactoryPlacerValidationMod.class.getModule(),
               "Study class is not isolated from the harness module: " + type.getName());
            var archived = zip.getEntry(entry);
            require(archived != null, "Study class missing from normal JAR: " + entry);
            String classHash;
            try (var input = url.openStream(); var archive = zip.getInputStream(archived)) {
               classHash = hash(input.readAllBytes());
               require(classHash.equals(hash(archive.readAllBytes())),
                  "Loaded study class bytes differ from the normal JAR: " + type.getName());
            }
            JsonObject origin = new JsonObject();
            origin.addProperty("class", type.getName());
            origin.addProperty("resource_uri", url.toURI().toString());
            origin.addProperty("code_source_uri", source.getLocation().toURI().toString());
            origin.addProperty("sha256", classHash);
            origins.add(origin);
         }
         JsonArray textures = new JsonArray();
         for (MachineKind kind : new MachineKind[] {
            MachineKind.FACTORY_HOPPER, MachineKind.FACTORY_DROPPER, MachineKind.SMALL_LAB_FURNACE}) {
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
               EngineersDecorReforged.MOD_ID, FactoryStudyGeometry.texturePath(kind));
            String entry = "assets/" + texture.getNamespace() + "/" + texture.getPath();
            var archived = zip.getEntry(entry);
            require(archived != null, "Approved study texture missing from normal JAR: " + entry);
            var active = mc.getResourceManager().getResource(texture).orElseThrow();
            require(active.sourcePackId().equals("mod/" + EngineersDecorReforged.MOD_ID),
               "Approved study artwork overridden by another resource pack: " + texture);
            byte[] activeBytes;
            String archivedHash;
            try (var input = active.open(); var archive = zip.getInputStream(archived)) {
               activeBytes = input.readAllBytes();
               archivedHash = hash(archive.readAllBytes());
            }
            String activeHash = hash(activeBytes);
            String expectedHash = FactoryStudyGeometry.sourceSha256(kind);
            require(activeHash.equals(archivedHash) && activeHash.equals(expectedHash),
               "Active study artwork differs from the approved source or normal JAR: " + texture);
            JsonObject textureProof = new JsonObject();
            textureProof.addProperty("machine", kind.registryName());
            textureProof.addProperty("resource", texture.toString());
            textureProof.addProperty("jar_entry", entry);
            textureProof.addProperty("active_pack", active.sourcePackId());
            textureProof.addProperty("active_sha256", activeHash);
            textureProof.addProperty("jar_entry_sha256", archivedHash);
            textureProof.addProperty("approved_source_sha256", expectedHash);
            textureProof.addProperty("bytes", activeBytes.length);
            textureProof.addProperty("unchanged_source_bytes", true);
            textures.add(textureProof);
         }
         proof.add("study_textures", textures);
      }

      String baseline = System.getProperty("iedctr.factoryStudySourceBaseline", "");
      String candidateCommit = System.getProperty("iedctr.factoryStudyCandidateCommit", "");
      String manifestHash = System.getProperty("iedctr.factoryStudyWorkingManifestSha256", "");
      String manifestPath = System.getProperty("iedctr.factoryStudyWorkingManifest", "");
      require(baseline.matches("[a-f0-9]{40}"), "Missing source baseline commit for study runtime evidence");
      require(candidateCommit.matches("[a-f0-9]{40}"), "Missing exact candidate commit for study runtime evidence");
      require(candidateCommit.equals(System.getProperty("iedctr.executedSourceSha", "")),
         "Candidate commit does not match the client source identity");
      require(manifestHash.matches("[a-f0-9]{64}"), "Missing working-tree manifest checksum for study runtime evidence");
      require(!manifestPath.isBlank(), "Missing working-tree manifest path for study runtime evidence");
      Path manifest = Path.of(manifestPath).toRealPath();
      require(Files.isRegularFile(manifest) && hash(Files.readAllBytes(manifest)).equals(manifestHash),
         "Working-tree manifest bytes do not match the declared checksum");
      JsonObject source = new JsonObject();
      source.addProperty("status", "EXACT_COMMIT_CANDIDATE");
      source.addProperty("baseline_commit", baseline);
      source.addProperty("candidate_commit", candidateCommit);
      source.addProperty("working_manifest_path", manifest.toString());
      source.addProperty("working_manifest_sha256", manifestHash);
      source.addProperty("working_manifest_bytes_verified", true);
      source.addProperty("baseline_is_integrated_change_commit", false);
      source.addProperty("scope", "Exact candidate commit, baseline provenance and source manifest; no merge, release or installation implied.");
      proof.add("study_source_provenance", source);
      return proof;
   }

   private static String hash(byte[] bytes) throws Exception { return FactoryControlRuntimeProof.hash(bytes); }
   private static void require(boolean condition, String message) { FactoryControlRuntimeProof.require(condition, message); }
}
