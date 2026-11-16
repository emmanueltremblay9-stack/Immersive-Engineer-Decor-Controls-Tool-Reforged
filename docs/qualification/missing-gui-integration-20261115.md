# Missing GUI integration — candidate, not qualified

This work is an integration candidate, not a release or evidence that the latest
machine interfaces have already been tested in game. One aggregate PR is intended;
individual machine work must not be advertised as independently ready.

## Factory Hopper

The album capture from run `43707198231` genuinely showed the legacy GUI. It was
not a capture of the newly requested design. The owner subsequently requested the
Factory Placer V2 grey-steel material and frame as a visual reference **only**;
the Hopper must retain its own controls, slot positions and behavior.

The candidate renderer reuses strips of the retained Placer V2 artwork without
copying the Placer arrangement, labels or control semantics. No new raster image
is generated. Vanilla still draws all 54 menu items and owns interaction. Hopper
range, period, transfer count, redstone, delay indicator and selected-slot state
retain their existing paths. Its original control atlas remains in use.

Source artwork SHA-256:
- Placer V2: `ff079488dd0d1c7434597a6915231d88b81732472bb23ccc8a61235eca27ec2b`
- Original Hopper control atlas: `8a085db73897ea8e7a2e6af75b822a4509f5a0efa300a0a08d22da58f024795f`

Required before readiness: exact candidate compilation, exact-JAR native Hopper
control workflow in English/normal and French/small-window modes, visual review
of the newly captured frame, slot visibility, tooltips and state changes. Old
album screenshots and JAR hashes cannot qualify the candidate.

Other missing GUI integrations and their evidence will be recorded here once
source references and patches have been reconciled. A provisional concept is not
an approved final visual reference.

## Aggregate candidate currently included

- Factory Hopper: grey steel/frame, original Hopper layout and controls.
- Factory Dropper: retained V2, twelve inputs and three filters, original actions.
- Labeled Crate: retained V1, 54 storage + frame hint + player inventory.
- Small Electrical Furnace: retained exact reference, existing seven machine slots.
- Small Waste Incinerator: retained exact reference, existing sixteen queue slots.
- Crate label editor: recovered retained V1 artwork, native text fields and buttons; native qualification pending.
- Metal Crafting Table: retained V3, historical approved addition of eighteen
  persistent storage slots; hammer/grid/output indices preserved.

## Explicitly NOT covered

1. Small Lab Furnace: reference found is a provisional direction, not final approval.
Factory Dropper V2 retained artwork is now integrated with twelve input slots,
three filter slots and live controls. Native qualification remains pending.
The recovered Crate label editor is now integrated but is not yet included in the
six-container capture harness. Its separate native interaction/visual evidence
remains mandatory before readiness.

This candidate is therefore not 'all interfaces completed'. No user acceptance,
native verification or release may be inferred from inclusion in a draft PR.

## Exact historical source reconciliation

`missing-gui-source-delta.json` pins each touched pre-existing production source
file against base `4edd018989648e1a542b699292b6ecd9d37ad1a2`. The validation helper
requires exact current bytes, reverses only recorded changes and verifies the
old hash. Historical manifests and screenshot proofs are not rewritten. New
native evidence must identify this candidate's own commit and JAR. Mutation
tests reject arbitrary source edits, wrong baselines and altered delta records.

Static review notes: retained furnace geometry checks cover 95 machine/player
anchors. Native review must also check the 250/256-high panels at a GUI viewport
of 240 pixels and the electrical speed-selector mask. These are unverified
visual conditions, not evidence of successful client qualification.

## Owner-requested steel-frame refinement

After reviewing the first actual captures, the owner requested improved steel
frames for both Factory Hopper and Factory Dropper. The structural rails, corner
fasteners, separators and slot rims now reuse the retained Placer steel texture.
The Dropper's retained control symbols remain as small bronze accents; its
control regions and slot coordinates are unchanged. The Hopper keeps its own
layout, not a copy of the Placer. No other GUI asset is changed.

Shape: stronger external bevelled rails and riveted corners. Texture: existing
Placer UV strips and rim samples only, no generated texture. Style: neutral steel
structure with limited warm warning accents. Validation requires new exact-head
client captures and user visual review; previous captures do not qualify this
refinement.
