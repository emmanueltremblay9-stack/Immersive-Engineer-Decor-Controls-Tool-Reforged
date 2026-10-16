# Lava surface continuity

## Regression and scope

The 2026-10-16 screenshot reported triangular openings around catwalk stairs and poles with lava. A separate controlled scene on the source tree shared by PR64 head 4fdcfcd2 and merge c1e832bc reproduced discontinuous surface corners. The installed Windows 1.1.60 JAR was independently qualified as SHA-256 866B3256187E3533DD09E18FDD7F3395155E7763D14D69CCC90A214EACCCF517; the reproduction used the equivalent development sources, not a launch of the installed JAR.

Before correction, native rendering produced:

| Completed scene | Recognized top faces | Attachment cells | Discontinuous shared corners | Largest height gap |
| --- | ---: | ---: | ---: | ---: |
| Controlled vanilla | 26 | 0 | 0 | 0 |
| Controlled decorative | 26 | 24 | 24 | 0.4747475 blocks |
| Natural vanilla | 13 | 0 | 0 | 0 |

Server/client states agreed in these scenes. The local run was stopped before the fourth scene and before Embeddium because the host had only about 75 MiB of free physical memory while the user's Prism session was active. Only the strictly identified AI-Work development JVM was stopped. This partial run is evidence of the native defect, not a completed suite.

The first exploratory comparison substituted liquid block states and therefore also changed occlusion. That comparison is not the regression oracle. The shared-corner measurements above come from actual emitted native vertices without state substitution. The retained fixture compares heights while keeping block states and shapes intact.

## Targeted correction

Native chunk meshing already supplied the position-aware current fluid, but LiquidBlockRenderer read six adjacent fluids, diagonals, and fluid above from BlockState. Flowing lava stored in an exterior-cell attachment was therefore absent from those calculations. The optional Embeddium 1.0.15 fluidHeight method had the same issue for its sampled cell.

The client mixins now query BlockGetter.getFluidState at each actual sample position. Vanilla height weights, block models, occlusion shapes, fluid propagation, buckets, storage, saves and accumulator behavior are unchanged. No blanket opt-in for other blocks or mods is added.

## Isolated regression suite

Run on Linux with the existing prerequisites:

    python tools/validation/client_gameplay.py --renderer-profile native --lava-surface-only --prepare-timeout 600 --timeout 600
    python tools/validation/client_gameplay.py --renderer-profile embeddium --lava-surface-only --prepare-timeout 600 --timeout 600

The existing nonce guard creates a fresh integrated fixture world under build. It never opens a user save. Four cases compare controlled and naturally spreading lava with vanilla and decorative scenes, using a stepped central support, stairs and upright poles. Natural scenes place one source and allow 500 client ticks to settle.

Checks include server/client state agreement, actual native emitted top vertices against the position-aware vanilla height rules with identical geometry, shared-corner continuity, nonempty attachment/top coverage, and real calls to Embeddium's height method in all four horizontal directions when that renderer is loaded. These Embeddium probes qualify its height calculation; screenshots capture its actual displayed pipeline. They are not a direct capture of the complete Embeddium vertex buffer.

The harness rejects incomplete/stale proofs, mismatch counts, nonfinite vertices/heights, missing actual Embeddium probes, discontinuous raw corners, and missing full-size screenshots. Existing full gameplay, fluid/accumulator GameTests and four renderer profiles remain in CI. Synthetic JSON/PNG evidence is retained; no JAR or user screenshot is uploaded.

The exact user scene and complete mod stack have not been replayed. Do not claim every diagonal slope in the screenshot is incorrect or that arbitrary fluid/rendering integrations are qualified.
