# TrainIQ QA Findings To Improve

Audit date: 2026-05-09

Refresh date: 2026-05-10, current worktree

Scope: target-state blueprint, Android app source, Gradle/CI, docs, tests, emulator smoke, Health Connect, Gemini/AI, Room/data, UI/UX/accessibility, release readiness.

## Findings

### QA-2026-05-09-001

- finding_id: QA-2026-05-09-001
- priority: P0
- area: data, performance, Android lifecycle
- status: partially-done
- owner suggestion: Android data/platform owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt:141` serializes the updated full app state with `gson.toJson(updated)`.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/migration/JsonRoomImportPlanner.kt:327` clears mirror tables during mirror imports before broad reinsert/upsert work.
  - The refreshed 2026-05-09 emulator smoke launched successfully but `am start -W` reported `WaitTime: 6446`, and `gfxinfo` reported 6 janky frames out of 8 rendered frames, keeping the startup performance risk open.
  - Current worktree moves active-workout rest timer updates from full JSON mirror import to a targeted `UPDATE active_workout_sessions` path through `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/TrainIqDao.kt`, `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt`, and `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`.
  - Current worktree moves active-workout draft updates from full JSON mirror import to a targeted `active_workout_drafts` upsert plus `active_workout_sessions.updatedAt` update through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout discard from full JSON mirror import to targeted deletes for workout log-event snapshots, workout log events, active workout session children, performed exercises, and the draft workout session through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout set logging from full JSON mirror import to targeted upserts for the active session, draft, active set, undo log event, and undo snapshot rows through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout set editing from full JSON mirror import to targeted active set, draft, rest-timer, and current undo snapshot updates through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout set type editing from full JSON mirror import to targeted active set type, current undo snapshot type, and session timestamp updates.
  - Current worktree moves active-workout collapse/expand toggles from full JSON mirror import to targeted `active_workout_collapsed_exercises` insert/delete plus session timestamp update.
  - Current worktree moves body measurement add/delete from full JSON mirror import to targeted `body_measurements` insert/delete paths through the same DAO/runtime-store/repository path.
  - `TrainIQ-Project/app/src/test/java/com/trainiq/architecture/RoomAuthorityArchitectureTest.kt` now guards that `updateActiveWorkoutRestTimer(...)`, `updateActiveWorkoutDraft(...)`, `logActiveWorkoutSet(...)`, and `discardActiveWorkout(...)` use targeted Room updates/deletes instead of `runtimeStore.update { ... }`.
- expected target-state behavior: Normal user mutations use bounded targeted DAO transactions. Startup and critical input paths do not perform full-state JSON serialization, broad import planning, or broad mirror table replacement.
- concrete recommended fix: Keep JSON import for legacy/import tooling only. Add targeted DAO-backed repository mutations for active workout logging, meal save/delete, routine edit/delete, measurement edit/delete, finish/discard, and profile writes. Add a regression guard that these hot paths do not call `RoomTrainIqRuntimeStore.update()`.
- regression risk: High. This touches persistence and process-restart correctness; migrate flow by flow behind tests instead of replacing all mutations at once.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest --console=plain`, plus an active-workout logging smoke with `adb shell dumpsys gfxinfo com.trainiq framestats`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/TrainIqDao.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/architecture/RoomAuthorityArchitectureTest.kt`
- verification evidence:
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 9163`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8013`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 6942`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 11091`; crash buffer was empty.
  - 2026-05-10 baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
- external sources used: None. Local Room DAO patterns and existing architecture tests were sufficient; no Android/Room API ambiguity blocked this batch.
- remaining risk: This moves the rest timer, active draft, active set logging/editing/type editing, active collapse/expand, active discard, and body measurement add/delete paths to targeted Room persistence. Finish, meal save/delete, routine edit/delete, and profile writes still need targeted DAO migrations and process-restart correctness tests before QA-001 can close.

### QA-2026-05-09-002

- finding_id: QA-2026-05-09-002
- priority: P0
- area: release, privacy, security
- status: blocked
- owner suggestion: product/legal/release owner
- current evidence with file references:
  - `TrainIQ-Project/docs/release/owner-action-tracker.md:5` marks release status as blocked.
  - `TrainIQ-Project/docs/release/play-console-owner-checklist.md` requires owner confirmation for Health Apps declaration, Data Safety, privacy policy URL, and signing.
  - `TrainIQ-Project/app/src/main/AndroidManifest.xml:4` through `TrainIQ-Project/app/src/main/AndroidManifest.xml:12` declare camera, internet, six Health Connect read permissions, and background health read.
- expected target-state behavior: Play submission, Data Safety, Health Connect declarations, privacy policy, signing, and production AI boundary are explicitly approved before release.
- concrete recommended fix: Complete the release owner checklist, decide and document production AI mode, approve Data Safety answers, confirm background Health Connect read justification, and record signing ownership.
- regression risk: Medium. Documentation and release gate changes are low code risk but high compliance risk if inaccurate.
- minimal verification command/check: Review `TrainIQ-Project/docs/release/owner-action-tracker.md` and confirm all P0 owner gates are closed or have written release exceptions.

### QA-2026-05-09-003

- finding_id: QA-2026-05-09-003
- priority: P0
- area: accessibility, UX, release
- status: blocked
- owner suggestion: accessibility/manual QA owner
- current evidence with file references:
  - `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md:26` through `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md:35` list required TalkBack/Switch Access flows as `NOT_RUN`.
  - `TrainIQ-Project/docs/qa/talkback-switch-access-test-script.md` defines the manual flow but final signoff remains unchecked.
- expected target-state behavior: Active workout, scanner, Health Connect rationale, AI routine generation, Settings destructive actions, font scaling, and dark mode have signed TalkBack and Switch Access evidence before release.
- concrete recommended fix: Run the manual accessibility script on current build, attach evidence paths, update signoff, and file code issues for failed flows.
- regression risk: Low for documentation, potentially medium for UI fixes discovered by the pass.
- minimal verification command/check: Complete `TrainIQ-Project/docs/qa/talkback-switch-access-test-script.md` on a device/emulator with TalkBack and Switch Access enabled.

### QA-2026-05-09-004

- finding_id: QA-2026-05-09-004
- priority: P1
- area: Android lifecycle, performance
- status: done
- owner suggestion: Android app owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/home/HomeScreen.kt:129` starts a `while (true)` refresh loop from `HomeViewModel`.
  - The loop refreshes dashboard and Health Connect status on a timer even if the Home destination is retained but no longer visible.
- expected target-state behavior: Periodic foreground refresh is visible-lifecycle aware and does not keep retained off-screen top-level destinations doing Health Connect/dashboard work.
- concrete recommended fix: Move periodic refresh triggering to lifecycle-aware UI collection or a visibility signal, and keep the ViewModel refresh API idempotent. Add a test for off-screen Home not scheduling refresh work.
- regression risk: Medium. Home freshness can regress if lifecycle boundaries are too strict.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "*Home*" --console=plain` plus manual top-level navigation with log evidence that refresh pauses off-screen.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/home/HomeScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/home/HomeDashboardRefreshTest.kt`
- verification evidence:
  - RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --console=plain` failed while `HomeViewModel` still owned the retained periodic `while (true)` refresh loop.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --console=plain`
- remaining risk: Periodic refresh now runs under `HomeRoute` `repeatOnLifecycle(Lifecycle.State.STARTED)`, but manual top-level navigation log evidence that refresh pauses off-screen was not captured in this pass.

### QA-2026-05-09-005

- finding_id: QA-2026-05-09-005
- priority: P1
- area: performance, release
- status: blocked
- owner suggestion: product/Android performance owner
- current evidence with file references:
  - `TrainIQ-Project/docs/qa/performance-threshold-decision-record.md` still requires product confirmation for numeric thresholds.
  - `TrainIQ-Project/macrobenchmark/src/main/java/com/trainiq/macrobenchmark/TrainIqStartupBenchmark.java` defines startup/frame benchmarks, but emulator benchmark results are not release-certifying.
  - 2026-05-09 debug emulator smoke remains risky: latest `am start -W` reported `WaitTime: 6446`; latest `gfxinfo` showed 6/8 janky frames on the first-draw sample.
- expected target-state behavior: Profileable/release startup, navigation, active workout logging, scanner launch, and settings scroll have approved p50/p95/jank thresholds and physical-device evidence.
- concrete recommended fix: Set numeric thresholds, run macrobenchmarks on at least one physical lower-end device and one representative modern device, and track profileable/release results separately from debug emulator signals.
- regression risk: Low for measurement setup, medium if performance fixes alter startup/data flow.
- minimal verification command/check: `./gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain` on a physical device with approved benchmark suppression policy only when justified.

### QA-2026-05-09-006

- finding_id: QA-2026-05-09-006
- priority: P1
- area: security, privacy, backend
- status: needs-decision
- owner suggestion: product/backend/security/legal owner
- current evidence with file references:
  - `TrainIQ-Project/docs/architecture/ai-gateway-decision-record.md` keeps the production AI boundary unresolved.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/remote/GeminiApi.kt:14` correctly sends the API key in `x-goog-api-key`, and app services use Gemini 2.5 Flash, but the current production mode remains BYOK/direct-client unless changed.
- expected target-state behavior: Production release chooses one signed-off AI mode: BYOK accepted, backend gateway, OAuth-mediated access, hybrid, or AI scoped out.
- concrete recommended fix: Close the AI boundary decision record and update release/privacy/Data Safety docs to match the chosen mode.
- regression risk: High if changing architecture from BYOK to backend; low if documenting a BYOK MVP exception.
- minimal verification command/check: Confirm `TrainIQ-Project/docs/architecture/ai-gateway-decision-record.md` has a final decision and owner signoff.

### QA-2026-05-09-007

- finding_id: QA-2026-05-09-007
- priority: P1
- area: backend, UX
- status: done
- owner suggestion: AI/platform owner
- current evidence with file references:
  - Prior evidence found `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiSupport.kt` only mapping HTTP 429 into a typed AI rate-limit exception and no explicit `withTimeout` policy.
  - Current worktree adds `AiFeature` timeout budgets, `AiTimeoutException`, `withTimeout(...)`, and cancellation propagation in `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiSupport.kt`.
  - Current worktree adds feature-scoped in-memory 429 throttles through `AiFeatureThrottle` and `AiFeatureThrottledException` in `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiSupport.kt`.
  - Gemini meal scan, workout debrief, goal advice, weekly report, and routine generation now call `callGeminiWithBoundedRetry(feature = ...)` in `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiServices.kt` and `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/RoutineGeneratorService.kt`.
  - Routine generation rethrows rate-limit/throttle failures so the existing snackbar path can show `toAiUserMessage(...)`; other AI features keep local fallback output with existing fallback copy/source markers.
- expected target-state behavior: Every AI feature has explicit timeout, cancellation, retry, fallback, rate-limit, and user-message policy.
- concrete recommended fix: Add central typed AI result/failure mapping, per-feature timeout constants, cancellation propagation, and feature throttles. Cover Gemini timeout, 429, invalid JSON, offline, and local fallback in tests.
- regression risk: Medium. Timeouts can prematurely fallback on slow but valid responses if set too aggressively.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "*Ai*" --console=plain`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiSupport.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiServices.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/RoutineGeneratorService.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/ai/services/AiServicesTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/ai/services/RoutineGeneratorServiceTest.kt`
- verification evidence:
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --tests "com.trainiq.ai.services.RoutineGeneratorServiceTest" --console=plain`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --tests "com.trainiq.ai.services.RoutineGeneratorServiceTest" --console=plain`
  - RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --tests "com.trainiq.ai.services.RoutineGeneratorServiceTest" --console=plain` failed while the new test called `toAiUserMessage(...)` on a nullable `Throwable?` without a safe call.
  - PASS after fix: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --tests "com.trainiq.ai.services.RoutineGeneratorServiceTest" --console=plain`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain`
  - After-change PASS: `./gradlew.bat :app:test --console=plain`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain`
  - After-change PASS: `./gradlew.bat :app:test --console=plain`
  - Emulator check NOT RUN: first availability guard misread multi-line `adb devices` output and skipped despite `emulator-5554` being present; rerun targeted the emulator directly.
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 7888`; crash buffer was empty.
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain`; `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 9918`; crash buffer was empty.
- external sources used:
  - Kotlin null safety documentation: https://kotlinlang.org/docs/null-safety.html. Used to confirm the compiler failure should be fixed with a safe-call/null-safe expression in the test assertion.
- remaining risk: Throttles are process-local and reset after app process death. That is acceptable for an MVP client boundary, but production release still depends on QA-006's signed AI boundary decision.

### QA-2026-05-09-008

- finding_id: QA-2026-05-09-008
- priority: P2
- area: Android lifecycle, UX
- status: done
- owner suggestion: Android UI owner
- current evidence with file references:
  - Prior evidence found camera permission/error state stored in non-saveable composable state.
  - Current worktree uses `rememberSaveable`/`CameraScannerRestorableState.Saver` for permission denied and camera error state in `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/CameraScannerScreen.kt`.
- expected target-state behavior: Camera denied/error/capture states survive configuration changes where user context could otherwise become misleading.
- concrete recommended fix: Move camera permission and capture state into ViewModel or `rememberSaveable` where appropriate, and add a state restoration test for denied/error states.
- regression risk: Low to medium. Avoid persisting transient capture-in-progress state incorrectly after process death.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "*Camera*" --console=plain` plus rotate/recreate scanner smoke.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/CameraScannerScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/CameraScannerStateTest.kt`
- verification evidence:
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*Camera*" --console=plain`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*Camera*" --console=plain`
- remaining risk: `isCapturing` intentionally remains transient and resets after recreation to avoid resuming a stale photo capture. Emulator rotate/recreate scanner smoke was not run in this pass, so the release matrix item remains open.

### QA-2026-05-09-009

- finding_id: QA-2026-05-09-009
- priority: P2
- area: accessibility, UI
- status: done
- owner suggestion: Android UI/accessibility owner
- current evidence with file references:
  - Prior evidence found unlabeled Canvas chart surfaces in `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt` and `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`.
  - Current dirty worktree adds `lineChartContentDescription(...)` in `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt` and applies chart semantics in both shared and workout chart call sites.
- expected target-state behavior: Charts and custom visualizations expose meaningful semantic summaries for assistive technology.
- concrete recommended fix: Keep the new `Modifier.semantics { contentDescription = ... }` chart summaries, broaden the same pattern to remaining custom visualizations, and confirm TalkBack output manually before release.
- regression risk: Low. Risk is mainly inaccurate summaries if data labels do not match chart values.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.ui.LineChartSemanticsTest" --console=plain` and `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.ui.AppLineChartAccessibilityTest" --console=plain`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/core/ui/LineChartSemanticsTest.kt`
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/core/ui/AppLineChartAccessibilityTest.kt`
- verification evidence:
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.ui.LineChartSemanticsTest" :app:compileDebugAndroidTestKotlin --console=plain`
  - PASS: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.ui.AppLineChartAccessibilityTest" --console=plain` on `emulator-5554`
- remaining risk: Chart summaries are intentionally concise and derived from existing chart point labels/values; manual TalkBack review is still needed for release signoff, so the broader accessibility release item remains open.

### QA-2026-05-09-010

- finding_id: QA-2026-05-09-010
- priority: P2
- area: Android lifecycle, data
- status: done
- owner suggestion: Android platform owner
- current evidence with file references:
  - Prior evidence found `TrainIQ-Project/app/src/main/java/com/trainiq/core/health/HealthConnectBackgroundSyncWorker.kt` retrying all thrown failures.
  - Current worktree adds `shouldRetryHealthConnectBackgroundSyncFailure(...)` and avoids retrying `SecurityException`, `IllegalArgumentException`, and `UnsupportedOperationException`.
- expected target-state behavior: Background sync retries transient failures only and does not loop on permanent provider, permission, or configuration states.
- concrete recommended fix: Classify exceptions into transient/permanent categories, return `Result.failure()` or `Result.success()` for permanent states, and keep retry for network/transient Health Connect errors.
- regression risk: Medium. Misclassification could stop recovery from real transient failures.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnectBackgroundSync*" --console=plain`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/health/HealthConnectBackgroundSyncWorker.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/core/health/HealthConnectBackgroundSyncWorkerTest.kt`
- verification evidence:
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnectBackgroundSync*" --console=plain`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnectBackgroundSync*" --console=plain`
- remaining risk: Retry classification is intentionally conservative; unknown failures still retry, while permission/configuration/provider unsupported exceptions stop immediate retry loops. End-to-end WorkManager behavior with actual revoked permissions/provider states was not run.

### QA-2026-05-09-011

- finding_id: QA-2026-05-09-011
- priority: P2
- area: UI, UX, accessibility
- status: partially-done
- owner suggestion: Android UI owner
- current evidence with file references:
  - Prior runtime artifact `runtime-gemini-test/active-workout-start.xml` shows dense active-workout rows with clipped/tiny bounds for `Set 1`.
  - Prior runtime artifact `runtime-gemini-test/routine-ai-dialog.xml` includes `NAF="true"` nodes and English labels such as `Days per week`, `Available equipment`, and `Experience level`.
  - Current worktree uses a `ModalBottomSheet` for generated routine preview and replaces no-op metadata `AssistChip` controls with non-clickable `GeneratedRoutineInfoPill` labels in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/RoutineDialogs.kt`.
  - Current worktree changes active/routine set scan-row metric labels from `Reps` to compact Dutch `Herh.` through `RepetitionsMetricLabel` in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`.
  - Current worktree changes active-workout rest timer icon-only controls from terse labels like `30 seconden minder` to contextual Dutch labels such as `Rusttimer 30 seconden korter` in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`.
  - Current worktree changes the active-workout session status metric label from English `Rest` to Dutch `Rust` through `activeWorkoutRestStatusLabel()` in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`.
  - Current worktree gives active-workout session summary metrics equal weighted columns and merged `Label: value` accessibility summaries through `StatusMetric(...)` and `statusMetricContentDescription(...)` in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`.
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutDialogPresentationPolicyTest.kt` now guards that generated routine preview avoids dense `AlertDialog`, keeps scroll support, avoids stale English labels, and uses read-only metadata labels.
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt` now guards the active/routine set metric labels, active-workout rest status label, status metric accessibility summaries, and rest timer action descriptions.
- expected target-state behavior: Active workout and AI routine generation remain reachable, Dutch, labeled, and unclipped at 360px-class widths and font scale 1.3+.
- concrete recommended fix: Re-run current compact/font-scale QA, then replace dense alert-dialog/routine controls with adaptive full-screen or sticky-action sheet layouts and fix untranslated labels/semantics.
- regression risk: Medium. Layout changes can affect workout speed and routine generation conversion.
- minimal verification command/check: Emulator/device smoke at 360x640 and font scale 1.3+ with `uiautomator dump`; inspect for clipped bounds, `NAF="true"`, and English copy.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/RoutineDialogs.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutDialogPresentationPolicyTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- verification evidence:
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutDialogPresentationPolicyTest" --console=plain`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutDialogPresentationPolicyTest" --console=plain`
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain`
  - After-change PASS: `./gradlew.bat :app:test --console=plain`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain`
  - After-change PASS: `./gradlew.bat :app:test --console=plain`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 12422`; crash buffer was empty.
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8696`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain`
  - TIMED OUT: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain` exceeded the two-minute command timeout without returning output.
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 10652`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8548`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8563`; crash buffer was empty.
- external sources used: None. Local source and tests were sufficient; no Android, Material, accessibility, or Gradle ambiguity blocked this batch.
- remaining risk: AI routine preview metadata, active set metric copy, active-workout rest/status metrics, and rest timer icon-only actions are less likely to expose no-op controls, stale English labels, context-free labels, or uneven metric columns, but compact 360px/font-scale runtime QA and active-workout dense row clipping verification remain open.

### QA-2026-05-09-012

- finding_id: QA-2026-05-09-012
- priority: P2
- area: tests, accessibility
- status: partially-done
- owner suggestion: Android QA owner
- current evidence with file references:
  - Current dirty worktree adds shared line chart semantics tests in `TrainIQ-Project/app/src/test/java/com/trainiq/core/ui/LineChartSemanticsTest.kt` and `TrainIQ-Project/app/src/androidTest/java/com/trainiq/core/ui/AppLineChartAccessibilityTest.kt`.
  - Current worktree adds a generated routine preview source guard in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutDialogPresentationPolicyTest.kt`.
  - Current worktree adds active/routine set metric label guards in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds an active-workout rest status label guard in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds a merged active-workout sticky status semantics summary in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`, with a unit guard in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds a merged active-workout bottom bar semantics summary in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`, with a unit guard in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds merged status metric accessibility summaries in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`, with unit guards in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds a merged rest-timer card semantics summary in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`, with a unit guard in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds rest timer action-description guards, including a contextual skip action label, in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds Health Connect rationale reason guards in `TrainIQ-Project/app/src/test/java/com/trainiq/core/health/HealthConnectReadPermissionsTest.kt`.
  - Current worktree adds Settings destructive action confirmation copy guards in `TrainIQ-Project/app/src/test/java/com/trainiq/features/settings/SettingsUiStateTest.kt`.
  - Current worktree adds scanner permission-gate copy guards in `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/CameraScannerStateTest.kt`.
  - Current worktree adds scanner processing/completed/empty sheet state and action copy guards in `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/CameraScannerStateTest.kt`.
  - Signed manual accessibility coverage still lacks release evidence.
- expected target-state behavior: Dense custom surfaces have at least basic automated accessibility coverage in addition to manual TalkBack/Switch Access certification.
- concrete recommended fix: Keep the new chart semantics tests and add Compose UI or instrumentation assertions for active workout controls, AI routine generation, scanner states, Settings destructive actions, and Health Connect rationale. Enable broader Android accessibility checks where compatible with the stack.
- regression risk: Low. Some checks can be flaky if they depend on rendered text or device configuration.
- minimal verification command/check: `./gradlew.bat :app:connectedDebugAndroidTest --console=plain`.
- files changed:
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/core/ui/AppLineChartAccessibilityTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/core/ui/LineChartSemanticsTest.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutDialogPresentationPolicyTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- verification evidence:
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.ui.LineChartSemanticsTest" :app:compileDebugAndroidTestKotlin --console=plain`
  - PASS: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.ui.AppLineChartAccessibilityTest" --console=plain` on `emulator-5554`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutDialogPresentationPolicyTest" --console=plain`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain`
  - PASS: `./gradlew.bat :app:test --console=plain`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 12422`; crash buffer was empty.
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8696`; crash buffer was empty.
  - TIMED OUT: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain` exceeded the two-minute command timeout without returning output.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 10652`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.health.HealthConnectReadPermissionsTest" --tests "*Settings*" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.health.HealthConnectReadPermissionsTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 9589`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 9120`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8724`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8611`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 9828`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8744`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - FAIL then fixed: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache` failed at compile because the new test referenced missing `sampleWorkoutDay`; the test now constructs a minimal `WorkoutDay` inline.
  - PASS after fix: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 9717`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8563`; crash buffer was empty.
- remaining risk: Automated coverage now exists for shared line chart semantics, generated routine preview presentation/copy guards, active/routine set metric label guards, active-workout sticky status summary semantics, active-workout bottom bar summary semantics, status metric summary semantics, rest-timer card summary semantics, rest timer action descriptions including skip, Health Connect rationale reasons, Settings destructive confirmation copy, camera fallback policy/copy, scanner permission-gate copy, and scanner sheet state/action copy, but signed manual accessibility coverage remains open.

### QA-2026-05-09-013

- finding_id: QA-2026-05-09-013
- priority: P2
- area: Android lifecycle, UX
- status: done
- owner suggestion: Android camera/nutrition owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/AndroidManifest.xml:14` declares `android.hardware.camera` as `required="false"`.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/CameraScannerScreen.kt:352` creates a `LifecycleCameraController`.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/CameraScannerScreen.kt:364` calls `controller.bindToLifecycle(lifecycleOwner)` when permission is granted, without a visible `runCatching`/fallback path around bind failure.
- expected target-state behavior: Scanner entry handles no usable camera, CameraX bind failure, revoked camera, and device-specific camera errors with a clear user-facing fallback instead of crash or blank scanner.
- concrete recommended fix: Check camera feature/camera provider availability before showing scanner actions, wrap camera binding in a failure path that sets `cameraError`, and provide manual meal/barcode fallback actions. Add a fake/no-camera or bind-failure state test.
- regression risk: Low to medium. Scanner startup behavior can change on devices where CameraX initializes slowly; keep retry/manual fallback available.
- minimal verification command/check: Camera scanner emulator/device smoke with camera disabled or unavailable where possible, plus `./gradlew.bat :app:testDebugUnitTest --tests "*Camera*" --console=plain`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/CameraScannerScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/CameraScannerStateTest.kt`
- verification evidence:
  - RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --console=plain` failed while camera fallback helpers were absent.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --console=plain`
- remaining risk: Unit coverage verifies fallback policy and copy. Device smoke with a physically unavailable/disabled camera was not available in this pass.

## Refresh Audit - 2026-05-10

Audit scope: full target-state QA refresh against `TrainIQ_Target_State_Blueprint.md`, current Android source, build/test config, existing QA docs, release docs, emulator availability, and official Android/Gemini documentation.

### QA-2026-05-10-014

- finding_id: QA-2026-05-10-014
- priority: P0
- area: data, performance, Android lifecycle
- status: partially-done
- owner suggestion: Android data/platform owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt:138` still provides `update(transform)` that serializes the entire current app state and imports it through the Room mirror path.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt:239` starts/resumes active workout sessions through `runtimeStore.update`.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt:747` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt:825` show routine set/day mutations still using full-state update.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt:1051` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt:1152` show recipe, meal, and food mutations still using full-state update. Active set editing and body measurement add/delete now use targeted Room writes.
  - 2026-05-10 emulator smoke installed the app and reached Home, but `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: timeout`, `WaitTime: 20254`; crash buffer was empty.
- external sources used:
  - None for the local persistence finding. Local source and emulator evidence were sufficient.
- expected target-state behavior: Normal user mutations use bounded targeted DAO transactions. Startup and critical input paths avoid full-state JSON serialization, broad import planning, or broad Room mirror replacement.
- concrete recommended fix: Continue QA-2026-05-09-001 one flow at a time: active workout start/edit/finish/undo/collapse, routine edit/delete, meal save/delete, recipe save/delete, measurement add/delete, and profile writes should move to targeted DAO transactions with process-restart correctness tests.
- regression risk: High. Persistence changes can resurrect deleted rows or lose active workout data if transaction boundaries are wrong.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`, repository process-restart tests for each migrated path, `./gradlew.bat :app:connectedDebugAndroidTest --console=plain --no-configuration-cache`, and emulator launch/logcat smoke.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/TrainIqDao.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/architecture/RoomAuthorityArchitectureTest.kt`
- verification evidence:
  - 2026-05-10 baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
- remaining risk: Measurement writes, active set edits/type edits, and active collapse toggles are targeted, but process-restart instrumentation proof and larger hot paths remain open.

### QA-2026-05-10-015

- finding_id: QA-2026-05-10-015
- priority: P0
- area: release, accessibility
- status: blocked
- owner suggestion: accessibility/manual QA owner
- current evidence with file references:
  - `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md:24` through `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md:35` still list all critical TalkBack/Switch Access flows as `NOT_RUN`.
  - `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md:39` through `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md:44` show all release signoff checkboxes unchecked.
  - `TrainIQ-Project/docs/release/owner-action-tracker.md:13` keeps A11Y-001 open and release-blocking.
- external sources used:
  - Android Developers, Compose accessibility and scalable content docs, accessed 2026-05-10: https://developer.android.com/develop/ui/compose/accessibility and https://developer.android.com/develop/ui/compose/accessibility/scalable-content
- expected target-state behavior: Critical flows have signed TalkBack, Switch Access, large font, and dark-mode evidence before release.
- concrete recommended fix: Run the existing manual assistive-tech script on the current build, attach evidence paths, update the signoff file, and file code issues for failed flows.
- regression risk: Low for documentation; medium if UI fixes are required after manual QA.
- minimal verification command/check: Complete `TrainIQ-Project/docs/qa/talkback-switch-access-test-script.md` and update `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md` with tester/device/build/font/theme evidence.

### QA-2026-05-10-016

- finding_id: QA-2026-05-10-016
- priority: P1
- area: UI, UX, accessibility
- status: open
- owner suggestion: Android UI owner
- current evidence with file references:
  - 2026-05-10 polish: `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt` no longer forces shared `AppScreenHeader` title/subtitle text into ellipsized one-line/two-line clamps.
  - 2026-05-10 polish: `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt` no longer disables wrapping for the routine set index/type labels.
  - 2026-05-10 polish: `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt` now guards those critical shared header and routine set label wrapping constraints.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt:1720` through `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt:1774` still present AI routine generation in an `AlertDialog`, while the blueprint target prefers adaptive full-screen route or modal sheet with sticky actions unless compact evidence proves the dialog safe.
- external sources used:
  - Android Developers, Compose scalable content, accessed 2026-05-10: https://developer.android.com/develop/ui/compose/accessibility/scalable-content
- expected target-state behavior: Critical titles, workout labels, generated routine previews, and action areas wrap/reflow deliberately at 360x640, 360x800, font scale 1.3 and 1.5, without hiding essential context or actions.
- concrete recommended fix: Convert AI routine generation to an adaptive full-screen/sheet presentation or capture compact/large-font proof for the current dialog; keep the new shared header and routine set label wrapping guards in place.
- regression risk: Medium. Wrapping can increase vertical pressure on dense workout screens, so verify compact layouts after changes.
- verification evidence:
  - 2026-05-10 baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.critical headers and set labels allow wrapping at large font scale" --console=plain --no-configuration-cache` failed while the wrapping guard detected the old clamps.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.critical headers and set labels allow wrapping at large font scale" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:assembleDebug :app:test :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 11218`; Training tab UI dump rendered; crash buffer was empty.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- remaining risk: AI routine dialog compact behavior is still unproven at 360x640/360x800 with font scale 1.3/1.5; manual TalkBack/Switch Access evidence remains covered by QA-2026-05-10-015.
- minimal verification command/check: Compact emulator UI dump/screenshot pass for active workout and AI routine generation at 360x640/360x800 and font scale 1.3/1.5.

### QA-2026-05-10-017

- finding_id: QA-2026-05-10-017
- priority: P1
- area: performance, release
- status: blocked
- owner suggestion: product/Android performance owner
- current evidence with file references:
  - `TrainIQ-Project/docs/qa/performance-threshold-decision-record.md:15` through `TrainIQ-Project/docs/qa/performance-threshold-decision-record.md:19` still require product confirmation for startup and frame-jank thresholds.
  - `TrainIQ-Project/macrobenchmark/src/main/java/com/trainiq/macrobenchmark/TrainIqStartupBenchmark.java:31` through `TrainIQ-Project/macrobenchmark/src/main/java/com/trainiq/macrobenchmark/TrainIqStartupBenchmark.java:82` define baseline profile and macrobenchmark coverage, but current release docs still require physical-device evidence.
  - 2026-05-10 emulator launch reached Home with empty crash buffer but `am start -W` timed out at `WaitTime: 20254`; `dumpsys gfxinfo com.trainiq framestats` returned `Failure while dumping the app`.
- external sources used:
  - Android Developers, Baseline Profiles overview and Create Baseline Profiles, accessed 2026-05-10: https://developer.android.com/baseline-profiles and https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile
- expected target-state behavior: Release/profileable startup, top-level navigation, settings scroll, scanner launch, and active workout logging have approved thresholds and physical-device macrobenchmark evidence.
- concrete recommended fix: Approve numeric thresholds, run the device-lab performance plan on at least one lower-end and one representative modern physical device, and keep debug-emulator timeout as a signal until release/profileable evidence explains or eliminates it.
- regression risk: Medium. Performance fixes may touch startup data flow, baseline profile generation, or Compose initialization.
- minimal verification command/check: `./gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain --no-configuration-cache` on physical devices, plus `adb shell am start -W -n com.trainiq/.MainActivity` without timeout.

### QA-2026-05-10-018

- finding_id: QA-2026-05-10-018
- priority: P1
- area: privacy, security, release
- status: needs-decision
- owner suggestion: product/backend/security/legal owner
- current evidence with file references:
  - `TrainIQ-Project/docs/release/owner-action-tracker.md:11` through `TrainIQ-Project/docs/release/owner-action-tracker.md:14` keep Data Safety, performance, accessibility, and AI owner gates open.
  - `TrainIQ-Project/docs/release/owner-action-tracker.md:33` through `TrainIQ-Project/docs/release/owner-action-tracker.md:38` state release remains blocked until those gates are approved.
  - `TrainIQ-Project/app/src/main/AndroidManifest.xml:12` declares `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`, which needs release justification and owner confirmation.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/remote/GeminiApi.kt:10` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/remote/GeminiApi.kt:16` correctly keep Gemini key transport in the `x-goog-api-key` header, but production BYOK/gateway/OAuth mode remains an owner decision.
- external sources used:
  - Android Developers, Health Connect sync data, accessed 2026-05-10: https://developer.android.com/health-and-fitness/health-connect/sync-data
  - Google AI for Developers, Gemini thinking and structured output docs, accessed 2026-05-10: https://ai.google.dev/gemini-api/docs/thinking and https://ai.google.dev/gemini-api/docs/structured-output
- expected target-state behavior: Play/Data Safety, privacy policy, Health Connect background read, signing ownership, and production AI boundary are approved before release.
- concrete recommended fix: Close LEGAL-001, AI-001, and background Health Connect justification with owner evidence; update Data Safety/privacy docs to match the final AI and telemetry mode.
- regression risk: High for release/compliance accuracy if docs diverge from implementation.
- minimal verification command/check: Owner review of `TrainIQ-Project/docs/release/owner-action-tracker.md`, `TrainIQ-Project/docs/architecture/production-ai-boundary-decision-gate.md`, Data Safety worksheet, and privacy-policy draft.

### QA-2026-05-10-019

- finding_id: QA-2026-05-10-019
- priority: P2
- area: Android lifecycle, data
- status: open
- owner suggestion: Android Health Connect owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:90` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:99` gate background reads on SDK availability, feature availability, and granted background permission.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:193` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:221` support per-metric sync state.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:325` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:336` use per-record-type changes tokens.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:348` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:367` page `readRecords` calls.
  - End-to-end provider-missing, revoked permission, partial permission, and background-read flows were not executed in this audit.
- external sources used:
  - Android Developers, Health Connect sync data, accessed 2026-05-10: https://developer.android.com/health-and-fitness/health-connect/sync-data
- expected target-state behavior: Health Connect behaves correctly across provider missing/update required, no permission, partial permission, revoked permission while open, and background-read availability states.
- concrete recommended fix: Run device/emulator Health Connect smoke for provider missing/update, partial grants, revocation while app is open, and background-read unavailable/granted states; attach UI dumps/logcat evidence.
- regression risk: Medium. Permission flow fixes can alter consent clarity or accidentally block partial metrics.
- minimal verification command/check: Emulator/device manual script covering Health Connect provider and permission states, plus `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnect*" --console=plain --no-configuration-cache`.

### QA-2026-05-10-020

- finding_id: QA-2026-05-10-020
- priority: P1
- area: tests, release
- status: done
- owner suggestion: Android/release owner
- current evidence with file references:
  - `TrainIQ-Project/app/build.gradle.kts:162` through `TrainIQ-Project/app/build.gradle.kts:255` define Room migration-chain marker generation tasks.
  - `.github/workflows/android.yml:28` through `.github/workflows/android.yml:41` run unit tests, lint, Android test compilation, macrobenchmark compilation, and signing readiness, but do not run marker generation.
  - `.github/workflows/android.yml:78` builds signed release artifacts with `:app:checkReleaseSigningReadiness :app:assembleRelease :app:bundleRelease`, but does not require `generateReleaseRoomMigrationChainVerificationMarker`.
  - `TrainIQ_Target_State_Blueprint.md` requires release artifacts to be blocked without fresh migration-marker evidence or an owner-approved exception.
- external sources used:
  - None. Local Gradle and CI config were sufficient.
- expected target-state behavior: Release artifacts require fresh Room migration-chain runtime proof or an explicit owner-approved exception.
- concrete recommended fix: Wire `generateCiRoomMigrationChainVerificationMarkers` or release-specific marker generation into CI/release jobs, or document the marker as diagnostic-only and update the blueprint/release gates accordingly.
- regression risk: Medium. CI runtime can increase substantially because marker generation depends on connected tests.
- minimal verification command/check: CI job or local equivalent runs `./gradlew.bat :app:generateReleaseRoomMigrationChainVerificationMarker --console=plain --no-configuration-cache` before release artifact generation.
- files changed:
  - `.github/workflows/android.yml`
- verification evidence:
  - 2026-05-10 polish: `.github/workflows/android.yml` now adds a `room-migration-marker` job that runs `:app:generateCiRoomMigrationChainVerificationMarkers` inside an Android emulator runner.
  - 2026-05-10 polish: `signed-release` now depends on both `validate` and `room-migration-marker`, so signed release artifacts cannot be built by the workflow unless the Room migration marker gate passes.
  - 2026-05-10 PASS: `./gradlew.bat :app:generateCiRoomMigrationChainVerificationMarkers --dry-run --console=plain --no-configuration-cache`.
  - 2026-05-10 PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- external sources used: None. Local Gradle task wiring and workflow scope were sufficient.
- remaining risk: The first GitHub-hosted emulator run can still expose infrastructure issues, but the release workflow is now gated on marker generation instead of treating marker evidence as optional.

### QA-2026-05-10-021

- finding_id: QA-2026-05-10-021
- priority: P3
- area: release
- status: needs-decision
- owner suggestion: release owner
- current evidence with file references:
  - `TrainIQ-Project/app/build.gradle.kts:29` sets `versionCode = 1` and `versionName = "1.0"`.
  - Release owner gates remain open in `TrainIQ-Project/docs/release/owner-action-tracker.md:11` through `TrainIQ-Project/docs/release/owner-action-tracker.md:14`.
- external sources used:
  - None. Local release config was sufficient.
- expected target-state behavior: Play upload uses an owner-approved versioning strategy and release metadata.
- concrete recommended fix: Confirm whether first Play upload should remain `1.0`/`1`, or set a pre-release/internal-track version scheme before signed release generation.
- regression risk: Low for code; medium for release operations if a Play track already used versionCode 1.
- minimal verification command/check: Release owner records versioning decision before `:app:bundleRelease` artifacts are uploaded.

## 2026-05-10 Verification Summary

- `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin :app:checkReleaseSigningReadiness :macrobenchmark:compileProfileableJavaWithJavac --console=plain --no-configuration-cache`: PASS.
- Release-readiness worker also verified `:app:assembleRelease` and `:app:bundleRelease`: PASS; local signing was not configured, so unsigned local release artifacts are expected.
- `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`: PASS on `emulator-5554`.
- `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity`: FAIL/PERF RISK, returned `Status: timeout`, `WaitTime: 20254`; Home rendered in UI dump after launch.
- `adb -s emulator-5554 logcat -d -b crash`: PASS, empty crash buffer.
- `adb -s emulator-5554 shell dumpsys gfxinfo com.trainiq framestats`: FAIL/INCONCLUSIVE, returned `Failure while dumping the app`.
- `:app:lintDebug`: PASS with warnings reported by the release-readiness worker, including blocking `SharedPreferences.commit()` in `AndroidKeystoreGeminiKeyStore.kt`, unused legacy color resources, and dependency update warnings.
