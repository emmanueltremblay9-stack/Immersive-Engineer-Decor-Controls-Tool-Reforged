# Four existing lamps on native Double T supports

The existing Iron Bulb, Ceiling Edge, Floor Edge and Inset lights gain one native attachment slot on the original ceiling support and on both wall orientations. Alarm lamps, new items/recipes, external Multipart and changed stair/catwalk geometry are excluded. Ordinary light placement remains delegated to its existing item/block rules; the raised-catwalk Inset interaction remains unchanged.

## Placement

The original Iron Bulb-under-ceiling-support path remains the legacy none/creative/paid block property. Other compatible mounts use one native slot, mutually exclusive with that legacy bulb. The existing lamp model is only translated, with its ordinary facing, textures and UVs retained. Ceiling and Floor Edge lights retain their upper/lower edge positions and player-facing rule; Bulb and Inset retain their clicked-face orientation.

Attachment is accepted only on an internal structural face when the unscaled lamp fits in the host cell, has genuine metal contact and no structural overlap. Cell-boundary or incompatible faces retain ordinary placement. There is no inferred rotation into another lamp's shape. For the ceiling support's edge lights, the normal player-facing orientation must put their mounting plate on the beam.

Both native placement orders are supported: place the support then the light, or add the support around an existing normal light in its target cell. Wall clicks keep the approved zones even when the light already occupies that target cell. The native ItemStack placement snapshot/event owns cancellation and payment; the old light is conserved, not dropped and rebought. A second attachment is rejected.

## Ownership, selection and storage

The lamp is separately selected and mined through the existing native mining-intent and held-input guard. The shape adds no collision to the structural support. Removing a paid lamp returns its original item once; whole-support loot returns the support and its paid lamp once. Creative additions do not create paid refunds. Normal tool/loot rules follow the existing composite-support contract.

Only one boolean `support_light` marker is added to each host state. The native `double_t_light` BlockEntity stores one SupportLamp compound: Version1, the original lamp state, Paid, rigid X/Y/Z translation and the host's quarter-turn Frame. Its immutable pose resolves native host rotations, and is sent through standard update packets and NeoForge ModelData. It has no ticker, menu, inventory, energy or fluid capability. Unknown serialized payloads round-trip without creating payable items.

The hosts now have a native BlockEntity, including unlit supports; memory cost in large builds has not been profiled. Registered state counts become13824 for the original support and6144 for the wall support. This is a storage/design statement, not a performance result. The light model lookup is frozen once per bake and shared across wrappers, rather than copied for every host state.

Fluid source/current identity stays in the host's existing state. The attachment's lamp state is dry so it cannot duplicate a fluid cell. Old legacy bulbs remain readable without a migration rewrite; no personal world migration is claimed.

## Validation scope

Tests cover ordinary poses, the four identities/three mounts, two placement orders and rollback, paid/creative ownership, partial and full drops, stale completions, source/current fields, serialization/update tags, native host rotations, one-mirror asymmetric junction geometry and independent negative/double arms. Targeted client proofs use the existing nonce-guarded cloud profile and world, inspect original lamp quads/UVs and perform real process restart and held native input.

The opt-in observer counts actual ItemEntity spawn stacks during held input; it does not credit an inventory simulator or alter item gravity. This avoids mistaking a dropped item falling away from the floating fixture for a missing refund. Personal modpack, arbitrary schematic/command transformations and wall-specific alternative-renderer screenshots are not inferred from the native proof. Exact artifact/CI results are recorded in the updated PR description.
