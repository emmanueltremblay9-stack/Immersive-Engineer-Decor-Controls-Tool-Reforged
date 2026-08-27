# Prism preservation and cleanup

## Original LAB instance postflight

- Instance: `1.21.1 TesT LaB`
- `instance.cfg` SHA-256 before/after: `E7DD4C3CFA0F8C156A656BD62290317D35983670D2F96FE4F8889430E9DC17A5`
- `mmc-pack.json` SHA-256 before/after: `CE56C32326FB72473FDF5E301E375B5E1D4DD992A95DCB385232183D470EF2E7`
- Java path: `C:/Users/Emmanuel Tremblay/AppData/Local/Programs/Java/graalvm-21/bin/javaw.exe`
- Memory after mission: minimum 2,048 MiB, maximum 6,144 MiB
- Active JAR count after mission: 51
- Matching target JAR count after mission: 1
- Target SHA-256 after mission: `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`

The original LAB configuration and installed target artifact match preflight exactly.

At the final 2026-08-27 03:00 EDT closure recheck, both configuration hashes still matched, the active mods tree contained 51 JARs and exactly one target JAR with the recorded SHA-256, the four-instance name-set hash still matched, no mission-prefixed instance remained, and no Java, JavaW, or Prism Launcher process was running. The read-only check exited 0 with `PRISM_PRESERVATION_RECHECK=PASS`.

## Instance-list restoration

Before mission temporary instances:

- Instance count: 4
- Sorted names SHA-256 (UTF-8, LF-separated, no trailing LF): `351FD5BCD0FAF2F0C702F30510B1BA0CD753A2CBD42B59C04E93F118F1E2AD51`

After cleanup:

- Instance count: 4
- Names: `1.21.1`, `1.21.1 TesT LaB`, `1.21.1 TesT play`, `26.2`
- Sorted names SHA-256: `351FD5BCD0FAF2F0C702F30510B1BA0CD753A2CBD42B59C04E93F118F1E2AD51`
- Mission-prefixed instances remaining in Prism: 0
- Java/Prism processes remaining: 0

The filesystem safety layer rejected permanent recursive deletion before execution. The 15 exact mission-owned directories were therefore moved recoverably to:

```text
C:\AI-Work\iedct-postrelease-closure-20260826-215006-external\prism\removed-instances
```

They are no longer registered under or present in Prism's `instances` directory. No original instance was moved or deleted.
