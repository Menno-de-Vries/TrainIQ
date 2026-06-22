param(
    [string]$SourcePath = "",
    [string]$DestinationDir = "",
    [switch]$Force,
    [switch]$VerifyOnly,
    [switch]$HelpSamsungDownload
)

$ErrorActionPreference = "Stop"

$SamsungHealthDataSdkOverviewUrl = "https://developer.samsung.com/health/data/overview.html"
$SamsungHealthDataSdkCodelabUrl = "https://developer.samsung.com/codelab/health/steps-data.html"
$SamsungHealthDataSdkReleaseNotesUrl = "https://developer.samsung.com/health/data/release-note.html"

function Write-SamsungHealthDataSdkDownloadHelp {
    Write-Host "Samsung Health Data SDK API AAR is distributed by Samsung in its Health Data SDK/sample package."
    Write-Host "Official SDK download page: $SamsungHealthDataSdkOverviewUrl"
    Write-Host "Official steps codelab: $SamsungHealthDataSdkCodelabUrl"
    Write-Host "Official release notes: $SamsungHealthDataSdkReleaseNotesUrl"
    Write-Host "Download Samsung's Health Data SDK package from the official SDK download page, accept Samsung's SDK terms there, then run:"
    Write-Host "powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-samsung-health-data-sdk-aar.ps1 -SourcePath `"C:\path\to\downloaded-samsung-health-data-sdk-or-sample.zip`""
}

function Get-Sha256 {
    param([Parameter(Mandatory = $true)][string]$Path)

    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Test-SamsungDataSdkApiAarName {
    param([Parameter(Mandatory = $true)][string]$Name)

    return $Name -like "*samsung-health-data-api*" -and $Name -like "*.aar"
}

function Find-SamsungHealthDataSdkApiAar {
    param([Parameter(Mandatory = $true)][string]$Path)

    $resolvedPath = Resolve-Path -LiteralPath $Path
    $sourceItem = Get-Item -LiteralPath $resolvedPath.Path
    $tempDir = $null

    if ($sourceItem.PSIsContainer) {
        $searchRoot = $sourceItem.FullName
    } elseif ($sourceItem.Extension -ieq ".zip") {
        $tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("trainiq-samsung-health-data-sdk-" + [System.Guid]::NewGuid())
        New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
        Expand-Archive -LiteralPath $sourceItem.FullName -DestinationPath $tempDir -Force
        $searchRoot = $tempDir
    } elseif ($sourceItem.Extension -ieq ".aar") {
        $searchRoot = $sourceItem.DirectoryName
    } else {
        throw "SourcePath must be a Samsung Health Data SDK .zip, a directory, or a samsung-health-data-api*.aar file."
    }

    $aarFiles = if ($sourceItem.Extension -ieq ".aar") {
        @($sourceItem)
    } else {
        @(Get-ChildItem -LiteralPath $searchRoot -Recurse -File | Where-Object { $_.Extension -ieq ".aar" })
    }

    $dataApiAars = @($aarFiles | Where-Object { Test-SamsungDataSdkApiAarName -Name $_.Name })
    $legacySamsungAars = @($aarFiles | Where-Object {
        $_.Name -like "*samsung-health*" -and -not (Test-SamsungDataSdkApiAarName -Name $_.Name)
    })

    if ($dataApiAars.Count -eq 0) {
        $legacyList = if ($legacySamsungAars.Count -gt 0) {
            " Legacy/other Samsung Health AAR files ignored: " + (($legacySamsungAars | ForEach-Object { $_.Name }) -join ", ")
        } else {
            ""
        }
        throw "No samsung-health-data-api*.aar found in SourcePath.$legacyList Download Samsung's official Health Data SDK package from $SamsungHealthDataSdkOverviewUrl, or use the steps codelab sample at $SamsungHealthDataSdkCodelabUrl. See $SamsungHealthDataSdkReleaseNotesUrl for current requirements."
    }

    if ($dataApiAars.Count -gt 1) {
        throw "Multiple samsung-health-data-api*.aar files found. Pass the exact AAR file as SourcePath: $($dataApiAars.FullName -join ', ')"
    }

    return [pscustomobject]@{
        Aar = $dataApiAars[0]
        TempDir = $tempDir
    }
}

if ($HelpSamsungDownload) {
    Write-SamsungHealthDataSdkDownloadHelp
    exit 0
}

if (-not $SourcePath) {
    Write-SamsungHealthDataSdkDownloadHelp
    throw "SourcePath is required unless -HelpSamsungDownload is used."
}

$root = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
if (-not $DestinationDir) {
    $DestinationDir = Join-Path $root "app\libs"
}

$result = $null
try {
    $result = Find-SamsungHealthDataSdkApiAar -Path $SourcePath
    $sourceAar = $result.Aar
    $sourceHash = Get-Sha256 -Path $sourceAar.FullName

    if ($VerifyOnly) {
        Write-Host "Samsung Health Data SDK API AAR source verified: $($sourceAar.FullName)"
        Write-Host "SHA256: $sourceHash"
        exit 0
    }

    New-Item -ItemType Directory -Force -Path $DestinationDir | Out-Null
    $targetPath = Join-Path $DestinationDir $sourceAar.Name

    if (Test-Path -LiteralPath $targetPath) {
        $targetHash = Get-Sha256 -Path $targetPath
        if ($targetHash -ne $sourceHash -and -not $Force) {
            throw "Destination already has $($sourceAar.Name) with a different SHA256. Re-run with -Force to replace it."
        }
    }

    Copy-Item -LiteralPath $sourceAar.FullName -Destination $targetPath -Force:$Force
    $targetHashAfterCopy = Get-Sha256 -Path $targetPath
    if ($targetHashAfterCopy -ne $sourceHash) {
        throw "Copied AAR SHA256 mismatch. Source=$sourceHash Destination=$targetHashAfterCopy"
    }

    $statusPath = Join-Path $DestinationDir "samsung-health-data-sdk-aar-status.txt"
    Set-Content -LiteralPath $statusPath -Encoding UTF8 -Value @(
        "Samsung Health Data SDK API AAR installed: True",
        "Installed at: $(Get-Date -Format o)",
        "Source: $($sourceAar.FullName)",
        "Destination: $targetPath",
        "SHA256: $sourceHash",
        "Next check: .\gradlew.bat :app:checkSamsungHealthDataSdkReadiness --console=plain --no-configuration-cache",
        "Parity build: .\gradlew.bat :app:assembleSamsungHealthParityDebug --console=plain --no-configuration-cache",
        "Parity install: .\gradlew.bat :app:installSamsungHealthParityDebug --console=plain --no-configuration-cache"
    )

    Write-Host "Samsung Health Data SDK API AAR installed: $targetPath"
    Write-Host "SHA256: $sourceHash"
    Write-Host "Status: $statusPath"
} finally {
    if ($result -and $result.TempDir -and (Test-Path -LiteralPath $result.TempDir)) {
        Remove-Item -LiteralPath $result.TempDir -Recurse -Force
    }
}
