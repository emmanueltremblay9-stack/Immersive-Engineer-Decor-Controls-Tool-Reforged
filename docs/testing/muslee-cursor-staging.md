# Stage slotless Muslee cursor interactions

This follow-up depends on the generic reserve core in PR #155. It supplies pure
cursor/reserve plans for the requested interaction, not an active GUI or transaction.

## Interaction contract

- The future invisible input region covers the whole funnel and the space above
  its opening. It accepts an ingredient held on the cursor. The three gauges are
  passive; there is no visible input slot. The accepted amount converts to generic units; any amount
  beyond the relevant gauge's remaining capacity stays on the cursor.
- The future depicted muesli bar is an invisible press button. One activation uses
  one unit from each reserve and creates four bars on the cursor, never in inventory.
  No animation is introduced.
- Pressing requires an empty cursor or an exactly component-compatible bar stack,
  and room for the entire four-bar batch. Incompatible/full/insufficient cursors,
  missing ingredients and exhausted/unqualified durability reject without mutation.
- Plans and returned cursor stacks are detached. The existing 0..64 qualified
  durability policy is used: the final successful batch returns zero durability,
  not a tool-deletion operation. Absorption leaves durability unqualified/unchanged
  (reported as -1, never a value to write onto an item).

## Required server integration, not implemented here

A plan is not authorization to apply either side. No real inventory, menu cursor,
press component, slot, packet, UI hitbox, animation or network registration changes.
The existing held-right-click gameplay remains untouched. The adapter must capture
and revalidate server-owned cursor, press identity, reserve, durability and session,
apply all deltas atomically with rollback/recovery under the existing revision and
journal requirements, and synchronize cursor/gauges only from authoritative results.

Existing capture-memory admission and transaction tracking gates remain blocked.
The dormant old INSERT/WITHDRAW protocol is not activated or silently repurposed.
The legacy live item still has 128 durability; migration to the approved 64-point
model must be qualified separately. This helper cannot make that decision.

## Verification

Real NeoForge GameTests cover cursor room at 0/1/60/61/64 bars, incompatible and
named components, malformed source limits, qualified durability boundaries,
missing reserves, mixed seed absorption, overflow leftovers, immutable snapshots,
and repeated batches with exact item/unit conservation. Repository CI is required
for engine validation. No live Muslee interaction or complete atomicity proof is
claimed by these staging tests.
