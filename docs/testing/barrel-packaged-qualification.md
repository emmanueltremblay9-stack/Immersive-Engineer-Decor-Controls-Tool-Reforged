# Supplemental Barrel legacy packaged diagnostic

This independent test complements `barrel-fluid-validation.yml`. That original
six-run cumulative GUI matrix, its Freezer checks, and its evidence gate remain
retained, with the Barrel legacy selector explicitly set to false. Passing the
supplemental Barrel job does not override a failure in
that cumulative matrix, and does not qualify Freezer, Placer, Solar or Milking.

## Artifact and runtime

`barrel-packaged-native.yml` builds one ordinary production JAR at the exact
candidate commit, records SHA-256 and source identity, and shares those same
bytes with all six native runs. Each run rebuilds the ordinary JAR and rejects a
byte mismatch. The separate `barrelPackagedValidation` mod is the only loaded
development mod; production classes/resources come from the normal JAR, never
`sourceSets.main.output`. The normal artifact excludes this separate test
harness. Historical validation classes already present in main are not removed
by this work; their activation properties must be absent.

Runtime evidence binds class resource bytes, code-source URI and module to that
JAR. It also checks the actual active Barrel artwork against the archive and the
retained 201x226 texture hash. The exact selector
`iedctr.dev.barrelV5Reference=false` is required and recorded as
`legacy-diagnostic-only`; every other production selector remains forbidden.
V5 artwork and the normal default presentation are not qualified by this job.
The separate V5 packaged job must qualify the default with no selector present.

## Bounded cases and limitations

The six profiles are water/lava/milk at English 1280x720 and French 1280x260,
GUI scale 1. Each opens the real registered Barrel menu with XTEST right-click
on the placed block in a fresh isolated integrated world. Empty, half and full
tank states are **injected on the server** for display testing, followed by
client identity/amount synchronization. The gate independently decodes captured
PNGs, checks the empty backing, the blank upper region at half fill, and the
reported non-uniform occupied pixel samples. Selected-fluid identity and native
atlas sprite/tint availability are checked separately. They do **not** prove
that those pixels depict the correct sprite or tint. The proof explicitly sets
`rendered_sprite_identity_verified`, `natural_collection_verified`, and
`visual_acceptance` to false. Human review of every water/lava/milk capture,
including tint and fill level, remains required. This is not natural fluid collection or
bucket-processing qualification.

Screenshots and full tank hover evidence are captured at
`RenderFrameEvent.Post` with LOWEST event priority, after the game renderer and
JEI's screen rendering. Screen/frame/menu/pointer identities are correlated;
overlays, recipe toasts and the later profiler chart cannot mask accepted
captures. This does not claim coverage of arbitrary mods drawing after that
event. A bounded player-inventory bucket pickup/return checks native input and
client/server conservation without exercising machine processing.

This profile does not cover resource reload, animation over time, all GUI
scales, other languages, V5 conformity, or complete human visual acceptance.
Synthetic proof tests check acceptance/rejection logic, not Minecraft behavior.
Mapped Java compilation and all six native runs must succeed on the final exact
commit before any native qualification is claimed. No native results are
bundled by adding the harness.

Run a single profile only from a clean committed checkout, for example:

```sh
python tools/validation/barrel_packaged_native.py \
  --runtime-jar /absolute/path/to/normal.jar \
  --barrel-fluid water --language en_us --height 720
```

The runner preserves logs and JSON/PNG evidence under
`build/barrel-packaged-native-*`, including preparation failures. A shared
artifact is mandatory in CI; local runs may build their own ordinary JAR.
