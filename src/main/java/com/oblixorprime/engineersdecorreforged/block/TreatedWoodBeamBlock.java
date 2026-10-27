package com.oblixorprime.engineersdecorreforged.block;

import com.mojang.serialization.MapCodec;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The straight pole's three wood cuboids, rigidly oriented; steel IDs remain separate. */
public final class TreatedWoodBeamBlock extends NativeDecorativeBlocks.NativeOpenBlock {
   public static final MapCodec<TreatedWoodBeamBlock> CODEC=simpleCodec(TreatedWoodBeamBlock::new);
   public enum Mount implements StringRepresentable {
      CEILING, WALL_VERTICAL, WALL_HORIZONTAL;
      @Override public String getSerializedName(){return name().toLowerCase(Locale.ROOT);}
   }
   public static final EnumProperty<Mount> MOUNT=EnumProperty.create("mount",Mount.class);
   public static final BooleanProperty JOIN_NEGATIVE=BooleanProperty.create("join_negative");
   public static final BooleanProperty JOIN_POSITIVE=BooleanProperty.create("join_positive");
   public static final BooleanProperty POLE=BooleanProperty.create("pole");
   private static final VoxelShape[][][] SHAPES=new VoxelShape[3][4][8];
   static {
      for(Mount mount:Mount.values()) for(int joins=0;joins<8;joins++) {
         VoxelShape shape=transform(profile(0,16),mount,false);
         if((joins&1)!=0)shape=Shapes.or(shape,transform(mount==Mount.WALL_HORIZONTAL?profile(10.5,16):profile(0,5.5),mount,true));
         if((joins&2)!=0)shape=Shapes.or(shape,transform(mount==Mount.WALL_HORIZONTAL?profile(0,5.5):profile(10.5,16),mount,true));
         if((joins&4)!=0&&mount==Mount.CEILING)shape=Shapes.or(shape,verticalProfile(0,11));
         for(int facing=0;facing<4;facing++){SHAPES[mount.ordinal()][facing][joins]=shape.optimize();shape=clockwise(shape);}
      }
   }
   public TreatedWoodBeamBlock(Properties properties) {
      super(properties);
      registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH)
         .setValue(MOUNT,Mount.CEILING).setValue(JOIN_NEGATIVE,false).setValue(JOIN_POSITIVE,false).setValue(POLE,false));
   }
   @Override protected MapCodec<? extends Block> codec(){return CODEC;}
   @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(BlockStateProperties.HORIZONTAL_FACING,MOUNT,JOIN_NEGATIVE,JOIN_POSITIVE,POLE);
   }
   public static Mount wallMount(double horizontal,double vertical) {
      return Math.abs(horizontal-.5)>Math.abs(vertical-.5)+1.e-7 ? Mount.WALL_HORIZONTAL:Mount.WALL_VERTICAL;
   }
   @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction face=context.getClickedFace();Mount mount=Mount.CEILING;
      if(face.getAxis().isHorizontal()) {
         var hit=context.getClickLocation();var pos=context.getClickedPos();
         double horizontal=face.getAxis()==Direction.Axis.Z?hit.x-pos.getX():hit.z-pos.getZ();
         mount=wallMount(horizontal,hit.y-pos.getY());
      } else face=context.getHorizontalDirection();
      BlockState state=defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,face).setValue(MOUNT,mount);
      return NativeFluidloggedBlock.placement(connected(state,context.getLevel(),context.getClickedPos()),context);
   }
   private BlockState connected(BlockState state,BlockGetter level,BlockPos pos) {
      Mount mount=state.getValue(MOUNT);Direction facing=state.getValue(BlockStateProperties.HORIZONTAL_FACING);
      Direction positive=mount==Mount.WALL_HORIZONTAL?Direction.UP:facing.getClockWise();
      boolean pole=mount==Mount.CEILING&&level.getBlockState(pos.below()).is(ModBlocks.TREATED_WOOD_POLE.get())
         &&level.getBlockState(pos.below()).getValue(BlockStateProperties.FACING).getAxis()==Direction.Axis.Y;
      return state.setValue(JOIN_NEGATIVE,connects(level.getBlockState(pos.relative(positive.getOpposite())),state))
         .setValue(JOIN_POSITIVE,connects(level.getBlockState(pos.relative(positive)),state)).setValue(POLE,pole);
   }
   private static boolean connects(BlockState neighbor,BlockState state) {
      Mount mount=state.getValue(MOUNT);
      if(mount==Mount.CEILING&&neighbor.getBlock() instanceof PortedBlocks.SupportBlock)return true;
      if(!(neighbor.getBlock() instanceof TreatedWoodBeamBlock))return false;
      if(mount==Mount.CEILING)return neighbor.getValue(MOUNT)==Mount.CEILING;
      return neighbor.getValue(MOUNT)!=Mount.CEILING
         &&neighbor.getValue(BlockStateProperties.HORIZONTAL_FACING)==state.getValue(BlockStateProperties.HORIZONTAL_FACING);
   }
   public static boolean ceilingBeam(BlockState state){return state.getBlock() instanceof TreatedWoodBeamBlock&&state.getValue(MOUNT)==Mount.CEILING;}
   @Override protected BlockState updateShape(BlockState state,Direction face,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
      NativeFluidloggedBlock.schedule(state,level,pos);return connected(state,level,pos);
   }
   @Override protected BlockState rotate(BlockState state,Rotation rotation) {
      return state.setValue(BlockStateProperties.HORIZONTAL_FACING,rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
   }
   @Override protected BlockState mirror(BlockState state,Mirror mirror) {
      Direction facing=state.getValue(BlockStateProperties.HORIZONTAL_FACING);
      if(mirror==Mirror.NONE)return state;
      // Reversing the local perpendicular axis swaps the two connector bits.
      Direction positive=state.getValue(MOUNT)==Mount.WALL_HORIZONTAL?Direction.UP:facing.getClockWise();
      Direction changed=mirror.mirror(facing),mirroredPositive=mirror.mirror(positive);
      BlockState result=state.setValue(BlockStateProperties.HORIZONTAL_FACING,changed);
      Direction expected=state.getValue(MOUNT)==Mount.WALL_HORIZONTAL?Direction.UP:changed.getClockWise();
      return mirroredPositive==expected?result:result.setValue(JOIN_NEGATIVE,state.getValue(JOIN_POSITIVE)).setValue(JOIN_POSITIVE,state.getValue(JOIN_NEGATIVE));
   }
   @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
      int facing=switch(state.getValue(BlockStateProperties.HORIZONTAL_FACING)){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
      int joins=(state.getValue(JOIN_NEGATIVE)?1:0)|(state.getValue(JOIN_POSITIVE)?2:0)|(state.getValue(POLE)?4:0);
      return SHAPES[state.getValue(MOUNT).ordinal()][facing][joins];
   }
   @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return getShape(state,level,pos,context);}
   private static VoxelShape profile(double min,double max) {
      return Shapes.or(Block.box(5.75,5.5,min,10.25,10.5,max),Block.box(10.25,5.75,min,10.5,10.25,max),Block.box(5.5,5.75,min,5.75,10.25,max));
   }
   private static VoxelShape verticalProfile(double min,double max) {
      return Shapes.or(Block.box(5.75,min,5.5,10.25,max,10.5),Block.box(10.25,min,5.75,10.5,max,10.25),Block.box(5.5,min,5.75,5.75,max,10.25));
   }
   private static VoxelShape transform(VoxelShape source,Mount mount,boolean cross) {
      VoxelShape result=Shapes.empty();
      for(var b:source.toAabbs()) {
         double[] lo={2,2,2},hi={-1,-1,-1};
         for(double x:new double[]{b.minX,b.maxX})for(double y:new double[]{b.minY,b.maxY})for(double z:new double[]{b.minZ,b.maxZ}) {
            double[] p=switch(mount) {
               case CEILING->cross?new double[]{z,y+5.5/16,1-x}:new double[]{x,y+5.5/16,z};
               case WALL_VERTICAL->cross?new double[]{z,x,y+5.5/16}:new double[]{x,1-z,y+5.5/16};
               case WALL_HORIZONTAL->cross?new double[]{x,1-z,y+5.5/16}:new double[]{z,x,y+5.5/16};
            };
            for(int i=0;i<3;i++){lo[i]=Math.min(lo[i],p[i]);hi[i]=Math.max(hi[i],p[i]);}
         }
         result=Shapes.or(result,Shapes.box(lo[0],lo[1],lo[2],hi[0],hi[1],hi[2]));
      }
      return result;
   }
   private static VoxelShape clockwise(VoxelShape shape) {
      VoxelShape result=Shapes.empty();for(var b:shape.toAabbs())result=Shapes.or(result,Shapes.box(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX));return result;
   }
}
