# Prism client smoke

## Primary run

The installed 1.1.53 JAR was discovered with Minecraft 1.21.1, NeoForge 21.1.233, Immersive Engineering 12.4.2-194, and optional JEI 19.39.0.368.

- Engineer's Manual: opened the root, mod section, Engineer's Tools index, Coarse Iron Grit, Coarse Gold Grit, an IE-native entry, and back navigation. No broken page or manual exception was observed.
- Small Solar Panel: panel energy changed from 10,000 FE to 9,744 FE while an empty receiver gained 256 FE. A horizontal-face receiver also gained 256 FE. Total energy was conserved exactly and the shared transfer limit was respected.
- Metal Sliding Door: closed, opened, and reclosed states were directly observed and logged.
- Iron Hatch: closed, opened, and reclosed states were directly observed. The open-state screenshot and reclosed marker are retained; the attempted open chat marker itself was not recorded.
- Sound status: structure/event verified; audibly observed `NOT_OBJECTIVELY_OBSERVABLE` because no objective audio-capture channel was available.
- Creative inventory: the custom tab was accessible, its exact tooltip was shown, and representative public blocks/items were visible across the list without a manifest duplicate.
- Optional JEI: JEI initialized and displayed the Small Solar Panel crafting page.
- Shutdown: Save & Quit, server stop, world save, and client stop all completed.

Primary log SHA-256: `8FAF40B659AEE0C750BE9CE3E16E7C2A73E39F6EACBD2520645FF37AE357B8B6`.

## Corrected mining run

The first mining attempt was rejected as evidence because its chat confirmations were not bound to real post-break state. The final run used a controlled platform, fixed coordinates, Survival mode, ordinary diamond tools, and paired server-side queries:

- `IEDCT_METAL_PRESENT_FINAL_BEFORE_PICKAXE53` → `IEDCT_METAL_ABSENT_FINAL_AFTER_PICKAXE53`
- `IEDCT_CRATE_PRESENT_FINAL_BEFORE_AXE53` → `IEDCT_CRATE_ABSENT_FINAL_AFTER_AXE53`
- `IEDCT_GRIT_PRESENT_FINAL_BEFORE_SHOVEL53` → `IEDCT_GRIT_ABSENT_FINAL_AFTER_SHOVEL53`

Targets were a steel catwalk, labeled crate, and dense grit dirt block. All three were physically broken through the temporary Attack/Destroy key binding while holding the correct vanilla tool. This demonstrates normal representative blocks do not require RETIA/REDIA.

Mining log SHA-256: `14D84225DFA8472CA1D2A6E655622AD7CF1C132074C4BEC170FC9D34C9F7622F`.

The only `ERROR` records in that run were two `Win32: Failed to open clipboard: Access is denied` messages caused by automation text injection. No registry, capability, manual, datapack, or mod exception followed; the world and all dimensions saved cleanly.
