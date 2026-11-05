package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeSnapshotSelection.*;

/** Standalone synthetic framing cases; does not qualify recovery or gameplay. */
public final class MusleeSnapshotSelectionCases {
    private static final UUID WORLD = UUID.fromString("aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLAYER = UUID.fromString("bbbbbbbb-bbbb-3bbb-abbb-bbbbbbbbbbbb");
    private static final UUID SNAPSHOT = UUID.fromString("cccccccc-cccc-4ccc-accc-cccccccccccc");
    private static int assertions;
    private static void check(boolean ok, String why) {
        assertions++;
        if (!ok) throw new AssertionError(why);
    }
    private static byte[] hash(int value) { byte[] result = new byte[32]; Arrays.fill(result, (byte) value); return result; }
    private static MusleeSnapshotSelection sample() { return create(WORLD, PLAYER, 7, SNAPSHOT, 1234, hash(17), hash(34)).orElseThrow(); }
    private static void resign(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(bytes, 144));
        System.arraycopy(digest, 0, bytes, 144, 32);
    }
    private static void rejected(byte[] bytes, String why) { check(decode(bytes).isEmpty(), why); }
    private static void roundTrips() throws Exception {
        MusleeSnapshotSelection entry = sample();
        byte[] encoded = entry.encode();
        ByteBuffer b = ByteBuffer.wrap(encoded);
        check(ENCODED_BYTES == 176 && DIGEST_OFFSET == 144, "framing constants");
        check(Arrays.equals(Arrays.copyOf(encoded, 8), new byte[]{'I','E','D','M','U','S','P',0}), "magic");
        check(b.getInt(8) == 176 && b.getShort(12) == 1 && b.getShort(14) == 0, "header");
        check(b.getLong(16) == WORLD.getMostSignificantBits() && b.getLong(24) == WORLD.getLeastSignificantBits(), "world network order");
        check(b.getLong(32) == PLAYER.getMostSignificantBits() && b.getLong(40) == PLAYER.getLeastSignificantBits(), "player network order");
        check(b.getLong(48) == 7 && b.getLong(56) == SNAPSHOT.getMostSignificantBits()
                && b.getLong(64) == SNAPSHOT.getLeastSignificantBits() && b.getLong(72) == 1234, "number and snapshot offsets");
        check(Arrays.equals(Arrays.copyOfRange(encoded, 80, 112), hash(17)), "candidate offset");
        check(Arrays.equals(Arrays.copyOfRange(encoded, 112, 144), hash(34)), "predecessor offset");
        check(Arrays.equals(entry.manifestDigest(), MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(encoded, 144))), "manifest digest excludes digest slot");
        check(!Arrays.equals(entry.manifestDigest(), MessageDigest.getInstance("SHA-256").digest(encoded)), "manifest digest is not whole-file hash");
        MusleeSnapshotSelection decoded = decode(encoded).orElseThrow();
        check(decoded.worldId().equals(WORLD) && decoded.playerId().equals(PLAYER) && decoded.snapshotId().equals(SNAPSHOT), "identities including v3 offline player");
        check(decoded.writerGeneration() == 7 && decoded.compressedLength() == 1234, "numbers");
        check(Arrays.equals(decoded.encode(), encoded), "stable round trip");
        check(decoded.candidateFileName().equals("cccccccc-cccc-4ccc-accc-cccccccccccc.muslee-candidate.nbt.gz"), "canonical derived filename");
        for (long n : new long[]{1, Integer.MAX_VALUE, (long)Integer.MAX_VALUE + 1, Long.MAX_VALUE}) {
            MusleeSnapshotSelection boundary = create(WORLD, PLAYER, n, SNAPSHOT, n, new byte[32], new byte[32]).orElseThrow();
            check(decode(boundary.encode()).orElseThrow().compressedLength() == n, "positive signed i64 bounds, zero hashes structurally allowed");
        }
    }
    private static void corruption() throws Exception {
        byte[] base = sample().encode();
        rejected(null, "null");
        for (int length = 0; length < 176; length++) rejected(Arrays.copyOf(base, length), "every truncation");
        for (int length : new int[]{177, 208, 352, 4096}) rejected(Arrays.copyOf(base, length), "no trailing data");
        for (int offset = 0; offset < 176; offset++) {
            for (int bit = 0; bit < 8; bit++) {
                byte[] changed = base.clone(); changed[offset] ^= (byte)(1 << bit);
                rejected(changed, "every single-bit mutation");
            }
        }
        for (int offset = 0; offset < 8; offset++) {
            byte[] changed = base.clone(); changed[offset] ^= 1; resign(changed); rejected(changed, "resigned bad magic");
        }
        for (int n : new int[]{0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, 175, 177}) {
            byte[] changed = base.clone(); ByteBuffer.wrap(changed).putInt(8, n); resign(changed); rejected(changed, "u32 declared length strictly fixed");
        }
        for (int offset : new int[]{12, 14}) for (int n : new int[]{2, 255, 256, 32767, 32768, 65535}) {
            byte[] changed = base.clone(); ByteBuffer.wrap(changed).putShort(offset, (short)n); resign(changed); rejected(changed, "unknown version or flags");
        }
        byte[] versionZero = base.clone(); ByteBuffer.wrap(versionZero).putShort(12, (short)0); resign(versionZero); rejected(versionZero, "zero version");
        byte[] flagOne = base.clone(); ByteBuffer.wrap(flagOne).putShort(14, (short)1); resign(flagOne); rejected(flagOne, "flag one");
        for (int offset : new int[]{48, 72}) for (long n : new long[]{0, -1, Long.MIN_VALUE}) {
            byte[] changed = base.clone(); ByteBuffer.wrap(changed).putLong(offset, n); resign(changed); rejected(changed, "nonpositive signed i64");
        }
        for (int offset : new int[]{16, 32, 56}) {
            byte[] zero = base.clone(); Arrays.fill(zero, offset, offset + 16, (byte)0); resign(zero); rejected(zero, "nil UUID");
            for (int variant : new int[]{0x00, 0x40, 0xc0, 0xe0}) {
                byte[] changed = base.clone(); changed[offset + 8] = (byte)variant; resign(changed); rejected(changed, "non-RFC variant");
            }
        }
        for (int version = 0; version <= 15; version++) if (version != 4) {
            byte[] changed = base.clone(); changed[62] = (byte)((version << 4) | 12); resign(changed); rejected(changed, "snapshot must be v4");
        }
        byte[] changedCandidate = base.clone(); changedCandidate[80] ^= 1; resign(changedCandidate);
        check(decode(changedCandidate).isPresent(), "valid new candidate digest remains structural; external reader checks actual bytes");
    }
    private static void inputsAndImmutability() {
        for (UUID bad : new UUID[]{null, new UUID(0, 0), new UUID(1, 1), new UUID(1, -1)}) {
            check(create(bad, PLAYER, 1, SNAPSHOT, 1, hash(1), hash(2)).isEmpty(), "bad world");
            check(create(WORLD, bad, 1, SNAPSHOT, 1, hash(1), hash(2)).isEmpty(), "bad player");
            check(create(WORLD, PLAYER, 1, bad, 1, hash(1), hash(2)).isEmpty(), "bad snapshot");
        }
        check(create(WORLD, PLAYER, 1, PLAYER, 1, hash(1), hash(2)).isEmpty(), "v3 snapshot rejected");
        for (long n : new long[]{0, -1, Long.MIN_VALUE}) {
            check(create(WORLD, PLAYER, n, SNAPSHOT, 1, hash(1), hash(2)).isEmpty(), "bad generation");
            check(create(WORLD, PLAYER, 1, SNAPSHOT, n, hash(1), hash(2)).isEmpty(), "bad length");
        }
        for (byte[] bad : new byte[][]{null, new byte[0], new byte[31], new byte[33], new byte[176]}) {
            check(create(WORLD, PLAYER, 1, SNAPSHOT, 1, bad, hash(2)).isEmpty(), "bad candidate hash length");
            check(create(WORLD, PLAYER, 1, SNAPSHOT, 1, hash(1), bad).isEmpty(), "bad predecessor hash length");
        }
        byte[] candidate = hash(1), predecessor = hash(2);
        MusleeSnapshotSelection entry = create(WORLD, PLAYER, 1, SNAPSHOT, 1, candidate, predecessor).orElseThrow();
        byte[] pristine = entry.encode(); candidate[0] ^= 1; predecessor[0] ^= 1;
        entry.candidateSha256()[0] ^= 1; entry.predecessorSelectionSha256()[0] ^= 1; entry.manifestDigest()[0] ^= 1; entry.encode()[0] ^= 1;
        check(Arrays.equals(pristine, entry.encode()), "create arguments and all byte getters defensive");
        byte[] input = entry.encode(), unchanged = input.clone();
        MusleeSnapshotSelection decoded = decode(input).orElseThrow();
        check(Arrays.equals(input, unchanged), "decode leaves input unchanged");
        input[0] ^= 1;
        check(Arrays.equals(decoded.encode(), unchanged), "decode owns bytes");
    }
    public static void run() throws Exception { assertions = 0; roundTrips(); corruption(); inputsAndImmutability(); }
    public static void main(String[] args) throws Exception { run(); System.out.println("PASS snapshot selection: " + assertions + " synthetic assertions"); }
}
