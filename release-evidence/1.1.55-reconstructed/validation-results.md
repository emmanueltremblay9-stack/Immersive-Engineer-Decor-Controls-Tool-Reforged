# Validation results

| Gate | Result | Evidence |
| --- | --- | --- |
| Source authority | PASS | Isolated release branch based on verified public `main` commit `5e03b3e66d700abf1745a802a8ba0bd879511cf3`; release-candidate commit `7de4cc704272a7bc5b1877333c06f43ba8ff893a`. |
| Version synchronization | PASS | `gradle.properties`, NeoForge metadata, changelog, release notes, bundles, workflow, and new publisher manifest use `1.1.55-reconstructed`; historical 1.1.54 evidence remains untouched. |
| `validateManualResources` | PASS | Exit 0; 223 manual sources and 424 named crafting widgets. |
| `validateProjectMetadata` | PASS | Exit 0; attribution, support metadata, and release version 1.1.55 validated. |
| `validateSignItemModels` | PASS | Exit 0; flat parents, front textures, absent elements, GUI lighting, and transforms validated for both sign items. |
| Publisher unit suite | PASS | 29/29 tests, exit 0. |
| Clean builds / reproducibility | PASS | Three clean builds, each exit 0; every JAR is 2,689,752 bytes with SHA-256 `956FC45E...A63167C`. |
| Lane A: no JEI | PASS | NeoForge 21.1.230, IE 12.4.2-194, JEI absent; exact mod list observed; 188/188 required GameTests, exit 0. |
| Lane B: declared baseline | PASS | NeoForge 21.1.230, IE 12.4.2-194, JEI 19.32.0.359; exact mod list observed; 188/188 required GameTests, exit 0. |
| Lane C: reporter versions | PASS | NeoForge 21.1.248, IE 12.4.2-194, JEI 19.44.0.406; exact mod list observed; 188/188 required GameTests, exit 0. |
| JAR inspection | PASS | 2,984 entries; 1.1.55 manifest/NeoForge metadata, four new runtime block classes, and two corrected sign item models present. |
| Dedicated server | PASS | Baseline lane reached `Done (0.522s)`, accepted `stop`, saved all dimensions, and returned exit 0. |
| Patched-client inventory and JEI visual screenshots | NOT_PERFORMED | GUI-control surface could not automate the Minecraft client; no visual-runtime result is claimed. |
| CI final head | PENDING | Requires the release PR head to exist remotely. |
| Prism installation | PASS-WITH-GAP | Exact 1.1.55 JAR installed; size/hash/mod id/version/feature entries match and one matching JAR remains. Full unrelated-JAR pre/post hash comparison was interrupted before its post snapshot; the install report records only the identified 1.1.54 JAR as deleted. |
| GitHub release/readback | NOT_PERFORMED | Publication is downstream of final-head CI and merge. |
| CurseForge publication/readback | NOT_PERFORMED | New hash-pinned manifest is prepared; dry-run and upload require the GitHub release to exist. |
| Modrinth publication | BLOCKED | `BLOCKED_BY_MISSING_CONFIGURATION`; no project or dependency identifiers are guessed. |

Warnings about command ambiguity, asset URL schemes, dependency override differences between matrix lanes, offline test-server mode, and deprecated/unchecked compilation APIs were observed. None caused a required test, build, load, save, or validator failure.
