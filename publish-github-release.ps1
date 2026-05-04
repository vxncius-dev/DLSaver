param(
    [string]$Repo = "vxncius-dev/DLSaver",
    [string]$Branch = "main",
    [string]$ManifestPath = "releases/android/manifest.json",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$gradleFile = Join-Path $rootDir "app\build.gradle.kts"
$localManifestFile = Join-Path $rootDir "manifest.json"
$buildScript = Join-Path $rootDir "build-release.ps1"
$releaseAbi = "arm64-v8a"

function Read-GradleValue {
    param(
        [string]$Content,
        [string]$Name
    )

    $match = [regex]::Match($Content, "$Name\s*=\s*`"?([^`"\r\n]+)`"?")
    if (-not $match.Success) {
        throw "Nao foi possivel identificar $Name em app/build.gradle.kts."
    }
    return $match.Groups[1].Value.Trim()
}

function Invoke-GhJson {
    param(
        [string[]]$Arguments
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & gh @Arguments 2>$null
        if ($LASTEXITCODE -ne 0) {
            return $null
        }
        return ($output -join "`n") | ConvertFrom-Json
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Invoke-GhChecked {
    param(
        [string[]]$Arguments
    )

    & gh @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "gh falhou: gh $($Arguments -join ' ')"
    }
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI (gh) nao encontrado. Instale/login com gh auth login antes de publicar."
}

if (-not (Test-Path $gradleFile)) {
    throw "Nao encontrei app/build.gradle.kts."
}

$gradleContent = Get-Content -Path $gradleFile -Raw
$versionCode = [int](Read-GradleValue -Content $gradleContent -Name "versionCode")
$versionName = Read-GradleValue -Content $gradleContent -Name "versionName"
$tag = "v$versionName"
$assetName = "dlsaver_v${versionName}_${releaseAbi}.apk"
$apkFile = Join-Path $rootDir "releases\$assetName"

Push-Location $rootDir
try {
    if (-not $SkipBuild) {
        & $buildScript
    }

    if (-not (Test-Path $apkFile)) {
        throw "Nao encontrei o APK esperado: $apkFile"
    }

    $apkHash = (Get-FileHash -Algorithm SHA256 -Path $apkFile).Hash.ToLowerInvariant()
    $apkSize = (Get-Item $apkFile).Length
    $publishedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    $downloadUrl = "https://github.com/$Repo/releases/download/$tag/$assetName"

    $remoteManifestApiPath = "repos/$Repo/contents/${ManifestPath}?ref=$Branch"
    $remoteManifest = Invoke-GhJson -Arguments @(
        "api",
        $remoteManifestApiPath
    )

    $existingReleases = @()
    $remoteSha = $null
    if ($remoteManifest -ne $null) {
        $remoteSha = $remoteManifest.sha
        $remoteText = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String(($remoteManifest.content -replace "\s", "")))
        $existing = $remoteText | ConvertFrom-Json
        if ($existing.releases) {
            $existingReleases = @($existing.releases | Where-Object { [int]$_.versionCode -ne $versionCode })
        }
    } elseif (Test-Path $localManifestFile) {
        $existing = Get-Content -Path $localManifestFile -Raw | ConvertFrom-Json
        if ($existing.releases) {
            $existingReleases = @($existing.releases | Where-Object { [int]$_.versionCode -ne $versionCode })
        }
    }

    $notes = "Versao de lancamento"
    if (Test-Path $localManifestFile) {
        $localManifest = Get-Content -Path $localManifestFile -Raw | ConvertFrom-Json
        $localCurrent = @($localManifest.releases | Where-Object { [int]$_.versionCode -eq $versionCode }) | Select-Object -First 1
        if ($localCurrent -and -not [string]::IsNullOrWhiteSpace($localCurrent.notes)) {
            $notes = $localCurrent.notes
        }
    }

    $newRelease = [ordered]@{
        versionCode = $versionCode
        versionName = $versionName
        apkPath = $downloadUrl
        apkUrl = $downloadUrl
        notes = $notes
        publishedAt = $publishedAt
        sha256 = $apkHash
        sizeBytes = $apkSize
        abi = $releaseAbi
    }

    $manifest = [ordered]@{
        appId = "com.vxncius.dlsaver"
        releases = @($newRelease) + @($existingReleases | Sort-Object -Property versionCode -Descending)
    }
    $manifestText = ($manifest | ConvertTo-Json -Depth 8) + "`n"
    Set-Content -Path $localManifestFile -Value $manifestText -Encoding UTF8

    $releaseInfo = Invoke-GhJson -Arguments @("release", "view", $tag, "--repo", $Repo, "--json", "tagName,assets")
    $releaseExists = $null -ne $releaseInfo
    if ($releaseExists) {
        $existingAsset = @($releaseInfo.assets | Where-Object { $_.name -eq $assetName }) | Select-Object -First 1
        $assetDigest = "sha256:$apkHash"
        if (-not $existingAsset -or [int64]$existingAsset.size -ne $apkSize -or $existingAsset.digest -ne $assetDigest) {
            Invoke-GhChecked -Arguments @("release", "upload", $tag, $apkFile, "--repo", $Repo, "--clobber")
        } else {
            Write-Host "APK ja existe na release com o mesmo hash; pulando upload."
        }
        Invoke-GhChecked -Arguments @("release", "edit", $tag, "--repo", $Repo, "--title", "DLSaver $tag", "--notes", $notes, "--latest")
    } else {
        Invoke-GhChecked -Arguments @("release", "create", $tag, $apkFile, "--repo", $Repo, "--title", "DLSaver $tag", "--notes", $notes, "--latest")
    }

    $manifestContent = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($manifestText))
    $putArgs = @(
        "api",
        "--method", "PUT",
        "repos/$Repo/contents/$ManifestPath",
        "--field", "message=Update Android manifest for $tag",
        "--field", "content=$manifestContent",
        "--field", "branch=$Branch"
    )
    if ($remoteSha) {
        $putArgs += @("--field", "sha=$remoteSha")
    }
    Invoke-GhChecked -Arguments $putArgs

    Write-Host "Publicado: $tag"
    Write-Host "APK: $downloadUrl"
    Write-Host "Manifest remoto: https://github.com/$Repo/blob/$Branch/$ManifestPath"
}
finally {
    Pop-Location
}
