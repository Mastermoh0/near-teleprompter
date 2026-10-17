param([switch]$CreateKey)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$signingRoot = Join-Path $projectRoot '.signing'
$signingProperties = Join-Path $signingRoot 'release.properties'
$signingKey = Join-Path $signingRoot 'near-release.p12'
$keytool = Join-Path $projectRoot '.tools\jdk\bin\keytool.exe'
if (-not (Test-Path -LiteralPath $keytool)) { throw 'Run scripts/setup-tools.ps1 first.' }

if ($CreateKey -and -not (Test-Path -LiteralPath $signingProperties)) {
    if (Test-Path -LiteralPath $signingKey) { throw 'An existing signing key has no properties file. Restore its configuration; do not replace the key.' }
    New-Item -ItemType Directory -Force -Path $signingRoot | Out-Null
    $randomBytes = [byte[]]::new(32)
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($randomBytes)
    $signingPassword = [Convert]::ToBase64String($randomBytes)
    $previousKeytoolPassword = $env:NEAR_KEYTOOL_PASSWORD
    try {
        $env:NEAR_KEYTOOL_PASSWORD = $signingPassword
        & $keytool -genkeypair -keystore $signingKey -storetype PKCS12 -alias near-release -keyalg RSA -keysize 4096 -validity 10000 -storepass:env NEAR_KEYTOOL_PASSWORD -keypass:env NEAR_KEYTOOL_PASSWORD -dname 'CN=Near Teleprompter Release'
        if ($LASTEXITCODE -ne 0) { throw 'Release signing key generation failed.' }
        $propertyText = "storeFile=.signing/near-release.p12`nstorePassword=$signingPassword`nkeyAlias=near-release`nkeyPassword=$signingPassword`n"
        [System.IO.File]::WriteAllText($signingProperties, $propertyText, [System.Text.UTF8Encoding]::new($false))
    } finally {
        $env:NEAR_KEYTOOL_PASSWORD = $previousKeytoolPassword
        $signingPassword = $null
        $propertyText = $null
    }
    Write-Output 'Release key created in ignored .signing/. Back up the key and its properties privately before publishing updates.'
}
if (-not (Test-Path -LiteralPath $signingProperties) -and -not $env:NEAR_SIGNING_STORE_FILE) {
    throw 'Release signing is missing. Use -CreateKey for your first release or restore your existing signing configuration.'
}

& (Join-Path $PSScriptRoot 'gradle.ps1') @('assembleRelease','testReleaseUnitTest','lintRelease')
$buildSource = Get-Content -LiteralPath (Join-Path $projectRoot 'app\build.gradle') -Raw
$version = [regex]::Match($buildSource, "versionName\s+'([^']+)'").Groups[1].Value
if (-not $version) { throw 'Could not read versionName.' }
$outputRoot = Join-Path $projectRoot 'releases'
New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null
$apkName = "near-teleprompter-$version.apk"
$apkOutput = Join-Path $outputRoot $apkName
Copy-Item -LiteralPath (Join-Path $projectRoot 'app\build\outputs\apk\release\app-release.apk') -Destination $apkOutput -Force
$signer = Join-Path $projectRoot '.tools\android-sdk\build-tools\35.0.0\lib\apksigner.jar'
& (Join-Path $projectRoot '.tools\jdk\bin\java.exe') -jar $signer verify --verbose --print-certs $apkOutput
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
$hash = (Get-FileHash -LiteralPath $apkOutput -Algorithm SHA256).Hash.ToLowerInvariant()
[System.IO.File]::WriteAllText((Join-Path $outputRoot 'SHA256SUMS.txt'), "$hash  $apkName`n", [System.Text.UTF8Encoding]::new($false))
Write-Output "Signed release APK: releases/$apkName"
