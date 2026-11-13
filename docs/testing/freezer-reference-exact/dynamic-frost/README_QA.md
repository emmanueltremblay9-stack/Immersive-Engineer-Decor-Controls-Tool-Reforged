# Small Water Freezer V5 — Corrected frost candidate (graphic-only)

## Source
Original `background_canonical.png`: SHA256 `68c1dc11e7d0e920addebca18100892861a70c5d68db5547b8014235acee3d46`; 1254×1254 RGB. Source is copied unchanged. Previous uploaded image has identical RGBA pixels.

## Change from preceding candidate
Removed broad chamber mask intersecting hinge and frame, replaced it with a curved, feathered inset that excludes the two hinge guards. Refined the clean chamber with layered, seeded blue/cyan cloud depth, no new props or mechanics.

## Composition
`freezer_v5_chamber_clean_base.png` is the reconstructed static clean background; draw corresponding `freezer_v5_frost_overlay_phaseN.png` for server-authoritative N=0..4. Overlays are full-canvas transparent RGBA. Composite proofs are `freezer_v5_composite_phaseN.png`.

## QA — performed
- Strict RGBA diff versus canonical at every mask alpha==0 pixel: 0 for all 5 phases.
- Hinge pixel regions left (378,432)..(442,467) and right (811,431)..(864,467): 0 changed pixels in phases 0–4.
- 314-px review sheet and 1280×260 chamber-strip review generated and visually inspected by author. Main phase progression reads at both review sizes.
- Reproducible source script `reproduce.py` can rebuild full exported PNGs from the canonical PNG using Python 3, Pillow, NumPy and SciPy.

## Visual reservations — DO NOT mark final acceptance
The flattened source has frost painted directly onto the glass rim and near hinges. That static ice is preserved intentionally outside the mask to avoid damaging V5 chrome and rivets. It remains visible in phase 0; it is not controlled by the phase overlay. The clean chamber replaces occluded pixels with new procedural blue depth shading, not recovered original layers. Mask is human-measured and conservatively inset; no pixel-perfect automated glass segmentation is claimed. Full independent visual acceptance and Minecraft tests are NOT_PERFORMED.

No code, GitHub changes, PRs, integration, or activation.
