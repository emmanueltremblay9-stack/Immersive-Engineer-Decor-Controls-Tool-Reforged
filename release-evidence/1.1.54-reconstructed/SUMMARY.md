# Release evidence summary — 1.1.54-reconstructed

Qualification status on 2026-08-12: the collision correction is verified by automated tests, a reproducible canonical JAR, a packaged NeoForge server smoke, and exact Prism installation readback. Public-release and connector readbacks are recorded separately and must not be inferred from this engineering summary.

- Artifact commit and tag target: `0d766573aeb563be3467dfb2df213e95b56f843a`.
- Trigger: the canonical `1.1.53-reconstructed` JAR produced incorrect Iron Hatch and Metal Sliding Door collision geometry in Prism LAB.
- Objective RED: 182 tests executed against the prior geometry; exactly four collision assertions failed while 178 unaffected tests passed.
- Root cause: hatch shape coordinates disagreed with its model, while sliding-door collision ignored state/model distinctions and retained decorative tracks as open movement collision.
- Final automated result: two fresh `182/182` GameTest runs passed, followed by metadata/resource validation and `clean build`, all exit 0.
- Canonical JAR: 2,678,440 bytes, 2,980 entries, SHA-256 `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`.
- CI: `main` run `31598329191`, artifact `9142044566`, conclusion `success`, same JAR hash as the fresh local build.
- Packaged server: NeoForge 21.1.230 with Immersive Engineering 12.4.2-194 reached `Done`, then stopped and saved through authenticated loopback RCON; no relevant failure or orphan process remained.
- Prism LAB: exactly one target JAR is installed and matches the canonical hash; the other 50 JARs match the pre-install manifest by name, size, and SHA-256.
- Client world entry: `BLOCKED_BY_UNRESOLVED_TRANSIENT_LOOPBACK`. The two crash logs contain no target-mod stack frame, while later minimal Java probes pass on the same Prism Java and JVM arguments. This supports, but does not prove, independence from the mod.
- Audio: user reported it sounded good before this closure. No new objective audio capture was performed because this mission prohibited audio/video capture.
- Ruflo: `NOT_APPLICABLE`; not activated.
- Modrinth: `NOT_PERFORMED`; this mission did not authorize a new Modrinth publication path.

Full raw logs and temporary server/runtime material remain outside Git under `C:\AI-Work\iedct-release-1.1.54-closure`. This directory contains compact, non-secret evidence and hashes.
