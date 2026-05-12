# Owner Action Tracker

Last updated: 2026-05-12

Release status: `BLOCKED`

This tracker is the release-control source for owner-only approvals. Codex/local engineering must not mark any row `APPROVED` without the required owner evidence.

Current owner handoff packet: `docs/release/owner-decision-packet-2026-05-10.md`

| ID | Owner | Action | Blocking docs | Evidence required | Status | Release impact |
|---|---|---|---|---|---|---|
| LEGAL-001 | Product owner, legal/privacy owner, release owner | Complete Data Safety decision gates and Play worksheet before submission | `docs/release/data-safety-decision-gates.md`, `docs/release/play-console-data-safety-worksheet.md`, `docs/release/privacy-policy-draft.md` | Final production build config/dependency scan, completed Play worksheet, published privacy policy URL, signed legal/owner approval | OPEN | Blocks Play submission and release |
| PERF-001 | Product owner, Android owner, release owner | Set numeric performance thresholds and run device-lab plan | `docs/qa/performance-threshold-decision-record.md`, `docs/qa/device-lab-performance-readiness.md`, `docs/qa/performance-evidence-template.md` | Approved thresholds, physical device matrix, macrobenchmark/profileable or release results, logcat crash/ANR scan | OPEN | Blocks performance certification and release |
| A11Y-001 | Accessibility owner, manual QA tester, release owner | Complete human TalkBack/Switch Access signoff | `docs/qa/talkback-switch-access-test-script.md`, `docs/qa/human-assistive-tech-qa-signoff.md`, `docs/qa/accessibility-certification-boundary.md` | Completed flow table, recordings/screenshots, tester notes, device/Android/build/font/theme evidence, accessibility owner signoff | OPEN | Blocks accessibility certification and release |
| AI-001 | Product owner, backend owner, security owner, legal/privacy owner | Choose production AI mode and update privacy/Data Safety docs | `docs/architecture/production-ai-boundary-decision-gate.md`, `docs/security/byok-vs-production-gateway-risk-register.md`, `docs/security/production-ai-boundary-checklist.md` | Signed AI mode decision, backend/security design if selected, risk acceptance or mitigations, updated Data Safety worksheet and privacy policy | OPEN | Blocks production AI readiness; release remains blocked unless AI is explicitly scoped out by product/security/legal |

## Current Evidence Notes

- PERF-001 has partial local evidence: `docs/qa/performance-evidence-2026-05-11-sm-s931b-profileable.md` records a SM-S931B profileable macrobenchmark run with 3 tests, 0 failures, 0 errors; a targeted deterministic active-workout logging run with 1 test, 0 failures, 0 errors; and profileable launch/memory/crash capture with empty crash/ANR slices. PERF-001 remains `OPEN` because thresholds, device matrix approval, broader repeated-flow memory evidence, and owner signoff are still missing.
- A11Y-001 has expanded automated/runtime support evidence but remains `OPEN`: 360x640/font-scale UIAutomator evidence now covers top-level screens, progress, seeded active workout, scanner permission gates, Health Connect rationale, and Settings destructive dialogs with `NAF=0`; this does not replace human TalkBack/Switch Access signoff.
- Health Connect runtime evidence remains partial: the scripted no-permission baseline and rationale/manage-access paths are recorded, but provider-missing, partial-grant, revoke-while-open, and background-read granted/unavailable cases still require a safe disposable test profile/device.
- Scanner runtime evidence remains partial: camera permission gate and rotation evidence is recorded without granting camera access, but real barcode recognition and AI photo capture require an approved safe camera test setup.

## Status Rules

- `OPEN`: owner action has not started or no evidence has been attached.
- `IN_REVIEW`: owner evidence exists and is under review.
- `APPROVED`: owner has signed off and required downstream docs are updated.
- `BLOCKED`: owner cannot approve because a product/legal/backend/manual-QA prerequisite is missing.

## Cross-Document Consistency Rules

- Data Safety answers must be rechecked after telemetry, backend, AI gateway, or account/auth decisions.
- Performance thresholds must be approved before device-lab certification can pass.
- Accessibility certification remains blocked until human TalkBack/Switch Access testing is signed off.
- Production AI remains BYOK/local-client only until AI mode is chosen and implemented or explicitly scoped out.
- Privacy policy and Play worksheet must be updated after any AI, backend, telemetry, analytics, crash reporting, or account/auth change.

## Release Gate

Release remains `BLOCKED` until:

- `LEGAL-001` is `APPROVED`;
- `PERF-001` is `APPROVED`;
- `A11Y-001` is `APPROVED`;
- `AI-001` is `APPROVED`, or product/security/legal explicitly scope AI out and document release implications in this tracker and `docs/release/final-release-risk-register.md`.
