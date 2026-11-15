# Smelter ordinary-JAR qualification

This is an independent harness on production base `97883bec49afbc65bb4cc8270987f44686a6a7e1`.
It does not depend on the Milking PR, change gameplay, enable a production presentation
selector, or install a dedicated server. The client creates only a nonce-marked isolated
integrated world. Ordinary production artifact SHA-256 is required to be
`53ff03f30cb0c8921f4a8fdb205788cba567aab515804c7d2c7a0ab2daf6f98f`.

The original `NativeSmelterGuiValidation` is copied verbatim except its package, enforced
by a conservation test. A separate development mod bootstraps it without activating the
bundled `NativeClientMiningValidation`. Runtime classpath excludes all main outputs.
Gradle rejects other harnesses, split packages, changed ordinary JAR bytes and leaked
harness classes. Before/after origin checks bind six production classes, the mod module,
and active faceplate bytes to the normal JAR; selectors and resource overrides fail closed.

The original `validate_smelter_gui_proof` is composed, not replaced or relaxed. Profiles:
EN 1280x720 scale 1 and FR 1280x260 scale 1. Expected proof retains live client/server
menu identity, slots and synchronized heating state; injected 0/50/100 progress frames;
closed/open drawer frames; scaled native screen callbacks, resize, Escape, restored
focus and localized accessible labels. Five framebuffer captures per profile.

Limits: setup injects inventory, lava and energy, and freezes progress via server fixture.
This is not natural smelting qualification, physical OS input, inventory transfer routing,
JEI, narrated audio, resource reload or final owner visual acceptance. Existing PNG gate
checks hash/header/dimensions and capture state, not semantic pixel recognition. A native
PASS must only be claimed after an actual successful CI/client run; synthetic tests and
source-conservation tests are not native proof.

Run from a clean committed checkout:

```sh
python tools/validation/smelter_packaged_native.py --profile en-1280x720-scale1 --expected-jar-sha256 53ff03f30cb0c8921f4a8fdb205788cba567aab515804c7d2c7a0ab2daf6f98f
```

Retained artifacts include normal JAR, runtime classpath report, exact source SHA, raw
JSON, captures, bootstrap/build/client logs and crash reports. No mutable external world
or existing saves are accepted. No production source is changed by this adaptation.

## Initial packaged-run setup correction

Run `43658693970` compiled/loaded the normal JAR and reached the retained fixture,
but both profiles timed out waiting for the screen/menu to synchronize. Logs show
flat-world initial spawn at y=-60. The fixture teleports the player to (.5,81,3.5)
without placing a floor there; only the machine has support. Falling out of menu
range is therefore a concrete setup risk, not yet an observed causal diagnosis
(the first artifacts did not retain player coordinates at failure).

The separate bootstrap now builds a 3x3 stone support at y80, x=-1..1,z=2..4,
and waits for the server task before invoking the unchanged fixture. Production
menu-distance rules and every original assertion remain unchanged. Failures retain
client/server positions, menu identities/fields and a native screenshot, all explicitly
failed diagnostic evidence rather than successful qualification. Native confirmation
of this setup correction is pending; do not label the initial runs successful.
