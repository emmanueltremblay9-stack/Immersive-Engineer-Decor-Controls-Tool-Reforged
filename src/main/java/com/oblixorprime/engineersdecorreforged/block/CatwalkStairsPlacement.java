package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Keeps the existing model's high end attached to the surface the player selected. */
public final class CatwalkStairsPlacement {
   private static final double EDGE_BAND = 0.25;

   private CatwalkStairsPlacement() {
   }

   /** Facing is the uphill direction in both the saved block state and the original models. */
   public static Direction facing(BlockPlaceContext context) {
      if (context instanceof AttachedContext attached) {
         return attached.attachment.facing();
      }
      Direction edge = selectedEdge(context.getClickedFace(), context.getClickLocation(), originalClickedPos(context));
      return edge == null ? context.getHorizontalDirection() : edge.getOpposite();
   }

   /** Resolve before vanilla tests its ordinary adjacent position, which may be obstructed. */
   @Nullable
   public static Attachment attachment(UseOnContext context) {
      BlockPos source = context.getClickedPos();
      BlockState state = context.getLevel().getBlockState(source);
      if (state.is(ModBlocks.STEEL_CATWALK_STAIRS.get())) {
         Direction uphill = state.getValue(PortedBlocks.HORIZONTAL_FACING);
         Direction end = stairEnd(context.getClickedFace(), context.getClickLocation(), source, uphill);
         if (end == null) {
            return null;
         }
         return new Attachment(source.relative(end).offset(0, end == uphill ? 1 : -1, 0), uphill);
      }
      if (!state.is(ModBlocks.STEEL_CATWALK.get()) && !state.is(ModBlocks.STEEL_CATWALK_TA.get())
         && !state.is(ModBlocks.STEEL_FLOOR_GRATING.get())) {
         return null;
      }
      Direction edge = selectedEdge(context.getClickedFace(), context.getClickLocation(), source);
      if (edge == null) {
         return null;
      }
      // A bottom-mounted catwalk ends at the bottom of its cell; raised decks end at the top.
      BlockPos target = source.relative(edge);
      if (state.is(ModBlocks.STEEL_CATWALK.get())) {
         target = target.below();
      }
      return new Attachment(target, edge.getOpposite());
   }

   public static BlockPlaceContext context(UseOnContext original, Attachment attachment) {
      return new AttachedContext(original, attachment);
   }

   private static BlockPos originalClickedPos(BlockPlaceContext context) {
      return context.replacingClickedOnBlock() ? context.getClickedPos()
         : context.getClickedPos().relative(context.getClickedFace().getOpposite());
   }

   @Nullable
   private static Direction selectedEdge(Direction face, Vec3 hit, BlockPos pos) {
      if (face.getAxis().isHorizontal()) {
         return face;
      }
      double x = hit.x - pos.getX();
      double z = hit.z - pos.getZ();
      double distance = EDGE_BAND;
      Direction edge = null;
      // Stable tie order matches the existing border-selection convention.
      if (z <= distance) {
         distance = z;
         edge = Direction.NORTH;
      }
      if (1.0 - z < distance) {
         distance = 1.0 - z;
         edge = Direction.SOUTH;
      }
      if (x < distance) {
         distance = x;
         edge = Direction.WEST;
      }
      if (1.0 - x < distance) {
         edge = Direction.EAST;
      }
      return edge;
   }

   @Nullable
   private static Direction stairEnd(Direction face, Vec3 hit, BlockPos pos, Direction uphill) {
      if (face == uphill || face == uphill.getOpposite()) {
         return face;
      }
      double along = (hit.x - pos.getX() - 0.5) * uphill.getStepX()
         + (hit.z - pos.getZ() - 0.5) * uphill.getStepZ();
      if (Math.abs(along) < 1.0E-6) {
         return null;
      }
      // Top, underside and lateral clicks select the end physically under the cursor,
      // independently of the player's look direction (including a steep downward look).
      return along > 0 ? uphill : uphill.getOpposite();
   }

   public record Attachment(BlockPos target, Direction facing) {
   }

   private static final class AttachedContext extends BlockPlaceContext {
      private final Attachment attachment;

      private AttachedContext(UseOnContext original, Attachment attachment) {
         super(original.getLevel(), original.getPlayer(), original.getHand(), original.getItemInHand(),
            new BlockHitResult(Vec3.atCenterOf(attachment.target()), original.getClickedFace(), attachment.target(), false));
         this.attachment = attachment;
         // Pin the chosen cell even when it is occupied: never shift to a second cell on failure.
         this.replaceClicked = true;
      }

      @Override
      public boolean canPlace() {
         return getLevel().getBlockState(getClickedPos()).canBeReplaced(this);
      }
   }
}
