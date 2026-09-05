# 1.1.55-reconstructed qualification and publication summary

Release-candidate source commit: `7de4cc704272a7bc5b1877333c06f43ba8ff893a`

Release tag commit: `a2c2eff1bbe924354133ef38aa8705fc7c8255b3`

Publication-automation hotfix commit: `13f8464515c622592d6e193fb9ab01ea3a73f567`

Canonical runtime JAR: `immersive_engineer_decor_controls_tool_reforged-1.1.55-reconstructed.jar`

Canonical size/SHA-256: `2,689,752` bytes / `956FC45E04675427AB98A79BB82F22E28F55E7D13EBDCD54F86260515A63167C`

## Qualified locally

- Issues #8 and #9: one obsolete generic support-removal GameTest was replaced with seven targeted required regression tests; the combined required suite increased from 182 to 188.
- Issue #10: deterministic item-model validator passes for both sign items; world blockstate/model resources remain outside the change.
- Three GameTest lanes pass 188/188: minimum without JEI, minimum with JEI, and the reporter versions.
- Manual, project-metadata, and sign-model validators pass.
- Three fresh clean builds produce the same runtime JAR size and SHA-256.
- JAR metadata, version, dependency ranges, changed classes, and sign resources pass direct inspection.
- A development `runServer` smoke reaches `Done`, accepts `stop`, saves all dimensions, and returns Gradle exit status 0.
- The exact canonical JAR is installed in Prism LAB with matching size, SHA-256, mod id, version, and changed-feature entries; exactly one matching JAR remains.

## Published and closed

- Final release-commit CI run `33937057791` passed at `a2c2eff1bbe924354133ef38aa8705fc7c8255b3`.
- GitHub release `v1.1.55-reconstructed` is public, non-draft, and non-prerelease. Its downloaded asset is 2,689,752 bytes and matches the canonical SHA-256.
- CurseForge file `8810946` is public for client and server, Minecraft 1.21.1, and NeoForge. Its downloaded JAR matches the canonical size and SHA-256.
- CurseForge exposes exactly one project relation: Immersive Engineering (`231951`) as `RequiredDependency`.
- The initial CurseForge publication run `33938108941` made the single upload POST and reached `UPLOADED_PROCESSING`; resume run `33938637770` performed no POST and completed with `RESUMED_PUBLICATION_VERIFIED`.
- The cross-tag durable-state false positive found during publication was corrected in PR #14. The publisher suite now passes 31/31 and post-merge CI run `33937942443` passed.
- GitHub issues #8, #9, and #10 were closed as `completed` only after merged-code, qualification, release, and publication evidence existed.

## Evidence boundary

- Patched-client inventory and JEI visual screenshots are `BLOCKED_BY_USER_INTERRUPTED_COMPUTER_USE`; the disposable client and target mod reached resource reload, but no world-join or admissible visual evidence was captured. This is neither a product failure nor a visual PASS.
- The first dedicated-server launch reached `Done` but its piped stop command was rejected by PowerShell parameter binding. The exact test was repeated with a PTY and passed cleanly; the failed control-channel attempt is retained in `dedicated-server-smoke.md`.
- The original 1.1.55 installation's full unrelated-Prism-JAR pre/post comparison remains `NOT_PERFORMED` and cannot be recovered retroactively because its pre-install baseline is unavailable. A distinct fresh postpublication comparison now passes for all 50 unrelated JARs and the canonical target; see `postinterruption-closure-2026-09-05/SUMMARY.md`.
- Modrinth publication is `BLOCKED_BY_MISSING_CONFIGURATION`; no project or dependency identifiers were guessed.
- The previous loopback incident is currently classified `SHARED_ENVIRONMENT_FAILURE` / `HOST_TEMP_DIRECTORY_AF_UNIX_CONNECT_FAILURE`, not an unresolved mod-runtime defect. Historical evidence remains unchanged.
- The historical `release-evidence/1.1.54-reconstructed` tree was not modified.

## Dependency interpretation

Engineer's Decor, Engineer's Tools, and Redstone Gauges and Switches are incorporated source projects, not dependencies to install. Immersive Engineering is the sole required project dependency. JEI remains optional NeoForge integration metadata and is intentionally absent from CurseForge upload relations.
