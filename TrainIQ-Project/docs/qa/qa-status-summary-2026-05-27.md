# TrainIQ QA Status Summary - 2026-05-27

Current status: `PARTIAL`

Release-ready by full DoD: `NO`

## 2026-05-28 Direct APK readiness refresh

- Commit/build identifier: `fd9512e1`.
- Device/emulator: `emulator-5554`, `sdk_gphone64_x86_64`.
- App version/build id: `versionName 1.0.1-A`, `versionCode 2`, package `com.trainiq`.
- PASS: `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:connectedDebugAndroidTest`, `:app:checkReleaseSigningReadiness`, `:app:assembleRelease`, fresh `:app:installRelease`, release cold launch and release logcat crash/ANR scan.
- FAIL / not release-blocking app bug: the exact debug-to-release upgrade command failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` because debug and release signatures differ. This confirms the command is not a valid proxy for direct APK user upgrades unless both APKs share signing lineage.
- New reproducible app P0/P1/P2/P3 bugs from executed checks: none.
- Evidence: `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/summary.txt`.
- Direct APK Ready: `NO`, because owner/manual gates remain open and have not been owner-approved for defer.


## What is green

- Automated baseline passed after the active-workout active-key schema fix:
  - `assembleDebug`
  - `testDebugUnitTest`
  - `lintDebug`
  - `connectedDebugAndroidTest`
- Current-build emulator smoke passed with no TrainIQ crash/ANR match.
- No open P0/P1/P2 bugs are known from executed checks.
- Eight executed-loop findings were fixed and verified:
  - `QA-2026-05-27-001`: local data clear missed OpenAI encrypted key storage.
  - `QA-2026-05-27-002`: saved recipe delete could trigger ANR.
  - `QA-2026-05-27-003`: Room migration marker generation drifted behind v13.
  - `QA-2026-05-27-004`: Coach/Settings controls hardened to explicit 48dp touch height.
  - `QA-2026-05-27-005`: Settings feedback/telemetry switches gained stateful accessibility labels.
  - `QA-2026-05-27-006`: Settings feedback touch-target/clipping issue was fixed.
  - `QA-2026-05-27-007`: Settings large-font text clipping issue was fixed.
  - `QA-2026-05-27-008`: active-workout logged-set correction crashed when the Room draft active key was a workout-exercise id.
- Runtime coverage exists for major paths including first-run Home, Settings/Health Connect rationale, Nutrition recipe create/use/edit/delete, Progress add/invalid/delete, Coach local goal advice, active workout log/finish/completion and cross-tab/lifecycle smoke.
- Source/unit/contract coverage was refreshed for AI, scanner/barcode, Health Connect policy, accessibility semantics, dynamic color, adaptive layout and performance tooling buildability. Physical-device assistive-tech state was captured on `SM-S931B`; accessibility services are disabled, so TalkBack/Switch traversal remains open.

## Why Done is still open

The remaining gaps require runtime or owner evidence that was intentionally not produced in the safe run; the physical-device macrobenchmark gate is now closed as PASS:

- TalkBack/Switch Access traversal; physical device currently has accessibility disabled (`accessibility_enabled=0`, `enabled_accessibility_services=null`).
- Health Connect partial grant, revoke-while-open and background-read runtime matrix.
- Privacy/security real-key save/readback/signoff.
- Live AI/provider flows.
- Real camera/scanner return through app navigation.
- Manual deep-runtime UX audits for active-workout edits, Exercise History, long forms, smart-scale valid result, full touch-target certification, overlap/clipping, modal focus containment and focus order.

## Release decision

Recommended decision: `DEFER RELEASE READINESS`

Rationale: the executed QA loops found and fixed P0/P1 issues and the automated baseline is green after the active-workout active-key schema fix, but the full Definition of Done explicitly requires runtime/owner gates that remain `NOT RUN` without owner-approved defer.

## Reviewer map

- Full ledger: `docs/qa/full-app-qa-run-2026-05-27.md`
- Open gaps snapshot: `docs/qa/evidence/2026-05-27-dod-open-gaps-audit/not-run-snapshot.txt`
- Owner checklist: `docs/qa/release-gate-owner-checklist-2026-05-27.md`
- Next-run commands: `docs/qa/next-run-command-sheet-2026-05-27.md`
- Fixed findings index: `docs/qa/fixed-findings-index-2026-05-27.md`
- Evidence index: `docs/qa/evidence-index-2026-05-27.md`
