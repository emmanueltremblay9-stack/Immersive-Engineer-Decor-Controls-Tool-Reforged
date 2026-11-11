# Muslee irreversible unified reserves

This reserve-core change supersedes the stored-ingredient identity and withdrawal
semantics in the earlier reserve-core, hardening, base-seed and tag-policy notes.
Those notes describe the historical v1 design, not the new admission contract.

## Behavior

- One accepted item becomes one generic unit. Counters are cereal, fruit and seeds,
  each capped at eight. Different accepted seeds and different item components mix.
- Accepted sources remain vanilla bread, vanilla apple, and the original five base
  seeds plus the live `musli_bar_press_seeds` tag. Naming a counter cereal or fruit
  does not silently admit additional foods or tags.
- Insert plans expose a compartment and count, never a stored or returned item.
  Source validation and atomic consumption remain the caller's responsibility.
- Withdrawal always fails. No seed type, item identity or component data is stored
  in new reserves. Native source stack limits validate offers, but do not limit a
  generic reserve's capacity.
- Tag changes affect future insertions only. Previously converted units stay usable.

## Persistence

New payload: `iedctr_musli_reserve_v2` with exactly four integer fields:
`version=2`, `cereal`, `fruit`, `seeds`. Each counter must be between zero and eight.
Unknown/missing fields, wrong types, wrong versions and concurrent v1/v2 keys fail
closed without modifying source data.

Valid v1 ItemStack payloads are strictly decoded and checked for exact codec round
trip, correct bread/apple identity, capacity and each stack's own maximum. Existing
seed extensions remain migratable even if later revoked. Reading derives counts
without rewriting the press. A no-op preserves the original representation; the
next actual staged change removes v1 and writes v2. Ambiguous or malformed legacy
payloads are left intact for recovery. Tool damage and unrelated data are preserved.

## Explicit integration boundary

This is a pure reserve helper, not a player inventory transaction or live menu.
The current held-right-click recipe and its yield of four bars are unchanged.
No input/output slots, new gesture, output destination, networking or durability
behavior is introduced. The existing runtime admission and transaction safety gates
remain unchanged. The requested future single-input/three-gauge UI is separate work.

## Validation

Updated real NeoForge GameTests cover mixed seed conversion, irreversible storage,
capacity, strict v2 decoding, valid and invalid legacy migration, tag changes,
source detachment, stale expectations, unrelated data and serialization.

Local Gradle could not start compilation: its distribution download from
services.gradle.org was unavailable in the cloud execution environment. This is not
a compile or GameTest pass. The draft PR must receive real repository CI validation.
No merge, publication or runtime installation is performed by this change.
