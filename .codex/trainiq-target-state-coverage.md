# TrainIQ Target-State Coverage

Date: 2026-05-08

Summary: 24 requirements, 24 PASS, 0 FAIL, 0 BLOCKED.

| Requirement | Implementation files | Tests | Android QA evidence | Status | Notes |
|---|---|---|---|---|---|
| MVVM + Clean Architecture + UDF | `UseCases.kt`, feature ViewModels, `FocusedRepositories.kt` | `RepositoryDecompositionArchitectureTest`, use case tests | Launch and top-level route QA | PASS | ViewModels consume use cases/repositories and expose UI state. |
| Business logic in use cases/focused collaborators | `UseCases.kt`, `ActiveWorkoutSessionMutations.kt`, `WorkoutProgressionSuggestionCalculator.kt`, `ExerciseLibrarySeeder.kt` | `BuildHomeDashboardUseCaseTest`, `StartWorkoutSessionUseCaseTest`, `ActiveWorkoutSessionMutationsTest`, `WorkoutProgressionSuggestionCalculatorTest`, `ExerciseLibrarySeederTest` | Active workout restore QA | PASS | Coordinator delegates active-session mutation, progression policy, and exercise library seeding to focused units. |
| One sealed UI state per screen, Workout/Camera single public `uiState` | `ScreenUiState.kt`, `WorkoutScreen.kt`, `CameraScannerScreen.kt` | `ScreenUiStateArchitectureTest`, `WorkoutUiStateReducerTest`, `CameraUiStateMapperTest` | Camera denied/granted QA | PASS | Workout and Camera expose one public `StateFlow<ScreenUiState<...>>`. |
| Hilt DI and singleton repositories | `AppModule.kt`, `FocusedRepositories.kt` | compile, `RepositoryDecompositionArchitectureTest` | App launch QA | PASS | Domain repository bindings use focused implementations. |
| Type-safe navigation, no string routes | `TrainIqNav.kt` | navigation route tests, adaptive route propagation test | Compact and expanded navigation QA | PASS | Serializable routes and typed navigation used. |
| Room runtime source of truth | `RoomTrainIqRuntimeStore.kt`, DAO flows | `RoomAuthorityArchitectureTest`, `RoomAuthoritativeReadParityTest`, connected Room tests | Clear-data relaunch QA | PASS | Runtime state comes from DAO flows; legacy JSON seeds/imports only. |
| Writes through Room transaction path | `RoomTrainIqRuntimeStore.kt`, mutation services | `WorkoutSessionTransactionTest`, `ActiveWorkoutSessionMutationsTest` | Active restore connected test | PASS | Runtime updates flow through transaction import sink. |
| Legacy JSON bridge only | `TrainIqLocalStore.kt`, `RoomTrainIqRuntimeStore.kt` | architecture/static guard tests | Clear-data relaunch QA | PASS | No ViewModel direct dependency on `TrainIqLocalStore`. |
| Settings reset/clear via use cases | `SettingsSection.kt`, `UseCases.kt` | `ClearAppDataUseCaseTest`, `SettingsUiStateTest` | reset dialog/confirm and clear dialog/confirm screenshots | PASS | Reset profile and clear local data verified through UI. |
| Existing data migration/preservation | migration/import classes and Room tests | `TrainIqDatabaseMigrationTest`, `RoomJsonImportSinkTest`, import dry-run tests | connected test suite | PASS | Connected Android tests pass. |
| Health Connect status/provider/rationale/token policy | `HealthConnectDataSource.kt`, Health helpers, worker | health policy/read permission/background tests | Settings Health Connect status visible | PASS | SDK/rationale/cache paths covered by tests and UI. |
| Gemini 2.5 Flash structured JSON | AI services and DTOs | `AiServicesTest`, `RoutineGeneratorServiceTest` | no external API call required | PASS | JSON mime configs and parser tests enforce structured output. |
| Material 3, dynamic color, typography | theme and UI files | `ThemeDynamicColorTest`, lint | compact/expanded screenshots | PASS | Lint passed; UI uses Material 3 theme. |
| Shimmer/animated/haptic UX patterns | shared UI and feature screens | UI state and reducer tests | screenshots and manual interactions | PASS | No blocking spinner/layout issues found in QA. |
| Adaptive layouts all top-level routes | `TrainIqNav.kt`, feature routes | `AdaptiveFeatureRoutePropagationTest`, adaptive navigation tests | `manual-23-expanded-home.png`, `manual-24-expanded-training.png`, `manual-25-expanded-rotated.png` | PASS | Expanded viewport and rotation verified. |
| Performance baseline/startup safeguards | baseline profile, diagnostics | diagnostics tests, lint | launch/logcat QA | PASS | No startup crash, ANR, or app fatal logs observed. |
| Repository decomposition | `FocusedRepositories.kt`, `ActiveWorkoutSessionMutations.kt`, `WorkoutProgressionSuggestionCalculator.kt`, `ExerciseLibrarySeeder.kt` | repository decomposition and behavioral extraction tests | active workout connected restore | PASS | Monolithic interface binding removed; high-risk business/seeding clusters extracted. |
| Use case orchestration | `UseCases.kt` | dashboard/start/clear use case tests | active workflow QA | PASS | Dashboard merge, workout start validation, reset/clear workflows covered. |
| Camera denied path | `CameraScannerScreen.kt` | camera mapper/state tests | `manual-06-camera-permission.png`, `manual-07-permission-dialog.xml`, `manual-08-camera-denied.png` | PASS | Denial leaves user-safe camera-access screen with settings action. |
| Camera granted path | `CameraScannerScreen.kt` | camera mapper/state tests | `manual-09-camera-granted.png/xml` | PASS | Barcode scanner preview route opens with permission granted. |
| Active workout restore | Room active workout tables, route/viewmodel | `ActiveWorkoutRestoreInstrumentedTest`, mutation tests | connected Android test PASS | PASS | Seeded active session restores after activity recreation. |
| Release signing and CI | `app/build.gradle.kts`, `.github/workflows/android-release.yml`, README | `checkReleaseSigningReadiness` | release APK/AAB build outputs | PASS | Secrets/env/Gradle props supported; no hardcoded secrets. |
| No app TODO/TBD/deferred markers | source/docs | grep scan | n/a | PASS | Static scan found no target-state TODO/TBD/FIXME/deferred markers outside generated build outputs. |
| Final validation | Gradle and adb | full unit/lint/connected/release commands | logcat crash buffer empty | PASS | Required commands passed; type-safe navigation keep warning resolved. |
