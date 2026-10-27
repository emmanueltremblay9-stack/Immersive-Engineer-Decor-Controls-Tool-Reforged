# Steel Double T wall placement and junctions

This change extends the existing `vertical_steel_double_t_support` item; it adds no block, recipe, texture, diagonal or connector design. The original ceiling support and its paid Iron Bulb attachment remain unchanged. The separate four-light extension is not implemented by this prerequisite.

## Placement and geometry

On any horizontal wall face, click the outer left/right quarter to place the beam horizontally along that wall. The upper/lower edges select upright placement. At corners the nearer edge wins; the central 50% width and equal-distance cases select upright. These are the retained 25/50/25 placement zones, not a new camera-look rule. Floor and ceiling clicks remain rejected by this wall item. The ceiling beam's existing placement is unchanged.

The upright profile is unchanged. The horizontal wall profile is its rigid in-wall rotation: the same 6-by-5-pixel section, 16-pixel length, four cuboids, textures and vertex UVs. The back flange contacts the clicked wall plane in all four facings.

Like the original ceiling support, neighbours cause perpendicular half-cross arms within the current block. Only wall Double T blocks facing the same wall plane qualify. Neighbours on another wall plane, original ceiling beams and wood beams do not qualify. Both placement orders and neighbour replacement/removal are covered. The arms are rigid transforms of the existing steel cross model; no new corner piece or diagonal is introduced.

## Saved state and existing contracts

New boolean properties `wall_horizontal`, `join_negative`, `join_positive` default to false. Older named state palettes load upright, with their old facing/fluid fields; nearby joins are refreshed on normal neighbour updates. No world rewriting or forced chunk loading is added. For upright beams, positive is facing-clockwise; for horizontal wall beams, positive is down. Structure mirrors swap the arms when their local axis reverses.

Selection and collision follow the same transformed core/arms. Native water, lava and transient flowing-water fields are retained. The existing self-item loot and tool rules are unchanged; connectors do not have independent items. Breaking a wall is not given a new destruction/drop rule.

## Qualification

The new native item-placement regression fails on the previous main: a side-edge click yields horizontal length6/16 instead of16/16. After correction,672 required GameTests pass locally; after the resource correction135 JUnit tests and the build pass. Tests exercise four wall faces, two hands, click zones/corners/ties, both placement orders, incompatible planes, neighbour removal, rotated UVs/models, collision/contact, native server ray/mining, one self drop, fluid retention, legacy properties and serialized state. A first join fixture used raw setBlock rather than item placement; it was corrected to exercise the actual item transaction without relaxing the join assertion.

The first native bake check exposed a pole-specific helper stretching connector cuboids; the generator now performs a rigid transform without clipping. A regression checks each connector cuboid and texture coordinate.

`python3 tools/models/generate_vertical_double_t.py --check` verifies the generated resources. All32 geometric property combinations resolve exactly one core plus the requested existing arms; the fluid properties do not affect model selection. Registered Cartesian state count is3072 (previously384), not a performance claim.

Native screenshots/restart and exact-commit CI results are reported with the PR. A server-side NBT round trip is distinguished from an actual process restart. No modpack, alternative renderer or four-light qualification is inferred from these tests.
