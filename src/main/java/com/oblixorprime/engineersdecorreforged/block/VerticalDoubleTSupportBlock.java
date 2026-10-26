package com.oblixorprime.engineersdecorreforged.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The original four beam cuboids rigidly rotated upright, backing the clicked wall. */
public final class VerticalDoubleTSupportBlock extends HorizontalDirectionalBlock implements NativeTransientFluidBlock {
   public static final MapCodec<VerticalDoubleTSupportBlock> CODEC=simpleCodec(VerticalDoubleTSupportBlock::new);
   private static final VoxelShape NORTH=Shapes.or(
      Block.box(5,0,15,11,16,16),Block.box(9,0,12,10,16,15),
      Block.box(6,0,12,7,16,15),Block.box(5,0,11,11,16,12)).optimize();
   private static final VoxelShape EAST=clockwise(NORTH);
   private static final VoxelShape SOUTH=clockwise(EAST);
   private static final VoxelShape WEST=clockwise(SOUTH);

   public VerticalDoubleTSupportBlock(Properties properties) {
      super(properties);
      registerDefaultState(NativeFluidloggedBlock.dry(defaultBlockState()).setValue(FACING,Direction.NORTH));
   }

   @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
   @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
      builder.add(FACING,BlockStateProperties.WATERLOGGED,NativeFluidloggedBlock.FLUID_CELL,StairWaterFlow.WATER_FLOW);
   }

   @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction face=context.getClickedFace();
      return face.getAxis().isHorizontal()
         ?NativeFluidloggedBlock.placement(defaultBlockState().setValue(FACING,face),context):null;
   }

   @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
   @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
   @Override public FluidState getFluidState(BlockState state){return StairWaterFlow.fluid(state);}
   @Override protected BlockState updateShape(BlockState state,Direction face,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
      NativeFluidloggedBlock.schedule(state,level,pos);
      return super.updateShape(state,face,neighbor,level,pos,neighborPos);
   }
   @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
      return switch(state.getValue(FACING)){case EAST->EAST;case SOUTH->SOUTH;case WEST->WEST;default->NORTH;};
   }
   @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return getShape(state,level,pos,context);}
   private static VoxelShape clockwise(VoxelShape shape) {
      VoxelShape result=Shapes.empty();
      for(var b:shape.toAabbs())result=Shapes.or(result,Shapes.create(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX));
      return result.optimize();
   }
}
