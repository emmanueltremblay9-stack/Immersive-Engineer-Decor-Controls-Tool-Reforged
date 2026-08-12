# Immersive Engineer Decor&Controls&Tool Reforged

Reconstructed NeoForge 1.21.1 source project for Immersive Engineer Decor&Controls&Tool Reforged.

This workspace was rebuilt from the published `engineers_decor_reforged-1.1.jar` artifact, with decompiled Java reviewed and repaired into a buildable Gradle project. It includes the recovered resources, Gradle wrapper, source, and GameTest regression coverage for the reconstructed behavior.

## Validation

```powershell
.\gradlew.bat compileJava
.\gradlew.bat runGameTestServer
.\gradlew.bat clean build
```

Latest local validation for `1.1.53-reconstructed`:

- `validateManualResources`, `validateProjectMetadata`, `compileJava`, and the `processResources` datagen-equivalent gate passed.
- `runGameTestServer` passed with all 179 required GameTests in repeated local and CI runs.
- `clean build` passed; JVM unit tests are `NO-SOURCE` because regression coverage is implemented as NeoForge GameTests.
- Runtime jar: `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.53-reconstructed.jar`, 2,670,951 bytes, SHA-256 `7F357843ACD1E8A9D85D03B979315E3E19058223EC6C10B156375479321BFE98`.
- Two clean local builds and the downloaded CI artifact produced the same runtime-JAR SHA-256.
- The verified JAR was installed into the NeoForge 1.21.1 Prism LAB, exercised in client smoke tests, and started/stopped cleanly in a dedicated-server smoke test.
- `validateManualResources` rejects missing manual pages and crafting widgets that point at non-crafting recipes.
- `validateProjectMetadata` checks version parity, public support links, issue templates, and root/packaged attribution parity.
- The normal build workflow does not modify Prism; release qualification uses an explicit hash-verified LAB installation procedure.

## Config status

The COMMON config flags keep registry content stable and control which groups are shown in the mod's Creative tab. They do not remove registered blocks, items, menus, or saved-world content.

## Optional JEI support

The mod has isolated optional JEI integration for Minecraft 1.21.1 / NeoForge using `mezz.jei:jei-1.21.1-neoforge-api` as `compileOnly` and `mezz.jei:jei-1.21.1-neoforge` as `runtimeOnly`.

- JEI is not shaded or bundled into the rebuilt mod jar.
- JEI metadata is marked `type="optional"` in `META-INF/neoforge.mods.toml`.
- Registered catalysts: Metal Crafting Table for crafting, Small Lab Furnace for smelting, and Small Electrical Furnace for smelting.
- Registered info pages cover the reconstructed engineer tools and the main workshop/fluid/factory machines.
- JEI imports are isolated to `com.oblixorprime.engineersdecorreforged.compat.jei`.

## Bug reports

Report problems through [this fork's GitHub Issues form](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/issues/new/choose). Include the exact mod, Minecraft, NeoForge, Java, and Immersive Engineering versions; reproduction steps; expected and actual behavior; the complete mod list; `latest.log` or `debug.log`; and any crash report.

This is an unofficial reconstruction and fusion fork. Please do not send fork-specific defects to the original Engineer's Decor, RsGauges, Engineer's Tools, or Immersive Engineering authors.

## Licensing and provenance

This repository is distributed under the MIT License. See `LICENSE`, `NOTICE.md`, and `CREDITS.md` at the repository root; byte-equivalent copies are packaged under `META-INF` in release jars. Reconstruction provenance and the recovered base-artifact hash are recorded in `README_RECONSTRUCTION.md`.

## Notes

See `README_RECONSTRUCTION.md` for reconstruction provenance and `CHANGELOG.md` for release-style repair notes.
