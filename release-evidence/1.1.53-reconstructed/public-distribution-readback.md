# Public distribution readback — 1.1.53-reconstructed

Readback finalized: 2026-08-12T10:43:56Z

## Canonical GitHub release

- status: `PUBLISHED`
- tag: `v1.1.53-reconstructed`
- artifact commit: `dc1f3a33943abc69885f6bec32a3193b9b304341`
- URL: https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/releases/tag/v1.1.53-reconstructed
- JAR: `immersive_engineer_decor_controls_tool_reforged-1.1.53-reconstructed.jar`
- size: `2,670,951` bytes
- SHA-256: `7F357843ACD1E8A9D85D03B979315E3E19058223EC6C10B156375479321BFE98`

The public GitHub release and tag remain unchanged.

## New runtime finding

The user reported the door/hatch sounds as good and the collision boxes as not good while exercising the exact canonical JAR in Prism LAB. Direct source/model comparison identified collision geometry that plausibly explains both reports; the mechanism is classified as `SUPPORTED_INFERENCE` pending a dedicated traversal capture. See `runtime-collision-regression.md`.

## Publication outcome

The external 1.1.53 propagation gate is `FAIL`. No 1.1.53 file was submitted to CurseForge or Modrinth after this finding, no CurseForge metadata save was submitted, and no community reply was posted.

The existing GitHub release is preserved as historical evidence. The runtime correction belongs in a separately versioned and qualified `1.1.54-reconstructed` release.

## Ruflo boundary

- `RUFLO_PRODUCTION_USE`: `NOT_APPLICABLE`
- Ruflo was not activated or used for publication.
- No repository workflow or publication file references Ruflo, and no Ruflo secret was added by this task.
