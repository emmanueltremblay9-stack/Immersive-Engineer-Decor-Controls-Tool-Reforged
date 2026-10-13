package com.oblixorprime.engineersdecorreforged.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/** Draws the selected railing component without changing its physical or rendered geometry. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class RailingHighlightEvents {
   private RailingHighlightEvents() {
   }

   @SubscribeEvent
   public static void renderBlockHighlight(RenderHighlightEvent.Block event) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity cameraEntity = event.getCamera().getEntity();
      if (minecraft.level == null || minecraft.player == null || cameraEntity == null) {
         return;
      }

      BlockPos pos = event.getTarget().getBlockPos();
      if (!minecraft.level.getWorldBorder().isWithinBounds(pos)) {
         return;
      }

      // Vanilla picking uses the camera entity's eye, not the displaced third-person camera.
      float partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
      Vec3 start = cameraEntity.getEyePosition(partialTick);
      Vec3 end = start.add(cameraEntity.getViewVector(partialTick).scale(minecraft.player.blockInteractionRange()));
      PortedBlocks.RailingPart part = PortedBlocks.selectRailingPart(minecraft.level, pos, start, end, cameraEntity);
      if (part == null || part.shape().isEmpty()) {
         return;
      }

      Vec3 cameraPosition = event.getCamera().getPosition();
      renderOutline(
         event.getPoseStack(),
         event.getMultiBufferSource().getBuffer(RenderType.lines()),
         part.shape(),
         pos.getX() - cameraPosition.x,
         pos.getY() - cameraPosition.y,
         pos.getZ() - cameraPosition.z
      );
      event.setCanceled(true);
   }

   private static void renderOutline(PoseStack poseStack, VertexConsumer vertices, VoxelShape shape, double x, double y, double z) {
      PoseStack.Pose pose = poseStack.last();
      shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
         // Voxel-shape edges are axis-aligned, so their signed axis is already a unit normal.
         float normalX = (float)Math.signum(maxX - minX);
         float normalY = (float)Math.signum(maxY - minY);
         float normalZ = (float)Math.signum(maxZ - minZ);
         vertices.addVertex(pose, (float)(minX + x), (float)(minY + y), (float)(minZ + z))
            .setColor(0.0F, 0.0F, 0.0F, 0.4F)
            .setNormal(pose, normalX, normalY, normalZ);
         vertices.addVertex(pose, (float)(maxX + x), (float)(maxY + y), (float)(maxZ + z))
            .setColor(0.0F, 0.0F, 0.0F, 0.4F)
            .setNormal(pose, normalX, normalY, normalZ);
      });
   }
}
