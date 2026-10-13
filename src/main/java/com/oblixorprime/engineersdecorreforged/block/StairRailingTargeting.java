package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Adds only our stair railing's one-cell upper overhang to ordinary block picking. */
public final class StairRailingTargeting {
   private static final double EPSILON = 1.0E-9;

   private StairRailingTargeting() {}

   /** Vanilla block occlusion plus the nearest visible upper stair rail; never loads chunks. */
   public static BlockHitResult clipBlocks(Level level, Vec3 start, Vec3 end, Entity context) {
      BlockHitResult vanilla = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, context));
      return extendBlocks(level, start, end, vanilla);
   }

   /** Called after vanilla has picked entities and applied its separate interaction ranges. */
   public static HitResult extendPick(Entity camera, double blockRange, float partialTick, HitResult vanilla) {
      if (!Double.isFinite(blockRange) || blockRange <= 0) {
         return vanilla;
      }
      Vec3 start = camera.getEyePosition(partialTick);
      Vec3 end = start.add(camera.getViewVector(partialTick).scale(blockRange));
      BlockHitResult block = clipBlocks(camera.level(), start, end, camera);
      if (!isUpperRailHit(camera.level(), block)
         || start.distanceToSqr(block.getLocation()) > blockRange * blockRange
         || vanilla.getType() != HitResult.Type.MISS
            && start.distanceToSqr(vanilla.getLocation()) <= start.distanceToSqr(block.getLocation()) + EPSILON
         || hasCloserEntity(camera, start, end, block)) {
         return vanilla;
      }
      return block;
   }

   /** Uses vanilla's pickable-entity predicate, hit inflation and shared-vehicle handling. */
   public static boolean hasCloserEntity(Entity camera, Vec3 start, Vec3 end, BlockHitResult block) {
      double distance = start.distanceToSqr(block.getLocation());
      EntityHitResult entity = ProjectileUtil.getEntityHitResult(camera, start, end,
         camera.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0),
         candidate -> !candidate.isSpectator() && candidate.isPickable(), distance);
      // Vanilla also occludes a block with a nearer pickable entity outside entity attack range.
      return entity != null && start.distanceToSqr(entity.getLocation()) < distance;
   }

   private static BlockHitResult extendBlocks(Level level, Vec3 start, Vec3 end, BlockHitResult vanilla) {
      Nearest nearest = new Nearest(vanilla, vanilla.getType() == HitResult.Type.BLOCK
         ? start.distanceToSqr(vanilla.getLocation()) : Double.POSITIVE_INFINITY);
      // Traverse only the real ray. A rail is at most one cell above its owning stair.
      // Returning null visits every ray cell so competing stair overhangs use exact hit distance.
      BlockGetter.traverseBlocks(start, end, nearest, (result, cell) -> {
         BlockPos owner = cell.below();
         if (!level.hasChunkAt(owner) || !level.getWorldBorder().isWithinBounds(owner)) {
            return null;
         }
         BlockState state = level.getBlockState(owner);
         if (!(state.getBlock() instanceof PortedBlocks.CatwalkStairsBlock)) {
            return null;
         }
         PortedBlocks.RailingPart part = PortedBlocks.selectStairRailingPart(state, owner, start, end);
         if (part == null || part.kind() != PortedBlocks.PartKind.RAILING) {
            return null;
         }
         BlockHitResult hit = part.shape().clip(start, end, owner);
         if (hit != null && hit.getLocation().y >= owner.getY() + 1.0 - EPSILON) {
            double distance = start.distanceToSqr(hit.getLocation());
            // Ordinary blocks win exact ties; the extension cannot pick through a wall.
            if (distance < result.distance - EPSILON) {
               result.hit = hit;
               result.distance = distance;
            }
         }
         return null;
      }, result -> null);
      return nearest.hit;
   }

   private static boolean isUpperRailHit(Level level, BlockHitResult hit) {
      return hit.getType() == HitResult.Type.BLOCK
         && hit.getLocation().y >= hit.getBlockPos().getY() + 1.0 - EPSILON
         && level.getBlockState(hit.getBlockPos()).getBlock() instanceof PortedBlocks.CatwalkStairsBlock;
   }

   private static final class Nearest {
      private BlockHitResult hit;
      private double distance;

      private Nearest(BlockHitResult hit, double distance) {
         this.hit = hit;
         this.distance = distance;
      }
   }
}
