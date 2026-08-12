# Validation results

| Gate | Result | Evidence |
| --- | --- | --- |
| Project scan/root/version | PASS | Isolated Git clone; Java 21, NeoForge 1.21.1; version synchronized to 1.1.54. |
| `tasks` and `runData` discovery | PASS | `runGameTestServer` exists; `runData` is not exposed and is `NOT_APPLICABLE`. |
| Test-only RED | PASS | Four dedicated collision tests failed on 1.1.53 geometry; 178 unaffected tests passed. |
| First post-fix GameTest | PASS | 182/182 required tests. |
| Repeated final GameTests | PASS | Two consecutive 182/182 runs, exit 0. |
| `validateManualResources` | PASS | 223 manual sources and 424 named crafting widgets. |
| `validateProjectMetadata` | PASS | Attribution/support/version 1.1.54 synchronized. |
| `compileJava` | PASS | Latest compile exit 0. |
| `processResources` | PASS | Exit 0. |
| `clean build` | PASS | Exit 0; JVM `test` is `NO-SOURCE`. |
| `git diff --check` | PASS | Latest pre-commit check exit 0. |
| First runtime JAR readback | PASS | 2,678,440 bytes; SHA-256 `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`; 2,980 entries. |
| Two-build reproducibility | PASS | Two no-remote clones of `a38a7ae73ba0fd8c7f788d5760bf4597c3e4f32d`, both `clean build --no-build-cache`, plus qualification build: identical size, 2,980 entries, and SHA-256. |
| Prism install/traversal | PENDING | Exact 1.1.53 remains installed until final JAR qualification. |
| Dedicated server | PENDING | Awaiting final JAR. |
| CI | PENDING | Awaiting PR. |

Audio:

```text
AUDIO_USER_OBSERVED: GOOD
AUDIO_OBJECTIVE_CAPTURE: NOT_OBJECTIVELY_OBSERVABLE
```
