package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
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
public final class VerticalDoubleTSupportBlock extends HorizontalDirectionalBlock implements NativeTransientFluidBlock,DoubleTLightHost {
   public static final MapCodec<VerticalDoubleTSupportBlock> CODEC=simpleCodec(VerticalDoubleTSupportBlock::new);
   public static final BooleanProperty WALL_HORIZONTAL=BooleanProperty.create("wall_horizontal");
   public static final BooleanProperty JOIN_NEGATIVE=BooleanProperty.create("join_negative");
   public static final BooleanProperty JOIN_POSITIVE=BooleanProperty.create("join_positive");
   public static final BooleanProperty CEILING_JOIN=BooleanProperty.create("ceiling_join");
   private static final VoxelShape NORTH=Shapes.or(
      Block.box(5,0,15,11,16,16),Block.box(9,0,12,10,16,15),
      Block.box(6,0,12,7,16,15),Block.box(5,0,11,11,16,12)).optimize();
   // Same four cuboids as steel_double_t_support_xconnect_model, after the upright rotation.
   private static final VoxelShape POSITIVE=Shapes.or(
      Block.box(11,5,15,16,11,16),Block.box(11,5,11,16,11,12),
      Block.box(10,6,12,16,7,15),Block.box(10,9,12,16,10,15)).optimize();
   // Extends the upright only into its directly adjacent aligned ceiling beam.
   private static final VoxelShape CEILING_END=Shapes.or(
      Block.box(5,16,15,11,27,16),Block.box(9,16,12,10,27,15),
      Block.box(6,16,12,7,27,15),Block.box(5,16,11,11,27,12));
   // A two-pixel lap bridges the existing wall half-cross into a horizontal beam's underside/side flange.
   private static final VoxelShape[][] SIDE_JOIN_BRIDGES=new VoxelShape[4][2];
   private static final VoxelShape[][][] SHAPES=new VoxelShape[2][4][8];
   static {
      VoxelShape positiveBridge=Block.box(10,10,10,16,12,12),negativeBridge=Block.box(0,10,10,6,12,12);
      for(int facing=0;facing<4;facing++) {
         SIDE_JOIN_BRIDGES[facing][0]=positiveBridge;
         SIDE_JOIN_BRIDGES[facing][1]=negativeBridge;
         positiveBridge=clockwise(positiveBridge);negativeBridge=clockwise(negativeBridge);
      }
      for(int mount=0;mount<2;mount++)for(int joins=0;joins<8;joins++) {
         VoxelShape shape=NORTH;
         if((joins&1)!=0)shape=Shapes.or(shape,inWallHalfTurn(POSITIVE));
         if((joins&2)!=0)shape=Shapes.or(shape,POSITIVE);
         if((joins&4)!=0&&mount==0)shape=Shapes.or(shape,CEILING_END);
         if(mount==1)shape=inWallClockwise(shape);
         for(int facing=0;facing<4;facing++){SHAPES[mount][facing][joins]=shape.optimize();shape=clockwise(shape);}
      }
   }

   public VerticalDoubleTSupportBlock(Properties properties) {
      // The tiny ceiling-side lap depends on the directly adjacent beam type, so do not cache this shape by BlockState alone.
      super(properties.dynamicShape().lightLevel(state->Math.max(NativeFluidloggedBlock.light(state),SupportLights.attached(state)?14:0)));
      registerDefaultState(NativeFluidloggedBlock.dry(defaultBlockState()).setValue(FACING,Direction.NORTH)
         .setValue(WALL_HORIZONTAL,false).setValue(JOIN_NEGATIVE,false).setValue(JOIN_POSITIVE,false)
         .setValue(CEILING_JOIN,false).setValue(SupportLights.ATTACHED,false));
   }
   @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
   @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
      builder.add(FACING,WALL_HORIZONTAL,JOIN_NEGATIVE,JOIN_POSITIVE,CEILING_JOIN,SupportLights.ATTACHED,
         BlockStateProperties.WATERLOGGED,NativeFluidloggedBlock.FLUID_CELL,StairWaterFlow.WATER_FLOW);
   }
   public static boolean horizontalAt(double horizontal,double vertical) {
      double x=Math.abs(horizontal-.5),y=Math.abs(vertical-.5);
      return x>.25&&x>y+1.e-7;
   }
   @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction face=context.getClickedFace();
      if(!face.getAxis().isHorizontal())return null;
      var pos=context.getClickedPos();
      var state=defaultBlockState().setValue(FACING,face).setValue(WALL_HORIZONTAL,false);
      return NativeFluidloggedBlock.placement(connected(state,context.getLevel(),pos),context);
   }
   @Override public ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state) {
      return new ItemStack(ModBlocks.STEEL_DOUBLE_T_SUPPORT.get());
   }
   private static Direction positive(BlockState state) {
      // In-wall clockwise rotation maps the former right arm to the lower arm.
      return state.getValue(WALL_HORIZONTAL)?Direction.DOWN:state.getValue(FACING).getClockWise();
   }
   /** A wall-mounted support can continue into a directly touching horizontal steel or wood beam. */
   public static boolean connectsToCeilingBeam(BlockState wallState,BlockState ceilingState,Direction direction) {
      if(!(wallState.getBlock() instanceof VerticalDoubleTSupportBlock)
         ||!direction.getAxis().isHorizontal())return false;
      Direction arm=positive(wallState);
      if(direction!=arm&&direction!=arm.getOpposite())return false;
      return ceilingState.getBlock() instanceof PortedBlocks.SupportBlock||TreatedWoodBeamBlock.ceilingBeam(ceilingState);
   }
   private static boolean compatible(BlockState state,BlockState neighbor,Direction direction) {
      return TreatedWoodBeamBlock.orthogonalWall(state,neighbor)||connectsToCeilingBeam(state,neighbor,direction);
   }
   private static BlockState connected(BlockState state,BlockGetter level,BlockPos pos) {
      Direction direction=positive(state);
      Direction negative=direction.getOpposite();
      return state.setValue(JOIN_NEGATIVE,compatible(state,level.getBlockState(pos.relative(negative)),negative))
         .setValue(JOIN_POSITIVE,compatible(state,level.getBlockState(pos.relative(direction)),direction))
         .setValue(CEILING_JOIN,!state.getValue(WALL_HORIZONTAL)&&alignedCeiling(state,level.getBlockState(pos.above())));
   }
   private static boolean alignedCeiling(BlockState state,BlockState above) {
      Direction.Axis axis=state.getValue(FACING).getAxis();
      if(above.getBlock() instanceof PortedBlocks.SupportBlock)
         return above.getValue(PortedBlocks.EASTWEST)==(axis==Direction.Axis.X);
      return TreatedWoodBeamBlock.ceilingBeam(above)&&above.getValue(FACING).getAxis()==axis;
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
   @Override public VoxelShape mountingShape(BlockState state,BlockGetter level,BlockPos pos){return supportShape(state.setValue(JOIN_NEGATIVE,false).setValue(JOIN_POSITIVE,false).setValue(CEILING_JOIN,false),level,pos);}
   @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SupportLights.outline(state,level,pos,supportShape(state,level,pos));}
   @Override public VoxelShape supportShape(BlockState state,BlockGetter level,BlockPos pos) {
      int facing=switch(state.getValue(FACING)){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
      int joins=(state.getValue(JOIN_NEGATIVE)?1:0)|(state.getValue(JOIN_POSITIVE)?2:0)|(state.getValue(CEILING_JOIN)?4:0);
      VoxelShape shape=SHAPES[state.getValue(WALL_HORIZONTAL)?1:0][facing][joins];
      if(!state.getValue(WALL_HORIZONTAL)) {
         Direction positive=positive(state),negative=positive.getOpposite();
         if(state.getValue(JOIN_POSITIVE)&&connectsToCeilingBeam(state,level.getBlockState(pos.relative(positive)),positive))
            shape=Shapes.or(shape,SIDE_JOIN_BRIDGES[facing][0]);
         if(state.getValue(JOIN_NEGATIVE)&&connectsToCeilingBeam(state,level.getBlockState(pos.relative(negative)),negative))
            shape=Shapes.or(shape,SIDE_JOIN_BRIDGES[facing][1]);
      }
      return shape.optimize();
   }
   @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return supportShape(state,level,pos);}
   @Override public boolean onDestroyedByPlayer(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,boolean harvest,FluidState fluid){
      if(player==null)return super.onDestroyedByPlayer(state,level,pos,player,harvest,fluid);
      var completion=RailingMining.complete(player,level,pos,state);if(completion==null)return false;
      return completion.part().kind()==PortedBlocks.PartKind.SUPPORT_LIGHT?SupportLights.remove(state,level,pos,player,harvest,completion):super.onDestroyedByPlayer(state,level,pos,player,harvest,fluid);
   }
   @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){return SupportLights.drops(state,params,super.getDrops(state,params));}
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
