package com.oblixorprime.engineersdecorreforged.rsgauges;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A two-block cabinet. Missing saved part properties deliberately remain legacy singles. */
public class IndustrialSwitchBlock extends ControlsBlockTypes.ToggleSwitchBlock {
   public static final MapCodec<IndustrialSwitchBlock> CODEC = simpleCodec(IndustrialSwitchBlock::new);
   public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
   private static final VoxelShape NORTH = Block.box(0, 0, 7.5, 16, 16, 16);
   private static final VoxelShape EAST = Block.box(0, 0, 0, 8.5, 16, 16);
   private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 8.5);
   private static final VoxelShape WEST = Block.box(7.5, 0, 0, 16, 16, 16);

   public IndustrialSwitchBlock(Properties properties) {
      super(properties.pushReaction(PushReaction.BLOCK));
      registerDefaultState(defaultBlockState().setValue(PART, Part.LEGACY));
   }

   @Override
   protected MapCodec<? extends DirectionalBlock> codec() { return CODEC; }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(PART);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos pos = context.getClickedPos();
      if (pos.getY() >= context.getLevel().getMaxBuildHeight() - 1
         || !context.getLevel().getBlockState(pos.above()).canBeReplaced(context)) {
         return null;
      }
      Direction face = context.getClickedFace();
      Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
      return defaultBlockState().setValue(PART, Part.LOWER).setValue(FACING, facing);
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      if (state.getValue(PART) == Part.LOWER) {
         level.setBlock(pos.above(), state.setValue(PART, Part.UPPER), 3);
      }
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return state.getValue(PART) != Part.UPPER || isPart(level.getBlockState(pos.below()), Part.LOWER);
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      Part part = state.getValue(PART);
      if (part == Part.UPPER && direction == Direction.DOWN) {
         return isPart(neighbor, Part.LOWER) ? neighbor.setValue(PART, Part.UPPER) : Blocks.AIR.defaultBlockState();
      }
      if (part == Part.LOWER && direction == Direction.UP && !isPart(neighbor, Part.UPPER)) {
         return Blocks.AIR.defaultBlockState();
      }
      return state;
   }

   private boolean isPart(BlockState state, Part part) {
      return state.is(this) && state.getValue(PART) == part;
   }

   @Override
   protected void setPowered(Level level, BlockPos pos, BlockState state, boolean powered) {
      if (state.getValue(PART) == Part.UPPER) {
         pos = pos.below();
         state = level.getBlockState(pos);
         if (!isPart(state, Part.LOWER)) { return; }
      }
      level.setBlock(pos, state.setValue(ControlsBlockTypes.POWERED, powered), 3);
      notifySignalNeighbors(level, pos);
      if (state.getValue(PART) == Part.LOWER) {
         notifySignalNeighbors(level, pos.above());
      }
   }

   private void notifySignalNeighbors(Level level, BlockPos pos) {
      level.updateNeighborsAt(pos, this);
      // Direct output is exposed on every side, so every adjacent conductor can relay it.
      for (Direction side : Direction.values()) {
         level.updateNeighborsAt(pos.relative(side), this);
      }
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
      super.onRemove(state, level, pos, replacement, moving);
      if (!state.is(replacement.getBlock()) && state.getValue(ControlsBlockTypes.POWERED)) {
         notifySignalNeighbors(level, pos);
      }
   }

   @Override
   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide && player.isCreative() && state.getValue(PART) == Part.UPPER) {
         BlockPos lower = pos.below();
         if (isPart(level.getBlockState(lower), Part.LOWER)) {
            level.setBlock(lower, Blocks.AIR.defaultBlockState(), 35);
         }
      }
      return super.playerWillDestroy(level, pos, state, player);
   }

   @Override
   protected BlockState rotate(BlockState state, Rotation rotation) {
      return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState state, Mirror mirror) {
      return rotate(state, mirror.getRotation(state.getValue(FACING)));
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      Direction facing = state.getValue(FACING);
      if (state.getValue(PART) == Part.LEGACY) {
         // Preserve original model rotations, including the unrotated UP/DOWN variants.
         facing = facing.getAxis().isHorizontal() ? facing.getOpposite() : Direction.NORTH;
      }
      VoxelShape shape = switch (facing) {
         case EAST -> EAST;
         case SOUTH -> SOUTH;
         case WEST -> WEST;
         default -> NORTH;
      };
      if (state.getValue(PART) == Part.LEGACY) {
         var bounds = shape.bounds();
         return Block.box(bounds.minX * 16, 0, bounds.minZ * 16, bounds.maxX * 16, 32, bounds.maxZ * 16);
      }
      return shape;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return getShape(state, level, pos, context);
   }

   public enum Part implements StringRepresentable {
      LEGACY, LOWER, UPPER;
      @Override
      public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
   }
}
