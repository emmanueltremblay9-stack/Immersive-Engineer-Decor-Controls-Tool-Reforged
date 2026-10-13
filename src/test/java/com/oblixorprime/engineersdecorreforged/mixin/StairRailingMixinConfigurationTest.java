package com.oblixorprime.engineersdecorreforged.mixin;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** Inspects mapped bytecode/resources without loading any Minecraft client class on a server. */
public class StairRailingMixinConfigurationTest {
   private static final String CONFIG = "immersive_engineer_decor_controls_tool_reforged.mixins.json";
   private static final String TARGET = "net/minecraft/client/renderer/GameRenderer";
   private static final String DESCRIPTOR = "(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;";
   private static final String MIXIN = "com/oblixorprime/engineersdecorreforged/mixin/client/StairRailingPickMixin";

   @Test
   public void exactMappedClientPickDescriptorExistsAndReturnsHitResults() throws IOException {
      int[] methodsAndReturns = new int[2];
      try (InputStream stream = resource(TARGET + ".class")) {
         new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
               if (!name.equals("pick") || !descriptor.equals(DESCRIPTOR)) return null;
               methodsAndReturns[0]++;
               assertTrue("pick target must stay a private instance method", (access & Opcodes.ACC_PRIVATE) != 0);
               assertEquals("pick target must not become static", 0, access & Opcodes.ACC_STATIC);
               return new MethodVisitor(Opcodes.ASM9) {
                  @Override
                  public void visitInsn(int opcode) {
                     if (opcode == Opcodes.ARETURN) methodsAndReturns[1]++;
                  }
               };
            }
         }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      }
      assertEquals("the configured full method descriptor must identify exactly one mapped method", 1, methodsAndReturns[0]);
      assertTrue("RETURN injection needs an actual HitResult return", methodsAndReturns[1] > 0);
   }

   @Test
   public void mixinTargetsOnlyTheVerifiedMethodAtReturnAndFailsIfMissing() throws IOException {
      List<String> targets = new ArrayList<>();
      List<String> methods = new ArrayList<>();
      List<String> at = new ArrayList<>();
      int[] require = {-1};
      boolean[] cancellable = {false};
      try (InputStream stream = resource(MIXIN + ".class")) {
         new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
               if (!descriptor.equals("Lorg/spongepowered/asm/mixin/Mixin;")) return null;
               return new AnnotationVisitor(Opcodes.ASM9) {
                  @Override
                  public AnnotationVisitor visitArray(String name) {
                     if (!name.equals("value")) return null;
                     return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(String key, Object value) { targets.add(((Type)value).getInternalName()); }
                     };
                  }
               };
            }
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
               return new MethodVisitor(Opcodes.ASM9) {
                  @Override
                  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                     if (!descriptor.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) return null;
                     return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(String key, Object value) {
                           if (key.equals("require")) require[0] = (Integer)value;
                           if (key.equals("cancellable")) cancellable[0] = (Boolean)value;
                        }
                        @Override
                        public AnnotationVisitor visitArray(String key) {
                           if (key.equals("method")) return new AnnotationVisitor(Opcodes.ASM9) {
                              @Override
                              public void visit(String name, Object value) { methods.add((String)value); }
                           };
                           if (key.equals("at")) return new AnnotationVisitor(Opcodes.ASM9) {
                              @Override
                              public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                                 return new AnnotationVisitor(Opcodes.ASM9) {
                                    @Override
                                    public void visit(String key, Object value) { if (key.equals("value")) at.add((String)value); }
                                 };
                              }
                           };
                           return null;
                        }
                     };
                  }
               };
            }
         }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      }
      assertEquals(List.of(TARGET), targets);
      assertEquals(List.of("pick" + DESCRIPTOR), methods);
      assertEquals(List.of("RETURN"), at);
      assertEquals("silently losing the upper-rail correction is not allowed", 1, require[0]);
      assertTrue(cancellable[0]);
   }

   @Test
   public void mixinConfigurationIsRegisteredAndStrictlyClientOnly() throws IOException {
      JsonObject config;
      try (InputStream stream = resource(CONFIG)) {
         config = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
      }
      assertTrue(config.get("required").getAsBoolean());
      assertEquals("JAVA_21", config.get("compatibilityLevel").getAsString());
      assertEquals("com.oblixorprime.engineersdecorreforged.mixin", config.get("package").getAsString());
      assertEquals(1, config.getAsJsonArray("client").size());
      assertEquals("client.StairRailingPickMixin", config.getAsJsonArray("client").get(0).getAsString());
      assertFalse("no client classes may be listed on both distributions", config.has("mixins"));
      assertFalse("no client classes may be listed for the server", config.has("server"));
      assertEquals(1, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
      try (InputStream stream = resource("META-INF/neoforge.mods.toml")) {
         String manifest = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
         assertTrue("NeoForge must discover the exact packaged config", manifest.contains("[[mixins]]\nconfig=\"" + CONFIG + "\""));
      }
   }

   private static InputStream resource(String path) {
      InputStream stream = StairRailingMixinConfigurationTest.class.getClassLoader().getResourceAsStream(path);
      assertNotNull("required bytecode/resource missing from test runtime: " + path, stream);
      return stream;
   }
}
