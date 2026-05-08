# Performance Evidence Template

Last updated: 2026-05-08

## Run Metadata

- Tester:
- Date:
- Device model:
- Android version:
- Build variant:
- App version/build:
- Battery saver: on/off
- Thermal state:
- Network: wifi/cellular/offline
- Commit/build identifier:

## Commands

```powershell
.\gradlew.bat :app:assembleProfileable :macrobenchmark:assembleAndroidTest --console=plain --no-daemon
.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain --no-daemon
adb shell dumpsys gfxinfo com.trainiq framestats
adb shell dumpsys meminfo com.trainiq
adb logcat -d -t 3000
```

## Results

| Flow | Metric | Threshold | Result | PASS/FAIL | Evidence |
|---|---|---|---|---|---|
| Cold startup | p50/p95 | PRODUCT_CONFIRMATION_REQUIRED | NOT_RUN | NOT_RUN | |
| Warm startup | p50/p95 | PRODUCT_CONFIRMATION_REQUIRED | NOT_RUN | NOT_RUN | |
| Tab navigation | jank/frame timing | PRODUCT_CONFIRMATION_REQUIRED | NOT_RUN | NOT_RUN | |
| Settings scroll | jank/frame timing | PRODUCT_CONFIRMATION_REQUIRED | NOT_RUN | NOT_RUN | |
| Active workout logging | jank/frame timing | PRODUCT_CONFIRMATION_REQUIRED | NOT_RUN | NOT_RUN | |
| Nutrition scanner | startup/capture/result latency | PRODUCT_CONFIRMATION_REQUIRED | NOT_RUN | NOT_RUN | |
| AI fallback/retry | latency/retry count | PRODUCT_CONFIRMATION_REQUIRED | NOT_RUN | NOT_RUN | |
| Repeated navigation | memory delta | PRODUCT_CONFIRMATION_REQUIRED | NOT_RUN | NOT_RUN | |

## Logcat Summary

- FATAL EXCEPTION:
- ANR:
- Repeated severe app errors:
- Notes:

## Decision

- [ ] Meets release performance threshold.
- [ ] Needs optimization before release.
- [ ] Needs product threshold decision.
- [ ] Needs additional physical-device run.

