# Validation results

## Qualifying functional commit

Commit `0bda63097e61dbcfa1ffba24641a6ae87a6882b1` contains corrective commit `886010f4b07b13b68ba01f82b6f362de8ca40801` in its ancestry.

| Gate | Result | Evidence |
| --- | --- | --- |
| `tasks` and `runData` discovery | PASS | Gradle task inventory inspected; `runData` is not exposed. |
| `validateManualResources` | PASS | 223 manual entries and 424 named widgets validated. |
| `validateProjectMetadata` | PASS | Version/support/license/attribution parity validated. |
| `compileJava` | PASS | Exit 0. |
| `processResources` | PASS | Exit 0; selected non-mutating datagen/resource equivalent. |
| `runGameTestServer` | PASS | Repeated runs; 179/179 required tests, zero failures. |
| `clean build` | PASS | Exit 0; JVM `test` is `NO-SOURCE`. |
| Independent build A | PASS | JAR SHA-256 `7F357843...BFE98`. |
| Independent build B | PASS | Same byte length and SHA-256. |
| Downloaded CI JAR | PASS | Same byte length and SHA-256. |
| Client smoke | PASS | Manual, FE, structure events, Creative inventory, JEI, mining, clean shutdown. |
| Dedicated server | PASS | Mod loaded, world reached `Done (7.067s)`, clean RCON stop. |

The release-evidence/documentation commit does not alter runtime sources or packaged resources. Final qualification commands and two fresh independent builds are rerun after that commit; their exact commit and exit ledger are recorded before publication.

## Known non-failures and limits

- Audio could not be captured objectively. Door/hatch state transitions and their game-event structures were verified; `audibly observed` is `NOT_OBJECTIVELY_OBSERVABLE`.
- Two client launch attempts initially failed because of a host loopback-selector/thread environment issue. The successful runs used an isolated Windows selector profile. Those earlier crash reports are retained externally and are not represented as successful evidence.
- Two clipboard-access GL errors were caused by automated text injection during the mining redo. They are unrelated to mod loading/gameplay; the same run completed all paired mining queries and shut down cleanly.
