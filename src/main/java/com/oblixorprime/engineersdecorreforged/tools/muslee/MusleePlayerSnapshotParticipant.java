package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Inactive participant codec for a COMPLETE normal player snapshot, never the journal.
 * No save/load hooks, restoration, file access, ownership grant or mutation is installed.
 * Structural validity is not durable outcome evidence or permission to restore a cursor.
 * Whole-player serialization/copy costs are NOT qualified aggregate FIFO memory bounds.
 */
public final class MusleePlayerSnapshotParticipant {
    public static final String KEY = "iedctr:muslee_player_participant";
    public enum Status { ABSENT, VALID, INVALID }
    public enum Outcome { EXECUTED, NO_EFFECT }
    private static final Set<String> EMPTY_KEYS = Set.of("version", "worldId", "playerId", "cursorState", "receipt");
    private static final Set<String> CARRIED_KEYS = Set.of("version", "worldId", "playerId", "cursorState", "receipt", "cursor");
    private static final Set<String> RECEIPT_KEYS = Set.of("transactionId", "pressId", "writerGeneration", "outcome", "beforeHash", "afterHash");

    /** Proposed minimal receipt schema. Hashes are supplied claims, not independently proven outcomes. */
    public record Receipt(UUID transactionId, UUID pressId, long writerGeneration,
                          Outcome outcome, byte[] beforeHash, byte[] afterHash) {
        public Receipt {
            if (!randomId(transactionId) || !randomId(pressId) || transactionId.equals(pressId)
                    || writerGeneration <= 0 || outcome == null || beforeHash == null || afterHash == null
                    || beforeHash.length != 32 || afterHash.length != 32)
                throw new IllegalArgumentException("Invalid receipt structure");
            beforeHash = beforeHash.clone();
            afterHash = afterHash.clone();
            if (outcome == Outcome.NO_EFFECT && !Arrays.equals(beforeHash, afterHash))
                throw new IllegalArgumentException("No-effect state hashes must agree");
        }
        @Override public byte[] beforeHash() { return beforeHash.clone(); }
        @Override public byte[] afterHash() { return afterHash.clone(); }
        @Override public boolean equals(Object other) {
            return other instanceof Receipt r && transactionId.equals(r.transactionId)
                    && pressId.equals(r.pressId) && writerGeneration == r.writerGeneration
                    && outcome == r.outcome && Arrays.equals(beforeHash, r.beforeHash)
                    && Arrays.equals(afterHash, r.afterHash);
        }
        @Override public int hashCode() {
            int value = java.util.Objects.hash(transactionId, pressId, writerGeneration, outcome);
            return 31 * (31 * value + Arrays.hashCode(beforeHash)) + Arrays.hashCode(afterHash);
        }
    }

    public static final class Decoded {
        private final ItemStack cursor;
        private final Receipt receipt;
        private Decoded(ItemStack cursor, Receipt receipt) { this.cursor = cursor; this.receipt = receipt; }
        public Receipt receipt() { return receipt; }
        public Optional<ItemStack> cursorCopy(HolderLookup.Provider registries) { return detach(cursor, registries); }
        public boolean cursorEmpty() { return cursor.isEmpty(); }
    }
    public record ReadResult(Status status, Optional<Decoded> value) {
        public ReadResult {
            if (status == null || value == null || (status == Status.VALID) != value.isPresent())
                throw new IllegalArgumentException("Inconsistent participant result");
        }
    }
    private static final ReadResult ABSENT = new ReadResult(Status.ABSENT, Optional.empty());
    private static final ReadResult INVALID = new ReadResult(Status.INVALID, Optional.empty());
    private MusleePlayerSnapshotParticipant() {}

    /**
     * Stage a new complete native snapshot, with explicit current cursor ownership and retained receipt.
     * Existing malformed participant data is never silently overwritten. Replacing a valid participant
     * still requires external authoritative transaction/ownership validation; this method provides none.
     */
    public static Optional<CompoundTag> stage(CompoundTag nativeSnapshot, ItemStack cursor,
            UUID worldId, UUID playerId, Receipt receipt, HolderLookup.Provider registries) {
        if (!identity(worldId) || !identity(playerId) || receipt == null || registries == null
                || !nativePlayer(nativeSnapshot, playerId)) return Optional.empty();
        if (read(nativeSnapshot, worldId, playerId, registries).status == Status.INVALID) return Optional.empty();
        Optional<ItemStack> detached = detach(cursor, registries);
        if (detached.isEmpty()) return Optional.empty();
        try {
            CompoundTag participant = new CompoundTag();
            participant.putInt("version", 1);
            participant.putString("worldId", worldId.toString());
            participant.putString("playerId", playerId.toString());
            participant.putString("cursorState", detached.get().isEmpty() ? "empty" : "carried");
            if (!detached.get().isEmpty()) participant.put("cursor", detached.get().save(registries));
            participant.put("receipt", encodeReceipt(receipt));
            CompoundTag candidate = nativeSnapshot.copy();
            candidate.put(KEY, participant);
            return read(candidate, worldId, playerId, registries).status == Status.VALID
                    ? Optional.of(candidate) : Optional.empty();
        } catch (RuntimeException invalid) { return Optional.empty(); }
    }

    /** ABSENT means nonparticipating/legacy only; known participants must block on absence externally. */
    public static ReadResult read(CompoundTag snapshot, UUID worldId, UUID playerId,
                                  HolderLookup.Provider registries) {
        if (!identity(worldId) || !identity(playerId) || registries == null || !nativePlayer(snapshot, playerId)) return INVALID;
        if (!snapshot.contains(KEY)) return ABSENT;
        try {
            if (!snapshot.contains(KEY, Tag.TAG_COMPOUND)) return INVALID;
            CompoundTag p = snapshot.getCompound(KEY);
            if (!p.contains("version", Tag.TAG_INT) || p.getInt("version") != 1
                    || !p.contains("worldId", Tag.TAG_STRING) || !worldId.toString().equals(p.getString("worldId"))
                    || !p.contains("playerId", Tag.TAG_STRING) || !playerId.toString().equals(p.getString("playerId"))
                    || !p.contains("cursorState", Tag.TAG_STRING) || !p.contains("receipt", Tag.TAG_COMPOUND)) return INVALID;
            Receipt receipt = decodeReceipt(p.getCompound("receipt"));
            ItemStack cursor;
            switch (p.getString("cursorState")) {
                case "empty" -> {
                    if (!p.getAllKeys().equals(EMPTY_KEYS)) return INVALID;
                    cursor = ItemStack.EMPTY;
                }
                case "carried" -> {
                    if (!p.getAllKeys().equals(CARRIED_KEYS) || !p.contains("cursor", Tag.TAG_COMPOUND)) return INVALID;
                    CompoundTag encoded = p.getCompound("cursor");
                    Optional<ItemStack> parsed = ItemStack.parse(registries, encoded);
                    if (parsed.isEmpty() || parsed.get().isEmpty() || parsed.get().getCount() <= 0
                            || parsed.get().getCount() > parsed.get().getMaxStackSize()
                            || !parsed.get().save(registries).equals(encoded)) return INVALID;
                    Optional<ItemStack> detached = detach(parsed.get(), registries);
                    if (detached.isEmpty()) return INVALID;
                    cursor = detached.get();
                }
                default -> { return INVALID; }
            }
            return new ReadResult(Status.VALID, Optional.of(new Decoded(cursor, receipt)));
        } catch (RuntimeException invalid) { return INVALID; }
    }

    private static Optional<ItemStack> detach(ItemStack cursor, HolderLookup.Provider registries) {
        if (cursor == null || registries == null || (!cursor.isEmpty()
                && (cursor.getCount() <= 0 || cursor.getCount() > cursor.getMaxStackSize()))) return Optional.empty();
        Optional<MusleeItemStackCapture> capture = MusleeItemStackCapture.capture(cursor, registries);
        if (capture.isEmpty()) return Optional.empty();
        try (MusleeItemStackCapture owned = capture.get()) { return owned.restore(registries); }
    }
    private static CompoundTag encodeReceipt(Receipt r) {
        CompoundTag tag = new CompoundTag();
        tag.putString("transactionId", r.transactionId.toString());
        tag.putString("pressId", r.pressId.toString());
        tag.putLong("writerGeneration", r.writerGeneration);
        tag.putString("outcome", r.outcome.name());
        tag.putByteArray("beforeHash", r.beforeHash());
        tag.putByteArray("afterHash", r.afterHash());
        return tag;
    }
    private static Receipt decodeReceipt(CompoundTag tag) {
        if (!tag.getAllKeys().equals(RECEIPT_KEYS) || !tag.contains("transactionId", Tag.TAG_STRING)
                || !tag.contains("pressId", Tag.TAG_STRING) || !tag.contains("writerGeneration", Tag.TAG_LONG)
                || !tag.contains("outcome", Tag.TAG_STRING) || !tag.contains("beforeHash", Tag.TAG_BYTE_ARRAY)
                || !tag.contains("afterHash", Tag.TAG_BYTE_ARRAY)) throw new IllegalArgumentException("Invalid receipt fields");
        UUID transaction = UUID.fromString(tag.getString("transactionId"));
        UUID press = UUID.fromString(tag.getString("pressId"));
        if (!transaction.toString().equals(tag.getString("transactionId"))
                || !press.toString().equals(tag.getString("pressId"))) throw new IllegalArgumentException("Noncanonical UUID");
        return new Receipt(transaction, press, tag.getLong("writerGeneration"), Outcome.valueOf(tag.getString("outcome")),
                tag.getByteArray("beforeHash"), tag.getByteArray("afterHash"));
    }
    private static boolean nativePlayer(CompoundTag snapshot, UUID playerId) {
        try { return snapshot != null && snapshot.hasUUID("UUID") && snapshot.getUUID("UUID").equals(playerId); }
        catch (RuntimeException invalid) { return false; }
    }
    private static boolean identity(UUID id) { return id != null && id.variant() == 2 && !id.equals(new UUID(0, 0)); }
    private static boolean randomId(UUID id) { return identity(id) && id.version() == 4; }
}
