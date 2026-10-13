# Stair-integrated railings and existing railings above stairs

## Scope and save compatibility

Holding Steel Railing and using an ordinary right click on either lateral half of Steel Catwalk Stairs edits that side inside the existing stair block. Survival installation consumes one railing; removing a paid side refunds one. Mining a side uses the normal server game-mode START/completion path, hardness, tool wear and protection events. Mining the step removes the stair and returns its remaining purchased railings through the ordinary loot path.

The existing `left_railing`, `right_railing`, `facing` and `waterlogged` properties and model files remain unchanged. New `edited_stair_sides` bits distinguish a deliberately installed/removed side from an untouched legacy automatic neighbor connection. Missing properties in older saves default to no edits/no paid credits, so legacy geometry does not mint items. `paid_railings` is a fungible purchased-material balance, capped at the number of visible sides, just as for flat catwalks.

Both tread collision tops now match the unchanged stair model: 4/16 and 12/16. Selection follows a narrow, stepped approximation of the existing sloped railing side envelope. Side collision reaches 24/16 above each tread, with the center passage left clear.

## Automated coverage

`StairRailingGameTests` covers native in-place installation/removal, payment, all four rotations and both sides, selective mining versus step mining, START/STOP duration and tool durability, drops, waterlogging, NBT/codec reload, legacy neighbor connections, event cancellation, adventure CAN_BREAK, spectator/creative restrictions, wrong tools, disabled tile drops, stale targets, sloped selection, fence-height collision and open center headroom.

`StairSupportedRailingGameTests` separately exercises the pre-existing standalone Steel Railing configuration visible in the reported screenshot's tooltip: above a stair or next to a stair, each side/rotation, downward and oblique real-eye targeting, the center opening exposing the supporting stair, and native-duration mining. These tests do not equate a separate railing block with the newly integrated stair-side state.

Run the full dedicated GameTest suite along with compilation and ordinary tests. CI is required because the editing environment has no usable Gradle distribution/dependency cache. No interactive game validation is claimed by these automated tests.

## Interactive acceptance checks still required

1. In survival and creative, hold Steel Railing, click the left/right portion of stairs in each orientation, and confirm no additional block appears above or beside the stair
2. Confirm the outline selects only the aimed side, mining that side preserves the stair and opposite railing, and aiming at the center step mines the stair normally
3. Retarget during mining, cancel mining, and repeat with a protected-area plugin enabled; no stale completion should remove another side or the stair
4. Walk and attempt auto-jump/jump off both low and high treads; the side barrier must block ordinary escape while the center remains traversable
5. Repeat for old standalone railing blocks already placed above or next to stairs, comparing their actual tooltip and outline
6. Save/reload and apply neighbor changes, confirming deliberate edits persist without material duplication

## Native picking limitation

The unchanged stair model extends its upper side rail beyond the stair block cell. Vanilla's block-cell traversal can discover that overhang on downward/oblique rays that enter the owning stair cell. A purely horizontal ray confined to the cell above cannot discover the lower owner. This patch does not install a global picking override or move the model; that limitation remains and must not be presented as validated upper-overhang targeting from every camera angle.
