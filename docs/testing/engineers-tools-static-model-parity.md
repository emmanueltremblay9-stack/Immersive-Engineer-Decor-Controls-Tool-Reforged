# Engineer's Tools static item model parity

These four resource models are restored from [stfwi/engineers-tools](https://github.com/stfwi/engineers-tools/tree/2c4debb672f1a70015fb058256d21550f9ffe37f), commit `2c4debb672f1a70015fb058256d21550f9ffe37f` (Engineer's Tools 1.1.6 / Minecraft 1.16.5).

The [upstream MIT license](https://github.com/stfwi/engineers-tools/blob/2c4debb672f1a70015fb058256d21550f9ffe37f/license), blob `acee935363d5e3136f2e89a287023f40677f303d`, is Copyright (c) 2018-2020 Stefan Wilhelm (wile). Existing root and packaged LICENSE, NOTICE.md and CREDITS.md retain that copyright, permission notice and source attribution unchanged.

| Item | Upstream model Git blob | Unchanged PNG Git blob |
| --- | --- | --- |
| diving_capsule | `d3676f8eaac86902ac05ef2dcc35e98ae7942a9c` | `696a1a80f756aecc264b46868eba80d87a85e653` |
| material_box | `a78dc81fc07325e96b49ca691bdbbef1592ee2e0` | `0c8446c4638ddca67fd62a6d5ebbd3677732c601` |
| musli_bar_press | `07b544b9571a1f796fd7d03a8e65a57aec326382` | `cd94fe28d72199df78e7d5fb9c7c3571e0093d78` |
| crushing_hammer | `c5a41eb8d9ccd4c2a79a090e4ab71f92ae679ab6` | `23fab9c877f06602768edac3ca69f0289f37878b` |

Source paths are `src/main/resources/assets/engineerstools/models/item/<item>.json` and `textures/item/<item>.png` under the same assets namespace. The four files in `tools/validation/fixtures/engineers_tools_1_1_6/` preserve the exact upstream JSON bytes as test references.

Runtime models replace only `engineerstools:` with `immersive_engineer_decor_controls_tool_reforged:` in texture references. Capsule (7 elements), Material Box (22) and Muslee Bar Press (11) retain every element, face, UV, rotation, lighting property and display transform, without adding a parent or left-hand transforms absent upstream. Crushing Hammer retains `item/handheld` and all four first/third-person left/right transforms. No texture is replaced.

## Automated validation

`./gradlew validateToolsItemModels` builds the runtime JAR and verifies each pinned fixture's Git blob SHA-1, full parsed JSON equality, exact namespace-only text substitution, unchanged PNG bytes, and one exact model/texture entry in the runtime JAR. `check` depends on this task, so the existing CI `build` step executes it. Four `TOOLS_MODEL_PARITY_PASS` receipts identify the validated models and packaging.

These checks establish static resource and packaging parity. They do not qualify the user's Windows rendering, first/third-person appearance or left-hand fallback on Minecraft 1.21.1. That visual check remains outstanding. No Java gameplay, recipe, durability, GUI, dormant feature, sign model or fluid behavior is changed. Tracker rendering is a separate port.
