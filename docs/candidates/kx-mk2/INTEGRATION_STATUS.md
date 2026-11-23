# K/X MK2 integration status — 2026-11-22

## Current task boundary

- PR: #200, draft only.
- Parent implementation head for the resumed delta: `df42ba66fce56bd6d11b5fd3804a1a69ff32f2c4` (itself descended from `b57de4fa1aa5bb464d274df7321d1379f3e5d8f9`).
- Owner decision: ONE registered block and ONE BlockItem, path `kx_mk2_support`; all eight K/X R3 forms are derived automatically from canonical reciprocal connections.
- Explicit exclusions: #199 Heavy I-only, #207 microconduit, Prism alpha/Thermal JAR, electrical gameplay, merge, release and publication.

## 2026-11-23 — registry and K/X qualification delta

- Baseline commit: `df42ba66fce56bd6d11b5fd3804a1a69ff32f2c4`.
- Exact current registry inventory: 204 registered mod blocks and 204 `ModBlocks.ORDERED_BLOCKS` entries.
- Native fluidlogging catalogue: exactly 203 entries. `kx_mk2_support` is the sole additional registered exception; every other entry remains a `NativeFluidloggedBlock`.
- `kx_mk2_support` is explicitly negative for both `NativeFluidCells.supported` and `ExternalFluidAdapter.supported` for every blockstate. No fluid property or fluidlogging adapter was added.
- Added only the K/X ID to the vanilla `mineable/pickaxe` block tag; the block retains its metal `requiresCorrectToolForDrops` property.
- Actions run `45092816977` reproduced three failures with and without Lithium: native catalogue count drift, blanket native-catalogue assertion, and missing conventional mining tag. This delta corrects those causes; the new CI results remain pending until Actions completes.
- New GameTests cover an unloaded real chunk edge without force-loading, a loaded cross-chunk reciprocal edge, both `ChunkEvent` lifecycle directions, NBT reload of both endpoint BlockEntities, X-arm removal with three real remaining arms, one drop/no second item debit, and exact UL-versus-LL arm bits while reusing `k_left_side`.
- The common placement-engine regression applies a crossing X with five unique cells, verifies exactly five total item debits, and verifies a repeated application charges zero additional items.
- No changes were pushed to PR #199 or #207. This PR #200 delta touches two paths that are independently modified on PR #199 (`ExternalFluidloggingGameTests.java` and `mineable/pickaxe.json`); the other PR branch remains unchanged and future combined integration must reconcile those overlapping paths.
- No Prism JAR or R3 model/atlas assets were changed or added. No merge, release or publication was performed.

## Implemented in this draft delta

1. `KXModule` adds exactly one block, one BlockItem and one BlockEntity type through the existing IEDCTR registries. Initialization occurs before legacy aliases and registry binding.
2. `KXSupportBlock` stores six-way facing plus four-way roll (24 proper orientations). Its collision core is the common R3 I-profile; brace-arm collision is not yet qualified.
3. `KXSupportBlockEntity` persists schema version 1 with up to four directed edge records. Each edge stores peer position, local/peer slots, plane and both explicit port coordinates.
4. An edge is real only when the peer chunk is already loaded, the peer is still the same K/X block, orientations match, positions are adjacent on the 45-degree brace plane, and the peer stores the exact inverse record. One-sided, stale, absent or mismatched peers contribute no arms.
5. Source and target slots/coordinates are supplied explicitly. The API never guesses same/opposite slot pairing. The eight topology keys are derived only from those reciprocal records and the verified down-face support contact.
6. Server updates send the canonical state plus a derived model-selection projection. Chunk load/unload invalidations are deferred to the server task queue and never force-load chunks.
7. `KXTopology` selects all eight R3 keys: clean, anchor_down_only, K-left, K-right, K-both, K-staggered, X-centered and X-broken-three. The broken X exposes only three actual arms and no center plate.
8. The legacy two-endpoint `KXInteraction` path accepts only an explicitly matching source/target slot because its existing `Span` record can represent one slot only. Different slots are rejected; no opposite-slot rule is inferred.
9. GameTests cover one-item placement, all 24 blockstate orientations, all eight topology keys, save/load round-trip, update-tag/model-data synchronization and one-sided/removed-peer ghost rejection.

## Explicit remaining gates

- **Player port-picking / span placement:** no proven user-facing source-to-target pairing contract or per-cell intermediate segment map was present in the recovered core. The metadata API requires both endpoint slots and coordinates explicitly; it does not invent a click mapping, place intermediate blocks, or consume items. The old ambiguous interaction path fails closed.
- **Rendering:** the R3 BBModels and atlas remain private test inputs. Redistribution rights are UNKNOWN. No blockstate model, item model, atlas, texture, or fallback renderer is added to this public repository. Client rendering is therefore intentionally NOT_READY; this draft must not be built/distributed as a playable public version.
- **Translations:** the existing language JSON files are concurrently modified by #207 and are intentionally untouched in this delta.
- **Arm collision:** only the shared central I-profile collision is present. Exact arm collision needs a separately reviewed geometry/collision contract.
- **Native Blockbench open/save/reopen:** NOT_RUN. Local static R3 package checks do not claim native Blockbench qualification.
- **Actual client visual rendering:** NOT_RUN / BLOCKED_BY_REDISTRIBUTION_RIGHTS.

## Provenance and safety

- R3 source ZIP authenticated locally by SHA-256 `4eae32046a0acdbcdf828cc519eac53eaad4c7ffa9c3e52f2fb780b8d636af26`; it is not the 81-file Handoff archive.
- R3 atlas SHA-256 `27c8fef6a2c816ddd82ab760308b3b23a9c6e81331b1b66e1170211fbb6b532b`; not copied into the repository.
- No third-party source/atlas assets were added. No electrical, FE, or unrelated gameplay was changed.
- No files from #199 or #207 were edited. No merge, tag, release or publication was performed.
