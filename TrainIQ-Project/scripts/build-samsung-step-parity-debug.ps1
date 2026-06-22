# build-samsung-step-parity-debug.ps1
param(
    [string]$SourcePath = "",
    [string]$AdbPath = "",
    [string]$Serial = "",
    [string]$OutputDir = "",
    [switch]$SkipInstall,
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
        [string]$Serial = "",
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    $adbArguments = if ($Serial) {
        @("-s", $Serial) + $Arguments
    } else {
        $Arguments
    }
    return (& $Adb @adbArguments 2>&1 | Out-String).Trim()
}

function Invoke-LoggedCommand {
    param(
        [Parameter(Mandatory = $true)][scriptblock]$Command,
        [Parameter(Mandatory = $true)][string]$LogFile,
        [Parameter(Mandatory = $true)][string]$FailureMessage
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $Command 2>&1
        $exitCode = if ($LASTEXITCODE -is [int]) { $LASTEXITCODE } else { 0 }
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    $output | Out-String | Set-Content -LiteralPath $LogFile -Encoding UTF8
    if ($exitCode -ne 0) {
        throw "$FailureMessage See $LogFile"
    }
}

function Get-ConnectedDeviceSerials {
    param([Parameter(Mandatory = $true)][string]$Adb)

    $lines = (& $Adb devices -l 2>&1 | Out-String) -split "\r?\n"
    return @(
        $lines |
            Where-Object { $_ -match "^\S+\s+device\s" } |
            ForEach-Object { ($_ -split "\s+")[0] }
    )
}

function Resolve-DeviceSerial {
    param(
        [Parameter(Mandatory = $true)][string]$Adb,
        [string]$RequestedSerial
    )

    $connected = Get-ConnectedDeviceSerials -Adb $Adb
    if ($RequestedSerial) {
        if ($connected -notcontains $RequestedSerial) {
            throw "Requested adb serial '$RequestedSerial' is not connected. Connected devices: $($connected -join ', ')"
        }
        return $RequestedSerial
    }

    if ($connected.Count -eq 0) {
        throw "No adb device is connected. Connect a physical Samsung device before Samsung Health step parity testing."
    }
    if ($connected.Count -gt 1) {
        throw "Multiple adb devices are connected. Pass -Serial to avoid installing/testing on the wrong device: $($connected -join ', ')"
    }
    return $connected[0]
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

function Assert-PhysicalSamsungTarget {
    param(
        [Parameter(Mandatory = $true)][string]$Adb,
        [Parameter(Mandatory = $true)][string]$Serial,
        [Parameter(Mandatory = $true)][string]$OutputDir
    )

    $manufacturer = Invoke-Adb -Adb $Adb -Serial $Serial -Arguments @("shell", "getprop", "ro.product.manufacturer")
    $model = Invoke-Adb -Adb $Adb -Serial $Serial -Arguments @("shell", "getprop", "ro.product.model")
    $androidVersion = Invoke-Adb -Adb $Adb -Serial $Serial -Arguments @("shell", "getprop", "ro.build.version.release")
    $emulatorFlag = Invoke-Adb -Adb $Adb -Serial $Serial -Arguments @("shell", "getprop", "ro.kernel.qemu")
    $androidMajor = 0
    $androidMajorKnown = [int]::TryParse((($androidVersion.Trim()) -split "\.")[0], [ref]$androidMajor)
    $isSamsung = $manufacturer.Trim() -match "(?i)samsung"
    $isEmulator = $emulatorFlag.Trim() -eq "1" -or $model.Trim() -match "(?i)emulator|sdk_gphone|generic"
    $isAndroid10OrLater = $androidMajorKnown -and $androidMajor -ge 10

    Set-Content -LiteralPath (Join-Path $OutputDir "physical-device-gate.txt") -Encoding UTF8 -Value @(
        "Physical Samsung device gate",
        "Serial: $Serial",
        "Manufacturer: $($manufacturer.Trim())",
        "Model: $($model.Trim())",
        "Android version: $($androidVersion.Trim())",
        "Android 10 or later: $isAndroid10OrLater",
        "ro.kernel.qemu: $($emulatorFlag.Trim())",
        "Samsung manufacturer detected: $isSamsung",
        "Emulator detected: $isEmulator",
        "Physical Samsung device ready: $($isSamsung -and -not $isEmulator -and $isAndroid10OrLater)",
        "Samsung Health Data SDK emulator support: not supported"
    )

    if (-not $isSamsung -or $isEmulator -or -not $isAndroid10OrLater) {
        throw "Physical Samsung device required for Samsung Health Data SDK parity. See $(Join-Path $OutputDir "physical-device-gate.txt")"
    }
}

function Assert-SamsungHealthRuntime {
    param(
        [Parameter(Mandatory = $true)][string]$Adb,
        [Parameter(Mandatory = $true)][string]$Serial,
        [Parameter(Mandatory = $true)][string]$OutputDir
    )

    $packageName = "com.sec.android.app.shealth"
    $packagePath = Invoke-Adb -Adb $Adb -Serial $Serial -Arguments @("shell", "pm", "path", $packageName)
    $packageDump = Invoke-Adb -Adb $Adb -Serial $Serial -Arguments @("shell", "dumpsys", "package", $packageName)
    $installed = $packagePath -match "package:" -or $packageDump -match "Package \[$packageName\]"
    $versionNameLine = (($packageDump -split "\r?\n") | Where-Object { $_ -match "versionName=" } | Select-Object -First 1)
    if (-not $versionNameLine) {
        $versionNameLine = "versionName=(not found)"
    }
    $versionName = ($versionNameLine -replace "^.*versionName=", "").Trim()
    $versionReady = $installed -and (Test-VersionAtLeast -VersionName $versionName -MinimumVersionName "6.30.2")

    Set-Content -LiteralPath (Join-Path $OutputDir "samsung-health-runtime-gate.txt") -Encoding UTF8 -Value @(
        "Samsung Health runtime gate",
        "Samsung Health package: $packageName",
        "Samsung Health installed: $installed",
        "Samsung Health version 6.30.2 or later: $versionReady",
        "Samsung Health $($versionNameLine.Trim())"
    )

    if (-not $installed) {
        throw "Samsung Health package com.sec.android.app.shealth is not installed on the selected device."
    }
    if (-not $versionReady) {
        throw "Samsung Health version 6.30.2 or later is required for Samsung Health Data SDK parity. See $(Join-Path $OutputDir "samsung-health-runtime-gate.txt")"
    }
}

$root = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
if (-not $OutputDir) {
    $stamp = Get-Date -Format "yyyy-MM-dd-HHmmss"
    $OutputDir = Join-Path $root ".codex\device-qa\samsung-step-parity-build-$stamp"
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$installAarScript = Join-Path $PSScriptRoot "install-samsung-health-data-sdk-aar.ps1"
$collectEvidenceScript = Join-Path $PSScriptRoot "collect-samsung-step-parity-evidence.ps1"
if ($SourcePath) {
    Invoke-LoggedCommand `
        -Command { & powershell -NoProfile -ExecutionPolicy Bypass -File $installAarScript -SourcePath $SourcePath } `
        -LogFile (Join-Path $OutputDir "install-samsung-health-data-sdk-aar.txt") `
        -FailureMessage "Samsung Health Data SDK API AAR install failed."
}

Push-Location $root
try {
    Invoke-LoggedCommand `
        -Command { .\gradlew.bat :app:checkSamsungHealthDataSdkReadiness --console=plain --no-configuration-cache } `
        -LogFile (Join-Path $OutputDir "gradle-samsung-health-data-sdk-readiness.txt") `
        -FailureMessage "Samsung Health Data SDK API AAR readiness failed."
} finally {
    Pop-Location
}

$adb = Resolve-Adb -RequestedPath $AdbPath
$selectedSerial = Resolve-DeviceSerial -Adb $adb -RequestedSerial $Serial
Set-Content -LiteralPath (Join-Path $OutputDir "adb-selected-device.txt") -Encoding UTF8 -Value @(
    "Selected adb serial: $selectedSerial",
    "Physical Samsung device is required; Samsung Health Data SDK is not supported on emulators."
)
Assert-PhysicalSamsungTarget -Adb $adb -Serial $selectedSerial -OutputDir $OutputDir
Assert-SamsungHealthRuntime -Adb $adb -Serial $selectedSerial -OutputDir $OutputDir

Push-Location $root
try {
    Invoke-LoggedCommand `
        -Command { .\gradlew.bat :app:assembleSamsungHealthParityDebug --console=plain --no-configuration-cache } `
        -LogFile (Join-Path $OutputDir "gradle-assemble-debug-samsung-parity.txt") `
        -FailureMessage "Samsung Health parity assembleDebug failed."

    if (-not $SkipInstall) {
        $env:ANDROID_SERIAL = $selectedSerial
        Invoke-LoggedCommand `
            -Command { .\gradlew.bat :app:installSamsungHealthParityDebug --console=plain --no-configuration-cache } `
            -LogFile (Join-Path $OutputDir "gradle-install-debug-samsung-parity.txt") `
            -FailureMessage "Samsung Health parity installDebug failed."
    }
} finally {
    Remove-Item Env:\ANDROID_SERIAL -ErrorAction SilentlyContinue
    Pop-Location
}

$previousAndroidSerial = $env:ANDROID_SERIAL
$env:ANDROID_SERIAL = $selectedSerial
try {
    $collectArguments = @(
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        $collectEvidenceScript,
        "-AdbPath",
        $adb,
        "-OutputDir",
        (Join-Path $OutputDir "evidence"),
        "-SamsungHealthAllSteps",
        $SamsungHealthAllSteps,
        "-TrainIqDisplayedSteps",
        $TrainIqDisplayedSteps
    )
    Invoke-LoggedCommand `
        -Command { & powershell @collectArguments } `
        -LogFile (Join-Path $OutputDir "collect-samsung-step-parity-evidence.txt") `
        -FailureMessage "Samsung step parity evidence collection failed."
} finally {
    if ($previousAndroidSerial) {
        $env:ANDROID_SERIAL = $previousAndroidSerial
    } else {
        Remove-Item Env:\ANDROID_SERIAL -ErrorAction SilentlyContinue
    }
}

Set-Content -LiteralPath (Join-Path $OutputDir "next-steps.txt") -Encoding UTF8 -Value @(
    "Samsung Health parity build helper completed.",
    "Open TrainIQ Settings on the selected physical Samsung device.",
    "If needed, tap Samsung toegang geven and grant Samsung Health steps.",
    "Tap Vernieuwen, then compare Samsung Health All steps with TrainIQ.",
    "Use Diagnose kopieren and paste into evidence\samsung-step-diagnosis.txt.",
    "Output: $OutputDir"
)
Write-Host "Samsung Health parity build/evidence output written to $OutputDir"
