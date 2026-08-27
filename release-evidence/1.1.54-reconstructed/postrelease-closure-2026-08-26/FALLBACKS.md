# Skill and tooling fallbacks

The following requested skills were not exposed in this session. Their absence does not stop the mission; each function is routed to the narrowest available skill or direct evidence method.

| Requested skill | Fallback |
|---|---|
| `prism-active-mods-tree-resolver` | `prism-lab-runtime-management` plus direct `instance.cfg`, `mmc-pack.json`, and active mods-tree reads |
| `prism-launcher-config-diff-audit` | deterministic pre/post file inventory and SHA-256 comparison |
| `minecraft-manual-client-smoke` | native Prism CLI `--launch --world --offline` harness and log markers; no UI |
| `minecraft-client-server-config-split-audit` | direct inventory/diff of `config`, `defaultconfigs`, and per-world `serverconfig` |
| `gradle-modrinth-curseforge-release-audit` | official CurseForge API documentation plus repository workflow/script inspection |
| `github-actions-secret-scope-audit` | direct workflow-expression, permissions, and names-only `gh secret list` audit |
| `github-workflow-dispatch-input-audit` | direct YAML schema parsing and mocked dispatch-contract tests |
| `gradle-resource-filtering-secret-audit` | targeted Gradle/resource reads plus secret/entropy scan of tracked files and artifacts |
| `build-artifact-provenance-manifest` | repository-owned compact JSON manifest with authoritative release metadata and SHA-256 |
| `codex-download-file-integrity-proof` | direct download, byte count, SHA-256, and JAR metadata inspection |
| `github-release-draft-readback-audit` | conditional direct `gh api` readback; no release mutation is currently authorized or expected |
| `notion-duplicate-page-detection-audit` | existing-page lookup followed by update/readback through the available Notion evidence-ledger skill |
| `codex-goal-blocked-threshold-audit` | explicit gate ledger and final unresolved-gap classification |

Available skills actually loaded: `codex-goal-completion-requirement-mapper`, `codex-dirty-worktree-guard`, `codex-workspace-root-policy-reconciler`, `reconstructed-mod-safe-change`, `minecraft-loader-detection`, `prism-lab-runtime-management`, `minecraft-mod-build-install-verification`, `notion-project-evidence-ledger-update`, `git-atomic-commit`, `github-push-verification`, `codex-shell-timeout-exit-status-ledger`, `codex-missing-skill-fallback-reporter`, and `codex-final-evidence-reporter`.

`windows-java-tooling-recovery` was loaded after the standalone selector probes established a Java/JVM path discrepancy. It was used only for read-only runtime/source inventory and process-local diagnosis; no Java repair or persistent setting change was performed.
