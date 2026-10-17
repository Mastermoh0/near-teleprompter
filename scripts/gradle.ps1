param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GradleArguments)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = Join-Path $projectRoot '.tools\jdk'
$env:ANDROID_HOME = Join-Path $projectRoot '.tools\android-sdk'
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.android'
$env:ANDROID_AVD_HOME = Join-Path $projectRoot '.android\avd'
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$buildTemp = Join-Path $projectRoot '.tools\tmp'
New-Item -ItemType Directory -Force -Path $buildTemp | Out-Null
$env:TEMP = $buildTemp
$env:TMP = $buildTemp
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=`"$buildTemp`" -Djava.io.tmpdir=`"$buildTemp`""
Push-Location $projectRoot
try {
    & (Join-Path $projectRoot '.tools\gradle-8.13\bin\gradle.bat') --console=plain @GradleArguments
    if ($LASTEXITCODE -ne 0) { throw 'Gradle task failed.' }
} finally { Pop-Location }
