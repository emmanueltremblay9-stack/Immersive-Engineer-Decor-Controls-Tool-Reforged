package com.oblixorprime.engineersdecorreforged.kx;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Canonical persisted K/X state for the single kx_mk2_support block.
 *
 * A connection is stored at BOTH endpoints. It is considered real only when
 * the peer is loaded, is still a K/X block, has the same 24-way orientation,
 * and contains the exact inverse record. Nearby blocks and one-sided NBT never
 * create arms. Source and target slots/coordinates are always supplied explicitly;
 * this class does not infer a port-pairing rule.
 *
 * This is a state/registry adapter, not the player interaction or long-span
 * segment-placement adapter. Those remain blocked until the missing pairing and
 * per-cell placement contract is established.
 */
public final class KXSupportBlockEntity extends BlockEntity {
    private static final String ROOT = "KxMk2";
    private static final int DATA_VERSION = 1;
    private static final int MAX_CONNECTIONS = 4;

    /** Renderer-facing projection. No R3 model or atlas bytes are embedded here. */
    public static final ModelProperty<KXTopology.Selection> MODEL_SELECTION = new ModelProperty<>();

    public record PortSpec(KXTopology.Slot slot, double x, double y, double z, String plane) {
        public PortSpec {
            Objects.requireNonNull(slot);
            Objects.requireNonNull(plane);
            KXTopology.validatePortGeometry(new KXTopology.Port(slot, x, y, z, plane, true));
        }
    }

    /** Local-to-peer edge plus the exact caller-supplied port at each endpoint. */
    public record Connection(BlockPos peerPos, KXTopology.Slot localSlot, KXTopology.Slot peerSlot,
            String plane, double localX, double localY, double localZ,
            double peerX, double peerY, double peerZ) {
        public Connection {
            peerPos = Objects.requireNonNull(peerPos).immutable();
            Objects.requireNonNull(localSlot);
            Objects.requireNonNull(peerSlot);
            Objects.requireNonNull(plane);
            KXTopology.validatePortGeometry(new KXTopology.Port(localSlot,
                localX, localY, localZ, plane, true));
            KXTopology.validatePortGeometry(new KXTopology.Port(peerSlot,
                peerX, peerY, peerZ, plane, true));
        }
    }

    public record LinkResult(boolean applied, String reason) {
        private static LinkResult accepted() { return new LinkResult(true, "explicit reciprocal edge recorded"); }
        private static LinkResult rejected(String reason) { return new LinkResult(false, reason); }
    }

    private final EnumMap<KXTopology.Slot, Connection> connections = new EnumMap<>(KXTopology.Slot.class);
    private CompoundTag unknownPayload;
    private boolean dataValid = true;
    private KXTopology.Selection clientSelection;

    public KXSupportBlockEntity(BlockPos pos, BlockState state) {
        super(KXModule.KX_MK2_SUPPORT_ENTITY.get(), pos, state);
    }

    public int connectionCount() {
        return connections.size();
    }

    public Map<KXTopology.Slot, Connection> connectionSnapshot() {
        return Map.copyOf(connections);
    }

    public KXTopology.Selection selection() {
        if (!dataValid) return cleanSelection("unknown or invalid persisted K/X schema");
        if (connections.isEmpty()) {
            return KXTopology.select(List.of(), "front", hasDownAnchor());
        }
        String plane = null;
        for (Connection connection : connections.values()) {
            if (plane == null) plane = connection.plane();
            else if (!plane.equals(connection.plane())) {
                return cleanSelection("mixed flange planes are not a single MK2 topology");
            }
        }
        List<KXTopology.Port> valid = verifiedPorts();
        try {
            return KXTopology.select(valid, Objects.requireNonNullElse(plane, "front"), false);
        } catch (IllegalArgumentException invalidTopology) {
            return cleanSelection("invalid reciprocal topology: " + invalidTopology.getMessage());
        }
    }

    private static KXTopology.Selection cleanSelection(String reason) {
        return new KXTopology.Selection(KXTopology.Mode.CLEAN, Set.of(), Set.of(), false,
            Map.of(), "clean", reason);
    }

    private boolean hasDownAnchor() {
        if (level == null || level.isClientSide) return false;
        BlockPos below = worldPosition.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    private List<KXTopology.Port> verifiedPorts() {
        var result = new ArrayList<KXTopology.Port>();
        if (level == null || level.isClientSide || !dataValid) return result;
        for (Connection connection : connections.values()) {
            if (isReciprocal(connection)) {
                result.add(new KXTopology.Port(connection.localSlot(), connection.localX(),
                    connection.localY(), connection.localZ(), connection.plane(), true));
            }
        }
        return result;
    }

    private boolean isReciprocal(Connection connection) {
        if (level == null || !level.hasChunkAt(connection.peerPos())) return false;
        if (!level.getBlockState(connection.peerPos()).is(KXModule.KX_MK2_SUPPORT.get())) return false;
        BlockEntity remote = level.getBlockEntity(connection.peerPos());
        if (!(remote instanceof KXSupportBlockEntity other) || !other.dataValid) return false;
        KXOrientation orientation = KXSupportBlock.orientation(getBlockState());
        if (!orientation.equals(KXSupportBlock.orientation(other.getBlockState()))) return false;
        if (!adjacent45(worldPosition, connection.peerPos(), orientation)) return false;
        Connection inverse = other.connections.get(connection.peerSlot());
        return inverseOf(inverse, worldPosition, connection);
    }

    private static boolean inverseOf(Connection inverse, BlockPos localPos, Connection forward) {
        return inverse != null
            && inverse.peerPos().equals(localPos)
            && inverse.localSlot() == forward.peerSlot()
            && inverse.peerSlot() == forward.localSlot()
            && inverse.plane().equals(forward.plane())
            && same(inverse.localX(), forward.peerX())
            && same(inverse.localY(), forward.peerY())
            && same(inverse.localZ(), forward.peerZ())
            && same(inverse.peerX(), forward.localX())
            && same(inverse.peerY(), forward.localY())
            && same(inverse.peerZ(), forward.localZ());
    }

    private static boolean same(double a, double b) {
        return Math.abs(a - b) <= 1.0e-7;
    }

    private boolean canAdd(KXTopology.Slot slot, String plane) {
        if (!dataValid || connections.size() >= MAX_CONNECTIONS || connections.containsKey(slot)) return false;
        return connections.values().stream().allMatch(existing -> existing.plane().equals(plane));
    }

    /**
     * Record a caller-verified reciprocal contact between two already placed K/X
     * blocks. Both endpoint slots and both attachment coordinates are mandatory.
     * No slot mapping, intermediate block, item cost, or gameplay behavior is inferred.
     */
    public static LinkResult recordExplicitConnection(ServerLevel level,
            BlockPos fromPos, PortSpec fromPort, BlockPos toPos, PortSpec toPort) {
        Objects.requireNonNull(level);
        Objects.requireNonNull(fromPos);
        Objects.requireNonNull(toPos);
        Objects.requireNonNull(fromPort);
        Objects.requireNonNull(toPort);
        if (!level.getServer().isSameThread()) return LinkResult.rejected("must run on authoritative server thread");
        if (fromPos.equals(toPos)) return LinkResult.rejected("a connection needs two distinct blocks");
        if (!fromPort.plane().equals(toPort.plane())) return LinkResult.rejected("source and target flange planes differ");
        if (!level.hasChunkAt(fromPos) || !level.hasChunkAt(toPos)) {
            return LinkResult.rejected("both endpoint chunks must already be loaded; no chunk is force-loaded");
        }
        BlockState fromState = level.getBlockState(fromPos);
        BlockState toState = level.getBlockState(toPos);
        if (!fromState.is(KXModule.KX_MK2_SUPPORT.get()) || !toState.is(KXModule.KX_MK2_SUPPORT.get())) {
            return LinkResult.rejected("both endpoints must be the registered K/X MK2 block");
        }
        KXOrientation orientation = KXSupportBlock.orientation(fromState);
        if (!orientation.equals(KXSupportBlock.orientation(toState))) {
            return LinkResult.rejected("endpoint orientations differ");
        }
        if (!adjacent45(fromPos, toPos, orientation)) {
            return LinkResult.rejected("a stored edge must join immediately adjacent cells on the 45-degree brace plane");
        }
        if (!(level.getBlockEntity(fromPos) instanceof KXSupportBlockEntity from)
                || !(level.getBlockEntity(toPos) instanceof KXSupportBlockEntity to)) {
            return LinkResult.rejected("endpoint BlockEntity is missing");
        }
        if (!from.canAdd(fromPort.slot(), fromPort.plane()) || !to.canAdd(toPort.slot(), toPort.plane())) {
            return LinkResult.rejected("port is already occupied, plane differs, or persisted schema is unknown");
        }

        Connection forward = new Connection(toPos, fromPort.slot(), toPort.slot(), fromPort.plane(),
            fromPort.x(), fromPort.y(), fromPort.z(), toPort.x(), toPort.y(), toPort.z());
        Connection reverse = new Connection(fromPos, toPort.slot(), fromPort.slot(), toPort.plane(),
            toPort.x(), toPort.y(), toPort.z(), fromPort.x(), fromPort.y(), fromPort.z());

        from.connections.put(fromPort.slot(), forward);
        to.connections.put(toPort.slot(), reverse);
        if (!from.topologyValid() || !to.topologyValid()) {
            from.connections.remove(fromPort.slot());
            to.connections.remove(toPort.slot());
            return LinkResult.rejected("explicit ports do not produce a valid K/X topology");
        }
        from.refreshLoadedPeers();
        to.refreshLoadedPeers();
        return LinkResult.accepted();
    }

    private static boolean adjacent45(BlockPos from, BlockPos to, KXOrientation orientation) {
        KXOrientation.Int3 delta = new KXOrientation.Int3(
            to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
        int across = delta.dot(orientation.right());
        int up = delta.dot(orientation.axis());
        int depth = delta.dot(orientation.depth());
        return depth == 0 && Math.abs(across) == 1 && Math.abs(up) == 1;
    }

    private boolean topologyValid() {
        if (!dataValid || connections.size() > MAX_CONNECTIONS) return false;
        if (connections.isEmpty()) return true;
        String plane = connections.values().iterator().next().plane();
        if (connections.values().stream().anyMatch(c -> !plane.equals(c.plane()))) return false;
        try {
            KXTopology.select(verifiedPorts(), plane, false);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private void refreshLoadedPeers() {
        refreshVisualState();
        if (level == null || level.isClientSide) return;
        for (Connection connection : connections.values()) {
            if (!level.hasChunkAt(connection.peerPos())) continue;
            if (level.getBlockEntity(connection.peerPos()) instanceof KXSupportBlockEntity peer) {
                peer.refreshVisualState();
            }
        }
    }

    private void refreshVisualState() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    private void removeInverse(BlockPos localPos, Connection forward) {
        Connection inverse = connections.get(forward.peerSlot());
        if (inverseOf(inverse, localPos, forward)) {
            connections.remove(forward.peerSlot());
            refreshLoadedPeers();
        }
    }

    /** Called only for actual block replacement/removal, never for chunk unload. */
    public static void detachLoadedPeers(Level level, BlockPos pos) {
        if (level.isClientSide || !level.hasChunkAt(pos)) return;
        if (!(level.getBlockEntity(pos) instanceof KXSupportBlockEntity local)) return;
        List<Connection> snapshot = List.copyOf(local.connections.values());
        for (Connection connection : snapshot) {
            if (!level.hasChunkAt(connection.peerPos())) continue;
            if (level.getBlockEntity(connection.peerPos()) instanceof KXSupportBlockEntity peer) {
                peer.removeInverse(pos, connection);
            }
        }
        local.connections.clear();
        local.refreshVisualState();
    }

    /**
     * Chunk load callbacks may fire before a chunk is promoted to FULL. Defer Level
     * reads to the next server task and use getChunkNow (never force-load a chunk).
     */
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        net.minecraft.world.level.ChunkPos pos = event.getChunk().getPos();
        level.getServer().execute(() -> {
            LevelChunk loaded = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (loaded == null) return;
            for (BlockEntity blockEntity : loaded.getBlockEntities().values()) {
                if (!(blockEntity instanceof KXSupportBlockEntity local)) continue;
                local.refreshVisualState();
                for (Connection connection : local.connections.values()) {
                    if (!level.hasChunkAt(connection.peerPos())) continue;
                    if (level.getBlockEntity(connection.peerPos()) instanceof KXSupportBlockEntity peer) {
                        peer.refreshVisualState();
                    }
                }
            }
        });
    }

    /** Refresh surviving remote endpoints after the unloading chunk leaves the loaded set. */
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        var remotePeers = new HashSet<BlockPos>();
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (!(blockEntity instanceof KXSupportBlockEntity local)) continue;
            for (Connection connection : local.connections.values()) {
                if (!new net.minecraft.world.level.ChunkPos(connection.peerPos()).equals(chunk.getPos())) {
                    remotePeers.add(connection.peerPos());
                }
            }
        }
        level.getServer().execute(() -> {
            for (BlockPos peerPos : remotePeers) {
                if (!level.hasChunkAt(peerPos)) continue;
                if (level.getBlockEntity(peerPos) instanceof KXSupportBlockEntity peer) {
                    peer.refreshVisualState();
                }
            }
        });
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unknownPayload != null) {
            tag.put(ROOT, unknownPayload.copy());
            return;
        }
        CompoundTag root = new CompoundTag();
        root.putInt("Version", DATA_VERSION);
        ListTag list = new ListTag();
        for (Connection connection : connections.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Peer", connection.peerPos().asLong());
            entry.putString("LocalSlot", connection.localSlot().name());
            entry.putString("PeerSlot", connection.peerSlot().name());
            entry.putString("Plane", connection.plane());
            entry.putDouble("LocalX", connection.localX());
            entry.putDouble("LocalY", connection.localY());
            entry.putDouble("LocalZ", connection.localZ());
            entry.putDouble("PeerX", connection.peerX());
            entry.putDouble("PeerY", connection.peerY());
            entry.putDouble("PeerZ", connection.peerZ());
            list.add(entry);
        }
        root.put("Connections", list);
        tag.put(ROOT, root);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        connections.clear();
        unknownPayload = null;
        dataValid = true;
        clientSelection = null;
        if (tag.contains(ROOT, Tag.TAG_COMPOUND)) {
            CompoundTag root = tag.getCompound(ROOT);
            if (root.getInt("Version") != DATA_VERSION || !root.contains("Connections", Tag.TAG_LIST)) {
                invalidate(root);
            } else {
                ListTag list = root.getList("Connections", Tag.TAG_COMPOUND);
                if (list.size() > MAX_CONNECTIONS) {
                    invalidate(root);
                } else {
                    try {
                        for (int i = 0; i < list.size(); i++) {
                            CompoundTag entry = list.getCompound(i);
                            Connection connection = new Connection(
                                BlockPos.of(entry.getLong("Peer")),
                                KXTopology.Slot.valueOf(entry.getString("LocalSlot")),
                                KXTopology.Slot.valueOf(entry.getString("PeerSlot")),
                                entry.getString("Plane"),
                                entry.getDouble("LocalX"), entry.getDouble("LocalY"), entry.getDouble("LocalZ"),
                                entry.getDouble("PeerX"), entry.getDouble("PeerY"), entry.getDouble("PeerZ"));
                            if (connections.putIfAbsent(connection.localSlot(), connection) != null) {
                                throw new IllegalArgumentException("duplicate local port");
                            }
                        }
                    } catch (RuntimeException invalid) {
                        invalidate(root);
                    }
                }
            }
        }
        if (tag.contains("RenderState", Tag.TAG_COMPOUND)) {
            clientSelection = readRenderState(tag.getCompound("RenderState"));
        }
        if (level != null && level.isClientSide) requestModelDataUpdate();
    }

    private void invalidate(CompoundTag root) {
        connections.clear();
        unknownPayload = root.copy();
        dataValid = false;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        writeRenderState(tag, selection());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public ModelData getModelData() {
        KXTopology.Selection current = (clientSelection != null
            && (level == null || level.isClientSide)) ? clientSelection : selection();
        return current == null ? ModelData.EMPTY
            : ModelData.builder().with(MODEL_SELECTION, current).build();
    }

    private static void writeRenderState(CompoundTag tag, KXTopology.Selection selection) {
        CompoundTag render = new CompoundTag();
        render.putString("Mode", selection.mode().name());
        render.putString("Asset", selection.assetKey());
        int arms = 0;
        for (KXTopology.Slot slot : selection.arms()) arms |= 1 << slot.ordinal();
        render.putInt("Arms", arms);
        int sides = 0;
        for (KXTopology.Side side : selection.sidePlates()) sides |= 1 << side.ordinal();
        render.putInt("Sides", sides);
        render.putBoolean("Center", selection.centerPlate());
        if (selection.sidePlateHeights().containsKey(KXTopology.Side.LEFT)) {
            render.putBoolean("HasLeftHeight", true);
            render.putDouble("LeftHeight", selection.sidePlateHeights().get(KXTopology.Side.LEFT));
        }
        if (selection.sidePlateHeights().containsKey(KXTopology.Side.RIGHT)) {
            render.putBoolean("HasRightHeight", true);
            render.putDouble("RightHeight", selection.sidePlateHeights().get(KXTopology.Side.RIGHT));
        }
        tag.put("RenderState", render);
    }

    private static KXTopology.Selection readRenderState(CompoundTag render) {
        try {
            KXTopology.Mode mode = KXTopology.Mode.valueOf(render.getString("Mode"));
            EnumSet<KXTopology.Slot> arms = EnumSet.noneOf(KXTopology.Slot.class);
            for (KXTopology.Slot slot : KXTopology.Slot.values()) {
                if ((render.getInt("Arms") & (1 << slot.ordinal())) != 0) arms.add(slot);
            }
            EnumSet<KXTopology.Side> sides = EnumSet.noneOf(KXTopology.Side.class);
            for (KXTopology.Side side : KXTopology.Side.values()) {
                if ((render.getInt("Sides") & (1 << side.ordinal())) != 0) sides.add(side);
            }
            var heights = new EnumMap<KXTopology.Side, Double>(KXTopology.Side.class);
            if (render.getBoolean("HasLeftHeight")) heights.put(KXTopology.Side.LEFT, render.getDouble("LeftHeight"));
            if (render.getBoolean("HasRightHeight")) heights.put(KXTopology.Side.RIGHT, render.getDouble("RightHeight"));
            return new KXTopology.Selection(mode, arms, sides, render.getBoolean("Center"),
                heights, render.getString("Asset"), "server-synchronized canonical topology");
        } catch (RuntimeException invalid) {
            return null;
        }
    }
}
