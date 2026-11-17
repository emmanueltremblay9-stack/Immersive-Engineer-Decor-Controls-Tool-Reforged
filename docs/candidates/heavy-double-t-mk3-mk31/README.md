# Heavy Industrial Double-T Support — MK3 / MK3.1 (candidate)

Project: Immersive Engineer Decor & Controls & Tool Reforged (DS-IEDCTR)

**Status: PR_DRAFT_RUNTIME_CANDIDATE / NOT_MERGED / NOT_TESTED_IN_GAME.**

Source reviewed against implementation `main` at `e8b21c91d5a7a3a6beb9d0b12c71e8bccc0f8d2c` (2026-11-16). At the initial archive-only commit this candidate did not touch production sources. The later draft integration now stages new runtime Java/resources and a **PROPOSED**, not adopted, separate registry entry; it does not rename existing Double-T IDs, rewrite saved states, alter paid bulbs or publish artifacts.

## Contents

- `IEDCTR_Heavy_Double_T_MK3_MK31_SOURCES.tar.xz` — seven source files, including four Java candidate classes, the Python resource generator, six static-test definitions and the owner's original `.bbs.json` structural reference.
- MK3: 6 directional axes/faces × 4 quarter-turn rolls (24 discrete orientations); context-sensitive axial joints, side junctions and visual ends.
- MK3.1: conditional baseplates on directly contacting sturdy faces, without plate duplication between compatible candidate supports.

**Archive SHA-256:** `6e30c2e467ca5c7d2615a48023f5c66e2a0987828ccc9473eaedb23734345749`
**Archive Git blob SHA-1:** `c76b4d9455df02f4a4ec863837aa077c723acf05`
**Source conversation ZIP SHA-256:** `bdcb2dc65bd4b5523b0ac80738ac3ed9ca0d012c2065d24ab57ace83f965a931`

This compact source archive is **not byte-identical** to the conversation ZIP. The 87 generated model/asset JSON files are **not committed** here; they are reproducible by running the included `tools/models/generate_heavy_double_t.py`. The owner's image reference is also omitted from this source-only archive. The full original ZIP remains separately available in the originating conversation.

## Reproduce the static candidate check

Extract the `.tar.xz` into an isolated workspace, then from its root:

```bash
python3 tools/models/generate_heavy_double_t.py
python3 tools/models/generate_heavy_double_t.py --check
python3 -m unittest discover -s tests -v
```

Reproduced locally before upload: **87 JSON assets generated / checked, six static tests PASS**. These checks prove neither NeoForge compilation nor Minecraft rendering.

## Integration gates (NOT_PERFORMED)

1. Re-read live target `AGENTS.md`, branch/HEAD, target source paths, ongoing PR overlap and the accepted source-policy revision before editing runtime files.
2. Review owner decision on candidate registration ID `heavy_industrial_double_t_support` versus integration with existing `steel_double_t_support` and `vertical_steel_double_t_support`. No new registry identity is adopted here.
3. Add registration, localization, mining metadata and generated resources only after compatibility checks, and verify APIs against the exact NeoForge toolchain.
4. Compile; run focused JUnit/GameTests; inspect actual placement in 24 orientations, all six anchors, CTM surface/UV continuity, chunk reload and neighbor changes, legacy support/lamp/fluidlogging regressions and dedicated-server behavior.

**Explicit limitations:** the candidate does not include a dedicated pixel-continuous CTM atlas, full collision/visual QA or validated compatibility with legacy supports. Do not claim implementation, integration, release or adoption from this commit.

**Git operation scope:** candidate source archive + this README on a dedicated branch only; no PR, merge, tag, release or publication.

## Reviewable source expansion (2026-11-17)

The `review/` directory now contains the seven original archive members as individual files, plus all 87 generated JSON resources. The archive is retained unchanged as provenance. Candidate files remain under `docs/candidates/`; this does not register a new block, alter production source/resource sets or claim runtime integration.

To reproduce from the repository root:

```sh
cd docs/candidates/heavy-double-t-mk3-mk31/review
python3 tools/models/generate_heavy_double_t.py --check
python3 -m unittest discover -s tests -v
```

Reproduced on this revision: 87 generated assets checked; six static tests passed. NeoForge compilation, GameTests, actual CTM visual continuity and runtime integration remain NOT_RUN. This revision makes the candidate reviewable through a draft PR, superseding the earlier branch-only delivery scope; it does not adopt the candidate registration ID.

## Candidate compilation gate

`bash gradlew compileHeavyDoubleTCandidateJava jar` now compiles the actual four candidate classes against the production toolchain in a separate source set. The dedicated workflow verifies compiled classes exist and do not enter the normal JAR. Candidate models discover registered blocks by class rather than referencing a nonexistent `ModBlocks` field; this does not register or adopt any candidate block.

Local cloud compilation is currently blocked before Gradle starts: the official distribution download reports `Network is unreachable`. The new CI gate must pass before claiming compilation. No candidate runtime is loaded by this source set, so placement/rendering/GameTests still remain unqualified.

## 2026-11-17 — Draft runtime integration gate

The PR now stages the production source files, all 87 source-identical generated JSON assets and a **proposed** registered `immersive_engineer_decor_controls_tool_reforged:heavy_industrial_double_t_support` item. Legacy identities are unchanged, with the historical bulb and fluidlogging logic unmodified. `HeavyDoubleTTopology` draws junction geometry only when the new heavy beam's and legacy beam's collision profiles share positive area at the touching face. It does not rewrite legacy neighbor blockstates. Sturdy full-face backing produces an anchor; compatible heavy axial sections produce continuous middles.

This is **draft source integration**, not owner acceptance of a new registry name, passed gameplay QA, connected pixel UV proof or release. The stage's GameTests are source-provided; they remain NOT_RUN until an actual target GameTest server run reports their execution. CI compilation, JUnit and JAR inspection provide only their respective evidence; do not substitute them for rendered client or actual gameplay tests. Existing client texture atlases are reused and exact CTM seam/UV continuity is still unqualified.

The separate compile-only `heavyDoubleTCandidate` Gradle source set was removed so duplicate packaged and review code are not compiled together; the historical review copy remains unchanged under `docs/candidates/`. This branch may only be merged by a separate owner decision after verified integration.
