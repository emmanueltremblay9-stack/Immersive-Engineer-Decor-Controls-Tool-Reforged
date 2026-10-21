# Chimney smoke, adaptive heads and fitting restoration

## Behavior

- Restores documented cosmetic smoke on Dark Shingle Roof Chimney, using the original Engineer's Decor vanilla smoke particles.
- New heads start at intensity 5, or use the redstone strength already present when placed. Click to cycle 0–15; 0 is off. When holding another chimney head, an ordinary right-click places it instead, allowing stacking without sneaking. Smoke adjustment consumes no item.
- A head alone retains its original 6/16-block height, footprint and textures. When another head is directly above, its four walls extend to 16/16, filling the former 10/16 gap. Only the exposed head at the top of each directly connected chain emits smoke. A gap starts a separate chain. Ordinary blocks above do not form a chimney chain or impose a new obstruction rule.
- This uses the same existing item and registry ID, with an adaptive model/state; no new vertical-segment item is introduced. A chimney trunk can support the head stack in every orientation.
- Intensity remains local to each head. Adding/removing heads preserves that head's manual setting; an exposed head resumes at its saved setting. The last observed redstone input is saved separately so only a true signal change overrides the chosen intensity, including an input change to 0. An explicit click can adjust a head under constant signal; no intensity propagates through the stack.
- The chimney remains decorative: no fuel, furnace or heat system is added.
- The Chimney Trunk now has selection and collision following its preserved model, including the upper rim used to place a head directly above it. New trunks remain upright and straight when neighboring stairs change.
- Multiple trunks can also touch through their existing walls, but each retains its original roof piece. For a plain upright stack, use multiple adaptive heads above the roof fitting.
- Registry IDs, textures, original isolated-head model, recipes and drops are retained. Existing property-less heads load silently at 0 and can be activated by clicking or redstone. Existing trunk facing, half, shape and waterlogging are preserved. Existing corner/inverted trunks are not migrated; re-place them for the upright fitting.
- Old saved head stacks without adaptive properties may need a neighbor update or click to refresh their model state. Smoke suppression also checks the actual block above, preventing lower-head emissions even before that state refresh. No world-wide migration or block replacement is performed.

## Regression coverage

Dedicated GameTests cover the click cycle/off state, held-item preservation, all redstone strengths and powered placement, state/codec reload and legacy states, bounded particles, actual cap placement from ray hits on the rim, all 80 legacy trunk states, exact model/shape comparison and rotations.

Additional tests cover 2, 3 and 5 heads on a trunk in all four orientations, continuous adaptive walls, per-head state persistence, top-only emission, gaps, add/remove/reconnect updates, ordinary blocks above, top-head controls, retained manual settings and unchanged/changed redstone signals. Multiple-trunk assemblies are covered separately.

The historic smoke implementation and existing models derive from Engineer's Decor by Stefan Wilhelm / wile (MIT), retaining repository attribution. Reference: https://github.com/stfwi/engineers-decor/blob/a561e61e35ce0367088356967e1ea4cad5c1f27d/src/main/java/wile/engineersdecor/blocks/EdChimneyBlock.java

## Verification limits

Dedicated tests validate server behavior, geometry/model parity and the shared emission logic. They do not establish client appearance, particle settings or a full world unload/restart. Client checks remain required before release qualification: build/remove a multi-head stack, verify joined walls and only the top plume, cycle off/on, apply/remove redstone, and save/reopen. No installation or publication is part of this change.
