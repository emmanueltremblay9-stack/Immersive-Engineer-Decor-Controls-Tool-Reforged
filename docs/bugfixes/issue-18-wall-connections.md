# Issue #18: decorative wall connections

## Cause and repair

Clinker Brick Wall, Slag Brick Wall, Rebar Concrete Wall and Steel Mesh Fence
are registered as vanilla `WallBlock` instances. Their IDs were missing from
`minecraft:walls`, the tag vanilla wall connection logic checks for adjacent
walls. Add all four IDs with `replace: false`, preserving vanilla and other
mods' tag entries. Steel Mesh Fence remains a wall-based block; this does not
convert it to a wooden fence or change its models, collision, IDs or recipes.

## Regression coverage

Five required GameTests cover loaded tag membership, same-type adjacency,
all ordered mixed-type pairs, both placement orders with a vanilla cobblestone
wall, and existing solid-block behavior. Placement and neighbor-update paths
are checked in all four horizontal directions. Both sides must connect, and
removing a neighbor must clear the connection. These tests exercise newly
placed blocks and neighbor updates; existing isolated saved states may need a
neighbor update to refresh their connections.

## Verification boundary

CI runs the dedicated GameTest server and normal build checks. Client visual
inspection, a packaged dedicated server and release JAR qualification are
separate checks and are not claimed by this fix. No runtime artifact is
published by the build workflow. Licensing and release metadata are unchanged.
