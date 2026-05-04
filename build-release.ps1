$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$gradleFile = Join-Path $rootDir "app\build.gradle.kts"
$apkOutputDir = Join-Path $rootDir "app\build\outputs\apk\release"
$releasesDir = Join-Path $rootDir "releases"
$gradleWrapperBat = Join-Path $rootDir "gradlew.bat"
$gradleWrapperShell = Join-Path $rootDir "gradlew"
$releaseAbi = "arm64-v8a"

if (-not (Test-Path $gradleFile)) {
    throw "Nao encontrei o arquivo app/build.gradle.kts."
}

$gradleContent = Get-Content -Path $gradleFile -Raw
$versionMatch = [regex]::Match($gradleContent, 'versionName\s*=\s*"([^"]+)"')

if (-not $versionMatch.Success) {
    throw "Nao foi possivel identificar o versionName em app/build.gradle.kts."
}

$versionName = $versionMatch.Groups[1].Value

Push-Location $rootDir
try {
    if (Test-Path $gradleWrapperBat) {
        & $gradleWrapperBat assembleRelease
    } elseif (Test-Path $gradleWrapperShell) {
        & $gradleWrapperShell assembleRelease
    } else {
        gradle assembleRelease
    }

    if (-not (Test-Path $apkOutputDir)) {
        throw "Nao encontrei a pasta de saida do APK de release."
    }

    $apkFile = Get-ChildItem -Path $apkOutputDir -Filter "*-release.apk" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

    if (-not $apkFile) {
        throw "Nenhum APK de release foi encontrado em $apkOutputDir."
    }

    New-Item -ItemType Directory -Path $releasesDir -Force | Out-Null

    $destination = Join-Path $releasesDir "dlsaver_v${versionName}_${releaseAbi}.apk"
    Copy-Item -Path $apkFile.FullName -Destination $destination -Force

    Write-Host "APK copiado para: $destination"
}
finally {
    Pop-Location
}
