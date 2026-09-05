# GameTest runtime matrix

All commands used the checked-in wrapper through `scripts/invoke-gradle.ps1`, with the process-local Java temporary-directory safeguard.

## Lane A — required minimum without JEI

```powershell
.\scripts\invoke-gradle.ps1 runGameTestServer --stacktrace -ProjectProperties @{ include_jei_runtime='false'; neo_version='21.1.230' }
```

Observed mod list: IEDCT `1.1.55-reconstructed`, Immersive Engineering `12.4.2-194`, Minecraft `1.21.1`, NeoForge `21.1.230`; JEI absent.

Result: `All 188 required tests passed`; Gradle exit status `0`.

## Lane B — declared baseline

```powershell
.\scripts\invoke-gradle.ps1 runGameTestServer --stacktrace -ProjectProperties @{ include_jei_runtime='true'; neo_version='21.1.230'; jei_version='19.32.0.359' }
```

Observed mod list: IEDCT `1.1.55-reconstructed`, Immersive Engineering `12.4.2-194`, JEI `19.32.0.359`, Minecraft `1.21.1`, NeoForge `21.1.230`.

Result: `All 188 required tests passed`; Gradle exit status `0`.

## Lane C — reporter versions

```powershell
.\scripts\invoke-gradle.ps1 runGameTestServer --stacktrace -ProjectProperties @{ include_jei_runtime='true'; neo_version='21.1.248'; jei_version='19.44.0.406' }
```

Observed mod list: IEDCT `1.1.55-reconstructed`, Immersive Engineering `12.4.2-194`, JEI `19.44.0.406`, Minecraft `1.21.1`, NeoForge `21.1.248`.

Result: `All 188 required tests passed`; Gradle exit status `0`.

No declared minimum version was raised.
