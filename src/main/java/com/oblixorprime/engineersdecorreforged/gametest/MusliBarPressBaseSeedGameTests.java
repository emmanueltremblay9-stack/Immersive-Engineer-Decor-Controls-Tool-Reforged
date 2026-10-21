package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressItem;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.Compartment;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.State;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Synchronous real-registry fixtures; not end-to-end datapack-file reload tests. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusliBarPressBaseSeedGameTests {
   private MusliBarPressBaseSeedGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void five_base_seeds_survive_empty_extension_tag(GameTestHelper helper) {
      Registry<Item> registry = BuiltInRegistries.ITEM;
      Map<TagKey<Item>, List<Holder<Item>>> original = snapshotTags(registry);
      helper.assertTrue(original.containsKey(MusliBarPressItem.ACCEPTED_SEEDS), "fixture needs the original tag");
      Item hemp = registry.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "seed"));
      helper.assertTrue(hemp != null && hemp != Items.AIR, "exact IE hemp seed is registered");
      List<Item> baseSeeds = List.of(Items.WHEAT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.BEETROOT_SEEDS, hemp);
      try {
         registry.bindTags(withSeeds(original, List.of()));
         for (Item item : baseSeeds) {
            ItemStack seed = new ItemStack(item, 1);
            helper.assertTrue(!seed.is(MusliBarPressItem.ACCEPTED_SEEDS), "control: base seed removed from live tag");
            helper.assertTrue(MusliBarPressReserve.isSeedAdmissible(seed), "approved base seed remains admissible");
            State supplied = supplied();
            var plan = MusliBarPressReserve.planInsert(supplied, seed).orElseThrow();
            helper.assertTrue(plan.updated().seeds().is(item), "exact base identity preserved");
            helper.assertTrue(MusliBarPressReserve.hasAdmissibleIngredients(plan.updated()), "base ingredients remain eligible");
            helper.assertValueEqual(1, seed.getCount(), "pure plan does not consume offered seed");
         }
      } finally {
         registry.bindTags(original);
      }
      helper.assertTrue(snapshotTags(registry).equals(original), "original tag memberships restored");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void replacement_tag_extends_base_and_revoked_stock_remains_withdrawable(GameTestHelper helper) {
      Registry<Item> registry = BuiltInRegistries.ITEM;
      Map<TagKey<Item>, List<Holder<Item>>> original = snapshotTags(registry);
      var lookup = helper.getLevel().registryAccess();
      try {
         registry.bindTags(withSeeds(original, List.of(Items.TORCH.builtInRegistryHolder())));
         ItemStack extension = new ItemStack(Items.TORCH, 4);
         helper.assertTrue(MusliBarPressReserve.isSeedAdmissible(new ItemStack(Items.WHEAT_SEEDS)), "replacement cannot revoke a base seed");
         State state = MusliBarPressReserve.planInsert(supplied(), extension).orElseThrow().updated();
         helper.assertTrue(MusliBarPressReserve.hasAdmissibleIngredients(state), "live extension is eligible");
         ItemStack originalPress = new ItemStack(EngineerToolsModule.MUSLI_BAR_PRESS.get());
         ItemStack stored = MusliBarPressReserve.stage(originalPress, lookup, State.EMPTY, state).orElseThrow();
         var saved = stored.save(lookup);

         registry.bindTags(withSeeds(original, List.of()));
         State revoked = MusliBarPressReserve.read(stored, lookup).orElseThrow();
         helper.assertTrue(revoked.sameContents(state), "tag removal cannot change stored stock");
         helper.assertTrue(saved.equals(stored.save(lookup)), "revocation preserves original serialized bytes");
         helper.assertTrue(!MusliBarPressReserve.hasAdmissibleIngredients(revoked), "revoked extension cannot authorize pressing");
         helper.assertTrue(MusliBarPressReserve.planInsert(revoked, extension).isEmpty(), "revoked extension cannot be topped up");
         var withdrawal = MusliBarPressReserve.planWithdraw(revoked, Compartment.SEEDS, 2).orElseThrow();
         helper.assertValueEqual(2, withdrawal.count(), "revoked stock remains withdrawable");
         helper.assertValueEqual(2, withdrawal.updated().seeds().getCount(), "withdrawal conserves remainder");
         ItemStack remainder = MusliBarPressReserve.stage(stored, lookup, revoked, withdrawal.updated()).orElseThrow();
         State reloaded = MusliBarPressReserve.read(ItemStack.parse(lookup, remainder.save(lookup)).orElseThrow(), lookup).orElseThrow();
         helper.assertTrue(!MusliBarPressReserve.hasAdmissibleIngredients(reloaded), "serialization cannot restore revoked eligibility");

         registry.bindTags(withSeeds(original, List.of(Items.TORCH.builtInRegistryHolder())));
         helper.assertTrue(MusliBarPressReserve.hasAdmissibleIngredients(reloaded), "readmission uses live tag without reinsertion");
         helper.assertValueEqual(4, extension.getCount(), "offered stack remains untouched throughout");
      } finally {
         registry.bindTags(original);
      }
      helper.assertTrue(snapshotTags(registry).equals(original), "all original tags restored");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void empty_tag_does_not_admit_unrelated_items(GameTestHelper helper) {
      Registry<Item> registry = BuiltInRegistries.ITEM;
      Map<TagKey<Item>, List<Holder<Item>>> original = snapshotTags(registry);
      try {
         registry.bindTags(withSeeds(original, List.of()));
         helper.assertTrue(!MusliBarPressReserve.isSeedAdmissible(null), "null is not a seed");
         helper.assertTrue(!MusliBarPressReserve.isSeedAdmissible(ItemStack.EMPTY), "empty is not a seed");
         for (Item item : List.of(Items.TORCH, Items.DIRT)) {
            ItemStack candidate = new ItemStack(item);
            helper.assertTrue(!MusliBarPressReserve.isSeedAdmissible(candidate), "unlisted item remains inadmissible");
            helper.assertTrue(MusliBarPressReserve.planInsert(State.EMPTY, candidate).isEmpty(), "no insertion through fallback");
         }
      } finally {
         registry.bindTags(original);
      }
      helper.assertTrue(snapshotTags(registry).equals(original), "original tags restored after negative cases");
      helper.succeed();
   }

   private static State supplied() {
      State bread = MusliBarPressReserve.planInsert(State.EMPTY, new ItemStack(Items.BREAD, 2)).orElseThrow().updated();
      return MusliBarPressReserve.planInsert(bread, new ItemStack(Items.APPLE, 2)).orElseThrow().updated();
   }

   private static Map<TagKey<Item>, List<Holder<Item>>> withSeeds(
      Map<TagKey<Item>, List<Holder<Item>>> original, List<Holder<Item>> seeds
   ) {
      Map<TagKey<Item>, List<Holder<Item>>> result = new HashMap<>(original);
      result.put(MusliBarPressItem.ACCEPTED_SEEDS, List.copyOf(seeds));
      return result;
   }

   private static Map<TagKey<Item>, List<Holder<Item>>> snapshotTags(Registry<Item> registry) {
      Map<TagKey<Item>, List<Holder<Item>>> result = new HashMap<>();
      registry.getTags().forEach(pair -> result.put(pair.getFirst(), pair.getSecond().stream().toList()));
      return result;
   }
}
