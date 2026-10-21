# Muslee FIFO and revision-fence foundation

This is an inactive production foundation following reserve core PR68 and protocol
/session ingress PR69. It does not register networking, menus or screens, modify
inventory, activate reserve pressing, repair or change durability. The actual GUI
must remain inactive until the remaining adapter and transaction guarantees exist.

## Queue contract and integration boundary

The queue models one press's volatile FIFO ownership and accounting state. A host
must maintain exactly one queue per durable press UUID, validate the current GUI
session through the approved ingress, and supply trustworthy captures and byte
upper bounds before admission. A numeric bound is not a memory-sizing proof.

The intended contract is 32 non-engaged commands plus at most one engaged command,
strict server-admission FIFO without type priority, and a 262144-byte reservation
budget for non-engaged captures. Atomic engagement removes the head, transfers its
reservation/ownership exactly once, and excludes it from the waiting budget while
retaining the engaged lease until authoritative finalization. Session invalidation
must target only non-engaged work and preserve the active lease.

This queue stores ticket metadata, not ItemStacks or actual capture graphs. Its
reservation ledger and terminal ticket states do not free an external capture store
by magic. Integration must release each actual capture safely exactly once, retain
necessary engaged captures, prove the bound includes nested components/copies and
all overhead, and refuse admission when that proof is unavailable. No guessed
Minecraft/JVM heap-size estimator is supplied by this PR.

Expiration uses a monotonic clock and the original admission instant, not ticks or
wall time. Commands become ineligible at exactly 30 real seconds, even if physical
removal is deferred. Expiration cleanup and resumption are bounded to eight examined
commands per pass. Required targeted invalidation is distinct from expiration
cleanup. Coalesced continuation demand must be driven by the server tick adapter;
there is no new polling timer or automatic replay.

If bounded expiration cleanup cannot finish before an admission capacity decision,
CLEANUP_REQUIRED is silent backpressure, not a false queue-full indication and not
permission to retry the rejected action automatically. Its recognized UUID remains
consumed in ingress. A later fresh manual interaction has a fresh action UUID.

## Revision-fence contract and integration boundary

Independent source-slot and reserve-compartment fences use nonnegative signed-long
revisions, immutable complete-state snapshots and generation identity. A change
followed by restoration must invalidate old captures, even if the final content is
equal. No-op or rejected transactions must not advance a revision. Overflow or
uncertain tracking makes only that fence unusable; reset requires exclusion of old
capture references and cannot revive an old ticket. Every affected location must
pass preflight under the common authoritative lock before any inventory write;
a snapshot match alone is not write permission. Released snapshots are cleared
and cannot be read or reused.

The standalone fence accepts canonical complete-state bytes supplied by a trusted
adapter. This is not an ItemStack serializer and not proof of complete component
comparison. The adapter must represent exact logical identity, count, all component
values/presence and nested state, then observe every mutation from every origin,
including internal component changes. Polling equality, inventory setChanged,
slot replacement alone and menu state IDs cannot establish the approved anti-ABA
contract. If this cannot be proved, affected engagement must remain refused.

## Before any GUI activation

- Bind the merged protocol/session ingress to authoritative sender/session/press
  ownership; do not create parallel queues for one durable press UUID.
- Implement and prove capture-size accounting and immutable complete ItemStack
  snapshots, including reference release and protected engaged ownership.
- Implement complete mutation tracking for source slots, destinations and reserve
  compartments, with targeted queue invalidation on fence uncertainty/reset.
- Compose queue selection with live session/read-set/destination revalidation and
  an atomic inventory-plus-reserve commit. A queue engagement is not that commit.
- Connect safe session invalidation, authoritative transaction reconciliation,
  monotonic clock failure and single-tick continuation delivery.
- Only then register the menu/screen/payload and qualify actual load/withdraw paths.

Contract references: TOOLS-279–287 (FIFO/lifecycle), 313–325 and 342–347 (revisions
and complete immutable captures), 348–359 (memory/ownership), 360–368 (bounded
resumption and exact monotonic expiry). No design choices are reopened.

## Validation

Production classes and focused JUnit tests are included. Existing exact-commit CI
must compile under NeoForge, run JUnit, the 506 existing GameTests and existing
client/renderer regressions. Terminal evidence belongs in the PR description.
These results must not be presented as an active Muslee GUI or full transaction
qualification. No desktop build, installation, release or upload is required.
