# Exact ordinary-JAR TFC and Mooshroom qualification

This harness closes an artifact-identity gap, not a new gameplay feature. The two
historical client fixtures remain at their original assertions, time bounds and
native XTest input checks. Only package/mod subscriber ID, dedicated selectors,
and before/after provenance insertion differ; structural conservation tests check
that boundary. Production Java/resources and the historical validators are untouched.

## Two-stage runtime, deliberately not the legacy source profile

1. Build `jar` normally on NeoForge **21.1.230**, with no compatibility property.
2. Hash and copy that artifact into the isolated evidence directory. Require SHA-256
   `53ff03f30cb0c8921f4a8fdb205788cba567aab515804c7d2c7a0ab2daf6f98f`.
3. Start a new Gradle invocation with `include_tfc_packaged_validation=true` and the
   mandatory `tfc_runtime_jar` pointing at that copy. This selects NeoForge **21.1.234**
   and official TFC **4.2.12** plus Patchouli **1.21.1-92-NEOFORGE** only for compatibility.
4. The compatibility task graph rejects production `compileJava`, `processResources`,
   `classes` and `jar`: no reconstruction of the candidate under the other loader.
   The separate harness compiles against the copied JAR and main's dependency classpath,
   not main outputs. Runtime main outputs and split packages are forbidden.

Never use `include_tfc_milking_runtime=true` in this harness: that historical profile
adds fixture sources to main and is still retained independently for source/GameTests.
Crossover and other fixtures cannot be co-enabled. Default production dependencies,
normal loader selection and game behavior remain unchanged when the new flag is absent.

## Artifact and dependency evidence

Production origins include the screen, menu, machine, layout and TFC compatibility
boundary/adapter from the exact ordinary JAR. Active Milking artwork is checked against
the same archive and its retained hashes. Runtime origins are verified before/after.
TFC's loaded JAR must match the existing binary pin
`3c2d80e47c78cfbd617823ebe0d3d5e2f9ba9e7f04c1b34867dbab3894584440`;
its DairyAnimal class resource bytes and module root are checked independently.
Patchouli is version-pinned to the established official Maven coordinate. Its observed
JAR hash is recorded and verified against the retained bytes, but it has no preexisting
fixed digest pin in this repository. Do not describe it as digest-pinned.
The native proof, ordinary JAR, dependency JARs, classpath/build report, exact source,
raw screenshots and logs are retained. Offline replay accepts `archive_path` for the ordinary JAR and `dependency_archives`
with exactly `tfc` and `patchouli` paths. These copies supply archive bytes only; the
original CI paths, code-source URIs, module roots and proof JSON remain unchanged.
Absent these explicit parameters, live CI continues checking the actual original files.

## Preserved scope

Both executions use FR 1280x720 GUI scale 2, with independent nonce-marked worlds:

- TFC: named cow identity synchronized client/server, READY/RECOVERING/native tooltip,
  400-tick fixture cooldown, no repeated production, second production after expiry,
  original configuration restoration and native XTest hover.
- Mooshroom: RED/BROWN adults and babies, milk bucket/tank behavior, 120 machine ticks,
  baby rejection, source portraits and identity tooltips with native XTest hover.

These are integrated-client fixtures, not new dedicated-server installations. TFC's
400-tick duration is an existing bounded fixture setting restored by the fixture,
not a new gameplay default. No Crossover or broad modpack compatibility is qualified.
No owner visual acceptance or release follows from synthetic tests. Native and Java
compilation results must be reported separately, only after actual execution.

```sh
python tools/validation/tfc_packaged_recovery_native.py --profile fr-1280x720-scale2
python tools/validation/tfc_packaged_mooshroom_native.py --profile fr-1280x720-scale2
```

Run from a clean committed checkout; both scripts retain fatal/crash rejection by
composing the original validators, without weakening assertions or retrying failures.

## Runtime dependency graph correction

Initial packaged run `43662247467` built the normal JAR and prepared/verified the
harness, but the client launch task graph was correctly rejected before Minecraft
started: production build tasks were still reachable. A filtered
`main.runtimeClasspath - main.output` collection can retain the output's build-task
dependencies even after its files are removed. Runtime now uses main's dependency
configuration directly, with an exact file-set comparison against the former filtered
set in the verifier. The guard is retained and reports offending task paths and their
immediate dependents. No task is excluded/skipped to bypass the guard. Native confirmation
of this correction remains pending; the rejected launch is not a gameplay failure or PASS.
