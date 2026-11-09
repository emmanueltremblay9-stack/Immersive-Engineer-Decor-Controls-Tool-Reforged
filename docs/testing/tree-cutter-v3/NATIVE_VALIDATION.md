# Tree Cutter V3 bounded native qualification

## Status and execution

This is a new, unexecuted native qualification fixture until exact-head CI supplies passing evidence. Local Python tests use synthetic proof records and rasterizations solely to test the evidence validator. They are not native screenshots or gameplay qualification.

Run on a clean committed Linux checkout with Java 21, Xvfb, xauth and working Gradle dependency access:

```
python -m unittest discover -s tools/validation -p 'test_tree_cutter*.py' -v
python tools/validation/tree_cutter_v3_native.py --language en_us --width 1280 --height 720 --gui-scale 1
```

The workflow checks out the exact pull-request head, runs JUnit/GameTests, and runs these native profiles:

- English 1280×720, GUI scale 1.
- French 1280×260, GUI scale 1.
- English 1920×1080, GUI scales 1 and 2.
- French 1920×1080, GUI scale 3.

French at 260 framebuffer pixels cannot use actual GUI scale 2 or 3 without violating Minecraft's minimum GUI viewport. The runner rejects silently clamped scales instead of claiming them.

## What this fixture measures

- An isolated nonce-owned fresh survival flat world and physical XTest right-click on the verified machine block raycast open the **normal registered Tree Cutter screen**. It never constructs the production screen directly.
- All 37 transformed slot centers and four outside boundaries are checked against the production screen. Player slots must reference the player's inventory and the exact expected backing indices.
- Each route has an independent synthetic setup containing exactly one oak log. Transformed production screen-handler mouse click/release calls pick it up. Independent integrated-server predicates confirm pickup and total conservation. These are **screen-handler inventory events, not physical mouse inventory input**.
- With the log carried, the production screen handler must report handling the output-slot click attempt. Subsequent client/server snapshots must still show the log carried, output empty and total unchanged, and `mayPlace` must be false. This state was already true before the attempt, so it does **not** prove that a click packet was sent or processed. The proof records `rejected_output_attempts_state_preserved`, explicitly sets `output_packet_processing_observed` to false and rejects the former unsupported `rejected_insertion_server_acks` claim. A dropped/no-op packet is not distinguished by this bounded check.
- The output route returns to a real player slot; all player routes return to their source. All 37 pickups and returns require causal client/server state changes and no world item drops. Output attempts require unchanged state only; their packet handling is not directly observed. Route setup intentionally replaces the preceding synthetic route's one-item inventory; conservation applies within each route, not across 37 separately seeded cases.
- Final player ownership is rechecked after close/reopen with a new menu ID and after **resource reload** with the same reopened menu. Resource reload is not disk/world reload.
- Native screenshots are cropped in memory strictly inside the transformed GUI bounds. No complete framebuffer, world, HUD or save is written as a screenshot or uploaded. Four GUI crops are validated against the original opaque V3 artwork at SHA-256 `bdab10d19c0da44c113f84f1330513f4ff71e81dfe7d3eab602ce27202c0650d`. Broad source-pixel sampling excludes item areas and the energy strip; it accepts only exact source colors within one texel of the expected coordinate, not an RGB tolerance or mismatch quota.

## Artifact identity and isolation

This uses `treeCutterPackagedClient`, not ordinary `runClient`. It extends the established separate Factory Placer test source set. The Gradle verifier forbids production `main.output` on the runtime classpath and requires the normal JAR there. The fixture is absent from the production JAR, and the test mod is a separate module.

`TreeCutterRuntimeProof` verifies the loaded production mod file is exactly the expected JAR and SHA-256, records actual resource and code-source URIs, checks secure-JAR ownership and archived class-byte equality for the mod entry point, Tree Cutter screen, geometry, menu and machine entity. It also checks the active artwork pack and archived/active texture hashes. A source-output runtime fails; merely building or hashing a JAR does not qualify as packaged execution. The runner binds the nonce, exact source SHA, runtime hash, successful exit, completion log marker, viewport, language and evidence. Each matrix job builds its exact-head normal JAR independently; identical cross-job JAR hashes are not asserted.

## Explicit limits

This suite does not qualify actual tree felling, drop generation, high-capacity overflow/recovery, legacy migration, NBT/disk persistence, multiplayer, arbitrary modpacks, drag/shift/double-click inventory gestures, rendered tooltip localization, or owner visual acceptance. Those need their own tests/evidence. Migration and real machine behavior remain the mechanics GameTests' responsibility. This fixture never opens a user's existing world or migrates user data.

The Tree Cutter has no ordinary FE input/storage capability. Setup asserts the underside capability is absent; no FE is injected, and partial/full energy display or consumption is not gameplay-qualified. Adding FE behavior is outside this change.

The local cloud could not compile mapped Java or launch Minecraft: a process-local writable Gradle cache reached the wrapper download, which failed with `Network is unreachable` for Gradle 9.2.1. Exact-head CI must compile and execute the harness before any native pass is claimed. No merge, release, or publication is performed by this workflow.
