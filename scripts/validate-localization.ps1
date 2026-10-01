[CmdletBinding()]
param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$ReportPath = 'build/reports/localization-validation.json',
    [int]$ExpectedLocaleCount = 120,
    [int]$ExpectedManualSourceCount = 223
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ModId = 'immersive_engineer_decor_controls_tool_reforged'
$AssetRoot = Join-Path $ProjectRoot "src/main/resources/assets/$ModId"
$LangRoot = Join-Path $AssetRoot 'lang'
$ManualRoot = Join-Path $AssetRoot 'manual'
$AuthoritativeAutoload = Join-Path $ProjectRoot 'src/main/resources/assets/immersiveengineering/manual/autoload.json'
$MirrorAutoload = Join-Path $ManualRoot 'autoload.json'
$StrictUtf8 = [System.Text.UTF8Encoding]::new($false, $true)
$PlaceholderPattern = [regex]'%(?:(?:\d+)\$)?[-#+ 0,(<]*\d*(?:\.\d+)?[tT]?[a-zA-Z%]'
$Errors = [System.Collections.Generic.List[string]]::new()
$ExpectedSemanticValues = [ordered]@{
    "block.$ModId.metal_crafting_table.slots.hammer" = "Engineer's Hammer"
    "item.$ModId.crushing_hammer.help" = "Manual handheld ore crushing`n hammer. Craft with raw iron, raw`n gold, or matching ores to make`n Immersive Engineering dust. The`n hammer takes durability damage`n and is not consumed until it`n breaks."
    "item.$ModId.diving_capsule.help" = "Restores air when you are about`n to drown. It works from inventory`n with a cooldown and can also be`n used manually."
    "item.$ModId.material_box.help" = "Portable single-material storage.`n Hold the box in the main hand and`n a stack in the offhand to store it;`n use with empty offhand to retrieve."
    "item.$ModId.musli_bar_press.help" = "Portable food press. Use with`n bread, an apple, and wheat,`n melon, pumpkin, beetroot, or`n hemp seeds to press four bars."
    "item.$ModId.musli_bar.help" = "Fast nutritional snack made from`n seeds, fruit, and bread."
    "item.$ModId.sleeping_bag.help" = "Weatherproof portable rest kit.`n Use it at night in the Overworld`n to skip to morning without setting`n a spawn point."
    "item.$ModId.stimpack.help" = "Automatically injects when health`n is low, healing, protecting, and`n briefly boosting movement with a`n cooldown. Can also be used manually."
    "item.$ModId.tracker.help" = "Click a block to store its location.`n Sneak-use to clear the target. The`n tooltip reports the saved dimension`n and coordinates."
    "jei.info.$ModId.crushing_hammer" = "Portable ore-processing hammer. Crafting recipes damage it and return the same tool. Hitting a target with the hammer applies knockback."
    "jei.info.$ModId.diving_capsule" = "Emergency air capsule. It can refill air manually or auto-trigger at low air, then enters cooldown."
    "jei.info.$ModId.material_box" = "Stores up to 512 of one plain, stackable material type. Hold the box in main hand with matching material in off hand to load it, or use it with an empty off hand to retrieve material."
    "jei.info.$ModId.metal_crafting_table" = "Metal workbench with a dedicated hammer slot, a 3x3 crafting grid, and an output slot. JEI shows it as a crafting catalyst."
    "jei.info.$ModId.musli_bar" = "Fast engineer ration produced by the Muslee Bar Press or its recipe."
    "jei.info.$ModId.musli_bar_press" = "Hand tool for compact rations. It consumes bread, an apple, and an accepted seed to create four Muslee Bars when output space is available."
    "jei.info.$ModId.sleeping_bag" = "Portable overworld sleep tool. It advances a safe night to morning without setting spawn and refuses unsafe or non-night use."
    "jei.info.$ModId.stimpack" = "Emergency medical tool. It can be used manually, and it can auto-trigger at low health to grant short survival effects before entering cooldown."
    "jei.info.$ModId.tracker" = "Stores a target dimension and block position on use. Sneak-use clears the stored target, and the tooltip shows the saved location when one is complete."
}

function Read-StrictUtf8Text {
    param([Parameter(Mandatory)][string]$Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $offset = 0
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $offset = 3
    }
    return $StrictUtf8.GetString($bytes, $offset, $bytes.Length - $offset)
}

function Read-LocaleObject {
    param([Parameter(Mandatory)][string]$Path)

    $text = Read-StrictUtf8Text -Path $Path
    $document = [System.Text.Json.JsonDocument]::Parse($text)
    try {
        if ($document.RootElement.ValueKind -ne [System.Text.Json.JsonValueKind]::Object) {
            throw 'root value is not an object'
        }

        $map = [System.Collections.Generic.Dictionary[string, string]]::new([System.StringComparer]::Ordinal)
        foreach ($property in $document.RootElement.EnumerateObject()) {
            if ($property.Value.ValueKind -ne [System.Text.Json.JsonValueKind]::String) {
                throw "key '$($property.Name)' does not contain a string"
            }
            if (-not $map.TryAdd($property.Name, $property.Value.GetString())) {
                throw "duplicate key '$($property.Name)'"
            }
        }
        return $map
    }
    finally {
        $document.Dispose()
    }
}

function Get-SortedPlaceholders {
    param([AllowEmptyString()][string]$Value)

    $tokens = @($PlaceholderPattern.Matches($Value) | ForEach-Object { $_.Value })
    [System.Array]::Sort($tokens, [System.StringComparer]::Ordinal)
    return ,$tokens
}

function Test-StringArraysEqual {
    param([string[]]$Left, [string[]]$Right)

    if ($Left.Count -ne $Right.Count) {
        return $false
    }
    for ($index = 0; $index -lt $Left.Count; $index++) {
        if (-not [string]::Equals($Left[$index], $Right[$index], [System.StringComparison]::Ordinal)) {
            return $false
        }
    }
    return $true
}

function Add-ManualSources {
    param(
        [Parameter(Mandatory)][System.Text.Json.JsonElement]$Element,
        [Parameter(Mandatory)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Sources
    )

    switch ($Element.ValueKind) {
        ([System.Text.Json.JsonValueKind]::Object) {
            foreach ($property in $Element.EnumerateObject()) {
                if ($property.Name -eq 'source' -and $property.Value.ValueKind -eq [System.Text.Json.JsonValueKind]::String) {
                    $Sources.Add($property.Value.GetString())
                }
                Add-ManualSources -Element $property.Value -Sources $Sources
            }
        }
        ([System.Text.Json.JsonValueKind]::Array) {
            foreach ($item in $Element.EnumerateArray()) {
                Add-ManualSources -Element $item -Sources $Sources
            }
        }
    }
}

if (-not (Test-Path -LiteralPath $LangRoot -PathType Container)) {
    throw "Language directory not found: $LangRoot"
}
if (-not (Test-Path -LiteralPath $ManualRoot -PathType Container)) {
    throw "Manual directory not found: $ManualRoot"
}

$LocaleFiles = @(Get-ChildItem -LiteralPath $LangRoot -Filter '*.json' -File | Sort-Object Name)
if ($LocaleFiles.Count -ne $ExpectedLocaleCount) {
    $Errors.Add("Expected $ExpectedLocaleCount language locale files, found $($LocaleFiles.Count)")
}
$EnglishFile = Join-Path $LangRoot 'en_us.json'
if (-not (Test-Path -LiteralPath $EnglishFile -PathType Leaf)) {
    throw "English locale not found: $EnglishFile"
}

$ParsedLocales = [System.Collections.Generic.Dictionary[string, object]]::new([System.StringComparer]::Ordinal)
foreach ($file in $LocaleFiles) {
    try {
        $ParsedLocales.Add($file.BaseName, (Read-LocaleObject -Path $file.FullName))
    }
    catch {
        $Errors.Add("$($file.Name): $($_.Exception.Message)")
    }
}

if (-not $ParsedLocales.ContainsKey('en_us')) {
    throw 'English locale could not be parsed; parity cannot be evaluated.'
}

$English = $ParsedLocales['en_us']
$EnglishKeys = @($English.Keys)
[System.Array]::Sort($EnglishKeys, [System.StringComparer]::Ordinal)
$SemanticValueMismatchCount = 0
foreach ($semanticKey in $ExpectedSemanticValues.Keys) {
    if (-not $English.ContainsKey($semanticKey) -or
        -not [string]::Equals(
            [string]$English[$semanticKey],
            [string]$ExpectedSemanticValues[$semanticKey],
            [System.StringComparison]::Ordinal
        )) {
        $SemanticValueMismatchCount++
        $Errors.Add("English semantic baseline mismatch: $semanticKey")
    }
}
$KeyCounts = [System.Collections.Generic.List[int]]::new()
$MissingKeyCount = 0
$ExtraKeyCount = 0
$PlaceholderMismatchCount = 0
$NewlineMismatchCount = 0
$EmptyValueCount = 0
$ExactEnglishClones = [System.Collections.Generic.List[string]]::new()

foreach ($locale in @($ParsedLocales.Keys | Sort-Object)) {
    $map = $ParsedLocales[$locale]
    $KeyCounts.Add($map.Count)

    $missing = @($EnglishKeys | Where-Object { -not $map.ContainsKey($_) })
    $extra = @($map.Keys | Where-Object { -not $English.ContainsKey($_) })
    $MissingKeyCount += $missing.Count
    $ExtraKeyCount += $extra.Count
    foreach ($key in $missing) { $Errors.Add("$locale missing key: $key") }
    foreach ($key in $extra) { $Errors.Add("$locale has extra key: $key") }

    $allValuesMatchEnglish = $locale -ne 'en_us' -and $missing.Count -eq 0 -and $extra.Count -eq 0
    foreach ($key in $EnglishKeys) {
        if (-not $map.ContainsKey($key)) {
            $allValuesMatchEnglish = $false
            continue
        }

        $value = [string]$map[$key]
        if ([string]::IsNullOrWhiteSpace($value)) {
            $EmptyValueCount++
            $Errors.Add("$locale has an empty value: $key")
        }

        $expectedPlaceholders = @(Get-SortedPlaceholders -Value ([string]$English[$key]))
        $actualPlaceholders = @(Get-SortedPlaceholders -Value $value)
        if (-not (Test-StringArraysEqual -Left $expectedPlaceholders -Right $actualPlaceholders)) {
            $PlaceholderMismatchCount++
            $Errors.Add("$locale placeholder mismatch: $key")
        }

        $expectedNewlines = ([regex]::Matches([string]$English[$key], "`n")).Count
        $actualNewlines = ([regex]::Matches($value, "`n")).Count
        if ($expectedNewlines -ne $actualNewlines) {
            $NewlineMismatchCount++
            $Errors.Add("$locale newline mismatch: $key ($actualNewlines instead of $expectedNewlines)")
        }

        if ($allValuesMatchEnglish -and -not [string]::Equals($value, [string]$English[$key], [System.StringComparison]::Ordinal)) {
            $allValuesMatchEnglish = $false
        }
    }
    if ($allValuesMatchEnglish) {
        $ExactEnglishClones.Add($locale)
        $Errors.Add("$locale is an exact full-file English clone")
    }
}

$AutoloadMirrorMatch = $false
$ManualSources = [System.Collections.Generic.List[string]]::new()
if (-not (Test-Path -LiteralPath $AuthoritativeAutoload -PathType Leaf) -or -not (Test-Path -LiteralPath $MirrorAutoload -PathType Leaf)) {
    $Errors.Add('Both Engineer''s Manual autoload files must exist.')
}
else {
    try {
        $authoritativeHash = (Get-FileHash -LiteralPath $AuthoritativeAutoload -Algorithm SHA256).Hash
        $mirrorHash = (Get-FileHash -LiteralPath $MirrorAutoload -Algorithm SHA256).Hash
        $AutoloadMirrorMatch = $authoritativeHash -eq $mirrorHash
        if (-not $AutoloadMirrorMatch) {
            $Errors.Add('Engineer''s Manual autoload files differ.')
        }

        $autoloadText = Read-StrictUtf8Text -Path $AuthoritativeAutoload
        $autoloadDocument = [System.Text.Json.JsonDocument]::Parse($autoloadText)
        try {
            Add-ManualSources -Element $autoloadDocument.RootElement -Sources $ManualSources
        }
        finally {
            $autoloadDocument.Dispose()
        }
    }
    catch {
        $Errors.Add("Engineer''s Manual autoload validation failed: $($_.Exception.Message)")
    }
}

$SourceSlugs = [System.Collections.Generic.List[string]]::new()
$SeenSources = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
foreach ($source in $ManualSources) {
    if (-not $SeenSources.Add($source)) {
        $Errors.Add("Duplicate manual source: $source")
    }
    $prefix = "$ModId`:"
    if (-not $source.StartsWith($prefix, [System.StringComparison]::Ordinal)) {
        $Errors.Add("Manual source is not qualified with ${ModId}: $source")
        continue
    }
    $slug = $source.Substring($prefix.Length)
    $SourceSlugs.Add($slug)
    $entryJson = Join-Path $ManualRoot ($slug + '.json')
    if (-not (Test-Path -LiteralPath $entryJson -PathType Leaf)) {
        $Errors.Add("Missing manual entry JSON: $slug")
    }
}

$ExpectedManualPages = @($SourceSlugs | Sort-Object -Unique)
if ($ExpectedManualPages.Count -ne $ExpectedManualSourceCount) {
    $Errors.Add("Expected $ExpectedManualSourceCount unique manual sources, found $($ExpectedManualPages.Count)")
}
$ManualDirectories = @(Get-ChildItem -LiteralPath $ManualRoot -Directory | Sort-Object Name)
if ($ManualDirectories.Count -ne $ExpectedLocaleCount) {
    $Errors.Add("Expected $ExpectedLocaleCount manual locale directories, found $($ManualDirectories.Count)")
}
$ManualPageCounts = [System.Collections.Generic.List[int]]::new()
$ManualMissingCount = 0
$ManualExtraCount = 0
$ManualInvalidUtf8Count = 0
$ManualEmptyCount = 0
$GritAnchorLocaleCount = 0
$MusliAnchorLocaleCount = 0
$MusliRecipeAnchorLocaleCount = 0

$LanguageLocales = @($LocaleFiles.BaseName | Sort-Object)
$ManualLocales = @($ManualDirectories.Name | Sort-Object)
foreach ($locale in $LanguageLocales) {
    if ($locale -notin $ManualLocales) {
        $Errors.Add("Missing manual locale directory: $locale")
    }
}
foreach ($locale in $ManualLocales) {
    if ($locale -notin $LanguageLocales) {
        $Errors.Add("Manual locale has no matching language JSON: $locale")
    }
}

foreach ($directory in $ManualDirectories) {
    $pages = @(Get-ChildItem -LiteralPath $directory.FullName -Filter '*.txt' -File | Sort-Object Name)
    $ManualPageCounts.Add($pages.Count)
    $actualSlugs = @($pages.BaseName)
    $missing = @($ExpectedManualPages | Where-Object { $_ -notin $actualSlugs })
    $extra = @($actualSlugs | Where-Object { $_ -notin $ExpectedManualPages })
    $ManualMissingCount += $missing.Count
    $ManualExtraCount += $extra.Count
    foreach ($slug in $missing) { $Errors.Add("$($directory.Name) missing manual page: $slug") }
    foreach ($slug in $extra) { $Errors.Add("$($directory.Name) has extra manual page: $slug") }

    foreach ($page in $pages) {
        try {
            $text = Read-StrictUtf8Text -Path $page.FullName
            if ([string]::IsNullOrWhiteSpace($text)) {
                $ManualEmptyCount++
                $Errors.Add("Empty manual page: $($directory.Name)/$($page.Name)")
            }
        }
        catch {
            $ManualInvalidUtf8Count++
            $Errors.Add("Invalid UTF-8 manual page $($directory.Name)/$($page.Name): $($_.Exception.Message)")
        }
    }

    $engineerTools = Join-Path $directory.FullName 'engineer_tools.txt'
    if (Test-Path -LiteralPath $engineerTools -PathType Leaf) {
        $text = Read-StrictUtf8Text -Path $engineerTools
        if ($text.Contains('<&iron_grit>') -and $text.Contains('<&gold_grit>')) {
            $GritAnchorLocaleCount++
        }
        else {
            $Errors.Add("$($directory.Name) engineer_tools.txt is missing grit anchors")
        }
        if ($text.Contains('<&musli_bar>')) {
            $MusliAnchorLocaleCount++
        }
        else {
            $Errors.Add("$($directory.Name) engineer_tools.txt is missing the Musli Bar recipe anchor")
        }
    }

    $musliBarPage = Join-Path $directory.FullName 'musli_bar.txt'
    if (Test-Path -LiteralPath $musliBarPage -PathType Leaf) {
        $text = Read-StrictUtf8Text -Path $musliBarPage
        if ($text.Contains('<&recipe>')) {
            $MusliRecipeAnchorLocaleCount++
        }
        else {
            $Errors.Add("$($directory.Name) musli_bar.txt is missing the recipe anchor")
        }
    }
}

$MusliWidgetDefinitionsValid = $false
$EngineerToolsWidget = Join-Path $ManualRoot 'engineer_tools.json'
$MusliBarWidget = Join-Path $ManualRoot 'musli_bar.json'
try {
    $engineerWidgetDocument = [System.Text.Json.JsonDocument]::Parse((Read-StrictUtf8Text -Path $EngineerToolsWidget))
    $musliWidgetDocument = [System.Text.Json.JsonDocument]::Parse((Read-StrictUtf8Text -Path $MusliBarWidget))
    try {
        $engineerRecipe = $engineerWidgetDocument.RootElement.GetProperty('musli_bar').GetProperty('recipe').GetString()
        $engineerType = $engineerWidgetDocument.RootElement.GetProperty('musli_bar').GetProperty('type').GetString()
        $pageRecipe = $musliWidgetDocument.RootElement.GetProperty('recipe').GetProperty('recipe').GetString()
        $pageType = $musliWidgetDocument.RootElement.GetProperty('recipe').GetProperty('type').GetString()
        $expectedRecipe = "$ModId`:musli_bar"
        $MusliWidgetDefinitionsValid =
            $engineerType -eq 'crafting' -and $pageType -eq 'crafting' -and
            $engineerRecipe -eq $expectedRecipe -and $pageRecipe -eq $expectedRecipe
        if (-not $MusliWidgetDefinitionsValid) {
            $Errors.Add('Musli Bar widget definitions do not reference the active crafting recipe.')
        }
    }
    finally {
        $engineerWidgetDocument.Dispose()
        $musliWidgetDocument.Dispose()
    }
}
catch {
    $Errors.Add("Musli Bar widget validation failed: $($_.Exception.Message)")
}

$minimumKeys = if ($KeyCounts.Count -gt 0) { ($KeyCounts | Measure-Object -Minimum).Minimum } else { 0 }
$maximumKeys = if ($KeyCounts.Count -gt 0) { ($KeyCounts | Measure-Object -Maximum).Maximum } else { 0 }
$minimumManualPages = if ($ManualPageCounts.Count -gt 0) { ($ManualPageCounts | Measure-Object -Minimum).Minimum } else { 0 }
$maximumManualPages = if ($ManualPageCounts.Count -gt 0) { ($ManualPageCounts | Measure-Object -Maximum).Maximum } else { 0 }

$Report = [ordered]@{
    result = if ($Errors.Count -eq 0) { 'PASS' } else { 'FAIL' }
    expected_locales = $ExpectedLocaleCount
    locales = $LocaleFiles.Count
    parsed_locales = $ParsedLocales.Count
    english_keys = $EnglishKeys.Count
    minimum_keys = $minimumKeys
    maximum_keys = $maximumKeys
    missing_keys = $MissingKeyCount
    extra_keys = $ExtraKeyCount
    empty_values = $EmptyValueCount
    placeholder_mismatches = $PlaceholderMismatchCount
    newline_mismatches = $NewlineMismatchCount
    semantic_value_mismatches = $SemanticValueMismatchCount
    exact_english_clone_locales = @($ExactEnglishClones)
    expected_manual_sources = $ExpectedManualSourceCount
    manual_sources = $ExpectedManualPages.Count
    manual_locales = $ManualDirectories.Count
    minimum_manual_pages = $minimumManualPages
    maximum_manual_pages = $maximumManualPages
    missing_manual_pages = $ManualMissingCount
    extra_manual_pages = $ManualExtraCount
    invalid_utf8_manual_pages = $ManualInvalidUtf8Count
    empty_manual_pages = $ManualEmptyCount
    grit_anchor_locales = $GritAnchorLocaleCount
    musli_anchor_locales = $MusliAnchorLocaleCount
    musli_recipe_anchor_locales = $MusliRecipeAnchorLocaleCount
    musli_widget_definitions_valid = $MusliWidgetDefinitionsValid
    autoload_mirrors_match = $AutoloadMirrorMatch
    errors = @($Errors)
}

$resolvedReportPath = if ([System.IO.Path]::IsPathRooted($ReportPath)) {
    $ReportPath
}
else {
    Join-Path $ProjectRoot $ReportPath
}
$reportDirectory = Split-Path -Parent $resolvedReportPath
if (-not (Test-Path -LiteralPath $reportDirectory)) {
    New-Item -ItemType Directory -Path $reportDirectory -Force | Out-Null
}
[System.IO.File]::WriteAllText(
    $resolvedReportPath,
    (($Report | ConvertTo-Json -Depth 6) + "`n"),
    [System.Text.UTF8Encoding]::new($false)
)

$Report | ConvertTo-Json -Depth 6
if ($Errors.Count -ne 0) {
    exit 1
}
