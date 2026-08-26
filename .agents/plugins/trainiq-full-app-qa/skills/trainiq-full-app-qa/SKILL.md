---
name: trainiq-full-app-qa
description: "Use when running, planning, or documenting a full TrainIQ Android QA cycle across all tabs and underlying flows: Start/Home, Training, Voeding, Voortgang, Coach, Meer/Instellingen, CameraScanner, Health Connect, AI/fallbacks, backend/data, design, accessibility, performance, privacy/security, release readiness, findings, and regression evidence."
---

# TrainIQ Full-App QA

Use this skill for complete TrainIQ QA. Cover the whole app, not only one feature.

## Required Inputs

Read these first:

- `AGENTS.md`
- `TrainIQ-Project/README.md`
- `TrainIQ-Project/docs/qa/full-app-qa-basis.md`
- `TrainIQ-Project/docs/qa/full-app-qa-run-template.md`
- Existing related QA docs in `TrainIQ-Project/docs/qa/`
- Relevant source/tests for any failing or changed flow

Use these supporting skills when available:

- `test-android-apps`
- `android-quality-gate`
- `trainiq-target-state-qa`
- `trainiq-polish-regression-guard` before implementing fixes

## Workflow

1. Create or select a QA run file.
   - Prefer `TrainIQ-Project/docs/qa/full-app-qa-run-YYYY-MM-DD.md`.
   - If no run file exists for the date, copy from `TrainIQ-Project/docs/qa/full-app-qa-run-template.md`.
   - The helper script can do this: `powershell -NoProfile -ExecutionPolicy Bypass -File .agents/plugins/trainiq-full-app-qa/skills/trainiq-full-app-qa/scripts/new-full-app-qa-run.ps1`.

2. Run the smallest useful automated checks first.
   - `.\gradlew.bat :app:assembleDebug --console=plain`
   - `.\gradlew.bat :app:testDebugUnitTest --console=plain`
   - `.\gradlew.bat :app:lintDebug --console=plain`
   - `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` only when a device/emulator is available.

3. Run targeted tests for high-risk areas.
   - AI/nutrition: `AiServicesTest`
   - Navigation: route and adaptive navigation tests
   - Workout: active workout, debrief, progression, transaction tests
   - Nutrition: forms, scan state, meal identity, save/reopen behavior
   - Progress: measurement validation and chart semantics
   - Coach: goal advice, weekly report, Dutch-output/fallback guards
   - Settings: Health Connect, key storage, telemetry, destructive actions

4. Exercise every top-level tab and all reachable subsections.
   - Start/Home
   - Training
   - Voeding
   - Voortgang
   - Coach
   - Meer/Instellingen
   - Cross-tab flows: CameraScanner, barcode scanner, scale scanner, Health Connect rationale, savedStateHandle returns, back stack, lifecycle.

5. Record evidence as `PASS`, `FAIL`, or `NOT RUN`.
   - `NOT RUN` must include the exact reason.
   - Any `FAIL` must become a finding using the schema in `full-app-qa-basis.md`.

6. Keep high-risk regressions explicit.
   - Multi-component meal scan must preserve item identity for inputs like `kip rollade 80g, kaas 30g, wrap 60g, saus 15g, sla 20g`.
   - Workout completion must not use local fallback when AI is enabled, configured, and returns valid Dutch JSON.
   - Active workout sets must remain tied to the correct exercise/source workout exercise.
   - Historic meal snapshots must not silently change after food/recipe edits.
   - AI must not overwrite deterministic profile/calorie baseline values.
   - Secrets must never appear in logs, URLs, screenshots, crash evidence, or production BuildConfig values.

7. Before claiming QA complete, check release gates.
   - Accessibility owner signoff remains required for TalkBack/Switch Access.
   - Performance certification requires physical-device macrobenchmark evidence and thresholds.
   - Health Connect matrix must cover provider missing, no permission, partial permission, revoke while open, and background-read states.
   - Privacy/security and Play Data Safety decisions remain owner/legal/security gated when production behavior changes.

## Output Requirements

Final response must include:

- QA run file path
- Commands run with `PASS`, `FAIL`, or `NOT RUN`
- Highest-risk findings
- Release gates still open
- Next safest action

Do not mark release readiness complete unless the run file proves every required gate is closed.
