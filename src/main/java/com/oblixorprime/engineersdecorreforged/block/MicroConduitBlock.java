package com.oblixorprime.engineersdecorreforged.block;

import com.mojang.serialization.MapCodec;
import com.oblixorprime.engineersdecorreforged.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One 16x16x16 placement, nine physically parallel but electrically independent LV lanes. */
public final class MicroConduitBlock extends Block {
   public static final MapCodec<MicroConduitBlock> CODEC = simpleCodec(MicroConduitBlock::new);
   public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
   public static final BooleanProperty SUPPORTED = BooleanProperty.create("supported");

   public MicroConduitBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any()
         .setValue(AXIS, Direction.Axis.X).setValue(SUPPORTED, false));
   }

   @Override
   protected MapCodec<? extends Block> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(AXIS, SUPPORTED);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      // Horizontal placement on a floor/ceiling; vertical riser when aiming at a wall head-on.
      Direction side = context.getClickedFace();
      Direction view = context.getHorizontalDirection();
      Direction.Axis axis = side.getAxis() == Direction.Axis.Y
         ? view.getAxis() : side.getAxis() == view.getAxis() ? Direction.Axis.Y : view.getAxis();
      return defaultBlockState().setValue(AXIS, axis);
   }

   @Override
   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return switch (state.getValue(AXIS)) {
         case X -> Block.box(0, 1.8, 2.1, 16, 14.2, 4.6);
         case Y -> Block.box(1.8, 0, 2.1, 14.2, 16, 4.6);
         case Z -> Block.box(2.1, 1.8, 0, 4.6, 14.2, 16);
      };
   }

   @Override
   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return getShape(state, level, pos, context);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      // Remove the paid S01 attachment, returning exactly one item.
      if (!player.isShiftKeyDown() || !state.getValue(SUPPORTED)) return InteractionResult.PASS;
      if (!level.isClientSide) {
         level.setBlock(pos, state.setValue(SUPPORTED, false), Block.UPDATE_ALL);
         if (!player.getAbilities().instabuild) popResource(level, pos, new ItemStack(ModItems.MICROCONDUIT_SUPPORT.get()));
      }
      return InteractionResult.SUCCESS;
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      if (!state.is(newState.getBlock()) && state.getValue(SUPPORTED) && !level.isClientSide) {
         popResource(level, pos, new ItemStack(ModItems.MICROCONDUIT_SUPPORT.get()));
      }
      super.onRemove(state, level, pos, newState, moving);
   }
}
