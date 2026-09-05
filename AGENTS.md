# Local execution safeguards

- On Windows, if Gradle reports `Unable to establish loopback connection` from `PipeImpl` or `UnixDomainSockets`, do not repeat the wrapper command unchanged. Use `scripts/invoke-gradle.ps1`; it assigns a verified non-synchronized Java temporary directory before invoking the checked-in wrapper.
- Keep the temporary-directory workaround process-local. Do not commit user-specific `TEMP`, `TMP`, `JAVA_HOME`, or Gradle cache paths to project configuration.
- Pass Gradle `-Pname=value` overrides through `scripts/invoke-gradle.ps1 -ProjectProperties @{ name = 'value' }`. Direct dotted `-P` tokens can be split by PowerShell parameter binding and misread as Gradle task names.
