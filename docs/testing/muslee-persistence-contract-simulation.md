# Muslee persistence failure-contract simulation

Status: TEST-ONLY MODEL. No production adapter, save/load hook, attachment, writer,
filesystem operation or runtime gate is added. Passing these tests does not qualify
Minecraft persistence, durability, memory use or actual inventory conservation.

## Executable contract

`MusleePersistenceContractCases` models one four-bar press from one unit of each
reserve and durability64 to empty reserves, four cursor bars and durability63.
Scalar records stand in for complete canonical player state; they are NOT a real
ItemStack/component encoding. A synthetic atomic snapshot contains affected state
and its receipt together. The oracle admits only the complete before or after pair.

Seven simulated interruption points cover before engagement, after engagement,
before replacement, uncertain replacement retaining old or new state, after replacement
and after terminal journal recording. Reconciliation never writes, replays, repairs
or synthesizes a terminal record. A retained old snapshot after engagement does not
prove cancellation. A simulated matching receipt can describe execution, but missing
terminal history keeps the domain suspended. Repeated reconciliation is read-only. A separate no-effect receipt is modeled for
qualified cancellation; it is never inferred from the unchanged snapshot alone.

Negative controls deliberately break policies: dropped receipt, receipt without
state, stale later save, duplicated cursor, false cancellation, recovery replay,
wrong output destination and early publication. Fault controls run across all seven
cut points, checking both inactive and active failure boundaries. The read-only oracle
checks write counts as well as evidence, so even an equal-state replay is detected. The independent oracle rejects the
inconsistent pairs/outcomes or detects recovery mutation. This is evidence that the
model checks those requirements, not evidence that a real adapter obeys them.

The standalone Java21 suite currently passes698 assertions. JUnit invokes the same
suite. Future engine tests must replace the synthetic storage with the real boundary
and inject failures into actual saves and lifecycle transitions. Do not treat a
simulated recovery result as real authoritative evidence or an activation permit.

## Exact dependency source audit

Official NeoForge21.1.230 artifacts were inspected read-only; computed SHA256 matched
the corresponding publisher checksum:

- [Sources JAR](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.230/neoforge-21.1.230-sources.jar):
  `31a960fc4fd5f0dc5501b3429e3a39c30d466019b2d6da06f19ceaf733e82e32`.
- [Userdev JAR](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.230/neoforge-21.1.230-userdev.jar):
  `7fdb5f30aaf322fc951f5dcc8c9ff87cd38711239e0dc6fbaed110d8582a318a`.

Userdev config binds Minecraft1.21.1 and NeoForm1.21.1-20240808.144430.
The changelog maps the release to PR3168, merge commit
`869777fa2e6fc91b55a406e4b9733cb1dca92402`. These four artifact files were compared
byte-for-byte with that commit:

- [AttachmentHolder122–170](https://github.com/neoforged/NeoForge/blob/869777fa2e6fc91b55a406e4b9733cb1dca92402/src/main/java/net/neoforged/neoforge/attachment/AttachmentHolder.java#L122-L170): attachment serialization and deserialization exceptions log and skip entries. Required receipts need independent complete-snapshot validation.
- [PlayerDataStorage patch3–10](https://github.com/neoforged/NeoForge/blob/869777fa2e6fc91b55a406e4b9733cb1dca92402/patches/net/minecraft/world/level/storage/PlayerDataStorage.java.patch#L3-L10): save event is after replacement; exceptions are caught/logged. This event is not a precommit participant or durability receipt.
- [PlayerList patch38–61](https://github.com/neoforged/NeoForge/blob/869777fa2e6fc91b55a406e4b9733cb1dca92402/patches/net/minecraft/server/players/PlayerList.java.patch#L38-L61): integrated-host loading differs from playerIo loading; logout event precedes save.
- [ServerPlayer patch183–201](https://github.com/neoforged/NeoForge/blob/869777fa2e6fc91b55a406e4b9733cb1dca92402/patches/net/minecraft/server/level/ServerPlayer.java.patch#L183-L201): Close follows removed/transferState; respawn persistent data and clone hooks require explicit handling.

The exact userdev Entity patch204–208 stores attachments/persistent data in entity
NBT before additional save data;215–216 load them. Its full file differs from the
merge commit, so no byte-identical GitHub link is asserted for that file.

## Remaining real integration obligations

A press-only sidecar or ordinary attachment cannot establish the required boundary.
A conditional adapter for participating players must own complete staged snapshots,
required cursor/receipt validation, stale subsequent saves, backup/load selection,
and every cursor transfer (close, death, logout, drops and respawn). Co-location in
NBT alone does not prove atomic durability. Integrated-host world player data must
also participate. A custom persisted cursor must not become an unauthorized replay
or duplicate when vanilla moves/drops carried items.

Full vanilla method bodies, real cursor/death/drop paths, backup selection and
supported-platform force/replace/directory durability remain unverified. No live
save semantics are changed by this test-only proposal. Real engine failure injection,
complete ownership/mutation tracking and platform proof remain activation gates.
