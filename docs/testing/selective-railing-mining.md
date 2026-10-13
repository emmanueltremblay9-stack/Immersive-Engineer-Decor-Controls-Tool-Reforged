# Selective railing mining and fence-height protection

## Player behavior

- Left-click and finish mining the visible railing side to remove only that side.
  Other sides retain their saved state, waterlogging and purchased-item balance.
- Mining the last standalone side removes the block. Mining a catwalk rail keeps
  the floor; mining the catwalk floor removes the floor and its remaining rails.
- The outline shows the same selected component that mining uses. A nearer rail
  or another block occludes a farther rail. Shared boundary ties choose the floor
  first, then north/east/south/west. The legacy all-false standalone still means
  its existing visible north rail.
- Existing right-click side editing and support-top addition remain available.
- Visible models, textures and joins are unchanged. The physical guardrail is
  1.5 blocks above its walking surface: Y=24/16 for standalone railing and Y=27/16
  above the catwalk block origin, whose floor is Y=3/16. An open side stays open.

## Native mining and protection

`LeftClickBlock.START` records targeting only; it never removes or refunds a
side. Completed mining uses NeoForge's `onDestroyedByPlayer`, after native mining
progress, permissions, BreakEvent and tool wear. The final START-event veto is
checked again, including a listener that runs later at the same priority.
The accepted BreakEvent is also retained until completion, so a later veto
cannot leave a reusable token for a completion without a fresh mining start.

The starting block state and component must still match at completion. An abort,
retarget, concurrent edit or replacement cannot turn an intended rail break into
floor or replacement-block destruction. Client continuation resets through native
stop/start mining when the component changes within the same block, giving the
new component its own normal mining progress. Logout, dimension and player-clone
events clear pending targets.

Partial survival mining drops one recorded paid addition only when native
`willHarvest` permits it. The drop uses `Block.dropResources` with a pre-mining
tool snapshot, preserving ordinary loot hooks, `BlockDropsEvent` and `doTileDrops`.
Wrong-tool mining still removes the chosen side and destroys its credit, without
a refund. Creative mining drops nothing. Final standalone mining and catwalk
floor mining use native whole-block destruction and existing loot. Old unpaid
sides never acquire new refund credits.

## Verification and limits

- `SelectiveRailingMiningGameTests`: actual player eye/view raycasts and native
  START/STOP/completion paths, all sides, floor discrimination, timed progress,
  paid/legacy/creative/wrong-tool loot, tool breakage, drop and interaction vetoes,
  stale/aborted targets, occlusion, support blocks, water and NBT round-trips
- `NativeRailingGameTests`: prior material-conservation tests now mine the catwalk
  floor or each standalone side through the native aimed interaction path
- `NativeRailingJumpBarrierGameTests`: actual `Entity.move` with deterministic
  normal jump impulses, ordinary stepping and bounded pushes at all four sides
  and corners; open-side controls and exact selection/collision shapes
- `RailingSelectionGameTests`: inside/outside/top rays for every side, floor
  top/bottom rays, solid occlusion, legacy north selection, original visual
  height and deterministic single-component corner outlines
- Existing walking-containment and contextual-manual tests remain required

Headless jumping uses an ordinary 0.42 impulse, 0.08 gravity and 0.98 vertical
drag with an adult zombie body. This tests physical collision, not the client's
auto-jump input or autonomous mob AI. Flying, climbing, teleporting, jump boosts
and modified movement are outside this guarantee. NBT round-trips are not a full
server restart. Client outline rendering, held-button continuation, protection
denial prediction and actual auto-jump remain manual client validation items.

CI results must be tied to the exact commit; this document does not claim a
passing run or release qualification by itself.
