# Immersive Engineer Decor&Controls&Tool Reforged

Reconstructed NeoForge 1.21.1 source project for Immersive Engineer Decor&Controls&Tool Reforged.

This workspace was rebuilt from the published `engineers_decor_reforged-1.1.jar` artifact, with decompiled Java reviewed and repaired into a buildable Gradle project. It includes the recovered resources, Gradle wrapper, source, and GameTest regression coverage for the reconstructed behavior.

## Validation

```powershell
.\gradlew.bat compileJava
.\gradlew.bat runGameTestServer
.\gradlew.bat clean build
```

Latest qualified and publicly released version: `1.1.56-reconstructed`.

- The [GitHub release](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/releases/tag/v1.1.56-reconstructed) and [CurseForge file 9042960](https://www.curseforge.com/minecraft/mc-mods/immersive-engineers-decor-controls-tools-reforged/files/9042960) are public and independently hash-verified. Both serve the same 19,568,379-byte runtime JAR: SHA-256 `fa7bde923433658a78dcf3792c9fc6af2e0b5212a7897e1b21c3323e7af809a4`.
- Release/tag commit `527b6d1afbcb283d6ff08cc40bc7a92ec229c399` has qualified source tree `963b564f803f8537fa89033a5b055a7d8e2aa72c`. Later publication-tooling and documentation commits on `main` are maintenance state, not the source identity of the published JAR.
- Qualification passed all 189 required NeoForge GameTests, localization/manual/metadata/sign validators, and two byte-identical clean builds. The release publisher suite passed 31/31 mocked tests; after [PR #31](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/31), the current publication-tooling suite passes 32/32.
- CurseForge declares Immersive Engineering project `231951` as the sole `RequiredDependency`; JEI remains optional. The three fused source projects are provenance, not install dependencies.
- Modrinth publication remains `BLOCKED_BY_MISSING_CONFIGURATION`; no project or dependency identifier was guessed.
- Native fluency across 120 locales, exhaustive visual coverage and arbitrary UI-scale tooltip geometry remain unverified. Earlier sampled Hindi/Arabic/Thai rendering limitations remain recorded, with exact component attribution unverified. No new client launch, Prism installation or packaged dedicated-server qualification is claimed for 1.1.56.
- Qualification and publication readbacks are recorded in [the 1.1.56 evidence summary](release-evidence/1.1.56-reconstructed/SUMMARY.md), including the separate release and maintenance source identities.
- `validateManualResources` rejects missing manual pages and crafting widgets that point at non-crafting recipes.
- `validateProjectMetadata` checks version parity, public support links, issue templates, and root/packaged attribution parity.
- The normal build workflow does not modify Prism; an installation requires its own verified procedure.

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
