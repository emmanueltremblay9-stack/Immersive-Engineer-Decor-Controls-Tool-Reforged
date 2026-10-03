# Validation results

Qualification evidence and later publication tooling are separate from this documentation closure. The checksum file records the verified public runtime artifact; it does not imply a binary stored in this directory.

| Gate | Result | Evidence |
| --- | --- | --- |
| Source identity | PASS | Preparation `04258c36c4a7617118404759452f5b1208f9d782`; immutable release `527b6d1afbcb283d6ff08cc40bc7a92ec229c399`; same qualified tree `963b564f803f8537fa89033a5b055a7d8e2aa72c`. Later main/docs commits are maintenance. |
| Clean qualification builds | PASS | Two builds, exit 0; identical 19,568,379 bytes and SHA-256 `fa7bde923433658a78dcf3792c9fc6af2e0b5212a7897e1b21c3323e7af809a4`. Same Windows toolchain scope. |
| Qualification validators | PASS | Localization: 120 locales / 898 keys / 223 manual pages each; manual: 223 sources / 424 widgets; metadata and sign models, exit 0. |
| Required GameTests | PASS | 189/189 in declared minimum lane; release-commit Build 37074896351 passed. Higher dependency-version lanes are not new qualification. |
| Mocked publisher tests | PASS | Tagged-release qualification 31/31; current post-release PR #31 tooling 32/32; post-PR31 Build 37086900977 passed. |
| GitHub publication | PASS | Release 402228962 / asset 606766184; public, non-draft/non-prerelease; API digest and anonymous download match canonical bytes. |
| CurseForge publication | PASS | Project 1555214 / file 9042960; approved/public status 4, release type 1; correct four labels and sole IE231951 RequiredDependency. Anonymous bytes equal GitHub. |
| Upload/recovery | PASS | Dry-run 37086906569 success; production37087128090 one POST, workflow failure/UPLOADED_PROCESSING/exit4; tokenless resume37087857213 success/RESUMED_PUBLICATION_VERIFIED/exit0/no POST. Real intent/result artifacts retained externally. |
| Evidence package counts | PASS | GitHub-stage package 89 verified checksums; later CurseForge package 121. Separate counts and provenance, not publisher-test totals. |
| Modrinth | BLOCKED | BLOCKED_BY_MISSING_CONFIGURATION; no mutation. |
| Linguistic/visual coverage | UNVERIFIED | Native fluency unavailable; exhaustive visual/UI-scale/per-frame binding not established; sampled Hindi/Arabic/Thai limitations retain unverified component attribution. |
| New packaged runtime / Prism | NOT_PERFORMED | No new client launch, installation or packaged dedicated-server qualification. Historical runtime results do not establish 1.1.56 behavior. |

This closure changes documentation/readbacks only. Local JSON/checksum/reference validation and exact PR CI belong to the external postpublication closure package; no new local runtime build is claimed here. Public release/tag/artifact and historical release records remain unchanged.
