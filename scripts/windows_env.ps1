$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
function Initialize-BuildEnvironment {
    param([string]$BuildRoot = $ProjectRoot)
    $localJava = Join-Path $ProjectRoot '.tools\jdk'
    if (Test-Path "$localJava\bin\java.exe") { $env:JAVA_HOME = $localJava }
    if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
        throw 'JDK missing. Follow docs/AGENT_SETUP.md; place JDK 17 in .tools\jdk or set JAVA_HOME.'
    }
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $localSdk = Join-Path $ProjectRoot '.tools\android-sdk'
    if (Test-Path "$localSdk\platforms\android-36\android.jar") { $env:ANDROID_HOME = $localSdk }
    if (-not $env:ANDROID_HOME -or -not (Test-Path "$env:ANDROID_HOME\platforms\android-36\android.jar")) {
        throw 'Android SDK 36 missing. Follow docs/AGENT_SETUP.md or set ANDROID_HOME.'
    }
    $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
    $env:GRADLE_USER_HOME = Join-Path $ProjectRoot '.gradle-home'
    # Preserve other local properties; replace only the machine-specific SDK location.
    $file = Join-Path $BuildRoot 'local.properties'
    $lines = @()
    if (Test-Path $file) { $lines = @(Get-Content $file | Where-Object { $_ -notmatch '^\s*sdk\.dir\s*=' }) }
    $sdkPath = $env:ANDROID_HOME.Replace('\','/').Replace(':','\:')
    $lines += "sdk.dir=$sdkPath"
    [IO.File]::WriteAllLines($file, $lines, [Text.UTF8Encoding]::new($false))
}
function Find-Adb {
    $candidates = @($env:ADB, "$ProjectRoot\.tools\android-sdk\platform-tools\adb.exe")
    if ($env:ANDROID_HOME) { $candidates += "$env:ANDROID_HOME\platform-tools\adb.exe" }
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate -PathType Leaf)) { return $candidate }
    }
    $command = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    throw 'adb.exe missing. Follow docs/AGENT_SETUP.md or set ADB to its full path.'
}

function Get-ProjectGradle {
    $zip = Join-Path $ProjectRoot '.tools\downloads\gradle-8.14.3-bin.zip'
    $install = Join-Path $ProjectRoot '.tools\gradle'
    $launcher = Join-Path $install 'gradle-8.14.3\bin\gradle.bat'
    if (Test-Path $zip) {
        $expected = 'bd71102213493060956ec229d946beee57158dbd89d0e62b91bca0fa2c5f3531'
        $stream = [IO.File]::OpenRead($zip)
        $sha = [Security.Cryptography.SHA256]::Create()
        try { $actual = [BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-', '').ToLowerInvariant() }
        finally { $stream.Dispose(); $sha.Dispose() }
        if ($actual -ne $expected) {
            throw "Incomplete or invalid Gradle ZIP: $zip. Replace it with the complete browser download (gradle-8.14.3-bin.zip)."
        }
        if (-not (Test-Path $launcher)) {
            if (Test-Path $install) {
                throw "Incomplete Gradle extraction at $install. Move this directory aside, then retry."
            }
            Write-Host 'Extracting verified local Gradle ZIP...'
            Add-Type -AssemblyName System.IO.Compression.FileSystem
            [IO.Compression.ZipFile]::ExtractToDirectory($zip, $install)
        }
        if (-not (Test-Path $launcher)) { throw 'Gradle launcher missing after extraction.' }
        Write-Host 'Using project-local Gradle 8.14.3.'
        return $launcher
    }
    return (Join-Path $ProjectRoot 'scripts\gradlew.bat')
}
