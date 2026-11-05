package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * NEW proposed format v1, inactive: framing/canonicality/integrity only.
 * No writer, persistence, authority, identity allocation, chain verification,
 * gameplay commit, outcome proof or recovery mutation is implemented here.
 */
public final class MusleeJournalEnvelope {
    public static final int HEADER_BYTES = 144;
    public static final int DIGEST_OFFSET = 112;
    public static final int MAX_ENTRY_BYTES = 262144;
    private static final byte[] MAGIC = {'I', 'E', 'D', 'M', 'U', 'S', 'J', 0};
    private static final int VERSION = 1;
    private static final int HASH_BYTES = 32;
    private static final HexFormat HEX = HexFormat.of();
    private static final String UUID_TEXT = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final Pattern PAYLOAD = Pattern.compile(
            "\\{\"afterHash\":\"([0-9a-f]{64})\",\"beforeHash\":\"([0-9a-f]{64})\","
            + "\"playerId\":\"(" + UUID_TEXT + ")\",\"receiptHash\":\"([0-9a-f]{64}|)\","
            + "\"worldId\":\"(" + UUID_TEXT + ")\"\\}");

    public enum Kind { ENGAGED, EXECUTED, CANCELLED }
    public enum ReadStatus { ENTRY, END_OF_STREAM, INVALID }

    /** Clean EOF is distinct from a corrupt or truncated record; neither proves manifest extent. */
    public record ReadResult(ReadStatus status, Optional<Entry> entry) {
        public ReadResult {
            if (status == null || entry == null || (status == ReadStatus.ENTRY) != entry.isPresent())
                throw new IllegalArgumentException("Inconsistent stream result");
        }
    }
    private static final ReadResult END = new ReadResult(ReadStatus.END_OF_STREAM, Optional.empty());
    private static final ReadResult INVALID = new ReadResult(ReadStatus.INVALID, Optional.empty());

    /** Immutable structural data. Possession of an Entry grants no authority. */
    public static final class Entry {
        private final Kind kind;
        private final UUID transactionId;
        private final UUID pressId;
        private final UUID segmentId;
        private final long writerGeneration;
        private final UUID worldId;
        private final UUID playerId;
        private final byte[] previousHash;
        private final byte[] beforeHash;
        private final byte[] afterHash;
        private final byte[] receiptHash;

        private Entry(Kind kind, UUID transactionId, UUID pressId, UUID segmentId,
                      long writerGeneration, UUID worldId, UUID playerId, byte[] previousHash,
                      byte[] beforeHash, byte[] afterHash, byte[] receiptHash) {
            this.kind = kind;
            this.transactionId = transactionId;
            this.pressId = pressId;
            this.segmentId = segmentId;
            this.writerGeneration = writerGeneration;
            this.worldId = worldId;
            this.playerId = playerId;
            // Arrays are detached by create before ownership passes here.
            this.previousHash = previousHash;
            this.beforeHash = beforeHash;
            this.afterHash = afterHash;
            this.receiptHash = receiptHash;
        }
        public Kind kind() { return kind; }
        public UUID transactionId() { return transactionId; }
        public UUID pressId() { return pressId; }
        public UUID segmentId() { return segmentId; }
        public long writerGeneration() { return writerGeneration; }
        public UUID worldId() { return worldId; }
        public UUID playerId() { return playerId; }
        public byte[] previousHash() { return previousHash.clone(); }
        public byte[] beforeHash() { return beforeHash.clone(); }
        public byte[] afterHash() { return afterHash.clone(); }
        public byte[] receiptHash() { return receiptHash.clone(); }

        public byte[] encode() {
            byte[] payload = payloadText(this).getBytes(StandardCharsets.US_ASCII);
            ByteBuffer out = ByteBuffer.allocate(HEADER_BYTES + payload.length).order(ByteOrder.BIG_ENDIAN);
            out.put(MAGIC).putInt(out.capacity()).putShort((short) VERSION).putShort((short) kindId(kind));
            out.putInt(HEADER_BYTES).putInt(payload.length);
            putUuid(out, transactionId);
            putUuid(out, pressId);
            out.putLong(writerGeneration);
            putUuid(out, segmentId);
            out.put(previousHash);
            out.position(HEADER_BYTES);
            out.put(payload);
            byte[] encoded = out.array();
            byte[] digest = MusleeJournalIntegrity.checksum(encoded, DIGEST_OFFSET).orElseThrow();
            System.arraycopy(digest, 0, encoded, DIGEST_OFFSET, HASH_BYTES);
            return encoded;
        }
    }

    private record Header(Kind kind, int totalBytes, UUID transactionId, UUID pressId,
                          UUID segmentId, long generation, byte[] predecessor) {}

    private MusleeJournalEnvelope() {}

    /** Pure construction with exact schema validation and detached input arrays. */
    public static Optional<Entry> create(Kind kind, UUID transactionId, UUID pressId, UUID segmentId,
                                         long writerGeneration, UUID worldId, UUID playerId,
                                         byte[] previousHash, byte[] beforeHash, byte[] afterHash, byte[] receiptHash) {
        if (kind == null || !distinctIds(transactionId, pressId, segmentId) || writerGeneration <= 0
                || !authorityId(worldId) || !authorityId(playerId)
                || !hash(previousHash) || !hash(beforeHash) || !hash(afterHash)
                || receiptHash == null || receiptHash.length != (kind == Kind.ENGAGED ? 0 : HASH_BYTES))
            return Optional.empty();
        byte[] previous = previousHash.clone();
        byte[] before = beforeHash.clone();
        byte[] after = afterHash.clone();
        byte[] receipt = receiptHash.clone();
        if (kind == Kind.CANCELLED && !Arrays.equals(before, after)) return Optional.empty();
        return Optional.of(new Entry(kind, transactionId, pressId, segmentId, writerGeneration,
                worldId, playerId, previous, before, after, receipt));
    }

    /** Exact one-entry decoding, with no normalization and no accepted trailing bytes. */
    public static Optional<Entry> decode(byte[] encoded) {
        if (encoded == null || encoded.length < HEADER_BYTES || encoded.length > MAX_ENTRY_BYTES)
            return Optional.empty();
        byte[] snapshot = encoded.clone();
        Optional<Header> parsed = header(snapshot);
        if (parsed.isEmpty() || parsed.get().totalBytes != snapshot.length
                || !MusleeJournalIntegrity.verifyIntegrityOnly(snapshot, DIGEST_OFFSET)) return Optional.empty();
        Header h = parsed.get();
        // The fixed schema contains only ASCII UUID/hash strings, so malformed UTF-8,
        // BOMs, escapes, surrogates and non-ASCII data cannot normalize into valid input.
        for (int i = HEADER_BYTES; i < snapshot.length; i++) {
            if (snapshot[i] < 0) return Optional.empty();
        }
        String text = new String(snapshot, HEADER_BYTES, snapshot.length - HEADER_BYTES, StandardCharsets.US_ASCII);
        var match = PAYLOAD.matcher(text);
        if (!match.matches()) return Optional.empty();
        Optional<Entry> entry = create(h.kind, h.transactionId, h.pressId, h.segmentId, h.generation,
                UUID.fromString(match.group(5)), UUID.fromString(match.group(3)), h.predecessor,
                HEX.parseHex(match.group(2)), HEX.parseHex(match.group(1)), HEX.parseHex(match.group(4)));
        // Explicit byte-exact regeneration establishes canonical fixed-schema JCS.
        return entry.isPresent() && Arrays.equals(snapshot, entry.get().encode()) ? entry : Optional.empty();
    }

    /**
     * Read ONE record. Validate the fixed header before requesting any payload bytes.
     * An invalid/truncated record is distinct from clean EOF. Transport IO failures propagate;
     * callers must stop the reader, not guess a new boundary or truncate the source.
     */
    public static ReadResult read(InputStream stream) throws IOException {
        if (stream == null) return INVALID;
        byte[] fixed = stream.readNBytes(HEADER_BYTES);
        if (fixed.length == 0) return END;
        if (fixed.length != HEADER_BYTES) return INVALID;
        Optional<Header> parsed = header(fixed);
        if (parsed.isEmpty()) return INVALID;
        int remainder = parsed.get().totalBytes - HEADER_BYTES;
        byte[] payload = stream.readNBytes(remainder);
        if (payload.length != remainder) return INVALID;
        byte[] complete = Arrays.copyOf(fixed, parsed.get().totalBytes);
        System.arraycopy(payload, 0, complete, HEADER_BYTES, remainder);
        Optional<Entry> decoded = decode(complete);
        return decoded.isPresent() ? new ReadResult(ReadStatus.ENTRY, decoded) : INVALID;
    }

    /** SHA256 over complete bytes INCLUDING entry digest; caller must first verify the entry. */
    public static byte[] chainHash(byte[] encoded) {
        if (encoded == null || encoded.length < HEADER_BYTES || encoded.length > MAX_ENTRY_BYTES)
            throw new IllegalArgumentException("Bounded verified entry required");
        try {
            return MessageDigest.getInstance("SHA-256").digest(encoded.clone());
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 unavailable", unavailable);
        }
    }

    private static Optional<Header> header(byte[] fixed) {
        if (fixed.length < HEADER_BYTES) return Optional.empty();
        for (int i = 0; i < MAGIC.length; i++) if (fixed[i] != MAGIC[i]) return Optional.empty();
        ByteBuffer in = ByteBuffer.wrap(fixed).order(ByteOrder.BIG_ENDIAN);
        in.position(8);
        long total = Integer.toUnsignedLong(in.getInt());
        int version = Short.toUnsignedInt(in.getShort());
        Kind kind = switch (Short.toUnsignedInt(in.getShort())) {
            case 1 -> Kind.ENGAGED;
            case 2 -> Kind.EXECUTED;
            case 3 -> Kind.CANCELLED;
            default -> null;
        };
        long headerBytes = Integer.toUnsignedLong(in.getInt());
        long payloadBytes = Integer.toUnsignedLong(in.getInt());
        // v1 is even tighter than the outer limit: all field sizes are fixed by kind.
        if (version != VERSION || kind == null || headerBytes != HEADER_BYTES
                || total > MAX_ENTRY_BYTES || total != headerBytes + payloadBytes
                || payloadBytes != (kind == Kind.ENGAGED ? 276 : 340)) return Optional.empty();
        UUID transaction = getUuid(in);
        UUID press = getUuid(in);
        long generation = in.getLong();
        UUID segment = getUuid(in);
        if (!distinctIds(transaction, press, segment) || generation <= 0) return Optional.empty();
        byte[] previous = new byte[HASH_BYTES];
        in.get(previous);
        return Optional.of(new Header(kind, (int) total, transaction, press, segment, generation, previous));
    }

    private static String payloadText(Entry e) {
        return "{\"afterHash\":\"" + HEX.formatHex(e.afterHash) + "\",\"beforeHash\":\"" + HEX.formatHex(e.beforeHash)
                + "\",\"playerId\":\"" + e.playerId + "\",\"receiptHash\":\"" + HEX.formatHex(e.receiptHash)
                + "\",\"worldId\":\"" + e.worldId + "\"}";
    }
    private static boolean hash(byte[] bytes) { return bytes != null && bytes.length == HASH_BYTES; }
    private static boolean randomId(UUID id) { return id != null && id.variant() == 2 && id.version() == 4; }
    private static boolean authorityId(UUID id) { return id != null && id.variant() == 2; }
    private static boolean distinctIds(UUID transaction, UUID press, UUID segment) {
        return randomId(transaction) && randomId(press) && randomId(segment)
                && !transaction.equals(press) && !transaction.equals(segment) && !press.equals(segment);
    }
    private static int kindId(Kind kind) {
        return switch (kind) { case ENGAGED -> 1; case EXECUTED -> 2; case CANCELLED -> 3; };
    }
    private static void putUuid(ByteBuffer out, UUID id) {
        out.putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits());
    }
    private static UUID getUuid(ByteBuffer in) { return new UUID(in.getLong(), in.getLong()); }
}
