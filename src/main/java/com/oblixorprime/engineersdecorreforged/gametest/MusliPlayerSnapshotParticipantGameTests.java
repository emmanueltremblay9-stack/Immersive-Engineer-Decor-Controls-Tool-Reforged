package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.State;
import com.oblixorprime.engineersdecorreforged.tools.muslee.MusleePlayerSnapshotParticipant;
import com.oblixorprime.engineersdecorreforged.tools.muslee.MusleePlayerSnapshotParticipant.Outcome;
import com.oblixorprime.engineersdecorreforged.tools.muslee.MusleePlayerSnapshotParticipant.Receipt;
import com.oblixorprime.engineersdecorreforged.tools.muslee.MusleePlayerSnapshotParticipant.Status;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercises the real inactive codec, not a runtime save/load adapter or durability proof. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusliPlayerSnapshotParticipantGameTests {
    private static final String KEY = MusleePlayerSnapshotParticipant.KEY;
    private MusliPlayerSnapshotParticipantGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void participant_stages_native_snapshot_and_detaches_every_result(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        UUID world = UUID.randomUUID();
        ItemStack cursor = bars();
        player.containerMenu.setCarried(cursor);
        CompoundTag nativeRoot = player.saveWithoutId(new CompoundTag());
        CompoundTag originalRoot = nativeRoot.copy();
        ItemStack originalCursor = cursor.copy();
        Receipt receipt = receipt();
        CompoundTag staged = MusleePlayerSnapshotParticipant.stage(nativeRoot, cursor, world,
                player.getUUID(), receipt, helper.getLevel().registryAccess()).orElseThrow();
        helper.assertTrue(nativeRoot.equals(originalRoot), "staging leaves native source NBT untouched");
        helper.assertTrue(ItemStack.matches(originalCursor, cursor), "staging leaves live cursor untouched");
        helper.assertTrue(staged.getUUID("UUID").equals(player.getUUID()), "native root UUID is preserved");
        CompoundTag withoutParticipant = staged.copy();
        withoutParticipant.remove(KEY);
        helper.assertTrue(withoutParticipant.equals(nativeRoot), "all native snapshot fields are preserved");
        var decoded = MusleePlayerSnapshotParticipant.read(staged, world, player.getUUID(),
                helper.getLevel().registryAccess()).value().orElseThrow();
        helper.assertTrue(!decoded.cursorEmpty(), "carried state is explicit");
        helper.assertTrue(decoded.receipt().equals(receipt), "receipt fields and hashes round-trip");
        decoded.receipt().afterHash()[0] = 99;
        helper.assertTrue(decoded.receipt().equals(receipt), "decoded receipt arrays are not mutable aliases");
        ItemStack first = decoded.cursorCopy(helper.getLevel().registryAccess()).orElseThrow();
        helper.assertTrue(ItemStack.matches(originalCursor, first), "count and custom data round-trip");
        first.setCount(1);
        CompoundTag changed = new CompoundTag(); changed.putString("witness", "changed");
        first.set(DataComponents.CUSTOM_DATA, CustomData.of(changed));
        helper.assertTrue(ItemStack.matches(originalCursor,
                decoded.cursorCopy(helper.getLevel().registryAccess()).orElseThrow()), "cursor copies are independent");
        cursor.setCount(2);
        cursor.set(DataComponents.CUSTOM_DATA, CustomData.of(changed));
        helper.assertTrue(ItemStack.matches(originalCursor,
                decoded.cursorCopy(helper.getLevel().registryAccess()).orElseThrow()), "source stack mutation cannot alter capture");
        staged.getCompound(KEY).getCompound("cursor").putInt("count", 1);
        staged.putString("unrelated", "returned-root-only");
        staged.getList("Pos", Tag.TAG_DOUBLE).clear();
        helper.assertTrue(nativeRoot.equals(originalRoot), "returned NBT is detached from native source");
        helper.assertTrue(ItemStack.matches(originalCursor,
                decoded.cursorCopy(helper.getLevel().registryAccess()).orElseThrow()), "decoded cursor is detached from input NBT");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void participant_valid_load_is_detached_and_does_not_restore_player(GameTestHelper helper) {
        ServerPlayer source = player(helper);
        ItemStack press = stagedPress(helper);
        source.getInventory().setItem(0, press.copy());
        UUID world = UUID.randomUUID();
        source.containerMenu.setCarried(bars());
        CompoundTag staged = MusleePlayerSnapshotParticipant.stage(source.saveWithoutId(new CompoundTag()),
                source.containerMenu.getCarried(), world, source.getUUID(), receipt(),
                helper.getLevel().registryAccess()).orElseThrow();
        CompoundTag beforeRead = staged.copy();
        ServerPlayer loaded = player(helper, source.getUUID());
        loaded.load(staged.copy());
        helper.assertTrue(loaded.getUUID().equals(source.getUUID()), "reload fixture preserves the authenticated player identity");
        helper.assertTrue(ItemStack.matches(press, loaded.getInventory().getItem(0)),
                "native inventory press retains reserves, damage and unrelated CUSTOM_NAME");
        helper.assertTrue(MusliBarPressReserve.read(press, helper.getLevel().registryAccess()).orElseThrow()
                .sameContents(MusliBarPressReserve.read(loaded.getInventory().getItem(0),
                        helper.getLevel().registryAccess()).orElseThrow()), "press generic reserves survive participant staging");
        helper.assertTrue(loaded.containerMenu.getCarried().isEmpty(), "codec installs no native cursor restoration");
        helper.assertValueEqual(0, countBars(loaded), "no synthetic inventory output on load");
        var decoded = MusleePlayerSnapshotParticipant.read(staged, world, loaded.getUUID(),
                helper.getLevel().registryAccess()).value().orElseThrow();
        helper.assertTrue(ItemStack.matches(bars(), decoded.cursorCopy(helper.getLevel().registryAccess()).orElseThrow()),
                "valid normal-player participant makes a detached cursor available only");
        helper.assertTrue(staged.equals(beforeRead), "read does not rewrite the complete snapshot");
        helper.assertTrue(loaded.containerMenu.getCarried().isEmpty(), "reading never grants cursor ownership");
        // Use a fresh output compound: reusing staged would hide native omission of unknown fields.
        CompoundTag freshNativeResave = loaded.saveWithoutId(new CompoundTag());
        helper.assertTrue(MusleePlayerSnapshotParticipant.read(freshNativeResave, world, loaded.getUUID(),
                helper.getLevel().registryAccess()).status() == Status.ABSENT,
                "inactive participant is omitted by native re-save; no receipt-retention hook is installed");
        helper.assertTrue(ItemStack.matches(bars(), source.containerMenu.getCarried()), "source retains its live cursor");
        // Test-only composition of the proposed normal-player load. No runtime hook or ownership proof.
        loaded.containerMenu.setCarried(decoded.cursorCopy(helper.getLevel().registryAccess()).orElseThrow());
        helper.assertTrue(ItemStack.matches(bars(), loaded.containerMenu.getCarried()),
                "test-only explicit load composition restores exact detached carried state");
        helper.assertValueEqual(0, countBars(loaded), "test-only cursor assignment creates no inventory duplicate");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void participant_native_close_stages_explicit_empty_with_same_receipt(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        UUID world = UUID.randomUUID();
        Receipt receipt = receipt();
        player.containerMenu.setCarried(bars());
        CompoundTag carried = MusleePlayerSnapshotParticipant.stage(player.saveWithoutId(new CompoundTag()),
                player.containerMenu.getCarried(), world, player.getUUID(), receipt,
                helper.getLevel().registryAccess()).orElseThrow();
        helper.assertTrue(player.isAlive() && !player.hasDisconnected(), "native inventory-return branch is live");
        player.containerMenu.removed(player);
        helper.assertTrue(player.containerMenu.getCarried().isEmpty(), "native close clears carried output");
        helper.assertValueEqual(4, countBars(player), "native close moves four bars into inventory");
        CompoundTag afterClose = player.saveWithoutId(new CompoundTag());
        afterClose.put(KEY, carried.getCompound(KEY).copy());
        CompoundTag beforeStage = afterClose.copy();
        CompoundTag empty = MusleePlayerSnapshotParticipant.stage(afterClose, ItemStack.EMPTY, world,
                player.getUUID(), receipt, helper.getLevel().registryAccess()).orElseThrow();
        var decoded = MusleePlayerSnapshotParticipant.read(empty, world, player.getUUID(),
                helper.getLevel().registryAccess()).value().orElseThrow();
        helper.assertTrue(decoded.cursorEmpty() && decoded.cursorCopy(helper.getLevel().registryAccess()).orElseThrow().isEmpty(),
                "transferred cursor is represented by explicit empty, not absent participant");
        helper.assertTrue(!empty.getCompound(KEY).contains("cursor"), "empty participant has no stale cursor payload");
        helper.assertTrue(empty.getCompound(KEY).getCompound("receipt").equals(carried.getCompound(KEY).getCompound("receipt")),
                "ownership transfer retains exact receipt fields");
        helper.assertTrue(afterClose.equals(beforeStage), "replacing valid participant does not mutate source");
        ServerPlayer loaded = player(helper, player.getUUID()); loaded.load(empty.copy());
        helper.assertValueEqual(4, countBars(loaded), "native inventory owns output after load");
        helper.assertTrue(ItemStack.matches(bars(), loaded.getInventory().getItem(0)),
                "native transfer and load retain exact output custom data and count");
        helper.assertTrue(loaded.containerMenu.getCarried().isEmpty(), "no duplicate cursor after native transfer");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void participant_receipt_arrays_are_owned_and_invalid_receipts_rejected(GameTestHelper helper) {
        UUID transaction = UUID.randomUUID(), press = UUID.randomUUID();
        byte[] before = new byte[32], after = new byte[32]; after[0] = 1;
        Receipt receipt = new Receipt(transaction, press, 1, Outcome.EXECUTED, before, after);
        before[0] = 7; after[0] = 8;
        helper.assertTrue(receipt.beforeHash()[0] == 0 && receipt.afterHash()[0] == 1, "constructor owns receipt arrays");
        receipt.beforeHash()[0] = 9; receipt.afterHash()[0] = 9;
        helper.assertTrue(receipt.beforeHash()[0] == 0 && receipt.afterHash()[0] == 1, "accessors never expose receipt arrays");
        byte[] equalAfter = new byte[32]; equalAfter[0] = 1;
        Receipt equivalent = new Receipt(transaction, press, 1, Outcome.EXECUTED, new byte[32], equalAfter);
        helper.assertTrue(receipt.equals(equivalent) && receipt.hashCode() == equivalent.hashCode(),
                "receipt equality and hash code compare array contents");
        rejectsReceipt(helper, () -> new Receipt(transaction, press, 0, Outcome.EXECUTED, new byte[32], new byte[32]));
        rejectsReceipt(helper, () -> new Receipt(transaction, transaction, 1, Outcome.EXECUTED, new byte[32], new byte[32]));
        rejectsReceipt(helper, () -> new Receipt(transaction, press, 1, Outcome.EXECUTED, new byte[31], new byte[32]));
        rejectsReceipt(helper, () -> new Receipt(transaction, press, 1, Outcome.NO_EFFECT, new byte[32], after));
        Receipt noEffect = new Receipt(transaction, press, 1, Outcome.NO_EFFECT, new byte[32], new byte[32]);
        helper.assertTrue(Arrays.equals(noEffect.beforeHash(), noEffect.afterHash()), "no-effect receipt requires equal hashes");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void participant_distinguishes_absent_empty_invalid_and_identity_mismatch(GameTestHelper helper) {
        ServerPlayer player = player(helper); UUID world = UUID.randomUUID();
        CompoundTag root = player.saveWithoutId(new CompoundTag());
        helper.assertTrue(MusleePlayerSnapshotParticipant.read(root, world, player.getUUID(),
                helper.getLevel().registryAccess()).status() == Status.ABSENT, "native legacy snapshot is absent");
        CompoundTag empty = MusleePlayerSnapshotParticipant.stage(root, ItemStack.EMPTY, world, player.getUUID(),
                receipt(), helper.getLevel().registryAccess()).orElseThrow();
        helper.assertTrue(MusleePlayerSnapshotParticipant.read(empty, world, player.getUUID(),
                helper.getLevel().registryAccess()).status() == Status.VALID, "explicit empty is valid, not absent");
        invalid(helper, empty, world, player.getUUID(), s -> s.put(KEY, new CompoundTag()), "empty participant compound");
        invalid(helper, empty, world, player.getUUID(), s -> s.putString(KEY, "bad"), "wrong participant type");
        invalid(helper, empty, world, player.getUUID(), s -> s.remove("UUID"), "missing native UUID");
        invalid(helper, empty, world, player.getUUID(), s -> s.putString("UUID", player.getUUID().toString()), "wrong native UUID type");
        invalid(helper, empty, world, player.getUUID(), s -> s.putUUID("UUID", UUID.randomUUID()), "foreign native player");
        invalid(helper, empty, UUID.randomUUID(), player.getUUID(), s -> {}, "foreign world");
        invalid(helper, empty, world, UUID.randomUUID(), s -> {}, "foreign expected player");
        invalid(helper, empty, world, player.getUUID(), s -> s.getCompound(KEY).put("cursor", new CompoundTag()), "empty with cursor payload");
        ItemStack named = bars(); named.set(DataComponents.CUSTOM_NAME, Component.literal("unsupported"));
        helper.assertTrue(MusleePlayerSnapshotParticipant.stage(root, named, world, player.getUUID(), receipt(),
                helper.getLevel().registryAccess()).isEmpty(), "narrow capture rejects CUSTOM_NAME rather than dropping it");
        helper.assertTrue(!root.contains(KEY) && named.has(DataComponents.CUSTOM_NAME), "failed stage preserves inputs");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void participant_strict_field_types_keys_counts_and_receipts_fail_closed(GameTestHelper helper) {
        ServerPlayer player = player(helper); UUID world = UUID.randomUUID();
        CompoundTag valid = MusleePlayerSnapshotParticipant.stage(player.saveWithoutId(new CompoundTag()), bars(), world,
                player.getUUID(), receipt(), helper.getLevel().registryAccess()).orElseThrow();
        for (String field : new String[]{"version", "worldId", "playerId", "cursorState", "receipt", "cursor"}) {
            invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).remove(field), "missing " + field);
            invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).put(field,
                    field.equals("version") ? StringTag.valueOf("1") : IntTag.valueOf(1)), "wrong type " + field);
        }
        invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).putInt("version", 2), "unknown version");
        invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).putInt("extra", 1), "unknown participant field");
        invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).putString("cursorState", "unknown"), "unknown state");
        for (String field : new String[]{"worldId", "playerId"}) {
            invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).putString(field, "invalid"), "malformed " + field);
            invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).putString(field, UUID.randomUUID().toString()), "foreign " + field);
        }
        for (int count : new int[]{-1, 0, 65})
            invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).getCompound("cursor").putInt("count", count), "cursor count " + count);
        invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).getCompound("cursor").putString("count", "4"), "cursor count type");
        invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).getCompound("cursor").putInt("extra", 1), "unknown cursor field");
        for (String field : new String[]{"transactionId", "pressId", "writerGeneration", "outcome", "beforeHash", "afterHash"}) {
            invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).getCompound("receipt").remove(field), "missing receipt " + field);
            invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).getCompound("receipt").putInt(field, 1), "wrong receipt type " + field);
        }
        invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).getCompound("receipt").putInt("extra", 1), "unknown receipt field");
        for (String field : new String[]{"transactionId", "pressId"}) {
            for (String id : new String[]{"invalid", "00000000-0000-0000-0000-000000000000", "12345678-1234-1234-9234-123456789abc",
                    "ABCDEFAB-1234-4234-9234-123456789ABC"})
                invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).getCompound("receipt").putString(field, id), "invalid receipt UUID " + field);
        }
        invalid(helper, valid, world, player.getUUID(), s -> {
            CompoundTag r = s.getCompound(KEY).getCompound("receipt"); r.putString("pressId", r.getString("transactionId"));
        }, "reused receipt identities");
        for (long generation : new long[]{0, -1})
            invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).getCompound("receipt").putLong("writerGeneration", generation), "invalid generation");
        invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).getCompound("receipt").putString("outcome", "UNKNOWN"), "unknown outcome");
        invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).getCompound("receipt").putString("outcome", "NO_EFFECT"), "no-effect unequal hashes");
        for (String field : new String[]{"beforeHash", "afterHash"})
            for (int length : new int[]{0, 31, 33})
                invalid(helper, valid, world, player.getUUID(), s -> s.getCompound(KEY).getCompound("receipt").putByteArray(field, new byte[length]), "invalid hash length");
        helper.succeed();
    }

    private static void invalid(GameTestHelper helper, CompoundTag valid, UUID world, UUID player,
                                Consumer<CompoundTag> mutation, String label) {
        CompoundTag candidate = valid.copy(); mutation.accept(candidate);
        CompoundTag before = candidate.copy();
        var result = MusleePlayerSnapshotParticipant.read(candidate, world, player, helper.getLevel().registryAccess());
        helper.assertTrue(result.status() == Status.INVALID && result.value().isEmpty(), label + " rejected");
        helper.assertTrue(MusleePlayerSnapshotParticipant.stage(candidate, ItemStack.EMPTY, world, player,
                receipt(), helper.getLevel().registryAccess()).isEmpty(), label + " cannot be silently overwritten");
        helper.assertTrue(candidate.equals(before), label + " leaves source untouched");
    }

    private static void rejectsReceipt(GameTestHelper helper, Runnable construction) {
        boolean rejected = false;
        try { construction.run(); } catch (IllegalArgumentException expected) { rejected = true; }
        helper.assertTrue(rejected, "malformed receipt constructor rejected");
    }

    private static Receipt receipt() {
        byte[] after = new byte[32]; after[0] = 1;
        return new Receipt(UUID.randomUUID(), UUID.randomUUID(), 1, Outcome.EXECUTED, new byte[32], after);
    }

    private static ItemStack bars() {
        ItemStack bars = new ItemStack(EngineerToolsModule.MUSLI_BAR.get(), 4);
        CompoundTag data = new CompoundTag(); data.putString("witness", "detached-cursor");
        data.putIntArray("nested_array", new int[]{1, 2, 3});
        bars.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return bars;
    }

    private static ItemStack stagedPress(GameTestHelper helper) {
        ItemStack press = new ItemStack(EngineerToolsModule.MUSLI_BAR_PRESS.get());
        press.setDamageValue(7);
        press.set(DataComponents.CUSTOM_NAME, Component.literal("Snapshot inventory witness"));
        State state = State.EMPTY;
        for (ItemStack offered : new ItemStack[]{new ItemStack(Items.BREAD, 2),
                new ItemStack(Items.APPLE, 3), new ItemStack(Items.WHEAT_SEEDS, 4)})
            state = MusliBarPressReserve.planInsert(state, offered).orElseThrow().updated();
        return MusliBarPressReserve.stage(press, helper.getLevel().registryAccess(), State.EMPTY, state).orElseThrow();
    }

    private static int countBars(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(EngineerToolsModule.MUSLI_BAR.get())) count += stack.getCount();
        }
        return count;
    }

    private static ServerPlayer player(GameTestHelper helper) { return player(helper, UUID.randomUUID()); }

    private static ServerPlayer player(GameTestHelper helper, UUID identity) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(identity, "MusliSnapshot"), ClientInformation.createDefault());
        player.connection = new FakePlayer(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "MusliTransport")).connection;
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return player;
    }
}
