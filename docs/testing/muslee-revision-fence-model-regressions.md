# Muslee revision-fence deterministic model regressions

## Scope

Test-only follow-up to merged PR #71, pinned to implementation `main`
`f4e28b3e619254d40f74a7677bd9c94610e24c05`. Uses the actual production
`MusleeRevisionFence` without substitute implementations or Minecraft stubs.

The standalone `MusleeRevisionFenceModelCases` and JUnit 4 wrapper execute the
same state-machine assertions with 16 deterministic seeds, 4,000 operations per
seed (64,000 total), and three independent location fences per trial. The
reference oracle tracks revision, valid tracking, captured bytes and live capture
ownership across random capture, close, atomic preflight, committed change,
no-op, refused transaction, ABA comparison, unrelated-location capture,
uncertainty, invalid preflight and reinitialization attempts.

Important acceptance properties:

- Every effective committed change advances the 64-bit revision exactly once;
  no-ops, rejected work and preflight never advance it.
- Already captured complete bytes stay detached after changes to source buffers.
- A changed-then-restored state cannot validate an old revision.
- Unknown/untracked mutations become unusable and refuse further captures.
- Reset cannot succeed with unreleased old captures or without authoritative
  tracking proof; no stale generation can become current again.
- Closing captures twice does not double-release; released captures cannot be
  read or used for validation.
- A foreign capture is rejected without making unrelated location tracking fail.

## Exact validation boundary

The tests prove *only* the standalone revision-fence behavior under supplied
canonical byte arrays, not the existence or completeness of a Minecraft
`ItemStack` canonical serializer or capture of all actual mutation origins.
They do not demonstrate TOOLS-346/347 runtime coverage, memory-usage bounds,
serialization ownership, thread safety under the engine, durable journal safety,
GUI commands, inventory mutations or end-to-end atomic transactions. Existing
fail-closed guards remain mandatory.

No production files, dependencies, workflows, registered gameplay or release state
are changed. PR #82's capture-store work is not touched. No new design choice is
introduced. All 368 approved decisions remain unchanged, including TOOLS-137 A.

## Validation

Run on the exact branch head:

```sh
./gradlew compileJava processResources --stacktrace
./gradlew test --stacktrace
./gradlew runGameTestServer --stacktrace
./gradlew build --stacktrace
```

Local standalone Java 21 evidence does not count as a Gradle/NeoForge PASS.
The existing project CI and root `AGENTS.md` safeguards apply. No merge,
release, permission changes, new recovery rights or GUI activation are included.
