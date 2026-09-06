# TrainIQ Flow Audit Polish Progress

## 2026-09-06 Fresh audit rerun

Implemented the five new findings FRESH-001 through FRESH-005 documented in [the findings register](TrainIQ_QA_Findings_To_Improve.md): targeted workout undo, local activity dates, routine-input validation, bounded plate previews and recoverable meal-save errors. Task branch: `codex/five-fresh-flow-fixes`, from main `acaf79086d2a9aaa26688b3d313b114ce032a245`. The PR carries final-commit local verification and environment limits. Existing historical findings and unrelated worktrees remain preserved; no overall readiness or alignment score was re-certified.

Updated date: 2026-05-12

Source of truth: `docs/TrainIQ_QA_Findings_2026-05-12_Flow_Audit.md`

Readiness status: ready-to-use: no. `FLOW-005` remains blocked until a disposable Health Connect device/profile is available for mutable provider and permission-state evidence.

## QA-2026-05-12-FLOW-001

- Priority: P2
- Flow/module: connected normal, break-app, and confused-user app flow smoke
- Done criteria: connected flow suite launches from a known clean local test state, reaches Start, Training, Voeding, Coach, Meer/Instellingen, and verifies stable guidance/fallback affordances without launch/navigation crashes.
- Status: done
- Files changed: `TrainIQ-Project/app/src/androidTest/java/com/trainiq/flow/TrainIqFlowSmokeInstrumentedTest.kt`, `TrainIQ-Project/app/build.gradle.kts`
- Verification: PASS `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B (`RFCY60HNHNJ`)
- Known regressions: none known
- Remaining work: none for this finding
- Readiness status: done

## QA-2026-05-12-FLOW-002

- Priority: P2
- Flow/module: Settings accessibility
- Done criteria: theme-mode chips expose explicit accessible labels, focused unit/source guard passes, lint passes, and Settings remains visually coherent.
- Status: done
- Files changed: `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`, `TrainIQ-Project/app/src/test/java/com/trainiq/features/settings/SettingsUiStateTest.kt`
- Verification: PASS `.\gradlew.bat :app:clean :app:testDebugUnitTest --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`; PASS `.\gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`
- Known regressions: none known
- Remaining work: optional compact-device UI dump for NAF evidence during the next manual QA pass
- Readiness status: done

## QA-2026-05-12-FLOW-003

- Priority: P2
- Flow/module: first-run QA fixture
- Done criteria: instrumentation setup clears only `trainiq.db` and the TrainIQ DataStore preferences file before launch, then verifies first-run guidance without production reset UI or destructive device-wide behavior.
- Status: done
- Files changed: `TrainIQ-Project/app/src/androidTest/java/com/trainiq/flow/TrainIqFlowSmokeInstrumentedTest.kt`
- Verification: PASS targeted connected flow test on SM-S931B; fixture deletes only `trainiq.db` and `datastore/trainiq_preferences.preferences_pb` inside the app data directory before launch
- Known regressions: none known
- Remaining work: none for the safe local-state fixture; Health Connect permissions/provider state remain intentionally untouched
- Readiness status: done

## QA-2026-05-12-FLOW-004

- Priority: P3
- Flow/module: compact Progress discoverability
- Done criteria: compact bottom navigation still excludes Progress, Settings reads as the overflow route, and Progress is discoverable through Settings copy/action near the top.
- Status: done
- Files changed: `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`, `TrainIQ-Project/app/src/test/java/com/trainiq/navigation/AdaptiveNavigationPolicyTest.kt`
- Verification: PASS `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.navigation.AdaptiveNavigationPolicyTest" --console=plain --no-configuration-cache`; PASS targeted connected flow test on SM-S931B
- Known regressions: none known
- Remaining work: none for compact overflow discoverability
- Readiness status: done

## QA-2026-05-12-FLOW-005

- Priority: P1
- Flow/module: Health Connect runtime matrix
- Done criteria: provider-missing/update, partial grants, revoke-while-open, background-read unavailable/granted states are verified on a disposable profile/device with UI dumps and crash slices.
- Status: blocked
- Files changed: `docs/TrainIQ_Flow_Audit_Polish_Progress.md`
- Verification: NOT RUN. No disposable/safe Health Connect device/profile was provided in this session.
- Known regressions: none known
- Remaining work: run `TrainIQ-Project/scripts/collect-health-connect-runtime-evidence.ps1` plus the matrix steps in `TrainIQ-Project/docs/qa/health-connect-runtime-matrix-2026-05-10.md` on a disposable profile/device where Health Connect provider and permissions can be safely changed.
- Readiness status: blocked
