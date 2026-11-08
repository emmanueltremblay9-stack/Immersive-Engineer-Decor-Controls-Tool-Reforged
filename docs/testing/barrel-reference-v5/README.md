## Main reconciliation, 8 November 2026

Merged main `fdf26aadc45580b7ca4211a32323125f3c276d5b` into the existing V5 branch without rewriting history. The only text conflict was MachineMenu construction and slot projection. Barrel and Factory Placer now have separate, mutually exclusive presentation inputs. Factory Placer retains its ordinary client route; Barrel retains its existing default-OFF experiment pending full delivery qualification. Public server constructors disable both, and the client factory also rejects presentation on server worlds.

The new server GameTest covers both machine kinds, both construction paths and all four combinations of historical properties. Existing V5 and packaged/default Placer CI provide separate client coverage. New exact-commit CI is required after this reconciliation. The unchanged downstream menu/data/transfer body is pinned against main, including its existing Factory Placer accessor. Original V5 artwork and all merged main artwork remain byte-identical.

Local 287 Python tests, geometry/resource/source guards and Java syntax parsing passed before the added server test. Local mapped compilation is blocked before compilation by the uncached Gradle distribution and unavailable network access to services.gradle.org. No new native pass, normal V5 path, resource reload, final visual acceptance, merge or release is claimed here.

Historical qualification and design records follow.

# Barrel V5 opt-in presentation candidate

Production base: PR151 commit `8718a0896bc7330fb4dde5e272a4fdf078db0804`.
Intended stack base: PR165 `a736f97adc220137fe5c8ff7fb8031e865786762` (same production files).
Pattern reference: PR164 `2b601b2aae77cd6ae716d027d476be4d0d9806fe`.
No PR165-owned files changed. Integrated as a separate draft stacked on PR165; no merge or release.
The three recovered baseline files had an extra trailing blank line; integration normalizes only that whitespace and updates the source guard hash.

## Implementation

Enable only on the client with `-Diedctr.dev.barrelV5Reference=true`. The default is OFF.
`MachineMenu.client` captures the option at construction. The public server constructor
always passes false, even in an integrated server sharing the same JVM. The registration
chooses `BarrelReferenceScreen` only when its menu has the matching presentation; otherwise
it selects the unchanged `MachineScreen.FluidBarrelScreen`.

The source PNG was visually inspected and copied byte for byte. SHA-256:
`df89575afd98ccee396b38cee34f230ef03f94528d5a4ad5bc3425658ef54ca9`.
No redraw, patch, crop, baked fluid, label or frame modification is applied.

The 1137x1383 image maps exactly 1:3 to a 379x461 logical canvas. A single uniform
viewport transform fits that canvas with up to six pixels of margin and no stretching.
Vanilla 16x16 item stacks, counts, durability and carried stacks render within that same
pose; all pointer event positions and drag deltas use its inverse. Wheel values remain
unscaled. Item and tank tooltips render after restoring the normal screen pose.

The measured conservative interiors and all 38 mappings are in geometry-manifest.json.
Machine indices remain 0=input and 1=output. Menu indices 2..28 retain player inventory
indices 9..35; 29..37 retain hotbar indices 0..8. Slot roles, output-only behavior,
quick-move, synchronized fields and capacity are unchanged. The hitbox retains vanilla's
16x16 target with its usual padding clipped to the measured interior, rather than making
the entire large decorative aperture clickable.

A conservative inner rectangle at source [510,633) x [345,579) is wholly within the
central dark aperture, clear of chamfered corners and metal framing. This deliberately
leaves a small dark rim. Fill height is bottom-up and floored from the actual synchronized
amount/capacity, safely clamped; less than one logical pixel remains visually empty but
still reports the true amount in its tooltip. It calls the existing PR151
FluidBarrelSpriteRenderer, resolving still sprites and tint from the current native atlas.
Missing sprites leave the retained empty aperture visible. No new fluid mechanics,
recipes, server protocol or tank capacity are introduced.

## Executed checks

- Java 21 dependency-free checks pass: 38 unique slots, all full item rectangles within
  measured interiors, nonoverlapping hover targets, boundaries, nine viewport shapes,
  inverse pointer/delta transforms, negative pointer flooring, clamped/overflow-safe and
  monotonic fluid fill arithmetic.
- Exact PNG SHA-256 passes.
- Source routing guards pass, including unchanged downstream menu data/transfer code.
- Java syntax parsing passes for all six candidate Java sources. This is NOT NeoForge
  type checking, compilation or Minecraft runtime proof.

Commands from this candidate root:

    bash tools/validation/check_barrel_reference.sh
    python tools/validation/check_barrel_reference_wiring.py
    java tools/validation/ParseBarrelSources.java

## Native verification gate (not yet run)

First integrate on the desired exact baseline and run its complete Gradle compile/test,
GameTests and required CI. Resolve any overlap with PR164 menu construction deliberately;
do not replace its Freezer projection when applying these PR151-based changes. This
candidate is not a full repository checkout and does not include Minecraft dependencies.

The new dedicated NativeBarrelReferenceValidation fixture and barrel_reference_native.py
runner are implemented but NOT executed in Minecraft here. They do not enable or edit the
existing PR165 corrected-screen fixture, whose green result does not prove this presentation. Record commit, built JAR
SHA-256, client/server option values, resource pack, locale and GUI scale in every proof.

1. Default-OFF regression: open a real Barrel, assert legacy screen class and slot count,
   verify legacy art, transfers and capacity. With the option set on a dedicated server
   only, assert authoritative slot coordinates and index ordering are unchanged.
2. Option-ON: open the real synchronized Barrel menu, assert BarrelReferenceScreen and
   38 matching indices; inspect empty PNG rendering and blank plaque. Capture native
   screenshots at GUI scales 1/2/3 and window sizes 1280x720, 1920x1080 and narrow/portrait.
3. For water, lava and milk (actual registered milk identity), test empty/partial/full,
   1mB, resource reload and pack still-sprite/tint overrides. Compare screenshots outside
   the inner tank rectangle to the empty state at identical transform. Ensure zero frame
   pixels change and scissor is restored for subsequent slots/tooltips. Do not accept a
   flat substitute colour as fluid proof.
4. Put distinctive stacks in all 38 slots. Inspect art/item alignment, counts/durability,
   cursor and hovered highlight. Click each centre and all four just-outside boundaries
   with fractional transforms; assert matching slot indices, no neighboring hit.
5. Exercise pickup/place, right-click split, double-click collection, shift-click in both
   directions, hotbar number keys, drag splitting, output insertion rejection, carried
   stack outside clicks, Escape/close/reopen and resizing while carrying a stack. Assert
   server state and no duplication/loss. Test full inventory and blocked output.
6. Check English/French item, input/output and tank tooltips at each edge of the viewport;
   ensure they use normal screen size and stay in bounds. Verify a second machine's menu
   is unaffected. Keep the option OFF by default even after these candidate checks.

## Review/application

`barrel-v5-source.patch` contains only text source/resource delta versus PR151 (plus new
source check); copy the exact binary background.png separately to its matching resource
path. `src/` contains the complete candidate replacement/new files. The three changed
existing files are MachineMenu.java, ClientModEvents.java and FluidBarrelSpriteRenderer.java; merge their narrow delta,
not wholesale replacement if the integration baseline contains PR164. The renderer retains its legacy six-coordinate signature and adds explicit screen-space
scissor bounds for transformed callers. MachineScreen.java is unchanged.
The `baseline/` and `reference/` directories are read-only provenance inputs and must not
be copied into the target source tree. Validation scripts/docs are delivered separately.


## Dedicated native fixture and exact-head workflow

The new workflow `.github/workflows/barrel-reference-validation.yml` checks out the exact
PR head, runs geometry/resource guards, complete Gradle compile/test/GameTests/JAR tasks,
then runs fresh native EN 1280x720 and FR 1280x260 clients. It records source SHA and built
JAR hashes. `runClient` uses development classes, so the JAR hash is build provenance,
not a claim that the jar was the client classpath. No CI execution has been performed yet.

NativeBarrelReferenceValidation is an independent client event subscriber enabled only by
`iedctr.barrelReferenceNative`, with `iedctr.clientGameplay=false`. It refuses existing saves,
loaded worlds, stale capture folders, a mismatched nonce/directory, remote connections and
missing commit identity. It creates a fresh flat integrated world and real Barrel entity.
It verifies the authoritative server menu never uses the client projection.

Implemented bounded checks (unrun in Minecraft):
- All 38 menu indices, container indices, item anchors, centres and four hover boundaries.
- Each real screen coordinate picks up one fixture stack through vanilla click/release
  packets and returns it to an empty player slot, with server-side identity/count checks.
  Input uses an empty bucket with an empty tank; output uses server-seeded stone solely
  to exercise extraction routing, not to claim a valid machine-produced item recipe.
- Empty plus water/lava/registered NeoForge milk at half/full capacity: real server fluid
  handler, synchronized amount and fluid identity, seven native framebuffer screenshots,
  seven tank PNG crops, hashes, screen class, menu ID, actual capacity and fit scale.
- A fixed native-pixel ring around the glass is hashed outside the transformed tank plus
  a two-screen-pixel boundary margin. Its seven hashes must stay equal. Each fluid must
  have distinct empty/half/full tank crops, and the three full-fluid crops must differ.
  This is not a full-frame artwork identity or artistic acceptance test.
- Close acknowledged by server; reopen retains fluid identity, amount and slot geometry.

The screen uses explicit GUI-screen scissor coordinates for fluid drawing; Minecraft
1.21.1 scissor rectangles do not inherit the pose transform. The old renderer call retains
its legacy behavior. The 720-high case tests translated clipping; the 260-high case tests
translated fractional clipping. A pass needs actual distinct native tank captures, never
just successful screen construction or nonempty screenshot files.

Executed locally: 14 synthetic Python evidence-validator tests pass, covering both
requested viewport/locale combinations and rejection of missing/duplicate slots/cases,
stale identity, wrong transform, invalid PNG hashes/dimensions, symlinks, changed frame
rings, duplicate fluid crops, crashes and failed completion. These tests use generated
PNG fixtures and are NOT screenshots or a native runtime pass. Java syntax parsing passes.
The JUnit wrapper also checks the actual resource PNG's dimensions and hash, but JUnit,
full NeoForge type checking and native clients remain unrun in this partial checkout.

Remaining native coverage includes physical OS pointer input, other GUI scales, arbitrary
resizing while carrying, drag split, shift-click, double-click, right-click, hotbar number
keys, output insertion rejection, full-inventory handling, resource reload/pack override,
tooltip screenshots, and default-OFF legacy regression. Existing general tests may cover
some mechanics but cannot establish the V5 presentation for these interactions.

## GUI-scale evidence extension

The native workflow now preserves EN 1280x720 scale 1 and FR 1280x260 scale 1,
then adds independent EN 1920x1080 jobs at explicit GUI scales 1, 2 and 3.
Each job executes the same bounded 38-slot pickup/return routes and seven fluid
captures; it does not add other gestures or broaden visual acceptance claims.
These new profiles require a new native run on the exact updated commit. Earlier
scale-1 runtime evidence cannot establish scale-2/3 success.

The runner accepts `--width`, `--height` and `--gui-scale` (1/2/3). It rejects sizes
that Minecraft cannot run at the requested scale instead of accepting a fallback.
Both the overall proof and every capture record requested/actual GUI scale, scaled
GUI dimensions and framebuffer dimensions. The validator rejects missing or changed
values, including scale-1 fallback. Viewport fit uses scaled GUI dimensions and
matches the Java float/nextDown rounding. Capture bounds round in GUI coordinates
before multiplying by the actual window GUI scale, matching the renderer's scissor;
they do not use a framebuffer-width/scaled-width ratio. The tank's framebuffer
position, dimensions, PNG size and hash are all checked. At 1920x1080 the expected
full-aperture crops are 40x77, 80x154 and 90x174 native pixels for scales 1/2/3.
The surrounding ring uses framebuffer coordinates with a two-native-pixel exclusion
band and is compared only within one profile, not between different GUI scales.

Dependency-free Java checks and synthetic Python fixtures cover all five profiles,
plus 1921x1081 scale 3 to exercise ceil-rounded GUI dimensions. Synthetic PNGs are
not Minecraft screenshots. Production rendering, artwork and slot handling remain
unchanged by this validation-only extension. Physical OS pointer input, the other
gestures and the other native checklist items above remain separate acceptance work.
