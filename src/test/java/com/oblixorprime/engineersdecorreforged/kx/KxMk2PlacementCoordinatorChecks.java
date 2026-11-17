package com.oblixorprime.engineersdecorreforged.kx;

import com.oblixorprime.engineersdecorreforged.kx.KxMk2PlacementCoordinator.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Run with plain javac/java 21; NO GameTest/NeoForge claims. */
public final class KxMk2PlacementCoordinatorChecks {
    private static int assertions;
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    private static final Cell A = new Cell(0, 0, 0);
    private static final Cell B = new Cell(1, 1, 0);
    private static final Cell C = new Cell(2, 2, 0);

    private static class FakeWorld implements WorldPort<String, String> {
        final Map<Cell, String> blocks = new HashMap<>();
        final List<Cell> puts = new ArrayList<>();
        final List<Cell> restores = new ArrayList<>();
        final Set<Cell> rejected = new HashSet<>();
        boolean server = true, valid = true, failNotify, failRestore;
        int failAtPut = -1, preflightCount, refreshCount, notifyCount;
        Set<Cell> lastAffected = Set.of();
        @Override public boolean onServerThread() { return server; }
        @Override public boolean validateGraphAndPlan(Plan<String> plan) { return valid; }
        @Override public boolean canOccupy(Piece<String> piece) {
            preflightCount++;
            return !rejected.contains(piece.cell());
        }
        @Override public String snapshot(Cell cell) { return blocks.getOrDefault(cell, "AIR"); }
        @Override public boolean put(Piece<String> piece) {
            puts.add(piece.cell());
            blocks.put(piece.cell(), piece.payload()); // Model partially successful put before false.
            return puts.size() != failAtPut;
        }
        @Override public void restore(Cell cell, String before) {
            restores.add(cell);
            if (failRestore && cell.equals(B)) throw new IllegalStateException("forced restore error");
            blocks.put(cell, before);
        }
        @Override public void refreshTopology(Set<Cell> affected) {
            refreshCount++; lastAffected = affected;
        }
        @Override public void notifyClients(Set<Cell> affected) {
            notifyCount++;
            if (failNotify) throw new IllegalStateException("forced sync error");
        }
    }

    private static Plan<String> plan(Cell... cells) {
        List<Piece<String>> pieces = new ArrayList<>();
        for (Cell cell : cells) pieces.add(new Piece<>(cell, "BEAM"));
        return new Plan<>(pieces, Set.of(new Cell(10, 2, 7)));
    }

    public static void main(String[] args) {
        FakeWorld w = new FakeWorld();
        Result r = KxMk2PlacementCoordinator.apply(w, plan(A, B));
        check(r.status() == Status.APPLIED, "success status");
        check(w.puts.equals(List.of(A, B)), "two writes");
        check(w.restores.isEmpty(), "no restores success");
        check(w.refreshCount == 1 && w.notifyCount == 1, "refresh and client sync");
        check(w.lastAffected.contains(new Cell(10, 2, 7)), "remote affected endpoints");
        check(w.lastAffected.contains(new Cell(1, 1, 0)), "diagonal invalidation");
        check(KxMk2PlacementCoordinator.affectedCells(Set.of(A), Set.of()).size() == 19, "6 axial + 12 diagonal + center");
        check(KxMk2PlacementCoordinator.affectedCells(Set.of(A), Set.of()).contains(new Cell(-1, 0, 1)), "negative 45 degree peer");
        check(!KxMk2PlacementCoordinator.affectedCells(Set.of(A), Set.of()).contains(new Cell(1, 1, 1)), "no unrelated corner");

        w = new FakeWorld(); w.rejected.add(B);
        r = KxMk2PlacementCoordinator.apply(w, plan(A, B));
        check(r.status() == Status.REJECTED_NO_WRITES, "preflight rejection");
        check(w.puts.isEmpty() && w.restores.isEmpty(), "preflight cannot edit world");
        check(w.notifyCount == 0, "no sync on preflight reject");

        w = new FakeWorld(); w.valid = false;
        r = KxMk2PlacementCoordinator.apply(w, plan(A));
        check(r.status() == Status.REJECTED_NO_WRITES && w.puts.isEmpty(), "invalid reciprocal graph rejected");

        w = new FakeWorld(); w.server = false;
        r = KxMk2PlacementCoordinator.apply(w, plan(A));
        check(r.status() == Status.REJECTED_NO_WRITES && w.puts.isEmpty(), "client thread cannot mutate");

        w = new FakeWorld();
        r = KxMk2PlacementCoordinator.apply(w, plan(A, A));
        check(r.status() == Status.REJECTED_NO_WRITES && w.puts.isEmpty(), "duplicate cells rejected");

        w = new FakeWorld(); w.blocks.put(A, "ORIGINAL_A"); w.blocks.put(B, "ORIGINAL_B"); w.failAtPut = 2;
        r = KxMk2PlacementCoordinator.apply(w, plan(A, B, C));
        check(r.status() == Status.ROLLED_BACK, "reversible failure");
        check(w.blocks.get(A).equals("ORIGINAL_A") && w.blocks.get(B).equals("ORIGINAL_B"), "restored state even after partially failed put");
        check(w.puts.equals(List.of(A, B)), "never writes C after error");
        check(w.restores.equals(List.of(B, A)), "restore reverse order");
        check(w.notifyCount == 1, "rollback resynchronizes clients");

        w = new FakeWorld(); w.failAtPut = 2; w.failRestore = true;
        r = KxMk2PlacementCoordinator.apply(w, plan(A, B));
        check(r.status() == Status.RECOVERY_REQUIRED, "failed restoration escalated");
        check(r.restorationFailures().equals(List.of(B)), "exact failed cell reported");

        w = new FakeWorld(); w.failNotify = true;
        r = KxMk2PlacementCoordinator.apply(w, plan(A));
        check(r.status() == Status.APPLIED_SYNC_PENDING, "sync failure not falsely reported as success");
        check(w.blocks.get(A).equals("BEAM"), "committed state retained on sync failure");

        System.out.println("KxMk2PlacementCoordinatorChecks PASS: " + assertions + " assertions (plain Java 21, NO NeoForge/Minecraft)");
    }
}
