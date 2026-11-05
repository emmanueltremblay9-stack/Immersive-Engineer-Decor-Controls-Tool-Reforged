package com.oblixorprime.engineersdecorreforged.tools;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * Pure staging for three irreversible ingredient reserves. Accepted items become
 * units, never stored ItemStacks. This helper does not commit inventory changes,
 * activate a GUI or bypass the transaction/capture admission gates.
 */
public final class MusliBarPressReserve {
   public static final int CAPACITY_PER_COMPARTMENT = 8;
   private static final String LEGACY_KEY = "iedctr_musli_reserve_v1";
   private static final String KEY = "iedctr_musli_reserve_v2";
   private static final Set<String> KEYS = Set.of("version", "cereal", "fruit", "seeds");
   private static final Set<String> LEGACY_KEYS = Set.of("version", "bread", "apple", "seeds");
   private static final ResourceLocation BASE_HEMP_SEED = ResourceLocation.fromNamespaceAndPath("immersiveengineering", "seed");

   public enum Compartment { CEREAL, FRUIT, SEEDS }

   /** Counts only: neither ingredient identity nor components survive conversion. */
   public static final class State {
      public static final State EMPTY = new State(0, 0, 0);
      private final int cereal;
      private final int fruit;
      private final int seeds;

      private State(int cereal, int fruit, int seeds) {
         this.cereal = cereal;
         this.fruit = fruit;
         this.seeds = seeds;
      }

      public int cereal() { return cereal; }
      public int fruit() { return fruit; }
      public int seeds() { return seeds; }
      public int contents(Compartment compartment) {
         return switch (compartment) {
            case CEREAL -> cereal;
            case FRUIT -> fruit;
            case SEEDS -> seeds;
         };
      }
      public boolean sameContents(State other) {
         return other != null && cereal == other.cereal && fruit == other.fruit && seeds == other.seeds;
      }
      private boolean isValid() { return validCount(cereal) && validCount(fruit) && validCount(seeds); }
      private State with(Compartment compartment, int count) {
         return switch (compartment) {
            case CEREAL -> new State(count, fruit, seeds);
            case FRUIT -> new State(cereal, count, seeds);
            case SEEDS -> new State(cereal, fruit, count);
         };
      }
   }

   /** The caller must validate the source and atomically consume count items. */
   public static final class Plan {
      private final State updated;
      private final Compartment compartment;
      private final int count;
      private Plan(State updated, Compartment compartment, int count) {
         this.updated = updated;
         this.compartment = compartment;
         this.count = count;
      }
      public State updated() { return updated; }
      public Compartment compartment() { return compartment; }
      public int count() { return count; }
   }

   private MusliBarPressReserve() {}

   /** Reads without mutation; strict legacy decoding prevents silent data loss. */
   public static Optional<State> read(ItemStack press, HolderLookup.Provider registries) {
      if (!isPress(press) || registries == null) return Optional.empty();
      CompoundTag root = customData(press);
      if (root.contains(KEY) && root.contains(LEGACY_KEY)) return Optional.empty();
      if (!root.contains(KEY) && !root.contains(LEGACY_KEY)) return Optional.of(State.EMPTY);
      try {
         if (root.contains(LEGACY_KEY)) return readLegacy(root, registries);
         if (!root.contains(KEY, Tag.TAG_COMPOUND)) return Optional.empty();
         CompoundTag payload = root.getCompound(KEY);
         if (!payload.getAllKeys().equals(KEYS)) return Optional.empty();
         for (String field : KEYS) {
            if (!payload.contains(field, Tag.TAG_INT)) return Optional.empty();
         }
         if (payload.getInt("version") != 2) return Optional.empty();
         State state = new State(payload.getInt("cereal"), payload.getInt("fruit"), payload.getInt("seeds"));
         return state.isValid() ? Optional.of(state) : Optional.empty();
      } catch (RuntimeException invalid) {
         return Optional.empty();
      }
   }

   /**
    * A previously accepted legacy extension remains convertible after tag removal.
    * Identity/components are decoded exactly before deriving counts, and source
    * data is never rewritten by reading. Ambiguous legacy data stays recoverable.
    */
   private static Optional<State> readLegacy(CompoundTag root, HolderLookup.Provider registries) {
      if (!root.contains(LEGACY_KEY, Tag.TAG_COMPOUND)) return Optional.empty();
      CompoundTag payload = root.getCompound(LEGACY_KEY);
      if (!payload.contains("version", Tag.TAG_INT) || payload.getInt("version") != 1
         || !LEGACY_KEYS.containsAll(payload.getAllKeys())) return Optional.empty();
      Optional<ItemStack> bread = parseLegacySlot(payload, "bread", registries);
      Optional<ItemStack> apple = parseLegacySlot(payload, "apple", registries);
      Optional<ItemStack> seeds = parseLegacySlot(payload, "seeds", registries);
      if (bread.isEmpty() || apple.isEmpty() || seeds.isEmpty()) return Optional.empty();
      if ((!bread.get().isEmpty() && !bread.get().is(Items.BREAD))
         || (!apple.get().isEmpty() && !apple.get().is(Items.APPLE))) return Optional.empty();
      return Optional.of(new State(bread.get().getCount(), apple.get().getCount(), seeds.get().getCount()));
   }

   private static Optional<ItemStack> parseLegacySlot(CompoundTag payload, String key, HolderLookup.Provider registries) {
      if (!payload.contains(key)) return Optional.of(ItemStack.EMPTY);
      if (!payload.contains(key, Tag.TAG_COMPOUND)) return Optional.empty();
      CompoundTag saved = payload.getCompound(key);
      Optional<ItemStack> parsed = ItemStack.parse(registries, saved);
      if (parsed.isEmpty() || parsed.get().isEmpty()) return Optional.empty();
      ItemStack stack = parsed.get();
      if (!validCount(stack.getCount()) || stack.getCount() > stack.getMaxStackSize()
         || !saved.equals(stack.save(registries))) return Optional.empty();
      return parsed;
   }

   /** Live admission is checked only for new inputs; all admitted variants pool. */
   public static Optional<Plan> planInsert(State current, ItemStack offered) {
      if (current == null || !current.isValid() || offered == null || offered.isEmpty()
         || offered.getMaxStackSize() < 1 || offered.getCount() > offered.getMaxStackSize()) return Optional.empty();
      Compartment kind = classify(offered);
      if (kind == null) return Optional.empty();
      int amount = Math.min(offered.getCount(), CAPACITY_PER_COMPARTMENT - current.contents(kind));
      if (amount <= 0) return Optional.empty();
      return Optional.of(new Plan(current.with(kind, current.contents(kind) + amount), kind, amount));
   }

   /** Pure one-batch consumption; output capacity and durability are caller prerequisites. */
   public static Optional<State> consumeBatch(State current) {
      if (!hasAdmissibleIngredients(current)) return Optional.empty();
      return Optional.of(new State(current.cereal - 1, current.fruit - 1, current.seeds - 1));
   }

   /** Conversion is irreversible: there is no ingredient extraction plan. */
   public static Optional<Plan> planWithdraw(State current, Compartment compartment, int possibleToReceive) {
      return Optional.empty();
   }

   /**
    * Detached staging only, not a transaction, source revalidation or anti-ABA fence.
    * A real change converts validated legacy data once; a no-op preserves its bytes.
    */
   public static Optional<ItemStack> stage(ItemStack press, HolderLookup.Provider registries, State expected, State desired) {
      if (expected == null || desired == null || !expected.isValid() || !desired.isValid()) return Optional.empty();
      Optional<State> actual = read(press, registries);
      if (actual.isEmpty() || !actual.get().sameContents(expected)) return Optional.empty();
      if (actual.get().sameContents(desired)) return Optional.of(press.copy());
      try {
         ItemStack staged = press.copy();
         CompoundTag root = customData(staged);
         root.remove(LEGACY_KEY);
         CompoundTag payload = new CompoundTag();
         payload.putInt("version", 2);
         payload.putInt("cereal", desired.cereal);
         payload.putInt("fruit", desired.fruit);
         payload.putInt("seeds", desired.seeds);
         root.put(KEY, payload);
         staged.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
         Optional<State> verified = read(staged, registries);
         return verified.isPresent() && verified.get().sameContents(desired) ? Optional.of(staged) : Optional.empty();
      } catch (RuntimeException invalid) {
         return Optional.empty();
      }
   }

   private static boolean validCount(int count) { return count >= 0 && count <= CAPACITY_PER_COMPARTMENT; }
   private static boolean isPress(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getCount() == 1 && stack.getItem() instanceof MusliBarPressItem;
   }

   /** The original five seeds plus live extensions; no expansion of ingredient sets. */
   public static boolean isSeedAdmissible(ItemStack ingredient) {
      if (ingredient == null || ingredient.isEmpty()) {
         return false;
      }
      // TOOLS-192/193 allow extensions, not revocation of the original five seeds.
      // Exact registry identity avoids admitting an unrelated hemp-looking item.
      // Never use admission eligibility to validate already-persisted seed stock.
      return ingredient.is(Items.WHEAT_SEEDS)
         || ingredient.is(Items.MELON_SEEDS)
         || ingredient.is(Items.PUMPKIN_SEEDS)
         || ingredient.is(Items.BEETROOT_SEEDS)
         || BASE_HEMP_SEED.equals(BuiltInRegistries.ITEM.getKey(ingredient.getItem()))
         || ingredient.is(MusliBarPressItem.ACCEPTED_SEEDS);
   }

   /** Converted stock is generic and independent of subsequent tag changes. */
   public static boolean hasAdmissibleIngredients(State current) {
      return current != null && current.isValid() && current.cereal > 0 && current.fruit > 0 && current.seeds > 0;
   }

   private static Compartment classify(ItemStack ingredient) {
      if (ingredient.is(Items.BREAD)) return Compartment.CEREAL;
      if (ingredient.is(Items.APPLE)) return Compartment.FRUIT;
      if (isSeedAdmissible(ingredient)) return Compartment.SEEDS;
      return null;
   }

   private static CompoundTag customData(ItemStack stack) {
      return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
   }
}
