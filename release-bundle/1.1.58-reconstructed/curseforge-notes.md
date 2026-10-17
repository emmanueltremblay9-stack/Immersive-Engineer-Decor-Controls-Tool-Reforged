# 1.1.58-reconstructed

Draft candidate notes. This version is not yet published or fully qualified. The previous 1.1.57 candidate was deferred without publication; all of its fixes and synchronized-pair functionality are carried forward here. The version increment itself changes metadata and documentation only, preserving runtime behavior and dependencies.

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

## Candidate validation status

Draft only: publication qualification remains incomplete. Preparation starts from commit `90548e2afc49feb7b6328d52e58db8fc3eebe0fd`, which already contains the hatch-continuation, metal-ladder clearance, Steel Mesh Fence Gate passage/connection fixes, and adjacent-pair synchronization from PRs #43–46. That commit still builds 1.1.57 and is not the final source identity for 1.1.58.

The final 1.1.58 source commit must pass exact-commit Build CI. Independently identify the rebuilt JAR by SHA-256, size and embedded mod/version, verify packaged notices, and record the applicable client checks before publication. The packaged dedicated-server smoke result and its limits are recorded below. The merged source, independently rebuilt byte identity and packaged-server smoke evidence are recorded below. Final client qualification and publication preflight are still pending. GameTests alone do not replace those runtime checks.

Earlier 1.1.57 CI results, JAR hashes and gameplay observations are historical evidence only and must not be relabelled as 1.1.58 qualification. No exhaustive reporter-modpack compatibility claim is made. See [the qualification record](../../docs/releases/next-patch-qualification.md) for evidence boundaries and outstanding gates.

## Current preparation evidence

Merged runtime source: `19c6bc7f8322002fd1d4710885d13f4482b4cb45`. Build run `38332247766` passed 262 GameTests and 84 publisher tests. A separate clean cloud build and actual packaged-server run `38336616164` verified SHA-256 `bc77067f9f6e0c03f987123b490812d32aa9171ad388fcd87ecdf50777883fdb`, size 19,677,256 bytes, embedded version/mod ID and packaged attribution matching the source.

The isolated NeoForge 21.1.230 server with Immersive Engineering 12.4.2-194 successfully started, saved, stopped and restarted. Five asserted block states survived restart; redstone response and linked gate removal passed. Both phases stopped cleanly with no detected errors. This was a console-driven packaged-server smoke test. Loot, real-player clicks/climbing/rendering and multiplayer synchronization were not exercised by it.

Earlier owner permission to omit a dedicated-server test was superseded by the request to perform this test. No omitted check is reported as passing. Final client evidence on 1.1.58 and remaining publication preflight remain pending; 1.1.57 user-reported gameplay stays scoped to that older artifact.
