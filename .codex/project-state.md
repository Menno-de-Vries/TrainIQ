# TrainIQ Project State

Last updated: 2026-05-08

## Current Status

Current status: done

Known risks: none known

TrainIQ is a working Android app under `D:\GitHub\TrainIQ\TrainIQ-Project` with package `com.trainiq`. It already has Hilt, typed Compose navigation, Material 3 UI, Health Connect integration, Gemini 2.5 Flash JSON calls, local AI fallbacks, diagnostics, and a meaningful unit test suite.

The canonical target-state file is `TrainIQ_Target_State_Blueprint.md`. The typo variant `TrainIQ_Target_State_Blprint.md` was not present. `TrainIQ_Target_State_Blueprint.md` had pre-existing uncommitted formatting changes and was not modified by this pass.

## Safety Constraints

- Preserve installed app data and the existing Gemini key.
- Avoid uninstall, app-data clear, emulator reset, signing/applicationId changes, destructive migrations, and secret exposure.
- Use incremental installs only when Android validation requires a deploy.

## Investigation Summary

- Blueprint compliance: many foundation items are present; Room source-of-truth, adaptive layout, background sync, UseCase/Health sync tests, and `@ViewModelScoped` remain gaps.
- UX/design: dynamic color was missing; adaptive layout, touch targets, contrast, content descriptions, and loading states need polish.
- Android QA: emulator launch of installed app succeeded non-destructively; no sampled TrainIQ crash; Health Connect permissions currently not granted.
- Architecture risk: largest risk is split persistence between Room scaffolding and JSON runtime storage.
- Gemini/data safety: key is in Preferences DataStore, masked in settings display, network logging is disabled; storage is not encrypted at rest.

## Implemented This Pass

- Added dynamic color support for Android 12+ in `TrainIqTheme`.
- Added `shouldUseDynamicColor` unit coverage.
- Masked the Settings Gemini API-key entry field while typing and used password keyboard behavior.
- Changed live Health Connect step refresh to require only the steps permission instead of all Health Connect permissions.
- Added focused tests for Settings key-entry masking and Health Connect permission policy.
- Added Android Keystore-backed encrypted Gemini key storage with non-destructive DataStore fallback.
- Wired Settings and AI usage gating through the encrypted key migration path while preserving the existing installed key path.
- Added backup/device-transfer exclusions for the encrypted Gemini SharedPreferences file.
- Added per-metric Health Connect status models and partial-permission behavior that keeps cached metrics instead of clearing sync state.
- Added adaptive navigation foundation using Material3 `WindowSizeClass`: bottom navigation on compact widths and navigation rail on medium/expanded widths.
- Added a staged Room source-of-truth migration plan; runtime cutover is intentionally deferred until import, rollback/fallback, and migration tests exist.
- Added fixture-based JSON-to-Room import/fallback tests without touching production `trainiq-state.json` or switching runtime reads.
- Added isolated `JsonRoomImportPlanner` and `JsonRoomImportCoordinator` helpers for Room-compatible import plans, schema parity gap reporting, failed-import fallback, and stable repeated-import plans.
- Added Room version 10 persistence foundation for nutrition detail and active workout/event state: foods, recipes, recipe ingredients, meal items, active workout sessions, drafts, collapsed exercise ids, active workout sets, workout log events, and workout event set snapshots.
- Added DAO insert/count methods and a real transactional `RoomJsonImportSink` for test-backed JSON-to-Room import verification.
- Added connected in-memory Room import/rollback tests that prove successful representative fixture import, forced transaction rollback, JSON fallback untrusted behavior, fixture immutability, and stable repeated imports.
- Added Room schema export with generated `TrainIqDatabase` schemas for versions 9 and 10.
- Added Room `MigrationTestHelper` infrastructure and a connected migration test proving representative version 9 data survives the 9 to 10 migration.
- Added production Room database/DAO providers that register all explicit migrations without destructive fallback; runtime repositories remain JSON-backed.
- Added a non-authoritative Room import dry run launched after `TrainIqLocalStore` successfully loads JSON.
- Added internal dry-run status reporting for success, failure, missing JSON, invalid JSON, schema blockers, and not-attempted state.
- Added unit and connected in-memory Room tests proving dry-run success/failure behavior while JSON remains authoritative.
- Added Room schema version 11 mirror import metadata for non-authoritative dry-run generations, source fingerprints, schema version, row counts, stale-row counts, mismatch counts, and authority flags.
- Added transactional dry-run reconciliation that clears stale mirror rows and imports current JSON-shaped rows only inside a successful `RoomDatabase.withTransaction` block.
- Added failure rollback safeguards so a failed dry run keeps the previous Room mirror metadata and rows intact while JSON remains authoritative.
- Added Room 10 to 11 migration wiring and connected migration coverage for version 10 mirror-data preservation plus version 11 metadata-table usability.
- Added a fail-closed `RoomRuntimeReadinessGate` for future Room runtime-read source selection. It returns structured ready/blocked results and never changes the current JSON runtime source.
- Added readiness checks for latest successful generation, current JSON fingerprint match, zero mismatch count, fresh mirror metadata, JSON-only authority flags, explicit migration-chain verification, and unknown-state fail-closed behavior.
- Moved JSON fingerprinting into shared `RoomJsonFingerprint` so dry-run imports and readiness checks compare the same non-secret SHA-256 fingerprint.
- Added older Room migration-chain tests for start versions 2 through 8. Versions 2, 3, 4, and 8 are grounded in committed database/entity history; versions 5, 6, and 7 are reconstructed from explicit migration SQL because no committed schema export exists for those intermediate versions.
- Hardened historical migration behavior by conditionally adding `workout_sets.repsInReserve` in `Migration3To4` and `Migration4To5` for databases that reached v4 through an older incomplete migration path.
- Added explicit migration-chain readiness states: verified, unverified, partial, missing, stale, failed, and unknown. The runtime-read readiness gate only passes `VERIFIED`.

## Validation

- Passed focused unit test:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.core.theme.ThemeDynamicColorTest --console=plain`
- Passed focused Settings test:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.features.settings.SettingsUiStateTest --console=plain`
- Passed focused Health Connect policy test:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.datasource.HealthConnectPermissionPolicyTest --console=plain`
- Passed focused Gemini key migration test:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.core.security.GeminiKeyMigrationTest --console=plain`
- Passed focused adaptive navigation policy test:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.navigation.AdaptiveNavigationPolicyTest --console=plain`
- Passed focused JSON-to-Room import/fallback test:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.JsonRoomImportPlannerTest --console=plain`
- Passed connected Room import/rollback test:
  `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.migration.RoomJsonImportSinkTest" --console=plain`
- Passed Room 9 to 10 migration test:
  `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest" --console=plain`
- Passed migration pass focused planner test:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.JsonRoomImportPlannerTest --console=plain`
- Passed migration pass compile/lint:
  `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Passed migration pass full unit suite:
  `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed Room dry-run unit test:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomImportDryRunTest --console=plain`
- Passed Room dry-run connected test:
  `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.migration.RoomImportDryRunInstrumentedTest" --console=plain`
- Passed dry-run pass compile/lint:
  `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Passed dry-run pass full unit suite:
  `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed Room reconciliation planner unit test:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.JsonRoomImportPlannerTest --console=plain`
- Passed Room reconciliation dry-run unit test:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomImportDryRunTest --console=plain`
- Passed Room reconciliation connected test:
  `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.migration.RoomImportDryRunInstrumentedTest" --console=plain`
- Passed Room 9/10/11 migration connected test:
  `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest" --console=plain`
- Passed Room reconciliation full unit suite:
  `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed Room reconciliation build/lint:
  `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Passed Room readiness gate targeted unit test:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomRuntimeReadinessGateTest --console=plain`
- Passed Room readiness adjacent import tests:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomImportDryRunTest --tests com.trainiq.data.migration.JsonRoomImportPlannerTest --console=plain`
- Passed Room readiness full unit suite:
  `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed Room readiness compile/lint:
  `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Passed older Room migration-chain connected test:
  `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest" --console=plain`
- Passed readiness gate migration-chain status unit test:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomRuntimeReadinessGateTest --console=plain`
- Passed older migration-chain pass full unit suite:
  `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed older migration-chain pass compile/lint:
  `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Passed Room schema/import compile and lint check:
  `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Passed full debug unit suite after Room schema/import pass:
  `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed full unit suite after JSON import pass:
  `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed compile/package check after JSON import pass:
  `.\gradlew.bat :app:assembleDebug --console=plain`
- Passed lint after JSON import pass:
  `.\gradlew.bat :app:lintDebug --console=plain`
- Passed lint after fixing the API-31 dynamic color guard:
  `.\gradlew.bat :app:lintDebug --console=plain`
- Passed final full debug unit tests:
  `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed compile check:
  `.\gradlew.bat :app:assembleDebug --console=plain`
- Passed full debug unit tests:
  `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed safe incremental install preserving app data:
  `.\gradlew.bat :app:installDebug --console=plain`
- Passed launch/UI/log smoke check on `emulator-5554`:
  focus is `com.trainiq/com.trainiq.MainActivity`, UI tree shows Start dashboard and bottom navigation, sampled app log has no TrainIQ fatal crash.
- Passed final safe replacement install and launch smoke:
  `.\gradlew.bat :app:installDebug --console=plain`, then `adb shell am start -W -n com.trainiq/.MainActivity`.

## Changed Files

- `TrainIQ-Project/app/src/main/java/com/trainiq/core/theme/Theme.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiUsageGate.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/di/AppModule.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/security/GeminiKeyMigration.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/security/AndroidKeystoreGeminiKeyStore.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/migration/JsonRoomImportPlanner.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/Entities.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/TrainIqDao.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/TrainIqDatabase.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/TrainIqMigrations.kt`
- `TrainIQ-Project/app/schemas/com.trainiq.core.database.TrainIqDatabase/9.json`
- `TrainIQ-Project/app/schemas/com.trainiq.core.database.TrainIqDatabase/10.json`
- `TrainIQ-Project/app/schemas/com.trainiq.core.database.TrainIqDatabase/11.json`
- `TrainIQ-Project/app/src/androidTest/java/com/trainiq/core/database/TrainIqDatabaseMigrationTest.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/migration/RoomImportDryRun.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/migration/RoomRuntimeReadinessGate.kt`
- `TrainIQ-Project/app/src/test/java/com/trainiq/data/migration/RoomImportDryRunTest.kt`
- `TrainIQ-Project/app/src/test/java/com/trainiq/data/migration/RoomRuntimeReadinessGateTest.kt`
- `TrainIQ-Project/app/src/androidTest/java/com/trainiq/data/migration/RoomImportDryRunInstrumentedTest.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/local/TrainIqLocalStore.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/domain/model/DomainModels.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/MainActivity.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/navigation/TrainIqNav.kt`
- `TrainIQ-Project/app/src/main/res/xml/backup_rules.xml`
- `TrainIQ-Project/app/src/main/res/xml/data_extraction_rules.xml`
- `TrainIQ-Project/app/build.gradle.kts`
- `TrainIQ-Project/gradle/libs.versions.toml`
- `TrainIQ-Project/app/src/test/java/com/trainiq/data/datasource/HealthConnectPermissionPolicyTest.kt`
- `TrainIQ-Project/app/src/test/java/com/trainiq/data/migration/JsonRoomImportPlannerTest.kt`
- `TrainIQ-Project/app/src/androidTest/java/com/trainiq/data/migration/RoomJsonImportSinkTest.kt`
- `TrainIQ-Project/app/src/test/resources/room-import/valid-representative-trainiq-state.json`
- `TrainIQ-Project/app/src/test/resources/room-import/minimal-valid-trainiq-state.json`
- `TrainIQ-Project/app/src/test/resources/room-import/missing-optional-fields-trainiq-state.json`
- `TrainIQ-Project/app/src/test/resources/room-import/malformed-trainiq-state.json`
- `TrainIQ-Project/app/src/test/java/com/trainiq/core/security/GeminiKeyMigrationTest.kt`
- `TrainIQ-Project/app/src/test/java/com/trainiq/navigation/AdaptiveNavigationPolicyTest.kt`
- `TrainIQ-Project/app/src/test/java/com/trainiq/features/settings/SettingsUiStateTest.kt`
- `TrainIQ-Project/app/src/test/java/com/trainiq/core/theme/ThemeDynamicColorTest.kt`
- `.codex/automation-state/trainiq-target-polish.md`
- `.codex/project-state.md`

## Staged Room Migration Plan

Runtime source of truth is still JSON-backed through `filesDir/trainiq-state.json`; Room now has a broader schema and tested import sink, but a full cutover in this pass would still risk installed data. Runtime reads were not switched.

Current import foundation:

- Fixture tests cover representative JSON, minimal JSON, missing optional fields, malformed JSON, failed import fallback, and stable repeated import plans.
- `JsonRoomImportPlanner` maps Room-compatible rows for profile, routines, days, exercises, workout exercises, routine sets, workout sessions, performed exercises, workout sets, meal summary totals, food items, recipes, recipe ingredients, meal items, active workout state, workout log events, and measurements.
- `JsonRoomImportCoordinator` returns `Imported` only after the sink transaction succeeds; on parse/import failure it returns the provided JSON fallback state and marks Room as not trusted.
- `RoomJsonImportSink` imports plans with `RoomDatabase.withTransaction`; connected tests prove forced failure after early writes rolls back inserted rows.
- `RoomImportDryRun` now imports the loaded/migrated JSON state snapshot into Room best-effort after JSON load. It never changes the runtime source decision: `jsonAuthoritative = true`, `roomAuthoritative = false`.
- `TrainIqLocalStore.roomImportDryRunStatus` exposes internal non-secret dry-run status while `TrainIqLocalStore.state` remains the JSON-backed runtime state flow.
- `TrainIqDatabase` now exports schemas; version 9 was reconstructed from `HEAD` in a temporary worktree through Room's normal schema export flow, and version 10 was generated from the current schema.
- `TrainIqDatabaseMigrationTest` creates a version 9 database from the exported schema, seeds representative existing rows, runs `Migration9To10`, validates the version 10 schema, verifies old rows survive, and inserts/queries new version 10 tables.
- `RoomMirrorImportRunEntity` records non-secret mirror generation metadata: generation id, source fingerprint, import timings, schema version, expected/imported/stale/mismatch row counts, and flags that keep JSON authoritative and Room non-authoritative.
- `RoomJsonImportSink` now reconciles mirror rows by clearing stale mirror tables and repopulating the current JSON-shaped plan inside one transaction. A failed transaction preserves the prior mirror and metadata.
- `TrainIqDatabaseMigrationTest` also creates a version 10 database, seeds representative mirror rows, runs `Migration10To11`, validates the version 11 schema, verifies old mirror rows survive, and inserts/queries import-run metadata.
- `RoomRuntimeReadinessGate` now exists for future runtime-source selection. It only returns `Ready` when the latest mirror run is successful, the current authoritative JSON fingerprint matches the mirror fingerprint, mismatch count is zero, the mirror is not stale, authority flags still mark JSON as authoritative, and migration-chain readiness is explicitly true.
- `RoomRuntimeReadinessGate` fails closed with structured reasons for missing JSON, missing metadata, failed/latest in-progress import, fingerprint mismatch, mirror mismatch, stale mirror, unverified migration chain, and unknown states.
- `TrainIqDatabaseMigrationTest` now creates legacy databases for start versions 2 through 8, seeds representative profile/routine/session/set/meal/measurement data, migrates each to the current schema through `TrainIqMigrations.All`, verifies legacy values/defaults survive, and verifies current food/mirror metadata tables are usable.
- `Migration3To4` and `Migration4To5` now conditionally add `workout_sets.repsInReserve` if missing. This is non-destructive and covers databases that reached v4 through older migration code that did not add the column.
- No production runtime reads were switched to Room; no production file deletion, JSON fallback removal, or authoritative Room source selection was added.

Current schema parity gaps:

- Room 10 now covers the previously missing JSON domains for nutrition detail and active workout/event state in the import plan.
- Production database builder/migration registration now exists, but repository cutover wiring is intentionally not added.
- Non-authoritative Room dry-run population now has generation metadata, stale-row delete reconciliation, mismatch counts, rollback tests, and a fail-closed runtime-read readiness gate.
- Version 2 through 8 migration-chain safety is now covered by connected tests using committed schema evidence where available and explicit migration SQL where intermediate commits are missing.
- Version 10 to 11 migration safety is covered for mirror metadata.
- The readiness gate intentionally requires `RoomMigrationChainVerification.VERIFIED`; production cutover wiring still must keep this non-verified until live-shape import validation also passes.
- Room tables intentionally avoid strict foreign-key enforcement for legacy JSON import tolerance; hardening can follow after live-data import validation.

Required stages:

1. Feed migration-chain and live-shape import validation status into the readiness gate only after both pass in the app's non-runtime dry-run path.
2. Add a non-runtime cutover status provider that remains non-verified by default outside tests.
3. Switch one low-risk repository flow to Room-backed observation only after the readiness gate, migration/import/rollback tests, and Android smoke pass.

## Known Existing Dirty Worktree

- `TrainIQ_Target_State_Blueprint.md` was already modified before this implementation pass. Treat it as user-owned unless explicitly instructed otherwise.

## Next Safe Implementation Order

1. Add a non-authoritative migration/import verification status provider into the Room readiness gate while keeping runtime Room reads disabled.
2. Continue Health Connect hardening with per-metric isolation for incremental sync token/change failures and explicit background-safe scheduling policy.
3. Add deeper screen-level adaptive layouts beyond the Home dashboard, especially Training list/detail and Nutrition editor panes.
4. Run medium/expanded visual QA with screenshots once emulator display resizing is available.
5. Add a later, explicitly verified cleanup stage for legacy Gemini DataStore key removal only after encrypted readback is proven across installs.
6. Continue accessibility pass for icon descriptions and 48dp minimum touch targets.

## 2026-05-07 Continuation Update

- Added a synthetic, non-secret live-shape JSON fixture at `TrainIQ-Project/app/src/test/resources/room-import/live-shape-current-trainiq-state.json`.
- Added unit validation that the current JSON shape maps into the Room import plan without schema parity gaps and without mutating the fixture.
- Added connected in-memory Room validation that the live-shape fixture transactionally populates profile, exercise, routine, nutrition detail, active workout, and workout event mirror tables.
- Extended `RoomRuntimeReadinessGate` with explicit live-shape import verification statuses. Runtime Room reads remain blocked unless live-shape validation is explicitly `VERIFIED`; default/unknown/partial/failed states fail closed.
- Added Health Connect per-metric sync-status policy tests and changed full sync so failed metric reads are reported per metric while successful metrics remain synced.
- Wired `WindowSizeClass` into the Home route and made the dashboard grid use 2 columns on compact, 3 on medium, and 4 on expanded widths. Navigation rail behavior remains for medium/expanded widths.
- Added `RoomImportDryRunStatus.toReadinessVerification(...)` so non-runtime dry-run results can feed the readiness gate in a fail-closed way; migration-chain status remains externally supplied and unverified by default until proven.
- Safe validation completed after this continuation:
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.JsonRoomImportPlannerTest --console=plain`
  - `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.migration.RoomJsonImportSinkTest" --console=plain`
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.datasource.HealthConnectPermissionPolicyTest --console=plain`
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.navigation.AdaptiveNavigationPolicyTest --console=plain`
  - `.\gradlew.bat :app:testDebugUnitTest --console=plain`
  - `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
  - `.\gradlew.bat :app:installDebug --console=plain`
  - `adb shell am start -W -n com.trainiq/.MainActivity`
  - repeated after the final readiness-status mapper: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
  - repeated after the final readiness-status mapper: `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Android QA evidence: emulator `emulator-5554` launched `com.trainiq/.MainActivity`; UI tree shows TrainIQ Start dashboard and bottom navigation; sampled logcat showed no TrainIQ fatal crash.
- `TrainIQ_Target_State_Blueprint.md` remains user-owned and was not edited in this continuation. No `trainiq-state.json` file exists in the repo tree, and no installed app data was cleared.

Remaining blockers:

- Room: JSON is still authoritative. Runtime Room cutover still needs a production/non-runtime verification status provider and final readiness-gate integration that proves migration chain plus live-shape import verification from the app's own dry-run status. Older migration-chain tests and fixture live-shape import tests now pass.
- Health Connect: full sync now isolates per-metric read failures, but incremental sync still depends on global Changes API behavior and needs a background-safe scheduling/foreground policy before claiming full target-state compliance.
- Adaptive layout: app shell and Home dashboard adapt to WindowSizeClass, but Training, Nutrition, Progress, Coach, and Settings still need screen-specific wide layouts and medium/expanded screenshot QA.
- Gemini key cleanup: encrypted migration/hardening remains non-destructive; legacy cleanup is intentionally deferred until a later verified stage proves safe.

## 2026-05-08 Continuation Update

- Stopped newly saved Gemini API keys from being written back to the legacy plaintext DataStore key. `SettingsViewModel.saveGeminiKey` now routes only through encrypted `AiUsageGate.saveApiKey`; the legacy DataStore value is still readable for migration fallback and clearable for user-requested removal.
- Corrected Health Connect active calories to use `ActiveCaloriesBurnedRecord` and `android.permission.health.READ_ACTIVE_CALORIES_BURNED` instead of total calories. Manifest, datasource tracked records, required permission map, UI permission request set, mapper, and tests were updated together.
- Updated Health Connect rationale copy to six signals, added workouts as an explicit rationale item, and changed calorie wording to active calories.
- Hardened incremental Health Connect failure behavior so existing cached metrics and the current changes token are preserved when the Changes API fails; metric statuses become failed instead of losing the dashboard cache through a top-level error.
- Added `HealthConnectBackgroundSyncWorker` and `HealthConnectBackgroundSyncScheduler` using WorkManager unique periodic work with backoff. `MainActivity` schedules it after startup without uninstalling, clearing data, or changing app identity.
- Wired `TrainIqLocalStore` to evaluate `RoomRuntimeReadinessGate` after JSON load/update and dry-run import. Runtime reads remain JSON-backed because production migration-chain verification remains `UNVERIFIED`; this is intentional fail-closed behavior.
- Changed high-value Settings draft state to `rememberSaveable` and wrapped barcode scanner saved-state result handling in typed helper functions.

Validation on 2026-05-08:

- Passed: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.core.security.GeminiKeyMigrationTest --tests com.trainiq.features.settings.SettingsUiStateTest --console=plain`
- Passed: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.core.health.HealthConnectReadPermissionsTest --tests com.trainiq.data.datasource.HealthConnectPermissionPolicyTest --console=plain`
- Passed: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.core.health.HealthConnectBackgroundSyncPolicyTest --tests com.trainiq.data.datasource.HealthConnectPermissionPolicyTest --console=plain`
- Passed: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomRuntimeReadinessGateTest --tests com.trainiq.data.migration.RoomImportDryRunTest --console=plain`
- Passed: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.navigation.AdaptiveNavigationPolicyTest --tests com.trainiq.features.settings.SettingsUiStateTest --console=plain`
- Passed: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed: `.\gradlew.bat :app:assembleDebug --console=plain`
- Passed: `.\gradlew.bat :app:lintDebug --console=plain`
- Not run: connected Android install/launch QA, because `adb` is not available on PATH in this environment.

Current remaining work:

1. Full Room runtime source-of-truth cutover is still intentionally blocked by production migration-chain verification and Android smoke availability; JSON remains authoritative and the readiness gate exposes the blocked state.
2. Health Connect now has correct active calories, per-metric full-sync isolation, cached incremental failure fallback, and WorkManager scheduling, but still needs fake-client coverage for SDK states, token expiry, pagination, partial/revoked permissions, and provider update paths.
3. Adaptive layout remains partial outside the shell/Home; screen-specific medium/expanded layouts and visual QA remain for Training, Nutrition, Progress, Coach, and Settings.
4. Workout single-`uiState` consolidation and repository/use-case extraction remain P2 architecture refactors that should be handled one bounded context at a time with tests.
5. Legacy Gemini DataStore cleanup is still deferred by design until encrypted readback can be verified safely across installed-app lifecycle scenarios.

## 2026-05-08 Room Verification Provider Update

- Added `RoomMigrationChainVerificationProvider` as the production migration-chain verification source for Room runtime-read readiness.
- Added structured provider model fields for current Room version, required covered range, covered range, verification status, reason, marker id, verification timestamp, and freshness.
- Added `RoomMigrationChainVerificationMarkerSource` and a production `NoTrustedRoomMigrationChainVerificationMarkerSource`. This is deliberately fail-closed: production reports `NOT_RUN` until a future trusted marker source exists.
- Extended the readiness model with `RoomMigrationChainVerification.NOT_RUN` and `RoomRuntimeReadinessFailure.MIGRATION_CHAIN_NOT_RUN`.
- `TrainIqLocalStore` now passes `roomMigrationChainVerificationProvider.report().status` into the readiness verification created from the dry-run import status. JSON remains the runtime source because production migration verification is not `VERIFIED`.
- Added provider tests and expanded readiness-gate tests for non-verified states, missing provider report mapping, stale/partial markers, and verified-marker behavior.
- Resolved adb discovery by using `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe`; adb is still not on PATH, but connected QA can run through the full path.

Validation on provider update:

- Passed: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomMigrationChainVerificationProviderTest --tests com.trainiq.data.migration.RoomRuntimeReadinessGateTest --console=plain`
- Passed: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed: `.\gradlew.bat :app:assembleDebug --console=plain`
- Passed: `.\gradlew.bat :app:lintDebug --console=plain`
- Passed safe update install: `.\gradlew.bat :app:installDebug --console=plain`
- Passed adb launch: `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -W -n com.trainiq/.MainActivity`
- Passed UI/focus smoke: `mCurrentFocus=Window{... com.trainiq/com.trainiq.MainActivity}` and UI tree includes `TrainIQ`, `Start`, `Training`, `Voeding`, `Trend`, `Coach`, `Meer`.
- Passed crash-buffer check: filtered `adb logcat -d -b crash` had no TrainIQ fatal entries.
- Passed connected Room migration tests: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest" --console=plain`
- Passed connected Room import tests: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.migration.RoomJsonImportSinkTest" --console=plain`

Current remaining blockers:

1. Full Room runtime source-of-truth cutover remains intentionally blocked by the fail-closed production provider. A trusted, non-destructive marker source must be added before the provider can report `VERIFIED`.
2. JSON remains authoritative and fallback remains intact; no runtime read path was switched to Room.
3. Health Connect has correct active calories, WorkManager scheduling, per-metric full-sync isolation, and cached incremental failure fallback; deeper fake-client SDK/token/revocation coverage remains future P2 test work.
4. Adaptive medium/expanded layouts outside Home/shell and the Workout single-`uiState` / repository extraction refactors remain separate P2 implementation passes.

## 2026-05-08 End-to-End Fix Update

- Diagnosed Gemini save visibility failure: after plaintext DataStore rewrites were stopped, Settings/Nutrition/Camera Scanner still observed legacy `AiPreferences.apiKey`, so encrypted saves could succeed while the UI still looked unconfigured.
- Added `AiUsageGate.resolveSettings(...)` and routed Settings, Nutrition, and Camera Scanner preference flows through it. Encrypted storage is now the configured-state source, with legacy plaintext DataStore kept only as a non-destructive migration fallback.
- Changed encrypted Gemini key writes and clears to synchronous `SharedPreferences.commit()` so `saveApiKey(...)` returns false on durable preference write failure instead of reporting success after an async enqueue.
- Added a regression test proving failed encrypted save does not overwrite an existing encrypted key.
- Added a trusted debug Room marker generation path: `generateDebugRoomMigrationChainVerificationMarker` depends on `connectedDebugAndroidTest` and writes a generated debug asset marker only after connected instrumentation succeeds.
- Switched production marker lookup to `AssetRoomMigrationChainVerificationMarkerSource`, which reads `room_migration_chain_verification_marker.json` from assets and rejects markers for the wrong build variant.
- Added marker payload SHA-256 validation and provider tests for absent, valid, stale, partial, invalid-hash, missing-hash, and wrong-variant markers. Missing/invalid/stale/untrusted marker states still fail closed.
- Runtime Room reads were not enabled. The repository/local-store runtime remains JSON-backed; the readiness gate continues to expose JSON as authoritative and Room as non-authoritative.
- Generated debug marker verified metadata only: marker id, build variant, test task, Room version/range coverage, verification timestamp, migration count, and payload hash. No secrets or user data are included.

Validation on end-to-end fix:

- Passed focused Gemini/settings/Room tests:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.core.security.GeminiKeyMigrationTest --tests com.trainiq.features.settings.SettingsUiStateTest --tests com.trainiq.data.migration.RoomMigrationChainVerificationProviderTest --tests com.trainiq.data.migration.RoomRuntimeReadinessGateTest --console=plain`
- Passed full debug unit suite:
  `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed connected Room migration/import/dry-run tests:
  `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest,com.trainiq.data.migration.RoomJsonImportSinkTest,com.trainiq.data.migration.RoomImportDryRunInstrumentedTest" --console=plain`
- Passed marker generation path:
  `.\gradlew.bat :app:generateDebugRoomMigrationChainVerificationMarker --console=plain`
- Passed package build:
  `.\gradlew.bat :app:assembleDebug --console=plain`
- Passed lint:
  `.\gradlew.bat :app:lintDebug --console=plain`
- Passed safe update install:
  `.\gradlew.bat :app:installDebug --console=plain`
- Passed launch/focus smoke:
  `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -W -n com.trainiq/.MainActivity`
- Passed UI smoke:
  UI tree showed `TrainIQ`, `Start`, `Training`, `Voeding`, `Trend`, `Coach`, and `Meer`.
- Passed recent filtered logcat crash/error check:
  no `FATAL EXCEPTION`, `AndroidRuntime`, Room/SQLite app crash, or TrainIQ fatal entry found after launch.

Safety notes:

- No uninstall, app-data clear, emulator reset, signing/applicationId change, destructive Room migration, JSON fallback removal, or runtime Room source switch was performed.
- No Gemini key was printed, logged, screenshotted, exported, or overwritten during manual QA. Live fake-key save was intentionally not performed against the installed app because it could overwrite a real user key.
- `trainiq-state.json` was not found in the repo tree and was not modified.

Superseded remaining work from the previous checkpoint:

1. Release/CI marker generation was completed in the next section.
2. JSON remains authoritative by design because this pass did not switch any runtime read path to Room.
3. Manual Gemini save/reopen/restart QA was completed on a fresh emulator install with a fake non-secret key.

## 2026-05-08 Release/CI Marker Completion

- Added release/profileable marker generation tasks matching the debug path:
  - `generateReleaseRoomMigrationChainVerificationMarker`
  - `generateProfileableRoomMigrationChainVerificationMarker`
  - `generateCiRoomMigrationChainVerificationMarkers`
- All marker generation remains gated by connected instrumentation through `connectedDebugAndroidTest`, which is the available connected test task in this project.
- Added variant-specific generated asset directories for debug, release, and profileable builds.
- Added `roomMigrationVerificationTimestampMillis` as an optional Gradle property and task input so CI can generate deterministic/testable marker metadata.
- Verified release marker generation, release APK packaging, debug build, release build, lint, unit tests, connected Room migration/import tests, and emulator smoke.
- Verified Gemini key save/reopen/restart on a fresh emulator install. The UI showed only the masked configured key; no raw key was logged or printed.
- Runtime Room source-of-truth remains JSON-backed. The Room marker blocker is resolved, but the source switch remains intentionally off because no runtime read path was changed in this focused pass.

Validation on release/CI marker completion:

- Passed provider/readiness tests:
  `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomMigrationChainVerificationProviderTest --tests com.trainiq.data.migration.RoomRuntimeReadinessGateTest --console=plain`
- Passed release marker generation:
  `.\gradlew.bat :app:generateReleaseRoomMigrationChainVerificationMarker "-ProomMigrationVerificationTimestampMillis=<now>" --console=plain`
- Passed CI marker generation:
  `.\gradlew.bat :app:generateCiRoomMigrationChainVerificationMarkers "-ProomMigrationVerificationTimestampMillis=<now>" --console=plain`
- Passed full unit tests:
  `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed debug and release package builds:
  `.\gradlew.bat :app:assembleDebug :app:assembleRelease --console=plain`
- Passed lint:
  `.\gradlew.bat :app:lintDebug --console=plain`
- Passed release APK marker check:
  `jar tf app-release-unsigned.apk` showed `assets/room_migration_chain_verification_marker.json`.
- Passed fresh-install emulator smoke:
  `.\gradlew.bat :app:installDebug --console=plain`
  `adb shell am start -W -n com.trainiq/.MainActivity`
- Passed Gemini persistence smoke:
  fake non-secret key saved on a fresh install; after `adb shell am force-stop com.trainiq` and cold relaunch, Settings still showed the masked configured key and the update-key action.
- Passed TrainIQ process logcat filter:
  no app crash, Room/migration, marker, Gemini, or security exception entries from the running app process.

Non-blocking follow-up items:

- Broader adaptive screen layouts outside Home/shell remain P2 polish.
- Workout single-`uiState` consolidation remains a P2 architecture refactor.
- Deeper Health Connect fake-client coverage for provider/token/revocation states remains P2 validation polish.
