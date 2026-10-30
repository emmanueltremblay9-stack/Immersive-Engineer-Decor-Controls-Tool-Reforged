param(
   [string]$OldNamespace = "engineers_decor_reforged",
   [string]$CurrentNamespace = "immersive_engineer_decor_controls_tool_reforged",
   [switch]$StrictRecipeParity
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$ResourceRoot = Join-Path $ProjectRoot "src\main\resources"
$currentAssetRoot = Join-Path $ResourceRoot "assets\$CurrentNamespace"
$currentDataRoot = Join-Path $ResourceRoot "data\$CurrentNamespace"

function Get-Names([string]$Path) {
   if (-not (Test-Path -LiteralPath $Path)) {
      return @()
   }

   return @(Get-ChildItem -LiteralPath $Path -File -Filter "*.json" | ForEach-Object { $_.BaseName } | Sort-Object -Unique)
}

function Test-LangKey([string]$LangText, [string]$Key) {
   return $LangText.Contains("`"$Key`"")
}

Set-Location -LiteralPath $ProjectRoot

$deletedOldResources = @(
   git status --short -- "src/main/resources/assets/$OldNamespace" "src/main/resources/data/$OldNamespace" |
      Where-Object { $_.StartsWith(" D ") } |
      ForEach-Object { $_.Substring(3) }
)

$missingReplacements = @(
   foreach ($oldPath in $deletedOldResources) {
      $replacement = $oldPath -replace [regex]::Escape($OldNamespace), $CurrentNamespace
      if (-not (Test-Path -LiteralPath $replacement)) {
         [pscustomobject]@{
            Old = $oldPath
            Expected = $replacement
         }
      }
   }
)

$textExtensions = @(".json", ".txt", ".mcmeta")
$currentResourceFiles = @()
foreach ($root in @($currentAssetRoot, $currentDataRoot, (Join-Path $ResourceRoot "data\minecraft"), (Join-Path $ResourceRoot "assets\immersiveengineering"))) {
   if (Test-Path -LiteralPath $root) {
      $currentResourceFiles += Get-ChildItem -LiteralPath $root -Recurse -File |
         Where-Object { $textExtensions -contains $_.Extension.ToLowerInvariant() }
   }
}

$legacyReferenceFiles = @(
   if ($currentResourceFiles.Count -gt 0) {
      Select-String -LiteralPath $currentResourceFiles.FullName -Pattern $OldNamespace -SimpleMatch -List |
         ForEach-Object { Resolve-Path -Relative $_.Path }
   }
)

$blockstates = Get-Names (Join-Path $currentAssetRoot "blockstates")
$lootTables = Get-Names (Join-Path $currentDataRoot "loot_table\blocks")
$itemModels = Get-Names (Join-Path $currentAssetRoot "models\item")
$recipes = Get-Names (Join-Path $currentDataRoot "recipe")
$recipeRoot = Join-Path $currentDataRoot "recipe"
$recipeIds = @(
   if (Test-Path -LiteralPath $recipeRoot) {
      Get-ChildItem -LiteralPath $recipeRoot -Recurse -File -Filter "*.json" | ForEach-Object {
         $relativePath = [System.IO.Path]::GetRelativePath($recipeRoot, $_.FullName)
         ($relativePath.Substring(0, $relativePath.Length - $_.Extension.Length) -replace "\\", "/")
      }
   }
)
$manualRoot = Join-Path $currentAssetRoot "manual"
$manualRecipePattern = '"recipe"\s*:\s*"' + [regex]::Escape($CurrentNamespace) + ':([^\"]+)"'
$manualRecipeReferences = @(
   if (Test-Path -LiteralPath $manualRoot) {
      Get-ChildItem -LiteralPath $manualRoot -Recurse -File -Filter "*.json" | ForEach-Object {
         $manualFile = $_
         $manualText = Get-Content -LiteralPath $manualFile.FullName -Raw
         [regex]::Matches($manualText, $manualRecipePattern) | ForEach-Object {
            [pscustomobject]@{
               File = [System.IO.Path]::GetRelativePath($ProjectRoot, $manualFile.FullName)
               Recipe = $_.Groups[1].Value
            }
         }
      }
   }
)
$missingManualRecipeReferences = @($manualRecipeReferences | Where-Object { $_.Recipe -notin $recipeIds })
$langPath = Join-Path $currentAssetRoot "lang\en_us.json"
$langText = if (Test-Path -LiteralPath $langPath) { Get-Content -LiteralPath $langPath -Raw } else { "" }

$expectedNoLoot = @("industrialswitch_top")
$expectedNoItemModel = @("ariadne_marker", "industrialswitch_top")
$expectedNoBlockLang = @("industrialswitch_top")
$internalItemModels = @("qube", "rustic_circular_gauge", "tracker_model", "tracker_pointer_model")
$allowedNoRecipe = @(
   "charged_lapis",
   "dark_shingle_roof_block",
   "gold_grit",
   "halfslab_sheetmetal_aluminum",
   "halfslab_sheetmetal_copper",
   "halfslab_sheetmetal_gold",
   "halfslab_sheetmetal_iron",
   "halfslab_sheetmetal_steel",
   "halfslab_treated_wood",
   "iron_grit",
   "musli_bar",
   "qube",
   "rustic_circular_gauge",
   "tracker_model",
   "tracker_pointer_model"
)

$missingLoot = @($blockstates | Where-Object { $_ -notin $lootTables -and $_ -notin $expectedNoLoot })
$missingItemModel = @($blockstates | Where-Object { $_ -notin $itemModels -and $_ -notin $expectedNoItemModel })
$missingBlockLang = @($blockstates | Where-Object { -not (Test-LangKey $langText "block.$CurrentNamespace.$_") -and $_ -notin $expectedNoBlockLang })
$missingItemLang = @(
   $itemModels | Where-Object {
      -not (Test-LangKey $langText "item.$CurrentNamespace.$_") -and
      -not (Test-LangKey $langText "block.$CurrentNamespace.$_") -and
      $_ -notin $internalItemModels
   }
)
$itemModelsWithoutRecipe = @($itemModels | Where-Object { $_ -notin $recipes })
$unexpectedItemModelsWithoutRecipe = @($itemModelsWithoutRecipe | Where-Object { $_ -notin $allowedNoRecipe })

$summary = [pscustomobject]@{
   DeletedOldNamespaceResources = $deletedOldResources.Count
   MissingCurrentNamespaceReplacements = $missingReplacements.Count
   CurrentResourceFilesScannedForLegacyReferences = $currentResourceFiles.Count
   CurrentResourceFilesWithLegacyReferences = $legacyReferenceFiles.Count
   Blockstates = $blockstates.Count
   BlockLootTables = $lootTables.Count
   ItemModels = $itemModels.Count
   Recipes = $recipes.Count
   ManualRecipeReferences = $manualRecipeReferences.Count
   MissingManualRecipeReferences = $missingManualRecipeReferences.Count
   MissingBlockLootTables = $missingLoot.Count
   MissingBlockItemModels = $missingItemModel.Count
   MissingBlockLangKeys = $missingBlockLang.Count
   MissingItemLangKeys = $missingItemLang.Count
   ItemModelsWithoutSameNameRecipe = $itemModelsWithoutRecipe.Count
   UnexpectedItemModelsWithoutSameNameRecipe = $unexpectedItemModelsWithoutRecipe.Count
}

$summary | Format-List

if ($missingReplacements.Count -gt 0) {
   Write-Host "Missing current namespace replacements:"
   $missingReplacements | Format-Table -AutoSize
}

if ($legacyReferenceFiles.Count -gt 0) {
   Write-Host "Current resource files with legacy namespace references:"
   $legacyReferenceFiles | Sort-Object | Format-Table -AutoSize
}

if ($missingLoot.Count -gt 0) {
   Write-Host "Missing block loot tables:"
   $missingLoot | Sort-Object
}

if ($missingItemModel.Count -gt 0) {
   Write-Host "Missing block item models:"
   $missingItemModel | Sort-Object
}

if ($missingBlockLang.Count -gt 0) {
   Write-Host "Missing block language keys:"
   $missingBlockLang | Sort-Object
}

if ($missingItemLang.Count -gt 0) {
   Write-Host "Missing item language keys:"
   $missingItemLang | Sort-Object
}

if ($missingManualRecipeReferences.Count -gt 0) {
   Write-Host "Manual references to missing local recipes:"
   $missingManualRecipeReferences | Sort-Object File, Recipe | Format-Table -AutoSize
}

if ($StrictRecipeParity -and $unexpectedItemModelsWithoutRecipe.Count -gt 0) {
   Write-Host "Unexpected item models without same-name recipes:"
   $unexpectedItemModelsWithoutRecipe | Sort-Object
}

$hardErrorCount = $missingReplacements.Count +
   $legacyReferenceFiles.Count +
   $missingLoot.Count +
   $missingItemModel.Count +
   $missingBlockLang.Count +
   $missingItemLang.Count +
   $missingManualRecipeReferences.Count

if ($StrictRecipeParity) {
   $hardErrorCount += $unexpectedItemModelsWithoutRecipe.Count
}

if ($hardErrorCount -gt 0) {
   throw "Resource parity audit failed with $hardErrorCount issue(s)."
}

Write-Host "Resource parity audit passed."
