package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressItem;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.Compartment;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.Plan;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests real registry tag rebinding, not an end-to-end datapack file reload. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusliBarPressReserveTagGameTests {
   private MusliBarPressReserveTagGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void reserve_tag_revocation_preserves_stock_and_reallow_restores_admission(GameTestHelper helper) {
      Registry<Item> registry = BuiltInRegistries.ITEM;
      Map<TagKey<Item>, List<Holder<Item>>> original = snapshotTags(registry);
      TagKey<Item> accepted = MusliBarPressItem.ACCEPTED_SEEDS;
      helper.assertTrue(original.containsKey(accepted), "accepted seed tag must already exist before the fixture changes it");
      Holder<Item> extension = Items.TORCH.builtInRegistryHolder();
      Map<TagKey<Item>, List<Holder<Item>>> revoked = new HashMap<>(original);
      revoked.put(accepted, original.get(accepted).stream().filter(holder -> holder.value() != Items.TORCH).toList());
      Map<TagKey<Item>, List<Holder<Item>>> allowed = new HashMap<>(revoked);
      List<Holder<Item>> withExtension = new ArrayList<>(revoked.get(accepted));
      withExtension.add(extension);
      allowed.put(accepted, List.copyOf(withExtension));

      // All operations are synchronous on the GameTest server thread. Never yield,
      // schedule a tick or leave any registry mutation active outside this try/finally.
      try {
         registry.bindTags(revoked);
         ItemStack offered = new ItemStack(Items.TORCH, 4);
         offered.set(DataComponents.CUSTOM_NAME, Component.literal("Datapack extension batch"));
         helper.assertTrue(!MusliBarPressReserve.isSeedAdmissible(offered), "control: untagged extension is not admissible");
         helper.assertTrue(MusliBarPressReserve.planInsert(State.EMPTY, offered).isEmpty(), "control: no insertion before tag admission");

         registry.bindTags(allowed);
         helper.assertTrue(offered.is(accepted), "real ItemStack tag lookup sees added extension");
         State supplied = MusliBarPressReserve.planInsert(State.EMPTY, new ItemStack(Items.BREAD, 2)).orElseThrow().updated();
         supplied = MusliBarPressReserve.planInsert(supplied, new ItemStack(Items.APPLE, 2)).orElseThrow().updated();
         Plan insert = MusliBarPressReserve.planInsert(supplied, offered).orElseThrow();
         helper.assertValueEqual(4, insert.count(), "tagged extension admitted without changing its count");
         helper.assertTrue(MusliBarPressReserve.hasAdmissibleIngredients(insert.updated()), "all three ingredients eligible while tag admits extension");
         var lookup = helper.getLevel().registryAccess();
         ItemStack source = new ItemStack(EngineerToolsModule.MUSLI_BAR_PRESS.get());
         ItemStack stored = MusliBarPressReserve.stage(source, lookup, State.EMPTY, insert.updated()).orElseThrow();
         var savedBeforeRevocation = stored.save(lookup);

         registry.bindTags(revoked);
         helper.assertTrue(!offered.is(accepted), "real ItemStack tag lookup sees revocation");
         State readable = MusliBarPressReserve.read(stored, lookup).orElseThrow();
         helper.assertTrue(readable.sameContents(insert.updated()), "revoked stock remains readable with exact contents");
         helper.assertTrue(savedBeforeRevocation.equals(stored.save(lookup)), "tag revocation does not rewrite stored press data");
         helper.assertTrue(!MusliBarPressReserve.hasAdmissibleIngredients(readable), "revoked stock is not eligible for future pressing");
         helper.assertTrue(MusliBarPressReserve.planInsert(readable, offered.copyWithCount(1)).isEmpty(), "cannot top up revoked stock");
         helper.assertTrue(MusliBarPressReserve.planInsert(State.EMPTY, offered.copyWithCount(1)).isEmpty(), "cannot insert revoked extension into an empty reserve");

         Plan withdraw = MusliBarPressReserve.planWithdraw(readable, Compartment.SEEDS, 2).orElseThrow();
         helper.assertValueEqual(4, withdraw.count() + withdraw.updated().seeds().getCount(), "withdrawal under revocation conserves all four items");
         helper.assertTrue(ItemStack.isSameItemSameComponents(offered, withdraw.moved()), "withdrawal preserves revoked item's exact components");
         ItemStack remainderPress = MusliBarPressReserve.stage(stored, lookup, readable, withdraw.updated()).orElseThrow();
         ItemStack reloadedPress = ItemStack.parse(lookup, remainderPress.save(lookup)).orElseThrow();
         State remainder = MusliBarPressReserve.read(reloadedPress, lookup).orElseThrow();
         helper.assertTrue(remainder.sameContents(withdraw.updated()), "revoked remainder survives full ItemStack serialization");
         helper.assertTrue(!MusliBarPressReserve.hasAdmissibleIngredients(remainder), "serialization cannot restore revoked eligibility");

         registry.bindTags(allowed);
         helper.assertTrue(MusliBarPressReserve.hasAdmissibleIngredients(remainder), "reallow makes existing stock eligible without rewriting it");
         Plan topUp = MusliBarPressReserve.planInsert(remainder, offered.copyWithCount(1)).orElseThrow();
         helper.assertValueEqual(3, topUp.updated().seeds().getCount(), "same exact extension can be inserted after reallow");
         Plan finalWithdrawal = MusliBarPressReserve.planWithdraw(topUp.updated(), Compartment.SEEDS, 8).orElseThrow();
         helper.assertValueEqual(5, withdraw.count() + finalWithdrawal.count(), "four initial items plus one new insertion are conserved");
         helper.assertTrue(finalWithdrawal.updated().seeds().isEmpty(), "withdrawal to zero releases seed identity");
         helper.assertValueEqual(4, offered.getCount(), "pure plans never consume the offered source stack");
         helper.assertTrue(MusliBarPressReserve.read(source, lookup).orElseThrow().sameContents(State.EMPTY), "staging never changes the actual original press");
      } finally {
         registry.bindTags(original);
      }
      helper.assertTrue(snapshotTags(registry).equals(original), "all original registry tag memberships restored");
      helper.succeed();
   }

   private static Map<TagKey<Item>, List<Holder<Item>>> snapshotTags(Registry<Item> registry) {
      Map<TagKey<Item>, List<Holder<Item>>> result = new HashMap<>();
      registry.getTags().forEach(pair -> result.put(pair.getFirst(), pair.getSecond().stream().toList()));
      return result;
   }
}
