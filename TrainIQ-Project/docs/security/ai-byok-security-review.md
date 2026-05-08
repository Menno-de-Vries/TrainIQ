# AI BYOK Security Review

Last updated: 2026-05-08

Status: local client review only.

## Findings

| Area | Local status | Evidence | Remaining risk |
|---|---|---|---|
| Hardcoded secrets | No hardcoded production Gemini API key found | `app/build.gradle.kts`, `GeminiApi.kt`, code scan | OWNER_CONFIRMATION_REQUIRED for CI/release secrets |
| API key transport | Header-based `x-goog-api-key`, not URL query | `GeminiApi.kt`, `GeminiApiContractTest.kt` | Gemini endpoint/provider policy confirmation |
| API key storage | Android Keystore-backed AES/GCM encrypted storage | `AndroidKeystoreGeminiKeyStore.kt` | Device compromise/root remains out of scope |
| Legacy key migration | Fail-closed if encrypted write/readback fails | `GeminiKeyMigration.kt`, `GeminiKeyMigrationTest.kt` | Existing users need migration QA |
| Missing key behavior | AI falls back locally or disables actions | `AiUsageGate.kt`, `AiServices.kt`, `NutritionScreen.kt`, `CameraScannerScreen.kt` | Manual UX copy review |
| Key logging | No API key logging found in production source scan | `rg Log/apiKey` scan | Keep static scan in release checklist |
| Model ID | Explicit `gemini-2.5-flash` | `AiSupport.kt` | Monitor Gemini deprecations |
| Retry behavior | Bounded shared retry for Gemini calls | `AiSupport.kt`, `AiServices.kt`, `RoutineGeneratorService.kt` | Product latency thresholds needed |
| Image upload bounds | Meal images capped/compressed before Gemini | `AiServices.kt`, `AiServicesTest.kt` | Device-lab large image QA |
| Telemetry | Opt-in, no static API token, privacy guard tests | `TelemetryExport.kt`, `TelemetryExportPipelineTest.kt` | Endpoint/processor confirmation |

## User Disclosure

Settings now states that explicit AI actions send the needed prompt, context, and selected photo when relevant to Google Gemini using the locally stored API key.

Evidence: `app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`

## Required Release Checks

- [ ] Confirm production build does not inject a hardcoded Gemini API key.
- [ ] Confirm logging level remains non-sensitive.
- [ ] Confirm privacy policy describes Gemini requests and BYOK.
- [ ] Confirm production AI boundary decision.
- [ ] Confirm telemetry endpoint does not receive health data, notes, API keys, or photos.

