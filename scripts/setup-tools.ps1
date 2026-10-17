$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsRoot = Join-Path $projectRoot '.tools'
New-Item -ItemType Directory -Force -Path $toolsRoot | Out-Null

function Get-VerifiedArchive($Uri, $Destination, $Sha256) {
    if (-not (Test-Path -LiteralPath $Destination)) {
        Write-Output "Downloading $(Split-Path -Leaf $Destination)..."
        Invoke-WebRequest -Uri $Uri -OutFile $Destination
    }
    $actualHash = (Get-FileHash -LiteralPath $Destination -Algorithm SHA256).Hash
    if ($actualHash -ne $Sha256) { throw "Checksum failed for $Destination" }
}

if (-not (Test-Path -LiteralPath (Join-Path $toolsRoot 'jdk'))) {
    $jdkAssets = Invoke-RestMethod -Uri 'https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse'
    $jdkPackage = $jdkAssets[0].binary.package
    $jdkZip = Join-Path $toolsRoot 'jdk.zip'
    Get-VerifiedArchive $jdkPackage.link $jdkZip $jdkPackage.checksum
    Expand-Archive -LiteralPath $jdkZip -DestinationPath (Join-Path $toolsRoot 'jdk-extract') -Force
    $jdkDirectory = Get-ChildItem -LiteralPath (Join-Path $toolsRoot 'jdk-extract') -Directory | Select-Object -First 1
    Move-Item -LiteralPath $jdkDirectory.FullName -Destination (Join-Path $toolsRoot 'jdk')
}

if (-not (Test-Path -LiteralPath (Join-Path $toolsRoot 'gradle-8.13'))) {
    $gradleHash = (Invoke-RestMethod -Uri 'https://downloads.gradle.org/distributions/gradle-8.13-bin.zip.sha256').Trim()
    $gradleZip = Join-Path $toolsRoot 'gradle.zip'
    Get-VerifiedArchive 'https://downloads.gradle.org/distributions/gradle-8.13-bin.zip' $gradleZip $gradleHash
    Expand-Archive -LiteralPath $gradleZip -DestinationPath $toolsRoot -Force
}

$sdkRoot = Join-Path $toolsRoot 'android-sdk'
$sdkManager = Join-Path $sdkRoot 'cmdline-tools\latest\bin\sdkmanager.bat'
if (-not (Test-Path -LiteralPath $sdkManager)) {
    $sdkZip = Join-Path $toolsRoot 'commandlinetools.zip'
    Get-VerifiedArchive 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip' $sdkZip '98b565cb657b012dae6794cefc0f66ae1efb4690c699b78a614b4a6a3505b003'
    $sdkExtract = Join-Path $toolsRoot 'sdk-extract'
    Expand-Archive -LiteralPath $sdkZip -DestinationPath $sdkExtract -Force
    New-Item -ItemType Directory -Force -Path (Join-Path $sdkRoot 'cmdline-tools') | Out-Null
    Move-Item -LiteralPath (Join-Path $sdkExtract 'cmdline-tools') -Destination (Join-Path $sdkRoot 'cmdline-tools\latest')
}

$env:JAVA_HOME = Join-Path $toolsRoot 'jdk'
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.android'
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
1..20 | ForEach-Object { 'y' } | & $sdkManager "--sdk_root=$sdkRoot" --licenses *> (Join-Path $toolsRoot 'sdk-licenses.log')
if ($LASTEXITCODE -ne 0) { throw 'Android SDK license setup failed.' }
& $sdkManager "--sdk_root=$sdkRoot" 'platforms;android-36' 'build-tools;35.0.0' 'platform-tools'
if ($LASTEXITCODE -ne 0) { throw 'Android SDK installation failed.' }
$sdkProperty = $sdkRoot.Replace('\','/').Replace(':','\:')
Set-Content -LiteralPath (Join-Path $projectRoot 'local.properties') -Value "sdk.dir=$sdkProperty" -Encoding utf8
Write-Output 'Local Android toolchain is ready.'
