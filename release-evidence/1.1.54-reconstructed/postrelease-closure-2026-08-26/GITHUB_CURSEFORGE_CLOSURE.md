# GitHub integration and guarded CurseForge attempt

## Integrated implementation

- PR: [#3](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/3)
- PR head: `e18d155490f49b3a658363e013c186c3c1b5300c`
- Merged: 2026-08-27T05:00:36Z
- Merge commit / then-current `main`: `1b3d4db1ae2215deb754ed8c37ce9352aa428c67`
- Build runs 33040937740 and 33040964379: `SUCCESS`

The merged publisher dry run without `CURSEFORGE_API_TOKEN`, [run 33041164651](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/33041164651), completed successfully. Result artifact 9633912553 was named `iedct-cf-v1.1.54-reconstructed--33041164651-1--result--AUTOMATION_READY_DRY_RUN--0`; its Actions archive digest was `aa4109564d0511c971df04897d43a18f49621147c9a6593e5e330924634f02ab`. The extracted report was 1,904 bytes with SHA-256 `5F10F226C988C6E55FE7D1FAAF7FA30C9ACE7C5D24A2DE8A608830E6DADBBD20` and reproduced metadata hash `14886AF9E71407D12E664B853A21EF221715CD37349EA0B45A6BBC3981EFBE43`, multipart hash `ABB81B6E0ED9B4586306ACE4B4E167E1C51C2631CF9B1ED4F24F6248F1271597`, and size 2,681,730. No CurseForge token or upload was used; the workflow's read-only GitHub token verified the source.

## First authenticated run stopped before upload

[Run 33041227322](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/33041227322) received the repository secret through GitHub Actions and used it for authenticated preflight; this evidence records only its name and masked use, not its value. The prepare step returned `CURSEFORGE_GAME_VERSION_UNRESOLVED` with exit 7. GitHub step readback proves:

- `Persist upload intent before any POST`: skipped
- `Submit exact persisted request once`: skipped
- retained workflow artifacts: zero
- returned CurseForge file ID: none

The public duplicate scan immediately after the run still found no 1.1.54 file. Therefore this was an authenticated preflight failure, not an upload attempt and not an ambiguous POST outcome.

## Root cause and correction

The request correctly uploads `gameVersionNames: ["Client", "Server", "1.21.1", "NeoForge"]`. The original authenticated preflight incorrectly required all four upload labels to appear as unique records in `/api/game/versions`. The official API distinguishes the game-version catalog from the upload `gameVersionNames` labels, whose examples include environment labels alongside a numeric version.

Correction commit `0f2117d1153d8122ca371f0a3478761fa7c89789` adds an explicit `gameVersionLookupNames: ["1.21.1"]` subset. The authenticated lookup resolves only that catalog-backed version; the request and final public readback still require all four labels. A fake endpoint exposing only `1.21.1` now passes prepare, while a configured but absent lookup record still fails before POST.

Local correction proof:

- publisher suite: 28/28, exit 0
- Python compile and JSON parse: exit 0
- actionlint 1.7.12: exit 0
- live dry run without `CURSEFORGE_API_TOKEN`: `AUTOMATION_READY_DRY_RUN`, exit 0
- gitleaks 8.30.1: exit 0, approximately 28.85 MB scanned, no leaks
- metadata/multipart hashes and multipart size: unchanged
- independent code review: PASS

`CORRECTIVE_PR_CI_AT_THAT_SNAPSHOT: PENDING`

`CURSEFORGE_UPLOAD_POST_ATTEMPTED_AT_THAT_SNAPSHOT: NO`

`CURSEFORGE_PUBLICATION_STATE_AT_THAT_SNAPSHOT: NOT_PERFORMED_SAFE_PREFLIGHT_STOP`

## PR 4 integration and multi-record diagnosis

PR [#4](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/4) merged the upload-label/catalog split at `316a864ce6d54a26bfe435def6fe4ce4da23fe77` on 2026-08-27T05:23:56Z. Build runs 33042249389 and 33042263714 both succeeded. Merged dry-run [33042387761](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/33042387761) succeeded from that exact `main` commit; downloaded artifact 9634352015 reproduced the canonical 1,904-byte report, report SHA-256 `5F10F226C988C6E55FE7D1FAAF7FA30C9ACE7C5D24A2DE8A608830E6DADBBD20`, and unchanged request hashes.

Authenticated `main` run [33042452350](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/33042452350) again stopped in prepare with the original generic unresolved status. Intent persistence and upload were skipped; artifact count was zero and no file ID was returned.

A dedicated diagnostic branch made the catalog outcomes distinct and included an explicit forced stop immediately after a hypothetical exact match. Authenticated diagnostic run [33042661545](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/33042661545) returned `CURSEFORGE_GAME_VERSION_AMBIGUOUS`. The persistence/upload steps were skipped and artifact count was zero. This proves that `1.21.1` exists as multiple exact-name catalog records; no response body, token, or other catalog names were logged.

Commit `0b3ced31b54444b312577fb23485a1e4eac132cc` therefore treats one-or-more same-name records as valid only when every ID is a positive non-boolean integer. It binds a sorted unique ID list into both prepare and publish durable-intent comparisons. Missing and invalid-ID records still fail before persistence/POST; upload metadata remains unchanged because the official request uses `gameVersionNames`.

`MULTI_RECORD_CORRECTIVE_PR_CI_AT_THAT_SNAPSHOT: PENDING`

`PRE_PR5_AUTHENTICATED_RUNS_UPLOAD_POST_ATTEMPTED: NO`

## PR 5 integration and merged dry run

PR [#5](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/5) merged the typed multi-record correction at `f29c73370e586df33c0c0036603a816c53082903` on 2026-08-27T05:43:00Z. Build runs 33043252946 and 33043264510 both succeeded.

Merged dry-run [33043386837](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/33043386837), without `CURSEFORGE_API_TOKEN`, passed from that exact `main` commit. Result artifact 9634702716 was named `iedct-cf-v1.1.54-reconstructed--33043386837-1--result--AUTOMATION_READY_DRY_RUN--0`; its Actions archive digest was `911E9B052FDFAA74D28841B1067474797D28BC96F44B0C918800F6CC4B391C69`. The retained report reproduced the canonical asset and deterministic request hashes without an upload; the read-only GitHub token remained available.

`MULTI_RECORD_CORRECTIVE_PR_CI: PASS`

## One guarded upload and durable processing result

Authenticated [run 33043440815](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/33043440815) ran from exact `main` commit `f29c73370e586df33c0c0036603a816c53082903`:

- all 28 then-current publisher tests passed;
- authenticated prepare passed and resolved `1.21.1` to sorted IDs 11779, 12735, and 16115;
- `Persist upload intent before any POST` succeeded;
- intent artifact 9634723269 was named `iedct-cf-v1.1.54-reconstructed--33043440815-1--intent--abb81b6e0ed9` with archive digest `79CAD9C583EAC28908EF25C1043B78899C66452E5EF83A674F94C92C3C1FC7A4`;
- `Submit exact persisted request once` issued one non-retried upload POST and received file ID 8744461;
- bounded polling ended as `UPLOADED_PROCESSING`, exit 4;
- result artifact 9634910382 was named `iedct-cf-v1.1.54-reconstructed--33043440815-1--result--UPLOADED_PROCESSING--8744461` with archive digest `42F291FAAAC9AD0E7A550661D02FA37E518A5FE4AEB9C7E230943082892211D4`.

The extracted 2,245-byte sanitized result has SHA-256 `713041D61F6359B87E14EE401D89AACEFD34F55FD5BCA21BE5078CCF59F6E615`. It records canonical asset/request hashes, the live resolved-ID set, and the positive file ID. This was an accepted upload, not yet complete public proof.

## CurseForge-token-free resume and exact public readback

[Run 33044033312](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/33044033312) supplied existing file ID 8744461 without a CurseForge token. Prepare, intent persistence, and upload were all skipped. The resume step failed before target-file validation with `CURSEFORGE_PROJECT_RELATION_MISMATCH`; result artifact 9634930848 preserved that status. No CurseForge token or POST was used; the workflow's read-only GitHub token remained available for public source verification.

Direct public readback then proved file 8744461 is approved and the public JAR is exact: 2,678,440 bytes, SHA-256 `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`, mod ID `immersive_engineer_decor_controls_tool_reforged`, version `1.1.54-reconstructed`, and exact release labels/metadata. The file/project endpoints expose only Immersive Engineering `RequiredDependency`, not the three approved `Include` relations.

## Accepted-file resume correction and blocked relation repair

Commit `06f88fbf7ed734d400067511de587249251d2a8e` keeps accepted-file verification read-only and ordered:

1. an explicit positive resume ID is validated directly, without a version-list/history scan;
2. otherwise, an exact public duplicate is authoritative before stale durable-history reconciliation;
3. a durable processing file ID skips only mutable project-level aggregation;
4. the approved baseline file and exact target-file metadata, relations, size, hash, mod ID, and version remain mandatory;
5. project-level aggregation remains mandatory whenever a new upload path is still possible.

The final suite is 29/29, Python compilation and `git diff --check` pass, actionlint 1.7.12 passes, and gitleaks scans approximately 28.87 MB with no leak. Independent code and documentation reviews pass this reduced diff.

[PR 6](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/6) was opened from `codex/curseforge-resume-after-project-drift` against exact base `f29c73370e586df33c0c0036603a816c53082903`. Its initial evidence-bearing head was `816229e69115902ce99e81da699f36bd9cd5e98d`; the final evidence-anchor follow-up and final-head CI remain pending at this snapshot.

`ACCEPTED_FILE_RESUME_PR_CI_AT_THIS_SNAPSHOT: OPEN_PENDING`

A proposed metadata-only relation repair was removed before commit or POST. Official CurseForge documentation does not accept `Include` in upload/update writes, and its public enum distinguishes `Include` from `EmbeddedLibrary`; submitting `embeddedLibrary` could produce materially different metadata. Computer Use is prohibited, so no legacy/dashboard editor was used.

`CURSEFORGE_UPLOAD_POST_COUNT: 1`

`CURSEFORGE_FILE_ID: 8744461`

`CURSEFORGE_PUBLIC_HASH_MATCH: YES`

`CURSEFORGE_PUBLICATION_STATE: BLOCKED_PUBLIC_RELATION_MISMATCH`
