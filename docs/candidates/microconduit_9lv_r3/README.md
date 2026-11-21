# IEDCTR microconduit R3 — source assets and implementation qualification
Status: CANDIDATE / DRAFT / NOT_MERGED.
Scope: C01, C03, S01 only; E01-E03 are intentionally excluded by owner decision.

Owner-defined contract:
- C01: NINE circular, physically distinct LV microconduits in ONE straight line in a 16x16x16 block envelope.
- Each conduit is a separate LV circuit, with no implicit joining between channels 1..9.
- C03: one compact machine connector accepts exactly ONE designated LV line.
- S01: separate structural support, nine rounded clamps on a rail; mechanical only, never FE conductor.
- Industrial IE look: galvanized steel, brass clamps, dark machine connector. No sci-fi glow.
- Construction eventually supports wall/ceiling/floor/beam/column; each mounting and render variant requires verification.
- R3 .bbmodel files are EDITABLE SOURCE artifacts, not automatically valid in-game models.
- Atlas PNG is the exact embedded R3 atlas used by all three sources.

Validation required before marking functional: Blockbench readback, NeoForge asset/model loading, Java runtime energy-capability integration,
nine-lane isolation, exact FE conservation, loading/unloading, multi-sink/source, no forced chunks, placement/collision,
and a real Minecraft GameTest/physical test. A static pass does NOT establish those results.

PR is not authorization to merge/tag/release/publish. Do not alter unrelated GUI, KX, Double-T or release areas.

## R3 functional topology update
- Multiple individually placed C03 machine connectors may tap the SIDES of a long C01 bundle, not only its end faces. This is required for more than one LV line to be used over a shared physical run.
- Each C03 exposes a single selected lane [1..9] and explicitly configures source/sink behavior; lane changes cannot move saved residual FE between lanes.
- Only same-axis adjacent C01 blocks continue a conduit segment. Side-by-side perpendicular bundles do not merge.
- Source/sink transitions read the machine's faced NeoForge energy capability; no C01 FE buffer is created.
- Side-tap model/port visual join, mounting proof and runtime behavior in Minecraft still require native validation.
