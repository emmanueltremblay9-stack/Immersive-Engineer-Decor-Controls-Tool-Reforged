package com.oblixorprime.engineersdecorreforged.rsgauges;

import com.mojang.serialization.MapCodec;
import com.oblixorprime.engineersdecorreforged.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ControlsBlockTypes {
   public static final DirectionProperty FACING = BlockStateProperties.FACING;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty POWER_BOOL = BooleanProperty.create("power");
   public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
   public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);
   private static final VoxelShape CONTACT_PLATE_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
   private static final VoxelShape FALLTHROUGH_FRAME_SHAPE = Block.box(0.0, 11.0, 0.0, 16.0, 14.0, 16.0);
   private static final VoxelShape TRAPDOOR_PANEL_SHAPE = Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);
   private static final VoxelShape OPEN_TRAPDOOR_NORTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
   private static final VoxelShape OPEN_TRAPDOOR_EAST_SHAPE = Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private static final VoxelShape OPEN_TRAPDOOR_SOUTH_SHAPE = Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
   private static final VoxelShape OPEN_TRAPDOOR_WEST_SHAPE = Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
   private static final VoxelShape POWER_PLANT_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);
   private static final TagKey<Item> CONFIG_WRENCHES = TagKey.create(
      Registries.ITEM, ResourceLocation.fromNamespaceAndPath("immersiveengineering", "tools/hammers")
   );

   private ControlsBlockTypes() {
   }

   private static VoxelShape attachedDeviceShape(Direction facing, double min, double max, double thickness) {
      return switch (facing) {
         case NORTH -> Block.box(min, min, 16.0 - thickness, max, max, 16.0);
         case SOUTH -> Block.box(min, min, 0.0, max, max, thickness);
         case WEST -> Block.box(16.0 - thickness, min, min, 16.0, max, max);
         case EAST -> Block.box(0.0, min, min, thickness, max, max);
         case UP -> Block.box(min, 0.0, min, max, thickness, max);
         case DOWN -> Block.box(min, 16.0 - thickness, min, max, 16.0, max);
         default -> throw new MatchException(null, null);
      };
   }

   private static VoxelShape trapdoorPanelShape(BlockState state) {
      if (!(Boolean)state.getValue(POWERED)) {
         return TRAPDOOR_PANEL_SHAPE;
      }

      return switch ((Direction)state.getValue(FACING)) {
         case EAST -> OPEN_TRAPDOOR_EAST_SHAPE;
         case SOUTH -> OPEN_TRAPDOOR_SOUTH_SHAPE;
         case WEST -> OPEN_TRAPDOOR_WEST_SHAPE;
         case NORTH, UP, DOWN -> OPEN_TRAPDOOR_NORTH_SHAPE;
      };
   }

   private static boolean hasAttachedSupport(BlockState state, LevelReader level, BlockPos pos) {
      Direction facing = (Direction)state.getValue(FACING);
      BlockPos supportPos = pos.relative(facing.getOpposite());
      return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
   }

   private static BlockState updateAttachedSupport(BlockState state, Direction direction, LevelAccessor level, BlockPos pos) {
      return direction == ((Direction)state.getValue(FACING)).getOpposite() && !hasAttachedSupport(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
   }

   public static boolean triggerSwitchLinkTarget(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      Block block = state.getBlock();
      if (block instanceof ControlsBlockTypes.SwitchLinkReceiverBlock receiver) {
         receiver.receiveSwitchLink(level, pos, state);
         return true;
      } else if (block instanceof ControlsBlockTypes.SwitchLinkPulseReceiverBlock receiver) {
         receiver.receiveSwitchLink(level, pos, state);
         return true;
      } else {
         return false;
      }
   }

   private static void giveLinkedPearl(Level level, BlockPos pos, ItemStack stack, Player player, InteractionHand hand) {
      ItemStack linkedPearl = SwitchLinkPearlItem.linkedTo(level, pos);
      if (!player.isCreative()) {
         stack.shrink(1);
      }

      if (stack.isEmpty()) {
         player.setItemInHand(hand, linkedPearl);
      } else if (!player.addItem(linkedPearl)) {
         player.drop(linkedPearl, false);
      }
   }

   public static class BooleanIndicatorBlock extends DirectionalBlock {
      public static final MapCodec<ControlsBlockTypes.BooleanIndicatorBlock> CODEC = simpleCodec(ControlsBlockTypes.BooleanIndicatorBlock::new);

      public BooleanIndicatorBlock(Properties properties) {
         super(properties);
         this.registerDefaultState(
            (BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(ControlsBlockTypes.POWER_BOOL, false)
         );
      }

      protected MapCodec<? extends DirectionalBlock> codec() {
         return CODEC;
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{FACING, ControlsBlockTypes.POWER_BOOL});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState state = (BlockState)this.defaultBlockState().setValue(FACING, context.getClickedFace());
         return (BlockState)state.setValue(ControlsBlockTypes.POWER_BOOL, this.readPower(context.getLevel(), context.getClickedPos(), state));
      }

      protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
         return ControlsBlockTypes.hasAttachedSupport(state, level, pos);
      }

      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         return ControlsBlockTypes.updateAttachedSupport(state, direction, level, pos);
      }

      protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
         if (!level.isClientSide) {
            this.updatePowered(level, pos, state);
         }
      }

      protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
         if (!level.isClientSide) {
            level.scheduleTick(pos, this, 20);
         }
      }

      protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
         this.updatePowered(level, pos, state);
         level.scheduleTick(pos, this, 20);
      }

      private void updatePowered(Level level, BlockPos pos, BlockState state) {
         boolean powered = this.readPower(level, pos, state);
         if (powered != (Boolean)state.getValue(ControlsBlockTypes.POWER_BOOL)) {
            level.setBlock(pos, (BlockState)state.setValue(ControlsBlockTypes.POWER_BOOL, powered), 3);
         }
      }

      private boolean readPower(Level level, BlockPos pos, BlockState state) {
         return ControlsBlockTypes.GaugeBlock.readAttachedSignal(level, pos, (Direction)state.getValue(FACING)) > 0;
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return ControlsBlockTypes.attachedDeviceShape((Direction)state.getValue(FACING), 3.0, 13.0, 3.0);
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return Shapes.empty();
      }
   }

   public static class ComparatorSwitchBlock extends ControlsBlockTypes.ToggleSwitchBlock {
      public static final MapCodec<ControlsBlockTypes.ComparatorSwitchBlock> CODEC = simpleCodec(ControlsBlockTypes.ComparatorSwitchBlock::new);

      public ComparatorSwitchBlock(Properties properties) {
         super(properties);
      }

      @Override
      protected MapCodec<? extends DirectionalBlock> codec() {
         return CODEC;
      }

      @Override
      public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState state = super.getStateForPlacement(context);
         return (BlockState)state.setValue(ControlsBlockTypes.POWERED, readAttachedSignal(context.getLevel(), context.getClickedPos(), state) > 0);
      }

      @Override
      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         if (!level.isClientSide) {
            this.updatePowered(level, pos, state);
         }

         return InteractionResult.SUCCESS;
      }

      protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
         if (!level.isClientSide) {
            level.scheduleTick(pos, this, 4);
         }
      }

      protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
         if (!level.isClientSide) {
            level.scheduleTick(pos, this, 2);
         }
      }

      protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
         this.updatePowered(level, pos, state);
         level.scheduleTick(pos, this, 20);
      }

      private void updatePowered(Level level, BlockPos pos, BlockState state) {
         boolean powered = readAttachedSignal(level, pos, state) > 0;
         if (powered != (Boolean)state.getValue(ControlsBlockTypes.POWERED)) {
            this.setPowered(level, pos, state, powered);
         }
      }

      private static int readAttachedSignal(Level level, BlockPos pos, BlockState state) {
         Direction facing = (Direction)state.getValue(FACING);
         BlockPos attachedPos = pos.relative(facing.getOpposite());
         BlockState attachedState = level.getBlockState(attachedPos);
         int analog = attachedState.hasAnalogOutputSignal() ? attachedState.getAnalogOutputSignal(level, attachedPos) : 0;
         int inventory = level.getBlockEntity(attachedPos) instanceof Container container ? AbstractContainerMenu.getRedstoneSignalFromContainer(container) : 0;
         int redstone = attachedState.getSignal(level, attachedPos, facing);
         int indirect = bestNeighborSignalExcept(level, attachedPos, pos);
         return Mth.clamp(Math.max(Math.max(Math.max(analog, inventory), redstone), indirect), 0, 15);
      }

      private static int bestNeighborSignalExcept(Level level, BlockPos pos, BlockPos excludedNeighbor) {
         int signal = 0;

         for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!neighbor.equals(excludedNeighbor)) {
               signal = Math.max(signal, level.getSignal(neighbor, direction.getOpposite()));
               if (signal >= 15) {
                  return 15;
               }
            }
         }

         return signal;
      }
   }

   public enum ContactShape {
      ATTACHED_BUTTON,
      CONTACT_PLATE,
      FALLTHROUGH_FRAME,
      TRAPDOOR_PANEL,
      POWER_PLANT;
   }

   public static class ContactSwitchBlock extends ControlsBlockTypes.PulseSwitchBlock {
      private final ControlsBlockTypes.ContactShape shape;

      public ContactSwitchBlock(Properties properties) {
         this(properties, ControlsBlockTypes.ContactShape.ATTACHED_BUTTON);
      }

      public ContactSwitchBlock(Properties properties, ControlsBlockTypes.ContactShape shape) {
         super(properties, 12);
         this.shape = shape;
      }

      @Override
      public BlockState getStateForPlacement(BlockPlaceContext context) {
         if (this.requiresFloorSupport() && context.getClickedFace() != Direction.UP) {
            return null;
         }

         return super.getStateForPlacement(context);
      }

      private boolean requiresFloorSupport() {
         return this.shape != ControlsBlockTypes.ContactShape.ATTACHED_BUTTON;
      }

      @Override
      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return switch (this.shape) {
            case ATTACHED_BUTTON -> super.getShape(state, level, pos, context);
            case CONTACT_PLATE -> ControlsBlockTypes.CONTACT_PLATE_SHAPE;
            case FALLTHROUGH_FRAME -> ControlsBlockTypes.FALLTHROUGH_FRAME_SHAPE;
            case TRAPDOOR_PANEL -> ControlsBlockTypes.trapdoorPanelShape(state);
            case POWER_PLANT -> ControlsBlockTypes.POWER_PLANT_SHAPE;
         };
      }

      @Override
      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return switch (this.shape) {
            case ATTACHED_BUTTON, FALLTHROUGH_FRAME, POWER_PLANT -> Shapes.empty();
            case CONTACT_PLATE -> ControlsBlockTypes.CONTACT_PLATE_SHAPE;
            case TRAPDOOR_PANEL -> ControlsBlockTypes.trapdoorPanelShape(state);
         };
      }

      protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
         if (!level.isClientSide) {
            level.scheduleTick(pos, this, 12);
         }
      }

      protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
         if (!level.isClientSide && !(Boolean)state.getValue(ControlsBlockTypes.POWERED)) {
            this.setPowered(level, pos, state, true);
            level.scheduleTick(pos, this, 12);
         }
      }

      @Override
      protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
         boolean occupied = !level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(0.05), Entity::isAlive).isEmpty();
         if (occupied != (Boolean)state.getValue(ControlsBlockTypes.POWERED)) {
            this.setPowered(level, pos, state, occupied);
         }

         level.scheduleTick(pos, this, 12);
      }
   }

   public static class DimmerBlock extends DirectionalBlock {
      public static final MapCodec<ControlsBlockTypes.DimmerBlock> CODEC = simpleCodec(ControlsBlockTypes.DimmerBlock::new);

      public DimmerBlock(Properties properties) {
         super(properties);
         this.registerDefaultState(
            (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH))
                  .setValue(ControlsBlockTypes.POWERED, false))
               .setValue(ControlsBlockTypes.POWER, 0)
         );
      }

      protected MapCodec<? extends DirectionalBlock> codec() {
         return CODEC;
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{FACING, ControlsBlockTypes.POWERED, ControlsBlockTypes.POWER});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         return (BlockState)this.defaultBlockState().setValue(FACING, context.getClickedFace());
      }

      protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
         return ControlsBlockTypes.hasAttachedSupport(state, level, pos);
      }

      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         return ControlsBlockTypes.updateAttachedSupport(state, direction, level, pos);
      }

      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         if (!level.isClientSide) {
            int next = dimmerPowerFromHit(state, pos, hit);
            level.setBlock(pos, (BlockState)((BlockState)state.setValue(ControlsBlockTypes.POWER, next)).setValue(ControlsBlockTypes.POWERED, next > 0), 3);
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.relative(((Direction)state.getValue(FACING)).getOpposite()), this);
         }

         return InteractionResult.SUCCESS;
      }

      private static int dimmerPowerFromHit(BlockState state, BlockPos pos, BlockHitResult hit) {
         Vec3 location = hit.getLocation();
         double travel = switch ((Direction)state.getValue(FACING)) {
            case UP, DOWN -> location.z - pos.getZ();
            default -> location.y - pos.getY();
         };
         return Mth.clamp((int)Math.floor(travel * 16.0), 0, 15);
      }

      protected boolean isSignalSource(BlockState state) {
         return true;
      }

      protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
         return (Integer)state.getValue(ControlsBlockTypes.POWER);
      }

      protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
         return this.getSignal(state, level, pos, direction);
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return ControlsBlockTypes.attachedDeviceShape((Direction)state.getValue(FACING), 4.0, 12.0, 4.0);
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return Shapes.empty();
      }
   }

   public static class ElevatorButtonBlock extends ControlsBlockTypes.PulseSwitchBlock {
      public ElevatorButtonBlock(Properties properties) {
         super(properties, 20);
         this.registerDefaultState((BlockState)this.defaultBlockState().setValue(ControlsBlockTypes.VARIANT, 0));
      }

      @Override
      public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState state = super.getStateForPlacement(context);
         double localY = context.getClickLocation().y - context.getClickedPos().getY();
         return (BlockState)state.setValue(ControlsBlockTypes.VARIANT, elevatorVariantFromClickHeight(localY));
      }

      private static int elevatorVariantFromClickHeight(double localY) {
         if (localY < 0.375) {
            return 1;
         } else {
            return localY > 0.625 ? 2 : 0;
         }
      }

      @Override
      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{ControlsBlockTypes.VARIANT});
      }
   }

   public static class GaugeBlock extends DirectionalBlock {
      public static final MapCodec<ControlsBlockTypes.GaugeBlock> CODEC = simpleCodec(ControlsBlockTypes.GaugeBlock::new);

      public GaugeBlock(Properties properties) {
         super(properties);
         this.registerDefaultState(
            (BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(ControlsBlockTypes.POWER, 0)
         );
      }

      protected MapCodec<? extends DirectionalBlock> codec() {
         return CODEC;
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{FACING, ControlsBlockTypes.POWER});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState state = (BlockState)this.defaultBlockState().setValue(FACING, context.getClickedFace());
         return (BlockState)state.setValue(ControlsBlockTypes.POWER, this.readPower(context.getLevel(), context.getClickedPos(), state));
      }

      protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
         return ControlsBlockTypes.hasAttachedSupport(state, level, pos);
      }

      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         return ControlsBlockTypes.updateAttachedSupport(state, direction, level, pos);
      }

      protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
         this.updatePower(level, pos, state);
      }

      protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
         if (!level.isClientSide) {
            level.scheduleTick(pos, this, 20);
         }
      }

      protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
         this.updatePower(level, pos, state);
         level.scheduleTick(pos, this, 20);
      }

      protected void updatePower(Level level, BlockPos pos, BlockState state) {
         if (!level.isClientSide) {
            int power = this.readPower(level, pos, state);
            if (power != (Integer)state.getValue(ControlsBlockTypes.POWER)) {
               level.setBlock(pos, (BlockState)state.setValue(ControlsBlockTypes.POWER, power), 3);
            }
         }
      }

      protected int readPower(Level level, BlockPos pos, BlockState state) {
         return readAttachedSignal(level, pos, (Direction)state.getValue(FACING));
      }

      protected static int readAttachedSignal(Level level, BlockPos pos, Direction facing) {
         BlockPos attachedPos = pos.relative(facing.getOpposite());
         BlockState attachedState = level.getBlockState(attachedPos);
         if (attachedState.isAir()) {
            return 0;
         }

         int direct = level.getSignal(attachedPos, facing);
         int indirect = level.getBestNeighborSignal(attachedPos);
         int analog = attachedState.hasAnalogOutputSignal() ? attachedState.getAnalogOutputSignal(level, attachedPos) : 0;
         return Mth.clamp(Math.max(Math.max(direct, indirect), analog), 0, 15);
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return ControlsBlockTypes.attachedDeviceShape((Direction)state.getValue(FACING), 3.0, 13.0, 3.0);
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return Shapes.empty();
      }
   }

   public static class IndicatorBlock extends ControlsBlockTypes.GaugeBlock {
      public IndicatorBlock(Properties properties) {
         super(properties);
      }

      @Override
      protected int readPower(Level level, BlockPos pos, BlockState state) {
         return readAttachedSignal(level, pos, (Direction)state.getValue(FACING)) > 0 ? 15 : 0;
      }
   }

   public static class PulseSwitchBlock extends ControlsBlockTypes.ToggleSwitchBlock {
      private final int pulseTicks;

      public PulseSwitchBlock(Properties properties, int pulseTicks) {
         super(properties);
         this.pulseTicks = pulseTicks;
      }

      @Override
      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         if (!level.isClientSide) {
            this.setPowered(level, pos, state, true);
            level.scheduleTick(pos, this, this.pulseTicks);
         }

         return InteractionResult.SUCCESS;
      }

      protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
         if ((Boolean)state.getValue(ControlsBlockTypes.POWERED)) {
            this.setPowered(level, pos, state, false);
         }
      }
   }

   public static class SensitiveGlassBlock extends Block {
      public SensitiveGlassBlock(Properties properties) {
         super(properties);
         this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(ControlsBlockTypes.POWERED, false));
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{ControlsBlockTypes.POWERED});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         return (BlockState)this.defaultBlockState()
            .setValue(ControlsBlockTypes.POWERED, context.getLevel().getBestNeighborSignal(context.getClickedPos()) > 0);
      }

      protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
         if (!level.isClientSide) {
            boolean powered = level.getBestNeighborSignal(pos) > 0;
            if (powered != (Boolean)state.getValue(ControlsBlockTypes.POWERED)) {
               level.setBlock(pos, (BlockState)state.setValue(ControlsBlockTypes.POWERED, powered), 3);
            }
         }
      }
   }

   public enum SensorKind {
      DAY,
      RAIN,
      LIGHTNING,
      LIGHT,
      ENTITY,
      LINEAR_ENTITY,
      PLAYER,
      VILLAGER,
      ANIMAL,
      MOB,
      LIVING,
      BLOCK;
   }

   public static class SensorSwitchBlock extends ControlsBlockTypes.ToggleSwitchBlock {
      private final ControlsBlockTypes.SensorKind kind;

      public SensorSwitchBlock(Properties properties, ControlsBlockTypes.SensorKind kind) {
         super(properties);
         this.kind = kind;
      }

      @Override
      public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState state = super.getStateForPlacement(context);
         return (BlockState)state.setValue(ControlsBlockTypes.POWERED, this.evaluatePlacement(context.getLevel(), context.getClickedPos(), state));
      }

      protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
         if (!level.isClientSide) {
            level.scheduleTick(pos, this, 20);
         }
      }

      @Override
      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            this.updateSensor(serverLevel, pos, state);
            level.scheduleTick(pos, this, 20);
         }

         return InteractionResult.SUCCESS;
      }

      protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
         this.updateSensor(level, pos, state);
         level.scheduleTick(pos, this, 40);
      }

      private void updateSensor(ServerLevel level, BlockPos pos, BlockState state) {
         boolean powered = this.evaluate(level, pos, state);
         if (powered != (Boolean)state.getValue(ControlsBlockTypes.POWERED)) {
            this.setPowered(level, pos, state, powered);
         }
      }

      private boolean evaluatePlacement(Level level, BlockPos pos, BlockState state) {
         return switch (this.kind) {
            case DAY -> level.dimensionType().hasSkyLight() && level.getDayTime() % 24000L < 12000L;
            case RAIN -> level.isRainingAt(pos.above());
            case LIGHTNING -> level.isThundering() && level.canSeeSky(pos.above());
            case LIGHT -> level.getMaxLocalRawBrightness(pos) >= 8;
            case ENTITY, LINEAR_ENTITY, PLAYER, VILLAGER, ANIMAL, MOB, LIVING -> false;
            case BLOCK -> !level.getBlockState(pos.relative((Direction)state.getValue(FACING))).isAir();
         };
      }

      private boolean evaluate(ServerLevel level, BlockPos pos, BlockState state) {
         return switch (this.kind) {
            case DAY -> level.dimensionType().hasSkyLight() && level.getDayTime() % 24000L < 12000L;
            case RAIN -> level.isRainingAt(pos.above());
            case LIGHTNING -> level.isThundering() && level.canSeeSky(pos.above());
            case LIGHT -> level.getMaxLocalRawBrightness(pos) >= 8;
            case ENTITY -> this.hasEntity(level, pos, Entity.class);
            case LINEAR_ENTITY -> this.hasLinearEntity(level, pos, (Direction)state.getValue(FACING), Entity.class);
            case PLAYER -> this.hasEntity(level, pos, Player.class);
            case VILLAGER -> this.hasEntity(level, pos, Villager.class);
            case ANIMAL -> this.hasEntity(level, pos, Animal.class);
            case MOB -> this.hasEntity(level, pos, Mob.class);
            case LIVING -> this.hasEntity(level, pos, LivingEntity.class);
            case BLOCK -> !level.getBlockState(pos.relative((Direction)state.getValue(FACING))).isAir();
         };
      }

      private boolean hasEntity(ServerLevel level, BlockPos pos, Class<? extends Entity> type) {
         AABB box = new AABB(pos).inflate(4.0);
         return !level.getEntitiesOfClass(type, box, entity -> entity.isAlive()).isEmpty();
      }

      private boolean hasLinearEntity(ServerLevel level, BlockPos pos, Direction facing, Class<? extends Entity> type) {
         Vec3 center = Vec3.atCenterOf(pos);
         Vec3i normal = facing.getNormal();
         Vec3 end = center.add(normal.getX() * 4.5, normal.getY() * 4.5, normal.getZ() * 4.5);
         AABB box = new AABB(
               Math.min(center.x, end.x),
               Math.min(center.y, end.y),
               Math.min(center.z, end.z),
               Math.max(center.x, end.x),
               Math.max(center.y, end.y),
               Math.max(center.z, end.z)
            )
            .inflate(0.5);
         return !level.getEntitiesOfClass(type, box, Entity::isAlive).isEmpty();
      }
   }

   public static class SwitchLinkPulseReceiverBlock extends ControlsBlockTypes.PulseSwitchBlock {
      public static final MapCodec<ControlsBlockTypes.SwitchLinkPulseReceiverBlock> CODEC = simpleCodec(ControlsBlockTypes.SwitchLinkPulseReceiverBlock::new);

      public SwitchLinkPulseReceiverBlock(Properties properties) {
         super(properties, 25);
      }

      @Override
      protected MapCodec<? extends DirectionalBlock> codec() {
         return CODEC;
      }

      protected ItemInteractionResult useItemOn(
         ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
      ) {
         if (stack.is(Items.ENDER_PEARL)) {
            if (!level.isClientSide) {
               ControlsBlockTypes.giveLinkedPearl(level, pos, stack, player, hand);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
         } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         }
      }

      protected void receiveSwitchLink(Level level, BlockPos pos, BlockState state) {
         this.setPowered(level, pos, state, true);
         level.scheduleTick(pos, this, 25);
      }
   }

   public static class CasedSwitchLinkPulseReceiverBlock extends ControlsBlockTypes.SwitchLinkPulseReceiverBlock {
      public static final MapCodec<ControlsBlockTypes.CasedSwitchLinkPulseReceiverBlock> CODEC = simpleCodec(
         ControlsBlockTypes.CasedSwitchLinkPulseReceiverBlock::new
      );

      public CasedSwitchLinkPulseReceiverBlock(Properties properties) {
         super(properties);
      }

      @Override
      protected MapCodec<? extends DirectionalBlock> codec() {
         return CODEC;
      }

      @Override
      protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
         return true;
      }

      @Override
      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         return state;
      }

      @Override
      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return Shapes.block();
      }

      @Override
      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return Shapes.block();
      }
   }

   public static class SwitchLinkReceiverBlock extends ControlsBlockTypes.ToggleSwitchBlock {
      public static final MapCodec<ControlsBlockTypes.SwitchLinkReceiverBlock> CODEC = simpleCodec(ControlsBlockTypes.SwitchLinkReceiverBlock::new);

      public SwitchLinkReceiverBlock(Properties properties) {
         super(properties);
      }

      @Override
      protected MapCodec<? extends DirectionalBlock> codec() {
         return CODEC;
      }

      protected ItemInteractionResult useItemOn(
         ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
      ) {
         if (stack.is(Items.ENDER_PEARL)) {
            if (!level.isClientSide) {
               ControlsBlockTypes.giveLinkedPearl(level, pos, stack, player, hand);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
         } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         }
      }

      protected void receiveSwitchLink(Level level, BlockPos pos, BlockState state) {
         this.setPowered(level, pos, state, !(Boolean)state.getValue(ControlsBlockTypes.POWERED));
      }
   }

   public static class CasedSwitchLinkReceiverBlock extends ControlsBlockTypes.SwitchLinkReceiverBlock {
      public static final MapCodec<ControlsBlockTypes.CasedSwitchLinkReceiverBlock> CODEC = simpleCodec(
         ControlsBlockTypes.CasedSwitchLinkReceiverBlock::new
      );

      public CasedSwitchLinkReceiverBlock(Properties properties) {
         super(properties);
      }

      @Override
      protected MapCodec<? extends DirectionalBlock> codec() {
         return CODEC;
      }

      @Override
      protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
         return true;
      }

      @Override
      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         return state;
      }

      @Override
      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return Shapes.block();
      }

      @Override
      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return Shapes.block();
      }
   }

   public static class ToggleSwitchBlock extends DirectionalBlock {
      public static final MapCodec<ControlsBlockTypes.ToggleSwitchBlock> CODEC = simpleCodec(ControlsBlockTypes.ToggleSwitchBlock::new);

      public ToggleSwitchBlock(Properties properties) {
         super(properties);
         this.registerDefaultState(
            (BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(ControlsBlockTypes.POWERED, false)
         );
      }

      protected MapCodec<? extends DirectionalBlock> codec() {
         return CODEC;
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{FACING, ControlsBlockTypes.POWERED});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getClickedFace())).setValue(ControlsBlockTypes.POWERED, false);
      }

      protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
         return ControlsBlockTypes.hasAttachedSupport(state, level, pos);
      }

      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         return ControlsBlockTypes.updateAttachedSupport(state, direction, level, pos);
      }

      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         if (!level.isClientSide) {
            this.setPowered(level, pos, state, !(Boolean)state.getValue(ControlsBlockTypes.POWERED));
         }

         return InteractionResult.SUCCESS;
      }

      protected void setPowered(Level level, BlockPos pos, BlockState state, boolean powered) {
         level.setBlock(pos, (BlockState)state.setValue(ControlsBlockTypes.POWERED, powered), 3);
         level.updateNeighborsAt(pos, this);
         level.updateNeighborsAt(pos.relative(((Direction)state.getValue(FACING)).getOpposite()), this);
      }

      protected boolean isSignalSource(BlockState state) {
         return true;
      }

      protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
         return state.getValue(ControlsBlockTypes.POWERED) ? 15 : 0;
      }

      protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
         return this.getSignal(state, level, pos, direction);
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return ControlsBlockTypes.attachedDeviceShape((Direction)state.getValue(FACING), 4.0, 12.0, 4.0);
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return Shapes.empty();
      }
   }
}
