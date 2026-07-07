param(
   [string]$ModsDir,
   [string]$TargetJarName,
   [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

function Read-Properties([string]$Path) {
   $result = @{}
   foreach ($line in Get-Content -LiteralPath $Path) {
      $trimmed = $line.Trim()
      if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
         continue
      }

      $parts = $trimmed -split "=", 2
      if ($parts.Count -eq 2) {
         $result[$parts[0].Trim()] = $parts[1].Trim()
      }
   }

   return $result
}

function Read-LocalEnvValue([string]$Path, [string]$Name) {
   if (-not (Test-Path -LiteralPath $Path)) {
      return $null
   }

   foreach ($line in Get-Content -LiteralPath $Path) {
      $trimmed = $line.Trim()
      if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
         continue
      }

      $parts = $trimmed -split "=", 2
      if ($parts.Count -eq 2 -and $parts[0].Trim() -eq $Name) {
         return $parts[1].Trim().Trim('"').Trim("'")
      }
   }

   return $null
}

function Read-ZipEntryText([string]$ZipPath, [string]$EntryName) {
   Add-Type -AssemblyName System.IO.Compression.FileSystem
   $zip = [System.IO.Compression.ZipFile]::OpenRead($ZipPath)
   try {
      $entry = $zip.Entries | Where-Object { $_.FullName -eq $EntryName } | Select-Object -First 1
      if ($null -eq $entry) {
         return $null
      }

      $reader = New-Object System.IO.StreamReader($entry.Open())
      try {
         return $reader.ReadToEnd()
      } finally {
         $reader.Dispose()
      }
   } finally {
      $zip.Dispose()
   }
}

function Read-ModMetadata([string]$JarPath) {
   $toml = Read-ZipEntryText $JarPath "META-INF/neoforge.mods.toml"
   if ($null -eq $toml) {
      $toml = Read-ZipEntryText $JarPath "META-INF/mods.toml"
   }

   if ($null -eq $toml) {
      return $null
   }

   $modsBlock = [regex]::Match($toml, '(?ms)^\s*\[\[mods\]\]\s*(.*?)(?=^\s*\[\[|^\s*\[|\z)')
   $metadataText = if ($modsBlock.Success) { $modsBlock.Groups[1].Value } else { $toml }
   $modId = [regex]::Match($metadataText, '(?m)^\s*modId\s*=\s*"([^"]+)"')
   $version = [regex]::Match($metadataText, '(?m)^\s*version\s*=\s*"([^"]+)"')
   $displayName = [regex]::Match($metadataText, '(?m)^\s*displayName\s*=\s*"([^"]+)"')
   [pscustomobject]@{
      ModId = if ($modId.Success) { $modId.Groups[1].Value } else { $null }
      Version = if ($version.Success) { $version.Groups[1].Value } else { $null }
      DisplayName = if ($displayName.Success) { $displayName.Groups[1].Value } else { $null }
      Toml = $toml
   }
}

function Assert-WithinDirectory([string]$Parent, [string]$Child) {
   $parentFull = [System.IO.Path]::GetFullPath($Parent).TrimEnd('\') + '\'
   $childFull = [System.IO.Path]::GetFullPath($Child)
   if (-not $childFull.StartsWith($parentFull, [System.StringComparison]::OrdinalIgnoreCase)) {
      throw "Refusing to modify path outside target mods directory: $childFull"
   }
}

Set-Location -LiteralPath $ProjectRoot
$properties = Read-Properties (Join-Path $ProjectRoot "gradle.properties")
$modId = $properties["mod_id"]
$modName = $properties["mod_name"]
$modVersion = $properties["mod_version"]

if (-not $modId -or -not $modVersion) {
   throw "Could not read mod_id and mod_version from gradle.properties."
}

$tomlPath = Join-Path $ProjectRoot "src\main\resources\META-INF\neoforge.mods.toml"
$toml = Get-Content -LiteralPath $tomlPath -Raw
$tomlModId = [regex]::Match($toml, '(?m)^\s*modId\s*=\s*"([^"]+)"').Groups[1].Value
$tomlVersion = [regex]::Match($toml, '(?m)^\s*version\s*=\s*"([^"]+)"').Groups[1].Value
if ($tomlModId -ne $modId -or $tomlVersion -ne $modVersion) {
   throw "Version metadata is not synchronized. gradle.properties=$modId/$modVersion, neoforge.mods.toml=$tomlModId/$tomlVersion"
}

if (-not $ModsDir) {
   $ModsDir = $env:CODEX_MINECRAFT_MODS_DIR
}

if (-not $ModsDir) {
   $ModsDir = Read-LocalEnvValue (Join-Path $ProjectRoot ".codex\local.env") "CODEX_MINECRAFT_MODS_DIR"
}

if (-not $ModsDir) {
   throw "Target mods directory is required. Pass -ModsDir or set CODEX_MINECRAFT_MODS_DIR."
}

$resolvedModsDir = [System.IO.Path]::GetFullPath($ModsDir)
if (-not (Test-Path -LiteralPath $resolvedModsDir -PathType Container)) {
   throw "Target mods directory does not exist: $resolvedModsDir"
}

if (-not $SkipBuild) {
   & (Join-Path $ProjectRoot "gradlew.bat") clean build
   if ($LASTEXITCODE -ne 0) {
      throw "Gradle clean build failed with exit code $LASTEXITCODE"
   }
}

$excludedJarPattern = '(?i)(sources|javadoc|dev|plain|test|api)\.jar$'
$runtimeJars = Get-ChildItem -LiteralPath (Join-Path $ProjectRoot "build\libs") -File -Filter "*.jar" |
   Where-Object { $_.Name -notmatch $excludedJarPattern }

$matchingRuntimeJars = @()
foreach ($jar in $runtimeJars) {
   $metadata = Read-ModMetadata $jar.FullName
   if ($metadata -and $metadata.ModId -eq $modId -and $metadata.Version -eq $modVersion) {
      $matchingRuntimeJars += $jar
   }
}

if ($matchingRuntimeJars.Count -ne 1) {
   throw "Expected exactly one runtime jar for $modId $modVersion, found $($matchingRuntimeJars.Count)."
}

$sourceJar = $matchingRuntimeJars[0]
$targetJarName = if ($TargetJarName) { $TargetJarName } else { $sourceJar.Name }
if ([System.IO.Path]::GetFileName($targetJarName) -ne $targetJarName) {
   throw "TargetJarName must be a file name, not a path: $targetJarName"
}

if (-not $targetJarName.EndsWith(".jar", [System.StringComparison]::OrdinalIgnoreCase)) {
   throw "TargetJarName must end with .jar: $targetJarName"
}

$oldJars = @()
foreach ($jar in Get-ChildItem -LiteralPath $resolvedModsDir -File -Filter "*.jar") {
   $metadata = Read-ModMetadata $jar.FullName
   if (($metadata -and $metadata.ModId -eq $modId) -or $jar.BaseName -like "$modId*" -or $jar.Name -ieq $targetJarName) {
      $oldJars += $jar
   }
}

foreach ($jar in $oldJars) {
   Assert-WithinDirectory $resolvedModsDir $jar.FullName
   Remove-Item -LiteralPath $jar.FullName
}

$targetJarPath = Join-Path $resolvedModsDir $targetJarName
Assert-WithinDirectory $resolvedModsDir $targetJarPath
Copy-Item -LiteralPath $sourceJar.FullName -Destination $targetJarPath

$sourceHash = (Get-FileHash -LiteralPath $sourceJar.FullName -Algorithm SHA256).Hash
$targetHash = (Get-FileHash -LiteralPath $targetJarPath -Algorithm SHA256).Hash
$sourceSize = (Get-Item -LiteralPath $sourceJar.FullName).Length
$targetSize = (Get-Item -LiteralPath $targetJarPath).Length

$remaining = @()
foreach ($jar in Get-ChildItem -LiteralPath $resolvedModsDir -File -Filter "*.jar") {
   $metadata = Read-ModMetadata $jar.FullName
   if (($metadata -and $metadata.ModId -eq $modId) -or $jar.BaseName -like "$modId*" -or $jar.Name -ieq $targetJarName) {
      $remaining += $jar
   }
}

$installedMetadata = Read-ModMetadata $targetJarPath
if ($sourceHash -ne $targetHash) {
   throw "Installed jar hash mismatch."
}

if ($remaining.Count -ne 1) {
   throw "Expected exactly one installed jar for $modId, found $($remaining.Count)."
}

if (-not $installedMetadata -or $installedMetadata.ModId -ne $modId -or $installedMetadata.Version -ne $modVersion) {
   throw "Installed jar metadata does not match $modId $modVersion."
}

$report = [pscustomobject]@{
   mod_id = $modId
   mod_name = $modName
   version = $modVersion
   loader = "NeoForge"
   source_jar = $sourceJar.FullName
   installed_jar = $targetJarPath
   deleted_old_jars = @($oldJars | ForEach-Object { $_.FullName })
   source_size = $sourceSize
   installed_size = $targetSize
   source_sha256 = $sourceHash
   installed_sha256 = $targetHash
   hash_match = $sourceHash -eq $targetHash
   remaining_matching_jars = $remaining.Count
   installed_metadata = [pscustomobject]@{
      mod_id = $installedMetadata.ModId
      version = $installedMetadata.Version
      display_name = $installedMetadata.DisplayName
   }
}

$reportPath = Join-Path $ProjectRoot "build\install-report.json"
$report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $reportPath -Encoding UTF8
$report | ConvertTo-Json -Depth 6
