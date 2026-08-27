# Loopback and UNIX-domain socket probes

All probes were compiled and run with the exact Prism-selected GraalVM Java 21 executable. Additional unmitigated runs used both installed Temurin 21.0.11 executables. The probe source and class files are retained only in the external evidence root.

## WEPoll selector differential

| Profile | Selector | Standalone `Pipe.open()` | Explicit IPv4 TCP | Exit |
|---|---:|---:|---:|---:|
| GraalVM, current process environment, 200 cycles | 0/200 | 200/200 | 200/200 | 2 |
| Temurin user install, current process environment, 200 cycles | 0/200 | 200/200 | 200/200 | 2 |
| Temurin system install, current process environment, 200 cycles | 0/200 | 200/200 | 200/200 | 2 |
| GraalVM, `-Djava.io.tmpdir=C:\AI-Work\iedct-tmp-20260826`, 20 cycles | 0/20 | 20/20 | 20/20 | 2 |
| GraalVM, `-Djdk.net.unixdomain.tmpdir=C:\AI-Work\iedct-tmp-20260826`, 1,500 cycles | 1,500/1,500 | 1,500/1,500 | 1,500/1,500 | 0 |

The mitigated run used 500 serial cycles plus four workers with 250 cycles each. It had zero failures, did not time out, and left zero files in the alternate socket directory.

## Direct AF_UNIX path probe

Using GraalVM 21.0.7:

| Bind/connect mode | Path class | Result |
|---|---|---|
| Automatically bound AF_UNIX | `%TEMP%\socket_*` | `Invalid argument: connect` |
| Explicit AF_UNIX | `C:\AI-Work\iedct-uds-*.sock` | pass |
| Explicit AF_UNIX | `%TEMP%\iedct-uds-tmp-*.sock` | `Invalid argument: connect` |
| Explicit AF_UNIX | long-form current temp path | `Invalid argument: connect` |

The short 8.3 spelling and long spelling of the current temp location both fail. The shorter `C:\AI-Work` location passes. This isolates the observed mechanism to AF_UNIX connect behavior under the current temp-directory location; it does not establish why Windows rejects that location.

## Scope of mitigation

The only successful mitigation was a JVM process argument on temporary Prism controls:

```text
-Djdk.net.unixdomain.tmpdir=C:\AI-Work\iedct-tmp-20260826
```

It was not persisted to the original Prism instance, Java installation, user environment, system environment, registry, network stack, or firewall.
