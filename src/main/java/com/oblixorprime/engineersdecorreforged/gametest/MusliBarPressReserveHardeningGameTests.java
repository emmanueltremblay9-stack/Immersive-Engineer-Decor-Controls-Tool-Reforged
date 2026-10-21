package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.Plan;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.State;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Focused regressions for the existing PR #68 reserve core; no runtime press operation. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusliBarPressReserveHardeningGameTests {
   private MusliBarPressReserveHardeningGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void custom_stack_limit_below_eight_caps_reserve(GameTestHelper helper) {
      ItemStack firstOffer = new ItemStack(Items.BREAD, 2);
      firstOffer.set(DataComponents.MAX_STACK_SIZE, 3);
      Plan first = MusliBarPressReserve.planInsert(State.EMPTY, firstOffer).orElseThrow();
      helper.assertValueEqual(2, first.count(), "first transfer retains both items");
      ItemStack secondOffer = new ItemStack(Items.BREAD, 3);
      secondOffer.set(DataComponents.MAX_STACK_SIZE, 3);
      Plan second = MusliBarPressReserve.planInsert(first.updated(), secondOffer).orElseThrow();
      helper.assertValueEqual(1, second.count(), "only remaining native slot capacity accepted");
      helper.assertValueEqual(3, second.updated().bread().getCount(), "native stack cap respected");
      helper.assertValueEqual(3, secondOffer.getCount(), "pure plan cannot mutate offered source");
      helper.assertTrue(MusliBarPressReserve.planInsert(second.updated(), secondOffer).isEmpty(), "native maximum prevents further addition");
      HolderLookup.Provider lookup = helper.getLevel().registryAccess();
      ItemStack press = new ItemStack(EngineerToolsModule.MUSLI_BAR_PRESS.get());
      ItemStack staged = MusliBarPressReserve.stage(press, lookup, State.EMPTY, second.updated()).orElseThrow();
      ItemStack reloaded = MusliBarPressReserve.read(staged, lookup).orElseThrow().bread();
      helper.assertValueEqual(3, reloaded.getMaxStackSize(), "persisted custom maximum survives round-trip");
      helper.assertValueEqual(3, reloaded.getCount(), "persisted count matches actual cap");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void malformed_offered_stack_over_own_max_rejected(GameTestHelper helper) {
      ItemStack malformedOffer = new ItemStack(Items.APPLE, 4);
      malformedOffer.set(DataComponents.MAX_STACK_SIZE, 2);
      helper.assertTrue(MusliBarPressReserve.planInsert(State.EMPTY, malformedOffer).isEmpty(), "oversize offered item rejected");
      helper.assertValueEqual(4, malformedOffer.getCount(), "rejection does not shrink source stack");
      Plan ordinary = MusliBarPressReserve.planInsert(State.EMPTY, new ItemStack(Items.APPLE, 8)).orElseThrow();
      helper.assertValueEqual(8, ordinary.updated().apple().getCount(), "ordinary item capacity 8 unchanged");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void persisted_stack_over_own_max_fails_closed(GameTestHelper helper) {
      HolderLookup.Provider lookup = helper.getLevel().registryAccess();
      ItemStack invalid = new ItemStack(Items.BREAD, 5);
      invalid.set(DataComponents.MAX_STACK_SIZE, 3);
      CompoundTag payload = new CompoundTag();
      payload.putInt("version", 1);
      payload.put("bread", invalid.save(lookup));
      CompoundTag root = new CompoundTag();
      root.put("iedctr_musli_reserve_v1", payload);
      ItemStack press = new ItemStack(EngineerToolsModule.MUSLI_BAR_PRESS.get());
      press.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
      ItemStack original = press.copy();
      helper.assertTrue(MusliBarPressReserve.read(press, lookup).isEmpty(), "invalid persisted stack rejected");
      helper.assertTrue(ItemStack.matches(original, press), "read failure leaves press unchanged");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void vanilla_custom_data_snapshot_detached_for_supported_components(GameTestHelper helper) {
      CompoundTag initial = new CompoundTag();
      initial.putString("marker", "original");
      ItemStack source = new ItemStack(Items.BREAD, 2);
      source.set(DataComponents.CUSTOM_DATA, CustomData.of(initial));
      Plan planned = MusliBarPressReserve.planInsert(State.EMPTY, source).orElseThrow();
      CompoundTag changed = new CompoundTag();
      changed.putString("marker", "changed");
      source.set(DataComponents.CUSTOM_DATA, CustomData.of(changed));
      ItemStack exposed = planned.updated().bread();
      helper.assertValueEqual("original", exposed.get(DataComponents.CUSTOM_DATA).copyTag().getString("marker"), "snapshot isolated from source component replacement");
      exposed.set(DataComponents.CUSTOM_DATA, CustomData.of(changed));
      helper.assertValueEqual("original", planned.updated().bread().get(DataComponents.CUSTOM_DATA).copyTag().getString("marker"), "snapshot isolated from returned stack replacement");
      helper.succeed();
   }
}
