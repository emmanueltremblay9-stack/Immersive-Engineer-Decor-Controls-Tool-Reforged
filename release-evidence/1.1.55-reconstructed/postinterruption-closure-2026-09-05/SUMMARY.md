# Post-interruption residual-evidence closure — 1.1.55-reconstructed

This addendum records the bounded reconciliation performed after the user stopped the disposable client-visual run. It is evidence-only: Computer Use was not resumed, and no mod source, resource, version, build, release tag, GitHub Release, CurseForge file, issue, or Modrinth project was changed.

The canonical runtime artifact remains `immersive_engineer_decor_controls_tool_reforged-1.1.55-reconstructed.jar`, 2,689,752 bytes, SHA-256 `956FC45E04675427AB98A79BB82F22E28F55E7D13EBDCD54F86260515A63167C`.

## Interrupted process and disposable-instance postflight

- At `2026-09-05T04:49:54.6520692Z`, the historical Java PID `9976` and Prism PID `9452` were both absent. No Java, Javaw, or Prism Launcher process was running, and zero unrelated processes were terminated.
- The disposable instance `IEDCT-VISUAL-115-20260904` matched the controlled base template: 50 non-target JARs across its active and disabled sets, with no missing, added, or changed base JAR. It contained only the controlled `neutral-world`, zero screenshots, one canonical target JAR, and copied logs/configuration evidence.
- The client process had launched and target-mod initialization reached resource reload, but the log contains no world-join marker and no admissible inventory or JEI screenshot was captured before the user's interruption. This is not a product failure and is not visual proof.
- The instance's unique evidence was copied before cleanup. Recursive deletion was blocked before execution by tool policy, so the safe fallback moved the instance and its empty temporary directory out of Prism into the recoverable quarantine at `C:\AI-Work\iedct-residual-gaps-20260904-232647\quarantine`. The original Prism instance path and temporary path are absent; the quarantined instance contains 346 files totaling 508,237,710 bytes. Nothing was deleted.

Status:

```text
DISPOSABLE_MINECRAFT_PROCESS: ALREADY_EXITED
PRISM_GENERAL_PROCESS: NOT_RUNNING
UNRELATED_PROCESSES_TERMINATED: 0
DISPOSABLE_INSTANCE_STATUS: QUARANTINED_RECOVERABLY_AFTER_EVIDENCE_COPY
VISUAL_QA_VERDICT: BLOCKED_BY_USER_INTERRUPTED_COMPUTER_USE
```

## Fresh postpublication Prism preservation

The copied pre/post manifests were independently rehashed and recomputed by filename, byte size, and SHA-256. The current Prism LAB inventory was also compared to the post manifest.

| Assertion | Result |
| --- | --- |
| Pre/post total JARs | 51 / 51 |
| Pre/post unrelated JARs | 50 / 50 |
| Missing / added / changed unrelated JARs | 0 / 0 / 0 |
| Target JAR count | 1 |
| Target size | 2,689,752 bytes |
| Target SHA-256 | `956FC45E04675427AB98A79BB82F22E28F55E7D13EBDCD54F86260515A63167C` |
| Recorded comparison agrees with independent recomputation | yes |
| Current Prism LAB agrees with the post manifest | 51/51; no divergence |

```text
FRESH_POSTPUBLICATION_EXHAUSTIVE_PRESERVATION: PASS
FRESH_PRISM_PRESERVATION_VERDICT: PASS
```

This compensating proof does not rewrite history. No contemporaneous full pre-install inventory exists for the original 1.1.55 installation:

```text
ORIGINAL_1_1_55_INSTALL_EXHAUSTIVE_PRESERVATION: NOT_PERFORMED_AND_NOT_RETROACTIVELY_RECOVERABLE
ORIGINAL_PRISM_COMPARISON_VERDICT: BLOCKED_BY_MISSING_PREINSTALL_BASELINE
```

## Public artifact and optional-channel readback

- GitHub release `383098584` remains public, non-draft, and non-prerelease at lightweight tag `v1.1.55-reconstructed`, commit `a2c2eff1bbe924354133ef38aa8705fc7c8255b3`. A fresh GET of the sole release asset returned 2,689,752 bytes and the canonical SHA-256.
- CurseForge file `8810946` remains public/approved with status `4`, release type `1`, Minecraft `1.21.1`, NeoForge, client/server labels, and a 2,689,752-byte file. A fresh GET returned the canonical SHA-256.
- The bounded Modrinth recheck found no project ID/slug, publisher configuration, workflow, or secret name. No identifier was guessed and no project was created.

```text
GITHUB_ARTIFACT_INTEGRITY: PASS
CURSEFORGE_ARTIFACT_INTEGRITY: PASS
MODRINTH_VERDICT: BLOCKED_BY_MISSING_CONFIGURATION
MODRINTH_ACTION_REQUIRED_FOR_RELEASE_QUALITY: NO
```

## Evidence files

| File | SHA-256 |
| --- | --- |
| `prism-pre.json` | `B14088916E5155BDA500D4CF1B12F3DE6B9C077E4068196E67F5FB9BFC93E99E` |
| `prism-post.json` | `938FE884752B884D43DC770EE19EE48E72519039205BA3C85D12DB992E5ADCBC` |
| `prism-comparison.json` | `7F5BB7B70EB09A5AB9BB843E8781EDC9539C0AC3D70F303AB1826A9A9A580C30` |
| `process-audit.json` | `58CDF362F092214905785FC2C0DF977D5F82AD8B507D8801B61E737FC8E5954C` |
| `instance-postflight.json` | `43C4E0554FE85216FE1DAE374FAD535CC92586ED81F434F7875B2B3ABE67E1BE` |
| `cleanup-postflight.json` | `10AEC3825A463B8E0146D155A7AD40B96042540F228AFFCB6058A703B799E50A` |
| `public-artifact-readback.json` | `7C0019BDF21EA7F5F74A76280050EC07503939DC787BA879DA41ACE7D0C3A757` |

The larger copied logs remain outside Git under `C:\AI-Work\iedct-residual-gaps-20260904-232647\client-visual-evidence\postflight`. Their recorded hashes are retained in `instance-postflight.json`.

`artifact-manifest.json` remains unchanged because it records the original qualification/publication artifact. This addendum preserves the later compensating evidence without changing any canonical artifact field.

```text
FINAL_VERDICT: PASS-WITH-GAPS
```
