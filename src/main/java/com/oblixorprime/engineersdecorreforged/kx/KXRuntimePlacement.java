package com.oblixorprime.engineersdecorreforged.kx;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Connects the recovered MK2 engine to the bridge's bounded invalidation footprint.
 * The engine remains the sole placement/economy transaction owner. Calling the
 * generic coordinator's apply as a second transaction would bypass item charging.
 * This is a common-code seam, not a registered Minecraft block or level adapter.
 */
public final class KXRuntimePlacement {
    private KXRuntimePlacement() {}

    public interface SyncPort {
        /** Must not force-load chunks; defer unloaded endpoints in the game adapter. */
        void refreshTopology(Set<KXPlacement.Cell> affected);
        void notifyClients(Set<KXPlacement.Cell> affected);
    }

    public enum SyncState { NOT_NEEDED, COMPLETED, PENDING }
    public record Result(KXPlacement.Result placement, SyncState sync,
                         Set<KXPlacement.Cell> affected) {
        public Result { affected = Set.copyOf(affected); }
    }

    public static Result apply(KXPlacement.World world, KXPlacement.Plan plan, SyncPort sync) {
        Objects.requireNonNull(world); Objects.requireNonNull(plan); Objects.requireNonNull(sync);
        // Plan's public record constructor is not a validation boundary. Rebuild
        // from authoritative spans so injected cells/slots cannot reach the world.
        try {
            if (!plan.equals(KXPlacement.plan(plan.orientation(), plan.plane(), plan.spans())))
                return rejected("plan differs from canonical MK2 occupancy");
        } catch (IllegalArgumentException | NullPointerException invalid) {
            return rejected("invalid MK2 plan");
        }
        KXPlacement.Result placed = KXPlacement.apply(world, plan);
        if (placed.status() == KXPlacement.Status.REJECTED)
            return new Result(placed, SyncState.NOT_NEEDED, Set.of());

        Set<KXPlacement.Cell> affected = affectedCells(plan);
        boolean complete = true;
        try { sync.refreshTopology(affected); } catch (RuntimeException failure) { complete = false; }
        try { sync.notifyClients(affected); } catch (RuntimeException failure) { complete = false; }
        // Never retry placement or item consumption merely because sync failed.
        // Rollback/recovery status remains visible independently of sync status.
        return new Result(placed, complete ? SyncState.COMPLETED : SyncState.PENDING, affected);
    }

    public static Set<KXPlacement.Cell> affectedCells(KXPlacement.Plan plan) {
        var occupied = new LinkedHashSet<KxMk2PlacementCoordinator.Cell>();
        var endpoints = new LinkedHashSet<KxMk2PlacementCoordinator.Cell>();
        for (KXPlacement.Cell cell : plan.segments().keySet()) occupied.add(toBridge(cell));
        for (KXPlacement.Span span : plan.spans()) {
            endpoints.add(toBridge(span.fromPillar())); endpoints.add(toBridge(span.toPillar()));
        }
        var result = new LinkedHashSet<KXPlacement.Cell>();
        for (var cell : KxMk2PlacementCoordinator.affectedCells(occupied, endpoints))
            result.add(new KXPlacement.Cell(cell.x(), cell.y(), cell.z()));
        return Set.copyOf(result);
    }

    private static KxMk2PlacementCoordinator.Cell toBridge(KXPlacement.Cell cell) {
        return new KxMk2PlacementCoordinator.Cell(cell.x(), cell.y(), cell.z());
    }
    private static Result rejected(String detail) {
        return new Result(new KXPlacement.Result(KXPlacement.Status.REJECTED, 0, 0, detail),
                SyncState.NOT_NEEDED, Set.of());
    }
}
