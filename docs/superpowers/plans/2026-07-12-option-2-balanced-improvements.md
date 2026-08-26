# TrainIQ Option 2 Balanced Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the selected data-integrity, Health Connect, scanner-performance, lifecycle, export, compact-navigation, and CI gaps without a broad repository rewrite.

**Architecture:** Keep Room authoritative and move identity allocation and export reads behind transactional store boundaries. Preserve existing MVVM/UDF flows; Health Connect remains per-metric and WorkManager-backed, transient Coach input moves into saved ViewModel state, and compact navigation changes without altering typed routes.

**Tech Stack:** Kotlin, Jetpack Compose, Room 2.7, Health Connect 1.1, WorkManager 2.10, Hilt, JUnit/Turbine, Android Macrobenchmark, GitHub Actions.

## Global Constraints

- No Room schema version change unless compilation proves the selected transactional solution cannot be implemented on schema v16.
- No new runtime dependency.
- No global `TrainIqDataCoordinator` or active-workout snapshot rewrite.
- Preserve Gemini/OpenAI provider behavior, image limit `1_500_000` bytes, source limit `6 MiB`, and maximum image dimension `1_280` px.
- Preserve partial Health Connect permissions and never retry permanent denied, unavailable, provider-missing, or invalid-configuration states.
- Compact navigation shows five destinations; medium/expanded navigation rail keeps all six typed destinations.
- Add only focused tests that reproduce a selected risk.
- Do not commit, push, publish, sign, or upload artifacts in this run.

---

### Task 1: Transactional Nutrition Identity and Export Snapshot

**Files:**
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/TrainIqDao.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/domain/usecase/UseCases.kt`
- Test: `TrainIQ-Project/app/src/androidTest/java/com/trainiq/data/repository/TargetedRoomPersistenceInstrumentedTest.kt`
- Test: focused JVM export test beside the existing import/export use-case tests

**Interfaces:**
- `RoomTrainIqRuntimeStore.saveFood(...)` must return the actually persisted `FoodItemStorage` after resolving explicit ID, duplicate barcode, or a fresh ID inside the same mutex/Room transaction.
- `RoomTrainIqRuntimeStore.readExportSnapshot(): TrainIqStorageState` must read one consistent Room snapshot without relying on `state.value`.
- `ExportAppDataUseCase` must serialize `readExportSnapshot()`.

- [ ] Write a failing test that saves two new AI foods sequentially without waiting for Flow invalidation and asserts two IDs and two rows.
- [ ] Run the focused test and confirm the current `max + 1` path fails.
- [ ] Move food identity/barcode resolution and upsert into the transactional store boundary; return the persisted storage object.
- [ ] Run the focused test and existing nutrition persistence tests.
- [ ] Write a failing test that writes a record and immediately exports, then asserts the record exists in JSON.
- [ ] Add the transactionally consistent one-shot snapshot and route export through it.
- [ ] Run focused export/import and targeted Room persistence tests.

### Task 2: Health Connect Token and Background Work Correctness

**Files:**
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/core/health/HealthConnectBackgroundSyncWorker.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`
- Test: `TrainIQ-Project/app/src/test/java/com/trainiq/data/datasource/HealthConnectPermissionPolicyTest.kt`
- Test: `TrainIQ-Project/app/src/test/java/com/trainiq/core/health/HealthConnectBackgroundSyncWorkerTest.kt`

**Interfaces:**
- Full-sync token updates must exclude every metric present in `metricFailures` after reads and token acquisition.
- Worker retry policy must accept the full `HealthConnectStatus` and retry only `ERROR` or at least one `FAILED` metric.
- Settings Health Connect refresh must reconcile the existing unique periodic work once per relevant resume/result, using `HealthConnectBackgroundSyncScheduler`.

- [ ] Add a failing token-policy test for failed metric read plus successful token request.
- [ ] Filter token updates to successful metrics and confirm the next sync stays full for the failed metric.
- [ ] Add failing worker policy tests for `CONNECTED + FAILED`, denied, and unavailable statuses.
- [ ] Update retry policy minimally and run worker tests.
- [ ] Add a focused scheduler reconciliation test or source-independent ViewModel test.
- [ ] Inject the existing scheduler into Settings and reconcile after the status refresh.
- [ ] Run all focused Health Connect tests.

### Task 3: Bounded Off-Main AI Image Preparation

**Files:**
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiServices.kt`
- Test: `TrainIQ-Project/app/src/test/java/com/trainiq/ai/services/AiServicesTest.kt`
- Test: one Android instrumentation test only if JVM bitmap coverage cannot exercise the decoder

**Interfaces:**
- Image preparation becomes a suspending operation and performs file I/O, bounds decode, sampled decode, resize, and JPEG compression on `Dispatchers.IO` or an injected background dispatcher.
- Both meal and scale services keep the same fallback results and upload limits.

- [ ] Add a failing test for a large-dimension JPEG that requires sampled decoding and bounded upload output.
- [ ] Change the preparation API to suspend and decode bounds before pixels.
- [ ] Calculate `inSampleSize`, decode a bounded bitmap, resize to at most `1_280` px, compress at the existing quality, and enforce `1_500_000` bytes.
- [ ] Recycle temporary bitmaps only when safe and preserve fallback behavior for invalid images.
- [ ] Run focused AI service tests.

### Task 4: Saved Coach Draft and Five-Destination Compact Navigation

**Files:**
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/features/coach/CoachScreen.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/navigation/TrainIqNav.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`
- Test: focused Coach draft state test
- Test: `TrainIQ-Project/app/src/test/java/com/trainiq/navigation/AdaptiveNavigationPolicyTest.kt`

**Interfaces:**
- `CoachViewModel` owns a lightweight serializable profile draft plus dirty state in `SavedStateHandle`; persistence hydration is ignored while dirty and Save resets the draft from the persisted result.
- Compact bottom destinations are Home, Train, Nutrition, Coach, Settings/Meer. Progress remains a typed route and is directly reachable from Settings/Meer; navigation rail keeps all six.

- [ ] Add a failing Coach draft test covering edit, profile refresh, and recreation.
- [ ] Hoist draft events/state to `CoachViewModel` with `SavedStateHandle`; remove the composable overwrite path.
- [ ] Run the Coach tests.
- [ ] Change the compact destination policy test to expect five items and explicit Progress discoverability.
- [ ] Implement the compact list and a Settings/Meer Progress action without changing route types.
- [ ] Run navigation, Coach, Settings, and compact UI tests.

### Task 5: Minimal Enforceable CI and Final Verification

**Files:**
- Create: `.github/workflows/android.yml`
- Modify: `TrainIQ-Project/README.md`
- Modify: `docs/TrainIQ_Target_State_Progress.md`
- Modify: `docs/TrainIQ_QA_Findings_To_Improve.md`

**Interfaces:**
- Pull requests run debug unit tests, lint, debug assemble, AndroidTest Kotlin compilation, and macrobenchmark compilation with read-only permissions.
- Room migration marker and signed release remain manual jobs; no automatic push trigger is added.
- Third-party actions are pinned to immutable commit SHAs.

- [ ] Add the PR validation workflow and manual migration/release jobs using the existing Gradle tasks and external signing secrets.
- [ ] Validate workflow syntax and Gradle task names without publishing artifacts.
- [ ] Update README and QA/progress docs to match actual automation and implemented findings.
- [ ] Run focused tests, then `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin :macrobenchmark:compileProfileableJavaWithJavac`.
- [ ] Install/launch on the available emulator, smoke compact navigation and Settings, and scan the crash buffer.
- [ ] Run `git diff --check` and review the complete diff for unrelated changes.

