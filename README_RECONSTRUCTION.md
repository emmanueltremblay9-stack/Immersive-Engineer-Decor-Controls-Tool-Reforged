# Engineer's Decor & Controls Reforged Reconstruction

This is a separate reconstruction workspace created from the published CurseForge `engineers_decor_reforged-1.1.jar` artifact.

Recovered base artifact SHA-256: `422EDB90047C6B1E188BC204DAA2C926DB11DC07DE527B269377369871465DE7`.

The exact historical CurseForge distribution URL/file ID was not present in the recovered evidence and is intentionally not guessed here.

Original recovered artifacts remain untouched in:

`C:\Users\Emmanuel Tremblay\AI Depot\Codex Documents\Engineer Decor Controls Reforged Recovery\artifacts`

Status:

- Decompiled Java source was generated with Vineflower.
- Resources were copied from the recovered JAR extraction.
- The original Gradle source project was not found locally.
- Decompiled Java must be reviewed and validated before treating this as the authoritative source project.
- Gradle `build` succeeds after correcting two decompiler artifacts in the reconstruction copy:
  - removed unreachable `break` statements after `yield` in `MachineBlockEntity.java`
  - removed raw `BlockEntityType` casts in capability registration in `EngineersDecorReforged.java`

Validation targets:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat runGameTestServer
.\gradlew.bat build
```

Automation I/O fix:

- Small electrical furnace input slots are insertion-only for automation.
- Automation extraction is limited to layout result slots when a machine layout defines result slots.
- Regression coverage was added to `MachineGuiGameTests`.
- `runGameTestServer` now completes successfully; the latest local run passed 148 required GameTests.

Additional bug hunt fixes:

- Enabled NeoForge's milk fluid during mod construction so the Small Milking Machine can expose stored milk to fluid pipes.
- Mapped internal `"milk"` storage to `NeoForgeMod.MILK` for fluid handler fill/drain/read operations.
- Restored downward milk fluid push from the Small Milking Machine into tanks or pipes below.
- Added GameTest coverage for milk fluid exposure and downward tank transfer.
- Restricted specialized fluid machines to their intended fluids:
  - Small Milking Machine: milk only.
  - Small Mineral Smelter: lava only.
  - Passive Fluid Accumulator: water only.
- Restored processing-phase comparator output for the Small Mineral Smelter and Small Freezer.
- Attached redstone controls now require their backing support and drop when that support is removed.
- Loaded machine runtime timers are clamped during NBT load so malformed negative or oversized counters cannot survive into runtime behavior.
- Powered iron hatches stay open when right-clicked while still powered.
- Small block breakers and small tree cutters route harvested drops into their internal drop buffer instead of spawning them into the world.
- Factory hopper pulse mode respects the redstone condition before collecting loose world items.
- Small milking machine bucket output is no longer blocked by a full internal tank, and stale active state is cleared when no output/storage path exists.
- Direct filled-bucket use updates the visible fluid block state immediately.
- Small tree cutters clear their active visual state immediately when redstone disables them during cooldown.
- Factory droppers handle duplicate filters atomically so a second matching filter cannot emit a partial duplicate stack.
- Factory hopper world-item collection updates adjacent comparators after inventory changes.
- Industrial comparator switches ignore their own output when reading the attached signal, avoiding self-latched power.
- Material boxes recover from invalid stored item IDs in custom data and can accept valid materials again.
- REDIA Tool torch placement now accepts replaceable blocks such as short grass and validates the resulting torch state before consuming a torch or durability.
- Pulse-control GameTest fixtures now place valid backing support so scheduled reset coverage does not depend on unsupported floating controls.
- Factory hopper pulse mode only collects loose world items when a manual, ignored-redstone, continuous, or fresh redstone-edge trigger actually fires.
- The shared blank GameTest structure now declares an 8x8x16 footprint so multi-block fixtures are spaced correctly as the suite grows.
- Metal sliding door hitboxes now follow the centered closed panel and the side-stacked open model instead of using vanilla hinged-door edge boxes.
- Machine menu progress meters now report normalized percentages, and Small Block Breaker total work ticks are synced to the menu.
- Tracker tooltips ignore incomplete saved target custom data instead of showing fake origin coordinates.
- Metal Rung Ladder and Staggered Metal Steps are tagged as `minecraft:climbable`, restoring player ladder movement.
- REDIA Tool behavior was reimplemented against the original MIT Engineer's Tools source: multi-tool action exposure, torch placement, sneak-use ground cycling and shearing, safe attack, tree felling, and diamond over-repair Efficiency/Fortune progression.
- Mind Squeezer behavior now matches the original MIT Engineer's Tools source: stackable to 64, no durability or breaking, no creative cost bypass, original `maxHealth / 10` low-health threshold and health cost, hunger exhaustion, and short blindness on successful conversion.
- Ariadne Coal behavior now matches the original MIT Engineer's Tools source: a single-stack 100-use marker tool that wears on successful placement, does not wear on failed placement, and breaks on the final use.
- Muslee Bar Press now accepts wheat, melon, pumpkin, beetroot, and Immersive Engineering Industrial Hemp seeds through a shared item tag for both inventory use and direct recipes.
- Machine menu shift-click handling now ignores negative and out-of-range slot indices instead of throwing from malformed menu input.
- REDIA Tool now uses the original MIT Engineer's Tools 3D item model instead of a flat generated item model.
- REDIA Tool durability-dependent Efficiency and Fortune decay curves now match the effective original 1.16 config defaults.
- Optional JEI support is isolated in a compatibility package and adds crafting/smelting catalysts, concise item and machine info pages, and GUI smelting click areas without making JEI a required runtime dependency.
