# Passive Fluid Accumulator waterlogging

## Scope and compatibility

The existing `passive_fluid_accumulator` ID, item, block entity, GUI/menu, 4000 mB
tank, bucket slots, capabilities, collision shape and model are preserved. The catalogue
extension makes its existing machine subclass inherit the native exterior-cell protocol.
All 17 entity machines and three entity-free valves are explicitly integrated separately.

The original `waterlogged` boolean retains its name and false legacy default. New
`fluid_cell` (empty/lava/creosote) and `water_flow` (0..15, default 0) properties use ordinary
block-state save/palette/update codecs. Source water/lava/IE-creosote placement preserves
the replaced source; flowing input never becomes a stored source. Natural transient
water follows vanilla levels/recession. Lava and current never become tank water.

The existing blockstate JSON has a single `""` variant, which matches all exterior-cell states. Both states therefore retain the existing `block/device/passive_fluid_accumulator_model`; no duplicate model, item or resource registration is required.

## Collection and interaction contract

Collection code is deliberately unchanged:

- One collection adds 250 mB per 80 machine ticks, up to the existing 4000 mB capacity
- Any one of the six adjacent source-water cells qualifies, including a waterlogged adjacent block
- Multiple sources do not multiply the collection rate
- Flowing water does not qualify
- The accumulator's own cell is not checked, so waterlogging alone does not generate tank water
- Existing passive collection does not consume the neighboring source and needs no FE

Normal bucket use retains existing tank filling/draining. Sneaking bypasses the machine interaction and lets the real bucket item fill or drain the exterior world cell. World water and tank contents are independent. Repeated world pickup returns nothing when dry; pipe drainage never drains world water. Normal destruction leaves the exact exterior source/current in the world. A normal empty bucket cannot fall back to draining that cell when the tank is empty or below 1000 mB.

## Automated coverage

`PassiveAccumulatorGameTests` adds ten focused GameTests:

1. Real survival `BlockItem` placement through all six clicked faces, dry and underwater, with item consumption, preserved entity/kind, 4000 mB capability and no FE
2. Actual item placement in air, flowing water and lava without minting a source
3. Real normal player bucket interactions preserving the existing tank behavior in dry and wet states
4. Real sneaking player interaction followed by bucket raycast, repeated filled/empty bucket cycles, scheduled ticks, duplicate liquid insertion rejection, unchanged entity and independent tank balance
5. Reconstructed entity plus block-state save/load, inventory/tank preservation, post-load neighbor updates, and safe legacy NBT/codec defaults
6. Actual machine ticks for all six source directions in dry and wet states, own-cell exclusion, flowing-water exclusion, exact cadence and stopping when the adjacent source is removed
7. Adjacent waterlogged blocks, multiple-source rate, capacity, bucket input/output slots, simulated/executed pipe drainage and rejected lava storage
8. Real registered server ticking underwater, then stopped collection after the only adjacent source is removed
9. Scheduled fluid propagation into a bounded neighboring cell, no new source, and no source recreation after pickup and later fluid updates
10. Normal player destruction, one remaining world source, one bucket pickup and no second bucket

The existing machine GUI/capability tests remain applicable because the new block is still a `MachineBlocks.MachineBlock` with the same `MachineKind`.

## Validation status

The ten original contracts remain required regression tests. Their old water-only
assertions now distinguish exterior source lava from newly created water and reject
occupied native cells before bucket consumption. The complete catalogue server suite
passes 485 GameTests; additional machine tests cover full entity NBT/reload, occupied-cell
machine ticks, normal-bucket fallback with0/750/1000/4000 mB, incompatible creosote buckets
and own-cell/lava/current collection exclusions. Exact-commit CI remains the client gate.
No installed Prism JAR or user's world was accessed.

## Manual in-game checks before release

1. Place the same accumulator item on dry ground and into a source-water pool. Check the model and selection shape, retained water, GUI and pipe connections
2. Test ordinary filled/empty bucket use against the tank; then sneak-use both buckets against world water, with an empty and a partly filled tank
3. Keep one adjacent water source and observe collection. Remove it, leaving only the accumulator's own waterlogging, and verify collection stops
4. Let water flow into a neighboring empty cell, pick up the waterlogged source, then update nearby blocks repeatedly; check that no extra source appears
5. Save and reload, checking world water, stored amount and bucket inventory. Also load an older world containing a dry accumulator
6. Break a waterlogged accumulator, pick up the remaining source once and verify that there is no second source or extra bucket. Confirm the ordinary block/inventory drops remain unchanged
