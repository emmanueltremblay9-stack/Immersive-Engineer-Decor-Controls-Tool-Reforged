# Pole head/foot placement restoration

## Cause and contract

All three head/foot items were registered as generic surface-mounted plates.
That lost the pole-specific placement inversion and demanded a sturdy full face,
so the pieces could not function as heads on narrow poles. Their collision shape
also covered only a thin plate instead of the visible full-length structural end.

The upstream reference is `stfwi/engineers-decor` commit
`a561e61e35ce0367088356967e1ea4cad5c1f27d`,
`EdStraightPoleBlock.getStateForPlacement` and the three head registrations in
`ModContent`: clicked-face placement, inverted when the clicked neighbor is a
pole. Looking upward/downward alone is not the placement contract.

## Restored behavior

- Thick steel, thin steel, and treated-wood heads use one dedicated structural class
- Clicking a solid top places a foot; clicking the top of a pole places a head
- Bottom and horizontal clicked faces obey the same inversion, independently of camera pitch
- Existing pole heads and treated-wood pole supports count as pole connections, as upstream
- The three existing registry IDs, items, recipes, models, and six saved facing values remain unchanged
- A placed head does not disappear or rotate when an adjacent block changes
- Full-length shaft and end-plate collision/selection replace the old isolated small plate

Ordinary straight poles and treated-wood angled support behavior are not changed.
This change does not rewrite existing head orientation in a saved world.

## Automated checks

`PoleHeadGameTests` uses real `BlockItem.place` across three head families,
eight clicked-support types, all six clicked faces, and three player pitches.
It also round-trips every saved facing through NBT, removes adjacent supports,
and verifies the full-length selection/collision shape.

The test-only commit `e9262317e0c80dda9fc5fddce11f6b1da72abee3`
was compiled and tested in [RED run 38406087922](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/38406087922).
Compilation and unit tests passed; exactly the two new GameTests failed on
narrow-pole placement and disappearing structural ends. See draft PR54 for the
final exact-commit CI result. Local Gradle execution was blocked while
downloading the wrapper distribution; do not treat a local command as a pass.

## Remaining client verification

In a separate test world, compare all three families placed against floor,
ceiling, vertical pole tops, and horizontal pole ends. Confirm matching visible
head/foot plate location and selection outline, then reload the world. No user's
running Minecraft instance or live save was accessed for this change.
