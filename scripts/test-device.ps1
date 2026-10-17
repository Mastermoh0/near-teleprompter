param([Parameter(Mandatory = $true)][string]$Serial)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'gradle.ps1') @('assembleDebug','assembleDebugAndroidTest')
$adb = Join-Path $projectRoot '.tools\android-sdk\platform-tools\adb.exe'
& $adb -s $Serial install -r (Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk')
if ($LASTEXITCODE -ne 0) { throw 'App installation failed.' }
& $adb -s $Serial install -r (Join-Path $projectRoot 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk')
if ($LASTEXITCODE -ne 0) { throw 'Test installation failed.' }
$testOutput = & $adb -s $Serial shell am instrument -w -r com.mastermoh.near.test/androidx.test.runner.AndroidJUnitRunner
$testOutput | Tee-Object -FilePath (Join-Path $projectRoot '.tools\instrumentation.log')
if (-not ($testOutput -match 'OK \(\d+ tests?\)')) { throw 'Android device tests did not pass. See .tools/instrumentation.log.' }
