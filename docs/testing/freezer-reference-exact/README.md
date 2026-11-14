> Current activation candidate: owner accepted the four-phase appearance and authorized preparation of normal Freezer V5 selection. Dynamic chamber/FE now select without development properties; the exact snapshot remains available with its historical explicit property. Server slot count/order, backing inventory, protocol and gameplay are unchanged. New normal-JAR evidence must pass on this commit; earlier opt-in receipts do not prove this default. Below sections retain earlier provenance where noted.

# Current opt-in dynamic chamber candidate

See [dynamic integration](dynamic-frost/INTEGRATION.md). Live-energy mode now also draws reconstructed chamber layers by server phase. Exact snapshot remains original and production remains unchanged. Sections below are historical static-stage evidence, not current dynamic qualification.

# Freezer V5 exact-reference binding experiment

LOCAL DEVELOPMENT ONLY. NOT FINISHED GUI OR FUNCTIONAL ACCEPTANCE.
Base commit: 8c8dd2f0d6dc018bd874c961e2703d85f6bd4dee (PR151 source).
Created in a new local checkout from committed objects, applied the preserved V3
patch, then adapted reference identity, conservative geometry, opt-in, tests/docs.
V3 checkout/deliverables and V5 source/native JSON/render proof remain untouched.
No source artwork edits, new images, dark alternative, network, push/PR, merge,
release, user-PC access or upload were performed.

## Retained reference

Source freezer-mixed-rp-v5-preview.png, 1254×1254 RGB.
Byte SHA256: 68c1dc11e7d0e920addebca18100892861a70c5d68db5547b8014235acee3d46.
RGBA SHA256: 61377093d58be84df2852af2e020ae1df56c36190f9a4536d5e65c4260de0d0c.
The original PNG is copied byte-for-byte to freezer_reference_v5/background.png.
Only the PNG and nearest/clamp metadata are runtime artwork, not the large JSON.
Official proof render SHA256: f8f376c0b974929421f8400335966680ca02ae620a029ca028c55986d01e2304.
Zero decoded source/runtime/official-render differences do not prove GPU display.
Four CPU sampling checks likewise are models, not screenshots or native proof.

## Default off and unchanged machine semantics

Opt in only with ./gradlew runClient -Pdev_freezer_v5_exact_reference=true
or client JVM -Diedctr.dev.freezerV5ExactReference=true. Old V3 property does not
activate this candidate. Restart/reopen after changing the property.
The client menu captures the flag and screen factory reads that captured choice.
Authoritative constructors and server worlds retain legacy presentation. Flag off
uses original MachineScreen.SmallFreezerScreen, MachineLayout and source art.
All 39 menu slots remain ordered: input0, reserved1, output2, inventory3..29,
hotbar30..38. No recipe, FE, NBT, packet, transfer-range or machine-state changes.

The prior conditional recovery button and Ctrl+Shift+R are preserved. They appear
only with nonempty reserved slot1, use standard QUICK_MOVE, guard carried stacks,
and retain contents on full inventory. This behavior still requires native/game
validation. Recovery is excluded from empty-art fidelity captures.

## Presentation and tests

See GEOMETRY.md and geometry-manifest.json for V5 pixel measurements, anchor boxes,
transform and screenshot protocol. Full image remains uncropped/unreflowed. Vanilla
renders items, highlights and count/durability overlays. Tooltips use normal screen
coordinates. No title/state overlay or authored animation was introduced.
V5 static-art tooltip covers its central chamber and baked energy track.

Run offline:
- bash tools/validation/check_freezer_reference_geometry.sh
- python tools/validation/verify_freezer_reference.py --source ORIGINAL.png --proof-render OFFICIAL.png
- Java syntax parser tools/validation/ParseJavaSources.java on the changed Java files.

Executed results are recorded in geometry-checks.txt, resource-checks.json,
java-parse-check.txt, event-routing-checks.json and validation-status.json.
The manually triggered workflow remains unrun and would require a real exact commit.
Baseline CI success does not validate this candidate. No JAR was produced.

## Remaining gates

Mapped compileJava, JUnit runner, GameTests, build, exact-commit CI and actual client
captures/interactions remain unexecuted. Prior Gradle9.2.1 download failed Network
unreachable; no retry/bypass. Xvfb unavailable. No user-PC fallback attempted.
Before acceptance, build/identify exact JAR by SHA256 and run flag off/on against a
server without the flag; check all 39 IDs and reserved saved-data recovery including
full/partial inventories and carried stacks. Exercise all 38 slots with pickup,
right click, shift-click both ways, number keys, double click, throw, outside clicks,
click/release/drag, resize, scroll, close/reopen, fractional/negative pointers and
normal-size tooltips. Check JEI, resource packs and readability at required viewports.
Keep raw captures, dimensions, transform, explicit crop and sampled pixel diffs.

Frost, energy segments and phase icons are STATIC REFERENCE ART, not live FE or
progress. Real dynamics still need approved neutral plates/state frames, explicit
server mapping and all idle/invalid-input/no-FE/blocked/redstone/completion states.
Do not label this experiment full functional acceptance or pixel-perfect GPU output.
