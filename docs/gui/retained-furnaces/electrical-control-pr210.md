# Small Electrical Furnace: live rotary control and segmented FE meter

## Scope and source baseline

Owner request: resume the Small Electrical Furnace integration in PR #210, correct the FE bar and make the control turn when clicked.

- Consumer baseline: `366ecf2c9d7e74afcc6398b80cfa84896b74e819`, branch `codex/iedctr-prism-test-20261123`.
- Minecraft Java 1.21.1, NeoForge 21.1.230, Java 21; Parchment 2024.11.17, from this baseline's build configuration.
- GUI GOV: `1b50dd50a0698c4da3f87aaf99358748d7597a3e`, source package 0.3.0. README, manifest, profile, role/authority, platform, QA and graphic workflow were read at that commit; their manifest SHA-256 values were checked.
- Versioned API references: [NeoForge 1.21.1 screens](https://docs.neoforged.net/docs/1.21.1/gui/screens/) and [menus](https://docs.neoforged.net/docs/1.21.1/gui/menus/).

This is a bounded consumer correction. PR #210 remains a Prism test draft; it does not grant merge, release or installation authority.

## Defects and correction

The old handler selected an absolute speed from the pointer quadrant. Repeating a click at the same location therefore selected the same speed, and a center click selected OFF. The renderer erased the handle and moved a small luminous marker. The old FE drawing painted one continuous rectangle over the eight glass cells and their dark separators.

The new control sends one relative step per click. The server applies that step to its current speed, including when several clicks arrive before the client receives an update. The renderer displays the original textured grip at the confirmed detent, around the fixed bezel. The energy renderer fills each of the eight source-defined glass interiors separately. Empty glass, metal bevels and separators retain the original texture.

### Interaction contract

| Input | Result |
|---|---|
| Left click in control | Advance one detent: OFF, I, II, III, OFF |
| Right click in control | Move one detent backward |
| Middle or unsupported button | No control action |
| Click outside control | Existing container behavior |
| Reopen menu | Show the authoritative stored speed |

Control hit region remains local GUI `[168,201) x [74,103)`. The panel remains 228 x 250 logical units; the retained raster is 1199 x 1312. All machine slots and player inventory coordinates are unchanged.

Action 30 remains the existing absolute selector. New action 32 carries direction `+1` or `-1` in `valueA` and zero in `valueB`. Reserved action 31 remains inert. The electrical menu validates active ownership, spectator status, proximity, block presence, machine kind and world before forwarding an allowed request. Accepted state is broadcast through the existing menu data; no client-predicted speed is treated as authoritative.

Synchronization fields remain: heat 0, stored FE 1, progress 2/3, selected speed 4, capacity 5. The four modes retain their existing processing steps and consumption. Turning the control itself consumes no FE.

### Artwork contract

The original PNG bytes are unchanged:

| File under `textures/gui/retained_furnaces/` | SHA-256 |
|---|---|
| `electrical.png` | `263f98bec7a5f8ef37549fa4412a0bbc3e0de2097701b54f76e2a5c25452cd34` |
| `electrical_dial_neutral.png` | `393047059bdabd4abe9a94383f44164c6d4dab1438862fd0d1129f87294243c3` |

The old neutral patch's actual source origin is (900,390), established by pixel comparison with the retained background. Visual review found that it retains an upright rectangular silhouette, so it is no longer used at runtime. The complete circular control cap pivots at source (964,462); circular row runs of radius 38 sample the existing background texture and replace the old painted grip, while the surrounding outer bezel, fixed detent marks and plate stay stationary. Rotation angles from its original upright direction are 180,270,0,90 degrees for OFF,I,II,III. Its embedded index mark rotates with the grip.

FE glass spans source y `[450,483)` and eight x intervals: `[493,527)`, `[532,564)`, `[569,600)`, `[606,638)`, `[643,673)`, `[678,708)`, `[714,744)`, `[750,780)`. Each cell represents one eighth of the synchronized capacity; the active cell fills fractionally from left to right. Invalid or zero capacity draws no fill. Arithmetic uses long intermediates and clamps overcapacity. Translucent amber shading preserves the source glass shading below it.

Tooltips report stored FE/capacity and the selected mode. English, French and Canadian French explain both click directions; wrapping preserves translated line breaks. No runtime text is painted into the texture.

## Verification and limits

Added Java regression coverage exercises forward/reverse cycles without intermediate acknowledgements, malformed payloads, stale/distant/spectator/removed contexts, reopen/save-load, eight-cell fractions and mouse-button bounds. The GUI album native fixture exercises physical synthetic XTest button events against the ordinary candidate JAR and records server/client state plus native screenshots for all detents and FE empty/half/full.

Local checks during preparation: source integrity, PNG identities, JSON parsing, pure Java control smoke, tooltip dispatcher recording and whitespace checks. Local Gradle cannot download its distribution in the preparation environment, so local compilation and Minecraft runtime checks are **BLOCKED**, not reported as passing. The existing PR workflows provide build, GameTest and native evidence for the actual uploaded commit; their resulting status and artifacts must be inspected separately.

Acceptance requires visible handle rotation, every same-point click counted by the server, four unique confirmed detents, persistence after reopening, zero/four/eight lit cells for 0/16000/32000 FE, and no FE fill on the bezel or separators. Static assertions alone do not establish visual acceptance. Owner visual acceptance, Prism installation and release remain separate.
