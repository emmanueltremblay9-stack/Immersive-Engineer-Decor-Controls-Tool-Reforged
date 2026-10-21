package com.oblixorprime.engineersdecorreforged.block;

import static org.junit.Assert.*;

import java.io.InputStream;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

/** Checks the compiled recipient contract without bootstrapping client Minecraft classes. */
public class SlidingDoorSoundBroadcastTest {
   private static final String OWNER =
      "com/oblixorprime/engineersdecorreforged/block/PortedBlocks$SlidingDoorBlock";
   private static final String LEVEL = "net/minecraft/world/level/Level";
   private static final String HELPER =
      "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Z)V";
   private static final String SOUND =
      "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;"
      + "Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V";

   @Test
   public void serverSoundDoesNotExcludeTheInitiatingPlayer() throws Exception {
      ClassNode owner = new ClassNode(Opcodes.ASM9);
      try (InputStream stream = getClass().getClassLoader().getResourceAsStream(OWNER + ".class")) {
         assertNotNull("compiled sliding-door resource is required", stream);
         new ClassReader(stream).accept(owner, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      }

      MethodNode helper = owner.methods.stream()
         .filter(method -> method.name.equals("playSlidingDoorSound") && method.desc.equals(HELPER))
         .findFirst().orElse(null);
      assertNotNull("sound helper must not accept an initiating entity", helper);

      Frame<SourceValue>[] frames = new Analyzer<>(new SourceInterpreter()).analyze(OWNER, helper);
      int soundCalls = 0;
      for (int i = 0; i < helper.instructions.size(); i++) {
         AbstractInsnNode instruction = helper.instructions.get(i);
         if (!(instruction instanceof MethodInsnNode call) || !call.name.equals("playSound")) continue;
         soundCalls++;
         assertEquals(LEVEL, call.owner);
         assertEquals("retain the Entity/block-position sound overload", SOUND, call.desc);
         Frame<SourceValue> frame = frames[i];
         assertNotNull("sound invocation must be reachable", frame);
         int argumentCount = Type.getArgumentTypes(call.desc).length;
         SourceValue excludedEntity = frame.getStack(frame.getStackSize() - argumentCount);
         assertEquals("the exclusion argument must have one source", 1, excludedEntity.insns.size());
         assertEquals("broadcast to everyone, including the initiating player",
            Opcodes.ACONST_NULL, excludedEntity.insns.iterator().next().getOpcode());
      }
      assertEquals("one sound dispatch per helper invocation", 1, soundCalls);
   }
}
