# Java, JVM, and host manifest

## Host

- OS: Microsoft Windows 11 Home 10.0.26200, build 26200
- Architecture: amd64
- Process `TEMP` / `TMP`: `C:\Users\EMMANU~1\AppData\Local\Temp`
- IPv4 dynamic TCP range: start 49152, count 16384
- IPv6 dynamic TCP range: start 49152, count 16384
- Java agents active in the Prism profile: none

No registry, firewall, Winsock, TCP range, Java installation, account, or system environment setting was changed.

## Java executables tested

| Runtime | Path | SHA-256 |
|---|---|---|
| Oracle GraalVM 21.0.7+8.1 | `C:\Users\Emmanuel Tremblay\AppData\Local\Programs\Java\graalvm-21\bin\java.exe` | `1C0E41ADA2ECA1AD2E98F7D61CE7BCDE38FDA306422D5FD33CB635A416E659B5` |
| Eclipse Temurin 21.0.11+10 (user) | `C:\Users\Emmanuel Tremblay\AppData\Local\Programs\Java\temurin-21\bin\java.exe` | `5E0FAB9F07952CEB6E71EB9FD33E1ED69959904CA00CF70869B7BAF516A98016` |
| Eclipse Temurin 21.0.11+10 (system) | `C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin\java.exe` | `5E0FAB9F07952CEB6E71EB9FD33E1ED69959904CA00CF70869B7BAF516A98016` |

All three selected `sun.nio.ch.WEPollSelectorProvider` and reproduced the unmitigated selector failure. The two Temurin installations contain the same executable bytes.

## Local JDK source evidence

- GraalVM `lib\src.zip` SHA-256: `51C38E3C9951F96359EC3863911E4882B9FC05ACB0CE568FD3866E25571E5B31`
- `WEPollSelectorImpl` creates `PipeImpl(sp, true, false)`, preferring AF_UNIX for wakeup support.
- `PipeImpl` uses an automatically bound UNIX-domain listener when AF_UNIX is supported and preferred.
- `UnixDomainSocketsUtil.getTempDir()` on Windows resolves in this order: `jdk.net.unixdomain.tmpdir` system/net property, `%TEMP%`, then `java.io.tmpdir`.

This source ordering explains why changing only `java.io.tmpdir` did not bypass the current `%TEMP%` directory, while the process-local `jdk.net.unixdomain.tmpdir` property did.
