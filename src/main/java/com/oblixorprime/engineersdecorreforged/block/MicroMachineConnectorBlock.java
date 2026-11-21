package com.oblixorprime.engineersdecorreforged.block;

import com.mojang.serialization.MapCodec;
import com.oblixorprime.engineersdecorreforged.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** One C03 connector connects one selected lane, never all nine. */
public final class MicroMachineConnectorBlock extends BaseEntityBlock {
   public static final MapCodec<MicroMachineConnectorBlock> CODEC = simpleCodec(MicroMachineConnectorBlock::new);
   public static final DirectionProperty FACING = BlockStateProperties.FACING;
   public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 0, 8);

   public MicroMachineConnectorBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(FACING, Direction.EAST).setValue(CHANNEL, 4));
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
      builder.add(FACING, CHANNEL);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      // FACING points from the terminal toward the adjacent machine or IE energy connector.
      return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new MicroConnectorBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.MICRO_CONNECTOR.get(),
         MicroConnectorBlockEntity::serverTick);
   }

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide) {
         if (player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof MicroConnectorBlockEntity entity) {
               entity.toggleSource();
               player.displayClientMessage(Component.literal("LV connector: " + (entity.isSource() ? "SOURCE" : "SINK")), true);
            }
         } else {
            int ch = (state.getValue(CHANNEL) + 1) % 9;
            level.setBlock(pos, state.setValue(CHANNEL, ch), net.minecraft.world.level.block.Block.UPDATE_ALL);
            player.displayClientMessage(Component.literal("LV channel " + (ch + 1) + " / 9"), true);
         }
      }
      return InteractionResult.SUCCESS;
   }
}
