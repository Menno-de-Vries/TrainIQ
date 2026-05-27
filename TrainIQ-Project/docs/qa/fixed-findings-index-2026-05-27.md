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

- Runtime closure: active-workout logged-set edit/delete interaction now has emulator-only instrumented coverage via ActiveWorkoutSetActionsInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-active-workout-set-actions-loop/ActiveWorkoutSetActionsInstrumentedTest-rerun.txt.
- Runtime closure: Exercise History seeded detail now has emulator-only instrumented coverage via ExerciseHistoryInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-exercise-history-loop/ExerciseHistoryInstrumentedTest-visible-stats-pass.txt.
- Runtime closure: barcode scanner camera denied/granted path now has emulator-only instrumented coverage via CameraPermissionScannerInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-camera-permission-loop/CameraPermissionScannerInstrumentedTest-pass.txt.
- Runtime closure: Nutrition Ochtend add-sheet long-form IME path at font scale 1.5 now has emulator-only instrumented coverage via NutritionLongFormImeInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-nutrition-longform-ime-loop/NutritionLongFormImeInstrumentedTest.txt.
- Runtime closure: Coach seeded training insights and nutrition coach message now have emulator-only instrumented coverage via CoachInsightsInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-coach-insights-loop/CoachInsightsInstrumentedTest.txt.
- Runtime closure: Coach weekly report local-analysis generation now has emulator-only instrumented coverage via CoachInsightsInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-coach-insights-loop/CoachInsightsInstrumentedTest-weekly-report.txt.
- Runtime closure: Coach fallback source label and structured bullet/section clarity now have emulator-only coverage via CoachInsightsInstrumentedTest; evidence: docs/qa/evidence/2026-05-27-emulator-coach-insights-loop/CoachInsightsInstrumentedTest-weekly-report.txt.
