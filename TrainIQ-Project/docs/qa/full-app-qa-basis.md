# TrainIQ Full-App QA Basis

Last updated: 2026-05-26

Status: baseline QA plan. This document defines the required full-app QA scope for every deep TrainIQ QA cycle. It does not certify release readiness by itself.

## Goal

Validate the complete TrainIQ Android app across frontend, backend/data, feature behavior, AI/fallbacks, Health Connect, design, accessibility, performance, privacy/security, and release readiness.

Every check must be recorded as `PASS`, `FAIL`, or `NOT RUN` with a reason. Every `FAIL` must produce a finding with repro steps, expected behavior, actual behavior, evidence, severity, recommended fix, regression risk, and minimal verification.

## Required Skills And Workflows

- Use `test-android-apps` for Android build/test/emulator/device QA, adb/logcat, lifecycle, permissions, accessibility, performance, crash checks, and smoke screenshots.
- Use `android-quality-gate` for build health, lifecycle, navigation, loading/error/offline states, runtime permissions, accessibility, jank, memory, and release-readiness checks.
- Use `trainiq-target-state-qa` for full TrainIQ blueprint alignment across backend, data, design, features, privacy, security, performance, and release blockers.
- Use `trainiq-polish-regression-guard` before implementing fixes from QA findings.

## Full Tab Matrix

| Tab/area | Frontend and UX checks | Backend/data checks | AI, permissions, and edge checks |
|---|---|---|---|
| Start/Home | First run, missing profile, missing Health Connect, missing routine, dashboard cards, CTA routing, loading/empty/error/partial states, dark mode, large font, tablet/foldable layout | `BuildHomeDashboardUseCase`, repository aggregation, Health Connect cache, energy/dashboard data after restart and local clear | Health Connect unavailable/no permission, stale cache, AI insight fallback |
| Training | Routine list, create/edit/delete routine, generated routine preview, exercise library, exercise picker, exercise history, active workout controls, rest timer, finish flow, completion screen, dense control touch targets | Room transactions, workout log events, progression suggestions, active workout restore, debrief persistence, `sourceWorkoutExerciseId` integrity | AI routine generation, workout debrief valid Gemini response, missing key, disabled AI, malformed JSON, timeout, rate-limit, local fallback |
| Voeding | AI meal scan, manual food, products, recipes, meal logging, barcode lookup, meal history, reuse meal, long forms, keyboard/IME behavior, preview clarity | Nutrition mappers, save meal transactions, food/recipe/meal snapshots, scanned result storage/clear, app restart/reopen integrity | Multi-component scan identity, suspicious duplicate warning, camera denied, missing AI key, invalid AI response, barcode offline/fail |
| Voortgang | Body measurement forms, weight/body-fat/muscle validation, smart-scale result review, charts, trend labels, empty states, dark mode, large font | Measurement persistence, progress overview aggregation, chart point generation, delete behavior after restart | AI scale valid/partial/no result, camera denied, manual fallback |
| Coach | Goal advice, weekly report, training insights, nutrition coach message, source labels, loading/error/fallback states, advice readability | Goal baseline math, profile/calorie target integrity, weekly report parsing, Dutch-output guard | AI enabled/disabled/missing key/invalid response/English response/timeout/rate-limit/offline, deep-mode thinking budget, JSON schema |
| Meer/Instellingen | Theme mode, telemetry opt-in/out, Gemini/OpenAI key save/delete, provider preference, Health Connect status/rationale/settings, destructive confirmations, disclosure clarity | UserPreferencesRepository, Android Keystore stores, Health Connect sync preferences, local data clear, DataStore/Room/cache state | Secrets not logged or exposed, telemetry opt-in only, provider missing/update, partial Health Connect, revoke while open, background-read |
| Cross-tab | Camera scanner, barcode scanner, scale scanner, tab switching, back stack, scanner return values, savedStateHandle results, app background/foreground, rotate/recreate | Shared navigation state, active workout/session/meal/progress persistence across lifecycle | Permission denied/granted, no camera, offline/slow network, repository save fail, logcat crash/ANR scan |

## High-Risk Regression Checks

- Nutrition AI item identity: explicit user context such as `kip rollade 80g, kaas 30g, wrap 60g, saus 15g, sla 20g` must remain separate through scan result, preview, edit, save, reopen, and app restart.
- Workout AI fallback: when AI is enabled, a valid key exists, and Gemini returns valid Dutch structured JSON, workout completion must use Gemini/API source instead of local fallback.
- Active workout integrity: logged sets must remain attached to the correct exercise and source workout exercise after edit, undo, delete, finish, recreate, and restart.
- Meal snapshot integrity: historical meal logs must keep saved names/macros and must not silently change when a food or recipe is edited later.
- Profile/calorie integrity: Coach AI must not overwrite deterministic local baseline values.
- Secrets/privacy: API keys must never appear in logs, URLs, screenshots, crash evidence, or BuildConfig production values.

## Automated Baseline

Run from `D:\GitHub\TrainIQ\TrainIQ-Project`:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:lintDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Targeted checks:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --console=plain
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.navigation.*" --console=plain
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.*" --console=plain
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.*" --console=plain
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.progress.*" --console=plain
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.coach.*" --console=plain
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.settings.*" --console=plain
```

Performance/device certification:

```powershell
.\gradlew.bat :app:assembleProfileable :macrobenchmark:assembleAndroidTest --console=plain
.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain
```

## Manual Device Pass

Record exact device model, Android version, app version/build id, build variant, commit/build identifier, theme, font scale, network state, permission state, and tester.

Required manual pass:

- Open every top-level tab: Start, Training, Voeding, Voortgang, Coach, Meer.
- In each tab, open every reachable subsection, primary CTA, edit/save flow, destructive action, loading/empty/error state, and back route.
- Capture screenshots or screen recording for every failure.
- Capture logcat crash/ANR slice after smoke and after any crash-like behavior.
- Record `PASS`, `FAIL`, or `NOT RUN` for every row in the run template.

## Accessibility And Design Pass

- Run large font at 1.3 and 1.5 where available.
- Run light theme, dark theme, and system/dynamic color on Android 12+.
- Run TalkBack and Switch Access on high-risk screens: active workout, nutrition forms/scanner, Coach advice, Progress charts, Settings destructive dialogs, Health Connect rationale.
- Check touch targets, labels, focus order, contrast, text overlap, keyboard/IME behavior, and modal focus containment.
- Use `docs/qa/talkback-switch-access-test-script.md` and `docs/qa/human-assistive-tech-qa-signoff.md` for final accessibility evidence.

## Findings Schema

Use this schema for every failure or improvement:

```markdown
## Finding QA-YYYY-MM-DD-###

- priority: P0 | P1 | P2 | P3
- area: frontend | backend | data | AI | Health Connect | UX/design | accessibility | performance | privacy | security | release | tests
- tab/flow:
- status: open | blocked | needs-decision | done
- current evidence:
- expected behavior:
- actual behavior:
- repro steps:
- recommended fix:
- regression risk:
- minimal verification:
- owner suggestion:
```

Severity:

- `P0`: crash, data loss, privacy leak, unusable core flow.
- `P1`: wrong saved data, wrong AI/fallback behavior, broken workout/nutrition/progress/coach flow, Health Connect sync/permission failure.
- `P2`: accessibility issue, unclear UX, design issue, jank, weak error state.
- `P3`: polish or feature improvement.

## Release Gate Rule

Do not mark QA complete unless:

- All full-tab matrix rows are `PASS`, `FAIL`, or `NOT RUN` with reason.
- All P0/P1 findings have evidence and fix direction.
- Accessibility, performance, Health Connect, privacy/security, and Play release gates have owner signoff or remain explicitly open.
- No release-readiness claim is made from emulator-only performance evidence.
