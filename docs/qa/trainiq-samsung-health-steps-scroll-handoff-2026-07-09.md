# TrainIQ Samsung Health Steps and Workout Scroll Handoff

Last updated: 2026-07-09

## Start Here in the New Chat

Use this document as the source of truth for the next investigation. Do not assume the Samsung Health step-parity issue is fixed. The startup crash introduced during the direct Samsung SDK work is fixed and verified, but the user still sees TrainIQ progress through the day remaining substantially below Samsung Health All steps. The earlier `600 versus 180` values were an example, not a fixed reproducible pair.

Recommended opening instruction for the new chat:

> Continue the TrainIQ Samsung Health step-parity and active-workout scroll investigation from `docs/qa/trainiq-samsung-health-steps-scroll-handoff-2026-07-09.md`. First inspect the current code, existing QA evidence, and connected devices. Do not redo the already verified continuation crash fix. Diagnose the live step mismatch end to end using copied Settings diagnostics and physical Samsung evidence. Use official Samsung and Android sources, add focused regression protection before changing behavior, run broad Android verification, and finish with a physical-device comparison. Do not uninstall the app, clear app data, change permissions automatically, or leave test files on the phone.

## Handoff Summary

- task: Resolve the persistent Samsung Health All steps mismatch and the remaining active-workout vertical scroll stutter.
- scope: `D:\GitHub\TrainIQ`, primarily `TrainIQ-Project/app`, the Samsung parity scripts, and QA documentation.
- status: The crash blocker is done. Samsung step parity is still open. Scroll smoothness improved but is still open according to physical-device feedback.
- current branch: `Paid-Base-Model`
- current commit: `8b12ea2c` (`Refactor TrainIQ data flow and UI state handling`, 2026-06-22)
- worktree at handoff creation: clean.
- important user constraint: testing must not leave test files on the phone. Do not uninstall or clear app data unless the user explicitly asks, because local TrainIQ data may be lost.

## User-Observed Behavior

### Samsung Health steps

The app remains much too low compared with Samsung Health as the day progresses. A previously mentioned comparison was Samsung Health `600` versus TrainIQ `180`, but the user explicitly clarified that these numbers were only an example. The actual invariant is that TrainIQ falls increasingly behind Samsung Health All steps during normal daily use.

The user tested multiple successive builds on a physical Samsung phone and repeatedly confirmed that the mismatch remained. Therefore unit tests, emulator fallback behavior, and a successful launch are not acceptance proof for parity.

### Active strength-workout scrolling

Vertical scrolling up and down in the active strength-training experience still stutters. A prior layout adjustment reduced the problem but did not remove it. Treat this as a separate performance workstream after or alongside step-source diagnosis; a step-data fix must not be presented as a scroll fix.

## What Has Already Been Implemented

### Health Connect step selection

`HealthConnectDataSource` reads the local-day Health Connect aggregate and builds diagnostics for:

- general Health Connect `StepsRecord.COUNT_TOTAL`;
- Samsung Health-origin aggregate steps;
- Samsung Health-origin raw step-record sum;
- steps overlapping Health Connect exercise-session windows;
- direct Samsung Health Data SDK total when available;
- the final display value and query/freshness information.

The intended display policy is to avoid selecting a lower Samsung-origin value when a higher valid Health Connect value is visible. Old cache entries fall back through mapper logic, while new syncs persist an explicit `displayStepsToday`.

Relevant files:

- `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/mapper/Mappers.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/domain/model/DomainModels.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`

### Direct Samsung Health Data SDK path

A direct Samsung-specific adapter now exists in `SamsungHealthDirectStepsDataSource.kt`. It:

- is gated to Android API 29 or later;
- checks Samsung Health installation and minimum supported runtime;
- checks and requests Samsung steps read permission;
- supports Samsung SDK static fields, getters, and Kotlin companion shapes through reflection;
- requests `DataType.StepsType.TOTAL` for the same local-day window as Health Connect;
- supports both a direct local-time filter and Samsung's grouped local-time-filter builder shape;
- sums aggregate response values;
- returns a status instead of crashing on unexpected SDK failures.

The official API AAR is present at:

- `TrainIQ-Project/app/libs/samsung-health-data-api-1.1.0.aar`

Gradle and manifest readiness include Java/Kotlin 17, Gson, Parcelize, Samsung migration metadata, package visibility, and a focused `tools:overrideLibrary` for the Samsung library's API 29 minimum. TrainIQ's general `minSdk` remains lower; runtime gating prevents use of the direct SDK below API 29.

### Settings diagnostics and actions

Settings exposes the Health Connect/Samsung diagnostic state and actions including:

- `Samsung toegang geven`;
- `Diagnose kopieren`;
- `Samsung Health openen` for Sync now;
- Health Connect refresh.

The copied diagnosis is the highest-value missing artifact for the next investigation. It should reveal whether the low value originates in Samsung direct SDK reading, Health Connect aggregation, final display selection, stale cache, or UI refresh.

Relevant file:

- `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`

### Samsung SDK helper scripts

These scripts are committed under `TrainIQ-Project/scripts`:

- `install-samsung-health-data-sdk-aar.ps1`: verifies/installs an exact `samsung-health-data-api*.aar` from a user-supplied AAR, folder, or ZIP.
- `build-samsung-step-parity-debug.ps1`: checks SDK readiness, requires one selected physical Samsung Android 10+ device, checks Samsung Health 6.30.2+, builds, installs, and starts evidence collection.
- `collect-samsung-step-parity-evidence.ps1`: records device/build/launch/parity evidence on the development machine under `TrainIQ-Project/.codex/device-qa`.

The evidence helper must not push evidence or test files to phone storage.

### Active-workout scroll mitigation already attempted

The active routine card previously used a generic wrapping action layout. It now uses the dedicated lightweight `ActiveRoutineActionRow`, a stable two-button `Row`, with a focused source guard in `WorkoutInputValidationTest`. This reduced the reported stutter but did not eliminate it.

The broader workout screen remains performance-sensitive because it includes a large `LazyColumn`, remembered per-card/edit state, live draft updates, focus/IME handling, gesture handling, and sticky/bottom workout controls. Do not assume the remaining stutter is caused by the active-routine action row.

Relevant files:

- `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
- `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`

## Crash Found and Fixed

The first physical Samsung build with the direct SDK crashed at startup with:

```text
java.lang.StackOverflowError
SamsungHealthDirectStepsDataSource$forwarding$1.resumeWith
SamsungHealthDirectStepsDataSource.kt:447
```

Root cause: the reflection-to-coroutine bridge forwarded results using unqualified `::resume` and `::resumeWithException` callable references. Inside the wrapper continuation this recursively resumed the wrapper itself.

The fix captures the original continuation as `downstream` and explicitly calls:

```kotlin
downstream.resume(value)
downstream.resumeWithException(throwable)
```

Do not replace this with the old callable-reference form. A focused guard exists in `HealthConnectPermissionPolicyTest`.

## Errors Encountered During Setup

These issues have already been understood and should not consume another investigation cycle:

1. `The argument '.\scripts\install-samsung-health-data-sdk-aar.ps1' ... does not exist`
   Cause: the command was run from `D:\GitHub\TrainIQ` instead of `D:\GitHub\TrainIQ\TrainIQ-Project`, where the `scripts` folder exists.

2. `adb.exe: device 'a' not found`
   Cause: an invalid or truncated adb serial was passed. Always obtain the exact serial from `adb devices -l` and pass it as one quoted value.

3. Samsung AAR `minSdkVersion 29` versus app `minSdkVersion 26`
   Resolution: a focused manifest `tools:overrideLibrary="com.samsung.android.sdk.health.data"` was added, while the direct data source hard-gates execution to API 29+.

4. `Health Data SDK API AAR niet beschikbaar`
   Resolution: the official `samsung-health-data-api-1.1.0.aar` was installed and committed. Legacy `samsung-health-data-*.aar` files intentionally do not enable readiness.

5. App startup crash after installing the SDK build
   Resolution: the continuation recursion described above was fixed and verified on the physical Samsung device.

6. Emulator cannot prove Samsung parity
   Samsung Health Data SDK is not supported on emulators. The emulator is useful only for fallback/no-crash behavior.

## Verification Already Completed

### Tests and build

The focused policy test passed:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache
```

The broad Android gate passed after the crash fix:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache
```

`git diff --check` also passed apart from expected Windows line-ending warnings.

### Emulator

The SDK-present debug build installed and launched on `emulator-5554`. Settings correctly reported that the Samsung Health app was not found, and tapping `Samsung toegang geven` did not crash TrainIQ. This verifies fallback behavior only.

### Physical Samsung device

Historical device: Samsung SM-S931B, Android 16. The adb serial used then was `adb-RFCY60HNHNJ-Jf2gXF._adb-tls-connect._tcp`; discover the current serial again because wireless adb serials can change.

After the continuation fix:

- cold launch returned `Status: ok` and `LaunchState: COLD`;
- TrainIQ remained visible and alive;
- no Samsung Device Care app-error dialog appeared;
- the crash buffer contained no TrainIQ fatal exception;
- a quick `/sdcard` scan found no TrainIQ-named test files;
- no uninstall or app-data clear was performed.

Historical evidence folders include:

- `TrainIQ-Project/.codex/device-qa/emulator-crash-smoke-2026-06-22`
- `TrainIQ-Project/.codex/device-qa/physical-launch-fallback-2026-06-22-1131`
- `TrainIQ-Project/.codex/device-qa/physical-launch-after-forwarding-fix-2026-06-22-1136`
- `TrainIQ-Project/.codex/device-qa/samsung-step-parity-build-2026-06-22-112916`

## What Is Still Unresolved

### Exact Samsung Health All steps parity

There is still no accepted physical-device evidence showing that TrainIQ follows Samsung Health All steps across the day. A successful build, permission action, or no-crash launch is not sufficient.

The next chat must capture, at the same moment:

- Samsung Health All steps;
- TrainIQ displayed steps;
- copied TrainIQ Settings diagnosis;
- whether `Samsung toegang geven` was granted;
- whether Samsung Health was synced immediately beforehand;
- the local time and time zone.

Repeat this at least three times as the count increases. The important signal is not only one absolute difference but whether TrainIQ's delta over time follows Samsung Health's delta.

### Workout-recorded steps question

TrainIQ currently aggregates the full local day and does not intentionally subtract steps that overlap an exercise session. The diagnostic separately calculates steps inside Health Connect exercise-session windows only to help explain source differences. Therefore an automatically detected Samsung workout should not, by TrainIQ policy, remove those steps from the daily total.

However, the current evidence does not prove what Samsung Health has exported into Health Connect or what the direct SDK returns on this phone. That distinction must be settled from the live diagnostics rather than inferred from the workout label in Samsung Health.

### Scroll stutter

No objective post-change frame-time trace proves that active-workout scrolling is smooth. The user's physical-device feedback is the acceptance signal and says it still stutters. The next performance pass should capture frame/jank evidence on the actual active workout, then correlate spikes with recomposition, layout, database writes, focus/IME handling, or gesture work.

## Smart Diagnosis Tree for the Next Chat

Start with the copied Settings diagnosis before editing code.

1. Direct Samsung SDK status is unavailable or permission missing.
   Fix the runtime/readiness/permission-resolution path. Confirm developer mode or approved app access and the Samsung Health version. Do not compensate with arithmetic in the UI.

2. Direct Samsung SDK total matches Samsung Health, but TrainIQ display is lower.
   The defect is after data acquisition: inspect `displaySteps` selection, DataStore persistence, mapper fallback, repository cache, ViewModel refresh, and Home rendering.

3. Direct Samsung SDK total is positive but already much lower than Samsung Health.
   Inspect the reflected aggregate request, local-day boundaries, grouped filter behavior, response value types, pagination/group summing, SDK freshness, and whether the SDK permission covers the intended total-steps data.

4. Health Connect aggregate is high, but TrainIQ display is low.
   Inspect selection and stale-cache overwrite behavior. A lower Samsung-origin result must not replace a higher valid Health Connect aggregate.

5. Health Connect and direct Samsung SDK are both low.
   Verify Samsung Health Sync now, data source/app priorities, developer-mode or partnership requirements, permission state, and Samsung export freshness. This is likely a provider/input issue unless the query window is wrong.

6. Values match immediately after refresh but later drift.
   Inspect refresh cadence, ChangesToken handling, background worker scheduling, cache timestamps, and whether the UI observes the refreshed state.

7. Day-boundary-only mismatch.
   Inspect `LocalDateTime`, `ZoneId.systemDefault()`, inclusive/exclusive end times, daylight-saving transitions, and whether Samsung Health's displayed day uses the same zone.

## Recommended Next Execution Sequence

Run commands from `D:\GitHub\TrainIQ\TrainIQ-Project`.

1. Confirm repository and devices:

```powershell
git status --short
adb devices -l
```

2. Build/install on exactly one selected physical Samsung device:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-samsung-step-parity-debug.ps1 -Serial "<exact-adb-serial>"
```

3. On the phone:

- open Samsung Health and use Sync now;
- open TrainIQ Settings;
- grant Health Connect steps if needed;
- tap `Samsung toegang geven` if direct status says permission is missing;
- tap refresh;
- tap `Diagnose kopieren` and provide the complete text to the next chat.

4. Record matching values without modifying phone storage:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-samsung-step-parity-evidence.ps1 -SamsungHealthAllSteps <value> -TrainIqDisplayedSteps <value>
```

5. Add a focused failing test for the identified branch, apply the smallest fix, then run the focused test and broad gate.

6. Repeat the comparison on the physical phone at multiple increasing step counts. Confirm no crash and scan only for stray TrainIQ test artifacts; do not clear or uninstall the app.

7. For the scroll issue, use a release-like/profileable build and capture an active-workout scroll trace on the physical phone. Test repeated fast up/down scroll with the keyboard closed and open, while set values are unchanged and while editing, to isolate the expensive path.

## Acceptance Criteria

### Steps

- TrainIQ and Samsung Health use the same local day and are compared immediately after sync/refresh.
- TrainIQ's increase over time follows Samsung Health's increase over time.
- Any remaining small difference is explainable by normal provider refresh delay and converges after refresh.
- Settings diagnostics show which source won and why.
- Workout-overlapping steps are not subtracted from the daily display.
- Permission denial, unavailable SDK, old Android, or missing Samsung Health never crashes the app.

### Active-workout scrolling

- Repeated vertical up/down scrolling on the user's physical device is visually smooth in the active strength workout.
- No text-field focus, keyboard, sticky control, or set-edit action causes recurring frame stalls.
- The fix has focused regression coverage and does not break workout editing, set completion, or restoration.

### Safety and cleanup

- No test files are pushed to phone storage.
- No app data is cleared and the app is not uninstalled without explicit user approval.
- Evidence remains under `TrainIQ-Project/.codex/device-qa` on the development machine.

## Official Sources to Recheck

Use current official documentation because Samsung SDK requirements can change:

- [Samsung Health Data SDK overview and download](https://developer.samsung.com/health/data/overview.html)
- [Samsung Health steps-data codelab](https://developer.samsung.com/codelab/health/steps-data.html)
- [Samsung Health Data SDK release notes](https://developer.samsung.com/health/data/release-note.html)
- [Android Health Connect aggregate data](https://developer.android.com/health-and-fitness/guides/health-connect/develop/aggregate-data)
- [Android Health Connect DataOrigin API](https://developer.android.com/reference/androidx/health/connect/client/records/metadata/DataOrigin)
- [Android Compose performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)
- [Android system tracing for app performance](https://developer.android.com/topic/performance/tracing)

## Existing Detailed Reference

The earlier implementation and acceptance detail remains in:

- `docs/qa/samsung-health-step-parity-acceptance-2026-06-22.md`
- `docs/TrainIQ_QA_Findings_To_Improve.md`
- `docs/TrainIQ_Target_State_Progress.md`

This handoff supersedes any earlier implication that the step-count issue was solved. Only the direct-SDK startup crash is conclusively closed; live Samsung Health All steps parity and active-workout scroll smoothness remain open.
