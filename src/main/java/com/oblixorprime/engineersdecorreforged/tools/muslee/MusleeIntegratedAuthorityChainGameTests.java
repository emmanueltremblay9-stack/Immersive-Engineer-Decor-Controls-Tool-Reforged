package com.oblixorprime.engineersdecorreforged.tools.muslee;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * INACTIVE INTEGRATION EXPERIMENT. Real participant1, compressed CandidateIO and selection1 reader;
 * SIMULATED/TEST-ONLY receipt2 linkage, HEAD and terminal records, NOT production codecs or authority.
 * The complete enrolled origin and fixed HEAD selection are explicit test premises, not tested authority.
 * Rewriting HEAD itself to an old prefix is not prevented by these hashes; publication remains unqualified.
 * No publisher, lease, admission bypass, live player hook, atomic install or durability qualification.
 * Every file is inside a freshly created test-owned temporary directory, removed in finally.
 */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusleeIntegratedAuthorityChainGameTests {
    private static final long BUDGET = 4L * 1024 * 1024;
    private static final String ZERO = "0".repeat(64);
    private MusleeIntegratedAuthorityChainGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void simulated_terminal_chain_loads_real_native_candidate_only_after_validation(GameTestHelper h) throws Exception {
        Path dir = Files.createTempDirectory("muslee-integrated-valid-");
        try {
            Fixture f = fixture(h); Chain c = new Chain(f);
            Node origin = stage(h, dir, f, f.snapshot, f.original, null, 1);
            c.add(origin); c.terminal = terminal(f, origin); select(dir, origin);
            ServerPlayer loaded = check(h, dir, f, c, origin, true);
            h.assertTrue(ItemStack.matches(new ItemStack(Items.DIAMOND, 3), loaded.getInventory().getItem(8)),
                    "native load reconstructs selected inventory");
            h.assertTrue(ItemStack.matches(new ItemStack(Items.EMERALD, 2), loaded.containerMenu.getCarried()),
                    "test-only assignment uses detached participant cursor after complete modeled proof");
            h.assertTrue(loaded.getInventory().getItem(0).isEmpty(), "cursor is not also installed in inventory");
            h.succeed();
        } finally { clean(dir); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void simulated_candidate_and_head_crash_cuts_remain_unknown_without_load_or_writes(GameTestHelper h) throws Exception {
        Path dir = Files.createTempDirectory("muslee-integrated-crash-");
        try {
            Fixture f = fixture(h); Chain c = new Chain(f);
            Node origin = stage(h, dir, f, f.snapshot, f.original, null, 1);
            byte[] selection = Files.readAllBytes(manifest(dir, origin));
            Files.delete(manifest(dir, origin));
            check(h, dir, f, c, origin, false); // Candidate only: no HEAD, no selection, no terminal.
            Files.write(manifest(dir, origin), selection, StandardOpenOption.CREATE_NEW);
            c.add(origin); select(dir, origin);
            check(h, dir, f, c, origin, false); // HEAD visible, terminal absent: never auto-complete.
            c.terminal = terminal(f, origin);
            Files.delete(dir.resolve(origin.selection.candidateFileName()));
            check(h, dir, f, c, origin, false); // Terminal alone cannot replace missing native bytes.
            h.succeed();
        } finally { clean(dir); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void simulated_fixed_head_rejects_competing_and_stale_valid_successors(GameTestHelper h) throws Exception {
        Path dir = Files.createTempDirectory("muslee-integrated-forks-");
        try {
            Fixture f = fixture(h); Chain c = new Chain(f);
            Node origin = stage(h, dir, f, f.snapshot, f.original, null, 1);
            c.add(origin); c.terminal = terminal(f, origin);
            CompoundTag a = f.snapshot.copy(); a.putInt("Score", 41);
            CompoundTag b = f.snapshot.copy(); b.putInt("Score", 42);
            Node first = stage(h, dir, f, a, UUID.randomUUID(), origin, 7);
            Node second = stage(h, dir, f, b, UUID.randomUUID(), origin, 7);
            c.add(first); c.add(second); select(dir, second);
            check(h, dir, f, c, first, false);
            check(h, dir, f, c, origin, false);
            ServerPlayer loaded = check(h, dir, f, c, second, true);
            h.assertValueEqual(42, loaded.getScore(), "only independently fixed selected successor loads");
            h.succeed();
        } finally { clean(dir); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void simulated_wrong_terminal_original_snapshot_and_missing_ancestry_fail_closed(GameTestHelper h) throws Exception {
        Path dir = Files.createTempDirectory("muslee-integrated-links-");
        try {
            Fixture f = fixture(h); Chain c = new Chain(f);
            Node origin = stage(h, dir, f, f.snapshot, f.original, null, 1);
            Node later = stage(h, dir, f, f.snapshot, UUID.randomUUID(), origin, 7);
            c.add(origin); c.add(later); select(dir, later);
            Terminal good = terminal(f, origin);
            for (Terminal wrong : new Terminal[]{
                    new Terminal(good.receipt, later.selectionDigest(), good.head),
                    new Terminal(good.receipt, good.selection, later.head.digest()),
                    new Terminal(ZERO, good.selection, good.head)}) {
                c.terminal = wrong; check(h, dir, f, c, later, false);
            }
            c.terminal = good;
            c.original = UUID.randomUUID(); check(h, dir, f, c, later, false);
            c.original = f.original;
            c.nodes.remove(origin.head.digest()); check(h, dir, f, c, later, false);
            c.add(origin);
            Files.delete(archive(dir, origin.head)); check(h, dir, f, c, later, false);
            h.succeed();
        } finally { clean(dir); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void simulated_empty_descendant_retains_original_receipt_without_stale_cursor_resurrection(GameTestHelper h) throws Exception {
        Path dir = Files.createTempDirectory("muslee-integrated-retirement-");
        try {
            Fixture f = fixture(h); Chain c = new Chain(f);
            Node origin = stage(h, dir, f, f.snapshot, f.original, null, 1);
            c.add(origin); c.terminal = terminal(f, origin);
            // A separate disposable player models a completed cursor -> inventory transition.
            // This is NOT an atomic runtime install adapter and never mutates the source player.
            ServerPlayer transfer = player(h, f.source.getUUID()); transfer.load(f.snapshot.copy());
            transfer.getInventory().setItem(0, new ItemStack(Items.EMERALD, 2));
            CompoundTag empty = MusleePlayerSnapshotParticipant.stage(transfer.saveWithoutId(new CompoundTag()),
                    ItemStack.EMPTY, f.world, f.source.getUUID(), f.receipt, h.getLevel().registryAccess()).orElseThrow();
            h.assertTrue(empty.getCompound(MusleePlayerSnapshotParticipant.KEY).getCompound("receipt").equals(
                    f.snapshot.getCompound(MusleePlayerSnapshotParticipant.KEY).getCompound("receipt")),
                    "real participant1 retains exact original receipt fields");
            Node descendant = stage(h, dir, f, empty, UUID.randomUUID(), origin, 7);
            c.add(descendant); select(dir, descendant);
            ServerPlayer loaded = check(h, dir, f, c, descendant, true);
            h.assertTrue(loaded.containerMenu.getCarried().isEmpty(), "explicit EMPTY retires carried ownership in selected snapshot");
            h.assertTrue(ItemStack.matches(new ItemStack(Items.EMERALD, 2), loaded.getInventory().getItem(0)),
                    "inventory owns exact transferred stack, without cursor duplicate");
            h.assertTrue(!descendant.selection.snapshotId().equals(f.original), "new snapshot has fresh identity");
            h.assertTrue(c.original.equals(origin.selection.snapshotId()), "SIMULATED receipt2 origin remains original commit snapshot");
            check(h, dir, f, c, origin, false);
            h.succeed();
        } finally { clean(dir); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void simulated_generation_regression_and_cross_enrollment_head_reject(GameTestHelper h) throws Exception {
        Path dir = Files.createTempDirectory("muslee-integrated-scope-");
        try {
            Fixture f = fixture(h); Chain c = new Chain(f);
            Node origin = stage(h, dir, f, f.snapshot, f.original, null, 7);
            c.add(origin); c.terminal = terminal(f, origin);
            Node regressed = stage(h, dir, f, f.snapshot, UUID.randomUUID(), origin, 1);
            c.add(regressed); select(dir, regressed);
            check(h, dir, f, c, regressed, false);
            Node later = stage(h, dir, f, f.snapshot, UUID.randomUUID(), origin, 8);
            SimulatedHead wrongScope = new SimulatedHead(f.world, f.source.getUUID(), UUID.randomUUID(), 8,
                    later.selectionDigest(), origin.head.digest());
            Node foreign = new Node(later.selection, wrongScope);
            Files.write(archive(dir, wrongScope), wrongScope.bytes(), StandardOpenOption.CREATE_NEW);
            c.add(foreign); select(dir, foreign);
            check(h, dir, f, c, foreign, false);
            c.add(later); select(dir, later);
            check(h, dir, f, c, later, true);
            // A digest-key alias cannot manufacture a cyclic predecessor: record content must hash to its key.
            c.nodes.put(origin.head.digest(), later);
            check(h, dir, f, c, later, false);
            h.succeed();
        } finally { clean(dir); }
    }

    // Strings/UUIDs are immutable: these records deliberately do not pretend to implement HEAD1/journal2 bytes.
    private record SimulatedHead(UUID world, UUID player, UUID enrollment, long generation,
                                 String selection, String previous) {
        byte[] bytes() { return ("SIMULATED-TEST-ONLY-HEAD\n" + world + "\n" + player + "\n" + enrollment
                + "\n" + generation + "\n" + selection + "\n" + previous).getBytes(StandardCharsets.UTF_8); }
        String digest() { return hash(bytes()); }
    }
    private record Terminal(String receipt, String selection, String head) {}
    private record Node(MusleeSnapshotSelection selection, SimulatedHead head) {
        String selectionDigest() { return hex(selection.manifestDigest()); }
    }
    private record Fixture(ServerPlayer source, UUID world, UUID enrollment, UUID original,
                           MusleePlayerSnapshotParticipant.Receipt receipt, CompoundTag snapshot) {}
    private static final class Chain {
        final Map<String, Node> nodes = new HashMap<>();
        final Fixture fixture;
        UUID original;
        Terminal terminal;
        Chain(Fixture f) { fixture = f; original = f.original; }
        void add(Node n) { nodes.put(n.head.digest(), n); }
    }

    /** TEST-ONLY read-only reconciliation. Empty is modeled UNKNOWN, never cancellation or legacy fallback. */
    private static Optional<CompoundTag> reconcile(GameTestHelper h, Path dir, Chain c, Node requested) throws Exception {
        Fixture f = c.fixture;
        if (!Files.exists(dir.resolve("HEAD")) || c.terminal == null) return Optional.empty();
        String current = hash(Files.readAllBytes(dir.resolve("HEAD")));
        if (!current.equals(requested.head.digest())) return Optional.empty();
        HashSet<String> seen = new HashSet<>();
        Node child = null;
        CompoundTag selected = null;
        boolean originalProven = false;
        while (!current.equals(ZERO)) {
            if (!seen.add(current)) return Optional.empty(); // Never accept cycles or repeat an ancestor.
            Node n = c.nodes.get(current);
            if (n == null || !current.equals(n.head.digest()) || !Files.exists(archive(dir, n.head))
                    || !Arrays.equals(Files.readAllBytes(archive(dir, n.head)), n.head.bytes())
                    || !n.head.world.equals(f.world) || !n.head.player.equals(f.source.getUUID())
                    || !n.head.enrollment.equals(f.enrollment) || !n.head.selection.equals(n.selectionDigest())
                    || n.head.generation != n.selection.writerGeneration()) return Optional.empty();
            if (child != null && (child.head.generation < n.head.generation
                    || !hex(child.selection.predecessorSelectionSha256()).equals(n.selectionDigest()))) return Optional.empty();
            Node parent = c.nodes.get(n.head.previous);
            if (!n.head.previous.equals(ZERO) && parent == null) return Optional.empty();
            byte[] previous = parent == null ? new byte[32] : parent.selection.manifestDigest();
            var read = MusleeSnapshotSelectionReader.readExpected(manifest(dir, n), dir, f.world, f.source.getUUID(),
                    n.head.generation, HexFormat.of().parseHex(n.head.selection), previous,
                    BUDGET, BUDGET, h.getLevel().registryAccess());
            if (read.snapshot().isEmpty()) return Optional.empty();
            CompoundTag snapshot = read.snapshot().orElseThrow().snapshotCopy();
            var participant = MusleePlayerSnapshotParticipant.read(snapshot, f.world, f.source.getUUID(),
                    h.getLevel().registryAccess()).value().orElseThrow();
            if (!participant.receipt().equals(f.receipt)) return Optional.empty();
            if (selected == null) selected = snapshot;
            if (n.selection.snapshotId().equals(c.original)) {
                // Acyclic construction: original UUID -> receipt claim -> native bytes -> selection -> HEAD -> terminal.
                if (!c.terminal.receipt.equals(simulatedReceiptDigest(f, c.original))
                        || !c.terminal.selection.equals(n.selectionDigest()) || !c.terminal.head.equals(current)) return Optional.empty();
                originalProven = true;
            }
            child = n; current = n.head.previous;
        }
        return originalProven ? Optional.ofNullable(selected) : Optional.empty();
    }

    private static ServerPlayer check(GameTestHelper h, Path dir, Fixture f, Chain c, Node requested, boolean success) throws Exception {
        Map<String, String> before = directoryHashes(dir);
        CompoundTag sourceBefore = f.source.saveWithoutId(new CompoundTag());
        ItemStack sourceCursor = f.source.containerMenu.getCarried().copy();
        ServerPlayer target = player(h, f.source.getUUID());
        if (!success) {
            target.getInventory().setItem(4, new ItemStack(Items.GOLD_INGOT, 9));
            target.containerMenu.setCarried(new ItemStack(Items.REDSTONE, 5));
        } else {
            h.assertTrue(target.getInventory().isEmpty() && target.containerMenu.getCarried().isEmpty(),
                    "positive fixture starts with no existing inventory/cursor owner");
        }
        CompoundTag targetBefore = target.saveWithoutId(new CompoundTag());
        ItemStack targetCursor = target.containerMenu.getCarried().copy();
        Optional<CompoundTag> accepted = reconcile(h, dir, c, requested);
        h.assertTrue(accepted.isPresent() == success, "modeled reconciliation returns selected snapshot or UNKNOWN");
        int loads = 0, cursorAssignments = 0;
        if (accepted.isPresent()) {
            CompoundTag snapshot = accepted.orElseThrow();
            target.load(snapshot); loads++;
            var p = MusleePlayerSnapshotParticipant.read(snapshot, f.world, f.source.getUUID(),
                    h.getLevel().registryAccess()).value().orElseThrow();
            // TEST-ONLY fresh disposable player, not a real save/load hook or granted runtime ownership.
            h.assertTrue(target.containerMenu.getCarried().isEmpty(), "test-only restore never replaces an occupied cursor");
            target.containerMenu.setCarried(p.cursorCopy(h.getLevel().registryAccess()).orElseThrow()); cursorAssignments++;
        }
        h.assertValueEqual(success ? 1 : 0, loads, "UNKNOWN never calls native player load");
        h.assertValueEqual(success ? 1 : 0, cursorAssignments, "UNKNOWN never assigns cursor");
        if (!success) {
            h.assertTrue(targetBefore.equals(target.saveWithoutId(new CompoundTag())), "UNKNOWN leaves target native state untouched");
            h.assertTrue(ItemStack.matches(targetCursor, target.containerMenu.getCarried()), "UNKNOWN leaves target cursor untouched");
        }
        h.assertTrue(before.equals(directoryHashes(dir)), "reconciliation/load never writes, deletes, repairs or adopts files");
        h.assertTrue(sourceBefore.equals(f.source.saveWithoutId(new CompoundTag())), "source native player state unchanged");
        h.assertTrue(ItemStack.matches(sourceCursor, f.source.containerMenu.getCarried()), "source cursor unchanged");
        return target;
    }

    private static Fixture fixture(GameTestHelper h) {
        ServerPlayer source = player(h, UUID.randomUUID());
        source.getInventory().setItem(8, new ItemStack(Items.DIAMOND, 3));
        source.containerMenu.setCarried(new ItemStack(Items.EMERALD, 2));
        UUID world = UUID.randomUUID(), original = UUID.randomUUID(); // Allocate BEFORE candidate/selection/HEAD.
        byte[] after = new byte[32]; after[0] = 1;
        var receipt = new MusleePlayerSnapshotParticipant.Receipt(UUID.randomUUID(), UUID.randomUUID(), 1,
                MusleePlayerSnapshotParticipant.Outcome.EXECUTED, new byte[32], after);
        CompoundTag snapshot = MusleePlayerSnapshotParticipant.stage(source.saveWithoutId(new CompoundTag()),
                source.containerMenu.getCarried(), world, source.getUUID(), receipt, h.getLevel().registryAccess()).orElseThrow();
        return new Fixture(source, world, UUID.randomUUID(), original, receipt, snapshot);
    }
    private static ServerPlayer player(GameTestHelper h, UUID id) {
        ServerPlayer p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                new GameProfile(id, "MusleeChain"), ClientInformation.createDefault());
        p.connection = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "MusleeTransport")).connection;
        return p;
    }
    private static Node stage(GameTestHelper h, Path dir, Fixture f, CompoundTag snapshot, UUID id,
                              Node predecessor, long generation) throws Exception {
        Path candidate = dir.resolve(id + MusleeSnapshotCandidateIO.FILE_SUFFIX);
        var result = MusleeSnapshotCandidateIO.writeNewCandidate(candidate, snapshot, f.world, f.source.getUUID(),
                h.getLevel().registryAccess(), BUDGET);
        h.assertTrue(result.status() == MusleeSnapshotCandidateIO.Status.VERIFIED_CANDIDATE_NOT_PUBLISHED,
                "actual compressed native candidate is verified, not published");
        var selection = MusleeSnapshotSelection.create(f.world, f.source.getUUID(), generation, id, Files.size(candidate),
                sha(Files.readAllBytes(candidate)), predecessor == null ? new byte[32] : predecessor.selection.manifestDigest()).orElseThrow();
        SimulatedHead head = new SimulatedHead(f.world, f.source.getUUID(), f.enrollment, generation,
                hex(selection.manifestDigest()), predecessor == null ? ZERO : predecessor.head.digest());
        Node node = new Node(selection, head);
        Files.write(manifest(dir, node), selection.encode(), StandardOpenOption.CREATE_NEW);
        Files.write(archive(dir, head), head.bytes(), StandardOpenOption.CREATE_NEW);
        return node;
    }
    private static Terminal terminal(Fixture f, Node origin) {
        return new Terminal(simulatedReceiptDigest(f, f.original), origin.selectionDigest(), origin.head.digest());
    }
    private static String simulatedReceiptDigest(Fixture f, UUID original) {
        // NOT receipt2 serialization: participant1 has NO originalOutcomeSnapshotUUID field.
        // An immutable test-only external claim supplies that missing proposed binding, without modifying v1 bytes.
        var r = f.receipt;
        return hash(("SIMULATED-TEST-ONLY-RECEIPT2\n" + f.world + "\n" + f.source.getUUID() + "\n"
                + original + "\n" + r.transactionId() + "\n" + r.pressId() + "\n" + r.writerGeneration()
                + "\n" + r.outcome() + "\n" + hex(r.beforeHash()) + "\n" + hex(r.afterHash())).getBytes(StandardCharsets.UTF_8));
    }
    private static void select(Path dir, Node n) throws IOException {
        // Simple fixture write, explicitly NOT atomic replacement, qualified publication or real-world authority.
        Files.write(dir.resolve("HEAD"), n.head.bytes());
    }
    private static Path manifest(Path dir, Node n) { return dir.resolve(n.selectionDigest() + ".selection"); }
    private static Path archive(Path dir, SimulatedHead h) { return dir.resolve(h.digest() + ".simulated-head"); }
    private static String hex(byte[] b) { return HexFormat.of().formatHex(b); }
    private static byte[] sha(byte[] b) {
        try { return MessageDigest.getInstance("SHA-256").digest(b); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static String hash(byte[] b) { return hex(sha(b)); }
    private static Map<String, String> directoryHashes(Path dir) throws IOException {
        Map<String, String> result = new TreeMap<>();
        try (var files = Files.list(dir)) {
            for (Path file : files.toList()) result.put(file.getFileName().toString(), hash(Files.readAllBytes(file)));
        }
        return result;
    }
    private static void clean(Path dir) throws IOException {
        try (var files = Files.list(dir)) { for (Path file : files.toList()) Files.delete(file); }
        Files.delete(dir);
    }
}
