# A2 implementation — FIX-01 / FIX-02

Base inputs were frozen by root after passing local baseline build/unit/lint/Android-test compile, synthetic gateway tests and focused connected proof. No app/test write preceded that signal. A2 owns only the paths below; no Gradle, device, live service, credentials or schema work was performed by this worker.

## Changes

- FIX-01: a narrow preferences-owned `HealthConnectSyncCoordinator` uses one cancellable Mutex for the complete Health Connect status/provider read/cache commit. `getStatus` enters that ownership before checking provider availability or reading preferences. `clearLocalPrivateData` uses the same mutex, so successful reset clears after any already running read/commit. No older active sync can write after that reset. Existing internal metric clears remain inside sync ownership, avoiding nested/reentrant locks. A genuinely new sync remains permitted; deleting local data does not revoke Android permission.
- FIX-02: both meal save overloads reach a transaction-local positive-ID existence guard before parent/child allocation or writes. Recipe save has the same guard. Clear, actionable Dutch exceptions use existing failure handling. Valid edits preserve original meal date when date was not explicitly edited, and original recipe `createdAt`; null/0 create behavior is retained. No claim is made about recycled-ID identity after another row is created.

### A4 correction after independent review

The A2 callsite review was incomplete: it inspected coordinator/runtime callers but missed the production UI's saved positive `mealSaveId` reservation. REVIEW-B-01 proved the original guard would reject a first manual meal. A2 is superseded for the meal command contract by this correction; no passing execution was claimed for that intermediate state.

`MealSaveTarget.Create(reservedId)` and `Edit(id)` now travel explicitly through the UI callback, ViewModel, use case, domain repository and coordinator. The saved reservation still survives retry/recreation; the create path permits it and replaces that same parent/items on retry rather than duplicating the meal. Edit continues to reject a missing parent transactionally. The existing coordinator/runtime compatibility paths retain null/zero=create and positive=edit, so no legacy edit is silently turned into creation. Recipe retains null/zero=create semantics.

Narrow typed `MissingMealEditException` / `MissingRecipeEditException` map to safe create-recovery guidance while retaining the draft and releasing pending state. Unknown storage errors still use safe generic retry messages; arbitrary throwable text is never displayed.

A4 adds one Room reserved-create→retry→reopen test through both save overloads, retains deleted-edit tests, adds two safe-guidance JVM tests and adapts the actual first-add retry fixture to explicit create commands without changing its exactly-once assertions. Existing real manual first-add and barcode-to-meal routes remain central-runner acceptance tests. Root confirmed the failed gate exited before any A4 writes; A4 ran no Gradle/device operations.

Expanded paths: `domain/repository/MealSaveTarget.kt` (new), `Repositories.kt`, `domain/usecase/UseCases.kt`, `data/repository/FocusedRepositories.kt`, `TrainIqRepository.kt`, `features/nutrition/NutritionScreen.kt`, `MealSave.kt`, `MealSaveTest.kt`, `NutritionFirstAddInstrumentedTest.kt`, `BarcodeMealFlowInstrumentedTest.kt`, and the already owned runtime/Room-test paths.

## Regression proof added

- `HealthConnectSyncCoordinatorTest`: five deterministic coroutine tests cover predecessor commit ordering, reset during suspended provider work, canceled waiter, canceled owner, provider/reset failure and subsequent recovery. No sleeps, Android/provider fixture or actual health data.
- `TargetedRoomPersistenceInstrumentedTest`: two actual Room tests prove deleted meal/recipe edits cannot recreate parent or children, including both meal overloads, unchanged survivor nutrients/timestamps, and database reopen. Existing sequential-create/valid-edit tests already prove both happy paths and metadata preservation.

Tests were written before production implementation, but red-before-fix execution was NOT RUN because the central runner exclusively owns Gradle/device work. Root/C must record integrated results before completion claims.

## Exact paths

- `TrainIQ-Project/app/src/main/java/com/trainiq/core/datastore/HealthConnectSyncCoordinator.kt` (new)
- `TrainIQ-Project/app/src/main/java/com/trainiq/core/datastore/UserPreferencesRepository.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt`
- `TrainIQ-Project/app/src/test/java/com/trainiq/core/datastore/HealthConnectSyncCoordinatorTest.kt` (new)
- `TrainIQ-Project/app/src/androidTest/java/com/trainiq/data/repository/TargetedRoomPersistenceInstrumentedTest.kt`
- `docs/qa/2026-09-30/implementation-A.md`

## Verification handoff

- Static diff review / `git diff --check`: PASS.
- Build/unit/lint/connected: NOT RUN by A2; central runner pending.
- Focused JVM class: `com.trainiq.core.datastore.HealthConnectSyncCoordinatorTest` plus existing `com.trainiq.data.datasource` contracts.
- Connected class: `com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest`; retain the existing sequential meal/recipe creation/edit tests alongside the two added deletion regressions.
- Material limits: reset waits an existing provider operation rather than forcibly canceling it; its completion latency inherits the provider's existing response behavior. SDK/provider/device parity is not proved by the pure coordinator tests. No new private-data upload, logging, persistence format, permission, dependency or migration is introduced.
- Future raw Health Connect throwable-message exposure was considered but not expanded into this package without proof of actual sensitive details. No app-wide privacy guarantee is inferred.

Final integration acceptance supersedes the wave2 pending status: explicit MealSaveTarget.Create reservations preserve first-save/retry idempotence, Edit guards reject missing parents for both meal overloads and recipes with safe guidance. Controlled HC/reset/cancellation, transactional no-resurrection/reopen and reserved-create-twice proofs pass in fresh992JVM and full202Android gates. No schema or identity-format migration; recycled-ID limitation remains deferred. Full provenance and attempts are in verification.md.
