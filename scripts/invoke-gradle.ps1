[CmdletBinding(PositionalBinding = $false)]
param(
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$GradleArguments,

    [hashtable]$ProjectProperties = @{},

    [string]$JavaTempDirectory = 'C:\AI-Work\iedct-gradle-temp'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$wrapper = Join-Path $projectRoot 'gradlew.bat'
if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
    throw "Gradle wrapper not found: $wrapper"
}

New-Item -ItemType Directory -Path $JavaTempDirectory -Force | Out-Null
$resolvedJavaTemp = (Resolve-Path -LiteralPath $JavaTempDirectory).Path
$previousEnvironment = @{
    TEMP = $env:TEMP
    TMP = $env:TMP
    JAVA_TOOL_OPTIONS = $env:JAVA_TOOL_OPTIONS
}

$gradleExitCode = 1
try {
    $env:TEMP = $resolvedJavaTemp
    $env:TMP = $resolvedJavaTemp
    $javaTempOption = "-Djava.io.tmpdir=`"$resolvedJavaTemp`""
    $env:JAVA_TOOL_OPTIONS = if ([string]::IsNullOrWhiteSpace($previousEnvironment.JAVA_TOOL_OPTIONS)) {
        $javaTempOption
    } else {
        "$($previousEnvironment.JAVA_TOOL_OPTIONS) $javaTempOption"
    }

    $resolvedGradleArguments = @($GradleArguments)
    foreach ($propertyName in @($ProjectProperties.Keys | Sort-Object)) {
        if ($propertyName -notmatch '^[A-Za-z0-9_.-]+$') {
            throw "Invalid Gradle project-property name: $propertyName"
        }
        $propertyValue = $ProjectProperties[$propertyName]
        if ($null -eq $propertyValue) {
            throw "Gradle project property '$propertyName' has a null value"
        }
        $resolvedGradleArguments += "-P$propertyName=$propertyValue"
    }

    & $wrapper @resolvedGradleArguments
    $gradleExitCode = $LASTEXITCODE
} finally {
    foreach ($name in $previousEnvironment.Keys) {
        $value = $previousEnvironment[$name]
        if ($null -eq $value) {
            Remove-Item "Env:$name" -ErrorAction SilentlyContinue
        } else {
            Set-Item "Env:$name" $value
        }
    }
}

exit $gradleExitCode
