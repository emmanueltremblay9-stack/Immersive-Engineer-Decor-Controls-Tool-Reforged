# R04 held-item boundary sub-lot (qualification only)

Base is exact artwork candidate PR204, 4e5c1b68c0fed9e091438e26c0c6d3e2ff262f2a.
This extension changes test harness/validators/workflow only. PR196 and PR204
must remain unchanged; no merge, artwork acceptance or publication is implied.
Dedicated project source: https://chatgpt.com/c/6afbbec6-f500-83ea-a7c5-451c6def8c92

Opt-in: `python tools/validation/factory_control_tooltip_native.py --machine hopper
--language en_us --width 1280 --height 720 --interaction` (likewise dropper).
Use the isolated Actions workflow `factory-r04-interaction.yml` for native
compilation/execution. Local synthetic tests are not native evidence.

After the existing unchanged tooltip cases, the isolated integrated test world
receives exactly 64 stone in player slot 9. A real XTest pickup is required,
followed by left/right clicks at eight fixed, slot/control-excluded decorative
points. Finally a right click eight logical pixels outside the left panel is
the positive witness: 63 carried, one item in one world entity, total 64.
No world item entity is altered to force the witness outcome.

Every click requires a matching observed Minecraft mouse press/release,
unchanged menu identity, client/server inventory convergence, conserved item
counts, unchanged stable configuration fields, and before/after rendered PNGs.
No-op server observations establish unchanged authoritative state, not receipt
of a rejected packet. Full player inventory including offhand/armor is checked.
Original exact-JAR/runtime-origin, nonce-owned world/display, timeout and
existing tooltip evidence gates remain active.

Scope is GUI scale 1, EN 1280x720, eight representative points per machine.
Not exhaustive pixel hit-testing, all 14 controls, slider endpoints, persistence,
distribution, scale 3/4, modpack coverage or human-input testing. Actual native
results and JAR/capture hashes must be checked after CI. R04 artistic approval
remains PENDING independently of technical qualification.
