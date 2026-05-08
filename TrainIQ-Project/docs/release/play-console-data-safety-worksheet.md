# Play Console Data Safety Worksheet

Last updated: 2026-05-08

Status: local evidence worksheet only. This is not a Play Console submission and is not legal advice.

## Local App Evidence Summary

- Manifest permissions: `app/src/main/AndroidManifest.xml`
- Build/dependencies: `app/build.gradle.kts`, `gradle/libs.versions.toml`
- Health Connect implementation: `app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`
- AI/Gemini implementation: `app/src/main/java/com/trainiq/ai/services/`, `app/src/main/java/com/trainiq/data/remote/GeminiApi.kt`
- BYOK storage: `app/src/main/java/com/trainiq/core/security/AndroidKeystoreGeminiKeyStore.kt`
- Telemetry implementation: `app/src/main/java/com/trainiq/core/diagnostics/TelemetryExport.kt`
- Local deletion path: `app/src/main/java/com/trainiq/domain/usecase/UseCases.kt`, `app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`

## Data Safety Worksheet

| Data type | Collected | Shared | Purpose | Encrypted in transit | User deletion path | Evidence file/path | Owner/legal confirmation needed |
|---|---|---|---|---|---|---|---|
| Name/profile label | yes | no locally | Profile personalization and coaching context | n/a local only | Settings -> Lokale data wissen; profile reset | `Entities.kt`, `SettingsSection.kt`, `ClearAppDataUseCase` | OWNER_CONFIRMATION_REQUIRED for production backend status |
| Age, sex, height, weight, body fat, activity level, goal | yes | no locally | Macro targets, coaching, progress display | n/a local only | Settings -> Profiel verwijderen or Lokale data wissen | `DomainModels.kt`, `Entities.kt`, `SettingsSection.kt` | OWNER_CONFIRMATION_REQUIRED |
| Workout routines, sets, sessions, exercise history | yes | no locally | Training log, active workout, progress | n/a local only | Settings -> Lokale data wissen; per-item delete actions | `Entities.kt`, `WorkoutScreen.kt`, `TrainIqRepository.kt` | OWNER_CONFIRMATION_REQUIRED |
| Nutrition foods, recipes, meal logs, notes | yes | no locally except Gemini only when user triggers AI meal/routine/advice context | Nutrition tracking and reusable food/recipe logging | yes for Gemini HTTPS calls; unknown for any future backend | Settings -> Lokale data wissen; per-item delete actions | `NutritionScreen.kt`, `GeminiApi.kt`, `AppModule.kt` | OWNER_CONFIRMATION_REQUIRED |
| Meal photo selected for AI scan | yes when user captures/selects scan | yes to Google Gemini when AI scan is explicitly triggered | Meal analysis estimate | yes, HTTPS via Retrofit base URL | Local temp/image handling and Settings -> Lokale data wissen; verify camera cache behavior before release | `CameraScannerScreen.kt`, `AiServices.kt`, `GeminiApi.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect steps | yes if permission granted | no locally | Dashboard, recovery/activity context | n/a local only | Settings -> Lokale data wissen clears cache; revoke access in Android Health Connect | `AndroidManifest.xml`, `HealthConnectDataSource.kt`, `UserPreferencesRepository.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect heart rate | yes if permission granted | no locally | Recovery/activity context | n/a local only | Same as Health Connect above | `AndroidManifest.xml`, `HealthConnectDataSource.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect sleep | yes if permission granted | no locally | Recovery/activity context | n/a local only | Same as Health Connect above | `AndroidManifest.xml`, `HealthConnectDataSource.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect active calories | yes if permission granted | no locally | Energy balance context | n/a local only | Same as Health Connect above | `AndroidManifest.xml`, `HealthConnectDataSource.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect weight | yes if permission granted | no locally | Progress context | n/a local only | Same as Health Connect above | `AndroidManifest.xml`, `HealthConnectDataSource.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect exercise sessions | yes if permission granted | no locally | Training context | n/a local only | Same as Health Connect above | `AndroidManifest.xml`, `HealthConnectDataSource.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect sync tokens/cache metadata | yes | no | Incremental sync and cache freshness | n/a local only | Settings -> Lokale data wissen clears cache | `UserPreferencesRepository.kt` | OWNER_CONFIRMATION_REQUIRED |
| Gemini API key | yes, user-entered | yes to Google Gemini as request header during explicit AI calls | BYOK authentication | yes, HTTPS header; not URL query | Settings -> Sleutel verwijderen or Lokale data wissen | `AndroidKeystoreGeminiKeyStore.kt`, `GeminiApi.kt`, `SettingsSection.kt` | OWNER_CONFIRMATION_REQUIRED |
| Technical telemetry: screen names, tap targets, state names, startup/performance summaries | yes only if opt-in and build endpoint configured | unknown/yes if production endpoint configured | Diagnostics and performance monitoring | unknown until production telemetry endpoint is selected | Settings toggle; Lokale data wissen clears preference | `Telemetry.kt`, `TelemetryExport.kt`, `DiagnosticsTracker.kt` | OWNER_CONFIRMATION_REQUIRED |
| Crash/performance local diagnostics | yes locally | no by default | QA diagnostics | n/a local only | Settings -> Lokale data wissen where persisted preferences exist | `AndroidPerformanceSessionMonitor.kt`, `PerformanceSessionStore.kt` | OWNER_CONFIRMATION_REQUIRED |
| Account identifiers/auth data | no local account system found | no | Not implemented | n/a | n/a | no auth/account implementation found in `app/src/main/java` | OWNER_CONFIRMATION_REQUIRED |
| Advertising ID | no evidence found | no evidence found | Not used by local code | n/a | n/a | no ad SDK dependency found in Gradle scan | OWNER_CONFIRMATION_REQUIRED |

## Exported Components Review

| Component | Exported | Protection | Purpose | Evidence |
|---|---|---|---|---|
| `.MainActivity` | true | launcher | App entrypoint | `AndroidManifest.xml` |
| `.core.health.HealthConnectPermissionsRationaleActivity` | true | action-scoped | Health Connect rationale | `AndroidManifest.xml` |
| Health Connect onboarding aliases | true | Health Connect onboarding permissions | Android/Health Connect onboarding surfaces | `AndroidManifest.xml` |
| Health Connect permission usage alias | true | `android.permission.START_VIEW_PERMISSION_USAGE` | Permission usage UI | `AndroidManifest.xml` |

## Owner Checklist Before Submission

- Confirm whether production telemetry upload is enabled and who receives it.
- Confirm retention period for local telemetry/performance session data.
- Confirm whether any backend receives profile, workout, nutrition, or Health Connect data.
- Confirm exact privacy policy URL and publication date.
- Confirm Health Connect declaration wording for each requested data type.
- Confirm whether meal images are retained locally after scan or only transiently used.

## Closure Control

Status: `OPEN`

Owner role: legal/privacy owner + Play Console release owner

Decision required: approve final Data Safety answers for the release candidate.

Allowed options:

- Approve worksheet as matching final production build.
- Request changes and update this worksheet plus privacy policy.
- Block release until product/backend/AI/telemetry decisions are complete.

Required evidence:

- Final production build config and dependency scan.
- Completed Data Safety answers.
- Published privacy policy URL.
- Owner/legal approval note.

Exact completion criteria:

- Every worksheet row has final owner-confirmed collected/shared/encrypted/deletion answers.
- No row still depends on an unresolved telemetry/backend/AI/account decision.
- `docs/release/data-safety-decision-gates.md` has been completed.
- `LEGAL-001` in `docs/release/owner-action-tracker.md` is `APPROVED`.

Release impact if not completed: release remains `BLOCKED`; do not submit Play Data Safety answers.

Signoff:

- Owner:
- Decision:
- Date:
- Status: `OPEN | IN_REVIEW | APPROVED | BLOCKED`
