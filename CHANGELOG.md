# Changelog

All notable reconstruction, repair, and validation changes for this project are recorded here.

## [1.1.53-reconstructed] - 2026-08-11

### Validation

- Confirmed `validateManualResources` passes for 223 manual sources and 424 named crafting widgets.
- Confirmed `validateProjectMetadata`, `compileJava`, and `processResources` pass.
- Confirmed `runGameTestServer` passes with all 179 required GameTests in three consecutive post-review runs.
- Confirmed `clean build` passes; the JVM unit-test task is `NO-SOURCE`.
- Confirmed a local development-client smoke test opens the EDCT Engineer's Tools index and renders both Coarse Iron Grit and Coarse Gold Grit pages; the resulting `latest.log` contains no error, exception, `ManualRecipeRef`, or manual-initialization failure matches.
- Confirmed the runtime jar is 2,670,995 bytes with SHA-256 `CD92F95870EFE059775FD600C6619C2B355C7C87BF4A66A2CEE852FB54CF0735`.
- Confirmed the built jar contains version `1.1.53-reconstructed`, mod id `immersive_engineer_decor_controls_tool_reforged`, matching MIT attribution, safe grit manual widgets, and all 199 mining-tool assignments.
- `runData` is not exposed by this reconstruction; `processResources` plus the two deterministic resource validators were used as the non-mutating equivalent.
- No Prism installation was performed; the client check used the isolated Gradle development runtime only.

### Fixed

- Replaced the two Engineer's Manual widgets that incorrectly cast smelting recipes as crafting recipes, while preserving the legacy Iron Grit and Gold Grit pages.
- Restored active Small Solar Panel FE output to receivers below or on the four horizontal sides, with one shared 256 FE transfer budget and no night generation.
- Restored iron open/close sounds and game events for the Metal Sliding Door and Iron Hatch while preserving hand operation.
- Added conventional pickaxe, axe, and shovel tags for every public block so normal vanilla tools receive correct speed and drop behavior instead of leaving REDIA as the practical universal option.

### Added

- Added exhaustive GameTests for creative-item routing, conventional mining tools, material sound families, solar output accounting, and sided FE behavior.
- Added deterministic manual-resource and repository-metadata validators to the Gradle `check` lifecycle.
- Added root MIT attribution files, GitHub issue forms, README support guidance, and NeoForge issue/repository metadata.
- Added CI execution of the dedicated GameTest server.

### Changed

- Synchronized release metadata to `1.1.53-reconstructed`.

## [1.1.41-reconstructed] - 2026-07-09

### Validation

- Confirmed `compileJava` passes after adding isolated optional JEI support.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.41-reconstructed.jar`.
- Confirmed `runGameTestServer` passes with all 182 required GameTests.
- Confirmed the rebuilt jar is 2,679,330 bytes with SHA-256 `B295003D72B45B880E36BD41AAD1AEC14E5CDA33F2508E61ACA49900FD44BB95`.
- Confirmed JEI imports are isolated to `com.oblixorprime.engineersdecorreforged.compat.jei`.
- Confirmed the rebuilt jar does not bundle JEI classes and marks JEI as an optional NeoForge dependency.

### Added

- Added optional JEI integration using `mezz.jei:jei-1.21.1-neoforge-api` as `compileOnly` and `mezz.jei:jei-1.21.1-neoforge` as `runtimeOnly`.
- Registered JEI catalysts for the Metal Crafting Table, Small Lab Furnace, and Small Electrical Furnace.
- Added JEI ingredient info pages for the reconstructed engineer tools and the main workshop, fluid, power, and factory machines.
- Added safe JEI smelting click areas for the Small Lab Furnace and Small Electrical Furnace screens.

### Changed

- Declared JEI as an optional NeoForge dependency instead of a required runtime dependency.

## [1.1.28-reconstructed] - 2026-06-28

### Validation

- Confirmed `compileJava` passes.
- Confirmed `runGameTestServer` passes with all 167 required GameTests.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.28-reconstructed.jar`.
- Confirmed the rebuilt jar is 2,653,649 bytes with SHA-256 `68A1168AF9A115ED84291586FD2675E1FF33A85A7280654DCB196DFC299D8FDA`.
- Confirmed `scripts/audit-resource-parity.ps1` passes: all deleted `engineers_decor_reforged` resources have current namespace replacements and current resource roots contain no legacy namespace references.
- Did not run `install-mod.ps1` for this release prep.

### Fixed

- Fixed Metal Sliding Doors so adjacent matching doors form a bounded two-door pair instead of behaving like an unlimited connected redstone network.
- Synced paired Metal Sliding Door `OPEN` and `POWERED` state server-side while keeping the lower half as the logical controller and the upper half as a mirror.
- Added GameTest coverage for two-door pairing, toggling from either paired door, and redstone behavior that does not chain past the pair.

### Changed

- Added a lightweight resource parity audit script for the namespace migration.
- Documented that existing COMMON config flags are not currently used to disable registered content.
- Synchronized user-visible mod version metadata to `1.1.28-reconstructed`.

## [1.1.27-reconstructed] - 2026-06-28

### Validation

- Confirmed the new comparator-switch regression fails before the fix when a directional repeater powers the attached block.
- Confirmed `runGameTestServer` passes with all 164 required GameTests after the comparator-switch direction fix.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.27-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `300ACD992BD3DB1ECA3A24BC1534FB7984D3B4C71C83D3485A65303FCBD26DFE`.

### Fixed

- Fixed industrial comparator switches reading directional redstone from neighbors around the attached block. Repeaters and other directional outputs now power the backing block from the correct side instead of being ignored.
- Added GameTest coverage for directional repeater power into the comparator switch backing block.
- Synchronized user-visible mod version metadata to `1.1.27-reconstructed`.

## [1.1.26-reconstructed] - 2026-06-27

### Validation

- Confirmed `compileJava` passes through the full GameTest server run after the small block breaker full-buffer state fix.
- Confirmed `runGameTestServer` passes with all 163 required GameTests, including full drop-buffer small block breaker active-state coverage.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.26-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `706169F227E1F41CF407E348680FA8CFD1EDE044ECC94FC568F07D46EAC3F450`.

### Fixed

- Fixed small block breaker completion against a full internal drop buffer so the target block stays intact, work progress is cleared, the active block state is cleared, and the machine backs off instead of immediately restarting active work.
- Added GameTest coverage for full drop-buffer small block breaker behavior.
- Synchronized user-visible mod version metadata to `1.1.26-reconstructed`.

## [1.1.25-reconstructed] - 2026-06-20

### Validation

- Confirmed `compileJava` passes after the factory placer comparator refresh fix.
- Confirmed `runGameTestServer` passes with all 162 required GameTests, including blocked factory placer spit-out comparator coverage.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.25-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `2EF85F5D6F8B932C734EC0767D3EB5127224C382A28FC55DEA2FB28C78C7CD92`.

### Fixed

- Fixed factory placer failed-placement spit-out inventory updates so adjacent comparators refresh after a manual or redstone-edge blocked placement ejects stock.
- Added GameTest coverage for blocked factory placer spit-out dropping comparator output back to zero after the last stock item is ejected.
- Synchronized user-visible mod version metadata to `1.1.25-reconstructed`.

## [1.1.24-reconstructed] - 2026-06-20

### Validation

- Confirmed `compileJava` passes after the dimmer click-axis fix.
- Confirmed `runGameTestServer` passes with all 161 required GameTests, including floor-mounted dimmer analog output coverage.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.24-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `0540AE636E2A405B62E89D6E30894B552345023F7390FF8BD971D11A6074DA4D`.

### Fixed

- Fixed floor- and ceiling-mounted industrial dimmers so their analog output is selected along the panel travel axis instead of the panel thickness axis.
- Added GameTest coverage for floor-mounted dimmer low and high click positions.
- Synchronized user-visible mod version metadata to `1.1.24-reconstructed`.

## [1.1.23-reconstructed] - 2026-06-20

### Validation

- Confirmed `compileJava` passes after the saved machine-settings NBT type guard.
- Confirmed `runGameTestServer` passes with all 160 required GameTests, including malformed saved default-field coverage for factory hopper, factory dropper, factory placer, and small electrical furnace settings.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.23-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `0DD8152E0D7EAFDD85C3316D813F1000C1FDC7912812B5F95721926CF6F265FF`.

### Fixed

- Fixed defaulted machine settings so malformed non-integer saved NBT values for hopper logic, dropper speed/count/logic, placer logic, and electrical furnace speed fall back to their intended defaults instead of loading as zero.
- Added GameTest coverage for malformed saved machine setting tags.
- Synchronized user-visible mod version metadata to `1.1.23-reconstructed`.

## [1.1.22-reconstructed] - 2026-06-19

### Validation

- Confirmed `compileJava` passes after the saved target-data validation fix.
- Confirmed `runGameTestServer` passes with all 159 required GameTests, including malformed Tracker and Switch Link Pearl target-data coverage.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.22-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `3B34F6ADB953E2D0E1263777D80654CEBBE84C4E77DBA041EE158D0816A325C4`.

### Fixed

- Fixed Tracker and Switch Link Pearl saved target validation so malformed coordinate tags are ignored instead of being interpreted as fallback origin coordinates.
- Added GameTest coverage for malformed Tracker tooltip data and malformed Switch Link Pearl block use.
- Synchronized user-visible mod version metadata to `1.1.22-reconstructed`.

## [1.1.21-reconstructed] - 2026-06-19

### Validation

- Confirmed `compileJava` passes after the labeled crate sanitizer fix.
- Confirmed `runGameTestServer` passes with all 157 required GameTests, including Unicode control-character sanitization coverage for labeled crate labels and label packets.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.21-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `E0A54D588F09BE04E11B6E8838EED941BE7817C9E1643F9F6CFAEAF0674345C7`.

### Fixed

- Fixed labeled crate label sanitization so C1 Unicode control characters are stripped instead of being saved or sent through the label packet.
- Added GameTest coverage for direct sanitizer behavior and network payload sanitization.
- Synchronized user-visible mod version metadata to `1.1.21-reconstructed`.

## [1.1.20-reconstructed] - 2026-06-19

### Validation

- Confirmed `compileJava` passes after the contact-control placement guard.
- Confirmed `runGameTestServer` passes with all 156 required GameTests, including floor contact-control placement rejection coverage.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.20-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `23F38BA17C48DEC1F14E85DFA38CFF9F55CB08D69A3E4D1F7DAAA9CF52FFCF17`.

### Fixed

- Fixed floor contact controls so contact mats, sensitive trapdoors, fall-through detectors, and power plants reject wall or ceiling placement that would leave their horizontal model/collision panel detached from the clicked support.
- Added GameTest coverage for top-face placement and wall/ceiling rejection across the affected contact-control families.
- Synchronized user-visible mod version metadata to `1.1.20-reconstructed`.

## [1.1.19-reconstructed] - 2026-06-19

### Validation

- Confirmed `runGameTestServer` passes with all 155 required GameTests, including open Steel Mesh Fence Gate collision coverage.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.19-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `90D34298EEA82BC835D31DE5761351AE5F730A123A962A587C458E88310FEB42`.

### Fixed

- Fixed Steel Mesh Fence Gate collision so opened gates clear the center passage instead of retaining the closed center-panel hitbox.
- Added GameTest coverage for open Steel Mesh Fence Gate collision in north and rotated east orientations.
- Synchronized user-visible mod version metadata to `1.1.19-reconstructed`.

## [1.1.18-reconstructed] - 2026-06-19

### Validation

- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.18-reconstructed.jar`.
- Confirmed `runGameTestServer` passes with all 154 required GameTests, including cased Switch Link receiver full-block collision coverage.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `BB92EEC8B0FA6A766299DBFD9DFB6126173396BEADA173ACFC715B411A5EEA51`.

### Fixed

- Fixed cased Switch Link receiver variants so they behave as full blocks instead of surface-mounted controls that drop when backing support is removed.
- Added GameTest coverage for full-block survival and collision on cased Switch Link receiver variants.
- Synchronized user-visible mod version metadata to `1.1.18-reconstructed`.

## [1.1.17-reconstructed] - 2026-06-19

### Validation

- Confirmed `runGameTestServer` passes with all 153 required GameTests, including creative Ariadne Coal placement coverage.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.17-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `CA7F3353BB1850E5C8653528FA3072474286419B9CB5B7D0C080E54A5EC8CF36`.

### Fixed

- Fixed Ariadne Coal marker placement so creative/instabuild use does not wear or consume the item.
- Added GameTest coverage for creative Ariadne Coal placement at the final-durability edge.
- Synchronized user-visible mod version metadata to `1.1.17-reconstructed`.

## [1.1.16-reconstructed] - 2026-06-18

### Validation

- Confirmed `runGameTestServer` passes with all 152 required GameTests, including blocked REDIA Tool farmland/path conversion coverage.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.16-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `F3BC8BA3A9D19CF23C5C34DAEEAF86B47A9A21B1383546B14730313D65249C1C`.

### Fixed

- Fixed REDIA Tool sneak ground-cycling so air-sensitive farmland/path conversions require air above.
- Added GameTest coverage for blocked REDIA Tool farmland/path conversion.
- Synchronized user-visible mod version metadata to `1.1.16-reconstructed`.

## [1.1.15-reconstructed] - 2026-06-18

### Validation

- Confirmed `runGameTestServer` passes with all 151 required GameTests, including hatch, powered trapdoor panel, slab, and slab-slice collision coverage.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.15-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `B417DDFE59FCFDC3CA0BE0F419FEAFBB68F5C1A1A6D3907F6419ADA98FEF2946`.

### Fixed

- Fixed visible hatch and trapdoor-style panels so their collision shape matches the rendered plate instead of becoming pass-through while still visibly blocking space.
- Added GameTest coverage for open iron hatch side-plate collision, powered control trapdoor panel collision, vanilla-style slab collision, and custom slab-slice collision.
- Synchronized user-visible mod version metadata to `1.1.15-reconstructed`.

## [1.1.14-reconstructed] - 2026-06-18

### Validation

- Confirmed `runGameTestServer` passes with all 149 required GameTests, including `legacy_registry_ids_alias_to_renamed_mod_namespace`.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.14-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `62B9CEC8F657791DAEE91FC58085A9B68CD9B9FA527D40839AE7F53D7162CEB2`.

### Fixed

- Added NeoForge registry aliases from the legacy `engineers_decor_reforged` namespace to `immersive_engineer_decor_controls_tool_reforged` so old world block/item ids, the machine block entity, menus, and the REDIA repair recipe serializer resolve after the mod id rename.
- Added GameTest coverage proving representative legacy block, item, block entity, menu, and recipe serializer ids alias to the renamed registry entries.
- Synchronized user-visible mod version metadata to `1.1.14-reconstructed`.

## [1.1.13-reconstructed] - 2026-06-18

### Validation

- Confirmed `runGameTestServer` passes with all 148 required GameTests under mod id `immersive_engineer_decor_controls_tool_reforged`.
- Confirmed `clean build` passes and produces `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.13-reconstructed.jar`.
- Confirmed the rebuilt and installed jars contain `META-INF/neoforge.mods.toml` metadata for mod id `immersive_engineer_decor_controls_tool_reforged`, version `1.1.13-reconstructed`, and display name `Immersive Engineer Decor&Controls&Tool Reforged`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance as `Immersive Engineer Decor&Controls&Tool Reforged.jar` with matching SHA-256 `35D154D6062F4A71532725CE5A69049BC06A733D37A8C0D1DC2C1AEA6724FF43`.

### Changed

- Migrated the internal NeoForge mod id/resource namespace from `engineers_decor_reforged` to `immersive_engineer_decor_controls_tool_reforged`.
- Updated the mod display name to `Immersive Engineer Decor&Controls&Tool Reforged`.
- Extended `install-mod.ps1` with `-TargetJarName` so Prism LAB installs can keep the friendly jar filename `Immersive Engineer Decor&Controls&Tool Reforged.jar`.
- Synchronized user-visible mod version metadata to `1.1.13-reconstructed`.

## [1.1.12-reconstructed] - 2026-06-18

### Validation

- Confirmed `runGameTestServer` passes with all 148 required GameTests.
- Confirmed `clean build` passes and produces `build/libs/engineers_decor_reforged-1.1.12-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance with matching SHA-256 hashes.

### Fixed

- Quoted Windows Gradle wrapper path variable assignments so `gradlew.bat` works from checkout paths containing `&`.
- Synchronized user-visible mod version metadata to `1.1.12-reconstructed`.

## [1.1.11-reconstructed] - 2026-06-18

### Validation

- Confirmed `runGameTestServer` passes with all 148 required GameTests.
- Confirmed `clean build` passes and produces `build/libs/engineers_decor_reforged-1.1.11-reconstructed.jar`.
- Confirmed the rebuilt jar contains the restored REDIA Tool 3D item model, texture, REDIA tool class, and metadata version `1.1.11-reconstructed`.

### Fixed

- Restored the original MIT Engineer's Tools REDIA Tool 3D item model instead of using the flat generated item model.
- Corrected REDIA Tool durability-based Efficiency and Fortune decay curves to the effective original 1.16 config defaults.
- Synchronized user-visible mod version metadata to `1.1.11-reconstructed`.

### Added

- Added GameTest coverage for REDIA Tool durability-dependent Efficiency/Fortune curve thresholds.

## [1.1.10-reconstructed] - 2026-06-17

### Validation

- Confirmed `runGameTestServer` passes with all 147 required GameTests.
- Confirmed `clean build` passes and produces `build/libs/engineers_decor_reforged-1.1.10-reconstructed.jar`.
- Confirmed the rebuilt jar metadata reports `engineers_decor_reforged` version `1.1.10-reconstructed`.

### Fixed

- Guarded machine menu shift-click handling against negative and out-of-range slot indices so malformed menu input returns `ItemStack.EMPTY` instead of throwing from `this.slots.get(index)`.
- Synchronized user-visible mod version metadata to `1.1.10-reconstructed`.

### Added

- Added GameTest coverage for invalid machine menu quick-move indices.

## [1.1.9-reconstructed] - 2026-06-17

### Validation

- Confirmed `runGameTestServer` passes with all 146 required GameTests.
- Confirmed `clean build` passes and produces `build/libs/engineers_decor_reforged-1.1.9-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance with matching SHA-256 hashes.

### Changed

- Added Immersive Engineering Industrial Hemp Seeds (`immersiveengineering:seed`) to `engineers_decor_reforged:musli_bar_press_seeds`.
- Muslee Bar Press inventory use and both Muslee recipes now accept Industrial Hemp Seeds through the existing shared accepted-seed tag.
- Updated Muslee Bar Press tooltip and manual text to mention hemp seeds.
- Synchronized user-visible mod version metadata to `1.1.9-reconstructed`.

### Added

- Extended Muslee Bar Press seed GameTest coverage to include the Immersive Engineering hemp seed item.

## [1.1.8-reconstructed] - 2026-06-17

### Validation

- Confirmed `runGameTestServer` passes with all 146 required GameTests.
- Confirmed `clean build` passes and produces `build/libs/engineers_decor_reforged-1.1.8-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance with matching SHA-256 hashes.

### Fixed

- Restored Muslee Bar Press seed parity from Engineer's Tools metadata and readme:
  - Inventory use now accepts wheat, melon, pumpkin, and beetroot seeds instead of wheat seeds only.
  - The Muslee Bar shapeless recipe and Muslee Bar Press shaped recipe now share the same accepted-seed item tag.
  - Output-space simulation now treats any accepted seed as the consumed seed slot, preserving the atomic no-loss behavior for full inventories.
- Updated Muslee Bar Press tooltip and manual text so the documented seed options match runtime behavior.
- Synchronized user-visible mod version metadata to `1.1.8-reconstructed`.

### Added

- Added the `engineers_decor_reforged:musli_bar_press_seeds` item tag.
- Added GameTest coverage for melon, pumpkin, and beetroot seed press use.

## [1.1.7-reconstructed] - 2026-06-17

### Validation

- Confirmed `runGameTestServer` passes with all 145 required GameTests.
- Confirmed `clean build` passes and produces `build/libs/engineers_decor_reforged-1.1.7-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance with matching SHA-256 hashes.

### Fixed

- Restored original Ariadne Coal parity from Engineer's Tools:
  - Ariadne Coal is now a single-stack durability tool instead of a stack of consumable marker items.
  - Ariadne Coal now uses the original 100-use durability.
  - Successful marker placement now costs one durability and breaks the item on the final use.
  - Failed marker placement now leaves durability unchanged.
  - Placement feedback now uses the original hit/break style sounds instead of the placeholder stone-place sound.
- Updated Ariadne Coal tooltip and manual text so the documented cost matches the restored durability behavior.
- Synchronized user-visible mod version metadata to `1.1.7-reconstructed`.

### Added

- Updated Ariadne Coal GameTests for single-stack behavior, original durability, placement wear, failed-use no-wear behavior, and final-use break behavior.

## [1.1.6-reconstructed] - 2026-06-17

### Validation

- Confirmed `runGameTestServer` passes with all 144 required GameTests.
- Confirmed `clean build` passes and produces `build/libs/engineers_decor_reforged-1.1.6-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance with matching SHA-256 hashes.

### Fixed

- Restored original Mind Squeezer parity from Engineer's Tools:
  - the Mind Squeezer is stackable to 64 and no longer has durability or breaks
  - creative/instabuild no longer bypasses the original lapis and XP requirements
  - low-health refusal now uses the original `maxHealth / 10` threshold
  - successful conversion now consumes one lapis and one XP level, applies the original `maxHealth / 10` health cost, adds hunger exhaustion, and briefly blinds the user
  - success sounds now match the original hurt/enchantment feedback instead of the placeholder grindstone sound
- Updated Mind Squeezer tooltip and manual text so the documented costs match the restored behavior.
- Synchronized user-visible mod version metadata to `1.1.6-reconstructed`.

### Added

- Updated GameTest coverage for Mind Squeezer stackability, non-durability, original costs/effects, low-health threshold behavior, and creative no-bypass behavior.

## [1.1.5-reconstructed] - 2026-06-17

### Validation

- Confirmed `runGameTestServer` passes with all 144 required GameTests.
- Confirmed `clean build` passes and produces `build/libs/engineers_decor_reforged-1.1.5-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance with matching SHA-256 hashes.

### Fixed

- Restored additional original Engineer's Tools parity:
  - Sleeping Bag now uses the original 4096 durability instead of 48.
  - Sleeping Bag keeps its original hidden durability bar.
  - Crushing Hammer entity hits now cancel vanilla attack damage after applying knockback.
  - Crushing Hammer now disables shields like the original item.
  - Crushing Hammer block mining now returns the original no-use-stat result after server-side wear handling.
- Synchronized user-visible mod version metadata to `1.1.5-reconstructed`.

### Added

- Added GameTest coverage for Sleeping Bag durability/bar behavior and updated Crushing Hammer attack/shield parity coverage.

## [1.1.4-reconstructed] - 2026-06-17

### Validation

- Confirmed `runGameTestServer` passes with all 143 required GameTests.
- Confirmed `clean build` passes and produces `build/libs/engineers_decor_reforged-1.1.4-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance with matching SHA-256 hashes.

### Fixed

- Restored original Charged Lapis parity from Engineer's Tools:
  - heals `maxHealth / 20` instead of a fixed 3 HP
  - clears fire on use
  - keeps the original foil item glint
- Synchronized user-visible mod version metadata to `1.1.4-reconstructed`.

### Added

- Added GameTest coverage for Charged Lapis healing, fire clearing, XP grant, item consumption, and foil behavior.

## [1.1.1-reconstructed] - 2026-06-17

### Validation

- Confirmed `compileJava` passes.
- Confirmed `runGameTestServer` passes with all 139 required GameTests.
- Confirmed `clean build` passes and produces `build/libs/engineers_decor_reforged-1.1.1-reconstructed.jar`.
- Installed the rebuilt jar into the Prism `1.21.1 TesT LaB` instance with matching SHA-256 hashes.

### Fixed

- Reimplemented the REDIA Tool against the original MIT Engineer's Tools behavior:
  - diamond-grade axe/pickaxe/shovel/hoe/shears capability exposure
  - original 3000 durability
  - normal-use torch placement from inventory without REDIA durability loss
  - sneak-use plant snipping, ground cycling, and axe stripping
  - sheep/entity shearing
  - villager, owned-pet, and neutral zombified piglin safe-attack cancellation
  - sneak-break connected log felling
  - durability-dependent Efficiency/Fortune decay
- Added the REDIA diamond repair/over-repair recipe and serializer so over-repair restores the original Efficiency/Fortune progression.
- Synchronized user-visible mod version metadata to `1.1.1-reconstructed`.

### Added

- Added GameTest coverage for REDIA multi-tool actions, ground cycling, plant snipping, entity shearing, safe attack, tree felling, and diamond over-repair.
- Added `install-mod.ps1` to reproducibly build/install the runtime jar and write `build/install-report.json`.

## [1.1-reconstructed] - 2026-06-17

### Validation

- Confirmed `compileJava` passes.
- Confirmed `runGameTestServer` passes with all 132 required GameTests.
- Confirmed `build` passes and produces `build/libs/engineers_decor_reforged-1.1-reconstructed.jar`.
- Latest rebuilt jar observed locally: `2,450,208` bytes, updated `2026-06-17 3:21:21 PM`.

### Added

- Reconstructed a NeoForge 1.21.1 Gradle source project from the published `engineers_decor_reforged-1.1.jar`.
- Restored recovered resources, Gradle wrapper files, mod metadata, recipes, loot tables, models, blockstates, language files, and manual assets.
- Added the project logo as the root `pack.png` asset and NeoForge mod-list `logoFile`.
- Added GameTest coverage for machine menus, automation transfer, machine save/load normalization, fluid handlers, redstone controls, accessway blocks, and selected tool behavior.
- Added regression coverage for:
  - attached redstone controls dropping when backing support is removed
  - negative and oversized machine runtime timers being clamped on load
  - powered iron hatches staying open while redstone-powered
  - block breaker and tree cutter drop buffering
  - factory hopper pulse redstone behavior
  - milking machine bucket and fluid output paths
  - direct filled-bucket fluid state updates
  - factory dropper duplicate filter handling
  - factory hopper comparator refresh after collection
  - comparator switch self-output isolation
  - material box recovery from invalid stored item data
  - REDIA Tool torch placement into replaceable blocks
  - GameTest fixture spacing for larger multi-block tests
  - metal sliding door hitboxes matching the visible closed/open models
  - machine menu progress meters reporting normalized percentages
  - tracker tooltips ignoring incomplete saved target data
  - metal rung ladders and staggered metal steps registered as climbable blocks

### Fixed

- Removed decompiler artifacts that prevented clean compilation:
  - unreachable `break` statements after `yield` in `MachineBlockEntity.java`
  - raw `BlockEntityType` casts in capability registration
- Fixed automation extraction so machines with defined result slots expose output-only extraction and insertion-only input behavior where appropriate.
- Enabled NeoForge milk fluid and mapped internal `"milk"` tank storage to `NeoForgeMod.MILK`.
- Restored downward milk transfer from the Small Milking Machine into tanks or fluid handlers below.
- Restricted specialized fluid machines to their intended fluids:
  - Small Milking Machine accepts milk only.
  - Small Mineral Smelter accepts lava only.
  - Passive Fluid Accumulator accepts water only.
- Restored processing-phase comparator output for the Small Mineral Smelter and Small Freezer.
- Made attached redstone controls require sturdy backing support and drop when support is removed.
- Clamped malformed loaded machine runtime timers so invalid values cannot persist into runtime behavior.
- Kept powered iron hatches open when right-clicked while still receiving redstone power.
- Routed Small Block Breaker and Small Tree Cutter harvested drops into their internal drop buffers instead of spawning them into the world.
- Made factory hopper pulse mode respect redstone trigger semantics before collecting loose world items.
- Prevented factory hopper pulse mode from collecting while redstone power is merely held without a new edge.
- Allowed Small Milking Machine bucket output even when the internal tank is full, and cleared stale active state when no output/storage path exists.
- Updated visible fluid block state immediately after direct filled-bucket use.
- Cleared Small Tree Cutter active visual state immediately when redstone disables it during cooldown.
- Made factory dropper duplicate filters reserve simulated stock per match so a duplicate filter cannot emit a partial duplicate stack.
- Refreshed adjacent comparators after factory hopper world-item collection changes inventory.
- Prevented industrial comparator switches from latching from their own output.
- Normalized invalid Material Box `stored_item` custom data as empty so boxes can recover and accept valid materials again.
- Allowed REDIA Tool torch placement into replaceable blocks such as short grass while validating the resulting torch state before consuming resources.
- Added valid backing support to pulse-control GameTest fixtures.
- Enlarged the shared blank GameTest structure footprint to 8x8x16 so multi-block fixtures are spaced correctly.
- Replaced the metal sliding door's vanilla hinged-door shape with model-aligned sliding-door shapes for closed and open states.
- Normalized machine menu progress reporting for lab furnaces, electrical furnaces, temperature machines, and block breakers.
- Synced Small Block Breaker process totals through menu data so progress percentages use the real configured work time.
- Suppressed Tracker location tooltips when saved target custom data has a dimension but incomplete coordinates.
- Added Metal Rung Ladder and Staggered Metal Steps to the vanilla `minecraft:climbable` block tag so player ladder movement works in-game.

### Repository

- Published initial reconstructed source import in commit `58fb6e4`.
- Published machine/control/tool regression repairs in commit `abda90c`.
- Published REDIA Tool and pulse-control fixture repairs in commit `78489ce`.
- Published factory hopper pulse collection and GameTest footprint repairs in commit `838a0cb`.
- Published mod logo asset in commit `489efa6`.
- Published menu progress and tracker tooltip repairs in commit `330dce5`.

### Known Follow-Up

- Review whether Gradle project version `1.1-reconstructed` and `neoforge.mods.toml` display version `1.1` should be aligned for release packaging.
- Continue bug hunting in machine automation, fluid handlers, redstone support/drop rules, save/load normalization, menu shift-click behavior, and tool edge cases.
