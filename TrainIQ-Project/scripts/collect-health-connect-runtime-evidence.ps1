param(
    [string]$AdbPath = "",
    [string]$OutputDir = "",
    [switch]$InstallDebug,
    [switch]$MutablePermissionProfileConfirmed
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

$root = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
if (-not $OutputDir) {
    $stamp = Get-Date -Format "yyyy-MM-dd-HHmmss"
    $OutputDir = Join-Path $root ".codex\device-qa\health-connect-runtime-$stamp"
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$adb = Resolve-Adb -RequestedPath $AdbPath
$appId = "com.trainiq"
$mainActivity = "com.trainiq/.MainActivity"
$rationaleActivity = "com.trainiq/.core.health.HealthConnectPermissionsRationaleActivity"
$healthConnectSettingsAction = "android.health.connect.action.MANAGE_HEALTH_PERMISSIONS"

if ($InstallDebug) {
    Push-Location $root
    try {
        .\gradlew.bat :app:installDebug --console=plain --no-configuration-cache |
            Set-Content -LiteralPath (Join-Path $OutputDir "gradle-install-debug.txt") -Encoding UTF8
    } finally {
        Pop-Location
    }
}

Invoke-Adb -Adb $adb -Arguments @("devices") -OutFile (Join-Path $OutputDir "adb-devices.txt") | Out-Null
Invoke-Adb -Adb $adb -Arguments @("shell", "pm", "list", "packages") -OutFile (Join-Path $OutputDir "packages-all.txt") | Out-Null
Select-String -Path (Join-Path $OutputDir "packages-all.txt") -Pattern "health|trainiq|vending" |
    ForEach-Object { $_.Line } |
    Set-Content -LiteralPath (Join-Path $OutputDir "health-packages.txt") -Encoding UTF8

Invoke-Adb -Adb $adb -Arguments @("shell", "dumpsys", "package", $appId) -OutFile (Join-Path $OutputDir "trainiq-package.txt") | Out-Null
Select-String -Path (Join-Path $OutputDir "trainiq-package.txt") -Pattern "android.permission.health|granted=" |
    ForEach-Object { $_.Line.Trim() } |
    Set-Content -LiteralPath (Join-Path $OutputDir "trainiq-health-permissions.txt") -Encoding UTF8
if (-not (Test-Path -LiteralPath (Join-Path $OutputDir "trainiq-health-permissions.txt"))) {
    New-Item -ItemType File -Path (Join-Path $OutputDir "trainiq-health-permissions.txt") | Out-Null
}

Invoke-Adb -Adb $adb -Arguments @("logcat", "-c") -OutFile (Join-Path $OutputDir "logcat-clear.txt") | Out-Null
Invoke-Adb -Adb $adb -Arguments @("shell", "am", "force-stop", $appId) -OutFile (Join-Path $OutputDir "force-stop-before-main.txt") | Out-Null
Invoke-Adb -Adb $adb -Arguments @("shell", "am", "start", "-W", "-n", $mainActivity) -OutFile (Join-Path $OutputDir "launch-main.txt") | Out-Null
Start-Sleep -Seconds 2
Dump-Ui -Adb $adb -OutFile (Join-Path $OutputDir "main.xml")

Invoke-Adb -Adb $adb -Arguments @("shell", "am", "start", "-W", "-n", $rationaleActivity) -OutFile (Join-Path $OutputDir "launch-rationale.txt") | Out-Null
Start-Sleep -Seconds 2
Dump-Ui -Adb $adb -OutFile (Join-Path $OutputDir "health-rationale.xml")

Invoke-Adb -Adb $adb -Arguments @("shell", "am", "start", "-W", "-a", $healthConnectSettingsAction) -OutFile (Join-Path $OutputDir "launch-health-connect-settings.txt") | Out-Null
Start-Sleep -Seconds 2
Dump-Ui -Adb $adb -OutFile (Join-Path $OutputDir "health-connect-settings.xml")

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

$blocked = @(
    "# Mutable Health Connect Cases",
    "",
    "The script did not grant, revoke, uninstall, disable, or mutate Health Connect/provider state.",
    "Mutable permission/profile cases require a disposable emulator or physical-device profile.",
    "",
    "| Scenario | Status | Reason |",
    "| --- | --- | --- |",
    "| Provider missing/update required | NOT_RUN | Requires provider-disabled/outdated environment. |",
    "| Partial permission grant | NOT_RUN | Requires changing Health Connect grants. |",
    "| Revoke while app is open | NOT_RUN | Requires changing Health Connect grants while TrainIQ is active. |",
    "| Background-read granted/unavailable | NOT_RUN | Requires controlled background permission/feature state. |"
)

if ($MutablePermissionProfileConfirmed) {
    $blocked += ""
    $blocked += "MutablePermissionProfileConfirmed was set. Use the captured baseline files in this folder before running manual grant/revoke steps; this script still avoids making those changes automatically."
}

Set-Content -LiteralPath (Join-Path $OutputDir "mutable-cases-status.md") -Value $blocked -Encoding UTF8

Write-Host "Health Connect evidence written to $OutputDir"
