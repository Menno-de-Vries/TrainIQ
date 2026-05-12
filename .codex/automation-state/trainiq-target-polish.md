# TrainIQ Target Polish Automation State

Last updated: 2026-05-12

Current app-ready context: this is historical target-polish state from earlier implementation passes. It is superseded for current release decisions by `.codex/automation-state/trainiq-app-ready-goal.md`, `.codex/automation-state/trainiq-cycle.md`, `docs/TrainIQ_App_Ready_Completion_Audit.md`, and `docs/TrainIQ_Target_State_Progress.md`. TrainIQ remains `not ready-to-use` until owner/manual/safe-device blockers are closed.

## Safety Rules

- Do not run `adb uninstall`, `pm clear`, storage/cache clears, emulator resets, or signing/applicationId changes.
- Do not expose, print, export, screenshot, commit, or log the installed Gemini key.
- Prefer incremental install/debug deploys that preserve `com.trainiq` app data.

## Blueprint Requirement Checklist

| Requirement | Status | Evidence / Gap |
| --- | --- | --- |
| Health Connect automatically collects passive health data | partial | Source checks SDK and syncs metrics; no background-safe scheduler yet. |
| Recognize trends in recovery, sleep, nutrition, and training | partial | Dashboard/progress/coach logic exists; recovery/sleep trend depth remains limited. |
| Proactive coach advice | partial | Coach/weekly/workout advice exists, mostly explicit-action driven. |
| Calm, fast, modern UX | partial | Material 3 UI exists; adaptive layout/accessibility/contrast polish remains. |
| Offline-safe behavior | partial | Local fallbacks exist; Room source-of-truth target not reached. |
| Structured AI output, no parsing hacks | done | Gemini requests use JSON MIME and `JsonParser.parseString`; no regex JSON extraction found. |
| One consistent architecture flow | partial | MVVM/use cases exist; JSON local store and Room scaffolding are split. |
| Business logic only in UseCases/domain/repositories | partial | Broadly followed; large repository and some UI-local logic remain. |
| UI reads state from ViewModels | partial | Most screens do; active workout/workout flows expose multiple streams. |
| One `uiState: StateFlow<T>` per screen | partial | Most comply; Workout/active workout need consolidation. |
| Sealed Loading/Success/Error UI state | partial | Most comply; active workout/camera have expanded or non-sealed models. |
| Hilt DI | done | Hilt app/activity/viewmodels/repository bindings present. |
| Repositories are `@Singleton` | done | Main repository and services are singleton-scoped. |
| ViewModel-dependent objects use `@ViewModelScoped` | missing | No usage found. |
| Type-safe navigation via `kotlinx.serialization` | done | `@Serializable` routes and `composable<T>` are used. |
| No string-based navigation routes | done | No string route model found in main navigation. |
| No database mapping in UI layer | partial | Mapping layer exists; full audit pending. |
| Room primary source of truth | missing | Runtime app data still uses `filesDir/trainiq-state.json`. |
| DataStore only for preferences/settings/sync metadata | partial | DataStore use is appropriate, but app data is JSON local store. |
| Room migrations/no destructive changes | partial | Migrations exist; database builder/migration wiring not found. |
| Health Connect SDK status check | done | `HealthConnectClient.getSdkStatus()` used. |
| Provider missing handling | done | `PROVIDER_MISSING` state and install intents exist. |
| Permission rationale before system prompt | done | Rationale/onboarding activity and permission manager UI exist. |
| ChangesToken incremental sync | done | Token persisted; `getChanges` and expiry fallback implemented. |
| Only changed Health Connect data fetched after token | done | Incremental path applies changes. |
| Background-safe Health Connect sync | missing | No WorkManager/background schedule found. |
| Metrics: steps, heart rate, sleep, active calories, weight, workouts | done | Permissions and datasource support all listed metrics. |
| Gemini 2.5 Flash default | done | Model constant uses `gemini-2.5-flash`. |
| Fast mode thinking disabled | done | Meal scan uses `thinkingBudget = 0`. |
| Deep mode thinking budget 500-1000 | done | Deep calls use `thinkingBudget = 1000`. |
| Senior Strength Coach persona | done | Prompt layer includes coach persona behavior. |
| `responseMimeType = application/json` | done | Generation config sets JSON MIME. |
| No regex AI JSON parsing | done | Direct JSON parser used. |
| MaterialTheme colorScheme/typography | partial | Theme primitives used broadly; raw components remain. |
| Dynamic Color Android 12+ | done | Added `TrainIqTheme(dynamicColor = true)` with SDK gate and test. |
| No legacy Material 2 components | partial | Material3 Compose used; legacy Material dependency remains. |
| Shimmer loading states | partial | Shimmer helper exists; some spinners/plain loading remain. |
| AnimatedContent subtle animation | partial | Helper exists; not universal. |
| Haptic feedback important actions | partial | Nav/workout/home actions use haptics; audit pending. |
| Adaptive layouts via WindowSizeClass | missing | No `WindowSizeClass` usage found. |
| Baseline Profiles/startup optimization | partial | Performance monitor/profileable build exists; baseline profile validation pending. |
| No heavy work on Main Thread | partial | Coroutines used; full audit pending. |
| Avoid unnecessary recompositions/stable state | partial | Some state patterns exist; needs UI audit. |
| Required mapper tests | done | Mapper tests present. |
| Required UseCase tests | missing | No clear UseCase-specific tests found. |
| Repository logic tests | partial | Repository tests present; coverage incomplete. |
| Health sync tests | missing | No Health Connect datasource tests found. |
| AI parsing tests | done | AI service parsing tests present. |
| Navigation route tests | done | Navigation tests present. |

## Completed Improvements

- Added Android 12+ Dynamic Color support in `TrainIqTheme`.
- Added unit test coverage for the dynamic-color SDK/request gate.
- Masked the Gemini API-key entry field while typing and switched it to password keyboard behavior.
- Hardened live Health Connect step refresh so it only requires the steps permission, not every Health Connect metric permission.
- Added focused unit tests for Settings key-entry masking and Health Connect permission policy.
- Added non-destructive encrypted Gemini key storage through Android Keystore AES-GCM with legacy DataStore fallback preserved.
- Excluded the encrypted Gemini preference file from backup and device-transfer rules.
- Added per-metric Health Connect status modeling and partial-permission handling that preserves cached metrics instead of clearing all sync state.
- Added `WindowSizeClass`-driven adaptive navigation shell: compact uses the existing bottom bar; medium/expanded use a navigation rail.
- Added staged Room source-of-truth plan below; runtime switchover remains blocked until schema parity and import/rollback tests are in place.
- Added fixture-based JSON-to-Room import planning tests without switching runtime reads away from `trainiq-state.json`.
- Added isolated `JsonRoomImportPlanner`/`JsonRoomImportCoordinator` helpers for test-backed import planning, parity-gap reporting, and fallback outcomes.
- Added Room schema version 10 tables for food items, recipes, recipe ingredients, itemized meal items, active workout session/draft/collapsed/set state, workout log events, and workout log event set snapshots.
- Added DAO import/count operations and a real `RoomJsonImportSink` that imports fixture plans inside `RoomDatabase.withTransaction`.
- Added connected in-memory Room import tests proving representative fixture import, transaction rollback on forced failure, JSON fallback untrusted state, fixture immutability, and stable repeated imports.
- Added Room schema export setup with generated `TrainIqDatabase` schema files for versions 9 and 10.
- Added `room-testing` migration-test infrastructure and a connected `MigrationTestHelper` test proving version 9 data survives migration to version 10 and new version 10 tables are usable.
- Added production `TrainIqDatabase`/DAO providers that register all explicit migrations and do not use destructive migration; repositories still read/write JSON runtime state.
- Added a non-authoritative Room import dry-run hook after JSON load in `TrainIqLocalStore`.
- Added `RoomImportDryRun` status modeling: not attempted, success, failed, skipped missing JSON, skipped invalid JSON, and skipped schema blocker.
- Added dry-run tests proving success keeps JSON authoritative, failure is non-fatal, missing/invalid JSON skips import, repeated attempts are stable, and real in-memory Room rollback works.
- Added Room schema version 11 mirror import-run metadata so dry-run rows are tied to a non-authoritative generation, source fingerprint, schema version, row counts, stale-row count, mismatch count, and JSON/Room authority flags.
- Added transactional dry-run mirror reconciliation: successful dry runs clear stale mirror rows and repopulate current JSON-shaped rows inside one Room transaction; failed imports roll back and keep the previous mirror and JSON fallback safe.
- Added stale/delete and mismatch coverage to connected Room dry-run tests, including repeated generation metadata, removed-record handling, failed-import rollback, and `roomAuthoritative = false`.
- Added Room 10 to 11 migration wiring and a connected migration test proving existing version 10 mirror rows survive upgrade and the version 11 metadata table is usable.
- Added a fail-closed `RoomRuntimeReadinessGate` for future runtime Room reads. It only reports ready when the latest mirror generation is `SUCCESS`, the source fingerprint matches the current authoritative JSON, mismatch count is zero, the mirror is not stale, metadata exists, authority flags remain JSON-only, and migration-chain readiness is explicitly true.
- Added structured readiness failure reasons for missing JSON, missing mirror generation, failed latest generation, import in progress, fingerprint mismatch, mirror mismatch, stale mirror, unverified migration chain, and unknown state.
- Shared non-secret SHA-256 JSON fingerprinting between the dry-run importer and readiness gate.
- Added older Room migration-chain coverage for versions 2 through 8 by reconstructing start schemas from committed database history for versions 2, 3, 4, and 8, and from explicit migration SQL for intermediate versions 5, 6, and 7.
- Added a connected migration-chain test that creates representative legacy data at each start version 2 through 8, migrates to the current Room schema through `TrainIqMigrations.All`, verifies legacy data survived, and verifies current v10/v11 tables are usable.
- Hardened `Migration3To4` and `Migration4To5` with a conditional non-destructive `repsInReserve` add for legacy databases that reached v4 through an older incomplete migration path.
- Added explicit readiness-gate migration-chain statuses for verified, unverified, partial, missing, stale, failed, and unknown states; only `VERIFIED` can pass.

## Changed Files

- `TrainIQ-Project/app/src/main/java/com/trainiq/core/theme/Theme.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/domain/model/DomainModels.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiUsageGate.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/security/GeminiKeyMigration.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/security/AndroidKeystoreGeminiKeyStore.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/di/AppModule.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/MainActivity.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/navigation/TrainIqNav.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/migration/JsonRoomImportPlanner.kt`
- `TrainIQ-Project/app/src/main/res/xml/backup_rules.xml`
- `TrainIQ-Project/app/src/main/res/xml/data_extraction_rules.xml`
- `TrainIQ-Project/app/build.gradle.kts`
- `TrainIQ-Project/gradle/libs.versions.toml`
- `TrainIQ-Project/app/src/test/java/com/trainiq/data/datasource/HealthConnectPermissionPolicyTest.kt`
- `TrainIQ-Project/app/src/test/java/com/trainiq/core/security/GeminiKeyMigrationTest.kt`
- `TrainIQ-Project/app/src/test/java/com/trainiq/navigation/AdaptiveNavigationPolicyTest.kt`
- `TrainIQ-Project/app/src/test/java/com/trainiq/data/migration/JsonRoomImportPlannerTest.kt`
- `TrainIQ-Project/app/src/test/resources/room-import/valid-representative-trainiq-state.json`
- `TrainIQ-Project/app/src/test/resources/room-import/minimal-valid-trainiq-state.json`
- `TrainIQ-Project/app/src/test/resources/room-import/missing-optional-fields-trainiq-state.json`
- `TrainIQ-Project/app/src/test/resources/room-import/malformed-trainiq-state.json`
- `TrainIQ-Project/app/src/test/java/com/trainiq/features/settings/SettingsUiStateTest.kt`
- `TrainIQ-Project/app/src/test/java/com/trainiq/core/theme/ThemeDynamicColorTest.kt`
- `.codex/automation-state/trainiq-target-polish.md`
- `.codex/project-state.md`

## Validation Results

- Passed: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.core.theme.ThemeDynamicColorTest --console=plain`
- Passed: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.features.settings.SettingsUiStateTest --console=plain`
- Passed: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.datasource.HealthConnectPermissionPolicyTest --console=plain`
- Passed: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.core.security.GeminiKeyMigrationTest --console=plain`
- Passed: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.navigation.AdaptiveNavigationPolicyTest --console=plain`
- Passed: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.JsonRoomImportPlannerTest --console=plain`
- Passed Room schema/import targeted JVM test: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.JsonRoomImportPlannerTest --console=plain`
- Passed connected in-memory Room import/rollback test: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.migration.RoomJsonImportSinkTest" --console=plain`
- Passed after Room schema/import pass: `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Passed after Room schema/import pass: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed Room 9 to 10 migration test: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest" --console=plain`
- Passed Room import regression after migration wiring: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.migration.RoomJsonImportSinkTest" --console=plain`
- Passed migration pass targeted planner test: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.JsonRoomImportPlannerTest --console=plain`
- Passed migration pass build/lint: `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Passed migration pass full unit suite: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed Room dry-run unit test after RED/green cycle: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomImportDryRunTest --console=plain`
- Passed real Room dry-run connected test: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.migration.RoomImportDryRunInstrumentedTest" --console=plain`
- Passed dry-run pass build/lint: `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Passed dry-run pass full unit suite: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed Room reconciliation planner unit test: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.JsonRoomImportPlannerTest --console=plain`
- Passed Room reconciliation dry-run unit test: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomImportDryRunTest --console=plain`
- Passed Room reconciliation connected test: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.migration.RoomImportDryRunInstrumentedTest" --console=plain`
- Passed Room 9/10/11 migration connected test: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest" --console=plain`
- Passed Room reconciliation full unit suite: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed Room reconciliation build/lint: `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Passed Room readiness gate RED/green targeted unit test: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomRuntimeReadinessGateTest --console=plain`
- Passed Room readiness adjacent import tests: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomImportDryRunTest --tests com.trainiq.data.migration.JsonRoomImportPlannerTest --console=plain`
- Passed Room readiness full unit suite: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed Room readiness build/lint: `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Passed older Room migration-chain connected test after RED/debug cycle: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest" --console=plain`
- Passed readiness gate migration-chain status unit test: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomRuntimeReadinessGateTest --console=plain`
- Passed older migration-chain pass full unit suite: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed older migration-chain pass build/lint: `.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain`
- Passed after JSON import pass: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed after JSON import pass: `.\gradlew.bat :app:assembleDebug --console=plain`
- Passed after JSON import pass: `.\gradlew.bat :app:lintDebug --console=plain`
- Passed after final patches: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed after final patches: `.\gradlew.bat :app:lintDebug --console=plain`
- Passed after final patches: `.\gradlew.bat :app:assembleDebug --console=plain`
- Passed safe incremental deploy after final patches: `.\gradlew.bat :app:installDebug --console=plain`
- Passed safe launch check after final patches: `adb shell am start -W -n com.trainiq/.MainActivity`
- Passed: `.\gradlew.bat :app:assembleDebug --console=plain`
- Passed: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed safe incremental deploy: `.\gradlew.bat :app:installDebug --console=plain`
- Passed safe launch check: `adb shell am start -n com.trainiq/.MainActivity`
- Android QA read-only evidence: emulator `emulator-5554` has installed `com.trainiq`, launch resolves to `.MainActivity`, UI tree shows Start dashboard and bottom nav, no sampled TrainIQ crash.
- Post-install QA evidence: focus is `com.trainiq/com.trainiq.MainActivity`; UI tree shows `TrainIQ`, `Start`, `Training`, `Voeding`, `Trend`, `Coach`, `Meer`; sampled app log has no `FATAL EXCEPTION` or `AndroidRuntime` crash.

## Failed Checks

- Initial RED test failed as expected before implementation: unresolved `shouldUseDynamicColor`.
- Initial `lintDebug` after dynamic color failed on API-31 dynamic color calls; fixed with explicit API-guarded dynamic color helper and reran lint successfully.
- Settings RED test failed as expected before implementation: unresolved `shouldMaskGeminiApiKeyInput`.
- Health Connect RED test failed as expected before implementation: unresolved `hasHealthConnectPermission`.
- Gemini key migration RED test failed as expected before implementation: unresolved migration/store types.
- Health Connect per-metric RED test failed as expected before implementation: unresolved metric status model/helper.
- Adaptive navigation RED test initially failed as expected before implementation; a follow-up Gradle alias issue was fixed by renaming the version-catalog alias to avoid reserved `class`.
- JSON-to-Room import RED test failed as expected before implementation: unresolved import planner/coordinator/parity types.
- First JSON-to-Room green run exposed Gson/Kotlin default handling for missing non-null fields; fixed by normalizing profile sex at the import boundary.
- First connected Room import test failed because instrumentation assets were not packaged; fixed by adding `androidTest` assets from the existing safe test fixtures.
- First connected Room import test then exposed Gson/Kotlin defaults for missing non-null Room text fields; fixed with import-boundary normalization before Room inserts.
- Initial Room migration RED test failed because `MigrationTestHelper`/`room-testing` was missing; fixed by adding Room testing dependency, schema export, and migration test assets.
- Initial Room dry-run RED test failed because `RoomImportDryRun`/status types did not exist; implemented them and reran successfully.
- Initial Room reconciliation RED test failed as expected because dry-run status, DAO, and metadata APIs did not expose generation id, mismatch count, stale-row count, or latest mirror import run metadata.
- Initial Room readiness gate RED test failed as expected because `RoomRuntimeReadinessGate`, `RoomRuntimeReadiness`, `RoomRuntimeReadinessFailure`, and shared `RoomJsonFingerprint` did not exist.
- Initial older migration-chain test failed because migrated v2/v3 databases were missing `workout_sets.repsInReserve`; root cause was historical v4 entities requiring the column while the older 3 to 4 migration did not add it. Fixed with conditional non-destructive column-add logic.
- A second older migration-chain test run failed because reconstructed v4-v8 test schemas omitted `repsInReserve` even though committed v4/v8 entity history had it. Fixed the test reconstruction to match historical entity evidence.
- A parallel Gradle validation attempt hit a Windows/Kotlin build-intermediate lock; root cause was concurrent Gradle work on the same debug outputs. Stopped the Gradle daemon and reran validation serially successfully.
- Parallel Gradle/KSP validation triggered incremental KSP cache corruption; fixed by stopping the Gradle daemon and deleting only generated project build artifacts under `app/build/kspCaches` and `app/build/generated/ksp`, then reran validation serially.
- `git diff --check` still reports a pre-existing blank line at EOF in `TrainIQ_Target_State_Blueprint.md`; the blueprint remains untouched by this pass.
- `rg --files` failed locally with access denied; PowerShell enumeration used instead.
- First post-launch log command failed because PowerShell `$PID` is read-only; reran with `$procId` successfully.
- First launch focus check after incremental install returned launcher/null focus; reran `am start -W`, waited, and verified `com.trainiq/.MainActivity`.

## Blockers

- Room migration to source of truth is still high risk because runtime state still lives in `filesDir/trainiq-state.json`; production migration wiring, 9 to 10 tests, and non-authoritative dry-run import now exist, but runtime cutover and live-data QA are still pending.
- Gemini live-flow validation depends on existing installed key; key must not be exposed or cleared.
- Health Connect full validation needs provider availability, permissions, and seeded data.

## Staged Room Source-of-Truth Plan

Status: blocked for runtime switchover; schema/import foundation is now test-backed.

1. Schema parity: version 10 now adds Room entities/DAOs for foods, recipes, recipe ingredients, meal items, active workout state, and workout log events.
2. Import path: `RoomJsonImportSink` now populates Room from fixture import plans inside a transaction without deleting or mutating JSON input.
3. Verification tests: JVM fixture tests cover representative, minimal, missing optional fields, malformed JSON, failed import fallback, and stable repeated import plans.
4. Rollback/fallback tests: connected in-memory Room tests prove forced transaction failure leaves inserted counts at zero and keeps JSON fallback untrusted/available.
5. Migration tests: version 9 to 10 is now covered with `MigrationTestHelper`; earlier historical migration chains still need coverage before full Room reliance.
6. Dry run: after JSON load, `TrainIqLocalStore` launches a best-effort Room import from the loaded/migrated state snapshot and exposes status without making Room authoritative.
7. Reconciliation: version 11 stores import generation metadata and the dry-run sink replaces stale mirror rows only inside a successful transaction; failed imports preserve the prior mirror and do not mark Room authoritative.
8. Readiness gate: `RoomRuntimeReadinessGate` now refuses future Room runtime reads unless the current JSON fingerprint matches the latest successful mirror generation, mismatch count is zero, the mirror is fresh, and migration-chain readiness is explicitly `VERIFIED`.
9. Older migration chain: connected tests now cover start versions 2 through 8 to current schema. Versions 5 through 7 are reconstructed from migration SQL because no committed schema export exists.
10. Runtime cutover: still deferred; no production runtime reads were switched to Room and JSON fallback remains intact.

Current schema parity gaps:

- Production runtime cutover is not ready because repositories still read/write JSON runtime state.
- Non-authoritative Room population now has generation metadata, stale-row deletion, and mismatch reporting, but still shares the same tables that a future authoritative Room runtime would use; runtime cutover needs a final mirror-vs-authoritative separation decision.
- Version 9 to 10 migration is now tested against representative existing rows and new v10 table usability.
- Version 10 to 11 migration is now tested against representative version 10 mirror rows and metadata table usability.
- Older migration-chain tests now pass for versions 2 through 8, but live-shape import validation still blocks any runtime cutover.
- Readiness gate exists and is tested; migration-chain status must still be supplied by future cutover wiring and should remain non-verified outside these tests until live-shape validation passes.
- Production database builder/migration registration now exists without switching repositories to Room.
- Room tables intentionally avoid strict foreign keys for legacy-import tolerance; integrity hardening can happen after import parity is validated against live data shapes.

## Remaining Risks

- Runtime persistence remains JSON-backed despite broader Room schema/import/reconciliation/readiness scaffolding; source-of-truth migration is staged but not cut over.
- Gemini key now migrates to encrypted app-private storage, but the legacy DataStore key is intentionally retained as a non-destructive fallback until a later cleanup stage can prove safe.
- Adaptive navigation exists at the app shell and Home dashboard now adapts its grid columns; individual Training, Nutrition, Progress, Coach, and Settings list-detail/tablet layouts and screenshot validation still need separate passes.
- Health Connect partial-permission degradation now has per-metric status modeling and full-sync read isolation; incremental sync global-token failure behavior and background-safe scheduling/policy remain incomplete.
- Full compile, full unit suite, lint, and incremental install/flow QA still need to run after each patch.

## 2026-05-07 Continuation

- Added live-shape Room import validation using the non-secret `live-shape-current-trainiq-state.json` fixture.
- Added readiness-gate live-shape verification states. Runtime Room reads still fail closed unless migration chain and live-shape import validation are both verified.
- Added Health Connect per-metric sync-status tests and full-sync metric read isolation so one failed metric can report `FAILED` without marking unrelated metrics failed.
- Added Home dashboard WindowSizeClass adaptation: compact uses 2 columns, medium 3, expanded 4; app shell navigation rail behavior remains for medium/expanded.
- Added a fail-closed `RoomImportDryRunStatus.toReadinessVerification(...)` mapper so non-runtime dry-run results can feed readiness checks without enabling Room runtime reads.
- Passed targeted and broad validation:
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
- Safe Android QA evidence: emulator `emulator-5554`, package `com.trainiq`, incremental debug install, Start dashboard visible, sampled logcat has no TrainIQ fatal crash.
- Safety: no uninstall, no app-data clear, no emulator reset, no Gemini key exposure, no Room runtime read cutover, and no repo `trainiq-state.json` mutation.

## Next Safest Action

Historical next actions below were superseded by later app-ready work and should not be used as the current release plan.

Current next safest action: stop until at least one required owner/manual/safe-device input is available. Continue only with the matching evidence path: Health Connect edge-state matrix on a disposable profile, real camera/barcode capture on an approved test setup, manual TalkBack/Switch Access signoff, performance owner certification, release owner decisions, or Gemini-enabled debrief with approved credentials/network use.

Superseded 2026-05-08 actions:

1. Add a non-authoritative migration/import verification status provider into the Room readiness gate while keeping runtime Room reads disabled.
2. Continue Health Connect hardening with per-metric isolation for incremental sync token/change failures and explicit background-safe scheduling policy.
3. Add deeper screen-level adaptive layouts beyond the Home dashboard, especially Training list/detail and Nutrition editor panes.
4. Run medium/expanded visual QA with screenshots once emulator display resizing is available.

## 2026-05-08 Implementation Continuation

- Stopped new Gemini API key saves from rewriting the legacy plaintext DataStore `gemini_api_key`; Settings now writes only through `AiUsageGate`/encrypted key storage while legacy DataStore read fallback and non-destructive clear remain.
- Replaced Health Connect active-calorie support from `TotalCaloriesBurnedRecord` / `READ_TOTAL_CALORIES_BURNED` to `ActiveCaloriesBurnedRecord` / `READ_ACTIVE_CALORIES_BURNED` across manifest, datasource, mapper, and permission helper.
- Updated Health Connect rationale copy from five to six signals and added explicit workouts rationale; active calorie copy now distinguishes active expenditure from total expenditure.
- Added incremental Health Connect failure fallback that preserves the existing cache and changes token while marking metric sync statuses failed instead of collapsing the whole Health Connect status to top-level error.
- Added a conservative WorkManager background sync path with unique periodic work, battery-not-low constraint, and exponential backoff. It refreshes through the existing Health Connect datasource without clearing data or requiring network.
- Wired production Room preflight into `TrainIqLocalStore.roomRuntimeReadiness`; runtime Room reads remain disabled/fail-closed because migration-chain verification is still not marked `VERIFIED` in production.
- Improved Settings draft durability with `rememberSaveable` for API key entry, profile form fields, sex/activity selection, goal, and destructive confirmation state.
- Wrapped the barcode scanner saved-state handoff behind typed helper functions and a single internal key constant.

Validation:

- Passed focused Gemini/settings tests: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.core.security.GeminiKeyMigrationTest --tests com.trainiq.features.settings.SettingsUiStateTest --console=plain`
- Passed active-calorie/permission tests: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.core.health.HealthConnectReadPermissionsTest --tests com.trainiq.data.datasource.HealthConnectPermissionPolicyTest --console=plain`
- Passed Health Connect background/failure policy tests: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.core.health.HealthConnectBackgroundSyncPolicyTest --tests com.trainiq.data.datasource.HealthConnectPermissionPolicyTest --console=plain`
- Passed Room readiness/import tests: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomRuntimeReadinessGateTest --tests com.trainiq.data.migration.RoomImportDryRunTest --console=plain`
- Passed navigation/settings focused tests: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.navigation.AdaptiveNavigationPolicyTest --tests com.trainiq.features.settings.SettingsUiStateTest --console=plain`
- Passed full unit suite: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed compile/package: `.\gradlew.bat :app:assembleDebug --console=plain`
- Passed lint: `.\gradlew.bat :app:lintDebug --console=plain`
- Android connected QA was not run because `adb` is not available on PATH in this environment.

Safety:

- No uninstall, app-data clear, emulator reset, destructive Room migration, applicationId/signing change, or Gemini key exposure.
- Runtime reads were not switched to Room; JSON remains authoritative while Room readiness reports fail-closed production status.

Remaining deferred target-state work:

- Full Room source-of-truth cutover remains deferred until production migration-chain verification can be set from a non-destructive validated signal and a device smoke pass is available.
- Health Connect still needs deeper fake-client tests around SDK unavailable/outdated states, pagination, revoked permissions, and token expiry; the app now has a background scheduler and safer cached incremental failure handling.
- Training/Nutrition/Progress/Coach/Settings still need richer screen-specific medium/expanded layouts and screenshot QA; Settings draft state was hardened in this pass.
- Workout one-`uiState` consolidation and repository/use-case extraction remain larger P2 refactors that should be done one bounded context at a time with tests.

## 2026-05-08 Room Verification Provider Continuation

- Added a production `RoomMigrationChainVerificationProvider` with structured, non-secret metadata: current Room version, required migration range, covered migration range, status, reason, marker id, verification timestamp, and freshness.
- Added `RoomMigrationChainVerificationMarkerSource` plus the production `NoTrustedRoomMigrationChainVerificationMarkerSource`; production therefore reports `NOT_RUN` by default and remains fail-closed.
- Extended `RoomMigrationChainVerification` and `RoomRuntimeReadinessFailure` with `NOT_RUN` / `MIGRATION_CHAIN_NOT_RUN`.
- Wired `TrainIqLocalStore` to feed the provider status into `RoomImportDryRunStatus.toReadinessVerification(...)` before evaluating `RoomRuntimeReadinessGate`.
- Runtime reads remain JSON-backed and Room remains non-authoritative unless a future trusted marker source proves the migration chain as `VERIFIED` and all other gates pass.
- Added tests proving the provider defaults to `NOT_RUN`, stale/partial markers do not verify, a valid marker only verifies the migration-chain portion, null provider reports map to `MISSING`, and the readiness gate blocks non-verified statuses including `NOT_RUN`.
- Resolved the previous adb blocker by using the local SDK adb executable directly: `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Ran safe connected Android QA without uninstalling, clearing data, resetting the emulator, exposing keys, or changing app identity.

Validation:

- Passed provider/gate tests: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomMigrationChainVerificationProviderTest --tests com.trainiq.data.migration.RoomRuntimeReadinessGateTest --console=plain`
- Passed full unit suite: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed compile/package: `.\gradlew.bat :app:assembleDebug --console=plain`
- Passed lint: `.\gradlew.bat :app:lintDebug --console=plain`
- Passed safe update install: `.\gradlew.bat :app:installDebug --console=plain`
- Passed launch smoke: `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -W -n com.trainiq/.MainActivity`
- Passed focus/UI smoke: focus is `com.trainiq/com.trainiq.MainActivity`; UI tree shows `TrainIQ`, `Start`, `Training`, `Voeding`, `Trend`, `Coach`, and `Meer`.
- Passed crash check: `adb logcat -d -b crash` filtered for TrainIQ/AndroidRuntime returned no fatal TrainIQ crash entries.
- Passed connected Room migration-chain tests: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest" --console=plain`
- Passed connected Room import tests: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.migration.RoomJsonImportSinkTest" --console=plain`

Current blocker status:

- adb: resolved through full local SDK path; not added to PATH.
- Room runtime cutover: still intentionally blocked. Production migration-chain provider exists, but its default status is `NOT_RUN` because no trusted verification marker source exists. JSON remains authoritative.
- Room safety: no destructive migration, no Room runtime read switch, no JSON fallback removal.
- Remaining target-state work: a future non-destructive trusted marker generation path is required before any Room cutover; broad P2 architecture/workout/adaptive refactors remain separate bounded implementation passes.

## 2026-05-08 End-to-End Fix Continuation

- Fixed Gemini configured-state persistence after encrypted-only saves by resolving Settings, Nutrition, and Camera Scanner AI preferences through `AiUsageGate.resolveSettings(...)`; the UI no longer depends on the legacy plaintext DataStore key to know a key is configured.
- Made `AndroidKeystoreGeminiKeyStore.writeKey(...)` use synchronous `SharedPreferences.commit()` so a save result reflects durable encrypted preference persistence instead of an async write enqueue.
- Kept blank/invalid Gemini key behavior fail-safe: invalid saves surface a visible error and do not overwrite the existing encrypted key; encrypted save failure preserves the current key.
- Replaced the fail-closed placeholder Room marker source with `AssetRoomMigrationChainVerificationMarkerSource`, which reads a generated asset marker and filters it to the current `BuildConfig.BUILD_TYPE`.
- Added `generateDebugRoomMigrationChainVerificationMarker`; it writes the debug marker only after `connectedDebugAndroidTest` passes. The marker contains only non-secret metadata and a SHA-256 payload hash.
- Hardened `RoomMigrationChainVerificationProvider` so missing, invalid-hash, wrong-variant, stale, partial, future-dated, or absent markers fail closed. Only a fresh, hashed marker matching the current Room version/range can report `VERIFIED`.
- Runtime Room reads remain disabled/JSON-authoritative. Even with a verified marker, `RoomRuntimeReadiness.Ready` still reports `roomAuthoritative = false`; no repository read path was switched to Room in this pass.
- Safe Android QA was run through `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe`; no uninstall, data clear, emulator reset, Gemini key exposure, or destructive migration occurred.

Validation:

- Passed focused Gemini/settings/Room tests: `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.core.security.GeminiKeyMigrationTest --tests com.trainiq.features.settings.SettingsUiStateTest --tests com.trainiq.data.migration.RoomMigrationChainVerificationProviderTest --tests com.trainiq.data.migration.RoomRuntimeReadinessGateTest --console=plain`
- Passed full debug unit suite: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- Passed connected Room migration/import/dry-run set: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest,com.trainiq.data.migration.RoomJsonImportSinkTest,com.trainiq.data.migration.RoomImportDryRunInstrumentedTest" --console=plain`
- Passed marker generation path: `.\gradlew.bat :app:generateDebugRoomMigrationChainVerificationMarker --console=plain`
- Passed package build with generated marker asset: `.\gradlew.bat :app:assembleDebug --console=plain`
- Passed lint: `.\gradlew.bat :app:lintDebug --console=plain`
- Passed safe update install: `.\gradlew.bat :app:installDebug --console=plain`
- Passed launch/focus smoke: `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -W -n com.trainiq/.MainActivity`; focus was `com.trainiq/com.trainiq.MainActivity`.
- Passed UI smoke: UI tree showed `TrainIQ`, `Start`, `Training`, `Voeding`, `Trend`, `Coach`, and `Meer`.
- Passed crash/error log check: recent filtered logcat showed no `FATAL EXCEPTION`, `AndroidRuntime`, Room/SQLite app crash, or TrainIQ fatal entry.

Current blocker status:

- adb: resolved through the local SDK path; still not added to PATH.
- Gemini key save: fixed in code and covered by unit tests; live manual save with a fake key was not performed because it could overwrite a real installed user key.
- Room marker: trusted debug generation path exists and is enabled for debug builds after connected instrumentation passes. Release/prod remains fail-closed unless an equivalent controlled marker generation path is run for that variant.
- Room source of truth: runtime Room read switch is still intentionally off; JSON fallback remains intact and authoritative.
