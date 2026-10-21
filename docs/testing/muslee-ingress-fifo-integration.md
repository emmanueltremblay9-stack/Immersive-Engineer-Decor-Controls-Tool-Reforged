# Muslee ingress/FIFO composition regressions

Test-only follow-up to merged protocol/session PR #69 and FIFO PR #71, pinned to
`f4e28b3e619254d40f74a7677bd9c94610e24c05`. No production files are changed.

The fixture serializes calls to the actual MusleeSessionIngress and MusleeCommandQueue
under one explicit lock. It is NOT the future production sender/session/press adapter.
It deliberately separates UUID recognition from FIFO admission and queue ownership
from actual capture memory. Boolean permission and numerical capture sizes are
fixture inputs, not evidence of live authorization or retained-heap sizing.

Eight scenarios verify:

- A full queue refuses admission without making the recognized UUID reusable.
- A memory-budget/invalid-bound refusal leaves no ticket or partial reservation.
- Invalid envelopes/UUIDs cannot reserve memory; malformed recognized commands consume history only.
- Exact 30-second expiry releases a queue lease without erasing session deduplication.
- The 4096th recognized ID triggers invalidation; explicit owner cleanup preserves active work.
- Eight-entry cleanup defers remaining work; replay cannot trigger a second admission cleanup.
- Clock failure invalidates waiting work, not an engaged lease, and restoration does not replay.
- The fixture revalidates current access at engagement instead of trusting prior recognition.

TOOLS-290-308, 348-368 and existing FIFO/session constraints remain unchanged.
New manual actions use new UUIDs. No GUI is opened automatically. Closed sessions
remain terminal, and only verified transaction finalization resumes queued work.

Run the standalone Java 21 Cases main or its JUnit 4 wrapper; they execute identical
assertions against exact production classes, without Minecraft/API stubs. Exact-head
Gradle/NeoForge CI remains required. This is in-process composition coverage, not
network delivery, GUI synchronization, a production transaction, complete mutation
tracking, ItemStack immutability or disk/journal qualification. No workflow, dependency,
permission, persistence, repair, release or merge changes. Root AGENTS.md applies.
