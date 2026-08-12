# Loopback diagnostic

Two Prism/Minecraft failures reported `Unable to establish loopback connection`, `WEPollSelectorImpl`, and `SocketException: Invalid argument: connect`. The local crash report SHA-256 is `41789BC67B5BB76E49C316F0B353CD7C2A48A3279ED6AFEC5537589AD008059A`; the downloaded `KPZt3HP` log SHA-256 is `6019C980225F8B1B15B626BAD0E869B25281B2784E34C3CCDC4051282CEB88D0`.

Direct findings:

- neither stack contains a class from the target mod;
- the first loopback failure also occurs in NeoForge's independent version-check thread;
- JaCoCo, OpenTelemetry, and unsup components are disabled, and the launch has no `-javaagent` argument;
- the target mod had registered its content before the failure.

A minimal Java 21 probe opened a selector, bound a `ServerSocketChannel` to `127.0.0.1`, connected a `SocketChannel`, accepted the connection, and exchanged bytes. It passed with:

- the Prism GraalVM 21 runtime;
- the Gradle Temurin 21.0.11 runtime;
- the system Temurin 21.0.11 runtime;
- the Prism GraalVM runtime with the exact configured JVM arguments.

Probe results SHA-256: `5557DFA678F775B13A0424CA282E3D5D69731427ECAC38DFEF1FBA0CD39630D6`.

The later packaged NeoForge server also bound loopback, reached `Done`, accepted an authenticated loopback RCON command, and stopped cleanly. These controls show no persistent Java/Windows loopback failure, but they cannot identify the cause of the earlier incidents.

`LOOPBACK_ROOT_CAUSE: UNRESOLVED_TRANSIENT`

`TARGET_MOD_CAUSATION: NOT_OBSERVED; SUPPORTED-INDEPENDENCE, NOT PROVEN`
