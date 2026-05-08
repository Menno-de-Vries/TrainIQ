# Production AI Boundary Checklist

Last updated: 2026-05-08

Status: backend/product handoff checklist.

## Product Decisions

- [ ] PRODUCT_CONFIRMATION_REQUIRED: local BYOK only, server-side gateway, OAuth gateway, or short-lived token model.
- [ ] PRODUCT_CONFIRMATION_REQUIRED: whether TrainIQ requires accounts.
- [ ] PRODUCT_CONFIRMATION_REQUIRED: whether AI is free, quota-limited, subscription-based, or user-key-only.
- [ ] PRODUCT_CONFIRMATION_REQUIRED: retention period for AI prompts/responses.
- [ ] PRODUCT_CONFIRMATION_REQUIRED: user deletion/export behavior for server-side AI data.

## Backend Requirements If Gateway Is Chosen

- [ ] Authenticated API.
- [ ] Authorization per user/account.
- [ ] Secrets stored outside client and outside source control.
- [ ] Quotas/rate limits per user/device/account.
- [ ] Abuse prevention.
- [ ] Request validation and size limits.
- [ ] Sensitive payload redaction in logs.
- [ ] Structured response validation.
- [ ] Fallback/error contract matching current client behavior.
- [ ] Monitoring and incident response.
- [ ] Data retention/deletion implementation.

## Android Client Migration Hooks

- [ ] Keep `AiUsageGate` as the central readiness/key/access boundary.
- [ ] Add gateway mode without changing feature screen contracts.
- [ ] Preserve missing-key/gateway-unavailable local fallback states.
- [ ] Keep model/schema configuration centralized.
- [ ] Add tests proving API keys are not logged or sent in URLs.
- [ ] Update Settings copy based on final production mode.
- [ ] Update Data Safety and privacy policy based on final production mode.

## Release Blocker

Production AI readiness is blocked until the product/backend owner chooses and implements the production boundary.

