# Device-Lab Performance Plan

Last updated: 2026-05-08

Status: preparation only. Physical-device certification is not complete.

## Device Matrix

| Class | Device | Android version | Required |
|---|---|---|---|
| Low/mid phone | OWNER_CONFIRMATION_REQUIRED | Android 12 or 13 | yes |
| Current mainstream phone | OWNER_CONFIRMATION_REQUIRED | Android 14 or 15 | yes |
| Large phone | OWNER_CONFIRMATION_REQUIRED | Android 14+ | yes |
| Tablet/foldable | OWNER_CONFIRMATION_REQUIRED | Android 14+ | recommended by blueprint |
| Reference emulator | Medium Phone AVD | Current local emulator | smoke only, not certification |

## Build Instructions

From `D:\GitHub\TrainIQ\TrainIQ-Project`:

```powershell
.\gradlew.bat :app:assembleProfileable :macrobenchmark:assembleAndroidTest --console=plain --no-daemon
.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain --no-daemon
```

If release signing is available to the owner:

```powershell
.\gradlew.bat :app:assembleRelease --console=plain --no-daemon
```

Do not run release upload/publish tasks from local automation.

## Flows To Measure

- Cold startup.
- Warm startup.
- Bottom navigation between all top-level tabs.
- Settings scroll.
- Active workout logging: add set, edit weight/reps, finish workout.
- Nutrition scanner open/capture/result fallback.
- Coach AI request fallback and missing-key state.
- Generated routine preview open/scroll/save/cancel.

## Metrics

| Metric | Threshold | Source |
|---|---|---|
| Cold startup p50/p95 | PRODUCT_CONFIRMATION_REQUIRED | Blueprint requires thresholds but does not define numbers |
| Warm startup p50/p95 | PRODUCT_CONFIRMATION_REQUIRED | Blueprint requires thresholds but does not define numbers |
| Frame jank during tab switch | PRODUCT_CONFIRMATION_REQUIRED | Macrobenchmark/profileable run |
| Frame jank during active workout logging | PRODUCT_CONFIRMATION_REQUIRED | Macrobenchmark/profileable run |
| Memory growth after repeated navigation | PRODUCT_CONFIRMATION_REQUIRED | Android Studio profiler or `adb shell dumpsys meminfo` |
| Battery/network behavior during AI failure/retry | PRODUCT_CONFIRMATION_REQUIRED | Device-lab observation |

## Evidence To Capture

- Device model, Android version, battery mode, thermal state if available.
- Build variant and commit/build identifier.
- Macrobenchmark reports.
- Perfetto trace for any failed/janky flow.
- `adb logcat` crash/ANR scan.
- `adb shell dumpsys gfxinfo com.trainiq framestats` where useful.
- `adb shell dumpsys meminfo com.trainiq` before and after repeated flows.

## Local Risk Notes

- Debug emulator jank must not be used as release severity.
- AI retry is bounded, but production network behavior still needs poor-network testing.
- Meal image compression bounds are present, but device-lab should include large image capture/import.

