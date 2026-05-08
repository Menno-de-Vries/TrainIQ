# TrainIQ Play, Privacy, and Release Evidence

Last updated: 2026-05-08

This file is a local evidence pack. It does not claim Play Console submission, legal approval, or published policy verification.

## Local Data Inventory

| Data area | Local storage / path | Purpose | Sharing / upload status |
|---|---|---|---|
| Profile and goals | Room entities via `app/src/main/java/com/trainiq/core/database/Entities.kt` | Coaching, nutrition targets, dashboard context | Local only unless future backend is added |
| Workouts, routines, exercises, sets | Room entities and repositories | Training log, active workout, history, progress | Local only |
| Nutrition, foods, recipes, meal logs | Room entities and nutrition screens | Meal logging, macros, reusable products/recipes | Local only except user-triggered Gemini meal scan request |
| Progress measurements | Room entities and progress screen | Body metrics and trend display | Local only |
| Health Connect cache and sync tokens | DataStore via `UserPreferencesRepository` | Incremental sync, recent health metrics, token validity | Local only |
| Gemini API key | Android Keystore-backed encrypted SharedPreferences via `AndroidKeystoreGeminiKeyStore` | BYOK AI calls | Stored locally; sent only as `x-goog-api-key` header to Gemini when user-triggered AI is enabled |
| Telemetry preference | DataStore | Opt-in state | Default off |
| Technical telemetry queue | In-memory queue in `TelemetryExport.kt` | Privacy-safe diagnostics when enabled | Upload disabled by default; no static API token in `BuildConfig` |

## Health Connect Declaration Inputs

Requested permissions declared locally:

- `android.permission.health.READ_STEPS`
- `android.permission.health.READ_HEART_RATE`
- `android.permission.health.READ_SLEEP`
- `android.permission.health.READ_ACTIVE_CALORIES_BURNED`
- `android.permission.health.READ_WEIGHT`
- `android.permission.health.READ_EXERCISE`
- `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`

Local implementation evidence:

- Availability and provider handling: `app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`
- Rationale/onboarding activities: `app/src/main/AndroidManifest.xml`
- Background gating: `app/src/main/java/com/trainiq/core/health/HealthConnectBackgroundSyncWorker.kt`
- Partial permission tests: `app/src/test/java/com/trainiq/data/datasource/HealthConnectPermissionPolicyTest.kt`

Owner action still required:

- Confirm each requested Health Connect data type in Play Console Health Apps declaration.
- Confirm background read justification is accurate for release.
- Confirm Data Safety answers match the final published privacy policy.

## Data Safety Draft Inputs

Local defaults and evidence:

- No account system is implemented locally.
- No advertising identifier usage was found in local app code.
- AI is opt-in and requires a user-provided Gemini API key.
- Telemetry is opt-in and disabled by default.
- Health data is read from Health Connect only after rationale and system permission flow.
- Local destructive actions are disclosed in Settings and clear local app data without claiming Android Health Connect access revocation.

Data Safety answers require owner confirmation for:

- Whether telemetry endpoint will be enabled in a production build.
- Whether uploaded telemetry is considered diagnostics/performance data only.
- Whether any future backend receives profile, nutrition, workout, or Health Connect data.
- Whether the published privacy policy URL covers BYOK Gemini requests and Health Connect permissions.

## Privacy Policy Requirements

A published policy should cover:

- Health Connect data types requested and why.
- Local-only storage for profile, workouts, nutrition, progress, and Health Connect cache.
- User-triggered Gemini requests and BYOK behavior.
- API keys stored locally with Android Keystore-backed encryption.
- Telemetry opt-in, categories, endpoint owner, retention, and opt-out.
- Local data deletion behavior and Health Connect permission revocation path through Android settings.
- Contact channel for privacy requests.

## AI Boundary Evidence

Local client state:

- Stable model ID: `gemini-2.5-flash` in `app/src/main/java/com/trainiq/ai/services/AiSupport.kt`.
- Header auth: `x-goog-api-key` in `app/src/main/java/com/trainiq/data/remote/GeminiApi.kt`.
- Structured JSON schemas: `app/src/main/java/com/trainiq/ai/services/GeminiJsonSchemas.kt`.
- Shared bounded retry: `app/src/main/java/com/trainiq/ai/services/AiSupport.kt`.
- BYOK migration and storage: `app/src/main/java/com/trainiq/core/security/GeminiKeyMigration.kt` and `AndroidKeystoreGeminiKeyStore.kt`.

Production blocker:

- The blueprint recommends a server-side Gemini boundary or OAuth-backed access controls for production. That requires a product/backend decision and is not locally implementable in this Android-only workspace.

## Manual QA Still Required Before Release

- Play Console Health Apps declaration review.
- Data Safety form submission and comparison against published privacy policy.
- Full TalkBack pass for active workout, scanner permission/result states, Health Connect rationale, AI routine generation, and Settings destructive actions.
- Switch Access pass for the same flows.
- Physical-device profileable/release performance pass with p50/p95 thresholds for startup, top-level navigation, settings scroll, and active workout logging.

