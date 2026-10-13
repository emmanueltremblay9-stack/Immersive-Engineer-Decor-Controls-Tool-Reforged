package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Checks exact mapped protocol/pose contracts without loading client-only Minecraft classes. */
public class MiningPosePacketOrderTest {
   private static final String EVENT_HANDLER = "com/oblixorprime/engineersdecorreforged/client/RailingMiningClientEvents";
   private static final String MOVE = "net/minecraft/network/protocol/game/ServerboundMovePlayerPacket$Rot";

   @Test
   public void neoForgeHeldAttackChecksItsActiveBindingContext() throws IOException {
      List<String> calls = calls("net/minecraft/client/KeyMapping", "isDown", "()Z");
      assertTrue("setDown(true) is insufficient while a GUI disables the native IN_GAME binding",
         containsMethod(calls, "isConflictContextAndModifierActive"));
   }

   @Test
   public void nativeLookAngleReadsPacketYawAndPitchWithoutRenderHeadInterpolation() throws IOException {
      List<String> calls = calls("net/minecraft/world/entity/Entity", "getLookAngle", "()Lnet/minecraft/world/phys/Vec3;");
      assertTrue("native current pitch must feed the ray", containsMethod(calls, "getXRot"));
      assertTrue("native accepted packet yaw must feed the ray", containsMethod(calls, "getYRot"));
      assertTrue("native direction construction must remain in use", containsMethod(calls, "calculateViewVector"));
      assertFalse("render head pose cannot determine a packet action", containsMethod(calls, "getViewYRot"));
      assertFalse("render head pose cannot determine a packet action", containsMethod(calls, "getYHeadRot"));
   }

   @Test
   public void nativeBlockActionHasNoAimSoItCannotReplaceThePrecedingRotationUpdate() throws IOException {
      List<String> floatingFields = new ArrayList<>();
      try (InputStream stream = resource("net/minecraft/network/protocol/game/ServerboundPlayerActionPacket.class")) {
         new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
               if ((access & Opcodes.ACC_STATIC) == 0 && (descriptor.equals("F") || descriptor.equals("D"))) floatingFields.add(name);
               return null;
            }
         }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      }
      assertTrue("a protocol change adding native aim needs this workaround reviewed", floatingFields.isEmpty());
   }

   @Test
   public void clientStartAndHoldFlushCameraRotationBeforeAnyNativeMiningContinuation() throws IOException {
      List<String> calls = calls(EVENT_HANDLER, "continueMining",
         "(Lnet/neoforged/neoforge/event/entity/player/PlayerInteractEvent$LeftClickBlock;)V");
      int constructed = calls.indexOf(MOVE + ".<init>(FFZ)V");
      int sent = indexOfMethodAfter(calls, "send", constructed);
      assertTrue("turn-and-click needs a vanilla rotation packet before START/STOP", constructed >= 0);
      assertTrue("the pose packet must actually be sent", sent > constructed);
      int yaw = indexOfMethodAfter(calls, "getViewYRot", -1);
      int pitch = indexOfMethodAfter(calls, "getViewXRot", -1);
      assertTrue("camera yaw must also match native mounted-player targeting", yaw >= 0 && yaw < constructed);
      assertTrue("camera pitch must be captured before constructing the packet", pitch >= 0 && pitch < constructed);
      String action = "field:net/neoforged/neoforge/event/entity/player/PlayerInteractEvent$LeftClickBlock$Action.";
      assertTrue("creative and initial survival START must enter the pose hook", calls.contains(action + "START"));
      assertTrue("survival HOLD/completion must enter the pose hook", calls.contains(action + "CLIENT_HOLD"));
      int match = indexOfMethodAfter(calls, "matchesCurrentTarget", -1);
      int stop = indexOfMethodAfter(calls, "stopDestroyBlock", -1);
      int start = indexOfMethodAfter(calls, "startDestroyBlock", -1);
      assertTrue("even an unchanged HOLD must flush before its eventual native STOP", match > sent);
      assertTrue("restart cannot send ABORT before synchronizing its new aim", stop > sent);
      assertTrue("restart cannot send START before synchronizing its new aim", start > sent);
   }

   private static boolean containsMethod(List<String> calls, String method) {
      return indexOfMethodAfter(calls, method, -1) >= 0;
   }

   private static int indexOfMethodAfter(List<String> calls, String method, int index) {
      for (int i = Math.max(0, index + 1); i < calls.size(); i++) {
         if (calls.get(i).contains("." + method + "(")) return i;
      }
      return -1;
   }

   private static List<String> calls(String owner, String method, String descriptor) throws IOException {
      List<String> calls = new ArrayList<>();
      boolean[] found = {false};
      try (InputStream stream = resource(owner + ".class")) {
         new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
               if (!name.equals(method) || !desc.equals(descriptor)) return null;
               found[0] = true;
               return new MethodVisitor(Opcodes.ASM9) {
                  @Override
                  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                     calls.add("field:" + owner + "." + name);
                  }

                  @Override
                  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
                     calls.add(owner + "." + name + desc);
                  }
               };
            }
         }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      }
      assertTrue("required exact mapped method missing: " + owner + "." + method + descriptor, found[0]);
      return calls;
   }

   private static InputStream resource(String path) {
      InputStream stream = MiningPosePacketOrderTest.class.getClassLoader().getResourceAsStream(path);
      assertNotNull("required mapped resource missing: " + path, stream);
      return stream;
   }
}
