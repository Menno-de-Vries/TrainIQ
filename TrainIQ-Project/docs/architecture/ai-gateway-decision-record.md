# AI Gateway Decision Record

Last updated: 2026-05-08

Status: decision package. No production gateway decision has been made locally.

## Context

TrainIQ currently supports local/dev Bring Your Own Key AI usage from the Android client for Gemini and OpenAI. The target blueprint allows client-side BYOK for local/dev MVP only and recommends a production server-side AI boundary or OAuth-backed access controls.

Local implementation evidence:

- Stable Gemini model: `gemini-2.5-flash`.
- Stable OpenAI model: `gpt-4.1-mini` via `BuildConfig.OPENAI_MODEL`.
- Provider routing: `AiProviderRouter` tries the selected preferred provider first and can fall back to the other configured provider on transient failures before local fallback.
- API key header: `x-goog-api-key` in `GeminiApi.kt`.
- OpenAI auth header: `Authorization: Bearer ...` in `OpenAiApi.kt`.
- Key storage: Android Keystore-backed encrypted SharedPreferences in `AndroidKeystoreGeminiKeyStore.kt` and `AndroidKeystoreOpenAiKeyStore.kt`.
- Missing-key graceful fallback: `AiUsageGate.kt`, `AiServices.kt`, `RoutineGeneratorService.kt`.
- No hardcoded production Gemini/OpenAI secret found in app code scan.

## Option 1: Local BYOK Only

Description: Users provide their own Gemini and/or OpenAI API key, stored locally. Client calls the selected provider directly.

Benefits:

- Smallest backend scope.
- Works offline/manual when missing key.
- Clear local/dev behavior.

Risks:

- User manages billing/key lifecycle.
- Harder to enforce product quotas or abuse controls.
- Not ideal for production UX.
- Privacy/Data Safety wording must clearly disclose third-party AI provider sharing.

Required work:

- Keep disclosures current.
- Keep key storage and no-log tests.
- Confirm Google Gemini terms and privacy behavior.

Migration path:

- Keep `AiUsageGate` as the decision boundary.
- Add alternate gateway-backed key provider without changing feature screens.

## Option 2: Server-Side AI Gateway

Description: Android app sends structured requests to TrainIQ backend; backend holds Gemini credentials and calls Gemini.

Benefits:

- Centralized quota, abuse, logging, model routing, and prompt policy.
- No user API key required.
- Easier model migration.

Risks:

- Backend stores/processes sensitive health/nutrition/training context.
- Requires authentication, authorization, monitoring, retention policy, and incident response.
- More Play/Data Safety/privacy obligations.

Required backend work:

- Authenticated API.
- Per-user authorization.
- Secrets management.
- Request validation and redaction.
- Rate limiting and abuse controls.
- Privacy retention and deletion implementation.
- Observability without sensitive payload logging.

## Option 3: OAuth/User-Account Mediated Gateway

Description: Users sign in; backend uses user identity and entitlements to mediate AI access.

Benefits:

- Supports subscriptions/quotas.
- Enables cross-device state if product chooses.
- Stronger abuse and entitlement controls.

Risks:

- Account lifecycle, deletion, support, and compliance scope.
- More Data Safety categories.
- Requires secure auth implementation and backend operations.

Required product decisions:

- Is TrainIQ account-based?
- Which identity provider?
- What deletion/export requirements?
- Is AI included, metered, or subscription-based?

## Option 4: Short-Lived Client Token

Description: Backend issues short-lived scoped tokens for client AI calls or gateway calls.

Benefits:

- Reduces long-lived secrets on device.
- Allows quota and entitlement checks.
- Can preserve some client-side request flow.

Risks:

- Still requires backend auth and token issuance.
- Scope and revocation must be designed carefully.
- May not remove all sensitive data sharing from client.

Required work:

- Token minting endpoint.
- Token scope/audience/expiry design.
- Client token refresh and failure states.
- Abuse controls and monitoring.

## Recommendation For Next Product Decision

Use local BYOK only for internal/local/dev builds until product decides whether TrainIQ will have accounts and a backend. For production consumer release, prefer server-side or OAuth-mediated gateway if AI is a core feature.

PRODUCT_CONFIRMATION_REQUIRED: choose production AI boundary, account model, billing/quota model, data retention, and deletion behavior.
