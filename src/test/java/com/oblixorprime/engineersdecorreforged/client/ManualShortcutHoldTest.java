package com.oblixorprime.engineersdecorreforged.client;

/** JUnit entry point plus standalone runner; no Minecraft bootstrap needed. */
public final class ManualShortcutHoldTest {
   @org.junit.Test
   public void holdScenarios() {
      main(new String[0]);
   }

   public static void main(String[] args) {
      ManualShortcutHold<String> hold = new ManualShortcutHold<>();
      check(!hold.update("a", false, 0), "not pressed");
      check(!hold.update("a", true, 0), "start");
      check(!hold.update("a", true, 499), "threshold");
      check(hold.update("a", true, 500), "open");
      check(!hold.update("a", true, 1000), "one shot");
      check(!hold.update("b", true, 1500), "one shot across target changes");
      hold.update(null, false, 1501);
      check(!hold.update("a", true, 2000), "release rearms");
      check(!hold.update("b", true, 2499), "target changes reset timer");
      check(!hold.update("b", true, 2500), "old timer discarded");
      check(hold.update("b", true, 2999), "new target completes");
      hold.reset();
      hold.update("a", true, 0);
      hold.update(null, true, 400);
      check(!hold.update("a", true, 800), "leaving tooltip resets");
      check(hold.update("a", true, 1300), "reentered tooltip completes");
      hold.reset();
      hold.update("a", true, 0);
      hold.update("a", false, 400);
      check(!hold.update("a", true, 700), "interrupted hold resets");
      check(hold.update("a", true, 1200), "fresh hold completes");
      hold.reset();
      check(!hold.update(null, true, 9000), "unmapped items never open");
      check(!hold.update(null, true, 10000), "unmapped items stay inert");
      System.out.println("Manual shortcut hold: 16 assertions passed");
   }

   private static void check(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }
}
