# Post-release closure — 1.1.54-reconstructed

This directory is the authoritative entry point for the client-loopback diagnosis and guarded CurseForge publication work begun on 2026-08-26. The parent evidence directory remains the immutable 2026-08-12 release-qualification snapshot.

## Engineering decision

- `CLIENT_LOOPBACK_VERDICT: SHARED_ENVIRONMENT_FAILURE`
- `CLIENT_LOOPBACK_CAUSATION: HOST_TEMP_DIRECTORY_AF_UNIX_CONNECT_FAILURE`
- Unmitigated controlled runs: 6/6 reproduced the exact loopback signature across no target mod, 1.1.53, and 1.1.54.
- Process-local mitigated runs: 6/6 entered the neutral world and remained stable for the 15-second observation interval.
- The process-local discriminator was `-Djdk.net.unixdomain.tmpdir=C:\AI-Work\iedct-tmp-20260826`.
- The deeper Windows reason for rejecting AF_UNIX connects under the user's current temp path remains `UNVERIFIED`.
- `SYSTEM_NETWORK_CHANGES: NONE`.
- The evidence does not satisfy `TARGET_MOD_CAUSAL`; no target source change, version 1.1.55 build, tag, or release was created.

## CurseForge closure state

- Project: 1555214.
- Canonical source: the existing public GitHub `v1.1.54-reconstructed` JAR, 2,678,440 bytes, SHA-256 `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`.
- Guarded publisher tests: 28/28 passed locally, including the catalog-backed game-version lookup regression.
- The GitHub Actions secret name `CURSEFORGE_API_TOKEN` exists; its value was never read or printed. The user confirmed it contains a newly generated replacement for the token previously exposed in chat.
- The final workflow separates secret-free dry-run from authenticated prepare/publish, keeps intent and result reports physically separate, persists an exact upload intent before the one non-retried POST, and uses workflow-run/job metadata—while GitHub keeps it queryable—to block when result artifacts are missing or expired.
- PR 3 merged the guarded workflow to `main` at `1b3d4db1ae2215deb754ed8c37ce9352aa428c67`; both Build checks and the first merged secret-free dry run passed.
- The first authenticated workflow run stopped during prepare with `CURSEFORGE_GAME_VERSION_UNRESOLVED`. Intent persistence and upload steps were skipped, the run produced no artifact, and public state remained without a 1.1.54 file. The corrective implementation is commit `0f2117d1153d8122ca371f0a3478761fa7c89789`, pending PR/CI.
- Real-publication status and file ID will be recorded here only after public metadata, dependency, size, JAR identity, and SHA-256 readbacks pass.

## Proof taxonomy

1. Unit/mock proof validates deterministic bytes, the documented `relations.projects` request schema, zero-based public pagination, durable intent handling, no automatic POST retry, ambiguous-outcome blocking, resume behavior, and secret non-disclosure.
2. Live dry-run proof validates the public GitHub source and current public CurseForge baseline without a CurseForge token or upload.
3. An accepted upload response proves only that CurseForge returned a file ID; it is not public-release proof.
4. Public-release proof requires a public file readback, all expected game-version labels and relations, approved status, exact size, exact SHA-256, and embedded NeoForge mod ID/version.

The upload API supports only the explicit Immersive Engineering `requiredDependency` relation used here. The three existing public `Include` relations cannot be transmitted by that API contract. Live preflight confirms all three are currently project-level dependencies and appear on each of the four newest public files, but their appearance on the new file remains unverified until real post-upload readback.

## Preservation

- The original dirty worktree remained read only and matched all preflight Git/config hashes at postflight.
- The original Prism LAB instance matched its preflight configuration and target-JAR hashes.
- No Computer Use, OBS, Game Bar, screenshot, OCR, audio, or video capture was used.
- No global Java, registry, firewall, Winsock, TCP, environment, or account setting was changed.

## Evidence map

- Client decision: `CLIENT_LOOPBACK_CAUSALITY.md`
- Controlled matrix: `PRISM_AB_MANIFEST.md`, `PRISM_AB_RUNS.md`, `PRISM_AB_RESULTS.json`
- Java/JVM probes: `LOOPBACK_PROBES.md`, `JAVA_JVM_MANIFEST.md`
- Historical crash and static target audit: `CRASH_AUDIT.md`
- Publisher design and publication state: `CURSEFORGE_AUTOMATION.md`, `CURSEFORGE_PUBLICATION_MANIFEST.json`
- GitHub integration and guarded-attempt chronology: `GITHUB_CURSEFORGE_CLOSURE.md`
- Local build/test/scanner gates: `LOCAL_VALIDATION.md`
- Original and Prism preservation: `PRESERVATION_POSTFLIGHT.md`, `PRISM_INVENTORY.md`
- Sanitized commands: `COMMAND_LEDGER.md`

`CLOSURE_STATUS: IN_PROGRESS_PENDING_INTEGRATION_AND_PUBLIC_READBACK`
