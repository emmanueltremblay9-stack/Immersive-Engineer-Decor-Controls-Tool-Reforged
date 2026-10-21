package com.oblixorprime.engineersdecorreforged.tools;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class AriadneCoalItem extends TooltipItem {
   public AriadneCoalItem(Properties properties) {
      super("ariadne_coal", properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      }

      Level level = context.getLevel();
      Direction face = context.getClickedFace();
      BlockPos clickedPos = context.getClickedPos();
      BlockPos placePos = context.getClickedPos().relative(face);
      BlockState marker = (BlockState)((BlockState)((PortedBlocks.AriadneMarkerBlock)ModBlocks.ARIADNE_MARKER.get())
            .defaultBlockState()
            .setValue(PortedBlocks.FACING, face))
         .setValue(PortedBlocks.ARIADNE_MARKER_ROTATION, markerDirection(context));
      marker = NativeFluidloggedBlock.placement(marker, level.getFluidState(placePos));
      if (level.getBlockState(placePos).canBeReplaced()
         && level.getBlockState(clickedPos).isFaceSturdy(level, clickedPos, face)
         && marker.canSurvive(level, placePos)) {
         if (!level.isClientSide) {
            if (!level.setBlock(placePos, marker, 11)) return InteractionResult.FAIL;
            NativeFluidloggedBlock.schedule(marker, level, placePos);
            ItemStack stack = context.getItemInHand();
            boolean broke = false;
            if (!player.getAbilities().instabuild) {
               stack.setDamageValue(stack.getDamageValue() + 1);
               if (stack.getDamageValue() >= stack.getMaxDamage()) {
                  player.setItemInHand(context.getHand(), ItemStack.EMPTY);
                  broke = true;
               }
            }

            level.playSound(null, placePos, broke ? SoundEvents.WOOD_BREAK : SoundEvents.GRAVEL_HIT, player.getSoundSource(), 0.4F, 2.0F);
            player.displayClientMessage(Component.translatable("item.immersive_engineer_decor_controls_tool_reforged.ariadne_coal.msg.placed"), true);
         }

         return InteractionResult.SUCCESS;
      } else {
         return fail(level, player);
      }
   }

   private static InteractionResult fail(Level level, Player player) {
      if (!level.isClientSide) {
         player.displayClientMessage(Component.translatable("item.immersive_engineer_decor_controls_tool_reforged.ariadne_coal.msg.badtarget"), true);
      }

      return InteractionResult.FAIL;
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
