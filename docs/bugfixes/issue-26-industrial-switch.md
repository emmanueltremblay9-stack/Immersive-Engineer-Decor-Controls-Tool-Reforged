# Issue #26: Industrial Switch cabinet placement and collision

The shipped model is a two-block-tall cabinet, with its controls facing north in
model space. Previously it inherited a small wall switch's clicked-face placement,
outline and empty collision. Its horizontal model rotations also faced inward.

New item placement reserves two blocks, faces the player on floors and outward
from a clicked wall, and gives each half a matching cabinet-envelope outline and
solid collision. Either half toggles the linked 0/15 output. Lower-authoritative
state synchronization preserves facing and power. Each cabinet drops one item
when either half is broken in survival; creative removal drops none. Pistons
cannot move this cabinet independently. Placement under a ceiling or other
occupied headroom fails without replacing the obstruction.

The original model meshes and textures are unchanged. The lower renders the
original tall mesh; the upper has an empty model, its own interaction and collision,
and the original particle texture. Bounding shapes enclose the cabinet, rather
than tracing its tiny decorative gaps. Other controls retain their old behavior.

## Existing saves

A new `part` property defaults to `legacy`, preserving old six-direction facing
and powered values and all original model rotations. Legacy entries are never
converted automatically, removed for missing a partner or backing support, or
allowed to overwrite an occupied block above. They receive the enclosing outline
and collision; upper-half raycast behavior still requires a real upper block.
Break and re-place an old cabinet in clear two-block space to opt into the paired
layout and corrected outward orientation. No world scan or destructive migration
is performed. The item ID remains unchanged.

## Verification

Dedicated GameTests cover actual floor/wall item placement, all four horizontal
orientations, lower and upper outline/collision, upper raycast selection, toggling
and redstone, obstructed headroom, pair cleanup and loot ownership, saved-state
round trips, old saves without `part`, and rotation synchronization. The existing
23-control toggle regression continues to include legacy Industrial Switches.

CI runs compile/resources, tests, dedicated GameTests and build. This does not
claim client visual inspection, the reporter's full modpack, or packaged server
qualification. Publication workflows, models, textures and release metadata are
unchanged; no runtime artifacts are uploaded.
