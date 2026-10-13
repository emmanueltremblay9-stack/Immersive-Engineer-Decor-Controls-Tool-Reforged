package com.oblixorprime.engineersdecorreforged.client;

import java.util.Objects;

/** Small, clock-injected state machine; contains no client or Minecraft dependencies. */
final class ManualShortcutHold<T> {
   static final long HOLD_MILLIS = 500;
   private T target;
   private long since;
   private boolean fired;

   boolean update(T current, boolean down, long now) {
      if (!down) {
         reset();
         return false;
      }
      if (fired) return false;
      if (current == null) {
         target = null;
         return false;
      }
      if (!Objects.equals(target, current)) {
         target = current;
         since = now;
         return false;
      }
      if (now - since < HOLD_MILLIS) return false;
      fired = true;
      return true;
   }

   void reset() {
      target = null;
      fired = false;
   }
}
