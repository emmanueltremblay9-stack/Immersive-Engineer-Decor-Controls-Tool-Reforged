# Chimney smoke and fitting restoration

## Behavior

- Restores the documented cosmetic smoke on Dark Shingle Roof Chimney, using the original Engineer's Decor vanilla smoke particles.
- New caps start at intensity 5, or use the redstone strength already present when placed. Click to cycle 0–15; 0 is off. Held items are not consumed.
- As in the original, neighbor notifications synchronize intensity with the current incoming redstone strength, including 0 after a signal is removed. This can replace a manually selected intensity after a neighbor changes.
- The chimney remains decorative: no fuel, furnace, stack scanning or new obstruction rules are added.
- The Chimney Trunk now has selection and collision following its preserved model, including the upper rim used to place the separate cap directly above it.
- New trunks place upright and straight and no longer turn into plain roof corners when neighboring stairs change.
- Stacks of 2, 3 or 5 upright trunks plus a final cap have tested rim placement and touching wall collision in all four orientations. The preserved trunk is a roof fitting, so every stacked trunk still shows its roof piece. Caps alone are only 6/16 block tall and are not full-height stack segments; no new vertical model is introduced.
- Registry IDs, textures, models, recipes and drops are retained. Existing property-less caps load silently at 0 and can be activated by clicking or redstone; existing trunk facing, half, shape and waterlogging are preserved. Existing corner or inverted trunks are not migrated; re-place them for the normal upright fitting.

## Regression coverage

Dedicated GameTests cover the click cycle and off state, held-item preservation, all redstone strengths and removal, serialized state reload for all intensities and legacy states, particle generation bounds, actual cap placement by ray-hitting the trunk rim in four directions, upright placement/neighbor updates, and all 80 legacy trunk states.

The code is based on Engineer's Decor by Stefan Wilhelm / wile (MIT), retaining repository attribution. Reference: https://github.com/stfwi/engineers-decor/blob/a561e61e35ce0367088356967e1ea4cad5c1f27d/src/main/java/wile/engineersdecor/blocks/EdChimneyBlock.java

## Verification limits

The dedicated tests validate server behavior and the shared emission algorithm. They do not establish client visual appearance, particle settings, a full world unload/restart or the exact player's pointer interaction. Client checks remain required before release qualification: place the cap on the visible rim, adjust through off/on, apply/remove redstone, save/reopen, and confirm visible smoke. No installation or publication is part of this change.
