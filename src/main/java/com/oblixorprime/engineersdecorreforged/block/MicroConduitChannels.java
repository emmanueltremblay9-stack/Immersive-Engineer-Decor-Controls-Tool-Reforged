package com.oblixorprime.engineersdecorreforged.block;

/** Minimal nine-lane routing invariant independent of client visuals and physical conduit layout. */
public final class MicroConduitChannels {
   public static final int COUNT = 9;

   private MicroConduitChannels() { }

   public static boolean valid(int channel) {
      return channel >= 0 && channel < COUNT;
   }

   /** Circuit 1 cannot feed circuits 2..9 without an expressly implemented bridge. */
   public static boolean same(int source, int destination) {
      return valid(source) && valid(destination) && source == destination;
   }
}
