package com.oblixorprime.engineersdecorreforged.block;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Entity equality uses the network ID, shared by LocalPlayer and ServerPlayer in
 * an integrated game. Their mining intentions must never share one map entry.
 * Keep the existing weak keys and lifecycle cleanup, with independent side locks.
 */
final class MiningIntentStore<P, I> {
   private final Map<P, I> client = Collections.synchronizedMap(new WeakHashMap<>());
   private final Map<P, I> server = Collections.synchronizedMap(new WeakHashMap<>());

   I get(P player, boolean clientSide) {
      return side(clientSide).get(player);
   }

   void put(P player, boolean clientSide, I intent) {
      side(clientSide).put(player, intent);
   }

   I remove(P player, boolean clientSide) {
      return side(clientSide).remove(player);
   }

   void clearClient() {
      client.clear();
   }

   private Map<P, I> side(boolean clientSide) {
      return clientSide ? client : server;
   }
}
