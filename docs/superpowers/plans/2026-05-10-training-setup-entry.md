# Training Setup Entry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose a clear setup action for empty active routines so users can reach the existing routine detail builder without changing current start or persistence behavior.

**Architecture:** Reuse `WorkoutScreen`'s existing `selectedRoutineId` detail-mode state. `ActiveRoutineCard` gets an `onOpenDetails` callback and shows `Routine inrichten` only when `firstStartableDay()` is null; startable routines keep the existing `Training starten` action.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, existing `WorkoutInputValidationTest` source-level regression tests, Gradle debug unit tests.

---

### Task 1: Add Source-Level Regression Guards

**Files:**
- Modify: `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\test\java\com\trainiq\features\workout\WorkoutInputValidationTest.kt`
- Read: `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\features\workout\WorkoutScreen.kt`

- [ ] **Step 1: Add tests for the setup-entry and preserved existing actions**

Add these tests near the existing active routine tests:

```kotlin
@Test
fun `empty active routine exposes setup entry in active routine card`() {
    val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
    val activeRoutineCard = workoutScreen
        .substringAfter("private fun ActiveRoutineCard(")
        .substringBefore("@Composable\nprivate fun SectionHeader")

    assertTrue(activeRoutineCard.contains("onOpenDetails: (Long) -> Unit"))
    assertTrue(activeRoutineCard.contains("activeRoutineSetupLabel()"))
    assertTrue(activeRoutineCard.contains("onOpenDetails(activeRoutine.id)"))
}

@Test
fun `routine setup label stays Dutch`() {
    assertEquals("Routine inrichten", activeRoutineSetupLabel())
}

@Test
fun `routine overview keeps details action`() {
    val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
    val routineCardOverview = workoutScreen
        .substringAfter("if (!detailMode) {")
        .substringBefore("return\n    }")

    assertTrue(routineCardOverview.contains("onOpenDetails"))
    assertTrue(routineCardOverview.contains("Text(\"Details\")"))
}
```

- [ ] **Step 2: Run the targeted test and verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache
```

Expected: FAIL because `activeRoutineSetupLabel()` and the `ActiveRoutineCard` callback are not implemented yet.

### Task 2: Implement the Active Routine Setup Entry

**Files:**
- Modify: `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\features\workout\WorkoutScreen.kt`

- [ ] **Step 1: Pass a detail callback from `WorkoutScreen` to `ActiveRoutineCard`**

Replace:

```kotlin
item { ActiveRoutineCard(activeRoutine = overview.activeRoutine, onStartWorkout = onStartWorkout) }
```

with:

```kotlin
item {
    ActiveRoutineCard(
        activeRoutine = overview.activeRoutine,
        onStartWorkout = onStartWorkout,
        onOpenDetails = { selectedRoutineId = it },
    )
}
```

- [ ] **Step 2: Extend `ActiveRoutineCard` signature**

Replace:

```kotlin
private fun ActiveRoutineCard(activeRoutine: WorkoutRoutine?, onStartWorkout: (Long) -> Unit) {
```

with:

```kotlin
private fun ActiveRoutineCard(
    activeRoutine: WorkoutRoutine?,
    onStartWorkout: (Long) -> Unit,
    onOpenDetails: (Long) -> Unit,
) {
```

- [ ] **Step 3: Show setup action only for non-startable active routines**

Replace the non-startable branch:

```kotlin
if (startableDay == null) {
    Text(activeRoutineNeedsExerciseText())
} else {
```

with:

```kotlin
if (startableDay == null) {
    Text(activeRoutineNeedsExerciseText())
    PrimaryActionButton(
        onClick = { onOpenDetails(activeRoutine.id) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(activeRoutineSetupLabel())
    }
} else {
```

- [ ] **Step 4: Add the setup label helper**

Near `activeRoutineNeedsExerciseText()`, add:

```kotlin
internal fun activeRoutineSetupLabel(): String = "Routine inrichten"
```

Keep `activeRoutineStartLabel(dayName: String): String = "Training starten"` unchanged.

### Task 3: Verify and Record the Implementation

**Files:**
- Verify: `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\features\workout\WorkoutScreen.kt`
- Verify: `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\test\java\com\trainiq\features\workout\WorkoutInputValidationTest.kt`

- [ ] **Step 1: Run the targeted workout test**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache
```

Expected: PASS.

- [ ] **Step 2: Run the debug build**

Run:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache
```

Expected: PASS.

- [ ] **Step 3: Optional physical-device smoke**

If the SM-S931B device is connected, install and launch:

```powershell
.\gradlew.bat :app:installDebug --console=plain --no-configuration-cache
& 'C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell am start -W -n com.trainiq/.MainActivity
```

Expected: Training still opens. A clean empty active routine should expose `Routine inrichten`; tapping it should open the existing routine detail screen where `Sessies` and `Eerste oefening toevoegen` already exist.

- [ ] **Step 4: Commit only implementation files**

Stage only these files if this task is being committed separately:

```powershell
git add -- TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt
git commit -m "polish(training): expose routine setup entry"
```

Do not stage unrelated cycle-state or QA docs unless that is explicitly part of the current commit.

---

## Self-Review

- Spec coverage: The plan implements the active-routine setup action, routes it through existing detail mode, preserves start behavior, and adds regression tests for existing details/start behavior.
- Placeholder scan: No placeholders or deferred implementation steps.
- Type consistency: `onOpenDetails: (Long) -> Unit`, `selectedRoutineId`, `activeRoutineSetupLabel()`, and `firstStartableDay()` match existing `WorkoutScreen.kt` patterns.
