# New Milking and Block Breaker artwork integration

This is a partial visual integration of the supplied 3 November artwork, not implementation of new machine mechanics. Original source PNG bytes are preserved. It supersedes the old Milking 941×1672 crop and Block Breaker v0.8.1 background in these two screen paths only.

## Integrated

- Milking: supplied 1254-square clean background, existing input/output and 27+9 player slots, actual synchronized milk amount/capacity, cropped milk and front-glass layers. Logical canvas 228×228; 16-square item hitboxes.
- Block Breaker: supplied v3 central broken-block scene, actual output slot and 27+9 player slots, synchronized FE. Logical canvas 256×256 with independently rounded column coordinates. The central broken block is a static illustration, never the target block from the world.
- Existing source framebuffer fixture extended to include Block Breaker, with empty/half/full FE captures. Milking geometry expectations updated; real bucket input/output captures and server quick-move proofs are included. Existing smelter and other-machine checks are retained.

## Explicitly unavailable, not simulated

- Milking has no synchronized selected-animal identity/name, dedicated FE storage, extraction-cycle contract or mushroom-cream recipe. Header, portrait and those gauges stay neutral, with translated explanatory tooltips. No arbitrary animal or production animation is drawn.
- Block Breaker has no synchronized fluid storage in this visual patch. Its fluid area remains empty; no water/Plant Oil/lubricant effect, tank, port or cost is invented. Power remains non-interactive neutral decoration; its four art colors have no approved state mapping. The large progress art stays neutral, and real work/status information remains accessible by tooltip.
- No server behavior, recipe, balance, item slot index, save data or port contract is changed.

## Verification

Geometry tests establish bounds and non-overlap, not an in-game PASS. The nine new geometry JUnit cases require CI execution. All 29 Python native-proof tests and locale JSON parsing pass locally. Local Gradle could not download its official distribution (network unreachable). Native screenshot and compile results must be checked on the exact pushed commit, then visually reviewed. Never label mockups or these static geometry tests as native captures.

## Asset provenance

Supplied Milking nine-layer kit and Small Block Breaker RP Graphic Kit v3. Only layers with truthful runtime data are active. Remaining layers retain their original source identity outside runtime until their mechanical contracts are implemented. These hashes identify the unmodified bytes; they do not independently establish redistribution rights.

- `milking_redesign_v1/milk.png`: SHA-256 `f6f0507deb081a2a83b6bb455abc6237ac2b41f4ab21d43c833e356fb2d9bbe5`.
- `milking_redesign_v1/background.png`: SHA-256 `871f7c4512227153a4c49591c5facdbc95b1443114cc3206659d6dd524d1326b`.
- `milking_redesign_v1/glass.png`: SHA-256 `0688ffa93b80ee59aa9ac35fa1316c885c6413f72b80a8af7a3c67284306eec4`.
- `block_breaker_rp_v3/power_neutral.png`: SHA-256 `23ff15c60420b8951117f748d0b6122e174c915f87e91466930af80fbdc12123`.
- `block_breaker_rp_v3/background.png`: SHA-256 `2594e227dd153ffa7b1954d8d5d3069e10d0176c0e8142c7f7d2727f315b0a0d`.
