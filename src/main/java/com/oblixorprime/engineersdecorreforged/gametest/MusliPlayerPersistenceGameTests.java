package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.State;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native in-memory player serialization characterization, NOT disk durability or a runtime adapter. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusliPlayerPersistenceGameTests {
    private static final String MARKER = "iedctr_test_receipt_marker_not_authority";
    private MusliPlayerPersistenceGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void native_player_snapshot_keeps_press_and_marker_but_not_cursor(GameTestHelper helper) {
        ServerPlayer original = player(helper);
        ItemStack press = stagedPress(helper);
        original.getInventory().setItem(0, press.copy());
        ItemStack cursor = new ItemStack(EngineerToolsModule.MUSLI_BAR.get(), 4);
        cursor.set(DataComponents.CUSTOM_NAME, Component.literal("Cursor output witness"));
        original.containerMenu.setCarried(cursor.copy());
        // Deliberately only a marker, NOT a transaction receipt or outcome proof.
        original.getPersistentData().putString(MARKER, "present");
        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        ServerPlayer restored = player(helper);
        restored.load(saved.copy());

        helper.assertTrue(ItemStack.matches(press, restored.getInventory().getItem(0)),
                "native player snapshot retains complete staged press components and damage");
        helper.assertTrue(MusliBarPressReserve.read(press, helper.getLevel().registryAccess()).orElseThrow()
                .sameContents(MusliBarPressReserve.read(restored.getInventory().getItem(0),
                        helper.getLevel().registryAccess()).orElseThrow()), "generic reserves round-trip");
        helper.assertTrue("present".equals(restored.getPersistentData().getString(MARKER)),
                "persistent marker survives independently of cursor");
        helper.assertTrue(restored.containerMenu.getCarried().isEmpty(),
                "native load does not reconstruct the carried output");
        helper.assertValueEqual(0, countBars(restored), "cursor output was not silently saved in inventory");
        helper.assertTrue(ItemStack.matches(cursor, original.containerMenu.getCarried()),
                "serialization does not consume or transfer original cursor");
        helper.assertValueEqual(0, countBars(original), "serialization did not move output to inventory");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void native_inventory_snapshot_does_not_distinguish_cursor_counts(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.getInventory().setItem(0, stagedPress(helper));
        player.containerMenu.setCarried(ItemStack.EMPTY);
        CompoundTag emptyCursor = player.saveWithoutId(new CompoundTag());
        for (int count : new int[]{1, 4, 64}) {
            player.containerMenu.setCarried(new ItemStack(EngineerToolsModule.MUSLI_BAR.get(), count));
            CompoundTag withCursor = player.saveWithoutId(new CompoundTag());
            helper.assertTrue(emptyCursor.getList("Inventory", Tag.TAG_COMPOUND)
                    .equals(withCursor.getList("Inventory", Tag.TAG_COMPOUND)),
                    "native inventory serialization excludes cursor count " + count);
            ServerPlayer restored = player(helper);
            restored.load(withCursor.copy());
            helper.assertTrue(restored.containerMenu.getCarried().isEmpty(), "no cursor restore for count " + count);
            helper.assertValueEqual(0, countBars(restored), "no persisted bars for count " + count);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void native_menu_removal_moves_cursor_to_inventory_when_room_exists(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertTrue(player.isAlive() && !player.hasDisconnected(), "native live not-disconnected removal branch");
        ItemStack bars = new ItemStack(EngineerToolsModule.MUSLI_BAR.get(), 4);
        bars.set(DataComponents.CUSTOM_NAME, Component.literal("Removal witness " + UUID.randomUUID()));
        player.containerMenu.setCarried(bars.copy());
        try {
            player.containerMenu.removed(player);
            helper.assertTrue(player.containerMenu.getCarried().isEmpty(), "native removal clears cursor");
            helper.assertValueEqual(4, countBars(player), "native removal returns exactly four bars to inventory");
            helper.assertTrue(drops(player, bars).isEmpty(), "room prevents world drop");
            ServerPlayer restored = player(helper);
            restored.load(player.saveWithoutId(new CompoundTag()));
            helper.assertValueEqual(4, countBars(restored), "after transfer output enters native player snapshot");
            helper.assertTrue(restored.containerMenu.getCarried().isEmpty(), "load keeps transferred output off cursor");
        } finally {
            drops(player, bars).forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void native_menu_removal_with_full_inventory_moves_cursor_to_world(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertTrue(player.isAlive() && !player.hasDisconnected(), "native live not-disconnected removal branch");
        for (int slot = 0; slot < player.getInventory().items.size(); slot++)
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        ItemStack bars = new ItemStack(EngineerToolsModule.MUSLI_BAR.get(), 4);
        bars.set(DataComponents.CUSTOM_NAME, Component.literal("Drop witness " + UUID.randomUUID()));
        player.containerMenu.setCarried(bars.copy());
        player.containerMenu.removed(player);
        var dropped = drops(player, bars);
        try {
            helper.assertTrue(player.containerMenu.getCarried().isEmpty(), "full inventory still clears cursor");
            helper.assertValueEqual(0, countBars(player), "full inventory accepts no bars");
            helper.assertValueEqual(4, dropped.stream().mapToInt(entity -> entity.getItem().getCount()).sum(),
                    "exact four-bar output crosses to world entity ownership");
            ServerPlayer restored = player(helper);
            restored.load(player.saveWithoutId(new CompoundTag()));
            helper.assertValueEqual(0, countBars(restored), "player snapshot excludes world-owned output");
            helper.assertTrue(restored.containerMenu.getCarried().isEmpty(), "no duplicate cursor on load");
        } finally {
            dropped.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    private static java.util.List<ItemEntity> drops(ServerPlayer player, ItemStack identity) {
        return player.serverLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8),
                entity -> ItemStack.isSameItemSameComponents(identity, entity.getItem()));
    }

    private static ItemStack stagedPress(GameTestHelper helper) {
        ItemStack source = new ItemStack(EngineerToolsModule.MUSLI_BAR_PRESS.get());
        source.setDamageValue(7);
        source.set(DataComponents.CUSTOM_NAME, Component.literal("Persistence witness"));
        State state = State.EMPTY;
        for (ItemStack offered : new ItemStack[]{new ItemStack(Items.BREAD, 2),
                new ItemStack(Items.APPLE, 3), new ItemStack(Items.WHEAT_SEEDS, 4)})
            state = MusliBarPressReserve.planInsert(state, offered).orElseThrow().updated();
        return MusliBarPressReserve.stage(source, helper.getLevel().registryAccess(), State.EMPTY, state).orElseThrow();
    }

    private static int countBars(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(EngineerToolsModule.MUSLI_BAR.get())) count += stack.getCount();
        }
        return count;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "MusliSaveTest"), ClientInformation.createDefault());
        // Only transport is fake. Native ServerPlayer serialization remains in use.
        player.connection = new FakePlayer(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "MusliTransport")).connection;
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        BlockPos position = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        return player;
    }
}
