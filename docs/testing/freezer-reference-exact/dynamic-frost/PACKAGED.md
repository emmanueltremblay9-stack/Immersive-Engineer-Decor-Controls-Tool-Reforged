# Ordinary-JAR opt-in qualification

`freezer-packaged-native.yml` builds one normal production JAR from the exact clean commit, copies it with its SHA-256/source identity, then tests those same bytes in EN 1280×720 and FR 1280×260 (GUI scale 1). Each profile independently rebuilds and rejects a different artifact. No production default changes.

The separate `iedctr_freezer_validation` development module only observes provenance. Runtime excludes main source outputs and registers only that observer as a development mod. The existing bundled world/Freezer fixture is explicitly activated with `iedctr.clientGameplay`, `iedctr.freezerReferenceOnly` and `iedctr.dev.freezerV5LiveEnergy`. This is **not fixture-free**, not default-path qualification and not physical OS input.

Before the world fixture and after its result, the observer verifies normal.jar path/hash, authoritative module root, resource URI, code source, class bytes (screen, geometry, live state, menu, layout, block entity and bundled drivers), and all eight active candidate PNG hashes/pack origins. The independent Python validator rejects changed artifact bytes, resource-pack overrides, source-output provenance, missing/duplicate classes, wrong selectors and leaked observer packages. Existing native inventory, recipe, energy and frost-crop checks remain mandatory.

Run after commit on Linux with native dependencies and Gradle access:

```sh
python tools/validation/freezer_packaged_native.py --runtime-jar /absolute/path/to/normal.jar --language en_us --height 720
```

The runner retains the tested JAR, artifact identity, runtime classpath report, before/after provenance, native logs and captures under build/freezer-packaged-native-*. It refuses dirty checkouts and source changes during execution. The historical fixture invokes screen methods rather than OS input. Phase 0 is idle, phases 1–3 follow the natural recipe, phase 4 resets within the completion tick. No final visual acceptance, release or activation is implied. Native packaged execution remains pending until CI succeeds on this exact change.
