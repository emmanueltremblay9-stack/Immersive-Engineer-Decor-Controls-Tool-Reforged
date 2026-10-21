# Muslee protocol exhaustive boundary regressions

Test-only follow-up to merged PR #69. No production source changes, no GUI or
network activation. These cases use the actual MusleeCommandProtocol and
MusleeSessionIngress classes at base 5b4c05de6d17ab19199bed0e942fb0cbae07c3cb.

The standalone Java 21 Cases entry point and JUnit 4 wrapper run the same assertions:
all 65536 protocol versions, lengths 0-8193, every type byte, both one-byte argument
domains, every UUID version nibble and variant byte, authorization-before-recognition,
malformed-command replay, safe history release, and 800 concurrent duplicate attempts.

Unknown versions/oversize packets cannot invoke recognition. Malformed commands
with validated identities consume their UUID; invalid UUIDs do not. No received
byte array is mutated. Exactly one concurrent copy can be recognized. Thread waits
are bounded; worker failures propagate and the executor is shut down in finally.

No packets are sent to Minecraft and no GUI/session ownership adapter is tested.
Local Java execution is not NeoForge runtime qualification. Existing exact-head CI
must compile and execute the JUnit wrapper, GameTests and normal regressions.
All 368 decisions remain unchanged. No workflow, dependency, permission, release,
merge or installation change. Root AGENTS.md applies.
