package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * S01 is a real, paid structural attachment to C01 occupying the same voxel.
 * This is not a separate wire conductor or a second full block colliding with C01.
 */
public final class MicroSupportItem extends Item {
   public MicroSupportItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      BlockState state = level.getBlockState(context.getClickedPos());
      if (!state.is(ModBlocks.MICROCONDUIT_9LV.get()) || state.getValue(MicroConduitBlock.SUPPORTED)) {
         return InteractionResult.PASS;
      }
      if (!level.isClientSide) {
         level.setBlock(context.getClickedPos(), state.setValue(MicroConduitBlock.SUPPORTED, true), Block.UPDATE_ALL);
         if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
         }
      }
      return InteractionResult.SUCCESS;
   }
}
