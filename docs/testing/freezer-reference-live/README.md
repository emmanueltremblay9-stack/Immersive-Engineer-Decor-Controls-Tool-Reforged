# Retained Freezer V5: live-energy presentation experiment

This is a bounded local implementation, **not a built or runtime-accepted GUI**. Production defaults, machine mechanics, recipes, NBT, packets, menu ordering and the previous exact-reference delivery remain unchanged.

## Select the comparison mode

- No flag: existing production Freezer screen.
- `-Pdev_freezer_v5_exact_reference=true`: unchanged retained-art snapshot, baked eight-of-eleven energy and baked frost. No dynamic texture patches.
- `-Pdev_freezer_v5_live_energy=true`: same retained V5 geometry, real synchronized FE driving source-derived gauge cells, exact FE tooltip, real synchronized progress/phase/status/redstone tooltip. Frost stays static.
- If both flags are set, exact snapshot wins. Both flags default OFF and are only forwarded to the development client run.
- On Windows use the repository `scripts/invoke-gradle.ps1` property mechanism described in AGENTS.md. No Gradle invocation was made for this change.

## Honest art separation

The original RGB PNG is unchanged: SHA-256 `68c1dc11e7d0e920addebca18100892861a70c5d68db5547b8014235acee3d46`, 1254 × 1254. Runtime keeps the whole source and its existing 314 × 314 virtual canvas. There is no resized replacement and no embedded huge proof JSON.

The energy draw plan samples lit/dark cells from that same original texture. The rounded conservative well polygon excludes the metallic housing. One source-to-canvas pose transform is shared by every source-pixel patch; source boundaries are not rounded separately to canvas integers. The model emits at most 25 patches for these states, before vanilla item rendering.

The eleven cells are quantized with `floor(clampedFE * 11 / capacity)`; 8000/16000 FE therefore shows five fully lit cells, not six. The tooltip retains exact FE, including 12345. The existing `energyDisplayGranularity` helper is intentionally not used because it does not describe the Freezer's raw FE synchronization.

At eight lit cells no substitutions are emitted, so the entire reference is recovered exactly. The ninth cell has a small orange spill baked into its left edge. At all other states, unlit cells are sampled from the genuinely dark middle donor; this removes that spill inside the well. Warm reflections on metal outside the mask remain unchanged. Donor reuse and minor width scaling are explicit source-art recomposition, not recovery of a hidden original layer. Slightly irregular source cell shapes remain a native-visual review item.

## Frost gate and authoritative state

The chamber frost, icicles and three decorative cube icons are baked art. There is no clean chamber, alpha-separated frost layer, or reliable reconstruction of the pixels behind the frost. This change never clears, fades, paints over, or invents those pixels. It never relabels a darkened full-frost picture as zero frost.

`FrostState` prepares the exact read-only state contract. The chamber tooltip explicitly says **STATIC FROST ART**, then presents existing localized menu progress, phase, status and redstone values. `FROST_HIT_REGION` is only the conservative tooltip/model rectangle, not a chamber silhouette or approved erase mask. `frostPatches` is deliberately empty. A separately reviewed clean chamber + aligned transparent frost layer and precise permitted-pixel mask are required for a true live frost rendering pass.

- FE: `MachineMenu.energyStored()` reads raw data[3], capacity getter returns 16000.
- Progress: `freezerProgressTicks()` reads clamped data[1], total uses `MachineBlockEntity.FREEZER_PROCESS_TICKS` (180).
- Phase: `freezerPhase()` reads data[14], clamped 0..4.
- Status/redstone: `freezerStatus()` and `freezerRedstonePowered()` decode data[15].
- These synchronized values are displayed independently. After consuming the last 8 FE, a real transient can be progress=1 and WAITING_FOR_ENERGY. Do not reset it in the renderer.
- Blocked, powered, invalid, empty and no-energy processing paths reset progress on the server. Completion also resets it. Water-bucket completion can be INVALID_INPUT because an empty bucket remains; single ice completion may be IDLE; a remaining valid stack may be READY. No durable 100% frame or client completion latch is invented.

## Reproduce local-only tests

Run `bash tools/validation/check_freezer_reference_live.sh [output-directory]` with Java 21. It compiles only the dependency-free geometry/model and standalone test harness. It does not compile Minecraft classes, use Gradle, access the network, or launch the game.

Run `python tools/validation/check_freezer_reference_live_wiring.py` for explicitly labeled source-contract checks. These are not runtime tests. Existing geometry and event-routing scripts remain applicable.

The CPU renderer consumes the exact production model's rectangles and original PNG using integer nearest-center source blits. It verifies all twelve gauge states, the original eight-cell snapshot and pixel identity outside the conservative mask. The contact sheet shows FE0, FE8000 and FE16000. Source-resolution CPU frames are then downsampled for its thumbnails. That illustrative two-step thumbnail process is not proof of direct GPU sampling at 314px, GUI scales, item interactions, font rendering, shader state, or native frames.

## Passed and still required

Passed locally: pure Java state/mode/overflow/boundary tests, progress1 + waiting transition, no stale 100-FE rounding, no completion latch, source-patch bounds, original SHA, monotonic gauge pixels, zero energized amber pixels inside the empty well, zero changed pixels outside the mask, exact snapshot identity, source-wiring checks, inherited geometry/event checks, Java parsing, whitespace checks, protected-input hashes.

Not run: mapped NeoForge compilation, Gradle/JUnit integration, dedicated server/GameTests, CI, native Minecraft GUI capture, actual tooltip/input/drag/recovery runtime, client/server integration, release artifact. No JAR exists for this change. No Git write, push, upload or publication was performed.

Native review must still verify GUI-scale sampling/seams, tooltips and localized text, all inventory interactions, legacy slot recovery, FE changes, blocked/redstone/no-energy transitions and the preserved static-frost disclosure. This experiment must remain default OFF until those gates pass.
