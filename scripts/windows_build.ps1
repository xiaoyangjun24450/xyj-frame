param([Parameter(Mandatory=$true)][ValidatePattern('^[a-z][a-z0-9-]*$')][string]$App, [switch]$BuildOnly, [string]$Serial)
$ErrorActionPreference = 'Stop'
try {
    . "$PSScriptRoot\windows_env.ps1"
    $appDir = Join-Path $ProjectRoot "apps\$App"
    if (-not (Test-Path "$appDir\build.gradle.kts")) { throw "Unknown app: $App" }
    Initialize-BuildEnvironment
    Push-Location $ProjectRoot
    try {
        $gradle = Get-ProjectGradle
        & $gradle ":${App}:assembleDebug"
        if ($LASTEXITCODE -ne 0) { throw "Build failed ($LASTEXITCODE)." }
        if (-not $BuildOnly) {
            $adb = Find-Adb
            $deviceArgs = @()
            if ($Serial) { $deviceArgs = @('-s', $Serial) }
            & $adb @deviceArgs install -r -t "$appDir\build\outputs\apk\debug\$App-debug.apk"
            if ($LASTEXITCODE -ne 0) { throw "Installation failed ($LASTEXITCODE). Check adb devices and USB authorization." }
        }
    } finally { Pop-Location }
} catch { Write-Error $_ -ErrorAction Continue; exit 1 }
