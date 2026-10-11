# Issue #20: structured vertical clinker slab placement

## Cause and scope

The `VerticalSlabBlock` inherited `HorizontalFacingBlock`'s player-facing placement convention. For this particular model, `facing` identifies the occupied half: the north model occupies z=0..8, while its raised brick detailing extends toward positive z. Consequently, the inherited opposite-of-player facing puts the occupied half toward the player and leaves a half-block gap against a wall directly ahead.

The fix overrides placement for `VerticalSlabBlock` only, using the player's horizontal direction. Existing model/blockstate assets, collision geometry, registry IDs, shared horizontal-facing behavior, and already placed block states are unchanged. The default north/south/east/west state meanings are preserved. No migration or automatic rotation of existing builds is performed.

## Regression coverage

`VerticalSlabPlacementGameTests` exercises actual `BlockItem.place` calls:

- Placement against walls directly ahead in all four horizontal directions
- Placement on floors and ceilings for each horizontal player direction
- Actual placed block identity and state, outline bounds, and collision bounds

The wall cases prove that the occupied half is flush against the support rather than leaving the reported air gap. Floor/ceiling cases protect the same player-relative orientation convention. The block continues to use player direction for placement, including oblique wall clicks; this change deliberately does not introduce a separate support-face orientation rule.

The source model check confirms the base body at [0,0,0]..[16,16,8], raised elements on its positive-z side, and blockstate rotations north=0, east=90, south=180, west=270.

## Qualification limits

Dedicated GameTests validate server-side placement and shapes, not client rendering. No interactive client inspection, reporter modpack reproduction, packaged dedicated-server qualification, or new release is claimed by this change.
