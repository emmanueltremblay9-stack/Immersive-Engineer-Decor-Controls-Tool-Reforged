# Existing catwalk stair railings: landing continuity

Keep the existing standard and top-aligned landings as the height reference. Their flat railing caps end at local Y=16. The stair's upper cap previously ended at Y=30.5 and its lower cap at Y=15.5. Existing native placement offsets therefore produced a 1.5/16 upper height mismatch, a 0.5/16 lower mismatch, and a 1/16 mismatch between chained stair segments.

Align only the existing stair railing profiles: upper cap Y=32, lower cap Y=16. Preserve their 1.5-pixel thickness and existing posts; extend the post tops to those caps. Keep the handrail at 45 degrees and translate it vertically so its top plane follows `y + z = 32` in the north-facing model. This is a rigid handrail translation, not a lateral shift or change of stair pitch. UVs, texture references, element counts, model rotations, structural stringers, treads and block-state mappings stay unchanged.

The narrow stepped selection envelope follows the new endpoints. Fence collision remains Y=36/16 above the owner, with the same center opening and unchanged 4/16 and 12/16 tread collision. State properties, fluid handling, deliberate edits, paid balances, saved facing values and placement rules do not change. No additional block, shared-pole rule, recipe or lateral placement behavior is introduced.

## Regression coverage

`CatwalkJunctionModelTest` checks both landing references, lower/chain pitch, the rotated handrail top plane, all 32 declared facing/side/water variants, and frozen semantic fingerprints after restoring only the reviewed coordinates. The fingerprints cover the complete three changed model resources, including all other geometry, UVs and metadata.

`ShapeCacheGameTests` retains the independent frozen collision/support oracle and original stair outline. Only the explicit outline band between that original profile and the packaged model cap endpoints is added to its expected outline; cache reuse, geometry keys and every fluid/payment/edit-history state remain checked.

`UpperStairRailingTargetingGameTests` exercises native targeting/mining in the newly visible upper cap band, both sides and all rotations, dry/wet states, one paid refund, surviving opposite side and existing collision. Existing reach, occlusion, protection and movement regressions still apply.

The opt-in `iedctr.catwalkJunctionOnly` capture mode runs only inside the existing nonce-guarded disposable native client. It captures upper/lower standard and raised landings and a two-segment chain, each in four orientations. It uses native item attachment and explicit railing states for capture; its successful capture status is not itself an alignment or survival qualification. Inspect original before/after images and retain the loaded packaged JAR hash. Do not infer visual success from JSON measurements alone.
