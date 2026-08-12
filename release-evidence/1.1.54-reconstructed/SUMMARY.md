# Release evidence summary — 1.1.54-reconstructed

Status at evidence-pack creation: engineering implementation is green in the first post-fix GameTest run; repeated validation, runtime, CI, and publication readbacks remain pending until their gates execute.

- Trigger: user-observed collision boxes were not good while exercising the exact canonical `1.1.53-reconstructed` JAR in Prism LAB. Audio remains `AUDIO_USER_OBSERVED: GOOD` and was not recaptured.
- Registry IDs: `immersive_engineer_decor_controls_tool_reforged:iron_hatch` and `immersive_engineer_decor_controls_tool_reforged:metal_sliding_door`.
- Objective 1.1.53 reproduction: the canonical 2,670,951-byte JAR has SHA-256 `7F357843ACD1E8A9D85D03B979315E3E19058223EC6C10B156375479321BFE98`; the final test-only RED run executed 182 tests and failed exactly four new collision tests while the other 178 passed.
- Root cause: hatch shape coordinates/thickness disagreed with its models; sliding-door outline/collision ignored `HALF`, ignored hinge-specific model rotation in some states, and treated decorative tracks as open movement collision.
- Fix: immutable state-aware hatch shapes and cached model-derived sliding-door shapes, with open movement collision limited to solid retracted components.
- First post-fix result: all 182 required GameTests passed.
- Ruflo: `NOT_APPLICABLE`; not activated.
- Modrinth: `BLOCKED_BY_MISSING_CONFIGURATION`; templates only, no project creation.

Raw logs, screenshots, and runtime backups are retained outside Git. This directory records compact non-secret evidence and hashes.
