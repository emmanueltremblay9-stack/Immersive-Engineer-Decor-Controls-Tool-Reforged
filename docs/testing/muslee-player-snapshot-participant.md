# Proposed native player snapshot participant

Inactive real NBT/ItemStack participant codec. This is an implementation step toward
normal player-state cursor restoration, not a save/load hook or an activation grant.
It does not replay journal commands or call setCarried, load, save files, or mutate
inventory. Normal player snapshots may contain their ordinary Inventory; the journal
still contains only minimal evidence, never these complete snapshots.

## Proposed schema version1

A required namespaced root compound `iedctr:muslee_player_participant` contains:

- INT version1, canonical worldId/playerId UUID strings bound to expected identities.
- STRING cursorState exactly `empty` or `carried`.
- COMPOUND cursor only for carried, with exact native ItemStack representation.
- COMPOUND receipt: canonical random version4 transactionId/pressId (distinct),
  positive LONG writerGeneration, outcome EXECUTED or NO_EFFECT, and exactly32-byte
  beforeHash/afterHash arrays. NO_EFFECT hashes must agree; agreement is not proof.

Native root UUID must match the expected player. Unknown participant/receipt keys,
wrong exact NBT types, missing fields, noncanonical identities, malformed/oversized
stacks and invalid state combinations reject. This is a new proposed participant
schema. It does not define the eventual authoritative receipt-hash encoding or
claim those supplied hashes prove execution.

Read distinguishes ABSENT, VALID and INVALID. ABSENT is only structural absence;
a known participating player must block if it occurs. Never interpret it as an
empty cursor or cancelled transaction. Empty cursor state explicitly retains the
receipt. Returned receipt arrays and cursor copies are detached. Native unrelated
snapshot fields are preserved by staging, and malformed existing participants are
not silently overwritten or removed.

## Qualification limits

The caller supplies a complete authoritative native snapshot. Identity checking is
not a complete validator of every vanilla/modded player field or Inventory slot.
The codec detaches cursor state using the existing narrow ItemStack capture: unsupported
components fail closed rather than being silently dropped. In particular, arbitrary
rich names/modded component graphs are not qualified. Full player serialization,
NBT copying, decoding and component allocations are not an aggregate memory proof.
The command capture gate remains closed.

VALID is structural readability only. It grants no writer authority, load ownership,
rollback, retry, cancellation, restore permission or durable outcome. Staging a newer
snapshot still needs the actual transaction fence, complete read/write sets and an
authoritative selection of the world/player snapshot and its receipt history.

## Required later integration

Normal player loading may reconstruct the saved authoritative cursor only after
validated snapshot selection establishes a single owner. The player/lifecycle adapter
must prevent a second restore and stale saves. After native cursor-to-inventory
transfer, the next coordinated snapshot holds inventory bars plus an explicit empty
participant cursor and retained receipt; removing the participant is not sufficient.
World drops cross the player/entity persistence boundary and remain unqualified.
Integrated-host world-player snapshots, backups, supported-platform force/replace,
writer fencing and crash failure injection still need implementation and proof.

No live attachment registration is used, avoiding silent optional-attachment omission.
A native re-save without a future required participant hook would omit this root
field; this is a known integration blocker, not an accepted fallback.

This version can structurally replace an existing valid receipt. It does not enforce
retention of unresolved receipt history across subsequent transactions; the future
owner must refuse that replacement until history retention is qualified. Native
ItemStack parsing precedes narrow capture validation, so this API must not be
advertised as bounded hostile-NBT ingestion. Real validated save/load boundaries and
complete memory qualification remain required upstream.

## Engine tests

Six GameTests exercise actual ServerPlayer, ItemStack and NBT behavior: native press
state preservation beside the participant; detached custom-data cursor copies;
test-only composition of native player load followed by explicit cursor assignment;
carried-to-inventory transfer followed by EMPTY with unchanged receipt; immutable
receipt arrays; strict schema/type/identity/count mutations; unsupported cursor
components; and native re-save into a fresh CompoundTag omitting the inactive field.
The explicit assignment is only a fixture, not a runtime single-owner load adapter.
Actual compilation and execution must be established by CI on the published head.
