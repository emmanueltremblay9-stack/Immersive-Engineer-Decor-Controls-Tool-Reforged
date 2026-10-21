package com.oblixorprime.engineersdecorreforged.utility;

import com.mojang.serialization.MapCodec;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.NativeTransientFluidBlock;
import com.oblixorprime.engineersdecorreforged.block.StairWaterFlow;
import com.oblixorprime.engineersdecorreforged.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MachineBlocks {
   public static final DirectionProperty FACING = BlockStateProperties.FACING;
   public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
   public static final BooleanProperty LIT = BlockStateProperties.LIT;
   public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
   public static final BooleanProperty FILLED = BooleanProperty.create("filled");
   public static final IntegerProperty PHASE_0_3 = IntegerProperty.create("phase", 0, 3);
   public static final IntegerProperty PHASE_0_4 = IntegerProperty.create("phase", 0, 4);
   public static final IntegerProperty LEVEL_0_3 = IntegerProperty.create("level", 0, 3);
   public static final IntegerProperty LEVEL_0_4 = IntegerProperty.create("level", 0, 4);
   public static final IntegerProperty EXPOSITION = IntegerProperty.create("exposition", 0, 4);
   public static final BooleanProperty RS_N = BooleanProperty.create("rs_n");
   public static final BooleanProperty RS_E = BooleanProperty.create("rs_e");
   public static final BooleanProperty RS_S = BooleanProperty.create("rs_s");
   public static final BooleanProperty RS_W = BooleanProperty.create("rs_w");
   public static final BooleanProperty RS_U = BooleanProperty.create("rs_u");
   public static final BooleanProperty RS_D = BooleanProperty.create("rs_d");
   private static final VoxelShape PASSIVE_FLUID_ACCUMULATOR_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);
   private static final VoxelShape SMALL_BLOCK_BREAKER_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);
   private static final double LABEL_MIN_U = 0.15625;
   private static final double LABEL_MAX_U = 0.84375;
   private static final double LABEL_MIN_V = 0.25;
   private static final double LABEL_MAX_V = 0.71875;

   private MachineBlocks() {
   }

   private static VoxelShape horizontalBox(Direction facing, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      return switch (facing) {
         case SOUTH -> Block.box(16.0 - maxX, minY, 16.0 - maxZ, 16.0 - minX, maxY, 16.0 - minZ);
         case EAST -> Block.box(16.0 - maxZ, minY, minX, 16.0 - minZ, maxY, maxX);
         case WEST -> Block.box(minZ, minY, 16.0 - maxX, maxZ, maxY, 16.0 - minX);
         default -> Block.box(minX, minY, minZ, maxX, maxY, maxZ);
      };
   }

   private static VoxelShape pipeValveShape(Direction facing) {
      return switch (facing.getAxis()) {
         case X -> Block.box(0.0, 2.0, 2.0, 16.0, 14.0, 14.0);
         case Y -> Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
         case Z -> Block.box(2.0, 2.0, 0.0, 14.0, 14.0, 16.0);
         default -> throw new MatchException(null, null);
      };
   }

   public static class ActiveHorizontalMachineBlock extends MachineBlocks.HorizontalMachineBlock {
      public ActiveHorizontalMachineBlock(Properties properties, MachineKind kind) {
         super(properties, kind);
         this.registerDefaultState((BlockState)this.defaultBlockState().setValue(MachineBlocks.ACTIVE, false));
      }

      @Override
      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{MachineBlocks.ACTIVE});
      }

      @Override
      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return switch (this.kind()) {
            case SMALL_BLOCK_BREAKER -> MachineBlocks.SMALL_BLOCK_BREAKER_SHAPE;
            case SMALL_MILKING_MACHINE -> MachineBlocks.horizontalBox(
               (Direction)state.getValue(MachineBlocks.HORIZONTAL_FACING), 0.0, 0.0, 0.0, 16.0, 16.0, 23.0
            );
            case SMALL_TREE_CUTTER -> MachineBlocks.horizontalBox((Direction)state.getValue(MachineBlocks.HORIZONTAL_FACING), 0.0, 0.0, -4.0, 16.0, 8.0, 16.0);
            default -> super.getShape(state, level, pos, context);
         };
      }

      @Override
      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.getShape(state, level, pos, context);
      }
   }

   public static class DirectionalMachineBlock extends MachineBlocks.MachineBlock {
      public DirectionalMachineBlock(Properties properties, MachineKind kind) {
         super(properties, kind);
         this.registerDefaultState((BlockState)((BlockState)NativeFluidloggedBlock.dry(this.stateDefinition.any())).setValue(MachineBlocks.FACING, Direction.NORTH));
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{MachineBlocks.FACING});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         Direction facing = context.getClickedFace();
         if (this.kind() == MachineKind.FACTORY_HOPPER) {
            facing = facing.getOpposite();
            if (facing == Direction.UP) {
               facing = Direction.DOWN;
            }
         }

         return NativeFluidloggedBlock.placement((BlockState)this.defaultBlockState().setValue(MachineBlocks.FACING, facing), context);
      }

      protected BlockState rotate(BlockState state, Rotation rotation) {
         return (BlockState)state.setValue(MachineBlocks.FACING, rotation.rotate((Direction)state.getValue(MachineBlocks.FACING)));
      }

      protected BlockState mirror(BlockState state, Mirror mirror) {
         return state.rotate(mirror.getRotation((Direction)state.getValue(MachineBlocks.FACING)));
      }
   }

   public static class HorizontalMachineBlock extends MachineBlocks.MachineBlock {
      public HorizontalMachineBlock(Properties properties, MachineKind kind) {
         super(properties, kind);
         this.registerDefaultState((BlockState)((BlockState)NativeFluidloggedBlock.dry(this.stateDefinition.any())).setValue(MachineBlocks.HORIZONTAL_FACING, Direction.NORTH));
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{MachineBlocks.HORIZONTAL_FACING});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         return NativeFluidloggedBlock.placement((BlockState)this.defaultBlockState().setValue(MachineBlocks.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite()), context);
      }

      protected BlockState rotate(BlockState state, Rotation rotation) {
         return (BlockState)state.setValue(MachineBlocks.HORIZONTAL_FACING, rotation.rotate((Direction)state.getValue(MachineBlocks.HORIZONTAL_FACING)));
      }

      protected BlockState mirror(BlockState state, Mirror mirror) {
         return state.rotate(mirror.getRotation((Direction)state.getValue(MachineBlocks.HORIZONTAL_FACING)));
      }
   }

   public static class LabeledCrateBlock extends MachineBlocks.HorizontalMachineBlock {
      public static final MapCodec<MachineBlocks.LabeledCrateBlock> CODEC = simpleCodec(MachineBlocks.LabeledCrateBlock::new);

      public LabeledCrateBlock(Properties properties) {
         super(properties, MachineKind.LABELED_CRATE);
      }

      @Override
      protected MapCodec<? extends BaseEntityBlock> codec() {
         return CODEC;
      }

      @Override
      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         if (isLabelHit(state, hit)) {
            if (level.isClientSide && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
               LabeledCrateLabelClientBridge.open(pos, machine.labelLines());
            }

            return InteractionResult.SUCCESS;
         } else {
            return super.useWithoutItem(state, level, pos, player, hit);
         }
      }

      @Override
      protected ItemInteractionResult useItemOn(
         ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
      ) {
         if (isLabelHit(state, hit)) {
            if (level.isClientSide && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
               LabeledCrateLabelClientBridge.open(pos, machine.labelLines());
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
         } else {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
         }
      }

      @Override
      public BlockState getStateForPlacement(BlockPlaceContext context) {
         Direction labelDirection = context.isSecondaryUseActive() ? context.getHorizontalDirection() : context.getHorizontalDirection().getOpposite();
         return NativeFluidloggedBlock.placement((BlockState)this.defaultBlockState().setValue(MachineBlocks.HORIZONTAL_FACING, labelDirection), context);
      }

      public static boolean isLabelHit(BlockState state, BlockHitResult hit) {
         if (!state.hasProperty(MachineBlocks.HORIZONTAL_FACING)) {
            return false;
         }

         Direction facing = (Direction)state.getValue(MachineBlocks.HORIZONTAL_FACING);
         if (hit.getDirection() != facing) {
            return false;
         }

         BlockPos pos = hit.getBlockPos();
         Vec3 hitLocation = hit.getLocation();
         double localX = hitLocation.x - pos.getX();
         double localY = hitLocation.y - pos.getY();
         double localZ = hitLocation.z - pos.getZ();
         double u = facing.getAxis() == Axis.Z ? localX : localZ;
         return u >= 0.15625 && u <= 0.84375 && localY >= 0.25 && localY <= 0.71875;
      }
   }

   public static class LevelDirectionalMachineBlock extends MachineBlocks.DirectionalMachineBlock {
      public LevelDirectionalMachineBlock(Properties properties, MachineKind kind) {
         super(properties, kind);
         this.registerDefaultState((BlockState)this.defaultBlockState().setValue(MachineBlocks.LEVEL_0_4, 0));
      }

      @Override
      public BlockState getStateForPlacement(BlockPlaceContext context) {
         Direction direction = context.isSecondaryUseActive() ? context.getClickedFace() : Direction.UP;
         return NativeFluidloggedBlock.placement((BlockState)this.defaultBlockState().setValue(MachineBlocks.FACING, direction), context);
      }

      @Override
      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{MachineBlocks.LEVEL_0_4});
      }
   }

   public static class LevelOnlyMachineBlock extends MachineBlocks.MachineBlock {
      public LevelOnlyMachineBlock(Properties properties, MachineKind kind) {
         super(properties, kind);
         this.registerDefaultState((BlockState)((BlockState)NativeFluidloggedBlock.dry(this.stateDefinition.any())).setValue(MachineBlocks.LEVEL_0_3, 0));
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{MachineBlocks.LEVEL_0_3});
      }
   }

   public static class LitHorizontalMachineBlock extends MachineBlocks.HorizontalMachineBlock {
      public LitHorizontalMachineBlock(Properties properties, MachineKind kind) {
         super(properties, kind);
         this.registerDefaultState((BlockState)this.defaultBlockState().setValue(MachineBlocks.LIT, false));
      }

      @Override
      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{MachineBlocks.LIT});
      }
   }

   public static class LitMachineBlock extends MachineBlocks.MachineBlock {
      public LitMachineBlock(Properties properties, MachineKind kind) {
         super(properties, kind);
         this.registerDefaultState((BlockState)((BlockState)NativeFluidloggedBlock.dry(this.stateDefinition.any())).setValue(MachineBlocks.LIT, false));
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{MachineBlocks.LIT});
      }
   }

   public static class MachineBlock extends BaseEntityBlock implements NativeTransientFluidBlock {
      public static final MapCodec<MachineBlocks.MachineBlock> CODEC = simpleCodec(
         properties -> new MachineBlocks.MachineBlock(properties, MachineKind.LABELED_CRATE)
      );
      private final MachineKind kind;

      public MachineBlock(Properties properties, MachineKind kind) {
         super(properties);
         this.kind = kind;
         registerDefaultState(NativeFluidloggedBlock.dry(defaultBlockState()));
      }

      public MachineKind kind() {
         return this.kind;
      }

      protected MapCodec<? extends BaseEntityBlock> codec() {
         return CODEC;
      }

      public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
         return new MachineBlockEntity(pos, state);
      }

      public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
         return level.isClientSide ? null : createTickerHelper(type, (BlockEntityType)ModBlockEntities.MACHINE.get(), MachineBlockEntity::serverTick);
      }

      protected RenderShape getRenderShape(BlockState state) {
         return RenderShape.MODEL;
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.kind == MachineKind.PASSIVE_FLUID_ACCUMULATOR ? MachineBlocks.PASSIVE_FLUID_ACCUMULATOR_SHAPE : super.getShape(state, level, pos, context);
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.getShape(state, level, pos, context);
      }

      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            player.openMenu(machine);
         }

         return InteractionResult.SUCCESS;
      }

      protected ItemInteractionResult useItemOn(
         ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
      ) {
         if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            if (level.isClientSide && machine.canPreviewItemUse(stack)) {
               return ItemInteractionResult.sidedSuccess(true);
            }

            if (machine.handleItemUse(stack, player, hand)) {
               return ItemInteractionResult.sidedSuccess(false);
            }
         }

         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }

      protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
         if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            machine.clearMetalCraftingResultForDrop();
            Containers.dropContents(level, pos, machine);
            level.updateNeighbourForOutputSignal(pos, this);
         }

         super.onRemove(state, level, pos, newState, movedByPiston);
      }

      protected boolean hasAnalogOutputSignal(BlockState state) {
         return true;
      }

      protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
         return level.getBlockEntity(pos) instanceof MachineBlockEntity machine ? machine.comparatorOutput() : 0;
      }
      @Override protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(BlockStateProperties.WATERLOGGED, NativeFluidloggedBlock.FLUID_CELL, StairWaterFlow.WATER_FLOW);
      }
      @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
         return NativeFluidloggedBlock.placement(defaultBlockState(), context);
      }
      @Override protected FluidState getFluidState(BlockState state) { return StairWaterFlow.fluid(state); }
      @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         NativeFluidloggedBlock.schedule(state, level, pos);
         return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
      }
      @Override public boolean canPlaceLiquid(Player player, BlockGetter level, BlockPos pos, BlockState state, net.minecraft.world.level.material.Fluid fluid) {
         return (player == null || player.isShiftKeyDown()) && NativeTransientFluidBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
      }
      @Override public ItemStack pickupBlock(Player player, LevelAccessor level, BlockPos pos, BlockState state) {
         return player != null && !player.isShiftKeyDown() ? ItemStack.EMPTY : NativeTransientFluidBlock.super.pickupBlock(player, level, pos, state);
      }
   }

   /** Water in the block's world cell is independent of the existing machine tank. */
   public static class PassiveFluidAccumulatorBlock extends MachineBlocks.MachineBlock {
      public static final MapCodec<MachineBlocks.PassiveFluidAccumulatorBlock> CODEC = simpleCodec(MachineBlocks.PassiveFluidAccumulatorBlock::new);
      public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

      public PassiveFluidAccumulatorBlock(Properties properties) {
         super(properties, MachineKind.PASSIVE_FLUID_ACCUMULATOR);
         this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
      }

      @Override
      protected MapCodec<? extends BaseEntityBlock> codec() {
         return CODEC;
      }








   }

   public static class MetalCraftingTableBlock extends MachineBlocks.MachineBlock {
      public static final MapCodec<MachineBlocks.MetalCraftingTableBlock> CODEC = simpleCodec(MachineBlocks.MetalCraftingTableBlock::new);

      public MetalCraftingTableBlock(Properties properties) {
         super(properties, MachineKind.METAL_CRAFTING_TABLE);
      }

      @Override
      protected MapCodec<? extends BaseEntityBlock> codec() {
         return CODEC;
      }
   }

   public static class MilkingMachineBlock extends MachineBlocks.ActiveHorizontalMachineBlock {
      public MilkingMachineBlock(Properties properties) {
         super(properties, MachineKind.SMALL_MILKING_MACHINE);
         this.registerDefaultState((BlockState)this.defaultBlockState().setValue(MachineBlocks.FILLED, false));
      }

      @Override
      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{MachineBlocks.FILLED});
      }
   }

   public static class OpenDirectionalMachineBlock extends MachineBlocks.DirectionalMachineBlock {
      public OpenDirectionalMachineBlock(Properties properties, MachineKind kind) {
         super(properties, kind);
         this.registerDefaultState((BlockState)this.defaultBlockState().setValue(MachineBlocks.OPEN, false));
      }

      @Override
      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{MachineBlocks.OPEN});
      }
   }

   public static class Phase3HorizontalMachineBlock extends MachineBlocks.HorizontalMachineBlock {
      public Phase3HorizontalMachineBlock(Properties properties, MachineKind kind) {
         super(properties, kind);
         this.registerDefaultState((BlockState)this.defaultBlockState().setValue(MachineBlocks.PHASE_0_3, 0));
      }

      @Override
      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{MachineBlocks.PHASE_0_3});
      }
   }

   public static class Phase4HorizontalMachineBlock extends MachineBlocks.HorizontalMachineBlock {
      public Phase4HorizontalMachineBlock(Properties properties, MachineKind kind) {
         super(properties, kind);
         this.registerDefaultState((BlockState)this.defaultBlockState().setValue(MachineBlocks.PHASE_0_4, 0));
      }

      @Override
      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{MachineBlocks.PHASE_0_4});
      }
   }

   public static class PipeValveBlock extends DirectionalBlock implements NativeTransientFluidBlock {
      public static final MapCodec<MachineBlocks.PipeValveBlock> CODEC = simpleCodec(MachineBlocks.PipeValveBlock::plain);
      private final boolean redstoneAware;

      public static MachineBlocks.PipeValveBlock plain(Properties properties) {
         return new MachineBlocks.PipeValveBlock(properties, false);
      }

      public PipeValveBlock(Properties properties, boolean redstoneAware) {
         super(properties);
         this.redstoneAware = redstoneAware;
         this.registerDefaultState(
            (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)NativeFluidloggedBlock.dry(this.stateDefinition.any()))
                                 .setValue(FACING, Direction.NORTH))
                              .setValue(MachineBlocks.RS_N, false))
                           .setValue(MachineBlocks.RS_E, false))
                        .setValue(MachineBlocks.RS_S, false))
                     .setValue(MachineBlocks.RS_W, false))
                  .setValue(MachineBlocks.RS_U, false))
               .setValue(MachineBlocks.RS_D, false)
         );
      }

      protected MapCodec<? extends DirectionalBlock> codec() {
         return CODEC;
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(BlockStateProperties.WATERLOGGED, NativeFluidloggedBlock.FLUID_CELL, StairWaterFlow.WATER_FLOW);
         builder.add(
            new Property[]{FACING, MachineBlocks.RS_N, MachineBlocks.RS_E, MachineBlocks.RS_S, MachineBlocks.RS_W, MachineBlocks.RS_U, MachineBlocks.RS_D}
         );
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         Direction direction = context.isSecondaryUseActive() ? context.getClickedFace().getOpposite() : context.getClickedFace();
         return NativeFluidloggedBlock.placement(this.withRedstoneConnectors(context.getLevel(), context.getClickedPos(), (BlockState)this.defaultBlockState().setValue(FACING, direction)), context);
      }

      protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
         if (!level.isClientSide && this.redstoneAware) {
            BlockState updated = this.withRedstoneConnectors(level, pos, state);
            if (updated != state) {
               level.setBlock(pos, updated, 3);
            }
         }
      }

      private BlockState withRedstoneConnectors(Level level, BlockPos pos, BlockState state) {
         return !this.redstoneAware
            ? state
            : (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)state.setValue(
                              MachineBlocks.RS_N, level.getSignal(pos.north(), Direction.SOUTH) > 0
                           ))
                           .setValue(MachineBlocks.RS_E, level.getSignal(pos.east(), Direction.WEST) > 0))
                        .setValue(MachineBlocks.RS_S, level.getSignal(pos.south(), Direction.NORTH) > 0))
                     .setValue(MachineBlocks.RS_W, level.getSignal(pos.west(), Direction.EAST) > 0))
                  .setValue(MachineBlocks.RS_U, level.getSignal(pos.above(), Direction.DOWN) > 0))
               .setValue(MachineBlocks.RS_D, level.getSignal(pos.below(), Direction.UP) > 0);
      }

      protected boolean hasAnalogOutputSignal(BlockState state) {
         return this.redstoneAware;
      }

      protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
         return this.redstoneAware ? level.getBestNeighborSignal(pos) : 0;
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return MachineBlocks.pipeValveShape((Direction)state.getValue(FACING));
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.getShape(state, level, pos, context);
      }

      protected BlockState rotate(BlockState state, Rotation rotation) {
         return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
      }

      protected BlockState mirror(BlockState state, Mirror mirror) {
         return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
      }
      @Override protected FluidState getFluidState(BlockState state) { return StairWaterFlow.fluid(state); }
      @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         NativeFluidloggedBlock.schedule(state, level, pos);
         return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
      }
   }

   public static class SolarPanelBlock extends MachineBlocks.MachineBlock {
      public SolarPanelBlock(Properties properties) {
         super(properties, MachineKind.SMALL_SOLAR_PANEL);
         this.registerDefaultState((BlockState)((BlockState)NativeFluidloggedBlock.dry(this.stateDefinition.any())).setValue(MachineBlocks.EXPOSITION, 0));
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{MachineBlocks.EXPOSITION});
      }
   }
}
