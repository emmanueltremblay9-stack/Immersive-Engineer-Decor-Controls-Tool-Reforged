# Validation results

| Gate | Result | Evidence |
| --- | --- | --- |
| Git/source authority | PASS | Clean isolated `main` at merge commit `0d766573`; original dirty worktree fingerprinted separately. |
| `validateManualResources` | PASS | Exit 0; 223 manual sources and 424 named crafting widgets. |
| `validateProjectMetadata` | PASS | Exit 0; attribution, support, and 1.1.54 metadata synchronized. |
| `compileJava` | PASS | Exit 0. |
| `processResources` | PASS | Exit 0. |
| GameTest pass 1 | PASS | 182/182 required tests, 3.615 seconds, exit 0. |
| GameTest pass 2 | PASS | 182/182 required tests, 3.593 seconds, exit 0. |
| `clean build` | PASS | Exit 0; canonical local JAR produced. |
| `git diff --check` | PASS | Exit 0. |
| CI provenance | PASS | `main` run 31598329191 at `0d766573`, artifact 9142044566, conclusion success. |
| CI/local equality | PASS | Both JARs are 2,678,440 bytes and SHA-256 `80F6FF...0E29`. |
| JAR metadata/resources | PASS | 2,980 entries; mod id/version/license/dependencies and changed classes/resources present. |
| Targeted non-regressions | PASS | Default creative routing, conventional mining, iron sound families, solar FE conservation, manual resources, and JEI wiring are covered structurally/GameTest. |
| Packaged dedicated server | PASS | Exact JAR reached `Done`; controlled RCON stop; clean save/exit; no relevant failure/orphan. |
| Prism installation | PASS | Committed post-install manifest: one exact target JAR, canonical hash, and 50 unrelated JARs unchanged. |
| Prism client world entry | BLOCKED | `BLOCKED_BY_UNRESOLVED_CLIENT_LOOPBACK_FAILURE`; causation remains unresolved and no visual interaction was allowed. |
| Computer Use | NOT_PERFORMED | Explicitly prohibited and not invoked. |

Fresh validation ledger SHA-256: `4F669BE86C6757C2A31D20AF127837968055B52C340D5B5150CF70E56A120B1F`.

Audio status:

```text
AUDIO_USER_OBSERVED: GOOD (prior user report)
AUDIO_OBJECTIVE_CAPTURE: NOT_PERFORMED
```
