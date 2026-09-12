# Nutrition follow-up: full local suite recovery

Final result, 2026-09-13 (Europe/Amsterdam): **PASS — all 154 connected tests,
zero failures/errors/skips; all 917 JVM tests; debug assembly, Android-test
compilation and lint (zero errors, 68 advisory warnings).**

Base: `9b53c3b`, branch `codex/nutrition-scanner-energy`, primary worktree
`C:/My-PC-Files/GitHub/TrainIQ`. No additional worktree, remote test, dependency,
schema, permission declaration, release, push or PR is part of this follow-up.

## Diagnosis and fixes

The previous interrupted suite is preserved as historical evidence in
[nutrition-scanner-energy.md](nutrition-scanner-energy.md). A fresh six-test run
reproduced both outstanding failures before the fixes: the Coach navigation
assertion failed and `SleepRoutineInstrumentedTest` crashed with
`ForegroundServiceDidNotStartInTimeException`.

The compact navigation exposes Coach by content description. Its old test only
looked for text. The fixture also opened/deleted a separate Room database instead
of using the application's existing test database, and did not establish
onboarding/AI preferences. It now uses the existing shared fixture, explicitly
completes onboarding, disables AI, matches the accessible navigation action and
scrolls the report button into view. Report assertions retain highlights, next
step, training context and nutrition advice. The report's visible disabled-AI
message replaces an obsolete remote-success snackbar expectation; the compact
card displays only the first two summary sentences.

Sleep confirmation previously invalidated the playback revision and called
`Context.stopService`, even while a foreground start was still pending. Android
logged `Bringing down service while still waiting for start foreground` and
crashed before the service could repair this. Cancellation now reaches only a
foreground-promoted instance through its main-looper lifecycle; queued starts
invalidate themselves. The active reference is cleared before stopping and on
destruction, audio resources are released, and cancellation callbacks cannot
stop a newer revision. The requested revision is captured atomically when a
reminder is requested.
Cancellation, countdown replacement and notification fallback are serialized on
the same main looper as the service handshake and guarded by revision, preventing
late work from replacing a newer notification.

The service also briefly acknowledges rejected starts with a separate silent
notification, immediately removes it and stops only that start ID. The separate
ID preserves the confirmed countdown. Rejected starts never request audio focus
or construct a player. Normal playback, alarm scheduling and the existing channel
remain unchanged.
Stopping active playback detaches its foreground notification synchronously before
the scheduler posts the countdown; deferred detachment could otherwise let Android
remove the replacement notification. The regression waits for initial asynchronous
notification placement, then continuously asserts preservation beyond the timeout.

The added framework regression holds the main looper while requesting and
cancelling three reminders, then queues three rejected starts after confirmation,
asserts no alarm playback and an intact countdown, and observes beyond the
foreground-start timeout. The original crashing Room/reconciliation test remains
intact. Merely fixing the stale-start branch initially passed focused sleep tests
but still failed the full suite (127/154 executed). The expanded single regression
then reproduced that missing scheduler race before the lifecycle fix. These
intermediate failures remain recorded, not represented as passing evidence.

Owning-platform reference, checked 2026-09-12:
[Android foreground-service timeout troubleshooting](https://developer.android.com/develop/background-work/services/fgs/troubleshooting#internal-exception-foregroundservicedidnotstartintimeexception).

## Local environment and commands

The broadened JVM run also exposed `HomeObservationTest` racing a real IO thread
against `runCurrent()`. The failure read energy totals before refreshed Health
Connect steps arrived. Home now follows the existing onboarding constructor
pattern: Hilt supplies `Dispatchers.IO`, while the internal test constructor
accepts the test dispatcher. Both refresh paths share that dispatcher. The test
keeps every existing error/retry, live intake/workout and step-energy assertion;
its worker and Main now use the same deterministic test scheduler. The original
failure is retained as `home-observation-red.xml`; the focused test and subsequent
complete 917-test JVM run passed.

Once the suite could run past the old crash, it exposed obsolete navigation
fixtures in Exercise History and the two top-level flow smoke tests. The history
test now initializes onboarding and locates Training by its accessible action.
It verifies the two seeded sessions and best weight before explicitly scrolling
the lazy list to its volume chart below the compact viewport.
The flow smoke resets preferences through their repository, explicitly exercises
first-run deferral, checks the current empty-state guidance, opens body progress
through Coach, and retains recreation/back-stack assertions. It no longer assumes
that first launch skips onboarding or that Settings owns the removed progress link.

Run from `TrainIQ-Project/` using Android Studio JBR and the installed SDK:

```powershell
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
$env:ANDROID_HOME='C:/Users/menno/AppData/Local/Android/Sdk'
$env:ANDROID_SERIAL='emulator-5580'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.coach.CoachInsightsInstrumentedTest' --console=plain --max-workers=2
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain --max-workers=2
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --max-workers=2
```

Focused sleep selection: `com.trainiq.features.sleep.SleepNotificationInstrumentedTest,com.trainiq.features.sleep.SleepRoutineInstrumentedTest,com.trainiq.features.sleep.SleepAlarmRegressionTest`.
The 12-test follow-up adds
`com.trainiq.features.workout.ExerciseHistoryInstrumentedTest,com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest`
to that same `-Pandroid.testInstrumentationRunnerArguments.class=` value. Final
focused history proof uses `com.trainiq.features.workout.ExerciseHistoryInstrumentedTest`
with the baseline tasks in the first command above. No filter is used for the
full-suite command.

The existing task-owned `TrainIQ_Agent_Nutrition_20260912` AVD is reused at API 36,
360 x 640 dp and font scale 1.3, headless with SwiftShader. No AVD/image download,
data wipe or new emulator profile. Generated evidence remains ignored under
`.codex/device-qa/suite-recovery/` and the normal Gradle build output directories.

The user requested maximum local test-environment coverage and will perform the
physical-phone camera checks separately. Emulator camera and accessibility
evidence does not establish physical autofocus, barcode scanning distance,
lighting tolerance, audible output or OEM behavior.

## Verification results

The final unfiltered `:app:connectedDebugAndroidTest --console=plain --max-workers=2`
completed in 6m 4s with **154/154 PASS**, after the final source/test changes.
The retained authoritative report is `.codex/device-qa/suite-recovery/full-final.xml`.
The previous full-suite failures below are historical and superseded by this run.
All 917 JVM tests, debug assembly and lint also passed on the final production
source; no input changed afterwards except documentation.

The agent-started emulator was stopped after verification. Its pre-existing AVD
was retained. Accessibility settings were verified restored to their original
values. No physical device was connected or modified.

- PASS: debug assembly, Android-test Kotlin compilation, all 917 JVM tests (zero
  failures/errors/skips), and lint (zero errors, 68 advisory warnings).
- PASS: the corrected Coach integration test in isolation.
- PASS: nine focused sleep tests, including the new delayed-cancellation
  regression and the original Room/reconciliation test that crashed before the fix.
- Intermediate evidence is retained: `reproduced.xml`, `sleep-fixed.xml` (nine
  sleep passes and one subsequently corrected Coach text failure), and
  `coach-fixed.xml`. A test-only compile error from importing member APIs as
  extensions was corrected before these passing runs.
- The first uninterrupted full run executed all 154 tests: 149 passed and five
  failed. Those failures identified asynchronous countdown sequencing and the
  obsolete Exercise History/top-level smoke expectations described above. After
  correction, the nine sleep tests and two flow tests passed together; their
  remaining chart-visibility failure prompted the explicit lazy-list scroll.

## Camera and accessibility smoke

The installed TalkBack 16.0 service was enabled temporarily on the agent-owned
AVD; `dumpsys accessibility` confirms it was bound with spoken/haptic/audible
feedback and touch exploration. The original accessibility settings (no enabled
service, accessibility disabled) were restored before the final suite.

PASS for the observed smoke scope: keyboard Tab/Enter opened the nutrition menu;
TalkBack focus moved into the sheet and to scanner/permission headings on window
changes. Product -> barcode scanner without camera access -> system Back returned
to Products. Granting camera access for this session opened the actual emulator
camera preview; Cancel returned to Products. Screenshots and the bound-service
dump are retained under `.codex/device-qa/suite-recovery/`.

This is limited active-TalkBack/window-focus and keyboard smoke, not a full manual
screen-reader certification. Spoken output and complete swipe traversal were not
verified; adb-injected gestures did not provide reliable traversal evidence.
The emulator's synthetic camera preview and automated EAN-13 image recognition
do not establish physical-camera scanning quality. The user will test that on
their own phone, as explicitly agreed during this follow-up.

## Changed sources

- `app/src/main/java/com/trainiq/core/sleep/SleepAlarmPlaybackService.kt`
- `app/src/main/java/com/trainiq/core/sleep/SleepRoutineScheduler.kt`
- `app/src/main/java/com/trainiq/features/home/HomeScreen.kt`
- `app/src/test/java/com/trainiq/features/home/HomeObservationTest.kt`
- `app/src/androidTest/java/com/trainiq/features/coach/CoachInsightsInstrumentedTest.kt`
- `app/src/androidTest/java/com/trainiq/features/sleep/SleepNotificationInstrumentedTest.kt`
- `app/src/androidTest/java/com/trainiq/features/workout/ExerciseHistoryInstrumentedTest.kt`
- `app/src/androidTest/java/com/trainiq/flow/TrainIqFlowSmokeInstrumentedTest.kt`

All app paths are relative to `TrainIQ-Project/`. Documentation: this guide and
the follow-up pointer in `docs/agent-guides/nutrition-scanner-energy.md`.
