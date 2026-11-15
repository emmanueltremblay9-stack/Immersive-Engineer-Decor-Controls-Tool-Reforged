# Tree Cutter V3 retained reference

## Provenance

The production `textures/gui/tree_cutter_v3_reference/background.png` is an exact
byte copy of `source_validated.png` in `TREE_CUTTER_APPROVED_V3_RASTER_KIT_PARTIAL_v1.zip`.

- Archive SHA-256: `025c8200f1cdeecb0e94b6a96bd1417b819e497551fc23fc0508d9bd4af5e96e`
- PNG SHA-256: `bdab10d19c0da44c113f84f1330513f4ff71e81dfe7d3eab602ce27202c0650d`
- PNG dimensions: 1254 × 1254; bytes: 2186328.

The original image was recovered and visually inspected before measuring the
integration. No source image edits, cutouts, recomposition, panel replacement or
illustration changes are included. The archive's source-original hash identifies
an earlier source encoding, not these exact `source_validated.png` bytes.

## Layout contract

`TreeCutterV3ReferenceGeometry` scales the full image uniformly by one third to a
418 × 418 logical canvas and fits that canvas into the current viewport. The
menu has one output (index 0), 27 main inventory slots (1–27) and nine hotbar slots
(28–36). Each native 16 × 16 item footprint is centered within its measured source
window. Slot input, drag deltas, rendering and public GUI bounds share one view
transform. Labels are native bounded tooltips rather than text painted over art.

The sole machine source aperture is `[895,279,1014,394)`. All 36 player windows
come directly from the retained kit's conservative `slot_regions`. Vanilla owns
item rendering, counts, carried stacks, keyboard/shift-click handling and click
packets; this screen does not construct replacement inventory logic.

The new amber energy indicator is runtime state, not an extracted art layer. It
uses synchronized `energyStored`/`energyCapacity` and is confined to logical
`[280,57,342,63)`, wholly inside source safe region `[838,170,1027,189)`.
At zero FE it paints nothing. It never repaints the rim or the saw/tree decoration.
The current Tree Cutter has no FE storage/charging mechanics, so ordinary gameplay
keeps this indicator dormant and suppresses the energy tooltip at zero capacity.
Partial/full FE checks would require explicitly synthetic visual state; this
change does not implement the deferred Tree Fell energy contract.

The normal client screen registration selects `TreeCutterV3ReferenceScreen`.
There is no development property gate. Server menu topology remains aligned with
the client; only the client applies the retained-reference presentation anchors.

## Qualification gates

CPU geometry checks cover 37 unique visible anchors, full 16 × 16 containment,
non-overlap, half-open hover edges, viewport fit, inverse transforms, transformed
drag deltas, outward public GUI bounds and bounded FE overlays. JUnit also checks
the actual packaged PNG's exact bytes/hash/dimensions. These are not native proof.

Native qualification must use the ordinary built JAR, prove its exact SHA-256 and
active resource origin, and run a separate validation mod. No screenshot-only
reconstruction or direct construction of the production screen counts as default
routing proof. Open the placed Tree Cutter through ordinary game interaction.

Required native matrix: English at 1280 × 720, French at a 260-pixel-high window,
GUI scales 1, 2 and 3 (record effective scale when Minecraft clamps it). For each:

1. Verify the default screen type, one machine output and 36 player slots.
2. Check every slot center and edge against its screen transform and source
   aperture; confirm hover/highlight does not land in adjacent art.
3. Exercise native pickup, return, shift-click output to player inventory,
   player inventory/hotbar movement and rejected player-to-output insertion.
   Check server acknowledgements and item conservation, not only the client view.
4. Check carried-stack rendering, keyboard slot actions, drag/release, reopening,
   resize and resource reload. Test ordinary localized title, output and energy
   tooltips without capture suppression, including small-window bounds.
5. Confirm the current zero-capacity Tree Cutter does not show an invented FE
   state or energy tooltip. Partial/full FE is unsupported ordinary gameplay.
   Verify active background bytes against the retained hash after reload.
6. Verify the migration/recovery mechanics separately, including old stored logs
   and non-log item preservation according to the approved migration contract.

Export only the GUI-region native captures. Do not publish whole-desktop/raw
captures, local identifiers, unrelated world/player content or private paths.
Keep test fixtures outside the distributed mod JAR. Record unrun/blocked gates
explicitly; passing CPU checks alone is not visual or gameplay acceptance.
