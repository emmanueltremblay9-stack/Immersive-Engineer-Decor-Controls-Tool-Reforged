# Release evidence summary — 1.1.54-reconstructed

> Historical qualification snapshot (2026-08-12). Its loopback and CurseForge statements, and the corresponding fields in `artifact-manifest.json`, are retained as evidence of what was known then. Current status is maintained in the [2026-08-26 post-release closure](postrelease-closure-2026-08-26/SUMMARY.md).

Qualification status on 2026-08-12: the collision correction is verified by automated tests, a reproducible canonical JAR, a packaged NeoForge server smoke, and exact Prism installation readback. GitHub publication, supersession, and Notion connector readbacks passed; CurseForge publication is blocked by missing authorized configuration.

- Artifact commit and tag target: `0d766573aeb563be3467dfb2df213e95b56f843a`.
- Trigger: the canonical `1.1.53-reconstructed` JAR produced incorrect Iron Hatch and Metal Sliding Door collision geometry in Prism LAB.
- Objective RED: 182 tests executed against the prior geometry; exactly four collision assertions failed while 178 unaffected tests passed.
- Root cause: hatch shape coordinates disagreed with its model, while sliding-door collision ignored state/model distinctions and retained decorative tracks as open movement collision.
- Final automated result: two fresh `182/182` GameTest runs passed, followed by metadata/resource validation and `clean build`, all exit 0.
- Canonical JAR: 2,678,440 bytes, 2,980 entries, SHA-256 `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`.
- CI: `main` run `31598329191`, artifact `9142044566`, conclusion `success`, same JAR hash as the fresh local build.
- Packaged server: NeoForge 21.1.230 with Immersive Engineering 12.4.2-194 reached `Done`, then stopped and saved through authenticated loopback RCON; no relevant failure or orphan process remained.
- Prism LAB: the committed post-install manifest records exactly one canonical target JAR; the other 50 JARs match the pre-install manifest by name, size, and SHA-256.
- GitHub Release: annotated tag `v1.1.54-reconstructed` targets the artifact commit; the public JAR redownload matches the canonical size and SHA-256. The 1.1.53 release is visibly marked superseded while its tag and assets were retained unchanged.
- Notion: the project page identifies 1.1.54 as current and records the same artifact, test, runtime, GitHub, CurseForge, and client-loopback boundaries.
- CurseForge: `BLOCKED_BY_MISSING_CONFIGURATION`; project ID 1555214 is known, but no API token, GitHub secret, upload workflow, Gradle publisher, or authorized comment API is configured.
- Client world entry: `BLOCKED_BY_UNRESOLVED_CLIENT_LOOPBACK_FAILURE`; `CLIENT_LOOPBACK_CAUSATION: UNRESOLVED`. The crash logs have no target-mod stack frame, while later minimal Java probes pass on the same Prism Java and JVM arguments, but no client A/B control proves or excludes target-mod causation.
- Audio: user reported it sounded good before this closure. No new objective audio capture was performed because this mission prohibited audio/video capture.
- Ruflo: `NOT_APPLICABLE`; not activated.
- Modrinth: `NOT_PERFORMED`; this mission did not authorize a new Modrinth publication path.

Full raw logs and temporary server/runtime material remain outside Git under `C:\AI-Work\iedct-release-1.1.54-closure`. This directory contains compact, non-secret evidence and hashes.
