# Muslee targeted waiting-command invalidation

Base: `f4e28b3e619254d40f74a7677bd9c94610e24c05` after PR #71.

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
a transaction by this class. The queue only releases its numerical reservation and
logical ownership. Actual captures must be released by their owner only when safe;
already-engaged work and its captures remain protected. No command is retried.

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
