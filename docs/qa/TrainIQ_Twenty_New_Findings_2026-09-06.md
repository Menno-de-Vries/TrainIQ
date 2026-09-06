# Twenty-finding rerun — 2026-09-06

Base: 2ea47df763d21930d6975bbe7ee269048261c606 (fresh origin/main). Branch: codex/twenty-new-audit-fixes. No unrelated cleanup or release.

Audit: onboarding, Home, typed navigation, workout/routine/library/active/completion flows, nutrition library/recipe/meal/scanner, Progress, Coach/profile and Settings; supporting use cases, coordinator, Room transactions/DAO, analytics and existing regression suites. Prior PRs #16–#19 inspected. Existing historical-week comparison and Room measurement ordering rejected as already intentional/correct.

Before-edit selection (P2 unless stated; implementation now complete, verification recorded below). Paths are relative to TrainIQ-Project/app/src/main/java/com/trainiq. Each row records current evidence, intended behavior and minimum proof. Each finding is bounded to existing behavior; supporting state helpers introduce no new product surface.

| ID | Observed problem / affected code | Expected smallest change | Regression proof |
|---|---|---|---|
| TWENTY-01 | nutrition/NutritionScreen.kt copies gramsUsed when editing a SNAPSHOT without scaling its nutrient values. | Scale snapshot nutrients with the gram ratio, retaining typed invalid input. | Resize up/down, repeated edits, food/recipe unchanged. |
| TWENTY-02 | LoggedMealItem.toMealEntrySnapshot copies totals for all servings; save multiplies them again. | Convert logged snapshot totals back to per-serving values when editing/reusing. | Multi-serving round trip and single serving. |
| TWENTY-03 | NutritionViewModel.analyze permits competing imported-photo requests, publishes late results and never releases imported files. | Own latest imported analysis and release temporary files on every exit, including no config. | Late completion, cancellation, failure/retry, disabled AI. |
| TWENTY-04 | Nutrition and CameraScanner photo-picker callbacks copy up to 6 MB synchronously on main. | Dispatch import IO safely off main and preserve cancellation/cleanup. | Controlled import dispatch, failure and cancellation; local scanner UI. |
| TWENTY-05 | CoachViewModel.saveProfile restores old advice after the user edits the draft during save; save also lacks a pending guard. | Single in-flight save; publish advice only for still-current draft. | Delayed save plus edit, duplicate submission, error/retry. |
| TWENTY-06 | WorkoutCompletionViewModel.load has uncaught storage failures and concurrent loads can publish an older session. | Own latest load, recover initial errors, retain valid summary on refresh failure. | Initial failure/retry, competing sessions, refresh failure. |
| TWENTY-07 | WorkoutViewModel overview/preferences flows terminate on error and route shows endless placeholder content. | Reloadable observations with an actual retry/error surface. | Flow error/retry and production UI retry. |
| TWENTY-08 | WorkoutViewModel routine/set/history mutations launch unhandled suspending writes. | Safe actionable failure feedback; never report success after failed writes. | Representative mutation failure/retry and cancellation. |
| TWENTY-09 | Settings preference/key/reminder actions launch uncaught writes. | Recover with safe feedback and retain observed values on failure. | Settings mutation failure/retry and cancellation. |
| TWENTY-10 | WorkoutProgressionSuggestionCalculator maps rep targets by exercise ID, so duplicate plan entries overwrite each other. | Evaluate each plan's own repetition target. | Same exercise with two distinct targets. |
| TWENTY-11 | Progression averages unset RPE=0 together with rated sets, hiding high effort and changing advice. | Average only finite recorded RPE in the existing valid range. | Mixed rated/unrated, all unknown, valid high effort. |
| TWENTY-12 | StartWorkoutSessionUseCase folds suggestions by exercise and seeds only the first duplicate plan; suggestion draft defaults NORMAL over a planned set type. | Seed each matching plan separately and preserve its set type. | Duplicate exercise plans and non-normal planned type. |
| TWENTY-13 | Active workout timers floor fractional remaining seconds and adjust from a once-per-second cached value, losing rapid adjustments. | Consistent ceiling countdown and adjustments from the current deadline, with immediate local update. | Subsecond boundary, expiry, consecutive adjustments. |
| TWENTY-14 | savePendingGeneratedRoutine marks pending only inside launch, allowing duplicate saves before dispatch. | Set pending synchronously and clear it on every terminal path. | Queued double tap, error/retry, cancellation. |
| TWENTY-15 | WorkoutCompletionScreen starts its 12-second auto-return while loading or showing an error, potentially leaving before any summary can be read or retried. | Start countdown only for a successfully loaded summary; keep errors/loading on screen and retain interaction cancellation. | Loading/error never navigate; success gets a full countdown; interaction cancels. |
| TWENTY-16 | saveMeal obtains original timestamp from asynchronously observed snapshot; immediate edit can move historical meal to today. | Preserve an existing meal's date inside the Room transaction. | Historical meal edit without waiting for Flow, database reopen. |
| TWENTY-17 | saveRecipe takes createdAt from observed state; immediate edit can replace original creation time. | Preserve existing creation time transactionally. | Back-to-back historical recipe edit and reopen. |
| TWENTY-18 | saveRecipe builds its return value using foods captured before save, so a just-saved ingredient can disappear from returned totals. | Build returned recipe from authoritative ingredient food data. | Save food then recipe without Flow wait; totals and IDs. |
| TWENTY-19 | Room saveFood permits editing one existing product to another product's barcode, leaving ambiguous barcode identity. | Reject conflicting edits without overwriting either product; preserve new-scan matching behavior. | Conflicting edit rollback, same-product edit, new scan matching. |
| TWENTY-20 | Home/nutrition/insight workout-calorie sums include today's draft or incomplete sessions. | Count only completed sessions consistently across all consumers. | Completed/draft/cancelled/other-day and local day boundary. |

Local baseline PASS: :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain (3m41s), on untouched base, Android Studio JBR and installed Android SDK. Existing nullable Java type warning in AppDialogAccessibilityTest. Evidence remains untracked under app/build.

## Local verification environment

Windows, Android Studio bundled JBR, installed SDK 36; debug variant. The initial read-only agent AVD did not boot and only its owned process was stopped. The installed `TrainIQ_Release_20260906` AVD runs read-only on `emulator-5586`, Android 16/API 36. An isolated adb server on port 5038 avoids the pre-existing unresponsive server; all device commands are scoped to that port and serial. No physical/user device, AVD data, credentials or live AI provider was used. SDK/device permissions were not changed to force acceptance.

Environment command reference: Android's [adb manual](https://android.googlesource.com/platform/packages/modules/adb/+/refs/heads/master/docs/user/adb.1.md) documents the server port option; the [emulator command-line reference](https://developer.android.com/studio/run/emulator-commandline) documents read-only launch options.

## Regression coverage

- 01-02: `MealPortionsTest` proves proportional edits, repeated resizing, unchanged library references, invalid input and per-serving round trips.
- 03: the production imported-photo paths use `LatestScanRequest`; extended `LatestScanRequestTest` proves rejected provider imports cancel older work and release both files. Existing late-provider, immediate cancellation and failure/retry tests are reused. Coordinator publication already checks cancellation on main and remains unchanged.
- 04: `ScannerImageImportTest` controls the IO dispatcher and checks thread ownership, unreadable input, retry and cancellation after copying. Progress uses the same import/analysis ownership to avoid leaking its copied files.
- 05: actual `CoachDraftStateTest` ViewModel tests cover edits during delayed save, duplicate queued submission, failure and retry.
- 06: `WorkoutCompletionLoaderTest` covers missing/failed initial reads, retry, competing sessions and retention after refresh failure.
- 07: `WorkoutObservationsTest` proves independent overview/preferences errors, retry and resumed updates; production error UI has a reachable retry test.
- 08-09: `UserActionTest` exercises the shared production write boundary used by Workout and Settings: failure leaves values unchanged and emits no success, retry succeeds, cancellation propagates including nested result handling. Real disk-full/Keystore faults are not injected on a user device.
- 10-11: `WorkoutProgressionSuggestionCalculatorTest` covers repeated exercise plans with distinct targets, mixed unrated/high-effort sets and all-unrated sets.
- 12: `StartWorkoutSessionUseCaseTest` exercises actual use-case seeding for duplicate plans and back-off type preservation.
- 13: `RestTimerTest` covers the final fractional second, expiry, elapsed-time adjustments, rapid queued taps, serialized writes, consecutive failures, rollback to the last successful value and retry.
- 14: `UserActionTest` proves the production single-submission helper rejects queued duplicate taps and clears pending on success, failure and cancellation. The existing routine preview source wiring guard now refers to this helper, with behavior tested independently.
- 15: production `WorkoutCompletionRecoveryInstrumentedTest` covers loading/error retention, retry and the full success countdown.
- 16-19: `TargetedRoomPersistenceInstrumentedTest` uses real Room transactions, back-to-back writes without Flow waits, preserved historical dates, returned authoritative ingredients, conflicting barcode rollback, new-scan matching and database reopen. All 33 cases passed during implementation. No schema or migration change.
- 20: `CompletedWorkoutCaloriesTest` covers completed/draft/incomplete/out-of-day records, an empty day and the local DST day boundary; all three consumers call this rule.

Implementation RED evidence: new progression assertions, duplicate-plan seeding, delayed Coach save behavior, and three Room date/conflict assertions failed before their respective fixes and passed after. Combined unit checks also caught an outdated routine guard assertion after extraction; it was updated to the new wiring while keeping its UI assertions and adding behavior coverage.

## Scope and remaining acceptance

Exactly TWENTY-01 through TWENTY-20 are selected. No dependency, schema, navigation contract, model/provider, permission, release policy or generated artifact is changed. Existing correct historical-week comparison and measurement ordering remain. Unrelated worktrees/branches are preserved. No remote tests, merge, release or cleanup is authorized by this rerun.

Final local commands, runtime evidence and PR commit/check status follow below after verification. Physical-device performance, live AI, camera hardware and TalkBack are not claimed verified by this emulator/unit pass.

Selection refinement before editing TWENTY-15: routine generation dismissal is already blocked by RoutineGeneratorDialog's onDismiss guard. Replaced that unreachable candidate with observed completion countdown behavior.

## Manual walkthrough (debug, local emulator)

- Baseline onboarding: welcome/goals, Health Connect skip, AI skip, local setup completion and guided tour through Home, Training and Nutrition. No provider credentials or Health permissions were entered.
- Changed APK cold launch succeeds. At the existing 720x1280 override and font scale 1.3, visited Home discovery mode, Training empty routine state, Nutrition daily totals and the meal-source sheet, Coach Week/Goals, Settings and the Progress route.
- Visually inspected captures of Coach empty profile state, Progress measurement/import controls, Settings switches and Nutrition totals/source actions. Captures and UI trees stay untracked in `app/build/audit-*.png` and `app/build/audit-ui.xml`.
- Opened and cancelled the Android photo picker from Progress; navigated back and among top-level routes. Changed workout haptics in Settings and observed the updated switch semantics plus success snackbar.
- This manual pass does not simulate actual provider analysis or disk failure. Deterministic tests cover those cancellation/error boundaries. Actual active-workout edits/restoration, long-form IME and saved-state paths are covered by the selected existing Android classes below.
## Combined evidence and baseline limitations

Run from `TrainIQ-Project/`, with session environment:

```powershell
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
$env:ANDROID_HOME='C:/Users/menno/AppData/Local/Android/Sdk'
$env:ANDROID_ADB_SERVER_PORT='5038'
$env:ANDROID_SERIAL='emulator-5586'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

Implementation baseline: PASS, 879 unit tests; lint 0 errors / 65 warnings. Local JDK/SDK tooling reports its existing SDK XML version warning; no SDK/dependency upgrade was performed.

Broad affected Android run: `:app:connectedDebugAndroidTest` with `-Pandroid.testInstrumentationRunnerArguments.class=` and these exact comma-separated classes:

```text
com.trainiq.features.workout.WorkoutCompletionRecoveryInstrumentedTest,com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest,com.trainiq.features.workout.ActiveWorkoutRestoreInstrumentedTest,com.trainiq.features.workout.GeneratedRoutinePreviewInstrumentedTest,com.trainiq.features.workout.WorkoutAiRoutineGenerationStateRestorationInstrumentedTest,com.trainiq.features.coach.CoachProfileStateRestorationInstrumentedTest,com.trainiq.features.nutrition.NutritionAiResultStateRestorationInstrumentedTest,com.trainiq.features.nutrition.NutritionLongFormImeInstrumentedTest,com.trainiq.features.nutrition.ScannerRecoveryInstrumentedTest,com.trainiq.features.FeatureRecoveryInstrumentedTest,com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest,com.trainiq.core.database.TrainIqDatabaseMigrationTest
```

Result: **65/67 PASS, 2 existing failures, 0 skipped**. In particular: Room persistence 33/33, migration 11/11, completion/retry/interaction 4/4, actual active-set edit/delete flow 1/1, Coach saved-state 4/4, nutrition result restoration 3/3, long-form IME 1/1 and scanner recovery 1/1 passed.

The two failures were reproduced **unchanged on current main 2ea47df763d21930d6975bbe7ee269048261c606**, using a clean detached baseline worktree and the same emulator/configuration. Baseline command: `:app:connectedDebugAndroidTest` filtered to `com.trainiq.features.workout.ActiveWorkoutRestoreInstrumentedTest,com.trainiq.features.workout.WorkoutAiRoutineGenerationStateRestorationInstrumentedTest` (2/4 PASS, identical 2 failures):

- `activeWorkoutRestoresFromRoomAfterActivityRecreation`: times out at line 109 looking for the text `Training` before recreation. The compact short navigation bar exposes its accessible description and icon, not that text label.
- `aiRoutineDraftFieldsSurviveStateRestorationBeforeGenerate`: expects `advanced`, receives `intermediate` after the existing selection/restoration sequence. The same mismatch occurs on main; no speculative product/test repair is included.

These tests remain intact. Their failure is not classified as success, and full Android-suite/device certification is not claimed. Earlier historical failures documented in the findings index were not treated as evidence for these two; fresh current-main evidence is retained at `artifacts/twenty-audit/baseline-results.xml`, alongside `broad-results.xml`. Long logcat filenames exceed this PowerShell copy environment's path limit, so the compact XML results are the retained comparison evidence.

A final timer rollback review also restores its completion flags with the confirmed deadline. The invalidated workout subset and migration marker are rerun with the final code; unchanged passing lower-layer/UI evidence is reused. The migration marker task itself depends on connected tests, so it is scoped to the owned serial and migration class, alongside the affected workout classes. No generated marker is committed.

No hosted workflow is manually dispatched; GitHub checks are reported separately from local test evidence. The final commit SHA and exact check snapshot are recorded in the PR to avoid a self-referential documentation commit.
Final-code subset: **16/16 PASS** for `com.trainiq.core.database.TrainIqDatabaseMigrationTest,com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest,com.trainiq.features.workout.WorkoutCompletionRecoveryInstrumentedTest`. The separate `:app:generateDebugRoomMigrationChainVerificationMarker` invocation with that filter **PASS** (40 seconds). Batching that existing marker task with lint first failed Gradle's implicit-output-dependency validation; running the documented separate task resolved it without code/config changes. Generated evidence remains untracked.
## Requested follow-up: resolve both existing restoration-test failures

The owner explicitly requested resolving both failures before further work. No product change was necessary:

- Active workout test now initializes its own completed onboarding/tour preferences, finds the accessible Training navigation action on both compact and labelled layouts, opens the actual active workout, and verifies the logged-set correction action before and after activity recreation. The former test depended on previous test state and stopped at a hidden text label; it never tested the active workout itself.
- AI routine test now scrolls the advanced-level chip into view before a real click and asserts selection both before and after saved-state restoration. Diagnostic RED evidence placed an assertion immediately after the old click: `Selected=false` already before restoration. Thus the old test never selected the off-screen option; the app's saved state was correct.

Focused verification: both complete test classes PASS, 4/4, on the same API 36 read-only emulator at 720x1280 / font 1.3. No expectation, valid assertion or test was removed to conceal a failure. The full 67-case affected matrix and local baseline are rerun for the follow-up commit, with exact final results in PR #20. The earlier failure sections above remain historical evidence, superseded by this follow-up.