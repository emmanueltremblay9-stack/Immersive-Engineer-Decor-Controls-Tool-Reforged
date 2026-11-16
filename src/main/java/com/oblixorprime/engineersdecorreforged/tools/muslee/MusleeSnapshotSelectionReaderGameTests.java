package com.oblixorprime.engineersdecorreforged.tools.muslee;

import com.mojang.authlib.GameProfile;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Test-owned temporary files only. Validation does not establish publication or restoration authority. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusleeSnapshotSelectionReaderGameTests {
    private static final long BUDGET = 4L * 1024 * 1024;
    private static final long GENERATION = 7;
    private MusleeSnapshotSelectionReaderGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void selection_real_native_file_validates_and_returns_detached_snapshot(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("muslee-selection-valid-");
        try {
            Fixture f = fixture(helper);
            Selection s = write(helper, directory, f, f.snapshot(), false);
            helper.assertValueEqual(176L, Files.size(s.manifest()), "manifest has exact fixed framing");
            Map<String, String> before = directoryHashes(directory);
            var result = read(helper, directory, s, f.world(), f.player().getUUID(), GENERATION, s.head(), s.previous(), BUDGET, BUDGET);
            helper.assertTrue(result.status() == MusleeSnapshotSelectionReader.Status.VALIDATED_CANDIDATE_NOT_AUTHORITY,
                    "successful byte validation grants no authority");
            var snapshot = result.snapshot().orElseThrow();
            helper.assertTrue(snapshot.snapshotCopy().equals(f.snapshot()), "complete native snapshot survives selection read");
            var participant = MusleePlayerSnapshotParticipant.read(snapshot.snapshotCopy(), f.world(), f.player().getUUID(),
                    helper.getLevel().registryAccess()).value().orElseThrow();
            helper.assertValueEqual(1L, participant.receipt().writerGeneration(), "retained receipt may predate selection generation seven");
            CompoundTag detached = snapshot.snapshotCopy();
            detached.remove("Inventory");
            detached.getCompound(MusleePlayerSnapshotParticipant.KEY).putString("cursorState", "changed");
            helper.assertTrue(snapshot.snapshotCopy().equals(f.snapshot()), "result snapshot is deeply detached");
            snapshot.selection().encode()[0] ^= 1;
            snapshot.selection().candidateSha256()[0] ^= 1;
            helper.assertTrue(Arrays.equals(s.head(), snapshot.selection().manifestDigest()), "selection accessors cannot mutate result");
            helper.assertTrue(before.equals(directoryHashes(directory)), "successful read makes no filesystem changes");
            helper.assertTrue(ItemStack.matches(new ItemStack(Items.EMERALD, 2), f.player().containerMenu.getCarried()),
                    "reader does not transfer live cursor ownership");
            helper.succeed();
        } finally { clean(directory); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void selection_competing_successors_require_independent_current_head(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("muslee-selection-head-");
        try {
            Fixture f = fixture(helper);
            Selection a = write(helper, directory, f, f.snapshot(), false);
            CompoundTag changed = f.snapshot().copy(); changed.putInt("Score", 917);
            Selection b = write(helper, directory, f, changed, false);
            helper.assertTrue(!Arrays.equals(a.head(), b.head()), "competing valid successors have distinct current heads");
            helper.assertTrue(read(helper, directory, a, f.world(), f.player().getUUID(), GENERATION, a.head(), a.previous(), BUDGET, BUDGET)
                    .snapshot().isPresent(), "externally selected first successor validates");
            reject(helper, directory, b, f.world(), f.player().getUUID(), GENERATION, a.head(), a.previous(), BUDGET, BUDGET,
                    "same generation/predecessor cannot replace expected current head");
            helper.assertTrue(read(helper, directory, b, f.world(), f.player().getUUID(), GENERATION, b.head(), b.previous(), BUDGET, BUDGET)
                    .snapshot().isPresent(), "second successor validates only with its own independently selected head");
            reject(helper, directory, a, f.world(), f.player().getUUID(), GENERATION, b.head(), b.previous(), BUDGET, BUDGET,
                    "no fallback to another valid candidate");
            helper.succeed();
        } finally { clean(directory); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void selection_wrong_expectations_and_read_budgets_fail_without_writes(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("muslee-selection-expectation-");
        try {
            Fixture f = fixture(helper); Selection s = write(helper, directory, f, f.snapshot(), false);
            for (String mismatch : new String[]{"current", "previous", "world", "player", "generation", "compressed-budget", "nbt-budget"}) {
                byte[] head = s.head().clone(), previous = s.previous().clone();
                if (mismatch.equals("current")) head[0] ^= 1;
                if (mismatch.equals("previous")) previous[0] ^= 1;
                reject(helper, directory, s, mismatch.equals("world") ? UUID.randomUUID() : f.world(),
                        mismatch.equals("player") ? UUID.randomUUID() : f.player().getUUID(),
                        mismatch.equals("generation") ? GENERATION + 1 : GENERATION, head, previous,
                        mismatch.equals("compressed-budget") ? 1 : BUDGET, mismatch.equals("nbt-budget") ? 1 : BUDGET, mismatch);
            }
            helper.succeed();
        } finally { clean(directory); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void selection_missing_truncated_and_tampered_files_fail_without_fallback(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("muslee-selection-files-");
        try {
            Fixture f = fixture(helper);
            // Leave an unrelated valid candidate present throughout: failures must not scan or adopt it.
            write(helper, directory, f, f.snapshot(), false);
            for (String mutation : new String[]{"missing-candidate", "missing-manifest", "manifest-truncate", "manifest-append",
                    "manifest-corrupt", "candidate-truncate", "candidate-append", "candidate-corrupt"}) {
                Selection s = write(helper, directory, f, f.snapshot(), false);
                Path target = mutation.startsWith("manifest") || mutation.equals("missing-manifest") ? s.manifest() : s.candidate();
                byte[] bytes = Files.readAllBytes(target);
                if (mutation.startsWith("missing")) Files.delete(target);
                else {
                    if (mutation.endsWith("truncate")) bytes = Arrays.copyOf(bytes, bytes.length - 1);
                    else if (mutation.endsWith("append")) bytes = Arrays.copyOf(bytes, bytes.length + 1);
                    else bytes[0] ^= 1;
                    Files.write(target, bytes);
                }
                reject(helper, directory, s, f.world(), f.player().getUUID(), GENERATION, s.head(), s.previous(), BUDGET, BUDGET, mutation);
            }
            helper.succeed();
        } finally { clean(directory); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void selection_authenticated_wrong_length_hash_and_strict_participant_fail(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("muslee-selection-content-");
        try {
            Fixture f = fixture(helper);
            for (String mutation : new String[]{"length", "hash", "missing", "world", "player", "native-player", "unknown-field", "trailing-nbt"}) {
                CompoundTag snapshot = f.snapshot().copy();
                CompoundTag participant = snapshot.getCompound(MusleePlayerSnapshotParticipant.KEY);
                if (mutation.equals("missing")) snapshot.remove(MusleePlayerSnapshotParticipant.KEY);
                if (mutation.equals("world")) participant.putString("worldId", UUID.randomUUID().toString());
                if (mutation.equals("player")) participant.putString("playerId", UUID.randomUUID().toString());
                if (mutation.equals("native-player")) snapshot.putUUID("UUID", UUID.randomUUID());
                if (mutation.equals("unknown-field")) participant.putInt("extra", 1);
                Selection s = write(helper, directory, f, snapshot, true, mutation.equals("trailing-nbt"));
                if (mutation.equals("length") || mutation.equals("hash")) {
                    byte[] hash = sha(Files.readAllBytes(s.candidate()));
                    if (mutation.equals("hash")) hash[0] ^= 1;
                    s = manifest(directory, f, s.id(), s.candidate(), Files.size(s.candidate()) + (mutation.equals("length") ? 1 : 0), hash);
                }
                reject(helper, directory, s, f.world(), f.player().getUUID(), GENERATION, s.head(), s.previous(), BUDGET, BUDGET, mutation);
            }
            helper.succeed();
        } finally { clean(directory); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void selection_invalid_expectation_inputs_do_not_touch_files(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("muslee-selection-input-");
        try {
            Fixture f = fixture(helper); Selection s = write(helper, directory, f, f.snapshot(), false);
            for (String invalid : new String[]{"manifest", "directory", "world", "player", "generation", "head", "previous",
                    "head-width", "previous-width", "compressed-budget", "nbt-budget", "registries"}) {
                Map<String, String> before = directoryHashes(directory);
                var result = MusleeSnapshotSelectionReader.readExpected(
                        invalid.equals("manifest") ? null : s.manifest(), invalid.equals("directory") ? null : directory,
                        invalid.equals("world") ? null : f.world(), invalid.equals("player") ? null : f.player().getUUID(),
                        invalid.equals("generation") ? 0 : GENERATION,
                        invalid.equals("head") ? null : invalid.equals("head-width") ? new byte[31] : s.head(),
                        invalid.equals("previous") ? null : invalid.equals("previous-width") ? new byte[33] : s.previous(),
                        invalid.equals("compressed-budget") ? 0 : BUDGET, invalid.equals("nbt-budget") ? 0 : BUDGET,
                        invalid.equals("registries") ? null : helper.getLevel().registryAccess());
                helper.assertTrue(result.status() == MusleeSnapshotSelectionReader.Status.INVALID_EXPECTATION && result.snapshot().isEmpty(),
                        invalid + " rejects invalid authority context without a candidate");
                helper.assertTrue(before.equals(directoryHashes(directory)), invalid + " leaves all files unchanged");
            }
            helper.succeed();
        } finally { clean(directory); }
    }

    private record Fixture(ServerPlayer player, UUID world, CompoundTag snapshot) {}
    private record Selection(UUID id, Path manifest, Path candidate, byte[] head, byte[] previous) {}

    private static Fixture fixture(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "MusliSelection"), ClientInformation.createDefault());
        player.connection = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "MusliTransport")).connection;
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.getInventory().setItem(8, new ItemStack(Items.DIAMOND, 3));
        player.containerMenu.setCarried(new ItemStack(Items.EMERALD, 2));
        UUID world = UUID.randomUUID(); byte[] after = new byte[32]; after[0] = 1;
        var receipt = new MusleePlayerSnapshotParticipant.Receipt(UUID.randomUUID(), UUID.randomUUID(), 1,
                MusleePlayerSnapshotParticipant.Outcome.EXECUTED, new byte[32], after);
        CompoundTag snapshot = MusleePlayerSnapshotParticipant.stage(player.saveWithoutId(new CompoundTag()),
                player.containerMenu.getCarried(), world, player.getUUID(), receipt, helper.getLevel().registryAccess()).orElseThrow();
        return new Fixture(player, world, snapshot);
    }

    private static Selection write(GameTestHelper helper, Path directory, Fixture f, CompoundTag snapshot, boolean raw) throws Exception {
        return write(helper, directory, f, snapshot, raw, false);
    }
    private static Selection write(GameTestHelper helper, Path directory, Fixture f, CompoundTag snapshot, boolean raw, boolean trailing) throws Exception {
        UUID id = UUID.randomUUID(); Path candidate = directory.resolve(id + MusleeSnapshotCandidateIO.FILE_SUFFIX);
        if (raw) {
            try (var out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(candidate)))) {
                NbtIo.write(snapshot, out);
                if (trailing) out.writeByte(42);
            }
        } else {
            var result = MusleeSnapshotCandidateIO.writeNewCandidate(candidate, snapshot, f.world(), f.player().getUUID(),
                    helper.getLevel().registryAccess(), BUDGET);
            helper.assertTrue(result.status() == MusleeSnapshotCandidateIO.Status.VERIFIED_CANDIDATE_NOT_PUBLISHED,
                    "real native candidate fixture verified before selection creation");
        }
        byte[] bytes = Files.readAllBytes(candidate);
        return manifest(directory, f, id, candidate, bytes.length, sha(bytes));
    }
    private static Selection manifest(Path directory, Fixture f, UUID id, Path candidate, long size, byte[] hash) throws Exception {
        // Test authority context is set before writing disk, never obtained by decoding the file under test.
        byte[] previous = new byte[32]; previous[0] = 19;
        var selection = MusleeSnapshotSelection.create(f.world(), f.player().getUUID(), GENERATION, id, size, hash, previous).orElseThrow();
        byte[] encoded = selection.encode();
        byte[] independentlyExpectedHead = sha(Arrays.copyOf(encoded, 144));
        Path manifest = directory.resolve(UUID.randomUUID() + ".selection");
        Files.write(manifest, encoded);
        return new Selection(id, manifest, candidate, independentlyExpectedHead, previous);
    }
    private static MusleeSnapshotSelectionReader.Result read(GameTestHelper helper, Path directory, Selection s, UUID world,
            UUID player, long generation, byte[] head, byte[] previous, long compressedBudget, long nbtBudget) {
        return MusleeSnapshotSelectionReader.readExpected(s.manifest(), directory, world, player, generation,
                head, previous, compressedBudget, nbtBudget, helper.getLevel().registryAccess());
    }
    private static void reject(GameTestHelper helper, Path directory, Selection s, UUID world, UUID player, long generation,
            byte[] head, byte[] previous, long compressedBudget, long nbtBudget, String label) throws Exception {
        Map<String, String> before = directoryHashes(directory);
        var result = read(helper, directory, s, world, player, generation, head, previous, compressedBudget, nbtBudget);
        helper.assertTrue(result.status() == MusleeSnapshotSelectionReader.Status.INVALID_OR_UNAVAILABLE && result.snapshot().isEmpty(),
                label + " fails closed without candidate result");
        helper.assertTrue(before.equals(directoryHashes(directory)), label + " neither creates, deletes, repairs nor rewrites any file");
    }
    private static Map<String, String> directoryHashes(Path directory) throws Exception {
        Map<String, String> hashes = new TreeMap<>();
        try (var files = Files.list(directory)) {
            for (Path file : files.toList()) hashes.put(file.getFileName().toString(), HexFormat.of().formatHex(sha(Files.readAllBytes(file))));
        }
        return hashes;
    }
    private static byte[] sha(byte[] bytes) throws Exception { return MessageDigest.getInstance("SHA-256").digest(bytes); }
    private static void clean(Path directory) throws IOException {
        try (var files = Files.list(directory)) { for (Path file : files.toList()) Files.delete(file); }
        Files.delete(directory);
    }
}
