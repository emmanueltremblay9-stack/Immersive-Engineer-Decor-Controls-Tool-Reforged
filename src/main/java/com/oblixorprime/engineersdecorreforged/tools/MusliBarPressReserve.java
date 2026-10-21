package com.oblixorprime.engineersdecorreforged.tools;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * Immutable ingredient-reserve snapshots and pure staging for a Muslee Bar Press.
 *
 * This layer does NOT transfer player inventory, commit a transaction, open a GUI,
 * enqueue work or implement pressing. A caller may commit a staged stack only in
 * its separately validated, atomic, server-authoritative transaction.
 */
public final class MusliBarPressReserve {
   public static final int CAPACITY_PER_COMPARTMENT = 8;
   private static final String KEY = "iedctr_musli_reserve_v1";
   private static final String VERSION = "version";
   private static final String BREAD = "bread";
   private static final String APPLE = "apple";
   private static final String SEEDS = "seeds";
   private static final Set<String> ALLOWED_KEYS = Set.of(VERSION, BREAD, APPLE, SEEDS);

   public enum Compartment { BREAD, APPLE, SEEDS }

   /** ItemStacks are always defensively copied on input and output. */
   public static final class State {
      public static final State EMPTY = new State(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
      private final ItemStack bread;
      private final ItemStack apple;
      private final ItemStack seeds;

      private State(ItemStack bread, ItemStack apple, ItemStack seeds) {
         this.bread = bread.copy();
         this.apple = apple.copy();
         this.seeds = seeds.copy();
      }

      public ItemStack bread() { return bread.copy(); }
      public ItemStack apple() { return apple.copy(); }
      public ItemStack seeds() { return seeds.copy(); }

      public ItemStack contents(Compartment compartment) {
         return switch (compartment) {
            case BREAD -> bread();
            case APPLE -> apple();
            case SEEDS -> seeds();
         };
      }

      public boolean sameContents(State other) {
         return other != null && ItemStack.matches(bread, other.bread)
            && ItemStack.matches(apple, other.apple)
            && ItemStack.matches(seeds, other.seeds);
      }

      private boolean isValid() {
         return validStack(bread, Compartment.BREAD)
            && validStack(apple, Compartment.APPLE)
            && validStack(seeds, Compartment.SEEDS);
      }

      private State with(Compartment compartment, ItemStack next) {
         return switch (compartment) {
            case BREAD -> new State(next, apple, seeds);
            case APPLE -> new State(bread, next, seeds);
            case SEEDS -> new State(bread, apple, next);
         };
      }
   }

   /** Pure operation plan. The moved stack and resulting snapshot are detached. */
   public static final class Plan {
      private final State updated;
      private final ItemStack moved;

      private Plan(State updated, ItemStack moved) {
         this.updated = updated;
         this.moved = moved.copy();
      }

      public State updated() { return updated; }
      public ItemStack moved() { return moved.copy(); }
      public int count() { return moved.getCount(); }
   }

   private MusliBarPressReserve() {}

   /**
    * Read a complete reserve snapshot; malformed data return Optional.empty().
    * A press without our key has the unmodified, empty legacy reserve state.
    */
   public static Optional<State> read(ItemStack press, HolderLookup.Provider registries) {
      if (!isPress(press) || registries == null) {
         return Optional.empty();
      }
      CompoundTag root = customData(press);
      if (!root.contains(KEY)) {
         return Optional.of(State.EMPTY);
      }
      if (!root.contains(KEY, Tag.TAG_COMPOUND)) {
         return Optional.empty();
      }
      CompoundTag payload = root.getCompound(KEY);
      if (!payload.contains(VERSION, Tag.TAG_INT) || payload.getInt(VERSION) != 1
         || !ALLOWED_KEYS.containsAll(payload.getAllKeys())) {
         return Optional.empty();
      }
      try {
         Optional<ItemStack> bread = parseSlot(payload, BREAD, registries);
         Optional<ItemStack> apple = parseSlot(payload, APPLE, registries);
         Optional<ItemStack> seeds = parseSlot(payload, SEEDS, registries);
         if (bread.isEmpty() || apple.isEmpty() || seeds.isEmpty()) {
            return Optional.empty();
         }
         State result = new State(bread.get(), apple.get(), seeds.get());
         return result.isValid() ? Optional.of(result) : Optional.empty();
      } catch (RuntimeException invalid) {
         // Fail closed; never modify or normalize an ambiguous stored payload.
         return Optional.empty();
      }
   }

   /** Pure admission simulation. A different seed variety or incompatible component set fails. */
   public static Optional<Plan> planInsert(State current, ItemStack offered) {
      if (current == null || !current.isValid() || offered == null || offered.isEmpty()
         || offered.getMaxStackSize() < 1 || offered.getCount() > offered.getMaxStackSize()) {
         return Optional.empty();
      }
      Compartment kind = classify(offered);
      if (kind == null || !validStack(offered.copyWithCount(1), kind)) {
         return Optional.empty();
      }
      ItemStack existing = current.contents(kind);
      if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, offered)) {
         return Optional.empty();
      }
      // Component overrides may lower a normally stackable ingredient's native maximum.
      int effectiveCapacity = Math.min(CAPACITY_PER_COMPARTMENT, offered.getMaxStackSize());
      int amount = Math.min(offered.getCount(), effectiveCapacity - existing.getCount());
      if (amount <= 0) {
         return Optional.empty();
      }
      ItemStack newContents = offered.copyWithCount(existing.getCount() + amount);
      State updated = current.with(kind, newContents);
      return updated.isValid() ? Optional.of(new Plan(updated, offered.copyWithCount(amount))) : Optional.empty();
   }

   /** Pure withdrawal simulation; caller must independently validate destination capacity. */
   public static Optional<Plan> planWithdraw(State current, Compartment compartment, int possibleToReceive) {
      if (current == null || !current.isValid() || compartment == null || possibleToReceive <= 0) {
         return Optional.empty();
      }
      ItemStack existing = current.contents(compartment);
      if (existing.isEmpty()) {
         return Optional.empty();
      }
      int amount = Math.min(existing.getCount(), possibleToReceive);
      ItemStack remainder = existing.getCount() == amount
         ? ItemStack.EMPTY : existing.copyWithCount(existing.getCount() - amount);
      State updated = current.with(compartment, remainder);
      return updated.isValid() ? Optional.of(new Plan(updated, existing.copyWithCount(amount))) : Optional.empty();
   }

   /**
    * Stage a new, detached press ItemStack, never mutating the actual source.
    * This is NOT a transaction commit, anti-ABA fence, admission check or permission to replace an inventory slot.
    */
   public static Optional<ItemStack> stage(ItemStack press, HolderLookup.Provider registries, State expected, State desired) {
      if (expected == null || desired == null || !expected.isValid() || !desired.isValid()) {
         return Optional.empty();
      }
      Optional<State> actual = read(press, registries);
      if (actual.isEmpty() || !actual.get().sameContents(expected)) {
         return Optional.empty();
      }
      if (actual.get().sameContents(desired)) {
         // A logical no-op must not rewrite an existing representation.
         return Optional.of(press.copy());
      }
      try {
         ItemStack staged = press.copy();
         CompoundTag root = customData(staged);
         if (desired.sameContents(State.EMPTY)) {
            root.remove(KEY);
         } else {
            CompoundTag payload = new CompoundTag();
            payload.putInt(VERSION, 1);
            writeSlot(payload, BREAD, desired.bread, registries);
            writeSlot(payload, APPLE, desired.apple, registries);
            writeSlot(payload, SEEDS, desired.seeds, registries);
            root.put(KEY, payload);
         }
         if (root.isEmpty()) {
            staged.remove(DataComponents.CUSTOM_DATA);
         } else {
            staged.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
         }
         // Explicit round-trip check: reject any non-representable staged state.
         Optional<State> verified = read(staged, registries);
         return verified.isPresent() && verified.get().sameContents(desired) ? Optional.of(staged) : Optional.empty();
      } catch (RuntimeException invalid) {
         return Optional.empty();
      }
   }

   private static Optional<ItemStack> parseSlot(CompoundTag payload, String key, HolderLookup.Provider registries) {
      if (!payload.contains(key)) {
         return Optional.of(ItemStack.EMPTY);
      }
      if (!payload.contains(key, Tag.TAG_COMPOUND)) {
         return Optional.empty();
      }
      CompoundTag saved = payload.getCompound(key);
      Optional<ItemStack> parsed = ItemStack.parse(registries, saved);
      if (parsed.isEmpty() || parsed.get().isEmpty()) {
         return Optional.empty();
      }
      Tag reserialized = parsed.get().save(registries);
      // Reject data the codec would silently drop or normalize.
      return saved.equals(reserialized) ? Optional.of(parsed.get().copy()) : Optional.empty();
   }

   private static void writeSlot(CompoundTag payload, String key, ItemStack stack, HolderLookup.Provider registries) {
      if (!stack.isEmpty()) {
         Tag encoded = stack.save(registries);
         if (!(encoded instanceof CompoundTag compound)) {
            throw new IllegalStateException("Expected a compound for Muslee Bar Press reserve item");
         }
         payload.put(key, compound.copy());
      }
   }

   private static boolean isPress(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getCount() == 1 && stack.getItem() instanceof MusliBarPressItem;
   }

   private static boolean validStack(ItemStack stack, Compartment compartment) {
      if (stack.isEmpty()) {
         return true;
      }
      if (stack.getCount() < 1 || stack.getMaxStackSize() < 1
         || stack.getCount() > Math.min(CAPACITY_PER_COMPARTMENT, stack.getMaxStackSize())) {
         return false;
      }
      return switch (compartment) {
         case BREAD -> stack.is(Items.BREAD);
         case APPLE -> stack.is(Items.APPLE);
         // Persistence cannot depend on today's tag membership: TOOLS-194
         // requires revoked extensions to remain readable and withdrawable.
         // The codec proves identity/components; admission is checked separately.
         case SEEDS -> true;
      };
   }

   /** Current admission only; never use this to validate persisted stock. */
   public static boolean isSeedAdmissible(ItemStack ingredient) {
      return ingredient != null && !ingredient.isEmpty()
         && ingredient.is(MusliBarPressItem.ACCEPTED_SEEDS);
   }

   /**
    * Ingredient eligibility only, not permission to press. Future integration must
    * revalidate the live tags and its complete transaction immediately before use.
    */
   public static boolean hasAdmissibleIngredients(State current) {
      return current != null && current.isValid() && !current.bread.isEmpty()
         && !current.apple.isEmpty() && isSeedAdmissible(current.seeds);
   }

   private static Compartment classify(ItemStack ingredient) {
      if (ingredient.is(Items.BREAD)) {
         return Compartment.BREAD;
      }
      if (ingredient.is(Items.APPLE)) {
         return Compartment.APPLE;
      }
      if (isSeedAdmissible(ingredient)) {
         return Compartment.SEEDS;
      }
      return null;
   }

   private static CompoundTag customData(ItemStack stack) {
      return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
   }
}
