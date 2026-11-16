package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/**
 * Read-only validation of one externally expected selection and its candidate bytes.
 * Does not establish the expected head's authority, publish, repair, fallback, load a
 * player, restore a cursor, or qualify complete history/ownership/memory/durability.
 */
public final class MusleeSnapshotSelectionReader {
    public enum Status { VALIDATED_CANDIDATE_NOT_AUTHORITY, INVALID_EXPECTATION, INVALID_OR_UNAVAILABLE }
    public static final class Snapshot {
        private final MusleeSnapshotSelection selection;
        private final CompoundTag nativeSnapshot;
        private Snapshot(MusleeSnapshotSelection selection, CompoundTag nativeSnapshot) {
            this.selection = selection; this.nativeSnapshot = nativeSnapshot;
        }
        public MusleeSnapshotSelection selection() { return selection; }
        public CompoundTag snapshotCopy() { return nativeSnapshot.copy(); }
    }
    public record Result(Status status, Optional<Snapshot> snapshot) {
        public Result {
            if (status == null || snapshot == null
                    || (status == Status.VALIDATED_CANDIDATE_NOT_AUTHORITY) != snapshot.isPresent())
                throw new IllegalArgumentException("Inconsistent selection result");
        }
    }
    private static final Result INVALID_INPUT = new Result(Status.INVALID_EXPECTATION, Optional.empty());
    private static final Result INVALID = new Result(Status.INVALID_OR_UNAVAILABLE, Optional.empty());
    private MusleeSnapshotSelectionReader() {}

    /**
     * Current digest, predecessor/genesis context and selection generation MUST come from
     * independently verified authority, never be copied from the file being checked.
     * Trusted directory ownership/exclusion and finite qualified budgets are caller duties.
     */
    public static Result readExpected(Path manifest, Path candidateDirectory, UUID expectedWorld,
            UUID expectedPlayer, long expectedSelectionGeneration, byte[] expectedCurrentDigest,
            byte[] expectedPredecessorDigest, long maxCompressedBytes, long maxReadNbtBytes,
            HolderLookup.Provider registries) {
        if (manifest == null || candidateDirectory == null || !identity(expectedWorld) || !identity(expectedPlayer)
                || expectedSelectionGeneration <= 0 || !hash(expectedCurrentDigest) || !hash(expectedPredecessorDigest)
                || maxCompressedBytes <= 0 || maxReadNbtBytes <= 0 || registries == null) return INVALID_INPUT;
        byte[] current = expectedCurrentDigest.clone(), predecessor = expectedPredecessorDigest.clone();
        try {
            final byte[] encoded;
            try (FileChannel channel = FileChannel.open(manifest, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
                 var input = Channels.newInputStream(channel)) {
                if (channel.size() != MusleeSnapshotSelection.ENCODED_BYTES) return INVALID;
                encoded = input.readNBytes(MusleeSnapshotSelection.ENCODED_BYTES + 1);
                if (encoded.length != MusleeSnapshotSelection.ENCODED_BYTES
                        || channel.size() != MusleeSnapshotSelection.ENCODED_BYTES) return INVALID;
            }
            Optional<MusleeSnapshotSelection> parsed = MusleeSnapshotSelection.decode(encoded);
            if (parsed.isEmpty()) return INVALID;
            MusleeSnapshotSelection selection = parsed.get();
            if (!MessageDigest.isEqual(current, selection.manifestDigest())
                    || !MessageDigest.isEqual(predecessor, selection.predecessorSelectionSha256())
                    || !expectedWorld.equals(selection.worldId()) || !expectedPlayer.equals(selection.playerId())
                    || expectedSelectionGeneration != selection.writerGeneration()
                    || selection.compressedLength() > maxCompressedBytes) return INVALID;
            // No path string from disk is ever resolved. UUID spelling is fixed by the codec.
            Path candidate = candidateDirectory.resolve(selection.snapshotId().toString() + MusleeSnapshotCandidateIO.FILE_SUFFIX);
            CompoundTag nativeSnapshot = readCandidate(candidate, selection, maxReadNbtBytes);
            if (nativeSnapshot == null || MusleePlayerSnapshotParticipant.read(nativeSnapshot,
                    expectedWorld, expectedPlayer, registries).status() != MusleePlayerSnapshotParticipant.Status.VALID) return INVALID;
            // A retained transaction receipt may predate this selection's writer generation.
            return new Result(Status.VALIDATED_CANDIDATE_NOT_AUTHORITY,
                    Optional.of(new Snapshot(selection, nativeSnapshot)));
        } catch (IOException | RuntimeException unavailable) { return INVALID; }
    }

    private static CompoundTag readCandidate(Path path, MusleeSnapshotSelection selection, long nbtBudget) throws IOException {
        final MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException unavailable) { return null; }
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            if (channel.size() != selection.compressedLength()) return null;
            try (DigestInputStream hashed = new DigestInputStream(Channels.newInputStream(channel), digest);
                 GZIPInputStream gzip = new GZIPInputStream(hashed);
                 DataInputStream data = new DataInputStream(gzip)) {
                CompoundTag snapshot = NbtIo.read(data, NbtAccounter.create(nbtBudget));
                if (data.read() != -1) return null;
                hashed.transferTo(OutputStream.nullOutputStream());
                if (channel.size() != selection.compressedLength()
                        || !MessageDigest.isEqual(selection.candidateSha256(), digest.digest())) return null;
                return snapshot;
            }
        }
    }
    private static boolean identity(UUID id) { return id != null && id.variant() == 2 && !id.equals(new UUID(0, 0)); }
    private static boolean hash(byte[] value) { return value != null && value.length == 32; }
}
