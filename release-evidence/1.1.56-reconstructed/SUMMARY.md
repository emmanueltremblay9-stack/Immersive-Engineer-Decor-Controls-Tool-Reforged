# 1.1.56-reconstructed qualification and publication summary

Preparation source commit: `04258c36c4a7617118404759452f5b1208f9d782` (from main `8e8794624db0cf6ccc0be5397682b8c9ce6080f2`).

Release-preparation [PR #30](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/30) merge / immutable release tag commit: `527b6d1afbcb283d6ff08cc40bc7a92ec229c399`.

Qualified source tree shared by preparation and release commits: `963b564f803f8537fa89033a5b055a7d8e2aa72c`.

Later publication-tooling [PR #31](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/31) merge / pre-closure main: `c428f3be625a538a34abd30e340f61ef2903ac73`. The later documentation closure on `main` is maintenance state, not the source of the already published JAR.

Canonical runtime JAR: `immersive_engineer_decor_controls_tool_reforged-1.1.56-reconstructed.jar` — 19,568,379 bytes, 29,748 ZIP entries; SHA-256 `fa7bde923433658a78dcf3792c9fc6af2e0b5212a7897e1b21c3323e7af809a4`. [SHA256SUMS.txt](SHA256SUMS.txt) records this verified public artifact identity; no binary is committed here.

## Qualification

- Two clean builds without build cache passed; the second bound the exact clean preparation commit/tree. Both produced the canonical bytes. Same Windows toolchain reproducibility was proven; no general cross-host certification is claimed.
- All 189 required GameTests passed in the declared NeoForge 21.1.230 / IE 12.4.2-194 / optional JEI 19.32.0.359 qualification lane.
- Localization (120 locales, 898 keys and 223 manual pages each), manual resources (223 sources, 424 widgets), metadata and sign-model validators passed.
- The qualification publisher suite passed **31/31** mocked tests. Post-release tooling after PR #31 separately passes **32/32**; this does not rewrite tagged-release qualification.
- Exact release-commit [Build 37074896351](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/37074896351) passed; later publication-tooling [Build 37086900977](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/37086900977) passed with 189 GameTests, 32 publisher tests and the canonical runtime SHA.

## Publication and readback

- [GitHub release 402228962](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/releases/tag/v1.1.56-reconstructed): tag `v1.1.56-reconstructed`, asset `606766184`, public/non-draft/non-prerelease. Published `2026-10-03T00:16:21Z` (2026-10-02 America/Toronto). API digest and anonymous downloaded size/SHA match.
- [CurseForge project 1555214/file 9042960](https://www.curseforge.com/minecraft/mc-mods/immersive-engineers-decor-controls-tools-reforged/files/9042960): approved/public, status `4`, release type `1`, Client/Server/Minecraft 1.21.1/NeoForge. Downloaded JAR is fully byte-identical to GitHub.
- Exactly one relation: Immersive Engineering (`231951`) as `RequiredDependency`. JEI is optional; incorporated source projects are attribution covered by `CREDITS.md` and `NOTICE.md`, not runtime dependencies or required Include relations.
- [Dry-run 37086906569](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/37086906569) passed `AUTOMATION_READY_DRY_RUN`. [Production 37087128090](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/37087128090) made **one POST**, persisted positive file ID `9042960`, and ended workflow failure / `UPLOADED_PROCESSING` / exit `4`.
- [Tokenless resume 37087857213](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/37087857213) passed `RESUMED_PUBLICATION_VERIFIED` / exit `0`, with prepare/persist/submit skipped and zero POSTs. The actual intent artifact `11260822153` preceded the sole submit; production result `11261197546` and resume result `11261077915` retain the request/file/hash binding. No unknown outcome or duplicate POST.
- Compact [GitHub readback](github-release-readback.json), [CurseForge readback](curseforge-publication-readback.json), [validation results](validation-results.md) and [completed checklist](PUBLICATION_CHECKLIST.md) retain the verified identities and boundary.

## Retained evidence and limitations

External package labels: `release-candidate-prep-evidence-20261001`, `release-candidate-final-review-evidence-20261002`, `release-merge-evidence-20261002`, `release-publication-evidence-20261002`, and `curseforge-1.1.56-enable-and-publish-evidence-20261002`. Full command receipts, manifests, logs, artifact archives and binaries remain external. The GitHub-stage package verified 89 checksums; the later CurseForge package verified 121. These are separate package counts, not test totals or a combined closure count.

- Modrinth remains `BLOCKED_BY_MISSING_CONFIGURATION`; no guessed identifiers or mutation.
- Native fluency is unavailable; exhaustive 120-locale visual coverage, arbitrary UI-scale tooltip geometry and per-frame artifact binding remain unverified. Earlier sampled shared Hindi/Arabic/Thai rendering limitations remain confirmed, with exact component attribution unverified.
- No new 1.1.56 client launch, Prism installation, packaged dedicated-server or higher dependency-version qualification is claimed. Historical 1.1.55 runtime evidence is not promoted into this release.
- Historical evidence and original dirty checkout are preserved. This postpublication record does not modify the public tag, release body, asset or CurseForge file.
