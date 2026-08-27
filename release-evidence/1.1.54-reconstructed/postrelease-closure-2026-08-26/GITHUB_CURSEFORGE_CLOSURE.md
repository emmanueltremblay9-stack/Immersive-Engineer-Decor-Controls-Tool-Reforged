# GitHub integration and guarded CurseForge attempt

## Integrated implementation

- PR: [#3](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/3)
- PR head: `e18d155490f49b3a658363e013c186c3c1b5300c`
- Merged: 2026-08-27T05:00:36Z
- Merge commit / then-current `main`: `1b3d4db1ae2215deb754ed8c37ce9352aa428c67`
- Build runs 33040937740 and 33040964379: `SUCCESS`

The merged secret-free publisher dry run, [run 33041164651](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/33041164651), completed successfully. Result artifact 9633912553 was named `iedct-cf-v1.1.54-reconstructed--33041164651-1--result--AUTOMATION_READY_DRY_RUN--0`; its Actions archive digest was `aa4109564d0511c971df04897d43a18f49621147c9a6593e5e330924634f02ab`. The extracted report was 1,904 bytes with SHA-256 `5F10F226C988C6E55FE7D1FAAF7FA30C9ACE7C5D24A2DE8A608830E6DADBBD20` and reproduced metadata hash `14886AF9E71407D12E664B853A21EF221715CD37349EA0B45A6BBC3981EFBE43`, multipart hash `ABB81B6E0ED9B4586306ACE4B4E167E1C51C2631CF9B1ED4F24F6248F1271597`, and size 2,681,730. No CurseForge token or upload was used.

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
- live secret-free dry run: `AUTOMATION_READY_DRY_RUN`, exit 0
- gitleaks 8.30.1: exit 0, approximately 28.85 MB scanned, no leaks
- metadata/multipart hashes and multipart size: unchanged
- independent code review: PASS

`CORRECTIVE_PR_CI: PENDING`

`CURSEFORGE_UPLOAD_POST_ATTEMPTED: NO`

`CURSEFORGE_PUBLICATION_STATE: NOT_PERFORMED_SAFE_PREFLIGHT_STOP`
