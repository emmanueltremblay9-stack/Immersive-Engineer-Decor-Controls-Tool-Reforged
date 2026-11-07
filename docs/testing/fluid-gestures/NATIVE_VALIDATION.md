# Funnel / Accumulator native gestures

This opt-in qualification does not modify production rendering, layouts or mechanics.
The helper lives in `src/fluidGestureNativeValidation`, enabled only with
`-Pinclude_fluid_gesture_native_validation=true`; default JARs must exclude it.

## Run

Use Java 21, Python 3, Linux with Xvfb, xauth, libX11 and libXtst, and a working
Gradle wrapper distribution/dependency cache. From a **clean committed checkout**:

```sh
python -m unittest discover -s tools/validation -p 'test_fluid_gestures_native.py' -v
python tools/validation/fluid_gestures_native.py --language en_us
python tools/validation/fluid_gestures_native.py --language fr_fr
```

CI: `.github/workflows/fluid-gestures-native.yml`. The bounded matrix crosses EN/FR
with 1280x720 scale 1 (1280x720 logical), 800x600 scale 2 (400x300 logical),
and 960x810 scale 3 (320x270 logical). Each job has a 35-minute limit and unique
locale/resolution/scale artifact name. Each run uses a fresh nonce-owned flat world
and exact checked-out SHA. The actual GUI scale must equal the requested scale;
clamping fails qualification. Every capture records framebuffer, native window and
logical GUI dimensions separately, plus raw and scaled Minecraft mouse coordinates.
The validator independently computes the logical dimensions (ceiling division),
centered target coordinates and raw-to-logical mapping; mismatched geometry fails. A marker created
exclusively inside the newly launched xvfb-run binds the exact DISPLAY to the nonce
and is verified by the Java fixture before any native input. The runner checks
clean source before and after execution, bounded prepare/client processes, no crash
or fatal logs, completion marker, native PNG hashes/dimensions and strict JSON proof.
Artifacts are retained under `build/fluid-gestures-native-*/`, including failures.

## Evidence contract

The input filter is checked on the authoritative server `MachineMenu` and its
`MachineBlockEntity` backing inventory: empty buckets accepted, water buckets
rejected. The client menu has a provisional `SimpleContainer`, so it is checked
only for valid empty-bucket insertion and noninsertable output. Per-machine slot
contract evidence is bound to the same menu ID as its gestures. This qualification
does not change the client projection or production server filtering.

For **each** machine, require header, tank, empty input/output roles, structural
outlet/cap and occupied input tooltip observations. Actual `mc.mouseHandler`
coordinates must match the independently validated GUI target, with a tooltip
rendering-path event (`RenderTooltipEvent.Color`, after cancellable tooltip Pre)
followed by `ScreenEvent.Render.Post` in the same frame, observed only while that
actual target is active, matched menu/slot state, and full-window PNG at every hover.
The evidence names this event path explicitly. Inspect every PNG for visible
tooltip content: event observation alone is not pixel-level acceptance.

Gestures use XTest MotionNotify, native button press/release and native Shift
press/release. The normal GLFW/Minecraft callbacks and inventory packet handling
remain responsible for delivery. No direct screen click callbacks, `menu.clicked`,
`quickMoveStack` or inventory game-mode calls substitute for native input.
Separate ticks verify GLFW pressed state, release state, actual pointer and
server/client agreement. Held keys/buttons are released on success and failure.

Sequence: player bucket pickup → input placement → input pickup → player return;
Shift-click player → input → player; seed exactly 1000mB water while input is empty,
then native Shift insertion of an empty bucket, ordinary processing ticks, native
output pickup and player return; pick up that same produced water bucket, click the
input slot, await matching server/client snapshots with the water bucket still
carried, then return it to the player slot. This is an **invalid-input attempt with
state conservation**, not proof of server rejection or client prediction correction.
The pre/post state for `invalid_insert_attempt` is identical: a swallowed click
could also satisfy those snapshots. GLFW pressed/released observations prove native
button delivery, not receipt of the corresponding container-click packet. The
fixture has no action-correlated server packet acknowledgement. The proof explicitly
records `invalid_input_evidence: native-attempt-and-state-conservation-only`, and the
validator requires this scope marker. Authoritative `mayPlace`/backing inventory
rejection is checked separately as the slot contract, not as executed packet evidence. Input-only cases have no fluid, no neighboring
fluid and clear frozen weather. No redstone pause or production mechanic changes.

Every server-confirmed state transition checks server and client menu identity, both machine
slots, aggregate player bucket counts, carried stack, fluid amount, one conserved
bucket/container, and absence of server-side dropped items. The processing seed is
recorded separately and is not an output-stack injection. Seeding is isolated
fixture setup, not evidence of a user gesture.

The six hover captures and twelve gesture captures per machine are **bounded native
interaction evidence** (36 captures per job, 216 for all six profiles), not full visual
acceptance. Server receipt/rejection of the invalid-input attempt, drag/quick-craft,
slot-boundary/frame hit tests, other scales and resizing
remain unqualified. The v2 evidence contract requires the added native attempt sequence
and explicit geometry; older v1 results cannot qualify this extension.
A syntactically valid fixture or passing Python mutation tests do not prove mapped
compilation or native execution. Those claims require successful exact-source CI
and inspection of its captures. No merge or release is performed by this workflow.
