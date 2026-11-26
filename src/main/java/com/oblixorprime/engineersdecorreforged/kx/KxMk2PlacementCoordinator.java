package com.oblixorprime.engineersdecorreforged.kx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Bounded, platform-neutral transaction bridge for the EXISTING K/X MK2 planning engine.
 *
 * It does not decide K/X topology, 24 rotations, anchors or 45-degree occupancy: those
 * remain owned by the existing MK2 core. The NeoForge implementation of WorldPort must
 * verify those exact core decisions before touching the world, and must preserve FULL
 * BlockState, BlockEntity and fluid contents in its snapshots.
 *
 * Contract: call on the authoritative server thread. Put/restore must not spawn drops,
 * consume items, emit irreversible events, or expose half-placed groups externally.
 * Snapshots are all taken before the first write. There is no Minecraft integration or
 * crash-proof atomicity until a real WorldPort and GameTests have been implemented.
 */
public final class KxMk2PlacementCoordinator {
    private KxMk2PlacementCoordinator() {}

    public record Cell(int x, int y, int z) {
        Cell offset(int dx, int dy, int dz) {
            return new Cell(x + dx, y + dy, z + dz);
        }
    }

    public record Piece<P>(Cell cell, P payload) {
        public Piece {
            Objects.requireNonNull(cell, "cell");
            Objects.requireNonNull(payload, "payload");
        }
    }

    /** Exact plan from the MK2 core. affectedBeyondCells includes remote brace endpoints. */
    public record Plan<P>(List<Piece<P>> pieces, Set<Cell> affectedBeyondCells) {
        public Plan {
            pieces = List.copyOf(Objects.requireNonNull(pieces, "pieces"));
            affectedBeyondCells = Set.copyOf(Objects.requireNonNull(affectedBeyondCells, "affectedBeyondCells"));
        }
    }

    public interface WorldPort<P, S> {
        boolean onServerThread();
        /** Validate reciprocal edges, 24-axis orientation and exact occupancy from MK2. */
        boolean validateGraphAndPlan(Plan<P> plan);
        /** Validate loaded chunk, world bounds, collision, replaceability and permission. */
        boolean canOccupy(Piece<P> piece);
        /** Complete before any writes: includes block entity contents and fluids. */
        S snapshot(Cell cell);
        /** Return false on rejection; must be rollback-safe even when returning false. */
        boolean put(Piece<P> piece);
        /** Restore exactly the snapshot, without drops or item consumption. */
        void restore(Cell cell, S before);
        /** Recompute K/X on all changed endpoints, including diagonal peers. */
        void refreshTopology(Set<Cell> affected);
        /** Explicitly send client/chunk model-data invalidations to all endpoints. */
        void notifyClients(Set<Cell> affected);
    }

    public enum Status {
        APPLIED,
        APPLIED_SYNC_PENDING,
        REJECTED_NO_WRITES,
        ROLLED_BACK,
        RECOVERY_REQUIRED
    }

    public record Result(Status status, String detail, List<Cell> touched,
                         List<Cell> restorationFailures, Set<Cell> affected) {
        public Result {
            Objects.requireNonNull(status, "status");
            detail = Objects.requireNonNullElse(detail, "");
            touched = List.copyOf(touched);
            restorationFailures = List.copyOf(restorationFailures);
            affected = Set.copyOf(affected);
        }
    }

    public static <P, S> Result apply(WorldPort<P, S> world, Plan<P> plan) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(plan, "plan");
        if (!world.onServerThread()) {
            return rejected("Not on the authoritative server thread");
        }
        if (plan.pieces().isEmpty()) {
            return rejected("Empty group");
        }
        Set<Cell> uniqueCells = new LinkedHashSet<>();
        for (Piece<P> piece : plan.pieces()) {
            if (!uniqueCells.add(piece.cell())) {
                return rejected("Duplicate occupied cell: " + piece.cell());
            }
        }

        // No world mutation before BOTH the graph check and all occupancy checks pass.
        Map<Cell, S> before = new LinkedHashMap<>();
        try {
            if (!world.validateGraphAndPlan(plan)) return rejected("Core graph / plan rejected");
            for (Piece<P> piece : plan.pieces()) {
                if (!world.canOccupy(piece)) return rejected("Cell rejected: " + piece.cell());
            }
            for (Piece<P> piece : plan.pieces()) {
                before.put(piece.cell(), Objects.requireNonNull(world.snapshot(piece.cell()), "snapshot"));
            }
        } catch (RuntimeException error) {
            return rejected("Preflight or snapshot failed: " + error);
        }

        List<Cell> touched = new ArrayList<>();
        RuntimeException failure = null;
        try {
            for (Piece<P> piece : plan.pieces()) {
                // An earlier successful write can trigger a change: check again before put.
                if (!world.canOccupy(piece)) throw new IllegalStateException("Changed during group: " + piece.cell());
                touched.add(piece.cell()); // Include even a partially failed put in rollback.
                if (!world.put(piece)) throw new IllegalStateException("Put rejected: " + piece.cell());
            }
        } catch (RuntimeException error) {
            failure = error;
        }

        Set<Cell> affected = affectedCells(uniqueCells, plan.affectedBeyondCells());
        if (failure != null) {
            List<Cell> failedRestore = new ArrayList<>();
            List<Cell> reversed = new ArrayList<>(touched);
            Collections.reverse(reversed);
            for (Cell cell : reversed) {
                try { world.restore(cell, before.get(cell)); }
                catch (RuntimeException error) { failedRestore.add(cell); }
            }
            boolean refreshed = refreshAndNotify(world, affected);
            Status status = failedRestore.isEmpty() && refreshed ? Status.ROLLED_BACK : Status.RECOVERY_REQUIRED;
            return new Result(status, "Failed placement: " + failure, touched, failedRestore, affected);
        }

        boolean refreshed = refreshAndNotify(world, affected);
        return new Result(refreshed ? Status.APPLIED : Status.APPLIED_SYNC_PENDING,
                refreshed ? "World group committed; sync calls accepted" : "World group committed; resync required",
                touched, List.of(), affected);
    }

    /** Includes six axial neighbors and twelve 45-degree edge-diagonal neighbors. */
    public static Set<Cell> affectedCells(Set<Cell> occupied, Set<Cell> extra) {
        LinkedHashSet<Cell> affected = new LinkedHashSet<>(occupied);
        affected.addAll(extra);
        for (Cell p : occupied) {
            for (int axis = 0; axis < 3; axis++) {
                for (int sign : new int[] {-1, 1}) {
                    int[] d = {0, 0, 0};
                    d[axis] = sign;
                    affected.add(p.offset(d[0], d[1], d[2]));
                }
            }
            for (int zeroAxis = 0; zeroAxis < 3; zeroAxis++) {
                for (int a : new int[] {-1, 1}) for (int b : new int[] {-1, 1}) {
                    int[] d = {a, a, a};
                    d[zeroAxis] = 0;
                    d[(zeroAxis + 1) % 3] = a;
                    d[(zeroAxis + 2) % 3] = b;
                    affected.add(p.offset(d[0], d[1], d[2]));
                }
            }
        }
        return Collections.unmodifiableSet(affected);
    }

    private static <P, S> boolean refreshAndNotify(WorldPort<P, S> world, Set<Cell> affected) {
        boolean refreshed = true;
        try { world.refreshTopology(affected); } catch (RuntimeException error) { refreshed = false; }
        try { world.notifyClients(affected); } catch (RuntimeException error) { refreshed = false; }
        return refreshed;
    }

    private static Result rejected(String message) {
        return new Result(Status.REJECTED_NO_WRITES, message, List.of(), List.of(), Set.of());
    }
}
