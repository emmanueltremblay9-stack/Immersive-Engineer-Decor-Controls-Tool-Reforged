package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeJournalEnvelope.*;

/** Synthetic codec tests only: no writer, recovery or gameplay authority is qualified. */
public final class MusleeJournalEnvelopeCases {
    private static final UUID TX = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID PRESS = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID SEGMENT = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID WORLD = UUID.fromString("aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLAYER = UUID.fromString("bbbbbbbb-bbbb-3bbb-abbb-bbbbbbbbbbbb");
    private static int assertions;
    private static void check(boolean ok, String why) {
        assertions++;
        if (!ok) throw new AssertionError(why);
    }
    private static byte[] hash(int value) { byte[] h = new byte[32]; Arrays.fill(h, (byte)value); return h; }
    private static Entry sample(Kind kind) {
        return create(kind, TX, PRESS, SEGMENT, 7, WORLD, PLAYER, hash(0x11), hash(0x22),
                hash(kind == Kind.CANCELLED ? 0x22 : 0x33), kind == Kind.ENGAGED ? new byte[0] : hash(0xab)).orElseThrow();
    }
    private static void resign(byte[] bytes) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        sha.update(bytes, 0, 112);
        sha.update(bytes, 144, bytes.length - 144);
        System.arraycopy(sha.digest(), 0, bytes, 112, 32);
    }
    private static byte[] payload(byte[] base, String text) throws Exception {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = Arrays.copyOf(base, 144 + data.length);
        ByteBuffer.wrap(bytes).putInt(8, bytes.length).putInt(20, data.length);
        System.arraycopy(data, 0, bytes, 144, data.length);
        resign(bytes);
        return bytes;
    }
    private static String text(byte[] bytes) { return new String(bytes, 144, bytes.length - 144, StandardCharsets.UTF_8); }
    private static void rejected(byte[] bytes, String reason) { check(decode(bytes).isEmpty(), reason); }
    private static void roundTrips() throws Exception {
        check(HEADER_BYTES == 144 && DIGEST_OFFSET == 112 && MAX_ENTRY_BYTES == 262144, "published framing constants");
        for (Kind kind : Kind.values()) {
            Entry entry = sample(kind);
            byte[] bytes = entry.encode();
            ByteBuffer header = ByteBuffer.wrap(bytes);
            check(Arrays.equals(Arrays.copyOf(bytes, 8), new byte[]{'I','E','D','M','U','S','J',0}), "magic");
            check(header.getInt(8) == bytes.length && header.getShort(12) == 1, "length/version");
            check(header.getShort(14) == (kind == Kind.ENGAGED ? 1 : kind == Kind.EXECUTED ? 2 : 3), "kind value");
            check(header.getInt(16) == 144 && header.getInt(20) == bytes.length - 144, "payload length");
            check(header.getLong(24) == TX.getMostSignificantBits() && header.getLong(32) == TX.getLeastSignificantBits(), "network-order transaction");
            check(header.getLong(40) == PRESS.getMostSignificantBits() && header.getLong(56) == 7 && header.getLong(64) == SEGMENT.getMostSignificantBits(), "header identity/generation");
            String expected = "{\"afterHash\":\"" + HexFormat.of().formatHex(entry.afterHash())
                    + "\",\"beforeHash\":\"" + HexFormat.of().formatHex(entry.beforeHash())
                    + "\",\"playerId\":\"" + PLAYER + "\",\"receiptHash\":\""
                    + HexFormat.of().formatHex(entry.receiptHash()) + "\",\"worldId\":\"" + WORLD + "\"}";
            check(text(bytes).equals(expected), "exact ASCII JCS schema");
            byte[] independentlySigned = bytes.clone(); resign(independentlySigned);
            check(Arrays.equals(bytes, independentlySigned), "independent digest");
            Entry decoded = decode(bytes).orElseThrow();
            check(Arrays.equals(decoded.encode(), bytes), "stable round trip");
            check(decoded.kind() == kind && decoded.transactionId().equals(TX) && decoded.pressId().equals(PRESS)
                    && decoded.segmentId().equals(SEGMENT) && decoded.writerGeneration() == 7
                    && decoded.worldId().equals(WORLD) && decoded.playerId().equals(PLAYER), "all structural fields, offline v3 player");
            check(Arrays.equals(chainHash(bytes), MessageDigest.getInstance("SHA-256").digest(bytes)), "chain hashes complete record");
            check(!Arrays.equals(chainHash(bytes), Arrays.copyOfRange(bytes, 112, 144)), "chain and entry digest differ");
            byte[] changed = bytes.clone(); changed[112] ^= 1;
            check(!Arrays.equals(chainHash(changed), chainHash(bytes)), "stored digest included in chain hash");
        }
    }
    private static void invalidInputs() {
        for (byte[] invalid : new byte[][]{null, new byte[0], new byte[143], new byte[MAX_ENTRY_BYTES + 1]}) {
            try { chainHash(invalid); throw new AssertionError("invalid chain hash bound accepted"); }
            catch (IllegalArgumentException expected) { check(true, "chain hash input bound"); }
        }
        check(create(null, TX, PRESS, SEGMENT, 1, WORLD, PLAYER, hash(1), hash(2), hash(3), new byte[0]).isEmpty(), "null kind");
        for (long generation : new long[]{0, -1, Long.MIN_VALUE})
            check(create(Kind.ENGAGED, TX, PRESS, SEGMENT, generation, WORLD, PLAYER, hash(1), hash(2), hash(3), new byte[0]).isEmpty(), "nonpositive generation");
        check(create(Kind.ENGAGED, TX, PRESS, SEGMENT, Long.MAX_VALUE, WORLD, PLAYER, hash(1), hash(2), hash(3), new byte[0]).isPresent(), "maximum generation valid structurally");
        UUID[] badIds = {null, new UUID(0, 0), PLAYER, UUID.fromString("11111111-1111-4111-0111-111111111111")};
        for (UUID bad : badIds) {
            check(create(Kind.ENGAGED, bad, PRESS, SEGMENT, 1, WORLD, PLAYER, hash(1), hash(2), hash(3), new byte[0]).isEmpty(), "bad transaction ID");
            check(create(Kind.ENGAGED, TX, bad, SEGMENT, 1, WORLD, PLAYER, hash(1), hash(2), hash(3), new byte[0]).isEmpty(), "bad press ID");
            check(create(Kind.ENGAGED, TX, PRESS, bad, 1, WORLD, PLAYER, hash(1), hash(2), hash(3), new byte[0]).isEmpty(), "bad segment ID");
        }
        for (UUID[] ids : new UUID[][]{{TX, TX, SEGMENT}, {TX, PRESS, TX}, {TX, PRESS, PRESS}})
            check(create(Kind.ENGAGED, ids[0], ids[1], ids[2], 1, WORLD, PLAYER, hash(1), hash(2), hash(3), new byte[0]).isEmpty(), "separate identity domains");
        for (UUID bad : new UUID[]{null, new UUID(0, 0), UUID.fromString("11111111-1111-4111-0111-111111111111")}) {
            check(create(Kind.ENGAGED, TX, PRESS, SEGMENT, 1, bad, PLAYER, hash(1), hash(2), hash(3), new byte[0]).isEmpty(), "bad world");
            check(create(Kind.ENGAGED, TX, PRESS, SEGMENT, 1, WORLD, bad, hash(1), hash(2), hash(3), new byte[0]).isEmpty(), "bad player");
        }
        for (byte[] bad : new byte[][]{null, new byte[0], new byte[31], new byte[33]}) {
            check(create(Kind.ENGAGED, TX, PRESS, SEGMENT, 1, WORLD, PLAYER, bad, hash(2), hash(3), new byte[0]).isEmpty(), "bad predecessor size");
            check(create(Kind.ENGAGED, TX, PRESS, SEGMENT, 1, WORLD, PLAYER, hash(1), bad, hash(3), new byte[0]).isEmpty(), "bad before size");
            check(create(Kind.ENGAGED, TX, PRESS, SEGMENT, 1, WORLD, PLAYER, hash(1), hash(2), bad, new byte[0]).isEmpty(), "bad after size");
            check(create(Kind.EXECUTED, TX, PRESS, SEGMENT, 1, WORLD, PLAYER, hash(1), hash(2), hash(3), bad).isEmpty(), "terminal receipt required");
        }
        check(create(Kind.ENGAGED, TX, PRESS, SEGMENT, 1, WORLD, PLAYER, hash(1), hash(2), hash(3), hash(4)).isEmpty(), "engaged receipt must be empty");
        check(create(Kind.CANCELLED, TX, PRESS, SEGMENT, 1, WORLD, PLAYER, hash(1), hash(2), hash(3), hash(4)).isEmpty(), "cancelled state must be unchanged");
    }
    private static void malformedBytes() throws Exception {
        byte[] good = sample(Kind.EXECUTED).encode();
        rejected(null, "null input");
        for (int size = 0; size < good.length; size++) rejected(Arrays.copyOf(good, size), "truncation " + size);
        rejected(Arrays.copyOf(good, good.length + 1), "trailing byte");
        rejected(new byte[MAX_ENTRY_BYTES + 1], "oversize buffer");
        for (int offset = 0; offset < good.length; offset++) {
            byte[] changed = good.clone(); changed[offset] ^= 1; rejected(changed, "tampering offset " + offset);
        }
        for (int offset : new int[]{8, 16, 20}) for (int length : new int[]{0, 143, 144, 262144, 262145, Integer.MAX_VALUE, Integer.MIN_VALUE, -1}) {
            byte[] changed = good.clone(); ByteBuffer.wrap(changed).putInt(offset, length); resign(changed);
            if (!Arrays.equals(changed, good)) rejected(changed, "unsigned/exact length boundary");
        }
        for (int offset : new int[]{12, 14}) for (int value : new int[]{0, 4, 65535}) {
            byte[] changed = good.clone(); ByteBuffer.wrap(changed).putShort(offset, (short)value); resign(changed); rejected(changed, "version/kind");
        }
        for (long value : new long[]{0, -1, Long.MIN_VALUE}) {
            byte[] changed = good.clone(); ByteBuffer.wrap(changed).putLong(56, value); resign(changed); rejected(changed, "wire generation");
        }
        byte[] duplicateDomain = good.clone(); System.arraycopy(good, 24, duplicateDomain, 40, 16); resign(duplicateDomain); rejected(duplicateDomain, "wire identity collision");
        for (int offset : new int[]{24, 40, 64}) {
            byte[] changed = good.clone(); changed[offset + 6] = 0x31; resign(changed); rejected(changed, "wire UUID version");
            changed = good.clone(); changed[offset + 8] = 0x01; resign(changed); rejected(changed, "wire UUID variant");
        }
        String canonical = text(good);
        String[] invalid = {
            canonical.replace("afterHash", "otherHash"),
            canonical.replace("beforeHash", "afterHash"),
            canonical.replace(PLAYER.toString(), PLAYER.toString().toUpperCase(java.util.Locale.ROOT)),
            canonical.replace("abab", "ABAB"),
            canonical.replace("\"afterHash\"", "\"\\u0061fterHash\""),
            canonical.replace("\"afterHash\":", "\"afterHash\": "),
            canonical.replace("\"receiptHash\":\"" + "ab".repeat(32) + "\"", "\"receiptHash\":\"\""),
            canonical.replace(PLAYER.toString(), "00000000-0000-0000-0000-000000000000"),
            canonical.replace("\"playerId\":\"" + PLAYER + "\",", ""),
            canonical.substring(0, canonical.length() - 1) + ",\"x\":\"y\"}",
            canonical + " ", "\ufeff" + canonical,
            canonical.replace("bbbb", "bbé"),
            canonical.replace("bbbb", "\\ud800"),
            canonical.replace(PLAYER.toString(), "bbbbbbbb-bbbb-3bbb-0bbb-bbbbbbbbbbbb"),
            canonical.replace(WORLD.toString(), "00000000-0000-0000-0000-000000000000"),
            canonical.replace("\"afterHash\":\"" + "33".repeat(32) + "\",\"beforeHash\":\"" + "22".repeat(32) + "\"",
                    "\"beforeHash\":\"" + "22".repeat(32) + "\",\"afterHash\":\"" + "33".repeat(32) + "\"")
        };
        for (String bad : invalid) { check(!bad.equals(canonical), "mutation must change payload"); rejected(payload(good, bad), "canonical rejection with valid digest"); }
        byte[] malformedUtf8 = good.clone(); malformedUtf8[160] = (byte)0xc0; malformedUtf8[161] = (byte)0xaf; resign(malformedUtf8); rejected(malformedUtf8, "malformed UTF-8 with valid digest");
        byte[] cancelled = good.clone(); ByteBuffer.wrap(cancelled).putShort(14, (short)3); resign(cancelled); rejected(cancelled, "cancelled differing state with valid digest");
    }
    private static void immutability() {
        byte[] previous = hash(1), before = hash(2), after = hash(3), receipt = hash(4);
        Entry entry = create(Kind.EXECUTED, TX, PRESS, SEGMENT, 1, WORLD, PLAYER, previous, before, after, receipt).orElseThrow();
        byte[] original = entry.encode();
        for (byte[] supplied : new byte[][]{previous, before, after, receipt}) Arrays.fill(supplied, (byte)0);
        check(Arrays.equals(original, entry.encode()), "constructor defensive copies");
        for (byte[] exposed : new byte[][]{entry.previousHash(), entry.beforeHash(), entry.afterHash(), entry.receiptHash(), entry.encode()}) Arrays.fill(exposed, (byte)0);
        check(Arrays.equals(original, entry.encode()), "accessors defensive copies");
        byte[] source = original.clone(); Entry decoded = decode(source).orElseThrow();
        check(Arrays.equals(source, original), "decode source not mutated");
        chainHash(source); check(Arrays.equals(source, original), "chain hash source not mutated");
        Arrays.fill(source, (byte)0); check(Arrays.equals(original, decoded.encode()), "decoded entry detached");
    }
    private static final class HeaderOnlyStream extends InputStream {
        final byte[] header; int position;
        HeaderOnlyStream(byte[] record) { header = Arrays.copyOf(record, 144); }
        @Override public int read() {
            if (position == 144) throw new AssertionError("invalid header caused payload read");
            return header[position++] & 255;
        }
    }
    private static void checkRead(ReadResult result, ReadStatus expected, String reason) {
        check(result.status() == expected, reason + " status");
        check(result.entry().isPresent() == (expected == ReadStatus.ENTRY), reason + " entry presence");
    }
    private static Entry readEntry(InputStream stream) throws IOException {
        ReadResult result = read(stream);
        checkRead(result, ReadStatus.ENTRY, "complete record");
        return result.entry().orElseThrow();
    }
    private static void streaming() throws Exception {
        byte[] first = sample(Kind.ENGAGED).encode(), second = sample(Kind.EXECUTED).encode();
        byte[] joined = Arrays.copyOf(first, first.length + second.length); System.arraycopy(second, 0, joined, first.length, second.length);
        ByteArrayInputStream stream = new ByteArrayInputStream(joined);
        check(Arrays.equals(readEntry(stream).encode(), first), "first framed record");
        check(stream.available() == second.length, "next record not consumed");
        check(Arrays.equals(readEntry(stream).encode(), second), "second framed record");
        checkRead(read(stream), ReadStatus.END_OF_STREAM, "clean EOF");
        checkRead(read(null), ReadStatus.INVALID, "null is invalid, not EOF");
        byte[] badChecksum = first.clone(); badChecksum[112] ^= 1;
        checkRead(read(new ByteArrayInputStream(badChecksum)), ReadStatus.INVALID, "complete record with bad checksum is invalid");
        byte[] badCanonical = payload(first, text(first).replace("afterHash", "otherHash"));
        checkRead(read(new ByteArrayInputStream(badCanonical)), ReadStatus.INVALID, "complete signed malformed schema is invalid");
        ByteArrayInputStream truncatedSecond = new ByteArrayInputStream(Arrays.copyOf(joined, joined.length - 1));
        check(Arrays.equals(readEntry(truncatedSecond).encode(), first), "complete prefix before truncated second record");
        checkRead(read(truncatedSecond), ReadStatus.INVALID, "truncated second record is not clean EOF");
        for (int size = 0; size < first.length; size++)
            checkRead(read(new ByteArrayInputStream(Arrays.copyOf(first, size))),
                    size == 0 ? ReadStatus.END_OF_STREAM : ReadStatus.INVALID, "stream truncation " + size);
        for (int offset : new int[]{8, 16, 20}) for (int value : new int[]{0, 143, 262144, 262145, Integer.MIN_VALUE, -1}) {
            byte[] bad = first.clone(); ByteBuffer.wrap(bad).putInt(offset, value);
            checkRead(read(new HeaderOnlyStream(bad)), ReadStatus.INVALID, "invalid framing rejected before payload read");
        }
        for (int total : new int[]{144, 145, 262143, 262144, 262145, Integer.MAX_VALUE, Integer.MIN_VALUE, -1}) {
            byte[] bad = first.clone(); ByteBuffer.wrap(bad).putInt(8, total).putInt(20, total - 144);
            checkRead(read(new HeaderOnlyStream(bad)), ReadStatus.INVALID, "self-consistent impossible schema lengths rejected before payload read");
        }
        for (int offset : new int[]{0, 12, 14}) {
            byte[] bad = first.clone(); bad[offset] = (byte)0xff;
            checkRead(read(new HeaderOnlyStream(bad)), ReadStatus.INVALID, "invalid magic/version/kind rejected before payload read");
        }
        byte[] invalidGeneration = first.clone(); ByteBuffer.wrap(invalidGeneration).putLong(56, 0);
        checkRead(read(new HeaderOnlyStream(invalidGeneration)), ReadStatus.INVALID, "bad generation rejected before payload read");
        byte[] collidedIds = first.clone(); System.arraycopy(first, 24, collidedIds, 40, 16);
        checkRead(read(new HeaderOnlyStream(collidedIds)), ReadStatus.INVALID, "colliding identities rejected before payload read");
        IOException failure = new IOException("synthetic transport failure");
        InputStream broken = new InputStream() { @Override public int read() throws IOException { throw failure; } };
        try { read(broken); throw new AssertionError("transport error swallowed"); }
        catch (IOException expected) { check(expected == failure, "transport failure propagated"); }
        InputStream lateFailure = new InputStream() {
            int p;
            @Override public int read() throws IOException { if (p == 144) throw failure; return first[p++] & 255; }
        };
        try { read(lateFailure); throw new AssertionError("payload transport error swallowed"); }
        catch (IOException expected) { check(expected == failure, "payload transport failure propagated"); }
    }
    public static void run() throws Exception {
        assertions = 0;
        roundTrips(); invalidInputs(); malformedBytes(); immutability(); streaming();
    }
    public static void main(String[] args) throws Exception {
        run(); System.out.println("PASS inactive envelope codec: " + assertions + " assertions; framing, canonicality, integrity and bounded streaming only");
    }
}
