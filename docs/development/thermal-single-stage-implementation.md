# Thermal single-stage Freezer / Smelter — implementation candidate

Status: DRAFT / UNMERGED / NOT_RELEASED. Owner requested one machine = one stage, shared Freezer-like textless GUI, top input, bottom output, automatic recipes and item-stack cycles. Code changes are on the PR branch; this document does not claim runtime qualification.

## Server contract

| Machine | Input | One physical machine cycle | Output |
| --- | --- | --- | --- |
| Small Water Freezer | Water bucket or 1,000 mB water | cool | 1 Ice |
| Small Water Freezer | 1 Ice | cool | 1 Packed Ice |
| Small Water Freezer | 1 Packed Ice | cool | 1 Blue Ice |
| Small Mineral Smelter | Recognized mineral | heat | 1 Magma Block |
| Small Mineral Smelter | 1 Magma Block | heat | 1,000 mB lava |
| Small Mineral Smelter | 1,000 mB **top-inserted** lava | cool | 1 Obsidian |

The first two Smelter mappings derive from historic hot/magma/lava behavior and are implementation assumptions pending owner confirmation. The last stage (Lava → Obsidian) is an explicit owner decision. Each completed cycle processes one unit and may repeat for a remaining compatible input stack. **Outputs are not automatically re-fed as inputs.**

The Smelter's 1,000 mB output lava is never input credit: only actual top-port lava admission increments the reserved credit. Bottom fluid extraction excludes credited lava. In a tank mixing credited incoming lava and output lava, output is independently drainable while the input remains protected. Older fluid tanks migrate as *output* with zero credited input to avoid surprise conversion.

The old mode-selection packet has no effect; the new screen has no recipe buttons. An active legacy Smelter batch can finish once using its saved state. All subsequent batches resolve from actual item/fluid inputs. NBT includes a new auto-recipe version, active stage and credited lava amount.

The Freezer's direct water-bucket interaction inserts the held bucket into slot 0; it no longer grants an instant Ice item. The server cycle still produces the empty bucket; bottom automation may retrieve it separately. Energy/redstone/output-blocked states pause a current batch rather than granting a free new stage. Changing a compatible-looking but different item invalidates the old work via a persisted input signature.

## Slots / ports / presentation

- Both: input backing index 0 above the chamber, output backing index 2 below, 27 inventory + 9 hotbar slots unchanged.
- Freezer: legacy reserved slot 1 remains hidden and recoverable by explicit Ctrl+Shift+R if occupied.
- Item top: input only. Item bottom: products only, plus optional empty-bucket recovery on Freezer. Normal side item ports blocked.
- Freezer water fluid top input only; Smelter lava fluid top input and fluid output bottom. Energy port remains machine-defined.
- Client GUI maps the two machines to a common 314x314 virtual canvas, with empty title/inventory labels, phase-driven frost/heat and FE segmentation. Source Freezer V5 art and historic Smelter furnace excerpt are reused rather than overwriting either original PNG.
- The newest generated side-by-side art is **not** integrated as exact runtime raster; actual texture registration/visual similarity and steel patch fit require review before acceptance.
- All quantity/status information remains outside the main faceplate in hover tooltips or item overlays. Screenreader/localization coverage is not claimed complete.

## Qualification gates still required

1. Compile source/resources; run JUnit and comprehensive validation without masking historical preservation failures.
2. Execute Native Minecraft/NeoForge tests for every stage, empty/full outputs, 64-item stacks, mismatched components, top/bottom/side capability matrix, simulation vs actual transfers, fluid mixing and conservation.
3. Verify redstone pauses, FE starvation, reload during a cycle, old selected-mode NBT, historic hidden slot recovery, new top-lava input, bottom output and bottom fluid extraction.
4. Capture each GUI at supported GUI scales and EN/FR with both empty and active states; verify slot rims, item footprints, hitboxes, icon-only tooltips/narration, texture artifacts and no unintended title collision.
5. Review conflicts with PR #196 and PR #208 before any integration. Merge, release, tag, publication and installation in Prism are outside this candidate.

Evidence status: GitHub per-file write/readback where recorded; CI/real-world qualification apply only to the exact tested commit and must be checked independently.
