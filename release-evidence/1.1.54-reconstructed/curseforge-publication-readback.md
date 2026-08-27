# CurseForge publication readback

> Historical configuration snapshot (2026-08-12). Current automation and publication status is maintained in [postrelease-closure-2026-08-26/SUMMARY.md](postrelease-closure-2026-08-26/SUMMARY.md).

Project ID: `1555214`.

Configuration audit:

- no CurseForge token/API key is present in the local environment;
- the repository has only `.github/workflows/build.yml`, with no CurseForge publication workflow;
- the repository exposes no GitHub Actions secret names;
- the Gradle build has no CurseForge publication task/plugin;
- no authorized CurseForge comment API is configured.

No secret was requested or captured. No CurseForge metadata, file, main-file status, download, or community reply was changed.

`CURSEFORGE_VERDICT: BLOCKED_BY_MISSING_CONFIGURATION`
