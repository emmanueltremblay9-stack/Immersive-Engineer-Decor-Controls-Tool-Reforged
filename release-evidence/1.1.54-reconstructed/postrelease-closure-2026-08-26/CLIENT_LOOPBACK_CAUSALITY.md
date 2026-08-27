# Client loopback causality decision

## Decision

`SHARED_ENVIRONMENT_FAILURE`

Observed mechanism: `HOST_TEMP_DIRECTORY_AF_UNIX_CONNECT_FAILURE`.

## Basis

1. The historical crash signature spans four target-mod versions and two Java vendors.
2. The current exact selector failure reproduces in a standalone Java probe, outside Minecraft and outside the target mod.
3. Explicit IPv4 loopback TCP and the ordinary TCP-backed `Pipe.open()` pass while the Windows WEPoll selector's AF_UNIX wakeup pipe fails.
4. Direct AF_UNIX bind/connect fails under the current `%TEMP%` location and succeeds under `C:\AI-Work`.
5. In the controlled Minecraft matrix, no target, 1.1.53, and 1.1.54 all fail unmitigated and all enter the neutral world with the same process-local socket-directory override.
6. Target source/JAR analysis found no direct dependency on the failing Java networking path and no target frame appears in the crash chain.

## Consequences

- The evidence does not satisfy `TARGET_MOD_CAUSAL`; no target-code fix or 1.1.55 version bump is permitted.
- A per-process JVM workaround is technically supported, but production-wide deployment is not part of this mission.
- The root reason that Windows rejects AF_UNIX connects at this specific temp-directory location remains `UNVERIFIED`.
- Global network and system changes: none.
