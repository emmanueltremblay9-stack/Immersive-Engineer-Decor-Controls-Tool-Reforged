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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** Restores Engineer's Decor's adjustable cosmetic chimney (Stefan Wilhelm / wile, MIT). */
public final class ChimneyBlock extends Block {
   public static final MapCodec<ChimneyBlock> CODEC = simpleCodec(ChimneyBlock::new);
   public static final IntegerProperty POWER = BlockStateProperties.POWER;
   public static final BooleanProperty STACKED = BooleanProperty.create("stacked");
   public static final IntegerProperty INPUT_POWER = IntegerProperty.create("input_power", 0, 15);
   private static final VoxelShape SHORT_SHAPE = chimneyShape(6);
   private static final VoxelShape EXTENDED_SHAPE = chimneyShape(16);

   private static VoxelShape chimneyShape(double height) {
      return Shapes.or(box(3, 0, 3, 13, height, 5), box(3, 0, 11, 13, height, 13),
         box(3, 0, 5, 5, height, 11), box(11, 0, 5, 13, height, 11), box(5, 0, 5, 11, 1, 11));
   }

   public ChimneyBlock(Properties properties) {
      super(properties);
      // Old saves have no power property: retain their silent state until clicked or powered.
      // Also avoids smoke in item/model previews, as in the original implementation.
      registerDefaultState(stateDefinition.any().setValue(POWER, 0).setValue(STACKED, false).setValue(INPUT_POWER, 0));
   }

   @Override
   protected MapCodec<? extends Block> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(POWER, STACKED, INPUT_POWER);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      int signal = context.getLevel().getBestNeighborSignal(context.getClickedPos());
      return defaultBlockState().setValue(POWER, signal == 0 ? 5 : signal).setValue(INPUT_POWER, signal)
         .setValue(STACKED, hasChimneyAbove(context.getLevel(), context.getClickedPos()));
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(STACKED) ? EXTENDED_SHAPE : SHORT_SHAPE;
   }

   @Override
   public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level,
                                 BlockPos pos, BlockPos neighborPos) {
      return side == Direction.UP ? state.setValue(STACKED, neighbor.getBlock() instanceof ChimneyBlock) : state;
   }

   private static boolean hasChimneyAbove(BlockGetter level, BlockPos pos) {
      return level.getBlockState(pos.above()).getBlock() instanceof ChimneyBlock;
   }

   /** Uses the actual neighbor too, so legacy states cannot leak smoke through another chimney head. */
   public static boolean shouldEmitSmoke(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getBlock() instanceof ChimneyBlock && state.getValue(POWER) > 0 && !hasChimneyAbove(level, pos);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      cycleIntensity(state, level, pos);
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                            Player player, InteractionHand hand, BlockHitResult hit) {
      // Holding another head means build the stack; do not consume the click as a smoke adjustment.
      if (stack.is(asItem())) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
      cycleIntensity(state, level, pos);
      return ItemInteractionResult.sidedSuccess(level.isClientSide);
   }

   private static void cycleIntensity(BlockState state, Level level, BlockPos pos) {
      if (!level.isClientSide) {
         level.setBlock(pos, state.setValue(POWER, (state.getValue(POWER) + 1) & 15)
            .setValue(STACKED, hasChimneyAbove(level, pos)).setValue(INPUT_POWER, level.getBestNeighborSignal(pos)), UPDATE_ALL);
      }
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      if (level.isClientSide) return;
      int signal = level.getBestNeighborSignal(pos);
      // Structural notifications must not erase a manual setting. Only a changed
      // incoming signal takes control again, including a change back to zero.
      BlockState updated = state.setValue(STACKED, hasChimneyAbove(level, pos));
      if (signal != state.getValue(INPUT_POWER)) {
         updated = updated.setValue(POWER, signal).setValue(INPUT_POWER, signal);
      }
      if (updated != state) {
         level.setBlock(pos, updated, UPDATE_CLIENTS);
      }
   }

   @Override
   public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (level.isClientSide && state.is(this) && shouldEmitSmoke(state, level, pos)) {
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
