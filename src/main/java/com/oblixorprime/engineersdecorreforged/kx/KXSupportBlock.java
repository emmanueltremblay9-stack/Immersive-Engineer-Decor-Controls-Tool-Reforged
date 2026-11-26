package com.oblixorprime.engineersdecorreforged.kx;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Single K/X MK2 block; its canonical BlockEntity state selects the eight R3 forms. */
public final class KXSupportBlock extends BaseEntityBlock {
    public static final MapCodec<KXSupportBlock> CODEC = simpleCodec(KXSupportBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty ROLL = IntegerProperty.create("roll", 0, 3);

    // Common R3 I-profile collision core only. Arm collision is not yet qualified.
    private static final VoxelShape LOCAL_CORE = Shapes.or(
        Block.box(3.25, 0.0, 2.75, 12.75, 16.0, 4.2),
        Block.box(3.25, 0.0, 11.8, 12.75, 16.0, 13.25),
        Block.box(7.05, 0.0, 4.2, 8.95, 16.0, 11.8),
        Block.box(3.25, 0.0, 2.35, 3.8, 16.0, 2.75),
        Block.box(12.2, 0.0, 2.35, 12.75, 16.0, 2.75),
        Block.box(3.25, 0.0, 13.25, 3.8, 16.0, 13.65),
        Block.box(12.2, 0.0, 13.25, 12.75, 16.0, 13.65)
    ).optimize();
    private static final Map<KXOrientation, VoxelShape> ROTATED_CORE = new ConcurrentHashMap<>();

    public KXSupportBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP).setValue(ROLL, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROLL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction playerDirection = context.getPlayer() == null ? Direction.NORTH : context.getPlayer().getDirection();
        KXOrientation orientation = new KXOrientation(KXOrientation.Face.valueOf(face.name()),
            rollFor(face, playerDirection));
        return stateForOrientation(defaultBlockState(), orientation);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KXSupportBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        KXOrientation current = orientation(state);
        KXOrientation.Int3 axis = rotate(current.axis(), rotation);
        KXOrientation.Int3 right = rotate(current.right(), rotation);
        KXOrientation.Int3 depth = rotate(current.depth(), rotation);
        for (KXOrientation candidate : KXOrientation.all()) {
            if (candidate.axis().equals(axis) && candidate.right().equals(right)
                    && candidate.depth().equals(depth)) return stateForOrientation(state, candidate);
        }
        throw new IllegalStateException("Rotated K/X basis is not one of the 24 proper orientations");
    }

    public static KXOrientation orientation(BlockState state) {
        return new KXOrientation(KXOrientation.Face.valueOf(state.getValue(FACING).name()), state.getValue(ROLL));
    }

    public static BlockState stateForOrientation(BlockState state, KXOrientation orientation) {
        return state.setValue(FACING, Direction.valueOf(orientation.facing().name()))
            .setValue(ROLL, orientation.roll());
    }

    private static int rollFor(Direction facing, Direction playerDirection) {
        KXOrientation.Face face = KXOrientation.Face.valueOf(facing.name());
        KXOrientation.Int3 look = vector(playerDirection);
        int bestRoll = 0, bestScore = Integer.MIN_VALUE;
        for (int roll = 0; roll < 4; roll++) {
            int score = new KXOrientation(face, roll).right().dot(look);
            if (score > bestScore) { bestScore = score; bestRoll = roll; }
        }
        return bestRoll;
    }

    private static KXOrientation.Int3 rotate(KXOrientation.Int3 vector, Rotation rotation) {
        return vector(rotation.rotate(direction(vector)));
    }

    private static Direction direction(KXOrientation.Int3 vector) {
        if (vector.x() == 1 && vector.y() == 0 && vector.z() == 0) return Direction.EAST;
        if (vector.x() == -1 && vector.y() == 0 && vector.z() == 0) return Direction.WEST;
        if (vector.x() == 0 && vector.y() == 1 && vector.z() == 0) return Direction.UP;
        if (vector.x() == 0 && vector.y() == -1 && vector.z() == 0) return Direction.DOWN;
        if (vector.x() == 0 && vector.y() == 0 && vector.z() == 1) return Direction.SOUTH;
        if (vector.x() == 0 && vector.y() == 0 && vector.z() == -1) return Direction.NORTH;
        throw new IllegalArgumentException("K/X basis vector is not cardinal: " + vector);
    }

    private static KXOrientation.Int3 vector(Direction direction) {
        return new KXOrientation.Int3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private static VoxelShape shapeFor(BlockState state) {
        return ROTATED_CORE.computeIfAbsent(orientation(state), KXSupportBlock::rotateCore);
    }

    private static VoxelShape rotateCore(KXOrientation orientation) {
        VoxelShape result = Shapes.empty();
        for (AABB box : LOCAL_CORE.toAabbs()) {
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
            for (int ix = 0; ix < 2; ix++) for (int iy = 0; iy < 2; iy++) for (int iz = 0; iz < 2; iz++) {
                KXOrientation.Vec3 point = orientation.toWorld(new KXOrientation.Vec3(
                    (ix == 0 ? box.minX : box.maxX) * 16.0,
                    (iy == 0 ? box.minY : box.maxY) * 16.0,
                    (iz == 0 ? box.minZ : box.maxZ) * 16.0));
                minX = Math.min(minX, point.x()); maxX = Math.max(maxX, point.x());
                minY = Math.min(minY, point.y()); maxY = Math.max(maxY, point.y());
                minZ = Math.min(minZ, point.z()); maxZ = Math.max(maxZ, point.z());
            }
            result = Shapes.or(result, Shapes.box(minX / 16.0, minY / 16.0, minZ / 16.0,
                maxX / 16.0, maxY / 16.0, maxZ / 16.0));
        }
        return result.optimize();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(replacement.getBlock())) {
            KXSupportBlockEntity.detachLoadedPeers(level, pos);
        }
        super.onRemove(state, level, pos, replacement, movedByPiston);
    }
}
