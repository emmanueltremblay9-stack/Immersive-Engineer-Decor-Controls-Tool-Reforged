package com.oblixorprime.engineersdecorreforged.block;

import com.mojang.serialization.MapCodec;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PortedBlocks {
   public static final DirectionProperty FACING = BlockStateProperties.FACING;
   public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty NORTH = BooleanProperty.create("north");
   public static final BooleanProperty EAST = BooleanProperty.create("east");
   public static final BooleanProperty SOUTH = BooleanProperty.create("south");
   public static final BooleanProperty WEST = BooleanProperty.create("west");
   // Legacy saves have no paid additions: their existing one-item loot remains valid.
   public static final IntegerProperty PAID_RAILINGS = IntegerProperty.create("paid_railings", 0, 4);
   public static final BooleanProperty LEFT_RAILING = BooleanProperty.create("left_railing");
   public static final BooleanProperty RIGHT_RAILING = BooleanProperty.create("right_railing");
   // Bit 0 is left, bit 1 is right. Unedited legacy sides retain automatic neighbor connections.
   public static final IntegerProperty EDITED_STAIR_SIDES = IntegerProperty.create("edited_stair_sides", 0, 3);
   public static final BooleanProperty EASTWEST = BooleanProperty.create("eastwest");
   public static final BooleanProperty LEFTBEAM = BooleanProperty.create("leftbeam");
   public static final BooleanProperty RIGHTBEAM = BooleanProperty.create("rightbeam");
   public static final IntegerProperty DOWNCONNECT = IntegerProperty.create("downconnect", 0, 2);
   public static final IntegerProperty TVARIANT = IntegerProperty.create("tvariant", 0, 3);
   public static final IntegerProperty PARTS = IntegerProperty.create("parts", 0, 14);
   public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 4);
   public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, 1);
   public static final IntegerProperty ARIADNE_MARKER_ROTATION = IntegerProperty.create("rotation", 0, 7);
   public static final EnumProperty<PortedBlocks.SimpleHalf> HALF = EnumProperty.create("half", PortedBlocks.SimpleHalf.class);
   public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
   public static final EnumProperty<PortedBlocks.SlidingDoorPairSide> PAIR_SIDE = EnumProperty.create(
      "pair_side", PortedBlocks.SlidingDoorPairSide.class
   );
   private static final double BORDER_PLACEMENT_BAND = 0.25D;
   private static final VoxelShape FLOOR_GRATING_SHAPE = Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);
   private static final VoxelShape SUPPORT_BEAM_NORTH_SOUTH_SHAPE = Shapes.or(
      Block.box(5.0, 15.0, 0.0, 11.0, 16.0, 16.0),
      new VoxelShape[]{
         Block.box(5.0, 11.0, 0.0, 11.0, 12.0, 16.0),
         Block.box(6.0, 12.0, 0.0, 7.0, 15.0, 16.0),
         Block.box(9.0, 12.0, 0.0, 10.0, 15.0, 16.0)
      }
   );
   private static final VoxelShape SUPPORT_BEAM_EAST_WEST_SHAPE = Shapes.or(
      Block.box(0.0, 15.0, 5.0, 16.0, 16.0, 11.0),
      new VoxelShape[]{
         Block.box(0.0, 11.0, 5.0, 16.0, 12.0, 11.0),
         Block.box(0.0, 12.0, 6.0, 16.0, 15.0, 7.0),
         Block.box(0.0, 12.0, 9.0, 16.0, 15.0, 10.0)
      }
   );
   private static final VoxelShape CATWALK_FLOOR_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0);
   private static final VoxelShape RAISED_CATWALK_SHAPE = Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);
   private static final VoxelShape NORTH_RAIL_SHAPE = Block.box(0.0, 3.0, 0.0, 16.0, 16.0, 2.0);
   private static final VoxelShape EAST_RAIL_SHAPE = Block.box(14.0, 3.0, 0.0, 16.0, 16.0, 16.0);
   private static final VoxelShape SOUTH_RAIL_SHAPE = Block.box(0.0, 3.0, 14.0, 16.0, 16.0, 16.0);
   private static final VoxelShape WEST_RAIL_SHAPE = Block.box(0.0, 3.0, 0.0, 2.0, 16.0, 16.0);
   private static final VoxelShape TABLE_SHAPE = Shapes.or(
      Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0),
      new VoxelShape[]{
         Block.box(1.0, 0.0, 1.0, 4.0, 14.0, 4.0),
         Block.box(12.0, 0.0, 1.0, 15.0, 14.0, 4.0),
         Block.box(1.0, 0.0, 12.0, 4.0, 14.0, 15.0),
         Block.box(12.0, 0.0, 12.0, 15.0, 14.0, 15.0)
      }
   );
   private static final VoxelShape STOOL_SHAPE = Shapes.or(
      Block.box(4.0, 7.0, 4.0, 12.0, 10.0, 12.0),
      new VoxelShape[]{Block.box(7.0, 0.0, 7.0, 9.0, 7.0, 9.0), Block.box(4.0, 0.0, 7.0, 12.0, 1.0, 9.0), Block.box(7.0, 0.0, 4.0, 9.0, 1.0, 12.0)}
   );

   private PortedBlocks() {
   }

   private static Direction catwalkStairPlacementFacing(BlockPlaceContext context) {
      return CatwalkStairsPlacement.facing(context);
   }

   private static Direction resolveHorizontalFacingFromClickedFaceBorder(BlockPlaceContext context, Direction fallback) {
      Direction clickedFace = context.getClickedFace();
      if (clickedFace != Direction.UP && clickedFace != Direction.DOWN) {
         return clickedFace.getAxis().isHorizontal() ? clickedFace : fallback;
      }

      Vec3 hit = context.getClickLocation();
      BlockPos clickedPos = context.getClickedPos();
      double localX = hit.x - clickedPos.getX();
      double localZ = hit.z - clickedPos.getZ();
      double north = localZ;
      double south = 1.0D - localZ;
      double west = localX;
      double east = 1.0D - localX;
      double nearest = BORDER_PLACEMENT_BAND;
      Direction result = fallback;

      if (north <= nearest) {
         nearest = north;
         result = Direction.NORTH;
      }

      if (south < nearest) {
         nearest = south;
         result = Direction.SOUTH;
      }

      if (west < nearest) {
         nearest = west;
         result = Direction.WEST;
      }

      if (east < nearest) {
         result = Direction.EAST;
      }

      return result;
   }

   private static VoxelShape centeredAxisShape(Direction facing, double min, double max) {
      return switch (facing.getAxis()) {
         case X -> Block.box(0.0, min, min, 16.0, max, max);
         case Y -> Block.box(min, 0.0, min, max, 16.0, max);
         case Z -> Block.box(min, min, 0.0, max, max, 16.0);
         default -> throw new MatchException(null, null);
      };
   }

   private static VoxelShape centeredPlaneShape(Direction facing, double min, double max) {
      return switch (facing.getAxis()) {
         case X -> Block.box(min, 0.0, 0.0, max, 16.0, 16.0);
         case Y -> Block.box(0.0, min, 0.0, 16.0, max, 16.0);
         case Z -> Block.box(0.0, 0.0, min, 16.0, 16.0, max);
         default -> throw new MatchException(null, null);
      };
   }

   private static VoxelShape facePlateShape(Direction facing, double min, double max, double thickness) {
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

   private static VoxelShape orientedNorthShape(VoxelShape northShape, Direction facing, boolean horizontalRotation) {
      VoxelShape oriented = Shapes.empty();
      for (AABB box : northShape.toAabbs()) {
         oriented = Shapes.or(oriented, Shapes.create(orientedNorthBox(box, facing, horizontalRotation)));
      }

      return oriented.optimize();
   }

   private static AABB orientedNorthBox(AABB box, Direction facing, boolean horizontalRotation) {
      if (horizontalRotation && !facing.getAxis().isHorizontal()) {
         return box;
      }

      return switch (facing) {
         case DOWN -> new AABB(1.0 - box.maxX, box.minZ, box.minY, 1.0 - box.minX, box.maxZ, box.maxY);
         case UP -> new AABB(
            1.0 - box.maxX, 1.0 - box.maxZ, 1.0 - box.maxY, 1.0 - box.minX, 1.0 - box.minZ, 1.0 - box.minY
         );
         case NORTH -> box;
         case SOUTH -> new AABB(1.0 - box.maxX, box.minY, 1.0 - box.maxZ, 1.0 - box.minX, box.maxY, 1.0 - box.minZ);
         case WEST -> new AABB(box.minZ, box.minY, 1.0 - box.maxX, box.maxZ, box.maxY, 1.0 - box.minX);
         case EAST -> new AABB(1.0 - box.maxZ, box.minY, box.minX, 1.0 - box.minZ, box.maxY, box.maxX);
      };
   }

   private static VoxelShape horizontalPlateShape(Direction facing, double thickness) {
      return switch (facing) {
         case NORTH -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, thickness);
         case SOUTH -> Block.box(0.0, 0.0, 16.0 - thickness, 16.0, 16.0, 16.0);
         case WEST -> Block.box(0.0, 0.0, 0.0, thickness, 16.0, 16.0);
         case EAST -> Block.box(16.0 - thickness, 0.0, 0.0, 16.0, 16.0, 16.0);
         default -> Block.box(0.0, 0.0, 0.0, 16.0, thickness, 16.0);
      };
   }

   private static VoxelShape horizontalHalfShape(Direction facing) {
      return switch (facing) {
         case NORTH -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0);
         case SOUTH -> Block.box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0);
         case WEST -> Block.box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0);
         case EAST -> Block.box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0);
         default -> Shapes.block();
      };
   }

   private static VoxelShape catwalkShape(BlockState state, boolean includeFloor) {
      VoxelShape shape = includeFloor ? CATWALK_FLOOR_SHAPE : Shapes.empty();
      if ((Boolean)state.getValue(NORTH)) {
         shape = Shapes.or(shape, NORTH_RAIL_SHAPE);
      }

      if ((Boolean)state.getValue(EAST)) {
         shape = Shapes.or(shape, EAST_RAIL_SHAPE);
      }

      if ((Boolean)state.getValue(SOUTH)) {
         shape = Shapes.or(shape, SOUTH_RAIL_SHAPE);
      }

      if ((Boolean)state.getValue(WEST)) {
         shape = Shapes.or(shape, WEST_RAIL_SHAPE);
      }

      return shape;
   }

   private static VoxelShape railingShape(BlockState state) {
      VoxelShape shape = Shapes.empty();
      boolean any = (Boolean)state.getValue(NORTH) || (Boolean)state.getValue(EAST) || (Boolean)state.getValue(SOUTH) || (Boolean)state.getValue(WEST);
      if (!any || (Boolean)state.getValue(NORTH)) {
         shape = Shapes.or(shape, NORTH_RAIL_SHAPE);
      }

      if ((Boolean)state.getValue(EAST)) {
         shape = Shapes.or(shape, EAST_RAIL_SHAPE);
      }

      if ((Boolean)state.getValue(SOUTH)) {
         shape = Shapes.or(shape, SOUTH_RAIL_SHAPE);
      }

      if ((Boolean)state.getValue(WEST)) {
         shape = Shapes.or(shape, WEST_RAIL_SHAPE);
      }

      return shape;
   }

   public enum PartKind { BASE, RAILING, INSET_LIGHT }

   /** A visible selectable component; lamps are distinct from the base without inventing a rail side. */
   public record RailingPart(@Nullable Direction side, VoxelShape shape, PartKind kind) {
      public RailingPart(@Nullable Direction side, VoxelShape shape) {
         this(side, shape, side == null ? PartKind.BASE : PartKind.RAILING);
      }

      public boolean sameComponent(RailingPart other) {
         return kind == other.kind && side == other.side;
      }
   }

   /** Shared by native mining and the client highlight. World occlusion is authoritative. */
   @Nullable
   public static RailingPart selectRailingPart(Level level, BlockPos pos, Vec3 start, Vec3 end, Entity context) {
      BlockState state = level.getBlockState(pos);
      if (!hasSelectableRailings(state)) {
         return null;
      }
      BlockHitResult worldHit = StairRailingTargeting.clipBlocks(level, start, end, context);
      if (worldHit.getType() != HitResult.Type.BLOCK || !worldHit.getBlockPos().equals(pos)) {
         return null;
      }
      if (state.getBlock() instanceof CatwalkStairsBlock) {
         return StairRailingTargeting.hasCloserEntity(context, start, end, worldHit)
            ? null : selectStairRailingPart(state, pos, start, end);
      }
      if (state.getBlock() instanceof RaisedCatwalkBlock raised) {
         return raised.selectPart(state, pos, start, end);
      }
      CatwalkBlock block = (CatwalkBlock)state.getBlock();
      state = block.explicitSides(state);
      RailingPart selected = null;
      double distance = Double.POSITIVE_INFINITY;
      if (!(block instanceof RailingBlock)) {
         BlockHitResult floorHit = CATWALK_FLOOR_SHAPE.clip(start, end, pos);
         if (floorHit != null) {
            selected = new RailingPart(null, CATWALK_FLOOR_SHAPE);
            distance = start.distanceToSqr(floorHit.getLocation());
         }
      }
      // At shared boundaries the floor, then north/east/south/west, wins exact ties.
      for (Direction side : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
         if (!state.getValue(CatwalkBlock.connectionProperty(side))) {
            continue;
         }
         VoxelShape shape = switch (side) {
            case NORTH -> NORTH_RAIL_SHAPE;
            case EAST -> EAST_RAIL_SHAPE;
            case SOUTH -> SOUTH_RAIL_SHAPE;
            case WEST -> WEST_RAIL_SHAPE;
            default -> throw new IllegalStateException("Expected a horizontal rail side");
         };
         BlockHitResult hit = shape.clip(start, end, pos);
         if (hit != null && start.distanceToSqr(hit.getLocation()) < distance - 1.0E-9) {
            selected = new RailingPart(side, shape);
            distance = start.distanceToSqr(hit.getLocation());
         }
      }
      return selected;
   }

   public static boolean hasSelectableRailings(BlockState state) {
      // Include unlit raised decks too: an old lamp completion must never turn into a base break.
      return state.getBlock() instanceof CatwalkBlock || state.getBlock() instanceof CatwalkStairsBlock
         || state.getBlock() instanceof RaisedCatwalkBlock;
   }

   @Nullable
   static RailingPart selectStairRailingPart(BlockState state, BlockPos pos, Vec3 start, Vec3 end) {
      Direction facing = state.getValue(HORIZONTAL_FACING);
      VoxelShape floor = catwalkStairShape(facing);
      BlockHitResult hit = floor.clip(start, end, pos);
      RailingPart selected = hit == null ? null : new RailingPart(null, floor);
      double distance = hit == null ? Double.POSITIVE_INFINITY : start.distanceToSqr(hit.getLocation());
      for (boolean left : new boolean[]{true, false}) {
         if (!state.getValue(left ? LEFT_RAILING : RIGHT_RAILING)) {
            continue;
         }
         VoxelShape shape = sideRailShape(facing, left);
         hit = shape.clip(start, end, pos);
         if (hit != null && start.distanceToSqr(hit.getLocation()) < distance - 1.0E-9) {
            selected = new RailingPart(left ? facing.getCounterClockWise() : facing.getClockWise(), shape);
            distance = start.distanceToSqr(hit.getLocation());
         }
      }
      return selected;
   }

   private static VoxelShape railingCollision(BlockState state, boolean includeFloor) {
      VoxelShape shape = includeFloor ? CATWALK_FLOOR_SHAPE : Shapes.empty();
      double top = includeFloor ? 27.0 : 24.0; // Fence-height protection above each walking surface.
      boolean legacyNorth = !includeFloor && CatwalkBlock.sideCount(state) == 0;
      if (legacyNorth || state.getValue(NORTH)) {
         shape = Shapes.or(shape, Block.box(0, 3, 0, 16, top, 2));
      }
      if (state.getValue(EAST)) {
         shape = Shapes.or(shape, Block.box(14, 3, 0, 16, top, 16));
      }
      if (state.getValue(SOUTH)) {
         shape = Shapes.or(shape, Block.box(0, 3, 14, 16, top, 16));
      }
      if (state.getValue(WEST)) {
         shape = Shapes.or(shape, Block.box(0, 3, 0, 2, top, 16));
      }
      return shape;
   }

   private static VoxelShape catwalkStairShape(Direction facing) {
      return switch (facing) {
         case NORTH -> Shapes.or(Block.box(0, 0, 8, 16, 4, 16), Block.box(0, 4, 0, 16, 12, 8));
         case SOUTH -> Shapes.or(Block.box(0, 0, 0, 16, 4, 8), Block.box(0, 4, 8, 16, 12, 16));
         case WEST -> Shapes.or(Block.box(8, 0, 0, 16, 4, 16), Block.box(0, 4, 0, 8, 12, 16));
         case EAST -> Shapes.or(Block.box(0, 0, 0, 8, 4, 16), Block.box(8, 4, 0, 16, 12, 16));
         default -> CATWALK_FLOOR_SHAPE;
      };
   }

   private static final VoxelShape[][] STAIR_RAIL_SHAPES = stairRailShapes(false);
   private static final VoxelShape[][] STAIR_RAIL_COLLISIONS = stairRailShapes(true);

   private static VoxelShape[][] stairRailShapes(boolean collision) {
      VoxelShape[][] result = new VoxelShape[4][2];
      for (int side = 0; side < 2; side++) {
         VoxelShape north = Shapes.empty();
         double minX = side == 0 ? 0 : 14;
         for (int z = 0; z < 16; z++) {
            double floor = z < 8 ? 12 : 4;
            // Keep the visible sloped outline, but guard the whole side above the highest tread.
            // A locally lowered cap is only one block above the upper tread, allowing a diagonal
            // normal/auto-jump over its lower half before the entity descends onto the lower tread.
            double top = collision ? 12 + 24 : 30.5 - z * 15.0 / 16.0;
            north = Shapes.or(north, Block.box(minX, floor, z, minX + 2, top, z + 1));
         }
         for (Direction facing : Direction.Plane.HORIZONTAL) {
            result[facing.get2DDataValue()][side] = orientedNorthShape(north, facing, true);
         }
      }
      return result;
   }

   private static VoxelShape sideRailShape(Direction facing, boolean left) {
      // StairRailingTargeting also discovers the unchanged upper model from the cell above.
      return STAIR_RAIL_SHAPES[facing.get2DDataValue()][left ? 0 : 1];
   }

   public static class AriadneMarkerBlock extends PortedBlocks.DirectionalPortBlock {
      public static final MapCodec<PortedBlocks.AriadneMarkerBlock> CODEC = simpleCodec(PortedBlocks.AriadneMarkerBlock::new);

      public AriadneMarkerBlock(Properties properties) {
         super(properties);
         this.registerDefaultState((BlockState)this.defaultBlockState().setValue(PortedBlocks.ARIADNE_MARKER_ROTATION, 0));
      }

      @Override
      protected MapCodec<? extends DirectionalBlock> codec() {
         return CODEC;
      }

      @Override
      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{FACING, PortedBlocks.ARIADNE_MARKER_ROTATION});
      }

      protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
         Direction facing = (Direction)state.getValue(FACING);
         BlockPos supportPos = pos.relative(facing.getOpposite());
         return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
      }

      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         return direction == ((Direction)state.getValue(FACING)).getOpposite() && !this.canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return PortedBlocks.facePlateShape((Direction)state.getValue(FACING), 0.0, 16.0, 0.1);
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return Shapes.empty();
      }
   }

   public static class CatwalkBlock extends Block implements NativeFluidloggedBlock {
      public CatwalkBlock(Properties properties) {
         super(properties.lightLevel(NativeFluidloggedBlock::light));
         this.registerDefaultState(
            (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(PortedBlocks.NORTH, false))
                        .setValue(PortedBlocks.EAST, false))
                     .setValue(PortedBlocks.SOUTH, false))
                  .setValue(PortedBlocks.WEST, false))
               .setValue(PortedBlocks.WATERLOGGED, false).setValue(NativeFluidloggedBlock.FLUID_CELL, NativeFluidloggedBlock.Cell.EMPTY).setValue(PortedBlocks.PAID_RAILINGS, 0)
         );
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{PortedBlocks.NORTH, PortedBlocks.EAST, PortedBlocks.SOUTH, PortedBlocks.WEST,
            PortedBlocks.WATERLOGGED, NativeFluidloggedBlock.FLUID_CELL, PortedBlocks.PAID_RAILINGS});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         return NativeFluidloggedBlock.placement(this.defaultBlockState(), context);
      }

      public FluidState getFluidState(BlockState state) {
         return NativeFluidloggedBlock.fluidState(state);
      }

      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         NativeFluidloggedBlock.schedule(state, level, pos);

         // These properties describe installed edge rails, not neighbor connections.
         return state;
      }

      protected BlockState explicitSides(BlockState state) {
         // Old standalone all-false states visibly contain a north rail.
         return this instanceof RailingBlock && sideCount(state) == 0 ? state.setValue(NORTH, true) : state;
      }

      private static int sideCount(BlockState state) {
         int count = 0;
         for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (state.getValue(connectionProperty(direction))) {
               ++count;
            }
         }
         return count;
      }

      protected final int paidRailings(BlockState state) {
         int capacity = Math.max(0, sideCount(state) - (this instanceof RailingBlock ? 1 : 0));
         return Math.min(state.getValue(PAID_RAILINGS), capacity);
      }

      protected static BooleanProperty connectionProperty(Direction direction) {
         return switch (direction) {
            case NORTH -> PortedBlocks.NORTH;
            case SOUTH -> PortedBlocks.SOUTH;
            case WEST -> PortedBlocks.WEST;
            case EAST -> PortedBlocks.EAST;
            default -> PortedBlocks.NORTH;
         };
      }

      protected static Direction clickedEdge(Vec3 hit, BlockPos pos, Direction fallback) {
         double x = Mth.clamp(hit.x - pos.getX(), 0.0, 1.0);
         double z = Mth.clamp(hit.z - pos.getZ(), 0.0, 1.0);
         Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
         double[] distances = {z, 1.0 - x, 1.0 - z, x};
         double nearest = Math.min(Math.min(distances[0], distances[1]), Math.min(distances[2], distances[3]));
         Direction result = null;
         for (int i = 0; i < directions.length; ++i) {
            if (Math.abs(distances[i] - nearest) < 1.0E-7) {
               if (directions[i] == fallback) {
                  return fallback;
               }
               if (result == null) {
                  result = directions[i];
               }
            }
         }
         return result == null ? fallback : result;
      }

      @Override
      protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
         if (!stack.is(ModBlocks.STEEL_RAILING.get().asItem())) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
         }
         if (player.isSpectator() || !player.getAbilities().mayBuild || !level.mayInteract(player, pos)
            || !level.getWorldBorder().isWithinBounds(pos)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
         }
         if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
         }

         // Interaction listeners can replace the target or the held stack after dispatch.
         // Never restore a stale block state or charge an item no longer held in this hand.
         state = level.getBlockState(pos);
         stack = player.getItemInHand(hand);
         if (!state.is(this) || !stack.is(ModBlocks.STEEL_RAILING.get().asItem())) {
            return ItemInteractionResult.CONSUME;
         }
         state = explicitSides(state);
         BooleanProperty side = connectionProperty(clickedEdge(hit.getLocation(), pos, player.getDirection()));
         boolean removing = state.getValue(side);
         boolean creative = player.getAbilities().instabuild;
         int paid = paidRailings(state);
         BlockState changed = state.setValue(side, !removing);
         int refund = 0;
         if (removing) {
            // Credits are fungible: removing a legacy/free edge can release a paid credit,
            // but never more items than were actually installed. Creative removal destroys it.
            if (paid > 0) {
               --paid;
               refund = 1;
            }
            if (this instanceof RailingBlock && sideCount(changed) == 0) {
               changed = state.getFluidState().createLegacyBlock();
               ++refund; // The original standalone block owns one base railing item.
            } else {
               changed = changed.setValue(PAID_RAILINGS, paid);
            }
         } else {
            changed = changed.setValue(PAID_RAILINGS, paid + (creative ? 0 : 1));
            if (!level.isUnobstructed(changed, pos, CollisionContext.of(player))) {
               // A recognized edit must not fall through to placing another block above it.
               return ItemInteractionResult.CONSUME;
            }
         }

         if (!level.setBlock(pos, changed, Block.UPDATE_ALL)) {
            return ItemInteractionResult.CONSUME;
         }
         if (!creative) {
            if (!removing) {
               stack.shrink(1);
            } else if (refund > 0) {
               ItemStack returned = new ItemStack(ModBlocks.STEEL_RAILING.get(), refund);
               if (!player.getInventory().add(returned)) {
                  player.drop(returned, false);
               }
            }
         }
         level.playSound(null, pos, removing ? SoundEvents.METAL_BREAK : SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
         level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
         return ItemInteractionResult.sidedSuccess(false);
      }

      @Override
      protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
         List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
         int paid = paidRailings(state);
         // Respect the original loot result, including explosion survival and custom tables.
         if (paid > 0 && drops.stream().anyMatch(stack -> stack.is(this.asItem()))) {
            drops.add(new ItemStack(ModBlocks.STEEL_RAILING.get(), paid));
         }
         return drops;
      }

      @Override
      public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                         boolean willHarvest, FluidState fluid) {
         if (player == null) {
            return super.onDestroyedByPlayer(state, level, pos, null, willHarvest, fluid);
         }
         RailingMining.Completion completion = RailingMining.complete(player, level, pos, state);
         if (completion == null) {
            return false;
         }
         RailingPart part = completion.part();
         BlockState explicit = explicitSides(state);
         if (part.side() == null || this instanceof RailingBlock && sideCount(explicit) == 1) {
            // Floor mining removes the catwalk normally; the final standalone side owns its base drop.
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
         }

         int paid = paidRailings(explicit);
         BlockState changed = explicit.setValue(connectionProperty(part.side()), false)
            .setValue(PAID_RAILINGS, Math.max(0, paid - 1));
         if (level.setBlock(pos, changed, Block.UPDATE_ALL) && !level.isClientSide && !player.getAbilities().instabuild && willHarvest) {
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
            if (paid > 0) {
               // Route one purchased rail through normal loot/drop hooks and doTileDrops.
               Block.dropResources(ModBlocks.STEEL_RAILING.get().defaultBlockState(), level, pos, null,
                  player, completion.tool());
            }
         }
         // A partial removal must not invoke whole-block playerDestroy or duplicate its loot.
         return false;
      }

      @Override
      protected BlockState rotate(BlockState state, Rotation rotation) {
         state = explicitSides(state);
         BlockState rotated = state;
         for (Direction direction : Direction.Plane.HORIZONTAL) {
            rotated = rotated.setValue(connectionProperty(rotation.rotate(direction)), state.getValue(connectionProperty(direction)));
         }
         return rotated;
      }

      @Override
      protected BlockState mirror(BlockState state, Mirror mirror) {
         state = explicitSides(state);
         BlockState mirrored = state;
         for (Direction direction : Direction.Plane.HORIZONTAL) {
            mirrored = mirrored.setValue(connectionProperty(mirror.mirror(direction)), state.getValue(connectionProperty(direction)));
         }
         return mirrored;
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return PortedBlocks.catwalkShape(state, true);
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return PortedBlocks.railingCollision(state, true);
      }
   }

   public static class CatwalkStairsBlock extends HorizontalDirectionalBlock implements NativeFluidloggedBlock {
      public static final net.minecraft.world.level.block.state.properties.IntegerProperty WATER_FLOW = net.minecraft.world.level.block.state.properties.IntegerProperty.create("water_flow", 0, 15);
      public static final MapCodec<PortedBlocks.CatwalkStairsBlock> CODEC = simpleCodec(PortedBlocks.CatwalkStairsBlock::new);

      public CatwalkStairsBlock(Properties properties) {
         super(properties.lightLevel(NativeFluidloggedBlock::light));
         this.registerDefaultState(
            (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any())
                        .setValue(PortedBlocks.HORIZONTAL_FACING, Direction.NORTH))
                     .setValue(PortedBlocks.LEFT_RAILING, false))
                  .setValue(PortedBlocks.RIGHT_RAILING, false))
               .setValue(WATER_FLOW, 0).setValue(PortedBlocks.WATERLOGGED, false).setValue(NativeFluidloggedBlock.FLUID_CELL, NativeFluidloggedBlock.Cell.EMPTY).setValue(EDITED_STAIR_SIDES, 0).setValue(PAID_RAILINGS, 0)
         );
      }

      protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
         return CODEC;
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{PortedBlocks.HORIZONTAL_FACING, PortedBlocks.LEFT_RAILING, PortedBlocks.RIGHT_RAILING, PortedBlocks.WATERLOGGED, NativeFluidloggedBlock.FLUID_CELL, WATER_FLOW, EDITED_STAIR_SIDES, PAID_RAILINGS});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         Direction facing = PortedBlocks.catwalkStairPlacementFacing(context);
         return this.withSideRailings(
            NativeFluidloggedBlock.placement(this.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, facing), context),
            context.getLevel(),
            context.getClickedPos()
         );
      }

      public FluidState getFluidState(BlockState state) {
         return StairWaterFlow.fluid(state);
      }

      @Override
      public boolean canPlaceLiquid(@javax.annotation.Nullable net.minecraft.world.entity.player.Player player, BlockGetter level, BlockPos pos, BlockState state, net.minecraft.world.level.material.Fluid fluid) {
         if (fluid == net.minecraft.world.level.material.Fluids.FLOWING_WATER)
            return !state.getValue(WATERLOGGED) && state.getValue(NativeFluidloggedBlock.FLUID_CELL) == NativeFluidloggedBlock.Cell.EMPTY;
         if (StairWaterFlow.active(state) && fluid != net.minecraft.world.level.material.Fluids.WATER) return false;
         return NativeFluidloggedBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
      }

      @Override
      public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
         if (fluid.getType() == net.minecraft.world.level.material.Fluids.FLOWING_WATER
               && canPlaceLiquid(null, level, pos, state, fluid.getType())) {
            if (!level.isClientSide()) {
               if (!level.setBlock(pos, StairWaterFlow.withFlow(state, fluid), 3)) return false;
               level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
            }
            return true;
         }
         if (!canPlaceLiquid(null, level, pos, state, fluid.getType())) return false;
         return NativeFluidloggedBlock.super.placeLiquid(level, pos, state.setValue(WATER_FLOW, 0), fluid);
      }

      @Override
      public net.minecraft.world.item.ItemStack pickupBlock(@javax.annotation.Nullable net.minecraft.world.entity.player.Player player, LevelAccessor level, BlockPos pos, BlockState state) {
         if (StairWaterFlow.active(state)) return net.minecraft.world.item.ItemStack.EMPTY;
         return NativeFluidloggedBlock.super.pickupBlock(player, level, pos, state.setValue(WATER_FLOW, 0));
      }

      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         NativeFluidloggedBlock.schedule(state, level, pos);

         Direction facing = (Direction)state.getValue(PortedBlocks.HORIZONTAL_FACING);
         if (direction == facing.getCounterClockWise() && (state.getValue(EDITED_STAIR_SIDES) & 1) == 0) {
            return (BlockState)state.setValue(PortedBlocks.LEFT_RAILING, this.connectsToSideRailing(neighborState));
         } else {
            return direction == facing.getClockWise() && (state.getValue(EDITED_STAIR_SIDES) & 2) == 0
               ? (BlockState)state.setValue(PortedBlocks.RIGHT_RAILING, this.connectsToSideRailing(neighborState))
               : state;
         }
      }

      private BlockState withSideRailings(BlockState state, BlockGetter level, BlockPos pos) {
         Direction facing = (Direction)state.getValue(PortedBlocks.HORIZONTAL_FACING);
         return (BlockState)((BlockState)state.setValue(
               PortedBlocks.LEFT_RAILING, this.connectsToSideRailing(level.getBlockState(pos.relative(facing.getCounterClockWise())))
            ))
            .setValue(PortedBlocks.RIGHT_RAILING, this.connectsToSideRailing(level.getBlockState(pos.relative(facing.getClockWise()))));
      }

      private boolean connectsToSideRailing(BlockState neighborState) {
         Block block = neighborState.getBlock();
         return block instanceof PortedBlocks.RailingBlock || block instanceof PortedBlocks.FenceGateSegmentBlock;
      }

      private static int paidRailings(BlockState state) {
         int count = (state.getValue(LEFT_RAILING) ? 1 : 0) + (state.getValue(RIGHT_RAILING) ? 1 : 0);
         return Math.min(state.getValue(PAID_RAILINGS), count);
      }

      private static boolean leftSide(BlockState state, BlockPos pos, BlockHitResult hit, Direction fallback) {
         Direction left = state.getValue(HORIZONTAL_FACING).getCounterClockWise();
         double offset = (hit.getLocation().x - pos.getX() - 0.5) * left.getStepX()
            + (hit.getLocation().z - pos.getZ() - 0.5) * left.getStepZ();
         return Math.abs(offset) < 1.0E-7 ? fallback == left : offset > 0;
      }

      private static BlockState editSide(BlockState state, boolean left, boolean present, int paid) {
         return state.setValue(left ? LEFT_RAILING : RIGHT_RAILING, present)
            .setValue(EDITED_STAIR_SIDES, state.getValue(EDITED_STAIR_SIDES) | (left ? 1 : 2))
            .setValue(PAID_RAILINGS, paid);
      }

      @Override
      protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
         if (!stack.is(ModBlocks.STEEL_RAILING.get().asItem())) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
         }
         if (player.isSpectator() || !player.getAbilities().mayBuild || !level.mayInteract(player, pos)
            || !level.getWorldBorder().isWithinBounds(pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
         }
         if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
         }
         state = level.getBlockState(pos);
         stack = player.getItemInHand(hand);
         if (!state.is(this) || !stack.is(ModBlocks.STEEL_RAILING.get().asItem())) {
            return ItemInteractionResult.CONSUME;
         }
         boolean left = leftSide(state, pos, hit, player.getDirection());
         boolean removing = state.getValue(left ? LEFT_RAILING : RIGHT_RAILING);
         boolean creative = player.getAbilities().instabuild;
         int paid = paidRailings(state);
         int refund = removing && paid > 0 ? 1 : 0;
         BlockState changed = editSide(state, left, !removing, removing ? paid - refund : paid + (creative ? 0 : 1));
         if (!removing && !level.isUnobstructed(changed, pos, CollisionContext.of(player))) {
            return ItemInteractionResult.CONSUME;
         }
         if (!level.setBlock(pos, changed, Block.UPDATE_ALL)) {
            return ItemInteractionResult.CONSUME;
         }
         if (!creative) {
            if (!removing) {
               stack.shrink(1);
            } else if (refund > 0) {
               ItemStack returned = new ItemStack(ModBlocks.STEEL_RAILING.get());
               if (!player.getInventory().add(returned)) {
                  player.drop(returned, false);
               }
            }
         }
         level.playSound(null, pos, removing ? SoundEvents.METAL_BREAK : SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1, 1);
         level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
         return ItemInteractionResult.sidedSuccess(false);
      }

      @Override
      protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
         List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
         int paid = paidRailings(state);
         if (paid > 0 && drops.stream().anyMatch(stack -> stack.is(this.asItem()))) {
            drops.add(new ItemStack(ModBlocks.STEEL_RAILING.get(), paid));
         }
         return drops;
      }

      @Override
      public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                         boolean willHarvest, FluidState fluid) {
         if (player == null) {
            return super.onDestroyedByPlayer(state, level, pos, null, willHarvest, fluid);
         }
         RailingMining.Completion completion = RailingMining.complete(player, level, pos, state);
         if (completion == null) {
            return false;
         }
         if (completion.part().side() == null) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
         }
         boolean left = completion.part().side() == state.getValue(HORIZONTAL_FACING).getCounterClockWise();
         int paid = paidRailings(state);
         BlockState changed = editSide(state, left, false, Math.max(0, paid - 1));
         if (level.setBlock(pos, changed, Block.UPDATE_ALL) && !level.isClientSide && !player.getAbilities().instabuild && willHarvest) {
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
            if (paid > 0) {
               Block.dropResources(ModBlocks.STEEL_RAILING.get().defaultBlockState(), level, pos, null, player, completion.tool());
            }
         }
         return false;
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         VoxelShape shape = PortedBlocks.catwalkStairShape((Direction)state.getValue(PortedBlocks.HORIZONTAL_FACING));
         if ((Boolean)state.getValue(PortedBlocks.LEFT_RAILING)) {
            shape = Shapes.or(shape, PortedBlocks.sideRailShape((Direction)state.getValue(PortedBlocks.HORIZONTAL_FACING), true));
         }

         if ((Boolean)state.getValue(PortedBlocks.RIGHT_RAILING)) {
            shape = Shapes.or(shape, PortedBlocks.sideRailShape((Direction)state.getValue(PortedBlocks.HORIZONTAL_FACING), false));
         }

         return shape;
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         Direction facing = state.getValue(HORIZONTAL_FACING);
         VoxelShape shape = catwalkStairShape(facing);
         if (state.getValue(LEFT_RAILING)) {
            shape = Shapes.or(shape, STAIR_RAIL_COLLISIONS[facing.get2DDataValue()][0]);
         }
         if (state.getValue(RIGHT_RAILING)) {
            shape = Shapes.or(shape, STAIR_RAIL_COLLISIONS[facing.get2DDataValue()][1]);
         }
         return shape;
      }
   }

   public static class CenteredPoleBlock extends PortedBlocks.DirectionalPortBlock {
      private final double min;
      private final double max;

      public CenteredPoleBlock(Properties properties, double min, double max) {
         super(properties);
         this.min = min;
         this.max = max;
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return PortedBlocks.centeredAxisShape((Direction)state.getValue(FACING), this.min, this.max);
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.getShape(state, level, pos, context);
      }
   }

   public static class DirectionalPortBlock extends DirectionalBlock {
      public static final MapCodec<PortedBlocks.DirectionalPortBlock> CODEC = simpleCodec(PortedBlocks.DirectionalPortBlock::new);

      public DirectionalPortBlock(Properties properties) {
         super(properties);
         this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
      }

      protected MapCodec<? extends DirectionalBlock> codec() {
         return CODEC;
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{FACING});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         return (BlockState)this.defaultBlockState().setValue(FACING, context.getClickedFace());
      }
   }

   public static class FenceGateSegmentBlock extends HorizontalDirectionalBlock {
      public static final MapCodec<PortedBlocks.FenceGateSegmentBlock> CODEC = simpleCodec(PortedBlocks.FenceGateSegmentBlock::new);
      private static final VoxelShape CLOSED_NORTH_SOUTH_SHAPE = Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0);
      private static final VoxelShape CLOSED_EAST_WEST_SHAPE = Block.box(7.0, 0.0, 0.0, 9.0, 16.0, 16.0);
      private static final VoxelShape OPEN_NORTH_SHAPE = Shapes.or(
         Block.box(0.0, 0.0, 6.5, 3.0, 16.0, 10.5), Block.box(12.0, 0.0, 0.0, 16.0, 16.0, 10.5)
      );
      private static final VoxelShape OPEN_EAST_SHAPE = rotateClockwise(OPEN_NORTH_SHAPE);
      private static final VoxelShape OPEN_SOUTH_SHAPE = rotateClockwise(OPEN_EAST_SHAPE);
      private static final VoxelShape OPEN_WEST_SHAPE = rotateClockwise(OPEN_SOUTH_SHAPE);
      // Keep the rendered/selection outline, but let a 0.6-wide player fit between the
      // structural posts (3/16..13/16). The thin inward decorative leaf is not a barrier.
      private static final VoxelShape OPEN_NORTH_COLLISION = Shapes.or(
         Block.box(0.0, 0.0, 6.5, 3.0, 16.0, 10.5), Block.box(13.0, 0.0, 0.0, 16.0, 16.0, 10.5)
      );
      private static final VoxelShape OPEN_EAST_COLLISION = rotateClockwise(OPEN_NORTH_COLLISION);
      private static final VoxelShape OPEN_SOUTH_COLLISION = rotateClockwise(OPEN_EAST_COLLISION);
      private static final VoxelShape OPEN_WEST_COLLISION = rotateClockwise(OPEN_SOUTH_COLLISION);

      public FenceGateSegmentBlock(Properties properties) {
         super(properties);
         this.registerDefaultState(
            (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(PortedBlocks.HORIZONTAL_FACING, Direction.NORTH))
                  .setValue(PortedBlocks.OPEN, false))
               .setValue(PortedBlocks.SEGMENT, 0)
         );
      }

      protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
         return CODEC;
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{PortedBlocks.HORIZONTAL_FACING, PortedBlocks.OPEN, PortedBlocks.SEGMENT});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
         return below.getBlock() instanceof PortedBlocks.FenceGateSegmentBlock && below.getValue(PortedBlocks.SEGMENT) == 0
            ? (BlockState)((BlockState)((BlockState)this.defaultBlockState()
                     .setValue(PortedBlocks.HORIZONTAL_FACING, (Direction)below.getValue(PortedBlocks.HORIZONTAL_FACING)))
                  .setValue(PortedBlocks.OPEN, (Boolean)below.getValue(PortedBlocks.OPEN)))
               .setValue(PortedBlocks.SEGMENT, 1)
            : (BlockState)((BlockState)this.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite()))
               .setValue(PortedBlocks.SEGMENT, 0);
      }

      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         if (!level.isClientSide) {
            boolean open = !state.getValue(PortedBlocks.OPEN);
            this.setOpen(level, pos, state, open);
            BlockState changed = level.getBlockState(pos);
            if (changed.is(this) && changed.getValue(PortedBlocks.OPEN) == open) {
               level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            }
         }

         return InteractionResult.SUCCESS;
      }

      private void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
         level.setBlock(pos, (BlockState)state.setValue(PortedBlocks.OPEN, open), 10);
         BlockPos linkedPos = state.getValue(PortedBlocks.SEGMENT) == 0 ? pos.above() : pos.below();
         BlockState linkedState = level.getBlockState(linkedPos);
         if (linkedState.getBlock() instanceof PortedBlocks.FenceGateSegmentBlock
            && linkedState.getValue(PortedBlocks.SEGMENT) != state.getValue(PortedBlocks.SEGMENT)) {
            level.setBlock(linkedPos, (BlockState)linkedState.setValue(PortedBlocks.OPEN, open), 10);
            level.updateNeighborsAt(linkedPos, this);
         }

         level.updateNeighborsAt(pos, this);
      }

      protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
         if (!state.is(newState.getBlock())) {
            BlockPos linkedPos = state.getValue(PortedBlocks.SEGMENT) == 0 ? pos.above() : pos.below();
            BlockState linkedState = level.getBlockState(linkedPos);
            if (linkedState.getBlock() instanceof PortedBlocks.FenceGateSegmentBlock
               && linkedState.getValue(PortedBlocks.SEGMENT) != state.getValue(PortedBlocks.SEGMENT)) {
               level.removeBlock(linkedPos, false);
            }
         }

         super.onRemove(state, level, pos, newState, movedByPiston);
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         Direction facing = (Direction)state.getValue(PortedBlocks.HORIZONTAL_FACING);
         if ((Boolean)state.getValue(PortedBlocks.OPEN)) {
            return switch (facing) {
               case NORTH -> OPEN_NORTH_SHAPE;
               case EAST -> OPEN_EAST_SHAPE;
               case SOUTH -> OPEN_SOUTH_SHAPE;
               case WEST -> OPEN_WEST_SHAPE;
               default -> OPEN_NORTH_SHAPE;
            };
         }

         return facing.getAxis() == Axis.Z ? CLOSED_NORTH_SOUTH_SHAPE : CLOSED_EAST_WEST_SHAPE;
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         if (!state.getValue(PortedBlocks.OPEN)) {
            return this.getShape(state, level, pos, context);
         }
         return switch (state.getValue(PortedBlocks.HORIZONTAL_FACING)) {
            case NORTH -> OPEN_NORTH_COLLISION;
            case EAST -> OPEN_EAST_COLLISION;
            case SOUTH -> OPEN_SOUTH_COLLISION;
            case WEST -> OPEN_WEST_COLLISION;
            default -> OPEN_NORTH_COLLISION;
         };
      }

      private static VoxelShape rotateClockwise(VoxelShape shape) {
         VoxelShape rotated = Shapes.empty();

         for (AABB box : shape.toAabbs()) {
            rotated = Shapes.or(rotated, Shapes.create(1.0 - box.maxZ, box.minY, box.minX, 1.0 - box.minZ, box.maxY, box.maxX));
         }

         return rotated;
      }
   }

   public static class FixedShapeBlock extends Block {
      public static final MapCodec<PortedBlocks.FixedShapeBlock> CODEC = simpleCodec(properties -> new PortedBlocks.FixedShapeBlock(properties, Shapes.block()));
      private final VoxelShape shape;
      private final boolean emptyCollision;

      public FixedShapeBlock(Properties properties, VoxelShape shape) {
         this(properties, shape, false);
      }

      public FixedShapeBlock(Properties properties, VoxelShape shape, boolean emptyCollision) {
         super(properties);
         this.shape = shape;
         this.emptyCollision = emptyCollision;
      }

      protected MapCodec<? extends Block> codec() {
         return CODEC;
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.shape;
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.emptyCollision ? Shapes.empty() : this.shape;
      }
   }

   public static class FloorGratingBlock extends PortedBlocks.FixedShapeBlock {
      public FloorGratingBlock(Properties properties) {
         super(properties, PortedBlocks.FLOOR_GRATING_SHAPE);
      }
   }

   public static class HatchBlock extends HorizontalDirectionalBlock {
      public static final MapCodec<PortedBlocks.HatchBlock> CODEC = simpleCodec(properties -> new PortedBlocks.HatchBlock(BlockSetType.IRON, properties));
      private static final VoxelShape CLOSED_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0);
      private static final double OPEN_SHAPE_THICKNESS = 2.875;
      private final BlockSetType type;

      public HatchBlock(BlockSetType type, Properties properties) {
         super(properties);
         this.type = type;
         this.registerDefaultState(
            (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(PortedBlocks.HORIZONTAL_FACING, Direction.NORTH))
                  .setValue(PortedBlocks.OPEN, false))
               .setValue(PortedBlocks.POWERED, false)
         );
      }

      protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
         return CODEC;
      }

      public BlockSetType type() {
         return this.type;
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{PortedBlocks.HORIZONTAL_FACING, PortedBlocks.OPEN, PortedBlocks.POWERED});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
         // Our model faces the hinge edge, opposite to vanilla TrapDoorBlock.FACING.
         Direction face = context.getClickedFace();
         Direction hinge = !context.replacingClickedOnBlock() && face.getAxis().isHorizontal()
            ? face.getOpposite() : context.getHorizontalDirection();
         return (BlockState)((BlockState)((BlockState)this.defaultBlockState()
                  .setValue(PortedBlocks.HORIZONTAL_FACING, hinge))
               .setValue(PortedBlocks.POWERED, powered))
            .setValue(PortedBlocks.OPEN, powered);
      }

      @Override
      public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, @Nullable LivingEntity entity) {
         if (!state.getValue(PortedBlocks.OPEN)) {
            return false;
         }
         BlockPos belowPos = pos.below();
         BlockState below = level.getBlockState(belowPos);
         // The ladder hook expects vanilla's facing convention, opposite our physical hinge edge.
         // Translate only this query state; already placed hatches keep their saved orientation.
         BlockState vanillaFacing = state.setValue(PortedBlocks.HORIZONTAL_FACING,
            state.getValue(PortedBlocks.HORIZONTAL_FACING).getOpposite());
         return below.getBlock().makesOpenTrapdoorAboveClimbable(below, level, belowPos, vanillaFacing);
      }

      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         if (!level.isClientSide) {
            boolean open = (Boolean)state.getValue(PortedBlocks.POWERED) || !(Boolean)state.getValue(PortedBlocks.OPEN);
            if (open != (Boolean)state.getValue(PortedBlocks.OPEN)) {
               this.setOpen(player, level, pos, state, open);
            }
         }

         return InteractionResult.SUCCESS;
      }

      protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
         if (!level.isClientSide) {
            boolean powered = level.hasNeighborSignal(pos);
            if (powered != (Boolean)state.getValue(PortedBlocks.POWERED)) {
               this.setOpen(null, level, pos, (BlockState)state.setValue(PortedBlocks.POWERED, powered), powered);
            }
         }
      }

      private void setOpen(@Nullable Entity entity, Level level, BlockPos pos, BlockState state, boolean open) {
         boolean changed = (Boolean)state.getValue(PortedBlocks.OPEN) != open;
         level.setBlock(pos, (BlockState)state.setValue(PortedBlocks.OPEN, open), 10);
         level.updateNeighborsAt(pos, this);
         if (changed) {
            level.playSound(
               entity,
               pos,
               open ? this.type.trapdoorOpen() : this.type.trapdoorClose(),
               SoundSource.BLOCKS,
               1.0F,
               level.getRandom().nextFloat() * 0.1F + 0.9F
            );
            level.gameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
         }
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return state.getValue(PortedBlocks.OPEN)
            ? PortedBlocks.horizontalPlateShape((Direction)state.getValue(PortedBlocks.HORIZONTAL_FACING), OPEN_SHAPE_THICKNESS)
            : CLOSED_SHAPE;
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.getShape(state, level, pos, context);
      }
   }

   public static class HorizontalFacingBlock extends HorizontalDirectionalBlock {
      public static final MapCodec<PortedBlocks.HorizontalFacingBlock> CODEC = simpleCodec(PortedBlocks.HorizontalFacingBlock::new);

      public HorizontalFacingBlock(Properties properties) {
         super(properties);
         this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(PortedBlocks.HORIZONTAL_FACING, Direction.NORTH));
      }

      protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
         return CODEC;
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{PortedBlocks.HORIZONTAL_FACING});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         return (BlockState)this.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
      }
   }

   public static class PartsBlock extends Block {
      public PartsBlock(Properties properties) {
         super(properties);
         this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(PortedBlocks.PARTS, 0));
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{PortedBlocks.PARTS});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         double localY = context.getClickLocation().y - Math.floor(context.getClickLocation().y);
         int part = Mth.clamp((int)Math.floor(localY * 15.0), 0, 14);
         return (BlockState)this.defaultBlockState().setValue(PortedBlocks.PARTS, part);
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         int y = (Integer)state.getValue(PortedBlocks.PARTS);
         return Block.box(0.0, y, 0.0, 16.0, Math.min(16.0, y + 2.0), 16.0);
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.getShape(state, level, pos, context);
      }
   }

   public static class RailingBlock extends PortedBlocks.CatwalkBlock {
      public RailingBlock(Properties properties) {
         super(properties);
      }

      @Override
      public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState current = context.getLevel().getBlockState(context.getClickedPos());
         if (current.is(this)) {
            if (context.replacingClickedOnBlock() || context.getClickedFace() != Direction.UP) {
               return null;
            }
            Player player = context.getPlayer();
            if (!context.getLevel().getWorldBorder().isWithinBounds(context.getClickedPos())
               || player != null && (player.isSpectator() || !player.getAbilities().mayBuild
                  || !context.getLevel().mayInteract(player, context.getClickedPos()))) {
               return null;
            }
            current = explicitSides(current);
            BooleanProperty side = connectionProperty(clickedEdge(context.getClickLocation(), context.getClickedPos(), context.getHorizontalDirection()));
            if (current.getValue(side)) {
               return null;
            }
            // Native BlockItem placement handles target permissions, obstruction, payment
            // and canceled-placement rollback when the support below was actually clicked.
            boolean creative = player != null && player.getAbilities().instabuild;
            return current.setValue(side, true).setValue(PAID_RAILINGS, paidRailings(current) + (creative ? 0 : 1));
         }
         return super.getStateForPlacement(context).setValue(
            connectionProperty(clickedEdge(context.getClickLocation(), context.getClickedPos(), context.getHorizontalDirection())), true);
      }

      @Override
      protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
         if (context.replacingClickedOnBlock() || !context.getItemInHand().is(this.asItem()) || context.getClickedFace() != Direction.UP) {
            return false;
         }
         state = explicitSides(state);
         BooleanProperty side = connectionProperty(clickedEdge(context.getClickLocation(), context.getClickedPos(), context.getHorizontalDirection()));
         return !state.getValue(side);
      }

      @Override
      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return PortedBlocks.railingShape(state);
      }

      @Override
      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return PortedBlocks.railingCollision(state, false);
      }
   }

   public static class SimpleDoorLikeBlock extends HorizontalDirectionalBlock {
      public static final MapCodec<PortedBlocks.SimpleDoorLikeBlock> CODEC = simpleCodec(PortedBlocks.SimpleDoorLikeBlock::new);

      public SimpleDoorLikeBlock(Properties properties) {
         super(properties);
         this.registerDefaultState(
            (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any())
                           .setValue(PortedBlocks.HORIZONTAL_FACING, Direction.NORTH))
                        .setValue(PortedBlocks.HALF, PortedBlocks.SimpleHalf.LOWER))
                     .setValue(PortedBlocks.HINGE, DoorHingeSide.LEFT))
                  .setValue(PortedBlocks.OPEN, false))
               .setValue(PortedBlocks.POWERED, false)
         );
      }

      protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
         return CODEC;
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{PortedBlocks.HORIZONTAL_FACING, PortedBlocks.HALF, PortedBlocks.HINGE, PortedBlocks.OPEN, PortedBlocks.POWERED});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
         return (BlockState)((BlockState)((BlockState)this.defaultBlockState()
                  .setValue(PortedBlocks.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite()))
               .setValue(PortedBlocks.POWERED, powered))
            .setValue(PortedBlocks.OPEN, powered);
      }

      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         if (!level.isClientSide) {
            level.setBlock(pos, (BlockState)state.cycle(PortedBlocks.OPEN), 10);
            level.updateNeighborsAt(pos, this);
         }

         return InteractionResult.SUCCESS;
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return state.getValue(PortedBlocks.OPEN)
            ? PortedBlocks.horizontalPlateShape((Direction)state.getValue(PortedBlocks.HORIZONTAL_FACING), 2.0)
            : Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return state.getValue(PortedBlocks.OPEN) ? Shapes.empty() : this.getShape(state, level, pos, context);
      }
   }

   public enum SimpleHalf implements StringRepresentable {
      UPPER("upper"),
      LOWER("lower");

      private final String name;

      SimpleHalf(String name) {
         this.name = name;
      }

      public String getSerializedName() {
         return this.name;
      }
   }

   public enum SlidingDoorPairSide implements StringRepresentable {
      SINGLE("single"),
      LEFT("left"),
      RIGHT("right");

      private final String name;

      SlidingDoorPairSide(String name) {
         this.name = name;
      }

      public String getSerializedName() {
         return this.name;
      }

      public SlidingDoorPairSide opposite() {
         return switch (this) {
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            case SINGLE -> SINGLE;
         };
      }
   }

   public static class StoolBlock extends PortedBlocks.FixedShapeBlock {
      public StoolBlock(Properties properties) {
         super(properties, PortedBlocks.STOOL_SHAPE);
      }
   }

   public static class SupportBlock extends Block {
      public SupportBlock(Properties properties) {
         super(properties);
         this.registerDefaultState(
            (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(PortedBlocks.EASTWEST, false))
                     .setValue(PortedBlocks.LEFTBEAM, false))
                  .setValue(PortedBlocks.RIGHTBEAM, false))
               .setValue(PortedBlocks.DOWNCONNECT, 0)
         );
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{PortedBlocks.EASTWEST, PortedBlocks.LEFTBEAM, PortedBlocks.RIGHTBEAM, PortedBlocks.DOWNCONNECT});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         Direction direction = context.getHorizontalDirection();
         return this.connectedSupportState(
            (BlockState)this.defaultBlockState().setValue(PortedBlocks.EASTWEST, direction.getAxis() == Axis.X), context.getLevel(), context.getClickedPos()
         );
      }

      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         return !direction.getAxis().isHorizontal() && direction != Direction.DOWN ? state : this.connectedSupportState(state, level, pos);
      }

      private BlockState connectedSupportState(BlockState state, BlockGetter level, BlockPos pos) {
         boolean eastWest = (Boolean)state.getValue(PortedBlocks.EASTWEST);
         Direction leftDirection = eastWest ? Direction.NORTH : Direction.WEST;
         Direction rightDirection = eastWest ? Direction.SOUTH : Direction.EAST;
         return (BlockState)((BlockState)((BlockState)state.setValue(
                  PortedBlocks.LEFTBEAM, this.connectsToSupport(level.getBlockState(pos.relative(leftDirection)))
               ))
               .setValue(PortedBlocks.RIGHTBEAM, this.connectsToSupport(level.getBlockState(pos.relative(rightDirection)))))
            .setValue(PortedBlocks.DOWNCONNECT, this.downConnection(level.getBlockState(pos.below())));
      }

      private boolean connectsToSupport(BlockState neighborState) {
         return neighborState.getBlock() instanceof PortedBlocks.SupportBlock;
      }

      private int downConnection(BlockState neighborState) {
         Block block = neighborState.getBlock();
         if (block == ModBlocks.THIN_STEEL_POLE.get()) {
            return 1;
         } else {
            return block == ModBlocks.THICK_STEEL_POLE.get() ? 2 : 0;
         }
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         boolean eastWest = state.getValue(PortedBlocks.EASTWEST);
         VoxelShape beam = eastWest ? PortedBlocks.SUPPORT_BEAM_EAST_WEST_SHAPE : PortedBlocks.SUPPORT_BEAM_NORTH_SOUTH_SHAPE;
         if (state.getValue(PortedBlocks.LEFTBEAM)) {
            beam = Shapes.or(beam, eastWest ? Block.box(5.0, 11.0, 0.0, 11.0, 16.0, 5.0) : Block.box(0.0, 11.0, 5.0, 5.0, 16.0, 11.0));
         }

         if (state.getValue(PortedBlocks.RIGHTBEAM)) {
            beam = Shapes.or(beam, eastWest ? Block.box(5.0, 11.0, 11.0, 11.0, 16.0, 16.0) : Block.box(11.0, 11.0, 5.0, 16.0, 16.0, 11.0));
         }

         if ((Integer)state.getValue(PortedBlocks.DOWNCONNECT) > 0) {
            beam = Shapes.or(beam, Block.box(6.0, 0.0, 6.0, 10.0, 11.0, 10.0));
         }

         return beam;
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.getShape(state, level, pos, context);
      }
   }

   public abstract static class DirectionalShapeBlock extends PortedBlocks.DirectionalPortBlock {
      private final VoxelShape[] shapes = new VoxelShape[Direction.values().length];
      private final boolean emptyCollision;

      protected DirectionalShapeBlock(Properties properties, VoxelShape northShape, boolean horizontalRotation, boolean emptyCollision) {
         super(properties);
         this.emptyCollision = emptyCollision;
         for (Direction facing : Direction.values()) {
            this.shapes[facing.ordinal()] = PortedBlocks.orientedNorthShape(northShape, facing, horizontalRotation);
         }
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.shapes[((Direction)state.getValue(FACING)).ordinal()];
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.emptyCollision ? Shapes.empty() : this.getShape(state, level, pos, context);
      }
   }

   /** Explicit light-only opt-in: structural pole heads also use DirectionalShapeBlock. */
   public abstract static class NativeLightBlock extends PortedBlocks.DirectionalShapeBlock implements NativeFluidloggedBlock {
      protected NativeLightBlock(Properties properties, VoxelShape northShape, boolean horizontalRotation, boolean emptyCollision) {
         super(properties.lightLevel(state -> Math.max(14, NativeFluidloggedBlock.light(state))), northShape, horizontalRotation, emptyCollision);
         this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false)
            .setValue(NativeFluidloggedBlock.FLUID_CELL, NativeFluidloggedBlock.Cell.EMPTY)
            .setValue(CatwalkStairsBlock.WATER_FLOW, 0));
      }

      @Override
      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(WATERLOGGED, NativeFluidloggedBlock.FLUID_CELL, CatwalkStairsBlock.WATER_FLOW);
      }

      @Override
      public FluidState getFluidState(BlockState state) {
         return StairWaterFlow.fluid(state);
      }

      @Override
      public boolean canPlaceLiquid(@javax.annotation.Nullable net.minecraft.world.entity.player.Player player, BlockGetter level, BlockPos pos, BlockState state, net.minecraft.world.level.material.Fluid fluid) {
         if (fluid == net.minecraft.world.level.material.Fluids.FLOWING_WATER)
            return !state.getValue(WATERLOGGED) && state.getValue(NativeFluidloggedBlock.FLUID_CELL) == NativeFluidloggedBlock.Cell.EMPTY;
         if (StairWaterFlow.active(state) && fluid != net.minecraft.world.level.material.Fluids.WATER) return false;
         return NativeFluidloggedBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
      }

      @Override
      public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
         if (fluid.getType() == net.minecraft.world.level.material.Fluids.FLOWING_WATER
               && canPlaceLiquid(null, level, pos, state, fluid.getType())) {
            if (!level.isClientSide()) {
               if (!level.setBlock(pos, StairWaterFlow.withFlow(state, fluid), 3)) return false;
               level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
            }
            return true;
         }
         if (!canPlaceLiquid(null, level, pos, state, fluid.getType())) return false;
         return NativeFluidloggedBlock.super.placeLiquid(level, pos, state.setValue(CatwalkStairsBlock.WATER_FLOW, 0), fluid);
      }

      @Override
      public net.minecraft.world.item.ItemStack pickupBlock(@javax.annotation.Nullable net.minecraft.world.entity.player.Player player, LevelAccessor level, BlockPos pos, BlockState state) {
         if (StairWaterFlow.active(state)) return net.minecraft.world.item.ItemStack.EMPTY;
         return NativeFluidloggedBlock.super.pickupBlock(player, level, pos, state.setValue(CatwalkStairsBlock.WATER_FLOW, 0));
      }

      @Override
      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         NativeFluidloggedBlock.schedule(state, level, pos);
         return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
      }

   }

   public static class OppositeFaceLightBlock extends PortedBlocks.NativeLightBlock {
      public OppositeFaceLightBlock(Properties properties, VoxelShape northShape) {
         super(properties, northShape, false, true);
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         return NativeFluidloggedBlock.placement(this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite()), context);
      }
   }

   public static class HorizontalLookLightBlock extends PortedBlocks.NativeLightBlock {
      public HorizontalLookLightBlock(Properties properties, VoxelShape northShape) {
         super(properties, northShape, true, true);
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         return NativeFluidloggedBlock.placement(this.defaultBlockState().setValue(FACING, context.getHorizontalDirection()), context);
      }
   }

   public static class SteelFramedWindowBlock extends PortedBlocks.DirectionalPortBlock {
      public SteelFramedWindowBlock(Properties properties) {
         super(properties);
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         Direction facing = context.getHorizontalDirection();
         Player player = context.getPlayer();
         if (player != null && Math.abs(player.getLookAngle().y) > 0.9) {
            facing = context.getNearestLookingDirection();
         } else {
            for (Direction direction : Direction.values()) {
               BlockState neighbor = context.getLevel().getBlockState(context.getClickedPos().relative(direction));
               if (neighbor.is(this)) {
                  facing = (Direction)neighbor.getValue(FACING);
                  break;
               }
            }
         }

         return (BlockState)this.defaultBlockState().setValue(FACING, facing);
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return PortedBlocks.centeredPlaneShape((Direction)state.getValue(FACING), 7.5, 8.5);
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.getShape(state, level, pos, context);
      }
   }

   public static class SurfaceMountedBlock extends PortedBlocks.DirectionalPortBlock {
      private final double min;
      private final double max;
      private final double thickness;
      private final boolean emptyCollision;

      public SurfaceMountedBlock(Properties properties, double min, double max, double thickness, boolean emptyCollision) {
         super(properties);
         this.min = min;
         this.max = max;
         this.thickness = thickness;
         this.emptyCollision = emptyCollision;
      }

      protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
         Direction facing = (Direction)state.getValue(FACING);
         BlockPos supportPos = pos.relative(facing.getOpposite());
         return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
      }

      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         return direction == ((Direction)state.getValue(FACING)).getOpposite() && !this.canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return PortedBlocks.facePlateShape((Direction)state.getValue(FACING), this.min, this.max, this.thickness);
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.emptyCollision ? Shapes.empty() : this.getShape(state, level, pos, context);
      }
   }

   public static class ExitSignBlock extends PortedBlocks.SurfaceMountedBlock {
      public ExitSignBlock(Properties properties) {
         super(properties, 1.0, 15.0, 1.0, true);
      }

      @Override
      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return switch (state.getValue(FACING)) {
            case NORTH -> Block.box(3.0, 7.0, 15.5, 13.0, 13.0, 16.0);
            case SOUTH, UP, DOWN -> Block.box(3.0, 7.0, 0.0, 13.0, 13.0, 0.5);
            case WEST -> Block.box(15.5, 7.0, 3.0, 16.0, 13.0, 13.0);
            case EAST -> Block.box(0.0, 7.0, 3.0, 0.5, 13.0, 13.0);
         };
      }
   }

   public static class TableBlock extends PortedBlocks.FixedShapeBlock {
      public TableBlock(Properties properties) {
         super(properties, PortedBlocks.TABLE_SHAPE);
      }
   }

   public static class VariantOnlyBlock extends Block {
      public VariantOnlyBlock(Properties properties) {
         super(properties);
         this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(PortedBlocks.VARIANT, 0));
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{PortedBlocks.VARIANT});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockPos pos = context.getClickedPos();
         int variant = Math.floorMod(pos.getX() * 31 + pos.getY() * 7 + pos.getZ(), 5);
         return (BlockState)this.defaultBlockState().setValue(PortedBlocks.VARIANT, variant);
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return PortedBlocks.RAISED_CATWALK_SHAPE;
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.getShape(state, level, pos, context);
      }
   }

   public static class VariantSlabBlock extends SlabBlock {
      public VariantSlabBlock(Properties properties) {
         super(properties);
         this.registerDefaultState((BlockState)this.defaultBlockState().setValue(PortedBlocks.TVARIANT, 0));
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{PortedBlocks.TVARIANT});
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState state = super.getStateForPlacement(context);
         int variant = Math.floorMod(context.getClickedPos().getX() * 31 + context.getClickedPos().getY() * 7 + context.getClickedPos().getZ(), 4);
         return state == null ? null : (BlockState)state.setValue(PortedBlocks.TVARIANT, variant);
      }
   }

   public static class VerticalSlabBlock extends PortedBlocks.HorizontalFacingBlock {
      public VerticalSlabBlock(Properties properties) {
         super(properties);
      }

      @Override
      public BlockState getStateForPlacement(BlockPlaceContext context) {
         // Facing identifies the occupied half; the structured face points back toward the player.
         return this.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, context.getHorizontalDirection());
      }

      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return PortedBlocks.horizontalHalfShape((Direction)state.getValue(PortedBlocks.HORIZONTAL_FACING));
      }

      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return this.getShape(state, level, pos, context);
      }
   }

   public static class SlidingDoorBlock extends DoorBlock {
      public static final MapCodec<PortedBlocks.SlidingDoorBlock> CODEC = simpleCodec(properties -> new PortedBlocks.SlidingDoorBlock(BlockSetType.IRON, properties));
      private static final int UPDATE_FLAGS = 10;
      private static final VoxelShape LOWER_CLOSED_MODEL_SHAPE = Shapes.or(
         Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0),
         Block.box(15.0, 0.0, 6.0, 16.0, 16.0, 7.0),
         Block.box(15.0, 0.0, 9.0, 16.0, 16.0, 10.0),
         Block.box(0.0, 0.0, 6.0, 15.0, 0.25, 7.0),
         Block.box(0.0, 0.0, 9.0, 15.0, 0.25, 10.0)
      );
      private static final VoxelShape UPPER_CLOSED_MODEL_SHAPE = Shapes.or(
         Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0),
         Block.box(15.0, 0.0, 6.0, 16.0, 16.0, 7.0),
         Block.box(15.0, 0.0, 9.0, 16.0, 16.0, 10.0),
         Block.box(0.0, 15.5, 6.0, 15.0, 16.0, 7.0),
         Block.box(0.0, 15.5, 9.0, 15.0, 16.0, 10.0)
      );
      private static final VoxelShape LOWER_OPEN_MODEL_SHAPE = Shapes.or(
         Block.box(14.75, 0.25, 7.0, 15.0, 16.0, 9.0),
         Block.box(15.0, 0.0, 6.0, 16.0, 16.0, 10.0),
         Block.box(0.0, 0.0, 6.0, 15.0, 0.25, 7.0),
         Block.box(0.0, 0.0, 9.0, 15.0, 0.25, 10.0),
         Block.box(0.0, 0.0, 7.0, 15.0, 0.0625, 9.0)
      );
      private static final VoxelShape UPPER_OPEN_MODEL_SHAPE = Shapes.or(
         Block.box(14.75, 0.0, 7.0, 16.0, 16.0, 9.0),
         Block.box(15.0, 0.0, 6.0, 16.0, 16.0, 7.0),
         Block.box(15.0, 0.0, 9.0, 16.0, 16.0, 10.0),
         Block.box(0.0, 15.5, 6.0, 15.0, 16.0, 7.0),
         Block.box(0.0, 15.75, 7.0, 15.0, 16.0, 9.0),
         Block.box(0.0, 15.5, 9.0, 15.0, 16.0, 10.0)
      );
      private static final VoxelShape LOWER_OPEN_COLLISION_SHAPE = Shapes.or(
         Block.box(14.75, 0.25, 7.0, 15.0, 16.0, 9.0), Block.box(15.0, 0.0, 6.0, 16.0, 16.0, 10.0)
      );
      private static final VoxelShape UPPER_OPEN_COLLISION_SHAPE = Shapes.or(
         Block.box(14.75, 0.0, 7.0, 16.0, 16.0, 9.0),
         Block.box(15.0, 0.0, 6.0, 16.0, 16.0, 7.0),
         Block.box(15.0, 0.0, 9.0, 16.0, 16.0, 10.0)
      );
      private static final VoxelShape[] LOWER_CLOSED_MODEL_SHAPES = rotatedShapes(LOWER_CLOSED_MODEL_SHAPE);
      private static final VoxelShape[] UPPER_CLOSED_MODEL_SHAPES = rotatedShapes(UPPER_CLOSED_MODEL_SHAPE);
      private static final VoxelShape[] LOWER_OPEN_MODEL_SHAPES = rotatedShapes(LOWER_OPEN_MODEL_SHAPE);
      private static final VoxelShape[] UPPER_OPEN_MODEL_SHAPES = rotatedShapes(UPPER_OPEN_MODEL_SHAPE);
      private static final VoxelShape[] LOWER_OPEN_COLLISION_SHAPES = rotatedShapes(LOWER_OPEN_COLLISION_SHAPE);
      private static final VoxelShape[] UPPER_OPEN_COLLISION_SHAPES = rotatedShapes(UPPER_OPEN_COLLISION_SHAPE);

      public SlidingDoorBlock(BlockSetType type, Properties properties) {
         super(type, properties);
         this.registerDefaultState(
            (BlockState)this.defaultBlockState().setValue(PortedBlocks.PAIR_SIDE, PortedBlocks.SlidingDoorPairSide.SINGLE)
         );
      }

      @Override
      public MapCodec<? extends DoorBlock> codec() {
         return CODEC;
      }

      @Override
      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(new Property[]{PortedBlocks.PAIR_SIDE});
      }

      @Override
      public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState state = super.getStateForPlacement(context);
         if (state == null) {
            return null;
         }

         PortedBlocks.SlidingDoorPair pair = this.findSinglePlacementPair(context.getLevel(), context.getClickedPos(), state);
         if (pair == null) {
            return setPairSide(state, PortedBlocks.SlidingDoorPairSide.SINGLE);
         }

         boolean powered = isDirectlyPowered(context.getLevel(), context.getClickedPos()) || isDirectlyPowered(context.getLevel(), pair.pos);
         boolean open = powered || (Boolean)pair.state.getValue(DoorBlock.OPEN);
         return (BlockState)((BlockState)setPairSide(state, pair.selfSide).setValue(DoorBlock.POWERED, powered)).setValue(DoorBlock.OPEN, open);
      }

      @Override
      public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
         super.setPlacedBy(level, pos, state, placer, stack);
         if (!level.isClientSide) {
            this.finishPairingAfterPlacement(level, pos, level.getBlockState(pos));
         }
      }

      @Override
      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
         if (!level.isClientSide) {
            BlockPos lowerPos = lowerPos(pos, state);
            BlockState lowerState = level.getBlockState(lowerPos);
            if (this.isLowerSlidingDoor(lowerState)) {
               PortedBlocks.SlidingDoorPair pair = this.storedPair(level, lowerPos, lowerState);
               boolean powered = isDirectlyPowered(level, lowerPos) || pair != null && isDirectlyPowered(level, pair.pos);
               boolean open = powered || !(Boolean)lowerState.getValue(DoorBlock.OPEN);
               this.setSynchronizedState(player, level, lowerPos, lowerState, pair, powered, open);
            }
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      }

      @Override
      public void setOpen(@Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {
         if (!state.is(this)) {
            return;
         }

         BlockPos lowerPos = lowerPos(pos, state);
         BlockState lowerState = level.getBlockState(lowerPos);
         if (!this.isLowerSlidingDoor(lowerState)) {
            return;
         }

         PortedBlocks.SlidingDoorPair pair = this.storedPair(level, lowerPos, lowerState);
         boolean powered = isDirectlyPowered(level, lowerPos) || pair != null && isDirectlyPowered(level, pair.pos);
         this.setSynchronizedState(entity, level, lowerPos, lowerState, pair, powered, powered || open);
      }

      @Override
      protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
         if (!level.isClientSide) {
            BlockPos lowerPos = lowerPos(pos, state);
            BlockState lowerState = level.getBlockState(lowerPos);
            if (this.isLowerSlidingDoor(lowerState)) {
               this.syncPoweredState(level, lowerPos, lowerState);
            }
         }
      }

      @Override
      protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
         BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
         if (!updated.is(this) || !direction.getAxis().isHorizontal()) {
            return updated;
         }

         PortedBlocks.SlidingDoorPairSide pairSide = updated.getValue(PortedBlocks.PAIR_SIDE);
         if (pairSide != PortedBlocks.SlidingDoorPairSide.SINGLE && direction == pairedDirection(updated)) {
            BlockPos lowerPos = lowerPos(pos, updated);
            BlockPos pairedLowerPos = lowerPos.relative(direction);
            BlockPos neighborLowerPos = lowerPos(neighborPos, neighborState);
            BlockState pairedLowerState = level.getBlockState(pairedLowerPos);
            if (!pairedLowerPos.equals(neighborLowerPos) || !this.isReciprocalPair(updated, pairedLowerState)) {
               return setPairSide(updated, PortedBlocks.SlidingDoorPairSide.SINGLE);
            }
         }

         return updated;
      }

      @Override
      protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
         if (!level.isClientSide && !state.is(newState.getBlock())) {
            this.unpairNeighborOnRemove(level, pos, state);
         }

         super.onRemove(state, level, pos, newState, movedByPiston);
      }

      @Override
      protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return state.getValue(OPEN) ? openModelShape(state) : closedModelShape(state);
      }

      @Override
      protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
         return state.getValue(OPEN) ? openCollisionShape(state) : closedModelShape(state);
      }

      private void finishPairingAfterPlacement(Level level, BlockPos lowerPos, BlockState lowerState) {
         if (!this.isLowerSlidingDoor(lowerState)) {
            return;
         }

         PortedBlocks.SlidingDoorPair pair = this.findSinglePlacementPair(level, lowerPos, lowerState);
         if (pair == null) {
            this.setDoorHalves(level, lowerPos, setPairSide(lowerState, PortedBlocks.SlidingDoorPairSide.SINGLE));
            this.syncPoweredState(level, lowerPos, level.getBlockState(lowerPos));
            return;
         }

         boolean powered = isDirectlyPowered(level, lowerPos) || isDirectlyPowered(level, pair.pos);
         boolean open = powered || (Boolean)lowerState.getValue(DoorBlock.OPEN) || (Boolean)pair.state.getValue(DoorBlock.OPEN);
         BlockState selfState = (BlockState)((BlockState)setPairSide(lowerState, pair.selfSide).setValue(DoorBlock.POWERED, powered))
            .setValue(DoorBlock.OPEN, open);
         BlockState pairState = (BlockState)((BlockState)setPairSide(pair.state, pair.pairSide).setValue(DoorBlock.POWERED, powered))
            .setValue(DoorBlock.OPEN, open);
         this.setDoorHalves(level, lowerPos, selfState);
         this.setDoorHalves(level, pair.pos, pairState);
      }

      private void syncPoweredState(Level level, BlockPos lowerPos, BlockState lowerState) {
         PortedBlocks.SlidingDoorPair pair = this.storedPair(level, lowerPos, lowerState);
         boolean powered = isDirectlyPowered(level, lowerPos) || pair != null && isDirectlyPowered(level, pair.pos);
         boolean powerChanged = powered != (Boolean)lowerState.getValue(DoorBlock.POWERED)
            || pair != null && powered != (Boolean)pair.state.getValue(DoorBlock.POWERED);

         if (pair == null && lowerState.getValue(PortedBlocks.PAIR_SIDE) != PortedBlocks.SlidingDoorPairSide.SINGLE) {
            BlockState singleState = (BlockState)((BlockState)setPairSide(lowerState, PortedBlocks.SlidingDoorPairSide.SINGLE)
                  .setValue(DoorBlock.POWERED, powered))
               .setValue(DoorBlock.OPEN, powered || (Boolean)lowerState.getValue(DoorBlock.OPEN));
            this.setDoorHalves(level, lowerPos, singleState);
            return;
         }

         if (powerChanged) {
            this.setSynchronizedState(null, level, lowerPos, lowerState, pair, powered, powered);
         }
      }

      private void setSynchronizedState(
         @Nullable Entity entity,
         Level level,
         BlockPos lowerPos,
         BlockState lowerState,
         @Nullable PortedBlocks.SlidingDoorPair pair,
         boolean powered,
         boolean open
      ) {
         boolean openChanged = open != (Boolean)lowerState.getValue(DoorBlock.OPEN)
            || pair != null && open != (Boolean)pair.state.getValue(DoorBlock.OPEN);
         BlockState selfState = (BlockState)((BlockState)lowerState.setValue(DoorBlock.POWERED, powered)).setValue(DoorBlock.OPEN, open);
         this.setDoorHalves(level, lowerPos, selfState);
         if (pair != null) {
            BlockState pairState = (BlockState)((BlockState)pair.state.setValue(DoorBlock.POWERED, powered)).setValue(DoorBlock.OPEN, open);
            this.setDoorHalves(level, pair.pos, pairState);
         }

         if (openChanged) {
            this.playSlidingDoorSound(entity, level, lowerPos, open);
            level.gameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, lowerPos);
         }
      }

      private void setDoorHalves(Level level, BlockPos lowerPos, BlockState lowerState) {
         BlockState normalizedLower = (BlockState)lowerState.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
         if (!level.getBlockState(lowerPos).equals(normalizedLower)) {
            level.setBlock(lowerPos, normalizedLower, UPDATE_FLAGS);
         }

         BlockPos upperPos = lowerPos.above();
         BlockState upperState = level.getBlockState(upperPos);
         if (upperState.is(this)) {
            BlockState normalizedUpper = (BlockState)normalizedLower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            if (!upperState.equals(normalizedUpper)) {
               level.setBlock(upperPos, normalizedUpper, UPDATE_FLAGS);
            }
         }
      }

      private PortedBlocks.SlidingDoorPair findSinglePlacementPair(LevelAccessor level, BlockPos lowerPos, BlockState lowerState) {
         PortedBlocks.SlidingDoorPair preferred = lowerState.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT
            ? this.singlePlacementPair(level, lowerPos, lowerState, lowerState.getValue(DoorBlock.FACING).getCounterClockWise())
            : this.singlePlacementPair(level, lowerPos, lowerState, lowerState.getValue(DoorBlock.FACING).getClockWise());
         if (preferred != null) {
            return preferred;
         }

         Direction fallbackDirection = lowerState.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT
            ? lowerState.getValue(DoorBlock.FACING).getClockWise()
            : lowerState.getValue(DoorBlock.FACING).getCounterClockWise();
         return this.singlePlacementPair(level, lowerPos, lowerState, fallbackDirection);
      }

      @Nullable
      private PortedBlocks.SlidingDoorPair singlePlacementPair(LevelAccessor level, BlockPos lowerPos, BlockState lowerState, Direction direction) {
         Direction facing = lowerState.getValue(DoorBlock.FACING);
         BlockPos pairPos = lowerPos.relative(direction);
         BlockState pairState = level.getBlockState(pairPos);
         if (!this.isLowerSlidingDoor(pairState)
            || pairState.getValue(DoorBlock.FACING) != facing
            || pairState.getValue(PortedBlocks.PAIR_SIDE) != PortedBlocks.SlidingDoorPairSide.SINGLE
            || !this.hasMatchingUpper(level, pairPos, pairState)) {
            return null;
         }

         PortedBlocks.SlidingDoorPairSide selfSide = direction == facing.getClockWise()
            ? PortedBlocks.SlidingDoorPairSide.LEFT
            : PortedBlocks.SlidingDoorPairSide.RIGHT;
         return new PortedBlocks.SlidingDoorPair(pairPos, pairState, selfSide, selfSide.opposite());
      }

      @Nullable
      private PortedBlocks.SlidingDoorPair storedPair(LevelAccessor level, BlockPos lowerPos, BlockState lowerState) {
         PortedBlocks.SlidingDoorPairSide selfSide = lowerState.getValue(PortedBlocks.PAIR_SIDE);
         if (selfSide == PortedBlocks.SlidingDoorPairSide.SINGLE) {
            return null;
         }

         BlockPos pairPos = lowerPos.relative(pairedDirection(lowerState));
         BlockState pairState = level.getBlockState(pairPos);
         if (!this.isReciprocalPair(lowerState, pairState)) {
            return null;
         }

         return new PortedBlocks.SlidingDoorPair(pairPos, pairState, selfSide, selfSide.opposite());
      }

      private boolean isReciprocalPair(BlockState selfState, BlockState pairState) {
         if (!this.isLowerSlidingDoor(pairState)
            || pairState.getValue(DoorBlock.FACING) != selfState.getValue(DoorBlock.FACING)
            || pairState.getValue(PortedBlocks.PAIR_SIDE) != selfState.getValue(PortedBlocks.PAIR_SIDE).opposite()) {
            return false;
         }

         return true;
      }

      private boolean isLowerSlidingDoor(BlockState state) {
         return state.is(this) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
      }

      private boolean hasMatchingUpper(LevelAccessor level, BlockPos lowerPos, BlockState lowerState) {
         BlockState upperState = level.getBlockState(lowerPos.above());
         return upperState.is(this)
            && upperState.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER
            && upperState.getValue(DoorBlock.FACING) == lowerState.getValue(DoorBlock.FACING);
      }

      private void unpairNeighborOnRemove(Level level, BlockPos pos, BlockState state) {
         PortedBlocks.SlidingDoorPairSide pairSide = state.getValue(PortedBlocks.PAIR_SIDE);
         if (pairSide == PortedBlocks.SlidingDoorPairSide.SINGLE) {
            return;
         }

         BlockPos lowerPos = lowerPos(pos, state);
         BlockPos pairPos = lowerPos.relative(pairedDirection(state));
         BlockState pairState = level.getBlockState(pairPos);
         if (!this.isReciprocalPair(state, pairState)) {
            return;
         }

         boolean powered = isDirectlyPowered(level, pairPos);
         BlockState singleState = (BlockState)((BlockState)setPairSide(pairState, PortedBlocks.SlidingDoorPairSide.SINGLE)
               .setValue(DoorBlock.POWERED, powered))
            .setValue(DoorBlock.OPEN, powered || (Boolean)pairState.getValue(DoorBlock.OPEN));
         this.setDoorHalves(level, pairPos, singleState);
      }

      private static BlockPos lowerPos(BlockPos pos, BlockState state) {
         return state.is(ModBlocks.METAL_SLIDING_DOOR.get()) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
      }

      private static boolean isDirectlyPowered(LevelAccessor level, BlockPos lowerPos) {
         return level.hasNeighborSignal(lowerPos) || level.hasNeighborSignal(lowerPos.above());
      }

      private static Direction pairedDirection(BlockState state) {
         PortedBlocks.SlidingDoorPairSide pairSide = state.getValue(PortedBlocks.PAIR_SIDE);
         Direction facing = state.getValue(DoorBlock.FACING);
         return pairSide == PortedBlocks.SlidingDoorPairSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
      }

      private static BlockState setPairSide(BlockState state, PortedBlocks.SlidingDoorPairSide pairSide) {
         BlockState pairedState = (BlockState)state.setValue(PortedBlocks.PAIR_SIDE, pairSide);
         return switch (pairSide) {
            case LEFT -> (BlockState)pairedState.setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
            case RIGHT -> (BlockState)pairedState.setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT);
            case SINGLE -> pairedState;
         };
      }

      private void playSlidingDoorSound(@Nullable Entity entity, Level level, BlockPos pos, boolean open) {
         level.playSound(
            entity, pos, open ? this.type().doorOpen() : this.type().doorClose(), SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F
         );
      }

      private static VoxelShape closedModelShape(BlockState state) {
         VoxelShape[] shapes = state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_CLOSED_MODEL_SHAPES : UPPER_CLOSED_MODEL_SHAPES;
         return shapes[modelRotationSteps(state)];
      }

      private static VoxelShape openModelShape(BlockState state) {
         VoxelShape[] shapes = state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_OPEN_MODEL_SHAPES : UPPER_OPEN_MODEL_SHAPES;
         return shapes[modelRotationSteps(state)];
      }

      private static VoxelShape openCollisionShape(BlockState state) {
         VoxelShape[] shapes = state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_OPEN_COLLISION_SHAPES : UPPER_OPEN_COLLISION_SHAPES;
         return shapes[modelRotationSteps(state)];
      }

      private static int modelRotationSteps(BlockState state) {
         Direction facing = state.getValue(FACING);
         DoorHingeSide hinge = state.getValue(HINGE);
         return switch (facing) {
            case SOUTH -> hinge == DoorHingeSide.LEFT ? 0 : 2;
            case WEST -> hinge == DoorHingeSide.LEFT ? 1 : 3;
            case NORTH -> hinge == DoorHingeSide.LEFT ? 2 : 0;
            case EAST -> hinge == DoorHingeSide.LEFT ? 3 : 1;
            default -> 0;
         };
      }

      private static VoxelShape[] rotatedShapes(VoxelShape shape) {
         VoxelShape clockwise = rotateClockwise(shape);
         VoxelShape opposite = rotateClockwise(clockwise);
         return new VoxelShape[]{shape, clockwise, opposite, rotateClockwise(opposite)};
      }

      private static VoxelShape rotateClockwise(VoxelShape shape) {
         VoxelShape rotated = Shapes.empty();

         for (AABB box : shape.toAabbs()) {
            rotated = Shapes.or(rotated, Shapes.create(1.0 - box.maxZ, box.minY, box.minX, 1.0 - box.minZ, box.maxY, box.maxX));
         }

         return rotated;
      }
   }

   private static final class SlidingDoorPair {
      private final BlockPos pos;
      private final BlockState state;
      private final PortedBlocks.SlidingDoorPairSide selfSide;
      private final PortedBlocks.SlidingDoorPairSide pairSide;

      private SlidingDoorPair(BlockPos pos, BlockState state, PortedBlocks.SlidingDoorPairSide selfSide, PortedBlocks.SlidingDoorPairSide pairSide) {
         this.pos = pos;
         this.state = state;
         this.selfSide = selfSide;
         this.pairSide = pairSide;
      }
   }
}
