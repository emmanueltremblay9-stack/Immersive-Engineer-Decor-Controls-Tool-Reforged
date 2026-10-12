# 1.1.57-reconstructed

Draft candidate notes. This version is not yet published or fully qualified.

## Fixes

- Corrected Factory Hopper placement/output direction and extraction from adjacent vanilla inventories.
- Corrected Steel Floor Grating collision height and Steel Double T Support collision geometry.
- Restored Panzer Glass Slab translucent render declarations.
- Aligned the Exit Sign selection outline while preserving its empty collision.
- Restored connections between decorative walls, Steel Mesh Fence, and vanilla walls.
- Corrected Iron Hatch hinge selection when placing against floors, ceilings, and walls.
- Restored climbing into an open Iron Hatch from a ladder directly below on the same support wall, including Metal Rung Ladder and Staggered Metal Steps. Closed hatches and ladders on another edge do not gain climbing.
- Corrected Structured Vertical Clinker Slab orientation when placed against a wall directly ahead, removing the unwanted gap.
- Aligned the Door Sensor selection outline with its model.
- Corrected Industrial Switch cabinet placement and outward orientation. New cabinets occupy two blocks with matching selection and collision; either half toggles the linked redstone output. Breaking either half in survival drops one cabinet item.

## Existing worlds and limitations

Back up your world before updating. Existing Industrial Switch cabinets keep their legacy state and are not automatically converted. Break and re-place an old cabinet in clear two-block space to obtain the paired layout and corrected outward orientation. Upper-half selection requires a real upper block. This update does not scan worlds or overwrite occupied space.

Already placed hatches and vertical slabs are not automatically rotated. Existing wall connections may need a neighbor update to refresh. Steel Mesh Fence remains a wall-based block. Hopper extraction supports adjacent vanilla `Container` inventories; inventories exposing only NeoForge `IItemHandler` are not supported by this extraction path. Door Sensor mounting appearance and empty collision are unchanged.

## Compatibility

Minecraft **1.21.1**, Java **21**, and NeoForge **21.1.230** minimum. Immersive Engineering **1.21.1-12.4.2-194** is required; JEI **19.32.0.359** is optional. Engineer's Decor, Engineer's Tools, and Redstone Gauges and Switches are incorporated source projects, not additional runtime dependencies to install.

## Candidate validation status

The final candidate still requires exact-commit CI, independent JAR identity and packaged-notice checks, client inspection, and packaged dedicated-server validation. Earlier test results and artifact hashes do not qualify this differently versioned JAR. No exhaustive reporter-modpack compatibility claim is made.

Candidate JARs built before the hatch ladder-continuation correction are superseded for qualification, even when they have the same version string. Final checks must use the corrected source commit and exact JAR bytes.
