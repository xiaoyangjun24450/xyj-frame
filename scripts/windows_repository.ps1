param([Parameter(Mandatory=$true)][string]$Repository, [switch]$BuildOnly, [switch]$Logs, [string]$Serial)
$ErrorActionPreference = 'Stop'
try {
    . "$PSScriptRoot\windows_env.ps1"
    $repo = (Resolve-Path -LiteralPath $Repository).Path
    if (-not (Test-Path "$repo\.git")) { throw 'Independent Git checkout required.' }
    $config = Get-Content "$repo\windows-host.json" -Raw | ConvertFrom-Json
    $adbArgs = @()
    if ($Serial) { $adbArgs = @('-s', $Serial) }
    if (-not $Logs) {
        Initialize-BuildEnvironment -BuildRoot $repo
        $gradle = Get-ProjectGradle
        Push-Location $repo
        try {
            & $gradle -p $repo $config.task
            if ($LASTEXITCODE -ne 0) { throw "Build failed ($LASTEXITCODE)." }
        } finally { Pop-Location }
    }
    $metadataPath = Join-Path $repo $config.metadata
    $metadata = Get-Content $metadataPath -Raw | ConvertFrom-Json
    if ($Logs) {
        $adb = Find-Adb
        $processId = & $adb @adbArgs shell pidof -s $metadata.applicationId
        if ($LASTEXITCODE -ne 0 -or "$processId".Trim() -notmatch '^\d+$') { throw 'Open the app on the device first.' }
        & $adb @adbArgs logcat "--pid=$($processId.Trim())" '*:I'
    } elseif (-not $BuildOnly) {
        if (@($metadata.elements).Count -ne 1) { throw 'Split APKs require an explicit installation plan.' }
        $apk = Join-Path (Split-Path $metadataPath) $metadata.elements[0].outputFile
        $adb = Find-Adb
        & $adb @adbArgs install -r -t $apk
    }
    if ($LASTEXITCODE -ne 0) { throw "Command failed ($LASTEXITCODE)." }
} catch { Write-Error $_ -ErrorAction Continue; exit 1 }
