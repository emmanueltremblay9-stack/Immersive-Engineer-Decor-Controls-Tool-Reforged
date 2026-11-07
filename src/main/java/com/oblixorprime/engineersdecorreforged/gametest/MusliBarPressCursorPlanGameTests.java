package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressCursorPlan;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressCursorPlan.Plan;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.State;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real server registry tests of detached plans only, not a menu or live transaction. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusliBarPressCursorPlanGameTests {
   private MusliBarPressCursorPlanGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void cursor_press_requires_room_for_all_four_bars(GameTestHelper helper) {
      State source = reserve(3, 4, 5);
      for (int count : new int[]{0, 1, 60, 61, 64}) {
         ItemStack cursor = count == 0 ? ItemStack.EMPTY : bars(count);
         ItemStack before = cursor.copy();
         var result = MusliBarPressCursorPlan.press(source, cursor, 64);
         helper.assertTrue(result.isPresent() == (count <= 60), "four-bar room boundary: " + count);
         if (result.isPresent()) {
            Plan plan = result.orElseThrow();
            helper.assertTrue(ItemStack.isSameItemSameComponents(plan.cursorAfter(), bars(1)), "default output components");
            helper.assertValueEqual(count + 4, plan.cursorAfter().getCount(), "all four bars reach cursor");
            helper.assertTrue(plan.reserveAfter().sameContents(reserve(2, 3, 4)), "exactly one unit per reserve consumed");
            helper.assertValueEqual(4, plan.barsProduced(), "one batch contains four bars");
            helper.assertValueEqual(3, plan.consumedItems(), "three reserve units consumed");
            helper.assertValueEqual(63, plan.remainingDurability(), "one durability consumed");
         }
         helper.assertTrue(ItemStack.matches(cursor, before), "source cursor unchanged at boundary " + count);
         helper.assertTrue(source.sameContents(reserve(3, 4, 5)), "source reserve unchanged");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void cursor_press_rejects_incompatible_components_and_malformed_counts(GameTestHelper helper) {
      State source = reserve(1, 1, 1);
      ItemStack named = bars(1);
      named.set(DataComponents.CUSTOM_NAME, Component.literal("Personal bar"));
      ItemStack smallerLimit = bars(1);
      smallerLimit.set(DataComponents.MAX_STACK_SIZE, 4);
      for (ItemStack cursor : new ItemStack[]{new ItemStack(Items.APPLE), named, smallerLimit, bars(65)}) {
         ItemStack before = cursor.copy();
         helper.assertTrue(MusliBarPressCursorPlan.press(source, cursor, 64).isEmpty(), "incompatible or malformed cursor rejected");
         helper.assertTrue(ItemStack.matches(cursor, before), "rejection preserves source cursor components and count");
      }
      helper.assertTrue(source.sameContents(reserve(1, 1, 1)), "rejection preserves reserves");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void cursor_press_accepts_only_qualified_durability(GameTestHelper helper) {
      State source = reserve(1, 1, 1);
      for (int durability : new int[]{0, 1, 64, 65, 128, -1, Integer.MAX_VALUE}) {
         var result = MusliBarPressCursorPlan.press(source, ItemStack.EMPTY, durability);
         helper.assertTrue(result.isPresent() == (durability == 1 || durability == 64), "qualified durability boundary: " + durability);
         if (result.isPresent()) {
            helper.assertValueEqual(durability - 1, result.orElseThrow().remainingDurability(), "exact durability subtraction");
            helper.assertTrue(result.orElseThrow().reserveAfter().sameContents(State.EMPTY), "last units consumed");
         }
      }
      helper.assertTrue(source.sameContents(reserve(1, 1, 1)), "durability planning never mutates reserve");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void cursor_press_rejects_each_missing_reserve_and_null_inputs(GameTestHelper helper) {
      for (State state : new State[]{State.EMPTY, reserve(0, 1, 1), reserve(1, 0, 1), reserve(1, 1, 0)}) {
         ItemStack cursor = bars(1);
         helper.assertTrue(MusliBarPressCursorPlan.press(state, cursor, 64).isEmpty(), "each missing ingredient prevents output");
         helper.assertValueEqual(1, cursor.getCount(), "missing ingredient does not change cursor");
      }
      helper.assertTrue(MusliBarPressCursorPlan.press(null, ItemStack.EMPTY, 64).isEmpty(), "null reserve rejected");
      helper.assertTrue(MusliBarPressCursorPlan.press(reserve(1, 1, 1), null, 64).isEmpty(), "null cursor rejected");
      helper.assertTrue(MusliBarPressCursorPlan.absorb(null, new ItemStack(Items.BREAD)).isEmpty(), "null absorption reserve rejected");
      helper.assertTrue(MusliBarPressCursorPlan.absorb(State.EMPTY, null).isEmpty(), "null absorption cursor rejected");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void cursor_absorption_pools_seed_kinds_and_preserves_leftover_components(GameTestHelper helper) {
      State source = reserve(2, 3, 0);
      Plan wheat = MusliBarPressCursorPlan.absorb(source, new ItemStack(Items.WHEAT_SEEDS, 3)).orElseThrow();
      helper.assertTrue(wheat.cursorAfter().isEmpty(), "fully absorbed cursor becomes empty");
      ItemStack melon = new ItemStack(Items.MELON_SEEDS, 7);
      melon.set(DataComponents.CUSTOM_NAME, Component.literal("Melon batch"));
      Plan mixed = MusliBarPressCursorPlan.absorb(wheat.reserveAfter(), melon).orElseThrow();
      helper.assertTrue(mixed.reserveAfter().sameContents(reserve(2, 3, 8)), "seed kinds pool and other reserves stay unchanged");
      helper.assertValueEqual(5, mixed.consumedItems(), "only available seed capacity consumed");
      helper.assertValueEqual(2, mixed.cursorAfter().getCount(), "partial absorption leaves exact remainder");
      helper.assertTrue(ItemStack.isSameItemSameComponents(melon, mixed.cursorAfter()), "leftover preserves original item and components");
      helper.assertValueEqual(0, mixed.barsProduced(), "absorption creates no bars");
      helper.assertValueEqual(-1, mixed.remainingDurability(), "absorption makes no durability claim");
      helper.assertValueEqual(7, melon.getCount(), "original cursor never consumed by staging");
      helper.assertTrue(source.sameContents(reserve(2, 3, 0)), "original reserve unchanged");
      helper.assertTrue(MusliBarPressCursorPlan.absorb(mixed.reserveAfter(), new ItemStack(Items.PUMPKIN_SEEDS)).isEmpty(), "full seed reserve rejects further seed kinds");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void cursor_absorption_caps_every_compartment_and_rejects_invalid_offers(GameTestHelper helper) {
      State source = reserve(7, 7, 7);
      ItemStack[] cursors = {new ItemStack(Items.BREAD, 64), new ItemStack(Items.APPLE, 64), new ItemStack(Items.BEETROOT_SEEDS, 64)};
      State[] expected = {reserve(8, 7, 7), reserve(7, 8, 7), reserve(7, 7, 8)};
      for (int index = 0; index < cursors.length; index++) {
         Plan plan = MusliBarPressCursorPlan.absorb(source, cursors[index]).orElseThrow();
         helper.assertTrue(plan.reserveAfter().sameContents(expected[index]), "only target compartment increases");
         helper.assertValueEqual(1, plan.consumedItems(), "capacity permits exactly one item");
         helper.assertValueEqual(63, plan.cursorAfter().getCount(), "63 items remain on cursor");
         helper.assertValueEqual(64, cursors[index].getCount(), "source cursor unchanged");
      }
      for (ItemStack rejected : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.STONE), new ItemStack(Items.BREAD, 65)}) {
         helper.assertTrue(MusliBarPressCursorPlan.absorb(source, rejected).isEmpty(), "invalid absorption offer rejected");
      }
      for (ItemStack cursor : cursors) {
         helper.assertTrue(MusliBarPressCursorPlan.absorb(reserve(8, 8, 8), cursor).isEmpty(), "full compartment cannot absorb");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void cursor_plans_own_defensive_copies_and_are_repeatable(GameTestHelper helper) {
      State source = reserve(1, 1, 1);
      ItemStack cursor = bars(1);
      Plan first = MusliBarPressCursorPlan.press(source, cursor, 2).orElseThrow();
      Plan again = MusliBarPressCursorPlan.press(source, cursor, 2).orElseThrow();
      helper.assertTrue(first.reserveAfter().sameContents(again.reserveAfter()), "repeated planning does not double consume");
      helper.assertTrue(ItemStack.matches(first.cursorAfter(), again.cursorAfter()), "repeat plans agree on output");
      ItemStack returned = first.cursorAfter();
      returned.setCount(64);
      returned.set(DataComponents.CUSTOM_NAME, Component.literal("Changed outside plan"));
      cursor.setCount(22);
      helper.assertValueEqual(5, first.cursorAfter().getCount(), "source and returned-copy mutation cannot change plan");
      helper.assertTrue(ItemStack.isSameItemSameComponents(first.cursorAfter(), bars(1)), "returned component mutation cannot change plan");
      helper.assertValueEqual(5, again.cursorAfter().getCount(), "independent plan remains unchanged");
      ItemStack ingredients = new ItemStack(Items.BREAD, 12);
      Plan absorption = MusliBarPressCursorPlan.absorb(State.EMPTY, ingredients).orElseThrow();
      Plan repeated = MusliBarPressCursorPlan.absorb(State.EMPTY, ingredients).orElseThrow();
      ingredients.setCount(1);
      absorption.cursorAfter().setCount(50);
      helper.assertValueEqual(4, absorption.cursorAfter().getCount(), "absorption owns remainder and returns copies");
      helper.assertValueEqual(4, repeated.cursorAfter().getCount(), "repeat absorption is detached");
      helper.assertValueEqual(8, absorption.reserveAfter().cereal(), "generic converted units unchanged by cursor mutation");
      helper.assertTrue(source.sameContents(reserve(1, 1, 1)), "source units unchanged by all plans");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void cursor_repeated_absorb_and_press_conserves_items_until_durability_zero(GameTestHelper helper) {
      State state = State.EMPTY;
      int durability = 64;
      int consumed = 0;
      int produced = 0;
      ItemStack output = ItemStack.EMPTY;
      for (int cycle = 0; cycle < 64; cycle++) {
         for (ItemStack ingredient : new ItemStack[]{new ItemStack(Items.BREAD), new ItemStack(Items.APPLE),
            new ItemStack(cycle % 2 == 0 ? Items.WHEAT_SEEDS : Items.MELON_SEEDS)}) {
            Plan absorb = MusliBarPressCursorPlan.absorb(state, ingredient).orElseThrow();
            state = absorb.reserveAfter();
            consumed += absorb.consumedItems();
            helper.assertTrue(absorb.cursorAfter().isEmpty(), "single ingredient fully absorbed");
            helper.assertValueEqual(1, ingredient.getCount(), "planning preserves input stack");
         }
         Plan press = MusliBarPressCursorPlan.press(state, output, durability).orElseThrow();
         state = press.reserveAfter();
         durability = press.remainingDurability();
         output = press.cursorAfter();
         produced += press.barsProduced();
         helper.assertTrue(state.sameContents(State.EMPTY), "one batch exhausts supplied units");
         helper.assertValueEqual((cycle % 16 + 1) * 4, output.getCount(), "cursor accumulation has no lost or duplicated bars");
         if (output.getCount() == 64) output = ItemStack.EMPTY; // Model the caller moving a full cursor elsewhere.
      }
      helper.assertValueEqual(192, consumed, "64 batches consume exactly 192 ingredients");
      helper.assertValueEqual(256, produced, "64 batches produce exactly 256 bars");
      helper.assertValueEqual(0, durability, "last qualified use reaches zero");
      helper.assertTrue(MusliBarPressCursorPlan.press(reserve(1, 1, 1), ItemStack.EMPTY, durability).isEmpty(), "fresh ingredients cannot bypass exhausted durability");
      helper.succeed();
   }

   private static ItemStack bars(int count) {
      return new ItemStack(EngineerToolsModule.MUSLI_BAR.get(), count);
   }

   private static State reserve(int cereal, int fruit, int seeds) {
      State state = State.EMPTY;
      if (cereal > 0) state = MusliBarPressReserve.planInsert(state, new ItemStack(Items.BREAD, cereal)).orElseThrow().updated();
      if (fruit > 0) state = MusliBarPressReserve.planInsert(state, new ItemStack(Items.APPLE, fruit)).orElseThrow().updated();
      if (seeds > 0) state = MusliBarPressReserve.planInsert(state, new ItemStack(Items.WHEAT_SEEDS, seeds)).orElseThrow().updated();
      return state;
   }
}
