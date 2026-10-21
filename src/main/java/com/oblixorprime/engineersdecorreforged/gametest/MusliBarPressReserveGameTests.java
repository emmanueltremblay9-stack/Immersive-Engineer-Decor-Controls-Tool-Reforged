package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.Compartment;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.Plan;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.State;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusliBarPressReserveGameTests {
   private MusliBarPressReserveGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void reserve_default_empty_and_capacity_eight(GameTestHelper helper) {
      ItemStack press = press();
      State initial = MusliBarPressReserve.read(press, registries(helper)).orElseThrow();
      helper.assertTrue(initial.sameContents(State.EMPTY), "legacy press starts with empty internal reserve");
      Plan bread = MusliBarPressReserve.planInsert(initial, new ItemStack(Items.BREAD, 11)).orElseThrow();
      helper.assertValueEqual(8, bread.count(), "bread reserve accepts exactly its eight free spaces");
      helper.assertValueEqual(8, bread.updated().bread().getCount(), "bread compartment reaches 8");
      helper.assertValueEqual(0, initial.bread().getCount(), "simulation never changes the original snapshot");
      State six = MusliBarPressReserve.planInsert(initial, new ItemStack(Items.BREAD, 6)).orElseThrow().updated();
      ItemStack offeredFive = new ItemStack(Items.BREAD, 5);
      Plan topUp = MusliBarPressReserve.planInsert(six, offeredFive).orElseThrow();
      helper.assertValueEqual(2, topUp.count(), "insertion accepts only remaining capacity");
      helper.assertValueEqual(5, offeredFive.getCount(), "offered stack is not consumed by simulation");
      helper.assertValueEqual(6, six.bread().getCount(), "previous contents remain unchanged");
      helper.assertTrue(MusliBarPressReserve.planInsert(bread.updated(), new ItemStack(Items.BREAD)).isEmpty(), "ninth bread refused");
      Plan apple = MusliBarPressReserve.planInsert(bread.updated(), new ItemStack(Items.APPLE, 9)).orElseThrow();
      helper.assertValueEqual(8, apple.updated().apple().getCount(), "apple compartment has independent capacity");
      helper.assertValueEqual(8, apple.updated().bread().getCount(), "apple insertion preserves bread reserve");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void reserve_single_seed_variety_and_release_at_zero(GameTestHelper helper) {
      Plan wheat = MusliBarPressReserve.planInsert(State.EMPTY, new ItemStack(Items.WHEAT_SEEDS, 8)).orElseThrow();
      helper.assertTrue(
         MusliBarPressReserve.planInsert(wheat.updated(), new ItemStack(Items.MELON_SEEDS, 1)).isEmpty(),
         "different seed variant cannot mix with an occupied seed reserve"
      );
      Plan withdraw = MusliBarPressReserve.planWithdraw(wheat.updated(), Compartment.SEEDS, 8).orElseThrow();
      helper.assertValueEqual(8, withdraw.count(), "all eight wheat seeds are offered to destination simulation");
      helper.assertTrue(withdraw.updated().seeds().isEmpty(), "at zero, seed variety is released");
      Plan melon = MusliBarPressReserve.planInsert(withdraw.updated(), new ItemStack(Items.MELON_SEEDS, 3)).orElseThrow();
      helper.assertTrue(melon.updated().seeds().is(Items.MELON_SEEDS), "new seed variety accepted after emptying");
      helper.assertValueEqual(3, melon.updated().seeds().getCount(), "seed quantity preserved");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void reserve_staged_roundtrip_preserves_unrelated_data_and_tool(GameTestHelper helper) {
      ItemStack original = press();
      original.setDamageValue(3);
      CompoundTag originalExtras = new CompoundTag();
      originalExtras.putString("another_mod_key", "keep-this-unchanged");
      original.set(DataComponents.CUSTOM_DATA, CustomData.of(originalExtras));
      ItemStack enrichedBread = new ItemStack(Items.BREAD, 6);
      enrichedBread.set(DataComponents.CUSTOM_NAME, Component.literal("Tagged Bread"));
      Plan bread = MusliBarPressReserve.planInsert(State.EMPTY, enrichedBread).orElseThrow();
      Plan apple = MusliBarPressReserve.planInsert(bread.updated(), new ItemStack(Items.APPLE, 5)).orElseThrow();
      Plan seeds = MusliBarPressReserve.planInsert(apple.updated(), new ItemStack(Items.WHEAT_SEEDS, 4)).orElseThrow();
      HolderLookup.Provider lookup = registries(helper);
      ItemStack staged = MusliBarPressReserve.stage(original, lookup, State.EMPTY, seeds.updated()).orElseThrow();
      helper.assertValueEqual(3, original.getDamageValue(), "source damage unchanged");
      helper.assertTrue(MusliBarPressReserve.read(original, lookup).orElseThrow().sameContents(State.EMPTY), "source reserve not mutated");
      helper.assertValueEqual(3, staged.getDamageValue(), "staging preserves damage");
      helper.assertTrue(staged.is(original.getItem()), "staging keeps original press item type");
      helper.assertValueEqual("keep-this-unchanged", customData(staged).getString("another_mod_key"), "unrelated NBT retained");
      ItemStack roundTrip = ItemStack.parse(lookup, staged.save(lookup)).orElseThrow();
      State restored = MusliBarPressReserve.read(roundTrip, lookup).orElseThrow();
      helper.assertTrue(restored.sameContents(seeds.updated()), "full saved ingredient contents round trip");
      helper.assertValueEqual(Component.literal("Tagged Bread"), restored.bread().get(DataComponents.CUSTOM_NAME), "ingredient component survives serialization");
      helper.assertValueEqual(3, roundTrip.getDamageValue(), "damage survives save and load");
      helper.assertValueEqual("keep-this-unchanged", customData(roundTrip).getString("another_mod_key"), "unrelated saved NBT survives");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void reserve_fail_closed_corrupt_payload_and_stale_expectation(GameTestHelper helper) {
      HolderLookup.Provider lookup = registries(helper);
      ItemStack malformed = press();
      CompoundTag wrongType = new CompoundTag();
      wrongType.putString("iedctr_musli_reserve_v1", "corrupted-payload");
      malformed.set(DataComponents.CUSTOM_DATA, CustomData.of(wrongType));
      ItemStack unchanged = MusliBarPressReserve.stage(press(), lookup, State.EMPTY, State.EMPTY).orElseThrow();
      helper.assertTrue(customData(unchanged).isEmpty(), "logical no-op leaves empty state unmodified");
      helper.assertTrue(MusliBarPressReserve.read(malformed, lookup).isEmpty(), "corrupt payload rejected");
      State desired = MusliBarPressReserve.planInsert(State.EMPTY, new ItemStack(Items.BREAD, 2)).orElseThrow().updated();
      helper.assertTrue(MusliBarPressReserve.stage(malformed, lookup, State.EMPTY, desired).isEmpty(), "cannot overwrite corrupt reserve");
      helper.assertValueEqual("corrupted-payload", customData(malformed).getString("iedctr_musli_reserve_v1"), "corrupt payload preserved for recovery");
      ItemStack valid = MusliBarPressReserve.stage(press(), lookup, State.EMPTY, desired).orElseThrow();
      helper.assertTrue(MusliBarPressReserve.stage(valid, lookup, State.EMPTY, desired).isEmpty(), "stale reserve snapshot cannot be staged again");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void reserve_rejects_corrupt_counts_and_unexpected_fields(GameTestHelper helper) {
      HolderLookup.Provider lookup = registries(helper);
      ItemStack corrupted = press();
      CompoundTag payload = new CompoundTag();
      payload.putInt("version", 1);
      payload.put("bread", new ItemStack(Items.BREAD, 9).save(lookup));
      CompoundTag root = new CompoundTag();
      root.put("iedctr_musli_reserve_v1", payload);
      corrupted.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
      helper.assertTrue(MusliBarPressReserve.read(corrupted, lookup).isEmpty(), "saved count exceeding capacity rejected");
      helper.assertValueEqual(9, customData(corrupted).getCompound("iedctr_musli_reserve_v1").getCompound("bread").getInt("count"), "corrupt saved state not rewritten");

      ItemStack unexpected = press();
      CompoundTag extraPayload = new CompoundTag();
      extraPayload.putInt("version", 1);
      extraPayload.putString("unknown_field", "must_not_ignore");
      CompoundTag extras = new CompoundTag();
      extras.put("iedctr_musli_reserve_v1", extraPayload);
      unexpected.set(DataComponents.CUSTOM_DATA, CustomData.of(extras));
      helper.assertTrue(MusliBarPressReserve.read(unexpected, lookup).isEmpty(), "unknown reserve field rejected without normalization");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void reserve_snapshots_are_independent_and_component_exact(GameTestHelper helper) {
      ItemStack firstPress = press();
      ItemStack secondPress = press();
      ItemStack offered = new ItemStack(Items.BREAD, 4);
      Plan plan = MusliBarPressReserve.planInsert(State.EMPTY, offered).orElseThrow();
      offered.shrink(4);
      ItemStack leakedCopy = plan.updated().bread();
      leakedCopy.shrink(2);
      helper.assertValueEqual(4, plan.updated().bread().getCount(), "snapshot cannot be mutated through external reference");
      ItemStack staged = MusliBarPressReserve.stage(firstPress, registries(helper), State.EMPTY, plan.updated()).orElseThrow();
      helper.assertValueEqual(4, MusliBarPressReserve.read(staged, registries(helper)).orElseThrow().bread().getCount(), "first staged reserve changed");
      helper.assertTrue(MusliBarPressReserve.read(secondPress, registries(helper)).orElseThrow().sameContents(State.EMPTY), "second press unaffected");
      helper.assertTrue(MusliBarPressReserve.read(firstPress, registries(helper)).orElseThrow().sameContents(State.EMPTY), "first actual press unaffected by staging");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void reserve_partial_transfers_conserve_exact_components(GameTestHelper helper) {
      ItemStack offered = new ItemStack(Items.WHEAT_SEEDS, 11);
      offered.set(DataComponents.CUSTOM_NAME, Component.literal("Seed batch"));
      Plan insertion = MusliBarPressReserve.planInsert(State.EMPTY, offered).orElseThrow();
      helper.assertValueEqual(8, insertion.count(), "seed capacity is eight");
      helper.assertValueEqual(11, offered.getCount(), "planning does not consume offered items");
      helper.assertTrue(MusliBarPressReserve.planInsert(insertion.updated(), offered).isEmpty(), "full seeds refuse overflow");
      helper.assertTrue(MusliBarPressReserve.planWithdraw(insertion.updated(), Compartment.SEEDS, 0).isEmpty(), "no destination space moves nothing");
      helper.assertTrue(MusliBarPressReserve.planWithdraw(insertion.updated(), Compartment.SEEDS, -1).isEmpty(), "negative space refused");
      Plan partial = MusliBarPressReserve.planWithdraw(insertion.updated(), Compartment.SEEDS, 3).orElseThrow();
      helper.assertValueEqual(3, partial.count(), "withdrawal respects destination limit");
      helper.assertValueEqual(8, partial.count() + partial.updated().seeds().getCount(), "partial withdrawal conserves stock");
      helper.assertTrue(ItemStack.isSameItemSameComponents(offered, partial.moved()), "withdrawal preserves exact variant");
      ItemStack movedCopy = partial.moved();
      movedCopy.setCount(64);
      movedCopy.set(DataComponents.CUSTOM_NAME, Component.literal("Changed externally"));
      helper.assertValueEqual(3, partial.count(), "moved stack exposure is detached");
      helper.assertTrue(ItemStack.isSameItemSameComponents(offered, partial.moved()), "moved components remain detached");
      helper.assertTrue(MusliBarPressReserve.planInsert(partial.updated(), new ItemStack(Items.WHEAT_SEEDS)).isEmpty(), "different components cannot merge");
      Plan end = MusliBarPressReserve.planWithdraw(partial.updated(), Compartment.SEEDS, Integer.MAX_VALUE).orElseThrow();
      helper.assertValueEqual(8, partial.count() + end.count(), "complete withdrawal neither duplicates nor loses items");
      helper.assertTrue(end.updated().sameContents(State.EMPTY), "empty withdrawal releases variant");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void reserve_rejects_wrong_tools_versions_and_slot_types(GameTestHelper helper) {
      HolderLookup.Provider lookup = registries(helper);
      helper.assertTrue(MusliBarPressReserve.read(new ItemStack(Items.STICK), lookup).isEmpty(), "non-press refused");
      helper.assertTrue(MusliBarPressReserve.read(press().copyWithCount(2), lookup).isEmpty(), "stacked press is ambiguous");
      helper.assertTrue(MusliBarPressReserve.planInsert(State.EMPTY, new ItemStack(Items.STICK)).isEmpty(), "unaccepted ingredient refused");
      for (int version : new int[]{0, 2}) {
         CompoundTag payload = new CompoundTag();
         payload.putInt("version", version);
         ItemStack invalid = withPayload(payload);
         helper.assertTrue(MusliBarPressReserve.read(invalid, lookup).isEmpty(), "unsupported version refused");
      }
      CompoundTag payload = new CompoundTag();
      payload.putInt("version", 1);
      ItemStack explicitlyEmpty = withPayload(payload);
      ItemStack noOp = MusliBarPressReserve.stage(explicitlyEmpty, lookup, State.EMPTY, State.EMPTY).orElseThrow();
      helper.assertTrue(customData(noOp).equals(customData(explicitlyEmpty)), "no-op preserves explicit empty encoding");
      payload.put("bread", new ItemStack(Items.APPLE).save(lookup));
      helper.assertTrue(MusliBarPressReserve.read(withPayload(payload), lookup).isEmpty(), "wrong compartment item refused");
      CompoundTag nested = (CompoundTag) new ItemStack(Items.BREAD).save(lookup);
      nested.putString("unknown_nested_field", "must not be discarded");
      payload.put("bread", nested);
      ItemStack lossy = withPayload(payload);
      CompoundTag before = customData(lossy);
      helper.assertTrue(MusliBarPressReserve.read(lossy, lookup).isEmpty(), "lossy nested codec representation refused");
      helper.assertTrue(MusliBarPressReserve.stage(lossy, lookup, State.EMPTY, State.EMPTY).isEmpty(), "lossy payload cannot be normalized");
      helper.assertTrue(before.equals(customData(lossy)), "rejected nested payload preserved exactly");
      payload.putString("bread", "invalid");
      helper.assertTrue(MusliBarPressReserve.read(withPayload(payload), lookup).isEmpty(), "wrong slot encoding refused");
      helper.succeed();
   }

   private static ItemStack withPayload(CompoundTag payload) {
      ItemStack result = press();
      CompoundTag root = new CompoundTag();
      root.put("iedctr_musli_reserve_v1", payload.copy());
      result.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
      return result;
   }

   private static ItemStack press() {
      return new ItemStack(EngineerToolsModule.MUSLI_BAR_PRESS.get());
   }

   private static HolderLookup.Provider registries(GameTestHelper helper) {
      return helper.getLevel().registryAccess();
   }

   private static CompoundTag customData(ItemStack stack) {
      return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
   }
}
