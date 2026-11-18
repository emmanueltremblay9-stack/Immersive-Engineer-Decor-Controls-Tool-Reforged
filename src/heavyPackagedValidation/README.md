# Heavy native capture harness (bounded evidence)

This isolated development-only mod loads the ordinary production JAR, not main
source outputs. The driver pins its bytes and source commit, creates a fresh
nonce-owned world/display, and retains screenshots and a structured report.
No production model, texture or block behavior is changed.

## Matrix

34 captures: 24 facing/roll combinations; six sturdy-neighbor anchors; an axial
Heavy join followed by removal; a side branch followed by removal. Scene setup
uses server `setBlock`, not simulated or physical item placement. On removals,
the center block is not reset, and no renderer invalidation is forced by the
harness. Each capture waits for the client state and neighbors, 40 stable client
ticks and 40 world render frames. Actual client BakedModel/ModelData quads must
exist with finite positions/UVs and nonmissing sprites. Quads must change after
each neighbor removal. Class, texture, JAR and image bytes are recorded.

## What PASS means, and what it does not

PASS proves the bounded native capture/provenance checks, not visual acceptance.
In particular a fresh BakedModel query is not proof that an already-built chunk
mesh has refreshed. The before/after screenshots must be inspected for that.
The report keeps `mesh_refresh_acceptance`, `visual_acceptance` and
`pixel_ctm_verified` false. No pixel-continuous atlas, exact collision/visual
match, item economy, reload/restart or alternate-renderer qualification is
inferred. One oblique camera per case does not expose every hidden face. Anchor cameras
are placed on the support side of the opaque backing block, with the UP view
above the fixture floor; plate contact faces may still be occluded by design.

The screenshot review should check profile direction and roll, UV orientation,
visible seams/endcaps, six contact plates, axial/branch attachment and visible
neighbor-removal updates. Record concrete observations separately from automated
results; do not interpret a screenshot hash or green test as a visual verdict.

## Execution

`python tools/validation/heavy_native.py --width 1280 --height 720`

Requires Linux, Java 21, Xvfb/xauth, normal Gradle/dependency access and a clean
checkout. `.github/workflows/heavy-double-t-native.yml` runs this exact-head
profile and archives evidence, including failure logs, for review. Python
contract tests do not run Minecraft and must not be described as native results.
