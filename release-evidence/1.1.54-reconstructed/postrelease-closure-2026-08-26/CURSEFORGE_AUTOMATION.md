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
- Authenticated prepare phase that resolves current game-version names but performs no POST.
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

- Publisher unit/mock tests: 27/27 passed with `ResourceWarning` promoted to error.
- Covered: documented relation schema plus wrong-schema rejection, zero-based pagination, request-byte parity, exact duplicate, divergent duplicate, missing/rejected/forbidden token, project denial, durable intent validation, mutated intent rejection, stale intent-as-result rejection, missing/expired state after a persisted intent, processing resume, non-retried server/transport/JSON/missing-ID ambiguity, unexpected upload/poll failures, file-ID preservation, public metadata/dependency/hash readback, and secret non-disclosure.
- actionlint 1.7.12 archive SHA-256: `6E7241B51E6817EA6A047693D8E6FED13B31819C9A0DD6C5A726E1592D22F6E9`.
- actionlint result: exit 0 for all workflows.
- Current publisher/workflow/test SHA-256 values: `6A59D46067E0D97D2B8B25EB6A44477F4877640786ABF07E4F41DF4735E38010`, `BD280594358BFE5CE87CC9CFECD34A79FF0A38DEEB3E2935572069DA948A9B88`, and `8CD93ECAD89B02FBEF4149C9C4422F4DDD0F851E994F0BE4193BEAC8FF221935`.
- Live dry-run status: `AUTOMATION_READY_DRY_RUN`, exit 0, no upload.
- The live dry run also matched all four project-level dependencies and all four relations on approved baseline file 8420050. The four newest public files expose the same full relation set; the three older files predate the project-level `Include` configuration.
- Retained report: `CURSEFORGE_DRY_RUN_REPORT.json`; SHA-256 `5F10F226C988C6E55FE7D1FAAF7FA30C9ACE7C5D24A2DE8A608830E6DADBBD20`.
- Metadata SHA-256: `14886AF9E71407D12E664B853A21EF221715CD37349EA0B45A6BBC3981EFBE43`.
- Multipart SHA-256: `ABB81B6E0ED9B4586306ACE4B4E167E1C51C2631CF9B1ED4F24F6248F1271597`; 2,681,730 bytes.

This validation is anchored to isolated-branch implementation commit `2d186c5fdfcc082c5cb7b350eb5694ff5ee9f715` and remains unmerged until PR/CI integration is recorded. A live dry-run is preflight proof, not upload or publication proof.

## Authority and publication state

- Process `CURSEFORGE_API_TOKEN` value inspected: no
- Repository Actions secret name `CURSEFORGE_API_TOKEN`: present; names-only update timestamp `2026-08-27T03:50:33Z`
- User confirmation: the repository secret contains a completely new replacement token
- Secret value read, printed, copied, or logged by Codex: no
- Real upload attempted at this snapshot: no
- Public file ID at this snapshot: none

`CURSEFORGE_AUTOMATION_VERDICT: READY_PENDING_PR_CI`

`CURSEFORGE_PUBLICATION_VERDICT: NOT_PERFORMED_PENDING_MERGED_WORKFLOW`
