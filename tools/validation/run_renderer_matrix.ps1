# Run from the repository root; only fresh build/ profiles are used.

[CmdletBinding()]
param([switch]$FlowDistanceOnly, [string]$ResumeDirectory)
$ErrorActionPreference='Stop'
$root=Get-Location
if($ResumeDirectory){
 $matrix=(Resolve-Path -LiteralPath $ResumeDirectory).Path
 $owned=(Join-Path $root 'build')+[IO.Path]::DirectorySeparatorChar
 if(-not $matrix.StartsWith($owned,[StringComparison]::OrdinalIgnoreCase)){throw 'Resume directory must be inside this repository build directory'}
}else{
 $matrix=Join-Path $root ($(if($FlowDistanceOnly){'build/renderer-flow-matrix-'}else{'build/renderer-matrix-'})+[guid]::NewGuid().ToString('N'))
 New-Item -ItemType Directory -Path $matrix | Out-Null
}
$pathFile=if($FlowDistanceOnly){'build/renderer-flow-matrix-path.txt'}else{'build/renderer-matrix-path.txt'}
Set-Content -LiteralPath $pathFile -Value $matrix
$deps=Join-Path $matrix 'dependencies'
if(-not (Test-Path -LiteralPath $deps)){New-Item -ItemType Directory -Path $deps | Out-Null}
$paths=@{}
foreach($id in @('J7b96IEd','ZkPOaQLQ')){
 $meta=Invoke-RestMethod -Uri ('https://api.modrinth.com/v2/version/'+$id)
 $f=@($meta.files | Where-Object primary)[0]
 if(([uri]$f.url).Host -ne 'cdn.modrinth.com'){throw 'Nonofficial dependency host'}
 $target=Join-Path $deps $f.filename
 if(-not (Test-Path -LiteralPath $target)){Invoke-WebRequest -Uri $f.url -OutFile $target -UseBasicParsing}
 if((Get-FileHash -LiteralPath $target -Algorithm SHA512).Hash.ToLowerInvariant() -ne $f.hashes.sha512){throw 'Official dependency checksum mismatch'}
 $meta | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $deps ($id+'.json')) -Encoding utf8
 $paths[$id]=$target
}
[string[]]$extra=@()
if($FlowDistanceOnly){$extra=@('--flow-distance-only')}
foreach($profile in @('native','embeddium','modernfix','both')){
 $dir=Join-Path $matrix $profile
 $game=Join-Path $dir 'run'
 if(Test-Path -LiteralPath $game){
  & python tools/validation/validate_renderer_run.py --game $game --log (Join-Path $dir 'client.log') --profile $profile --exit-code 0 @extra
  if($LASTEXITCODE -ne 0){throw ("Existing incomplete/invalid profile; refusing to overwrite: "+$profile)}
  Write-Output ("REUSE "+$profile+" completed evidence")
  continue
 }
 New-Item -ItemType Directory -Path $game | Out-Null
 $nonce=[guid]::NewGuid().ToString('N')
 [IO.File]::WriteAllText((Join-Path $game '.iedctr-client-gameplay-nonce'),$nonce)
 [IO.File]::WriteAllLines((Join-Path $game 'options.txt'),@('onboardAccessibility:false','tutorialStep:none','renderDistance:2','simulationDistance:2','maxFps:60','pauseOnLostFocus:false','autoJump:true','soundCategory_master:0.0'))
 $env:IEDCTR_FLOW_DISTANCE_ONLY=$FlowDistanceOnly.ToString().ToLowerInvariant()
 $env:IEDCTR_GAMEPLAY_DIR=$game
 $env:IEDCTR_GAMEPLAY_NONCE=$nonce
 $selected=@()
 if($profile -in @('embeddium','both')){$selected += $paths['J7b96IEd']}
 if($profile -in @('modernfix','both')){$selected += $paths['ZkPOaQLQ']}
 $env:IEDCTR_RENDERER_JARS=$selected -join ';'
 $init=Join-Path $dir 'renderer.init.gradle'
 $initText=@"
gradle.projectsEvaluated {
    def jars = System.getenv('IEDCTR_RENDERER_JARS')
    if (jars) rootProject.dependencies.add('runtimeOnly', rootProject.files(jars.split(';')))
    rootProject.extensions.getByName('neoForge').runs.named('client') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplay', 'true'
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        systemProperty 'iedctr.canalEast', 'true'
        systemProperty 'iedctr.flowDistanceOnly', System.getenv('IEDCTR_FLOW_DISTANCE_ONLY')
        programArgument '--width'
        programArgument '1280'
        programArgument '--height'
        programArgument '720'
    }
}
"@
 [IO.File]::WriteAllText($init,$initText)
 Write-Output ("START "+$profile+" "+[DateTime]::UtcNow.ToString('o')+" "+$dir)
 & .\scripts\invoke-gradle.ps1 --init-script $init runClient --stacktrace *> (Join-Path $dir 'client.log')
 $exit=$LASTEXITCODE
 Write-Output ("EXIT "+$profile+" "+$exit+" "+[DateTime]::UtcNow.ToString('o'))
 if($exit -ne 0){throw ("Renderer profile failed: "+$profile)}
 & python tools/validation/validate_renderer_run.py --game $game --log (Join-Path $dir 'client.log') --profile $profile --exit-code $exit @extra
 if($LASTEXITCODE -ne 0){throw ("Renderer evidence failed: "+$profile)}
}

