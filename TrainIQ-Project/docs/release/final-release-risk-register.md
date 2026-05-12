# Final Release Risk Register

Last updated: 2026-05-12

Status: release guardrail. These risks are not closed by local implementation alone.

Release status: `BLOCKED`

Release remains blocked until `docs/release/owner-action-tracker.md` shows:

- `LEGAL-001`: `APPROVED`
- `PERF-001`: `APPROVED`
- `A11Y-001`: `APPROVED`
- `AI-001`: `APPROVED`, or explicitly scoped out by product/security/legal with documented release implications.

| Risk | Current local status | Why unresolved | Owner needed | Evidence required to close | Release impact | Next safe action |
|---|---|---|---|---|---|---|
| Legal/Data Safety answers may change with telemetry/backend/AI/account decisions | Local worksheet and change matrix prepared | Final production services and legal interpretation are owner decisions | Product owner, legal/privacy owner, release owner | Signed Data Safety worksheet, published privacy policy URL, final production build config/dependency scan | Cannot submit Play Console truthfully without owner/legal confirmation | Complete `docs/release/data-safety-decision-gates.md` before Play submission |
| Physical-device performance thresholds are undefined | SM-S931B profileable startup/navigation/active-workout logging evidence exists with empty crash/ANR slices | Blueprint requires owner-approved thresholds, approved device matrix, and broader repeated-flow memory evidence before certification | Product owner, Android owner, release owner | Approved numeric thresholds, device matrix, device-lab results, crash/ANR scan, owner signoff | Cannot certify performance for release | Fill `docs/qa/performance-threshold-decision-record.md` and run device-lab plan |
| Accessibility certification requires human assistive-tech QA | Automated/runtime support evidence expanded; 360x640/font-scale UIAutomator checks cover top-level screens, seeded active workout, scanner permission gates, Health Connect rationale, and Settings destructive dialogs with `NAF=0` | Spoken output, focus order, and Switch Access reachability require human testing | Accessibility owner/manual QA tester | Completed `docs/qa/human-assistive-tech-qa-signoff.md`, recordings, screenshots, tester notes | Cannot claim accessibility certification | Execute TalkBack/Switch Access script on target device matrix |
| Production AI boundary remains BYOK/local-client only | BYOK hardened locally; AI decision gate prepared | Server/OAuth/gateway requires product/backend architecture | Product owner, backend owner, security owner, legal/privacy owner | Signed AI boundary decision, backend architecture if selected, updated privacy/Data Safety docs | Cannot claim production gateway/OAuth readiness | Choose one option in `docs/architecture/production-ai-boundary-decision-gate.md` |
| Health Connect edge-state evidence is incomplete | No-permission baseline, app-owned rationale, manage-access launch, manifest/package visibility, and background-read declaration guards are recorded | Provider-missing/update, partial-grant, revoke-while-open, and background-read granted/unavailable states require a safe disposable permission profile/device | Android owner, release owner, privacy owner if scope changes | UI dumps, permission state before/after, logcat crash buffers, and notes from a safe test profile/device | Cannot claim complete Health Connect runtime readiness | Run `docs/qa/health-connect-runtime-matrix-2026-05-10.md` edge-state plan only on a safe profile/device |
| Scanner capture evidence is incomplete | Permission gate, font-scale, barcode preview, and AI scanner permission-gate rotation evidence are recorded without unsafe data/camera use | Real barcode recognition and AI photo capture require approved camera setup and may capture real-world data | Android owner, manual QA tester, privacy owner if captured data is retained | Safe camera/barcode setup notes, UI dumps/screenshots, logcat crash scan, permission restoration evidence | Cannot claim full scanner readiness | Run real capture/barcode checks only on an approved safe test setup |
| Release signing/versioning is unresolved | Local signing-readiness task runs but reports unsigned release artifacts because signing is not configured | Uploadable artifacts require signing ownership, keystore handling, versioning strategy, or dated release exception | Release owner | Signing policy, versioning decision, artifact provenance, or written exception | Cannot upload or certify release artifacts | Configure signing/versioning or approve a dated exception before release artifacts |

## Accidental Claim Prevention

Release notes, Play Console forms, privacy policy, and QA summaries must not state these risks are complete until the evidence column is satisfied.

## Closure Criteria

| Owner action | Required status | Closure evidence |
|---|---|---|
| LEGAL-001 | APPROVED | Completed Data Safety gates, final Play worksheet, published privacy policy URL, legal/privacy owner signoff |
| PERF-001 | APPROVED | Numeric thresholds, physical device matrix, device-lab results, Android/product owner signoff |
| A11Y-001 | APPROVED | Human TalkBack/Switch Access signoff, recordings/screenshots/tester notes, accessibility owner signoff |
| AI-001 | APPROVED or explicitly scoped out | Signed AI mode decision, security/product/backend/legal signoff, updated privacy/Data Safety docs or documented release exclusion |
| Health Connect runtime matrix | APPROVED/PASS | Safe-profile provider/permission/background evidence with UI dumps and crash logs |
| Scanner capture evidence | APPROVED/PASS or explicitly scoped out | Safe real-camera/barcode capture evidence or documented release exclusion |
| Release signing/versioning | APPROVED | Signing ownership, keystore policy, versioning decision, artifact provenance or dated exception |

If any owner action is `OPEN`, `IN_REVIEW`, or `BLOCKED`, final release status must remain `BLOCKED`.
