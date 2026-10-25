package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.client.TrackerItemRenderer;
import com.oblixorprime.engineersdecorreforged.client.TrackerRenderState;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix4f;

/** Invoked only by the existing nonce-guarded isolated client fixture; no player/world mutation. */
final class TrackerRenderValidation {
   private TrackerRenderValidation() {}
   private static final int BODY_VERTICES = 45 * 4;
   private static final int TOTAL_VERTICES = (45 + 66) * 4;

   static void verify(Minecraft mc, Path directory, String nonce) throws Exception {
      require(Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce), "Tracker fixture isolation");
      var stack = new ItemStack(EngineerToolsModule.TRACKER.get());
      require(IClientItemExtensions.of(stack).getCustomRenderer() instanceof TrackerItemRenderer, "Tracker extension registered");
      require(mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0).isCustomRenderer(), "Tracker uses builtin/entity");
      require(quads(mc, mc.getModelManager().getModel(TrackerItemRenderer.BODY)) == 45, "Body has all original faces");
      require(quads(mc, mc.getModelManager().getModel(TrackerItemRenderer.POINTER)) == 66, "Pointer has all original faces");

      var cases = new JsonArray();
      Mesh neutral = null;
      for (var context : ItemDisplayContext.values()) {
         var mesh = render(mc, stack, context, context.name(), cases);
         if (context == ItemDisplayContext.GUI) neutral = mesh;
      }
      require(neutral != null, "GUI context present");

      var malformed = stack.copy();
      var tag = targetTag(mc, 32);
      tag.putString("target_x", "invalid coordinate");
      malformed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      require(TrackerRenderState.target(malformed).isEmpty(), "Malformed coordinate is ignored");
      require(render(mc, malformed, ItemDisplayContext.GUI, "malformed_target", cases).points.equals(neutral.points),
         "Malformed target keeps neutral display");

      var foreign = stack.copy();
      tag = targetTag(mc, 32);
      tag.putString("target_dimension", "iedctr_validation:other_dimension");
      foreign.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      require(render(mc, foreign, ItemDisplayContext.GUI, "foreign_dimension", cases).points.equals(neutral.points),
         "Other dimension keeps neutral display");

      boolean pointerMoved = false;
      for (int offset : new int[]{32, -32}) {
         var aimed = stack.copy();
         aimed.set(DataComponents.CUSTOM_DATA, CustomData.of(targetTag(mc, offset)));
         require(TrackerRenderState.target(aimed).isPresent(), "Current target format is read");
         var mesh = render(mc, aimed, ItemDisplayContext.GUI, "target_" + offset, cases);
         require(mesh.points.subList(0, BODY_VERTICES).equals(neutral.points.subList(0, BODY_VERTICES)),
            "Target changes only pointer pose");
         pointerMoved |= !mesh.points.subList(BODY_VERTICES, TOTAL_VERTICES)
            .equals(neutral.points.subList(BODY_VERTICES, TOTAL_VERTICES));
      }
      require(pointerMoved, "Stored target rotates pointer");

      var proof = new JsonObject();
      proof.addProperty("suite", "tracker-render-v1");
      proof.addProperty("status", "passed");
      proof.addProperty("nonce", nonce);
      proof.addProperty("body_quads", 45);
      proof.addProperty("pointer_quads", 66);
      proof.addProperty("pointer_moved", pointerMoved);
      proof.addProperty("first_person_original_skip", true);
      proof.add("cases", cases);
      Files.writeString(directory.resolve("client-tracker-render-result.json"),
         new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
      EngineersDecorReforged.LOGGER.info("IEDCTR_TRACKER_RENDER_PASS nonce={} cases={} firstPersonSkipped=2", nonce, cases.size());
   }

   private static CompoundTag targetTag(Minecraft mc, int offset) {
      var tag = new CompoundTag();
      var pos = mc.player.blockPosition().offset(offset, 16, 0);
      tag.putString("target_dimension", mc.level.dimension().location().toString());
      tag.putInt("target_x", pos.getX());
      tag.putInt("target_y", pos.getY());
      tag.putInt("target_z", pos.getZ());
      return tag;
   }

   private static int quads(Minecraft mc, BakedModel model) {
      require(model != mc.getModelManager().getMissingModel(), "Auxiliary model loaded");
      var faces = new ArrayList<Direction>(List.of(Direction.values()));
      faces.add(null);
      int count = 0;
      for (var face : faces) {
         for (var quad : model.getQuads(null, face, RandomSource.create(42))) {
            require(!quad.getSprite().contents().name().getPath().equals("missingno"), "Auxiliary texture loaded");
            count++;
         }
      }
      return count;
   }

   private static Mesh render(Minecraft mc, ItemStack stack, ItemDisplayContext context, String name, JsonArray cases) {
      var before = stack.copy();
      var poses = new PoseStack();
      poses.translate(0.125, 0.25, 0.375);
      var poseBefore = new Matrix4f(poses.last().pose());
      var mesh = new Mesh();
      mc.getItemRenderer().render(stack, context, false, poses, renderType -> {
         mesh.buffers++;
         return mesh;
      }, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
         mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0));
      boolean skipped = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
         || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
      require(mesh.points.size() == (skipped ? 0 : TOTAL_VERTICES), "Unexpected vertices in " + name);
      require(skipped ? mesh.buffers == 0 : mesh.buffers > 0, "Unexpected buffer use in " + name);
      require(ItemStack.matches(before, stack), "Rendering must not change item data");
      require(poses.last().pose().equals(poseBefore), "Rendering restores pose stack");
      var result = new JsonObject();
      result.addProperty("name", name);
      result.addProperty("context", context.name());
      result.addProperty("vertices", mesh.points.size());
      result.addProperty("buffers", mesh.buffers);
      result.addProperty("unchanged_stack", true);
      result.addProperty("pose_restored", true);
      cases.add(result);
      return mesh;
   }

   private static void require(boolean condition, String message) {
      if (!condition) throw new IllegalStateException(message);
   }

   private static final class Mesh implements VertexConsumer {
      final List<Vec3> points = new ArrayList<>();
      int buffers;
      public VertexConsumer addVertex(float x, float y, float z) { points.add(new Vec3(x, y, z)); return this; }
      public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
      public VertexConsumer setUv(float u, float v) { return this; }
      public VertexConsumer setUv1(int u, int v) { return this; }
      public VertexConsumer setUv2(int u, int v) { return this; }
      public VertexConsumer setNormal(float x, float y, float z) { return this; }
   }
}
