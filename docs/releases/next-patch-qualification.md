# Next patch: 1.1.58 qualification record and draft release notes

Status: candidate preparation only, not completed qualification or publication.
Observed 2026-10-13 UTC. Active candidate version `1.1.58-reconstructed` is
configured in project metadata and the changelog. `1.1.57-reconstructed` was
deferred without publication. All fixes and functionality through PRs #43–46
are carried forward; this increment changes metadata and documentation only.
No qualified 1.1.58 JAR, final source commit, reviewed publisher manifest, or
candidate artifact hash is established here.

Every 1.1.57 JAR and hash remains historical evidence. Changing embedded version
metadata produces different artifact bytes; repeat the exact-commit CI,
byte-identity, client and packaged-server gates on the final 1.1.58 artifact.
Do not carry an older hash into a new candidate manifest.

## Source and evidence boundary

### Active preparation base

- Live main verified on 2026-10-13 at
  `90548e2afc49feb7b6328d52e58db8fc3eebe0fd`. This is the preparation base,
  not the final bumped source commit.
- The base contains the earlier placement/collision/selection fixes and the
  CurseForge-only local-source publication tooling from PR
  [41](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/41), plus:
  - [PR #43](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/43): Iron Hatch ladder continuation
  - [PR #44](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/44): metal-ladder physical wall clearance
  - [PR #45](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/45): Steel Mesh Fence Gate passage and connections
  - [PR #46](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/46): accepted-click synchronization for adjacent matching accessways
- Latest public baseline observed on the [CurseForge project](https://www.curseforge.com/minecraft/mc-mods/immersive-engineers-decor-controls-tools-reforged)
  is [1.1.56, file 9042960](https://www.curseforge.com/minecraft/mc-mods/immersive-engineers-decor-controls-tools-reforged/files/9042960).
  No new upload, release, project edit, or issue closure is part of this work.

### Historical 1.1.57 candidate evidence

- [Build run 38326979132](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/38326979132),
  attempt 1, is verified completed/success for a push of the preparation-base
  commit `90548e2afc49feb7b6328d52e58db8fc3eebe0fd`. Its reported results are
  262 required GameTests and 84 publisher tests.
- That run builds **1.1.57**, with reported runtime SHA-256
  `bb635e2c85cc20bb4acc677313bec7d2de186f2dc4132774a9385067fc6c33fc`.
  This digest must not be reused for 1.1.58. Earlier gameplay observations
  likewise remain scoped to the earlier installed artifact, not the new version.
- The [preserved 1.1.57 draft](../../release-bundle/1.1.57-reconstructed/curseforge-notes.md)
  records an earlier notes snapshot. It is not evidence of a published release.

### Earlier fix-merge observations

- Earlier candidate preparation base:
  `e302e772d450a040ad97ed596f3b06cdef6db01f`.
- Earlier inspected fix-merge commit: `e9beae1ba5bc5c1800f59ba1787f77268b6555b5`, after PRs
  [35](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/35),
  [36](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/36),
  [37](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/37),
  [38](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/38),
  [39](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/39), and
  [40](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/40).
- [Build run 38187269438](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/38187269438)
  was recorded completed/success for that earlier fix-merge commit, reporting
  216 required GameTests and 32 publisher tests. Its CI-reported runtime SHA-256
  was `b04793dbe9c2f705f8b2206f8843b9293e1d5f3ff9b8a3e9cfa2e4170c6d49b7`.
  Those bytes were not obtained and independently hashed in that preparation.
  That earlier build uses version `1.1.56-reconstructed`; it does not identify
  either subsequent candidate.

## Draft player-facing notes

Candidate notes are also staged in
[`release-bundle/1.1.58-reconstructed/curseforge-notes.md`](../../release-bundle/1.1.58-reconstructed/curseforge-notes.md).
They remain a draft pending exact-artifact qualification and final review.

This patch adds accepted-click synchronization for adjacent matching doors and
fence gates, including inherited vanilla interactions and IEDCTR Steel Mesh
Fence Gates. Synchronization is restricted to one valid pair; rows do not chain.
Existing sliding-door behavior is retained.

It also fixes several placement, collision, selection, and rendering problems:

- Factory Hopper placement/output direction and extraction from adjacent vanilla
  inventories (#22)
- Steel Floor Grating collision height (#23) and Steel Double T Support collision
  geometry (#24)
- Panzer Glass Slab translucent render declarations (#21) and Exit Sign selection
  outline, retaining its empty collision (#33)
- Connections between decorative walls and Steel Mesh Fence, including mixed
  connections and vanilla walls (#18)
- Player passage through an open Steel Mesh Fence Gate, retaining post/folded-side
  collision, and correctly aligned Steel Mesh Fence connections to the gate
- Iron Hatch hinge selection when placing against floors, ceilings, and walls (#19)
- Ladder continuation into an open Iron Hatch when the ladder immediately below
  shares the leaf's support edge; includes vanilla ladders, Metal Rung Ladder,
  and Staggered Metal Steps, with no climbing through closed or misaligned hatches
- Vanilla physical wall clearance for Metal Rung Ladder and Staggered Metal Steps,
  preventing the player's head from catching below the open Iron Hatch when
  pressing toward the supporting wall
- Structured Vertical Clinker Slab orientation, removing the unwanted gap for
  placement against a wall directly ahead (#20)
- Door Sensor selection outline aligned with its model (#25)
- Industrial Switch cabinet placement, outward facing, two-block selection and
  collision, linked interaction/redstone, and single-item drops (#26)

### Existing worlds and limitations

Back up a world before updating. Existing Industrial Switch cabinets keep their
legacy state and are not automatically converted. Break and re-place an old
cabinet in clear two-block space to obtain the paired layout and corrected
outward orientation. Legacy cabinets cannot provide upper-half selection without
a real upper block. The update does not scan worlds or overwrite occupied space.

Already placed hatches and vertical slabs are not automatically rotated. Existing
wall connections may need a neighbor update to refresh. Steel Mesh Fence remains
a wall-based block. Hopper extraction supports adjacent vanilla `Container`
inventories; inventories exposing only NeoForge `IItemHandler` are not supported
by this extraction path. Door Sensor mounting appearance and empty collision are
unchanged. No exhaustive reporter-modpack compatibility claim is made.

Existing metal ladders preserve facing and waterlogging but gain the same thin
collision strip as vanilla ladders. Models, selection outlines, and placement
remain unchanged. No re-placement or state migration is required for that fix.

Steel Mesh Fence Gate preserves its saved state schema and selection outline;
open collision leaves a player-width corridor while retaining both structural
sides. Models are unchanged, so the thin inward decorative leaf is intentionally
excluded from movement collision. Existing manual-only redstone behavior is
unchanged. See the [tests-first gate report](../bugfixes/steel-mesh-gate-passage-connections.md)
for the ten added GameTests and exact RED evidence. Earlier candidate JARs are
not qualified for these subsequent gate corrections.

Synchronization does not propagate redstone, override refused interactions, or
adapt custom block interaction methods and custom early-use items. Existing
saved block states are preserved; no pair cache or migration is introduced.
See the [synchronized-accessway report](../bugfixes/synchronized-accessway-pairs.md)
for supported pair rules, interaction safeguards, tests and compatibility limits.

## Remaining release gates

1. Verify publication rights and authenticated upload authority for project
   1555214. The official [CurseForge project page](https://www.curseforge.com/minecraft/mc-mods/immersive-engineers-decor-controls-tools-reforged)
   displayed MIT License on 2026-10-12 at 04:29 UTC, matching the repository
   `LICENSE`, packaged metadata, and README; the public label discrepancy recorded
   in [issue #34](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/issues/34)
   is no longer observed. This public readback does not establish authenticated
   upload authority or complete provenance clearance. Preserve all upstream
   notices; this candidate preparation does not change licenses.
2. Confirm candidate version `1.1.58-reconstructed` and the final bumped source commit; run the full Build
   workflow on that exact commit. Independently hash the actual private release JAR, check size,
   embedded mod/version and packaged notices, and create the reviewed manifest
   from those bytes. Do not infer an artifact identity from an earlier CI log.
3. Run client inspection using those bytes: transparent slab rendering, grating
   and support traversal, hopper input/output, mixed wall connections, hatch and
   slab orientations, Door Sensor selection, and new/legacy Industrial Switches.
   For hatch traversal, climb from the ladder below through the open hatch on all
   four support edges using vanilla ladders, Metal Rung Ladder, and Staggered
   Metal Steps while pressing toward the wall; verify actual ascent/descent and
   any collision-sensitive existing builds. Then check closed, opposite/perpendicular, and missing
   ladder arrangements, manual/redstone transitions, and existing saved hatches.
   For Steel Mesh Fence Gates, check open passage, closed blocking and aligned
   fence connections. For synchronized pairs, check opening/closing from either
   leaf and side, door halves and gate segments, rows/singles, refused/iron/custom
   interactions, held-item behavior and independent redstone behavior. Include
   save/reload, partner removal and break/re-place behavior. Preserve
   logs/screenshots labelled with the exact JAR hash.
4. Run an actual packaged dedicated server with the same JAR and required
   Immersive Engineering dependency; check startup, placement/redstone/loot,
   save/restart, and absence of client-only class-loading failures. Development
   GameTests do not replace this gate.
5. Review the final notes and hash-pin them. Run the CurseForge-only local-source
   dry-run using the reviewed source/CI/byte identities. A dry-run is not a
   publication or a client/server validation.

This metadata preparation does not establish new client visuals, packaged-server
validation or authenticated upload rights. No user-computer access, server
installation, merge or publication is part of the version bump. Historical
runtime results cannot silently satisfy the differently versioned artifact's
gates.

## Hatch continuation regression evidence

Test-only commit `7485e4f55fd6561f17ae48c8829aa58176049230` compiled and ran all
223 GameTests in [Build run 38288241929](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/38288241929).
Exactly four new positive-path tests failed at actual player climbing: aligned
ladder, manual opening, loaded open state, and actual placement. The three new
negative tests and all 216 prior tests passed. This establishes the missing
ladder-to-hatch bridge on the previous implementation, not a reproduced
wrong-side movement effect in a reporter modpack.

Corrected-commit GREEN CI and final-artifact client/server qualification must be
recorded separately; the RED run does not qualify a release. See
[`iron-hatch-ladder-continuation.md`](../bugfixes/iron-hatch-ladder-continuation.md)
for the hook compatibility and test scope.

## Metal ladder physical-clearance regression evidence

Test-only commit `5d5093b48dac53829a0ca4f5b1adee94ca868402` compiled and ran all
229 required GameTests in [Build run 38297973485](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/38297973485).
Exactly four new metal-specific tests failed. Both metal ladder types permitted
a zero wall gap and stopped the player's head at the hatch underside, with feet
approximately 1.8 blocks below it. Vanilla ascent, closed-hatch blocking, and all
223 prior GameTests passed.

These tests use `Entity.move` collision resolution with prescribed displacements,
not full `Player.travel`, client input, or gravity. NBT state round-tripping does
not establish actual world reload behavior. The six tests are unchanged by the
minimal two-registration correction. See
[`metal-ladder-hatch-clearance.md`](../bugfixes/metal-ladder-hatch-clearance.md)
for cause, collision impact, evidence, and limits. Corrected-commit GREEN CI and
final-artifact client/server qualification remain separate gates. Earlier
candidate bytes containing PR #43 alone must not be published or reused as final
qualification evidence.
