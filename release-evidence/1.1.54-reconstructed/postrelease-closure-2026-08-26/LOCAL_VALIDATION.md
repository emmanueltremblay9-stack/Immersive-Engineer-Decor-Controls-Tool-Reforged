# Local validation gates

All passing Gradle gates used only this child-process option because the unmitigated Gradle daemon reproduces the diagnosed host failure:

```text
JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\AI-Work\iedct-tmp-20260826
```

The variable was scoped to each command process and restored immediately afterward.

| Gate | Exit | Result |
|---|---:|---|
| Unmitigated `gradlew.bat check --console=plain` | 1 | Failed before tasks with `java.io.IOException: Unable to establish loopback connection` |
| Process-local mitigated `gradlew.bat check --console=plain` | 0 | `BUILD SUCCESSFUL`; metadata/manual-resource checks passed |
| Process-local mitigated `gradlew.bat clean build --console=plain` | 0 | `BUILD SUCCESSFUL` |
| Process-local mitigated `gradlew.bat runGameTestServer --console=plain` | 0 | Fresh final-revision run: 182/182 required GameTests passed in 3.142 s |
| Publisher unit/mock tests with `ResourceWarning` as error | 0 | Fresh preflight-fix run: 28/28 passed |
| actionlint 1.7.12 | 0 | all workflows passed |
| gitleaks 8.30.1 working-directory scan | 0 | Fresh final-revision run: ~28.85 MB scanned; no leaks found |
| Targeted known-token/private-key prefix scan | 0 | zero matches |
| Live publisher dry run | 0 | `AUTOMATION_READY_DRY_RUN`; project/file relation baselines matched; retained report SHA-256 `5F10F226C988C6E55FE7D1FAAF7FA30C9ACE7C5D24A2DE8A608830E6DADBBD20`; no upload |
| Evidence JSON parse | 0 | all compact JSON valid |
| `git diff --check` | 0 | no whitespace errors |

The final publisher regressions specifically prove that an intent report cannot be accepted as a result, a successful persisted-intent workflow step blocks when state artifacts are missing or expired, an unexpected failure after CurseForge returns a file ID preserves that ID while producing `UPLOAD_OUTCOME_UNKNOWN`, and authenticated catalog lookup accepts multiple `1.21.1` type records only when every ID is a positive non-boolean integer, binds a sorted unique ID set, rejects missing/invalid records before POST, and retains all four upload labels.

## Build artifact

- JAR: `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.54-reconstructed.jar`
- Size: 2,678,440 bytes
- SHA-256: `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`
- Match to canonical public GitHub release asset: yes

## GameTest log

- Required tests: 182
- Passed: 182
- Failed: 0
- Fresh final-revision duration: 3.142 s
- `run/logs/latest.log` size: 14,726 bytes
- SHA-256: `5F2210993F0E7D31509CA07DD299E17015CAF0BA33B5823A818E1E2D3AD0145D`
- Java processes remaining afterward: 0

## Scanner provenance

- actionlint 1.7.12 archive SHA-256: `6E7241B51E6817EA6A047693D8E6FED13B31819C9A0DD6C5A726E1592D22F6E9`
- gitleaks 8.30.1 archive SHA-256: `D29144DEFF3A68AA93CED33DDDF84B7FDC26070ADD4AA0F4513094C8332AFC4E`
- Final gitleaks JSON report: 3 bytes (`[]` plus LF), SHA-256 `37517E5F3DC66819F61F5A7BB8ACE1921282415F10551D2DEFA5C3EB0985B570`

No build or validation command installed a mod, launched the original Prism instance, or changed a system/network setting.
