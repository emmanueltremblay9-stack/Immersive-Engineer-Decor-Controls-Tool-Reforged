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
placement is synthetic GLFW positioning, verified outside the GUI before capture;
no physical input is claimed for the album.

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
