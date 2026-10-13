# Raised steel catwalk placement restoration

## Cause and reference

The recovered registration used a generic `VariantOnlyBlock`, selecting variant
`floorMod(x * 31 + y * 7 + z, 5)`. The choice was unrelated to supports or player
actions, and every variant had the same collision geometry.

The reference is `stfwi/engineers-decor` commit
`a561e61e35ce0367088356967e1ea4cad5c1f27d`,
`EdCatwalkTopAlignedBlock.java` and `ModContent.java` lines 493–522.

## Restored behavior

- Variant 0: plain deck, at y = 14–16 pixels
- Variant 1: deck with a thick-steel-pole connection below
- Variant 2: deck with a thin-steel-pole connection below
- Variant 3: frame selected by clicking the sturdy top of an ordinary support
- Variant 4: light explicitly installed by using an iron inset light underneath

Pole heads count as their matching steel poles. Pole variants change when the
support changes and revert to plain when it disappears. Frame placement remains
an explicit choice on ordinary supports; a steel pole below takes precedence.
Removing that pole returns to plain. Neighbor changes never overwrite a light.
Removing a light discovers any pole that was added beneath it while installed.

Holding another raised catwalk and clicking its top or underside extends the
platform horizontally in the player's facing direction. This uses the normal
NeoForge item-placement path, including collision, replacement, item cost and
the cancellable placement event. Occupied targets do not cause vertical fallback
or skipping to a farther block.

Each variant has its original structural selection/collision shape. Waterlogging
is restored. Only the light variant emits light, at level 14 to match the existing
iron inset light. Installed survival lamps are returned once on removal or normal
whole-block mining. Loot honors the existing base table and explosion conditions.

## Save compatibility and material conservation

The existing registry ID, item, recipe, five model selectors and numeric `variant`
values are unchanged. All five legacy values load without a conversion. New
`waterlogged` and `paid_light` properties default to false in old saves.

Old coordinate-selected light variants never consumed an inset light. They keep
their appearance and now emit light, but cannot be cashed out for a free lamp.
Creative installation likewise grants no paid credit. A survival installation
records one paid lamp; removal clears the credit before returning it. A full
inventory drops the refund. The credit survives both NBT and block-state-codec
serialization. Side and top neighbor updates preserve legacy choices; changing
the actual lower support recomputes only the non-light connection.

## Verification

Eleven required `RaisedCatwalkGameTests` cover real item placement at all five
former coordinate residues, all six clicked faces, plain/thick/thin/head support,
sturdy-top frames, wet placement, reversible neighbor changes, all variant
shapes, every saved state, survival and creative lamp economics, legacy saves,
both interaction hands, full inventory refunds, real game-mode mining,
adventure/spectator denial, horizontal extension and placement cancellation.

Java 21 syntax parsing and `git diff --check` were run locally. Local Gradle was
blocked by the unavailable wrapper distribution endpoint; compilation and runtime
GameTest results must be read from the exact final commit's CI. These checks do
not constitute client rendering or in-game visual qualification.

In a separate test world, verify plain and framed placements, both pole sizes and
heads, extension above/below, light emission and removal, waterlogged models,
selection outlines and persistence after reopening the world. No live user save
or running client was accessed for this change.
