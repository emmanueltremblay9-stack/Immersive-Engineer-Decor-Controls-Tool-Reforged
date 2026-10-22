/*
 * Adapted from wile.engineerstools.detail.ModRenderers.TrackerIster, Engineer's Tools 1.1.6.
 * Copyright (c) 2020 Stefan Wilhelm (wile), MIT.
 * NeoForge API and read-only current target adaptation: see docs/testing/tracker-render-parity.md.
 */
package com.oblixorprime.engineersdecorreforged.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class TrackerItemRenderer extends BlockEntityWithoutLevelRenderer {
   public static final ModelResourceLocation BODY = ModelResourceLocation.standalone(
      ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "item/tracker_model"));
   public static final ModelResourceLocation POINTER = ModelResourceLocation.standalone(
      ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "item/tracker_pointer_model"));

   // Transient display cache replaces upstream's persistent random tracker id; it does not keep stacks alive.
   private final Map<ItemStack, LastAngles> previous = new WeakHashMap<>();
   private record LastAngles(TrackerRenderState.Target target, TrackerRenderState.Angles angles) {}

   public TrackerItemRenderer() {
      super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
   }

   private Optional<TrackerRenderState.Angles> rotations(ItemStack stack, Minecraft mc) {
      var target = TrackerRenderState.target(stack);
      if (mc.player == null || mc.level == null || target.isEmpty()
            || !target.get().dimension().equals(mc.level.dimension().location())) {
         previous.remove(stack);
         return Optional.empty();
      }
      var pos = target.get().position();
      var look = mc.player.getLookAngle();
      var current = TrackerRenderState.angles(pos.getX() - mc.player.getX(),
         pos.getY() - mc.player.getY(), pos.getZ() - mc.player.getZ(), look.x, look.y, look.z);
      if (current.isPresent()) {
         previous.put(stack, new LastAngles(target.get(), current.get()));
         return current;
      }
      var last = previous.get(stack);
      return last != null && last.target().equals(target.get()) ? Optional.of(last.angles()) : Optional.empty();
   }

   @Override
   public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses,
                            MultiBufferSource buffers, int light, int overlay) {
      // The original renderer deliberately draws neither first-person hand.
      if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) return;
      var mc = Minecraft.getInstance();
      var rotations = rotations(stack, mc);
      var itemRenderer = mc.getItemRenderer();
      var vertices = ItemRenderer.getFoilBuffer(buffers, RenderType.cutout(), true, false);
      poses.pushPose();
      try {
         itemRenderer.renderModelLists(mc.getModelManager().getModel(BODY), stack, light, overlay, poses, vertices);
         poses.translate(0.5, 0.5, 0.5);
         if (rotations.isEmpty()) {
            poses.mulPose(Axis.YP.rotationDegrees(180));
         } else {
            poses.mulPose(Axis.YP.rotationDegrees(rotations.get().yaw()));
            poses.mulPose(Axis.XP.rotationDegrees(rotations.get().pitch()));
         }
         poses.scale(0.6f, 0.6f, 0.6f);
         poses.translate(-0.5, -0.5, -0.5);
         itemRenderer.renderModelLists(mc.getModelManager().getModel(POINTER), stack, light, overlay, poses, vertices);
      } finally {
         poses.popPose();
      }
   }
}
