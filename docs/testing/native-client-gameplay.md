# Native client mining regression fixture

Run on a Linux CI host with the existing Java 21, Gradle-wrapper dependencies,
Xvfb and xauth prerequisites:

```sh
python tools/validation/client_gameplay.py --prepare-timeout 600 --timeout 420
```

The runner creates a fresh `build/client-gameplay-*/run` directory, writes a
random isolation marker, and starts the normal NeoForge client with the fixture
explicitly enabled. The fixture refuses an existing world or nonempty saves
directory, creates its own uniquely named flat integrated world, and stops the
client after a terminal result. No ordinary client session is affected when the
system property is absent. Test files remain under this isolated evidence
directory for diagnosis; no user save, options, resource pack or server is used.

The ten required cases cover creative and survival mining from below and each
horizontal direction. Each case:

1. Uses the connected real LocalPlayer, ClientLevel and integrated ServerPlayer,
   and verifies the two distinct players have equal network IDs, equality and
   hashes, reproducing the shared-JVM intent-map collision that dedicated-server
   tests alone cannot exercise
2. Establishes a real previous client/server pose selecting the catwalk deck,
   then turns to its inset lamp in the same tick as the native START call
3. Calls the normal NeoForge click-input hook before native
   MultiPlayerGameMode.startDestroyBlock/continueDestroyBlock
4. Requires every survival HOLD to retain its client-side component intent and
   native mining state despite equal-ID server START processing; records bounded
   START/ABORT/STOP counters and native destroy stages for failure diagnosis
5. Requires the lamp to be lit immediately before the native attack call, then
   checks its synchronous lit-to-unlit prediction and exactly one client lamp
   completion event inside that call; a later server update cannot qualify
6. Freezes the exact completion action's original vanilla sequence (START in
   creative, STOP in survival), then waits until its native ACK handling has run
   on the client thread; an earlier survival START ACK cannot qualify completion
7. Allows a temporary lit rollback after a legitimate early-STOP ACK while
   preserving the same catwalk and held-input latch, and requires eventual
   authoritative and reconciled-client lamp-only completion within the deadline
8. Holds attack beyond a full mining duration and verifies every input is
   canceled, with no further START/STOP packets and no catwalk loss
9. Checks authoritative server state, paid lamp loot and native tool wear
10. Releases input, then proves a fresh attack can actually mine the retained
   deck through prediction, normal packets, ACK and authoritative server state

The packet observer is passive: packets are neither fabricated nor changed.
Fixture setup changes only its own server world and player. A stone platform
below the player catches legitimate gravity-affected loot for exact counting.

A non-pausing screen blocks incidental OS input while the fixture invokes the
same input gate and game mode methods used by Minecraft's attack handling.
NeoForge's native attack binding uses the IN_GAME conflict context, so merely
calling `setDown(true)` with any screen open still makes `isDown()` return false.
The fixture first verifies this contract at runtime, then temporarily changes
only its isolated attack mapping to UNIVERSAL, checks the active held binding on
every attack, and restores its original context on exit. Production input and
its release/retarget reset conditions are unchanged. Failures record the latch's
last reset reason and the client completion-event count for diagnosis.
Therefore this is initialized native client/server gameplay and reconciliation
coverage, **not** a physical mouse/keyboard, visual-rendering, or native
LocalPlayer auto-jump movement test. Auto-jump is enabled in the fixture options
but movement is not qualified by this suite. It also does not yet qualify
server-denied lamp reconciliation or a release/repress between two client ticks.

A title screen, class loading, timeout, missing case, stale nonce, missing ACK,
extra mining packet, fatal diagnostic or nonzero client exit is a failure.
`client-gameplay-result.json` and logs are written in the fresh evidence
folder; each completed case is persisted before proceeding. The separate
`test_client_gameplay.py` unit tests validate these evidence gates. Passing
those Python tests alone is not evidence that Minecraft gameplay passed.
