# TrainIQ Trust and Interaction Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:test-driven-development for each behavior change and superpowers:verification-before-completion before handoff.

**Goal:** Make TrainIQ navigation calmer and coaching terminology truthful while adding recoverable Trend errors, without expanding architecture or dependencies.

**Architecture:** Preserve the existing MVVM/Room/domain flow. Change only internal domain names and derived state, remove one root pointer modifier, and reuse the shared reloadable observation primitive.

**Tech Stack:** Kotlin, Jetpack Compose, StateFlow, Hilt, JUnit4, Turbine, Gradle.

## Global Constraints

- No new dependency or Room schema version.
- No broad UI redesign or unrelated refactor.
- No extra tests unless they protect changed behavior.
- Preserve bottom-bar/rail state restoration, Health Connect behavior, AI behavior, and all persistence paths.
- Every production behavior change follows a witnessed red-green cycle.

---

### Task 1: Visible-only top-level navigation

**Files:**
- Modify: `TrainIQ-Project/app/src/test/java/com/trainiq/navigation/AdaptiveNavigationPolicyTest.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/navigation/TrainIqNav.kt`

**Interface:** The six existing typed top-level destinations remain unchanged; only `topLevelTabSwipeNavigation` and `compactSwipeNavigationRouteClasses` are removed.

- [ ] Replace the obsolete swipe-parity test with a failing source contract that rejects the global swipe modifier/helper.
- [ ] Run `:app:testDebugUnitTest --tests "com.trainiq.navigation.AdaptiveNavigationPolicyTest"` and confirm RED.
- [ ] Remove the modifier, helper, policy function, and now-unused gesture/haptic imports.
- [ ] Re-run the focused test and confirm GREEN.

### Task 2: Truthful local coaching and weekly load semantics

**Files:**
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/domain/model/DomainModels.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/analytics/AnalyticsEngine.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/features/home/HomeScreen.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/features/progress/ProgressScreen.kt`
- Modify: focused existing tests under `app/src/test/java/com/trainiq`

**Interfaces:**
- Produce `HomeDashboard.coachInsight: String` in place of `aiInsight`.
- Produce `ProgressOverview.weeklyLoadRatio: Double?` in place of `fatigueIndex`.
- Produce `AnalyticsEngine.weeklyLoadRatio(latestWeeklyVolume: Double, baselineWeeklyVolume: Double): Double`.

- [ ] Add failing tests for one-week `null`, multi-week ratio, Home local-source copy, and absence of fatigue/RPE claims.
- [ ] Run the focused repository/Home/Progress tests and confirm RED.
- [ ] Implement the minimal model, calculation, repository, and copy changes.
- [ ] Re-run focused tests and confirm GREEN.

### Task 3: Reloadable Trend errors

**Files:**
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/features/progress/ProgressScreen.kt`
- Modify: `TrainIQ-Project/app/src/test/java/com/trainiq/features/progress/ProgressMeasurementValidationTest.kt`
- Modify: `TrainIQ-Project/app/src/test/java/com/trainiq/features/ui/FatalErrorRetrySourceTest.kt`

**Interface:** `progressUiState(observation: Result<ProgressOverview>?, message: UiMessage?)` maps null to Loading, failure to Error, and success to Success. `ProgressViewModel.retry()` increments its reload counter.

- [ ] Add failing state-mapping and retry-wiring tests.
- [ ] Run the two focused suites and confirm RED.
- [ ] Reuse `reloadableObservation`, wire `viewModel::retry`, and add the 48dp `Opnieuw proberen` action.
- [ ] Re-run focused tests and confirm GREEN.

### Task 4: Regression and runtime gate

**Files:** No new production files.

- [ ] Run the full debug unit, assemble, lint, and Android-test compilation gate.
- [ ] Install and cold-launch on `emulator-5554`.
- [ ] Tap all six top-level destinations using UI-tree bounds; verify vertical/diagonal swipes do not switch tabs and inspect crash logcat.
- [ ] Request an independent diff review and fix every Critical or Important finding.
- [ ] Update findings/progress/cycle state with exact evidence and remaining risk.
