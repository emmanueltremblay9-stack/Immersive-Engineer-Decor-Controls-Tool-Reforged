# CurseForge automation evidence

## Authoritative source

- GitHub repository: `emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged`
- Public tag: `v1.1.54-reconstructed`
- GitHub release ID: 369309975; `draft=false`; `prerelease=false`
- Canonical asset ID: 511687758
- Asset: `immersive_engineer_decor_controls_tool_reforged-1.1.54-reconstructed.jar`
- Size: 2,678,440 bytes
- GitHub digest and redownload SHA-256: `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`
- Embedded mod ID/version: `immersive_engineer_decor_controls_tool_reforged` / `1.1.54-reconstructed`

The publisher downloads this existing public asset. It does not build, transform, rename, or replace it.

## Existing public baseline

Live preflight on 2026-08-27 found no public 1.1.53 or 1.1.54 file in CurseForge project 1555214. The approved baseline is file 8420050 (`1.1.41-reconstructed`) with release type `release`, labels `Client`, `Server`, `1.21.1`, and `NeoForge`, and these public relations:

- project 313866 / `engineers-decor` / `Include`
- project 319716 / `engineers-tools` / `Include`
- project 296686 / `redstone-gauges-and-switches` / `Include`
- project 231951 / `immersive-engineering` / `RequiredDependency`

JEI is absent from the approved CurseForge metadata and is not added.

## Request contract and platform boundary

The [official CurseForge Upload API](https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-api) accepts `gameVersionNames`, requires project relations under `relations.projects`, and supports `requiredDependency` but not `Include` as an upload relation type. The request therefore transmits only Immersive Engineering as `requiredDependency`.

All four approved public relations are required in public readback. The three `Include` relations are platform behavior that mocked tests can model but only a real post-upload readback can prove.

`isMarkedForManualRelease=false` is explicit. The documented `true` setting holds an approved file for a later manual choice, and the same documentation exposes no API operation for that later release. The user authorized the attached autonomous, no-computer-use closure prompt as executable instructions and later confirmed the new replacement repository secret. Selecting `false` is therefore a scope-derived decision, not an unqualified platform-safety claim: public verification is necessarily post-publication rather than a pre-publication quarantine.

## Implemented controls

- Hash-pinned JSON release manifest and UTF-8 changelog.
- GitHub release state, asset cardinality, name, size, API digest, redownload hash, mod ID, and version checks.
- Zero-based `pageIndex` pagination across the public CurseForge file list.
- Exact-match idempotency and hard conflict for divergent or ambiguous version reuse.
- Deterministic metadata and multipart bytes shared by dry-run, prepare, and publish.
- Dry-run workflow step without `CURSEFORGE_API_TOKEN`; the read-only GitHub token remains available for source verification.
- Authenticated prepare phase that resolves the configured catalog-backed `gameVersionLookupNames` subset but performs no POST; the complete `gameVersionNames` upload labels remain unchanged.
- Separate ephemeral intent and result files; an intent-only report can never be uploaded under a result artifact name.
- Immutable GitHub Actions upload-intent artifact persisted before POST.
- Prior workflow-run attempts and job-step conclusions are reconciled; while matching GitHub metadata remains queryable, a successful intent-persistence step without an active result, including after artifact expiry, blocks another POST. It cannot recover an expired processing file ID.
- Current intent artifact ID/name and request hashes revalidated immediately before POST.
- Exactly one non-retried upload POST.
- `UPLOAD_OUTCOME_UNKNOWN` for post-POST transport/server/JSON/missing-ID ambiguity and unexpected failures anywhere after the POST boundary; a returned file ID is preserved.
- Prior unmatched intent or ambiguous result blocks another POST.
- Positive processing file IDs are resumed without a CurseForge token or second upload.
- Required public approved status, metadata, relations, size, SHA-256, mod ID, and version readback.
- Sanitized reports and workflow summaries contain no token value, request header, or response body.
- Tag-scoped non-cancelling concurrency; read-only `contents` and `actions` permissions; full-SHA third-party actions.

## Validation snapshot

- Publisher unit/mock tests: 29/29 passed with `ResourceWarning` promoted to error.
- Covered: documented relation schema plus wrong-schema rejection, zero-based pagination, request-byte parity, exact duplicate, divergent duplicate, missing/rejected/forbidden token, catalog-backed lookup distinct from upload labels, project denial, durable intent validation, mutated intent rejection, stale intent-as-result rejection, missing/expired state after a persisted intent, explicit and durable processing resume, exact-public-file precedence over stale history and mutable aggregation, non-retried server/transport/JSON/missing-ID ambiguity, unexpected upload/poll failures, file-ID preservation, public metadata/dependency/hash readback, and secret non-disclosure.
- actionlint 1.7.12 archive SHA-256: `6E7241B51E6817EA6A047693D8E6FED13B31819C9A0DD6C5A726E1592D22F6E9`.
- actionlint result: exit 0 for all workflows.
- Current publisher/workflow/test SHA-256 values at commit `06f88fbf7ed734d400067511de587249251d2a8e`: `41AB04476D7AEDD6483C017DD4D9615045AC6AAE25BC4EFF86C3044982B25B9A`, `BD280594358BFE5CE87CC9CFECD34A79FF0A38DEEB3E2935572069DA948A9B88`, and `BC89130B57166A4C6C5C616ACE95F3FAB158325761EE29815F86BDD82E8EF4BE`.
- Live dry-run status: `AUTOMATION_READY_DRY_RUN`, exit 0, no upload.
- The live dry run also matched all four project-level dependencies and all four relations on approved baseline file 8420050. The four newest public files expose the same full relation set; the three older files predate the project-level `Include` configuration.
- Retained report: `CURSEFORGE_DRY_RUN_REPORT.json`; SHA-256 `5F10F226C988C6E55FE7D1FAAF7FA30C9ACE7C5D24A2DE8A608830E6DADBBD20`.
- Metadata SHA-256: `14886AF9E71407D12E664B853A21EF221715CD37349EA0B45A6BBC3981EFBE43`.
- Multipart SHA-256: `ABB81B6E0ED9B4586306ACE4B4E167E1C51C2631CF9B1ED4F24F6248F1271597`; 2,681,730 bytes.

The original implementation was integrated by PR 3, the upload-label/catalog split by PR 4, and the multi-record correction by PR 5. PR 5 merged at `f29c73370e586df33c0c0036603a816c53082903`; both Build checks and merged dry-run 33043386837 succeeded. The 29-test accepted-file resume correction is commit `06f88fbf7ed734d400067511de587249251d2a8e` and remains pending integration PR/CI at this snapshot.

## Integrated workflow and first authenticated attempt

- PR 3 merged at `1b3d4db1ae2215deb754ed8c37ce9352aa428c67`; both Build checks succeeded.
- Merged dry-run 33041164651 succeeded and retained sanitized result artifact 9633912553.
- Authenticated run 33041227322 failed closed in prepare with `CURSEFORGE_GAME_VERSION_UNRESOLVED` because the preflight incorrectly required the environment/loader labels to exist in the game-version catalog.
- In that run, `Persist upload intent before any POST` and `Submit exact persisted request once` were both skipped; the run retained zero artifacts and returned no file ID. No upload was attempted.
- The correction separates the full upload-label list from `gameVersionLookupNames: ["1.21.1"]`. The metadata and multipart hashes remain unchanged.
- PR 4 merged that split at `316a864ce6d54a26bfe435def6fe4ce4da23fe77`; both Build checks and merged dry-run 33042387761 succeeded.
- Authenticated `main` run 33042452350 again failed before persistence/POST. A diagnostic branch guaranteed to stop before persistence classified the live lookup as `CURSEFORGE_GAME_VERSION_AMBIGUOUS` in run 33042661545. Both runs retained zero artifacts.
- Multiple same-name catalog entries are legitimate records under different game-version types. The current correction validates every positive numeric ID, deduplicates and sorts the ID set, and requires prepare/publish to recompute the same set before POST.

## PR 5 integration and accepted upload

- PR 5 merged the typed multi-record correction at `f29c73370e586df33c0c0036603a816c53082903`; Build runs 33043252946 and 33043264510 both succeeded.
- Merged dry-run 33043386837 passed from that exact commit. Artifact 9634702716 retained the canonical report/request hashes and performed no upload.
- Authenticated run 33043440815 resolved `1.21.1` to live IDs 11779, 12735, and 16115; the sorted set matched prepare/publish intent validation.
- Intent artifact 9634723269 was persisted before POST with archive digest `79CAD9C583EAC28908EF25C1043B78899C66452E5EF83A674F94C92C3C1FC7A4`.
- Exactly one upload POST returned file ID 8744461. The bounded public poll ended as `UPLOADED_PROCESSING`; result artifact 9634910382 preserved that positive ID. No second upload POST was issued.
- CurseForge-token-free resume run 33044033312 performed no upload and exposed mutable project-level relation drift before file-level readback. Commit `06f88fbf7ed734d400067511de587249251d2a8e` makes explicit, exact-public, and durable accepted-file verification skip only that mutable aggregate while retaining the approved baseline file and every exact target-file gate.

## Public artifact proof and relation blocker

Fresh public readback of file 8744461 proves project ID 1555214, approved status 4, release type 1, exact filename/display name, labels `Client`, `Server`, `1.21.1`, and `NeoForge`, and size 2,678,440 bytes. The public redownload SHA-256 is `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`; embedded NeoForge metadata matches mod ID `immersive_engineer_decor_controls_tool_reforged` and version `1.1.54-reconstructed`.

The file and project dependency endpoints expose only project 231951 / `immersive-engineering` / `RequiredDependency`. The three expected public `Include` relations are absent. The official write API accepts `embeddedLibrary` but not `Include`, while the official public enum defines `EmbeddedLibrary` and `Include` as different values. A metadata-update prototype was therefore rejected during independent review and removed before commit or POST. No relation-update POST and no additional upload POST occurred.

## Authority and publication state

- Process `CURSEFORGE_API_TOKEN` value inspected: no
- Repository Actions secret name `CURSEFORGE_API_TOKEN`: present; names-only update timestamp `2026-08-27T03:50:33Z`
- User confirmation: the repository secret contains a completely new replacement token
- Secret value read, printed, copied, or logged by Codex: no
- Authenticated publication workflow dispatched: yes; run 33043440815 crossed the guarded upload boundary
- Real upload POST count: exactly one
- Public file ID: 8744461
- Public artifact hash match: yes
- Public relation match: no; three approved `Include` relations are missing
- Relation-update POST attempted: no
- Additional upload POST attempted: no

`CURSEFORGE_AUTOMATION_VERDICT: PASS`

`CURSEFORGE_PUBLICATION_VERDICT: BLOCKED_PUBLIC_RELATION_MISMATCH`
