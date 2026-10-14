package com.oblixorprime.engineersdecorreforged.client;

/** Prevents one held attack from falling through a removed lamp into its retained deck. */
final class InsetLightMiningHold {
   private Object world;
   private Object player;
   private Long owner;
   private String lastReset = "none";

   void completed(Object world, Object player, long owner) {
      this.world = world;
      this.player = player;
      this.owner = owner;
   }

   boolean suppress(Object world, Object player, boolean attackDown, Long target, boolean retainedOwner) {
      if (owner == null) {
         return false;
      }
      String reason = this.world != world ? "world changed"
         : this.player != player ? "player changed"
         : !attackDown ? "attack released or binding context inactive"
         : !owner.equals(target) ? "target changed"
         : !retainedOwner ? "owner replaced" : null;
      if (reason != null) {
         reset();
         lastReset = reason;
         return false;
      }
      return true;
   }

   String diagnostic() {
      return "armed=" + (owner != null) + ", owner=" + owner + ", last reset=" + lastReset;
   }

   void reset() {
      if (owner != null) lastReset = "explicit release, logout, or player clone";
      world = null;
      player = null;
      owner = null;
   }
}
