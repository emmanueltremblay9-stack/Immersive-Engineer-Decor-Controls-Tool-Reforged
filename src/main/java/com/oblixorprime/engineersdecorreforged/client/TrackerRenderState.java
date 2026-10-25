/*
 * Tracker display-angle formula adapted from Engineer's Tools 1.1.6.
 * Copyright (c) 2020 Stefan Wilhelm (wile), MIT.
 * See docs/testing/tracker-render-parity.md for source and compatibility boundaries.
 */
package com.oblixorprime.engineersdecorreforged.client;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Read-only view of the current target format; never writes or migrates item data. */
public final class TrackerRenderState {
   private TrackerRenderState() {}

   public record Target(ResourceLocation dimension, BlockPos position) {}
   public record Angles(int pitch, int yaw) {}

   public static Optional<Target> target(ItemStack stack) {
      var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
      if (!tag.contains("target_dimension", Tag.TAG_STRING)
            || !tag.contains("target_x", Tag.TAG_INT) || !tag.contains("target_y", Tag.TAG_INT)
            || !tag.contains("target_z", Tag.TAG_INT)) return Optional.empty();
      var dimension = ResourceLocation.tryParse(tag.getString("target_dimension"));
      return dimension == null ? Optional.empty() : Optional.of(new Target(dimension,
         new BlockPos(tag.getInt("target_x"), tag.getInt("target_y"), tag.getInt("target_z"))));
   }

   /** Empty near the target means retain the previous display angles, as upstream did. */
   public static Optional<Angles> angles(double x, double y, double z, double lookX, double lookY, double lookZ) {
      double distanceSquared = x*x + y*y + z*z;
      if (!Double.isFinite(distanceSquared) || distanceSquared < 0.3
            || !Double.isFinite(lookX) || !Double.isFinite(lookY) || !Double.isFinite(lookZ)) {
         return Optional.empty();
      }
      double distance = Math.sqrt(distanceSquared);
      double horizontal = Math.sqrt(x*x + z*z);
      double yaw = Math.toDegrees(Math.atan2(lookZ, lookX) - Math.atan2(z/distance, x/distance));
      double pitch = Math.abs(y) + 5 > horizontal
         ? Math.toDegrees(Math.acos(Math.clamp(lookY, -1.0, 1.0)) - Math.acos(Math.clamp(y/distance, -1.0, 1.0))) : 0;
      if (pitch > 180) pitch -= 360;
      if (yaw > 180) yaw -= 360;
      if (Math.abs(pitch) < 30) pitch = 0;
      return Optional.of(new Angles((int)pitch, (int)yaw));
   }
}
