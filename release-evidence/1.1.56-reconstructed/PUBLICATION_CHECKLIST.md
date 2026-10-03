# Completed publication/readback checklist for 1.1.56-reconstructed

GitHub and CurseForge publication are complete. This checklist closes the former preparation scaffold; detailed identities and retained receipts are in [SUMMARY.md](SUMMARY.md).

- [x] Preparation PR #30 reviewed and merged before publication; immutable tag `v1.1.56-reconstructed` targets `527b6d1afbcb283d6ff08cc40bc7a92ec229c399`, qualified tree `963b564f803f8537fa89033a5b055a7d8e2aa72c`. Later main/publication-tooling/docs commits are maintenance only.
- [x] Canonical JAR name, mod ID, version, 19,568,379-byte size, 29,748 entries and SHA-256 `fa7bde923433658a78dcf3792c9fc6af2e0b5212a7897e1b21c3323e7af809a4` verified by redownload/ZIP inspection.
- [x] GitHub release `402228962` and asset `606766184` public, non-draft/non-prerelease; release body/tag/asset preserved.
- [x] Post-release PR #31 binds the publisher to 1.1.56; current mocked suite 32/32, distinct from qualification 31/31. Merged dry-run passed.
- [x] Actual intent persisted before the sole upload POST; positive file ID `9042960` durably recorded. Initial `UPLOADED_PROCESSING`/exit 4 retained; tokenless resume skipped upload steps, made no second POST and passed public verification.
- [x] CurseForge project `1555214`, file `9042960` approved/public; correct Client/Server/Minecraft 1.21.1/NeoForge labels and release type. Anonymous JAR bytes equal GitHub exactly; exactly one current 1.1.56 file.
- [x] Sole `RequiredDependency`: Immersive Engineering project `231951`. Incorporated projects remain attribution in CREDITS.md/NOTICE.md; no Include relation required. JEI optional.
- [x] Duplicate detection, immutable intent, unknown-outcome stop and tokenless accepted-ID recovery guards retained; historical release evidence unchanged.
- [ ] Modrinth remains `BLOCKED_BY_MISSING_CONFIGURATION`; no submission or mutation authorized/performed.
- [ ] Native fluency, exhaustive visual coverage, arbitrary UI scales and exact Hindi/Arabic/Thai component attribution remain unresolved.
- [ ] New client launch, Prism installation, packaged dedicated-server and higher-version dependency lanes remain `NOT_PERFORMED` for 1.1.56. Build/GameTests/public artifact verification do not establish these results.
