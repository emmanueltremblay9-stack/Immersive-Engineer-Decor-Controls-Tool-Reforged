package com.oblixorprime.engineersdecorreforged.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** New MK3/MK3.1 prototype block. Does not replace either legacy steel Double-T registry ID. */
public final class HeavyDoubleTSupportBlock extends NativeDecorativeBlocks.NativeOpenBlock implements NativeFluidloggedBlock.CompactState {
   public static final MapCodec<HeavyDoubleTSupportBlock> CODEC = simpleCodec(HeavyDoubleTSupportBlock::new);
   public static final DirectionProperty FACING = BlockStateProperties.FACING;
   public static final IntegerProperty ROLL = IntegerProperty.create("roll", 0, 3);

   // I-profile, short cantilever and visually supported brace. Decorative connector plates are
   // selected from world context; collision remains the core profile only.
   private static final VoxelShape CORE_LOCAL = Shapes.or(
      Block.box(2, 0, 2, 14, 16, 4),
      Block.box(7, 0, 4, 9, 16, 12),
      Block.box(2, 0, 12, 14, 16, 14),
      Block.box(0, 11.5, 5.5, 8, 14, 10.5),
      Block.box(2, 4, 6, 10, 12, 10)
   ).optimize();
   private static final VoxelShape[][] COLLISIONS = new VoxelShape[Direction.values().length][4];
   static {
      for (Direction facing : Direction.values()) for (int roll = 0; roll < 4; roll++) {
         COLLISIONS[facing.ordinal()][roll] = HeavyDoubleTOrientation.shape(CORE_LOCAL, facing, roll);
      }
   }

   public HeavyDoubleTSupportBlock(Properties properties) {
      super(properties.noOcclusion().dynamicShape());
      registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP).setValue(ROLL, 0));
   }

   @Override protected MapCodec<? extends Block> codec() { return CODEC; }

   @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(FACING, ROLL);
   }

   @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction face = context.getClickedFace();
      return NativeFluidloggedBlock.placement(
         defaultBlockState().setValue(FACING, face).setValue(ROLL, clickRoll(context, face)), context);
   }

   /** Four quadrants of the clicked face control roll, for all six placement faces. */
   private static int clickRoll(BlockPlaceContext context, Direction face) {
      Vec3 hit = context.getClickLocation();
      double x = hit.x - Math.floor(hit.x) - .5;
      double y = hit.y - Math.floor(hit.y) - .5;
      double z = hit.z - Math.floor(hit.z) - .5;
      Direction right = HeavyDoubleTOrientation.right(face, 0);
      Direction depth = HeavyDoubleTOrientation.depth(face, 0);
      double u = x * right.getStepX() + y * right.getStepY() + z * right.getStepZ();
      double v = x * depth.getStepX() + y * depth.getStepY() + z * depth.getStepZ();
      if (Math.abs(u) < 1e-5 && Math.abs(v) < 1e-5) return 0;
      return Math.abs(u) >= Math.abs(v) ? (u >= 0 ? 0 : 2) : (v >= 0 ? 1 : 3);
   }

   @Override protected BlockState rotate(BlockState state, Rotation rotation) {
      Direction newFacing = rotation.rotate(state.getValue(FACING));
      Direction newRight = rotation.rotate(HeavyDoubleTOrientation.right(state.getValue(FACING), state.getValue(ROLL)));
      return state.setValue(FACING, newFacing).setValue(ROLL, HeavyDoubleTOrientation.rollFor(newFacing, newRight));
   }

   @Override protected BlockState mirror(BlockState state, Mirror mirror) {
      // Proper quarter-turn orientation retained. A mirror of the asymmetric bracket needs
      // a separately mirrored asset and is intentionally NOT claimed as fidelity-preserving.
      Direction nextFacing = mirror.mirror(state.getValue(FACING));
      Direction nextRight = mirror.mirror(HeavyDoubleTOrientation.right(state.getValue(FACING), state.getValue(ROLL)));
      return state.setValue(FACING, nextFacing).setValue(ROLL, HeavyDoubleTOrientation.rollFor(nextFacing, nextRight));
   }

   @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return COLLISIONS[state.getValue(FACING).ordinal()][state.getValue(ROLL)];
   }

   @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return COLLISIONS[state.getValue(FACING).ordinal()][state.getValue(ROLL)];
   }

   @Override protected BlockState updateShape(BlockState state, Direction changedSide, BlockState neighbor,
      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      // Contextual model data contains no saved stale neighbor mask. Flag same-state rerender
      // when an adjacent block changes; don't convert or destroy already placed supports.
      NativeFluidloggedBlock.schedule(state, level, pos);
      if (level instanceof Level world && !world.isClientSide) {
         world.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
      }
      return state;
   }
}
