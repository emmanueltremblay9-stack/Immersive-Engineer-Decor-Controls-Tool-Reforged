# Barrel V5 default packaged qualification

This supplemental gate qualifies the default V5 presentation from the ordinary
production JAR without any presentation-selection property. It does not replace
Factory/Breaker, Funnel/Accumulator or cumulative GUI gates. The driver is a
separate explicitly enabled test mod, `iedctr_barrel_v5_validation`, outside
`src/main` and the production JAR. All `iedctr.dev.*` properties, including the
historical V5 opt-in property even when false, and competing native fixture
selectors are rejected.

The evidence schema is `barrel-v5-packaged-native-v2` with runtime mode
`v5-default`, an empty production-selector list and mandatory selector-absence
attestation. Earlier opt-in evidence cannot satisfy this default-path gate.

## Running

On a Linux machine with Java 21, Python, Xvfb, xauth, X11/XTEST and Mesa:

```sh
python -W error::ResourceWarning -m unittest discover -s tools/validation -p 'test_barrel_v5_packaged*.py' -v
python tools/validation/barrel_v5_packaged_native.py --runtime-jar /absolute/path/to/normal.jar --language en_us --height 720 --barrel-fluid water
python tools/validation/barrel_v5_packaged_native.py --runtime-jar /absolute/path/to/normal.jar --language fr_fr --height 260 --barrel-fluid water
```

Use a clean committed checkout. Repeat both locales for lava and milk. The
workflow `barrel-v5-packaged-native.yml` builds one ordinary artifact, hashes it,
shares it across these six profiles and verifies its exact source SHA and bytes.
The runner requires the supplied JAR to equal the ordinary local rebuild. Width
is 1280 and GUI scale is 1; each phase has a bounded timeout. Evidence is retained
on failure as well as success. Do not pass legacy/native-fixture flags alongside
this gate; it rejects competing selectors and source-output runtimes.

## Evidence and limitations

- Production class resources, code sources, modules and bytes are checked against
  the loaded ordinary JAR. This includes the V5 screen, geometry, state arithmetic,
  menu, layout, block entity and existing native-fluid renderer.
- Active V5 artwork must come from the production mod and retain SHA-256
  `df89575afd98ccee396b38cee34f230ef03f94528d5a4ad5bc3425658ef54ca9`.
- The isolated integrated-server world and fluid amounts are **fixture setup**.
  Empty, half and full states do not prove natural fluid collection or processing.
- Opening the placed Barrel is an XTEST right click on the actual block hit.
  Pickup and return of a bucket in player slot 2 use XTEST left clicks at the
  V5-transformed coordinates, including the reduced FR260 viewport. Both client
  and independently sampled server menu state must conserve the item; nearby
  dropped items fail the run.
- Six full-window PNGs are taken after GameRenderer/JEI screen rendering at
  `RenderFrameEvent.Post:LOWEST`, tied to the same pointer, menu, screen and frame.
  Full-tank tooltip bounds must fit the viewport.
- All 38 slot anchor positions and center/four-outside-edge hover responses are
  checked through read-only screen hooks. These are **not physical edge gestures**.
- Samples stay inside the transformed fluid inner canvas `(170,115,41,78)`.
  Occupied samples must be textured and differ from the captured empty artwork;
  the half-full upper sample must match the empty capture. The empty faceplate is
  intentionally not assumed to be a uniform legacy backing color.
- Fluid identity, active native texture provenance, tint metadata, PNG hashes,
  sampled PNG color counts and unchanged-artifact checks fail closed. This is not
  an exact rendered-sprite/tint or human visual acceptance claim. Resource reload,
  all-slot physical interactions, bucket processing and automation are outside
  this bounded packaged gate.

Python tests use synthetic evidence and PNGs solely to exercise rejection paths.
They do not demonstrate Minecraft execution. A prepared harness or passing Python
tests must not be reported as a passed packaged/native profile. Native compilation
and execution require a successful Gradle dependency/bootstrap environment and
actual evidence from the exact committed candidate.
