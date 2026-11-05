package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/**
 * Inactive real I/O for NEW, unselected native player snapshot candidates, NOT journal entries.
 * Never overwrites, publishes, restores, deletes, repairs, falls back or selects authority.
 * Caller must supply an isolated trusted directory/destination; runtime writer/path ownership,
 * platform power-loss durability and complete memory qualification remain external gates.
 */
public final class MusleeSnapshotCandidateIO {
    public static final String FILE_SUFFIX = ".muslee-candidate.nbt.gz";
    public enum Status { VERIFIED_CANDIDATE_NOT_PUBLISHED, NOT_CREATED, UNKNOWN }
    enum Point { BEFORE_OPEN, DURING_WRITE, AFTER_OPEN, BEFORE_GZIP_FINISH, AFTER_SERIALIZE, BEFORE_FILE_FORCE, AFTER_FILE_FORCE,
        BEFORE_DIRECTORY_FORCE, AFTER_DIRECTORY_FORCE, BEFORE_READBACK, AFTER_READBACK }
    @FunctionalInterface interface FaultInjector { void at(Point point, Path candidate) throws IOException; }
    public record FileReceipt(long compressedBytes, byte[] sha256) {
        public FileReceipt {
            if (compressedBytes <= 0 || sha256 == null || sha256.length != 32)
                throw new IllegalArgumentException("Invalid file receipt");
            sha256 = sha256.clone();
        }
        @Override public byte[] sha256() { return sha256.clone(); }
    }
    public record Result(Status status, Optional<FileReceipt> receipt) {
        public Result {
            if (status == null || receipt == null
                    || (status == Status.VERIFIED_CANDIDATE_NOT_PUBLISHED) != receipt.isPresent())
                throw new IllegalArgumentException("Inconsistent candidate result");
        }
    }
    private static final Result NOT_CREATED = new Result(Status.NOT_CREATED, Optional.empty());
    private static final Result UNKNOWN = new Result(Status.UNKNOWN, Optional.empty());
    private MusleeSnapshotCandidateIO() {}

    /** No caller can use this result as commit permission or a durable execution receipt. */
    public static Result writeNewCandidate(Path destination, CompoundTag nativeSnapshot,
            UUID worldId, UUID playerId, HolderLookup.Provider registries, long maxReadNbtBytes) {
        return writeNewCandidate(destination, nativeSnapshot, worldId, playerId, registries,
                maxReadNbtBytes, (point, path) -> {});
    }

    /** Package-private failure seam used only by real temporary-file GameTests. */
    static Result writeNewCandidate(Path destination, CompoundTag nativeSnapshot,
            UUID worldId, UUID playerId, HolderLookup.Provider registries, long maxReadNbtBytes,
            FaultInjector faults) {
        if (destination == null || destination.getParent() == null || destination.getFileName() == null
                || !destination.getFileName().toString().endsWith(FILE_SUFFIX) || nativeSnapshot == null
                || maxReadNbtBytes <= 0 || faults == null) return NOT_CREATED;
        final CompoundTag detached;
        final MessageDigest digest;
        try {
            if (MusleePlayerSnapshotParticipant.read(nativeSnapshot, worldId, playerId, registries).status()
                    != MusleePlayerSnapshotParticipant.Status.VALID) return NOT_CREATED;
            detached = nativeSnapshot.copy();
            if (MusleePlayerSnapshotParticipant.read(detached, worldId, playerId, registries).status()
                    != MusleePlayerSnapshotParticipant.Status.VALID) return NOT_CREATED;
            digest = MessageDigest.getInstance("SHA-256");
            faults.at(Point.BEFORE_OPEN, destination);
        } catch (IOException | RuntimeException | NoSuchAlgorithmException rejected) { return NOT_CREATED; }

        final long size;
        // Even an open failure can have an uncertain filesystem outcome. Only CREATE_NEW's
        // explicit existing-file refusal proves this operation did not overwrite that file.
        try {
            FileChannel opened;
            try {
                opened = FileChannel.open(destination, StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            } catch (FileAlreadyExistsException exists) { return NOT_CREATED; }
            try (FileChannel channel = opened;
                 DigestOutputStream hashed = new DigestOutputStream(
                         faultingOutput(Channels.newOutputStream(channel), faults, destination), digest);
                 GZIPOutputStream compressed = new GZIPOutputStream(hashed);
                 DataOutputStream data = new DataOutputStream(compressed)) {
                faults.at(Point.AFTER_OPEN, destination);
                NbtIo.write(detached, data);
                data.flush();
                faults.at(Point.BEFORE_GZIP_FINISH, destination);
                compressed.finish();
                compressed.flush();
                faults.at(Point.AFTER_SERIALIZE, destination);
                faults.at(Point.BEFORE_FILE_FORCE, destination);
                channel.force(true);
                faults.at(Point.AFTER_FILE_FORCE, destination);
                size = channel.position();
            }
            faults.at(Point.BEFORE_DIRECTORY_FORCE, destination);
            // Unsupported directory force must fail closed, never silently downgrade.
            try (FileChannel directory = FileChannel.open(destination.getParent(),
                    StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                directory.force(true);
            }
            faults.at(Point.AFTER_DIRECTORY_FORCE, destination);
            byte[] expectedHash = digest.digest();
            faults.at(Point.BEFORE_READBACK, destination);
            if (!verifyReadback(destination, detached, worldId, playerId, registries,
                    maxReadNbtBytes, size, expectedHash)) return UNKNOWN;
            faults.at(Point.AFTER_READBACK, destination);
            return new Result(Status.VERIFIED_CANDIDATE_NOT_PUBLISHED,
                    Optional.of(new FileReceipt(size, expectedHash)));
        } catch (IOException | RuntimeException uncertain) { return UNKNOWN; }
    }

    private static OutputStream faultingOutput(OutputStream target, FaultInjector faults, Path path) {
        return new FilterOutputStream(target) {
            private boolean first = true;
            @Override public void write(int value) throws IOException {
                out.write(value); afterWrite();
            }
            @Override public void write(byte[] values, int offset, int length) throws IOException {
                out.write(values, offset, length); if (length > 0) afterWrite();
            }
            private void afterWrite() throws IOException {
                if (first) { first = false; faults.at(Point.DURING_WRITE, path); }
            }
        };
    }

    private static boolean verifyReadback(Path path, CompoundTag expected, UUID worldId, UUID playerId,
            HolderLookup.Provider registries, long maxNbtBytes, long expectedSize, byte[] expectedHash) throws IOException {
        final MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException absent) { return false; }
        try (FileChannel file = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
             DigestInputStream hashed = new DigestInputStream(Channels.newInputStream(file), digest);
             GZIPInputStream compressed = new GZIPInputStream(hashed);
             DataInputStream data = new DataInputStream(compressed)) {
            if (file.size() != expectedSize) return false;
            CompoundTag read = NbtIo.read(data, NbtAccounter.create(maxNbtBytes));
            if (data.read() != -1) return false;
            // Include every raw compressed byte, even if a decompressor ignored a suffix.
            hashed.transferTo(OutputStream.nullOutputStream());
            return file.size() == expectedSize && MessageDigest.isEqual(expectedHash, digest.digest())
                    && expected.equals(read)
                    && MusleePlayerSnapshotParticipant.read(read, worldId, playerId, registries).status()
                        == MusleePlayerSnapshotParticipant.Status.VALID;
        }
    }
}
