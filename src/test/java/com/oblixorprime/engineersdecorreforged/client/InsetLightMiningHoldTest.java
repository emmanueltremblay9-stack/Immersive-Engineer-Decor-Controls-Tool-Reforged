package com.oblixorprime.engineersdecorreforged.client;

/** Pure input-latch regression tests; actual client prediction has a separate runtime test. */
public final class InsetLightMiningHoldTest {
   @org.junit.Test
   public void completedLampHoldScenarios() {
      main(new String[0]);
   }

   public static void main(String[] args) {
      InsetLightMiningHold hold = new InsetLightMiningHold();
      Object world = new Object();
      Object player = new Object();
      check(!hold.suppress(world, player, true, 12L, true), "ordinary/denied mining has no completed lamp latch");
      hold.completed(world, player, 12L);
      for (int tick = 0; tick < 100; tick++) {
         check(hold.suppress(world, player, true, 12L, true), "held attack cannot fall through after native cooldown");
      }
      check(!hold.suppress(world, player, false, 12L, true), "physical release clears the latch");
      check(!hold.suppress(world, player, true, 12L, true), "fresh deliberate click can mine the retained deck");
      hold.completed(world, player, 12L);
      hold.reset();
      check(!hold.suppress(world, player, true, 12L, true), "release/repress between ticks also permits a fresh click");
      hold.completed(world, player, 12L);
      check(!hold.suppress(world, player, true, 13L, true), "retargeting a different owner is never swallowed");
      check(!hold.suppress(world, player, true, 12L, true), "returning after retargeting permits a deliberate new mine");
      hold.completed(world, player, 12L);
      check(!hold.suppress(world, player, true, null, false), "looking into air clears the latch");
      check(!hold.suppress(world, player, true, 12L, true), "returning from air permits a new mine");
      hold.completed(world, player, 12L);
      check(hold.suppress(world, player, true, 12L, true), "successful client lamp prediction starts protection");
      check(hold.suppress(world, player, true, 12L, true), "relit correction still names the retained catwalk owner");
      check(hold.suppress(world, player, true, 12L, true), "later server-only lamp removal cannot expose the deck to held attack");
      check(!hold.suppress(world, player, false, 12L, true), "release after correction permits an explicit retry");
      check(!hold.suppress(world, player, true, 12L, true), "fresh click after correction is not swallowed");
      hold.completed(world, player, 12L);
      check(hold.suppress(world, player, true, 12L, true), "denied prediction keeps one held attack latched to its owner");
      check(!hold.suppress(world, player, false, 12L, true), "release also clears a permanently denied prediction");
      check(!hold.suppress(world, player, true, 12L, true), "a fresh attempt after a protection denial is allowed");
      hold.completed(world, player, 12L);
      check(!hold.suppress(world, player, true, 12L, false), "an unrelated replacement block clears latch");
      check(!hold.suppress(world, player, true, 12L, true), "returning to a replacement cannot resurrect the old latch");
      hold.completed(world, player, 12L);
      check(!hold.suppress(new Object(), player, true, 12L, true), "world changes clear latch even at same coordinates");
      hold.completed(world, player, 12L);
      check(!hold.suppress(world, new Object(), true, 12L, true), "player replacement clears latch");
      hold.completed(world, player, Long.MIN_VALUE);
      check(hold.suppress(world, player, true, Long.MIN_VALUE, true), "packed coordinates use equality rather than object identity");
      hold.completed(world, player, Long.MAX_VALUE);
      check(hold.suppress(world, player, true, Long.MAX_VALUE, true), "latest actual completion replaces the previous owner");
      hold.reset();
      check(!hold.suppress(world, player, true, Long.MAX_VALUE, true), "logout reset clears all state");
      System.out.println("Inset light mining hold: 123 assertions passed");
   }

   private static void check(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }
}
