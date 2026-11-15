# V5 chamber frost: organic-only correction (P2/P3)

The immutable V5 source (`68c1dc11e7d0e920addebca18100892861a70c5d68db5547b8014235acee3d46`), existing chamber mask, chamber base and phases 0/1/4 are unchanged bit-for-bit.

The previous distance-to-aperture fade made a horizontal ruler-straight transition across the lower chamber. The new P2/P3 alpha reveal uses a deterministic spatially varying sinusoidal envelope and smoothstep feather; frost layers remain clipped to the **unchanged mask**. This is an authored visual correction, not gameplay.

Mask alpha nonzero support **xyxy [432,363,823,598]** with exclusive high endpoint. `[438,369,817,592]` describes the original inset construction, not the feather support.

Pixel checks: 0 RGBA differences outside mask for every composite; 0 hinge pixel differences in guarded regions; original PNG, clean base, mask and phases 0/1/4 byte-identical. Before/after and 314px/1280x260 previews include nearest-neighbor to approximate UI scaling without claiming exact Minecraft behaviour.

Runtime mapping: display the synchronized server phase immediately; phase4 is transient and resets to 0 during server tick 180. Redstone and insufficient FE reset server progress to 0. No client hold, delay, extrapolation, or local reset is added.

**Not tested:** actual Minecraft UI rendering, game scale, integration, networking, or independent final visual approval. Static frost along original frame remains intentionally immutable; original flattened layers cannot be recovered.

No code, GitHub, PR, activation or release operations performed.
