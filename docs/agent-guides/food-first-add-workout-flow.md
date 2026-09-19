# Food first add and workout input evidence

Scope: local-only implementation on `codex/food-first-add-workout-flow`, based on clean `main` / `origin/main` at `90c026c76e18696bec8e4752f12d8475bf7a2e95` (2026-09-20). No dependency, schema, permission, remote service or release changes.

## Diagnosis and decisions

- All meal sources converge on the nutrition draft. Its saveable default date was captured when the screen was first composed. After a day change/restoration, the first meal was stored on the old day and absent from today's list; successful save then reset the date, making a subsequent attempt appear to work. A regression failed with September 19 instead of September 20 before the fix. Save only an explicit date override; resolve an implicit today at commit time. Editing a historical meal and explicitly selected dates retain their date.
- The shared outside-tap handler treated `awaitTouchSlopOrCancellation() == null` as a tap, including gestures consumed by child controls. Its regression lost input focus on a child button click before the fix. Require a completed, unconsumed up gesture instead. A consumed or cancelled gesture must not dismiss the keyboard via the parent. The focused component test also checks two button actions and a real background tap.
- Real `NutritionViewModel -> use cases -> repository -> Room -> uiState` tests cover fresh first save, duplicate pending requests, sequential saves, missing-reference failure, retry, exactly-once callbacks/rows and activity recreation. Existing pending-submit and stable meal-ID guards already satisfy deduplication; no extra debounce or persistence rewrite was added. A real navigation/manual-input test sends touch events with the keyboard open and verifies the persisted calories.
- These are reproduced defects in the shared paths, not proof that every intermittent incident has the same cause. Barcode/AI provider behavior is tested with deterministic inputs, without live camera/cloud calls.
- Active workout, routine set editing and direct exercise input no longer ask for RPE. Keep persisted fields, mappers, schemas, import/export and historical readings compatible. New logged sets use the existing unknown sentinel `rpe = 0.0`, `repsInReserve = null`; corrections preserve actual historical effort. A hidden/stale draft RPE cannot block logging or masquerade as reported effort. Copy/relog routes converge on `logSet`.
- Keep existing objective volume, estimated 1RM and conservative progression behavior. No new formula claims to infer exact perceived effort from weight/repetitions.
- Numeric workout inputs show values only; labels/semantics carry kilogram/seconds. Three cells replace four; completed rows use completion styling rather than effort color. Input and row action targets are at least 48 dp. IME Next follows repetitions -> weight -> rest; Done ends editing without submitting the entire set. Existing card identity, set type, rest timer, correction and delete confirmation remain.
- Zero recorded weight/rest is displayed as zero instead of being replaced with planned values. This preserves meaningful bodyweight/no-rest results.
- Visual inspection exposed an enormous 1RM preview for rejected input. The preview now reuses set-input validation and suppresses invalid/zero-weight estimates; the existing strength formula is unchanged. `ExercisePlanValidationTest.liveStrengthPreviewRequiresValidObjectiveInput` failed before this fix and passed afterward.
- At font scale 1.5, the empty-history/plan texts touched each other, and a scrolled routine set-type chip could move under the transparent fixed drag handle. The plan uses a wrapping row; the editor reserves the handle's 72 dp above its scroll viewport and keeps the visible handle there. The existing restoration test reproduced the missed chip selection before this layout fix.
- Physical screenshots also exposed window panning in addition to Compose IME padding: focused input could move above the status bar while blank space appeared above the keyboard. `MainActivity` now explicitly uses `adjustResize`, matching its existing edge-to-edge/IME-inset handling and Android's documented setup. Before/after captures on API 36 show the three labeled inputs visible above the numeric keyboard at 150% text after the fix. This configuration affects the main activity, so navigation/flow smoke checks are included in the final scope.

## Local verification

Environment: bundled Android Studio JBR, installed SDK, isolated existing `TrainIQ_Agent_Nutrition_20260912` AVD on `emulator-5580`, Android 16/API 36. No physical device was connected or used. Agent launched this AVD with no snapshot, no window and no audio; no AVD/image/download was created. Small profile: 720x1280 px at 320 dpi (360x640 dp), light mode, font scale 1.3.

PowerShell setup from `TrainIQ-Project/`:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SERIAL='emulator-5580'
```

Focused red/green evidence:

- `:app:testDebugUnitTest --tests 'com.trainiq.features.workout.WorkoutInputValidationTest'`: five expected failures before implementation; later all affected nutrition/workout JVM tests passed (233 tests).
- `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.nutrition.BarcodeMealFlowInstrumentedTest`: date regression failed before implementation, then passed with restoration and explicit-date coverage.
- `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.ui.TapOutsideFocusInstrumentedTest`: child-focus regression failed before the handler fix, passed after it; a first attempted Final-pass implementation failed the background-tap assertion and was corrected to the standard Main-pass helper.
- During workout test development, a new physical click after deletion failed. A screenshot and bounds probe showed the transient deletion snackbar covered the log button. The test now waits for that message to disappear and for IME layout to settle before one click. No save assertion was removed or replaced with a ViewModel call.

Final baseline/affected command (result recorded after completion):

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.package=com.trainiq.features.workout,com.trainiq.features.nutrition,com.trainiq.core.ui' --console=plain --max-workers=2
```

Build, unit (918 tests, zero failures/errors/skips), lint (zero errors, 68 warnings; none attributed to the affected screen files) and all 60 connected tests passed. Later visual findings triggered the focused follow-up below; final follow-up results are recorded separately.

Follow-up after visual fixes:

```powershell
# Dark mode and font scale 1.5; same 360x640 dp AVD
.\gradlew.bat :app:testDebugUnitTest '--tests=com.trainiq.features.workout.*' :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest,com.trainiq.features.workout.PlanDraftRestorationInstrumentedTest,com.trainiq.features.workout.ExerciseEditorRecoveryInstrumentedTest' --console=plain --max-workers=2
# Restore light / 1.3; final code baseline and all workout instrumentation
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.package=com.trainiq.features.workout' --console=plain --max-workers=2
```

Both passed: all focused workout unit tests and 7 dark/large-font connected tests; final build, 919 unit tests (zero failures/errors/skips), lint and 22 workout connected tests. The unchanged nutrition/shared handler retain their passing evidence from the 60-test run.

Visually inspected normal, editing, invalid input, completed and empty workout captures on the small profile, plus dark/1.5 completed and empty states. A Gboard font-change notice obstructed the first dark keyboard captures; they were rejected as visual evidence and the active-workout capture repeated after the notice disappeared. No emulator image/package or keyboard setting was changed to bypass a test.

After the window configuration fix, the dark/1.5 active-workout and real nutrition tests passed (3 tests). The editing/error screenshots were recaptured and inspected successfully. Final configuration verification:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.package=com.trainiq.features.workout,com.trainiq.features.nutrition,com.trainiq.core.ui,com.trainiq.navigation,com.trainiq.flow' --console=plain --max-workers=2
```

Final result: **BUILD SUCCESSFUL**, 65 connected tests, no failures/skips; build/lint passed and the unchanged 919-test unit result was reused by Gradle. Final light/1.3 captures were collected; the crash log buffer was empty. Restored the AVD's original light mode/font scale 1.3 and stopped only the emulator launched by this task. `git diff --cached --check` passed; exact staged paths contain source/tests/documentation only.

Core changed paths under `TrainIQ-Project/`: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/trainiq/core/ui/AppDesign.kt`, `app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`, and `app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`, plus matching unit/instrumented regression tests. No push, PR, merge or release was performed.

Generated XML/HTML reports, screenshots and emulator artifacts remain untracked. Local captures are under `.codex/device-qa/food-workout/` in the repository root. No performance, physical-device, live AI/barcode-network or TalkBack-service claim is made; semantics and focus traversal are exercised through Compose instrumentation. No migration/release gate is applicable to this patch.

## Worktree safety

Primary worktree identified from Git's common directory: `C:/My-PC-Files/GitHub/TrainIQ`. Nine other worktrees are detached and clean for tracked/untracked files, but contain ignored Gradle/Kotlin/build outputs. Retained all: no exact merged PR-head/task-ownership/inactivity proof sufficient for the repository cleanup contract, and no authority to discard ambiguous artifacts. No branches or worktrees were deleted.

Retained paths under `C:/My-PC-Files/GitHub/`:

- `TrainIQ-apk-ca4aabf6`
- `TrainIQ-main-apk-47fdfb69`
- `TrainIQ-main-apk-8cbbb164`
- `TrainIQ-main-apk-90c026c7`
- `TrainIQ-main-apk-910afc79`
- `TrainIQ-main-apk-a5f5e339`
- `TrainIQ-main-apk-a7120d09`
- `TrainIQ-main-apk-a8ff65a3`
- `TrainIQ-main-apk-f06c3f8e`

## Primary references

- [Compose gesture handling and event consumption](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures).
- [Compose focus traversal](https://developer.android.com/develop/ui/compose/touch-input/focus/change-focus-behavior).
- [IME actions](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/input/ImeAction).
- [Android accessible touch target size](https://support.google.com/accessibility/android/answer/7101858?hl=en).
- [Compose edge-to-edge and adjustResize](https://developer.android.com/develop/ui/compose/system/setup-e2e).

Repository versions/patterns remained authoritative; no library upgrades were necessary.
