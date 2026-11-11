# Tree Cutter single-output inventory

This change implements one real output slot (backing index 0; menu player slots
1-36). It does not implement the deferred Tree Fell algorithm, FE charge,
top-down traversal, leaf processing or work-duration decisions.

## Capacity and ports

The existing log harvesting cadence and traversal remain unchanged. Each block's
complete loot list must fit in output 0 before the block is removed. Mixed
incompatible stacks or overflow leave that block intact. No newly produced loot
is inserted into hidden compatibility positions. Existing item capability faces,
including unsided access, stay available with one extract-only slot. Vanilla
sided and unsided container extraction cannot reach hidden positions.

## Versioned legacy migration

The owner explicitly accepted discarding old stored logs. Only an inventory
loaded without `TreeInventoryVersion` migrates, on its first server tick after
tags are available. It removes only stacks satisfying all three conditions:

- Membership in Minecraft's item `logs` tag.
- Item registry namespace exactly `minecraft`.
- Empty component patch (no customization, including removals of defaults).

This applies to saved positions 0-26. Modded, customized, non-log and unknown
items are preserved. Their continued presence is intentional, not failed deletion.
Version 1 is saved after migration. New block entities start at version 1.
Loading alone does not delete anything; saving an old entity before its first
tick keeps the absent version marker. Unknown or malformed version markers do
not trigger deletion (malformed markers are conservatively normalized to -1).
Newly produced logs survive subsequent reloads. No user world is opened by tests.

## Recovery and removal

The backing list stays 27 items with the original Items NBT format. Positions
1-26 become drain-only compatibility storage. Each server tick moves compatible
items to output 0 up to the item's and container's stack limits. Different
components do not merge. Recovery works during redstone pause; pending contents
block new harvesting so production cannot starve recovery. Capability simulation
and menu reads do not move items. Players and automation take from output 0;
a later tick exposes the next retained item. Breaking the block drops the
remaining backing inventory through the existing container removal path.

## Evidence scope

Nine added GameTests cover menu/automation, all saved positions with repeated
reload, components/stack limits, backpressure, full/mixed/partial harvesting,
multi-stack preflight, vanilla hopper extraction, break conservation, one-time
migration and unknown/new versions. The prior harvest test remains. Synthetic
save/reload is not natural chunk-unload qualification. Multi-stack coverage
invokes the production preflight; no custom real-world multi-drop log loot table
is installed. Native client/gameplay and exact-head mapped CI remain required.
