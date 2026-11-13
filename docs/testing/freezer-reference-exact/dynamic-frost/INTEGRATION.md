# Dynamic chamber candidate: opt-in only

This change uses the specialist's corrected V5 layers with organic phase 2/3 revision in `iedctr.dev.freezerV5LiveEnergy` only. Exact-reference mode still draws the unchanged canonical PNG; ordinary production selection and server gameplay are unchanged.

## Rendering contract
- Reconstructed clean base preserves every canonical pixel outside the specialist mask. The original remains `background.png`.
- The full-canvas transparent phase overlay is selected directly from synchronized `menu.freezerPhase()` (clamped 0..4), never client time, guessed progress or status.
- Phase 0 overlay is fully transparent: no active frost fill. Decorative frost remains on the rim near metal/hinges. This is explicitly not total defrost of the original illustration.
- Existing FE cell rendering is applied after the chamber layers and is unchanged.
- Gameplay currently resets progress and phase when stopped by redstone, insufficient FE, invalid input or completed cycle. Follow those server facts; no client-side hold or change to mechanics.
- Phase 4 is set at completion and immediately reset in the same server tick. The asset is supported and statically tested, but no lingering phase 4 is invented or claimed as naturally observed.

## Evidence boundaries
The native fixture retains all ten previous interaction cases, adds actual recipe captures for phases 0..3 plus completion reset, and hashes only the interior chamber crop as well as the framebuffer. All idle FE states and completed recipe must share the same phase-zero chamber crop; four natural phases must differ. This is bounded native proof, not physical OS input, normal production path or final visual acceptance. The downloaded specialist composites are CPU previews, not native screenshots.

Static JUnit guards check every pixel outside the mask, hinge guard regions, zero alpha for phase 0 and distinct overlay coverage. The model test checks direct authoritative phase selection independently from progress/status. Candidate provenance and source SHA are in the specialist manifests. New source data does not authorize gameplay changes or production activation.

CI/native execution for this change is pending. Do not reuse the old static-frost receipts as evidence for these new assets.

## Organic phase 2/3 revision
Only overlays 2 and 3 were replaced by the specialist's organic lower-edge correction. Canonical background, clean base, mask and phases 0/1/4 were independently checked byte-identical before/after integration. The delivered archive SHA-256 is `0d4b27195df581587104ff014ce588e1bb722922f3d35e0425ee13ddee84d7b2`. `runtime-assets.sha256` pins the integrated PNGs; the updated source manifests describe the complete specialist package, including preview files not installed in the runtime. The earlier `README_QA.md` describes the preceding corrected candidate; `REVISION_NOTES.md` describes this later correction. The supplied reproduction script was not executed.

Phase 0 native capture represents idle before the natural recipe, not a claimed active cycle phase. Native rendering and final visual acceptance remain pending.
