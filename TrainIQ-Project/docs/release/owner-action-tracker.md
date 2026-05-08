# Owner Action Tracker

Last updated: 2026-05-08

Release status: `BLOCKED`

This tracker is the release-control source for owner-only approvals. Codex/local engineering must not mark any row `APPROVED` without the required owner evidence.

| ID | Owner | Action | Blocking docs | Evidence required | Status | Release impact |
|---|---|---|---|---|---|---|
| LEGAL-001 | Product owner, legal/privacy owner, release owner | Complete Data Safety decision gates and Play worksheet before submission | `docs/release/data-safety-decision-gates.md`, `docs/release/play-console-data-safety-worksheet.md`, `docs/release/privacy-policy-draft.md` | Final production build config/dependency scan, completed Play worksheet, published privacy policy URL, signed legal/owner approval | OPEN | Blocks Play submission and release |
| PERF-001 | Product owner, Android owner, release owner | Set numeric performance thresholds and run device-lab plan | `docs/qa/performance-threshold-decision-record.md`, `docs/qa/device-lab-performance-readiness.md`, `docs/qa/performance-evidence-template.md` | Approved thresholds, physical device matrix, macrobenchmark/profileable or release results, logcat crash/ANR scan | OPEN | Blocks performance certification and release |
| A11Y-001 | Accessibility owner, manual QA tester, release owner | Complete human TalkBack/Switch Access signoff | `docs/qa/talkback-switch-access-test-script.md`, `docs/qa/human-assistive-tech-qa-signoff.md`, `docs/qa/accessibility-certification-boundary.md` | Completed flow table, recordings/screenshots, tester notes, device/Android/build/font/theme evidence, accessibility owner signoff | OPEN | Blocks accessibility certification and release |
| AI-001 | Product owner, backend owner, security owner, legal/privacy owner | Choose production AI mode and update privacy/Data Safety docs | `docs/architecture/production-ai-boundary-decision-gate.md`, `docs/security/byok-vs-production-gateway-risk-register.md`, `docs/security/production-ai-boundary-checklist.md` | Signed AI mode decision, backend/security design if selected, risk acceptance or mitigations, updated Data Safety worksheet and privacy policy | OPEN | Blocks production AI readiness; release remains blocked unless AI is explicitly scoped out by product/security/legal |

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

