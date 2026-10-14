package com.oblixorprime.engineersdecorreforged.block;

/** Reproduces real Entity's equal-ID client/server keys without launching Minecraft. */
public final class MiningIntentStoreTest {
   private static int assertions;

   @org.junit.Test
   public void integratedClientAndServerIntentLifecyclesStayIndependent() {
      main(new String[0]);
   }

   public static void main(String[] args) {
      assertions = 0;
      MiningIntentStore<EntityId, String> intents = new MiningIntentStore<>();
      EntityId local = new EntityId(42);
      EntityId server = new EntityId(42);
      EntityId other = new EntityId(43);
      check(local != server && local.equals(server), "fixture must reproduce equal network IDs across distinct players");
      check(local.hashCode() == server.hashCode(), "equal Entity IDs must use the same hash");
      intents.put(local, true, "client lamp START");
      intents.put(server, false, "server lamp START");
      check("client lamp START".equals(intents.get(local, true)), "server START cannot overwrite client HOLD intent");
      check("server lamp START".equals(intents.get(server, false)), "client and server hold distinct world-bound intentions");
      check("client lamp START".equals(intents.remove(local, true)), "native client completion consumes its own intent");
      check("server lamp START".equals(intents.get(server, false)), "client prediction cannot consume authoritative completion");
      intents.put(local, true, "client deck START");
      check("server lamp START".equals(intents.remove(server, false)), "server completion consumes only its own intent");
      check("client deck START".equals(intents.get(local, true)), "server completion cannot clear a later client START");
      intents.put(server, false, "server deck START");
      intents.put(other, false, "other player START");
      intents.clearClient();
      check(intents.get(local, true) == null, "disconnect clears every client intent");
      check("server deck START".equals(intents.get(server, false)), "client disconnect cannot remove equal-ID server intent");
      check("other player START".equals(intents.get(other, false)), "disconnect cannot remove another server player's intent");
      intents.remove(server, false);
      check(intents.get(server, false) == null, "server abort/logout removes its intent");
      check("other player START".equals(intents.get(other, false)), "server abort/logout remains player-specific");
      intents.put(local, true, "new client START");
      intents.remove(server, false);
      check("new client START".equals(intents.get(local, true)), "absent server cleanup cannot erase an equal-ID client key");
      System.out.println("Mining intent side isolation: " + assertions + " assertions passed");
   }

   private record EntityId(int id) {}

   private static void check(boolean condition, String message) {
      assertions++;
      if (!condition) throw new AssertionError(message);
   }
}
