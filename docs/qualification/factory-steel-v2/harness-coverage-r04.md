# R04 harness extension: scope and pending runtime evidence

This change modifies test drivers/validators only. It does not change artwork,
production mechanics, network packets, or release approval.

## Album

`tools/validation/gui_album_three.py` still requires a clean committed checkout,
nonce-owned fresh integrated world/display, exact normal JAR hash, separate
harness module and production screen class provenance. Default GUI scale is 2.

In addition to the six existing screen captures, Hopper records selection indices
0, 6 and 12. Selection is **server-seeded ContainerData**, not a user click and not
an automatic transfer traversal. The real menu broadcasts that field to the
client. Each capture requires matching server/client selection and menu ID,
three completed screen-render callbacks with the requested client state, and
full client/server occupancy (54 Hopper slots). Render callbacks prove the screen
was rendered with that state, not that a particular draw call was intercepted.

The PNG validator independently compares each selected row with the same row
unselected in another screenshot. It requires changed pixels on both expected
top/bottom corner-marker edges, with matching pixels in both unselected captures
to exclude general scene drift. This uses the real sparse corner sprite, not a
fictional solid outline. Synthetic tests reject the old 17-pixel pitch and an
absent marker; actual native captures must still pass this new gate. This is
localized visibility/alignment evidence, not a full pixel-perfect renderer or
artistic acceptance test. The six columns and all 18 selections are not covered.

All 36 player slots, 18 Hopper storage slots and 12 Dropper input slots are seeded
with items of varying counts, including retained 64-count examples. Continuous
transfer is disabled in the seeded album world (no redstone source, no neighboring
inventory or dropped-item source). Dropper input/player occupancy must be at least
48 server/client slots. Filter slots are not artificially populated. Pointer
placement is synthetic XTest OS motion, verified outside the GUI before capture;
physical_os_input=true records this honestly, human_input=false, and zero button
events are required. No selection click is performed.

## Dropper control driver

`tools/validation/factory_control_tooltip_native.py --machine dropper` now records
before/after captures for filtergate, externgate and triggermode. Each transition
is a real XTest left click through the existing production screen/packet path.
The driver waits for the exact expected logic value on both client and server,
checks container identity, records the acknowledged before/after state, then
waits for tooltip render frames before the after-capture. The Python validator
checks all three bit transitions, their continuity, and their correspondence to
the captured client states. Existing text, bounds, gray style, isolation, hashes
and render-frame gates remain mandatory. Dropper tooltip wording is static, so
no semantic wording change is falsely required or claimed.

Coverage is one transition per switch, not all cycle positions or sliders,
manual dispensing, item transfer, networking on a dedicated server, or a human
mouse test. Hopper's existing XTest inversion/trigger tests remain in place.

## Run after integration

Build and mapped compile the test module through the existing scripts/Gradle
configuration. Run album at scale 2 and both localized native control suites on
the exact committed source/JAR. Review captures independently. The source parser
and Python fixture tests do **not** replace that runtime qualification; no new
native screenshot or mapped compilation has been produced by this local edit.

## Native 88 album timeout follow-up

Native run 88 timed out at WAIT before any captures. The old proof did not expose
which wait condition was blocking, so its exact cause cannot be reconstructed.
The prior GLFW target at window width/height minus two was unsafe on a bounded
Xvfb desktop: a nonzero window origin/decorations can make that target unreachable.
The driver also did not request focus. These are identified harness defects,
not a confirmed explanation of that particular timeout.

The revised driver reuses the already native-validated XTest motion mechanism
from the Dropper harness, focuses the window, targets window-local (8,8), and still
requires the actual Minecraft pointer callback to report that position. It also
requires the observed pointer to be outside the panel. Only OS-synthetic pointer motion is sent; no click, synthetic selection callback,
timer extension or removed selection gate is added. The validator requires the
explicit motion-only input method, physical_os_input=true, human_input=false and
synthetic_button_events=0; negative tests reject clicks and ambiguous provenance.
Periodic/changed-reason diagnostics and terminal proof record actual Minecraft
and GLFW pointer positions, window position/dimensions/focus, client selection,
occupied slots and frame count. They distinguish focus/pointer, synchronization,
render callbacks and server-inspection waits if the native run fails again.
The correction requires a new native run; source tests are not runtime proof.

## Native ee: observed callback stall

The ee proof now establishes the blocked gate: focused window at (0,0), GLFW cursor
(8,8), Minecraft MouseHandler (640,360), selection 0, 54 occupied slots and three
render callbacks. It is not a selection synchronization or render-frame timeout.
The first-move discard hypothesis is consistent with these observations, but the
mapped MouseHandler source was not present in this local workspace/cache to
verify its implementation.

The next driver removes pre-opening motion. After the screen opens it sends a
prime motion to (12,12), observes that GLFW reached it, waits a different client
tick, then sends (8,8). It still requires Minecraft's actual callback coordinates.
If that acknowledgement is absent it repeats the distinct two-point sequence,
never repeatedly sending an already-current target and never writing MouseHandler
state. Phase and last-motion tick are included in diagnostics. No timeout, pixel,
state, frame, inventory, provenance or input-scope gate is relaxed.
