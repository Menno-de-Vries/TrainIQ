# Baseline tests — 2026-09-30

## Scope and environment

Wave 1 inventory and local baseline, no production source/test changes. Governing sources read: root `AGENTS.md`, full `docs/agent-guides/local-testing.md`, target-state blueprint and architecture ADRs. Starting clean tree: `d32922e642ca38a348d978716f81723396c6762e`; execution branch `codex/full-audit-2026-09-30`. Risk is cross-feature verification; baseline results are evidence, not proof of all feature/platform states.

Working directory: `C:\My-PC-Files\GitHub\TrainIQ\TrainIQ-Project`. Local properties absent; command-session discovery selected Android Studio JBR `C:\Program Files\Android\Android Studio\jbr` (OpenJDK 25.0.2) and installed SDK `C:\Users\menno\AppData\Local\Android\Sdk`. Installed platforms include Android 36 and 36.1. Project: Gradle 9.3.1, AGP 9.1.0, compile SDK 36.1, target 36, min 26; configured AndroidJUnitRunner. No downloads, clean, remote runner/cache, signing or credential changes.

## Commands and results

Session environment:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --offline
```

Baseline result: PASS, exit 0, `BUILD SUCCESSFUL in 4m 8s`; 69 actionable tasks (29 executed, 40 up-to-date). All four requested tasks passed. JVM XML results: 973 tests across 132 suites, 0 failures/errors/skips. Lint: 0 errors and 68 existing warnings (23 GradleDependency, 15 UseKtx, 14 NewerVersionAvailable, 7 UnusedResources, 2 each AndroidGradlePluginVersion/ApplySharedPref/MissingKeepAnnotation/ObsoleteSdkInt, 1 UnsupportedChromeOsCameraSystemFeature). These are reported findings; no dependency or unrelated source changes were made. Reports: `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/test-results/testDebugUnitTest/`, `app/build/reports/lint-results-debug.html` and `.xml`. Generated log is ignored `.codex/baseline-2026-09-30/gradle.log` inside the app project. Early warning: SDK processing understands XML through version 3 but encountered version 4. No environment configuration was written to tracked files.

```powershell
node --test food-gateway/server.test.mjs food-gateway/fatsecret.test.mjs
```

PASS: Node v24.17.0, 7 tests, 7 pass, 0 fail/cancel/skip/todo. Synthetic transport/fake credentials and loopback server only; no live upstream requests. Evidence: `.codex/baseline-2026-09-30/gateway-tests.log`.

Connected baseline: PASS 9/9, 0 failed/skipped, exit 0, 58s; 78 tasks (23 executed/55 up-to-date). Root-owned isolated `emulator-5580`, installed `TrainIQ_Agent_API36_20260806`, Android 16/API 36. Behavior sources still baseline HEAD; report files alone set BuildConfig GIT_DIRTY. Exact canonical command below. The final integrated verification will run the full connected suite rather than duplicate it before and after changes. Reports: `app/build/reports/androidTests/connected/debug/` and `app/build/outputs/androidTest-results/connected/debug/`; baseline result XML copied to ignored `.codex/baseline-2026-09-30/connected-results/` before final suite can overwrite it. Root baseline UI evidence recorded separately in `.codex/device-qa/audit-2026-09-30/` at repository root; cold launch Status ok / WaitTime 1019ms, compact Coach/Progress captured. These debug emulator data do not certify physical performance. Migration marker, profileable/macrobenchmark and signing readiness: NOT RUN in this wave; signing/release work is outside this baseline scope.

## Inventory and layer matrix

Counts are source inventory, not executed test counts: 132 JVM Kotlin files with 973 `@Test` occurrences; 54 connected Kotlin files including shared DB helper with 190 occurrences; 2 macrobenchmark Java classes with 6 tests. Five Room-import JSON resources provide minimal, representative, current shape, missing optional and malformed fixtures. 42 JVM files include File/readText/substring source assertions; these enforce structural intent but cannot certify framework/transport runtime behavior.

| Surface | Existing cheapest proof | Existing wider proof | Proposed check / residual gap |
|---|---|---|---|
| Domain/math/use cases | Energy/nutrition/hydration/strength, validation, home builder, imports | Feature smoke | Reuse focused unit tests for changed rules; no percentage target |
| ViewModel/state/Flow | Home, workout reducers/observations, nutrition, settings, coach drafts, onboarding, sleep policies | Restoration/recovery UI suites | Deterministic state/Flow first; actual process death remains different from Activity recreation |
| Room/import/persistence | Mappers, import planner/readiness, repository transactions/parity, debrief identity | TargetedRoomPersistence, CoordinatorMutation, RoomJsonImportSink, RoomImportDryRun, migrations | Full connected suite plus migration marker after passing chain proof; DB close/reopen does not certify entire process lifecycle |
| Health Connect | Partial/revoked permissions, per-metric token/cache/status policy, cancellation, background worker | Rationale/provider intent suites | Missing executable provider/fake paging and insert/update/delete application tests: no pageToken/hasMore/UpsertionChange/DeletionChange references in current datasource test inventory. Safe provider/runtime matrix still needed |
| AI/remote/security | Structured service parsing, invalid JSON, timeout, cancellation, throttling/fallback, provider routing, key migration/privacy guards | Image preparation, AI Settings route | Synthetic contracts cannot certify live BYOK/model/provider behavior; no live calls in baseline |
| Nutrition/camera/barcode | Input/write/portions/hydration, latest request, camera state/privacy, product lookup | Permission/frame/recognition/scanner recovery, manual/AI result restoration, IME/add/meal flows | Safe emulator scanner semantics first; CameraX hardware/provider availability needs permitted device evidence |
| Navigation/adaptive/accessibility | Typed route/policy/propagation, charts/dialog/action layouts | Swipe/nav/onboarding/viewport, focus/readability/chart semantics, long forms | Existing expanded viewport test is fixed geometry; manual TalkBack/Switch Access and whole-flow device configurations cannot be inferred |
| Sleep/reminders/diagnostics | Sleep policy, reminders/privacy, diagnostics/frame/export | Alarm/routine/observation/notification suites | Connected baseline; preserve clock-boundary evidence and investigate flakes without unchanged retry |
| Food gateway | 7 Node tests: GTIN, serving normalization, token concurrency/expiry, fail closed, safe errors | Loopback HTTP status/privacy tests | PASS synthetic baseline; no upstream credentials or authorization implied |
| Performance | Source guards for seed paths and bounded persistence | Baseline profile producer, cold start, navigation/settings, workout frames; scanner minified smoke | Build profileable/benchmark packages if scope widens; physical device needed for performance claims |

## Defect versus evidence gap

No failing baseline defect was established. Lint warnings and the SDK XML mismatch are baseline findings. Listed residuals are coverage or runtime-evidence limits, not assertions that current application behavior fails. Existing ActivityScenario recreation, StateRestorationTester and database reopen tests provide distinct useful proof and should remain; any process-death check must prove additional risk rather than duplicate them. No `@Ignore`, `Thread.sleep`, TODO or FIXME was found in the current test roots; coroutine `delay` occurrences in retry tests run under controlled `runTest` virtual time, and source guards also mention delay literals.

Evidence validity ends when relevant source/config/dependency/fixture/schema/environment inputs change. Reuse passing results until invalidated; run focused proof at the owning layer for changes, and widen only for unique transitive risk. Generated outputs remain untracked.


Filtered baseline connected command (same JAVA_HOME/SDK environment):

```powershell
$env:ANDROID_SERIAL='emulator-5580'
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest,com.trainiq.features.workout.WorkoutCompletionRecoveryInstrumentedTest,com.trainiq.features.coach.CoachHealthNavigationInstrumentedTest' --console=plain --offline
```

Baseline freeze completed before wave 2 source edits. No app tracked paths changed during baseline.
