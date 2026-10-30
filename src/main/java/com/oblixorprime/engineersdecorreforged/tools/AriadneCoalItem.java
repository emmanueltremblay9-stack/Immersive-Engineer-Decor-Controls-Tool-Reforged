package com.oblixorprime.engineersdecorreforged.tools;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class AriadneCoalItem extends NonEnchantableTooltipItem {
   public AriadneCoalItem(Properties properties) {
      super("ariadne_coal", properties);
   }

   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
      Level level = context.getLevel();
      if (level.isClientSide) {
         return InteractionResult.PASS;
      }
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      }

      Direction face = context.getClickedFace();
      BlockPos clickedPos = context.getClickedPos();
      BlockPos placePos = context.getClickedPos().relative(face);
      BlockState clickedState = level.getBlockState(clickedPos);
      BlockState marker = (BlockState)((BlockState)((PortedBlocks.AriadneMarkerBlock)ModBlocks.ARIADNE_MARKER.get())
            .defaultBlockState()
            .setValue(PortedBlocks.FACING, face))
         .setValue(PortedBlocks.ARIADNE_MARKER_ROTATION, markerDirection(context));
      if ((level.getBlockState(placePos).canBeReplaced() || clickedState.is(ModBlocks.ARIADNE_MARKER.get()))
         && clickedState.isFaceSturdy(level, clickedPos, face)
         && marker.canSurvive(level, placePos)) {
         if (level.setBlock(placePos, marker, 3)) {
            boolean broke = false;
            stack.setDamageValue(stack.getDamageValue() + 1);
            if (stack.getDamageValue() >= stack.getMaxDamage()) {
               player.setItemInHand(context.getHand(), ItemStack.EMPTY);
               broke = true;
            }

            level.playSound(null, clickedPos, broke ? SoundEvents.WOOD_BREAK : SoundEvents.GRAVEL_HIT, player.getSoundSource(), 0.4F, 2.0F);
            return InteractionResult.SUCCESS;
         }
         return InteractionResult.FAIL;
      }
      return InteractionResult.PASS;
   }

   private static int markerDirection(UseOnContext context) {
      BlockPos pos = context.getClickedPos();
      Vec3 hit = context.getClickLocation();
      double x = clampUnit(hit.x - pos.getX());
      double y = clampUnit(hit.y - pos.getY());
      double z = clampUnit(hit.z - pos.getZ());
      Direction face = context.getClickedFace();

      double horizontal = switch (face) {
         case WEST -> z;
         case EAST -> 1.0 - z;
         case NORTH -> 1.0 - x;
         case SOUTH, UP, DOWN -> x;
         default -> throw new MatchException(null, null);
      };

      double vertical = switch (face) {
         case UP, DOWN -> z;
         default -> y;
      };
      return nearestOriginalDirection(horizontal, vertical);
   }

   private static int nearestOriginalDirection(double horizontal, double vertical) {
      double dx = horizontal - 0.5;
      double dy = vertical - 0.5;
      return Math.floorMod((int)Math.round(Math.atan2(dy, dx) / (Math.PI / 4)), 8);
   }

   private static double clampUnit(double value) {
      return Math.max(0.0, Math.min(1.0, value));
   }
}
