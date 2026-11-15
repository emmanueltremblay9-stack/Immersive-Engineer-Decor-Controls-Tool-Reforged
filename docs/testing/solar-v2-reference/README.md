# Solar V2 ordinary-JAR default candidate

Current candidate changes the normal client presentation to retained Solar V2 when
`iedctr.dev.solarV2Reference` is absent or true. Explicit false is retained solely
as a legacy diagnostic fallback. Server slot coordinates, mechanics, FE behavior,
art bytes and other machines are unchanged. This is not a merge or release.

The native fixture has moved out of main sources into the separate
`iedctr_solar_validation` test mod. The five native profiles now launch the ordinary
production JAR, with no presentation selector and no main source output on the runtime
classpath. Gradle rejects duplicate archive entries, fixture leakage, split packages,
wrong development-mod registration and a supplied artifact differing from the normal
JAR. Runtime proof verifies the actual loaded JAR/hash, authoritative class origins,
class bytes, separate harness module and active retained artwork pack/hash at boot and
completion. Suite v3 retains the strict physical-input and pixel checks from v2.
Each profile archives the actual tested JAR and its identity report for independent
rehashing, rather than only retaining a reported digest.

The cumulative corrected-machine GUI checks explicitly force only Solar and Barrel
legacy fallbacks and label them diagnostic-only. They do not qualify the default
Solar path. Normal-path qualification requires this candidate's fresh exact-head CI,
including all five profiles. Historical successful opt-in/source-runtime results below
do not substitute for that requirement. Opening/closing are still programmatic;
complex gestures, JEI targeting, resource reload and full visual acceptance are not
claimed. Other branches and their qualification history are untouched.

The sections below preserve the historical opt-in development record. Statements
about default OFF or source-runtime scope describe those earlier revisions, not the
current candidate.

## Branch reconciliation, 2026-11-10

Reconciled Solar head `898a0307d755b28af671036a3f2786a14343b1a9` with its current Barrel base `15593b28a6853ba36775ebb419b8465a5e2f3240`, without rewriting history. MachineMenu uses independent Barrel, Factory Placer, and Solar requests: Barrel and Placer retain their base defaults, Solar stays opt-in, and server-side construction rejects all three presentations. Original Solar artwork and downstream menu mechanics are unchanged.

The preservation manifest now names the exact integrated Barrel parent; each listed file was checked byte-for-byte against that parent before updating its digest. Geometry, slot protocol and pixel acceptance criteria are unchanged. The production-expression routing matrix now covers all three flags across client/server and absent/false/true properties.

Local validation: all 375 Python validation tests pass, including the executable routing matrix; Solar and Barrel geometry/art checks, Solar wiring guard and whitespace checks pass. An initial routing-test parser failure was corrected by removing exactly one constructor-closing parenthesis rather than all trailing parentheses. Local Gradle compilation did not start: distribution 9.2.1 was unavailable and the download failed with network unreachable. Exact new-head CI and native evidence remain required; historical results below are not new-head qualification. No PR merge or release.

# Small Solar Panel V2 opt-in presentation

Stack base: PR166 remote commit `9ddbd4af5b06d4ed915c42c758d9f3840844b80e`, tree
`9bcb0cb64c53937ada0b8cad1e2a8bad704e575a`. This candidate adds a separate default-OFF
presentation. It neither replaces the production Solar screen nor changes the retained
Barrel experiment. Publication, full visual acceptance, merge and release are separate.

## Source and geometry

The retained source was inspected visually and copied byte-for-byte from the
[immutable Small Solar Panel V2 artwork](https://github.com/emmanueltremblay9-stack/Master-Generalist-GUI-Architect-Designer-DEV/blob/0c166b34fb19cdf4a54373ff5e9c4770a854be12/candidates/iedctr/retained-concepts/2026-11-04/small-solar-panel-v2.png).
It is 1254 by 1254 pixels, SHA-256
`d702051b09e09cd7c9ebd7ea874356ba82324db07bcb0657cf8ac66bdf54c796`.
Only that PNG, canonical public provenance and measured coordinates are included. The
partial authoring kit is not a runtime dependency and is not included in this candidate.

The 418 by 418 logical canvas maps the source exactly 1:3. One uniform transform fits
the complete canvas within up to six GUI pixels of margin without stretching or
upscaling. All 37 vanilla 16 by 16 item origins are centered inside conservative source
interiors documented in `geometry-manifest.json`. Index 0 is the existing Cell;
1 through 27 remain player inventory slots 9 through 35; 28 through 36 remain hotbar
slots 0 through 8. Decorative apertures do not enlarge vanilla slot targets. Hover
padding is clipped to measured interiors; complete items remain inside those interiors.

Click, release, drag, movement and scroll positions use the same inverse transform.
Drag pixel deltas are also inverted; wheel values are not. Fractional hover positions
are retained even though Minecraft's render method accepts integer mouse coordinates.
Tooltips render after restoring the normal screen pose. The retained blank plaque is
left blank and no label backgrounds or masking panels are added. The existing localized
FE tooltip and Cell role are retained rather than inventing new machine states.

## Live FE and preserved behavior

Enable only on the client with `-Diedctr.dev.solarV2Reference=true`.
`MachineMenu.client` captures this independent property when constructing the client
projection. The public server constructor explicitly disables it, including when the
integrated server shares that JVM. Default clients continue using the unmodified
`MachineScreen.SmallSolarPanelScreen`. Barrel's independent flag and projection are
unchanged.

The native source meter's measured safe interior is `[146,588,997,625)` in source
coordinates. The dynamic overlay rounds inward to `[49,196,332,208)` on the logical
canvas, leaving the frame untouched. Fill is horizontal, floored and clamped from
actual synchronized energy/capacity. Only the existing legacy FE orange and highlight
colors are reused. There is no dark-backing redraw, recoloring, baked example charge,
new illustrated fill, sunlight state, panel motion or fabricated machine data. At zero
FE nothing is drawn over the source. Less than one logical pixel can remain visually
empty; the tooltip still reports the synchronized FE value.

Screen-space scissor bounds are transformed and rounded inward before Minecraft's
window GUI scale is applied. Scissor state is restored in a finally block. Real slot
rules, chargeable items, transfers, server slot order, 64,000 FE capacity, comparator
behavior and 100 FE display granularity are unchanged. `preserved-baseline-sha256.json`
freezes the existing production screen, layout, entity, energy sync, old artwork and
all retained Barrel files. The source guard also hashes the entire downstream menu
implementation, including synchronization, transfer and validation logic.

## Local checks and limits

Executed successfully during candidate preparation:

- Java 21 dependency-free checks: all 37 unique indices, item containment, disjoint
  hover targets, boundaries, ten viewport shapes, inverse absolute/delta transforms,
  negative-pointer flooring, invalid input rejection, empty/half/full/subpixel FE,
  clamped overflow-safe arithmetic and monotonic fill.
- Exact PNG SHA-256, dimensions, manifest mappings, source/input routing, legacy
  fallback, client-only flags, unchanged downstream menu and preserved files.
- Existing Barrel dependency-free geometry and source guards, unchanged.
- Java syntax parsing for all 318 sources. Parsing alone is not mapped type checking.
- All 166 Python validation unit tests, including 25 independent Solar native-evidence
  validator tests, pass. These synthetic tests are not native Minecraft execution.
- Workflow YAML parses with all five native profiles, Python compilation and whitespace
  checks pass.

Commands from repository root:

    bash tools/validation/check_solar_v2_reference.sh
    python tools/validation/check_solar_v2_reference_wiring.py
    bash tools/validation/check_barrel_reference.sh
    python tools/validation/check_barrel_reference_wiring.py
    java tools/validation/ParseBarrelSources.java
    python -m unittest discover -s tools/validation -p 'test_solar_v2_reference_native.py' -v

Full mapped compilation and native execution were not available in the preparation
workspace: the complete Gradle distribution/dependency cache is absent, wrapper network
download is unavailable and Xvfb is absent. No native screenshots or passing Minecraft
result are claimed from that workspace. The new workflow must pass on the exact
published candidate commit before its runtime evidence can be accepted.

## Isolated native gate

`.github/workflows/solar-v2-reference-validation.yml` checks out the exact PR head (not
a synthetic merge commit), runs source checks, mapped compile, JUnit, existing GameTests
and JAR build, then runs an independent nonce-owned native fixture at these profiles:

- English, 1280 by 720, GUI scale 1.
- French, 1280 by 260, GUI scale 1.
- English, 1920 by 1080, GUI scales 1, 2 and 3.

The runner refuses a dirty candidate, stale captures, existing saves, mismatched source
SHA/art/locale, fallback GUI scale, incorrect framebuffer dimensions or geometry,
missing routes, crashes and timeouts. It rechecks clean source identity after execution.
It retains JSON proof, PNGs, crop hashes and logs with the tested source identity;
the compile job separately records the built JAR SHA-256.

The native fixture opens a real synchronized Solar menu in a new integrated world. It
uses the existing `immersiveengineering:capacitor_lv` item and its actual NeoForge
energy capability, places it via XTest physical click/release events into the Cell, and
observes real server FE movement with FE/item conservation. An opaque roof prevents
solar generation during that controlled test. All 37 slots are picked up and placed
through the physical GLFW input path and vanilla packets with server observation. FE
snapshots at zero, half and full are controlled fixture states, not claims about new
production mechanics. It captures the meter and checks surrounding canvas pixels,
closes the screen, verifies server close, then reopens and checks state/geometry.

The Python validator independently decodes PNG pixels, matches the cropped meter to
the full screenshot, verifies the expected horizontal FE colors and verifies that the
retained canvas outside the allowed FE/item regions is unchanged across charge states.
Synthetic PNG fixtures test the validator; they are explicitly not native proof.

## Remaining visual/interaction acceptance

Even green bounded native checks do not claim physical mouse input, resource reload,
all tooltip edge positions, drag splitting, shift/right/double-click combinations,
hotbar keys, resizing while carrying items, full-inventory failures, or final visual
acceptance. Those remain separate broader acceptance checks. Default-OFF operation
must remain unchanged regardless of the native fixture's result. No merge or release
is performed by this workflow.

## Scale 3 external-bounds regression

Exact source `b48879737f741eef594383a4ff18b9df095301bf` compiled and passed the native
interaction sequence, but its 1920 by 1080 GUI-scale-3 evidence correctly failed the
cross-state artwork check. JEI's item sidebar overlapped the right edge: 37 exterior
pixels changed from empty to half and 33 from empty to full, all within inclusive
framebuffer bounds x=1476..1480, y=802..827. The changes were an animated crimson JEI
entry over the retained canvas, not energy escaping the FE scissor. Independently
recomputed PNG digests matched the native digests.

The screen's inherited NeoForge public bounds reported its internal, unscaled
`0,0,418,418` canvas instead of the rendered GUI-space rectangle. JEI reads those four
public accessors through its NeoForge ScreenHelper. The fix reports outward-rounded
transformed bounds from `getGuiLeft`, `getGuiTop`, `getXSize` and `getYSize`, while
keeping vanilla's internal slot/render fields and inverse input on the logical canvas.
At GUI scale 3 the correct rectangle is `146,6,348,348` in GUI coordinates. The native
fixture now records/asserts these public bounds, independently checked by Python.

This correction covers JEI sidebar placement, not complete JEI slot-input integration.
JEI recipe/use/bookmark targeting can derive ingredient rectangles from public origins
plus the still-local slot coordinates. Those scaled-slot interactions are not qualified
by the bounded native slot-click tests and remain a separate acceptance task.

JEI remains enabled. No source pixels, FE clipping, exterior masks or cross-state
pixel requirements are relaxed. Corrected native screenshots must be obtained from
the new exact candidate head; the earlier failure is not reclassified as a pass.

Primary API references:

- [NeoForge 1.21.1 AbstractContainerScreen accessors](https://github.com/NeoForged/NeoForge/blob/1.21.1/patches/net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.java.patch)
- [JEI 1.21.1 container GUI properties](https://github.com/mezz/JustEnoughItems/blob/1.21.1/Common/src/main/java/mezz/jei/common/gui/GuiProperties.java)
- [JEI NeoForge ScreenHelper](https://github.com/mezz/JustEnoughItems/blob/1.21.1/NeoForge/src/main/java/mezz/jei/neoforge/platform/ScreenHelper.java)


## Physical input qualification candidate, 2026-11-10

Proof suite v2 replaces direct screen-handler clicks with XTest physical motion and
button events, restricted to the runner's nonce-owned Xvfb display. All 37 source
slots and their inventory destinations use the real GLFW/Minecraft input path;
the real Cell charging setup uses the same path. The proof requires exactly 76
clicks, their ordered stages/targets, observed GLFW coordinates within one GUI pixel
of independently recomputed centers, and the same menu identity throughout.
Clicks are separated by at least 350 ms to exclude double-click collection.

Each of the 37 routes also records server-observed source, carried, destination and
total inventory counts for pickup and return. Existing real Cell capability/FE,
server acceptance, item conservation and geometry checks remain mandatory.
Screenshots wait for an observed physically parked pointer and at least two completed
screen renders at the requested FE state. Missing, stale, non-finite, wrong-menu or
handler-only evidence fails closed. No PNG, FE crop, mask, public-bounds or exterior
pixel criterion is relaxed.

Opening, closing and reopening remain programmatic. Drag, shift-click, keyboard,
JEI recipe/use/bookmark input, tooltip coverage and resource reload are not qualified
by this bounded test. Solar remains default OFF; no other screen or mechanics change.
This candidate still requires fresh five-profile CI/native results on its exact head;
prior handler-only results do not qualify v2. Local Python and standalone guards do
not constitute a Minecraft runtime pass.
