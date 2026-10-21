# Muslee targeted waiting-command invalidation

Original base: `f4e28b3e619254d40f74a7677bd9c94610e24c05` after PR #71.
Composition reviewed against `0b9a7d67a82cab8afcf8d0be5a29771026b2fb16`,
including the concrete capture ownership introduced by PR #82.

The queue already supports whole-session invalidation. `invalidateWaiting(ticket)`
adds the smaller primitive needed to invalidate commands dependent on a particular
source slot, reserve compartment or output location without purging independent
commands from the same session (TOOLS-315/317, 323/324 and 339/340).

Only an actual waiting ticket owned by this queue is removed. Removal, terminal
state and reservation retirement occur under the queue monitor, exactly once.
Null, foreign, engaged and already-terminal references do nothing. Remaining
commands retain relative FIFO order. The scan is bounded by the existing 32 waiting
tickets. Mandatory invalidation is not ordinary eight-examination expiry cleanup.
Reentrant mutation during a validation pass remains prohibited.

The host MUST identify the complete dependency set and hold its common authoritative
serialization lock across all affected queue/fence invalidations and safe capture
releases. One call is atomic; separate calls across queues/fences are not made into
a transaction by this class. Retirement calls the queue's existing release path:
when a ticket owns a concrete MusleeItemStackCapture, its private byte buffer is
cleared and released exactly once, and the ticket drops its capture/owner references.
Scalar-only tickets have no concrete capture to release. Separately owned revision-
fence captures and other external resources remain the responsibility of their
actual owner; the adapter must not double-release a queue-owned capture. Already-
engaged work and its captures remain protected. No command is retried.

Tests use the actual queue and revision-fence classes, real detached byte captures,
and an explicitly controlled clock. They cover all 32 removal positions, same-session
survivors, foreign/terminal references, blocked reentry, safe reset after engaged
capture release, mandatory 32-ticket invalidation under a common fixture lock,
800 concurrent duplicate removals and 128 invalidation/engagement races. Futures
and shutdown waits are bounded and worker failures propagate.

The standalone Cases main and JUnit 4 wrapper run identical cases. Local Java 21
execution is not NeoForge, ItemStack immutability, exhaustive live mutation tracking,
real retained-heap accounting or GUI/transaction qualification. Existing exact-head
CI must compile and run JUnit, GameTests, build and client regressions. No activation,
recipe, network registration, persistence, workflow, dependency or release change.
All 368 decisions remain unchanged; TOOLS-137 A stays approved. Root AGENTS.md applies.

## Concrete ownership composition regression

MusleeTargetedCaptureOwnershipTest exercises the PR #82 package-private ownership
fixture: targeted waiting invalidation releases actual encoded storage exactly once,
preserves the engaged capture and same-session FIFO survivors, and cannot affect
another queue's capture. It also checks immediate budget reuse and terminal cleanup.
The opaque fixture bytes are not an ItemStack serialization test or a retained-heap
proof. Production admitCapture remains unconditionally refused until qualified
memory bounds exist. No GUI or runtime capture admission is enabled.
