package com.oblixorprime.engineersdecorreforged.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.tools.TrackerItem;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class TrackerItemRenderer extends BlockEntityWithoutLevelRenderer {
   private static final Map<ItemStack, AngleState> ANGLE_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
   public static final ModelResourceLocation BODY_MODEL = ModelResourceLocation.standalone(
      ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "item/tracker_model")
   );
   public static final ModelResourceLocation POINTER_MODEL = ModelResourceLocation.standalone(
      ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "item/tracker_pointer_model")
   );

   public TrackerItemRenderer() {
      super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
   }

   @Override
   public void renderByItem(
      ItemStack stack,
      ItemDisplayContext context,
      PoseStack pose,
      MultiBufferSource buffers,
      int combinedLight,
      int combinedOverlay
   ) {
      if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      ItemRenderer renderer = minecraft.getItemRenderer();
      BakedModel body = minecraft.getModelManager().getModel(BODY_MODEL);
      BakedModel pointer = minecraft.getModelManager().getModel(POINTER_MODEL);
      pose.pushPose();
      renderModel(renderer, body, stack, pose, buffers, combinedLight, combinedOverlay);
      pose.translate(0.5, 0.5, 0.5);
      AngleState angles = pointerAngles(stack, minecraft.player);
      if (angles == null) {
         pose.mulPose(Axis.YP.rotationDegrees(180.0F));
      } else {
         pose.mulPose(Axis.YP.rotationDegrees(angles.yaw()));
         pose.mulPose(Axis.XP.rotationDegrees(angles.pitch()));
      }
      pose.scale(0.6F, 0.6F, 0.6F);
      pose.translate(-0.5, -0.5, -0.5);
      renderModel(renderer, pointer, stack, pose, buffers, combinedLight, combinedOverlay);
      pose.popPose();
   }

   private static void renderModel(
      ItemRenderer renderer,
      BakedModel model,
      ItemStack stack,
      PoseStack pose,
      MultiBufferSource buffers,
      int combinedLight,
      int combinedOverlay
   ) {
      for (BakedModel pass : model.getRenderPasses(stack, true)) {
         for (RenderType renderType : pass.getRenderTypes(stack, true)) {
            VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffers, renderType, true, stack.hasFoil());
            renderer.renderModelLists(pass, stack, combinedLight, combinedOverlay, pose, consumer);
         }
      }
   }

   private static AngleState pointerAngles(ItemStack stack, LocalPlayer player) {
      if (player == null) {
         return null;
      }
      Optional<ResourceLocation> dimension = TrackerItem.targetDimension(stack);
      Optional<BlockPos> target = TrackerItem.targetPosition(stack);
      if (dimension.isEmpty() || target.isEmpty() || !dimension.get().equals(player.level().dimension().location())) {
         ANGLE_CACHE.remove(stack);
         return null;
      }

      AngleState cached = ANGLE_CACHE.get(stack);
      if (cached != null && cached.tick() == player.tickCount) {
         return cached;
      }
      BlockPos targetPos = target.get();
      Vec3 direction = new Vec3(targetPos.getX(), targetPos.getY(), targetPos.getZ()).subtract(player.position());
      if (direction.lengthSqr() < 0.3) {
         return cached;
      }
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      double vertical = Math.abs(direction.y);
      direction = direction.normalize();
      Vec3 look = player.getLookAngle();
      double yaw = Math.toDegrees(Math.atan2(look.z, look.x) - Math.atan2(direction.z, direction.x));
      double pitch = vertical + 5.0 > horizontal ? Math.toDegrees(Math.acos(look.y) - Math.acos(direction.y)) : 0.0;
      yaw = wrapDegrees(yaw);
      pitch = wrapDegrees(pitch);
      if (Math.abs(pitch) < 30.0) {
         pitch = 0.0;
      }
      AngleState angles = new AngleState(player.tickCount, (int)pitch, (int)yaw);
      ANGLE_CACHE.put(stack, angles);
      return angles;
   }

   private static double wrapDegrees(double angle) {
      while (angle > 180.0) {
         angle -= 360.0;
      }
      while (angle < -180.0) {
         angle += 360.0;
      }
      return angle;
   }

   private record AngleState(int tick, float pitch, float yaw) {
   }
}
