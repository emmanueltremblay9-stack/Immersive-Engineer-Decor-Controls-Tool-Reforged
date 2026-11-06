# Muslee protocol v2 exhaustive boundary regressions

The existing adversarial suite now targets the cursor-only production
MusleeCommandProtocol and MusleeSessionIngress classes. The protocol remains
dormant, without GUI or network activation. This supersedes the v1 argument
acceptance cases, not the safety or replay checks.

The standalone Java 21 Cases entry point and JUnit 4 wrapper share assertions:
all 65536 protocol versions, lengths 0–8193, every type byte, all 65536 declared
parameter lengths for ABSORB and PRESS, every one-byte legacy payload for types
1–3, every UUID version nibble and variant byte, authorization-before-recognition,
malformed-command replay, safe history release, and 800 concurrent duplicate attempts.
Only v2, types 1/3, zero parameter length and exactly 37 bytes are accepted.
Type 2 is permanently reserved for rejected legacy withdrawal.

Unknown versions (including v1) and oversize packets cannot invoke authorization
or recognition. Malformed commands with validated identities consume their UUID;
invalid UUIDs do not. A malformed legacy payload cannot be corrected and replayed
using the same action ID. No received byte array is mutated. Exactly one concurrent
copy can be recognized. Thread waits are bounded; worker failures propagate and
the executor is shut down in finally.

No packets are sent to Minecraft and no GUI/session ownership adapter is tested.
No local Java execution is claimed for this revision. Existing exact-head CI must
compile and execute the JUnit wrapper, GameTests and normal regressions. In-game
qualification is still required before activation. No workflow, dependency,
permission, release, merge or installation change. Root AGENTS.md applies.
