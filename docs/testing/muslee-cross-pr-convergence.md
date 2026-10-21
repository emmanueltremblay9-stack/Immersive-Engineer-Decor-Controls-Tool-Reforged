# Muslee cross-PR convergence CI: integration carrier ONLY — DO NOT MERGE

## Exact source and intended use

This branch is an **isolated CI carrier**, not a proposed replacement for the separately
reviewable PR #80, #86 or #87. Its purpose is to run all their real Java/JUnit and
NeoForge GameTest logic on **one exact commit** after PR #82 became part of `main`.
Do not merge this carrier; integrate each owning PR individually after its own
review, and rerun target CI after every real integration.

**Requalification base implementation `main`:**
`6d159f045dc442de3414e89e81af8616e6be4ad8`.
**Original carrier base (historical):**
`934024f2b313624c0053e3571e0c88d3553e88bc`.
The root `AGENTS.md` safeguards apply. The source PRs and exact HEADs selected are:

- **PR #80** — targeted FIFO invalidation, HEAD
  `967ac7ff75c3386dd195aa959fb6c5a4ff6c1be8`.
  Preserve its `invalidateWaiting(ticket)` method and import its unchanged test/doc
  blobs, but **do not replace** the newer PR #82 queue with the old PR #80 queue.
  Integrate the additive 18-line hunk into the exact current `main` source instead.
- **PR #86** — exact-size two-pass output, HEAD
  `883022d506f5f127965c20730cd675625429c49b`.
  Import its modified `MusleeItemStackCapture.java`, new exact-output class,
  JUnit, NeoForge GameTests and original bound-progress note. The changed capture
  file must be reconciled with the exact main blob, not assumed universally safe.
- **PR #87** — runtime admission remains gated, HEAD
  `0232d249a190633a224a8c28f99693fd2f560f82`.
  Import its new real NeoForge capture-admission GameTests and scope doc unchanged.

A **new test-only Cases/JUnit pair** verifies joint PR #80 + PR #82 behavior:
real concrete owned buffers (as opposed to the old scalar-only tickets), a single
waiting ticket removed without affecting other work, exact one-time release,
engaged ownership on invalidation/clock uncertainty, FIFO survivors, expiry at
30 real seconds, public capture-admission fail-closed, and 128 races between
engagement and targeted invalidation. The package-private ownership fixture is
used solely within tests; its scalar number is **not** a memory proof.

## Requalification after main advanced

The previous carrier HEAD was `678a46f7eac40ef2313ff3593a057311cb717c16`.
The new QA-only merge preserves that carrier's production and test blobs exactly,
including the queue's PR #82 ownership and PR #80 targeted invalidation. It imports
all 16 added files from the new main base unchanged: the press-batch, repair,
withdrawal and rotation plan/policy helpers, their Cases/JUnit wrappers and docs.
The main delta and the carrier's 14 changed paths do not intersect. The only
newly authored change is this scope note; there is no new runtime implementation.

Previous CI success is historical evidence for its original tested base, not a
PASS for this requalification. New push and pull-request runs must identify the
new carrier HEAD and their actual checkout SHA. A pull-request synthetic merge
must not be silently equated with the branch HEAD. Record terminal conclusions,
GameTest counts, JAR digest and remaining gaps in the review receipt. If main or
a source PR advances again, report that drift instead of promoting this pinned
composition to a current-main qualification. No local Java/NeoForge execution
was performed during this requalification preparation.

## Validation distinctions

- Local Java 21 `javac --release 21 -Xlint:all -Werror` and Cases replay **PASS**
  on the exact combined queue, but with an explicitly minimal test-only
  `MusleeItemStackCapture` stub. It validates API/lifecycle composition only;
  it does not prove the real capture codec or NeoForge behavior.
- GitHub Actions on the carrier's exact HEAD must complete all Gradle/JUnit,
  dedicated NeoForge GameTests (including imported #86/#87 cases), client,
  renderer and build jobs. Report actual terminal evidence and failures.
- Verify that the original production gate `admitCapture(...)` still returns
  `INVALID_BOUND` until the complete TOOLS-351 aggregate retained/peak-heap proof
  is independently established. Faster/exact output encoding is **not** that proof.
- Fail closed on unknown components or mutation origins. Never infer
  TOOLS-346/347 completion, complete transaction read/write sets, lock authority,
  data durability or inventory commit from an isolated CI PASS.

## Publication and integration boundaries

**DO NOT MERGE this QA carrier.** It temporarily repeats another PR's modified
files to test their combination; that is not a request to supersede the source
PRs, rewrite history or publish a mod. No GUI/menu/network registration,
pressing, repair, transaction execution, recovery, memory gate bypass, dependency,
workflow, release, tag, permissions or protected setting is changed.

All 368 approved decisions, including TOOLS-137 A, remain unchanged. External
writes performed for this QA carrier are restricted to a dedicated branch and
one draft PR; no source PR is retargeted, rebased or merged in this work.
