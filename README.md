# Immersive Engineer Decor&Controls&Tool Reforged

Reconstructed NeoForge 1.21.1 source project for Immersive Engineer Decor&Controls&Tool Reforged.

This workspace was rebuilt from the published `engineers_decor_reforged-1.1.jar` artifact, with decompiled Java reviewed and repaired into a buildable Gradle project. It includes the recovered resources, Gradle wrapper, source, and GameTest regression coverage for the reconstructed behavior.

## Validation

```powershell
.\gradlew.bat compileJava
.\gradlew.bat runGameTestServer
.\gradlew.bat clean build
```

Latest local validation before publishing:

- `compileJava` passed.
- `runGameTestServer` passed with 182/182 required GameTests.
- `clean build` passed.
- Latest rebuilt jar observed locally: `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.41-reconstructed.jar`, 2,679,330 bytes, SHA-256 `B295003D72B45B880E36BD41AAD1AEC14E5CDA33F2508E61ACA49900FD44BB95`.
- `scripts/audit-resource-parity.ps1` passed: every deleted `engineers_decor_reforged` resource has an equivalent `immersive_engineer_decor_controls_tool_reforged` replacement, and current resource roots contain no legacy namespace references.
- Prism LAB installation passed as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `B295003D72B45B880E36BD41AAD1AEC14E5CDA33F2508E61ACA49900FD44BB95`.

## Config status

The existing COMMON config flags are registered, but they are not currently used to disable registered blocks, items, menus, or other content. Do not treat them as functional content toggles until that behavior is implemented and validated.

## Optional JEI support

The mod has isolated optional JEI integration for Minecraft 1.21.1 / NeoForge using `mezz.jei:jei-1.21.1-neoforge-api` as `compileOnly` and `mezz.jei:jei-1.21.1-neoforge` as `runtimeOnly`.

- JEI is not shaded or bundled into the rebuilt mod jar.
- JEI metadata is marked `type="optional"` in `META-INF/neoforge.mods.toml`.
- Registered catalysts: Metal Crafting Table for crafting, Small Lab Furnace for smelting, and Small Electrical Furnace for smelting.
- Registered info pages cover the reconstructed engineer tools and the main workshop/fluid/factory machines.
- JEI imports are isolated to `com.oblixorprime.engineersdecorreforged.compat.jei`.

## Notes

See `README_RECONSTRUCTION.md` for reconstruction provenance and `CHANGELOG.md` for release-style repair notes.
