package com.oblixorprime.engineersdecorreforged.tools;

import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve.State;
import com.oblixorprime.engineersdecorreforged.tools.muslee.MusleePressBatchPlan;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * Detached cursor/reserve deltas for the slotless press design. NOT a live menu,
 * packet handler or atomic transaction. The server adapter must qualify ownership,
 * session, revision, durability migration, capture bounds and all read/write sets
 * before applying both sides under its authoritative transaction boundary.
 */
public final class MusliBarPressCursorPlan {
   public static final int BARS_PER_BATCH = 4;

   public static final class Plan {
      private final State reserveAfter;
      private final ItemStack cursorAfter;
      private final int consumedItems;
      private final int barsProduced;
      private final int remainingDurability;

      private Plan(State reserveAfter, ItemStack cursorAfter, int consumedItems,
                   int barsProduced, int remainingDurability) {
         this.reserveAfter = reserveAfter;
         this.cursorAfter = cursorAfter.copy();
         this.consumedItems = consumedItems;
         this.barsProduced = barsProduced;
         this.remainingDurability = remainingDurability;
      }
      public State reserveAfter() { return reserveAfter; }
      public ItemStack cursorAfter() { return cursorAfter.copy(); }
      public int consumedItems() { return consumedItems; }
      public int barsProduced() { return barsProduced; }
      /** -1 for absorption: that operation neither changes nor qualifies durability. */
      public int remainingDurability() { return remainingDurability; }
   }

   private MusliBarPressCursorPlan() {}

   /**
    * Clicking the future funnel/above-opening input region absorbs the accepted amount from the
    * server's cursor snapshot; overflow stays on that cursor. No visible item slot.
    */
   public static Optional<Plan> absorb(State current, ItemStack cursor) {
      Optional<MusliBarPressReserve.Plan> insertion = MusliBarPressReserve.planInsert(current, cursor);
      if (insertion.isEmpty()) return Optional.empty();
      var accepted = insertion.get();
      ItemStack remainder = cursor.copy();
      remainder.shrink(accepted.count());
      return Optional.of(new Plan(accepted.updated(), remainder, accepted.count(), 0, -1));
   }

   /**
    * Clicking the future depicted bar plans one complete four-bar batch onto the
    * cursor, never into player inventory. Reject before consumption when it cannot
    * all fit. Durability must already be qualified against the existing 0..64
    * policy: the live legacy 128-durability item is not silently migrated here.
    */
   public static Optional<Plan> press(State current, ItemStack cursor, int verifiedRemainingDurability) {
      if (cursor == null || !MusliBarPressReserve.hasAdmissibleIngredients(current)) return Optional.empty();
      ItemStack output = new ItemStack(EngineerToolsModule.MUSLI_BAR.get(), BARS_PER_BATCH);
      int room;
      if (cursor.isEmpty()) {
         room = output.getMaxStackSize();
      } else {
         if (cursor.getCount() > cursor.getMaxStackSize()
            || !ItemStack.isSameItemSameComponents(cursor, output)) return Optional.empty();
         room = Math.min(cursor.getMaxStackSize(), output.getMaxStackSize()) - cursor.getCount();
      }
      // The batch helper is arithmetic only. Generic units have already passed
      // admission at absorption; subsequent tag revocation cannot revoke those units.
      var batch = MusleePressBatchPlan.evaluate(new MusleePressBatchPlan.Input(
         current.cereal(), current.fruit(), current.seeds(), true, verifiedRemainingDurability, room));
      if (batch.isEmpty()) return Optional.empty();
      State reserveAfter = MusliBarPressReserve.consumeBatch(current).orElseThrow();
      ItemStack cursorAfter = cursor.isEmpty() ? output : cursor.copyWithCount(cursor.getCount() + BARS_PER_BATCH);
      return Optional.of(new Plan(reserveAfter, cursorAfter, 3, batch.get().barsProduced(), batch.get().durabilityAfter()));
   }
}
