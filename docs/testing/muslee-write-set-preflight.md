# Muslee write-set preflight — necessary check, never permission to commit

## Exact source and scope

This proposed inactive production helper and its JUnit 4 regression suite are based on
IMPL `main` `0b9a7d67a82cab8afcf8d0be5a29771026b2fb16` (merged PR #82).
The actual `MusleeRevisionFence.java` at that commit has Git blob
`cc2c7e07d20c1a7dede8d88f104b716dddeb032d` and is not modified here.

`MusleeWriteSetGate.preflightOnly` checks a caller-supplied, *preenumerated* set of
independent revision fences using `preflightForCommit(finalCanonicalBytes)`.
The helper snapshots the proposed byte arrays before caller mutation, refuses
missing/duplicate locations before touching any fence, and refuses a failing or
uncertain preflight. It cannot alter authoritative inventories or their revision
counters, and a successful `PREFLIGHT_ONLY` result is explicitly **not** a commit
permission. The prior PR #84's *read-set* comparison is independent of this new
*write-set* preflight; the two primitives do not depend on each other's branches.

Defensive development limits: no more than 40 unique candidate locations per
invocation, and no individual byte array larger than 262144 bytes may be copied
into a proposed snapshot. These are conservative fail-closed input limits, **not**
a proof of a complete Minecraft write set, and **not** a Java heap-memory budget.
The caller must supply the complete authoritative mapping; an omitted location
cannot be detected by this helper.

## No transaction/activation rights

To use such a helper in future runtime code, the host must first independently:

- prove complete canonical `ItemStack` representations, including nested components,
  current values and presence/absence;
- obtain all source, destination and reserve read/write dependencies, enforce the
  existing 36-slot and reserve contracts without inventing additional destinations;
- hold the *one existing authoritative server transaction lock* from current-state
  sampling through every read-set verification, every write-fence preflight, all
  actual writes and one `recordTransaction` for every changed location;
- preserve session/press identity, FIFO order, exact expiry, replay protection,
  targeted invalidation, safe capture ownership and journal authority;
- prove cumulative **262144-byte retained/peak-memory** bounds for pending captures
  before allowing actual `MusleeCommandQueue.admitCapture` (PR #82 deliberately
  refuses it until then);
- stop on any missing/unknown input, failed preflight, stale revision or
  unqualified environment. Do not retry recognized commands automatically.

A positive result from this API has no executable effect. Later invalidations
cannot be reversed or old captures revived. When one fence fails with unprovable
proposed bytes or revision overflow, others are left untouched by this helper;
no inventory write is attempted or rolled back. The caller must not cache an
old successful check across release of the authoritative lock.

## Validation and acceptance

The exact `MusleeRevisionFence.java`, new `MusleeWriteSetGate.java` and new
`MusleeWriteSetGateCases.java` were compiled locally with the available Java 21
compiler using `javac --release 21 -Xlint:all -Werror`. The executable assertions
cover 12000 seeded three-location scenarios and targeted cases for all-valid/no
mutation, missing and duplicate inputs, detached proposals, bounded input refusal,
independent-fence failure, and `Long.MAX_VALUE` revision preflight.

These tests use *synthetic canonical bytes* against actual revision-fence code.
They do **not** demonstrate real Minecraft inventory updates, JVM memory-profile
qualification, nested component mutation tracking, a server lock, rollback or
actual GUI/network behavior. A companion JUnit 4 test invokes the same cases.

Require exact-head GitHub CI to compile and run JUnit, existing NeoForge GameTests,
client regressions and build before review. Local Java success is not a NeoForge
PASS. No changes to existing gameplay files, imports/dependencies, Gradle/workflows,
permissions, releases or branch-protection rules. No merge or publication in this
PR. Root `AGENTS.md` applies. Preserve TOOLS-001–368, including TOOLS-137 A.
