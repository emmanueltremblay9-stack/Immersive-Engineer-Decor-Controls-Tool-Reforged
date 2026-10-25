# Stair-integrated railings and existing railings above stairs

## Scope and save compatibility

Holding Steel Railing and using an ordinary right click on either lateral half of Steel Catwalk Stairs edits that side inside the existing stair block. Survival installation consumes one railing; removing a paid side refunds one. Mining a side uses the normal server game-mode START/completion path, hardness, tool wear and protection events. Mining the step removes the stair and returns its remaining purchased railings through the ordinary loot path.

The existing `left_railing`, `right_railing`, `facing` and `waterlogged` properties remain unchanged. The existing railing model profiles are aligned to landing caps as described in [the junction correction](../fixes/catwalk-stair-landing-junction.md); treads and structural connectors stay unchanged. New `edited_stair_sides` bits distinguish a deliberately installed/removed side from an untouched legacy automatic neighbor connection. Missing properties in older saves default to no edits/no paid credits, so legacy geometry does not mint items. `paid_railings` is a fungible purchased-material balance, capped at the number of visible sides, just as for flat catwalks.

Both tread collision tops match the unchanged tread geometry: 4/16 and 12/16. Selection follows a narrow, stepped approximation of the existing sloped railing side envelope. Side collision uses one cap at Y=36/16 along each complete installed side, 24/16 above the highest tread, with the center passage left clear. The visible outline remains sloped. A lower collision cap following each tread left the downhill half only one block above a player taking off from the upper tread, allowing a diagonal jump over it.

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

## Upper-overhang targeting

The stair railing model extends its upper side rail above the owning stair cell. A focused shared helper traverses the ordinary eye ray and examines only the stair directly below each visited cell. It selects only an existing railing shape intersected above its owner, only within block reach, and only before the ordinary block/entity hit. Exact ties retain the ordinary block. No chunk is force-loaded, and `Level.clip`, model resources, collision, and server mining packets are not globally replaced.

NeoForge 21.1.230 has no supported post-pick event. A single client-only RETURN Mixin in `GameRenderer.pick(Entity, double, double, float)` therefore preserves vanilla's existing result unless this narrow extension finds a nearer visible upper rail. NeoForge already supplies Mixin; no new external dependency is added. `minecraft.hitResult` and `crosshairPickEntity` are then assigned by vanilla itself, so highlight, press/hold/release mining and entity attacks use the same result. Dedicated servers load no client Mixin class. Native START/STOP progress, cancellation, permissions, tool use and drops remain in the existing mining path; the shared selector revalidates nearest world/entity occlusion at completion.

`UpperStairRailingTargetingGameTests` exercises purely horizontal rays above the owner in all four rotations/both sides, the Y=1 boundary, inside-to-outside rays, nearer solid/standalone/competing stair targets, farther blocks/entities, exact solid overlap ties, nearer pickable entities, an entity-clamped MISS, actual reach, empty air above the model, native-duration mining, cancellation and a wall appearing after START. These are executable regression tests, not evidence of a client session or of runtime Mixin injection.

Additional client acceptance checks:

1. Verify startup applies the Mixin without missing-target errors, then stand level with the visible high rail and aim horizontally from both sides in all four rotations. The outline must select only the side and the tooltip must identify the owning stair
2. Hold attack until completion, release/retarget during progress, and repeat in creative. Exactly the selected rail disappears; the stair/opposite side remain. There must be no repeated start reset while holding a steady target
3. Place a wall/standalone railing/another upper stair closer to the camera and an entity between camera and rail. The nearer target wins; the stair cannot be mined through it. Place the same targets behind the upper rail and confirm the visible rail wins
4. Check just inside/outside reach, the transition across Y=1, aiming above the rendered top, and first/third-person cameras. No invisible collision-height target or extended reach is allowed
5. Repeat cancellation/protected-area checks and verify ordinary blocks, entities and existing standalone rails keep their usual targeting and mining. High overhang targeting does not relax native right-click packet validation; in-place side editing remains available by clicking the stair's lateral half
6. Run the dedicated-server suite to verify client-only Mixin isolation. Review compatibility with any mod that also transforms `GameRenderer.pick` before calling client QA complete

## Elevated movement regression

`ElevatedStairRailingMovementGameTests` adds a dedicated 11³ fixture containing a suspended stair, its bottom-mounted catwalk landings, and air outside the side barriers. It drives real server `Entity.move` with ordinary step resolution and a deterministic vanilla-strength jump trajectory from both tread heights, in all four orientations and on both sides. The high-to-low diagonal route crosses the tread seam while airborne; removing only the attempted side must allow the same route to exit. Every movement checks body containment, nonintersection, ground contact after landing, and unchanged block/payment state. These tests exercise collision, not the client's auto-jump input algorithm. Runtime pass/fail evidence must come from CI on the candidate commit.
