# Health Connect Runtime Matrix - 2026-05-10

Device: SM-S931B (`RFCY60HNHNJ`)

Purpose: capture real-device Health Connect behavior without silently changing a user's health-data permissions. This matrix records what was actually tested and what remains blocked or unsafe without a dedicated test profile.

## Evidence Paths

- Runtime Settings smoke: `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-runtime/`
- Follow-up rationale/manifest visibility smoke: `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-followup/`
- Main launch after manifest visibility: `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-followup/launch-after-manifest-visibility.txt`
- Rationale launch: `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-followup/launch-rationale.txt`
- App permission state: `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-followup/trainiq-health-permissions.txt`
- Rationale UI dump: `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-followup/health-rationale.xml`
- Scripted repeatable baseline: `TrainIQ-Project/scripts/collect-health-connect-runtime-evidence.ps1`
- 2026-05-11 scripted baseline evidence: `TrainIQ-Project/.codex/device-qa/2026-05-11-health-connect-scripted-baseline-debug-v4/`

## Matrix

| Scenario | Result | Evidence | Status | Notes |
| --- | --- | --- | --- | --- |
| Health Connect package available | Device lists `com.google.android.healthconnect.controller`; app also declares package visibility for this controller. | `health-packages.txt`, `AndroidManifest.xml` | PASS | Modern Android Health Connect package is visible. |
| App installed and launchable | Main activity launched with `Status: ok`, `LaunchState: COLD`, `WaitTime: 706`. | `2026-05-11-health-connect-scripted-baseline-debug-v4/launch-main.txt`, `main.xml` | PASS | Script force-stops before launch for deterministic cold-start evidence; crash slice was empty. |
| No Health Connect permissions granted | All requested Health permissions, including background read, are `granted=false`. | `2026-05-11-health-connect-scripted-baseline-debug-v4/trainiq-health-permissions.txt` | PASS | Confirms the no-permission baseline without granting data access. |
| Settings/manage-access no-permission state | System Health Connect manage-access screen opens through `android.health.connect.action.MANAGE_HEALTH_PERMISSIONS` with `Status: ok` and renders Health Connect access sections. | `2026-05-11-health-connect-scripted-baseline-debug-v4/launch-health-connect-settings.txt`, `health-connect-settings.xml` | PASS | The script verifies the direct manage-access path without changing grants. |
| App-owned rationale before system prompt | Rationale activity launches with `Status: ok`, shows purpose text and signal-level reasons for steps, heart rate, and sleep in the visible viewport. | `2026-05-11-health-connect-scripted-baseline-debug-v4/launch-rationale.txt`, `health-rationale.xml` | PASS | This stops before granting/revoking permissions. |
| Crash scan for baseline/rationale/manage-access smoke | Logcat crash slice is empty after the scripted baseline run. | `2026-05-11-health-connect-scripted-baseline-debug-v4/logcat-crash-slice.txt` | PASS | No fatal crash was captured from the scripted launches. |
| Provider missing/update required | Not executed. | None | NOT_RUN | Requires emulator/device profile without Health Connect or with outdated provider. Current physical device has provider installed. |
| Partial permission grant | Not executed. | None | NOT_RUN | Requires changing real Health Connect grants. Use a dedicated test profile/device before running. |
| Revoke while app is open | Not executed. | None | NOT_RUN | Requires changing real Health Connect grants while TrainIQ is active. Use a dedicated test profile/device before running. |
| Background-read unavailable/granted | Not executed beyond confirming requested background permission is currently `granted=false`. | `trainiq-health-permissions.txt` | PARTIAL | Requires controlled permission grant and feature-state check on a safe test profile. |

## Safe Rerun Steps

Use an emulator or disposable physical-device profile before changing Health Connect permissions.

1. Install debug build: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`.
2. Capture baseline packages: `adb shell pm list packages | Select-String -Pattern "health|trainiq"`.
3. Launch TrainIQ: `adb shell am start -W -n com.trainiq/.MainActivity`.
4. Dump Settings no-permission UI and logcat crash buffer.
5. Launch rationale only: `adb shell am start -W -n com.trainiq/.core.health.HealthConnectPermissionsRationaleActivity`.
6. For partial/revoke/background cases, use only the test profile's Health Connect permission UI; capture before/after `dumpsys package com.trainiq` permission state, UI dumps, and logcat crash buffers.

Repeatable non-mutating baseline command:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-health-connect-runtime-evidence.ps1 -OutputDir '.codex\device-qa\YYYY-MM-DD-health-connect-scripted-baseline'
```

Use `-InstallDebug` when the debug package is not already installed. Use `-MutablePermissionProfileConfirmed` only to annotate that a safe profile exists; the script still avoids granting, revoking, uninstalling, or disabling Health Connect automatically.

## Readiness Impact

This matrix improves Health Connect runtime evidence but does not close QA-2026-05-10-019. Release readiness still requires provider missing/update, partial grant, revoke-while-open, and background-read unavailable/granted runtime evidence on a safe test device/profile.
