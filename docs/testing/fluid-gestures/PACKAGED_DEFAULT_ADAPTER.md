# Packaged/default Funnel and Accumulator qualification

## Candidate and honest status

PR171's original implementation (`4e95ac450768d868c1af3ba0fe362314e85a6fd3`) is reconciled with main after PR170 (`7f8f50e86d030797aa648ec21cea70dc4658bd6b`). The reconciliation preserves all 25 original PR171 files, main's ordinary Factory Placer V2 packaging, and the independent Block Breaker qualification. No backend/menu/network changes or new production-render changes are added by this adapter. The Accumulator collection tooltip keeps its native 240px maximum wrap.

The adapter has been reconstructed after a replaced execution environment. Historical hashes, test runs and native results from a different checkout do not qualify this candidate. Local Python proof/contract tests and static review are separate from mapped compilation and native execution. At preparation time, no mapped build, native client, CI workflow, publication, merge to main, or release has been run for this reconstructed adapter. The first CI results must be inspected on the exact committed candidate.

## One six-profile workflow, one ordinary artifact

The existing `fluid-gestures-native.yml` workflow now uses the packaged driver, replacing its six source-runtime executions rather than adding six more. An ordinary-artifact build job produces the production JAR without test flags, hashes it, binds it to the candidate source SHA, and uploads it. All six matrix jobs download and verify the same JAR and manifest. Each job also rebuilds the clean source locally and requires byte equality with the shared artifact before launching.

Profiles: EN/FR at GUI scale 1 (1280×720), scale 2 (800×600), and scale 3 (960×810). The historical source-runtime scripts and fixtures remain for their original records and offline tests; their prior passes are not evidence of packaged/default behavior.

## Independent test mod and provenance

`iedctr_fluid_validation`, package `com.oblixorprime.iedctrtest.fluid`, is isolated from production. Compilation uses the ordinary production JAR plus API dependencies, not main output. Runtime excludes main output. `verifyFluidPackagedRuntime` rejects leaked driver classes, split packages, a supplied JAR different from the ordinary rebuild, or incorrect development-mod registration. The fluid run rejects simultaneous old fluid or Factory/Breaker test profiles, without modifying those profiles.

Runtime independently verifies ModList JAR path and hash, SecureJar root, production class resources and code sources, module isolation, exact archived class bytes (including the retained-artwork renderer), and active texture-pack bytes. Any `iedctr.dev.*` selector or old fluid/reference driver property must be absent, even if its value is false. `iedctr.fluidPackagedNative=true` is only test-driver activation.

The runner requires a clean committed checkout, fresh nonce-owned Xvfb display, exact source SHA throughout, zero client exit code, no timeout/crash/fatal log, and matching screenshot hashes. An explicitly supplied artifact must remain unchanged during preparation and execution. Reports, logs, source/hash manifest, ordinary JAR and captures are retained.

## Native actions and evidence limits

Each machine opens through an actual block hit followed by XTest RMB press/release, with empty hand and survival reach checked. Menu gestures retain physical XTest pointer/buttons/Shift and independent authoritative client/server menu, inventory and fluid conservation checks. No direct menu-click, inventory-gameMode shortcut or synthetic screen opening is used.

The original 12 hover and 24 gesture captures per profile are retained. Six additional empty/half/full tank captures use explicitly labelled setup seeds. Capacities are Funnel 3000 mB and Accumulator 4000 mB; the empty bucket stays outside machine input during these captures and tanks return to empty before the original gestures. World creation, placement, teleport and seeds are setup, not natural acquisition. Invalid insertion remains an attempted native action plus unchanged-state/conservation evidence, not causal server acknowledgement.

Headers and tanks wait at least 120 ticks and until the recipe toast disappears. No unrelated toast is cleared. The resulting 42 captures per profile (252 across six profiles) prove capture identity and synchronized state; visual fill correctness, text readability and aesthetics still require reviewing actual screenshots. Tooltip-render events alone do not prove text contents or readability.

## Retained artwork review

The production PNGs were opened and visually inspected in this reconstructed checkout. The blank metal nameplates, square input/output cells, Funnel horizontal gauge, Accumulator vertical glass tank, and separate inventory/hotbar structure remain intact. Their bytes match GUI DEV PR5 commit `0c166b34fb19cdf4a54373ff5e9c4770a854be12`:

- Funnel: `c5dcc2ee56fdace5f9bf66456e0a4dca91ce4fddb78887a9a6b60ff914336a64`
- Accumulator: `da2a83c0f8cc68be5f69ad4f3cc392cc4a600206ae39ac8b81142e421e466872`

This asset inspection is not a current native-render screenshot pass.

## Reproduction

Run offline proof and retained GUI tests:

```sh
PYTHONDONTWRITEBYTECODE=1 python -W error::ResourceWarning -m unittest discover -s tools/validation -p 'test_fluid*.py' -v
PYTHONDONTWRITEBYTECODE=1 python -W error::ResourceWarning -m unittest discover -s tools/validation -p 'test_accumulator_tooltip_wrap.py' -v
```

With mapped build dependencies available:

```sh
./gradlew --no-daemon -Pinclude_fluid_packaged_validation=true verifyFluidPackagedRuntime
```

For authorized Linux native execution, after committing the candidate:

```sh
python tools/validation/fluid_packaged_native.py --runtime-jar /absolute/path/normal.jar --language en_us --width 1280 --height 720 --gui-scale 1
python tools/validation/fluid_packaged_native.py --runtime-jar /absolute/path/normal.jar --language en_us --width 800 --height 600 --gui-scale 2
python tools/validation/fluid_packaged_native.py --runtime-jar /absolute/path/normal.jar --language en_us --width 960 --height 810 --gui-scale 3
```

Repeat for `fr_fr`. Java 21, Xvfb, xauth, X11/XTest and the game's mapped dependencies are required. Do not inject presentation selectors through environment/JVM options. Review all six evidence galleries and exact artifact hashes before declaring qualification complete.
