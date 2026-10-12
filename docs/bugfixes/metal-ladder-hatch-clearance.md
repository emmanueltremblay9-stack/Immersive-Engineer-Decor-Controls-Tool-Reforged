# Metal ladders: physical clearance below an open Iron Hatch

## Confirmed cause

The reporter confirmed that upward movement on Metal Rung Ladder stops below an
open Iron Hatch while a vanilla ladder passes at the same location, after the
separate hatch-continuation hook correction in PR #43.

Both Metal Rung Ladder and Staggered Metal Steps are vanilla `LadderBlock`
instances, but their registrations disabled collision with `noCollission()`.
Consequently a player moving toward the supporting wall could move all the way
to that wall. The open hatch's collision plate extends `2.875/16 = 0.1796875`
blocks into the shaft. That plate then blocks the player's head before the
player's feet reach the hatch block. A true `onClimbable()` value does not remove
this physical obstacle.

Vanilla ladder collision holds the body `3/16 = 0.1875` blocks from the wall,
leaving `1/128 = 0.0078125` blocks of clearance beside the hatch plate. The RED
runtime test independently uses `Blocks.LADDER` as its collision/movement
reference rather than manufacturing a replacement reference shape.

## Minimal correction and existing worlds

Remove `noCollission()` only from the two affected `LadderBlock` registrations.
Keep `noOcclusion()` and inherit the normal vanilla ladder collision shape.
Both types have a separately reproduced failure; the change is not based only
on similarity between their registrations.

This deliberately adds the vanilla ladder's thin physical collision strip to
existing metal ladder blocks. Saved `facing` and `waterlogged` values, registry
IDs, models, textures, selection outlines, placement/support rules, climbable
tags, and sounds are unchanged. No re-placement, state migration, or world scan
is required. The hatch's placement, state schema, collision, and PR #43 hook are
unchanged. Closed hatches remain physical barriers.

## Tests-first runtime evidence

The six required `HatchLadderTravelGameTests` were committed before the production
change as `5d5093b48dac53829a0ca4f5b1adee94ca868402` on PR
[44](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/44).
[RED Build run 38297973485](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/38297973485)
passed compilation and ran 229 required GameTests with exactly four expected
failures:

- Metal Rung Ladder collision-resolved upward movement
- Staggered Metal Steps collision-resolved upward movement
- Metal ladder movement after NBT state round-tripping
- Metal ladder collision-shape parity against the actual vanilla ladder

The two movement failures reported an initial wall gap of `0.0`, final feet at
approximately `hatchY - 1.79999995`, and the head exactly at `hatchY`. This
reproduces the underside obstruction before the hatch continuation hook can
control movement. The vanilla ascent control, closed-hatch control, and all
223 pre-existing GameTests passed.

The tests remain unchanged by the correction. Their SHA-256 is
`6a915606a22d499d93569ef7931f40d7306c8cd14eb18105f08450b97bd878d2`.
They cover four horizontal support edges, both powered values for open hatches,
both metal ladder types, actual supporting walls, preserved saved state, and
vanilla collision parity for every facing/waterlogged combination.

## Evidence limits and remaining qualification

Movement tests use real `Entity.move(MoverType.SELF, ...)` collision resolution
with prescribed upward displacements. They do not simulate full `Player.travel`,
client input, or gravity. NBT round-tripping is not an actual world save/reload.
The correction still requires GREEN CI on its exact commit, then human-visible
client traversal and an actual packaged dedicated-server check using the final
hash-identified JAR.

All earlier candidate JARs, including builds containing only the PR #43 hatch
hook correction, are superseded for qualification even if the version string is
still `1.1.57-reconstructed`. Recheck the final bytes; the RED run does not qualify
a release and no publication is claimed here.
