# TrainIQ Target State Status

Generated: 2026-05-08

## Final Status

Status: blocked

The current tree builds, tests, installs, launches, and passes emulator smoke checks, but the blueprint's Room source-of-truth and strict architecture requirements are not fully satisfied because runtime repository state still flows through `TrainIqLocalStore` JSON state rather than DAO-backed Room flows.

## Blueprint Coverage Matrix

| # | Requirement | Current status | Files/modules involved | Implementation/verification evidence | Status |
|---|---|---|---|---|---|
| 1 | MVVM + Clean Architecture + UDF | Mostly present, with repository-heavy business logic | `domain/usecase/UseCases.kt`, `data/repository/TrainIqRepository.kt`, feature ViewModels | Hilt ViewModels and StateFlows exist; audits found business rules still concentrated in repository | blocked |
| 2 | Business logic exclusively in UseCases | Repository still contains progression, dashboard, workout, nutrition logic | `TrainIqRepository.kt`, `UseCases.kt` | Static audit found use cases are often pass-through wrappers | blocked |
| 3 | UI reads ViewModel state only | Mostly present | `features/*/*Screen.kt` | Screens collect ViewModel flows; no direct DB use in UI found | done |
| 4 | Each screen has one `uiState: StateFlow<T>` | Most screens comply; workout still exposes multiple flows | `WorkoutScreen.kt`, other feature screens | Audit found `WorkoutViewModel` has multiple public StateFlows | blocked |
| 5 | UI-state sealed Loading/Success/Error | Most screens comply; workout/camera have custom shapes | `WorkoutScreen.kt`, `CameraScannerScreen.kt`, `SettingsSection.kt` | Settings has animated sealed state; workout/camera remain exceptions | blocked |
| 6 | Hilt DI and singleton repositories | Present | `core/di/AppModule.kt`, `TrainIqRepository.kt` | Build and Hilt compile passed | done |
| 7 | ViewModel-dependent objects use `@ViewModelScoped` | No scoped collaborators identified/implemented | `app/src/main/java` | Audit found no `@ViewModelScoped` usage | blocked |
| 8 | Type-safe navigation only | Present | `navigation/TrainIqNav.kt` | `@Serializable` routes and `composable<T>` are used; navigation tests pass | done |
| 9 | No business logic or database mapping in composables | Mostly present | `data/mapper/Mappers.kt`, feature screens | Mapping is in data layer; some UI validation remains intentionally UI-adjacent | done |
| 10 | Room primary source of truth | Room exists as mirror/readiness layer, not runtime source | `core/database/*`, `data/local/TrainIqLocalStore.kt`, `TrainIqRepository.kt` | DAO/schema/migrations exist; repository still observes JSON store | blocked |
| 11 | DataStore only for preferences, AI settings, sync metadata/tokens | Mostly present | `core/datastore/UserPreferencesRepository.kt` | Health token/cache metadata in DataStore; audit noted possible domain `streak_count` concern | blocked |
| 12 | Room migrations and schema export | Present with manual migrations | `TrainIqDatabase.kt`, `TrainIqMigrations.kt`, `app/schemas/*` | Instrumentation migration tests passed; no AutoMigration used | done |
| 13 | Health Connect SDK status and provider missing handling | Present | `HealthConnectDataSource.kt` | Unit tests and connected tests passed | done |
| 14 | Health Connect rationale screen before system prompt | Improved to screen-first | `HealthConnectUiHelpers.kt`, `HealthConnectPermissionsRationaleActivity.kt`, `AndroidManifest.xml` | Emulator launched rationale screen; screenshot/UI dump captured | done |
| 15 | Health Connect ChangesToken incremental sync | Present and hardened | `HealthConnectDataSource.kt`, `HealthConnectPermissionPolicyTest.kt` | Added token-failure test and failure payload to avoid pretending sync is complete | done |
| 16 | Health Connect metrics: steps, heart rate, sleep, active calories, weight, workouts | Present | `HealthConnectDataSource.kt`, `HealthConnectUiHelpers.kt`, manifest | Uses `ActiveCaloriesBurnedRecord` and six read permissions; tests passed | done |
| 17 | Health Connect background-safe sync | Present and hardened | `HealthConnectBackgroundSyncWorker.kt`, `HealthConnectBackgroundSyncWorkerTest.kt` | Added retry policy for `HealthConnectState.ERROR`; connected tests passed | done |
| 18 | Gemini 2.5 Flash default | Present | `ai/services/AiSupport.kt`, `AiServices.kt`, `RoutineGeneratorService.kt` | Unit tests validate model/config | done |
| 19 | Gemini structured JSON, `application/json`, no regex JSON extraction | Present | `GeminiDtos.kt`, `AiServices.kt`, `GeminiPrompts.kt` | Unit tests passed | done |
| 20 | Fast mode thinking disabled, deep mode 500-1000 tokens | Present | `AiServices.kt`, `RoutineGeneratorService.kt` | Tests validate 0 and 1000 token configs | done |
| 21 | AI persona Senior Strength Coach | Present in prompts | `GeminiPrompts.kt` | Static audit verified Dutch coach/persona prompts | done |
| 22 | Offline/failed-network AI behavior | Improved | `AiServices.kt`, `AiServicesTest.kt` | Meal image API failures now return explicit local fallback | done |
| 23 | Material 3, theme color/typography, Dynamic Color | Present | `core/theme/*`, feature screens | Lint/build/emulator visual smoke passed | done |
| 24 | Shimmer loading and AnimatedContent | Improved | `core/ui/ScreenChrome.kt`, `SettingsSection.kt` | Settings loading now uses shimmer placeholders and `AnimatedScreenState` | done |
| 25 | Haptics for important actions | Present | `TrainIqNav.kt`, workout screens | Static audit verified haptics in navigation/workout paths | done |
| 26 | Adaptive layouts via WindowSizeClass | Present but concentrated | `MainActivity.kt`, `TrainIqNav.kt`, `HomeScreen.kt` | Window class drives rail/grid; broader feature adaptivity remains limited | blocked |
| 27 | Baseline profiles | Present | `app/src/main/baseline-prof.txt` | Release build produced baseline profile metadata; profile may be shallow | done |
| 28 | No heavy main-thread work / lifecycle sanity | Mostly present | repository, workers, screens | Coroutines/WorkManager used; emulator smoke found no crash/hang | done |
| 29 | Required tests for mappers/use cases/repository/health/AI/navigation | Broad coverage present | `app/src/test`, `app/src/androidTest` | Full unit and connected tests passed | done |
| 30 | Real-user install/open/navigate without blockers | Smoke-passed for core tabs | APKs, emulator artifacts | Debug APK installed; Home, Training, Nutrition, Progress, Coach, Settings, Health rationale smoke-passed | done |
| 31 | Privacy-sensitive backup handling | Improved | `AndroidManifest.xml` | `android:allowBackup` set to `false` | done |
| 32 | Final installable artifact | Produced | `app/build/outputs` | Debug APK, unsigned release APK, release AAB generated | done |

## Files Changed In This Pass

- `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiServices.kt`: return explicit local fallback when meal image Gemini/API call fails.
- `TrainIQ-Project/app/src/test/java/com/trainiq/ai/services/AiServicesTest.kt`: regression coverage for meal scan network fallback.
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`: preserve cached incremental data on Changes API failure; mark full sync as failed when ChangesToken cannot be acquired.
- `TrainIQ-Project/app/src/test/java/com/trainiq/data/datasource/HealthConnectPermissionPolicyTest.kt`: regression coverage for incremental failure and full-sync token failure payloads.
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/health/HealthConnectBackgroundSyncWorker.kt`: retry background work when Health Connect returns transient `ERROR`.
- `TrainIQ-Project/app/src/test/java/com/trainiq/core/health/HealthConnectBackgroundSyncWorkerTest.kt`: retry policy coverage.
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/health/HealthConnectUiHelpers.kt`: route permission entry to the full Health Connect rationale activity.
- `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`: animate Settings screen state and use shimmer loading placeholders.
- `TrainIQ-Project/app/src/main/AndroidManifest.xml`: disable Android backup for health-app privacy posture.

Note: the worktree already contained unrelated modified/untracked files before this pass; they were not reverted.

## Validation Evidence

Commands run from `D:\GitHub\TrainIQ\TrainIQ-Project`:

- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain`: PASS.
- Targeted red/green regression command for new tests: initially failed at compile for missing helpers, then PASS after implementation.
- `.\gradlew.bat :app:lintDebug --console=plain`: PASS.
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease :app:bundleRelease :app:connectedDebugAndroidTest --console=plain`: PASS.
- `.\gradlew.bat :app:installDebug --console=plain`: PASS on `emulator-5554`.

Android emulator QA:

- Device: `emulator-5554`, `Medium_Phone(AVD) - 16`.
- Connected Android tests: 12/12 passed.
- Smoke-tested app launch and bottom-tab navigation: Home, Training, Nutrition, Progress, Coach, Settings.
- Health Connect rationale activity launched and rendered before system permission flow.
- Captured screenshots/UI dumps under `D:\GitHub\TrainIQ\.codex\android-smoke`.
- Crash buffer: `adb logcat -b crash -d` returned empty.
- App process alive after smoke: `pidof com.trainiq` returned a process id.

Artifacts:

- Debug APK: `D:\GitHub\TrainIQ\TrainIQ-Project\app\build\outputs\apk\debug\app-debug.apk`
- Unsigned release APK: `D:\GitHub\TrainIQ\TrainIQ-Project\app\build\outputs\apk\release\app-release-unsigned.apk`
- Release AAB: `D:\GitHub\TrainIQ\TrainIQ-Project\app\build\outputs\bundle\release\app-release.aab`

## Remaining Risks / Blockers

- Runtime persistence is not Room-authoritative. `TrainIqRepository` still consumes and mutates `TrainIqLocalStore` JSON state, while Room is currently used as mirror/import readiness infrastructure.
- Business logic is not exclusively in UseCases; `TrainIqRepository` remains a large business-logic holder.
- Workout and camera flows still do not fully match the strict one-`uiState` sealed Loading/Success/Error contract.
- Adaptive layouts are present at navigation/home level but not uniformly across all feature screens.
- Release artifact is unsigned because no release signing credentials/config were provided. The generated release APK is unsigned; the generated AAB is local build output only.

## Web Research Used

None. All issues were resolved from local code and Gradle/emulator evidence.
