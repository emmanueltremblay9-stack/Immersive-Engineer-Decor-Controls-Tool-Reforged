# Native catwalk and railing sides

## Scope and controls

This change restores the existing side-editing behavior advertised by Steel
Catwalk and Steel Railing tooltips. It uses their existing models and blockstate
side flags, without a multipart dependency or additional model assets.

- Hold a Steel Railing and use it on the edge of a Steel Catwalk or an existing
  Steel Railing to add that side. Use the same edge again to remove it.
- North, east, south and west are independent. Adjacent corners, opposite sides,
  three-sided end caps and all four sides are supported.
- The nearest horizontal edge to the hit point determines the side, including
  when the player hits the inside face of an existing rail. Exact ties prefer
  the player's horizontal direction when it is tied, then north/east/south/west.
- Initial standalone placement uses the same hit-edge selection.
- To add a missing side to a standalone railing, aim down through that empty
  edge at the top of the supporting block. Normal block placement merges the
  new side into the existing railing above, like combining slabs. This makes
  opposite sides reachable even though the missing side has nothing to hit.
  Click a visible railing side directly to remove it.
- Ordinary neighbor changes do not add or erase selected sides. Waterlogging,
  saved block states, rotations and mirrors preserve the configuration.
- Sneaking retains Minecraft's normal item-placement bypass rather than editing
  the existing block.

Steel Catwalk Stairs and Raised Steel Catwalk are outside this change. Their
existing behavior and assets are unchanged.

Four sides form a continuous perimeter for ordinary ground-level movement, not
a universal mob cage. The existing collision height is unchanged; jumping,
flying, climbing or teleporting mobs can require different containment.

## Survival, creative and old worlds

A newly placed standalone railing owns one base railing item. Each further
survival addition consumes one railing. A catwalk starts with no railing items;
each survival addition consumes one. A persisted `paid_railings` count records
only these purchased additions.

Removing a side refunds one recorded addition when available. Removing the final
standalone side also returns its base item and removes the block; waterlogged
railings leave source water behind. Breaking the block returns its ordinary
base loot plus the recorded additions when the ordinary loot table allows the
base drop. Full-inventory refunds fall back to a dropped item.

The credit count belongs to the whole block, rather than a particular side.
Removing a formerly free side may release a purchased credit, but total refunds
cannot exceed the block's base item and recorded purchases. Credits are capped
by the number of remaining sides, excluding the standalone base side.

Creative additions consume nothing and create no refund credits. Creative
removals destroy any corresponding credit without a refund, matching creative
destruction rather than leaving refundable credits behind.

Old saves default to zero paid additions, preserving their historical base-only
drops. Old automatically generated sides remain present until edited and do not
produce new items when removed. The old standalone all-false state already
displays a north rail; editing or transforming it makes that north side explicit
before changing the selected side. No world migration or model replacement is
required.

## Verification

`NativeRailingGameTests` exercises native server interaction through
`FakePlayer.gameMode.useItemOn`, rather than calling only a block handler. Its
regression matrix covers every side and configuration, persistence, transforms,
collision, existing model selection, and item accounting. Individual test names
describe the covered cases.

The blockstate JSON predicates omit `paid_railings`, so each of the 160 possible
states per block still selects exactly one existing model. Static enumeration
of both blockstate files confirms that property coverage. The models already
provide all four edge directions and all corner combinations.

Execution results belong to the exact CI commit. This document is not a claim
of a completed client playtest, a passing CI run, or release qualification.
