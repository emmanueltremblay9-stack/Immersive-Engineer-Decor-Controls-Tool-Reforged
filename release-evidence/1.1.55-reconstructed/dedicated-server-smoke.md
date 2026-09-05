# Dedicated-server smoke

## Retained failed control-channel attempt

The first baseline `runServer` process loaded the exact 1.1.55 mod list and reached `Done (0.651s)`. PowerShell rejected the piped `stop` object before it could bind to `scripts/invoke-gradle.ps1`, so the specifically identified test JVM was stopped and Gradle returned exit status `1`. This proves startup only and is not counted as the passing server gate.

## Passing repeated attempt

The same baseline command was rerun in a PTY with an external Gradle init script that forwards `System.in` to `runServer`:

```powershell
.\scripts\invoke-gradle.ps1 runServer --stacktrace --init-script C:\AI-Work\iedct-qualification-20260904-205500\forward-server-stdin.init.gradle -ProjectProperties @{ include_jei_runtime='true'; neo_version='21.1.230'; jei_version='19.32.0.359' }
```

Observed mod list:

- IEDCT `1.1.55-reconstructed`
- Immersive Engineering `12.4.2-194`
- JEI `19.32.0.359`
- Minecraft `1.21.1`
- NeoForge `21.1.230`

Observed lifecycle:

- `Done (0.522s)!`
- `stop` accepted on standard input
- players and worlds saved
- all dimensions reported saved
- `BUILD SUCCESSFUL`
- `DEDICATED_SERVER_EXIT=0`

Verdict: `PASS` for a development dedicated-server smoke. This is distinct from a packaged-server install and from client visual runtime.
