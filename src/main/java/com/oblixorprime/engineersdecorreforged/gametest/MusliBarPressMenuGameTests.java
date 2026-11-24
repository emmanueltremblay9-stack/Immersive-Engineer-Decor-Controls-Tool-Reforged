package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.menu.MusliBarPressMenu;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve;
import com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeCommandProtocol;
import com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeSessionIngress;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Real server-side GUI lifecycle GameTests, not persistence or mutation approval.
 * The old immediate-crafting tests remain historical fixtures, distinct from this
 * active right-click path. No normal/uncertain login or crash proof is asserted.
 */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusliBarPressMenuGameTests {
   private MusliBarPressMenuGameTests() {}

   private static ServerPlayer player(GameTestHelper helper) {
      ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
         new GameProfile(UUID.randomUUID(), "MusliGuiTest"), ClientInformation.createDefault());
      player.connection = new FakePlayer(helper.getLevel(),
         new GameProfile(UUID.randomUUID(), "MusliTransport")).connection;
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      return player;
   }

   private static MusliBarPressMenu open(GameTestHelper helper, ServerPlayer player, ItemStack press) {
      player.setItemInHand(InteractionHand.MAIN_HAND, press);
      press.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(player.containerMenu instanceof MusliBarPressMenu,
         "right-click should open the real server MusliBarPressMenu");
      MusliBarPressMenu menu = (MusliBarPressMenu) player.containerMenu;
      helper.assertTrue(MusleeCommandProtocol.isV4(menu.sessionId()), "server generates the session UUIDv4");
      helper.assertValueEqual(MusliBarPressMenu.PLAYER_SLOT_COUNT, menu.slots.size(),
         "menu has exactly 36 vanilla player slots and no visible ingredient or output slot");
      return menu;
   }

   private static ItemStack pressWithReserves(GameTestHelper helper, int amount) {
      ItemStack press = new ItemStack(EngineerToolsModule.MUSLI_BAR_PRESS.get());
      var state = MusliBarPressReserve.State.EMPTY;
      state = MusliBarPressReserve.planInsert(state, new ItemStack(Items.BREAD, amount)).orElseThrow().updated();
      state = MusliBarPressReserve.planInsert(state, new ItemStack(Items.APPLE, amount)).orElseThrow().updated();
      state = MusliBarPressReserve.planInsert(state, new ItemStack(Items.WHEAT_SEEDS, amount)).orElseThrow().updated();
      return MusliBarPressReserve.stage(press, helper.getLevel().registryAccess(), MusliBarPressReserve.State.EMPTY, state).orElseThrow();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void right_click_opens_real_menu_without_crafting(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      ItemStack press = new ItemStack(EngineerToolsModule.MUSLI_BAR_PRESS.get());
      player.getInventory().add(new ItemStack(Items.BREAD, 2));
      player.getInventory().add(new ItemStack(Items.APPLE, 2));
      player.getInventory().add(new ItemStack(Items.WHEAT_SEEDS, 2));
      MusliBarPressMenu menu = open(helper, player, press);
      helper.assertValueEqual(2, count(player, Items.BREAD), "open does not consume cereal");
      helper.assertValueEqual(2, count(player, Items.APPLE), "open does not consume fruit");
      helper.assertValueEqual(2, count(player, Items.WHEAT_SEEDS), "open does not consume seed");
      helper.assertValueEqual(0, press.getDamageValue(), "open never damages the tool");
      helper.assertValueEqual(0, count(player, EngineerToolsModule.MUSLI_BAR.get()), "opening never grants bars");
      helper.assertTrue(menu.stillValid(player), "held press retains menu access");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void gauges_read_server_generic_reserves(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      MusliBarPressMenu menu = open(helper, player, pressWithReserves(helper, 5));
      for (int i = 0; i < 3; i++) {
         helper.assertValueEqual(5, menu.reserve(i), "three gauges reflect only server press counts");
      }
      helper.assertTrue(!MusliBarPressMenu.TRANSACTIONS_QUALIFIED,
         "persistence and FIFO capture gates have not been waived");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void absorb_intent_cannot_change_inventory_or_reserves(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      MusliBarPressMenu menu = open(helper, player, pressWithReserves(helper, 3));
      menu.setCarried(new ItemStack(Items.BREAD, 4));
      ItemStack beforePress = player.getMainHandItem().copy();
      ItemStack beforeCursor = menu.getCarried().copy();
      byte[] command = MusleeCommandProtocol.encode(menu.sessionId(), new MusleeCommandProtocol.Command(
         UUID.randomUUID(), MusleeCommandProtocol.Type.ABSORB));
      helper.assertTrue(menu.receiveIntent(player, command) == MusleeSessionIngress.Status.RECOGNIZED,
         "server recognizes the approved v2 intent");
      helper.assertTrue(ItemStack.matches(beforePress, player.getMainHandItem()), "no press mutation without durability gate");
      helper.assertTrue(ItemStack.matches(beforeCursor, menu.getCarried()), "no cursor loss without durable commit");
      helper.assertValueEqual(3, menu.reserve(0), "cereal reserve unchanged");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void duplicate_press_intent_never_duplicates_output(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      MusliBarPressMenu menu = open(helper, player, pressWithReserves(helper, 2));
      UUID actionId = UUID.randomUUID();
      byte[] command = MusleeCommandProtocol.encode(menu.sessionId(), new MusleeCommandProtocol.Command(
         actionId, MusleeCommandProtocol.Type.PRESS));
      helper.assertTrue(menu.receiveIntent(player, command) == MusleeSessionIngress.Status.RECOGNIZED,
         "first command recognized but not committed");
      helper.assertTrue(menu.receiveIntent(player, command) == MusleeSessionIngress.Status.DUPLICATE,
         "replayed UUID cannot count as a second press");
      helper.assertValueEqual(0, count(player, EngineerToolsModule.MUSLI_BAR.get()), "no bars appeared");
      helper.assertValueEqual(2, menu.reserve(1), "no reserve decrement");
      helper.assertValueEqual(0, player.getMainHandItem().getDamageValue(), "no durability spent");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void wrong_held_item_is_not_an_authoritative_session(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      MusliBarPressMenu menu = open(helper, player, pressWithReserves(helper, 2));
      byte[] command = MusleeCommandProtocol.encode(menu.sessionId(), new MusleeCommandProtocol.Command(
         UUID.randomUUID(), MusleeCommandProtocol.Type.ABSORB));
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
      helper.assertTrue(!menu.stillValid(player), "session invalid when press leaves the bound hand");
      helper.assertTrue(menu.receiveIntent(player, command) == MusleeSessionIngress.Status.UNAUTHORIZED,
         "no stale press access");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void menu_closure_invalidates_session_without_replay(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      MusliBarPressMenu menu = open(helper, player, pressWithReserves(helper, 2));
      byte[] command = MusleeCommandProtocol.encode(menu.sessionId(), new MusleeCommandProtocol.Command(
         UUID.randomUUID(), MusleeCommandProtocol.Type.PRESS));
      menu.removed(player);
      helper.assertTrue(!menu.stillValid(player), "closed menu is invalid");
      helper.assertTrue(menu.receiveIntent(player, command) == MusleeSessionIngress.Status.CLOSED,
         "closed session never recognizes another command");
      helper.assertValueEqual(0, count(player, EngineerToolsModule.MUSLI_BAR.get()), "no replayed output");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void normal_close_returns_cursor_to_inventory_once(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      MusliBarPressMenu menu = open(helper, player, pressWithReserves(helper, 2));
      menu.setCarried(new ItemStack(Items.APPLE, 3));
      player.closeContainer();
      helper.assertTrue(menu.getCarried().isEmpty(), "native close clears carried items");
      helper.assertValueEqual(3, count(player, Items.APPLE), "native close returns cursor once");
      menu.removed(player);
      helper.assertValueEqual(3, count(player, Items.APPLE), "no replay on repeated removal");
      helper.assertValueEqual(2, MusliBarPressReserve.read(player.getMainHandItem(),
         helper.getLevel().registryAccess()).orElseThrow().cereal(), "reserves unchanged on close");
      helper.succeed();
   }

   private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
      int result = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(item)) result += stack.getCount();
      }
      return result;
   }
}
