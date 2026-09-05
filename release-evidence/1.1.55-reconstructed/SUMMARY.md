# 1.1.55-reconstructed qualification summary

Release-candidate source commit: `7de4cc704272a7bc5b1877333c06f43ba8ff893a`

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

## Evidence boundary

- Patched-client inventory and JEI visual screenshots are `NOT_PERFORMED`; no visual-runtime result is claimed.
- The first dedicated-server launch reached `Done` but its piped stop command was rejected by PowerShell parameter binding. The exact test was repeated with a PTY and passed cleanly; the failed control-channel attempt is retained in `dedicated-server-smoke.md`.
- Final-head CI, GitHub release, CurseForge publication, and public readbacks are recorded only after those states exist. None is claimed by this prepublication snapshot.
- The historical `release-evidence/1.1.54-reconstructed` tree was not modified.

## Dependency interpretation

Engineer's Decor, Engineer's Tools, and Redstone Gauges and Switches are incorporated source projects, not dependencies to install. Immersive Engineering is the sole required project dependency. JEI remains optional NeoForge integration metadata and is intentionally absent from CurseForge upload relations.
