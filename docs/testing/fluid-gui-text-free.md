# Retained Funnel and Accumulator V1 integration

## Critical review and correction

The first text-only pass cleaned the legacy gray panels, but it did not integrate
the orange/metal V1 concepts retained in [GUI DEV PR5](https://github.com/emmanueltremblay9-stack/Master-Generalist-GUI-Architect-Designer-DEV/pull/5).
Green tests of that old canvas did not establish visual fidelity to the newer
references. The largest defects were the wrong artwork and proportions, the
legacy mask patches, a baked blue stream despite an empty Funnel, and static
rain/water artwork on Accumulator. Both retained originals are already text-free
and state-neutral, so those repairs and false-state cues are now absent.

The originals are copied byte-for-byte from source commit
`0c166b34fb19cdf4a54373ff5e9c4770a854be12`, directory
`candidates/iedctr/retained-concepts/2026-11-04/`:

- `small-fluid-funnel-v1.png`: SHA-256 `c5dcc2ee56fdace5f9bf66456e0a4dca91ce4fddb78887a9a6b60ff914336a64`.
- `passive-fluid-accumulator-v1.png`: SHA-256 `da2a83c0f8cc68be5f69ad4f3cc392cc4a600206ae39ac8b81142e421e466872`.

They are flattened concepts, not a new layered kit. No image pixels were edited.
Historical source files and their provenance remain in the repository.

## Runtime mapping

Both whole 1254-square originals map to a 256-square GUI without aspect distortion.
The separately fitted inventory columns are `[44,63,82,101,120,139,158,177,196]`,
rows `[138,156,175]`, and hotbar row `206`. Every real item stays 16-square.
The two large machine frames contain ordinary 16-square slots: Funnel `(40,70)`
and `(200,70)`, Accumulator `(40,100)` and `(200,100)`. Their full decorative
faces are not new click targets. Container indices, input/output roles,
27+9 inventory and all menu/backend behavior remain unchanged.

The exact renderer paints only synchronized stored-fluid height, bottom-up and
clamped to the existing capacity, inside bevel-clipped glass. Translucency keeps
the original reflections visible. Water/lava/milk/generic Funnel codes retain
their existing meaning; generic fluid is not mislabelled as water. Accumulator
continues to contain only its existing water. There is no simulated incoming
stream, rain, activity, energy or production animation. Orange pipes and hotbar
end lights remain decoration, not operating-state indicators.

Identity is available on the header plate, storage on the visible glass,
Accumulator collection instructions on the top cap, and Funnel outlet
information on the lower outlet. Empty-slot role tooltips and occupied-item
hover remain unchanged. No permanent title or amount is drawn.

## Offline verification

Run `python -W error::ResourceWarning -m unittest discover -s tools/validation -p 'test_*.py' -v`.
All 157 tests pass. The eight fluid tests compile the exact renderer and screen
hover/label methods with recording doubles plus the actual pure geometry helper.
They cover empty/half/full/overflow/negative values, invalid capacity, all five
Funnel fluid codes, three origins, exact blit/source geometry, glass clipping,
transparent fill, and machine-slot tooltip separation. Negative controls restore
text, remove outlet hover, make water opaque or shift an item into a metal rim;
each is rejected.

The Java fixture decodes both original PNGs and examines every source pixel under
all 38 mapped item rectangles per GUI, including hotbar and machine slots. Those
footprints must remain opaque dark interiors rather than metal. Original hashes
and EN/FR tooltip keys are checked. Geometry/layout JUnit tests additionally
cover all slot positions, no overlap, invalid indices, bounded fill, and roles.

Syntax parsing passes for 315 Java sources, and diff checks pass. This is not
mapped compilation. The local workspace lacks Gradle's distribution and cannot
download it; mapped compilation, JUnit and native evidence must come from CI.

## Native qualification and remaining criticism

Fresh exact-commit Minecraft EN 1280x720 and FR 1280x260 captures are required;
legacy-gray captures do not qualify this integration. Check empty/partial/full
levels, all slot alignments, frame/glass preservation and small-window fit. The
256-square canvas fits the existing 260-pixel-high stress window at GUI scale 1;
smaller logical viewports remain a limitation, not a new adaptive-scaling claim.

The retained concepts are visually busy and the machine-slot frames are larger
than the item hitboxes. Keep those tradeoffs visible during review rather than
inventing enlarged controls. New title/cap/outlet/tank targets are tested in
isolation; the existing native fixture does not establish all those pointer
paths. Physical pointer/drag/shift-click interactions, other fluid types in the
new glass, resource packs/reload, full reporter-modpack play and owner visual
acceptance remain unqualified until specifically exercised. No merge or release.
