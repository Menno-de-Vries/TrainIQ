# Final Release Risk Register

Last updated: 2026-05-08

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
| Physical-device performance thresholds are undefined | Macrobenchmark/profileable readiness docs prepared | Blueprint requires thresholds but repo does not define numbers | Product owner, Android owner | Approved numeric thresholds and physical-device macrobenchmark evidence | Cannot certify performance for release | Fill `docs/qa/performance-threshold-decision-record.md` and run device-lab plan |
| Accessibility certification requires human assistive-tech QA | Manual plan and signoff template prepared | Spoken output, focus order, and Switch Access reachability require human testing | Accessibility owner/manual QA tester | Completed `docs/qa/human-assistive-tech-qa-signoff.md`, recordings, screenshots, tester notes | Cannot claim accessibility certification | Execute TalkBack/Switch Access script on target device matrix |
| Production AI boundary remains BYOK/local-client only | BYOK hardened locally; AI decision gate prepared | Server/OAuth/gateway requires product/backend architecture | Product owner, backend owner, security owner, legal/privacy owner | Signed AI boundary decision, backend architecture if selected, updated privacy/Data Safety docs | Cannot claim production gateway/OAuth readiness | Choose one option in `docs/architecture/production-ai-boundary-decision-gate.md` |

## Accidental Claim Prevention

Release notes, Play Console forms, privacy policy, and QA summaries must not state these risks are complete until the evidence column is satisfied.

## Closure Criteria

| Owner action | Required status | Closure evidence |
|---|---|---|
| LEGAL-001 | APPROVED | Completed Data Safety gates, final Play worksheet, published privacy policy URL, legal/privacy owner signoff |
| PERF-001 | APPROVED | Numeric thresholds, physical device matrix, device-lab results, Android/product owner signoff |
| A11Y-001 | APPROVED | Human TalkBack/Switch Access signoff, recordings/screenshots/tester notes, accessibility owner signoff |
| AI-001 | APPROVED or explicitly scoped out | Signed AI mode decision, security/product/backend/legal signoff, updated privacy/Data Safety docs or documented release exclusion |

If any owner action is `OPEN`, `IN_REVIEW`, or `BLOCKED`, final release status must remain `BLOCKED`.
