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
- Secret-free dry-run workflow step.
- Authenticated prepare phase that resolves the configured catalog-backed `gameVersionLookupNames` subset but performs no POST; the complete `gameVersionNames` upload labels remain unchanged.
- Separate ephemeral intent and result files; an intent-only report can never be uploaded under a result artifact name.
- Immutable GitHub Actions upload-intent artifact persisted before POST.
- Prior workflow-run attempts and job-step conclusions are reconciled; while matching GitHub metadata remains queryable, a successful intent-persistence step without an active result, including after artifact expiry, blocks another POST. It cannot recover an expired processing file ID.
- Current intent artifact ID/name and request hashes revalidated immediately before POST.
- Exactly one non-retried upload POST.
- `UPLOAD_OUTCOME_UNKNOWN` for post-POST transport/server/JSON/missing-ID ambiguity and unexpected failures anywhere after the POST boundary; a returned file ID is preserved.
- Prior unmatched intent or ambiguous result blocks another POST.
- Positive processing file IDs are resumed without a token or second upload.
- Required public approved status, metadata, relations, size, SHA-256, mod ID, and version readback.
- Sanitized reports and workflow summaries contain no token, request header, or response body.
- Tag-scoped non-cancelling concurrency; read-only `contents` and `actions` permissions; full-SHA third-party actions.

## Validation snapshot

- Publisher unit/mock tests: 28/28 passed with `ResourceWarning` promoted to error.
- Covered: documented relation schema plus wrong-schema rejection, zero-based pagination, request-byte parity, exact duplicate, divergent duplicate, missing/rejected/forbidden token, catalog-backed lookup distinct from upload labels, project denial, durable intent validation, mutated intent rejection, stale intent-as-result rejection, missing/expired state after a persisted intent, processing resume, non-retried server/transport/JSON/missing-ID ambiguity, unexpected upload/poll failures, file-ID preservation, public metadata/dependency/hash readback, and secret non-disclosure.
- actionlint 1.7.12 archive SHA-256: `6E7241B51E6817EA6A047693D8E6FED13B31819C9A0DD6C5A726E1592D22F6E9`.
- actionlint result: exit 0 for all workflows.
- Current publisher/workflow/test SHA-256 values: `26EE738543047549973C621E67687486B48B85550C63A5005D85AD296F725960`, `BD280594358BFE5CE87CC9CFECD34A79FF0A38DEEB3E2935572069DA948A9B88`, and `E2413A0F32F44EF79C61A07592DC8F2F69FD258426B27E73A8A51112AD17BACF`.
- Live dry-run status: `AUTOMATION_READY_DRY_RUN`, exit 0, no upload.
- The live dry run also matched all four project-level dependencies and all four relations on approved baseline file 8420050. The four newest public files expose the same full relation set; the three older files predate the project-level `Include` configuration.
- Retained report: `CURSEFORGE_DRY_RUN_REPORT.json`; SHA-256 `5F10F226C988C6E55FE7D1FAAF7FA30C9ACE7C5D24A2DE8A608830E6DADBBD20`.
- Metadata SHA-256: `14886AF9E71407D12E664B853A21EF221715CD37349EA0B45A6BBC3981EFBE43`.
- Multipart SHA-256: `ABB81B6E0ED9B4586306ACE4B4E167E1C51C2631CF9B1ED4F24F6248F1271597`; 2,681,730 bytes.

The original implementation was integrated by PR 3 and the upload-label/catalog split by PR 4. The multi-record correction and current 28-test result are anchored to isolated-branch commit `0b3ced31b54444b312577fb23485a1e4eac132cc` and remain pending corrective PR/CI. A live dry-run is preflight proof, not upload or publication proof.

## Integrated workflow and first authenticated attempt

- PR 3 merged at `1b3d4db1ae2215deb754ed8c37ce9352aa428c67`; both Build checks succeeded.
- Merged dry-run 33041164651 succeeded and retained sanitized result artifact 9633912553.
- Authenticated run 33041227322 failed closed in prepare with `CURSEFORGE_GAME_VERSION_UNRESOLVED` because the preflight incorrectly required the environment/loader labels to exist in the game-version catalog.
- In that run, `Persist upload intent before any POST` and `Submit exact persisted request once` were both skipped; the run retained zero artifacts and returned no file ID. No upload was attempted.
- The correction separates the full upload-label list from `gameVersionLookupNames: ["1.21.1"]`. The metadata and multipart hashes remain unchanged.
- PR 4 merged that split at `316a864ce6d54a26bfe435def6fe4ce4da23fe77`; both Build checks and merged dry-run 33042387761 succeeded.
- Authenticated `main` run 33042452350 again failed before persistence/POST. A diagnostic branch guaranteed to stop before persistence classified the live lookup as `CURSEFORGE_GAME_VERSION_AMBIGUOUS` in run 33042661545. Both runs retained zero artifacts.
- Multiple same-name catalog entries are legitimate records under different game-version types. The current correction validates every positive numeric ID, deduplicates and sorts the ID set, and requires prepare/publish to recompute the same set before POST.

## Authority and publication state

- Process `CURSEFORGE_API_TOKEN` value inspected: no
- Repository Actions secret name `CURSEFORGE_API_TOKEN`: present; names-only update timestamp `2026-08-27T03:50:33Z`
- User confirmation: the repository secret contains a completely new replacement token
- Secret value read, printed, copied, or logged by Codex: no
- Authenticated publication workflow dispatched: yes, run 33041227322
- Real upload POST attempted at this snapshot: no; prepare stopped before intent persistence and upload
- Public file ID at this snapshot: none

`CURSEFORGE_AUTOMATION_VERDICT: MULTI_RECORD_FIX_READY_PENDING_PR_CI`

`CURSEFORGE_PUBLICATION_VERDICT: NOT_PERFORMED_SAFE_PREFLIGHT_STOP`
