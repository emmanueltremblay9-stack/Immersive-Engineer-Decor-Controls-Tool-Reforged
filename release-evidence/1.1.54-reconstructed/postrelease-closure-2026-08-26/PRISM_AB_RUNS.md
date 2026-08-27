# Controlled Prism A/B results

Each result required the native Prism `--world` argument to be accepted, the neutral-world SHA-256 to match, zero Java/Prism orphan processes after cleanup, zero suspicious secret files, and zero unredacted access-token lines.

## Unmitigated sequence

| Order | Run | Arm | Target | Marker | Primary evidence SHA-256 |
|---:|---|---|---|---|---|
| 1 | `IEDCT-CLOSURE-T-20260826` | TREATMENT | 1.1.54 | `LOOPBACK_SIGNATURE` | `9E7D061983D3EF4DEEC60D17F45B744CD7CE1D9F04E041BF93480F2619051615` |
| 2 | `IEDCT-U-C0-R1B-826` | CONTROL-0 | absent | `LOOPBACK_SIGNATURE` | `8A6D63AF3C082B15DF478B3B1D9D2AC673A74F740C4346D0B193908986617B4F` |
| 3 | `IEDCT-U-C1-R1-826` | CONTROL-1 | 1.1.53 | `LOOPBACK_SIGNATURE` | `4FDBC3DB508F2A5BD19D0E00008EEE2308D8D82807E1E5A82AAEE7612AEA69BA` |
| 4 | `IEDCT-U-C0-R2-826` | CONTROL-0 | absent | `LOOPBACK_SIGNATURE` | `9B4D1ABD25B90EEDDDA6C096F3CC828138E2A137B83457E64B0ECAF2888DF6D8` |
| 5 | `IEDCT-U-T-R2-826` | TREATMENT | 1.1.54 | `LOOPBACK_SIGNATURE` | `BFBE8637F45FF22E04BB5FCA7BF6BA0E5FEB951E63559776EB7CFB58F47CE500` |
| 6 | `IEDCT-U-C1-R2-826` | CONTROL-1 | 1.1.53 | `LOOPBACK_SIGNATURE` | `82805862D3EDE12295B7827DBC91F290BFF3B3CAA5D172F02990301CC7C594EE` |

All six runs failed with the exact same causal signature:

```text
java.io.IOException: Unable to establish loopback connection
<- java.net.SocketException: Invalid argument: connect
```

The failure reproduces with no target mod, with 1.1.53, and with 1.1.54.

## Process-local mitigated sequence

| Order | Run | Arm | Target | Marker | Stable interval | `latest.log` SHA-256 |
|---:|---|---|---|---|---:|---|
| 1 | `IEDCT-M-T-R1-826` | TREATMENT | 1.1.54 | `PLAYER_JOINED_WORLD` | 15 s | `ECD21244138E785D21F1DCD51ABAB43ED0A4773949B166942DA4F7DF7BBCC7EF` |
| 2 | `IEDCT-M-C0-R1-826` | CONTROL-0 | absent | `PLAYER_JOINED_WORLD` | 15 s | `D7CB36D54586288DD3A619D152D96D7778CB03B06A54FFABC4338B9AF617F771` |
| 3 | `IEDCT-M-C1-R1-826` | CONTROL-1 | 1.1.53 | `PLAYER_JOINED_WORLD` | 15 s | `BEB345F748E6A85F059C23ECBD5699D55F34AC5FCAAA5E992421A28C5050509A` |
| 4 | `IEDCT-M-C0-R2-826` | CONTROL-0 | absent | `PLAYER_JOINED_WORLD` | 15 s | `1287C58C6AA6138FE528AF3F4ADD96B2B2BB328152205EC67667C3514C33FD8A` |
| 5 | `IEDCT-M-T-R2-826` | TREATMENT | 1.1.54 | `PLAYER_JOINED_WORLD` | 15 s | `2076A6E4D4E840EFF5F3FBB78407B5702F72EBB83AAA2E742CF9CBD7E5D2026E` |
| 6 | `IEDCT-M-C1-R2-826` | CONTROL-1 | 1.1.53 | `PLAYER_JOINED_WORLD` | 15 s | `D055383AEAA2A2A7DBBC8FADBCF5FEB4294F892508365043AB4BBFCD63107A82` |

All six mitigated runs entered the world and remained stable for the observation interval. The harness then deliberately stopped the mission-owned processes; those forced post-success stops are not crash evidence.

## Differential verdict

- Target presence/version does not change the unmitigated outcome.
- The process-local AF_UNIX temp-directory change flips all three arms from the exact crash signature to stable world entry.
- `CLIENT_LOOPBACK_VERDICT: SHARED_ENVIRONMENT_FAILURE`
- `CLIENT_LOOPBACK_CAUSATION: HOST_TEMP_DIRECTORY_AF_UNIX_CONNECT_FAILURE`

The deeper reason Windows rejects AF_UNIX connect operations under the current temp-directory location remains unresolved. It is not necessary to assign causation to the target mod, and no 1.1.55 target-mod build is authorized by this result.
