# TrainIQ QA Status Summary - 2026-05-27

Current status: `PARTIAL`

Release-ready by full DoD: `NO`

## What is green

- Automated baseline passed after the touch-target fix:
  - `assembleDebug`
  - `testDebugUnitTest`
  - `lintDebug`
  - `connectedDebugAndroidTest`
- Current-build emulator smoke passed with no TrainIQ crash/ANR match.
- No open P0/P1/P2 bugs are known from executed checks.
- Four executed-loop findings were fixed and verified:
  - `QA-2026-05-27-001`: local data clear missed OpenAI encrypted key storage.
  - `QA-2026-05-27-002`: saved recipe delete could trigger ANR.
  - `QA-2026-05-27-003`: Room migration marker generation drifted behind v13.`r`n  - `QA-2026-05-27-004`: Coach/Settings controls hardened to explicit 48dp touch height.
- Runtime coverage exists for major paths including first-run Home, Settings/Health Connect rationale, Nutrition recipe create/use/edit/delete, Progress add/invalid/delete, Coach local goal advice, active workout log/finish/completion and cross-tab/lifecycle smoke.
- Source/unit/contract coverage was refreshed for AI, scanner/barcode, Health Connect policy, accessibility semantics, dynamic color, adaptive layout and performance tooling buildability. Physical-device assistive-tech state was captured on `SM-S931B`; accessibility services are disabled, so TalkBack/Switch traversal remains open.

## Why Done is still open

The remaining gaps require runtime or owner evidence that was intentionally not produced in the safe run; the physical-device macrobenchmark gate is now closed as PASS:

- Physical-device macrobenchmark timing trace.
- TalkBack/Switch Access traversal; physical device currently has accessibility disabled (`accessibility_enabled=0`, `enabled_accessibility_services=null`).
- Health Connect partial grant, revoke-while-open and background-read runtime matrix.
- Privacy/security real-key save/readback/signoff.
- Live AI/provider flows.
- Real camera/scanner return through app navigation.
- Manual deep-runtime UX audits for active-workout edits, Exercise History, long forms, smart-scale valid result, full touch-target certification, overlap/clipping, modal focus containment and focus order.

## Release decision

Recommended decision: `DEFER RELEASE READINESS`

Rationale: the executed QA loops found and fixed P0/P1 issues and the automated baseline is green after the post-touch-target rerun, but the full Definition of Done explicitly requires runtime/owner gates that remain `NOT RUN` without owner-approved defer.

## Reviewer map

- Full ledger: `docs/qa/full-app-qa-run-2026-05-27.md`
- Open gaps snapshot: `docs/qa/evidence/2026-05-27-dod-open-gaps-audit/not-run-snapshot.txt`
- Owner checklist: `docs/qa/release-gate-owner-checklist-2026-05-27.md`
- Next-run commands: `docs/qa/next-run-command-sheet-2026-05-27.md`
- Fixed findings index: `docs/qa/fixed-findings-index-2026-05-27.md`
- Evidence index: `docs/qa/evidence-index-2026-05-27.md`




