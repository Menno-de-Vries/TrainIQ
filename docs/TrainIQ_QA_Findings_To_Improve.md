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
  - Current worktree moves active-workout start/resume from full JSON mirror import to targeted active-session, draft-session, active-draft, and performed-exercise writes through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout draft updates from full JSON mirror import to a targeted `active_workout_drafts` upsert plus `active_workout_sessions.updatedAt` update through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout discard from full JSON mirror import to targeted deletes for workout log-event snapshots, workout log events, active workout session children, performed exercises, and the draft workout session through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout set logging from full JSON mirror import to targeted upserts for the active session, draft, active set, undo log event, and undo snapshot rows through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout set editing from full JSON mirror import to targeted active set, draft, rest-timer, and current undo snapshot updates through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout set type editing from full JSON mirror import to targeted active set type, current undo snapshot type, and session timestamp updates.
  - Current worktree moves active-workout set deletion from full JSON mirror import to targeted active set deletion, pending add-event cleanup, and session timestamp updates.
  - Current worktree moves active-workout collapse/expand toggles from full JSON mirror import to targeted `active_workout_collapsed_exercises` insert/delete plus session timestamp update.
  - Current worktree moves active-workout finish from full JSON mirror import to a targeted Room transaction for completed `workout_sessions`, `performed_exercises`, `workout_sets`, debrief fields, and active-workout runtime cleanup.
  - Current worktree moves active-workout undo from full JSON mirror import to a targeted Room transaction for restored `active_workout_sets`, pending undo event snapshots, and active session timestamp updates.
  - Current worktree moves body measurement add/delete from full JSON mirror import to targeted `body_measurements` insert/delete paths through the same DAO/runtime-store/repository path.
  - Current worktree moves meal save/delete from full JSON mirror import to targeted `meals` and `meal_items` upsert/delete transactions.
  - Current worktree moves profile save/reset from full JSON mirror import to targeted `user_profile` upsert/delete calls.
  - Current worktree moves active routine selection from full JSON mirror import to targeted `workout_routines` active-flag update.
  - Current worktree moves superset grouping from full JSON mirror import to targeted `workout_exercises.superset_group_id` updates.
  - Current worktree moves workout exercise plan updates from full JSON mirror import to targeted workout-exercise upsert plus per-exercise routine-set replacement.
  - Current worktree moves planned and active-workout exercise replacement from full JSON mirror import to targeted `workout_exercises` upsert for the affected row, with active-session timestamp update when applicable.
  - Current worktree moves routine set edits from full JSON mirror import to targeted `routine_sets` upserts plus synchronized `workout_exercises` target updates.
  - Current worktree moves delayed startup exercise-library seeding from full JSON mirror import to a targeted `exercises` upsert path through `ExerciseLibrarySeeder.missingCanonicalExercises(...)`, `RoomTrainIqRuntimeStore.seedExerciseLibrary(...)`, and `TrainIqDao.insertExercises(...)`.
  - 2026-05-11 source scan `rg "runtimeStore\.update\(" TrainIQ-Project/app/src/main/java/com/trainiq -n` returned no app-source callers; `RoomTrainIqRuntimeStore.update(transform)` remains only as legacy/import infrastructure.
  - Current worktree removes the public `RoomTrainIqRuntimeStore.update(transform)` API entirely; legacy JSON seeding remains private in `seedRoomFromLegacyJsonIfNeeded()`, while mirror-run/dry-run infrastructure remains in `data/migration`.
  - `TrainIQ-Project/app/src/test/java/com/trainiq/architecture/RoomAuthorityArchitectureTest.kt` now guards that `updateActiveWorkoutRestTimer(...)`, `updateActiveWorkoutDraft(...)`, `logActiveWorkoutSet(...)`, and `discardActiveWorkout(...)` use targeted Room updates/deletes instead of `runtimeStore.update { ... }`.
- expected target-state behavior: Normal user mutations use bounded targeted DAO transactions. Startup and critical input paths do not perform full-state JSON serialization, broad import planning, or broad mirror table replacement.
- concrete recommended fix: Keep JSON import for legacy/import tooling only. Add targeted DAO-backed repository mutations for active workout logging, meal save/delete, routine edit/delete, measurement edit/delete, finish/discard, and profile writes. Add a regression guard that these hot paths do not call `RoomTrainIqRuntimeStore.update()`.
- regression risk: High. This touches persistence and process-restart correctness; migrate flow by flow behind tests instead of replacing all mutations at once.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest --console=plain`, plus an active-workout logging smoke with `adb shell dumpsys gfxinfo com.trainiq framestats`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/TrainIqDao.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/ExerciseLibrarySeeder.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/architecture/RoomAuthorityArchitectureTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/data/repository/ExerciseLibrarySeederTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/data/repository/WorkoutSessionTransactionTest.kt`
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
  - 2026-05-11 exercise-library seeding after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.repository.ExerciseLibrarySeederTest" --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise-library seeding source scan PASS: `rg "runtimeStore\.update\(" TrainIQ-Project/app/src/main/java/com/trainiq -n` returned no matches.
  - 2026-05-11 exercise-library seeding broad gate PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise-library seeding device smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 710`; after waiting for the delayed seed job, `logcat-crash-slice.txt` was empty. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-11-post-exercise-seed-launch/`.
  - 2026-05-11 runtime update API removal PASS: focused `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.repository.WorkoutSessionTransactionTest" --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-11 runtime update API removal PASS: broad `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 runtime update API removal PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 698`; after startup delay, `logcat-crash-slice.txt` was empty. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-11-post-runtime-update-removal-launch/`.
  - 2026-05-11 post-removal connected persistence PASS: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache`; report `TEST-SM-S931B - 16-_app-.xml` recorded `tests="26" failures="0" errors="0" skipped="0"`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-workout finish persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.architecture.RoomAuthorityArchitectureTest`.
  - 2026-05-10 active-workout finish persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.data.repository.TrainIqRepositoryTest --tests com.trainiq.data.repository.WorkoutSessionTransactionTest --tests com.trainiq.data.repository.ActiveWorkoutSessionMutationsTest --tests com.trainiq.data.repository.WorkoutLogEventTest --tests com.trainiq.data.repository.WorkoutCompletionSummaryTest`.
  - 2026-05-10 active-workout finish persistence after-change PASS: `./gradlew :app:assembleDebug`.
  - 2026-05-10 active-workout finish persistence after-change PASS: `./gradlew :app:lintDebug`.
  - 2026-05-10 active-workout undo persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.architecture.RoomAuthorityArchitectureTest --tests com.trainiq.data.repository.WorkoutLogEventTest`.
  - 2026-05-10 active-workout undo persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.data.repository.TrainIqRepositoryTest --tests com.trainiq.data.repository.ActiveWorkoutSessionMutationsTest --tests com.trainiq.data.repository.WorkoutLogEventTest --tests com.trainiq.domain.usecase.StartWorkoutSessionUseCaseTest --tests com.trainiq.features.workout.WorkoutInputValidationTest`.
  - 2026-05-10 active-workout undo persistence after-change PASS: `./gradlew :app:assembleDebug`.
  - 2026-05-10 active-workout undo persistence after-change PASS: `./gradlew :app:lintDebug`.
  - 2026-05-10 routine core persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.architecture.RoomAuthorityArchitectureTest`.
  - 2026-05-10 routine core persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.data.repository.TrainIqRepositoryTest --tests com.trainiq.features.workout.WorkoutInputValidationTest --tests com.trainiq.domain.usecase.StartWorkoutSessionUseCaseTest`.
  - 2026-05-10 routine core persistence after-change PASS: `./gradlew :app:assembleDebug`.
  - 2026-05-10 routine core persistence after-change PASS: `./gradlew :app:lintDebug`.
  - 2026-05-10 routine set add/delete/move persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.architecture.RoomAuthorityArchitectureTest --tests com.trainiq.data.repository.TrainIqRepositoryTest`.
  - 2026-05-10 routine set add/delete/move persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.features.workout.WorkoutInputValidationTest --tests com.trainiq.domain.usecase.StartWorkoutSessionUseCaseTest --tests com.trainiq.data.repository.TrainIqRepositoryTest`.
  - 2026-05-10 routine set add/delete/move persistence after-change PASS: `./gradlew :app:assembleDebug`.
  - 2026-05-10 routine set add/delete/move persistence after-change PASS: `./gradlew :app:lintDebug`.
  - 2026-05-10 workout day add/remove persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.architecture.RoomAuthorityArchitectureTest --tests com.trainiq.data.repository.TrainIqRepositoryTest`.
  - 2026-05-10 workout day add/remove persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.features.workout.WorkoutInputValidationTest --tests com.trainiq.domain.usecase.StartWorkoutSessionUseCaseTest --tests com.trainiq.data.repository.TrainIqRepositoryTest`.
  - 2026-05-10 workout day add/remove persistence after-change PASS: `./gradlew :app:assembleDebug`.
  - 2026-05-10 workout day add/remove persistence after-change PASS: `./gradlew :app:lintDebug`.
  - 2026-05-10 workout exercise add/remove persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --tests "com.trainiq.data.repository.TrainIqRepositoryTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 workout exercise add/remove persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 workout exercise add/remove physical-device smoke PASS on SM-S931B: `:app:installDebug`, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 794`, Home rendered in `TrainIQ-Project/.codex/trainiq-app-ready-smoke.xml`, and crash buffer was empty.
  - 2026-05-10 add-exercise-to-routine persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --tests "com.trainiq.data.repository.TrainIqRepositoryTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 add-exercise-to-routine persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 add-exercise-to-routine physical-device smoke PASS on SM-S931B: `:app:installDebug`, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 692`, Home rendered in `TrainIQ-Project/.codex/trainiq-app-ready-smoke-latest.xml`, and crash buffer was empty.
  - 2026-05-10 session delete persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --tests "com.trainiq.data.repository.TrainIqRepositoryTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 session delete persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 session delete physical-device smoke PASS on SM-S931B: `:app:installDebug`, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 701`, Home rendered in `TrainIQ-Project/.codex/trainiq-app-ready-smoke-final.xml`, and crash buffer was empty.
  - 2026-05-10 generated-routine save persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --tests "com.trainiq.data.repository.TrainIqRepositoryTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 generated-routine save persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 generated-routine save physical-device smoke PASS on SM-S931B: `:app:installDebug`, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 699`, Home rendered in `TrainIQ-Project/.codex/trainiq-app-ready-smoke-generated-routine.xml`, and crash buffer was empty.
  - 2026-05-10 recipe/food persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --tests "com.trainiq.data.repository.TrainIqRepositoryTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 recipe/food persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 recipe/food physical-device smoke PASS on SM-S931B: `:app:installDebug`, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 714`, Home rendered in `TrainIQ-Project/.codex/trainiq-app-ready-smoke-food-recipe.xml`, and crash buffer was empty.
  - 2026-05-10 process-restart correctness instrumentation PASS: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B. The test writes targeted routine, nutrition, meal, profile, measurement, workout day/exercise, and session rows, closes/reopens the database, and verifies inserted rows persist while deleted session/nutrition/meal/measurement/workout rows do not resurrect.
  - 2026-05-11 meal restart coverage follow-up PASS: same targeted connected class on SM-S931B recorded `tests="5" failures="0" errors="0"` including `targetedMealMutationsSurviveDatabaseReopen`.
  - 2026-05-11 active-workout start persistence PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`; `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with `tests="6" failures="0" errors="0"`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 tooling note: an initial parallel Gradle verification attempt failed with Kotlin/Hilt incremental cache file registration/exists errors; after `./gradlew.bat --stop`, the same checks passed serially.
  - 2026-05-11 active routine selection persistence PASS: `./gradlew.bat clean :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`; `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 7 tests, 0 failures, 0 errors; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 tooling note: the first post-change compile showed stale unresolved-reference errors across existing helper imports; `clean` rebuilt Kotlin/KSP state and verification then passed.
  - 2026-05-11 superset persistence PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`; `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 8 tests, 0 failures, 0 errors; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 workout exercise plan persistence PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`; `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 9 tests, 0 failures, 0 errors; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 replace-exercise-in-plan persistence PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`; `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 10 tests, 0 failures, 0 errors; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 replace-exercise-in-active-workout persistence PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`; `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 11 tests, 0 failures, 0 errors; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-workout runtime mutation restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 12 tests, 0 failures, 0 errors including `targetedActiveWorkoutRuntimeMutationsSurviveDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-workout finish/undo restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 14 tests, 0 failures, 0 errors including `targetedActiveWorkoutFinishSurvivesDatabaseReopenAndClearsRuntimeRows` and `targetedActiveWorkoutUndoSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 routine-set edit/replace restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 15 tests, 0 failures, 0 errors including `targetedRoutineSetEditAndReplaceSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 routine lifecycle restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 16 tests, 0 failures, 0 errors including `targetedRoutineCreateUpdateDeleteSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise reorder restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 17 tests, 0 failures, 0 errors including `targetedExerciseReorderSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 generated-routine graph restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 18 tests, 0 failures, 0 errors including `targetedGeneratedRoutineGraphSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 profile reset restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 19 tests, 0 failures, 0 errors including `targetedProfileResetSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-set delete restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 20 tests, 0 failures, 0 errors including `targetedActiveWorkoutSetDeleteSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-set type edit restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 21 tests, 0 failures, 0 errors including `targetedActiveWorkoutSetTypeEditSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-set value edit restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 22 tests, 0 failures, 0 errors including `targetedActiveWorkoutSetValueEditSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-collapse expand restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 23 tests, 0 failures, 0 errors including `targetedActiveWorkoutCollapseExpandSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-workout discard restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 24 tests, 0 failures, 0 errors including `targetedActiveWorkoutDiscardSurvivesDatabaseReopenAndClearsRuntimeRows`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 workout debrief refresh restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 25 tests, 0 failures, 0 errors including `targetedWorkoutDebriefRefreshSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 routine cascade delete restart coverage PASS: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 26 tests, 0 failures, 0 errors including `targetedRoutineCascadeDeleteSurvivesDatabaseReopen`.
  - 2026-05-11 post-routine-cascade broad/device gate PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B cold launch returned `Status: ok`, `WaitTime: 892`, and the crash buffer was empty. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-11-post-cascade-launch/`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
- external sources used: None. Local Room DAO patterns and existing architecture tests were sufficient; no Android/Room API ambiguity blocked this batch.
- remaining risk: This moves active workout start/resume, rest timer, active draft, active set logging/editing/type editing/deletion/undo, active collapse/expand, active discard/finish, workout debrief refresh, active-workout exercise replacement, session delete, routine create/update/delete, active routine selection, generated-routine save, exercise reorder, superset grouping, workout exercise replacement in plans, workout exercise plan updates, workout day add/remove, workout exercise add/remove, add-exercise-to-routine, routine set add/edit/delete/move, meal save/delete, recipe/food save/delete, profile save/reset, and body measurement add/delete paths to targeted Room persistence. Instrumentation process-restart tests now cover representative targeted generated-routine graph save, routine lifecycle, standalone routine cascade delete, exercise reorder, routine-set edit/replace, active routine selection, superset grouping, active-workout runtime mutations, active-workout collapse/expand deletion, active-workout set delete/type-edit/value-edit cleanup, active-workout discard/finish/undo cleanup, workout debrief refresh, workout exercise replacement in plans and active workouts, workout exercise plan update, nutrition save/delete, meal save/delete, active-workout start, profile save/reset, profile/measurement add-delete, session delete, workout day/exercise add, workout exercise delete, and workout day cascade delete persistence after database reopen; broader process-restart coverage for every individual mutation can still be expanded before fully closing QA-001.

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
  - 2026-05-12 PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --console=plain --no-configuration-cache`.
  - 2026-05-12 tooling note: `./gradlew.bat :app:testDebugUnitTest --tests "*Home*" --console=plain --no-configuration-cache` failed because the broad wildcard filter resolved to a non-test include (`ui-home.xml`); use the exact `HomeDashboardRefreshTest` class filter for repeatable verification.
- remaining risk: Periodic refresh now runs under `HomeRoute` `repeatOnLifecycle(Lifecycle.State.STARTED)` and the exact source/unit guard passes, but manual top-level navigation log evidence that refresh pauses off-screen was not captured because adding runtime diagnostic logging solely for this evidence would be more invasive than the current risk.

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
  - 2026-05-12 PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*Camera*" --console=plain --no-configuration-cache`.
  - 2026-05-12 PASS: disposable emulator smoke in `TrainIQ-Project/.codex/device-qa/2026-05-12-scanner-rotate-recreate-qa/` launched the AI scanner permission gate from the Ochtend meal sheet at 360x640/mdpi/font scale 1.5 without granting camera permission, rotated portrait to landscape and back to portrait, and captured `110-ai-scanner-before-rotate.xml`, `111-ai-scanner-landscape.xml`, and `112-ai-scanner-portrait-restored.xml` with `Cameratoegang nodig`, `Toegang geven`, `Terug`, `NAF=0`, and empty crash buffer.
- remaining risk: `isCapturing` intentionally remains transient and resets after recreation to avoid resuming a stale photo capture. The permission-gate rotate/recreate smoke now passes; preview/capture rotation still needs safe camera-use signoff before release.

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
  - 2026-05-11 compact/font-scale QA on SM-S931B captured Training and AI routine dialog dumps at font scale 1.3 and 1.5 under `TrainIQ-Project/.codex/device-qa/2026-05-11-compact-font-workout-qa/`; pre-fix font scale 1.5 AI dialog exposed one `NAF="true"` equipment field when its visual label was partially clipped.
  - 2026-05-11 polish added an explicit reusable `accessibilityLabel` path to `TapOnlyOutlinedTextField` and applied it to the AI routine `Beschikbaar materiaal` field, preserving the visual label while keeping the accessibility name available at large font scale.
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
  - 2026-05-11 compact/font-scale baseline PASS/PARTIAL: SM-S931B font scale 1.3 and 1.5 Training dumps had `NAF=0`, no stale English copy, and empty crash slices; the AI routine dialog had `NAF=0` at 1.3 but `NAF=1` at 1.5 for the equipment text field.
  - 2026-05-11 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
  - 2026-05-11 after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 after-change PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B font scale 1.5 AI routine dialog dump `font-scale-1.5-after-fix-v2-ai-dialog.xml` recorded `NAF=0`, no stale English copy, `AI-routine genereren`, `Genereren`, and `content-desc="Beschikbaar materiaal"`; crash slice was empty and font scale was restored to `1.0`.
  - 2026-05-11 active-workout compact/font-scale PASS: SM-S931B setup created `QAFontRoutine`, added `Ab Wheel Rollout`, launched active workout, and captured `45-after-copy-active-workout-font-1.5-v2.xml` with `Actieve training`, `Training afronden`, `Ab Wheel Rollout`, `0/3 sets - 8-12 herh.`, `NAF=0`, no stale `Reps`/`Rest`/`8-12 reps` copy, empty crash slice, and restored font scale `1.0`.
- external sources used: None. Local source and tests were sufficient; no Android, Material, accessibility, or Gradle ambiguity blocked this batch.
- remaining risk: AI routine preview metadata, active set metric copy, active-workout rest/status metrics, rest timer icon-only actions, and first active-workout row semantics are less likely to expose no-op controls, stale English labels, context-free labels, or uneven metric columns. AI routine dialog and representative active-workout font-scale 1.5 semantics are verified on one 360dp-class physical device; deeper active-workout rows after logged sets and manual TalkBack/Switch Access verification remain open.

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
- remaining risk: Automated coverage now exists for shared line chart semantics, generated routine preview presentation/copy guards, active/routine set metric label guards, active-workout sticky status summary semantics, active-workout bottom bar summary semantics, status metric summary semantics, rest-timer card summary semantics, rest timer action descriptions including skip, Health Connect rationale reasons, Settings destructive confirmation copy, camera fallback policy/copy, scanner permission-gate copy, scanner sheet state/action copy, and shared nutrition field label semantics, but signed manual accessibility coverage remains open.
  - 2026-05-11 scanner/nutrition compact-font PASS/PARTIAL: SM-S931B font scale 1.5 evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-scanner-fontscale-qa/` verified the Nutrition first viewport and meal source sheet with `NAF=0`; `Foto / AI-inschatting` was visible but disabled because AI was not configured/enabled on this device. The manual product form first reproduced `NAF=2` on the clipped numeric field row, then `NutritionTextField` and `NutritionNumberField` gained merged label semantics. Focused `CameraScannerStateTest`, broad Gradle gate, debug reinstall, `50-after-number-field-product-form-top.xml`, and `51-after-number-field-product-form-lower.xml` passed with `NAF=0`, product/barcode/numeric labels visible, `content-desc` on `kcal / 100g` and `Eiwit / 100g`, `Barcode scannen`/`Product opslaan` visible in the lower dump, empty crash slice, and restored font scale `1.0`. Follow-up barcode launch evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-barcode-scanner-fontscale-qa/04-after-barcode-tap.xml` reached the camera permission gate with `Cameratoegang nodig`, `Geef cameratoegang om de scanner te gebruiken.`, `Toegang geven`, `Terug`, `NAF=0`, empty crash slice, and restored font scale `1.0`. Post-permission preview evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-barcode-scanner-preview-fontscale-qa/05-barcode-preview.xml` temporarily granted camera permission, reached `Barcodescanner`, `Richt de camera op de barcode van het product.`, `Annuleren`, `NAF=0`, empty crash slice, restored font scale `1.0`, and restored camera permission to its original denied state.
  - 2026-05-11 Health Connect rationale compact-font PASS: non-mutating SM-S931B font scale 1.5 evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-health-connect-rationale-fontscale-qa/` launched `HealthConnectPermissionsRationaleActivity` directly without tapping the permission button. `02-top-font-1.5.xml`, `03-scrolled-font-1.5.xml`, and `06-bottom-font-1.5.xml` passed with `NAF=0`, visible rationale text and permission reasons, visible `TrainIQ verbinden`, `Health Connect-toegang geven`, and `Doorgaan naar TrainIQ`; `09-logcat-trainiq-crash.txt` was empty and font scale was restored to `1.0`.
  - 2026-05-11 Settings destructive-actions compact-font PASS: SM-S931B font scale 1.5 evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-settings-destructive-fontscale-qa/` safely opened and canceled all destructive confirmation dialogs. Pre-fix evidence found a clipped Health Connect action button with `NAF=1`; `SettingsSection.kt` now gives Health Connect Settings actions explicit semantics labels and `SettingsUiStateTest` guards those labels. Focused `SettingsUiStateTest`, debug reinstall, broad Gradle gate, `71-after-fix-health-clipped.xml`, `72-after-fix-profile-actions.xml`, and `80` through `82` dialog dumps passed with `NAF=0`, visible `Annuleren`/`Bevestigen`, empty TrainIQ crash slice, and restored font scale `1.0`.
  - 2026-05-11 360x640 emulator compact-font PARTIAL/PASS: `Medium_Phone` was launched headless with `-skin 360x640`, installed the current debug build, and captured font scale 1.5 evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-360x640-emulator-fontscale-qa/`. Initial evidence showed the fixed-height labeled bottom nav and shared header consumed nearly the whole first viewport. `TrainIqNav.kt` now condenses compact short-height bottom navigation, `AppDesign.kt` uses a denser shared header on screens `<= 640dp` high, and tests guard the short-screen policy. Focused navigation/workout/settings tests and the broad Gradle gate passed. Rerun evidence `201-after-header-fix-home-font-1.5.xml`, `202-after-header-fix-settings-font-1.5.xml`, and `203-after-header-fix-home-multiscroll-font-1.5.xml` passed with `NAF=0`, successful cold launch, empty TrainIQ crash slice, restored font scale, and scroll-reachable Home body copy. Remaining risk: the first 360x640 font scale 1.5 viewport still prioritizes header/nav over rich content, so broader 360x640 screen-by-screen design signoff remains open.
  - 2026-05-11 360x640 Training compact-font PASS/PARTIAL: debug and profileable emulator evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-360x640-training-fontscale-qa/`, `TrainIQ-Project/.codex/device-qa/2026-05-11-360x640-active-workout-fontscale-qa/`, and `TrainIQ-Project/.codex/device-qa/2026-05-11-360x640-active-workout-route-after-compact-set-type/` verified Training top/scrolled/routine-detail plus seeded active-workout top/scrolled states at font scale 1.5 with `NAF=0`, empty TrainIQ crash slices, and restored font scale. Profileable benchmark seed made `Benchmark routine` and `Training starten` reachable on the disposable emulator. Pre-fix deeper Training scroll evidence exposed clipped routine-creation buttons with `NAF=1`; `RoutineCreationCard` now adds explicit `Lege routine maken` and `Met AI genereren` semantics labels, guarded by `WorkoutInputValidationTest`. Pre-fix active-workout route evidence exposed a clipped scrolled set-type chip with `NAF=1`; active-workout set-type selection now uses the existing compact dropdown mode on `<= 640dp` high screens, guarded by `WorkoutInputValidationTest`. Focused workout tests and the profileable build passed. Remaining risk: broader 360x640 screen-by-screen design signoff remains partial.
  - 2026-05-12 360x640 top-level compact-font PASS/PARTIAL: debug emulator evidence in `TrainIQ-Project/.codex/device-qa/2026-05-12-360x640-top-level-fontscale-qa/` captured Start, Training, Voeding, Coach, Instellingen, and Settings-to-Voortgang top/scrolled states at font scale 1.5 with `NAF=0` and empty crash buffers. Remaining risk: this is UIAutomator/font-scale smoke evidence, not manual TalkBack/Switch Access signoff or full interaction coverage for every nested flow.

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
- remaining risk: Unit coverage verifies fallback policy and copy. Device smoke with a physically unavailable/disabled camera was not available in this pass. At font scale 1.5, the source sheet, AI-disabled photo action, manual product form, barcode scanner permission gate, AI scanner permission-gate rotate/recreate path, and post-permission barcode preview rendered without NAF/crash. Real barcode recognition and AI photo capture still need manual/device signoff with safe camera usage.

## Refresh Audit - 2026-05-10

Audit scope: full target-state QA refresh against `TrainIQ_Target_State_Blueprint.md`, current Android source, build/test config, existing QA docs, release docs, emulator availability, and official Android/Gemini documentation.

### QA-2026-05-10-014

- finding_id: QA-2026-05-10-014
- priority: P0
- area: data, performance, Android lifecycle
- status: partially-done
- owner suggestion: Android data/platform owner
- current evidence with file references:
  - `RoomTrainIqRuntimeStore.update(transform)` has been removed. The only remaining full-state JSON import path inside `RoomTrainIqRuntimeStore` is the private one-time legacy seed from `TrainIqLocalStore.exportLegacyState()`, and explicit mirror-run/dry-run infrastructure remains under `TrainIQ-Project/app/src/main/java/com/trainiq/data/migration`.
  - Known repository mutation hot paths and delayed exercise-library seeding now use targeted Room writes; remaining work is to expand runtime QA and decide whether the private one-time legacy seed can be deleted after migration support is no longer needed.
  - Routine create/update/delete, exercise reorder, workout day add/remove, workout exercise add/remove, and routine set add/edit/delete/move now use targeted Room writes.
  - 2026-05-11 source scan confirms no `runtimeStore.update(...)` callers remain under `TrainIQ-Project/app/src/main/java/com/trainiq`; recipe/food mutations, active set editing/deletion, meal save/delete, profile save/reset, body measurement add/delete, and exercise-library seeding now use targeted Room writes.
  - 2026-05-10 emulator smoke installed the app and reached Home, but `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: timeout`, `WaitTime: 20254`; crash buffer was empty.
- external sources used:
  - None for the local persistence finding. Local source and emulator evidence were sufficient.
- expected target-state behavior: Normal user mutations use bounded targeted DAO transactions. Startup and critical input paths avoid full-state JSON serialization, broad import planning, or broad Room mirror replacement.
- concrete recommended fix: Continue QA-2026-05-09-001 one flow at a time: remaining evidence gaps should receive targeted process-restart correctness tests and runtime QA.
- regression risk: High. Persistence changes can resurrect deleted rows or lose active workout data if transaction boundaries are wrong.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`, repository process-restart tests for each migrated path, `./gradlew.bat :app:connectedDebugAndroidTest --console=plain --no-configuration-cache`, and emulator launch/logcat smoke.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/TrainIqDao.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/ExerciseLibrarySeeder.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/architecture/RoomAuthorityArchitectureTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/data/repository/ExerciseLibrarySeederTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/data/repository/WorkoutSessionTransactionTest.kt`
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
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise-library seeding after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.repository.ExerciseLibrarySeederTest" --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise-library seeding source scan PASS: `rg "runtimeStore\.update\(" TrainIQ-Project/app/src/main/java/com/trainiq -n` returned no matches.
  - 2026-05-11 exercise-library seeding broad gate PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise-library seeding device smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 710`; after waiting for the delayed seed job, `logcat-crash-slice.txt` was empty. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-11-post-exercise-seed-launch/`.
  - 2026-05-11 runtime update API removal PASS: focused `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.repository.WorkoutSessionTransactionTest" --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-11 runtime update API removal PASS: broad `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 runtime update API removal PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 698`; after startup delay, `logcat-crash-slice.txt` was empty. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-11-post-runtime-update-removal-launch/`.
  - 2026-05-11 post-removal connected persistence PASS: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache`; report `TEST-SM-S931B - 16-_app-.xml` recorded `tests="26" failures="0" errors="0" skipped="0"`.
- remaining risk: Normal app-source callers cannot use `RoomTrainIqRuntimeStore.update(transform)` because the API has been removed. Full closure still needs broader runtime QA, and a later engineering decision on when the private one-time legacy seed can be deleted after migration support is no longer needed.

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
- status: partially-done
- owner suggestion: Android UI owner
- current evidence with file references:
  - 2026-05-10 polish: `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt` no longer forces shared `AppScreenHeader` title/subtitle text into ellipsized one-line/two-line clamps.
  - 2026-05-10 polish: `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt` no longer disables wrapping for the routine set index/type labels.
  - 2026-05-10 polish: `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt` now guards those critical shared header and routine set label wrapping constraints.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt` still presents AI routine generation in an `AlertDialog`, but 2026-05-11 SM-S931B compact/font-scale evidence now proves the current first-viewport dialog state has labeled controls at font scale 1.5 after adding the equipment field accessibility label.
- external sources used:
  - Android Developers, Compose scalable content, accessed 2026-05-10: https://developer.android.com/develop/ui/compose/accessibility/scalable-content
- expected target-state behavior: Critical titles, workout labels, generated routine previews, and action areas wrap/reflow deliberately at 360x640, 360x800, font scale 1.3 and 1.5, without hiding essential context or actions.
- concrete recommended fix: Keep the shared header/routine-set wrapping guards and AI equipment accessibility label in place; next, verify deeper scrolled AI routine controls and active-workout dense rows at compact width/font scale before deciding whether a full-screen/sheet conversion is still needed.
- regression risk: Medium. Wrapping can increase vertical pressure on dense workout screens, so verify compact layouts after changes.
- verification evidence:
  - 2026-05-10 baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.critical headers and set labels allow wrapping at large font scale" --console=plain --no-configuration-cache` failed while the wrapping guard detected the old clamps.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.critical headers and set labels allow wrapping at large font scale" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:assembleDebug :app:test :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 11218`; Training tab UI dump rendered; crash buffer was empty.
  - 2026-05-11 baseline PASS/PARTIAL: SM-S931B font scale 1.3/1.5 Training dumps had `NAF=0`, no stale English copy, and empty crash slices; AI routine dialog had `NAF=0` at 1.3 and `NAF=1` at 1.5 before the equipment-label fix.
  - 2026-05-11 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
  - 2026-05-11 after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 after-change PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B font scale 1.5 AI routine dialog dump recorded `NAF=0`, no stale English copy, `content-desc="Beschikbaar materiaal"`, visible `AI-routine genereren` and `Genereren`, empty crash slice, and font scale restored to `1.0`.
  - 2026-05-11 active-workout compact/font-scale PASS: routine-detail setup first exposed a partially visible `Sessie toevoegen` field with `NAF=1`; after adding `accessibilityLabel = "Sessienaam optioneel"`, the same routine-detail state recorded `NAF=0`, `content-desc="Sessienaam optioneel"`, no stale English copy, and empty crash slice.
  - 2026-05-11 active-workout compact/font-scale PASS: after replacing the active exercise summary copy from `8-12 reps` to `8-12 herh.`, focused `WorkoutInputValidationTest`, broad `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin`, reinstall, and SM-S931B active-workout font scale 1.5 UI dump all passed; the final dump recorded `Actieve training`, `Training afronden`, `Ab Wheel Rollout`, `NAF=0`, no stale English copy, and an empty crash slice.
  - 2026-05-11 logged-set edit compact/font-scale PASS: SM-S931B runtime QA in `TrainIQ-Project/.codex/device-qa/2026-05-11-active-workout-logged-set-fontscale-qa/` started `QAFontRoutine`, logged one `Ab Wheel Rollout` set, opened the logged-set correction state, and captured `18-edit-logged-set-font-1.5.xml` with `NAF=0`, visible `Gewicht`, `Reps`, `RPE`, `Zelfde opnieuw`, `Training afronden`, and empty `19-font-1.5-logcat-errors.txt`; font scale was restored to `1.0`. Earlier `NAF=1/2` dumps were clipped offscreen/top-viewport icon-button artifacts and disappeared when the relevant controls were fully visible.
  - 2026-05-11 deeper AI routine compact/font-scale PASS: SM-S931B runtime QA in `TrainIQ-Project/.codex/device-qa/2026-05-11-ai-routine-deeper-fontscale-qa/` reproduced a pre-fix `NAF=1` deload switch in `33-ai-dialog-after-precise-scroll.xml`, then added a switch content description and verified `38-after-fix-ai-dialog-scrolled-controls.xml` with `Ervaringsniveau`, `Beginner`, `Gemiddeld`, `Gevorderd`, `Sessieduur: 60 min`, `Deload-richtlijn opnemen`, `content-desc="Deload-richtlijn opnemen"`, `NAF=0`, empty `39-after-fix-logcat-errors.txt`, and restored font scale `1.0`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- remaining risk: First-viewport and deeper scrolled AI routine dialog behavior, the routine-detail session-name field, a representative active-workout first exercise row, visible logger controls, logged-set correction controls, seeded 360x640 active-workout top/scrolled states, and top-level 360x640 Start/Training/Voeding/Coach/Instellingen/Voortgang states are verified at font scale 1.5. Manual TalkBack/Switch Access and deeper nested-flow interaction evidence remain open.
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
- status: partially-done
- owner suggestion: Android Health Connect owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:90` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:99` gate background reads on SDK availability, feature availability, and granted background permission.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:193` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:221` support per-metric sync state.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:325` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:336` use per-record-type changes tokens.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:348` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:367` page `readRecords` calls.
  - 2026-05-10 physical-device evidence on SM-S931B confirmed the app launches, Settings renders Health Connect status copy (`Health Connect: Toegang vereist`), the Android Health Connect controller package is present as `com.google.android.healthconnect.controller`, and crash buffers after launch/settings were empty under `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-runtime/`.
  - 2026-05-10 follow-up evidence found the physical device exposes Health Connect as `com.google.android.healthconnect.controller`; `AndroidManifest.xml` now declares visibility for that package and `com.android.vending` in addition to the older Play Health Connect package used by the provider-update onboarding intent.
  - 2026-05-11 repeatable non-mutating evidence collection is available at `TrainIQ-Project/scripts/collect-health-connect-runtime-evidence.ps1`; latest SM-S931B output is `TrainIQ-Project/.codex/device-qa/2026-05-11-health-connect-scripted-baseline-debug-v4/`.
  - End-to-end provider-missing, revoked permission, partial permission, and background-read flows remain unexecuted in this audit.
- external sources used:
  - Android Developers, Health Connect sync data, accessed 2026-05-10: https://developer.android.com/health-and-fitness/health-connect/sync-data
- expected target-state behavior: Health Connect behaves correctly across provider missing/update required, no permission, partial permission, revoked permission while open, and background-read availability states.
- concrete recommended fix: Extend the device/emulator Health Connect smoke from the current no-permission Settings evidence to provider missing/update, partial grants, revocation while app is open, and background-read unavailable/granted states; attach UI dumps/logcat evidence.
- regression risk: Medium. Permission flow fixes can alter consent clarity or accidentally block partial metrics.
- minimal verification command/check: Emulator/device manual script covering Health Connect provider and permission states, plus `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnect*" --console=plain --no-configuration-cache`.
- verification evidence:
  - 2026-05-10 PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnect*" --console=plain --no-configuration-cache`.
  - 2026-05-10 PARTIAL: SM-S931B launch/settings Health Connect smoke captured `health-packages.txt`, `launch-main.txt`, Settings UI dumps, and empty crash buffers in `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-runtime/`.
  - 2026-05-10 PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.health.HealthConnectReadPermissionsTest" --tests "*HealthConnect*" --console=plain --no-configuration-cache`.
  - 2026-05-10 PASS: `./gradlew.bat :app:processDebugMainManifest :app:assembleDebug :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 PASS: post-manifest visibility `:app:installDebug` and SM-S931B launch returned `Status: ok`, `WaitTime: 708`; evidence is in `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-followup/`.
  - 2026-05-11 PASS: added a source-level regression guard that `AndroidManifest.xml` still declares `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`, because `HealthConnectBackgroundSyncScheduler` depends on `HealthConnectDataSource.canReadInBackground()` and the release owner gate depends on this permission being explicit.
  - 2026-05-11 PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnect*" --console=plain --no-configuration-cache`.
  - 2026-05-11 PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 NOT_RUN: provider-missing, partial-grant, revoke-while-open, and background-read granted/unavailable runtime cases were not executed because the connected SM-S931B is a real device with Health Connect installed and no disposable permission profile was confirmed.
  - 2026-05-11 PASS/PARTIAL: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-health-connect-runtime-evidence.ps1 -AdbPath 'C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe' -OutputDir '.codex\device-qa\2026-05-11-health-connect-scripted-baseline-debug-v4'` captured cold main launch (`Status: ok`, `WaitTime: 706`), rationale launch (`Status: ok`), Health Connect manage-access launch (`Status: ok`), all requested health permissions ungranted, and an empty crash slice. Mutable provider/permission cases remain `NOT_RUN`.
  - 2026-05-11 PASS: non-mutating Health Connect rationale compact-font smoke in `TrainIQ-Project/.codex/device-qa/2026-05-11-health-connect-rationale-fontscale-qa/` launched `com.trainiq.core.health.HealthConnectPermissionsRationaleActivity` directly at font scale 1.5, captured top/scrolled/bottom UI dumps with `NAF=0`, visible rationale reasons and connect actions, empty TrainIQ crash slice, and restored font scale to `1.0`.

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

## 2026-05-10 Physical Device Normal/Weird Flow QA

- Device: Samsung SM-S931B, Android 16, physical device via `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Evidence folder: `.codex/device-qa/2026-05-10-normal-weird-flow/`.
- Build/install: PASS, `./gradlew.bat :app:assembleDebug :app:installDebug --console=plain --no-configuration-cache`.
- Unit verification: PASS, `./gradlew.bat :app:testDebugUnitTest --console=plain --no-configuration-cache`.
- Cold launch after unlock: PASS, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 721`; Home rendered with onboarding CTA and bottom navigation.
- Force-stop resume: PASS, `WaitTime: 702`; Home rendered after `am force-stop` and relaunch.
- Normal flow coverage: PASS for top-level Start, Training, Voeding, Coach, Meer/Instellingen navigation; settings scroll; empty routine dialog; AI routine generation dialog; nutrition add bottom sheet; manual product entry.
- Weird flow coverage: PASS/no crash for repeated back presses, rapid tab tapping, horizontal swipes, landscape rotation and portrait restore, force-stop resume, and back-stack exits.
- Crash evidence: PASS, `crash-buffer.txt`, `targeted-crash-buffer.txt`, `deep-crash-buffer.txt`, and `scanner-crash-buffer.txt` were empty.
- Findings from this pass:
  - Back-spam exits to the launcher from top-level screens. This appears platform-normal, but it means follow-up automated weird-flow scripts must relaunch before continuing tap sequences.
  - Scanner/photo permission flow was not precisely reached in this coordinate pass; follow-up should target the `Foto / AI-inschatting` action from the nutrition sheet with UIAutomator node bounds instead of approximate taps.

## 2026-05-10 Physical Device Scanner Follow-up QA

- Device: Samsung SM-S931B, Android 16, physical device via `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Evidence folder: `.codex/device-qa/2026-05-10-scanner-permission-precise/`.
- Build/install: PASS, `./gradlew.bat :app:assembleDebug :app:installDebug --console=plain --no-configuration-cache`.
- Cold launch: PASS, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 739`.
- Nutrition source sheet: PASS, `Toevoegen aan Ochtend` sheet rendered with `Handmatig product maken`, `Opgeslagen product gebruiken`, `Opgeslagen recept gebruiken`, `AI-context voor foto`, and `Foto / AI-inschatting`.
- Scanner entry: PASS, direct tap on `Foto / AI-inschatting` node bounds `[72,1842][1008,1986]` opened `Camerascanner`.
- Scanner capture/back flow: PASS, `Foto maken` action remained stable; Back returned from scanner to Voeding, then Back returned to Start.
- Crash evidence: PASS, `crash-buffer.txt`, `direct-crash-buffer.txt`, and `capture-crash-buffer.txt` were empty.
- Remaining risk: Camera permission denial was not shown because the device already allowed or did not prompt during this run. A true denial-path pass still needs app permission reset or a fresh install/user profile before release accessibility signoff.

## 2026-05-10 Physical Device Active-Workout Completion Attempt

- Device: Samsung SM-S931B, Android 16, physical device via `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Evidence folder: `.codex/device-qa/2026-05-10-active-workout-completion-debrief/`.
- Cold launch: PASS, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 771`; a later relaunch after the blocked setup returned `WaitTime: 716`.
- Routine setup: PARTIAL, `Lege routine maken` opened the expected `Routine maken` dialog. After hiding the Samsung keyboard before pressing `Maken`, the app showed `Routine aangemaakt.` and `QA routine` as the active routine.
- Completion/debrief coverage: NOT RUN/BLOCKED. The newly created empty routine showed `Open deze routine hieronder en voeg eerst een trainingsdag met oefening toe voordat je start.`, but no routine-detail, day-add, exercise-add, or workout-start control was visible in the UI dump after repeated taps and scroll attempts.
- Crash evidence: PASS, `crash-buffer.txt` was empty.
- Verification: PASS, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain --no-configuration-cache`.
- Follow-up needed: create or expose a reliable QA fixture/setup path for an active routine with at least one workout day, exercise, and routine set, then rerun active-workout finish and completion/debrief runtime QA.

## 2026-05-10 Training Setup Entry Polish

- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: empty active routines now expose a visible `Routine inrichten` action from the active-routine card. The action reuses the existing routine detail mode instead of adding a new builder path.
- Regression coverage: source-level tests now guard that the empty active routine card exposes the setup callback/label, the label remains Dutch, and the routine overview keeps the existing `Details` action.
- Verification: PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Physical-device evidence: PASS on SM-S931B, installed debug build, launched with `WaitTime: 1151`, created empty `QA routine`, verified `Routine inrichten` appears after scrolling the active-routine card, and verified tapping it opens the existing routine detail screen with `Info`/`Sessies`.
- Crash evidence: PASS, `.codex/device-qa/2026-05-10-training-setup-entry-after/final-crash-buffer.txt` was empty.

## 2026-05-10 Training Setup Tab Polish

- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: not-startable routines now open routine detail on `Sessies` first, so the `Routine inrichten` path lands where `Eerste oefening toevoegen` is available. Startable routines still open on `Info`, preserving the existing normal detail flow.
- Regression coverage: targeted tests guard the default detail tab for empty routines, routines with empty days, and routines with a startable exercise.
- Verification: RED, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache` failed on missing `initialRoutineDetailTab`.
- Verification: PASS, same targeted test after implementation.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Remaining risk: runtime device proof should now rerun the full empty-routine setup, add-first-exercise, active-workout finish, and completion/debrief flow.

## 2026-05-10 Training Setup QA Then Copy Polish

- QA scope: reran the targeted workout regression suite against the current setup-entry and setup-tab changes before making another app change.
- QA finding: the empty active-routine helper copy still said to open the routine below, while the UI now exposes a direct `Routine inrichten` action. This was a low-risk UX mismatch in the setup flow.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: updated the helper copy to `Tik op Routine inrichten en voeg eerst een oefening toe voordat je start.`
- Verification: QA baseline PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
- Verification: RED, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.active routine without exercises explains the editor action" --console=plain --no-configuration-cache` failed on the stale copy.
- Verification: PASS, same targeted test after copy polish.
- Verification: PASS, full `WorkoutInputValidationTest`.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Remaining risk: physical-device add-first-exercise and completion/debrief runtime QA remains the next step.

## 2026-05-10 Training Setup QA Then Button Polish

- QA scope: reran targeted workout regression coverage for the setup-entry/setup-tab path and inspected the current active-routine card source.
- QA finding: the `Routine inrichten` primary setup action was text-only while comparable add/setup actions in the Training screen use the Add icon for quick scanning. The existing source-level guard also sliced too broadly, so it could pass by seeing Add icons outside `ActiveRoutineCard`.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: added `Icons.Rounded.Add` to the `Routine inrichten` button and tightened the regression guard to inspect only `ActiveRoutineCard`.
- Verification: QA baseline PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
- Verification: RED, targeted setup-entry test failed once the source slice was narrowed and the Add icon was required.
- Verification: PASS, same targeted test after button affordance polish.
- Verification: PASS, full `WorkoutInputValidationTest`.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Remaining risk: physical-device add-first-exercise and completion/debrief runtime QA remains the next step.

## 2026-05-10 Training Setup Runtime QA Then Scroll Polish

- QA scope: physical-device runtime QA on Samsung SM-S931B for the empty active-routine setup path after the setup-entry, setup-tab, copy, and button-affordance polish.
- Evidence folder: `.codex/device-qa/2026-05-10-qa-polish-training-setup-runtime/`.
- QA evidence:
  - PASS build/install: `./gradlew.bat :app:assembleDebug :app:installDebug --console=plain --no-configuration-cache`.
  - PASS clean launch: `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 936`.
  - PASS recovery from weird keyboard/settings interruption: app returned to the create-routine dialog without crash and retained the entered routine name.
  - PASS empty active routine card showed the updated helper copy and `Routine inrichten`.
  - PASS tapping `Routine inrichten` opened detail mode with `Sessies` selected and `Eerste oefening toevoegen` reachable.
  - QA finding: opening detail from a scrolled Training list could preserve the old scroll offset, clipping the detail header/back affordance at the top.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: Training now keeps a `LazyListState` and scrolls to item 0 when routine detail mode opens.
- Verification: RED, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.routine detail resets training list scroll when opened" --console=plain --no-configuration-cache` failed before the scroll reset.
- Verification: PASS, same targeted test after polish.
- Verification: PASS, full `WorkoutInputValidationTest`.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Verification: PASS, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; after force-stop relaunch `Status: ok`, `WaitTime: 705`.
- Runtime smoke after polish: PASS, retapping `Routine inrichten` opened detail with `Terug naar routines`, routine title, `Info`, and selected `Sessies` visible at the top instead of clipped.
- Crash evidence: PASS, `.codex/device-qa/2026-05-10-qa-polish-training-setup-runtime/14-after-polish-crash-buffer.txt` was empty.
- Remaining risk: full add-first-exercise, active-workout start, finish, and completion/debrief runtime QA remains open.

## 2026-05-10 Extended QA Timebox

- QA scope: broad Android QA pass across build/test/lint, top-level navigation, Training setup, Nutrition, Settings/More, crash buffers, and source-level consistency checks.
- Timebox start: 2026-05-10T20:17:57+02:00.
- Evidence folder: `.codex/device-qa/2026-05-10-hour-qa/`.
- Build/static verification:
  - PASS, `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS source scan: no broad TODO/FIXME/HACK hotspots in app source; typed navigation use remains concentrated in `TrainIqNav.kt`.
  - PASS/expected: decorative `contentDescription = null` icons exist inside labeled buttons/rows; actionable workout controls inspected in this pass have labels or merged semantics.
- Physical-device QA:
  - Device: Samsung SM-S931B, Android 16, `RFCY60HNHNJ`.
  - PASS clean launch: `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 735`.
  - PASS top-level traversal: Start, Training, Voeding, Coach, and Meer rendered without crash.
  - PASS crash evidence: `.codex/device-qa/2026-05-10-hour-qa/04-tab-traverse-crash-buffer.txt` was empty.
  - PASS Nutrition smoke: Voeding rendered Vandaag/Toevoegen/AI-resultaat/Recepten tabs, meal sections, and add actions; crash buffer stayed empty.
  - PASS Settings/More smoke: Settings rendered status, theme mode, AI status, Health Connect status, and progress entry without crash.
  - PARTIAL Training setup: active empty routine card showed `Routine inrichten`; setup path remains reachable, but full add-first-exercise/start/finish/debrief was not completed in this pass because the coordinate attempt missed the setup button and scrolled into the exercise library instead of opening detail.
- New QA finding:
  - finding_id: QA-2026-05-10-022
  - priority: P2
  - area: UX, accessibility
  - status: done
  - owner suggestion: Android UI owner
  - current evidence with file references:
    - `.codex/device-qa/2026-05-10-hour-qa/03-tab-training.xml` shows that after clean launch with an existing empty active routine, the Training first viewport prioritizes `Routine maken` before `Actieve routine`.
    - In the same dump, the active routine card starts at `bounds="[48,1195][1032,1746]"` and the `Routine inrichten` button extends to `bounds="[96,1666][984,1818]"`, below the scroll viewport ending at `1746`, so the primary next setup action is partly clipped.
    - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt` currently emits `RoutineCreationCard` before `ActiveRoutineCard` in the Training `LazyColumn`.
  - expected target-state behavior: When an active routine exists, Training should prioritize the current next action. Empty active-routine setup should be visible and tappable without relying on precise scroll position.
  - concrete recommended fix: In the Training screen, render `ActiveRoutineCard` before `RoutineCreationCard` when an active routine exists, while keeping routine creation available directly below. Add a source-level ordering guard and rerun the physical-device setup smoke.
  - regression risk: Low to medium. It changes first-viewport ordering but does not remove any action; verify empty/no-active-routine onboarding still shows routine creation clearly.
  - minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`, and physical-device Training first-viewport smoke.
- External sources used: None. Local runtime evidence and source inspection were sufficient.
- Remaining risk: full add-first-exercise, active-workout start, finish, and completion/debrief runtime QA remains open.

## 2026-05-10 Training First-Viewport Order Polish

- Finding closed: QA-2026-05-10-022.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: Training now renders `ActiveRoutineCard` before `RoutineCreationCard` when `overview.activeRoutine` exists, while preserving the original no-active-routine flow where routine creation appears first.
- Regression coverage: added a targeted source-level guard that verifies the active-routine-first branch and the no-active-routine creation-first fallback.
- Verification: RED, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.active routine is prioritized before routine creation when present" --console=plain --no-configuration-cache` failed on the old ordering.
- Verification: PASS, same targeted test after implementation.
- Verification: PASS, full `WorkoutInputValidationTest`.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Physical-device evidence: PASS on SM-S931B, installed debug build, launched with `am start -S -W` `Status: ok`, navigated to Training, verified `Actieve routine` and fully visible `Routine inrichten` appear before `Routine maken`; evidence in `.codex/device-qa/2026-05-10-training-first-viewport-after-order-polish/15-training-final.xml`.
- Crash evidence: PASS, `.codex/device-qa/2026-05-10-training-first-viewport-after-order-polish/16-crash-buffer-final.txt` was empty.
- External sources used: None. Local runtime evidence and source inspection were sufficient.
- Remaining risk: full add-first-exercise, active-workout start, finish, and completion/debrief runtime QA remains open.

## 2026-05-10 Settings Gemini Key Help Polish

- Target-state link: Settings is the control center for AI, privacy, and local key handling; Gemini keys must not be committed or placed in `BuildConfig`.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/settings/SettingsUiStateTest.kt`
  - `TrainIQ-Project/README.md`
- Fix: Settings now shows a compact Gemini API-key setup instruction, the official Google AI Studio API Keys URL, and a warning not to share or commit the key. The Android README mirrors the same short setup path.
- Regression coverage: added a targeted Settings guard for the Google AI Studio label, URL, paste/save instruction, and no-commit warning.
- Verification: baseline PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
- Verification: RED, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.settings.SettingsUiStateTest.geminiApiKeyHelpPointsToGoogleAiStudioWithoutEncouragingCommittedSecrets" --console=plain --no-configuration-cache` failed before the helper functions existed.
- Verification: PASS, same targeted test after implementation.
- Verification: PASS, full `SettingsUiStateTest`.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Physical-device evidence: PASS on SM-S931B, installed debug build, launched Settings, verified `AI / Gemini`, `Google AI Studio`, `https://aistudio.google.com/app/apikey`, and `commit hem nooit`; evidence in `.codex/device-qa/2026-05-10-settings-gemini-key-help-polish/03-settings-ai.xml`.
- Crash evidence: PASS, `.codex/device-qa/2026-05-10-settings-gemini-key-help-polish/04-crash-buffer.txt` was empty.
- External sources used: Google AI for Developers, Using Gemini API keys, accessed 2026-05-10: https://ai.google.dev/gemini-api/docs/api-key. It documents creating/managing Gemini API keys in Google AI Studio and links to the API Keys page.
- Remaining risk: BYOK/direct-client production mode remains an owner decision under existing release/privacy findings.

## 2026-05-10 Training Setup To Completion Polish

- Target-state link: Training setup should let users move from an empty active routine to a saved workout and completion/debrief without hidden validation traps.
- QA evidence before fix: physical-device QA on SM-S931B could create a session, add `Ab Wheel Rollout`, start the workout, and reach the active logger, but tapping `Set loggen` failed with `Voer een gewicht tussen 0 en 1000 kg in.` even though the bodyweight set visually had no planned kg.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: active workout logger defaults missing planned weight to `0` for bodyweight/no-weight sets, and both UI draft rendering and `logSet` use the same effective draft that fills missing saved-draft fields from the planned set.
- Regression coverage: added targeted tests for bodyweight draft weight text and effective UI draft fallback, including persisted drafts with blank weight but planned reps.
- Verification: baseline PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
- Verification: RED, targeted bodyweight draft tests failed before `activeSetDraftWeightText` and `activeSetUiDraft` existed.
- Verification: PASS, full `WorkoutInputValidationTest` after implementation.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Tooling note: parallel Gradle invocations caused Kotlin/KSP incremental-cache lock failures; `./gradlew.bat --stop` followed by sequential test/build resolved the tooling issue.
- Physical-device evidence: PASS on SM-S931B. The app launched with `Status: ok`, Training started the QA routine, the active logger accepted `Set loggen` without weight/reps errors, `1 set gelogd` appeared, finish confirmation saved the partial session, and completion rendered `Voltooid`, `Slimme samenvatting`, `Lokale fallback`, `Sets 1`, and `Sterkste set: 0 kg x 12`.
- Crash evidence: PASS, `.codex/device-qa/2026-05-10-training-setup-to-completion-polish/40-after-save-crash-buffer.txt` was empty.
- External sources used: None. Local runtime evidence and existing app tests were sufficient.
- Remaining risk: completion with Gemini-enabled debrief still needs API-key/network-path evidence; this pass verified local fallback completion.

## 2026-08-05 Coach Profile Draft Recreation Polish

### QA-2026-08-05-023

- finding_id: QA-2026-08-05-023
- priority: P2
- area: Android lifecycle, UX, tests
- status: done
- owner suggestion: Android UI owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/coach/CoachScreen.kt` held every unsaved profile/goal field, biological-sex choice, activity-level choice, and validation error in ordinary `remember` state.
  - The target-state blueprint requires onboarding input to survive rotation, resize, app switching, and process recreation where feasible.
  - A real `MainActivity` instrumentation test reproduced the defect: `Rotatieprofiel` was entered and `Vrouw` selected, then `ActivityScenario.recreate()` reset the name field to empty.
- external sources used: None. Repository target-state requirements, Compose behavior, and local instrumentation evidence were sufficient.
- expected target-state behavior: Unsaved Coach profile/goal input remains intact across Activity recreation until the user saves or explicitly changes it.
- concrete implemented fix: `CoachScreen` now uses `rememberSaveable` for all editable profile/goal strings, `BiologicalSex`, activity level, and the current validation error while leaving persisted profile hydration and Room ownership unchanged.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/coach/CoachScreen.kt`
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/flow/TrainIqFlowSmokeInstrumentedTest.kt`
- regression risk: Low. The change affects only ephemeral pre-save UI state; persisted profile mapping, validation, use cases, and Room writes are unchanged.
- verification evidence:
  - RED: the focused connected test failed after recreation with `EditableText = ''` while expecting `Rotatieprofiel`.
  - PASS: the same focused connected test after `rememberSaveable` implementation.
  - PASS: focused `ProfileInputValidationTest`.
  - PASS: local baseline surfaces `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug`.
  - PASS: `:app:generateDebugRoomMigrationChainVerificationMarker`, including 45 connected tests on agent-owned `Pixel_8_API_36` / Android 16 with 0 failures.
  - PASS: `:app:assembleProfileable`, `:macrobenchmark:assembleAndroidTest`, and `:app:checkReleaseSigningReadiness`; production signing remains intentionally unconfigured.
  - PASS: post-fix debug install and cold launch returned `Status: ok`, `TotalTime: 1832`, with empty crash and TrainIQ fatal/ANR slices.
- minimal verification command/check: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest#profileDraftSurvivesActivityRecreationBeforeSave" --console=plain --no-configuration-cache`.
- remaining risk: This test proves Activity recreation. Full OS-killed process restoration remains bounded by Android's saveable-state delivery and should be rechecked if navigation state restoration changes.

### QA-2026-08-06-025

- finding_id: QA-2026-08-06-025
- priority: P2
- area: Android lifecycle, UX, tests
- status: done
- owner suggestion: Android UI owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt` kept the complete manual product draft, selected product ID, and field validation feedback in ordinary `remember` state.
  - The target-state blueprint requires manual nutrition logging to remain reliable without AI and user-entered state to survive rotation, resize, app switching, and Activity recreation where feasible.
  - A real `MainActivity` instrumentation test reproduced the defect on Android 16: after entering a complete `Rotatiehavermout` product draft and triggering fat validation feedback, `ActivityScenario.recreate()` reset the product name to empty and discarded the remaining draft.
- external sources used: None. Repository target-state requirements, Compose behavior, and local instrumentation evidence were sufficient.
- expected target-state behavior: Manual product name, barcode, nutrition values, edit selection, and current validation feedback remain intact across Activity recreation until the user saves, cancels, or explicitly changes them.
- implementation plan:
  1. Add a red connected regression test that enters all manual product fields, triggers representative validation feedback, and recreates `MainActivity`.
  2. Make only the product editor's draft, selected product ID, and derived error feedback saveable; leave submit guards, ViewModel actions, domain validation, and Room persistence unchanged.
  3. Re-run the focused test, then the local baseline, connected suite, migration marker, profileable packaging, signing-readiness check, and runtime crash smoke.
  4. Produce an explicitly non-production debug APK for user testing; retain production release blockers unchanged.
- concrete implemented fix: `NutritionScreen` now uses `rememberSaveable` for the selected product ID and manual product name, barcode, calories, protein, carbohydrates, and fat fields. A compact `listSaver` preserves `FoodFieldErrors` without making domain/storage models Android-specific.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/flow/TrainIqFlowSmokeInstrumentedTest.kt`
- regression risk: Low. The change affects only ephemeral pre-submit UI state; validation rules, duplicate-submit guards, save use cases, targeted Room writes, and persisted food models are unchanged.
- verification evidence:
  - Baseline PASS: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
  - RED: the focused connected test found `EditableText = ''` for `Productnaam` after Activity recreation while expecting `Rotatiehavermout`.
  - PASS: the focused connected test after the saveable-state implementation, including preservation of all six fields and `Vul een niet-negatieve waarde in.` feedback, on agent-owned `Pixel_8_API_36` / Android 16.
  - PASS: after-change `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`.
  - PASS: full `:app:connectedDebugAndroidTest` with 46 tests, 0 failures, 0 errors, and 0 skipped on the same emulator.
  - PASS: `:app:generateDebugRoomMigrationChainVerificationMarker` in the documented separate invocation.
  - PASS: `:app:assembleProfileable`, `:macrobenchmark:assembleAndroidTest`, and `:app:checkReleaseSigningReadiness` in separate invocations; production signing remains intentionally unconfigured.
  - Tooling note: the first combined no-daemon profileable/macrobenchmark/signing invocation exceeded the 120-second shell limit while Gradle was still active. After confirming the stop-requested daemon exited, the same three unchanged gates passed separately; no product failure was hidden or retried unchanged.
  - PASS: debug install and cold launch returned `Status: ok`, `TotalTime: 6045`; crash buffer and TrainIQ fatal/ANR slices were empty.
  - PASS: final debug APK at `TrainIQ-Project/app/build/outputs/apk/debug/app-debug.apk`, 51,116,437 bytes, SHA-256 `E4EDA2C206E28F2E7D646FFBC715E46E9228D1F7D018E10B0C626A666D8B7503`.
- minimal verification command/check: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest#manualFoodDraftSurvivesActivityRecreationBeforeSave" --console=plain --no-configuration-cache`.
- remaining risk: The connected test proves Activity recreation and representative validation feedback. Full OS-killed process restoration remains bounded by Android saveable-state delivery; AI-result drafts remain a separate future lifecycle batch.

### QA-2026-08-06-027

- finding_id: QA-2026-08-06-027
- priority: P2
- area: Android lifecycle, UX, tests
- status: done
- owner suggestion: Android UI owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt` kept meal type, meal name/notes, edit identity, manual quick-add quantities, meal item requests, and meal/quantity validation feedback in ordinary `remember` state.
  - The target-state blueprint requires complete manual meal logging without AI and editable rows before save, while entered state should survive recreation where feasible.
  - A real `MainActivity` instrumentation test reproduced the defect on Android 16: after adding a local product to an evening meal, editing it to 175g, adding a note, and triggering name validation, `ActivityScenario.recreate()` removed `Maaltijd controleren` because the item draft was empty again.
- external sources used: None. Repository target-state requirements, Compose behavior, and local instrumentation evidence were sufficient.
- expected target-state behavior: Meal type, name/notes, edit identity, product/recipe quantities, draft item references/grams/notes, and current validation feedback remain intact across Activity recreation until the user saves or explicitly changes the concept.
- implementation plan:
  1. Add a connected regression test that creates a deterministic local product, adds it to a meal, edits the meal, triggers validation feedback, and recreates `MainActivity`.
  2. Make only the manual meal editor's primitive draft and error state saveable; leave modal/scanner state, validation, submit guards, ViewModel actions, use cases, and Room persistence unchanged.
  3. Re-run the focused test and full local baseline, connected suite, migration marker, profileable/macrobenchmark packaging, signing-readiness, and runtime crash smoke.
  4. Produce an explicitly non-production debug APK and retain all production owner gates.
- concrete implemented fix: `NutritionScreen` now uses `rememberSaveable` for meal type, name, notes, quick-add quantities, and edit ID. Compact primitive `listSaver` implementations preserve `MealEntryRequest` values, `MealFieldErrors`, and quick-add errors without making domain models Android-specific.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/flow/TrainIqFlowSmokeInstrumentedTest.kt`
- regression risk: Low. Only ephemeral pre-submit UI state changed; meal validation, duplicate-submit guards, use cases, targeted Room transactions, persisted meals, action sheets, and scanner navigation remain unchanged.
- verification evidence:
  - Baseline PASS: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
  - RED: after correcting the test's expected validation copy to the repository-owned `Naam is verplicht.`, the focused connected test timed out after recreation because `Maaltijd controleren` no longer existed.
  - PASS: focused connected test after implementation, preserving `Avond`, the meal note, the local product reference, edited 175g, and name-validation feedback on agent-owned `Pixel_8_API_36` / Android 16.
  - PASS: after-change `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`.
  - PASS: full `:app:connectedDebugAndroidTest` with 48 tests, 0 failures, 0 errors, and 0 skipped on the same emulator.
  - PASS: `:app:generateDebugRoomMigrationChainVerificationMarker`.
  - PASS: `:app:assembleProfileable`, `:macrobenchmark:assembleAndroidTest`, and `:app:checkReleaseSigningReadiness`; production signing remains intentionally unconfigured.
  - PASS: debug install and cold launch returned `Status: ok`, `LaunchState: COLD`, `TotalTime: 5581`; the TrainIQ fatal/ANR scan was empty.
  - PASS: final debug APK at `TrainIQ-Project/app/build/outputs/apk/debug/app-debug.apk`, 51,116,437 bytes, SHA-256 `44C5CEC9B91534099F232AB33BC36B67551F34C97829162659D36A3991124FBD`.
  - Test-hardening note: the first broad-suite run exposed multiple same-label product actions retained by the long-lived instrumentation process. The test now waits for the product action and scrolls/clicks the last matching action, representing the newly created product deterministically; the changed focused test and the full suite then passed.
- minimal verification command/check: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest#mealDraftSurvivesActivityRecreationBeforeSave" --console=plain --no-configuration-cache`.
- remaining risk: The connected test proves Activity recreation and representative validation feedback. Full OS-killed process restoration remains bounded by Android saveable-state delivery; AI-result drafts remain a separate future lifecycle batch.

### QA-2026-08-06-026

- finding_id: QA-2026-08-06-026
- priority: P2
- area: Android lifecycle, UX, tests
- status: done
- owner suggestion: Android UI owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt` kept the selected recipe ID, recipe fields, quick-ingredient input, ingredient list, and recipe/ingredient validation feedback in ordinary `remember` state.
  - The target-state blueprint requires manual nutrition logging to remain reliable without AI and user-entered state to survive rotation, resize, app switching, and Activity recreation where feasible.
  - A real `MainActivity` instrumentation test reproduced the defect on Android 16: after adding an 80g ingredient and entering an unfinished invalid second ingredient, `ActivityScenario.recreate()` reset `Receptnaam` to empty and discarded the ingredient draft.
- external sources used: None. Repository target-state requirements, Compose behavior, and local instrumentation evidence were sufficient.
- expected target-state behavior: Recipe identity, recipe and quick-ingredient fields, added ingredients, and current validation feedback remain intact across Activity recreation until the user saves, cancels, removes, or explicitly changes them.
- implementation plan:
  1. Add a red connected regression test that covers both an already-added recipe ingredient and unfinished invalid quick-ingredient input before recreating `MainActivity`.
  2. Make only the recipe editor's manual draft and validation state saveable through primitive values; leave action sheets, scanner navigation, submit guards, ViewModel actions, domain validation, and Room persistence unchanged.
  3. Re-run the focused test, then the local baseline, connected suite, migration marker, profileable packaging, macrobenchmark packaging, signing-readiness check, and runtime crash smoke.
  4. Produce an explicitly non-production debug APK for user testing; retain production release blockers unchanged.
- concrete implemented fix: `NutritionScreen` now uses `rememberSaveable` for the selected recipe ID and every recipe/quick-ingredient input. Compact primitive `listSaver` implementations preserve ingredient `(foodId, grams)` pairs plus `RecipeFieldErrors`; the existing food-error saver preserves quick-ingredient validation feedback.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/flow/TrainIqFlowSmokeInstrumentedTest.kt`
- regression risk: Low. The change affects only ephemeral pre-submit UI state; validation rules, duplicate-submit guards, save use cases, targeted Room writes, and persisted recipe models are unchanged. Transient modals and scanner-navigation state intentionally remain transient.
- verification evidence:
  - Baseline PASS: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
  - RED: the focused connected test found `EditableText = ''` for `Receptnaam` after Activity recreation while expecting `Rotatierecept`.
  - PASS: the focused connected test after the saveable-state implementation, including the added 80g ingredient, all recipe fields, unfinished quick-ingredient values, and `Vul een niet-negatieve waarde in.` feedback, on agent-owned `Pixel_8_API_36` / Android 16.
  - PASS: after-change `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`.
  - PASS: full `:app:connectedDebugAndroidTest` with 47 tests, 0 failures, 0 errors, and 0 skipped on the same emulator.
  - PASS: `:app:generateDebugRoomMigrationChainVerificationMarker` in the documented separate invocation.
  - PASS: `:app:assembleProfileable`, `:macrobenchmark:assembleAndroidTest`, and `:app:checkReleaseSigningReadiness`; production signing remains intentionally unconfigured.
  - PASS: debug install and cold launch returned `Status: ok`, `LaunchState: COLD`, `TotalTime: 2905`; the TrainIQ fatal/ANR scan was empty.
  - PASS: final debug APK at `TrainIQ-Project/app/build/outputs/apk/debug/app-debug.apk`, 51,116,437 bytes, SHA-256 `507376A24B544421BCC380B0AD78EEB072E3DF1D616EB91B9A87EEEF76FC88E3`.
  - Tooling notes: the first focused Gradle filter was parsed as a task because its PowerShell argument was not quoted; the corrected quoted invocation produced the expected red test. The emulator disconnected before the first green attempt, so the same agent-owned AVD was safely restarted and the unchanged focused test then passed. The first standalone migration-marker shell omitted the SDK environment; the corrected canonical invocation passed. None of these were product failures.
- minimal verification command/check: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest#recipeDraftSurvivesActivityRecreationBeforeSave" --console=plain --no-configuration-cache`.
- remaining risk: The connected test proves Activity recreation and representative validation feedback. Full OS-killed process restoration remains bounded by Android saveable-state delivery; AI-result drafts remain a separate future lifecycle batch.

### QA-2026-08-06-028

- finding_id: QA-2026-08-06-028
- priority: P2
- area: Android lifecycle, UX, tests
- status: done
- owner suggestion: Android UI owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt` kept the optional AI meal-scan context in ordinary `remember` state even though the field is editable before any scanner, camera, credential, or network action.
  - The Nutrition target state requires scanner entry to remain available and recover cleanly across hostile display/lifecycle changes without blocking manual logging.
  - A real `MainActivity` instrumentation test reproduced the defect on Android 16: `Vegetarische maaltijd na krachttraining` was entered under `AI-resultaat`, then `ActivityScenario.recreate()` reset `Optionele context` to empty.
- external sources used: None. Repository target-state requirements, Compose behavior, and local instrumentation evidence were sufficient.
- expected target-state behavior: User-entered meal-scan context remains intact across Activity recreation until the user edits it or launches a flow that explicitly consumes it, regardless of whether AI is currently configured.
- implementation plan:
  1. Add a connected red regression test using the disabled-AI local fallback state so camera, Gemini, API keys, and network are never invoked.
  2. Make only the optional AI meal context saveable; leave AI preferences, credentials, scanner navigation, analysis results, and remote boundaries unchanged.
  3. Re-run the focused test and full local baseline, connected suite, migration marker, profileable/macrobenchmark packaging, signing-readiness, and runtime crash smoke.
  4. Produce an explicitly non-production debug APK and retain all production AI/release owner gates.
- concrete implemented fix: `NutritionScreen` now owns `aiContext` with `rememberSaveable`, matching the already saveable manual product, recipe, and meal inputs without persisting it to Room or preferences.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/flow/TrainIqFlowSmokeInstrumentedTest.kt`
- regression risk: Very low. One ephemeral string changes from `remember` to `rememberSaveable`; AI opt-in, key storage, scanner enablement, navigation, analysis, schema validation, and Gemini transport are unchanged.
- verification evidence:
  - Baseline PASS: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
  - RED: focused connected test found `EditableText = ''` after recreation while expecting `Vegetarische maaltijd na krachttraining`.
  - PASS: the same focused test after the one-line saveable-state implementation on agent-owned `Pixel_8_API_36` / Android 16 with AI disabled and no external boundary invoked.
  - PASS: after-change `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`.
  - PASS: full `:app:connectedDebugAndroidTest` with 49 tests, 0 failures, 0 errors, and 0 skipped on the same emulator.
  - PASS: `:app:generateDebugRoomMigrationChainVerificationMarker`.
  - PASS: `:app:assembleProfileable`, `:macrobenchmark:assembleAndroidTest`, and `:app:checkReleaseSigningReadiness`; production signing remains intentionally unconfigured.
  - PASS: after restarting the agent-owned AVD following an emulator disconnect, debug install and cold launch returned `Status: ok`, `LaunchState: COLD`, `TotalTime: 8785`; the TrainIQ fatal/ANR scan was empty.
  - PASS: final debug APK at `TrainIQ-Project/app/build/outputs/apk/debug/app-debug.apk`, 51,116,437 bytes, SHA-256 `92630A82B52839FF5A61D7191C84B2324DA31BA1A2C87C7D4222E1D8DE492FB9`.
- minimal verification command/check: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest#aiMealContextSurvivesActivityRecreationBeforeScan" --console=plain --no-configuration-cache`.
- remaining risk: Activity recreation is proven; OS-killed restoration remains bounded by Android saveable-state delivery. User-edited AI result items and their validation feedback remain a separate lifecycle batch requiring deterministic synthetic-result test infrastructure, not live Gemini or camera use.

### QA-2026-08-06-029

- finding_id: QA-2026-08-06-029
- priority: P2
- area: Android lifecycle, Nutrition UX, tests
- status: done
- owner suggestion: Android UI owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt` held editable AI-result rows and per-field validation errors in ordinary `remember` state.
  - `LaunchedEffect(scanResult)` also rebuilt those rows from the original analysis result after recreation, overwriting user corrections before save.
  - A deterministic `StateRestorationTester` component test reproduced the defect without camera, Gemini, credentials, network, or persistence: after editing `Originele bowl` to `Bewerkte bowl`, changing grams to `180`, and entering invalid fat `-4`, restoration returned the name to `Originele bowl`.
- external sources used: None. Repository target-state requirements, pinned local Compose test APIs, and synthetic local test evidence were sufficient.
- expected target-state behavior: User corrections to every editable AI meal-result field and current validation feedback survive saveable-state restoration until the user saves, deletes, or receives a genuinely new analysis result.
- implementation plan:
  1. Prove result-state loss with one synthetic connected component test and representative validation feedback.
  2. Serialize only editable row primitives and indexed field errors with compact Compose savers, keyed by the current scan result so a new analysis resets stale edits.
  3. Remove unconditional result rehydration from the lifecycle effect; leave scanning, Gemini, schema validation, submit behavior, and Room ownership unchanged.
  4. Run the full local quality matrix and produce a non-production debug APK.
- concrete implemented fix: `NutritionScreen` now initializes editable AI rows from a new `scanResult` through `rememberSaveable(scanResult, saver = ...)`. Primitive savers preserve all row values, nullable confidence/notes, and indexed `AiItemFieldErrors`; the scan-result effect now handles only routing and meal-type suggestions.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/features/nutrition/NutritionAiResultStateRestorationInstrumentedTest.kt`
- regression risk: Low. The change is limited to ephemeral pre-submit UI state. New scan results still reset the draft, while AI opt-in, credentials, camera navigation, Gemini transport, nutrition validation, use cases, and Room writes are unchanged.
- verification evidence:
  - Baseline PASS: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
  - RED: the focused test restored `EditableText = 'Originele bowl'` while expecting `Bewerkte bowl`.
  - GREEN: the focused test preserved `Bewerkte bowl`, `180`, `-4`, and `Vul een niet-negatieve waarde in.` after save/restore.
  - PASS: after-change `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`.
  - PASS: full `:app:connectedDebugAndroidTest` with 50 tests, 0 failures, 0 errors, and 0 skipped on agent-owned `Pixel_8_API_36` / Android 16.
  - PASS: `:app:generateDebugRoomMigrationChainVerificationMarker`, `:app:assembleProfileable`, `:macrobenchmark:assembleAndroidTest`, and `:app:checkReleaseSigningReadiness`; production signing remains intentionally unconfigured.
  - PASS: debug install and cold launch returned `Status: ok`, `LaunchState: COLD`, `TotalTime: 5444`; the TrainIQ fatal/ANR scan returned 0 matches.
  - PASS: final debug APK at `TrainIQ-Project/app/build/outputs/apk/debug/app-debug.apk`, 51,116,437 bytes, SHA-256 `E70A39254B034F5CEC02A0C3EE5E6570B7D413856C4826ABFE94F2C9F4EFC3B1`.
- minimal verification command/check: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.nutrition.NutritionAiResultStateRestorationInstrumentedTest#editedAiResultAndValidationErrorSurviveStateRestoration" --no-daemon --stacktrace`.
- remaining risk: The deterministic test proves Compose save/restore for meal-result edits. Recipe-target routing remains transient and should be a separate bounded lifecycle test; production release remains blocked by existing owner/manual/device gates.

### QA-2026-08-06-030

- finding_id: QA-2026-08-06-030
- priority: P2
- area: Android lifecycle, Nutrition UX, tests
- status: done
- owner suggestion: Android UI owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt` kept the recipe-versus-meal AI destination flag `aiScanForRecipe` in ordinary `remember` state.
  - Recipe photo actions set the flag before scanning, and `LaunchedEffect(scanResult)` uses it to route successful results to either `Recepten / Fotocontrole` or the ordinary meal `AI-resultaat` destination.
  - A deterministic `StateRestorationTester` component test reproduced the defect without camera, Gemini, credentials, network, or persistence: a synthetic local-fallback result initially opened in `Fotocontrole`, but save/restore reset the flag and rerouted it to the meal result flow.
- external sources used: None. Repository target-state requirements, pinned local Compose test APIs, and synthetic local test evidence were sufficient.
- expected target-state behavior: An AI result initiated for a recipe ingredient remains in `Recepten / Fotocontrole` across Compose save/restore until the user completes or leaves that flow.
- implementation plan:
  1. Prove the destination loss with a synthetic result and no external service or persistence boundary.
  2. Make only the boolean destination flag saveable; leave result data, camera/Gemini integration, credentials, submit behavior, and Room ownership unchanged.
  3. Re-run both AI-result restoration tests and the full local build, unit, lint, connected, Room, profileable, packaging, signing-readiness, and runtime matrix.
  4. Produce an explicitly non-production debug APK for user testing while retaining all release gates.
- concrete implemented fix: `NutritionScreen` now stores `aiScanForRecipe` with `rememberSaveable`, so the scan-result routing effect retains the recipe destination after restoration.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/features/nutrition/NutritionAiResultStateRestorationInstrumentedTest.kt`
- regression risk: Low. The production change is one ephemeral boolean state holder. The scanner, AI request, schema, credentials, nutrition validation, submission, and Room paths are unchanged.
- verification evidence:
  - Baseline PASS: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
  - RED: after save/restore the focused test could not find `Fotocontrole`, proving the recipe destination had been lost.
  - GREEN: both tests in `NutritionAiResultStateRestorationInstrumentedTest` passed, preserving the recipe destination and the previously covered editable row/error state.
  - PASS: after-change `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`.
  - PASS: full isolated `:app:connectedDebugAndroidTest` with 51 tests, 0 failures, 0 errors, and 0 skipped on agent-owned `TrainIQ_Agent_API36_20260806` / Android 16.
  - PASS: `:app:generateDebugRoomMigrationChainVerificationMarker`, including the isolated connected dependency.
  - PASS: `:app:assembleProfileable`, `:macrobenchmark:assembleAndroidTest`, and `:app:checkReleaseSigningReadiness`; production signing remains intentionally unconfigured.
  - PASS: final-branch debug install and cold launch returned `Status: ok`, `LaunchState: COLD`, `TotalTime: 3024`; the TrainIQ fatal/ANR scan returned 0 matches.
  - PASS: final debug APK at `TrainIQ-Project/app/build/outputs/apk/debug/app-debug.apk`, 51,116,437 bytes, SHA-256 `6B0726F56CB744930A42A037A5CE3249B5A99A243D389EF7F7CC2A20720C1E23`.
  - Isolation note: an unisolated aggregate Gradle attempt discovered an unrelated emulator as well as the agent AVD. The agent device completed 51 tests green, while the unrelated emulator disconnected; authoritative connected and Room-marker evidence was rerun through an isolated local adb server exposing only the agent AVD.
- minimal verification command/check: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.nutrition.NutritionAiResultStateRestorationInstrumentedTest" --no-daemon --stacktrace`.
- remaining risk: No known local synthetic AI-result routing/restoration gap remains in this bounded Nutrition batch. Production release remains blocked by existing owner/manual/safe-device gates.

### QA-2026-08-06-031

- finding_id: QA-2026-08-06-031
- priority: P2
- area: Android lifecycle, Settings UX, tests
- status: done
- owner suggestion: Android UI owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt` declared editable profile fields with `rememberSaveable`, but an unconditional `LaunchedEffect(profile)` rehydrated every field and cleared `profileInputError` whenever a recreated Activity launched a new composition.
  - `profileInputError` additionally used ordinary `remember`, so the invalid-field context itself was never registered for saveable restoration.
  - A real `MainActivity` test entered `Rotatie-instellingen`, set age to `0`, triggered `Leeftijd moet tussen 1 en 120 zijn.`, and proved recreation restored the name as empty before the fix.
  - The blueprint requires hostile display/lifecycle changes to recover cleanly and includes the Settings/profile form in critical lifecycle and profile evidence.
- external sources used: None. Repository target-state requirements, local Compose behavior, and the existing keyed saveable-state pattern provide sufficient evidence.
- expected target-state behavior: The complete Settings profile draft and its current validation feedback survive Activity recreation until the user edits input, saves successfully, resets the profile, clears app data, or receives a genuinely changed persisted profile.
- implementation plan:
  1. Add a red real-UI test with a non-empty name, invalid age, and field-specific error, then recreate `MainActivity`.
  2. Key every profile-derived saveable field and its error to `profile`, and remove the lifecycle effect that overwrites restored state; retain current initialization when persisted profile content actually changes.
  3. Re-run the focused test and full local build, unit, lint, connected, Room, profileable, packaging, signing-readiness, and runtime matrix.
  4. Produce an explicitly non-production debug APK while retaining all production release gates.
- concrete implemented fix: `SettingsScreen` now initializes its profile draft and `ProfileInputValidationError` through `rememberSaveable(profile)`. Removing the unconditional `LaunchedEffect(profile)` lets Android restore unsaved input/error state while a changed persisted `profile` still resets the keyed editors to the authoritative values.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/flow/TrainIqFlowSmokeInstrumentedTest.kt`
- regression risk: Low. The change affects only ephemeral pre-submit Settings state. Profile validation, use cases, Room ownership, destructive confirmations, AI credentials, Health Connect, and network behavior are unchanged.
- verification evidence:
  - Baseline reuse PASS: the unchanged branch commit from the preceding cycle had passed `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`, 51 connected tests, Room marker, profileable/macrobenchmark packaging, signing-readiness, and runtime smoke.
  - RED: after Activity recreation the strengthened focused test found `EditableText = ''` for `Naam` while expecting `Rotatie-instellingen`; the invalid age and error were also no longer available.
  - GREEN: the same focused test preserved `Rotatie-instellingen`, age `0`, and `Leeftijd moet tussen 1 en 120 zijn.` after the keyed saveable-state implementation.
  - PASS: after-change `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`.
  - PASS: full isolated `:app:connectedDebugAndroidTest` with 52 tests, 0 failures, 0 errors, and 0 skipped on agent-owned `TrainIQ_Agent_API36_20260806` / Android 16.
  - PASS: `:app:generateDebugRoomMigrationChainVerificationMarker`, including its isolated 52-test connected dependency.
  - PASS: `:app:assembleProfileable`, `:macrobenchmark:assembleAndroidTest`, and `:app:checkReleaseSigningReadiness`; production signing remains intentionally unconfigured.
  - PASS: debug install and cold launch returned `Status: ok`, `LaunchState: COLD`, `TotalTime: 3472`; the TrainIQ fatal/ANR scan returned 0 matches.
  - PASS: final debug APK at `TrainIQ-Project/app/build/outputs/apk/debug/app-debug.apk`, 51,116,437 bytes, SHA-256 `8A971D5891E1EB0636A5AC8F1732AFF3C44F48DCCE37CD5889346E34A608F586`.
  - Test-debugging note: the first post-recreation assertion waited for the off-screen header instead of the restored profile section. After correcting that test condition, a one-line saveable error change still failed; systematic root-cause tracing identified the overwriting `LaunchedEffect(profile)` before the final implementation.
- minimal verification command/check: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest#settingsProfileValidationErrorSurvivesActivityRecreation" --console=plain --no-configuration-cache`.
- remaining risk: The test proves Activity recreation with a null persisted profile. Full OS-killed process restoration remains bounded by Android saveable-state delivery; production release remains blocked by existing owner/manual/safe-device gates.

### QA-2026-08-06-032

- finding_id: QA-2026-08-06-032
- priority: P2
- area: Android lifecycle, training UX, tests
- status: done
- owner suggestion: Android UI owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt` kept both `showCreateDialog` and the unsaved `CreateRoutineDialog` name in ordinary `remember` state.
  - A real `MainActivity` test opened `Lege routine maken`, entered `Rotatieroutine`, recreated the Activity, and timed out waiting for `Routinenaam` because the complete dialog disappeared.
  - The blueprint requires unsaved form/workflow state to recover across Activity recreation and explicitly includes routine creation among lifecycle-sensitive training flows.
- external sources used:
  - Android Developers, `Start the emulator from the command line`, accessed 2026-08-06: https://developer.android.com/studio/run/emulator-commandline. Used only to recover the local Android test harness with an even explicit port and supported headless startup; the product defect and fix are established by repository/runtime evidence.
- expected target-state behavior: The manual routine-creation dialog and its non-empty draft name survive Activity recreation until the user creates the routine or explicitly dismisses the dialog.
- implementation plan:
  1. Add a red real-UI test that opens the manual routine dialog, enters a unique name, recreates `MainActivity`, and asserts the dialog plus draft remain.
  2. Make only the dialog visibility and name saveable; leave AI generation, loading, Room, navigation, and other workout state unchanged.
  3. Re-run the focused test, local baseline, full connected/Room matrix, release-like packaging, signing-readiness, and runtime smoke.
  4. Produce an explicitly non-production debug APK while retaining every production release gate.
- concrete implemented fix: `WorkoutScreen` now stores `showCreateDialog` with `rememberSaveable`, and `CreateRoutineDialog` stores its name with `rememberSaveable`. Normal create/dismiss callbacks still remove the dialog and its ephemeral draft.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/flow/TrainIqFlowSmokeInstrumentedTest.kt`
- regression risk: Low. The change affects only unsaved manual routine-creation UI state. Routine persistence, AI generation, workout execution, validation, Room, permissions, and network behavior are unchanged.
- verification evidence:
  - Baseline reuse PASS: exact `main` tree `cca2990a493596ea27d621375a3c9265b7b126a3` had passed build/unit/lint, 52 connected tests, Room marker, release-like packaging, signing-readiness, and runtime smoke in the preceding authorized cycle.
  - RED: the focused real-Activity test failed 1/1 after recreation with a timeout on the second `Routinenaam` wait at `TrainIqFlowSmokeInstrumentedTest.kt:149`; unchanged production code had closed the dialog.
  - GREEN: the identical focused test passed 1/1 after the two saveable-state changes and retained `Rotatieroutine`.
  - PASS: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`.
  - PASS: full `:app:connectedDebugAndroidTest` with 53 tests, 0 failures, 0 errors, and 0 skipped on agent-owned `Pixel_8_API_36` / Android 16.
  - PASS: `:app:generateDebugRoomMigrationChainVerificationMarker`, including its 53-test connected dependency.
  - PASS: `:app:assembleProfileable`, `:macrobenchmark:assembleAndroidTest`, and `:app:checkReleaseSigningReadiness`; production signing remains intentionally unconfigured.
  - PASS: debug install and cold launch returned `Status: ok`, `LaunchState: COLD`, `TotalTime: 8069`; the TrainIQ fatal/ANR scan returned 0 matches.
  - PASS: branch debug APK at `TrainIQ-Project/app/build/outputs/apk/debug/app-debug.apk`, 50,834,984 bytes, SHA-256 `BD7D25618E1291B14450351E6755EE623E039D768E3600A675D5D5ED624A8D46`.
  - Test-environment note: initial cold boots exceeded the first 55-second bound, and one pre-result run was terminated after its ADB transport stalled. Verbose, even-port headless startup isolated the problem to emulator boot/cleanup timing; the authoritative RED, GREEN, 53-test suite, packaging, install, launch, and crash evidence all ran afterward on the stable Pixel AVD.
- minimal verification command/check: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest#manualRoutineDraftSurvivesActivityRecreationBeforeCreate" --console=plain --no-configuration-cache`.
- remaining risk: Activity recreation is proven. Full OS-killed restoration remains bounded by Android saveable-state delivery; AI routine generation has separate transient form state and remains outside this manual-creation batch. Production release remains blocked by existing owner/manual/safe-device gates.
