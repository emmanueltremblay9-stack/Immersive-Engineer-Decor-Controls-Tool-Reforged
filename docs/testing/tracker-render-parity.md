# Tracker renderer port

Source: Engineer's Tools 1.1.6 / Minecraft 1.16.5, [stfwi/engineers-tools at 2c4debb672f1a70015fb058256d21550f9ffe37f](https://github.com/stfwi/engineers-tools/tree/2c4debb672f1a70015fb058256d21550f9ffe37f). MIT, Copyright (c) 2018-2020 Stefan Wilhelm (wile). Existing root and packaged LICENSE, NOTICE.md and CREDITS.md retain attribution and permission notices.

| Source | Git blob | Use |
| --- | --- | --- |
| detail/ModRenderers.java | b37b8d1537112b7815122c13fc6c77d0e5d02e47 | Adapt TrackerIster render order, transforms, neutral pose and first-person exclusion |
| items/TrackerItem.java | fa955313670b8573050d894e0f0802267c7183c6 | Adapt only client display-angle formula and near-target retention |
| models/item/tracker.json | bcee4bd256c9dfbd9560c0921ea721af71b7040e | Exact JSON except texture namespace |
| models/item/tracker_model.json | 3526404b4a5b1e2ca4ea1085fe5a32c3de8aa78d | Exact JSON except texture namespace |
| models/item/tracker_pointer_model.json | 149dd22819a7861fbfa044fc9b1c55dec1018729 | Exact JSON except texture namespace |
| textures/item/tracker.png | d181d51b583819bf1f8e223be0a15a143b041cf3 | Existing bytes retained |
| textures/item/tracker_pointer_back.png | c9ea1afd61b573f409cfe711c85364a232e6c7f8 | Exact missing upstream asset added |
| textures/item/tracker_pointer_front.png | 64061f3b791b5871be115e123ab788e773f2075b | Exact missing upstream asset added |
| textures/item/tracker_pointer_side1.png | 236c0982f3326fb3871b5776724b901089e2381d | Exact missing upstream asset added |
| textures/item/tracker_pointer_side2.png | fcf17893fdebf73a1356d31d285d4f0b8fc52831 | Exact missing upstream asset added |
| textures/item/tracker_side.png | 61c1b087546b40b1ffb3ae13e7a42ec88b382d6f | Exact missing upstream asset added |

Java paths above are relative to `src/main/java/wile/engineerstools/`; resources are relative to `src/main/resources/assets/engineerstools/`. The original JSON fixtures are preserved under `tools/validation/fixtures/tracker_1_1_6/`.

## Preserved rendering contract

The item uses `builtin/entity`, the original 8-element body and 13-element pointer, all UVs and display transforms, and all six original textures. The renderer draws the body first, rotates the pointer around (0.5, 0.5, 0.5), uses the original 0.6 scale, and shows the original 180-degree neutral orientation when no valid target is available.

Both FIRST_PERSON_LEFT_HAND and FIRST_PERSON_RIGHT_HAND return before drawing, exactly as TrackerIster did. No new first-person appearance is invented. The pointer uses the original angle formula, integer quantization, vertical cone and 30-degree pitch dead zone. Near-target retention is transient and keyed weakly by the live stack; no persistent random id is created.

## Required 1.21.1 adaptation

[NeoForge's BEWLR extension event](https://docs.neoforged.net/docs/1.21.1/blockentities/ber/#blockentitywithoutlevelrenderer) replaces ISTER registration. [Standalone additional models](https://docs.neoforged.net/docs/1.21.1/resources/client/models/#registering-additional-models) replace legacy special inventory model registration. Model lookup happens on every render so resource reloads do not retain obsolete baked models.

The renderer reads the existing CUSTOM_DATA keys `target_dimension`, `target_x`, `target_y`, `target_z`, accepting only complete correctly typed coordinates and a valid dimension. It never writes stack data. Missing/malformed targets and targets outside the current dimension render neutrally. No legacy tracker id, trackerdata compound, playerpos write, entity tracking, server tick, world scan, item interaction or saved-data migration is restored. The existing TrackerItem.java, recipe, tooltip, registration and gameplay remain unchanged.

The inspected Tracker item history contains the reconstructed import and later tooltip/data validation fixes; no contrary first-person rendering contract was found. This observation does not certify unknown external design decisions.

## Validation and limits

- Five JUnit tests cover original cardinal yaw/wrapping, vertical cone/dead zone, near-target retention signal and non-finite input rejection. The packaging task requires a real XML report with all five executed and none skipped.
- `./gradlew validateTrackerModels` verifies pinned original fixture blobs, full parsed JSON and exact namespace-only equality, all six PNG blob hashes and all nine model/texture entries in the runtime JAR. Existing `check` invokes this task.
- The existing nonce-guarded isolated client fixture calls the real ItemRenderer entry point, verifies both auxiliary models and their sprites, all nine display contexts (zero buffers/vertices in both first-person views), malformed/foreign targets, pointer motion with unchanged body geometry, balanced pose stacks and unchanged item components. Thirteen per-case receipts are required by the Python harness in every client profile; evidence is retained as client-tracker-render-result.json.

These are executable model, packaging and client render-path checks, not a screenshot-based qualification of the user's Windows client. Appearance and visual inspection on that client remain outstanding. No Prism files, local worlds or release are changed.
