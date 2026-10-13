package com.oblixorprime.engineersdecorreforged.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Restores Engineer's Decor's adjustable cosmetic chimney (Stefan Wilhelm / wile, MIT). */
public final class ChimneyBlock extends Block {
   public static final MapCodec<ChimneyBlock> CODEC = simpleCodec(ChimneyBlock::new);
   public static final IntegerProperty POWER = BlockStateProperties.POWER;
   private static final VoxelShape SHAPE = box(3, 0, 3, 13, 6, 13);

   public ChimneyBlock(Properties properties) {
      super(properties);
      // Old saves have no power property: retain their silent state until clicked or powered.
      // Also avoids smoke in item/model previews, as in the original implementation.
      registerDefaultState(stateDefinition.any().setValue(POWER, 0));
   }

   @Override
   protected MapCodec<? extends Block> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(POWER);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      int signal = context.getLevel().getBestNeighborSignal(context.getClickedPos());
      return defaultBlockState().setValue(POWER, signal == 0 ? 5 : signal);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      cycleIntensity(state, level, pos);
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                            Player player, InteractionHand hand, BlockHitResult hit) {
      cycleIntensity(state, level, pos);
      return ItemInteractionResult.sidedSuccess(level.isClientSide);
   }

   private static void cycleIntensity(BlockState state, Level level, BlockPos pos) {
      if (!level.isClientSide) {
         level.setBlock(pos, state.setValue(POWER, (state.getValue(POWER) + 1) & 15), UPDATE_ALL);
      }
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      if (level.isClientSide) return;
      int signal = level.getBestNeighborSignal(pos);
      if (signal != state.getValue(POWER)) {
         level.setBlock(pos, state.setValue(POWER, signal), UPDATE_CLIENTS);
      }
   }

   @Override
   public boolean shouldCheckWeakPower(BlockState state, LevelReader level, BlockPos pos, Direction side) {
      return false;
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (level.isClientSide && state.is(this)) {
         emitSmoke(state.getValue(POWER), pos, random, level::addParticle);
      }
   }

   /** Shared emission algorithm so dedicated tests can verify particles without loading client classes. */
   public static void emitSmoke(int intensity, BlockPos pos, RandomSource random, SmokeSink sink) {
      if (intensity <= 0) return;
      int count = 1 + random.nextInt(10) * intensity / 15;
      for (int i = 0; i < count; i++) {
         double kind = random.nextDouble() * intensity / 5;
         sink.add(kind > 0.7 ? ParticleTypes.LARGE_SMOKE : kind > 0.4 ? ParticleTypes.SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE,
            pos.getX() + 0.5 + random.nextDouble() * 0.2,
            pos.getY() + 0.9 + random.nextDouble() * 0.1,
            pos.getZ() + 0.5 + random.nextDouble() * 0.2,
            -0.02 + random.nextDouble() * 0.04,
            0.05 + random.nextDouble() * 0.1,
            -0.02 + random.nextDouble() * 0.04);
      }
   }

   @FunctionalInterface
   public interface SmokeSink {
      void add(ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz);
   }
}
