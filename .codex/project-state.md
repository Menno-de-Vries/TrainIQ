# TrainIQ Project State

Last updated: 2026-05-07

## Current Status

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
