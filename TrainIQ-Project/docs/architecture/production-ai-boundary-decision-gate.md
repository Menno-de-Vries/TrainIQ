# Production AI Boundary Decision Gate

Last updated: 2026-05-08

Status: product/backend/security decision gate. No backend or OAuth implementation is approved in this local workspace.

## Current State

TrainIQ currently uses BYOK/local-client Gemini calls:

- User stores a Gemini API key locally.
- Key storage is Android Keystore-backed.
- API key is sent to Gemini in `x-goog-api-key` header for explicit user-triggered AI actions.
- Missing key or disabled AI falls back to manual/local behavior.
- No TrainIQ backend or OAuth account flow was found in local app code.

## Decision Options

| Option | Description | Release implication | Required signoff |
|---|---|---|---|
| Keep BYOK only | User supplies Gemini API key; Android app calls Gemini directly | Must disclose third-party Gemini sharing and user-managed billing/key risk | Product, legal/privacy, security |
| Server-side AI gateway | App sends AI requests to TrainIQ backend; backend calls Gemini | Requires backend auth, secrets, quotas, retention/deletion, server privacy controls | Product, backend, security, legal/privacy |
| OAuth/account-mediated gateway | User signs in; backend mediates AI access by identity/entitlement | Adds account lifecycle, identifiers, deletion/export, entitlement support | Product, backend, security, legal/privacy |
| Hybrid BYOK + gateway | Users or builds can use BYOK or TrainIQ gateway | Must make mode clear in UI/Data Safety/privacy policy; support both failure modes | Product, backend, security, legal/privacy |

## Locally Mitigated

- No hardcoded production Gemini key found.
- Header auth, not URL query.
- Encrypted local key storage with readback verification.
- Fail-closed legacy key migration.
- Bounded AI retry/fallback.
- Settings disclosure for explicit AI data flow.

## Not Solvable Without Backend/Product Architecture

- Central quota and abuse control.
- Server-side secrets management.
- OAuth/account entitlement.
- Server-side retention/deletion/export.
- Production AI cost and billing policy.
- Production monitoring without sensitive prompt logging.

## Gate To Close

This gate closes only when:

1. Product chooses an option.
2. Backend/security owners approve architecture if server/gateway/OAuth is selected.
3. Legal/privacy owner updates Data Safety and privacy policy.
4. Android client copy and failure states match the final mode.
5. Evidence is archived in the release package.

## Owner Handoff Control

Status: `OPEN`

Owner role:

- Product owner
- Backend owner
- Security owner
- Legal/privacy owner
- Android owner if client changes are required

Decision required:

- Choose the production AI mode for the release candidate.
- Decide whether AI is in release scope, BYOK-only, gateway-backed, OAuth/account-mediated, or hybrid.

Allowed options:

- Keep BYOK/local-client only for this release.
- Add server-side AI gateway.
- Add OAuth/account-mediated gateway.
- Ship hybrid BYOK + gateway.
- Scope production AI out of release with documented impact.

Required evidence:

- Selected option and rationale.
- Backend/security design if gateway/OAuth/hybrid is selected.
- Updated `docs/security/byok-vs-production-gateway-risk-register.md`.
- Updated Data Safety worksheet and privacy policy.
- Android implementation evidence if client behavior changes.

Exact completion criteria:

- One allowed option is selected and signed off.
- Required backend/security/legal docs are complete for the selected option.
- Privacy policy and Play worksheet match the selected option.
- `AI-001` in `docs/release/owner-action-tracker.md` is `APPROVED`, or product/security explicitly scopes AI out with documented release implications.

Downstream docs that must be updated:

- `docs/security/byok-vs-production-gateway-risk-register.md`
- `docs/release/play-console-data-safety-worksheet.md`
- `docs/release/privacy-policy-draft.md`
- `docs/release/data-safety-decision-gates.md`
- `docs/release/final-release-risk-register.md`
- `docs/release/owner-action-tracker.md`

Release impact if not completed:

- Production AI gateway/OAuth readiness remains blocked.
- Release remains `BLOCKED` unless AI is explicitly scoped out by product/security/legal with documented implications.

Signoff:

- Owner:
- Decision:
- Date:
- Status: `OPEN | IN_REVIEW | APPROVED | BLOCKED`
