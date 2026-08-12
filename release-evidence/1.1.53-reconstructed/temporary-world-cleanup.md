# Temporary-world cleanup — 1.1.53-reconstructed

## Result

`PASS`

Evidence finalized: `2026-08-12T10:43:56Z`

This save belonged to the separate Prism instance folder named `1.21.1`, not the validated `1.21.1 TesT LaB` instance used for the runtime JAR and interaction check. The cleanup mission explicitly targeted this potentially leftover non-LAB save.

Exact source save:

`C:\Users\Emmanuel Tremblay\AppData\Roaming\PrismLauncher\instances\1.21.1\minecraft\saves\IEDCT Mining QA 1_1_53`

Identity was established from the unique folder match, `level.dat` LevelName `IEDCT Mining QA 1.1.53`, creation/write timestamps, and the archived client log that recorded the controlled QA markers and world saves.

Backup created before removal:

`C:\AI-Work\iedct-world-backups\IEDCT-Mining-QA-1_1_53-20260812-054804.zip`

- backup size: `9,599,306` bytes
- backup SHA-256: `240962C79B81514FE58D4D2EF1A6E6203AFCAB31B0F2B9C73CB9ED95A32FD7C2`
- source file count: `26`
- source bytes: `17,500,358`
- ZIP entries: `29`

Only the exact QA save folder was removed. The other save in that instance remained unchanged. The archive remains external to Git and outside the Minecraft instance.
