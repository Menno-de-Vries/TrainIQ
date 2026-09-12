# Nutrition, scanner and energy compass

The interrupted full-suite result below is historical. The
[suite recovery follow-up](nutrition-suite-recovery.md) fixes the sleep start/cancel
race and obsolete test fixtures. Final local verification on 2026-09-13 passed
all 154 connected tests and 917 JVM tests, with zero failures/errors/skips.

Task base: `f06c3f8e7f59962ffae86bc59de50743c370fc82`, primary worktree
`C:/My-PC-Files/GitHub/TrainIQ`, branch `codex/nutrition-scanner-energy`.

## Contracts

- Meal review keeps grams and serving counts. `Telt mee als vocht` maps valid grams
  one-to-one to per-serving ml; unchecked maps to zero. This is a product rule,
  not density inference. Barcode beverage metadata only seeds the editable choice.
  Manual hydration retains its independent explicit-volume flow.
- Existing Room hydration fields and aggregation remain authoritative. There is no
  schema change, migration or backfill. Explicitly re-saving an item applies the new rule.
- Scanner destinations carry a compact meal/product/recipe intent and optional
  meal category in the typed route. Existing saveable parent drafts retain fields
  and ingredients; the parent ViewModel retains the pending intent in SavedStateHandle.
  Resolved barcode products are handed back in memory; a pending barcode can be
  looked up again after process loss.
- Frame processing is single-flight on a dedicated executor. Empty or failed frames
  release their ImageProxy and allow another frame. A repeated unsuccessful code
  is suppressed until explicit retry; a different code can start another lookup.
  Success has a consumption guard. Back and modal dismissal share scanner exit.
- Home uses the existing dashboard macro data and MacroBreakdownCard immediately
  below EnergyBalanceCard. Meal lists and meal-category overviews remain absent.

## Platform reference

Checked against pinned ML Kit 17.3.0: [ML Kit barcode scanning](https://developers.google.com/ml-kit/vision/barcode-scanning/android).
Potential barcodes and zoom suggestions are supported. Zoom options use the
CameraX camera's available min/max ratio; existing pinch controls remain enabled.
All formats remain enabled to preserve the existing 8–14 digit acceptance path.
Material [bottom-sheet dismissal](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
uses the same exit callback as explicit cancellation; no custom navigation bypass
or predictive-back override was introduced.

## Local verification

Evidence is recorded after the final checks. Generated reports, screenshots and
emulator logs stay untracked under `app/build/` and
`.codex/device-qa/nutrition-scanner-energy/`.

Environment: Windows PowerShell, Android Studio JBR, installed local Android SDK.
The existing agent AVD (`TrainIQ_Agent_API36_20260806`) was started as
`emulator-5580`, but test installation failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
Its installation was preserved and that emulator stopped. One new task-owned AVD,
`TrainIQ_Agent_Nutrition_20260912`, uses the already installed API 36 Google Play
x86_64 image. Its first windowed start failed in emulator initialization; the
headless `-gpu swiftshader -no-snapshot` start booted successfully. Tests use this
AVD as `emulator-5580`, 360 x 640 dp, font scale 1.3. No wipe, download,
physical-device use or live provider requests.
No push, PR, merge, deploy, release or hosted test execution.

## Regression development evidence

- The first complete JVM run exposed outdated source assertions for the former
  target enum and the former Product Library detour. Assertions now describe the
  shared intent and contextual meal review. The adaptive-route source assertion
  retains its original check; the width argument was moved before callbacks.
- The first installed connected selection ran 34 tests: 32 passed. The new cancel
  test matched both the modal and underlying cancel buttons; it now selects the
  modal action explicitly. A real recipe test queried the next screen during an
  input/navigation transition; it now dismisses the keyboard and waits for that
  destination while retaining every Room amount, calorie, edit and delete assertion.
- The corrective four-class run passed all 16 tests, including recipe persistence,
  scanner recovery, AI result restoration/cancellation, and Home macro semantics.
- Screen lifetime guards discard late captured/imported photos after exit. Barcode
  exit closes request admission as well as cancelling the current lookup.
- The new navigation enum uses `Keep`, consistent with the pinned Navigation/R8
  boundary. Existing MealType keep rules remain unchanged.

## Changed paths

Production paths below are relative to `TrainIQ-Project/app/src/main/java/com/trainiq/`:

- `features/nutrition/NutritionScreen.kt`: shared saved destination, cancellation
  restoration, recipe lookup recovery and gram-derived hydration review.
- `features/nutrition/MealHydration.kt`: validated per-serving product rule.
- `features/nutrition/BarcodeProductScannerRoute.kt`: duplicate suppression,
  explicit retry, persistent last barcode, single consumption and closed exits.
- `features/nutrition/CameraScannerScreen.kt`: frame lifetime, executor, scanner
  options, modal/system exit and stale image guards.
- `navigation/TrainIqNav.kt`: compact typed intent and consumed cancellation event.
- `features/home/HomeScreen.kt`, `core/util/Formatters.kt`: macro card and safe,
  labelled progress with readable compact rows.

Tests below are relative to `TrainIQ-Project/app/src/`:

- `test/java/com/trainiq/features/nutrition/MealHydrationTest.kt`
- `test/java/com/trainiq/features/nutrition/NutritionScanDestinationTest.kt`
- `test/java/com/trainiq/features/nutrition/BarcodeProductScannerViewModelTest.kt`
- `test/java/com/trainiq/features/nutrition/NutritionInputValidationTest.kt`
- `androidTest/java/com/trainiq/features/nutrition/BarcodeFrameInstrumentedTest.kt`
- `androidTest/java/com/trainiq/features/nutrition/BarcodeMealFlowInstrumentedTest.kt`
- `androidTest/java/com/trainiq/features/nutrition/ScannerRecoveryInstrumentedTest.kt`
- `androidTest/java/com/trainiq/features/nutrition/MealDetailPersistenceTest.kt`
- `androidTest/java/com/trainiq/features/nutrition/NutritionAiResultStateRestorationInstrumentedTest.kt`
- `androidTest/java/com/trainiq/features/nutrition/NutritionLongFormImeInstrumentedTest.kt`
- `androidTest/java/com/trainiq/features/FeatureRecoveryInstrumentedTest.kt`

Documentation: this guide, `TrainIQ_Target_State_Blueprint.md`,
`docs/agent-guides/coach-hydration-food-providers.md`, and
`docs/agent-guides/input-meal-alarm-flows.md`.

## Final verification

All commands below ran from `TrainIQ-Project/`, with Android Studio JBR in
`JAVA_HOME`, the installed SDK in `ANDROID_HOME`, `ANDROID_SERIAL=emulator-5580`,
and `--console=plain --max-workers=2`.

- PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`;
  917 JVM tests, zero failures/errors. Lint: zero errors, 68 warnings. Android test
  Kotlin compilation also passed, both explicitly and through connected tasks.
- FAIL: the full `:app:connectedDebugAndroidTest` run planned 153 tests, executed
  126 (122 passed, four failed), and aborted on
  `SleepRoutineInstrumentedTest.roomReopenPreservesConfirmationAndReconciliationPlansNextDay`:
  `SleepAlarmPlaybackService` raised `ForegroundServiceDidNotStartInTimeException`.
  The remaining 27 tests did not execute; they are not recorded as passing/skipped.
  `CoachInsightsInstrumentedTest` also timed out waiting for its initial Coach
  navigation text. Neither Coach nor sleep production/test code was modified.
- Two additional failures in that full run were corrected at their test boundary:
  the 150%-font Nutrition test now scrolls to its offscreen add button and explicitly
  closes the IME; scanner back now injects the actual Android BACK key rather than
  asking Espresso to select the unfocused application window behind a modal.
  Assertions remain intact; no tests were skipped or disabled.
- PASS: final `:app:connectedDebugAndroidTest` with
  `-Pandroid.testInstrumentationRunnerArguments.class=` selecting the eleven classes
  below, followed by `:app:lintDebug`: **36/36**, zero failures/skips.
  `ScannerRecoveryInstrumentedTest`, `NutritionLongFormImeInstrumentedTest`,
  `BarcodeFrameInstrumentedTest`, `BarcodeMealFlowInstrumentedTest`,
  `NutritionAiResultStateRestorationInstrumentedTest`, `BarcodeRecognitionInstrumentedTest`,
  `CameraPermissionScannerInstrumentedTest`, `HydrationUiTest`, and
  `MealDetailPersistenceTest` are in `com.trainiq.features.nutrition`;
  `FeatureRecoveryInstrumentedTest` is in `com.trainiq.features`;
  `HydrationPersistenceTest` is in `com.trainiq.core.database`.
- Retained full-suite evidence includes passing migration coverage, including
  `migration17To18PreservesHistoryWithoutHydrationBackfill`. No schema was changed.

Authoritative retained XML: `.codex/device-qa/nutrition-scanner-energy/full-suite.xml`
and `scoped-suite.xml`. The full-suite failure remains open; the whole suite was
not repeated after unrelated Coach/sleep failures. Dark compact captures of Home,
macro targets, hydration review and scanner recovery were visually inspected.

NOT RUN: physical-camera scan throughput/zoom smoke and human TalkBack/Switch Access.
No suitable physical device was connected. Emulator image recognition, semantics
and frame contracts do not establish real-world scan performance. Release/signing,
deployment, hosted testing and live food-provider/AI requests were outside scope.

- PASS: three compact light-theme tests at font scale 1.3, selecting
  `FeatureRecoveryInstrumentedTest#macroProgressHasTargetsAndSafeMissingGoalSemantics`,
  `BarcodeMealFlowInstrumentedTest#scannedDrinkReachesPersistentHydrationTotal`, and
  `ScannerRecoveryInstrumentedTest#barcodeResolvingAndMissingResultExposeRetryAndManualActions`.
  The scanner light capture was inspected. An additional two-test capture attempt
  also passed, but UTP uninstalled the target before the app-private external macro
  and hydration captures could be copied; those light images are not claimed as
  retained visual evidence. Their dark captures and light semantics checks remain available.

The task-created AVD is retained at
`C:/Users/menno/.android/avd/TrainIQ_Agent_Nutrition_20260912.avd` for reuse.
The emulator process is stopped at handoff. The original AVD and its differently
signed TrainIQ installation were not cleared or uninstalled.
