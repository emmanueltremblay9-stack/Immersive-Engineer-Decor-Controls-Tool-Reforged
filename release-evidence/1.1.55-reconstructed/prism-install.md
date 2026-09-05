# Prism LAB installation

Target: `C:\Users\Emmanuel Tremblay\AppData\Roaming\PrismLauncher\instances\1.21.1 TesT LaB\minecraft\mods`

The target was resolved from the authorized NeoForge 1.21.1 LAB default. A read-only preflight found exactly one matching installed mod:

- `immersive_engineer_decor_controls_tool_reforged-1.1.54-reconstructed.jar`
- 2,678,440 bytes
- SHA-256 `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`
- embedded mod id `immersive_engineer_decor_controls_tool_reforged`
- embedded version `1.1.54-reconstructed`

Installation command:

```powershell
.\install-mod.ps1 -ModsDir 'C:\Users\Emmanuel Tremblay\AppData\Roaming\PrismLauncher\instances\1.21.1 TesT LaB\minecraft\mods' -SkipBuild
```

The script produced `build/install-report.json` without a terminating error and recorded exactly the identified 1.1.54 JAR in `deleted_old_jars`. PowerShell scripts do not populate `$LASTEXITCODE`, so the wrapper command's blank `$LASTEXITCODE` is not reported as a numeric script status. A separate read-only verifier returned exit status 0.

Independent readback:

- built JAR: `C:\AI-Work\iedct-issues-8-9-20260904-191718\build\libs\immersive_engineer_decor_controls_tool_reforged-1.1.55-reconstructed.jar`
- installed JAR: `C:\Users\Emmanuel Tremblay\AppData\Roaming\PrismLauncher\instances\1.21.1 TesT LaB\minecraft\mods\immersive_engineer_decor_controls_tool_reforged-1.1.55-reconstructed.jar`
- deleted old JAR: `immersive_engineer_decor_controls_tool_reforged-1.1.54-reconstructed.jar`
- source and installed size: 2,689,752 bytes
- source and installed SHA-256: `956FC45E04675427AB98A79BB82F22E28F55E7D13EBDCD54F86260515A63167C`
- hash match: `true`
- remaining matching JARs: `1`
- installed mod id/version: `immersive_engineer_decor_controls_tool_reforged` / `1.1.55-reconstructed`
- new light/window runtime classes and both corrected sign item resources: present

Gap: the attempted all-unrelated-JAR pre/post hash comparison exited before its post snapshot because of the `$LASTEXITCODE` handling described above. The installer source confines deletion to its metadata/name-matched list and its concrete report names only the old 1.1.54 JAR, but a complete before/after hash proof for every unrelated mod is `NOT_PERFORMED`.

Minecraft was not launched for this installation. Installation integrity does not constitute client visual-runtime proof.
