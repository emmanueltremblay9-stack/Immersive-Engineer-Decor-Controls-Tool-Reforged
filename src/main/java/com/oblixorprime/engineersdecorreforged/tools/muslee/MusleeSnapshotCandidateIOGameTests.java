package com.oblixorprime.engineersdecorreforged.tools.muslee;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real temporary files only: no runtime save hooks, authority selection or power-loss claim. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusleeSnapshotCandidateIOGameTests {
    private static final long READ_BUDGET = 4L * 1024 * 1024;
    private MusleeSnapshotCandidateIOGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void candidate_real_native_snapshot_roundtrips_with_exact_file_receipt(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("muslee-candidate-roundtrip-");
        try {
            Fixture fixture = fixture(helper);
            CompoundTag before = fixture.snapshot().copy();
            Path path = candidate(directory, "carried");
            var result = write(helper, fixture, path);
            verifiedReceipt(helper, path, result);
            CompoundTag read = read(path);
            helper.assertTrue(before.equals(read), "complete native snapshot survives compressed file roundtrip");
            helper.assertTrue(before.equals(fixture.snapshot()), "I/O does not mutate supplied snapshot");
            var decoded = MusleePlayerSnapshotParticipant.read(read, fixture.world(), fixture.player().getUUID(),
                    helper.getLevel().registryAccess()).value().orElseThrow();
            helper.assertTrue(decoded.receipt().equals(fixture.receipt()), "participant receipt survives file roundtrip");
            helper.assertTrue(ItemStack.matches(bars(), decoded.cursorCopy(helper.getLevel().registryAccess()).orElseThrow()),
                    "custom cursor component and count survive file roundtrip");
            ServerPlayer loaded = player(helper, fixture.player().getUUID());
            loaded.load(read.copy());
            helper.assertTrue(ItemStack.matches(new ItemStack(Items.DIAMOND, 3), loaded.getInventory().getItem(8)),
                    "native inventory survives compressed candidate load");
            helper.assertTrue(loaded.containerMenu.getCarried().isEmpty(), "native load installs no cursor restoration hook");
            helper.assertTrue(ItemStack.matches(bars(), fixture.player().containerMenu.getCarried()),
                    "writing did not transfer source cursor ownership");
            helper.succeed();
        } finally { clean(directory); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void candidate_native_close_writes_distinct_empty_snapshot_without_rewriting_carried(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("muslee-candidate-transfer-");
        try {
            Fixture fixture = fixture(helper);
            Path carriedPath = candidate(directory, "carried");
            var carriedResult = write(helper, fixture, carriedPath);
            verifiedReceipt(helper, carriedPath, carriedResult);
            byte[] carriedBytes = Files.readAllBytes(carriedPath);
            ServerPlayer source = fixture.player();
            helper.assertTrue(source.isAlive() && !source.hasDisconnected(), "native close inventory-return branch is live");
            source.containerMenu.removed(source);
            helper.assertTrue(source.containerMenu.getCarried().isEmpty(), "native close emptied cursor");
            helper.assertValueEqual(4, countBars(source), "native close transferred four bars to inventory");
            CompoundTag nativeAfter = source.saveWithoutId(new CompoundTag());
            nativeAfter.put(MusleePlayerSnapshotParticipant.KEY,
                    fixture.snapshot().getCompound(MusleePlayerSnapshotParticipant.KEY).copy());
            CompoundTag empty = MusleePlayerSnapshotParticipant.stage(nativeAfter, ItemStack.EMPTY, fixture.world(),
                    source.getUUID(), fixture.receipt(), helper.getLevel().registryAccess()).orElseThrow();
            Path emptyPath = candidate(directory, "empty");
            var emptyResult = MusleeSnapshotCandidateIO.writeNewCandidate(emptyPath, empty, fixture.world(), source.getUUID(),
                    helper.getLevel().registryAccess(), READ_BUDGET);
            verifiedReceipt(helper, emptyPath, emptyResult);
            refused(helper, MusleeSnapshotCandidateIO.writeNewCandidate(carriedPath, empty, fixture.world(), source.getUUID(),
                    helper.getLevel().registryAccess(), READ_BUDGET), MusleeSnapshotCandidateIO.Status.NOT_CREATED,
                    "new state cannot overwrite prior verified candidate");
            helper.assertTrue(Arrays.equals(carriedBytes, Files.readAllBytes(carriedPath)), "old immutable candidate remains byte-exact");
            helper.assertTrue(!Arrays.equals(carriedResult.receipt().orElseThrow().sha256(), emptyResult.receipt().orElseThrow().sha256()),
                    "distinct native states have distinct byte receipts");
            var carried = MusleePlayerSnapshotParticipant.read(read(carriedPath), fixture.world(), source.getUUID(),
                    helper.getLevel().registryAccess()).value().orElseThrow();
            CompoundTag emptyRead = read(emptyPath);
            var transferred = MusleePlayerSnapshotParticipant.read(emptyRead, fixture.world(), source.getUUID(),
                    helper.getLevel().registryAccess()).value().orElseThrow();
            helper.assertTrue(!carried.cursorEmpty() && transferred.cursorEmpty(), "new candidate explicitly replaces carried with EMPTY");
            helper.assertTrue(carried.receipt().equals(transferred.receipt()), "participant transaction receipt retained unchanged");
            helper.assertTrue(!emptyRead.getCompound(MusleePlayerSnapshotParticipant.KEY).contains("cursor"), "no stale carried payload");
            ServerPlayer loaded = player(helper, source.getUUID()); loaded.load(emptyRead.copy());
            helper.assertValueEqual(4, countBars(loaded), "native reload owns four bars in inventory");
            helper.assertTrue(ItemStack.matches(bars(), loaded.getInventory().getItem(0)), "transferred custom data preserved");
            helper.assertTrue(loaded.containerMenu.getCarried().isEmpty(), "empty candidate does not duplicate cursor");
            helper.succeed();
        } finally { clean(directory); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void candidate_existing_target_and_preopen_failure_never_create_or_overwrite(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("muslee-candidate-preopen-");
        try {
            Fixture fixture = fixture(helper);
            Path existing = candidate(directory, "existing");
            byte[] sentinel = {4, 8, 15, 16, 23, 42}; Files.write(existing, sentinel, StandardOpenOption.CREATE_NEW);
            refused(helper, write(helper, fixture, existing), MusleeSnapshotCandidateIO.Status.NOT_CREATED, "existing target");
            helper.assertTrue(Arrays.equals(sentinel, Files.readAllBytes(existing)), "existing target is never truncated or replaced");
            Path unopened = candidate(directory, "unopened");
            boolean[] reached = {false};
            var result = MusleeSnapshotCandidateIO.writeNewCandidate(unopened, fixture.snapshot(), fixture.world(), fixture.player().getUUID(),
                    helper.getLevel().registryAccess(), READ_BUDGET, (point, path) -> {
                        if (point == MusleeSnapshotCandidateIO.Point.BEFORE_OPEN) { reached[0] = true; throw new IOException("injected before open"); }
                    });
            refused(helper, result, MusleeSnapshotCandidateIO.Status.NOT_CREATED, "before-open failure");
            helper.assertTrue(reached[0] && !Files.exists(unopened), "before-open seam reached without creating a file");
            helper.succeed();
        } finally { clean(directory); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void candidate_every_postopen_fault_is_unknown_and_retains_its_file(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("muslee-candidate-faults-");
        try {
            Fixture fixture = fixture(helper);
            for (MusleeSnapshotCandidateIO.Point failing : MusleeSnapshotCandidateIO.Point.values()) {
                if (failing == MusleeSnapshotCandidateIO.Point.BEFORE_OPEN) continue;
                Path destination = candidate(directory, failing.name());
                boolean[] reached = {false};
                var result = MusleeSnapshotCandidateIO.writeNewCandidate(destination, fixture.snapshot(), fixture.world(), fixture.player().getUUID(),
                        helper.getLevel().registryAccess(), READ_BUDGET, (point, path) -> {
                            if (point == failing) { reached[0] = true; throw new IOException("injected " + failing); }
                        });
                helper.assertTrue(reached[0], "fault boundary actually reached: " + failing);
                refused(helper, result, MusleeSnapshotCandidateIO.Status.UNKNOWN, failing.name());
                helper.assertTrue(Files.isRegularFile(destination), "uncertain candidate retained, not deleted: " + failing);
                if (failing == MusleeSnapshotCandidateIO.Point.DURING_WRITE)
                    helper.assertTrue(Files.size(destination) > 0, "first compressed bytes survive write fault, even before AFTER_OPEN");
            }
            helper.succeed();
        } finally { clean(directory); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void candidate_corrupt_truncated_or_appended_readback_is_unknown_and_retained(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("muslee-candidate-tamper-");
        try {
            Fixture fixture = fixture(helper);
            for (String mutation : new String[]{"corrupt", "truncate", "append"}) {
                Path destination = candidate(directory, mutation);
                byte[][] tampered = {null};
                var result = MusleeSnapshotCandidateIO.writeNewCandidate(destination, fixture.snapshot(), fixture.world(), fixture.player().getUUID(),
                        helper.getLevel().registryAccess(), READ_BUDGET, (point, path) -> {
                            if (point != MusleeSnapshotCandidateIO.Point.BEFORE_READBACK) return;
                            byte[] bytes = Files.readAllBytes(path);
                            if (mutation.equals("corrupt")) bytes[0] ^= 1;
                            else if (mutation.equals("truncate")) bytes = Arrays.copyOf(bytes, bytes.length / 2);
                            else { bytes = Arrays.copyOf(bytes, bytes.length + 3); bytes[bytes.length - 1] = 42; }
                            tampered[0] = bytes;
                            Files.write(path, bytes, StandardOpenOption.TRUNCATE_EXISTING);
                        });
                helper.assertTrue(tampered[0] != null, "readback mutation seam reached: " + mutation);
                refused(helper, result, MusleeSnapshotCandidateIO.Status.UNKNOWN, mutation);
                helper.assertTrue(Arrays.equals(tampered[0], Files.readAllBytes(destination)),
                        "rejected candidate retained without repair: " + mutation);
            }
            helper.succeed();
        } finally { clean(directory); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void candidate_missing_participant_foreign_ids_suffix_and_read_budget_fail_closed(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("muslee-candidate-validation-");
        try {
            Fixture fixture = fixture(helper);
            for (String invalid : new String[]{"missing", "world", "player", "native-player", "suffix"}) {
                CompoundTag snapshot = fixture.snapshot().copy();
                UUID world = fixture.world(), player = fixture.player().getUUID();
                Path destination = invalid.equals("suffix") ? directory.resolve("not-a-candidate.dat") : candidate(directory, invalid);
                if (invalid.equals("missing")) snapshot.remove(MusleePlayerSnapshotParticipant.KEY);
                if (invalid.equals("world")) world = UUID.randomUUID();
                if (invalid.equals("player")) player = UUID.randomUUID();
                if (invalid.equals("native-player")) snapshot.putUUID("UUID", UUID.randomUUID());
                boolean[] touched = {false};
                var result = MusleeSnapshotCandidateIO.writeNewCandidate(destination, snapshot, world, player,
                        helper.getLevel().registryAccess(), READ_BUDGET, (point, path) -> touched[0] = true);
                refused(helper, result, MusleeSnapshotCandidateIO.Status.NOT_CREATED, invalid);
                helper.assertTrue(!touched[0] && !Files.exists(destination), "invalid input rejected before any I/O seam: " + invalid);
            }
            Path limited = candidate(directory, "budget");
            boolean[] reached = {false};
            var result = MusleeSnapshotCandidateIO.writeNewCandidate(limited, fixture.snapshot(), fixture.world(), fixture.player().getUUID(),
                    helper.getLevel().registryAccess(), 1L, (point, path) -> {
                        if (point == MusleeSnapshotCandidateIO.Point.BEFORE_READBACK) reached[0] = true;
                    });
            refused(helper, result, MusleeSnapshotCandidateIO.Status.UNKNOWN, "read budget exhausted");
            helper.assertTrue(reached[0] && Files.isRegularFile(limited), "budget failure retains written file after reaching readback");
            helper.assertTrue(read(limited).equals(fixture.snapshot()), "retained file is valid with adequate test read budget");
            helper.succeed();
        } finally { clean(directory); }
    }

    private record Fixture(ServerPlayer player, UUID world, MusleePlayerSnapshotParticipant.Receipt receipt, CompoundTag snapshot) {}

    private static Fixture fixture(GameTestHelper helper) {
        ServerPlayer player = player(helper, UUID.randomUUID());
        player.getInventory().setItem(8, new ItemStack(Items.DIAMOND, 3));
        player.containerMenu.setCarried(bars());
        UUID world = UUID.randomUUID();
        byte[] after = new byte[32]; after[0] = 1;
        var receipt = new MusleePlayerSnapshotParticipant.Receipt(UUID.randomUUID(), UUID.randomUUID(), 1,
                MusleePlayerSnapshotParticipant.Outcome.EXECUTED, new byte[32], after);
        CompoundTag snapshot = MusleePlayerSnapshotParticipant.stage(player.saveWithoutId(new CompoundTag()),
                player.containerMenu.getCarried(), world, player.getUUID(), receipt, helper.getLevel().registryAccess()).orElseThrow();
        return new Fixture(player, world, receipt, snapshot);
    }

    private static MusleeSnapshotCandidateIO.Result write(GameTestHelper helper, Fixture fixture, Path path) {
        return MusleeSnapshotCandidateIO.writeNewCandidate(path, fixture.snapshot(), fixture.world(), fixture.player().getUUID(),
                helper.getLevel().registryAccess(), READ_BUDGET);
    }

    private static void verifiedReceipt(GameTestHelper helper, Path path, MusleeSnapshotCandidateIO.Result result) throws Exception {
        helper.assertTrue(result.status() == MusleeSnapshotCandidateIO.Status.VERIFIED_CANDIDATE_NOT_PUBLISHED,
                "candidate verified on this filesystem; this is not publication or power-loss qualification");
        var receipt = result.receipt().orElseThrow();
        byte[] bytes = Files.readAllBytes(path);
        helper.assertValueEqual((long) bytes.length, receipt.compressedBytes(), "receipt records exact compressed file length");
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
        helper.assertTrue(Arrays.equals(hash, receipt.sha256()), "receipt hash matches independent hash of actual file bytes");
        receipt.sha256()[0] ^= 1;
        helper.assertTrue(Arrays.equals(hash, receipt.sha256()), "file receipt hash accessor is defensive");
    }

    private static void refused(GameTestHelper helper, MusleeSnapshotCandidateIO.Result result,
                                MusleeSnapshotCandidateIO.Status status, String label) {
        helper.assertTrue(result.status() == status && result.receipt().isEmpty(), label + " returns " + status + " without receipt");
    }

    private static CompoundTag read(Path path) throws IOException {
        try (var data = new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
            return NbtIo.read(data, NbtAccounter.create(READ_BUDGET));
        }
    }

    private static Path candidate(Path directory, String name) { return directory.resolve(name + MusleeSnapshotCandidateIO.FILE_SUFFIX); }

    private static void clean(Path directory) throws IOException {
        // Only files created inside this test-owned fresh directory, never any playerdata/live path.
        try (var files = Files.list(directory)) {
            for (Path file : files.toList()) Files.delete(file);
        }
        Files.delete(directory);
    }

    private static ItemStack bars() {
        ItemStack stack = new ItemStack(EngineerToolsModule.MUSLI_BAR.get(), 4);
        CompoundTag data = new CompoundTag(); data.putString("witness", "candidate-file-cursor");
        data.putIntArray("nested_array", new int[]{1, 2, 3});
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    private static int countBars(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(EngineerToolsModule.MUSLI_BAR.get())) count += stack.getCount();
        }
        return count;
    }

    private static ServerPlayer player(GameTestHelper helper, UUID identity) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(identity, "MusliCandidate"), ClientInformation.createDefault());
        player.connection = new FakePlayer(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "MusliTransport")).connection;
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return player;
    }
}
