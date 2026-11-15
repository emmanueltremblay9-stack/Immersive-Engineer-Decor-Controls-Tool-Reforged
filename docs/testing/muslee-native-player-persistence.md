# Native player persistence characterization

This suite exercises real `ServerPlayer`, menu, inventory, ItemStack and NBT code.
It characterizes the existing missing persistence participant; it does not install
one. No player save hook, journal writer, filesystem change or activation is added.

The fixture is a real ServerPlayer with only a no-op FakePlayer network transport,
following the existing CakeMealGameTests precedent. Native saveWithoutId/load are
called in memory; these tests do not exercise disk replacement, integrated-host
world saves, backups, fsync or restart durability.

## Serialization cases

- A real press with generic reserve counts, damage and custom name survives native
  player serialization and load. A receipt-like persistent marker also survives,
  while four named bars on the menu cursor do not. The marker is deliberately not
  a transaction receipt or execution proof. The test proves why its presence alone
  would be insufficient. Saving does not transfer the original cursor to inventory.
- Empty, one-bar, four-bar and full-bar cursors produce the same serialized Inventory
  list. Fresh player load does not restore those cursor stacks or put them in inventory.

## Native menu removal cases

- A fresh live, not-disconnected ServerPlayer with inventory room receives exactly the four
  cursor bars in inventory after native menu removal. Cursor clears, no matching
  world item exists, and the transferred bars then survive native player save/load.
- With main inventory full, native removal clears the cursor and drops exactly four
  uniquely identified bars into real world ItemEntities. A player-only snapshot does
  not contain them. The test cleans up its uniquely identified dropped entities.

These call the actual menu removal method, not the network close protocol. They do
not test disconnect/death events, crash persistence of world entities or the full
client/server close sequence. Fresh fixtures assert alive and not disconnected;
no hasDisconnected override changes that native decision.

These assertions pin current behavior and must be reconsidered when a qualified
participant-aware persistence adapter intentionally changes that contract.

## Exact vanilla source provenance

The official [Minecraft 1.21.1 manifest](https://piston-meta.mojang.com/v1/packages/82ba6501da174a665c1787775f946487422f2432/1.21.1.json)
provided these artifacts; downloaded SHA1 values matched the manifest:

- [client.jar](https://piston-data.mojang.com/v1/objects/30c73b1c5da787909b2f73340419fdf13b9def88/client.jar): `30c73b1c5da787909b2f73340419fdf13b9def88`.
- [official mappings](https://piston-data.mojang.com/v1/objects/2244b6f072256667bcd9a73df124d6c58de77992/client.txt): `2244b6f072256667bcd9a73df124d6c58de77992`.

Inspection used JDK javap plus official mappings. The following are original source
line numbers from mappings/bytecode LineNumberTables, not claims of a published
source repository:

- Player.addAdditionalSaveData893–923 and readAdditionalSaveData845–889 serialize
  Inventory without menu cursor.
- AbstractContainerMenu.removed558–569 drops carried state for dead/disconnected
  server players, otherwise places it back into inventory, then empties cursor.
- ServerPlayer.closeContainer1213–1215 and doCloseContainer1219–1223 send close,
  remove/transfer menu state, then switch to inventoryMenu.
- ServerPlayer.drop1846–1860 adds the dropped ItemEntity to the level, crossing out
  of the player-file persistence domain.
- PlayerDataStorage.save36–47 snapshots player NBT, writes compressed temporary
  data and calls Util.safeReplaceFile while catching/logging exceptions.
- PlayerDataStorage.load78–88 can select `.dat_old` after failure of `.dat`.
- Util.safeReplaceFile796–797 discards safeReplaceOrMoveFile's boolean outcome.
  The latter800–828 retries multi-step backup/delete/move and can restore a backup.
  Renamer711–715 uses Files.move with no ATOMIC_MOVE and catches IOException.
- NbtIo.writeCompressed71–75 closes ordinary compressed streams, without force/fsync.

Exact NeoForge21.1.230 patch provenance is recorded in
[muslee-persistence-contract-simulation.md](muslee-persistence-contract-simulation.md).
Its post-save event does not repair the discarded replacement outcome or provide
an atomic durability receipt.

## Consequence for integration

A press-only attachment or sidecar cannot qualify cursor-only output. A future
participant must own normal snapshot selection and later cursor transfers, including
inventory and world drops. Reconstructing an authoritative cursor during normal
player loading needs an explicit ownership contract distinct from journal replay.
No timeout, old snapshot, marker presence or successful serialization proves that a
transaction did not execute or that its output is durable. Existing gates stay shut.
