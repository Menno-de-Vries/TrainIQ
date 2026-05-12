# Data Safety Decision Gates

Last updated: 2026-05-12

Status: release guardrail. These gates prevent local evidence from being treated as final Play Console or legal approval.

Current release status: `BLOCKED`. Use these gates with `docs/release/owner-decision-packet-2026-05-10.md` (content refreshed on 2026-05-12), `docs/release/owner-action-tracker.md`, and `docs/release/play-console-owner-checklist.md`. Local engineering evidence does not close these gates without owner/legal approval.

## Gate DS-01: Production Telemetry

Decision owner: Product owner + legal/privacy owner + release owner

Current local state: `CURRENT_LOCAL_STATE_ONLY`

- Telemetry is opt-in in app UI.
- Build defaults set telemetry upload off and no static telemetry API token is embedded.
- Local diagnostics may still be kept for QA.

Release decision required:

- Is production telemetry upload enabled?
- What endpoint, processor, retention period, and region apply?
- Which event names and payload fields are uploaded?

Required evidence:

- Final `BuildConfig` values for production.
- Telemetry payload sample.
- Privacy/legal approval.
- Updated `docs/release/play-console-data-safety-worksheet.md`.

Gate marker: `OWNER_LEGAL_CONFIRMATION_REQUIRED`, `RECHECK_BEFORE_PLAY_SUBMISSION`

## Gate DS-02: Crash/Analytics SDKs

Decision owner: Engineering owner + legal/privacy owner

Current local state: `CURRENT_LOCAL_STATE_ONLY`

- No Firebase Crashlytics, Firebase Analytics, ads SDK, or third-party analytics dependency was found in the current Gradle scan.
- AndroidX metrics-performance is present for local performance/session diagnostics.

Release decision required:

- Are any crash/analytics SDKs added, removed, or enabled for release?
- Do SDKs collect identifiers, diagnostics, device data, or usage events?

Required evidence:

- Final dependency tree.
- SDK data collection disclosures.
- Data Safety worksheet update.

Gate marker: `OWNER_LEGAL_CONFIRMATION_REQUIRED`, `RECHECK_BEFORE_PLAY_SUBMISSION`

## Gate DS-03: Backend Sync

Decision owner: Product owner + backend owner + legal/privacy owner

Current local state: `CURRENT_LOCAL_STATE_ONLY`

- No account/auth backend sync was found in local app code.
- Gemini calls go directly to Google Gemini only for explicit AI features in current BYOK mode.

Release decision required:

- Will profile, workouts, nutrition, progress, Health Connect cache, or AI context sync to TrainIQ servers?
- What retention, deletion, export, and breach-response processes apply?

Required evidence:

- Backend API inventory.
- Auth/account model.
- Server-side retention/deletion design.
- Updated privacy policy and Data Safety answers.

Gate marker: `OWNER_LEGAL_CONFIRMATION_REQUIRED`, `RECHECK_BEFORE_PLAY_SUBMISSION`

## Gate DS-04: Production AI Boundary

Decision owner: Product owner + backend owner + security owner + legal/privacy owner

Current local state: `CURRENT_LOCAL_STATE_ONLY`

- Current AI mode is local-client BYOK.
- Requests are user-triggered and use the user-provided Gemini key.
- The blueprint says production should prefer a server-side Gemini boundary or OAuth-backed access controls.

Release decision required:

- Keep BYOK only, add server gateway, add OAuth/account-mediated gateway, or support hybrid mode?
- Does production AI receive health, nutrition, training, photos, or profile data server-side?

Required evidence:

- Completed `docs/architecture/production-ai-boundary-decision-gate.md`.
- Updated `docs/security/byok-vs-production-gateway-risk-register.md`.
- Updated Data Safety worksheet and published privacy policy.

Gate marker: `OWNER_LEGAL_CONFIRMATION_REQUIRED`, `RECHECK_BEFORE_PLAY_SUBMISSION`

## Gate DS-05: OAuth/Account Login

Decision owner: Product owner + security owner + legal/privacy owner

Current local state: `CURRENT_LOCAL_STATE_ONLY`

- No local account/auth system was found in the current implementation scan.

Release decision required:

- Will TrainIQ add sign-in, account identifiers, cloud profile, subscription, entitlement, or deletion/export flows?

Required evidence:

- Auth provider and account lifecycle documentation.
- Data deletion/export process.
- Updated Data Safety and privacy policy.

Gate marker: `OWNER_LEGAL_CONFIRMATION_REQUIRED`, `RECHECK_BEFORE_PLAY_SUBMISSION`

## Required Pre-Submission Procedure

Before Play submission:

1. Re-run dependency and manifest scan.
2. Re-run AI/network/storage scan.
3. Compare final production build config against this document.
4. Update the Data Safety worksheet.
5. Obtain owner/legal approval.
6. Archive the signed-off worksheet with the release candidate.

## Owner Handoff Control

Status: `OPEN`

Owner role:

- Product owner
- Legal/privacy owner
- Release owner
- Backend owner if backend sync, production AI gateway, or account login is introduced

Decision required:

- Confirm final production telemetry, crash/analytics SDK, backend sync, production AI, and account/auth posture.
- Confirm whether the current Play worksheet remains valid or must be changed.

Allowed options:

- Approve current local-only/BYOK/no-account/no-production-telemetry worksheet for submission.
- Approve with changes and update downstream docs before submission.
- Block release because production architecture or legal answers are not final.

Required evidence:

- Completed `docs/release/play-console-data-safety-worksheet.md`.
- Final production manifest/dependency/build-config scan.
- Published privacy policy URL and final policy text.
- Signed owner/legal approval.

Exact completion criteria:

- Every gate DS-01 through DS-05 has a selected answer.
- Every `OWNER_LEGAL_CONFIRMATION_REQUIRED` item is either resolved or explicitly release-scoped out by the legal/privacy owner.
- `docs/release/play-console-data-safety-worksheet.md`, `docs/release/privacy-policy-draft.md` or published policy text, and `docs/release/final-release-risk-register.md` are updated to match.
- `LEGAL-001` in `docs/release/owner-action-tracker.md` is `APPROVED`.

Downstream docs that must be updated:

- `docs/release/play-console-data-safety-worksheet.md`
- `docs/release/privacy-policy-draft.md`
- `docs/release/final-release-risk-register.md`
- `docs/release/owner-action-tracker.md`
- AI/security docs if AI mode changes

Release impact if not completed:

- Play Console/Data Safety submission remains blocked.
- Release status remains `BLOCKED`.

Signoff:

- Owner:
- Decision:
- Date:
- Status: `OPEN | IN_REVIEW | APPROVED | BLOCKED`
