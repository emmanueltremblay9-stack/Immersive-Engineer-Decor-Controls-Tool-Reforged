# Factory Placer grey-steel V2 retained-reference presentation

This is a default-OFF, client-only presentation of the existing Factory Placer. It does not replace the legacy screen by default and changes no machine gameplay, saved inventory, protocol, or server action.

## Enable and fall back

Pass the JVM property `-Diedctr.dev.factoryPlacerV2Reference=true` to the development client. Only `MachineMenu.client` reads it, only for `FACTORY_PLACER`, and only on a client level. Public/server menu constructors always retain legacy coordinates. With the property absent or false, `ClientModEvents` registers the existing `MachineScreen.FactoryPlacerScreen` behavior. The choice is fixed when each menu opens; reopening applies a changed property.

The dedicated native qualification profile and its instructions are documented separately by the native fixture. This property does not enable a release, add a config migration, or require any server setting.

## Exact retained source

- Source project: [GUI DEV PR 7](https://github.com/emmanueltremblay9-stack/Master-Generalist-GUI-Architect-Designer-DEV/pull/7).
- Source head: `e4a5f6c76710cd1d801c054a669016a48aca3fa4`.
- Retained filename: `factory-placer-grey-steel-v2-concept.png`, from `candidates/iedctr/factory-placer/grey-steel-v2/`.
- Source Git blob: `5649702960955531ff692b51d906ab0b5486bd59`.
- PNG: **1199 × 1312**, **1,991,965 bytes**.
- SHA-256: `ff079488dd0d1c7434597a6915231d88b81732472bb23ccc8a61235eca27ec2b`.
- Runtime copy: `assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_placer_v2_reference/background.png`.

The source PNG is copied byte-for-byte. No regeneration, crop, recompression, color change, or added lettering is baked into it. The retained steel/anthracite composition, blank plaque, restrained copper fasteners, hazard strips and bevels stay intact.

## Geometry and vanilla inventory

`FactoryPlacerV2ReferenceGeometry` measures every interior in source pixels. The source is drawn uniformly at one-third size. Its logical extent is 399⅔ × 437⅓; the containing integer canvas is 400 × 438. The fractional unused right/bottom remainder is intentional: the odd source dimensions are **not stretched** to fit an integer canvas. A second uniform fit keeps the complete canvas within six logical viewport pixels, never upscales it, and has no arbitrary minimum scale. Only tiny viewports relax the margin.

There are **54 real vanilla menu slots**:

| Menu indices | Contents | Original backing indices |
| --- | --- | --- |
| 0–17 | Machine, 3 rows × 6 | Machine 0–17 |
| 18–44 | Player, 3 rows × 9 | Player 9–35 |
| 45–53 | Hotbar, 1 row × 9 | Player 0–8 |

The saved machine container still has **27 slots**. Hidden backing indices 18–26 are not remapped, exposed, deleted, or made into player slots. Slot count/order, validation, transfer rules, and server backing indices are unchanged. The menu changes only client x/y positions. Every complete 16 × 16 item and hover highlight fits its measured dark interior, with vanilla hover padding clipped only if needed. The selected machine-slot outline is 18 × 18 and also stays inside its measured interior.

Rendering, hover, press, release, drag, movement and scrolling share one inverse transform. Scroll wheel values are not scaled; drag pixel deltas are. Outside clicks continue to vanilla. Vanilla still owns carried stacks, double-click, shift-click, quick craft, number keys, close, tooltips and inventory packets. Public GUI bounds are rounded outward in screen coordinates for JEI and other consumers, while internal vanilla coordinates remain on the logical canvas.

## Existing controls and synchronized state

Only the existing synchronized fields are rendered:

- Field 0: logic bits (bit 0 inverted redstone, bit 1 continuous, bit 2 ignores external redstone).
- Field 1: external redstone **or** the server's manual-trigger flash. It is not a guarantee that placement succeeded.
- Field 2: next machine slot/cursor (0–17). Active row is cursor / 6.

The three retained row buttons send **action 22**, values 0/1/2. This sets the starting cursor to 0/6/12; it does **not** restrict placement to that row. The red control sends **action 21**, value 1, requesting one attempt. Inversion sends **action 20**, cycling bits 0 and 2 as `0 → 1 → 4 → 0`, preserving the continuous bit. Trigger mode sends **action 20**, XOR 2, preserving redstone mode. Ignore-power mode already runs automatically in the backend; the tooltip states this rather than promising edge-only behavior in that mode.

Controls accept left presses with an empty carried stack. Right/middle presses, carrying, drag/release and non-control clicks are passed to vanilla and never synthesize control actions. There is no optimistic local state mutation: the markers change only when server fields synchronize. No energy gauge, placement animation, success indicator, sound, or new action effect is invented.

The source's decorative levers remain unchanged. Small live markers below their tracks show the actual modes: the three-position redstone marker is power / no power / ignore power from left to right; the two-position trigger marker is signal-change / continuous. A small inner red-button marker reflects field 1. A gold row marker and a gold in-interior slot border reflect field 2. These are confined to measured flat/dark regions, not frame bevels. Unselected row-marker regions are untouched source art. EN and French (France/Canada) localized tooltips describe state and actions; the header plaque has no painted text. New control, empty-slot and header tooltips use native font splitting, bounded to `max(1, min(240, viewportWidth - 16))` screen pixels, including French text on small or high-GUI-scale views. Existing vanilla item tooltips are untouched.

## Verification and limits

`FactoryPlacerV2ReferenceChecks` is dependency-free and checks all 54 indices, complete item/selection containment, non-overlap, hover boundaries, ten viewport transforms, drag round trips, public screen-space bounds, invalid inputs, all control regions, empty/held/right/middle input routing and native logic/action parity. `FactoryPlacerV2ReferenceGeometryTest` runs those checks under JUnit and verifies the packaged PNG's hash, dimensions and byte length.

Local Java 21 pure checks and syntax parsing can run without a Minecraft launch. They do not prove mapped Minecraft APIs, native rendering, packets, persistence, or gameplay. Those require the exact-commit build and native qualification profile; do not label this presentation game-verified from pure tests alone.

The packaged qualification profile uses the normal production JAR with a separate
test-only harness mod, rather than loading production classes from source output.
It checks the unchanged flag-absent legacy path separately from opt-in V2. The
extended V2 suite covers advanced vanilla gestures, native EN/FR/fr_ca tooltips,
and all 18 selection outlines. Its fail-closed verifier rejects invisible edges
in a small viewport rather than counting absent raster coverage as success.
These are validation requirements, not a statement that the newest commit passed.
V2 remains development opt-in; merging tests does not activate or publish it.
