# Muslee real-ItemStack admission safety regression

## Scope and exact baseline

Test-only supplement to merged PR #82 and the standalone queue/capture ownership tests.
Original behavior reference: IMPL `main` `1e17fe021dc5090904c1c95d3381828c7783cfe7`.
Current QA integration base: `main` `8854e26a4c06e8fb082fbd137f16fc54cf4408e7`.
The four tests use the actual production `MusleeItemStackCapture` and
`MusleeCommandQueue`; their Java source is unchanged from PR #87's previous head.
This proposal adds an engine GameTest class and this documentation only.
No existing source, mod behavior, CI workflow or dependency changes are requested.

## Why this test matters

The merged PR #82 deliberately refuses public `admitCapture` until TOOLS-351's
**complete retained/peak-heap bound** has been demonstrated for a qualified
server runtime. Neither the encoded bytes nor a caller-provided scalar budget
proves that bound. Regressions in admission could silently turn an incomplete
memory qualification into a live runtime behavior change. These tests exercise
that gate against actual Minecraft `ItemStack` captures, not just synthetic bytes.

Four server GameTests:

1. Representable bread/apple/wheat-seed captures are detached, but all are refused
   for runtime admission. The unowned capture is cleared once; source stacks,
   reservations and engagement remain unchanged.
2. A supported nested `CUSTOM_DATA` payload remains unchanged through refusal.
   An already released capture cannot restore an item.
3. Full *scalar-only* FIFO accounting cannot bypass the real-capture admission
   gate; neither can newly available queue space after explicit cleanup.
4. An existing queue-owned **real** capture from the package-private ownership
   fixture cannot be stolen by another queue's public admission call. The proper
   owner safely releases it once through explicit waiting-session invalidation.

## Limits and acceptance

The package-private `admitCaptureForOwnershipTest` is used **only** for the final
engine lifecycle fixture; its numeric value is never portrayed as a valid
production memory-size argument. This test does NOT exercise real runtime
admission, GUI, packet handling, inventory transfers, pressing, repairs, native
JVM object-graph layout, full mutation origin tracking, or atomic transactions.

Acceptance requires the exact-commit repository CI to compile the new GameTests,
run JUnit and the real NeoForge dedicated GameTest server, pass client/renderer
regressions and build. A standalone source inspection is not a GameTest PASS.

PR #86's exact-size output-buffer encoder is already merged and included in this
integration base. The tests must still refuse capture admission: that encoder
does not qualify TOOLS-351's complete retained/peak-heap bound. Memory proof and
runtime activation remain separate requirements.
If the actual memory-proof policy is later qualified and explicitly adopted, this
safety test must be revised under a separately authorized requirement/acceptance
change, not silently disabled.

No merge, release, tag, deployment or permission changes are included. All
TOOLS-001–368 decisions, including TOOLS-137 option A, remain unchanged.
Follow the current root `AGENTS.md` for any build invocation.
