# Immersive Engineer Decor&Controls&Tool Reforged

Reconstructed NeoForge 1.21.1 source project for Immersive Engineer Decor&Controls&Tool Reforged.

This workspace was rebuilt from the published `engineers_decor_reforged-1.1.jar` artifact, with decompiled Java reviewed and repaired into a buildable Gradle project. It includes the recovered resources, Gradle wrapper, source, and GameTest regression coverage for the reconstructed behavior.

## Validation

```powershell
.\gradlew.bat compileJava
.\gradlew.bat runGameTestServer
.\gradlew.bat clean build
```

Latest local validation for `1.1.54-reconstructed`:

- `validateManualResources`, `validateProjectMetadata`, `compileJava`, and the `processResources` datagen-equivalent gate passed.
- Two fresh post-fix `runGameTestServer` runs passed all 182 required GameTests, including exact hatch/sliding-door geometry and a real-player collision sweep; the final `main` CI gate also passed.
- `clean build` passed; JVM unit tests are `NO-SOURCE` because regression coverage is implemented as NeoForge GameTests.
- Runtime JAR size/SHA-256, reproducibility, exact Prism installation, packaged dedicated-server smoke, CI provenance, and external publication readbacks are recorded in `release-evidence/1.1.54-reconstructed`. Prism client world entry is explicitly separated because its loopback incident remains unresolved.
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
