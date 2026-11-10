package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.Compartment;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.State;
import java.util.Set;
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
public final class MusliBarPressReserveMigrationGameTests {
   private static final String V1 = "iedctr_musli_reserve_v1";
   private static final String V2 = "iedctr_musli_reserve_v2";

   private MusliBarPressReserveMigrationGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void legacy_read_converts_counts_but_only_real_change_migrates(GameTestHelper helper) {
      var lookup = helper.getLevel().registryAccess();
      ItemStack bread = new ItemStack(Items.BREAD, 3);
      bread.set(DataComponents.CUSTOM_NAME, Component.literal("Legacy named bread"));
      CompoundTag legacy = new CompoundTag();
      legacy.putInt("version", 1);
      legacy.put("bread", bread.save(lookup));
      legacy.put("apple", new ItemStack(Items.APPLE, 2).save(lookup));
      // Strictly valid legacy seed slots remain readable even if their old admission was revoked.
      legacy.put("seeds", new ItemStack(Items.TORCH, 4).save(lookup));
      ItemStack source = withPayload(V1, legacy);
      source.setDamageValue(3);
      CompoundTag root = customData(source);
      root.putString("other_mod", "untouched");
      source.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
      ItemStack original = source.copy();
      State converted = MusliBarPressReserve.read(source, lookup).orElseThrow();
      helper.assertValueEqual(3, converted.cereal(), "legacy bread converts to cereal count");
      helper.assertValueEqual(2, converted.fruit(), "legacy apple converts to fruit count");
      helper.assertValueEqual(4, converted.seeds(), "legacy revoked seed identity converts to seed count");
      helper.assertTrue(MusliBarPressReserve.hasAdmissibleIngredients(converted), "converted ingredients no longer depend on old identity admission");
      helper.assertTrue(ItemStack.matches(source, original), "read does not rewrite legacy source");
      ItemStack noOp = MusliBarPressReserve.stage(source, lookup, converted, converted).orElseThrow();
      helper.assertTrue(ItemStack.matches(original, noOp), "logical no-op preserves complete legacy representation");
      helper.assertTrue(customData(noOp).contains(V1) && !customData(noOp).contains(V2), "no-op never migrates bytes");
      State desired = MusliBarPressReserve.planInsert(converted, new ItemStack(Items.MELON_SEEDS)).orElseThrow().updated();
      ItemStack migrated = MusliBarPressReserve.stage(source, lookup, converted, desired).orElseThrow();
      helper.assertTrue(!customData(migrated).contains(V1), "real change removes old identity payload");
      CompoundTag payload = customData(migrated).getCompound(V2);
      helper.assertTrue(payload.getAllKeys().equals(Set.of("version", "cereal", "fruit", "seeds")), "migration serializes only version and counters");
      helper.assertValueEqual(5, payload.getInt("seeds"), "legacy and new seed varieties pool");
      helper.assertValueEqual("untouched", customData(migrated).getString("other_mod"), "migration preserves unrelated custom data");
      helper.assertValueEqual(3, migrated.getDamageValue(), "migration preserves press wear");
      helper.assertTrue(ItemStack.matches(source, original), "staging does not mutate legacy source");
      helper.assertTrue(MusliBarPressReserve.read(migrated, lookup).orElseThrow().sameContents(desired), "migrated counters round trip");
      helper.assertTrue(MusliBarPressReserve.stage(migrated, lookup, converted, desired).isEmpty(), "stale pre-migration snapshot fails closed");
      helper.assertTrue(MusliBarPressReserve.planWithdraw(converted, Compartment.SEEDS, 8).isEmpty(), "legacy conversion does not expose original items");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void v2_requires_exact_keys_integer_types_and_bounded_counts(GameTestHelper helper) {
      for (String field : new String[]{"version", "cereal", "fruit", "seeds"}) {
         CompoundTag missing = validPayload();
         missing.remove(field);
         rejected(helper, withPayload(V2, missing), "missing " + field);
         CompoundTag wrongType = validPayload();
         wrongType.putString(field, "2");
         rejected(helper, withPayload(V2, wrongType), "string " + field);
         CompoundTag byteType = validPayload();
         byteType.putByte(field, (byte) 2);
         rejected(helper, withPayload(V2, byteType), "non-int numeric " + field);
      }
      for (String field : new String[]{"cereal", "fruit", "seeds"}) {
         for (int value : new int[]{-1, 9, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            CompoundTag outOfRange = validPayload();
            outOfRange.putInt(field, value);
            rejected(helper, withPayload(V2, outOfRange), "out of bounds " + field);
         }
      }
      for (int version : new int[]{0, 1, 3}) {
         CompoundTag unsupported = validPayload();
         unsupported.putInt("version", version);
         rejected(helper, withPayload(V2, unsupported), "unsupported v2 version");
      }
      CompoundTag extra = validPayload();
      extra.putString("identity", "minecraft:wheat_seeds");
      rejected(helper, withPayload(V2, extra), "unknown v2 field");
      CompoundTag zero = validPayload();
      zero.putInt("cereal", 0);
      zero.putInt("fruit", 0);
      zero.putInt("seeds", 0);
      var lookup = helper.getLevel().registryAccess();
      ItemStack empty = withPayload(V2, zero);
      helper.assertTrue(MusliBarPressReserve.read(empty, lookup).orElseThrow().sameContents(State.EMPTY), "all-zero counters are valid");
      helper.assertTrue(ItemStack.matches(empty, MusliBarPressReserve.stage(empty, lookup, State.EMPTY, State.EMPTY).orElseThrow()), "explicit zero payload survives no-op");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void conflicting_versions_and_wrong_payload_types_fail_closed(GameTestHelper helper) {
      CompoundTag legacy = new CompoundTag();
      legacy.putInt("version", 1);
      ItemStack both = withPayload(V2, validPayload());
      CompoundTag root = customData(both);
      root.put(V1, legacy);
      both.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
      rejected(helper, both, "simultaneous v1 and v2 payloads are ambiguous");
      for (String key : new String[]{V1, V2}) {
         ItemStack wrongType = new ItemStack(EngineerToolsModule.MUSLI_BAR_PRESS.get());
         CompoundTag malformed = new CompoundTag();
         malformed.putString(key, "not-a-compound");
         wrongType.set(DataComponents.CUSTOM_DATA, CustomData.of(malformed));
         rejected(helper, wrongType, "wrong payload type");
      }
      helper.succeed();
   }

   private static void rejected(GameTestHelper helper, ItemStack press, String reason) {
      var lookup = helper.getLevel().registryAccess();
      ItemStack before = press.copy();
      helper.assertTrue(MusliBarPressReserve.read(press, lookup).isEmpty(), reason + " rejected on read");
      helper.assertTrue(MusliBarPressReserve.stage(press, lookup, State.EMPTY, State.EMPTY).isEmpty(), reason + " cannot normalize through stage");
      helper.assertTrue(ItemStack.matches(before, press), reason + " preserves exact source data");
   }

   private static CompoundTag validPayload() {
      CompoundTag payload = new CompoundTag();
      payload.putInt("version", 2);
      payload.putInt("cereal", 8);
      payload.putInt("fruit", 8);
      payload.putInt("seeds", 8);
      return payload;
   }

   private static ItemStack withPayload(String key, CompoundTag payload) {
      ItemStack press = new ItemStack(EngineerToolsModule.MUSLI_BAR_PRESS.get());
      CompoundTag root = new CompoundTag();
      root.put(key, payload.copy());
      press.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
      return press;
   }

   private static CompoundTag customData(ItemStack stack) {
      return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
   }
}
