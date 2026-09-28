# Play Console Data Safety Worksheet

> Release scope update (2026-09-06): [itch.io release policy](itch-release-policy.md) supersedes the owner-approval and mandatory certification release gates below. LEGAL-001, PERF-001, A11Y-001, and AI-001 are retired for this personal itch.io project. Older BLOCKED/OPEN statements are historical or refer to optional certification/future Play submission, not current itch.io delivery. Preserve actual test results and technical findings; do not claim missing evidence passed.

Last updated: 2026-05-12

Status: local evidence worksheet only. This is not a Play Console submission and is not legal advice.

Current release status: `BLOCKED`. Use this worksheet with `docs/release/owner-decision-packet-2026-05-10.md` (content refreshed on 2026-05-12), `docs/release/owner-action-tracker.md`, and `docs/release/play-console-owner-checklist.md`. Do not submit Data Safety answers until `LEGAL-001` is approved with final production evidence.

## Local App Evidence Summary

- Manifest permissions: `app/src/main/AndroidManifest.xml`
- Build/dependencies: `app/build.gradle.kts`, `gradle/libs.versions.toml`
- Health Connect implementation: `app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`
- AI provider implementation: `app/src/main/java/com/trainiq/ai/services/AiProviders.kt`, `app/src/main/java/com/trainiq/data/remote/GeminiApi.kt`, `app/src/main/java/com/trainiq/data/remote/OpenAiApi.kt`
- BYOK storage: `app/src/main/java/com/trainiq/core/security/AndroidKeystoreGeminiKeyStore.kt`, `app/src/main/java/com/trainiq/core/security/AndroidKeystoreOpenAiKeyStore.kt`
- Telemetry implementation: `app/src/main/java/com/trainiq/core/diagnostics/TelemetryExport.kt`
- Local deletion path: `app/src/main/java/com/trainiq/domain/usecase/UseCases.kt`, `app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`

## Data Safety Worksheet

| Data type | Collected | Shared | Purpose | Encrypted in transit | User deletion path | Evidence file/path | Owner/legal confirmation needed |
|---|---|---|---|---|---|---|---|
| Name/profile label | yes | no locally | Profile personalization and coaching context | n/a local only | Settings -> Lokale data wissen; profile reset | `Entities.kt`, `SettingsSection.kt`, `ClearAppDataUseCase` | OWNER_CONFIRMATION_REQUIRED for production backend status |
| Age, sex, height, weight, body fat, activity level, goal, optional manual calorie target | yes | yes to the first configured AI provider in the user's order with a saved key when goal advice is requested | Goal advice and macro targets | yes for provider HTTPS calls | Settings -> Profiel verwijderen or Lokale data wissen | `DomainModels.kt`, `Entities.kt`, `SettingsSection.kt`, `AiServices.kt` | OWNER_CONFIRMATION_REQUIRED |
| Workout routines, sets, sessions, exercise history, and routine-generation inputs | yes | yes to the first configured AI provider in the user's order with a saved key for routine generation and workout debrief. Routine inputs can include goal, target focus, days per week, equipment, experience, duration, deload choice, priority muscles, preferred/excluded exercises, and existing exercise names. Debrief inputs may be queued on workout completion and processed later in the background when AI is enabled and a provider key is available. | Training log, routine generation, active workout, progress, and workout debrief | yes for provider HTTPS calls | Settings -> Lokale data wissen; per-item workout deletion requests cancellation of its queued debrief | `Entities.kt`, `WorkoutScreen.kt`, `TrainIqRepository.kt`, `WorkoutDebriefWorker.kt`, `AiServices.kt`, `AiPrompts.kt` | OWNER_CONFIRMATION_REQUIRED |
| Weekly report aggregates: training volume, weight trend, adherence percentage | yes, derived locally from workout and meal timestamps | yes to the first configured AI provider in the user's order with a saved key when the user requests a weekly AI report; raw timestamps are read locally and not sent | Weekly training and adherence summary | yes for provider HTTPS calls | Settings -> Lokale data wissen; source workout/measurement/meal delete actions | `CoachScreen.kt`, `AiServices.kt`, `Entities.kt`, `NutritionScreen.kt`, `WorkoutScreen.kt` | OWNER_CONFIRMATION_REQUIRED |
| Saved nutrition foods, recipes, meal records, and notes | yes | no; these records and note contents do not feed current AI requests. Meal timestamps are read locally to derive the adherence percentage included in a user-requested weekly report | Nutrition tracking, reusable food/recipe logging, and local adherence calculation | n/a for these local records | Settings -> Lokale data wissen; per-item delete actions | `NutritionScreen.kt`, `AiServices.kt` | OWNER_CONFIRMATION_REQUIRED |
| Meal photo selected for AI scan and active scan details | yes when user captures/selects scan | yes to the first configured AI provider in the user's order with a saved key: selected image, formatted capture time, suggested meal type, and sanitized user-provided scan context | Meal analysis estimate | yes, HTTPS via provider API | Local temp/image handling and Settings -> Lokale data wissen; verify camera cache behavior before release | `CameraScannerScreen.kt`, `AiProviders.kt`, `GeminiApi.kt`, `OpenAiApi.kt`, `AiServices.kt` | OWNER_CONFIRMATION_REQUIRED |
| Smart-scale/body-measurement photo selected for AI reading | yes when user selects the photo | yes to the first configured AI provider in the user's order with a saved key when the reading is processed; any context supplied for the reading may accompany the image | Read body measurements from the selected image and context | yes, HTTPS via provider API | Verify image/context retention and deletion behavior before release; Settings -> Lokale data wissen for app data | `AiServices.kt` (`BodyMeasurementPhotoService`), `CameraScannerScreen.kt`, `AiProviders.kt`, `GeminiApi.kt`, `OpenAiApi.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect steps | yes if permission granted | no locally | Dashboard, recovery/activity context | n/a local only | Settings -> Lokale data wissen clears cache; revoke access in Android Health Connect | `AndroidManifest.xml`, `HealthConnectDataSource.kt`, `UserPreferencesRepository.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect heart rate | yes if permission granted | no locally | Recovery/activity context | n/a local only | Same as Health Connect above | `AndroidManifest.xml`, `HealthConnectDataSource.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect sleep | yes if permission granted | no locally | Recovery/activity context | n/a local only | Same as Health Connect above | `AndroidManifest.xml`, `HealthConnectDataSource.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect active calories | yes if permission granted | no locally | Energy balance context | n/a local only | Same as Health Connect above | `AndroidManifest.xml`, `HealthConnectDataSource.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect exercise sessions | yes if permission granted | no locally | Training context | n/a local only | Same as Health Connect above | `AndroidManifest.xml`, `HealthConnectDataSource.kt` | OWNER_CONFIRMATION_REQUIRED |
| Health Connect sync tokens/cache metadata | yes | no | Incremental sync and cache freshness | n/a local only | Settings -> Lokale data wissen clears cache | `UserPreferencesRepository.kt` | OWNER_CONFIRMATION_REQUIRED |
| Google Gemini and OpenAI API keys | yes, user-entered separately for each provider | yes to the corresponding provider as authentication for AI requests, including deferred workout debrief | BYOK authentication | yes, HTTPS headers (`x-goog-api-key` for Gemini; `Authorization: Bearer` for OpenAI) | Settings -> AI-sleutels verwijderen or Lokale data wissen | `AndroidKeystoreGeminiKeyStore.kt`, `AndroidKeystoreOpenAiKeyStore.kt`, `GeminiApi.kt`, `OpenAiApi.kt`, `SettingsSection.kt` | OWNER_CONFIRMATION_REQUIRED |
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
