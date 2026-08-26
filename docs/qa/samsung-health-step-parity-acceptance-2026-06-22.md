# Samsung Health Step Parity Acceptance - 2026-06-22

## Goal

TrainIQ should display a daily step count that matches Samsung Health All steps as closely as the available source allows. The current implementation is Health Connect-first and shows the highest Health Connect-visible daily total from:

- general Health Connect `StepsRecord.COUNT_TOTAL`
- Samsung Health-origin `StepsRecord.COUNT_TOTAL`
- higher Samsung-origin raw `StepsRecord` export when Samsung aggregate under-reports

This avoids under-reporting when Health Connect exposes the higher Samsung export, but it cannot read Samsung Health's private pedometer total if Samsung Health has not written that total into Health Connect.

## Current Health Connect Acceptance

The Health Connect-only path is acceptable when all of these are true on a real Samsung phone:

1. Samsung Health All steps and TrainIQ are compared on the same local day.
2. Samsung Health has been opened and `Sync now` has been used.
3. Health Connect grants TrainIQ `READ_STEPS`.
4. Health Connect shows Samsung Health as a recent source for Activity > Steps.
5. If Health Connect shows multiple Activity/Steps sources, Samsung Health is placed highest in Health Connect App priorities when Samsung Health should be the leading user-visible total.
6. TrainIQ Settings shows either the higher Samsung Health-export or the higher Health Connect aggregate.
7. TrainIQ does not show a lower Samsung-origin value when the general Health Connect aggregate is higher.

## Samsung Health Data SDK Acceptance

If Samsung Health continues to show substantially more steps than TrainIQ after the Health Connect checks above, the remaining parity fix is direct Samsung Health Data SDK verification on a physical Samsung device.

Implementation prerequisites:

1. Add Samsung's `samsung-health-data-api-1.1.0.aar` or newer supported Samsung Health Data SDK API AAR under `TrainIQ-Project/app/libs`.
2. Keep the existing Gradle `app/libs/*.aar` wiring and `SAMSUNG_HEALTH_DATA_SDK_AAR_PRESENT` BuildConfig readiness flag; the readiness flag must only count filenames containing `samsung-health-data-api`, not legacy `samsung-health-data` SDK AARs. Keep Samsung's companion readiness in place: Java 17 source/target compatibility, Kotlin JVM toolchain 17, explicit Gson dependency, Kotlin Parcelize plugin, and `com.samsung.android.sdk.health.data.MIGRATION_COMPLETED=true` manifest metadata.
3. Do not reintroduce legacy Samsung Health SDK permission metadata such as `com.samsung.android.health.permission.read`, `com.samsung.android.health.permission.write`, or `com.samsung.shealth.step_daily_trend`.
4. Keep `SAMSUNG_HEALTH_NON_API_AAR_PRESENT` and the Settings copied diagnostic able to distinguish "no Samsung AAR" from "legacy/other Samsung AAR present but ignored"; the latter must not enable the direct steps path.
5. Keep `com.sec.android.app.shealth` declared in the Android package-visibility `<queries>` block so the app remains compatible with Samsung Health discovery on Android 11+ package visibility rules.
6. Keep Health Connect as the default source and use Samsung Health Data SDK only as a Samsung-specific parity source when available and consented.
7. Use the existing compile-safe `SamsungHealthDirectStepsDataSource` reflection adapter to check Samsung Health Data SDK step read permission through `Permission.of(DataTypes.STEPS, AccessType.READ)`; the adapter must handle static field, static getter, and Kotlin companion property shapes for these SDK values.
8. Use the Settings `Samsung toegang geven` action to call Samsung `requestPermissions` when the SDK AAR is present and steps permission is missing. If Samsung Health returns a resolvable platform exception, the action should open Samsung's resolution flow and ask the user to refresh TrainIQ afterward.
9. Use the same adapter to read today's total steps with `DataType.StepsType.TOTAL` over the same local-day range used by Health Connect. The adapter should support both a direct `setLocalTimeFilter(...)` request builder and Samsung's documented grouped builder example with `setLocalTimeFilterWithGroup(LocalTimeFilter.of(...), LocalTimeGroup.of(LocalTimeGroupUnit.MINUTELY, 30))`.
10. Sum the Samsung aggregate response `dataList` values, matching Samsung's documented `healthDataStore.aggregateData(readRequest).dataList.sumOf { it.value as Long }` total-steps pattern.
11. Use the Samsung SDK total only when it is positive and fresher or higher than the Health Connect-visible total.
12. Preserve Settings diagnostics so users can see Health Connect visible steps, Samsung SDK visible steps, query window, and freshness.
13. Keep the permission check/request path compatible with both direct `Set` returns and Samsung's documented granted-permissions wrapper shape, and surface unexpected Samsung permission-action failures as Settings status instead of an uncaught ViewModel crash.

Physical verification is required because Samsung documents that Samsung Health Data SDK is not supported on emulators. Minimum proof:

1. Install a debug build on a physical Samsung device with Samsung Health 6.30.2 or later.
2. Enable Samsung Health developer mode for local debug builds, or use the approved partnership/app verification path for distributed builds.
3. Grant Samsung Health Data SDK steps permission and Health Connect steps permission.
4. Tap `Samsung toegang geven` in Settings if the Samsung direct status reports missing SDK permission.
5. Record Samsung Health All steps and TrainIQ steps at three times during the day after `Sync now`.
6. Pass when TrainIQ matches Samsung Health All steps within normal provider refresh delay, or Settings clearly shows that Samsung SDK data is unavailable and why.

2026-06-22 emulator crash smoke:

- PASS: debug install on `emulator-5554` completed with the Samsung Health Data SDK AAR present.
- PASS: cold launch returned `Status: ok`, `LaunchState: COLD`; the app process remained alive and the Android crash buffer was empty. The emulator briefly showed a System UI ANR dialog under startup load, but logcat did not attribute an ANR or fatal exception to `com.trainiq`.
- PASS: Settings rendered the Samsung diagnostics with `Samsung Health runtime: app niet gevonden`, and tapping `Samsung toegang geven` did not crash the app; `com.trainiq` stayed alive and the UI still showed the expected Samsung direct status.
- PARTIAL: this proves the no-Samsung/emulator fallback and permission-action crash hardening, but not Samsung's real physical permission dialog or live All steps parity.

2026-06-22 physical crash smoke:

- FAIL/PASS: physical SM-S931B launch reproduced `java.lang.StackOverflowError` in `SamsungHealthDirectStepsDataSource.forwarding` before the continuation fix; after forwarding into the original downstream continuation, the focused Samsung/Health Connect policy test passed.
- PASS: after the fix, broad `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin` passed, and physical SM-S931B cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 569`; the UI showed TrainIQ, no Samsung Device Care app-error dialog appeared, and the crash buffer contained no TrainIQ fatal exception.
- PASS: the phone QA used adb launch/UI/logcat reads only; the quick `/sdcard` scan found no TrainIQ-named test files left on the phone. Do not uninstall or clear app data as cleanup unless the owner explicitly asks, because that can remove local TrainIQ data.

Repeatable evidence helper:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-samsung-step-parity-evidence.ps1 -InstallDebug
```

Physical Samsung parity build helper:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-samsung-step-parity-debug.ps1
```

If the local checkout still reports that the Samsung Health Data SDK API AAR is unavailable, show the official Samsung download/setup pointers first:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-samsung-health-data-sdk-aar.ps1 -HelpSamsungDownload
```

When the Samsung Health Data SDK package has just been downloaded, the helper can install the exact `samsung-health-data-api*.aar` first:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-samsung-step-parity-debug.ps1 -SourcePath "C:\path\to\samsung-health-data-sdk.zip"
```

This helper is intentionally stricter than a normal debug build. It first runs `:app:checkSamsungHealthDataSdkReadiness`, then requires one selected physical Samsung Android 10+ target, rejects emulators through `ro.kernel.qemu`, checks `com.sec.android.app.shealth`, requires Samsung Health 6.30.2 or later, builds through `:app:assembleSamsungHealthParityDebug`, installs through `:app:installSamsungHealthParityDebug` unless `-SkipInstall` is passed, and finally delegates to `collect-samsung-step-parity-evidence.ps1`.

The parity Gradle tasks can also be run directly:

```powershell
.\gradlew.bat :app:assembleSamsungHealthParityDebug --console=plain --no-configuration-cache
.\gradlew.bat :app:installSamsungHealthParityDebug --console=plain --no-configuration-cache
```

For a known mismatch sample such as Samsung Health 600 versus TrainIQ 180, pass the visible values explicitly:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-samsung-step-parity-evidence.ps1 -SamsungHealthAllSteps 600 -TrainIqDisplayedSteps 180
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-samsung-step-parity-debug.ps1 -SamsungHealthAllSteps 600 -TrainIqDisplayedSteps 180
```

The helper writes an evidence folder under `.codex/device-qa/samsung-step-parity-*` on the development machine with `adb-devices.txt`, device manufacturer/model/version, `device-readiness.txt` with physical Samsung/emulator/Android 10+ classification, Samsung Health package dumps for `com.sec.android.app.shealth`, `samsung-health-readiness.txt` with package install and version evidence, local Samsung AAR status, TrainIQ launch timing, crash slice, `manual-comparison.md`, `parity-result.txt` with match/under-report/over-report classification, `acceptance-gates.txt` with physical Samsung Android 10+, Samsung Health Data SDK API AAR, Samsung Health install, Samsung Health 6.30.2+, captured values, and exact-value-match gates, plus an empty `samsung-step-diagnosis.txt` file for the Settings copy action. It does not mutate Samsung Health or Health Connect permissions automatically and should not push evidence/test files to phone storage.

## Source Notes

- Samsung Health Data SDK can access selected data from Samsung Health, including data collected from Galaxy Watch/Ring and transferred to the paired phone.
- Samsung states the Data SDK can provide total steps from multiple devices.
- Samsung `StepsType` describes smartphone plus connected wearable step data, summarizes phone/watch steps without duplicates, and exposes `TOTAL`.
- Samsung's aggregate-data guide uses `DataType.StepsType.TOTAL` and says the summed hourly total can be verified against Samsung Health's Pedometer tracker.
- Samsung's HealthDataStore steps example builds the total request with `DataType.StepsType.TOTAL.requestBuilder.setLocalTimeFilterWithGroup(...)`, so the direct adapter keeps a grouped request fallback.
- Samsung's codelab uses the AAR path `samsung-health-data-api-1.1.0.aar` and developer mode for testing.
- Samsung's overview page is the official SDK download page and includes Samsung Health Data SDK v1.1.0; the rendered page exposes a Samsung file endpoint behind SDK terms, so TrainIQ keeps the install helper as a user-supplied local `.aar`/folder/zip flow instead of bypassing Samsung's download/terms flow.
- Samsung's migration guide distinguishes the deprecated `samsung-health-data-(version).aar` from the new `samsung-health-data-api-(version).aar`; only the `-api-` AAR should enable direct Data SDK parity.
- Samsung's release notes and overview state the SDK does not support emulators.
- Android's Health Connect aggregate docs state that Activity/Steps aggregates account for duplicate data using the user's Health Connect app-priority order, so TrainIQ now surfaces that priority check when multiple step sources are visible.
