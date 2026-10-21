package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.serialization.Codec;
import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressItem;
import com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeItemStackCapture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusleeItemStackCaptureGameTests {
    private MusleeItemStackCaptureGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void capture_ordinary_ingredients_and_empty(GameTestHelper h) {
        var lookup = h.getLevel().registryAccess();
        for (Item item : new Item[]{Items.BREAD, Items.APPLE, Items.WHEAT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.BEETROOT_SEEDS}) {
            ItemStack source = new ItemStack(item, 8);
            try (var capture = MusleeItemStackCapture.capture(source, lookup).orElseThrow()) {
                h.assertTrue(capture.matches(source, lookup), "ordinary ingredient captured exactly");
                h.assertTrue(ItemStack.matches(source, capture.restore(lookup).orElseThrow()), "ordinary ingredient restored");
                h.assertTrue(capture.encodedSize() > 0, "encoding exists without claiming heap size");
            }
        }
        try (var empty = MusleeItemStackCapture.capture(ItemStack.EMPTY, lookup).orElseThrow()) {
            h.assertTrue(empty.restore(lookup).orElseThrow().isEmpty(), "empty location remains empty");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void capture_nested_custom_data_has_no_mutable_alias(GameTestHelper h) {
        var lookup = h.getLevel().registryAccess();
        CompoundTag nested = new CompoundTag(); nested.putInt("value", 12);
        ListTag list = new ListTag(); list.add(StringTag.valueOf("first"));
        CompoundTag data = new CompoundTag(); data.put("nested", nested); data.put("list", list); data.putIntArray("array", new int[]{1, 2, 3});
        ItemStack source = new ItemStack(Items.BREAD, 4); source.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        try (var capture = MusleeItemStackCapture.capture(source, lookup).orElseThrow()) {
            // Deliberately exercise in-place mutation, not merely replacement of the component.
            source.get(DataComponents.CUSTOM_DATA).getUnsafe().getCompound("nested").putInt("value", 99);
            source.setCount(1);
            ItemStack first = capture.restore(lookup).orElseThrow();
            h.assertValueEqual(4, first.getCount(), "count detached");
            h.assertValueEqual(12, first.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("nested").getInt("value"), "nested component detached");
            first.get(DataComponents.CUSTOM_DATA).getUnsafe().getCompound("nested").putInt("value", 77);
            ItemStack second = capture.restore(lookup).orElseThrow();
            h.assertValueEqual(12, second.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("nested").getInt("value"), "each decode independently detached");
            h.assertTrue(!capture.matches(source, lookup), "source mutation no longer matches");
            h.assertTrue(capture.matches(second, lookup), "snapshot unchanged by decoded alias mutation");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void capture_default_component_removal_is_preserved(GameTestHelper h) {
        var lookup = h.getLevel().registryAccess();
        ItemStack source = new ItemStack(Items.BREAD, 3);
        h.assertTrue(source.has(DataComponents.FOOD), "bread has default food component");
        source.remove(DataComponents.FOOD);
        try (var capture = MusleeItemStackCapture.capture(source, lookup).orElseThrow()) {
            ItemStack restored = capture.restore(lookup).orElseThrow();
            h.assertTrue(!restored.has(DataComponents.FOOD), "default component remains explicitly removed");
            h.assertTrue(source.getComponentsPatch().equals(restored.getComponentsPatch()), "removal patch retained");
            h.assertTrue(!capture.matches(new ItemStack(Items.BREAD, 3), lookup), "absence differs from default presence");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void capture_canonical_key_order_and_numeric_types(GameTestHelper h) {
        var lookup = h.getLevel().registryAccess();
        CompoundTag a = new CompoundTag(); a.putInt("Aa", 1); a.putInt("BB", 2); a.putByte("typed", (byte) 3);
        CompoundTag b = new CompoundTag(); b.putByte("typed", (byte) 3); b.putInt("BB", 2); b.putInt("Aa", 1);
        ItemStack first = tagged(a), second = tagged(b);
        try (var capture = MusleeItemStackCapture.capture(first, lookup).orElseThrow()) {
            h.assertTrue(capture.matches(second, lookup), "compound insertion order, including colliding keys, canonicalized");
            b.putInt("typed", 3); second.set(DataComponents.CUSTOM_DATA, CustomData.of(b));
            h.assertTrue(!capture.matches(second, lookup), "equal numeric value with another NBT type differs");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void capture_refuses_transient_lossy_and_unproven_types(GameTestHelper h) {
        var lookup = h.getLevel().registryAccess();
        DataComponentType<Integer> transientType = DataComponentType.<Integer>builder().networkSynchronized(ByteBufCodecs.INT).build();
        DataComponentType<Integer> lossyType = DataComponentType.<Integer>builder().persistent(Codec.unit(0)).build();
        ItemStack transientStack = new ItemStack(Items.BREAD); transientStack.set(transientType, 7);
        ItemStack lossyStack = new ItemStack(Items.BREAD); lossyStack.set(lossyType, 9);
        h.assertTrue(MusleeItemStackCapture.capture(transientStack, lookup).isEmpty(), "transient component cannot silently disappear");
        h.assertTrue(MusleeItemStackCapture.capture(lossyStack, lookup).isEmpty(), "unknown lossy codec never admitted");
        h.assertValueEqual(Integer.valueOf(7), transientStack.get(transientType), "rejection leaves transient source intact");
        h.assertValueEqual(Integer.valueOf(9), lossyStack.get(lossyType), "rejection leaves lossy source intact");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void capture_refuses_unrepresentable_and_oversized_values(GameTestHelper h) {
        var lookup = h.getLevel().registryAccess();
        CompoundTag nonfinite = new CompoundTag(); nonfinite.putFloat("value", Float.NaN);
        h.assertTrue(MusleeItemStackCapture.capture(tagged(nonfinite), lookup).isEmpty(), "NaN payload cannot be normalized silently");
        CompoundTag large = new CompoundTag(); large.putByteArray("large", new byte[MusleeItemStackCapture.MAX_ENCODED_BYTES]);
        h.assertTrue(MusleeItemStackCapture.capture(tagged(large), lookup).isEmpty(), "bounded encoding rejects oversized data");
        CompoundTag deep = new CompoundTag(), cursor = deep;
        for (int i = 0; i < 40; i++) { CompoundTag next = new CompoundTag(); cursor.put("child", next); cursor = next; }
        h.assertTrue(MusleeItemStackCapture.capture(tagged(deep), lookup).isEmpty(), "unsupported nesting refused");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void capture_close_is_terminal_and_idempotent(GameTestHelper h) {
        var lookup = h.getLevel().registryAccess();
        ItemStack source = new ItemStack(Items.APPLE, 3);
        var capture = MusleeItemStackCapture.capture(source, lookup).orElseThrow();
        capture.close(); capture.close();
        h.assertTrue(capture.isClosed(), "closed permanently");
        h.assertValueEqual(0, capture.encodedSize(), "owned encoded storage released");
        h.assertTrue(capture.restore(lookup).isEmpty(), "cannot restore after release");
        h.assertTrue(!capture.matches(source, lookup), "released capture never validates");
        h.assertValueEqual(3, source.getCount(), "closing does not mutate source");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void capture_seed_identity_survives_real_tag_revocation(GameTestHelper h) {
        var registry = BuiltInRegistries.ITEM;
        Map<TagKey<Item>, List<Holder<Item>>> original = new HashMap<>();
        registry.getTags().forEach(pair -> original.put(pair.getFirst(), pair.getSecond().stream().toList()));
        TagKey<Item> tag = MusliBarPressItem.ACCEPTED_SEEDS;
        h.assertTrue(original.containsKey(tag), "real seed tag exists");
        Map<TagKey<Item>, List<Holder<Item>>> allowed = new HashMap<>(original);
        List<Holder<Item>> values = new ArrayList<>(original.get(tag)); values.add(Items.TORCH.builtInRegistryHolder()); allowed.put(tag, values);
        Map<TagKey<Item>, List<Holder<Item>>> revoked = new HashMap<>(original);
        revoked.put(tag, original.get(tag).stream().filter(holder -> holder.value() != Items.TORCH).toList());
        try {
            registry.bindTags(allowed);
            ItemStack source = new ItemStack(Items.TORCH, 4);
            h.assertTrue(source.is(tag), "extension actually admitted by tag");
            try (var capture = MusleeItemStackCapture.capture(source, h.getLevel().registryAccess()).orElseThrow()) {
                registry.bindTags(revoked);
                ItemStack restored = capture.restore(h.getLevel().registryAccess()).orElseThrow();
                h.assertTrue(!restored.is(tag), "real revocation visible");
                h.assertTrue(ItemStack.matches(source, restored), "capture retains exact identity/count independent of admissibility");
                h.assertTrue(capture.matches(source, h.getLevel().registryAccess()), "tag membership is not structural capture state");
            }
        } finally { registry.bindTags(original); }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void capture_refuses_hidden_empty_and_unqualified_text(GameTestHelper h) {
        var lookup = h.getLevel().registryAccess();
        ItemStack hidden = tagged(new CompoundTag()); hidden.setCount(0);
        h.assertTrue(hidden.isEmpty(), "fixture logically empty but owns stack data");
        h.assertTrue(MusleeItemStackCapture.capture(hidden, lookup).isEmpty(), "noncanonical empty cannot erase hidden state");
        ItemStack named = new ItemStack(Items.BREAD); named.set(DataComponents.CUSTOM_NAME, Component.literal("Unqualified text"));
        h.assertTrue(MusleeItemStackCapture.capture(named, lookup).isEmpty(), "rich text remains explicitly unsupported, not silently dropped");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void capture_observe_runtime_profile_without_qualification(GameTestHelper h) {
        for (String property : new String[]{"java.runtime.version", "java.vm.name", "java.vm.vendor", "os.name", "os.arch"})
            System.out.println("MUSLEE_CAPTURE_PROFILE_CANDIDATE " + property + "=" + System.getProperty(property, "UNAVAILABLE"));
        try {
            HotSpotDiagnosticMXBean bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            for (String option : new String[]{"ObjectAlignmentInBytes", "UseCompressedOops", "UseCompressedClassPointers"}) {
                try {
                    String value = bean == null ? "UNAVAILABLE" : bean.getVMOption(option).getValue();
                    System.out.println("MUSLEE_CAPTURE_PROFILE_CANDIDATE " + option + "=" + value);
                } catch (RuntimeException unavailable) {
                    System.out.println("MUSLEE_CAPTURE_PROFILE_CANDIDATE " + option + "=UNAVAILABLE");
                }
            }
        } catch (RuntimeException | LinkageError unavailable) {
            System.out.println("MUSLEE_CAPTURE_PROFILE_CANDIDATE vm_options=UNAVAILABLE");
        }
        System.out.println("MUSLEE_CAPTURE_PROFILE_CANDIDATE qualified=false; observation_only=true");
        h.succeed();
    }

    private static ItemStack tagged(CompoundTag data) {
        ItemStack stack = new ItemStack(Items.BREAD, 2); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); return stack;
    }
}
