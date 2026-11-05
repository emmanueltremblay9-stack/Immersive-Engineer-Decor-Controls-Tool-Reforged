package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Proposed, inactive player-snapshot selection framing, separate from the JCS journal.
 * Decoding proves structural integrity only: identity, current head, predecessor and
 * generation acceptance require an external reader policy. A zero predecessor is
 * structurally valid and does not authorize bootstrap or rollback.
 */
public final class MusleeSnapshotSelection {
    public static final int ENCODED_BYTES = 176;
    public static final int TOTAL_BYTES = ENCODED_BYTES;
    public static final int DIGEST_OFFSET = 144;
    public static final int VERSION = 1;
    private static final byte[] MAGIC = {'I', 'E', 'D', 'M', 'U', 'S', 'P', 0};
    private final byte[] bytes;

    private MusleeSnapshotSelection(byte[] ownedBytes) { this.bytes = ownedBytes; }

    public static Optional<MusleeSnapshotSelection> create(UUID worldId, UUID playerId,
            long writerGeneration, UUID snapshotId, long compressedLength,
            byte[] candidateSha256, byte[] predecessorSelectionSha256) {
        if (!identity(worldId) || !identity(playerId) || !identity(snapshotId)
                || snapshotId.version() != 4 || writerGeneration <= 0 || compressedLength <= 0
                || candidateSha256 == null || candidateSha256.length != 32
                || predecessorSelectionSha256 == null || predecessorSelectionSha256.length != 32) {
            return Optional.empty();
        }
        ByteBuffer b = ByteBuffer.allocate(TOTAL_BYTES).order(ByteOrder.BIG_ENDIAN);
        b.put(MAGIC).putInt(TOTAL_BYTES).putShort((short) VERSION).putShort((short) 0);
        putUuid(b, worldId); putUuid(b, playerId);
        b.putLong(writerGeneration); putUuid(b, snapshotId); b.putLong(compressedLength);
        b.put(candidateSha256).put(predecessorSelectionSha256);
        byte[] encoded = b.array();
        b.put(digest(encoded));
        return Optional.of(new MusleeSnapshotSelection(encoded));
    }

    public static Optional<MusleeSnapshotSelection> decode(byte[] encoded) {
        if (encoded == null || encoded.length != TOTAL_BYTES) return Optional.empty();
        byte[] owned = encoded.clone();
        ByteBuffer b = ByteBuffer.wrap(owned).order(ByteOrder.BIG_ENDIAN);
        for (byte magicByte : MAGIC) if (b.get() != magicByte) return Optional.empty();
        if (b.getInt() != TOTAL_BYTES || b.getShort() != VERSION || b.getShort() != 0
                || !identity(uuid(b, 16)) || !identity(uuid(b, 32))
                || b.getLong(48) <= 0 || !identity(uuid(b, 56))
                || uuid(b, 56).version() != 4 || b.getLong(72) <= 0
                || !MessageDigest.isEqual(digest(owned), Arrays.copyOfRange(owned, DIGEST_OFFSET, TOTAL_BYTES))) {
            return Optional.empty();
        }
        return Optional.of(new MusleeSnapshotSelection(owned));
    }

    public UUID worldId() { return uuid(buffer(), 16); }
    public UUID playerId() { return uuid(buffer(), 32); }
    public long writerGeneration() { return buffer().getLong(48); }
    public UUID snapshotId() { return uuid(buffer(), 56); }
    public long compressedLength() { return buffer().getLong(72); }
    public byte[] candidateSha256() { return Arrays.copyOfRange(bytes, 80, 112); }
    public byte[] predecessorSelectionSha256() { return Arrays.copyOfRange(bytes, 112, 144); }
    /** SHA-256 of bytes 0..143, not SHA-256 of the whole encoded selection. */
    public byte[] manifestDigest() { return Arrays.copyOfRange(bytes, DIGEST_OFFSET, TOTAL_BYTES); }
    public byte[] encode() { return bytes.clone(); }
    /** Filename is derived exclusively from the validated UUID; no stored path is accepted. */
    public String candidateFileName() { return snapshotId() + ".muslee-candidate.nbt.gz"; }

    private ByteBuffer buffer() { return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN); }
    private static UUID uuid(ByteBuffer b, int offset) { return new UUID(b.getLong(offset), b.getLong(offset + 8)); }
    private static void putUuid(ByteBuffer b, UUID id) { b.putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()); }
    private static boolean identity(UUID id) {
        return id != null && id.variant() == 2
                && (id.getMostSignificantBits() != 0 || id.getLeastSignificantBits() != 0);
    }
    private static byte[] digest(byte[] encoded) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(encoded, 0, DIGEST_OFFSET);
            return sha.digest();
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
