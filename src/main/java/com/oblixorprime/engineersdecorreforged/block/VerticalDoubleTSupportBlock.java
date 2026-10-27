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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Original Double T profile and half-cross arms, rigidly oriented in the clicked wall plane. */
public final class VerticalDoubleTSupportBlock extends HorizontalDirectionalBlock implements NativeTransientFluidBlock {
   public static final MapCodec<VerticalDoubleTSupportBlock> CODEC=simpleCodec(VerticalDoubleTSupportBlock::new);
   public static final BooleanProperty WALL_HORIZONTAL=BooleanProperty.create("wall_horizontal");
   public static final BooleanProperty JOIN_NEGATIVE=BooleanProperty.create("join_negative");
   public static final BooleanProperty JOIN_POSITIVE=BooleanProperty.create("join_positive");
   private static final VoxelShape NORTH=Shapes.or(
      Block.box(5,0,15,11,16,16),Block.box(9,0,12,10,16,15),
      Block.box(6,0,12,7,16,15),Block.box(5,0,11,11,16,12)).optimize();
   // Same four cuboids as steel_double_t_support_xconnect_model, after the upright rotation.
   private static final VoxelShape POSITIVE=Shapes.or(
      Block.box(11,5,15,16,11,16),Block.box(11,5,11,16,11,12),
      Block.box(10,6,12,16,7,15),Block.box(10,9,12,16,10,15)).optimize();
   private static final VoxelShape[][][] SHAPES=new VoxelShape[2][4][4];
   static {
      for(int mount=0;mount<2;mount++)for(int joins=0;joins<4;joins++) {
         VoxelShape shape=NORTH;
         if((joins&1)!=0)shape=Shapes.or(shape,inWallHalfTurn(POSITIVE));
         if((joins&2)!=0)shape=Shapes.or(shape,POSITIVE);
         if(mount==1)shape=inWallClockwise(shape);
         for(int facing=0;facing<4;facing++){SHAPES[mount][facing][joins]=shape.optimize();shape=clockwise(shape);}
      }
   }

   public VerticalDoubleTSupportBlock(Properties properties) {
      super(properties);
      registerDefaultState(NativeFluidloggedBlock.dry(defaultBlockState()).setValue(FACING,Direction.NORTH)
         .setValue(WALL_HORIZONTAL,false).setValue(JOIN_NEGATIVE,false).setValue(JOIN_POSITIVE,false));
   }
   @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
   @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
      builder.add(FACING,WALL_HORIZONTAL,JOIN_NEGATIVE,JOIN_POSITIVE,
         BlockStateProperties.WATERLOGGED,NativeFluidloggedBlock.FLUID_CELL,StairWaterFlow.WATER_FLOW);
   }
   public static boolean horizontalAt(double horizontal,double vertical) {
      double x=Math.abs(horizontal-.5),y=Math.abs(vertical-.5);
      return x>.25&&x>y+1.e-7;
   }
   @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction face=context.getClickedFace();
      if(!face.getAxis().isHorizontal())return null;
      var hit=context.getClickLocation();var pos=context.getClickedPos();
      double horizontal=face.getAxis()==Direction.Axis.Z?hit.x-pos.getX():hit.z-pos.getZ();
      var state=defaultBlockState().setValue(FACING,face).setValue(WALL_HORIZONTAL,horizontalAt(horizontal,hit.y-pos.getY()));
      return NativeFluidloggedBlock.placement(connected(state,context.getLevel(),pos),context);
   }
   private static Direction positive(BlockState state) {
      // In-wall clockwise rotation maps the former right arm to the lower arm.
      return state.getValue(WALL_HORIZONTAL)?Direction.DOWN:state.getValue(FACING).getClockWise();
   }
   private static boolean compatible(BlockState state,BlockState neighbor) {
      return neighbor.getBlock() instanceof VerticalDoubleTSupportBlock&&neighbor.getValue(FACING)==state.getValue(FACING);
   }
   private static BlockState connected(BlockState state,BlockGetter level,BlockPos pos) {
      Direction direction=positive(state);
      return state.setValue(JOIN_NEGATIVE,compatible(state,level.getBlockState(pos.relative(direction.getOpposite()))))
         .setValue(JOIN_POSITIVE,compatible(state,level.getBlockState(pos.relative(direction))));
   }
   @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
   @Override protected BlockState mirror(BlockState state,Mirror mirror) {
      Direction next=mirror.mirror(state.getValue(FACING));
      BlockState result=state.setValue(FACING,next);
      return mirror.mirror(positive(state))==positive(result)?result:result
         .setValue(JOIN_NEGATIVE,state.getValue(JOIN_POSITIVE)).setValue(JOIN_POSITIVE,state.getValue(JOIN_NEGATIVE));
   }
   @Override public FluidState getFluidState(BlockState state){return StairWaterFlow.fluid(state);}
   @Override protected BlockState updateShape(BlockState state,Direction face,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
      NativeFluidloggedBlock.schedule(state,level,pos);
      return connected(state,level,pos);
   }
   @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
      int facing=switch(state.getValue(FACING)){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
      int joins=(state.getValue(JOIN_NEGATIVE)?1:0)|(state.getValue(JOIN_POSITIVE)?2:0);
      return SHAPES[state.getValue(WALL_HORIZONTAL)?1:0][facing][joins];
   }
   @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return getShape(state,level,pos,context);}
   private static VoxelShape inWallHalfTurn(VoxelShape shape) {
      VoxelShape result=Shapes.empty();for(var b:shape.toAabbs())result=Shapes.or(result,Shapes.box(1-b.maxX,1-b.maxY,b.minZ,1-b.minX,1-b.minY,b.maxZ));return result;
   }
   private static VoxelShape inWallClockwise(VoxelShape shape) {
      VoxelShape result=Shapes.empty();for(var b:shape.toAabbs())result=Shapes.or(result,Shapes.box(b.minY,1-b.maxX,b.minZ,b.maxY,1-b.minX,b.maxZ));return result;
   }
   private static VoxelShape clockwise(VoxelShape shape) {
      VoxelShape result=Shapes.empty();for(var b:shape.toAabbs())result=Shapes.or(result,Shapes.box(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX));return result;
   }
}
