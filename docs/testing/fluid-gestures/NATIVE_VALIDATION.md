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

CI: `.github/workflows/fluid-gestures-native.yml`. The bounded profile is 1280x720,
GUI scale 1, fresh nonce-owned flat world, exact checked-out SHA. A marker created
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
output pickup and player return. Input-only cases have no fluid, no neighboring
fluid and clear frozen weather. No redstone pause or production mechanic changes.

Every server-confirmed state transition checks server and client menu identity, both machine
slots, aggregate player bucket counts, carried stack, fluid amount, one conserved
bucket/container, and absence of server-side dropped items. The processing seed is
recorded separately and is not an output-stack injection. Seeding is isolated
fixture setup, not evidence of a user gesture.

The six hover captures and nine gesture captures per machine are **bounded native
interaction evidence**, not full visual acceptance or coverage at other scales.
A syntactically valid fixture or passing Python mutation tests do not prove mapped
compilation or native execution. Those claims require successful exact-source CI
and inspection of its captures. No merge or release is performed by this workflow.
