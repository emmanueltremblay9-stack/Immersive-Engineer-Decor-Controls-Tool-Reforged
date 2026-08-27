# Controlled Prism A/B manifest

## Immutable sources

- Read-only source instance: `1.21.1 TesT LaB`
- Prism Launcher: 11.0.3; executable SHA-256 `C24C7C84FCE7FF1D12C709E0BCC8993AAA2A8CB662381C960CCB7D93C88BC2E3`
- Original active mods count: 51 JARs
- Original target JAR: 2,678,440 bytes; SHA-256 `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`
- Initial non-target arm manifest before temporary-memory normalization: 210 files, 445,555,658 bytes; SHA-256 `6B96112E4C23876DCCDE7C026782C43F0AED21C455B24AD92D6386A1BCA00F4C`
- Reusable target-free template after normalization and neutral-world insertion: 224 files, 449,404,984 bytes; SHA-256 `D777267EDA918FEE54697257AEEF683974B21A22D124CEACD0906D59911E8C3E`
- Harness: 16,458 bytes; SHA-256 `4612394461A1DB4AFDCE807BA608A5CF9FB24607EB4F3378DE6B7D8E3B2FC371`

The source instance was copied into mission-owned temporary instances. Runtime logs, caches, prior saves, screenshots, and prior crash reports were excluded from the controlled template.

The template arithmetic is exact: changing temporary `MaxMemAlloc` from `6144` to `3072` reduced `instance.cfg` from 4,375 to 4,374 bytes, so the normalized 210-file base is 445,555,657 bytes. Adding the 14-file, 3,849,327-byte neutral world gives 224 files and 449,404,984 bytes. The source instance itself retained `MaxMemAlloc=6144`.

## Neutral world

The neutral world was generated with the official Mojang 1.21.1 server JAR and then stopped through server stdin.

- Server JAR: 51,627,615 bytes
- Mojang SHA-1: `59353FB40C36D304F2035D51E7D6E6BAA98DC05C`
- SHA-256: `E3BC55693E93CDA0188F2E60AEA28113FC647C5E85A15FA3D1B347349231B4BB`
- Server log SHA-256: `BF23549B2C0F8E91058E15A33E41BE3C962408744EB32BB3EEEEB54A01B30A39`
- World snapshot: 14 files, 3,849,327 bytes
- Normalized world SHA-256: `394BD1F52D5598416EFAD600BBCBF1C7CAD26D028648A13794F75028C5A70EFB`
- `level.dat` SHA-256: `F9A7BA46A57BFAABF74E3EEB32FF501047FE175BE22E3D967B40D7411C2442B7`

Each run started from the same immutable world snapshot.

## Differential arms

| Arm | Target-mod state |
|---|---|
| `CONTROL-0` | target JAR absent |
| `CONTROL-1` | public GitHub 1.1.53 JAR, 2,670,951 bytes, SHA-256 `7F357843ACD1E8A9D85D03B979315E3E19058223EC6C10B156375479321BFE98` |
| `TREATMENT` | public GitHub 1.1.54 JAR, 2,678,440 bytes, SHA-256 `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29` |

The two JARs were downloaded from their public GitHub releases and matched the GitHub API asset size/digest metadata.

## Common launch controls

- Native Prism CLI route: `--launch <temporary-instance> --world neutral-world --offline <ephemeral-name>`
- No Computer Use, window automation, screenshots, OCR, OBS, Game Bar, audio, or video.
- Original JVM arguments were preserved in all arms.
- Temporary-arm maximum memory was uniformly reduced from 6,144 MiB to 3,072 MiB after Prism's free-memory guard rejected 6,144 MiB. Minimum memory remained 2,048 MiB. The original instance stayed at 6,144 MiB.
- Unmitigated JVM-profile SHA-256: `AE9266E8A139EFB36C7FA8753CBC1CD971AB5C75141A17EB79EF871B600CC32C`
- Mitigated JVM-profile SHA-256: `8EE604616F41CE5FE310F312A4035EFA3B8BE6032402C07CFE465B7DA78835CC`
- The only mitigated-profile addition was process-local `-Djdk.net.unixdomain.tmpdir=C:\AI-Work\iedct-tmp-20260826`.

One first `CONTROL-0` harness attempt (`IEDCT-U-C0-R1-826`) was interrupted after an evidence-file sharing error. It produced no verdict and is excluded. The corrected rerun is `IEDCT-U-C0-R1B-826`.
