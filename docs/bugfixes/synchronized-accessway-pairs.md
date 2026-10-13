# Synchronized adjacent accessway clicks

Implementation base: `44c126990506f866825eaa88dd7c1e210a6920ea` (main after PR #45).
Draft feature work only. This document does not establish a qualified or published JAR.

## Supported scope

- A matching pair of ordinary hand-openable vanilla doors, including wooden and
  copper doors. Doors must be the same registered block, on the same level, with
  the same facing and reciprocal opposite hinges.
- The IEDCTR Old Industrial Wood Door follows the same rule.
- Two side-by-side vanilla fence gates of the same registered block and facing
  axis. Native gates can reverse their facing when opened from the other side;
  north/south and east/west are therefore the respective matching axes.
- Two matching IEDCTR Steel Mesh Fence Gates, either both single-height or both
  two-segment. A click on either valid segment addresses the complete gate.
- Mod-registered `DoorBlock`/`FenceGateBlock` subclasses only when their block-use
  methods remain inherited unchanged from the native classes. The runtime tests
  include an inherited-only door subclass. Custom interaction overrides are
  deliberately unsupported, even if they currently return PASS.

Gate adjacency must be unique in both directions. A contiguous row of three or
more matching gates remains individually operated. There is no row traversal or
chain propagation. Door hinges establish one reciprocal neighbor at most.

This is main-hand click synchronization. Trapdoors, Iron Hatch, iron doors,
custom lock/interaction implementations, and arbitrary mod-specific accessways
are not automatically adapted. The Metal Sliding Door keeps its existing
persisted pair, interaction, collision and redstone implementation unchanged.

## Interaction and permission boundary

The feature observes native server interactions rather than setting `OPEN`.
It records the source right-click and native BLOCK phase, then requires a
same-player, same-position, same-tick `BLOCK_OPEN`/`BLOCK_CLOSE` event with a real
change from the recorded state. The steel segmented gate now emits that native
game event after its existing segment update succeeds.

The companion must be a valid current pair and accessible to the same player.
The companion path checks loaded chunks, reach, world border, feature flags and
`Level.mayInteract`, then runs:

1. `RightClickBlock` protection hooks, retaining their final cancellation and
   block/item-use decisions
2. The native `ItemStack.onItemUseFirst` wrapper and `ITEM_BEFORE_BLOCK` hook,
   only when the held item's hook is the inherited no-op implementation
3. The native block `useItemOn` wrapper and its BLOCK hook
4. Native `useWithoutItem` only for an uncancelled `PASS_TO_DEFAULT_BLOCK_INTERACTION`

Ordinary held blocks and sticks remain usable. Items overriding the early-use
hook are not replayed on a companion. There is no item-placement fallback,
inventory substitution, direct state write, or retry that forces a refused leaf.
Cancelled events are checked explicitly, including cancellation whose result is
the default PASS. Nested duplicate phase events are treated as ambiguous and
stop forwarding. A scoped `try/finally` guard prevents recursive pair handling.

Event handlers can change the world. The pair, block identities, halves, powered
state and desired open state are therefore checked again after companion hooks.
Already synchronized leaves are not toggled twice; removed/replaced blocks are
not recreated from old state snapshots.

Public NeoForge events describe the normal synchronous native interaction path.
This is not a universal compatibility guarantee for third-party code that
fabricates player-caused game events, mutates blocks inside interaction listeners,
or replaces the normal interaction pipeline. Unknown custom block/item methods
fail closed rather than guessing their authorization contract.

## Redstone, saves, and previous fixes

No redstone signal or `POWERED` value is copied. Native powered doors/gates are
excluded from companion forwarding while powered; their individual native
behavior is unchanged. The manual-only steel gate gains no redstone response.
Existing Metal Sliding Door paired redstone behavior is unchanged.

No new block state, block entity, pair cache in saved data, NBT field, model,
texture, collision or fence-connection change is required. Pairs are derived
from current valid states for each interaction, including after a save/load.
Removing a partner leaves the remaining accessway individually usable. PR #45
gate passage and fence-connection fixes remain intact.

## Verification

`SynchronizedAccesswayGameTests` invokes the real server game-mode interaction
method through a NeoForge `FakePlayer`. This avoids firing a mock network login
that cannot negotiate Immersive Engineering's custom payloads. It does not
replace source interactions with direct OPEN writes.

Test fixtures are registered only when the dedicated run's
`neoforge.enabledGameTestNamespaces` includes this mod. They use a separate test
namespace and add no ordinary-play content or assets.

Coverage includes four facings, both door halves, gate reversal, both segment
heights, opposite-side clicks, repeated same-tick use, event counts, singles,
ambiguous rows, mismatched/incomplete pairs, native iron refusal, custom lock and
item-method exclusion, held-item preservation, source/target event vetoes,
default-PASS and late/nested vetoes, replacement/removal during hooks, NBT state
round trips, local redstone, and expired/same-tick cancelled source context.

Initial tests-only CI compiled, but failed at mock-player login before reaching
feature assertions. That run is not behavioral RED evidence. The fixture was
changed to NeoForge FakePlayer before requesting a second tests-only run.
Exact-commit RED/GREEN CI results and client/packaged-server qualification must
be recorded separately when observed. A static review or passing publisher
tests alone does not establish runtime feature correctness.
