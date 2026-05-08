# Data Safety Change Impact Matrix

Last updated: 2026-05-08

Status: release guardrail. All rows require `RECHECK_BEFORE_PLAY_SUBMISSION`.

| Scenario | Data Safety impact | Likely answer changes | Privacy policy impact | Required evidence | Gate marker |
|---|---|---|---|---|---|
| Production telemetry remains disabled | Current worksheet may remain close to local state | Diagnostics/performance upload may remain no/none except local diagnostics | Policy can describe telemetry as disabled unless enabled | Final production `BuildConfig`, no endpoint/token, telemetry off screenshot | CURRENT_LOCAL_STATE_ONLY, OWNER_LEGAL_CONFIRMATION_REQUIRED |
| Production telemetry is enabled | Diagnostics/performance data may be collected/shared | Technical events, performance summaries, endpoint/processor, retention, opt-out must be declared | Add processor, purpose, retention, opt-out, payload categories | Telemetry payload sample, endpoint owner, retention policy | OWNER_LEGAL_CONFIRMATION_REQUIRED, RECHECK_BEFORE_PLAY_SUBMISSION |
| Crash/analytics SDK added | SDK may collect crash logs, device IDs, usage, diagnostics | Collection/sharing may change for device/app activity/diagnostics/identifiers | Add SDK provider, data types, retention, opt-out where applicable | Final dependency tree, SDK privacy docs, payload sample | OWNER_LEGAL_CONFIRMATION_REQUIRED, RECHECK_BEFORE_PLAY_SUBMISSION |
| Crash/analytics SDK removed | May reduce collection/sharing | Data Safety answers may remove SDK-specific categories | Policy should remove obsolete SDK references | Dependency tree and production build evidence | OWNER_LEGAL_CONFIRMATION_REQUIRED, RECHECK_BEFORE_PLAY_SUBMISSION |
| Backend sync introduced | Profile, workout, nutrition, progress, Health Connect, or AI data may be shared with TrainIQ backend | Local-only answers become collected/shared/processed server-side | Add backend controller/processor, retention, deletion/export, security | API inventory, auth model, server retention/deletion docs | OWNER_LEGAL_CONFIRMATION_REQUIRED, RECHECK_BEFORE_PLAY_SUBMISSION |
| Production AI gateway introduced | AI prompts/context/photos may route through TrainIQ backend | Third-party sharing and server processing answers change | Add gateway data flow, retention, model provider, deletion, abuse controls | AI gateway design, logs/redaction policy, deletion process | OWNER_LEGAL_CONFIRMATION_REQUIRED, RECHECK_BEFORE_PLAY_SUBMISSION |
| OAuth/account login introduced | Account identifiers and auth data become collected | Account, identifiers, contact info, entitlement/subscription data may apply | Add account lifecycle, deletion/export, provider details | Auth architecture, provider docs, account deletion flow | OWNER_LEGAL_CONFIRMATION_REQUIRED, RECHECK_BEFORE_PLAY_SUBMISSION |
| Local BYOK remains only AI mode | User key and explicit Gemini requests remain main AI privacy surface | Gemini API key and third-party Gemini sharing remain user-triggered | Policy must clearly describe BYOK, user-triggered prompts/context/photos | BYOK UI disclosure, GeminiApi header auth, no-key fallback tests | CURRENT_LOCAL_STATE_ONLY, OWNER_LEGAL_CONFIRMATION_REQUIRED |

## Release Guardrail

If any row changes after this document is signed off, release must pause until:

- `docs/release/play-console-data-safety-worksheet.md` is updated.
- `docs/release/privacy-policy-draft.md` or published policy text is updated.
- The Play Console owner confirms the final answers.

