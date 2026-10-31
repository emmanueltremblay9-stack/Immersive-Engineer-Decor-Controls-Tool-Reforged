# Small Mineral Smelter Hybrid - v0.5 runtime faceplate

Status: DRAFT GUI IMPLEMENTATION stacked on the Small Mineral Smelter Hybrid PR #132. Not a gameplay, balancing, visual-QA or publication approval.

## Source provenance

- Source identity: IEDCTR-SMELTER-EXACT-REFERENCE-V05, version 0.5.0.
- User-supplied exact screenshot: 1168 x 1346 pixels, SHA-256 d48c7e0ea4ba0851280813166393678a6f7151f6e1e05d3c51ebd55646b6943d. It contains frozen example readings and is not installed as the runtime texture.
- Runtime resource comes from the separate blank live-data image within the user-provided review.html v0.5 bundle (SMELTER_BUNDLE.assets.runtime_base). Its Git blob SHA is 9f5253b8fce3d33121d368eff2f2e320d1db8ed3.
- Runtime resource path: src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/small_mineral_smelter_hybrid_v05/background.png.
- The Git blob identity is not the checksum of the original frozen screenshot.

## Bounded implementation

- Render the 1168 x 1346 runtime image on a 234 x 270 logical canvas.
- Overlay only server-projected thermal, tank, FE, redstone and mode-selection values, never sample screenshot values.
- Preserve the mineral input backing index 0 and product output backing index 2. The historical backing index 1 remains hidden and is not fuel.
- Provide a client-only output drawer; the server output slot remains authoritative.
- Align real inventory 27 and hotbar 9 slot positions to the user artwork (columns 27, 48, 68, 88, 109, 129, 150, 170, 191; rows 171, 189, 207 and 231).
- Keep all gameplay mechanics, balancing parameters, save format, mod version and dependencies unchanged from PR #132.

## Gates and limitations

Validate compilation, resources, unit tests and GameTests at the exact PR head. Client screenshots are still needed at multiple GUI scales for font legibility, output drawer, mode hit targets, shift-click, keyboard input, asset alignment and resource-pack loading. FE/cable, fluid and item automation, save migration and gameplay balancing remain outside this UI-only patch and are NOT_PERFORMED until directly tested.

Using the v0.5 source asset does not prove pixel-perfect rendered output: the UI downsamples the source and paints live overlays. The mechanical contract v0.4 and its numbers remain PROPOSED. This PR must remain Draft, with no merge, tag, release or publication.
