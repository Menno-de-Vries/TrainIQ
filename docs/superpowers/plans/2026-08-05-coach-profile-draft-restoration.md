# Coach Profile Draft Restoration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve unsaved Coach profile and goal input across Android Activity recreation so rotation or resize cannot erase onboarding work.

**Architecture:** Keep the draft owned by `CoachScreen`, because it is ephemeral UI input and persisted profile data remains ViewModel/Room-owned. Replace the non-saveable Compose state with `rememberSaveable`; strings and the two enum values use Compose's Bundle-compatible automatic saver. Verify the user-visible behavior through the real `MainActivity` and Compose semantics.

**Tech Stack:** Kotlin, Jetpack Compose `rememberSaveable`, Compose UI instrumentation, ActivityScenario, Gradle.

## Global Constraints

- Preserve `MVVM + Clean Architecture + UDF`; do not move draft mapping or persistence into composables beyond ephemeral input ownership.
- Do not add dependencies, permissions, schemas, services, remote calls, or production-release claims.
- Run Android tests only on the agent-owned local `Pixel_8_API_36` emulator.
- Keep Room authoritative for saved profile data; this batch changes only pre-save form restoration.

---

### Task 1: Reproduce profile-draft loss with an instrumentation test

**Files:**
- Modify: `TrainIQ-Project/app/src/androidTest/java/com/trainiq/flow/TrainIqFlowSmokeInstrumentedTest.kt`

**Interfaces:**
- Consumes: `MainActivity`, the Home `Instellen starten` action, Coach profile fields, and `ActivityScenario.recreate()`.
- Produces: `profileDraftSurvivesActivityRecreationBeforeSave()`, a real-UI regression test for draft text and biological-sex selection.

- [x] **Step 1: Write the failing test**

```kotlin
@Test
fun profileDraftSurvivesActivityRecreationBeforeSave() {
    ActivityScenario.launch<MainActivity>(
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    ).use { scenario ->
        waitForText("Instellen starten")
        tap("Instellen starten")
        waitForText("Doeladvies")
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Rotatieprofiel")
        tap("Vrouw")

        scenario.recreate()

        waitForText("Rotatieprofiel")
        compose.onNodeWithText("Rotatieprofiel").assertIsDisplayed()
        compose.onNodeWithText("Vrouw").assertIsSelected()
    }
}
```

- [x] **Step 2: Run the test and verify RED**

Run from `TrainIQ-Project/`:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest#profileDraftSurvivesActivityRecreationBeforeSave" --console=plain --no-configuration-cache
```

Expected: the test reaches the Coach form, recreates `MainActivity`, then fails because `Rotatieprofiel` and the `Vrouw` selection are reset by ordinary `remember` state.

### Task 2: Make the complete profile draft saveable

**Files:**
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/features/coach/CoachScreen.kt`
- Test: `TrainIQ-Project/app/src/androidTest/java/com/trainiq/flow/TrainIqFlowSmokeInstrumentedTest.kt`

**Interfaces:**
- Consumes: Compose saveable-state registry and persisted `currentProfile` hydration already performed by `LaunchedEffect(profile)`.
- Produces: saveable draft state for `name`, `age`, `sex`, `height`, `weight`, `bodyFat`, `activityLevel`, `goal`, and validation error state where supported.

- [x] **Step 1: Add the minimal implementation**

Use `rememberSaveable` for every user-editable string. Store `BiologicalSex.name` as the saveable value and derive/update the enum through a small local mapping, or use a stable `Saver`; keep `LaunchedEffect(profile)` as the authority when an actual saved profile loads. Preserve the existing validation and save callbacks unchanged.

- [x] **Step 2: Run the focused test and verify GREEN**

Run the exact connected test command from Task 1. Expected: `1 test`, `0 failures`, `0 errors`.

- [x] **Step 3: Run focused JVM profile validation**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.profile.ProfileInputValidationTest" --console=plain --no-configuration-cache
```

Expected: PASS; saveability must not change validation rules.

### Task 3: Record and verify the closed-loop improvement

**Files:**
- Modify: `docs/TrainIQ_QA_Findings_To_Improve.md`
- Modify: `docs/TrainIQ_Target_State_Progress.md`
- Modify: `.codex/automation-state/trainiq-cycle.md`

**Interfaces:**
- Consumes: the red/green evidence and final local gates.
- Produces: one new closed lifecycle finding marked done, current verification evidence, and the unchanged owner-gate boundary for production release.

- [x] **Step 1: Record the finding and implementation evidence**

Add a dated P2 Android-lifecycle finding describing the lost Coach draft, target-state criterion, files changed, focused test, regression risk, and `done` status. Update progress and cycle state concisely without claiming owner-gated release readiness.

- [x] **Step 2: Run the affected-layer and authorized-PR gates**

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain --no-configuration-cache
.\gradlew.bat :app:generateDebugRoomMigrationChainVerificationMarker --console=plain --no-configuration-cache
```

Expected: all Gradle tasks pass and all connected tests report zero failures.

- [x] **Step 3: Run release-like packaging gates**

```powershell
.\gradlew.bat :app:assembleProfileable :macrobenchmark:assembleAndroidTest :app:checkReleaseSigningReadiness --console=plain --no-configuration-cache --no-daemon
```

Expected: profileable and macrobenchmark packages build; signing readiness accurately reports that production release signing is not configured.

- [x] **Step 4: Inspect runtime and repository evidence**

Launch the merged candidate on the agent-owned emulator, recreate the Coach profile draft once, capture an empty TrainIQ crash slice, run `git diff --check`, inspect exact staged paths, and scan the diff for secrets/generated artifacts before commit.

- [x] **Step 5: Commit**

Stage only the plan, instrumentation test, Coach screen, findings/progress/state files, then create one revertible Conventional Commit describing lifecycle restoration and exact verification.

## Self-review

- Spec coverage: the plan covers reproduction, all editable draft fields, real Activity recreation, focused validation, broad Android gates, documentation, and release-boundary reporting.
- Placeholder scan: no deferred implementation or test placeholders remain.
- Type consistency: the test exercises the existing `MainActivity`/Coach UI; production retains `BiologicalSex` at call boundaries and only its saveable representation changes locally.
