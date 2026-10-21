# Muslee read-set gate: composed revision comparisons, not transaction permission

## Scope and baseline

New inactive helper `MusleeReadSetGate` composes existing `MusleeRevisionFence`
checks across an explicitly supplied set of independent observed locations. It
supports the *necessary* complete read-set validation step behind TOOLS-315–347.
Reviewed implementation baseline: `f4e28b3e619254d40f74a7677bd9c94610e24c05`.

The helper copies the supplied canonical-state bytes as they enter each observation,
snapshots the list membership, refuses absent inputs and repeated fence identities,
and checks each captured revision and snapshot against the supplied current bytes.
Its positive `MATCHES_ONLY` result is deliberately not a grant to commit any work.
A foreign or released capture fails, stale ABA captures fail after a change followed
by restoration, and an untracked mismatch invalidates only the affected fence.

## Mandatory external conditions

- The host MUST establish all source-slot, destination-slot, press and reserve-
  compartment dependencies. This API **cannot discover omitted read-set entries**.
- The host MUST provide full canonical ItemStack identity, quantity, component
  presence and values. Synthetic bytes are not proof of Minecraft representation.
- The host MUST observe every possible mutation origin, including in-place nested
  component changes, and must hold a single server-authoritative lock from sampling
  through checks, write preflight, all inventory/reserve writes and revision records.
- Every affected write fence still requires `preflightForCommit` under that lock.
  No write is authorized by `MATCHES_ONLY` without complete downstream validation.
- An existing ambiguous/unavailable capture or unknown heap-memory upper bound
  (TOOLS-350/351) still prohibits real FIFO admission; this helper neither grants
  ownership nor enables PR #82's deliberately disabled capture admission.
- External capture ownership and safe targeted queue invalidation remain separate.
  A rejected read-set check never replays a recognized command.

## Tests and evidence

`MusleeReadSetGateCases` tests detached array inputs, null/empty/duplicate locations,
read-only invariants, stale/released/foreign captures, exact ABA, untracked changes,
independent location preservation and 8,192 deterministic three-location trials.
The companion JUnit 4 `MusleeReadSetGateTest` executes the same cases under the
existing Gradle test task. The standalone Java21 replay includes the actual
verified production `MusleeRevisionFence.java`, not a replacement mock.

Required exact-head validation: Java 21 Gradle compilation, JUnit, existing
NeoForge GameTests, client regressions and build. Standalone in-process PASS does
**not** establish actual Minecraft GUI, network, heap sizing or atomic mutations.

All new paths: no mutation of existing mod behavior or repository build/workflow
configuration. All 368 approved decisions (including TOOLS-137 A) are retained.
No GUI activation, runtime registration, item transfer, recipe, repair, release or
merge is part of this change. Follow root `AGENTS.md` for Gradle execution.
