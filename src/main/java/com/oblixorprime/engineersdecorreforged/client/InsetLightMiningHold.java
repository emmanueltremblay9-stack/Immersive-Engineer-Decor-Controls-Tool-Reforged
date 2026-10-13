package com.oblixorprime.engineersdecorreforged.client;

/** Prevents one held attack from falling through a removed lamp into its retained deck. */
final class InsetLightMiningHold {
   private Object world;
   private Object player;
   private Long owner;

   void completed(Object world, Object player, long owner) {
      this.world = world;
      this.player = player;
      this.owner = owner;
   }

   boolean suppress(Object world, Object player, boolean attackDown, Long target, boolean retainedOwner) {
      if (owner == null) {
         return false;
      }
      if (this.world != world || this.player != player || !attackDown
         || !owner.equals(target) || !retainedOwner) {
         reset();
         return false;
      }
      return true;
   }

   void reset() {
      world = null;
      player = null;
      owner = null;
   }
}
