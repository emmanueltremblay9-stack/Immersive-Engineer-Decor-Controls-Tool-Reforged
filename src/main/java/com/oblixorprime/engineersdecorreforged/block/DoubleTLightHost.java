package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Structural shape only: decorative attachments never add physical collision. */
public interface DoubleTLightHost extends EntityBlock {
   VoxelShape supportShape(BlockState state, BlockGetter level, BlockPos pos);
   default VoxelShape mountingShape(BlockState state,BlockGetter level,BlockPos pos){return supportShape(state,level,pos);}
   @Override default net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new DoubleTLightBlockEntity(pos,state);
   }
}
