# Raised Steel Catwalk Stair V connection

New registry ID: `raised_steel_catwalk_stairs`. This is independent of the flat
platform unification proposal. No old block/item ID, placed platform, recipe or
world palette is converted. No stair crafting/conversion recipe was specified;
the item is currently available through the existing creative catalogue/commands.

The metal model, facing convention, rails, paid railing state, placement helper,
walking/headroom collision, outline, fluids and mining path reuse the existing
Steel Catwalk Stair. The V is contextual decoration, not a new separately mined
component or a change to the lower pole's physical collision or drops.

## Approved model and rendering ownership

The approved revision02 continuous support mesh is retained in
`tools/models/raised_stair_v_approved.json` (canonical vertices/triangles SHA256
`9e4f13ebc34a935100759abc959b84bed273ceb34b2255e73e5a1b22afbf825a`).
Following the reported mismatch, `generate_raised_stair_v.py` retains that mesh
at and above y=-4, including both contacts under the steps. Only the stem below
that plane is adapted to the actual pole section. `raised_stair_sections.py`
derives contours from the original cuboid models, including the wood's small
right-angle corner steps and the steel poles' hollow sections. A continuous loft
inside the contextual pole cell joins each section to the approved V. All 18
metal stair elements remain unchanged.

The new stair renders the upper V. The independent pole below renders the lower
part in place of its ordinary overlapping pole geometry while this stair is
present. Removing/replacing the neighbor restores the original pole model. Only
appearance changes; the pole keeps its own registry state, fluid, collision,
ownership and normal drops. The lower mesh remains inside that pole's cell rather
than being drawn a full block outside the stair's render bounds. The lower geometry now distinguishes wood, thin steel and thick steel, using
their respective existing textures. At the lower cell boundary its full contour,
centre and dimensions match the ordinary pole. The wood is 5/16 wide with its
actual stepped corners; thin steel is 4/16 with a 2/16 opening; thick steel is 6/16
with a 4/16 opening. Steel cavities close internally within the transition rather
than displaying a solid square at the join. The lower cap is omitted only when a
matching full-length vertical ordinary pole continues below; exposed ends retain
a cap. This introduces no new saved state or support-loss mechanic.

Material follows exactly Raised Platform's identity checks: thick/thin steel poles
and their heads; treated wood pole/head; straight wood support only facing UP.
Other supports select no V. No additional orientation filters, frame/light feature,
material items or pole consumption are inferred. Client model data derives the
connection from the current render-region neighbors. No material BlockState,
BlockEntity, ticking handler or migration is added. Saved mechanical state count
matches the original stair rather than multiplying it by cosmetic materials.

The models use NeoForge's built-in OBJ and composite loaders, already part of the
existing dependency. OBJ vertices have UVs and normals; UV tiling never samples a
neighboring atlas sprite. There is no newly installed connected-texture/render mod
and no claim of a verified Blockbench project.

## Validation scope

`RaisedStairMeshTest` checks surface-area conservation across splitting/rotation,
owner-cell bounds, atlas UV bounds, unchanged metal-model hash and state-model
resolution, distinct section resources, unchanged upper geometry and absence of
an invented recipe. `RaisedStairSectionContourTest` checks both directions of
contour coverage against the original pole cuboid union for all three sections
and four rotations. It rejects surplus/missing edges, diagonal corner cuts,
filled steel holes and duplicate caps at a matching neighbor; area alone is not
the acceptance criterion.

`RaisedStairVGameTests` checks actual registered item placement in both support
orders/four horizontal facings, exact support identities and orientation negatives,
independent pole preservation, support replacement/loss, saved rail ownership,
fluid selectors and state/NBT round trips, original collision/outline and state
schema, and native START→completed survival mining with exactly one stair drop.
The native-catalogue assertion increases only by this one explicit opt-in block.

`NativeRaisedStairVValidation` is disabled by default. When explicitly enabled, it
requires the existing disposable world's nonce marker and exact save directory,
uses already-loaded chunks, captures all four rotations on wood/thin/thick steel,
checks actual baked part/texture selection and foot contours/cap selection, then verifies original pole restoration
after ordinary-stair replacement. A separate launch reloads the same saved world
and checks the saved new stair and independent pole. Scene setup uses explicit
server block states; item-placement and survival ownership proofs are separate
GameTests. Capturing images alone is not a visual verdict; inspect the PNGs.

Full modpack compatibility and alternate-renderer appearance require their own
actual scenario coverage. Existing general CI renderer profiles do not by
themselves constitute a rendered V-stair scene on the user's modpack.
