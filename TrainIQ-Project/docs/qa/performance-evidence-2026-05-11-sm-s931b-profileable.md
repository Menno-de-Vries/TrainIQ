# Performance Evidence - SM-S931B Profileable Run - 2026-05-11

## Run Metadata

- Tester: Codex local run
- Date: 2026-05-11
- Device model: SM-S931B
- Android version: 16, SDK 36
- Build variant: `profileable`
- App version/build: local worktree, git base `b6b0d56`
- Battery saver: not recorded
- Battery state: USB powered, 100%
- Thermal state: `Thermal Status: 0`
- Network: not recorded
- Commit/build identifier: local worktree with app-ready changes; not a clean release commit

## Commands

```powershell
.\gradlew.bat :app:assembleProfileable :macrobenchmark:assembleAndroidTest --console=plain --no-configuration-cache
.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain --no-configuration-cache
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.ProfileableBenchmarkSeedArchitectureTest" :macrobenchmark:compileProfileableJavaWithJavac :macrobenchmark:connectedProfileableAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.macrobenchmark.TrainIqStartupBenchmark#activeWorkoutLoggingFrames" --console=plain --no-configuration-cache
```

## Result Summary

- PASS: `:app:assembleProfileable :macrobenchmark:assembleAndroidTest`
- PASS: `:macrobenchmark:connectedProfileableAndroidTest`
- PASS: `:app:installProfileable`
- PASS: profileable launch and memory/crash capture after explicit profileable install.
- PASS: deterministic profileable active-workout logging macrobenchmark after adding a profileable-only seed activity guarded by `ProfileableBenchmarkSeedArchitectureTest`.
- Macrobenchmark XML: `macrobenchmark/build/outputs/androidTest-results/connected/profileable/TEST-SM-S931B - 16-_macrobenchmark-.xml`
- Benchmark data: `macrobenchmark/build/outputs/connected_android_test_additional_output/profileable/connected/SM-S931B - 16/com.trainiq.macrobenchmark-benchmarkData.json`
- Perfetto traces: `macrobenchmark/build/outputs/connected_android_test_additional_output/profileable/connected/SM-S931B - 16/*.perfetto-trace`
- Baseline profile output: `macrobenchmark/build/outputs/connected_android_test_additional_output/profileable/connected/SM-S931B - 16/trainiq-critical-journeys-startup-prof.txt`
- Memory/crash artifacts: `.codex/device-qa/2026-05-11-profileable-memory-crash/`
- Active-workout logging attempt artifacts: `.codex/device-qa/2026-05-11-active-workout-performance-attempt/`
- Active-workout deterministic seed/debug artifacts: `.codex/device-qa/2026-05-11-active-workout-benchmark-debug/`

## Test Results

| Test | Result |
| --- | --- |
| `generateBaselineProfileForCriticalJourneys` | PASS |
| `coldStartupWithRequiredBaselineProfile` | PASS |
| `topLevelNavigationAndSettingsScrollFrames` | PASS |
| `activeWorkoutLoggingFrames` | PASS |

Original full macrobenchmark XML result: 3 tests, 0 failures, 0 errors, 0 skipped.

Targeted active-workout macrobenchmark result: 1 test, 0 failures, 0 errors, 0 skipped.

## Metrics

| Flow | Metric | Result | Threshold | PASS/FAIL |
| --- | --- | --- | --- | --- |
| Cold startup | `timeToInitialDisplayMs` min / median / max | 202.812761 / 245.662812 / 337.020729 ms | `PRODUCT_CONFIRMATION_REQUIRED` | NOT CERTIFIED |
| Top-level navigation + Settings scroll | `frameCount` min / median / max | 331 / 331 / 333 frames | `PRODUCT_CONFIRMATION_REQUIRED` | NOT CERTIFIED |
| Top-level navigation + Settings scroll | `frameDurationCpuMs` P50 / P90 / P95 / P99 | 1.348958 / 2.41025 / 3.0113126 / 5.33931872 ms | `PRODUCT_CONFIRMATION_REQUIRED` | NOT CERTIFIED |
| Top-level navigation + Settings scroll | `frameOverrunMs` P50 / P90 / P95 / P99 | -4.559821 / -3.31613 / -2.8739724 / 3.01926004 ms | `PRODUCT_CONFIRMATION_REQUIRED` | NOT CERTIFIED |
| Active-workout logging | `frameCount` min / median / max | 9 / 9 / 9 frames | `PRODUCT_CONFIRMATION_REQUIRED` | NOT CERTIFIED |
| Active-workout logging | `frameDurationCpuMs` P50 / P90 / P95 / P99 | 1.687865 / 2.4953752 / 2.960833 / 3.22850592 ms | `PRODUCT_CONFIRMATION_REQUIRED` | NOT CERTIFIED |
| Active-workout logging | `frameOverrunMs` P50 / P90 / P95 / P99 | -5.288078 / -4.1804796 / -3.9324458 / -3.733878 ms | `PRODUCT_CONFIRMATION_REQUIRED` | NOT CERTIFIED |

## Profileable Memory And Crash/ANR Capture

- Command: `.\gradlew.bat :app:installProfileable --console=plain --no-configuration-cache`
- Launch: `adb shell am start -W -n com.trainiq/.MainActivity`
- Launch result: `Status: ok`, `LaunchState: COLD`, `TotalTime: 159`, `WaitTime: 161`
- Before-launch memory state: `No process found for: com.trainiq`
- After-navigation memory: `TOTAL PSS: 108947`, `TOTAL RSS: 219432`, `TOTAL SWAP PSS: 1267`
- After-navigation app summary: Java Heap 8228 PSS / 31660 RSS; Native Heap 14416 PSS / 16964 RSS; Code 29212 PSS / 108096 RSS; Stack 1044 PSS / 1052 RSS; Graphics 45864 PSS / 45864 RSS; Private Other 5784 PSS; System 4399 PSS.
- Logcat crash slice: 0 lines in `.codex/device-qa/2026-05-11-profileable-memory-crash/logcat-crash-slice.txt`
- Dropbox ANR/crash scan: 0 lines in `.codex/device-qa/2026-05-11-profileable-memory-crash/dropbox-anr-crash-scan.txt`
- Activity process scan: TrainIQ process present as top activity; no ANR line was captured in `.codex/device-qa/2026-05-11-profileable-memory-crash/activity-process-anr-scan.txt`.

## Logcat Summary

- FATAL EXCEPTION: none captured in the post-install profileable logcat crash slice.
- ANR: none captured in the activity/dropbox scan.
- Repeated severe app errors: none captured in the post-install profileable logcat crash slice.
- Notes: This is still a single-device local run, not owner-approved certification.

## Decision

- [ ] Meets release performance threshold.
- [ ] Needs optimization before release.
- [x] Needs product threshold decision.
- [x] Needs additional physical-device run.

## Active-Workout Logging Evidence Attempt

- Result: NOT_RUN / blocked.
- Evidence: `.codex/device-qa/2026-05-11-active-workout-performance-attempt/README.md`
- Summary: Training was reachable, but the profileable app state had no active routine or active workout. Tapping `Lege routine maken` opened the routine-name dialog, but coordinate/IME automation moved focus into Samsung keyboard settings during text entry. No active workout was started and no logging performance metric was captured.
- Follow-up result: PASS. A profileable-only `BenchmarkSeedActivity` now seeds a deterministic active workout; the macrobenchmark opens the seeded active workout and logs one set.
- Guardrail: `ProfileableBenchmarkSeedArchitectureTest` verifies the seed activity lives in `src/profileable`, is declared only by the profileable manifest, and is not present in the main manifest.

## Readiness Impact

This run provides physical-device profileable startup, top-level navigation/settings-scroll, active-workout logging, post-launch memory, and crash/ANR scan evidence. It does not certify release performance because numeric thresholds, device matrix approval, broader flow memory evidence, and release owner signoff remain open under `PERF-001`.
