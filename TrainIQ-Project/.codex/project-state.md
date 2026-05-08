# TrainIQ Project State

Last updated: 2026-05-08

## Current Objective

Bring the Android app as close as local tools allow to `D:\GitHub\TrainIQ\TrainIQ_Target_State_Blueprint.md`.

## Blueprint Coverage Checklist

| ID | Requirement | Status | Evidence | Priority |
|---|---|---|---|---|
| B-01 | Product vision: Health Connect, domain intelligence, Gemini 2.5 Flash, Material 3, resilient manual logging | satisfied | `app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`, `app/src/main/java/com/trainiq/ai/services/AiSupport.kt`, `app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt` | P1 |
| B-02 | MVVM/Clean/UDF with screen UI state and no UI database mapping | satisfied | `app/src/test/java/com/trainiq/architecture/ScreenUiStateArchitectureTest.kt`, `app/src/main/java/com/trainiq/domain/usecase/UseCases.kt` | P1 |
| B-03 | Room migrations, FK cleanup/checks, schema v12, dirty-data tests | satisfied | `app/src/main/java/com/trainiq/core/database/TrainIqMigrations.kt`, `app/src/androidTest/java/com/trainiq/core/database/TrainIqDatabaseMigrationTest.kt`, `app/schemas/com.trainiq.core.database.TrainIqDatabase/12.json` | P0 |
| B-04 | Health Connect availability, rationale, partial permissions, changes-token sync, background gating | satisfied locally | `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`, `app/src/main/java/com/trainiq/core/health/HealthConnectBackgroundSyncWorker.kt`, `app/src/test/java/com/trainiq/data/datasource/HealthConnectPermissionPolicyTest.kt` | P0 |
| B-05 | Health Connect Play declaration, Data Safety parity, privacy policy parity | blocked | `docs/play-privacy-release-evidence.md` | external/manual |
| B-06 | Gemini 2.5 Flash stable model, JSON schema, thinking budgets, header auth, safe fallback, bounded 429 retry | satisfied locally | `app/src/main/java/com/trainiq/ai/services/AiSupport.kt`, `app/src/main/java/com/trainiq/ai/services/AiServices.kt`, `app/src/main/java/com/trainiq/ai/services/RoutineGeneratorService.kt`, `app/src/main/java/com/trainiq/ai/services/GeminiJsonSchemas.kt`, `app/src/test/java/com/trainiq/ai/services/AiServicesTest.kt`, `app/src/test/java/com/trainiq/ai/services/RoutineGeneratorServiceTest.kt`, `app/src/test/java/com/trainiq/data/remote/GeminiApiContractTest.kt` | P0 |
| B-07 | Production AI gateway/OAuth boundary | blocked | `docs/play-privacy-release-evidence.md` | external/product |
| B-08 | Material 3, dynamic color, compact navigation, hidden Progress path, swipe sync, text-input/IME stability | satisfied locally | `app/src/main/java/com/trainiq/navigation/TrainIqNav.kt`, `app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`, `app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`, `app/src/main/java/com/trainiq/features/progress/ProgressScreen.kt`, QA artifacts in `D:\GitHub\TrainIQ\qa-cycle-runtime\resume-final` | P1 |
| B-09 | Accessibility labels, focus behavior, touch targets, font scaling readiness | partial locally | Compose semantics in feature screens; emulator XML smoke in `D:\GitHub\TrainIQ\qa-cycle-runtime\resume-final`; full TalkBack/Switch Access remains manual | external/manual |
| B-10 | Feature states: home degrade, training, nutrition scanner fallbacks, coach stale/missing data, progress, settings controls | satisfied locally | `app/src/main/java/com/trainiq/features/*`, `app/src/test/java/com/trainiq/features/nutrition/*`, `app/src/test/java/com/trainiq/features/settings/SettingsUiStateTest.kt` | P1 |
| B-11 | Performance: no startup-blocking sync, baseline/profile module, macrobenchmark target checks, privacy-safe telemetry | satisfied locally | `macrobenchmark/`, `app/src/main/baseline-prof.txt`, `app/src/main/java/com/trainiq/core/diagnostics/TelemetryExport.kt`, `app/src/test/java/com/trainiq/core/diagnostics/TelemetryExportPipelineTest.kt` | P1 |
| B-12 | Profileable/release macrobenchmark thresholds and physical-device verdict | blocked | `docs/play-privacy-release-evidence.md` | external/manual |
| B-13 | Release/build readiness: signing readiness check, local path cleanup, migration marker docs | satisfied locally | `app/build.gradle.kts`, `gradle.properties`, `macrobenchmark/build.gradle.kts`, validation commands in automation state | P1 |
| B-14 | Privacy/security: BYOK local hardening, no hardcoded secrets, no API key URLs/logs, local data deletion disclosures | satisfied locally | `app/src/main/java/com/trainiq/core/security/GeminiKeyMigration.kt`, `app/src/main/java/com/trainiq/core/security/AndroidKeystoreGeminiKeyStore.kt`, `app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`, `docs/play-privacy-release-evidence.md` | P0 |

## Latest Local Verification Summary

- Gradle assemble/unit/androidTest compile/macrobenchmark compile/lint/profileable/signing readiness: PASS on current workspace.
- Connected debug Android tests: PASS on current workspace.
- Emulator install/launch smoke QA: PASS on `emulator-5554` with fresh artifacts in `D:\GitHub\TrainIQ\qa-cycle-runtime\target-state-final`.
- Swipe QA: PASS for Nutrition pager `Vandaag -> Recepten -> Producten`.
- Text input/IME QA: PASS for Coach `Gewicht (kg)` field.
- Logcat/crash buffer: no app crash or ANR captured; fresh launch scan only matched normal `uiautomator` `AndroidRuntime` process lines.

## Known External Blockers

- Play Console Health Apps declaration, Data Safety submission, and published privacy policy verification. Local readiness package: `docs/release/play-console-data-safety-worksheet.md`, `docs/release/privacy-policy-draft.md`, `docs/release/play-console-owner-checklist.md`.
- Full TalkBack/Switch Access certification. Local readiness package: `docs/qa/accessibility-manual-qa-plan.md`, `docs/qa/talkback-switch-access-test-script.md`.
- Physical-device performance certification. Local readiness package: `docs/qa/device-lab-performance-plan.md`, `docs/qa/performance-evidence-template.md`.
- Production server-side Gemini/OAuth gateway product and backend decision. Local readiness package: `docs/architecture/ai-gateway-decision-record.md`, `docs/security/ai-byok-security-review.md`, `docs/security/production-ai-boundary-checklist.md`.

## Latest Blocker Burn-Down Pass

- Reviewed manifest permissions, exported Health Connect components, Gradle dependencies, AI/BYOK storage and network paths, telemetry, local storage, and deletion paths.
- Added Settings disclosure that explicit AI actions send the needed prompt/context and selected photo when relevant to Google Gemini using the locally stored API key.
- Prepared owner/legal/manual/backend handoff documents for all four remaining blockers.
- Marked legal/product/device-lab/manual assertions as `OWNER_CONFIRMATION_REQUIRED` or `PRODUCT_CONFIRMATION_REQUIRED` where local evidence cannot prove them.

## Latest Risk Decision-Gate Pass

- Converted Legal/Data Safety volatility into explicit gates: `docs/release/data-safety-decision-gates.md` and `docs/release/data-safety-change-impact-matrix.md`.
- Converted physical performance risk into threshold/device-lab gates: `docs/qa/performance-threshold-decision-record.md` and `docs/qa/device-lab-performance-readiness.md`.
- Converted accessibility certification risk into human QA boundaries: `docs/qa/accessibility-certification-boundary.md` and `docs/qa/human-assistive-tech-qa-signoff.md`.
- Converted production AI risk into architecture/security decision gates: `docs/architecture/production-ai-boundary-decision-gate.md` and `docs/security/byok-vs-production-gateway-risk-register.md`.
- Added final release guardrail register: `docs/release/final-release-risk-register.md`.
- No additional code defect was identified that could safely close these external/manual/backend risks locally.

## Owner Handoff Status

Release status: `BLOCKED`

Owner action tracker: `docs/release/owner-action-tracker.md`

Remaining approvals:

- `LEGAL-001`: product/legal/release owner must approve Data Safety gates, Play worksheet, and published privacy policy.
- `PERF-001`: product/Android/release owner must approve numeric thresholds and physical-device performance evidence.
- `A11Y-001`: accessibility owner/manual QA tester must approve TalkBack/Switch Access signoff.
- `AI-001`: product/backend/security/legal owners must choose production AI mode, or explicitly scope AI out with documented implications.

Next safe action: hand off `docs/release/owner-action-tracker.md` to the named owners. Codex/local engineering must not mark release ready until required approvals are present.
