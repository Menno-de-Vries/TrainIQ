param(
    [string]$AdbPath = "",
    [string]$OutputDir = "",
    [switch]$InstallDebug,
    [int]$SamsungHealthAllSteps = -1,
    [int]$TrainIqDisplayedSteps = -1
)

$ErrorActionPreference = "Stop"

function Resolve-Adb {
    param([string]$RequestedPath)

    if ($RequestedPath -and (Test-Path -LiteralPath $RequestedPath)) {
        return (Resolve-Path -LiteralPath $RequestedPath).Path
    }

    $fromPath = Get-Command adb -ErrorAction SilentlyContinue
    if ($fromPath) {
        return $fromPath.Source
    }

    $androidHome = $env:ANDROID_HOME
    if (-not $androidHome) {
        $androidHome = $env:ANDROID_SDK_ROOT
    }
    if ($androidHome) {
        $candidate = Join-Path $androidHome "platform-tools\adb.exe"
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    $localCandidate = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path -LiteralPath $localCandidate) {
        return $localCandidate
    }

    throw "adb was not found. Pass -AdbPath or set ANDROID_HOME/ANDROID_SDK_ROOT."
}

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)][string]$Adb,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$OutFile
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $text = & $Adb @Arguments 2>&1 | Out-String
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    Set-Content -LiteralPath $OutFile -Value $text.TrimEnd() -Encoding UTF8
    return $text
}

function Dump-Ui {
    param(
        [Parameter(Mandatory = $true)][string]$Adb,
        [Parameter(Mandatory = $true)][string]$OutFile
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $xml = & $Adb exec-out uiautomator dump /dev/tty 2>&1 | Out-String
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    Set-Content -LiteralPath $OutFile -Value $xml.TrimEnd() -Encoding UTF8
}

function Write-Lines {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [AllowEmptyString()][string[]]$Lines
    )

    Set-Content -LiteralPath $Path -Value $Lines -Encoding UTF8
}

function Get-ParityResultLines {
    param(
        [int]$SamsungHealthAllSteps,
        [int]$TrainIqDisplayedSteps
    )

    $lines = @(
        "Samsung Health All steps provided: $($SamsungHealthAllSteps -ge 0)",
        "TrainIQ displayed steps provided: $($TrainIqDisplayedSteps -ge 0)"
    )
    if ($SamsungHealthAllSteps -ge 0 -and $TrainIqDisplayedSteps -ge 0) {
        $difference = $SamsungHealthAllSteps - $TrainIqDisplayedSteps
        $absoluteDifference = [Math]::Abs($difference)
        $ratio = if ($SamsungHealthAllSteps -gt 0) {
            [Math]::Round(($TrainIqDisplayedSteps / [double]$SamsungHealthAllSteps) * 100, 1)
        } else {
            100
        }
        $status = if ($absoluteDifference -eq 0) {
            "MATCH"
        } elseif ($TrainIqDisplayedSteps -lt $SamsungHealthAllSteps) {
            "TRAINIQ_UNDER_REPORTS"
        } else {
            "TRAINIQ_OVER_REPORTS"
        }
        $lines += @(
            "Samsung Health All steps: $SamsungHealthAllSteps",
            "TrainIQ displayed steps: $TrainIqDisplayedSteps",
            "Difference Samsung minus TrainIQ: $difference",
            "Absolute difference: $absoluteDifference",
            "TrainIQ percent of Samsung Health: $ratio%",
            "Parity status: $status"
        )
    } else {
        $lines += @(
            "Difference Samsung minus TrainIQ: not captured",
            "Parity status: NOT_CAPTURED"
        )
    }
    return $lines
}

function Get-DeviceReadinessLines {
    param(
        [string]$Manufacturer,
        [string]$Model,
        [string]$AndroidVersion,
        [string]$EmulatorFlag
    )

    $manufacturerClean = $Manufacturer.Trim()
    $modelClean = $Model.Trim()
    $androidVersionClean = $AndroidVersion.Trim()
    $emulatorFlagClean = $EmulatorFlag.Trim()
    $androidMajorText = ($androidVersionClean -split "\.")[0]
    $androidMajor = 0
    $androidMajorKnown = [int]::TryParse($androidMajorText, [ref]$androidMajor)
    $android10OrLater = $androidMajorKnown -and $androidMajor -ge 10
    $isSamsung = $manufacturerClean -match "(?i)samsung"
    $isEmulator = $emulatorFlagClean -eq "1" -or $modelClean -match "(?i)emulator|sdk_gphone|generic"
    $physicalSamsungLikely = $isSamsung -and -not $isEmulator
    $deviceMeetsSamsungDataSdkRuntime = $physicalSamsungLikely -and $android10OrLater

    return @(
        "Manufacturer: $manufacturerClean",
        "Model: $modelClean",
        "Android version: $androidVersionClean",
        "Android major version parsed: $androidMajorText",
        "Android 10 or later: $android10OrLater",
        "ro.kernel.qemu: $emulatorFlagClean",
        "Samsung manufacturer detected: $isSamsung",
        "Emulator detected: $isEmulator",
        "Physical Samsung device likely: $physicalSamsungLikely",
        "Device meets Samsung Health Data SDK runtime target: $deviceMeetsSamsungDataSdkRuntime",
        "Samsung Health Data SDK emulator support: not supported"
    )
}

function Test-VersionAtLeast {
    param(
        [string]$VersionName,
        [string]$MinimumVersionName
    )

    $versionParts = @($VersionName -split "[\._-]" | ForEach-Object {
        $digits = [regex]::Match($_, "^\d+").Value
        if ($digits) { [int]$digits }
    })
    $minimumParts = @($MinimumVersionName -split "[\._-]" | ForEach-Object {
        $digits = [regex]::Match($_, "^\d+").Value
        if ($digits) { [int]$digits }
    })
    if ($versionParts.Count -eq 0 -or $minimumParts.Count -eq 0) {
        return $false
    }
    $maxLength = [Math]::Max($versionParts.Count, $minimumParts.Count)
    for ($index = 0; $index -lt $maxLength; $index++) {
        $versionPart = if ($index -lt $versionParts.Count) { $versionParts[$index] } else { 0 }
        $minimumPart = if ($index -lt $minimumParts.Count) { $minimumParts[$index] } else { 0 }
        if ($versionPart -ne $minimumPart) {
            return $versionPart -gt $minimumPart
        }
    }
    return $true
}

function Get-AcceptanceGateLines {
    param(
        [bool]$DeviceMeetsRuntimeTarget,
        [bool]$SamsungDataApiAarPresent,
        [bool]$SamsungHealthInstalled,
        [bool]$SamsungHealthVersionReady,
        [bool]$StepValuesCaptured,
        [bool]$StepValuesMatch
    )

    $exactParityProofReady = $DeviceMeetsRuntimeTarget -and
        $SamsungDataApiAarPresent -and
        $SamsungHealthInstalled -and
        $SamsungHealthVersionReady -and
        $StepValuesCaptured -and
        $StepValuesMatch

    return @(
        "Physical Samsung Android 10+ target: $DeviceMeetsRuntimeTarget",
        "Samsung Health Data SDK API AAR present: $SamsungDataApiAarPresent",
        "Samsung Health installed: $SamsungHealthInstalled",
        "Samsung Health version 6.30.2 or later: $SamsungHealthVersionReady",
        "Samsung and TrainIQ step values captured: $StepValuesCaptured",
        "Samsung and TrainIQ step values match: $StepValuesMatch",
        "Exact Samsung Health All steps parity proof ready: $exactParityProofReady"
    )
}

$root = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
if (-not $OutputDir) {
    $stamp = Get-Date -Format "yyyy-MM-dd-HHmmss"
    $OutputDir = Join-Path $root ".codex\device-qa\samsung-step-parity-$stamp"
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$adb = Resolve-Adb -RequestedPath $AdbPath
$appId = "com.trainiq"
$mainActivity = "com.trainiq/.MainActivity"
$samsungHealthPackage = "com.sec.android.app.shealth"
$libsDir = Join-Path $root "app\libs"
$samsungDataApiAars = @()
$legacySamsungAars = @()
if (Test-Path -LiteralPath $libsDir) {
    $samsungAars = Get-ChildItem -Path $libsDir -File |
        Where-Object { $_.Extension -ieq ".aar" }
    $samsungDataApiAars = $samsungAars |
        Where-Object { $_.Name -like "*samsung-health-data-api*" }
    $legacySamsungAars = $samsungAars |
        Where-Object { $_.Name -like "*samsung-health*" -and $_.Name -notlike "*samsung-health-data-api*" }
}
$samsungDataApiAarLines = @($samsungDataApiAars | ForEach-Object { $_.FullName })
if ($samsungDataApiAarLines.Count -eq 0) {
    $samsungDataApiAarLines = @("(none)")
}
$legacySamsungAarLines = @($legacySamsungAars | ForEach-Object { $_.FullName })
if ($legacySamsungAarLines.Count -eq 0) {
    $legacySamsungAarLines = @("(none)")
}

if ($InstallDebug) {
    Push-Location $root
    try {
        .\gradlew.bat :app:installDebug --console=plain --no-configuration-cache |
            Set-Content -LiteralPath (Join-Path $OutputDir "gradle-install-debug.txt") -Encoding UTF8
    } finally {
        Pop-Location
    }
}

Invoke-Adb -Adb $adb -Arguments @("devices", "-l") -OutFile (Join-Path $OutputDir "adb-devices.txt") | Out-Null
$deviceManufacturer = Invoke-Adb -Adb $adb -Arguments @("shell", "getprop", "ro.product.manufacturer") -OutFile (Join-Path $OutputDir "device-manufacturer.txt")
$deviceModel = Invoke-Adb -Adb $adb -Arguments @("shell", "getprop", "ro.product.model") -OutFile (Join-Path $OutputDir "device-model.txt")
$deviceAndroidVersion = Invoke-Adb -Adb $adb -Arguments @("shell", "getprop", "ro.build.version.release") -OutFile (Join-Path $OutputDir "device-android-version.txt")
$deviceEmulatorFlag = Invoke-Adb -Adb $adb -Arguments @("shell", "getprop", "ro.kernel.qemu") -OutFile (Join-Path $OutputDir "device-emulator-flag.txt")
$deviceManufacturerClean = $deviceManufacturer.Trim()
$deviceModelClean = $deviceModel.Trim()
$deviceAndroidVersionClean = $deviceAndroidVersion.Trim()
$deviceEmulatorFlagClean = $deviceEmulatorFlag.Trim()
$deviceAndroidMajorText = ($deviceAndroidVersionClean -split "\.")[0]
$deviceAndroidMajor = 0
$deviceAndroidMajorKnown = [int]::TryParse($deviceAndroidMajorText, [ref]$deviceAndroidMajor)
$deviceAndroid10OrLater = $deviceAndroidMajorKnown -and $deviceAndroidMajor -ge 10
$deviceIsSamsung = $deviceManufacturerClean -match "(?i)samsung"
$deviceIsEmulator = $deviceEmulatorFlagClean -eq "1" -or $deviceModelClean -match "(?i)emulator|sdk_gphone|generic"
$deviceMeetsSamsungDataSdkRuntime = $deviceIsSamsung -and -not $deviceIsEmulator -and $deviceAndroid10OrLater
Write-Lines -Path (Join-Path $OutputDir "device-readiness.txt") -Lines (
    Get-DeviceReadinessLines `
        -Manufacturer $deviceManufacturer `
        -Model $deviceModel `
        -AndroidVersion $deviceAndroidVersion `
        -EmulatorFlag $deviceEmulatorFlag
)
$samsungHealthPackagePath = Invoke-Adb -Adb $adb -Arguments @("shell", "pm", "path", $samsungHealthPackage) -OutFile (Join-Path $OutputDir "samsung-health-package-path.txt")
$samsungHealthPackageDump = Invoke-Adb -Adb $adb -Arguments @("shell", "dumpsys", "package", $samsungHealthPackage) -OutFile (Join-Path $OutputDir "samsung-health-package.txt")
Invoke-Adb -Adb $adb -Arguments @("shell", "dumpsys", "package", $appId) -OutFile (Join-Path $OutputDir "trainiq-package.txt") | Out-Null

$samsungHealthInstalled = $samsungHealthPackagePath -match "package:" -or $samsungHealthPackageDump -match "Package \[$samsungHealthPackage\]"
$samsungHealthDumpLines = $samsungHealthPackageDump -split "\r?\n"
$samsungHealthVersionNameLine = ($samsungHealthDumpLines | Where-Object { $_ -match "versionName=" } | Select-Object -First 1)
$samsungHealthVersionCodeLine = ($samsungHealthDumpLines | Where-Object { $_ -match "versionCode=" } | Select-Object -First 1)
if (-not $samsungHealthVersionNameLine) {
    $samsungHealthVersionNameLine = "versionName=(not found)"
}
if (-not $samsungHealthVersionCodeLine) {
    $samsungHealthVersionCodeLine = "versionCode=(not found)"
}
$samsungHealthVersionNameLine = $samsungHealthVersionNameLine.Trim()
$samsungHealthVersionCodeLine = $samsungHealthVersionCodeLine.Trim()
$samsungHealthVersionName = ($samsungHealthVersionNameLine -replace "^.*versionName=", "").Trim()
$samsungHealthVersionReady = $samsungHealthInstalled -and (
    Test-VersionAtLeast -VersionName $samsungHealthVersionName -MinimumVersionName "6.30.2"
)

Write-Lines -Path (Join-Path $OutputDir "samsung-health-readiness.txt") -Lines @(
    "Samsung Health package installed: $samsungHealthInstalled",
    "Samsung Health required for Data SDK: 6.30.2 or later",
    "Samsung Health package: $samsungHealthPackage",
    "Samsung Health $samsungHealthVersionNameLine",
    "Samsung Health $samsungHealthVersionCodeLine"
)

Write-Lines -Path (Join-Path $OutputDir "samsung-aar-status.txt") -Lines (
    @(
    "app/libs Samsung Health Data SDK API AAR present: $($samsungDataApiAars.Count -gt 0)",
    "Matched Data SDK API files:"
    ) +
    $samsungDataApiAarLines +
    @(
    "",
    "Legacy/other Samsung Health AAR files ignored for direct Data SDK readiness:"
    ) +
    $legacySamsungAarLines
)

$stepValuesCaptured = $SamsungHealthAllSteps -ge 0 -and $TrainIqDisplayedSteps -ge 0
$stepValuesMatch = $stepValuesCaptured -and $SamsungHealthAllSteps -eq $TrainIqDisplayedSteps
Write-Lines -Path (Join-Path $OutputDir "acceptance-gates.txt") -Lines (
    Get-AcceptanceGateLines `
        -DeviceMeetsRuntimeTarget $deviceMeetsSamsungDataSdkRuntime `
        -SamsungDataApiAarPresent ($samsungDataApiAars.Count -gt 0) `
        -SamsungHealthInstalled $samsungHealthInstalled `
        -SamsungHealthVersionReady $samsungHealthVersionReady `
        -StepValuesCaptured $stepValuesCaptured `
        -StepValuesMatch $stepValuesMatch
)

Invoke-Adb -Adb $adb -Arguments @("logcat", "-c") -OutFile (Join-Path $OutputDir "logcat-clear.txt") | Out-Null
Invoke-Adb -Adb $adb -Arguments @("shell", "am", "force-stop", $appId) -OutFile (Join-Path $OutputDir "force-stop-before-main.txt") | Out-Null
Invoke-Adb -Adb $adb -Arguments @("shell", "am", "start", "-W", "-n", $mainActivity) -OutFile (Join-Path $OutputDir "launch-main.txt") | Out-Null
Start-Sleep -Seconds 2
Dump-Ui -Adb $adb -OutFile (Join-Path $OutputDir "main.xml")

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
try {
    $crashLog = & $adb logcat -d -t 500 2>&1 | Select-String -Pattern "FATAL EXCEPTION|ANR in com.trainiq|E AndroidRuntime.*com.trainiq|Process: com.trainiq"
} finally {
    $ErrorActionPreference = $previousErrorActionPreference
}
$logcatCrashSlice = Join-Path $OutputDir "logcat-crash-slice.txt"
$crashLog | ForEach-Object { $_.Line } | Set-Content -LiteralPath $logcatCrashSlice -Encoding UTF8
if (-not (Test-Path -LiteralPath $logcatCrashSlice)) {
    New-Item -ItemType File -Path $logcatCrashSlice | Out-Null
}

$manualComparison = @(
    "# Samsung Health Step Parity Manual Check",
    "",
    "Use the installed build on a physical Samsung phone.",
    "",
    "1. Open Samsung Health and note All steps for the same local day.",
    "2. Open TrainIQ > Settings.",
    "3. If visible, tap Samsung toegang geven and grant Samsung Health steps.",
    "4. Tap Vernieuwen.",
    "5. Tap Diagnose kopieren and paste the copied text into samsung-step-diagnosis.txt.",
    "6. Fill the table below at least three times during the day after Samsung Health Sync now.",
    "",
    "| Time | Samsung Health All steps | TrainIQ shown steps | Difference | Samsung direct status | Notes |",
    "| --- | ---: | ---: | ---: | --- | --- |"
)
if ($SamsungHealthAllSteps -ge 0 -or $TrainIqDisplayedSteps -ge 0) {
    $difference = if ($SamsungHealthAllSteps -ge 0 -and $TrainIqDisplayedSteps -ge 0) {
        $SamsungHealthAllSteps - $TrainIqDisplayedSteps
    } else {
        ""
    }
    $manualComparison += "| $(Get-Date -Format "HH:mm") | $SamsungHealthAllSteps | $TrainIqDisplayedSteps | $difference | captured-by-script | initial CLI values |"
}
Write-Lines -Path (Join-Path $OutputDir "manual-comparison.md") -Lines $manualComparison
Write-Lines -Path (Join-Path $OutputDir "parity-result.txt") -Lines (
    Get-ParityResultLines -SamsungHealthAllSteps $SamsungHealthAllSteps -TrainIqDisplayedSteps $TrainIqDisplayedSteps
)
New-Item -ItemType File -Force -Path (Join-Path $OutputDir "samsung-step-diagnosis.txt") | Out-Null

$summary = @(
    "# Samsung Step Parity Evidence Summary",
    "",
    "- Output: $OutputDir",
    "- Samsung Health package: $samsungHealthPackage",
    "- Device readiness: device-readiness.txt",
    "- Samsung Health installed: $samsungHealthInstalled",
    "- Samsung Health version evidence: samsung-health-readiness.txt",
    "- Samsung/TrainIQ parity result: parity-result.txt",
    "- Acceptance gates: acceptance-gates.txt",
    "- Samsung Health Data SDK API AAR present locally: $($samsungDataApiAars.Count -gt 0)",
    "- Legacy/other Samsung Health AAR count ignored for direct Data SDK readiness: $($legacySamsungAars.Count)",
    "- TrainIQ package: $appId",
    "",
    "PASS criteria:",
    "- Physical Samsung device is connected, not only an emulator.",
    "- Samsung Health is installed and has synced All steps.",
    "- Samsung Health Data SDK API AAR is present under app/libs for direct SDK parity.",
    "- Settings diagnosis shows either a direct Samsung total matching Samsung Health All steps or a clear unavailable reason.",
    "- logcat-crash-slice.txt is empty."
)
Write-Lines -Path (Join-Path $OutputDir "summary.md") -Lines $summary

Write-Host "Samsung step parity evidence written to $OutputDir"
