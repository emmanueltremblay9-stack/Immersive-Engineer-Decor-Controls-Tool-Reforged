# Iron Hatch: ladder continuation at the open leaf

## Cause and scope

The hatch is a `HorizontalDirectionalBlock`, not a `TrapDoorBlock`. NeoForge
replaces the vanilla `LivingEntity.onClimbable` trapdoor fallback with
`CommonHooks.isLivingOnLadder`, which queries each block's `isLadder` hook.
NeoForge gives `TrapDoorBlock` a ladder-continuation override, but the custom
hatch never inherited it. Membership in `minecraft:trapdoors` does not provide
that behavior. The confirmed regression is a missing bridge from the ladder
below into the open hatch; a wrong-side movement effect in a reporter modpack
has not been reproduced.

Implement the missing `HatchBlock.isLadder` override using NeoForge's
`makesOpenTrapdoorAboveClimbable` extension point on the block immediately below.
Return false when the hatch is closed. The hatch's `facing` denotes its physical
hinge edge, opposite vanilla's facing convention, so translate the temporary
query state's facing before calling the lower block's hook. For example, a
north-edge hatch continues a south-facing ladder mounted on the north wall.

No world state is changed by this query. Registry IDs, the three-property saved
state schema, placement rules, models, collision, tags, sounds, and manual and
redstone interaction remain unchanged. Existing hatches need no state migration
to receive the bridge. This does not make the hatch an unconditional ladder or
add continuation through a gap.

## Pinned API verification

Official NeoForge Maven source and userdev JARs for **21.1.230**, **21.1.233**,
and **21.1.248** have byte-identical relevant sections:

- `CommonHooks.java:382`: `isLivingOnLadder`, including the optional full-AABB scan
- `IBlockExtension.java:181`: default `isLadder`
- `IBlockExtension.java:194`: default `makesOpenTrapdoorAboveClimbable`
- `LivingEntity.java.patch:343-354`: replacement of the vanilla fallback
- `TrapDoorBlock.java.patch:9-17`: open-trapdoor ladder-continuation override

Sources are the `neoforge-VERSION-sources.jar` and
`neoforge-VERSION-userdev.jar` artifacts in the official Maven directories for
[21.1.230](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.230/),
[21.1.233](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.233/), and
[21.1.248](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.248/).
API identity is not a runtime compatibility result for those three versions.

## Regression coverage and evidence

Seven required `HatchClimbingGameTests` query actual `Player.onClimbable` and the
NeoForge block hook. Fresh players have their feet in the hatch block and their
entire AABB above the ladder below; the optional full-bounding-box ladder scan
therefore cannot hide a missing hatch bridge.

- All four hinge directions with vanilla Ladder, Metal Rung Ladder, and
  Staggered Metal Steps; both powered values, independent vanilla trapdoor
  reference, real ladder support, and shape-edge alignment
- Opposite/perpendicular ladders, closed hatches, missing/non-ladder blocks, and
  a one-block vertical gap
- Actual manual opening/closing and redstone power transitions
- Every facing/open/powered saved-state combination, serialization round trip,
  unchanged collision, and no query mutation
- Actual item placement against floor, ceiling, and four walls for every player
  direction, followed by aligned ladder continuation

The tests were committed first as
`7485e4f55fd6561f17ae48c8829aa58176049230`.
[RED Build run 38288241929](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/38288241929)
passed compilation and normal tests, then ran 223 GameTests with exactly four
expected positive-path failures. All three negative-path tests and 216 prior
GameTests passed. The tests are unchanged by the implementation fix.

Corrected-commit GREEN CI, actual player movement through the final packaged
JAR, client visuals, and packaged dedicated-server checks remain separate
qualification gates. This change does not claim publication or final artifact
qualification.

A subsequent metal-ladder-only physical obstruction was reproduced after this
hook correction: see [metal ladder hatch clearance](metal-ladder-hatch-clearance.md).
The original tests query climbing eligibility and do not establish unobstructed
physical movement from below. The later regression uses collision-resolved entity
movement and restores the metal ladders' vanilla wall clearance.
