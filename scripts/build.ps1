param([switch]$Verify)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = Join-Path $projectRoot '.tools\jdk'
$env:ANDROID_HOME = Join-Path $projectRoot '.tools\android-sdk'
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.android'
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$buildTemp = Join-Path $projectRoot '.tools\tmp'
New-Item -ItemType Directory -Force -Path $buildTemp | Out-Null
# Windows packaged environments can virtualize the default socket temp directory.
$env:TEMP = $buildTemp
$env:TMP = $buildTemp
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=`"$buildTemp`" -Djava.io.tmpdir=`"$buildTemp`""
$gradle = Join-Path $projectRoot '.tools\gradle-8.13\bin\gradle.bat'
if (-not (Test-Path -LiteralPath $gradle)) { throw 'Run scripts/setup-tools.ps1 first, or use the Gradle wrapper with your Android SDK.' }
Push-Location $projectRoot
try {
    if ($Verify) { & $gradle --console=plain assembleDebug testDebugUnitTest lintDebug }
    else { & $gradle --console=plain assembleDebug }
    if ($LASTEXITCODE -ne 0) { throw 'Android build failed.' }
    New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot 'releases') | Out-Null
    $version = [regex]::Match((Get-Content (Join-Path $projectRoot 'app\build.gradle') -Raw), "versionName\s+'([^']+)'").Groups[1].Value
    if (-not $version) { throw 'Could not read versionName.' }
    $apkName = "near-teleprompter-$version-debug.apk"
    Copy-Item -LiteralPath (Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk') -Destination (Join-Path $projectRoot "releases\$apkName") -Force
    Write-Output "APK: releases/$apkName"
} finally { Pop-Location }
