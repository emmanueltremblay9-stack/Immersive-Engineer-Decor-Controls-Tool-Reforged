package com.oblixorprime.engineersdecorreforged.tools.muslee;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Detached, deliberately narrow ItemStack capture; no gameplay registration.
 * Only the enumerated vanilla component codecs are supported. Unknown/transient
 * components are refused, never silently discarded. This is not a universal mod
 * component serializer, mutation tracker, inventory transaction or memory proof.
 *
 * Encoded size is a wire/storage bound, NOT retained-heap accounting. In particular
 * codec temporary objects, decoded ItemStacks, array headers and ownership metadata
 * are not bounded by encodedSize(). Runtime FIFO admission needs a separate proven
 * complete bound and remains unavailable in this foundation.
 */
public final class MusleeItemStackCapture implements AutoCloseable {
    public static final int MAX_ENCODED_BYTES = 262144;
    private static final int MAX_DEPTH = 32;
    private byte[] encoded;
    private Object owner;
    private int releases;

    private MusleeItemStackCapture(byte[] ownedBytes) { encoded = ownedBytes; }

    /** Caller must serialize authoritative access; this does not detect concurrent ABA. */
    public static Optional<MusleeItemStackCapture> capture(ItemStack source, HolderLookup.Provider registries) {
        if (source == null || registries == null) return Optional.empty();
        try {
            byte[] bytes = encode(source, registries);
            ItemStack detached = decode(bytes, registries);
            // Verify complete effective state as well as the persisted patch. A codec
            // success or ItemStack.copy alone cannot establish this invariant.
            if (!ItemStack.matches(source, detached) || !Arrays.equals(bytes, encode(detached, registries)))
                return Optional.empty();
            return Optional.of(new MusleeItemStackCapture(bytes));
        } catch (IOException | RuntimeException unrepresentable) {
            return Optional.empty();
        }
    }

    /** Restore from private bytes with no shared mutable component state. Audited immutable defaults may be shared. */
    public synchronized Optional<ItemStack> restore(HolderLookup.Provider registries) {
        if (encoded == null || owner != null || registries == null) return Optional.empty();
        try {
            ItemStack restored = decode(encoded, registries);
            if (!Arrays.equals(encoded, encode(restored, registries))) return Optional.empty();
            return Optional.of(restored);
        } catch (IOException | RuntimeException unrepresentable) {
            return Optional.empty();
        }
    }

    /** Content comparison only; independent authoritative revision fencing is mandatory. */
    public synchronized boolean matches(ItemStack current, HolderLookup.Provider registries) {
        if (encoded == null || owner != null || current == null || registries == null) return false;
        try { return Arrays.equals(encoded, encode(current, registries)); }
        catch (IOException | RuntimeException unrepresentable) { return false; }
    }

    public synchronized int encodedSize() { return encoded == null ? 0 : encoded.length; }
    public synchronized boolean isClosed() { return encoded == null; }

    @Override public synchronized void close() {
        if (owner == null) clear();
    }

    // Package-private ownership seam. Queue tokens never escape the integration.
    synchronized boolean claim(Object token) {
        if (token == null || encoded == null || owner != null) return false;
        owner = token; return true;
    }
    synchronized boolean transfer(Object expected, Object next) {
        if (expected == null || next == null || encoded == null || owner != expected) return false;
        owner = next; return true;
    }
    synchronized void release(Object token) {
        if (token != null && owner == token) { owner = null; clear(); }
    }
    synchronized boolean ownedBy(Object token) { return encoded != null && token != null && owner == token; }
    synchronized boolean isReleased() { return encoded == null; }
    synchronized int releaseCount() { return releases; }
    private void clear() {
        if (encoded != null) { Arrays.fill(encoded, (byte) 0); encoded = null; releases++; }
    }
    /** Opaque-byte fixture only: not a valid ItemStack factory or production admission. */
    static MusleeItemStackCapture fromEncodedForOwnershipTest(byte[] bytes) {
        if (bytes == null || bytes.length > MAX_ENCODED_BYTES) throw new IllegalArgumentException();
        return new MusleeItemStackCapture(bytes.clone());
    }

    private static byte[] encode(ItemStack stack, HolderLookup.Provider registries) throws IOException {
        CompoundTag root = new CompoundTag();
        if (stack.isEmpty()) {
            if (stack != ItemStack.EMPTY) throw new IllegalArgumentException("Noncanonical empty stack may hide state");
            root.putBoolean("empty", true);
        } else {
            // ItemStack parse uses the item prototype; equal set values may collapse
            // back to that shared prototype. Never pretend to detach mutable defaults.
            // CUSTOM_DATA is the only supported mutable value and must have no default.
            if (stack.getPrototype().has(DataComponents.CUSTOM_DATA))
                throw new IllegalArgumentException("Mutable prototype component is not qualified");
            requireSafePrototype(stack.getPrototype());
            CompoundTag effective = new CompoundTag();
            for (DataComponentType<?> type : stack.getComponents().keySet()) {
                requireSupported(type);
                requireSupportedValue(stack, type);
                effective.put(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString(), encodeValue(stack, type, registries));
            }
            // A removed unsupported component must not escape the policy just because
            // it is absent from the effective map.
            for (var entry : stack.getComponentsPatch().entrySet()) requireSupported(entry.getKey());
            root.put("stack", stack.save(registries));
            root.put("effective", effective);
        }
        return MusleeExactDataOutput.encode(MAX_ENCODED_BYTES, output -> {
            output.writeByte(Tag.TAG_COMPOUND);
            output.writeUTF("");
            writeCanonical(root, output, 0);
        });
    }

    static void requireSafePrototype(net.minecraft.core.component.DataComponentMap prototype) {
        if (prototype.has(DataComponents.CUSTOM_DATA))
            throw new IllegalArgumentException("Mutable prototype component is not qualified");
        // Empty content alone is not immutability: public record constructors may
        // retain a caller-owned ArrayList. Only audited immutable default objects
        // may be shared by ItemStack.parse's prototype normalization.
        if (prototype.has(DataComponents.LORE) && prototype.get(DataComponents.LORE) != ItemLore.EMPTY)
            throw new IllegalArgumentException("Unaudited lore prototype");
        if (prototype.has(DataComponents.ATTRIBUTE_MODIFIERS) && prototype.get(DataComponents.ATTRIBUTE_MODIFIERS) != ItemAttributeModifiers.EMPTY)
            throw new IllegalArgumentException("Unaudited attribute prototype");
        if (prototype.has(DataComponents.ENCHANTMENTS) && prototype.get(DataComponents.ENCHANTMENTS) != ItemEnchantments.EMPTY)
            throw new IllegalArgumentException("Unaudited enchantment prototype");
        if (prototype.has(DataComponents.FOOD)) {
            var food = prototype.get(DataComponents.FOOD);
            if (!(food.effects() instanceof ImmutableList<?>) || !food.effects().isEmpty() || food.usingConvertsTo().isPresent())
                throw new IllegalArgumentException("Unaudited food prototype");
        }
    }

    private static void requireSupportedValue(ItemStack stack, DataComponentType<?> type) {
        // This initial adapter handles ordinary ingredient defaults, not recursive
        // item-containing food effects or arbitrary rich text/attribute graphs.
        if (type == DataComponents.FOOD) {
            var food = stack.get(DataComponents.FOOD);
            if (!food.effects().isEmpty() || food.usingConvertsTo().isPresent())
                throw new IllegalArgumentException("Nested food state is not qualified");
        }
        if (type == DataComponents.LORE && !stack.get(DataComponents.LORE).lines().isEmpty())
            throw new IllegalArgumentException("Rich text lore is not qualified");
        if (type == DataComponents.ATTRIBUTE_MODIFIERS && !stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().isEmpty())
            throw new IllegalArgumentException("Attribute graph is not qualified");
        if (type == DataComponents.ENCHANTMENTS && !stack.get(DataComponents.ENCHANTMENTS).isEmpty())
            throw new IllegalArgumentException("Enchantment graph is not qualified");
    }

    private static <T> Tag encodeValue(ItemStack stack, DataComponentType<T> type, HolderLookup.Provider registries) {
        if (type == DataComponents.CUSTOM_DATA) return stack.get(DataComponents.CUSTOM_DATA).copyTag();
        return type.codecOrThrow().encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack.get(type)).getOrThrow();
    }

    private static void requireSupported(DataComponentType<?> type) {
        if (type.isTransient() || !(type == DataComponents.CUSTOM_DATA
                || type == DataComponents.MAX_STACK_SIZE || type == DataComponents.MAX_DAMAGE
                || type == DataComponents.DAMAGE || type == DataComponents.REPAIR_COST
                || type == DataComponents.RARITY || type == DataComponents.FOOD
                || type == DataComponents.ENCHANTMENTS || type == DataComponents.ATTRIBUTE_MODIFIERS
                || type == DataComponents.LORE || type == DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
            throw new IllegalArgumentException("Component has no audited capture representation");
    }

    private static ItemStack decode(byte[] bytes, HolderLookup.Provider registries) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
        // Parser accounting is defensive only, not a proof of JVM retained heap.
        CompoundTag root = NbtIo.read(input, NbtAccounter.create(16L * MAX_ENCODED_BYTES));
        if (input.available() != 0) throw new IOException("Trailing capture data");
        if (root.getBoolean("empty")) return ItemStack.EMPTY;
        return ItemStack.parse(registries, root.getCompound("stack")).orElseThrow();
    }

    /** Standard NBT payload with deterministic compound order and preserved scalar types. */
    private static void writeCanonical(Tag tag, DataOutput output, int depth) throws IOException {
        if (depth > MAX_DEPTH) throw new IOException("Capture nesting limit");
        if (tag instanceof CompoundTag compound) {
            ArrayList<String> keys = new ArrayList<>(compound.getAllKeys());
            Collections.sort(keys);
            for (String key : keys) {
                Tag value = compound.get(key);
                if (value == null || value.getId() == Tag.TAG_END) throw new IOException("Invalid compound value");
                output.writeByte(value.getId()); output.writeUTF(key);
                writeCanonical(value, output, depth + 1);
            }
            output.writeByte(Tag.TAG_END);
        } else if (tag instanceof ListTag list) {
            output.writeByte(list.getElementType()); output.writeInt(list.size());
            for (Tag value : list) writeCanonical(value, output, depth + 1);
        } else {
            // DataOutput normalizes NaN payloads; refusing them avoids claiming an
            // exact bit-preserving representation that the NBT writer cannot provide.
            if (tag instanceof FloatTag f && !Float.isFinite(f.getAsFloat())) throw new IOException("Nonfinite float");
            if (tag instanceof DoubleTag d && !Double.isFinite(d.getAsDouble())) throw new IOException("Nonfinite double");
            tag.write(output);
        }
    }

}
