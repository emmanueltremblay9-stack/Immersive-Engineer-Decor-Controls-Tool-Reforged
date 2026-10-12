# Steel Mesh Fence Gate: passage and fence connections

## Reproduced causes

The open gate reused its coarse selection outline for collision. In its north
orientation the left post ended at `3/16`, while the combined folded-side box
started at `12/16`. The remaining `9/16 = 0.5625` blocks cannot admit a normal
`0.6`-block-wide player. The earlier center-probe test was narrower than the
player and therefore did not establish actual passage.

Steel Mesh Fence uses vanilla `WallBlock`. Its gate-connection rule recognizes
`FenceGateBlock`, but the segmented mesh gate extends
`HorizontalDirectionalBlock`. The actual world tests reproduce missing
connections on both intended gate-post sides, regardless of placement order,
open state, or segment.

## Minimal correction

- Keep the existing gate outline and all model/texture resources. Separate open
  collision from selection and start its folded-side box at the structural
  post's inner edge, `13/16`, leaving `10/16 = 0.625` blocks of passage. Both
  structural sides still collide. This deliberately excludes the thin inward
  decorative leaf from movement collision; it does not remove all open collision.
  Closed collision is unchanged.
- Register Steel Mesh Fence as a small `WallBlock` subclass. For a horizontal
  segmented-gate neighbor only, give the inherited wall calculation a temporary
  vanilla-gate state with the same facing/open values. Nothing is substituted
  in the world. Reuse vanilla placement/update calculations for connections,
  side heights and center posts rather than copying its wall algorithm.
- Replay only the relevant horizontal calculations during placement, preserving
  the original waterlogged value without scheduling extra placement-time fluid
  ticks. Ordinary neighbor updates retain vanilla fluid scheduling. Gates above
  or below the wall keep their actual geometry.

Registry IDs, gate `facing/open/segment` and fence
`north/east/south/west/up/waterlogged` schemas, assets, interactions, segment
linking/removal, loot and existing manual-only redstone behavior remain intact.
No `powered` state or automatic redstone opening is added. Existing gate
collision changes immediately on loading the fix; saved fence connections may
need a neighbor update. No world scan or state migration is performed.

## Tests-first runtime evidence

The ten required `SteelMeshGateGameTests` were frozen before production edits at
commit `4f2b814cca1c4b7551f26ba334733bdc52671fdc` on
[draft PR 45](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/45).
Their unchanged SHA-256 is
`c7ebbd93457c8066e15c885e1812fe1d63feec0d43251d7050b16a3370b10740`.

[RED Build run 38308824127](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/38308824127)
passed compilation and executed 239 required GameTests. Exactly six new tests
failed as expected:

- Open passage and NBT-restored open passage each failed all 24 combinations:
  four facings, both travel directions, lower-only/upper-only/paired segments.
- The connection matrix failed its 64 aligned cases, covering both placement
  orders, open/closed gates and both segment values.
- Paired connections, vanilla wall-topology parity, and real above-gate topology
  each reproduced the missing connection.

All 229 pre-existing tests and four new controls passed: closed-gate physical
blocking, physical post/folded-side blocking without neighboring fences, saved
state/schema preservation, and existing manual interaction under real redstone
neighbor updates. The unchanged suite also checks gate removal, both click
halves, wrong-axis nonconnections, actual waterlogged placement, above-block
changes, and wall-state parity against a real vanilla gate.

## Qualification boundary

Movement tests call real `Entity.move(MoverType.SELF, ...)` with a survival
player and prescribed horizontal displacements. They are collision-engine
tests, not full `Player.travel`/input/gravity or visual client tests. NBT state
round-tripping is not a world save/reload. The source correction still needs
GREEN CI on its exact commit, then visible client traversal and a packaged
dedicated-server check using the final independently hash-identified JAR.

All candidate JARs built before these gate corrections are superseded for
qualification, including artifacts containing both prior hatch/ladder fixes
and carrying the same `1.1.57-reconstructed` version string. This record does
not establish a qualified release artifact or claim publication.
