# TrainIQ Fixed Findings Index - 2026-05-27

Source ledger: `docs/qa/full-app-qa-run-2026-05-27.md`

Status: all findings discovered in the executed QA loops are fixed and have targeted verification plus regression evidence recorded in the source ledger.

## QA-2026-05-27-001

- Priority: `P1`
- Area: `Meer/Instellingen - privacy/security - local data clear`
- Issue: local data clear cleared only Gemini encrypted key state and missed OpenAI encrypted key storage.
- Fix: `ClearAppDataUseCase` uses all-key cleanup through the AI usage gate.
- Verification: targeted `ClearAppDataUseCaseTest`, full JVM tests, debug build, lint and connected regression passed.
- Remaining gate: privacy/security owner signoff still requires real-key save/readback and post-clear verification.

## QA-2026-05-27-002

- Priority: `P0`
- Area: `Voeding - recipes - destructive action - runtime ANR`
- Issue: saved recipe delete could trigger Android not-responding/ANR.
- Fix: close pending delete dialog state before invoking delete, and simplify delete dialog body layout.
- Verification: compile, targeted nutrition tests, install/runtime recipe delete retest, logcat no TrainIQ ANR/FATAL, full baseline regression passed.
- Remaining gate: none for the fixed ANR; broader nutrition/runtime gates remain listed separately in the main ledger.

## QA-2026-05-27-003

- Priority: `P1`
- Area: `Database/migration - release marker generation - buildscript drift`
- Issue: Gradle marker generator emitted Room migration chain v12 metadata while source-of-truth provider expected v13.
- Fix: buildscript marker/current/required/covered version moved to v13; regression test asserts buildscript/provider contract.
- Verification: `RoomMigrationChainVerificationProviderTest`, targeted nutrition tests and full baseline regression passed.
- Remaining gate: release artifact signing should still include normal migration-chain checklist.

## Current fixed-finding DoD status

- Repro recorded: yes.
- Expected/actual recorded: yes.
- Evidence recorded: yes.
- Fix recorded: yes.
- Targeted verification recorded: yes.
- Regression result recorded: yes.
- Open P0/P1/P2 from executed checks: none known.

This file is an index only. The full evidence paths and detailed repro text remain in `docs/qa/full-app-qa-run-2026-05-27.md`.

- QA-2026-05-27-004 - P2 accessibility touch-target hardening for Coach profile chips and Settings theme/feedback controls. Targeted verification: compileDebugKotlin, physical install, centered UIAutomator re-measure, clean logcat. Evidence: docs/qa/evidence/2026-05-27-physical-touch-target-audit-loop/touch-target-centered-after-fix-summary.txt.
- QA-2026-05-27-005 - P2 emulator accessibility semantics fix for anonymous Settings feedback/telemetry switches at font scale 1.5. Targeted verification: SettingsUiStateTest, emulator runtime UIAutomator dump with `NAF count: 0`, assembleDebug, emulator-only TrainIqFlowSmokeInstrumentedTest, lintDebug, clean logcat. Evidence: docs/qa/evidence/2026-05-27-emulator-ux-loop-settings-font/switch-label-runtime-summary.txt.
- QA-2026-05-27-006 - P2 emulator Settings feedback touch-target/clipping fix. Targeted verification: SettingsUiStateTest, assembleDebug, installDebug on `emulator-5554`, emulator UIAutomator audit across Start/Training/Voeding/Coach/Meer with `0` under-48dp clickable/focusable nodes, row-toggle runtime smoke, lintDebug, clean logcat. Evidence: docs/qa/evidence/2026-05-27-emulator-touch-target-audit-loop/touch-target-audit-summary-compact-settings-fix.txt.
- QA-2026-05-27-007 - P2 emulator Settings large-font text clipping fix. Targeted verification: SettingsUiStateTest, assembleDebug, installDebug on `emulator-5554`, font-scale 1.5 UIAutomator text-bounds audit across Start/Training/Voeding/Coach/Meer with `0` suspect text bounds after the fix, lintDebug, clean logcat. Evidence: docs/qa/evidence/2026-05-27-emulator-text-clipping-loop/text-clipping-audit-summary-after-fix.txt.
- QA-2026-05-27-008 - P1 active-workout logged-set correction crash. Targeted verification: compileDebugKotlin, compileDebugAndroidTestKotlin, testDebugUnitTest, lintDebug, previously failing connected classes, and full connectedDebugAndroidTest passed after removing the invalid active-key exercise FK via Room v14 migration. Evidence: docs/qa/evidence/2026-05-27-connected-baseline-refresh-loop/connectedDebugAndroidTest-final-after-room-v14-marker-fix.txt.
- QA-2026-05-29-001 - P1 scanner navigation savedStateHandle clear bug. Targeted verification: ScannerSavedStateHandleInstrumentedTest reproduced stale barcode state before the fix and passed after clear helpers published explicit empty string values; ScannerModeRouteTest, testDebugUnitTest, clean full connectedDebugAndroidTest and lintDebug passed. Evidence: docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/summary.txt.
- QA-2026-05-29-002 - P1 AI provider router transient failover bug. Targeted verification: AiProviderRouterTest reproduced OpenAI-first HTTP 429 blocking Gemini fallback before the fix and passed after provider-scoped retry throttling; AI services tests, compileDebugKotlin, testDebugUnitTest, lintDebug and assembleDebug passed. Evidence: docs/qa/evidence/2026-05-29-ai-provider-router-loop/summary.txt.
- QA-2026-05-29-003 - P2 Settings Health Connect partial-permission copy bug. Targeted verification: SettingsUiStateTest reproduced the expected partial-label/message behavior after the fix; testDebugUnitTest, lintDebug and assembleDebug passed. Evidence: docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/summary.txt.
- QA-2026-05-29-004 - P0 release lifecycle ANR. Targeted verification: release cold-idle and rotation-only repro produced `ANR in com.trainiq` before the fix; after moving Health Connect background scheduling and telemetry flush off the main dispatcher, assembleRelease, installRelease, cold-idle release smoke, rotation-only release smoke, full lifecycle release smoke, testDebugUnitTest and assembleDebug passed with empty TrainIQ crash/ANR scans. Evidence: docs/qa/evidence/2026-05-29-release-lifecycle-runtime-smoke-loop/summary.txt.
- QA-2026-05-29-005 - P0 Health Connect partial-grant startup ANR. Targeted verification: release launch with `READ_ACTIVE_CALORIES_BURNED` granted reproduced `ANR in com.trainiq`; after moving Health Connect datasource entrypoints to `Dispatchers.IO` and reducing Health Connect record read page size to 100, compileDebugKotlin, compileReleaseKotlin, assembleRelease, isolated partial-grant release launch, revoke-while-open plus relaunch, testDebugUnitTest, lintDebug and assembleDebug passed with empty TrainIQ crash/ANR scans. Evidence: docs/qa/evidence/2026-05-29-healthconnect-revoke-while-open-loop/summary.txt.

- Runtime closure: active-workout logged-set edit/delete interaction now has emulator-only instrumented coverage via ActiveWorkoutSetActionsInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-active-workout-set-actions-loop/ActiveWorkoutSetActionsInstrumentedTest-rerun.txt.
- Runtime closure: Exercise History seeded detail now has emulator-only instrumented coverage via ExerciseHistoryInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-exercise-history-loop/ExerciseHistoryInstrumentedTest-visible-stats-pass.txt.
- Runtime closure: barcode scanner camera denied/granted path now has emulator-only instrumented coverage via CameraPermissionScannerInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-camera-permission-loop/CameraPermissionScannerInstrumentedTest-pass.txt.
- Runtime closure: Nutrition Ochtend add-sheet long-form IME path at font scale 1.5 now has emulator-only instrumented coverage via NutritionLongFormImeInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-nutrition-longform-ime-loop/NutritionLongFormImeInstrumentedTest.txt.
- Runtime closure: Coach seeded training insights and nutrition coach message now have emulator-only instrumented coverage via CoachInsightsInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-coach-insights-loop/CoachInsightsInstrumentedTest.txt.
- Runtime closure: Coach weekly report local-analysis generation now has emulator-only instrumented coverage via CoachInsightsInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-coach-insights-loop/CoachInsightsInstrumentedTest-weekly-report.txt.
- Runtime closure: Coach fallback source label and structured bullet/section clarity now have emulator-only coverage via CoachInsightsInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-coach-insights-loop/CoachInsightsInstrumentedTest-weekly-report.txt.

# QA-2026-05-29-008 - P0 - Release dark/font-scale startup ANR

- status: fixed
- area: Release APK - dark mode/font-scale startup responsiveness
- fix: replaced Home startup shimmer placeholders with static placeholders and delayed `MainActivity` startup diagnostics/JankStats/background sync scheduling by 8 seconds.
- verification: release cold launch under dark mode/font scale 1.3 stayed strict TrainIQ crash/ANR/security-clean for 35 seconds after the fix; regression unit/lint/debug/release assemble passed.
- evidence: `docs/qa/evidence/2026-05-29-release-dark-font13-topnav-loop/summary.txt`

# QA-2026-05-29-007 - P0 - Release cold-launch early accessibility dump ANR

- status: fixed
- area: Release APK - startup/accessibility focus responsiveness
- fix: moved Home Health Connect status refresh to `Dispatchers.IO` and dashboard mapping to `Dispatchers.Default`, reducing early post-first-draw main-thread pressure.
- verification: release cold launch plus early UIAutomator dump returned no strict TrainIQ crash/ANR/security matches after the fix; `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease` passed.
- evidence: `docs/qa/evidence/2026-05-29-current-release-readiness-refresh-loop/summary.txt`

# QA-2026-05-29-006 - P1 - Profileable active-workout benchmark seed/launch ANR

- status: fixed
- area: Profileable Macrobenchmark - active workout seed/start/logging
- fix: moved profileable seed work off `BenchmarkSeedActivity.onCreate` main-thread blocking, removed the seed task after completion, and lengthened bounded UI waits in the active-workout benchmark harness.
- verification: targeted active-workout Macrobenchmark passed 1/1 with explicit `EMULATOR` suppression; seeded launch and post-benchmark strict TrainIQ crash/ANR/security scans returned no matches; regression compile/unit/lint/assemble passed.
- evidence: `docs/qa/evidence/2026-05-29-profileable-active-workout-benchmark-loop/summary.txt`
