param([Parameter(Mandatory=$true)][ValidatePattern('^[a-z][a-z0-9-]*$')][string]$App, [string]$Serial)
$ErrorActionPreference = 'Stop'
try {
    . "$PSScriptRoot\windows_env.ps1"
    $metadata = Join-Path $ProjectRoot "apps\$App\build\outputs\apk\debug\output-metadata.json"
    if (-not (Test-Path $metadata)) { throw "Build $App first to obtain its application ID." }
    $Package = (Get-Content $metadata -Raw | ConvertFrom-Json).applicationId
    if (-not $Package -or $Package -notmatch '^[a-zA-Z0-9_.]+$') { throw 'Invalid application ID in build metadata.' }
    $adb = Find-Adb
    $deviceArgs = @()
    if ($Serial) { $deviceArgs = @('-s', $Serial) }
    $processId = & $adb @deviceArgs shell pidof -s $Package
    if ($LASTEXITCODE -ne 0 -or -not $processId) { throw "Process not found: $Package. Open the app on the phone first; check adb devices." }
    $processId = "$processId".Trim()
    if ($processId -notmatch '^\d+$') { throw "Unexpected process ID: $processId" }
    & $adb @deviceArgs logcat "--pid=$processId" '*:I'
    if ($LASTEXITCODE -ne 0) { throw "logcat failed ($LASTEXITCODE)." }
} catch { Write-Error $_ -ErrorAction Continue; exit 1 }
