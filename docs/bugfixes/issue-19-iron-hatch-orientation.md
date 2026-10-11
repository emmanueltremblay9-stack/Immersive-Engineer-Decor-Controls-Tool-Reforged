# Issue #19: Iron Hatch placement orientation

## Cause and scope

The hatch model and collision use `facing` as the physical hinge edge. Vanilla
trapdoors use the opposite convention. Reusing the generic opposite-of-player
placement direction therefore opens a ground-placed hatch toward the opposite
edge from an oak trapdoor placed in the same situation.

Change only `HatchBlock.getStateForPlacement`: floor/ceiling and replaceable-block
placement use the player's horizontal direction as the hinge edge; placement
against a solid horizontal face uses the opposite of that clicked face, matching
vanilla's hinge selection even when the player looks obliquely at the wall.
Power still initializes both `powered` and `open`.

The existing state meanings, model/blockstate assets, shapes, registry IDs,
manual interaction and redstone behavior are preserved. Already placed hatches
are not rotated or migrated. The hatch remains its existing lower-only design;
this does not introduce vanilla's top-half or waterlogging properties.

## Regression coverage

Four required `HatchPlacementGameTests` exercise actual `BlockItem.place` on
floors, ceilings and all four wall faces, for each player direction, both with
and without existing power (48 cases), plus 16 horizontal clicks replacing grass. Vanilla `OAK_TRAPDOOR.getStateForPlacement`
is the independent reference, rather than duplicating the fixed direction rule.
The tests check placed identity, initial power/open state, hinge direction,
manual opening when unpowered, and the opened outline/collision edge against
vanilla. Different leaf thicknesses are intentionally allowed.

Existing accessway tests continue to cover all hatch state shapes and powered
manual interaction. The new tests are committed before the implementation fix
to establish the regression on the old placement code.

## Qualification limits

Dedicated GameTests validate server placement and collision behavior. No client
visual inspection, reporter modpack reproduction, packaged dedicated-server
qualification or release is claimed. The unchanged build workflow does not
upload runtime artifacts. Licensing and release metadata are unchanged.
