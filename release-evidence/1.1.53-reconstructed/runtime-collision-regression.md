# Runtime collision regression — 1.1.53-reconstructed

Observed: 2026-08-12 (America/Toronto)
Evidence finalized: 2026-08-12T10:43:56Z

## Result

`FAIL`

The user exercised the Metal Sliding Door and Iron Hatch in the validated Prism LAB installation and reported that the sounds were good but the collision boxes were not good. The installed runtime JAR was then re-read as:

- file: `immersive_engineer_decor_controls_tool_reforged-1.1.53-reconstructed.jar`
- size: `2,670,951` bytes
- SHA-256: `7F357843ACD1E8A9D85D03B979315E3E19058223EC6C10B156375479321BFE98`

This is byte-identical to the canonical GitHub 1.1.53 release asset, so the observation applies to the published artifact rather than a stale LAB installation.

## Direct source readback

The tagged 1.1.53 source has two collision behaviors that plausibly explain the observed problem. The source-to-symptom mapping below is a `SUPPORTED_INFERENCE`; the user's report and exact installed-JAR identity are `CONFIRMED`:

1. `HatchBlock.getCollisionShape` returns the full visible upright plate for an open hatch. Its closed shape is also a top-mounted 2-pixel slab (`Y=14..16`), while the model geometry is a lower plate reaching approximately `Y=0..3`.
2. `SlidingDoorBlock` does not override `getCollisionShape`. Its inherited collision therefore follows `getShape`; the open shape includes floor-track strips at `Y=0..0.25`. Their role in the reported traversal problem is supported by the geometry readback but was not isolated by a dedicated runtime capture.

The 1.1.53 tests encode the first behavior as expected and primarily query `getShape` for the sliding door. They therefore do not protect the runtime traversal behavior reported by the user.

## Reference-only candidate correction

The protected original dirty worktree already contains uncommitted reference changes that independently converge on the same correction:

- open Iron Hatch collision is empty;
- closed Iron Hatch shape/collision is the lower `Y=0..3` plate;
- open Metal Sliding Door collision retains only the side stack and excludes the floor-track strips;
- regression tests distinguish outline shape from collision shape.

Those uncommitted files were read only. They were not copied, altered, staged, built, committed, or published by this closure task.

## Distribution decision

- Do not publish the 1.1.53 JAR to CurseForge or Modrinth.
- Do not post comments claiming the complete 1.1.53 runtime correction is publicly available there.
- Keep the existing annotated Git tag and GitHub release immutable.
- Prepare and qualify a distinct `1.1.54-reconstructed` artifact in a separately authorized implementation task.

Minimum 1.1.54 proof must include targeted collision GameTests for both blocks, the full required GameTest suite, a clean build, exact JAR/install hash equality, and live Prism traversal checks in open and closed states.
