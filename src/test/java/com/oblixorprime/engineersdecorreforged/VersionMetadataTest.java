package com.oblixorprime.engineersdecorreforged;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import org.junit.Test;

/** Verify the ordinary distributable produced by Gradle, without starting Minecraft. */
public class VersionMetadataTest {
   private static String requiredProperty(String name) {
      String value = System.getProperty(name);
      assertNotNull("Gradle must supply " + name, value);
      return value;
   }

   private static Path runtimeJar() {
      return Path.of(requiredProperty("iedctr.runtimeJar"));
   }

   private static String expectedVersion() {
      return requiredProperty("iedctr.expectedModVersion");
   }

   @Test
   public void ordinaryJarFilenameMatchesDeclaredCoordinates() {
      assertEquals(requiredProperty("iedctr.expectedModId") + "-" + expectedVersion() + ".jar",
            runtimeJar().getFileName().toString());
   }

   @Test
   public void packagedNeoForgeMetadataMatchesDeclaredCoordinates() throws Exception {
      try (JarFile jar = new JarFile(runtimeJar().toFile())) {
         var entry = jar.getJarEntry("META-INF/neoforge.mods.toml");
         assertNotNull("Ordinary JAR must contain NeoForge metadata", entry);
         String metadata;
         try (var input = jar.getInputStream(entry)) {
            metadata = new String(input.readAllBytes(), StandardCharsets.UTF_8);
         }
         var versions = Pattern.compile("(?m)^version=\"([^\"]+)\"$").matcher(metadata);
         assertTrue("Packaged mod version is required", versions.find());
         assertEquals(expectedVersion(), versions.group(1));
         assertEquals("Exactly one mod version must be packaged", 1,
               Pattern.compile("(?m)^version=").matcher(metadata).results().count());
         assertTrue(metadata.contains("modId=\"" + requiredProperty("iedctr.expectedModId") + "\""));
      }
   }

   @Test
   public void bothManifestVersionsMatchDeclaredVersion() throws Exception {
      try (JarFile jar = new JarFile(runtimeJar().toFile())) {
         assertNotNull("Ordinary JAR must contain a manifest", jar.getManifest());
         var attributes = jar.getManifest().getMainAttributes();
         assertEquals(expectedVersion(), attributes.getValue("Specification-Version"));
         assertEquals(expectedVersion(), attributes.getValue("Implementation-Version"));
      }
   }
}
