# BYOK vs Production Gateway Risk Register

Last updated: 2026-05-08

Status: security/product risk register.

| Risk | BYOK/local-client state | Gateway/OAuth state | Local mitigation | Decision gate |
|---|---|---|---|---|
| User API key exposure | Key exists on device, encrypted with Android Keystore-backed storage | No long-lived Gemini key on device if gateway holds secret | Keystore storage, no URL key, no key logging found | Choose BYOK vs gateway |
| Abuse/quota control | Mostly delegated to user's Gemini account | Centralized quotas possible | Bounded client retry | Backend quota design required |
| Billing ownership | User may bear Gemini costs | Product/backend may bear or meter costs | Settings cost warning | Product pricing decision |
| Sensitive prompt logging | Client does not intentionally log prompts/keys | Backend must avoid logging sensitive prompts | Local no-log scan and telemetry guards | Backend observability policy |
| Data Safety complexity | Third-party Gemini sharing from client | TrainIQ backend processing may add collection/sharing categories | Data Safety matrix and policy draft | Legal/privacy review |
| Account deletion/export | No account system locally | Required if account/OAuth is introduced | Local data clear path | Account lifecycle design |
| Offline/manual fallback | Local manual fallback works | Must preserve gateway-unavailable fallback | Existing local fallback states | Client/server contract |
| Model migration | Client model constant update needed | Backend can centralize model routing | Centralized `GEMINI_FLASH_MODEL` | Product/backend model policy |
| Regional/compliance controls | Limited to Gemini/user key behavior | Backend can enforce region/retention if designed | Disclosure only | Legal/backend architecture |

## Release Guardrail

Do not claim production AI readiness until `docs/architecture/production-ai-boundary-decision-gate.md` is signed off by product/backend/security/legal owners.

## Closure Control

Status: `OPEN`

Owner role: security owner + product owner + backend owner

Decision required: accept the selected AI boundary risk posture or require architecture changes.

Allowed options:

- Accept BYOK/local-client risk for this release.
- Require server-side gateway.
- Require OAuth/account-mediated gateway.
- Approve hybrid mode.
- Block release or scope AI out.

Required evidence:

- Signed production AI boundary decision.
- Threat/risk notes for selected mode.
- Data Safety/privacy updates for selected mode.

Exact completion criteria:

- Risk table reflects selected mode.
- Unaccepted risks have owner-approved mitigations or release exceptions.
- `AI-001` in `docs/release/owner-action-tracker.md` is `APPROVED` or explicitly scoped out.

Release impact if not completed: release remains `BLOCKED` for production AI readiness.
