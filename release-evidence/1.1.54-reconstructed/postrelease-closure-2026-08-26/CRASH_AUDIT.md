# Client loopback crash audit

## Historical corpus

The read-only Prism LAB `crash-reports` directory contains 14 reports with the exact text `Unable to establish loopback connection`.

| Target version loaded | Reports |
|---|---:|
| `1.1.46-reconstructed` | 5 |
| `1.1.48-reconstructed` | 1 |
| `1.1.53-reconstructed` | 4 |
| `1.1.54-reconstructed` | 4 |

The same signature occurred under Oracle GraalVM 21.0.7 (11 reports) and Eclipse Temurin 21.0.11 (3 reports). No report contains a target-mod stack frame.

## Exact failing path

Current controlled treatment crashes resolve to this chain:

```text
Netty NioEventLoop
-> WEPollSelectorProvider.openSelector
-> WEPollSelectorImpl
-> PipeImpl(AF_UNIX preferred)
-> UnixDomainSockets.connect
-> java.net.SocketException: Invalid argument: connect
-> java.io.IOException: Unable to establish loopback connection
```

Representative controlled crash artifact:

- External file: `prism\runs\IEDCT-U-T-R2-826\crash-2026-08-26_22.28.41-client.txt`
- Size: 58,115 bytes
- SHA-256: `BFBE8637F45FF22E04BB5FCA7BF6BA0E5FEB951E63559776EB7CFB58F47CE500`
- Relevant lines: `PipeImpl.java:103`, `PipeImpl.java:204`, `WEPollSelectorImpl.java:79`, `UnixDomainSockets.java:154`, and native `connect0`.

## Target-mod static scope

- Targeted source import search found zero `java.net`, `java.nio.channels`, or `java.util.concurrent` imports in `src/main/java`.
- `jdeps --ignore-missing-deps --multi-release 21 -verbose:class` on the exact public 1.1.54 JAR exited 0, emitted 2,973 lines, and emitted zero dependencies on `java.net.*`, `java.nio.channels.*`, or `sun.nio.ch.*`.
- The non-persistent code graph contained 31,676 nodes and 38,341 edges. Targeted network/selector/pipe/executor searches found no direct target-source hook; the only Netty import found was test-only.

Static evidence is not used alone as proof of non-causality. The controlled A/B matrix in `PRISM_AB_RUNS.md` supplies the differential runtime evidence.
