# Validation results

| Gate | Result | Evidence |
| --- | --- | --- |
| Source authority | PASS | Isolated release branch based on verified public `main` commit `5e03b3e66d700abf1745a802a8ba0bd879511cf3`; release-candidate commit `7de4cc704272a7bc5b1877333c06f43ba8ff893a`; release tag commit `a2c2eff1bbe924354133ef38aa8705fc7c8255b3`. |
| Version synchronization | PASS | `gradle.properties`, NeoForge metadata, changelog, release notes, bundles, workflow, and new publisher manifest use `1.1.55-reconstructed`; historical 1.1.54 evidence remains untouched. |
| `validateManualResources` | PASS | Exit 0; 223 manual sources and 424 named crafting widgets. |
| `validateProjectMetadata` | PASS | Exit 0; attribution, support metadata, and release version 1.1.55 validated. |
| `validateSignItemModels` | PASS | Exit 0; flat parents, front textures, absent elements, GUI lighting, and transforms validated for both sign items. |
| Publisher unit suite | PASS | Release-candidate suite: 29/29, exit 0. After the cross-tag durable-state fix in PR #14: 31/31, exit 0. |
| Clean builds / reproducibility | PASS | Three clean builds, each exit 0; every JAR is 2,689,752 bytes with SHA-256 `956FC45E...A63167C`. |
| Lane A: no JEI | PASS | NeoForge 21.1.230, IE 12.4.2-194, JEI absent; exact mod list observed; 188/188 required GameTests, exit 0. |
| Lane B: declared baseline | PASS | NeoForge 21.1.230, IE 12.4.2-194, JEI 19.32.0.359; exact mod list observed; 188/188 required GameTests, exit 0. |
| Lane C: reporter versions | PASS | NeoForge 21.1.248, IE 12.4.2-194, JEI 19.44.0.406; exact mod list observed; 188/188 required GameTests, exit 0. |
| JAR inspection | PASS | 2,984 entries; 1.1.55 manifest/NeoForge metadata, four new runtime block classes, and two corrected sign item models present. |
| Dedicated server | PASS | Baseline lane reached `Done (0.522s)`, accepted `stop`, saved all dimensions, and returned exit 0. |
| Patched-client inventory and JEI visual screenshots | BLOCKED | `BLOCKED_BY_USER_INTERRUPTED_COMPUTER_USE`; the disposable client and target mod reached resource reload, but no world-join or admissible screenshots were captured. This is neither a product failure nor a visual PASS. |
| CI final release commit | PASS | Run `33937057791` completed successfully at exact tag commit `a2c2eff1bbe924354133ef38aa8705fc7c8255b3`; its downloaded runtime JAR matches the canonical SHA-256. |
| Publication-automation hotfix CI | PASS | PR #14 fixed cross-tag persisted-intent scoping; post-merge run `33937942443` completed successfully at `13f8464515c622592d6e193fb9ab01ea3a73f567`, including 31/31 publisher tests and 188 GameTests. |
| Prism installation | PASS-WITH-GAP | Exact 1.1.55 JAR installed; size/hash/mod id/version/feature entries match and one matching JAR remains. Original release-install exhaustive comparison remains `NOT_PERFORMED` because no contemporaneous full pre-install inventory is available; the install report records only the identified 1.1.54 JAR as deleted. |
| Fresh postpublication Prism preservation | PASS | Independent comparison of `postinterruption-closure-2026-09-05/prism-pre.json` and `prism-post.json`: 50 unrelated JARs unchanged by filename, size, and SHA-256; exactly one canonical target JAR; current LAB still matches the post manifest. This compensating proof does not replace the missing original pre-install baseline. |
| GitHub release/readback | PASS | Release `383098584`, tag `v1.1.55-reconstructed`, public non-draft/non-prerelease; asset `545205479`, 2,689,752 bytes; API digest and independent download SHA-256 match the canonical JAR. |
| CurseForge publication/readback | PASS | Project `1555214`, file `8810946`, public status `4`, release type `1`; client/server, Minecraft 1.21.1, NeoForge; downloaded size/SHA-256 match. Exactly one relation exists: Immersive Engineering (`231951`) as `RequiredDependency`. |
| CurseForge POST/resume safety | PASS | Run `33938108941` made the single POST and recorded `UPLOADED_PROCESSING`; run `33938637770` resumed file `8810946` without a token, prepare step, intent persist, or POST and returned `RESUMED_PUBLICATION_VERIFIED`. |
| Issue closure | PASS | Issues #8, #9, and #10 read back `closed` with state reason `completed` after publication evidence existed. |
| Modrinth publication | BLOCKED | `BLOCKED_BY_MISSING_CONFIGURATION`; no project or dependency identifiers are guessed. |
| Prior loopback classification | SHARED_ENVIRONMENT_FAILURE | Current diagnosis is `HOST_TEMP_DIRECTORY_AF_UNIX_CONNECT_FAILURE`; it is not carried forward as an unresolved mod-runtime defect. Historical snapshots remain unchanged. |

Warnings about command ambiguity, asset URL schemes, dependency override differences between matrix lanes, offline test-server mode, and deprecated/unchecked compilation APIs were observed. None caused a required test, build, load, save, or validator failure.
