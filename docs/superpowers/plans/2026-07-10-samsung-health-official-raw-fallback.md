# Samsung Health Official Raw Steps Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Laat TrainIQ in het fysieke bewijsgeval 84 officiële Samsung raw stappen tonen in plaats van de onderrapporterende aggregate 5.

**Architecture:** De bestaande Health Connect-broninspectie blijft verantwoordelijk voor een afgeleide Samsung raw dagsom. Een pure exacte-package-guard voorkomt lookalike-bronnen; de bestaande Samsung-selector kiest vervolgens de hoogste positieve officiële raw/aggregate-waarde. De bestaande domeinselector behoudt de directe Samsung Data SDK als hoogste autoriteit en de algemene Health Connect aggregate als laatste fallback.

**Tech Stack:** Kotlin, AndroidX Health Connect, JUnit 4, Gradle, Hilt/MVVM-bestaande architectuur.

## Global Constraints

- Alleen exact `com.sec.android.app.shealth` (case-insensitive) mag raw fallback-data leveren.
- Verse directe Samsung Data SDK `TOTAL` blijft altijd leidend, inclusief `0`.
- Individuele raw records, IDs en timestamps worden niet nieuw gepersist.
- Het bestaande lokale dagvenster en bestaande cache-/datumwissel-/permissionlogica blijven intact.
- Geen nieuwe dependency, feature flag, commit of push.
- Bewaar alle bestaande niet-gerelateerde worktreewijzigingen.

---

### Task 1: Official Samsung origin and raw/aggregate selector

**Files:**
- Modify: `TrainIQ-Project/app/src/test/java/com/trainiq/data/datasource/HealthConnectPermissionPolicyTest.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`

**Interfaces:**
- Produces: `internal fun String.isOfficialSamsungHealthDataOrigin(): Boolean`
- Produces: `internal fun resolveSamsungHealthVisibleSteps(samsungHealthAggregateSteps: Int?, samsungRawStepRecordSum: Int?): Int?`

- [x] **Step 1: Write the failing screenshot regression and package-guard tests**

```kotlin
@Test
fun officialSamsungRawStepsWinWhenAggregateUnderReports() {
    assertEquals(
        84,
        resolveSamsungHealthVisibleSteps(
            samsungHealthAggregateSteps = 5,
            samsungRawStepRecordSum = 84,
        ),
    )
    assertEquals(84, resolveSamsungHealthVisibleSteps(null, 84))
    assertEquals(84, resolveSamsungHealthVisibleSteps(84, 5))
}

@Test
fun onlyOfficialSamsungHealthPackageCanProvideRawFallback() {
    assertTrue("com.sec.android.app.shealth".isOfficialSamsungHealthDataOrigin())
    assertTrue("COM.SEC.ANDROID.APP.SHEALTH".isOfficialSamsungHealthDataOrigin())
    assertFalse("com.example.samsung.health".isOfficialSamsungHealthDataOrigin())
}
```

- [x] **Step 2: Run the focused test and verify RED**

Run from `TrainIQ-Project`:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest"
```

Expected: failure because raw 84 is still discarded and the exact-package helper does not exist.

- [x] **Step 3: Implement the minimal exact-origin selector**

```kotlin
internal fun String.isOfficialSamsungHealthDataOrigin(): Boolean =
    equals(SamsungHealthDirectStepsDataSource.SamsungHealthPackageName, ignoreCase = true)

internal fun resolveSamsungHealthVisibleSteps(
    samsungHealthAggregateSteps: Int?,
    samsungRawStepRecordSum: Int?,
): Int? = listOfNotNull(
    samsungHealthAggregateSteps?.takeIf { it > 0 },
    samsungRawStepRecordSum?.takeIf { it > 0 },
).maxOrNull()
```

In `readStepSourceSnapshotToday`, voeg raw counts en Samsung package authority alleen toe wanneer `packageName.isOfficialSamsungHealthDataOrigin()` waar is. Laat generieke bronlabels ongemoeid voor diagnose.

- [x] **Step 4: Re-run the focused test and verify GREEN**

Run the same command. Expected: all `HealthConnectPermissionPolicyTest` tests pass.

### Task 2: Explain the selected raw fallback and guard regressions

**Files:**
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/domain/model/DomainModels.kt`
- Modify: `TrainIQ-Project/app/src/test/java/com/trainiq/data/datasource/HealthConnectPermissionPolicyTest.kt`
- Modify: `docs/TrainIQ_QA_Findings_To_Improve.md`
- Modify: `docs/TrainIQ_Target_State_Progress.md`

**Interfaces:**
- Produces: `HealthConnectStepDiagnostic.usesSamsungRawStepFallback: Boolean`
- Consumes: the selected `samsungHealthStepsToday` scalar from Task 1.

- [x] **Step 1: Write a failing diagnostic regression for the physical evidence**

```kotlin
val diagnostic = HealthConnectStepDiagnostic(
    aggregateStepsToday = 5,
    samsungHealthStepsToday = 84,
    samsungHealthAggregateStepsToday = 5,
    samsungRawStepRecordSumToday = 84,
    queriedAt = 123L,
    sourceLabels = listOf("Samsung Health", "Jouw telefoon"),
)
assertEquals(84, diagnostic.displaySteps)
assertTrue(diagnostic.usesSamsungRawStepFallback)
assertTrue(diagnostic.samsungHealthComparisonSummary.contains("raw"))
```

- [x] **Step 2: Run the focused test and verify RED**

Run the Task 1 command. Expected: failure because the diagnostic property/copy does not exist yet.

- [x] **Step 3: Add an explicit derived flag and raw-fallback copy**

The flag is true only when direct/cache routes are not active, raw is positive and higher than the Samsung aggregate, and the displayed/selected Samsung value equals raw. Add explicit raw-fallback branches before generic Samsung-export branches in `aggregateAuthorityLabel`, `samsungHealthComparisonSummary`, `parityGapSummary` and `healthConnectVisibleStepSummary`.

- [x] **Step 4: Update tracked QA/progress documentation**

Record the 2026-07-10 physical evidence `5 aggregate versus 84 official raw/Samsung UI`, the guarded exact-package fallback, and the remaining need for the user's final physical-device smoke. Do not edit the untracked handoff document.

- [x] **Step 5: Run focused and broad verification**

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest"
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin
```

Expected: all tasks complete successfully with no failed tests or lint errors.

- [x] **Step 6: Run emulator/device smoke available in the workspace**

Install/launch the debug APK on an available adb target, trigger refresh, inspect UI/logcat for crashes, and capture the exact APK path for the user's Samsung test. The user's physical Samsung comparison remains the final parity acceptance.
