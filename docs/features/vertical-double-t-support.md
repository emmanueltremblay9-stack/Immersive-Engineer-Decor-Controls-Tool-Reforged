# Vertical Steel Double T Support

`vertical_steel_double_t_support` is a separate creative/prototype block. Its
recipe is deliberately undecided. It does not replace or convert the existing
`steel_double_t_support`.

The four cuboids and existing texture references are rigidly rotated from the
horizontal core by `(x, y, z) -> (x, 16-z, y)`. The length remains 16 model units;
the cross section remains 6 by 5. UV coordinates follow the same physical
corners. The default north-facing model occupies `[5,0,11]..[11,16,16]` and its
back flange touches the south wall at the cell boundary. The other three
facings are quarter turns of that geometry.

Use the ordinary block item on a horizontal wall face. The clicked face, rather
than the player's look direction, determines the facing. Floor and ceiling
clicks are rejected without charging an item. The target is the ordinary
adjacent placement cell. Outline and collision follow the four rotated volumes.
Like the existing horizontal support, it is not destroyed when its backing block
is removed; this feature introduces no new support-loss rule. It adds no beam
connectors, pole fittings or lamp attachment capability.

Fluidlogging uses the existing native source/current storage, neighbor scheduling,
rotation and saving mechanisms. The new block has 384 enumerated states
(4 facings × 2 waterlogged values × 3 fluid-cell values × 16 flow values), with no
BlockEntity. Horizontal geometry, textures, behavior and registry ID are unchanged.

## Validation

`tools/models/generate_vertical_double_t.py` regenerates the four new resource
files without editing the original model. `VerticalDoubleTModelTest` independently
checks the original model identity, rigid coordinates and per-corner UV mapping.
`VerticalDoubleTGameTests` exercises real item placement on all four walls and both
hands, rejected placements, exact collision/outline rotation, source/current
storage and NBT state round trips, structure rotations, neighbor changes and
native survival removal with a single drop. The catalogue-wide fluid placement
test uses a horizontal wall face for this wall-mounted block instead of skipping
its coverage.

`NativeVerticalDoubleTValidation` is an opt-in client diagnostic, disabled by
default. It requires both a matching nonce marker and the exact owned test-world
path. It uses native server game-mode placement selected by an actual eye ray,
compares baked vertices/textures/UVs against the rotated original model, captures
the four wall directions, and can inspect a saved block after process restart.
Neither an automated result file nor a successful client exit substitutes for
human screenshot inspection. Exact artifact hashes, execution outcomes and visual
limits belong in the accompanying qualification report.

This branch is based on main independently of the raised-stair V prototype and
Iron Bulb attachment branches. Their combined composition requires separate
requalification. Survival crafting, the user's modpack, and new lamp or connector
semantics are not qualified by this feature.
