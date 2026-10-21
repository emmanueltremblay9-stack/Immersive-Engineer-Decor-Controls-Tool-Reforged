package com.oblixorprime.engineersdecorreforged.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/** Mod-owned opt-ins retain vanilla geometry and mechanics; vanilla registrations are untouched. */
public final class NativeDecorativeBlocks {
   private NativeDecorativeBlocks() {}
   public static class NativeSolidBlock extends Block implements NativeFluidloggedBlock {
      public static final MapCodec<Block> CODEC = simpleCodec(NativeSolidBlock::new);
      public NativeSolidBlock(Properties properties) {
         super(properties);
         registerDefaultState(NativeFluidloggedBlock.dry(defaultBlockState()));
      }
      @Override public MapCodec<? extends Block> codec() { return CODEC; }
      @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(NativeFluidloggedBlock.FLUID_CELL);
         builder.add(BlockStateProperties.WATERLOGGED);
      }
      @Override public FluidState getFluidState(BlockState state) { return NativeFluidloggedBlock.fluidState(state); }
      @Override public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
         return NativeFluidloggedBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
      }
      @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
         return NativeFluidloggedBlock.super.placeLiquid(level, pos, state, fluid);
      }
      @Override public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
         return NativeFluidloggedBlock.super.pickupBlock(player, level, pos, state);
      }
      @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound(BlockState state) {
         return NativeFluidloggedBlock.super.getPickupSound(state);
      }
      @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState placed=super.getStateForPlacement(context);
         if(placed==null)return null;
         return NativeFluidloggedBlock.placement(placed,context);
      }
      @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         NativeFluidloggedBlock.schedule(state,level,pos);
         BlockState changed=super.updateShape(state,side,neighbor,level,pos,neighborPos);
         return changed;
      }
   }

   public static class NativeOpenBlock extends Block implements NativeTransientFluidBlock {
      public static final MapCodec<Block> CODEC = simpleCodec(NativeOpenBlock::new);
      public NativeOpenBlock(Properties properties) {
         super(properties);
         registerDefaultState(NativeFluidloggedBlock.dry(defaultBlockState()));
      }
      @Override protected MapCodec<? extends Block> codec() { return CODEC; }
      @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(NativeFluidloggedBlock.FLUID_CELL, StairWaterFlow.WATER_FLOW);
         builder.add(BlockStateProperties.WATERLOGGED);
      }
      @Override public FluidState getFluidState(BlockState state) { return StairWaterFlow.fluid(state); }
      @Override public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
         return NativeTransientFluidBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
      }
      @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
         return NativeTransientFluidBlock.super.placeLiquid(level, pos, state, fluid);
      }
      @Override public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
         return NativeTransientFluidBlock.super.pickupBlock(player, level, pos, state);
      }
      @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound(BlockState state) {
         return NativeTransientFluidBlock.super.getPickupSound(state);
      }
      @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState placed=super.getStateForPlacement(context);
         if(placed==null)return null;
         return NativeFluidloggedBlock.placement(placed,context);
      }
      @Override protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         NativeFluidloggedBlock.schedule(state,level,pos);
         BlockState changed=super.updateShape(state,side,neighbor,level,pos,neighborPos);
         return changed;
      }
   }

   public static class NativeStairBlock extends StairBlock implements NativeTransientFluidBlock {
      public static final MapCodec<NativeStairBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
         BlockState.CODEC.fieldOf("base_state").forGetter(block -> block.nativeBase), propertiesCodec()
      ).apply(instance, NativeStairBlock::new));
      private final BlockState nativeBase;
      public NativeStairBlock(BlockState base, Properties properties) {
         super(base, properties);
         this.nativeBase = base;
         registerDefaultState(NativeFluidloggedBlock.dry(defaultBlockState()));
      }
      @Override public MapCodec<? extends StairBlock> codec() { return CODEC; }
      @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(NativeFluidloggedBlock.FLUID_CELL, StairWaterFlow.WATER_FLOW);
      }
      @Override public FluidState getFluidState(BlockState state) { return StairWaterFlow.fluid(state); }
      @Override public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
         return NativeTransientFluidBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
      }
      @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
         return NativeTransientFluidBlock.super.placeLiquid(level, pos, state, fluid);
      }
      @Override public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
         return NativeTransientFluidBlock.super.pickupBlock(player, level, pos, state);
      }
      @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound(BlockState state) {
         return NativeTransientFluidBlock.super.getPickupSound(state);
      }
      @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState placed=super.getStateForPlacement(context);
         if(placed==null)return null;
         return NativeFluidloggedBlock.placement(placed,context);
      }
      @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         NativeFluidloggedBlock.schedule(state,level,pos);
         BlockState changed=super.updateShape(state,side,neighbor,level,pos,neighborPos);
         return changed;
      }
   }

   public static class NativeWallBlock extends WallBlock implements NativeTransientFluidBlock {
      public static final MapCodec<WallBlock> CODEC = simpleCodec(NativeWallBlock::new);
      public NativeWallBlock(Properties properties) {
         super(properties);
         registerDefaultState(NativeFluidloggedBlock.dry(defaultBlockState()));
      }
      @Override public MapCodec<WallBlock> codec() { return CODEC; }
      @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(NativeFluidloggedBlock.FLUID_CELL, StairWaterFlow.WATER_FLOW);
      }
      // WallBlock keys its private shape maps by complete states built with default extra properties.
      // Project only the cell to that canonical key; topology and vanilla shape caches stay intact.
      @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
         return super.getShape(NativeFluidloggedBlock.dry(state), level, pos, context);
      }
      @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
         return super.getCollisionShape(NativeFluidloggedBlock.dry(state), level, pos, context);
      }
      @Override public FluidState getFluidState(BlockState state) { return StairWaterFlow.fluid(state); }
      @Override public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
         return NativeTransientFluidBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
      }
      @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
         return NativeTransientFluidBlock.super.placeLiquid(level, pos, state, fluid);
      }
      @Override public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
         return NativeTransientFluidBlock.super.pickupBlock(player, level, pos, state);
      }
      @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound(BlockState state) {
         return NativeTransientFluidBlock.super.getPickupSound(state);
      }
      @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState placed=super.getStateForPlacement(context);
         if(placed==null)return null;
         return NativeFluidloggedBlock.placement(placed,context);
      }
      @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         NativeFluidloggedBlock.schedule(state,level,pos);
         BlockState changed=super.updateShape(state,side,neighbor,level,pos,neighborPos);
         return changed;
      }
   }

   public static class NativeLadderBlock extends LadderBlock implements NativeTransientFluidBlock {
      public static final MapCodec<LadderBlock> CODEC = simpleCodec(NativeLadderBlock::new);
      public NativeLadderBlock(Properties properties) {
         super(properties);
         registerDefaultState(NativeFluidloggedBlock.dry(defaultBlockState()));
      }
      @Override public MapCodec<LadderBlock> codec() { return CODEC; }
      @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(NativeFluidloggedBlock.FLUID_CELL, StairWaterFlow.WATER_FLOW);
      }
      @Override public FluidState getFluidState(BlockState state) { return StairWaterFlow.fluid(state); }
      @Override public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
         return NativeTransientFluidBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
      }
      @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
         return NativeTransientFluidBlock.super.placeLiquid(level, pos, state, fluid);
      }
      @Override public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
         return NativeTransientFluidBlock.super.pickupBlock(player, level, pos, state);
      }
      @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound(BlockState state) {
         return NativeTransientFluidBlock.super.getPickupSound(state);
      }
      @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState placed=super.getStateForPlacement(context);
         if(placed==null)return null;
         return NativeFluidloggedBlock.placement(placed,context);
      }
      @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         NativeFluidloggedBlock.schedule(state,level,pos);
         BlockState changed=super.updateShape(state,side,neighbor,level,pos,neighborPos);
         return changed;
      }
   }

   public static class NativeSlabBlock extends SlabBlock implements NativeTransientFluidBlock {
      public static final MapCodec<SlabBlock> CODEC = simpleCodec(NativeSlabBlock::new);
      public NativeSlabBlock(Properties properties) {
         super(properties);
         registerDefaultState(NativeFluidloggedBlock.dry(defaultBlockState()));
      }
      @Override public MapCodec<? extends SlabBlock> codec() { return CODEC; }
      @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(NativeFluidloggedBlock.FLUID_CELL, StairWaterFlow.WATER_FLOW);
      }
      @Override public FluidState getFluidState(BlockState state) { return StairWaterFlow.fluid(state); }
      @Override public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
         return NativeTransientFluidBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
      }
      @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
         return NativeTransientFluidBlock.super.placeLiquid(level, pos, state, fluid);
      }
      @Override public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
         return NativeTransientFluidBlock.super.pickupBlock(player, level, pos, state);
      }
      @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound(BlockState state) {
         return NativeTransientFluidBlock.super.getPickupSound(state);
      }
      @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState placed=super.getStateForPlacement(context);
         if(placed==null)return null;
         BlockState existing=context.getLevel().getBlockState(context.getClickedPos());
         // Vanilla clears water on double-slab merge; full-block storage keeps the paid cell.
         if(existing.is(this))return NativeFluidloggedBlock.copyCell(placed,existing);
         return NativeFluidloggedBlock.placement(placed,context);
      }
      @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         NativeFluidloggedBlock.schedule(state,level,pos);
         BlockState changed=super.updateShape(state,side,neighbor,level,pos,neighborPos);
         return changed;
      }
   }

   public static class NativeDoorBlock extends DoorBlock implements NativeTransientFluidBlock {
      public static final MapCodec<NativeDoorBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
         BlockSetType.CODEC.fieldOf("block_set_type").forGetter(NativeDoorBlock::type), propertiesCodec()
      ).apply(instance, NativeDoorBlock::new));
      public NativeDoorBlock(BlockSetType type, Properties properties) {
         super(type, properties);
         registerDefaultState(NativeFluidloggedBlock.dry(defaultBlockState()));
      }
      @Override public MapCodec<? extends DoorBlock> codec() { return CODEC; }
      @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(NativeFluidloggedBlock.FLUID_CELL, StairWaterFlow.WATER_FLOW);
         builder.add(BlockStateProperties.WATERLOGGED);
      }
      @Override public FluidState getFluidState(BlockState state) { return StairWaterFlow.fluid(state); }
      @Override public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
         return NativeTransientFluidBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
      }
      @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
         return NativeTransientFluidBlock.super.placeLiquid(level, pos, state, fluid);
      }
      @Override public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
         return NativeTransientFluidBlock.super.pickupBlock(player, level, pos, state);
      }
      @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound(BlockState state) {
         return NativeTransientFluidBlock.super.getPickupSound(state);
      }
      @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState placed=super.getStateForPlacement(context);
         if(placed==null)return null;
         return NativeFluidloggedBlock.placement(placed,context);
      }
      @Override protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         NativeFluidloggedBlock.schedule(state,level,pos);
         BlockState changed=super.updateShape(state,side,neighbor,level,pos,neighborPos);
         if(changed.getBlock() instanceof DoorBlock) {
            if(!changed.is(this))return Blocks.AIR.defaultBlockState();
            return NativeFluidloggedBlock.copyCell(changed,state);
         }
         return changed;
      }
      @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
         level.setBlock(pos.above(),NativeFluidloggedBlock.placement(state.setValue(HALF,DoubleBlockHalf.UPPER),level.getFluidState(pos.above())),3);
      }
      @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
         if(!level.isClientSide && (player.isCreative() || !player.hasCorrectToolForDrops(state,level,pos)) && state.getValue(HALF)==DoubleBlockHalf.UPPER) {
            BlockPos lower=pos.below();BlockState bottom=level.getBlockState(lower);
            if(bottom.is(this) && bottom.getValue(HALF)==DoubleBlockHalf.LOWER) {
               // Vanilla's plant helper restores only water and promotes current to a source.
               level.setBlock(lower,level.getFluidState(lower).createLegacyBlock(),35);
               level.levelEvent(player,2001,lower,Block.getId(bottom));
            }
         }
         return super.playerWillDestroy(level,pos,state,player);
      }
   }
}
