# Metal Crafting Table retained V3

## Scope and provenance

The owner requested 18 persistent storage slots on 4 November 2026. This change
retains hammer index 0, grid indices 1–9, and virtual output index 10. Storage
uses indices 11–28; the menu contains 29 machine slots and 36 player slots.
Storage is not a recipe ingredient source. The existing crafting and hammer
recipe mechanics are unchanged. Older NBT loads into the expanded container;
no existing index is remapped and the virtual output is still cleared on load.

Artwork is the retained `metal-crafting-v3-static-exact.official-render.png`,
1142 × 1378, copied unchanged. SHA-256:
`3332714591f4691f47c0f555f40638fe3a31bd9d00f0c8f67c5934664a00631c`.
Its archived JSON source is in GUI DEV commit
`8344355fecae8a5ee476c5ddd74471d2a16d2337`:
`candidates/iedctr/exact-gui-json/2026-11-06/current/metal_crafting_table/metal-crafting-v3-static-exact.toolkit.json.gz`.
The runtime canvas is 228 × 276; native 16-pixel item hitboxes are centered on
the retained artwork openings. No baked labels or new artwork were introduced.

## Verification boundaries

The new GameTest checks all 18 storage slots, save/load item identity and count,
exclusion from crafting, 65 menu slots, and shift-click retrieval of slot 28.
Existing hammer/grid/output tests remain. Tests must run on the aggregate PR's
exact commit. This source change alone does not establish native visual or
mouse-input qualification. No release or merge is implied.
