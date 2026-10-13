# 1.1.58-reconstructed

Corrective update carrying forward the deferred 1.1.57 candidate changes. The previous 1.1.57 candidate was deferred without publication; all of its fixes and synchronized-pair functionality are carried forward here. The version increment itself changes metadata and documentation only, preserving runtime behavior and dependencies.

## Added

- Adjacent matching doors and fence gates now open and close together after an accepted player click. This covers vanilla doors/gates, mod blocks inheriting their vanilla interaction behavior, and IEDCTR Steel Mesh Fence Gates. Pairs are limited to two leaves and do not propagate down rows. Existing sliding-door behavior is retained.

## Fixes

- Corrected Factory Hopper placement/output direction and extraction from adjacent vanilla inventories.
- Corrected Steel Floor Grating collision height and Steel Double T Support collision geometry.
- Restored Panzer Glass Slab translucent render declarations.
- Aligned the Exit Sign selection outline while preserving its empty collision.
- Restored connections between decorative walls, Steel Mesh Fence, and vanilla walls.
- Restored walking through an open Steel Mesh Fence Gate while preserving collision on its posts and folded side. Steel Mesh Fence now connects to the gate on the correctly aligned sides.
- Corrected Iron Hatch hinge selection when placing against floors, ceilings, and walls.
- Restored climbing into an open Iron Hatch from a ladder directly below on the same support wall, including Metal Rung Ladder and Staggered Metal Steps. Closed hatches and ladders on another edge do not gain climbing.
- Restored vanilla ladder collision for Metal Rung Ladder and Staggered Metal Steps so your head no longer catches below an open Iron Hatch when climbing against the wall.
- Corrected Structured Vertical Clinker Slab orientation when placed against a wall directly ahead, removing the unwanted gap.
- Aligned the Door Sensor selection outline with its model.
- Corrected Industrial Switch cabinet placement and outward orientation. New cabinets occupy two blocks with matching selection and collision; either half toggles the linked redstone output. Breaking either half in survival drops one cabinet item.

## Existing worlds and limitations

Back up your world before updating. Existing Industrial Switch cabinets keep their legacy state and are not automatically converted. Break and re-place an old cabinet in clear two-block space to obtain the paired layout and corrected outward orientation. Upper-half selection requires a real upper block. This update does not scan worlds or overwrite occupied space.

Already placed hatches and vertical slabs are not automatically rotated. Existing wall connections may need a neighbor update to refresh. Steel Mesh Fence remains a wall-based block. Hopper extraction supports adjacent vanilla `Container` inventories; inventories exposing only NeoForge `IItemHandler` are not supported by this extraction path. Door Sensor mounting appearance and empty collision are unchanged.

Existing metal ladders keep their facing and waterlogged state but now have the same thin physical collision strip as vanilla ladders. Models, selection outlines, and placement rules are unchanged. No re-placement is needed for this correction.

Steel Mesh Fence Gate preserves saved states, models and selection outlines. The thin inward decorative leaf no longer restricts the open collision corridor below player width. Gate connections may need a neighbor update. Manual interaction and the existing lack of automatic redstone opening are unchanged.

Synchronization does not forward redstone signals or bypass refused interactions. Blocks with custom interaction overrides and items with custom pre-use behavior are excluded conservatively; compatibility with every modded door or lock is not claimed.

## Compatibility

Minecraft **1.21.1**, Java **21**, and NeoForge **21.1.230** minimum. Immersive Engineering **1.21.1-12.4.2-194** is required; JEI **19.32.0.359** is optional. Engineer's Decor, Engineer's Tools, and Redstone Gauges and Switches are incorporated source projects, not additional runtime dependencies to install.

## Validation and limits

Built from source commit `19c6bc7f8322002fd1d4710885d13f4482b4cb45`. Exact-commit Build CI passed 262 GameTests and 84 publisher tests. An independent clean cloud build verified the packaged JAR SHA-256 `bc77067f9f6e0c03f987123b490812d32aa9171ad388fcd87ecdf50777883fdb`, size 19,677,256 bytes, embedded mod/version and packaged attribution.

A real packaged dedicated server using Minecraft 1.21.1, NeoForge 21.1.230 and Immersive Engineering 12.4.2-194 passed startup, saving, clean shutdown and restart. Five block states persisted. Console checks verified redstone response, linked gate removal and exactly one gate item dropped when destroying its lower segment. This is a bounded server smoke test, not exhaustive multiplayer or loot coverage.

After installation of this exact JAR in the existing Prism profile, the project owner reported that doors, the gate and ladder work. This is owner-reported client testing, not an independent visual recording. Automated tests and this report do not establish compatibility with every modpack or custom door implementation.

Version 1.1.57 was deferred and never published; its changes are included here. Earlier candidate JARs are superseded. Keep a world backup before updating.
